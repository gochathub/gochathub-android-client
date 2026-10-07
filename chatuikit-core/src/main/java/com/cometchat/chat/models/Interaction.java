package com.cometchat.chat.models;

import java.util.*;
import java.io.File;
import com.cometchat.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class Interaction extends ModelBase {
    private String elementId;
    private long interactedAt;
    public Interaction() {}
    public Interaction(String elementId, long interactedAt) { this.elementId = elementId; this.interactedAt = interactedAt; }
    public String getElementId() { return elementId; }
    public void setElementId(String v) { this.elementId = v; }
    public long getInteractedAt() { return interactedAt; }
    public void setInteractedAt(long v) { this.interactedAt = v; }
    @Override public Interaction clone() { return (Interaction) super.clone(); }
}
