package com.nooralislam.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nooralislam.app.R
import com.nooralislam.app.data.CityOption
import com.nooralislam.app.data.PrayerTimesModel
import com.nooralislam.app.ui.CardShape
import com.nooralislam.app.ui.NoorColors
import com.nooralislam.app.ui.cardBorder
import com.nooralislam.app.ui.surfaceCardColors
import com.nooralislam.app.utils.PrayerQiblaMath
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    darkMode: Boolean,
    city: CityOption,
    prayerTimes: PrayerTimesModel,
    todayLog: JSONObject?,
    khatmahCount: Int,
    xp: Int,
    quizCompletedCount: Int,
    readProphetsCount: Int,
    readSahabaCount: Int,
    tasbihLifetime: Int,
    onTogglePrayerOnTime: (String) -> Unit,
    onNavigateTab: (Int) -> Unit,
    onOpenWorshipSubTab: (Int) -> Unit
) {
    var nowCal by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowCal = Calendar.getInstance()
            delay(1000L)
        }
    }

    val hijri = remember(nowCal.get(Calendar.DAY_OF_YEAR)) { PrayerQiblaMath.gregorianToHijri(nowCal) }
    val gregFormatted = remember(nowCal.get(Calendar.DAY_OF_YEAR)) {
        SimpleDateFormat("d MMMM yyyy", Locale("ar")).format(nowCal.time)
    }
    val nextPrayer = PrayerQiblaMath.getNextPrayer(prayerTimes, nowCal)
    val qiblaBearing = remember(city.id) { PrayerQiblaMath.qiblaBearing(city.lat, city.lon).toInt() }

    val prayersObj = todayLog?.optJSONObject("prayers")
    val fivePrayers = listOf(
        "fajr" to "الفجر",
        "dhuhr" to "الظهر",
        "asr" to "العصر",
        "maghrib" to "المغرب",
        "isha" to "العشاء"
    )
    val onTimeCount = fivePrayers.count { (k, _) -> prayersObj?.optString(k) == "on_time" }

    val accent = if (darkMode) NoorColors.EmeraldGlow else NoorColors.Emerald
    val goldAccent = if (darkMode) NoorColors.GoldLight else NoorColors.Gold
    val mutedColor = if (darkMode) NoorColors.DarkMuted else NoorColors.MutedLight

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner with Mosque Image
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = cardBorder(darkMode),
                colors = surfaceCardColors(darkMode)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.hero_mosque_arch),
                            contentDescription = "مسجد نور الإسلام",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xBB0B3A30),
                                            Color(0xEA0F5E4D)
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = "${hijri.formattedAr} · $gregFormatted · ${city.nameAr}",
                                    color = Color(0xFFFDE68A),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "رفيقك اليومي في رحاب القرآن الكريم والذكر وسير الأنبياء والصحابة",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 25.sp
                                )
                                Surface(
                                    color = Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("الصلاة القادمة", color = Color(0xFFE5DEC9), fontSize = 10.sp)
                                            Text(
                                                "${nextPrayer.nameAr} — ${PrayerQiblaMath.formatTime(nextPrayer.timeHours)}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("الوقت المتبقي", color = Color(0xFFE5DEC9), fontSize = 10.sp)
                                            Text(
                                                PrayerQiblaMath.formatCountdown(nextPrayer.remainingSeconds),
                                                color = Color(0xFFFDE68A),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6 Prayer Time Chips
                    val allTimes = listOf(
                        "الفجر" to prayerTimes.fajr,
                        "الشروق" to prayerTimes.sunrise,
                        "الظهر" to prayerTimes.dhuhr,
                        "العصر" to prayerTimes.asr,
                        "المغرب" to prayerTimes.maghrib,
                        "العشاء" to prayerTimes.isha
                    )
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allTimes.forEach { (label, hrs) ->
                            val isNext = label == nextPrayer.nameAr
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isNext) accent.copy(alpha = 0.14f) else if (darkMode) NoorColors.DarkSurfaceAlt else NoorColors.Parchment,
                                border = BorderStroke(1.dp, if (isNext) accent else if (darkMode) NoorColors.DarkBorder else NoorColors.BorderLight),
                                modifier = Modifier.clickable { onOpenWorshipSubTab(1) }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(label, fontSize = 11.sp, color = if (isNext) accent else mutedColor, fontWeight = FontWeight.Bold)
                                    Text(PrayerQiblaMath.formatTime(hrs), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Verse of the day
                    Surface(
                        color = if (darkMode) NoorColors.DarkSurfaceAlt else Color(0xFFFCF8EE),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("آية اليوم وتدبر", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = goldAccent)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "﴿الَّذِينَ آمَنُوا وَتَطْمَئِنُّ قُلُوبُهُم بِذِكْرِ اللَّهِ ۗ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ﴾",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 26.sp
                            )
                            Text("سورة الرعد · الآية ٢٨", fontSize = 11.sp, color = mutedColor, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }

        // Daily Persistent 5-Prayer Challenge
        item {
            Card(
                shape = CardShape,
                border = cardBorder(darkMode),
                colors = surfaceCardColors(darkMode)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("التحدي اليومي — الصلوات الخمس في وقتها", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("سجّل صلواتك واجمع النقاط وتابع السنن الرواتب ($onTimeCount / 5)", fontSize = 12.sp, color = mutedColor)
                        }
                        OutlinedButton(
                            onClick = { onOpenWorshipSubTab(0) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("الإحصائيات", fontSize = 11.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        fivePrayers.forEach { (key, label) ->
                            val isOnTime = prayersObj?.optString(key) == "on_time"
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isOnTime) accent else if (darkMode) NoorColors.DarkSurfaceAlt else NoorColors.Parchment,
                                border = BorderStroke(1.dp, if (isOnTime) accent else if (darkMode) NoorColors.DarkBorder else NoorColors.BorderLight),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onTogglePrayerOnTime(key) }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                                ) {
                                    if (isOnTime) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                    Text(
                                        label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOnTime) Color.White else if (darkMode) NoorColors.DarkInk else NoorColors.InkLight,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Header
        item {
            Text("أقسام الموسوعة الإسلامية", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }

        // Bento Cards Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BentoImageCard(
                    darkMode = darkMode,
                    tag = "١١٤ سورة · ٦٠٤ صفحات · ٣٦ قارئاً",
                    title = "القرآن والتفسير والتحفيظ والتسميع",
                    desc = "مصحف صفحة بصفحة، التفسير الميسر صوتياً، تكرار الحفظ، تنزيل التلاوات بدون نت، والتسميع بالمايك.",
                    footer = "تقدم الختمة: $khatmahCount / ٣٠ جزءاً",
                    action = "فتح المصحف",
                    imageRes = R.drawable.quran_manuscript_art,
                    onClick = { onNavigateTab(1) }
                )
                BentoImageCard(
                    darkMode = darkMode,
                    tag = "زاوية القبلة $qiblaBearing° · بوصلة حية",
                    title = "القبلة والمواقيت والتقويم الهجري",
                    desc = "بوصلة قبلة حية بمستشعر الهاتف، مواقيت الصلاة بـ GPS أو ٢١ مدينة، وتقويم هجري وميلادي.",
                    footer = hijri.formattedAr,
                    action = "فتح القبلة والمواقيت",
                    imageRes = R.drawable.kaaba_qibla_compass,
                    onClick = { onOpenWorshipSubTab(1) }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BentoCompactCard(
                        modifier = Modifier.weight(1f),
                        darkMode = darkMode,
                        tag = "١٠٠ مرحلة · ٥٠٠ سؤال",
                        title = "المسابقات والألعاب",
                        desc = "١٠٠ مرحلة، تحدي ٦٠ ثانية، أكمل الآية ومن القائل، وتحدي الأصدقاء.",
                        footer = "المكتمل: $quizCompletedCount/١٠٠ · $xp XP",
                        onClick = { onNavigateTab(3) }
                    )
                    BentoCompactCard(
                        modifier = Modifier.weight(1f),
                        darkMode = darkMode,
                        tag = "٢٥ نبياً · ٥٥ صحابياً",
                        title = "القصص والسير",
                        desc = "قصص جميع الأنبياء كاملةً وسير ٥٥ صحابياً بدون اختصار.",
                        footer = "قرأت $readProphetsCount نبياً · $readSahabaCount صحابياً",
                        onClick = { onNavigateTab(2) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BentoCompactCard(
                        modifier = Modifier.weight(1f),
                        darkMode = darkMode,
                        tag = "٧ أقسام · سبحة تفاعلية",
                        title = "الأذكار والسبحة",
                        desc = "أذكار الصباح والمساء والرقية وسبحة إلكترونية.",
                        footer = "التسبيحات: $tasbihLifetime",
                        onClick = { onOpenWorshipSubTab(3) }
                    )
                    BentoCompactCard(
                        modifier = Modifier.weight(1f),
                        darkMode = darkMode,
                        tag = "٥٤ حديثاً · ٩٩ اسماً · زكاة",
                        title = "الأحاديث والزكاة",
                        desc = "موسوعة الأحاديث، الأسماء الحسنى، وحاسبة الزكاة.",
                        footer = "استكشاف الكنوز",
                        onClick = { onOpenWorshipSubTab(2) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BentoImageCard(
    darkMode: Boolean,
    tag: String,
    title: String,
    desc: String,
    footer: String,
    action: String,
    imageRes: Int,
    onClick: () -> Unit
) {
    val accent = if (darkMode) NoorColors.EmeraldGlow else NoorColors.Emerald
    val muted = if (darkMode) NoorColors.DarkMuted else NoorColors.MutedLight
    Card(
        shape = CardShape,
        border = cardBorder(darkMode),
        colors = surfaceCardColors(darkMode),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tag, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accent)
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(desc, fontSize = 12.sp, color = muted, lineHeight = 18.sp)
                }
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            HorizontalDivider(color = if (darkMode) NoorColors.DarkBorder else NoorColors.BorderLight)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(footer, fontSize = 12.sp, color = muted)
                Text(action, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent)
            }
        }
    }
}

@Composable
private fun BentoCompactCard(
    modifier: Modifier,
    darkMode: Boolean,
    tag: String,
    title: String,
    desc: String,
    footer: String,
    onClick: () -> Unit
) {
    val accent = if (darkMode) NoorColors.EmeraldGlow else NoorColors.Emerald
    val muted = if (darkMode) NoorColors.DarkMuted else NoorColors.MutedLight
    Card(
        shape = CardShape,
        border = cardBorder(darkMode),
        colors = surfaceCardColors(darkMode),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tag, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(desc, fontSize = 11.sp, color = muted, lineHeight = 16.sp, minLines = 2, maxLines = 3)
            Spacer(Modifier.height(2.dp))
            Text(footer, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
