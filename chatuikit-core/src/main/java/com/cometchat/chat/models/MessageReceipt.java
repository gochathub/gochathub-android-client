package com.cometchat.chat.models;

import java.util.*;
import java.io.File;
import com.cometchat.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class MessageReceipt extends ModelBase {
    public static final String RECEIPT_TYPE_DELIVERED = "delivered";
    public static final String RECEIPT_TYPE_READ = "read";
    public static final String RECEIPT_TYPE_DELIVERED_TO_ALL = "deliveredToAll";
    public static final String RECEIPT_TYPE_READ_BY_ALL = "readByAll";
    private long messageId;
    private User sender;
    private String receiverType;
    private String receiverId;
    private long timestamp;
    private String receiptType;
    private long deliveredAt;
    private long readAt;
    private String messageSender;
    public MessageReceipt() {}
    public long getMessageId() { return messageId; }
    public void setMessageId(long v) { this.messageId = v; }
    public User getSender() { return sender; }
    public void setSender(User v) { this.sender = v; }
    public String getReceiverType() { return receiverType; }
    public void setReceiverType(String v) { this.receiverType = v; }
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String v) { this.receiverId = v; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long v) { this.timestamp = v; }
    public String getReceiptType() { return receiptType; }
    public void setReceiptType(String v) { this.receiptType = v; }
    public long getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(long v) { this.deliveredAt = v; }
    public long getReadAt() { return readAt; }
    public void setReadAt(long v) { this.readAt = v; }
    public String getMessageSender() { return messageSender; }
    public void setMessageSender(String v) { this.messageSender = v; }
    @Override public MessageReceipt clone() { return (MessageReceipt) super.clone(); }
    public String getReceivertype() { return receiverType; }
    public void setReceivertype(String v) { this.receiverType = v; }
}
