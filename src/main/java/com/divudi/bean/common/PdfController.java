package com.divudi.bean.common;

import ca.uhn.fhir.model.api.IElement;
import com.divudi.bean.hr.StaffImageController;
import com.divudi.data.InvestigationItemType;
import com.divudi.data.InvestigationItemValueType;
import com.divudi.data.ReportTemplateRow;
import com.divudi.data.ReportTemplateRowBundle;
import com.divudi.entity.lab.InvestigationItem;
import com.divudi.entity.lab.PatientReport;
import com.divudi.entity.lab.PatientReportItemValue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.inject.Named;
import javax.enterprise.context.RequestScoped;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Objects;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import java.util.Optional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Canvas;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.text.pdf.qrcode.BitMatrix;
import java.util.function.Supplier;
import javax.inject.Inject;

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

    /**
     * Creates a new instance of PdfController
     */
    public PdfController() {
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

        // Process patient report item values
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

        // Process report items (Labels)
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

        // Example positions and styles (you may need to adjust these based on your layout)
        float leftMargin = 50;
        float topMargin = pageHeight - 50;
        float lineSpacing = 15;
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        float fontSize = 12;

        // Patient Name
        if (report.getPatientInvestigation().getPatient().getPerson().getNameWithTitle() != null) {
            String patientName = report.getPatientInvestigation().getPatient().getPerson().getNameWithTitle();
            Paragraph p = new Paragraph("Patient Name: " + patientName)
                    .setFont(font)
                    .setFontSize(fontSize)
                    .setFixedPosition(leftMargin, topMargin, pageWidth - leftMargin * 2);
            document.add(p);
            topMargin -= lineSpacing;
        }

        // PHN
        if (report.getPatientInvestigation().getPatient().getPhn() != null) {
            String phn = report.getPatientInvestigation().getPatient().getPhn();
            Paragraph p = new Paragraph("PHN: " + phn)
                    .setFont(font)
                    .setFontSize(fontSize)
                    .setFixedPosition(leftMargin, topMargin, pageWidth - leftMargin * 2);
            document.add(p);
            topMargin -= lineSpacing;
        }

        // Phone
        if (report.getPatientInvestigation().getPatient().getPerson().getPhone() != null) {
            String phone = report.getPatientInvestigation().getPatient().getPerson().getPhone();
            Paragraph p = new Paragraph("Phone: " + phone)
                    .setFont(font)
                    .setFontSize(fontSize)
                    .setFixedPosition(leftMargin, topMargin, pageWidth - leftMargin * 2);
            document.add(p);
            topMargin -= lineSpacing;
        }

        // Age
        if (report.getPatientInvestigation().getPatient().getPerson().getAgeAsShortString() != null) {
            String age = report.getPatientInvestigation().getPatient().getPerson().getAgeAsShortString();
            Paragraph p = new Paragraph("Age: " + age)
                    .setFont(font)
                    .setFontSize(fontSize)
                    .setFixedPosition(leftMargin, topMargin, pageWidth - leftMargin * 2);
            document.add(p);
            topMargin -= lineSpacing;
        }

        // Sex
        if (report.getPatientInvestigation().getPatient().getPerson().getSex() != null) {
            String sex = report.getPatientInvestigation().getPatient().getPerson().getSex().toString();
            Paragraph p = new Paragraph("Sex: " + sex)
                    .setFont(font)
                    .setFontSize(fontSize)
                    .setFixedPosition(leftMargin, topMargin, pageWidth - leftMargin * 2);
            document.add(p);
            topMargin -= lineSpacing;
        }

        // Investigation Name
        if (report.getPatientInvestigation().getInvestigation().getName() != null) {
            String investigationName = report.getPatientInvestigation().getInvestigation().getName();
            Paragraph p = new Paragraph("Investigation: " + investigationName)
                    .setFont(font)
                    .setFontSize(fontSize)
                    .setFixedPosition(leftMargin, topMargin, pageWidth - leftMargin * 2);
            document.add(p);
            topMargin -= lineSpacing;
        }

        // Specimen
        if (report.getPatientInvestigation().getInvestigation().getSample() != null
                && report.getPatientInvestigation().getInvestigation().getSample().getName() != null) {
            String specimen = report.getPatientInvestigation().getInvestigation().getSample().getName();
            Paragraph p = new Paragraph("Specimen: " + specimen)
                    .setFont(font)
                    .setFontSize(fontSize)
                    .setFixedPosition(leftMargin, topMargin, pageWidth - leftMargin * 2);
            document.add(p);
            topMargin -= lineSpacing;
        }

        // Sampled Time
        if (report.getPatientInvestigation().getSampledAt() != null) {
            String sampledTime = new java.text.SimpleDateFormat("hh:mm a").format(report.getPatientInvestigation().getSampledAt());
            Paragraph p = new Paragraph("Sampled Time: " + sampledTime)
                    .setFont(font)
                    .setFontSize(fontSize)
                    .setFixedPosition(leftMargin, topMargin, pageWidth - leftMargin * 2);
            document.add(p);
            topMargin -= lineSpacing;
        }

        // Billed Date
        if (report.getPatientInvestigation().getBillItem().getBill().getCreatedAt() != null) {
            String billedDate = new java.text.SimpleDateFormat("dd/MM/yyyy").format(report.getPatientInvestigation().getBillItem().getBill().getCreatedAt());
            Paragraph p = new Paragraph("Billed Date: " + billedDate)
                    .setFont(font)
                    .setFontSize(fontSize)
                    .setFixedPosition(leftMargin, topMargin, pageWidth - leftMargin * 2);
            document.add(p);
            topMargin -= lineSpacing;
        }

        // Billed Time
        if (report.getPatientInvestigation().getBillItem().getBill().getCreatedAt() != null) {
            String billedTime = new java.text.SimpleDateFormat("hh:mm a").format(report.getPatientInvestigation().getBillItem().getBill().getCreatedAt());
            Paragraph p = new Paragraph("Billed Time: " + billedTime)
                    .setFont(font)
                    .setFontSize(fontSize)
                    .setFixedPosition(leftMargin, topMargin, pageWidth - leftMargin * 2);
            document.add(p);
            topMargin -= lineSpacing;
        }

        // Bill Number
        if (report.getPatientInvestigation().getBillItem().getBill().getDeptId() != null) {
            String billNo = report.getPatientInvestigation().getBillItem().getBill().getDeptId();
            Paragraph p = new Paragraph("Bill No: " + billNo)
                    .setFont(font)
                    .setFontSize(fontSize)
                    .setFixedPosition(leftMargin, topMargin, pageWidth - leftMargin * 2);
            document.add(p);
            topMargin -= lineSpacing;
        }

        // Reported Date
        if (report.getCreatedAt() != null) {
            String reportedDate = new java.text.SimpleDateFormat("dd/MM/yyyy").format(report.getCreatedAt());
            Paragraph p = new Paragraph("Reported Date: " + reportedDate)
                    .setFont(font)
                    .setFontSize(fontSize)
                    .setFixedPosition(leftMargin, topMargin, pageWidth - leftMargin * 2);
            document.add(p);
            topMargin -= lineSpacing;
        }

        // Reported Time
        if (report.getCreatedAt() != null) {
            String reportedTime = new java.text.SimpleDateFormat("hh:mm a").format(report.getCreatedAt());
            Paragraph p = new Paragraph("Reported Time: " + reportedTime)
                    .setFont(font)
                    .setFontSize(fontSize)
                    .setFixedPosition(leftMargin, topMargin, pageWidth - leftMargin * 2);
            document.add(p);
            topMargin -= lineSpacing;
        }

        // Referring Doctor
        if (report.getPatientInvestigation().getBillItem().getBill().getReferredBy() != null
                && report.getPatientInvestigation().getBillItem().getBill().getReferredBy().getPerson().getNameWithTitle() != null) {
            String referringDoctor = report.getPatientInvestigation().getBillItem().getBill().getReferredBy().getPerson().getNameWithTitle();
            Paragraph p = new Paragraph("Referring Doctor: " + referringDoctor)
                    .setFont(font)
                    .setFontSize(fontSize)
                    .setFixedPosition(leftMargin, topMargin, pageWidth - leftMargin * 2);
            document.add(p);
            topMargin -= lineSpacing;
        }

        // Authorized User Details
        if (report.getApproveUser() != null && report.getApproveUser().getStaff() != null) {
            // Authorized Code
            if (report.getApproveUser().getStaff().getCode() != null) {
                String authorizedCode = report.getApproveUser().getStaff().getCode();
                Paragraph p = new Paragraph("Authorized Code: " + authorizedCode)
                        .setFont(font)
                        .setFontSize(fontSize)
                        .setFixedPosition(leftMargin, topMargin, pageWidth - leftMargin * 2);
                document.add(p);
                topMargin -= lineSpacing;
            }

            // Authorized Position
            if (report.getApproveUser().getStaff().getSpeciality() != null
                    && report.getApproveUser().getStaff().getSpeciality().getName() != null) {
                String authorizedPosition = report.getApproveUser().getStaff().getSpeciality().getName();
                Paragraph p = new Paragraph("Authorized Position: " + authorizedPosition)
                        .setFont(font)
                        .setFontSize(fontSize)
                        .setFixedPosition(leftMargin, topMargin, pageWidth - leftMargin * 2);
                document.add(p);
                topMargin -= lineSpacing;
            }

            // Authorized Qualification
            if (report.getApproveUser().getStaff().getQualification() != null) {
                String authorizedQualification = report.getApproveUser().getStaff().getQualification();
                Paragraph p = new Paragraph("Authorized Qualification: " + authorizedQualification)
                        .setFont(font)
                        .setFontSize(fontSize)
                        .setFixedPosition(leftMargin, topMargin, pageWidth - leftMargin * 2);
                document.add(p);
                topMargin -= lineSpacing;
            }
        }

        // Collecting Center
        if (report.getPatientInvestigation().getBillItem().getBill().getCollectingCentre() != null
                && report.getPatientInvestigation().getBillItem().getBill().getCollectingCentre().getChequePrintingName() != null) {
            String collectingCenter = report.getPatientInvestigation().getBillItem().getBill().getCollectingCentre().getChequePrintingName();
            Paragraph p = new Paragraph("Collecting Center: " + collectingCenter)
                    .setFont(font)
                    .setFontSize(fontSize)
                    .setFixedPosition(leftMargin, topMargin, pageWidth - leftMargin * 2);
            document.add(p);
            topMargin -= lineSpacing;
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
                        signatureImage.setFixedPosition(leftMargin, topMargin - 100); // Adjust position as needed
                        signatureImage.scaleToFit(100, 50); // Adjust size as needed
                        document.add(signatureImage);
                        topMargin -= 100; // Update the top margin if needed
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
                // Optionally, add a page break between tables
                // document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            }
        }

        document.close();

        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        // Set the downloading file
        return DefaultStreamedContent.builder()
                .name("bundle_report.pdf")
                .contentType("application/pdf")
                .stream(() -> inputStream)
                .build();
    }

    private void addDataToPdf(Document document, ReportTemplateRowBundle addingBundle, String type) {
        if (type == null || type.isEmpty()) {
            type = "BillList";
        }

        // Create a new Table for each call
        Table table = new Table(new float[]{10, 40, 15, 15, 10, 10}); // Adjust column widths as needed
        table.setWidth(100); // Set width to 100% of available space

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
            default:
                table.addCell(new Cell().add(new Paragraph("Data for unknown type"))); // Default handling for unknown types
                break;
        }

        // Add the table to the document
        document.add(table);

        // Optionally, add spacing or a separator between tables
        document.add(new Paragraph("\n"));
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
                        row.getPayment() != null && row.getPayment().getBill() != null ? row.getPayment().getBill().getDeptId() : "N/A")));

                table.addCell(new Cell().add(new Paragraph(
                        row.getPayment() != null && row.getPayment().getBill() != null ? row.getPayment().getBill().getBillClassType().toString() : "N/A")));

                table.addCell(new Cell().add(new Paragraph(
                        row.getPayment() != null && row.getPayment().getBill() != null ? row.getPayment().getBill().getCreatedAt().toString() : "N/A")));

                table.addCell(new Cell().add(new Paragraph(
                        row.getPayment() != null ? row.getPayment().getCreditCardRefNo() : "N/A")));

                table.addCell(new Cell().add(new Paragraph(
                        row.getPayment() != null && row.getPayment().getBank() != null ? row.getPayment().getBank().getName() : "N/A")));

                table.addCell(new Cell().add(new Paragraph(
                        row.getPayment() != null && row.getPayment().getBill() != null && row.getPayment().getBill().getBackwardReferenceBill() != null ? row.getPayment().getBill().getBackwardReferenceBill().getDeptId() : "N/A")));

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
                table.addCell(new Cell().add(new Paragraph(
                        row.getBill() != null ? row.getBill().getDeptId() : "N/A")));

                table.addCell(new Cell().add(new Paragraph(
                        row.getBill() != null ? row.getBill().getBillTypeAtomic().getLabel() : "N/A")));

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

        // Create a title paragraph for the report name
        Paragraph titleParagraph = new Paragraph(addingBundle.getName())
                .setTextAlignment(TextAlignment.LEFT)
                .setBold();
        document.add(titleParagraph);

        // Paragraph for the total
        Paragraph totalParagraph = new Paragraph("Total: " + String.format("%.2f", addingBundle.getTotal()))
                .setTextAlignment(TextAlignment.RIGHT)
                .setItalic();
        document.add(totalParagraph);

        // Visual separator after the total
        document.add(lineSeparator);
    }

}
