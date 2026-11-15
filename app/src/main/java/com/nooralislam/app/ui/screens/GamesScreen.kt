package com.nooralislam.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nooralislam.app.data.QuizQuestion
import com.nooralislam.app.data.QuizStage
import com.nooralislam.app.data.QuranRepository
import com.nooralislam.app.ui.CardShape
import com.nooralislam.app.ui.NoorColors
import com.nooralislam.app.ui.cardBorder
import com.nooralislam.app.ui.surfaceCardColors
import kotlinx.coroutines.delay
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GamesScreen(
    darkMode: Boolean,
    repository: QuranRepository,
    xp: Int,
    quizProgress: Map<Int, Int>,
    speedBestScore: Int,
    onStageComplete: (stageNumber: Int, stars: Int, earnedXp: Int) -> Unit,
    onAwardXp: (Int) -> Unit,
    onSpeedBestUpdate: (Int) -> Unit
) {
    val context = LocalContext.current
    val accent = if (darkMode) NoorColors.EmeraldGlow else NoorColors.Emerald
    val goldAccent = if (darkMode) NoorColors.GoldLight else NoorColors.Gold
    val muted = if (darkMode) NoorColors.DarkMuted else NoorColors.MutedLight

    val stages = remember { repository.getQuizStages() }
    val allQuestions = remember(stages) { stages.flatMap { it.questions } }
    val puzzles = remember { repository.getPuzzles() }

    val level = remember(xp) { (xp / 150) + 1 }
    val levelPct = remember(xp) { (xp % 150) / 150f }
    val totalStars = quizProgress.values.sum()

    // 0 = 100 Stages, 1 = 60s Speed, 2 = 55 Puzzles, 3 = Friend Challenge
    var subTab by remember { mutableIntStateOf(0) }

    // Active Stage State
    var activeStage by remember { mutableStateOf<QuizStage?>(null) }
    var stageQIdx by remember { mutableIntStateOf(0) }
    var stageScore by remember { mutableIntStateOf(0) }
    var stageSelectedOpt by remember { mutableStateOf<Int?>(null) }
    var stageFinished by remember { mutableStateOf(false) }

    // Speed Mode State
    var speedRunning by remember { mutableStateOf(false) }
    var speedTimeLeft by remember { mutableIntStateOf(60) }
    var speedScore by remember { mutableIntStateOf(0) }
    var speedCombo by remember { mutableIntStateOf(0) }
    var speedQIdx by remember { mutableIntStateOf(0) }
    var speedShuffled by remember { mutableStateOf(allQuestions.shuffled()) }
    var speedFinished by remember { mutableStateOf(false) }

    LaunchedEffect(speedRunning) {
        if (speedRunning) {
            while (speedTimeLeft > 0 && speedRunning) {
                delay(1000L)
                speedTimeLeft--
            }
            if (speedTimeLeft <= 0) {
                speedRunning = false
                speedFinished = true
                onSpeedBestUpdate(speedScore)
                onAwardXp(speedScore * 2)
            }
        }
    }

    // Puzzles State
    var puzzleFilter by remember { mutableStateOf("الكل") }
    var puzzleIndex by remember { mutableIntStateOf(0) }
    var puzzleSelected by remember { mutableStateOf<Int?>(null) }
    var puzzleShowHint by remember { mutableStateOf(false) }

    // Friend Seed Challenge State
    var seedInput by remember { mutableStateOf("NOOR-1447") }
    var seedQuestions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var seedQIdx by remember { mutableIntStateOf(0) }
    var seedSelected by remember { mutableStateOf<Int?>(null) }
    var seedScore by remember { mutableIntStateOf(0) }
    var seedDone by remember { mutableStateOf(false) }

    fun generateSeedQuestions(seedStr: String): List<QuizQuestion> {
        if (allQuestions.isEmpty()) return emptyList()
        var h = 2166136261L
        seedStr.forEach { ch ->
            h = h xor ch.code.toLong()
            h *= 16777619L
        }
        val pool = allQuestions.toMutableList()
        val picked = mutableListOf<QuizQuestion>()
        repeat(5.coerceAtMost(pool.size)) {
            h = (h * 6364136223846793005L + 1442695040888963407L)
            val idx = abs((h ushr 16).toInt()) % pool.size
            picked.add(pool.removeAt(idx))
        }
        return picked
    }

    // Active Stage Round View
    activeStage?.let { st ->
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { activeStage = null }) { Text("← العودة للمراحل") }
                    Text("المرحلة ${st.stageNumber}: ${st.titleAr}", fontWeight = FontWeight.Bold, color = accent, fontSize = 14.sp)
                }
            }
            if (stageFinished) {
                val stars = when {
                    stageScore == st.questions.size -> 3
                    stageScore >= 4 -> 2
                    stageScore >= 3 -> 1
                    else -> 0
                }
                item {
                    Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                if (stars >= 1) "أحسنت! اكتملت المرحلة ✓" else "تحتاج ٣ إجابات صحيحة على الأقل للفتح",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (stars >= 1) accent else Color(0xFFD97706)
                            )
                            Text("النتيجة: $stageScore من ${st.questions.size} · النجوم: $stars/3", fontSize = 15.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                repeat(3) { i ->
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (i < stars) goldAccent else muted.copy(alpha = 0.3f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = {
                                    stageQIdx = 0; stageScore = 0; stageSelectedOpt = null; stageFinished = false
                                }, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                                    Text("إعادة المحاولة")
                                }
                                OutlinedButton(onClick = { activeStage = null }) { Text("المراحل") }
                            }
                        }
                    }
                }
            } else {
                val q = st.questions[stageQIdx]
                item {
                    Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("السؤال ${stageQIdx + 1} من ${st.questions.size} · النقاط: $stageScore", fontSize = 12.sp, color = accent, fontWeight = FontWeight.Bold)
                            Text(q.question, fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
                            q.options.forEachIndexed { idx, opt ->
                                val isChosen = stageSelectedOpt == idx
                                val isRight = idx == q.correctIndex
                                val bg = when {
                                    stageSelectedOpt == null -> if (darkMode) NoorColors.DarkSurfaceAlt else NoorColors.Parchment
                                    isRight -> accent.copy(alpha = 0.2f)
                                    isChosen && !isRight -> Color(0x33DC2626)
                                    else -> if (darkMode) NoorColors.DarkSurfaceAlt else NoorColors.Parchment
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = bg,
                                    border = BorderStroke(
                                        1.dp,
                                        when {
                                            stageSelectedOpt != null && isRight -> accent
                                            isChosen && !isRight -> Color(0xFFDC2626)
                                            else -> if (darkMode) NoorColors.DarkBorder else NoorColors.BorderLight
                                        }
                                    ),
                                    modifier = Modifier.fillMaxWidth().clickable(enabled = stageSelectedOpt == null) {
                                        stageSelectedOpt = idx
                                        if (idx == q.correctIndex) stageScore++
                                    }
                                ) {
                                    Text(opt, modifier = Modifier.padding(14.dp), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            if (stageSelectedOpt != null) {
                                if (q.explanation.isNotBlank()) {
                                    Surface(color = accent.copy(alpha = 0.1f), shape = RoundedCornerShape(10.dp)) {
                                        Text(q.explanation, fontSize = 12.sp, modifier = Modifier.padding(10.dp), lineHeight = 19.sp)
                                    }
                                }
                                Button(
                                    onClick = {
                                        if (stageQIdx < st.questions.lastIndex) {
                                            stageQIdx++
                                            stageSelectedOpt = null
                                        } else {
                                            val stars = when {
                                                stageScore == st.questions.size -> 3
                                                stageScore >= 4 -> 2
                                                stageScore >= 3 -> 1
                                                else -> 0
                                            }
                                            stageFinished = true
                                            onStageComplete(st.stageNumber, stars, stageScore * 12)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (stageQIdx < st.questions.lastIndex) "السؤال التالي" else "عرض النتيجة")
                                }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // XP & Level Header
        item {
            Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("ساحة المسابقات والألعاب الإسلامية", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text("المستوى $level · $xp XP · النجوم المكتسبة: $totalStars / ${stages.size * 3}", fontSize = 12.sp, color = muted)
                        }
                        Surface(color = goldAccent.copy(alpha = 0.18f), shape = RoundedCornerShape(10.dp)) {
                            Text("المكتمل ${quizProgress.count { it.value >= 1 }}/${stages.size}", color = goldAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                    LinearProgressIndicator(progress = { levelPct }, modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("١٠٠ مرحلة (٥٠٠ سؤال)", "تحدي ٦٠ ثانية", "ألغاز الآيات (${puzzles.size})", "تحدي الأصدقاء").forEachIndexed { idx, t ->
                            FilterChip(selected = subTab == idx, onClick = { subTab = idx }, label = { Text(t, fontSize = 12.sp) })
                        }
                    }
                }
            }
        }

        // TAB 0: 100 STAGES
        if (subTab == 0) {
            val grouped = stages.chunked(2)
            items(grouped) { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { st ->
                        val stars = quizProgress[st.stageNumber] ?: -1
                        val unlocked = st.stageNumber == 1 || (quizProgress[st.stageNumber - 1] ?: 0) >= 1 || stars >= 0
                        Card(
                            shape = CardShape,
                            border = if (unlocked) cardBorder(darkMode) else BorderStroke(1.dp, muted.copy(alpha = 0.25f)),
                            colors = surfaceCardColors(darkMode),
                            modifier = Modifier.weight(1f).clickable(enabled = unlocked) {
                                activeStage = st; stageQIdx = 0; stageScore = 0; stageSelectedOpt = null; stageFinished = false
                            }
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("مرحلة ${st.stageNumber}", fontWeight = FontWeight.Bold, color = if (unlocked) accent else muted, fontSize = 13.sp)
                                    if (!unlocked) Icon(Icons.Default.Lock, contentDescription = null, tint = muted, modifier = Modifier.size(16.dp))
                                }
                                Text(st.titleAr, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(st.chapterTitleAr, fontSize = 10.sp, color = muted, maxLines = 1)
                                Row {
                                    repeat(3) { i ->
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (i < stars.coerceAtLeast(0)) goldAccent else muted.copy(alpha = 0.25f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // TAB 1: 60-SECOND SPEED CHALLENGE
        if (subTab == 1) {
            item {
                Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("تحدي السرعة (٦٠ ثانية)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accent)
                            Text("أفضل نتيجة: $speedBestScore", fontWeight = FontWeight.Bold, color = goldAccent, fontSize = 13.sp)
                        }
                        if (!speedRunning && !speedFinished) {
                            Text("أجب عن أكبر عدد من الأسئلة خلال ٦٠ ثانية. الإجابات المتتالية تضاعف نقاطك!", fontSize = 13.sp, color = muted)
                            Button(
                                onClick = {
                                    speedShuffled = allQuestions.shuffled()
                                    speedQIdx = 0; speedScore = 0; speedCombo = 0; speedTimeLeft = 60
                                    speedFinished = false; speedRunning = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("ابدأ التحدي الآن")
                            }
                        } else if (speedRunning) {
                            val q = speedShuffled[speedQIdx % speedShuffled.size]
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("⏱ $speedTimeLeft ثانية", fontWeight = FontWeight.Bold, color = if (speedTimeLeft <= 10) Color(0xFFDC2626) else accent, fontSize = 16.sp)
                                Text("النقاط: $speedScore · سلسلة: $speedCombo🔥", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text(q.question, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            q.options.forEachIndexed { idx, opt ->
                                OutlinedButton(
                                    onClick = {
                                        if (idx == q.correctIndex) {
                                            speedCombo++
                                            speedScore += 10 + (speedCombo * 2)
                                        } else {
                                            speedCombo = 0
                                        }
                                        speedQIdx++
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(opt, modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        } else {
                            Text("انتهى الوقت! نتيجتك: $speedScore نقطة · (+${speedScore * 2} XP)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accent)
                            Button(
                                onClick = {
                                    speedShuffled = allQuestions.shuffled()
                                    speedQIdx = 0; speedScore = 0; speedCombo = 0; speedTimeLeft = 60
                                    speedFinished = false; speedRunning = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accent)
                            ) {
                                Text("العب مرة أخرى")
                            }
                        }
                    }
                }
            }
        }

        // TAB 2: VERSE & QUOTE PUZZLES (55)
        if (subTab == 2) {
            item {
                val filteredPuzzles = puzzles.filter {
                    puzzleFilter == "الكل" ||
                        (puzzleFilter == "أكمل الآية" && it.kind == "complete_verse") ||
                        (puzzleFilter == "من القائل" && it.kind == "who_said")
                }
                val safeIdx = puzzleIndex.coerceIn(0, (filteredPuzzles.size - 1).coerceAtLeast(0))
                Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("الكل", "أكمل الآية", "من القائل").forEach { f ->
                                FilterChip(selected = puzzleFilter == f, onClick = {
                                    puzzleFilter = f; puzzleIndex = 0; puzzleSelected = null; puzzleShowHint = false
                                }, label = { Text(f, fontSize = 12.sp) })
                            }
                        }
                        if (filteredPuzzles.isNotEmpty()) {
                            val pz = filteredPuzzles[safeIdx]
                            Text("لغز ${safeIdx + 1} من ${filteredPuzzles.size}", fontSize = 12.sp, color = accent, fontWeight = FontWeight.Bold)
                            Text(pz.prompt, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 25.sp)
                            pz.options.forEachIndexed { idx, opt ->
                                val isChosen = puzzleSelected == idx
                                val isRight = idx == pz.correctIndex
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = when {
                                        puzzleSelected == null -> if (darkMode) NoorColors.DarkSurfaceAlt else NoorColors.Parchment
                                        isRight -> accent.copy(alpha = 0.2f)
                                        isChosen && !isRight -> Color(0x33DC2626)
                                        else -> if (darkMode) NoorColors.DarkSurfaceAlt else NoorColors.Parchment
                                    },
                                    border = cardBorder(darkMode),
                                    modifier = Modifier.fillMaxWidth().clickable(enabled = puzzleSelected == null) {
                                        puzzleSelected = idx
                                        if (idx == pz.correctIndex) onAwardXp(15)
                                    }
                                ) {
                                    Text(opt, modifier = Modifier.padding(12.dp), fontSize = 14.sp)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(onClick = { puzzleShowHint = !puzzleShowHint }) {
                                    Text(if (puzzleShowHint) "إخفاء التلميح" else "عرض تلميح", fontSize = 12.sp)
                                }
                                Button(onClick = {
                                    puzzleIndex = (safeIdx + 1) % filteredPuzzles.size
                                    puzzleSelected = null; puzzleShowHint = false
                                }, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                                    Text("اللغز التالي", fontSize = 12.sp)
                                }
                            }
                            if (puzzleShowHint && pz.hint.isNotBlank()) {
                                Text("تلميح: ${pz.hint}", fontSize = 12.sp, color = goldAccent)
                            }
                            if (puzzleSelected != null && pz.ref.isNotBlank()) {
                                Text("المرجع: ${pz.ref}", fontSize = 12.sp, color = muted)
                            }
                        }
                    }
                }
            }
        }

        // TAB 3: FRIEND SEED CHALLENGE
        if (subTab == 3) {
            item {
                Card(shape = CardShape, border = cardBorder(darkMode), colors = surfaceCardColors(darkMode)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("تحدي الأصدقاء برمز مشترك", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accent)
                        Text("أدخل أي رمز تحدٍ وشاركه مع صديقك لتظهر لكما نفس الأسئلة الخمسة تماماً.", fontSize = 12.sp, color = muted)
                        OutlinedTextField(
                            value = seedInput,
                            onValueChange = { seedInput = it.uppercase() },
                            label = { Text("رمز التحدي") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val code = seedInput.ifBlank { "NOOR-${(1000..9999).random()}" }
                                    seedInput = code
                                    seedQuestions = generateSeedQuestions(code)
                                    seedQIdx = 0; seedSelected = null; seedScore = 0; seedDone = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accent),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("بدء التحدي بالرمز", fontSize = 12.sp)
                            }
                            OutlinedButton(onClick = {
                                val randomCode = "NOOR-${(1000..9999).random()}"
                                seedInput = randomCode
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "أتحدّاك في مسابقة نور الإسلام! أدخل الرمز ($randomCode) في قسم تحدي الأصدقاء.")
                                }
                                context.startActivity(Intent.createChooser(share, "مشاركة رمز التحدي"))
                            }) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("توليد ومشاركة", fontSize = 12.sp)
                            }
                        }

                        if (seedQuestions.isNotEmpty() && !seedDone) {
                            HorizontalDivider()
                            val sq = seedQuestions[seedQIdx]
                            Text("سؤال ${seedQIdx + 1} من ${seedQuestions.size} · الرمز: $seedInput", fontSize = 12.sp, color = goldAccent, fontWeight = FontWeight.Bold)
                            Text(sq.question, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            sq.options.forEachIndexed { i, o ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = when {
                                        seedSelected == null -> if (darkMode) NoorColors.DarkSurfaceAlt else NoorColors.Parchment
                                        i == sq.correctIndex -> accent.copy(alpha = 0.2f)
                                        seedSelected == i -> Color(0x33DC2626)
                                        else -> if (darkMode) NoorColors.DarkSurfaceAlt else NoorColors.Parchment
                                    },
                                    border = cardBorder(darkMode),
                                    modifier = Modifier.fillMaxWidth().clickable(enabled = seedSelected == null) {
                                        seedSelected = i
                                        if (i == sq.correctIndex) seedScore++
                                    }
                                ) {
                                    Text(o, modifier = Modifier.padding(12.dp), fontSize = 14.sp)
                                }
                            }
                            if (seedSelected != null) {
                                Button(onClick = {
                                    if (seedQIdx < seedQuestions.lastIndex) {
                                        seedQIdx++; seedSelected = null
                                    } else {
                                        seedDone = true
                                        onAwardXp(seedScore * 15)
                                    }
                                }, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                                    Text(if (seedQIdx < seedQuestions.lastIndex) "التالي" else "إنهاء التحدي")
                                }
                            }
                        } else if (seedDone) {
                            HorizontalDivider()
                            Text("أنهيت تحدي ($seedInput) بنتيجة $seedScore من ${seedQuestions.size}! (+${seedScore * 15} XP)", fontWeight = FontWeight.Bold, color = accent)
                            Button(onClick = {
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "حصلت على $seedScore من ${seedQuestions.size} في تحدي نور الإسلام ($seedInput)! هل يمكنك هزيمتي؟")
                                }
                                context.startActivity(Intent.createChooser(share, "مشاركة النتيجة"))
                            }, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("مشاركة نتيجتي")
                            }
                        }
                    }
                }
            }
        }
    }
}
