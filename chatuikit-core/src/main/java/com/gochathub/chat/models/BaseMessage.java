package com.gochathub.chat.models;

import java.util.*;
import java.io.File;
import com.gochathub.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class BaseMessage extends AppEntity {
    public static final String TABLE_CONVERSATIONS = "Conversations";
    protected long id;
    protected String muid;
    protected User sender;
    protected AppEntity receiver;
    protected String receiverUid;
    protected long quotedMessageId;
    protected BaseMessage quotedMessage;
    protected String type;
    protected String receiverType;
    protected String category;
    protected long sentAt;
    protected long deliveredAt;
    protected long readAt;
    protected JSONObject metadata;
    protected long readByMeAt;
    protected long deliveredToMeAt;
    protected long deletedAt;
    protected long editedAt;
    protected String deletedBy;
    protected String editedBy;
    protected long updatedAt;
    protected String conversationId;
    protected long parentMessageId;
    protected int replyCount;
    protected List<User> mentionedUser;
    protected boolean hasMentionedMe;
    protected JSONObject rawMessage;
    protected List<ReactionCount> reactions;
    protected long pinnedAt;
    protected String pinnedBy;
    protected long savedAt;
    protected boolean threadSubscribed;
    protected int unreadRepliesCount;
    public BaseMessage() {}
    public BaseMessage(String receiverUid, String type, String receiverType) { this.receiverUid = receiverUid; this.type = type; this.receiverType = receiverType; }
    public long getId() { return id; }
    public void setId(long v) { this.id = v; }
    public String getMuid() { return muid; }
    public void setMuid(String v) { this.muid = v; }
    public User getSender() { return sender; }
    public void setSender(User v) { this.sender = v; }
    public AppEntity getReceiver() { return receiver; }
    public void setReceiver(AppEntity v) { this.receiver = v; }
    public String getReceiverUid() { return receiverUid; }
    public void setReceiverUid(String v) { this.receiverUid = v; }
    public long getQuotedMessageId() { return quotedMessageId; }
    public void setQuotedMessageId(long v) { this.quotedMessageId = v; }
    public BaseMessage getQuotedMessage() { return quotedMessage; }
    public void setQuotedMessage(BaseMessage v) { this.quotedMessage = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public String getReceiverType() { return receiverType; }
    public void setReceiverType(String v) { this.receiverType = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }
    public long getSentAt() { return sentAt; }
    public void setSentAt(long v) { this.sentAt = v; }
    public long getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(long v) { this.deliveredAt = v; }
    public long getReadAt() { return readAt; }
    public void setReadAt(long v) { this.readAt = v; }
    public JSONObject getMetadata() { return metadata; }
    public void setMetadata(JSONObject v) { this.metadata = v; }
    public long getReadByMeAt() { return readByMeAt; }
    public void setReadByMeAt(long v) { this.readByMeAt = v; }
    public long getDeliveredToMeAt() { return deliveredToMeAt; }
    public void setDeliveredToMeAt(long v) { this.deliveredToMeAt = v; }
    public long getDeletedAt() { return deletedAt; }
    public void setDeletedAt(long v) { this.deletedAt = v; }
    public long getEditedAt() { return editedAt; }
    public void setEditedAt(long v) { this.editedAt = v; }
    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String v) { this.deletedBy = v; }
    public String getEditedBy() { return editedBy; }
    public void setEditedBy(String v) { this.editedBy = v; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long v) { this.updatedAt = v; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String v) { this.conversationId = v; }
    public long getParentMessageId() { return parentMessageId; }
    public void setParentMessageId(long v) { this.parentMessageId = v; }
    public int getReplyCount() { return replyCount; }
    public void setReplyCount(int v) { this.replyCount = v; }
    public void setHasMentionedMe(boolean v) { this.hasMentionedMe = v; }
    public JSONObject getRawMessage() { return rawMessage; }
    public void setRawMessage(JSONObject v) { this.rawMessage = v; }
    public List<ReactionCount> getReactions() { return reactions; }
    public void setReactions(List<ReactionCount> v) { this.reactions = v; }
    public long getPinnedAt() { return pinnedAt; }
    public void setPinnedAt(long v) { this.pinnedAt = v; }
    public String getPinnedBy() { return pinnedBy; }
    public void setPinnedBy(String v) { this.pinnedBy = v; }
    public long getSavedAt() { return savedAt; }
    public void setSavedAt(long v) { this.savedAt = v; }
    public boolean isThreadSubscribed() { return threadSubscribed; }
    public void setThreadSubscribed(boolean v) { this.threadSubscribed = v; }
    public int getUnreadRepliesCount() { return unreadRepliesCount; }
    public void setUnreadRepliesCount(int v) { this.unreadRepliesCount = v; }
    @Override public boolean equals(Object o) { return o instanceof BaseMessage && java.util.Objects.equals((((BaseMessage) o)).id, id); }
    @Override public int hashCode() { return java.util.Objects.hash(id); }
    @Override public BaseMessage clone() { return (BaseMessage) super.clone(); }
    public boolean isPinned() { return pinnedAt > 0; }
    public boolean isSaved() { return savedAt > 0; }
    public boolean hasMentionedMe() { return hasMentionedMe; }
    public List<User> getMentionedUsers() { return mentionedUser; }
    public void setMentionedUsers(List<User> v) { this.mentionedUser = v; }
}
