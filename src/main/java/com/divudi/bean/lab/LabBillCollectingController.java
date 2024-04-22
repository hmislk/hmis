/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.lab;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.Sex;
import com.divudi.data.Title;
import com.divudi.data.dataStructure.YearMonthDay;
import com.divudi.ejb.BillNumberGenerator;

import com.divudi.entity.Bill;
import com.divudi.entity.BillComponent;
import com.divudi.entity.BillEntry;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Doctor;
import com.divudi.entity.Institution;
import com.divudi.entity.Patient;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.Person;
import com.divudi.entity.Staff;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.facade.PersonFacade;
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
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class LabBillCollectingController implements Serializable {

    BilledBill current;
    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    private boolean printPreview;
    private String patientTabId = "tabNewPt";
    private YearMonthDay yearMonthDay;
    private PaymentScheme paymentScheme;
    private Patient newPatient;
    private Patient searchedPatient;
    private Doctor referredBy;
    //  private Institution creditCompany;
    private Institution collectingCentre;
    private Staff staff;
    private double total;
    private double discount;
    private double netTotal;
    double vatPlusNetTotal;
    double vat;
    private double cashPaid;
    private double cashBalance;
    private String creditCardRefNo;
    private String chequeRefNo;
    private Institution chequeBank;
    private BillItem currentBillItem;
    //Bill Items
    private List<BillComponent> lstBillComponents;
    private List<BillFee> lstBillFees;
    private List<BillItem> lstBillItems;
    private List<BillEntry> lstBillEntries;
    private Integer index;
    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;
    @Inject
    private BillBeanController billBean;

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
    //Temprory Variable
    private Patient tmpPatient;

    public void dateChangeListen() {
        getNewPatient().getPerson().setDob(getCommonFunctions().guessDob(yearMonthDay));

    }

    public BilledBill getCurrent() {
        return current;
    }

    public void setCurrent(BilledBill current) {
        this.current = current;
    }

    public Title[] getTitle() {
        return Title.values();
    }

    public Sex[] getSex() {
        return Sex.values();
    }

    private void clearBillValues() {
        setNewPatient(null);
        setSearchedPatient(null);
        setReferredBy(null);
//        setCreditCompany(null);
        setYearMonthDay(null);
        setChequeBank(null);
        setPaymentScheme(null);
        setChequeRefNo("");
        setCreditCardRefNo("");
        currentBillItem = null;
        setLstBillComponents(null);
        setLstBillEntries(null);
        setLstBillFees(null);
        setStaff(null);
        lstBillEntries = new ArrayList<>();
        calTotals();
        setCashPaid(0.0);
        setDiscount(0.0);
        setCashBalance(0.0);
        patientTabId = "tabNewPt";
        current = new BilledBill();
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    private void savePatient() {
        if (getPatientTabId().equals("tabNewPt")) {
            getNewPatient().setCreater(getSessionController().getLoggedUser());
            getNewPatient().setCreatedAt(new Date());

            getNewPatient().getPerson().setCreater(getSessionController().getLoggedUser());
            getNewPatient().getPerson().setCreatedAt(new Date());

            getPersonFacade().create(getNewPatient().getPerson());
            getPatientFacade().create(getNewPatient());
            tmpPatient = getNewPatient();

        } else if (getPatientTabId().equals("tabSearchPt")) {
            tmpPatient = getSearchedPatient();
        }
    }

    public void putToBills() {
        Set<Department> billDepts = new HashSet<Department>();
        //List<Bill> bills = new ArrayList<Bill>();
        for (BillEntry e : lstBillEntries) {
            billDepts.add(e.getBillItem().getItem().getDepartment());
        }
        for (Department d : billDepts) {
            BilledBill myBill = new BilledBill();
            saveBill(d, myBill);
            bills.add(myBill);
            List<BillItem> list = new ArrayList<>();
            for (BillEntry e : lstBillEntries) {
                if (Objects.equals(e.getBillItem().getItem().getDepartment().getId(), d.getId())) {
                    //e.setBill(myBill);
                    myBill.getBillItems().add(saveBillItem(myBill, e));
                    calculateBillItem(myBill, e);
                }

                billFacade.edit(myBill);
            }
            //set Bill Item Properties like bill TOtal, Discount
            //Save Bill
        }

//        for (Bill myBill : bills) {
//            for (BillEntry e : lstBillEntries) {
//                if (e.getBill() == myBill) {
//                    ///Save Bill Items
//                }
//            }
//            // Save Bill Changes
//        }
    }

    private int checkDepartment() {

        int c = 0;
        Department tdep = new Department();
        tdep.setId(0L);
        // //////// // System.out.println("Check Dept 1 " + c);
        //Createa a list of bills
        for (BillEntry be : getLstBillEntries()) {
            if (be.getBillItem().getItem().getDepartment().getId() != tdep.getId()) {
                tdep = be.getBillItem().getItem().getDepartment();
                c++;
                //  //////// // System.out.println("Check Dept  " + be.getBillItem().getItem().getDepartment().getName());
            }
        }
        //////// // System.out.println("Check Dept 4 " + c);

        return c;
    }

    List<Bill> bills;

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public void settleBill() {
        if (errorCheck()) {
            return;
        }

        bills = new ArrayList<>();

        savePatient();
        if (checkDepartment() == 1) {
            current = new BilledBill();
            Bill b = saveBill(lstBillEntries.get(0).getBillItem().getItem().getDepartment(), current);

            b.setBillItems(saveBillItems(b));
            billFacade.edit(b);
            calculateBillItems(b);
            bills.add(b);
        } else {
            putToBills();
        }

        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;
    }

    private void calculateBillItem(Bill bill, BillEntry e) {
        double s = 0.0;
        double i = 0.0;
        double g = 0.0;
        for (BillFee bf : e.getLstBillFees()) {
            g = g + bf.getFee().getFee();
            if (bf.getFee().getStaff() == null) {
                i = i + bf.getFeeValue();
            } else {
                s = s + bf.getFeeValue();
            }
            if (bf.getId() == null || bf.getId() == 0) {
                getBillFeeFacade().create(bf);
            } else {
                getBillFeeFacade().edit(bf);
            }
        }

        bill.setStaffFee(s);
        bill.setPerformInstitutionFee(i);
        bill.setTotal(g);
        bill.setNetTotal(g);
        getFacade().edit(bill);
    }

    private void calculateBillItems(Bill bill) {
        double s = 0.0;
        double i = 0.0;
        double g = 0.0;
        for (BillEntry e : getLstBillEntries()) {
            for (BillFee bf : e.getLstBillFees()) {
                g = g + bf.getFee().getFee();
                if (bf.getFee().getStaff() == null) {
                    i = i + bf.getFeeValue();
                } else {
                    s = s + bf.getFeeValue();
                }
                if (bf.getId() == null || bf.getId() == 0) {
                    getBillFeeFacade().create(bf);
                } else {
                    getBillFeeFacade().edit(bf);
                }
            }
        }
        bill.setStaffFee(s);
        bill.setPerformInstitutionFee(i);
        bill.setTotal(g);
        bill.setNetTotal(g);
        getFacade().edit(bill);
    }

    private void savePatientInvestigation(BillEntry e, BillComponent bc) {
        PatientInvestigation ptIx = new PatientInvestigation();
        ptIx.setBillItem(e.getBillItem());
        ptIx.setBillComponent(bc);
        ptIx.setPackege(bc.getPackege());
        ptIx.setApproved(Boolean.FALSE);
        ptIx.setCancelled(Boolean.FALSE);
        ptIx.setCollected(Boolean.FALSE);
        ptIx.setDataEntered(Boolean.FALSE);
        ptIx.setInvestigation((Investigation) bc.getItem());
        ptIx.setOutsourced(Boolean.FALSE);
        ptIx.setPatient(tmpPatient);
//        ptIx.setEncounter(tmpPatientEncounter);
        ptIx.setPerformed(Boolean.FALSE);
        ptIx.setPrinted(Boolean.FALSE);
        ptIx.setPrinted(Boolean.FALSE);
        ptIx.setReceived(Boolean.FALSE);

        ptIx.setReceiveDepartment(e.getBillItem().getItem().getDepartment());
        ptIx.setApproveDepartment(e.getBillItem().getItem().getDepartment());
        ptIx.setDataEntryDepartment(e.getBillItem().getItem().getDepartment());
        ptIx.setPrintingDepartment(e.getBillItem().getItem().getDepartment());
        ptIx.setPerformDepartment(e.getBillItem().getItem().getDepartment());

        if (e.getBillItem().getItem() == null) {
            JsfUtil.addErrorMessage("No Bill Item Selected");
        } else if (e.getBillItem().getItem().getDepartment() == null) {
            JsfUtil.addErrorMessage("Under administration, add a Department for this investigation " + e.getBillItem().getItem().getName());
        } else if (e.getBillItem().getItem().getDepartment().getInstitution() == null) {
            JsfUtil.addErrorMessage("Under administration, add an Institution for the department " + e.getBillItem().getItem().getDepartment());
        } else if (e.getBillItem().getItem().getDepartment().getInstitution() != getSessionController().getLoggedUser().getInstitution()) {
            ptIx.setOutsourcedInstitution(e.getBillItem().getItem().getInstitution());
        }

        ptIx.setRetired(false);
        getPatientInvestigationFacade().create(ptIx);

    }

    private void saveBillComponent(BillEntry e, Bill b) {
        for (BillComponent bc : e.getLstBillComponents()) {
            bc.setBill(b);
            getBillComponentFacade().create(bc);
            if (bc.getItem() instanceof Investigation) {
                savePatientInvestigation(e, bc);
            }

        }
    }

    private void saveBillFee(BillEntry e, Bill b) {
        for (BillFee bf : e.getLstBillFees()) {
            bf.setBill(b);
            getBillFeeFacade().create(bf);

        }
    }

    private BillItem saveBillItem(Bill b, BillEntry e) {
        //   BillItem temBi = e.getBillItem();
        e.getBillItem().setCreatedAt(new Date());
        e.getBillItem().setCreater(getSessionController().getLoggedUser());
        //  e.getBillItem().setDeptId(e.getBillItem().getItem().getDepartment().getId());
        e.getBillItem().setBill(b);
        getBillItemFacade().create(e.getBillItem());
        ////////// // System.out.println("Saving Bill Item : " + temBi.getItem().getName());

        saveBillComponent(e, b);
        saveBillFee(e, b);

        return e.getBillItem();
    }

    private List<BillItem> saveBillItems(Bill b) {
        List<BillItem> billItems = new ArrayList<>();
        for (BillEntry e : getLstBillEntries()) {
            // BillItem temBi = e.getBillItem();
            e.getBillItem().setCreatedAt(new Date());
            e.getBillItem().setCreater(getSessionController().getLoggedUser());
            e.getBillItem().setBill(b);
            getBillItemFacade().create(e.getBillItem());
            ////////// // System.out.println("Saving Bill Item : " + e.getBillItem().getItem().getName());

            saveBillComponent(e, b);
            saveBillFee(e, b);

            billItems.add(e.getBillItem());
        }
        return billItems;
    }

    PaymentMethod paymentMethod;

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    private Bill saveBill(Department bt, BilledBill temp) {

        //getCurrent().setCashBalance(cashBalance); 
        //getCurrent().setCashPaid(cashPaid);
        //  temp.setBillType(bt);
        temp.setBillType(BillType.LabBill);

        temp.setDepartment(getSessionController().getLoggedUser().getDepartment());
        temp.setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        temp.setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        temp.setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        temp.setToDepartment(bt);
        temp.setToInstitution(bt.getInstitution());

        temp.setCollectingCentre(collectingCentre);
        //  temp.setCreditCompany(creditCompany);

        temp.setStaff(staff);
        temp.setReferredBy(referredBy);
        temp.setCreditCardRefNo(creditCardRefNo);
        temp.setBank(chequeBank);
        temp.setChequeRefNo(chequeRefNo);

        temp.setBillDate(new Date());
        temp.setBillTime(new Date());
        temp.setPatient(tmpPatient);
//        temp.setPatientEncounter(patientEncounter);
        temp.setPaymentScheme(getPaymentScheme());
        temp.setPaymentMethod(paymentMethod);
        temp.setCreatedAt(new Date());
        temp.setCreater(getSessionController().getLoggedUser());
        temp.setDeptId(getBillNumberBean().departmentBillNumberGenerator(temp.getDepartment(), temp.getBillType(), BillClassType.BilledBill, BillNumberSuffix.NONE));
        temp.setInsId(getBillNumberBean().institutionBillNumberGenerator(temp.getInstitution(), temp.getBillType(), BillClassType.BilledBill, BillNumberSuffix.NONE));
        if (temp.getId() == null) {
            getFacade().create(temp);
        }
        return temp;

    }

    private boolean errorCheck() {
        if (getPatientTabId().toString().equals("tabNewPt")) {

            if (getNewPatient().getPerson().getName() == null || getNewPatient().getPerson().getName().trim().equals("") || getNewPatient().getPerson().getSex() == null || getNewPatient().getPerson().getDob() == null) {
                JsfUtil.addErrorMessage("Can not bill without Patient Name, Age or Sex.");
                return true;
            }

            if (!com.divudi.java.CommonFunctions.checkAgeSex(getNewPatient().getPerson().getDob(), getNewPatient().getPerson().getSex(), getNewPatient().getPerson().getTitle())) {
                JsfUtil.addErrorMessage("Check Title,Age,Sex");
                return true;
            }

            if (getNewPatient().getPerson().getPhone().length() < 1) {
                JsfUtil.addErrorMessage("Phone Number is Required it should be fill");
                return true;
            }

        }
        if (getLstBillEntries().isEmpty()) {

            JsfUtil.addErrorMessage("No investigations are added to the bill to settle");
            return true;
        }

        if (collectingCentre == null) {
            JsfUtil.addErrorMessage("Please Select Collecting Centre or Credit company");
            return true;

        }

        return false;
    }

    public void addToBill() {

        if (getCurrentBillItem() == null) {
            JsfUtil.addErrorMessage("Nothing to add");
            return;
        }
        if (getCurrentBillItem().getItem() == null) {
            JsfUtil.addErrorMessage("Please select an investigation");
            return;
        }

//        getCurrent().setDepartment(getSessionController().getLoggedUser().getDepartment());
//        getCurrent().setInstitution(getSessionController().getLoggedUser().getInstitution());
//        getCurrentBillItem().setBill(getCurrent());
        BillEntry addingEntry = new BillEntry();
        addingEntry.setBillItem(getCurrentBillItem());
        addingEntry.setLstBillComponents(getBillBean().billComponentsFromBillItem(getCurrentBillItem()));
        addingEntry.setLstBillFees(getBillBean().billFeefromBillItem(getCurrentBillItem()));
        addingEntry.setLstBillSessions(getBillBean().billSessionsfromBillItem(getCurrentBillItem()));
        getLstBillEntries().add(addingEntry);
        getCurrentBillItem().setRate(getBillBean().billItemRate(addingEntry));
        getCurrentBillItem().setQty(1.0);
        getCurrentBillItem().setNetValue(getCurrentBillItem().getRate() * getCurrentBillItem().getQty()); // Price == Rate as Qty is 1 here

        calTotals();
        if (getCurrentBillItem().getNetValue() == 0.0) {
            JsfUtil.addErrorMessage("Please enter the rate");
            return;
        }
        clearBillItemValues();
        //JsfUtil.addSuccessMessage("Item Added");
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
        double dis = 0.0;
        double v = 0.0;

        for (BillEntry be : getLstBillEntries()) {
            BillItem bi = be.getBillItem();
            bi.setDiscount(0.0);
            bi.setGrossValue(0.0);
            bi.setNetValue(0.0);
            bi.setVat(0.0);
            bi.setVatPlusNetValue(0.0);

            for (BillFee bf : be.getLstBillFees()) {
                if (bf.getBillItem().getItem().isUserChangable() && bf.getBillItem().getItem().isDiscountAllowed() != true) {
                    //////// // System.out.println("Total is " + tot);
                    //       //////// // System.out.println("Bill Fee value is " + bf.getFeeValue());
                    tot += bf.getFeeValue();
                    //////// // System.out.println("After addition is " + tot);
                    bf.getBillItem().setNetValue(bf.getBillItem().getNetValue() + bf.getFeeValue());
                    bf.getBillItem().setGrossValue(bf.getBillItem().getGrossValue() + bf.getFeeValue());

                } else //////// // System.out.println("12");
                {
                    if (bf.getBillItem().getItem().isDiscountAllowed() != null && bf.getBillItem().getItem().isDiscountAllowed() == true) {
                        if (getPaymentScheme() == null) {
                            bf.setFeeValue(bf.getFee().getFee());
                            dis = 0.0;
                            bf.getBillItem().setDiscount(0.0);
                        } else {
                            //    bf.setFeeValueBoolean(bf.getFee().getFee() / 100 * (100 - getPaymentScheme().getDiscountPercent()));
                            //     dis += (bf.getFee().getFee() / 100 * (getPaymentScheme().getDiscountPercent()));
                            //              bf.getBillItem().setDiscount(bf.getBillItem().getDiscount() + bf.getFee().getFee() / 100 * (getPaymentScheme().getDiscountPercent()));
                        }
                        tot += bf.getFee().getFee();
                        bf.getBillItem().setGrossValue(bf.getBillItem().getGrossValue() + bf.getFee().getFee());

                        bf.getBillItem().setNetValue(bf.getBillItem().getNetValue() + bf.getBillItem().getGrossValue() - bf.getBillItem().getDiscount());
                    } else {
                        //////// // System.out.println("13");
                        tot = tot + bf.getFeeValue();
                        bf.setFeeValue(bf.getFee().getFee());
                        bf.getBillItem().setGrossValue(bf.getBillItem().getGrossValue() + bf.getFee().getFee());
                        bf.getBillItem().setNetValue(bf.getBillItem().getNetValue() + bf.getFee().getFee());
                    }
                }

                bf.getBillItem().setVat(bf.getBillItem().getVat() + bf.getFeeVat());
                v=v+bf.getFeeVat();
            }
        }
        setDiscount(dis);
        setTotal(tot);
        setNetTotal(tot - dis);
        setVat(v);
        setVatPlusNetTotal(v+netTotal);

    }

    public void feeChanged() {
        lstBillItems = null;
        getLstBillItems();
        calTotals();
    }

    public void prepareNewBill() {
        clearBillItemValues();
        clearBillValues();
        printPreview = false;
        lstBillEntries = null;

    }

    public void removeBillItem() {

        //TODO: Need to add Logic
        //////// // System.out.println(getIndex());
        if (getIndex() != null) {
            boolean remove;
            BillEntry temp = getLstBillEntries().get(getIndex());
            //////// // System.out.println("Removed Item:" + temp.getBillItem().getNetValue());
            recreateList(temp);
            // remove = getLstBillEntries().remove(getIndex());

            //  getLstBillEntries().remove(index);
            ////////// // System.out.println("Is Removed:" + remove);
            calTotals();

        }

    }

    public void recreateList(BillEntry r) {
        List<BillEntry> temp = new ArrayList<BillEntry>();
        for (BillEntry b : getLstBillEntries()) {
            if (b.getBillItem().getItem() != r.getBillItem().getItem()) {
                temp.add(b);
                //////// // System.out.println(b.getBillItem().getNetValue());
            }
        }
        lstBillEntries = temp;
        lstBillComponents = getBillBean().billComponentsFromBillEntries(lstBillEntries);
        lstBillFees = getBillBean().billFeesFromBillEntries(lstBillEntries);
    }

    public void onTabChange(TabChangeEvent event) {
        setPatientTabId(event.getTab().getId());

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

    public LabBillCollectingController() {
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
    }

    public String getPatientTabId() {
        return patientTabId;
    }

    public void setPatientTabId(String patientTabId) {
        this.patientTabId = patientTabId;
    }

    public Patient getNewPatient() {
        if (newPatient == null) {
            newPatient = new Patient();
            Person p = new Person();

            newPatient.setPerson(p);
        }
        return newPatient;
    }

    public void setNewPatient(Patient newPatient) {
        this.newPatient = newPatient;
    }

    public Patient getSearchedPatient() {
        return searchedPatient;
    }

    public void setSearchedPatient(Patient searchedPatient) {
        this.searchedPatient = searchedPatient;
    }

    public Doctor getReferredBy() {
        return referredBy;
    }

    public void setReferredBy(Doctor referredBy) {
        this.referredBy = referredBy;
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
    }

    public double getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(double cashBalance) {
        this.cashBalance = cashBalance;
    }

    public String getCreditCardRefNo() {
        return creditCardRefNo;
    }

    public void setCreditCardRefNo(String creditCardRefNo) {
        this.creditCardRefNo = creditCardRefNo;
    }

    public String getChequeRefNo() {
        return chequeRefNo;
    }

    public void setChequeRefNo(String chequeRefNo) {
        this.chequeRefNo = chequeRefNo;
    }

    public Institution getChequeBank() {
        if (chequeBank == null) {
            chequeBank = new Institution();
        }

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

    private Patient getTmpPatient() {
        return tmpPatient;
    }

    public void setTmpPatient(Patient tmpPatient) {
        this.tmpPatient = tmpPatient;
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

    public Institution getCollectingCentre() {
        return collectingCentre;
    }

    public void setCollectingCentre(Institution collectingCentre) {
        this.collectingCentre = collectingCentre;
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

    public double getVat() {
        return vat;
    }

    public void setVat(double vat) {
        this.vat = vat;
    }

    public double getVatPlusNetTotal() {
        return vatPlusNetTotal;
    }

    public void setVatPlusNetTotal(double vatPlusNetTotal) {
        this.vatPlusNetTotal = vatPlusNetTotal;
    }

}
