package com.divudi.bean.common;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;

import com.divudi.entity.Bill;
import com.divudi.entity.BillComponent;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillFeePayment;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Payment;
import com.divudi.entity.RefundBill;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillFeePaymentFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.CancelledBillFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.RefundBillFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.java.CommonFunctions;
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
import org.primefaces.model.LazyDataModel;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.Title;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.service.ProfessionalPaymentService;
import java.text.DecimalFormat;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class StaffPaymentBillController implements Serializable {

    @EJB
    private RefundBillFacade refundBillFacade;
    @EJB
    private CancelledBillFacade cancelledBillFacade;
    @EJB
    private BillComponentFacade billComponentFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    BillFeePaymentFacade BillFeePaymentFacade;
    @EJB
    StaffFacade staffFacade;
    @EJB
    BillNumberGenerator billNumberBean;
    @EJB
    CashTransactionBean cashTransactionBean;

    @EJB
    ProfessionalPaymentService professionalPaymentService;

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    SessionController sessionController;
    @Inject
    private BillBeanController billBean;
    @Inject
    DrawerController drawerController;

    private List<BillComponent> billComponents;
    private List<BillItem> billItems;
    private static final long serialVersionUID = 1L;
    private Date fromDate;
    private Date toDate;
    private CommonFunctions commonFunctions;
    List<Bill> selectedItems;
    private Bill current;
    private List<Bill> items = null;
    String selectText = "";
    Staff currentStaff;
    private List<BillFee> dueBillFeeReport;
    List<BillFee> dueBillFees;
    List<BillFee> payingBillFees;
    double totalDue;
    double totalPaying;
    private double totalPayingWithoutWht;
    private double withholdingTax;
    private double totalPaidForCurrentProfessionalForCurrentMonthForCurrentInstitute;
    private Double withholdingTaxLimit;
    private Double withholdingTaxPercentage;

    private Boolean printPreview = false;
    PaymentMethod paymentMethod;
    Speciality speciality;
    private SearchKeyword searchKeyword;
    private PaymentMethodData staffPaymentMethodData;
    private List<BillFee> billFees;
    private List<BillFee> tblBillFees;
    private LazyDataModel<BillFee> dueBillFee;

    public Title[] getTitle() {
        return Title.values();
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

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

    public void newPayment() {
        recreateModel();

    }

    private void recreateModel() {
        billFees = null;
        billItems = null;
        printPreview = false;
        billItems = null;
        selectedItems = null;
        items = null;
        dueBillFeeReport = null;
        dueBillFees = null;
        payingBillFees = null;
        billFees = null;
        current = null;
        selectText = "";
        currentStaff = null;
        totalDue = 0.0;
        totalPaying = 0.0;
        withholdingTax = 0.0;
        printPreview = false;
        paymentMethod = PaymentMethod.Cash;
        speciality = null;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
//        currentStaff = null;
//        dueBillFees = new ArrayList<>();
//        payingBillFees = new ArrayList<>();
//        totalPaying = 0.0;
//        totalDue = 0.0;
    }

    public List<Staff> completeStaff(String query) {
        List<Staff> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        }

        HashMap hm = new HashMap();
        if (speciality != null) {
            sql = "select p from Staff p "
                    + " where p.retired=false "
                    + " and ((p.person.name) like :q "
                    + " or  (p.code) like :q ) "
                    + " and p.speciality=:sp "
                    + " order by p.person.name";
            hm.put("sp", getSpeciality());
        } else {
            sql = "select p from Staff p "
                    + " where p.retired=false "
                    + " and ((p.person.name) like :q "
                    + " or  (p.code) like :q )"
                    + " order by p.person.name";
        }
        //////// // System.out.println(sql);
        hm.put("q", "%" + query.toUpperCase() + "%");
        suggestions = getStaffFacade().findByJpql(sql, hm, 20);

        return suggestions;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
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

    public void calculateDueFeesForOpdForSelectedPeriod() {
        if (currentStaff == null || currentStaff.getId() == null) {
            dueBillFees = new ArrayList<>();
            return;
        }
        List<BillTypeAtomic> btcs = new ArrayList<>();
        btcs.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
        btcs.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        btcs.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);
        btcs.add(BillTypeAtomic.CC_BILL);
        String jpql;
        HashMap params = new HashMap();
        jpql = "select bf "
                + " from BillFee bf "
                + " where bf.retired=false "
                + " and bf.bill.billTypeAtomic in :btcs "
                + " and bf.bill.cancelled=:bc "
                + " and bf.bill.refunded=:brfnd "
                + " and bf.bill.createdAt between :fd and :td "
                + " and (bf.feeValue - bf.paidValue) > 0 "
                + " and bf.staff=:staff ";
        params.put("btcs", btcs);
        params.put("bc", false);
        params.put("brfnd", false);
        params.put("fd", fromDate);
        params.put("td", toDate);
        params.put("staff", currentStaff);

        boolean testing = false;
        if (testing) {
            BillFee bf = new BillFee();
            bf.getBill();
        }

        dueBillFees = getBillFeeFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);

        if (configOptionApplicationController.getBooleanValueByKey("Remove Refunded Bill From OPD Staff Payment")) {
            List<BillFee> removeingBillFees = new ArrayList<>();
            for (BillFee bf : dueBillFees) {
                params = new HashMap();
                jpql = "SELECT bi FROM BillItem bi where "
                        + " bi.retired=false"
                        + " and bi.bill.cancelled=false "
                        + " and type(bi.bill)=:class "
                        + " and bi.referanceBillItem.id=" + bf.getBillItem().getId();
                params.put("class", RefundBill.class);
                BillItem rbi = getBillItemFacade().findFirstByJpql(jpql, params);

                if (rbi != null) {
                    removeingBillFees.add(bf);
                }

            }
            dueBillFees.removeAll(removeingBillFees);
        }
        calculateTotalPaymentsForTheProfessionalForCurrentMonthForCurrentInstitution();
        peformeCalculations();
    }

    public void calculateDueFees() {
        if (currentStaff == null || currentStaff.getId() == null) {
            dueBillFees = new ArrayList<>();
        } else {
            List<BillTypeAtomic> btcs = new ArrayList<>();
            btcs.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
            btcs.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            btcs.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);
            btcs.add(BillTypeAtomic.CC_BILL);
            String sql;
            HashMap h = new HashMap();
            sql = "select b from BillFee b where "
                    + " b.retired=false"
                    + " and b.bill.billTypeAtomic in :btcs"
                    + " and b.bill.cancelled=false "
                    + " and (b.feeValue - b.paidValue) > 0 "
                    + " and b.staff=:staff ";
            h.put("btcs", btcs);
            h.put("staff", currentStaff);

            dueBillFees = getBillFeeFacade().findByJpql(sql, h, TemporalType.TIMESTAMP);

            if (configOptionApplicationController.getBooleanValueByKey("Remove Refunded Bill From OPD Staff Payment")) {
                List<BillFee> removeingBillFees = new ArrayList<>();
                for (BillFee bf : dueBillFees) {
                    h = new HashMap();
                    sql = "SELECT bi FROM BillItem bi where "
                            + " bi.retired=false"
                            + " and bi.bill.cancelled=false "
                            + " and type(bi.bill)=:class "
                            + " and bi.referanceBillItem.id=" + bf.getBillItem().getId();
                    h.put("class", RefundBill.class);
                    BillItem rbi = getBillItemFacade().findFirstByJpql(sql, h);

                    if (rbi != null) {
                        removeingBillFees.add(bf);
                    }

                }
                dueBillFees.removeAll(removeingBillFees);
            }

        }
    }

    public void calculateDueFeesOpdForSelectedPeriod() {
        if (currentStaff == null || currentStaff.getId() == null) {
            dueBillFees = new ArrayList<>();
        } else {
            List<BillTypeAtomic> btcs = new ArrayList<>();
            btcs.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
            btcs.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            btcs.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);
            btcs.add(BillTypeAtomic.CC_BILL);
            String jpql;
            HashMap params = new HashMap();
            jpql = "select bf from BillFee bf where "
                    + " bf.retired=false"
                    + " and bf.bill.createdAt between :fd and :td "
                    + " and bf.bill.billTypeAtomic in :btcs"
                    + " and bf.bill.cancelled=false "
                    + " and (bf.feeValue - bf.paidValue) > 0 "
                    + " and bf.staff=:staff ";
            params.put("btcs", btcs);
            params.put("fd", fromDate);
            params.put("td", toDate);
            params.put("staff", currentStaff);

            dueBillFees = getBillFeeFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);

            if (configOptionApplicationController.getBooleanValueByKey("Remove Refunded Bill From OPD Staff Payment")) {
                List<BillFee> removeingBillFees = new ArrayList<>();
                for (BillFee bf : dueBillFees) {
                    params = new HashMap();
                    jpql = "SELECT bi FROM BillItem bi where "
                            + " bi.retired=false"
                            + " and bi.bill.cancelled=false "
                            + " and type(bi.bill)=:class "
                            + " and bi.referanceBillItem.id=" + bf.getBillItem().getId();
                    params.put("class", RefundBill.class);
                    BillItem rbi = getBillItemFacade().findFirstByJpql(jpql, params);

                    if (rbi != null) {
                        removeingBillFees.add(bf);
                    }

                }
                dueBillFees.removeAll(removeingBillFees);
            }

        }
    }

    public void calculateTotalDue() {
        totalDue = 0;
        for (BillFee f : dueBillFees) {
            totalDue = totalDue + f.getFeeValue() - f.getPaidValue();
        }
    }

    public void peformeCalculations() {
        calculateTotalDue();
        calculatePaymentsSelected();
        calculateWithholdingTax();
    }

    public void calculatePaymentsSelected() {
        totalPaying = 0;
        for (BillFee f : payingBillFees) {
            totalPaying = totalPaying + (f.getFeeValue() - f.getPaidValue());
        }
    }

    public void calculateWithholdingTax() {
        System.out.println("Calculating Withholding Tax:");
        System.out.println("totalPaying = " + totalPaying);
        System.out.println("Total Paid For Current Professional This Month: " + totalPaidForCurrentProfessionalForCurrentMonthForCurrentInstitute);
        if (totalPaidForCurrentProfessionalForCurrentMonthForCurrentInstitute == 0.0) {
            withholdingTax = 0.0;
            totalPayingWithoutWht = totalPaying;
            System.out.println("No previous payments found. Withholding Tax set to 0.");
            return;
        }
        Double paidValue = Math.abs(totalPaidForCurrentProfessionalForCurrentMonthForCurrentInstitute);
        System.out.println("Paid Value: " + paidValue);
        System.out.println("Withholding Tax Limit: " + getWithholdingTaxLimit());
        if (getWithholdingTaxLimit() < paidValue) {
            withholdingTax = totalPaying * (getWithholdingTaxPercentage() / 100);
            System.out.println("Withholding Tax Percentage: " + getWithholdingTaxPercentage());
            System.out.println("Withholding Tax Calculated: " + withholdingTax);
        } else {
            withholdingTax = 0.0; // Ensure withholdingTax is set to 0.0
            System.out.println("Paid Value is less than Withholding Tax Limit. No Withholding Tax applied.");
        }
        totalPayingWithoutWht = totalPaying - withholdingTax;
        System.out.println("Total Paying Without WHT: " + totalPayingWithoutWht);
    }

    public void calculateTotalPaymentsForTheProfessionalForCurrentMonthForCurrentInstitution() {
        if (currentStaff == null) {
            return;
        }
        totalPaidForCurrentProfessionalForCurrentMonthForCurrentInstitute
                = professionalPaymentService.findSumOfProfessionalPaymentsDone(sessionController.getInstitution(), currentStaff);
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public List<BillFee> getDueBillFees() {
        return dueBillFees;
    }

    public void setDueBillFees(List<BillFee> dueBillFees) {
        this.dueBillFees = dueBillFees;
    }

    public List<BillFee> getPayingBillFees() {
        return payingBillFees;
    }

    public void setPayingBillFees(List<BillFee> payingBillFees) {
        //////// // System.out.println("setting paying bill fees " + payingBillFees.size());
        this.payingBillFees = payingBillFees;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public Staff getCurrentStaff() {
        return currentStaff;

    }

    public void setCurrentStaff(Staff currentStaff) {
        this.currentStaff = currentStaff;
    }

    public List<Bill> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from Bill c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        current = new BilledBill();
    }

    public void setSelectedItems(List<Bill> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private Bill createPaymentBill() {
        BilledBill tmp = new BilledBill();
        tmp.setBillDate(Calendar.getInstance().getTime());
        tmp.setBillTime(Calendar.getInstance().getTime());
        tmp.setBillType(BillType.PaymentBill);
        tmp.setBillTypeAtomic(BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_OPD_SERVICES);
        tmp.setCreatedAt(Calendar.getInstance().getTime());
        tmp.setCreater(getSessionController().getLoggedUser());
        tmp.setDepartment(getSessionController().getDepartment());
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.OPD_PROFESSIONAL_PAYMENT_BILL);
        tmp.setDeptId(deptId);
        tmp.setInsId(deptId);

        tmp.setDiscount(0.0);
        tmp.setDiscountPercent(0.0);

        tmp.setInstitution(getSessionController().getInstitution());
        tmp.setNetTotal(0 - totalPayingWithoutWht);
        tmp.setTax(withholdingTax);
        tmp.setPaymentMethod(paymentMethod);
        tmp.setStaff(currentStaff);
        tmp.setToStaff(currentStaff);
        tmp.setTotal(0 - totalPaying);

        return tmp;
    }

    public String navigateToStaffPaymentFromDuePayment(Staff s) {
        currentStaff = s;
        speciality = s.getSpeciality();
        System.out.println("Staff = " + currentStaff);
        System.out.println("Speciality = " + speciality);
        calculateDueFeesOpdForSelectedPeriod();
        System.out.println("Due Bill Fees = " + dueBillFees);
        return "/opd/professional_payments/payment_staff_bill?faces-redirect=true";
    }
    
    

    public String navigateToViewOpdPayProfessionalPayments() {
        recreateModel();
        return "/opd/professional_payments/payment_staff_bill?faces-redirect=true;";
    }

    private boolean errorCheck() {
        if (currentStaff == null) {
            JsfUtil.addErrorMessage("Please select a Staff Memeber");
            return true;
        }
        if (dueBillFees == null) {
            JsfUtil.addErrorMessage("Please select payments to update");
            return true;
        }
        peformeCalculations();
        if (totalPaying == 0) {
            JsfUtil.addErrorMessage("Please select payments to update");
            return true;
        }
        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Please select a payment method");
            return true;
        }

        return false;
    }

    public Boolean isPrintPreview() {
        return printPreview;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public void settleBill() {
        if (errorCheck()) {
            return;
        }
        peformeCalculations();
        Bill newlyCreatedPaymentBill = createPaymentBill();
        current = newlyCreatedPaymentBill;
        getBillFacade().create(newlyCreatedPaymentBill);
        Payment newlyCreatedPayment = createPayment(newlyCreatedPaymentBill, paymentMethod);
        drawerController.updateDrawerForOuts(newlyCreatedPayment);
        saveBillCompo(newlyCreatedPaymentBill, newlyCreatedPayment);
        printPreview = true;
        JsfUtil.addSuccessMessage("Successfully Paid");
    }

    private void saveBillCompo(Bill paymentBill, Payment paymentBillPayment) {
        for (BillFee originalBillFee : getPayingBillFees()) {
//            saveBillItemForPaymentBill(b, bf); //for create bill fees and billfee payments
            saveBillItemForPaymentBill(paymentBill, originalBillFee, paymentBillPayment);
//            saveBillFeeForPaymentBill(b,bf); No need to add fees for this bill
            originalBillFee.setPaidValue(originalBillFee.getFeeValue());
            originalBillFee.setSettleValue(originalBillFee.getFeeValue());
            getBillFeeFacade().edit(originalBillFee);
            //////// // System.out.println("marking as paid");
        }
    }

    private void saveBillItemForPaymentBill(Bill b, BillFee bf) {
        BillItem i = new BillItem();
        i.setReferanceBillItem(bf.getBillItem());
        i.setReferenceBill(bf.getBill());
        i.setPaidForBillFee(bf);
        i.setBill(b);
        i.setCreatedAt(Calendar.getInstance().getTime());
        i.setCreater(getSessionController().getLoggedUser());
        i.setDiscount(0.0);
        i.setGrossValue(bf.getFeeValue());
//        if (bf.getBillItem() != null && bf.getBillItem().getItem() != null) {
//            i.setItem(bf.getBillItem().getItem());
//        }
        i.setNetValue(bf.getFeeValue());
        i.setQty(1.0);
        i.setRate(bf.getFeeValue());
        getBillItemFacade().create(i);
        b.getBillItems().add(i);
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

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public BillFacade getEjbFacade() {
        return billFacade;
    }

    public void setEjbFacade(BillFacade ejbFacade) {
        this.billFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public StaffPaymentBillController() {
    }

    public Bill getCurrent() {
        if (current == null) {
            current = new BilledBill();
        }
        return current;
    }

    public void prepareToInitializeNewProfessionalPayment() {
        currentStaff = null;
        dueBillFees = new ArrayList<>();
        payingBillFees = new ArrayList<>();
        totalPaying = 0.0;
        totalDue = 0.0;
        recreateModel();
    }

    public void setCurrent(Bill current) {
        this.current = current;
    }

    public void delete() {
        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
//        getItems();
        current = null;
        getCurrent();
    }

    //for bill fee payments
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

    public void createBillFeePaymentAndPayment(BillFee bf, Payment p) {
        BillFeePayment bfp = new BillFeePayment();
        bfp.setBillFee(bf);
        bfp.setAmount(bf.getSettleValue());
        bfp.setInstitution(getSessionController().getInstitution());
        bfp.setDepartment(getSessionController().getDepartment());
        bfp.setCreater(getSessionController().getLoggedUser());
        bfp.setCreatedAt(new Date());
        bfp.setPayment(p);
        getBillFeePaymentFacade().create(bfp);
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

    //for bill fee payments
    private BillFacade getFacade() {
        return billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public Boolean getPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(Boolean printPreview) {
        this.printPreview = printPreview;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
        //  resetLists();
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
        //  resetLists();
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public List<BillFee> getDueBillFeeReport() {
        String sql;
        Map temMap = new HashMap();
        sql = "select b from BillFee b where b.retired=false and b.bill.cancelled=false and (b.feeValue - b.paidValue) > 0 and b.bill.institution.id=" + getSessionController().getInstitution().getId() + " and b.bill.billDate between :fromDate and :toDate order by b.staff.id  ";
        //////// // System.out.println("sql is " + sql);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        dueBillFeeReport = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        //////// // System.out.println(dueBillFeeReport.size());

        if (dueBillFeeReport == null) {
            dueBillFeeReport = new ArrayList<BillFee>();
        }

        return dueBillFeeReport;
    }

    public List<BillFee> getDueBillFeeReportAll() {

        String sql;
        Map temMap = new HashMap();
        if (!getSelectText().equals("")) {
            sql = "select b from BillFee b where b.retired=false and (b.bill.billType!=:btp and b.bill.billType!=:btp2) and b.bill.cancelled=false and (b.feeValue - b.paidValue) > 0 and  b.bill.billDate between :fromDate and :toDate and (b.staff.person.name) like '%" + selectText.toUpperCase() + "%' order by b.staff.id  ";
        } else {
            sql = "select b from BillFee b where b.retired=false and (b.bill.billType!=:btp and b.bill.billType!=:btp2) and b.bill.cancelled=false and (b.feeValue - b.paidValue) > 0 and  b.bill.billDate between :fromDate and :toDate order by b.staff.id  ";
        }
        //////// // System.out.println("sql is " + sql);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", BillType.ChannelPaid);
        temMap.put("btp2", BillType.ChannelCredit);

        dueBillFeeReport = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        //////// // System.out.println(dueBillFeeReport.size());

        if (dueBillFeeReport == null) {
            dueBillFeeReport = new ArrayList<BillFee>();
        }

        return dueBillFeeReport;
    }

    private boolean paymentMethodDataErrorCheck() {
        if (getCurrent().getPaymentMethod() == PaymentMethod.Card) {
            if (getStaffPaymentMethodData().getCreditCard().getNo().trim().equalsIgnoreCase("")) {
                JsfUtil.addErrorMessage("Add CreditCard No");
                return true;
            } else if (getStaffPaymentMethodData().getCreditCard().getInstitution() == null) {
                JsfUtil.addErrorMessage("Select Card Bank");
                return true;
            } else if (getStaffPaymentMethodData().getCreditCard().getComment().trim().equalsIgnoreCase("") && configOptionApplicationController.getBooleanValueByKey("Staff Credit Settle - CreditCard Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Add Comment");
                return true;
            }
            return false;
        } else if (getCurrent().getPaymentMethod() == PaymentMethod.Cheque) {
            if (getStaffPaymentMethodData().getCheque().getNo().trim().equalsIgnoreCase("")) {
                JsfUtil.addErrorMessage("Add Cheque No");
                return true;
            } else if (getStaffPaymentMethodData().getCheque().getInstitution() == null) {
                JsfUtil.addErrorMessage("Select Cheque Bank");
                return true;
            } else if (getStaffPaymentMethodData().getCheque().getDate() == null) {
                JsfUtil.addErrorMessage("Add Cheque Date");
                return true;
            } else if (getStaffPaymentMethodData().getCheque().getComment().trim().equalsIgnoreCase("") && configOptionApplicationController.getBooleanValueByKey("Staff Credit Settle - Cheque Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Add Comment");
                return true;
            }
            return false;
        } else if (getCurrent().getPaymentMethod() == PaymentMethod.Slip) {
            if (getStaffPaymentMethodData().getSlip().getInstitution() == null) {
                JsfUtil.addErrorMessage("Select Slip Bank");
                return true;
            } else if (getStaffPaymentMethodData().getSlip().getDate() == null) {
                JsfUtil.addErrorMessage("Add Slip Date");
                return true;
            } else if (getStaffPaymentMethodData().getSlip().getComment().trim().equalsIgnoreCase("") && configOptionApplicationController.getBooleanValueByKey("Staff Credit Settle - Slip Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Add Comment");
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean errorCheckForCreditSettle() {
        if (getCurrentStaff() == null) {
            JsfUtil.addErrorMessage("Select Staff");
            return true;
        }

        if (getCurrent().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select Payment Method");
            return true;
        }

        if (getCurrent().getNetTotal() < 1) {
            JsfUtil.addErrorMessage("Type Amount");
            return true;
        }

        return false;
    }

    public void settleStaffCredit() {

        if (errorCheckForCreditSettle()) {
            return;
        }

        if (paymentMethodDataErrorCheck()) {
            return;
        }

        getCurrent().setTotal(getCurrent().getNetTotal());
        DecimalFormat df = new DecimalFormat("00000");
        String s = df.format(getCurrent().getIntInvoiceNumber());
        getCurrent().setInvoiceNumber(createInvoiceNumberSuffix() + s);
        saveBill();
        saveBillItem();
        createPaymentForStaffCreditSettleBill(getCurrent(), getCurrent().getPaymentMethod());

        currentStaff.setCurrentCreditValue(currentStaff.getCurrentCreditValue() - getCurrent().getNetTotal());
        staffFacade.edit(currentStaff);
        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;

    }

    private void saveBill() {

        getCurrent().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.StaffCreditSettle, BillClassType.BilledBill, BillNumberSuffix.PTYPAY));
        getCurrent().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.StaffCreditSettle, BillClassType.BilledBill, BillNumberSuffix.PTYPAY));
        getCurrent().setBillTypeAtomic(BillTypeAtomic.STAFF_CREDIT_SETTLE);
        getCurrent().setBillType(BillType.StaffCreditSettle);

        getCurrent().setDepartment(getSessionController().getDepartment());
        getCurrent().setInstitution(getSessionController().getInstitution());
//        getCurrent().setComments(comment);

        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());

        getCurrent().setStaff(currentStaff);

        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());

        getCurrent().setTotal(getCurrent().getNetTotal());
        getCurrent().setNetTotal(getCurrent().getNetTotal());

        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getStaffPaymentMethodData());

        getBillFacade().create(getCurrent());
    }

    private String createInvoiceNumberSuffix() {

        Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH);
        String s1;
        String s2;
        if (m < 3) {
            s1 = Integer.toString(y - 1);
            s2 = Integer.toString(y);

        } else {
            s1 = Integer.toString(y);
            s2 = Integer.toString(y + 1);

        }
        String s = s1.substring(2, 4) + s2.substring(2, 4) + "-";

        return s;
    }

    public void createPaymentForStaffCreditSettleBill(Bill b, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(b);
        p.setPaidValue(0 - Math.abs(b.getNetTotal()));
        setPaymentMethodData(p, pm);
    }

    private void saveBillItem() {
        BillItem tmp = new BillItem();

        tmp.setCreatedAt(new Date());
        tmp.setCreater(getSessionController().getLoggedUser());
        tmp.setBill(getCurrent());
        tmp.setNetValue(0 - getCurrent().getNetTotal());
        getBillItemFacade().create(tmp);
    }

    public void createNewSettleBill() {
        current = new Bill();
        makeNull();
    }

    public void makeNull() {
        paymentMethod = null;
        staffPaymentMethodData = null;
        currentStaff = null;
        printPreview = false;
    }

    public void setDueBillFeeReport(List<BillFee> dueBillFeeReport) {
        this.dueBillFeeReport = dueBillFeeReport;
    }

    public List<BillItem> getBillItems() {
        if (getCurrent() != null) {
            String sql = "SELECT b FROM BillItem b WHERE b.retired=false and b.bill.id=" + getCurrent().getId();
            billItems = getBillItemFacade().findByJpql(sql);
            if (billItems == null) {
                billItems = new ArrayList<BillItem>();
            }
        }

        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public CancelledBillFacade getCancelledBillFacade() {
        return cancelledBillFacade;
    }

    public void setCancelledBillFacade(CancelledBillFacade cancelledBillFacade) {
        this.cancelledBillFacade = cancelledBillFacade;
    }

    public BillComponentFacade getBillComponentFacade() {
        return billComponentFacade;
    }

    public void setBillComponentFacade(BillComponentFacade billComponentFacade) {
        this.billComponentFacade = billComponentFacade;
    }

    public RefundBillFacade getRefundBillFacade() {
        return refundBillFacade;
    }

    public void setRefundBillFacade(RefundBillFacade refundBillFacade) {
        this.refundBillFacade = refundBillFacade;
    }

    public void setBillComponents(List<BillComponent> billComponents) {
        this.billComponents = billComponents;
    }

    public void setBillFees(List<BillFee> billFees) {
        this.billFees = billFees;
    }

    public void setItems(List<Bill> items) {
        this.items = items;
    }

    public LazyDataModel<BillFee> getDueBillFee() {
        return dueBillFee;
    }

    public void setDueBillFee(LazyDataModel<BillFee> dueBillFee) {
        this.dueBillFee = dueBillFee;
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

    public List<BillFee> getTblBillFees() {
        return tblBillFees;
    }

    public void setTblBillFees(List<BillFee> tblBillFees) {
        this.tblBillFees = tblBillFees;
    }

    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    public BillFeePaymentFacade getBillFeePaymentFacade() {
        return BillFeePaymentFacade;
    }

    public void setBillFeePaymentFacade(BillFeePaymentFacade BillFeePaymentFacade) {
        this.BillFeePaymentFacade = BillFeePaymentFacade;
    }

    public PaymentMethodData getStaffPaymentMethodData() {
        if (staffPaymentMethodData == null) {
            staffPaymentMethodData = new PaymentMethodData();
        }
        return staffPaymentMethodData;
    }

    public void setStaffPaymentMethodData(PaymentMethodData staffPaymentMethodData) {
        this.staffPaymentMethodData = staffPaymentMethodData;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public double getTotalPaidForCurrentProfessionalForCurrentMonthForCurrentInstitute() {
        return totalPaidForCurrentProfessionalForCurrentMonthForCurrentInstitute;
    }

    public void setTotalPaidForCurrentProfessionalForCurrentMonthForCurrentInstitute(double totalPaidForCurrentProfessionalForCurrentMonthForCurrentInstitute) {
        this.totalPaidForCurrentProfessionalForCurrentMonthForCurrentInstitute = totalPaidForCurrentProfessionalForCurrentMonthForCurrentInstitute;
    }

    public Double getWithholdingTaxLimit() {
        if (withholdingTaxLimit == null) {
            withholdingTaxLimit = configOptionApplicationController.getDoubleValueByKey("Withholding Tax Limit");
            if (withholdingTaxLimit == null || withholdingTaxLimit == 0.0) {
                withholdingTaxLimit = 100000.00;
            }
        }
        return withholdingTaxLimit;
    }

    public void setWithholdingTaxLimit(Double withholdingTaxLimit) {
        this.withholdingTaxLimit = withholdingTaxLimit;
    }

    public double getWithholdingTax() {
        return withholdingTax;
    }

    public void setWithholdingTax(double withholdingTax) {
        this.withholdingTax = withholdingTax;
    }

    public Double getWithholdingTaxPercentage() {
        if (withholdingTaxPercentage == null) {
            withholdingTaxPercentage = configOptionApplicationController.getDoubleValueByKey("Withholding Tax Percentage");
        }
        return withholdingTaxPercentage;
    }

    public void setWithholdingTaxPercentage(Double withholdingTaxPercentage) {
        this.withholdingTaxPercentage = withholdingTaxPercentage;
    }

    public double getTotalPayingWithoutWht() {
        return totalPayingWithoutWht;
    }

    public void setTotalPayingWithoutWht(double totalPayingWithoutWht) {
        this.totalPayingWithoutWht = totalPayingWithoutWht;
    }

}
