package com.gochathub.chat.core;

import java.util.*;
import com.gochathub.chat.models.*;
import com.gochathub.chat.enums.*;

/** Query parameters; the hub datasources translate them into REST calls. */
public class UsersRequest {
    /** Not supported: the Kit's datasources fetch over REST. */
    public void fetchNext(CometChat.CallbackListener<List<User>> l) {
        l.onError(new com.gochathub.chat.exceptions.CometChatException("hub_unsupported", "fetchNext is not available"));
    }
    private int limit;
    private long token;
    private String searchKeyword;
    private boolean hideBlockedUsers;
    private String role;
    private boolean friendsOnly;
    private List<String> roles;
    private List<String> tags;
    private boolean withTags;
    private String userStatus;
    private int nextPage;
    private int totalPages;
    private int currentPage;
    private boolean inProgress;
    private List<String> uids;
    private List<String> searchInFields;
    private String sortBy;
    private String sortOrder;
    public int getLimit() { return limit; }
    public String getSearchKeyword() { return searchKeyword; }
    public boolean isHideBlockedUsers() { return hideBlockedUsers; }
    public String getUserStatus() { return userStatus; }
    public String getRole() { return role; }
    public boolean isFriendsOnly() { return friendsOnly; }
    public List<String> getRoles() { return roles; }
    public List<String> getTags() { return tags; }
    public boolean isWithTags() { return withTags; }
    public List<String> getUIDs() { return uids; }
    public String getSortBy() { return sortBy; }
    public String getSortOrder() { return sortOrder; }
    public int getPage() { return currentPage; }
    public static class UsersRequestBuilder {
        private final UsersRequest r = new UsersRequest();
        public UsersRequestBuilder setLimit(int v) { r.limit = v; return this; }
        public UsersRequestBuilder setSearchKeyword(String v) { r.searchKeyword = v; return this; }
        public UsersRequestBuilder hideBlockedUsers(boolean v) { r.hideBlockedUsers = v; return this; }
        public UsersRequestBuilder setUserStatus(String v) { r.userStatus = v; return this; }
        public UsersRequestBuilder setRole(String v) { r.role = v; return this; }
        public UsersRequestBuilder friendsOnly(boolean v) { r.friendsOnly = v; return this; }
        public UsersRequestBuilder setRoles(List<String> v) { r.roles = v; return this; }
        public UsersRequestBuilder setTags(List<String> v) { r.tags = v; return this; }
        public UsersRequestBuilder withTags(boolean v) { r.withTags = v; return this; }
        public UsersRequestBuilder setUIDs(List<String> v) { r.uids = v; return this; }
        // unmapped: searchIn(List<String>)
        public UsersRequestBuilder sortBy(String v) { r.sortBy = v; return this; }
        // unmapped: sortByOrder(String)
        public UsersRequestBuilder setPage(int v) { r.currentPage = v; return this; }
        public UsersRequest build() { return r; }
    }
}
