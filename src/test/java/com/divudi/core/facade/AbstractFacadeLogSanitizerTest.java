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
    public void redactBoundValues_removesMultiLineBindValue() {
        String result = AbstractFacade.redactBoundValues(
                "Error Code: 1054\nCall: SELECT 1 FROM T WHERE A = ?\n\tbind => [a\nsecretline\nmore]\nQuery: ReadAllQuery(x)");
        assertFalse(result.contains("secretline"), result);
        assertFalse(result.contains("more"), result);
        assertTrue(result.contains("Error Code: 1054"), result);
        assertTrue(result.contains("bind => [redacted]"), result);
    }

    @Test
    public void redactBoundValues_removesUnclosedMultiLineBindToEnd() {
        String result = AbstractFacade.redactBoundValues("Error Code: 1\nbind => [a\nsecret");
        assertFalse(result.contains("secret"), result);
        assertEquals("Error Code: 1 / bind => [redacted]", result);
    }

    @Test
    public void redactBoundValues_removesBindValueContainingClosingBracket() {
        String result = AbstractFacade.redactBoundValues(
                "Error Code: 1\nbind => [x], secret]tail]\nQuery: ReadAllQuery(y)");
        assertFalse(result.contains("secret"), result);
        assertFalse(result.contains("tail"), result);
        assertTrue(result.contains("bind => [redacted]"), result);
    }

    @Test
    public void redactBoundValues_removesMultiLineCallAndQueryText() {
        String result = AbstractFacade.redactBoundValues(
                "Error Code: 1\nCall: SELECT 1\nWHERE NAME = 'callsecret'\nQuery: ReadAllQuery(sql=\"x\nWHERE NAME = 'querysecret'\")");
        assertFalse(result.contains("callsecret"), result);
        assertFalse(result.contains("querysecret"), result);
        assertEquals("Error Code: 1 / Call: [redacted] / Query: [redacted]", result);
    }

    @Test
    public void redactBoundValues_removesMultiLineQuotedMysqlValues() {
        String result = AbstractFacade.redactBoundValues(
                "Duplicate entry 'v\nw' for key 'K'\nIncorrect integer value: 'a\r\nb' for column 'AGE'");
        assertFalse(result.contains("v / w"), result);
        assertFalse(result.contains("'v"), result);
        assertFalse(result.contains("w'"), result);
        assertFalse(result.contains("'a"), result);
        assertFalse(result.contains("b'"), result);
        assertEquals("Duplicate entry '[redacted]' for key 'K' / Incorrect integer value: '[redacted]' for column 'AGE'", result);
    }

    @Test
    public void redactBoundValues_removesQuotedValueContainingApostrophe() {
        String result = AbstractFacade.redactBoundValues(
                "Duplicate entry 'O'Brien' for key 'NAME' / Incorrect integer value: 'x'y' for column 'AGE'");
        assertFalse(result.contains("Brien"), result);
        assertFalse(result.contains("y'"), result);
        assertEquals("Duplicate entry '[redacted]' for key 'NAME' / Incorrect integer value: '[redacted]' for column 'AGE'", result);
    }

    @Test
    public void sanitizedJpql_redactsLiteralsAndKeepsOneLine() {
        String result = AbstractFacade.sanitizedJpql(
                "select s from Staff s\n where s.person.name like '%O''Brien%'\n and s.code = 'NIC123' and s.retired=:ret");
        assertFalse(result.contains("Brien"), result);
        assertFalse(result.contains("NIC123"), result);
        assertFalse(result.contains("\n"), result);
        assertEquals("select s from Staff s where s.person.name like '[redacted]' and s.code = '[redacted]' and s.retired=:ret", result);
        assertNull(AbstractFacade.sanitizedJpql(null));
    }

    @Test
    public void sanitizedJpql_redactsLiteralWithUnescapedApostrophe() {
        String result = AbstractFacade.sanitizedJpql(
                "select s from Staff s where s.person.name like '%O'Brien%' and s.code like '%D'Arcy%' and s.retired=:ret");
        assertFalse(result.contains("Brien"), result);
        assertFalse(result.contains("Arcy"), result);
        assertEquals("select s from Staff s where s.person.name like '[redacted]' and s.code like '[redacted]' and s.retired=:ret", result);
    }

    @Test
    public void sanitizedJpql_redactsToEndWhenAQuoteIsLeftOver() {
        String result = AbstractFacade.sanitizedJpql(
                "select s from Staff s where s.person.name = 'O' Brien' and s.retired=:ret");
        assertFalse(result.contains("Brien"), result);
        assertEquals("select s from Staff s where s.person.name = '[redacted]", result);
    }

    private static final String JPQL_PREFIX = "select s from Staff s where s.person.name like ";
    private static final int LONG = 200_000;

    @Test
    public void sanitizedJpql_longLiteralCompletesAndIsRedacted() {
        String result = AbstractFacade.sanitizedJpql(JPQL_PREFIX + "'" + "a".repeat(LONG) + "' and s.retired=:ret");
        assertFalse(result.contains("aaa"), result);
        assertTrue(result.startsWith(JPQL_PREFIX + "'[redacted]"), result);
        assertTrue(result.endsWith(AbstractFacade.TRUNCATED_MARKER), result);
    }

    @Test
    public void sanitizedJpql_longUnclosedLiteralCompletesAndIsRedacted() {
        String result = AbstractFacade.sanitizedJpql(JPQL_PREFIX + "'" + "a".repeat(LONG));
        assertFalse(result.contains("aaa"), result);
        assertEquals(JPQL_PREFIX + "'[redacted]" + AbstractFacade.TRUNCATED_MARKER, result);
    }

    @Test
    public void sanitizedJpql_manyQuotesCompleteAndAreRedacted() {
        String stray = AbstractFacade.sanitizedJpql(JPQL_PREFIX + "'" + "'".repeat(LONG / 2) + "x");
        assertEquals(JPQL_PREFIX + "'[redacted]'" + AbstractFacade.TRUNCATED_MARKER, stray);
        String strayData = AbstractFacade.sanitizedJpql(JPQL_PREFIX + "'x".repeat(LONG / 2));
        assertFalse(strayData.contains("x"), strayData);
        assertTrue(strayData.startsWith(JPQL_PREFIX + "'[redacted]"), strayData);
        String abutting = AbstractFacade.sanitizedJpql(JPQL_PREFIX + "'a'".repeat(66_000));
        assertFalse(abutting.contains("a'"), abutting);
        assertTrue(abutting.startsWith(JPQL_PREFIX + "'[redacted]"), abutting);
        String spaced = AbstractFacade.sanitizedJpql(JPQL_PREFIX + "'secret' or ".repeat(20_000) + "1=1");
        assertFalse(spaced.contains("secret"), spaced);
        // The 4 000-char cut lands inside a literal, so the redact-to-end fallback applies.
        assertEquals(JPQL_PREFIX + "'[redacted]" + AbstractFacade.TRUNCATED_MARKER, spaced);
    }

    @Test
    public void redactBoundValues_longInputCompletesAndIsRedacted() {
        String entry = AbstractFacade.redactBoundValues("Duplicate entry '" + "s".repeat(LONG) + "' for key 'K'");
        assertEquals("Duplicate entry '[redacted]'" + AbstractFacade.TRUNCATED_MARKER, entry);
        String unclosed = AbstractFacade.redactBoundValues("Incorrect integer value: '" + "s".repeat(LONG));
        assertEquals("Incorrect integer value: '[redacted]'" + AbstractFacade.TRUNCATED_MARKER, unclosed);
        String quotes = AbstractFacade.redactBoundValues("value: '" + "s'".repeat(LONG / 2));
        assertTrue(quotes.startsWith("value: '[redacted]'"), quotes);
        String bind = AbstractFacade.redactBoundValues("Error Code: 1\nbind => [" + "s".repeat(LONG) + "]");
        assertEquals("Error Code: 1 / bind => [redacted]" + AbstractFacade.TRUNCATED_MARKER, bind);
    }

    @Test
    public void sanitizedCauseChain_fallsBackWhenSanitizingThrows() {
        RuntimeException broken = new RuntimeException() {
            @Override
            public String getMessage() {
                throw new StackOverflowError();
            }
        };
        assertEquals("[unloggable: java.lang.StackOverflowError]", AbstractFacade.sanitizedCauseChain(broken));
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
