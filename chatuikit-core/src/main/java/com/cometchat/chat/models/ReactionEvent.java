package com.cometchat.chat.models;

import java.util.*;
import java.io.File;
import com.cometchat.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class ReactionEvent extends ModelBase {
    private Reaction reaction;
    private String receiverId;
    private String receiverType;
    private String conversationId;
    private long parentMessageId;
    public ReactionEvent() {}
    public Reaction getReaction() { return reaction; }
    public void setReaction(Reaction v) { this.reaction = v; }
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String v) { this.receiverId = v; }
    public String getReceiverType() { return receiverType; }
    public void setReceiverType(String v) { this.receiverType = v; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String v) { this.conversationId = v; }
    public long getParentMessageId() { return parentMessageId; }
    public void setParentMessageId(long v) { this.parentMessageId = v; }
    @Override public ReactionEvent clone() { return (ReactionEvent) super.clone(); }
}
