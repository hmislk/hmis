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
import com.divudi.bean.common.UtilityController;
import com.divudi.data.Sex;
import com.divudi.data.Title;
import com.divudi.data.dataStructure.YearMonthDay;
import com.divudi.data.inward.SurgeryBillType;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.Patient;
import com.divudi.entity.Person;
import com.divudi.entity.inward.Admission;
import com.divudi.entity.inward.EncounterComponent;
import com.divudi.entity.inward.PatientRoom;
import com.divudi.facade.AdmissionFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PatientRoomFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.RoomFacade;
import java.io.Serializable;
import java.util.ArrayList;
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

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class BhtEditController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    BillBeanController billBean;
    /////////////
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
    ////////////////
    private List<PatientRoom> patientRoom;
    List<Admission> selectedItems;
    private List<Admission> items = null;
    private List<Patient> patientList;
    /////////////
    private Admission current;
    String selectText = "";
    @EJB
    private BillFacade billFacade;
    String comment;

    @Inject
    InwardStaffPaymentBillController inwardStaffPaymentBillController;
    @Inject
    CommonFunctions commonFunctions;

    YearMonthDay yearMonthDay;

    public void resetSpecialities() {
        if (current == null) {
            return;
        }
        if (current.getOpdDoctor() != null) {
            getInwardStaffPaymentBillController().setSpeciality(current.getOpdDoctor().getSpeciality());
        }
        if (current.getReferringDoctor() != null) {
            getInwardStaffPaymentBillController().setReferringDoctorSpeciality(current.getReferringDoctor().getSpeciality());
        }
    }

    private boolean checkPaymentIsMade() {
        boolean flag = false;
        String sql = "select b from BilledBill b"
                + "  where b.retired=false "
                + " and b.patientEncounter=:pEnc "
                + " and b.cancelled=false ";
        HashMap hm = new HashMap();
        hm.put("pEnc", current);
        List<Bill> bills = getBillFacade().findBySQL(sql, hm);
        if (bills.isEmpty()) {
            return flag;
        } else if (bills.size() == 1) {
            Bill b = bills.get(0);
            if (b.getSurgeryBillType() == SurgeryBillType.TimedService) {
                List<EncounterComponent> enc = getBillBean().getEncounterComponents(b);
                for (EncounterComponent e : enc) {
                    if (!e.getBillFee().getPatientItem().isRetired()) {
                        flag = true;
                    }
                }
            } else {
                flag=true;
            }
        }
        if (bills.size() > 1) {
            flag = true;
        }
        return flag;
    }

