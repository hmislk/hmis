package com.divudi.bean.clinical;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.clinical.CoveringLetterRecordType;
import com.divudi.core.data.clinical.DocumentTemplateType;
import com.divudi.core.data.dto.CoveringLetterRowDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.clinical.CoveringLetter;
import com.divudi.core.entity.clinical.CoveringLetterItem;
import com.divudi.core.entity.clinical.DocumentTemplate;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.CoveringLetterFacade;
import com.divudi.core.facade.DocumentTemplateFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.CoveringLetterService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
 * Batch covering letters / invoices listing several admissions or OPD credit
 * bills.
 */
@Named("coveringLetterController")
@SessionScoped
public class CoveringLetterController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String BATCH_PAGE = "/covering_letters/covering_letter_batch?faces-redirect=true";
    private static final String LIST_PAGE = "/covering_letters/covering_letter_list?faces-redirect=true";
    private static final String PRINT_PAGE = "/covering_letters/covering_letter_print?faces-redirect=true";

    @EJB
    private CoveringLetterService coveringLetterService;
    @EJB
    private CoveringLetterFacade coveringLetterFacade;
    @EJB
    private DocumentTemplateFacade documentTemplateFacade;
    @Inject
    private SessionController sessionController;

    // batch
    private CoveringLetterRecordType recordType = CoveringLetterRecordType.Admission;
    private Institution issuingInstitution;
    private Institution creditCompany;
    private Patient patient;
    private Date fromDate;
    private Date toDate;
    private AdmissionType admissionType;
    private Department department;
    private boolean onlyUnsettled = false;
    private List<CoveringLetterRowDTO> rows = new ArrayList<>();
    private List<CoveringLetterRowDTO> selectedRows = new ArrayList<>();
    private List<DocumentTemplate> templates;
    private DocumentTemplate selectedTemplate;
    private CoveringLetter current;

    // list
    private Date searchFromDate;
    private Date searchToDate;
    private Institution searchCreditCompany;
    private String searchReferenceNo;
    private List<CoveringLetter> letters = new ArrayList<>();
    private CoveringLetter selectedLetter;
    private String cancelComment;

    // ------------------------------------------------------------- navigation
    public String navigateToCoveringLetterBatch() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        fromDate = c.getTime();
        toDate = new Date();
        recordType = CoveringLetterRecordType.Admission;
        onlyUnsettled = false;
        issuingInstitution = sessionController.getInstitution();
        creditCompany = null;
        patient = null;
        admissionType = null;
        department = null;
        rows = new ArrayList<>();
        selectedRows = new ArrayList<>();
        selectedTemplate = null;
        current = null;
        templates = null;
        getTemplates();
        return BATCH_PAGE;
    }

    public String navigateToCoveringLetterList() {
        Calendar c = Calendar.getInstance();
        searchToDate = c.getTime();
        c.add(Calendar.DAY_OF_MONTH, -30);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        searchFromDate = c.getTime();
        searchCreditCompany = null;
        searchReferenceNo = null;
        selectedLetter = null;
        cancelComment = null;
        searchLetters();
        return LIST_PAGE;
    }

    // ---------------------------------------------------------- batch actions
    public void fillRows() {
        selectedRows = new ArrayList<>();
        rows = new ArrayList<>();
        if (creditCompany == null) {
            JsfUtil.addErrorMessage("Select a credit company");
            return;
        }
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Select the date range");
            return;
        }
        rows = fetchRows();
        coveringLetterService.markAlreadyInLetter(rows);
        if (rows.isEmpty()) {
            JsfUtil.addErrorMessage("No records found");
        }
    }

    private List<CoveringLetterRowDTO> fetchRows() {
        if (recordType == CoveringLetterRecordType.OpdBill) {
            return coveringLetterService.fetchOpdBillRows(creditCompany, patient, fromDate, toDate, onlyUnsettled);
        }
        return coveringLetterService.fetchAdmissionRows(creditCompany, patient, fromDate, toDate,
                admissionType, department, onlyUnsettled);
    }

    /**
     * Re-runs the list query with the current filters and refreshes every
     * printed field of the selected rows from it, so a letter is never built
     * from values captured at Fill. Returns an error message for the first
     * selected row that no longer matches the filters (e.g. settled, cancelled,
     * unlinked from the credit company), or null when all rows still match.
     */
    private String refreshSelectedRows(List<CoveringLetterRowDTO> selected) {
        Map<Long, CoveringLetterRowDTO> fresh = new HashMap<>();
        for (CoveringLetterRowDTO f : fetchRows()) {
            fresh.put(f.getId(), f);
        }
        for (CoveringLetterRowDTO r : selected) {
            CoveringLetterRowDTO f = fresh.get(r.getId());
            if (f == null) {
                return r.getRefNo() + " no longer matches the filters (it may have been settled, cancelled or "
                        + "changed). Press Fill again.";
            }
            r.setRefNo(f.getRefNo());
            r.setDate(f.getDate());
            r.setPatientName(f.getPatientName());
            r.setPolicyNo(f.getPolicyNo());
            r.setAmount(f.getAmount());
            r.setCompanyAmount(f.getCompanyAmount());
        }
        return null;
    }

    public void recordTypeChanged() {
        rows = new ArrayList<>();
        selectedRows = new ArrayList<>();
    }

    public String generateLetter() {
        if (selectedTemplate == null) {
            JsfUtil.addErrorMessage("Select a letter template");
            return "";
        }
        if (selectedRows == null || selectedRows.isEmpty()) {
            JsfUtil.addErrorMessage("Select at least one record");
            return "";
        }
        if (issuingInstitution == null) {
            JsfUtil.addErrorMessage("Select the issuing institution");
            return "";
        }
        String fmt = sessionController.getApplicationPreference() != null
                ? sessionController.getApplicationPreference().getLongDateFormat() : null;
        // List the rows in table order, not in the order the user ticked them.
        List<CoveringLetterRowDTO> orderedRows = new ArrayList<>();
        for (CoveringLetterRowDTO r : rows) {
            if (selectedRows.contains(r)) {
                orderedRows.add(r);
            }
        }
        if (orderedRows.isEmpty()) {
            orderedRows = selectedRows;
        }
        String invalid = refreshSelectedRows(orderedRows);
        if (invalid != null) {
            JsfUtil.addErrorMessage(invalid);
            return "";
        }
        CoveringLetter l = coveringLetterService.createLetter(selectedTemplate, orderedRows, recordType,
                issuingInstitution, creditCompany, sessionController.getLoggedUser(), fmt);
        if (l == null) {
            JsfUtil.addErrorMessage("Letter could not be generated");
            return "";
        }
        current = l;
        return PRINT_PAGE;
    }

    public double getSelectedTotal() {
        double t = 0;
        if (selectedRows != null) {
            for (CoveringLetterRowDTO r : selectedRows) {
                if (r != null && r.getAmount() != null) {
                    t += r.getAmount();
                }
            }
        }
        return t;
    }

    public String navigateBackFromPrint() {
        if (creditCompany != null && fromDate != null && toDate != null) {
            fillRows();
        }
        return BATCH_PAGE;
    }

    // ------------------------------------------------------------ list actions
    public void searchLetters() {
        Map<String, Object> m = new HashMap<>();
        StringBuilder j = new StringBuilder("select l from CoveringLetter l where l.retired=false ");
        if (searchFromDate != null && searchToDate != null) {
            j.append("and l.createdAt between :fd and :td ");
            m.put("fd", searchFromDate);
            m.put("td", searchToDate);
        }
        if (searchCreditCompany != null) {
            j.append("and l.creditCompany=:cc ");
            m.put("cc", searchCreditCompany);
        }
        if (searchReferenceNo != null && !searchReferenceNo.trim().isEmpty()) {
            j.append("and upper(l.referenceNo) like :rn ");
            m.put("rn", "%" + searchReferenceNo.trim().toUpperCase() + "%");
        }
        j.append("order by l.id desc");
        letters = coveringLetterFacade.findByJpql(j.toString(), m, TemporalType.TIMESTAMP);
        if (letters == null) {
            letters = new ArrayList<>();
        }
    }

    public String reprint(CoveringLetter l) {
        if (l == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        current = l;
        return PRINT_PAGE;
    }

    public void cancelSelectedLetter() {
        if (selectedLetter == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        if (cancelComment == null || cancelComment.trim().isEmpty()) {
            JsfUtil.addErrorMessage("A comment is required to cancel a letter");
            return;
        }
        Date now = new Date();
        selectedLetter.setRetired(true);
        selectedLetter.setRetirer(sessionController.getLoggedUser());
        selectedLetter.setRetiredAt(now);
        selectedLetter.setRetireComments(cancelComment.trim());
        for (CoveringLetterItem i : selectedLetter.getItems()) {
            i.setRetired(true);
        }
        coveringLetterFacade.edit(selectedLetter);
        JsfUtil.addSuccessMessage("Letter " + selectedLetter.getReferenceNo() + " cancelled");
        selectedLetter = null;
        cancelComment = null;
        searchLetters();
    }

    // -------------------------------------------------------------- accessors
    public List<CoveringLetterRecordType> getRecordTypes() {
        return Arrays.asList(CoveringLetterRecordType.values());
    }

    @SuppressWarnings("unchecked")
    public List<DocumentTemplate> getTemplates() {
        if (templates == null) {
            Map<String, Object> m = new HashMap<>();
            m.put("ret", false);
            m.put("type", DocumentTemplateType.CoveringLetter);
            templates = documentTemplateFacade.findByJpql("select c from DocumentTemplate c "
                    + "where c.retired=:ret and c.type=:type order by c.name", m);
            if (templates == null) {
                templates = new ArrayList<>();
            }
        }
        return templates;
    }

    public CoveringLetterRecordType getRecordType() {
        return recordType;
    }

    public void setRecordType(CoveringLetterRecordType recordType) {
        this.recordType = recordType;
    }

    public Institution getIssuingInstitution() {
        return issuingInstitution;
    }

    public void setIssuingInstitution(Institution issuingInstitution) {
        this.issuingInstitution = issuingInstitution;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
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

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public boolean isOnlyUnsettled() {
        return onlyUnsettled;
    }

    public void setOnlyUnsettled(boolean onlyUnsettled) {
        this.onlyUnsettled = onlyUnsettled;
    }

    public List<CoveringLetterRowDTO> getRows() {
        return rows;
    }

    public void setRows(List<CoveringLetterRowDTO> rows) {
        this.rows = rows;
    }

    public List<CoveringLetterRowDTO> getSelectedRows() {
        if (selectedRows == null) {
            selectedRows = new ArrayList<>();
        }
        return selectedRows;
    }

    public void setSelectedRows(List<CoveringLetterRowDTO> selectedRows) {
        this.selectedRows = selectedRows;
    }

    public DocumentTemplate getSelectedTemplate() {
        return selectedTemplate;
    }

    public void setSelectedTemplate(DocumentTemplate selectedTemplate) {
        this.selectedTemplate = selectedTemplate;
    }

    public CoveringLetter getCurrent() {
        return current;
    }

    public void setCurrent(CoveringLetter current) {
        this.current = current;
    }

    public Date getSearchFromDate() {
        return searchFromDate;
    }

    public void setSearchFromDate(Date searchFromDate) {
        this.searchFromDate = searchFromDate;
    }

    public Date getSearchToDate() {
        return searchToDate;
    }

    public void setSearchToDate(Date searchToDate) {
        this.searchToDate = searchToDate;
    }

    public Institution getSearchCreditCompany() {
        return searchCreditCompany;
    }

    public void setSearchCreditCompany(Institution searchCreditCompany) {
        this.searchCreditCompany = searchCreditCompany;
    }

    public String getSearchReferenceNo() {
        return searchReferenceNo;
    }

    public void setSearchReferenceNo(String searchReferenceNo) {
        this.searchReferenceNo = searchReferenceNo;
    }

    public List<CoveringLetter> getLetters() {
        return letters;
    }

    public void setLetters(List<CoveringLetter> letters) {
        this.letters = letters;
    }

    public CoveringLetter getSelectedLetter() {
        return selectedLetter;
    }

    public void setSelectedLetter(CoveringLetter selectedLetter) {
        this.selectedLetter = selectedLetter;
    }

    public String getCancelComment() {
        return cancelComment;
    }

    public void setCancelComment(String cancelComment) {
        this.cancelComment = cancelComment;
    }
}
