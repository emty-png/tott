package com.tott.app.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                EntryPointAccessors
                    .fromApplication(context.applicationContext, WorkerEntryPoint::class.java)
                    .scheduler()
                    .rescheduleAll()
            } catch (e: Exception) {
                // store not ready; next app open reschedules via normal flow
            } finally {
                pending.finish()
            }
        }
    }
}
