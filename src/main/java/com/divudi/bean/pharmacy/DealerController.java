/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.InstitutionFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public class DealerController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;

    @EJB
    private InstitutionFacade ejbFacade;
    private Institution current;
    private List<Institution> items = null;
    List<Institution> dealor = null;

    List<Institution> institutionList;

    public List<Institution> completeDealor(String query) {

        String sql;
        Map m = new HashMap();

        sql = "select c from Institution c where c.retired=false and "
                + " c.institutionType =:t and (c.name) like :q order by c.name";
        //////// // System.out.println(sql);
        m.put("t", InstitutionType.Dealer);
        m.put("q", "%" + query.toUpperCase() + "%");
        institutionList = getEjbFacade().findByJpql(sql, m);
        //////// // System.out.println("suggestions = " + suggestions);

        return institutionList;
    }

    public List<Institution> completeActiveDealor(String query) {

        String sql;
        Map m = new HashMap();

        sql = "select c from Institution c where c.retired=false and c.inactive=false and "
                + " c.institutionType =:t and (c.name) like :q order by c.name ";
        m.put("t", InstitutionType.Dealer);
        m.put("q", "%" + query.toUpperCase() + "%");
        institutionList = getEjbFacade().findByJpql(sql, m);

        return institutionList;
    }
    
    public List<Institution> getAllDealors(){
        String sql;
        Map m = new HashMap();

        sql = "select c from Institution c where "
                + " c.institutionType =:t and c.name is not null and c.name <> '' order by c.name";

        m.put("t", InstitutionType.Dealer);
        return getEjbFacade().findByJpql(sql, m);
    }

    public List<Institution> findAllDealors() {

        String sql;
        Map m = new HashMap();

        sql = "select c from Institution c where c.retired=false and "
                + " c.institutionType =:t and c.name is not null and c.name <> '' order by c.name";
        //////// // System.out.println(sql);
        m.put("t", InstitutionType.Dealer);
        institutionList = getEjbFacade().findByJpql(sql, m);
        //////// // System.out.println("suggestions = " + suggestions);

        return institutionList;
    }

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    public void generateCodeForDealor(){
        if(current == null){
            JsfUtil.addErrorMessage("Please select or add a supplier first.");
            return;
        }

        String prefix = configOptionApplicationController.getLongTextValueByKey("Prefix for supplier code generation", "SUP");
        List<Institution> allDealors = findAllDealors();

        // Build HashSet of existing codes for O(1) lookup
        Set<String> existingCodes = new HashSet<>();
        for(Institution dealer : allDealors){
            if(dealer.getInstitutionCode() != null){
                existingCodes.add(dealer.getInstitutionCode().toUpperCase());
            }
        }

        long number = 1;
        String generatedCode;

        // Find the next available code number using O(1) HashSet lookup
        while(true){
            String formattedNumber = String.format("%04d", number);
            generatedCode = prefix + formattedNumber;

            // O(1) check instead of O(n) loop
            if(!existingCodes.contains(generatedCode.toUpperCase())){
                current.setInstitutionCode(generatedCode);
                JsfUtil.addSuccessMessage("Code generated successfully: " + generatedCode);
                return;
            }
            number++;
        }
    }

    public void prepareAdd() {
        current = new Institution();
        current.setInstitutionType(InstitutionType.Dealer);
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {
        if(current.getName() == null || current.getName().isEmpty()){
            JsfUtil.addErrorMessage("Please add name of the supplier.");
            return;
        }

        if(current.getInstitutionCode() == null || current.getInstitutionCode().isEmpty()){
            JsfUtil.addErrorMessage("Please add code of the supplier.");
            return;
        }

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
             getItems();
    }

    public InstitutionFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(InstitutionFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public DealerController() {
    }

    public Institution getCurrent() {
        if (current == null) {
            current = new Institution();
        }
        return current;
    }

    public void setCurrent(Institution current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        //      getItems();
        current = null;
        getCurrent();
    }

    public void deletedDealorList() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        Map m = new HashMap();

        String sql = "SELECT i FROM Institution i where i.retired=true and i.institutionType =:tp"
                + " order by i.name";
        m.put("tp", InstitutionType.Dealer);
        dealor = getEjbFacade().findByJpql(sql, m);



    }

    private InstitutionFacade getFacade() {
        return ejbFacade;
    }

    public List<Institution> getDealor() {
        return dealor;
    }

    public void setDealor(List<Institution> dealor) {
        this.dealor = dealor;
    }

    public List<Institution> getItems() {
        String sql = "SELECT i FROM Institution i "
                + " where i.retired=false and i.institutionType =:tp"
                + " order by i.name";
        HashMap hm = new HashMap();
        hm.put("tp", InstitutionType.Dealer);
        items = getEjbFacade().findByJpql(sql, hm);
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    /**
     *
     */
    @FacesConverter("deal")
    public static class DealerControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            DealerController controller = (DealerController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "dealerController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            long key;
            key = Long.parseLong(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            return String.valueOf(value);
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Institution) {
                Institution o = (Institution) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + DealerController.class.getName());
            }
        }
    }
}
