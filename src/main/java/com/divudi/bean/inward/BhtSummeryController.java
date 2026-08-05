/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.BillController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ConfigOptionController;
import com.divudi.bean.common.EnumController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;

import com.divudi.bean.common.WebUserController;
import com.divudi.bean.membership.MembershipSchemeController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.ChargeItemTotal;
import com.divudi.core.data.dataStructure.DepartmentBillItems;
import com.divudi.core.data.dataStructure.InwardBillItem;
import com.divudi.core.data.inward.AdmissionTypeEnum;
import com.divudi.core.data.inward.InwardChargeType;
import static com.divudi.core.data.inward.InwardChargeType.RoomCharges;
import com.divudi.ejb.BillNumberGenerator;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.PatientItem;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.GuardianRoom;
import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.entity.inward.PatientRoomTimedItemCharge;
import com.divudi.core.entity.inward.RoomCategory;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.entity.inward.TheatreRoom;
import com.divudi.core.entity.inward.TimedItem;
import com.divudi.core.entity.inward.TimedItemFee;
import com.divudi.core.entity.membership.InwardMemberShipDiscount;
import com.divudi.core.facade.AdmissionTypeFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PatientItemFacade;
import com.divudi.core.data.inward.TheatreTransferType;
import com.divudi.core.entity.inward.PatientTransferRequest;
import com.divudi.core.facade.PatientRoomFacade;
import com.divudi.core.facade.PatientRoomTimedItemChargeFacade;
import com.divudi.core.facade.PatientTransferRequestFacade;
import com.divudi.core.facade.ServiceFacade;
import com.divudi.core.facade.TimedItemFeeFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.dataStructure.CreditCompanyAllocation;
import com.divudi.core.entity.EncounterCreditCompany;
import com.divudi.core.entity.Staff;
import com.divudi.core.facade.EncounterCreditCompanyFacade;
import com.divudi.core.util.CommonFunctions;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.primefaces.PrimeFaces;
import org.primefaces.event.ReorderEvent;
import org.primefaces.event.RowEditEvent;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class BhtSummeryController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private PatientRoomFacade patientRoomFacade;
    @EJB
    private PatientRoomTimedItemChargeFacade patientRoomTimedItemChargeFacade;
    @EJB
    private PatientTransferRequestFacade patientTransferRequestFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private ServiceFacade serviceFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PatientItemFacade patientItemFacade;
    @EJB
    private TimedItemFeeFacade timedItemFeeFacade;
    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    AdmissionTypeFacade admissionTypeFacade;
    @EJB
    EncounterCreditCompanyFacade encounterCreditCompanyFacade;
    ////////////////////////////

    @EJB
    private BillNumberGenerator billNumberBean;
    @Inject
    WebUserController webUserController;
    @Inject
    PriceMatrixController priceMatrixController;
    //////////////////////////
    @EJB
    private com.divudi.service.AuditService auditService;
    @Inject
    private SessionController sessionController;
    @Inject
    private InwardTimedItemController inwardTimedItemController;
    @Inject
    private DischargeController dischargeController;
    @Inject
    BillController billController;
    @Inject
    RoomChangeController roomChangeController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    ConfigOptionController configOptionController;
    @Inject
    AdmissionController admissionController;
    @Inject
    InwardPaymentController inwardPaymentController;
    ////////////////////////
    private List<DepartmentBillItems> departmentBillItems;
    private Map<Long, BillItem> latestCheckedBillItemsByItem;
    private List<BillFee> profesionallFee;
    private List<BillFee> doctorAndNurseFee;
    private List<BillFee> allDoctorCharges;
    // Holds the doctor whose fee breakdown is shown in the "how the total is calculated" popup.
    private DoctorFeeGroup selectedDoctorFeeGroup;
    List<BillItem> pharmacyItems;
    private List<Bill> paymentBill;
    private List<Bill> postFinalPaymentBill;
    private List<Bill> pharmacyIssues;

    //Groping Medicine by Issueing Department
    private List<Bill> etuMedicineIssues;
    private List<Bill> pharmacyMedicineIssues;
    private List<Bill> inwardMedicineIssues;
    private List<Bill> theatreMedicineIssues;
    private List<Bill> storeMedicineIssues;
    private List<Bill> inventryMedicineIssues;

    List<Bill> storeIssues;
    private List<Bill> surgeryBills;
    private Bill surgeryBill;
    List<PatientItem> patientItems;
    private List<ChargeItemTotal> chargeItemTotals;
    List<PatientRoom> patientRooms;
    private PatientRoom pendingOverlapRoom;
    private List<CreditCompanyAllocation> creditCompanyAllocations;
    private EncounterCreditCompany newEncounterCreditCompany;
    private boolean creatingNewVersion;
    //////////////////////////
    private double grantTotal = 0.0;
    private double discount;
    private double billLevelDiscount = 0.0;
    private double itemDiscountTotal = 0.0;
    private double chargeTypeDiscountTotal = 0.0;
    private double due;
    private double paid;
    private double paidByPatient;
    private double paidByCompany;
    private PatientItem tmpPI;
    private PatientEncounter patientEncounter;
    private Bill current;
    private Bill originalBill;
    private Bill tempBill;
    private Date currentTime;
    private Date toTime;
    Date fromDate;
    Date toDate;
    private Date date;
    private boolean printPreview;
    //////////////////////////
    // Custom2 (Custom Bills tab) print-format settings
    private boolean custom2ShowAddress;
    private boolean custom2ShowNic;
    private boolean custom2ShowPhone;
    private boolean custom2ShowGuardian;
    private boolean custom2ShowCorporateSponsor;
    // Custom4 (Custom Bills tab - letterhead) print-format settings
    private boolean custom4ShowAddress;
    private boolean custom4ShowNic;
    private boolean custom4ShowPhone;
    private boolean custom4ShowGuardian;
    private boolean custom4ShowCorporateSponsor;
    @Inject
    private InwardMemberShipDiscount inwardMemberShipDiscount;
    @Inject
    BillBhtController billBhtController;
    private Item item;
    boolean changed = false;
    boolean showOrginalBill = false;
    private String duration;
    private boolean patientEncounterHasProvisionalBill = false;
    private List<PatientEncounter> childPatientEncouters;
    private Institution institution;
    private boolean estimatedBillView = false;

    public String navigateToIntrimBillEstimate() {
        institution = sessionController.getInstitution();
        createTablesWithEstimatedProfessionalFees();
        return "/inward/inward_bill_intrim_estimate?faces-redirect=true";
    }

    public String navigateToPatientRoomDetails() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No Admission Selected");
            return "";
        }
        createTables();
        return "/inward/inward_patient_room_details?faces-redirect=true";
    }

    public List<RoomGanttBar> getRoomGanttBars() {
        List<PatientRoom> rooms = getPatientRooms();
        if (rooms == null || rooms.isEmpty()) {
            return new ArrayList<>();
        }
        Date now = new Date();
        Date spanStart = null;
        Date spanEnd = null;
        for (PatientRoom r : rooms) {
            if (r.getAdmittedAt() != null) {
                if (spanStart == null || r.getAdmittedAt().before(spanStart)) {
                    spanStart = r.getAdmittedAt();
                }
            }
            Date end = r.getDischargedAt() != null ? r.getDischargedAt() : now;
            if (spanEnd == null || end.after(spanEnd)) {
                spanEnd = end;
            }
        }
        if (spanStart == null || spanEnd == null || !spanEnd.after(spanStart)) {
            return new ArrayList<>();
        }
        long totalMs = spanEnd.getTime() - spanStart.getTime();
        List<RoomGanttBar> bars = new ArrayList<>();
        for (PatientRoom r : rooms) {
            if (r.getAdmittedAt() == null) {
                continue;
            }
            if (r instanceof TheatreRoom) {
                continue;
            }
            Date barEnd = r.getDischargedAt() != null ? r.getDischargedAt() : now;
            long offsetMs = r.getAdmittedAt().getTime() - spanStart.getTime();
            long durationMs = barEnd.getTime() - r.getAdmittedAt().getTime();
            double rawOffset = (offsetMs * 100.0) / totalMs;
            double rawWidth = Math.max((durationMs * 100.0) / totalMs, 1.0);
            boolean guardian = "class com.divudi.core.entity.inward.GuardianRoom"
                    .equals(r.getPatientRoomClass());
            bars.add(new RoomGanttBar(
                    r.getRoomFacilityCharge() != null ? r.getRoomFacilityCharge().getName() : "—",
                    guardian,
                    !r.isDischarged(),
                    String.format(java.util.Locale.US, "%.3f", rawOffset),
                    String.format(java.util.Locale.US, "%.3f", rawWidth),
                    rawWidth > 8.0
            ));
        }
        return bars;
    }

    /**
     * Gantt bars merging ward room stays and theatre visits for the unified
     * patient timeline. Theatre bars are amber (active) or grey (completed).
     */
    private transient List<RoomGanttBar> cachedUnifiedGanttBars;
    private transient long cachedUnifiedGanttBarsComputedAtMillis;
    private static final long UNIFIED_GANTT_BARS_CACHE_TTL_MILLIS = 5000;

    /**
     * Clears the Gantt bar cache immediately. The 5s TTL above is a safety net
     * for the wall-clock "Now" marker, which has no explicit save event to hook
     * into - but any action that actually persists a room's
     * admitted/discharged/retired state must call this so the same AJAX
     * response reflects the change instead of waiting out the TTL.
     */
    public void invalidateUnifiedGanttBarsCache() {
        cachedUnifiedGanttBars = null;
        cachedUnifiedGanttBarsComputedAtMillis = 0;
    }

    public List<RoomGanttBar> getUnifiedGanttBars() {
        if (cachedUnifiedGanttBars != null
                && (System.currentTimeMillis() - cachedUnifiedGanttBarsComputedAtMillis) < UNIFIED_GANTT_BARS_CACHE_TTL_MILLIS) {
            return cachedUnifiedGanttBars;
        }
        List<PatientRoom> rooms = getPatientRooms();
        List<PatientTransferRequest> theatreRequests = loadTheatreRequestsForTimeline();

        if ((rooms == null || rooms.isEmpty()) && theatreRequests.isEmpty()) {
            return new ArrayList<>();
        }
        if (rooms == null) {
            rooms = new ArrayList<>();
        }

        Date now = new Date();
        Date spanStart = null;
        Date spanEnd = null;

        for (PatientRoom r : rooms) {
            if (r.getAdmittedAt() != null) {
                if (spanStart == null || r.getAdmittedAt().before(spanStart)) {
                    spanStart = r.getAdmittedAt();
                }
            }
            Date end = r.getDischargedAt() != null ? r.getDischargedAt() : now;
            if (spanEnd == null || end.after(spanEnd)) {
                spanEnd = end;
            }
        }

        for (PatientTransferRequest req : theatreRequests) {
            if (req.getInitiatedAt() == null) {
                continue;
            }
            Date end = req.getReturnedToWardAt() != null ? req.getReturnedToWardAt() : now;
            if (spanStart == null || req.getInitiatedAt().before(spanStart)) {
                spanStart = req.getInitiatedAt();
            }
            if (spanEnd == null || end.after(spanEnd)) {
                spanEnd = end;
            }
        }

        if (spanStart == null || spanEnd == null || !spanEnd.after(spanStart)) {
            cachedUnifiedGanttBars = getRoomGanttBars();
            cachedUnifiedGanttBarsComputedAtMillis = System.currentTimeMillis();
            return cachedUnifiedGanttBars;
        }

        long totalMs = spanEnd.getTime() - spanStart.getTime();
        List<RoomGanttBar> bars = new ArrayList<>();

        // Ward room bars (same colour logic as getRoomGanttBars)
        for (PatientRoom r : rooms) {
            if (r.getAdmittedAt() == null) {
                continue;
            }
            if (r instanceof TheatreRoom) {
                continue;
            }
            Date barEnd = r.getDischargedAt() != null ? r.getDischargedAt() : now;
            long offsetMs = r.getAdmittedAt().getTime() - spanStart.getTime();
            long durationMs = barEnd.getTime() - r.getAdmittedAt().getTime();
            double rawOffset = (offsetMs * 100.0) / totalMs;
            double rawWidth = Math.max((durationMs * 100.0) / totalMs, 1.0);
            boolean guardian = "class com.divudi.core.entity.inward.GuardianRoom"
                    .equals(r.getPatientRoomClass());
            bars.add(new RoomGanttBar(
                    r.getRoomFacilityCharge() != null ? r.getRoomFacilityCharge().getName() : "—",
                    guardian,
                    !r.isDischarged(),
                    String.format(java.util.Locale.US, "%.3f", rawOffset),
                    String.format(java.util.Locale.US, "%.3f", rawWidth),
                    rawWidth > 8.0
            ));
        }

        // Theatre visit bars — amber while active, grey when returned
        for (PatientTransferRequest req : theatreRequests) {
            if (req.getInitiatedAt() == null) {
                continue;
            }
            Date barEnd = req.getReturnedToWardAt() != null ? req.getReturnedToWardAt() : now;
            boolean active = req.getReturnedToWardAt() == null;
            long offsetMs = req.getInitiatedAt().getTime() - spanStart.getTime();
            long durationMs = barEnd.getTime() - req.getInitiatedAt().getTime();
            double rawOffset = (offsetMs * 100.0) / totalMs;
            double rawWidth = Math.max((durationMs * 100.0) / totalMs, 1.0);
            String theatreName = req.getToRoomFacilityCharge() != null
                    ? req.getToRoomFacilityCharge().getName() : "Theatre";
            String barColor = active
                    ? "linear-gradient(90deg,#fd7e14,#ffc107)"
                    : "linear-gradient(90deg,#adb5bd,#ced4da)";
            bars.add(new RoomGanttBar(
                    theatreName,
                    barColor,
                    String.format(java.util.Locale.US, "%.3f", rawOffset),
                    String.format(java.util.Locale.US, "%.3f", rawWidth),
                    rawWidth > 8.0
            ));
        }

        cachedUnifiedGanttBars = bars;
        cachedUnifiedGanttBarsComputedAtMillis = System.currentTimeMillis();
        return cachedUnifiedGanttBars;
    }

    private List<PatientTransferRequest> loadTheatreRequestsForTimeline() {
        if (!(patientEncounter instanceof Admission)) {
            return new ArrayList<>();
        }
        HashMap<String, Object> params = new HashMap<>();
        params.put("admission", patientEncounter);
        params.put("type", TheatreTransferType.SEND_TO_THEATRE);
        String jpql = "SELECT r FROM PatientTransferRequest r "
                + "WHERE r.admission = :admission "
                + "AND r.theatreTransferType = :type "
                + "AND r.retired = false "
                + "ORDER BY r.initiatedAt";
        List<PatientTransferRequest> result = patientTransferRequestFacade.findByJpql(jpql, params);
        return result != null ? result : new ArrayList<>();
    }

    public String navigateToInpatientProfile() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No Admission Selected");
            return "";
        }
        fillSurgeryBills();
        return "/inward/admission_profile.xhtml?faces-redirect=true";
    }

    private void fillSurgeryBills() {
        surgeryBills = billController.fillPatientSurgeryBills(patientEncounter);
        if (surgeryBills == null || surgeryBills.isEmpty()) {
            surgeryBill = null;
        } else {
            surgeryBill = surgeryBills.get(0);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Custom Bills tab - Custom2 print format">
    public void loadCustom2Config() {
        custom2ShowAddress = configOptionController.getBooleanValueByKey("Inward Final Bill Custom2 - Show Patient Address", false);
        custom2ShowNic = configOptionController.getBooleanValueByKey("Inward Final Bill Custom2 - Show Patient NIC", false);
        custom2ShowPhone = configOptionController.getBooleanValueByKey("Inward Final Bill Custom2 - Show Patient Phone", false);
        custom2ShowGuardian = configOptionController.getBooleanValueByKey("Inward Final Bill Custom2 - Show Guardian", true);
        custom2ShowCorporateSponsor = configOptionController.getBooleanValueByKey("Inward Final Bill Custom2 - Show Corporate Sponsor", true);
        // Custom Bills tab shares one settings dialog across Custom2 and Custom4 (letterhead)
        loadCustom4Config();
    }

    public void saveCustom2Config() {
        if (!webUserController.hasPrivilege("ChangeReceiptPrintingPaperTypes")) {
            JsfUtil.addErrorMessage("You do not have privilege to change Custom Bills configuration");
            return;
        }
        try {
            configOptionController.setBooleanValueByKey("Inward Final Bill Custom2 - Show Patient Address", custom2ShowAddress);
            configOptionController.setBooleanValueByKey("Inward Final Bill Custom2 - Show Patient NIC", custom2ShowNic);
            configOptionController.setBooleanValueByKey("Inward Final Bill Custom2 - Show Patient Phone", custom2ShowPhone);
            configOptionController.setBooleanValueByKey("Inward Final Bill Custom2 - Show Guardian", custom2ShowGuardian);
            configOptionController.setBooleanValueByKey("Inward Final Bill Custom2 - Show Corporate Sponsor", custom2ShowCorporateSponsor);
            // Custom Bills tab shares one settings dialog across Custom2 and Custom4 (letterhead)
            persistCustom4Config();
            JsfUtil.addSuccessMessage("Custom Bills configuration saved successfully");
            loadCustom2Config();
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving Custom Bills configuration: " + e.getMessage());
        }
    }

    /**
     * Charges grouped by inward charge type (excluding Professional Charge,
     * which is printed separately on the Professional Bill section),
     * alphabetical by display name, for the Custom2 "Final Bill" totals-only
     * section.
     */
    public List<Map.Entry<String, Double>> getCustom2CategoryTotals(Bill bill) {
        Map<String, Double> totals = new TreeMap<>();
        if (bill == null || bill.getBillItems() == null) {
            return new ArrayList<>(totals.entrySet());
        }
        for (BillItem bi : bill.getBillItems()) {
            if (bi.getInwardChargeType() == InwardChargeType.ProfessionalCharge) {
                continue;
            }
            if (bi.getAdjustedValue() == 0.0) {
                continue;
            }
            String label = getChargeTypeLabel(bi.getInwardChargeType());
            totals.merge(label, bi.getAdjustedValue(), Double::sum);
        }
        return new ArrayList<>(totals.entrySet());
    }

    /**
     * Charges grouped by inward charge type (excluding Professional Charge,
     * which is now listed as individual per-doctor fee lines), alphabetical
     * by display name, for the Custom3 5x5 impact-printer bill ("Custom Bill
     * 2" in the UI).
     */
    public List<Map.Entry<String, Double>> getCustom3CategoryTotals(Bill bill) {
        Map<String, Double> totals = new TreeMap<>();
        if (bill == null || bill.getBillItems() == null) {
            return new ArrayList<>(totals.entrySet());
        }
        for (BillItem bi : bill.getBillItems()) {
            if (bi.getInwardChargeType() == InwardChargeType.ProfessionalCharge) {
                continue;
            }
            if (bi.getAdjustedValue() == 0.0) {
                continue;
            }
            String label = getChargeTypeLabel(bi.getInwardChargeType());
            totals.merge(label, bi.getAdjustedValue(), Double::sum);
        }
        return new ArrayList<>(totals.entrySet());
    }

    /**
     * Sum of prior payments/receipts recorded against this admission — the
     * Custom3 bill's "Deposit" line. Same backwardReferenceBills source and
     * qualifying filter as the Custom2 receipts table (finalBillCustom2.xhtml
     * lines 280-291), just summed instead of rendered row by row.
     */
    public double getCustom3DepositTotal(Bill bill) {
        if (bill == null) {
            return 0.0;
        }
        List<Bill> receipts = (bill.getPatientEncounter() != null && bill.getPatientEncounter().getFinalBill() != null)
                ? bill.getPatientEncounter().getFinalBill().getBackwardReferenceBills()
                : bill.getBackwardReferenceBills();
        double total = 0.0;
        if (receipts == null) {
            return total;
        }
        for (Bill b : receipts) {
            if (b.getNetTotal() == 0.0) {
                continue;
            }
            boolean qualifies = (!b.isCancelled() && "class com.divudi.core.entity.BilledBill".equals(b.getBillClass()))
                    || (!b.isCancelled() && b.getRefundedBill() == null && "class com.divudi.core.entity.RefundBill".equals(b.getBillClass()));
            if (qualifies) {
                total += b.getNetTotal();
            }
        }
        return total;
    }

    public boolean isCustom2ShowAddress() {
        return custom2ShowAddress;
    }

    public void setCustom2ShowAddress(boolean custom2ShowAddress) {
        this.custom2ShowAddress = custom2ShowAddress;
    }

    public boolean isCustom2ShowNic() {
        return custom2ShowNic;
    }

    public void setCustom2ShowNic(boolean custom2ShowNic) {
        this.custom2ShowNic = custom2ShowNic;
    }

    public boolean isCustom2ShowPhone() {
        return custom2ShowPhone;
    }

    public void setCustom2ShowPhone(boolean custom2ShowPhone) {
        this.custom2ShowPhone = custom2ShowPhone;
    }

    public boolean isCustom2ShowGuardian() {
        return custom2ShowGuardian;
    }

    public void setCustom2ShowGuardian(boolean custom2ShowGuardian) {
        this.custom2ShowGuardian = custom2ShowGuardian;
    }

    public boolean isCustom2ShowCorporateSponsor() {
        return custom2ShowCorporateSponsor;
    }

    public void setCustom2ShowCorporateSponsor(boolean custom2ShowCorporateSponsor) {
        this.custom2ShowCorporateSponsor = custom2ShowCorporateSponsor;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Custom Bills tab - Custom4 print format (letterhead)">
    public void loadCustom4Config() {
        custom4ShowAddress = configOptionController.getBooleanValueByKey("Inward Final Bill Custom4 - Show Patient Address", false);
        custom4ShowNic = configOptionController.getBooleanValueByKey("Inward Final Bill Custom4 - Show Patient NIC", false);
        custom4ShowPhone = configOptionController.getBooleanValueByKey("Inward Final Bill Custom4 - Show Patient Phone", false);
        custom4ShowGuardian = configOptionController.getBooleanValueByKey("Inward Final Bill Custom4 - Show Guardian", true);
        custom4ShowCorporateSponsor = configOptionController.getBooleanValueByKey("Inward Final Bill Custom4 - Show Corporate Sponsor", true);
    }

    public void saveCustom4Config() {
        if (!webUserController.hasPrivilege("ChangeReceiptPrintingPaperTypes")) {
            JsfUtil.addErrorMessage("You do not have privilege to change Custom Bills configuration");
            return;
        }
        try {
            persistCustom4Config();
            JsfUtil.addSuccessMessage("Custom Bills configuration saved successfully");
            loadCustom4Config();
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving Custom Bills configuration: " + e.getMessage());
        }
    }

    private void persistCustom4Config() {
        configOptionController.setBooleanValueByKey("Inward Final Bill Custom4 - Show Patient Address", custom4ShowAddress);
        configOptionController.setBooleanValueByKey("Inward Final Bill Custom4 - Show Patient NIC", custom4ShowNic);
        configOptionController.setBooleanValueByKey("Inward Final Bill Custom4 - Show Patient Phone", custom4ShowPhone);
        configOptionController.setBooleanValueByKey("Inward Final Bill Custom4 - Show Guardian", custom4ShowGuardian);
        configOptionController.setBooleanValueByKey("Inward Final Bill Custom4 - Show Corporate Sponsor", custom4ShowCorporateSponsor);
    }

    public boolean isCustom4ShowAddress() {
        return custom4ShowAddress;
    }

    public void setCustom4ShowAddress(boolean custom4ShowAddress) {
        this.custom4ShowAddress = custom4ShowAddress;
    }

    public boolean isCustom4ShowNic() {
        return custom4ShowNic;
    }

    public void setCustom4ShowNic(boolean custom4ShowNic) {
        this.custom4ShowNic = custom4ShowNic;
    }

    public boolean isCustom4ShowPhone() {
        return custom4ShowPhone;
    }

    public void setCustom4ShowPhone(boolean custom4ShowPhone) {
        this.custom4ShowPhone = custom4ShowPhone;
    }

    public boolean isCustom4ShowGuardian() {
        return custom4ShowGuardian;
    }

    public void setCustom4ShowGuardian(boolean custom4ShowGuardian) {
        this.custom4ShowGuardian = custom4ShowGuardian;
    }

    public boolean isCustom4ShowCorporateSponsor() {
        return custom4ShowCorporateSponsor;
    }

    public void setCustom4ShowCorporateSponsor(boolean custom4ShowCorporateSponsor) {
        this.custom4ShowCorporateSponsor = custom4ShowCorporateSponsor;
    }
    // </editor-fold>

    public String navigateToAddServiceFromSurgeriesFromAdmissionProfile() {
        if (surgeryBills == null) {
            JsfUtil.addErrorMessage("No Surgeries added yet");
            return null;
        }
        if (surgeryBills.isEmpty()) {
            JsfUtil.addErrorMessage("No Surgeries added yet");
            return null;
        }
        if (surgeryBill == null) {
            surgeryBill = surgeryBills.get(0);
        }
        billBhtController.resetBillData();
        billBhtController.setBills(surgeryBills);
        billBhtController.setBatchBill(surgeryBill);
        return "/theater/inward_bill_surgery_service";
    }

    public List<PatientRoom> getPatientRooms() {
        if (patientRooms == null) {
            patientRooms = createPatientRooms();
        }
        return patientRooms;
    }

    public void setPatientRooms(List<PatientRoom> patientRooms) {
        this.patientRooms = patientRooms;
    }

    public List<BillFee> getDoctorAndNurseFee() {
        if (doctorAndNurseFee == null) {
            List<PatientEncounter> cpts = getInwardBean().fetchChildPatientEncounter(getPatientEncounter());
            doctorAndNurseFee = getInwardBean().createDoctorAndNurseFee(getPatientEncounter(), cpts);
        }
        return doctorAndNurseFee;
    }

    public void setDoctorAndNurseFee(List<BillFee> doctorAndNurseFee) {
        this.doctorAndNurseFee = doctorAndNurseFee;
    }

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public void setPriceMatrixController(PriceMatrixController priceMatrixController) {
        this.priceMatrixController = priceMatrixController;
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
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    /**
     * Snapshot of a room's per-charge discount values for audit events
     * (#22238).
     */
    private Map<String, Object> roomDiscountAuditMap(PatientRoom pr) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (pr == null) {
            return m;
        }
        m.put("room", pr.getRoomFacilityCharge() != null ? pr.getRoomFacilityCharge().getName() : null);
        m.put("discountRoomCharge", pr.getDiscountRoomCharge());
        m.put("discountMaintainCharge", pr.getDiscountMaintainCharge());
        m.put("discountLinenCharge", pr.getDiscountLinenCharge());
        m.put("discountMedicalCareCharge", pr.getDiscountMedicalCareCharge());
        m.put("discountAdministrationCharge", pr.getDiscountAdministrationCharge());
        m.put("discountNursingCharge", pr.getDiscountNursingCharge());
        m.put("discountMoCharge", pr.getDiscountMoCharge());
        return m;
    }

    private void auditRoomDiscountChange(Map<String, Object> before, PatientRoom pr) {
        auditService.logEncounterAudit(getPatientEncounter(), "Room Discount Changed",
                before, roomDiscountAuditMap(pr), sessionController.getLoggedUser(),
                "PatientRoom", pr.getId());
    }

    public void changeDiscountListener(ChargeItemTotal cit) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("chargeType", cit.getInwardChargeType());
        before.put("total", cit.getTotal());
        before.put("itemDiscount", cit.getDiscount());
        before.put("requestedChargeTypeDiscount", cit.getChargeTypeDiscount());
        if (cit.getChargeTypeDiscount() < 0) {
            cit.setChargeTypeDiscount(0);
            JsfUtil.addErrorMessage("Charge type discount cannot be negative");
        }
        double maxAllowed = cit.getTotal() - cit.getDiscount();
        if (cit.getChargeTypeDiscount() > maxAllowed) {
            cit.setChargeTypeDiscount(0);
            JsfUtil.addErrorMessage("Charge type discount cannot exceed the remaining net for this charge type");
        }
        updateTotal();
        rebuildCreditCompanyAllocations();

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("chargeType", cit.getInwardChargeType());
        after.put("total", cit.getTotal());
        after.put("itemDiscount", cit.getDiscount());
        after.put("appliedChargeTypeDiscount", cit.getChargeTypeDiscount());
        after.put("adjustedTotal", cit.getAdjustedTotal());
        auditService.logEncounterAudit(getPatientEncounter(), "Settlement Discount Changed",
                before, after, sessionController.getLoggedUser());
    }

    public void changeDiscountListenerPatientRoomRoomCharge(PatientRoom pR) {
        Map<String, Object> before = roomDiscountAuditMap(getPatientRoomFacade().findWithoutCache(pR.getId()));
        getPatientRoomFacade().edit(pR);
        double disCountPercent = 0;

        //Room Charge
        if (pR.getDiscountRoomCharge() != 0 && pR.getCalculatedRoomCharge() != 0) {
            disCountPercent = (pR.getDiscountRoomCharge() * 100) / pR.getCalculatedRoomCharge();
            updatePatientRoomCharge(pR, disCountPercent);
        }

        updateRoomChargeTypeTotal();
        auditRoomDiscountChange(before, pR);

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
    }

    public void changeDiscountListenerPatientRoomMaintain(PatientRoom pR) {
        Map<String, Object> before = roomDiscountAuditMap(getPatientRoomFacade().findWithoutCache(pR.getId()));
        getPatientRoomFacade().edit(pR);
        double disCountPercent = 0;

        //Maintain
        if (pR.getDiscountMaintainCharge() != 0 && pR.getCalculatedMaintainCharge() != 0) {
            disCountPercent = (pR.getDiscountMaintainCharge() * 100) / pR.getCalculatedMaintainCharge();
            updatePatientMaintainCharge(pR, disCountPercent);
        }

        updateRoomChargeTypeTotal();
        auditRoomDiscountChange(before, pR);

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
    }

    public void changeDiscountListenerPatientRoomLinen(PatientRoom pR) {
        Map<String, Object> before = roomDiscountAuditMap(getPatientRoomFacade().findWithoutCache(pR.getId()));
        getPatientRoomFacade().edit(pR);
        double disCountPercent = 0;

        //Linen
        if (pR.getDiscountLinenCharge() != 0 && pR.getCalculatedLinenCharge() != 0) {
            disCountPercent = (pR.getDiscountLinenCharge() * 100) / pR.getCalculatedLinenCharge();
            updatePatientLinenCharge(pR, disCountPercent);
        }

        updateRoomChargeTypeTotal();
        auditRoomDiscountChange(before, pR);

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
    }

    public void changeDiscountListenerPatientRoomMedicalCare(PatientRoom pR) {
        Map<String, Object> before = roomDiscountAuditMap(getPatientRoomFacade().findWithoutCache(pR.getId()));
        getPatientRoomFacade().edit(pR);
        double disCountPercent = 0;

        //Medical Care Charge
        if (pR.getDiscountMedicalCareCharge() != 0 && pR.getCalculatedMedicalCareCharge() != 0) {
            disCountPercent = (pR.getDiscountMedicalCareCharge() * 100) / pR.getCalculatedMedicalCareCharge();
            updatePatientMedicalCareIcuCharge(pR, disCountPercent);
        }

        updateRoomChargeTypeTotal();
        auditRoomDiscountChange(before, pR);

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
    }

    public void changeDiscountListenerPatientRoomAdministration(PatientRoom pR) {
        Map<String, Object> before = roomDiscountAuditMap(getPatientRoomFacade().findWithoutCache(pR.getId()));
        getPatientRoomFacade().edit(pR);
        double disCountPercent = 0;
        //Administration Charge
        if (pR.getDiscountAdministrationCharge() != 0 && pR.getCalculatedAdministrationCharge() != 0) {
            disCountPercent = (pR.getDiscountAdministrationCharge() * 100) / pR.getCalculatedAdministrationCharge();
            updatePatientAdministrationCharge(pR, disCountPercent);
        }

        updateRoomChargeTypeTotal();
        auditRoomDiscountChange(before, pR);

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
    }

    public void changeDiscountListenerPatientRoomNursing(PatientRoom pR) {
        Map<String, Object> before = roomDiscountAuditMap(getPatientRoomFacade().findWithoutCache(pR.getId()));
        getPatientRoomFacade().edit(pR);
        double disCountPercent = 0;

        //Nursing
        if (pR.getDiscountNursingCharge() != 0 && pR.getCalculatedNursingCharge() != 0) {
            disCountPercent = (pR.getDiscountNursingCharge() * 100) / pR.getCalculatedNursingCharge();
            updatePatientNursingCharge(pR, disCountPercent);
        }

        updateRoomChargeTypeTotal();
        auditRoomDiscountChange(before, pR);

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
    }

    public void changeDiscountListenerPatientRoomMo(PatientRoom pR) {
        Map<String, Object> before = roomDiscountAuditMap(getPatientRoomFacade().findWithoutCache(pR.getId()));
        getPatientRoomFacade().edit(pR);
        double disCountPercent = 0;

        //MO
        if (pR.getDiscountMoCharge() != 0 && pR.getCalculatedMoCharge() != 0) {
            disCountPercent = (pR.getDiscountMoCharge() * 100) / pR.getCalculatedMoCharge();
            updatePatientMoCharge(pR, disCountPercent);
        }

        updateRoomChargeTypeTotal();
        auditRoomDiscountChange(before, pR);

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
    }

    private void updateRoomChargeTypeTotal() {
        for (ChargeItemTotal chargeItemTotal : chargeItemTotals) {
            switch (chargeItemTotal.getInwardChargeType()) {
                case AdministrationCharge:
                    chargeItemTotal.setDiscount(getInwardBean().calPatientRoomAdminChargeDiscount(getPatientEncounter()));
                    //    chargeItemTotal.setAdjustedTotal(chargeItemTotal.getTotal());
                    break;
                case MedicalCareICU:
                    chargeItemTotal.setDiscount(getInwardBean().calPatientRoomMadicalCareChargeDiscount(getPatientEncounter()));
                    //chargeItemTotal.setAdjustedTotal(chargeItemTotal.getTotal());
                    break;
                case LinenCharges:
                    chargeItemTotal.setDiscount(getInwardBean().calPatientRoomLinenChargeDiscount(getPatientEncounter()));
                    ///   chargeItemTotal.setAdjustedTotal(chargeItemTotal.getTotal());
                    break;
                case RoomCharges:
                    chargeItemTotal.setDiscount(getInwardBean().calPatientRoomChargeDiscount(getPatientEncounter()));
                    //   chargeItemTotal.setAdjustedTotal(chargeItemTotal.getTotal());
                    break;
                case MOCharges:
                    chargeItemTotal.setDiscount(getInwardBean().calPatientMoChargeDiscount(getPatientEncounter()));
                    // chargeItemTotal.setAdjustedTotal(chargeItemTotal.getTotal());
                    break;
                case MaintainCharges:
                    chargeItemTotal.setDiscount(getInwardBean().calPatientMaintananceChargeDiscount(getPatientEncounter()));
                    ///   chargeItemTotal.setAdjustedTotal(chargeItemTotal.getTotal());
                    break;
                case NursingCharges:
                    chargeItemTotal.setDiscount(getInwardBean().calPatientNursingChargeDiscount(getPatientEncounter()));
                    //  chargeItemTotal.setAdjustedTotal(chargeItemTotal.getTotal());
                    break;
            }

        }

        updateTotal();
    }

    public void changeAdjustedProValue(BillFee billFee) {
        getBillFeeFacade().edit(billFee);
        for (ChargeItemTotal chargeItemTotal : chargeItemTotals) {
            switch (chargeItemTotal.getInwardChargeType()) {
                case ProfessionalCharge:
                    chargeItemTotal.setAdjustedTotal(getInwardBean().getProfessionalCharge(getPatientEncounter(), childPatientEncouters));
                    break;
            }
        }

        updateTotal();
    }

    /**
     * Handles drag-and-drop reordering of the doctor rows in the grouped
     * Professional Fee table. The grouped list is rebuilt fresh on each render
     * (so PrimeFaces' in-place reorder would be lost); instead we take the
     * displayed order, apply the move via the event's from/to indices, and
     * write a sequential orderNo onto every underlying fee. The next render
     * re-groups ordered by orderNo, and the printed bill reproduces it via
     * &#64;OrderBy("orderNo, feeAdjusted").
     */
    public void onGroupedProfessionalFeeReorder(ReorderEvent event) {
        List<DoctorFeeGroup> groups = getGroupedProfessionalFees();
        int from = event.getFromIndex();
        int to = event.getToIndex();
        if (from < 0 || to < 0 || from >= groups.size() || to >= groups.size()) {
            return;
        }
        DoctorFeeGroup moved = groups.remove(from);
        groups.add(to, moved);

        int serial = 0;
        for (DoctorFeeGroup grp : groups) {
            for (BillFee bf : grp.getFees()) {
                bf.setOrderNo(serial++);
                getBillFeeFacade().edit(bf);
            }
        }
        JsfUtil.addSuccessMessage("Doctor order updated");
    }

    /**
     * Captures the professional fee list in its current displayed (grouped,
     * doctor-by-doctor) order into orderNo. Called when Save Provisional Bill
     * or Settle is clicked so the saved bill keeps exactly the order shown on
     * screen, which the combined-doctor print preview then reproduces via
     * &#64;OrderBy("orderNo, feeAdjusted").
     */
    private void persistGroupedProfessionalFeeOrder() {
        int serial = 0;
        for (DoctorFeeGroup grp : getGroupedProfessionalFees()) {
            for (BillFee bf : grp.getFees()) {
                bf.setOrderNo(serial++);
                getBillFeeFacade().edit(bf);
            }
        }
    }

    /**
     * Groups the professional fee list by doctor so the final bill shows a
     * single line per doctor with the combined total. Each group keeps its
     * individual fees so the breakdown popup can show how the total was
     * calculated. Only the fees that are actually billed (not cancelled, real
     * billed bill, non-zero amount) are included, matching what the per-fee
     * table displayed.
     */
    public List<DoctorFeeGroup> getGroupedProfessionalFees() {
        Map<Staff, DoctorFeeGroup> groups = new LinkedHashMap<>();
        if (profesionallFee != null) {
            for (BillFee bf : profesionallFee) {
                if (bf.getStaff() == null || bf.getBill() == null) {
                    continue;
                }
                if (bf.getBill().isCancelled() || !(bf.getBill() instanceof BilledBill)) {
                    continue;
                }
                if (bf.getFeeValue() == 0) {
                    continue;
                }
                double adjusted = bf.getFeeAdjusted() != 0 ? bf.getFeeAdjusted() : bf.getFeeValue();
                DoctorFeeGroup group = groups.get(bf.getStaff());
                if (group == null) {
                    group = new DoctorFeeGroup(bf.getStaff());
                    groups.put(bf.getStaff(), group);
                }
                group.getFees().add(bf);
                group.setTotal(group.getTotal() + bf.getFeeValue());
                group.setTotalAdjusted(group.getTotalAdjusted() + adjusted);
            }
        }
        List<DoctorFeeGroup> result = new ArrayList<>(groups.values());
        // Order doctors by the smallest orderNo among their fees so a manual
        // drag-reorder (which writes orderNo) is reproduced. A stable sort keeps
        // first-appearance order when nothing has been reordered (all orderNo 0).
        result.sort(Comparator.comparingInt(grp -> {
            int min = Integer.MAX_VALUE;
            for (BillFee bf : grp.getFees()) {
                min = Math.min(min, bf.getOrderNo());
            }
            return min;
        }));
        return result;
    }

    /**
     * Applies an edited doctor-level Adjusted Value back onto that doctor's
     * individual fees, split in proportion to their current adjusted amounts so
     * the persisted bill and the printout stay consistent. The last fee absorbs
     * any rounding remainder so the parts always sum to the entered total.
     */
    public void changeGroupedAdjustedValue(DoctorFeeGroup group) {
        if (group == null || group.getFees().isEmpty()) {
            return;
        }
        double newTotal = group.getTotalAdjusted();
        List<BillFee> fees = group.getFees();

        double oldSum = 0;
        for (BillFee bf : fees) {
            oldSum += bf.getFeeAdjusted() != 0 ? bf.getFeeAdjusted() : bf.getFeeValue();
        }

        double allocated = 0;
        for (int k = 0; k < fees.size(); k++) {
            BillFee bf = fees.get(k);
            double share;
            if (k == fees.size() - 1) {
                share = newTotal - allocated;
            } else if (oldSum != 0) {
                double base = bf.getFeeAdjusted() != 0 ? bf.getFeeAdjusted() : bf.getFeeValue();
                share = Math.round((newTotal * base / oldSum) * 100.0) / 100.0;
                allocated += share;
            } else {
                share = Math.round((newTotal / fees.size()) * 100.0) / 100.0;
                allocated += share;
            }
            bf.setFeeAdjusted(share);
            getBillFeeFacade().edit(bf);
        }

        refreshProfessionalChargeAdjustedTotal();
        updateTotal();
    }

    /**
     * Refreshes the ProfessionalCharge category adjusted total from the grouped
     * doctor fees the user actually sees. When the config flag merges assisting
     * fees into {@code profesionallFee}, those fees live on a different bill
     * type, so summing only {@code getProfessionalCharge(...)}
     * (InwardProfessional) would silently drop the assisting-fee adjustments
     * from settlement. Summing the grouped totals keeps the category total
     * consistent with the displayed table in both the merged and non-merged
     * configurations.
     */
    private void refreshProfessionalChargeAdjustedTotal() {
        double groupedAdjusted = 0;
        for (DoctorFeeGroup g : getGroupedProfessionalFees()) {
            groupedAdjusted += g.getTotalAdjusted();
        }
        for (ChargeItemTotal chargeItemTotal : chargeItemTotals) {
            if (chargeItemTotal.getInwardChargeType() == InwardChargeType.ProfessionalCharge) {
                chargeItemTotal.setAdjustedTotal(groupedAdjusted);
            }
        }
    }

    /**
     * Selects the doctor whose fee breakdown should be shown in the popup.
     */
    public void viewDoctorFeeBreakdown(DoctorFeeGroup group) {
        selectedDoctorFeeGroup = group;
    }

    /**
     * Persists an Adjusted Value edited directly on a single fee inside the
     * breakdown popup, then refreshes the doctor's adjusted total (popup footer
     * and grouped row) and the professional charge totals.
     */
    public void changeFeeAdjustedInBreakdown(BillFee billFee) {
        getBillFeeFacade().edit(billFee);
        if (selectedDoctorFeeGroup != null) {
            double sum = 0;
            for (BillFee bf : selectedDoctorFeeGroup.getFees()) {
                sum += bf.getFeeAdjusted() != 0 ? bf.getFeeAdjusted() : bf.getFeeValue();
            }
            selectedDoctorFeeGroup.setTotalAdjusted(sum);
        }
        refreshProfessionalChargeAdjustedTotal();
        updateTotal();
    }

    public DoctorFeeGroup getSelectedDoctorFeeGroup() {
        return selectedDoctorFeeGroup;
    }

    public void setSelectedDoctorFeeGroup(DoctorFeeGroup selectedDoctorFeeGroup) {
        this.selectedDoctorFeeGroup = selectedDoctorFeeGroup;
    }

    public List<Bill> getEtuMedicineIssues() {
        return etuMedicineIssues;
    }

    public void setEtuMedicineIssues(List<Bill> etuMedicineIssues) {
        this.etuMedicineIssues = etuMedicineIssues;
    }

    public List<Bill> getPharmacyMedicineIssues() {
        return pharmacyMedicineIssues;
    }

    public void setPharmacyMedicineIssues(List<Bill> pharmacyMedicineIssues) {
        this.pharmacyMedicineIssues = pharmacyMedicineIssues;
    }

    public List<Bill> getInwardMedicineIssues() {
        return inwardMedicineIssues;
    }

    public void setInwardMedicineIssues(List<Bill> inwardMedicineIssues) {
        this.inwardMedicineIssues = inwardMedicineIssues;
    }

    public List<Bill> getTheatreMedicineIssues() {
        return theatreMedicineIssues;
    }

    public void setTheatreMedicineIssues(List<Bill> theatreMedicineIssues) {
        this.theatreMedicineIssues = theatreMedicineIssues;
    }

    public List<Bill> getStoreMedicineIssues() {
        return storeMedicineIssues;
    }

    public void setStoreMedicineIssues(List<Bill> storeMedicineIssues) {
        this.storeMedicineIssues = storeMedicineIssues;
    }

    public List<Bill> getInventryMedicineIssues() {
        return inventryMedicineIssues;
    }

    public void setInventryMedicineIssues(List<Bill> inventryMedicineIssues) {
        this.inventryMedicineIssues = inventryMedicineIssues;
    }

    /**
     * View model for one doctor's combined professional fee row on the final
     * bill, carrying the summed total and the individual fees behind it.
     */
    public static class DoctorFeeGroup implements Serializable {

        private static final long serialVersionUID = 1L;

        private Staff staff;
        private final List<BillFee> fees = new ArrayList<>();
        private double total;
        private double totalAdjusted;

        public DoctorFeeGroup() {
        }

        public DoctorFeeGroup(Staff staff) {
            this.staff = staff;
        }

        public Staff getStaff() {
            return staff;
        }

        public void setStaff(Staff staff) {
            this.staff = staff;
        }

        public List<BillFee> getFees() {
            return fees;
        }

        public double getTotal() {
            return total;
        }

        public void setTotal(double total) {
            this.total = total;
        }

        public double getTotalAdjusted() {
            return totalAdjusted;
        }

        public void setTotalAdjusted(double totalAdjusted) {
            this.totalAdjusted = totalAdjusted;
        }
    }

    public void changeAdjustedValueRoomCharge(PatientRoom pR) {
        getPatientRoomFacade().edit(pR);
        double value = 0;
        for (ChargeItemTotal chargeItemTotal : chargeItemTotals) {
            switch (chargeItemTotal.getInwardChargeType()) {
                case RoomCharges:
                    value = getInwardBean().calPatientRoomChargeAdjusted(getPatientEncounter());
                    value += getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    value += getInwardBean().caltValueFromAdditionalCharge(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    chargeItemTotal.setAdjustedTotal(value);
                    break;
            }

        }

        updateTotal();
    }

    public void changeAdjustedValueLinen(PatientRoom pR) {
        getPatientRoomFacade().edit(pR);
        double value = 0;
        for (ChargeItemTotal chargeItemTotal : chargeItemTotals) {
            switch (chargeItemTotal.getInwardChargeType()) {

                case LinenCharges:
                    value = getInwardBean().calPatientRoomLinenChargeAdjusted(getPatientEncounter());
                    value += getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    value += getInwardBean().caltValueFromAdditionalCharge(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    chargeItemTotal.setAdjustedTotal(value);
                    break;

            }

        }

        updateTotal();
    }

    public void changeAdjustedValueMedicalCare(PatientRoom pR) {
        getPatientRoomFacade().edit(pR);
        double value = 0;
        for (ChargeItemTotal chargeItemTotal : chargeItemTotals) {
            switch (chargeItemTotal.getInwardChargeType()) {

                case MedicalCareICU:
                    value = getInwardBean().calPatientRoomMadicalCareAdjusted(getPatientEncounter());
                    value += getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    value += getInwardBean().caltValueFromAdditionalCharge(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    chargeItemTotal.setAdjustedTotal(value);
                    break;

            }

        }

        updateTotal();
    }

    public void changeAdjustedValueAdministration(PatientRoom pR) {
        getPatientRoomFacade().edit(pR);
        double value = 0;
        for (ChargeItemTotal chargeItemTotal : chargeItemTotals) {
            switch (chargeItemTotal.getInwardChargeType()) {

                case AdministrationCharge:
                    value = getInwardBean().calPatientRoomAdminAdjusted(getPatientEncounter());
                    value += getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    value += getInwardBean().caltValueFromAdditionalCharge(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    chargeItemTotal.setAdjustedTotal(value);
                    break;

            }

        }

        updateTotal();
    }

    public void changeAdjustedValueNursing(PatientRoom pR) {
        getPatientRoomFacade().edit(pR);
        double value = 0;
        for (ChargeItemTotal chargeItemTotal : chargeItemTotals) {
            switch (chargeItemTotal.getInwardChargeType()) {

                case NursingCharges:
                    value = getInwardBean().calPatientNursingChargeAdjusted(getPatientEncounter());
                    value += getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    value += getInwardBean().caltValueFromAdditionalCharge(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    chargeItemTotal.setAdjustedTotal(value);
                    break;

            }

        }

        updateTotal();
    }

    public void changeAdjustedValueMo(PatientRoom pR) {
        getPatientRoomFacade().edit(pR);
        double value = 0;
        for (ChargeItemTotal chargeItemTotal : chargeItemTotals) {
            switch (chargeItemTotal.getInwardChargeType()) {

                case MOCharges:
                    value = getInwardBean().calPatientMoChargeAdjusted(getPatientEncounter());
                    value += getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    value += getInwardBean().caltValueFromAdditionalCharge(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    chargeItemTotal.setAdjustedTotal(value);
                    break;
            }

        }

        updateTotal();
    }

    public void changeAdjustedValueMaintain(PatientRoom pR) {
        getPatientRoomFacade().edit(pR);
        double value = 0;
        for (ChargeItemTotal chargeItemTotal : chargeItemTotals) {
            switch (chargeItemTotal.getInwardChargeType()) {
                case MaintainCharges:
                    value = getInwardBean().calPatientMaintananceChargeAdjusted(getPatientEncounter());
                    value += getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    value += getInwardBean().caltValueFromAdditionalCharge(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
                    chargeItemTotal.setAdjustedTotal(value);
                    break;
            }

        }

        updateTotal();
    }

    public void calculateDiscount() {
        for (ChargeItemTotal cit : chargeItemTotals) {
            double discountValue = 0;
            switch (cit.getInwardChargeType()) {
                case MaintainCharges:
                    discountValue = updatePatientMaintainCharge(cit.getInwardChargeType());
                    break;
                case MOCharges:
                    discountValue = updatePatientMoCharge(cit.getInwardChargeType());
                    break;
                case NursingCharges:
                    discountValue = updatePatientNursingCharge(cit.getInwardChargeType());
                    break;
                case RoomCharges:
                    discountValue = updatePatientRoomCharge(cit.getInwardChargeType());
                    break;
                case MedicalCareICU:
                    discountValue = updatePatientMedicalCareIcuCharge(cit.getInwardChargeType());
                    break;
                case AdministrationCharge:
                    discountValue = updatePatientAdministrationCharge(cit.getInwardChargeType());
                    break;
                case LinenCharges:
                    discountValue = updatePatientLinenCharge(cit.getInwardChargeType());
                    break;
                case Medicine:
                    // Discount/margin for pharmacy issues (PharmacyBhtPre) is already
                    // calculated and persisted on the BillItem/Bill at issue time.
                    // Recomputing here at final-bill time was the slow path that hung
                    // on heavy patients (Issue #20081). Skipping is safe — the totals
                    // shown in the summary panel come from chargeItemTotals which is
                    // populated separately in createChargeItemTotals().
                    // discountValue = updateIssueBillFees(cit.getInwardChargeType(), BillType.PharmacyBhtPre);
                    break;
                case GeneralIssuing:
                    // Same as Medicine above — store issues (StoreBhtPre) already have
                    // their discount/margin persisted at issue time. Skip recompute.
                    // discountValue = updateIssueBillFees(cit.getInwardChargeType(), BillType.StoreBhtPre);
                    break;
                default:
                    discountValue = discountSet(cit);
            }

            cit.setDiscount(discountValue);
            cit.setAdjustedTotal(cit.getTotal());

        }

    }

    public double discountSet(ChargeItemTotal cit, double discountPercent) {
        if (discountPercent == 0 || cit.getTotal() == 0
                || cit.getInwardChargeType() == InwardChargeType.ProfessionalCharge
                || cit.getInwardChargeType() == InwardChargeType.DoctorAndNurses) {

            cit.setDiscount(0);
            cit.setAdjustedTotal(cit.getTotal());
            return 0;
        }

        double dis = 0;

        dis += updateServiceBillFees(cit.getInwardChargeType(), discountPercent);
        dis += updatePatientItems(cit.getInwardChargeType(), discountPercent);

        //Unknown Total Discount
        //   dis += (getValueForDiscount(cit) * discountPercent) / 100;
        return dis;
    }

    @Inject
    MembershipSchemeController membershipSchemeController;

    public double discountSet(ChargeItemTotal cit) {
//        MembershipScheme membershipScheme = membershipSchemeController.fetchPatientMembershipScheme(getPatientEncounter().getPatient(), getSessionController().getApplicationPreference().isMembershipExpires());
        PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(), null, getPatientEncounter().getCreditCompany(), cit.getInwardChargeType(), getPatientEncounter().getAdmissionType());
        if (pm == null || pm.getDiscountPercent() == 0 || cit.getTotal() == 0
                || cit.getInwardChargeType() == InwardChargeType.ProfessionalCharge
                || cit.getInwardChargeType() == InwardChargeType.DoctorAndNurses) {

            updateServiceBillFeesWithOutMatrix(cit.getInwardChargeType());
            updatePatientItemsWithOutMatrix(cit.getInwardChargeType());
            cit.setDiscount(0);
            cit.setAdjustedTotal(cit.getTotal());
            return 0;
        }

        double dis = 0;

        dis += updateServiceBillFees(cit.getInwardChargeType(), pm.getDiscountPercent());
        dis += updatePatientItems(cit.getInwardChargeType(), pm.getDiscountPercent());

        //Unknown Total Discount
        //  dis += (getValueForDiscount(cit) * pm.getDiscountPercent()) / 100;
        return dis;
    }

    private double getValueForDiscount(ChargeItemTotal chargeItemTotal) {
        double total = chargeItemTotal.getTotal();

        double serviceValue = getInwardBean().calServiceBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter(), childPatientEncouters);
        double patientItemTotal = getInwardBean().calTimedPatientItemByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter());
        double outSide = getInwardBean().calOutSideBillItemsTotalByInwardChargeType(chargeItemTotal.getInwardChargeType(), getPatientEncounter());

        double value = total - (serviceValue + patientItemTotal + outSide);

        return value;
    }

    private double updateServiceBillFees(InwardChargeType inwardChargeType, double discountPercent) {
        double disTot = 0;
        List<BillFee> list = getInwardBean().getServiceBillFeesByInwardChargeType(inwardChargeType, getPatientEncounter());

        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (BillFee bf : list) {
            double value = bf.getFeeGrossValue() + bf.getFeeMargin();
            double dis = (value * discountPercent) / 100;
            disTot += dis;
            bf.setFeeDiscount(dis);
            bf.setFeeValue(value - dis);
            getBillFeeFacade().edit(bf);
        }

        List<BillItem> listBillItems = getInwardBean().getServiceBillItemByInwardChargeType(inwardChargeType, getPatientEncounter());

        for (BillItem b : listBillItems) {
            getBillBean().updateBillItemByBillFee(b);
        }

        return disTot;
    }

    private void updateServiceBillFeesWithOutMatrix(InwardChargeType inwardChargeType) {
        getInwardBean().bulkClearServiceBillFeesWithOutMatrix(inwardChargeType, getPatientEncounter());
    }

    private double updateIssueBillFees(InwardChargeType inwardChargeType, double discountPercent, BillType billType) {
        List<BillItem> listBillItems = getInwardBean().getIssueBillItemByInwardChargeType(getPatientEncounter(), billType);

        double disTot = 0;
        if (listBillItems == null || listBillItems.isEmpty()) {
            return disTot;
        }

        for (BillItem bf : listBillItems) {
            double value = bf.getGrossValue() + bf.getMarginValue();
            double dis = (value * discountPercent) / 100;
            disTot += dis;
            bf.setDiscount(dis);
            bf.setNetValue(value - dis);
            getBillItemFacade().edit(bf);
        }

        disTot += calDiscountServicePatientItems(inwardChargeType, discountPercent);

        return disTot;
    }

    private double updatePatientItems(InwardChargeType inwardChargeType, double discountPercent) {
        double disTot = updateTimedServiceBillItems(inwardChargeType, discountPercent);

        List<PatientItem> list = getInwardBean().fetchTimedPatientItemByInwardChargeType(inwardChargeType, getPatientEncounter());
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (PatientItem bf : list) {
            double value = bf.getServiceValue();
            double dis = (value * discountPercent) / 100;
            disTot += dis;
            bf.setDiscount(dis);
            getPatientItemFacade().edit(bf);
        }

        return disTot;
    }

    /**
     * Discounts timed services that carry their charge on a BillItem.
     * <p>
     * A timed service added from the consume page now creates its own Bill and
     * BillItem, and the inward total for it is summed from the BillItem side —
     * so the discount has to land there. It cannot go through
     * {@code updateServiceBillFees}, because a timed service has no BillFee and
     * recomputing its net value from fees would zero the charge. The matching
     * PatientItem is kept in step so the breakdown screens agree with the bill.
     */
    private double updateTimedServiceBillItems(InwardChargeType inwardChargeType, double discountPercent) {
        List<BillItem> list = getInwardBean().fetchTimedServiceBillItemsByInwardChargeType(inwardChargeType, getPatientEncounter());
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (BillItem bi : list) {
            double value = bi.getGrossValue() + bi.getMarginValue();
            double dis = (value * discountPercent) / 100;
            disTot += dis;
            bi.setDiscount(dis);
            bi.setNetValue(value - dis);
            getBillItemFacade().edit(bi);

            PatientItem pi = getInwardBean().fetchPatientItemByBillItem(bi);
            if (pi != null) {
                pi.setDiscount(dis);
                getPatientItemFacade().edit(pi);
            }
        }

        return disTot;
    }

    private void updatePatientItemsWithOutMatrix(InwardChargeType inwardChargeType) {
        getInwardBean().bulkClearPatientItemsWithOutMatrix(inwardChargeType, getPatientEncounter());
        getInwardBean().bulkClearTimedServiceBillItemsWithOutMatrix(inwardChargeType, getPatientEncounter());
    }

    private double updatePatientRoomCharge(InwardChargeType inwardChargeType) {
        List<PatientRoom> list = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (PatientRoom bf : list) {
            PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(
                    getPatientEncounter().getPaymentMethod(), null, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType(), bf.getRoomFacilityCharge().getRoomCategory());

            if (pm != null) {
                disTot += updatePatientRoomCharge(bf, pm.getDiscountPercent());
            } else {
                bf.setDiscountRoomCharge(0.0);
                getPatientRoomFacade().edit(bf);
            }
        }

        disTot += calDiscountServicePatientItems(inwardChargeType);

        return disTot;
    }

    private double updatePatientRoomCharge(PatientRoom patientRoom, double discountPercent) {

        double value = patientRoom.getCalculatedRoomCharge();
        double dis = (value * discountPercent) / 100;

        patientRoom.setDiscountRoomCharge(dis);
        //   patientRoom.setAdjustedRoomCharge(value);
        getPatientRoomFacade().edit(patientRoom);

        return dis;
    }

    private double updatePatientMaintainCharge(InwardChargeType inwardChargeType) {
        List<PatientRoom> list = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        PriceMatrix pm;
        for (PatientRoom bf : list) {
            pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                    null, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType(), bf.getRoomFacilityCharge().getRoomCategory());

            if (pm != null) {
                disTot += updatePatientMaintainCharge(bf, pm.getDiscountPercent());
            } else {
                bf.setDiscountMaintainCharge(0.0);
                getPatientRoomFacade().edit(bf);
            }
        }

        disTot += calDiscountServicePatientItems(inwardChargeType);

        return disTot;
    }

    private double calDiscountServicePatientItems(InwardChargeType inwardChargeType) {
        PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                null, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType());
        double disTot = 0;
        if (pm != null) {
            disTot += updateServiceBillFees(inwardChargeType, pm.getDiscountPercent());
            disTot += updatePatientItems(inwardChargeType, pm.getDiscountPercent());
        } else {
            updateServiceBillFeesWithOutMatrix(inwardChargeType);
            updatePatientItemsWithOutMatrix(inwardChargeType);
        }

        return disTot;
    }

    private double calDiscountServicePatientItems(InwardChargeType inwardChargeType, double discount) {
        double disTot = 0;

        disTot += updateServiceBillFees(inwardChargeType, discount);
        disTot += updatePatientItems(inwardChargeType, discount);

        return disTot;
    }

    private double updatePatientMaintainCharge(PatientRoom patientRoom, double discountPercent) {

        double value = patientRoom.getCalculatedMaintainCharge();
        double dis = (value * discountPercent) / 100;
        patientRoom.setDiscountMaintainCharge(dis);
        //   patientRoom.setAdjustedMaintainCharge(value);
        getPatientRoomFacade().edit(patientRoom);

        return dis;
    }

    private double updatePatientMoCharge(InwardChargeType inwardChargeType) {
        List<PatientRoom> list = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (PatientRoom bf : list) {
            PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                    null, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType(), bf.getRoomFacilityCharge().getRoomCategory());

            if (pm != null) {
                disTot += updatePatientMoCharge(bf, pm.getDiscountPercent());
            } else {
                bf.setDiscountMoCharge(0.0);
                getPatientRoomFacade().edit(bf);
            }
        }

        disTot += calDiscountServicePatientItems(inwardChargeType);

        return disTot;
    }

    private double updatePatientMoCharge(PatientRoom patientRoom, double discountPercent) {

        double value = patientRoom.getCalculatedMoCharge();
        double dis = (value * discountPercent) / 100;
        patientRoom.setDiscountMoCharge(dis);
        // patientRoom.setAdjustedMoCharge(value);
        getPatientRoomFacade().edit(patientRoom);

        return dis;
    }

    private double updatePatientMedicalCareIcuCharge(InwardChargeType inwardChargeType) {
        List<PatientRoom> list = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (PatientRoom bf : list) {
            PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                    null, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType(), bf.getRoomFacilityCharge().getRoomCategory());

            if (pm != null) {
                disTot += updatePatientMedicalCareIcuCharge(bf, pm.getDiscountPercent());
            } else {
                bf.setDiscountMedicalCareCharge(0.0);
                getPatientRoomFacade().edit(bf);
            }
        }

        disTot += calDiscountServicePatientItems(inwardChargeType);

        return disTot;
    }

    private double updatePatientMedicalCareIcuCharge(PatientRoom patientRoom, double discountPercent) {

        double value = patientRoom.getCalculatedMedicalCareCharge();
        double dis = (value * discountPercent) / 100;
        patientRoom.setDiscountMedicalCareCharge(dis);
        //  patientRoom.setAjdustedMedicalCareCharge(value);
        getPatientRoomFacade().edit(patientRoom);

        return dis;
    }

    private double updatePatientAdministrationCharge(InwardChargeType inwardChargeType) {
        List<PatientRoom> list = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (PatientRoom bf : list) {
            PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                    null, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType(), bf.getRoomFacilityCharge().getRoomCategory());
            if (pm != null) {
                disTot += updatePatientAdministrationCharge(bf, pm.getDiscountPercent());
            } else {
                bf.setDiscountAdministrationCharge(0.0);
                getPatientRoomFacade().edit(bf);
            }
        }

        disTot += calDiscountServicePatientItems(inwardChargeType);

        return disTot;
    }

    private double updatePatientAdministrationCharge(PatientRoom patientRoom, Double discountPercent) {

        double value = patientRoom.getCalculatedAdministrationCharge();

        double dis = (value * discountPercent) / 100;
        patientRoom.setDiscountAdministrationCharge(dis);
        //  patientRoom.setAjdustedAdministrationCharge(value);
        getPatientRoomFacade().edit(patientRoom);

        return dis;
    }

    private double updatePatientLinenCharge(InwardChargeType inwardChargeType) {
        List<PatientRoom> list = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (PatientRoom bf : list) {
            PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                    null, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType(), bf.getRoomFacilityCharge().getRoomCategory());
            if (pm != null) {
                disTot += updatePatientLinenCharge(bf, pm.getDiscountPercent());
            } else {
                bf.setDiscountLinenCharge(0.0);
                getPatientRoomFacade().edit(bf);
            }
        }

        disTot += calDiscountServicePatientItems(inwardChargeType);
        return disTot;

    }

    private double updatePatientLinenCharge(PatientRoom patientRoom, double discountPercent) {

        double value = patientRoom.getCalculatedLinenCharge();
        double dis = (value * discountPercent) / 100;
        patientRoom.setDiscountLinenCharge(dis);
        //    patientRoom.setAjdustedLinenCharge(value);
        getPatientRoomFacade().edit(patientRoom);
        return dis;

    }

    private double updatePatientNursingCharge(InwardChargeType inwardChargeType) {
        List<PatientRoom> list = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);
        double disTot = 0;
        if (list == null || list.isEmpty()) {
            return disTot;
        }

        for (PatientRoom bf : list) {
            PriceMatrix pm = getPriceMatrixController().getInwardMemberDisCount(getPatientEncounter().getPaymentMethod(),
                    null, getPatientEncounter().getCreditCompany(), inwardChargeType, getPatientEncounter().getAdmissionType(), bf.getRoomFacilityCharge().getRoomCategory());

            if (pm != null) {
                disTot += updatePatientNursingCharge(bf, pm.getDiscountPercent());
            } else {
                bf.setDiscountNursingCharge(0.0);
                getPatientRoomFacade().edit(bf);
            }
        }

        disTot += calDiscountServicePatientItems(inwardChargeType);

        return disTot;
    }

    private double updatePatientNursingCharge(PatientRoom patientRoom, double discountPercent) {

        double value = patientRoom.getCalculatedNursingCharge();
        double dis = (value * discountPercent) / 100;
        patientRoom.setDiscountNursingCharge(dis);
        //   patientRoom.setAjdustedNursingCharge(value);
        getPatientRoomFacade().edit(patientRoom);

        return dis;
    }

    public void updatePatientItem(PatientItem patientItem) {
        getInwardTimedItemController().finalizeService(patientItem);
        createPatientItems();
        createChargeItemTotals();

    }

    /**
     * Entry point for the "Save Changes" button. If the room's current
     * admitted/discharged times overlap another (non-Guardian/Theatre) room
     * period for the same patient or bed, ask for confirmation before saving
     * instead of saving immediately.
     */
    public void checkBeforeUpdatePatientRoom(PatientRoom patientRoom) {
        if (hasOverlap(patientRoom)) {
            pendingOverlapRoom = patientRoom;
            PrimeFaces.current().executeScript("PF('dlgRoomOverlapConfirm').show();");
            return;
        }
        updatePatientRoom(patientRoom);
    }

    public void confirmUpdatePatientRoomWithOverlap() {
        updatePatientRoom(pendingOverlapRoom);
        pendingOverlapRoom = null;
    }

    public PatientRoom getPendingOverlapRoom() {
        return pendingOverlapRoom;
    }

    public String getPendingOverlapDescription() {
        return getOverlapDescription(pendingOverlapRoom);
    }

    /**
     * The other non-Guardian/Theatre room stay(s) for the same patient
     * encounter or same bed (RoomFacilityCharge) whose admitted/discharged time
     * range overlaps this room's. Returned (rather than just a count) so
     * callers can report exactly which room(s) are the conflict instead of a
     * generic "overlap detected" message.
     */
    public List<PatientRoom> getOverlappingRooms(PatientRoom patientRoom) {
        if (patientRoom == null || patientRoom.getAdmittedAt() == null || patientRoom.getPatientEncounter() == null) {
            return new ArrayList<>();
        }
        if (patientRoom.getDischargedAt() != null && patientRoom.getDischargedAt().before(patientRoom.getAdmittedAt())) {
            return new ArrayList<>();
        }
        if (patientRoom instanceof GuardianRoom || patientRoom instanceof TheatreRoom) {
            return new ArrayList<>();
        }
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT pr2 FROM PatientRoom pr2 WHERE pr2.retired = false ");
        jpql.append("AND TYPE(pr2) != :guardianClass AND TYPE(pr2) != :theatreClass ");
        Map<String, Object> params = new HashMap<>();
        params.put("guardianClass", GuardianRoom.class);
        params.put("theatreClass", TheatreRoom.class);
        if (patientRoom.getId() != null) {
            jpql.append("AND pr2.id != :excludeId ");
            params.put("excludeId", patientRoom.getId());
        }
        if (patientRoom.getRoomFacilityCharge() != null) {
            jpql.append("AND (pr2.patientEncounter = :pe OR pr2.roomFacilityCharge = :rfc) ");
            params.put("rfc", patientRoom.getRoomFacilityCharge());
        } else {
            jpql.append("AND pr2.patientEncounter = :pe ");
        }
        params.put("pe", patientRoom.getPatientEncounter());
        if (patientRoom.getDischargedAt() != null) {
            jpql.append("AND pr2.admittedAt < :to ");
            params.put("to", patientRoom.getDischargedAt());
        }
        jpql.append("AND (pr2.dischargedAt IS NULL OR pr2.dischargedAt > :from)");
        params.put("from", patientRoom.getAdmittedAt());
        // TemporalType.TIMESTAMP is required: the 2-arg findByJpql overload binds
        // every Date with TemporalType.DATE, truncating admittedAt/dischargedAt to
        // midnight, so two stays merely sharing a calendar day were reported as
        // overlapping even when the times did not actually conflict.
        List<PatientRoom> overlaps = getPatientRoomFacade().findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
        return overlaps != null ? overlaps : new ArrayList<>();
    }

    /**
     * True when this room's admitted/discharged time range overlaps another
     * non-Guardian/Theatre room period for the same patient encounter or the
     * same bed (RoomFacilityCharge). Used both to gate the save confirmation
     * and to render a persistent warning on the room row.
     */
    public boolean hasOverlap(PatientRoom patientRoom) {
        return !getOverlappingRooms(patientRoom).isEmpty();
    }

    /**
     * Human-readable summary of which specific room(s) this room's time range
     * conflicts with, e.g. "Room 412 (Active)". Used by the row-level "Overlap"
     * tag and the save confirmation dialog so a conflict can be identified and
     * resolved instead of showing a generic warning that stays stuck when there
     * are 3+ open (non-discharged) room stays.
     */
    public String getOverlapDescription(PatientRoom patientRoom) {
        List<PatientRoom> overlaps = getOverlappingRooms(patientRoom);
        if (overlaps.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("Overlaps with ");
        for (int i = 0; i < overlaps.size(); i++) {
            PatientRoom pr2 = overlaps.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            String roomName = pr2.getRoomFacilityCharge() != null && pr2.getRoomFacilityCharge().getName() != null
                    ? pr2.getRoomFacilityCharge().getName() : "an unnamed room";
            sb.append(roomName).append(pr2.getDischargedAt() == null ? " (Active)" : " (Left)");
        }
        return sb.toString();
    }

    public boolean isAnyRoomOverlapping() {
        if (patientRooms == null) {
            return false;
        }
        for (PatientRoom pr : patientRooms) {
            if (hasOverlap(pr)) {
                return true;
            }
        }
        return false;
    }

    public void updatePatientRoom(PatientRoom patientRoom) {
        if (patientRoom == null) {
            return;
        }

        List<PatientRoom> rooms = patientRooms;
        if (rooms != null) {
            if (rooms.size() > 1) {
                int currentRoomIndex = rooms.indexOf(patientRoom);
                if (currentRoomIndex > 0) {
                    PatientRoom previousRoom = rooms.get(currentRoomIndex - 1);

                    if (patientRoom.getAdmittedAt() != null && previousRoom.getDischargedAt() != null) {
                        if (patientRoom.getAdmittedAt().before(previousRoom.getDischargedAt())) {
                            JsfUtil.addErrorMessage("Admitted time must be after the discharge time of the previous room.");
                            return;
                        }
                    }
                }
            }
        }

        // If validation passes, save the room (edit or create)
        if (patientRoom.getId() != null) {
            getPatientRoomFacade().edit(patientRoom);
        } else {
            getPatientRoomFacade().create(patientRoom);
        }

        invalidateUnifiedGanttBarsCache();

        // Refresh the tables or any other necessary actions after saving
        createTables();
    }

    public void updateChargesForRoom(PatientRoom pr) {
        if (pr == null || pr.getRoomFacilityCharge() == null) {
            JsfUtil.addErrorMessage("Room facility charge not set");
            return;
        }
        if (pr.isFromPackage() && !isPackageRoomDurationExceeded(pr)) {
            // Package-locked charge stays as set by InpatientPackageApplicationBean.
            patientRooms = null;
            createTables();
            return;
        }
        RoomFacilityCharge rfc = pr.getRoomFacilityCharge();
        pr.setCurrentRoomCharge(rfc.getRoomCharge() != null ? rfc.getRoomCharge() : 0.0);
        pr.setCurrentMaintananceCharge(rfc.getMaintananceCharge() != null ? rfc.getMaintananceCharge() : 0.0);
        pr.setCurrentNursingCharge(rfc.getNursingCharge() != null ? rfc.getNursingCharge() : 0.0);
        pr.setCurrentMoCharge(rfc.getMoCharge() != null ? rfc.getMoCharge() : 0.0);
        pr.setCurrentMoChargeForAfterDuration(rfc.getMoChargeForAfterDuration() != null ? rfc.getMoChargeForAfterDuration() : 0.0);
        pr.setCurrentLinenCharge(rfc.getLinenCharge() != null ? rfc.getLinenCharge() : 0.0);
        pr.setCurrentAdministrationCharge(rfc.getAdminstrationCharge());
        pr.setCurrentMedicalCareCharge(rfc.getMedicalCareCharge());
        getPatientRoomFacade().edit(pr);
        patientRooms = null;
        createTables();
    }

    public void updatePrintingPatientRoom(PatientRoom patientRoom) {
        if (patientRoom.getId() != null) {
            getPatientRoomFacade().edit(patientRoom);
        } else {
            getPatientRoomFacade().create(patientRoom);
        }

        if (patientRoom.isFromPackage() && !isPackageRoomDurationExceeded(patientRoom)) {
            // Package-locked room: currentRoomCharge already holds the package's fixed
            // total, not a per-block rate - do not overwrite it with the facility rate
            // while still within the included duration.
            patientRoom.setCalculatedRoomCharge(patientRoom.getCurrentRoomCharge() + patientRoom.getAddedRoomCharge());
        } else {
            patientRoom.setCurrentRoomCharge(patientRoom.getRoomFacilityCharge().getRoomCharge());
            calCulateRoomCharge(patientRoom);
        }

        updatePaitentRoomAdjustedTotal();
    }

    private void updatePaitentRoomAdjustedTotal() {
        for (ChargeItemTotal cit : chargeItemTotals) {
            if (cit.getInwardChargeType() == InwardChargeType.RoomCharges) {
                double dbl = 0;
                for (PatientRoom pr : cit.getPatientRooms()) {
                    if (pr.getReferencePatientRoom() != null) {
                        dbl += pr.getReferencePatientRoom().getCalculatedRoomCharge();
                    }
                }
                cit.setAdjustedTotal(dbl);
            }
        }
    }

    private void calCulateRoomCharge(PatientRoom p) {
        double charge;
        //    System.err.println("1 " + p.getRoomFacilityCharge());
        //   System.err.println("2 " + p.getCurrentRoomCharge());
        if (p.getRoomFacilityCharge() == null || p.getCurrentRoomCharge() == 0) {
            return;
        }

        TimedItemFee timedFee = p.getRoomFacilityCharge().getTimedItemFee();
        double roomCharge = p.getCurrentRoomCharge();

        charge = roomCharge * getInwardBean().calCount(timedFee, p.getAdmittedAt(), p.getDischargedAt());

        p.setCalculatedRoomCharge(charge);
    }

    private boolean isPackageRoomDurationExceeded(PatientRoom pr) {
        if (pr.getIncludedRoomDurationHours() == null) {
            return true;
        }
        Date to = pr.getDischargedAt() != null ? pr.getDischargedAt() : new Date();
        if (pr.getAdmittedAt() == null) {
            return false;
        }
        long stayedHours = java.time.Duration.between(
                pr.getAdmittedAt().toInstant(), to.toInstant()).toHours();
        return stayedHours > pr.getIncludedRoomDurationHours();
    }

    public double getPackageRoomVarianceCharge(PatientRoom pr) {
        if (pr == null || !pr.isFromPackage() || !isPackageRoomDurationExceeded(pr) || pr.getRoomFacilityCharge() == null) {
            return 0.0;
        }
        // currentRoomCharge holds the package's locked TOTAL for this room, not a
        // per-block rate, so it must not be used as the multiplicand here (that was
        // the bug: reusing calCulateRoomCharge(pr), which multiplies
        // pr.getCurrentRoomCharge() by elapsed blocks). Both sides of this variance
        // must be derived from the room's real per-block rate, RoomFacilityCharge.roomCharge.
        Double facilityRoomCharge = pr.getRoomFacilityCharge().getRoomCharge();
        if (facilityRoomCharge == null) {
            return 0.0;
        }
        TimedItemFee timedFee = pr.getRoomFacilityCharge().getTimedItemFee();
        double liveEquivalent = facilityRoomCharge * getInwardBean().calCount(timedFee, pr.getAdmittedAt(), pr.getDischargedAt());
        // RoomFacilityCharge.roomCharge is a rate per TimedItemFee.durationHours block (see
        // InwardBeanController.calCount: charge = roomCharge * count, where count is the number
        // of durationHours-sized blocks between admittedAt/dischargedAt). Room-charge TimedItemFee
        // configs are conventionally 24-hour ("per day") blocks, so we use the actual configured
        // durationHours here (falling back to 24.0 if unset) rather than hardcoding 24.
        double blockHours = (timedFee != null && timedFee.getDurationHours() > 0) ? timedFee.getDurationHours() : 24.0;
        double includedEquivalent = facilityRoomCharge * (pr.getIncludedRoomDurationHours() / blockHours);
        return Math.max(0.0, liveEquivalent - includedEquivalent);
    }

    private boolean checkDischargeTime() {
        if (getPatientEncounter() == null) {
            return true;
        }

        if (getPatientEncounter().getDateOfAdmission() == null) {
            return true;
        }

        if (date == null) {
            return true;
        }

        if (getPatientEncounter().getDateOfAdmission().after(date)) {
            JsfUtil.addErrorMessage("Check Discharge Time should be after Admitted Time");
            return true;
        }

        if (getPatientEncounter().getAdmissionType() != null
                && getPatientEncounter().getAdmissionType().isRoomChargesAllowed()) {
            boolean nursingDischargeRequired = configOptionApplicationController.getBooleanValueByKey(
                    "Inward Administrative Discharge - Require Nursing Discharge", true);
            if (nursingDischargeRequired && !getPatientEncounter().isNursingDischarged()) {
                JsfUtil.addErrorMessage("Nursing discharge must be completed before the bill can be settled.");
                return true;
            }
            Date nursingDt = getPatientEncounter().getNursingDischargeDateTime();
            if (nursingDt != null && date.before(nursingDt)) {
                JsfUtil.addErrorMessage("Discharge time must be on or after the nursing discharge time.");
                return true;
            }
            Date roomDt = getPatientEncounter().getRoomDischargeDateTime();
            if (roomDt != null && date.before(roomDt)) {
                JsfUtil.addErrorMessage("Discharge time must be on or after the room discharge time.");
                return true;
            }
        }

        return false;

    }

    public void checkDate() {
        if (checkDischargeTime()) {
            return;
        }

        makeNull();
        createTables();
    }

    private List<BillItem> billItems;

    @Inject
    BillBeanController billBean;

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    private void updatePaymentBillList() {
        for (Bill bill : getPaymentBill()) {
            //getBillBean().updateInwardDipositList(getCurrent().getPatientEncounter(), b);
            if (getCurrent().getPatientEncounter() != null && bill != null) {
                if (getCurrent().getPatientEncounter().isPaymentFinalized() && getCurrent().getPatientEncounter().getFinalBill() != null) {
                    bill.setForwardReferenceBill(getCurrent().getPatientEncounter().getFinalBill());
                    getBillFacade().edit(bill);
                    if (!getCurrent().getPatientEncounter().getFinalBill().getBackwardReferenceBills().contains(bill)) {
                        getCurrent().getPatientEncounter().getFinalBill().getBackwardReferenceBills().add(bill);
                    }
                    getBillFacade().edit(patientEncounter.getFinalBill());
                }
            }
        }
    }

    public void settleOriginalBill() {
        if (errorCheck()) {
            return;
        }

        saveOriginalBill();
        saveOriginalBillItem();

        JsfUtil.addSuccessMessage("Original Bill Saved");

    }

    public void createTempBill() {
        // Capture the current grouped (doctor-by-doctor) professional fee order so the
        // Temporary Bill preview shows the combined doctor list with the latest adjusted
        // values, in the same order shown on screen.
        persistGroupedProfessionalFeeOrder();
        tempBill = null;
        updateTotal();
        saveTempBill();
        saveTempBillItem();
    }

    public void saveProvisionalBill() {
        if (errorCheck()) {
            return;
        }

        originalBill.setDiscount(discount);
        originalBill.setNetTotal(originalBill.getGrantTotal() - discount);
        getBillFacade().edit(originalBill);

        saveBill();
        getCurrent().setBillTypeAtomic(BillTypeAtomic.INWARD_PROVISIONAL_BILL);
        getCurrent().setBillType(BillType.InwardProvisionalBill);
        getCurrent().setBackwardReferenceBill(originalBill);
        getBillFacade().edit(getCurrent());
        saveBillItem();
        JsfUtil.addSuccessMessage("Provisional Bill Saved");
        showOrginalBill = false;
        printPreview = true;
        originalBill = null;
    }

    public void settle() {
        if (errorCheck()) {
            return;
        }

        persistGroupedProfessionalFeeOrder();

        originalBill.setDiscount(discount);
        originalBill.setNetTotal(originalBill.getGrantTotal() - discount);
        getBillFacade().edit(originalBill);

        saveBill();
        saveBillItem();

        // Scoped here (not in the shared saveBill()) because saveBill() is also called by
        // saveProvisionalBill() for a bill that is about to become BillType.InwardProvisionalBill,
        // not a final bill — flipping confirmedFinalBill there would wrongly unconfirm the real
        // confirmed version on every provisional save.
        getCurrent().setConfirmedFinalBill(true);
        if (getPatientEncounter().getFinalBill() != null && !getPatientEncounter().getFinalBill().getId().equals(getCurrent().getId())) {
            Bill oldConfirmed = getPatientEncounter().getFinalBill();
            oldConfirmed.setConfirmedFinalBill(false);
            getBillFacade().edit(oldConfirmed);
        }
        getBillFacade().edit(getCurrent());

        if (getPatientEncounter().getPaymentMethod() == PaymentMethod.Credit) {
            getInwardBean().updateCreditDetail(getPatientEncounter(), getCurrent().getNetTotal());
            for (CreditCompanyAllocation alloc : creditCompanyAllocations) {
                if (alloc.getAllocatedAmount() > 0) {
                    saveCCBillForAllocation(getPatientEncounter(), alloc);
                }
            }
        }

        getPatientEncounter().setFinalBill(getCurrent());
        getPatientEncounter().setGrantTotal(getCurrent().getGrantTotal());
        getPatientEncounter().setDiscount(getCurrent().getDiscount());
        getPatientEncounter().setNetTotal(getCurrent().getNetTotal());
        getPatientEncounter().setPaymentFinalized(true);
        getPatientEncounterFacade().edit(getPatientEncounter());
        getCurrent().setReferenceBill(originalBill);
        getBillFacade().edit(getCurrent());

        updatePaymentBillList();

        Map<String, Object> settlementState = new LinkedHashMap<>();
        settlementState.put("finalBillId", getCurrent().getId());
        settlementState.put("grantTotal", getCurrent().getGrantTotal());
        settlementState.put("discount", getCurrent().getDiscount());
        settlementState.put("netTotal", getCurrent().getNetTotal());
        settlementState.put("itemDiscountTotal", itemDiscountTotal);
        settlementState.put("chargeTypeDiscountTotal", chargeTypeDiscountTotal);
        settlementState.put("billLevelDiscount", billLevelDiscount);
        if (chargeItemTotals != null) {
            for (ChargeItemTotal cit : chargeItemTotals) {
                if (cit.getDiscount() != 0) {
                    settlementState.put("itemDiscount_" + cit.getInwardChargeType(), cit.getDiscount());
                }
                if (cit.getChargeTypeDiscount() != 0) {
                    settlementState.put("chargeTypeDiscount_" + cit.getInwardChargeType(), cit.getChargeTypeDiscount());
                }
            }
        }
        auditService.logEncounterAudit(getPatientEncounter(), "Final Bill Settled",
                null, settlementState, sessionController.getLoggedUser(),
                "Bill", getCurrent().getId());

        JsfUtil.addSuccessMessage("Bill Saved");

        showOrginalBill = false;
        printPreview = true;
        originalBill = null;
        creatingNewVersion = false;
    }

    /**
     * Navigate to the appropriate payment collection page after the final bill
     * is settled. For Cash/Card/Cheque patients this goes to the standard
     * inward payment page; for Credit patients who have a patient co-payment
     * portion it goes to the dedicated co-payment page.
     */
    public String navigateToCollectPayment() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No patient encounter selected");
            return "";
        }
        inwardPaymentController.makeNull();
        inwardPaymentController.getCurrent().setPatientEncounter(patientEncounter);
        inwardPaymentController.bhtListener();
        if (patientEncounter.getPaymentMethod() == PaymentMethod.Credit) {
            return "/credit/inward_patient_copay_payment?faces-redirect=true";
        }
        return "/inward/inward_bill_payment?faces-redirect=true";
    }

    public String settleProvisionalBill(Bill b) {

        if (b == null) {
            JsfUtil.addErrorMessage("Error : Bill Not Found!");
            return "";
        }

        setPatientEncounter(b.getPatientEncounter());

        originalBill = b.getBackwardReferenceBill();
        originalBill.setDiscount(b.getDiscount());
        originalBill.setNetTotal(originalBill.getGrantTotal() - b.getDiscount());
        getBillFacade().edit(originalBill);

//        current = new BilledBill();
//        getCurrent().copy(b);
//        getCurrent().copyValue(b);
//        getCurrent().setBillTypeAtomic(BillTypeAtomic.INWARD_FINAL_BILL);
//        getCurrent().setBillType(BillType.InwardFinalBill);
//        getBillFacade().create(getCurrent());
//        
//        for (BillItem bi : b.getBillItems()) {
//            BillItem bin = new BillItem();
//            bin.copy(bi);
//            bin.setBill(getCurrent());
//            getBillItemFacade().create(bin);
//            if (!bi.getProFees().isEmpty() || bi.getProFees() != null) {
//                for (BillFee bf : bi.getProFees()) {
//                    BillFee nbf = new BillFee();
//                    nbf.copy(bf);
//                    nbf.setBill(getCurrent());
//                    nbf.setBillItem(bin);
//                    billFeeFacade.create(nbf);
//                }
//            }
//            if(bi.getBillFees() != null || !bi.getBillFees().isEmpty()){
//                for(BillFee bf : bi.getBillFees()){
//                    BillFee nbf = new BillFee();
//                    nbf.copy(bf);
//                    nbf.setBill(getCurrent());
//                    nbf.setBillItem(bin);
//                    billFeeFacade.create(nbf);
//                }
//            }
//        }
        setCurrent(b);
        getCurrent().setBillTypeAtomic(BillTypeAtomic.INWARD_FINAL_BILL);
        getCurrent().setBillType(BillType.InwardFinalBill);

        getBillFacade().edit(current);

        if (getPatientEncounter().getPaymentMethod() == PaymentMethod.Credit) {
            getInwardBean().updateCreditDetail(getPatientEncounter(), getCurrent().getNetTotal());
            createCreditBillForCreditCompany(getPatientEncounter(), getCurrent().getNetTotal());
        }

        getCurrent().setConfirmedFinalBill(true);
        if (getPatientEncounter().getFinalBill() != null && !getPatientEncounter().getFinalBill().getId().equals(getCurrent().getId())) {
            Bill oldConfirmed = getPatientEncounter().getFinalBill();
            oldConfirmed.setConfirmedFinalBill(false);
            getBillFacade().edit(oldConfirmed);
        }

        getPatientEncounter().setFinalBill(getCurrent());
        getPatientEncounter().setGrantTotal(getCurrent().getGrantTotal());
        getPatientEncounter().setDiscount(getCurrent().getDiscount());
        getPatientEncounter().setNetTotal(getCurrent().getNetTotal());
        getPatientEncounter().setPaymentFinalized(true);
        getPatientEncounterFacade().edit(getPatientEncounter());
        getCurrent().setReferenceBill(originalBill);
        getBillFacade().edit(getCurrent());

        updatePaymentBillList();
        JsfUtil.addSuccessMessage("Bill Saved");

        showOrginalBill = false;
        printPreview = true;
        originalBill = null;

        return "inward_bill_final?faces-redirect=true";
    }

    /**
     * Starts a brand-new final bill version for the same admission, seeded from
     * {@code sourceBill}'s discount and credit-company allocations, but
     * recomputing all live charge tables fresh (the source bill's exact
     * BillItems are not frozen/replayed).
     */
    public String createNewVersionFromBill(Bill sourceBill) {
        creatingNewVersion = true;
        setPatientEncounter(sourceBill.getPatientEncounter());

        createTables();
        calculateDiscount();
        updateTotal();

        if (sourceBill.getBillFinanceDetails() != null
                && sourceBill.getBillFinanceDetails().getBillDiscount() != null
                && sourceBill.getBillFinanceDetails().getBillDiscount().doubleValue() != 0) {
            billLevelDiscount = sourceBill.getBillFinanceDetails().getBillDiscount().doubleValue();
            // Recompute discount/due for the restored bill-level discount, mirroring
            // listnerDiscontAmmountChanged() — done manually (not via that listener)
            // because creditCompanyAllocations isn't seeded yet at this point and the
            // listener would rebuild it from live data before rebuildAllocationsFromSourceBill runs.
            discount = itemDiscountTotal + chargeTypeDiscountTotal + billLevelDiscount;
            due = (grantTotal - discount) - paid;
        }

        creditCompanyAllocations = rebuildAllocationsFromSourceBill(sourceBill);

        // Initializes `originalBill` (mirrors toSettle()'s sequence) — settle() requires
        // it to be non-null; without this call it stays null on the create-version path.
        settleOriginalBill();

        getCurrent().setPreviousVersion(sourceBill);

        return "inward_bill_final?faces-redirect=true";
    }

    /**
     * Rebuilds the on-screen credit-company allocation split from the CC
     * commitment bills recorded against {@code sourceBill}, so a new version
     * starts from the same split rather than forcing the cashier to re-allocate
     * from scratch.
     */
    private List<CreditCompanyAllocation> rebuildAllocationsFromSourceBill(Bill sourceBill) {
        List<CreditCompanyAllocation> allocations = new ArrayList<>();
        String jpql = "select b from Bill b where b.referenceBill = :sourceBill and b.billTypeAtomic = :atomic and b.cancelled = false";
        Map<String, Object> params = new HashMap<>();
        params.put("sourceBill", sourceBill);
        params.put("atomic", BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);
        List<Bill> ccBills = getBillFacade().findByJpql(jpql, params);

        double allocatedSoFar = 0.0;
        if (ccBills != null) {
            for (Bill ccBill : ccBills) {
                allocations.add(new CreditCompanyAllocation(ccBill.getCreditCompany(), ccBill.getNetTotal()));
                allocatedSoFar += ccBill.getNetTotal();
            }
        }

        double newLiveNetDue = Math.max(0.0, (grantTotal - discount) - paidByPatient - paidByCompany);
        double patientPortion = Math.max(0.0, newLiveNetDue - allocatedSoFar);
        allocations.add(new CreditCompanyAllocation(patientPortion, true));

        return allocations;
    }

    public void createCreditBillForCreditCompany(PatientEncounter patientEncounter, Double netTotal) {
        updateTotal();
        Double due = netTotal - (paidByCompany + paidByPatient);
        List<EncounterCreditCompany> encounterCreditCompanys = fillCreditCompaniesByPatient(patientEncounter);
        for (EncounterCreditCompany ecc : encounterCreditCompanys) {
            if (due > 0) {
                if (due > ecc.getCreditLimit()) {
                    saveCreditBillForCreditCompany(patientEncounter, ecc, ecc.getCreditLimit());
                    due = due - ecc.getCreditLimit();
                } else {
                    saveCreditBillForCreditCompany(patientEncounter, ecc, due);
                    due = due - due;
                }
            }
        }
        JsfUtil.addSuccessMessage("Credit Bill Successfully Created.");
    }

    public List<EncounterCreditCompany> fillCreditCompaniesByPatient(PatientEncounter patientEncounter) {
        List<EncounterCreditCompany> encounterCreditCompanys = new ArrayList<>();
        String sql = "select ecc from EncounterCreditCompany ecc"
                + "  where ecc.retired=false "
                + " and ecc.patientEncounter=:pEnc ";
        HashMap hm = new HashMap();
        hm.put("pEnc", patientEncounter);
        encounterCreditCompanys = encounterCreditCompanyFacade.findByJpql(sql, hm);
        return encounterCreditCompanys;
    }

    public void saveCreditBillForCreditCompany(PatientEncounter pe, EncounterCreditCompany ecc, Double value) {
        saveCCBill(pe, ecc, value);
    }

    /**
     * Discards the on-screen credit-company allocation split and rebuilds it
     * from the present net due (total − discounts − paid). Called when Process
     * is clicked or any discount is changed so the allocation always follows
     * the latest discounts. Cashier-entered splits are intentionally reset here
     * — their total no longer matches the new net due.
     */
    private void rebuildCreditCompanyAllocations() {
        if (getPatientEncounter() == null
                || getPatientEncounter().getPaymentMethod() != PaymentMethod.Credit) {
            return;
        }
        // When creating a new final bill version, the allocation list was already
        // seeded from the source bill's credit-company split (see
        // createNewVersionFromBill/rebuildAllocationsFromSourceBill) — clicking
        // Process here must not discard that starting point. In the normal
        // interim-settle flow, always rebuild from live data.
        if (!creatingNewVersion) {
            creditCompanyAllocations = null;
        }
        populateCreditCompanyAllocations();
    }

    /**
     * Action for the Process button on the final bill page: recalculates all
     * totals and re-splits the credit company allocations against the
     * recalculated net due.
     */
    public void processFinalBill() {
        updateTotal();
        rebuildCreditCompanyAllocations();
    }

    private void populateCreditCompanyAllocations() {
        // Preserve cashier-entered amounts — only build the list when it is empty/null
        if (creditCompanyAllocations != null && !creditCompanyAllocations.isEmpty()) {
            return;
        }
        creditCompanyAllocations = new ArrayList<>();
        double remaining = Math.max(0.0, (grantTotal - discount) - paidByPatient - paidByCompany);
        List<EncounterCreditCompany> eccs = fillCreditCompaniesByPatient(patientEncounter);
        if (eccs != null && !eccs.isEmpty()) {
            // Sort by institution name for a stable, deterministic split order
            eccs.sort(Comparator.comparing(
                    ecc -> ecc.getInstitution() != null ? ecc.getInstitution().getName() : "",
                    Comparator.nullsLast(Comparator.naturalOrder())));
            // Every registered company gets a row (0.00 when the due is already
            // covered) so the cashier can redistribute freely. Auto-split honours
            // each company's credit limit; manual edits afterwards are not capped.
            for (EncounterCreditCompany ecc : eccs) {
                double alloc = Math.min(Math.max(0.0, remaining), ecc.getCreditLimit());
                creditCompanyAllocations.add(new CreditCompanyAllocation(ecc, alloc));
                remaining -= alloc;
            }
        } else if (patientEncounter.getCreditCompany() != null) {
            creditCompanyAllocations.add(new CreditCompanyAllocation(patientEncounter.getCreditCompany(),
                    Math.max(0.0, remaining)));
            remaining = 0;
        }
        // The patient co-payment row is ALWAYS shown (even at 0.00) so it can be
        // adjusted by hand against the company rows.
        creditCompanyAllocations.add(new CreditCompanyAllocation(Math.max(0.0, remaining), true));
    }

    private boolean checkCreditAllocationTotal() {
        if (getPatientEncounter() == null
                || getPatientEncounter().getPaymentMethod() != PaymentMethod.Credit) {
            return false;
        }
        double expected = Math.max(0.0, (grantTotal - discount) - paidByPatient - paidByCompany);
        if (expected > 0 && (creditCompanyAllocations == null || creditCompanyAllocations.isEmpty())) {
            JsfUtil.addErrorMessage("Please allocate the full credit due amount before settlement");
            return true;
        }
        // Credit limits are enforced only when auto-generating the split; the
        // cashier may exceed them when adjusting by hand. The only hard rules at
        // settlement: no negative rows, and the rows must add up to the net due.
        double totalAllocated = 0.0;
        for (CreditCompanyAllocation alloc : creditCompanyAllocations) {
            if (alloc.getAllocatedAmount() < 0) {
                JsfUtil.addErrorMessage("Allocated amounts cannot be negative");
                return true;
            }
            totalAllocated += alloc.getAllocatedAmount();
        }
        if (Math.abs(totalAllocated - expected) > 0.01) {
            JsfUtil.addErrorMessage("Total allocation (" + String.format("%.2f", totalAllocated)
                    + ") must equal the net due amount (" + String.format("%.2f", expected)
                    + "). Difference: " + String.format("%.2f", totalAllocated - expected));
            return true;
        }
        return false;
    }

    /**
     * Adds a credit company to this BHT from the final bill page. The
     * EncounterCreditCompany is persisted immediately — it becomes part of the
     * encounter's permanent company list, exactly as when added at admission —
     * and an allocation row is added so the cashier can settle against it right
     * away. The company takes up to its credit limit out of the patient's
     * remaining co-payment share.
     */
    public void addNewCreditCompany() {
        boolean added = false;
        try {
            added = addNewCreditCompanyInternal();
        } finally {
            // Tell the client whether the add passed so the dialog closes only on
            // success (see btnAddCreditCompany oncomplete).
            if (PrimeFaces.current().isAjaxRequest()) {
                PrimeFaces.current().ajax().addCallbackParam("creditCompanyAdded", added);
            }
        }
    }

    /**
     * @return true when the company was persisted and allocated; false when a
     * validation check rejected the input.
     */
    private boolean addNewCreditCompanyInternal() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No admission selected");
            return false;
        }
        if (patientEncounter.getPaymentMethod() != PaymentMethod.Credit) {
            JsfUtil.addErrorMessage("Credit companies can only be added to a credit admission");
            return false;
        }
        if (newEncounterCreditCompany == null || newEncounterCreditCompany.getInstitution() == null) {
            JsfUtil.addErrorMessage("Please select a credit company");
            return false;
        }
        if (newEncounterCreditCompany.getCreditLimit() <= 0) {
            JsfUtil.addErrorMessage("Please enter a credit limit greater than zero");
            return false;
        }
        if (creditCompanyAllocations == null) {
            creditCompanyAllocations = new ArrayList<>();
        }
        // Check what is actually registered against the encounter, not just what is
        // on screen: populateCreditCompanyAllocations() stops allocating once the due
        // is covered, so a registered company can be absent from the allocation list
        // and would otherwise be persisted a second time here.
        for (EncounterCreditCompany registered : fillCreditCompaniesByPatient(patientEncounter)) {
            if (newEncounterCreditCompany.getInstitution().equals(registered.getInstitution())) {
                JsfUtil.addErrorMessage(newEncounterCreditCompany.getInstitution().getName()
                        + " is already registered for this BHT");
                return false;
            }
        }
        for (CreditCompanyAllocation alloc : creditCompanyAllocations) {
            if (!alloc.isPatientPortion()
                    && newEncounterCreditCompany.getInstitution().equals(alloc.getCreditCompany())) {
                JsfUtil.addErrorMessage(newEncounterCreditCompany.getInstitution().getName()
                        + " is already allocated for this BHT");
                return false;
            }
        }

        newEncounterCreditCompany.setPatientEncounter(patientEncounter);
        newEncounterCreditCompany.setCreatedAt(new Date());
        newEncounterCreditCompany.setCreater(sessionController.getLoggedUser());
        newEncounterCreditCompany.setRetired(false);
        encounterCreditCompanyFacade.create(newEncounterCreditCompany);

        double expected = Math.max(0.0, (grantTotal - discount) - paidByPatient - paidByCompany);
        double ccSum = 0.0;
        for (CreditCompanyAllocation alloc : creditCompanyAllocations) {
            if (!alloc.isPatientPortion()) {
                ccSum += alloc.getAllocatedAmount();
            }
        }
        double allocatable = Math.max(0.0, Math.min(expected - ccSum, newEncounterCreditCompany.getCreditLimit()));
        creditCompanyAllocations.add(new CreditCompanyAllocation(newEncounterCreditCompany, allocatable));
        moveCompanyRowsAbovePatientRow();
        recalculatePatientPortion();

        JsfUtil.addSuccessMessage(newEncounterCreditCompany.getInstitution().getName() + " added");
        newEncounterCreditCompany = new EncounterCreditCompany();
        return true;
    }

    /**
     * Clears the add-credit-company form so the popup opens blank rather than
     * showing whatever was typed the last time it was cancelled.
     */
    public void prepareNewCreditCompany() {
        newEncounterCreditCompany = new EncounterCreditCompany();
    }

    /**
     * Keeps every credit company row grouped together at the top of the
     * allocation table with the patient co-payment row displayed last. A newly
     * added company is appended to the end of the list, which would otherwise
     * push it below the patient row created earlier by
     * {@link #populateCreditCompanyAllocations()}. The sort is stable, so the
     * existing company order is preserved.
     */
    private void moveCompanyRowsAbovePatientRow() {
        if (creditCompanyAllocations == null) {
            return;
        }
        creditCompanyAllocations.sort(Comparator.comparing(CreditCompanyAllocation::isPatientPortion));
    }

    public EncounterCreditCompany getNewEncounterCreditCompany() {
        if (newEncounterCreditCompany == null) {
            newEncounterCreditCompany = new EncounterCreditCompany();
        }
        return newEncounterCreditCompany;
    }

    public void setNewEncounterCreditCompany(EncounterCreditCompany newEncounterCreditCompany) {
        this.newEncounterCreditCompany = newEncounterCreditCompany;
    }

    /**
     * Recalculates the patient co-payment row so it always equals: net due –
     * sum of all CC company allocations. Called via p:ajax whenever a CC
     * company amount is changed by the user.
     */
    public void recalculatePatientPortion() {
        if (creditCompanyAllocations == null || creditCompanyAllocations.isEmpty()) {
            return;
        }
        double expected = Math.max(0.0, (grantTotal - discount) - paidByPatient - paidByCompany);
        double ccSum = 0.0;
        for (CreditCompanyAllocation alloc : creditCompanyAllocations) {
            if (!alloc.isPatientPortion()) {
                ccSum += alloc.getAllocatedAmount();
            }
        }
        double patientShare = expected - ccSum;
        for (CreditCompanyAllocation alloc : creditCompanyAllocations) {
            if (alloc.isPatientPortion()) {
                alloc.setAllocatedAmount(patientShare < 0 ? 0.0 : patientShare);
                return;
            }
        }
        // No patient row exists yet; add one if there is a remainder
        if (patientShare > 0.01) {
            creditCompanyAllocations.add(new CreditCompanyAllocation(patientShare, true));
        }
    }

    private void saveCCBillForAllocation(PatientEncounter pe, CreditCompanyAllocation alloc) {
        // Patient co-payment rows are NOT saved as CC commitment bills.
        // The patient settles their share via the normal inward payment flow.
        if (alloc.isPatientPortion()) {
            return;
        }
        if (alloc.getEncounterCreditCompany() != null) {
            saveCCBill(pe, alloc.getEncounterCreditCompany(), alloc.getAllocatedAmount());
        } else {
            saveCCBillByInstitution(pe, alloc.getCreditCompany(), alloc.getAllocatedAmount());
        }
    }

    /**
     * Closes off any timed service still running at discharge, stopping it at
     * the discharge time and pricing it for that duration.
     * <p>
     * This used to be a hard block ("Please Finalize Patient Timed Service")
     * that made staff go back and stop each service by hand. Stopping them at
     * the discharge time is what that manual step amounted to anyway, and doing
     * it here guarantees the charge is priced for the real length of stay
     * instead of whatever stale value was last persisted.
     */
    private void finalizeRunningTimedServices(Date dischargeTime) {
        if (childPatientEncouters == null || childPatientEncouters.isEmpty()) {
            childPatientEncouters = getInwardBean().fetchChildPatientEncounter(getPatientEncounter());
        }
        List<PatientItem> running = getInwardBean().fetchRunningTimedPatientItems(getPatientEncounter(), childPatientEncouters);
        if (running == null || running.isEmpty()) {
            return;
        }

        int closed = 0;
        for (PatientItem pi : running) {
            if (pi.getBillItem() != null && pi.getBillItem().isFromPackage()) {
                continue;
            }
            if (pi.getFromTime() != null && dischargeTime.before(pi.getFromTime())) {
                continue;
            }
            // getTimedItemFee never returns null — it hands back an empty fee
            // with durationHours = 0, which would price the service at zero.
            // Check for a real fee row instead, and skip rather than wipe the
            // charge of a service nobody has configured a price for.
            if (getInwardBean().getAllTimedItemFees((TimedItem) pi.getItem()).isEmpty()) {
                continue;
            }
            pi.setToTime(dischargeTime);
            // Priced through calTotalTimedChargeForItem, the same path the
            // manual stop uses, so tiered fee blocks and foreigner rates give
            // the same amount whether a service is stopped by hand or here.
            // The foreigner flag comes from the item's own encounter, which for
            // a baby's service is the child encounter, not the mother's.
            PatientEncounter owner = pi.getPatientEncounter() != null
                    ? pi.getPatientEncounter() : getPatientEncounter();
            pi.setServiceValue(getInwardBean().calTotalTimedChargeForItem(
                    (TimedItem) pi.getItem(), pi.getFromTime(), pi.getToTime(),
                    owner.isForiegner()));
            getPatientItemFacade().edit(pi);
            syncTimedServiceCharge(pi);
            closed++;
        }

        if (closed > 0) {
            patientItems = null;
            JsfUtil.addSuccessMessage(closed + " running timed service(s) were stopped at the discharge time.");
        }
    }

    /**
     * Pushes a recalculated timed-service charge onto its BillItem and Bill, so
     * the inward totals (which sum the BillItem side) never read a stale
     * duration. Package-locked items keep their fixed price.
     * <p>
     * The discount is read from the BillItem, not the PatientItem. The BillItem
     * is the side the discount routines clear when no price matrix applies, and
     * the bulk clear cannot reach the PatientItem (it filters on
     * {@code billItem is null}). Taking the discount from the PatientItem would
     * silently re-apply one that had just been removed. The PatientItem is
     * mirrored back so the breakdown screens still agree with the bill.
     */
    private void syncTimedServiceCharge(PatientItem patientItem) {
        if (patientItem == null || patientItem.getBillItem() == null) {
            return;
        }
        BillItem bi = patientItem.getBillItem();
        if (bi.isFromPackage()) {
            return;
        }
        double discount = bi.getDiscount();
        bi.setGrossValue(patientItem.getServiceValue());
        bi.setNetValue(patientItem.getServiceValue() + bi.getMarginValue() - discount);
        bi.setFromTime(patientItem.getFromTime());
        bi.setToTime(patientItem.getToTime());
        getBillItemFacade().edit(bi);

        if (patientItem.getDiscount() != discount) {
            patientItem.setDiscount(discount);
            getPatientItemFacade().edit(patientItem);
        }

        Bill b = bi.getBill();
        if (b != null) {
            b.setTotal(bi.getGrossValue());
            b.setNetTotal(bi.getNetValue());
            getBillFacade().edit(b);
        }
    }

    public void dischargeCancel() {

        if (getPatientEncounter().isDischarged() == false) {
            JsfUtil.addErrorMessage("There is no discharge to cancel");
            return;
        }

        if (getPatientEncounter().getCurrentPatientRoom() != null) {
            if (getPatientEncounter().getCurrentPatientRoom().getDischargedAt() == getPatientEncounter().getDateOfDischarge()) {
                getPatientEncounter().getCurrentPatientRoom().setDischargedAt(null);
                getPatientRoomFacade().edit(getPatientEncounter().getCurrentPatientRoom());
            }
        }

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("discharged", patientEncounter.isDischarged());
        before.put("dateOfDischarge", patientEncounter.getDateOfDischarge());

        patientEncounter.setDischarged(false);
        patientEncounter.setDateOfDischarge(null);
        getPatientEncounterFacade().edit(patientEncounter);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("discharged", patientEncounter.isDischarged());
        after.put("dateOfDischarge", patientEncounter.getDateOfDischarge());
        auditService.logEncounterAudit(patientEncounter, "Discharge Cancelled",
                before, after, sessionController.getLoggedUser());

        JsfUtil.addSuccessMessage("Discharge Cancelled Successfully");

    }

    public void addVat() {
        if (getPatientEncounter() == null) {
            return;
        }
        Double rc = 0.0;
        List<ChargeItemTotal> cts = getChargeItemTotals();
        for (ChargeItemTotal ci : cts) {
            if (ci.getInwardChargeType() == RoomCharges) {
                rc = ci.getNetTotal();
            }
        }

        String j = "select i from Item i where i.inwardChargeType=:ict and i.retired=false order by i.id desc";
        Map m = new HashMap();
        m.put("ict", InwardChargeType.VAT);
        Item i = getItemFacade().findFirstByJpql(j, m);

        if (i == null) {
            JsfUtil.addErrorMessage("No VAT service");
            return;
        } else {

        }

    }

    public void discharge() {
        if (getPatientEncounter() == null) {
            return;
        }

        if (getPatientEncounter().getParentEncounter() != null) {
            JsfUtil.addErrorMessage("Final bills can only be settled for the parent (mother) encounter. "
                    + "Settle it from the mother's admission — it will automatically include this baby's charges.");
            return;
        }

        if (getPatientEncounter().isDischarged()) {
            JsfUtil.addErrorMessage("Patient Already Discharged");
            return;
        }

        if (date == null) {
            JsfUtil.addErrorMessage("Please Enter the Date");
            return;
        }

        if (checkDischargeTime()) {
            return;
        }

        finalizeRunningTimedServices(date);

        if (!getPatientEncounter().isClinicallyDischarged()) {
            JsfUtil.addErrorMessage("Warning: Clinical discharge has not been confirmed for this patient.");
        }

        if (getPatientEncounter().getAdmissionType() != null
                && getPatientEncounter().getAdmissionType().isRoomChargesAllowed()) {
            // Re-fetch the encounter from the DB/L2 cache to get the authoritative currentPatientRoom FK.
            // The session-scoped patientEncounter may be stale: a room change via RoomChangeController
            // updates PatientEncounter.currentPatientRoom in the DB but not in this session object,
            // so reading currentPatientRoom directly from the session would check the OLD room (which
            // is already discharged) and incorrectly let the guard pass while the new room is still active.
            PatientEncounter freshEncounter = patientEncounterFacade.find(getPatientEncounter().getId());
            PatientRoom currentRoom = freshEncounter != null ? freshEncounter.getCurrentPatientRoom() : null;
            if (currentRoom != null && currentRoom.getDischargedAt() == null) {
                JsfUtil.addErrorMessage("Cannot discharge patient: the current room has not been discharged. " + "Please discharge the room first to record an accurate billing end time.");
                return;
            }
        }

        getPatientEncounter().setDateOfDischarge(date);
        getDischargeController().setCurrent((Admission) getPatientEncounter());
        getDischargeController().discharge();

        JsfUtil.addSuccessMessage("Patient  Discharged");

        setPatientEncounter(getPatientEncounter());
        createTables();
    }

    private boolean errorCheck() {
        if (getPatientEncounter() == null) {
            return true;
        }

        if (getPatientEncounter().isPaymentFinalized() && !creatingNewVersion) {
            JsfUtil.addErrorMessage("Payment is Finalized U need to cancel Previuios Final Bill of This Bht");
            return true;
        }

        if (checkCatTotal()) {
            return true;
        }

        if (checkCreditAllocationTotal()) {
            return true;
        }

        if (discount > grantTotal) {
            JsfUtil.addErrorMessage("Total discount (" + discount + ") exceeds total charges (" + grantTotal + ")");
            return true;
        }

        return false;

    }

    private boolean checkCatTotal() {
        double tot = 0.0;
        double tot2 = 0.0;
        for (ChargeItemTotal cit : getChargeItemTotals()) {
            tot += cit.getTotal();
            tot2 += cit.getAdjustedTotal();
        }

        double different = Math.abs((tot - tot2));

        if (different > 0.1) {
            if (configOptionApplicationController.getBooleanValueByKey("Block Inward Final Bill When Category Adjusted Total Differs From Actual Total", true)) {
                JsfUtil.addErrorMessage("There is a difference between actual and adjusted values.");
                JsfUtil.addErrorMessage("Please Adjust category amount correctly.");
                return true;
            }
        }
        return false;
    }

    @Inject
    IntrimPrintController intrimPrintController;

    public IntrimPrintController getIntrimPrintController() {
        return intrimPrintController;
    }

    public void setIntrimPrintController(IntrimPrintController intrimPrintController) {
        this.intrimPrintController = intrimPrintController;
    }

    public String toPrintItrim() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;
        //  makeNull();
        getIntrimPrintController().makeNull();
        if (getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("Please Select Patient Encounter");
            return "";
        }

        createTables();

        getIntrimPrintController().getCurrentBill().setPatientEncounter(getPatientEncounter());
        getIntrimPrintController().getCurrentBill().setTotal(grantTotal);
        getIntrimPrintController().getCurrentBill().setPaidAmount(paid);
        getIntrimPrintController().getCurrentBill().setAdjustedTotal(grantTotal);

        for (ChargeItemTotal cit : chargeItemTotals) {
            BillItem billItem = new BillItem();
            billItem.setInwardChargeType(cit.getInwardChargeType());
            billItem.setBill(getIntrimPrintController().getCurrentBill());
            billItem.setGrossValue(cit.getTotal());
            billItem.setAdjustedValue(cit.getTotal());
            billItem.setReferanceBillItem(getBillBean().fetchBillItem(patientEncounter, BillType.InwardIntrimBill, cit.getInwardChargeType()));
            getIntrimPrintController().getCurrentBill().getBillItems().add(billItem);
        }

        return "inward_bill_intrim_print";
    }

    public boolean checkBill() {
        if (configOptionApplicationController.getBooleanValueByKey("Need to check inward bills before discharge")) {
            return false;
        }

        if (getInwardBean().checkByBillFee(getPatientEncounter(), new BilledBill(), BillType.InwardBill)) {
            JsfUtil.addErrorMessage("Some Inward Service Bills Are Not Checked ");
            return true;
        }

//        if (getInwardBean().checkByBillFee(getPatientEncounter(), new RefundBill(), BillType.InwardBill)) {
//            JsfUtil.addErrorMessage("Some Inward Service Bills Are Not Checked ");
//            return true;
//        }
        if (getInwardBean().checkByBillFee(getPatientEncounter(), new BilledBill(), BillType.InwardProfessional)) {
            JsfUtil.addErrorMessage("Some Inward Pro Bills Are Not Checked ");
            return true;
        }

//        if (getInwardBean().checkByBillFee(getPatientEncounter(), new RefundBill(), BillType.InwardProfessional)) {
//            JsfUtil.addErrorMessage("Some Inward Pro Bills Are Not Checked ");
//            return true;
//        }
        if (getInwardBean().checkByBillItem(getPatientEncounter(), new PreBill(), BillType.PharmacyBhtPre)) {
            JsfUtil.addErrorMessage("Some Pharmacy Issue Bills Are Not Checked 1 ");
            return true;
        }

        if (getInwardBean().checkByBillItem(getPatientEncounter(), new RefundBill(), BillType.PharmacyBhtPre)) {
            JsfUtil.addErrorMessage("Some Pharmacy Issue Bills Are Not Checked 2 ");
            return true;
        }

        if (getInwardBean().checkByBillItem(getPatientEncounter(), new PreBill(), BillType.StoreBhtPre)) {
            JsfUtil.addErrorMessage("Some Store Issue Bills Are Not Checked 1");
            return true;
        }

        if (getInwardBean().checkByBillItem(getPatientEncounter(), new RefundBill(), BillType.StoreBhtPre)) {
            JsfUtil.addErrorMessage("Some Store Issue Bills Are Not Checked 2");
            return true;
        }

        if (getInwardBean().checkByBillItem(getPatientEncounter(), new BilledBill(), BillType.InwardOutSideBill)) {
            JsfUtil.addErrorMessage("Some Inward Out Side Bills Are Not Checked ");
            return true;
        }

//        if (getInwardBean().checkByBillItem(getPatientEncounter(), new RefundBill(), BillType.InwardOutSideBill)) {
//            JsfUtil.addErrorMessage("Some Inward Out Side Bills Are Not Checked ");
//            return true;
//        }
        if (getInwardBean().checkByBillItem(getPatientEncounter(), new BilledBill(), BillType.InwardPaymentBill)) {
            JsfUtil.addErrorMessage("Some Inward Payment Bills Are Not Checked ");
            return true;
        }

        if (getInwardBean().checkByBillItem(getPatientEncounter(), new RefundBill(), BillType.InwardPaymentBill)) {
            JsfUtil.addErrorMessage("Some Inward Payment Bills Are Not Checked ");
            return true;
        }

        return false;
    }

    public String toSettle() {

        if (getPatientEncounter() == null) {
            return "";
        }

        if (!getPatientEncounter().isDischarged()) {
            JsfUtil.addErrorMessage(" Please Discharge This Patient ");
            return "";
        }

        if (getPatientEncounter().getAdmissionType() == null) {
            return "";
        }

        System.out.println("Patient Encounter = " + getPatientEncounter());
        System.out.println("Admission Type = " + getPatientEncounter().getAdmissionType());
        System.out.println("Admission Type Enum = " + getPatientEncounter().getAdmissionType().getAdmissionTypeEnum());

        System.out.println("Match = " + (getPatientEncounter().getAdmissionType().getAdmissionTypeEnum() == AdmissionTypeEnum.Admission));

        System.out.println("Privilege = " + getWebUserController().hasPrivilege("InwardBillSettleWithoutCheck"));

        System.out.println("Option = " + configOptionApplicationController.getBooleanValueByKey("Need to check inward bills before discharge"));

        System.out.println("Starting Bills Checking Process.... ");
        if (getPatientEncounter().getAdmissionType().getAdmissionTypeEnum() == AdmissionTypeEnum.Admission && !getWebUserController().hasPrivilege("InwardBillSettleWithoutCheck")) {
            System.out.println("Checking.... ---> ");
            if (checkBill()) {
                return "";
            }
        } else {
            System.out.println("Ignore Checking.... ---> ");
        }

        System.out.println("End Bills Checking Process.... ");

        if (getPatientEncounter().getPaymentMethod() == PaymentMethod.Credit) {
            if (getPatientEncounter().getCreditCompany() == null) {
                JsfUtil.addErrorMessage("Payment method is Credit So Please Select Credit Company");
            }
        }

        childPatientEncouters = getInwardBean().fetchChildPatientEncounter(patientEncounter);
        createTables();

        if (configOptionApplicationController.getBooleanValueByKey("Professional Fee and Assisting Fees are shown as one charge type on the final bill.", false)) {
            // Both lists already have feeAdjusted = feeValue (set by setProfesionallFeeAdjusted /
            // setAssistingFeeAdjusted in createTables), so the merged list shows matching adjusted values.
            allDoctorCharges = new ArrayList<>();
            allDoctorCharges.addAll(profesionallFee);
            allDoctorCharges.addAll(doctorAndNurseFee);

            profesionallFee.clear();
            doctorAndNurseFee.clear();

            profesionallFee.addAll(allDoctorCharges);

            allDoctorCharges.clear();

            createChargeItemTotals();
        }

        calculateDiscount();
        updateTotal();
        settleOriginalBill();
        return "inward_bill_final?faces-redirect=true";

    }

    private void saveBill() {

        getCurrent().setGrantTotal(grantTotal);
        getCurrent().setTotal(grantTotal);
        getCurrent().setDiscount(discount);
        getCurrent().setNetTotal(grantTotal - discount);
        getCurrent().setPaidAmount(paid);
        getCurrent().setClaimableTotal(adjustedTotal);
        getCurrent().setSettledAmountBySponsor(paidByCompany);
        getCurrent().setSettledAmountByPatient(paidByPatient);
        getCurrent().setPaymentMethod(getPatientEncounter().getPaymentMethod());
        getCurrent().setCreditCompany(getPatientEncounter().getCreditCompany());
        getCurrent().setInstitution(patientEncounter.getInstitution());
        getCurrent().setDepartment(patientEncounter.getDepartment());
        getCurrent().setBillTypeAtomic(BillTypeAtomic.INWARD_FINAL_BILL);
        getCurrent().setBillType(BillType.InwardFinalBill);

        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());
        getCurrent().setPatientEncounter(patientEncounter);
        getCurrent().setPatient(patientEncounter.getPatient());
