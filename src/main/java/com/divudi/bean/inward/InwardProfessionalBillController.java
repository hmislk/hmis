/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.inward.PatientEncounterComponentType;
import com.divudi.data.inward.SurgeryBillType;
import com.divudi.ejb.BillNumberGenerator;

import com.divudi.entity.Bill;
import com.divudi.entity.BillEntry;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Item;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.inward.EncounterComponent;
import com.divudi.facade.BillEntryFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.EncounterComponentFacade;
import com.divudi.facade.FeeFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * formatics)
 */
@Named
@SessionScoped
public class InwardProfessionalBillController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    AdmissionController admissionController;
    @Inject 
    CommonController commonController;
    ////////////////////
    @EJB
    private BillFacade ejbFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    FeeFacade feeFacade;
    @EJB
    BillEntryFacade billEntryFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private BillSessionFacade billSessionFacade;
    @EJB
    ItemFacade itemFacade;
    @EJB
    private StaffFacade staffFacade;
    ////////////////////
    @Inject
    private BillBeanController billBean;
    @EJB
    BillNumberGenerator billNumberBean;

    CommonFunctions commonFunctions;
    //////////////////    
    private List<Bill> items = null;
    List<BillFee> lstBillFees;
    List<BillItem> lstBillItems;
    List<BillEntry> lstBillEntries;
    /////////////////
    String patientTabId = "tabNewPt";
    String selectText = "";
    private String ageText;
    private Speciality speciality;
    private Staff staff;
    private Bill current;
    BillEntry removeBillEntry;
    private BillFee currentBillFee;
    private double billTotal;
    double cashPaid;
    double cashBalance;
    private Integer index;
    private Date newDob;
    boolean toClearBill = false;
    boolean printPreview;
    Bill batchBill;
    EncounterComponent proEncounterComponent;
    List<EncounterComponent> proEncounterComponents;
    @EJB
    EncounterComponentFacade encounterComponentFacade;

    public List<Staff> completeItems(String qry) {
        HashMap hm = new HashMap();
        String sql;
        sql = "select c from Staff c "
                + " where c.retired=false ";

        if (getProEncounterComponent() != null && getProEncounterComponent().getBillFee() != null && getProEncounterComponent().getBillFee().getSpeciality() != null) {
            sql += " and c.speciality=:sp";
            hm.put("sp", getProEncounterComponent().getBillFee().getSpeciality());
        }
        sql += " and ((c.person.name) like :q "
                + " or (c.code) like :q )"
                + " order by c.person.name";

        hm.put("q", "%" + qry.toUpperCase() + "%");
        List<Staff> s = getStaffFacade().findByJpql(sql, hm, 20);
        ////// // System.out.println("s = " + s);
        return s;
    }

    public EncounterComponentFacade getEncounterComponentFacade() {
        return encounterComponentFacade;
    }

    public void setEncounterComponentFacade(EncounterComponentFacade encounterComponentFacade) {
        this.encounterComponentFacade = encounterComponentFacade;
    }

    public List<EncounterComponent> getProEncounterComponents() {
        if (proEncounterComponents == null) {
            proEncounterComponents = new ArrayList<>();
        }
        return proEncounterComponents;
    }

    public void setProEncounterComponents(List<EncounterComponent> proEncounterComponents) {
        this.proEncounterComponents = proEncounterComponents;
    }

    public EncounterComponent getProEncounterComponent() {
        if (proEncounterComponent == null) {
            proEncounterComponent = new EncounterComponent();
            proEncounterComponent.setBillFee(new BillFee());
        }
        return proEncounterComponent;
    }

    public void setProEncounterComponent(EncounterComponent proEncounterComponent) {
        this.proEncounterComponent = proEncounterComponent;
    }

    private boolean generalChecking() {
        if (getBatchBill().getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("Admission ?");
            return true;
        }
        if (getBatchBill().getProcedure().getItem() == null) {
            JsfUtil.addErrorMessage("Select Surgery");
            return true;
        }

        if (getBatchBill().getPatientEncounter().isPaymentFinalized()) {
            JsfUtil.addErrorMessage("Final Payment is Finalized");
            return true;
        }

        return false;

    }

    public void updateProFee(BillFee bf) {
        updateBillFee(bf);
    }

    private void updateBillItem(BillItem billItem) {
        double value = getBillBean().getTotalByBillFee(billItem);
        billItem.setNetValue(value);
        getBillItemFacade().edit(billItem);
    }

    private void updateBill(Bill bill) {
        double value = getBillBean().getTotalByBillItem(bill);
        bill.setTotal(value);
        bill.setNetTotal(value);
        getEjbFacade().edit(bill);
    }

    public void removeProEncFromList(EncounterComponent encounterComponent) {
        removeEncounterComponentFromList(encounterComponent, getProEncounterComponents());
    }

    private void removeEncounterComponentFromList(EncounterComponent encounterComponent, List<EncounterComponent> list) {
        list.remove(encounterComponent.getOrderNo());

        int index = 0;
        for (EncounterComponent ec : list) {
            ec.setOrderNo(index++);
        }

    }

    public void removeProEncFromDbase(EncounterComponent encounterComponent) {
        if (generalChecking()) {
            return;
        }

        if (encounterComponent.getBillFee().getPaidValue() != 0) {
            JsfUtil.addErrorMessage("Staff Payment Already Paid U cant Remove");
            return;
        }

        retiredEncounterComponent(encounterComponent);
        retiredBillFee(encounterComponent.getBillFee());

        updateBillItem(encounterComponent.getBillItem());
        updateBill(encounterComponent.getBillItem().getBill());
        getBillBean().updateBatchBill(getBatchBill());

    }

    private void retiredEncounterComponent(EncounterComponent encounterComponent) {
        encounterComponent.setRetired(true);
        encounterComponent.setRetiredAt(new Date());
        encounterComponent.setRetirer(getSessionController().getLoggedUser());
        getEncounterComponentFacade().edit(encounterComponent);
    }

    private void retiredBillFee(BillFee removingFee) {

        if (removingFee != null) {
            removingFee.setRetired(true);
            removingFee.setRetiredAt(new Date());
            removingFee.setRetirer(getSessionController().getLoggedUser());
            getBillFeeFacade().edit(removingFee);

        }

    }

    private void saveBill(Bill bill, BillNumberSuffix billNumberSuffix) {
        if (bill.getId() == null) {
            bill.setForwardReferenceBill(getBatchBill());
            bill.setSurgeryBillType(SurgeryBillType.ProfessionalFee);
            bill.setCreatedAt(Calendar.getInstance().getTime());
            bill.setCreater(getSessionController().getLoggedUser());
            bill.setBillDate(new Date());
            bill.setBillTime(new Date());
            bill.setPatientEncounter(getBatchBill().getPatientEncounter());
            bill.setProcedure(getBatchBill().getProcedure());
            bill.setDepartment(getSessionController().getDepartment());
            bill.setInstitution(getSessionController().getInstitution());

            bill.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), bill.getBillType(), BillClassType.BilledBill, billNumberSuffix));
            bill.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), bill.getBillType(), BillClassType.BilledBill, billNumberSuffix));

            getEjbFacade().create(bill);
        } else {
            getEjbFacade().edit(bill);
        }
    }

    private void saveBillItem(BillItem billItem, Bill bill) {
        if (billItem.getId() == null) {
            billItem.setBill(bill);
            billItem.setCreatedAt(new Date());
            billItem.setCreater(getSessionController().getLoggedUser());

            getBillItemFacade().create(billItem);
        } else {
            getBillItemFacade().edit(billItem);
        }

    }

    private void saveBillFee(BillFee bf, Bill bill, BillItem bIllItem, double value) {
        if (bf.getId() == null) {
            bf.setBill(bill);
            bf.setFee(getInwardBean().getStaffFeeForInward(getSessionController().getLoggedUser()));
            bf.setBillItem(bIllItem);
            bf.setCreatedAt(Calendar.getInstance().getTime());
            bf.setCreater(getSessionController().getLoggedUser());

            bf.setFeeAt(Calendar.getInstance().getTime());
            bf.setFeeValue(value);
            bf.setFeeGrossValue(value);
            bf.setDepartment(getSessionController().getDepartment());
            bf.setPatienEncounter(getBatchBill().getProcedure());
            bf.setPatient(getBatchBill().getPatientEncounter().getPatient());
            bf.setInstitution(getSessionController().getInstitution());

            getBillFeeFacade().create(bf);
        } else {
            getBillFeeFacade().edit(bf);
        }
    }

    private void saveEncounterComponent(BillItem billItem, EncounterComponent ec) {
        if (ec.getId() == null) {
            ec.setBillItem(billItem);
            ec.setCreatedAt(Calendar.getInstance().getTime());
            ec.setCreater(getSessionController().getLoggedUser());
            ec.setPatientEncounter(getBatchBill().getProcedure());
            if (ec.getBillFee() != null) {
                ec.setStaff(ec.getBillFee().getStaff());
            }
            getEncounterComponentFacade().create(ec);
        } else {
            getEncounterComponentFacade().edit(ec);
        }
    }

    private boolean saveProfessionalBill() {
        BillItem bItem;
        if (getCurrent().getId() == null) {
            saveBill(getCurrent(), BillNumberSuffix.INWPRO);
            bItem = new BillItem();
            saveBillItem(bItem, getCurrent());
        } else {
            getEjbFacade().edit(getCurrent());
            bItem = getBillBean().fetchFirstBillItem(getCurrent());
        }

        for (EncounterComponent ec : getProEncounterComponents()) {
            saveBillFee(ec.getBillFee(), getCurrent(), bItem, ec.getBillFee().getFeeValue());
            saveEncounterComponent(bItem, ec);
        }

        updateBillItem(bItem);
        updateBill(getCurrent());

        return false;
    }

    public void saveSurgeryProfessional() {
        if (generalChecking()) {
            return;
        }

        if (!getProEncounterComponents().isEmpty()) {
            saveProfessionalBill();
        }

        getBillBean().updateBatchBill(getBatchBill());

        JsfUtil.addSuccessMessage("Surgery Detail Successfull Updated");

        //    makeNull();
    }

    private void updateBillFee(BillFee bf) {
        getBillFeeFacade().edit(bf);
        updateBillItem(bf.getBillItem());
        updateBill(bf.getBill());
        getBillBean().updateBatchBill(getBatchBill());
    }

    public void addProfessionalFee() {
        if (generalChecking()) {
            return;
        }

        if (getProEncounterComponent().getBillFee().getStaff() == null) {
            JsfUtil.addErrorMessage("Select Staff ");
            return;
        }

        if (getProEncounterComponent().getPatientEncounterComponentType() == PatientEncounterComponentType.Performed_By) {
            getBatchBill().setStaff(getProEncounterComponent().getBillFee().getStaff());
        }

        proEncounterComponent.setPatientEncounter(getBatchBill().getPatientEncounter());
        proEncounterComponent.setChildEncounter(getBatchBill().getProcedure());
        proEncounterComponent.setOrderNo(getProEncounterComponents().size() + 1);
        getProEncounterComponents().add(proEncounterComponent);

        saveSurgeryProfessional();

        proEncounterComponent = null;
    }

    public Bill getBatchBill() {
        return batchBill;
    }

    public void setBatchBill(Bill batchBill) {
        this.batchBill = batchBill;
    }

    public List<Staff> completeStaff(String query) {
        List<Staff> suggestions;
        String sql;
        HashMap hm = new HashMap();

        if (getCurrentBillFee() != null && getCurrentBillFee().getSpeciality() != null) {
            sql = " select p from Staff p where p.retired=false and "
                    + " ((p.person.name) like :q "
                    + " or  (p.code) like :q  ) "
                    + " and p.speciality=:spe order by p.person.name";
            hm.put("spe", getCurrentBillFee().getSpeciality());
        } else {
            sql = " select p from Staff p where p.retired=false and "
                    + " ((p.person.name) "
                    + " like :q or  (p.code) like :q "
                    + " ) order by p.person.name";
        }
        hm.put("q", "%" + query.toUpperCase() + "%");
        suggestions = getStaffFacade().findByJpql(sql, hm, 20);

        return suggestions;
    }

    public boolean isToClearBill() {
        return toClearBill;
    }

    public void setToClearBill(boolean toClearBill) {
        this.toClearBill = toClearBill;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public void updateFees(AjaxBehaviorEvent event) {
    }

    public double getCashPaid() {
        return cashPaid;
    }

    public void setCashPaid(double cashPaid) {
        this.cashPaid = cashPaid;
        cashBalance = cashPaid - current.getTotal();
    }

    public double getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(double cashBalance) {
        this.cashBalance = cashBalance;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public BillEntry getRemoveBillEntry() {
        return removeBillEntry;
    }

    public void setRemoveBillEntry(BillEntry removeBillEntry) {
        this.removeBillEntry = removeBillEntry;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public AdmissionController getAdmissionController() {
        return admissionController;
    }

    public List<Item> completeItem(String qry) {
        List<Item> completeItems = getItemFacade().findByJpql("select c from Item c where c.retired=false and (type(c) = Service or type(c) = Packege ) and (c.name) like '%" + qry.toUpperCase() + "%' order by c.name");
        return completeItems;
    }

    public void setAdmissionController(AdmissionController admissionController) {
        this.admissionController = admissionController;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public BillEntryFacade getBillEntryFacade() {
        return billEntryFacade;
    }
    
  

    public void setBillEntryFacade(BillEntryFacade billEntryFacade) {
        this.billEntryFacade = billEntryFacade;
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

    public List<BillFee> getLstBillFees() {
        if (lstBillFees == null) {
            // lstBillFees = getBillBean().billFeesFromBillEntries(getLstBillEntries());
            lstBillFees = new ArrayList<>();
        }
        return lstBillFees;
    }

    public void setLstBillFees(List<BillFee> lstBillFees) {
        this.lstBillFees = lstBillFees;
    }

    public List<BillItem> getLstBillItems() {
        if (lstBillItems == null) {
            lstBillItems = getBillBean().billItemsFromBillEntries(getLstBillEntries());
        }
        return lstBillItems;
    }

    public void setLstBillItems(List<BillItem> lstBillItems) {
        this.lstBillItems = lstBillItems;
    }

    public String getPatientTabId() {
        return patientTabId;
    }

    public void setPatientTabId(String patientTabId) {
        this.patientTabId = patientTabId;
    }

    public FeeFacade getFeeFacade() {
        return feeFacade;
    }

    public void setFeeFacade(FeeFacade feeFacade) {
        this.feeFacade = feeFacade;
    }

    private void recreateBillItems() {
        //Only remove Total and BillComponenbts,Fee and Sessions. NOT bill Entries
        lstBillFees = null;
        lstBillItems = null;

        billTotal = 0.0;
    }

    public void addToBill() {
        if (getCurrent().getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("Please Select Patient Encounter");
            return;
        }

        if (currentBillFee == null) {
            JsfUtil.addErrorMessage("Nothing to add");
            return;
        }
        
        if(currentBillFee.getSpeciality()==null){
             JsfUtil.addErrorMessage("Please select a Speciality");
                return;
        }else if (currentBillFee.getStaff()==null){
             JsfUtil.addErrorMessage("Please select a Staff");
                return;
        }else if (currentBillFee.getFeeValue()== 0.0) {
             JsfUtil.addErrorMessage("Please add fee");
                return;
        }else if (currentBillFee.getFeeAt()== null) {
             JsfUtil.addErrorMessage("Please select Date");
                return;
        }
        if (getCurrent().getId() == null) {
            getCurrent().setDepartment(getSessionController().getLoggedUser().getDepartment());
            getCurrent().setInstitution(getSessionController().getLoggedUser().getInstitution());
        }

        currentBillFee.setPatienEncounter(getCurrent().getPatientEncounter());
        currentBillFee.setOrderNo(lstBillFees.size() + 1);
        lstBillFees.add(getCurrentBillFee());

        calTotals();
        //    clearBillItemValues();

        currentBillFee = null;

        save();
        //   JsfUtil.addSuccessMessage("Fee Added");
    }

    public void feeChanged(BillFee bf) {
        lstBillItems = null;
        getLstBillItems();
        calTotals();

        getBillFeeFacade().edit(bf);
        getBillItemFacade().edit(billItem);
        getEjbFacade().edit(current);
    }

    private void calTotals() {
        double tot = 0.0;
        double dis = 0.0;
        int index = 0;
        for (BillFee bf : getLstBillFees()) {
            bf.setOrderNo(index++);
            tot += bf.getFeeValue();
        }

        getBillItem().setGrossValue(tot);
        getBillItem().setNetValue(tot);

        getCurrent().setDiscount(dis);
        getCurrent().setTotal(tot);
        getCurrent().setNetTotal(tot - dis);

    }

    public void clearBillItemValues() {
        //setCurrentBillItem(null);
        // setCurrentBillFee(null);
        recreateBillItems();
    }

    public void onTabChange(TabChangeEvent event) {
        setPatientTabId(event.getTab().getId());

    }

    private void saveBill() {
        if (getCurrent().getId() == null) {

            getCurrent().setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), getCurrent().getBillType(), BillClassType.BilledBill, BillNumberSuffix.INWPRO));
            getCurrent().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getCurrent().getBillType(), BillClassType.BilledBill, BillNumberSuffix.INWPRO));

            /////////
            getCurrent().setPatientEncounter(getCurrent().getPatientEncounter());
            getCurrent().setReferredBy(getCurrent().getReferredBy());
            getCurrent().setCollectingCentre(getCurrent().getCollectingCentre());
            getCurrent().setStaff(getCurrent().getStaff());
//        getCurrent().setTotal(bi.getFeeValue());
//        getCurrent().setNetTotal(bi.getFeeValue());
//        ////////////////

            getCurrent().setBillDate(new Date());
            getCurrent().setBillTime(new Date());
            getCurrent().setPatient(getCurrent().getPatientEncounter().getPatient());

            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());

            getFacade().create(getCurrent());
        } else {
            getFacade().edit(getCurrent());
        }

    }

    private boolean errorCheck() {
        if (getCurrent().getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("Selct Patient Encounter");
            return true;
        }

        if (lstBillFees.size() <= 0) {
            JsfUtil.addErrorMessage("Professional Fee Should Not Empty");
            return true;
        }

        return false;
    }

    public void save() {
        if (errorCheck()) {
            return;
        }

        saveBill();
        saveBillItem(getCurrent());

        for (BillFee bf : getLstBillFees()) {
            saveBillFee(getCurrent(), getBillItem(), bf);
        }

        JsfUtil.addSuccessMessage("Bill Saved");

    }

    public void selectBatchBillListener() {
        makeNullList();
        Bill fetchedBill = getBillBean().fetchByForwardBill(getBatchBill(), SurgeryBillType.ProfessionalFee);
        if (fetchedBill != null) {
            setCurrent(fetchedBill);
            List<EncounterComponent> enc = getBillBean().getEncounterComponents(getCurrent());

            if (enc != null) {
                setProEncounterComponents(enc);
            }
        } else {
            current = null;
        }

    }

    public void makeNull() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;
        current = null;
        batchBill = null;
        makeNullList();
        
        
    }

    public String navigateToAddProfessionalFeesFromMenu() {
        makeNull();
        return "/inward/inward_bill_professional?faces-redirect=true";
    }
    
    public String navigateToAddProfessionalFeesFromInpatientProfile() {
        return "/inward/inward_bill_professional?faces-redirect=true";
    }

    public String navigateToAddEstimatedProfessionalFeeFromMenu() {
        makeNull();
        return "/inward/inward_bill_professional_estimate?faces-redirect=true";
    }

    public String navigateToAddEstimatedProfessionalFeeFromInpatientProfile() {
        return "/inward/inward_bill_professional_estimate?faces-redirect=true";
    }

    
    public void makeNullList() {
        currentBillFee = null;
        billItem = null;
        lstBillEntries = null;
        lstBillFees = null;
        lstBillItems = null;
        proEncounterComponent = null;
        proEncounterComponents = null;
    }

    BillItem billItem;

    public BillItem getBillItem() {
        if (billItem == null) {
            billItem = new BillItem();
        }
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    private void saveBillItem(Bill bill) {

        if (getBillItem().getId() == null) {

            getBillItem().setBill(bill);

            getBillItem().setCreatedAt(new Date());
            getBillItem().setCreater(getSessionController().getLoggedUser());

            getBillItemFacade().create(getBillItem());

        }

    }

    @Inject
    private InwardBeanController inwardBean;

    private void saveBillFee(Bill b, BillItem bItm, BillFee bf) {
        if (bf.getId() == null) {
            bf.setBillItem(bItm);
            bf.setBill(b);
            bf.setFee(getInwardBean().getStaffFeeForInward(getSessionController().getLoggedUser()));
            if (bf.getFeeAt() == null) {
                bf.setFeeAt(new Date());
            }
            bf.setPatienEncounter(getCurrent().getPatientEncounter());
            bf.setPatient(getCurrent().getPatientEncounter().getPatient());
            bf.setCreater(getSessionController().getLoggedUser());
            bf.setCreatedAt(Calendar.getInstance().getTime());

            getBillFeeFacade().create(bf);
        }

    }

    private void clearBillValues() {
        current = null;
        toClearBill = false;

        currentBillFee = new BillFee();
        //currentBillItem = new BillItem();
        setLstBillEntries(null);
    }

    public String getSelectText() {
        return selectText;
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public BillFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(BillFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public InwardProfessionalBillController() {
    }

    public Bill getCurrent() {
        if (current == null) {
            current = new BilledBill();
            current.setBillType(BillType.InwardProfessional);
            current.setDepartment(getSessionController().getLoggedUser().getDepartment());
            current.setInstitution(getSessionController().getLoggedUser().getInstitution());
        }
        return current;
    }

    public void setCurrent(Bill current) {
        this.current = current;
    }

    private BillFacade getFacade() {
        return ejbFacade;
    }

    public void removeBillItem() {
        //TODO: Need to add Logic
        //////// // System.out.println(getIndex());
        if (getIndex() != null) {
            //   boolean remove;
            BillEntry temp = getLstBillEntries().get(getIndex());
            //////// // System.out.println("Removed Item:" + temp.getBillItem().getNetValue());
            recreateList(temp);
            // remove = getLstBillEntries().remove(getIndex());

            //  getLstBillEntries().remove(index);
            ////////// // System.out.println("Is Removed:" + remove);
            calTotals();
            //////// // System.out.println(getCurrent().getNetTotal());
        }
    }

    public void remove(BillFee bf) {
        bf.setRetiredAt(new Date());
        bf.setRetired(true);
        bf.setRetirer(getSessionController().getLoggedUser());
        getBillFeeFacade().edit(bf);

        getLstBillFees().remove(bf.getOrderNo());
        calTotals();
    }

    public void recreateList(BillEntry r) {
        List<BillEntry> temp = new ArrayList<>();
        for (BillEntry b : getLstBillEntries()) {
            if (b.getBillItem().getItem() != r.getBillItem().getItem()) {
                temp.add(b);
                //////// // System.out.println(b.getBillItem().getNetValue());
            }
        }
        lstBillEntries = temp;
        lstBillFees = getBillBean().billFeesFromBillEntries(lstBillEntries);
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public double getBillTotal() {
        if (billTotal == 0.0) {
            billTotal = getBillBean().billTotalFromBillEntries(getLstBillEntries());
        }
        return billTotal;
    }

    public void setBillTotal(double billTotal) {
        this.billTotal = billTotal;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public BillSessionFacade getBillSessionFacade() {
        return billSessionFacade;
    }

    public void setBillSessionFacade(BillSessionFacade billSessionFacade) {
        this.billSessionFacade = billSessionFacade;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public Date getNewDob() {
        return newDob;
    }

    public void setNewDob(Date newDob) {
        this.newDob = newDob;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    public String getAgeText() {
        return ageText;
    }

    public void setAgeText(String ageText) {
        this.ageText = ageText;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public BillFee getCurrentBillFee() {
        if (currentBillFee == null) {
            currentBillFee = new BillFee();

        }

        return currentBillFee;
    }

    public void setCurrentBillFee(BillFee currentBillFee) {
        this.currentBillFee = currentBillFee;

    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    
}
