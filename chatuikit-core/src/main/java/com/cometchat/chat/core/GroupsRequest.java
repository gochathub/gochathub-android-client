package com.cometchat.chat.core;

import java.util.*;
import com.cometchat.chat.models.*;
import com.cometchat.chat.enums.*;

/** Query parameters; the hub datasources translate them into REST calls. */
public class GroupsRequest {
    /** Not supported: the Kit's datasources fetch over REST. */
    public void fetchNext(CometChat.CallbackListener<List<Group>> l) {
        l.onError(new com.cometchat.chat.exceptions.CometChatException("hub_unsupported", "fetchNext is not available"));
    }
    private int limit;
    private long cursor;
    private String searchKeyword;
    private int nextPage;
    private int totalPages;
    private int currentPage;
    private boolean inProgress;
    private boolean joinedOnly;
    private List<String> tags;
    private boolean withTags;
    public int getLimit() { return limit; }
    public String getSearchKeyword() { return searchKeyword; }
    public boolean isJoinedOnly() { return joinedOnly; }
    public List<String> getTags() { return tags; }
    public boolean isWithTags() { return withTags; }
    public int getPage() { return currentPage; }
    public static class GroupsRequestBuilder {
        private final GroupsRequest r = new GroupsRequest();
        public GroupsRequestBuilder setLimit(int v) { r.limit = v; return this; }
        public GroupsRequestBuilder setSearchKeyWord(String v) { r.searchKeyword = v; return this; }
        public GroupsRequestBuilder joinedOnly(boolean v) { r.joinedOnly = v; return this; }
        public GroupsRequestBuilder setTags(List<String> v) { r.tags = v; return this; }
        public GroupsRequestBuilder withTags(boolean v) { r.withTags = v; return this; }
        public GroupsRequestBuilder setPage(int v) { r.currentPage = v; return this; }
        public GroupsRequest build() { return r; }
    }
}
