package com.nooralislam.app.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nooralislam.app.data.ProphetStory
import com.nooralislam.app.data.QuranRepository
import com.nooralislam.app.data.SahabiStory
import com.nooralislam.app.ui.CardShape
import com.nooralislam.app.ui.NoorColors
import com.nooralislam.app.ui.cardBorder
import com.nooralislam.app.ui.surfaceCardColors
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoriesScreen(
    darkMode: Boolean,
    repository: QuranRepository,
    readProphets: Set<String>,
    readSahaba: Set<String>,
    onToggleReadProphet: (String) -> Unit,
    onToggleReadSahabi: (String) -> Unit
) {
    val context = LocalContext.current
    val accent = if (darkMode) NoorColors.EmeraldGlow else NoorColors.Emerald
    val goldAccent = if (darkMode) NoorColors.GoldLight else NoorColors.Gold
    val muted = if (darkMode) NoorColors.DarkMuted else NoorColors.MutedLight

    val prophets = remember { repository.getProphets() }
    val sahaba = remember { repository.getSahaba() }

    var mode by remember { mutableIntStateOf(0) } // 0 = Prophets, 1 = Sahaba
    var query by remember { mutableStateOf("") }
    var sahabiCat by remember { mutableStateOf("الكل") }
    var selectedProphet by remember { mutableStateOf<ProphetStory?>(null) }
    var selectedSahabi by remember { mutableStateOf<SahabiStory?>(null) }

    // Arabic TTS Narration
    var isSpeaking by remember { mutableStateOf(false) }
    val tts = remember {
        var inst: TextToSpeech? = null
        inst = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                inst?.setLanguage(Locale("ar"))
            }
        }
        inst
    }
    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    fun speakAll(paragraphs: List<String>) {
        if (isSpeaking) {
            tts.stop()
            isSpeaking = false
            return
        }
        isSpeaking = true
        val full = paragraphs.joinToString(" . ").take(3800)
        tts.speak(full, TextToSpeech.QUEUE_FLUSH, null, "story_tts")
    }

    // Detail View: Prophet
    selectedProphet?.let { p ->
        val isRead = readProphets.contains(p.id)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = {
                        tts.stop(); isSpeaking = false; selectedProphet = null
                    }) {
                        Text("← العودة للأنبياء")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { speakAll(listOf(p.summary) + p.paragraphs) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSpeaking) Color(0xFFB91C1C) else accent)
                        ) {
                            Icon(if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isSpeaking) "إيقاف" else "استماع", fontSize = 12.sp)
                        }
                        FilterChip(
                            selected = isRead,
                            onClick = { onToggleReadProphet(p.id) },
                            label = { Text(if (isRead) "تمت القراءة ✓" else "تحديد كمقروء", fontSize = 12.sp) }
                        )
                    }
                }
            }
            item {
                Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("${p.order}. ${p.nameAr}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accent)
                        Text(p.titleAr, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = goldAccent)
                        Text(
                            "القوم/العصر: ${p.eraOrPeople} · الذكر في القرآن: ${p.quranMentions} مرة · ${p.keySurahs}",
                            fontSize = 12.sp,
                            color = muted
                        )
                        HorizontalDivider()
                        Text(p.summary, fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 25.sp)
                    }
                }
            }
            items(p.paragraphs) { para ->
                Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                    Text(
                        para,
                        fontSize = 15.sp,
                        lineHeight = 26.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            if (p.miracles.isNotEmpty()) {
                item {
                    Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("المعجزات والآيات البينات", fontWeight = FontWeight.Bold, color = goldAccent, fontSize = 15.sp)
                            p.miracles.forEach { m ->
                                Text("• $m", fontSize = 14.sp, lineHeight = 22.sp)
                            }
                        }
                    }
                }
            }
            if (p.lessons.isNotEmpty()) {
                item {
                    Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("العبر والدروس المستفادة", fontWeight = FontWeight.Bold, color = accent, fontSize = 15.sp)
                            p.lessons.forEach { l ->
                                Text("✓ $l", fontSize = 14.sp, lineHeight = 22.sp)
                            }
                        }
                    }
                }
            }
        }
        return
    }

    // Detail View: Sahabi
    selectedSahabi?.let { s ->
        val isRead = readSahaba.contains(s.id)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = {
                        tts.stop(); isSpeaking = false; selectedSahabi = null
                    }) {
                        Text("← العودة للصحابة")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { speakAll(listOf(s.summary) + s.story) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSpeaking) Color(0xFFB91C1C) else accent)
                        ) {
                            Icon(if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isSpeaking) "إيقاف" else "استماع", fontSize = 12.sp)
                        }
                        FilterChip(
                            selected = isRead,
                            onClick = { onToggleReadSahabi(s.id) },
                            label = { Text(if (isRead) "تمت القراءة ✓" else "تحديد كمقروء", fontSize = 12.sp) }
                        )
                    }
                }
            }
            item {
                Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${s.order}. ${s.nameAr}", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = accent)
                        Text("${s.titleAr} · ${s.category}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = goldAccent)
                        if (s.keyVirtue.isNotBlank()) {
                            Surface(color = accent.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    "منقبة بارزة: ${s.keyVirtue}",
                                    color = accent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        Text(s.summary, fontSize = 15.sp, lineHeight = 25.sp)
                    }
                }
            }
            items(s.story) { para ->
                Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                    Text(para, fontSize = 15.sp, lineHeight = 26.sp, modifier = Modifier.padding(16.dp))
                }
            }
        }
        return
    }

    // Main List View
    val sahabaCategories = remember(sahaba) {
        listOf("الكل") + sahaba.map { it.category }.distinct()
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("موسوعة قصص الأنبياء وسير الصحابة الكاملة", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(
                        "تمت قراءة ${readProphets.size}/${prophets.size} نبياً و ${readSahaba.size}/${sahaba.size} صحابياً (متاحة كاملة بدون نت)",
                        fontSize = 12.sp,
                        color = muted
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { mode = 0 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (mode == 0) accent else accent.copy(alpha = 0.14f),
                                contentColor = if (mode == 0) Color.White else accent
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("قصص الأنبياء (${prophets.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { mode = 1 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (mode == 1) accent else accent.copy(alpha = 0.14f),
                                contentColor = if (mode == 1) Color.White else accent
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("سير الصحابة (${sahaba.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                        },
                        placeholder = { Text("ابحث بالاسم أو القوم أو الصفة...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (mode == 1) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            sahabaCategories.forEach { cat ->
                                FilterChip(
                                    selected = sahabiCat == cat,
                                    onClick = { sahabiCat = cat },
                                    label = { Text(cat, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (mode == 0) {
            val filtered = prophets.filter {
                query.isBlank() ||
                    QuranRepository.normalizeArabic(it.nameAr).contains(QuranRepository.normalizeArabic(query)) ||
                    QuranRepository.normalizeArabic(it.titleAr).contains(QuranRepository.normalizeArabic(query)) ||
                    QuranRepository.normalizeArabic(it.eraOrPeople).contains(QuranRepository.normalizeArabic(query))
            }
            items(filtered, key = { it.id }) { p ->
                val isRead = readProphets.contains(p.id)
                Card(
                    shape = CardShape,
                    border = if (isRead) BorderStroke(1.5.dp, accent) else cardBorder(darkMode),
                    colors = surfaceCardColors(darkMode),
                    modifier = Modifier.fillMaxWidth().clickable { selectedProphet = p }
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${p.order}. ${p.nameAr}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accent)
                            if (isRead) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "مقروءة", tint = accent, modifier = Modifier.size(18.dp))
                            }
                        }
                        Text(p.titleAr, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = goldAccent)
                        Text(p.summary, fontSize = 13.sp, color = muted, maxLines = 3)
                        Text("${p.eraOrPeople} · ${p.paragraphs.size} فقرات تفصيلية", fontSize = 11.sp, color = accent)
                    }
                }
            }
        } else {
            val filtered = sahaba.filter {
                (sahabiCat == "الكل" || it.category == sahabiCat) &&
                    (query.isBlank() ||
                        QuranRepository.normalizeArabic(it.nameAr).contains(QuranRepository.normalizeArabic(query)) ||
                        QuranRepository.normalizeArabic(it.titleAr).contains(QuranRepository.normalizeArabic(query)))
            }
            items(filtered, key = { it.id }) { s ->
                val isRead = readSahaba.contains(s.id)
                Card(
                    shape = CardShape,
                    border = if (isRead) BorderStroke(1.5.dp, accent) else cardBorder(darkMode),
                    colors = surfaceCardColors(darkMode),
                    modifier = Modifier.fillMaxWidth().clickable { selectedSahabi = s }
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${s.order}. ${s.nameAr}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accent)
                            if (isRead) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "مقروءة", tint = accent, modifier = Modifier.size(18.dp))
                            }
                        }
                        Text("${s.titleAr} · ${s.category}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = goldAccent)
                        Text(s.summary, fontSize = 13.sp, color = muted, maxLines = 3)
                    }
                }
            }
        }
    }
}
