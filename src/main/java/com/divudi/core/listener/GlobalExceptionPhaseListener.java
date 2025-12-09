package com.divudi.core.listener;

import com.divudi.core.data.ExceptionLogger;
import javax.enterprise.inject.spi.CDI;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import java.io.Serializable;

/**
 * Logs all uncaught exceptions globally
 * Added by Dr M H B Ariyaratne (buddhika.ari@gamil.com) with support from ChatGPT
 */
public class GlobalExceptionPhaseListener implements PhaseListener, Serializable {

    @Override
    public void afterPhase(PhaseEvent event) {
        Throwable exception = (Throwable) event.getFacesContext()
                .getExternalContext()
                .getRequestMap()
                .get("javax.servlet.error.exception");

        if (exception != null) {
            try {
                ExceptionLogger logger = CDI.current().select(ExceptionLogger.class).get();
                logger.log(exception);
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void beforePhase(PhaseEvent event) {
    }

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.RENDER_RESPONSE;
    }
}

