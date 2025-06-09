package com.divudi.service;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;

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

}
