package com.divudi.service;

import java.time.ZonedDateTime;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

/**
 * CDI ApplicationScoped bean that captures the true application startup time.
 *
 * Uses a CDI @Observes @Initialized(ApplicationScoped.class) observer instead
 * of an EJB @Singleton @Startup, which avoids a Payara 5 / Weld 3 race condition
 * (WELD-001303: No active contexts for @Dependent) that occurs when the EJB
 * container tries to build CDI lifecycle interceptors before the @Dependent
 * scope is active. The CDI @Initialized event fires during CDI startup while
 * all CDI contexts are already active, capturing a time equivalent to true
 * application startup.
 *
 * @author L C J Samarasekara <lawan.chaamindu1234@gmail.com>
 */
@ApplicationScoped
public class ApplicationStartupTimeService {

    private ZonedDateTime startupTime;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object ignored) {
        startupTime = ZonedDateTime.now();
    }

    public ZonedDateTime getStartupTime() {
        return startupTime;
    }
}
