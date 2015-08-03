/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, agentsFeesoratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.FeeType;
import com.divudi.entity.Fee;
import com.divudi.entity.Institution;
import com.divudi.entity.ServiceSession;
import com.divudi.facade.AgentsFeesFacade;
import com.divudi.entity.channel.AgentsFees;
import com.divudi.facade.FeeFacade;
import com.divudi.facade.ServiceSessionFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 Informatics)
 */
@Named
@SessionScoped
public class AgentsFeesController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    ////////////////////////////
    @EJB
    private AgentsFeesFacade ejbFacade;
    @EJB
    private FeeFacade feeFacade;
    @EJB
    private ServiceSessionFacade serviceSessionFacade;
    /////////////////////////////    
    private List<AgentsFees> items = null;
    //////////////////////////////    
    private AgentsFees current;
    private Institution agent;
    private double feeAmount;

    public void prepareAdd() {
        current = new AgentsFees();
    }

    private void recreateModel() {
        items = null;
        current = null;
        agent = null;
        feeAmount = 0.0;
    }

    private void saveFee(ServiceSession ss) {
        getCurrent().getFee().setServiceSession(ss);
        getCurrent().getFee().setFee(feeAmount);
        getCurrent().getFee().setInstitution(getAgent());
        getFeeFacade().create(getCurrent().getFee());
    }

    public void saveSelected() {
        if (getCurrent().getServiceSession() != null) {

            AgentsFees tp = getEjbFacade().findFirstBySQL("select s from AgentsFees s where s.serviceSession.id=" + getCurrent().getServiceSession().getId()
                    + " and s.agent.id=" + getAgent().getId());

            if (tp != null) {
                tp.getFee().setFee(feeAmount);
                getFeeFacade().edit(tp.getFee());
                return;
            }

            saveFee(getCurrent().getServiceSession());
            getCurrent().setAgent(getAgent());
            if (getCurrent().getId() != null && getCurrent().getId() > 0) {
                getEjbFacade().edit(current);
                UtilityController.addSuccessMessage("Updated Successfully.");
            } else {
                getEjbFacade().create(current);
                UtilityController.addSuccessMessage("Saved Successfully");
            }
            current = null;

        } else {
            removeOldFees();

            String sql = "Select s From ServiceSession s where s.retired=false order by s.staff.speciality.name,s.staff.person.name,s.sessionWeekday,s.startingTime ";
            List<ServiceSession> tmp = getServiceSessionFacade().findBySQL(sql);
            for (ServiceSession ss : tmp) {
                saveFee(ss);
                getCurrent().setAgent(getAgent());
                getCurrent().setServiceSession(ss);
                if (getCurrent().getId() != null && getCurrent().getId() > 0) {
                    getEjbFacade().edit(current);
                } else {
                    getEjbFacade().create(current);
                }
                current = null;
            }

        }


        recreateModel();
        getItems();
    }

    public void removeOldFees() {
        List<AgentsFees> tmp = getEjbFacade().findBySQL("Select i From AgentsFees i where "
                + " i.agent.id=" + getAgent().getId());

        for (AgentsFees ag : tmp) {
            getEjbFacade().remove(ag);
        }
    }

    public AgentsFeesFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(AgentsFeesFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public AgentsFeesController() {
    }

    public AgentsFees getCurrent() {
        if (current == null) {
            current = new AgentsFees();
            current.setFee(new Fee());
            current.getFee().setFeeType(FeeType.OtherInstitution);
        }
        return current;
    }

    public void setCurrent(AgentsFees current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            getEjbFacade().edit(current);
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    public List<AgentsFees> getItems() {
        if (getAgent() != null) {
            items = getEjbFacade().findBySQL("Select i From AgentsFees i where i.agent.id=" + getAgent().getId());
        }
        if (items == null) {
            items = new ArrayList<AgentsFees>();
        }

        return items;
    }

    public FeeFacade getFeeFacade() {
        return feeFacade;
    }

    public void setFeeFacade(FeeFacade feeFacade) {
        this.feeFacade = feeFacade;
    }

    public ServiceSessionFacade getServiceSessionFacade() {
        return serviceSessionFacade;
    }

    public void setServiceSessionFacade(ServiceSessionFacade serviceSessionFacade) {
        this.serviceSessionFacade = serviceSessionFacade;
    }

    public Institution getAgent() {
        return agent;
    }

    public void setAgent(Institution agent) {
        this.agent = agent;

    }

    public double getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(double feeAmount) {
        this.feeAmount = feeAmount;
    }

    /**
     *
     */
    @FacesConverter(forClass = AgentsFees.class)
    public static class AgentsFeesControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            AgentsFeesController controller = (AgentsFeesController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "agentsFeesController");
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
            if (object instanceof AgentsFees) {
                AgentsFees o = (AgentsFees) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + AgentsFeesController.class.getName());
            }
        }
    }
}
