package com.divudi.ejb;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.BillNumber;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillNumberFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import javax.persistence.TemporalType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Inward deposit / payment / post-final payment bill numbers (issue #23986).
 */
public class InwardPaymentBillNumberGeneratorTest {

    private static final String UNIQUE_PER_ADMISSION_TYPE = "Bill Number Generation Strategy - Unique Serial Per Admission Type for Inward Payments";
    private static final String OMIT_YEAR = "Inward Payment Bill Numbers - Omit Year";
    private static final String OMIT_ADMISSION_TYPE_CODE = "Inward Payment Bill Numbers - Omit Admission Type Code";
    private static final String ADD_INSTITUTION_CODE = "Add the Institution Code to the Bill Number Generator";

    private static class StubConfig extends ConfigOptionApplicationController {

        final Map<String, Boolean> booleans = new HashMap<>();
        final Map<String, String> texts = new HashMap<>();

        @Override
        public Boolean getBooleanValueByKey(String key, boolean defaultValue) {
            return booleans.getOrDefault(key, defaultValue);
        }

        @Override
        public String getLongTextValueByKey(String key, String defaultValue) {
            return texts.getOrDefault(key, defaultValue);
        }

        @Override
        public String getShortTextValueByKey(String key, String defaultValue) {
            return texts.getOrDefault(key, defaultValue);
        }

        @Override
        public Integer getIntegerValueByKey(String key, Integer defaultValue) {
            return defaultValue;
        }
    }

    /**
     * Holds BillNumber rows in memory and answers the counter queries of the
     * inward payment generator by matching their parameters and null filters.
     */
    private static class InMemoryBillNumberFacade extends BillNumberFacade {

        final List<BillNumber> rows = new ArrayList<>();

        @Override
        protected EntityManager getEntityManager() {
            return null;
        }

        @Override
        public BillNumber findFreshByJpql(String jpql, Map<String, Object> parameters) {
            for (BillNumber row : rows) {
                if (matches(row, jpql, parameters) && parameters.get("yr").equals(row.getBillYear())) {
                    return row;
                }
            }
            return null;
        }

        @Override
        public long findLongByJpql(String jpql, Map<String, Object> parameters) {
            int year = (Integer) parameters.get("yr");
            long max = 0L;
            for (BillNumber row : rows) {
                if (matches(row, jpql, parameters) && row.getBillYear() != null && row.getBillYear() < year) {
                    max = Math.max(max, row.getLastBillNumber());
                }
            }
            return max;
        }

        @Override
        public void createAndFlush(BillNumber entity) {
            rows.add(entity);
        }

        @Override
        public void editAndFlush(BillNumber entity) {
            // rows are held by reference
        }

        private boolean matches(BillNumber row, String jpql, Map<String, Object> p) {
            if (row.getBillTypeAtomic() != p.get("bTp")) {
                return false;
            }
            if (p.containsKey("dep") && row.getDepartment() != p.get("dep")) {
                return false;
            }
            if (p.containsKey("ins") && row.getInstitution() != p.get("ins")) {
                return false;
            }
            if (p.containsKey("admType")) {
                if (row.getAdmissionType() != p.get("admType")) {
                    return false;
                }
            } else if (jpql.contains("b.admissionType is null") && row.getAdmissionType() != null) {
                return false;
            }
            if (jpql.contains("b.department is null") && row.getDepartment() != null) {
                return false;
            }
            return !(jpql.contains("b.toDepartment is null") && row.getToDepartment() != null);
        }
    }

    private static class StubBillFacade extends BillFacade {

        @Override
        protected EntityManager getEntityManager() {
            return null;
        }

        @Override
        public Long findAggregateLong(String jpql, Map<String, Object> parameters, TemporalType tt) {
            return 0L;
        }
    }

    /**
     * The pre-existing yearly generators are stubbed so the tests can tell
     * when the inward payment generator hands over to them unchanged.
     */
    private static class GeneratorWithExistingPathsStubbed extends BillNumberGenerator {

        @Override
        public String departmentBillNumberGeneratorYearly(Department dep, BillTypeAtomic billType) {
            return "EXISTING-DEPT";
        }

        @Override
        public String departmentBillNumberGeneratorYearly(Department dep, BillTypeAtomic billType, AdmissionType admissionType) {
            return "EXISTING-DEPT-" + admissionType.getCode();
        }

        @Override
        public String institutionBillNumberGeneratorYearly(Institution ins, BillTypeAtomic billType) {
            return "EXISTING-INS";
        }

        @Override
        public String institutionBillNumberGeneratorYearly(Institution ins, BillTypeAtomic billType, AdmissionType admissionType) {
            return "EXISTING-INS-" + admissionType.getCode();
        }
    }

    private StubConfig config;
    private InMemoryBillNumberFacade billNumberFacade;
    private Institution institution;
    private Department department;
    private AdmissionType bht;
    private AdmissionType opdCard;
    private String yy;
    private int currentYear;

    @BeforeEach
    public void setUp() {
        config = new StubConfig();
        billNumberFacade = new InMemoryBillNumberFacade();

        institution = new Institution();
        institution.setId(1L);
        institution.setInstitutionCode("CO");

        department = new Department();
        department.setId(10L);
        department.setDepartmentCode("IW");
        department.setInstitution(institution);

        bht = new AdmissionType();
        bht.setId(100L);
        bht.setCode("BHT");

        opdCard = new AdmissionType();
        opdCard.setId(101L);
        opdCard.setCode("OC");

        config.texts.put("Bill Number Suffix for " + BillTypeAtomic.INWARD_DEPOSIT, "DE");
        config.texts.put("Bill Number Suffix for " + BillTypeAtomic.INWARD_PAYMENT, "DIS");
        config.texts.put("Bill Number Suffix for " + BillTypeAtomic.POST_FINAL_BILL_INWARD_PAYMENT, "FP");
        config.booleans.put(ADD_INSTITUTION_CODE, false);

        currentYear = Calendar.getInstance().get(Calendar.YEAR);
        yy = String.format("%02d", currentYear % 100);
    }

    private BillNumberGenerator wire(BillNumberGenerator generator) throws Exception {
        generator.configOptionApplicationController = config;
        generator.billNumberFacade = billNumberFacade;
        Field billFacade = BillNumberGenerator.class.getDeclaredField("billFacade");
        billFacade.setAccessible(true);
        billFacade.set(generator, new StubBillFacade());
        return generator;
    }

    @Test
    @DisplayName("New options off: numbers come from the existing yearly generators")
    public void optionsOffUseExistingGenerators() throws Exception {
        BillNumberGenerator generator = wire(new GeneratorWithExistingPathsStubbed());

        assertEquals("EXISTING-DEPT", generator.departmentInwardPaymentBillNumberGenerator(department, BillTypeAtomic.INWARD_DEPOSIT, bht));
        assertEquals("EXISTING-INS", generator.institutionInwardPaymentBillNumberGenerator(institution, BillTypeAtomic.INWARD_DEPOSIT, bht));

        config.booleans.put(UNIQUE_PER_ADMISSION_TYPE, true);
        assertEquals("EXISTING-DEPT-BHT", generator.departmentInwardPaymentBillNumberGenerator(department, BillTypeAtomic.INWARD_DEPOSIT, bht));
        assertEquals("EXISTING-INS-BHT", generator.institutionInwardPaymentBillNumberGenerator(institution, BillTypeAtomic.INWARD_DEPOSIT, bht));
        // No admission type on the encounter: the admission-type setting does not apply.
        assertEquals("EXISTING-DEPT", generator.departmentInwardPaymentBillNumberGenerator(department, BillTypeAtomic.INWARD_DEPOSIT, null));

        // Omitting the admission type code alone does not change a number that carries no code.
        config.booleans.put(UNIQUE_PER_ADMISSION_TYPE, false);
        config.booleans.put(OMIT_ADMISSION_TYPE_CODE, true);
        assertEquals("EXISTING-DEPT", generator.departmentInwardPaymentBillNumberGenerator(department, BillTypeAtomic.INWARD_DEPOSIT, bht));
    }

    @Test
    @DisplayName("Both options on: IW/DE/000001, one series per bill type shared by all admission types")
    public void bothOptionsGiveShortNumbersWithOneSeriesPerBillType() throws Exception {
        BillNumberGenerator generator = wire(new BillNumberGenerator());
        config.booleans.put(UNIQUE_PER_ADMISSION_TYPE, true);
        config.booleans.put(OMIT_YEAR, true);
        config.booleans.put(OMIT_ADMISSION_TYPE_CODE, true);

        assertEquals("IW/DE/000001", generator.departmentInwardPaymentBillNumberGenerator(department, BillTypeAtomic.INWARD_DEPOSIT, bht));
        assertEquals("IW/DE/000002", generator.departmentInwardPaymentBillNumberGenerator(department, BillTypeAtomic.INWARD_DEPOSIT, opdCard));
        assertEquals("IW/DIS/000001", generator.departmentInwardPaymentBillNumberGenerator(department, BillTypeAtomic.INWARD_PAYMENT, bht));
        assertEquals("IW/FP/000001", generator.departmentInwardPaymentBillNumberGenerator(department, BillTypeAtomic.POST_FINAL_BILL_INWARD_PAYMENT, opdCard));

        assertEquals("DE/000001", generator.institutionInwardPaymentBillNumberGenerator(institution, BillTypeAtomic.INWARD_DEPOSIT, bht));
        assertEquals("DE/000002", generator.institutionInwardPaymentBillNumberGenerator(institution, BillTypeAtomic.INWARD_DEPOSIT, opdCard));
    }

    @Test
    @DisplayName("Institution code on: prefixes both numbers as before")
    public void institutionCodePrefixesNumbers() throws Exception {
        BillNumberGenerator generator = wire(new BillNumberGenerator());
        config.booleans.put(ADD_INSTITUTION_CODE, true);
        config.booleans.put(OMIT_YEAR, true);

        assertEquals("COIW/DE/000001", generator.departmentInwardPaymentBillNumberGenerator(department, BillTypeAtomic.INWARD_DEPOSIT, bht));
        assertEquals("CO/DE/000001", generator.institutionInwardPaymentBillNumberGenerator(institution, BillTypeAtomic.INWARD_DEPOSIT, bht));
    }

    @Test
    @DisplayName("Omit year only: admission type code and a series per admission type are kept")
    public void omitYearKeepsAdmissionTypeSeries() throws Exception {
        BillNumberGenerator generator = wire(new BillNumberGenerator());
        config.booleans.put(UNIQUE_PER_ADMISSION_TYPE, true);
        config.booleans.put(OMIT_YEAR, true);

        assertEquals("IW/DE/BHT/000001", generator.departmentInwardPaymentBillNumberGenerator(department, BillTypeAtomic.INWARD_DEPOSIT, bht));
        assertEquals("IW/DE/OC/000001", generator.departmentInwardPaymentBillNumberGenerator(department, BillTypeAtomic.INWARD_DEPOSIT, opdCard));
        assertEquals("IW/DE/BHT/000002", generator.departmentInwardPaymentBillNumberGenerator(department, BillTypeAtomic.INWARD_DEPOSIT, bht));
        assertEquals("DE/OC/000001", generator.institutionInwardPaymentBillNumberGenerator(institution, BillTypeAtomic.INWARD_DEPOSIT, opdCard));
    }

    @Test
    @DisplayName("Omit admission type code only: year kept, one series per bill type")
    public void omitAdmissionTypeCodeKeepsYear() throws Exception {
        BillNumberGenerator generator = wire(new BillNumberGenerator());
        config.booleans.put(UNIQUE_PER_ADMISSION_TYPE, true);
        config.booleans.put(OMIT_ADMISSION_TYPE_CODE, true);

        assertEquals("IW/DE/" + yy + "/000001", generator.departmentInwardPaymentBillNumberGenerator(department, BillTypeAtomic.INWARD_DEPOSIT, bht));
        assertEquals("IW/DE/" + yy + "/000002", generator.departmentInwardPaymentBillNumberGenerator(department, BillTypeAtomic.INWARD_DEPOSIT, opdCard));
        assertEquals("DE/" + yy + "/000001", generator.institutionInwardPaymentBillNumberGenerator(institution, BillTypeAtomic.INWARD_DEPOSIT, bht));
    }

    @Test
    @DisplayName("Year omitted: the serial carries on from earlier years instead of restarting")
    public void omittedYearSerialDoesNotRestart() throws Exception {
        BillNumberGenerator generator = wire(new BillNumberGenerator());
        config.booleans.put(UNIQUE_PER_ADMISSION_TYPE, true);
        config.booleans.put(OMIT_YEAR, true);
        config.booleans.put(OMIT_ADMISSION_TYPE_CODE, true);
        billNumberFacade.rows.add(departmentRow(currentYear - 2, 400L));
        billNumberFacade.rows.add(departmentRow(currentYear - 1, 950L));
        // This year's counter already exists but is behind last year's.
        billNumberFacade.rows.add(departmentRow(currentYear, 30L));

        assertEquals("IW/DE/000951", generator.departmentInwardPaymentBillNumberGenerator(department, BillTypeAtomic.INWARD_DEPOSIT, bht));
        assertEquals("IW/DE/000952", generator.departmentInwardPaymentBillNumberGenerator(department, BillTypeAtomic.INWARD_DEPOSIT, opdCard));
    }

    @Test
    @DisplayName("Year kept: the serial still restarts every year")
    public void keptYearSerialRestarts() throws Exception {
        BillNumberGenerator generator = wire(new BillNumberGenerator());
        config.booleans.put(UNIQUE_PER_ADMISSION_TYPE, true);
        config.booleans.put(OMIT_ADMISSION_TYPE_CODE, true);
        billNumberFacade.rows.add(departmentRow(currentYear - 1, 950L));

        assertEquals("IW/DE/" + yy + "/000001", generator.departmentInwardPaymentBillNumberGenerator(department, BillTypeAtomic.INWARD_DEPOSIT, bht));
    }

    @Test
    @DisplayName("Admission-type setting off with year omitted: dedicated series per bill type")
    public void omitYearWithoutAdmissionTypeSetting() throws Exception {
        BillNumberGenerator generator = wire(new BillNumberGenerator());
        config.booleans.put(OMIT_YEAR, true);

        assertEquals("IW/DE/000001", generator.departmentInwardPaymentBillNumberGenerator(department, BillTypeAtomic.INWARD_DEPOSIT, bht));
        assertEquals("IW/DE/000002", generator.departmentInwardPaymentBillNumberGenerator(department, BillTypeAtomic.INWARD_DEPOSIT, null));
        assertEquals("IW/DIS/000001", generator.departmentInwardPaymentBillNumberGenerator(department, BillTypeAtomic.INWARD_PAYMENT, opdCard));
    }

    @Test
    @DisplayName("Institution code not added: institution number has no leading delimiter")
    public void institutionNumberHasNoLeadingDelimiter() throws Exception {
        BillNumberGenerator generator = wire(new BillNumberGenerator());

        assertEquals("DE/" + yy + "/000001", generator.institutionBillNumberGeneratorYearly(institution, BillTypeAtomic.INWARD_DEPOSIT));
        assertEquals("DE/BHT/" + yy + "/000001", generator.institutionBillNumberGeneratorYearly(institution, BillTypeAtomic.INWARD_DEPOSIT, bht));

        config.booleans.put(ADD_INSTITUTION_CODE, true);
        assertEquals("CO/DE/" + yy + "/000002", generator.institutionBillNumberGeneratorYearly(institution, BillTypeAtomic.INWARD_DEPOSIT));
    }

    private BillNumber departmentRow(int year, long lastBillNumber) {
        BillNumber row = new BillNumber();
        row.setBillTypeAtomic(BillTypeAtomic.INWARD_DEPOSIT);
        row.setDepartment(department);
        row.setBillYear(year);
        row.setLastBillNumber(lastBillNumber);
        return row;
    }
}
