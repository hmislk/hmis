package com.divudi.service;

import com.divudi.core.data.PatientComplaintAction;
import com.divudi.core.data.PatientComplaintSource;
import com.divudi.core.data.PatientComplaintStatus;
import com.divudi.core.data.dto.PatientComplaintLogRowDto;
import com.divudi.core.data.dto.PatientComplaintPortalRowDto;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientComplaint;
import com.divudi.core.entity.PatientComplaintCategory;
import com.divudi.core.entity.PatientComplaintLog;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.PatientComplaintCategoryFacade;
import com.divudi.core.facade.PatientComplaintFacade;
import com.divudi.core.facade.PatientComplaintLogFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.WebUserFacade;
import com.divudi.core.facade.WebUserPrivilegeFacade;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 * Patient complaints register (#25886): staff entry, portal submission,
 * assignment, status changes and the log. Business refusals are thrown as
 * {@link PatientComplaintException}. Privilege checks are made by the callers
 * (the staff controller); the portal methods take the portal account's person
 * id from the server-side session and never trust an id from the request.
 */
@Stateless
public class PatientComplaintService {

    /** An identical online complaint from the same patient inside this window is treated as a double submit. */
    public static final int DUPLICATE_WINDOW_MINUTES = 10;
    public static final int MAX_TEXT_LENGTH = 4000;
    public static final int MAX_NOTE_LENGTH = 2000;

    @EJB
    private PatientComplaintFacade complaintFacade;
    @EJB
    private PatientComplaintLogFacade logFacade;
    @EJB
    private PatientComplaintCategoryFacade categoryFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private WebUserFacade webUserFacade;
    @EJB
    private WebUserPrivilegeFacade webUserPrivilegeFacade;
    @EJB
    private InstitutionFacade institutionFacade;

    /** Privilege a user needs to be given complaints; same name the staff pages check. */
    public static final String HANDLE_PRIVILEGE = "PatientComplaintHandle";

    // ---- pure rules -------------------------------------------------------

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Trims; null for blank. */
    public static String clean(String s) {
        return blank(s) ? null : s.trim();
    }

    /**
     * Refuses a status change that is not allowed: wrong step, Closed without a
     * response, or a reopen without a note.
     */
    public static void checkStatusChange(PatientComplaintStatus from, PatientComplaintStatus to,
            String response, String note) {
        if (from == null) {
            throw new PatientComplaintException("Complaint not found.");
        }
        if (to == null) {
            throw new PatientComplaintException("Select the new status.");
        }
        if (!from.canMoveTo(to)) {
            throw new PatientComplaintException("A complaint that is " + from.getLabel()
                    + " cannot be changed to " + to.getLabel() + ".");
        }
        if (to == PatientComplaintStatus.CLOSED && blank(response)) {
            throw new PatientComplaintException("Enter the response to the patient before closing the complaint.");
        }
        if (from == PatientComplaintStatus.CLOSED && blank(note)) {
            throw new PatientComplaintException("Enter a note saying why the complaint is reopened.");
        }
    }

    /** True only when the complaint belongs to one of the patients bound to the portal session. */
    public static boolean isOwnedByAny(Long complaintPatientId, Collection<Long> sessionPatientIds) {
        return complaintPatientId != null && sessionPatientIds != null && sessionPatientIds.contains(complaintPatientId);
    }

    /** A staff user reaches only the complaints of their own institution. */
    public static boolean isSameInstitution(Long complaintInstitutionId, Long userInstitutionId) {
        return complaintInstitutionId != null && complaintInstitutionId.equals(userInstitutionId);
    }

    /** Refuses a complaint of another institution with the same message as a missing one. */
    public static void checkInstitution(Long complaintInstitutionId, Long userInstitutionId) {
        if (!isSameInstitution(complaintInstitutionId, userInstitutionId)) {
            throw new PatientComplaintException("Complaint not found.");
        }
    }

    /** A response cannot be changed on a Closed complaint; the patient may already have read it. */
    public static void checkResponseEditable(PatientComplaintStatus status) {
        if (status == PatientComplaintStatus.CLOSED) {
            throw new PatientComplaintException("Reopen the complaint to change the response.");
        }
    }

    private static void checkLength(String s, int max, String what) {
        if (s != null && s.length() > max) {
            throw new PatientComplaintException("The " + what + " is too long (" + max + " characters at most).");
        }
    }

    // ---- staff ------------------------------------------------------------

    private void log(PatientComplaint c, PatientComplaintAction action, PatientComplaintStatus from,
            PatientComplaintStatus to, String note, WebUser user, Patient patient) {
        PatientComplaintLog l = new PatientComplaintLog();
        l.setComplaint(c);
        l.setAction(action);
        l.setFromStatus(from);
        l.setToStatus(to);
        l.setNote(note);
        l.setCreatedAt(new Date());
        l.setCreater(user);
        l.setPatient(patient);
        logFacade.create(l);
    }

    private PatientComplaint lockOpen(Long complaintId, Long userInstitutionId) {
        PatientComplaint c = complaintId == null ? null : complaintFacade.findWithLock(complaintId);
        if (c == null || c.isRetired()) {
            throw new PatientComplaintException("Complaint not found.");
        }
        checkInstitution(c.getInstitution() == null ? null : c.getInstitution().getId(), userInstitutionId);
        return c;
    }

    /** True when the user holds the handle privilege (any department, not retired). */
    public boolean canHandle(Long webUserId) {
        if (webUserId == null) {
            return false;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("uid", webUserId);
        m.put("pr", com.divudi.core.data.Privileges.valueOf(HANDLE_PRIVILEGE));
        return webUserPrivilegeFacade.findLongByJpql(
                "select count(w) from WebUserPrivilege w where w.retired=false and w.webUser.id=:uid and w.privilege=:pr", m) > 0;
    }

    /** Users who can be given complaints, matching the typed text on name or login name. */
    public List<WebUser> completeHandlers(String query) {
        if (query == null) {
            return new ArrayList<>();
        }
        Map<String, Object> m = new HashMap<>();
        m.put("q", "%" + query.trim().toUpperCase() + "%");
        m.put("pr", com.divudi.core.data.Privileges.valueOf(HANDLE_PRIVILEGE));
        List<WebUser> l = webUserFacade.findByJpql(
                "select distinct u from WebUser u where u.retired=false"
                + " and (upper(u.webUserPerson.name) like :q or upper(u.name) like :q)"
                + " and exists (select w from WebUserPrivilege w where w.retired=false and w.webUser=u and w.privilege=:pr)"
                + " order by u.webUserPerson.name", m, 20);
        return l == null ? new ArrayList<>() : l;
    }

    private PatientComplaintCategory requireCategory(PatientComplaintCategory category) {
        if (category == null || category.getId() == null) {
            throw new PatientComplaintException("Select the complaint category.");
        }
        Map<String, Object> m = new HashMap<>();
        m.put("id", category.getId());
        PatientComplaintCategory found = categoryFacade.findFirstByJpql(
                "select c from PatientComplaintCategory c where c.retired=false and c.id=:id", m);
        if (found == null) {
            throw new PatientComplaintException("Select the complaint category.");
        }
        return found;
    }

    /** A complaint typed in by staff on site. The patient may be null for someone who is not registered. */
    public PatientComplaint createStaffComplaint(Patient patient, String complainantName, String complainantPhone,
            PatientComplaintCategory category, String description, Institution institution, Department department,
            WebUser user) {
        if (user == null) {
            throw new PatientComplaintException("You must be logged in to record a complaint.");
        }
        if (institution == null) {
            throw new PatientComplaintException("Your login has no institution, so the complaint cannot be filed.");
        }
        String desc = clean(description);
        if (desc == null) {
            throw new PatientComplaintException("Enter what the complaint is about.");
        }
        checkLength(desc, MAX_TEXT_LENGTH, "complaint text");
        String name = clean(complainantName);
        if (patient == null && name == null) {
            throw new PatientComplaintException("Select the patient, or enter the name of the person complaining.");
        }
        PatientComplaintCategory cat = requireCategory(category);
        Date now = new Date();
        PatientComplaint c = new PatientComplaint();
        if (patient != null) {
            c.setPatient(patient);
        } else {
            c.setComplainantName(name);
            c.setComplainantPhone(clean(complainantPhone));
        }
        c.setSource(PatientComplaintSource.STAFF);
        c.setCategory(cat);
        c.setDescription(desc);
        c.setStatus(PatientComplaintStatus.NEW);
        c.setInstitution(institution);
        c.setDepartment(department);
        c.setCreater(user);
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        complaintFacade.create(c);
        log(c, PatientComplaintAction.CREATED, null, PatientComplaintStatus.NEW, "Entered on site", user, patient);
        return c;
    }

    public PatientComplaint assign(Long complaintId, Long assigneeId, WebUser user, Long userInstitutionId) {
        PatientComplaint c = lockOpen(complaintId, userInstitutionId);
        if (c.getStatus() == PatientComplaintStatus.CLOSED) {
            throw new PatientComplaintException("A closed complaint cannot be assigned. Reopen it first.");
        }
        WebUser assignee = null;
        if (assigneeId != null) {
            assignee = webUserFacade.find(assigneeId);
            if (assignee == null || assignee.isRetired()) {
                throw new PatientComplaintException("Select a valid user to assign the complaint to.");
            }
            if (!canHandle(assignee.getId())) {
                throw new PatientComplaintException("That user cannot handle complaints.");
            }
        }
        c.setAssignee(assignee);
        c.setUpdatedAt(new Date());
        complaintFacade.edit(c);
        String who = assignee == null ? "Unassigned"
                : "Assigned to " + (assignee.getWebUserPerson() != null && assignee.getWebUserPerson().getName() != null
                        ? assignee.getWebUserPerson().getName() : assignee.getName());
        log(c, PatientComplaintAction.ASSIGNED, c.getStatus(), c.getStatus(), who, user, null);
        return c;
    }

    public PatientComplaint saveResponse(Long complaintId, String response, WebUser user, Long userInstitutionId) {
        PatientComplaint c = lockOpen(complaintId, userInstitutionId);
        checkResponseEditable(c.getStatus());
        String r = clean(response);
        if (r == null) {
            throw new PatientComplaintException("Enter the response.");
        }
        checkLength(r, MAX_TEXT_LENGTH, "response");
        c.setResponse(r);
        c.setUpdatedAt(new Date());
        complaintFacade.edit(c);
        log(c, PatientComplaintAction.RESPONSE_SAVED, c.getStatus(), c.getStatus(), null, user, null);
        return c;
    }

    /**
     * New to In Progress, In Progress to Closed (needs a response), Closed back
     * to In Progress (needs a note). Re-reads and locks the complaint, so two
     * overlapping requests cannot both pass the check.
     */
    public PatientComplaint changeStatus(Long complaintId, PatientComplaintStatus next, String response,
            String note, WebUser user, Long userInstitutionId) {
        PatientComplaint c = lockOpen(complaintId, userInstitutionId);
        PatientComplaintStatus from = c.getStatus();
        String r = clean(response);
        String n = clean(note);
        checkStatusChange(from, next, r, n);
        checkLength(r, MAX_TEXT_LENGTH, "response");
        checkLength(n, MAX_NOTE_LENGTH, "note");
        Date now = new Date();
        c.setStatus(next);
        c.setUpdatedAt(now);
        if (next == PatientComplaintStatus.CLOSED) {
            c.setResponse(r);
            c.setClosedAt(now);
            c.setClosedBy(user);
        } else if (from == PatientComplaintStatus.CLOSED) {
            c.setClosedAt(null);
            c.setClosedBy(null);
        }
        complaintFacade.edit(c);
        log(c, PatientComplaintAction.STATUS_CHANGED, from, next, n, user, null);
        return c;
    }

    @SuppressWarnings("unchecked")
    public List<PatientComplaintLogRowDto> fetchLog(Long complaintId, Long userInstitutionId) {
        Map<String, Object> m = new HashMap<>();
        m.put("cid", complaintId);
        m.put("ins", userInstitutionId);
        return (List<PatientComplaintLogRowDto>) logFacade.findLightsByJpql(
                "select new com.divudi.core.data.dto.PatientComplaintLogRowDto("
                + "l.createdAt, l.action, l.fromStatus, l.toStatus, l.note, p.name)"
                + " from PatientComplaintLog l left join l.creater u left join u.webUserPerson p"
                + " where l.complaint.id=:cid and l.complaint.institution.id=:ins order by l.id", m);
    }

    // ---- client portal ----------------------------------------------------

    /** Ids of the patient records bound to the portal account's person, read on the server. */
    public List<Long> fetchPatientIdsForPerson(Long personId) {
        List<Long> out = new ArrayList<>();
        if (personId == null) {
            return out;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("pid", personId);
        List<?> found = patientFacade.findLightsByJpqlStrict(
                "select p.id from Patient p where p.retired=false and p.person.id=:pid order by p.id", m);
        for (Object o : found) {
            out.add((Long) o);
        }
        return out;
    }

    /**
     * A logged-in portal patient submits a complaint. The person id comes from
     * the portal session, never from the request. A repeat of the same text
     * inside the duplicate window returns the complaint already saved.
     */
    public PatientComplaint submitFromPortal(Long sessionPersonId, Long categoryId, String description) {
        List<Long> patientIds = fetchPatientIdsForPerson(sessionPersonId);
        if (patientIds.isEmpty()) {
            throw new PatientComplaintException("Your account is not linked to a patient record.");
        }
        String desc = clean(description);
        if (desc == null) {
            throw new PatientComplaintException("Enter what your complaint is about.");
        }
        checkLength(desc, MAX_TEXT_LENGTH, "complaint text");
        PatientComplaintCategory category = new PatientComplaintCategory();
        category.setId(categoryId);
        PatientComplaintCategory cat = requireCategory(category);
        // The lock is the first write-path read: two overlapping submits of one patient run one after the other.
        Patient patient = patientFacade.findWithLock(patientIds.get(0));
        if (patient == null) {
            throw new PatientComplaintException("Your account is not linked to a patient record.");
        }
        Date now = new Date();
        Map<String, Object> m = new HashMap<>();
        m.put("pid", patient.getId());
        m.put("cid", cat.getId());
        m.put("since", new Date(now.getTime() - DUPLICATE_WINDOW_MINUTES * 60L * 1000L));
        m.put("src", PatientComplaintSource.PORTAL);
        List<PatientComplaint> recent = complaintFacade.findByJpql(
                "select c from PatientComplaint c where c.retired=false and c.patient.id=:pid and c.category.id=:cid"
                + " and c.source=:src and c.createdAt>=:since order by c.id", m, TemporalType.TIMESTAMP);
        if (recent != null) {
            for (PatientComplaint r : recent) {
                if (desc.equals(clean(r.getDescription()))) {
                    return r;
                }
            }
        }
        PatientComplaint c = new PatientComplaint();
        c.setPatient(patient);
        // The portal has no department context; file under the default institution so staff of it see it.
        c.setInstitution(institutionFacade.findDefaultInstitution());
        c.setSource(PatientComplaintSource.PORTAL);
        c.setCategory(cat);
        c.setDescription(desc);
        c.setStatus(PatientComplaintStatus.NEW);
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        complaintFacade.create(c);
        log(c, PatientComplaintAction.CREATED, null, PatientComplaintStatus.NEW, "Submitted online", null, patient);
        return c;
    }

    private static final String PORTAL_SELECT = "select new com.divudi.core.data.dto.PatientComplaintPortalRowDto("
            + "c.id, cat.name, c.description, c.status, c.createdAt, c.closedAt, c.response)"
            + " from PatientComplaint c left join c.category cat where c.retired=false and c.patient.id in :pids";

    /** The portal patient's own complaints, newest first. Filters on the session's patients on the server. */
    @SuppressWarnings("unchecked")
    public List<PatientComplaintPortalRowDto> fetchPortalComplaints(Long sessionPersonId) {
        List<Long> patientIds = fetchPatientIdsForPerson(sessionPersonId);
        if (patientIds.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, Object> m = new HashMap<>();
        m.put("pids", patientIds);
        return (List<PatientComplaintPortalRowDto>) complaintFacade.findLightsByJpqlStrict(
                PORTAL_SELECT + " order by c.id desc", m);
    }

    /**
     * One complaint for the portal patient. Re-checks ownership against the
     * session's patients; refuses with the same message whether the complaint
     * is missing or belongs to someone else.
     */
    @SuppressWarnings("unchecked")
    public PatientComplaintPortalRowDto fetchPortalComplaint(Long sessionPersonId, Long complaintId) {
        List<Long> patientIds = fetchPatientIdsForPerson(sessionPersonId);
        if (complaintId == null || patientIds.isEmpty()) {
            throw new PatientComplaintException("Complaint not found.");
        }
        Map<String, Object> m = new HashMap<>();
        m.put("pids", patientIds);
        m.put("id", complaintId);
        List<PatientComplaintPortalRowDto> rows = (List<PatientComplaintPortalRowDto>) complaintFacade.findLightsByJpqlStrict(
                PORTAL_SELECT + " and c.id=:id", m);
        if (rows == null || rows.isEmpty()) {
            throw new PatientComplaintException("Complaint not found.");
        }
        return rows.get(0);
    }

    /** Active categories, for the staff entry form and the portal form. */
    public List<PatientComplaintCategory> fetchActiveCategories() {
        List<PatientComplaintCategory> l = categoryFacade.findByJpql(
                "select c from PatientComplaintCategory c where c.retired=false order by c.name");
        return l == null ? new ArrayList<>() : l;
    }
}
