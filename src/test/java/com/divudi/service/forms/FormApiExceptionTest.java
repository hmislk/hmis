package com.divudi.service.forms;

import com.divudi.core.entity.WebUser;
import javax.ejb.ApplicationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FormApiExceptionTest {

    @Test
    public void formApiExceptionHasApplicationExceptionAnnotation() {
        ApplicationException ann = FormApiException.class.getAnnotation(ApplicationException.class);
        assertNotNull(ann, "FormApiException must be annotated with @ApplicationException");
        assertTrue(ann.rollback(), "@ApplicationException rollback must be true");
    }

    @Test
    public void createFormTemplateWithNullNameThrowsFormApiException() {
        FormApiService service = new FormApiService();
        WebUser user = new WebUser();
        user.setId(1L);

        FormApiException ex = assertThrows(
                FormApiException.class,
                () -> service.createFormTemplate(null, "desc", "css", user)
        );
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("name is required"));
    }
}
