package com.divudi.service.inward;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.Title;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Person;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guardian and NIC rules for inward admissions. (Issue #25032)
 */
public class AdmissionGuardianRulesTest {

    private static final String GUARDIAN_REQUIRED = "Guardian Details Required in Patient Admission";
    private static final String NIC_EITHER = "Inward Admission - Require Patient NIC or Guardian NIC";
    private static final String TRIM = "Inward Admission - Guardian Checks Treat Whitespace-Only Values as Missing";
    private static final String GUARDIAN_NIC_REQUIRED = "Guardian NIC is Required in Patient Admission";

    /** Config stub: keys not set explicitly return the default the caller passed, like the real bean. */
    private static class StubConfig extends ConfigOptionApplicationController {

        final Map<String, Boolean> values = new HashMap<>();

        StubConfig with(String key, boolean value) {
            values.put(key, value);
            return this;
        }

        @Override
        public Boolean getBooleanValueByKey(String key) {
            return getBooleanValueByKey(key, false);
        }

        @Override
        public Boolean getBooleanValueByKey(String key, boolean defaultValue) {
            return values.containsKey(key) ? values.get(key) : defaultValue;
        }
    }

    /** An admission whose guardian has every field filled in. */
    private static Admission completeAdmission() {
        Admission a = new Admission();
        Patient patient = new Patient();
        patient.setPerson(new Person());
        a.setPatient(patient);
        Person g = a.getGuardian();
        g.setTitle(Title.Mr);
        g.setName("Guardian Name");
        g.setNic("123456789V");
        g.setAddress("1 Main Street");
        g.setMobile("0771234567");
        g.setPhone("0112345678");
        a.setGuardianRelationshipToPatient(new Item());
        return a;
    }

    private static Person patientPerson(Admission a) {
        return a.getPatient().getPerson();
    }

    // ---- existing guardian checks keep working --------------------------------

    @Test
    void noKeysSet_nothingIsChecked() {
        Admission a = new Admission();
        assertTrue(AdmissionGuardianRules.validate(a, new StubConfig()).isEmpty());
    }

    @Test
    void guardianRequired_completeGuardianPasses() {
        assertTrue(AdmissionGuardianRules.validate(completeAdmission(),
                new StubConfig().with(GUARDIAN_REQUIRED, true)).isEmpty());
    }

    @Test
    void guardianRequired_reportsEveryMissingFieldTogether() {
        Admission a = completeAdmission();
        a.getGuardian().setName(null);
        a.getGuardian().setNic("");
        a.getGuardian().setAddress(null);
        List<String> errors = AdmissionGuardianRules.validate(a, new StubConfig().with(GUARDIAN_REQUIRED, true));
        assertEquals(List.of("Guardian Name is Required", "Guardian NIC is Required",
                "Guardian Address is Required"), errors);
    }

    @Test
    void homePhoneIsOnlyRequiredWhenItsKeyIsOn() {
        Admission a = completeAdmission();
        a.getGuardian().setPhone(null);
        assertTrue(AdmissionGuardianRules.validate(a, new StubConfig().with(GUARDIAN_REQUIRED, true)).isEmpty());
        assertEquals(List.of("Guardian Home Phone Number is Required"),
                AdmissionGuardianRules.validate(a, new StubConfig().with(GUARDIAN_REQUIRED, true)
                        .with("Guardian Home Phone Number is Required in Patient Admission", true)));
    }

    // ---- whitespace handling --------------------------------------------------

    @Test
    void whitespaceOnlyPassesByDefault_legacyBehaviourUnchanged() {
        Admission a = completeAdmission();
        a.getGuardian().setName(" ");
        a.getGuardian().setNic("   ");
        assertTrue(AdmissionGuardianRules.validate(a, new StubConfig().with(GUARDIAN_REQUIRED, true)).isEmpty());
    }

    @Test
    void whitespaceOnlyIsMissingWhenTrimKeyIsOn() {
        Admission a = completeAdmission();
        a.getGuardian().setName(" ");
        a.getGuardian().setMobile("  ");
        List<String> errors = AdmissionGuardianRules.validate(a,
                new StubConfig().with(GUARDIAN_REQUIRED, true).with(TRIM, true));
        assertEquals(List.of("Guardian Name is Required", "Guardian Mobile Number is Required"), errors);
    }

    // ---- patient NIC, else guardian NIC ---------------------------------------

    @Test
    void nicRuleOff_blankNicsAreNotChecked() {
        assertTrue(AdmissionGuardianRules.validate(new Admission(), new StubConfig()).isEmpty());
    }

    @Test
    void nicRuleOn_bothBlank_blocks() {
        List<String> errors = AdmissionGuardianRules.validate(new Admission(), new StubConfig().with(NIC_EITHER, true));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).startsWith("Patient NIC or Guardian NIC is Required"));
    }

    @Test
    void nicRuleOn_patientNicOnly_passes() {
        Admission a = completeAdmission();
        a.getGuardian().setNic(null);
        patientPerson(a).setNic("901234567V");
        assertTrue(AdmissionGuardianRules.validate(a, new StubConfig().with(NIC_EITHER, true)).isEmpty());
    }

    @Test
    void nicRuleOn_guardianNicOnly_passes() {
        Admission a = completeAdmission();
        patientPerson(a).setNic(null);
        assertTrue(AdmissionGuardianRules.validate(a, new StubConfig().with(NIC_EITHER, true)).isEmpty());
    }

    @Test
    void nicRuleOn_whitespaceNicsCountAsMissing_evenWithTrimKeyOff() {
        Admission a = completeAdmission();
        a.getGuardian().setNic("  ");
        patientPerson(a).setNic(" ");
        assertEquals(1, AdmissionGuardianRules.validate(a, new StubConfig().with(NIC_EITHER, true)).size());
    }

    @Test
    void nicRuleOn_worksWithoutTheGuardianMasterKey() {
        // Guardian Details Required is off: no per-field checks, but the NIC rule still runs.
        List<String> errors = AdmissionGuardianRules.validate(new Admission(), new StubConfig().with(NIC_EITHER, true));
        assertEquals(1, errors.size());
    }

    @Test
    void nicRuleOn_patientWithoutPersonDoesNotThrow() {
        Admission a = completeAdmission();
        a.setPatient(null);
        assertTrue(AdmissionGuardianRules.validate(a, new StubConfig().with(NIC_EITHER, true)).isEmpty());
        a.getGuardian().setNic(null);
        assertEquals(1, AdmissionGuardianRules.validate(a, new StubConfig().with(NIC_EITHER, true)).size());
    }

    @Test
    void guardianNicRequiredKeyStillDemandsGuardianNic_evenWhenPatientHasOne() {
        // The new rule does not relax the existing per-field key; hospitals that want
        // "either one" turn Guardian NIC is Required off (documented).
        Admission a = completeAdmission();
        a.getGuardian().setNic(null);
        patientPerson(a).setNic("901234567V");
        assertEquals(List.of("Guardian NIC is Required"), AdmissionGuardianRules.validate(a,
                new StubConfig().with(GUARDIAN_REQUIRED, true).with(NIC_EITHER, true)));
        assertTrue(AdmissionGuardianRules.validate(a, new StubConfig().with(GUARDIAN_REQUIRED, true)
                .with(NIC_EITHER, true).with(GUARDIAN_NIC_REQUIRED, false)).isEmpty());
    }

    @Test
    void nullArguments_returnNoErrors() {
        assertTrue(AdmissionGuardianRules.validate(null, new StubConfig()).isEmpty());
        assertTrue(AdmissionGuardianRules.validate(new Admission(), null).isEmpty());
    }
}