//        getCurrent().setMembershipScheme(membershipSchemeController.fetchPatientMembershipScheme(patientEncounter.getPatient(), getSessionController().getApplicationPreference().isMembershipExpires()));
        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());

        writeDiscountBreakdownToFinanceDetails(getCurrent());

        // Version-serial assignment and persist are done under a per-encounter lock
        // (held for the read-count-then-write span, not just the read) so two
        // concurrent final-bill saves for the same admission cannot both compute
        // the same serial before either insert commits.
        getBillNumberBean().withFinalBillVersionLock(patientEncounter, () -> {
            int versionSerial = getBillNumberBean().computeNextFinalBillVersionSerial(patientEncounter);
            getCurrent().setFinalBillVersionSerial(versionSerial);
            getCurrent().setDeptId(getBillNumberBean().departmentBillNumberGenerator(patientEncounter.getDepartment(), BillType.InwardFinalBill, BillClassType.BilledBill, BillNumberSuffix.INWFINAL) + "/" + versionSerial);
            getCurrent().setInsId(getBillNumberBean().institutionBillNumberGenerator(patientEncounter.getInstitution(), BillType.InwardFinalBill, BillClassType.BilledBill, BillNumberSuffix.INWFINAL) + "/" + versionSerial);

            if (getCurrent().getId() == null) {
                getBillFacade().create(getCurrent());
            } else {
                getBillFacade().edit(getCurrent());
            }
            return null;
        });
    }

    private void writeDiscountBreakdownToFinanceDetails(Bill bill) {
        BillFinanceDetails bfd = bill.getBillFinanceDetails();
        bfd.setBillDiscount(BigDecimal.valueOf(billLevelDiscount));
        bfd.setLineDiscount(BigDecimal.valueOf(itemDiscountTotal + chargeTypeDiscountTotal));
        bfd.setTotalDiscount(BigDecimal.valueOf(discount));
    }

    private void saveTempBill() {

        getTempBill().setGrantTotal(grantTotal);
        getTempBill().setTotal(grantTotal);
        getTempBill().setDiscount(discount);
        getTempBill().setNetTotal(grantTotal - discount);
        getTempBill().setPaidAmount(paid);
        getTempBill().setClaimableTotal(adjustedTotal);
        getTempBill().setSettledAmountBySponsor(paidByCompany);
        getTempBill().setSettledAmountByPatient(paidByPatient);
        getTempBill().setPaymentMethod(getPatientEncounter().getPaymentMethod());
        getTempBill().setCreditCompany(getPatientEncounter().getCreditCompany());
        getTempBill().setInstitution(getSessionController().getInstitution());
        getTempBill().setDeptId("Temp/Final Bill");
        getTempBill().setInsId("Temp/Final Bill");

        getTempBill().setBillDate(new Date());
        getTempBill().setBillTime(new Date());
        getTempBill().setPatientEncounter(patientEncounter);
        getTempBill().setPatient(patientEncounter.getPatient());
//        getCurrent().setMembershipScheme(membershipSchemeController.fetchPatientMembershipScheme(patientEncounter.getPatient(), getSessionController().getApplicationPreference().isMembershipExpires()));
        getTempBill().setCreatedAt(new Date());
        getTempBill().setCreater(getSessionController().getLoggedUser());

        writeDiscountBreakdownToFinanceDetails(getTempBill());
    }

    private void saveOriginalBill() {

        getOriginalBill().setGrantTotal(grantTotal);
        getOriginalBill().setTotal(grantTotal);
        getOriginalBill().setDiscount(discount);
        getOriginalBill().setNetTotal(grantTotal - discount);
        getOriginalBill().setPaidAmount(paid);
        getOriginalBill().setClaimableTotal(adjustedTotal);
        getOriginalBill().setSettledAmountBySponsor(paidByCompany);
        getOriginalBill().setSettledAmountByPatient(paidByPatient);
        getOriginalBill().setPaymentMethod(getPatientEncounter().getPaymentMethod());
        getOriginalBill().setCreditCompany(getPatientEncounter().getCreditCompany());
        getOriginalBill().setInstitution(patientEncounter.getInstitution());
        getOriginalBill().setDepartment(patientEncounter.getDepartment());
        getOriginalBill().setBillTypeAtomic(BillTypeAtomic.INWARD_ORIGINAL_FINAL_BILL);
        getOriginalBill().setDeptId(getBillNumberBean().departmentBillNumberGenerator(patientEncounter.getDepartment(), BillType.InwardOriginalFinalBill, BillClassType.BilledBill, BillNumberSuffix.INWFINALORG));
        getOriginalBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(patientEncounter.getInstitution(), BillType.InwardOriginalFinalBill, BillClassType.BilledBill, BillNumberSuffix.INWFINALORG));

        getOriginalBill().setBillType(BillType.InwardOriginalFinalBill);

        getOriginalBill().setBillDate(new Date());
        getOriginalBill().setBillTime(new Date());
        getOriginalBill().setPatientEncounter(patientEncounter);
        getOriginalBill().setPatient(patientEncounter.getPatient());
