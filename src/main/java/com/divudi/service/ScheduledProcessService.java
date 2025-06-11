package com.divudi.service;

import com.divudi.core.entity.ScheduledProcessConfiguration;
import com.divudi.core.facade.ScheduledProcessConfigurationFacade;
import com.divudi.core.data.ScheduledFrequency;
import java.util.Calendar;
import java.util.Date;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class ScheduledProcessService {

    @EJB
    private ScheduledProcessConfigurationFacade configFacade;

    @Asynchronous
    public void executeScheduledProcess(ScheduledProcessConfiguration config) {
        if (config == null) {
            return;
        }
        // Mark start
        config.setLastProcessCompleted(false);
        config.setLastRunStarted(new Date());
        config.setLastSupposedAt(config.getNextSupposedAt());
        config.setNextSupposedAt(calculateNextSupposedAt(config.getScheduledFrequency(), config.getNextSupposedAt()));
        configFacade.edit(config);

        // Placeholder switch for future implementations
        switch (config.getScheduledProcess()) {
            case Record_Pharmacy_Stock_Values:
                // TODO: implement process
                break;
            case All_Drawer_Balances:
                // TODO: implement process
                break;
            case All_Collection_Centre_Balances:
                // TODO: implement process
                break;
            case All_Credit_Company_Balances:
                // TODO: implement process
                break;
            default:
                break;
        }

        // Mark end
        config.setLastRunEnded(new Date());
        config.setLastProcessCompleted(true);
        configFacade.edit(config);
    }

    public Date calculateNextSupposedAt(ScheduledFrequency frequency, Date from) {
        Calendar cal = Calendar.getInstance();
        if (from != null) {
            cal.setTime(from);
        }
        switch (frequency) {
            case Hourly:
                cal.add(Calendar.HOUR_OF_DAY, 1);
                break;
            case Midnight:
                cal.add(Calendar.DATE, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                break;
            case WeekEnd:
                while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
                    cal.add(Calendar.DATE, 1);
                }
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                break;
            case MonthEnd:
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                cal.add(Calendar.MONTH, 1);
                break;
            case YearEnd:
                cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR));
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                cal.add(Calendar.YEAR, 1);
                break;
            default:
                cal.add(Calendar.HOUR_OF_DAY, 1);
        }
        return cal.getTime();
    }
}
