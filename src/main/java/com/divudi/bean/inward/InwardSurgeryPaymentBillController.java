package com.divudi.bean.inward;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;

import com.divudi.core.data.BillType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.SearchKeyword;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillComponent;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillFeePayment;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.cashTransaction.Drawer;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.BillComponentFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillFeePaymentFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.CancelledBillFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.RefundBillFacade;
import com.divudi.core.facade.StaffFacade;
import com.divudi.service.DrawerService;
import com.divudi.service.ProfessionalPaymentService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Controller for handling surgery payments for inward surgeons
 *
 * @author Development Team
 */
@Named
@SessionScoped
public class InwardSurgeryPaymentBillController implements Serializable {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private RefundBillFacade refundBillFacade;
    @EJB
    private CancelledBillFacade cancelledBillFacade;
    @EJB
    private BillComponentFacade billComponentFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    ProfessionalPaymentService professionalPaymentService;
    @EJB
    DrawerService drawerService;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    BillNumberGenerator billNumberBean;
    @EJB
    StaffFacade staffFacade;
    @EJB
    private CashTransactionBean cashTransactionBean;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    SessionController sessionController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    DrawerController drawerController;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    private static final long serialVersionUID = 1L;
    private Date fromDate;
    private Date toDate;

    private Bill current;
    private List<Bill> items = null;

    private Staff currentSurgeon;
    private List<BillFee> dueSurgeryFees;
    private List<BillFee> payingSurgeryFees;
    private boolean allowUserToSelectPayWithholdingTaxDuringSurgeryPayments;
    private String withholdingTaxCalculationStatus;
    private List<String> withholdingTaxCalculationStatuses;
    private double withholdingTax;
    private double totalPaidForCurrentSurgeonForCurrentMonthForCurrentInstitute;
    private Double withholdingTaxLimit;
    private Double withholdingTaxPercentage;
    private double totalDue;
    private double totalPaying;
    private double totalPayingWithoutWht;

    private Boolean printPreview = false;
    private PaymentMethod paymentMethod;
    
    private SearchKeyword searchKeyword;
    private AdmissionType admissionType;
    private Institution institution;
    private boolean feeCollectedByDoctor = false; // Flag to mark fee as collected by surgeon
    private BillItem surgery; // Selected surgery for filtering

    private List<BillComponent> billComponents;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Navigation Methods">
    public String navigateToInwardSurgeryPayment() {
        specialization = null;
        currentSurgeon = null;
        paymentMethod = null;
        dueSurgeryFees = null;
        totalDue = 0.0;
        totalPaying = 0.0;
        printPreview = false;
        feeCollectedByDoctor = false;
        return "/inward/inward_bill_surgery_payment?faces-redirect=true";
    }

