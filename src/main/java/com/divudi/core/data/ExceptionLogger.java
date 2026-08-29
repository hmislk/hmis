package com.divudi.core.data;

import java.io.Serializable;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

/**
 * Global Exception Logger
 * Generated with help from ChatGPT by Dr M H B Ariyaratne, buddhika.ari@gmail.com
 * 
 */
@Named
@ApplicationScoped
public class ExceptionLogger implements Serializable {

//    private static final Logger logger = LogManager.getLogger(ExceptionLogger.class);

    public void log(Throwable t) {
        if (t != null) {
//            logger.error("Unhandled Exception Caught:", t);
        } else {
//            logger.error("Throwable is null. Nothing to log.");
        }
    }

    public void log(String message, Throwable t) {
        if (t != null) {
//            logger.error(message, t);
        } else {
//            logger.error(message + " (Exception was null)");
        }
    }
}
