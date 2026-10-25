package com.smartalarm

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

class ChallengeActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TASK_ID = "TASK_ID"
        const val EXTRA_ALARM_ID = "ALARM_ID"

        fun taskDisplayName(id: Int): String = when (id.coerceIn(1, 20)) {
            1 -> "Eye Tracking 60s"
            2 -> "Math Easy"
            3 -> "Math Medium"
            4 -> "Math Hard (10 Questions)"
            5 -> "QR Code"
            6 -> "NFC Tap"
            7 -> "Shake 50x"
            8 -> "Walk 30 Steps"
            9 -> "Squats 10x"
            10 -> "Typing Phrase"
            11 -> "Memory Sequence"
            12 -> "Puzzle Slider"
            13 -> "Voice Repeat"
            14 -> "Smile 5s"
            15 -> "Photo Challenge"
            16 -> "Light Sensor"
            17 -> "Word Scramble"
            18 -> "Pattern Draw"
            19 -> "Trivia Quiz"
            20 -> "Breath Hold"
            else -> "Dismiss Task"
        }

        fun taskDescription(id: Int): String = when (id.coerceIn(1, 20)) {
            1 -> "Stay awake 60s • tap 6 times"
            2 -> "Solve 3 easy sums"
            3 -> "Solve 3 medium sums"
            4 -> "Solve 10 hard math problems (+, −)"
            5 -> "Type your bathroom code WAKE-2026"
            6 -> "Press & hold 3s (NFC tap)"
            7 -> "Shake phone 50 times"
            8 -> "Walk 30 steps with phone"
            9 -> "Do 10 squats holding phone"
            10 -> "Retype the phrase exactly"
            11 -> "Repeat the Simon sequence (5 rounds)"
            12 -> "Solve the 3x3 slider"
            13 -> "Say the phrase out loud"
            14 -> "Press & hold smile 5s"
            15 -> "Take a photo (e.g. bathroom)"
            16 -> "Shine bright light (>500 lux)"
            17 -> "Unscramble 3 words"
            18 -> "Draw the pattern twice"
            19 -> "Answer 3 trivia correctly"
            20 -> "Hold phone steady 10s"
            else -> ""
        }

        fun launch(ctx: Context, taskId: Int) {
            try {
                ctx.startActivity(
                    Intent(ctx, ChallengeActivity::class.java)
                        .putExtra(EXTRA_TASK_ID, taskId.coerceIn(1, 20))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
            } catch (_: Exception) {
            }
        }
    }

    private var taskId: Int = 4
    private var alarmId: String? = null
    private var solved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskId = (intent?.getIntExtra(EXTRA_TASK_ID, 4) ?: 4).coerceIn(1, 20)
        alarmId = intent?.getStringExtra(EXTRA_ALARM_ID)
        // If launched without an explicit task (e.g. deep link), fall back to persisted alarm.
        if (intent?.hasExtra(EXTRA_TASK_ID) != true) {
            try {
                taskId = AlarmScheduler.load(applicationContext).taskId.coerceIn(1, 20)
            } catch (_: Exception) {
            }
        }
        // Show over lock screen + turn screen on so the challenge is visible.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (_: Exception) {
        }
        // Lock volume to maximum immediately
        enforceMaxVolume()
        // Back press must not bypass the task.
        try {
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Toast.makeText(
                        this@ChallengeActivity,
                        "Complete the task to dismiss the alarm",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        } catch (_: Exception) {
        }
        setContent {
            SmartAlarmTheme {
                ChallengeScreen(taskId = taskId, onSolved = { solveAndFinish() })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enforceMaxVolume()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enforceMaxVolume()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_MUTE -> {
                enforceMaxVolume()
                if (event.action == KeyEvent.ACTION_DOWN) {
                    Toast.makeText(
                        this,
                        "Volume is locked! You must solve the challenge to silence the alarm.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_VOLUME_MUTE ||
            keyCode == KeyEvent.KEYCODE_VOLUME_UP
        ) {
            enforceMaxVolume()
            Toast.makeText(
                this,
                "Volume is locked! You must solve the challenge to silence the alarm.",
                Toast.LENGTH_SHORT
            ).show()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun enforceMaxVolume() {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
        } catch (_: Exception) {
        }
    }

    private fun solveAndFinish() {
        if (solved) return
        solved = true
        try {
            AlarmScheduler.markConsumed(applicationContext, alarmId)
        } catch (_: Exception) {
        }
        try {
            // Ask the ringing service to stop + drop its notification, verified as solved
            val stop = Intent(applicationContext, AlarmForegroundService::class.java)
                .setAction(AlarmForegroundService.ACTION_STOP)
                .putExtra(AlarmForegroundService.EXTRA_TASK_SOLVED, true)
                .putExtra(AlarmForegroundService.EXTRA_ALARM_ID, alarmId)
            try {
                applicationContext.startService(stop)
            } catch (_: Exception) {
            }
            try {
                applicationContext.stopService(
                    Intent(applicationContext, AlarmForegroundService::class.java)
                )
            } catch (_: Exception) {
            }
        } catch (_: Exception) {
        }
        try {
            Toast.makeText(this, "Challenge solved! Alarm dismissed — good morning!", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
        }
        try {
            finish()
        } catch (_: Exception) {
        }
    }
}

// ---------------------------------------------------------------------------
// Screen shell
// ---------------------------------------------------------------------------

@Composable
private fun ChallengeScreen(taskId: Int, onSolved: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    fun toast(msg: String) {
        scope.launch { snackbar.showSnackbar(msg) }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Color(0xFFF8FAFC)
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4F46E5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "ALARM RINGING • VOLUME LOCKED",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        ChallengeActivity.taskDisplayName(taskId),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        ChallengeActivity.taskDescription(taskId),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp
                    )
                }
            }
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(Modifier.padding(16.dp)) {
                    TaskBody(taskId = taskId, onSolved = onSolved, toast = ::toast)
                }
            }
            VolumeLockedBadge()
        }
    }
}

