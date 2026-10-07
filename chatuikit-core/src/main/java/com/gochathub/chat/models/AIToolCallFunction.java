package com.gochathub.chat.models;

import java.util.*;
import java.io.File;
import com.gochathub.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class AIToolCallFunction extends ModelBase {
    private String name;
    private String arguments;
    public AIToolCallFunction() {}
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getArguments() { return arguments; }
    public void setArguments(String v) { this.arguments = v; }
    @Override public AIToolCallFunction clone() { return (AIToolCallFunction) super.clone(); }
}
