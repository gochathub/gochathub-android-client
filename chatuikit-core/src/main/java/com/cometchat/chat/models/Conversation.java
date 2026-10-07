package com.cometchat.chat.models;

import java.util.*;
import java.io.File;
import com.cometchat.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class Conversation extends ModelBase {
    private String conversationId;
    private String conversationType;
    private BaseMessage lastMessage;
    private AppEntity conversationWith;
    private int unreadMessageCount;
    private long updatedAt;
    private List<String> tags;
    private int unreadMentionsCount;
    private long lastReadMessageId;
    private long latestMessageId;
    private long pinnedAt;
    private String pinnedBy;
    public Conversation() {}
    public Conversation(String conversationId, String conversationType) { this.conversationId = conversationId; this.conversationType = conversationType; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String v) { this.conversationId = v; }
    public String getConversationType() { return conversationType; }
    public void setConversationType(String v) { this.conversationType = v; }
    public BaseMessage getLastMessage() { return lastMessage; }
    public void setLastMessage(BaseMessage v) { this.lastMessage = v; }
    public AppEntity getConversationWith() { return conversationWith; }
    public void setConversationWith(AppEntity v) { this.conversationWith = v; }
    public int getUnreadMessageCount() { return unreadMessageCount; }
    public void setUnreadMessageCount(int v) { this.unreadMessageCount = v; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long v) { this.updatedAt = v; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> v) { this.tags = v; }
    public int getUnreadMentionsCount() { return unreadMentionsCount; }
    public void setUnreadMentionsCount(int v) { this.unreadMentionsCount = v; }
    public long getLastReadMessageId() { return lastReadMessageId; }
    public void setLastReadMessageId(long v) { this.lastReadMessageId = v; }
    public long getLatestMessageId() { return latestMessageId; }
    public void setLatestMessageId(long v) { this.latestMessageId = v; }
    public long getPinnedAt() { return pinnedAt; }
    public void setPinnedAt(long v) { this.pinnedAt = v; }
    public String getPinnedBy() { return pinnedBy; }
    public void setPinnedBy(String v) { this.pinnedBy = v; }
    @Override public boolean equals(Object o) { return o instanceof Conversation && java.util.Objects.equals((((Conversation) o)).conversationId, conversationId); }
    @Override public int hashCode() { return java.util.Objects.hash(conversationId); }
    @Override public Conversation clone() { return (Conversation) super.clone(); }
    public boolean isPinned() { return pinnedAt > 0; }
    public boolean isSystemPinned() { return false; }
}
