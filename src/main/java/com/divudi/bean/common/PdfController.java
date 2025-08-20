package com.divudi.bean.common;

import com.divudi.bean.hr.StaffImageController;
import com.divudi.bean.lab.CommonReportItemController;
import com.divudi.bean.lab.PatientInvestigationController;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.inject.Named;
import javax.enterprise.context.RequestScoped;

import com.divudi.core.util.CommonFunctions;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import com.divudi.core.entity.lab.PatientReport;
import com.divudi.core.data.ReportTemplateRowBundle;
import javax.inject.Inject;
import com.divudi.core.data.InvestigationItemType;
import com.divudi.core.data.InvestigationItemValueType;
import com.divudi.core.data.ReportItemType;
import com.divudi.core.data.ReportTemplateRow;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.lab.CommonReportItem;
import com.divudi.core.entity.lab.InvestigationItem;
import com.divudi.core.entity.lab.PatientReportItemValue;
import com.divudi.core.entity.lab.PatientSampleComponant;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.Style;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import java.util.function.Supplier;

/**
 *
 * @author Dr M H B Ariyaratne buddhika.ari@gmail.com
 *
 */
@Named
@RequestScoped
public class PdfController {

    @Inject
    StaffImageController staffImageController;
    @Inject
    CommonReportItemController commonReportItemController;
    @Inject
    PatientInvestigationController patientInvestigationController;
    @Inject
    SearchController searchController;

