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

    public void selectAdmission(Long admissionId) {
        PatientEncounter encounter = patientEncounterFacade.find(admissionId);
        if (encounter == null || !(encounter instanceof Admission)) {
            JsfUtil.addErrorMessage("No Admission Found");
            return;
        }
        admissionController.setCurrent((Admission) encounter);
    }

    public String navigatetoNursingWorkBench() {
        clearData();
        loadLists();
        return "/nurse/index?faces-redirect=true";
    }

    public String navigateToBackNursingWorkBench() {
        Admission savedCurrent = admissionController.getCurrent();
        clearData();
        admissionController.setCurrent(savedCurrent);
        loadLists();
        return "/nurse/index?faces-redirect=true";
    }

    private void loadLists() {
        bhtList = new ArrayList<>();
        roomList = new ArrayList<>();
        if (sessionController.getDepartment() == null) {
            return;
        }
        String baseCondition = "from PatientEncounter p "
                + "where p.discharged = false "
                + "and p.paymentFinalized = false "
                + "and p.currentPatientRoom.roomFacilityCharge.department =:department ";
        HashMap params = new HashMap();
        params.put("department", sessionController.getDepartment());

        bhtList = patientEncounterFacade.findLightsByJpql(
                "select new com.divudi.core.data.dto.PatientRoomDTO( p.id, p.bhtNo, p.currentPatientRoom.roomFacilityCharge.room.name ) "
                + baseCondition + "order by p.bhtNo asc", params);

        roomList = patientEncounterFacade.findLightsByJpql(
                "select new com.divudi.core.data.dto.PatientRoomDTO( p.id, p.bhtNo, p.currentPatientRoom.roomFacilityCharge.room.name ) "
                + baseCondition + "order by p.currentPatientRoom.roomFacilityCharge.room.name asc, p.bhtNo asc", params);
    }

    public List<PatientRoomDTO> getPatientEncounter() {
        return bhtList;
    }

    public List<PatientRoomDTO> getBhtList() {
        return bhtList;
    }

    public void setBhtList(List<PatientRoomDTO> bhtList) {
        this.bhtList = bhtList;
    }

    public List<PatientRoomDTO> getRoomList() {
        return roomList;
    }

    public void setRoomList(List<PatientRoomDTO> roomList) {
        this.roomList = roomList;
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
