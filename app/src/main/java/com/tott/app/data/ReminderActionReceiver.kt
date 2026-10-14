package com.tott.app.data

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.tott.app.widget.TottWidget
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_DONE = "com.tott.app.action.DONE"
        const val ACTION_SNOOZE = "com.tott.app.action.SNOOZE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(REMINDER_KEY_ID, -1)
        if (id <= 0) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entry = EntryPointAccessors
                    .fromApplication(context.applicationContext, WorkerEntryPoint::class.java)
                when (intent.action) {
                    ACTION_DONE -> {
                        val t = entry.repo().get(id)
                        if (t != null && !t.done) {
                            entry.repo().toggle(id)
                            if (t.rrule.isNotEmpty()) {
                                val nid = entry.repo().spawnNext(id)
                                entry.repo().get(nid)?.let { entry.scheduler().schedule(it) }
                            }
                        }
                        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                            .cancel(id.toInt())
                        try {
                            TottWidget().updateAll(context)
                        } catch (e: Exception) {
                        }
                    }
                    ACTION_SNOOZE -> entry.scheduler().snooze(id, 10)
                }
            } catch (e: Exception) {
                // store unavailable; next app open heals state
            } finally {
                pending.finish()
            }
        }
    }
}
