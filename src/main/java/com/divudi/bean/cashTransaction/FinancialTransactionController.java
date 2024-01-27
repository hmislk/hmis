package com.divudi.bean.cashTransaction;
// <editor-fold defaultstate="collapsed" desc="Imports">
import java.util.HashMap;
// </editor-fold>  
import com.divudi.bean.common.BillController;
import com.divudi.bean.common.SessionController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillType;
import com.divudi.entity.Bill;
import com.divudi.entity.Payment;
import com.divudi.facade.BillFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Named
@SessionScoped
public class FinancialTransactionController implements Serializable {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    BillFacade billFacade;
    @EJB
    PaymentFacade paymentFacade;
    // </editor-fold>  

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    SessionController sessionController;
    @Inject
    BillController billController;
    @Inject
    PaymentController paymentController;
    // </editor-fold>  

    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    private Bill currentBill;
    private Payment currentPayment;
    private Payment removingPayment;
    private List<Payment> currentBillPayments;
    private List<Bill> shiftBalanceTransferBills;
    private List<Bill> fundBillsForClosureBills;
    private Bill selectedBill;
    // </editor-fold>  

    // <editor-fold defaultstate="collapsed" desc="Constructors">
    public FinancialTransactionController() {
    }

    // </editor-fold> 
    
    // <editor-fold defaultstate="collapsed" desc="Navigational Methods">
    public String navigateToFinancialTransactionIndex() {
        return "/cashier/index?faces-redirect=false;";
    }

    public String navigateToCreateNewInitialFundBill() {
        prepareToAddNewInitialFundBill();
        return "/cashier/initial_fund_bill?faces-redirect=false;";
    }

    public String navigateShiftBalanceTransferReceiveBill() {
        prepareToAddNewInitialFundBill();
        getAllShiftBalanceTransferBill();
        return "/cashier/shift_balance_transfer_receive_bill";
    }

    private void prepareToAddNewInitialFundBill() {
        currentBill = new Bill();
        currentBill.setBillType(BillType.ShiftStartFundBill);
        currentBill.setBillClassType(BillClassType.Bill);
    }
    // </editor-fold>  

    // <editor-fold defaultstate="collapsed" desc="Functional Methods">
    public void addPaymentToInitialFundBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentBill.getBillType() != BillType.ShiftStartFundBill) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPayment == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPayment.getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select a Payment Method");
            return;
        }
        getCurrentBillPayments().add(currentPayment);
        calculateInitialFundBillTotal();
        currentPayment = null;
    }

    public void removePayment() {
        getCurrentBillPayments().remove(removingPayment);
        calculateInitialFundBillTotal();
        currentPayment = null;
    }

    private void calculateInitialFundBillTotal() {
        double total = 0.0;
        for (Payment p : getCurrentBillPayments()) {
            total += p.getPaidValue();
        }
        currentBill.setTotal(total);
    }

    public String settleInitialFundBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getBillType() != BillType.ShiftStartFundBill) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());

        billController.save(currentBill);
        for (Payment p : getCurrentBillPayments()) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            paymentController.save(p);
        }
        return "/cashier/initial_fund_bill_print";
    }
    // </editor-fold>  

// <editor-fold defaultstate="collapsed" desc="Sample Code Block">
// </editor-fold>  
// <editor-fold defaultstate="collapsed" desc="ShiftEndFundBill">
    
    public String navigateToShiftClosureSummaryBill(){
        
        return "/cashier/shift_closure_summery_bill";
    }
    
    
    public boolean nonClosedShiftStartFundBillIsAvailable(){
        String jpql = "select b "
                + " from Bill b "
                + " where b.staff=:staff "
                + " and b.retired=:ret "
                + " and b.billType=:ofb "
                + " and b.referenceBill is null";
        Map m = new HashMap();
        m.put("staff", sessionController.getLoggedUser().getStaff());
        m.put("ret", false);
        m.put("ofb", BillType.ShiftStartFundBill);
        return false;
    }
    
    public void listBillsFromInitialFundBillUpToNow(){
        
    }
    
    
// </editor-fold>  
// <editor-fold defaultstate="collapsed" desc="BalanceTransferFundBill">
    /**
     *
     * User click to Crete Transfer Bill Add (fromStaff 0 the user) Select User
     * to transfer (toStaff) settle to print
     *
     */
