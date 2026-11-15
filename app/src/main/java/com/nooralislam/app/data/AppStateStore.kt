package com.nooralislam.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AppStateStore(context: Context) {
    private val prefs = context.getSharedPreferences("noor_al_islam_prefs", Context.MODE_PRIVATE)

    fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", false)
    fun setDarkMode(v: Boolean) = prefs.edit().putBoolean("dark_mode", v).apply()

    fun getCityId(): String = prefs.getString("city_id", "cairo") ?: "cairo"
    fun setCityId(v: String) = prefs.edit().putString("city_id", v).apply()

    fun isHanafiAsr(): Boolean = prefs.getBoolean("hanafi_asr", false)
    fun setHanafiAsr(v: Boolean) = prefs.edit().putBoolean("hanafi_asr", v).apply()

    fun getActiveSurahId(): Int = prefs.getInt("active_surah_id", 1).coerceIn(1, 114)
    fun setActiveSurahId(v: Int) = prefs.edit().putInt("active_surah_id", v.coerceIn(1, 114)).apply()

    fun getActiveReciterId(): String = prefs.getString("active_reciter_id", "afs") ?: "afs"
    fun setActiveReciterId(v: String) = prefs.edit().putString("active_reciter_id", v).apply()

    fun getFontSize(): Int = prefs.getInt("mushaf_font_size", 24).coerceIn(16, 40)
    fun setFontSize(v: Int) = prefs.edit().putInt("mushaf_font_size", v.coerceIn(16, 40)).apply()

    fun getXp(): Int = prefs.getInt("xp_points", 0)
    fun addXp(delta: Int): Int {
        val next = (getXp() + delta).coerceAtLeast(0)
        prefs.edit().putInt("xp_points", next).apply()
        return next
    }

    fun getSpeedBestScore(): Int = prefs.getInt("speed_best", 0)
    fun setSpeedBestScore(score: Int) {
        if (score > getSpeedBestScore()) prefs.edit().putInt("speed_best", score).apply()
    }

    fun getQuizProgress(): Map<Int, Int> {
        val raw = prefs.getString("quiz_progress_json", "{}") ?: "{}"
        val map = mutableMapOf<Int, Int>()
        runCatching {
            val obj = JSONObject(raw)
            obj.keys().forEach { k -> k.toIntOrNull()?.let { map[it] = obj.getInt(k) } }
        }
        return map
    }

    fun saveQuizStageResult(stageNumber: Int, stars: Int): Map<Int, Int> {
        val map = getQuizProgress().toMutableMap()
        val prev = map[stageNumber] ?: -1
        if (stars > prev) map[stageNumber] = stars
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k.toString(), v) }
        prefs.edit().putString("quiz_progress_json", obj.toString()).apply()
        return map
    }

    fun getKhatmahJuz(): Set<Int> {
        return prefs.getStringSet("khatmah_juz", emptySet()).orEmpty()
            .mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun toggleKhatmahJuz(juz: Int): Set<Int> {
        val s = getKhatmahJuz().toMutableSet()
        if (!s.add(juz)) s.remove(juz)
        prefs.edit().putStringSet("khatmah_juz", s.map { it.toString() }.toSet()).apply()
        return s
    }

    fun getReadProphets(): Set<String> = prefs.getStringSet("read_prophets", emptySet()).orEmpty().toSet()
    fun toggleReadProphet(id: String): Set<String> {
        val s = getReadProphets().toMutableSet()
        if (!s.add(id)) s.remove(id)
        prefs.edit().putStringSet("read_prophets", s).apply()
        return s
    }

    fun getReadSahaba(): Set<String> = prefs.getStringSet("read_sahaba", emptySet()).orEmpty().toSet()
    fun toggleReadSahabi(id: String): Set<String> {
        val s = getReadSahaba().toMutableSet()
        if (!s.add(id)) s.remove(id)
        prefs.edit().putStringSet("read_sahaba", s).apply()
        return s
    }

    fun getBookmarks(): List<QuranBookmark> {
        val raw = prefs.getString("quran_bookmarks_json", "[]") ?: "[]"
        val list = mutableListOf<QuranBookmark>()
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    QuranBookmark(
                        surahId = o.getInt("surahId"),
                        surahName = o.getString("surahName"),
                        ayahNumber = o.getInt("ayahNumber"),
                        textPreview = o.getString("textPreview")
                    )
                )
            }
        }
        return list
    }

    fun toggleBookmark(b: QuranBookmark): List<QuranBookmark> {
        val list = getBookmarks().toMutableList()
        val existingIdx = list.indexOfFirst { it.surahId == b.surahId && it.ayahNumber == b.ayahNumber }
        if (existingIdx >= 0) list.removeAt(existingIdx) else list.add(0, b)
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject().apply {
                    put("surahId", it.surahId)
                    put("surahName", it.surahName)
                    put("ayahNumber", it.ayahNumber)
                    put("textPreview", it.textPreview)
                }
            )
        }
        prefs.edit().putString("quran_bookmarks_json", arr.toString()).apply()
        return list
    }

    // Worship logs: dateKey -> { prayers: {fajr..isha: "on_time"|"late"|"missed"}, rawatib: {k: bool} }
    fun getAllWorshipLogs(): Map<String, JSONObject> {
        val raw = prefs.getString("worship_logs_json", "{}") ?: "{}"
        val out = mutableMapOf<String, JSONObject>()
        runCatching {
            val obj = JSONObject(raw)
            obj.keys().forEach { dateKey ->
                obj.optJSONObject(dateKey)?.let { out[dateKey] = it }
            }
        }
        return out
    }

    fun setPrayerStatus(dateKey: String, prayerKey: String, status: String): Map<String, JSONObject> {
        val all = getAllWorshipLogs().toMutableMap()
        val day = all[dateKey] ?: JSONObject()
        val prayers = day.optJSONObject("prayers") ?: JSONObject()
        prayers.put(prayerKey, status)
        day.put("prayers", prayers)
        all[dateKey] = day
        saveAllWorshipLogs(all)
        return all
    }

    fun toggleRawatib(dateKey: String, rawatibKey: String): Map<String, JSONObject> {
        val all = getAllWorshipLogs().toMutableMap()
        val day = all[dateKey] ?: JSONObject()
        val raw = day.optJSONObject("rawatib") ?: JSONObject()
        raw.put(rawatibKey, !raw.optBoolean(rawatibKey, false))
        day.put("rawatib", raw)
        all[dateKey] = day
        saveAllWorshipLogs(all)
        return all
    }

    private fun saveAllWorshipLogs(all: Map<String, JSONObject>) {
        val root = JSONObject()
        all.forEach { (k, v) -> root.put(k, v) }
        prefs.edit().putString("worship_logs_json", root.toString()).apply()
    }

    fun getAdhkarCounts(): Map<String, Int> {
        val savedDate = prefs.getString("adhkar_date", "")
        val today = todayDateKey()
        if (savedDate != today) return emptyMap()
        val raw = prefs.getString("adhkar_counts_json", "{}") ?: "{}"
        val out = mutableMapOf<String, Int>()
        runCatching {
            val obj = JSONObject(raw)
            obj.keys().forEach { k -> out[k] = obj.getInt(k) }
        }
        return out
    }

    fun incrementDhikrCount(id: String, target: Int): Map<String, Int> {
        val map = getAdhkarCounts().toMutableMap()
        val cur = map[id] ?: 0
        if (cur < target) map[id] = cur + 1
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit()
            .putString("adhkar_date", todayDateKey())
            .putString("adhkar_counts_json", obj.toString())
            .apply()
        return map
    }

    fun getTasbihCount(): Int = prefs.getInt("tasbih_count", 0)
    fun setTasbihCount(v: Int) = prefs.edit().putInt("tasbih_count", v).apply()
    fun getTasbihTarget(): Int = prefs.getInt("tasbih_target", 33)
    fun setTasbihTarget(v: Int) = prefs.edit().putInt("tasbih_target", v).apply()
    fun getTasbihLifetime(): Int = prefs.getInt("tasbih_lifetime", 0)
    fun setTasbihLifetime(v: Int) = prefs.edit().putInt("tasbih_lifetime", v).apply()

    fun getCustomTasbihPhrases(): List<CustomTasbihPhrase> {
        val raw = prefs.getString("custom_tasbih_json", null)
        if (raw == null) {
            return listOf(
                CustomTasbihPhrase("سبحان الله", 33),
                CustomTasbihPhrase("الحمد لله", 33),
                CustomTasbihPhrase("الله أكبر", 34),
                CustomTasbihPhrase("لا إله إلا الله وحده لا شريك له", 100),
                CustomTasbihPhrase("أستغفر الله العظيم وأتوب إليه", 100),
                CustomTasbihPhrase("اللهم صل وسلم على نبينا محمد", 100)
            )
        }
        val list = mutableListOf<CustomTasbihPhrase>()
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(CustomTasbihPhrase(o.getString("text"), o.getInt("target")))
            }
        }
        return list
    }

    fun addCustomTasbihPhrase(phrase: CustomTasbihPhrase): List<CustomTasbihPhrase> {
        val list = getCustomTasbihPhrases().toMutableList()
        list.add(phrase)
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("text", it.text).put("target", it.target)) }
        prefs.edit().putString("custom_tasbih_json", arr.toString()).apply()
        return list
    }

    fun getReminderPrefs(): ReminderPrefs {
        return ReminderPrefs(
            prePrayer30Min = prefs.getBoolean("rem_pre_prayer_30", true),
            morningEnabled = prefs.getBoolean("rem_morning_on", false),
            morningTime = prefs.getString("rem_morning_time", "06:30") ?: "06:30",
            eveningEnabled = prefs.getBoolean("rem_evening_on", false),
            eveningTime = prefs.getString("rem_evening_time", "17:00") ?: "17:00"
        )
    }

    fun saveReminderPrefs(p: ReminderPrefs) {
        prefs.edit()
            .putBoolean("rem_pre_prayer_30", p.prePrayer30Min)
            .putBoolean("rem_morning_on", p.morningEnabled)
            .putString("rem_morning_time", p.morningTime)
            .putBoolean("rem_evening_on", p.eveningEnabled)
            .putString("rem_evening_time", p.eveningTime)
            .apply()
    }

    companion object {
        fun todayDateKey(offsetDays: Int = 0): String {
            val cal = Calendar.getInstance()
            if (offsetDays != 0) cal.add(Calendar.DAY_OF_YEAR, offsetDays)
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        }
    }
}
