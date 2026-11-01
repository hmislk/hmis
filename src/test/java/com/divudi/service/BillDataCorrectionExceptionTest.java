package com.divudi.service;

import com.divudi.core.entity.WebUser;
import java.util.HashMap;
import javax.ejb.ApplicationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BillDataCorrectionExceptionTest {

    @Test
    public void billDataCorrectionExceptionHasApplicationExceptionAnnotation() {
        ApplicationException ann = BillDataCorrectionException.class.getAnnotation(ApplicationException.class);
        assertNotNull(ann, "BillDataCorrectionException must be annotated with @ApplicationException");
        assertTrue(ann.rollback(), "@ApplicationException rollback must be true");
    }

    @Test
    public void correctDataWithMissingTargetTypeThrowsBillDataCorrectionException() {
        BillDataCorrectionService service = new BillDataCorrectionService();
        WebUser user = new WebUser();
        user.setId(1L);

        BillDataCorrectionException ex = assertThrows(
                BillDataCorrectionException.class,
                () -> service.correctData(null, 100L, new HashMap<>(), "audit", "approver", user)
        );
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("targetType is required"));
    }
}
