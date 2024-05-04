package com.divudi.data;

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
    OPD_BILL_CANCELLATION("Bill Cancellation - System Notification", NotificationMedium.SYSTEM_NOTIFICATION, TriggerTypeParent.OPD_BILL_CANCELLATION),
    OPD_BILL_CANCELLATION_SMS("Bill Cancellation - SMS", NotificationMedium.SMS, TriggerTypeParent.OPD_BILL_CANCELLATION),
    OPD_BILL_CANCELLATION_EMAIL("Bill Cancellation - Email", NotificationMedium.EMAIL, TriggerTypeParent.OPD_BILL_CANCELLATION),;

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