//        getCurrent().setMembershipScheme(membershipSchemeController.fetchPatientMembershipScheme(patientEncounter.getPatient(), getSessionController().getApplicationPreference().isMembershipExpires()));
        getOriginalBill().setCreatedAt(new Date());
        getOriginalBill().setCreater(getSessionController().getLoggedUser());

        writeDiscountBreakdownToFinanceDetails(getOriginalBill());
        if (getOriginalBill().getId() == null) {
            getBillFacade().create(getOriginalBill());
        } else {
            getBillFacade().edit(getOriginalBill());
        }
    }

    private void saveCCBill(PatientEncounter pe, EncounterCreditCompany ecc, Double value) {

        Bill creditCompanyBill = new BilledBill();

        creditCompanyBill.setGrantTotal(value);
        creditCompanyBill.setTotal(value);
        creditCompanyBill.setNetTotal(value);
        creditCompanyBill.setInstitution(patientEncounter.getInstitution());
        creditCompanyBill.setDepartment(patientEncounter.getDepartment());
        creditCompanyBill.setCreditCompany(ecc.getInstitution());
        creditCompanyBill.setPaymentMethod(PaymentMethod.Credit);

        creditCompanyBill.setDeptId(getBillNumberBean().departmentBillNumberGenerator(patientEncounter.getDepartment(), BillType.InwardFinalBillCCPayment, BillClassType.BilledBill, BillNumberSuffix.INWFINALCCPAY));
        creditCompanyBill.setInsId(getBillNumberBean().institutionBillNumberGenerator(patientEncounter.getInstitution(), BillType.InwardFinalBillCCPayment, BillClassType.BilledBill, BillNumberSuffix.INWFINALCCPAY));

        creditCompanyBill.setBillType(BillType.InwardFinalBillCCPayment);
        creditCompanyBill.setBillTypeAtomic(BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);

        creditCompanyBill.setBillDate(new Date());
        creditCompanyBill.setBillTime(new Date());
        creditCompanyBill.setPatientEncounter(patientEncounter);
        creditCompanyBill.setPatient(patientEncounter.getPatient());
//        getCurrent().setMembershipScheme(membershipSchemeController.fetchPatientMembershipScheme(patientEncounter.getPatient(), getSessionController().getApplicationPreference().isMembershipExpires()));
        creditCompanyBill.setCreatedAt(new Date());
        creditCompanyBill.setCreater(getSessionController().getLoggedUser());
        creditCompanyBill.setReferenceBill(getCurrent());

        if (creditCompanyBill.getId() == null) {
            getBillFacade().create(creditCompanyBill);
        } else {
            getBillFacade().edit(creditCompanyBill);
        }
    }

    private void saveCCBillByInstitution(PatientEncounter pe, Institution company, Double value) {

        Bill creditCompanyBill = new BilledBill();

        creditCompanyBill.setGrantTotal(value);
        creditCompanyBill.setTotal(value);
        creditCompanyBill.setNetTotal(value);
        creditCompanyBill.setInstitution(patientEncounter.getInstitution());
        creditCompanyBill.setDepartment(patientEncounter.getDepartment());
        creditCompanyBill.setCreditCompany(company);
        creditCompanyBill.setPaymentMethod(PaymentMethod.Credit);

        creditCompanyBill.setDeptId(getBillNumberBean().departmentBillNumberGenerator(patientEncounter.getDepartment(), BillType.InwardFinalBillCCPayment, BillClassType.BilledBill, BillNumberSuffix.INWFINALCCPAY));
        creditCompanyBill.setInsId(getBillNumberBean().institutionBillNumberGenerator(patientEncounter.getInstitution(), BillType.InwardFinalBillCCPayment, BillClassType.BilledBill, BillNumberSuffix.INWFINALCCPAY));

        creditCompanyBill.setBillType(BillType.InwardFinalBillCCPayment);
        creditCompanyBill.setBillTypeAtomic(BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);

        creditCompanyBill.setBillDate(new Date());
        creditCompanyBill.setBillTime(new Date());
        creditCompanyBill.setPatientEncounter(patientEncounter);
        creditCompanyBill.setPatient(patientEncounter.getPatient());
        creditCompanyBill.setCreatedAt(new Date());
        creditCompanyBill.setCreater(getSessionController().getLoggedUser());
        creditCompanyBill.setReferenceBill(getCurrent());

        if (creditCompanyBill.getId() == null) {
            getBillFacade().create(creditCompanyBill);
        } else {
            getBillFacade().edit(creditCompanyBill);
        }
    }

