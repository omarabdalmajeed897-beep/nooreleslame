package com.nooralislam.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nooralislam.app.audio.OfflineReciterManager
import com.nooralislam.app.data.AppStateStore
import com.nooralislam.app.data.CityOption
import com.nooralislam.app.data.QuranRepository
import com.nooralislam.app.notifications.NotificationScheduler
import com.nooralislam.app.ui.NoorColors
import com.nooralislam.app.ui.NoorTheme
import com.nooralislam.app.ui.screens.GamesScreen
import com.nooralislam.app.ui.screens.HomeScreen
import com.nooralislam.app.ui.screens.QuranScreen
import com.nooralislam.app.ui.screens.StoriesScreen
import com.nooralislam.app.ui.screens.WorshipScreen
import com.nooralislam.app.utils.PrayerQiblaMath
import kotlinx.coroutines.delay
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                NoorAlIslamRootApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoorAlIslamRootApp() {
    val context = LocalContext.current
    val store = remember { AppStateStore(context) }
    val repository = remember { QuranRepository(context) }
    val reciterManager = remember { OfflineReciterManager(context) }

    var darkMode by remember { mutableStateOf(store.isDarkMode()) }
    var activeTab by remember { mutableIntStateOf(0) } // 0=Home, 1=Quran, 2=Stories, 3=Games, 4=Worship
    var worshipSubTab by remember { mutableIntStateOf(0) }

    var customGpsCity by remember { mutableStateOf<CityOption?>(null) }
    var cityId by remember { mutableStateOf(store.getCityId()) }
    val activeCity = customGpsCity ?: PrayerQiblaMath.PRESET_CITIES.firstOrNull { it.id == cityId } ?: PrayerQiblaMath.PRESET_CITIES.first()

    var hanafiAsr by remember { mutableStateOf(store.isHanafiAsr()) }
    var activeSurahId by remember { mutableIntStateOf(store.getActiveSurahId()) }
    var activeReciterId by remember { mutableStateOf(store.getActiveReciterId()) }
    var fontSize by remember { mutableIntStateOf(store.getFontSize()) }

    var xp by remember { mutableIntStateOf(store.getXp()) }
    var speedBestScore by remember { mutableIntStateOf(store.getSpeedBestScore()) }
    var quizProgress by remember { mutableStateOf(store.getQuizProgress()) }
    var khatmahJuz by remember { mutableStateOf(store.getKhatmahJuz()) }
    var readProphets by remember { mutableStateOf(store.getReadProphets()) }
    var readSahaba by remember { mutableStateOf(store.getReadSahaba()) }
    var bookmarks by remember { mutableStateOf(store.getBookmarks()) }
    var worshipLogs by remember { mutableStateOf(store.getAllWorshipLogs()) }
    var adhkarCounts by remember { mutableStateOf(store.getAdhkarCounts()) }

    var tasbihCount by remember { mutableIntStateOf(store.getTasbihCount()) }
    var tasbihTarget by remember { mutableIntStateOf(store.getTasbihTarget()) }
    var tasbihLifetime by remember { mutableIntStateOf(store.getTasbihLifetime()) }
    var customTasbihPhrases by remember { mutableStateOf(store.getCustomTasbihPhrases()) }
    var reminderPrefs by remember { mutableStateOf(store.getReminderPrefs()) }

    var nowCal by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowCal = Calendar.getInstance()
            delay(1000L)
        }
    }

    val prayerTimes = remember(activeCity.id, hanafiAsr, nowCal.get(Calendar.DAY_OF_YEAR)) {
        PrayerQiblaMath.calculateTimes(activeCity.lat, activeCity.lon, activeCity.tzOffset, hanafiAsr, nowCal)
    }
    val nextPrayer = PrayerQiblaMath.getNextPrayer(prayerTimes, nowCal)

    NoorTheme(darkMode = darkMode) {
        val accent = if (darkMode) NoorColors.EmeraldGlow else NoorColors.Emerald
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Image(
                                painter = painterResource(R.drawable.noor_logo),
                                contentDescription = "شعار نور الإسلام",
                                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Column {
                                Text("نور الإسلام", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text(activeCity.nameAr, fontSize = 10.sp, color = if (darkMode) NoorColors.DarkMuted else NoorColors.MutedLight)
                            }
                        }
                    },
                    actions = {
                        Surface(
                            color = accent.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clickable {
                                    worshipSubTab = 1
                                    activeTab = 4
                                }
                        ) {
                            Text(
                                "${nextPrayer.nameAr} ${PrayerQiblaMath.formatCountdown(nextPrayer.remainingSeconds)}",
                                color = accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                        IconButton(onClick = {
                            darkMode = !darkMode
                            store.setDarkMode(darkMode)
                        }) {
                            Icon(
                                if (darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "الوضع الليلي"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (darkMode) NoorColors.DarkSurface else Color.White
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = if (darkMode) NoorColors.DarkSurface else Color.White
                ) {
                    val items = listOf(
                        Triple(0, "الرئيسية", Icons.Default.Home),
                        Triple(1, "القرآن", Icons.Default.MenuBook),
                        Triple(2, "القصص", Icons.Default.AutoStories),
                        Triple(3, "المسابقة", Icons.Default.EmojiEvents),
                        Triple(4, "العبادات", Icons.Default.Explore)
                    )
                    items.forEach { (idx, label, icon) ->
                        NavigationBarItem(
                            selected = activeTab == idx,
                            onClick = { activeTab = idx },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                when (activeTab) {
                    0 -> {
                        val todayKey = AppStateStore.todayDateKey(0)
                        HomeScreen(
                            darkMode = darkMode,
                            city = activeCity,
                            prayerTimes = prayerTimes,
                            todayLog = worshipLogs[todayKey],
                            khatmahCount = khatmahJuz.size,
                            xp = xp,
                            quizCompletedCount = quizProgress.count { it.value >= 1 },
                            readProphetsCount = readProphets.size,
                            readSahabaCount = readSahaba.size,
                            tasbihLifetime = tasbihLifetime,
                            onTogglePrayerOnTime = { prayerKey ->
                                val cur = worshipLogs[todayKey]?.optJSONObject("prayers")?.optString(prayerKey, "")
                                val next = if (cur == "on_time") "none" else "on_time"
                                worshipLogs = store.setPrayerStatus(todayKey, prayerKey, next)
                            },
                            onNavigateTab = { tab -> activeTab = tab },
                            onOpenWorshipSubTab = { sub ->
                                worshipSubTab = sub
                                activeTab = 4
                            }
                        )
                    }
                    1 -> {
                        QuranScreen(
                            darkMode = darkMode,
                            repository = repository,
                            reciterManager = reciterManager,
                            activeSurahId = activeSurahId,
                            activeReciterId = activeReciterId,
                            fontSize = fontSize,
                            khatmahJuz = khatmahJuz,
                            bookmarks = bookmarks,
                            onSelectSurah = {
                                activeSurahId = it
                                store.setActiveSurahId(it)
                            },
                            onSelectReciter = {
                                activeReciterId = it
                                store.setActiveReciterId(it)
                            },
                            onFontSizeChange = {
                                fontSize = it
                                store.setFontSize(it)
                            },
                            onToggleKhatmahJuz = { khatmahJuz = store.toggleKhatmahJuz(it) },
                            onToggleBookmark = { bookmarks = store.toggleBookmark(it) },
                            onAwardXp = { xp = store.addXp(it) }
                        )
                    }
                    2 -> {
                        StoriesScreen(
                            darkMode = darkMode,
                            repository = repository,
                            readProphets = readProphets,
                            readSahaba = readSahaba,
                            onToggleReadProphet = { readProphets = store.toggleReadProphet(it) },
                            onToggleReadSahabi = { readSahaba = store.toggleReadSahabi(it) }
                        )
                    }
                    3 -> {
                        GamesScreen(
                            darkMode = darkMode,
                            repository = repository,
                            xp = xp,
                            quizProgress = quizProgress,
                            speedBestScore = speedBestScore,
                            onStageComplete = { stNum, stars, earnedXp ->
                                quizProgress = store.saveQuizStageResult(stNum, stars)
                                xp = store.addXp(earnedXp)
                            },
                            onAwardXp = { xp = store.addXp(it) },
                            onSpeedBestUpdate = {
                                store.setSpeedBestScore(it)
                                speedBestScore = store.getSpeedBestScore()
                            }
                        )
                    }
                    4 -> {
                        WorshipScreen(
                            darkMode = darkMode,
                            initialSubTab = worshipSubTab,
                            repository = repository,
                            city = activeCity,
                            hanafiAsr = hanafiAsr,
                            prayerTimes = prayerTimes,
                            worshipLogs = worshipLogs,
                            adhkarCounts = adhkarCounts,
                            tasbihCount = tasbihCount,
                            tasbihTarget = tasbihTarget,
                            tasbihLifetime = tasbihLifetime,
                            customTasbihPhrases = customTasbihPhrases,
                            khatmahJuz = khatmahJuz,
                            reminderPrefs = reminderPrefs,
                            xp = xp,
                            quizCompletedCount = quizProgress.count { it.value >= 1 },
                            readProphetsCount = readProphets.size,
                            readSahabaCount = readSahaba.size,
                            onSetPrayerStatus = { dk, pk, st ->
                                worshipLogs = store.setPrayerStatus(dk, pk, st)
                            },
                            onToggleRawatib = { dk, rk ->
                                worshipLogs = store.toggleRawatib(dk, rk)
                            },
                            onCityChange = { chosen ->
                                if (chosen.id == "gps") {
                                    customGpsCity = chosen
                                } else {
                                    customGpsCity = null
                                    cityId = chosen.id
                                    store.setCityId(chosen.id)
                                }
                                val times = PrayerQiblaMath.calculateTimes(chosen.lat, chosen.lon, chosen.tzOffset, hanafiAsr)
                                NotificationScheduler.rescheduleAll(context, reminderPrefs, times)
                            },
                            onHanafiChange = { h ->
                                hanafiAsr = h
                                store.setHanafiAsr(h)
                            },
                            onIncrementDhikr = { id, tgt ->
                                adhkarCounts = store.incrementDhikrCount(id, tgt)
                            },
                            onTasbihTap = {
                                tasbihCount = (tasbihCount + 1)
                                if (tasbihCount > tasbihTarget) tasbihCount = 1
                                tasbihLifetime++
                                store.setTasbihCount(tasbihCount)
                                store.setTasbihLifetime(tasbihLifetime)
                            },
                            onTasbihReset = {
                                tasbihCount = 0
                                store.setTasbihCount(0)
                            },
                            onTasbihTargetChange = { tgt ->
                                tasbihTarget = tgt
                                tasbihCount = 0
                                store.setTasbihTarget(tgt)
                                store.setTasbihCount(0)
                            },
                            onAddTasbihPhrase = { phrase ->
                                customTasbihPhrases = store.addCustomTasbihPhrase(phrase)
                            },
                            onToggleKhatmahJuz = { khatmahJuz = store.toggleKhatmahJuz(it) },
                            onSaveReminders = { prefs ->
                                reminderPrefs = prefs
                                store.saveReminderPrefs(prefs)
                            }
                        )
                    }
                }
            }
        }
    }
}
