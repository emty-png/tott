package com.tott.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tott.app.MainActivity
import com.tott.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

const val REMINDER_CHANNEL = "tott_reminders"
const val REMINDER_WORK_PREFIX = "tott-reminder-"
const val REMINDER_KEY_ID = "task_id"

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val repo: TaskRepository
) {
    private fun wm(): WorkManager = WorkManager.getInstance(ctx)

    fun ensureChannel() {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(REMINDER_CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(REMINDER_CHANNEL, "Task reminders", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    fun schedule(t: TaskEntity) {
        cancel(t.id)
        if (t.done || t.dueDate < 0 || t.dueTime < 0) return
        val trigger = LocalDate.ofEpochDay(t.dueDate)
            .atTime(t.dueTime / 60, t.dueTime % 60)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val delay = trigger - System.currentTimeMillis()
        if (delay <= 0) return
        val req = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(REMINDER_KEY_ID to t.id))
            .build()
        wm().enqueueUniqueWork(REMINDER_WORK_PREFIX + t.id, ExistingWorkPolicy.REPLACE, req)
    }

    fun cancel(id: Long) = wm().cancelUniqueWork(REMINDER_WORK_PREFIX + id)

    fun rescheduleAll() {
        for (t in repo.all()) {
            if (!t.done && t.dueDate >= 0 && t.dueTime >= 0) schedule(t)
        }
    }

    /** Fired by the worker: re-check done state, then notify. */
    fun fireIfDue(id: Long) {
        val t = repo.get(id) ?: return
        if (t.done) return
        notifyTask(t)
    }

    fun sendTest() {
        postNotification(-1, "Test reminder", "Notifications are working")
    }

    fun snooze(id: Long, minutes: Long) {
        val t = repo.get(id) ?: return
        if (t.done) return
        val req = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(minutes, TimeUnit.MINUTES)
            .setInputData(workDataOf(REMINDER_KEY_ID to id))
            .build()
        wm().enqueueUniqueWork(REMINDER_WORK_PREFIX + id, ExistingWorkPolicy.REPLACE, req)
    }

    fun notifyTask(t: TaskEntity) {
        ensureChannel()
        val due = TaskRepository.displayDue(t)
        val doneSubs = t.subs.count { it.d }
        val big = buildString {
            append("${t.project} · $due")
            if (t.rrule.isNotEmpty()) append(" · repeats ${t.rrule}")
            if (t.subs.isNotEmpty()) {
                append("\n\n")
                t.subs.take(5).forEach { s ->
                    append(if (s.d) "✓ " else "○ ")
                    appendLine(s.t)
                }
                if (t.subs.size > 5) appendLine("…and ${t.subs.size - 5} more")
            }
        }.trimEnd()
        val b = baseBuilder(t.id, t.title, "${t.project} · $due", t.id)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(big)
                    .setSummaryText("tott")
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        if (t.subs.isNotEmpty()) b.setProgress(t.subs.size, doneSubs, false)
        b.addAction(
            R.drawable.ic_notif, "Mark done",
            actionIntent(t.id, ReminderActionReceiver.ACTION_DONE, t.id.toInt() + 1000)
        )
        b.addAction(
            R.drawable.ic_notif, "Snooze 10 min",
            actionIntent(t.id, ReminderActionReceiver.ACTION_SNOOZE, t.id.toInt() + 2000)
        )
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(t.id.toInt(), b.build())
    }

    private fun actionIntent(taskId: Long, action: String, reqCode: Int): PendingIntent {
        val intent = Intent(ctx, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(REMINDER_KEY_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            ctx, reqCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun baseBuilder(id: Long, title: String, text: String, detailId: Long): NotificationCompat.Builder {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            if (detailId > 0) putExtra("open_detail", detailId)
        }
        val pi = PendingIntent.getActivity(
            ctx, id.toInt(),
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(ctx, REMINDER_CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setColor(0xFF6750A4.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pi)
    }

    fun postNotification(id: Long, title: String, text: String) {
        ensureChannel()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id.toInt(), baseBuilder(id, title, text, id).build())
    }
}
