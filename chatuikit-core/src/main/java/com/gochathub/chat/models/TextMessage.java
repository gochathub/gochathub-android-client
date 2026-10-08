package com.gochathub.chat.models;

import java.util.*;
import java.io.File;
import com.gochathub.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class TextMessage extends BaseMessage {
    private String text;
    protected List<String> tags;
    private ModerationStatus moderationStatus;
    public TextMessage() { this.type = "text"; }
    public TextMessage(String receiverUid, String text, String receiverType) { this.receiverUid = receiverUid; this.text = text; this.receiverType = receiverType; this.type = "text"; }
    public String getText() { return text; }
    public void setText(String v) { this.text = v; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> v) { this.tags = v; }
    public ModerationStatus getModerationStatus() { return moderationStatus; }
    public void setModerationStatus(ModerationStatus v) { this.moderationStatus = v; }
    @Override public TextMessage clone() { return (TextMessage) super.clone(); }
}
