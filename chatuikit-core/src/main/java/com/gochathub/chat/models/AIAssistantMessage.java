package com.gochathub.chat.models;

import java.util.*;
import java.io.File;
import com.gochathub.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class AIAssistantMessage extends BaseMessage {
    private long runId;
    private String threadId;
    private String text;
    private List<String> tags;
    private List<AIAssistantElement> elements;
    public AIAssistantMessage() {}
    public AIAssistantMessage(String receiverUid, String text, String receiverType) { this.receiverUid = receiverUid; this.text = text; this.receiverType = receiverType; }
    public long getRunId() { return runId; }
    public void setRunId(long v) { this.runId = v; }
    public String getThreadId() { return threadId; }
    public void setThreadId(String v) { this.threadId = v; }
    public String getText() { return text; }
    public void setText(String v) { this.text = v; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> v) { this.tags = v; }
    public List<AIAssistantElement> getElements() { return elements; }
    public void setElements(List<AIAssistantElement> v) { this.elements = v; }
    @Override public AIAssistantMessage clone() { return (AIAssistantMessage) super.clone(); }
}
