package com.gochathub.gochathubclient

import android.app.Application
import com.cometchat.uikit.core.hub.Hub

public class GoChatHubApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Hub.init(this)
    }
}