//    public void edit
    // private void saveAdmissionBillFee
    private void saveBillItem() {
        double temProfFee = 0;
        double temHosFee = 0.0;
        for (ChargeItemTotal cit : chargeItemTotals) {
            BillItem temBi = new BillItem();
            temBi.setBill(getCurrent());
            temBi.setInwardChargeType(cit.getInwardChargeType());
            temBi.setGrossValue(cit.getTotal());
            temBi.setDiscount(cit.getDiscount() + cit.getChargeTypeDiscount());
            temBi.setChargeTypeDiscount(cit.getChargeTypeDiscount());
            temBi.setNetValue(cit.getNetTotal());
            temBi.setAdjustedValue(cit.getAdjustedTotal());
            temBi.setDescreption(cit.getComments());
            temBi.setCreatedAt(new Date());
            temBi.setCreater(getSessionController().getLoggedUser());

            if (temBi.getId() == null) {
                getBillItemFacade().create(temBi);
            } else {
                getBillItemFacade().edit(temBi);
            }

            if (cit.getInwardChargeType() == InwardChargeType.ProfessionalCharge) {
                updateProBillFee(temBi);
                temProfFee += cit.getTotal();
            } else {
                if (configOptionApplicationController.getBooleanValueByKey("Create Professional Bill Fees For Assistant Chargers", false)) {
                    if (cit.getInwardChargeType() == InwardChargeType.DoctorAndNurses) {
                        updateProBillFeeForDocAndNeurses(temBi);
                    }
                }
                temHosFee += cit.getTotal();
            }

            if (cit.getInwardChargeType() == InwardChargeType.RoomCharges) {
                saveRoomBillFee(getPatientRooms(), temBi);
            }

            getBillItemFacade().edit(temBi);

            getCurrent().getBillItems().add(temBi);
        }

        getCurrent().setProfessionalFee(temProfFee);
        getCurrent().setHospitalFee(temHosFee);

        getBillFacade().edit(getCurrent());
    }

    private void saveTempBillItem() {
        double temProfFee = 0;
        double temHosFee = 0.0;
        for (ChargeItemTotal cit : chargeItemTotals) {
            BillItem temBi = new BillItem();
            temBi.setBill(getTempBill());
            temBi.setInwardChargeType(cit.getInwardChargeType());
            temBi.setGrossValue(cit.getTotal());
            temBi.setDiscount(cit.getDiscount() + cit.getChargeTypeDiscount());
            temBi.setChargeTypeDiscount(cit.getChargeTypeDiscount());
            temBi.setNetValue(cit.getNetTotal());
            temBi.setAdjustedValue(cit.getAdjustedTotal());
            temBi.setDescreption(cit.getComments());
            temBi.setCreatedAt(new Date());
            temBi.setCreater(getSessionController().getLoggedUser());

            if (cit.getInwardChargeType() == InwardChargeType.ProfessionalCharge) {
                updateProTempBillFee(temBi);
                temProfFee += cit.getTotal();
            } else {
                if (configOptionApplicationController.getBooleanValueByKey("Create Professional Bill Fees For Assistant Chargers", false)) {
                    if (cit.getInwardChargeType() == InwardChargeType.DoctorAndNurses) {
                        updateProTempBillFeeForDocAndNeurses(temBi);
                    }
                }
                temHosFee += cit.getTotal();
            }

            if (cit.getInwardChargeType() == InwardChargeType.RoomCharges) {
                saveTempRoomBillFee(getPatientRooms(), temBi);
            }

            getTempBill().getBillItems().add(temBi);
        }

        getTempBill().setProfessionalFee(temProfFee);
        getTempBill().setHospitalFee(temHosFee);
    }

    private void saveOriginalBillItem() {
        double temProfFee = 0;
        double temHosFee = 0.0;
        for (ChargeItemTotal cit : chargeItemTotals) {
            BillItem temBi = new BillItem();
            temBi.setBill(getOriginalBill());
            temBi.setInwardChargeType(cit.getInwardChargeType());
            temBi.setGrossValue(cit.getTotal());
            temBi.setDiscount(cit.getDiscount() + cit.getChargeTypeDiscount());
            temBi.setChargeTypeDiscount(cit.getChargeTypeDiscount());
            temBi.setNetValue(cit.getNetTotal());
            temBi.setAdjustedValue(cit.getAdjustedTotal());
            temBi.setDescreption(cit.getComments());
            temBi.setCreatedAt(new Date());
            temBi.setCreater(getSessionController().getLoggedUser());

            if (temBi.getId() == null) {
                getBillItemFacade().create(temBi);
            } else {
                getBillItemFacade().edit(temBi);
            }

            if (cit.getInwardChargeType() == InwardChargeType.ProfessionalCharge) {
                updateProBillFee(temBi);
                temProfFee += cit.getTotal();
            } else {
                temHosFee += cit.getTotal();
            }

            if (cit.getInwardChargeType() == InwardChargeType.RoomCharges) {
                saveRoomBillFee(getPatientRooms(), temBi);
            }

            getBillItemFacade().edit(temBi);

            getOriginalBill().getBillItems().add(temBi);
        }

        getOriginalBill().setProfessionalFee(temProfFee);
        getOriginalBill().setHospitalFee(temHosFee);
        getBillFacade().edit(getOriginalBill());
    }

    private List<BillFee> feesOrderedByOrderNo(List<BillFee> fees) {
        List<BillFee> ordered = new ArrayList<>(fees);
        ordered.sort(Comparator.comparingInt(BillFee::getOrderNo));
        return ordered;
    }

    /**
     * Stores ONE professional fee per doctor on the saved bill: a doctor's
     * individual fees are merged into a single new BillFee (summed feeValue and
     * feeAdjusted) attached to the final/temp bill item via referenceBillItem,
     * preserving the manual doctor order via orderNo.
     * <p>
     * The original per-encounter fees are left untouched on their
     * InwardProfessional bills, so doctor-payment/commission reports (which
     * read those bills) stay correct. The merged fees belong to the final bill
     * (InwardFinalBill), so they are distinguishable from the source fees by
     * bill type. When {@code persist} is false (temp preview) the merged fees
     * are kept in memory only.
     */
    private void addMergedDoctorFeesToProFees(List<BillFee> sourceFees, BillItem bItem, boolean persist) {
        Map<Staff, BillFee> merged = new LinkedHashMap<>();
        for (BillFee bf : feesOrderedByOrderNo(sourceFees)) {
            Staff staff = bf.getStaff();
            if (staff == null) {
                continue;
            }
            double adjusted = bf.getFeeAdjusted() != 0 ? bf.getFeeAdjusted() : bf.getFeeValue();
            BillFee m = merged.get(staff);
            if (m == null) {
                m = new BillFee();
                m.setStaff(staff);
                m.setFee(bf.getFee());
                m.setBill(bItem.getBill());
                m.setBillItem(bItem);
                m.setReferenceBillItem(bItem);
                m.setOrderNo(bf.getOrderNo());
                m.setCreatedAt(new Date());
                m.setCreater(getSessionController().getLoggedUser());
                merged.put(staff, m);
            }
            m.setFeeValue(m.getFeeValue() + bf.getFeeValue());
            m.setFeeAdjusted(m.getFeeAdjusted() + adjusted);
        }
        for (BillFee m : merged.values()) {
            if (persist) {
                getBillFeeFacade().create(m);
            }
            bItem.getProFees().add(m);
        }
    }

    private void updateProBillFee(BillItem bItem) {
        addMergedDoctorFeesToProFees(getProfesionallFee(), bItem, true);
    }

    private void updateProTempBillFee(BillItem bItem) {
        addMergedDoctorFeesToProFees(getProfesionallFee(), bItem, false);
    }

    private void updateProBillFeeForDocAndNeurses(BillItem bItem) {
        addMergedDoctorFeesToProFees(getDoctorAndNurseFee(), bItem, true);
    }

    private void updateProTempBillFeeForDocAndNeurses(BillItem bItem) {
        addMergedDoctorFeesToProFees(getDoctorAndNurseFee(), bItem, false);
    }

    private void saveRefencePatientRoom(PatientRoom pr) {
        if (pr.getId() == null) {
            getPatientRoomFacade().create(pr);
        } else {
            getPatientRoomFacade().edit(pr);
        }
    }

    private void saveRoomBillFee(List<PatientRoom> patientRooms, BillItem bItem) {
        List<BillFee> list = new ArrayList<>();
        for (PatientRoom pt : patientRooms) {
            BillFee tmp = new BillFee();
            tmp.setBill(bItem.getBill());
            tmp.setBillItem(bItem);

            saveRefencePatientRoom(pt);

            tmp.setReferencePatientRoom(pt);

            if (tmp.getId() == null) {
                getBillFeeFacade().create(tmp);
            } else {
                getBillFeeFacade().edit(tmp);
            }

            list.add(tmp);

        }

        bItem.setBillFees(list);

    }

    private void saveTempRoomBillFee(List<PatientRoom> patientRooms, BillItem bItem) {
        List<BillFee> list = new ArrayList<>();
        for (PatientRoom pt : patientRooms) {
            BillFee tmp = new BillFee();
            tmp.setBill(bItem.getBill());
            tmp.setBillItem(bItem);

            tmp.setReferencePatientRoom(pt);
            list.add(tmp);

        }

        bItem.setBillFees(list);

    }

    /**
     * Guards inward_bill_intrim.xhtml / inward_bill_intrim_estimate.xhtml
     * against being reached (e.g. via browser back/forward or a stale tab)
     * while patientEncounter still points at a baby (child) admission — closes
     * the gap where visiting any Inpatient Dashboard unconditionally mirrors
     * the current admission into patientEncounter, independent of the
     * createIntrimBillTable()/
     * createTablesWithEstimatedProfessionalFees()/navigateToIntrimBillFromPatientProfile()
     * guards. Safe to call on every page load.
     */
    public void redirectIfEncounterIsBabyAdmission() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc.isPostback()) {
            return;
        }
        if (patientEncounter == null || patientEncounter.getParentEncounter() == null) {
            return;
        }
        try {
            fc.getExternalContext().getFlash().setKeepMessages(true);
            JsfUtil.addErrorMessage("Interim/Estimated bills can only be generated for the parent (mother) encounter. "
                    + "Generate it from the mother's admission — it will automatically include this baby's charges.");
            fc.getExternalContext().redirect(
                    fc.getExternalContext().getRequestContextPath() + "/faces/inward/admission_profile.xhtml");
        } catch (java.io.IOException e) {
            // redirect failed — nothing further we can do at render time
        }
    }

    public String createIntrimBillTable() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No Admission Selected");
            return "";
        }
        if (patientEncounter.getParentEncounter() != null) {
            JsfUtil.addErrorMessage("Interim bills can only be generated for the parent (mother) encounter. "
                    + "Generate it from the mother's admission — it will automatically include this baby's charges.");
            clear();
            return "";
        }
        if (configOptionApplicationController.getBooleanValueByKey("Restrict Access to Intrim Bill if Provisional Bill is Created")) {
            if (admissionController.isAddmissionHaveProvisionalBill((Admission) patientEncounter)) {
                JsfUtil.addErrorMessage("There is a Provisional Bill For This Admission");
                clear();
                return "";
            }
        }
        childPatientEncouters = null;
        createTables();
        return "/inward/inward_bill_intrim?faces-redirect=true";
    }

    public void createTables() {
        makeNull();

        if (patientEncounter == null) {
            return;
        }

        if (childPatientEncouters == null || childPatientEncouters.isEmpty()) {
            childPatientEncouters = getInwardBean().fetchChildPatientEncounter(getPatientEncounter());
        }

        createPatientRooms();
        createPatientItems();

        if (!configOptionApplicationController.getBooleanValueByKey("Medicine, Sort by the type of department that issued it.", false)) {
            pharmacyIssues = getInwardBean().fetchIssueTable(getPatientEncounter(), BillType.PharmacyBhtPre, childPatientEncouters);
        } else {
            etuMedicineIssues = getInwardBean().fetchIssueTable(getPatientEncounter(), BillType.PharmacyBhtPre, childPatientEncouters, DepartmentType.Etu);
            pharmacyMedicineIssues = getInwardBean().fetchIssueTable(getPatientEncounter(), BillType.PharmacyBhtPre, childPatientEncouters, DepartmentType.Pharmacy);
            inwardMedicineIssues = getInwardBean().fetchIssueTable(getPatientEncounter(), BillType.PharmacyBhtPre, childPatientEncouters, DepartmentType.Inward);
            theatreMedicineIssues = getInwardBean().fetchIssueTable(getPatientEncounter(), BillType.PharmacyBhtPre, childPatientEncouters, DepartmentType.Theatre);
            storeMedicineIssues = getInwardBean().fetchIssueTable(getPatientEncounter(), BillType.PharmacyBhtPre, childPatientEncouters, DepartmentType.Store);
            inventryMedicineIssues = getInwardBean().fetchIssueTable(getPatientEncounter(), BillType.PharmacyBhtPre, childPatientEncouters, DepartmentType.Inventry);
        }

        storeIssues = getInwardBean().fetchIssueTable(getPatientEncounter(), BillType.StoreBhtPre, childPatientEncouters);
        departmentBillItems = getInwardBean().createDepartmentBillItemsOptimized(patientEncounter, null, childPatientEncouters);
        additionalChargeBill = getInwardBean().fetchOutSideBill(getPatientEncounter(), childPatientEncouters);
        getInwardBean().setProfesionallFeeAdjusted(getPatientEncounter(), childPatientEncouters);
        getInwardBean().setAssistingFeeAdjusted(getPatientEncounter(), childPatientEncouters);
        profesionallFee = getInwardBean().createProfesionallFee(getPatientEncounter(), childPatientEncouters);
        doctorAndNurseFee = getInwardBean().createDoctorAndNurseFee(getPatientEncounter(), childPatientEncouters);
        paymentBill = getInwardBean().fetchPaymentBill(getPatientEncounter(), childPatientEncouters);

        createChargeItemTotals();
        updateTotal();

        JsfUtil.addSuccessMessage("Recalculated Successfully");

        if (patientEncounter != null && patientEncounter.getDateOfDischarge() != null) {
            date = patientEncounter.getDateOfDischarge();
        } else {
            date = null;
        }

    }

    public void createTablesWithEstimatedProfessionalFees() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        makeNull();
        estimatedBillView = true;

        if (patientEncounter == null) {
            return;
        }
        if (patientEncounter.getParentEncounter() != null) {
            JsfUtil.addErrorMessage("Estimated bills can only be generated for the parent (mother) encounter. "
                    + "Generate it from the mother's admission — it will automatically include this baby's charges.");
            clear();
            return;
        }

        if (childPatientEncouters == null || childPatientEncouters.isEmpty()) {
            childPatientEncouters = getInwardBean().fetchChildPatientEncounter(getPatientEncounter());
        }

        createPatientRooms();
        createPatientItems();
        pharmacyIssues = getInwardBean().fetchIssueTable(getPatientEncounter(), BillType.PharmacyBhtPre, childPatientEncouters);
        storeIssues = getInwardBean().fetchIssueTable(getPatientEncounter(), BillType.StoreBhtPre, childPatientEncouters);
        departmentBillItems = getInwardBean().createDepartmentBillItemsOptimized(patientEncounter, null, childPatientEncouters);
        additionalChargeBill = getInwardBean().fetchOutSideBill(getPatientEncounter(), childPatientEncouters);
        getInwardBean().setProfesionallFeeAdjusted(getPatientEncounter(), childPatientEncouters);
        getInwardBean().setAssistingFeeAdjusted(getPatientEncounter(), childPatientEncouters);
        profesionallFee = getInwardBean().createProfesionallFeeEstimated(getPatientEncounter());
        doctorAndNurseFee = getInwardBean().createDoctorAndNurseFee(getPatientEncounter(), childPatientEncouters);
        paymentBill = getInwardBean().fetchPaymentBill(getPatientEncounter(), childPatientEncouters);

        createChargeItemTotals();

        updateTotal();

        if (patientEncounter != null && patientEncounter.getDateOfDischarge() != null) {
            date = patientEncounter.getDateOfDischarge();
        } else {
            date = null;
        }

    }

    private List<PatientItem> createPatientItems() {
        patientItems = getInwardBean().fetchPatientItem(getPatientEncounter(), childPatientEncouters);

        if (patientItems == null) {
            patientItems = new ArrayList<>();
        }

        for (PatientItem pi : patientItems) {
            TimedItemFee timedItemFee = getInwardBean().getTimedItemFee((TimedItem) pi.getItem());
            double count = getInwardBean().calCount(timedItemFee, pi.getFromTime(), pi.getToTime());
            pi.setServiceValue(count * timedItemFee.getFee());
        }

        return patientItems;
    }

    public List<PatientItem> getPatientItems() {
        if (patientItems == null) {
            patientItems = createPatientItems();
        }

        return patientItems;
    }

    public void finalizeService(PatientItem patientItem) {
        if (patientItem.getToTime() != null) {
            if (patientItem.getToTime().before(patientItem.getFromTime())) {
                JsfUtil.addErrorMessage("Service Not Finalize check Service Start Time & End Time");
                return;
            }
        }

        if (patientItem.getToTime() == null) {
            patientItem.setToTime(Calendar.getInstance().getTime());
        }

        TimedItemFee timedItemFee = getInwardBean().getTimedItemFee((TimedItem) patientItem.getItem());
        double count = getInwardBean().calCount(timedItemFee, patientItem.getFromTime(), patientItem.getToTime());
        patientItem.setServiceValue(count * timedItemFee.getFee());

        getPatientItemFacade().edit(patientItem);
        syncTimedServiceCharge(patientItem);

        createPatientItems();

    }

    public void makeNull() {
        changed = false;
        chargeItemTotals = null;
        billLevelDiscount = 0;
        itemDiscountTotal = 0;
        chargeTypeDiscountTotal = 0;
        grantTotal = 0.0;
        discount = 0.0;
        due = 0.0;
        paid = 0.0;
        profesionallFee = null;
        doctorAndNurseFee = null;
        allDoctorCharges = null;
        patientItems = null;
        paymentBill = null;
        postFinalPaymentBill = null;
        departmentBillItems = null;
        latestCheckedBillItemsByItem = null;
        printPreview = false;
        current = null;
        tmpPI = null;
        currentTime = null;
        toTime = null;
        patientRooms = null;
        creditCompanyAllocations = null;
        newEncounterCreditCompany = null;
        estimatedBillView = false;

        etuMedicineIssues = null;
        pharmacyMedicineIssues = null;
        inwardMedicineIssues = null;
        theatreMedicineIssues = null;
        storeMedicineIssues = null;
        inventryMedicineIssues = null;
    }

    public void onInstitutionChange() {
        patientEncounter = null;
        billBhtController.resetBillData();
        makeNull();
    }

    public void clear() {
        patientEncounter = null;
        institution = sessionController.getInstitution();
        makeNull();
    }

    public List<Admission> completeAdmissionNotFinalized(String query) {
        return admissionController.completePatientNotFinalizedByInstitution(query, institution);
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<BillItem> getSummaryOfDoctorChargers(List<BillItem> bi, PatientEncounter pe) {
        List<BillItem> newBillItems = new ArrayList<>();
        // LinkedHashMap preserves first-appearance order of each staff member, which follows
        // the orderNo ordering of proFees, so the manual drag order survives in this layout too.
        Map<Staff, BillFee> staffFeeMap = new LinkedHashMap<>();
        double totalFee = 0.0;

        for (BillItem i : bi) {
            if ((i.getInwardChargeType() == InwardChargeType.ProfessionalCharge
                    || i.getInwardChargeType() == InwardChargeType.DoctorAndNurses)
                    && i.getAdjustedValue() != 0) {
//                System.out.println("i = " + i);
//                System.out.println("i.getInwardChargeType() = " + i.getInwardChargeType());

                if (i.getProFees() == null) {
                    List<BillFee> docAndNurseFee = new ArrayList<>();
                    for (BillFee bf : getInwardBean().createDoctorAndNurseFee(pe, childPatientEncouters)) {
                        bf.setFeeAdjusted(bf.getFeeValue());
                        docAndNurseFee.add(bf);
                        i.setProFees(docAndNurseFee);
                    }
                }

                for (BillFee bf : i.getProFees()) {
                    Staff staff = bf.getStaff();
//                    System.out.println("staff = " + staff.getPerson().getNameWithTitle());
//                    System.out.println("bf.fee = " + bf.getFeeAdjusted());
//                    System.out.println("bf.feeV = " + bf.getFeeValue());

                    if (staffFeeMap.containsKey(staff)) {
                        if (bf.getFeeAdjusted() > 0) {
                            staffFeeMap.get(staff).setFeeAdjusted(staffFeeMap.get(staff).getFeeAdjusted() + bf.getFeeAdjusted());
                        } else {
                            staffFeeMap.get(staff).setFeeAdjusted(staffFeeMap.get(staff).getFeeAdjusted() + bf.getFeeValue());
                        }
                    } else {
                        BillFee newBillFee = new BillFee();
                        newBillFee.setStaff(staff);
                        if (bf.getFeeAdjusted() > 0) {
                            newBillFee.setFeeAdjusted(bf.getFeeAdjusted());
                        } else {
                            newBillFee.setFeeAdjusted(bf.getFeeValue());
                        }

                        staffFeeMap.put(staff, newBillFee);
                    }
                }

                totalFee += i.getAdjustedValue();
            }
        }

        List<BillFee> proFees = new ArrayList<>(staffFeeMap.values());

        if (!proFees.isEmpty()) {
            BillItem newBillItem = new BillItem();
            newBillItem.setInwardChargeType(InwardChargeType.ProfessionalCharge);
            newBillItem.setProFees(proFees);
            newBillItem.setAdjustedValue(totalFee);
            newBillItems.add(newBillItem);
        }

        return newBillItems;
    }

    public String navigateToIntrimBill() {
        patientEncounter = null;
        institution = sessionController.getInstitution();
        makeNull();
        return "/inward/inward_bill_intrim?faces-redirect=true";
    }

    public String navigateToIntrimBillFromPatientProfile() {
        if (patientEncounter != null && patientEncounter.getParentEncounter() != null) {
            JsfUtil.addErrorMessage("Interim bills can only be generated for the parent (mother) encounter. "
                    + "Generate it from the mother's admission — it will automatically include this baby's charges.");
            clear();
            return "";
        }
        childPatientEncouters = null;
        createTables();
        return "/inward/inward_bill_intrim?faces-redirect=true";
    }

    public String navigateToIntrimBillRefresh() {
        if (inwardPaymentController.getCurrent() != null
                && inwardPaymentController.getCurrent().getPatientEncounter() != null) {
            this.patientEncounter = inwardPaymentController.getCurrent().getPatientEncounter();
        }
        childPatientEncouters = null;
        createTables();
        return "/inward/inward_bill_intrim?faces-redirect=true";
    }

    public String toIntrimBillclear() {
        patientEncounter = null;
        makeNull();
        return "/inward/inward_bill_intrim?faces-redirect=true";
    }

    public void updateAdmissionFee(AdmissionType at) {
        getAdmissionTypeFacade().edit(at);
        createTables();
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public List<EncounterCreditCompany> getEncounterCreditCompanys() {
        if (patientEncounter == null) {
            return new ArrayList<>();
        }
        return fillCreditCompaniesByPatient(patientEncounter);
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
//        makeNull();
        this.patientEncounter = patientEncounter;
        invalidateUnifiedGanttBarsCache();
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    private com.divudi.core.entity.Institution resolveSingleCreditCompany(PatientEncounter encounter) {
        if (encounter == null) {
            return null;
        }
        String jpql = "select e from EncounterCreditCompany e where e.retired = false and e.patientEncounter = :enc";
        java.util.HashMap<String, Object> hm = new java.util.HashMap<>();
        hm.put("enc", encounter);
        List<EncounterCreditCompany> list = encounterCreditCompanyFacade.findByJpql(jpql, hm, 2);
        if (list != null && list.size() == 1) {
            return list.get(0).getInstitution();
        }
        return null;
    }

    private List<PatientRoom> createPatientRooms() {

        patientRooms = getInwardBean().fetchPatientRoomAll(getPatientEncounter(), childPatientEncouters);

        setPatientRoomData();
        // totalLinen = getInwardBean().calTotalLinen(tmp);

        return patientRooms;
    }

    private void setPatientRoomData() {
        PaymentMethod pm = getPatientEncounter().getPaymentMethod();
        PaymentScheme scheme = getPatientEncounter().getPaymentScheme();
        AdmissionType admType = getPatientEncounter().getAdmissionType();

        com.divudi.core.entity.Institution creditCompany = resolveSingleCreditCompany(getPatientEncounter());

        // Fetch all discount percentages once per recalculation (not per room)
        double roomPct = getPriceMatrixController().getInwardDiscountPercentForChargeType(pm, scheme, admType, InwardChargeType.RoomCharges, creditCompany);
        double maintainPct = getPriceMatrixController().getInwardDiscountPercentForChargeType(pm, scheme, admType, InwardChargeType.MaintainCharges, creditCompany);
        double linenPct = getPriceMatrixController().getInwardDiscountPercentForChargeType(pm, scheme, admType, InwardChargeType.LinenCharges, creditCompany);
        double nursingPct = getPriceMatrixController().getInwardDiscountPercentForChargeType(pm, scheme, admType, InwardChargeType.NursingCharges, creditCompany);
        double moPct = getPriceMatrixController().getInwardDiscountPercentForChargeType(pm, scheme, admType, InwardChargeType.MOCharges, creditCompany);
        double adminPct = getPriceMatrixController().getInwardDiscountPercentForChargeType(pm, scheme, admType, InwardChargeType.AdministrationCharge, creditCompany);
        double medicalCarePct = getPriceMatrixController().getInwardDiscountPercentForChargeType(pm, scheme, admType, InwardChargeType.MedicalCareICU, creditCompany);

        for (PatientRoom p : patientRooms) {
            if (p.getAdmittedAt() == null) {
                p.setAdmittedAt(new Date());
            }
            calculateRoomCharge(p);
            calculateMaintananceCharge(p);
            calculateLinenCharge(p);
            if (!(p instanceof GuardianRoom)) {
                calculateNursingCharge(p);
                calculateMoCharge(p);
                calculateAdministrationCharge(p);
                calculateMedicalCareCharge(p);
            }
            calculateTimedItemCharges(p);

            applyRoomChargeDiscounts(p, roomPct, maintainPct, linenPct, nursingPct, moPct, adminPct, medicalCarePct);

            getPatientRoomFacade().edit(p);
        }
    }

    private void applyRoomChargeDiscounts(PatientRoom p,
            double roomPct, double maintainPct, double linenPct, double nursingPct,
            double moPct, double adminPct, double medicalCarePct) {
        if (p.isFromPackage() && !isPackageRoomDurationExceeded(p)) {
            // Package-locked room: the price is fixed by the package, not subject
            // to PriceMatrix discount percentages while within the included duration.
            p.setDiscountRoomCharge(0.0);
            p.setDiscountMaintainCharge(0.0);
            p.setDiscountLinenCharge(0.0);
            p.setDiscountNursingCharge(0.0);
            p.setDiscountMoCharge(0.0);
            p.setDiscountAdministrationCharge(0.0);
            p.setDiscountMedicalCareCharge(0.0);
            p.setAdjustedRoomCharge(p.getCalculatedRoomCharge());
            p.setAdjustedMaintainCharge(p.getCalculatedMaintainCharge());
            p.setAjdustedLinenCharge(p.getCalculatedLinenCharge());
            p.setAjdustedNursingCharge(p.getCalculatedNursingCharge());
            p.setAdjustedMoCharge(p.getCalculatedMoCharge());
            p.setAjdustedAdministrationCharge(p.getCalculatedAdministrationCharge());
            p.setAjdustedMedicalCareCharge(p.getCalculatedMedicalCareCharge());
            return;
        }
        double roomDisc = (roomPct / 100.0) * p.getCalculatedRoomCharge();
        double maintainDisc = (maintainPct / 100.0) * p.getCalculatedMaintainCharge();
        double linenDisc = (linenPct / 100.0) * p.getCalculatedLinenCharge();
        double nursingDisc = (nursingPct / 100.0) * p.getCalculatedNursingCharge();
        double moDisc = (moPct / 100.0) * p.getCalculatedMoCharge();
        double adminDisc = (adminPct / 100.0) * p.getCalculatedAdministrationCharge();
        double medicalCareDisc = (medicalCarePct / 100.0) * p.getCalculatedMedicalCareCharge();

        p.setDiscountRoomCharge(roomDisc);
        p.setDiscountMaintainCharge(maintainDisc);
        p.setDiscountLinenCharge(linenDisc);
        p.setDiscountNursingCharge(nursingDisc);
        p.setDiscountMoCharge(moDisc);
        p.setDiscountAdministrationCharge(adminDisc);
        p.setDiscountMedicalCareCharge(medicalCareDisc);

        p.setAdjustedRoomCharge(p.getCalculatedRoomCharge() - roomDisc);
        p.setAdjustedMaintainCharge(p.getCalculatedMaintainCharge() - maintainDisc);
        p.setAjdustedLinenCharge(p.getCalculatedLinenCharge() - linenDisc);
        p.setAjdustedNursingCharge(p.getCalculatedNursingCharge() - nursingDisc);
        p.setAdjustedMoCharge(p.getCalculatedMoCharge() - moDisc);
        p.setAjdustedAdministrationCharge(p.getCalculatedAdministrationCharge() - adminDisc);
        p.setAjdustedMedicalCareCharge(p.getCalculatedMedicalCareCharge() - medicalCareDisc);
    }

    /**
     * Margin (positive or negative) from the room-category price-adjustment
     * matrix (InwardPriceAdjustment rows configured on the room facility
     * category price-matrix admin page) for a single room-related charge amount
     * of a given PatientRoom. Returns 0 when the room has no
     * department/room-category context or no matching matrix row exists.
     */
    private double roomChargeMatrixMargin(PatientRoom p, double chargeValue) {
        if (p.getRoomFacilityCharge() == null || chargeValue == 0.0) {
            return 0.0;
        }
        Department department = p.getRoomFacilityCharge().getDepartment();
        if (department == null) {
            return 0.0;
        }
        RoomCategory roomCategory = p.getRoomFacilityCharge().getRoomCategory();
        PatientEncounter encounter = getPatientEncounter();
        PaymentMethod paymentMethod = encounter == null ? null : encounter.getPaymentMethod();
        AdmissionType admissionType = encounter == null ? null : encounter.getAdmissionType();
        Institution creditCompany = resolveSingleCreditCompany(encounter);
        PriceMatrix priceMatrix = getPriceMatrixController().fetchRoomChargeMargin(department, chargeValue, paymentMethod, creditCompany, admissionType, roomCategory);
        if (priceMatrix == null) {
            return 0.0;
        }
        return (chargeValue * priceMatrix.getMargin()) / 100.0;
    }

    private void calculateLinenCharge(PatientRoom p) {

        if (p.getRoomFacilityCharge() == null || p.getCurrentLinenCharge() == 0.0) {
            p.setCalculatedLinenCharge(0);
            p.setMarginLinenCharge(0.0);
            return;
        }

        double linen = p.getCurrentLinenCharge();
        Date dischargedAt = p.getDischargedAt();
        ////System.out.println("dischargedAt = " + dischargedAt);
        ////System.out.println("linen = " + linen);
        if (dischargedAt == null) {
            dischargedAt = new Date();
        }

        double extra = p.getAddedLinenCharge();
        double calculated;
        ////System.out.println("extra = " + extra);
        if (CommonFunctions.checkToDateAreInSameDay(p.getAdmittedAt(), dischargedAt)) {
            if (p.getAdmittedAt().equals(dischargedAt)) {
                calculated = 0 + extra;
            } else {
                calculated = linen + extra;
            }
        } else {
            calculated = (linen * CommonFunctions.getDayCount(p.getAdmittedAt(), dischargedAt)) + extra;
        }

        double linenMargin = configOptionApplicationController.getBooleanValueByKey("Inward Room Price Matrix - Exclude Linen Charge", false)
                ? 0.0 : roomChargeMatrixMargin(p, calculated);
        p.setMarginLinenCharge(linenMargin);
        p.setCalculatedLinenCharge(calculated + linenMargin);
    }

    private void calculateMoCharge(PatientRoom p) {

        if (p.getRoomFacilityCharge() == null || p.getCurrentMoCharge() == 0.0) {
            p.setCalculatedMoCharge(0);
            p.setMarginMoCharge(0.0);
            return;
        }

        double calculated;
        if (!sessionController.getApplicationPreference().isInwardMoChargeCalculateInitialTime()) {
            double mo = p.getCurrentMoCharge();
            calculated = getCharge(p, mo) + p.getAddedMoCharge();
        } else {
            Date dischargedAt = p.getDischargedAt();
            long dCount = CommonFunctions.getDayCount(p.getAdmittedAt(), dischargedAt);

            if (dCount <= p.getRoomFacilityCharge().getTimedItemFee().getDurationDaysForMoCharge()) {
                calculated = p.getCurrentMoCharge() + p.getAddedMoCharge();
            } else {
                long extra = dCount - p.getRoomFacilityCharge().getTimedItemFee().getDurationDaysForMoCharge();
                calculated = (p.getCurrentMoChargeForAfterDuration() * extra) + p.getCurrentMoCharge();
            }
        }

        double moMargin = configOptionApplicationController.getBooleanValueByKey("Inward Room Price Matrix - Exclude MO Charge", false)
                ? 0.0 : roomChargeMatrixMargin(p, calculated);
        p.setMarginMoCharge(moMargin);
        p.setCalculatedMoCharge(calculated + moMargin);
    }

    private void calculateAdministrationCharge(PatientRoom p) {

        if (p.getRoomFacilityCharge() == null || p.getCurrentAdministrationCharge() == 0.0) {
            p.setCalculatedAdministrationCharge(0);
            p.setMarginAdministrationCharge(0.0);
            return;
        }

        double adm = p.getCurrentAdministrationCharge();
        double calculated = getCharge(p, adm) + p.getAddedAdministrationCharge();
        double adminMargin = configOptionApplicationController.getBooleanValueByKey("Inward Room Price Matrix - Exclude Administration Charge", false)
                ? 0.0 : roomChargeMatrixMargin(p, calculated);
        p.setMarginAdministrationCharge(adminMargin);
        p.setCalculatedAdministrationCharge(calculated + adminMargin);
    }

    private void calculateMedicalCareCharge(PatientRoom p) {

        if (p.getRoomFacilityCharge() == null || p.getCurrentMedicalCareCharge() == 0.0) {
            p.setCalculatedMedicalCareCharge(0);
            p.setMarginMedicalCareCharge(0.0);
            return;
        }

        double med = p.getCurrentMedicalCareCharge();
        double calculated = getCharge(p, med) + p.getAddedMedicalCareCharge();
        double medicalCareMargin = configOptionApplicationController.getBooleanValueByKey("Inward Room Price Matrix - Exclude Medical Care Charge", false)
                ? 0.0 : roomChargeMatrixMargin(p, calculated);
        p.setMarginMedicalCareCharge(medicalCareMargin);
        p.setCalculatedMedicalCareCharge(calculated + medicalCareMargin);
    }

    private void calculateNursingCharge(PatientRoom p) {

        if (p.getRoomFacilityCharge() == null || p.getCurrentNursingCharge() == 0) {
            p.setCalculatedNursingCharge(0);
            p.setMarginNursingCharge(0.0);
            return;
        }

        double nursing = p.getCurrentNursingCharge();
        double calculated = getCharge(p, nursing) + p.getAddedNursingCharge();
        double nursingMargin = configOptionApplicationController.getBooleanValueByKey("Inward Room Price Matrix - Exclude Nursing Charge", false)
                ? 0.0 : roomChargeMatrixMargin(p, calculated);
        p.setMarginNursingCharge(nursingMargin);
        p.setCalculatedNursingCharge(calculated + nursingMargin);
    }

    private void calculateRoomCharge(PatientRoom p) {

        if (p.getRoomFacilityCharge() == null || p.getCurrentRoomCharge() == 0) {
            p.setCalculatedRoomCharge(0);
            p.setMarginRoomCharge(0.0);
            return;
        }

        if (p.isFromPackage() && !isPackageRoomDurationExceeded(p)) {
            // Package-locked room: currentRoomCharge already holds the package's
            // fixed total for the room, not a per-block rate — do not multiply
            // it by elapsed TimedItemFee blocks while within the included duration.
            p.setCalculatedRoomCharge(p.getCurrentRoomCharge() + p.getAddedRoomCharge());
            p.setMarginRoomCharge(0.0);
            return;
        }

        double roomCharge = p.getCurrentRoomCharge();
        ////System.out.println("roomCharge = " + roomCharge);
        double calculated = getCharge(p, roomCharge) + p.getAddedRoomCharge();
        ////System.out.println("calculated = " + calculated);

        double roomMargin = roomChargeMatrixMargin(p, calculated);
        p.setMarginRoomCharge(roomMargin);

        p.setCalculatedRoomCharge(calculated + roomMargin);
    }

    private double getCharge(PatientRoom patientRoom, double value) {
        ////System.out.println("value = " + value);
        ////System.out.println("patientRoom = " + patientRoom);
        TimedItemFee timedFee = patientRoom.getRoomFacilityCharge().getTimedItemFee();
        ////System.out.println("timedFee = " + timedFee);
        Date dischargeAt = patientRoom.getDischargedAt();
        ////System.out.println("dischargeAt = " + dischargeAt);

        if (dischargeAt == null) {
            dischargeAt = new Date();
        }

        if (getPatientEncounter().getCurrentPatientRoom() == null) {
            return 0;
        }

        if (getPatientEncounter().getCurrentPatientRoom().equals(patientRoom)) {
            return value * getInwardBean().calCount(timedFee, patientRoom.getAdmittedAt(), dischargeAt);
        } else {
            //System.out.println("value * getInwardBean().calCount(timedFee, patientRoom.getAdmittedAt(), dischargeAt) = " + value * getInwardBean().calCount(timedFee, patientRoom.getAdmittedAt(), dischargeAt));
            return value * getInwardBean().calCount(timedFee, patientRoom.getAdmittedAt(), dischargeAt);
        }

    }

    private void calculateMaintananceCharge(PatientRoom p) {
        if (p.getRoomFacilityCharge() == null || p.getCurrentMaintananceCharge() == 0) {
            p.setCalculatedMaintainCharge(0);
            p.setMarginMaintainCharge(0.0);
            return;
        }
        double maintanance = p.getCurrentMaintananceCharge();
        double calculated = getCharge(p, maintanance) + p.getAddedMaintainCharge();
        double maintainMargin = configOptionApplicationController.getBooleanValueByKey("Inward Room Price Matrix - Exclude Maintenance Charge", false)
                ? 0.0 : roomChargeMatrixMargin(p, calculated);
        p.setMarginMaintainCharge(maintainMargin);
        p.setCalculatedMaintainCharge(calculated + maintainMargin);
    }

    private void calculateTimedItemCharges(PatientRoom p) {
        List<PatientRoomTimedItemCharge> charges = getInwardBean().fetchTimedItemCharges(p);
        if (charges == null || charges.isEmpty()) {
            return;
        }
        Date to = p.getDischargedAt() != null ? p.getDischargedAt() : new Date();
        for (PatientRoomTimedItemCharge tc : charges) {
            if (tc.getTimedItem() == null) {
                continue;
            }
            boolean foreigner = p.getPatientEncounter() != null && p.getPatientEncounter().isForiegner();
            double total = getInwardBean().calTotalTimedChargeForItem(tc.getTimedItem(), p.getAdmittedAt(), to, foreigner);
            tc.setCalculatedCharge(total);
            patientRoomTimedItemChargeFacade.edit(tc);
        }
    }

    public PatientRoomFacade getPatientRoomFacade() {
        return patientRoomFacade;
    }

    public void setPatientRoomFacade(PatientRoomFacade patientRoomFacade) {
        this.patientRoomFacade = patientRoomFacade;
    }

    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private ItemFacade itemFacade;

    public ServiceFacade getServiceFacade() {
        return serviceFacade;
    }

    public void setServiceFacade(ServiceFacade serviceFacade) {
        this.serviceFacade = serviceFacade;
    }

    public List<BillFee> getProfesionallFee() {
        if (profesionallFee == null) {
            profesionallFee = getInwardBean().createProfesionallFee(getPatientEncounter(), childPatientEncouters);
        }
        return profesionallFee;
    }

    public void setProfesionallFee(List<BillFee> profesionallFee) {
        this.profesionallFee = profesionallFee;
    }

    public List<Bill> getPaymentBill() {
        if (paymentBill == null) {
            paymentBill = getInwardBean().fetchPaymentBill(getPatientEncounter(), childPatientEncouters);
        }
        return paymentBill;
    }

    public void setPaymentBill(List<Bill> paymentBill) {
        this.paymentBill = paymentBill;
    }

    public List<Bill> getPostFinalPaymentBill() {
        if (postFinalPaymentBill == null) {
            postFinalPaymentBill = getInwardBean().fetchPostFinalPaymentBill(getPatientEncounter(), childPatientEncouters);
        }
        return postFinalPaymentBill;
    }

    public void setPostFinalPaymentBill(List<Bill> postFinalPaymentBill) {
        this.postFinalPaymentBill = postFinalPaymentBill;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public double getPaid() {
        return paid;
    }

    public void setPaid(double paid) {
        this.paid = paid;
    }

    public void calFinalValue() {
        grantTotal = 0;
        itemDiscountTotal = 0;
        chargeTypeDiscountTotal = 0;
        adjustedTotal = 0;
        grossTotal = 0;
        marginTotal = 0;
        vatTotal = 0;
        for (ChargeItemTotal c : getChargeItemTotals()) {
            grantTotal += c.getTotal();
            itemDiscountTotal += c.getDiscount();
            chargeTypeDiscountTotal += c.getChargeTypeDiscount();
            adjustedTotal += c.getAdjustedTotal();
            grossTotal += c.getGross();
            marginTotal += c.getMargin();
            vatTotal += c.getVat();
        }
        discount = itemDiscountTotal + chargeTypeDiscountTotal + billLevelDiscount;
    }

    double adjustedTotal = 0;
    double grossTotal = 0;
    double marginTotal = 0;
    double vatTotal = 0;

    public double getGrantTotal() {
        return grantTotal;
    }

    public void setGrantTotal(double grantTotal) {
        this.grantTotal = grantTotal;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getBillLevelDiscount() {
        return billLevelDiscount;
    }

    public void setBillLevelDiscount(double billLevelDiscount) {
        this.billLevelDiscount = billLevelDiscount;
    }

    public double getItemDiscountTotal() {
        return itemDiscountTotal;
    }

    public void setItemDiscountTotal(double itemDiscountTotal) {
        this.itemDiscountTotal = itemDiscountTotal;
    }

    public double getChargeTypeDiscountTotal() {
        return chargeTypeDiscountTotal;
    }

    public void setChargeTypeDiscountTotal(double chargeTypeDiscountTotal) {
        this.chargeTypeDiscountTotal = chargeTypeDiscountTotal;
    }

    public double getGrossTotal() {
        return grossTotal;
    }

    public void setGrossTotal(double grossTotal) {
        this.grossTotal = grossTotal;
    }

    public double getMarginTotal() {
        return marginTotal;
    }

    public void setMarginTotal(double marginTotal) {
        this.marginTotal = marginTotal;
    }

    public double getVatTotal() {
        return vatTotal;
    }

    public void setVatTotal(double vatTotal) {
        this.vatTotal = vatTotal;
    }

    public double getDue() {
        return due;
    }

    public void setDue(double due) {
        this.due = due;
    }

    public Date getCurrentTime() {
        currentTime = Calendar.getInstance().getTime();

        return currentTime;
    }

    public void setCurrentTime(Date currentTime) {
        this.currentTime = currentTime;
    }

    public PatientItemFacade getPatientItemFacade() {
        return patientItemFacade;
    }

    public void setPatientItemFacade(PatientItemFacade patientItemFacade) {
        this.patientItemFacade = patientItemFacade;
    }

    public TimedItemFeeFacade getTimedItemFeeFacade() {
        return timedItemFeeFacade;
    }

    public void setTimedItemFeeFacade(TimedItemFeeFacade timedItemFeeFacade) {
        this.timedItemFeeFacade = timedItemFeeFacade;
    }

    @Inject
    EnumController enumController;

    private void createChargeItemTotals() {
        Map<InwardChargeType, Double> previousTypeDiscounts = new HashMap<>();
        if (chargeItemTotals != null) {
            for (ChargeItemTotal old : chargeItemTotals) {
                if (old.getChargeTypeDiscount() != 0) {
                    previousTypeDiscounts.put(old.getInwardChargeType(), old.getChargeTypeDiscount());
                }
            }
        }
        chargeItemTotals = new ArrayList<>();

        for (InwardChargeType i : enumController.getInwardChargeTypesForSetting()) {
            ChargeItemTotal cit = new ChargeItemTotal();
            cit.setInwardChargeType(i);

            chargeItemTotals.add(cit);
        }

        if (getPatientEncounter() != null) {
            setKnownChargeTot();

            setServiceTotCategoryWise();

            setTimedServiceTotCategoryWise();

            setChargeValueFromAdditional();

            setGrossMarginVatBreakdown();

        }

        setNetAdjustValue();

        restoreChargeItemComments();

        for (ChargeItemTotal cit : chargeItemTotals) {
            Double previous = previousTypeDiscounts.get(cit.getInwardChargeType());
            if (previous != null) {
                cit.setChargeTypeDiscount(previous);
            }
        }

    }

    private void restoreChargeItemComments() {
        if (getPatientEncounter() == null
                || !getPatientEncounter().isPaymentFinalized()
                || getPatientEncounter().getFinalBill() == null) {
            return;
        }

        if (getPatientEncounter().getFinalBill().getBillFinanceDetails() != null
                && getPatientEncounter().getFinalBill().getBillFinanceDetails().getBillDiscount() != null) {
            billLevelDiscount = getPatientEncounter().getFinalBill().getBillFinanceDetails().getBillDiscount().doubleValue();
        }

        for (BillItem existing : getPatientEncounter().getFinalBill().getBillItems()) {
            for (ChargeItemTotal cit : chargeItemTotals) {
                if (existing.getInwardChargeType() == cit.getInwardChargeType()) {
                    cit.setComments(existing.getDescreption());
                    cit.setChargeTypeDiscount(existing.getChargeTypeDiscount());
                    break;
                }
            }
        }
    }

    private void setNetAdjustValue() {
        for (ChargeItemTotal cit : chargeItemTotals) {
            cit.setAdjustedTotal(cit.getTotal());
        }
    }

    private void setChargeValueFromAdditional() {
        // OPTIMIZED: Fetch all totals in ONE bulk query
        Map<InwardChargeType, Double> bulkTotals = getInwardBean().caltValueFromAdditionalChargeBulk(getPatientEncounter(), childPatientEncouters);

        for (ChargeItemTotal cit : chargeItemTotals) {
            double adj = bulkTotals.getOrDefault(cit.getInwardChargeType(), 0.0);
            double tot = cit.getTotal();
            cit.setTotal(tot + adj);
        }
    }

    /**
     * Populates Gross/Service Charge (Margin)/VAT on each ChargeItemTotal so
     * the "Charges" summary table and the Summary panel can show the full
     * breakdown, not just the net total. Services/investigations
     * (BillItem-backed) come from a bulk JPQL sum grouped by InwardChargeType;
     * Professional/Assisting fees (BillFee-backed, staff fee records) are
     * summed from the lists already fetched for their respective tabs.
     */
    private void setGrossMarginVatBreakdown() {
        Map<InwardChargeType, double[]> serviceBreakdown = getInwardBean().calServiceBillItemsGrossMarginVatByInwardChargeTypeBulk(getPatientEncounter(), childPatientEncouters);
        // Timed services that predate the bill-at-add change still carry their
        // charge on the PatientItem alone. They are part of the gross for their
        // charge type, so they have to be added to the BillItem-side breakdown —
        // otherwise a charge type holding both kinds would report only the
        // BillItem half as gross while Total and Net show the full amount.
        Map<InwardChargeType, Double> timedItemTotals = getInwardBean().getTimedItemFeeTotalByInwardChargeTypeBulk(getPatientEncounter(), childPatientEncouters);

        for (ChargeItemTotal cit : chargeItemTotals) {
            double[] values = serviceBreakdown.get(cit.getInwardChargeType());
            Double timedTotal = timedItemTotals.get(cit.getInwardChargeType());
            if (values != null) {
                cit.setGross(values[0] + (timedTotal != null ? timedTotal : 0.0));
                cit.setMargin(values[1]);
                cit.setVat(values[2]);
            } else {
                cit.setGross(cit.getTotal());
            }
        }

        double proGross = 0.0;
        double proMargin = 0.0;
        double proVat = 0.0;
        for (BillFee bf : getProfesionallFee()) {
            proGross += bf.getFeeGrossValue() != null ? bf.getFeeGrossValue() : bf.getFeeValue();
            proMargin += bf.getFeeMargin();
            proVat += bf.getFeeVat();
        }

        double docGross = 0.0;
        double docMargin = 0.0;
        double docVat = 0.0;
        for (BillFee bf : getDoctorAndNurseFee()) {
            docGross += bf.getFeeGrossValue() != null ? bf.getFeeGrossValue() : bf.getFeeValue();
            docMargin += bf.getFeeMargin();
            docVat += bf.getFeeVat();
        }

        boolean mergedProAndDoc = configOptionApplicationController.getBooleanValueByKey(
                "Professional Fee and Assisting Fees are shown as one charge type on the final bill.", false);

        for (ChargeItemTotal cit : chargeItemTotals) {
            if (cit.getInwardChargeType() == InwardChargeType.ProfessionalCharge) {
                if (mergedProAndDoc) {
                    cit.setGross(proGross + docGross);
                    cit.setMargin(proMargin + docMargin);
                    cit.setVat(proVat + docVat);
                } else {
                    cit.setGross(proGross);
                    cit.setMargin(proMargin);
                    cit.setVat(proVat);
                }
            } else if (cit.getInwardChargeType() == InwardChargeType.DoctorAndNurses) {
                if (mergedProAndDoc) {
                    cit.setGross(0.0);
                    cit.setMargin(0.0);
                    cit.setVat(0.0);
                } else {
                    cit.setGross(docGross);
                    cit.setMargin(docMargin);
                    cit.setVat(docVat);
                }
            } else if (cit.getInwardChargeType() == InwardChargeType.AdmissionFee) {
                cit.setGross(cit.getTotal());
                cit.setMargin(0.0);
                cit.setVat(0.0);
            }
        }
    }

    private void updateRoomChargeList() {

        for (PatientRoom rcd : patientRooms) {
            getPatientRoomFacade().edit(rcd);
        }

    }

    @Inject
    private InwardBeanController inwardBean;

    public void updateTotal() {
        calFinalValue();

        if (configOptionApplicationController.getBooleanValueByKey("Allow Final Bill Total Without Restrictions & Price Difference")) {
            grantTotal = adjustedTotal;
        }

        paidByPatient = getInwardBean().getPaidByPatientValue(getPatientEncounter());
        paidByCompany = getInwardBean().getPaidByCompanyValue(getPatientEncounter());
        paid = paidByPatient + paidByCompany;

        due = (grantTotal - discount) - paid;

        changed = false;

        if (getPatientEncounter() != null && getPatientEncounter().getPaymentMethod() == PaymentMethod.Credit) {
            populateCreditCompanyAllocations();
        }

        //Update Last Processing Details
        persistLastProcessingSnapshot();

    }

    private void persistLastProcessingSnapshot() {
        patientEncounter.setLastProcessBy(sessionController.getLoggedUser());
        patientEncounter.setLastProcessAt(new Date());
        patientEncounter.setTotalAtFinalProcessing(grantTotal);
        patientEncounter.setTotalPatientPaidAtFinalProcessing(paidByPatient);
        patientEncounter.setTotalCompanyPaidAtFinalProcessing(paidByCompany);
        patientEncounter.setDiscountAvailableAtFinalProcessing(discount);
        patientEncounter.setAmountDueAtFinalProcessing(due);
        patientEncounterFacade.edit(patientEncounter);
        System.out.println("Update Last Processing Details of " + patientEncounter.getBhtNo());
    }

    public void changeIsMade() {
        changed = true;
    }

    public void listnerDiscontAmmountChanged() {
        if (billLevelDiscount < 0) {
            billLevelDiscount = 0;
            JsfUtil.addErrorMessage("Bill level discount cannot be negative");
        }
        discount = itemDiscountTotal + chargeTypeDiscountTotal + billLevelDiscount;
        if (discount > grantTotal) {
            billLevelDiscount = 0;
            discount = itemDiscountTotal + chargeTypeDiscountTotal;
            JsfUtil.addErrorMessage("Total discount cannot exceed total charges");
        }
        due = (grantTotal - discount) - paid;
        rebuildCreditCompanyAllocations();
    }

    public boolean isShowOrginalBill() {
        return showOrginalBill;
    }

    public void setShowOrginalBill(boolean showOrginalBill) {
        this.showOrginalBill = showOrginalBill;
    }

    public String getChargeTypeLabel(com.divudi.core.data.inward.InwardChargeType type) {
        return configOptionApplicationController.getInwardChargeTypeLabel(type);
    }

    public List<ChargeItemTotal> getChargeItemTotals() {
        if (chargeItemTotals == null) {
            if (childPatientEncouters == null || childPatientEncouters.isEmpty()) {
                childPatientEncouters = getInwardBean().fetchChildPatientEncounter(getPatientEncounter());
            }
            createChargeItemTotals();
            // This lazy path runs at render time when no action recomputed the
            // totals (e.g. the page is redisplayed after a validation failure).
            // Without recomputing here the charge tables show values while the
            // summary (gross/discount/total/due) stays at 0.
            calFinalValue();
            if (getPatientEncounter() != null) {
                paidByPatient = getInwardBean().getPaidByPatientValue(getPatientEncounter());
                paidByCompany = getInwardBean().getPaidByCompanyValue(getPatientEncounter());
                paid = paidByPatient + paidByCompany;
                due = (grantTotal - discount) - paid;
            }
        }
        return chargeItemTotals;
    }

    public void onEdit(RowEditEvent event) {
    }

    /**
     * The amount the allocation rows (companies + patient) must add up to: net
     * total after all discounts, less what is already paid.
     */
    public double getCreditDueToAllocate() {
        return Math.max(0.0, (grantTotal - discount) - paidByPatient - paidByCompany);
    }

    public List<CreditCompanyAllocation> getCreditCompanyAllocations() {
        return creditCompanyAllocations;
    }

    public void setCreditCompanyAllocations(List<CreditCompanyAllocation> creditCompanyAllocations) {
        this.creditCompanyAllocations = creditCompanyAllocations;
    }

    private List<Bill> additionalChargeBill;

    private void setKnownChargeTot() {
        // Fetch all 7 PatientRoom charge sums in a single query
        Map<InwardChargeType, Double> roomSums = getInwardBean().getPatientRoomChargeSumsBulk(getPatientEncounter(), childPatientEncouters);

        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.add(BillTypeAtomic.PHARMACY_DIRECT_ISSUE);
        btas.add(BillTypeAtomic.PHARMACY_DIRECT_ISSUE_CANCELLED);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_RETURN);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_CANCELLATION);
        btas.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD);
        btas.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN);
        btas.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_RETURN);
        btas.add(BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_CANCELLATION);

        List<BillTypeAtomic> medicineCancellationBtas = new ArrayList<>();
        medicineCancellationBtas.add(BillTypeAtomic.PHARMACY_DIRECT_ISSUE_CANCELLED);
        medicineCancellationBtas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION);
        medicineCancellationBtas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_CANCELLATION);
        medicineCancellationBtas.add(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION);
        medicineCancellationBtas.add(BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_CANCELLATION);

        for (ChargeItemTotal i : chargeItemTotals) {
            switch (i.getInwardChargeType()) {
                case AdmissionFee:
                    if (getPatientEncounter().getAdmissionType() != null) {
                        i.setTotal(getInwardBean().getAdmissionCharge(getPatientEncounter(), childPatientEncouters));
                    }
                    break;
                case RoomCharges:
                    i.setTotal(roomSums.getOrDefault(InwardChargeType.RoomCharges, 0.0));
                    break;
                case MOCharges:
                    i.setTotal(roomSums.getOrDefault(InwardChargeType.MOCharges, 0.0));
                    break;
                case NursingCharges:
                    i.setTotal(roomSums.getOrDefault(InwardChargeType.NursingCharges, 0.0));
                    break;
                case MaintainCharges:
                    i.setTotal(roomSums.getOrDefault(InwardChargeType.MaintainCharges, 0.0));
                    break;
                case MedicalCareICU:
                    i.setTotal(roomSums.getOrDefault(InwardChargeType.MedicalCareICU, 0.0));
                    break;
                case AdministrationCharge:
                    i.setTotal(roomSums.getOrDefault(InwardChargeType.AdministrationCharge, 0.0));
                    break;
                case LinenCharges:
                    i.setTotal(roomSums.getOrDefault(InwardChargeType.LinenCharges, 0.0));
                    break;
                case Medicine:
                    if (!configOptionApplicationController.getBooleanValueByKey("Medicine, Sort by the type of department that issued it.", false)) {
                        i.setTotal(getInwardBean().calCostOfIssueByBill(getPatientEncounter(), btas, childPatientEncouters));
                    }
                    break;
                case CancelledReturnedMedicine:
                    if (!configOptionApplicationController.getBooleanValueByKey("Medicine, Sort by the type of department that issued it.", false)) {
                        i.setTotal(getInwardBean().calCancelledCostOfIssueByBill(getPatientEncounter(), medicineCancellationBtas, childPatientEncouters));
                    }
                    break;
                case GeneralIssuing:
                    i.setTotal(getInwardBean().calNetCostOfIssue(getPatientEncounter(), BillType.StoreBhtPre, childPatientEncouters));
                    break;
                case ProfessionalCharge:
                    if (configOptionApplicationController.getBooleanValueByKey("Professional Fee and Assisting Fees are shown as one charge type on the final bill.", false)) {
                        double professionalFee = getInwardBean().calculateProfessionalCharges(getPatientEncounter(), childPatientEncouters, estimatedBillView);
                        double assistingFee = getInwardBean().calculateDoctorAndNurseCharges(getPatientEncounter(), childPatientEncouters);
                        i.setTotal(professionalFee + assistingFee);
                    } else {
                        i.setTotal(getInwardBean().calculateProfessionalCharges(getPatientEncounter(), childPatientEncouters, estimatedBillView));
                    }
                    break;
                case DoctorAndNurses:
                    if (configOptionApplicationController.getBooleanValueByKey("Professional Fee and Assisting Fees are shown as one charge type on the final bill.", false)) {
                        i.setTotal(0.0);
                    } else {
                        i.setTotal(getInwardBean().calculateDoctorAndNurseCharges(getPatientEncounter(), childPatientEncouters));
                    }
                    break;
            }
        }

        if (configOptionApplicationController.getBooleanValueByKey("Medicine, Sort by the type of department that issued it.", false)) {
            List<DepartmentType> medicineIssueingDepartmentTypes = new ArrayList<>();
            medicineIssueingDepartmentTypes.add(DepartmentType.Etu);
            medicineIssueingDepartmentTypes.add(DepartmentType.Pharmacy);
            medicineIssueingDepartmentTypes.add(DepartmentType.Inward);
            medicineIssueingDepartmentTypes.add(DepartmentType.Theatre);
            medicineIssueingDepartmentTypes.add(DepartmentType.Store);
            medicineIssueingDepartmentTypes.add(DepartmentType.Inventry);

            for (DepartmentType dt : medicineIssueingDepartmentTypes) {
                switch (dt) {
                    case Etu:
                        ChargeItemTotal etuDrugTotal = new ChargeItemTotal();
                        etuDrugTotal.setInwardChargeType(InwardChargeType.Etu_Medicine);
                        etuDrugTotal.setTotal(getInwardBean().calCostOfIssueByBill(getPatientEncounter(), btas, childPatientEncouters, DepartmentType.Etu));
                        chargeItemTotals.add(etuDrugTotal);
                        break;
                    case Pharmacy:
                        ChargeItemTotal pharmacyDrugTotal = new ChargeItemTotal();
                        pharmacyDrugTotal.setInwardChargeType(InwardChargeType.Pharmacy_Medicine);
                        pharmacyDrugTotal.setTotal(getInwardBean().calCostOfIssueByBill(getPatientEncounter(), btas, childPatientEncouters, DepartmentType.Pharmacy));
                        chargeItemTotals.add(pharmacyDrugTotal);
                        break;
                    case Inward:
                        ChargeItemTotal inwardDrugTotal = new ChargeItemTotal();
                        inwardDrugTotal.setInwardChargeType(InwardChargeType.Inward_Medicine);
                        inwardDrugTotal.setTotal(getInwardBean().calCostOfIssueByBill(getPatientEncounter(), btas, childPatientEncouters, DepartmentType.Inward));
                        chargeItemTotals.add(inwardDrugTotal);
                        break;
                    case Theatre:
                        ChargeItemTotal theatreDrugTotal = new ChargeItemTotal();
                        theatreDrugTotal.setInwardChargeType(InwardChargeType.Theatre_Medicine);
                        theatreDrugTotal.setTotal(getInwardBean().calCostOfIssueByBill(getPatientEncounter(), btas, childPatientEncouters, DepartmentType.Theatre));
                        chargeItemTotals.add(theatreDrugTotal);
                        break;
                    case Store:
                        ChargeItemTotal storeDrugTotal = new ChargeItemTotal();
                        storeDrugTotal.setInwardChargeType(InwardChargeType.Store_Medicine);
                        storeDrugTotal.setTotal(getInwardBean().calCostOfIssueByBill(getPatientEncounter(), btas, childPatientEncouters, DepartmentType.Store));
                        chargeItemTotals.add(storeDrugTotal);
                        break;
                    case Inventry:
                        ChargeItemTotal inventryDrugTotal = new ChargeItemTotal();
                        inventryDrugTotal.setInwardChargeType(InwardChargeType.Etu_Medicine);
                        inventryDrugTotal.setTotal(getInwardBean().calCostOfIssueByBill(getPatientEncounter(), btas, childPatientEncouters, DepartmentType.Inventry));
                        chargeItemTotals.add(inventryDrugTotal);
                        break;
                    default:
                        throw new AssertionError();
                }
            }
        }
    }

    private void setServiceTotCategoryWise() {
        // OPTIMIZED: Fetch all totals in ONE bulk query instead of N separate queries
        Map<InwardChargeType, Double> bulkTotals = getInwardBean().calServiceBillItemsTotalByInwardChargeTypeBulk(getPatientEncounter(), childPatientEncouters);

        for (ChargeItemTotal ch : chargeItemTotals) {
            Double total = bulkTotals.getOrDefault(ch.getInwardChargeType(), 0.0);
            ch.setTotal(ch.getTotal() + total);
        }
    }

    public List<InwardBillItem> getInwardBillItemByType() {
        List<InwardBillItem> inwardBillItems = new ArrayList<>();
        for (InwardChargeType i : InwardChargeType.values()) {
            InwardBillItem tmp = new InwardBillItem();
            tmp.setInwardChargeType(i);
            tmp.setBillItems(getInwardBean().getService(i, getPatientEncounter()));
            inwardBillItems.add(tmp);
        }

        return inwardBillItems;

    }

    public void calculateDuration() {
        if (patientEncounter.getDateOfAdmission() != null && patientEncounter.getDateOfDischarge() != null) {
            // Convert java.util.Date to LocalDateTime
            LocalDateTime admissionDateTime = patientEncounter.getDateOfAdmission().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            LocalDateTime dischargeDateTime = patientEncounter.getDateOfDischarge().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            // Calculate duration between admission and discharge
            Duration durationObj = Duration.between(admissionDateTime, dischargeDateTime);

            long days = durationObj.toDays();
            long hours = durationObj.toHours() % 24;
            long minutes = durationObj.toMinutes() % 60;
            long seconds = durationObj.getSeconds() % 60;

            // Format the result as a string
            this.duration = String.format("%d days, %d hours, %d minutes, %d seconds", days, hours, minutes, seconds);
        }
    }

    private void setTimedServiceTotCategoryWise() {
        // OPTIMIZED: Fetch all totals in ONE bulk query
        Map<InwardChargeType, Double> bulkTotals = getInwardBean().getTimedItemFeeTotalByInwardChargeTypeBulk(getPatientEncounter(), childPatientEncouters);

        for (ChargeItemTotal ch : chargeItemTotals) {
            Double total = bulkTotals.getOrDefault(ch.getInwardChargeType(), 0.0);
            ch.setTotal(ch.getTotal() + total);
        }
    }

    public void setChargeItemTotals(List<ChargeItemTotal> chargeItemTotals) {
        this.chargeItemTotals = chargeItemTotals;
    }

    public Bill getCurrent() {
        if (current == null) {
            current = new BilledBill();
        }
        return current;
    }

    public void setCurrent(Bill current) {
        this.current = current;
    }

    public Bill getOriginalBill() {
        if (originalBill == null) {
            originalBill = new BilledBill();
        }
        return originalBill;
    }

    public void setOriginalBill(Bill originalBill) {
        this.originalBill = originalBill;
    }

    public Bill getTempBill() {
        if (tempBill == null) {
            tempBill = new BilledBill();
        }
        return tempBill;
    }

    public void setTempBill(Bill tempBill) {
        this.tempBill = tempBill;
    }

    public void prepareNewBill() {
        patientEncounter = null;
        makeNull();

    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public DischargeController getDischargeController() {
        return dischargeController;
    }

    public void setDischargeController(DischargeController dischargeController) {
        this.dischargeController = dischargeController;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public InwardTimedItemController getInwardTimedItemController() {
        return inwardTimedItemController;
    }

    public void setInwardTimedItemController(InwardTimedItemController inwardTimedItemController) {
        this.inwardTimedItemController = inwardTimedItemController;
    }

    public PatientEncounterFacade getPatientEncounterFacade() {
        return patientEncounterFacade;
    }

    public void setPatientEncounterFacade(PatientEncounterFacade patientEncounterFacade) {
        this.patientEncounterFacade = patientEncounterFacade;
    }

    public List<Bill> getAdditionalChargeBill() {
        if (additionalChargeBill == null) {
            additionalChargeBill = getInwardBean().fetchOutSideBill(getPatientEncounter(), childPatientEncouters);
        }
        return additionalChargeBill;
    }

    public void setAdditionalChargeBill(List<Bill> additionalChargeBill) {
        this.additionalChargeBill = additionalChargeBill;
    }

    public PatientItem getTmpPI() {
        return tmpPI;
    }

    public void setTmpPI(PatientItem tmpPI) {
        this.tmpPI = tmpPI;
    }

    public Date getToTime() {
        return toTime;
    }

    public void setToTime(Date toTime) {
        this.toTime = toTime;
    }

    public List<DepartmentBillItems> getDepartmentBillItems() {
        if (departmentBillItems == null) {
            departmentBillItems = getInwardBean().createDepartmentBillItemsOptimized(patientEncounter, null, childPatientEncouters);
        }
        return departmentBillItems;
    }

    public void setDepartmentBillItems(List<DepartmentBillItems> departmentBillItems) {
        this.departmentBillItems = departmentBillItems;
    }

    /**
     * Returns the most recently checked inward BillItem for the given service
     * item of the current patient encounter, or null if none has been checked.
     * The Service Details tab binds to its {@code bill.checkedBy} /
     * {@code bill.checkeAt} to show "Checked By" / "Checked At".
     */
    public BillItem getLatestCheckedBillItem(Item item) {
        if (item == null || item.getId() == null) {
            return null;
        }
        if (latestCheckedBillItemsByItem == null) {
            latestCheckedBillItemsByItem = getInwardBean().getLatestCheckedBillItemsByItem(patientEncounter);
        }
        return latestCheckedBillItemsByItem.get(item.getId());
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public InwardMemberShipDiscount getInwardMemberShipDiscount() {
        return inwardMemberShipDiscount;
    }

    public void setInwardMemberShipDiscount(InwardMemberShipDiscount inwardMemberShipDiscount) {
        this.inwardMemberShipDiscount = inwardMemberShipDiscount;
    }

    public List<Bill> getPharmacyIssues() {
        return pharmacyIssues;
    }

    public void setPharmacyIssues(List<Bill> pharmacyIssues) {
        this.pharmacyIssues = pharmacyIssues;
    }

    public List<Bill> getStoreIssues() {
        return storeIssues;
    }

    public void setStoreIssues(List<Bill> storeIssues) {
        this.storeIssues = storeIssues;
    }

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    public double getAdjustedTotal() {
        return adjustedTotal;
    }

    public void setAdjustedTotal(double adjustedTotal) {
        this.adjustedTotal = adjustedTotal;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        billItems = getInwardBean().createBillItems(item, getPatientEncounter());
        this.item = item;
    }

    public List<BillItem> getPharmacyItems() {
        return pharmacyItems;
    }

    public void setPharmacyItems(List<BillItem> pharmacyItems) {
        this.pharmacyItems = pharmacyItems;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public AdmissionTypeFacade getAdmissionTypeFacade() {
        return admissionTypeFacade;
    }

    public void setAdmissionTypeFacade(AdmissionTypeFacade admissionTypeFacade) {
        this.admissionTypeFacade = admissionTypeFacade;
    }

    public WebUserController getWebUserController() {
        return webUserController;
    }

    public void setWebUserController(WebUserController webUserController) {
        this.webUserController = webUserController;
    }

    public List<Bill> getSurgeryBills() {
        return surgeryBills;
    }

    public void setSurgeryBills(List<Bill> surgeryBills) {
        this.surgeryBills = surgeryBills;
    }

    public Bill getSurgeryBill() {
        return surgeryBill;
    }

    public void setSurgeryBill(Bill surgeryBill) {
        this.surgeryBill = surgeryBill;
    }

    public double getPaidByPatient() {
        return paidByPatient;
    }

    public void setPaidByPatient(double paidByPatient) {
        this.paidByPatient = paidByPatient;
    }

    public double getPaidByCompany() {
        return paidByCompany;
    }

    public void setPaidByCompany(double paidByCompany) {
        this.paidByCompany = paidByCompany;
    }

    public String getDuration() {
        calculateDuration();
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public boolean isPatientEncounterHasProvisionalBill() {
        return patientEncounterHasProvisionalBill;
    }

    public void setPatientEncounterHasProvisionalBill(boolean patientEncounterHasProvisionalBill) {
        this.patientEncounterHasProvisionalBill = patientEncounterHasProvisionalBill;
    }

    public List<PatientEncounter> getChildPatientEncouters() {
        return childPatientEncouters;
    }

    public void setChildPatientEncouters(List<PatientEncounter> childPatientEncouters) {
        this.childPatientEncouters = childPatientEncouters;
    }

    /**
     * Computes a detailed duration breakdown for a PatientRoom that mirrors the
     * slot-counting logic in InwardBeanController.calCount(). Used by the room
     * details page to show how the billed slot count is derived.
     */
    public RoomDurationBreakdown getRoomDurationBreakdown(PatientRoom pr) {
        if (pr == null || pr.getRoomFacilityCharge() == null
                || pr.getRoomFacilityCharge().getTimedItemFee() == null
                || pr.getAdmittedAt() == null) {
            return null;
        }
        TimedItemFee tif = pr.getRoomFacilityCharge().getTimedItemFee();
        Date dischargedAt = pr.getDischargedAt() != null ? pr.getDischargedAt() : new Date();

        long totalMinutes = CommonFunctions.calculateDurationMin(pr.getAdmittedAt(), dischargedAt);
        double slotMinutes = tif.getDurationHours() * 60.0;
        double overshootMinutes = tif.getOverShootHours() * 60.0;

        if (slotMinutes == 0) {
            return new RoomDurationBreakdown(totalMinutes, slotMinutes, 0, totalMinutes, overshootMinutes, false, 0);
        }

        long completeSlots = (long) (totalMinutes / slotMinutes);
        double remainderMinutes = totalMinutes - (completeSlots * slotMinutes);
        boolean extraSlotCharged = (overshootMinutes != 0 && overshootMinutes <= remainderMinutes) || completeSlots == 0;
        long billedSlots = completeSlots + (extraSlotCharged ? 1 : 0);

        return new RoomDurationBreakdown(totalMinutes, slotMinutes, completeSlots,
                remainderMinutes, overshootMinutes, extraSlotCharged, billedSlots);
    }

    /**
     * The room-category price-matrix margin percentage applied to this room's
     * Room Charge, derived from the already-computed calculated/margin fields
     * (set by calculateRoomCharge()). Returns null when no matrix row applied.
     */
    public Double getRoomMatrixPercent(PatientRoom pr) {
        if (pr == null || pr.getMarginRoomCharge() == 0.0) {
            return null;
        }
        double slotRate = pr.getCalculatedRoomCharge() - pr.getMarginRoomCharge();
        if (slotRate == 0.0) {
            return null;
        }
        return (pr.getMarginRoomCharge() / slotRate) * 100.0;
    }

    public List<BillFee> getAllDoctorCharges() {
        return allDoctorCharges;
    }

    public void setAllDoctorCharges(List<BillFee> allDoctorCharges) {
        this.allDoctorCharges = allDoctorCharges;
    }

    public static class RoomDurationBreakdown {

        private final long totalMinutes;
        private final double slotMinutes;
        private final long completeSlots;
        private final double remainderMinutes;
        private final double overshootMinutes;
        private final boolean extraSlotCharged;
        private final long billedSlots;

        public RoomDurationBreakdown(long totalMinutes, double slotMinutes, long completeSlots,
                double remainderMinutes, double overshootMinutes,
                boolean extraSlotCharged, long billedSlots) {
            this.totalMinutes = totalMinutes;
            this.slotMinutes = slotMinutes;
            this.completeSlots = completeSlots;
            this.remainderMinutes = remainderMinutes;
            this.overshootMinutes = overshootMinutes;
            this.extraSlotCharged = extraSlotCharged;
            this.billedSlots = billedSlots;
        }

        public long getTotalMinutes() {
            return totalMinutes;
        }

        public long getTotalHours() {
            return totalMinutes / 60;
        }

        public long getTotalRemainingMinutes() {
            return totalMinutes % 60;
        }

        public double getSlotMinutes() {
            return slotMinutes;
        }

        public double getSlotHours() {
            return slotMinutes / 60.0;
        }

        public long getCompleteSlots() {
            return completeSlots;
        }

        public double getRemainderMinutes() {
            return remainderMinutes;
        }

        public long getRemainderHours() {
            return (long) remainderMinutes / 60;
        }

        public long getRemainderRemainingMinutes() {
            return (long) remainderMinutes % 60;
        }

        public double getOvershootMinutes() {
            return overshootMinutes;
        }

        public double getOvershootHours() {
            return overshootMinutes / 60.0;
        }

        public boolean isExtraSlotCharged() {
            return extraSlotCharged;
        }

        public long getBilledSlots() {
            return billedSlots;
        }
    }

}
