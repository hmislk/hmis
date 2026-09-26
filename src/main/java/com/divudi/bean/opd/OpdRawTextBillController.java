package com.divudi.bean.opd;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.util.OpdBillTextRenderer;
import com.divudi.service.BillService;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;

/**
 * Raw text (.prn) printing of the 5x5 Custom 3 OPD bill for dot-matrix
 * printers, plus the per-department print settings behind the "Raw Text
 * Settings" dialog. The .prn file is picked up by the client print agent
 * (tools/client-print-agent/) and raw-copied to the printer, so digits print
 * in the printer's own font instead of a rasterised browser image.
 */
@Named
@RequestScoped
public class OpdRawTextBillController implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String KEY_TOP_MARGIN = "OPD Raw Text Bill Top Margin Lines";
    public static final String KEY_LEFT_MARGIN = "OPD Raw Text Bill Left Margin Columns";
    public static final String KEY_RIGHT_BORDER = "OPD Raw Text Bill Right Border Column";
    public static final String KEY_CPI = "OPD Raw Text Bill Characters Per Inch";
    public static final String KEY_FORM_LENGTH = "OPD Raw Text Bill Form Length Lines";
    public static final String KEY_EMIT_ESCP = "OPD Raw Text Bill Emit ESC/P Codes";
    private static final String KEY_PRINT_NAME = "Show the print name of the items on the 5x5 Custum 3 bill.";

    private static final long DEFAULT_TOP_MARGIN = 8L;
    private static final long DEFAULT_LEFT_MARGIN = 0L;
    private static final long DEFAULT_RIGHT_BORDER = 50L;
    private static final long DEFAULT_CPI = 12L;
    private static final long DEFAULT_FORM_LENGTH = 0L;

    @Inject
    private SessionController sessionController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @EJB
    private BillService billService;

    private Long topMarginLines;
    private Long leftMarginColumns;
    private Long rightBorderColumn;
    private Long charactersPerInch;
    private Long formLengthLines;
    private boolean emitEscP;

    @PostConstruct
    public void init() {
        Department dept = sessionController.getDepartment();
        topMarginLines = longValue(KEY_TOP_MARGIN, dept, DEFAULT_TOP_MARGIN);
        leftMarginColumns = longValue(KEY_LEFT_MARGIN, dept, DEFAULT_LEFT_MARGIN);
        rightBorderColumn = longValue(KEY_RIGHT_BORDER, dept, DEFAULT_RIGHT_BORDER);
        charactersPerInch = longValue(KEY_CPI, dept, DEFAULT_CPI);
        formLengthLines = longValue(KEY_FORM_LENGTH, dept, DEFAULT_FORM_LENGTH);
        emitEscP = configOptionApplicationController
                .getBooleanValueByKeyForDepartment(KEY_EMIT_ESCP, dept, true);
    }

    /**
     * Streams the given bills as one .prn job, each bill on its own form.
     *
     * @param copies how many times the whole set is printed (the OPD bill
     * copies preference); values below 1 print once
     */
    public void streamBills(List<Bill> bills, boolean duplicate, int copies) {
        if (bills == null || bills.isEmpty()) {
            JsfUtil.addErrorMessage("No bill to print.");
            return;
        }
        List<Bill> job = new ArrayList<>();
        for (int i = 0; i < Math.max(1, copies); i++) {
            job.addAll(bills);
        }
        stream(job, duplicate, bills.get(0));
    }

    public void streamBills(List<Bill> bills, boolean duplicate) {
        streamBills(bills, duplicate, 1);
    }

    public void streamBill(Bill bill, boolean duplicate) {
        if (bill == null) {
            JsfUtil.addErrorMessage("No bill to print.");
            return;
        }
        stream(Collections.singletonList(bill), duplicate, bill);
    }

    private void stream(List<Bill> bills, boolean duplicate, Bill first) {
        String printedBy = sessionController.getLoggedUser() == null ? ""
                : sessionController.getLoggedUser().getName();
        String printedAt = formatPrintedAt(new Date());
        boolean usePrintName = configOptionApplicationController.getBooleanValueByKey(KEY_PRINT_NAME, false);

        String text = OpdBillTextRenderer.render(bills, billService::fetchBillPayments,
                duplicate, printedBy, printedAt, usePrintName, currentLayout());

        String id = first.getDeptId() == null ? String.valueOf(first.getId()) : first.getDeptId();
        String fileName = "opd-bill-" + id.replaceAll("[^A-Za-z0-9._-]", "_") + ".prn";

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        try (OutputStream os = response.getOutputStream()) {
            // ISO-8859-1 so ESC/P control bytes (0x1B, 0x0C) pass through unchanged.
            os.write(text.getBytes(Charset.forName("ISO-8859-1")));
            os.flush();
        } catch (java.io.IOException e) {
            JsfUtil.addErrorMessage("Could not generate the raw text bill: " + e.getMessage());
        }
        context.responseComplete();
    }

    public OpdBillTextRenderer.Layout currentLayout() {
        return new OpdBillTextRenderer.Layout()
                .setTopMarginLines(intValue(topMarginLines, DEFAULT_TOP_MARGIN))
                .setLeftMarginColumns(intValue(leftMarginColumns, DEFAULT_LEFT_MARGIN))
                .setRightBorderColumn(intValue(rightBorderColumn, DEFAULT_RIGHT_BORDER))
                .setCharactersPerInch(intValue(charactersPerInch, DEFAULT_CPI))
                .setFormLengthLines(intValue(formLengthLines, DEFAULT_FORM_LENGTH))
                .setEmitEscP(emitEscP);
    }

    /**
     * Saves the dialog values for the logged department.
     */
    public void saveSettings() {
        Department dept = sessionController.getDepartment();
        if (dept == null) {
            JsfUtil.addErrorMessage("Select a department first.");
            return;
        }
        OpdBillTextRenderer.Layout l = currentLayout();
        if (l.getRightBorderColumn() <= l.getLeftMarginColumns()) {
            JsfUtil.addErrorMessage("Right border column must be greater than the left margin.");
            return;
        }
        configOptionApplicationController.setLongValueByKeyForDepartment(KEY_TOP_MARGIN, dept, (long) l.getTopMarginLines());
        configOptionApplicationController.setLongValueByKeyForDepartment(KEY_LEFT_MARGIN, dept, (long) l.getLeftMarginColumns());
        configOptionApplicationController.setLongValueByKeyForDepartment(KEY_RIGHT_BORDER, dept, (long) l.getRightBorderColumn());
        configOptionApplicationController.setLongValueByKeyForDepartment(KEY_CPI, dept, (long) l.getCharactersPerInch());
        configOptionApplicationController.setLongValueByKeyForDepartment(KEY_FORM_LENGTH, dept, (long) l.getFormLengthLines());
        configOptionApplicationController.setBooleanValueByKeyForDepartment(KEY_EMIT_ESCP, dept, emitEscP);
        JsfUtil.addSuccessMessage("Raw text print settings saved for " + dept.getName());
    }

    private String formatPrintedAt(Date d) {
        String pattern = null;
        if (sessionController.getApplicationPreference() != null) {
            pattern = sessionController.getApplicationPreference().getLongDateTimeFormat();
        }
        SimpleDateFormat f;
        try {
            f = new SimpleDateFormat(pattern == null || pattern.trim().isEmpty()
                    ? "yyyy-MM-dd hh:mm a" : pattern, Locale.ENGLISH);
        } catch (IllegalArgumentException e) {
            f = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.ENGLISH);
        }
        f.setTimeZone(TimeZone.getTimeZone("Asia/Colombo"));
        return f.format(d);
    }

    private Long longValue(String key, Department dept, long def) {
        Long v = configOptionApplicationController.getLongValueByKeyForDepartment(key, dept, def);
        return v == null ? def : v;
    }

    private static int intValue(Long v, long def) {
        return (int) (v == null ? def : v);
    }

    public Long getTopMarginLines() {
        return topMarginLines;
    }

    public void setTopMarginLines(Long topMarginLines) {
        this.topMarginLines = topMarginLines;
    }

    public Long getLeftMarginColumns() {
        return leftMarginColumns;
    }

    public void setLeftMarginColumns(Long leftMarginColumns) {
        this.leftMarginColumns = leftMarginColumns;
    }

    public Long getRightBorderColumn() {
        return rightBorderColumn;
    }

    public void setRightBorderColumn(Long rightBorderColumn) {
        this.rightBorderColumn = rightBorderColumn;
    }

    public Long getCharactersPerInch() {
        return charactersPerInch;
    }

    public void setCharactersPerInch(Long charactersPerInch) {
        this.charactersPerInch = charactersPerInch;
    }

    public Long getFormLengthLines() {
        return formLengthLines;
    }

    public void setFormLengthLines(Long formLengthLines) {
        this.formLengthLines = formLengthLines;
    }

    public boolean isEmitEscP() {
        return emitEscP;
    }

    public void setEmitEscP(boolean emitEscP) {
        this.emitEscP = emitEscP;
    }
}
