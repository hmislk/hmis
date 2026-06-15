package com.divudi.service;

import java.time.ZonedDateTime;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;

/**
 * Singleton EJB that captures the application startup time on first access.
 * The @Startup annotation was removed to avoid a Payara 5 / Weld 3 CDI context
 * race condition where the @Dependent scope is not yet active when the container
 * tries to create lifecycle interceptors for the bean during early startup.
 *
 * @author L C J Samarasekara <lawan.chaamindu1234@gmail.com>
 */
@Singleton
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
