package com.cometchat.chat.models;

import java.util.*;
import java.io.File;
import com.cometchat.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class TypingIndicator extends ModelBase {
    public static final String TYPING_START = "started";
    public static final String TYPING_END = "ended";
    private User sender;
    private String receiverId;
    private String receiverType;
    private JSONObject metadata;
    private long lastTimestamp;
    private String typingStatus;
    public TypingIndicator() {}
    public TypingIndicator(String receiverId, String receiverType) { this.receiverId = receiverId; this.receiverType = receiverType; }
    public TypingIndicator(String receiverId, String receiverType, JSONObject metadata) { this.receiverId = receiverId; this.receiverType = receiverType; this.metadata = metadata; }
    public User getSender() { return sender; }
    public void setSender(User v) { this.sender = v; }
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String v) { this.receiverId = v; }
    public String getReceiverType() { return receiverType; }
    public void setReceiverType(String v) { this.receiverType = v; }
    public JSONObject getMetadata() { return metadata; }
    public void setMetadata(JSONObject v) { this.metadata = v; }
    public long getLastTimestamp() { return lastTimestamp; }
    public void setLastTimestamp(long v) { this.lastTimestamp = v; }
    public String getTypingStatus() { return typingStatus; }
    public void setTypingStatus(String v) { this.typingStatus = v; }
    @Override public TypingIndicator clone() { return (TypingIndicator) super.clone(); }
}
