package com.gochathub.chat.models;

import java.util.*;
import java.io.File;
import com.gochathub.chat.enums.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Plain data holder; no networking (the cloud SDK is not a dependency). */
public class FlagDetail extends ModelBase {
    private String reasonId;
    private String remark;
    public FlagDetail() {}
    public FlagDetail(String reasonId, String remark) { this.reasonId = reasonId; this.remark = remark; }
    public String getReasonId() { return reasonId; }
    public void setReasonId(String v) { this.reasonId = v; }
    public String getRemark() { return remark; }
    public void setRemark(String v) { this.remark = v; }
    @Override public FlagDetail clone() { return (FlagDetail) super.clone(); }
}
