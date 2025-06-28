/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Consultant in Health Informatics
 * (94) 71 5812399
 * (94) 71 5812399
 * buddhika.ari@gmail.com
 *
 */
package com.divudi.bean.common.membership;

import com.divudi.bean.common.SessionController;
import com.divudi.core.entity.Family;
import com.divudi.core.entity.FamilyMember;
import com.divudi.core.facade.FamilyFacade;
import com.divudi.core.facade.FamilyMemberFacade;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Consultant
 * (Health Informatics)
 *
 */
@Named
@SessionScoped
public class FamilyController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private FamilyFacade ejbFacade;
    @EJB
    FamilyMemberFacade familyMemberFacade;
    private Family current;
    private List<Family> families = null;
    private List<FamilyMember> members = null;

    public String navigateToListFamilies() {
        families=fetchAllFamilies();
        return "/membership/analytics/lists/families?faces-redirect=true";
    }

    public String navigateToListFamilyMembers() {
        members = fetchAllMembers();
        return "/membership/analytics/lists/members?faces-redirect=true";
    }

    public List<Family>  fetchAllFamilies(){
        String jpql = "select f "
                + " from Family f "
                + " where f.retired=:ret "
                + " order by f.membershipCardNo";
        Map m = new HashMap();
        m.put("ret", false);
        return ejbFacade.findByJpql(jpql, m);
    }

    public List<FamilyMember>  fetchAllMembers(){
        String jpql = "select fm "
                + " from FamilyMember fm "
                + " where fm.retired=:ret "
                + " order by fm.id";
        Map m = new HashMap();
        m.put("ret", false);
        return familyMemberFacade.findByJpql(jpql, m);
    }

    public void save(Family family) {
        if (family == null) {
            return;
        }
        if (family.getId() != null) {
            getFacade().edit(family);
        } else {
            family.setCreatedAt(new Date());
            family.setCreater(sessionController.getLoggedUser());
            getFacade().create(family);
        }
    }

    public FamilyFacade getEjbFacade() {
        return ejbFacade;
    }

    public FamilyController() {
    }

    public Family getCurrent() {
        if (current == null) {
            current = new Family();
        }
        return current;
    }

    public void setCurrent(Family current) {
        this.current = current;
    }

    private FamilyFacade getFacade() {
        return ejbFacade;
    }

    public List<FamilyMember> getMembers() {
        return members;
    }

    public void setMembers(List<FamilyMember> members) {
        this.members = members;
    }

    public List<Family> getFamilies() {
        return families;
    }

    public void setFamilies(List<Family> families) {
        this.families = families;
    }



    /**
     *
     */
    @FacesConverter(forClass = Family.class)
    public static class FamilyConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            FamilyController controller = (FamilyController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "familyController");
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
            if (object instanceof Family) {
                Family o = (Family) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + FamilyController.class.getName());
            }
        }
    }

}
