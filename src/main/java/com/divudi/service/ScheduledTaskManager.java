package com.divudi.service;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import com.divudi.core.entity.ScheduledProcessConfiguration;
import com.divudi.core.facade.ScheduledProcessConfigurationFacade;
import com.divudi.service.ScheduledProcessService;

/**
 *
 * @author Dr M H Buddhika Ariyaratne
 * buddhika.ari@gmail.com
 *
 */
@Singleton
@Startup
public class ScheduledTaskManager {

    @EJB
    private ChannelService channelService;

    @EJB
    private ScheduledProcessConfigurationFacade configFacade;

    @EJB
    private ScheduledProcessService scheduledProcessService;

        /**
     * Scheduled method to retire non-settled online bills every 5 minutes.
     */
    @Schedule(minute = "*/5", hour = "*", persistent = false)
    public void executeRetireNonSettledOnlineBills() {
        try {
            channelService.retireNonSettledOnlineBills();
            // Optionally, add logging here
        } catch (Exception e) {
            // Handle exceptions appropriately
            e.printStackTrace();
        }
    }

    /**
     * Check and execute scheduled processes every hour end.
     */
    @Schedule(minute = "59", hour = "*", persistent = false)
    public void executeScheduledProcesses() {
        try {
            List<ScheduledProcessConfiguration> configs = configFacade.findByJpql(
                    "select c from ScheduledProcessConfiguration c where c.retired=false",
                    new HashMap<>(), TemporalType.TIMESTAMP);
            Date now = new Date();
            for (ScheduledProcessConfiguration c : configs) {
                if (c.getNextSupposedAt() != null
                        && !c.getNextSupposedAt().after(now)
                        && (c.getLastProcessCompleted() == null || c.getLastProcessCompleted())) {
                    scheduledProcessService.executeScheduledProcess(c);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
