package com.cometchat.chat.core;

/** Server-side app settings the Kit once read from the cloud SDK; none exist on the hub. */
public final class SettingsRepo {
    private SettingsRepo() {}

    public static class Settings {
        public int getFileCount() { return 0; }
    }

    public static Settings getSettings() { return null; }
}
