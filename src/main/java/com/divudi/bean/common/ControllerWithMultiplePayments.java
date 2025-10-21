package com.divudi.bean.common;

import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import java.util.List;

/**
 * @author Senula Nanayakkara
 */
public interface ControllerWithMultiplePayments {
    double calculatRemainForMultiplePaymentTotal();

    void recieveRemainAmountAutomatically();

    void setPaymentMethodData(PaymentMethodData paymentMethodData);

    PaymentMethodData getPaymentMethodData();

    /**
     * Checks if the given ComponentDetail is the last payment entry in the multiple payment methods list.
     * This is used to determine which payment fields should be editable in the UI.
     *
     * Uses position-based comparison (indexOf) instead of equals() because ComponentDetail
     * does not implement equals/hashCode. This ensures proper identification of the last entry
     * even when objects are deserialized or recreated during JSF lifecycle.
     *
     * @param cd The ComponentDetail to check
     * @return true if cd is the last entry in the list, false otherwise
     */
    default boolean isLastPaymentEntry(ComponentDetail cd) {
        PaymentMethodData pmd = getPaymentMethodData();
        if (cd == null ||
            pmd == null ||
            pmd.getPaymentMethodMultiple() == null ||
            pmd.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null ||
            pmd.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().isEmpty()) {
            return false;
        }

        List<ComponentDetail> details = pmd.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails();
        int lastIndex = details.size() - 1;
        int currentIndex = details.indexOf(cd);
        return currentIndex != -1 && currentIndex == lastIndex;
    }
}
