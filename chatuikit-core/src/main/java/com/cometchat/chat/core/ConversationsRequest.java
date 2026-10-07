package com.cometchat.chat.core;

import java.util.*;
import com.cometchat.chat.models.*;
import com.cometchat.chat.enums.*;

/** Query parameters; the hub datasources translate them into REST calls. */
public class ConversationsRequest {
    /** Not supported: the Kit's datasources fetch over REST. */
    public void fetchNext(CometChat.CallbackListener<List<Conversation>> l) {
        l.onError(new com.cometchat.chat.exceptions.CometChatException("hub_unsupported", "fetchNext is not available"));
    }
    private int limit;
    private String conversationType;
    private boolean withUserAndGroupTags;
    private List<String> tags;
    private boolean withTags;
    private String searchKeyword;
    private int nextPage;
    private int totalPages;
    private int currentPage;
    private boolean inProgress;
    private List<String> userTags;
    private List<String> groupTags;
    private boolean includeBlockedUsers;
    private boolean withBlockedInfo;
    private boolean unread;
    private boolean hideAgentic;
    private boolean onlyAgentic;
    private String pinnedBy;
    public int getLimit() { return limit; }
    public String getConversationType() { return conversationType; }
    public boolean isWithUserAndGroupTags() { return withUserAndGroupTags; }
    public List<String> getTags() { return tags; }
    public boolean isWithTags() { return withTags; }
    public List<String> getUserTags() { return userTags; }
    public List<String> getGroupTags() { return groupTags; }
    public boolean isIncludeBlockedUsers() { return includeBlockedUsers; }
    public boolean isWithBlockedInfo() { return withBlockedInfo; }
    public String getSearchKeyword() { return searchKeyword; }
    public boolean isUnread() { return unread; }
    public int getPage() { return currentPage; }
    public boolean isHideAgentic() { return hideAgentic; }
    public boolean isOnlyAgentic() { return onlyAgentic; }
    public static class ConversationsRequestBuilder {
        private final ConversationsRequest r = new ConversationsRequest();
        public ConversationsRequestBuilder setLimit(int v) { r.limit = v; return this; }
        public ConversationsRequestBuilder setConversationType(String v) { r.conversationType = v; return this; }
        public ConversationsRequestBuilder withUserAndGroupTags(boolean v) { r.withUserAndGroupTags = v; return this; }
        public ConversationsRequestBuilder setTags(List<String> v) { r.tags = v; return this; }
        public ConversationsRequestBuilder withTags(boolean v) { r.withTags = v; return this; }
        public ConversationsRequestBuilder setUserTags(List<String> v) { r.userTags = v; return this; }
        public ConversationsRequestBuilder setGroupTags(List<String> v) { r.groupTags = v; return this; }
        public ConversationsRequestBuilder includeBlockedUsers(boolean v) { r.includeBlockedUsers = v; return this; }
        public ConversationsRequestBuilder withBlockedInfo(boolean v) { r.withBlockedInfo = v; return this; }
        public ConversationsRequestBuilder setSearchKeyword(String v) { r.searchKeyword = v; return this; }
        public ConversationsRequestBuilder setUnread(boolean v) { r.unread = v; return this; }
        public ConversationsRequestBuilder setPage(int v) { r.currentPage = v; return this; }
        public ConversationsRequestBuilder hideAgentic(boolean v) { r.hideAgentic = v; return this; }
        public ConversationsRequestBuilder onlyAgentic(boolean v) { r.onlyAgentic = v; return this; }
        public ConversationsRequestBuilder setPinnedBy(String v) { r.pinnedBy = v; return this; }
        public ConversationsRequest build() { return r; }
    }
}
