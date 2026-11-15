package com.nooralislam.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nooralislam.app.R
import com.nooralislam.app.audio.OfflineReciterManager
import com.nooralislam.app.data.AyahItem
import com.nooralislam.app.data.QuranBookmark
import com.nooralislam.app.data.QuranRepository
import com.nooralislam.app.data.ReciterInfo
import com.nooralislam.app.data.SurahMeta
import com.nooralislam.app.ui.CardShape
import com.nooralislam.app.ui.NoorColors
import com.nooralislam.app.ui.cardBorder
import com.nooralislam.app.ui.surfaceCardColors
import com.nooralislam.app.utils.TasmeeMatchResult
import com.nooralislam.app.utils.TasmeeNormalizer
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuranScreen(
    darkMode: Boolean,
    repository: QuranRepository,
    reciterManager: OfflineReciterManager,
    activeSurahId: Int,
    activeReciterId: String,
    fontSize: Int,
    khatmahJuz: Set<Int>,
    bookmarks: List<QuranBookmark>,
    onSelectSurah: (Int) -> Unit,
    onSelectReciter: (String) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onToggleKhatmahJuz: (Int) -> Unit,
    onToggleBookmark: (QuranBookmark) -> Unit,
    onAwardXp: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accent = if (darkMode) NoorColors.EmeraldGlow else NoorColors.Emerald
    val muted = if (darkMode) NoorColors.DarkMuted else NoorColors.MutedLight

    val surahs = remember { repository.getSurahs() }
    val reciters = remember { repository.getReciters() }
    val activeSurah = surahs.firstOrNull { it.id == activeSurahId } ?: SurahMeta(1, "الفاتحة", 7, "مكية", 1)
    val activeReciter = reciters.firstOrNull { it.id == activeReciterId } ?: reciters.first()
    val ayahs = remember(activeSurahId) { repository.getSurahAyahs(activeSurahId) }

    // Sub-tabs: 0=Reading&Memorize, 1=Mushaf(604), 2=Search, 3=Surahs&Reciters, 4=Bookmarks
    var subTab by remember { mutableIntStateOf(0) }
    var isPlayingSurah by remember { mutableStateOf(false) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    // Tafsir & Reading states
    var expandedTafsirAyah by remember { mutableStateOf<Int?>(null) }
    var hideVersesForTest by remember { mutableStateOf(false) }

    // Repeat Memorization states
    var repeatPanelOpen by remember { mutableStateOf(false) }
    var repeatFromAyah by remember(activeSurahId) { mutableIntStateOf(1) }
    var repeatToAyah by remember(activeSurahId) { mutableIntStateOf(ayahs.size.coerceAtMost(5).coerceAtLeast(1)) }
    var repeatTargetCount by remember { mutableIntStateOf(3) }
    var isRepeating by remember { mutableStateOf(false) }
    var repeatProgressLabel by remember { mutableStateOf("") }

    // Tasmee states
    var tasmeeOpen by remember { mutableStateOf(false) }
    var tasmeeFromAyah by remember(activeSurahId) { mutableIntStateOf(1) }
    var tasmeeToAyah by remember(activeSurahId) { mutableIntStateOf(ayahs.size.coerceAtMost(3).coerceAtLeast(1)) }
    var isListening by remember { mutableStateOf(false) }
    var spokenTranscript by remember { mutableStateOf("") }
    var manualCompareInput by remember { mutableStateOf("") }
    var tasmeeResult by remember { mutableStateOf<TasmeeMatchResult?>(null) }
    var tasmeeError by remember { mutableStateOf<String?>(null) }

    // Mushaf 604 page state
    var mushafPage by remember { mutableIntStateOf(1) }
    LaunchedEffect(activeSurahId) {
        val firstPage = ayahs.firstOrNull()?.page ?: 1
        mushafPage = firstPage.coerceIn(1, 604)
    }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    val searchResults = remember(searchQuery) {
        if (searchQuery.trim().length >= 2) repository.searchQuran(searchQuery) else emptyList()
    }

    // Surahs & Reciters filter & Offline download states
    var surahFilter by remember { mutableStateOf("") }
    var reciterFilter by remember { mutableStateOf("") }
    var downloadVersion by remember { mutableIntStateOf(0) }
    var isBatchDownloading by remember { mutableStateOf(false) }
    var batchProgress by remember { mutableIntStateOf(0) }
    var batchTotal by remember { mutableIntStateOf(0) }

    // Arabic TTS for Tafsir
    var ttsReady by remember { mutableStateOf(false) }
    var speakingTafsirAyah by remember { mutableStateOf<Int?>(null) }
    val tts = remember {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = instance?.setLanguage(Locale("ar"))
                ttsReady = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
        instance
    }
    DisposableEffect(Unit) {
        onDispose {
            reciterManager.stop()
            tts.stop()
            tts.shutdown()
        }
    }

    // SpeechRecognizer setup
    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }
    DisposableEffect(speechRecognizer) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    fun startSpeechCapture(targetText: String) {
        if (speechRecognizer == null) {
            tasmeeError = "خدمة التعرف على الصوت غير مفعّلة على هذا الجهاز — يمكنك كتابة التسميع أدناه للتحقق الفوري."
            return
        }
        tasmeeError = null
        tasmeeResult = null
        isListening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                isListening = false
                if (spokenTranscript.isNotBlank()) {
                    val res = TasmeeNormalizer.compare(targetText, spokenTranscript)
                    tasmeeResult = res
                    if (res.accuracyPercent >= 75) onAwardXp(30)
                } else {
                    tasmeeError = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                            "لم يتم التقاط صوت واضح — اقترب من الميكروفون أو جرّب التحقق النصي أدناه."
                        else -> "تعذر تشغيل الميكروفون (رمز $error). تأكد من اتصال خدمة الصوت أو استخدم التحقق النصي."
                    }
                }
            }
            override fun onResults(results: Bundle?) {
                val best = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (best.isNotBlank()) {
                    spokenTranscript = if (spokenTranscript.isBlank()) best else "$spokenTranscript $best"
                }
                isListening = false
                val res = TasmeeNormalizer.compare(targetText, spokenTranscript)
                tasmeeResult = res
                if (res.accuracyPercent >= 75) onAwardXp(30)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (partial.isNotBlank()) {
                    tasmeeResult = TasmeeNormalizer.compare(targetText, "$spokenTranscript $partial".trim())
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        runCatching { speechRecognizer.startListening(intent) }.onFailure {
            isListening = false
            tasmeeError = "تعذر بدء الميكروفون: ${it.message}"
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            tasmeeError = "يرجى منح إذن الميكروفون للاستماع للتسميع."
        } else {
            val targetText = ayahs.filter { it.numberInSurah in tasmeeFromAyah..tasmeeToAyah }.joinToString(" ") { it.text }
            startSpeechCapture(targetText)
        }
    }

    // Repeat Memorization coroutine runner
    fun startRepeatLoop() {
        val from = repeatFromAyah.coerceIn(1, ayahs.size.coerceAtLeast(1))
        val to = repeatToAyah.coerceIn(from, ayahs.size.coerceAtLeast(1))
        val slice = ayahs.filter { it.numberInSurah in from..to }
        if (slice.isEmpty()) return
        isRepeating = true
        isPlayingSurah = false
        var idx = 0
        var round = 1

        fun playStep() {
            if (!isRepeating) return
            if (idx >= slice.size) {
                if (round < repeatTargetCount) {
                    round++
                    idx = 0
                } else {
                    isRepeating = false
                    repeatProgressLabel = "اكتمل التكرار (${repeatTargetCount} مرات) ✓"
                    return
                }
            }
            val ayah = slice[idx]
            repeatProgressLabel = "آية ${ayah.numberInSurah} · التكرار $round من $repeatTargetCount"
            val url = activeReciter.buildAyahUrl(activeSurahId, ayah.numberInSurah, ayah.globalNumber)
            reciterManager.playUrlOrPath(
                source = url,
                onComplete = {
                    idx++
                    playStep()
                },
                onError = { msg ->
                    isRepeating = false
                    statusBanner = msg
                }
            )
        }
        playStep()
    }

    val isCurrentCached = remember(activeReciterId, activeSurahId, downloadVersion) {
        reciterManager.isSurahDownloaded(activeReciterId, activeSurahId)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Surah Hero Header
        item {
            Card(shape = RoundedCornerShape(18.dp), border = cardBorder(darkMode)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(R.drawable.quran_manuscript_art),
                        contentDescription = "القرآن الكريم",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color(0xCC0B3A30), Color(0xEC0B4A3C))))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "سورة ${activeSurah.nameAr}",
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${activeSurah.revelationType} · ${activeSurah.ayahCount} آية · الجزء ${activeSurah.juz} · ${activeReciter.nameAr}",
                                        color = Color(0xFFFDE68A),
                                        fontSize = 12.sp
                                    )
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.clickable {
                                        onToggleKhatmahJuz(activeSurah.juz)
                                    }
                                ) {
                                    val juzDone = khatmahJuz.contains(activeSurah.juz)
                                    Text(
                                        if (juzDone) "الجزء ${activeSurah.juz} ✓" else "تحديد الجزء ${activeSurah.juz}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (isPlayingSurah) {
                                            reciterManager.stop()
                                            isPlayingSurah = false
                                        } else {
                                            isRepeating = false
                                            statusBanner = "جاري تشغيل سورة ${activeSurah.nameAr}..."
                                            reciterManager.playSurah(
                                                reciter = activeReciter,
                                                surahId = activeSurahId,
                                                onPrepared = { statusBanner = null },
                                                onComplete = { isPlayingSurah = false },
                                                onError = { err ->
                                                    isPlayingSurah = false
                                                    statusBanner = err
                                                }
                                            )
                                            isPlayingSurah = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = NoorColors.EmeraldDark
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        if (isPlayingSurah) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (isPlayingSurah) "إيقاف التلاوة" else if (isCurrentCached) "تشغيل بدون نت" else "تشغيل السورة",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (isCurrentCached) {
                                            statusBanner = "سورة ${activeSurah.nameAr} محفوظة بالفعل على جهازك للعمل بدون نت ✓"
                                        } else {
                                            scope.launch {
                                                statusBanner = "جاري تنزيل سورة ${activeSurah.nameAr}..."
                                                val ok = reciterManager.downloadSurah(activeReciter, activeSurahId)
                                                downloadVersion++
                                                statusBanner = if (ok) {
                                                    "تم تنزيل سورة ${activeSurah.nameAr} للعمل بدون إنترنت ✓"
                                                } else {
                                                    "تعذر التنزيل — تحقق من اتصال الإنترنت"
                                                }
                                            }
                                        }
                                    },
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        if (isCurrentCached) Icons.Default.CheckCircle else Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isCurrentCached) "محفوظة" else "تنزيل", fontSize = 12.sp)
                                }
                            }

                            statusBanner?.let { msg ->
                                Text(msg, color = Color(0xFFFDE68A), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Sub-tabs Selector
        item {
            val subTabs = listOf(
                "القراءة والتحفيظ",
                "المصحف (٦٠٤)",
                "بحث القرآن",
                "السور والشيوخ",
                "العلامات (${bookmarks.size})"
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                subTabs.forEachIndexed { idx, label ->
                    val selected = subTab == idx
                    FilterChip(
                        selected = selected,
                        onClick = { subTab = idx },
                        label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        }

        // TAB 0: READING, TAFSIR, REPEAT & TASMEE
        if (subTab == 0) {
            item {
                Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { onFontSizeChange((fontSize - 2).coerceAtLeast(16)) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "تصغير الخط")
                                }
                                Text("الخط $fontSize", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                IconButton(
                                    onClick = { onFontSizeChange((fontSize + 2).coerceAtMost(38)) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "تكبير الخط")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { hideVersesForTest = !hideVersesForTest },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        if (hideVersesForTest) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (hideVersesForTest) "إظهار" else "إخفاء للاختبار", fontSize = 11.sp)
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    repeatPanelOpen = !repeatPanelOpen
                                    tasmeeOpen = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (repeatPanelOpen) accent else accent.copy(alpha = 0.14f),
                                    contentColor = if (repeatPanelOpen) Color.White else accent
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("تكرار الحفظ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    tasmeeOpen = !tasmeeOpen
                                    repeatPanelOpen = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (tasmeeOpen) accent else accent.copy(alpha = 0.14f),
                                    contentColor = if (tasmeeOpen) Color.White else accent
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("التسميع الصوتي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Repeat Memorization Panel
                        if (repeatPanelOpen) {
                            HorizontalDivider()
                            Text("تكرار الآيات للتحفيظ (${activeSurah.nameAr})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = accent)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                NumberStepper("من آية", repeatFromAyah, 1, ayahs.size.coerceAtLeast(1), modifier = Modifier.weight(1f)) {
                                    repeatFromAyah = it
                                    if (repeatToAyah < it) repeatToAyah = it
                                }
                                NumberStepper("إلى آية", repeatToAyah, repeatFromAyah, ayahs.size.coerceAtLeast(1), modifier = Modifier.weight(1f)) {
                                    repeatToAyah = it
                                }
                                NumberStepper("التكرار", repeatTargetCount, 1, 10, modifier = Modifier.weight(1f)) {
                                    repeatTargetCount = it
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = {
                                        if (isRepeating) {
                                            isRepeating = false
                                            reciterManager.stop()
                                        } else {
                                            startRepeatLoop()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isRepeating) Color(0xFFB91C1C) else accent)
                                ) {
                                    Icon(if (isRepeating) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isRepeating) "إيقاف التكرار" else "بدء التكرار", fontSize = 12.sp)
                                }
                                if (repeatProgressLabel.isNotBlank()) {
                                    Text(repeatProgressLabel, fontSize = 12.sp, color = accent, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Voice Tasmee Panel
                        if (tasmeeOpen) {
                            HorizontalDivider()
                            Text("استوديو التسميع الذكي بالمايك", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = accent)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NumberStepper("من آية", tasmeeFromAyah, 1, ayahs.size.coerceAtLeast(1), modifier = Modifier.weight(1f)) {
                                    tasmeeFromAyah = it
                                    if (tasmeeToAyah < it) tasmeeToAyah = it
                                }
                                NumberStepper("إلى آية", tasmeeToAyah, tasmeeFromAyah, ayahs.size.coerceAtLeast(1), modifier = Modifier.weight(1f)) {
                                    tasmeeToAyah = it
                                }
                            }
                            val targetTasmeeText = ayahs
                                .filter { it.numberInSurah in tasmeeFromAyah..tasmeeToAyah }
                                .joinToString(" ") { it.text }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (isListening) {
                                            speechRecognizer?.stopListening()
                                            isListening = false
                                            if (spokenTranscript.isNotBlank()) {
                                                val res = TasmeeNormalizer.compare(targetTasmeeText, spokenTranscript)
                                                tasmeeResult = res
                                                if (res.accuracyPercent >= 75) onAwardXp(30)
                                            }
                                        } else {
                                            spokenTranscript = ""
                                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                            if (granted) startSpeechCapture(targetTasmeeText) else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isListening) Color(0xFFB91C1C) else accent)
                                ) {
                                    Icon(if (isListening) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isListening) "إنهاء وفحص التسميع" else "ابدأ التسميع بالصوت", fontSize = 12.sp)
                                }
                            }

                            OutlinedTextField(
                                value = manualCompareInput,
                                onValueChange = { manualCompareInput = it },
                                label = { Text("أو اكتب التسميع يدوياً للتحقق الفوري (يعمل بدون نت)", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    if (manualCompareInput.isNotBlank()) {
                                        IconButton(onClick = {
                                            val res = TasmeeNormalizer.compare(targetTasmeeText, manualCompareInput)
                                            tasmeeResult = res
                                            if (res.accuracyPercent >= 75) onAwardXp(30)
                                        }) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "فحص", tint = accent)
                                        }
                                    }
                                }
                            )

                            tasmeeError?.let { err ->
                                Text(err, color = Color(0xFFD97706), fontSize = 12.sp)
                            }

                            tasmeeResult?.let { res ->
                                Surface(
                                    color = if (darkMode) NoorColors.DarkSurfaceAlt else NoorColors.Parchment,
                                    shape = RoundedCornerShape(12.dp),
                                    border = cardBorder(darkMode)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "الدقة: ${res.accuracyPercent}% (${res.matchedWords} من ${res.totalWords} كلمة)" +
                                                if (res.accuracyPercent >= 75) " · +30 XP ★" else "",
                                            fontWeight = FontWeight.Bold,
                                            color = if (res.accuracyPercent >= 75) accent else Color(0xFFD97706)
                                        )
                                        Text(
                                            text = buildAnnotatedString {
                                                res.wordStatuses.forEachIndexed { i, ws ->
                                                    withStyle(
                                                        SpanStyle(
                                                            color = if (ws.matched) accent else Color(0xFFDC2626),
                                                            fontWeight = if (ws.matched) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    ) {
                                                        append(ws.displayWord)
                                                    }
                                                    if (i < res.wordStatuses.lastIndex) append(" ")
                                                }
                                            },
                                            fontSize = 18.sp,
                                            lineHeight = 30.sp
                                        )
                                        if (res.missedWords.isNotEmpty()) {
                                            Text(
                                                "كلمات تحتاج مراجعة: ${res.missedWords.take(10).joinToString(" · ")}",
                                                fontSize = 12.sp,
                                                color = Color(0xFFDC2626)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Ayahs list
            items(ayahs, key = { it.globalNumber }) { ayah ->
                val isBookmarked = bookmarks.any { it.surahId == activeSurahId && it.ayahNumber == ayah.numberInSurah }
                val tafsirOpen = expandedTafsirAyah == ayah.numberInSurah
                Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(color = accent.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    "آية ${ayah.numberInSurah} · ص ${ayah.page}",
                                    color = accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        val url = activeReciter.buildAyahUrl(activeSurahId, ayah.numberInSurah, ayah.globalNumber)
                                        reciterManager.playUrlOrPath(url, onError = { statusBanner = it })
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "سماع الآية", tint = accent, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = {
                                        onToggleBookmark(
                                            QuranBookmark(
                                                surahId = activeSurahId,
                                                surahName = activeSurah.nameAr,
                                                ayahNumber = ayah.numberInSurah,
                                                textPreview = ayah.text.take(65)
                                            )
                                        )
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "علامة مرجعية",
                                        tint = if (isBookmarked) NoorColors.Gold else muted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (hideVersesForTest) "﴿ آية ${ayah.numberInSurah} — مخفية للمراجعة والحفظ ﴾" else "${ayah.text} ﴿${ayah.numberInSurah}﴾",
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.65).sp,
                            fontWeight = FontWeight.Medium,
                            color = if (hideVersesForTest) muted else if (darkMode) NoorColors.DarkInk else NoorColors.InkLight,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        if (ayah.tafsir.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (tafsirOpen) "إخفاء التفسير الميسر" else "عرض التفسير الميسر",
                                    color = accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        expandedTafsirAyah = if (tafsirOpen) null else ayah.numberInSurah
                                    }
                                )
                                if (tafsirOpen && ttsReady) {
                                    val isSpeakingThis = speakingTafsirAyah == ayah.numberInSurah
                                    TextButton(
                                        onClick = {
                                            if (isSpeakingThis) {
                                                tts.stop()
                                                speakingTafsirAyah = null
                                            } else {
                                                speakingTafsirAyah = ayah.numberInSurah
                                                tts.speak(ayah.tafsir, TextToSpeech.QUEUE_FLUSH, null, "tafsir_${ayah.globalNumber}")
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            if (isSpeakingThis) Icons.Default.Stop else Icons.Default.VolumeUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (isSpeakingThis) "إيقاف الصوت" else "سماع التفسير", fontSize = 11.sp)
                                    }
                                }
                            }
                            if (tafsirOpen) {
                                Surface(
                                    color = if (darkMode) NoorColors.DarkSurfaceAlt else NoorColors.Parchment,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        ayah.tafsir,
                                        fontSize = 13.sp,
                                        lineHeight = 22.sp,
                                        color = muted,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // TAB 1: MUSHAF 604 PAGES
        if (subTab == 1) {
            item {
                val pageAyahs = remember(mushafPage) { repository.getMushafPage(mushafPage) }
                val firstJuz = pageAyahs.firstOrNull()?.juz ?: 1
                Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { if (mushafPage > 1) mushafPage-- }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "الصفحة السابقة")
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("صفحة $mushafPage من ٦٠٤", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accent)
                                Text("الجزء $firstJuz", fontSize = 12.sp, color = muted)
                            }
                            IconButton(onClick = { if (mushafPage < 604) mushafPage++ }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "الصفحة التالية")
                            }
                        }
                        Slider(
                            value = mushafPage.toFloat(),
                            onValueChange = { mushafPage = it.toInt().coerceIn(1, 604) },
                            valueRange = 1f..604f
                        )
                        HorizontalDivider()
                        val grouped = pageAyahs.groupBy { it.surahName }
                        grouped.forEach { (surahName, list) ->
                            Surface(
                                color = accent.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "سورة $surahName",
                                    fontWeight = FontWeight.Bold,
                                    color = accent,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                            Text(
                                text = list.joinToString(" ") { "${it.text} ﴿${it.numberInSurah}﴾" },
                                fontSize = fontSize.sp,
                                lineHeight = (fontSize * 1.75).sp,
                                textAlign = TextAlign.Justify
                            )
                        }
                    }
                }
            }
        }

        // TAB 2: QURAN SEARCH
        if (subTab == 2) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    placeholder = { Text("ابحث في ٦٢٣٦ آية بدون تشكيل...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (searchQuery.trim().length >= 2 && searchResults.isEmpty()) {
                item {
                    Text("لا توجد نتائج مطابقة لـ \"$searchQuery\"", color = muted, fontSize = 13.sp)
                }
            }
            items(searchResults, key = { it.globalNumber }) { hit ->
                Card(
                    shape = CardShape,
                    border = cardBorder(darkMode),
                    colors = surfaceCardColors(darkMode),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectSurah(hit.surahId)
                            subTab = 0
                        }
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "سورة ${hit.surahName} · آية ${hit.numberInSurah} · صفحة ${hit.page}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        Text(hit.text, fontSize = 17.sp, lineHeight = 28.sp)
                    }
                }
            }
        }

        // TAB 3: 114 SURAHS, 36 RECITERS & BATCH OFFLINE DOWNLOADER
        if (subTab == 3) {
            item {
                val cachedCount = remember(activeReciterId, downloadVersion) {
                    reciterManager.countDownloadedForReciter(activeReciterId)
                }
                val cachedMb = remember(activeReciterId, downloadVersion) {
                    reciterManager.cachedSizeMbForReciter(activeReciterId)
                }
                Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("إدارة التلاوات بدون نت — ${activeReciter.nameAr}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = accent)
                        Text(
                            "السور المحفوظة لهذا القارئ: $cachedCount / ١١٤ (${"%.1f".format(cachedMb)} MB)",
                            fontSize = 12.sp,
                            color = muted
                        )
                        if (isBatchDownloading && batchTotal > 0) {
                            LinearProgressIndicator(
                                progress = { batchProgress.toFloat() / batchTotal.toFloat() },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("جاري التنزيل: $batchProgress من $batchTotal سورة...", fontSize = 12.sp, color = accent)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                enabled = !isBatchDownloading,
                                onClick = {
                                    scope.launch {
                                        isBatchDownloading = true
                                        val ids = (78..114).toList()
                                        batchTotal = ids.size
                                        batchProgress = 0
                                        for (id in ids) {
                                            reciterManager.downloadSurah(activeReciter, id)
                                            batchProgress++
                                            downloadVersion++
                                        }
                                        isBatchDownloading = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = accent)
                            ) {
                                Text("تنزيل جزء عمّ (٣٧ سورة)", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                enabled = !isBatchDownloading && cachedCount > 0,
                                onClick = {
                                    reciterManager.clearReciterCache(activeReciterId)
                                    downloadVersion++
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("مسح الكاش", fontSize = 11.sp)
                            }
                        }

                        HorizontalDivider()
                        Text("اختر القارئ (${reciters.size} قارئاً)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedTextField(
                            value = reciterFilter,
                            onValueChange = { reciterFilter = it },
                            placeholder = { Text("ابحث عن قارئ...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        val filteredReciters = reciters.filter {
                            reciterFilter.isBlank() || it.nameAr.contains(reciterFilter) || it.countryAr.contains(reciterFilter)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            filteredReciters.take(18).forEach { r ->
                                FilterChip(
                                    selected = r.id == activeReciterId,
                                    onClick = { onSelectReciter(r.id) },
                                    label = { Text("${r.nameAr} (${r.countryAr})", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = surahFilter,
                    onValueChange = { surahFilter = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("ابحث عن سورة من ١١٤ سورة...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val filteredSurahs = surahs.filter {
                surahFilter.isBlank() || QuranRepository.normalizeArabic(it.nameAr).contains(QuranRepository.normalizeArabic(surahFilter))
            }
            items(filteredSurahs, key = { it.id }) { s ->
                val isCached = reciterManager.isSurahDownloaded(activeReciterId, s.id)
                Card(
                    shape = CardShape,
                    border = if (s.id == activeSurahId) BorderStroke(1.5.dp, accent) else cardBorder(darkMode),
                    colors = surfaceCardColors(darkMode),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectSurah(s.id)
                            subTab = 0
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = accent.copy(alpha = 0.14f), shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    "${s.id}",
                                    color = accent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Column {
                                Text("سورة ${s.nameAr}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("${s.revelationType} · ${s.ayahCount} آية · ج${s.juz}", fontSize = 11.sp, color = muted)
                            }
                        }
                        if (isCached) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "محفوظة", tint = accent, modifier = Modifier.size(18.dp))
                        } else {
                            IconButton(onClick = {
                                scope.launch {
                                    reciterManager.downloadSurah(activeReciter, s.id)
                                    downloadVersion++
                                }
                            }) {
                                Icon(Icons.Default.Download, contentDescription = "تنزيل", tint = muted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // TAB 4: BOOKMARKS
        if (subTab == 4) {
            if (bookmarks.isEmpty()) {
                item {
                    Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                        Text(
                            "لا توجد علامات مرجعية محفوظة بعد. اضغط على أيقونة العلامة بجانب أي آية لحفظها هنا.",
                            color = muted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            items(bookmarks, key = { "${it.surahId}_${it.ayahNumber}" }) { b ->
                Card(
                    shape = CardShape,
                    border = cardBorder(darkMode),
                    colors = surfaceCardColors(darkMode),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectSurah(b.surahId)
                            subTab = 0
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("سورة ${b.surahName} · آية ${b.ayahNumber}", fontWeight = FontWeight.Bold, color = accent, fontSize = 13.sp)
                            Text(b.textPreview, fontSize = 14.sp, maxLines = 2)
                        }
                        IconButton(onClick = { onToggleBookmark(b) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFDC2626))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (value > min) onChange(value - 1) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(15.dp))
            }
            Text("$value", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            IconButton(onClick = { if (value < max) onChange(value + 1) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
            }
        }
    }
}
