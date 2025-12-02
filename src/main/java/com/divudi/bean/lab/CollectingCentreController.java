/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.lab;

import com.divudi.bean.common.AgentAndCcApplicationController;
import com.divudi.bean.common.InstitutionController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.CollectingCentrePaymentMethod;
import com.divudi.core.data.HistoryType;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.data.dto.AgentHistoryDTO;
import com.divudi.core.entity.AgentHistory;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.channel.AgentReferenceBook;
import com.divudi.core.facade.AgentHistoryFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.service.AgentHistoryService;
import com.divudi.service.AuditService;
import java.io.Serializable;
import java.util.ArrayList;
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
 * @author www.divudi.com
 */
@Named
@SessionScoped
public class CollectingCentreController implements Serializable {

    private boolean printPreview;

    /**
     * Creates a new instance of CollectingCentreController
     */
    public CollectingCentreController() {
        //////// // System.out.println("");
    }
    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    InstitutionController institutionController;
    @EJB
    private InstitutionFacade ejbFacade;
    @Inject
    AgentAndCcApplicationController collectingCentreApplicationController;

    @EJB
    AgentHistoryFacade agentHistoryFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    AgentHistoryService agentHistoryService;
    @EJB
    AuditService auditService;

    private int ccManagementIndex = 0;

    List<Institution> selectedItems;
    private Institution current;
    private List<Institution> items = null;
    String selectText = "";
    private List<AgentHistory> agentHistories;
    private Date fromDate;
    private Date toDate;
    private Institution collectingCentre;
    private Institution institution;
    private Bill bill;
    private List<Bill> bills;
    private Payment payment;
    private AgentHistory agentHistory;
    private Object auditDataBefore;
    private Object auditDataAfter;
    private int activeIndex;
    private String bookNumber;
    private List<AgentReferenceBook> ccBooks;

    public String navigateToPayToCollectingCentre() {
        bill = new Bill();
        collectingCentre = null;
        return "/collecting_centre/pay_collecting_centre?faces-redirect=true";
    }

    public void prepairColectingCentrePaymentDetails() {
        if (collectingCentre == null) {
            JsfUtil.addErrorMessage("Please select a Collecting Centre");
            return;
        }
        bill = new BilledBill();
        bill.setCollectingCentre(collectingCentre);
        bill.setFromInstitution(sessionController.getInstitution());
        bill.setToInstitution(collectingCentre);
        bill.setDepartment(sessionController.getDepartment());
        bill.setBillType(BillType.CollectingCentrePaymentMadeBill);
        bill.setBillTypeAtomic(BillTypeAtomic.CC_PAYMENT_RECEIVED_BILL);

    }

    public String navigateToEditNextCollectingCentreBalanceEntry(AgentHistory agentHx) {
        AgentHistory nahx = nextAgentHistory(agentHx);
        if (nahx == null) {
            JsfUtil.addErrorMessage("This is the Latest Record");
            return null;
        }
        return navigateToEditCollectingCentreBalanceEntry(nahx);
    }

    public String navigateToEditPreviousCollectingCentreBalanceEntry(AgentHistory agentHx) {
        AgentHistory nahx = previousAgentHistory(agentHx);
        return navigateToEditCollectingCentreBalanceEntry(nahx);
    }

    public void fixStartingBalanceFromLastEntry() {
        if (agentHistory == null) {
            return;
        }
        AgentHistory previousAgentHistory = previousAgentHistory(agentHistory);
        if (previousAgentHistory == null) {
            return;
        }
        agentHistory.setBalanceBeforeTransaction(previousAgentHistory.getBalanceAfterTransaction());
    }

    public void fixEndingBalance() {
        if (agentHistory == null) {
            return;
        }
        agentHistory.setBalanceAfterTransaction(agentHistory.getBalanceBeforeTransaction() + agentHistory.getTransactionValue());
    }

    public String navigateToCollectionCentreBookWiseDetail() {
        return "/reports/collectionCenterReports/collection_centre_book_wise_detail?faces-redirect=true";
    }

