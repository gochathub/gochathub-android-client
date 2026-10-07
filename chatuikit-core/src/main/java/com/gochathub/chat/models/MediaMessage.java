package com.gochathub.chat.models;

import java.util.*;
import java.io.File;
import com.gochathub.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class MediaMessage extends BaseMessage {
    private File file;
    private List<File> files;
    private String caption;
    private Attachment attachment;
    private List<Attachment> attachments;
    private List<String> tags;
    private ModerationStatus moderationStatus;
    public MediaMessage() {}
    public MediaMessage(String receiverUid, String type, String receiverType) { this.receiverUid = receiverUid; this.type = type; this.receiverType = receiverType; }
    public File getFile() { return file; }
    public void setFile(File v) { this.file = v; }
    public List<File> getFiles() { return files; }
    public void setFiles(List<File> v) { this.files = v; }
    public String getCaption() { return caption; }
    public void setCaption(String v) { this.caption = v; }
    public Attachment getAttachment() { return attachment; }
    public void setAttachment(Attachment v) { this.attachment = v; }
    public List<Attachment> getAttachments() { return attachments; }
    public void setAttachments(List<Attachment> v) { this.attachments = v; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> v) { this.tags = v; }
    public ModerationStatus getModerationStatus() { return moderationStatus; }
    public void setModerationStatus(ModerationStatus v) { this.moderationStatus = v; }
    @Override public MediaMessage clone() { return (MediaMessage) super.clone(); }
    public MediaMessage(String receiverUid, File file, String type, String receiverType) {
        this.receiverUid = receiverUid; this.file = file; this.type = type; this.receiverType = receiverType;
    }
    public MediaMessage(String receiverUid, List<File> files, String type, String receiverType) {
        this.receiverUid = receiverUid; this.files = files; this.type = type; this.receiverType = receiverType;
    }
}
