/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;

import com.divudi.data.inward.AdmissionTypeEnum;
import com.divudi.entity.Patient;
import com.divudi.entity.Person;
import com.divudi.entity.inward.Admission;
import com.divudi.entity.inward.GuardianRoom;
import com.divudi.entity.inward.PatientRoom;
import com.divudi.entity.inward.RoomFacilityCharge;
import com.divudi.facade.AdmissionFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PatientRoomFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.RoomFacade;
import com.divudi.facade.RoomFacilityChargeFacade;
import com.divudi.bean.common.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class RoomChangeController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    private RoomFacilityChargeController roomFacilityChargeController;
    @EJB
    private AdmissionFacade ejbFacade;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private PatientRoomFacade patientRoomFacade;
    @EJB
    private RoomFacade roomFacade;
    private List<PatientRoom> patientRoom;
    List<Admission> selectedItems;
    private Admission current;
    private List<Admission> items = null;
    private List<Patient> patientList;
    String selectText = "";
    private RoomFacilityCharge newRoomFacilityCharge;
    @Temporal(TemporalType.TIMESTAMP)
    private Date changeAt;
    private double addLinenCharge = 0.0;

    public void update(PatientRoom pR) {
        if (pR == null || pR.getRoomFacilityCharge() == null) {
            return;
        }

        pR.setCurrentAdministrationCharge(pR.getRoomFacilityCharge().getAdminstrationCharge());
        pR.setCurrentLinenCharge(pR.getRoomFacilityCharge().getLinenCharge());
        pR.setCurrentMaintananceCharge(pR.getRoomFacilityCharge().getMaintananceCharge());
        pR.setCurrentMedicalCareCharge(pR.getRoomFacilityCharge().getMedicalCareCharge());
        pR.setCurrentMoCharge(pR.getRoomFacilityCharge().getMoCharge());
        pR.setCurrentNursingCharge(pR.getRoomFacilityCharge().getNursingCharge());
        pR.setCurrentRoomCharge(pR.getRoomFacilityCharge().getRoomCharge());

        getPatientRoomFacade().edit(pR);
    }

    public void remove(PatientRoom pR) {
        if(pR==null){
            JsfUtil.addErrorMessage("No Patient Room Detected");
            return;
        }
        
        AdmissionTypeEnum admissionTypeEnum = pR.getPatientEncounter().getAdmissionType().getAdmissionTypeEnum();

        if (admissionTypeEnum == AdmissionTypeEnum.Admission
                && pR.getPreviousRoom() == null) {
            JsfUtil.addErrorMessage("To Delete Patient Room There should be Previus room U can ReSet Correct Room Facility and update");
            return;
        }
        
        if (admissionTypeEnum == AdmissionTypeEnum.Admission
                && pR.getNextRoom() != null && !pR.getNextRoom().isRetired() && pR.getPreviousRoom() != null 
                && !pR.getPreviousRoom().isRetired()) {
            JsfUtil.addErrorMessage("You have to Remove Last one First");
            return;
        }
        
        
        if (admissionTypeEnum == AdmissionTypeEnum.Admission
                && pR.getNextRoom() != null && !pR.getNextRoom().isRetired()) {
            JsfUtil.addErrorMessage("To Delete Patient Room There next Room Should Be Empty");
            return;
        }

        pR.setRetirer(getSessionController().getLoggedUser());
        pR.setRetired(true);
        pR.setRetiredAt(new Date());
        getPatientRoomFacade().edit(pR);

        if (admissionTypeEnum == AdmissionTypeEnum.Admission) {
            pR.getPreviousRoom().setDischarged(false);
            pR.getPreviousRoom().setDischargedAt(null);
            pR.getPreviousRoom().setDischargedBy(null);
            getPatientRoomFacade().edit(pR.getPreviousRoom());
            getCurrent().setCurrentPatientRoom(pR.getPreviousRoom());
            getEjbFacade().edit(getCurrent());
        }
    }

    public void discharge(PatientRoom pR) {
        if (pR.getDischargedAt() == null) {
            JsfUtil.addErrorMessage("Please Select Discharge Date");
            return;
        }

        pR.setDischarged(true);
        pR.setDischargedBy(getSessionController().getLoggedUser());
        getPatientRoomFacade().edit(pR);
    }

    public void dischargeWithCurrentTime(PatientRoom pR) {
        if (pR.getDischargedAt() == null) {
            pR.setDischargedAt(new Date());
        }

        pR.setDischarged(true);
        pR.setDischargedBy(getSessionController().getLoggedUser());
        getPatientRoomFacade().edit(pR);
    }

    public void dischargeCancel(PatientRoom pR) {
        pR.setDischarged(false);
        pR.setDischargedBy(null);
        getPatientRoomFacade().edit(pR);
        
    }

    public void removeGuardianRoom(PatientRoom pR) {

        if (pR.getNextRoom() != null && !pR.getNextRoom().isRetired()) {
            JsfUtil.addErrorMessage("To Delete Patient Room There next Room Should Be Empty");
            return;
        }

        pR.setRetirer(getSessionController().getLoggedUser());
        pR.setRetired(true);
        pR.setRetiredAt(new Date());
        getPatientRoomFacade().edit(pR);

        if (pR.getPreviousRoom() != null) {
            pR.getPreviousRoom().setDischarged(false);
            pR.getPreviousRoom().setDischargedAt(null);
            pR.getPreviousRoom().setDischargedBy(null);
            getPatientRoomFacade().edit(pR.getPreviousRoom());
        }

    }

    public void recreate() {
        patientRoom = null;
        selectedItems = null;
        current = null;
        items = null;
        patientList = null;
        changeAt = null;
        newRoomFacilityCharge = null;
    }

    private PatientRoom updatePatientRoom(PatientRoom patientRoom1) {
        if (patientRoom1 == null) {
            return null;
        }

        if (patientRoom1.getAdmittedAt() != null
                && patientRoom1.getAdmittedAt().getTime() > getChangeAt().getTime()) {
            JsfUtil.addErrorMessage("U cant discharge early date than admitted");
            return null;
        }

        patientRoom1.setDischarged(true);
        patientRoom1.setDischargedAt(getChangeAt());
        patientRoom1.setDischargedBy(getSessionController().getLoggedUser());
        getPatientRoomFacade().edit(patientRoom1);
        return patientRoom1;
    }

    @Inject
    private InwardBeanController inwardBean;

    public void change() {
        if (getCurrent().getCurrentPatientRoom() == null) {
            return;
        }

        PatientRoom oldPatientRoom = updatePatientRoom(getCurrent().getCurrentPatientRoom());

        if (oldPatientRoom == null) {
            return;
        }

        if (sessionController.getApplicationPreference().isInwardMoChargeCalculateInitialTime()) {
            if (errorCheck()) {
                return;
            }
        }

        PatientRoom newPatientRoom = new PatientRoom();
        newPatientRoom = getInwardBean().savePatientRoom(newPatientRoom, oldPatientRoom, getNewRoomFacilityCharge(), current, changeAt, getSessionController().getLoggedUser());
        getCurrent().setCurrentPatientRoom(newPatientRoom);
        getEjbFacade().edit(getCurrent());

        oldPatientRoom.setNextRoom(newPatientRoom);
        getPatientRoomFacade().edit(oldPatientRoom);

        JsfUtil.addSuccessMessage("Successfully Room Changed");

        // recreate();
        newRoomFacilityCharge = null;
        changeAt = null;
        createPatientRoom();
    }

    public void addNewRoom() {
        if (sessionController.getApplicationPreference().isInwardMoChargeCalculateInitialTime()) {
            if (errorCheck()) {
                return;
            }
        }

        PatientRoom newPatientRoom = new PatientRoom();
        newPatientRoom = getInwardBean().savePatientRoom(newPatientRoom, getNewRoomFacilityCharge(), current, changeAt, getSessionController().getLoggedUser());
        getCurrent().setCurrentPatientRoom(newPatientRoom);
        getEjbFacade().edit(getCurrent());

        JsfUtil.addSuccessMessage("Successfully Room Changed");

        // recreate();
        newRoomFacilityCharge = null;
        changeAt = null;
        createPatientRoom();
    }

    public boolean errorCheck() {
        if (getNewRoomFacilityCharge().getTimedItemFee().getDurationDaysForMoCharge() == 0.0) {
            JsfUtil.addErrorMessage("Plase Add Duration Days For Mo Charge");
            return true;
        }
        if (getNewRoomFacilityCharge().getMoChargeForAfterDuration().equals(null) || getNewRoomFacilityCharge().getMoChargeForAfterDuration().equals("") || getNewRoomFacilityCharge().getMoChargeForAfterDuration().equals(0.0)) {
            JsfUtil.addErrorMessage("Plase Add Charge for After Duration Days");
            return true;
        }
        return false;
    }

    private PatientRoom getLastGuardianRoom() {

        HashMap hm = new HashMap();
        String sql = "SELECT pr FROM GuardianRoom pr "
                + " where pr.patientEncounter=:pe "
                + " order by pr.admittedAt desc";
        hm.put("pe", getCurrent());
        return getPatientRoomFacade().findFirstByJpql(sql, hm);
    }

    public void changeGurdianRoom() {

        PatientRoom oldGaurdianRoom = updatePatientRoom(getLastGuardianRoom());

        GuardianRoom newGuardianRoom = new GuardianRoom();
        getInwardBean().savePatientRoom(newGuardianRoom, oldGaurdianRoom, getNewRoomFacilityCharge(), current, changeAt, getSessionController().getLoggedUser());
        //  getCurrent().setCurrentPatientRoom(cuPatientRoom);
        //     getEjbFacade().edit(getCurrent());

        if (oldGaurdianRoom != null) {
            oldGaurdianRoom.setNextRoom(newGuardianRoom);
            getPatientRoomFacade().edit(oldGaurdianRoom);
        }

        JsfUtil.addSuccessMessage("Successfully Room Changed");
        newRoomFacilityCharge = null;
        changeAt = null;
        createGuardianRoom();
    }

    public List<Admission> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from Admission c where c.retired=false and c.discharged!=true and (c.bhtNo) like '%" + getSelectText().toUpperCase() + "%' or (c.patient.person.name) like '%" + getSelectText().toUpperCase() + "%' order by c.bhtNo");
        return selectedItems;
    }

    @EJB
    private RoomFacilityChargeFacade roomFacilityChargeFacade;

    public void prepareAdd() {
        current = new Admission();
    }

    public void delete() {

        if (getCurrent() != null) {
            getCurrent().setRetired(true);
            getCurrent().setRetiredAt(new Date());
            getCurrent().setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    public void setSelectedItems(List<Admission> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        current = null;
        items = null;
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public AdmissionFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(AdmissionFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public RoomChangeController() {
    }

    public Admission getCurrent() {
       
        return current;
    }

    public void setCurrent(Admission current) {
        this.current = current;

    }

    public void createPatientRoom() {

        HashMap hm = new HashMap();
        String sql = "SELECT pr FROM PatientRoom pr "
                + " where pr.patientEncounter=:pe "
                + " and type(pr)!=:class "
                + " order by pr.admittedAt";
        hm.put("pe", getCurrent());
        hm.put("class", GuardianRoom.class);
        patientRoom = getPatientRoomFacade().findByJpql(sql, hm);

    }

    public void createGuardianRoom() {

        HashMap hm = new HashMap();
        String sql = "SELECT pr FROM GuardianRoom pr "
                + " where pr.patientEncounter=:pe "
                + " order by pr.admittedAt";
        hm.put("pe", getCurrent());
        patientRoom = getPatientRoomFacade().findByJpql(sql, hm);

    }

    private AdmissionFacade getFacade() {
        return ejbFacade;
    }

    public List<Admission> getItems() {
        if (items == null) {
            String temSql;
            temSql = "SELECT i FROM Admission i where i.retired=false and i.discharged=false order by i.bhtNo";
            items = getFacade().findByJpql(temSql);
            if (items == null) {
                items = new ArrayList<>();
            }
        }

        return items;
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

    public List<Patient> getPatientList() {
        if (patientList == null) {
            String temSql;
            temSql = "SELECT i FROM Patient i where i.retired=false ";
            patientList = getPatientFacade().findByJpql(temSql);
        }
        return patientList;
    }

    public void setPatientList(List<Patient> patientList) {
        this.patientList = patientList;
    }

    public List<PatientRoom> getPatientRoom() {
        if (patientRoom == null) {
            patientRoom = new ArrayList<>();
        }
        return patientRoom;
    }

    public void setPatientRoom(List<PatientRoom> patientRoom) {
        this.patientRoom = patientRoom;
    }

    public PatientRoomFacade getPatientRoomFacade() {
        return patientRoomFacade;
    }

    public void setPatientRoomFacade(PatientRoomFacade patientRoomFacade) {
        this.patientRoomFacade = patientRoomFacade;
    }

    public RoomFacade getRoomFacade() {

        return roomFacade;
    }

    public void setRoomFacade(RoomFacade roomFacade) {
        this.roomFacade = roomFacade;
    }

    public RoomFacilityCharge getNewRoomFacilityCharge() {
        return newRoomFacilityCharge;
    }

    public void setNewRoomFacilityCharge(RoomFacilityCharge newRoomFacilityCharge) {
        this.newRoomFacilityCharge = newRoomFacilityCharge;
    }

    public RoomFacilityChargeFacade getRoomFacilityChargeFacade() {
        return roomFacilityChargeFacade;
    }

    public void setRoomFacilityChargeFacade(RoomFacilityChargeFacade roomFacilityChargeFacade) {
        this.roomFacilityChargeFacade = roomFacilityChargeFacade;
    }

    public RoomFacilityChargeController getRoomFacilityChargeController() {
        return roomFacilityChargeController;
    }

    public void setRoomFacilityChargeController(RoomFacilityChargeController roomFacilityChargeController) {
        this.roomFacilityChargeController = roomFacilityChargeController;
    }

    public Date getChangeAt() {
        if (changeAt == null) {
            changeAt = Calendar.getInstance().getTime();
        }
        return changeAt;
    }

    public void setChangeAt(Date changeAt) {
        this.changeAt = changeAt;
    }

    public double getAddLinenCharge() {
        return addLinenCharge;
    }

    public void setAddLinenCharge(double addLinenCharge) {
        this.addLinenCharge = addLinenCharge;
    }

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    /**
     *
     */
    @FacesConverter("rcc")
    public static class DischargeControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            RoomChangeController controller = (RoomChangeController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "dischargeController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Admission) {
                Admission o = (Admission) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + RoomChangeController.class.getName());
            }
        }
    }
}
