package com.divudi.core.data;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class PvcCardLayout implements Serializable {

    public static final String SLOT_INSTITUTION_NAME = "institutionName";
    public static final String SLOT_DEPARTMENT_NAME = "departmentName";
    public static final String SLOT_NAME = "name";
    public static final String SLOT_DOB = "dob";
    public static final String SLOT_PHONE = "phone";
    public static final String SLOT_GENDER = "gender";
    public static final String SLOT_ADDRESS = "address";
    public static final String SLOT_BARCODE = "barcode";
    public static final String SLOT_PHN = "phn";

    private double widthMm;
    private double heightMm;
    private double marginMm;
    private Map<String, PvcCardSlot> slots = new LinkedHashMap<>();

    public static PvcCardLayout defaultLayout() {
        PvcCardLayout layout = new PvcCardLayout();
        layout.widthMm = 85.6;
        layout.heightMm = 54.0;
        layout.marginMm = 2.0;
        layout.slots.put(SLOT_INSTITUTION_NAME, new PvcCardSlot(true, 5, 4, 11, "#000000"));
        layout.slots.put(SLOT_DEPARTMENT_NAME, new PvcCardSlot(false, 5, 10, 9, "#000000"));
        layout.slots.put(SLOT_NAME, new PvcCardSlot(true, 5, 20, 9, "#000000"));
        layout.slots.put(SLOT_DOB, new PvcCardSlot(true, 5, 26, 8, "#000000"));
        layout.slots.put(SLOT_PHONE, new PvcCardSlot(true, 5, 32, 8, "#000000"));
        layout.slots.put(SLOT_GENDER, new PvcCardSlot(true, 5, 38, 8, "#000000"));
        layout.slots.put(SLOT_ADDRESS, new PvcCardSlot(false, 5, 44, 7, "#000000"));
        layout.slots.put(SLOT_BARCODE, PvcCardSlot.barcodeSlot(true, 5, 48, 40, 10, "code128"));
        layout.slots.put(SLOT_PHN, new PvcCardSlot(true, 5, 59, 7, "#000000"));
        return layout;
    }

    public static PvcCardLayout fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return defaultLayout();
        }
        try {
            JSONObject root = new JSONObject(json);
            PvcCardLayout defaults = defaultLayout();
            PvcCardLayout layout = new PvcCardLayout();
            layout.widthMm = root.optDouble("widthMm", defaults.widthMm);
            layout.heightMm = root.optDouble("heightMm", defaults.heightMm);
            layout.marginMm = root.optDouble("marginMm", defaults.marginMm);
            JSONObject slotsJson = root.optJSONObject("slots");
            for (Map.Entry<String, PvcCardSlot> entry : defaults.slots.entrySet()) {
                JSONObject slotJson = slotsJson == null ? null : slotsJson.optJSONObject(entry.getKey());
                layout.slots.put(entry.getKey(), slotJson == null ? entry.getValue() : PvcCardSlot.fromJson(slotJson));
            }
            return layout;
        } catch (Exception e) {
            return defaultLayout();
        }
    }

    public String toJson() {
        JSONObject root = new JSONObject();
        root.put("widthMm", widthMm);
        root.put("heightMm", heightMm);
        root.put("marginMm", marginMm);
        JSONObject slotsJson = new JSONObject();
        for (Map.Entry<String, PvcCardSlot> entry : slots.entrySet()) {
            slotsJson.put(entry.getKey(), entry.getValue().toJson());
        }
        root.put("slots", slotsJson);
        return root.toString();
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

    public double getMarginMm() {
        return marginMm;
    }

    public void setMarginMm(double marginMm) {
        this.marginMm = marginMm;
    }

    public Map<String, PvcCardSlot> getSlots() {
        return slots;
    }

    public void setSlots(Map<String, PvcCardSlot> slots) {
        this.slots = slots;
    }
}
