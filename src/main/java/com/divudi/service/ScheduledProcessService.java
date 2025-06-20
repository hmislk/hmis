package com.divudi.service;

import com.divudi.core.data.HistoricalRecordType;
import com.divudi.core.data.ScheduledFrequency;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.HistoricalRecord;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.ScheduledProcessConfiguration;
import com.divudi.core.facade.ScheduledProcessConfigurationFacade;
import com.divudi.service.HistoricalRecordService;
import com.divudi.service.StockService;
import com.divudi.core.data.StockValueRow;
import java.util.Calendar;
import java.util.Date;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class ScheduledProcessService {

    @EJB
    private ScheduledProcessConfigurationFacade configFacade;

    @EJB
    private HistoricalRecordService historicalRecordService;

    @EJB
    private StockService stockService;

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

        processScheduledTask(config);

        // Mark end
        config.setLastRunEnded(new Date());
        config.setLastProcessCompleted(true);
        configFacade.edit(config);
    }

    @Asynchronous
    public void executeScheduledProcessManual(ScheduledProcessConfiguration config) {
        executeScheduledProcessManual(config, false);
    }

    @Asynchronous
    public void executeScheduledProcessManual(ScheduledProcessConfiguration config, boolean runNext) {
        if (config == null) {
            return;
        }
        config.setLastProcessCompleted(false);
        config.setLastRunStarted(new Date());
        if (runNext) {
            config.setLastSupposedAt(config.getNextSupposedAt());
            config.setNextSupposedAt(calculateNextSupposedAt(config.getScheduledFrequency(), config.getNextSupposedAt()));
        }
        configFacade.edit(config);

        processScheduledTask(config);

        config.setLastRunEnded(new Date());
        config.setLastProcessCompleted(true);
        configFacade.edit(config);
    }

    private void recordPharmacyStockValues(ScheduledProcessConfiguration config) {
        if (config == null) {
            return;
        }
        Institution ins = config.getInstitution();
        Department dep = config.getDepartment();
        Institution site = config.getSite();
        if (dep != null) {
            site = dep.getSite();
            if (ins == null) {
                ins = dep.getInstitution();
            }
        }

        StockValueRow values = stockService.calculateStockValues(ins, site, dep);

        createHistoricalRecord(HistoricalRecordType.PHARMACY_STOCK_VALUE_PURCHASE_RATE,
                values != null ? values.getPurchaseValue() : 0.0,
                ins, site, dep);

        createHistoricalRecord(HistoricalRecordType.PHARMACY_STOCK_VALUE_RETAIL_RATE,
                values != null ? values.getRetailValue() : 0.0,
                ins, site, dep);

        createHistoricalRecord(HistoricalRecordType.PHARMACY_STOCK_VALUE_COST_RATE,
                values != null ? values.getCostValue() : 0.0,
                ins, site, dep);
    }

    private void createHistoricalRecord(HistoricalRecordType type, Double value,
            Institution institution, Institution site, Department department) {
        HistoricalRecord hr = new HistoricalRecord();
        hr.setHistoricalRecordType(type);
        hr.setInstitution(institution);
        hr.setSite(site);
        hr.setDepartment(department);
        hr.setRecordDate(new Date());
        hr.setRecordDateTime(new Date());
        hr.setRecordValue(value);
        historicalRecordService.createHistoricalRecord(hr);
    }

    private void processScheduledTask(ScheduledProcessConfiguration config) {
        if (config.getScheduledProcess() == null) {
            return;
        }
        switch (config.getScheduledProcess()) {
            case Record_Pharmacy_Stock_Values:
                recordPharmacyStockValues(config);
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
        }
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
                // Move to the last day of the next month from the provided date
                cal.add(Calendar.MONTH, 1); // jump to same day next month (handles month length overflow)
                cal.set(Calendar.DAY_OF_MONTH, 1); // first day of that month
                cal.add(Calendar.MONTH, 1); // move to first day of the following month
                cal.add(Calendar.DAY_OF_MONTH, -1); // step back one day -> last day of next month
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                break;
            case YearEnd:
                cal.add(Calendar.YEAR, 1);
                cal.set(Calendar.MONTH, Calendar.DECEMBER);
                cal.set(Calendar.DAY_OF_MONTH, 31);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                break;
            default:
                cal.add(Calendar.HOUR_OF_DAY, 1);
        }
        return cal.getTime();
    }
}
