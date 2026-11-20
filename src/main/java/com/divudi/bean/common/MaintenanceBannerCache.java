package com.divudi.bean.common;

import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.MaintenanceWindow;
import com.divudi.service.MaintenanceWindowRules;
import com.divudi.service.MaintenanceWindowService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

/**
 * Holds the upcoming and active maintenance windows for a short time so the
 * banner shown on every page does not query the database on each render (#25838).
 * Create and cancel call {@link #invalidate()} so a change shows at once on this server.
 */
@Named
@ApplicationScoped
public class MaintenanceBannerCache implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(MaintenanceBannerCache.class.getName());
    static final long TTL_MILLIS = 45_000L;

    @EJB
    private MaintenanceWindowService maintenanceWindowService;

    private List<MaintenanceWindow> windows = new ArrayList<>();
    private long loadedAt = 0L;

    public synchronized void invalidate() {
        loadedAt = 0L;
    }

    /** Windows whose banner is showing now for the given institution and department. */
    public List<MaintenanceWindow> visibleFor(Institution institution, Department department) {
        Date now = new Date();
        return MaintenanceWindowRules.filterVisible(snapshot(now), institution, department, now);
    }

    private synchronized List<MaintenanceWindow> snapshot(Date now) {
        if (now.getTime() - loadedAt > TTL_MILLIS) {
            try {
                windows = maintenanceWindowService.listUpcomingAndActive(now);
            } catch (Exception e) {
                // The banner must never break a page. Keep the old list and retry after the TTL.
                LOGGER.log(Level.WARNING, "Could not load maintenance windows: {0}", e.getClass().getSimpleName());
            }
            loadedAt = now.getTime();
        }
        return windows;
    }
}
