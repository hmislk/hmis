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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
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
    private List<Bill> fundTransferBillsToReceive;
    private List<Bill> fundBillsForClosureBills;
    private Bill selectedBill;
    private Bill nonClosedShiftStartFundBill;
    private List<Payment> paymentsFromShiftSratToNow;
    List<Payment> recievedBIllPayments;
    private List<Bill> allBillsShiftStartToNow;
    
    private double totalOpdBillValues;
    private double totalPharmecyBillValues;
    private double totalShiftStart;
    private double totalBalanceTransfer;
    private double totalTransferRecive;
    private double totalFunds;
    // </editor-fold>  

    // <editor-fold defaultstate="collapsed" desc="Constructors">
    public FinancialTransactionController() {
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="Navigational Methods">
    public String navigateToFinancialTransactionIndex() {
        resetClassVariables();
        return "/cashier/index?faces-redirect=false;";
    }

    public String navigateToCreateNewInitialFundBill() {
        resetClassVariables();
        prepareToAddNewInitialFundBill();
        return "/cashier/initial_fund_bill?faces-redirect=false;";
    }

    public String navigateToFundTransferBill() {
        resetClassVariables();
        prepareToAddNewFundTransferBill();
        return "/cashier/fund_transfer_bill";
    }

    public String navigateToFundDepositBill() {
        resetClassVariables();
        prepareToAddNewFundDepositBill();
        return "/cashier/deposit_funds";
    }

    public String navigateToReceiveNewFundTransferBill() {
        if (selectedBill == null) {
            JsfUtil.addErrorMessage("Please select a bill");
            return "";
        }
        if (selectedBill.getBillType() != BillType.FundTransferBill) {
            JsfUtil.addErrorMessage("Wrong Bill Type");
            return "";
        }
        resetClassVariablesWithoutSelectedBill();
        prepareToAddNewFundTransferReceiveBill();
        return "/cashier/fund_transfer_receive_bill";
    }

    public String navigateToReceiveFundTransferBillsForMe() {
        fillFundTransferBillsForMeToReceive();
        return "/cashier/fund_transfer_bills_for_me_to_receive";
    }

    private void prepareToAddNewInitialFundBill() {
        currentBill = new Bill();
        currentBill.setBillType(BillType.ShiftStartFundBill);
        currentBill.setBillClassType(BillClassType.Bill);
    }

    private void prepareToAddNewFundTransferBill() {
        currentBill = new Bill();
        currentBill.setBillType(BillType.FundTransferBill);
        currentBill.setBillClassType(BillClassType.Bill);
    }

    private void prepareToAddNewFundDepositBill() {
        currentBill = new Bill();
        currentBill.setBillType(BillType.DepositFundBill);
        currentBill.setBillClassType(BillClassType.Bill);
    }

    private void prepareToAddNewFundTransferReceiveBill() {
        System.out.println("this = " + "prepareToAddNewFundTransferReceiveBill working start");
        currentBill = new Bill();
        currentBill.setBillType(BillType.FundTransferReceivedBill);
        currentBill.setBillClassType(BillClassType.Bill);
        currentBill.setReferenceBill(selectedBill);
        currentBillPayments = new ArrayList<>();
        System.out.println("selected bill payments = " + selectedBill.getPayments().size());
        for (Payment p : selectedBill.getPayments()) {
            System.out.println("p = " + p);
            Payment np = p.copyAttributes();
            currentBillPayments.add(np);
            System.out.println("this = " + "payment Aded");

        }
        System.out.println("this = " + "prepareToAddNewFundTransferReceiveBill working end");
    }

    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Functional Methods">
    public void resetClassVariables() {
        currentBill = null;
        currentPayment = null;
        removingPayment = null;
        currentBillPayments = null;
        fundTransferBillsToReceive = null;
        fundBillsForClosureBills = null;
        selectedBill = null;
        nonClosedShiftStartFundBill = null;
        paymentsFromShiftSratToNow = null;
    }

    public void resetClassVariablesWithoutSelectedBill() {
        currentBill = null;
        currentPayment = null;
        removingPayment = null;
        currentBillPayments = null;
        fundTransferBillsToReceive = null;
        fundBillsForClosureBills = null;
        nonClosedShiftStartFundBill = null;
        paymentsFromShiftSratToNow = null;
    }

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

    public void addPaymentToFundTransferBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentBill.getBillType() != BillType.FundTransferBill) {
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
        calculateFundTransferBillTotal();
        currentPayment = null;
    }

    public void addPaymentToShiftEndFundBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentBill.getBillType() != BillType.ShiftEndFundBill) {
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
        calculateShiftEndFundBillTotal();
        currentPayment = null;
    }

    public void addPaymentToWithdrawalFundBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentBill.getBillType() != BillType.WithdrawalFundBill) {
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
        calculateWithdrawalFundBillTotal();
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
        currentBill.setNetTotal(total);
    }

    private void calculateFundTransferBillTotal() {
        double total = 0.0;
        for (Payment p : getCurrentBillPayments()) {
            total += p.getPaidValue();
        }
        currentBill.setTotal(total);
        currentBill.setNetTotal(total);
    }

    private void calculateShiftEndFundBillTotal() {
        double total = 0.0;
        for (Payment p : getCurrentBillPayments()) {
            total += p.getPaidValue();
        }
        currentBill.setTotal(total);
        currentBill.setNetTotal(total);
    }

    private void calculateWithdrawalFundBillTotal() {
        double total = 0.0;
        for (Payment p : getCurrentBillPayments()) {
            total += p.getPaidValue();
        }
        currentBill.setTotal(total);
        currentBill.setNetTotal(total);
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

        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());

        findNonClosedShiftStartFundBillIsAvailable();
        if (nonClosedShiftStartFundBill != null) {
            JsfUtil.addErrorMessage("A shift start fund bill is already available for closure.");
            return "";
        }

        billController.save(currentBill);
        for (Payment p : getCurrentBillPayments()) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            paymentController.save(p);
        }
        return "/cashier/initial_fund_bill_print";
    }

    public String settleFundTransferBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getBillType() != BillType.FundTransferBill) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getToStaff() == null) {
            JsfUtil.addErrorMessage("Select to whom to transfer");
            return "";
        }
        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());
        currentBill.setFromStaff(sessionController.getLoggedUser().getStaff());

        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());

        billController.save(currentBill);
        for (Payment p : getCurrentBillPayments()) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            paymentController.save(p);
        }
        return "/cashier/fund_transfer_bill_print";
    }

    public String settleWithdrawalFundBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getBillType() != BillType.WithdrawalFundBill) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());

        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());

        billController.save(currentBill);
        for (Payment p : getCurrentBillPayments()) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            paymentController.save(p);
        }
        return "/cashier/initial_withdrawal_processing_bill_print";
    }

    // </editor-fold>  