    public String navigateToViewInwardSurgeryPayments() {
        recreateModel();
        fetchWithholdingDetailConfiguration();
        return "/inward/inward_bill_surgery_payment?faces-redirect=true";
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Main Methods">
    public void calculateDueFeesForSurgeriesForSelectedPeriod() {
        List<BillTypeAtomic> btcs = new ArrayList<>();
        btcs.add(BillTypeAtomic.INWARD_THEATRE_PROFESSIONAL_FEE_BILL);

        String sql;
        Map<String, Object> h = new HashMap<>();
        sql = "select bf from BillFee bf where "
                + " bf.retired=false "
                + " and bf.bill.billTypeAtomic in :btcs "
                + " and bf.bill.cancelled=false "
                + " and bf.bill.createdAt between :fd and :td "
                + " and (bf.feeValue - bf.paidValue) > 0 "
                + " and bf.staff=:stf ";
        
        sql += " order by bf.createdAt desc";

        h.put("fd", fromDate);
        h.put("td", toDate);
        h.put("stf", currentSurgeon);
        h.put("btcs", btcs);
        dueSurgeryFees = getBillFeeFacade().findByJpql(sql, h, TemporalType.TIMESTAMP);
        
        List<BillFee> removeingBillFees = new ArrayList<>();
        for (BillFee bf : dueSurgeryFees) {
            h = new HashMap<>();
            h.put("btp", BillType.InwardBill);
            sql = "SELECT bi FROM BillItem bi where bi.retired=false "
                    + " and bi.bill.cancelled=false "
                    + " and bi.bill.billType=:btp "
                    + " and bi.referanceBillItem.id= " + bf.getBillItem().getId();
            BillItem rbi = getBillItemFacade().findFirstByJpql(sql, h);

            if (rbi != null) {
                removeingBillFees.add(bf);
            }
        }
        dueSurgeryFees.removeAll(removeingBillFees);

        calculateTotalPaymentsForTheSurgeonForCurrentMonthForCurrentInstitution();
        performCalculations();
    }

    private void calculateTotalPaymentsForTheSurgeonForCurrentMonthForCurrentInstitution() {
        if (currentSurgeon == null) {
            return;
        }
        totalPaidForCurrentSurgeonForCurrentMonthForCurrentInstitute
                = professionalPaymentService.findSumOfProfessionalPaymentsDone(sessionController.getInstitution(), currentSurgeon);
    }

    public void performCalculations() {
        calculateTotalDue();
        calculatePaymentsSelected();

        switch (getWithholdingTaxCalculationStatus()) {
            case "Depending On Payments":
                calculateWithholdingTaxDependingOnPayments();
                break;
            case "Include Withholding Tax":
                calculateWithWithholdingTax();
                break;
            case "Exclude Withholding Tax":
                calculateWithoutWithholdingTax();
                break;
            default:
                calculateWithholdingTaxDependingOnPayments();
        }
    }

    private void calculateTotalDue() {
        totalDue = 0.0;
        if (dueSurgeryFees == null) {
            return;
        }
        for (BillFee f : dueSurgeryFees) {
            totalDue += (f.getFeeValue() - f.getPaidValue());
        }
    }

    private void calculatePaymentsSelected() {
        totalPaying = 0;
        if (payingSurgeryFees == null) {
            return;
        }
        for (BillFee f : payingSurgeryFees) {
            totalPaying = totalPaying + (f.getFeeValue() - f.getPaidValue());
        }
    }

    private void calculateWithholdingTaxDependingOnPayments() {
        if (totalPaidForCurrentSurgeonForCurrentMonthForCurrentInstitute == 0.0) {
            withholdingTax = 0.0;
            totalPayingWithoutWht = totalPaying;
            return;
        }
        Double paidValue = Math.abs(totalPaidForCurrentSurgeonForCurrentMonthForCurrentInstitute);
        if (getWithholdingTaxLimit() < paidValue) {
            withholdingTax = totalPaying * (getWithholdingTaxPercentage() / 100);
        } else {
            withholdingTax = 0.0;
        }
        totalPayingWithoutWht = totalPaying - withholdingTax;
    }

    private void calculateWithWithholdingTax() {
        withholdingTax = totalPaying * (getWithholdingTaxPercentage() / 100);
        totalPayingWithoutWht = totalPaying - withholdingTax;
    }

    private void calculateWithoutWithholdingTax() {
        withholdingTax = 0.0;
        totalPayingWithoutWht = totalPaying - withholdingTax;
    }

    public void settle() {
        System.out.println("totalsettle = " + totalPaying);
        System.out.println("this = " + getPayingSurgeryFees().size());
        
        if (errorCheck()) {
            return;
        }
        
        if (paymentMethod == PaymentMethod.Cash && !feeCollectedByDoctor) {
            Drawer userDrawer = drawerService.getUsersDrawer(sessionController.getLoggedUser());
            if (userDrawer != null) {
                double drawerBalance = userDrawer.getCashInHandValue();
                double paymentAmount = getTotalPayingWithoutWht();

                if (configOptionApplicationController.getBooleanValueByKey("Enable Drawer Manegment", true)) {
                    if (drawerBalance < paymentAmount) {
                        JsfUtil.addErrorMessage("Not enough cash in your drawer to make this payment");
                        return;
                    }
                }
            }
        }
        
        performCalculations();
        Bill newlyCreatedPaymentBill = createPaymentBill();
        current = newlyCreatedPaymentBill;
        getBillFacade().create(newlyCreatedPaymentBill);
        Payment newlyCreatedPayment = createPayment(newlyCreatedPaymentBill, paymentMethod);
        
        if (!feeCollectedByDoctor) {
            drawerController.updateDrawerForOuts(newlyCreatedPayment);
        }
        
        saveBillCompo(newlyCreatedPaymentBill, newlyCreatedPayment);
        printPreview = true;
        JsfUtil.addSuccessMessage("Surgery Payment Successfully Processed");
    }

    public void settleWithoutPayment() {
        System.out.println("totalsettle (without payment) = " + totalPaying);
        System.out.println("this = " + getPayingSurgeryFees().size());
        
        if (errorCheck()) {
            return;
        }
        
        performCalculations();
        
        // Update bill fees without creating payment records
        for (BillFee originalBillFee : getPayingSurgeryFees()) {
            originalBillFee.setPaidValue(originalBillFee.getFeeValue());
            originalBillFee.setSettleValue(originalBillFee.getFeeValue());
            
            // Mark as collected by doctor if flag is set
            if (feeCollectedByDoctor) {
                originalBillFee.setFeeCollectedByDoctor(true);
            }
            
            getBillFeeFacade().edit(originalBillFee);
        }
        
        printPreview = true;
        JsfUtil.addSuccessMessage("Surgery Fees Successfully Settled (No Payment Record Created)");
    }

    private boolean errorCheck() {
        if (currentSurgeon == null) {
            JsfUtil.addErrorMessage("Please select a Surgeon");
            return true;
        }
        if (dueSurgeryFees == null) {
            JsfUtil.addErrorMessage("Please select surgeries to pay");
            return true;
        }
        if (totalPaying == 0) {
            JsfUtil.addErrorMessage("Please select surgeries to pay");
            return true;
        }
        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Please select a payment method");
            return true;
        }

        return false;
    }

