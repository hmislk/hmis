package com.divudi.service;

import java.time.ZonedDateTime;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * Eager singleton EJB that captures the actual application startup time.
 * This bean is instantiated immediately when the application starts (not lazily),
 * ensuring the recorded startup time reflects the actual server restart or deployment time.
 *
 * @author L C J Samarasekara <lawan.chaamindu1234@gmail.com>
 */
@Singleton
@Startup
public class ApplicationStartupTimeService {

    private ZonedDateTime startupTime;

    @PostConstruct
    public void init() {
        // Capture the startup time immediately when the application starts
        startupTime = ZonedDateTime.now();
    }

    /**
     * Gets the recorded application startup time
     * @return The ZonedDateTime when the application started
     */
    public ZonedDateTime getStartupTime() {
        return startupTime;
    }
}
