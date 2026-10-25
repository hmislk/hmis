package com.divudi.bean.channel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ChannelTenderedAmountRulesTest {

    @Test
    public void blank_isRejectedOnlyWhenRequired() {
        assertEquals("Please Enter Tenderd Amount", ChannelTenderedAmountRules.tenderedAmountError(null, 2000, true));
        assertEquals("Please Enter Tenderd Amount", ChannelTenderedAmountRules.tenderedAmountError("  ", 2000, true));
        assertNull(ChannelTenderedAmountRules.tenderedAmountError(null, 2000, false));
        assertNull(ChannelTenderedAmountRules.tenderedAmountError("", 2000, false));
    }

    @Test
    public void lessThanPayable_isRejectedWithPayableAmount() {
        assertEquals("Please enter the correct tendered amount. The payable amount is 2000.00.",
                ChannelTenderedAmountRules.tenderedAmountError("1000", 2000, false));
    }

    @Test
    public void equalOrMore_isAccepted() {
        assertNull(ChannelTenderedAmountRules.tenderedAmountError("2000", 2000, true));
        assertNull(ChannelTenderedAmountRules.tenderedAmountError(" 2500.50 ", 2000, true));
        assertNull(ChannelTenderedAmountRules.tenderedAmountError("2000", 1999.9999999, true));
    }

    @Test
    public void fractionalUnderpayment_isNotRoundedUp() {
        assertNotNull(ChannelTenderedAmountRules.tenderedAmountError("1999.995", 2000, true));
    }

    @Test
    public void nonPlainNumbers_areInvalid() {
        String invalid = "Please enter a valid tendered amount.";
        assertEquals(invalid, ChannelTenderedAmountRules.tenderedAmountError("abc", 2000, true));
        assertEquals(invalid, ChannelTenderedAmountRules.tenderedAmountError("NaN", 2000, true));
        assertEquals(invalid, ChannelTenderedAmountRules.tenderedAmountError("Infinity", 2000, true));
        assertEquals(invalid, ChannelTenderedAmountRules.tenderedAmountError("1E+2147483647", 2000, true));
        assertEquals(invalid, ChannelTenderedAmountRules.tenderedAmountError("-5000", 2000, true));
        assertEquals(invalid, ChannelTenderedAmountRules.tenderedAmountError("0x1p12", 2000, true));
        assertEquals(invalid, ChannelTenderedAmountRules.tenderedAmountError("1234567890123", 2000, true));
    }
}
