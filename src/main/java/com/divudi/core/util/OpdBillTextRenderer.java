package com.divudi.core.util;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.Person;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Function;

/**
 * Renders an OPD bill as fixed-width plain text for impact (dot-matrix)
 * printers, replicating the 5x5 Custom 3 OPD bill
 * ({@code resources/opd/sale_bill_five_five_custom_3.xhtml}) on the same
 * pre-printed stationery. Optionally wrapped in ESC/P control codes for raw
 * printing that bypasses the browser rasteriser.
 *
 * No printed character ever goes beyond {@link Layout#rightBorderColumn}: the
 * right-most part of a print line falls outside the physical paper, so every
 * right-aligned value ends exactly at that column.
 *
 * Pure and side-effect free: no CDI, no DB, no FacesContext — unit-testable.
 */
public final class OpdBillTextRenderer {

    private static final TimeZone COLOMBO = TimeZone.getTimeZone("Asia/Colombo");
    private static final int NO_WIDTH = 4;
    private static final int QTY_WIDTH = 5;
    private static final int MIN_VALUE_WIDTH = 10;
    private static final int MIN_BODY_WIDTH = 30;

    private OpdBillTextRenderer() {
    }

    /**
     * Page geometry and printer control settings, resolved per department from
     * ConfigOptions by the caller.
     */
    public static final class Layout {

        private int topMarginLines = 8;
        private int leftMarginColumns = 0;
        private int rightBorderColumn = 50;
        private int charactersPerInch = 12;
        private int formLengthLines = 0;
        private boolean emitEscP = true;

        public int getTopMarginLines() {
            return topMarginLines;
        }

        public Layout setTopMarginLines(int topMarginLines) {
            this.topMarginLines = Math.max(0, Math.min(60, topMarginLines));
            return this;
        }

        public int getLeftMarginColumns() {
            return leftMarginColumns;
        }

        public Layout setLeftMarginColumns(int leftMarginColumns) {
            this.leftMarginColumns = Math.max(0, Math.min(60, leftMarginColumns));
            return this;
        }

        public int getRightBorderColumn() {
            return rightBorderColumn;
        }

        public Layout setRightBorderColumn(int rightBorderColumn) {
            this.rightBorderColumn = Math.max(1, Math.min(200, rightBorderColumn));
            return this;
        }

        public int getCharactersPerInch() {
            return charactersPerInch;
        }

        /**
         * 10, 12 or 15; anything else falls back to 12.
         */
        public Layout setCharactersPerInch(int charactersPerInch) {
            this.charactersPerInch = (charactersPerInch == 10 || charactersPerInch == 15)
                    ? charactersPerInch : 12;
            return this;
        }

        public int getFormLengthLines() {
            return formLengthLines;
        }

        /**
         * Form length in lines sent as ESC C n; 0 leaves the printer's own
         * setting untouched.
         */
        public Layout setFormLengthLines(int formLengthLines) {
            this.formLengthLines = Math.max(0, Math.min(127, formLengthLines));
            return this;
        }

        public boolean isEmitEscP() {
            return emitEscP;
        }

        public Layout setEmitEscP(boolean emitEscP) {
            this.emitEscP = emitEscP;
            return this;
        }

        /**
         * Printable body width: from the left margin up to the right border,
         * never narrower than {@value #MIN_BODY_WIDTH} columns.
         */
        public int bodyWidth() {
            return Math.max(MIN_BODY_WIDTH, printableWidth());
        }

        /**
         * Columns actually on the paper between the left margin and the right
         * border. Output is always clipped to this, even when it is narrower
         * than the layout width.
         */
        public int printableWidth() {
            return Math.max(1, rightBorderColumn - leftMarginColumns);
        }
    }

