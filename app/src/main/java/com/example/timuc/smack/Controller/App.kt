package com.example.timuc.smack.Controller

import android.app.Application
import com.example.timuc.smack.Utilities.ShardPrefs

class App: Application() {

    companion object {
        lateinit var prefs: ShardPrefs
    }

    override fun onCreate() {
        prefs = ShardPrefs(applicationContext)
        super.onCreate()
    }
}