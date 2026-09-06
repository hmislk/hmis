package com.divudi.core.util;

import com.divudi.core.data.dto.PrintBillData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serialises and restores the itemised payment lines of a Multiple Payment Methods
 * bill to and from {@code Bill.paymentBreakdown}.
 *
 * Needed where the components are entered but no {@code Payment} rows are written yet -
 * the pharmacy Sale for Cashier pre-bill takes no money, so the cashier creates the real
 * Payment rows later against the settled bill. Before this the split lived only in the
 * session-scoped {@code PaymentMethodData}, so a reprint showed no breakdown at all while
 * the slip handed to the customer minutes earlier did (#22487).
 *
 * The stored value is print-only. It is never summed, never reconciled and must not be
 * read as evidence that money was received - persisted {@code Payment} rows remain the
 * single source of truth for that.
 *
 * Both directions are lenient by design: a bill whose breakdown is absent, empty or
 * unparseable prints without the itemisation rather than failing. Losing a decorative
 * line on a receipt is preferable to blocking a settle or a reprint.
 */
public class PaymentBreakdownCodec {

    private static final Logger LOGGER = Logger.getLogger(PaymentBreakdownCodec.class.getName());

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private PaymentBreakdownCodec() {
    }

    /**
     * Renders the given payment lines as a JSON array for storage on the bill.
     *
     * @param lines the itemised components; may be null or empty
     * @return the JSON array, or null when there is nothing worth storing or the write
     *         failed - callers store the result as-is and must tolerate null
     */
    public static String serialize(List<PrintBillData.PaymentLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(lines);
        } catch (Exception e) {
            // Never fail a settle over a print-only field: the bill and the stock
            // movement matter, the receipt itemisation does not.
            LOGGER.log(Level.WARNING, "Could not serialise payment breakdown; bill will be stored without it", e);
            return null;
        }
    }

    /**
     * Restores the payment lines previously stored by {@link #serialize(List)}.
     *
     * @param json the stored value; may be null, blank or malformed
     * @return the payment lines, or an empty list when nothing could be restored - never null
     */
    public static List<PrintBillData.PaymentLine> deserialize(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            List<PrintBillData.PaymentLine> lines
                    = MAPPER.readValue(json, new TypeReference<List<PrintBillData.PaymentLine>>() {
                    });
            return lines == null ? new ArrayList<>() : lines;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not restore payment breakdown; bill will print without it", e);
            return new ArrayList<>();
        }
    }
}
