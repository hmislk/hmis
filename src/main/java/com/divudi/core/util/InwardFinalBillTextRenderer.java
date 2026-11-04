package com.divudi.core.util;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders an inward Final Bill or Interim Bill as fixed-width plain text for
 * impact (dot-matrix) printers on pre-printed continuous stationery: only the
 * variable content is printed (title, header block, charge lines, payments,
 * totals, preparer). Optionally wrapped in ESC/P control codes.
 *
 * When a form length is given the bill is paged so that nothing prints over the
 * pre-printed header or footer of the next form.
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
    private static final String CONTINUED = "continued...";

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
        /** Credit-company (and patient) dues, printed after the net amount. */
        public List<Row> dueFrom = new ArrayList<>();
        public String totalAmount = "";
        public String discounts = "";
        public String deposit = "";
        public String netAmount = "";
        public String preparedBy = "";
        public boolean showDiscounts = true;
    }

    /**
     * The amount format for every printed figure: "#,##0.00" with the same rounding as the
     * on-screen bill (DecimalFormat default, HALF_EVEN), and a value that rounds to zero is
     * printed as 0.00, never -0.00.
     */
    public static DecimalFormat newMoneyFormat() {
        return new DecimalFormat("#,##0.00") {
            private static final long serialVersionUID = 1L;

            @Override
            public StringBuffer format(double number, StringBuffer result, FieldPosition pos) {
                int start = result.length();
                super.format(number, result, pos);
                if (result.substring(start).equals("-0.00")) {
                    result.setLength(start);
                    result.append("0.00");
                }
                return result;
            }
        };
    }

    public static int clampWidth(long width) {
        return (int) Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, width));
    }

    /** Same as the paging overload with no bottom margin. */
    public static String render(Data d, int lineWidth, int topMarginLines,
            int formLengthLines, boolean emitEscP) {
        return render(d, lineWidth, topMarginLines, 0, formLengthLines, emitEscP);
    }

    /**
     * @param topMarginLines blank lines before the first line of every page (0..40)
     * @param bottomMarginLines lines kept free at the foot of every page for the
     * pre-printed footer (0..40); used only when paging
     * @param formLengthLines form length in lines. When above zero it is sent as
     * ESC C n and the bill is paged: a page holds at most
     * formLength - top margin - bottom margin lines of content. Zero leaves the
     * printer's own length and prints one continuous stream.
     */
    public static String render(Data d, int lineWidth, int topMarginLines, int bottomMarginLines,
            int formLengthLines, boolean emitEscP) {
        int w = clampWidth(lineWidth);
        int top = Math.max(0, Math.min(40, topMarginLines));
        int bottom = Math.max(0, Math.min(40, bottomMarginLines));
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

        List<String> head = firstHeader(d, w);
        List<List<String>> units = chargeUnits(d.charges, w);
        List<String> totals = totalsBlock(d, w);

        if (formLengthLines <= 0) {
            appendBlank(sb, top);
            appendLines(sb, head);
            for (List<String> unit : units) {
                appendLines(sb, unit);
            }
            appendLines(sb, totals);
        } else {
            int usable = Math.max(1, formLengthLines - top - bottom);
            List<List<String>> pages = null;
            // The header height does not depend on the page number, only on its digit count.
            for (int digits = 1; digits <= 4; digits++) {
                pages = paginate(units, totals, head.size(),
                        continuationHeader(d, w, digits, digits).size(), usable);
                if (String.valueOf(pages.size()).length() <= digits) {
                    break;
                }
            }
            int count = pages.size();
            for (int i = 0; i < count; i++) {
                if (i > 0) {
                    sb.append('\f');
                }
                appendBlank(sb, top);
                appendLines(sb, i == 0 ? head : continuationHeader(d, w, i + 1, count));
                appendLines(sb, pages.get(i));
                if (i < count - 1) {
                    sb.append(spaces(w - CONTINUED.length())).append(CONTINUED).append('\n');
                }
            }
        }
        if (emitEscP) {
            sb.append('\f');
        }
        return sb.toString();
    }

    /** Splits the content into pages; each page holds the body lines after its header. */
    private static List<List<String>> paginate(List<List<String>> units, List<String> totals,
            int firstHeadLines, int contHeadLines, int usable) {
        List<List<String>> pages = new ArrayList<>();
        List<String> cur = new ArrayList<>();
        int used = firstHeadLines;
        for (List<String> unit : units) {
            // keep one line free for "continued..." because more content follows
            if (used + unit.size() + 1 > usable && !cur.isEmpty()) {
                pages.add(cur);
                cur = new ArrayList<>();
                used = contHeadLines;
            }
            cur.addAll(unit);
            used += unit.size();
        }
        if (used + totals.size() > usable && !cur.isEmpty()) {
            pages.add(cur);
            cur = new ArrayList<>();
        }
        cur.addAll(totals);
        pages.add(cur);
        return pages;
    }

    private static List<String> firstHeader(Data d, int w) {
        StringBuilder b = new StringBuilder();
        for (String line : wrap(safe(d.title), w)) {
            b.append(centre(line, w)).append('\n');
        }
        b.append('\n');
        pair(b, "Bill No : " + safe(d.billNo), "BHT No : " + safe(d.bhtNo), w);
        pair(b, "Date : " + safe(d.billDate), "Time : " + safe(d.billTime), w);
        wrapped(b, "Patient : ", safe(d.patientName), w);
        if (!safe(d.nic).isEmpty()) {
            wrapped(b, "NIC : ", safe(d.nic), w);
        }
        b.append(repeat('-', w)).append('\n');
        b.append(spread("PARTICULAR", "AMOUNT", w)).append('\n');
        b.append(repeat('-', w)).append('\n');
        return toLines(b);
    }

    /** Compact header for pages after the first: title, bill number, page n/N, column header. */
    private static List<String> continuationHeader(Data d, int w, int page, int pages) {
        String title = safe(d.title);
        String right = "Bill No : " + safe(d.billNo) + "   Page " + page + "/" + pages;
        StringBuilder b = new StringBuilder();
        if (title.length() + 2 + right.length() <= w) {
            b.append(title).append(spaces(w - title.length() - right.length())).append(right).append('\n');
        } else {
            for (String line : wrap(title, w)) {
                b.append(centre(line, w)).append('\n');
            }
            if (right.length() <= w) {
                b.append(right).append('\n');
            } else {
                pair(b, "Bill No : " + safe(d.billNo), "Page " + page + "/" + pages, w);
            }
        }
        b.append(repeat('-', w)).append('\n');
        b.append(spread("PARTICULAR", "AMOUNT", w)).append('\n');
        b.append(repeat('-', w)).append('\n');
        return toLines(b);
    }

    /**
     * The charge rows as units that must not be split across pages: one row with its
     * wrapped lines; a group heading together with its first line; a subtotal together
     * with the line before it.
     */
    private static List<List<String>> chargeUnits(List<Row> charges, int w) {
        int amountWidth = AMOUNT_WIDTH;
        for (Row r : charges) {
            amountWidth = Math.max(amountWidth, safe(r.getAmount()).length());
        }
        int labelWidth = Math.max(8, w - amountWidth - 1);
        List<List<String>> units = new ArrayList<>();
        boolean headingPending = false;
        for (Row r : charges) {
            if (r.kind == Row.SUBTOTAL) {
                String amount = safe(r.getAmount());
                if (units.isEmpty()) {
                    units.add(new ArrayList<>());
                }
                units.get(units.size() - 1).add(spaces(w - amount.length()) + amount);
                continue;
            }
            List<String> lines = rowLines(r, w, labelWidth);
            if (headingPending && !units.isEmpty()) {
                units.get(units.size() - 1).addAll(lines);
                headingPending = false;
            } else {
                units.add(new ArrayList<>(lines));
            }
            if (r.kind == Row.HEADING) {
                headingPending = true;
            }
        }
        return units;
    }

    private static List<String> rowLines(Row r, int w, int labelWidth) {
        List<String> out = new ArrayList<>();
        String amount = safe(r.getAmount());
        boolean detail = r.kind == Row.DETAIL;
        String first = detail ? "  " : "";
        String more = detail ? "    " : "  ";
        List<String> parts = wrap(safe(r.getLabel()), Math.max(4, labelWidth - first.length()));
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            if (i == 0) {
                part = first + part;
                out.add(amount.isEmpty() ? part : part + spaces(w - part.length() - amount.length()) + amount);
            } else {
                out.add(more + part);
            }
        }
        return out;
    }

    /** Closing rule, payments, totals, credit dues and preparer: always printed together. */
    private static List<String> totalsBlock(Data d, int w) {
        StringBuilder sb = new StringBuilder();
        sb.append(repeat('-', w)).append('\n');

        boolean side = w >= SIDE_BY_SIDE_MIN_WIDTH;
        List<String> left = new ArrayList<>();
        if (!d.payments.isEmpty()) {
            int half = side ? (w - 2) / 2 : w;
            left.add(spread("PAY TYPE", "AMOUNT", half));
            for (Row r : d.payments) {
                String amount = safe(r.getAmount());
                String type = clip(safe(r.getLabel()), Math.max(1, half - amount.length() - 1));
                left.add(type + spaces(half - type.length() - amount.length()) + amount);
            }
        }
        List<String> right = new ArrayList<>();
        int rightWidth = side ? w - ((w - 2) / 2) - 2 : w;
        right.add(spread("TOTAL AMOUNT", safe(d.totalAmount), rightWidth));
        if (d.showDiscounts) {
            right.add(spread("DISCOUNTS", safe(d.discounts), rightWidth));
        }
        right.add(spread("DEPOSIT", safe(d.deposit), rightWidth));
        right.add(spread("NET AMOUNT (LKR)", safe(d.netAmount), rightWidth));
        for (Row due : d.dueFrom) {
            right.addAll(rowLines(due, rightWidth, Math.max(8, rightWidth - AMOUNT_WIDTH - 1)));
        }

        if (side) {
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
        return toLines(sb);
    }

    private static List<String> toLines(StringBuilder sb) {
        List<String> lines = new ArrayList<>();
        for (String line : sb.toString().split("\n", -1)) {
            lines.add(line);
        }
        lines.remove(lines.size() - 1);   // the empty text after the last newline
        return lines;
    }

    private static void appendLines(StringBuilder sb, List<String> lines) {
        for (String line : lines) {
            sb.append(line).append('\n');
        }
    }

    private static void appendBlank(StringBuilder sb, int n) {
        for (int i = 0; i < n; i++) {
            sb.append('\n');
        }
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
        List<String> parts = wrap(value, Math.max(1, w - prefix.length()));
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
