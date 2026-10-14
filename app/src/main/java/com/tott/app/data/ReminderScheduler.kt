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
        postNotification(t.id, t.title, "${t.project} · ${TaskRepository.displayDue(t)}")
    }

    fun sendTest() {
        postNotification(-1, "Test reminder", "Notifications are working")
    }

    fun postNotification(id: Long, title: String, text: String) {
        ensureChannel()
        val intent = Intent(ctx, MainActivity::class.java).apply {
            if (id > 0) putExtra("open_detail", id)
        }
        val pi = PendingIntent.getActivity(
            ctx, id.toInt(),
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(
            id.toInt(),
            NotificationCompat.Builder(ctx, REMINDER_CHANNEL)
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()
        )
    }
}
