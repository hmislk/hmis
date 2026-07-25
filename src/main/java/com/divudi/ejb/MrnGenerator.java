package com.divudi.ejb;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.MrnNumber;
import com.divudi.core.facade.MrnNumberFacade;
import com.divudi.core.facade.PatientFacade;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * Generates the Medical Record Number (MRN, stored as {@code Patient.code})
 * according to a configurable strategy. Mirrors the locking, counter-entity,
 * and config-option patterns used by {@link BillNumberGenerator}.
 *
 * @author Dr. M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Stateless
public class MrnGenerator {

    // MRN generation strategies
    public static final String MRN_STRATEGY_MANUAL = "MANUAL";
    public static final String MRN_STRATEGY_SERIAL = "SERIAL";
    public static final String MRN_STRATEGY_YEAR_SERIAL = "YEAR_SERIAL";
    public static final String MRN_STRATEGY_INSTITUTION_SERIAL = "INSTITUTION_SERIAL";
    public static final String MRN_STRATEGY_YEAR_INSTITUTION_SERIAL = "YEAR_INSTITUTION_SERIAL";

    // Config option keys
    public static final String CONFIG_KEY_MRN_STRATEGY = "MRN Generation Strategy";
    public static final String CONFIG_KEY_MRN_SERIAL_DIGIT_COUNT = "MRN Serial Digit Count";
    public static final String CONFIG_KEY_MRN_DELIMITER = "MRN Number Delimiter";

    private static final int MAX_SERIAL_DIGITS = 12;

    @EJB
    private MrnNumberFacade mrnNumberFacade;

    @EJB
    private PatientFacade patientFacade;

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    private String getMrnStrategy() {
        String strategy = configOptionApplicationController.getLongTextValueByKey(CONFIG_KEY_MRN_STRATEGY, MRN_STRATEGY_MANUAL);
        if (strategy == null) {
            return MRN_STRATEGY_MANUAL;
        }
        strategy = strategy.trim().toUpperCase();
        if (strategy.isEmpty()) {
            return MRN_STRATEGY_MANUAL;
        }
        switch (strategy) {
            case MRN_STRATEGY_MANUAL:
            case MRN_STRATEGY_SERIAL:
            case MRN_STRATEGY_YEAR_SERIAL:
            case MRN_STRATEGY_INSTITUTION_SERIAL:
            case MRN_STRATEGY_YEAR_INSTITUTION_SERIAL:
                return strategy;
            default:
                return MRN_STRATEGY_MANUAL;
        }
    }

    public boolean isMrnAutoGenerationEnabled() {
        return !MRN_STRATEGY_MANUAL.equals(getMrnStrategy());
    }

    private String formatSerialNumber(Long serialNumber) {
        Integer digits = configOptionApplicationController.getIntegerValueByKey(CONFIG_KEY_MRN_SERIAL_DIGIT_COUNT, 6);
        if (digits == null || digits < 1) {
            digits = 6;
        }
        digits = Math.min(digits, MAX_SERIAL_DIGITS);
        return String.format("%0" + digits + "d", serialNumber);
    }

    private String getMrnDelimiter() {
        String delimiter = configOptionApplicationController.getShortTextValueByKey(CONFIG_KEY_MRN_DELIMITER, "/");
        if (delimiter == null) {
            return "/";
        }
        delimiter = delimiter.trim();
        if (delimiter.isEmpty()) {
            return "/";
        }
        return delimiter;
    }