    private Bill createPaymentBill() {
        BilledBill tmp = new BilledBill();
        tmp.setBillDate(Calendar.getInstance().getTime());
        tmp.setBillTime(Calendar.getInstance().getTime());
        tmp.setBillType(BillType.PaymentBill);
        tmp.setBillTypeAtomic(BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_INWARD_SERVICE);
        tmp.setCreatedAt(Calendar.getInstance().getTime());
        tmp.setCreater(getSessionController().getLoggedUser());
        tmp.setDepartment(getSessionController().getLoggedUser().getDepartment());

        tmp.setDeptId(getBillNumberBean().departmentBillNumberGeneratorYearly(getSessionController().getDepartment(), BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_INWARD_SERVICE));
        tmp.setInsId(getBillNumberBean().departmentBillNumberGeneratorYearly(getSessionController().getDepartment(), BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_INWARD_SERVICE));

        tmp.setDiscount(0.0);
        tmp.setDiscountPercent(0.0);

        tmp.setInstitution(getSessionController().getLoggedUser().getInstitution());
        tmp.setNetTotal(0 - totalPayingWithoutWht);
        tmp.setPaymentMethod(paymentMethod);
        tmp.setStaff(currentSurgeon);
        tmp.setToStaff(currentSurgeon);
        tmp.setTotal(0 - totalPaying);
        tmp.setTax(withholdingTax);

        return tmp;
    }

    public Payment createPayment(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        setPaymentMethodData(p, pm);
        return p;
    }

    public void setPaymentMethodData(Payment p, PaymentMethod pm) {
        p.setInstitution(getSessionController().getInstitution());
        p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(getSessionController().getLoggedUser());
        p.setPaymentMethod(pm);
        p.setPaidValue(p.getBill().getNetTotal());

        if (p.getId() == null) {
            getPaymentFacade().create(p);
        }
    }

    private void saveBillCompo(Bill paymentBill, Payment paymentBillPayment) {
        for (BillFee originalBillFee : getPayingSurgeryFees()) {
            saveBillItemForPaymentBill(paymentBill, originalBillFee, paymentBillPayment);
            originalBillFee.setPaidValue(originalBillFee.getFeeValue());
            originalBillFee.setSettleValue(originalBillFee.getFeeValue());
            
            // Mark as collected by doctor if flag is set
            if (feeCollectedByDoctor) {
                originalBillFee.setFeeCollectedByDoctor(true);
            }
            
            getBillFeeFacade().edit(originalBillFee);
        }
    }

