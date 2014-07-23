/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.FeeType;
import com.divudi.ejb.ChannelBean;
import com.divudi.ejb.FinalVariables;
import com.divudi.entity.Bill;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Patient;
import com.divudi.entity.Person;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.StaffFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class BookingPastController implements Serializable {

    private Speciality speciality;
    private Staff staff;
    private ServiceSession selectedServiceSession;
    private BillSession selectedBillSession;
    private Date date;
    private double doctorTotal;
    private double hospitalTotal;
    private double agentTotal;
    private double taxTotal;
    private double doctorTotalR;
    private double hospitalTotalR;
    private double agentTotalR;
    private double taxTotalR;
    ////////////////////
    private List<ServiceSession> serviceSessions;
    private List<BillSession> billSessions;
    private List<BillSession> filltredBillSession;
    ////////////////////
    @Inject
    private SessionController sessionController;
    @Inject
    private ChannelBillController channelCancelController;
    @Inject
    private ChannelReportController channelReportController;
    @Inject
    private ChannelSearchController channelSearchController;
    ///////////////////
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
    /////////////////////////
    @EJB
    FinalVariables finalVariables;
    @EJB
    private ChannelBean channelBean;

    private void calTotal() {
        doctorTotal = 0.0;
        hospitalTotal = 0.0;
        agentTotal = 0.0;
        taxTotal = 0.0;
        doctorTotalR = 0.0;
        hospitalTotalR = 0.0;
        agentTotalR = 0.0;
        taxTotalR = 0.0;

        List<BillSession> list;

        if (filltredBillSession == null) {
            list = billSessions;
        } else {
            list = filltredBillSession;
        }

        if (list == null) {
            return;
        }

        for (BillSession b : list) {
            doctorTotal += b.getDoctorFee().getBilledFee().getFeeValue();
            hospitalTotal += b.getHospitalFee().getBilledFee().getFeeValue();
            agentTotal += b.getAgentFee().getBilledFee().getFeeValue();
            taxTotal += b.getTax().getBilledFee().getFeeValue();
            doctorTotalR += b.getDoctorFee().getPrevFee().getFeeValue();
            hospitalTotalR += b.getHospitalFee().getPrevFee().getFeeValue();
            agentTotalR += b.getAgentFee().getPrevFee().getFeeValue();
            taxTotalR += b.getTax().getPrevFee().getFeeValue();

        }

    }

    public String nurse() {
        if (preSet()) {
            return "channel_nurse_view";
        } else {
            return "";
        }
    }

    public String doctor() {
        if (preSet()) {
            return "channel_doctor_view";
        } else {
            return "";
        }
    }

    public String session() {
        if (preSet()) {
            return "channel_session_view";
        } else {
            return "";
        }
    }

    public String phone() {
        if (preSet()) {
            return "channel_phone_view";
        } else {
            return "";
        }
    }

    public String user() {
        if (preSet()) {
            return "channel_user_view";
        } else {
            return "";
        }
    }

  

    public void updatePatient() {
        getBillSessionFacade().edit(getSelectedBillSession());

        getPatientFacade().edit(getSelectedBillSession().getBill().getPatient());
        UtilityController.addSuccessMessage("Patient Updated");
    }

    public void makeNull() {
        speciality = null;
        staff = null;
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
                sql = "select p from Staff p where p.retired=false and (upper(p.person.name) like '%" + query.toUpperCase() + "%'or  upper(p.code) like '%" + query.toUpperCase() + "%' ) and p.speciality.id = " + getSpeciality().getId() + " order by p.person.name";
            } else {
                sql = "select p from Staff p where p.retired=false and (upper(p.person.name) like '%" + query.toUpperCase() + "%'or  upper(p.code) like '%" + query.toUpperCase() + "%' ) order by p.person.name";
            }
            //System.out.println(sql);
            suggestions = getStaffFacade().findBySQL(sql);
        }
        return suggestions;
    }

    public List<Staff> getConsultants() {
        List<Staff> suggestions;
        String sql;

        if (getSpeciality() != null) {
            sql = "select p from Staff p where p.retired=false and p.speciality.id = " + getSpeciality().getId() + " order by p.person.name";
        } else {
            sql = "select p from Staff p where p.retired=false order by p.person.name";
        }
        //System.out.println(sql);
        suggestions = getStaffFacade().findBySQL(sql);

        return suggestions;
    }

    /**
     * Creates a new instance of bookingController
     */
    public BookingPastController() {
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        makeNull();
        this.speciality = speciality;
    }

    public Staff getStaff() {
        makeBillSessionNull();
        return staff;
    }

    public void setStaff(Staff staff) {

        this.staff = staff;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public List<ServiceSession> getServiceSessions() {
        serviceSessions = new ArrayList<ServiceSession>();
        String sql = "";

        if (staff != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(getDate());
            int wd = c.get(Calendar.DAY_OF_WEEK);
            sql = "Select s From ServiceSession s where s.retired=false and s.staff.id=" + getStaff().getId() + " and s.sessionWeekday=" + wd;
            serviceSessions = getServiceSessionFacade().findBySQL(sql);
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

    public FinalVariables getFinalVariables() {
        return finalVariables;
    }

    public void setFinalVariables(FinalVariables finalVariables) {
        this.finalVariables = finalVariables;
    }

    public List<BillSession> getBillSessions() {
        if (billSessions == null) {
            if (getSelectedServiceSession() != null) {
                String sql = "Select bs From BillSession bs where bs.retired=false and bs.serviceSession.id="
                        + getSelectedServiceSession().getId() + " and bs.sessionDate= :ssDate and bs.serviceSession.staff.id=" + getStaff().getId();
                HashMap hh = new HashMap();
                hh.put("ssDate", getDate());
                billSessions = getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE);

                setFees();
            }
        }

        return billSessions;
    }

    private void setFees() {
        for (BillSession bs : billSessions) {
            bs.setDoctorFee(getChannelBean().getChannelFee(bs, FeeType.Staff));
            bs.setTax(getChannelBean().getChannelFee(bs, FeeType.Tax));
            bs.setHospitalFee(getChannelBean().getChannelFee(bs, FeeType.OwnInstitution));
            bs.setAgentFee(getChannelBean().getChannelFee(bs, FeeType.OtherInstitution));
        }
    }

    public void setBillSessions(List<BillSession> billSessions) {
        this.billSessions = billSessions;
    }

    public ServiceSession getSelectedServiceSession() {
        return selectedServiceSession;
    }

    public void setSelectedServiceSession(ServiceSession selectedServiceSession) {
        makeBillSessionNull();
        calTotal();
        this.selectedServiceSession = selectedServiceSession;
    }

    public void makeBillSessionNull() {
        billSessions = null;
        filltredBillSession = null;
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
            UtilityController.addErrorMessage("Please select Service Session");
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

    public Date getDate() {
        if (date == null) {
            date = new Date();
        }
        return date;
    }

    public void setDate(Date date) {
        makeBillSessionNull();
        this.date = date;

    }

    public List<BillSession> getFilltredBillSession() {
        calTotal();
        return filltredBillSession;
    }

    public void setFilltredBillSession(List<BillSession> filltredBillSession) {
        this.filltredBillSession = filltredBillSession;
    }

    public ChannelBean getChannelBean() {
        return channelBean;
    }

    public void setChannelBean(ChannelBean channelBean) {
        this.channelBean = channelBean;
    }

    public double getDoctorTotal() {
        return doctorTotal;
    }

    public void setDoctorTotal(double doctorTotal) {
        this.doctorTotal = doctorTotal;
    }

    public double getHospitalTotal() {
        return hospitalTotal;
    }

    public void setHospitalTotal(double hospitalTotal) {
        this.hospitalTotal = hospitalTotal;
    }

    public double getAgentTotal() {
        return agentTotal;
    }

    public void setAgentTotal(double agentTotal) {
        this.agentTotal = agentTotal;
    }

    public double getTaxTotal() {
        return taxTotal;
    }

    public void setTaxTotal(double taxTotal) {
        this.taxTotal = taxTotal;
    }

    public double getDoctorTotalR() {
        return doctorTotalR;
    }

    public void setDoctorTotalR(double doctorTotalR) {
        this.doctorTotalR = doctorTotalR;
    }

    public double getHospitalTotalR() {
        return hospitalTotalR;
    }

    public void setHospitalTotalR(double hospitalTotalR) {
        this.hospitalTotalR = hospitalTotalR;
    }

    public double getAgentTotalR() {
        return agentTotalR;
    }

    public void setAgentTotalR(double agentTotalR) {
        this.agentTotalR = agentTotalR;
    }

    public double getTaxTotalR() {
        return taxTotalR;
    }

    public void setTaxTotalR(double taxTotalR) {
        this.taxTotalR = taxTotalR;
    }
}
