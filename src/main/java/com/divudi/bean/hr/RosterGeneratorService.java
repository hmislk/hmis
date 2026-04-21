/*
 * Open Hospital Management Information System
 * Auto Roster Generator Service
 *
 * Generates a RosterTable with shifts as rows and dates as columns.
 * Each cell contains a list of staff assigned to that shift on that date.
 *
 * Package: com.divudi.bean.hr
 * File: RosterGeneratorService.java
 */
package com.divudi.bean.hr;

import com.divudi.core.data.dataStructure.RosterCell;
import com.divudi.core.data.dataStructure.RosterRow;
import com.divudi.core.data.dataStructure.RosterTable;
import com.divudi.core.data.hr.DayType;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.hr.Roster;
import com.divudi.core.entity.hr.Shift;
import com.divudi.core.entity.hr.ShiftStaffRequirement;
import com.divudi.core.entity.hr.StaffShift;
import com.divudi.core.facade.ShiftFacade;
import com.divudi.core.facade.StaffShiftFacade;
import com.divudi.ejb.HumanResourceBean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class RosterGeneratorService implements Serializable {

    @EJB
    private StaffShiftFacade staffShiftFacade;

    @EJB
    private HumanResourceBean humanResourceBean;

    @EJB
    private ShiftFacade shiftFacade;

    @Inject
    private PhDateController phDateController;

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Generates a RosterTable for the given date range and roster.
     *
     * Output structure:
     *   - rows = shifts (ordered by shiftOrder)
     *   - columns = dates (fromDate to toDate)
     *   - each cell = list of Staff assigned to that shift on that date
     *
     * Algorithm:
     *   - Day by day, shift by shift
     *   - Picks staff based on ShiftStaffRequirement count for the day type
     *   - Avoids continuous (back-to-back) shifts where possible
     *   - Falls back to continuous only when not enough non-continuous staff
     *   - Distributes fairly: least-assigned staff gets picked first
     *
     * @param fromDate start of range (inclusive)
     * @param toDate end of range (inclusive)
     * @param roster the selected roster
     * @return RosterTable ready for rendering
     */
    public RosterTable generateRosterTable(Date fromDate, Date toDate, Roster roster) {

        RosterTable result = new RosterTable();
        List<String> warnings = new ArrayList<>();

        // --- Validate ---
        if (roster == null || fromDate == null || toDate == null) {
            warnings.add("Roster, from date, and to date are required.");
            result.setWarnings(warnings);
            return result;
        }
        if (fromDate.after(toDate)) {
            warnings.add("From date must be before to date.");
            result.setWarnings(warnings);
            return result;
        }

        // --- Load shifts ---
        List<Shift> shifts = fetchActiveShiftsOrdered(roster);
        if (shifts.isEmpty()) {
            warnings.add("No active shifts found for this roster.");
            result.setWarnings(warnings);
            return result;
        }

        // --- Load staff ---
        List<Staff> allStaff = humanResourceBean.fetchStaff(roster);
        if (allStaff.isEmpty()) {
            warnings.add("No staff available for this roster.");
        }

        // --- Build date list ---
        List<Date> dates = buildDateList(fromDate, toDate);
        result.setDates(dates);

        // --- Initialize rows (one per shift) ---
        Map<Long, RosterRow> rowMap = new LinkedHashMap<>();
        for (Shift shift : shifts) {
            RosterRow row = new RosterRow();
            row.setShift(shift);
            row.setShiftName(shift.getName());
            row.setShiftOrder(shift.getShiftOrder());
            row.setDayOff(shift.isDayOff());
            row.setCells(new ArrayList<>());
            rowMap.put(shift.getId(), row);
        }

        // --- Fairness tracking ---
        Map<Long, Integer> assignmentCounts = new HashMap<>();
        Map<Long, Date> lastAssignedDate = new HashMap<>();
        for (Staff s : allStaff) {
            assignmentCounts.put(s.getId(), 0);
            lastAssignedDate.put(s.getId(), null);
        }

        // --- Previous day continuity ---
        Map<Long, Set<Long>> previousDayTracking = loadPreviousDayAssignments(
                fromDate, allStaff, shifts);

        // --- Generate day by day ---
        for (Date date : dates) {

            DayType holidayType = phDateController.getHolidayType(date);

            // Current day tracking: shiftId -> set of staffIds assigned
            Map<Long, Set<Long>> currentDayAssignments = new HashMap<>();
            Long lastProcessedShiftId = null;

            for (Shift shift : shifts) {

                RosterCell cell = new RosterCell();
                cell.setDate(date);

                if (shift.isDayOff()) {
                    cell.setRequiredCount(0);
                    cell.setAssignedStaff(new ArrayList<>());
                    rowMap.get(shift.getId()).getCells().add(cell);
                    lastProcessedShiftId = shift.getId();
                    continue;
                }

                int requiredCount = resolveRequiredCount(shift.getStaffRequirement(), date, holidayType);
                cell.setRequiredCount(requiredCount);

                if (requiredCount <= 0 || allStaff.isEmpty()) {
                    cell.setAssignedStaff(new ArrayList<>());
                    rowMap.get(shift.getId()).getCells().add(cell);
                    lastProcessedShiftId = shift.getId();
                    continue;
                }

                // Split eligible staff into preferred and fallback
                List<Staff> preferred = new ArrayList<>();
                List<Staff> fallback = new ArrayList<>();

                for (Staff s : allStaff) {
                    if (isContinuousShift(s.getId(), shift,
                            currentDayAssignments, previousDayTracking)) {
                        fallback.add(s);
                    } else {
                        preferred.add(s);
                    }
                }

                // Sort by fairness
                Comparator<Staff> fairness = buildFairnessComparator(
                        assignmentCounts, lastAssignedDate);
                preferred.sort(fairness);
                fallback.sort(fairness);

                // Pick staff: preferred first, then fallback
                List<Staff> assigned = new ArrayList<>();
                int remaining = requiredCount;

                for (Staff s : preferred) {
                    if (remaining <= 0) break;
                    assigned.add(s);
                    remaining--;
                }
                for (Staff s : fallback) {
                    if (remaining <= 0) break;
                    assigned.add(s);
                    remaining--;
                }

                cell.setAssignedStaff(assigned);

                if (assigned.size() < requiredCount) {
                    warnings.add("Understaffed: " + shift.getName()
                            + " on " + formatDate(date)
                            + " - need " + requiredCount
                            + ", assigned " + assigned.size());
                }

                rowMap.get(shift.getId()).getCells().add(cell);

                // Update tracking
                Set<Long> assignedIds = new HashSet<>();
                for (Staff s : assigned) {
                    assignedIds.add(s.getId());
                    assignmentCounts.merge(s.getId(), 1, Integer::sum);
                    lastAssignedDate.put(s.getId(), date);
                }
                currentDayAssignments.put(shift.getId(), assignedIds);
                lastProcessedShiftId = shift.getId();
            }

            // Carry forward last shift for next day's continuity
            previousDayTracking = new HashMap<>();
            if (lastProcessedShiftId != null) {
                previousDayTracking.put(lastProcessedShiftId,
                        currentDayAssignments.getOrDefault(
                                lastProcessedShiftId, Collections.emptySet()));
            }
        }

        // --- Build result ---
        result.setRows(new ArrayList<>(rowMap.values()));
        result.setWarnings(warnings);
        return result;
    }

    // =========================================================================
    // REQUIRED COUNT
    // =========================================================================

    private int resolveRequiredCount(ShiftStaffRequirement req, Date date, DayType holidayType) {
        if (req == null) return 0;

        // Check holiday first (priority)
        if (holidayType != null) {
            if (holidayType == DayType.Poya)               return safe(req.getPoyaDayCount());
            if (holidayType == DayType.PublicHoliday)       return safe(req.getPublicHolidayCount());
            if (holidayType == DayType.MurchantileHoliday)  return safe(req.getMercantileHolidayCount());
        }

        // Fall back to weekday
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        switch (cal.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:    return safe(req.getMondayCount());
            case Calendar.TUESDAY:   return safe(req.getTuesdayCount());
            case Calendar.WEDNESDAY: return safe(req.getWednesdayCount());
            case Calendar.THURSDAY:  return safe(req.getThursdayCount());
            case Calendar.FRIDAY:    return safe(req.getFridayCount());
            case Calendar.SATURDAY:  return safe(req.getSaturdayCount());
            case Calendar.SUNDAY:    return safe(req.getSundayCount());
            default:                 return 0;
        }
    }

    private int safe(Integer val) {
        return val != null ? val : 0;
    }

    // =========================================================================
    // CONTINUOUS SHIFT DETECTION
    // =========================================================================

    private boolean isContinuousShift(Long staffId,
            Shift currentShift,
            Map<Long, Set<Long>> currentDayAssignments,
            Map<Long, Set<Long>> previousDayTracking) {

        // Case 1: Previous shift on same day
        Shift prevShift = currentShift.getPreviousShift();
        if (prevShift != null) {
            Set<Long> prevStaff = currentDayAssignments.getOrDefault(
                    prevShift.getId(), Collections.emptySet());
            if (prevStaff.contains(staffId)) {
                return true;
            }
        }

        // Case 2: First shift of day - check previous day's last shift
        if (currentShift.isFirstShift() || prevShift == null) {
            for (Set<Long> staffSet : previousDayTracking.values()) {
                if (staffSet.contains(staffId)) {
                    return true;
                }
            }
        }

        return false;
    }

    // =========================================================================
    // FAIRNESS
    // =========================================================================

    private Comparator<Staff> buildFairnessComparator(
            Map<Long, Integer> assignmentCounts,
            Map<Long, Date> lastAssignedDate) {

        return Comparator
                .comparingInt((Staff s) -> assignmentCounts.getOrDefault(s.getId(), 0))
                .thenComparing((Staff s) -> {
                    Date last = lastAssignedDate.get(s.getId());
                    return last != null ? last.getTime() : 0L;
                })
                .thenComparingLong(s -> s.getId());
    }

    // =========================================================================
    // DATA LOADING
    // =========================================================================

    private List<Shift> fetchActiveShiftsOrdered(Roster roster) {
        String jpql = "SELECT s FROM Shift s "
                + " WHERE s.roster = :r "
                + " AND s.retired = false "
                + " AND s.hideShift = false "
                + " ORDER BY s.shiftOrder";
        HashMap<String, Object> m = new HashMap<>();
        m.put("r", roster);
        return shiftFacade.findByJpql(jpql, m);
    }

    private Map<Long, Set<Long>> loadPreviousDayAssignments(Date fromDate, List<Staff> allStaff, List<Shift> shifts) {
        Map<Long, Set<Long>> result = new HashMap<>();
        if (shifts.isEmpty() || allStaff.isEmpty()) {
            return result;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(fromDate);
        cal.add(Calendar.DATE, -1);
        Date dayStart = clearTime(cal.getTime());

        cal.setTime(dayStart);
        cal.add(Calendar.DATE, 1);
        cal.add(Calendar.MILLISECOND, -1);
        Date dayEnd = cal.getTime();

        Shift lastShift = shifts.get(shifts.size() - 1);

        String jpql = "SELECT ss FROM StaffShift ss "
                + " WHERE ss.retired = false "
                + " AND ss.shift = :shift "
                + " AND ss.roster = :roster "
                + " AND ss.shiftDate >= :dayStart "
                + " AND ss.shiftDate <= :dayEnd";
        HashMap<String, Object> params = new HashMap<>();
        params.put("shift", lastShift);
        params.put("roster", lastShift.getRoster());
        params.put("dayStart", dayStart);
        params.put("dayEnd", dayEnd);

        List<StaffShift> records = staffShiftFacade.findByJpql(jpql, params);

        Set<Long> staffIds = new HashSet<>();
        if (records != null) {
            for (StaffShift ss : records) {
                if (ss.getStaff() != null && ss.getStaff().getId() != null) {
                    staffIds.add(ss.getStaff().getId());
                }
            }
        }
        if (!staffIds.isEmpty()) {
            result.put(lastShift.getId(), staffIds);
        }
        return result;
    }

    /**
     * Fetches existing StaffShift records and builds a RosterTable
     * in the same structure as generateRosterTable().
     */
    public RosterTable fetchExistingRosterTable(Date fromDate, Date toDate, Roster roster) {

        RosterTable result = new RosterTable();
        List<String> warnings = new ArrayList<>();

        if (roster == null || fromDate == null || toDate == null) {
            warnings.add("Roster, from date, and to date are required.");
            result.setWarnings(warnings);
            return result;
        }

        if (fromDate.after(toDate)) {
            warnings.add("From date must be before to date.");
            result.setWarnings(warnings);
            return result;
        }

        // --- Load shifts ---
        List<Shift> shifts = fetchActiveShiftsOrdered(roster);
        if (shifts.isEmpty()) {
            warnings.add("No active shifts found for this roster.");
            result.setWarnings(warnings);
            return result;
        }

        // --- Build date list ---
        List<Date> dates = buildDateList(fromDate, toDate);
        result.setDates(dates);

        // --- Bulk fetch all StaffShift records for this roster + date range ---
        Map<String, List<Staff>> cellMap = buildCellMap(roster, fromDate, toDate);

        // --- Pre-compute holiday types once per date ---
        Map<Long, DayType> holidayTypeMap = new HashMap<>();
        for (Date date : dates) {
            holidayTypeMap.put(clearTime(date).getTime(), phDateController.getHolidayType(date));
        }

        // --- Build rows (one per shift) ---
        for (Shift shift : shifts) {
            RosterRow row = new RosterRow();
            row.setShift(shift);
            row.setShiftName(shift.getName());
            row.setShiftOrder(shift.getShiftOrder());
            row.setDayOff(shift.isDayOff());
            row.setCells(new ArrayList<>());

            for (Date date : dates) {
                DayType holidayType = holidayTypeMap.get(clearTime(date).getTime());
                RosterCell cell = new RosterCell();
                cell.setDate(date);

                String key = shift.getId() + "_" + clearTime(date).getTime();
                List<Staff> assigned = cellMap.getOrDefault(key, new ArrayList<>());
                cell.setAssignedStaff(assigned);

                if (!shift.isDayOff()) {
                    int requiredCount = resolveRequiredCount(shift.getStaffRequirement(), date, holidayType);
                    cell.setRequiredCount(requiredCount);
                    if (assigned.size() < requiredCount) {
                        warnings.add("Understaffed: " + shift.getName()
                                + " on " + formatDate(date)
                                + " - need " + requiredCount
                                + ", have " + assigned.size());
                    }
                }

                row.getCells().add(cell);
            }

            result.getRows().add(row);
        }

        result.setWarnings(warnings);
        return result;
    }

    /**
     * Single bulk query: fetches all StaffShift records for a roster + date range,
     * then groups them into a map keyed by "shiftId_dateMillis" -> list of Staff.
     */
    private Map<String, List<Staff>> buildCellMap(Roster roster, Date fromDate, Date toDate) {
        Map<String, List<Staff>> cellMap = new HashMap<>();

        String jpql = "SELECT ss FROM StaffShift ss "
                + " WHERE ss.retired = false "
                + " AND ss.roster = :roster "
                + " AND ss.shiftDate >= :fromDate "
                + " AND ss.shiftDate <= :toDate";
        HashMap<String, Object> params = new HashMap<>();
        params.put("roster", roster);
        params.put("fromDate", clearTime(fromDate));
        params.put("toDate", clearTime(toDate));

        List<StaffShift> records = staffShiftFacade.findByJpql(jpql, params);
        if (records == null) return cellMap;

        for (StaffShift ss : records) {
            if (ss.getShift() == null || ss.getStaff() == null) continue;
            String key = ss.getShift().getId() + "_" + clearTime(ss.getShiftDate()).getTime();
            cellMap.computeIfAbsent(key, k -> new ArrayList<>()).add(ss.getStaff());
        }

        return cellMap;
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

    // =========================================================================
    // UTILITIES
    // =========================================================================

    private List<Date> buildDateList(Date from, Date to) {
        List<Date> dates = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTime(from);
        while (!cal.getTime().after(to)) {
            dates.add(cal.getTime());
            cal.add(Calendar.DATE, 1);
        }
        return dates;
    }

    private String formatDate(Date date) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }

    /**
     * Atomically replaces existing StaffShift records with new ones.
     * Runs in a single transaction — either everything commits or everything rolls back.
     */
    public void replaceRosterShifts(Roster roster, Date fromDate, Date toDate, RosterTable rosterTable, WebUser retirer) {
        // 1. Soft-delete existing
        Date from = clearTime(fromDate);
        Date to = clearTime(toDate);

        String jpql = "SELECT ss FROM StaffShift ss "
                + " WHERE ss.retired = false "
                + " AND ss.roster = :r "
                + " AND ss.shiftDate BETWEEN :fd AND :td";
        HashMap<String, Object> params = new HashMap<>();
        params.put("r", roster);
        params.put("fd", from);
        params.put("td", to);

        List<StaffShift> existing = staffShiftFacade.findByJpql(jpql, params);
        if (existing != null) {
            for (StaffShift ss : existing) {
                ss.setRetired(true);
                ss.setRetiredAt(new Date());
                ss.setRetirer(retirer);
                staffShiftFacade.edit(ss);
            }
        }

        // 2. Create new records from rosterTable
        if (rosterTable == null || rosterTable.getRows() == null) return;

        for (RosterRow row : rosterTable.getRows()) {
            if (row.isDayOff() || row.getCells() == null) continue;
            for (RosterCell cell : row.getCells()) {
                if (cell.getAssignedStaff() == null) continue;
                for (Staff staff : cell.getAssignedStaff()) {
                    StaffShift ss = new StaffShift();
                    ss.setStaff(staff);
                    ss.setShift(row.getShift());
                    ss.setRoster(roster);
                    ss.setShiftDate(cell.getDate());
                    // set any other fields you need
                    staffShiftFacade.create(ss);
                }
            }
        }
    }
}