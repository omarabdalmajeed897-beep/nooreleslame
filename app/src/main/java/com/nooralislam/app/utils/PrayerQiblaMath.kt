package com.nooralislam.app.utils

import com.nooralislam.app.data.CityOption
import com.nooralislam.app.data.PrayerTimesModel
import java.util.Calendar
import kotlin.math.*

data class HijriDateInfo(val day: Int, val month: Int, val year: Int, val monthNameAr: String) {
    val formattedAr: String get() = "$day $monthNameAr $year هـ"
}

data class NextPrayerStatus(val key: String, val nameAr: String, val timeHours: Double, val remainingSeconds: Long)

object PrayerQiblaMath {
    private const val KAABA_LAT = 21.4225
    private const val KAABA_LON = 39.8262

    val PRESET_CITIES = listOf(
        CityOption("cairo", "القاهرة، مصر", 30.0444, 31.2357, 2.0),
        CityOption("alex", "الإسكندرية، مصر", 31.2001, 29.9187, 2.0),
        CityOption("makkah", "مكة المكرمة", 21.4225, 39.8262, 3.0),
        CityOption("madinah", "المدينة المنورة", 24.4672, 39.6112, 3.0),
        CityOption("riyadh", "الرياض، السعودية", 24.7136, 46.6753, 3.0),
        CityOption("jeddah", "جدة، السعودية", 21.4858, 39.1925, 3.0),
        CityOption("dubai", "دبي، الإمارات", 25.2048, 55.2708, 4.0),
        CityOption("doha", "الدوحة، قطر", 25.2854, 51.5310, 3.0),
        CityOption("kuwait", "مدينة الكويت", 29.3759, 47.9774, 3.0),
        CityOption("amman", "عمّان، الأردن", 31.9454, 35.9284, 3.0),
        CityOption("quds", "القدس الشريف", 31.7683, 35.2137, 2.0),
        CityOption("baghdad", "بغداد، العراق", 33.3152, 44.3661, 3.0),
        CityOption("damascus", "دمشق، سوريا", 33.5138, 36.2765, 3.0),
        CityOption("beirut", "بيروت، لبنان", 33.8938, 35.5018, 2.0),
        CityOption("khartoum", "الخرطوم، السودان", 15.5007, 32.5599, 2.0),
        CityOption("tunis", "تونس العاصمة", 36.8065, 10.1815, 1.0),
        CityOption("algiers", "الجزائر العاصمة", 36.7538, 3.0588, 1.0),
        CityOption("casablanca", "الدار البيضاء، المغرب", 33.5731, -7.5898, 1.0),
        CityOption("istanbul", "إسطنبول، تركيا", 41.0082, 28.9784, 3.0),
        CityOption("london", "لندن، بريطانيا", 51.5074, -0.1278, 0.0)
    )

    val HIJRI_MONTHS_AR = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    fun qiblaBearing(lat: Double, lon: Double): Double {
        val phi1 = Math.toRadians(lat)
        val phi2 = Math.toRadians(KAABA_LAT)
        val dLambda = Math.toRadians(KAABA_LON - lon)
        val y = sin(dLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    fun distanceToKaabaKm(lat: Double, lon: Double): Int {
        val r = 6371.0
        val dLat = Math.toRadians(KAABA_LAT - lat)
        val dLon = Math.toRadians(KAABA_LON - lon)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat)) * cos(Math.toRadians(KAABA_LAT)) * sin(dLon / 2).pow(2)
        return (2.0 * r * atan2(sqrt(a), sqrt(1.0 - a))).roundToInt()
    }

