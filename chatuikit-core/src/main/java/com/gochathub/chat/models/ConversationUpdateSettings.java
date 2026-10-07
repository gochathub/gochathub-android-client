package com.gochathub.chat.models;

/** Which message kinds bump a conversation in the list. */
public class ConversationUpdateSettings extends ModelBase {
    private boolean callActivities = true;
    private boolean groupActions = true;
    private boolean customMessages = true;
    private boolean messageReplies = true;

    public boolean shouldUpdateOnCallActivities() { return callActivities; }
    public boolean shouldUpdateOnGroupActions() { return groupActions; }
    public boolean shouldUpdateOnCustomMessages() { return customMessages; }
    public boolean shouldUpdateOnMessageReplies() { return messageReplies; }
}