    public String navigateToEditCollectingCentreBalanceEntry(AgentHistory agentHx) {
        if (agentHx == null) {
            JsfUtil.addErrorMessage("No history selected");
            return "";
        }
        this.agentHistory = agentHx;
        // Clone the object to preserve the original state
        AgentHistoryDTO ahdto = new AgentHistoryDTO(agentHistory);
        auditDataBefore = ahdto;
        auditDataAfter = null;
        return "/collecting_centre/dev/edit_history_record?faces-redirect=true";
    }

    public void saveAgentHistory() {
        if (agentHistory == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        agentHistoryService.save(agentHistory, sessionController.getLoggedUser());
        AgentHistoryDTO ahdto = new AgentHistoryDTO(agentHistory);
        auditDataAfter = ahdto;

        auditService.logAudit(
                auditDataBefore,
                auditDataAfter,
                sessionController.getLoggedUser(),
                AgentHistoryDTO.class.getSimpleName(),
                "Update Agent History"
        );
        auditDataAfter = null;
        auditDataBefore = null;
    }

    public void processCollectingCentreBookWiseDetail() {
        //TO DO - Add Logic
        bills = new ArrayList<>();
    }

    public String saveAgentHistoryAndNavigateBackToCcStatement() {
        if (agentHistory == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        saveAgentHistory();
        return "/reports/collectionCenterReports/collection_center_statement_report?faces-redirect=true";
    }

    public String saveAgentHistoryAndNavigateToNextRecord() {
        if (agentHistory == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        saveAgentHistory();
        return navigateToEditNextCollectingCentreBalanceEntry(agentHistory);
    }

    public String fixAllRemainingRecords() {
        if (agentHistory == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }

        AgentHistory currentRecord = agentHistory;
        int recordsProcessed = 0;

        while (currentRecord != null) {
            // Set the current record for processing
            this.agentHistory = currentRecord;

            // Step 1: Fix starting balance from last record
            fixStartingBalanceFromLastEntry();

            // Step 2: Fix ending balance
            fixEndingBalance();

            // Step 3: Save the current record
            saveAgentHistory();

            recordsProcessed++;

            // Step 4: Get the next record
            AgentHistory nextRecord = nextAgentHistory(currentRecord);
            if (nextRecord == null) {
                // No more records to process
                break;
            }

            currentRecord = nextRecord;
        }

        JsfUtil.addSuccessMessage("Successfully processed " + recordsProcessed + " record(s)");
        return "/reports/collectionCenterReports/collection_center_statement_report?faces-redirect=true";
    }

    public AgentHistory nextAgentHistory(AgentHistory ahx) {
        if (ahx == null || ahx.getAgency() == null || ahx.getId() == null) {
            return null;
        }
        String jpql = "SELECT ah FROM AgentHistory ah "
                + "WHERE ah.retired = :ret "
                + "AND ah.agency = :agency "
                + "AND ah.bill.retired = :bret "
                + "AND ah.id > :thisid "
                + "ORDER BY ah.id ASC";

        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("bret", false);
        params.put("agency", ahx.getAgency());
        params.put("thisid", ahx.getId());

        return agentHistoryFacade.findFirstByJpql(jpql, params);
    }

    public AgentHistory previousAgentHistory(AgentHistory ahx) {
        if (ahx == null || ahx.getAgency() == null || ahx.getId() == null) {
            return null;
        }
        String jpql = "SELECT ah FROM AgentHistory ah "
                + "WHERE ah.retired = :ret "
                + "AND ah.bill.retired = :bret "
                + "AND ah.agency = :agency "
                + "AND ah.id < :thisid "
                + // '<' to get the previous record
                "ORDER BY ah.id DESC"; // Descending order to get the most recent previous one

        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("bret", false);
        params.put("agency", ahx.getAgency());
        params.put("thisid", ahx.getId());

        return agentHistoryFacade.findFirstByJpql(jpql, params);
    }

    public Double lastAgentBalance(Institution cc) {
        AgentHistory ah = lastAgentHistory(cc);
        if (ah == null) {
            return 0.0;
        }
        return ah.getBalanceAfterTransaction();
    }

    public AgentHistory lastAgentHistory(Institution cc) {
        if (cc == null || cc.getId() == null) {
            return null;
        }
        String jpql = "SELECT ah FROM AgentHistory ah "
                + "WHERE ah.retired = :ret "
                + "AND ah.agency = :agency "
                + "ORDER BY ah.id DESC";

        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("agency", cc);
        return agentHistoryFacade.findFirstByJpql(jpql, params);
    }

    public void settlePaymentBillToCollectingCentrePaymenMade() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (bill.getCollectingCentre() == null) {
            JsfUtil.addErrorMessage("Select a Collecting Center");
            return;
        }
        if (bill.getNetTotal() == 0.0) {
            JsfUtil.addErrorMessage("No value");
            return;
        }

        if (bill.getBillType() != BillType.CollectingCentrePaymentMadeBill) {
            JsfUtil.addErrorMessage("Wrong Bill Type");
            return;
        }

        bill.setGrantTotal(bill.getNetTotal());

        if (bill.getId() == null) {
            bill.setCreatedAt(new Date());
            bill.setCreater(sessionController.getLoggedUser());
            billFacade.create(bill);
        } else {
            billFacade.edit(bill);
        }

        //        Institution collectingCentre,
//            double hospitalFee,
//            double collectingCentreFee,
//            double staffFee,
//            double transactionValue,
//            HistoryType historyType,
//            Bill bill
//
        collectingCentreApplicationController.updateCcBalance(
                collectingCentre,
                0,
                0,
                0,
                bill.getNetTotal(),
                HistoryType.CollectingentrePaymentMadeBill,
                bill);

        printPreview = true;

    }

    @Deprecated // replace with #{institutionController.completeCollectingCenter}
    public List<Institution> completeCollecting(String query) {
        List<Institution> suggestions;
        String jpql;
        Map<String, Object> m = new HashMap<>();

        jpql = "select p "
                + "from Institution p "
                + "where p.retired = :ret "
                + "and p.institutionType = :insType "
                + "and upper(p.name) like :qry "
                + "order by p.name";

        m.put("ret", false);
        m.put("insType", InstitutionType.CollectingCentre);
        m.put("qry", "%" + query.toUpperCase() + "%");

        suggestions = getFacade().findByJpql(jpql, m);

        return suggestions;
    }

    public List<Institution> getSelectedItems() {
        if (selectText.trim().equals("")) {
            selectedItems = institutionController.completeInstitution(null, InstitutionType.CollectingCentre);
        } else {
            selectedItems = institutionController.completeInstitution(selectText, InstitutionType.CollectingCentre);
        }

//        selectedItems = getFacade().findByJpql("select c from Institution c where c.retired=false "
//                + "and i.institutionType = com.divudi.core.data.InstitutionType.CollectingCentre  "
//                + "and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        current = new Institution();
        current.setInstitutionType(InstitutionType.CollectingCentre);
    }

    public void setSelectedItems(List<Institution> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public void processCollectingCentreStatementReportNew() {

        String jpql = "select ah "
                + " from AgentHistory ah "
                + " where ah.retired=:ret"
                + " and ah.createdAt between :fd and :td "
                + " and ah.historyType <> :ht ";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("ht", HistoryType.CollectingCentreBalanceUpdateBill);

        if (collectingCentre != null) {
            jpql += " and ah.agency = :cc ";
            m.put("cc", collectingCentre);
        }

        if (institution != null) {
            jpql += " and ah.bill.institution = :ins ";
            m.put("ins", institution);
        }

        agentHistories = agentHistoryFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    public void generateAndAssignCode() {
        String jpql = "select c "
                + " from Institution c "
                + " where c.retired=:ret"
                + " and c.institutionType=:t "
                + " and c.code is not null "
                + " order by c.id desc";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("t", InstitutionType.CollectingCentre);
        Institution cc = getFacade().findFirstByJpql(jpql, m);
        if (cc == null) {
            return;
        }
        String previousCode = cc.getCode();
        getCurrent().setCode(newCode(previousCode));
    }

    public String newCode(String previousCode) {
        if (previousCode == null || previousCode.isEmpty()) {
            return "CC1"; // Default code if there's no previous code, adjust as needed
        }

        // Splitting the previous code into the string part and numeric part
        int i = previousCode.length() - 1;
        while (i >= 0 && Character.isDigit(previousCode.charAt(i))) {
            i--;
        }

        String prefix = previousCode.substring(0, i + 1);
        String numberPart = previousCode.substring(i + 1);

        // Parsing the numeric part to an integer and incrementing it
        int number;
        try {
            number = Integer.parseInt(numberPart);
        } catch (NumberFormatException e) {
            return prefix + "1"; // Default to 1 if parsing fails
        }
        number++;

        // Combining the incremented number with the prefix
        return prefix + number;
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

    public void save(Institution cc) {
        if (cc == null) {
            return;
        }
        if (cc.getId() != null) {
            getFacade().edit(cc);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            cc.setCreatedAt(new Date());
            cc.setCreater(getSessionController().getLoggedUser());
            getFacade().create(cc);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
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

    public Institution getCurrent() {
        if (current == null) {
            current = new Institution();
            current.setInstitutionType(InstitutionType.CollectingCentre);
        }
        return current;
    }

    public Institution findCollectingCentreByName(String name) {
        if (name == null) {
            return null;
        }
        if (name.trim().equals("")) {
            return null;
        }
        String jpql = "select c "
                + " from Institution c "
                + " where c.retired=:ret "
                + " and c.institutionType=:t "
                + " and c.name=:n";
        Map m = new HashMap<>();
        m.put("ret", false);
        m.put("t", InstitutionType.CollectingCentre);
        m.put("n", name);
        return getFacade().findFirstByJpql(jpql, m);
    }

    public Institution findCollectingCentreByCode(String code) {
        if (code == null) {
            return null;
        }
        if (code.trim().equals("")) {
            return null;
        }
        String jpql = "select c "
                + " from Institution c "
                + " where c.retired=:ret "
                + " and c.institutionType=:t "
                + " and c.code=:code";
        Map m = new HashMap<>();
        m.put("ret", false);
        m.put("t", InstitutionType.CollectingCentre);
        m.put("code", code);
        return getFacade().findFirstByJpql(jpql, m);
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
            String sql = "SELECT i FROM Institution i where i.retired=false and i.institutionType = com.divudi.core.data.InstitutionType.CollectingCentre order by i.name";
            items = getEjbFacade().findByJpql(sql);
        }
        return items;
    }

    public InstitutionController getInstitutionController() {
        return institutionController;
    }

    public void setInstitutionController(InstitutionController institutionController) {
        this.institutionController = institutionController;
    }

    public CollectingCentrePaymentMethod[] getCollectingCentrePaymentMethod() {
        return CollectingCentrePaymentMethod.values();
    }

    public List<AgentHistory> getAgentHistories() {
        return agentHistories;
    }

    public void setAgentHistories(List<AgentHistory> agentHistories) {
        this.agentHistories = agentHistories;
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

    public Institution getCollectingCentre() {
        return collectingCentre;
    }

    public void setCollectingCentre(Institution collectingCentre) {
        this.collectingCentre = collectingCentre;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public int getCcManagementIndex() {
        return ccManagementIndex;
    }

    public void setCcManagementIndex(int ccManagementIndex) {
        this.ccManagementIndex = ccManagementIndex;
    }

    public AgentHistory getAgentHistory() {
        return agentHistory;
    }

    public void setAgentHistory(AgentHistory agentHistory) {
        this.agentHistory = agentHistory;
    }

    public Object getAuditDataBefore() {
        return auditDataBefore;
    }

    public void setAuditDataBefore(Object auditDataBefore) {
        this.auditDataBefore = auditDataBefore;
    }

    public Object getAuditDataAfter() {
        return auditDataAfter;
    }

    public void setAuditDataAfter(Object auditDataAfter) {
        this.auditDataAfter = auditDataAfter;
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setActiveIndex(int activeIndex) {
        this.activeIndex = activeIndex;
    }

    public String getBookNumber() {
        return bookNumber;
    }

    public void setBookNumber(String bookNumber) {
        this.bookNumber = bookNumber;
    }

    public List<AgentReferenceBook> getCcBooks() {
        return ccBooks;
    }

    public void setCcBooks(List<AgentReferenceBook> ccBooks) {
        this.ccBooks = ccBooks;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

}