    /**
     * Renders one or more bills into a single print job; each bill starts on a
     * new form (top margin applied) and ends with a form feed.
     *
     * @param payments supplies the payment rows printed beside "Billed By" for
     * a bill (the template uses {@code billSearch.fetchBillPayments(bill)});
     * may be {@code null}
     * @param printedBy user name for the duplicate "Printed By" line
     * @param printedAt already formatted print date/time for duplicates
     * @param usePrintName print {@code item.printName} instead of
     * {@code item.name}
     */
    public static String render(List<Bill> bills, Function<Bill, List<Payment>> payments,
            boolean duplicate, String printedBy, String printedAt, boolean usePrintName,
            Layout layout) {
        Layout l = layout == null ? new Layout() : layout;
        StringBuilder sb = new StringBuilder(2048);
        if (l.isEmitEscP()) {
            sb.append('\u001B').append('@');                     // ESC @   — initialise
            sb.append('\u001B').append('x').append('\u0001');    // ESC x 1 — LQ mode
            switch (l.getCharactersPerInch()) {
                case 10:
                    sb.append('\u001B').append('P');             // ESC P   — 10 CPI
                    break;
                case 15:
                    sb.append('\u001B').append('g');             // ESC g   — 15 CPI
                    break;
                default:
                    sb.append('\u001B').append('M');             // ESC M   — 12 CPI
            }
            if (l.getFormLengthLines() > 0) {
                sb.append('\u001B').append('C').append((char) l.getFormLengthLines()); // ESC C n
            }
        }
        if (bills == null) {
            return sb.toString();
        }
        for (Bill b : bills) {
            if (b == null) {
                continue;
            }
            List<Payment> ps = payments == null ? null : payments.apply(b);
            sb.append(renderBill(b, ps, duplicate, printedBy, printedAt, usePrintName, l));
            sb.append('\f'); // form feed — next bill on its own form
        }
        return sb.toString();
    }

