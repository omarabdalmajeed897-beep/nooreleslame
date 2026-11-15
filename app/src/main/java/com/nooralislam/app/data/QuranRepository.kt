package com.nooralislam.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class QuranRepository(private val context: Context) {
    @Volatile private var quranJson: JSONObject? = null
    @Volatile private var recitersCache: List<ReciterInfo>? = null
    @Volatile private var surahsCache: List<SurahMeta>? = null
    @Volatile private var hadithsCache: List<HadithEntry>? = null
    @Volatile private var adhkarCache: List<DhikrEntry>? = null
    @Volatile private var namesCache: List<NameOfAllahEntry>? = null
    @Volatile private var prophetsCache: List<ProphetStory>? = null
    @Volatile private var sahabaCache: List<SahabiStory>? = null
    @Volatile private var quizCache: List<QuizStage>? = null
    @Volatile private var puzzlesCache: List<VerseQuotePuzzle>? = null
    private val mushafPageCache = HashMap<Int, List<AyahItem>>()

    private fun readAssetText(name: String): String {
        return context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun ensureQuranLoaded(): JSONObject {
        quranJson?.let { return it }
        return synchronized(this) {
            quranJson ?: JSONObject(readAssetText("quran-offline.json")).also { quranJson = it }
        }
    }

    private fun jsonStringList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) add(arr.optString(i, ""))
        }
    }

    fun getSurahs(): List<SurahMeta> {
        surahsCache?.let { return it }
        val root = JSONObject(readAssetText("reciters.json"))
        val arr = root.getJSONArray("surahs")
        val list = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    SurahMeta(
                        id = o.getInt("id"),
                        nameAr = o.getString("nameAr"),
                        ayahCount = o.getInt("ayahCount"),
                        revelationType = o.getString("revelationType"),
                        juz = o.getInt("juz")
                    )
                )
            }
        }
        surahsCache = list
        return list
    }

    fun getReciters(): List<ReciterInfo> {
        recitersCache?.let { return it }
        val root = JSONObject(readAssetText("reciters.json"))
        val arr = root.getJSONArray("reciters")
        val list = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    ReciterInfo(
                        id = o.getString("id"),
                        nameAr = o.getString("nameAr"),
                        styleAr = o.getString("styleAr"),
                        countryAr = o.getString("countryAr"),
                        serverBaseUrl = o.getString("serverBaseUrl"),
                        everyAyahFolder = o.optString("everyAyahFolder", "")
                    )
                )
            }
        }
        recitersCache = list
        return list
    }

    fun getSurahAyahs(surahId: Int): List<AyahItem> {
        val db = ensureQuranLoaded()
        val sName = getSurahs().firstOrNull { it.id == surahId }?.nameAr.orEmpty()
        val arr = db.optJSONArray(surahId.toString()) ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    AyahItem(
                        numberInSurah = o.getInt("n"),
                        globalNumber = o.getInt("g"),
                        page = o.getInt("p"),
                        juz = o.getInt("j"),
                        text = o.getString("t"),
                        tafsir = o.optString("f", ""),
                        surahId = surahId,
                        surahName = sName
                    )
                )
            }
        }
    }

    fun getMushafPage(pageNumber: Int): List<AyahItem> {
        val safePage = pageNumber.coerceIn(1, 604)
        mushafPageCache[safePage]?.let { return it }
        val db = ensureQuranLoaded()
        val surahs = getSurahs().associateBy { it.id }
        val result = mutableListOf<AyahItem>()
        for (sId in 1..114) {
            val arr = db.optJSONArray(sId.toString()) ?: continue
            val sName = surahs[sId]?.nameAr.orEmpty()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.getInt("p") == safePage) {
                    result.add(
                        AyahItem(
                            numberInSurah = o.getInt("n"),
                            globalNumber = o.getInt("g"),
                            page = safePage,
                            juz = o.getInt("j"),
                            text = o.getString("t"),
                            tafsir = o.optString("f", ""),
                            surahId = sId,
                            surahName = sName
                        )
                    )
                }
            }
        }
        mushafPageCache[safePage] = result
        return result
    }

    fun searchQuran(query: String): List<AyahItem> {
        val normQuery = normalizeArabic(query)
        if (normQuery.length < 2) return emptyList()
        val db = ensureQuranLoaded()
        val surahs = getSurahs().associateBy { it.id }
        val hits = mutableListOf<AyahItem>()
        for (sId in 1..114) {
            val arr = db.optJSONArray(sId.toString()) ?: continue
            val sName = surahs[sId]?.nameAr.orEmpty()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val text = o.getString("t")
                if (normalizeArabic(text).contains(normQuery)) {
                    hits.add(
                        AyahItem(
                            numberInSurah = o.getInt("n"),
                            globalNumber = o.getInt("g"),
                            page = o.getInt("p"),
                            juz = o.getInt("j"),
                            text = text,
                            tafsir = o.optString("f", ""),
                            surahId = sId,
                            surahName = sName
                        )
                    )
                    if (hits.size >= 60) return hits
                }
            }
        }
        return hits
    }

    fun getHadiths(): List<HadithEntry> {
        hadithsCache?.let { return it }
        val arr = JSONArray(readAssetText("hadiths.json"))
        val list = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    HadithEntry(
                        id = o.getInt("id"),
                        title = o.getString("title"),
                        category = o.getString("category"),
                        narrator = o.getString("narrator"),
                        source = o.getString("source"),
                        text = o.getString("text"),
                        lesson = o.getString("lesson")
                    )
                )
            }
        }
        hadithsCache = list
        return list
    }

    fun getAdhkar(): List<DhikrEntry> {
        adhkarCache?.let { return it }
        val arr = JSONArray(readAssetText("adhkar.json"))
        val list = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    DhikrEntry(
                        id = o.getString("id"),
                        category = o.getString("category"),
                        title = o.getString("title"),
                        text = o.getString("text"),
                        targetCount = o.getInt("targetCount"),
                        virtue = o.optString("virtue", "")
                    )
                )
            }
        }
        adhkarCache = list
        return list
    }

    fun getNamesOfAllah(): List<NameOfAllahEntry> {
        namesCache?.let { return it }
        val arr = JSONArray(readAssetText("names.json"))
        val list = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    NameOfAllahEntry(
                        number = o.getInt("number"),
                        name = o.getString("name"),
                        meaning = o.getString("meaning")
                    )
                )
            }
        }
        namesCache = list
        return list
    }

    fun getProphets(): List<ProphetStory> {
        prophetsCache?.let { return it }
        val arr = JSONArray(readAssetText("prophets.json"))
        val list = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    ProphetStory(
                        id = o.getString("id"),
                        order = o.getInt("order"),
                        nameAr = o.getString("nameAr"),
                        titleAr = o.getString("titleAr"),
                        eraOrPeople = o.getString("eraOrPeople"),
                        quranMentions = o.optInt("quranMentions", 0),
                        keySurahs = o.optString("keySurahs", ""),
                        summary = o.getString("summary"),
                        paragraphs = jsonStringList(o.optJSONArray("paragraphs")),
                        miracles = jsonStringList(o.optJSONArray("miracles")),
                        lessons = jsonStringList(o.optJSONArray("lessons"))
                    )
                )
            }
        }
        prophetsCache = list
        return list
    }

    fun getSahaba(): List<SahabiStory> {
        sahabaCache?.let { return it }
        val arr = JSONArray(readAssetText("sahaba.json"))
        val list = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    SahabiStory(
                        id = o.getString("id"),
                        order = o.getInt("order"),
                        nameAr = o.getString("nameAr"),
                        titleAr = o.getString("titleAr"),
                        category = o.getString("category"),
                        summary = o.getString("summary"),
                        story = jsonStringList(o.optJSONArray("story")),
                        keyVirtue = o.optString("keyVirtue", "")
                    )
                )
            }
        }
        sahabaCache = list
        return list
    }

    fun getQuizStages(): List<QuizStage> {
        quizCache?.let { return it }
        val arr = JSONArray(readAssetText("quiz.json"))
        val list = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val qArr = o.getJSONArray("questions")
                val questions = buildList {
                    for (j in 0 until qArr.length()) {
                        val qo = qArr.getJSONObject(j)
                        add(
                            QuizQuestion(
                                id = qo.getString("id"),
                                question = qo.getString("question"),
                                options = jsonStringList(qo.getJSONArray("options")),
                                correctIndex = qo.getInt("correctIndex"),
                                explanation = qo.optString("explanation", "")
                            )
                        )
                    }
                }
                add(
                    QuizStage(
                        stageNumber = o.getInt("stageNumber"),
                        titleAr = o.getString("titleAr"),
                        chapterTitleAr = o.optString("chapterTitleAr", ""),
                        questions = questions
                    )
                )
            }
        }
        quizCache = list
        return list
    }

    fun getPuzzles(): List<VerseQuotePuzzle> {
        puzzlesCache?.let { return it }
        val arr = JSONArray(readAssetText("puzzles.json"))
        val list = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    VerseQuotePuzzle(
                        id = o.getString("id"),
                        kind = o.getString("kind"),
                        prompt = o.getString("prompt"),
                        options = jsonStringList(o.getJSONArray("options")),
                        correctIndex = o.getInt("correctIndex"),
                        hint = o.optString("hint", ""),
                        ref = o.optString("ref", "")
                    )
                )
            }
        }
        puzzlesCache = list
        return list
    }

    companion object {
        fun normalizeArabic(input: String): String {
            return input
                .replace("\u0671", "\u0627")
                .replace(Regex("[\u0610-\u061A\u064B-\u065F\u06D6-\u06ED\u0640]"), "")
                .replace("\u0670", "\u0627")
                .replace(Regex("[أإآ]"), "ا")
                .replace("ى", "ي")
                .replace("ة", "ه")
                .replace("ؤ", "و")
                .replace("ئ", "ي")
                .trim()
        }
    }
}
