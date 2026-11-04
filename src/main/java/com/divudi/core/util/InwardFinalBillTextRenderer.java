package com.divudi.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders an inward Final Bill or Interim Bill as fixed-width plain text for
 * impact (dot-matrix) printers on pre-printed continuous stationery: only the
 * variable content is printed (title, header block, charge lines, payments,
 * totals, preparer). Optionally wrapped in ESC/P control codes.
 *
 * Pure and side-effect free: no CDI, no DB, no FacesContext - unit-testable.
 */
public final class InwardFinalBillTextRenderer {

    public static final int DEFAULT_WIDTH = 40;
    public static final int MIN_WIDTH = 24;
    public static final int MAX_WIDTH = 136;
    /** Totals and payments sit side by side only when the form is at least this wide. */
    public static final int SIDE_BY_SIDE_MIN_WIDTH = 66;
    private static final int AMOUNT_WIDTH = 12;

    private InwardFinalBillTextRenderer() {
    }

    /** One printed line item: a name and an already formatted amount (or text such as "Free of Charge"). */
    public static final class Row {

        private static final int NORMAL = 0;
        private static final int HEADING = 1;
        private static final int DETAIL = 2;
        private static final int SUBTOTAL = 3;

        private final String label;
        private final String amount;
        private final int kind;

        public Row(String label, String amount) {
            this(label, amount, NORMAL);
        }

        private Row(String label, String amount, int kind) {
            this.label = label;
            this.amount = amount;
            this.kind = kind;
        }

        /** A group heading with no amount on its line (e.g. "Consultant Fee"). */
        public static Row heading(String label) {
            return new Row(label, "", HEADING);
        }

        /** A line under a heading, indented (e.g. one doctor and the fee). */
        public static Row detail(String label, String amount) {
            return new Row(label, amount, DETAIL);
        }

        /** The group total, right-aligned on a line of its own. */
        public static Row subtotal(String amount) {
            return new Row("", amount, SUBTOTAL);
        }

        public String getLabel() {
            return label;
        }

        public String getAmount() {
            return amount;
        }
    }

    /** Everything the renderer prints. Amounts are pre-formatted strings. */
    public static final class Data {

        public String title = "";
        public String billNo = "";
        public String bhtNo = "";
        public String billDate = "";
        public String billTime = "";
        public String patientName = "";
        public String nic = "";
        public List<Row> charges = new ArrayList<>();
        public List<Row> payments = new ArrayList<>();
        public String totalAmount = "";
        public String discounts = "";
        public String deposit = "";
        public String netAmount = "";
        public String preparedBy = "";
        public boolean showDiscounts = true;
    }

