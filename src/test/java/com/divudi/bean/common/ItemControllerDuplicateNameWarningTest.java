package com.divudi.bean.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers issue #24197 - saving an item whose name matches an existing item
 * once case, spaces and punctuation are ignored produces a warning that lists
 * the matching items with their codes.
 *
 * <p>These tests exercise the real name-matching and message-building code.
 * The JPQL that fetches the other items of the same type needs a database and
 * is not covered here.</p>
 */
class ItemControllerDuplicateNameWarningTest {

    private static Object[] item(String name, String code) {
        return new Object[]{name, code};
    }

    private static List<Object[]> items(Object[]... rows) {
        return new ArrayList<>(Arrays.asList(rows));
    }

    private static void assertSameName(String a, String b) {
        assertEquals(ItemController.normalizeItemName(a), ItemController.normalizeItemName(b),
                "'" + a + "' and '" + b + "' should count as the same name");
    }

    private static void assertDifferentName(String a, String b) {
        assertNotEquals(ItemController.normalizeItemName(a), ItemController.normalizeItemName(b),
                "'" + a + "' and '" + b + "' should count as different names");
    }

    // ---- normalizeItemName ------------------------------------------------

    @Test
    void namesDifferingOnlyInCaseSpacingOrPunctuationAreTheSame() {
        assertSameName("ESR", "ESR ");
        assertSameName("Blood Urea", "BLOOD UREA");
        assertSameName("Blood Urea", "  blood   urea ");
        assertSameName("Blood Urea", "blood-urea.");
        assertSameName("E.S.R.", "ESR");
        assertSameName("Vit. B-12", "vit b12");
        assertSameName("Paracetamol (500mg)", "PARACETAMOL 500mg");
        assertSameName("1,000 IU", "1000 IU");
    }

    @Test
    void genuinelyDifferentNamesAreNotTheSame() {
        assertDifferentName("ESR", "ESR Test");
        assertDifferentName("Blood Urea", "Blood Sugar");
        assertDifferentName("Paracetamol 500mg", "Paracetamol 650mg");
    }

    @Test
    void decimalPointInsideANumberIsSignificant() {
        // 2.5 mg and 25 mg are different strengths, not a punctuation typo.
        assertDifferentName("Methotrexate 2.5mg", "Methotrexate 25mg");
        assertDifferentName("Levothyroxine 12.5mcg", "Levothyroxine 125mcg");
        assertSameName("Methotrexate 2.5mg", "METHOTREXATE 2.5 mg");
        // A full stop that is not between two digits is still ordinary punctuation.
        assertSameName("Tab. Aspirin", "Tab Aspirin");
        assertSameName("Aspirin 75mg.", "Aspirin 75mg");
    }

    @Test
    void lettersOutsideAsciiAreKeptSoDifferentNamesStayDifferent() {
        assertDifferentName("Vitamin B12 α", "Vitamin B12 β");
        // Sinhala: same word with different spacing matches, a different word does not.
        assertSameName("පැරසිටමෝල්", " පැරසිටමෝල් ");
        assertDifferentName("පැරසිටමෝල්", "පැරසි");
        assertFalse(ItemController.normalizeItemName("පැරසිටමෝල්").isEmpty());
    }

    @Test
    void composedAndDecomposedAccentsAreTheSame() {
        assertSameName("Café Tablet", "Café Tablet");
    }

    @Test
    void nameWithNothingToCompareGivesEmptyKey() {
        assertEquals("", ItemController.normalizeItemName(null));
        assertEquals("", ItemController.normalizeItemName(""));
        assertEquals("", ItemController.normalizeItemName("   "));
        assertEquals("", ItemController.normalizeItemName("-.-"));
    }

    @Test
    void caseFoldingDoesNotDependOnTheServerLocale() {
        Locale original = Locale.getDefault();
        try {
            // In a Turkish locale "I".toLowerCase() is a dotless i, so "TITLE" would not match "title".
            Locale.setDefault(new Locale("tr", "TR"));
            assertEquals("title", ItemController.normalizeItemName("TITLE"));
            assertSameName("TITLE", "title");
        } finally {
            Locale.setDefault(original);
        }
    }

    // ---- buildDuplicateItemNameWarning -------------------------------------

    @Test
    void noWarningWhenNoOtherItemHasTheSameName() {
        assertNull(ItemController.buildDuplicateItemNameWarning("ESR",
                items(item("ESR Test", "L001"), item("Blood Urea", "L002"))));
    }

    @Test
    void noWarningWhenThereAreNoOtherItems() {
        assertNull(ItemController.buildDuplicateItemNameWarning("ESR", items()));
        assertNull(ItemController.buildDuplicateItemNameWarning("ESR", Collections.<Object[]>emptyList()));
        assertNull(ItemController.buildDuplicateItemNameWarning("ESR", null));
    }

    @Test
    void noWarningWhenTheNewNameHasNothingToCompare() {
        assertNull(ItemController.buildDuplicateItemNameWarning(null, items(item("ESR", "L001"))));
        assertNull(ItemController.buildDuplicateItemNameWarning("  ", items(item("", "L001"), item(null, "L002"))));
        assertNull(ItemController.buildDuplicateItemNameWarning("--", items(item("-", "L001"))));
    }

    @Test
    void warningNamesTheMatchingItemAndItsCode() {
        String warning = ItemController.buildDuplicateItemNameWarning("ESR ",
                items(item("ESR", "LAB-014"), item("Blood Urea", "LAB-020")));
        assertNotNull(warning);
        assertTrue(warning.contains("ESR (code LAB-014)"), warning);
        assertFalse(warning.contains("Blood Urea"), warning);
        assertFalse(warning.contains("more"), warning);
    }

    @Test
    void warningListsEveryMatchWhenThereAreFew() {
        String warning = ItemController.buildDuplicateItemNameWarning("Blood Urea",
                items(item("BLOOD UREA", "A1"), item("blood-urea", "A2"), item("Blood  Urea.", "A3")));
        assertTrue(warning.contains("BLOOD UREA (code A1)"), warning);
        assertTrue(warning.contains("blood-urea (code A2)"), warning);
        assertTrue(warning.contains("Blood  Urea. (code A3)"), warning);
        assertFalse(warning.contains("more"), warning);
    }

    @Test
    void warningCapsTheListAndCountsTheRest() {
        String warning = ItemController.buildDuplicateItemNameWarning("ESR",
                items(item("ESR", "C1"), item("esr", "C2"), item("E.S.R", "C3"), item("E S R", "C4"), item("ESR-", "C5")));
        assertTrue(warning.contains("(code C1)"), warning);
        assertTrue(warning.contains("(code C3)"), warning);
        assertFalse(warning.contains("(code C4)"), warning);
        assertFalse(warning.contains("(code C5)"), warning);
        assertTrue(warning.contains("and 2 more"), warning);
    }

    @Test
    void warningOmitsMissingOrBlankCodesAndTrimsTheName() {
        String warning = ItemController.buildDuplicateItemNameWarning("ESR",
                items(item("  ESR  ", null), item("esr", "   ")));
        assertNotNull(warning);
        assertTrue(warning.contains("ESR, esr."), warning);
        assertFalse(warning.contains("code"), warning);
    }

    @Test
    void warningIgnoresItemsWhoseNameIsNull() {
        assertNull(ItemController.buildDuplicateItemNameWarning("ESR", items(item(null, "L001"))));
    }
}
