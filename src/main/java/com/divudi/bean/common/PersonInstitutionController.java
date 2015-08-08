/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.faces.FacesException;
import javax.annotation.Resource;
import javax.transaction.UserTransaction;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.common.util.PagingInfo;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

/**
 *
 * @author buddhika
 */
@Named
@SessionScoped
public class PersonInstitutionController implements Serializable {

    public PersonInstitutionController() {
        pagingInfo = new PagingInfo();
        converter = new PersonInstitutionConverter();
    }
    private PersonInstitution personInstitution = null;
    private List<PersonInstitution> personInstitutionItems = null;
    private PersonInstitutionFacade jpaController = null;
    private PersonInstitutionConverter converter = null;
    private PagingInfo pagingInfo = null;
    @Resource
    private UserTransaction utx = null;
    @PersistenceUnit(unitName = "hmisPU")
    private EntityManagerFactory emf = null;

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

    public PersonInstitutionFacade getPersonInstitutionFacade() {
        return personInstitutionFacade;
    }

    public void setPersonInstitutionFacade(PersonInstitutionFacade personInstitutionFacade) {
        this.personInstitutionFacade = personInstitutionFacade;
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

        System.out.println("selectedList = " + selectedList.size());
        for (Staff s : selectedList) {
            PersonInstitution pi = findDeactivatedPersonInstitution(institution, s, true);
            if (findDeactivatedPersonInstitution(institution, s, false) != null) {
                System.err.println("Alredy Added");
                continue;
            }
            System.out.println("pi = " + pi);
            if (pi == null) {
                PersonInstitution p = new PersonInstitution();
                p.setStaff(s);
                p.setInstitution(institution);
                p.setCreater(getSessionController().getLoggedUser());
                p.setCreatedAt(new Date());
                p.setType(PersonInstitutionType.Channelling);
                getPersonInstitutionFacade().create(p);
            } else {
                System.out.println("pi.getInstitution().getName() = " + pi.getInstitution().getName());
                System.out.println("pi.getStaff().getPerson().getNameWithInitials() = " + pi.getStaff().getPerson().getNameWithInitials());
                System.out.println("pi.isRetired() = " + pi.isRetired());
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
            System.err.println("ReportKeyword Is null");
            return;
        }
        institutionPersons = findPersonInstitutions(reportKeyWord.getInstitution(), reportKeyWord.getStaff(),reportKeyWord.getSpeciality());
        System.out.println("institutionPersons = " + institutionPersons.size());

    }

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

        staffsWithInstitutionPersons = getStaffFacade().findBySQL(sql, m);

        m = new HashMap();

        sql = " select s from Staff s where s.retired=false "
                + " and type(s)=:class ";

        if (speciality != null) {
            sql += " and s.speciality=:spe ";
            m.put("spe", speciality);
        }

        sql += " order by s.person.name ";

        m.put("class", Consultant.class);

        staffsAll = getStaffFacade().findBySQL(sql, m);

        withOutInstitutionPersonsStaffs.addAll(staffsAll);
        withOutInstitutionPersonsStaffs.removeAll(staffsWithInstitutionPersons);

        System.out.println("staffsAll = " + staffsAll.size());
        System.out.println("staffsWithInstitutionPersons = " + staffsWithInstitutionPersons.size());
        System.out.println("withOutInstitutionPersonsStaffs = " + withOutInstitutionPersonsStaffs.size());
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

        return getPersonInstitutionFacade().findFirstBySQL(sql, m);
    }

    public List<PersonInstitution> findPersonInstitutions(Institution i, Staff s,Speciality sp) {

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

        return getPersonInstitutionFacade().findBySQL(sql, m);
    }

    public PagingInfo getPagingInfo() {
        if (pagingInfo.getItemCount() == -1) {
            pagingInfo.setItemCount(getJpaController().count());
        }
        return pagingInfo;
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
            personInstitution = (PersonInstitution) JsfUtil.getObjectFromRequestParameter("jsfcrud.currentPersonInstitution", converter, null);
        }
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

    public String create() {
        try {
            utx.begin();
        } catch (Exception ex) {
        }
        try {
            Exception transactionException = null;
            getJpaController().create(personInstitution);
            try {
                utx.commit();
            } catch (javax.transaction.RollbackException ex) {
                transactionException = ex;
            } catch (Exception ex) {
            }
            if (transactionException == null) {
                JsfUtil.addSuccessMessage("PersonInstitution was successfully created.");
            } else {
                JsfUtil.ensureAddErrorMessage(transactionException, "A persistence error occurred.");
            }
        } catch (Exception e) {
            try {
                utx.rollback();
            } catch (Exception ex) {
            }
            JsfUtil.ensureAddErrorMessage(e, "A persistence error occurred.");
            return null;
        }
        return listSetup();
    }

    public String detailSetup() {
        return scalarSetup("personInstitution_detail");
    }

    public String editSetup() {
        return scalarSetup("personInstitution_edit");
    }

