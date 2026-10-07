package com.gochathub.chat.models;

import java.util.*;
import java.io.File;
import com.gochathub.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class AIToolCall extends ModelBase {
    private String id;
    private String type;
    private String displayName;
    private String executionText;
    private AIToolCallFunction function;
    public AIToolCall() {}
    public String getId() { return id; }
    public void setId(String v) { this.id = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String v) { this.displayName = v; }
    public String getExecutionText() { return executionText; }
    public void setExecutionText(String v) { this.executionText = v; }
    public AIToolCallFunction getFunction() { return function; }
    public void setFunction(AIToolCallFunction v) { this.function = v; }
    @Override public AIToolCall clone() { return (AIToolCall) super.clone(); }
}