    /**
     * Fetches (creating and seeding if necessary) the MrnNumber counter row for
     * the given year, or the global (non-year-scoped) counter when year is
     * null. Does not increment — the caller increments under the per-key lock
     * in {@link #generateMrn(Institution)}.
     */
    private MrnNumber fetchLastMrnNumberSynchronized(Integer year) {
        String jpql;
        Map<String, Object> hm = new HashMap<>();
        if (year == null) {
            jpql = "SELECT m FROM MrnNumber m WHERE m.year IS NULL";
        } else {
            jpql = "SELECT m FROM MrnNumber m WHERE m.year = :yr";
            hm.put("yr", year);
        }

        MrnNumber mrnNumber = mrnNumberFacade.findFreshByJpql(jpql, hm);

        if (mrnNumber == null) {
            mrnNumber = new MrnNumber();
            mrnNumber.setYear(year);

            String countJpql;
            Map<String, Object> countParams = new HashMap<>();
            if (year == null) {
                countJpql = "SELECT count(p) FROM Patient p WHERE p.code IS NOT NULL AND p.retired = false";
            } else {
                countJpql = "SELECT count(p) FROM Patient p WHERE p.code IS NOT NULL AND p.retired = false "
                        + "AND p.createdAt BETWEEN :startOfYear AND :endOfYear";

                Calendar startOfYear = Calendar.getInstance();
                startOfYear.set(Calendar.YEAR, year);
                startOfYear.set(Calendar.DAY_OF_YEAR, 1);
                startOfYear.set(Calendar.HOUR_OF_DAY, 0);
                startOfYear.set(Calendar.MINUTE, 0);
                startOfYear.set(Calendar.SECOND, 0);
                startOfYear.set(Calendar.MILLISECOND, 0);

                Calendar endOfYear = Calendar.getInstance();
                endOfYear.set(Calendar.YEAR, year);
                endOfYear.set(Calendar.MONTH, Calendar.DECEMBER);
                endOfYear.set(Calendar.DAY_OF_MONTH, 31);
                endOfYear.set(Calendar.HOUR_OF_DAY, 23);
                endOfYear.set(Calendar.MINUTE, 59);
                endOfYear.set(Calendar.SECOND, 59);
                endOfYear.set(Calendar.MILLISECOND, 999);

                countParams.put("startOfYear", startOfYear.getTime());
                countParams.put("endOfYear", endOfYear.getTime());
            }

            long seed = patientFacade.findLongByJpql(countJpql, countParams);
            mrnNumber.setLastMrnNumber(seed);
            mrnNumberFacade.createAndFlush(mrnNumber);
        }

        return mrnNumber;
    }

    private String getLockKey(boolean yearScoped) {
        return yearScoped ? "YEAR" : "GLOBAL";
    }

    /**
     * Generates the next MRN according to the configured strategy. Uses a
     * per-key (year-scoped vs global) {@link ReentrantLock} rather than a
     * blanket {@code synchronized} on the whole method so unrelated
     * concurrent callers are not serialized against each other.
     */
    public String generateMrn(Institution institution) {
        String strategy = getMrnStrategy();
        boolean yearScoped = MRN_STRATEGY_YEAR_SERIAL.equals(strategy) || MRN_STRATEGY_YEAR_INSTITUTION_SERIAL.equals(strategy);

        String lockKey = getLockKey(yearScoped);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());
        lock.lock();
        try {
            Integer year = yearScoped ? Calendar.getInstance().get(Calendar.YEAR) : null;
            MrnNumber mrnNumber = fetchLastMrnNumberSynchronized(year);

            Long lastMrnNumber = mrnNumber.getLastMrnNumber();
            if (lastMrnNumber == null) {
                lastMrnNumber = 0L;
            }
            Long next = lastMrnNumber + 1;
            mrnNumber.setLastMrnNumber(next);
            mrnNumberFacade.edit(mrnNumber);

            return buildMrn(strategy, institution, next);
        } finally {
            lock.unlock();
        }
    }

    private String buildMrn(String strategy, Institution institution, Long next) {
        String delimiter = getMrnDelimiter();
        String serial = formatSerialNumber(next);
        String twoDigitYear = String.format("%02d", Calendar.getInstance().get(Calendar.YEAR) % 100);
        String institutionCode = institution != null ? institution.getInstitutionCode() : null;
        boolean hasInstitutionCode = institutionCode != null && !institutionCode.trim().isEmpty();

        switch (strategy) {
            case MRN_STRATEGY_YEAR_SERIAL:
                return twoDigitYear + delimiter + serial;
            case MRN_STRATEGY_INSTITUTION_SERIAL:
                if (hasInstitutionCode) {
                    return institutionCode + delimiter + serial;
                }
                return serial;
            case MRN_STRATEGY_YEAR_INSTITUTION_SERIAL:
                if (hasInstitutionCode) {
                    return institutionCode + delimiter + twoDigitYear + delimiter + serial;
                }
                return twoDigitYear + delimiter + serial;
            case MRN_STRATEGY_MANUAL:
            case MRN_STRATEGY_SERIAL:
            default:
                return serial;
        }
    }

}
