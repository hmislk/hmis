/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.membership;

import com.divudi.bean.common.SessionController;

import com.divudi.entity.Institution;
import com.divudi.entity.Patient;
import com.divudi.entity.membership.MembershipScheme;
import com.divudi.facade.MembershipSchemeFacade;
import com.divudi.bean.common.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class MembershipSchemeController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private MembershipSchemeFacade ejbFacade;
    List<MembershipScheme> selectedItems;
    private MembershipScheme current;
    private List<MembershipScheme> items = null;
    String selectText = "";
    Institution lastInstitution;

    public MembershipScheme fetchPatientMembershipScheme(Patient patient, boolean hasExpiary) {

        MembershipScheme membershipScheme = null;
        if (hasExpiary) {
            if (patient != null
                    && patient.getPerson() != null) {

                Date fromDate = patient.getFromDate();
                Date toDate = patient.getToDate();

                if (fromDate != null && toDate != null) {
                    Calendar fCalendar = Calendar.getInstance();
                    fCalendar.setTime(fromDate);
                    Calendar tCalendar = Calendar.getInstance();
                    tCalendar.setTime(toDate);
                    Calendar nCalendar = Calendar.getInstance();

                    if (((fromDate.before(new Date()) && toDate.after(new Date())))
                            || (fCalendar.get(Calendar.DATE) == nCalendar.get(Calendar.DATE) || tCalendar.get(Calendar.DATE) == nCalendar.get(Calendar.DATE))) {
                        membershipScheme = patient.getPerson().getMembershipScheme();
                    }
                }
            }
        } else {
            if (patient !=null && patient.getPerson()!=null && patient.getPerson().getMembershipScheme() != null) {
                membershipScheme = patient.getPerson().getMembershipScheme();
            }
        }
        return membershipScheme;
    }

    public List<MembershipScheme> completeMembershipScheme(String qry) {
        List<MembershipScheme> c;
        HashMap hm = new HashMap();
        String sql = "select c from MembershipScheme c where c.retired=false and (c.name) "
                + " like :q order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        c = getFacade().findByJpql(sql, hm);

        if (c == null) {
            c = new ArrayList<>();
        }
        return c;
    }

    public List<MembershipScheme> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from MembershipScheme c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        current = new MembershipScheme();
        fillItems();
    }

    public void setSelectedItems(List<MembershipScheme> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {
//        getCurrent().setInstitution(getSessionController().getInstitution());
        if (getCurrent().getCode() == null || getCurrent().getCode().equals("")) {
            JsfUtil.addErrorMessage("Please Select Code Like \"LM\"");
            return;
        }
        if (getCurrent().getCode().length() > 2) {
            JsfUtil.addErrorMessage("Please Set Code Using 2 Charactors");
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
        fillItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public MembershipSchemeFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(MembershipSchemeFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public MembershipSchemeController() {
    }

    public MembershipScheme getCurrent() {
        if (current == null) {
            current = new MembershipScheme();
        }
        return current;
    }

    public void setCurrent(MembershipScheme current) {
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
        fillItems();
        current = null;
        getCurrent();
    }

    private MembershipSchemeFacade getFacade() {
        return ejbFacade;
    }

    public List<MembershipScheme> getItems() {
        if (items == null) {
            fillItems();
        }
        return items;
    }

    public void fillItems() {
        String j;
        j = "select s "
                + " from MembershipScheme s "
                + " where s.retired=false "
                + " and s.institution=:ins "
                + " order by s.name";
        Map m = new HashMap();
        m.put("ins", sessionController.getInstitution());
        items = getFacade().findByJpql(j, m);
    }

    public Institution getLastInstitution() {
        return lastInstitution;
    }

    public void setLastInstitution(Institution lastInstitution) {
        this.lastInstitution = lastInstitution;
    }

    /**
     *
     */
    @FacesConverter(forClass = MembershipScheme.class)
    public static class MembershipSchemeControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            MembershipSchemeController controller = (MembershipSchemeController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "membershipSchemeController");
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
            if (object instanceof MembershipScheme) {
                MembershipScheme o = (MembershipScheme) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + MembershipSchemeController.class.getName());
            }
        }
    }

    /**
     *
     */
    
}
