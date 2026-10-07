package com.gochathub.chat.models;

import java.util.*;
import java.io.File;
import com.gochathub.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class InteractionReceipt extends ModelBase {
    private String receiverId;
    private long messageId;
    private List<Interaction> interactions;
    private String receiverType;
    private String messageSenderUid;
    private User sender;
    public InteractionReceipt() {}
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String v) { this.receiverId = v; }
    public long getMessageId() { return messageId; }
    public void setMessageId(long v) { this.messageId = v; }
    public List<Interaction> getInteractions() { return interactions; }
    public void setInteractions(List<Interaction> v) { this.interactions = v; }
    public String getReceiverType() { return receiverType; }
    public void setReceiverType(String v) { this.receiverType = v; }
    public String getMessageSenderUid() { return messageSenderUid; }
    public void setMessageSenderUid(String v) { this.messageSenderUid = v; }
    public User getSender() { return sender; }
    public void setSender(User v) { this.sender = v; }
    @Override public InteractionReceipt clone() { return (InteractionReceipt) super.clone(); }
}
