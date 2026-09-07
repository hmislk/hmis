/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.dataStructure.DateGroup;
import com.divudi.core.data.dataStructure.RosterTable;
import com.divudi.core.data.dataStructure.RosterRow;
import com.divudi.core.data.dataStructure.RosterCell;

import com.divudi.ejb.HumanResourceBean;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.hr.Roster;
import com.divudi.core.entity.hr.Shift;
import com.divudi.core.entity.hr.StaffShift;
import com.divudi.core.facade.StaffShiftFacade;
import com.divudi.core.facade.StaffShiftHistoryFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class ShiftTableController implements Serializable {

    Date fromDate;
    Date toDate;
    Roster roster;
    Shift shift;
    StaffShift staffShift;
    RosterTable rosterTable;
    private List<DateGroup> dateGroups = new ArrayList<>();
    private String viewMode = "ALL";
    private Long selectedFilterStaffId;
    private Staff selectedFilterStaff;

    @EJB
    RosterGeneratorService rosterGeneratorService;

    @EJB
    HumanResourceBean humanResourceBean;

    @EJB
    StaffShiftFacade staffShiftFacade;
    @Inject
    SessionController sessionController;
    @Inject
    ShiftController shiftController;

    boolean all;
    Staff staff;

    private Integer activeDateIndex = -1;

    // ── ACTIONS ──────────────────────────────────────────────────────────

    public void makeNull() {
        fromDate = null;
        toDate = null;
        roster = null;
        rosterTable = null;
        clearAddDialogState();
        dateGroups = new ArrayList<>();
        viewMode = "ALL";
        selectedFilterStaffId = null;
        selectedFilterStaff = null;
    }

    public void makeTableNull() {
        rosterTable = null;
        clearAddDialogState();
        dateGroups = new ArrayList<>();
    }

    public void generateAutoRoster() {
        if (roster == null) {
            JsfUtil.addErrorMessage("Please select a roster.");
            return;
        }
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select from and to dates.");
            return;
        }
        rosterTable = rosterGeneratorService.generateRosterTable(fromDate, toDate, roster);
        if (rosterTable.getWarnings() != null) {
            for (String w : rosterTable.getWarnings()) {
                JsfUtil.addErrorMessage(w);
            }
        }
        rebuildDateGroups();
    }

    public void fetchShiftTable() {
        if (roster == null) {
            JsfUtil.addErrorMessage("Please select a roster.");
            return;
        }
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select from and to dates.");
            return;
        }
        rosterTable = rosterGeneratorService.fetchExistingRosterTable(fromDate, toDate, roster);
        if (rosterTable != null && rosterTable.getWarnings() != null) {
            for (String w : rosterTable.getWarnings()) {
                JsfUtil.addErrorMessage(w);
            }
        }
        if (rosterTable == null || rosterTable.getRows() == null || rosterTable.getRows().isEmpty()) {
            JsfUtil.addErrorMessage("No existing roster data found for the selected period.");
        }
        rebuildDateGroups();
    }

    public void selectRosterLstener() {
        makeTableNull();
        selectedFilterStaffId = null;
        selectedFilterStaff = null;
        getShiftController().setCurrentRoster(roster);
    }

    // ── DATE-GROUPED VIEW MODEL ──────────────────────────────────────────

    /**
     * Returns the cached date-grouped view model for the accordion UI.
     */
    public List<DateGroup> getDateGroups() {
        return dateGroups;
    }

    /**
     * Returns the roster data grouped by date for the accordion UI.
     * Each DateGroup contains ShiftGroups that reference the live RosterCells
     * inside rosterTable, so any edit flows back to the underlying table.
     */
    private void rebuildDateGroups() {
        dateGroups = new ArrayList<>();
        if (rosterTable == null || rosterTable.getDates() == null
                || rosterTable.getRows() == null) {
            return;
        }

        for (int dateIdx = 0; dateIdx < rosterTable.getDates().size(); dateIdx++) {
            Date d = rosterTable.getDates().get(dateIdx);
            DateGroup group = new DateGroup(d);

            for (RosterRow row : rosterTable.getRows()) {
                if (row.getCells() == null) continue;
                if (dateIdx >= row.getCells().size()) continue;

                RosterCell cell = row.getCells().get(dateIdx);
                DateGroup.ShiftGroup sg = new DateGroup.ShiftGroup(
                        row.getShift(),
                        row.getShiftName(),
                        cell
                );
                group.getShiftGroups().add(sg);
            }

            dateGroups.add(group);
        }
    }

    /**
     * Returns all non-day-off shifts available in this roster.
     * Used to populate the per-staff shift-change dropdown.
     */
    public List<Shift> getAvailableShifts() {
        List<Shift> shifts = new ArrayList<>();
        if (rosterTable == null || rosterTable.getRows() == null) return shifts;
        for (RosterRow row : rosterTable.getRows()) {
            if (row.getShift() == null) continue;
            if (row.isDayOff() || row.getShift().isDayOff()) continue;
            shifts.add(row.getShift());
        }
        return shifts;
    }

    // ── PER-STAFF EDITS ──────────────────────────────────────────────────

    /**
     * Changes a staff's shift on the given date.
     * Removes them from currentCell and adds them to the cell on the
     * target shift for the same date.
     */
    public void changeStaffShift(RosterCell currentCell, Staff staffMember, Long targetShiftId) {
        if (currentCell == null || staffMember == null || targetShiftId == null) {
            return;
        }

        Shift currentShift = findShiftForCell(currentCell);
        if (currentShift != null && targetShiftId.equals(currentShift.getId())) {
            return; // no-op: same shift
        }

        RosterCell targetCell = findCellForShiftAndDate(targetShiftId, currentCell.getDate());
        if (targetCell == null) {
            JsfUtil.addErrorMessage("Target shift not found.");
            return;
        }

        // Prevent duplicate in target
        if (targetCell.getAssignedStaff() != null) {
            for (Staff s : targetCell.getAssignedStaff()) {
                if (s != null && s.getId() != null
                        && s.getId().equals(staffMember.getId())) {
                    JsfUtil.addErrorMessage(staffLabel(staffMember)
                            + " is already in that shift.");
                    return;
                }
            }
        }

        removeStaffById(currentCell, staffMember.getId());

        if (targetCell.getAssignedStaff() == null) {
            targetCell.setAssignedStaff(new ArrayList<>());
        }
        targetCell.getAssignedStaff().add(staffMember);
        rebuildDateGroups();
    }

    /**
     * Removes a staff from the given cell.
     */
    public void removeStaffFromCell(RosterCell cell, Staff staffMember) {
        if (cell == null || staffMember == null) return;
        removeStaffById(cell, staffMember.getId());
        rebuildDateGroups();
    }

    private void removeStaffById(RosterCell cell, Long staffId) {
        if (cell == null || staffId == null) return;
        if (cell.getAssignedStaff() == null) return;
        Iterator<Staff> it = cell.getAssignedStaff().iterator();
        while (it.hasNext()) {
            Staff s = it.next();
            if (s != null && s.getId() != null && s.getId().equals(staffId)) {
                it.remove();
                return;
            }
        }
    }

    private Shift findShiftForCell(RosterCell cell) {
        if (cell == null || rosterTable == null || rosterTable.getRows() == null) {
            return null;
        }
        for (RosterRow row : rosterTable.getRows()) {
            if (row.getCells() == null) continue;
            for (RosterCell c : row.getCells()) {
                if (c == cell) return row.getShift();
            }
        }
        return null;
    }

    private RosterCell findCellForShiftAndDate(Long shiftId, Date date) {
        if (shiftId == null || date == null || rosterTable == null
                || rosterTable.getRows() == null) {
            return null;
        }
        Date target = clearTime(date);
        for (RosterRow row : rosterTable.getRows()) {
            if (row.getShift() == null || row.getShift().getId() == null) continue;
            if (!row.getShift().getId().equals(shiftId)) continue;
            if (row.getCells() == null) continue;
            for (RosterCell c : row.getCells()) {
                if (c.getDate() != null && clearTime(c.getDate()).equals(target)) {
                    return c;
                }
            }
        }
        return null;
    }

    // ── ADD STAFF DIALOG ─────────────────────────────────────────────────

    private RosterCell addingToCell;
    private Shift addingToShift;
    private Long staffToAddId;
    private List<Staff> availableStaffForAdd;

    // Bound to the shift-change dropdown. The onShiftChange listener reads
    // this value, then nulls it so each row re-renders with its own shift.
    private Long selectedShiftId;

    public Long getSelectedShiftId() {
        return selectedShiftId;
    }

    public void setSelectedShiftId(Long selectedShiftId) {
        this.selectedShiftId = selectedShiftId;
    }

    /**
     * Opens the add-staff dialog for a specific shift on a specific date.
     */
    public void openAddStaffDialog(RosterCell cell, Shift shiftParam) {
        this.addingToCell = cell;
        this.addingToShift = shiftParam;
        this.staffToAddId = null;
        refreshAvailableStaffForAdd();
    }

    /**
     * Rebuilds the dropdown list: roster staff NOT already in this cell.
     * Staff assigned to other shifts on the same day ARE included (with a warning on add).
     */
    private void refreshAvailableStaffForAdd() {
        availableStaffForAdd = new ArrayList<>();
        if (addingToCell == null || roster == null) return;

        List<Staff> rosterStaff = humanResourceBean.fetchStaff(roster);
        if (rosterStaff == null) return;

        List<Staff> inCell = addingToCell.getAssignedStaff() != null
                ? addingToCell.getAssignedStaff() : new ArrayList<>();

        for (Staff s : rosterStaff) {
            if (s == null || s.getId() == null) continue;
            boolean already = false;
            for (Staff c : inCell) {
                if (c != null && c.getId() != null && c.getId().equals(s.getId())) {
                    already = true;
                    break;
                }
            }
            if (!already) availableStaffForAdd.add(s);
        }
    }

    /**
     * Confirms add from the dialog.
     * Warns if staff is already assigned elsewhere on the same day,
     * but still performs the add.
     */
    public void confirmAddStaff() {
        if (addingToCell == null || staffToAddId == null) {
            JsfUtil.addErrorMessage("Please select a staff member.");
            return;
        }

        Staff resolved = null;
        if (availableStaffForAdd != null) {
            for (Staff s : availableStaffForAdd) {
                if (s != null && s.getId() != null && s.getId().equals(staffToAddId)) {
                    resolved = s;
                    break;
                }
            }
        }
        if (resolved == null) {
            JsfUtil.addErrorMessage("Selected staff not found.");
            return;
        }

        String conflictShift = findSameDayAssignment(resolved.getId(),
                addingToCell.getDate(), addingToCell);
        if (conflictShift != null) {
            JsfUtil.addErrorMessage("Warning: " + staffLabel(resolved)
                    + " is also assigned to " + conflictShift + " on this day.");
        }

        if (addingToCell.getAssignedStaff() == null) {
            addingToCell.setAssignedStaff(new ArrayList<>());
        }
        addingToCell.getAssignedStaff().add(resolved);

        staffToAddId = null;
        refreshAvailableStaffForAdd();
        rebuildDateGroups();
    }

    private String findSameDayAssignment(Long staffId, Date date, RosterCell excludeCell) {
        if (staffId == null || date == null) return null;
        if (rosterTable == null || rosterTable.getRows() == null) return null;
    
        Date target = clearTime(date);
    
        for (RosterRow row : rosterTable.getRows()) {
            if (row.getCells() == null) continue;
            for (RosterCell c : row.getCells()) {
                if (c == excludeCell) continue;
                if (c.getDate() == null) continue;
                if (!clearTime(c.getDate()).equals(target)) continue;
                if (c.getAssignedStaff() == null) continue;
                for (Staff s : c.getAssignedStaff()) {
                    if (s != null && s.getId() != null
                            && s.getId().equals(staffId)) {
                        return row.getShiftName();
                    }
                }
            }
        }
        return null;
    }

    private void clearAddDialogState() {
        addingToCell = null;
        addingToShift = null;
        staffToAddId = null;
        availableStaffForAdd = null;
    }

    private Date clearTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    // ── SAVE ─────────────────────────────────────────────────────────────

    @Inject
    PhDateController phDateController;

    @EJB
    StaffShiftHistoryFacade staffShiftHistoryFacade;

    public void save() {
        if (rosterTable == null || rosterTable.getRows() == null || rosterTable.getRows().isEmpty()) {
            JsfUtil.addErrorMessage("Nothing to save. Please generate a roster first.");
            return;
        }
        if (roster == null || fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Roster and date range are required.");
            return;
        }

        try {
            rosterGeneratorService.replaceRosterShifts(roster, fromDate, toDate, rosterTable,sessionController.getLoggedUser());
            JsfUtil.addSuccessMessage("Roster saved successfully.");
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Save failed — no changes were made. " + e.getMessage());
        }
    }

    /**
     * AJAX listener for per-row shift-change dropdown.
     * Reads the cell and staff from f:attribute, and the new shift id
     * from the component's submitted value.
     */
    public void onShiftChange(javax.faces.event.AjaxBehaviorEvent event) {
        javax.faces.component.UIComponent component = event.getComponent();

        // The selectedShiftId was already set by JSF from the dropdown value binding
        Long targetShiftId = this.selectedShiftId;

        Object cellObj = component.getAttributes().get("cellRef");
        Object staffObj = component.getAttributes().get("staffRef");

        if (!(cellObj instanceof RosterCell) || !(staffObj instanceof Staff)) {
            JsfUtil.addErrorMessage("Could not resolve cell or staff.");
            return;
        }

        RosterCell currentCell = (RosterCell) cellObj;
        Staff staffMember = (Staff) staffObj;

        changeStaffShift(currentCell, staffMember, targetShiftId);

        // Reset so dropdowns render with their row's current shift after update
        this.selectedShiftId = null;
    }

    /**
     * Called when user picks a staff from the Single Staff dropdown.
     */
    public void onFilterStaffChange() {
        selectedFilterStaff = null;
        if (selectedFilterStaffId == null || roster == null) return;

        List<Staff> rosterStaff = humanResourceBean.fetchStaff(roster);
        if (rosterStaff == null) return;

        for (Staff s : rosterStaff) {
            if (s != null && s.getId() != null
                    && s.getId().equals(selectedFilterStaffId)) {
                selectedFilterStaff = s;
                break;
            }
        }
    }

    /**
     * Returns all staff in this roster for the filter dropdown.
     */
    public List<Staff> getRosterStaffList() {
        if (roster == null) return new ArrayList<>();
        List<Staff> list = humanResourceBean.fetchStaff(roster);
        return list != null ? list : new ArrayList<>();
    }

    /**
     * Toggles the selected filter staff in/out of the given cell.
     * Called from Single Staff view when user clicks a cell.
     * Changes are in-memory only until Save is clicked.
     */
    public void toggleStaffInCell(RosterCell cell) {
        if (cell == null || selectedFilterStaff == null) return;
    
        if (isStaffAssignedToCell(cell)) {
            removeStaffById(cell, selectedFilterStaff.getId());
        } else {
            String conflictShift = findSameDayAssignment(
                    selectedFilterStaff.getId(), cell.getDate(), cell);
            if (conflictShift != null) {
                JsfUtil.addErrorMessage("Warning: " + staffLabel(selectedFilterStaff)
                        + " is also assigned to " + conflictShift + " on this day.");
            }
            if (cell.getAssignedStaff() == null) {
                cell.setAssignedStaff(new ArrayList<>());
            }
            cell.getAssignedStaff().add(selectedFilterStaff);
        }
        rebuildDateGroups();
    }

    /**
     * Checks whether the selected filter staff is assigned to the given cell.
     * Used by Single Staff view for tick/cross rendering.
     */
    public boolean isStaffAssignedToCell(RosterCell cell) {
        if (cell == null || selectedFilterStaff == null) return false;
        if (cell.getAssignedStaff() == null) return false;
        for (Staff s : cell.getAssignedStaff()) {
            if (s != null && s.getId() != null
                    && s.getId().equals(selectedFilterStaff.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Safe display label: "Name (code)" with fallbacks at every level.
     *
     * Reads Staff.code via getCodeRaw() to avoid the render-time mutation
     * side effect in Staff.getCode() (which writes a derived code back to
     * the field on blank values, dirtying the JPA entity).
     *
     * Name fallback: "Staff #id" if person or name is null.
     * Code fallback: raw code field; then 5-char name-derived string
     * (same logic as getCode(), but read-only); then id.
     */
    public String staffLabel(Staff s) {
        if (s == null) return "(unknown)";

        // --- name part ---
        String name = "";
        if (s.getPerson() != null && s.getPerson().getName() != null
                && !s.getPerson().getName().trim().isEmpty()) {
            name = s.getPerson().getName().trim();
        } else {
            name = "Staff #" + (s.getId() != null ? s.getId() : "?");
        }

        // --- code part (read-only; avoids Staff.getCode() side effect) ---
        String code = s.getCodeRaw();
        if (code == null || code.trim().isEmpty()) {
            // Mirror getCode()'s name-based fallback, but WITHOUT writing back.
            if (s.getPerson() != null && s.getPerson().getName() != null
                    && !s.getPerson().getName().trim().isEmpty()) {
                String temName = s.getPerson().getName() + "      ";
                code = temName.substring(0, 5);
            } else {
                code = s.getId() != null ? String.valueOf(s.getId()) : "?";
            }
        } else {
            code = code.trim();
        }

        return name + " (" + code + ")";
    }

    // ── GETTERS AND SETTERS ──────────────────────────────────────────────

    public ShiftController getShiftController() {
        return shiftController;
    }

    public void setShiftController(ShiftController shiftController) {
        this.shiftController = shiftController;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public HumanResourceBean getHumanResourceBean() {
        return humanResourceBean;
    }

    public void setHumanResourceBean(HumanResourceBean humanResourceBean) {
        this.humanResourceBean = humanResourceBean;
    }

    public Roster getRoster() {
        return roster;
    }

    public void setRoster(Roster roster) {
        this.roster = roster;
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

    public StaffShiftFacade getStaffShiftFacade() {
        return staffShiftFacade;
    }

    public void setStaffShiftFacade(StaffShiftFacade staffShiftFacade) {
        this.staffShiftFacade = staffShiftFacade;
    }

    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }

    public StaffShiftHistoryFacade getStaffShiftHistoryFacade() {
        return staffShiftHistoryFacade;
    }

    public void setStaffShiftHistoryFacade(StaffShiftHistoryFacade staffShiftHistoryFacade) {
        this.staffShiftHistoryFacade = staffShiftHistoryFacade;
    }

    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public StaffShift getStaffShift() {
        return staffShift;
    }

    public void setStaffShift(StaffShift staffShift) {
        this.staffShift = staffShift;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public RosterTable getRosterTable() {
        return rosterTable;
    }

    public void setRosterTable(RosterTable rosterTable) {
        this.rosterTable = rosterTable;
    }

    public RosterCell getAddingToCell() {
        return addingToCell;
    }

    public void setAddingToCell(RosterCell addingToCell) {
        this.addingToCell = addingToCell;
    }

    public Shift getAddingToShift() {
        return addingToShift;
    }

    public void setAddingToShift(Shift addingToShift) {
        this.addingToShift = addingToShift;
    }

    public Long getStaffToAddId() {
        return staffToAddId;
    }

    public void setStaffToAddId(Long staffToAddId) {
        this.staffToAddId = staffToAddId;
    }

    public List<Staff> getAvailableStaffForAdd() {
        if (availableStaffForAdd == null) {
            availableStaffForAdd = new ArrayList<>();
        }
        return availableStaffForAdd;
    }

    public void setAvailableStaffForAdd(List<Staff> availableStaffForAdd) {
        this.availableStaffForAdd = availableStaffForAdd;
    }

    public Integer getActiveDateIndex() { return activeDateIndex; }

    public void setActiveDateIndex(Integer activeDateIndex) { this.activeDateIndex = activeDateIndex; }

    public String getViewMode() {
        return viewMode;
    }

    public void setViewMode(String viewMode) {
        this.viewMode = viewMode;
    }

    public Long getSelectedFilterStaffId() {
        return selectedFilterStaffId;
    }

    public void setSelectedFilterStaffId(Long selectedFilterStaffId) {
        this.selectedFilterStaffId = selectedFilterStaffId;
    }

    public Staff getSelectedFilterStaff() {
        return selectedFilterStaff;
    }

    public void setSelectedFilterStaff(Staff selectedFilterStaff) {
        this.selectedFilterStaff = selectedFilterStaff;
    }
}