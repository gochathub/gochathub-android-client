package com.gochathub.chat.models;

import java.util.*;
import java.io.File;
import com.gochathub.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class AIToolResultMessage extends BaseMessage {
    private long runId;
    private String threadId;
    private String text;
    private String toolCallId;
    private List<String> tags;
    public AIToolResultMessage() {}
    public AIToolResultMessage(String receiverUid, String text, String receiverType) { this.receiverUid = receiverUid; this.text = text; this.receiverType = receiverType; }
    public long getRunId() { return runId; }
    public void setRunId(long v) { this.runId = v; }
    public String getThreadId() { return threadId; }
    public void setThreadId(String v) { this.threadId = v; }
    public String getText() { return text; }
    public void setText(String v) { this.text = v; }
    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String v) { this.toolCallId = v; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> v) { this.tags = v; }
    @Override public AIToolResultMessage clone() { return (AIToolResultMessage) super.clone(); }
}
