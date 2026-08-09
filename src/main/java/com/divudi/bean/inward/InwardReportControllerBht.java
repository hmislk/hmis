/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.FeeType;
import static com.divudi.core.data.FeeType.Service;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.hr.ReportKeyWord;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.data.table.String1Value2;
import com.divudi.core.data.table.String2Value4;
import com.divudi.core.entity.Appointment;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.PatientItem;
import com.divudi.core.entity.Speciality;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PatientItemFacade;
import com.divudi.core.facade.PatientRoomFacade;
import com.divudi.service.BillService;
import com.divudi.core.data.dto.InpatientPharmacyIssueDTO;
import com.divudi.core.data.dto.InpatientPharmacyNetSummaryDTO;
import com.divudi.core.data.dto.InpatientServiceIssueDTO;
import com.divudi.core.data.dto.BillListReportDTO;
import com.divudi.core.data.dto.InwardProfessionalPaymentAdmissionDTO;
import com.divudi.core.data.dto.InwardProfessionalPaymentAdmissionGroupDTO;
import com.divudi.core.data.dto.InwardProfessionalPaymentDetailRowDTO;
import com.divudi.core.data.dto.InwardProfessionalPaymentFeeRowDTO;
import com.divudi.core.data.dto.InwardProfessionalPaymentReportRowDTO;
import com.divudi.core.entity.Service;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.persistence.TemporalType;
import javax.servlet.http.HttpServletResponse;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
// NOTE: com.lowagie.text.Document/Font are used fully-qualified in the PDF
// export methods below (not imported) - org.apache.poi.ss.usermodel.Font
// (pulled in by the wildcard import) would otherwise collide with the
// same-named PDF class.
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author pdhs
 */
@Named
@SessionScoped
public class InwardReportControllerBht implements Serializable {

    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    PatientItemFacade patientItemFacade;
    @EJB
    PatientRoomFacade patientRoomFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    BillService billService;
    ////
    @Inject
    private SessionController sessionController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    AdmissionController admissionController;
    @Inject
    InwardSearch inwardSearch;

    PatientEncounter patientEncounter;
    Bill bill;
    private AdmissionType admissionType;
    private PaymentMethod paymentMethod;
    private Institution institution;
    private List<OpdService> opdServices;
    List<String1Value2> timedServices;
    List<RoomChargeInward> roomChargeInwards;
    List<String1Value2> professionals;
    List<String2Value4> inwardCharges;
    List<BillItem> creditPayment;
    List<Bill> paidbyPatientBillList;
    List<PatientRoom> patientRooms;

    private List<BillItem> pharmacyIssueBillItemsToPatientEncounter;
    private double pharmacyIssueBillItemsToPatientEncounterNetTotal;

    private List<InpatientPharmacyIssueDTO> pharmacyIssueDtosToPatientEncounter;
    private double pharmacyIssueDtosToPatientEncounterNetTotal;
    private double pharmacyIssueDtosToPatientEncounterGrossTotal;
    private double pharmacyIssueDtosToPatientEncounterDiscountTotal;
    private double pharmacyIssueDtosToPatientEncounterServiceChargeTotal;

    private List<InpatientPharmacyNetSummaryDTO> pharmacyNetSummaryDtosToPatientEncounter;
    private double pharmacyNetSummaryDtosToPatientEncounterNetTotal;

    private List<InpatientServiceIssueDTO> serviceIssueDtosToPatientEncounter;
    private double serviceIssueDtosToPatientEncounterNetTotal;

    private List<BillListReportDTO> serviceBillDtosToPatientEncounter;
    private double serviceBillDtosToPatientEncounterNetTotal;

    // Issue #22783 (Part B) - encounter-scoped payment bills (deposits +
    // appointment bills) and their cancellations.
    private List<BillListReportDTO> paymentBillDtosToPatientEncounter;
    private double paymentBillDtosToPatientEncounterNetTotal;

    // Issue #22783 (Part C) - department-wide, date-filtered payment bills.
    private List<BillListReportDTO> paymentBillDtosForDepartment;
    private double paymentBillDtosForDepartmentNetTotal;
    private Date fromDate;
    private Date toDate;
    private String bhtNoFilter;
    private String patientNameFilter;

    // Issue #22800 - admission-grouped inpatient professional payment report.
    private List<InwardProfessionalPaymentAdmissionGroupDTO> professionalPaymentReportGroups;

    // Issue #22803 - Summary vs Detailed report type, BHT-range search mode
    // (admission-to-admission instead of a date range), and a filter to show
    // only admissions that never got any professional fee added.
    private String professionalPaymentReportType = "summary"; // "summary" | "detailed"
    private String professionalPaymentSearchMode = "dateRange"; // "dateRange" | "bhtRange"
    private PatientEncounter admissionFromForProfessionalPaymentReport;
    private PatientEncounter admissionToForProfessionalPaymentReport;
    private boolean onlyAdmissionsWithoutProfessionalFees;

    private List<BillItem> labBillItemsToPatientEncounter;
    private double labBillItemsToPatientEncounterNetTotal;

    ReportKeyWord reportKeyWord;

    double opdSrviceGross;
    double opdServiceMargin;
    double opdServiceDiscount;
    private double opdNetTotal;
    double roomGross;
    double roomDiscount;
    double timedGross;
    double timedDiscount;
    double professionalGross;
    double inwardGross;
    double inwardMargin;
    double inwardDiscount;
    double inwardNetValue;
    double total;
    double discount;
    double netTotal;
    double creditPaymentTotalValue;
    double paidbyPatientTotalValue;

    Bill finalBill;
    private Department department;
    private Patient patient;
    private List<Bill> issueBills;