    public static int clampWidth(long width) {
        return (int) Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, width));
    }

    /**
     * @param topMarginLines blank lines before the title (0..40)
     * @param formLengthLines form length sent as ESC C n when emitting
     * ESC/P codes; 0 leaves the printer's own length
     */
    public static String render(Data d, int lineWidth, int topMarginLines,
            int formLengthLines, boolean emitEscP) {
        int w = clampWidth(lineWidth);
        StringBuilder sb = new StringBuilder(1024);

        if (emitEscP) {
            sb.append('\u001B').append('@');                     // ESC @   - initialise
            sb.append('\u001B').append('U').append('\u0001');    // ESC U 1 - unidirectional
            sb.append('\u001B').append('p').append('\u0000');    // ESC p 0 - proportional off
            sb.append('\u001B').append('x').append('\u0001');    // ESC x 1 - LQ mode
            sb.append('\u001B').append('P');                     // ESC P   - 10 CPI
            if (formLengthLines > 0) {
                sb.append('\u001B').append('C').append((char) Math.min(127, formLengthLines)); // ESC C n
            }
        }
        for (int i = 0; i < Math.max(0, Math.min(40, topMarginLines)); i++) {
            sb.append('\n');
        }

        for (String line : wrap(safe(d.title), w)) {
            sb.append(centre(line, w)).append('\n');
        }
        sb.append('\n');

        pair(sb, "Bill No : " + safe(d.billNo), "BHT No : " + safe(d.bhtNo), w);
        pair(sb, "Date : " + safe(d.billDate), "Time : " + safe(d.billTime), w);
        wrapped(sb, "Patient : ", safe(d.patientName), w);
        if (!safe(d.nic).isEmpty()) {
            wrapped(sb, "NIC : ", safe(d.nic), w);
        }

        sb.append(repeat('-', w)).append('\n');
        sb.append(spread("PARTICULAR", "AMOUNT", w)).append('\n');
        sb.append(repeat('-', w)).append('\n');

        int labelWidth = Math.max(8, w - AMOUNT_WIDTH - 1);
        for (Row r : d.charges) {
            String amount = safe(r.getAmount());
            if (r.kind == Row.SUBTOTAL) {
                sb.append(spaces(w - amount.length())).append(amount).append('\n');
                continue;
            }
            boolean detail = r.kind == Row.DETAIL;
            String first = detail ? "  " : "";
            String more = detail ? "    " : "  ";
            List<String> parts = wrap(safe(r.getLabel()), labelWidth - first.length());
            for (int i = 0; i < parts.size(); i++) {
                String part = parts.get(i);
                if (i == 0) {
                    part = first + part;
                    sb.append(part);
                    if (!amount.isEmpty()) {
                        sb.append(spaces(w - part.length() - amount.length())).append(amount);
                    }
                } else {
                    sb.append(more).append(part);
                }
                sb.append('\n');
            }
        }
        sb.append(repeat('-', w)).append('\n');

        List<String> left = new ArrayList<>();
        if (!d.payments.isEmpty()) {
            int half = w >= SIDE_BY_SIDE_MIN_WIDTH ? (w - 2) / 2 : w;
            left.add(spread("PAY TYPE", "AMOUNT", half));
            for (Row r : d.payments) {
                String amount = safe(r.getAmount());
                String type = clip(safe(r.getLabel()), Math.max(1, half - amount.length() - 1));
                left.add(type + spaces(half - type.length() - amount.length()) + amount);
            }
        }
        List<String> right = new ArrayList<>();
        int rightWidth = w >= SIDE_BY_SIDE_MIN_WIDTH ? w - ((w - 2) / 2) - 2 : w;
        right.add(spread("TOTAL AMOUNT", safe(d.totalAmount), rightWidth));
        if (d.showDiscounts) {
            right.add(spread("DISCOUNTS", safe(d.discounts), rightWidth));
        }
        right.add(spread("DEPOSIT", safe(d.deposit), rightWidth));
        right.add(spread("NET AMOUNT (LKR)", safe(d.netAmount), rightWidth));

        if (w >= SIDE_BY_SIDE_MIN_WIDTH) {
            int half = (w - 2) / 2;
            int n = Math.max(left.size(), right.size());
            for (int i = 0; i < n; i++) {
                String l = i < left.size() ? left.get(i) : "";
                String r = i < right.size() ? right.get(i) : "";
                sb.append(rtrim(padRight(l, half) + "  " + r)).append('\n');
            }
        } else {
            for (String l : left) {
                sb.append(l).append('\n');
            }
            if (!left.isEmpty()) {
                sb.append(repeat('-', w)).append('\n');
            }
            for (String r : right) {
                sb.append(r).append('\n');
            }
        }

        sb.append('\n');
        wrapped(sb, "BILL PREPARED BY : ", safe(d.preparedBy), w);
        if (emitEscP) {
            sb.append('\f');
        }
        return sb.toString();
    }

    /** Left text and right text on one line; stacked on two lines when they do not fit. */
    private static void pair(StringBuilder sb, String left, String right, int w) {
        if (left.length() + 1 + right.length() <= w) {
            sb.append(left).append(spaces(w - left.length() - right.length())).append(right).append('\n');
        } else {
            for (String line : wrap(left, w)) {
                sb.append(line).append('\n');
            }
            for (String line : wrap(right, w)) {
                sb.append(line).append('\n');
            }
        }
    }

    private static void wrapped(StringBuilder sb, String prefix, String value, int w) {
        List<String> parts = wrap(value, Math.max(8, w - prefix.length()));
        for (int i = 0; i < parts.size(); i++) {
            sb.append(i == 0 ? prefix : spaces(prefix.length())).append(parts.get(i)).append('\n');
        }
    }

    private static String spread(String left, String right, int w) {
        String l = left;
        String r = right == null ? "" : right;
        if (l.length() + 1 + r.length() > w) {
            l = clip(l, Math.max(1, w - 1 - r.length()));
        }
        return l + spaces(w - l.length() - r.length()) + r;
    }

    static List<String> wrap(String s, int width) {
        List<String> lines = new ArrayList<>();
        String line = "";
        for (String word : (s == null ? "" : s.trim()).split("\\s+")) {
            while (word.length() > width) {
                if (!line.isEmpty()) {
                    lines.add(line);
                    line = "";
                }
                lines.add(word.substring(0, width));
                word = word.substring(width);
            }
            if (line.isEmpty()) {
                line = word;
            } else if (line.length() + 1 + word.length() <= width) {
                line = line + " " + word;
            } else {
                lines.add(line);
                line = word;
            }
        }
        if (!line.isEmpty() || lines.isEmpty()) {
            lines.add(line);
        }
        return lines;
    }

    private static String centre(String s, int w) {
        return spaces((w - s.length()) / 2) + s;
    }

    private static String repeat(char c, int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) {
            b.append(c);
        }
        return b.toString();
    }

    private static String spaces(int n) {
        return repeat(' ', Math.max(0, n));
    }

    private static String padRight(String s, int n) {
        return s + spaces(n - s.length());
    }

    private static String rtrim(String s) {
        int e = s.length();
        while (e > 0 && s.charAt(e - 1) == ' ') {
            e--;
        }
        return s.substring(0, e);
    }

    private static String clip(String s, int n) {
        return s.length() > n ? s.substring(0, n) : s;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