    private void saveBillItemForPaymentBill(Bill newPaymentBill, BillFee originalBillFee, Payment p) {
        BillItem newlyCreatedPayingBillItem = new BillItem();
        newlyCreatedPayingBillItem.setReferanceBillItem(originalBillFee.getBillItem());
        newlyCreatedPayingBillItem.setReferenceBill(originalBillFee.getBill());
        newlyCreatedPayingBillItem.setPaidForBillFee(originalBillFee);
        newlyCreatedPayingBillItem.setBill(newPaymentBill);
        newlyCreatedPayingBillItem.setCreatedAt(Calendar.getInstance().getTime());
        newlyCreatedPayingBillItem.setCreater(getSessionController().getLoggedUser());
        newlyCreatedPayingBillItem.setDiscount(0.0);
        newlyCreatedPayingBillItem.setGrossValue(originalBillFee.getFeeValue());
        newlyCreatedPayingBillItem.setNetValue(originalBillFee.getFeeValue());
        newlyCreatedPayingBillItem.setQty(1.0);
        newlyCreatedPayingBillItem.setRate(originalBillFee.getFeeValue());
        getBillItemFacade().create(newlyCreatedPayingBillItem);

        BillFee newlyCreatedBillFee = saveBillFee(newlyCreatedPayingBillItem, p);

        originalBillFee.setReferenceBillFee(newlyCreatedBillFee);
        getBillFeeFacade().edit(originalBillFee);

        newPaymentBill.getBillItems().add(newlyCreatedPayingBillItem);
    }

    public BillFee saveBillFee(BillItem bi, Payment p) {
        BillFee bf = new BillFee();
        bf.setCreatedAt(Calendar.getInstance().getTime());
        bf.setCreater(getSessionController().getLoggedUser());
        bf.setBillItem(bi);
        bf.setReferenceBillFee(bi.getPaidForBillFee());
        bf.setReferenceBillItem(bi.getReferanceBillItem());
        bf.setPatienEncounter(bi.getBill().getPatientEncounter());
        bf.setPatient(bi.getBill().getPatient());
        bf.setFeeValue(0 - bi.getNetValue());
        bf.setFeeGrossValue(0 - bi.getGrossValue());
        bf.setSettleValue(0 - bi.getNetValue());
        bf.setCreatedAt(new Date());
        bf.setDepartment(getSessionController().getDepartment());
        bf.setInstitution(getSessionController().getInstitution());
        bf.setBill(bi.getBill());

        if (bf.getId() == null) {
            getBillFeeFacade().create(bf);
        }
        createBillFeePaymentAndPayment(bf, p);
        return bf;
    }

    public void createBillFeePaymentAndPayment(BillFee bf, Payment p) {
        // BillFeePayment is deprecated and no longer used
    }

    private void fetchWithholdingDetailConfiguration() {
        allowUserToSelectPayWithholdingTaxDuringSurgeryPayments
                = configOptionApplicationController.getBooleanValueByKey(
                        "Allow User To Select Whether To Pay Withholding Tax During Professional Payments", true);

        withholdingTaxCalculationStatuses = new ArrayList<>();
        withholdingTaxCalculationStatuses.add("Depending On Payments");
        withholdingTaxCalculationStatuses.add("Include Withholding Tax");
        withholdingTaxCalculationStatuses.add("Exclude Withholding Tax");

        if (configOptionApplicationController.getBooleanValueByKey(
                "Withholding Tax Calculated Depending On This Month's Payments During Professional Payments", false)) {
            withholdingTaxCalculationStatus = "Depending On Payments";
        } else if (configOptionApplicationController.getBooleanValueByKey(
                "Withholding Tax Is Always Calculated During Professional Payments", true)) {
            withholdingTaxCalculationStatus = "Include Withholding Tax";
        } else if (configOptionApplicationController.getBooleanValueByKey(
                "Withholding Tax Is Never Calculated During Professional Payments", false)) {
            withholdingTaxCalculationStatus = "Exclude Withholding Tax";
        } else {
            withholdingTaxCalculationStatus = "Depending On Payments";
        }
    }

