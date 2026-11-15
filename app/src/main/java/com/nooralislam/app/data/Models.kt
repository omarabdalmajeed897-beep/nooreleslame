package com.nooralislam.app.data

data class SurahMeta(
    val id: Int,
    val nameAr: String,
    val ayahCount: Int,
    val revelationType: String,
    val juz: Int
)

data class ReciterInfo(
    val id: String,
    val nameAr: String,
    val styleAr: String,
    val countryAr: String,
    val serverBaseUrl: String,
    val everyAyahFolder: String
) {
    fun buildSurahUrl(surahId: Int): String {
        val padded = surahId.coerceIn(1, 114).toString().padStart(3, '0')
        return "${serverBaseUrl}${padded}.mp3"
    }

    fun buildAyahUrl(surahId: Int, ayahNumber: Int, globalNumber: Int): String {
        return if (everyAyahFolder.isNotBlank()) {
            val s = surahId.toString().padStart(3, '0')
            val a = ayahNumber.toString().padStart(3, '0')
            "https://everyayah.com/data/${everyAyahFolder}/${s}${a}.mp3"
        } else {
            "https://cdn.islamic.network/quran/audio/128/ar.alafasy/${globalNumber}.mp3"
        }
    }
}

data class AyahItem(
    val numberInSurah: Int,
    val globalNumber: Int,
    val page: Int,
    val juz: Int,
    val text: String,
    val tafsir: String,
    val surahId: Int,
    val surahName: String
)

data class HadithEntry(
    val id: Int,
    val title: String,
    val category: String,
    val narrator: String,
    val source: String,
    val text: String,
    val lesson: String
)

data class DhikrEntry(
    val id: String,
    val category: String,
    val title: String,
    val text: String,
    val targetCount: Int,
    val virtue: String
)

data class NameOfAllahEntry(
    val number: Int,
    val name: String,
    val meaning: String
)

data class ProphetStory(
    val id: String,
    val order: Int,
    val nameAr: String,
    val titleAr: String,
    val eraOrPeople: String,
    val quranMentions: Int,
    val keySurahs: String,
    val summary: String,
    val paragraphs: List<String>,
    val miracles: List<String>,
    val lessons: List<String>
)

data class SahabiStory(
    val id: String,
    val order: Int,
    val nameAr: String,
    val titleAr: String,
    val category: String,
    val summary: String,
    val story: List<String>,
    val keyVirtue: String
)

data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class QuizStage(
    val stageNumber: Int,
    val titleAr: String,
    val chapterTitleAr: String,
    val questions: List<QuizQuestion>
)

data class VerseQuotePuzzle(
    val id: String,
    val kind: String,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val hint: String,
    val ref: String
)

data class PrayerTimesModel(
    val fajr: Double,
    val sunrise: Double,
    val dhuhr: Double,
    val asr: Double,
    val maghrib: Double,
    val isha: Double
)

data class QuranBookmark(
    val surahId: Int,
    val surahName: String,
    val ayahNumber: Int,
    val textPreview: String
)

data class CustomTasbihPhrase(
    val text: String,
    val target: Int
)

data class CityOption(
    val id: String,
    val nameAr: String,
    val lat: Double,
    val lon: Double,
    val tzOffset: Double
)

data class ReminderPrefs(
    val prePrayer30Min: Boolean = true,
    val morningEnabled: Boolean = false,
    val morningTime: String = "06:30",
    val eveningEnabled: Boolean = false,
    val eveningTime: String = "17:00"
)
