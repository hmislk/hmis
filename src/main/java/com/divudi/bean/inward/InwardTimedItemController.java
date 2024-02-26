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

import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.DepartmentType;
import com.divudi.data.inward.SurgeryBillType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Item;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.PatientItem;
import com.divudi.entity.inward.EncounterComponent;
import com.divudi.entity.inward.TimedItem;
import com.divudi.entity.inward.TimedItemFee;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.EncounterComponentFacade;
import com.divudi.facade.PatientItemFacade;
import com.divudi.facade.TimedItemFeeFacade;
import com.divudi.java.CommonFunctions;
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

    private CommonFunctions commonFunctions;
    @Inject
    private InwardBeanController inwardBean;
    @Inject
    BillBeanController billBean;
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

        timedEncounterComponent.setPatientEncounter(getBatchBill().getPatientEncounter());
        timedEncounterComponent.setChildEncounter(getBatchBill().getProcedure());
        timedEncounterComponent.setOrderNo(getTimedEncounterComponents().size());
        getTimedEncounterComponents().add(timedEncounterComponent);

        saveSurgeryTimedService();

        timedEncounterComponent = null;
    }

    private double savePatientItem(PatientItem patientItem) {
        TimedItemFee timedItemFee = getInwardBean().getTimedItemFee((TimedItem) patientItem.getItem());
        double count = getInwardBean().calCount(timedItemFee, patientItem.getFromTime(), patientItem.getToTime());

        patientItem.setServiceValue(count * timedItemFee.getFee());
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
        if (bf.getPatientItem().getToTime() != null) {
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
        if (patientItem != null) {
            patientItem.setRetirer(getSessionController().getLoggedUser());
            patientItem.setRetiredAt(new Date());
            patientItem.setRetired(true);
            getPatientItemFacade().edit(patientItem);
            
            createPatientItems();
            
            JsfUtil.addSuccessMessage("Removed successfully.");
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

    }

    public String navigateToAddInwardTimedServicesFromMenu() {
        makeNull();
        return "/inward/inward_timed_service_consume";
    }

    public String navigateToAddInwardTimedServicesFromInpatientProfile() {
        return "/inward/inward_timed_service_consume";
    }

    public String navigateToAddInwardTimedServiceForTheatreFromMenu() {
        makeNull();
        return "/theater/inward_timed_service_consume_surgery";
    }

    public String navigateToAddInwardTimedServiceForTheatreFromInpatientProfile() {
        makeNull();
        return "/theater/inward_timed_service_consume_surgery";
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
        return false;
    }

    public void save() {
        if (errorCheck()) {
            return;
        }
        TimedItemFee timedItemFee = getInwardBean().getTimedItemFee((TimedItem) getCurrent().getItem());
        if (getCurrent().getToTime() == null) {
            getCurrent().setToTime(new Date());
        }
        double count = getInwardBean().calCount(timedItemFee, getCurrent().getPatientEncounter().getDateOfAdmission(), getCurrent().getToTime());

        getCurrent().setServiceValue(count * timedItemFee.getFee());
        getCurrent().setCreater(getSessionController().getLoggedUser());
        getCurrent().setCreatedAt(Calendar.getInstance().getTime());
        if (getCurrent().getId() == null) {
            getPatientItemFacade().create(getCurrent());
        }

        PatientEncounter tmp = getCurrent().getPatientEncounter();

        current = new PatientItem();
        current.setPatientEncounter(tmp);
        current.setItem(null);

        createPatientItems();
        
        JsfUtil.addSuccessMessage("Added Successfully.");

    }

    public void finalizeService(PatientItem pic) {
        PatientItem temPi;
        if (pic.getToTime() != null) {
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

        TimedItemFee timedItemFee = getInwardBean().getTimedItemFee((TimedItem) temPi.getItem());
        double count = getInwardBean().calCount(timedItemFee, temPi.getFromTime(), temPi.getToTime());

        pic.setServiceValue(count * timedItemFee.getFee());

        getPatientItemFacade().edit(pic);

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
            TimedItemFee timedItemFee = getInwardBean().getTimedItemFee((TimedItem) pi.getItem());
            double count = getInwardBean().calCount(timedItemFee, pi.getFromTime(), pi.getToTime());
            pi.setServiceValue(count * timedItemFee.getFee());
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

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    public Date getFrmDate() {
        if (frmDate == null) {
            frmDate = com.divudi.java.CommonFunctions.getStartOfMonth(new Date());
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

}
