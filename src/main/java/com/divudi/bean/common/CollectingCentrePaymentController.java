package com.divudi.bean.common;

import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.light.common.BillLight;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import java.io.Serializable;
import java.util.ArrayList;
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
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Named
@SessionScoped
public class CollectingCentrePaymentController implements Serializable {

// <editor-fold defaultstate="collapsed" desc="Ejbs">
    @EJB
    BillFacade billFacade;
    @EJB
    private BillNumberGenerator billNumberGenerator;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    BillItemFacade billItemFacade;
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    SessionController sessionController;
// </editor-fold>
    
// <editor-fold defaultstate="collapsed" desc="Variables">
    private boolean ccPaymentSettlingStarted = false;
    private Date fromDate;
    private Date toDate;

    private Institution currentCollectingCentre;

    private List<BillLight> pandingCCpaymentBills;
    private List<BillLight> selectedCCpaymentBills;

    private double totalCCAmount;
    private double totalHospitalAmount;

    private double totalCCReceiveAmount = 0.0;
    private double payingTotalCCAmount = 0.0;
    private PaymentMethod paymentMethod;
    
    private boolean printPriview;

// </editor-fold>
    
// <editor-fold defaultstate="collapsed" desc="Navigation Method">
// </editor-fold>
    
// <editor-fold defaultstate="collapsed" desc="Functions">
    public CollectingCentrePaymentController() {
    }

    public void processCollectingCentrePayment() {
        if (currentCollectingCentre == null) {
            JsfUtil.addErrorMessage("Select Collecting Centre");
            return;
        }

        findPendingCCBills();
        calculateTotalOfPaymentReceive();
    }

    public void makeNull() {
        totalHospitalAmount = 0.0;
        totalCCAmount = 0.0;
        fromDate = null;
        toDate = null;
        selectedCCpaymentBills = null;
        pandingCCpaymentBills = null;
        ccPaymentSettlingStarted = false;
        currentCollectingCentre = null;
        totalCCReceiveAmount = 0.0;
        payingTotalCCAmount = 0.0;
        paymentMethod = null;
        printPriview = false;
    }

    public void findPendingCCBills() {
        System.out.println("findPendingCCBills");
        System.out.println("currentCollectingCentre = " + currentCollectingCentre);
        System.out.println("fromDate = " + fromDate);
        System.out.println("toDate = " + toDate);

        pandingCCpaymentBills = new ArrayList<>();
        String jpql;
        Map temMap = new HashMap();

        jpql = "select new com.divudi.core.light.common.BillLight(bill.id, bill.deptId, bill.referenceNumber, bill.createdAt, bill.patient.person.name,  bill.totalCenterFee, bill.totalHospitalFee ) "
                + " from Bill bill "
                + " where bill.collectingCentre=:cc "
                + " and bill.createdAt between :fromDate and :toDate "
                + " and bill.paid =:paid"
                + " and bill.retired=false ";

        jpql += " order by bill.createdAt asc ";
        temMap.put("cc", currentCollectingCentre);
        temMap.put("fromDate", fromDate);
        temMap.put("paid", false);
        temMap.put("toDate", toDate);

        pandingCCpaymentBills = billFacade.findLightsByJpql(jpql, temMap, TemporalType.TIMESTAMP);

        System.out.println("pandingCCpaymentBills = " + pandingCCpaymentBills.size());

        totalHospitalAmount = 0.0;
        totalCCAmount = 0.0;

        for (BillLight bl : pandingCCpaymentBills) {
            totalHospitalAmount += bl.getHospitalTotal();
            totalCCAmount += bl.getCcTotal();
        }

        System.out.println("totalHospitalAmount = " + totalHospitalAmount);
        System.out.println("totalCCAmount = " + totalCCAmount);

    }

    public void calculateTotalOfPaymentReceive() {
        System.out.println("calculateTotalOfPaymentReceive"); // Fixed typo in method name

        String jpql;
        Map<String, Object> temMap = new HashMap<>(); // Use generics for type safety

        jpql = "SELECT SUM(b.netTotal) " // Use "SUM" (JPQL is case-insensitive, but best practice)
                + "FROM Bill b " // Use a shorter alias for clarity
                + "WHERE b.billTypeAtomic = :atomic "
                + "AND b.fromInstitution = :cc "
                + "AND b.createdAt BETWEEN :fromDate AND :toDate "
                + "AND b.cancelled = FALSE "
                + "AND b.retired = FALSE";

        temMap.put("atomic", BillTypeAtomic.CC_PAYMENT_RECEIVED_BILL);
        temMap.put("cc", currentCollectingCentre);
        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);

        totalCCReceiveAmount = billFacade.findDoubleByJpql(jpql, temMap, TemporalType.TIMESTAMP);