    fun calculateTimes(
        lat: Double,
        lon: Double,
        tzOffset: Double,
        hanafiAsr: Boolean = false,
        cal: Calendar = Calendar.getInstance()
    ): PrayerTimesModel {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val a = (14 - m) / 12
        val yr = y + 4800 - a
        val mo = m + 12 * a - 3
        val jd = d + (153 * mo + 2) / 5 + 365 * yr + yr / 4 - yr / 100 + yr / 400 - 32045.0
        val days = jd - 2451545.0

        val g = Math.toRadians((357.529 + 0.98560028 * days) % 360.0)
        val q = (280.459 + 0.98564736 * days) % 360.0
        val l = Math.toRadians((q + 1.915 * sin(g) + 0.020 * sin(2 * g)) % 360.0)
        val e = Math.toRadians(23.439 - 0.00000036 * days)
        val decl = asin(sin(e) * sin(l))
        var ra = Math.toDegrees(atan2(cos(e) * sin(l), cos(l))) / 15.0
        ra = ((ra % 24.0) + 24.0) % 24.0
        val eqt = (q / 15.0) - ra

        val dhuhr = 12.0 + tzOffset - lon / 15.0 - eqt
        val phi = Math.toRadians(lat)

        fun sunAngleOffset(altDeg: Double): Double {
            val altRad = Math.toRadians(altDeg)
            val cosH = (sin(altRad) - sin(phi) * sin(decl)) / (cos(phi) * cos(decl))
            return Math.toDegrees(acos(cosH.coerceIn(-1.0, 1.0))) / 15.0
        }

        val sunriseOff = sunAngleOffset(-0.833)
        val fajrOff = sunAngleOffset(-19.5)
        val ishaOff = sunAngleOffset(-17.5)

        val shadowFactor = if (hanafiAsr) 2.0 else 1.0
        val asrAlt = atan(1.0 / (shadowFactor + tan(abs(phi - decl))))
        val asrCosH = (sin(asrAlt) - sin(phi) * sin(decl)) / (cos(phi) * cos(decl))
        val asrOff = Math.toDegrees(acos(asrCosH.coerceIn(-1.0, 1.0))) / 15.0

        return PrayerTimesModel(
            fajr = dhuhr - fajrOff,
            sunrise = dhuhr - sunriseOff,
            dhuhr = dhuhr,
            asr = dhuhr + asrOff,
            maghrib = dhuhr + sunriseOff,
            isha = dhuhr + ishaOff
        )
    }

    fun getNextPrayer(times: PrayerTimesModel, nowCal: Calendar = Calendar.getInstance()): NextPrayerStatus {
        val nowH = nowCal.get(Calendar.HOUR_OF_DAY) + nowCal.get(Calendar.MINUTE) / 60.0 + nowCal.get(Calendar.SECOND) / 3600.0
        val list = listOf(
            Triple("fajr", "الفجر", times.fajr),
            Triple("dhuhr", "الظهر", times.dhuhr),
            Triple("asr", "العصر", times.asr),
            Triple("maghrib", "المغرب", times.maghrib),
            Triple("isha", "العشاء", times.isha)
        )
        for ((k, n, h) in list) {
            if (h > nowH) {
                return NextPrayerStatus(k, n, h, ((h - nowH) * 3600.0).toLong().coerceAtLeast(0))
            }
        }
        val tomorrowFajr = times.fajr + 24.0
        return NextPrayerStatus("fajr", "الفجر", times.fajr, ((tomorrowFajr - nowH) * 3600.0).toLong().coerceAtLeast(0))
    }

    fun formatCountdown(totalSec: Long): String {
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    fun formatTime(hours: Double): String {
        val totalMin = (hours * 60).roundToInt().mod(24 * 60)
        val h24 = totalMin / 60
        val min = totalMin % 60
        val period = if (h24 >= 12) "م" else "ص"
        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
        return "%d:%02d %s".format(h12, min, period)
    }

    fun gregorianToHijri(cal: Calendar = Calendar.getInstance()): HijriDateInfo {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val jd = if (y > 1582 || (y == 1582 && m > 10) || (y == 1582 && m == 10 && d > 14)) {
            ((1461 * (y + 4800 + (m - 14) / 12)) / 4) +
                ((367 * (m - 2 - 12 * ((m - 14) / 12))) / 12) -
                ((3 * ((y + 4900 + (m - 14) / 12) / 100)) / 4) + d - 32075
        } else {
            367 * y - ((7 * (y + 5001 + (m - 9) / 7)) / 4) + ((275 * m) / 9) + d + 1729777
        }
        var l = jd - 1948440 + 10632
        val n = (l - 1) / 10631
        l = l - 10631 * n + 354
        val j = ((10985 - l) / 5316) * ((50 * l) / 17719) + (l / 5670) * ((43 * l) / 15238)
        l = l - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val hm = (24 * l) / 709
        val hd = l - (709 * hm) / 24
        val hy = 30 * n + j - 30
        val monthName = HIJRI_MONTHS_AR.getOrElse(hm - 1) { "محرم" }
        return HijriDateInfo(hd, hm, hy, monthName)
    }
}
