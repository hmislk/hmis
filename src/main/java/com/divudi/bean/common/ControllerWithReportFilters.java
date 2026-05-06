package com.divudi.bean.common;

import com.divudi.core.data.ReportViewType;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.inward.AdmissionType;
import java.util.Date;

/**
 * This interface defines standard report filters expected in report controllers
 * using JSF filtering components. Implementing controllers can use these
 * standard getters/setters to simplify filtering logic and reuse.
 *
 * @author ChatGPT
 */
public interface ControllerWithReportFilters {

    public Date getFromDate();

    public void setFromDate(Date fromDate);

    public Date getToDate();

    public void setToDate(Date toDate);

    public Institution getInstitution();

    public void setInstitution(Institution institution);

    public Institution getSite();

    public void setSite(Institution site);

    public Department getDepartment();

    public void setDepartment(Department department);

    public AdmissionType getAdmissionType();

    public void setAdmissionType(AdmissionType admissionType);

    public PaymentScheme getPaymentScheme();

    public void setPaymentScheme(PaymentScheme paymentScheme);

    public ReportViewType getReportViewType();

    public void setReportViewType(ReportViewType reportViewType);

}
