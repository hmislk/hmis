package com.divudi.core.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Enum for defining various icons with human-readable labels. Deprecated enums
 * are marked and maintained for compatibility with existing database entries.
 * New enums adhere to proper spellings and capitalization. Note: Image and
 * action paths are removed as per request.
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
public enum TriggerType {

    @Deprecated
    Order_Request("Order Request - System Notification", NotificationMedium.SYSTEM_NOTIFICATION, TriggerTypeParent.INPATIENT_ORDER_REQUEST), // Deprecated
    @Deprecated
    Order_Request_Sms("Order Request - SMS", NotificationMedium.SMS, TriggerTypeParent.INPATIENT_ORDER_REQUEST), // Deprecated
    @Deprecated
    Order_Request_Email("Order Request - Email", NotificationMedium.EMAIL, TriggerTypeParent.INPATIENT_ORDER_REQUEST), // Deprecated
    @Deprecated
    Transfer_Issue("Transfer Issue", NotificationMedium.SYSTEM_NOTIFICATION, TriggerTypeParent.TRANSFER_ISSUE), // Deprecated
    PURCHASE_ORDER_REQUEST("Purchase Order Request - System Notification", NotificationMedium.SYSTEM_NOTIFICATION, TriggerTypeParent.PURCHASE_ORDER_REQUEST),
    PURCHASE_ORDER_REQUEST_SMS("Purchase Order Request - SMS", NotificationMedium.SMS, TriggerTypeParent.PURCHASE_ORDER_REQUEST),
    PURCHASE_ORDER_REQUEST_EMAIL("Purchase Order Request - Email", NotificationMedium.EMAIL, TriggerTypeParent.PURCHASE_ORDER_REQUEST),
    INPATIENT_ORDER_REQUEST("Inpatient Order Request - System Notification", NotificationMedium.SYSTEM_NOTIFICATION, TriggerTypeParent.INPATIENT_ORDER_REQUEST),
    INPATIENT_ORDER_REQUEST_SMS("Inpatient Order Request - SMS", NotificationMedium.SMS, TriggerTypeParent.INPATIENT_ORDER_REQUEST),
    INPATIENT_ORDER_REQUEST_EMAIL("Inpatient Order Request - Email", NotificationMedium.EMAIL, TriggerTypeParent.INPATIENT_ORDER_REQUEST),
    TRANSFER_REQUEST("Transfer Request - System Notification", NotificationMedium.SYSTEM_NOTIFICATION, TriggerTypeParent.TRANSFER_REQUEST),
    TRANSFER_REQUEST_SMS("Transfer Request - SMS", NotificationMedium.SMS, TriggerTypeParent.TRANSFER_REQUEST),
    TRANSFER_REQUEST_EMAIL("Transfer Request - Email", NotificationMedium.EMAIL, TriggerTypeParent.TRANSFER_REQUEST),
    TRANSFER_ISSUE("Transfer Issue - System Notification", NotificationMedium.SYSTEM_NOTIFICATION, TriggerTypeParent.TRANSFER_ISSUE),
    TRANSFER_ISSUE_SMS("Transfer Issue - SMS", NotificationMedium.SMS, TriggerTypeParent.TRANSFER_ISSUE),
    TRANSFER_ISSUE_EMAIL("Transfer Issue - Email", NotificationMedium.EMAIL, TriggerTypeParent.TRANSFER_ISSUE),
    DIRECT_ISSUE("Direct Issue - System Notification", NotificationMedium.SYSTEM_NOTIFICATION, TriggerTypeParent.TRANSFER_ISSUE),
    DIRECT_ISSUE_SMS("Direct Issue - SMS", NotificationMedium.SMS, TriggerTypeParent.TRANSFER_ISSUE),
    DIRECT_ISSUE_EMAIL("Direct Issue - Email", NotificationMedium.EMAIL, TriggerTypeParent.TRANSFER_ISSUE),
    PURCHASE_ORDER_APPROVAL("Purchase Order Approval - System Notification", NotificationMedium.SYSTEM_NOTIFICATION, TriggerTypeParent.PURCHASE_ORDER_APPROVAL),
    PURCHASE_ORDER_APPROVAL_SMS("Purchase Order Approval - SMS", NotificationMedium.SMS, TriggerTypeParent.PURCHASE_ORDER_APPROVAL),
    PURCHASE_ORDER_APPROVAL_EMAIL("Purchase Order Approval - Email", NotificationMedium.EMAIL, TriggerTypeParent.PURCHASE_ORDER_APPROVAL),
    // Final/physical discharge — patient leaves the hospital; typically notifies the Guest Relations Officer.
    // Kept in its ORIGINAL ordinal position: this enum is persisted via @Enumerated(ORDINAL) on
    // Notification.triggerType and TriggerSubscription.triggerType, so existing constants must never be
    // reordered. The clinical and room discharge stages are appended at the END of the enum instead.
    INWARD_PATIENT_DISCHARGED("Inward Patient Final Discharge - System Notification", NotificationMedium.SYSTEM_NOTIFICATION, TriggerTypeParent.INWARD_PATIENT_DISCHARGED),
    INWARD_PATIENT_DISCHARGED_SMS("Inward Patient Final Discharge - SMS", NotificationMedium.SMS, TriggerTypeParent.INWARD_PATIENT_DISCHARGED),
    INWARD_PATIENT_DISCHARGED_EMAIL("Inward Patient Final Discharge - Email", NotificationMedium.EMAIL, TriggerTypeParent.INWARD_PATIENT_DISCHARGED),
    OPD_BILL_CANCELLATION("Bill Cancellation - System Notification", NotificationMedium.SYSTEM_NOTIFICATION, TriggerTypeParent.OPD_BILL_CANCELLATION),
    OPD_BILL_CANCELLATION_SMS("Bill Cancellation - SMS", NotificationMedium.SMS, TriggerTypeParent.OPD_BILL_CANCELLATION),
    OPD_BILL_CANCELLATION_EMAIL("Bill Cancellation - Email", NotificationMedium.EMAIL, TriggerTypeParent.OPD_BILL_CANCELLATION),
    FLOAT_TRANSFER_REQUEST("Float Transfer Request - System Notification", NotificationMedium.SYSTEM_NOTIFICATION, TriggerTypeParent.FLOAT_TRANSFER_REQUEST),
    FLOAT_TRANSFER_REQUEST_SMS("Float Transfer Request - SMS", NotificationMedium.SMS, TriggerTypeParent.FLOAT_TRANSFER_REQUEST),
    FLOAT_TRANSFER_REQUEST_EMAIL("Float Transfer Request - Email", NotificationMedium.EMAIL, TriggerTypeParent.FLOAT_TRANSFER_REQUEST),
    // ==== Appended at the end to preserve persisted ORDINAL values of all constants above ====
    // Inward discharge — stage 1: clinical discharge (clinician declares the patient medically fit;
    // typically notifies ward nursing).
    INWARD_PATIENT_CLINICAL_DISCHARGED("Inward Patient Clinical Discharge - System Notification", NotificationMedium.SYSTEM_NOTIFICATION, TriggerTypeParent.INWARD_PATIENT_CLINICAL_DISCHARGED),
    INWARD_PATIENT_CLINICAL_DISCHARGED_SMS("Inward Patient Clinical Discharge - SMS", NotificationMedium.SMS, TriggerTypeParent.INWARD_PATIENT_CLINICAL_DISCHARGED),
    INWARD_PATIENT_CLINICAL_DISCHARGED_EMAIL("Inward Patient Clinical Discharge - Email", NotificationMedium.EMAIL, TriggerTypeParent.INWARD_PATIENT_CLINICAL_DISCHARGED),
    // Inward discharge — stage 2: room/bed discharge (bed released; typically notifies billing).
    INWARD_PATIENT_ROOM_DISCHARGED("Inward Patient Room Discharge - System Notification", NotificationMedium.SYSTEM_NOTIFICATION, TriggerTypeParent.INWARD_PATIENT_ROOM_DISCHARGED),
    INWARD_PATIENT_ROOM_DISCHARGED_SMS("Inward Patient Room Discharge - SMS", NotificationMedium.SMS, TriggerTypeParent.INWARD_PATIENT_ROOM_DISCHARGED),
    INWARD_PATIENT_ROOM_DISCHARGED_EMAIL("Inward Patient Room Discharge - Email", NotificationMedium.EMAIL, TriggerTypeParent.INWARD_PATIENT_ROOM_DISCHARGED),
    // Inward room — patient admitted/added to a room
    INWARD_PATIENT_ROOM_ADDED("Inward Patient Room Added - System Notification", NotificationMedium.SYSTEM_NOTIFICATION, TriggerTypeParent.INWARD_PATIENT_ROOM_ADDED),
    INWARD_PATIENT_ROOM_ADDED_SMS("Inward Patient Room Added - SMS", NotificationMedium.SMS, TriggerTypeParent.INWARD_PATIENT_ROOM_ADDED),
    INWARD_PATIENT_ROOM_ADDED_EMAIL("Inward Patient Room Added - Email", NotificationMedium.EMAIL, TriggerTypeParent.INWARD_PATIENT_ROOM_ADDED),
    // Inward room — patient changed/moved to a different room
    INWARD_PATIENT_ROOM_CHANGED("Inward Patient Room Changed - System Notification", NotificationMedium.SYSTEM_NOTIFICATION, TriggerTypeParent.INWARD_PATIENT_ROOM_CHANGED),
    INWARD_PATIENT_ROOM_CHANGED_SMS("Inward Patient Room Changed - SMS", NotificationMedium.SMS, TriggerTypeParent.INWARD_PATIENT_ROOM_CHANGED),
    INWARD_PATIENT_ROOM_CHANGED_EMAIL("Inward Patient Room Changed - Email", NotificationMedium.EMAIL, TriggerTypeParent.INWARD_PATIENT_ROOM_CHANGED);