    private String scalarSetup(String destination) {
        reset(false);
        personInstitution = (PersonInstitution) JsfUtil.getObjectFromRequestParameter("jsfcrud.currentPersonInstitution", converter, null);
        if (personInstitution == null) {
            String requestPersonInstitutionString = JsfUtil.getRequestParameter("jsfcrud.currentPersonInstitution");
            JsfUtil.addErrorMessage("The personInstitution with id " + requestPersonInstitutionString + " no longer exists.");
            return relatedOrListOutcome();
        }
        return destination;
    }

    public String edit() {
        String personInstitutionString = converter.getAsString(FacesContext.getCurrentInstance(), null, personInstitution);
        String currentPersonInstitutionString = JsfUtil.getRequestParameter("jsfcrud.currentPersonInstitution");
        if (personInstitutionString == null || personInstitutionString.length() == 0 || !personInstitutionString.equals(currentPersonInstitutionString)) {
            String outcome = editSetup();
            if ("personInstitution_edit".equals(outcome)) {
                JsfUtil.addErrorMessage("Could not edit personInstitution. Try again.");
            }
            return outcome;
        }
        try {
            utx.begin();
        } catch (Exception ex) {
        }
        try {
            Exception transactionException = null;
            getJpaController().edit(personInstitution);
            try {
                utx.commit();
            } catch (javax.transaction.RollbackException ex) {
                transactionException = ex;
            } catch (Exception ex) {
            }
            if (transactionException == null) {
                JsfUtil.addSuccessMessage("PersonInstitution was successfully updated.");
            } else {
                JsfUtil.ensureAddErrorMessage(transactionException, "A persistence error occurred.");
            }
        } catch (Exception e) {
            try {
                utx.rollback();
            } catch (Exception ex) {
            }
            JsfUtil.ensureAddErrorMessage(e, "A persistence error occurred.");
            return null;
        }
        return detailSetup();
    }

    public String remove() {
        String idAsString = JsfUtil.getRequestParameter("jsfcrud.currentPersonInstitution");
        Long id = new Long(idAsString);
        try {
            utx.begin();
        } catch (Exception ex) {
        }
        try {
            Exception transactionException = null;
            getJpaController().remove(getJpaController().find(id));
            try {
                utx.commit();
            } catch (javax.transaction.RollbackException ex) {
                transactionException = ex;
            } catch (Exception ex) {
            }
            if (transactionException == null) {
                JsfUtil.addSuccessMessage("PersonInstitution was successfully deleted.");
            } else {
                JsfUtil.ensureAddErrorMessage(transactionException, "A persistence error occurred.");
            }
        } catch (Exception e) {
            try {
                utx.rollback();
            } catch (Exception ex) {
            }
            JsfUtil.ensureAddErrorMessage(e, "A persistence error occurred.");
            return null;
        }
        return relatedOrListOutcome();
    }

    private String relatedOrListOutcome() {
        String relatedControllerOutcome = relatedControllerOutcome();
        if (relatedControllerOutcome != null) {
            return relatedControllerOutcome;
        }
        return listSetup();
    }

    public List<PersonInstitution> getPersonInstitutionItems() {
        if (personInstitutionItems == null) {
            getPagingInfo();
            personInstitutionItems = getJpaController().findRange(new int[]{pagingInfo.getFirstItem(), pagingInfo.getFirstItem() + pagingInfo.getBatchSize()});
        }
        return personInstitutionItems;
    }

    public String next() {
        reset(false);
        getPagingInfo().nextPage();
        return "personInstitution_list";
    }

    public String prev() {
        reset(false);
        getPagingInfo().previousPage();
        return "personInstitution_list";
    }

    private String relatedControllerOutcome() {
        String relatedControllerString = JsfUtil.getRequestParameter("jsfcrud.relatedController");
        String relatedControllerTypeString = JsfUtil.getRequestParameter("jsfcrud.relatedControllerType");
        if (relatedControllerString != null && relatedControllerTypeString != null) {
            FacesContext context = FacesContext.getCurrentInstance();
            Object relatedController = context.getApplication().getELResolver().getValue(context.getELContext(), null, relatedControllerString);
            try {
                Class<?> relatedControllerType = Class.forName(relatedControllerTypeString);
                Method detailSetupMethod = relatedControllerType.getMethod("detailSetup");
                return (String) detailSetupMethod.invoke(relatedController);
            } catch (ClassNotFoundException e) {
                throw new FacesException(e);
            } catch (NoSuchMethodException e) {
                throw new FacesException(e);
            } catch (IllegalAccessException e) {
                throw new FacesException(e);
            } catch (InvocationTargetException e) {
                throw new FacesException(e);
            }
        }
        return null;
    }

    private void reset(boolean resetFirstItem) {
        personInstitution = null;
        personInstitutionItems = null;
        pagingInfo.setItemCount(-1);
        if (resetFirstItem) {
            pagingInfo.setFirstItem(0);
        }
    }

    public void validateCreate(FacesContext facesContext, UIComponent component, Object value) {
        PersonInstitution newPersonInstitution = new PersonInstitution();
        String newPersonInstitutionString = converter.getAsString(FacesContext.getCurrentInstance(), null, newPersonInstitution);
        String personInstitutionString = converter.getAsString(FacesContext.getCurrentInstance(), null, personInstitution);
        if (!newPersonInstitutionString.equals(personInstitutionString)) {
            createSetup();
        }
    }

    public Converter getConverter() {
        return converter;
    }

}