@Composable
private fun TaskBody(taskId: Int, onSolved: () -> Unit, toast: (String) -> Unit) {
    when (taskId) {
        1 -> EyeTask(onSolved)
        2 -> MathTask(level = 0, onSolved = onSolved)
        3 -> MathTask(level = 1, onSolved = onSolved)
        4 -> MathTask(level = 2, onSolved = onSolved)
        5 -> QrTask(onSolved)
        6 -> HoldTask(seconds = 3, label = "Press & hold 3s", hint = "Simulates tapping your NFC tag. Stick a tag by your sink for the real habit.", onSolved = onSolved)
        7 -> ShakeTask(required = 50, onSolved = onSolved, toast = toast)
        8 -> StepsTask(required = 30, onSolved = onSolved, toast = toast)
        9 -> SquatsTask(required = 10, onSolved = onSolved, toast = toast)
        10 -> TypingTask(onSolved)
        11 -> SimonTask(onSolved)
        12 -> SliderTask(onSolved)
        13 -> VoiceTask(onSolved, toast)
        14 -> HoldTask(seconds = 5, label = "Hold smile 5s", hint = "Press and hold while smiling into the mirror.", onSolved = onSolved)
        15 -> PhotoTask(onSolved, toast)
        16 -> LightTask(onSolved, toast)
        17 -> ScrambleTask(onSolved)
        18 -> PatternTask(onSolved)
        19 -> TriviaTask(onSolved)
        20 -> BreathTask(onSolved, toast)
        else -> MathTask(level = 0, onSolved = onSolved)
    }
}

// ---------------------------------------------------------------------------
// Shared: press-and-hold button with progress
// ---------------------------------------------------------------------------

@Composable
private fun HoldButton(seconds: Int, label: String, onComplete: () -> Unit) {
    var holding by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val totalMs = seconds * 1000L
    LaunchedEffect(holding) {
        if (!holding) {
            progress = 0f
            return@LaunchedEffect
        }
        val start = System.currentTimeMillis()
        while (holding) {
            val elapsed = System.currentTimeMillis() - start
            progress = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
            if (elapsed >= totalMs) {
                holding = false
                progress = 1f
                onComplete()
                break
            }
            delay(50)
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(if (holding) Color(0xFF4F46E5) else Color(0xFFEEF2FF))
                .border(1.dp, Color(0xFF6366F1), RoundedCornerShape(50))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            holding = true
                            try {
                                awaitRelease()
                            } finally {
                                holding = false
                            }
                        }
                    )
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (holding) "Keep holding… ${(progress * 100).toInt()}%" else label,
                fontWeight = FontWeight.Bold,
                color = if (holding) Color.White else Color(0xFF4F46E5)
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun HoldTask(seconds: Int, label: String, hint: String, onSolved: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(hint, fontSize = 13.sp, color = Color(0xFF64748B))
        HoldButton(seconds = seconds, label = label, onComplete = onSolved)
    }
}

@Composable
private fun VolumeLockedBadge() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Volume Locked • Solve challenge to silence alarm",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF991B1B)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Sensors helper
// ---------------------------------------------------------------------------

private data class Accel(val x: Float, val y: Float, val z: Float, val mag: Float)

@Composable
private fun rememberAccel(): Accel {
    val ctx = LocalContext.current
    var acc by remember { mutableStateOf(Accel(0f, 0f, 9.8f, 9.8f)) }
    DisposableEffect(ctx) {
        val sm = try {
            ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        } catch (_: Exception) {
            null
        }
        val sensor = try {
            sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        } catch (_: Exception) {
            null
        }
        if (sm == null || sensor == null) {
            onDispose { }
        } else {
            val l = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent?) {
                    if (e == null || e.values.size < 3) return
                    val x = e.values[0]
                    val y = e.values[1]
                    val z = e.values[2]
                    val m = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                    acc = Accel(x, y, z, m)
                }

                override fun onAccuracyChanged(s: Sensor?, a: Int) {}
            }
            try {
                sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_GAME)
            } catch (_: Exception) {
            }
            onDispose {
                try {
                    sm.unregisterListener(l)
                } catch (_: Exception) {
                }
            }
        }
    }
    return acc
}

