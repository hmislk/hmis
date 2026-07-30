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
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;

import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.inward.SurgeryBillType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.PatientItem;
import com.divudi.core.entity.inward.EncounterComponent;
import com.divudi.core.entity.inward.TimedItem;
import com.divudi.core.entity.inward.TimedItemFee;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.EncounterComponentFacade;
import com.divudi.core.facade.PatientItemFacade;
import com.divudi.core.facade.TimedItemFeeFacade;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.apache.commons.beanutils.BeanUtils;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class InwardTimedItemController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;
    //////////////////////
    private List<PatientItem> items;
    private PatientItem current;
    EncounterComponent timedEncounterComponent;
    List<EncounterComponent> timedEncounterComponents;
    Bill bill;
    Bill batchBill;
    /////////
    @EJB
    private PatientItemFacade patientItemFacade;
    @EJB
    private TimedItemFeeFacade timedItemFeeFacade;

    @Inject
    private InwardBeanController inwardBean;
    @Inject
    BillBeanController billBean;
    @Inject
    private SurgeryBillController surgeryBillController;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    EncounterComponentFacade encounterComponentFacade;
    @EJB
    BillNumberGenerator billNumberBean;

    Date frmDate;
    Date toDate;
    double total;
    double totalMins;
    private Institution institution;
    private Institution site;
    private Department department;
    private Department fromDepartment;

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public EncounterComponentFacade getEncounterComponentFacade() {
        return encounterComponentFacade;
    }

    public void setEncounterComponentFacade(EncounterComponentFacade encounterComponentFacade) {
        this.encounterComponentFacade = encounterComponentFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
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

    public EncounterComponent getTimedEncounterComponent() {
        if (timedEncounterComponent == null) {
            timedEncounterComponent = new EncounterComponent();
            BillFee bf = new BillFee();
            PatientItem pi = new PatientItem();
            pi.setItem(new Item());
            bf.setPatientItem(pi);
            timedEncounterComponent.setBillFee(bf);
        }
        return timedEncounterComponent;
    }

    public void setItems(List<PatientItem> items) {
        this.items = items;
    }

    public void createTimeServiceList() {

        String sql;
        HashMap m = new HashMap();

        sql = "select i from PatientItem i where "
                + " i.patientEncounter.dateOfDischarge between :fd and :td "
                + " and i.retired=false ";

        if (getCurrent().getItem() != null) {
            sql += " and i.item=:item";
            m.put("item", getCurrent().getItem());
        }

        if (institution != null) {
            sql += " and i.patientEncounter.institution=:ins";
            m.put("ins", institution);
        }

        if (site != null) {
            sql += " and i.patientEncounter.department.site=:site";
            m.put("site", site);
        }

        if (department != null) {
            sql += " and i.patientEncounter.department=:dept";
            m.put("dept", department);
        }

        m.put("fd", frmDate);
        m.put("td", toDate);

        items = getPatientItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        total = 0.0;
        totalMins = 0.0;
        for (PatientItem pi : items) {
            long l = (pi.getToTime().getTime() - pi.getFromTime().getTime()) / (1000 * 60);
            pi.setTmpConsumedTime(l);
            total += pi.getServiceValue();
            totalMins += pi.getTmpConsumedTime();
        }
    }

    private boolean generalChecking() {
        if (getBatchBill() == null) {
            return true;
        }

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

        if (getBatchBill().getPatientEncounter().isNursingDischarged()
                && !webUserController.hasPrivilege("InwardAddChargesAfterNursingDischarge")) {
            JsfUtil.addErrorMessage("Cannot add charges: nursing discharge has been confirmed for this patient.");
            return true;
        }

        return false;

    }

    public void addTimedService() {
        if (generalChecking()) {
            return;
        }

        if (getTimedEncounterComponent().getBillFee().getPatientItem().getItem() == null) {
            JsfUtil.addErrorMessage("Select Timed Service ");
            return;
        }

        PatientItem newPatientItem = getTimedEncounterComponent().getBillFee().getPatientItem();
        if (newPatientItem.getToTime() != null && newPatientItem.getFromTime() != null) {
            if (newPatientItem.getToTime().before(newPatientItem.getFromTime())) {
                JsfUtil.addErrorMessage("Service Not Finalize check Service Start Time & End Time");
                return;
            }
            if (newPatientItem.getToTime().getTime() == newPatientItem.getFromTime().getTime()) {
                JsfUtil.addErrorMessage("Service Start Time & End Time Can't Be Equal");
                return;
            }
        }

        timedEncounterComponent.setPatientEncounter(getBatchBill().getPatientEncounter());
        timedEncounterComponent.setChildEncounter(getBatchBill().getProcedure());
        timedEncounterComponent.setOrderNo(getTimedEncounterComponents().size());
        getTimedEncounterComponents().add(timedEncounterComponent);

        saveSurgeryTimedService();

        timedEncounterComponent = null;
    }

    private double savePatientItem(PatientItem patientItem) {
        double value = getInwardBean().calTotalTimedChargeForItem(
                (TimedItem) patientItem.getItem(),
                patientItem.getFromTime(),
                patientItem.getToTime(),
                patientItem.getPatientEncounter() != null && patientItem.getPatientEncounter().isForiegner());
        patientItem.setServiceValue(value);
        patientItem.setPatientEncounter(getBatchBill().getPatientEncounter());
        if (patientItem.getId() == null) {
            patientItem.setCreater(getSessionController().getLoggedUser());
            patientItem.setCreatedAt(Calendar.getInstance().getTime());
            getPatientItemFacade().create(patientItem);

        } else {
            getPatientItemFacade().edit(patientItem);
        }

        return patientItem.getServiceValue();
    }

    private void updateBillFee(BillFee bf) {
        getBillFeeFacade().edit(bf);
        updateBillItem(bf.getBillItem());
        updateBill(bf.getBill());
        getBillBean().updateBatchBill(getBatchBill());
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
        getBillFacade().edit(bill);
    }

    public void updateTimedService(BillFee bf) {
        if (generalChecking()) {
            return;
        }
        if (bf.getPatientItem().getToTime() != null && bf.getPatientItem().getFromTime() != null) {
            if (bf.getPatientItem().getToTime().before(bf.getPatientItem().getFromTime())) {
                JsfUtil.addErrorMessage("Service Not Finalize check Service Start Time & End Time");
                return;
            }
            if (bf.getPatientItem().getToTime().getTime() == bf.getPatientItem().getFromTime().getTime()) {
                JsfUtil.addErrorMessage("Service Start Time & End Time Can't Be Equal");
                return;
            }
        }

        double value = savePatientItem(bf.getPatientItem());
        bf.setFeeValue(value);
        bf.setFeeGrossValue(value);
        updateBillFee(bf);
    }

    public void removeTimedEncFromList(EncounterComponent encounterComponent) {
        removeEncounterComponentFromList(encounterComponent, getTimedEncounterComponents());
    }

    public void removeTimedEncFromDbase(EncounterComponent encounterComponent) {
        if (generalChecking()) {
            return;
        }
        if (encounterComponent.getBillItem() != null && encounterComponent.getBillItem().isFromPackage()) {
            JsfUtil.addErrorMessage("This item is included in the admission's package and cannot be removed.");
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

            PatientItem patientItem = removingFee.getPatientItem();
            removePatientItem(patientItem);
        }

    }

    public void removePatientItem(PatientItem patientItem) {
        if (patientItem != null && patientItem.getBillItem() != null && patientItem.getBillItem().isFromPackage()) {
            JsfUtil.addErrorMessage("This item is included in the admission's package and cannot be removed.");
            return;
        }
        if (patientItem != null && isLockedForChanges(patientItem.getPatientEncounter())) {
            return;
        }
        if (patientItem != null) {
            patientItem.setRetirer(getSessionController().getLoggedUser());
            patientItem.setRetiredAt(new Date());
            patientItem.setRetired(true);
            getPatientItemFacade().edit(patientItem);
            retireTimedServiceBill(patientItem);

            createPatientItems();

            JsfUtil.addSuccessMessage("Removed successfully.");
        }
    }

    /**
     * Retires the BillItem and Bill behind a removed timed service. Without
     * this the charge would survive the removal, since the inward totals are
     * summed from the BillItem side.
     */
    private void retireTimedServiceBill(PatientItem patientItem) {
        BillItem bi = patientItem.getBillItem();
        if (bi == null || bi.isFromPackage()) {
            return;
        }
        bi.setRetired(true);
        bi.setRetiredAt(new Date());
        bi.setRetirer(getSessionController().getLoggedUser());
        getBillItemFacade().edit(bi);

        Bill b = bi.getBill();
        if (b != null) {
            b.setRetired(true);
            b.setRetiredAt(new Date());
            b.setRetirer(getSessionController().getLoggedUser());
            getBillFacade().edit(b);
        }
    }

    private void removeEncounterComponentFromList(EncounterComponent encounterComponent, List<EncounterComponent> list) {
        list.remove(encounterComponent.getOrderNo());

        int index = 0;
        for (EncounterComponent ec : list) {
            ec.setOrderNo(index++);
        }

    }

    public void setTimedEncounterComponent(EncounterComponent timedEncounterComponent) {
        this.timedEncounterComponent = timedEncounterComponent;
    }

    public void selectSurgeryBillListener() {
        Bill fetchedBill = getBillBean().fetchByForwardBill(getBatchBill(), SurgeryBillType.TimedService);
        if (fetchedBill != null) {
            setBill(fetchedBill);
            List<EncounterComponent> enc = getBillBean().getEncounterComponents(getBill());

            if (enc != null) {
                setTimedEncounterComponents(enc);
            }
        }

    }

    private void saveBill(Bill bill, BillNumberSuffix billNumberSuffix) {
        if (bill.getId() == null) {
            bill.setForwardReferenceBill(getBatchBill());
            bill.setCreatedAt(Calendar.getInstance().getTime());
            bill.setCreater(getSessionController().getLoggedUser());
            bill.setPatientEncounter(getBatchBill().getPatientEncounter());
            bill.setProcedure(getBatchBill().getProcedure());
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
            bf.setPatienEncounter(getBatchBill().getProcedure());
            bf.setPatient(getBatchBill().getPatientEncounter().getPatient());
            bf.setInstitution(getSessionController().getInstitution());

            getBillFeeFacade().create(bf);
        } else {
            getBillFeeFacade().edit(bf);
        }
    }

    private boolean saveTimeServiceBill() {
        BillItem bItem;
        double netValue = 0;
        if (getBill().getId() == null) {
            saveBill(getBill(), BillNumberSuffix.TIME);
            bItem = new BillItem();
            saveBillItem(bItem, getBill());
        } else {
            getBillFacade().edit(getBill());
            bItem = getBillBean().fetchFirstBillItem(getBill());
        }

        for (EncounterComponent ec : getTimedEncounterComponents()) {
            netValue = savePatientItem(ec.getBillFee().getPatientItem());

            saveBillFee(ec.getBillFee(), getBill(), bItem, netValue);
            saveEncounterComponent(bItem, ec);
        }

        updateBillItem(bItem);
        updateBill(getBill());

        return false;
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

    public void saveSurgeryTimedService() {
        if (batchBill != null && surgeryBillController.isSurgeryLockedForAdditions(batchBill)) {
            JsfUtil.addErrorMessage("This surgery has been validated and is locked. Revert validation to make changes.");
            return;
        }

        if (generalChecking()) {
            return;
        }

        if (!getTimedEncounterComponents().isEmpty()) {
            saveTimeServiceBill();
        }

        getBillBean().updateBatchBill(getBatchBill());

        JsfUtil.addSuccessMessage("Surgery Detail Successfull Updated");

        //makeNull();
    }

    public Bill getBatchBill() {
        return batchBill;
    }

    public void setBatchBill(Bill batchBill) {
        this.batchBill = batchBill;
    }

    public Bill getBill() {
        if (bill == null) {
            bill = new BilledBill();
            bill.setSurgeryBillType(SurgeryBillType.TimedService);
        }
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public void makeNull() {
        items = null;
        current = null;
        timedEncounterComponent = null;
        timedEncounterComponents = null;
        batchBill = null;
        bill = null;
        fromDepartment = null;

    }

    public String navigateToAddInwardTimedServicesFromMenu() {
        makeNull();
        return "/inward/inward_timed_service_consume?faces-redirect=true";
    }

    public String navigateToAddInwardTimedServicesFromInpatientProfile(PatientEncounter pe) {
        makeNull();
        getCurrent().setPatientEncounter(pe);
        return "/inward/inward_timed_service_consume?faces-redirect=true";
    }

    public String navigateToAddInwardTimedServiceForTheatreFromMenu() {
        makeNull();
        return "/theater/inward_timed_service_consume_surgery?faces-redirect=true";
    }

    public String navigateToAddInwardTimedServiceForTheatreFromInpatientProfile() {
        makeNull();
        return "/theater/inward_timed_service_consume_surgery?faces-redirect=true";
    }

    public String navigateToSurgeryTimedServices(Bill surgeryBill) {
        makeNull();
        batchBill = surgeryBill;
        selectSurgeryBillListener();
        return "/theater/inward_timed_service_consume_surgery?faces-redirect=true";
    }

    private boolean errorCheck() {
        if (getCurrent().getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("Please Select BHT");
            return true;
        }
        if (getCurrent().getItem() == null) {
            JsfUtil.addErrorMessage("Please Select Service");
            return true;
        }
        if (isLockedForChanges(getCurrent().getPatientEncounter())) {
            return true;
        }
        if (getCurrent().getPatientEncounter().isNursingDischarged()
                && !webUserController.hasPrivilege("InwardAddChargesAfterNursingDischarge")) {
            JsfUtil.addErrorMessage("Cannot add charges: nursing discharge has been confirmed for this patient.");
            return true;
        }
        return false;
    }

    /**
     * A timed service and its bill stay mutable for the whole stay — the charge
     * keeps growing while the service runs — but become immutable once the
     * patient is discharged or the final payment is settled.
     */
    private boolean isLockedForChanges(PatientEncounter pe) {
        if (pe == null) {
            return false;
        }
        if (pe.isPaymentFinalized()) {
            JsfUtil.addErrorMessage("Final payment has been settled for this admission. Timed services can no longer be changed.");
            return true;
        }
        if (pe.isDischarged()) {
            JsfUtil.addErrorMessage("This patient has been discharged. Timed services can no longer be changed.");
            return true;
        }
        return false;
    }

    /**
     * Department the service was physically delivered in. Defaults to the
     * service item's own department and can be overridden by the user before
     * adding, since the same service may be given in a different ward or unit.
     */
    public Department getFromDepartment() {
        if (fromDepartment == null && current != null && current.getItem() != null) {
            fromDepartment = current.getItem().getDepartment();
        }
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    /**
     * Resets the delivering department to the newly selected service's own
     * department, so the default follows the item rather than sticking to
     * whatever was chosen for the previous service.
     */
    public void serviceSelectListener() {
        fromDepartment = current != null && current.getItem() != null
                ? current.getItem().getDepartment() : null;
    }

    public void save() {
        if (errorCheck()) {
            return;
        }
        if (getCurrent().getToTime() == null) {
            getCurrent().setToTime(new Date());
        }
        double value = getInwardBean().calTotalTimedChargeForItem(
                (TimedItem) getCurrent().getItem(),
                getCurrent().getPatientEncounter().getDateOfAdmission(),
                getCurrent().getToTime(),
                getCurrent().getPatientEncounter().isForiegner());
        getCurrent().setServiceValue(value);

        getCurrent().setCreater(getSessionController().getLoggedUser());
        getCurrent().setCreatedAt(Calendar.getInstance().getTime());
        getCurrent().setPatient(getCurrent().getPatientEncounter().getPatient());

        if (getCurrent().getId() == null) {
            createBillForTimedService(getCurrent());
            getPatientItemFacade().create(getCurrent());
        }

        PatientEncounter tmp = getCurrent().getPatientEncounter();

        current = new PatientItem();
        current.setPatientEncounter(tmp);
        current.setItem(null);
        fromDepartment = null;

        createPatientItems();

        JsfUtil.addSuccessMessage("Added Successfully.");

    }

    /**
     * Creates the Bill and BillItem that carry a newly added timed service.
     * <p>
     * One bill per service, so a bill never mixes items from two departments
     * and each service can be traced to where it was delivered:
     * {@code fromDepartment} is where the service was given (defaulting to the
     * item's department), {@code toDepartment} is the department that entered
     * it. The BillItem holds the charge; the PatientItem keeps holding the
     * timing. Both are kept in step by {@link #syncTimedServiceCharge}.
     */
    private void createBillForTimedService(PatientItem patientItem) {
        PatientEncounter pe = patientItem.getPatientEncounter();
        Department deliveringDepartment = getFromDepartment();
        if (deliveringDepartment == null) {
            deliveringDepartment = getSessionController().getDepartment();
        }

        BilledBill newBill = new BilledBill();
        newBill.setBillType(BillType.InwardBill);
        newBill.setBillTypeAtomic(BillTypeAtomic.INWARD_SERVICE_BILL);
        newBill.setPatient(pe.getPatient());
        newBill.setPatientEncounter(pe);
        newBill.setPaymentScheme(pe.getPaymentScheme());
        newBill.setPaymentMethod(pe.getPaymentMethod());
        newBill.setDepartment(getSessionController().getDepartment());
        newBill.setInstitution(getSessionController().getInstitution());
        newBill.setFromDepartment(deliveringDepartment);
        newBill.setFromInstitution(deliveringDepartment.getInstitution());
        newBill.setToDepartment(getSessionController().getDepartment());
        newBill.setToInstitution(getSessionController().getInstitution());
        newBill.setBillDate(new Date());
        newBill.setBillTime(new Date());
        newBill.setCreatedAt(new Date());
        newBill.setCreater(getSessionController().getLoggedUser());
        newBill.setTotal(patientItem.getServiceValue());
        newBill.setNetTotal(patientItem.getServiceValue());
        newBill.setDeptId(getBillNumberBean().departmentBillNumberGenerator(
                newBill.getDepartment(), BillType.InwardBill, BillClassType.BilledBill, BillNumberSuffix.INWSER));
        newBill.setInsId(getBillNumberBean().institutionBillNumberGenerator(
                newBill.getInstitution(), BillType.InwardBill, BillClassType.BilledBill, BillNumberSuffix.INWSER));
        getBillFacade().create(newBill);

        BillItem newBillItem = new BillItem();
        newBillItem.setBill(newBill);
        newBillItem.setItem(patientItem.getItem());
        newBillItem.setQty(1.0);
        newBillItem.setInwardChargeType(patientItem.getItem().getInwardChargeType());
        newBillItem.setPatientEncounter(pe);
        newBillItem.setGrossValue(patientItem.getServiceValue());
        newBillItem.setNetValue(patientItem.getServiceValue());
        newBillItem.setFromTime(patientItem.getFromTime());
        newBillItem.setToTime(patientItem.getToTime());
        newBillItem.setRequestedFromDepartment(deliveringDepartment);
        newBillItem.setRequestedToDepartment(getSessionController().getDepartment());
        newBillItem.setPeformedDepartment(deliveringDepartment);
        newBillItem.setCreatedAt(new Date());
        newBillItem.setCreater(getSessionController().getLoggedUser());
        getBillItemFacade().create(newBillItem);

        patientItem.setBill(newBill);
        patientItem.setBillItem(newBillItem);
    }

    /**
     * Pushes a recalculated timed-service charge onto its BillItem and Bill.
     * <p>
     * The BillItem is what the inward totals actually sum (see
     * {@code InwardBeanController.calServiceBillItemsTotalByInwardChargeTypeBulk}),
     * so it must never be left holding a stale duration. Package-locked items
     * are skipped — their price is fixed by the package.
     * <p>
     * The discount comes from the BillItem, which is the side the inward
     * discount routines clear when no price matrix applies; reading it from the
     * PatientItem would re-apply a discount that had just been removed. The
     * PatientItem is mirrored back so the breakdown screens still agree.
     */
    private void syncTimedServiceCharge(PatientItem patientItem) {
        if (patientItem == null || patientItem.getBillItem() == null) {
            return;
        }
        BillItem bi = patientItem.getBillItem();
        if (bi.isFromPackage()) {
            return;
        }
        double discount = bi.getDiscount();
        bi.setGrossValue(patientItem.getServiceValue());
        bi.setNetValue(patientItem.getServiceValue() + bi.getMarginValue() - discount);
        bi.setFromTime(patientItem.getFromTime());
        bi.setToTime(patientItem.getToTime());
        getBillItemFacade().edit(bi);

        if (patientItem.getDiscount() != discount) {
            patientItem.setDiscount(discount);
            getPatientItemFacade().edit(patientItem);
        }

        Bill b = bi.getBill();
        if (b != null) {
            b.setTotal(bi.getGrossValue());
            b.setNetTotal(bi.getNetValue());
            getBillFacade().edit(b);
        }
    }

    public void finalizeService(PatientItem pic) {
        if (pic != null && pic.getBillItem() != null && pic.getBillItem().isFromPackage()) {
            JsfUtil.addErrorMessage("This item is included in the admission's package and its charge cannot be changed.");
            return;
        }
        if (pic != null && isLockedForChanges(pic.getPatientEncounter())) {
            return;
        }
        PatientItem temPi;
        if (pic.getToTime() != null && pic.getFromTime() != null) {
            if (pic.getToTime().before(pic.getFromTime())) {
                JsfUtil.addErrorMessage("Service Not Finalize check Service Start Time & End Time");
                return;
            }
            if (pic.getToTime().getTime() == pic.getFromTime().getTime()) {
                JsfUtil.addErrorMessage("Service Start Time & End Time Can't Be Equal");
                return;
            }
        }

        if (pic.getToTime() == null) {
            temPi = new PatientItem();
            try {
                BeanUtils.copyProperties(temPi, pic);
                temPi.setId(null);
                temPi.setToTime(Calendar.getInstance().getTime());
            } catch (IllegalAccessException | InvocationTargetException ex) {
                Logger.getLogger(InwardTimedItemController.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            temPi = pic;
        }

        double value = getInwardBean().calTotalTimedChargeForItem(
                (TimedItem) temPi.getItem(),
                temPi.getFromTime(),
                temPi.getToTime(),
                pic.getPatientEncounter().isForiegner());
        pic.setServiceValue(value);

        getPatientItemFacade().edit(pic);
        syncTimedServiceCharge(pic);

        createPatientItems();

        JsfUtil.addSuccessMessage("Updated Successfully.");

    }

    public void createPatientItems() {
        String sql = "SELECT i FROM PatientItem i where type(i.item)=TimedItem "
                + " and i.retired=false and i.patientEncounter=:pe";
        HashMap hm = new HashMap();
        hm.put("pe", getCurrent().getPatientEncounter());
        items = getPatientItemFacade().findByJpql(sql, hm);

        if (items == null) {
            items = new ArrayList<>();
        }

        for (PatientItem pi : items) {
            double value = getInwardBean().calTotalTimedChargeForItem(
                    (TimedItem) pi.getItem(),
                    pi.getFromTime(),
                    pi.getToTime(),
                    getCurrent().getPatientEncounter().isForiegner());
            pi.setServiceValue(value);
        }
    }

    public List<PatientItem> getItems() {
        return items;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public PatientItemFacade getPatientItemFacade() {
        return patientItemFacade;
    }

    public void setPatientItemFacade(PatientItemFacade patientItemFacade) {
        this.patientItemFacade = patientItemFacade;
    }

    public PatientItem getCurrent() {
        if (current == null) {
            current = new PatientItem();
            current.setFromTime(Calendar.getInstance().getTime());
        }

        return current;
    }

    public void setCurrent(PatientItem current) {
        this.current = current;
    }

    public TimedItemFeeFacade getTimedItemFeeFacade() {
        return timedItemFeeFacade;
    }

    public void setTimedItemFeeFacade(TimedItemFeeFacade timedItemFeeFacade) {
        this.timedItemFeeFacade = timedItemFeeFacade;
    }

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    public Date getFrmDate() {
        if (frmDate == null) {
            frmDate = com.divudi.core.util.CommonFunctions.getStartOfMonth(new Date());
        }
        return frmDate;
    }

    public void setFrmDate(Date frmDate) {
        this.frmDate = frmDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = new Date();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getTotalMins() {
        return totalMins;
    }

    public void setTotalMins(double totalMins) {
        this.totalMins = totalMins;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public void clearDepartment() {
        department = null;
    }

}
