/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.AuditEventController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;

import com.divudi.core.data.BillType;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.channel.ReferenceBookEnum;

import com.divudi.core.entity.AgentHistory;
import com.divudi.core.entity.AuditEvent;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.channel.AgentReferenceBook;
import com.divudi.core.facade.AgentHistoryFacade;
import com.divudi.core.facade.AgentReferenceBookFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.util.CommonFunctions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    private static final long serialVersionUID = 1L;
    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")

    @EJB
    AgentReferenceBookFacade agentReferenceBookFacade;
    @EJB
    InstitutionFacade institutionFacade;
    @EJB
    private AgentHistoryFacade agentHistoryFacade;

    @Inject
    SessionController sessionController;
    @Inject
    private AuditEventController auditEventController;
    @Inject
    private WebUserController webUserController;

    AgentReferenceBook agentReferenceBook;
    private List<AgentReferenceBook> agentReferenceBooks;
    private List<AgentReferenceBook> selectedList;
    private List<AgentReferenceBook> agentRefBookList;
    private Date frmDate;
    private Date toDate;
    private AuditEvent editingCcBookEvent;

    private String comment;

    private boolean bookActive;

    private Institution collectingCentre;

    private AgentReferenceBook current;

    public String navigateToBookEditHistory(Long refBookID) {
        fillBookEditDetails(refBookID);
        return "/collecting_centre/cc_book_edit_history?faces-redirect=true";
    }

    public String navigateToBackCCBookSearch() {
        return "/collecting_centre/report_collecting_center_referece_book?faces-redirect=true";
    }

    private List<AuditEvent> refBookEditDetails;

    public void fillBookEditDetails(Long refBookID) {
        refBookEditDetails = new ArrayList();
        String eventTrigger = "Edit Collection Centre Referance Book";
        refBookEditDetails = auditEventController.fillAllAuditEvents(refBookID, eventTrigger);
    }

    public static String escapeSingleBackslash(String json) {
        if (json == null) {
            return null;
        }
        // Replace a single backslash (\) with double backslash (\\)
        return json.replace("\\", "\\\\")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static String findDifferences(String beforeJson, String afterJson) {
        Map<String, String> differences = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();

        try {
            Map<String, Object> map1 = mapper.readValue(escapeSingleBackslash(beforeJson), Map.class);
            Map<String, Object> map2 = mapper.readValue(escapeSingleBackslash(afterJson), Map.class);

            Set<String> allKeys = new HashSet<>();
            allKeys.addAll(map1.keySet());
            allKeys.addAll(map2.keySet());

            for (String key : allKeys) {
                Object val1 = map1.get(key);
                Object val2 = map2.get(key);

                if (!Objects.equals(val1, val2)) {
                    differences.put(key, String.valueOf(val1) + "  ->  " + String.valueOf(val2));
                }
            }
        } catch (JsonProcessingException e) {
            return "Error parsing JSON: " + e.getMessage();
        }
        // Build a string from the differences map
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : differences.entrySet()) {
            result.append(entry.getKey())
                    .append("  =  ")
                    .append(entry.getValue())
                    .append("\n");
        }

        return result.toString().replace("\n", "<br/>");
    }

    public List<Institution> completeAgent(String query) {
        List<Institution> suggestions;
        String sql;
        Map m = new HashMap();

        sql = "select c from Institution c where c.retired=false and "
                + " c.institutionType =:t and c.name like :q order by c.name";

        m.put("t", InstitutionType.Agency);
        m.put("q", "%" + query.toUpperCase() + "%");
        suggestions = getInstitutionFacade().findByJpql(sql, m);

        return suggestions;
    }

    public void saveLabBook() {
        saveAgentBook(ReferenceBookEnum.LabBook);
    }

    public void saveChannelBook() {
        saveAgentBook(ReferenceBookEnum.ChannelBook);
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

        AgentReferenceBook agentReferenceBook = getAgentReferenceBookFacade().findFirstByJpql(sql, m, TemporalType.DATE);

        if (agentReferenceBook == null) {
            return true;
        } else {
            return false;
        }

    }

    public Boolean checkAgentReferenceNumberAlredyExsist(String refNumber, Institution institution, BillType bt, PaymentMethod pm) {
        Double dbl = null;
        try {
            dbl = Double.parseDouble(refNumber);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String sql;
        Map m = new HashMap();

        sql = " select ah from AgentHistory ah join ah.bill b "
                + " where "
                + " b.retired=false "
                + " and b.billType=:bt "
                + " and b.paymentMethod=:pm "
                + " and b.creditCompany=:ins "
                + " and ((ah.referenceNo) like :rn) ";

        m.put("bt", bt);
        m.put("pm", pm);
        m.put("ins", institution);
        m.put("rn", "%" + refNumber.toUpperCase() + "%");

        List<AgentHistory> ahs = agentHistoryFacade.findByJpql(sql, m);

        if (ahs.isEmpty()) {
            return false;
        } else {
            return true;
        }

    }

    public void beginCcBookAudit(AgentReferenceBook agentReferenceBook) {
        String bookJson = agentReferenceBook.toString();
        editingCcBookEvent = auditEventController.createNewAuditEvent("Edit Collection Centre Referance Book", bookJson, agentReferenceBook.getId());
    }

    public void saveAgentBook(ReferenceBookEnum bookEnum) {
        // Validate inputs
        if (agentReferenceBook.getInstitution() == null) {
            JsfUtil.addErrorMessage("Please Select Institution.");
            return;
        }
        if (agentReferenceBook.getStrbookNumber().trim().equals("")) {
            JsfUtil.addErrorMessage("Please Enter Book Number.");
            return;
        }
        double startingNumber = agentReferenceBook.getStartingReferenceNumber();
        double endingNumber = agentReferenceBook.getEndingReferenceNumber();
        if (startingNumber <= 0 || startingNumber >= 99) {
            JsfUtil.addErrorMessage("Starting Reference Number should be between 01 and 99.");
            return;
        }
        if (endingNumber <= 1 || endingNumber > 99) {
            JsfUtil.addErrorMessage("Ending Reference Number should be between 02 and 99.");
            return;
        }
        if (startingNumber >= endingNumber) {
            JsfUtil.addErrorMessage("Starting Reference Number must be less than Ending Reference Number.");
            return;
        }

        // Check if book number already exists
        String sql = "SELECT a FROM AgentReferenceBook a WHERE a.retired=false AND a.deactivate=false AND a.referenceBookEnum=:rfe AND a.strbookNumber=:sbNumber";
        HashMap hm = new HashMap();
        hm.put("rfe", bookEnum);
        hm.put("sbNumber", agentReferenceBook.getStrbookNumber().trim());

        AgentReferenceBook arb = getAgentReferenceBookFacade().findFirstByJpql(sql, hm);

        if (arb != null) {
            JsfUtil.addErrorMessage("Book Number Is Already use in " + arb.getInstitution().getCode() + " - " + arb.getInstitution().getName());
            return;
        }

        // Save the agent reference book
        agentReferenceBook.setReferenceBookEnum(bookEnum);
        agentReferenceBook.setCreatedAt(new Date());
        agentReferenceBook.setCreater(getSessionController().getLoggedUser());
        agentReferenceBook.setDeactivate(false);
        getAgentReferenceBookFacade().create(agentReferenceBook);
        JsfUtil.addSuccessMessage("Saved");
        makeNull();
    }

    public void commonErrorMessageForSaveChannelBook(AgentReferenceBook arb) {
        JsfUtil.addErrorMessage("Agent Name - " + arb.getInstitution().getName() + "(" + arb.getInstitution().getCode() + ")");
        JsfUtil.addErrorMessage("Book No - " + arb.getBookNumber());
        JsfUtil.addErrorMessage("Starting Ref. Number - " + arb.getStartingReferenceNumber());
        JsfUtil.addErrorMessage("Ending Ref. Number - " + arb.getEndingReferenceNumber());
    }

    public void searchReferenceBooks() {
        createAllBookTable();
    }

    public void updateAgentBook(AgentReferenceBook book) {

        if (book == null) {
            JsfUtil.addErrorMessage("No Book Selected");
            return;
        }
        if (book.getId() == null) {
            JsfUtil.addErrorMessage("No Reference Book Selected");
            return;
        }
        if (editingCcBookEvent == null) {
            JsfUtil.addErrorMessage("No Audit Event");
            return;
        }

        if (!getWebUserController().hasPrivilege("EditData")) {
            searchReferenceBooks();
            JsfUtil.addErrorMessage("You have No Privilege for Edit Book.");
            return;
        }

        // Check if book number already exists
        String sql = "SELECT a FROM AgentReferenceBook a "
                + " WHERE a.retired=false "
                + " AND a.deactivate=false "
                + " AND a.referenceBookEnum=:rfe "
                + " AND a.strbookNumber=:sbNumber";
        HashMap hm = new HashMap();
        hm.put("rfe", ReferenceBookEnum.LabBook);
        hm.put("sbNumber", book.getStrbookNumber() == null ? null : book.getStrbookNumber().trim());

        AgentReferenceBook arb = getAgentReferenceBookFacade().findFirstByJpql(sql, hm);

        if (arb != null) {
            searchReferenceBooks();
            JsfUtil.addErrorMessage("Book number is already used in " + arb.getInstitution().getCode() + " - " + arb.getInstitution().getName());
            return;
        } else {
            book.setEditedAt(new Date());
            book.setEditor(sessionController.getLoggedUser());
            getAgentReferenceBookFacade().edit(book);

            auditEventController.completeAuditEvent(editingCcBookEvent, book.toString());

            JsfUtil.addSuccessMessage("Agent Reference Book was Successfully Updated");
        }
        searchReferenceBooks();
    }

    public void deleteAgentBook(AgentReferenceBook agentReferenceBook) {
        if (getComment() == null || getComment().trim() == "") {
            JsfUtil.addErrorMessage("Enter the comment");
            return;
        }
        if (!getWebUserController().hasPrivilege("DeleteData")) {
            JsfUtil.addErrorMessage("You have No Privilege for Delete Book.");
            return;
        }

        agentReferenceBook.setRetired(true);
        agentReferenceBook.setRetiredAt(new Date());
        agentReferenceBook.setRetirer(sessionController.getLoggedUser());
        agentReferenceBook.setRetireComments(comment);
        getAgentReferenceBookFacade().edit(agentReferenceBook);
        JsfUtil.addSuccessMessage("Agent Reference Book was Successfully Deleted");
    }

    public void createAllBookTable() {
        String jpql;
        HashMap m = new HashMap();
        jpql = "select a from AgentReferenceBook a where "
                + " a.createdAt between :fd and :td ";

        if (getCollectingCentre() != null) {
            jpql += " and a.institution=:cc";
            m.put("cc", getCollectingCentre());
        }

        m.put("fd", frmDate);
        m.put("td", toDate);

        agentRefBookList = getAgentReferenceBookFacade().findByJpql(jpql, m, TemporalType.DATE);
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

        agentReferenceBooks = getAgentReferenceBookFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public void bulkDeleteChannelBooks() {
        if (selectedList == null) {
            JsfUtil.addSuccessMessage("Nothing to Delete");
            return;
        }
        if (selectedList.isEmpty()) {
            JsfUtil.addSuccessMessage("Nothing to Delete(Empty)");
            return;
        }
        for (AgentReferenceBook rb : selectedList) {
            rb.setRetired(true);
            rb.setRetireComments("Bulk Delete");
            rb.setDeactivate(true);
            rb.setRetirer(getSessionController().getLoggedUser());
            rb.setRetiredAt(new Date());
            getAgentReferenceBookFacade().edit(rb);
        }
        createAllBooks();
    }

    public void makeNull() {
        agentReferenceBook = null;
    }

    public Boolean numberHasBeenIssuedToTheAgent(Institution institution, String refNumber) {
        if (institution == null || refNumber == null || refNumber.length() <= 2) {
            return false;
        }

        // Extract the book number and leaf number
        String bookNumber = refNumber.substring(0, refNumber.length() - 2);
        String leafNumberStr = refNumber.substring(refNumber.length() - 2);

        int leafNumber;
        try {
            leafNumber = Integer.parseInt(leafNumberStr);
        } catch (NumberFormatException e) {
            return false; // Leaf number is not a valid integer
        }

        HashMap<String, Object> m = new HashMap<>();
        m.put("ins", institution);
        m.put("bookNo", bookNumber);

        String jpql = "SELECT a FROM AgentReferenceBook a WHERE a.retired=false AND a.deactivate=false AND a.institution=:ins AND a.strbookNumber=:bookNo";

        AgentReferenceBook book = getAgentReferenceBookFacade().findFirstByJpql(jpql, m);

        if (book == null) {
            return false; // No matching book found
        }

// Convert double values to int for comparison
        int start = (int) book.getStartingReferenceNumber();
        int end = (int) book.getEndingReferenceNumber();

// Check if the leaf number is within the valid range of the found book
        return leafNumber >= start && leafNumber <= end;
    }

    public Boolean checkAgentReferenceNumber(String refNumber) {
        Double dbl = null;
        try {
            dbl = Double.parseDouble(refNumber);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    public Boolean agentReferenceNumberIsAlredyUsed(String refNumber, Institution institution, BillType bt, PaymentMethod pm) {
        String sql;
        Map m = new HashMap();
        sql = " select ah from AgentHistory ah "
                + " join ah.bill b "
                + " where b.retired=false "
                + " and b.billType=:bt "
                + " and b.paymentMethod=:pm "
                + " and b.creditCompany=:ins "
                + " and ah.referenceNumber like :rn ";
        m.put("bt", bt);
        m.put("pm", pm);
        m.put("ins", institution);
        m.put("rn", "%" + refNumber.toUpperCase() + "%");
        AgentHistory ahs = agentHistoryFacade.findFirstByJpql(sql, m);
        if (ahs == null) {
            return false;
        } else {
            return true;
        }
    }

    public boolean checkAgentReferenceNumberIsAlreadyUtilized(String refNumber) {
        String sql;
        Map m = new HashMap();
        sql = " select b "
                + " from Bill b "
                + " where b.retired=false "
                + " and b.referenceNumber=:rn ";
        m.put("rn", refNumber);
        AgentHistory ahs = agentHistoryFacade.findFirstByJpql(sql, m);
        if (ahs == null) {
            return false;
        } else {
            return true;
        }
    }

    public void listnerChannelAgentSelect() {
        listnerAgentSelect(ReferenceBookEnum.ChannelBook);
    }

    private void listnerAgentSelect(ReferenceBookEnum bookEnum) {
        agentReferenceBooks = new ArrayList<>();
        String sql;
        Map m = new HashMap();

        sql = " select a from AgentReferenceBook a where "
                + " a.retired=false "
                + " and a.institution=:ins "
                + " and a.referenceBookEnum=:rb "
                + " order by a.bookNumber desc";

        m.put("rb", bookEnum);
        m.put("ins", getAgentReferenceBook().getInstitution());

        agentReferenceBooks = getAgentReferenceBookFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 10);
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
        if (agentReferenceBooks == null) {
            agentReferenceBooks = new ArrayList<>();
        }
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

    public Date getFrmDate() {
        if (frmDate == null) {
            frmDate = com.divudi.core.util.CommonFunctions.getStartOfMonth(new Date());
        }
        return frmDate;
    }

    public void setFrmDate(Date frmDate) {
        this.frmDate = frmDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfMonth(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public List<AgentReferenceBook> getSelectedList() {
        return selectedList;
    }

    public void setSelectedList(List<AgentReferenceBook> selectedList) {
        this.selectedList = selectedList;
    }

    public AgentHistoryFacade getAgentHistoryFacade() {
        return agentHistoryFacade;
    }

    public void setAgentHistoryFacade(AgentHistoryFacade agentHistoryFacade) {
        this.agentHistoryFacade = agentHistoryFacade;
    }

    public List<AgentReferenceBook> getAgentRefBookList() {
        return agentRefBookList;
    }

    public void setAgentRefBookList(List<AgentReferenceBook> agentRefBookList) {
        this.agentRefBookList = agentRefBookList;
    }

    public Institution getCollectingCentre() {
        return collectingCentre;
    }

    public void setCollectingCentre(Institution collectingCentre) {
        this.collectingCentre = collectingCentre;
    }

    public AgentReferenceBook getCurrent() {
        return current;
    }

    public void setCurrent(AgentReferenceBook current) {
        this.current = current;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isBookActive() {
        return bookActive;
    }

    public void setBookActive(boolean bookActive) {
        this.bookActive = bookActive;
    }

    public WebUserController getWebUserController() {
        return webUserController;
    }

    public void setWebUserController(WebUserController webUserController) {
        this.webUserController = webUserController;
    }

    public List<AuditEvent> getRefBookEditDetails() {
        return refBookEditDetails;
    }

    public void setRefBookEditDetails(List<AuditEvent> refBookEditDetails) {
        this.refBookEditDetails = refBookEditDetails;
    }

}
