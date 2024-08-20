/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.entity.AuditEvent;
import com.divudi.facade.AuditEventFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class AuditEventController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private AuditEventFacade ejbFacade;
    private AuditEvent current;
    private List<AuditEvent> items = null;
    @Inject
    AuditEventApplicationController auditEventApplicationController;

    private Date fromDate;
    private Date toDate;

    public void updateAuditEvent(String buddhika) {
        String jpql = " select a "
                + " from AuditEvent a "
                + " where a.uuid=:niluka ";
        Map m = new HashMap();
        m.put("niluka", buddhika);
        AuditEvent auditEvent = getFacade().findFirstByJpql(jpql, m);
        if (auditEvent == null) {
            return;
        }
        Long duration;
        Date endTime = new Date();
        duration = endTime.getTime() - auditEvent.getEventDataTime().getTime();

        auditEvent.setEventEndTime(endTime);
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.saveAutitEvent(auditEvent);
    }

    public String navigateToAuditEventList() {
        return "/analytics/audit_event_list";
    }

    public String createAuditEvent(String eventName) {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();
        String url = request.getRequestURL().toString();
        String ipAddress = request.getRemoteAddr();
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }
        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger(eventName);
        String uuid = UUID.randomUUID().toString();
        auditEvent.setUuid(uuid);
        auditEventApplicationController.saveAutitEvent(auditEvent);
        return auditEvent.getUuid();
    }

    public List<AuditEvent> completeAuditEvent(String qry) {
        List<AuditEvent> list;
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from AuditEvent c "
                + " where c.retired=false "
                + " and (c.name) like :q "
                + " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        list = getFacade().findByJpql(sql, hm);

        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public void fillAllAuditEvents() {
        List<AuditEvent> list;
        String jpql;
        HashMap hm = new HashMap();
        jpql = "select c from AuditEvent c "
                + " where c.eventDataTime between :fd and :td ";
        hm.put("fd", fromDate);
        hm.put("td", toDate);
        items = getFacade().findByJpql(jpql, hm, TemporalType.TIMESTAMP);
    }

    public void prepareAdd() {
        current = new AuditEvent();
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
        } else {
            getFacade().create(current);
        }
    }

    private AuditEventFacade getEjbFacade() {
        return ejbFacade;
    }

    public AuditEventController() {
    }

    public AuditEvent getCurrent() {
        if (current == null) {
            current = new AuditEvent();
        }
        return current;
    }

    public void setCurrent(AuditEvent current) {
        this.current = current;
    }

    private AuditEventFacade getFacade() {
        return ejbFacade;
    }

    public List<AuditEvent> getItems() {
        return items;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    /**
     *
     */
    @FacesConverter(forClass = AuditEvent.class)
    public static class AuditEventConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            AuditEventController controller = (AuditEventController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "auditEventController");
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
            if (object instanceof AuditEvent) {
                AuditEvent o = (AuditEvent) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + AuditEventController.class.getName());
            }
        }
    }

}
