/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.clinical;

import com.divudi.bean.channel.ChannelBillController;
import com.divudi.bean.channel.ChannelReportController;
import com.divudi.bean.channel.ChannelSearchController;
import com.divudi.bean.common.BillController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.PatientController;
import com.divudi.bean.common.SessionController;

import com.divudi.bean.pharmacy.PharmacySaleController;
import com.divudi.data.BillType;
import com.divudi.data.inward.PatientEncounterType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.ChannelBean;
import com.divudi.ejb.ServiceSessionBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Doctor;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.Person;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.SessionNumberGenerator;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.DoctorFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.PatientEncounterFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.SessionNumberGeneratorFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.bean.common.util.JsfUtil;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class PracticeBookingController implements Serializable {

    private Speciality speciality;
    private ServiceSession selectedServiceSession;
    private BillSession selectedBillSession;
    Doctor doctor;
    ////////////////////
    private List<ServiceSession> serviceSessions;
    private List<BillSession> billSessions;
    List<BillSession> completedSessions;
    List<BillSession> toCompleteSessions;
    ////////////////////
    @Inject
    BillController billController;
    @Inject
    PharmacySaleController pharmacySaleController;
    @Inject
    private SessionController sessionController;
    @Inject
    private ChannelBillController channelCancelController;
    @Inject
    private ChannelReportController channelReportController;
    @Inject
    private ChannelSearchController channelSearchController;
    @Inject
    private PatientController patientController;
    @Inject
    CommonController commonController; 
    ///////////////////
    @EJB
    DoctorFacade doctorFacade;
    @EJB
    private StaffFacade staffFacade;
    @EJB
    private ServiceSessionFacade serviceSessionFacade;
    @EJB
    private BillSessionFacade billSessionFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private ChannelBean channelBean;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    ServiceSessionBean serviceSessionBean;
    @EJB
    PatientEncounterFacade patientEncounterFacade;
    //
    private boolean foreigner;

    BillSession billSession;
    PatientEncounter opdVisit;
    Date sessionDate;
    Date fromDate;
    Date toDate;

    public Date getSessionDate() {
        if (sessionDate == null) {
            sessionDate = new Date();
        }
        return sessionDate;
    }

    public void setSessionDate(Date sessionDate) {
        this.sessionDate = sessionDate;
    }

    public Date getFromDate() {
        if (toDate == null) {
            toDate = new Date();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (sessionDate == null) {
            sessionDate = new Date();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public void markAsAbent() {
        if (billSession == null) {
            JsfUtil.addErrorMessage("Select a visit to mark as absent");
            return;
        }
        if (billSession.isAbsent()) {
            JsfUtil.addErrorMessage("Already marked as absent. Nothing done");
            return;
        }

        billSession.setAbsent(true);
        billSession.setAbsentMarkedAt(new Date());
        billSession.setAbsentMarkedUser(getSessionController().getLoggedUser());

        getBillSessionFacade().edit(billSession);

        JsfUtil.addSuccessMessage("Marked as Absent");
    }

    public void markAsPresent() {
        if (billSession == null) {
            JsfUtil.addErrorMessage("Select a visit to mark as absent");
            return;
        }
        if (!billSession.isAbsent()) {
            JsfUtil.addErrorMessage("Not marked as absent. So can not mark as present. Nothing done");
            return;
        }
        billSession.setAbsent(false);
        billSession.setAbsentUnmarkedAt(new Date());
        billSession.setAbsentUnmarkedUser(getSessionController().getLoggedUser());

        getBillSessionFacade().edit(billSession);

        JsfUtil.addSuccessMessage("Marked as Absent");
    }

    public List<Doctor> completeDoctorsOfSelectedSpeciality(String qry) {
        String sql;
        Map m = new HashMap();
        List<Doctor> docs;
        if (speciality == null) {
            sql = "select d from Doctor d where d.retired=false order by d.person.name";
            docs = getDoctorFacade().findByJpql(sql);
        } else {
            sql = "select d from Doctor d where d.retired=false and d.speciality=:sp order by d.person.name";
            m.put("sp", speciality);
            docs = getDoctorFacade().findByJpql(sql, m);
        }
        return docs;
    }

    public PatientEncounterFacade getPatientEncounterFacade() {
        return patientEncounterFacade;
    }

    public void setPatientEncounterFacade(PatientEncounterFacade patientEncounterFacade) {
        this.patientEncounterFacade = patientEncounterFacade;
    }

    public BillSession getBillSession() {
        return billSession;
    }

    public void setBillSession(BillSession billSession) {
        this.billSession = billSession;
    }

    public PatientEncounter getOpdVisit() {
        return opdVisit;
    }

    public void setOpdVisit(PatientEncounter opdVisit) {
        this.opdVisit = opdVisit;
    }

    public String opdVisitFromQueue() {
        //   ////// // System.out.println("opd visit from queue");
        if (billSession == null) {
            JsfUtil.addErrorMessage("Please select encounter");
            opdVisit = null;
            return "";
        }
        opdVisitFromServiceSession();
        getPatientEncounterController().setCurrent(opdVisit);
        getPatientEncounterController().setStartedEncounter(opdVisit);
        return "clinical_new_opd_visit";
    }

    @Inject
    PatientEncounterController patientEncounterController;

    public PatientEncounterController getPatientEncounterController() {
        return patientEncounterController;
    }

    public void setPatientEncounterController(PatientEncounterController patientEncounterController) {
        this.patientEncounterController = patientEncounterController;
    }

    public String issueServices() {
        if (billSession == null) {
            JsfUtil.addErrorMessage("Please select encounter");
            opdVisit = null;
            return "";
        }
        Map m = new HashMap();
        m.put("bs", getBillSession());
        String sql;
        sql = "select pe from PatientEncounter pe where pe.billSession=:bs";
        opdVisit = getPatientEncounterFacade().findFirstByJpql(sql, m);
        //   ////// // System.out.println("opdVisit = " + opdVisit);
        if (opdVisit == null) {
            //   ////// // System.out.println("going for a new opd visit = ");
            newOpdVisit();
        }
        if (opdVisit == null) {
            JsfUtil.addErrorMessage("Can not create an opd encounter");
            return "";
        }
        getBillController().prepareNewBill();
        getBillController().setSearchedPatient(opdVisit.getPatient());
        getBillController().setFromOpdEncounter(true);
        getBillController().setOpdEncounterComments(opdVisit.getComments());
        getBillController().setPatientSearchTab(1);
        getBillController().setPatientTabId("tabSearchPt");
        getBillController().setReferredBy(doctor);
        return "/opd/opd_bill";
    }

    public String issuePharmacyBill() {
        if (billSession == null) {
            JsfUtil.addErrorMessage("Please select encounter");
            opdVisit = null;
            return "";
        }
        Map m = new HashMap();
        m.put("bs", getBillSession());
        String sql;
        sql = "select pe from PatientEncounter pe where pe.billSession=:bs";
        opdVisit = getPatientEncounterFacade().findFirstByJpql(sql, m);
        //   ////// // System.out.println("opdVisit = " + opdVisit);
        if (opdVisit == null) {
            //   ////// // System.out.println("going for a new opd visit = ");
            newOpdVisit();
        }
        if (opdVisit == null) {
            JsfUtil.addErrorMessage("Can not create an opd encounter");
            return "";
        }
        getPharmacySaleController().setPatient(opdVisit.getPatient());
        getPharmacySaleController().setPatientSearchTab(1);
        getPharmacySaleController().setOpdEncounterComments(opdVisit.getComments());
        getPharmacySaleController().setFromOpdEncounter(true);
        getPharmacySaleController().setPatientTabId("tabSearchPt");
        return "/clinical/clinical_pharmacy_sale";
    }

    public void opdVisitFromServiceSession() {
        //   ////// // System.out.println("opd visit from service session ");
        if (billSession == null) {
            JsfUtil.addErrorMessage("Please select encounter");
            opdVisit = null;
            return;
        }
        Map m = new HashMap();
        m.put("bs", getBillSession());
        String sql;
        sql = "select pe from PatientEncounter pe where pe.billSession=:bs";
        opdVisit = getPatientEncounterFacade().findFirstByJpql(sql, m);
        if (opdVisit == null) {
            newOpdVisit();
            String tt = "";
            SimpleDateFormat sm = new SimpleDateFormat("dd MMMM yyyy");
            tt = sm.format(new Date());
            tt += "\n\n";
            tt += billSession.getBill().getPatient().getPerson().getNameWithTitle();
            tt += "\n";
            tt += billSession.getBill().getPatient().getAge();
            tt += "\n";
            opdVisit.setComments(tt);
        }
    }

    public void newOpdVisit() {
        if (billSession == null) {
            JsfUtil.addErrorMessage("Please select encounter");
            opdVisit = null;
            return;
        }
        opdVisit = new PatientEncounter();
        opdVisit.setCreatedAt(Calendar.getInstance().getTime());
        opdVisit.setCreater(getSessionController().getLoggedUser());
        opdVisit.setPatient(billSession.getBill().getPatient());
        //   ////// // System.out.println("billSession = " + billSession);
        //   ////// // System.out.println("billSession.getBill() = " + billSession.getBill());
        //   ////// // System.out.println("billSession.getBill().getPatient() = " + billSession.getBill().getPatient());
        //   ////// // System.out.println("getBillSession().getBill().getPatient().getPerson().getName() = " + getBillSession().getBill().getPatient().getPerson().getName());
        opdVisit.setPatientEncounterType(PatientEncounterType.OpdVisit);
        opdVisit.setBillSession(billSession);
        opdVisit.setOpdDoctor(doctor);
        opdVisit.setFinalBill(billSession.getBill());
        getPatientEncounterFacade().create(opdVisit);

        billSession.setPatientEncounter(opdVisit);
        getBillSessionFacade().edit(billSession);

    }

//    public void addAndClear() {
//        addToQueue();
//        getPatientController().setCurrent(null);
//    }

//    public void addToQueue() {
//        if (getPatientController().getCurrent() == null || getPatientController().getCurrent().getId() == null) {
//            JsfUtil.addErrorMessage("Please select a patient");
//            return;
//        }
//        if (doctor == null) {
//            JsfUtil.addErrorMessage("Please select a doctor");
//            return;
//        }
//        if (getSelectedServiceSession() == null) {
//            JsfUtil.addErrorMessage("Please select session");
//            return;
//        }
//
//        addToSession(addToBilledItem(addToBill()));
//        listBillSessions();
//        JsfUtil.addSuccessMessage("Added to the queue");
//    }

//    private BillItem addToBilledItem(Bill b) {
//        BillItem bi = new BillItem();
//        bi.setCreatedAt(new Date());
//        bi.setCreater(getSessionController().getLoggedUser());
//        bi.setBill(b);
//        bi.setNetValue(b.getTotal());
//        bi.setSessionDate(getSelectedServiceSession().getSessionDate());
//        getBillItemFacade().create(bi);
//        return bi;
//    }

//    private void addToSession(BillItem bi) {
//        ////// // System.out.println("adding to session");
//        Bill b = bi.getBill();
//        BillSession bs = new BillSession();
//
//        bs.setBill(b);
//        bs.setBillItem(bi);
//        bs.setCreatedAt(Calendar.getInstance().getTime());
//        bs.setCreater(getSessionController().getLoggedUser());
//        bs.setServiceSession(getSelectedServiceSession());
//        ////// // System.out.println("getSelectedServiceSession() = " + getSelectedServiceSession());
//        bs.setSessionDate(getSelectedServiceSession().getSessionDate());
//        ////// // System.out.println("getSelectedServiceSession().getSessionDate() = " + getSelectedServiceSession().getSessionDate());
//        int count = getServiceSessionBean().getSessionNumber(getSelectedServiceSession(), Calendar.getInstance().getTime());
//        bs.setSerialNo(count);
//        bs.setStaff(getSelectedServiceSession().getStaff());
//
//        getBillSessionFacade().create(bs);
//
//    }

    private Bill addToBill() {
        Bill bi = new BilledBill();
        bi.setBookingId(getBillNumberBean().gpBookingIdGenerator());
        bi.setStaff(getDoctor());
        bi.setBillType(BillType.ClinicalOpdBooking);
        if (foreigner) {
            bi.setTotal(getSelectedServiceSession().getTotalFfee());
        } else {
            bi.setTotal(getSelectedServiceSession().getTotalFee());
        }
        bi.setPatient(getPatientController().getCurrent());
        //   ////// // System.out.println("pt = " + getPatientController().getCurrent().getPerson().getName());

        bi.setBillDate(new Date());
        bi.setBillTime(new Date());
        bi.setCreatedAt(new Date());
        bi.setCreater(getSessionController().getLoggedUser());
        getBillFacade().create(bi);
        return bi;
    }

    public void updatePatient() {
        getBillSessionFacade().edit(getSelectedBillSession());

        getPatientFacade().edit(getSelectedBillSession().getBill().getPatient());
        JsfUtil.addSuccessMessage("Patient Updated");
    }

    public void makeNull() {
        speciality = null;
        doctor = null;
        selectedServiceSession = null;
        /////////////////////
        serviceSessions = null;
        billSessions = null;
    }

    public List<Staff> completeStaff(String query) {
        List<Staff> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<Staff>();
        } else {
            if (getSpeciality() != null) {
                sql = "select p from Staff p where p.retired=false and ((p.person.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) and p.speciality.id = " + getSpeciality().getId() + " order by p.person.name";
            } else {
                sql = "select p from Staff p where p.retired=false and ((p.person.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) order by p.person.name";
            }
            //////// // System.out.println(sql);
            suggestions = getStaffFacade().findByJpql(sql);
        }
        return suggestions;
    }

    public List<Staff> getConsultants() {
        List<Staff> suggestions;
        String sql;

        if (getSpeciality() != null) {
            sql = "select p from Staff p where p.retired=false and p.speciality.id = " + getSpeciality().getId() + " order by p.person.name";
        } else {
            sql = "select p from Doctor p where p.retired=false order by p.person.name";
        }
        //////// // System.out.println(sql);
        suggestions = getStaffFacade().findByJpql(sql);

        return suggestions;
    }

    /**
     * Creates a new instance of BookingController
     */
    public PracticeBookingController() {
        serviceSessions = new ArrayList<>();
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public List<ServiceSession> getServiceSessions() {
        //////// // System.out.println("gettint service sessions");

        if (serviceSessions == null) {
            serviceSessions = new ArrayList<>();
            String sql;

            if (doctor != null) {
                try {
                    //////// // System.out.println("staff is " + staff);
                    sql = "Select s From ServiceSession s where s.retired=false and s.staff.id=" + getDoctor().getId() + " order by s.sessionWeekday";
                    List<ServiceSession> tmp = getServiceSessionFacade().findByJpql(sql);
                    //////// // System.out.println("tmp is " + tmp.size());
                    if (!tmp.isEmpty()) {
                        serviceSessions = getChannelBean().setSessionAt(tmp);
                    }
                } catch (Exception e) {
                    //////// // System.out.println("error 11 + " + e.getMessage());
                }
            }
        }

        return serviceSessions;
    }

    public void setServiceSessions(List<ServiceSession> serviceSessions) {

        this.serviceSessions = serviceSessions;
    }

    public ServiceSessionFacade getServiceSessionFacade() {
        return serviceSessionFacade;
    }

    public void setServiceSessionFacade(ServiceSessionFacade serviceSessionFacade) {
        this.serviceSessionFacade = serviceSessionFacade;
    }

    public void listBillSessions() {
        String sql = "Select bs From BillSession bs where "
                + " bs.retired=false and "
                + " bs.serviceSession=:ss and "
                + " bs.sessionDate= :ssDate"
                + " order by bs.serialNo";
        HashMap hh = new HashMap();
        hh.put("ssDate", sessionDate);
        hh.put("ss", getSelectedServiceSession());
        billSessions = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);
    }

    public void listCompleteAndToCompleteBillSessions() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        listCompletedBillSessions();
        listToCompleteBillSessions();
        
        
    }

    List<PatientEncounter> encounters;

    public void listPatientEncounters() {
        Date startTime = new Date();
        
        String sql = "Select pe From PatientEncounter pe "
                + " where pe.retired=false "
                + " and pe.patientEncounterType=:t "
                + " and pe.billSession.sessionDate=:ssDate "
                + " order by pe.id";
        HashMap hh = new HashMap();
        BillType billType;
        hh.put("t", PatientEncounterType.OpdVisit);
        hh.put("ssDate", sessionDate);
        PatientEncounter pe = new PatientEncounter();
//        pe.getBillSession().getSessionDate();
        encounters = patientEncounterFacade.findByJpql(sql, hh, TemporalType.DATE);
        
        
    }

    public List<PatientEncounter> getEncounters() {
        return encounters;
    }

    public void setEncounters(List<PatientEncounter> encounters) {
        this.encounters = encounters;
    }

//    public void opdVisitFromServiceSessionOld() {
//        //   ////// // System.out.println("opd visit from service session ");
//
//        Map m = new HashMap();
//        m.put("bs", getBillSession());
//        String sql;
//        sql = "select pe from PatientEncounter pe where pe.billSession=:bs";
//        opdVisit = getPatientEncounterFacade().findFirstByJpql(sql, m);
//
//    }

    public void listCompletedBillSessions() {
        String sql = "Select bs From BillSession bs where bs.retired=false and bs.serviceSession=:ss "
                + " and bs.sessionDate=:ssDate and bs.patientEncounter is not null "
                + " order by bs.serialNo";
        HashMap hh = new HashMap();
        hh.put("ssDate", sessionDate);
        hh.put("ss", getSelectedServiceSession());
        completedSessions = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);
    }

    public void listToCompleteBillSessions() {
        String sql = "Select bs From BillSession bs "
                + "where bs.retired=false and bs.serviceSession=:ss and bs.sessionDate=:ssDate and bs.patientEncounter is null "
                + "order by bs.serialNo";
        HashMap hh = new HashMap();
        hh.put("ssDate", sessionDate);
        hh.put("ss", getSelectedServiceSession());
        toCompleteSessions = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);
    }

    public List<BillSession> getBillSessions() {
        return billSessions;
    }

    public void setBillSessions(List<BillSession> billSessions) {
        this.billSessions = billSessions;
    }

    @EJB
    SessionNumberGeneratorFacade sngFacade;

    public SessionNumberGenerator getSessionNumberGenerator(Staff staff) {
        String j;
        Map m = new HashMap();
        m.put("staff", staff);
        j = "Select s from SessionNumberGenerator s where s.staff=:staff";
        SessionNumberGenerator s = sngFacade.findFirstByJpql(j, m);
        if (s == null) {
            s = new SessionNumberGenerator();
            s.setStaff(staff);
            sngFacade.create(s);
        }
        return s;
    }

    public ServiceSession getSelectedServiceSession() {
        String sql;
        Map m = new HashMap();
        m.put("s", doctor);
        m.put("d", sessionDate);
        sql = "select ss from ServiceSession ss where ss.staff=:s and ss.sessionDate=:d";
        selectedServiceSession = getServiceSessionFacade().findFirstByJpql(sql, m, TemporalType.DATE);
        ////// // System.out.println("selectedServiceSession = " + selectedServiceSession);
        if (selectedServiceSession == null) {
            selectedServiceSession = new ServiceSession();
            selectedServiceSession.setSessionNumberGenerator(getSessionNumberGenerator(doctor));
            selectedServiceSession.setSessionDate(sessionDate);
            selectedServiceSession.setStaff(doctor);
            selectedServiceSession.setSessionTime(Calendar.getInstance().getTime());
            getServiceSessionFacade().create(selectedServiceSession);
            ////// // System.out.println("new selectedServiceSession = " + selectedServiceSession);
        }
        return selectedServiceSession;
    }

    public void setSelectedServiceSession(ServiceSession selectedServiceSession) {
        this.selectedServiceSession = selectedServiceSession;
    }

    public void makeBillSessionNull() {
        billSessions = null;
    }

    public BillSessionFacade getBillSessionFacade() {
        return billSessionFacade;
    }

    public void setBillSessionFacade(BillSessionFacade billSessionFacade) {
        this.billSessionFacade = billSessionFacade;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
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

    public BillSession getSelectedBillSession() {
        if (selectedBillSession == null) {
            selectedBillSession = new BillSession();
            Bill b = new BilledBill();
            Patient p = new Patient();
            p.setPerson(new Person());
            b.setPatient(p);
            selectedBillSession.setBill(b);
        }
        return selectedBillSession;
    }

    public void setSelectedBillSession(BillSession selectedBillSession) {
        this.selectedBillSession = selectedBillSession;
        getChannelCancelController().makeNull();
        getChannelCancelController().setBillSession(selectedBillSession);
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public ChannelBillController getChannelCancelController() {
        return channelCancelController;
    }

    public void setChannelCancelController(ChannelBillController channelCancelController) {
        this.channelCancelController = channelCancelController;
    }

    public ChannelReportController getChannelReportController() {
        return channelReportController;
    }

    public void setChannelReportController(ChannelReportController channelReportController) {
        this.channelReportController = channelReportController;
    }

    public Boolean preSet() {
        if (getSelectedServiceSession() == null) {
            JsfUtil.addErrorMessage("Please select Service Session");
            return false;
        }

        getChannelReportController().setServiceSession(selectedServiceSession);
        return true;
    }

    public ChannelSearchController getChannelSearchController() {
        return channelSearchController;
    }

    public void setChannelSearchController(ChannelSearchController channelSearchController) {
        this.channelSearchController = channelSearchController;
    }

    public ChannelBean getChannelBean() {
        return channelBean;
    }

    public void setChannelBean(ChannelBean channelBean) {
        this.channelBean = channelBean;
    }

    public PatientController getPatientController() {
        return patientController;
    }

    public void setPatientController(PatientController patientController) {
        this.patientController = patientController;
    }

    /**
     *
     * @return
     */
    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public boolean isForeigner() {
        return foreigner;
    }

    public void setForeigner(boolean foreigner) {
        this.foreigner = foreigner;
    }

    public ServiceSessionBean getServiceSessionBean() {
        return serviceSessionBean;
    }

    public void setServiceSessionBean(ServiceSessionBean serviceSessionBean) {
        this.serviceSessionBean = serviceSessionBean;
    }

    public DoctorFacade getDoctorFacade() {
        return doctorFacade;
    }

    public void setDoctorFacade(DoctorFacade doctorFacade) {
        this.doctorFacade = doctorFacade;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public List<BillSession> getCompletedSessions() {
        return completedSessions;
    }

    public void setCompletedSessions(List<BillSession> completedSessions) {
        this.completedSessions = completedSessions;
    }

    public List<BillSession> getToCompleteSessions() {
        return toCompleteSessions;
    }

    public void setToCompleteSessions(List<BillSession> toCompleteSessions) {
        this.toCompleteSessions = toCompleteSessions;
    }

    public BillController getBillController() {
        return billController;
    }

    public void setBillController(BillController billController) {
        this.billController = billController;
    }

    public PharmacySaleController getPharmacySaleController() {
        return pharmacySaleController;
    }

    public void setPharmacySaleController(PharmacySaleController pharmacySaleController) {
        this.pharmacySaleController = pharmacySaleController;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

}
