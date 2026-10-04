package com.divudi.core.data;

import org.json.JSONObject;

import java.io.Serializable;

public class PvcCardSlot implements Serializable {

    private boolean visible;
    private double xMm;
    private double yMm;
    private double fontSizePt = 8.0;
    private String fontColor = "#000000";
    private double widthMm;
    private double heightMm;
    private String type = "code128";

    public PvcCardSlot() {
    }

    public PvcCardSlot(boolean visible, double xMm, double yMm, double fontSizePt, String fontColor) {
        this.visible = visible;
        this.xMm = xMm;
        this.yMm = yMm;
        this.fontSizePt = fontSizePt;
        this.fontColor = fontColor;
    }

    public static PvcCardSlot barcodeSlot(boolean visible, double xMm, double yMm, double widthMm, double heightMm, String type) {
        PvcCardSlot slot = new PvcCardSlot();
        slot.visible = visible;
        slot.xMm = xMm;
        slot.yMm = yMm;
        slot.widthMm = widthMm;
        slot.heightMm = heightMm;
        slot.type = type;
        return slot;
    }

    public static PvcCardSlot fromJson(JSONObject json) {
        PvcCardSlot slot = new PvcCardSlot();
        slot.visible = json.optBoolean("visible", false);
        slot.xMm = json.optDouble("xMm", 0.0);
        slot.yMm = json.optDouble("yMm", 0.0);
        slot.fontSizePt = json.optDouble("fontSizePt", 8.0);
        slot.fontColor = json.optString("fontColor", "#000000");
        slot.widthMm = json.optDouble("widthMm", 0.0);
        slot.heightMm = json.optDouble("heightMm", 0.0);
        slot.type = json.optString("type", "code128");
        return slot;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("visible", visible);
        json.put("xMm", xMm);
        json.put("yMm", yMm);
        json.put("fontSizePt", fontSizePt);
        json.put("fontColor", fontColor);
        json.put("widthMm", widthMm);
        json.put("heightMm", heightMm);
        json.put("type", type);
        return json;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public double getXMm() {
        return xMm;
    }

    public void setXMm(double xMm) {
        this.xMm = xMm;
    }

    public double getYMm() {
        return yMm;
    }

    public void setYMm(double yMm) {
        this.yMm = yMm;
    }

    public double getFontSizePt() {
        return fontSizePt;
    }

    public void setFontSizePt(double fontSizePt) {
        this.fontSizePt = fontSizePt;
    }

    public String getFontColor() {
        return fontColor;
    }

    public void setFontColor(String fontColor) {
        this.fontColor = fontColor;
    }

    public double getWidthMm() {
        return widthMm;
    }

    public void setWidthMm(double widthMm) {
        this.widthMm = widthMm;
    }

    public double getHeightMm() {
        return heightMm;
    }

    public void setHeightMm(double heightMm) {
        this.heightMm = heightMm;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