    private final String label;
    private final NotificationMedium medium;
    private final TriggerTypeParent parent;

    TriggerType(String label, NotificationMedium medium, TriggerTypeParent parent) {
        this.label = label;
        this.medium = medium;
        this.parent = parent;
    }

    public String getLabel() {
        return label;
    }

    public NotificationMedium getMedium() {
        return medium;
    }

    public TriggerTypeParent getParent() {
        return parent;
    }

    public static ArrayList<TriggerType> getTriggersByMedium(NotificationMedium medium) {
        return Arrays.stream(TriggerType.values())
                .filter(trigger -> trigger.getMedium() == medium)
                .sorted(Comparator.comparing(TriggerType::name))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static ArrayList<TriggerType> getTriggersByParent(TriggerTypeParent parent) {
        return Arrays.stream(TriggerType.values())
                .filter(trigger -> trigger.getParent() == parent)
                .sorted(Comparator.comparing(TriggerType::name))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static TriggerType[] getAlphabeticallySortedValuesArray() {
        return Arrays.stream(TriggerType.values())
                .sorted(Comparator.comparing(TriggerType::name))
                .toArray(TriggerType[]::new);
    }

    public static ArrayList<TriggerType> getAlphabeticallySortedValues() {
        return Arrays.stream(TriggerType.values())
                .sorted(Comparator.comparing(TriggerType::name))
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
