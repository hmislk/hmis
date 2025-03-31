package com.divudi.bean.common;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Patient;

public interface ControllerWithPatient {

    public Patient getPatient();

    public void setPatient(Patient patient);

    public boolean isPatientDetailsEditable();

    public void setPatientDetailsEditable(boolean patientDetailsEditable);

    public void toggalePatientEditable();

    public void setPaymentMethod(PaymentMethod paymentMethod);

    public PaymentMethod getPaymentMethod();

    public void listnerForPaymentMethodChange();
}