    /**
     * Creates a new instance of PdfController
     */
    public PdfController() {
    }
    public StreamedContent createPdfForBundle(ReportTemplateRowBundle rootBundle) throws IOException {
        if (rootBundle == null) {
            return null;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        if (rootBundle.getBundles() == null || rootBundle.getBundles().isEmpty()) {
            addDataToPdf(document, rootBundle, rootBundle.getBundleType());
        } else {
            for (ReportTemplateRowBundle childBundle : rootBundle.getBundles()) {
                addDataToPdf(document, childBundle, childBundle.getBundleType());
            }
        }

        document.close();

        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        // Set the downloading file
        return DefaultStreamedContent.builder()
                .name(rootBundle.getName()+".pdf")
                .contentType("application/pdf")
                .stream(() -> inputStream)
                .build();
    }

    public StreamedContent createPdfForPatientReport(PatientReport report) throws IOException {
        System.out.println("createPdfForPatientReport");
        System.out.println("Report: " + report);
        if (report == null) {
            System.out.println("Report is null, returning null.");
            return null;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);

        float pageWidth = pdfDoc.getDefaultPageSize().getWidth();
        float pageHeight = pdfDoc.getDefaultPageSize().getHeight();

        // <editor-fold defaultstate="collapsed" desc="Report Item Values">
        System.out.println("Processing patient report item values...");
        if (report.getPatientReportItemValues() != null) {
            for (PatientReportItemValue prv : report.getPatientReportItemValues()) {
                System.out.println("Processing PatientReportItemValue: " + prv);
                InvestigationItem item = prv.getInvestigationItem();
                if (item.isRetired()) {
                    System.out.println("Item is retired, skipping.");
                    continue;
                }

                String cssStyle = item.getCssStyle();
                System.out.println("CSS Style: " + cssStyle);
                Map<String, String> styleMap = parseCssStyle(cssStyle);
                System.out.println("Parsed Style Map: " + styleMap);

                float left = parseFloat(styleMap.get("left"), 0, pageWidth);
                float top = parseFloat(styleMap.get("top"), 0, pageHeight);
                float fontSize = parseFloat(styleMap.get("font-size"), 12, 0);
                String color = styleMap.get("color");

                System.out.println("Position - Left: " + left + ", Top: " + top + ", Font Size: " + fontSize + ", Color: " + color);

                String value = getValueBasedOnItemType(prv);
                System.out.println("Value to display: " + value);

                if (value != null && !value.isEmpty()) {
                    // Convert CSS top position to PDF coordinate (from bottom)
                    float yPosition = pageHeight - top;

                    if (containsHtml(value)) {
                        // Handling of HTML content at specific positions requires additional code
                        System.out.println("HTML content detected, adding to document.");

                        // For simplicity, render HTML content directly
                        // Note: Positioning HTML content at specific coordinates is complex
                        ByteArrayInputStream htmlStream = new ByteArrayInputStream(value.getBytes());
                        HtmlConverter.convertToPdf(htmlStream, pdfDoc);

                    } else {
                        // For plain text, use Paragraph and set fixed position
                        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                        Color colorRgb = parseColor(color);

                        Paragraph paragraph = new Paragraph(value)
                                .setFont(font)
                                .setFontSize(fontSize)
                                .setFontColor(colorRgb)
                                .setFixedPosition(left, yPosition, pageWidth - left);

                        document.add(paragraph);
                    }
                } else {
                    System.out.println("Value is null or empty, skipping.");
                }
            }
        } else {
            System.out.println("Patient report item values are null.");
        }

        // </editor-fold>
        // <editor-fold defaultstate="collapsed" desc="Report Labels">
        System.out.println("Processing report items (Labels)...");
        if (report.getItem() != null && report.getItem().getReportItems() != null) {
            for (InvestigationItem ii : report.getItem().getReportItems()) {
                System.out.println("Processing InvestigationItem: " + ii);
                if (ii.isRetired()) {
                    System.out.println("Item is retired, skipping.");
                    continue;
                }
                if (ii.getIxItemType() != InvestigationItemType.Label) {
                    System.out.println("Item is not a Label, skipping.");
                    continue;
                }
                String cssStyle = ii.getCssStyle();
                System.out.println("CSS Style: " + cssStyle);
                Map<String, String> styleMap = parseCssStyle(cssStyle);
                System.out.println("Parsed Style Map: " + styleMap);

                float left = parseFloat(styleMap.get("left"), 0, pageWidth);
                float top = parseFloat(styleMap.get("top"), 0, pageHeight);
                float fontSize = parseFloat(styleMap.get("font-size"), 12, 0);
                String color = styleMap.get("color");
                String cssColor = ii.getCssColor();
                if (color == null) {
                    color = cssColor;
                }

                System.out.println("Position - Left: " + left + ", Top: " + top + ", Font Size: " + fontSize + ", Color: " + color);

                String value = ii.getHtmltext(); // Assuming getHtmltext() method
                System.out.println("Value to display: " + value);

                if (value != null && !value.isEmpty()) {
                    // Convert CSS top position to PDF coordinate (from bottom)
                    float yPosition = pageHeight - top;

                    if (containsHtml(value)) {
                        // Handling of HTML content at specific positions
                        System.out.println("HTML content detected, adding to document at specific position.");

                        // Convert HTML content to elements
                        // Create a Div with absolute positioning
                        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                        Color colorRgb = parseColor(color);

                        // Create a Paragraph with the HTML content
                        Paragraph paragraph = new Paragraph();
                        paragraph.setFont(font)
                                .setFontSize(fontSize)
                                .setFontColor(colorRgb)
                                .setFixedPosition(left, yPosition, pageWidth - left);

                        // Add the HTML content to the Paragraph
                        // Note: iText doesn't parse HTML in Paragraphs directly
                        // We need to parse the HTML content and add elements
                        java.util.List<com.itextpdf.layout.element.IElement> elements
                                = HtmlConverter.convertToElements(value);

                        for (com.itextpdf.layout.element.IElement element : elements) {
                            paragraph.add((com.itextpdf.layout.element.IBlockElement) element);
                        }

                        document.add(paragraph);

                    } else {
                        // For plain text, use Paragraph and set fixed position
                        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                        Color colorRgb = parseColor(color);

                        Paragraph paragraph = new Paragraph(value)
                                .setFont(font)
                                .setFontSize(fontSize)
                                .setFontColor(colorRgb)
                                .setFixedPosition(left, yPosition, pageWidth - left);

                        document.add(paragraph);
                    }
                } else {
                    System.out.println("Value is null or empty, skipping.");
                }
            }
        } else {
            System.out.println("Report items are null.");
        }

        // </editor-fold>
        // <editor-fold defaultstate="collapsed" desc="Common Report Items">
        Category commonReportFormat = report.getReportFormat(); // Adjust as per your code

        // Get the list of common report items
        List<CommonReportItem> commonReportItems = commonReportItemController.listCommonRportItems(commonReportFormat);

        if (commonReportItems != null) {
            for (CommonReportItem myCli : commonReportItems) {

                System.out.println("myCli.getReportItemType() = " + myCli.getReportItemType());
                System.out.println("myCli.getItem() = " + myCli.getItem());
                System.out.println("myCli.getIxItemType() = " + myCli.getIxItemType());
                System.out.println("myCli.getInnerCssStyle() = " + myCli.getInnerCssStyle());
                System.out.println("myCli.getHtPix() = " + myCli.getHtPix());
                System.out.println("myCli.getWtPix() = " + myCli.getWtPix());

                if (myCli.isRetired()) {
                    continue;
                }

                // Parse CSS styles
                String cssStyle = myCli.getOuterCssStyle(); // Assuming getOuterCssStyle() method
                Map<String, String> styleMap = parseCssStyle(cssStyle);

                float left = parseFloat(styleMap.get("left"), 0, pageWidth);
                float top = parseFloat(styleMap.get("top"), 0, pageHeight);
                float fontSize = parseFloat(styleMap.get("font-size"), 12, 0);
                String color = styleMap.get("color");
                String innerCssStyle = myCli.getInnerCssStyle(); // Assuming getInnerCssStyle() method

                if (color == null) {
                    color = myCli.getCssColor(); // Assuming getCssColor() method
                }

                // Convert CSS top position to PDF coordinate (from bottom)
                float yPosition = pageHeight - top;

                // Determine the content to display based on item type
                String value = null;

                if (myCli.getIxItemType() == InvestigationItemType.Label) {
                    value = myCli.getName();
                } else if (myCli.getIxItemType() != InvestigationItemType.Label && myCli.getReportItemType() != null) {
                    ReportItemType reportItemType = myCli.getReportItemType();
                    value = getValueForReportItemType(reportItemType, report);
                }

                if (null != myCli.getReportItemType()) {
                    switch (myCli.getReportItemType()) {
                        case PatientName:
                            value = (report.getPatientInvestigation() != null && report.getPatientInvestigation().getPatient() != null
                                    && report.getPatientInvestigation().getPatient().getPerson() != null)
                                    ? report.getPatientInvestigation().getPatient().getPerson().getNameWithTitle() : "";
                            break;
                        case Phone:
                            value = (report.getPatientInvestigation() != null && report.getPatientInvestigation().getPatient() != null
                                    && report.getPatientInvestigation().getPatient().getPerson() != null)
                                    ? report.getPatientInvestigation().getPatient().getPerson().getPhone() : "";
                            break;
                        case PatientAge:
                            value = (report.getPatientInvestigation() != null && report.getPatientInvestigation().getPatient() != null
                                    && report.getPatientInvestigation().getPatient().getPerson() != null)
                                    ? report.getPatientInvestigation().getPatient().getPerson().getAgeAsShortString() : "";
                            break;
                        case PatientAgeOnBillDate:
                            value = (report.getPatientInvestigation() != null && report.getPatientInvestigation().getPatient() != null
                                    && report.getPatientInvestigation().getBillItem() != null && report.getPatientInvestigation().getBillItem().getBill() != null)
                                    ? report.getPatientInvestigation().getPatient().getAgeOnBilledDate(report.getPatientInvestigation().getBillItem().getBill().getCreatedAt()) : "";
                            break;
                        case PatientAgeandGender:
                            value = (report.getPatientInvestigation() != null && report.getPatientInvestigation().getPatient() != null
                                    && report.getPatientInvestigation().getPatient().getPerson() != null)
                                    ? report.getPatientInvestigation().getPatient().getPerson().getAgeAsShortString() + " / "
                                    + report.getPatientInvestigation().getPatient().getPerson().getSex() : "";
                            break;
                        case PatientSex:
                            value = (report.getPatientInvestigation() != null && report.getPatientInvestigation().getPatient() != null
                                    && report.getPatientInvestigation().getPatient().getPerson() != null)
                                    ? report.getPatientInvestigation().getPatient().getPerson().getSex().getLabel() : "";
                            break;
                        case InvestigationName:
                            value = (report.getPatientInvestigation() != null && report.getPatientInvestigation().getInvestigation() != null)
                                    ? report.getPatientInvestigation().getInvestigation().getName() : "";
                            break;
                        case Speciman:
                            value = (report.getPatientInvestigation() != null && report.getPatientInvestigation().getInvestigation() != null
                                    && report.getPatientInvestigation().getInvestigation().getSample() != null)
                                    ? report.getPatientInvestigation().getInvestigation().getSample().getName() : "";
                            break;
                        case SampledTime:
                            value = (report.getPatientInvestigation() != null)
                                    ? CommonFunctions.formatDate(report.getPatientInvestigation().getSampledAt(), "hh:mm a") : "";
                            break;
                        case CollectingCenter:
                            value = (report.getPatientInvestigation() != null && report.getPatientInvestigation().getBillItem() != null
                                    && report.getPatientInvestigation().getBillItem().getBill() != null
                                    && report.getPatientInvestigation().getBillItem().getBill().getCollectingCentre() != null)
                                    ? report.getPatientInvestigation().getBillItem().getBill().getCollectingCentre().getChequePrintingName() : "";
                            break;
                        case BilledDate:
                            value = (report.getPatientInvestigation() != null && report.getPatientInvestigation().getBillItem() != null
                                    && report.getPatientInvestigation().getBillItem().getBill() != null)
                                    ? CommonFunctions.formatDate(report.getPatientInvestigation().getBillItem().getBill().getCreatedAt(), "dd/MM/yyyy") : "";
                            break;
                        case BilledTime:
                            value = (report.getPatientInvestigation() != null && report.getPatientInvestigation().getBillItem() != null
                                    && report.getPatientInvestigation().getBillItem().getBill() != null)
                                    ? CommonFunctions.formatDate(report.getPatientInvestigation().getBillItem().getBill().getCreatedAt(), "hh:mm a") : "";
                            break;
                        case SampledDate:
                            value = (report.getPatientInvestigation() != null)
                                    ? CommonFunctions.formatDate(report.getPatientInvestigation().getSampledAt(), "dd/MM/yyyy") : "";
                            break;
                        case BillNo:
                            value = (report.getPatientInvestigation() != null && report.getPatientInvestigation().getBillItem() != null
                                    && report.getPatientInvestigation().getBillItem().getBill() != null)
                                    ? report.getPatientInvestigation().getBillItem().getBill().getDeptId() : "";
                            break;
                        case DepartmentBillNo:
                            value = (report.getPatientInvestigation() != null && report.getPatientInvestigation().getBillItem() != null
                                    && report.getPatientInvestigation().getBillItem().getBill() != null)
                                    ? report.getPatientInvestigation().getBillItem().getBill().getDeptId() : "";
                            break;
                        case ReportedDate:
                            value = (report.getCreatedAt() != null) ? CommonFunctions.formatDate(report.getCreatedAt(), "dd/MM/yyyy") : "";
                            break;
                        case ReportedTime:
                            value = (report.getCreatedAt() != null) ? CommonFunctions.formatDate(report.getCreatedAt(), "hh:mm a") : "";
                            break;
                        case ReferringDoctor:
                            value = (report.getPatientInvestigation() != null && report.getPatientInvestigation().getBillItem() != null
                                    && report.getPatientInvestigation().getBillItem().getBill() != null
                                    && report.getPatientInvestigation().getBillItem().getBill().getReferredBy() != null
                                    && report.getPatientInvestigation().getBillItem().getBill().getReferredBy().getPerson() != null)
                                    ? report.getPatientInvestigation().getBillItem().getBill().getReferredBy().getPerson().getNameWithTitle() : "";
                            break;
                        case SampledAt:
                            value = (report.getPatientInvestigation() != null)
                                    ? CommonFunctions.formatDate(report.getPatientInvestigation().getSampledAt(), "dd/MM/yyyy hh:mm a") : "";
                            break;
                        case ApprovedAt:
                            value = (report.getPatientInvestigation() != null)
                                    ? CommonFunctions.formatDate(report.getPatientInvestigation().getApproveAt(), "dd/MM/yyyy hh:mm a") : "";
                            break;
                        case ReceivedAt:
                            value = (report.getPatientInvestigation() != null)
                                    ? CommonFunctions.formatDate(report.getPatientInvestigation().getReceivedAt(), "dd/MM/yyyy hh:mm a") : "";
                            break;
                        case ReceivedOn:
                            value = (report.getPatientInvestigation() != null)
                                    ? CommonFunctions.formatDate(report.getPatientInvestigation().getReceivedAt(), "dd/MM/yyyy hh:mm a") : "";
                            break;
                        case PrintedAt:
                            value = (report.getPatientInvestigation() != null)
                                    ? CommonFunctions.formatDate(report.getPatientInvestigation().getPrintingAt(), "dd/MM/yyyy hh:mm a") : "";
                            break;
                        case AutherizedCode:
                            value = (report.getApproveUser() != null && report.getApproveUser().getStaff() != null)
                                    ? report.getApproveUser().getStaff().getCode() : "";
                            break;
                        case AutherizedPosition:
                            value = (report.getApproveUser() != null && report.getApproveUser().getStaff() != null)
                                    ? report.getApproveUser().getStaff().getSpeciality().getName() : "";
                            break;
                        case AutherizedQualification:
                            value = (report.getApproveUser() != null && report.getApproveUser().getStaff() != null)
                                    ? report.getApproveUser().getStaff().getQualification() : "";
                            break;
                        case MRN:
                            value = (report.getPatientInvestigation() != null && report.getPatientInvestigation().getPatient() != null)
                                    ? report.getPatientInvestigation().getPatient().getPhn() : "";
                            break;
                        case VisitType:
                            value = (report.getPatientInvestigation() != null && report.getPatientInvestigation().getBillItem() != null)
                                    ? report.getPatientInvestigation().getBillItem().getBill().getIpOpOrCc() : "";
                            break;
                        case BillingDepartment:
                            value = (report.getPatientInvestigation() != null && report.getPatientInvestigation().getBillItem() != null
                                    && report.getPatientInvestigation().getBillItem().getBill() != null
                                    && report.getPatientInvestigation().getBillItem().getBill().getDepartment() != null)
                                    ? report.getPatientInvestigation().getBillItem().getBill().getDepartment().getName() : "";
                            break;
                        case PerformDepartment:
                            value = (report.getPatientInvestigation() != null && report.getPatientInvestigation().getBillItem() != null
                                    && report.getPatientInvestigation().getBillItem().getBill() != null
                                    && report.getPatientInvestigation().getBillItem().getBill().getToDepartment() != null)
                                    ? report.getPatientInvestigation().getBillItem().getBill().getToDepartment().getName() : "";
                            break;
                        case BillingInstitution:
                            value = (report.getPatientInvestigation() != null && report.getPatientInvestigation().getBillItem() != null
                                    && report.getPatientInvestigation().getBillItem().getBill() != null
                                    && report.getPatientInvestigation().getBillItem().getBill().getInstitution() != null)
                                    ? report.getPatientInvestigation().getBillItem().getBill().getInstitution().getName() : "";
                            break;
                        case PerformInstitution:
                            value = (report.getPatientInvestigation() != null && report.getPatientInvestigation().getBillItem() != null
                                    && report.getPatientInvestigation().getBillItem().getBill() != null
                                    && report.getPatientInvestigation().getBillItem().getBill().getToInstitution() != null)
                                    ? report.getPatientInvestigation().getBillItem().getBill().getToInstitution().getName() : "";
                            break;
                        case BHT:
                            value = (report.getPatientInvestigation() != null && report.getPatientInvestigation().getEncounter() != null)
                                    ? report.getPatientInvestigation().getEncounter().getBhtNo() : "";
                            break;
                        case CollectedOn:
                            value = (report.getPatientInvestigation() != null)
                                    ? CommonFunctions.formatDate(report.getPatientInvestigation().getSampleCollectedAt(), "dd/MM/yyyy hh:mm a") : "";
                            break;
                        case SampledID:
                            List<PatientSampleComponant> pscs = patientInvestigationController.getPatientSampleComponentsByInvestigation(report.getPatientInvestigation());
                            String sampleIds = "";
                            System.out.println("pscs.size = " + pscs.size());
                            for (PatientSampleComponant psc : pscs) {
                                System.out.println("psc = " + psc);
                                System.out.println("psc.getSample() = " + psc.getPatientSample());
                                System.out.println("psc.getSample().getId() = " + psc.getPatientSample().getId());
                                System.out.println("");
                            }
                            value = sampleIds;

                            break;
                        default:
                            break;
                    }
                }

                if (value != null && !value.isEmpty()) {
                    // Handle content rendering
                    PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                    Color colorRgb = parseColor(color);

                    Paragraph paragraph = new Paragraph(value)
                            .setFont(font)
                            .setFontSize(fontSize)
                            .setFontColor(colorRgb)
                            .setFixedPosition(left, yPosition, pageWidth - left);

                    document.add(paragraph);
                }
            }
        } else {
            System.out.println("Common report items are null.");
        }

        if (report.getApproveUser() != null && report.getApproveUser().getStaff() != null) {
            StreamedContent signatureContent = staffImageController.getSignatureForPatientReport(report);
            if (signatureContent != null && signatureContent.getStream() != null) {
                Supplier<InputStream> signatureStreamSupplier = signatureContent.getStream();
                if (signatureStreamSupplier != null) {
                    try (InputStream signatureStream = signatureStreamSupplier.get()) {
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        int nRead;
                        byte[] data = new byte[16384];
                        while ((nRead = signatureStream.read(data, 0, data.length)) != -1) {
                            buffer.write(data, 0, nRead);
                        }
                        buffer.flush();
                        byte[] signatureBytes = buffer.toByteArray();

                        ImageData imageData = ImageDataFactory.create(signatureBytes);
                        Image signatureImage = new Image(imageData);
                        // Adjust the position based on the item's CSS styles
                        // For example, if you have a specific item for the signature
                        // You can retrieve its position and use it here
                        float signatureLeft = 100; // Adjust as needed
                        float signatureTop = 100; // Adjust as needed
                        signatureImage.setFixedPosition(signatureLeft, signatureTop); // Adjust position as needed
                        signatureImage.scaleToFit(100, 50); // Adjust size as needed
                        document.add(signatureImage);
                    } catch (IOException e) {
                        System.out.println("Error reading signature image: " + e.getMessage());
                    }
                } else {
                    System.out.println("Signature stream supplier is null.");
                }
            } else {
                System.out.println("Signature content is null or has no stream.");
            }
        }

        // </editor-fold>
        document.close();
        System.out.println("Document closed.");

        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        // Return the generated PDF as StreamedContent
        return DefaultStreamedContent.builder()
                .name("lab_report.pdf")
                .contentType("application/pdf")
                .stream(() -> inputStream)
                .build();
    }

    private String getValueForReportItemType(ReportItemType reportItemType, PatientReport report) {
        switch (reportItemType) {
            case PatientName:
                return report.getPatientInvestigation().getPatient().getPerson().getNameWithTitle();
            case Phn:
                return report.getPatientInvestigation().getPatient().getPhn();
            case Phone:
                return report.getPatientInvestigation().getPatient().getPerson().getPhone();
            case PatientAge:
                return report.getPatientInvestigation().getPatient().getPerson().getAgeAsShortString();
            case PatientSex:
                return report.getPatientInvestigation().getPatient().getPerson().getSex() != null
                        ? report.getPatientInvestigation().getPatient().getPerson().getSex().toString()
                        : "";
            case InvestigationName:
                return report.getPatientInvestigation().getInvestigation().getName();
            case Speciman:
                return report.getPatientInvestigation().getInvestigation().getSample() != null
                        ? report.getPatientInvestigation().getInvestigation().getSample().getName()
                        : "";
            case SampledTime:
                if (report.getPatientInvestigation().getSampledAt() != null) {
                    return new java.text.SimpleDateFormat("hh:mm a").format(report.getPatientInvestigation().getSampledAt());
                } else {
                    return "";
                }
            case BilledDate:
                if (report.getPatientInvestigation().getBillItem().getBill().getCreatedAt() != null) {
                    return new java.text.SimpleDateFormat("dd/MM/yyyy").format(report.getPatientInvestigation().getBillItem().getBill().getCreatedAt());
                } else {
                    return "";
                }
            case BilledTime:
                if (report.getPatientInvestigation().getBillItem().getBill().getCreatedAt() != null) {
                    return new java.text.SimpleDateFormat("hh:mm a").format(report.getPatientInvestigation().getBillItem().getBill().getCreatedAt());
                } else {
                    return "";
                }
            case BillNo:
                return report.getPatientInvestigation().getBillItem().getBill().getDeptId();
            case ReportedDate:
                if (report.getCreatedAt() != null) {
                    return new java.text.SimpleDateFormat("dd/MM/yyyy").format(report.getCreatedAt());
                } else {
                    return "";
                }
            case ReportedTime:
                if (report.getCreatedAt() != null) {
                    return new java.text.SimpleDateFormat("hh:mm a").format(report.getCreatedAt());
                } else {
                    return "";
                }
            case ReferringDoctor:
                if (report.getPatientInvestigation().getBillItem().getBill().getReferredBy() != null) {
                    return report.getPatientInvestigation().getBillItem().getBill().getReferredBy().getPerson().getNameWithTitle();
                } else {
                    return "";
                }
            // Add cases for other ReportItemTypes as needed
            default:
                return "";
        }
    }

    private Color parseColor(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) {
            return ColorConstants.BLACK;
        }
        colorStr = colorStr.trim();
        if (colorStr.startsWith("#")) {
            // Handle hex color codes
            int r = Integer.parseInt(colorStr.substring(1, 3), 16);
            int g = Integer.parseInt(colorStr.substring(3, 5), 16);
            int b = Integer.parseInt(colorStr.substring(5, 7), 16);
            return new DeviceRgb(r, g, b);
        } else if (colorStr.startsWith("rgb")) {
            // Handle rgb(r, g, b) format
            Pattern pattern = Pattern.compile("rgb\\s*\\(\\s*(\\d+),\\s*(\\d+),\\s*(\\d+)\\s*\\)");
            Matcher matcher = pattern.matcher(colorStr);
            if (matcher.matches()) {
                int r = Integer.parseInt(matcher.group(1));
                int g = Integer.parseInt(matcher.group(2));
                int b = Integer.parseInt(matcher.group(3));
                return new DeviceRgb(r, g, b);
            }
        }
        // Fallback or handle named colors
        return ColorConstants.BLACK;
    }