// <editor-fold defaultstate="collapsed" desc="Sample Code Block">
// </editor-fold>  
// <editor-fold defaultstate="collapsed" desc="ShiftEndFundBill">
    public String navigateToCreateShiftEndSummaryBill() {
        resetClassVariables();
        findNonClosedShiftStartFundBillIsAvailable();
        fillPaymentsFromShiftStartToNow();
        if (nonClosedShiftStartFundBill != null) {
            currentBill = new Bill();
            currentBill.setBillType(BillType.ShiftEndFundBill);
            currentBill.setBillClassType(BillClassType.Bill);
            currentBill.setReferenceBill(nonClosedShiftStartFundBill);
        } else {
            currentBill = null;
        }
        return "/cashier/shift_end_summery_bill";
    }

    public void fillPaymentsFromShiftStartToNow() {
       currentBillPayments = new ArrayList<>();
        if (nonClosedShiftStartFundBill == null) {
            return;
        }
        Long shiftStartBillId = nonClosedShiftStartFundBill.getId();
        String jpql = "select p "
                + "from Payment p "
                + "where p.creater = :cr "
                + "and p.retired = :ret "
                + "and p.id > :cid";
        Map<String, Object> m = new HashMap<>();
        m.put("cr", nonClosedShiftStartFundBill.getCreater());
        m.put("ret", false);
        m.put("cid", nonClosedShiftStartFundBill.getId());
        currentBillPayments = paymentFacade.findByJpql(jpql, m);
        resetTotalFundsValues();
        for (Payment p : currentBillPayments) {
            if (p.getBill().getBillType()==BillType.OpdBill) {
                totalOpdBillValues+=p.getPaidValue();
            }else if(p.getBill().getBillType()==BillType.PharmacySale){
                totalPharmecyBillValues+=p.getPaidValue();
            }else if(p.getBill().getBillType()==BillType.FundTransferBill){
                totalBalanceTransfer+=p.getPaidValue();
            }else if(p.getBill().getBillType()==BillType.FundTransferReceivedBill){
                totalTransferRecive+=p.getPaidValue();
            }else if(p.getBill().getBillType()==BillType.ShiftStartFundBill){
                totalShiftStart+=p.getPaidValue();
            }
        }
        calculateTotalFundsFromShiftStartToNow();
    }
    
    public void calculateTotalFundsFromShiftStartToNow(){
        double total=totalOpdBillValues+totalPharmecyBillValues+totalShiftStart+totalTransferRecive;
        totalFunds=total-totalBalanceTransfer;
    }
    
    public void resetTotalFundsValues(){
        totalOpdBillValues=0.0;
        totalPharmecyBillValues=0.0;
        totalShiftStart=0.0;
        totalTransferRecive=0.0;
        totalBalanceTransfer=0.0;
    }

    public void findNonClosedShiftStartFundBillIsAvailable() {
        nonClosedShiftStartFundBill = null;
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
        nonClosedShiftStartFundBill = billFacade.findFirstByJpql(jpql, m);
    }

    public void listBillsFromInitialFundBillUpToNow() {
        List<Bill> shiftStartFundBill;
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
        shiftStartFundBill = billFacade.findByJpql(jpql, m);

    }

    public String settleShiftEndFundBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getBillType() != BillType.ShiftEndFundBill) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getReferenceBill() == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());

        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());

        billController.save(currentBill);
        for (Payment p : getCurrentBillPayments()) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            paymentController.save(p);
        }

        nonClosedShiftStartFundBill.setReferenceBill(currentBill);
        billController.save(nonClosedShiftStartFundBill);
        resetTotalFundsValues();
        return "/cashier/shift_end_summery_bill_print";
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
     *
     * Another User create a BalanceTransferFundBill It has a toStaff attribute
     * loggedUser.getStaff =toStaff List such bills Click on one of them Copy
     * Payments from BalanceTransferFundBill User may change them settle to
     * print
     *
     * @return
     */
    public void fillFundTransferBillsForMeToReceive() {
        String sql;
        fundTransferBillsToReceive = null;
        Map tempMap = new HashMap();
        sql = "select s "
                + "from Bill s "
                + "where s.retired=:ret "
                + "and s.billType=:btype "
                + "and s.toStaff=:logStaff "
                + "and s.referenceBill is null "
                + "order by s.createdAt ";
        tempMap.put("btype", BillType.FundTransferBill);
        tempMap.put("ret", false);
        tempMap.put("logStaff", sessionController.getLoggedUser().getStaff());
        fundTransferBillsToReceive = billFacade.findByJpql(sql, tempMap);
    }

    public String settleFundTransferReceiveBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }

        if (currentBill.getReferenceBill() == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }

        if (currentBill.getBillType() != BillType.FundTransferReceivedBill) {
            JsfUtil.addErrorMessage("Error - bill type");
            return "";
        }

        if (currentBill.getReferenceBill().getBillType() != BillType.FundTransferBill) {
            JsfUtil.addErrorMessage("Error - Reference bill type");
            return "";
        }

        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());
        currentBill.setToStaff(sessionController.getLoggedUser().getStaff());
        currentBill.setFromStaff(currentBill.getReferenceBill().getFromStaff());
        billController.save(currentBill);
        System.out.println("currentBillPayments = " + currentBillPayments.size());
        for (Payment p : currentBillPayments) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            paymentController.save(p);
            System.out.println("p = " + p.getId());
        }
        currentBill.getReferenceBill().setReferenceBill(currentBill);
        billController.save(currentBill.getReferenceBill());
        
        return "/cashier/fund_transfer_receive_bill_print";
    }

