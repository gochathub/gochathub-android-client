package com.gochathub.gochathubclient.push

import android.content.Context
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

/** Connector callbacks → Push (docs/UNIFIEDPUSH.md /verified contract). */
public class PushServiceImpl : PushService() {
    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        val keys = endpoint.pubKeySet
        Push.onEndpoint(this, endpoint.url, keys?.pubKey.orEmpty(), keys?.auth.orEmpty())
    }

    override fun onMessage(message: PushMessage, instance: String) {
        Push.onMessage(this, message.content)
    }

    override fun onRegistrationFailed(reason: org.unifiedpush.android.connector.FailedReason, instance: String) {
        Push.onRegistrationFailed()
    }

    override fun onUnregistered(instance: String) {
        Push.onUnregistered(this)
    }
}
