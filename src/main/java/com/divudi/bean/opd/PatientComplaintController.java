package com.divudi.bean.opd;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.PatientComplaintStatus;
import com.divudi.core.data.dto.PatientComplaintLogRowDto;
import com.divudi.core.data.dto.PatientComplaintRowDto;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientComplaint;
import com.divudi.core.entity.PatientComplaintCategory;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.PatientComplaintFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.PatientComplaintException;
import com.divudi.service.PatientComplaintService;
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
 * Staff side of the Patient Complaints register (#25886): the register, the
 * complaint page and the on-site entry page. Every action re-checks the
 * privilege on the server.
 */
@Named
@SessionScoped
public class PatientComplaintController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private PatientComplaintService complaintService;
    @EJB
    private PatientComplaintFacade complaintFacade;
    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;

    // Register filters
    private Date fromDate;
    private Date toDate;
    private PatientComplaintStatus filterStatus;
    private PatientComplaintCategory filterCategory;
    private WebUser filterAssignee;
    private List<PatientComplaintRowDto> rows = new ArrayList<>();

    // Complaint page
    private PatientComplaint complaint;
    /** Id of the complaint the page was rendered for; posted back with every action (the bean is shared by browser tabs). */
    private Long complaintIdOnPage;
    private List<PatientComplaintLogRowDto> logRows = new ArrayList<>();
    private WebUser assigneeToSet;
    private String response;
    private String statusNote;

    // On-site entry
    private Patient entryPatient;
    private String entryName;
    private String entryPhone;
    private PatientComplaintCategory entryCategory;
    private String entryDescription;

    public boolean isCanHandle() {
        return webUserController.hasPrivilege("PatientComplaintHandle");
    }

    public boolean isCanCreate() {
        return webUserController.hasPrivilege("PatientComplaintCreate");
    }

    private Long insId() {
        return sessionController.getInstitution() == null ? null : sessionController.getInstitution().getId();
    }

    /** Users who can be assigned complaints, for the assignee box. */
    public List<WebUser> completeHandlers(String query) {
        return isCanHandle() ? complaintService.completeHandlers(query) : new ArrayList<>();
    }

    private boolean denyHandle() {
        if (isCanHandle()) {
            return false;
        }
        JsfUtil.addErrorMessage("You do not have the privilege for this action.");
        return true;
    }

    /** True (with a message) when the posted page is not for the complaint now held in the session. */
    private boolean pageOutOfDate() {
        if (complaint == null || complaint.getId() == null || complaintIdOnPage == null
                || !complaint.getId().equals(complaintIdOnPage)) {
            JsfUtil.addErrorMessage("This page is out of date. Open the complaint again.");
            return true;
        }
        return false;
    }

    // ---- register -----------------------------------------------------------

    public String navigateToRegister() {
        if (denyHandle()) {
            return null;
        }
        fromDate = CommonFunctions.getStartOfMonth();
        toDate = CommonFunctions.getEndOfDay();
        filterStatus = null;
        filterCategory = null;
        filterAssignee = null;
        rows = new ArrayList<>();
        complaint = null;
        complaintIdOnPage = null;
        return "/opd/complaints/complaint_register?faces-redirect=true";
    }

    @SuppressWarnings("unchecked")
    public void searchComplaints() {
        if (!isCanHandle()) {
            rows = new ArrayList<>();
            JsfUtil.addErrorMessage("You do not have the privilege for this page.");
            return;
        }
        if (insId() == null) {
            rows = new ArrayList<>();
            JsfUtil.addErrorMessage("Your login has no institution.");
            return;
        }
        StringBuilder jpql = new StringBuilder("select new com.divudi.core.data.dto.PatientComplaintRowDto("
                + "c.id, c.createdAt, c.source, cat.name, coalesce(pp.name, c.complainantName), c.status,"
                + " ap.name, c.closedAt)"
                + " from PatientComplaint c left join c.category cat left join c.patient p left join p.person pp"
                + " left join c.assignee a left join a.webUserPerson ap"
                + " where c.retired=false and c.institution.id=:ins and c.createdAt between :fd and :td");
        Map<String, Object> m = new HashMap<>();
        m.put("ins", insId());
        m.put("fd", fromDate);
        m.put("td", toDate);
        if (filterStatus != null) {
            jpql.append(" and c.status=:st");
            m.put("st", filterStatus);
        }
        if (filterCategory != null) {
            jpql.append(" and c.category.id=:cat");
            m.put("cat", filterCategory.getId());
        }
        if (filterAssignee != null) {
            jpql.append(" and c.assignee.id=:asg");
            m.put("asg", filterAssignee.getId());
        }
        jpql.append(" order by c.id desc");
        rows = (List<PatientComplaintRowDto>) complaintFacade.findLightsByJpql(jpql.toString(), m, TemporalType.TIMESTAMP);
        if (rows == null) {
            rows = new ArrayList<>();
        }
    }

    // ---- complaint page -----------------------------------------------------

    public String viewComplaint(Long complaintId) {
        if (denyHandle()) {
            return null;
        }
        PatientComplaint c = complaintId == null ? null : complaintFacade.find(complaintId);
        if (c == null || c.isRetired()
                || !PatientComplaintService.isSameInstitution(c.getInstitution() == null ? null : c.getInstitution().getId(), insId())) {
            JsfUtil.addErrorMessage("Complaint not found.");
            return null;
        }
        openComplaint(c);
        return "/opd/complaints/complaint_view?faces-redirect=true";
    }

    public String backToRegister() {
        if (denyHandle()) {
            return null;
        }
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfMonth();
        }
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        searchComplaints();
        return "/opd/complaints/complaint_register?faces-redirect=true";
    }

    private void openComplaint(PatientComplaint c) {
        complaint = c;
        complaintIdOnPage = c.getId();
        assigneeToSet = c.getAssignee();
        response = c.getResponse();
        statusNote = null;
        logRows = complaintService.fetchLog(c.getId(), insId());
    }

    private void reload() {
        PatientComplaint c = complaintFacade.findWithoutCache(complaint.getId());
        openComplaint(c == null ? complaint : c);
    }

    public void assign() {
        if (denyHandle() || pageOutOfDate()) {
            return;
        }
        try {
            complaintService.assign(complaintIdOnPage, assigneeToSet == null ? null : assigneeToSet.getId(),
                    sessionController.getLoggedUser(), insId());
            JsfUtil.addSuccessMessage("Assignment saved.");
        } catch (PatientComplaintException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        }
        reload();
    }

    public void saveResponse() {
        if (denyHandle() || pageOutOfDate()) {
            return;
        }
        try {
            complaintService.saveResponse(complaintIdOnPage, response, sessionController.getLoggedUser(), insId());
            JsfUtil.addSuccessMessage("Response saved.");
        } catch (PatientComplaintException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        }
        reload();
    }

    private void changeStatus(PatientComplaintStatus next, String done) {
        if (denyHandle() || pageOutOfDate()) {
            return;
        }
        try {
            complaintService.changeStatus(complaintIdOnPage, next, response, statusNote,
                    sessionController.getLoggedUser(), insId());
            JsfUtil.addSuccessMessage(done);
        } catch (PatientComplaintException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        }
        reload();
    }

    public void startWork() {
        changeStatus(PatientComplaintStatus.IN_PROGRESS, "Complaint is now In Progress.");
    }

    public void closeComplaint() {
        changeStatus(PatientComplaintStatus.CLOSED, "Complaint closed.");
    }

    public void reopenComplaint() {
        changeStatus(PatientComplaintStatus.IN_PROGRESS, "Complaint reopened.");
    }

    public boolean isCanStart() {
        return complaint != null && complaint.getStatus() == PatientComplaintStatus.NEW;
    }

    public boolean isCanClose() {
        return complaint != null && complaint.getStatus() == PatientComplaintStatus.IN_PROGRESS;
    }

    public boolean isCanReopen() {
        return complaint != null && complaint.getStatus() == PatientComplaintStatus.CLOSED;
    }

    // ---- on-site entry ------------------------------------------------------

    public String navigateToEntry() {
        if (!isCanCreate()) {
            JsfUtil.addErrorMessage("You do not have the privilege for this action.");
            return null;
        }
        clearEntry();
        return "/opd/complaints/complaint_entry?faces-redirect=true";
    }

    private void clearEntry() {
        entryPatient = null;
        entryName = null;
        entryPhone = null;
        entryCategory = null;
        entryDescription = null;
    }

    public void saveEntry() {
        if (!isCanCreate()) {
            JsfUtil.addErrorMessage("You do not have the privilege for this action.");
            return;
        }
        try {
            PatientComplaint c = complaintService.createStaffComplaint(entryPatient, entryName, entryPhone,
                    entryCategory, entryDescription, sessionController.getInstitution(),
                    sessionController.getDepartment(), sessionController.getLoggedUser());
            JsfUtil.addSuccessMessage("Complaint " + c.getId() + " recorded.");
            clearEntry();
        } catch (PatientComplaintException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        }
    }

    // ---- lookups for the pages ---------------------------------------------

    public List<PatientComplaintStatus> getStatuses() {
        return java.util.Arrays.asList(PatientComplaintStatus.values());
    }

    public List<PatientComplaintCategory> getCategories() {
        return complaintService.fetchActiveCategories();
    }

    // ---- getters and setters ------------------------------------------------

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

    public PatientComplaintStatus getFilterStatus() {
        return filterStatus;
    }

    public void setFilterStatus(PatientComplaintStatus filterStatus) {
        this.filterStatus = filterStatus;
    }

    public PatientComplaintCategory getFilterCategory() {
        return filterCategory;
    }

    public void setFilterCategory(PatientComplaintCategory filterCategory) {
        this.filterCategory = filterCategory;
    }

    public WebUser getFilterAssignee() {
        return filterAssignee;
    }

    public void setFilterAssignee(WebUser filterAssignee) {
        this.filterAssignee = filterAssignee;
    }

    public List<PatientComplaintRowDto> getRows() {
        return rows;
    }

    public PatientComplaint getComplaint() {
        return complaint;
    }

    public Long getComplaintIdOnPage() {
        return complaintIdOnPage;
    }

    public void setComplaintIdOnPage(Long complaintIdOnPage) {
        this.complaintIdOnPage = complaintIdOnPage;
    }

    public List<PatientComplaintLogRowDto> getLogRows() {
        return logRows;
    }

    public WebUser getAssigneeToSet() {
        return assigneeToSet;
    }

    public void setAssigneeToSet(WebUser assigneeToSet) {
        this.assigneeToSet = assigneeToSet;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getStatusNote() {
        return statusNote;
    }

    public void setStatusNote(String statusNote) {
        this.statusNote = statusNote;
    }

    public Patient getEntryPatient() {
        return entryPatient;
    }

    public void setEntryPatient(Patient entryPatient) {
        this.entryPatient = entryPatient;
    }

    public String getEntryName() {
        return entryName;
    }

    public void setEntryName(String entryName) {
        this.entryName = entryName;
    }

    public String getEntryPhone() {
        return entryPhone;
    }

    public void setEntryPhone(String entryPhone) {
        this.entryPhone = entryPhone;
    }

    public PatientComplaintCategory getEntryCategory() {
        return entryCategory;
    }

    public void setEntryCategory(PatientComplaintCategory entryCategory) {
        this.entryCategory = entryCategory;
    }

    public String getEntryDescription() {
        return entryDescription;
    }

    public void setEntryDescription(String entryDescription) {
        this.entryDescription = entryDescription;
    }
}
