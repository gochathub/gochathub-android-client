package com.cometchat.chat.models;

import java.util.*;
import java.io.File;
import com.cometchat.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class FlagReason extends ModelBase {
    private long createdAt;
    private String description;
    private String id;
    private String name;
    private long updatedAt;
    public FlagReason() {}
    public FlagReason(String id, String name) { this.id = id; this.name = name; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long v) { this.createdAt = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getId() { return id; }
    public void setId(String v) { this.id = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long v) { this.updatedAt = v; }
    @Override public FlagReason clone() { return (FlagReason) super.clone(); }
}
