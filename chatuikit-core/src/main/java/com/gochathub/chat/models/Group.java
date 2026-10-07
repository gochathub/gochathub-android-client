package com.gochathub.chat.models;

import java.util.*;
import java.io.File;
import com.gochathub.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class Group extends AppEntity {
    public static final String TABLE_GROUPS = "Groups";
    public static final String COLUMN_GUID = "guid";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_GROUP_TYPE = "type";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_ICON = "icon";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_OWNER = "owner";
    public static final String COLUMN_METADATA = "metadata";
    public static final String COLUMN_CREATED_AT = "createdAt";
    public static final String COLUMN_UPDATED_AT = "updatedAt";
    public static final String COLUMN_HAS_JOINED = "hasJoined";
    public static final String COLUMN_IDENTITY = "groupIdentity";
    private String guid;
    private String name;
    private String type;
    private String password;
    private String icon;
    private String description;
    private String owner;
    private JSONObject metadata;
    private long createdAt;
    private long updatedAt;
    private boolean hasJoined;
    private long joinedAt;
    private String scope;
    private int membersCount;
    private List<String> tags;
    private boolean isBannedFromGroup;
    public Group() {}
    public Group(String guid, String name, String type, String password) { this.guid = guid; this.name = name; this.type = type; this.password = password; }
    public Group(String guid, String name, String type, String password, String icon, String description) { this.guid = guid; this.name = name; this.type = type; this.password = password; this.icon = icon; this.description = description; }
    public String getGuid() { return guid; }
    public void setGuid(String v) { this.guid = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getPassword() { return password; }
    public void setPassword(String v) { this.password = v; }
    public String getIcon() { return icon; }
    public void setIcon(String v) { this.icon = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getOwner() { return owner; }
    public void setOwner(String v) { this.owner = v; }
    public JSONObject getMetadata() { return metadata; }
    public void setMetadata(JSONObject v) { this.metadata = v; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long v) { this.createdAt = v; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long v) { this.updatedAt = v; }
    public void setHasJoined(boolean v) { this.hasJoined = v; }
    public long getJoinedAt() { return joinedAt; }
    public void setJoinedAt(long v) { this.joinedAt = v; }
    public String getScope() { return scope; }
    public void setScope(String v) { this.scope = v; }
    public int getMembersCount() { return membersCount; }
    public void setMembersCount(int v) { this.membersCount = v; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> v) { this.tags = v; }
    @Override public boolean equals(Object o) { return o instanceof Group && java.util.Objects.equals((((Group) o)).guid, guid); }
    @Override public int hashCode() { return java.util.Objects.hash(guid); }
    @Override public Group clone() { return (Group) super.clone(); }
    public String getGroupType() { return type; }
    public void setGroupType(String v) { this.type = v; }
    public boolean isJoined() { return hasJoined; }
    public boolean isBannedFromGroup() { return isBannedFromGroup; }
}
