/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.AuditEventController;
import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.dataStructure.DepartmentBillItems;
import com.divudi.core.data.inward.PatientEncounterComponentType;
import com.divudi.core.data.inward.SurgeryBillType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.PatientItem;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.inward.EncounterComponent;
import com.divudi.core.entity.inward.TimedItem;
import com.divudi.core.entity.inward.TimedItemFee;
import com.divudi.core.entity.Speciality;
import com.divudi.core.entity.Staff;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.EncounterComponentFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PatientItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.StaffFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.PrimeFaces;

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
    private boolean duplicateConfirmationPending = false;
    // Clinical details (no billing)
    private EncounterComponent clinicalEncounterComponent;
    private List<EncounterComponent> clinicalEncounterComponents;
    private Speciality clinicalSpeciality;
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
    @EJB
    private StaffFacade staffFacade;

    //////
    @Inject
    private SessionController sessionController;
    @Inject
    InwardTimedItemController inwardTimedItemController;
    @Inject
    private BillBeanController billBean;
    @Inject
    private InwardBeanController inwardBean;
    @Inject
    BhtSummeryController bhtSummeryController;
    @Inject
    AuditEventController auditEventController;

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

        return "/theater/surgery_add?faces-redirect=true";
    }

    public String navigateToAddSurgeriesFromMenu() {
        resetSurgeryBillValues();
        return "/theater/surgery_add?faces-redirect=true";
    }

    public String navigateToSurgeryListFromAdmissionProfile() {
        PatientEncounter pe1 = getSurgeryBill().getPatientEncounter();
        resetSurgeryBillValues();
        getSurgeryBill().setPatientEncounter(pe1);
        surgeryList = null;
        return "/theater/inward_bill_surgery_list?faces-redirect=true";
    }

    public String navigateToSurgeryListFromMenu() {
        resetSurgeryBillValues();
        surgeryList = null;
        return "/theater/inward_bill_surgery_list?faces-redirect=true";
    }

    public String navigateToEditSurgery() {
        return "/theater/surgery_edit?faces-redirect=true";
    }

    // -------------------------------------------------------------------------
    // Surgery list & delete
    // -------------------------------------------------------------------------

    private List<Bill> surgeryList;
    private Bill selectedSurgeryToDelete;
    private List<Bill> blockingBillsForDelete;

    public List<Bill> getSurgeryList() {
        if (surgeryList == null && getSurgeryBill().getPatientEncounter() != null) {
            String jpql = "SELECT b FROM Bill b WHERE b.retired = false AND b.cancelled = false "
                    + " AND b.billType = :bt AND b.patientEncounter = :pe ORDER BY b.createdAt";
            HashMap<String, Object> params = new HashMap<>();
            params.put("bt", BillType.SurgeryBill);
            params.put("pe", getSurgeryBill().getPatientEncounter());
            surgeryList = getBillFacade().findByJpql(jpql, params);
        }
        return surgeryList;
    }

    /**
     * Called when user clicks Delete on a surgery row.
     * Checks for active child bills (professional, service, pharmacy, store)
     * linked via forwardReferenceBill. If any exist the delete is blocked and
     * blockingBillsForDelete is populated so the UI can show a warning.
     */
    public void checkAndDeleteSurgery(Bill surgery) {
        if (surgery == null || surgery.getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("Admission ?");
            return;
        }
        if (surgery.getPatientEncounter().isPaymentFinalized()) {
            JsfUtil.addErrorMessage("Final Payment is Finalized");
            return;
        }

        selectedSurgeryToDelete = surgery;
        blockingBillsForDelete = findActiveBillsByForwardRef(surgery);

        if (!blockingBillsForDelete.isEmpty()) {
            // Do not delete — UI will render the warning panel
            JsfUtil.addErrorMessage("This surgery has related bills that must be cancelled first. See the list below.");
            return;
        }

        // Safe to retire
        retireSurgeryBill(surgery);
        getBillBean().updateBatchBill(surgery);
        surgeryList = null; // refresh list
        JsfUtil.addSuccessMessage("Surgery removed successfully.");
    }

    private List<Bill> findActiveBillsByForwardRef(Bill surgery) {
        String jpql = "SELECT b FROM Bill b WHERE b.retired = false AND b.cancelled = false "
                + " AND b.forwardReferenceBill = :surg";
        HashMap<String, Object> params = new HashMap<>();
        params.put("surg", surgery);
        return getBillFacade().findByJpql(jpql, params);
    }

    private void retireSurgeryBill(Bill surgery) {
        surgery.setRetired(true);
        surgery.setRetiredAt(new Date());
        surgery.setRetirer(getSessionController().getLoggedUser());
        getBillFacade().edit(surgery);

        // Also retire the linked procedure PatientEncounter
        if (surgery.getProcedure() != null && surgery.getProcedure().getId() != null) {
            PatientEncounter proc = surgery.getProcedure();
            proc.setRetired(true);
            proc.setRetiredAt(new Date());
            proc.setRetirer(getSessionController().getLoggedUser());
            getPatientEncounterFacade().edit(proc);
        }
    }

    public Bill getSelectedSurgeryToDelete() {
        return selectedSurgeryToDelete;
    }

    public List<Bill> getBlockingBillsForDelete() {
        return blockingBillsForDelete;
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
        surgeryList = null;
        selectedSurgeryToDelete = null;
        blockingBillsForDelete = null;
        duplicateConfirmationPending = false;
        clinicalEncounterComponent = null;
        clinicalEncounterComponents = null;
        clinicalSpeciality = null;
    }

    public void saveProcedure() {

        PatientEncounter procedure = getSurgeryBill().getProcedure();

        if (procedure.getId() == null || procedure.getId() == 0) {
            procedure.setCreatedAt(new Date());
            procedure.setCreater(getSessionController().getLoggedUser());
            // Persist without parentEncounter first to avoid cascade issues with
            // the detached admission entity in the persistence context
            getPatientEncounterFacade().create(procedure);
            // Now link to parent and update
            procedure.setParentEncounter(getSurgeryBill().getPatientEncounter());
            getPatientEncounterFacade().edit(procedure);
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
        return performSave();
    }

    public String edit() {
        if (generalChecking()) {
            return null;
        }

        String beforeName = (surgeryBill != null && surgeryBill.getProcedure() != null
                && surgeryBill.getProcedure().getItem() != null)
                ? surgeryBill.getProcedure().getItem().getName() : "";
        String beforeJson = "{\"surgeryName\": \"" + beforeName.replace("\"", "\\\"") + "\"}";
        Long billId = surgeryBill != null ? surgeryBill.getId() : null;
        com.divudi.core.entity.AuditEvent auditEvent = auditEventController.createNewAuditEvent(
                "Edit Surgery", beforeJson, billId);

        saveProcedure();
        saveSurgeryBill();
        getBillBean().updateBatchBill(getSurgeryBill());

        String afterName = (surgeryBill != null && surgeryBill.getProcedure() != null
                && surgeryBill.getProcedure().getItem() != null)
                ? surgeryBill.getProcedure().getItem().getName() : "";
        String afterJson = "{\"surgeryName\": \"" + afterName.replace("\"", "\\\"") + "\"}";
        auditEventController.completeAuditEvent(auditEvent, afterJson);

        return auditEventController.navigateToAllAuditEventsForBill(billId);
    }

    /**
     * Creates a new instance of SurgeryBill
     */
    public SurgeryBillController() {
    }

    // -------------------------------------------------------------------------
    // Clinical Details (Performed By / Assisted By / Anaesthesia By / Op Notes)
    // -------------------------------------------------------------------------

    public String navigateToSurgicalDetails(Bill surgery) {
        clinicalEncounterComponent = null;
        clinicalEncounterComponents = null;
        clinicalSpeciality = null;
        this.surgeryBill = surgery;
        if (surgery != null && surgery.getProcedure() != null) {
            if (surgery.getProcedure().getFromTime() == null) {
                surgery.getProcedure().setFromTime(new Date());
            }
            if (surgery.getProcedure().getToTime() == null) {
                surgery.getProcedure().setToTime(new Date());
            }
        }
        fetchClinicalEncounterComponents();
        return "/theater/surgery_clinical_details?faces-redirect=true";
    }

    private void fetchClinicalEncounterComponents() {
        clinicalEncounterComponents = null;
        if (surgeryBill == null || surgeryBill.getProcedure() == null
                || surgeryBill.getProcedure().getId() == null) {
            clinicalEncounterComponents = new ArrayList<>();
            return;
        }
        String jpql = "SELECT ec FROM EncounterComponent ec"
                + " WHERE ec.retired = false"
                + " AND ec.patientEncounter = :proc"
                + " AND ec.billFee IS NULL"
                + " ORDER BY ec.orderNo";
        HashMap<String, Object> params = new HashMap<>();
        params.put("proc", surgeryBill.getProcedure());
        clinicalEncounterComponents = getEncounterComponentFacade().findByJpql(jpql, params);
    }

    public void addClinicalStaff() {
        if (surgeryBill == null || surgeryBill.getProcedure() == null) {
            JsfUtil.addErrorMessage("No surgery selected.");
            return;
        }
        if (surgeryBill.getPatientEncounter() != null && surgeryBill.getPatientEncounter().isPaymentFinalized()) {
            JsfUtil.addErrorMessage("Payment finalized; cannot modify clinical details.");
            return;
        }
        if (getClinicalEncounterComponent().getStaff() == null) {
            JsfUtil.addErrorMessage("Select Staff.");
            return;
        }
        if (getClinicalEncounterComponent().getPatientEncounterComponentType() == null) {
            JsfUtil.addErrorMessage("Select Role.");
            return;
        }
        clinicalEncounterComponent.setOrderNo(getClinicalEncounterComponents().size());
        clinicalEncounterComponent.setPatientEncounter(surgeryBill.getProcedure());
        clinicalEncounterComponent.setCreatedAt(new Date());
        clinicalEncounterComponent.setCreater(getSessionController().getLoggedUser());
        getEncounterComponentFacade().create(clinicalEncounterComponent);
        getClinicalEncounterComponents().add(clinicalEncounterComponent);
        clinicalEncounterComponent = null;
        clinicalSpeciality = null;
        JsfUtil.addSuccessMessage("Staff member added.");
    }

    public void removeClinicalStaff(EncounterComponent ec) {
        if (surgeryBill != null && surgeryBill.getPatientEncounter() != null && surgeryBill.getPatientEncounter().isPaymentFinalized()) {
            JsfUtil.addErrorMessage("Payment finalized; cannot modify clinical details.");
            return;
        }
        ec.setRetired(true);
        ec.setRetiredAt(new Date());
        ec.setRetirer(getSessionController().getLoggedUser());
        getEncounterComponentFacade().edit(ec);
        getClinicalEncounterComponents().remove(ec);
        int index = 0;
        for (EncounterComponent remaining : getClinicalEncounterComponents()) {
            remaining.setOrderNo(index++);
            getEncounterComponentFacade().edit(remaining);
        }
        JsfUtil.addSuccessMessage("Removed.");
    }

    public void saveSurgicalDetails() {
        if (surgeryBill == null || surgeryBill.getProcedure() == null) {
            JsfUtil.addErrorMessage("No surgery selected.");
            return;
        }
        if (surgeryBill.getPatientEncounter() != null && surgeryBill.getPatientEncounter().isPaymentFinalized()) {
            JsfUtil.addErrorMessage("Payment finalized; cannot modify clinical details.");
            return;
        }
        PatientEncounter proc = surgeryBill.getProcedure();
        getPatientEncounterFacade().edit(proc);
        JsfUtil.addSuccessMessage("Surgical details saved.");
    }

    public List<Staff> completeClinicalStaff(String qry) {
        String sql = "select c from Staff c where c.retired=false";
        HashMap<String, Object> hm = new HashMap<>();
        if (clinicalSpeciality != null) {
            sql += " and c.speciality=:sp";
            hm.put("sp", clinicalSpeciality);
        }
        sql += " and ((c.person.name) like :q or (c.code) like :q) order by c.person.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        return staffFacade.findByJpql(sql, hm, 20);
    }

    public Speciality getClinicalSpeciality() {
        return clinicalSpeciality;
    }

    public void setClinicalSpeciality(Speciality clinicalSpeciality) {
        this.clinicalSpeciality = clinicalSpeciality;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public EncounterComponent getClinicalEncounterComponent() {
        if (clinicalEncounterComponent == null) {
            clinicalEncounterComponent = new EncounterComponent();
        }
        return clinicalEncounterComponent;
    }

    public void setClinicalEncounterComponent(EncounterComponent clinicalEncounterComponent) {
        this.clinicalEncounterComponent = clinicalEncounterComponent;
    }

    public List<EncounterComponent> getClinicalEncounterComponents() {
        if (clinicalEncounterComponents == null) {
            fetchClinicalEncounterComponents();
        }
        return clinicalEncounterComponents;
    }

    public void setClinicalEncounterComponents(List<EncounterComponent> clinicalEncounterComponents) {
        this.clinicalEncounterComponents = clinicalEncounterComponents;
    }

    public Bill getSurgeryBill() {
        if (surgeryBill == null) {
            surgeryBill = new BilledBill();
            surgeryBill.setBillType(BillType.SurgeryBill);
            surgeryBill.setProcedure(new PatientEncounter());
        }
        return surgeryBill;
    }

    /**
     * Re-fetches the current surgery bill from the database so that the
     * in-memory object reflects any totals updated by child-bill operations
     * (e.g. professional fee cancellation) that ran in a different bean.
     */
    public void refreshSurgeryBillFromDb() {
        if (surgeryBill != null && surgeryBill.getId() != null) {
            surgeryBill = getBillFacade().find(surgeryBill.getId());
        }
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

    private boolean isDuplicateSurgery() {
        if (getSurgeryBill().getId() != null) {
            return false; // editing existing bill — no duplicate check
        }
        Long peId = getSurgeryBill().getPatientEncounter().getId();
        Long itemId = getSurgeryBill().getProcedure().getItem().getId();
        String dupJpql = "SELECT COUNT(pe) FROM PatientEncounter pe"
                + " WHERE pe.parentEncounter.id = :peId AND pe.item.id = :itemId"
                + " AND pe.retired = false";
        HashMap<String, Object> dupParams = new HashMap<>();
        dupParams.put("peId", peId);
        dupParams.put("itemId", itemId);
        long count = getPatientEncounterFacade().findLongByJpql(dupJpql, dupParams);
        return count > 0;
    }

    /**
     * Called by the "Add New Surgery" button (AJAX).
     * If a duplicate is detected, pushes a JS call to open the confirmation
     * dialog. Otherwise saves and redirects programmatically so that AJAX
     * navigation works correctly.
     */
    public void checkBeforeSave() {
        if (generalChecking()) {
            duplicateConfirmationPending = false;
            return;
        }
        if (isDuplicateSurgery()) {
            duplicateConfirmationPending = true;
            PrimeFaces.current().executeScript("PF('dlgDupConfirm').show();");
        } else {
            duplicateConfirmationPending = false;
            performSaveAndRedirect();
        }
    }

    /**
     * Called when the user explicitly confirms adding a duplicate surgery
     * (ajax=false button in the dialog).
     */
    public String saveConfirmed() {
        duplicateConfirmationPending = false;
        return performSave();
    }

    private void performSaveAndRedirect() {
        if (generalChecking()) {
            return;
        }
        saveProcedure();
        saveSurgeryBill();
        if (!getProEncounterComponents().isEmpty()) {
            saveProfessionalBill();
        }
        getBillBean().updateBatchBill(getSurgeryBill());
        JsfUtil.addSuccessMessage("Surgery Detail Added");
        PatientEncounter pe = getSurgeryBill().getPatientEncounter();
        bhtSummeryController.setPatientEncounter(pe);
        resetSurgeryBillValues();
        String outcome = bhtSummeryController.navigateToInpatientProfile();
        try {
            FacesContext fc = FacesContext.getCurrentInstance();
            String viewId = outcome.replace("?faces-redirect=true", "");
            String contextPath = fc.getExternalContext().getRequestContextPath();
            fc.getExternalContext().redirect(contextPath + "/faces" + viewId);
        } catch (java.io.IOException e) {
            // ignore — page will remain but success message is already shown
        }
    }

    private String performSave() {
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

    public boolean isDuplicateConfirmationPending() {
        return duplicateConfirmationPending;
    }

    public void setDuplicateConfirmationPending(boolean duplicateConfirmationPending) {
        this.duplicateConfirmationPending = duplicateConfirmationPending;
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
            List<PatientEncounter> cpts = getInwardBean().fetchChildPatientEncounter(getSurgeryBill().getPatientEncounter());
            departmentBillItems = getInwardBean().createDepartmentBillItems(getSurgeryBill().getPatientEncounter(), getSurgeryBill(),cpts);
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

    public void setSurgeryBill(Bill surgeryBill) {
        this.surgeryBill = surgeryBill;
    }

}