    /**
     * The body of a single bill (top margin included), without ESC/P
     * initialisation or the trailing form feed.
     */
    static String renderBill(Bill bill, List<Payment> payments, boolean duplicate,
            String printedBy, String printedAt, boolean usePrintName, Layout l) {
        int w = l.bodyWidth();
        List<String> lines = new ArrayList<>();
        DecimalFormat money = new DecimalFormat("#,##0.00");
        DecimalFormat count = new DecimalFormat("#,##0");

        // Header
        String deptName = bill.getDepartment() == null ? "" : safe(bill.getDepartment().getName());
        lines.add(centre(deptName.toUpperCase(Locale.ROOT), w));
        String markers = "";
        if (duplicate) {
            markers = "**Duplicate**";
        }
        if (bill.isCancelled()) {
            markers = (markers + " **Cancelled**").trim();
        }
        if (!markers.isEmpty()) {
            lines.add(centre(markers, w));
        }
        lines.add(repeat('-', w));

        // Patient and bill information — two columns like the HTML info-table
        SimpleDateFormat dfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        dfDate.setTimeZone(COLOMBO);
        SimpleDateFormat dfTime = new SimpleDateFormat("HH:mm a", Locale.ENGLISH);
        dfTime.setTimeZone(COLOMBO);
        Date created = bill.getCreatedAt();

        List<String[]> rows = new ArrayList<>();
        Person person = bill.getPatient() == null ? null : bill.getPatient().getPerson();
        if (bill.getPatient() != null) {
            String ageSex = null;
            if (person != null) {
                String sex = person.getSex() == null ? "" : person.getSex().getLabel();
                ageSex = safe(person.getAgeAsShortString()) + " / " + sex;
            }
            rows.add(new String[]{"Name :", person == null ? "" : safe(person.getNameWithTitle()),
                ageSex == null ? null : "Age/Sex :", ageSex});
        }
        rows.add(new String[]{"Bill Date:", created == null ? "" : dfDate.format(created),
            "Bill Time:", created == null ? "" : dfTime.format(created)});
        boolean hasBht = bill.getPatientEncounter() != null;
        rows.add(new String[]{"Bill No:", safe(bill.getDeptId()),
            hasBht ? "BHT No :" : null, hasBht ? safe(bill.getPatientEncounter().getBhtNo()) : null});
        infoBlock(lines, rows, w);
        String pm = bill.getPaymentMethod() == null ? "" : safe(bill.getPaymentMethod().getLabel());
        lines.addAll(wrapField("Payment Method : ", pm, w));
        lines.add(repeat('-', w));

        // Items
        List<BillItem> items = bill.getBillItems() == null ? new ArrayList<>() : bill.getBillItems();
        int valueWidth = MIN_VALUE_WIDTH;
        for (BillItem bi : items) {
            valueWidth = Math.max(valueWidth, money.format(bi.getGrossValue()).length() + 1);
        }
        int nameWidth = Math.max(4, w - NO_WIDTH - QTY_WIDTH - valueWidth);
        lines.add(padRight("NO", NO_WIDTH) + padRight("ITEM NAME", nameWidth)
                + padRight("QTY", QTY_WIDTH) + padLeft("VALUE", valueWidth));
        int n = 0;
        for (BillItem bi : items) {
            n++;
            String name = "";
            if (bi.getItem() != null) {
                name = safe(usePrintName ? bi.getItem().getPrintName() : bi.getItem().getName());
            }
            String qty = bi.getQty() == null ? "" : count.format(bi.getQty());
            List<String> nameLines = wrap(name, nameWidth - 1);
            for (int i = 0; i < nameLines.size(); i++) {
                if (i == 0) {
                    lines.add(padRight(String.valueOf(n), NO_WIDTH)
                            + padRight(nameLines.get(i), nameWidth)
                            + padRight(qty, QTY_WIDTH)
                            + padLeft(money.format(bi.getGrossValue()), valueWidth));
                } else {
                    lines.add(spaces(NO_WIDTH) + nameLines.get(i));
                }
            }
        }
        lines.add(repeat('-', w));

        // Totals
        lines.add(labelValue("Total:", money.format(bill.getTotal()), w));
        if (bill.getDiscount() != 0.0) {
            lines.add(labelValue("Discount:", money.format(-bill.getDiscount()), w));
            lines.add(labelValue("Net Total:", money.format(bill.getNetTotal()), w));
        }
        lines.add(labelValue("No of Items:", count.format(items.size()), w));

        // Billed by and payments
        String billedBy = bill.getCreater() == null ? "" : safe(bill.getCreater().getName());
        lines.addAll(wrapField("Billed By : ", billedBy, w));
        if (payments != null) {
            for (Payment p : payments) {
                if (p == null) {
                    continue;
                }
                String label = p.getPaymentMethod() == null ? "" : p.getPaymentMethod().toString();
                if (p.getPaymentMethod() == com.divudi.core.data.PaymentMethod.Card) {
                    label = label + " (" + safe(p.getCreditCardRefNo()) + ")";
                }
                lines.add(labelValue(label, money.format(p.getPaidValue()), w));
            }
        }

        if (duplicate) {
            lines.addAll(wrapField("Printed By : ", (safe(printedBy) + "  " + safe(printedAt)).trim(), w));
        }

        StringBuilder sb = new StringBuilder(1024);
        for (int i = 0; i < l.getTopMarginLines(); i++) {
            sb.append('\n');
        }
        String margin = spaces(l.getLeftMarginColumns());
        int printable = l.printableWidth();
        for (String line : lines) {
            String t = rtrim(clip(line, printable));
            sb.append(t.isEmpty() ? "" : margin + t).append('\n');
        }
        return sb.toString();
    }

