package com.divudi.core.util;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JsfUtilAmountFilterTest {

    @Test
    void parsesPlainAndThousandsSeparatedNumbers() {
        assertEquals(2500.0, JsfUtil.parseAmountFilter("2500"));
        assertEquals(2500.5, JsfUtil.parseAmountFilter(" 2,500.50 "));
        assertEquals(1500000.0, JsfUtil.parseAmountFilter("1,500,000"));
        assertEquals(-2500.0, JsfUtil.parseAmountFilter("-2500"));
    }

    @Test
    void rejectsTextThatIsNotAPlainAmount() {
        assertNull(JsfUtil.parseAmountFilter(null));
        assertNull(JsfUtil.parseAmountFilter(""));
        assertNull(JsfUtil.parseAmountFilter("abc"));
        assertNull(JsfUtil.parseAmountFilter("NaN"));
        assertNull(JsfUtil.parseAmountFilter("Infinity"));
        assertNull(JsfUtil.parseAmountFilter("12,5"));
        assertNull(JsfUtil.parseAmountFilter("1,500,50"));
        assertNull(JsfUtil.parseAmountFilter("2500d"));
        assertNull(JsfUtil.parseAmountFilter("1e3"));
        assertNull(JsfUtil.parseAmountFilter("0x1p4"));
    }

    @Test
    void unsignedAmountMatchesEitherSign() {
        Map<String, Object> params = new HashMap<>();

        String jpql = JsfUtil.exactAmountFilter("b.netTotal", "netTotal", "2,500", "Net Total", params);

        assertEquals(" and abs(abs(b.netTotal) - :netTotal) < 0.005 ", jpql);
        assertEquals(2500.0, params.get("netTotal"));
    }

    @Test
    void negativeAmountMatchesOnlyThatSign() {
        Map<String, Object> params = new HashMap<>();

        String jpql = JsfUtil.exactAmountFilter("b.netTotal", "netTotal", "-2500", "Net Total", params);

        assertEquals(" and abs(b.netTotal - :netTotal) < 0.005 ", jpql);
        assertEquals(-2500.0, params.get("netTotal"));
    }
}
