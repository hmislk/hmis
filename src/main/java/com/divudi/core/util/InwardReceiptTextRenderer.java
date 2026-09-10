package com.divudi.core.util;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Department;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Renders an inward deposit / payment receipt as fixed-width plain text for
 * impact (dot-matrix) printers. 40-column body. Optionally wrapped in ESC/P
 * control codes for raw printing that bypasses the browser rasteriser.
 *
 * Pure and side-effect free: no CDI, no DB, no FacesContext — unit-testable.
 */
public final class InwardReceiptTextRenderer {

    public static final int WIDTH = 40;
    private static final int LABEL_WIDTH = 15; // "Admission Type " then ':'

    private InwardReceiptTextRenderer() {
    }

    public static String render(Bill bill, String heading, boolean duplicate,
            boolean preprintedStationery, int topMarginLines, boolean emitEscP) {
        StringBuilder sb = new StringBuilder(1024);

        if (emitEscP) {
            sb.append('').append('@');   // ESC @  — initialise
            sb.append('').append('x').append(''); // ESC x 1 — LQ mode
            sb.append('').append('P');   // ESC P  — 10 CPI
        }

        int margin = Math.max(0, Math.min(40, topMarginLines));
        for (int i = 0; i < margin; i++) {
            sb.append('\n');
        }

        Department dept = bill.getDepartment();
        if (!preprintedStationery && dept != null) {
            centre(sb, safe(dept.getPrintingName()));
            centre(sb, safe(dept.getAddress()));
            String tel = safe(dept.getTelephone1());
            if (notBlank(dept.getTelephone2())) {
                tel = tel + " / " + dept.getTelephone2().trim();
            }
            centre(sb, tel);
            if (notBlank(dept.getFax())) {
                centre(sb, "Fax: " + dept.getFax().trim());
            }
        }

        String head = safe(heading);
        if (duplicate) {
            head = head + " **Duplicate**";
        }
        if (bill.isCancelled()) {
            head = head + " **Cancelled**";
        }
        centre(sb, head);
        rule(sb, '-');

        PatientEncounter pe = bill.getPatientEncounter();
        String admissionType = pe != null && pe.getAdmissionType() != null
                ? safe(pe.getAdmissionType().getName()) : "";
        String name = "", sex = "", address = "", phone = "", age = "";
        if (pe != null && pe.getPatient() != null && pe.getPatient().getPerson() != null) {
            name = safe(pe.getPatient().getPerson().getNameWithTitle());
            sex = pe.getPatient().getPerson().getSex() != null
                    ? pe.getPatient().getPerson().getSex().toString() : "";
            address = safe(pe.getPatient().getPerson().getAddress());
            phone = safe(pe.getPatient().getPerson().getPhone());
        }
        if (pe != null && pe.getPatient() != null) {
            age = String.valueOf(pe.getPatient().getAge());
        }
        String bht = pe != null ? safe(pe.getBhtNo()) : "";

        DecimalFormat money = new DecimalFormat("#,##0.00");
        SimpleDateFormat dfDate = new SimpleDateFormat("dd/MMM/yyyy");
        SimpleDateFormat dfTime = new SimpleDateFormat("hh:mm a");
        Date created = bill.getCreatedAt();

        field(sb, "Admission Type", admissionType);
        field(sb, "Name", name);
        field(sb, "Age / Gender", (age + " " + sex).trim());
        field(sb, "Address", address);
        field(sb, "Phone", phone);
        field(sb, "BHT No", bht);
        field(sb, "Bill No", safe(bill.getDeptId()));
        field(sb, "Bill Date", created == null ? "" : dfDate.format(created));
        field(sb, "Bill Time", created == null ? "" : dfTime.format(created));
        field(sb, "Payment", bill.getPaymentMethod() == null ? ""
                : bill.getPaymentMethod().toString());

        rule(sb, '=');
        String amt = money.format(bill.getTotal());
        String amtLabel = "Paying Amount";
        int pad = WIDTH - amtLabel.length() - amt.length();
        if (pad < 1) {
            pad = 1;
        }
        sb.append(amtLabel).append(spaces(pad)).append(amt).append('\n');
        rule(sb, '=');

        if (notBlank(bill.getComments())) {
            field(sb, "Comment", bill.getComments().trim());
        }

        sb.append('\n');
        String cashier = "";
        if (bill.getCreater() != null && bill.getCreater().getWebUserPerson() != null) {
            cashier = safe(bill.getCreater().getWebUserPerson().getName());
        }
        sb.append(clip("Cashier : " + cashier)).append('\n');

        if (emitEscP) {
            sb.append('\f'); // form feed — advance to next form
        }
        return sb.toString();
    }

    private static void field(StringBuilder sb, String label, String value) {
        String l = label;
        if (l.length() > LABEL_WIDTH) {
            l = l.substring(0, LABEL_WIDTH);
        }
        String prefix = padRight(l, LABEL_WIDTH) + ": ";
        int room = WIDTH - prefix.length();
        String v = value == null ? "" : value;
        if (v.length() <= room) {
            sb.append(prefix).append(v).append('\n');
        } else {
            // wrap continuation lines under the value column
            sb.append(prefix).append(v.substring(0, room)).append('\n');
            String rest = v.substring(room);
            String indent = spaces(prefix.length());
            while (rest.length() > room) {
                sb.append(indent).append(rest.substring(0, room)).append('\n');
                rest = rest.substring(room);
            }
            sb.append(indent).append(rest).append('\n');
        }
    }

    private static void centre(StringBuilder sb, String s) {
        String v = clip(s);
        int lead = (WIDTH - v.length()) / 2;
        if (lead < 0) {
            lead = 0;
        }
        sb.append(spaces(lead)).append(v).append('\n');
    }

    private static void rule(StringBuilder sb, char c) {
        for (int i = 0; i < WIDTH; i++) {
            sb.append(c);
        }
        sb.append('\n');
    }

    private static String clip(String s) {
        String v = s == null ? "" : s;
        return v.length() > WIDTH ? v.substring(0, WIDTH) : v;
    }

    private static String padRight(String s, int n) {
        StringBuilder b = new StringBuilder(s == null ? "" : s);
        while (b.length() < n) {
            b.append(' ');
        }
        return b.toString();
    }

    private static String spaces(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < Math.max(0, n); i++) {
            b.append(' ');
        }
        return b.toString();
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
