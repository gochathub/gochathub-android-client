package com.cometchat.chat.core;

import java.util.*;
import com.cometchat.chat.models.*;
import com.cometchat.chat.enums.*;

/** Query parameters; the hub datasources translate them into REST calls. */
public class ReactionsRequest {
    /** Not supported: the Kit's datasources fetch over REST. */
    public void fetchNext(CometChat.CallbackListener<List<Reaction>> l) {
        l.onError(new com.cometchat.chat.exceptions.CometChatException("hub_unsupported", "fetchNext is not available"));
    }
    private String affix;
    private int limit;
    private long messageId;
    private String reaction;
    private boolean inProgress;
    private String previousReactionId;
    private String nextReactionId;
    private boolean hasPrevious;
    public int getLimit() { return limit; }
    public long getMessageId() { return messageId; }
    public String getReaction() { return reaction; }
    public static class ReactionsRequestBuilder {
        private final ReactionsRequest r = new ReactionsRequest();
        public ReactionsRequestBuilder setLimit(int v) { r.limit = v; return this; }
        public ReactionsRequestBuilder setMessageId(long v) { r.messageId = v; return this; }
        public ReactionsRequestBuilder setReaction(String v) { r.reaction = v; return this; }
        public ReactionsRequest build() { return r; }
    }
}
