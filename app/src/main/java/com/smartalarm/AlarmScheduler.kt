package com.smartalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

/**
 * Data model for a configured alarm supporting multiple alarm times,
 * optional recurring days, custom ringtone, and dismissal challenge task.
 */
data class AlarmItem(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int = 7,
    val min: Int = 0,
    val label: String = "Alarm",
    val days: List<Int> = emptyList(), // Calendar.SUNDAY(1)..Calendar.SATURDAY(7). Empty = One-off.
    val ringtoneRes: Int = 0, // 0 = system default
    val taskId: Int = 4, // 1..20
    val enabled: Boolean = true,
    val triggerAt: Long = 0L
) {
    fun isRepeating(): Boolean = days.isNotEmpty()

    fun daysSummary(): String {
        if (days.isEmpty()) return "Once"
        if (days.size == 7) return "Every day"
        val weekdays = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
        val weekends = listOf(Calendar.SATURDAY, Calendar.SUNDAY)
        if (days.sorted() == weekdays.sorted()) return "Weekdays"
        if (days.sorted() == weekends.sorted()) return "Weekends"
        val dayNames = mapOf(
            Calendar.MONDAY to "Mon",
            Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu",
            Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat",
            Calendar.SUNDAY to "Sun"
        )
        val ordered = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)
        return ordered.filter { days.contains(it) }.joinToString(", ") { dayNames[it] ?: "" }
    }
}

/**
 * Single source of truth for scheduling multiple alarms + persisting them so they
 * survive process death and device reboots (see [BootReceiver]).
 */
object AlarmScheduler {

    private const val PREFS = "smart_alarm_prefs"
    private const val KEY_ALARMS_JSON = "alarms_json_v2"
    private const val KEY_HOUR = "hour"
    private const val KEY_MIN = "min"
    private const val KEY_RINGTONE = "ringtone_res"
    private const val KEY_TASK = "task_id"
    private const val KEY_TRIGGER = "trigger_at"
    private const val KEY_ENABLED = "enabled"

    data class SavedAlarm(
        val hour: Int,
        val min: Int,
        val ringtoneRes: Int,
        val taskId: Int,
        val triggerAt: Long,
        val enabled: Boolean
    )

    // -------------------------------------------------------------------------
    // Multi-Alarm Persistence
    // -------------------------------------------------------------------------

    fun loadAll(ctx: Context): List<AlarmItem> {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = p.getString(KEY_ALARMS_JSON, null)
        if (!raw.isNullOrBlank()) {
            try {
                val arr = JSONArray(raw)
                val list = mutableListOf<AlarmItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(fromJson(obj))
                }
                if (list.isNotEmpty()) return list
            } catch (_: Exception) {
            }
        }

