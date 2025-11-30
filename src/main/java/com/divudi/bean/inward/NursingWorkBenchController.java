package com.divudi.bean.inward;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ControllerWithPatient;
import com.divudi.bean.common.SearchController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.pharmacy.PharmacyRequestForBhtController;
import com.divudi.bean.pharmacy.PharmacySaleBhtController;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.facade.AdmissionFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.dto.PatientRoomDTO;
import com.divudi.core.entity.inward.Room;
import java.io.Serializable;
import java.util.ArrayList;
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

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Named
@SessionScoped
public class NursingWorkBenchController implements Serializable {

    private static final long serialVersionUID = 1L;

    ////////////
    @EJB
    private AdmissionFacade admissionFacade;
    @EJB
    private PatientEncounterFacade patientEncounterFacade;

    @Inject
    RoomController roomController;
    @Inject
    AdmissionController admissionController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    SessionController sessionController;
    @Inject
    RoomChangeController roomChangeController;
    @Inject
    BillBhtController billBhtController;
    @Inject
    InwardAdditionalChargeController inwardAdditionalChargeController;
    @Inject
    InwardProfessionalBillController inwardProfessionalBillController;
    @Inject
    InwardTimedItemController inwardTimedItemController;
    @Inject
    SurgeryBillController surgeryBillController;
    @Inject
    PharmacyRequestForBhtController pharmacyRequestForBhtController;
    @Inject
    PharmacySaleBhtController pharmacySaleBhtController;
    @Inject
    SearchController searchController;

    private List<PatientRoomDTO> roomList;
    private List<PatientRoomDTO> bhtList;

    public void clearData() {
        inwardProfessionalBillController.setBatchBill(null);
        inwardProfessionalBillController.getCurrent().setPatientEncounter(null);
        inwardTimedItemController.setBatchBill(null);
        inwardTimedItemController.getCurrent().setPatientEncounter(null);
        surgeryBillController.getSurgeryBill().setPatientEncounter(null);
        inwardAdditionalChargeController.getCurrent().setPatientEncounter(null);
        billBhtController.setPatientEncounter(null);
        admissionController.setCurrent(null);
        roomChangeController.setCurrent(null);
        pharmacyRequestForBhtController.setPatientEncounter(null);
        pharmacySaleBhtController.setPatientEncounter(null);
        searchController.setPatientEncounter(null);
    }

    public String navigateToAdmissionProfilePage(Long admissionId) {
        PatientEncounter encounter = patientEncounterFacade.find(admissionId);

        if (encounter == null) {
            JsfUtil.addErrorMessage("No Admission Found");
            return "";
        }
        if (!(encounter instanceof Admission)) {
            JsfUtil.addErrorMessage("Invalid Admission");
            return "";
        }
        Admission admission = (Admission) encounter;
        admissionController.setCurrent(admission);
        return admissionController.navigateToAdmissionProfilePage();
    }

    public String navigatetoNursingWorkBench() {
        clearData(); // clear data
        getPatientRooms(); // Load Room Details
        getPatientEncounter(); // Load BHT Details
        return "/nurse/index?faces-redirect=true";
    }

    public String navigateToBackNursingWorkBench() {
        clearData();
        return "/nurse/index?faces-redirect=true";
    }

    public List<PatientRoomDTO> getPatientRooms() {

        roomList = new ArrayList<>();
        List<Room> rooms = roomController.getItems();
        if (rooms == null) {
            return roomList;
        }

        for (Room r : rooms) {
            PatientEncounter p = null;
            PatientRoomDTO prDTO = new PatientRoomDTO();
            p = getPatientEncounterFromRoom(r);

            if (p != null) {
                prDTO.setAdmissionId(p.getId());
                prDTO.setBhtNo(p.getBhtNo());
                prDTO.setCurrentRoomNo(r.getName());
                prDTO.setInPatient(Boolean.TRUE);
            } else {
                prDTO.setAdmissionId(null);
                prDTO.setBhtNo("N/A");
                prDTO.setCurrentRoomNo(r.getName());
                prDTO.setInPatient(Boolean.FALSE);
            }
            roomList.add(prDTO);
        }
        return roomList;
    }

    public PatientEncounter getPatientEncounterFromRoom(Room patientRoom) {
        String jpql = "select p from "
                + " PatientEncounter p "
                + " where p.discharged =:discharged "
                + " and p.currentPatientRoom.roomFacilityCharge.room =:room ";

        HashMap params = new HashMap();
        params.put("discharged", false);
        params.put("room", patientRoom);

        PatientEncounter current = patientEncounterFacade.findFirstByJpql(jpql, params);

        return current;
    }

    public List<PatientRoomDTO> getPatientEncounter() {
        bhtList = new ArrayList<>();

        String jpql = "select new com.divudi.core.data.dto.PatientRoomDTO( p.id, p.bhtNo, p.currentPatientRoom.roomFacilityCharge.room.name ) "
                + "from PatientEncounter p "
                + " where p.discharged =:discharged "
                + " order by p.bhtNo asc";

        HashMap params = new HashMap();
        params.put("discharged", false);

        bhtList = patientEncounterFacade.findLightsByJpql(jpql, params);

        return bhtList;
    }

    public List<PatientRoomDTO> getRoomList() {
        return roomList;
    }

    public void setRoomList(List<PatientRoomDTO> roomList) {
        this.roomList = roomList;
    }

    public List<PatientRoomDTO> getBhtList() {
        return bhtList;
    }

    public void setBhtList(List<PatientRoomDTO> bhtList) {
        this.bhtList = bhtList;
    }

    /**
     *
     */
    @FacesConverter(forClass = Admission.class)
    public static class AdmissionControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            NursingWorkBenchController controller = (NursingWorkBenchController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "nursingWorkBenchController");
            if (controller == null) {
                return null;
            }
            Long l = getKey(value);
            if (l == null) {
                return null;
            }
            return controller.admissionFacade.find(l);
        }

        java.lang.Long getKey(String value) {
            if (value == null) {
                return null;
            }
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        String getStringKey(java.lang.Long value) {
            if (value == null) {
                return null;
            }
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
                        + object.getClass().getName() + "; expected type: " + Admission.class.getName());
            }
        }
    }
}
