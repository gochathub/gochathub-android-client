package com.cometchat.chat.models;

import java.util.*;
import java.io.File;
import com.cometchat.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class ReactionCount extends ModelBase {
    private String reaction;
    private boolean reactedByMe;
    private int count;
    public ReactionCount() {}
    public String getReaction() { return reaction; }
    public void setReaction(String v) { this.reaction = v; }
    public boolean getReactedByMe() { return reactedByMe; }
    public void setReactedByMe(boolean v) { this.reactedByMe = v; }
    public int getCount() { return count; }
    public void setCount(int v) { this.count = v; }
    @Override public ReactionCount clone() { return (ReactionCount) super.clone(); }
}
