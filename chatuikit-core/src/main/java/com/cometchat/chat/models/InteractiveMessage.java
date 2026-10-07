package com.cometchat.chat.models;

import java.util.*;
import java.io.File;
import com.cometchat.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class InteractiveMessage extends BaseMessage {
    private JSONObject interactiveData;
    private List<String> tags;
    private InteractionGoal interactionGoal;
    private List<Interaction> interactions;
    private boolean allowSenderInteraction;
    public InteractiveMessage() {}
    public InteractiveMessage(String receiverUid, String receiverType, String type, JSONObject interactiveData) { this.receiverUid = receiverUid; this.receiverType = receiverType; this.type = type; this.interactiveData = interactiveData; }
    public JSONObject getInteractiveData() { return interactiveData; }
    public void setInteractiveData(JSONObject v) { this.interactiveData = v; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> v) { this.tags = v; }
    public InteractionGoal getInteractionGoal() { return interactionGoal; }
    public void setInteractionGoal(InteractionGoal v) { this.interactionGoal = v; }
    public List<Interaction> getInteractions() { return interactions; }
    public void setInteractions(List<Interaction> v) { this.interactions = v; }
    public boolean isAllowSenderInteraction() { return allowSenderInteraction; }
    public void setAllowSenderInteraction(boolean v) { this.allowSenderInteraction = v; }
    @Override public InteractiveMessage clone() { return (InteractiveMessage) super.clone(); }
}
