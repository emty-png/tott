package com.tott.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TottApp : Application() {
    // Fresh installs start empty (no mock data). ObjectBox store is created lazily by Hilt.
}
