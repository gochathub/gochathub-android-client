package com.cometchat.chat.models;

import java.util.*;
import java.io.File;
import com.cometchat.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class AIAssistantElement extends ModelBase {
    private String type;
    private Object data;
    public AIAssistantElement() {}
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public Object getData() { return data; }
    public void setData(Object v) { this.data = v; }
    @Override public AIAssistantElement clone() { return (AIAssistantElement) super.clone(); }
}
