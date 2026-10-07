package com.gochathub.chat.models;

import java.util.*;
import java.io.File;
import com.gochathub.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class AIAssistantToolEndedEvent extends AIAssistantBaseEvent {
    private long runId;
    private String threadId;
    private String toolCallId;
    private String toolCallName;
    private String displayName;
    private String executionText;
    private String arguments;
    public AIAssistantToolEndedEvent() {}
    public long getRunId() { return runId; }
    public void setRunId(long v) { this.runId = v; }
    public String getThreadId() { return threadId; }
    public void setThreadId(String v) { this.threadId = v; }
    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String v) { this.toolCallId = v; }
    public String getToolCallName() { return toolCallName; }
    public void setToolCallName(String v) { this.toolCallName = v; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String v) { this.displayName = v; }
    public String getExecutionText() { return executionText; }
    public void setExecutionText(String v) { this.executionText = v; }
    public String getArguments() { return arguments; }
    public void setArguments(String v) { this.arguments = v; }
    @Override public AIAssistantToolEndedEvent clone() { return (AIAssistantToolEndedEvent) super.clone(); }
}
