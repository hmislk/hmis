package com.divudi.service.inward;

import javax.ejb.ApplicationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ItemRequestExceptionTest {

    @Test
    public void itemRequestExceptionHasApplicationExceptionAnnotation() {
        ApplicationException ann = ItemRequestException.class.getAnnotation(ApplicationException.class);
        assertNotNull(ann, "ItemRequestException must be annotated with @ApplicationException");
        assertTrue(ann.rollback(), "@ApplicationException rollback must be true");
    }

    @Test
    public void exceptionStoresMessage() {
        ItemRequestException ex = new ItemRequestException("Request is cancelled");
        assertEquals("Request is cancelled", ex.getMessage());
        assertTrue(ex instanceof RuntimeException);
    }
}

