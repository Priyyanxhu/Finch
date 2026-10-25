package com.smartalarm

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class AlarmForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "ALARM_CHANNEL"
        const val EXTRA_RINGTONE_RES = "RINGTONE_RES"
        const val EXTRA_TASK_ID = "TASK_ID"
        const val EXTRA_ALARM_ID = "ALARM_ID"
        const val EXTRA_TASK_SOLVED = "TASK_SOLVED"
        const val ACTION_STOP = "com.smartalarm.ACTION_STOP_ALARM"
        const val ACTION_DISMISS = "com.smartalarm.ACTION_DISMISS_ALARM"
        private const val NOTIF_ID = 1

        @Volatile
        var isRinging: Boolean = false
            private set
    }

    private var mp: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentTaskId: Int = 4
    private var currentAlarmId: String? = null

    private val volumeHandler = Handler(Looper.getMainLooper())
    private var volumeReceiver: BroadcastReceiver? = null
    private val volumeLockRunnable = object : Runnable {
        override fun run() {
            if (isRinging) {
                enforceMaxVolume()
                volumeHandler.postDelayed(this, 250)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        val existing = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.getNotificationChannel(CHANNEL_ID)
        } else null
        if (existing == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "Alarms", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Smart Alarm ringing notifications"
                setSound(null, null)
                enableVibration(true)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(ch)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Only allow stopping if the task was verified as solved
        if (intent?.action == ACTION_STOP || intent?.action == ACTION_DISMISS ||
            intent?.action == "STOP"
        ) {
            val isSolved = intent.getBooleanExtra(EXTRA_TASK_SOLVED, false)
            val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: currentAlarmId
            if (!isSolved && isRinging) {
                // Task has NOT been solved! Alarm cannot be silenced or stopped.
                val challengeIntent = Intent(this, ChallengeActivity::class.java)
                    .putExtra(ChallengeActivity.EXTRA_TASK_ID, currentTaskId)
                    .putExtra(ChallengeActivity.EXTRA_ALARM_ID, currentAlarmId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                try {
                    startActivity(challengeIntent)
                } catch (_: Exception) {
                }
                return START_NOT_STICKY
            }

            try {
                AlarmScheduler.markConsumed(applicationContext, alarmId)
            } catch (_: Exception) {
            }
            stopAlarm()
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val resId = intent.getIntExtra(EXTRA_RINGTONE_RES, 0)
        currentAlarmId = intent.getStringExtra(EXTRA_ALARM_ID)
        currentTaskId = intent.getIntExtra(EXTRA_TASK_ID, 4).let {
            if (it in 1..20) it else try {
                AlarmScheduler.load(applicationContext).taskId
            } catch (_: Exception) {
                4
            }.coerceIn(1, 20)
        }

        val challengeIntent = Intent(this, ChallengeActivity::class.java)
            .putExtra(ChallengeActivity.EXTRA_TASK_ID, currentTaskId)
            .putExtra(ChallengeActivity.EXTRA_ALARM_ID, currentAlarmId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val contentPI = PendingIntent.getActivity(
            this, 2, challengeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val solvePI = PendingIntent.getActivity(
            this, 3, challengeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val taskName = ChallengeActivity.taskDisplayName(currentTaskId)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Smart Alarm Ringing")
            .setContentText("Solve \"$taskName\" to dismiss • Volume locked")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPI)
            .setFullScreenIntent(contentPI, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Solve task to dismiss", solvePI)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIF_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                @Suppress("DEPRECATION")
                startForeground(NOTIF_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
            return START_NOT_STICKY
        }

        acquireWakeLock()
        startRinging(resId)
        return START_NOT_STICKY
    }

    private fun enforceMaxVolume() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxAlarm = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val curAlarm = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            if (curAlarm < maxAlarm) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarm, 0)
            }
            val maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val curMusic = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (curMusic < maxMusic) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0)
            }
            mp?.setVolume(1.0f, 1.0f)
        } catch (_: Exception) {
        }
    }

    private fun startVolumeLock() {
        enforceMaxVolume()
        volumeHandler.removeCallbacksAndMessages(null)
        volumeHandler.post(volumeLockRunnable)

        if (volumeReceiver == null) {
            volumeReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    enforceMaxVolume()
                }
            }
            val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(volumeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    registerReceiver(volumeReceiver, filter)
                }
            } catch (_: Exception) {
                try {
                    registerReceiver(volumeReceiver, filter)
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun stopVolumeLock() {
        volumeHandler.removeCallbacksAndMessages(null)
        volumeReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {
            }
        }
        volumeReceiver = null
    }

    private fun startRinging(resId: Int) {
        stopPlayerOnly()
        try {
            isRinging = true
            mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                if (!attachAlarmSound(this, resId)) {
                    throw IllegalStateException("No playable alarm sound")
                }
                isLooping = true
                setVolume(1.0f, 1.0f)
                prepare()
                start()
            }
            startVolumeLock()
        } catch (e: Exception) {
            e.printStackTrace()
            stopAlarm()
            stopSelf()
        }
    }

    private fun attachAlarmSound(mp: MediaPlayer, resId: Int): Boolean {
        if (resId != 0) {
            try {
                resources.openRawResourceFd(resId)?.use { fd ->
                    mp.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    return true
                }
            } catch (_: Exception) {
            }
        } else {
            try {
                val uri = try {
                    RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                } catch (_: Exception) {
                    null
                } ?: android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
                if (uri != null) {
                    mp.setDataSource(this, uri)
                    return true
                }
            } catch (_: Exception) {
            }
        }
        return try {
            resources.openRawResourceFd(R.raw.radar)?.use { fd ->
                mp.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                true
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun stopPlayerOnly() {
        val p = mp
        mp = null
        if (p == null) return
        try {
            try {
                if (p.isPlaying) p.stop()
            } catch (_: IllegalStateException) {
            }
        } finally {
            try {
                p.reset()
            } catch (_: Exception) {
            }
            try {
                p.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun stopAlarm() {
        isRinging = false
        stopVolumeLock()
        stopPlayerOnly()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (_: Exception) {
        }
        releaseWakeLock()
    }

    private fun acquireWakeLock() {
        try {
            releaseWakeLock()
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "SmartAlarm::Ringing"
            ).apply {
                acquire(5 * 60 * 1000L)
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (_: Exception) {
        }
        wakeLock = null
    }

    override fun onDestroy() {
        isRinging = false
        stopVolumeLock()
        stopAlarm()
        super.onDestroy()
    }
}
