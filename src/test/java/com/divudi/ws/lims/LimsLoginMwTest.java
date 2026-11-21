package com.divudi.ws.lims;

import org.hl7.fhir.r5.model.OperationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LimsLoginMwTest {

    private Lims lims;

    @BeforeEach
    void setUp() {
        lims = new Lims();
    }

    @Test
    @DisplayName("fhirToJson serializes OperationOutcome to valid FHIR JSON without recursion or error")
    void testFhirToJsonSerialization() {
        OperationOutcome successOutcome = lims.createOperationOutcomeForSuccess("Logged Successfully");
        String successJson = lims.fhirToJson(successOutcome);
        assertNotNull(successJson);
        assertTrue(successJson.contains("OperationOutcome"));
        assertTrue(successJson.contains("Logged Successfully"));

        OperationOutcome failureOutcome = lims.createOperationOutcomeForFailure("Invalid username or password");
        String failureJson = lims.fhirToJson(failureOutcome);
        assertNotNull(failureJson);
        assertTrue(failureJson.contains("OperationOutcome"));
        assertTrue(failureJson.contains("Invalid username or password"));
    }

    @Test
    @DisplayName("OperationOutcome structure and details are properly populated")
    void testOperationOutcomeStructure() {
        OperationOutcome successOutcome = lims.createOperationOutcomeForSuccess("Logged Successfully");
        assertNotNull(successOutcome.getIssue());
        assertFalse(successOutcome.getIssue().isEmpty());
        assertEquals(OperationOutcome.IssueSeverity.INFORMATION, successOutcome.getIssue().get(0).getSeverity());
        assertEquals(OperationOutcome.IssueType.INFORMATIONAL, successOutcome.getIssue().get(0).getCode());
        assertEquals("Logged Successfully", successOutcome.getIssue().get(0).getDetails().getText());

        OperationOutcome failureOutcome = lims.createOperationOutcomeForFailure("Invalid username or password");
        assertNotNull(failureOutcome.getIssue());
        assertFalse(failureOutcome.getIssue().isEmpty());
        assertEquals(OperationOutcome.IssueSeverity.ERROR, failureOutcome.getIssue().get(0).getSeverity());
        assertEquals(OperationOutcome.IssueType.SECURITY, failureOutcome.getIssue().get(0).getCode());
        assertEquals("Invalid username or password", failureOutcome.getIssue().get(0).getDetails().getText());
    }
}
