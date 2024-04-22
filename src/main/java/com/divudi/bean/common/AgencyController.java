/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.data.HistoryType;
import com.divudi.data.InstitutionType;
import com.divudi.entity.AgentHistory;
import com.divudi.entity.Institution;
import com.divudi.facade.AgentHistoryFacade;
import com.divudi.facade.InstitutionFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import com.divudi.bean.common.util.JsfUtil;
/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class AgencyController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private InstitutionFacade ejbFacade;
    @EJB
    AgentHistoryFacade agentHistoryFacade;
    List<Institution> selectedItems;
    private Institution current;
    private List<Institution> items = null;
    String selectText = "";

    public void randomlySetAgencyBalances() {
        List<Institution> suggestions;
        String sql;
        Map m = new HashMap();
        double b1 = 50000;
        double b2 = 30000;
        m.put("it", InstitutionType.Agency);
        sql = "select p from Institution p where"
                + "  p.retired=false and "
                + " p.institutionType=:it "
                + " order by p.name";
        suggestions = getFacade().findByJpql(sql, m);

        Collections.shuffle(suggestions);
        List<Institution> b1s = suggestions.subList(0, suggestions.size() / 2);
        List<Institution> b2s = suggestions.subList(suggestions.size() / 2, suggestions.size());

        for (Institution i : b1s) {
            double pb = i.getBallance();
            AgentHistory agentHistory = new AgentHistory();
            agentHistory.setCreatedAt(new Date());
            agentHistory.setCreater(getSessionController().getLoggedUser());
            agentHistory.setBill(null);
            agentHistory.setBeforeBallance(pb);
            agentHistory.setTransactionValue(b1);
            agentHistory.setHistoryType(HistoryType.ChannelBalanceReset);
            agentHistoryFacade.create(agentHistory);
            i.setBallance(b1);
            ejbFacade.edit(i);
        }

        for (Institution i : b2s) {
            double pb = i.getBallance();
            AgentHistory agentHistory = new AgentHistory();
            agentHistory.setCreatedAt(new Date());
            agentHistory.setCreater(getSessionController().getLoggedUser());
            agentHistory.setBill(null);
            agentHistory.setBeforeBallance(pb);
            agentHistory.setTransactionValue(b2);
            agentHistory.setHistoryType(HistoryType.ChannelBalanceReset);
            agentHistoryFacade.create(agentHistory);
            i.setBallance(b2);
            ejbFacade.edit(i);
        }

        
    }

    public List<Institution> completeAgency(String query) {
        List<Institution> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            Map m = new HashMap();
            m.put("it", InstitutionType.Agency);
            sql = "select p from Institution p where"
                    + "  p.retired=false and "
                    + " p.institutionType=:it "
                    + " and (((p.name) like '%" + query.toUpperCase() + "%') "
                    + " or ((p.institutionCode) like '%" + query.toUpperCase() + "%') ) "
                    + " order by p.name";
            //////// // System.out.println(sql);
            suggestions = getFacade().findByJpql(sql, m, 20);
        }
        return suggestions;
    }

    public List<Institution> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from Institution c where c.retired=false and i.institutionType = com.divudi.data.InstitutionType.Agency and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        current = new Institution();
        current.setInstitutionType(InstitutionType.Agency);
    }

    public void setSelectedItems(List<Institution> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {

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

    public void setSelectText(String selectText) {
        this.selectText = selectText;
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

    public AgencyController() {
    }

    public Institution getCurrent() {
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
        getItems();
        current = null;
        getCurrent();
    }

    private InstitutionFacade getFacade() {
        return ejbFacade;
    }

    public List<Institution> getItems() {
        if (items == null) {
            String sql = "SELECT i FROM Institution i where i.retired=false and i.institutionType = com.divudi.data.InstitutionType.Agency order by i.name";
            items = getEjbFacade().findByJpql(sql);
        }
        return items;
    }

}