    /**
     * Rows of {leftLabel, leftValue, rightLabel, rightValue}. The right block
     * is a label column plus a value column ending at the right border; the
     * left value wraps inside the space left of it.
     */
    private static void infoBlock(List<String> out, List<String[]> rows, int w) {
        int leftLabelWidth = 0;
        int rightLabelWidth = 0;
        int rightValueWidth = 0;
        for (String[] r : rows) {
            leftLabelWidth = Math.max(leftLabelWidth, r[0].length() + 1);
            if (r[2] != null) {
                rightLabelWidth = Math.max(rightLabelWidth, r[2].length() + 1);
                rightValueWidth = Math.max(rightValueWidth, safe(r[3]).length());
            }
        }
        int rightWidth = rightLabelWidth == 0 ? 0 : rightLabelWidth + rightValueWidth;
        if (rightWidth > w / 2) {
            rightWidth = w / 2;
        }
        int rightStart = w - rightWidth;
        int leftValueWidth = Math.max(1, rightStart - leftLabelWidth - 1);
        for (String[] r : rows) {
            // a row with nothing on the right may use the full width
            int room = r[2] == null ? Math.max(1, w - leftLabelWidth) : leftValueWidth;
            List<String> v = wrap(safe(r[1]), room);
            for (int i = 0; i < v.size(); i++) {
                String line = padRight(i == 0 ? r[0] : "", leftLabelWidth) + v.get(i);
                if (i == 0 && r[2] != null) {
                    line = padRight(line, rightStart)
                            + clip(padRight(r[2], rightLabelWidth) + safe(r[3]), rightWidth);
                }
                out.add(line);
            }
        }
    }

    private static List<String> wrapField(String label, String value, int w) {
        List<String> out = new ArrayList<>();
        List<String> v = wrap(value, Math.max(1, w - label.length()));
        for (int i = 0; i < v.size(); i++) {
            out.add((i == 0 ? label : spaces(label.length())) + v.get(i));
        }
        return out;
    }

    /**
     * Label on the left, value right-aligned so its last character sits
     * exactly on the right border.
     */
    private static String labelValue(String label, String value, int w) {
        String v = clip(value, w);
        String lab = clip(safe(label), Math.max(0, w - v.length() - 1));
        return lab + spaces(w - lab.length() - v.length()) + v;
    }

    /**
     * Word-wraps at spaces where possible, hard-splitting over-long words.
     * Always returns at least one (possibly empty) line.
     */
    static List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        String rest = text == null ? "" : text.trim();
        int wd = Math.max(1, width);
        while (rest.length() > wd) {
            int cut = rest.lastIndexOf(' ', wd);
            if (cut <= 0) {
                cut = wd;
            }
            out.add(rest.substring(0, cut).trim());
            rest = rest.substring(cut).trim();
        }
        out.add(rest);
        return out;
    }

    private static String centre(String s, int w) {
        String v = clip(s, w);
        return spaces((w - v.length()) / 2) + v;
    }

    private static String repeat(char c, int n) {
        StringBuilder b = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            b.append(c);
        }
        return b.toString();
    }

    private static String clip(String s, int n) {
        String v = s == null ? "" : s;
        return v.length() > n ? v.substring(0, n) : v;
    }

    private static String padRight(String s, int n) {
        StringBuilder b = new StringBuilder(s == null ? "" : s);
        while (b.length() < n) {
            b.append(' ');
        }
        return b.toString();
    }

    private static String padLeft(String s, int n) {
        String v = s == null ? "" : s;
        return spaces(n - v.length()) + v;
    }

    private static String spaces(int n) {
        return repeat(' ', Math.max(0, n));
    }

    private static String rtrim(String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == ' ') {
            end--;
        }
        return s.substring(0, end);
    }

    /**
     * Null-safe, printer-safe text: non-breaking spaces, tabs and line breaks
     * become single spaces, and any other character outside printable ASCII
     * becomes '?' — the printer's resident font cannot show it, and in the
     * default code page it would print as an unrelated symbol.
     */
    static String safe(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        boolean lastSpace = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c) || Character.isSpaceChar(c)) {
                if (!lastSpace) {
                    b.append(' ');
                }
                lastSpace = true;
                continue;
            }
            b.append(c >= 0x20 && c <= 0x7E ? c : '?');
            lastSpace = false;
        }
        return b.toString().trim();
    }
}
