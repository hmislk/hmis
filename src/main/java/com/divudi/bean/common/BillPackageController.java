/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.ItemLight;
import com.divudi.data.PaymentMethod;
import com.divudi.data.Sex;
import com.divudi.data.Title;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.data.dataStructure.YearMonthDay;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;

import com.divudi.ejb.ServiceSessionBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillComponent;
import com.divudi.entity.BillEntry;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Doctor;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Packege;
import com.divudi.entity.Patient;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.dataStructure.ComponentDetail;
import com.divudi.ejb.StaffBean;
import com.divudi.entity.Payment;
import com.divudi.facade.PaymentFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class BillPackageController implements Serializable, ControllerWithPatient, ControllerWithMultiplePayments {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;
    CommonFunctions commonFunctions;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillComponentFacade billComponentFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    CashTransactionBean cashTransactionBean;
    @EJB
    ServiceSessionBean serviceSessionBean;
    @EJB
    BillSessionFacade billSessionFacade;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    StaffBean staffBean;
    //</editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Injects">
    @Inject
    SessionController sessionController;
    @Inject
    private BillBeanController billBean;
    @Inject
    private BillSearch billSearch;
    @Inject
    ItemApplicationController itemApplicationController;
    @Inject
    PaymentSchemeController paymentSchemeController;
    @Inject
    ItemController itemController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    //</editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    private static final long serialVersionUID = 1L;

    private Bill batchBill;
    private boolean printPreview;
    //Interface Data
    private PaymentScheme paymentScheme;
    PaymentMethod paymentMethod;
    private Patient patient;
    private Doctor referredBy;
    private Institution creditCompany;
    private Staff staff;
    private double total;
    private double discount;
    private double netTotal;
    private double cashPaid;
    private double cashBalance;
    private Institution chequeBank;
    private BillItem currentBillItem;
    private Institution collectingCentre;
    private Staff toStaff;

    //Bill Items
    private List<BillComponent> lstBillComponents;
    private List<BillFee> lstBillFees;
    private List<BillItem> lstBillItems;
    private List<BillEntry> lstBillEntries;
    private Integer index;

    List<Bill> bills;
    private YearMonthDay yearMonthDay;
    PaymentMethodData paymentMethodData;
    Institution referredByInstitution;
    String referralId;

    private ItemLight itemLight;
    private List<ItemLight> opdPackages;
    private boolean patientDetailsEditable;
    private List<Payment> payments;

    //</editor-fold>
    private void savePatient() {
        if (getPatient() == null) {
            JsfUtil.addErrorMessage("No Patient to save");
            return;
        }
        if (getPatient().getPerson() == null) {
            JsfUtil.addErrorMessage("No person");
            return;
        }
        if (getPatient().getPerson().getId() == null) {
            getPatient().getPerson().setCreater(getSessionController().getLoggedUser());
            getPatient().getPerson().setCreatedAt(new Date());
            getPersonFacade().create(getPatient().getPerson());
        } else {
            getPersonFacade().edit(getPatient().getPerson());
        }
        if (getPatient().getId() == null) {
            getPatient().setCreater(getSessionController().getLoggedUser());
            getPatient().setCreatedAt(new Date());
            getPatientFacade().create(getPatient());
        } else {
            getPatientFacade().edit(getPatient());
        }
    }

    @Override
    public void toggalePatientEditable() {
        patientDetailsEditable = !patientDetailsEditable;
    }

    public void putToBills() {
        bills = new ArrayList<>();
        Set<Department> billDepts = new HashSet<>();
        for (BillEntry e : lstBillEntries) {
            billDepts.add(e.getBillItem().getItem().getDepartment());

        }
        for (Department d : billDepts) {
            BilledBill myBill = new BilledBill();

            saveBill(d, myBill);

            List<BillEntry> tmp = new ArrayList<>();
            List<BillItem> list = new ArrayList<>();
            for (BillEntry e : lstBillEntries) {

                if (Objects.equals(e.getBillItem().getItem().getDepartment().getId(), d.getId())) {
                    getBillBean().saveBillItem(myBill, e, getSessionController().getLoggedUser());
                    // getBillBean().calculateBillItem(myBill, e);   
                    list.add(e.getBillItem());
                    tmp.add(e);
                }
            }
            myBill.setBillItems(list);
            //////System.out.println("555");
            getBillBean().calculateBillItems(myBill, tmp);
            bills.add(myBill);
        }
    }

    private void saveBatchBill() {
        batchBill = new BilledBill();
        batchBill.setBillType(BillType.OpdBathcBill);
        batchBill.setBillTypeAtomic(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
        batchBill.setPaymentScheme(paymentScheme);
        batchBill.setPaymentMethod(paymentMethod);
        batchBill.setCreatedAt(new Date());
        batchBill.setCreater(getSessionController().getLoggedUser());

        batchBill.setDepartment(getSessionController().getLoggedUser().getDepartment());
        batchBill.setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        batchBill.setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        batchBill.setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        batchBill.setStaff(staff);
        batchBill.setReferredBy(referredBy);
        batchBill.setCreditCompany(creditCompany);

        getBillBean().setPaymentMethodData(batchBill, paymentMethod, getPaymentMethodData());

        batchBill.setBillDate(new Date());
        batchBill.setBillTime(new Date());
        batchBill.setPatient(getPatient());
        batchBill.setPaymentMethod(paymentMethod);
        batchBill.setPaymentScheme(getPaymentScheme());

        batchBill.setDeptId(getBillNumberBean().departmentBillNumberGenerator(batchBill.getDepartment(), batchBill.getToDepartment(), batchBill.getBillType(), BillClassType.BilledBill));
        batchBill.setInsId(getBillNumberBean().institutionBillNumberGenerator(batchBill.getInstitution(), batchBill.getBillType(), BillClassType.BilledBill, BillNumberSuffix.PACK));

        getBillFacade().create(batchBill);

        double dbl = 0;
        double dblTot = 0;
        for (Bill b : bills) {
            b.setBackwardReferenceBill(batchBill);
            dbl += b.getNetTotal();
            dblTot += b.getTotal();
            getBillFacade().edit(b);

            batchBill.getForwardReferenceBills().add(b);
        }

        batchBill.setNetTotal(dbl);
        batchBill.setTotal(dblTot);
        batchBill.setDiscount(dblTot - dbl);
        getBillFacade().edit(batchBill);

    }

    public void cancellAll() {
        Bill tmp = new CancelledBill();
        tmp.setCreatedAt(new Date());
        tmp.setCreater(getSessionController().getLoggedUser());
        getBillFacade().create(tmp);

        Bill billedBill = null;
        for (Bill b : bills) {
            billedBill = b.getBackwardReferenceBill();
            getBillSearch().setBill((BilledBill) b);
            getBillSearch().setPaymentMethod(b.getPaymentMethod());
            getBillSearch().setComment("Batch Cancell");
            //////System.out.println("ggg : " + getBillSearch().getComment());
            getBillSearch().cancelOpdBill();
        }

        tmp.copy(billedBill);
        tmp.setBilledBill(billedBill);

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(tmp, getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
    }

    private void saveBillItemSessions() {
        for (BillEntry be : lstBillEntries) {
            BillItem temBi = be.getBillItem();
            ////System.out.println("temBi = " + temBi);
            BillSession temBs = getServiceSessionBean().createBillSession(temBi);
            ////System.out.println("temBs = " + temBs);
            temBi.setBillSession(temBs);
            if (temBs != null) {
                getBillSessionFacade().create(temBs);
            }
        }
    }

    public void settleBill() {
        if (errorCheck()) {
            return;
        }
        savePatient();
        if (getBillBean().calculateNumberOfBillsPerOrder(getLstBillEntries()) == 1) {
            BilledBill temp = new BilledBill();
            Bill b = saveBill(lstBillEntries.get(0).getBillItem().getItem().getDepartment(), temp);
//            getBillBean().saveBillItems(b, getLstBillEntries(), getSessionController().getLoggedUser());
            b.setBillItems(getBillBean().saveBillItems(b, getLstBillEntries(), getSessionController().getLoggedUser()));
            getBillBean().calculateBillItems(b, getLstBillEntries());
            getBills().add(b);

        } else {
            //    //////System.out.println("11");
            putToBills();
            //   //////System.out.println("22");
        }

        saveBatchBill();
        payments = createPayments(getBatchBill(), paymentMethod);
        if (toStaff != null && getPaymentMethod() == PaymentMethod.Staff_Welfare) {
            staffBean.updateStaffWelfare(toStaff, batchBill.getNetTotal());
            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
        } else if (toStaff != null && getPaymentMethod() == PaymentMethod.Staff) {
            staffBean.updateStaffCredit(toStaff, batchBill.getNetTotal());
            JsfUtil.addSuccessMessage("Staff Credit Updated");
        }

        if (paymentMethod == PaymentMethod.PatientDeposit) {
            if (getPatient().getRunningBalance() != null) {
                getPatient().setRunningBalance(getPatient().getRunningBalance() - netTotal);
            } else {
                getPatient().setRunningBalance(0.0 - netTotal);
            }
            getPatientFacade().edit(getPatient());
        }
        saveBillItemSessions();

        clearBillItemValues();
        //////System.out.println("33");

        JsfUtil.addSuccessMessage(
                "Bill Saved");
        printPreview = true;
    }

    private Bill saveBill(Department bt, BilledBill temp) {

        //getCurrent().setCashBalance(cashBalance); 
        //getCurrent().setCashPaid(cashPaid);
        //  temp.setBillType(bt);
        temp.setBillType(BillType.OpdBill);
        temp.setBillTypeAtomic(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);

        temp.setBillPackege((Packege) currentBillItem.getItem());

        temp.setDepartment(getSessionController().getLoggedUser().getDepartment());
        temp.setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        temp.setToDepartment(bt);
        temp.setToInstitution(bt.getInstitution());

        temp.setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        temp.setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        temp.setStaff(staff);
        temp.setReferredBy(referredBy);
        temp.setCreditCompany(creditCompany);

        getBillBean().setPaymentMethodData(temp, paymentMethod, getPaymentMethodData());

        temp.setBillDate(new Date());
        temp.setBillTime(new Date());
        temp.setPatient(getPatient());
//        temp.setPatientEncounter(patientEncounter);
        temp.setPaymentMethod(paymentMethod);
        temp.setPaymentScheme(getPaymentScheme());

        temp.setCreatedAt(new Date());
        temp.setCreater(getSessionController().getLoggedUser());

        temp.setDeptId(getBillNumberBean().departmentBillNumberGenerator(temp.getDepartment(), temp.getToDepartment(), temp.getBillType(), BillClassType.BilledBill));
        temp.setInsId(getBillNumberBean().institutionBillNumberGenerator(temp.getInstitution(), temp.getToDepartment(), temp.getBillType(), BillClassType.BilledBill, BillNumberSuffix.PACK));

        if (temp.getId() == null) {
            getFacade().create(temp);
        }
        return temp;

    }

    public List<Payment> createPayments(Bill bill, PaymentMethod pm) {
        List<Payment> ps = new ArrayList<>();
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                Payment p = new Payment();
                p.setBill(bill);
                p.setInstitution(getSessionController().getInstitution());
                p.setDepartment(getSessionController().getDepartment());
                p.setCreatedAt(new Date());
                p.setCreater(getSessionController().getLoggedUser());
                p.setPaymentMethod(cd.getPaymentMethod());

                switch (cd.getPaymentMethod()) {
                    case Card:
                        p.setBank(cd.getPaymentMethodData().getCreditCard().getInstitution());
                        p.setCreditCardRefNo(cd.getPaymentMethodData().getCreditCard().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCreditCard().getTotalValue());
                        break;
                    case Cheque:
                        p.setChequeDate(cd.getPaymentMethodData().getCheque().getDate());
                        p.setChequeRefNo(cd.getPaymentMethodData().getCheque().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCheque().getTotalValue());
                        break;
                    case Cash:
                        p.setPaidValue(cd.getPaymentMethodData().getCash().getTotalValue());
                        break;
                    case ewallet:

                    case Agent:
                    case Credit:
                    case PatientDeposit:
                    case Slip:
                        p.setPaidValue(cd.getPaymentMethodData().getSlip().getTotalValue());
                        p.setBank(cd.getPaymentMethodData().getSlip().getInstitution());
                        p.setRealizedAt(cd.getPaymentMethodData().getSlip().getDate());
                    case OnCall:
                    case OnlineSettlement:
                    case Staff:
                        p.setPaidValue(cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                        if (cd.getPaymentMethodData().getStaffCredit().getToStaff() != null) {
                            staffBean.updateStaffCredit(cd.getPaymentMethodData().getStaffCredit().getToStaff(), cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
                        }
                    case YouOweMe:
                    case MultiplePaymentMethods:
                }

                paymentFacade.create(p);
                ps.add(p);
            }
        } else {
            Payment p = new Payment();
            p.setBill(bill);
            p.setInstitution(getSessionController().getInstitution());
            p.setDepartment(getSessionController().getDepartment());
            p.setCreatedAt(new Date());
            p.setCreater(getSessionController().getLoggedUser());
            p.setPaymentMethod(pm);

            switch (pm) {
                case Card:
                    p.setBank(paymentMethodData.getCreditCard().getInstitution());
                    p.setCreditCardRefNo(paymentMethodData.getCreditCard().getNo());
                    p.setPaidValue(paymentMethodData.getCreditCard().getTotalValue());
                    break;
                case Cheque:
                    p.setChequeDate(paymentMethodData.getCheque().getDate());
                    p.setChequeRefNo(paymentMethodData.getCheque().getNo());
                    p.setPaidValue(paymentMethodData.getCheque().getTotalValue());
                    break;
                case Cash:
                    p.setPaidValue(paymentMethodData.getCash().getTotalValue());
                    break;
                case ewallet:

                case Agent:
                case Credit:
                case PatientDeposit:
                case Slip:
                    p.setBank(paymentMethodData.getSlip().getInstitution());
                    p.setPaidValue(paymentMethodData.getSlip().getTotalValue());
                    p.setRealizedAt(paymentMethodData.getSlip().getDate());
                case OnCall:
                case OnlineSettlement:
                case Staff:
                case YouOweMe:
                case MultiplePaymentMethods:
            }

            p.setPaidValue(p.getBill().getNetTotal());
            paymentFacade.create(p);

            ps.add(p);
        }
        return ps;
    }

    public double calculatRemainForMultiplePaymentTotal() {

        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            double multiplePaymentMethodTotalValue = 0.0;
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffCredit().getTotalValue();

            }
            return total - multiplePaymentMethodTotalValue;
        }
        return total;
    }

    public void recieveRemainAmountAutomatically() {
        double remainAmount = calculatRemainForMultiplePaymentTotal();
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            int arrSize = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().size();
            ComponentDetail pm = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().get(arrSize - 1);
            if (pm.getPaymentMethod() == PaymentMethod.Cash) {
                pm.getPaymentMethodData().getCash().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Card) {
                pm.getPaymentMethodData().getCreditCard().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Cheque) {
                pm.getPaymentMethodData().getCheque().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Slip) {
                pm.getPaymentMethodData().getSlip().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.ewallet) {
                pm.getPaymentMethodData().getEwallet().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.PatientDeposit) {
                pm.getPaymentMethodData().getPatient_deposit().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Credit) {
                pm.getPaymentMethodData().getCredit().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Staff) {
                pm.getPaymentMethodData().getStaffCredit().setTotalValue(remainAmount);
            }

        }
    }

    private boolean errorCheck() {

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Name to Save Patient in Package Billing", false)) {
            if (getPatient().getPerson().getName() == null
                    || getPatient().getPerson().getName().trim().equals("")
                    || getPatient().getPerson().getSex() == null
                    || getPatient().getPerson().getDob() == null) {
                JsfUtil.addErrorMessage("Can not bill without Patient Name, Age or Sex.");
                return true;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Title And Gender To Save Patient in Package Billing", false)) {
            if (getPatient().getPerson().getTitle() == null) {
                JsfUtil.addErrorMessage("Please select title");
                return true;
            }
            if (getPatient().getPerson().getSex() == null) {
                JsfUtil.addErrorMessage("Please select gender");
                return true;
            }
        }
        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Age to Save Patient in Package Billing", false)) {
            if (getPatient().getPerson().getDob() == null) {
                JsfUtil.addErrorMessage("Please select patient date of birth");
                return true;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Phone Number to save Patient in Package Billing", false)) {
            if (getPatient().getPerson().getPhone() == null) {
                JsfUtil.addErrorMessage("Please Enter a Phone Number");
                return true;
            }
            if (getPatient().getPerson().getPhone().trim().equals("")) {
                JsfUtil.addErrorMessage("Please Enter a Phone Number");
                return true;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Area to save Patient in Package Billing", false)) {
            if (getPatient().getPerson().getArea() == null) {
                JsfUtil.addErrorMessage("Please Select Pataient Area");
                return true;
            }
            if (getPatient().getPerson().getArea().getName().trim().equals("")) {
                JsfUtil.addErrorMessage("Please Select Patient Area");
                return true;
            }
        }

        if (getLstBillEntries().isEmpty()) {

            JsfUtil.addErrorMessage("No investigations are added to the bill to settle");
            return true;
        }

        if (getPaymentMethod() == null) {
            return true;
        }

        if (getPaymentSchemeController().checkPaymentMethodError(paymentMethod, getPaymentMethodData())) {
            return true;
        }
        if (paymentMethod == PaymentMethod.Credit && creditCompany == null) {
            JsfUtil.addErrorMessage("Plase Select Credit Company");
            return true;
        }

        if (paymentMethod == PaymentMethod.PatientDeposit) {
            if (!getPatient().getHasAnAccount()) {
                JsfUtil.addErrorMessage("Patient has not account. Can't proceed with Patient Deposits");
                return true;
            }
            double creditLimitAbsolute = Math.abs(getPatient().getCreditLimit());
            double runningBalance;
            if (getPatient().getRunningBalance() != null) {
                runningBalance = getPatient().getRunningBalance();
            } else {
                runningBalance = 0.0;
            }
            double availableForPurchase = runningBalance + creditLimitAbsolute;

            if (netTotal > availableForPurchase) {
                JsfUtil.addErrorMessage("No Sufficient Patient Deposit");
                return true;
            }

        }

        if (paymentMethod == PaymentMethod.Credit) {
            if (creditCompany == null && collectingCentre == null) {
                JsfUtil.addErrorMessage("Please select Staff Member under welfare or credit company or Collecting centre.");
                return true;
            }
        }

        if (paymentMethod == PaymentMethod.Staff) {
            if (toStaff == null) {
                JsfUtil.addErrorMessage("Please select Staff Member.");
                return true;
            }

            if (toStaff.getCurrentCreditValue() + netTotal > toStaff.getCreditLimitQualified()) {
                JsfUtil.addErrorMessage("No enough Credit.");
                return true;
            }
        }

        if (paymentMethod == PaymentMethod.Staff_Welfare) {
            if (toStaff == null) {
                JsfUtil.addErrorMessage("Please select Staff Member under welfare.");
                return true;
            }
            if (Math.abs(toStaff.getAnnualWelfareUtilized()) + netTotal > toStaff.getAnnualWelfareQualified()) {
                JsfUtil.addErrorMessage("No enough credit.");
                return true;
            }

        }

        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            if (getPaymentMethodData() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            if (getPaymentMethodData().getPaymentMethodMultiple() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            if (getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            double multiplePaymentMethodTotalValue = 0.0;
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                if (paymentSchemeController.checkPaymentMethodError(cd.getPaymentMethod(), cd.getPaymentMethodData())) {
                    return true;
                }
                if (cd.getPaymentMethod().equals(PaymentMethod.Staff)) {
                    if (cd.getPaymentMethodData().getStaffCredit().getTotalValue() == 0.0 || cd.getPaymentMethodData().getStaffCredit().getToStaff() == null) {
                        JsfUtil.addErrorMessage("Please fill the Paying Amount and Staff Name");
                        return true;
                    }
                    if (cd.getPaymentMethodData().getStaffCredit().getToStaff().getCurrentCreditValue() + cd.getPaymentMethodData().getStaffCredit().getTotalValue() > cd.getPaymentMethodData().getStaffCredit().getToStaff().getCreditLimitQualified()) {
                        JsfUtil.addErrorMessage("No enough Credit.");
                        return true;
                    }
                }
                //TODO - filter only relavant value
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffCredit().getTotalValue();
            }
            double differenceOfBillTotalAndPaymentValue = netTotal - multiplePaymentMethodTotalValue;
            differenceOfBillTotalAndPaymentValue = Math.abs(differenceOfBillTotalAndPaymentValue);
            if (differenceOfBillTotalAndPaymentValue > 1.0) {
                JsfUtil.addErrorMessage("Mismatch in differences of multiple payment method total and bill total");
                return true;
            }

        }

        return false;
    }

    private void addEntry(BillItem bi) {
        if (bi == null) {
            JsfUtil.addErrorMessage("Nothing to add");
            return;
        }
        if (bi.getItem() == null) {
            JsfUtil.addErrorMessage("Please select an investigation");
            return;
        }

        BillEntry addingEntry = new BillEntry();
        addingEntry.setBillItem(bi);
        addingEntry.setLstBillComponents(getBillBean().billComponentsFromBillItem(bi));
        addingEntry.setLstBillFees(getBillBean().billFeefromBillItemPackage(bi, currentBillItem.getItem()));
        addingEntry.setLstBillSessions(getBillBean().billSessionsfromBillItem(bi));
        getLstBillEntries().add(addingEntry);
        bi.setRate(getBillBean().billItemRate(addingEntry));
        bi.setQty(1.0);
        bi.setNetValue(bi.getRate() * bi.getQty()); // Price == Rate as Qty is 1 here

        calTotals();
        if (bi.getNetValue() == 0.0) {
            JsfUtil.addErrorMessage("Please enter the rate");
            return;
        }
        //      clearBillItemValues();
        recreateBillItems();
    }

    public void addToBill() {
//        System.out.println("getCurrentBillItem = " + getCurrentBillItem());
//        System.out.println("getCurrentBillItem.item = " + getCurrentBillItem().getItem().getName());
        if (getLstBillEntries().size() > 0) {
            JsfUtil.addErrorMessage("You can not add more than on package at a time create new bill");
            return;
        }
        if (getCurrentBillItem() == null) {
            JsfUtil.addErrorMessage("Nothing to add");
            return;
        }
        if (getCurrentBillItem().getItem() == null) {
            JsfUtil.addErrorMessage("Please select an Item");
            return;
        }

        List<Item> itemList = getBillBean().itemFromPackage(currentBillItem.getItem());
        for (Item i : itemList) {
            if (i.getDepartment() == null) {
                JsfUtil.addErrorMessage("Under administration, add a Department for item " + i.getName());
                return;
            }

            BillItem tmp = new BillItem();
            tmp.setItem(i);
            addEntry(tmp);
        }
        JsfUtil.addSuccessMessage("Item Added");
    }

    public void clearBillItemValues() {
        setCurrentBillItem(null);
        recreateBillItems();
    }
    

    private void recreateBillItems() {
        //Only remove Total and BillComponenbts,Fee and Sessions. NOT bill Entries
        lstBillComponents = null;
        lstBillFees = null;
        lstBillItems = null;

        //billTotal = 0.0;
    }

    public void calTotals() {
        double tot = 0.0;
        double net = 0.0;
        double dis = 0.0;

        for (BillEntry be : getLstBillEntries()) {
            BillItem bi = be.getBillItem();
            bi.setDiscount(0.0);
            bi.setGrossValue(0.0);
            bi.setNetValue(0.0);

            for (BillFee bf : be.getLstBillFees()) {
                tot += bf.getFeeGrossValue();
                net += bf.getFeeValue();
                bf.getBillItem().setNetValue(bf.getBillItem().getNetValue() + bf.getFeeValue());
                bf.getBillItem().setGrossValue(bf.getBillItem().getGrossValue() + bf.getFeeGrossValue());
            }
        }
        setDiscount(dis);
        setTotal(net);
        setNetTotal(net);
    }

    public void feeChanged() {
        lstBillItems = null;
        getLstBillItems();
        calTotals();
    }

    public void clearBillValues() {
        setPatient(null);
        setReferredBy(null);
        setCreditCompany(null);
        setYearMonthDay(null);
        setBills(null);
        paymentMethodData = null;
        payments = null;
        setChequeBank(null);

        setCurrentBillItem(null);
        setLstBillComponents(null);
        setLstBillEntries(null);
        setLstBillFees(null);
        setStaff(null);
        setToStaff(null);
        lstBillEntries = new ArrayList<>();
        //   setForeigner(false);
        calTotals();

        setCashPaid(0.0);
        setDiscount(0.0);
        setCashBalance(0.0);
        printPreview = false;
    }

    public void prepareNewBill() {
        clearBillItemValues();
        clearBillValues();
        setPrintPreview(true);
        printPreview = false;
    }

    public void removeBillItem() {
        currentBillItem = null;
        lstBillComponents = null;
        lstBillFees = null;
        lstBillItems = null;
        lstBillEntries = null;
        setCashPaid(0.0);
        setDiscount(0.0);
        setCashBalance(0.0);
        setTotal(0.0);
        setNetTotal(0.0);
    }

    public void removeBillItem(Item itm) {
        List<Item> itemList = getBillBean().itemFromPackage(currentBillItem.getItem());
        System.out.println("itm = " + itm.getName());
        lstBillComponents = null;
        lstBillFees = null;
        lstBillItems = null;
        lstBillEntries = null;
        for (Item i : itemList) {
            System.out.println("i = " + i.getName());
            if (!i.getName().equals(itm.getName())) {
                System.out.println("i = ******" + i.getName());
                if (i.getDepartment() == null) {
                    JsfUtil.addErrorMessage("Under administration, add a Department for item " + i.getName());
                    return;
                }

                BillItem tmp = new BillItem();
                tmp.setItem(i);
                addEntry(tmp);
            }
        }
        calTotals();
    }

    public void recreateList(BillEntry r) {
        List<BillEntry> temp = new ArrayList<BillEntry>();
        for (BillEntry b : getLstBillEntries()) {
            if (b.getBillItem().getItem() != r.getBillItem().getItem()) {
                temp.add(b);
                //////System.out.println(b.getBillItem().getNetValue());
            }
        }
        lstBillEntries = temp;
        lstBillComponents = getBillBean().billComponentsFromBillEntries(lstBillEntries);
        lstBillFees = getBillBean().billFeesFromBillEntries(lstBillEntries);
    }

    @Override
    public Patient getPatient() {
        if (patient == null) {
            patient = new Patient();
            patientDetailsEditable = true;
        }
        return patient;
    }

    @Override
    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    // <editor-fold defaultstate="collapsed" desc="navigater">
    String navigateToNewOpdPackageBill(Patient patient) {
        clearBillItemValues();
        clearBillValues();
        printPreview = false;
        this.patient = patient;
        return "/opd/opd_bill_package?faces-redirect=true";
    }

    public String navigateToMedicalPakageBillingFromMenu() {
        clearBillValues();
        setPatient(getPatient());
//        appointmentController.prepereForInwardAppointPatient();
//        appointmentController.setSearchedPatient(getCurrent());
//        appointmentController.getCurrentAppointment().setPatient(getCurrent());
//        appointmentController.getCurrentBill().setPatient(getCurrent());
        return "/opd_bill_package_medical?faces-redirect=true";
    }
    //</editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Getter and Setter">
    public List<ItemLight> getOpdPackages() {
        if (opdPackages == null) {
            opdPackages = itemApplicationController.getPackages();
        }
        return opdPackages;
    }

    public void setOpdPackages(List<ItemLight> opdPackages) {
        this.opdPackages = opdPackages;
    }

    public BillSessionFacade getBillSessionFacade() {
        return billSessionFacade;
    }

    public void setBillSessionFacade(BillSessionFacade billSessionFacade) {
        this.billSessionFacade = billSessionFacade;
    }

    public ServiceSessionBean getServiceSessionBean() {
        return serviceSessionBean;
    }

    public void setServiceSessionBean(ServiceSessionBean serviceSessionBean) {
        this.serviceSessionBean = serviceSessionBean;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    private BillFacade getEjbFacade() {
        return billFacade;
    }

    private SessionController getSessionController() {
        return sessionController;
    }

    public BillPackageController() {
    }

    private BillFacade getFacade() {
        return billFacade;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
        calTotals();
    }

    public Doctor getReferredBy() {

        return referredBy;
    }

    public void setReferredBy(Doctor referredBy) {
        this.referredBy = referredBy;
    }

    public Institution getCreditCompany() {

        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public List<BillComponent> getLstBillComponents() {
        if (lstBillComponents == null) {
            lstBillComponents = getBillBean().billComponentsFromBillEntries(getLstBillEntries());
        }

        return lstBillComponents;
    }

    public void setLstBillComponents(List<BillComponent> lstBillComponents) {
        this.lstBillComponents = lstBillComponents;
    }

    public List<BillFee> getLstBillFees() {
        if (lstBillFees == null) {
            lstBillFees = getBillBean().billFeesFromBillEntries(getLstBillEntries());
        }

        return lstBillFees;
    }

    public void setLstBillFees(List<BillFee> lstBillFees) {
        this.lstBillFees = lstBillFees;
    }

    public List<BillItem> getLstBillItems() {
        if (lstBillItems == null) {
            lstBillItems = new ArrayList<BillItem>();
        }
        return lstBillItems;
    }

    public void setLstBillItems(List<BillItem> lstBillItems) {
        this.lstBillItems = lstBillItems;
    }

    public List<BillEntry> getLstBillEntries() {
        if (lstBillEntries == null) {
            lstBillEntries = new ArrayList<BillEntry>();
        }
        return lstBillEntries;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public Title[] getTitle() {
        return Title.values();
    }

    public Sex[] getSex() {
        return Sex.values();
    }

    public List<Bill> getBills() {
        if (bills == null) {
            bills = new ArrayList<Bill>();
        }
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public void setLstBillEntries(List<BillEntry> lstBillEntries) {
        this.lstBillEntries = lstBillEntries;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public double getCashPaid() {
        return cashPaid;
    }

    public void setCashPaid(double cashPaid) {
        this.cashPaid = cashPaid;
        cashBalance = cashPaid - getNetTotal();
    }

    public double getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(double cashBalance) {
        this.cashBalance = cashBalance;
    }

    public Institution getChequeBank() {

        return chequeBank;
    }

    public void setChequeBank(Institution chequeBank) {
        this.chequeBank = chequeBank;
    }

    public BillItem getCurrentBillItem() {
        if (currentBillItem == null) {
            currentBillItem = new BillItem();
        }

        return currentBillItem;
    }

    public void setCurrentBillItem(BillItem currentBillItem) {
        this.currentBillItem = currentBillItem;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;

    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public void setPatientFacade(PatientFacade patientFacade) {
        this.patientFacade = patientFacade;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;

    }

    public BillComponentFacade getBillComponentFacade() {
        return billComponentFacade;
    }

    public void setBillComponentFacade(BillComponentFacade billComponentFacade) {
        this.billComponentFacade = billComponentFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public PatientInvestigationFacade getPatientInvestigationFacade() {
        return patientInvestigationFacade;
    }

    public void setPatientInvestigationFacade(PatientInvestigationFacade patientInvestigationFacade) {
        this.patientInvestigationFacade = patientInvestigationFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;

    }

    public BillSearch getBillSearch() {
        return billSearch;
    }

    public void setBillSearch(BillSearch billSearch) {
        this.billSearch = billSearch;
    }

    public YearMonthDay getYearMonthDay() {
        if (yearMonthDay == null) {
            yearMonthDay = new YearMonthDay();
        }
        return yearMonthDay;
    }

    public void setYearMonthDay(YearMonthDay yearMonthDay) {
        this.yearMonthDay = yearMonthDay;
    }

    public Institution getReferredByInstitution() {
        return referredByInstitution;
    }

    public void setReferredByInstitution(Institution referredByInstitution) {
        this.referredByInstitution = referredByInstitution;
    }

    public String getReferralId() {
        return referralId;
    }

    public void setReferralId(String referralId) {
        this.referralId = referralId;
    }

    public Institution getCollectingCentre() {
        return collectingCentre;
    }

    public void setCollectingCentre(Institution collectingCentre) {
        this.collectingCentre = collectingCentre;
    }

    public ItemLight getItemLight() {
        if (getCurrentBillItem().getItem() != null) {
            this.itemLight = new ItemLight(getCurrentBillItem().getItem());
        }
        return itemLight;
    }

    public void setItemLight(ItemLight itemLight) {
        this.itemLight = itemLight;
        if (this.itemLight != null) {
            getCurrentBillItem().setItem(itemController.findItem(this.itemLight.getId()));
        }
    }

    public void feeChangeListener(BillFee bf) {
        if (bf.getFeeGrossValue() == null) {
            bf.setFeeGrossValue(0.0);
//            return;
        }

        lstBillItems = null;
        getLstBillItems();
        bf.setTmpChangedValue(bf.getFeeGrossValue());
        calTotals();
    }

    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController paymentSchemeController) {
        this.paymentSchemeController = paymentSchemeController;
    }

    @Override
    public boolean isPatientDetailsEditable() {
        return patientDetailsEditable;
    }

    @Override
    public void setPatientDetailsEditable(boolean patientDetailsEditable) {
        this.patientDetailsEditable = patientDetailsEditable;
    }

    public Bill getBatchBill() {
        return batchBill;
    }

    public void setBatchBill(Bill batchBill) {
        this.batchBill = batchBill;
    }

    public List<Payment> getPayments() {
        if (payments == null) {
            payments = new ArrayList<>();
        }
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }

}
