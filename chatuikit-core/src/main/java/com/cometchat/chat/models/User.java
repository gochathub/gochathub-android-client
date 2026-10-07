package com.cometchat.chat.models;

import java.util.*;
import java.io.File;
import com.cometchat.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class User extends AppEntity {
    protected String uid;
    protected String name;
    protected String avatar;
    protected String link;
    protected String role;
    protected JSONObject metadata;
    protected String status;
    protected String statusMessage;
    protected long lastActiveAt;
    protected boolean hasBlockedMe;
    protected boolean blockedByMe;
    protected List<String> tags;
    protected long deactivatedAt;
    public User() {}
    public User(String uid, String name) { this.uid = uid; this.name = name; }
    public String getUid() { return uid; }
    public void setUid(String v) { this.uid = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String v) { this.avatar = v; }
    public String getLink() { return link; }
    public void setLink(String v) { this.link = v; }
    public String getRole() { return role; }
    public void setRole(String v) { this.role = v; }
    public JSONObject getMetadata() { return metadata; }
    public void setMetadata(JSONObject v) { this.metadata = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String v) { this.statusMessage = v; }
    public long getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(long v) { this.lastActiveAt = v; }
    public boolean isHasBlockedMe() { return hasBlockedMe; }
    public void setHasBlockedMe(boolean v) { this.hasBlockedMe = v; }
    public boolean isBlockedByMe() { return blockedByMe; }
    public void setBlockedByMe(boolean v) { this.blockedByMe = v; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> v) { this.tags = v; }
    public long getDeactivatedAt() { return deactivatedAt; }
    public void setDeactivatedAt(long v) { this.deactivatedAt = v; }
    @Override public boolean equals(Object o) { return o instanceof User && java.util.Objects.equals((((User) o)).uid, uid); }
    @Override public int hashCode() { return java.util.Objects.hash(uid); }
    @Override public User clone() { return (User) super.clone(); }
    /** Wire form used by the mentions formatter to round-trip a user through span data. */
    public JSONObject toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("uid", uid).put("name", name).put("avatar", avatar).put("role", role).put("status", status);
            return o;
        } catch (org.json.JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    public static User fromJson(String json) {
        try {
            JSONObject o = new JSONObject(json);
            User u = new User(o.optString("uid"), o.optString("name"));
            u.avatar = o.optString("avatar", null);
            u.role = o.optString("role", null);
            u.status = o.optString("status", null);
            return u;
        } catch (org.json.JSONException e) {
            return null;
        }
    }
}
