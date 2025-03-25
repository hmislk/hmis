package com.divudi.bean.common;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.util.Date;
import java.time.ZoneId;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 *
 * @author Dr. M H B Ariyaratne
 */
@Named(value = "commonFunctionsController")
@ApplicationScoped
public class CommonFunctionsController {

    /**
     * Creates a new instance of CommonFunctionsController
     */
    public CommonFunctionsController() {
    }

    // TODO : JSF reference
    public Date getEndOfDay() {
        return getEndOfDay(new Date());
    }

    public static Date getEndOfDay(Date date) {
        // Convert Date to LocalDate
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        // Set the time to the end of the day
        LocalDateTime endOfDay = localDate.atTime(23, 59, 59, 999999999);

        // Convert back to Date
        return Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant());
    }
}
