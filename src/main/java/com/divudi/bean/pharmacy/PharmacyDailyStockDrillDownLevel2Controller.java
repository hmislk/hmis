package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillSearch;
import com.divudi.bean.pharmacy.PharmacyDailyStockDrillDownLevel1Controller.TransactionType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.light.common.BillLight;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.PharmacyService;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * F15 drill-down Level 2 controller — lists the individual bills behind a single
 * Level 1 row.
 *
 * For SALES rows the L1 grouping key is BillTypeAtomic + AdmissionType + PaymentScheme;
 * for the other transaction types it is BillTypeAtomic only. Filters (date range,
 * institution, site, department) are inherited from L1.
 *
 * Parent epic: #20236. This controller: #20240.
 */
@Named
@SessionScoped
public class PharmacyDailyStockDrillDownLevel2Controller implements Serializable {

    @EJB
    private PharmacyService pharmacyService;
    @Inject
    private BillSearch billSearch;

    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Institution site;
    private Department department;

    private TransactionType transactionType;
    private BillTypeAtomic billTypeAtomic;
    private AdmissionType admissionType;
    private PaymentScheme paymentScheme;

    private List<BillLight> bills = new ArrayList<>();

    private double totalGross;
    private double totalDiscount;
    private double totalServiceCharge;
    private double totalNet;
    private BigDecimal totalRetail = BigDecimal.ZERO;
    private BigDecimal totalPurchase = BigDecimal.ZERO;
    private BigDecimal totalCost = BigDecimal.ZERO;

    public PharmacyDailyStockDrillDownLevel2Controller() {
    }

    /**
     * Entry point invoked by Level 1's row-drill action. Copies the L1 row's
     * filter context (date range, institution/site/department, transaction type
     * and the row's selection key — atomic + admissionType + paymentScheme),
     * runs the fetch, then redirects to the L2 page.
     */
    public String navigateToLevel2FromLevel1(Date fromDate, Date toDate,
                                             Institution institution, Institution site, Department department,
                                             TransactionType transactionType, BillTypeAtomic billTypeAtomic,
                                             AdmissionType admissionType, PaymentScheme paymentScheme) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.institution = institution;
        this.site = site;
        this.department = department;
        this.transactionType = transactionType;
        this.billTypeAtomic = billTypeAtomic;
        this.admissionType = admissionType;
        this.paymentScheme = paymentScheme;
        loadBills();
        return "/pharmacy/reports/summary_reports/daily_stock_values_drill_down_level2?faces-redirect=true";
    }

    private void loadBills() {
        bills = new ArrayList<>();
        resetTotals();

        if (transactionType == null || billTypeAtomic == null) {
            JsfUtil.addErrorMessage("Missing transaction type or bill type — cannot load Level 2 bill list");
            return;
        }

        Date startOfDay = CommonFunctions.getStartOfDay(fromDate);
        Date endOfDay = CommonFunctions.getEndOfDay(toDate);

        switch (transactionType) {
            case SALES:
                bills = pharmacyService.fetchPharmacyIncomeBillLightsForLevel2(
                        startOfDay, endOfDay, institution, site, department, null,
                        admissionType, paymentScheme, billTypeAtomic);
                break;
            case PURCHASES:
                bills = pharmacyService.fetchPharmacyPurchaseBillLightsForLevel2(
                        startOfDay, endOfDay, institution, site, department, null,
                        admissionType, paymentScheme, billTypeAtomic);
                break;
            case TRANSFERS:
                bills = pharmacyService.fetchPharmacyTransferBillLightsForLevel2(
                        startOfDay, endOfDay, institution, site, department, null,
                        admissionType, paymentScheme, billTypeAtomic);
                break;
            case ADJUSTMENTS:
                bills = pharmacyService.fetchPharmacyAdjustmentBillLightsForLevel2(
                        startOfDay, endOfDay, institution, site, department, null,
                        admissionType, paymentScheme, billTypeAtomic);
                break;
        }

        if (bills == null) {
            bills = new ArrayList<>();
        }

        // Sort bills oldest first by bill date (per #20240 acceptance criteria).
        bills.sort(Comparator.comparing(
                BillLight::getBillDate,
                Comparator.nullsLast(Comparator.naturalOrder())));

        computeTotals();
    }

    private void resetTotals() {
        totalGross = 0;
        totalDiscount = 0;
        totalServiceCharge = 0;
        totalNet = 0;
        totalRetail = BigDecimal.ZERO;
        totalPurchase = BigDecimal.ZERO;
        totalCost = BigDecimal.ZERO;
    }

    private void computeTotals() {
        for (BillLight b : bills) {
            if (b.getTotal() != null) {
                totalGross += b.getTotal();
            }
            if (b.getDiscount() != null) {
                totalDiscount += b.getDiscount();
            }
            if (b.getServiceCharge() != null) {
                totalServiceCharge += b.getServiceCharge();
            }
            if (b.getNetTotal() != null) {
                totalNet += b.getNetTotal();
            }
            if (b.getTotalRetailSaleValue() != null) {
                totalRetail = totalRetail.add(b.getTotalRetailSaleValue());
            }
            if (b.getTotalPurchaseValue() != null) {
                totalPurchase = totalPurchase.add(b.getTotalPurchaseValue());
            }
            if (b.getTotalCostValue() != null) {
                totalCost = totalCost.add(b.getTotalCostValue());
            }
        }
    }

    /**
     * Back-to-Level-1 navigation. Level 1 is @SessionScoped so its previously
     * selected filters persist automatically — no explicit hand-back needed.
     */
    public String navigateBackToLevel1() {
        return "/pharmacy/reports/summary_reports/daily_stock_values_drill_down_level1?faces-redirect=true";
    }

    /**
     * Open the existing per-bill view page for the given Level 2 row — issue #20241.
     * Delegates to BillSearch which loads the full Bill by id and routes to the
     * correct view page for the bill's BillTypeAtomic (refund / cancellation /
     * wholesale / GRN / transfer / etc.).
     */
    public String viewBill(BillLight bill) {
        if (bill == null || bill.getId() == null) {
            JsfUtil.addErrorMessage("Cannot open bill view — bill is missing or has no id");
            return null;
        }
        return billSearch.navigateToViewBillByAtomicBillTypeByBillId(bill.getId());
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public List<BillLight> getBills() {
        return bills;
    }

    public void setBills(List<BillLight> bills) {
        this.bills = bills;
    }

    public double getTotalGross() {
        return totalGross;
    }

    public double getTotalDiscount() {
        return totalDiscount;
    }

    public double getTotalServiceCharge() {
        return totalServiceCharge;
    }

    public double getTotalNet() {
        return totalNet;
    }

    public BigDecimal getTotalRetail() {
        return totalRetail;
    }

    public BigDecimal getTotalPurchase() {
        return totalPurchase;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }
}
