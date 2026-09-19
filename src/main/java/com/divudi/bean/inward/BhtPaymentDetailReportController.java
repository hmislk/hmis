package com.divudi.bean.inward;

import com.divudi.bean.common.UserSettingsController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.BhtPaymentDetailDTO;
import com.divudi.core.data.inward.AdmissionStatus;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.util.JsfUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Controller for BHT Deposit and Credit Settlement Detail Report. One row per
 * individual transaction: a "Make a Deposit" (INWARD_DEPOSIT) payment, a "Make
 * a Payment" (INWARD_PAYMENT) payment, a "Post Final Payment"
 * (BillType.PostFinalBillInwardPayment) payment, or a CC settlement item - see
 * {@link com.divudi.core.data.dto.BhtPaymentDetailDTO#getPaymentCategory()}.
 * The three payment kinds are kept as separate categories and separate
 * per-method footer totals (issue #23262).
 */
@Named
@SessionScoped
public class BhtPaymentDetailReportController implements Serializable {

    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @Inject
    private UserSettingsController userSettingsController;

    private Date fromDate = startOfCurrentMonth();
    private Date toDate = endOfCurrentMonth();
    private String dateBasis = "dischargeDate";
    private AdmissionStatus admissionStatus = AdmissionStatus.DISCHARGED_AND_FINAL_BILL_COMPLETED;
    private AdmissionType admissionType;
    private PaymentMethod paymentMethod;
    /**
     * Restricts the report to one transaction category: {@code "Deposit"}
     * (INWARD_DEPOSIT rows) or {@code "Payment"} (INWARD_PAYMENT rows).
     * {@code null} means all categories, including Post Payment and CC
     * Settlement rows which this filter does not otherwise touch.
     */
    private String transactionType;
    private Institution institution;
    private Institution site;
    private Department department;

    private List<BhtPaymentDetailDTO> reportRows;
    /**
     * Snapshot of the filter description (period/status/type/etc., excluding
     * the "Generated" timestamp) at the moment {@link #generateReport()}
     * populated {@link #reportRows}. Used by the PDF export instead of
     * re-reading the live filter fields, which may have been changed on the
     * page (and pushed into these fields by the non-AJAX form submit) after
     * Generate was clicked but before PDF was clicked - without this
     * snapshot the exported PDF's header could describe different filters
     * than the rows it actually contains.
     */
    private String reportFilterDescription;
    private double grandTotal;
    private double grandTotalCcSettlement;
    private double grandTotalPayments;
    private double grandTotalPostPayments;
    /**
     * Ordered map of payment method → "Make a Deposit" (INWARD_DEPOSIT) total;
     * only methods with non-zero totals.
     */
    private Map<PaymentMethod, Double> depositTotalByMethod = new LinkedHashMap<>();
    /**
     * Deposit payment methods in the order they appeared, for UI iteration.
     */
    private List<PaymentMethod> usedDepositMethods = new ArrayList<>();
    /**
     * Ordered map of payment method → "Make a Payment" (INWARD_PAYMENT) total.
     * Kept separate from {@link #depositTotalByMethod} (deposits) and
     * {@link #postPaymentTotalByMethod} (post-final-bill payments) per issue
     * #23262 - deposit, payment and post-payment amounts must not be conflated
     * in a single per-method total.
     */
    private Map<PaymentMethod, Double> paymentTotalByMethod = new LinkedHashMap<>();
    private List<PaymentMethod> usedPaymentMethods = new ArrayList<>();
    /**
     * Ordered map of payment method → post-final-bill ("Post Final Payment")
     * total. Kept separate from deposits and payments per issue #23262.
     */
    private Map<PaymentMethod, Double> postPaymentTotalByMethod = new LinkedHashMap<>();
    private List<PaymentMethod> usedPostPaymentMethods = new ArrayList<>();

    public void generateReport() {
        reportRows = new ArrayList<>();
        grandTotal = 0;
        grandTotalCcSettlement = 0;
        grandTotalPayments = 0;
        grandTotalPostPayments = 0;
        depositTotalByMethod = new LinkedHashMap<>();
        usedDepositMethods = new ArrayList<>();
        paymentTotalByMethod = new LinkedHashMap<>();
        usedPaymentMethods = new ArrayList<>();
        postPaymentTotalByMethod = new LinkedHashMap<>();
        usedPostPaymentMethods = new ArrayList<>();

        List<PatientEncounter> encounters = fetchEncounters();
        if (encounters == null || encounters.isEmpty()) {
            return;
        }

        for (PatientEncounter enc : encounters) {
            String patientName = enc.getPatient() != null && enc.getPatient().getPerson() != null
                    ? enc.getPatient().getPerson().getNameWithTitle() : "";

            // "Make a Deposit" (INWARD_DEPOSIT) payments — one row per Payment record
            List<Payment> deposits = transactionType == null || "Deposit".equals(transactionType)
                    ? fetchDepositPayments(enc) : new ArrayList<>();
            for (Payment p : deposits) {
                BhtPaymentDetailDTO row = new BhtPaymentDetailDTO();
                row.setBhtNo(enc.getBhtNo());
                row.setPatientName(patientName);
                row.setAdmissionType(enc.getAdmissionType());
                row.setDateOfAdmission(enc.getDateOfAdmission());
                row.setDateOfDischarge(enc.getDateOfDischarge());
                row.setBillNo(p.getBill() != null ? p.getBill().getDeptId() : "");
                row.setCreatedAt(p.getCreatedAt());
                row.setPaymentMethod(p.getPaymentMethod());
                // Signed, not abs() - see fetchDepositPayments() javadoc: cancellations
                // arrive as separate negative-amount rows that must net out.
                row.setAmount(p.getPaidValue());
                row.setReferenceNo(p.getReferenceNo());
                row.setCreditCompanyName("");
                row.setPaymentCategory("Deposit");
                reportRows.add(row);
                double depositAmt = p.getPaidValue();
                grandTotal += depositAmt;
                if (p.getPaymentMethod() != null) {
                    depositTotalByMethod.merge(p.getPaymentMethod(), depositAmt, Double::sum);
                }
            }

            // "Make a Payment" (INWARD_PAYMENT) payments — one row per Payment
            // record. Issue #23262: kept a separate category from deposits and
            // from post-final-bill payments.
            List<Payment> payments = transactionType == null || "Payment".equals(transactionType)
                    ? fetchPayments(enc) : new ArrayList<>();
            for (Payment p : payments) {
                BhtPaymentDetailDTO row = new BhtPaymentDetailDTO();
                row.setBhtNo(enc.getBhtNo());
                row.setPatientName(patientName);
                row.setAdmissionType(enc.getAdmissionType());
                row.setDateOfAdmission(enc.getDateOfAdmission());
                row.setDateOfDischarge(enc.getDateOfDischarge());
                row.setBillNo(p.getBill() != null ? p.getBill().getDeptId() : "");
                row.setCreatedAt(p.getCreatedAt());
                row.setPaymentMethod(p.getPaymentMethod());
                // Signed, not abs() - see fetchPayments() javadoc: cancellations
                // arrive as separate negative-amount rows that must net out.
                row.setAmount(p.getPaidValue());
                row.setReferenceNo(p.getReferenceNo());
                row.setCreditCompanyName("");
                row.setPaymentCategory("Payment");
                reportRows.add(row);
                double paymentAmt = p.getPaidValue();
                grandTotal += paymentAmt;
                grandTotalPayments += paymentAmt;
                if (p.getPaymentMethod() != null) {
                    paymentTotalByMethod.merge(p.getPaymentMethod(), paymentAmt, Double::sum);
                }
            }

            // Post-final-bill ("Post Final Payment") payments — one row per Payment record.
            // Issue #23263: these were never queried here, so a BHT whose only
            // recorded payment was a post-final settlement (no deposit, no CC
            // settlement) was silently absent from this report.
            List<Payment> postPayments = transactionType == null
                    ? fetchPostFinalPayments(enc) : new ArrayList<>();
            for (Payment p : postPayments) {
                BhtPaymentDetailDTO row = new BhtPaymentDetailDTO();
                row.setBhtNo(enc.getBhtNo());
                row.setPatientName(patientName);
                row.setAdmissionType(enc.getAdmissionType());
                row.setDateOfAdmission(enc.getDateOfAdmission());
                row.setDateOfDischarge(enc.getDateOfDischarge());
                row.setBillNo(p.getBill() != null ? p.getBill().getDeptId() : "");
                row.setCreatedAt(p.getCreatedAt());
                row.setPaymentMethod(p.getPaymentMethod());
                // Signed, not abs() - see fetchPostFinalPayments() javadoc: a
                // cancelled post-final payment arrives as a separate
                // negative-amount row rather than a cancelled flag, so each row
                // must keep its real sign to show the full transaction trail.
                row.setAmount(p.getPaidValue());
                row.setReferenceNo(p.getReferenceNo());
                row.setCreditCompanyName("");
                row.setPaymentCategory("Post Payment");
                reportRows.add(row);
                grandTotal += p.getPaidValue();
                grandTotalPostPayments += p.getPaidValue();
                if (p.getPaymentMethod() != null) {
                    postPaymentTotalByMethod.merge(p.getPaymentMethod(), p.getPaidValue(), Double::sum);
                }
            }

            // CC settlement items — one row per BillItem
            List<BillItem> ccItems = transactionType == null
                    ? fetchCreditSettlementItems(enc) : new ArrayList<>();
            for (BillItem bi : ccItems) {
                String companyName = "";
                if (bi.getReferenceBill() != null && bi.getReferenceBill().getCreditCompany() != null) {
                    companyName = bi.getReferenceBill().getCreditCompany().getName();
                }
                BhtPaymentDetailDTO row = new BhtPaymentDetailDTO();
                row.setBhtNo(enc.getBhtNo());
                row.setPatientName(patientName);
                row.setAdmissionType(enc.getAdmissionType());
                row.setDateOfAdmission(enc.getDateOfAdmission());
                row.setDateOfDischarge(enc.getDateOfDischarge());
                row.setBillNo(bi.getBill() != null ? bi.getBill().getDeptId() : "");
                row.setCreatedAt(bi.getCreatedAt());
                row.setPaymentMethod(null);
                row.setAmount(bi.getNetValue());
                row.setReferenceNo("");
                row.setCreditCompanyName(companyName);
                row.setPaymentCategory("CC Settlement");
                reportRows.add(row);
                grandTotal += bi.getNetValue();
                grandTotalCcSettlement += bi.getNetValue();
            }
        }

        // Build ordered lists of used payment methods for UI iteration
        usedDepositMethods = new ArrayList<>(depositTotalByMethod.keySet());
        usedPaymentMethods = new ArrayList<>(paymentTotalByMethod.keySet());
        usedPostPaymentMethods = new ArrayList<>(postPaymentTotalByMethod.keySet());

        // Snapshot the filters that produced reportRows - see the field
        // javadoc on reportFilterDescription for why this must not be
        // recomputed from the (possibly since-changed) live filter fields.
        reportFilterDescription = buildFilterDescription(new SimpleDateFormat("dd/MM/yyyy hh:mm a"));
    }

    public double getTotalForDepositMethod(PaymentMethod pm) {
        return depositTotalByMethod.getOrDefault(pm, 0.0);
    }

    public double getTotalForPaymentMethod(PaymentMethod pm) {
        return paymentTotalByMethod.getOrDefault(pm, 0.0);
    }

    public double getTotalForPostPaymentMethod(PaymentMethod pm) {
        return postPaymentTotalByMethod.getOrDefault(pm, 0.0);
    }

    private List<PatientEncounter> fetchEncounters() {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder(
                "select distinct c from PatientEncounter c where c.retired = false");

        if (fromDate != null && toDate != null) {
            boolean useAdmissionDate = "admissionDate".equals(dateBasis)
                    || admissionStatus == AdmissionStatus.ADMITTED_BUT_NOT_DISCHARGED;
            if (useAdmissionDate) {
                jpql.append(" and c.dateOfAdmission between :fromDate and :toDate");
            } else {
                jpql.append(" and c.dateOfDischarge between :fromDate and :toDate");
            }
            params.put("fromDate", fromDate);
            params.put("toDate", toDate);
        }

        if (admissionStatus != null && admissionStatus != AdmissionStatus.ANY_STATUS) {
            switch (admissionStatus) {
                case ADMITTED_BUT_NOT_DISCHARGED:
                    jpql.append(" and c.discharged = :dis");
                    params.put("dis", false);
                    break;
                case DISCHARGED_BUT_FINAL_BILL_NOT_COMPLETED:
                    jpql.append(" and c.discharged = :dis and c.paymentFinalized = :pf");
                    params.put("dis", true);
                    params.put("pf", false);
                    break;
                case DISCHARGED_AND_FINAL_BILL_COMPLETED:
                    jpql.append(" and c.discharged = :dis and c.paymentFinalized = :pf");
                    params.put("dis", true);
                    params.put("pf", true);
                    break;
                default:
                    break;
            }
        }

        if (admissionType != null) {
            jpql.append(" and c.admissionType = :admType");
            params.put("admType", admissionType);
        }
        if (institution != null) {
            jpql.append(" and c.institution = :ins");
            params.put("ins", institution);
        }
        if (site != null) {
            jpql.append(" and c.department.site = :site");
            params.put("site", site);
        }
        if (department != null) {
            jpql.append(" and c.department = :dept");
            params.put("dept", department);
        }
        jpql.append(" order by c.bhtNo");
        return patientEncounterFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
    }

    /**
     * Fetch "Make a Deposit" (INWARD_DEPOSIT) payments for this encounter.
     *
     * Matches BOTH {@code BillTypeAtomic.INWARD_DEPOSIT} and
     * {@code BillTypeAtomic.INWARD_DEPOSIT_CANCELLATION}, and deliberately does
     * NOT filter on {@code p.cancelled} / {@code p.bill.cancelled}: when a
     * deposit is cancelled, HMIS sets {@code cancelled=true} on the original
     * bill and creates a companion reversal Bill+Payment with an inverted
     * (negative) amount under the CANCELLATION billTypeAtomic, rather than
     * flagging the original row. Filtering to a single billTypeAtomic and
     * excluding cancelled bills would make a cancelled deposit vanish from this
     * report with no trace it ever happened. Including both rows and keeping
     * each row's natural sign (no {@code Math.abs()} in the caller) lets the
     * per-method totals net out correctly. Mirrors
     * {@link #fetchPostFinalPayments} and {@link #fetchCreditSettlementItems}.
     */
    private List<Payment> fetchDepositPayments(PatientEncounter enc) {
        StringBuilder jpql = new StringBuilder("select p from Payment p"
                + " where p.retired = false"
                + " and p.bill.retired = false"
                + " and p.bill.billTypeAtomic in :btas"
                + " and p.bill.patientEncounter = :enc");
        Map<String, Object> params = new HashMap<>();
        params.put("btas", Arrays.asList(
                BillTypeAtomic.INWARD_DEPOSIT,
                BillTypeAtomic.INWARD_DEPOSIT_CANCELLATION));
        params.put("enc", enc);
        if (paymentMethod != null) {
            jpql.append(" and p.paymentMethod = :pm");
            params.put("pm", paymentMethod);
        }
        jpql.append(" order by p.createdAt");
        return paymentFacade.findByJpql(jpql.toString(), params);
    }

    /**
     * Fetch "Make a Payment" (INWARD_PAYMENT) payments for this encounter -
     * payments toward the bill made any time during the stay. Kept separate
     * from deposits (INWARD_DEPOSIT) and post-final-bill payments
     * (BillType.PostFinalBillInwardPayment). Issue #23262.
     *
     * Matches BOTH {@code BillTypeAtomic.INWARD_PAYMENT} and
     * {@code BillTypeAtomic.INWARD_PAYMENT_CANCELLATION}, and deliberately does
     * NOT filter on {@code p.cancelled} / {@code p.bill.cancelled}: when a
     * payment is cancelled, HMIS sets {@code cancelled=true} on the original
     * bill and creates a companion reversal Bill+Payment with an inverted
     * (negative) amount under the CANCELLATION billTypeAtomic, rather than
     * flagging the original row. Filtering to a single billTypeAtomic and
     * excluding cancelled bills would make a cancelled payment vanish from this
     * report with no trace it ever happened. Including both rows and keeping
     * each row's natural sign (no {@code Math.abs()} in the caller) lets the
     * per-method totals net out correctly. Mirrors
     * {@link #fetchPostFinalPayments} and {@link #fetchCreditSettlementItems}.
     */
    private List<Payment> fetchPayments(PatientEncounter enc) {
        StringBuilder jpql = new StringBuilder("select p from Payment p"
                + " where p.retired = false"
                + " and p.bill.retired = false"
                + " and p.bill.billTypeAtomic in :btas"
                + " and p.bill.patientEncounter = :enc");
        Map<String, Object> params = new HashMap<>();
        params.put("btas", Arrays.asList(
                BillTypeAtomic.INWARD_PAYMENT,
                BillTypeAtomic.INWARD_PAYMENT_CANCELLATION));
        params.put("enc", enc);
        if (paymentMethod != null) {
            jpql.append(" and p.paymentMethod = :pm");
            params.put("pm", paymentMethod);
        }
        jpql.append(" order by p.createdAt");
        return paymentFacade.findByJpql(jpql.toString(), params);
    }

    /**
     * Fetch post-final-bill ("Make Payment") payments for this encounter.
     *
     * Deliberately does NOT filter on {@code p.cancelled} /
     * {@code p.bill.cancelled}: mirrors
     * {@link BhtPaymentSummaryReportController#fetchPostFinalPayments} -
     * cancellation of a post-final-bill payment arrives as a separate
     * negative-amount row of the same bill type rather than a cancelled flag on
     * the original, so each row is kept as its own line (with its natural sign)
     * instead of being filtered or netted here. Issue #23263.
     */
    private List<Payment> fetchPostFinalPayments(PatientEncounter enc) {
        StringBuilder jpql = new StringBuilder("select p from Payment p"
                + " where p.retired = false"
                + " and p.bill.retired = false"
                + " and p.bill.billType = :bt"
                + " and p.bill.patientEncounter = :enc");
        Map<String, Object> params = new HashMap<>();
        params.put("bt", BillType.PostFinalBillInwardPayment);
        params.put("enc", enc);
        if (paymentMethod != null) {
            jpql.append(" and p.paymentMethod = :pm");
            params.put("pm", paymentMethod);
        }
        jpql.append(" order by p.createdAt");
        return paymentFacade.findByJpql(jpql.toString(), params);
    }

    /**
     * Fetch BillItems from INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED and
     * INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION bills that reference this
     * encounter. Deliberately does NOT filter on bi.bill.cancelled: cancelling
     * a CC payment sets cancelled=true on the original RECEIVED bill while its
     * negative-value items live on a separate CANCELLATION bill, so filtering
     * by cancelled would drop the original positive row and leave only the
     * negative one. Including both rows with their signed netValue preserves
     * the audit trail and nets out correctly.
     */
    private List<BillItem> fetchCreditSettlementItems(PatientEncounter enc) {
        String jpql = "select bi from BillItem bi"
                + " where bi.retired = false"
                + " and bi.bill.retired = false"
                + " and bi.bill.billTypeAtomic in :btas"
                + " and bi.patientEncounter = :enc"
                + " order by bi.createdAt";
        Map<String, Object> params = new HashMap<>();
        params.put("btas", Arrays.asList(
                BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED,
                BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION));
        params.put("enc", enc);
        return billItemFacade.findByJpql(jpql, params);
    }

    /**
     * Hand-built PDF export (issue #23445), mirroring
     * InwardReportControllerBht.downloadProfessionalPaymentSummaryPdf(): the
     * default {@code p:dataExporter type="pdf"} divides a PrimeFaces
     * DataTable's columns into EQUAL widths regardless of content (confirmed
     * by decompiling DataTablePDFExporter - it never calls
     * PdfPTable.setWidths()), so this report's long values (patient names,
     * "WARD/INWCAN/44"-style bill numbers) wrapped into an unreadable mess of
     * single/few characters per line. Building the PdfPTable directly lets us
     * set weighted column widths that actually fit the content, and only for
     * the columns currently toggled visible via "Configure Columns".
     */
    public void downloadBhtPaymentSummaryPdf() {
        if (reportRows == null || reportRows.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please generate the report first.");
            return;
        }

        List<String> columnKeys = new ArrayList<>();
        columnKeys.add("bhtNo");
        columnKeys.add("patientName");
        if (userSettingsController.isInwardBhtPaymentSummaryAdmissionTypeVisible()) {
            columnKeys.add("admissionType");
        }
        if (userSettingsController.isInwardBhtPaymentSummaryAdmittedVisible()) {
            columnKeys.add("admitted");
        }
        if (userSettingsController.isInwardBhtPaymentSummaryDischargedVisible()) {
            columnKeys.add("discharged");
        }
        if (userSettingsController.isInwardBhtPaymentSummaryTypeVisible()) {
            columnKeys.add("type");
        }
        if (userSettingsController.isInwardBhtPaymentSummaryBillNoVisible()) {
            columnKeys.add("billNo");
        }
        if (userSettingsController.isInwardBhtPaymentSummaryDateTimeVisible()) {
            columnKeys.add("dateTime");
        }
        if (userSettingsController.isInwardBhtPaymentSummaryPaymentMethodVisible()) {
            columnKeys.add("paymentMethod");
        }
        if (userSettingsController.isInwardBhtPaymentSummaryAmountVisible()) {
            columnKeys.add("amount");
        }
        if (userSettingsController.isInwardBhtPaymentSummaryReferenceNoVisible()) {
            columnKeys.add("referenceNo");
        }
        if (userSettingsController.isInwardBhtPaymentSummaryCreditCompanyVisible()) {
            columnKeys.add("creditCompany");
        }

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("bhtNo", "BHT No");
        headers.put("patientName", "Patient Name");
        headers.put("admissionType", "Admission Type");
        headers.put("admitted", "Admitted");
        headers.put("discharged", "Discharged");
        headers.put("type", "Type");
        headers.put("billNo", "Bill No");
        headers.put("dateTime", "Date / Time");
        headers.put("paymentMethod", "Payment Method");
        headers.put("amount", "Amount");
        headers.put("referenceNo", "Reference No");
        headers.put("creditCompany", "Credit Company");

        Map<String, Float> widths = new HashMap<>();
        widths.put("bhtNo", 3f);
        widths.put("patientName", 6f);
        widths.put("admissionType", 3f);
        widths.put("admitted", 2.5f);
        widths.put("discharged", 2.5f);
        widths.put("type", 2.5f);
        widths.put("billNo", 4f);
        widths.put("dateTime", 3.5f);
        widths.put("paymentMethod", 3f);
        widths.put("amount", 3f);
        widths.put("referenceNo", 3f);
        widths.put("creditCompany", 4f);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();

            String fileName = "BHT_Payment_Summary_"
                    + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".pdf";

            Document document = new Document(PageSize.A4.rotate(), 20f, 20f, 30f, 20f);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY);
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            DecimalFormat df = new DecimalFormat("#,##0.00");
            // SimpleDateFormat is not thread-safe - instantiate per export
            // rather than sharing static instances across concurrent requests.
            SimpleDateFormat shortDateFmt = new SimpleDateFormat("dd/MM/yyyy");
            SimpleDateFormat dateTimeFmt = new SimpleDateFormat("dd/MM/yyyy hh:mm a");

            Paragraph titlePara = new Paragraph("BHT Deposit and Credit Settlement Summary", titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(4f);
            document.add(titlePara);

            Paragraph metaPara = new Paragraph(buildFilterSummary(dateTimeFmt), metaFont);
            metaPara.setAlignment(Element.ALIGN_CENTER);
            metaPara.setSpacingAfter(10f);
            document.add(metaPara);

            PdfPTable table = new PdfPTable(columnKeys.size());
            table.setWidthPercentage(100);
            float[] widthArr = new float[columnKeys.size()];
            for (int c = 0; c < columnKeys.size(); c++) {
                widthArr[c] = widths.get(columnKeys.get(c));
            }
            table.setWidths(widthArr);
            table.setHeaderRows(1);

            for (String key : columnKeys) {
                PdfPCell cell = new PdfPCell(new Phrase(headers.get(key), headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(new Color(230, 230, 230));
                table.addCell(cell);
            }

            for (BhtPaymentDetailDTO row : reportRows) {
                for (String key : columnKeys) {
                    switch (key) {
                        case "bhtNo":
                            table.addCell(new Phrase(row.getBhtNo() != null ? row.getBhtNo() : "", normalFont));
                            break;
                        case "patientName":
                            table.addCell(new Phrase(row.getPatientName() != null ? row.getPatientName() : "", normalFont));
                            break;
                        case "admissionType":
                            table.addCell(new Phrase(row.getAdmissionType() != null ? row.getAdmissionType().getName() : "", normalFont));
                            break;
                        case "admitted":
                            table.addCell(new Phrase(row.getDateOfAdmission() != null ? shortDateFmt.format(row.getDateOfAdmission()) : "", normalFont));
                            break;
                        case "discharged":
                            table.addCell(new Phrase(row.getDateOfDischarge() != null ? shortDateFmt.format(row.getDateOfDischarge()) : "", normalFont));
                            break;
                        case "type":
                            table.addCell(new Phrase(row.getPaymentCategory() != null ? row.getPaymentCategory() : "", normalFont));
                            break;
                        case "billNo":
                            table.addCell(new Phrase(row.getBillNo() != null ? row.getBillNo() : "", normalFont));
                            break;
                        case "dateTime":
                            table.addCell(new Phrase(row.getCreatedAt() != null ? dateTimeFmt.format(row.getCreatedAt()) : "", normalFont));
                            break;
                        case "paymentMethod":
                            table.addCell(new Phrase(row.getPaymentMethod() != null ? row.getPaymentMethod().getLabel() : "CC Settlement", normalFont));
                            break;
                        case "amount": {
                            PdfPCell amountCell = new PdfPCell(new Phrase(df.format(row.getAmount()), normalFont));
                            amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                            table.addCell(amountCell);
                            break;
                        }
                        case "referenceNo":
                            table.addCell(new Phrase(row.getReferenceNo() != null ? row.getReferenceNo() : "", normalFont));
                            break;
                        case "creditCompany":
                            table.addCell(new Phrase(row.getCreditCompanyName() != null ? row.getCreditCompanyName() : "", normalFont));
                            break;
                        default:
                            table.addCell(new Phrase("", normalFont));
                            break;
                    }
                }
            }

            document.add(table);

            Paragraph footerPara = new Paragraph(buildTotalsFooter(df), FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY));
            footerPara.setSpacingBefore(10f);
            document.add(footerPara);

            document.close();

            byte[] pdfBytes = baos.toByteArray();
            externalContext.responseReset();
            externalContext.setResponseContentType("application/pdf");
            externalContext.setResponseContentLength(pdfBytes.length);
            externalContext.setResponseHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

            OutputStream out = externalContext.getResponseOutputStream();
            out.write(pdfBytes);
            out.flush();
            facesContext.responseComplete();
        } catch (Exception e) {
            Logger.getLogger(BhtPaymentDetailReportController.class.getName()).log(Level.SEVERE, "Error exporting BHT payment summary to PDF", e);
            JsfUtil.addErrorMessage("Failed to generate PDF: " + e.getMessage());
        }
    }

    private String buildTotalsFooter(DecimalFormat df) {
        StringBuilder sb = new StringBuilder();
        sb.append("Deposits - ");
        for (PaymentMethod pm : usedDepositMethods) {
            sb.append(pm.getLabel()).append(": ").append(df.format(getTotalForDepositMethod(pm))).append("   ");
        }
        sb.append("   Payments - ");
        for (PaymentMethod pm : usedPaymentMethods) {
            sb.append(pm.getLabel()).append(": ").append(df.format(getTotalForPaymentMethod(pm))).append("   ");
        }
        sb.append("Total Payments: ").append(df.format(grandTotalPayments));
        sb.append("   Post Payments - ");
        for (PaymentMethod pm : usedPostPaymentMethods) {
            sb.append(pm.getLabel()).append(": ").append(df.format(getTotalForPostPaymentMethod(pm))).append("   ");
        }
        sb.append("Total Post Payments: ").append(df.format(grandTotalPostPayments));
        sb.append("   CC Settlement: ").append(df.format(grandTotalCcSettlement));
        sb.append("   Grand Total: ").append(df.format(grandTotal));
        return sb.toString();
    }

    /**
     * Builds the PDF header's meta line from the snapshot taken by
     * {@link #generateReport()}, plus a "Generated" timestamp for the
     * current export - never re-reads the live filter fields (see
     * {@link #reportFilterDescription}'s javadoc).
     */
    private String buildFilterSummary(SimpleDateFormat headerDateFmt) {
        return reportFilterDescription + "\nGenerated: " + headerDateFmt.format(new Date());
    }

    /**
     * Describes the filters used for the just-executed query (period, status,
     * type, etc.) - called once from {@link #generateReport()} and cached in
     * {@link #reportFilterDescription}.
     */
    private String buildFilterDescription(SimpleDateFormat headerDateFmt) {
        StringBuilder sb = new StringBuilder();
        // Mirrors fetchEncounters()'s useAdmissionDate condition so the label
        // always names the date field the query actually filtered on.
        boolean useAdmissionDate = "admissionDate".equals(dateBasis)
                || admissionStatus == AdmissionStatus.ADMITTED_BUT_NOT_DISCHARGED;
        sb.append(useAdmissionDate ? "Admission Period: " : "Discharge Period: ")
                .append(fromDate != null ? headerDateFmt.format(fromDate) : "N/A")
                .append(" - ")
                .append(toDate != null ? headerDateFmt.format(toDate) : "N/A");

        if (admissionStatus != null) {
            sb.append("  |  Status: ").append(admissionStatus.getLabel());
        }
        if (admissionType != null) {
            sb.append("  |  Admission Type: ").append(admissionType.getName());
        }
        if (transactionType != null) {
            sb.append("  |  Transaction Type: ").append(transactionType);
        }
        if (paymentMethod != null) {
            sb.append("  |  Payment Method: ").append(paymentMethod.getLabel());
        }
        if (institution != null) {
            sb.append("  |  Institution: ").append(institution.getName());
        }
        if (site != null) {
            sb.append("  |  Site: ").append(site.getName());
        }
        if (department != null) {
            sb.append("  |  Department: ").append(department.getName());
        }
        return sb.toString();
    }

    public void makeNull() {
        fromDate = startOfCurrentMonth();
        toDate = endOfCurrentMonth();
        dateBasis = "dischargeDate";
        admissionStatus = AdmissionStatus.DISCHARGED_AND_FINAL_BILL_COMPLETED;
        admissionType = null;
        paymentMethod = null;
        transactionType = null;
        institution = null;
        site = null;
        department = null;
        reportRows = null;
        reportFilterDescription = null;
        grandTotal = 0;
        grandTotalCcSettlement = 0;
        grandTotalPayments = 0;
        grandTotalPostPayments = 0;
        depositTotalByMethod = new LinkedHashMap<>();
        usedDepositMethods = new ArrayList<>();
        paymentTotalByMethod = new LinkedHashMap<>();
        usedPaymentMethods = new ArrayList<>();
        postPaymentTotalByMethod = new LinkedHashMap<>();
        usedPostPaymentMethods = new ArrayList<>();

        // Every Configure Columns checkbox must show checked whenever the
        // user navigates in fresh, regardless of any previously saved
        // per-user preference from an earlier visit.
        userSettingsController.resetInwardBhtPaymentSummaryColumnsVisible();
    }

    private static Date startOfCurrentMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static Date endOfCurrentMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    // Getters / setters
    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public String getDateBasis() {
        return dateBasis;
    }

    public void setDateBasis(String dateBasis) {
        this.dateBasis = dateBasis;
    }

    public AdmissionStatus getAdmissionStatus() {
        return admissionStatus;
    }

    public void setAdmissionStatus(AdmissionStatus admissionStatus) {
        this.admissionStatus = admissionStatus;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<BhtPaymentDetailDTO> getReportRows() {
        return reportRows;
    }

    public double getGrandTotal() {
        return grandTotal;
    }

    public double getGrandTotalCcSettlement() {
        return grandTotalCcSettlement;
    }

    public double getGrandTotalPayments() {
        return grandTotalPayments;
    }

    public double getGrandTotalPostPayments() {
        return grandTotalPostPayments;
    }

    public List<PaymentMethod> getUsedDepositMethods() {
        return usedDepositMethods;
    }

    public Map<PaymentMethod, Double> getDepositTotalByMethod() {
        return depositTotalByMethod;
    }

    public List<PaymentMethod> getUsedPaymentMethods() {
        return usedPaymentMethods;
    }

    public Map<PaymentMethod, Double> getPaymentTotalByMethod() {
        return paymentTotalByMethod;
    }

    public List<PaymentMethod> getUsedPostPaymentMethods() {
        return usedPostPaymentMethods;
    }

    public Map<PaymentMethod, Double> getPostPaymentTotalByMethod() {
        return postPaymentTotalByMethod;
    }
}
