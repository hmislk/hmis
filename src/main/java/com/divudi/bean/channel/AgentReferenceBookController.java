/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.InstitutionType;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Institution;
import com.divudi.entity.channel.AgentReferenceBook;
import com.divudi.facade.AgentReferenceBookFacade;
import com.divudi.facade.InstitutionFacade;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Sniper
 */
@Named
@SessionScoped
public class AgentReferenceBookController implements Serializable {

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    AgentReferenceBook agentReferenceBook;
    @EJB
    AgentReferenceBookFacade agentReferenceBookFacade;
    @EJB
    InstitutionFacade institutionFacade;
    @EJB
    CommonFunctions commonFunctions;
    @Inject
    SessionController sessionController;

    List<AgentReferenceBook> agentReferenceBooks;
    Date frmDate;
    Date toDate;

    public List<Institution> completeAgent(String query) {
        List<Institution> suggestions;
        String sql;
        Map m = new HashMap();

        sql = "select c from Institution c where c.retired=false and "
                + " c.institutionType =:t and upper(c.name) like :q order by c.name";
        //System.out.println(sql);
        m.put("t", InstitutionType.Agency);
        m.put("q", "%" + query.toUpperCase() + "%");
        suggestions = getInstitutionFacade().findBySQL(sql, m);
        //System.out.println("suggestions = " + suggestions);

        return suggestions;
    }

    public void saveAgentBook() {
        if (agentReferenceBook.getInstitution() == null) {
            UtilityController.addErrorMessage("Please Select Institution.");
            return;
        }
        if (agentReferenceBook.getBookNumber() == 0.0) {
            UtilityController.addErrorMessage("please Enter Book Number.");
            return;
        }
        if (agentReferenceBook.getStartingReferenceNumber() == 0.0) {
            UtilityController.addErrorMessage("please Enter Starting Reference Number.");
            return;
        }
        if (agentReferenceBook.getEndingReferenceNumber() == 0.0) {
            UtilityController.addErrorMessage("please Enter Ending Reference Number.");
            return;
        }

        String sql;
        sql = "select a from AgentReferenceBook a where "
                + " a.retired=false ";

        agentReferenceBooks = getAgentReferenceBookFacade().findBySQL(sql);

        for (AgentReferenceBook arb : agentReferenceBooks) {
            if (arb.getBookNumber() == agentReferenceBook.getBookNumber()) {
                UtilityController.addErrorMessage("Book Number Is Alredy Given");
                return;
            }
            if (arb.getStartingReferenceNumber() == agentReferenceBook.getStartingReferenceNumber()) {
                UtilityController.addErrorMessage("Starting Reference Number Is Alredy Given");
                return;
            }
            if (arb.getEndingReferenceNumber() == agentReferenceBook.getEndingReferenceNumber()) {
                UtilityController.addErrorMessage("Ending Reference Number Is Alredy Given");
                return;
            }
        }
        getAgentReferenceBook().setCreatedAt(new Date());
        getAgentReferenceBook().setCreater(getSessionController().getLoggedUser());
        getAgentReferenceBook().setDeactivate(false);
        System.out.println("out 1 : " + getAgentReferenceBook().getInstitution().getName());
        System.out.println("out 2 : " + getAgentReferenceBook().getBookNumber());
        System.out.println("out 3 : " + getAgentReferenceBook().getStartingReferenceNumber());
        System.out.println("out 4 : " + getAgentReferenceBook().getEndingReferenceNumber());
        System.out.println("out 5 : " + getAgentReferenceBook().getCreatedAt());
        System.out.println("out 6 : " + getAgentReferenceBook().getCreater());
        getAgentReferenceBookFacade().create(agentReferenceBook);
        UtilityController.addSuccessMessage("Saved");
        makeNull();

    }

    public void createAllBooks() {
        String sql;
        HashMap m = new HashMap();

        sql = "select a from AgentReferenceBook a where "
                + " a.createdAt between :fd and :td "
                + " and a.retired=false ";

        if (getAgentReferenceBook().getInstitution() != null) {
            sql += " and a.institution=:ins ";
            m.put("ins", getAgentReferenceBook().getInstitution());
        }

        sql += " order by a.bookNumber ";

        m.put("fd", frmDate);
        m.put("td", toDate);

        agentReferenceBooks = getAgentReferenceBookFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
    }

    public void updateDecactivateAgentBook(AgentReferenceBook a) {

        a.setEditor(getSessionController().getLoggedUser());
        a.setEditedAt(new Date());
        getAgentReferenceBookFacade().edit(a);

        UtilityController.addSuccessMessage("Updated");
        createAllBooks();
    }

    public void makeNull() {
        agentReferenceBook = null;
    }

    public Boolean checkAgentReferenceNumber(Institution institution, String refNumber) {

        Double dbl = null;
        try {
            dbl = Double.parseDouble(refNumber);
        } catch (Exception e) {
            return false;
        }

        String sql;
        HashMap m = new HashMap();

        sql = "select a from AgentReferenceBook a where "
                + " a.startingReferenceNumber<= :ag "
                + " and a.endingReferenceNumber>= :ag "
                + " and a.retired=false "
                + " and a.deactivate=false "
                + " and a.institution=:ins";

        m.put("ins", institution);
        m.put("ag", dbl);

        AgentReferenceBook agentReferenceBook = getAgentReferenceBookFacade().findFirstBySQL(sql, m, TemporalType.DATE);

        if (agentReferenceBook == null) {
            return true;
        } else {
            return false;
        }
        
    }

    public AgentReferenceBook getAgentReferenceBook() {
        if (agentReferenceBook == null) {
            agentReferenceBook = new AgentReferenceBook();
        }
        return agentReferenceBook;
    }

    public void setAgentReferenceBook(AgentReferenceBook agentReferenceBook) {
        this.agentReferenceBook = agentReferenceBook;
    }

    public AgentReferenceBookFacade getAgentReferenceBookFacade() {
        return agentReferenceBookFacade;
    }

    public void setAgentReferenceBookFacade(AgentReferenceBookFacade agentReferenceBookFacade) {
        this.agentReferenceBookFacade = agentReferenceBookFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public List<AgentReferenceBook> getAgentReferenceBooks() {
        return agentReferenceBooks;
    }

    public void setAgentReferenceBooks(List<AgentReferenceBook> agentReferenceBooks) {
        this.agentReferenceBooks = agentReferenceBooks;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public Date getFrmDate() {
        if (frmDate == null) {
            frmDate = com.divudi.java.CommonFunctions.getStartOfMonth(new Date());
        }
        return frmDate;
    }

    public void setFrmDate(Date frmDate) {
        this.frmDate = frmDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = com.divudi.java.CommonFunctions.getEndOfMonth(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

}
