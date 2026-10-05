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
    /**
     * Optional page the card is placed on (e.g. A4 for the Epson disc/ID card
     * tray, which prints on an A4 canvas). 0 x 0 = no page: the page is the card.
     */
    private double pageWidthMm;
    private double pageHeightMm;
    /** Clockwise rotation of the card on the page: 0, 90, 180 or 270. */
    private int rotationDeg;
    /** Top-left of the (rotated) card on the page, in mm. */
    private double cardLeftMm;
    private double cardTopMm;
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
        layout.slots.put(SLOT_BARCODE, PvcCardSlot.barcodeSlot(true, 5, 42, 40, 8, "code128"));
        layout.slots.put(SLOT_PHN, new PvcCardSlot(true, 5, 51, 7, "#000000"));
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
            if (layout.widthMm <= 0) {
                layout.widthMm = defaults.widthMm;
            }
            layout.heightMm = root.optDouble("heightMm", defaults.heightMm);
            if (layout.heightMm <= 0) {
                layout.heightMm = defaults.heightMm;
            }
            layout.marginMm = root.optDouble("marginMm", defaults.marginMm);
            if (layout.marginMm < 0) {
                layout.marginMm = defaults.marginMm;
            }
            layout.pageWidthMm = Math.max(0, root.optDouble("pageWidthMm", 0));
            layout.pageHeightMm = Math.max(0, root.optDouble("pageHeightMm", 0));
            layout.rotationDeg = normalizeRotation(root.optInt("rotationDeg", 0));
            layout.cardLeftMm = Math.max(0, root.optDouble("cardLeftMm", 0));
            layout.cardTopMm = Math.max(0, root.optDouble("cardTopMm", 0));
            JSONObject slotsJson = root.optJSONObject("slots");
            for (Map.Entry<String, PvcCardSlot> entry : defaults.slots.entrySet()) {
                JSONObject slotJson = slotsJson == null ? null : slotsJson.optJSONObject(entry.getKey());
                layout.slots.put(entry.getKey(), slotJson == null ? entry.getValue() : PvcCardSlot.fromJson(slotJson, entry.getValue()));
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
        root.put("pageWidthMm", pageWidthMm);
        root.put("pageHeightMm", pageHeightMm);
        root.put("rotationDeg", rotationDeg);
        root.put("cardLeftMm", cardLeftMm);
        root.put("cardTopMm", cardTopMm);
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

    public double getPageWidthMm() {
        return pageWidthMm;
    }

    public void setPageWidthMm(double pageWidthMm) {
        this.pageWidthMm = pageWidthMm;
    }

    public double getPageHeightMm() {
        return pageHeightMm;
    }

    public void setPageHeightMm(double pageHeightMm) {
        this.pageHeightMm = pageHeightMm;
    }

    public int getRotationDeg() {
        return rotationDeg;
    }

    public void setRotationDeg(int rotationDeg) {
        this.rotationDeg = normalizeRotation(rotationDeg);
    }

    public double getCardLeftMm() {
        return cardLeftMm;
    }

    public void setCardLeftMm(double cardLeftMm) {
        this.cardLeftMm = cardLeftMm;
    }

    public double getCardTopMm() {
        return cardTopMm;
    }

    public void setCardTopMm(double cardTopMm) {
        this.cardTopMm = cardTopMm;
    }

    /** True when the card is printed on a larger page instead of being the page. */
    public boolean isPlacedOnPage() {
        return pageWidthMm > 0 && pageHeightMm > 0;
    }

    /** Width of the rotated card's bounding box on the page. */
    public double getPlacedWidthMm() {
        return rotationDeg == 90 || rotationDeg == 270 ? heightMm : widthMm;
    }

    /** Height of the rotated card's bounding box on the page. */
    public double getPlacedHeightMm() {
        return rotationDeg == 90 || rotationDeg == 270 ? widthMm : heightMm;
    }

    private static int normalizeRotation(int degrees) {
        int d = ((degrees % 360) + 360) % 360;
        return d == 90 || d == 180 || d == 270 ? d : 0;
    }

    public Map<String, PvcCardSlot> getSlots() {
        return slots;
    }

    public void setSlots(Map<String, PvcCardSlot> slots) {
        this.slots = slots;
    }
}
