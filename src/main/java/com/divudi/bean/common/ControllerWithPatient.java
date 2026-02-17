package com.divudi.bean.common;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Patient;
import java.util.Map;

public interface ControllerWithPatient {

    Patient getPatient();

    void setPatient(Patient patient);

    boolean isPatientDetailsEditable();

    void setPatientDetailsEditable(boolean patientDetailsEditable);

    void toggalePatientEditable();

    void setPaymentMethod(PaymentMethod paymentMethod);

    PaymentMethod getPaymentMethod();

    void listnerForPaymentMethodChange();
    
    void setInitialPatient(Map<String, Object> p);
    
    Map<String, Object> getInitialPatient();
}
