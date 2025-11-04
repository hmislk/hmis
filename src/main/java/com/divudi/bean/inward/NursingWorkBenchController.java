package com.divudi.bean.inward;

import com.divudi.bean.common.AppointmentController;
import com.divudi.bean.common.ClinicalFindingValueController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ControllerWithPatient;
import com.divudi.bean.common.SessionController;

import com.divudi.core.data.ApplicationInstitution;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dataStructure.YearMonthDay;
import com.divudi.core.data.inward.AdmissionStatus;
import com.divudi.core.data.inward.AdmissionTypeEnum;

import com.divudi.core.entity.Appointment;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.EncounterCreditCompany;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.facade.AdmissionFacade;
import com.divudi.core.facade.AppointmentFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.EncounterCreditCompanyFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PatientRoomFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.facade.RoomFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.pharmacy.PharmacyRequestForBhtController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.clinical.ClinicalFindingValueType;
import com.divudi.core.data.dto.PatientRoomDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.clinical.ClinicalFindingValue;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.Reservation;
import com.divudi.core.entity.inward.Room;
import com.divudi.core.facade.ClinicalFindingValueFacade;
import com.divudi.core.util.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import kotlin.collections.ArrayDeque;
import org.primefaces.event.TabChangeEvent;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Named
@SessionScoped
public class NursingWorkBenchController implements Serializable, ControllerWithPatient {

    private static final long serialVersionUID = 1L;

    ////////////
    @EJB
    private AdmissionFacade admissionFacade;
    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    
    
    @Inject
    RoomController roomController;

    private List<PatientRoomDTO> roomList;

    public String navigatetoNursingWorkBench() {
        // Load Room Details
        getPatientRooms();

        return "/nurse/index?faces-redirect=true";
    }

    public List<PatientRoomDTO> getPatientRooms() {

        roomList = new ArrayList<>();

        List<Room> rooms = roomController.getItems();

        for (Room r : rooms) {
            PatientEncounter p = null;
            System.out.println("r = " + r.getName());

            PatientRoomDTO prDTO = new PatientRoomDTO();

            p = getPatientEncounterFromRoom(r);

            if (p != null) {
                prDTO.setAdmissionId(p.getId());
                prDTO.setBhtNo(p.getBhtNo());
                prDTO.setCurrentRoomNo(r.getName());
                prDTO.setInPatient(Boolean.TRUE);
            } else {
                prDTO.setAdmissionId(0L);
                prDTO.setBhtNo("N/A");
                prDTO.setCurrentRoomNo(r.getName());
                prDTO.setInPatient(Boolean.FALSE);
            }

            roomList.add(prDTO);

        }
        System.out.println("rooms = " + rooms.size());
        System.out.println("roomList = " + roomList.size());
        return roomList;
    }

    public PatientEncounter getPatientEncounterFromRoom(Room patientRoom) {
        System.out.println("patientRoom = " + patientRoom.getId());
        String jpql = "select p from "
                + " PatientEncounter p "
                + " where p.discharged =:discharged "
                + " and p.currentPatientRoom.roomFacilityCharge.room =:room ";

        HashMap params = new HashMap();
        params.put("discharged", false);
        params.put("room", patientRoom);

        PatientEncounter current = patientEncounterFacade.findFirstByJpql(jpql, params);

        if (current != null) {
            System.out.println("current = " + current.getId());
            System.out.println("patientRoom = " + current.getCurrentPatientRoom().getRoomFacilityCharge().getRoom().getName());
        }else{
            System.out.println(patientRoom.getName()+ " has no Patient");
        }

        return current;
    }

    @Override
    public Patient getPatient() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setPatient(Patient patient) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isPatientDetailsEditable() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setPatientDetailsEditable(boolean patientDetailsEditable) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void toggalePatientEditable() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void listnerForPaymentMethodChange() {
        throw new UnsupportedOperationException("Not supported yet.");
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
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
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
                        + object.getClass().getName() + "; expected type: " + NursingWorkBenchController.class.getName());
            }
        }
    }
}
