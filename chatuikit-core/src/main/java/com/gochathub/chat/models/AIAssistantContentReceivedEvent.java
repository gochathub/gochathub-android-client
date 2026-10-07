package com.gochathub.chat.models;

import java.util.*;
import java.io.File;
import com.gochathub.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class AIAssistantContentReceivedEvent extends AIAssistantBaseEvent {
    private String streamMessageId;
    private long runId;
    private String threadId;
    private String delta;
    public AIAssistantContentReceivedEvent() {}
    public String getStreamMessageId() { return streamMessageId; }
    public void setStreamMessageId(String v) { this.streamMessageId = v; }
    public long getRunId() { return runId; }
    public void setRunId(long v) { this.runId = v; }
    public String getThreadId() { return threadId; }
    public void setThreadId(String v) { this.threadId = v; }
    public String getDelta() { return delta; }
    public void setDelta(String v) { this.delta = v; }
    @Override public AIAssistantContentReceivedEvent clone() { return (AIAssistantContentReceivedEvent) super.clone(); }
}
