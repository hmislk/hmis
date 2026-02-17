package com.divudi.bean.common;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Patient;

public interface ControllerWithPatient {

    Patient getPatient();

    void setPatient(Patient patient);

    boolean isPatientDetailsEditable();

    void setPatientDetailsEditable(boolean patientDetailsEditable);

    void toggalePatientEditable();

    void setPaymentMethod(PaymentMethod paymentMethod);

    PaymentMethod getPaymentMethod();

    void listnerForPaymentMethodChange();
}
