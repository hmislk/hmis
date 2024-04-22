/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.dataStructure.DepartmentBillItems;
import com.divudi.data.inward.PatientEncounterComponentType;
import com.divudi.data.inward.SurgeryBillType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.PatientItem;
import com.divudi.entity.PreBill;
import com.divudi.entity.RefundBill;
import com.divudi.entity.inward.EncounterComponent;
import com.divudi.entity.inward.TimedItem;
import com.divudi.entity.inward.TimedItemFee;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.EncounterComponentFacade;
import com.divudi.facade.PatientEncounterFacade;
import com.divudi.facade.PatientItemFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class SurgeryBillController implements Serializable {

    private Bill surgeryBill;
    private Bill professionalBill;
    private EncounterComponent proEncounterComponent;
    //////////
    private List<EncounterComponent> proEncounterComponents;
    private List<EncounterComponent> timedEncounterComponents;
    private List<DepartmentBillItems> departmentBillItems;
    //////

    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private EncounterComponentFacade encounterComponentFacade;
    @EJB
    private PatientItemFacade patientItemFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private PharmacyBean pharmacyBean;

    //////
    @Inject
    private SessionController sessionController;
    @Inject
    InwardTimedItemController inwardTimedItemController;
    @Inject
    CommonController commonController;
    @Inject
    private BillBeanController billBean;
    @Inject
    private InwardBeanController inwardBean;
    @Inject
    BhtSummeryController bhtSummeryController;

    public InwardTimedItemController getInwardTimedItemController() {
        return inwardTimedItemController;
    }

    public void setInwardTimedItemController(InwardTimedItemController inwardTimedItemController) {
        this.inwardTimedItemController = inwardTimedItemController;
    }

    public void updateProFee(BillFee bf) {
        updateBillFee(bf);
    }

    public void updateService(BillFee bf) {
        if (generalChecking()) {
            return;
        }

        updateBillFee(bf);
    }

    public String navigateToAddSurgeriesFromAdmissionProfile() {
        PatientEncounter pe1 = getSurgeryBill().getPatientEncounter();
        resetSurgeryBillValues();
        getSurgeryBill().setPatientEncounter(pe1);

        return "/theater/inward_bill_surgery?faces-redirect=true";
    }

    public String navigateToAddSurgeriesFromMenu() {
        resetSurgeryBillValues();
        return "/theater/inward_bill_surgery?faces-redirect=true";
    }

    private void updateBillFee(BillFee bf) {
        getBillFeeFacade().edit(bf);
        updateBillItem(bf.getBillItem());
        updateBill(bf.getBill());
        getBillBean().updateBatchBill(getSurgeryBill());
    }

    public void removeTimeService(PatientItem patientItem) {
        if (patientItem != null) {
            patientItem.setRetirer(getSessionController().getLoggedUser());
            patientItem.setRetiredAt(new Date());
            patientItem.setRetired(true);
            getPatientItemFacade().edit(patientItem);
        }
    }

    public void removeProEncFromList(EncounterComponent encounterComponent) {
        removeEncounterComponentFromList(encounterComponent, getProEncounterComponents());
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
        getBillBean().updateBatchBill(getSurgeryBill());

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

            PatientItem patientItem = removingFee.getPatientItem();
            if (patientItem != null) {
                patientItem.setRetirer(getSessionController().getLoggedUser());
                patientItem.setRetiredAt(new Date());
                patientItem.setRetired(true);
                getPatientItemFacade().edit(patientItem);
            }
        }

    }

    private void retiredBillItem(BillItem billItem) {

        if (billItem != null) {
            billItem.setRetired(true);
            billItem.setRetiredAt(new Date());
            billItem.setRetirer(getSessionController().getLoggedUser());
            getBillItemFacade().edit(billItem);
        }

    }

    public void removeServiceEncFromDbase(EncounterComponent encounterComponent) {
        if (generalChecking()) {
            return;
        }

        retiredEncounterComponent(encounterComponent);
        retiredBillItem(encounterComponent.getBillItem());

        for (BillFee bf : getBillFees(encounterComponent.getBillItem())) {
            retiredBillFee(bf);
        }

        updateBillItem(encounterComponent.getBillItem());
        updateBill(encounterComponent.getBillItem().getBill());
        getBillBean().updateBatchBill(getSurgeryBill());

    }

    public List<BillFee> getBillFees(BillItem billItem) {
        String sql = "Select bf from BillFee bf where retired=false and "
                + " bf.billItem=:bItm ";
        HashMap hm = new HashMap();
        hm.put("bItm", billItem);
        List<BillFee> lst = getBillFeeFacade().findByJpql(sql, hm);

        if (lst.isEmpty()) {
            return new ArrayList<>();
        }

        return lst;
    }

    private void removeEncounterComponentFromList(EncounterComponent encounterComponent, List<EncounterComponent> list) {
        list.remove(encounterComponent.getOrderNo());

        int index = 0;
        for (EncounterComponent ec : list) {
            ec.setOrderNo(index++);
        }

    }

    public void resetSurgeryBillValues() {
        surgeryBill = null;
        professionalBill = null;
        proEncounterComponent = null;
        proEncounterComponents = null;
        /////////////
        timedEncounterComponents = null;
        departmentBillItems = null;
        pharmacyIssues = null;

    }

    public void saveProcedure() {

        PatientEncounter procedure = getSurgeryBill().getProcedure();

        if (procedure.getId() == null || procedure.getId() == 0) {
            procedure.setParentEncounter(getSurgeryBill().getPatientEncounter());
            procedure.setCreatedAt(new Date());
            procedure.setCreater(getSessionController().getLoggedUser());

            getPatientEncounterFacade().create(procedure);
        } else {
            getPatientEncounterFacade().edit(procedure);
        }

    }

    private void saveSurgeryBill() {
        if (getSurgeryBill().getId() == null) {
            getSurgeryBill().setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.SurgeryBill, BillClassType.BilledBill, BillNumberSuffix.SURG));
            getSurgeryBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.SurgeryBill, BillClassType.BilledBill, BillNumberSuffix.SURG));

            getSurgeryBill().setInstitution(getSessionController().getInstitution());
            getSurgeryBill().setDepartment(getSessionController().getDepartment());
            getSurgeryBill().setFromDepartment(getSessionController().getDepartment());
            getSurgeryBill().setCreatedAt(Calendar.getInstance().getTime());
            getSurgeryBill().setCreater(getSessionController().getLoggedUser());

            getBillFacade().create(getSurgeryBill());
        } else {
            getBillFacade().edit(getSurgeryBill());
        }
    }

    private void saveBill(Bill bill, BillNumberSuffix billNumberSuffix) {
        if (bill.getId() == null) {
            bill.setForwardReferenceBill(getSurgeryBill());
            bill.setCreatedAt(Calendar.getInstance().getTime());
            bill.setCreater(getSessionController().getLoggedUser());
            bill.setPatientEncounter(getSurgeryBill().getPatientEncounter());
            bill.setProcedure(getSurgeryBill().getProcedure());
            bill.setDepartment(getSessionController().getDepartment());
            bill.setInstitution(getSessionController().getInstitution());

            bill.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), bill.getBillType(), BillClassType.BilledBill, billNumberSuffix));
            bill.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), bill.getBillType(), BillClassType.BilledBill, billNumberSuffix));

            getBillFacade().create(bill);
        } else {
            getBillFacade().edit(bill);
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
            bf.setPatienEncounter(getSurgeryBill().getProcedure());
            bf.setPatient(getSurgeryBill().getPatientEncounter().getPatient());
            bf.setInstitution(getSessionController().getInstitution());

            getBillFeeFacade().create(bf);
        } else {
            getBillFeeFacade().edit(bf);
        }
    }

    private double savePatientItem(PatientItem patientItem) {
        TimedItemFee timedItemFee = getInwardBean().getTimedItemFee((TimedItem) patientItem.getItem());
        double count = getInwardBean().calCount(timedItemFee, patientItem.getFromTime(), patientItem.getToTime());

        patientItem.setServiceValue(count * timedItemFee.getFee());
        patientItem.setPatientEncounter(getSurgeryBill().getPatientEncounter());
        if (patientItem.getId() == null) {
            patientItem.setCreater(getSessionController().getLoggedUser());
            patientItem.setCreatedAt(Calendar.getInstance().getTime());
            getPatientItemFacade().create(patientItem);

        } else {
            getPatientItemFacade().edit(patientItem);
        }

        return patientItem.getServiceValue();
    }

    private void saveEncounterComponent(BillItem billItem, EncounterComponent ec) {
        if (ec.getId() == null) {
            ec.setBillItem(billItem);
            ec.setCreatedAt(Calendar.getInstance().getTime());
            ec.setCreater(getSessionController().getLoggedUser());
            ec.setPatientEncounter(getSurgeryBill().getProcedure());
            if (ec.getBillFee() != null) {
                ec.setStaff(ec.getBillFee().getStaff());
            }
            getEncounterComponentFacade().create(ec);
        } else {
            getEncounterComponentFacade().edit(ec);
        }
    }

    public List<BillItem> getBillItems(Bill b) {
        String sql = "Select b From BillItem b where "
                + " b.retired=false and b.bill=:bill";
        HashMap hm = new HashMap();
        hm.put("bill", b);

        return getBillItemFacade().findByJpql(sql, hm);
    }

    private boolean saveProfessionalBill() {
        BillItem bItem;
        if (getProfessionalBill().getId() == null) {
            saveBill(getProfessionalBill(), BillNumberSuffix.INWPRO);
            bItem = new BillItem();
            saveBillItem(bItem, getProfessionalBill());
        } else {
            getBillFacade().edit(getProfessionalBill());
            bItem = getBillBean().fetchFirstBillItem(getProfessionalBill());
        }

        for (EncounterComponent ec : getProEncounterComponents()) {
            saveBillFee(ec.getBillFee(), getProfessionalBill(), bItem, ec.getBillFee().getFeeValue());
            saveEncounterComponent(bItem, ec);
        }

        updateBillItem(bItem);
        updateBill(getProfessionalBill());

        return false;
    }

    private double getTotalByBillFee(BillItem billItem) {
        String sql = "Select sum(bf.feeValue) from BillFee bf where "
                + " bf.retired=false and bf.billItem=:bItm";
        HashMap hm = new HashMap();
        hm.put("bItm", billItem);
        return getBillFeeFacade().findDoubleByJpql(sql, hm);
    }

    private double getTotalByBillItem(Bill bill) {
        String sql = "Select sum(bf.netValue) from BillItem bf where "
                + " bf.retired=false and bf.bill=:bill";
        HashMap hm = new HashMap();
        hm.put("bill", bill);
        return getBillItemFacade().findDoubleByJpql(sql, hm);
    }

    private void updateBillItem(BillItem billItem) {
        double value = getTotalByBillFee(billItem);
        billItem.setNetValue(value);
        getBillItemFacade().edit(billItem);
    }

    private void updateBill(Bill bill) {
        double value = getTotalByBillItem(bill);
        bill.setTotal(value);
        bill.setNetTotal(value);
        getBillFacade().edit(bill);
    }

    public String save() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (generalChecking()) {
            return "";
        }

        saveProcedure();
        saveSurgeryBill();

        if (!getProEncounterComponents().isEmpty()) {
            saveProfessionalBill();
        }
        getBillBean().updateBatchBill(getSurgeryBill());
        JsfUtil.addSuccessMessage("Surgery Detail Added");
        bhtSummeryController.setPatientEncounter(getSurgeryBill().getPatientEncounter());
        resetSurgeryBillValues();
        
        return bhtSummeryController.navigateToInpatientProfile();
    }

    /**
     * Creates a new instance of SurgeryBill
     */
    public SurgeryBillController() {
    }

    public Bill getSurgeryBill() {
        if (surgeryBill == null) {
            surgeryBill = new BilledBill();
            surgeryBill.setBillType(BillType.SurgeryBill);
            surgeryBill.setProcedure(new PatientEncounter());
        }
        return surgeryBill;
    }

