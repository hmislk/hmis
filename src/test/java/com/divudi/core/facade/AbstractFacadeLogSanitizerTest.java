package com.divudi.core.facade;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the swallowed-query-failure log line in AbstractFacade never
 * carries bound parameter values (issue #25075, review on PR #25080).
 */
public class AbstractFacadeLogSanitizerTest {

    private static final String ECLIPSELINK_MESSAGE
            = "\nInternal Exception: java.sql.SQLSyntaxErrorException: Unknown column 't0.TWOFACTORENABLED' in 'field list'\n"
            + "Error Code: 1054\n"
            + "Call: SELECT t0.ID, t0.NAME FROM WEBUSER t0 WHERE ((t0.NAME = ?) AND (t0.WEBUSERPASSWORD = ?))\n"
            + "\tbind => [secretuser, $2a$10$hashvalue]\n"
            + "Query: ReadAllQuery(referenceClass=WebUser sql=\"SELECT t0.ID FROM WEBUSER t0 WHERE (t0.NAME = 'secretuser')\")";

    @Test
    public void redactBoundValues_removesBindCallAndQueryText() {
        String result = AbstractFacade.redactBoundValues(ECLIPSELINK_MESSAGE);
        assertFalse(result.contains("secretuser"), result);
        assertFalse(result.contains("hashvalue"), result);
        assertTrue(result.contains("bind => [redacted]"), result);
        assertTrue(result.contains("Query: [redacted]"), result);
        assertTrue(result.contains("Unknown column 't0.TWOFACTORENABLED' in 'field list'"), result);
        assertFalse(result.contains("\n"), result);
    }

    @Test
    public void redactBoundValues_removesQuotedMysqlValues() {
        String result = AbstractFacade.redactBoundValues(
                "Duplicate entry 'patient-nic-123' for key 'NIC' / Incorrect integer value: 'abc' for column 'AGE'");
        assertFalse(result.contains("patient-nic-123"), result);
        assertFalse(result.contains("'abc'"), result);
        assertTrue(result.contains("for key 'NIC'"), result);
    }

    @Test
    public void redactBoundValues_handlesNull() {
        assertNull(AbstractFacade.redactBoundValues(null));
    }

    @Test
    public void sanitizedCauseChain_listsClassesAndSanitizedMessages() {
        SQLException root = new SQLException("Unknown column 'X' in 'field list'");
        RuntimeException top = new RuntimeException(ECLIPSELINK_MESSAGE, root);
        String chain = AbstractFacade.sanitizedCauseChain(top);
        assertTrue(chain.startsWith("java.lang.RuntimeException: "), chain);
        assertTrue(chain.contains(" <- java.sql.SQLException: Unknown column 'X' in 'field list'"), chain);
        assertFalse(chain.contains("secretuser"), chain);
    }

    @Test
    public void sanitizedCauseChain_stopsOnCycle() {
        RuntimeException a = new RuntimeException("a");
        RuntimeException b = new RuntimeException("b", a);
        a.initCause(b);
        String chain = AbstractFacade.sanitizedCauseChain(b);
        assertEquals("java.lang.RuntimeException: b <- java.lang.RuntimeException: a", chain);
    }
}
