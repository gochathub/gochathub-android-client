package com.gochathub.chat.core;

import com.gochathub.chat.models.BaseMessage;

/** Calls are out of scope (no WebRTC); kept so the conversation list's type switches compile. */
public class Call extends BaseMessage {
    public String getCallStatus() { return null; }
    public String getSessionId() { return null; }
}