//      private boolean checkServiceAdded() {
//        String sql = "select b from BilledBill b"
//                + "  where b.retired=false "
//                + " and b.patientEncounter=:pEnc "
//                + " and b.cancelled=false ";
//        HashMap hm = new HashMap();
//        hm.put("pEnc", current);
//        Bill bill = getBillFacade().findFirstBySQL(sql, hm);
//        if (bill != null) {
//            return true;
//        }
//
//        return false;
//    }
    public void cancelBht() {
        if (current == null) {
            return;
        }

        if (checkPaymentIsMade()) {
            UtilityController.addErrorMessage("Some Is made for this Bht please cancel all bills added for this bht ");
            return;
        }

        if (getComment() == null || getComment().trim().equals("")) {
            ////// // System.out.println("comment = " + comment);
            UtilityController.addErrorMessage("Type a Comment");
            return;
        }

        //Net to check if Any Payment Paid for this BHT
        for (PatientRoom pr : getPatientRoom()) {
            pr.setRetired(true);
            pr.setDischarged(true);
            pr.setRetiredAt(new Date());
            pr.setRetirer(getSessionController().getLoggedUser());
            getPatientRoomFacade().edit(pr);
        }
        current.setRetired(true);
        current.setRetireComments("BHT Cancel");
        current.setRetiredAt(new Date());
        current.setRetirer(getSessionController().getLoggedUser());
        current.setComments(comment);
        getEjbFacade().edit(current);

        UtilityController.addSuccessMessage("Bht Successfully Cancelled");
        makeNull();
    }

    public Title[] getTitle() {
        return Title.values();
    }

    public Sex[] getSex() {
        return Sex.values();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<Admission> getSelectedItems() {
        selectedItems = getFacade().findBySQL("select c from Admission c where c.retired=false and c.discharged!=true and upper(c.bhtNo) like '%" + getSelectText().toUpperCase() + "%' or upper(c.patient.person.name) like '%" + getSelectText().toUpperCase() + "%' order by c.bhtNo");
        return selectedItems;
    }

    public List<Admission> completePatient(String query) {
        List<Admission> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Admission c where c.retired=false and c.discharged=false and (upper(c.bhtNo) like '%" + query.toUpperCase() + "%' or upper(c.patient.person.name) like '%" + query.toUpperCase() + "%') order by c.bhtNo";
            //////// // System.out.println(sql);
            suggestions = getFacade().findBySQL(sql);
        }
        return suggestions;
    }

    public List<Admission> completePatientAll(String query) {
        List<Admission> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Admission c where "
                    + " c.retired=false "
                    //                    + " and c.discharged=false "
                    + " and (upper(c.bhtNo) like '%" + query.toUpperCase() + "%' or upper(c.patient.person.name) like '%" + query.toUpperCase() + "%') "
                    + " order by c.bhtNo ";
            //////// // System.out.println(sql);
            suggestions = getFacade().findBySQL(sql);
        }
        return suggestions;
    }

    public void prepareAdd() {
        current = new Admission();
    }

    public void delete() {

        if (getCurrent() != null) {
            getCurrent().setRetired(true);
            getCurrent().setRetiredAt(new Date());
            getCurrent().setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(getCurrent());
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        makeNull();
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

    public void makeNull() {
        patientRoom = null;
        selectedItems = null;
        items = null;
        patientList = null;
        current = null;
        selectText = "";
        yearMonthDay = new YearMonthDay();
    }

    public void save() {
        getPatientFacade().edit(getCurrent().getPatient());
        getPersonFacade().edit(getCurrent().getPatient().getPerson());
        getPersonFacade().edit(getCurrent().getGuardian());
        updateFirstPatientRoomAdmissionTime();
        getEjbFacade().edit(current);
        if (current.getFinalBill() != null) {
            getBillFacade().edit(current.getFinalBill());
            UtilityController.addSuccessMessage("Final Bill Updated");
        }
        UtilityController.addSuccessMessage("Detail Updated");
        makeNull();
    }

    public void updateFirstPatientRoomAdmissionTime() {
        HashMap hm = new HashMap();
        String sql = "SELECT pr FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter=:pe"
                + "  order by pr.admittedAt";
        hm.put("pe", getCurrent());
        PatientRoom tmp = getPatientRoomFacade().findFirstBySQL(sql, hm);

        if (tmp == null) {
            return;
        }

        tmp.setAdmittedAt(getCurrent().getDateOfAdmission());
        getPatientRoomFacade().edit(tmp);
    }

    public void dateChangeListen() {
        getCurrent().getPatient().getPerson().setDob(getCommonFunctions().guessDob(yearMonthDay));

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

    public BhtEditController() {
    }

    public Admission getCurrent() {
        if (current == null) {
            current = new Admission();
            Person p = new Person();
            Patient pp = new Patient();
            pp.setPerson(p);
            current.setPatient(pp);
        }
        return current;
    }

    public void setCurrent(Admission current) {
        this.current = current;
        createPatientRoom();
        if (current == null) {
            getYearMonthDay().setDay("0");
            getYearMonthDay().setMonth("0");
            getYearMonthDay().setYear("0");
        } else {
            getYearMonthDay().setDay(current.getPatient().getAgeDays() + "");
            getYearMonthDay().setMonth(current.getPatient().getAgeMonths() + "");
            getYearMonthDay().setYear(current.getPatient().getAgeYears() + "");
        }
    }

    private AdmissionFacade getFacade() {
        return ejbFacade;
    }

    public List<Admission> getItems() {
        if (items == null) {
            String temSql;
            temSql = "SELECT i FROM Admission i where i.retired=false and i.discharged=false order by i.bhtNo";
            items = getFacade().findBySQL(temSql);
            if (items == null) {
                items = new ArrayList<Admission>();
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
            patientList = getPatientFacade().findBySQL(temSql);
        }
        return patientList;
    }

    public void setPatientList(List<Patient> patientList) {
        this.patientList = patientList;
    }

    public List<PatientRoom> getPatientRoom() {

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

    private void createPatientRoom() {

        HashMap hm = new HashMap();
        String sql = "SELECT pr FROM PatientRoom pr where pr.retired=false"
                + " and pr.patientEncounter=:pe order by pr.createdAt";
        hm.put("pe", getCurrent());
        patientRoom = getPatientRoomFacade().findBySQL(sql, hm);

    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public InwardStaffPaymentBillController getInwardStaffPaymentBillController() {
        return inwardStaffPaymentBillController;
    }

    public void setInwardStaffPaymentBillController(InwardStaffPaymentBillController inwardStaffPaymentBillController) {
        this.inwardStaffPaymentBillController = inwardStaffPaymentBillController;
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

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    /**
     *
     */
    @FacesConverter("bhtEdit")
    public static class DischargeControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            BhtEditController controller = (BhtEditController) facesContext.getApplication().getELResolver().
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
                        + object.getClass().getName() + "; expected type: " + BhtEditController.class.getName());
            }
        }
    }
}
