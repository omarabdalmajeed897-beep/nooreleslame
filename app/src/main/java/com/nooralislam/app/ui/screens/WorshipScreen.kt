package com.nooralislam.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nooralislam.app.R
import com.nooralislam.app.data.AppStateStore
import com.nooralislam.app.data.CityOption
import com.nooralislam.app.data.CustomTasbihPhrase
import com.nooralislam.app.data.PrayerTimesModel
import com.nooralislam.app.data.QuranRepository
import com.nooralislam.app.data.ReminderPrefs
import com.nooralislam.app.notifications.NotificationScheduler
import com.nooralislam.app.notifications.PrayerAlarmReceiver
import com.nooralislam.app.ui.CardShape
import com.nooralislam.app.ui.NoorColors
import com.nooralislam.app.ui.cardBorder
import com.nooralislam.app.ui.surfaceCardColors
import com.nooralislam.app.utils.PrayerQiblaMath
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorshipScreen(
    darkMode: Boolean,
    initialSubTab: Int,
    repository: QuranRepository,
    city: CityOption,
    hanafiAsr: Boolean,
    prayerTimes: PrayerTimesModel,
    worshipLogs: Map<String, JSONObject>,
    adhkarCounts: Map<String, Int>,
    tasbihCount: Int,
    tasbihTarget: Int,
    tasbihLifetime: Int,
    customTasbihPhrases: List<CustomTasbihPhrase>,
    khatmahJuz: Set<Int>,
    reminderPrefs: ReminderPrefs,
    xp: Int,
    quizCompletedCount: Int,
    readProphetsCount: Int,
    readSahabaCount: Int,
    onSetPrayerStatus: (dateKey: String, prayerKey: String, status: String) -> Unit,
    onToggleRawatib: (dateKey: String, rawatibKey: String) -> Unit,
    onCityChange: (CityOption) -> Unit,
    onHanafiChange: (Boolean) -> Unit,
    onIncrementDhikr: (id: String, target: Int) -> Unit,
    onTasbihTap: () -> Unit,
    onTasbihReset: () -> Unit,
    onTasbihTargetChange: (Int) -> Unit,
    onAddTasbihPhrase: (CustomTasbihPhrase) -> Unit,
    onToggleKhatmahJuz: (Int) -> Unit,
    onSaveReminders: (ReminderPrefs) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current
    val accent = if (darkMode) NoorColors.EmeraldGlow else NoorColors.Emerald
    val goldAccent = if (darkMode) NoorColors.GoldLight else NoorColors.Gold
    val muted = if (darkMode) NoorColors.DarkMuted else NoorColors.MutedLight

    var subTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab.coerceIn(0, 6)) }

    // Arabic TTS for Hadiths
    var speakingHadithId by remember { mutableStateOf<Int?>(null) }
    val tts = remember {
        var inst: TextToSpeech? = null
        inst = TextToSpeech(context) { st ->
            if (st == TextToSpeech.SUCCESS) inst?.setLanguage(Locale("ar"))
        }
        inst
    }
    DisposableEffect(Unit) {
        onDispose { tts.stop(); tts.shutdown() }
    }

    // Datasets
    val hadiths = remember { repository.getHadiths() }
    val names99 = remember { repository.getNamesOfAllah() }
    val adhkar = remember { repository.getAdhkar() }

    // Hadith & Names filters
    var treasureMode by remember { mutableIntStateOf(0) } // 0 = Hadiths, 1 = 99 Names
    var hadithQuery by remember { mutableStateOf("") }
    var hadithCategory by remember { mutableStateOf("الكل") }
    var copiedBanner by remember { mutableStateOf<String?>(null) }
    var nameQuery by remember { mutableStateOf("") }

    // Adhkar filter
    var adhkarCat by remember { mutableStateOf("أذكار الصباح") }

    // Tasbih custom form state
    var selectedPhraseIndex by remember { mutableIntStateOf(0) }
    var newPhraseText by remember { mutableStateOf("") }
    var newPhraseTarget by remember { mutableStateOf("33") }

    // Zakat Calculator state
    var zCash by remember { mutableStateOf("0") }
    var zGoldGrams by remember { mutableStateOf("0") }
    var zGoldPrice by remember { mutableStateOf("3800") }
    var zSilverGrams by remember { mutableStateOf("0") }
    var zSilverPrice by remember { mutableStateOf("50") }
    var zInvestments by remember { mutableStateOf("0") }
    var zDebts by remember { mutableStateOf("0") }
    var zNisabStd by remember { mutableStateOf("gold") } // "gold" or "silver"

    // Calendar month offset
    var calMonthOffset by remember { mutableIntStateOf(0) }

    // Reminders editable state
    var remPrePrayer by remember(reminderPrefs) { mutableStateOf(reminderPrefs.prePrayer30Min) }
    var remMorningOn by remember(reminderPrefs) { mutableStateOf(reminderPrefs.morningEnabled) }
    var remMorningTime by remember(reminderPrefs) { mutableStateOf(reminderPrefs.morningTime) }
    var remEveningOn by remember(reminderPrefs) { mutableStateOf(reminderPrefs.eveningEnabled) }
    var remEveningTime by remember(reminderPrefs) { mutableStateOf(reminderPrefs.eveningTime) }
    var remStatusMsg by remember { mutableStateOf<String?>(null) }

    // Notifications permission launcher (Android 13+)
    val notifPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val updated = ReminderPrefs(remPrePrayer, remMorningOn, remMorningTime, remEveningOn, remEveningTime)
        onSaveReminders(updated)
        NotificationScheduler.rescheduleAll(context, updated, prayerTimes)
        remStatusMsg = if (granted) "تم تفعيل المنبهات وحفظ المواعيد ✓" else "تم حفظ المواعيد — فعّل إذن الإشعارات من إعدادات الجهاز."
    }

    // GPS Location launcher
    var gpsStatus by remember { mutableStateOf<String?>(null) }
    val locPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            runCatching {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                @Suppress("MissingPermission")
                val loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (loc != null) {
                    val tz = TimeZone.getDefault().rawOffset / 3600000.0
                    val custom = CityOption("gps", "موقعك الحالي (GPS)", loc.latitude, loc.longitude, tz)
                    onCityChange(custom)
                    gpsStatus = "تم تحديد موقعك (${"%.2f".format(loc.latitude)}, ${"%.2f".format(loc.longitude)}) ✓"
                } else {
                    gpsStatus = "تعذر التقاط إحداثيات GPS حالياً — اختر مدينتك من القائمة."
                }
            }.onFailure {
                gpsStatus = "تعذر الوصول للموقع — استخدم قائمة المدن."
            }
        } else {
            gpsStatus = "تم رفض إذن الموقع."
        }
    }

    // Live Qibla Compass Sensor
    var compassHeading by remember { mutableFloatStateOf(0f) }
    var sensorActive by remember { mutableStateOf(false) }
    if (subTab == 1) {
        DisposableEffect(context) {
            val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            val gravity = FloatArray(3)
            val geomag = FloatArray(3)
            var hasG = false
            var hasM = false
            val R = FloatArray(9)
            val I = FloatArray(9)
            val orientation = FloatArray(3)

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                        hasG = true
                    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        System.arraycopy(event.values, 0, geomag, 0, 3)
                        hasM = true
                    }
                    if (hasG && hasM && SensorManager.getRotationMatrix(R, I, gravity, geomag)) {
                        SensorManager.getOrientation(R, orientation)
                        val deg = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                        compassHeading = compassHeading + 0.15f * (((deg - compassHeading + 540f) % 360f) - 180f)
                        sensorActive = true
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            if (accel != null && mag != null) {
                sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
                sm.registerListener(listener, mag, SensorManager.SENSOR_DELAY_UI)
            }
            onDispose { sm.unregisterListener(listener) }
        }
    }

    val subTabs = listOf(
        "الصلوات والإحصائيات",
        "القبلة والتقويم",
        "الأحاديث والأسماء",
        "الأذكار (٢٦)",
        "السبحة",
        "الزكاة والختمة",
        "الإعدادات والتنبيهات"
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sub-Tab Selector
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                subTabs.forEachIndexed { idx, label ->
                    FilterChip(
                        selected = subTab == idx,
                        onClick = { subTab = idx },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        }

        // TAB 0: PRAYER TRACKER, RAWATIB, 7-DAY CANVAS CHART & REWARDS
        if (subTab == 0) {
            val todayKey = AppStateStore.todayDateKey(0)
            val todayObj = worshipLogs[todayKey]
            val prayersObj = todayObj?.optJSONObject("prayers")
            val rawObj = todayObj?.optJSONObject("rawatib")
            val fardList = listOf("fajr" to "الفجر", "dhuhr" to "الظهر", "asr" to "العصر", "maghrib" to "المغرب", "isha" to "العشاء")
            val rawatibList = listOf(
                "fajr_before" to "سنة الفجر (٢)",
                "dhuhr_before" to "قبل الظهر (٤)",
                "dhuhr_after" to "بعد الظهر (٢)",
                "maghrib_after" to "بعد المغرب (٢)",
                "isha_after" to "بعد العشاء (٢)"
            )

            // Streak Calculation
            var streak = 0
            for (d in 0 until 30) {
                val dk = AppStateStore.todayDateKey(-d)
                val pObj = worshipLogs[dk]?.optJSONObject("prayers")
                val cnt = fardList.count { (k, _) -> pObj?.optString(k) == "on_time" }
                if (cnt == 5) streak++ else if (d > 0) break
            }

            item {
                Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("متابعة الصلوات الخمس اليوم", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accent)
                            Surface(color = goldAccent.copy(alpha = 0.16f), shape = RoundedCornerShape(10.dp)) {
                                Text("سلسلة الالتزام: $streak أيام 🔥", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = goldAccent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                        fardList.forEach { (k, label) ->
                            val curStatus = prayersObj?.optString(k, "") ?: ""
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(
                                        Triple("on_time", "في وقتها", accent),
                                        Triple("late", "متأخرة", Color(0xFFD97706)),
                                        Triple("missed", "لم تصلَّ", Color(0xFFDC2626))
                                    ).forEach { (stKey, stLabel, stCol) ->
                                        val active = curStatus == stKey
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (active) stCol else stCol.copy(alpha = 0.1f),
                                            modifier = Modifier.clickable { onSetPrayerStatus(todayKey, k, stKey) }
                                        ) {
                                            Text(
                                                stLabel,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (active) Color.White else stCol,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider()
                        Text("السنن الرواتب (١٢ ركعة يومياً)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accent)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            rawatibList.forEach { (rk, rLabel) ->
                                val done = rawObj?.optBoolean(rk, false) == true
                                FilterChip(
                                    selected = done,
                                    onClick = { onToggleRawatib(todayKey, rk) },
                                    label = { Text(if (done) "$rLabel ✓" else rLabel, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // 7-Day Compose Canvas Bar Chart
            item {
                val weekData = (6 downTo 0).map { offset ->
                    val dk = AppStateStore.todayDateKey(-offset)
                    val pObj = worshipLogs[dk]?.optJSONObject("prayers")
                    val onTime = fardList.count { (k, _) -> pObj?.optString(k) == "on_time" }
                    dk.takeLast(5) to onTime
                }
                Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("إحصائيات التزام الأسبوع (٧ أيام)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = accent)
                        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                            val barWidth = (size.width / weekData.size) * 0.5f
                            val step = size.width / weekData.size
                            weekData.forEachIndexed { i, (_, count) ->
                                val barH = (count / 5f) * (size.height - 16f)
                                val x = i * step + (step - barWidth) / 2f
                                drawRoundRect(
                                    color = accent.copy(alpha = 0.14f),
                                    topLeft = Offset(x, 8f),
                                    size = Size(barWidth, size.height - 16f)
                                )
                                if (barH > 0f) {
                                    drawRoundRect(
                                        color = if (count == 5) accent else goldAccent,
                                        topLeft = Offset(x, size.height - 8f - barH),
                                        size = Size(barWidth, barH)
                                    )
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            weekData.forEach { (lbl, cnt) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$cnt/5", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accent)
                                    Text(lbl, fontSize = 9.sp, color = muted)
                                }
                            }
                        }
                    }
                }
            }

            // 8 Achievement Badges
            item {
                val badges = listOf(
                    Triple("المصلي المواظب", "صلاتك في وقتها اليوم", fardList.count { (k, _) -> prayersObj?.optString(k) == "on_time" } >= 5),
                    Triple("صاحب السلسلة", "التزام 3 أيام متواصلة", streak >= 3),
                    Triple("مبتدئ الختمة", "إتمام جزء واحد من القرآن", khatmahJuz.isNotEmpty()),
                    Triple("قارئ النصف", "إتمام ١٥ جزءاً من القرآن", khatmahJuz.size >= 15),
                    Triple("المسبّح الدائم", "١٠٠ تسبيحة في السجل", tasbihLifetime >= 100),
                    Triple("قارئ السير", "قراءة ٥ من قصص الأنبياء", readProphetsCount >= 5),
                    Triple("محب الصحابة", "قراءة ١٠ سير صحابة", readSahabaCount >= 10),
                    Triple("بطل المعرفة", "إتمام ٥ مراحل أو ٥٠٠ XP", quizCompletedCount >= 5 || xp >= 500)
                )
                Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("الأوسمة والمكافآت (${badges.count { it.third }}/${badges.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            badges.forEach { (title, sub, unlocked) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (unlocked) accent.copy(alpha = 0.15f) else muted.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, if (unlocked) accent else muted.copy(alpha = 0.25f))
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (unlocked) goldAccent else muted.copy(alpha = 0.35f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Column {
                                            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (unlocked) accent else muted)
                                            Text(sub, fontSize = 9.sp, color = muted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // TAB 1: LIVE QIBLA COMPASS, 20 CITIES + GPS, HANAFI ASR & HIJRI CALENDAR
        if (subTab == 1) {
            val qiblaBearing = remember(city) { PrayerQiblaMath.qiblaBearing(city.lat, city.lon) }
            val distanceKm = remember(city) { PrayerQiblaMath.distanceToKaabaKm(city.lat, city.lon) }
            val relativeAngle = ((qiblaBearing - compassHeading) + 360.0) % 360.0
            val alignedWithKaaba = abs(((relativeAngle + 180.0) % 360.0) - 180.0) <= 5.0

            item {
                Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.kaaba_qibla_compass),
                                contentDescription = "الكعبة المشرفة",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("بوصلة القبلة الحية", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accent)
                                Text("اتجاه الكعبة: ${qiblaBearing.roundToInt()}° · المسافة: $distanceKm كم", fontSize = 12.sp, color = muted)
                                Text(
                                    if (sensorActive) "مستشعر المغناطيسية نشط ✓" else "حرّك الهاتف أو اضبط الدوران يدوياً",
                                    fontSize = 11.sp,
                                    color = if (sensorActive) accent else goldAccent
                                )
                            }
                        }

                        // Compose Canvas Rotating Qibla Compass
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(210.dp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val r = size.minDimension / 2f - 10f
                                val center = Offset(size.width / 2f, size.height / 2f)
                                drawCircle(
                                    color = accent.copy(alpha = 0.08f),
                                    radius = r,
                                    center = center
                                )
                                drawCircle(
                                    color = if (alignedWithKaaba) accent else goldAccent,
                                    radius = r,
                                    center = center,
                                    style = Stroke(width = 6f)
                                )
                                rotate(degrees = relativeAngle.toFloat(), pivot = center) {
                                    // Kaaba Direction Needle
                                    drawLine(
                                        color = if (alignedWithKaaba) accent else goldAccent,
                                        start = center,
                                        end = Offset(center.x, center.y - r + 18f),
                                        strokeWidth = 10f,
                                        cap = StrokeCap.Round
                                    )
                                    drawCircle(
                                        color = Color(0xFF111827),
                                        radius = 16f,
                                        center = Offset(center.x, center.y - r + 20f)
                                    )
                                    drawCircle(
                                        color = goldAccent,
                                        radius = 16f,
                                        center = Offset(center.x, center.y - r + 20f),
                                        style = Stroke(width = 3f)
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🕋", fontSize = 24.sp)
                                Text("${qiblaBearing.roundToInt()}°", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = accent)
                            }
                        }

                        if (alignedWithKaaba) {
                            Surface(color = accent, shape = RoundedCornerShape(10.dp)) {
                                Text("أنت متجه نحو القبلة تماماً ✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
                            }
                        }

                        Text("معايرة يدوية لاتجاه الشمال (${compassHeading.roundToInt()}°)", fontSize = 11.sp, color = muted)
                        Slider(
                            value = compassHeading,
                            onValueChange = { compassHeading = it },
                            valueRange = 0f..360f
                        )
                    }
                }
            }

            // City Selector, GPS & Hanafi Asr
            item {
                Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("الموقع ومواقيت الصلاة (${city.nameAr})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accent)
                            OutlinedButton(onClick = {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    locPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                } else {
                                    locPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                }
                            }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("تحديد GPS", fontSize = 11.sp)
                            }
                        }
                        gpsStatus?.let { Text(it, fontSize = 11.sp, color = accent) }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("المذهب الحنفي لصلاة العصر", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Switch(checked = hanafiAsr, onCheckedChange = onHanafiChange)
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PrayerQiblaMath.PRESET_CITIES.forEach { c ->
                                FilterChip(
                                    selected = city.id == c.id,
                                    onClick = { onCityChange(c) },
                                    label = { Text(c.nameAr, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // Monthly Hijri & Gregorian Calendar
            item {
                val monthCal = remember(calMonthOffset) {
                    Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        add(Calendar.MONTH, calMonthOffset)
                    }
                }
                val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val firstHijri = PrayerQiblaMath.gregorianToHijri(monthCal)
                val todayCal = Calendar.getInstance()
                val isCurrentMonth = monthCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    monthCal.get(Calendar.MONTH) == todayCal.get(Calendar.MONTH)
                val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

                Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { calMonthOffset-- }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "السابق")
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${firstHijri.monthNameAr} ${firstHijri.year} هـ", fontWeight = FontWeight.Bold, color = accent, fontSize = 15.sp)
                                Text(
                                    "${monthCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("ar"))} ${monthCal.get(Calendar.YEAR)} م",
                                    fontSize = 12.sp,
                                    color = muted
                                )
                            }
                            IconButton(onClick = { calMonthOffset++ }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "التالي")
                            }
                        }
                        val weeks = (1..daysInMonth).toList().chunked(7)
                        weeks.forEach { week ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                week.forEach { d ->
                                    val cDay = (monthCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, d) }
                                    val hInfo = PrayerQiblaMath.gregorianToHijri(cDay)
                                    val isToday = isCurrentMonth && d == todayDay
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isToday) accent else if (darkMode) NoorColors.DarkSurfaceAlt else NoorColors.Parchment,
                                        border = cardBorder(darkMode),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 6.dp)) {
                                            Text("${hInfo.day}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isToday) Color.White else accent)
                                            Text("$d م", fontSize = 9.sp, color = if (isToday) Color.White.copy(alpha = 0.85f) else muted)
                                        }
                                    }
                                }
                                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }

        // TAB 2: HADITHS (54) & 99 NAMES OF ALLAH
        if (subTab == 2) {
            item {
                Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { treasureMode = 0 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (treasureMode == 0) accent else accent.copy(alpha = 0.14f),
                                    contentColor = if (treasureMode == 0) Color.White else accent
                                ),
                                modifier = Modifier.weight(1f)
                            ) { Text("الأحاديث (${hadiths.size})", fontSize = 12.sp) }
                            Button(
                                onClick = { treasureMode = 1 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (treasureMode == 1) accent else accent.copy(alpha = 0.14f),
                                    contentColor = if (treasureMode == 1) Color.White else accent
                                ),
                                modifier = Modifier.weight(1f)
                            ) { Text("الأسماء الحسنى (${names99.size})", fontSize = 12.sp) }
                        }
                        copiedBanner?.let {
                            Text(it, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (treasureMode == 0) {
                            OutlinedTextField(
                                value = hadithQuery,
                                onValueChange = { hadithQuery = it },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                placeholder = { Text("ابحث في الأحاديث النبوية...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            val cats = listOf("الكل") + hadiths.map { it.category }.distinct()
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                cats.forEach { cat ->
                                    FilterChip(selected = hadithCategory == cat, onClick = { hadithCategory = cat }, label = { Text(cat, fontSize = 11.sp) })
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = nameQuery,
                                onValueChange = { nameQuery = it },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                placeholder = { Text("ابحث في أسماء الله الحسنى...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            if (treasureMode == 0) {
                val filteredHadiths = hadiths.filter {
                    (hadithCategory == "الكل" || it.category == hadithCategory) &&
                        (hadithQuery.isBlank() ||
                            QuranRepository.normalizeArabic(it.title).contains(QuranRepository.normalizeArabic(hadithQuery)) ||
                            QuranRepository.normalizeArabic(it.text).contains(QuranRepository.normalizeArabic(hadithQuery)))
                }
                items(filteredHadiths, key = { it.id }) { h ->
                    val speakingThis = speakingHadithId == h.id
                    Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(h.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = accent, modifier = Modifier.weight(1f))
                                Surface(color = goldAccent.copy(alpha = 0.16f), shape = RoundedCornerShape(8.dp)) {
                                    Text(h.category, fontSize = 10.sp, color = goldAccent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                            Text(h.text, fontSize = 15.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium)
                            Text("الراوي: ${h.narrator} · المصدر: ${h.source}", fontSize = 11.sp, color = muted)
                            if (h.lesson.isNotBlank()) {
                                Surface(color = accent.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp)) {
                                    Text("الفائدة: ${h.lesson}", fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    if (speakingThis) {
                                        tts.stop(); speakingHadithId = null
                                    } else {
                                        speakingHadithId = h.id
                                        tts.speak(h.text, TextToSpeech.QUEUE_FLUSH, null, "hadith_${h.id}")
                                    }
                                }) {
                                    Icon(if (speakingThis) Icons.Default.Stop else Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (speakingThis) "إيقاف" else "استماع", fontSize = 11.sp)
                                }
                                OutlinedButton(onClick = {
                                    clipboard.setText(AnnotatedString("${h.title}\n${h.text}\n(${h.source})"))
                                    copiedBanner = "تم نسخ حديث \"${h.title}\" ✓"
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("نسخ", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                val filteredNames = names99.filter {
                    nameQuery.isBlank() ||
                        QuranRepository.normalizeArabic(it.name).contains(QuranRepository.normalizeArabic(nameQuery)) ||
                        QuranRepository.normalizeArabic(it.meaning).contains(QuranRepository.normalizeArabic(nameQuery))
                }
                items(filteredNames.chunked(2)) { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { n ->
                            Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode), modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("${n.number}. ${n.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accent)
                                    Text(n.meaning, fontSize = 12.sp, color = muted, lineHeight = 18.sp)
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // TAB 3: ADHKAR (7 CATEGORIES)
        if (subTab == 3) {
            item {
                val cats = remember(adhkar) { adhkar.map { it.category }.distinct() }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    cats.forEach { c ->
                        FilterChip(selected = adhkarCat == c, onClick = { adhkarCat = c }, label = { Text(c, fontSize = 11.sp) })
                    }
                }
            }
            val list = adhkar.filter { it.category == adhkarCat }
            items(list, key = { it.id }) { d ->
                val cur = adhkarCounts[d.id] ?: 0
                val done = cur >= d.targetCount
                Card(
                    shape = CardShape,
                    border = if (done) BorderStroke(1.5.dp, accent) else cardBorder(darkMode),
                    colors = surfaceCardColors(darkMode),
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (!done) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onIncrementDhikr(d.id, d.targetCount)
                        }
                    }
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(d.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accent)
                            Surface(color = if (done) accent else goldAccent.copy(alpha = 0.16f), shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    "$cur / ${d.targetCount}" + if (done) " ✓" else "",
                                    color = if (done) Color.White else goldAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(d.text, fontSize = 15.sp, lineHeight = 26.sp)
                        if (d.virtue.isNotBlank()) {
                            Text("الفضل: ${d.virtue}", fontSize = 11.sp, color = muted)
                        }
                        LinearProgressIndicator(
                            progress = { (cur.toFloat() / d.targetCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // TAB 4: ELECTRONIC TASBIH WITH CANVAS BEAD RING & CUSTOM PHRASES
        if (subTab == 4) {
            item {
                val activePhrase = customTasbihPhrases.getOrElse(selectedPhraseIndex) { CustomTasbihPhrase("سبحان الله", 33) }
                val progress = (tasbihCount.toFloat() / tasbihTarget.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("السبحة الإلكترونية التفاعلية", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = accent)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            customTasbihPhrases.forEachIndexed { idx, p ->
                                FilterChip(
                                    selected = selectedPhraseIndex == idx,
                                    onClick = {
                                        selectedPhraseIndex = idx
                                        onTasbihTargetChange(p.target)
                                    },
                                    label = { Text(p.text, fontSize = 11.sp) }
                                )
                            }
                        }

                        // Circular Tactile Bead Ring Canvas
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(220.dp).clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTasbihTap()
                            }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 16f
                                val r = size.minDimension / 2f - stroke
                                val center = Offset(size.width / 2f, size.height / 2f)
                                drawCircle(
                                    color = accent.copy(alpha = 0.14f),
                                    radius = r,
                                    center = center,
                                    style = Stroke(width = stroke)
                                )
                                drawArc(
                                    color = if (tasbihCount >= tasbihTarget) goldAccent else accent,
                                    startAngle = -90f,
                                    sweepAngle = 360f * progress,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                                // 33 decorative beads along circle
                                for (i in 0 until 33) {
                                    val ang = Math.toRadians((i * (360.0 / 33.0)) - 90.0)
                                    val bx = center.x + (r - 18f) * cos(ang).toFloat()
                                    val by = center.y + (r - 18f) * sin(ang).toFloat()
                                    val lit = (i.toFloat() / 33f) <= progress && tasbihCount > 0
                                    drawCircle(
                                        color = if (lit) goldAccent else accent.copy(alpha = 0.25f),
                                        radius = 4f,
                                        center = Offset(bx, by)
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                Text(activePhrase.text, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center, color = accent, maxLines = 2)
                                Text("$tasbihCount", fontWeight = FontWeight.ExtraBold, fontSize = 42.sp)
                                Text("من $tasbihTarget · اضغط للتسبيح", fontSize = 11.sp, color = muted)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            listOf(33, 99, 100).forEach { tgt ->
                                FilterChip(selected = tasbihTarget == tgt, onClick = { onTasbihTargetChange(tgt) }, label = { Text("$tgt") })
                            }
                            OutlinedButton(onClick = onTasbihReset) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("تصفير", fontSize = 12.sp)
                            }
                        }
                        Text("إجمالي التسبيحات الكلي: $tasbihLifetime تسبيحة", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = goldAccent)

                        HorizontalDivider()
                        Text("إضافة ذكر مخصص", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newPhraseText,
                                onValueChange = { newPhraseText = it },
                                label = { Text("الذكر") },
                                singleLine = true,
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedTextField(
                                value = newPhraseTarget,
                                onValueChange = { newPhraseTarget = it.filter { c -> c.isDigit() } },
                                label = { Text("الهدف") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                if (newPhraseText.isNotBlank()) {
                                    val t = newPhraseTarget.toIntOrNull()?.coerceIn(1, 1000) ?: 33
                                    onAddTasbihPhrase(CustomTasbihPhrase(newPhraseText.trim(), t))
                                    newPhraseText = ""
                                }
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "إضافة", tint = accent)
                            }
                        }
                    }
                }
            }
        }

        // TAB 5: ZAKAT CALCULATOR & 30-JUZ KHATMAH TRACKER
        if (subTab == 5) {
            item {
                val cash = zCash.toDoubleOrNull() ?: 0.0
                val gVal = (zGoldGrams.toDoubleOrNull() ?: 0.0) * (zGoldPrice.toDoubleOrNull() ?: 0.0)
                val sVal = (zSilverGrams.toDoubleOrNull() ?: 0.0) * (zSilverPrice.toDoubleOrNull() ?: 0.0)
                val inv = zInvestments.toDoubleOrNull() ?: 0.0
                val debts = zDebts.toDoubleOrNull() ?: 0.0
                val netWealth = (cash + gVal + sVal + inv - debts).coerceAtLeast(0.0)
                val nisab = if (zNisabStd == "gold") 85.0 * (zGoldPrice.toDoubleOrNull() ?: 0.0) else 595.0 * (zSilverPrice.toDoubleOrNull() ?: 0.0)
                val eligible = nisab > 0 && netWealth >= nisab
                val zakatDue = if (eligible) netWealth * 0.025 else 0.0

                Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("حاسبة الزكاة الشرعية (٢.٥٪)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accent)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = zNisabStd == "gold", onClick = { zNisabStd = "gold" }, label = { Text("نصاب الذهب (٨٥ جم)") })
                            FilterChip(selected = zNisabStd == "silver", onClick = { zNisabStd = "silver" }, label = { Text("نصاب الفضة (٥٩٥ جم)") })
                        }
                        ZakatField("النقد والمدخرات", zCash) { zCash = it }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ZakatField("الذهب (جرام)", zGoldGrams, Modifier.weight(1f)) { zGoldGrams = it }
                            ZakatField("سعر الجرام", zGoldPrice, Modifier.weight(1f)) { zGoldPrice = it }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ZakatField("الفضة (جرام)", zSilverGrams, Modifier.weight(1f)) { zSilverGrams = it }
                            ZakatField("سعر الجرام", zSilverPrice, Modifier.weight(1f)) { zSilverPrice = it }
                        }
                        ZakatField("الأسهم وعروض التجارة", zInvestments) { zInvestments = it }
                        ZakatField("الديون المستحقة عليك (تُخصم)", zDebts) { zDebts = it }

                        Surface(
                            color = if (eligible) accent.copy(alpha = 0.14f) else muted.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("صافي الوعاء الزكوي: ${"%,.0f".format(netWealth)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("حد النصاب الحالي: ${"%,.0f".format(nisab)}", fontSize = 12.sp, color = muted)
                                Text(
                                    if (eligible) "الزكاة الواجبة (٢.٥٪): ${"%,.2f".format(zakatDue)}" else "لم يبلغ المال النصاب الشرعي بعد",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = if (eligible) accent else muted
                                )
                            }
                        }
                    }
                }
            }

            // 30-Juz Khatmah Grid
            item {
                Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("خطة ختم القرآن الكريم (${khatmahJuz.size} / ٣٠ جزءاً)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accent)
                        LinearProgressIndicator(progress = { khatmahJuz.size / 30f }, modifier = Modifier.fillMaxWidth())
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..30).forEach { j ->
                                val done = khatmahJuz.contains(j)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (done) accent else if (darkMode) NoorColors.DarkSurfaceAlt else NoorColors.Parchment,
                                    border = cardBorder(darkMode),
                                    modifier = Modifier.clickable { onToggleKhatmahJuz(j) }
                                ) {
                                    Text(
                                        if (done) "ج$j ✓" else "جزء $j",
                                        color = if (done) Color.White else if (darkMode) NoorColors.DarkInk else NoorColors.InkLight,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // TAB 6: NOTIFICATIONS & REMINDER SETTINGS
        if (subTab == 6) {
            item {
                Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("جدولة تنبيهات الصلاة والأذكار", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accent)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("التنبيه قبل الصلاة بـ ٣٠ دقيقة", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("تذكير يومي قبل كل صلاة بنصف ساعة عبر منبه النظام", fontSize = 11.sp, color = muted)
                            }
                            Switch(checked = remPrePrayer, onCheckedChange = { remPrePrayer = it })
                        }
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("تنبيه أذكار الصباح", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Switch(checked = remMorningOn, onCheckedChange = { remMorningOn = it })
                        }
                        if (remMorningOn) {
                            OutlinedTextField(
                                value = remMorningTime,
                                onValueChange = { remMorningTime = it },
                                label = { Text("وقت أذكار الصباح (مثال 06:30)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("تنبيه أذكار المساء", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Switch(checked = remEveningOn, onCheckedChange = { remEveningOn = it })
                        }
                        if (remEveningOn) {
                            OutlinedTextField(
                                value = remEveningTime,
                                onValueChange = { remEveningTime = it },
                                label = { Text("وقت أذكار المساء (مثال 17:00)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    val updated = ReminderPrefs(remPrePrayer, remMorningOn, remMorningTime, remEveningOn, remEveningTime)
                                    onSaveReminders(updated)
                                    NotificationScheduler.rescheduleAll(context, updated, prayerTimes)
                                    remStatusMsg = "تم حفظ وجدولة جميع المنبهات بنجاح ✓"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("حفظ وتفعيل المنبهات")
                        }

                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    PrayerAlarmReceiver.showNotification(
                                        context,
                                        "نور الإسلام — إشعار تجريبي",
                                        "تعمل التنبيهات المحلية بنجاح على جهازك ✓",
                                        9991
                                    )
                                    remStatusMsg = "تم إرسال إشعار تجريبي لشريط الإشعارات ✓"
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إرسال إشعار تجريبي الآن")
                        }

                        remStatusMsg?.let { Text(it, fontSize = 12.sp, color = accent, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZakatField(
    label: String,
    value: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { s -> onChange(s.filter { it.isDigit() || it == '.' }) },
        label = { Text(label, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
    )
}
