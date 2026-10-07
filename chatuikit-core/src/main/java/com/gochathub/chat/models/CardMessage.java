package com.gochathub.chat.models;

import java.util.*;
import java.io.File;
import com.gochathub.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class CardMessage extends BaseMessage {
    private JSONObject card;
    private String text;
    private List<String> tags;
    public CardMessage() {}
    public JSONObject getCard() { return card; }
    public void setCard(JSONObject v) { this.card = v; }
    public String getText() { return text; }
    public void setText(String v) { this.text = v; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> v) { this.tags = v; }
    @Override public CardMessage clone() { return (CardMessage) super.clone(); }
}
