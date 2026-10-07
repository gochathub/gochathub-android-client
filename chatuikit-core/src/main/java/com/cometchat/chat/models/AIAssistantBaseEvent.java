package com.cometchat.chat.models;

import java.util.*;
import java.io.File;
import com.cometchat.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class AIAssistantBaseEvent extends ModelBase {
    private long id;
    private String type;
    private String conversationId;
    private String parentId;
    private JSONObject additionalProperties;
    public AIAssistantBaseEvent() {}
    public long getId() { return id; }
    public void setId(long v) { this.id = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String v) { this.conversationId = v; }
    public String getParentId() { return parentId; }
    public void setParentId(String v) { this.parentId = v; }
    public JSONObject getAdditionalProperties() { return additionalProperties; }
    public void setAdditionalProperties(JSONObject v) { this.additionalProperties = v; }
    @Override public AIAssistantBaseEvent clone() { return (AIAssistantBaseEvent) super.clone(); }
}