        // Migrate legacy single alarm if present
        if (p.contains(KEY_HOUR)) {
            try {
                val legacy = SavedAlarm(
                    hour = p.getInt(KEY_HOUR, 7).coerceIn(0, 23),
                    min = p.getInt(KEY_MIN, 0).coerceIn(0, 59),
                    ringtoneRes = p.getInt(KEY_RINGTONE, 0),
                    taskId = p.getInt(KEY_TASK, 4).coerceIn(1, 20),
                    triggerAt = p.getLong(KEY_TRIGGER, 0L),
                    enabled = p.getBoolean(KEY_ENABLED, false)
                )
                val migrated = listOf(
                    AlarmItem(
                        id = "alarm_legacy",
                        hour = legacy.hour,
                        min = legacy.min,
                        label = "Morning Alarm",
                        days = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY),
                        ringtoneRes = legacy.ringtoneRes,
                        taskId = legacy.taskId,
                        enabled = legacy.enabled,
                        triggerAt = legacy.triggerAt
                    )
                )
                saveAll(ctx, migrated)
                return migrated
            } catch (_: Exception) {
            }
        }

        // Default initial alarms for a wonderful out-of-the-box experience
        val defaults = listOf(
            AlarmItem(
                id = "alarm_morning",
                hour = 7,
                min = 0,
                label = "Morning Wake Up",
                days = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY),
                ringtoneRes = 0,
                taskId = 4,
                enabled = true,
                triggerAt = computeNextTriggerMillis(7, 0, listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY))
            ),
            AlarmItem(
                id = "alarm_weekend",
                hour = 8,
                min = 30,
                label = "Weekend Recharge",
                days = listOf(Calendar.SATURDAY, Calendar.SUNDAY),
                ringtoneRes = 0,
                taskId = 2,
                enabled = false,
                triggerAt = 0L
            )
        )
        saveAll(ctx, defaults)
        return defaults
    }

    fun saveAll(ctx: Context, alarms: List<AlarmItem>) {
        try {
            val arr = JSONArray()
            alarms.forEach { arr.put(toJson(it)) }
            val edit = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            edit.putString(KEY_ALARMS_JSON, arr.toString())

            // Also keep legacy keys in sync with the earliest enabled alarm for backward compatibility
            val nextUpcoming = getNextUpcomingAlarm(alarms)
            if (nextUpcoming != null) {
                edit.putInt(KEY_HOUR, nextUpcoming.hour)
                    .putInt(KEY_MIN, nextUpcoming.min)
                    .putInt(KEY_RINGTONE, nextUpcoming.ringtoneRes)
                    .putInt(KEY_TASK, nextUpcoming.taskId)
                    .putLong(KEY_TRIGGER, nextUpcoming.triggerAt)
                    .putBoolean(KEY_ENABLED, true)
            } else {
                edit.putBoolean(KEY_ENABLED, false)
                    .putLong(KEY_TRIGGER, 0L)
            }
            edit.apply()
        } catch (_: Exception) {
        }
    }

    fun saveAlarm(ctx: Context, alarm: AlarmItem): Long {
        val list = loadAll(ctx).toMutableList()
        val index = list.indexOfFirst { it.id == alarm.id }
        var scheduledTrigger = 0L

        val alarmToSave = if (alarm.enabled) {
            val trigger = schedule(ctx, alarm)
            scheduledTrigger = trigger
            alarm.copy(triggerAt = trigger)
        } else {
            cancel(ctx, alarm.id)
            alarm.copy(triggerAt = 0L)
        }

        if (index >= 0) {
            list[index] = alarmToSave
        } else {
            list.add(alarmToSave)
        }
        saveAll(ctx, list)
        return scheduledTrigger
    }

    fun toggleAlarm(ctx: Context, alarmId: String, enable: Boolean): Long {
        val list = loadAll(ctx).toMutableList()
        val index = list.indexOfFirst { it.id == alarmId }
        if (index < 0) return 0L
        val current = list[index]

        var newTrigger = 0L
        val updated = if (enable) {
            newTrigger = schedule(ctx, current.copy(enabled = true))
            current.copy(enabled = true, triggerAt = newTrigger)
        } else {
            cancel(ctx, alarmId)
            current.copy(enabled = false, triggerAt = 0L)
        }
        list[index] = updated
        saveAll(ctx, list)
        return newTrigger
    }

    fun deleteAlarm(ctx: Context, alarmId: String) {
        cancel(ctx, alarmId)
        val list = loadAll(ctx).filterNot { it.id == alarmId }
        saveAll(ctx, list)
    }

    fun getNextUpcomingAlarm(alarms: List<AlarmItem>): AlarmItem? {
        val now = System.currentTimeMillis()
        return alarms
            .filter { it.enabled && it.triggerAt > now }
            .minByOrNull { it.triggerAt }
            ?: alarms
                .filter { it.enabled }
                .minByOrNull { computeNextTriggerMillis(it.hour, it.min, it.days) }
    }

    // -------------------------------------------------------------------------
    // Alarm Scheduling Core
    // -------------------------------------------------------------------------

    fun computeNextTriggerMillis(hour: Int, min: Int, days: List<Int>): Long {
        val now = System.currentTimeMillis()
        if (days.isEmpty()) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                set(Calendar.MINUTE, min.coerceIn(0, 59))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return cal.timeInMillis
        }

        // Repeating alarm: check today and following 7 days
        for (offset in 0..7) {
            val testCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                set(Calendar.MINUTE, min.coerceIn(0, 59))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dow = testCal.get(Calendar.DAY_OF_WEEK)
            if (days.contains(dow) && testCal.timeInMillis > now) {
                return testCal.timeInMillis
            }
        }

        // Fallback to next week
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 7)
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, min.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun alarmIntents(ctx: Context, alarmId: String, ringtoneRes: Int, taskId: Int): Pair<PendingIntent, PendingIntent> {
        val alarmIntent = Intent(ctx.applicationContext, AlarmForegroundService::class.java)
            .putExtra(AlarmForegroundService.EXTRA_ALARM_ID, alarmId)
            .putExtra(AlarmForegroundService.EXTRA_RINGTONE_RES, ringtoneRes)
            .putExtra(AlarmForegroundService.EXTRA_TASK_ID, taskId)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val reqCode = (alarmId.hashCode() and 0x3FFFFFFF)

        val alarmPI = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getForegroundService(ctx, reqCode, alarmIntent, flags)
        } else {
            PendingIntent.getService(ctx, reqCode, alarmIntent, flags)
        }
        val showPI = PendingIntent.getActivity(
            ctx, reqCode + 0x20000000, Intent(ctx, MainActivity::class.java), flags
        )
        return alarmPI to showPI
    }

    fun schedule(ctx: Context, alarm: AlarmItem): Long {
        val appCtx = ctx.applicationContext
        val am = appCtx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val trigger = computeNextTriggerMillis(alarm.hour, alarm.min, alarm.days)
        val (alarmPI, showPI) = alarmIntents(appCtx, alarm.id, alarm.ringtoneRes, alarm.taskId)
        am.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, showPI), alarmPI)
        return trigger
    }

    fun cancel(ctx: Context, alarmId: String) {
        try {
            val appCtx = ctx.applicationContext
            val am = appCtx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val (alarmPI, showPI) = alarmIntents(appCtx, alarmId, 0, 1)
            try { am.cancel(alarmPI) } catch (_: Exception) {}
            try { alarmPI.cancel() } catch (_: Exception) {}
            try { showPI.cancel() } catch (_: Exception) {}
        } catch (_: Exception) {
        }
    }

    fun rescheduleAll(ctx: Context) {
        val list = loadAll(ctx).toMutableList()
        var updated = false
        for (i in list.indices) {
            val item = list[i]
            if (item.enabled) {
                val trigger = schedule(ctx, item)
                list[i] = item.copy(triggerAt = trigger)
                updated = true
            }
        }
        if (updated) saveAll(ctx, list)
    }

    /**
     * Marks an alarm consumed when solved or dismissed.
     * If the alarm is recurring, schedules the next recurrence.
     * If one-off, sets enabled = false.
     */
    fun markConsumed(ctx: Context, alarmId: String? = null) {
        try {
            val list = loadAll(ctx).toMutableList()
            if (list.isEmpty()) return

            val targetIndex = if (alarmId != null) {
                list.indexOfFirst { it.id == alarmId }
            } else {
                // Legacy / fallback: find alarm with triggerAt <= now
                val now = System.currentTimeMillis()
                list.indexOfFirst { it.enabled && (it.triggerAt <= now || (it.triggerAt - now) < 60_000L) }
            }

            if (targetIndex >= 0) {
                val current = list[targetIndex]
                if (current.isRepeating()) {
                    val next = computeNextTriggerMillis(current.hour, current.min, current.days)
                    schedule(ctx, current.copy(triggerAt = next))
                    list[targetIndex] = current.copy(triggerAt = next, enabled = true)
                } else {
                    cancel(ctx, current.id)
                    list[targetIndex] = current.copy(enabled = false, triggerAt = 0L)
                }
                saveAll(ctx, list)
            }
        } catch (_: Exception) {
        }
    }

    // -------------------------------------------------------------------------
    // Backward-Compatibility Helpers
    // -------------------------------------------------------------------------

    fun load(ctx: Context): SavedAlarm {
        val list = loadAll(ctx)
        val next = getNextUpcomingAlarm(list) ?: list.firstOrNull()
        return if (next != null) {
            SavedAlarm(next.hour, next.min, next.ringtoneRes, next.taskId, next.triggerAt, next.enabled)
        } else {
            SavedAlarm(7, 0, 0, 4, 0L, false)
        }
    }

    fun schedule(ctx: Context, hour: Int, min: Int, ringtoneRes: Int, taskId: Int): Long {
        val alarm = AlarmItem(
            id = "alarm_quick_${hour}_${min}",
            hour = hour,
            min = min,
            label = "Quick Alarm",
            days = emptyList(),
            ringtoneRes = ringtoneRes,
            taskId = taskId,
            enabled = true
        )
        return saveAlarm(ctx, alarm)
    }

    fun cancel(ctx: Context, ringtoneRes: Int, taskId: Int = 1) {
        val list = loadAll(ctx)
        val next = getNextUpcomingAlarm(list)
        if (next != null) {
            cancel(ctx, next.id)
            toggleAlarm(ctx, next.id, false)
        }
    }

    fun computeTriggerMillis(hour: Int, min: Int): Long =
        computeNextTriggerMillis(hour, min, emptyList())

    // -------------------------------------------------------------------------
    // JSON Serialization Helpers
    // -------------------------------------------------------------------------

    private fun toJson(alarm: AlarmItem): JSONObject {
        return JSONObject().apply {
            put("id", alarm.id)
            put("hour", alarm.hour)
            put("min", alarm.min)
            put("label", alarm.label)
            val daysArr = JSONArray()
            alarm.days.forEach { daysArr.put(it) }
            put("days", daysArr)
            put("ringtoneRes", alarm.ringtoneRes)
            put("taskId", alarm.taskId)
            put("enabled", alarm.enabled)
            put("triggerAt", alarm.triggerAt)
        }
    }

    private fun fromJson(obj: JSONObject): AlarmItem {
        val daysList = mutableListOf<Int>()
        val daysArr = obj.optJSONArray("days")
        if (daysArr != null) {
            for (i in 0 until daysArr.length()) {
                daysList.add(daysArr.getInt(i))
            }
        }
        return AlarmItem(
            id = obj.optString("id", UUID.randomUUID().toString()),
            hour = obj.optInt("hour", 7).coerceIn(0, 23),
            min = obj.optInt("min", 0).coerceIn(0, 59),
            label = obj.optString("label", "Alarm"),
            days = daysList,
            ringtoneRes = obj.optInt("ringtoneRes", 0),
            taskId = obj.optInt("taskId", 4).coerceIn(1, 20),
            enabled = obj.optBoolean("enabled", true),
            triggerAt = obj.optLong("triggerAt", 0L)
        )
    }
}
