package com.gochathub.chat.models;

import java.util.*;
import java.io.File;
import com.gochathub.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class Reaction extends ModelBase {
    private String id;
    private long messageId;
    private String reaction;
    private String uid;
    private long reactedAt;
    private User reactedBy;
    public Reaction() {}
    public long getMessageId() { return messageId; }
    public void setMessageId(long v) { this.messageId = v; }
    public String getReaction() { return reaction; }
    public void setReaction(String v) { this.reaction = v; }
    public String getUid() { return uid; }
    public void setUid(String v) { this.uid = v; }
    public long getReactedAt() { return reactedAt; }
    public void setReactedAt(long v) { this.reactedAt = v; }
    public User getReactedBy() { return reactedBy; }
    public void setReactedBy(User v) { this.reactedBy = v; }
    @Override public Reaction clone() { return (Reaction) super.clone(); }
    public String getReactionId() { return id; }
    public void setReactionId(String v) { this.id = v; }
}
