package com.cometchat.chat.models;

import java.util.*;
import java.io.File;
import com.cometchat.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class GroupMember extends User {
    private String scope;
    private long joinedAt;
    public GroupMember() {}
    public GroupMember(String uid, String scope) { this.uid = uid; this.scope = scope; }
    public String getScope() { return scope; }
    public void setScope(String v) { this.scope = v; }
    public long getJoinedAt() { return joinedAt; }
    public void setJoinedAt(long v) { this.joinedAt = v; }
    @Override public GroupMember clone() { return (GroupMember) super.clone(); }
}
