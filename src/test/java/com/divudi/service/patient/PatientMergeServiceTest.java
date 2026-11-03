package com.divudi.service.patient;

import com.divudi.core.data.PatientMergeStatus;
import com.divudi.core.entity.PatientMergeRecord;
import com.divudi.core.entity.WebUser;
import javax.ejb.ApplicationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PatientMergeServiceTest {

    @Test
    public void patientMergeExceptionHasApplicationExceptionAnnotation() {
        ApplicationException ann = PatientMergeException.class.getAnnotation(ApplicationException.class);
        assertNotNull(ann, "PatientMergeException must be annotated with @ApplicationException");
        assertTrue(ann.rollback(), "@ApplicationException rollback must be true");
    }

    @Test
    public void unmergeAlreadyReversedThrowsPatientMergeException() {
        PatientMergeService service = new PatientMergeService();
        PatientMergeRecord record = new PatientMergeRecord();
        record.setId(42L);
        record.setStatus(PatientMergeStatus.REVERSED);

        WebUser user = new WebUser();
        user.setId(1L);

        PatientMergeException ex = assertThrows(PatientMergeException.class, () -> service.unmerge(record, user));
        assertTrue(ex.getMessage().contains("already reversed"));
    }
}