@Composable
private fun rememberLux(): Float? {
    val ctx = LocalContext.current
    var lux by remember { mutableStateOf<Float?>(null) }
    DisposableEffect(ctx) {
        val sm = try {
            ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        } catch (_: Exception) {
            null
        }
        val sensor = try {
            sm?.getDefaultSensor(Sensor.TYPE_LIGHT)
        } catch (_: Exception) {
            null
        }
        if (sm == null || sensor == null) {
            onDispose { }
        } else {
            val l = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent?) {
                    if (e == null || e.values.isEmpty()) return
                    lux = e.values[0]
                }

                override fun onAccuracyChanged(s: Sensor?, a: Int) {}
            }
            try {
                sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            } catch (_: Exception) {
            }
            onDispose {
                try {
                    sm.unregisterListener(l)
                } catch (_: Exception) {
                }
            }
        }
    }
    return lux
}

private fun hasSensor(ctx: Context, type: Int): Boolean {
    return try {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sm.getDefaultSensor(type) != null
    } catch (_: Exception) {
        false
    }
}

// ---------------------------------------------------------------------------
// 1. Eye tracking 60s (tap 6x over 60s)
// ---------------------------------------------------------------------------

@Composable
private fun EyeTask(onSolved: () -> Unit) {
    var elapsed by remember { mutableIntStateOf(0) }
    var taps by remember { mutableIntStateOf(0) }
    var lastTap by remember { mutableStateOf(0L) }
    var done by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (elapsed < 60 && !done) {
            delay(1000)
            elapsed++
            if (elapsed >= 60 && taps >= 6) {
                done = true
                onSolved()
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Keep your eyes open for 60 seconds. Tap the button at least 6 times (about every 10s).", fontSize = 13.sp, color = Color(0xFF64748B))
        LinearProgressIndicator(progress = elapsed / 60f, modifier = Modifier.fillMaxWidth())
        Text("$elapsed / 60s • $taps / 6 taps", fontWeight = FontWeight.Bold)
        Button(
            onClick = {
                val now = System.currentTimeMillis()
                if (now - lastTap < 3000) return@Button
                lastTap = now
                taps++
                if (elapsed >= 60 && taps >= 6 && !done) {
                    done = true
                    onSolved()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("I'm awake 👀 ($taps/6)")
        }
        if (elapsed >= 60 && taps < 6) {
            Text("Time's up — keep tapping until 6!", color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
        }
    }
}

// ---------------------------------------------------------------------------
// 2-4. Math
// ---------------------------------------------------------------------------

private data class Sum(val q: String, val a: Int)

private fun genHardMathQuestions(rnd: java.util.Random = java.util.Random()): List<Sum> {
    fun r(f: Int, t: Int) = rnd.nextInt(t - f + 1) + f
    val list = mutableListOf<Sum>()

    // 1. 3-digit Addition with carry: a + b
    val a1 = r(350, 899)
    val b1 = r(275, 899)
    list.add(Sum("$a1 + $b1 = ?", a1 + b1))

    // 2. 3-digit Subtraction with borrow: a − b
    val a2 = r(550, 999)
    val b2 = r(189, a2 - 85)
    list.add(Sum("$a2 − $b2 = ?", a2 - b2))

    // 3. 3-term Mixed: a + b − c
    val a3 = r(180, 520)
    val b3 = r(190, 480)
    val c3 = r(95, 340)
    list.add(Sum("$a3 + $b3 − $c3 = ?", a3 + b3 - c3))

    // 4. 3-term Mixed: a − b + c
    val a4 = r(420, 850)
    val b4 = r(150, 380)
    val c4 = r(130, 460)
    list.add(Sum("$a4 − $b4 + $c4 = ?", a4 - b4 + c4))

    // 5. 3-term Double Subtraction: a − b − c
    val a5 = r(720, 999)
    val b5 = r(160, 340)
    val c5 = r(120, a5 - b5 - 80)
    list.add(Sum("$a5 − $b5 − $c5 = ?", a5 - b5 - c5))

    // 6. 3-term Triple Addition: a + b + c
    val a6 = r(165, 450)
    val b6 = r(175, 450)
    val c6 = r(185, 450)
    list.add(Sum("$a6 + $b6 + $c6 = ?", a6 + b6 + c6))

    // 7. Large 4-digit / High 3-digit Addition: a + b
    val a7 = r(580, 1250)
    val b7 = r(470, 990)
    list.add(Sum("$a7 + $b7 = ?", a7 + b7))

    // 8. 4-digit Subtraction with borrow: a − b
    val a8 = r(1100, 1850)
    val b8 = r(380, 890)
    list.add(Sum("$a8 − $b8 = ?", a8 - b8))

    // 9. Mixed Plus & Minus with carry & borrow: a − b + c
    val a9 = r(360, 750)
    val b9 = r(180, 420)
    val c9 = r(120, 350)
    list.add(Sum("$a9 − $b9 + $c9 = ?", a9 - b9 + c9))

    // 10. Large Mixed Challenge: a + b − c
    val a10 = r(420, 950)
    val b10 = r(380, 850)
    val c10 = r(240, 650)
    list.add(Sum("$a10 + $b10 − $c10 = ?", a10 + b10 - c10))

    return list.shuffled(rnd)
}

private fun genSums(level: Int, rnd: java.util.Random = java.util.Random()): List<Sum> {
    if (level == 2) {
        return genHardMathQuestions(rnd)
    }
    fun r(f: Int, t: Int) = rnd.nextInt(t - f + 1) + f
    return List(3) {
        when (level) {
            0 -> {
                val a = r(2, 20)
                val b = r(2, 20)
                if (rnd.nextBoolean()) Sum("$a + $b = ?", a + b)
                else {
                    val hi = maxOf(a, b)
                    val lo = minOf(a, b)
                    Sum("$hi − $lo = ?", hi - lo)
                }
            }
            else -> when (r(0, 1)) {
                0 -> {
                    val a = r(11, 99)
                    val b = r(11, 99)
                    Sum("$a + $b = ?", a + b)
                }
                else -> {
                    val a = r(20, 99)
                    val b = r(10, 49)
                    Sum("$a − $b = ?", a - b)
                }
            }
        }
    }
}

@Composable
private fun MathTask(level: Int, onSolved: () -> Unit) {
    val sums = remember(level) { genSums(level) }
    var idx by remember(level) { mutableIntStateOf(0) }
    var answer by remember(level, idx) { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    if (idx >= sums.size) {
        LaunchedEffect(Unit) { onSolved() }
        return
    }
    val cur = sums[idx]

    fun submitAnswer() {
        val v = answer.trim().toIntOrNull()
        if (v == null) {
            err = "Please enter a valid number"
            return
        }
        if (v == cur.a) {
            err = ""
            if (idx + 1 >= sums.size) {
                onSolved()
            } else {
                idx++
                answer = ""
            }
        } else {
            err = "Incorrect answer — calculate carefully and try again"
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Question ${idx + 1} of ${sums.size}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF4F46E5)
            )
            Text(
                "${((idx.toFloat() / sums.size) * 100).toInt()}% Done",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )
        }
        LinearProgressIndicator(
            progress = { (idx.toFloat()) / sums.size },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF1F5F9),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                cur.q,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = Color(0xFF0F172A),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 12.dp)
            )
        }
        OutlinedTextField(
            value = answer,
            onValueChange = {
                answer = it.filter { c -> c.isDigit() || c == '-' }
                if (err.isNotBlank()) err = ""
            },
            label = { Text("Your answer") },
            placeholder = { Text("Enter number") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { submitAnswer() }
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (err.isNotBlank()) {
            Text(
                err,
                color = Color(0xFFDC2626),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Button(
            onClick = { submitAnswer() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
        ) {
            Text(
                if (idx + 1 == sums.size) "Check & Dismiss Alarm" else "Submit Answer",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 5. QR (preset code)
// ---------------------------------------------------------------------------

@Composable
private fun QrTask(onSolved: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Go to your bathroom QR and type the code. Preset code for now:", fontSize = 13.sp, color = Color(0xFF64748B))
        Surface(color = Color(0xFF0F172A), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                "WAKE-2026",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
                letterSpacing = 3.sp
            )
        }
        Text("Tip: write WAKE-2026 on paper and stick it in the bathroom — that's your QR.", fontSize = 12.sp, color = Color(0xFF94A3B8))
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.uppercase(Locale.US); err = "" },
            label = { Text("Enter code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (err.isNotBlank()) Text(err, color = Color(0xFFDC2626))
        Button(
            onClick = {
                if (code.trim() == "WAKE-2026") onSolved()
                else err = "Code doesn't match — check WAKE-2026"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Verify")
        }
    }
}

// ---------------------------------------------------------------------------
// 7. Shake 50x
// ---------------------------------------------------------------------------

@Composable
private fun ShakeTask(required: Int, onSolved: () -> Unit, toast: (String) -> Unit) {
    val ctx = LocalContext.current
    val acc = rememberAccel()
    var count by remember { mutableIntStateOf(0) }
    var last by remember { mutableStateOf(0L) }
    var done by remember { mutableStateOf(false) }
    val hasAcc = remember { hasSensor(ctx, Sensor.TYPE_ACCELEROMETER) }
    LaunchedEffect(acc.mag) {
        if (done || !hasAcc) return@LaunchedEffect
        val now = System.currentTimeMillis()
        if (acc.mag > 13.5f && now - last > 280) {
            last = now
            count++
            if (count >= required) {
                done = true
                onSolved()
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Shake the phone hard! Each spike counts once.", fontSize = 13.sp, color = Color(0xFF64748B))
        LinearProgressIndicator(progress = count / required.toFloat(), modifier = Modifier.fillMaxWidth())
        Text("$count / $required shakes", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        if (!hasAcc) {
            Text("No accelerometer found — tap instead:", fontSize = 13.sp)
            Button(onClick = {
                count++
                if (count >= required && !done) {
                    done = true
                    onSolved()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Tap ($count/$required)") }
        }
    }
}

// ---------------------------------------------------------------------------
// 8. Walk 30 steps
// ---------------------------------------------------------------------------

@Composable
private fun StepsTask(required: Int, onSolved: () -> Unit, toast: (String) -> Unit) {
    val ctx = LocalContext.current
    val acc = rememberAccel()
    var count by remember { mutableIntStateOf(0) }
    var last by remember { mutableStateOf(0L) }
    var done by remember { mutableStateOf(false) }
    val hasStepCounter = remember { hasSensor(ctx, Sensor.TYPE_STEP_COUNTER) }
    val hasAcc = remember { hasSensor(ctx, Sensor.TYPE_ACCELEROMETER) }
    // Step-counter baseline path
    var base by remember { mutableStateOf<Int?>(null) }
    DisposableEffect(hasStepCounter) {
        if (!hasStepCounter) {
            onDispose { }
        } else {
            val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val s = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            val l = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent?) {
                    if (e == null || done) return
                    val total = e.values[0].toInt()
                    if (base == null) base = total
                    val walked = total - (base ?: total)
                    if (walked > count) {
                        count = walked.coerceAtMost(required)
                        if (count >= required) {
                            done = true
                            onSolved()
                        }
                    }
                }

                override fun onAccuracyChanged(s: Sensor?, a: Int) {}
            }
            try {
                sm.registerListener(l, s, SensorManager.SENSOR_DELAY_NORMAL)
            } catch (_: Exception) {
            }
            onDispose {
                try {
                    sm.unregisterListener(l)
                } catch (_: Exception) {
                }
            }
        }
    }
    // Accelerometer fallback detection
    LaunchedEffect(acc.mag) {
        if (done || hasStepCounter || !hasAcc) return@LaunchedEffect
        val now = System.currentTimeMillis()
        if (acc.mag > 11.2f && now - last > 380) {
            last = now
            count++
            if (count >= required) {
                done = true
                onSolved()
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Walk with the phone in hand/pocket. Each step counts.", fontSize = 13.sp, color = Color(0xFF64748B))
        LinearProgressIndicator(progress = count / required.toFloat(), modifier = Modifier.fillMaxWidth())
        Text("$count / $required steps", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        if (!hasAcc && !hasStepCounter) {
            Button(onClick = {
                count++
                if (count >= required && !done) {
                    done = true
                    onSolved()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Tap for each step ($count/$required)") }
        }
    }
}

// ---------------------------------------------------------------------------
// 9. Squats 10x
// ---------------------------------------------------------------------------

@Composable
private fun SquatsTask(required: Int, onSolved: () -> Unit, toast: (String) -> Unit) {
    val ctx = LocalContext.current
    val acc = rememberAccel()
    var count by remember { mutableIntStateOf(0) }
    var last by remember { mutableStateOf(0L) }
    var done by remember { mutableStateOf(false) }
    val hasAcc = remember { hasSensor(ctx, Sensor.TYPE_ACCELEROMETER) }
    LaunchedEffect(acc.mag) {
        if (done || !hasAcc) return@LaunchedEffect
        val now = System.currentTimeMillis()
        // Big deliberate up-down motion, slower cadence than shaking.
        if (acc.mag > 12.5f && now - last > 800) {
            last = now
            count++
            if (count >= required) {
                done = true
                onSolved()
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Hold the phone and do squats — one big down-up motion = 1 rep.", fontSize = 13.sp, color = Color(0xFF64748B))
        LinearProgressIndicator(progress = count / required.toFloat(), modifier = Modifier.fillMaxWidth())
        Text("$count / $required squats", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        if (!hasAcc) {
            Button(onClick = {
                count++
                if (count >= required && !done) {
                    done = true
                    onSolved()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Tap per squat ($count/$required)") }
        }
    }
}

// ---------------------------------------------------------------------------
// 10. Typing phrase
// ---------------------------------------------------------------------------

private val TYPING_PHRASES = listOf(
    "The early bird catches the worm",
    "Wake up and conquer the day",
    "Discipline beats motivation every morning",
    "I am awake alert and ready to win",
    "Small steps every day lead to big wins"
)

@Composable
private fun TypingTask(onSolved: () -> Unit) {
    val target = remember { TYPING_PHRASES.random() }
    var typed by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Retype exactly (case-insensitive):", fontSize = 13.sp, color = Color(0xFF64748B))
        Surface(color = Color(0xFFEEF2FF), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Text(target, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(12.dp))
        }
        OutlinedTextField(
            value = typed,
            onValueChange = { typed = it; err = "" },
            label = { Text("Type here") },
            modifier = Modifier.fillMaxWidth()
        )
        if (err.isNotBlank()) Text(err, color = Color(0xFFDC2626))
        Button(
            onClick = {
                if (typed.trim().equals(target.trim(), ignoreCase = true)) onSolved()
                else err = "Not exact — match every word"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit")
        }
    }
}

// ---------------------------------------------------------------------------
// 11. Simon (5 rounds)
// ---------------------------------------------------------------------------

@Composable
private fun SimonTask(onSolved: () -> Unit) {
    val colors = listOf(Color(0xFFEF4444), Color(0xFF22C55E), Color(0xFF3B82F6), Color(0xFFEAB308))
    val names = listOf("Red", "Green", "Blue", "Yellow")
    var seq by remember { mutableStateOf(List(5) { (0..3).random() }) }
    // Fix: proper 5-long random sequence
    LaunchedEffect(Unit) {
        seq = List(5) { (0..3).random() }
    }
    var round by remember { mutableIntStateOf(1) } // 1..5, length = round
    var showing by remember { mutableStateOf(true) }
    var flashIdx by remember { mutableIntStateOf(-1) }
    var input by remember { mutableStateOf(listOf<Int>()) }
    var msg by remember { mutableStateOf("Watch…") }

    LaunchedEffect(seq, round) {
        showing = true
        msg = "Watch… round $round/5"
        delay(600)
        for (i in 0 until round) {
            flashIdx = seq[i]
            delay(550)
            flashIdx = -1
            delay(280)
        }
        showing = false
        input = emptyList()
        msg = "Your turn — repeat $round"
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Round $round / 5 — $msg", fontWeight = FontWeight.Bold)
        LinearProgressIndicator(progress = round / 5f, modifier = Modifier.fillMaxWidth())
        LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(220.dp)) {
            items(4) { i ->
                    val lit = flashIdx == i
                    Box(
                        modifier = Modifier
                            .height(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (lit) colors[i] else colors[i].copy(alpha = 0.35f))
                            .border(2.dp, colors[i], RoundedCornerShape(16.dp))
                            .clickable(enabled = !showing) {
                                if (showing) return@clickable
                                val next = input + i
                                input = next
                                val pos = next.size - 1
                                if (seq[pos] != i) {
                                    msg = "Wrong — watch again"
                                    input = emptyList()
                                } else if (next.size == round) {
                                    if (round == 5) {
                                        onSolved()
                                    } else {
                                        round++
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(names[i], fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }
                }
            }
        }
}

// ---------------------------------------------------------------------------
// 12. 3x3 slider
// ---------------------------------------------------------------------------

private fun sliderScrambled(): MutableList<Int> {
    val b = mutableListOf(1, 2, 3, 4, 5, 6, 7, 8, 0)
    val rnd = java.util.Random()
    repeat(10) {
        val z = b.indexOf(0)
        val r = z / 3
        val c = z % 3
        val moves = mutableListOf<Int>()
        if (r > 0) moves.add(z - 3)
        if (r < 2) moves.add(z + 3)
        if (c > 0) moves.add(z - 1)
        if (c < 2) moves.add(z + 1)
        val m = moves[rnd.nextInt(moves.size)]
        b[z] = b[m]
        b[m] = 0
    }
    if (b == listOf(1, 2, 3, 4, 5, 6, 7, 8, 0)) {
        // One legal slide away from solved (empty moves left) — stays solvable.
        b[8] = 8
        b[7] = 0
    }
    return b
}

@Composable
private fun SliderTask(onSolved: () -> Unit) {
    var board by remember { mutableStateOf(sliderScrambled()) }
    var moves by remember { mutableIntStateOf(0) }
    val solved = board == listOf(1, 2, 3, 4, 5, 6, 7, 8, 0)
    LaunchedEffect(solved) {
        if (solved) {
            delay(300)
            onSolved()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Tap a tile next to the empty space to slide. Order 1-8.", fontSize = 13.sp, color = Color(0xFF64748B))
        Text("Moves: $moves", fontWeight = FontWeight.SemiBold)
        LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(9) { i ->
                val v = board[i]
                if (v == 0) {
                    Box(
                        Modifier
                            .height(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .height(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF))
                            .border(1.dp, Color(0xFF6366F1), RoundedCornerShape(12.dp))
                            .clickable {
                                val z = board.indexOf(0)
                                val r1 = i / 3
                                val c1 = i % 3
                                val r2 = z / 3
                                val c2 = z % 3
                                if (abs(r1 - r2) + abs(c1 - c2) == 1) {
                                    val nb = board.toMutableList()
                                    nb[z] = nb[i]
                                    nb[i] = 0
                                    board = nb
                                    moves++
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$v", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                    }
                }
            }
        }
        OutlinedButton(onClick = { board = sliderScrambled(); moves = 0 }, modifier = Modifier.fillMaxWidth()) {
            Text("Reshuffle")
        }
    }
}

// ---------------------------------------------------------------------------
// 13. Voice repeat
// ---------------------------------------------------------------------------

private val VOICE_PHRASES = listOf(
    "I am awake and ready",
    "Good morning world",
    "Today I will be unstoppable",
    "Rise and shine bright"
)

@Composable
private fun VoiceTask(onSolved: () -> Unit, toast: (String) -> Unit) {
    val ctx = LocalContext.current
    val target = remember { VOICE_PHRASES.random() }
    var heard by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    var typedFallback by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        try {
            val list = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val top = list?.firstOrNull()?.lowercase(Locale.US) ?: ""
            heard = list?.firstOrNull() ?: ""
            val norm = { s: String -> s.lowercase(Locale.US).replace(Regex("[^a-z ]"), "").trim() }
            if (top.isBlank()) {
                err = "Didn't hear you — try again"
            } else if (norm(top).contains(norm(target).split(" ").first()) || norm(target) in norm(top) || norm(top) in norm(target)) {
                onSolved()
            } else {
                err = "Heard \"$heard\" — say: \"$target\""
            }
        } catch (_: Exception) {
            err = "Speech failed — try again or type below"
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Say out loud:", fontSize = 13.sp, color = Color(0xFF64748B))
        Surface(color = Color(0xFFEEF2FF), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Text("\"$target\"", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(12.dp))
        }
        Button(
            onClick = {
                try {
                    val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Say: $target")
                    }
                    if (i.resolveActivity(ctx.packageManager) != null) launcher.launch(i)
                    else err = "No speech app — type the phrase below"
                } catch (_: Exception) {
                    err = "Speech unavailable — type the phrase below"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🎤 Speak now")
        }
        if (heard.isNotBlank()) Text("Heard: \"$heard\"", fontSize = 13.sp, color = Color(0xFF64748B))
        if (err.isNotBlank()) Text(err, color = Color(0xFFDC2626), fontSize = 13.sp)
        OutlinedTextField(
            value = typedFallback,
            onValueChange = { typedFallback = it },
            label = { Text("Fallback: type the phrase") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
            onClick = {
                if (typedFallback.trim().equals(target.trim(), ignoreCase = true)) onSolved()
                else toast("Type exactly: $target")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit typed fallback")
        }
    }
}

// ---------------------------------------------------------------------------
// 15. Photo
// ---------------------------------------------------------------------------

@Composable
private fun PhotoTask(onSolved: () -> Unit, toast: (String) -> Unit) {
    var taken by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) {
            taken = true
            onSolved()
        } else {
            toast("No photo captured — try again")
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Prove you're up: take a photo (e.g. bathroom / sink). Any photo dismisses.", fontSize = 13.sp, color = Color(0xFF64748B))
        Button(onClick = {
            try {
                launcher.launch(null)
            } catch (_: Exception) {
                toast("Camera unavailable")
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("📷 Take photo now")
        }
        if (taken) Text("Photo accepted!", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
    }
}

// ---------------------------------------------------------------------------
// 16. Light sensor
// ---------------------------------------------------------------------------

@Composable
private fun LightTask(onSolved: () -> Unit, toast: (String) -> Unit) {
    val ctx = LocalContext.current
    val lux = rememberLux()
    val hasLight = remember { hasSensor(ctx, Sensor.TYPE_LIGHT) }
    var done by remember { mutableStateOf(false) }
    LaunchedEffect(lux) {
        if (!done && lux != null && lux!! > 500) {
            done = true
            onSolved()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Turn on a bright light / shine flashlight at the sensor (>500 lux).", fontSize = 13.sp, color = Color(0xFF64748B))
        if (!hasLight) {
            Text("No light sensor on this device — hold the button 5s instead.", fontSize = 13.sp, color = Color(0xFFDC2626))
            HoldButton(seconds = 5, label = "Hold 5s (no sensor)", onComplete = onSolved)
        } else {
            Text(
                if (lux == null) "Reading…" else "${lux!!.toInt()} lux / 500 lux",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (lux != null && lux!! > 500) Color(0xFF16A34A) else Color(0xFF0F172A)
            )
            LinearProgressIndicator(
                progress = ((lux ?: 0f) / 500f).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth()
            )
            Text("Tip: point another phone's flashlight directly at the top of this phone.", fontSize = 12.sp, color = Color(0xFF94A3B8))
        }
    }
}

// ---------------------------------------------------------------------------
// 17. Word scramble
// ---------------------------------------------------------------------------

private val SCRAMBLE_WORDS = listOf("ALARM", "MORNING", "COFFEE", "ENERGY", "SUNRISE", "FOCUS", "POWER", "WINNER", "HUSTLE", "BRIGHT")

private fun scramble(w: String): String {
    var s = w
    var guard = 0
    while ((s == w || s.length < 2) && guard++ < 20) {
        s = w.toList().shuffled().joinToString("")
    }
    return s
}

@Composable
private fun ScrambleTask(onSolved: () -> Unit) {
    val words = remember { SCRAMBLE_WORDS.shuffled().take(3) }
    var idx by remember { mutableIntStateOf(0) }
    val scrambled = remember(words) { words.map { scramble(it) } }
    var guess by remember(idx) { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    if (idx >= words.size) {
        LaunchedEffect(Unit) { onSolved() }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Word ${idx + 1} / 3 — unscramble:", fontWeight = FontWeight.Bold)
        Text(scrambled[idx], fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        OutlinedTextField(
            value = guess,
            onValueChange = { guess = it.uppercase(Locale.US); err = "" },
            label = { Text("Your answer") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (err.isNotBlank()) Text(err, color = Color(0xFFDC2626))
        Button(
            onClick = {
                if (guess.trim() == words[idx]) {
                    if (idx + 1 >= words.size) onSolved() else {
                        idx++
                        guess = ""
                        err = ""
                    }
                } else err = "Not quite — try again"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check")
        }
    }
}

// ---------------------------------------------------------------------------
// 18. Pattern draw (tap the dots in order, twice)
// ---------------------------------------------------------------------------

@Composable
private fun PatternTask(onSolved: () -> Unit) {
    // Z-shape: 0,1,2,4,6,7,8
    val target = remember { listOf(0, 1, 2, 4, 6, 7, 8) }
    var taps by remember { mutableStateOf(listOf<Int>()) }
    var rounds by remember { mutableIntStateOf(0) }
    var msg by remember { mutableStateOf("Draw the highlighted pattern by tapping dots in order") }
    fun reset() {
        taps = emptyList()
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Round ${rounds + 1} / 2 — $msg", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(240.dp)) {
            items(9) { i ->
                val order = taps.indexOf(i)
                val inTarget = target.contains(i)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            when {
                                order >= 0 -> Color(0xFF4F46E5)
                                inTarget -> Color(0xFFEEF2FF)
                                else -> Color(0xFFF1F5F9)
                            }
                        )
                        .border(
                            2.dp,
                            if (inTarget || order >= 0) Color(0xFF6366F1) else Color(0xFFE2E8F0),
                            RoundedCornerShape(50)
                        )
                        .clickable {
                            if (taps.contains(i)) return@clickable
                            val expect = target.getOrNull(taps.size)
                            if (expect == i) {
                                taps = taps + i
                                if (taps.size == target.size) {
                                    if (rounds + 1 >= 2) onSolved()
                                    else {
                                        rounds++
                                        msg = "Perfect! Draw once more"
                                        reset()
                                    }
                                }
                            } else {
                                msg = "Wrong dot — start over"
                                reset()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (order >= 0) "${order + 1}" else if (inTarget) "•" else "",
                        color = if (order >= 0) Color.White else Color(0xFF6366F1),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
        OutlinedButton(onClick = { reset() }, modifier = Modifier.fillMaxWidth()) {
            Text("Clear")
        }
    }
}

// ---------------------------------------------------------------------------
// 19. Trivia (3 correct to pass)
// ---------------------------------------------------------------------------

private data class Quiz(val q: String, val options: List<String>, val answer: Int)

private val QUIZ_BANK = listOf(
    Quiz("How many minutes in an hour?", listOf("30", "60", "90", "100"), 1),
    Quiz("Which planet is known as the Red Planet?", listOf("Venus", "Mars", "Jupiter", "Saturn"), 1),
    Quiz("What is 7 × 8?", listOf("54", "56", "63", "48"), 1),
    Quiz("Which is the largest ocean?", listOf("Atlantic", "Indian", "Pacific", "Arctic"), 2),
    Quiz("How many days in a leap year?", listOf("365", "366", "364", "367"), 1),
    Quiz("What do bees make?", listOf("Honey", "Wax only", "Milk", "Silk"), 0),
    Quiz("Capital of France?", listOf("London", "Berlin", "Paris", "Rome"), 2),
    Quiz("Freezing point of water (°C)?", listOf("0", "100", "-10", "32"), 0),
    Quiz("How many legs does a spider have?", listOf("6", "8", "10", "4"), 1),
    Quiz("Which shape has 3 sides?", listOf("Square", "Triangle", "Circle", "Pentagon"), 1)
)

@Composable
private fun TriviaTask(onSolved: () -> Unit) {
    val questions = remember { QUIZ_BANK.shuffled().take(6) }
    var qi by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var picked by remember(qi) { mutableStateOf<Int?>(null) }
    var msg by remember(qi) { mutableStateOf("") }
    if (correct >= 3) {
        LaunchedEffect(Unit) { onSolved() }
        return
    }
    val q = questions[qi % questions.size]
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("$correct / 3 correct", fontWeight = FontWeight.Bold)
        LinearProgressIndicator(progress = correct / 3f, modifier = Modifier.fillMaxWidth())
        Text(q.q, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        q.options.forEachIndexed { i, opt ->
            val isPicked = picked == i
            val bg = when {
                picked == null -> Color(0xFFF1F4F9)
                i == q.answer -> Color(0xFFDCFCE7)
                isPicked -> Color(0xFFFEE2E2)
                else -> Color(0xFFF1F4F9)
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = picked == null) {
                        picked = i
                        if (i == q.answer) {
                            correct++
                            msg = "Correct!"
                        } else {
                            msg = "Wrong — correct: ${q.options[q.answer]}"
                        }
                    },
                shape = RoundedCornerShape(14.dp),
                color = bg
            ) {
                Text(opt, modifier = Modifier.padding(14.dp), fontWeight = FontWeight.SemiBold)
            }
        }
        if (msg.isNotBlank()) Text(msg, fontSize = 13.sp, color = if (msg.startsWith("Correct")) Color(0xFF16A34A) else Color(0xFFDC2626))
        if (picked != null) {
            Button(
                onClick = {
                    qi++
                    picked = null
                    msg = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Next")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 20. Breath hold (steady 10s)
// ---------------------------------------------------------------------------

@Composable
private fun BreathTask(onSolved: () -> Unit, toast: (String) -> Unit) {
    val acc = rememberAccel()
    var holding by remember { mutableStateOf(false) }
    var progressMs by remember { mutableStateOf(0L) }
    var done by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("Press & hold, keep the phone perfectly still for 10s") }
    LaunchedEffect(holding) {
        if (!holding) return@LaunchedEffect
        val start = System.currentTimeMillis()
        var lastOk = start
        while (holding && !done) {
            val still = abs(acc.mag - 9.8f) < 2.2f
            if (!still) {
                progressMs = 0L
                msg = "Too shaky — hold still to restart"
                // restart baseline
                delay(200)
                continue
            }
            progressMs = System.currentTimeMillis() - start
            msg = "Steady… ${(progressMs / 1000)}s / 10s"
            if (progressMs >= 10_000L) {
                done = true
                holding = false
                onSolved()
                break
            }
            delay(100)
        }
        if (!holding) progressMs = 0L
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(msg, fontSize = 13.sp, color = Color(0xFF64748B))
        LinearProgressIndicator(progress = (progressMs / 10_000f).coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth())
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(if (holding) Color(0xFF0284C7) else Color(0xFFF0F8FF))
                .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(50))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            holding = true
                            try {
                                awaitRelease()
                            } finally {
                                holding = false
                            }
                        }
                    )
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (holding) "Breathe slow… keep still" else "Press & hold 10s",
                fontWeight = FontWeight.Bold,
                color = if (holding) Color.White else Color(0xFF0284C7)
            )
        }
    }
}
