package com.divudi.service;

import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.MaintenanceWindow;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Plain-Java validation and visibility rules for maintenance windows (#25838).
 */
public final class MaintenanceWindowRules {

    /** Default lead time: the banner starts this long before the window. */
    public static final long DEFAULT_SHOW_BEFORE_MILLIS = 24L * 60 * 60 * 1000;

    /** Longest title the database column holds. */
    public static final int MAX_TITLE_LENGTH = 255;
    /** Longest message; it renders on every page. */
    public static final int MAX_MESSAGE_LENGTH = 1000;
    /** Longest reference note (default varchar column). */
    public static final int MAX_REFERENCE_LENGTH = 255;

    private MaintenanceWindowRules() {
    }

    /** The default "show from" time: 24 hours before the start. */
    public static Date defaultShowFrom(Date startAt) {
        return startAt == null ? null : new Date(startAt.getTime() - DEFAULT_SHOW_BEFORE_MILLIS);
    }

    /**
     * Checks the window and fills a missing showFromAt with the default.
     *
     * @throws MaintenanceWindowException when a rule is broken
     */
    public static void validate(MaintenanceWindow w) {
        if (w == null) {
            throw new MaintenanceWindowException("Maintenance window is required");
        }
        if (w.getMessage() == null || w.getMessage().trim().isEmpty()) {
            throw new MaintenanceWindowException("Message is required");
        }
        if (w.getMessage().length() > MAX_MESSAGE_LENGTH) {
            throw new MaintenanceWindowException("Message must be at most " + MAX_MESSAGE_LENGTH + " characters");
        }
        if (w.getTitle() != null && w.getTitle().length() > MAX_TITLE_LENGTH) {
            throw new MaintenanceWindowException("Title must be at most " + MAX_TITLE_LENGTH + " characters");
        }
        if (w.getExternalReference() != null && w.getExternalReference().length() > MAX_REFERENCE_LENGTH) {
            throw new MaintenanceWindowException("Reference must be at most " + MAX_REFERENCE_LENGTH + " characters");
        }
        if (w.getStartAt() == null) {
            throw new MaintenanceWindowException("Start time is required");
        }
        if (w.getEndAt() == null) {
            throw new MaintenanceWindowException("End time is required");
        }
        if (!w.getEndAt().after(w.getStartAt())) {
            throw new MaintenanceWindowException("End time must be after the start time");
        }
        if (w.getShowFromAt() == null) {
            w.setShowFromAt(defaultShowFrom(w.getStartAt()));
        }
        if (w.getShowFromAt().after(w.getStartAt())) {
            throw new MaintenanceWindowException("Show-from time must not be after the start time");
        }
    }

    /** True when the banner is due: showFromAt <= now < endAt and not retired. */
    public static boolean isShowing(MaintenanceWindow w, Date now) {
        if (w == null || w.isRetired() || w.getShowFromAt() == null || w.getEndAt() == null || now == null) {
            return false;
        }
        return !w.getShowFromAt().after(now) && now.before(w.getEndAt());
    }

    /**
     * True when the window targets this institution or department. A window
     * with no institutions and no departments targets everyone. Otherwise a
     * match on either list is enough.
     */
    public static boolean targets(MaintenanceWindow w, Institution institution, Department department) {
        if (w == null) {
            return false;
        }
        if (w.getInstitutions().isEmpty() && w.getDepartments().isEmpty()) {
            return true;
        }
        if (institution != null) {
            for (Institution i : w.getInstitutions()) {
                if (sameId(i == null ? null : i.getId(), institution.getId())) {
                    return true;
                }
            }
        }
        if (department != null) {
            for (Department d : w.getDepartments()) {
                if (sameId(d == null ? null : d.getId(), department.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Filters the given windows to those showing now for this institution/department. */
    public static List<MaintenanceWindow> filterVisible(List<MaintenanceWindow> all,
            Institution institution, Department department, Date now) {
        List<MaintenanceWindow> out = new ArrayList<>();
        if (all == null) {
            return out;
        }
        for (MaintenanceWindow w : all) {
            if (isShowing(w, now) && targets(w, institution, department)) {
                out.add(w);
            }
        }
        return out;
    }

    private static boolean sameId(Long a, Long b) {
        return a != null && a.equals(b);
    }
}
