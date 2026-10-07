package com.gochathub.chat.core;

import java.util.*;
import com.gochathub.chat.models.*;
import com.gochathub.chat.enums.*;

/** Query parameters; the hub datasources translate them into REST calls. */
public class MessagesRequest {
    /** Not supported: the Kit's datasources fetch over REST. */
    public void fetchNext(CometChat.CallbackListener<List<BaseMessage>> l) {
        l.onError(new com.gochathub.chat.exceptions.CometChatException("hub_unsupported", "fetchNext is not available"));
    }
    /** Not supported: the Kit's datasources fetch over REST. */
    public void fetchPrevious(CometChat.CallbackListener<List<BaseMessage>> l) {
        l.onError(new com.gochathub.chat.exceptions.CometChatException("hub_unsupported", "fetchPrevious is not available"));
    }
    private String UID;
    private String GUID;
    private int limit;
    private long messageId;
    private long timestamp;
    private int currentPage;
    private int totalPages;
    private String affix;
    private boolean hasNext;
    private boolean hasPrevious;
    private boolean inProgress;
    private boolean unread;
    private boolean hideMessagesFromBlockedUsers;
    private String searchKeyword;
    private long updatedAfter;
    private boolean updatesOnly;
    private String category;
    private String type;
    private long parentMessageId;
    private boolean hideReplies;
    private boolean hideDeleted;
    private List<String> categories;
    private List<String> types;
    private List<String> tags;
    private boolean withTags;
    private boolean mentionsWithTagInfo;
    private boolean mentionsWithBlockedInfo;
    private boolean interactionGoalCompletedOnly;
    private boolean hasAttachments;
    private boolean hasLinks;
    private boolean hasMentions;
    private boolean hasReactions;
    private List<String> mentionedUIDs;
    private List<AttachmentType> attachmentTypes;
    private boolean withParent;
    private boolean hideQuotedMessages;
    private boolean withThreadSubscribed;
    private boolean pinned;
    private boolean saved;
    private String pinnedSavedCursorId;
    private long lastReturnedPinnedSavedId;
    public int getLimit() { return limit; }
    public String getUID() { return UID; }
    public String getGUID() { return GUID; }
    public long getMessageId() { return messageId; }
    public boolean isUnread() { return unread; }
    public boolean isHideMessagesFromBlockedUsers() { return hideMessagesFromBlockedUsers; }
    public long getTimestamp() { return timestamp; }
    public String getSearchKeyword() { return searchKeyword; }
    public long getUpdatedAfter() { return updatedAfter; }
    public boolean isUpdatesOnly() { return updatesOnly; }
    public String getCategory() { return category; }
    public List<String> getCategories() { return categories; }
    public String getType() { return type; }
    public List<String> getTypes() { return types; }
    public List<AttachmentType> getAttachmentTypes() { return attachmentTypes; }
    public long getParentMessageId() { return parentMessageId; }
    public boolean isHideReplies() { return hideReplies; }
    public boolean isHideDeleted() { return hideDeleted; }
    public List<String> getTags() { return tags; }
    public boolean isWithTags() { return withTags; }
    public boolean isMentionsWithTagInfo() { return mentionsWithTagInfo; }
    public boolean isMentionsWithBlockedInfo() { return mentionsWithBlockedInfo; }
    public boolean isInteractionGoalCompletedOnly() { return interactionGoalCompletedOnly; }
    public boolean isHasAttachments() { return hasAttachments; }
    public boolean isHasLinks() { return hasLinks; }
    public boolean isHasReactions() { return hasReactions; }
    public boolean isHasMentions() { return hasMentions; }
    public boolean isWithParent() { return withParent; }
    public boolean isHideQuotedMessages() { return hideQuotedMessages; }
    public boolean isWithThreadSubscribed() { return withThreadSubscribed; }
    public List<String> getMentionedUIDs() { return mentionedUIDs; }
    public static class MessagesRequestBuilder {
        private final MessagesRequest r = new MessagesRequest();
        public MessagesRequestBuilder setLimit(int v) { r.limit = v; return this; }
        public MessagesRequestBuilder setUID(String v) { r.UID = v; return this; }
        public MessagesRequestBuilder setGUID(String v) { r.GUID = v; return this; }
        public MessagesRequestBuilder setMessageId(long v) { r.messageId = v; return this; }
        public MessagesRequestBuilder setUnread(boolean v) { r.unread = v; return this; }
        public MessagesRequestBuilder hideMessagesFromBlockedUsers(boolean v) { r.hideMessagesFromBlockedUsers = v; return this; }
        public MessagesRequestBuilder setTimestamp(long v) { r.timestamp = v; return this; }
        public MessagesRequestBuilder setSearchKeyword(String v) { r.searchKeyword = v; return this; }
        public MessagesRequestBuilder setUpdatedAfter(long v) { r.updatedAfter = v; return this; }
        public MessagesRequestBuilder updatesOnly(boolean v) { r.updatesOnly = v; return this; }
        public MessagesRequestBuilder setCategory(String v) { r.category = v; return this; }
        public MessagesRequestBuilder setCategories(List<String> v) { r.categories = v; return this; }
        public MessagesRequestBuilder setType(String v) { r.type = v; return this; }
        public MessagesRequestBuilder setTypes(List<String> v) { r.types = v; return this; }
        public MessagesRequestBuilder setAttachmentTypes(List<AttachmentType> v) { r.attachmentTypes = v; return this; }
        public MessagesRequestBuilder setParentMessageId(long v) { r.parentMessageId = v; return this; }
        public MessagesRequestBuilder hideReplies(boolean v) { r.hideReplies = v; return this; }
        public MessagesRequestBuilder hideDeletedMessages(boolean v) { r.hideDeleted = v; return this; }
        public MessagesRequestBuilder setTags(List<String> v) { r.tags = v; return this; }
        public MessagesRequestBuilder withTags(boolean v) { r.withTags = v; return this; }
        public MessagesRequestBuilder mentionsWithTagInfo(boolean v) { r.mentionsWithTagInfo = v; return this; }
        public MessagesRequestBuilder mentionsWithBlockedInfo(boolean v) { r.mentionsWithBlockedInfo = v; return this; }
        public MessagesRequestBuilder setInteractionGoalCompletedOnly(boolean v) { r.interactionGoalCompletedOnly = v; return this; }
        public MessagesRequestBuilder hasAttachments(boolean v) { r.hasAttachments = v; return this; }
        public MessagesRequestBuilder hasLinks(boolean v) { r.hasLinks = v; return this; }
        public MessagesRequestBuilder hasMentions(boolean v) { r.hasMentions = v; return this; }
        public MessagesRequestBuilder hasReactions(boolean v) { r.hasReactions = v; return this; }
        public MessagesRequestBuilder setMentionedUIDs(List<String> v) { r.mentionedUIDs = v; return this; }
        public MessagesRequestBuilder withParent(boolean v) { r.withParent = v; return this; }
        public MessagesRequestBuilder hideQuotedMessages(boolean v) { r.hideQuotedMessages = v; return this; }
        public MessagesRequestBuilder withThreadSubscribed(boolean v) { r.withThreadSubscribed = v; return this; }
        public MessagesRequestBuilder setPinned(boolean v) { r.pinned = v; return this; }
        public MessagesRequestBuilder setSaved(boolean v) { r.saved = v; return this; }
        public MessagesRequest build() { return r; }
    }
}
