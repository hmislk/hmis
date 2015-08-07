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
import com.divudi.entity.Institution;
import com.divudi.entity.PersonInstitution;
import com.divudi.entity.Staff;
import com.divudi.facade.PersonInstitutionFacade;
import java.io.Serializable;
import java.util.List;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.model.SelectItem;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

/**
 *
 * @author buddhika
 */
@Named
@SessionScoped
public class PersonInstitutionController implements Serializable{

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

    Institution institution;
    List<PersonInstitution> institutionPersons;
    Staff staff;

    public Institution getInstitution() {
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

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }
    
    
    
    public void addStaffToInstitutionPersons(){
        //Search database for same persons institution >> If retired > active, it not error
        // New Person Institition
        // Institution Person List Update
        // staff null
        
    }
    
    public void removeStaffToInstitutionPersons(){
        
    }
    
        
    
    public void fillStaffInstitutionPersons(){
        // sdfsdf
        
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
