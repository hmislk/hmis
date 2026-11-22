package com.divudi.bean.clientportal;

import com.divudi.core.data.dto.PatientComplaintPortalRowDto;
import com.divudi.core.entity.PatientComplaintCategory;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.PatientComplaintException;
import com.divudi.service.PatientComplaintService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Client portal: a logged-in patient submits a complaint and sees their own
 * complaints (#25886). The patient is always taken from the portal session on
 * the server; an id posted from the page is only ever a complaint id, and is
 * checked against that patient before anything is shown.
 */
@Named
@ViewScoped
public class ClientPortalComplaintController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ClientPortalSessionController clientPortalSessionController;
    @EJB
    private PatientComplaintService complaintService;

    private Long categoryId;
    private String description;
    private List<PatientComplaintPortalRowDto> complaints;
    private PatientComplaintPortalRowDto selected;
    /** Person of the portal session the cached complaints were read for. */
    private Long cachedForPersonId;

    /** Person of the portal account in the session, or null when not logged in. */
    private Long sessionPersonId() {
        if (!clientPortalSessionController.isLoggedIn()
                || clientPortalSessionController.getLoggedInAccount().getPerson() == null) {
            return null;
        }
        return clientPortalSessionController.getLoggedInAccount().getPerson().getId();
    }

    /**
     * Drops the cached list and the open detail when the session person is not
     * the one they were read for (another login on the same view, or logged out).
     * Returns the current session person id.
     */
    private Long syncCache() {
        Long personId = sessionPersonId();
        if (personId == null || !personId.equals(cachedForPersonId)) {
            complaints = null;
            selected = null;
            if (personId == null) {
                categoryId = null;
                description = null;
            }
            cachedForPersonId = personId;
        }
        return personId;
    }

    public List<PatientComplaintCategory> getCategories() {
        if (sessionPersonId() == null) {
            return new ArrayList<>();
        }
        return complaintService.fetchActiveCategories();
    }

    public List<PatientComplaintPortalRowDto> getComplaints() {
        if (syncCache() == null) {
            return new ArrayList<>();
        }
        if (complaints == null) {
            Long personId = sessionPersonId();
            complaints = personId == null ? new ArrayList<>() : complaintService.fetchPortalComplaints(personId);
        }
        return complaints;
    }

    public void submit() {
        Long personId = syncCache();
        if (personId == null) {
            JsfUtil.addErrorMessage("Please log in again.");
            return;
        }
        try {
            complaintService.submitFromPortal(personId, categoryId, description);
            JsfUtil.addSuccessMessage("Your complaint was received.");
            categoryId = null;
            description = null;
            selected = null;
        } catch (PatientComplaintException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        }
        complaints = null;
    }

    /** Shows one complaint; the id comes from the page, so ownership is re-checked here. */
    public void select(Long complaintId) {
        Long personId = syncCache();
        selected = null;
        if (personId == null) {
            JsfUtil.addErrorMessage("Please log in again.");
            return;
        }
        try {
            selected = complaintService.fetchPortalComplaint(personId, complaintId);
        } catch (PatientComplaintException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        }
    }

    public void closeDetail() {
        selected = null;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PatientComplaintPortalRowDto getSelected() {
        syncCache();
        return selected;
    }
}
