package com.divudi.bean.common;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.data.ReportViewType;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.AuditService;
import java.io.Serializable;
import java.util.*;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

@Named
@SessionScoped
public class BillDeptIdEditorController implements Serializable, ControllerWithReportFilters {

    private static final long serialVersionUID = 1L;

    @EJB
    private BillFacade billFacade;

    @EJB
    private AuditService auditService;

    @Inject
    private SessionController sessionController;

    private List<Bill> bills;
    private List<ReportViewType> reportViewTypes;

    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Institution site;
    private Department department;
    private AdmissionType admissionType;
    private PaymentScheme paymentScheme;
    private ReportViewType reportViewType;

    public String navigateToBillDeptIdEditor() {
        bills = null;
        return "/dataAdmin/bill_deptid_editor?faces-redirect=true";
    }

    public void listBills() {
        bills = null;
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder("select b from Bill b where b.retired=false");

        if (fromDate != null && toDate != null) {
            jpql.append(" and b.createdAt between :fd and :td");
            params.put("fd", fromDate);
            params.put("td", toDate);
        }
        if (institution != null) {
            jpql.append(" and b.department.institution=:ins");
            params.put("ins", institution);
        }
        if (site != null) {
            jpql.append(" and b.department.site=:site");
            params.put("site", site);
        }
        if (department != null) {
            jpql.append(" and b.department=:dept");
            params.put("dept", department);
        }

        jpql.append(" order by b.id");

        bills = billFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
    }

    public void saveDeptIds() {
        if (bills == null || bills.isEmpty()) {
            JsfUtil.addErrorMessage("No bills to update");
            return;
        }
        for (Bill b : bills) {
            if (b == null || b.getId() == null) {
                continue;
            }
            Bill original = billFacade.find(b.getId());
            if (original == null) {
                continue;
            }
            String beforeDeptId = original.getDeptId();
            String newDeptId = b.getDeptId();

            if (Objects.equals(beforeDeptId, newDeptId)) {
                // No change in department id; skip persisting and auditing
                continue;
            }

            original.setDeptId(newDeptId);
            billFacade.edit(original);

            Map<String, String> before = new HashMap<>();
            before.put("billId", String.valueOf(original.getId()));
            before.put("deptId", beforeDeptId);

            Map<String, String> after = new HashMap<>();
            after.put("billId", String.valueOf(original.getId()));
            after.put("deptId", original.getDeptId());

            auditService.logAudit(before, after, sessionController.getLoggedUser(), Bill.class.getSimpleName(), "Update Bill DeptId");
        }
        JsfUtil.addSuccessMessage("Bill DeptIds updated");
    }

    // Getters and Setters
    @Override
    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay();
        }
        return fromDate;
    }

    @Override
    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    @Override
    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        return toDate;
    }

    @Override
    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    @Override
    public Institution getInstitution() {
        return institution;
    }

    @Override
    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    @Override
    public Institution getSite() {
        return site;
    }

    @Override
    public void setSite(Institution site) {
        this.site = site;
    }

    @Override
    public Department getDepartment() {
        return department;
    }

    @Override
    public void setDepartment(Department department) {
        this.department = department;
    }

    @Override
    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    @Override
    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    @Override
    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    @Override
    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    @Override
    public ReportViewType getReportViewType() {
        return reportViewType;
    }

    @Override
    public void setReportViewType(ReportViewType reportViewType) {
        this.reportViewType = reportViewType;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public List<ReportViewType> getReportViewTypes() {
        reportViewTypes = new ArrayList<>(Arrays.asList(ReportViewType.values()));
        return reportViewTypes;
    }

}
