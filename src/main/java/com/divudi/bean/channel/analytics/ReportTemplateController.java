/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.channel.analytics;

import com.divudi.bean.common.*;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.analytics.ReportTemplateColumn;
import com.divudi.data.analytics.ReportTemplateType;
import com.divudi.entity.Department;
import com.divudi.entity.ReportTemplate;
import com.divudi.entity.Institution;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.facade.ReportTemplateFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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
 * in Health Informatics
 */
@Named
@SessionScoped
public class ReportTemplateController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private ReportTemplateFacade ejbFacade;
    private ReportTemplate current;
    private List<ReportTemplate> items = null;
    
    
    private Date date;
    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Department department;
    private WebUser user;
    private Staff staff;
    

    public void save(ReportTemplate reportTemplate) {
        if (reportTemplate == null) {
            return;
        }
        if (reportTemplate.getId() != null) {
            getFacade().edit(reportTemplate);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            reportTemplate.setCreatedAt(new Date());
            reportTemplate.setCreater(getSessionController().getLoggedUser());
            getFacade().create(reportTemplate);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
    }

    
    
    

    public ReportTemplate findReportTemplateByName(String name) {
        if (name == null) {
            return null;
        }
        if (name.trim().equals("")) {
            return null;
        }
        String jpql = "select a "
                + " from ReportTemplate a "
                + " where a.retired=:ret "
                + " and a.name=:n";
        Map m = new HashMap<>();
        m.put("ret", false);
        m.put("n", name);
        return getFacade().findFirstByJpql(jpql, m);
    }

    public List<ReportTemplate> completeReportTemplate(String qry) {
        List<ReportTemplate> list;
        String jpql;
        HashMap params = new HashMap();
        jpql = "select c from ReportTemplate c "
                + " where c.retired=false "
                + " and (c.name) like :q "
                + " order by c.name";
        params.put("q", "%" + qry.toUpperCase() + "%");
        list = getFacade().findByJpql(jpql, params);

        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public void prepareAdd() {
        current = new ReportTemplate();
    }

    public void recreateModel() {
        items = null;
    }

    public void processReport() {

    }

    public void saveSelected() {
        if (getCurrent().getName().isEmpty() || getCurrent().getName() == null) {
            JsfUtil.addErrorMessage("Please enter Value");
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

    public ReportTemplateFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(ReportTemplateFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public ReportTemplateController() {
    }

    public ReportTemplate getCurrent() {
        if (current == null) {
            current = new ReportTemplate();
        }
        return current;
    }

    public void setCurrent(ReportTemplate current) {
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
        getItems();
        current = null;
        getCurrent();
    }

    private ReportTemplateFacade getFacade() {
        return ejbFacade;
    }

    public String navigateToReportTemplateList() {
        items = getAllItems();
        return "/dataAdmin/report_template_list";
    }

    public String navigateToAddNewReportTemplate() {
        current = new ReportTemplate();
        return "/dataAdmin/report_template";
    }

    public void deleteReportTemplate() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        current.setRetired(true);
        current.setRetirer(sessionController.getLoggedUser());
        current.setRetiredAt(new Date());
        save(current);
        items = getAllItems();
        JsfUtil.addSuccessMessage("Removed");
    }

    public List<ReportTemplateType> getReportTemplateTypes() {
        return Arrays.asList(ReportTemplateType.values());
    }

    public String navigateToEditReportTemplate() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return null;
        }
        return "/dataAdmin/report_template";
    }

    public String navigateToEditGenerateReport() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return null;
        }
        return "/dataAdmin/report";
    }

    public List<ReportTemplate> getAllItems() {
        List<ReportTemplate> allItems;
        String j;
        j = "select a "
                + " from ReportTemplate a "
                + " where a.retired=false "
                + " order by a.name";
        allItems = getFacade().findByJpql(j);
        return allItems;
    }

    public List<ReportTemplate> getItems() {
        return items;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public WebUser getUser() {
        return user;
    }

    public void setUser(WebUser user) {
        this.user = user;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    /**
     *
     */
    @FacesConverter(forClass = ReportTemplate.class)
    public static class ReportTemplateConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ReportTemplateController controller = (ReportTemplateController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "reportTemplateController");
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
            if (object instanceof ReportTemplate) {
                ReportTemplate o = (ReportTemplate) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ReportTemplate.class.getName());
            }
        }
    }

}
