package com.cometchat.chat.models;

/** Base of the entities a conversation can be with (user or group). */
public abstract class AppEntity extends ModelBase {
    @Override
    public AppEntity clone() {
        return (AppEntity) super.clone();
    }
}
