package com.divudi.bean.inward;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Payment;
import com.divudi.core.util.InwardReceiptTextRenderer;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.BillService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;

/**
 * Raw text (.prn) printing of Inpatient deposit / payment receipts for
 * dot-matrix printers: the per-department "Raw Text" paper type, its layout
 * settings (Settings dialog), the on-screen preview, the .prn download and
 * the client print agent download. Shared by the deposit, payment and both
 * reprint pages.
 */
@Named
@RequestScoped
public class InwardRawTextReceiptController implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String KEY_RAW_TEXT_PAPER = "Inward Payment Bill Raw Text Paper";
    public static final String KEY_PREPRINTED = "Inward Raw Text Receipt Preprinted Stationery";
    public static final String KEY_TOP_MARGIN = "Inward Raw Text Receipt Top Margin Lines";
    public static final String KEY_EMIT_ESCP = "Inward Raw Text Receipt Emit ESC/P Codes";
    public static final String KEY_LINE_WIDTH = "Inward Raw Text Receipt Line Width";
    public static final String KEY_SHOW_ADMISSION_TYPE = "Inward Raw Text Receipt - Show Admission Type";
    public static final String KEY_SHOW_PATIENT_ADDRESS = "Inward Raw Text Receipt - Show Patient Address";
    public static final String KEY_SHOW_PATIENT_PHONE = "Inward Raw Text Receipt - Show Patient Phone";

    private static final long DEFAULT_TOP_MARGIN = 8L;
    /** Packaged from tools/client-print-agent/ by the maven-war-plugin. */
    private static final String PRINT_AGENT_DIR = "/WEB-INF/print-agent/";

    @Inject
    private SessionController sessionController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private WebUserController webUserController;
    @EJB
    private BillService billService;

    // Settings of the logged department, edited in the Settings dialog.
    private boolean rawTextPaper;
    private boolean preprintedStationery;
    private Long topMarginLines;
    private Long lineWidth;
    private boolean showAdmissionType;
    private boolean showPatientAddress;
    private boolean showPatientPhone;

    @PostConstruct
    public void init() {
        Department dept = sessionController.getDepartment();
        rawTextPaper = configOptionApplicationController
                .getBooleanValueByKeyForDepartment(KEY_RAW_TEXT_PAPER, dept, false);
        preprintedStationery = configOptionApplicationController
                .getBooleanValueByKeyForDepartment(KEY_PREPRINTED, dept, false);
        topMarginLines = longValue(KEY_TOP_MARGIN, dept, DEFAULT_TOP_MARGIN);
        lineWidth = longValue(KEY_LINE_WIDTH, dept, InwardReceiptTextRenderer.WIDTH);
        showAdmissionType = configOptionApplicationController
                .getBooleanValueByKeyForDepartment(KEY_SHOW_ADMISSION_TYPE, dept, true);
        showPatientAddress = configOptionApplicationController
                .getBooleanValueByKeyForDepartment(KEY_SHOW_PATIENT_ADDRESS, dept, true);
        showPatientPhone = configOptionApplicationController
                .getBooleanValueByKeyForDepartment(KEY_SHOW_PATIENT_PHONE, dept, true);
    }

    /**
     * The receipt text for {@code bill}, laid out with {@code layoutDept}'s
     * raw text settings.
     *
     * @param headingType bill type whose label heads the receipt
     * @param forPrinter true wraps the text in ESC/P codes (when enabled);
     * false gives plain text for the on-screen preview
     */
    public String renderReceipt(Bill bill, BillTypeAtomic headingType, boolean duplicate,
            Department layoutDept, boolean forPrinter) {
        boolean preprinted = configOptionApplicationController
                .getBooleanValueByKeyForDepartment(KEY_PREPRINTED, layoutDept, false);
        int topMargin = longValue(KEY_TOP_MARGIN, layoutDept, DEFAULT_TOP_MARGIN).intValue();
        boolean emitEscP = forPrinter && configOptionApplicationController
                .getBooleanValueByKey(KEY_EMIT_ESCP, true);
        int width = InwardReceiptTextRenderer.clampWidth(
                longValue(KEY_LINE_WIDTH, layoutDept, InwardReceiptTextRenderer.WIDTH));
        boolean admissionType = configOptionApplicationController
                .getBooleanValueByKeyForDepartment(KEY_SHOW_ADMISSION_TYPE, layoutDept, true);
        boolean patientAddress = configOptionApplicationController
                .getBooleanValueByKeyForDepartment(KEY_SHOW_PATIENT_ADDRESS, layoutDept, true);
        boolean patientPhone = configOptionApplicationController
                .getBooleanValueByKeyForDepartment(KEY_SHOW_PATIENT_PHONE, layoutDept, true);

        List<Payment> multiplePayments = bill.getPaymentMethod() == PaymentMethod.MultiplePaymentMethods
                ? billService.fetchBillPayments(bill) : null;
        return InwardReceiptTextRenderer.render(bill,
                InwardReceiptTextRenderer.headingFor(headingType),
                duplicate, preprinted, topMargin, emitEscP, multiplePayments,
                admissionType, patientAddress, patientPhone, width);
    }

    /**
     * Approximate on-screen preview of the .prn receipt: the same text
     * without printer control codes. Empty when there is no saved bill.
     */
    public String previewText(Bill bill, BillTypeAtomic headingType, boolean duplicate, Department layoutDept) {
        if (bill == null || bill.getId() == null) {
            return "";
        }
        return renderReceipt(bill, headingType, duplicate, layoutDept, false);
    }

    /**
     * Streams the receipt as a .prn download for the client print agent.
     *
     * @param fileNamePrefix e.g. "inward-deposit-"; the bill number and
     * ".prn" are appended
     */
    public void streamReceipt(Bill bill, BillTypeAtomic headingType, boolean duplicate,
            Department layoutDept, String fileNamePrefix) {
        if (bill == null || bill.getId() == null) {
            JsfUtil.addErrorMessage("No saved receipt to print.");
            return;
        }
        String text = renderReceipt(bill, headingType, duplicate, layoutDept, true);
        String fileName = fileNamePrefix
                + (bill.getDeptId() == null ? String.valueOf(bill.getId())
                        : bill.getDeptId().replaceAll("[^A-Za-z0-9._-]", "_"))
                + ".prn";

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        try (OutputStream os = response.getOutputStream()) {
            // ISO-8859-1 so ESC/P control bytes (0x1B, 0x0C) pass through unchanged.
            os.write(text.getBytes(Charset.forName("ISO-8859-1")));
            os.flush();
        } catch (IOException e) {
            JsfUtil.addErrorMessage("Could not generate the raw text receipt: " + e.getMessage());
        }
        context.responseComplete();
    }

    /**
     * Downloads the client print agent (script, launcher, example config,
     * README) as one zip, for one-time setup of a cashier PC.
     */
    public void streamPrintAgentZip() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext ec = context.getExternalContext();
        Set<String> paths = ec.getResourcePaths(PRINT_AGENT_DIR);
        if (paths == null || paths.isEmpty()) {
            JsfUtil.addErrorMessage("The print agent is not included in this installation.");
            return;
        }
        HttpServletResponse response = (HttpServletResponse) ec.getResponse();
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"hmis-print-agent.zip\"");
        try (ZipOutputStream zip = new ZipOutputStream(response.getOutputStream())) {
            for (String path : new TreeSet<>(paths)) {
                if (path.endsWith("/")) {
                    continue;
                }
                try (InputStream in = ec.getResourceAsStream(path)) {
                    if (in == null) {
                        continue;
                    }
                    zip.putNextEntry(new ZipEntry(path.substring(PRINT_AGENT_DIR.length())));
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        zip.write(buf, 0, n);
                    }
                    zip.closeEntry();
                }
            }
        } catch (IOException e) {
            JsfUtil.addErrorMessage("Could not prepare the print agent download: " + e.getMessage());
        }
        context.responseComplete();
    }

    /**
     * Saves the Settings dialog's raw text values for the logged department.
     */
    public void saveSettings() {
        if (!webUserController.hasPrivilege("ChangeReceiptPrintingPaperTypes")) {
            JsfUtil.addErrorMessage("You are not authorised to change print settings.");
            return;
        }
        Department dept = sessionController.getDepartment();
        if (dept == null) {
            JsfUtil.addErrorMessage("Select a department first.");
            return;
        }
        long margin = topMarginLines == null ? DEFAULT_TOP_MARGIN : Math.max(0L, Math.min(40L, topMarginLines));
        long width = InwardReceiptTextRenderer.clampWidth(
                lineWidth == null ? InwardReceiptTextRenderer.WIDTH : lineWidth);
        configOptionApplicationController.setBooleanValueByKeyForDepartment(KEY_RAW_TEXT_PAPER, dept, rawTextPaper);
        configOptionApplicationController.setBooleanValueByKeyForDepartment(KEY_PREPRINTED, dept, preprintedStationery);
        configOptionApplicationController.setLongValueByKeyForDepartment(KEY_TOP_MARGIN, dept, margin);
        configOptionApplicationController.setLongValueByKeyForDepartment(KEY_LINE_WIDTH, dept, width);
        configOptionApplicationController.setBooleanValueByKeyForDepartment(KEY_SHOW_ADMISSION_TYPE, dept, showAdmissionType);
        configOptionApplicationController.setBooleanValueByKeyForDepartment(KEY_SHOW_PATIENT_ADDRESS, dept, showPatientAddress);
        configOptionApplicationController.setBooleanValueByKeyForDepartment(KEY_SHOW_PATIENT_PHONE, dept, showPatientPhone);
        JsfUtil.addSuccessMessage("Raw text print settings saved for " + dept.getName());
    }

    private Long longValue(String key, Department dept, long def) {
        Long v = configOptionApplicationController.getLongValueByKeyForDepartment(key, dept, def);
        return v == null ? def : v;
    }

    public boolean isRawTextPaper() {
        return rawTextPaper;
    }

    public void setRawTextPaper(boolean rawTextPaper) {
        this.rawTextPaper = rawTextPaper;
    }

    public boolean isPreprintedStationery() {
        return preprintedStationery;
    }

    public void setPreprintedStationery(boolean preprintedStationery) {
        this.preprintedStationery = preprintedStationery;
    }

    public Long getTopMarginLines() {
        return topMarginLines;
    }

    public void setTopMarginLines(Long topMarginLines) {
        this.topMarginLines = topMarginLines;
    }

    public Long getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(Long lineWidth) {
        this.lineWidth = lineWidth;
    }

    public boolean isShowAdmissionType() {
        return showAdmissionType;
    }

    public void setShowAdmissionType(boolean showAdmissionType) {
        this.showAdmissionType = showAdmissionType;
    }

    public boolean isShowPatientAddress() {
        return showPatientAddress;
    }

    public void setShowPatientAddress(boolean showPatientAddress) {
        this.showPatientAddress = showPatientAddress;
    }

    public boolean isShowPatientPhone() {
        return showPatientPhone;
    }

    public void setShowPatientPhone(boolean showPatientPhone) {
        this.showPatientPhone = showPatientPhone;
    }
}