    private boolean containsHtml(String value) {
        return value != null && value.matches(".*\\<[^>]+>.*");
    }

    private Map<String, String> parseCssStyle(String cssStyle) {
        Map<String, String> styleMap = new HashMap<>();
        if (cssStyle != null) {
            String[] styles = cssStyle.split(";");
            for (String style : styles) {
                String[] keyValue = style.split(":");
                if (keyValue.length == 2) {
                    styleMap.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }
        }
        return styleMap;
    }

    private float parseFloat(String value, float defaultValue, float relativeTo) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            value = value.replaceAll("!important", "").trim();
            if (value.endsWith("%")) {
                value = value.replace("%", "").trim();
                float percentage = Float.parseFloat(value);
                return (percentage / 100f) * relativeTo;
            } else if (value.endsWith("pt")) {
                value = value.replace("pt", "").trim();
                return Float.parseFloat(value);
            } else {
                // Remove any non-numeric characters
                value = value.replaceAll("[^0-9.\\-]", "").trim();
                return Float.parseFloat(value);
            }
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String getValueBasedOnItemType(PatientReportItemValue prv) {
        InvestigationItem item = prv.getInvestigationItem();
        InvestigationItemType ixItemType = item.getIxItemType();
        InvestigationItemValueType ixItemValueType = item.getIxItemValueType();

        if (ixItemType == InvestigationItemType.Value && ixItemValueType == InvestigationItemValueType.Memo) {
            return prv.getLobValue();
        } else if (ixItemType == InvestigationItemType.Template) {
            return prv.getLobValue();
        } else if (ixItemType == InvestigationItemType.Value && ixItemValueType == InvestigationItemValueType.Varchar) {
            return prv.getStrValue();
        } else if (ixItemType == InvestigationItemType.Value && ixItemValueType == InvestigationItemValueType.Double) {
            return prv.getDisplayValue();
        } else if (ixItemType == InvestigationItemType.DynamicLabel) {
            return prv.getStrValue();
        } else if (ixItemType == InvestigationItemType.Flag) {
            return prv.getStrValue();
        } else if (ixItemType == InvestigationItemType.Calculation) {
            if (ixItemValueType == InvestigationItemValueType.Double) {
                Double doubleValue = prv.getDoubleValue();
                if (doubleValue != null) {
                    return String.format("%.1f", doubleValue);
                }
            } else if (ixItemValueType == InvestigationItemValueType.Varchar) {
                return prv.getStrValue();
            }
        }
        return null;
    }

    private void addDataToPdf(Document document, ReportTemplateRowBundle addingBundle, String type) {
        if (type == null || type.isEmpty()) {
            type = "BillList";
        }

        // Create a new Table for each call
        Table table = new Table(new float[]{10, 40, 15, 15, 10, 10});
        table.setWidth(200);

        switch (type) {
            case "opdServiceCollection":
                populateTableForItemSummaryGroupedByCategory(document, addingBundle);
                break;
            case "pharmacyCollection":
                populateTableForDepartmentCollection(document, addingBundle);
                break;
            case "ccCollection":
                populateTableForCcDeposits(document, addingBundle);
                break;
            case "companyPaymentBillOpd":
            case "companyPaymentBillInward":
            case "companyPaymentBillPharmacy":
            case "companyPaymentBillChannelling":
                populateTableForCompanyCollection(document, addingBundle);
                break;
            case "patientDepositPayments":
                populateTableForPatientDeposits(document, addingBundle);
                break;
            case "collectionForTheDay":
                populateTitleBundleForPdf(document, addingBundle);
                break;
            case "pettyCashPayments":
                populateTableForPettyCashPayments(document, addingBundle);
                break;
            case "ProfessionalPaymentBillReportOpd":
            case "ProfessionalPaymentBillReportChannelling":
            case "ProfessionalPaymentBillReportInward":
                populateTableForProfessionalPayments(document, addingBundle);
                break;
            case "paymentReportCards":
                populateTableForCreditCards(document, addingBundle);
                break;
            case "paymentReportStaffWelfare":
            case "paymentReportVoucher":
            case "paymentReportCheque":
            case "paymentReportEwallet":
            case "paymentReportSlip":
//                populateTableForPaymentReports(document, addingBundle);
                break;
            case "netCash":
                populateTitleBundleForPdf(document, addingBundle);
                break;
            case "income_breakdown_by_category_with_professional_fee":
                populateTableForIncomeByCategoryWithProfessionalFee(document, addingBundle);
                break;
            case "income_breakdown_by_category_with_out_professional_fee":
                populateTableForIncomeByCategoryWithoutProfessionalFee(document, addingBundle);
                break;
            case "itemized_sales_report_with_professional_fee":
                populateTableForItemizedSalesReportWithProfessionalFee(document, addingBundle);
                break;
            case "itemized_sales_report_without_professional_fee":
                populateTableForItemizedSalesReportWithoutProfessionalFee(document, addingBundle);
                break;
            case "opdServiceCollectionCredit":
                populateTableForCreditItemSummaryGroupedByCategory(document, addingBundle);
                break;
            case "netCashPlusCredit":
                populateTitleBundleForPdf(document, addingBundle);
                break;
            case "opdServiceBilled":
            case "opdServiceCancellations":
            case "opdServiceRefunds":
            case "cashierSummaryOpdCredit":
            case "opdServiceCancellationsCredit":
            case "opdServiceRefundsCredit":
            case "pharmacyNonCreditBills":
            case "ProfessionalPaymentsOPD":
            case "ProfessionalPaymentsOPDCancel":
            case "ProfessionalPaymentsInward":
            case "ProfessionalPaymentsInwardCancel":
            case "PettyCashPayment":
            case "PettyCashPaymentCancel":
            case "InwardPayments":
            case "InwardPaymentsCancel":
            case "InwardPaymentsRefund":
                populateTableForopdServiceBilled(document, addingBundle);
                break;
            case "pharmacyServiceCancellations":
            case "pharmacyServiceRefunds":
                populateTableForpharmacyServiceCancellations(document, addingBundle);
                break;
            default:
                table.addCell(new Cell().add(new Paragraph("Data for unknown type"))); // Default handling for unknown types
                break;
        }

        // Add the table to the document
        document.add(table);

        // Optionally, add spacing or a separator between tables
        document.add(new Paragraph("\n"));
    }


    private void populateTableForpharmacyServiceCancellations(Document document, ReportTemplateRowBundle addingBundle) {
        if (addingBundle.getReportTemplateRows() == null || addingBundle.getReportTemplateRows().isEmpty()) {
            // Handle empty data case - create simple table with "No Data" message
            Table noDataTable = new Table(1);
            noDataTable.setWidth(UnitValue.createPercentValue(100));

            // Add header with bundle name
            Cell headerCell = new Cell()
                    .add(new Paragraph("No Data for " + addingBundle.getName()))
                    .setBackgroundColor(ColorConstants.DARK_GRAY)
                    .setFontColor(ColorConstants.WHITE)
                    .setPadding(8);
            noDataTable.addHeaderCell(headerCell);

            document.add(noDataTable);
            return;
        }

        boolean includeBillType = true;

        // Define column structure based on template
        int totalColumns = includeBillType ? 6 : 5;

        // Create column widths array
        float[] columnWidths;
        if (includeBillType) {
            columnWidths = new float[]{8, 20, 15, 15, 25, 17}; // Serial, Date&Time, Bill No, Bill Type, Patient, Net Total
        } else {
            columnWidths = new float[]{8, 20, 15, 32, 25}; // Serial, Date&Time, Bill No, Patient, Net Total
        }

        // Create table
        Table table = new Table(columnWidths);
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginLeft(8); // Equivalent to m-2 class margin

        // Add table header with bundle name (equivalent to f:facet="header")
        Cell titleCell = new Cell(1, totalColumns)
                .add(new Paragraph(addingBundle.getName()))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(8)
                .setTextAlignment(TextAlignment.LEFT)
                .setBold();
        table.addHeaderCell(titleCell);

        // Add column headers
        table.addHeaderCell(new Cell()
                .add(new Paragraph("Serial"))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(5)
                .setTextAlignment(TextAlignment.CENTER));

        table.addHeaderCell(new Cell()
                .add(new Paragraph("Date & Time"))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(5)
                .setTextAlignment(TextAlignment.LEFT));

        table.addHeaderCell(new Cell()
                .add(new Paragraph("Bill No"))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(5)
                .setTextAlignment(TextAlignment.LEFT));

        if (includeBillType) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph("Bill Type"))
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setPadding(5)
                    .setTextAlignment(TextAlignment.LEFT));
        }

        table.addHeaderCell(new Cell()
                .add(new Paragraph("Patient"))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(5)
                .setTextAlignment(TextAlignment.LEFT));

        table.addHeaderCell(new Cell()
                .add(new Paragraph("Net Total"))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(5)
                .setTextAlignment(TextAlignment.RIGHT));

        // Add data rows
        int serialNumber = 1;
        for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
            // Serial number (n+1 equivalent)
            table.addCell(new Cell()
                    .add(new Paragraph(String.valueOf(serialNumber++)))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(4));

            // Date & Time
            table.addCell(new Cell().add(new Paragraph(
                    row.getPayment() != null && row.getPayment().getBill() != null ? row.getPayment().getBill().getCreatedAt().toString() : "N/A")));


            // Bill No - using insId instead of deptId as per the template
            table.addCell(new Cell()
                    .add(new Paragraph(row.getBill().getInsId() != null ? row.getBill().getInsId() : ""))
                    .setPadding(4));

            // Bill Type (conditional column)
            if (includeBillType) {
                table.addCell(new Cell()
                        .add(new Paragraph(row.getBill().getBillTypeAtomic() != null ?
                                row.getBill().getBillTypeAtomic().toString() : ""))
                        .setPadding(4));
            }

            // Patient
            String patientName = "";
            if (row.getBill().getPatient() != null &&
                    row.getBill().getPatient().getPerson() != null) {
                patientName = row.getBill().getPatient().getPerson().getNameWithTitle();
            }
            table.addCell(new Cell()
                    .add(new Paragraph(patientName))
                    .setPadding(4));

            // Net Total
            table.addCell(new Cell()
                    .add(new Paragraph(String.format("%,.2f", row.getBill().getNetTotal())))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setPadding(4));
        }

        // Add footer row with total (equivalent to f:facet="footer")
        // Empty cells for non-total columns
        int emptyFooterCells = includeBillType ? 5 : 4;
        for (int i = 0; i < emptyFooterCells; i++) {
            table.addFooterCell(new Cell().add(new Paragraph("")).setPadding(4));
        }

        // Net Total footer
        table.addFooterCell(new Cell()
                .add(new Paragraph(String.format("#,##0.00", addingBundle.getTotal())))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setBold()
                .setPadding(4));

        document.add(table);
    }

    private void populateTableForopdServiceBilled(Document document, ReportTemplateRowBundle addingBundle) {
        if (addingBundle.getReportTemplateRows() == null || addingBundle.getReportTemplateRows().isEmpty()) {
            // Handle empty data case - create simple table with "No Data" message
            Table noDataTable = new Table(1);
            noDataTable.setWidth(UnitValue.createPercentValue(100));

            // Add header with bundle name
            Cell headerCell = new Cell()
                    .add(new Paragraph("No Data for " + addingBundle.getName()))
                    .setBackgroundColor(ColorConstants.DARK_GRAY)
                    .setFontColor(ColorConstants.WHITE)
                    .setPadding(8);
            noDataTable.addHeaderCell(headerCell);

            document.add(noDataTable);
            return;
        }

        // Calculate dynamic column count based on available transaction types
        List<String> dynamicColumns = new ArrayList<>();

        // Add base columns
        List<String> baseColumns = Arrays.asList("Serial", "Date & Time", "Bill No", "Bill Type", "Patient", "Net Total");

        // Add conditional payment type columns based on bundle properties
        if (addingBundle.isHasCashTransaction()) {
            dynamicColumns.add("Cash");
        }
        if (addingBundle.isHasCardTransaction()) {
            dynamicColumns.add("Card");
        }
        if (addingBundle.isHasCreditTransaction()) {
            dynamicColumns.add("Credit");
        }
        if (addingBundle.isHasStaffWelfareTransaction()) {
            dynamicColumns.add("Staff Welfare");
        }
        if (addingBundle.isHasVoucherTransaction()) {
            dynamicColumns.add("Voucher");
        }
        if (addingBundle.isHasIouTransaction()) {
            dynamicColumns.add("IOU");
        }
        if (addingBundle.isHasAgentTransaction()) {
            dynamicColumns.add("Agent");
        }
        if (addingBundle.isHasChequeTransaction()) {
            dynamicColumns.add("Cheque");
        }
        if (addingBundle.isHasSlipTransaction()) {
            dynamicColumns.add("Slip");
        }
        if (addingBundle.isHasEWalletTransaction()) {
            dynamicColumns.add("eWallet");
        }
        if (addingBundle.isHasPatientDepositTransaction()) {
            dynamicColumns.add("Patient Deposit");
        }
        if (addingBundle.isHasPatientPointsTransaction()) {
            dynamicColumns.add("Patient Points");
        }
        if (addingBundle.isHasOnCallTransaction()) { // Assuming this maps to Online Settlement
            dynamicColumns.add("Online Settlement");
        }

        // Calculate total columns
        int totalColumns = baseColumns.size() + dynamicColumns.size();

        // Create column widths array - adjust as needed
        float[] columnWidths = new float[totalColumns];
        columnWidths[0] = 8;  // Serial
        columnWidths[1] = 15; // Date & Time
        columnWidths[2] = 12; // Bill No
        columnWidths[3] = 12; // Bill Type
        columnWidths[4] = 20; // Patient
        columnWidths[5] = 12; // Net Total

        // Set equal widths for payment type columns
        float paymentColumnWidth = 10;
        for (int i = 6; i < totalColumns; i++) {
            columnWidths[i] = paymentColumnWidth;
        }

        // Create table
        Table table = new Table(columnWidths);
        table.setWidth(UnitValue.createPercentValue(100));

        // Add title header spanning all columns
        Cell titleCell = new Cell(1, totalColumns)
                .add(new Paragraph(addingBundle.getName()))
                .setBackgroundColor(ColorConstants.DARK_GRAY)
                .setFontColor(ColorConstants.WHITE)
                .setPadding(8)
                .setTextAlignment(TextAlignment.LEFT);
        table.addHeaderCell(titleCell);

        // Add column headers
        for (String header : baseColumns) {
            Cell headerCell = new Cell()
                    .add(new Paragraph(header))
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setPadding(5)
                    .setTextAlignment(header.equals("Serial") ? TextAlignment.CENTER :
                            (header.equals("Net Total") ? TextAlignment.RIGHT : TextAlignment.LEFT));
            table.addHeaderCell(headerCell);
        }

        for (String header : dynamicColumns) {
            Cell headerCell = new Cell()
                    .add(new Paragraph(header))
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setPadding(5)
                    .setTextAlignment(TextAlignment.RIGHT);
            table.addHeaderCell(headerCell);
        }

        // Add data rows
        int serialNumber = 1;
        for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
            // Serial number
            table.addCell(new Cell()
                    .add(new Paragraph(String.valueOf(serialNumber++)))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(4));

            // Date & Time
            table.addCell(new Cell().add(new Paragraph(
                    row.getPayment() != null && row.getPayment().getBill() != null ? row.getPayment().getBill().getCreatedAt().toString() : "N/A")));

            // Bill No
            table.addCell(new Cell()
                    .add(new Paragraph(row.getBill().getDeptId() != null ? row.getBill().getDeptId() : ""))
                    .setPadding(4));

            // Bill Type
            table.addCell(new Cell()
                    .add(new Paragraph(row.getBill().getBillTypeAtomic() != null ? row.getBill().getBillTypeAtomic().toString() : ""))
                    .setPadding(4));

            // Patient
            String patientName = "";
            if (row.getBill().getPatient() != null &&
                    row.getBill().getPatient().getPerson() != null) {
                patientName = row.getBill().getPatient().getPerson().getNameWithTitle();
            }
            table.addCell(new Cell()
                    .add(new Paragraph(patientName))
                    .setPadding(4));

            // Net Total
            table.addCell(new Cell()
                    .add(new Paragraph(String.format("#,##0.00", row.getBill().getNetTotal())))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setPadding(4));

            // Add dynamic payment columns
            if (addingBundle.isHasCashTransaction()) {
                table.addCell(new Cell()
                        .add(new Paragraph(String.format("#,##0.00", row.getCashValue())))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setPadding(4));
            }
            if (addingBundle.isHasCardTransaction()) {
                table.addCell(new Cell()
                        .add(new Paragraph(String.format("#,##0.00", row.getCardValue())))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setPadding(4));
            }
            if (addingBundle.isHasCreditTransaction()) {
                table.addCell(new Cell()
                        .add(new Paragraph(String.format("#,##0.00", row.getCreditValue())))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setPadding(4));
            }
            if (addingBundle.isHasStaffWelfareTransaction()) {
                table.addCell(new Cell()
                        .add(new Paragraph(String.format("#,##0.00", row.getStaffWelfareValue())))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setPadding(4));
            }
            if (addingBundle.isHasVoucherTransaction()) {
                table.addCell(new Cell()
                        .add(new Paragraph(String.format("#,##0.00", row.getVoucherValue())))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setPadding(4));
            }
            if (addingBundle.isHasIouTransaction()) {
                table.addCell(new Cell()
                        .add(new Paragraph(String.format("#,##0.00", row.getIouValue())))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setPadding(4));
            }
            if (addingBundle.isHasAgentTransaction()) {
                table.addCell(new Cell()
                        .add(new Paragraph(String.format("#,##0.00", row.getAgentValue())))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setPadding(4));
            }
            if (addingBundle.isHasChequeTransaction()) {
                table.addCell(new Cell()
                        .add(new Paragraph(String.format("#,##0.00", row.getChequeValue())))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setPadding(4));
            }
            if (addingBundle.isHasSlipTransaction()) {
                table.addCell(new Cell()
                        .add(new Paragraph(String.format("#,##0.00", row.getSlipValue())))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setPadding(4));
            }
            if (addingBundle.isHasEWalletTransaction()) {
                table.addCell(new Cell()
                        .add(new Paragraph(String.format("#,##0.00", row.getEwalletValue())))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setPadding(4));
            }
            if (addingBundle.isHasPatientDepositTransaction()) {
                table.addCell(new Cell()
                        .add(new Paragraph(String.format("#,##0.00", row.getPatientDepositValue())))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setPadding(4));
            }
            if (addingBundle.isHasPatientPointsTransaction()) {
                table.addCell(new Cell()
                        .add(new Paragraph(String.format("#,##0.00", row.getPatientPointsValue())))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setPadding(4));
            }
            if (addingBundle.isHasOnCallTransaction()) {
                table.addCell(new Cell()
                        .add(new Paragraph(String.format("#,##0.00", row.getOnlineSettlementValue())))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setPadding(4));
            }
        }

        // Add footer row with totals
        // Empty cells for first columns
        for (int i = 0; i < 5; i++) {
            table.addFooterCell(new Cell().add(new Paragraph("")).setPadding(4));
        }

        // Net Total footer
        table.addFooterCell(new Cell()
                .add(new Paragraph(String.format("#,##0.00", addingBundle.getTotal())))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(4));

        // Add footer totals for payment columns
        if (addingBundle.isHasCashTransaction()) {
            table.addFooterCell(new Cell()
                    .add(new Paragraph(String.format("#,##0.00", addingBundle.getCashValue())))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setPadding(4));
        }
        if (addingBundle.isHasCardTransaction()) {
            table.addFooterCell(new Cell()
                    .add(new Paragraph(String.format("#,##0.00", addingBundle.getCardValue())))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setPadding(4));
        }
        if (addingBundle.isHasCreditTransaction()) {
            table.addFooterCell(new Cell()
                    .add(new Paragraph(String.format("#,##0.00", addingBundle.getCreditValue())))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setPadding(4));
        }
        if (addingBundle.isHasStaffWelfareTransaction()) {
            table.addFooterCell(new Cell()
                    .add(new Paragraph(String.format("#,##0.00", addingBundle.getStaffWelfareValue())))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setPadding(4));
        }
        if (addingBundle.isHasVoucherTransaction()) {
            table.addFooterCell(new Cell()
                    .add(new Paragraph(String.format("#,##0.00", addingBundle.getVoucherValue())))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setPadding(4));
        }
        if (addingBundle.isHasIouTransaction()) {
            table.addFooterCell(new Cell()
                    .add(new Paragraph(String.format("#,##0.00", addingBundle.getIouValue())))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setPadding(4));
        }
        if (addingBundle.isHasAgentTransaction()) {
            table.addFooterCell(new Cell()
                    .add(new Paragraph(String.format("#,##0.00", addingBundle.getAgentValue())))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setPadding(4));
        }
        if (addingBundle.isHasChequeTransaction()) {
            table.addFooterCell(new Cell()
                    .add(new Paragraph(String.format("#,##0.00", addingBundle.getChequeValue())))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setPadding(4));
        }
        if (addingBundle.isHasSlipTransaction()) {
            table.addFooterCell(new Cell()
                    .add(new Paragraph(String.format("#,##0.00", addingBundle.getSlipValue())))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setPadding(4));
        }
        if (addingBundle.isHasEWalletTransaction()) {
            table.addFooterCell(new Cell()
                    .add(new Paragraph(String.format("#,##0.00", addingBundle.getEwalletValue())))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setPadding(4));
        }
        if (addingBundle.isHasPatientDepositTransaction()) {
            table.addFooterCell(new Cell()
                    .add(new Paragraph(String.format("#,##0.00", addingBundle.getPatientDepositValue())))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setPadding(4));
        }
        if (addingBundle.isHasPatientPointsTransaction()) {
            table.addFooterCell(new Cell()
                    .add(new Paragraph(String.format("#,##0.00", addingBundle.getPatientPointsValue())))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setPadding(4));
        }
        if (addingBundle.isHasOnCallTransaction()) {
            table.addFooterCell(new Cell()
                    .add(new Paragraph(String.format("#,##0.00", addingBundle.getOnlineSettlementValue())))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setPadding(4));
        }

        document.add(table);
    }


    private void populateTableForCreditCards(Document document, ReportTemplateRowBundle addingBundle) {
        if (addingBundle.getReportTemplateRows() == null || addingBundle.getReportTemplateRows().isEmpty()) {
            // If no data, add a paragraph stating this
            Paragraph noDataParagraph = new Paragraph("No Data for " + addingBundle.getName());
            document.add(noDataParagraph);
        } else {
            // Create a new Table for each call
            Table table = new Table(new float[]{10, 10, 10, 15, 15, 15, 15}); // Adjust column widths as necessary
            table.setWidth(100); // Set width to 100% of available space

            // Add title row with the report name and total
            table.addCell(new Cell(1, 6).add(new Paragraph(addingBundle.getName())));
            table.addCell(new Cell().add(new Paragraph(String.format("%.2f", addingBundle.getTotal())))); // Formatting total as a string

            // Add header row only when there is data
            String[] headers = {"Bill No", "Bill Class", "Bill Type", "Card Ref. Number", "Bank", "Reference Bill", "Fee"};
            for (String header : headers) {
                table.addCell(new Cell().add(new Paragraph(header)));
            }

            // Populate data rows
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                table.addCell(new Cell().add(new Paragraph(
                        row.getPayment() != null && row.getPayment().getBill() != null && row.getPayment().getBill().getDeptId() != null ? row.getPayment().getBill().getDeptId() : "N/A")));

                table.addCell(new Cell().add(new Paragraph(
                        row.getPayment() != null && row.getPayment().getBill() != null && row.getPayment().getBill().getBillClassType() != null ? row.getPayment().getBill().getBillClassType().toString() : "N/A")));

                table.addCell(new Cell().add(new Paragraph(
                        row.getPayment() != null && row.getPayment().getBill() != null ? row.getPayment().getBill().getCreatedAt().toString() : "N/A")));

                table.addCell(new Cell().add(new Paragraph(
                        (row.getPayment() != null && row.getPayment().getCreditCardRefNo() != null) ? row.getPayment().getCreditCardRefNo() : "N/A")));

                table.addCell(new Cell().add(new Paragraph(
                        row.getPayment() != null && row.getPayment().getBank() != null ? row.getPayment().getBank().getName() : "N/A")));

                table.addCell(new Cell().add(new Paragraph(
                        row.getPayment() != null && row.getPayment().getBill() != null && row.getPayment().getBill().getBackwardReferenceBill() != null && row.getPayment().getBill().getBackwardReferenceBill().getDeptId() != null ? row.getPayment().getBill().getBackwardReferenceBill().getDeptId() : "N/A")));

                table.addCell(new Cell().add(new Paragraph(
                        String.format("%.2f", row.getPayment() != null ? row.getPayment().getPaidValue() : 0.0)))); // Format as string
            }

            // Add the table to the document
            document.add(table);
        }
    }

    private void populateTableForProfessionalPayments(Document document, ReportTemplateRowBundle addingBundle) {
        if (addingBundle.getReportTemplateRows() == null || addingBundle.getReportTemplateRows().isEmpty()) {
            // If no data, add a paragraph stating this
            Paragraph noDataParagraph = new Paragraph("No Data for " + addingBundle.getName());
            document.add(noDataParagraph);
        } else {
            // Create a new Table for each call
            Table table = new Table(new float[]{10, 20, 10}); // Set column widths. Adjust as necessary
            table.setWidth(100); // Set width to 100% of available space

            // Add title row with the report name and total
            table.addCell(new Cell(1, 2)
                    .add(new Paragraph(addingBundle.getName())));
            table.addCell(new Cell()
                    .add(new Paragraph(String.format("%.2f", addingBundle.getTotal())))); // Formatting total as a string

            // Add header row only when there is data
            String[] headers = {"Bill No", "Professional", "Fee"};
            for (String header : headers) {
                table.addCell(new Cell().add(new Paragraph(header)));
            }

            // Populate data rows
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                table.addCell(new Cell().add(new Paragraph(
                        row.getBill() != null ? row.getBill().getDeptId() : "N/A")));

                table.addCell(new Cell().add(new Paragraph(
                        row.getBill() != null && row.getBill().getStaff() != null && row.getBill().getStaff().getPerson() != null ? row.getBill().getStaff().getPerson().getNameWithTitle() : "N/A")));

                table.addCell(new Cell().add(new Paragraph(
                        String.format("%.2f", row.getBill() != null ? row.getBill().getNetTotal() : 0.0)))); // Format as string
            }

            // Add the table to the document
            document.add(table);
        }
    }

    private void populateTableForPettyCashPayments(Document document, ReportTemplateRowBundle addingBundle) {
        if (addingBundle.getReportTemplateRows() == null || addingBundle.getReportTemplateRows().isEmpty()) {
            // If no data, add a paragraph stating this
            Paragraph noDataParagraph = new Paragraph("No Data for " + addingBundle.getName());
            document.add(noDataParagraph);
        } else {
            // Create a new Table for each call
            Table table = new Table(new float[]{10, 15, 15, 40}); // Adjust column widths
            table.setWidth(100); // Set width to 100% of available space

            // Add title row with the report name and total
            table.addCell(new Cell(1, 3).add(new Paragraph(addingBundle.getName())));
            table.addCell(new Cell().add(new Paragraph(String.format("%.2f", addingBundle.getTotal())))); // Formatting total as a string

            // Add header row only when there is data
            String[] headers = {"Bill No", "Bill Type", "Fee", "Reference Bills"};
            for (String header : headers) {
                table.addCell(new Cell().add(new Paragraph(header)));
            }

            // Populate data rows
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                table.addCell(new Cell().add(new Paragraph((row.getBill() != null && row.getBill().getDeptId() != null) ? row.getBill().getDeptId() : "N/A")));

                table.addCell(new Cell().add(new Paragraph(row.getBill() != null ? row.getBill().getBillTypeAtomic().getLabel() : "N/A")));

                table.addCell(new Cell().add(new Paragraph(
                        String.format("%.2f", row.getBill() != null ? row.getBill().getNetTotal() : 0.0)))); // Format as string

                String refBills = Stream.of(
                        row.getBill().getBackwardReferenceBill(),
                        row.getBill().getForwardReferenceBill(),
                        row.getBill().getReferenceBill(),
                        row.getBill().getRefundedBill(),
                        row.getBill().getCancelledBill(),
                        row.getBill().getBilledBill())
                        .filter(Objects::nonNull)
                        .map(b -> Optional.ofNullable(b.getDeptId()).orElse("N/A"))
                        .collect(Collectors.joining(", "));

                // Merge cells for Reference Bills
                Cell refBillCell = new Cell(1, 3).add(new Paragraph(refBills));
                table.addCell(refBillCell);
            }

            // Add the table to the document
            document.add(table);
        }
    }

    private void populateTableForPatientDeposits(Document document, ReportTemplateRowBundle addingBundle) {
        if (addingBundle.getReportTemplateRows() == null || addingBundle.getReportTemplateRows().isEmpty()) {
            // If no data, add a paragraph stating this
            Paragraph noDataParagraph = new Paragraph("No Data for " + addingBundle.getName());
            document.add(noDataParagraph);
        } else {
            // Create a new Table for each call
            Table table = new Table(new float[]{10, 20, 20, 20}); // Set column widths. Adjust as necessary
            table.setWidth(100); // Set width to 100% of available space

            // Add title row with the report name and total
            table.addCell(new com.itextpdf.layout.element.Cell(1, 3)
                    .add(new Paragraph(addingBundle.getName())));
            table.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new Paragraph(String.format("%.2f", addingBundle.getTotal())))); // Formatting total as a string

            // Add header row only when there is data
            String[] headers = {"Bill No", "Patient", "Payment Method", "Value"};
            for (String header : headers) {
                table.addCell(new Cell().add(new Paragraph(header)));
            }

            // Populate data rows
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                table.addCell(new Cell().add(new Paragraph(
                        row.getBill() != null ? row.getBill().getDeptId() : "N/A")));

                table.addCell(new Cell().add(new Paragraph(
                        row.getBill() != null && row.getBill().getPatient() != null && row.getBill().getPatient().getPerson() != null ? row.getBill().getPatient().getPerson().getNameWithTitle() : "N/A")));

                table.addCell(new Cell().add(new Paragraph(
                        row.getBill() != null && row.getBill().getPaymentMethod() != null ? row.getBill().getPaymentMethod().getLabel() : "N/A")));

                table.addCell(new Cell().add(new Paragraph(
                        row.getBill() != null ? String.format("%.2f", row.getBill().getNetTotal()) : "0.00"))); // Format as string
            }

            // Add the table to the document
            document.add(table);
        }
    }

    private void populateTableForCompanyCollection(Document document, ReportTemplateRowBundle addingBundle) {
        if (addingBundle.getReportTemplateRows() == null || addingBundle.getReportTemplateRows().isEmpty()) {
            // If no data, add a paragraph stating this
            Paragraph noDataParagraph = new Paragraph("No Data for " + addingBundle.getName());
            document.add(noDataParagraph);
        } else {
            // Create a new Table for each call
            Table table = new Table(new float[]{10, 20, 20, 20}); // Set column widths. Adjust as necessary
            table.setWidth(100); // Set width to 100% of available space

            // Add title row with the report name and total
            table.addCell(new com.itextpdf.layout.element.Cell(1, 3)
                    .add(new Paragraph(addingBundle.getName())));
            table.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new Paragraph(String.format("%.2f", addingBundle.getTotal())))); // Formatting total as a string

            // Add header row only when there is data
            String[] headers = {"Bill No", "Company", "Payment Method", "Value"};
            for (String header : headers) {
                table.addCell(new Cell().add(new Paragraph(header)));
            }

            // Populate data rows
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                table.addCell(new Cell().add(new Paragraph(
                        row.getBill() != null ? row.getBill().getDeptId() : "Not available")));

                table.addCell(new Cell().add(new Paragraph(
                        row.getBill() != null && row.getBill().getFromInstitution() != null ? row.getBill().getFromInstitution().getName() : "Not available")));

                table.addCell(new Cell().add(new Paragraph(
                        row.getBill() != null && row.getBill().getPaymentMethod() != null ? row.getBill().getPaymentMethod().getLabel() : "Not available")));

                table.addCell(new Cell().add(new Paragraph(
                        row.getBill() != null ? String.format("%.2f", row.getBill().getNetTotal()) : "0.00"))); // Format as string
            }

            // Add the table to the document
            document.add(table);
        }
    }

    private void populateTableForCcDeposits(Document document, ReportTemplateRowBundle addingBundle) {
        if (addingBundle.getReportTemplateRows() == null || addingBundle.getReportTemplateRows().isEmpty()) {
            // If no data, add a paragraph stating this
            Paragraph noDataParagraph = new Paragraph("No Data for " + addingBundle.getName());
            document.add(noDataParagraph);
        } else {
            // Create a new Table for each call
            Table table = new Table(new float[]{10, 20, 20, 30, 20}); // Set column widths. Adjust as necessary
            table.setWidth(100); // Set width to 100% of available space

            // Add title row with the report name and total
            table.addCell(new com.itextpdf.layout.element.Cell(1, 4)
                    .add(new Paragraph(addingBundle.getName())));
            table.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new Paragraph(String.format("%.2f", addingBundle.getTotal())))); // Formatting total as a string

            // Add header row only when there is data
            String[] headers = {"Bill No", "Collecting Centre Name", "Collecting Centre Code", "Reference Bill", "Fee"};
            for (String header : headers) {
                table.addCell(new Cell().add(new Paragraph(header)));
            }

            // Populate data rows
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                table.addCell(new Cell().add(new Paragraph(
                        row.getBill() != null ? row.getBill().getDeptId() : "Not available")));

                table.addCell(new Cell().add(new Paragraph(
                        row.getBill() != null && row.getBill().getFromInstitution() != null ? row.getBill().getFromInstitution().getName() : "Not available")));

                table.addCell(new Cell().add(new Paragraph(
                        row.getBill() != null && row.getBill().getFromInstitution() != null ? row.getBill().getFromInstitution().getCode() : "Not available")));

                String refBills = "";
                if (row.getBill() != null) {
                    // Concatenate all possible reference bills
                    refBills = Stream.of(
                            row.getBill().getBackwardReferenceBill(),
                            row.getBill().getForwardReferenceBill(),
                            row.getBill().getReferenceBill(),
                            row.getBill().getRefundedBill(),
                            row.getBill().getCancelledBill(),
                            row.getBill().getBilledBill())
                            .filter(Objects::nonNull)
                            .map(b -> b.getDeptId())
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(", "));
                }
                table.addCell(new Cell().add(new Paragraph(refBills.isEmpty() ? "Not available" : refBills)));

                table.addCell(new Cell().add(new Paragraph(
                        row.getBill() != null ? String.format("%.2f", row.getBill().getNetTotal()) : "0.00"))); // Format as string
            }

            // Add the table to the document
            document.add(table);
        }
    }

    private void populateTableForItemSummaryGroupedByCategory(Document document, ReportTemplateRowBundle addingBundle) {
        if (addingBundle.getReportTemplateRows() != null && !addingBundle.getReportTemplateRows().isEmpty()) {
            // Create a new Table for each call
            Table table = new Table(new float[]{10, 40, 10, 10, 10, 10, 10}); // Example column widths
            table.setWidth(100); // Set width to 100% of available space

            // Add title row with bundle name and total
            table.addCell(new com.itextpdf.layout.element.Cell(1, 6)
                    .add(new Paragraph(addingBundle.getName())));
            table.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new Paragraph(String.format("%.2f", addingBundle.getTotal())))); // Formatting total as a string

            // Add headers
            String[] headers = {"Category", "Item / Service", "Count", "Hospital Fee", "Professional Fee", "Discount", "Net Amount"};
            for (String header : headers) {
                table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(header)));
            }

            // Populate table with data rows
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        row.getCategory() != null ? row.getCategory().getName() : "")));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        row.getItem() != null ? row.getItem().getName() : "")));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        String.valueOf(row.getItemCount()))));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        String.format("%.2f", row.getItemHospitalFee())))); // Format as string
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        String.format("%.2f", row.getItemProfessionalFee())))); // Format as string
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        String.format("%.2f", row.getItemDiscountAmount())))); // Format as string
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        String.format("%.2f", row.getItemNetTotal())))); // Format as string
            }

            // Add the table to the document
            document.add(table);
        } else {
            // Add a paragraph for no data
            Paragraph noDataParagraph = new Paragraph("No Data for " + addingBundle.getName());
            document.add(noDataParagraph);
        }
    }

    private void populateTableForCreditItemSummaryGroupedByCategory(Document document, ReportTemplateRowBundle addingBundle) {
        if (addingBundle.getReportTemplateRows() != null && !addingBundle.getReportTemplateRows().isEmpty()) {
            // Create a new Table for each call

            Table table = new Table(new float[]{40, 60, 10, 10, 10, 10, 10});
            table.setWidth(UnitValue.createPercentValue(100));
            Style cellStyle = new Style().setFontSize(9);  // Set font size

            // Add title row with bundle name and total
            table.addCell(new com.itextpdf.layout.element.Cell(1, 6)
                    .add(new Paragraph(addingBundle.getName())));
            table.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new Paragraph(String.format("%.2f", addingBundle.getTotal())))); // Formatting total as a string

            // Add headers
            String[] headers = {"Category", "Item / Service", "Count", "Hospital Fee", "Professional Fee", "Discount", "Net Amount"};
            for (String header : headers) {
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(header)));
            }

            // Populate table with data rows
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        row.getCategory() != null ? row.getCategory().getName() : "")));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        row.getItem() != null ? row.getItem().getName() : "")));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        String.valueOf(row.getItemCount()))));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        String.format("%.2f", row.getItemHospitalFee())))); // Format as string
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        String.format("%.2f", row.getItemProfessionalFee())))); // Format as string
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        String.format("%.2f", row.getItemDiscountAmount())))); // Format as string
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        String.format("%.2f", row.getItemNetTotal())))); // Format as string
            }

            table.getChildren().forEach(element -> {
                if (element instanceof Cell) {
                    ((Cell) element).addStyle(cellStyle);
                }
            });

            // Add the table to the document
            document.add(table);
        } else {
            // Add a paragraph for no data
            Paragraph noDataParagraph = new Paragraph("No Data for " + addingBundle.getName());
            document.add(noDataParagraph);
        }
    }

    private void populateTableForIncomeByCategoryWithProfessionalFee(Document document, ReportTemplateRowBundle addingBundle) {
        if (addingBundle.getReportTemplateRows() != null && !addingBundle.getReportTemplateRows().isEmpty()) {

            document.add(new Paragraph(addingBundle.getName()));
            document.add(new Paragraph(CommonFunctions.getDateTimeFormat(searchController.getFromDate()) + " to " + CommonFunctions.getDateTimeFormat(searchController.getToDate())));

            Table table = new Table(new float[]{55, 20, 25, 25, 25, 25, 25});
            table.setWidth(UnitValue.createPercentValue(100));

            // Add headers
            String[] headers = {"Category", "Count", "Gross Value", "Hospital Fee", "Professional Fee", "Discount", "Net Amount"};
            for (String header : headers) {
                table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(header)));
            }

            table.addFooterCell(new com.itextpdf.layout.element.Cell(1, 6).add(new Paragraph("Total")));
            table.addFooterCell(new Paragraph(String.format("%.2f", addingBundle.getTotal())));

            // Populate table with data rows
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        row.getCategory() != null ? row.getCategory().getName() : "")));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        String.valueOf(row.getItemCount()))));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        String.format("%.2f", row.getItemTotal())))); // Format as string
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        String.format("%.2f", row.getItemHospitalFee())))); // Format as string
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        String.format("%.2f", row.getItemProfessionalFee())))); // Format as string
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        String.format("%.2f", row.getItemDiscountAmount())))); // Format as string
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        String.format("%.2f", row.getItemNetTotal())))); // Format as string
            }

            // Add the table to the document
            document.add(table);
        } else {
            // Add a paragraph for no data
            Paragraph noDataParagraph = new Paragraph("No Data for " + addingBundle.getName());
            document.add(noDataParagraph);
        }
    }

    private void populateTableForIncomeByCategoryWithoutProfessionalFee(Document document, ReportTemplateRowBundle addingBundle) {
        if (addingBundle.getReportTemplateRows() != null && !addingBundle.getReportTemplateRows().isEmpty()) {

            document.add(new Paragraph(addingBundle.getName()));
            document.add(new Paragraph(CommonFunctions.getDateTimeFormat(searchController.getFromDate()) + " to " + CommonFunctions.getDateTimeFormat(searchController.getToDate())));

            Table table = new Table(new float[]{55, 20, 25, 25, 25, 25});
            table.setWidth(UnitValue.createPercentValue(100));

            // Add headers
            String[] headers = {"Category", "Count", "Gross Value", "Hospital Fee", "Discount", "Net Amount"};
            for (String header : headers) {
                table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(header)));
            }

            table.addFooterCell(new com.itextpdf.layout.element.Cell(1, 5).add(new Paragraph("Total")));
            table.addFooterCell(new Paragraph(String.format("%.2f", addingBundle.getTotal())));

            // Populate table with data rows
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        row.getCategory() != null ? row.getCategory().getName() : "")));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        String.valueOf(row.getItemCount()))));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        String.format("%.2f", row.getItemTotal())))); // Format as string
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        String.format("%.2f", row.getItemHospitalFee())))); // Format as string
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        String.format("%.2f", row.getItemDiscountAmount())))); // Format as string
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                        String.format("%.2f", row.getItemNetTotal())))); // Format as string
            }

            // Add the table to the document
            document.add(table);
        } else {
            // Add a paragraph for no data
            Paragraph noDataParagraph = new Paragraph("No Data for " + addingBundle.getName());
            document.add(noDataParagraph);
        }
    }

    private void populateTableForItemizedSalesReportWithProfessionalFee(Document document, ReportTemplateRowBundle addingBundle) {
        if (addingBundle != null && addingBundle.getReportTemplateRows() != null && !addingBundle.getReportTemplateRows().isEmpty()) {
            document.add(new Paragraph(addingBundle.getName()));
            document.add(new Paragraph(CommonFunctions.getDateTimeFormat(searchController.getFromDate()) + " to " + CommonFunctions.getDateTimeFormat(searchController.getToDate())));

            Table table = new Table(new float[]{40, 10, 60, 10, 20, 20, 10, 10, 10, 10});
            table.setWidth(UnitValue.createPercentValue(100));

            Style cellStyle = new Style().setFontSize(8);  // Set font size

            // Add headers
            String[] headers = {"Category", "Code", "Item/Service", "Count", "Bill No", "Patient", "Hospital Fee", "Professional Fee", "Discount", "Net Amount"};
            for (String header : headers) {
                table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(header)));
                table.addStyle(cellStyle);
            }

            table.addFooterCell(new com.itextpdf.layout.element.Cell(1, 9).add(new Paragraph("Total")));
            table.addFooterCell(new Paragraph(String.format("%.2f", addingBundle.getTotal())));

            // Populate table with data rows
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(row.getCategory() != null && row.getCategory().getName() != null ? row.getCategory().getName() : "")));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(row.getItem() != null && row.getItem().getCode() != null ? row.getItem().getCode() : "")));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(row.getItem() != null && row.getItem().getName() != null ? row.getItem().getName() : "")));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.valueOf(row.getItemCount()))));

                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(row.getBillItem() != null && row.getBillItem().getBill() != null && row.getBillItem().getBill().getDeptId() != null ? row.getBillItem().getBill().getDeptId() : "")));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(row.getBillItem() != null && row.getBillItem().getBill() != null && row.getBillItem().getBill().getPatient() != null && row.getBillItem().getBill().getPatient().getPerson() != null ? row.getBillItem().getBill().getPatient().getPerson().getNameWithTitle() : "")));

                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.format("%.2f", row.getItemHospitalFee() != null ? row.getItemHospitalFee() : 0))));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.format("%.2f", row.getItemProfessionalFee())))); // Format as string
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.format("%.2f", row.getItemDiscountAmount() != null ? row.getItemDiscountAmount() : 0))));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.format("%.2f", row.getItemNetTotal() != null ? row.getItemNetTotal() : 0))));
            }

            table.getChildren().forEach(element -> {
                if (element instanceof Cell) {
                    ((Cell) element).addStyle(cellStyle);
                }
            });

            // Add the table to the document
            document.add(table);
        } else {
            // Add a paragraph for no data
            Paragraph noDataParagraph = new Paragraph("No Data for " + (addingBundle != null ? addingBundle.getName() : "unknown"));
            document.add(noDataParagraph);
        }

    }

    private void populateTableForItemizedSalesReportWithoutProfessionalFee(Document document, ReportTemplateRowBundle addingBundle) {
        if (addingBundle != null && addingBundle.getReportTemplateRows() != null && !addingBundle.getReportTemplateRows().isEmpty()) {
            document.add(new Paragraph(addingBundle.getName()));
            document.add(new Paragraph(CommonFunctions.getDateTimeFormat(searchController.getFromDate()) + " to " + CommonFunctions.getDateTimeFormat(searchController.getToDate())));

            Table table = new Table(new float[]{40, 10, 60, 10, 20, 20, 10, 10, 10});
            table.setWidth(UnitValue.createPercentValue(100));
            Style cellStyle = new Style().setFontSize(8);  // Set font size

            // Add headers
            String[] headers = {"Category", "Code", "Item/Service", "Count", "Bill No", "Patient", "Hospital Fee", "Discount", "Net Amount"};
            for (String header : headers) {
                table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(header)));
                table.addStyle(cellStyle);
            }

            table.addFooterCell(new com.itextpdf.layout.element.Cell(1, 8).add(new Paragraph("Total")));
            table.addFooterCell(new Paragraph(String.format("%.2f", addingBundle.getTotal())));

            // Populate table with data rows
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(row.getCategory() != null && row.getCategory().getName() != null ? row.getCategory().getName() : "")));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(row.getItem() != null && row.getItem().getCode() != null ? row.getItem().getCode() : "")));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(row.getItem() != null && row.getItem().getName() != null ? row.getItem().getName() : "")));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.valueOf(row.getItemCount()))));

                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(row.getBillItem() != null && row.getBillItem().getBill() != null && row.getBillItem().getBill().getDeptId() != null ? row.getBillItem().getBill().getDeptId() : "")));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(row.getBillItem() != null && row.getBillItem().getBill() != null && row.getBillItem().getBill().getPatient() != null && row.getBillItem().getBill().getPatient().getPerson() != null ? row.getBillItem().getBill().getPatient().getPerson().getNameWithTitle() : "")));

                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.format("%.2f", row.getItemHospitalFee() != null ? row.getItemHospitalFee() : 0))));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.format("%.2f", row.getItemDiscountAmount() != null ? row.getItemDiscountAmount() : 0))));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.format("%.2f", row.getItemNetTotal() != null ? row.getItemNetTotal() : 0))));
            }

            table.getChildren().forEach(element -> {
                if (element instanceof Cell) {
                    ((Cell) element).addStyle(cellStyle);
                }
            });

            // Add the table to the document
            document.add(table);
        } else {
            // Add a paragraph for no data
            Paragraph noDataParagraph = new Paragraph("No Data for " + (addingBundle != null ? addingBundle.getName() : "unknown"));
            document.add(noDataParagraph);
        }

    }

    private void populateTableForDepartmentCollection(Document document, ReportTemplateRowBundle addingBundle) {
        if (addingBundle.getReportTemplateRows() == null || addingBundle.getReportTemplateRows().isEmpty()) {
            // If no data, add a paragraph stating this
            Paragraph noDataParagraph = new Paragraph("No Data for " + addingBundle.getName());
            document.add(noDataParagraph);
        } else {
            // Create a new Table for each call
            Table table = new Table(new float[]{10, 40}); // Set column widths. Adjust as necessary
            table.setWidth(100); // Set width to 100% of available space

            // Add title row with the report name and total
            table.addCell(new com.itextpdf.layout.element.Cell(1, 1)
                    .add(new Paragraph(addingBundle.getName())));
            table.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new Paragraph(String.format("%.2f", addingBundle.getTotal())))); // Formatting total as a string

            // Add header row only when there is data
            String[] headers = {"Department", "Collection"};
            for (String header : headers) {
                table.addCell(new Cell().add(new Paragraph(header)));
            }

            // Populate data rows
            for (ReportTemplateRow row : addingBundle.getReportTemplateRows()) {
                table.addCell(new Cell().add(new Paragraph(
                        row.getDepartment() != null ? row.getDepartment().getName() : "Not available")));
                table.addCell(new Cell().add(new Paragraph(
                        String.format("%.2f", row.getRowValue() != null ? row.getRowValue() : 0.0)))); // Format as string
            }

            // Add the table to the document
            document.add(table);
        }
    }

    private void populateTitleBundleForPdf(Document document, ReportTemplateRowBundle addingBundle) {
        // Create a solid line separator
        SolidLine lineDrawer = new SolidLine(1f); // 1f is the line width
        LineSeparator lineSeparator = new LineSeparator(lineDrawer);
        lineSeparator.setStrokeColor(ColorConstants.BLACK); // Set color of the line

        // Visual separator before the title
        document.add(lineSeparator);

        // Create a table with two columns
        float[] columnWidths = {2, 1}; // Adjust column widths as needed
        Table table; // Remove table border
        table = new Table(columnWidths)
                .useAllAvailableWidth();

        // Left-aligned cell (Title)
        Cell titleCell = new Cell()
                .add(new Paragraph(addingBundle.getName()).setBold().setTextAlignment(TextAlignment.LEFT));
        titleCell.setBorder(Border.NO_BORDER);
        table.addCell(titleCell);

        // Right-aligned cell (Total)
        Cell totalCell = new Cell()
                .add(new Paragraph("Total  : " + String.format("%.2f", addingBundle.getTotal())).setItalic().setTextAlignment(TextAlignment.RIGHT));
        totalCell.setBorder(Border.NO_BORDER);
        table.addCell(totalCell);
        table.setMarginBottom(8);
        table.setMarginTop(8);
        table.setBorder(Border.NO_BORDER);

        // Add the table to the document
        document.add(table);

        // Visual separator after the total
        document.add(lineSeparator);
    }

    // New method for DTO-based bundles
    public StreamedContent createPdfForDtoBundle(com.divudi.core.data.dto.DailyReturnBundleDTO rootBundle) throws IOException {
        if (rootBundle == null) {
            return null;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        if (rootBundle.getBundles() == null || rootBundle.getBundles().isEmpty()) {
            addDtoDataToPdf(document, rootBundle, rootBundle.getBundleType());
        } else {
            for (com.divudi.core.data.dto.DailyReturnBundleDTO childBundle : rootBundle.getBundles()) {
                addDtoDataToPdf(document, childBundle, childBundle.getBundleType());
            }
        }

        document.close();

        // Create a ByteArrayInputStream from the byte array
        byte[] bytes = outputStream.toByteArray();
        InputStream inputStream = new ByteArrayInputStream(bytes);

        // Create and return StreamedContent
        StreamedContent pdfSc = DefaultStreamedContent.builder()
                .name("daily_return_dto_" + new Date().getTime() + ".pdf")
                .contentType("application/pdf")
                .stream(() -> inputStream)
                .build();

        return pdfSc;
    }
    
    private void addDtoDataToPdf(Document document, com.divudi.core.data.dto.DailyReturnBundleDTO addingBundle, String type) {
        if (addingBundle == null) {
            return;
        }

        try {
            PdfFont font = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);

            // Add bundle title
            Paragraph title = new Paragraph(addingBundle.getName())
                    .setFont(boldFont)
                    .setFontSize(14)
                    .setMarginBottom(5)
                    .setTextAlignment(TextAlignment.LEFT);
            document.add(title);

            // Only add data table if there are rows to display
            if (addingBundle.getRows() != null && !addingBundle.getRows().isEmpty()) {
                // Create table with 7 columns
                Table table = new Table(UnitValue.createPercentArray(new float[]{2, 3, 1.5f, 2, 2, 1.5f, 2}));
                table.setWidth(UnitValue.createPercentValue(100));

                // Add header row
                String[] headers = {"Category", "Item / Service", "Count", "Hospital Fee", "Professional Fee", "Discount", "Net Amount"};
                for (String header : headers) {
                    Cell headerCell = new Cell()
                            .add(new Paragraph(header).setFont(boldFont).setFontSize(10))
                            .setTextAlignment(TextAlignment.CENTER)
                            .setBackgroundColor(new DeviceRgb(240, 240, 240));
                    table.addHeaderCell(headerCell);
                }

                // Add data rows
                for (com.divudi.core.data.dto.DailyReturnRowDTO row : addingBundle.getRows()) {
                    // Category
                    table.addCell(new Cell().add(new Paragraph(row.getCategoryName() != null ? row.getCategoryName() : "").setFont(font).setFontSize(9)));
                    // Item
                    table.addCell(new Cell().add(new Paragraph(row.getItemName() != null ? row.getItemName() : "").setFont(font).setFontSize(9)));
                    // Count
                    table.addCell(new Cell().add(new Paragraph(String.valueOf(row.getItemCount() != null ? row.getItemCount() : 0)).setFont(font).setFontSize(9)).setTextAlignment(TextAlignment.RIGHT));
                    // Hospital Fee
                    table.addCell(new Cell().add(new Paragraph(String.format("%.2f", row.getItemHospitalFee() != null ? row.getItemHospitalFee() : 0.0)).setFont(font).setFontSize(9)).setTextAlignment(TextAlignment.RIGHT));
                    // Professional Fee
                    table.addCell(new Cell().add(new Paragraph(String.format("%.2f", row.getItemProfessionalFee() != null ? row.getItemProfessionalFee() : 0.0)).setFont(font).setFontSize(9)).setTextAlignment(TextAlignment.RIGHT));
                    // Discount
                    table.addCell(new Cell().add(new Paragraph(String.format("%.2f", row.getItemDiscountAmount() != null ? row.getItemDiscountAmount() : 0.0)).setFont(font).setFontSize(9)).setTextAlignment(TextAlignment.RIGHT));
                    // Net Amount
                    table.addCell(new Cell().add(new Paragraph(String.format("%.2f", row.getItemNetTotal() != null ? row.getItemNetTotal() : 0.0)).setFont(font).setFontSize(9)).setTextAlignment(TextAlignment.RIGHT));
                }

                document.add(table);
            } else {
                // If no data, add a message
                Paragraph noData = new Paragraph("No Data for " + addingBundle.getName())
                        .setFont(font)
                        .setFontSize(10)
                        .setMarginBottom(5)
                        .setTextAlignment(TextAlignment.LEFT);
                document.add(noData);
            }

            // Add bundle total
            addDtoBundleTotal(document, addingBundle);

        } catch (IOException e) {
            // Handle font creation error
            System.err.println("Error creating PDF fonts: " + e.getMessage());
        }
    }
    
    private void addDtoBundleTotal(Document document, com.divudi.core.data.dto.DailyReturnBundleDTO addingBundle) {
        try {
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
            
            // Line separator before the total
            LineSeparator lineSeparator = new LineSeparator(new SolidLine());
            document.add(lineSeparator);

            // Create a table with 2 columns for the total (left aligned title, right aligned value)
            Table table = new Table(UnitValue.createPercentArray(new float[]{80, 20}));
            table.setWidth(UnitValue.createPercentValue(100));

            // Left-aligned cell (empty or can contain additional info)
            Cell titleCell = new Cell()
                    .add(new Paragraph("Total for " + addingBundle.getName()).setBold().setTextAlignment(TextAlignment.LEFT));
            titleCell.setBorder(Border.NO_BORDER);
            table.addCell(titleCell);

            // Right-aligned cell (Total)
            Cell totalCell = new Cell()
                    .add(new Paragraph("Total  : " + String.format("%.2f", addingBundle.getTotal() != null ? addingBundle.getTotal() : 0.0)).setFont(boldFont).setTextAlignment(TextAlignment.RIGHT));
            totalCell.setBorder(Border.NO_BORDER);
            table.addCell(totalCell);
            table.setMarginBottom(8);
            table.setMarginTop(8);
            table.setBorder(Border.NO_BORDER);

            // Add the table to the document
            document.add(table);

            // Visual separator after the total
            document.add(lineSeparator);
        } catch (IOException e) {
            // Handle font creation error
            System.err.println("Error creating PDF fonts for total: " + e.getMessage());
        }
    }


}