        System.out.println("totalCCReceiveAmount = " + totalCCReceiveAmount);
    }

    public void performCalculations() {
        if (selectedCCpaymentBills == null) {
            payingTotalCCAmount = 0.0;
        } else {
            double totalHospitalAmount = 0.0;

            for (BillLight bl : selectedCCpaymentBills) {
                totalHospitalAmount += bl.getHospitalTotal();
            }
            double payAmount = totalCCReceiveAmount - totalHospitalAmount;

            if (payAmount < 0.0) {
                payingTotalCCAmount = 0.0;
            } else {
                payingTotalCCAmount = payAmount;
            }
        }
        System.out.println("payingCCAmount = " + payingTotalCCAmount);
    }

    public void settlePaymentBill() {
        System.out.println("Settle Payment Bill");
        Bill newlyBill = saveBill();
        createPayment(newlyBill,paymentMethod);
        
        for(BillLight b : selectedCCpaymentBills){
            Bill bill = billFacade.find(b.getId());
            saveBillItemForPaymentBill(bill,newlyBill);
        }
        
        // Update CC Balance
        // Update Drower
        
        printPriview = true;

    }

    public Bill saveBill() {
        System.out.println("Save Bill");
        Bill ccAgentPaymentBill = new Bill();
        
        ccAgentPaymentBill.setCreater(sessionController.getLoggedUser());
        ccAgentPaymentBill.setCreatedAt(new Date());
        ccAgentPaymentBill.setInstitution(sessionController.getInstitution());
        ccAgentPaymentBill.setDepartment(sessionController.getDepartment());
        ccAgentPaymentBill.setToInstitution(currentCollectingCentre);
        ccAgentPaymentBill.setBillType(BillType.CollectingCentreAgentPayment);
        ccAgentPaymentBill.setBillDate(new Date());
        ccAgentPaymentBill.setBillTime(new Date());
        
        ccAgentPaymentBill.setBillTypeAtomic(BillTypeAtomic.CC_AGENT_PAYMENT);
        ccAgentPaymentBill.setNetTotal(0.0);
        ccAgentPaymentBill.setTotal(0.0);
        ccAgentPaymentBill.setPaidAmount(0.0);
        
        ccAgentPaymentBill.setPaymentMethod(paymentMethod);
        String billNumber = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.CC_AGENT_PAYMENT);
        
        ccAgentPaymentBill.setDeptId(billNumber);
        ccAgentPaymentBill.setInsId(billNumber);

        if (ccAgentPaymentBill.getId() == null) {
            billFacade.create(ccAgentPaymentBill);
        } else {
            billFacade.edit(ccAgentPaymentBill);
        }
        
        return ccAgentPaymentBill;
    }
    
    public Payment createPayment(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        setPaymentMethodData(p, pm);
        return p;
    }
    
    public void setPaymentMethodData(Payment p, PaymentMethod pm) {
        p.setInstitution(sessionController.getInstitution());
        p.setDepartment(sessionController.getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(sessionController.getLoggedUser());
        p.setPaymentMethod(pm);

        p.setPaidValue(p.getBill().getNetTotal());

        if (p.getId() == null) {
            paymentFacade.create(p);
        }

    }
    
    private void saveBillItemForPaymentBill(Bill ccBill, Bill originalBill) {
        BillItem paymentBillItem = new BillItem();
        paymentBillItem.setReferenceBill(ccBill);
        paymentBillItem.setBill(originalBill);
        paymentBillItem.setCreatedAt(new Date());
        paymentBillItem.setCreater(sessionController.getLoggedUser());
        paymentBillItem.setDiscount(0.0);
        paymentBillItem.setGrossValue(0.0);
        paymentBillItem.setNetValue(0.0);
        billItemFacade.create(paymentBillItem);

        ccBill.setPaid(true);
        ccBill.setPaidAmount(0.0);
        ccBill.setPaidAt(new Date());
        ccBill.setPaidBill(originalBill);
        billFacade.edit(ccBill);
        
//      BillFee newlyCreatedBillFee = saveBillFee(paymentBillItem, p);
//      originalBillFee.setReferenceBillFee(newlyCreatedBillFee);
//      getBillFeeFacade().edit(originalBillFee);

        originalBill.getBillItems().add(paymentBillItem);
    }


// </editor-fold>
    
// <editor-fold defaultstate="collapsed" desc="Getter & Setter">
    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfMonth();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfMonth();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public boolean isCcPaymentSettlingStarted() {
        return ccPaymentSettlingStarted;
    }

    public void setCcPaymentSettlingStarted(boolean ccPaymentSettlingStarted) {
        this.ccPaymentSettlingStarted = ccPaymentSettlingStarted;
    }

    public Institution getCurrentCollectingCentre() {
        return currentCollectingCentre;
    }

    public void setCurrentCollectingCentre(Institution currentCollectingCentre) {
        this.currentCollectingCentre = currentCollectingCentre;
    }

    public List<BillLight> getPandingCCpaymentBills() {
        return pandingCCpaymentBills;
    }

    public void setPandingCCpaymentBills(List<BillLight> pandingCCpaymentBills) {
        this.pandingCCpaymentBills = pandingCCpaymentBills;
    }

    public List<BillLight> getSelectedCCpaymentBills() {
        return selectedCCpaymentBills;
    }

    public void setSelectedCCpaymentBills(List<BillLight> selectedCCpaymentBills) {
        this.selectedCCpaymentBills = selectedCCpaymentBills;
    }

    public double getTotalCCAmount() {
        return totalCCAmount;
    }

    public void setTotalCCAmount(double totalCCAmount) {
        this.totalCCAmount = totalCCAmount;
    }

    public double getTotalHospitalAmount() {
        return totalHospitalAmount;
    }

    public void setTotalHospitalAmount(double totalHospitalAmount) {
        this.totalHospitalAmount = totalHospitalAmount;
    }

    public double getPayingTotalCCAmount() {
        return payingTotalCCAmount;
    }

    public void setPayingTotalCCAmount(double payingTotalCCAmount) {
        this.payingTotalCCAmount = payingTotalCCAmount;
    }

    public double getTotalCCReceiveAmount() {
        return totalCCReceiveAmount;
    }

    public void setTotalCCReceiveAmount(double totalCCReceiveAmount) {
        this.totalCCReceiveAmount = totalCCReceiveAmount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
// </editor-fold>

    public boolean isPrintPriview() {
        return printPriview;
    }

    public void setPrintPriview(boolean printPriview) {
        this.printPriview = printPriview;
    }
}
