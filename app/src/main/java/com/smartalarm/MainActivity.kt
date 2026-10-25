package com.smartalarm

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SmartAlarmTheme { SmartAlarmAppContent() } }
    }

    override fun onResume() {
        super.onResume()
        if (AlarmForegroundService.isRinging) {
            try {
                startActivity(
                    Intent(this, ChallengeActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
            } catch (_: Exception) {
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Data Models & Catalogs
// ---------------------------------------------------------------------------

data class Ringtone(
    val resId: Int,
    val name: String,
    val desc: String,
    val category: String = "Classic"
)

data class Task(
    val id: Int,
    val name: String,
    val desc: String,
    val difficulty: String = "Medium",
    val category: String = "Mental"
)

private fun allRingtones(): List<Ringtone> = listOf(
    Ringtone(0, "System Default", "Phone standard alarm tone", "Classic"),
    Ringtone(R.raw.rooster, "Rooster Morning", "Crisp crowing chirp", "Energetic"),
    Ringtone(R.raw.radar, "Radar", "Classic ping pattern", "Loud"),
    Ringtone(R.raw.argon, "Argon", "Soft synthesizer pad 880Hz", "Gentle"),
    Ringtone(R.raw.digital_alarm, "Digital Beep", "1200Hz rapid alert", "Loud"),
    Ringtone(R.raw.morning_flower, "Morning Flower", "Arpeggio 523Hz harmony", "Gentle"),
    Ringtone(R.raw.air_raid, "Air Raid", "Siren sweep emergency", "Loud"),
    Ringtone(R.raw.classic_bell, "Classic Bell", "Metallic twin-bell decay", "Classic"),
    Ringtone(R.raw.military, "Military Reveille", "Upbeat bugle march", "Energetic"),
    Ringtone(R.raw.electronic, "Electronic Pulse", "1400Hz frequency burst", "Loud"),
    Ringtone(R.raw.extreme_alarm, "Extreme Loud", "1500Hz urgent wake-up", "Loud"),
    Ringtone(R.raw.harp, "Gentle Harp", "659Hz calming strings", "Gentle"),
    Ringtone(R.raw.kettle, "Kettle", "Whistle alarm pitch", "Classic"),
    Ringtone(R.raw.motivational_beat, "Motivational Beat", "440Hz triple rhythm", "Energetic"),
    Ringtone(R.raw.inspiration, "Inspiration", "550Hz uplifting melody", "Energetic"),
    Ringtone(R.raw.energizer, "Energizer", "1100Hz high energy", "Energetic"),
    Ringtone(R.raw.sunrise, "Sunrise", "400Hz warm morning rise", "Gentle"),
    Ringtone(R.raw.uplift, "Uplift", "700Hz rising synth chords", "Energetic"),
    Ringtone(R.raw.workout_alarm, "Workout Alarm", "1300Hz power boost", "Energetic"),
    Ringtone(R.raw.alarm_clock_old, "Old Clock", "Mechanical pendulum ticks", "Classic"),
    Ringtone(R.raw.nuclear, "Nuclear Siren", "500Hz ominous slow sweep", "Loud"),
    Ringtone(R.raw.battle_alarm, "Battle Horn", "300Hz commanding brass", "Loud"),
    Ringtone(R.raw.church_bell, "Church Bell", "520Hz resonant chime", "Classic"),
    Ringtone(R.raw.ocean_wave, "Ocean Wave", "250Hz soothing drone", "Gentle"),
    Ringtone(R.raw.bird_chirp, "Bird Chirp", "2000Hz natural chirps", "Gentle"),
    Ringtone(R.raw.gentle_wake, "Gentle Wake", "432Hz zen frequency", "Gentle")
)

private fun allTasks(): List<Task> = listOf(
    Task(1, "Eye Tracking 60s", "Keep eyes open and focused for 60s", "Hard", "Sensors"),
    Task(2, "Math Easy", "Solve 3 single-digit arithmetic sums", "Easy", "Math"),
    Task(3, "Math Medium", "Solve 3 two-digit arithmetic problems", "Medium", "Math"),
    Task(4, "Math Hard", "Solve 10 mixed hard math problems (+, −)", "Hard", "Math"),
    Task(5, "QR Code", "Scan preset QR code in your kitchen/bathroom", "Medium", "Sensors"),
    Task(6, "NFC Tap", "Tap an NFC tag placed away from bed", "Easy", "Sensors"),
    Task(7, "Shake 50x", "Vigorously shake phone 50 times", "Hard", "Physical"),
    Task(8, "Walk 30 Steps", "Get out of bed and walk 30 steps", "Medium", "Physical"),
    Task(9, "Squats 10x", "Perform 10 full squats holding phone", "Hard", "Physical"),
    Task(10, "Typing Phrase", "Retype a morning phrase accurately", "Medium", "Mental"),
    Task(11, "Memory Sequence", "Simon says pattern game for 5 rounds", "Hard", "Mental"),
    Task(12, "Puzzle Slider", "Rearrange 3x3 numbered tiles slider", "Hard", "Mental"),
    Task(13, "Voice Repeat", "Speak the motivational phrase aloud", "Medium", "Mental"),
    Task(14, "Smile 5s", "Look at camera and hold a genuine smile", "Easy", "Sensors"),
    Task(15, "Photo Challenge", "Snap a photo of bathroom or kitchen sink", "Medium", "Sensors"),
    Task(16, "Light Sensor", "Expose phone to bright light (>500 lux)", "Medium", "Sensors"),
    Task(17, "Word Scramble", "Unscramble 3 morning vocabulary words", "Medium", "Mental"),
    Task(18, "Pattern Draw", "Connect dots following sample pattern", "Easy", "Mental"),
    Task(19, "Trivia Quiz", "Answer 3 general knowledge trivia questions", "Medium", "Mental"),
    Task(20, "Breath Hold", "Hold phone steady while taking a deep breath", "Easy", "Physical")
)

private fun taskIcon(id: Int): ImageVector = when (id) {
    1 -> Icons.Default.Visibility
    2 -> Icons.Default.CheckCircleOutline
    3 -> Icons.Default.Calculate
    4 -> Icons.Default.Bolt
    5 -> Icons.Default.QrCode
    6 -> Icons.Default.CloudUpload
    7 -> Icons.Default.Vibration
    8 -> Icons.AutoMirrored.Filled.DirectionsWalk
    9 -> Icons.Default.FitnessCenter
    10 -> Icons.Default.Keyboard
    11 -> Icons.Default.GridOn
    12 -> Icons.Default.Extension
    13 -> Icons.Default.Mic
    14 -> Icons.Default.Mood
    15 -> Icons.Default.PhotoCamera
    16 -> Icons.Default.LightMode
    17 -> Icons.Default.Translate
    18 -> Icons.Default.Edit
    19 -> Icons.AutoMirrored.Filled.HelpOutline
    20 -> Icons.Default.FavoriteBorder
    else -> Icons.Default.TaskAlt
}

// ---------------------------------------------------------------------------
// Theme
// ---------------------------------------------------------------------------

@Composable
fun SmartAlarmTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val light = lightColorScheme(
        primary = Color(0xFF4F46E5), // Indigo 600
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEEF2FF),
        onPrimaryContainer = Color(0xFF312E81),
        secondary = Color(0xFF0284C7), // Sky 600
        secondaryContainer = Color(0xFFE0F2FE),
        onSecondaryContainer = Color(0xFF0369A1),
        tertiary = Color(0xFF9333EA),
        tertiaryContainer = Color(0xFFF3E8FF),
        background = Color(0xFFF8FAFC),
        surface = Color.White,
        surfaceVariant = Color(0xFFF1F5F9),
        outline = Color(0xFFE2E8F0)
    )
    val darkScheme = darkColorScheme(
        primary = Color(0xFF818CF8),
        onPrimary = Color(0xFF1E1B4B),
        primaryContainer = Color(0xFF312E81),
        onPrimaryContainer = Color(0xFFEEF2FF),
        secondary = Color(0xFF38BDF8),
        secondaryContainer = Color(0xFF075985),
        tertiary = Color(0xFFC084FC),
        tertiaryContainer = Color(0xFF581C87),
        background = Color(0xFF0B0F19),
        surface = Color(0xFF151C2C),
        surfaceVariant = Color(0xFF1E293B),
        outline = Color(0xFF334155)
    )
    MaterialTheme(
        colorScheme = if (dark) darkScheme else light,
        content = content
    )
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

fun loadRandomQuote(ctx: Context): String {
    return try {
        val json = ctx.assets.open("quotes.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        if (arr.length() == 0) return "Every morning is a clean slate. Seize the day!"
        val idx = (0 until arr.length()).random()
        arr.optString(idx, "").trim().ifBlank { "Every morning is a clean slate. Seize the day!" }
    } catch (_: Exception) {
        "Every morning is a clean slate. Seize the day!"
    }
}

private fun formatClock(hour: Int, min: Int): String =
    String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), min.coerceIn(0, 59))

private fun amPm(hour: Int): String = if (hour < 12) "AM" else "PM"

private fun displayHour12(hour: Int): Int {
    val h = hour % 12
    return if (h == 0) 12 else h
}

private fun countdownText(triggerAt: Long): String {
    if (triggerAt <= 0L) return "disabled"
    val diff = (triggerAt - System.currentTimeMillis()).coerceAtLeast(0)
    val h = diff / 3_600_000
    val m = (diff % 3_600_000) / 60_000
    return when {
        h > 0 -> "in ${h}h ${m}m"
        m > 0 -> "in ${m}m"
        else -> "very soon"
    }
}

// ---------------------------------------------------------------------------
// Main Screen Content
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAlarmAppContent() {
    val ctx = LocalContext.current
    val appCtx = remember(ctx) { ctx.applicationContext }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    fun toast(msg: String) {
        scope.launch { snackbar.showSnackbar(msg) }
    }

    // Alarm list state
    var alarmsList by remember { mutableStateOf(AlarmScheduler.loadAll(appCtx)) }
    fun refreshAlarms() {
        alarmsList = AlarmScheduler.loadAll(appCtx)
    }

    val nextAlarm = remember(alarmsList) {
        AlarmScheduler.getNextUpcomingAlarm(alarmsList)
    }

    // Current navigation tab: 0 = Alarms, 1 = Challenges, 2 = Sounds, 3 = Quote
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Alarm Editor Dialog state
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var alarmToEdit by remember { mutableStateOf<AlarmItem?>(null) }

    // Ringtones & Tasks catalogs
    val ringtones = remember { allRingtones() }
    val tasks = remember { allTasks() }

    // Quote & Text-to-Speech
    var quote by rememberSaveable { mutableStateOf(loadRandomQuote(appCtx)) }
    val ttsHolder = remember { arrayOfNulls<TextToSpeech>(1) }
    var ttsReady by remember { mutableStateOf(false) }
    val pendingSpeak = remember { arrayOfNulls<String>(1) }

    // Ringtone audio preview
    val previewHolder = remember { arrayOfNulls<MediaPlayer>(1) }
    var playingRes by remember { mutableIntStateOf(-1) }

    fun stopAudio() {
        try {
            previewHolder[0]?.let { p ->
                try { if (p.isPlaying) p.stop() } catch (_: Exception) {}
                try { p.release() } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        previewHolder[0] = null
        playingRes = -1
    }

    fun playPreview(resId: Int) {
        stopAudio()
        if (resId == 0) {
            // System default tone: preview radar as representative sound
            val mp = try { MediaPlayer.create(appCtx, R.raw.radar) } catch (_: Exception) { null }
            if (mp != null) {
                previewHolder[0] = mp
                playingRes = 0
                mp.setOnCompletionListener {
                    try { it.release() } catch (_: Exception) {}
                    if (previewHolder[0] === it) previewHolder[0] = null
                    playingRes = -1
                }
                mp.start()
            }
            return
        }
        val mp = try { MediaPlayer.create(appCtx, resId) } catch (_: Exception) { null }
        if (mp == null) {
            toast("Couldn't play sound preview")
            return
        }
        previewHolder[0] = mp
        playingRes = resId
        mp.setOnCompletionListener {
            try { it.release() } catch (_: Exception) {}
            if (previewHolder[0] === it) previewHolder[0] = null
            playingRes = -1
        }
        mp.start()
    }

    fun speak(text: String) {
        try {
            val t = ttsHolder[0]
            if (t == null || !ttsReady) {
                pendingSpeak[0] = text
                toast("Voice warming up… will speak shortly")
                return
            }
            t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "quote")
        } catch (_: Exception) {
            toast("Couldn't speak right now")
        }
    }

    // TTS Lifecycle
    DisposableEffect(Unit) {
        val engine = try {
            TextToSpeech(appCtx) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    try {
                        val langRes = ttsHolder[0]?.setLanguage(Locale.US)
                        if (langRes == TextToSpeech.LANG_MISSING_DATA || langRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                            ttsHolder[0]?.language = Locale.getDefault()
                        }
                        ttsHolder[0]?.setSpeechRate(1.0f)
                    } catch (_: Exception) {}
                    ttsReady = true
                    pendingSpeak[0]?.let { queued ->
                        pendingSpeak[0] = null
                        try { ttsHolder[0]?.speak(queued, TextToSpeech.QUEUE_FLUSH, null, "quote") } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) { null }
        ttsHolder[0] = engine

        onDispose {
            stopAudio()
            try { engine?.shutdown() } catch (_: Exception) {}
            ttsHolder[0] = null
        }
    }

    // Notification Permission Handling (Android 13+)
    val notifGranted = remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    appCtx, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted.value = granted
        if (!granted) toast("Notifications denied — banner may be hidden when alarm rings")
    }

    // Exact Alarm Check
    fun checkExactAlarmPermission() {
        val am = appCtx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (!am.canScheduleExactAlarms()) {
                    toast("Allow exact alarms in Settings for maximum punctuality")
                    appCtx.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:${appCtx.packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            } catch (_: Exception) {}
        }
    }

    // Test Ring Helper
    fun testRingAlarm(ringtoneRes: Int, taskId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifGranted.value) {
            try { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) } catch (_: Exception) {}
        }
        stopAudio()
        try {
            val i = Intent(appCtx, AlarmForegroundService::class.java)
                .putExtra(AlarmForegroundService.EXTRA_RINGTONE_RES, ringtoneRes)
                .putExtra(AlarmForegroundService.EXTRA_TASK_ID, taskId)
            ContextCompat.startForegroundService(appCtx, i)
            toast("Alarm ringing! Solve challenge to stop.")
            appCtx.startActivity(
                Intent(appCtx, ChallengeActivity::class.java)
                    .putExtra(ChallengeActivity.EXTRA_TASK_ID, taskId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            toast("Couldn't start test alarm: ${e.message}")
        }
    }

    fun stopRingingService() {
        stopAudio()
        if (AlarmForegroundService.isRinging) {
            toast("Alarm is ringing! Solve challenge to stop.")
            try {
                appCtx.startActivity(
                    Intent(appCtx, ChallengeActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
            } catch (_: Exception) {}
            return
        }
        try {
            appCtx.stopService(Intent(appCtx, AlarmForegroundService::class.java))
            toast("Stopped")
        } catch (_: Exception) {}
    }

    val activeCount = alarmsList.count { it.enabled }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            if (activeCount > 0) Icons.Default.AlarmOn else Icons.Default.Alarm,
                            contentDescription = "Alarms"
                        )
                    },
                    label = { Text("Alarms", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Extension, contentDescription = "Missions") },
                    label = { Text("Missions", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.MusicNote, contentDescription = "Sounds") },
                    label = { Text("Sounds", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.FormatQuote, contentDescription = "Quotes") },
                    label = { Text("Daily Quote", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        checkExactAlarmPermission()
                        alarmToEdit = null
                        showEditor = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Alarm", modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Alarm", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                    }
                }
            }
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            // ---- Premium Top Header Bar ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Alarm,
                            contentDescription = "Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Finch Smart Alarm",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        if (activeCount == 0) "No active alarms"
                        else "$activeCount alarm${if (activeCount > 1) "s" else ""} scheduled",
                        fontSize = 12.5.sp,
                        color = if (activeCount > 0) MaterialTheme.colorScheme.primary else Color(0xFF64748B),
                        fontWeight = if (activeCount > 0) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                if (AlarmForegroundService.isRinging) {
                    Button(
                        onClick = { stopRingingService() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Stop Ringing", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    IconButton(onClick = { speak(quote) }) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Speak quote",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // ---- Notification Nudge (Android 13+) ----
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifGranted.value) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F8FF)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFBAE6FD))
                ) {
                    ListItem(
                        headlineContent = { Text("Enable Notifications", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                        supportingContent = { Text("Required for reliable alarm ringing banner.", fontSize = 12.sp) },
                        leadingContent = {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF0284C7))
                        },
                        trailingContent = {
                            Button(
                                onClick = {
                                    try {
                                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } catch (_: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) { Text("Allow", fontSize = 12.5.sp) }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            // ---- Dynamic Content per Tab ----
            when (selectedTab) {
                0 -> AlarmsTabContent(
                    alarms = alarmsList,
                    nextAlarm = nextAlarm,
                    onToggle = { alarm, enable ->
                        checkExactAlarmPermission()
                        val newTrigger = AlarmScheduler.toggleAlarm(appCtx, alarm.id, enable)
                        refreshAlarms()
                        if (enable) {
                            toast("Alarm enabled • ${countdownText(newTrigger)}")
                        } else {
                            toast("Alarm turned off")
                        }
                    },
                    onEdit = { alarm ->
                        alarmToEdit = alarm
                        showEditor = true
                    },
                    onDelete = { alarm ->
                        AlarmScheduler.deleteAlarm(appCtx, alarm.id)
                        refreshAlarms()
                        toast("Alarm deleted")
                    },
                    onTest = { alarm ->
                        testRingAlarm(alarm.ringtoneRes, alarm.taskId)
                    },
                    onAddNew = {
                        checkExactAlarmPermission()
                        alarmToEdit = null
                        showEditor = true
                    }
                )

                1 -> ChallengesTabContent(
                    tasks = tasks,
                    onTestChallenge = { taskId ->
                        try {
                            appCtx.startActivity(
                                Intent(appCtx, ChallengeActivity::class.java)
                                    .putExtra(ChallengeActivity.EXTRA_TASK_ID, taskId)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                            toast("Couldn't launch challenge preview")
                        }
                    },
                    onCreateAlarmWithTask = { taskId ->
                        checkExactAlarmPermission()
                        alarmToEdit = AlarmItem(
                            id = UUID.randomUUID().toString(),
                            hour = 7,
                            min = 0,
                            label = ChallengeActivity.taskDisplayName(taskId),
                            taskId = taskId,
                            days = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY),
                            enabled = true
                        )
                        showEditor = true
                    }
                )

                2 -> SoundsTabContent(
                    ringtones = ringtones,
                    playingRes = playingRes,
                    onPlayPreview = { playPreview(it) },
                    onStopPreview = { stopAudio() },
                    onTestFullAlarm = { resId ->
                        testRingAlarm(resId, 4)
                    }
                )

                3 -> MotivationTabContent(
                    quote = quote,
                    onRefresh = { quote = loadRandomQuote(appCtx) },
                    onSpeak = { speak(quote) }
                )
            }
        }
    }

    // ---- Alarm Editor Dialog ----
    if (showEditor) {
        AlarmEditorDialog(
            initialAlarm = alarmToEdit,
            ringtones = ringtones,
            tasks = tasks,
            playingRes = playingRes,
            onPlayRingtone = { playPreview(it) },
            onStopRingtone = { stopAudio() },
            onDismiss = {
                stopAudio()
                showEditor = false
            },
            onSave = { savedAlarm ->
                stopAudio()
                checkExactAlarmPermission()
                val trigger = AlarmScheduler.saveAlarm(appCtx, savedAlarm)
                refreshAlarms()
                showEditor = false
                toast("Saved! Alarm set for ${formatClock(savedAlarm.hour, savedAlarm.min)} (${countdownText(trigger)})")
            },
            onDelete = { idToDelete ->
                stopAudio()
                AlarmScheduler.deleteAlarm(appCtx, idToDelete)
                refreshAlarms()
                showEditor = false
                toast("Alarm deleted")
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Alarms Tab Content: Hero Banner + Multiple Alarms List
// ---------------------------------------------------------------------------

@Composable
fun AlarmsTabContent(
    alarms: List<AlarmItem>,
    nextAlarm: AlarmItem?,
    onToggle: (AlarmItem, Boolean) -> Unit,
    onEdit: (AlarmItem) -> Unit,
    onDelete: (AlarmItem) -> Unit,
    onTest: (AlarmItem) -> Unit,
    onAddNew: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
    ) {
        // ---- Hero Next Alarm Card ----
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF4F46E5), // Indigo
                                    Color(0xFF3B82F6), // Blue
                                    Color(0xFF06B6D4)  // Cyan
                                )
                            )
                        )
                        .padding(horizontal = 22.dp, vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (nextAlarm != null) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.22f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Alarm,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "NEXT ALARM • ${countdownText(nextAlarm.triggerAt)}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    formatClock(nextAlarm.hour, nextAlarm.min),
                                    color = Color.White,
                                    fontSize = 58.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-1).sp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    amPm(nextAlarm.hour),
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(top = 10.dp)
                                )
                            }
                            Text(
                                "${nextAlarm.label} • ${nextAlarm.daysSummary()}",
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(10.dp))
                            // Badges for assigned mission and tone
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.18f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            taskIcon(nextAlarm.taskId),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            ChallengeActivity.taskDisplayName(nextAlarm.taskId),
                                            color = Color.White,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.18f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        val toneName = allRingtones().firstOrNull { it.resId == nextAlarm.ringtoneRes }?.name ?: "System Default"
                                        Text(
                                            toneName,
                                            color = Color.White,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        } else {
                            // Empty state when no alarms are enabled
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "No Active Alarms",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Turn on an existing alarm or create a new one below.",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = onAddNew,
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF4F46E5)
                                )
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Create First Alarm", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // ---- Alarms Header ----
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Your Alarms",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "${alarms.size}",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                TextButton(onClick = onAddNew) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Alarm", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ---- Alarms List ----
        if (alarms.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Alarm,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No Alarms Found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap + Add Alarm to create your wake-up schedule.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(alarms, key = { it.id }) { alarm ->
                AlarmItemCard(
                    alarm = alarm,
                    onToggle = { onToggle(alarm, it) },
                    onEdit = { onEdit(alarm) },
                    onDelete = { onDelete(alarm) },
                    onTest = { onTest(alarm) }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Single Alarm Card Component
// ---------------------------------------------------------------------------

@Composable
fun AlarmItemCard(
    alarm: AlarmItem,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    val toneName = remember(alarm.ringtoneRes) {
        allRingtones().firstOrNull { it.resId == alarm.ringtoneRes }?.name ?: "System Default"
    }
    val taskName = remember(alarm.taskId) {
        ChallengeActivity.taskDisplayName(alarm.taskId)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (alarm.enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outline
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (alarm.enabled) 3.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top: Time + Label + Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            formatClock(alarm.hour, alarm.min),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (alarm.enabled) MaterialTheme.colorScheme.onSurface else Color(0xFF94A3B8)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            amPm(alarm.hour),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (alarm.enabled) MaterialTheme.colorScheme.primary else Color(0xFF94A3B8),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text(
                        alarm.label.ifBlank { "Alarm" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (alarm.enabled) MaterialTheme.colorScheme.onSurface else Color(0xFF94A3B8)
                    )
                }

                // Smooth M3 Switch
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFCBD5E1)
                    )
                )
            }

            Spacer(Modifier.height(10.dp))

            // Recurring Days Indicators: 7 Circles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dayOrder = listOf(
                    Calendar.MONDAY to "M",
                    Calendar.TUESDAY to "T",
                    Calendar.WEDNESDAY to "W",
                    Calendar.THURSDAY to "T",
                    Calendar.FRIDAY to "F",
                    Calendar.SATURDAY to "S",
                    Calendar.SUNDAY to "S"
                )
                if (alarm.days.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            "One-time alarm (Once)",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    dayOrder.forEach { (calDay, letter) ->
                        val isDaySelected = alarm.days.contains(calDay)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDaySelected && alarm.enabled) MaterialTheme.colorScheme.primary
                                    else if (isDaySelected) Color(0xFF94A3B8)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                letter,
                                color = if (isDaySelected) Color.White else Color(0xFF64748B),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Badges: Challenge & Ringtone
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            taskIcon(alarm.taskId),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            taskName,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFFA855F7),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            toneName,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Footer Action Buttons: Edit, Test, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onTest,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Test", fontSize = 12.5.sp)
                }
                Spacer(Modifier.width(4.dp))
                TextButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", fontSize = 12.5.sp)
                }
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Alarm",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Add / Edit Alarm Dialog Component
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditorDialog(
    initialAlarm: AlarmItem?,
    ringtones: List<Ringtone>,
    tasks: List<Task>,
    playingRes: Int,
    onPlayRingtone: (Int) -> Unit,
    onStopRingtone: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (AlarmItem) -> Unit,
    onDelete: (String) -> Unit
) {
    val isEditing = initialAlarm != null
    var hour by rememberSaveable { mutableIntStateOf(initialAlarm?.hour ?: 7) }
    var min by rememberSaveable { mutableIntStateOf(initialAlarm?.min ?: 0) }
    var label by rememberSaveable { mutableStateOf(initialAlarm?.label ?: "Morning Alarm") }
    val initialDays = remember {
        initialAlarm?.days ?: listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
    }
    var selectedDays by remember { mutableStateOf(initialDays.toSet()) }
    var selectedRingtoneRes by rememberSaveable { mutableIntStateOf(initialAlarm?.ringtoneRes ?: 0) }
    var selectedTaskId by rememberSaveable { mutableIntStateOf(initialAlarm?.taskId ?: 4) }

    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showTaskPicker by rememberSaveable { mutableStateOf(false) }
    var showTonePicker by rememberSaveable { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (isEditing) "Edit Alarm" else "New Alarm",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Close")
                        }
                    }
                }

                // Big Time Display + Change Time Button
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "TIME",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    formatClock(hour, min),
                                    fontSize = 46.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    amPm(hour),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showTimePicker = true },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Pick Time", fontSize = 13.sp)
                                }
                                OutlinedButton(
                                    onClick = { hour = (hour + 1) % 24 },
                                    shape = RoundedCornerShape(50)
                                ) { Text("+1h", fontSize = 13.sp) }
                                OutlinedButton(
                                    onClick = {
                                        val total = hour * 60 + min + 5
                                        hour = (total / 60) % 24
                                        min = total % 60
                                    },
                                    shape = RoundedCornerShape(50)
                                ) { Text("+5m", fontSize = 13.sp) }
                            }
                        }
                    }
                }

                // Label Input + Quick Suggestions
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Alarm Label", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            placeholder = { Text("e.g. Work, Workout, Medicine") }
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Work", "Gym", "Medicine", "Wake Up").forEach { suggestion ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { label = suggestion }
                                ) {
                                    Text(
                                        suggestion,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Repeat Days Selection
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Repeat Days", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Weekdays",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        selectedDays = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
                                    }
                                )
                                Text("•", fontSize = 11.sp, color = Color.Gray)
                                Text(
                                    "Once",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { selectedDays = emptySet() }
                                )
                            }
                        }

                        // 7 Day Toggle Chips
                        val dayMap = listOf(
                            Calendar.MONDAY to "M",
                            Calendar.TUESDAY to "T",
                            Calendar.WEDNESDAY to "W",
                            Calendar.THURSDAY to "T",
                            Calendar.FRIDAY to "F",
                            Calendar.SATURDAY to "S",
                            Calendar.SUNDAY to "S"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            dayMap.forEach { (calDay, letter) ->
                                val isChecked = selectedDays.contains(calDay)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isChecked) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable {
                                            selectedDays = if (isChecked) selectedDays - calDay else selectedDays + calDay
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        letter,
                                        color = if (isChecked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Challenge Mission Selector
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Dismiss Challenge", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                        val currentTask = tasks.firstOrNull { it.id == selectedTaskId } ?: tasks.first()
                        OutlinedCard(
                            onClick = { showTaskPicker = true },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    taskIcon(currentTask.id),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(currentTask.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(currentTask.desc, fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                                Text("Change", color = MaterialTheme.colorScheme.primary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Ringtone Selector
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Ringtone Sound", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                        val currentTone = ringtones.firstOrNull { it.resId == selectedRingtoneRes } ?: ringtones.first()
                        OutlinedCard(
                            onClick = { showTonePicker = true },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color(0xFFA855F7),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(currentTone.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(currentTone.desc, fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                                val isPlaying = playingRes == currentTone.resId
                                IconButton(
                                    onClick = {
                                        if (isPlaying) onStopRingtone() else onPlayRingtone(currentTone.resId)
                                    }
                                ) {
                                    Icon(
                                        if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = "Preview sound",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Buttons: Cancel, Delete (if editing), Save
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isEditing) {
                            OutlinedButton(
                                onClick = { onDelete(initialAlarm!!.id) },
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                modifier = Modifier.weight(1f)
                            ) { Text("Delete") }
                        } else {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                        }

                        Button(
                            onClick = {
                                val alarmToPersist = AlarmItem(
                                    id = initialAlarm?.id ?: UUID.randomUUID().toString(),
                                    hour = hour,
                                    min = min,
                                    label = label.ifBlank { "Alarm" },
                                    days = selectedDays.toList(),
                                    ringtoneRes = selectedRingtoneRes,
                                    taskId = selectedTaskId,
                                    enabled = true
                                )
                                onSave(alarmToPersist)
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Save Alarm", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Task Selection Dialog
    if (showTaskPicker) {
        AlertDialog(
            onDismissRequest = { showTaskPicker = false },
            title = { Text("Choose Dismiss Mission", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.height(340.dp)) {
                    items(tasks, key = { it.id }) { t ->
                        val isSel = t.id == selectedTaskId
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTaskId = t.id
                                    showTaskPicker = false
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(taskIcon(t.id), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${t.id}. ${t.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                    Text(t.desc, fontSize = 11.5.sp, color = Color(0xFF64748B))
                                }
                                if (isSel) {
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTaskPicker = false }) { Text("Close") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Ringtone Selection Dialog
    if (showTonePicker) {
        AlertDialog(
            onDismissRequest = {
                onStopRingtone()
                showTonePicker = false
            },
            title = { Text("Choose Ringtone", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.height(340.dp)) {
                    items(ringtones, key = { it.resId }) { r ->
                        val isSel = r.resId == selectedRingtoneRes
                        val isPlaying = playingRes == r.resId
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedRingtoneRes = r.resId
                                    onPlayRingtone(r.resId)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isPlaying) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.MusicNote,
                                    null,
                                    tint = if (isPlaying) MaterialTheme.colorScheme.primary else Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(r.name, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                    Text(r.desc, fontSize = 11.5.sp, color = Color(0xFF64748B))
                                }
                                if (isSel) {
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onStopRingtone()
                        showTonePicker = false
                    }
                ) { Text("Done") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Material 3 TimePicker Dialog
    if (showTimePicker) {
        val tpState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = min,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Set Alarm Time", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = tpState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        hour = tpState.hour
                        min = tpState.minute
                        showTimePicker = false
                    }
                ) { Text("OK", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Challenges Tab Content: Catalog of 20 wake-up missions + Interactive Test
// ---------------------------------------------------------------------------

@Composable
fun ChallengesTabContent(
    tasks: List<Task>,
    onTestChallenge: (Int) -> Unit,
    onCreateAlarmWithTask: (Int) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }

    val categories = listOf("All", "Math", "Physical", "Sensors", "Mental")

    val filteredTasks = remember(tasks, searchQuery, selectedCategory) {
        tasks.filter { t ->
            val matchCat = selectedCategory == "All" || t.category.equals(selectedCategory, ignoreCase = true)
            val matchQuery = searchQuery.isBlank() ||
                t.name.contains(searchQuery, ignoreCase = true) ||
                t.desc.contains(searchQuery, ignoreCase = true)
            matchCat && matchQuery
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
    ) {
        item {
            Text(
                "Wake-Up Challenges",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "When your alarm rings, solve your chosen mission to dismiss it. No accidental snoozing!",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search 20 challenges… e.g. math, squat, shake") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        // Category Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.5.sp) },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // Challenge Cards
        items(filteredTasks, key = { it.id }) { t ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    taskIcon(t.id),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${t.id}. ${t.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(8.dp))
                                val diffColor = when (t.difficulty) {
                                    "Easy" -> Color(0xFF10B981)
                                    "Medium" -> Color(0xFFF59E0B)
                                    else -> Color(0xFFEF4444)
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = diffColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        t.difficulty,
                                        color = diffColor,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(t.desc, fontSize = 12.5.sp, color = Color(0xFF64748B))
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onTestChallenge(t.id) },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Try Challenge", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { onCreateAlarmWithTask(t.id) },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1.2f),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Use in Alarm", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Sounds Tab Content: 25 High-Quality Tones + Live Preview Station
// ---------------------------------------------------------------------------

@Composable
fun SoundsTabContent(
    ringtones: List<Ringtone>,
    playingRes: Int,
    onPlayPreview: (Int) -> Unit,
    onStopPreview: () -> Unit,
    onTestFullAlarm: (Int) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }

    val categories = listOf("All", "Gentle", "Energetic", "Loud", "Classic")

    val filtered = remember(ringtones, searchQuery, selectedCategory) {
        ringtones.filter { r ->
            val matchCat = selectedCategory == "All" || r.category.equals(selectedCategory, ignoreCase = true)
            val matchQuery = searchQuery.isBlank() ||
                r.name.contains(searchQuery, ignoreCase = true) ||
                r.desc.contains(searchQuery, ignoreCase = true)
            matchCat && matchQuery
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
    ) {
        item {
            Text(
                "Ringtone Audio Station",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "25 bundled offline ringtones crafted for wakefulness + System Default.",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search tones… e.g. radar, bell, siren, harp") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        // Category Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.5.sp) },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // List of Ringtones
        items(filtered, key = { it.resId }) { r ->
            val isPlaying = playingRes == r.resId
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    1.dp,
                    if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isPlaying) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (isPlaying) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                r.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF64748B).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    r.category,
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            if (isPlaying) "Playing now… tap stop to pause" else r.desc,
                            fontSize = 12.sp,
                            color = if (isPlaying) MaterialTheme.colorScheme.primary else Color(0xFF64748B)
                        )
                    }

                    // Test Ring with this tone button
                    IconButton(
                        onClick = { onTestFullAlarm(r.resId) }
                    ) {
                        Icon(
                            Icons.Default.Alarm,
                            contentDescription = "Test Full Alarm",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Play/Stop Preview Button
                    IconButton(
                        onClick = {
                            if (isPlaying) onStopPreview() else onPlayPreview(r.resId)
                        }
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Stop" else "Play",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Motivation Tab Content: Inspiring Quotes + TTS Audio
// ---------------------------------------------------------------------------

@Composable
fun MotivationTabContent(
    quote: String,
    onRefresh: () -> Unit,
    onSpeak: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "Morning Motivation",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    "“$quote”",
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 26.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onSpeak,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Speak Voice", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }

                    OutlinedButton(
                        onClick = onRefresh,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("New Quote", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                    }
                }
            }
        }
    }
}
