package com.gochathub.chat.models;

import java.util.*;
import java.io.File;
import com.gochathub.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class Attachment extends ModelBase {
    private String fileName;
    private String fileExtension;
    private int fileSize;
    private String fileMimeType;
    private String fileUrl;
    private JSONObject metadata;
    public Attachment() {}
    public String getFileName() { return fileName; }
    public void setFileName(String v) { this.fileName = v; }
    public String getFileExtension() { return fileExtension; }
    public void setFileExtension(String v) { this.fileExtension = v; }
    public int getFileSize() { return fileSize; }
    public void setFileSize(int v) { this.fileSize = v; }
    public String getFileMimeType() { return fileMimeType; }
    public void setFileMimeType(String v) { this.fileMimeType = v; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String v) { this.fileUrl = v; }
    public JSONObject getMetadata() { return metadata; }
    public void setMetadata(JSONObject v) { this.metadata = v; }
    @Override public Attachment clone() { return (Attachment) super.clone(); }
}