//    private List<Bill> getBillsByForwardRef(Bill b) {
//        String sql = "Select bf from Bill bf where bf.cancelled=false and "
//                + " bf.retired=false and bf.forwardReferenceBill=:bill";
//        HashMap hm = new HashMap();
//        hm.put("bill", getBatchBill());
//        List<Bill> list = getBillFacade().findByJpql(sql, hm);
//
//        if (list == null) {
//            return new ArrayList<>();
//        }
//
//        return list;
//    }
//    public void setBatchBill(Bill surgeryBill) {
//        resetSurgeryBillValues();
//        this.surgeryBill = surgeryBill;
//        for (Bill b : getBillsByForwardRef(surgeryBill)) {
//            if (b.getSurgeryBillType() == SurgeryBillType.ProfessionalFee) {
//                // System.err.println(SurgeryBillType.ProfessionalFee);
//                setProfessionalBill(b);
//                List<EncounterComponent> enc = getBillBean().getEncounterComponents(b);
//                setProEncounterComponents(enc);
//            }
//
//            if (b.getSurgeryBillType() == SurgeryBillType.TimedService) {
//                List<EncounterComponent> enc = getBillBean().getEncounterComponents(b);
//                setTimedEncounterComponents(enc);
//            }
//
//            if (b.getSurgeryBillType() == SurgeryBillType.Service) {
//                departmentBillItems = getInwardBean().createDepartmentBillItems(surgeryBill.getPatientEncounter(), getBatchBill());
//            }
//
//            if (b.getSurgeryBillType() == SurgeryBillType.PharmacyItem) {
//                createIssueTable();
//            }
//
//        }
//    }
    private List<BillItem> pharmacyIssues;
    List<BillItem> storeIssues;

    public List<BillItem> getStoreIssues() {
        return storeIssues;
    }

    public void setStoreIssues(List<BillItem> storeIssues) {
        this.storeIssues = storeIssues;
    }

    public void createIssueTable() {
        pharmacyIssues = createIssueTable(BillType.PharmacyBhtPre);
        storeIssues = createIssueTable(BillType.StoreBhtPre);

    }

    private List<BillItem> createIssueTable(BillType billType) {
        String sql;
        HashMap hm;
        sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.forwardReferenceBill=:bil "
                + " and b.bill.billType=:btp "
                + " and b.bill.billedBill is null "
                + " and (type(b.bill)=:class)"
                + " order by b.item.name ";
        hm = new HashMap();
        hm.put("bil", getSurgeryBill());
        hm.put("btp", billType);
        hm.put("class", PreBill.class);

        List<BillItem> billItems = getBillItemFacade().findByJpql(sql, hm);

        hm.clear();
        sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp"
                + " and b.bill.forwardReferenceBill=:bil "
                + " and type(b.bill.billedBill)=:billedClass "
                + " and (type(b.bill)=:class)"
                + " order by b.item.name ";
        hm = new HashMap();
        hm.put("btp", billType);
        hm.put("class", RefundBill.class);
        hm.put("billedClass", PreBill.class);
        hm.put("bil", getSurgeryBill());
//        hm.put("pe", getBatchBill().getPatientEncounter());

        List<BillItem> billItems1 = getBillItemFacade().findByJpql(sql, hm);

        billItems.addAll(billItems1);

        return billItems;

    }

    public Bill getProfessionalBill() {
        if (professionalBill == null) {
            professionalBill = new BilledBill();
            professionalBill.setBillType(BillType.InwardProfessional);
            professionalBill.setSurgeryBillType(SurgeryBillType.ProfessionalFee);
        }

        return professionalBill;
    }

    public void setProfessionalBill(Bill professionalBill) {

        this.professionalBill = professionalBill;
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
        if (getSurgeryBill().getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("Admission ?");
            return true;
        }
        if (getSurgeryBill().getProcedure().getItem() == null) {
            JsfUtil.addErrorMessage("Select Surgery");
            return true;
        }

        if (getSurgeryBill().getPatientEncounter().isPaymentFinalized()) {
            JsfUtil.addErrorMessage("Final Payment is Finalized");
            return true;
        }

        return false;

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
            getSurgeryBill().setStaff(getProEncounterComponent().getBillFee().getStaff());
        }

        proEncounterComponent.setPatientEncounter(getSurgeryBill().getPatientEncounter());
        proEncounterComponent.setChildEncounter(getSurgeryBill().getProcedure());
        proEncounterComponent.setOrderNo(getProEncounterComponents().size());
        getProEncounterComponents().add(proEncounterComponent);

        proEncounterComponent = null;
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

    public PatientEncounterFacade getPatientEncounterFacade() {
        return patientEncounterFacade;
    }

    public void setPatientEncounterFacade(PatientEncounterFacade patientEncounterFacade) {
        this.patientEncounterFacade = patientEncounterFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
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

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public EncounterComponentFacade getEncounterComponentFacade() {
        return encounterComponentFacade;
    }

    public void setEncounterComponentFacade(EncounterComponentFacade encounterComponentFacade) {
        this.encounterComponentFacade = encounterComponentFacade;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public List<EncounterComponent> getTimedEncounterComponents() {
        if (timedEncounterComponents == null) {
            timedEncounterComponents = new ArrayList<>();
        }
        return timedEncounterComponents;
    }

    public void setTimedEncounterComponents(List<EncounterComponent> timedEncounterComponents) {
        this.timedEncounterComponents = timedEncounterComponents;
    }

    public PatientItemFacade getPatientItemFacade() {
        return patientItemFacade;
    }

    public void setPatientItemFacade(PatientItemFacade patientItemFacade) {
        this.patientItemFacade = patientItemFacade;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public List<DepartmentBillItems> getDepartmentBillItems() {
        if (departmentBillItems == null) {
            departmentBillItems = getInwardBean().createDepartmentBillItems(getSurgeryBill().getPatientEncounter(), getSurgeryBill());
        }
        return departmentBillItems;
    }

    public void setDepartmentBillItems(List<DepartmentBillItems> departmentBillItems) {
        this.departmentBillItems = departmentBillItems;
    }

    public List<BillItem> getPharmacyIssues() {
        return pharmacyIssues;
    }

    public void setPharmacyIssues(List<BillItem> pharmacyIssues) {
        this.pharmacyIssues = pharmacyIssues;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public void setSurgeryBill(Bill surgeryBill) {
        this.surgeryBill = surgeryBill;
    }

}
