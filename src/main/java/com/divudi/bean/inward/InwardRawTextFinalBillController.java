package com.divudi.bean.inward;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.util.InwardFinalBillTextRenderer;
import com.divudi.core.util.InwardFinalBillTextRenderer.Data;
import com.divudi.core.util.InwardFinalBillTextRenderer.Row;
import com.divudi.core.util.JsfUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;

/**
 * Raw text (.prn) printing of the inward Final Bill (Custom Bill 3 grouping)
 * and Interim Bill on pre-printed continuous stationery. Reuses the Inward Raw
 * Text machinery: the per-department "Inward Payment Bill Raw Text Paper"
 * switch gates the buttons, and the .prn is picked up by the client print agent.
 * Form length, top margin and line width are department ConfigOptions.
 */
@Named
@RequestScoped
public class InwardRawTextFinalBillController implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String KEY_TOP_MARGIN = "Inward Raw Text Final Bill Top Margin Lines";
    public static final String KEY_LINE_WIDTH = "Inward Raw Text Final Bill Line Width";
    public static final String KEY_FORM_LENGTH = "Inward Raw Text Final Bill Form Length Lines";
    public static final String KEY_INTERIM_TITLE = "Inward Raw Text Interim Bill Title";

    private static final long DEFAULT_TOP_MARGIN = 8L;
    private static final long DEFAULT_FORM_LENGTH = 33L; // 5.5 inch form at 6 lines per inch
    private static final String DEFAULT_INTERIM_TITLE = "INTERIM BILL";
    private static final TimeZone COLOMBO = TimeZone.getTimeZone("Asia/Colombo");

    @Inject
    private SessionController sessionController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private BhtSummeryController bhtSummeryController;

    public void streamFinalBill(Bill bill) {
        stream(bill, false, "inward-final-bill-");
    }

    public void streamInterimBill(Bill bill) {
        stream(bill, true, "inward-interim-bill-");
    }

    /** The raw text of the bill, for on-screen checks; no printer codes. */
    public String previewText(Bill bill, boolean interim) {
        if (bill == null) {
            return "";
        }
        return render(bill, interim, false);
    }

    private void stream(Bill bill, boolean interim, String fileNamePrefix) {
        if (bill == null || bill.getId() == null) {
            JsfUtil.addErrorMessage("No saved bill to print.");
            return;
        }
        String text = render(bill, interim, configOptionApplicationController
                .getBooleanValueByKey(InwardRawTextReceiptController.KEY_EMIT_ESCP, true));
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
            JsfUtil.addErrorMessage("Could not generate the raw text bill: " + e.getMessage());
        }
        context.responseComplete();
    }

    private String render(Bill bill, boolean interim, boolean emitEscP) {
        Department dept = sessionController.getDepartment();
        int topMargin = (int) longValue(KEY_TOP_MARGIN, dept, DEFAULT_TOP_MARGIN);
        int width = InwardFinalBillTextRenderer.clampWidth(
                longValue(KEY_LINE_WIDTH, dept, InwardFinalBillTextRenderer.DEFAULT_WIDTH));
        int formLength = (int) longValue(KEY_FORM_LENGTH, dept, DEFAULT_FORM_LENGTH);
        return InwardFinalBillTextRenderer.render(buildData(bill, interim), width, topMargin, formLength, emitEscP);
    }

    /** Collects what the printed form shows, grouping charges exactly as Custom Bill 3 does. */
    Data buildData(Bill bill, boolean interim) {
        DecimalFormat money = new DecimalFormat("#,##0.00");
        Data d = new Data();
        PatientEncounter pe = bill.getPatientEncounter();

        String admissionType = pe != null && pe.getAdmissionType() != null && pe.getAdmissionType().getName() != null
                ? pe.getAdmissionType().getName().trim() : "";
        d.title = interim
                ? configOptionApplicationController.getShortTextValueByKey(KEY_INTERIM_TITLE, DEFAULT_INTERIM_TITLE)
                : admissionType;
        d.billNo = bill.getDeptId();
        d.bhtNo = pe == null ? "" : pe.getBhtNo();
        if (bill.getCreatedAt() != null) {
            SimpleDateFormat date = new SimpleDateFormat("dd/MMM/yyyy", Locale.ENGLISH);
            SimpleDateFormat time = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
            date.setTimeZone(COLOMBO);
            time.setTimeZone(COLOMBO);
            d.billDate = date.format(bill.getCreatedAt());
            d.billTime = time.format(bill.getCreatedAt()).toLowerCase(Locale.ROOT);
        }
        if (pe != null && pe.getPatient() != null && pe.getPatient().getPerson() != null) {
            d.patientName = pe.getPatient().getPerson().getNameWithTitle();
            d.nic = pe.getPatient().getPerson().getNic();
        }
        if (bill.getCreater() != null) {
            d.preparedBy = bill.getCreater().getWebUserPerson() != null
                    && bill.getCreater().getWebUserPerson().getName() != null
                            ? bill.getCreater().getWebUserPerson().getName() : bill.getCreater().getName();
        }

        if (interim) {
            for (BillItem bi : bill.getBillItems()) {
                if (bi.getAdjustedValue() != 0) {
                    String label = bi.getDescreption() != null && !bi.getDescreption().trim().isEmpty()
                            ? bi.getDescreption()
                            : bi.getInwardChargeType() == null ? "" : bi.getInwardChargeType().getName();
                    d.charges.add(new Row(label, money.format(bi.getAdjustedValue())));
                }
            }
            d.showDiscounts = false;
            d.totalAmount = money.format(bill.getTotal());
            d.discounts = money.format(0.0);
            d.netAmount = money.format(bill.getTotal() - bill.getPaidAmount());
        } else {
            for (Map.Entry<String, Double> e : bhtSummeryController.getCustom3CategoryTotals(bill)) {
                d.charges.add(new Row(e.getKey(), money.format(e.getValue())));
            }
            addProfessionalRows(d, bill, money);
            d.totalAmount = money.format(bill.getTotal());
            d.discounts = money.format(bill.getDiscount());
            d.netAmount = money.format(bill.getNetTotal() - bill.getPaidAmount());
        }
        d.deposit = money.format(bhtSummeryController.getCustom3DepositTotal(bill));
        addPayments(d, bill, money);
        return d;
    }

    /** Per-doctor lines, as Custom Bill 3 lists them under the charge groups. */
    private void addProfessionalRows(Data d, Bill bill, DecimalFormat money) {
        for (BillItem bip : bill.getBillItems()) {
            if (bip.getInwardChargeType() != InwardChargeType.ProfessionalCharge || bip.getProFees() == null) {
                continue;
            }
            for (BillFee fe : bip.getProFees()) {
                if (fe.getBill() == null || fe.getBill().isCancelled()
                        || !"class com.divudi.core.entity.BilledBill".equals(fe.getBill().getBillClass())
                        || !(fe.getFeeAdjusted() != 0 || fe.isFreeOfCharge())) {
                    continue;
                }
                String name = fe.getStaff() == null || fe.getStaff().getPerson() == null ? ""
                        : fe.getStaff().getPerson().getNameWithTitle();
                if (fe.getStaff() != null && fe.getStaff().getSpeciality() != null) {
                    name = name + " (" + fe.getStaff().getSpeciality().getName() + ")";
                }
                String amount = fe.isFreeOfCharge() && fe.getFeeAdjusted() == 0
                        ? configOptionApplicationController.getShortTextValueByKey(
                                "Inward Final Bill - Label For Free Professional Fee", "Free of Charge")
                        : money.format(fe.getFeeAdjusted());
                d.charges.add(new Row(name, amount));
            }
            if (bip.getUnattributedProfessionalFeeValue() >= 0.01) {
                d.charges.add(new Row(configOptionApplicationController.getShortTextValueByKey(
                        "Inward Final Bill - Label For Other Professional Charges", "Other Professional Charges"),
                        money.format(bip.getUnattributedProfessionalFeeValue())));
            }
        }
    }

    /** Admission deposits and payments summed per payment method. */
    private void addPayments(Data d, Bill bill, DecimalFormat money) {
        Map<String, Double> byMethod = new LinkedHashMap<>();
        List<Bill> paid = bhtSummeryController.getPatientPaymentBillsForFinalBill(bill);
        for (Bill b : paid) {
            if (b.getNetTotal() == 0) {
                continue;
            }
            PaymentMethod pm = b.getPaymentMethod();
            byMethod.merge(pm == null ? "Other" : pm.getLabel(), b.getNetTotal(), Double::sum);
        }
        for (Map.Entry<String, Double> e : byMethod.entrySet()) {
            d.payments.add(new Row(e.getKey(), money.format(e.getValue())));
        }
    }

    private long longValue(String key, Department dept, long def) {
        Long v = configOptionApplicationController.getLongValueByKeyForDepartment(key, dept, def);
        return v == null ? def : v;
    }
}
