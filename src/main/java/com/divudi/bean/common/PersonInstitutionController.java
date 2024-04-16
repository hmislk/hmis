/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.PersonInstitutionType;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.entity.Consultant;
import com.divudi.entity.Institution;
import com.divudi.entity.PersonInstitution;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.facade.PersonInstitutionFacade;
import com.divudi.facade.StaffFacade;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author buddhika
 */
@Named
@SessionScoped
public class PersonInstitutionController implements Serializable {

    public PersonInstitutionController() {
    }

    private PersonInstitution personInstitution = null;
    private List<PersonInstitution> personInstitutionItems = null;
    private PersonInstitutionFacade jpaController = null;

    @Inject
    SessionController sessionController;

    @EJB
    StaffFacade staffFacade;
    @EJB
    PersonInstitutionFacade personInstitutionFacade;

    ReportKeyWord reportKeyWord;

    Institution institution;
    Speciality speciality;
    List<PersonInstitution> institutionPersons;
    List<Staff> selectedList;
    List<Staff> withOutInstitutionPersonsStaffs;

    public Institution getInstitution() {
        if (institution == null) {
            institution = getSessionController().getInstitution();
        }
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<PersonInstitution> getInstitutionPersons() {
        return institutionPersons;
    }

    public void setInstitutionPersons(List<PersonInstitution> institutionPersons) {
        this.institutionPersons = institutionPersons;
    }

    public List<Staff> getSelectedList() {
        return selectedList;
    }

    public void setSelectedList(List<Staff> selectedList) {
        this.selectedList = selectedList;
    }

    public List<Staff> getWithOutInstitutionPersonsStaffs() {
        return withOutInstitutionPersonsStaffs;
    }

    public void setWithOutInstitutionPersonsStaffs(List<Staff> withOutInstitutionPersonsStaffs) {
        this.withOutInstitutionPersonsStaffs = withOutInstitutionPersonsStaffs;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    private PersonInstitutionFacade getPersonInstitutionFacade() {
        return personInstitutionFacade;
    }

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    public void addStaffToInstitutionPersons() {
        //Search database for same persons institution >> If retired > active, it not error
        // New Person Institition
        // Institution Person List Update
        // staff null
        if (institution == null) {
            JsfUtil.addErrorMessage("Please Select Institution");
            return;
        }

        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing To Add.");
            return;
        }

        for (Staff s : selectedList) {
            PersonInstitution pi = findDeactivatedPersonInstitution(institution, s, true);
            if (findDeactivatedPersonInstitution(institution, s, false) != null) {
                continue;
            }
            if (pi == null) {
                PersonInstitution p = new PersonInstitution();
                p.setStaff(s);
                p.setInstitution(institution);
                p.setCreater(getSessionController().getLoggedUser());
                p.setCreatedAt(new Date());
                p.setType(PersonInstitutionType.Channelling);
                getPersonInstitutionFacade().create(p);
            } else {
                pi.setRetired(false);
                getPersonInstitutionFacade().edit(pi);
            }
        }
        JsfUtil.addSuccessMessage("Sucessfull " + selectedList.size() + " Doctors added");
        selectedList = new ArrayList<>();
        withOutInstitutionPersonsStaffs = new ArrayList<>();
        createWithOutInstitutionPersonsStaffs();

    }

    public void removeStaffToInstitutionPersons(PersonInstitution pi) {
        if (pi == null) {
            return;
        }
        pi.setRetired(true);
        pi.setRetirer(getSessionController().getLoggedUser());
        pi.setRetiredAt(new Date());
        getPersonInstitutionFacade().edit(pi);
        fillStaffInstitutionPersons();
        JsfUtil.addSuccessMessage("Removed");
    }

    public void fillStaffInstitutionPersons() {
        // sdfsdf
        //restrictions
        if (reportKeyWord == null) {
            return;
        }
        institutionPersons = findPersonInstitutions(reportKeyWord.getInstitution(), reportKeyWord.getStaff(), reportKeyWord.getSpeciality());

    }
// important for today

    public void createWithOutInstitutionPersonsStaffs() {
        if (institution == null) {
            JsfUtil.addErrorMessage("Please Select Institution");
            return;
        }
        List<Staff> staffsWithInstitutionPersons = new ArrayList<>();
        List<Staff> staffsAll = new ArrayList<>();
        withOutInstitutionPersonsStaffs = new ArrayList<>();
        String sql;
        Map m = new HashMap();

        sql = " select pi.staff from PersonInstitution pi where pi.retired=false "
                + " and pi.staff.retired=false "
                + " and pi.institution=:ins "
                + " and pi.type=:typ ";
        if (speciality != null) {
            sql += " and pi.staff.speciality=:spe ";
            m.put("spe", speciality);
        }

        m.put("ins", institution);

        m.put("typ", PersonInstitutionType.Channelling);

        staffsWithInstitutionPersons = getStaffFacade().findByJpql(sql, m);

        m = new HashMap();

        sql = " select s from Staff s where s.retired=false "
                + " and type(s)=:class ";

        if (speciality != null) {
            sql += " and s.speciality=:spe ";
            m.put("spe", speciality);
        }

        sql += " order by s.person.name ";

        m.put("class", Consultant.class);

        staffsAll = getStaffFacade().findByJpql(sql, m);

        withOutInstitutionPersonsStaffs.addAll(staffsAll);
        withOutInstitutionPersonsStaffs.removeAll(staffsWithInstitutionPersons);

    }

    public PersonInstitution findDeactivatedPersonInstitution(Institution i, Staff s, boolean b) {

        String sql;
        Map m = new HashMap();

        sql = " select pi from PersonInstitution pi where pi.staff=:staff "
                + " and pi.institution=:ins "
                + " and pi.type=:typ ";

        if (b) {
            sql += " and pi.retired=true ";
        } else {
            sql += " and pi.retired=false ";
        }

        m.put("ins", i);
        m.put("typ", PersonInstitutionType.Channelling);
        m.put("staff", s);

        return getPersonInstitutionFacade().findFirstByJpql(sql, m);
    }

    public List<PersonInstitution> findPersonInstitutions(Institution i, Staff s, Speciality sp) {

        String sql;
        Map m = new HashMap();

        sql = " select pi from PersonInstitution pi where pi.retired=false "
                + " and pi.type=:typ ";

        if (i != null) {
            sql += " and pi.institution=:ins ";
            m.put("ins", i);
        }

        if (s != null) {
            sql += " and pi.staff=:staff ";
            m.put("staff", s);
        }

        if (sp != null) {
            sql += " and pi.staff.speciality=:spe ";
            m.put("spe", sp);
        }

        sql += " order by pi.staff.person.name ";

        m.put("typ", PersonInstitutionType.Channelling);

        return getPersonInstitutionFacade().findByJpql(sql, m);
    }

   

    public PersonInstitutionFacade getJpaController() {
        if (jpaController == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            jpaController = (PersonInstitutionFacade) facesContext.getApplication().getELResolver().getValue(facesContext.getELContext(), null, "personInstitutionJpa");
        }
        return jpaController;
    }

    public SelectItem[] getPersonInstitutionItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(getJpaController().findAll(), false);
    }

    public SelectItem[] getPersonInstitutionItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(getJpaController().findAll(), true);
    }

    public PersonInstitution getPersonInstitution() {
        if (personInstitution == null) {
            personInstitution = new PersonInstitution();
        }
        return personInstitution;
    }

    public String listSetup() {
        reset(true);
        return "/personinstitution/personinstitution/personInstitution_list";
    }

    public String createSetup() {
        reset(false);
        personInstitution = new PersonInstitution();
        return "personInstitution_create";
    }

    private void reset(boolean resetFirstItem) {
        personInstitution = null;
        personInstitutionItems = null;
    }

   
    private PersonInstitutionFacade getFacade() {
        return personInstitutionFacade;
    }
    // new method for create doctor session 

    @FacesConverter(forClass = PersonInstitution.class)
    public static class PersonInstitutionConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            PersonInstitutionController controller = (PersonInstitutionController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "personInstitutionController");
            return controller.getFacade().find(getKey(value));
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
            if (object instanceof PersonInstitution) {
                PersonInstitution o = (PersonInstitution) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + PersonInstitution.class.getName());
            }
        }
    }

}