// </editor-fold>      
// <editor-fold defaultstate="collapsed" desc="DepositFundBill">
    //Lawan
    public void addPaymentToFundDepositBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentBill.getBillType() != BillType.DepositFundBill) {
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
        calculateFundDepositBillTotal();
        currentPayment = null;
    }

    private void calculateFundDepositBillTotal() {
        double total = 0.0;
        for (Payment p : getCurrentBillPayments()) {
            total += p.getPaidValue();
        }
        currentBill.setTotal(total);
        currentBill.setNetTotal(total);
    }

    public String settleFundDepositBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getBillType() != BillType.DepositFundBill) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());

        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());

        billController.save(currentBill);
        for (Payment p : getCurrentBillPayments()) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            paymentController.save(p);
        }
        return "/cashier/deposit_funds_print";
    }
// </editor-fold>  
// <editor-fold defaultstate="collapsed" desc="WithdrawalFundBill">

    public String navigateToCreateNewWithdrawalProcessingBill() {
        prepareToAddNewWithdrawalProcessingBill();
        return "/cashier/initial_withdrawal_processing_bill?faces-redirect=false;";
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

    public List<Bill> getFundTransferBillsToReceive() {
        return fundTransferBillsToReceive;
    }

    public void setFundTransferBillsToReceive(List<Bill> fundTransferBillsToReceive) {
        this.fundTransferBillsToReceive = fundTransferBillsToReceive;
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

    public Bill getNonClosedShiftStartFundBill() {
        return nonClosedShiftStartFundBill;
    }

    public void setNonClosedShiftStartFundBill(Bill nonClosedShiftStartFundBill) {
        this.nonClosedShiftStartFundBill = nonClosedShiftStartFundBill;
    }

    public List<Payment> getPaymentsFromShiftSratToNow() {
        if (paymentsFromShiftSratToNow == null) {
            paymentsFromShiftSratToNow = new ArrayList<>();
        }
        return paymentsFromShiftSratToNow;
    }

    public void setPaymentsFromShiftSratToNow(List<Payment> paymentsFromShiftSratToNow) {
        this.paymentsFromShiftSratToNow = paymentsFromShiftSratToNow;
    }

    public List<Bill> getAllBillsShiftStartToNow() {
        if (allBillsShiftStartToNow == null) {
            allBillsShiftStartToNow = new ArrayList<>();
        }
        return allBillsShiftStartToNow;
    }

    public void setAllBillsShiftStartToNow(List<Bill> allBillsShiftStartToNow) {
        this.allBillsShiftStartToNow = allBillsShiftStartToNow;
    }

    public double getTotalOpdBillValues() {
        return totalOpdBillValues;
    }

    public void setTotalOpdBillValues(double totalOpdBillValues) {
        this.totalOpdBillValues = totalOpdBillValues;
    }

    public double getTotalPharmecyBillValues() {
        return totalPharmecyBillValues;
    }

    public void setTotalPharmecyBillValues(double totalPharmecyBillValues) {
        this.totalPharmecyBillValues = totalPharmecyBillValues;
    }

    public double getTotalShiftStart() {
        return totalShiftStart;
    }

    public void setTotalShiftStart(double totalShiftStart) {
        this.totalShiftStart = totalShiftStart;
    }

    public double getTotalBalanceTransfer() {
        return totalBalanceTransfer;
    }

    public void setTotalBalanceTransfer(double totalBalanceTransfer) {
        this.totalBalanceTransfer = totalBalanceTransfer;
    }

    public double getTotalTransferRecive() {
        return totalTransferRecive;
    }

    public void setTotalTransferRecive(double totalTransferRecive) {
        this.totalTransferRecive = totalTransferRecive;
    }

    public double getTotalFunds() {
        return totalFunds;
    }

    public void setTotalFunds(double totalFunds) {
        this.totalFunds = totalFunds;
    }
    
    

}
