/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.data.inward.BedStatus;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.PatientRoomFacade;
import com.divudi.core.facade.RoomFacilityChargeFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Backing bean for the graphical SVG bed board (inward_bed_board.xhtml).
 *
 * Navigation model:
 *   - Starts at the Site (Institution with institutionType=Site) for the
 *     logged-in institution.
 *   - Each level shows: the current entity's svgParentView as the background,
 *     with each child entity's svgChildView overlaid inside it.
 *   - When no SVG is stored, a fallback HTML grid is rendered instead.
 *   - Clicking a child drills into it; the Up button navigates back.
 */
@Named
@SessionScoped
public class BedBoardController implements Serializable {

    @Inject
    SessionController sessionController;
    @Inject
    BhtSummeryController bhtSummeryController;

    @EJB
    DepartmentFacade departmentFacade;
    @EJB
    InstitutionFacade institutionFacade;
    @EJB
    RoomFacilityChargeFacade roomFacilityChargeFacade;
    @EJB
    PatientRoomFacade patientRoomFacade;

    // Navigation state
    private Institution currentSite;
    private Department currentDepartment; // null = at site level
    private List<Department> breadcrumb = new ArrayList<>();

    // Current level data
    private List<Department> childDepartments = new ArrayList<>();
    private List<RoomFacilityCharge> bedBoardRooms = new ArrayList<>();
    private Map<Long, BedStatus> liveBedStatusMap = new HashMap<>();

    // -------------------------------------------------------------------------
    // Initialisation — called via f:viewAction on page load
    // -------------------------------------------------------------------------

    public void initBedBoard() {
        breadcrumb = new ArrayList<>();
        currentDepartment = null;
        currentSite = findSite();
        loadCurrentLevel();
    }

