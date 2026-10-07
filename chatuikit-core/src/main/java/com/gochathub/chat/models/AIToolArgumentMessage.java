package com.gochathub.chat.models;

import java.util.*;
import java.io.File;
import com.gochathub.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class AIToolArgumentMessage extends BaseMessage {
    private long runId;
    private String threadId;
    private List<AIToolCall> toolCalls;
    private List<String> tags;
    public AIToolArgumentMessage() {}
    public AIToolArgumentMessage(String receiverUid, String receiverType) { this.receiverUid = receiverUid; this.receiverType = receiverType; }
    public long getRunId() { return runId; }
    public void setRunId(long v) { this.runId = v; }
    public String getThreadId() { return threadId; }
    public void setThreadId(String v) { this.threadId = v; }
    public List<AIToolCall> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<AIToolCall> v) { this.toolCalls = v; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> v) { this.tags = v; }
    @Override public AIToolArgumentMessage clone() { return (AIToolArgumentMessage) super.clone(); }
}
