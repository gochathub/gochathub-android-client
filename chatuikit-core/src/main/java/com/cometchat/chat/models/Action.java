package com.cometchat.chat.models;

import java.util.*;
import java.io.File;
import com.cometchat.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class Action extends BaseMessage {
    private AppEntity actionBy;
    private AppEntity actionFor;
    private AppEntity actionOn;
    private String message;
    private String rawData;
    private String action;
    private String oldScope;
    private String newScope;
    public Action() {}
    public AppEntity getActionBy() { return actionBy; }
    public void setActionBy(AppEntity v) { this.actionBy = v; }
    public AppEntity getActionFor() { return actionFor; }
    public void setActionFor(AppEntity v) { this.actionFor = v; }
    public AppEntity getActionOn() { return actionOn; }
    public void setActionOn(AppEntity v) { this.actionOn = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }
    public String getRawData() { return rawData; }
    public void setRawData(String v) { this.rawData = v; }
    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }
    public String getOldScope() { return oldScope; }
    public void setOldScope(String v) { this.oldScope = v; }
    public String getNewScope() { return newScope; }
    public void setNewScope(String v) { this.newScope = v; }
    @Override public Action clone() { return (Action) super.clone(); }
    public AppEntity getActioBy() { return actionBy; }
    public void setActioBy(AppEntity v) { this.actionBy = v; }
}
