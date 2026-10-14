package com.tott.app.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerEntryPoint {
    fun scheduler(): ReminderScheduler
}

class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getLong(REMINDER_KEY_ID, -1)
        if (id <= 0) return Result.success()
        return try {
            val sched = EntryPointAccessors
                .fromApplication(applicationContext, WorkerEntryPoint::class.java)
                .scheduler()
            sched.fireIfDue(id)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
