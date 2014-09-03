/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.entity.Department;
import com.divudi.entity.Fee;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.entity.Service;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.FeeFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.ServiceFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.StaffFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class ChannellingFeeController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    SessionController sessionController;

    @EJB
    private ItemFacade itemFacade;
    @EJB
    private ItemFeeFacade itemFeeFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private StaffFacade staffFacade;
    @EJB
    ServiceSessionFacade serviceSessionFacade;

    private Speciality speciality;
    private Staff doctor;
    private Item session;
    private ItemFee fee;
    private ItemFee removingFee;

    List<ServiceSession> sessions;
    private List<Staff> doctors;
    private List<ItemFee> fees;

    public List<Staff> completeDoctors(String query) {
        String sql;
        Map m = new HashMap();
        if (query == null) {
            doctors = new ArrayList<>();
        } else {
            m.put("qry", "%" + query.toUpperCase() + "%");
            if (speciality == null) {
                sql = "select p from Staff p "
                        + " where p.retired=false "
                        + " and (upper(p.person.name) like :qry "
                        + " or upper(p.code) like :qry ) "
                        + " order by p.person.name";
            } else {
                sql = "select p from Staff p "
                        + " where p.speciality=:spe "
                        + " and p.retired=false "
                        + " and (upper(p.person.name) like :qry "
                        + " or  upper(p.code) like :qry "
                        + " order by p.person.name";
                m.put("spe", speciality);
            }
            doctors = getStaffFacade().findBySQL(sql, m);
        }
        return doctors;
    }

    public void fillSessions() {
        String sql;
        Map m = new HashMap();
        sql = "Select s From ServiceSession s "
                + " where s.retired=false "
                + " and s.staff=:doc "
                + " order by s.sessionWeekday, sessionAt";
        m.put("doc", doctor);
        sessions = getServiceSessionFacade().findBySQL(sql, m);
    }

    public void fillFees() {
        String sql;
        Map m = new HashMap();
        sql = "Select f from ItemFee f "
                + " where f.retired=false and "
                + " f.item=:ses "
                + " order by f.id";
        m.put("ses", session);
        fees = getItemFeeFacade().findBySQL(sql, m);
    }

    public void saveCharge() {
        if (session == null) {
            UtilityController.addErrorMessage("Please select a session");
            return;
        }
        if (fee == null) {
            UtilityController.addErrorMessage("Please select a fee");
            return;
        }
        fee.setItem(session);
        if (fee.getId() == null || fee.getId() == 0) {
            fee.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            fee.setCreater(getSessionController().getLoggedUser());
            getItemFeeFacade().create(fee);
            UtilityController.addSuccessMessage("Fee Added");
        } else {
            getItemFeeFacade().edit(fee);
            UtilityController.addSuccessMessage("Fee Saved");
        }
        session.setTotal(calTot());
        getItemFacade().edit(session);
    }

    private double calTot() {
        double tot = 0.0;
        for (ItemFee i : getFees()) {
            tot += i.getFee();
        }
        return tot;
    }

    public ChannellingFeeController() {
    }

    public void delete() {
        if (removingFee != null) {
            removingFee.setRetired(true);
            removingFee.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            removingFee.setRetirer(getSessionController().getLoggedUser());
            getItemFeeFacade().edit(removingFee);
            UtilityController.addSuccessMessage("Deleted Successfull");
        } else {
            UtilityController.addSuccessMessage("Nothing To Delete");
        }
        setRemovingFee(null);
        setFee(null);
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
        setDoctors(null);
    }

    public Staff getDoctor() {
        return doctor;
    }

    public void setDoctor(Staff doctor) {
        this.doctor = doctor;
        setSessions(null);
    }

    public List<Staff> getDoctors() {
        return doctors;
    }

    public void setDoctors(List<Staff> doctors) {
        this.doctors = doctors;
        setDoctor(null);
    }

    public List<ServiceSession> getSessions() {
        return sessions;
    }

    public void setSessions(List<ServiceSession> sessions) {
        this.sessions = sessions;
        setSession(null);
    }

    public Item getSession() {
        if (sessions == null) {
            fillSessions();
        }
        return session;
    }

    public void setSession(Item session) {
        this.session = session;
        setFees(null);
    }

    public ItemFee getFee() {
        return fee;
    }

    public void setFee(ItemFee fee) {
        this.fee = fee;
    }

    public ItemFee getRemovingFee() {
        return removingFee;
    }

    public void setRemovingFee(ItemFee removingFee) {
        this.removingFee = removingFee;
    }

    public List<ItemFee> getFees() {
        if (fees == null) {
            fillFees();
        }
        return fees;
    }

    public void setFees(List<ItemFee> fees) {
        this.fees = fees;
        setFee(null);
        setRemovingFee(null);
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public ItemFeeFacade getItemFeeFacade() {
        return itemFeeFacade;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public ServiceSessionFacade getServiceSessionFacade() {
        return serviceSessionFacade;
    }

}
