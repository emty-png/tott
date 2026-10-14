package com.tott.app

import android.app.Application
import com.tott.app.data.TaskRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TottApp : Application() {
    @Inject lateinit var repo: TaskRepository
    override fun onCreate() {
        super.onCreate()
        repo.seedIfEmpty()
    }
}
