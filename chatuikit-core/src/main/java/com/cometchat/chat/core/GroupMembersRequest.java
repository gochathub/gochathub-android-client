package com.cometchat.chat.core;

import java.util.*;
import com.cometchat.chat.models.*;
import com.cometchat.chat.enums.*;

/** Query parameters; the hub datasources translate them into REST calls. */
public class GroupMembersRequest {
    /** Not supported: the Kit's datasources fetch over REST. */
    public void fetchNext(CometChat.CallbackListener<List<GroupMember>> l) {
        l.onError(new com.cometchat.chat.exceptions.CometChatException("hub_unsupported", "fetchNext is not available"));
    }
    private String guid;
    private int limit;
    private int currentPage;
    private int totalPages;
    private int nextPage;
    private long cursor;
    private String searchKeyword;
    private List<String> scopes;
    private String status;
    public String getGuid() { return guid; }
    public int getLimit() { return limit; }
    public String getSearchKeyword() { return searchKeyword; }
    public List<String> getScopes() { return scopes; }
    public int getPage() { return currentPage; }
    public String getStatus() { return status; }
    public static class GroupMembersRequestBuilder {
        public GroupMembersRequestBuilder(String guid) { r.guid = guid; }
        private final GroupMembersRequest r = new GroupMembersRequest();
        public GroupMembersRequestBuilder setGuid(String v) { r.guid = v; return this; }
        public GroupMembersRequestBuilder setLimit(int v) { r.limit = v; return this; }
        public GroupMembersRequestBuilder setSearchKeyword(String v) { r.searchKeyword = v; return this; }
        public GroupMembersRequestBuilder setScopes(List<String> v) { r.scopes = v; return this; }
        public GroupMembersRequestBuilder setStatus(String v) { r.status = v; return this; }
        public GroupMembersRequestBuilder setPage(int v) { r.currentPage = v; return this; }
        public GroupMembersRequest build() { return r; }
    }
}