    public String navigateToInpatientPharmacyItemList() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No encounter");
            return null;
        }
        if (configOptionApplicationController.getBooleanValueByKey("Use Legacy Calculation Method for Pharmacy Summary in Inward Patient Profile", true)) {
            List<BillTypeAtomic> btas = new ArrayList<>();
            btas.add(BillTypeAtomic.PHARMACY_DIRECT_ISSUE);
            btas.add(BillTypeAtomic.PHARMACY_DIRECT_ISSUE_CANCELLED);
            btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE);
            btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION);
            btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN);
            btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE);
            btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_CANCELLATION);
            btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_RETURN);
            btas.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD);
            btas.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN);
            btas.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION);
            pharmacyIssueBillItemsToPatientEncounterNetTotal = 0.0;
            pharmacyIssueBillItemsToPatientEncounter = billService.fetchBillItems(null, null, null, null, department, null, btas, patientEncounter);
            if (pharmacyIssueBillItemsToPatientEncounter != null) {
                for (BillItem bi : pharmacyIssueBillItemsToPatientEncounter) {
                    switch (bi.getBill().getBillTypeAtomic()) {
                        case PHARMACY_DIRECT_ISSUE_CANCELLED:
                        case DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION:
                        case DIRECT_ISSUE_INWARD_MEDICINE_RETURN:
                        case DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_CANCELLATION:
                        case DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_RETURN:
                        case ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION:
                        case ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN:
                            pharmacyIssueBillItemsToPatientEncounterNetTotal -= Math.abs(bi.getNetValue());
                            break;
                        case PHARMACY_DIRECT_ISSUE:
                        case DIRECT_ISSUE_INWARD_MEDICINE:
                        case DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE:
                        case ISSUE_MEDICINE_ON_REQUEST_INWARD:
                            pharmacyIssueBillItemsToPatientEncounterNetTotal += Math.abs(bi.getNetValue());
                            break;
                    }

                }
            }
        } else {
            List<BillTypeAtomic> btas = new ArrayList<>();
            btas.add(BillTypeAtomic.PHARMACY_DIRECT_ISSUE);
            btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE);
            btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE);
            List<BillItem> pharmacyIssuedDrugs = billService.fetchBillItems(null, null, null, null, department, null, btas, patientEncounter);

            btas = new ArrayList<>();
            btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN);
            btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_RETURN);
            List<BillItem> pharmacyIssuedReturnedDrugs = billService.fetchBillItems(null, null, null, null, department, null, btas, patientEncounter);

            List<BillItem> pharmacyIssuesWithoutCanceled = new ArrayList<>();

            for (BillItem bi : pharmacyIssuedDrugs) {
                if (!bi.getBill().isCancelled()) {
                    pharmacyIssuesWithoutCanceled.add(bi);
                }
            }

            pharmacyIssueBillItemsToPatientEncounter = new ArrayList<>();
            pharmacyIssueBillItemsToPatientEncounterNetTotal = 0.0;

            for (BillItem bic : pharmacyIssuesWithoutCanceled) {
                for (BillItem bir : pharmacyIssuedReturnedDrugs) {
                    if (bir.getReferanceBillItem() != null && bir.getReferanceBillItem().equals(bic)) {
                        bic.setQty(bic.getQty() - Math.abs(bir.getQty()));
                        bic.setNetValue(bic.getNetValue() - Math.abs(bir.getNetValue()));
                    }
                }
                pharmacyIssueBillItemsToPatientEncounter.add(bic);
                pharmacyIssueBillItemsToPatientEncounterNetTotal += bic.getNetValue();
            }

        }
        //department = null;
        return "/inward/reports/inpatient_pharmacy_item_list?faces-redirect=true";
    }

    public String navigateToInpatientPharmacyItemListDto() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No encounter");
            return null;
        }
        pharmacyIssueDtosToPatientEncounter = new ArrayList<>();
        pharmacyIssueDtosToPatientEncounterNetTotal = 0.0;
        pharmacyIssueDtosToPatientEncounterGrossTotal = 0.0;
        pharmacyIssueDtosToPatientEncounterDiscountTotal = 0.0;
        pharmacyIssueDtosToPatientEncounterServiceChargeTotal = 0.0;
        try {

            // New mode: Include regular issues and returns, but omit cancellations
            List<BillTypeAtomic> pharmacyTypes = new ArrayList<>();
            pharmacyTypes.add(BillTypeAtomic.PHARMACY_DIRECT_ISSUE);
            pharmacyTypes.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE);
            pharmacyTypes.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN);
            pharmacyTypes.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE);
            pharmacyTypes.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_RETURN);
            pharmacyTypes.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD);
            pharmacyTypes.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN);

            List<InpatientPharmacyIssueDTO> allDtos = fetchPharmacyIssueDtos(pharmacyTypes);

            // Use all results
            pharmacyIssueDtosToPatientEncounter.addAll(allDtos);


            // Calculate sum - positive for issues, negative for returns
            for (InpatientPharmacyIssueDTO dto : allDtos) {
                BillTypeAtomic billType = dto.getBillTypeAtomic();
                double netValue = dto.getNetValue() != null ? dto.getNetValue() : 0.0;
                double grossValue = dto.getGrossValue() != null ? dto.getGrossValue() : 0.0;
                double marginValue = dto.getMarginValue() != null ? dto.getMarginValue() : 0.0;
                double discount = dto.getDiscount() != null ? dto.getDiscount() : 0.0;

                boolean isReturn = billType == BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN
                        || billType == BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_RETURN
                        || billType == BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN;

                if (isReturn) {
                    pharmacyIssueDtosToPatientEncounterNetTotal -= Math.abs(netValue);
                    pharmacyIssueDtosToPatientEncounterGrossTotal -= Math.abs(grossValue);
                    pharmacyIssueDtosToPatientEncounterServiceChargeTotal -= Math.abs(marginValue);
                } else {
                    pharmacyIssueDtosToPatientEncounterNetTotal += Math.abs(netValue);
                    pharmacyIssueDtosToPatientEncounterGrossTotal += Math.abs(grossValue);
                    pharmacyIssueDtosToPatientEncounterServiceChargeTotal += Math.abs(marginValue);
                }
                pharmacyIssueDtosToPatientEncounterDiscountTotal += discount;
            }

        } catch (Exception e) {
            Logger.getLogger(InwardReportControllerBht.class.getName()).log(Level.SEVERE, "Error loading pharmacy issue DTOs", e);
            JsfUtil.addErrorMessage("Error loading pharmacy data");
            return null;
        }

        return "/inward/reports/inpatient_pharmacy_item_list_dto?faces-redirect=true";
    }

    public String navigateToPostDischargeReports() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No encounter selected");
            return null;
        }
        if (!Boolean.TRUE.equals(patientEncounter.getDischarged()) || !patientEncounter.isPaymentFinalized()) {
            JsfUtil.addErrorMessage("Post-discharge reports are only available after the admission is discharged and payment finalized");
            return null;
        }
        return "/inward/reports/post_discharge_reports?faces-redirect=true";
    }

    public String navigateToInpatientPharmacyNetSummaryDto() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No encounter");
            return null;
        }
        if (!Boolean.TRUE.equals(patientEncounter.getDischarged()) || !patientEncounter.isPaymentFinalized()) {
            JsfUtil.addErrorMessage("Post-discharge reports are only available after the admission is discharged and payment finalized");
            return null;
        }
        pharmacyNetSummaryDtosToPatientEncounter = new ArrayList<>();
        pharmacyNetSummaryDtosToPatientEncounterNetTotal = 0.0;
        try {
            List<BillTypeAtomic> issueTypes = new ArrayList<>();
            issueTypes.add(BillTypeAtomic.PHARMACY_DIRECT_ISSUE);
            issueTypes.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE);
            issueTypes.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE);
            issueTypes.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD);

            List<BillTypeAtomic> returnTypes = new ArrayList<>();
            returnTypes.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN);
            returnTypes.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_RETURN);
            returnTypes.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN);

            List<BillTypeAtomic> cancellationTypes = new ArrayList<>();
            cancellationTypes.add(BillTypeAtomic.PHARMACY_DIRECT_ISSUE_CANCELLED);
            cancellationTypes.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION);
            cancellationTypes.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_CANCELLATION);
            cancellationTypes.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION);

            pharmacyNetSummaryDtosToPatientEncounter = fetchPharmacyNetSummaryDtos(issueTypes, returnTypes, cancellationTypes);

            for (InpatientPharmacyNetSummaryDTO dto : pharmacyNetSummaryDtosToPatientEncounter) {
                pharmacyNetSummaryDtosToPatientEncounterNetTotal += dto.getNetValue() != null ? dto.getNetValue() : 0.0;
            }

        } catch (Exception e) {
            Logger.getLogger(InwardReportControllerBht.class.getName()).log(Level.SEVERE, "Error loading pharmacy net summary DTOs", e);
            JsfUtil.addErrorMessage("Error loading pharmacy data");
            return null;
        }

        return "/inward/reports/inpatient_pharmacy_net_summary_dto?faces-redirect=true";
    }

    public String navigateToInpatientServiceItemListDto() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No encounter");
            return null;
        }
        serviceIssueDtosToPatientEncounter = new ArrayList<>();
        serviceIssueDtosToPatientEncounterNetTotal = 0.0;
        try {
            List<BillTypeAtomic> serviceTypes = new ArrayList<>();
            serviceTypes.add(BillTypeAtomic.INWARD_SERVICE_BILL);
            serviceTypes.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
            serviceTypes.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
            serviceTypes.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION);
            serviceTypes.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
            serviceTypes.add(BillTypeAtomic.INWARD_OUTSIDE_CHARGES_BILL);
            serviceTypes.add(BillTypeAtomic.INWARD_OUTSIDE_CHARGES_BILL_CANCELLATION);

            serviceIssueDtosToPatientEncounter = fetchOnlyIPServiceIssueDtos(serviceTypes);

            for (InpatientServiceIssueDTO dto : serviceIssueDtosToPatientEncounter) {
                double netValue = dto.getNetValue() != null ? dto.getNetValue() : 0.0;
                if (Boolean.TRUE.equals(dto.getCancellation())) {
                    serviceIssueDtosToPatientEncounterNetTotal -= Math.abs(netValue);
                } else {
                    serviceIssueDtosToPatientEncounterNetTotal += Math.abs(netValue);
                }
            }
        } catch (Exception e) {
            Logger.getLogger(InwardReportControllerBht.class.getName()).log(Level.SEVERE, "Error loading service issue DTOs", e);
            JsfUtil.addErrorMessage("Error loading service data");
            return null;
        }
        return "/inward/reports/inpatient_service_item_list_dto?faces-redirect=true";
    }

    public String navigateToInpatientPharmacyAndServiceItemListDto() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No encounter");
            return null;
        }
        // Pharmacy part
        pharmacyIssueDtosToPatientEncounter = new ArrayList<>();
        pharmacyIssueDtosToPatientEncounterNetTotal = 0.0;
        try {
            List<BillTypeAtomic> pharmacyTypes = new ArrayList<>();
            pharmacyTypes.add(BillTypeAtomic.PHARMACY_DIRECT_ISSUE);
            pharmacyTypes.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE);
            pharmacyTypes.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN);
            pharmacyTypes.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE);
            pharmacyTypes.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_RETURN);
            pharmacyTypes.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD);
            pharmacyTypes.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN);
            List<InpatientPharmacyIssueDTO> allDtos = fetchPharmacyIssueDtos(pharmacyTypes);
            pharmacyIssueDtosToPatientEncounter.addAll(allDtos);
            for (InpatientPharmacyIssueDTO dto : allDtos) {
                BillTypeAtomic billType = dto.getBillTypeAtomic();
                double netValue = dto.getNetValue() != null ? dto.getNetValue() : 0.0;
                if (billType == BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN
                        || billType == BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_RETURN
                        || billType == BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN) {
                    pharmacyIssueDtosToPatientEncounterNetTotal -= Math.abs(netValue);
                } else {
                    pharmacyIssueDtosToPatientEncounterNetTotal += Math.abs(netValue);
                }
            }
        } catch (Exception e) {
            Logger.getLogger(InwardReportControllerBht.class.getName()).log(Level.SEVERE, "Error loading pharmacy issue DTOs for combined report", e);
            JsfUtil.addErrorMessage("Error loading pharmacy data");
            return null;
        }
        // Service part
        serviceIssueDtosToPatientEncounter = new ArrayList<>();
        serviceIssueDtosToPatientEncounterNetTotal = 0.0;
        try {
            List<BillTypeAtomic> serviceTypes = new ArrayList<>();
            serviceTypes.add(BillTypeAtomic.INWARD_SERVICE_BILL);
            serviceTypes.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
            serviceTypes.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
            serviceTypes.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION);
            serviceTypes.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
            serviceTypes.add(BillTypeAtomic.INWARD_OUTSIDE_CHARGES_BILL);
            serviceTypes.add(BillTypeAtomic.INWARD_OUTSIDE_CHARGES_BILL_CANCELLATION);
            serviceIssueDtosToPatientEncounter = fetchServiceIssueDtos(serviceTypes);
            for (InpatientServiceIssueDTO dto : serviceIssueDtosToPatientEncounter) {
                double netValue = dto.getNetValue() != null ? dto.getNetValue() : 0.0;
                if (Boolean.TRUE.equals(dto.getCancellation())) {
                    serviceIssueDtosToPatientEncounterNetTotal -= Math.abs(netValue);
                } else {
                    serviceIssueDtosToPatientEncounterNetTotal += Math.abs(netValue);
                }
            }
        } catch (Exception e) {
            Logger.getLogger(InwardReportControllerBht.class.getName()).log(Level.SEVERE, "Error loading service issue DTOs for combined report", e);
            JsfUtil.addErrorMessage("Error loading service data");
            return null;
        }
        return "/inward/reports/inpatient_pharmacy_and_service_item_list_dto?faces-redirect=true";
    }

    private List<InpatientServiceIssueDTO> fetchServiceIssueDtos(List<BillTypeAtomic> billTypes) {
        String jpql = "SELECT new com.divudi.core.data.dto.InpatientServiceIssueDTO("
                + "bi.id, "
                + "CASE WHEN bi.item.printName IS NULL OR bi.item.printName = '' "
                + "THEN CONCAT('Printing name is missing in ', bi.item.name) ELSE bi.item.printName END, "
                + "bi.qty, "
                + "bi.netValue, "
                + "bi.bill.createdAt, "
                + "bi.bill.billTypeAtomic, "
                + "COALESCE(bi.bill.department.name, 'N/A'), "
                + "bi.bill.cancelled) "
                + "FROM BillItem bi "
                + "WHERE bi.bill.patientEncounter = :patientEncounter "
                + "AND bi.bill.billTypeAtomic IN :billTypeAtomics "
                + "AND bi.retired = FALSE "
                + "AND bi.bill.retired = FALSE ";

        Map<String, Object> params = new HashMap<>();
        params.put("billTypeAtomics", billTypes);
        params.put("patientEncounter", patientEncounter);

        if (department != null) {
            jpql += "AND bi.bill.department = :department ";
            params.put("department", department);
        }

        jpql += "ORDER BY bi.bill.createdAt, bi.id";

        List<InpatientServiceIssueDTO> result = (List<InpatientServiceIssueDTO>) billItemFacade.findLightsByJpql(jpql, params);
        return result != null ? result : new ArrayList<>();
    }

    
    private List<InpatientServiceIssueDTO> fetchOnlyIPServiceIssueDtos(List<BillTypeAtomic> billTypes) {
        String jpql = "SELECT new com.divudi.core.data.dto.InpatientServiceIssueDTO("
                + "bi.id, "
                + "CASE WHEN bi.item.printName IS NULL OR bi.item.printName = '' "
                + "THEN CONCAT('Printing name is missing in ', bi.item.name) ELSE bi.item.printName END, "
                + "bi.qty, "
                + "bi.netValue, "
                + "bi.bill.createdAt, "
                + "bi.bill.billTypeAtomic, "
                + "COALESCE(bi.bill.department.name, 'N/A'), "
                + "bi.bill.cancelled) "
                + "FROM BillItem bi "
                + "WHERE bi.bill.patientEncounter = :patientEncounter "
                + "AND type(bi.item)=:btp "
                + "AND bi.bill.billTypeAtomic IN :billTypeAtomics "
                + "AND bi.retired = FALSE "
                + "AND bi.bill.retired = FALSE ";

        Map<String, Object> params = new HashMap<>();
        params.put("billTypeAtomics", billTypes);
        params.put("patientEncounter", patientEncounter);
        params.put("btp", Service.class);

        if (department != null) {
            jpql += "AND bi.bill.department = :department ";
            params.put("department", department);
        }

        jpql += "ORDER BY bi.bill.createdAt, bi.id";

        List<InpatientServiceIssueDTO> result = (List<InpatientServiceIssueDTO>) billItemFacade.findLightsByJpql(jpql, params);
        return result != null ? result : new ArrayList<>();
    }

    // Issue #21247 - Encounter-scoped list of inward service bills with a View
    // button that navigates to the existing reprint page (no whole-batch
    // cancellation needed; partial return is done from the reprint page).
    public String navigateToInpatientServiceBillListDto() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No encounter");
            return null;
        }
        serviceBillDtosToPatientEncounter = new ArrayList<>();
        serviceBillDtosToPatientEncounterNetTotal = 0.0;
        try {
            List<BillTypeAtomic> serviceTypes = new ArrayList<>();
            serviceTypes.add(BillTypeAtomic.INWARD_SERVICE_BILL);
            serviceTypes.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
            serviceTypes.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
            serviceTypes.add(BillTypeAtomic.INWARD_SERVICE_BILL_REFUND);
            serviceTypes.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_REFUND); // deprecated, for old refund bills
            serviceTypes.add(BillTypeAtomic.INWARD_OUTSIDE_CHARGES_BILL);
            serviceTypes.add(BillTypeAtomic.INWARD_OUTSIDE_CHARGES_BILL_CANCELLATION);

            serviceBillDtosToPatientEncounter = fetchServiceBillDtos(serviceTypes);

            for (BillListReportDTO dto : serviceBillDtosToPatientEncounter) {
                double netValue = dto.getNetTotal() != null ? dto.getNetTotal().doubleValue() : 0.0;
                serviceBillDtosToPatientEncounterNetTotal += netValue;
            }
        } catch (Exception e) {
            Logger.getLogger(InwardReportControllerBht.class.getName()).log(Level.SEVERE, "Error loading service bill DTOs", e);
            JsfUtil.addErrorMessage("Error loading service bill data");
            return null;
        }
        return "/inward/reports/inpatient_service_bill_list_dto?faces-redirect=true";
    }

    private List<BillListReportDTO> fetchServiceBillDtos(List<BillTypeAtomic> billTypes) {
        // Bill.retired/cancelled/refunded are primitive boolean and
        // total/discount/netTotal/margin are primitive double, so they are
        // never null - wrapping them in COALESCE makes EclipseLink return a
        // mismatched type (e.g. Integer for a boolean) that breaks the
        // reflective DTO-constructor binding. Project them directly; keep
        // COALESCE only for the nullable String/relationship fields.
        String jpql = "SELECT new com.divudi.core.data.dto.BillListReportDTO("
                + "b.id, "
                + "COALESCE(b.deptId, ''), "
                + "b.billTypeAtomic, "
                + "b.paymentMethod, "
                + "COALESCE(b.patientEncounter.patient.person.name, ''), "
                + "b.createdAt, "
                + "COALESCE(b.creater.name, ''), "
                + "b.retired, "
                + "b.cancelled, "
                + "b.refunded, "
                + "b.total, "
                + "b.discount, "
                + "b.netTotal, "
                + "COALESCE(b.patientEncounter.bhtNo, ''), "
                + "COALESCE(b.deptId, ''), "
                + "b.margin) "
                + "FROM Bill b "
                + "WHERE b.patientEncounter = :patientEncounter "
                + "AND b.billTypeAtomic IN :billTypeAtomics "
                + "AND b.retired = FALSE ";

        jpql += "ORDER BY b.createdAt, b.id";

        Map<String, Object> params = new HashMap<>();
        params.put("billTypeAtomics", billTypes);
        params.put("patientEncounter", patientEncounter);

        List<BillListReportDTO> result = (List<BillListReportDTO>) billFacade.findLightsByJpqlWithoutCache(jpql, params, javax.persistence.TemporalType.TIMESTAMP);
        return result != null ? result : new ArrayList<>();
    }

    // Issue #22783 (Part B) - unlike deposit bills, INWARD_APPOINTMENT_BILL /
    // INWARD_APPOINTMENT_CANCEL_BILL never populate Bill.patientEncounter
    // (only Appointment.patientEncounter is set, at admission time - see
    // BillFacade.findInwardBillReceiptDTO's javadoc, discovered while
    // building Part A). A plain "b.patientEncounter = :patientEncounter"
    // filter - which fetchServiceBillDtos above uses - silently excludes
    // both the appointment bill and its cancellation. This method unions
    // the two patterns in one query: deposit-family bills matched via
    // Bill.patientEncounter directly, appointment-family bills matched via
    // Appointment.patientEncounter (through Appointment.bill, and through
    // Bill.referenceBill for the cancel bill, which points back at the
    // original appointment bill). All patientEncounter/patient/creater
    // navigation uses explicit LEFT JOINs rather than dot-path expressions,
    // for the same reason: an inner-join dot-path would drop the
    // null-patientEncounter appointment rows from the SELECT list too.
    private List<BillListReportDTO> fetchPaymentBillDtosForEncounter(List<BillTypeAtomic> depositTypes, List<BillTypeAtomic> appointmentTypes) {
        String jpql = "SELECT new com.divudi.core.data.dto.BillListReportDTO("
                + "b.id, "
                + "COALESCE(b.deptId, ''), "
                + "b.billTypeAtomic, "
                + "b.paymentMethod, "
                + "COALESCE(per.name, per2.name, ''), "
                + "b.createdAt, "
                + "COALESCE(cr.name, ''), "
                + "b.retired, "
                + "b.cancelled, "
                + "b.refunded, "
                + "b.total, "
                + "b.discount, "
                + "b.netTotal, "
                + "COALESCE(pe.bhtNo, ''), "
                + "COALESCE(b.deptId, ''), "
                + "b.margin) "
                + "FROM Bill b "
                + "LEFT JOIN b.patientEncounter pe "
                + "LEFT JOIN pe.patient pt "
                + "LEFT JOIN pt.person per "
                + "LEFT JOIN b.patient pt2 "
                + "LEFT JOIN pt2.person per2 "
                + "LEFT JOIN b.creater cr "
                + "WHERE b.retired = FALSE "
                + "AND ("
                + "  (b.billTypeAtomic IN :depositTypes AND pe = :patientEncounter) "
                + "  OR "
                + "  (b.billTypeAtomic IN :appointmentTypes AND ("
                + "     b IN (SELECT a.bill FROM Appointment a WHERE a.patientEncounter = :patientEncounter) "
                + "     OR b.referenceBill IN (SELECT a.bill FROM Appointment a WHERE a.patientEncounter = :patientEncounter)"
                + "  ))"
                + ") "
                + "ORDER BY b.createdAt, b.id";

        Map<String, Object> params = new HashMap<>();
        params.put("depositTypes", depositTypes);
        params.put("appointmentTypes", appointmentTypes);
        params.put("patientEncounter", patientEncounter);

        List<BillListReportDTO> result = (List<BillListReportDTO>) billFacade.findLightsByJpqlWithoutCache(jpql, params, javax.persistence.TemporalType.TIMESTAMP);
        return result != null ? result : new ArrayList<>();
    }

    // Issue #22783 (Part B) - Encounter-scoped list of inward payment bills
    // (deposits + appointment bills) together with their cancellations/refunds.
    // Row-level View/Reprint action is done from the XHTML by calling the
    // existing BillSearch#navigateToViewBillByAtomicBillTypeByBillId(billId)
    // directly, same pattern as the service bill list (#21247).
    public String navigateToInpatientPaymentBillListDto() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No encounter");
            return null;
        }
        paymentBillDtosToPatientEncounter = new ArrayList<>();
        paymentBillDtosToPatientEncounterNetTotal = 0.0;
        try {
            List<BillTypeAtomic> depositTypes = new ArrayList<>();
            depositTypes.add(BillTypeAtomic.INWARD_DEPOSIT);
            depositTypes.add(BillTypeAtomic.INWARD_DEPOSIT_CANCELLATION);
            depositTypes.add(BillTypeAtomic.INWARD_DEPOSIT_REFUND);
            depositTypes.add(BillTypeAtomic.INWARD_DEPOSIT_REFUND_CANCELLATION);

            List<BillTypeAtomic> appointmentTypes = new ArrayList<>();
            appointmentTypes.add(BillTypeAtomic.INWARD_APPOINTMENT_BILL);
            appointmentTypes.add(BillTypeAtomic.INWARD_APPOINTMENT_CANCEL_BILL);

            paymentBillDtosToPatientEncounter = fetchPaymentBillDtosForEncounter(depositTypes, appointmentTypes);

            for (BillListReportDTO dto : paymentBillDtosToPatientEncounter) {
                double netValue = dto.getNetTotal() != null ? dto.getNetTotal().doubleValue() : 0.0;
                paymentBillDtosToPatientEncounterNetTotal += netValue;
            }
        } catch (Exception e) {
            Logger.getLogger(InwardReportControllerBht.class.getName()).log(Level.SEVERE, "Error loading payment bill DTOs", e);
            JsfUtil.addErrorMessage("Error loading payment bill data");
            return null;
        }
        return "/inward/reports/inpatient_payment_bill_list_dto?faces-redirect=true";
    }

    // Issue #22783 (Part C) - Department-wide, date-filtered list of inward
    // payment bills (deposits + appointment bills) together with their
    // cancellations/refunds, with optional BHT No / patient name filters.
    public String navigateToInwardPaymentBillListForDepartment() {
        if (department == null) {
            department = sessionController.getDepartment();
        }
        if (department == null) {
            JsfUtil.addErrorMessage("No department");
            return null;
        }
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        if (toDate == null) {
            toDate = new Date();
        }
        paymentBillDtosForDepartment = new ArrayList<>();
        paymentBillDtosForDepartmentNetTotal = 0.0;
        try {
            List<BillTypeAtomic> paymentTypes = new ArrayList<>();
            paymentTypes.add(BillTypeAtomic.INWARD_APPOINTMENT_BILL);
            paymentTypes.add(BillTypeAtomic.INWARD_APPOINTMENT_CANCEL_BILL);
            paymentTypes.add(BillTypeAtomic.INWARD_DEPOSIT);
            paymentTypes.add(BillTypeAtomic.INWARD_DEPOSIT_CANCELLATION);
            paymentTypes.add(BillTypeAtomic.INWARD_DEPOSIT_REFUND);
            paymentTypes.add(BillTypeAtomic.INWARD_DEPOSIT_REFUND_CANCELLATION);

            paymentBillDtosForDepartment = fetchPaymentBillDtosForDepartment(paymentTypes);

            for (BillListReportDTO dto : paymentBillDtosForDepartment) {
                double netValue = dto.getNetTotal() != null ? dto.getNetTotal().doubleValue() : 0.0;
                paymentBillDtosForDepartmentNetTotal += netValue;
            }
        } catch (Exception e) {
            Logger.getLogger(InwardReportControllerBht.class.getName()).log(Level.SEVERE, "Error loading department payment bill DTOs", e);
            JsfUtil.addErrorMessage("Error loading payment bill data");
            return null;
        }
        return "/inward/reports/inward_payment_bill_list_department_dto?faces-redirect=true";
    }

    private List<BillListReportDTO> fetchPaymentBillDtosForDepartment(List<BillTypeAtomic> billTypes) {
        // Explicit LEFT JOINs throughout (not dot-path navigation) for the
        // same reason as fetchPaymentBillDtosForEncounter above: implicit
        // path navigation through a null Bill.patientEncounter (true for
        // INWARD_APPOINTMENT_BILL / INWARD_APPOINTMENT_CANCEL_BILL) compiles
        // to an INNER JOIN and silently drops those rows from the whole
        // result set, not just the projected column. b.patient (set
        // directly on appointment bills) is the fallback patient path.
        String jpql = "SELECT new com.divudi.core.data.dto.BillListReportDTO("
                + "b.id, "
                + "COALESCE(b.deptId, ''), "
                + "b.billTypeAtomic, "
                + "b.paymentMethod, "
                + "COALESCE(per.name, per2.name, ''), "
                + "b.createdAt, "
                + "COALESCE(cr.name, ''), "
                + "b.retired, "
                + "b.cancelled, "
                + "b.refunded, "
                + "b.total, "
                + "b.discount, "
                + "b.netTotal, "
                + "COALESCE(pe.bhtNo, ''), "
                + "COALESCE(b.deptId, ''), "
                + "b.margin) "
                + "FROM Bill b "
                + "LEFT JOIN b.patientEncounter pe "
                + "LEFT JOIN pe.patient pt "
                + "LEFT JOIN pt.person per "
                + "LEFT JOIN b.patient pt2 "
                + "LEFT JOIN pt2.person per2 "
                + "LEFT JOIN b.creater cr "
                + "WHERE b.department = :department "
                + "AND b.billTypeAtomic IN :billTypeAtomics "
                + "AND b.retired = FALSE "
                + "AND b.createdAt BETWEEN :fromDate AND :toDate ";

        Map<String, Object> params = new HashMap<>();
        params.put("billTypeAtomics", billTypes);
        params.put("department", department);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        if (bhtNoFilter != null && !bhtNoFilter.trim().isEmpty()) {
            // pe.bhtNo alone misses INWARD_APPOINTMENT_BILL / INWARD_APPOINTMENT_CANCEL_BILL
            // bills (their Bill.patientEncounter is always null) - fall back to the
            // admission's BHT No via the Appointment that references this bill either
            // as its original bill or its cancellation bill.
            jpql += "AND (pe.bhtNo LIKE :bhtNo OR EXISTS ("
                    + "SELECT a FROM Appointment a "
                    + "WHERE (a.bill = b OR a.appointmentCancelBill = b) "
                    + "AND a.patientEncounter.bhtNo LIKE :bhtNo)) ";
            params.put("bhtNo", "%" + bhtNoFilter.trim() + "%");
        }

        if (patientNameFilter != null && !patientNameFilter.trim().isEmpty()) {
            jpql += "AND (per.name LIKE :patientName OR per2.name LIKE :patientName) ";
            params.put("patientName", "%" + patientNameFilter.trim() + "%");
        }

        jpql += "ORDER BY b.createdAt, b.id";

        List<BillListReportDTO> result = (List<BillListReportDTO>) billFacade.findLightsByJpqlWithoutCache(jpql, params, javax.persistence.TemporalType.TIMESTAMP);
        return result != null ? result : new ArrayList<>();
    }

    // Issue #22800 - department-wide, date-filtered Inpatient Professional
    // Payment Report, grouped by admission with one row per doctor+speciality.
    public String navigateToInwardProfessionalPaymentReport() {
        if (department == null) {
            department = sessionController.getDepartment();
        }
        if (department == null) {
            JsfUtil.addErrorMessage("No department");
            return null;
        }
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        if (toDate == null) {
            toDate = new Date();
        }
        // Issue #22805 review - without this guard, a BHT Range search left
        // with either endpoint unselected silently falls back to the date
        // range query below (dead giveaway: the filter panel still says "BHT
        // Range" but the results are date-range results).
        if ("bhtRange".equals(professionalPaymentSearchMode)
                && (admissionFromForProfessionalPaymentReport == null
                || admissionToForProfessionalPaymentReport == null)) {
            JsfUtil.addErrorMessage("Select both From BHT and To BHT for a BHT Range search");
            return null;
        }
        professionalPaymentReportGroups = new ArrayList<>();
        try {
            professionalPaymentReportGroups = fetchInwardProfessionalPaymentReportGroups();
        } catch (Exception e) {
            Logger.getLogger(InwardReportControllerBht.class.getName()).log(Level.SEVERE, "Error loading inpatient professional payment report", e);
            JsfUtil.addErrorMessage("Error loading professional payment report data");
            return null;
        }
        return "/inward/reports/inward_professional_payment_report_dto?faces-redirect=true";
    }

    // Issue #22803 - row-span helpers shared by the Excel and PDF exporters,
    // delegating to the same DTO getters the xhtml view binds `rowspan` to
    // (InwardProfessionalPaymentAdmissionGroupDTO.getSummaryAdmissionRowSpan/
    // getDetailedAdmissionRowSpan, InwardProfessionalPaymentDetailRowDTO.
    // getBlockRowSpan) so the export layout can never drift from the on-screen one.
    private int summaryAdmissionRowSpan(InwardProfessionalPaymentAdmissionGroupDTO group) {
        return group.getSummaryAdmissionRowSpan();
    }

    private int detailedConsultantBlockRowSpan(InwardProfessionalPaymentDetailRowDTO detail) {
        return detail.getBlockRowSpan();
    }

    private int detailedAdmissionRowSpan(InwardProfessionalPaymentAdmissionGroupDTO group) {
        return group.getDetailedAdmissionRowSpan();
    }

    // Issue #22803 - Excel export for the Summary report.
    public void downloadProfessionalPaymentSummaryExcel() {
        if (professionalPaymentReportGroups == null || professionalPaymentReportGroups.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please process the report first.");
            return;
        }

        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Professional Payment Summary");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            DataFormat dataFormat = workbook.createDataFormat();
            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));

            String[] headers = {"BHT Number", "Admitted", "Discharged", "Final Bill Number",
                "Consultant", "Speciality", "Sum Added Fee", "Sum Paid Fee", "Balance to Pay"};
            Row headerRow = sheet.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(headers[c]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (InwardProfessionalPaymentAdmissionGroupDTO group : professionalPaymentReportGroups) {
                int span = summaryAdmissionRowSpan(group);
                int admissionStartRow = rowIdx;
                List<InwardProfessionalPaymentReportRowDTO> detailRows = group.getDetailRows();

                for (int i = 0; i < span; i++) {
                    Row row = sheet.createRow(rowIdx++);
                    if (i == 0) {
                        row.createCell(0).setCellValue(group.getBhtNo() != null ? group.getBhtNo() : "");
                        row.createCell(1).setCellValue(group.getDateOfAdmission() != null ? dateFormat.format(group.getDateOfAdmission()) : "");
                        row.createCell(2).setCellValue(group.getDateOfDischarge() != null ? dateFormat.format(group.getDateOfDischarge()) : "");
                        row.createCell(3).setCellValue(group.getFirstFinalBillNo() != null ? group.getFirstFinalBillNo() : "");
                    }
                    if (detailRows != null && i < detailRows.size()) {
                        InwardProfessionalPaymentReportRowDTO detail = detailRows.get(i);
                        row.createCell(4).setCellValue(detail.getConsultantName() != null ? detail.getConsultantName() : "");
                        row.createCell(5).setCellValue(detail.getSpecialityName() != null ? detail.getSpecialityName() : "");
                        Cell addedCell = row.createCell(6);
                        addedCell.setCellValue(detail.getSumAddedFee());
                        addedCell.setCellStyle(moneyStyle);
                        Cell paidCell = row.createCell(7);
                        paidCell.setCellValue(detail.getSumPaidFee());
                        paidCell.setCellStyle(moneyStyle);
                        Cell balanceCell = row.createCell(8);
                        balanceCell.setCellValue(detail.getSumAddedFee() - detail.getSumPaidFee());
                        balanceCell.setCellStyle(moneyStyle);
                    }
                }

                if (span > 1) {
                    for (int c = 0; c <= 3; c++) {
                        sheet.addMergedRegion(new CellRangeAddress(admissionStartRow, rowIdx - 1, c, c));
                    }
                }
            }

            for (int c = 0; c < headers.length; c++) {
                sheet.autoSizeColumn(c);
            }

            // Issue #22805 review - serialize into memory first so a POI
            // failure can't leave a truncated file on a half-committed
            // response (matches the PDF exporters' existing pattern below).
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            byte[] bytes = baos.toByteArray();

            String filename = "Inpatient_Professional_Payment_Summary_"
                    + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".xlsx";
            response.reset();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            response.setContentLength(bytes.length);
            try (OutputStream out = response.getOutputStream()) {
                out.write(bytes);
            }
            facesContext.responseComplete();
        } catch (Exception e) {
            Logger.getLogger(InwardReportControllerBht.class.getName()).log(Level.SEVERE, "Error exporting professional payment summary to Excel", e);
            JsfUtil.addErrorMessage("Failed to generate Excel: " + e.getMessage());
        }
    }

    // Issue #22803 - Excel export for the Detailed report.
    public void downloadProfessionalPaymentDetailedExcel() {
        if (professionalPaymentReportGroups == null || professionalPaymentReportGroups.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please process the report first.");
            return;
        }

        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Professional Payment Detailed");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            DataFormat poiDataFormat = workbook.createDataFormat();
            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(poiDataFormat.getFormat("#,##0.00"));

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            CellStyle boldStyle = workbook.createCellStyle();
            boldStyle.setFont(boldFont);
            CellStyle boldMoneyStyle = workbook.createCellStyle();
            boldMoneyStyle.setFont(boldFont);
            boldMoneyStyle.setDataFormat(poiDataFormat.getFormat("#,##0.00"));

            String[] headers = {"BHT Number", "Admitted", "Discharged", "Final Bill Number",
                "Consultant", "Speciality", "Added Fee Date", "Added Fee Value", "Paid Date",
                "Paid Bill Number", "Paid Fee Value"};
            Row headerRow = sheet.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(headers[c]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (InwardProfessionalPaymentAdmissionGroupDTO group : professionalPaymentReportGroups) {
                int admissionStartRow = rowIdx;
                List<InwardProfessionalPaymentDetailRowDTO> detailedRows = group.getDetailedRows();
                boolean admissionInfoWritten = false;

                if (detailedRows == null || detailedRows.isEmpty()) {
                    Row row = sheet.createRow(rowIdx++);
                    writeDetailedAdmissionCells(row, group, dateFormat);
                    admissionInfoWritten = true;
                } else {
                    for (InwardProfessionalPaymentDetailRowDTO detail : detailedRows) {
                        int consultantStartRow = rowIdx;
                        int dataRows = Math.max(detail.rowCount(), 1);

                        for (int i = 0; i < dataRows; i++) {
                            Row row = sheet.createRow(rowIdx++);
                            if (!admissionInfoWritten) {
                                writeDetailedAdmissionCells(row, group, dateFormat);
                                admissionInfoWritten = true;
                            }
                            if (i == 0) {
                                row.createCell(4).setCellValue(detail.getConsultantName() != null ? detail.getConsultantName() : "");
                                row.createCell(5).setCellValue(detail.getSpecialityName() != null ? detail.getSpecialityName() : "");
                            }
                            if (i < detail.getAddedFeeValues().size()) {
                                Date addedDate = detail.getAddedFeeDates().get(i);
                                row.createCell(6).setCellValue(addedDate != null ? dateFormat.format(addedDate) : "");
                                Double addedValue = detail.getAddedFeeValues().get(i);
                                Cell addedCell = row.createCell(7);
                                addedCell.setCellValue(addedValue != null ? addedValue : 0.0);
                                addedCell.setCellStyle(moneyStyle);
                            }
                            if (i < detail.getPaidFeeValues().size()) {
                                Date paidDate = detail.getPaidFeeDates().get(i);
                                row.createCell(8).setCellValue(paidDate != null ? dateFormat.format(paidDate) : "");
                                String paidBillNo = detail.getPaidBillNumbers().get(i);
                                row.createCell(9).setCellValue(paidBillNo != null ? paidBillNo : "");
                                Double paidValue = detail.getPaidFeeValues().get(i);
                                Cell paidCell = row.createCell(10);
                                paidCell.setCellValue(paidValue != null ? paidValue : 0.0);
                                paidCell.setCellStyle(moneyStyle);
                            }
                        }

                        Row totalRow = sheet.createRow(rowIdx++);
                        Cell totalLabelCell = totalRow.createCell(6);
                        totalLabelCell.setCellValue("Total");
                        totalLabelCell.setCellStyle(boldStyle);
                        Cell totalAddedCell = totalRow.createCell(7);
                        totalAddedCell.setCellValue(detail.getSumAddedFee());
                        totalAddedCell.setCellStyle(boldMoneyStyle);
                        Cell totalPaidCell = totalRow.createCell(10);
                        totalPaidCell.setCellValue(detail.getSumPaidFee());
                        totalPaidCell.setCellStyle(boldMoneyStyle);

                        Row balanceRow = sheet.createRow(rowIdx++);
                        Cell balanceLabelCell = balanceRow.createCell(6);
                        balanceLabelCell.setCellValue("Balance to Pay");
                        balanceLabelCell.setCellStyle(boldStyle);
                        Cell balanceValueCell = balanceRow.createCell(7);
                        balanceValueCell.setCellValue(detail.getSumAddedFee() - detail.getSumPaidFee());
                        balanceValueCell.setCellStyle(boldMoneyStyle);

                        if (rowIdx - 1 > consultantStartRow) {
                            sheet.addMergedRegion(new CellRangeAddress(consultantStartRow, rowIdx - 1, 4, 4));
                            sheet.addMergedRegion(new CellRangeAddress(consultantStartRow, rowIdx - 1, 5, 5));
                        }
                    }
                }

                if (rowIdx - 1 > admissionStartRow) {
                    for (int c = 0; c <= 3; c++) {
                        sheet.addMergedRegion(new CellRangeAddress(admissionStartRow, rowIdx - 1, c, c));
                    }
                }
            }

            for (int c = 0; c < headers.length; c++) {
                sheet.autoSizeColumn(c);
            }

            // Issue #22805 review - same buffer-first pattern as the Summary
            // exporter above.
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            byte[] bytes = baos.toByteArray();

            String filename = "Inpatient_Professional_Payment_Detailed_"
                    + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".xlsx";
            response.reset();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            response.setContentLength(bytes.length);
            try (OutputStream out = response.getOutputStream()) {
                out.write(bytes);
            }
            facesContext.responseComplete();
        } catch (Exception e) {
            Logger.getLogger(InwardReportControllerBht.class.getName()).log(Level.SEVERE, "Error exporting professional payment detailed report to Excel", e);
            JsfUtil.addErrorMessage("Failed to generate Excel: " + e.getMessage());
        }
    }

    private void writeDetailedAdmissionCells(Row row, InwardProfessionalPaymentAdmissionGroupDTO group, SimpleDateFormat dateFormat) {
        row.createCell(0).setCellValue(group.getBhtNo() != null ? group.getBhtNo() : "");
        row.createCell(1).setCellValue(group.getDateOfAdmission() != null ? dateFormat.format(group.getDateOfAdmission()) : "");
        row.createCell(2).setCellValue(group.getDateOfDischarge() != null ? dateFormat.format(group.getDateOfDischarge()) : "");
        row.createCell(3).setCellValue(group.getFirstFinalBillNo() != null ? group.getFirstFinalBillNo() : "");
    }

    // Issue #22803 - PDF export for the Summary report. Mirrors
    // InwardReportController.downloadSurgeryCostEstimationPdf()'s structure
    // (OpenPDF PdfPTable/PdfPCell, direct HttpServletResponse write via
    // ExternalContext).
    public void downloadProfessionalPaymentSummaryPdf() {
        if (professionalPaymentReportGroups == null || professionalPaymentReportGroups.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please process the report first.");
            return;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();

            String fileName = "Inpatient_Professional_Payment_Summary_"
                    + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".pdf";

            com.lowagie.text.Document document = new com.lowagie.text.Document(
                    com.lowagie.text.PageSize.A4.rotate(), 15, 15, 30, 20);
            com.lowagie.text.pdf.PdfWriter.getInstance(document, baos);
            document.open();

            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            com.lowagie.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            com.lowagie.text.Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            DecimalFormat df = new DecimalFormat("#,##0.00");

            com.lowagie.text.Paragraph titlePara = new com.lowagie.text.Paragraph("Inpatient Professional Payment Report - Summary", titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(10);
            document.add(titlePara);

            String[] headers = {"BHT Number", "Admitted", "Discharged", "Final Bill Number",
                "Consultant", "Speciality", "Sum Added Fee", "Sum Paid Fee", "Balance to Pay"};
            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            float[] widths = {3f, 3f, 3f, 3f, 4f, 4f, 3f, 3f, 3f};
            table.setWidths(widths);

            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            double grandAdded = 0;
            double grandPaid = 0;

            for (InwardProfessionalPaymentAdmissionGroupDTO group : professionalPaymentReportGroups) {
                int span = summaryAdmissionRowSpan(group);
                List<InwardProfessionalPaymentReportRowDTO> detailRows = group.getDetailRows();

                PdfPCell bhtCell = new PdfPCell(new Phrase(group.getBhtNo() != null ? group.getBhtNo() : "", normalFont));
                bhtCell.setRowspan(span);
                table.addCell(bhtCell);

                PdfPCell admittedCell = new PdfPCell(new Phrase(group.getDateOfAdmission() != null ? sdf.format(group.getDateOfAdmission()) : "", normalFont));
                admittedCell.setRowspan(span);
                table.addCell(admittedCell);

                PdfPCell dischargedCell = new PdfPCell(new Phrase(group.getDateOfDischarge() != null ? sdf.format(group.getDateOfDischarge()) : "", normalFont));
                dischargedCell.setRowspan(span);
                table.addCell(dischargedCell);

                PdfPCell finalBillCell = new PdfPCell(new Phrase(group.getFirstFinalBillNo() != null ? group.getFirstFinalBillNo() : "", normalFont));
                finalBillCell.setRowspan(span);
                table.addCell(finalBillCell);

                if (detailRows == null || detailRows.isEmpty()) {
                    table.addCell(new Phrase("", normalFont));
                    table.addCell(new Phrase("", normalFont));
                    table.addCell(new Phrase("", normalFont));
                    table.addCell(new Phrase("", normalFont));
                    table.addCell(new Phrase("", normalFont));
                } else {
                    for (InwardProfessionalPaymentReportRowDTO detail : detailRows) {
                        table.addCell(new Phrase(detail.getConsultantName() != null ? detail.getConsultantName() : "", normalFont));
                        table.addCell(new Phrase(detail.getSpecialityName() != null ? detail.getSpecialityName() : "", normalFont));

                        PdfPCell addedCell = new PdfPCell(new Phrase(df.format(detail.getSumAddedFee()), normalFont));
                        addedCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        table.addCell(addedCell);

                        PdfPCell paidCell = new PdfPCell(new Phrase(df.format(detail.getSumPaidFee()), normalFont));
                        paidCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        table.addCell(paidCell);

                        PdfPCell balanceCell = new PdfPCell(new Phrase(df.format(detail.getSumAddedFee() - detail.getSumPaidFee()), normalFont));
                        balanceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        table.addCell(balanceCell);

                        grandAdded += detail.getSumAddedFee();
                        grandPaid += detail.getSumPaidFee();
                    }
                }
            }

            PdfPCell totalLblCell = new PdfPCell(new Phrase("Grand Total", boldFont));
            totalLblCell.setColspan(6);
            totalLblCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(totalLblCell);

            PdfPCell tgAdded = new PdfPCell(new Phrase(df.format(grandAdded), boldFont));
            tgAdded.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(tgAdded);

            PdfPCell tgPaid = new PdfPCell(new Phrase(df.format(grandPaid), boldFont));
            tgPaid.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(tgPaid);

            PdfPCell tgBalance = new PdfPCell(new Phrase(df.format(grandAdded - grandPaid), boldFont));
            tgBalance.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(tgBalance);

            document.add(table);
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
            // Issue #22805 review - e.getMessage() is often null for runtime
            // failures (e.g. NPE); log the stack trace too, matching the
            // Excel exporters above.
            Logger.getLogger(InwardReportControllerBht.class.getName()).log(Level.SEVERE, "Error exporting professional payment summary to PDF", e);
            JsfUtil.addErrorMessage("Failed to generate PDF: " + e.getMessage());
        }
    }

    // Issue #22803 - PDF export for the Detailed report.
    public void downloadProfessionalPaymentDetailedPdf() {
        if (professionalPaymentReportGroups == null || professionalPaymentReportGroups.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please process the report first.");
            return;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();

            String fileName = "Inpatient_Professional_Payment_Detailed_"
                    + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".pdf";

            com.lowagie.text.Document document = new com.lowagie.text.Document(
                    com.lowagie.text.PageSize.A3.rotate(), 15, 15, 30, 20);
            com.lowagie.text.pdf.PdfWriter.getInstance(document, baos);
            document.open();

            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            com.lowagie.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            com.lowagie.text.Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            DecimalFormat df = new DecimalFormat("#,##0.00");

            com.lowagie.text.Paragraph titlePara = new com.lowagie.text.Paragraph("Inpatient Professional Payment Report - Detailed", titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(10);
            document.add(titlePara);

            String[] headers = {"BHT Number", "Admitted", "Discharged", "Final Bill Number",
                "Consultant", "Speciality", "Added Fee Date", "Added Fee Value", "Paid Date",
                "Paid Bill Number", "Paid Fee Value"};
            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            float[] widths = {3f, 2.5f, 2.5f, 3f, 4f, 4f, 2.5f, 2.5f, 2.5f, 3f, 2.5f};
            table.setWidths(widths);

            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            for (InwardProfessionalPaymentAdmissionGroupDTO group : professionalPaymentReportGroups) {
                int admissionSpan = detailedAdmissionRowSpan(group);
                List<InwardProfessionalPaymentDetailRowDTO> detailedRows = group.getDetailedRows();

                PdfPCell bhtCell = new PdfPCell(new Phrase(group.getBhtNo() != null ? group.getBhtNo() : "", normalFont));
                bhtCell.setRowspan(admissionSpan);
                table.addCell(bhtCell);

                PdfPCell admittedCell = new PdfPCell(new Phrase(group.getDateOfAdmission() != null ? sdf.format(group.getDateOfAdmission()) : "", normalFont));
                admittedCell.setRowspan(admissionSpan);
                table.addCell(admittedCell);

                PdfPCell dischargedCell = new PdfPCell(new Phrase(group.getDateOfDischarge() != null ? sdf.format(group.getDateOfDischarge()) : "", normalFont));
                dischargedCell.setRowspan(admissionSpan);
                table.addCell(dischargedCell);

                PdfPCell finalBillCell = new PdfPCell(new Phrase(group.getFirstFinalBillNo() != null ? group.getFirstFinalBillNo() : "", normalFont));
                finalBillCell.setRowspan(admissionSpan);
                table.addCell(finalBillCell);

                if (detailedRows == null || detailedRows.isEmpty()) {
                    for (int i = 0; i < 7; i++) {
                        table.addCell(new Phrase("", normalFont));
                    }
                    continue;
                }

                for (InwardProfessionalPaymentDetailRowDTO detail : detailedRows) {
                    int blockSpan = detailedConsultantBlockRowSpan(detail);
                    int dataRows = Math.max(detail.rowCount(), 1);

                    PdfPCell consultantCell = new PdfPCell(new Phrase(detail.getConsultantName() != null ? detail.getConsultantName() : "", normalFont));
                    consultantCell.setRowspan(blockSpan);
                    table.addCell(consultantCell);

                    PdfPCell specialityCell = new PdfPCell(new Phrase(detail.getSpecialityName() != null ? detail.getSpecialityName() : "", normalFont));
                    specialityCell.setRowspan(blockSpan);
                    table.addCell(specialityCell);

                    for (int i = 0; i < dataRows; i++) {
                        if (i < detail.getAddedFeeValues().size()) {
                            Date addedDate = detail.getAddedFeeDates().get(i);
                            table.addCell(new Phrase(addedDate != null ? sdf.format(addedDate) : "", normalFont));
                            Double addedValue = detail.getAddedFeeValues().get(i);
                            PdfPCell addedCell = new PdfPCell(new Phrase(df.format(addedValue != null ? addedValue : 0.0), normalFont));
                            addedCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                            table.addCell(addedCell);
                        } else {
                            table.addCell(new Phrase("", normalFont));
                            table.addCell(new Phrase("", normalFont));
                        }

                        if (i < detail.getPaidFeeValues().size()) {
                            Date paidDate = detail.getPaidFeeDates().get(i);
                            table.addCell(new Phrase(paidDate != null ? sdf.format(paidDate) : "", normalFont));
                            String paidBillNo = detail.getPaidBillNumbers().get(i);
                            table.addCell(new Phrase(paidBillNo != null ? paidBillNo : "", normalFont));
                            Double paidValue = detail.getPaidFeeValues().get(i);
                            PdfPCell paidCell = new PdfPCell(new Phrase(df.format(paidValue != null ? paidValue : 0.0), normalFont));
                            paidCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                            table.addCell(paidCell);
                        } else {
                            table.addCell(new Phrase("", normalFont));
                            table.addCell(new Phrase("", normalFont));
                            table.addCell(new Phrase("", normalFont));
                        }
                    }

                    PdfPCell totalLabelCell = new PdfPCell(new Phrase("Total", boldFont));
                    table.addCell(totalLabelCell);
                    PdfPCell totalAddedCell = new PdfPCell(new Phrase(df.format(detail.getSumAddedFee()), boldFont));
                    totalAddedCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(totalAddedCell);
                    table.addCell(new Phrase("", normalFont));
                    table.addCell(new Phrase("", normalFont));
                    PdfPCell totalPaidCell = new PdfPCell(new Phrase(df.format(detail.getSumPaidFee()), boldFont));
                    totalPaidCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(totalPaidCell);

                    PdfPCell balanceLabelCell = new PdfPCell(new Phrase("Balance to Pay", boldFont));
                    table.addCell(balanceLabelCell);
                    PdfPCell balanceValueCell = new PdfPCell(new Phrase(df.format(detail.getSumAddedFee() - detail.getSumPaidFee()), boldFont));
                    balanceValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(balanceValueCell);
                    table.addCell(new Phrase("", normalFont));
                    table.addCell(new Phrase("", normalFont));
                    table.addCell(new Phrase("", normalFont));
                }
            }

            document.add(table);
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
            Logger.getLogger(InwardReportControllerBht.class.getName()).log(Level.SEVERE, "Error exporting professional payment detailed report to PDF", e);
            JsfUtil.addErrorMessage("Failed to generate PDF: " + e.getMessage());
        }
    }

    private List<InwardProfessionalPaymentAdmissionGroupDTO> fetchInwardProfessionalPaymentReportGroups() {
        List<InwardProfessionalPaymentAdmissionDTO> admissions = fetchProfessionalPaymentAdmissionsForDepartment();
        List<InwardProfessionalPaymentFeeRowDTO> feeRows = fetchProfessionalFeeRowsForDepartment();

        Map<Long, List<InwardProfessionalPaymentFeeRowDTO>> feeRowsByEncounter = feeRows.stream()
                .collect(Collectors.groupingBy(InwardProfessionalPaymentFeeRowDTO::getPatientEncounterId, LinkedHashMap::new, Collectors.toList()));

        List<Long> encounterIds = admissions.stream()
                .map(InwardProfessionalPaymentAdmissionDTO::getPatientEncounterId)
                .collect(Collectors.toList());
        Map<Long, String> firstFinalBillNoByEncounter = fetchFirstFinalBillNumbersByEncounterIds(encounterIds);

        List<InwardProfessionalPaymentAdmissionGroupDTO> groups = new ArrayList<>();
        for (InwardProfessionalPaymentAdmissionDTO admission : admissions) {
            InwardProfessionalPaymentAdmissionGroupDTO group = new InwardProfessionalPaymentAdmissionGroupDTO();
            group.setBhtNo(admission.getBhtNo());
            group.setDateOfAdmission(admission.getDateOfAdmission());
            group.setDateOfDischarge(admission.getDateOfDischarge());
            group.setFirstFinalBillNo(firstFinalBillNoByEncounter.get(admission.getPatientEncounterId()));

            List<InwardProfessionalPaymentFeeRowDTO> rowsForAdmission
                    = feeRowsByEncounter.getOrDefault(admission.getPatientEncounterId(), new ArrayList<>());
            group.setDetailRows(groupIntoDetailRows(rowsForAdmission));
            group.setDetailedRows(groupIntoDetailedRows(rowsForAdmission));
            groups.add(group);
        }

        // Issue #22803 - "admissions without professional fees" filter.
        // detailRows/detailedRows are always empty/non-empty together, since
        // both are derived from the same feeRowsByEncounter map.
        if (onlyAdmissionsWithoutProfessionalFees) {
            groups = groups.stream()
                    .filter(g -> g.getDetailRows() == null || g.getDetailRows().isEmpty())
                    .collect(Collectors.toList());
        }

        return groups;
    }

    // Issue #22805 review - shared by both query builders below, so the
    // BHT-range/date-range predicate can't drift between them.
    private String appendProfessionalPaymentRangePredicate(String jpql, Map<String, Object> params) {
        if ("bhtRange".equals(professionalPaymentSearchMode)
                && admissionFromForProfessionalPaymentReport != null
                && admissionToForProfessionalPaymentReport != null) {
            long peIdFrom = Math.min(admissionFromForProfessionalPaymentReport.getId(), admissionToForProfessionalPaymentReport.getId());
            long peIdTo = Math.max(admissionFromForProfessionalPaymentReport.getId(), admissionToForProfessionalPaymentReport.getId());
            params.put("peIdFrom", peIdFrom);
            params.put("peIdTo", peIdTo);
            return jpql + "AND pe.id BETWEEN :peIdFrom AND :peIdTo ";
        }
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);
        return jpql + "AND pe.dateOfAdmission BETWEEN :fromDate AND :toDate ";
    }

    private List<InwardProfessionalPaymentAdmissionDTO> fetchProfessionalPaymentAdmissionsForDepartment() {
        String jpql = "SELECT new com.divudi.core.data.dto.InwardProfessionalPaymentAdmissionDTO("
                + "pe.id, "
                + "COALESCE(pe.bhtNo, ''), "
                + "pe.dateOfAdmission, "
                + "pe.dateOfDischarge, "
                + "COALESCE(fb.deptId, '')) "
                + "FROM PatientEncounter pe "
                + "LEFT JOIN pe.finalBill fb "
                + "WHERE pe.department = :department ";

        Map<String, Object> params = new HashMap<>();
        params.put("department", department);

        jpql = appendProfessionalPaymentRangePredicate(jpql, params);

        jpql += "ORDER BY pe.dateOfAdmission, pe.id";

        List<InwardProfessionalPaymentAdmissionDTO> result = (List<InwardProfessionalPaymentAdmissionDTO>) billFacade.findLightsByJpqlWithoutCache(jpql, params, TemporalType.TIMESTAMP);
        return result != null ? result : new ArrayList<>();
    }

    private List<InwardProfessionalPaymentFeeRowDTO> fetchProfessionalFeeRowsForDepartment() {
        String jpql = "SELECT new com.divudi.core.data.dto.InwardProfessionalPaymentFeeRowDTO("
                + "pe.id, "
                + "st.id, "
                + "COALESCE(per.name, ''), "
                + "sp.id, "
                + "COALESCE(sp.name, ''), "
                + "bf.feeValue, "
                + "bf.paidValue, "
                + "COALESCE(rbfBill.deptId, ''), "
                + "rbfBill.createdAt, "
                + "bf.createdAt) "
                + "FROM BillFee bf "
                + "JOIN bf.bill b "
                + "JOIN b.patientEncounter pe "
                + "LEFT JOIN bf.staff st "
                + "LEFT JOIN st.person per "
                + "LEFT JOIN bf.speciality sp "
                + "LEFT JOIN bf.referenceBillFee rbf "
                + "LEFT JOIN rbf.bill rbfBill "
                + "WHERE bf.retired = false "
                + "AND b.retired = false "
                + "AND b.cancelled = false "
                + "AND b.billType = :billType "
                + "AND pe.department = :department ";

        Map<String, Object> params = new HashMap<>();
        params.put("billType", BillType.InwardProfessional);
        params.put("department", department);

        jpql = appendProfessionalPaymentRangePredicate(jpql, params);

        jpql += "ORDER BY pe.id, st.id, sp.id";

        List<InwardProfessionalPaymentFeeRowDTO> result = (List<InwardProfessionalPaymentFeeRowDTO>) billFacade.findLightsByJpqlWithoutCache(jpql, params, TemporalType.TIMESTAMP);
        return result != null ? result : new ArrayList<>();
    }

    // billTypeAtomic (not just billType) excludes cancellation-copy final bill
    // records, matching InwardSearch.fetchFinalBillVersions(). Batched across
    // all admissions on the report to avoid one query per admission.
    private Map<Long, String> fetchFirstFinalBillNumbersByEncounterIds(List<Long> encounterIds) {
        Map<Long, String> result = new HashMap<>();
        if (encounterIds == null || encounterIds.isEmpty()) {
            return result;
        }
        String jpql = "SELECT b.patientEncounter.id, b.deptId, b.finalBillVersionSerial "
                + "FROM Bill b "
                + "WHERE b.patientEncounter.id IN :encounterIds "
                + "AND b.billType = :billType "
                + "AND b.billTypeAtomic = :atomic "
                + "AND b.retired = false "
                + "ORDER BY b.patientEncounter.id, b.finalBillVersionSerial ASC";
        Map<String, Object> params = new HashMap<>();
        params.put("encounterIds", encounterIds);
        params.put("billType", BillType.InwardFinalBill);
        params.put("atomic", BillTypeAtomic.INWARD_FINAL_BILL);

        List<Object[]> rows = billFacade.findAggregates(jpql, params, TemporalType.TIMESTAMP);
        if (rows == null) {
            return result;
        }
        for (Object[] row : rows) {
            Long encounterId = (Long) row[0];
            // ORDER BY finalBillVersionSerial ASC - first row seen per encounter wins.
            if (!result.containsKey(encounterId)) {
                result.put(encounterId, (String) row[1]);
            }
        }
        return result;
    }

    // Groups charge-side BillFee rows by (staff, speciality) within one
    // admission, summing fee/paid values and collecting the distinct payment
    // bills that settled each doctor+speciality into comma-joined strings.
    private List<InwardProfessionalPaymentReportRowDTO> groupIntoDetailRows(List<InwardProfessionalPaymentFeeRowDTO> rows) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Map<String, InwardProfessionalPaymentReportRowDTO> rowsByStaffAndSpeciality = new LinkedHashMap<>();
        Map<String, LinkedHashSet<String>> billNumbersByKey = new LinkedHashMap<>();
        Map<String, LinkedHashSet<String>> billDatesByKey = new LinkedHashMap<>();

        for (InwardProfessionalPaymentFeeRowDTO fee : rows) {
            String key = fee.getStaffId() + "_" + fee.getSpecialityId();
            InwardProfessionalPaymentReportRowDTO row = rowsByStaffAndSpeciality.get(key);
            if (row == null) {
                row = new InwardProfessionalPaymentReportRowDTO();
                row.setConsultantName(fee.getStaffName());
                row.setSpecialityName(fee.getSpecialityName());
                rowsByStaffAndSpeciality.put(key, row);
                billNumbersByKey.put(key, new LinkedHashSet<>());
                billDatesByKey.put(key, new LinkedHashSet<>());
            }
            row.setSumAddedFee(row.getSumAddedFee() + (fee.getFeeValue() != null ? fee.getFeeValue() : 0.0));
            row.setSumPaidFee(row.getSumPaidFee() + (fee.getPaidValue() != null ? fee.getPaidValue() : 0.0));

            if (fee.getReferenceBillDeptId() != null && !fee.getReferenceBillDeptId().isEmpty()) {
                billNumbersByKey.get(key).add(fee.getReferenceBillDeptId());
            }
            if (fee.getReferenceBillCreatedAt() != null) {
                billDatesByKey.get(key).add(dateFormat.format(fee.getReferenceBillCreatedAt()));
            }
        }

        List<InwardProfessionalPaymentReportRowDTO> result = new ArrayList<>();
        for (Map.Entry<String, InwardProfessionalPaymentReportRowDTO> entry : rowsByStaffAndSpeciality.entrySet()) {
            InwardProfessionalPaymentReportRowDTO row = entry.getValue();
            row.setPaymentBillNumbers(String.join(", ", billNumbersByKey.get(entry.getKey())));
            row.setPaymentDates(String.join(", ", billDatesByKey.get(entry.getKey())));
            result.add(row);
        }
        return result;
    }

    // Issue #22803 - Detailed report grouping. Same (staff, speciality) key
    // as groupIntoDetailRows above, but keeps every individual fee row's
    // Added/Paid values and dates (rather than aggregating into one summary
    // row + comma-joined strings), for the Detailed report's per-consultant
    // block layout. Settlement in this codebase is always full-value per fee
    // (see InwardStaffPaymentBillController.saveBillCompo/
    // saveBillItemForPaymentBill) - paidValue is either 0 or equal to
    // feeValue - so a paid entry is only appended when paidValue > 0.
    private List<InwardProfessionalPaymentDetailRowDTO> groupIntoDetailedRows(List<InwardProfessionalPaymentFeeRowDTO> rows) {
        Map<String, InwardProfessionalPaymentDetailRowDTO> rowsByStaffAndSpeciality = new LinkedHashMap<>();

        for (InwardProfessionalPaymentFeeRowDTO fee : rows) {
            String key = fee.getStaffId() + "_" + fee.getSpecialityId();
            InwardProfessionalPaymentDetailRowDTO row = rowsByStaffAndSpeciality.get(key);
            if (row == null) {
                row = new InwardProfessionalPaymentDetailRowDTO();
                row.setConsultantName(fee.getStaffName());
                row.setSpecialityName(fee.getSpecialityName());
                rowsByStaffAndSpeciality.put(key, row);
            }

            double feeValue = fee.getFeeValue() != null ? fee.getFeeValue() : 0.0;
            row.getAddedFeeValues().add(feeValue);
            row.getAddedFeeDates().add(fee.getAddedFeeDate());
            row.setSumAddedFee(row.getSumAddedFee() + feeValue);

            if (fee.getPaidValue() != null && fee.getPaidValue() > 0) {
                row.getPaidFeeValues().add(fee.getPaidValue());
                row.getPaidFeeDates().add(fee.getReferenceBillCreatedAt());
                row.getPaidBillNumbers().add(fee.getReferenceBillDeptId());
                row.setSumPaidFee(row.getSumPaidFee() + fee.getPaidValue());
            }
        }

        return new ArrayList<>(rowsByStaffAndSpeciality.values());
    }

    // Issue #22803 - Department-scoped BHT-range autocomplete for the
    // "Admission From" / "Admission To" fields (BHT-range search mode).
    // Deliberately not reusing AdmissionController.completeBht /
    // completePatientEncounter - both are unfiltered by department and use a
    // broken "c.patient" alias.
    public List<PatientEncounter> completeAdmissionForProfessionalPaymentReport(String query) {
        Department dept = department != null ? department : sessionController.getDepartment();
        if (dept == null || query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String jpql = "SELECT pe FROM PatientEncounter pe "
                + "WHERE pe.department = :department "
                + "AND pe.retired = false "
                + "AND pe.bhtNo LIKE :bhtNo "
                + "ORDER BY pe.bhtNo";

        Map<String, Object> params = new HashMap<>();
        params.put("department", dept);
        params.put("bhtNo", "%" + query.trim() + "%");

        List<PatientEncounter> result = (List<PatientEncounter>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP, 20);
        return result != null ? result : new ArrayList<>();
    }

    // View button on the list row: load the bill into InwardSearch and open the
    // existing reprint page (same destination as the interim-bill View Bill flow).
    public String navigateToReprintServiceBill(Long billId) {
        if (billId == null) {
            JsfUtil.addErrorMessage("No bill");
            return null;
        }
        Bill b = billFacade.find(billId);
        if (b == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        inwardSearch.setBill(b);
        return "/inward/inward_reprint_bill_service?faces-redirect=true";
    }

    public String navigateToAdmissionProfile() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No encounter selected");
            return null;
        }
        if (patientEncounter instanceof Admission) {
            admissionController.setCurrent((Admission) patientEncounter);
        }
        return "/inward/admission_profile?faces-redirect=true";
    }

    private List<InpatientPharmacyIssueDTO> fetchPharmacyIssueDtos(List<BillTypeAtomic> billTypes) {
        String jpql = "SELECT new com.divudi.core.data.dto.InpatientPharmacyIssueDTO("
                + "bi.id, "
                + "bi.item.name, "
                + "bi.qty, "
                + "bi.netValue, "
                + "bi.bill.createdAt, "
                + "bi.bill.billTypeAtomic, "
                + "COALESCE(bi.bill.department.name, 'N/A'), "
                + "refBi.id, "
                + "bi.grossValue, "
                + "bi.discount, "
                + "bi.marginValue) "
                + "FROM BillItem bi LEFT JOIN bi.referanceBillItem refBi "
                + "WHERE bi.bill.patientEncounter = :patientEncounter "
                + "AND bi.bill.billTypeAtomic IN :billTypeAtomics "
                + "AND bi.retired = FALSE "
                + "AND bi.bill.retired = FALSE "
                + "AND bi.bill.cancelled = FALSE ";

        Map<String, Object> params = new HashMap<>();
        params.put("billTypeAtomics", billTypes);
        params.put("patientEncounter", patientEncounter);

        if (department != null) {
            jpql += "AND bi.bill.department = :department ";
            params.put("department", department);
        }

        jpql += "ORDER BY bi.bill.createdAt, bi.id";

        List<InpatientPharmacyIssueDTO> result = (List<InpatientPharmacyIssueDTO>) billItemFacade.findLightsByJpql(jpql, params);

        return result != null ? result : new ArrayList<>();
    }

    private List<InpatientPharmacyNetSummaryDTO> fetchPharmacyNetSummaryDtos(List<BillTypeAtomic> issueTypes,
            List<BillTypeAtomic> returnTypes, List<BillTypeAtomic> cancellationTypes) {
        String jpql = "SELECT new com.divudi.core.data.dto.InpatientPharmacyNetSummaryDTO("
                + "bi.item.id, "
                + "bi.item.name, "
                + "SUM(0 - bi.pharmaceuticalBillItem.qty), "
                + "SUM(bi.grossValue), "
                + "SUM(bi.discount), "
                + "SUM(bi.marginValue), "
                + "SUM(bi.netValue)) "
                + "FROM BillItem bi "
                + "WHERE bi.bill.patientEncounter = :patientEncounter "
                + "AND bi.retired = FALSE "
                + "AND bi.bill.retired = FALSE "
                + "AND ("
                + "  (bi.bill.billTypeAtomic IN :issueTypes AND bi.bill.cancelled = FALSE) "
                + "  OR bi.bill.billTypeAtomic IN :returnTypes "
                + "  OR bi.bill.billTypeAtomic IN :cancellationTypes"
                + ") "
                + "GROUP BY bi.item.id, bi.item.name "
                + "ORDER BY bi.item.name";

        Map<String, Object> params = new HashMap<>();
        params.put("patientEncounter", patientEncounter);
        params.put("issueTypes", issueTypes);
        params.put("returnTypes", returnTypes);
        params.put("cancellationTypes", cancellationTypes);

        List<InpatientPharmacyNetSummaryDTO> result = (List<InpatientPharmacyNetSummaryDTO>) billItemFacade.findLightsByJpql(jpql, params);

        return result != null ? result : new ArrayList<>();
    }

    public String madeNull() {
        patientEncounter = null;
        return "/pharmacy/reports/inpatient_pharmacy_item_list.xhtml?faces-redirect=true";
    }

    public String madeNullIssue() {
        patientEncounter = null;
        bill = null;
        //department = null;
        return "/pharmacy/reports/inpatient_pharmacy_requested_item_overview_by_bill?faces-redirect=true";
    }

    public String navigateToInpatientLabItemList() {

        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No encounter");
            return null;
        }

        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.add(BillTypeAtomic.INWARD_SERVICE_BILL);
        List<BillItem> labBillItems = new ArrayList<>();
        labBillItems = fetchLabBillItems(patientEncounter, btas);

        labBillItems = labBillItems.stream()
                .filter(bi -> bi.getItem().getClass().equals(Investigation.class))
                .collect(Collectors.toList());

        labBillItemsToPatientEncounterNetTotal = 0.0;
        labBillItemsToPatientEncounter = new ArrayList<>();

        for (BillItem bi : labBillItems) {
            if (!bi.getBill().isCancelled()) {
                labBillItemsToPatientEncounter.add(bi);
                labBillItemsToPatientEncounterNetTotal += bi.getNetValue();
            }
        }

        return "/inward/reports/inpatient_lab_investigation_item_list?faces-redirect=true";
    }

    public List<BillItem> fetchLabBillItems(PatientEncounter pt, List<BillTypeAtomic> billTypesAtomics) {
        String jpql = "select bi "
                + "from BillItem bi "
                + "where bi.bill.retired = false "
                + "and bi.bill.cancelled = false "
                + "and bi.retired = false "
                + "and bi.refunded = false "
                + "and bi.bill.billTypeAtomic in :billTypesAtomics "
                + "and bi.bill.patientEncounter = :pe ";

        HashMap<String, Object> params = new HashMap<>();
        params.put("pe", pt);
        params.put("billTypesAtomics", billTypesAtomics);

        return billItemFacade.findByJpql(jpql, params);
    }

    public String navigateToInpatientPharmacyItemListForPharmacy() {
        department = sessionController.getLoggedUser().getDepartment();
        patientEncounter = null;
        //department = null;
        return "/pharmacy/reports/inpatient_pharmacy_item_list?faces-redirect=true";
    }

    public String navigateToInpatientPharmacyRequestedAndIssuedOverviewInPharmacy() {
        department = sessionController.getLoggedUser().getDepartment();
        patientEncounter = null;
        bill = null;
        //department = null;
        return "/pharmacy/reports/inpatient_pharmacy_requested_item_overview_by_bill?faces-redirect=true";
    }

    public void processInpatientPharmacyItemList() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No encounter");
            return;
        }
        patient = patientEncounter.getPatient();
        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.add(BillTypeAtomic.PHARMACY_DIRECT_ISSUE);
        btas.add(BillTypeAtomic.PHARMACY_DIRECT_ISSUE_CANCELLED);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_CANCELLATION);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_RETURN);
        btas.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD);
        btas.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN);
        btas.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION);
        pharmacyIssueBillItemsToPatientEncounterNetTotal = 0.0;
        pharmacyIssueBillItemsToPatientEncounter = billService.fetchBillItems(null, null, null, null, department, null, btas, patientEncounter);
        if (pharmacyIssueBillItemsToPatientEncounter != null) {
            for (BillItem bi : pharmacyIssueBillItemsToPatientEncounter) {
                switch (bi.getBill().getBillTypeAtomic()) {
                    case PHARMACY_DIRECT_ISSUE_CANCELLED:
                    case DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION:
                    case DIRECT_ISSUE_INWARD_MEDICINE_RETURN:
                    case DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_CANCELLATION:
                    case DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_RETURN:
                    case ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION:
                    case ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN:
                        pharmacyIssueBillItemsToPatientEncounterNetTotal -= Math.abs(bi.getNetValue());
                        break;
                    case PHARMACY_DIRECT_ISSUE:
                    case DIRECT_ISSUE_INWARD_MEDICINE:
                    case DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE:
                    case ISSUE_MEDICINE_ON_REQUEST_INWARD:
                        pharmacyIssueBillItemsToPatientEncounterNetTotal += Math.abs(bi.getNetValue());
                        break;
                }

            }
        }
    }

    public Double[] fetchRoomValues() {
        HashMap hm = new HashMap();
        String sql = "SELECT"
                + " sum(pr.calculatedRoomCharge),"
                + " sum(pr.discountRoomCharge),"
                + " sum(pr.calculatedMaintainCharge), "
                + " sum(pr.discountMaintainCharge), "
                + " sum(pr.calculatedMoCharge), "
                + " sum(pr.discountMoCharge), "
                + " sum(pr.calculatedNursingCharge), "
                + " sum(pr.discountNursingCharge), "
                + " sum(pr.calculatedLinenCharge), "
                + " sum(pr.discountLinenCharge), "
                + " sum(pr.calculatedAdministrationCharge), "
                + " sum(pr.discountAdministrationCharge), "
                + " sum(pr.calculatedMedicalCareCharge), "
                + " sum(pr.discountMedicalCareCharge) "
                + " FROM PatientRoom pr "
                + " where pr.retired=false"
                //                + " and pr.patientEncounter.paymentFinalized=true
                + " and pr.patientEncounter=:pe ";

        if (admissionType != null) {
            sql = sql + " and pr.patientEncounter.admissionType=:at ";
            hm.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql = sql + " and pr.patientEncounter.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and pr.patientEncounter.creditCompany=:cc ";
            hm.put("cc", institution);
        }
        hm.put("pe", patientEncounter);

        Object obj[] = patientRoomFacade.findAggregateModified(sql, hm, TemporalType.TIMESTAMP);
        if (obj == null) {
            Double[] dbl = new Double[14];
            dbl[0] = 0.0;
            dbl[1] = 0.0;
            dbl[2] = 0.0;
            dbl[3] = 0.0;
            dbl[4] = 0.0;
            dbl[5] = 0.0;
            dbl[6] = 0.0;
            dbl[7] = 0.0;
            dbl[8] = 0.0;
            dbl[9] = 0.0;
            dbl[10] = 0.0;
            dbl[11] = 0.0;
            dbl[12] = 0.0;
            dbl[13] = 0.0;

            return dbl;
        } else {
            return Arrays.copyOf(obj, obj.length, Double[].class);
        }

    }

    public List<Object[]> fetchDoctorPaymentInward() {
        HashMap hm = new HashMap();
        String sql = "Select b.paidForBillFee.staff.speciality,"
                + " sum(b.paidForBillFee.feeValue) "
                + " FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.paidForBillFee.bill.patientEncounter.discharged=true "
                + " and(b.paidForBillFee.bill.billType=:refType1 "
                + " or b.paidForBillFee.bill.billType=:refType2 )"
                + " and b.paidForBillFee.bill.patientEncounter=:bhtno ";
        hm.put("bhtno", patientEncounter);

        if (admissionType != null) {
            sql = sql + " and b.bill.patientEncounter.admissionType=:at ";
            hm.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql = sql + " and b.bill.patientEncounter.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.bill.patientEncounter.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        sql += " group by b.paidForBillFee.staff.speciality "
                + " order by b.paidForBillFee.staff.speciality.name ";

        hm.put("bType", BillType.PaymentBill);
        hm.put("refType1", BillType.InwardBill);
        hm.put("refType2", BillType.InwardProfessional);

        return billFeeFacade.findAggregates(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<Object[]> fetchDoctorPaymentInwardModified() {
        HashMap hm = new HashMap();
        String sql = "Select b.staff.speciality,"
                + " sum(b.feeValue) "
                + " FROM BillFee b "
                + " where b.retired=false "
                + " and b.bill.patientEncounter.discharged=true "
                + " and(b.bill.billType=:refType1 "
                + " or b.bill.billType=:refType2 )"
                + " and b.bill.patientEncounter=:bhtno ";
        hm.put("bhtno", patientEncounter);

        if (admissionType != null) {
            sql = sql + " and b.bill.patientEncounter.admissionType=:at ";
            hm.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql = sql + " and b.bill.patientEncounter.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.bill.patientEncounter.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        sql += " group by b.staff.speciality "
                + " order by b.staff.speciality.name ";

        hm.put("refType1", BillType.InwardBill);
        hm.put("refType2", BillType.InwardProfessional);

        return billFeeFacade.findAggregates(sql, hm, TemporalType.TIMESTAMP);

    }

    public void createDoctorPaymentInward() {
        professionals = new ArrayList<>();
        List<Object[]> list = fetchDoctorPaymentInwardModified();
        for (Object[] obj : list) {
            Speciality speciality = (Speciality) obj[0];
            double dbl = (Double) obj[1];

            String1Value2 string1Value2 = new String1Value2();
            string1Value2.setString(speciality.getName());
            string1Value2.setValue1(dbl);

            professionalGross += string1Value2.getValue1();

            professionals.add(string1Value2);

        }

    }

    public void createTimedService() {
        HashMap hm = new HashMap();
        String sql = "SELECT i.item,"
                + " sum(i.serviceValue),"
                + " sum(i.discount) "
                + " FROM PatientItem i "
                + " where i.retired=false "
                + " and i.patientEncounter.discharged=true "
                + " and i.patientEncounter=:bhtno ";
        hm.put("bhtno", patientEncounter);

        if (admissionType != null) {
            sql = sql + " and i.patientEncounter.admissionType=:at ";
            hm.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and i.patientEncounter.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and i.patientEncounter.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        sql += " group by i.item "
                + " order by i.item.name";

        List<Object[]> results = billFeeFacade.findAggregates(sql, hm, TemporalType.DATE);

        if (results == null) {
            return;
        }

        timedServices = new ArrayList<>();

        for (Object[] obj : results) {
            String1Value2 row = new String1Value2();
            Item item = (Item) obj[0];
            row.setString(item.getName());
            row.setValue1((double) obj[1]);
            row.setValue2((double) obj[2]);

            timedGross += row.getValue1();
            timedDiscount += row.getValue2();
            timedServices.add(row);
        }

    }

    @EJB
    BillItemFacade billItemFacade;

    public void updateBillItemAndBill(Bill bill) {
        bill.setNetTotal(bill.getTotal() - bill.getDiscount());
        billFacade.edit(bill);

        if (bill.getSingleBillItem() != null) {
            bill.getSingleBillItem().setDiscount(bill.getDiscount());
            bill.getSingleBillItem().setNetValue(bill.getSingleBillItem().getGrossValue() - bill.getSingleBillItem().getDiscount());

            billItemFacade.edit(bill.getSingleBillItem());
        }
    }

    public void updateBillItem(BillItem billItem) {
        billItem.setNetValue((billItem.getGrossValue() + billItem.getMarginValue()) - billItem.getDiscount());

        billItemFacade.edit(billItem);
    }

    public void updatePatientBillItem(PatientItem patientItem) {

        patientItemFacade.edit(patientItem);
    }

    public void updateBillFee(BillFee billFee) {
        billFee.setFeeValue((billFee.getFeeGrossValue() + billFee.getFeeMargin()) - billFee.getFeeDiscount());

        billFeeFacade.edit(billFee);
    }

    public Double[] fetchAdmissionFeeValues() {
        Bill b = inwardBeanController.fetchFinalBill(patientEncounter);
        HashMap hm = new HashMap();
        hm.put("inwTp", InwardChargeType.AdmissionFee);
        hm.put("btp", BillType.InwardFinalBill);
        String sql = "SELECT  sum(i.grossValue),"
                + " sum(i.discount),"
                + " sum(i.netValue) "
                + " FROM BillItem i "
                + " where i.retired=false"
                + " and i.inwardChargeType=:inwTp"
                + " and i.bill.billType=:btp "
                + " and i.bill.patientEncounter.discharged=true "
                + " and i.bill.patientEncounter=:bhtno "
                + " and i.bill=:b ";
        hm.put("bhtno", patientEncounter);
        hm.put("b", b);
        if (admissionType != null) {
            sql = sql + " and i.bill.patientEncounter.admissionType=:at ";
            hm.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and i.bill.patientEncounter.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and i.bill.patientEncounter.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        Object obj[] = billFeeFacade.findAggregateModified(sql, hm, TemporalType.TIMESTAMP);
//        System.err.println("OBJ " + obj);
        if (obj == null) {
            Double[] dbl = new Double[3];
            dbl[0] = 0.0;
            dbl[1] = 0.0;
            dbl[2] = 0.0;
            return dbl;
        } else {
            return Arrays.copyOf(obj, obj.length, Double[].class);
        }

    }

    public double fetchOutSideFee() {
        HashMap hm = new HashMap();

        hm.put("btp", BillType.InwardOutSideBill);
        String sql = "SELECT  sum(i.netTotal)"
                + " FROM Bill i "
                + " where i.retired=false"
                + " and i.billType=:btp "
                + " and i.patientEncounter.discharged=true "
                + " and i.patientEncounter=:bhtno ";
        hm.put("bhtno", patientEncounter);

        if (admissionType != null) {
            sql = sql + " and i.patientEncounter.admissionType=:at ";
            hm.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and i.patientEncounter.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and i.patientEncounter.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        return billFeeFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    public double fetchAdmissionFee() {
        HashMap hm = new HashMap();
        String sql = "SELECT  sum(i.admissionType.admissionFee)"
                + " FROM PatientEncounter i "
                + " where i.retired=false "
                + " and i.discharged=true "
                + " and i..bhtno=:bhtno ";
        hm.put("bhtno", patientEncounter);

        if (admissionType != null) {
            sql = sql + " and i.admissionType=:at ";
            hm.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and i.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and i.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        return billFeeFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    public Double[] fetchIssue(BillType billType) {
        String sql;
        HashMap hm = new HashMap();
        sql = "SELECT  sum(b.grossValue),"
                + " sum(b.marginValue),"
                + " sum(b.discount),"
                + " sum(b.netValue) "
                + " FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.bill.patientEncounter.discharged=true "
                + " and b.bill.patientEncounter=:pe ";

        if (admissionType != null) {
            sql = sql + " and b.bill.patientEncounter.admissionType=:at ";
            hm.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and b.bill.patientEncounter.paymentMethod=:bt ";
            hm.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.bill.patientEncounter.creditCompany=:cc ";
            hm.put("cc", institution);
        }

        hm.put("pe", patientEncounter);
        hm.put("btp", billType);

        Object obj[] = billFeeFacade.findAggregateModified(sql, hm, TemporalType.TIMESTAMP);
//        System.err.println("OBJ " + obj);
        if (obj == null) {
            Double[] dbl = new Double[4];
            dbl[0] = 0.0;
            dbl[1] = 0.0;
            dbl[2] = 0.0;
            dbl[3] = 0.0;
            return dbl;
        } else {
            return Arrays.copyOf(obj, obj.length, Double[].class);
        }

    }

    public void createInwardService() {
        inwardCharges = new ArrayList<>();
        Double[] dbl = fetchAdmissionFeeValues();
        String2Value4 string1Value3 = new String2Value4();
        string1Value3.setString(InwardChargeType.AdmissionFee.getLabel());
        string1Value3.setValue1(dbl[0]);
        string1Value3.setValue3(dbl[1]);
        string1Value3.setValue4(dbl[2]);
        inwardGross += string1Value3.getValue1();
        inwardDiscount += string1Value3.getValue3();
        inwardNetValue += string1Value3.getValue4();
        inwardCharges.add(string1Value3);

        string1Value3 = new String2Value4();
        dbl = fetchIssue(BillType.PharmacyBhtPre);
        string1Value3.setString(InwardChargeType.Medicine.getLabel());
        string1Value3.setValue1(dbl[0]);
        string1Value3.setValue2(dbl[1]);
        string1Value3.setValue3(dbl[2]);
        string1Value3.setValue4(dbl[3]);
        inwardGross += string1Value3.getValue1();
        inwardMargin += string1Value3.getValue2();
        inwardDiscount += string1Value3.getValue3();
        inwardNetValue += string1Value3.getValue4();
        inwardCharges.add(string1Value3);

        string1Value3 = new String2Value4();
        dbl = fetchIssue(BillType.StoreBhtPre);
        string1Value3.setString(InwardChargeType.GeneralIssuing.getLabel());
        string1Value3.setValue1(dbl[0]);
        string1Value3.setValue2(dbl[1]);
        string1Value3.setValue3(dbl[2]);
        string1Value3.setValue4(dbl[3]);
        inwardGross += string1Value3.getValue1();
        inwardMargin += string1Value3.getValue2();
        inwardDiscount += string1Value3.getValue3();
        inwardNetValue += string1Value3.getValue4();
        inwardCharges.add(string1Value3);

        string1Value3 = new String2Value4();
        string1Value3.setString("Out Side Charges : ");
        string1Value3.setValue1(fetchOutSideFee());
        string1Value3.setValue4(string1Value3.getValue1());
        inwardGross += string1Value3.getValue1();
        inwardNetValue += string1Value3.getValue1();
        inwardCharges.add(string1Value3);

    }

    public void createOpdServiceWithoutPro() {
        String sql;
        Map m = new HashMap();
        sql = "select bf.billItem.item.category, "
                + " sum(bf.feeDiscount),"
                + " sum(bf.feeMargin),"
                + " sum(bf.feeGrossValue),"
                + " sum(bf.feeValue)"
                + " from BillFee bf "
                + " where bf.retired=false "
                + " and bf.bill.patientEncounter.discharged=true "
                + " and bf.billItem.retired=false "
                + " and bf.fee.feeType!=:ftp "
                + " and bf.bill.patientEncounter=:bhtno ";

        m.put("ftp", FeeType.Staff);
        m.put("billType", BillType.InwardBill);
        m.put("bhtno", patientEncounter);
        sql = sql + " and bf.bill.billType=:billType ";

        if (admissionType != null) {
            sql = sql + " and bf.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and bf.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and bf.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", institution);
        }

        sql = sql + " group by bf.billItem.item.category order by bf.billItem.item.category.name";
        List<Object[]> results = billFeeFacade.findAggregates(sql, m);

        ////// // System.out.println("results = " + results);
//        PatientEncounter pe = new PatientEncounter();
//        pe.getAdmissionType();
        if (results == null) {
            return;
        }

        opdServices = new ArrayList<>();

        for (Object[] objs : results) {

            OpdService row = new OpdService();

            row.setCategory((Category) objs[0]);
            row.setDiscount((Double) objs[1]);
            row.setMargin((Double) objs[2]);
            row.setGrossValue((Double) objs[3]);
            row.setNetValue((Double) objs[4]);

            opdSrviceGross += row.getGrossValue();
            opdServiceMargin += row.getMargin();
            opdServiceDiscount += row.getDiscount();
            opdNetTotal += row.getNetValue();

            opdServices.add(row);

        }

    }

    public List<BillItem> createCreditPayment(PatientEncounter pe) {

        String sql;
        Map m = new HashMap();
        sql = "SELECT bi FROM BillItem bi "
                + " WHERE bi.retired=false "
                + " and bi.bill.billType=:bty"
                + " and bi.patientEncounter=:bhtno";

        m.put("bty", BillType.CashRecieveBill);
        m.put("bhtno", pe);

        creditPayment = billItemFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

        return creditPayment;

    }

    public List<Bill> createPaidByPatient(PatientEncounter pe) {

        String sql;
        Map m = new HashMap();
        sql = "SELECT b FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billType=:bty"
                + " and b.patientEncounter=:bhtno";

        m.put("bty", BillType.InwardPaymentBill);
        m.put("bhtno", pe);

        paidbyPatientBillList = billFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

        return paidbyPatientBillList;

    }

    public void createOpdServiceWithoutPro2() {
        String sql;
        Map m = new HashMap();
        sql = "select bi.item.category, "
                + " sum(bi.discount),"
                + " sum(bi.marginValue),"
                + " sum(bi.grossValue),"
                + " sum(bi.netValue)"
                + " from BillItem bi join BillFee bf on bi.id=bf.billItem.id "
                + " where bi.bill.patientEncounter.discharged=true "
                + " and bi.retired=false "
                + " and bi.bill.retired=false "
                + " and bi.bill.patientEncounter=:bhtno "
                + " and bf.retired=false "
                + " and bf.fee.feeType!=:ftp ";

        m.put("ftp", FeeType.Staff);
        m.put("billType", BillType.InwardBill);
        m.put("bhtno", patientEncounter);
        sql = sql + " and bi.bill.billType=:billType ";

        if (admissionType != null) {
            sql = sql + " and bi.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and bi.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and bi.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", institution);
        }

        sql = sql + " group by bi.item.category "
                + " order by bi.item.category.name";
        List<Object[]> results = billFeeFacade.findAggregates(sql, m, TemporalType.DATE);

        if (results == null) {
            return;
        }

        opdServices = new ArrayList<>();

        for (Object[] objs : results) {

            OpdService row = new OpdService();
            row.setCategory((Category) objs[0]);
            row.setDiscount((double) objs[1]);
            row.setMargin((double) objs[2]);
            row.setGrossValue((double) objs[3]);
            row.setNetValue((double) objs[4]);

            opdSrviceGross += row.getGrossValue();
            opdServiceMargin += row.getMargin();
            opdServiceDiscount += row.getDiscount();

            opdServices.add(row);

        }

    }

    double roomAddition;

    public void createRoomTable() {
        roomChargeInwards = new ArrayList<>();

        Double[] dbl = fetchRoomValues();

        RoomChargeInward row = new RoomChargeInward();
        row.setInwardChargeType(InwardChargeType.RoomCharges);

        row.setCalculated(dbl[0]);
        row.setDiscount(dbl[1]);
//        row.setAddition(fetchPatientRoom_RoomAddition());
        roomGross += row.getCalculated();
        roomDiscount += row.getDiscount();
        roomAddition += row.getAddition();
        roomChargeInwards.add(row);
        ///////////////
        row = new RoomChargeInward();
        row.setInwardChargeType(InwardChargeType.MaintainCharges);
        row.setCalculated(dbl[2]);
        row.setDiscount(dbl[3]);
//        row.setAddition(fetchPatientRoom_MaintainAddition());
        roomGross += row.getCalculated();
        roomDiscount += row.getDiscount();
        roomAddition += row.getAddition();
        roomChargeInwards.add(row);
        ///////////////
        row = new RoomChargeInward();
        row.setInwardChargeType(InwardChargeType.MOCharges);
        row.setCalculated(dbl[4]);
        row.setDiscount(dbl[5]);
//        row.setAddition(fetchPatientRoom_MoAddition());
        roomGross += row.getCalculated();
        roomDiscount += row.getDiscount();
        roomAddition += row.getAddition();
        roomChargeInwards.add(row);
        ///////////////
        row = new RoomChargeInward();
        row.setInwardChargeType(InwardChargeType.NursingCharges);
        row.setCalculated(dbl[6]);
        row.setDiscount(dbl[7]);
//        row.setAddition(fetchPatientRoom_NursingAddition());
        roomGross += row.getCalculated();
        roomDiscount += row.getDiscount();
        roomAddition += row.getAddition();
        roomChargeInwards.add(row);
        ///////////////
        row = new RoomChargeInward();
        row.setInwardChargeType(InwardChargeType.LinenCharges);
        row.setCalculated(dbl[8]);
        row.setDiscount(dbl[9]);
//        row.setAddition(fetchPatientRoom_LinenAddition());
        roomGross += row.getCalculated();
        roomDiscount += row.getDiscount();
        roomAddition += row.getAddition();
        roomChargeInwards.add(row);
        ///////////////
        row = new RoomChargeInward();
        row.setInwardChargeType(InwardChargeType.AdministrationCharge);
        row.setCalculated(dbl[10]);
        row.setDiscount(dbl[11]);
//        row.setAddition(fetchPatientRoom_AdminAddition());
        roomGross += row.getCalculated();
        roomDiscount += row.getDiscount();
        roomAddition += row.getAddition();
        roomChargeInwards.add(row);
        ///////////////
        row = new RoomChargeInward();
        row.setInwardChargeType(InwardChargeType.MedicalCareICU);
        row.setCalculated(dbl[12]);
        row.setDiscount(dbl[13]);
//        row.setAddition(fetchPatientRoom_MedicalAddition());
        roomGross += row.getCalculated();
        roomDiscount += row.getDiscount();
        roomAddition += row.getAddition();
        roomChargeInwards.add(row);
        ///////////////

    }

    public void makeNull() {
        opdSrviceGross = 0;
        opdServiceMargin = 0;
        opdServiceDiscount = 0;
        opdNetTotal = 0;
        roomGross = 0;
        roomDiscount = 0;
        roomAddition = 0;
        finalBill = null;
        timedGross = 0;
        timedDiscount = 0;
        professionalGross = 0;
        inwardGross = 0;
        inwardMargin = 0;
        inwardDiscount = 0;
        inwardNetValue = 0;
    }

    public void selectLitener() {
        if (patientEncounter == null) {
            return;
        }

        bill = patientEncounter.getFinalBill();
    }

    @Inject
    InwardBeanController inwardBeanController;

    public double calTotalCreditCompany(List<BillItem> list) {
        if (list == null) {
            return 0;
        }
        double dbl = 0;
        for (BillItem bi : list) {

            dbl += bi.getNetValue();
        }

        return dbl;
    }

    public double calPaidbyPatient(List<Bill> lst) {

        if (lst == null) {
            return 0.0;
        }
        double dbl = 0.0;
        for (Bill b : lst) {
            dbl += b.getNetTotal();
        }
        return dbl;

    }

    public void process() {
        Date startTime = new Date();

        makeNull();

        createOpdServiceWithoutPro();
        createRoomTable();
        createDoctorPaymentInward();
        createTimedService();
        createInwardService();
        paidbyPatientBillList = createPaidByPatient(getPatientEncounter());
        paidbyPatientTotalValue = calPaidbyPatient(paidbyPatientBillList);

        creditPayment = createCreditPayment(getPatientEncounter());
        creditPaymentTotalValue = calTotalCreditCompany(creditPayment);

        finalBill = inwardBeanController.fetchFinalBill(patientEncounter);
        calTotal();

    }

    public void calTotal() {
        total = 0;
        discount = 0;
        netTotal = 0;

        total = opdSrviceGross + opdServiceMargin
                + roomGross + roomAddition
                + timedGross
                + professionalGross
                + inwardGross + inwardMargin;

        discount = opdServiceDiscount
                + roomDiscount
                + timedDiscount
                + inwardDiscount;

        netTotal = opdNetTotal
                + roomGross + roomAddition - roomDiscount
                + timedGross - timedDiscount
                + professionalGross
                + inwardNetValue;

    }

    public void process2() {
        makeNull();

        createOpdServiceWithoutPro2();
        createRoomTable();
        createDoctorPaymentInward();
        createTimedService();
        createInwardService();
        finalBill = inwardBeanController.fetchFinalBill(patientEncounter);

    }

    public void createAllRooms() {
        HashMap m = new HashMap();
        String sql = "SELECT pr FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.paymentFinalized=true "
                + " and pr.patientEncounter.dateOfDischarge between :fd and :td "
                + " and pr.patientEncounter.discharged=true ";

        if (getReportKeyWord().getAdmissionType() != null) {
            sql = sql + " and pr.patientEncounter.admissionType=:at ";
            m.put("at", getReportKeyWord().getAdmissionType());
        }

        if (getReportKeyWord().getPaymentMethod() != null) {
            sql = sql + " and pr.patientEncounter.paymentMethod=:bt ";
            m.put("bt", getReportKeyWord().getPaymentMethod());
        }

        if (getReportKeyWord().getInstitution() != null) {
            sql = sql + " and pr.patientEncounter.creditCompany=:cc ";
            m.put("cc", getReportKeyWord().getInstitution());
        }
        if (getReportKeyWord().getPatientEncounter() != null) {
            sql = sql + " and pr.patientEncounter=:pe ";
            m.put("pe", getReportKeyWord().getPatientEncounter());
        }

        sql += " order by pr.patientEncounter.bhtNo ";

        m.put("fd", getReportKeyWord().getFromDate());
        m.put("td", getReportKeyWord().getToDate());

        patientRooms = patientRoomFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public void fetchIssueBill() {
        issueBills = new ArrayList<>();
        if (getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("No Patient Encounter");
            return;
        }
        if (department == null) {
            JsfUtil.addErrorMessage("No Department selected");
            return;
        }
        String jpql;
        HashMap hm = new HashMap();
        jpql = "select b from Bill b "
                + "where b.patientEncounter=:pe "
                + "and b.billType=:bTp "
                + "and b.billTypeAtomic=:bta "
                + "and b.retired=false "
                + "and b.cancelled=false "
                + "and  b.toDepartment=:toDep";
        hm.put("pe", getPatientEncounter());
        hm.put("toDep", department);
        hm.put("bTp", BillType.InwardPharmacyRequest);
        hm.put("bta", BillTypeAtomic.REQUEST_MEDICINE_INWARD);

        issueBills = getBillFacade().findByJpql(jpql, hm);

    }

    public List<Bill> getBHTIssudBills(Bill b) {
        String sql = "Select b From Bill b where b.retired=false "
                + " and b.billType=:btp "
                + " and b.referenceBill=:ref ";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", BillType.PharmacyBhtPre);
        return getBillFacade().findByJpql(sql, hm);
    }

    public List<Bill> getReturnAndCancelBHTIssueBills(Bill b) {
        String sql = "Select b From Bill b where b.retired=false "
                + " and b.billTypeAtomic IN :btp "
                + " and b.referenceBill=:ref ";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN);
        btas.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION);
        hm.put("btp", btas);
        return getBillFacade().findByJpql(sql, hm);
    }

    ////////////GETTERS AND SETTERS
    public List<String1Value2> getTimedServices() {
        return timedServices;
    }

    public void setTimedServices(List<String1Value2> timedServices) {
        this.timedServices = timedServices;
    }

    public List<RoomChargeInward> getRoomChargeInwards() {
        return roomChargeInwards;
    }

    public void setRoomChargeInwards(List<RoomChargeInward> roomChargeInwards) {
        this.roomChargeInwards = roomChargeInwards;
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

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<OpdService> getOpdServices() {
        return opdServices;
    }

    public void setOpdServices(List<OpdService> opdServices) {
        this.opdServices = opdServices;
    }

    public List<String1Value2> getProfessionals() {
        return professionals;
    }

    public void setProfessionals(List<String1Value2> professionals) {
        this.professionals = professionals;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public List<String2Value4> getInwardCharges() {
        return inwardCharges;
    }

    public void setInwardCharges(List<String2Value4> inwardCharges) {
        this.inwardCharges = inwardCharges;
    }

    public PatientRoomFacade getPatientRoomFacade() {
        return patientRoomFacade;
    }

    public void setPatientRoomFacade(PatientRoomFacade patientRoomFacade) {
        this.patientRoomFacade = patientRoomFacade;
    }

    public double getOpdSrviceGross() {
        return opdSrviceGross;
    }

    public void setOpdSrviceGross(double opdSrviceGross) {
        this.opdSrviceGross = opdSrviceGross;
    }

    public double getOpdServiceMargin() {
        return opdServiceMargin;
    }

    public void setOpdServiceMargin(double opdServiceMargin) {
        this.opdServiceMargin = opdServiceMargin;
    }

    public double getOpdServiceDiscount() {
        return opdServiceDiscount;
    }

    public void setOpdServiceDiscount(double opdServiceDiscount) {
        this.opdServiceDiscount = opdServiceDiscount;
    }

    public double getRoomGross() {
        return roomGross;
    }

    public void setRoomGross(double roomGross) {
        this.roomGross = roomGross;
    }

    public double getRoomDiscount() {
        return roomDiscount;
    }

    public void setRoomDiscount(double roomDiscount) {
        this.roomDiscount = roomDiscount;
    }

    public double getTimedGross() {
        return timedGross;
    }

    public void setTimedGross(double timedGross) {
        this.timedGross = timedGross;
    }

    public double getTimedDiscount() {
        return timedDiscount;
    }

    public void setTimedDiscount(double timedDiscount) {
        this.timedDiscount = timedDiscount;
    }

    public double getProfessionalGross() {
        return professionalGross;
    }

    public void setProfessionalGross(double professionalGross) {
        this.professionalGross = professionalGross;
    }

    public double getInwardGross() {
        return inwardGross;
    }

    public void setInwardGross(double inwardGross) {
        this.inwardGross = inwardGross;
    }

    public double getInwardMargin() {
        return inwardMargin;
    }

    public void setInwardMargin(double inwardMargin) {
        this.inwardMargin = inwardMargin;
    }

    public double getInwardDiscount() {
        return inwardDiscount;
    }

    public void setInwardDiscount(double inwardDiscount) {
        this.inwardDiscount = inwardDiscount;
    }

    public double getOpdNetTotal() {
        return opdNetTotal;
    }

    public void setOpdNetTotal(double opdNetTotal) {
        this.opdNetTotal = opdNetTotal;
    }

    public List<BillItem> getCreditPayment() {
        return creditPayment;
    }

    public void setCreditPayment(List<BillItem> creditPayment) {
        this.creditPayment = creditPayment;
    }

    public double getCreditPaymentTotalValue() {
        return creditPaymentTotalValue;
    }

    public void setCreditPaymentTotalValue(double creditPaymentTotalValue) {
        this.creditPaymentTotalValue = creditPaymentTotalValue;
    }

    public List<Bill> getPaidbyPatientBillList() {
        return paidbyPatientBillList;
    }

    public void setPaidbyPatientBillList(List<Bill> paidbyPatientBillList) {
        this.paidbyPatientBillList = paidbyPatientBillList;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public double getPaidbyPatientTotalValue() {
        return paidbyPatientTotalValue;
    }

    public void setPaidbyPatientTotalValue(double paidbyPatientTotalValue) {
        this.paidbyPatientTotalValue = paidbyPatientTotalValue;
    }

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public List<PatientRoom> getPatientRooms() {
        return patientRooms;
    }

    public void setPatientRooms(List<PatientRoom> patientRooms) {
        this.patientRooms = patientRooms;
    }

    public List<BillItem> getPharmacyIssueBillItemsToPatientEncounter() {
        return pharmacyIssueBillItemsToPatientEncounter;
    }

    public void setPharmacyIssueBillItemsToPatientEncounter(List<BillItem> pharmacyIssueBillItemsToPatientEncounter) {
        this.pharmacyIssueBillItemsToPatientEncounter = pharmacyIssueBillItemsToPatientEncounter;
    }

    public double getPharmacyIssueBillItemsToPatientEncounterNetTotal() {
        return pharmacyIssueBillItemsToPatientEncounterNetTotal;
    }

    public void setPharmacyIssueBillItemsToPatientEncounterNetTotal(double pharmacyIssueBillItemsToPatientEncounterNetTotal) {
        this.pharmacyIssueBillItemsToPatientEncounterNetTotal = pharmacyIssueBillItemsToPatientEncounterNetTotal;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public List<BillItem> getLabBillItemsToPatientEncounter() {
        return labBillItemsToPatientEncounter;
    }

    public void setLabBillItemsToPatientEncounter(List<BillItem> labBillItemsToPatientEncounter) {
        this.labBillItemsToPatientEncounter = labBillItemsToPatientEncounter;
    }

    public double getLabBillItemsToPatientEncounterNetTotal() {
        return labBillItemsToPatientEncounterNetTotal;
    }

    public void setLabBillItemsToPatientEncounterNetTotal(double labBillItemsToPatientEncounterNetTotal) {
        this.labBillItemsToPatientEncounterNetTotal = labBillItemsToPatientEncounterNetTotal;
    }

    public List<Bill> getIssueBills() {
        return issueBills;
    }

    public void setIssueBills(List<Bill> issueBills) {
        this.issueBills = issueBills;
    }

    //DATA STRUCTURE
    public class OpdService {

        PatientEncounter bht;
        Category category;
        double grossValue;
        double discount;
        double margin;
        double netValue;

        public PatientEncounter getBht() {
            return bht;
        }

        public void setBht(PatientEncounter bht) {
            this.bht = bht;
        }

        public Category getCategory() {
            return category;
        }

        public void setCategory(Category category) {
            this.category = category;
        }

        public double getGrossValue() {
            return grossValue;
        }

        public void setGrossValue(double grossValue) {
            this.grossValue = grossValue;
        }

        public double getDiscount() {
            return discount;
        }

        public void setDiscount(double discount) {
            this.discount = discount;
        }

        public double getMargin() {
            return margin;
        }

        public void setMargin(double margin) {
            this.margin = margin;
        }

        public double getNetValue() {
            return netValue;
        }

        public void setNetValue(double netValue) {
            this.netValue = netValue;
        }

    }

    public class RoomChargeInward {

        InwardChargeType inwardChargeType;
        double calculated;
        double discount;
        double addition;

        public InwardChargeType getInwardChargeType() {
            return inwardChargeType;
        }

        public void setInwardChargeType(InwardChargeType inwardChargeType) {
            this.inwardChargeType = inwardChargeType;
        }

        public double getCalculated() {
            return calculated;
        }

        public void setCalculated(double calculated) {
            this.calculated = calculated;
        }

        public double getDiscount() {
            return discount;
        }

        public void setDiscount(double discount) {
            this.discount = discount;
        }

        public double getAddition() {
            return addition;
        }

        public void setAddition(double addition) {
            this.addition = addition;
        }

    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public Bill getFinalBill() {
        return finalBill;
    }

    public void setFinalBill(Bill finalBill) {
        this.finalBill = finalBill;
    }

    public double getRoomAddition() {
        return roomAddition;
    }

    public void setRoomAddition(double roomAddition) {
        this.roomAddition = roomAddition;
    }

    public InwardBeanController getInwardBeanController() {
        return inwardBeanController;
    }

    public void setInwardBeanController(InwardBeanController inwardBeanController) {
        this.inwardBeanController = inwardBeanController;
    }

    public double getInwardNetValue() {
        return inwardNetValue;
    }

    public void setInwardNetValue(double inwardNetValue) {
        this.inwardNetValue = inwardNetValue;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public PatientItemFacade getPatientItemFacade() {
        return patientItemFacade;
    }

    public void setPatientItemFacade(PatientItemFacade patientItemFacade) {
        this.patientItemFacade = patientItemFacade;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public List<InpatientPharmacyIssueDTO> getPharmacyIssueDtosToPatientEncounter() {
        return pharmacyIssueDtosToPatientEncounter;
    }

    public void setPharmacyIssueDtosToPatientEncounter(List<InpatientPharmacyIssueDTO> pharmacyIssueDtosToPatientEncounter) {
        this.pharmacyIssueDtosToPatientEncounter = pharmacyIssueDtosToPatientEncounter;
    }

    public double getPharmacyIssueDtosToPatientEncounterNetTotal() {
        return pharmacyIssueDtosToPatientEncounterNetTotal;
    }

    public void setPharmacyIssueDtosToPatientEncounterNetTotal(double pharmacyIssueDtosToPatientEncounterNetTotal) {
        this.pharmacyIssueDtosToPatientEncounterNetTotal = pharmacyIssueDtosToPatientEncounterNetTotal;
    }

    public double getPharmacyIssueDtosToPatientEncounterGrossTotal() {
        return pharmacyIssueDtosToPatientEncounterGrossTotal;
    }

    public void setPharmacyIssueDtosToPatientEncounterGrossTotal(double pharmacyIssueDtosToPatientEncounterGrossTotal) {
        this.pharmacyIssueDtosToPatientEncounterGrossTotal = pharmacyIssueDtosToPatientEncounterGrossTotal;
    }

    public double getPharmacyIssueDtosToPatientEncounterDiscountTotal() {
        return pharmacyIssueDtosToPatientEncounterDiscountTotal;
    }

    public void setPharmacyIssueDtosToPatientEncounterDiscountTotal(double pharmacyIssueDtosToPatientEncounterDiscountTotal) {
        this.pharmacyIssueDtosToPatientEncounterDiscountTotal = pharmacyIssueDtosToPatientEncounterDiscountTotal;
    }

    public double getPharmacyIssueDtosToPatientEncounterServiceChargeTotal() {
        return pharmacyIssueDtosToPatientEncounterServiceChargeTotal;
    }

    public void setPharmacyIssueDtosToPatientEncounterServiceChargeTotal(double pharmacyIssueDtosToPatientEncounterServiceChargeTotal) {
        this.pharmacyIssueDtosToPatientEncounterServiceChargeTotal = pharmacyIssueDtosToPatientEncounterServiceChargeTotal;
    }

    public List<InpatientPharmacyNetSummaryDTO> getPharmacyNetSummaryDtosToPatientEncounter() {
        return pharmacyNetSummaryDtosToPatientEncounter;
    }

    public void setPharmacyNetSummaryDtosToPatientEncounter(List<InpatientPharmacyNetSummaryDTO> pharmacyNetSummaryDtosToPatientEncounter) {
        this.pharmacyNetSummaryDtosToPatientEncounter = pharmacyNetSummaryDtosToPatientEncounter;
    }

    public double getPharmacyNetSummaryDtosToPatientEncounterNetTotal() {
        return pharmacyNetSummaryDtosToPatientEncounterNetTotal;
    }

    public void setPharmacyNetSummaryDtosToPatientEncounterNetTotal(double pharmacyNetSummaryDtosToPatientEncounterNetTotal) {
        this.pharmacyNetSummaryDtosToPatientEncounterNetTotal = pharmacyNetSummaryDtosToPatientEncounterNetTotal;
    }

    public List<InpatientServiceIssueDTO> getServiceIssueDtosToPatientEncounter() {
        return serviceIssueDtosToPatientEncounter;
    }

    public void setServiceIssueDtosToPatientEncounter(List<InpatientServiceIssueDTO> serviceIssueDtosToPatientEncounter) {
        this.serviceIssueDtosToPatientEncounter = serviceIssueDtosToPatientEncounter;
    }

    public double getServiceIssueDtosToPatientEncounterNetTotal() {
        return serviceIssueDtosToPatientEncounterNetTotal;
    }

    public void setServiceIssueDtosToPatientEncounterNetTotal(double serviceIssueDtosToPatientEncounterNetTotal) {
        this.serviceIssueDtosToPatientEncounterNetTotal = serviceIssueDtosToPatientEncounterNetTotal;
    }

    public List<BillListReportDTO> getServiceBillDtosToPatientEncounter() {
        return serviceBillDtosToPatientEncounter;
    }

    public void setServiceBillDtosToPatientEncounter(List<BillListReportDTO> serviceBillDtosToPatientEncounter) {
        this.serviceBillDtosToPatientEncounter = serviceBillDtosToPatientEncounter;
    }

    public double getServiceBillDtosToPatientEncounterNetTotal() {
        return serviceBillDtosToPatientEncounterNetTotal;
    }

    public void setServiceBillDtosToPatientEncounterNetTotal(double serviceBillDtosToPatientEncounterNetTotal) {
        this.serviceBillDtosToPatientEncounterNetTotal = serviceBillDtosToPatientEncounterNetTotal;
    }

    public List<BillListReportDTO> getPaymentBillDtosToPatientEncounter() {
        return paymentBillDtosToPatientEncounter;
    }

    public void setPaymentBillDtosToPatientEncounter(List<BillListReportDTO> paymentBillDtosToPatientEncounter) {
        this.paymentBillDtosToPatientEncounter = paymentBillDtosToPatientEncounter;
    }

    public double getPaymentBillDtosToPatientEncounterNetTotal() {
        return paymentBillDtosToPatientEncounterNetTotal;
    }

    public void setPaymentBillDtosToPatientEncounterNetTotal(double paymentBillDtosToPatientEncounterNetTotal) {
        this.paymentBillDtosToPatientEncounterNetTotal = paymentBillDtosToPatientEncounterNetTotal;
    }

    public List<BillListReportDTO> getPaymentBillDtosForDepartment() {
        return paymentBillDtosForDepartment;
    }

    public void setPaymentBillDtosForDepartment(List<BillListReportDTO> paymentBillDtosForDepartment) {
        this.paymentBillDtosForDepartment = paymentBillDtosForDepartment;
    }

    public double getPaymentBillDtosForDepartmentNetTotal() {
        return paymentBillDtosForDepartmentNetTotal;
    }

    public void setPaymentBillDtosForDepartmentNetTotal(double paymentBillDtosForDepartmentNetTotal) {
        this.paymentBillDtosForDepartmentNetTotal = paymentBillDtosForDepartmentNetTotal;
    }

    public List<InwardProfessionalPaymentAdmissionGroupDTO> getProfessionalPaymentReportGroups() {
        return professionalPaymentReportGroups;
    }

    public void setProfessionalPaymentReportGroups(List<InwardProfessionalPaymentAdmissionGroupDTO> professionalPaymentReportGroups) {
        this.professionalPaymentReportGroups = professionalPaymentReportGroups;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = new Date();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public String getBhtNoFilter() {
        return bhtNoFilter;
    }

    public void setBhtNoFilter(String bhtNoFilter) {
        this.bhtNoFilter = bhtNoFilter;
    }

    public String getPatientNameFilter() {
        return patientNameFilter;
    }

    public void setPatientNameFilter(String patientNameFilter) {
        this.patientNameFilter = patientNameFilter;
    }

    public String getProfessionalPaymentReportType() {
        return professionalPaymentReportType;
    }

    public void setProfessionalPaymentReportType(String professionalPaymentReportType) {
        this.professionalPaymentReportType = professionalPaymentReportType;
    }

    public String getProfessionalPaymentSearchMode() {
        return professionalPaymentSearchMode;
    }

    public void setProfessionalPaymentSearchMode(String professionalPaymentSearchMode) {
        this.professionalPaymentSearchMode = professionalPaymentSearchMode;
    }

    public PatientEncounter getAdmissionFromForProfessionalPaymentReport() {
        return admissionFromForProfessionalPaymentReport;
    }

    public void setAdmissionFromForProfessionalPaymentReport(PatientEncounter admissionFromForProfessionalPaymentReport) {
        this.admissionFromForProfessionalPaymentReport = admissionFromForProfessionalPaymentReport;
    }

    public PatientEncounter getAdmissionToForProfessionalPaymentReport() {
        return admissionToForProfessionalPaymentReport;
    }

    public void setAdmissionToForProfessionalPaymentReport(PatientEncounter admissionToForProfessionalPaymentReport) {
        this.admissionToForProfessionalPaymentReport = admissionToForProfessionalPaymentReport;
    }

    public boolean isOnlyAdmissionsWithoutProfessionalFees() {
        return onlyAdmissionsWithoutProfessionalFees;
    }

    public void setOnlyAdmissionsWithoutProfessionalFees(boolean onlyAdmissionsWithoutProfessionalFees) {
        this.onlyAdmissionsWithoutProfessionalFees = onlyAdmissionsWithoutProfessionalFees;
    }
}
