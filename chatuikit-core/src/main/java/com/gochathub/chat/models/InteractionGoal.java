package com.gochathub.chat.models;

import java.util.*;
import java.io.File;
import com.gochathub.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class InteractionGoal extends ModelBase {
    private List<String> elementIds;
    private String type;
    public InteractionGoal() {}
    public InteractionGoal(String type, List<String> elementIds) { this.type = type; this.elementIds = elementIds; }
    public List<String> getElementIds() { return elementIds; }
    public void setElementIds(List<String> v) { this.elementIds = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    @Override public InteractionGoal clone() { return (InteractionGoal) super.clone(); }
}