    public void recreateModel() {
        printPreview = false;
        items = null;
        dueSurgeryFees = null;
        payingSurgeryFees = null;
        fromDate = null;
        toDate = null;
        current = null;
        currentSurgeon = null;
        totalDue = 0.0;
        totalPaying = 0.0;
        printPreview = false;
        paymentMethod = null;
        feeCollectedByDoctor = false;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Autocomplete Methods">
    public List<Staff> completeSurgeon(String query) {
        List<Staff> suggestions;
        if (query == null || query.trim().isEmpty()) {
            suggestions = new ArrayList<>();
        } else {
            Map<String, Object> params = new HashMap<>();
            String jpql = "SELECT p FROM Staff p WHERE p.retired = false "
                    + "AND (UPPER(p.person.name) LIKE :query OR UPPER(p.code) LIKE :query) "
                    + "ORDER BY p.person.name";
            params.put("query", "%" + query.toUpperCase() + "%");
            suggestions = staffFacade.findByJpql(jpql, params);
        }
        return suggestions;
    }

    public List<BillItem> completeSurgery(String query) {
        List<BillItem> suggestions = new ArrayList<>();
        
        if (query == null || query.trim().isEmpty()) {
            return suggestions;
        }
        
        try {
            Map<String, Object> params = new HashMap<>();
            String jpql = "SELECT DISTINCT bi FROM BillItem bi "
                    + "JOIN bi.bill b "
                    + "WHERE bi.retired = false "
                    + "AND b.billTypeAtomic = :billTypeAtomic "
                    + "AND b.cancelled = false "
                    + "AND UPPER(bi.item.name) LIKE :query ";
            
            params.put("billTypeAtomic", BillTypeAtomic.INWARD_THEATRE_PROFESSIONAL_FEE_BILL);
            params.put("query", "%" + query.toUpperCase() + "%");
            
            // If surgeon is selected, filter surgeries by that surgeon's bills
            if (currentSurgeon != null) {
                jpql += "AND EXISTS (SELECT 1 FROM BillFee bf WHERE bf.billItem.id = bi.id AND bf.staff.id = :surgeonId) ";
                params.put("surgeonId", currentSurgeon.getId());
            }
            
            jpql += "ORDER BY bi.item.name";
            
            suggestions = getBillItemFacade().findByJpql(jpql, params);
        } catch (Exception e) {
            System.out.println("Error in completeSurgery: " + e.getMessage());
            e.printStackTrace();
        }
        
        return suggestions;
    }

    public boolean hasFeeCollectedByDoctor() {
        if (current == null) {
            return false;
        }
        
        List<BillFee> billFees = getBillFees();
        for (BillFee bf : billFees) {
            if (bf.isFeeCollectedByDoctor()) {
                return true;
            }
        }
        return false;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public List<BillComponent> getBillComponents() {
        if (getCurrent() != null) {
            String sql = "SELECT b FROM BillComponent b WHERE b.retired=false and b.bill.id=" + getCurrent().getId();
            billComponents = getBillComponentFacade().findByJpql(sql);
            if (billComponents == null) {
                billComponents = new ArrayList<>();
            }
        }
        return billComponents;
    }

    private List<BillFee> billFees;

    public List<BillFee> getBillFees() {
        if (getCurrent() != null) {
            if (billFees == null) {
                String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getCurrent().getId();
                billFees = getBillFeeFacade().findByJpql(sql);
                if (billFees == null) {
                    billFees = new ArrayList<>();
                }
            }
        }
        return billFees;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
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

    public Bill getCurrent() {
        return current;
    }

    public void setCurrent(Bill current) {
        this.current = current;
        this.billFees = null;  // Clear cache when current bill changes
    }

    public List<Bill> getItems() {
        return items;
    }

    public void setItems(List<Bill> items) {
        this.items = items;
    }

    public Staff getCurrentSurgeon() {
        return currentSurgeon;
    }

    public void setCurrentSurgeon(Staff currentSurgeon) {
        this.currentSurgeon = currentSurgeon;
    }

    public List<BillFee> getDueSurgeryFees() {
        return dueSurgeryFees;
    }

    public void setDueSurgeryFees(List<BillFee> dueSurgeryFees) {
        this.dueSurgeryFees = dueSurgeryFees;
    }

    public List<BillFee> getPayingSurgeryFees() {
        return payingSurgeryFees;
    }

    public void setPayingSurgeryFees(List<BillFee> payingSurgeryFees) {
        this.payingSurgeryFees = payingSurgeryFees;
    }

    public boolean isAllowUserToSelectPayWithholdingTaxDuringSurgeryPayments() {
        return allowUserToSelectPayWithholdingTaxDuringSurgeryPayments;
    }

    public void setAllowUserToSelectPayWithholdingTaxDuringSurgeryPayments(boolean allowUserToSelectPayWithholdingTaxDuringSurgeryPayments) {
        this.allowUserToSelectPayWithholdingTaxDuringSurgeryPayments = allowUserToSelectPayWithholdingTaxDuringSurgeryPayments;
    }

    public String getWithholdingTaxCalculationStatus() {
        if (withholdingTaxCalculationStatus == null) {
            withholdingTaxCalculationStatus = "Depending On Payments";
        }
        return withholdingTaxCalculationStatus;
    }

    public void setWithholdingTaxCalculationStatus(String withholdingTaxCalculationStatus) {
        this.withholdingTaxCalculationStatus = withholdingTaxCalculationStatus;
    }

    public List<String> getWithholdingTaxCalculationStatuses() {
        return withholdingTaxCalculationStatuses;
    }

    public void setWithholdingTaxCalculationStatuses(List<String> withholdingTaxCalculationStatuses) {
        this.withholdingTaxCalculationStatuses = withholdingTaxCalculationStatuses;
    }

    public double getWithholdingTax() {
        return withholdingTax;
    }

    public void setWithholdingTax(double withholdingTax) {
        this.withholdingTax = withholdingTax;
    }

    public double getTotalPaidForCurrentSurgeonForCurrentMonthForCurrentInstitute() {
        return totalPaidForCurrentSurgeonForCurrentMonthForCurrentInstitute;
    }

    public void setTotalPaidForCurrentSurgeonForCurrentMonthForCurrentInstitute(double totalPaidForCurrentSurgeonForCurrentMonthForCurrentInstitute) {
        this.totalPaidForCurrentSurgeonForCurrentMonthForCurrentInstitute = totalPaidForCurrentSurgeonForCurrentMonthForCurrentInstitute;
    }

    public Double getWithholdingTaxLimit() {
        if (withholdingTaxLimit == null) {
            withholdingTaxLimit = configOptionApplicationController.getDoubleValueByKey("Withholding Tax Limit", 0.0);
        }
        return withholdingTaxLimit;
    }

    public void setWithholdingTaxLimit(Double withholdingTaxLimit) {
        this.withholdingTaxLimit = withholdingTaxLimit;
    }

    public Double getWithholdingTaxPercentage() {
        if (withholdingTaxPercentage == null) {
            withholdingTaxPercentage = configOptionApplicationController.getDoubleValueByKey("Withholding Tax Percentage", 0.0);
        }
        return withholdingTaxPercentage;
    }

    public void setWithholdingTaxPercentage(Double withholdingTaxPercentage) {
        this.withholdingTaxPercentage = withholdingTaxPercentage;
    }

    public double getTotalDue() {
        return totalDue;
    }

    public void setTotalDue(double totalDue) {
        this.totalDue = totalDue;
    }

    public double getTotalPaying() {
        return totalPaying;
    }

    public void setTotalPaying(double totalPaying) {
        this.totalPaying = totalPaying;
    }

    public double getTotalPayingWithoutWht() {
        return totalPayingWithoutWht;
    }

    public void setTotalPayingWithoutWht(double totalPayingWithoutWht) {
        this.totalPayingWithoutWht = totalPayingWithoutWht;
    }

    public Boolean getPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(Boolean printPreview) {
        this.printPreview = printPreview;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public SearchKeyword getSearchKeyword() {
        if (searchKeyword == null) {
            searchKeyword = new SearchKeyword();
        }
        return searchKeyword;
    }

    public void setSearchKeyword(SearchKeyword searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public boolean isFeeCollectedByDoctor() {
        return feeCollectedByDoctor;
    }

    public void setFeeCollectedByDoctor(boolean feeCollectedByDoctor) {
        this.feeCollectedByDoctor = feeCollectedByDoctor;
    }

    public BillItem getSurgery() {
        return surgery;
    }

    public void setSurgery(BillItem surgery) {
        this.surgery = surgery;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    public BillComponentFacade getBillComponentFacade() {
        return billComponentFacade;
    }

    public void setBillComponentFacade(BillComponentFacade billComponentFacade) {
        this.billComponentFacade = billComponentFacade;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public BillFeePaymentFacade getBillFeePaymentFacade() {
        return billFeePaymentFacade;
    }

    public void setBillFeePaymentFacade(BillFeePaymentFacade billFeePaymentFacade) {
        this.billFeePaymentFacade = billFeePaymentFacade;
    }

    private String specialization; // For future specialization filtering

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    // </editor-fold>
}
