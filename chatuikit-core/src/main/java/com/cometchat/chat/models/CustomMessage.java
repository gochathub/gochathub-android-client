package com.cometchat.chat.models;

import java.util.*;
import java.io.File;
import com.cometchat.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class CustomMessage extends BaseMessage {
    private String subType;
    private JSONObject customData;
    private List<String> tags;
    private String text;
    private boolean updateConversation;
    private boolean sendNotification;
    private String conversationText;
    public CustomMessage() {}
    public CustomMessage(String receiverUid, String receiverType, String subType, JSONObject customData) { this.receiverUid = receiverUid; this.receiverType = receiverType; this.subType = subType; this.customData = customData; }
    public String getSubType() { return subType; }
    public void setSubType(String v) { this.subType = v; }
    public JSONObject getCustomData() { return customData; }
    public void setCustomData(JSONObject v) { this.customData = v; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> v) { this.tags = v; }
    @Override public CustomMessage clone() { return (CustomMessage) super.clone(); }
    public boolean willUpdateConversation() { return updateConversation; }
    public void shouldUpdateConversation(boolean v) { this.updateConversation = v; }
    public boolean willSendNotification() { return sendNotification; }
    public void shouldSendNotification(boolean v) { this.sendNotification = v; }
    public String getConversationText() { return conversationText; }
    public void setConversationText(String v) { this.conversationText = v; }
}
