package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.ChannelService;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Chinthaka Prasad
 */
@Named
@SessionScoped
public class OnlineBookingAgentController implements Serializable {

    private Institution current;
    private List<Institution> allAgents;
    
    private Date fromDate;
    private Date toDate;
    private Institution institutionForBookings;
    private Institution agentForBookings;

    @EJB
    private InstitutionFacade institutionFacade;

    @Inject
    private SessionController sessionController;
    
    @EJB
    private ChannelService channelService;
    

    public List<Institution> getAllAgents() {
        return allAgents;
    }

    public void setAllAgents(List<Institution> allAgents) {
        this.allAgents = allAgents;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public void prepareAdd() {
        current = new Institution();
        current.setInstitutionType(InstitutionType.OnlineBookingAgent);
    }

    public void delete() {

        if (getCurrent() != null && getCurrent().getId() != null) {
            getCurrent().setRetired(true);
            getCurrent().setRetiredAt(new Date());
            getCurrent().setRetirer(getSessionController().getLoggedUser());
            getInstitutionFacade().edit(getCurrent());
            current = null;
            fillAllAgents();
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            current = null;
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
    }

    @PostConstruct
    public void init() {
        fillAllAgents();
    }

    public boolean checkDuplicateCodeAvailability(Institution institution) {
        fillAllAgents();
        for (Institution ins : allAgents) {
            if (institution.getId() == null) {
                if (ins.getCode().equalsIgnoreCase(institution.getCode())) {
                    return true;
                }
            } else if (institution.getId() != null) {
                if (ins.getId() != institution.getId()) {
                    if (ins.getCode().equalsIgnoreCase(institution.getCode())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void saveSelected(boolean isNew) {
        if (getCurrent().getName() == null || getCurrent().getName().isEmpty()) {
            JsfUtil.addErrorMessage("Agent Name is missing.");
            return;
        }

        if (getCurrent().getCode() == null || getCurrent().getCode().isEmpty()) {
            JsfUtil.addErrorMessage("Agent Code is missing.");
            return;
        }
        
        if(checkDuplicateCodeAvailability(current)){
            JsfUtil.addErrorMessage("Agent Code is already taken. Use different one.");
            return;
        }

        if (getCurrent().getId() != null && getCurrent().getId() > 0 && !isNew) {
            getInstitutionFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else if (isNew && getCurrent().getId() == null) {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getInstitutionFacade().create(getCurrent());
            JsfUtil.addSuccessMessage("Saved Successfully");
        } else if (isNew && getCurrent().getId() != null) {
            JsfUtil.addErrorMessage("Please use update Button to edit Agent.");
        } else if (!isNew && getCurrent().getId() == null) {
            JsfUtil.addErrorMessage("Please Save the Agent.");
        }
        fillAllAgents();
    }

    public void fillAllAgents() {
        String j;
        j = "select i "
                + " from Institution i "
                + " where i.retired=:ret"
                + " and i.institutionType = :type"
                + " order by i.name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("type", InstitutionType.OnlineBookingAgent);
        allAgents = getInstitutionFacade().findByJpql(j, m);
    }

    public Institution getCurrent() {
        if (current == null) {
            current = new Institution();
            current.setInstitutionType(InstitutionType.OnlineBookingAgent);
        }
        return current;
    }

    public void setCurrent(Institution current) {
        this.current = current;
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

    public Institution getInstitutionForBookings() {
        return institutionForBookings;
    }

    public void setInstitutionForBookings(Institution institutionForBookings) {
        this.institutionForBookings = institutionForBookings;
    }

    public Institution getAgentForBookings() {
        return agentForBookings;
    }

    public void setAgentForBookings(Institution agentForBookings) {
        this.agentForBookings = agentForBookings;
    }

    public ChannelService getChannelService() {
        return channelService;
    }

    public void setChannelService(ChannelService channelService) {
        this.channelService = channelService;
    }

}