// </editor-fold>   
// <editor-fold defaultstate="collapsed" desc="BalanceTransferReceiveFundBill">
    /**
     *
     * pavan

 Another User create a BalanceTransferFundBill It has a toStaff attribute
 loggedUser.getStaff =toStaff List such bills Click on one of them Copy
 Payments from BalanceTransferFundBill User may change them settle to
 print
     *
     * @return 
     */
    public List<Bill> getAllShiftBalanceTransferBill() {
        String sql;
        shiftBalanceTransferBills = null;
        Map tempMap = new HashMap();
        sql = "select s from Bill s "
                + "where s.retired=false "
                + "and s.billType = :btype "
                + "and s.toStaff = :logStaff "
                + "order by s.createdAt ";
        tempMap.put("btype", BillType.BalanceTransferFundBill);
        tempMap.put("logStaff", sessionController.getLoggedUser().getStaff());
        shiftBalanceTransferBills = billFacade.findByJpql(sql, tempMap);
        return shiftBalanceTransferBills;
    }

    public void getPaymentsFromShiftBalanceTransferBill() {
        currentBillPayments = null;
        if (selectedBill != null) {
            for (Payment p : selectedBill.getPayments()) {
                currentBillPayments.add(p);
            }
        }

    }

    public String settleShiftBalanceTransferReceiveBill() {
        if (selectedBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        
        if (currentBill.getBillType() != BillType.ShiftStartFundBill) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        
        getPaymentsFromShiftBalanceTransferBill();
        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());
        removePayment();
        billController.save(currentBill);
        for (Payment p : currentBillPayments) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            paymentController.save(p);
        }
        shiftBalanceTransferBills.remove(currentBill);
        return "/cashier/shift_balance_transfer_receive_bill_print";
    }

// </editor-fold>      
// <editor-fold defaultstate="collapsed" desc="DepositFundBill">
    //Lawan
// </editor-fold>  
// <editor-fold defaultstate="collapsed" desc="WithdrawalFundBill">
    
    
    public String navigateToCreateNewWithdrawalProcessingBill(){
        prepareToAddNewWithdrawalProcessingBill();
        return "/cashier/initial_ithdrawal_processing_bill?faces-redirect=false;";
    }
    
    private void prepareToAddNewWithdrawalProcessingBill() {
        currentBill = new Bill();
        currentBill.setBillType(BillType.WithdrawalFundBill);
        currentBill.setBillClassType(BillClassType.Bill);
    }
    
    
//Damith
// </editor-fold>      
    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public Bill getCurrentBill() {
        return currentBill;
    }

    public void setCurrentBill(Bill currentBill) {
        this.currentBill = currentBill;
    }

    public Payment getCurrentPayment() {
        if (currentPayment == null) {
            currentPayment = new Payment();
        }
        return currentPayment;
    }

    public void setCurrentPayment(Payment currentPayment) {
        this.currentPayment = currentPayment;
    }

    public List<Payment> getCurrentBillPayments() {
        if (currentBillPayments == null) {
            currentBillPayments = new ArrayList<>();
        }
        return currentBillPayments;
    }

    public void setCurrentBillPayments(List<Payment> currentBillPayments) {
        this.currentBillPayments = currentBillPayments;
    }

    public Payment getRemovingPayment() {
        return removingPayment;
    }

    public void setRemovingPayment(Payment removingPayment) {
        this.removingPayment = removingPayment;
    }

    public List<Bill> getShiftBalanceTransferBills() {
        return shiftBalanceTransferBills;
    }

    public void setShiftBalanceTransferBills(List<Bill> shiftBalanceTransferBills) {
        this.shiftBalanceTransferBills = shiftBalanceTransferBills;
    }

    public Bill getSelectedBill() {
        return selectedBill;
    }

    public void setSelectedBill(Bill selectedBill) {
        this.selectedBill = selectedBill;
    }

    // </editor-fold>  

    public List<Bill> getFundBillsForClosureBills() {
        return fundBillsForClosureBills;
    }

    public void setFundBillsForClosureBills(List<Bill> fundBillsForClosureBills) {
        this.fundBillsForClosureBills = fundBillsForClosureBills;
    }
}
