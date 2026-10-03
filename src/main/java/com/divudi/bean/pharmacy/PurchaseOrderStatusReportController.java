package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ControllerWithReportFilters;
import com.divudi.core.data.BillType;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.data.ReportViewType;
import com.divudi.core.data.dto.PurchaseOrderStatusRowDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Pharmacy Analytics - Purchase Order Status report (#24280).
 *
 * Read-only list of pharmacy purchase orders by approval state, so staff who
 * do not hold the approver privilege can still see the approval queue.
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class PurchaseOrderStatusReportController implements Serializable, ControllerWithReportFilters {

    private static final long serialVersionUID = 1L;

    public enum PurchaseOrderStatus {
        WAITING_APPROVAL("Waiting for Approval (Finalized, Not Approved)", "Finalized"),
        APPROVED("Approved", "Approved"),
        SAVED_NOT_FINALIZED("Saved, Not Finalized", "Created");

        private final String label;
        private final String dateBasis;

        PurchaseOrderStatus(String label, String dateBasis) {
            this.label = label;
            this.dateBasis = dateBasis;
        }

        public String getLabel() {
            return label;
        }

        /**
         * Which PO date the From/To range filters on for this status.
         */
        public String getDateBasis() {
            return dateBasis;
        }
    }

    @EJB
    private BillFacade billFacade;

    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Institution site;
    private Department department;
    private AdmissionType admissionType;
    private PaymentScheme paymentScheme;
    private ReportViewType reportViewType;

    private Institution supplier;
    private PurchaseOrderStatus status = PurchaseOrderStatus.WAITING_APPROVAL;

    private List<PurchaseOrderStatusRowDTO> rows;
    private double netTotal;

    /**
     * Entry point from Pharmacy Analytics. Initialisation lives here rather
     * than in an f:viewAction because this controller is @SessionScoped.
     */
    public String navigateToPurchaseOrderStatusReport() {
        status = PurchaseOrderStatus.WAITING_APPROVAL;
        fromDate = CommonFunctions.getStartOfDay(CommonFunctions.addDaysToDate(new Date(), -30L));
        toDate = CommonFunctions.getEndOfDay();
        institution = null;
        site = null;
        department = null;
        supplier = null;
        rows = new ArrayList<>();
        netTotal = 0.0;
        return "/pharmacy/reports/procurement_reports/purchase_order_status_report?faces-redirect=true";
    }

    public void processReport() {
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select From and To dates");
            return;
        }
        if (status == null) {
            status = PurchaseOrderStatus.WAITING_APPROVAL;
        }

        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();
        jpql.append("select new com.divudi.core.data.dto.PurchaseOrderStatusRowDTO(")
                .append(" b.id, b.deptId, b.createdAt, createrPerson.name,")
                .append(" b.checkeAt, checkedByPerson.name,")
                .append(" supplier.name, dept.name, b.netTotal,")
                .append(" refBill.deptId, refBill.createdAt, approverPerson.name)")
                .append(" from BilledBill b")
                .append(" join b.toInstitution supplier")
                .append(" left join b.department dept")
                .append(" left join b.creater creater")
                .append(" left join creater.webUserPerson createrPerson")
                .append(" left join b.checkedBy checkedBy")
                .append(" left join checkedBy.webUserPerson checkedByPerson")
                .append(" left join b.referenceBill refBill")
                .append(" left join refBill.creater approver")
                .append(" left join approver.webUserPerson approverPerson")
                .append(" where b.retired=false")
                .append(" and b.cancelled=false")
                .append(" and b.billType=:bt")
                .append(" and supplier.institutionType=:insTp");
        params.put("bt", BillType.PharmacyOrder);
        params.put("insTp", InstitutionType.Dealer);

        // Filter on the join aliases, not b.checkedBy / b.referenceBill paths:
        // EclipseLink turns a LEFT JOIN into an inner join when the WHERE clause
        // re-navigates the same relationship, which drops every unapproved PO.
        String dateField;
        switch (status) {
            case APPROVED:
                jpql.append(" and refBill is not null")
                        .append(" and refBill.retired=false")
                        .append(" and refBill.cancelled=false");
                dateField = "refBill.createdAt";
                break;
            case SAVED_NOT_FINALIZED:
                jpql.append(" and checkedBy is null")
                        .append(" and refBill is null");
                dateField = "b.createdAt";
                break;
            case WAITING_APPROVAL:
            default:
                jpql.append(" and checkedBy is not null")
                        .append(" and refBill is null");
                dateField = "b.checkeAt";
                break;
        }
        jpql.append(" and ").append(dateField).append(" between :fd and :td");
        params.put("fd", fromDate);
        params.put("td", toDate);

        if (institution != null) {
            jpql.append(" and b.institution=:ins");
            params.put("ins", institution);
        }
        if (site != null) {
            jpql.append(" and dept.site=:site");
            params.put("site", site);
        }
        if (department != null) {
            jpql.append(" and dept=:dep");
            params.put("dep", department);
        }
        if (supplier != null) {
            jpql.append(" and supplier=:sup");
            params.put("sup", supplier);
        }
        jpql.append(" order by ").append(dateField).append(" desc");

        rows = (List<PurchaseOrderStatusRowDTO>) billFacade.findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
        if (rows == null) {
            rows = new ArrayList<>();
        }
        netTotal = 0.0;
        for (PurchaseOrderStatusRowDTO r : rows) {
            if (r.getNetTotal() != null) {
                netTotal += r.getNetTotal();
            }
        }
    }

    public void onStatusChange() {
        rows = new ArrayList<>();
        netTotal = 0.0;
    }

    public PurchaseOrderStatus[] getStatuses() {
        return PurchaseOrderStatus.values();
    }

    public boolean isApprovedView() {
        return status == PurchaseOrderStatus.APPROVED;
    }

    @Override
    public Date getFromDate() {
        return fromDate;
    }

    @Override
    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    @Override
    public Date getToDate() {
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

    public Institution getSupplier() {
        return supplier;
    }

    public void setSupplier(Institution supplier) {
        this.supplier = supplier;
    }

    public PurchaseOrderStatus getStatus() {
        return status;
    }

    public void setStatus(PurchaseOrderStatus status) {
        this.status = status;
    }

    public List<PurchaseOrderStatusRowDTO> getRows() {
        return rows;
    }

    public double getNetTotal() {
        return netTotal;
    }
}
