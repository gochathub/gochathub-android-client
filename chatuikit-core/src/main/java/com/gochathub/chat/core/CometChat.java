package com.gochathub.chat.core;

import com.gochathub.chat.exceptions.CometChatException;
import com.gochathub.chat.models.AIAssistantBaseEvent;
import com.gochathub.chat.models.AIAssistantMessage;
import com.gochathub.chat.models.AIToolArgumentMessage;
import com.gochathub.chat.models.AIToolResultMessage;
import org.json.JSONObject;

/**
 * Callback and listener types the Kit still names. There is no cloud SDK behind
 * them: realtime and REST go through the hub package; listener registration is a
 * no-op and extension calls fail with a stable code.
 */
public final class CometChat {
    private CometChat() {}

    public abstract static class CallbackListener<T> {
        public abstract void onSuccess(T result);
        public abstract void onError(CometChatException e);
    }

    public abstract static class AIAssistantListener {
        public void onAIAssistantEventReceived(AIAssistantBaseEvent event) {}
    }

    public abstract static class MessageListener {
        public void onAIAssistantMessageReceived(AIAssistantMessage message) {}
        public void onAIToolResultReceived(AIToolResultMessage message) {}
        public void onAIToolArgumentsReceived(AIToolArgumentMessage message) {}
    }

    public interface ConnectionListener {
        void onConnected();
        void onConnecting();
        void onDisconnected();
        void onFeatureThrottled();
        void onConnectionError(CometChatException error);
    }

    public static void addAIAssistantListener(String tag, AIAssistantListener l) {}
    public static void removeAIAssistantListener(String tag) {}
    public static void addMessageListener(String tag, MessageListener l) {}
    public static void removeMessageListener(String tag) {}
    public static void addConnectionListener(String tag, ConnectionListener l) {}
    public static void removeConnectionListener(String tag) {}

    /** The server has no pin/save quota endpoint; 0 means "use the Kit default". */
    public static int getPinMessageLimit() { return 0; }
    public static int getSaveMessageLimit() { return 0; }

    public static void callExtension(String slug, String method, String path, JSONObject body,
                                     CallbackListener<JSONObject> listener) {
        listener.onError(new CometChatException("hub_unsupported", "Extensions are not available"));
    }
}
