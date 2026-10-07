package com.cometchat.chat.models;

import java.util.*;
import java.io.File;
import com.cometchat.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class TransientMessage extends ModelBase {
    private String receiverId;
    private String receiverType;
    private User sender;
    private JSONObject data;
    public TransientMessage() {}
    public TransientMessage(String receiverId, String receiverType, JSONObject data) { this.receiverId = receiverId; this.receiverType = receiverType; this.data = data; }
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String v) { this.receiverId = v; }
    public String getReceiverType() { return receiverType; }
    public void setReceiverType(String v) { this.receiverType = v; }
    public User getSender() { return sender; }
    public void setSender(User v) { this.sender = v; }
    public JSONObject getData() { return data; }
    public void setData(JSONObject v) { this.data = v; }
    @Override public TransientMessage clone() { return (TransientMessage) super.clone(); }
}