    private Institution findSite() {
        // Every lookup is scoped to the logged-in institution. A Site is reached
        // through the departments owned by that institution (d.institution=:ins)
        // whose d.site points at the Site — Sites themselves are not directly
        // linked to the company, so we must go via departments.
        Institution ins = sessionController.getInstitution();
        if (ins == null) {
            return null;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("type", InstitutionType.Site);
        params.put("ins", ins);

        // Prefer a Site this institution can reach that has actually been
        // configured for the bed board — i.e. it has a stored floor-plan SVG.
        String configuredJpql = "SELECT DISTINCT d.site FROM Department d "
                + "WHERE d.retired=false "
                + "AND d.inactive=false "
                + "AND d.institution=:ins "
                + "AND d.site IS NOT NULL "
                + "AND d.site.retired=false "
                + "AND d.site.institutionType=:type "
                + "AND d.site.svgParentView IS NOT NULL "
                + "ORDER BY d.site.name";
        List<Institution> configured = institutionFacade.findByJpql(configuredJpql, params);
        if (configured != null && !configured.isEmpty()) {
            return configured.get(0);
        }

        // Otherwise, prefer a Site this institution can reach that has buildings
        // (top-level departments).
        String jpql = "SELECT DISTINCT d.site FROM Department d "
                + "WHERE d.retired=false "
                + "AND d.inactive=false "
                + "AND d.institution=:ins "
                + "AND d.superDepartment IS NULL "
                + "AND d.site IS NOT NULL "
                + "AND d.site.retired=false "
                + "AND d.site.institutionType=:type "
                + "ORDER BY d.site.name";
        List<Institution> sites = institutionFacade.findByJpql(jpql, params);
        if (sites != null && !sites.isEmpty()) {
            return sites.get(0);
        }

        // Fall back to any Site institution belonging to the logged-in institution.
        String fallbackJpql = "SELECT i FROM Institution i "
                + "WHERE i.retired=false "
                + "AND i.institutionType=:type "
                + "AND i.institution=:ins "
                + "ORDER BY i.name";
        List<Institution> fallback = institutionFacade.findByJpql(fallbackJpql, params);
        if (fallback != null && !fallback.isEmpty()) {
            return fallback.get(0);
        }
        return ins;
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    public void drillInto(Department dept) {
        if (dept == null) {
            return;
        }
        breadcrumb.add(currentDepartment);
        currentDepartment = dept;
        loadCurrentLevel();
    }

    public void goUp() {
        if (breadcrumb.isEmpty()) {
            return;
        }
        currentDepartment = breadcrumb.remove(breadcrumb.size() - 1);
        loadCurrentLevel();
    }

    public void refresh() {
        loadCurrentLevel();
    }

    /**
     * Open the inpatient profile of the patient currently in this bed.
     * Resolves the active (undischarged) PatientRoom for the given
     * RoomFacilityCharge and sets it on BhtSummeryController before navigating,
     * so the profile shows the bed's actual occupant (not a stale selection).
     */
    public String openBed(RoomFacilityCharge rfc) {
        if (rfc == null) {
            JsfUtil.addErrorMessage("No bed selected");
            return "";
        }
        Map<String, Object> params = new HashMap<>();
        params.put("rfc", rfc);
        List<PatientRoom> rooms = patientRoomFacade.findByJpql(
                "SELECT pr FROM PatientRoom pr "
                + "WHERE pr.retired=false AND pr.discharged=false "
                + "AND pr.roomFacilityCharge=:rfc "
                + "ORDER BY pr.id DESC",
                params);
        if (rooms == null || rooms.isEmpty()
                || rooms.get(0).getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("No patient is currently admitted in this bed");
            return "";
        }
        bhtSummeryController.setPatientEncounter(rooms.get(0).getPatientEncounter());
        return bhtSummeryController.navigateToInpatientProfile();
    }

    // -------------------------------------------------------------------------
    // Data loading
    // -------------------------------------------------------------------------

    public void loadCurrentLevel() {
        childDepartments = new ArrayList<>();
        bedBoardRooms = new ArrayList<>();
        liveBedStatusMap = new HashMap<>();

        if (currentDepartment == null) {
            // Site level: load the top-level departments (buildings) belonging to
            // the current site — i.e. linked to the site and having no parent.
            if (currentSite != null && currentSite.getId() != null) {
                Map<String, Object> params = new HashMap<>();
                params.put("siteId", currentSite.getId());
                String jpql = "SELECT d FROM Department d "
                        + "WHERE d.retired=false "
                        + "AND d.inactive=false "
                        + "AND d.superDepartment IS NULL "
                        + "AND d.site.id=:siteId "
                        + "ORDER BY d.name";
                childDepartments = departmentFacade.findByJpql(jpql, params);
            }
        } else {
            // Load child departments of the current department
            Map<String, Object> deptParams = new HashMap<>();
            deptParams.put("parent", currentDepartment);
            String deptJpql = "SELECT d FROM Department d "
                    + "WHERE d.retired=false "
                    + "AND d.inactive=false "
                    + "AND d.superDepartment=:parent "
                    + "ORDER BY d.name";
            childDepartments = departmentFacade.findByJpql(deptJpql, deptParams);

            // Load beds (RoomFacilityCharge) assigned to this department
            Map<String, Object> rfcParams = new HashMap<>();
            rfcParams.put("dept", currentDepartment);
            String rfcJpql = "SELECT rfc FROM RoomFacilityCharge rfc "
                    + "WHERE rfc.retired=false "
                    + "AND rfc.department=:dept "
                    + "ORDER BY rfc.name";
            bedBoardRooms = roomFacilityChargeFacade.findByJpql(rfcJpql, rfcParams);

            // Compute live status for each bed
            for (RoomFacilityCharge rfc : bedBoardRooms) {
                liveBedStatusMap.put(rfc.getId(), computeLiveStatus(rfc));
            }
        }
    }

    private BedStatus computeLiveStatus(RoomFacilityCharge rfc) {
        // Explicit non-Available status set on the room takes priority
        if (rfc.getRoom() != null
                && rfc.getRoom().getBedStatus() != null
                && rfc.getRoom().getBedStatus() != BedStatus.Available) {
            return rfc.getRoom().getBedStatus();
        }
        // Otherwise check for an active (undischarged) patient
        Map<String, Object> params = new HashMap<>();
        params.put("rfc", rfc);
        long count = patientRoomFacade.findLongByJpql(
                "SELECT COUNT(pr) FROM PatientRoom pr "
                + "WHERE pr.retired=false AND pr.discharged=false AND pr.roomFacilityCharge=:rfc",
                params);
        return count > 0 ? BedStatus.Occupied : BedStatus.Available;
    }

    // -------------------------------------------------------------------------
    // View helpers
    // -------------------------------------------------------------------------

    public boolean isCanGoUp() {
        return !breadcrumb.isEmpty();
    }

    public boolean isAtSiteLevel() {
        return currentDepartment == null;
    }

    public String getCurrentLevelName() {
        if (currentDepartment != null) {
            return currentDepartment.getName();
        }
        return currentSite != null ? currentSite.getName()
                : (sessionController.getInstitution() != null
                        ? sessionController.getInstitution().getName() : "Site");
    }

    public String getCurrentSvgParentView() {
        if (currentDepartment != null) {
            return currentDepartment.getSvgParentView();
        }
        return currentSite != null ? currentSite.getSvgParentView() : null;
    }

    public String getBedStatusCssClass(RoomFacilityCharge rfc) {
        if (rfc == null || rfc.getId() == null) {
            return BedStatus.Available.getCssClass();
        }
        BedStatus status = liveBedStatusMap.get(rfc.getId());
        return (status != null ? status : BedStatus.Available).getCssClass();
    }

    public String getBedStatusLabel(RoomFacilityCharge rfc) {
        if (rfc == null || rfc.getId() == null) {
            return BedStatus.Available.getDisplayLabel();
        }
        BedStatus status = liveBedStatusMap.get(rfc.getId());
        return (status != null ? status : BedStatus.Available).getDisplayLabel();
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Institution getCurrentSite() {
        return currentSite;
    }

    public Department getCurrentDepartment() {
        return currentDepartment;
    }

    public List<Department> getBreadcrumb() {
        return breadcrumb;
    }

    public List<Department> getChildDepartments() {
        return childDepartments;
    }

    public List<RoomFacilityCharge> getBedBoardRooms() {
        return bedBoardRooms;
    }
}
