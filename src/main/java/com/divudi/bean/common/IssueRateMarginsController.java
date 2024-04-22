/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.ejb.PharmacyBean;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.IssueRateMargins;
import com.divudi.facade.IssueRateMarginsFacade;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class IssueRateMarginsController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    CommonController commonController;
    @EJB
    private IssueRateMarginsFacade ejbFacade;
    private IssueRateMargins current;
    private List<IssueRateMargins> items = null;

    Institution fromInstitution;
    Institution toInstitution;
    Department fromDepartment;
    Department toDepartment;
    IssueRateMargins issueRateMargins;

    public void save() {
        if (getCurrent().getId() == null || getCurrent().getId() == 0) {
            getFacade().create(current);
        } else {
            getFacade().edit(current);
        }
    }

    public void makeNull() {
        issueRateMargins = null;
    }

    @EJB
    PharmacyBean pharmacyBean;

    public void add() {
        IssueRateMargins tmp = pharmacyBean.fetchIssueRateMargins(fromDepartment, getIssueRateMargins().getToDepartment());

        if (tmp != null) {
            JsfUtil.addErrorMessage("Already Exist");
            return;
        }

        getIssueRateMargins().setFromDepartment(fromDepartment);

        if (getIssueRateMargins().getId() == null) {
            ejbFacade.create(getIssueRateMargins());
        } else {
            ejbFacade.edit(getIssueRateMargins());
        }

        createMargins();

        issueRateMargins = null;
    }
    @Inject
    DepartmentController departmentController;

    public void addAllDep() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (fromDepartment == null) {
            return;
        }
        for (Department d : departmentController.getItems()) {
            IssueRateMargins tmp = pharmacyBean.fetchIssueRateMargins(fromDepartment, d);
            if (tmp != null) {
                continue;
            }

            IssueRateMargins newIssueRateMargins = new IssueRateMargins();
            newIssueRateMargins.setFromDepartment(fromDepartment);
            newIssueRateMargins.setToDepartment(d);
            newIssueRateMargins.setAtPurchaseRate(true);
            ejbFacade.create(newIssueRateMargins);
        }

        createMargins();
        
        
    }

    public void onEdit(IssueRateMargins tmp) {
        ejbFacade.edit(tmp);
        createMargins();
    }

    public void delete(IssueRateMargins tmp) {
        tmp.setRetired(Boolean.TRUE);
        ejbFacade.edit(tmp);
        createMargins();
    }

    public Institution getFromInstitution() {
        return fromInstitution;
    }

    public IssueRateMargins getIssueRateMargins() {
        if (issueRateMargins == null) {
            issueRateMargins = new IssueRateMargins();
        }
        return issueRateMargins;
    }

    public void setIssueRateMargins(IssueRateMargins issueRateMargins) {
        this.issueRateMargins = issueRateMargins;
    }

    public void setFromInstitution(Institution fromInstitution) {
        this.fromInstitution = fromInstitution;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public IssueRateMarginsFacade getEjbFacade() {
        return ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public IssueRateMarginsController() {
    }

    public void createMargins() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        String sql;
        sql = "select m from IssueRateMargins m "
                + " where m.retired=false ";

        items = ejbFacade.findByJpql(sql);

        
    }

    public IssueRateMargins getCurrent() {
        String sql;
        sql = "select m from IssueRateMargins m where m.fromInstitution=:fi and "
                + "m.toInstitution=:ti and "
                + "m.fromDepartment=:fd and "
                + "m.toDepartment=:td ";
        Map m = new HashMap();
        m.put("fd", fromDepartment);
        m.put("td", toDepartment);
        m.put("fi", fromInstitution);
        m.put("ti", toInstitution);
        current = getFacade().findFirstByJpql(sql, m);
        if (current == null) {
            current = new IssueRateMargins();
            current.setFromDepartment(fromDepartment);
            current.setToDepartment(toDepartment);
            current.setFromInstitution(fromInstitution);
            current.setToInstitution(toInstitution);
            getFacade().create(current);
        }
        return current;
    }

    public void setCurrent(IssueRateMargins current) {
        this.current = current;
    }

    private IssueRateMarginsFacade getFacade() {
        return ejbFacade;
    }

    public List<IssueRateMargins> getItems() {
        return items;
    }

    public void listAll() {
        items = getFacade().findAll();
    }

    /**
     *
     */
    @FacesConverter(forClass = IssueRateMargins.class)
    public static class IssueRateMarginsControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            IssueRateMarginsController controller = (IssueRateMarginsController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "issueRateMarginController");
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
            if (object instanceof IssueRateMargins) {
                IssueRateMargins o = (IssueRateMargins) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + IssueRateMarginsController.class.getName());
            }
        }
    }

    /**
     *
     */
    
    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

}
