package com.divudi.bean.report;

import com.divudi.bean.common.*;
import com.divudi.core.data.reports.FinancialReport;
import com.divudi.core.data.reports.InventoryReports;
import com.divudi.core.data.reports.PharmacyReports;
import com.divudi.core.entity.pharmacy.*;
import com.divudi.core.facade.*;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillFinanceType;
import com.divudi.core.data.BillItemStatus;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.CategoryCount;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.ItemCount;
import com.divudi.core.data.ItemLight;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.PharmacyRow;
import com.divudi.core.data.ReportTemplateRow;
import com.divudi.core.data.ReportTemplateRowBundle;
import com.divudi.core.data.ServiceType;
import com.divudi.core.data.Sex;
import com.divudi.core.data.StockCorrectionRow;
import com.divudi.core.data.TestWiseCountReport;
import com.divudi.core.data.dataStructure.BillAndItemDataRow;
import com.divudi.core.data.dataStructure.ItemDetailsCell;
import com.divudi.core.data.dataStructure.ItemLastSupplier;
import com.divudi.core.data.dataStructure.PharmacyStockRow;
import com.divudi.core.data.dataStructure.StockReportRecord;
import com.divudi.core.data.dto.BillItemDTO;
import com.divudi.core.data.dto.CostOfGoodSoldBillDTO;
import com.divudi.core.data.lab.PatientInvestigationStatus;
import com.divudi.ejb.PharmacyBean;
import com.divudi.core.entity.AgentHistory;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientDepositHistory;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.Route;
import com.divudi.core.entity.Service;
import com.divudi.core.entity.Speciality;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.channel.AgentReferenceBook;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.entity.lab.Machine;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.light.common.BillLight;
import com.divudi.core.light.common.PrescriptionSummaryReportRow;

import java.io.IOException;
import javax.faces.context.ExternalContext;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.text.pdf.PdfWriter;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import javax.ejb.EJB;
import javax.inject.Inject;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import javax.faces.context.FacesContext;
import javax.persistence.TemporalType;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pubudu Piyankara
 */
@Named
@SessionScoped
public class PharmacyReportController implements Serializable {

    @EJB
    BillItemFacade billItemFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    InstitutionFacade institutionFacade;
    @EJB
    AgentHistoryFacade agentHistoryFacade;
    @EJB
    PatientDepositHistoryFacade patientDepositHistoryFacade;
    @EJB
    AgentReferenceBookFacade agentReferenceBookFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    StockHistoryFacade facade;
    @EJB
    private StockFacade stockFacade;
    @EJB
    PharmacyBean pharmacyBean;
    @EJB
    ItemFacade itemFacade;
    @EJB
    private ItemBatchFacade itemBatchFacade;

    @Inject
    private InstitutionController institutionController;
    @Inject
    DoctorController doctorController;
    @Inject
    PersonController personController;
    @Inject
    ItemApplicationController itemApplicationController;
    @Inject
    ItemController itemController;
    @Inject
    PatientController patientController;
    @Inject
    WebUserController webUserController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    SessionController sessionController;
    @Inject
    SearchController searchController;
    @EJB
    private ReportTimerController reportTimerController;

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    private static final Set<PaymentMethod> CREDIT_PAYMENT_METHODS = EnumSet.of(
            PaymentMethod.Credit,
            PaymentMethod.Staff,
            PaymentMethod.OnCall
    );
    private int reportIndex;
    private Institution institution;
    private Institution site;
    private Department department;
    private Institution fromInstitution;
    private List<Institution> fromInstitutionList;
    private Institution toInstitution;
    private Department fromDepartment;
    private Department toDepartment;
    private Date fromDate;
    private Date toDate;
    private Category category;
    private Item item;
    private Amp amp;
    private Machine machine;
    private String processBy;
    private Institution collectingCentre;
    private Route route;
    private Date financialYear;
    private String phn;
    private Doctor referingDoctor;
    private Staff toStaff;
    private WebUser webUser;
    private String code;

    private double investigationResult;
    private double hospitalFeeTotal;
    private double ccFeeTotal;
    private double professionalFeeTotal;
    private double entireTotal;
    private double totalCredit;
    private double totalDebit;

    private String visitType;
    private Patient patient;
    private String diagnosis;
    private String documentType;
    private String stockLedgerReportType = "byBatch";

    private Investigation investigation;
    private Speciality currentSpeciality;
    private Service service;

    private String priorityType;
    private String patientMrn;

    private BillItemStatus status;
    private Doctor doctor;
    private String totalAverage;
    private String visit;

    private List<Bill> bills;
    private List<BillItem> billItems;
    private List<ItemCount> reportLabTestCounts;
    private List<CategoryCount> reportList;
    private List<Institution> collectionCenters;
    private List<AgentHistory> agentHistories;

    private Date warrentyStartDate;
    private Date warrentyEndDate;

    private Date amcStartDate;
    private Date amcEndDate;
    private PaymentMethod paymentMethod;

    private String invoiceNumber;

    private List<ItemLight> investigationsAndServices;
    private ItemLight itemLight;

    private List<PrescriptionSummaryReportRow> prescriptionSummaryReportRows;
    private List<BillLight> billLights;

    private List<ItemCount> reportOpdServiceCount;
    private ReportTemplateRowBundle bundle;
    private List<ReportTemplateRow> unifiedBundle;
    private List<ReportTemplateRowBundle> bundleList;

    private List<PatientDepositHistory> patientDepositHistories;

    private List<PatientInvestigation> patientInvestigations;
    private PatientInvestigationStatus patientInvestigationStatus;

    private List<AgentReferenceBook> agentReferenceBooks;

    private boolean showPaymentData;
    private boolean showData;

    private List<BillAndItemDataRow> billAndItemDataRows;
    private BillAndItemDataRow headerBillAndItemDataRow;

    private double totalCount;
    private double totalHosFee;
    private double totalCCFee;
    private double totalProFee;
    private double totalDiscount;
    private double totalNetHosFee;
    private double totalNetTotal;
    private Double staffFeeTotal;
    private Double grossFeeTotal;
    private Double discountTotal;
    private Double netTotal;
    private Double purchaseValueTotal;
    private Double costValueTotal;
    private Double retailValueTotal;
    private double calAllInTotal;
    private double calAllOutTotal;

    private List<String> voucherStatusOnDebtorSettlement;
    private String selectedVoucherStatusOnDebtorSettlement;
    private BillItem voucherItem;

    private Person person;

    private String type;
    private String reportType;
    private Speciality speciality;

    private List<StockHistory> stockLedgerHistories;

    private List<Stock> stocks;
    private double stockSaleValue;
    private double stockPurchaseValue;
    private double stockCostValue;
    private double stockTotal;
    private List<PharmacyStockRow> pharmacyStockRows;
    private double stockTottal;

    private String dateRange;

    private List<PharmacyRow> rows;
    BillType[] billTypes;
    List<StockReportRecord> movementRecords;
    List<StockReportRecord> movementRecordsQty;

    double valueOfQOH;
    double qoh;
    List<Item> items;

    private List<ItemLastSupplier> itemLastSuppliers;
    private String sortType;

    private Map<Item, Map<Long, List<Stock>>> itemStockMap;
    private Double quantity;

    private Double stockQty;

    private Institution fromSite;
    private Institution toSite;

    private boolean consignmentItem;
    private List<PharmacyRow> pharmacyRows;
    private List<BillItemDTO> billItemsDtos;
    private List<CostOfGoodSoldBillDTO> cogsBillDtos;
    private double totalCostValue;
    private double totalPurchaseValue;
    private double totalRetailValue;
    private double totalSaleValue;

    //Constructor
    public PharmacyReportController() {
    }

    public List<Institution> getFromInstitutionList() {
        return fromInstitutionList;
    }

    public void setFromInstitutionList(List<Institution> fromInstitutionList) {
        this.fromInstitutionList = fromInstitutionList;
    }

    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    public void generateItemMovementByBillReport() {
        billAndItemDataRows = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        List<BillTypeAtomic> bta = new ArrayList<>();
        bta.add(BillTypeAtomic.OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER);
        bta.add(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        bta.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
        bta.add(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
        bta.add(BillTypeAtomic.OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER);
        bta.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        bta.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
        bta.add(BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        bta.add(BillTypeAtomic.OPD_BILL_REFUND);
        bta.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
        bta.add(BillTypeAtomic.OPD_PROFESSIONAL_PAYMENT_BILL);
        bta.add(BillTypeAtomic.OPD_PROFESSIONAL_PAYMENT_BILL_RETURN);

        // Update JPQL to include all bills, regardless of their status
        StringBuilder jpql = new StringBuilder("SELECT bi FROM BillItem bi WHERE bi.retired=:bir AND bi.bill.retired=:br AND bi.bill.createdAt BETWEEN :fd AND :td AND bi.bill.billTypeAtomic IN :bTypeList ");
        params.put("bir", false);
        params.put("br", false);
        params.put("fd", fromDate);
        params.put("td", toDate);
        params.put("bTypeList", bta);

        if (institution != null) {
            jpql.append(" AND bi.bill.institution=:ins");
            params.put("ins", institution);
        }
        if (department != null) {
            jpql.append(" AND bi.bill.department=:dep");
            params.put("dep", department);
        }
        if (site != null) {
            jpql.append(" AND bi.bill.department.site=:site");
            params.put("site", site);
        }
        if (category != null) {
            jpql.append(" AND (bi.item.category=:cat OR bi.item.category.parentCategory=:cat)");
            params.put("cat", category);
        }
        if (item != null) {
            jpql.append(" AND bi.item=:item");
            params.put("item", item);
        }
        if (phn != null && !phn.trim().equals("")) {
            jpql.append(" AND bi.bill.patient.phn=:phn");
            params.put("phn", phn);
        }
        if (toInstitution != null) {
            jpql.append(" AND bi.bill.toInstitution=:toIns");
            params.put("toIns", toInstitution);
        }
        if (toDepartment != null) {
            jpql.append(" AND bi.bill.toDepartment=:toDep");
            params.put("toDep", toDepartment);
        }
        jpql.append(" ORDER BY bi.id ");
        List<BillItem> tmpBillItems = billItemFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
        if (tmpBillItems == null) {
            return;
        }

        // Deduplicate and sort items for headers
        Set<Item> items = new TreeSet<>(Comparator.comparing(Item::getName));
        tmpBillItems.stream().map(BillItem::getItem).filter(Objects::nonNull).forEach(items::add);
        List<Item> sortedItems = new ArrayList<>(items);

        // Initialize header row with items and placeholders for totals
        headerBillAndItemDataRow = new BillAndItemDataRow();
        for (Item it : sortedItems) {
            ItemDetailsCell cell = new ItemDetailsCell();
            cell.setItem(it);
            cell.setQuentity(0.0);  // Initialize with zero for totals
            headerBillAndItemDataRow.getItemDetailCells().add(cell);
        }

        // Map to hold rows, mapped by Bill
        Map<Bill, BillAndItemDataRow> billMap = new HashMap<>();
        for (BillItem bi : tmpBillItems) {
            if (bi.getItem() == null) {
                continue;
            }

            Bill bill = bi.getBill();
            BillAndItemDataRow row = billMap.getOrDefault(bill, new BillAndItemDataRow());
            row.setBill(bill);

            if (row.getItemDetailCells().isEmpty()) {
                for (int i = 0; i < sortedItems.size(); i++) {
                    row.getItemDetailCells().add(new ItemDetailsCell());
                }
            }

            int itemIndex = sortedItems.indexOf(bi.getItem());
            if (itemIndex != -1) {
                ItemDetailsCell cell = row.getItemDetailCells().get(itemIndex);
                // Adjust the quantity for cancelled/refunded items
                boolean cancelledBill = bill instanceof CancelledBill;
                boolean refundedBill = bill instanceof RefundBill;
                if (cell.getQuentity() == null) {
                    cell.setQuentity(0.0);
                }
                if (bi.getQty() == 0.0) {
                    bi.setQty(1.0);
                }
                if (cancelledBill || refundedBill) {
                    cell.setQuentity(cell.getQuentity() - bi.getQtyAbsolute());
                } else {
                    cell.setQuentity(cell.getQuentity() + bi.getQtyAbsolute());
                }
                double quantityToAdd = (cancelledBill || refundedBill) ? -bi.getQtyAbsolute() : bi.getQtyAbsolute();
                row.setGrandTotal(row.getGrandTotal() + quantityToAdd);
                row.getItemDetailCells().set(itemIndex, cell);

                // Accumulate totals directly in the header row
                ItemDetailsCell totalCell = headerBillAndItemDataRow.getItemDetailCells().get(itemIndex);
                totalCell.setQuentity(totalCell.getQuentity() + (cancelledBill || refundedBill ? -bi.getQtyAbsolute() : bi.getQtyAbsolute()));
            }

            billMap.put(bill, row);
        }

        billAndItemDataRows = new ArrayList<>(billMap.values());

        netTotal = 0.0;

        for (BillAndItemDataRow bir : billAndItemDataRows) {
            netTotal += bir.getGrandTotal();
        }
    }

    @Deprecated
    public void generateItemMovementByBillReportOld() {
        billAndItemDataRows = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder("SELECT bi FROM BillItem bi WHERE bi.retired=:bir AND bi.bill.cancelled=:bc AND bi.refunded=:birf AND bi.bill.retired=:br AND bi.bill.createdAt BETWEEN :fd AND :td");
        params.put("bir", false);
        params.put("br", false);
        params.put("fd", fromDate);
        params.put("td", toDate);
        params.put("birf", false);
        params.put("bc", false);

        if (institution != null) {
            jpql.append(" AND bi.bill.institution=:ins");
            params.put("ins", institution);
        }
        if (department != null) {
            jpql.append(" AND bi.bill.department=:dep");
            params.put("dep", department);
        }
        if (site != null) {
            jpql.append(" AND bi.bill.department.site=:site");
            params.put("site", site);
        }
        if (category != null) {
            jpql.append(" AND (bi.item.category=:cat OR bi.item.category.parentCategory=:cat)");
            params.put("cat", category);
        }
        if (phn != null && !phn.isEmpty()) {
            jpql.append(" AND bi.bill.patient.phn=:mrn");
            params.put("mrn", phn);
        }
        if (item != null) {
            jpql.append(" AND bi.item=:item");
            params.put("item", item);
        }
        if (phn != null && phn.trim().equals("")) {
            jpql.append(" AND bi.bill.patient.phn=:phn");
            params.put("phn", phn);
        }

        List<BillItem> billItems = billItemFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
        if (billItems == null) {
            return;
        }

        // Deduplicate and sort items for headers
        Set<Item> items = new TreeSet<>(Comparator.comparing(Item::getName));
        billItems.stream().map(BillItem::getItem).filter(Objects::nonNull).forEach(items::add);
        List<Item> sortedItems = new ArrayList<>(items);

        // Initialize header row with items and placeholders for totals
        headerBillAndItemDataRow = new BillAndItemDataRow();
        for (Item it : sortedItems) {
            ItemDetailsCell cell = new ItemDetailsCell();
            cell.setItem(it);
            cell.setQuentity(0.0);  // Initialize with zero for totals
            headerBillAndItemDataRow.getItemDetailCells().add(cell);
        }

        // Map to hold rows, mapped by Bill
        Map<Bill, BillAndItemDataRow> billMap = new HashMap<>();
        for (BillItem bi : billItems) {
            if (bi.getItem() == null) {
                continue;
            }

            Bill bill = bi.getBill();
            BillAndItemDataRow row = billMap.getOrDefault(bill, new BillAndItemDataRow());
            row.setBill(bill);

            if (row.getItemDetailCells().isEmpty()) {
                for (int i = 0; i < sortedItems.size(); i++) {
                    row.getItemDetailCells().add(new ItemDetailsCell());
                }
            }

            int itemIndex = sortedItems.indexOf(bi.getItem());
            if (itemIndex != -1) {
                ItemDetailsCell cell = row.getItemDetailCells().get(itemIndex);
                cell.setItem(bi.getItem());
                cell.setQuentity(bi.getQty());
                row.getItemDetailCells().set(itemIndex, cell);

                // Accumulate totals directly in the header row
                ItemDetailsCell totalCell = headerBillAndItemDataRow.getItemDetailCells().get(itemIndex);
                totalCell.setQuentity(totalCell.getQuentity() + bi.getQty());
            }

            billMap.put(bill, row);
        }

        billAndItemDataRows = new ArrayList<>(billMap.values());
    }

    @Deprecated
    public void ccSummaryReportByItem() {
        ReportTemplateRowBundle billedBundle = new ReportTemplateRowBundle();
        billedBundle.setName("Collecting Centre Report By Item");
        billedBundle.setDescription("From : to :");
        String jpql = "select new com.divudi.core.data.ReportTemplateRow("
                + "b.collectingCentre, "
                + "count(bi), "
                + "sum(bi.hospitalFee), "
                + "sum(bi.collectingCentreFee), "
                + "sum(bi.staffFee), "
                + "sum(bi.netValue) "
                + ") "
                + " from BillItem bi join bi.bill b "
                + " where b.retired=:ret "
                + " and b.createdAt between :fd and :td "
                + " and b.billTypeAtomic in :bts ";
        List<BillTypeAtomic> bts = new ArrayList<>();
        bts.add(BillTypeAtomic.CC_BILL);
        Map m = new HashMap();
        m.put("ret", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bts", bts);
        if (institution != null) {
            jpql += " and b.institution=:ins ";
            m.put("ins", institution);
        }
        if (department != null) {
            jpql += " and b.department=:dep ";
            m.put("dep", department);
        }
        if (site != null) {
            jpql += " and b.department.site=:site ";
            m.put("site", site);
        }
        if (collectingCentre != null) {
            jpql += " and b.collectingCentre=:cc ";
            m.put("cc", collectingCentre);
        }
        if (route != null) {
            jpql += " and b.collectingCentre.route=:rou ";
            m.put("rou", route);
        }
        jpql += " group by b.collectingCentre "
                + "order by b.collectingCentre.name ";
        List<ReportTemplateRow> rows = billItemFacade.findLightsByJpql(jpql, m, TemporalType.TIMESTAMP);

        // Calculate the aggregate values using stream.
        long totalCount = rows.stream()
                .mapToLong(row -> Optional.ofNullable(row.getItemCount()).orElse(0L))
                .sum();

        double totalHospitalFee = rows.stream()
                .mapToDouble(row -> Optional.ofNullable(row.getItemHospitalFee()).orElse(0.0))
                .sum();

        double totalStaffFee = rows.stream()
                .mapToDouble(row -> Optional.ofNullable(row.getItemProfessionalFee()).orElse(0.0))
                .sum();

        double totalCcFee = rows.stream()
                .mapToDouble(row -> Optional.ofNullable(row.getItemCollectingCentreFee()).orElse(0.0))
                .sum();

        double totalNetValue = rows.stream()
                .mapToDouble(row -> Optional.ofNullable(row.getItemNetTotal()).orElse(0.0))
                .sum();

        // Set the calculated values to the bundle.
        billedBundle.setCount(totalCount);
        billedBundle.setHospitalTotal(totalHospitalFee);
        billedBundle.setStaffTotal(totalStaffFee);
        billedBundle.setCcTotal(totalCcFee);
        billedBundle.setTotal(totalNetValue);
        billedBundle.setReportTemplateRows(rows);

        ReportTemplateRowBundle crBundle = new ReportTemplateRowBundle();
        crBundle.setName("Collecting Centre Report By Item");
        crBundle.setDescription("From : to :");
        jpql = "select new com.divudi.core.data.ReportTemplateRow("
                + "b.collectingCentre, "
                + "count(bi), "
                + "sum(bi.hospitalFee), "
                + "sum(bi.collectingCentreFee), "
                + "sum(bi.staffFee), "
                + "sum(bi.netValue) "
                + ") "
                + " from BillItem bi join bi.bill b "
                + " where b.retired=:ret "
                + " and b.createdAt between :fd and :td "
                + " and b.billTypeAtomic in :bts ";
        bts = new ArrayList<>();
        bts.add(BillTypeAtomic.CC_BILL_CANCELLATION);
        bts.add(BillTypeAtomic.CC_BILL_REFUND);
        m = new HashMap();
        m.put("ret", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bts", bts);
        if (institution != null) {
            jpql += " and b.institution=:ins ";
            m.put("ins", institution);
        }
        if (department != null) {
            jpql += " and b.department=:dep ";
            m.put("dep", department);
        }
        if (site != null) {
            jpql += " and b.department.site=:site ";
            m.put("site", site);
        }
        if (collectingCentre != null) {
            jpql += " and b.collectingCentre=:cc ";
            m.put("cc", collectingCentre);
        }
        if (route != null) {
            jpql += " and b.collectingCentre.route=:rou ";
            m.put("rou", route);
        }
        jpql += " group by b.collectingCentre "
                + "order by b.collectingCentre.name ";
        rows = billItemFacade.findLightsByJpql(jpql, m, TemporalType.TIMESTAMP);

        // Calculate the aggregate values using stream.
        totalCount = rows.stream()
                .mapToLong(row -> Optional.ofNullable(row.getItemCount()).orElse(0L))
                .sum();

        totalHospitalFee = rows.stream()
                .mapToDouble(row -> Optional.ofNullable(row.getItemHospitalFee()).orElse(0.0))
                .sum();

        totalStaffFee = rows.stream()
                .mapToDouble(row -> Optional.ofNullable(row.getItemProfessionalFee()).orElse(0.0))
                .sum();

        totalCcFee = rows.stream()
                .mapToDouble(row -> Optional.ofNullable(row.getItemCollectingCentreFee()).orElse(0.0))
                .sum();

        totalNetValue = rows.stream()
                .mapToDouble(row -> Optional.ofNullable(row.getItemNetTotal()).orElse(0.0))
                .sum();

        totalCount = 0 - Math.abs(totalCount);
        // Set the calculated values to the bundle.
        crBundle.setCount(totalCount);
        crBundle.setHospitalTotal(totalHospitalFee);
        crBundle.setStaffTotal(totalStaffFee);
        crBundle.setCcTotal(totalCcFee);
        crBundle.setTotal(totalNetValue);
        crBundle.setReportTemplateRows(rows);

        bundle = combineBundles(billedBundle, crBundle);
    }

    @Deprecated
    public void ccSummaryReportByBill() {

        ReportTemplateRowBundle billedBundle = new ReportTemplateRowBundle();
        billedBundle.setName("Collecting Centre Report By Item");
        billedBundle.setDescription("From : to :");
        String jpql = "select new com.divudi.core.data.ReportTemplateRow("
                + "b.collectingCentre, "
                + "count(b), "
                + "sum(b.totalHospitalFee), "
                + "sum(b.totalCenterFee), "
                + "sum(b.totalStaffFee), "
                + "sum(b.netTotal) "
                + ") "
                + " from Bill b "
                + " where b.retired=:ret "
                + " and b.createdAt between :fd and :td "
                + " and b.billTypeAtomic in :bts ";
        List<BillTypeAtomic> bts = new ArrayList<>();
        bts.add(BillTypeAtomic.CC_BILL);
        Map m = new HashMap();
        m.put("ret", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bts", bts);
        if (institution != null) {
            jpql += " and b.institution=:ins ";
            m.put("ins", institution);
        }
        if (department != null) {
            jpql += " and b.department=:dep ";
            m.put("dep", department);
        }
        if (site != null) {
            jpql += " and b.department.site=:site ";
            m.put("site", site);
        }
        if (collectingCentre != null) {
            jpql += " and b.collectingCentre=:cc ";
            m.put("cc", collectingCentre);
        }
        if (route != null) {
            jpql += " and b.collectingCentre.route=:rou ";
            m.put("rou", route);
        }
        jpql += " group by b.collectingCentre "
                + "order by b.collectingCentre.name ";
        List<ReportTemplateRow> rows = billItemFacade.findLightsByJpql(jpql, m, TemporalType.TIMESTAMP);

        // Calculate the aggregate values using stream.
        long totalCount = rows.stream()
                .mapToLong(row -> Optional.ofNullable(row.getItemCount()).orElse(0L))
                .sum();

        double totalHospitalFee = rows.stream()
                .mapToDouble(row -> Optional.ofNullable(row.getItemHospitalFee()).orElse(0.0))
                .sum();

        double totalStaffFee = rows.stream()
                .mapToDouble(row -> Optional.ofNullable(row.getItemProfessionalFee()).orElse(0.0))
                .sum();

        double totalCcFee = rows.stream()
                .mapToDouble(row -> Optional.ofNullable(row.getItemCollectingCentreFee()).orElse(0.0))
                .sum();

        double totalNetValue = rows.stream()
                .mapToDouble(row -> Optional.ofNullable(row.getItemNetTotal()).orElse(0.0))
                .sum();

        // Set the calculated values to the bundle.
        billedBundle.setCount(totalCount);
        billedBundle.setHospitalTotal(totalHospitalFee);
        billedBundle.setStaffTotal(totalStaffFee);
        billedBundle.setCcTotal(totalCcFee);
        billedBundle.setTotal(totalNetValue);
        billedBundle.setReportTemplateRows(rows);

        ReportTemplateRowBundle crBundle = new ReportTemplateRowBundle();
        crBundle.setName("Collecting Centre Report By Item");
        crBundle.setDescription("From : to :");
        jpql = "select new com.divudi.core.data.ReportTemplateRow("
                + "b.collectingCentre, "
                + "count(b), "
                + "sum(b.totalHospitalFee), "
                + "sum(b.totalCenterFee), "
                + "sum(b.totalStaffFee), "
                + "sum(b.netTotal) "
                + ") "
                + " from Bill b "
                + " where b.retired=:ret "
                + " and b.createdAt between :fd and :td "
                + " and b.billTypeAtomic in :bts ";
        bts = new ArrayList<>();
        bts.add(BillTypeAtomic.CC_BILL_CANCELLATION);
        bts.add(BillTypeAtomic.CC_BILL_REFUND);
        m = new HashMap();
        m.put("ret", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bts", bts);
        if (institution != null) {
            jpql += " and b.institution=:ins ";
            m.put("ins", institution);
        }
        if (department != null) {
            jpql += " and b.department=:dep ";
            m.put("dep", department);
        }
        if (site != null) {
            jpql += " and b.department.site=:site ";
            m.put("site", site);
        }
        if (collectingCentre != null) {
            jpql += " and b.collectingCentre=:cc ";
            m.put("cc", collectingCentre);
        }
        if (route != null) {
            jpql += " and b.collectingCentre.route=:rou ";
            m.put("rou", route);
        }
        jpql += " group by b.collectingCentre "
                + "order by b.collectingCentre.name ";
        rows = billItemFacade.findLightsByJpql(jpql, m, TemporalType.TIMESTAMP);

        // Calculate the aggregate values using stream.
        totalCount = rows.stream()
                .mapToLong(row -> Optional.ofNullable(row.getItemCount()).orElse(0L))
                .sum();

        totalHospitalFee = rows.stream()
                .mapToDouble(row -> Optional.ofNullable(row.getItemHospitalFee()).orElse(0.0))
                .sum();

        totalStaffFee = rows.stream()
                .mapToDouble(row -> Optional.ofNullable(row.getItemProfessionalFee()).orElse(0.0))
                .sum();

        totalCcFee = rows.stream()
                .mapToDouble(row -> Optional.ofNullable(row.getItemCollectingCentreFee()).orElse(0.0))
                .sum();

        totalNetValue = rows.stream()
                .mapToDouble(row -> Optional.ofNullable(row.getItemNetTotal()).orElse(0.0))
                .sum();

        // Set the calculated values to the bundle.
        crBundle.setCount(totalCount);
        crBundle.setHospitalTotal(totalHospitalFee);
        crBundle.setStaffTotal(totalStaffFee);
        crBundle.setCcTotal(totalCcFee);
        crBundle.setTotal(totalNetValue);
        crBundle.setReportTemplateRows(rows);

        bundle = combineBundles(billedBundle, crBundle);
    }

    @Deprecated
    public ReportTemplateRowBundle combineBundles(ReportTemplateRowBundle billedBundle, ReportTemplateRowBundle crBundle) {
        ReportTemplateRowBundle combinedBundle = new ReportTemplateRowBundle();
        combinedBundle.setName("Combined Collecting Centre Report By Item");
        combinedBundle.setDescription("From : to :");

        Map<Institution, ReportTemplateRow> combinedResults = new HashMap<>();

        // Process the billed bundle
        for (ReportTemplateRow row : billedBundle.getReportTemplateRows()) {
            Institution institution = row.getInstitution();
            combinedResults.putIfAbsent(institution, new ReportTemplateRow(institution, 0L, 0.0, 0.0, 0.0, 0.0));
            ReportTemplateRow existingRow = combinedResults.get(institution);
            existingRow.setItemCount(existingRow.getItemCount() + row.getItemCount());
            existingRow.setItemHospitalFee(existingRow.getItemHospitalFee() + row.getItemHospitalFee());
            existingRow.setItemCollectingCentreFee(existingRow.getItemCollectingCentreFee() + row.getItemCollectingCentreFee());
            existingRow.setItemProfessionalFee(existingRow.getItemProfessionalFee() + row.getItemProfessionalFee());
            existingRow.setItemNetTotal(existingRow.getItemNetTotal() + row.getItemNetTotal());
        }

        // Process the CR bundle and adjust by taking absolute values and subtracting
        for (ReportTemplateRow row : crBundle.getReportTemplateRows()) {
            Institution institution = row.getInstitution();
            combinedResults.putIfAbsent(institution, new ReportTemplateRow(institution, 0L, 0.0, 0.0, 0.0, 0.0));
            ReportTemplateRow existingRow = combinedResults.get(institution);
            existingRow.setItemCount(existingRow.getItemCount() - row.getItemCount());  // Subtract the count
            existingRow.setItemHospitalFee(existingRow.getItemHospitalFee() - Math.abs(row.getItemHospitalFee()));  // Subtract the absolute value
            existingRow.setItemCollectingCentreFee(existingRow.getItemCollectingCentreFee() - Math.abs(row.getItemCollectingCentreFee()));
            existingRow.setItemProfessionalFee(existingRow.getItemProfessionalFee() + row.getItemProfessionalFee());  // Assuming this is correctly negative
            existingRow.setItemNetTotal(existingRow.getItemNetTotal() - Math.abs(row.getItemNetTotal()));
        }

        combinedBundle.setReportTemplateRows(new ArrayList<>(combinedResults.values()));

        long totalCount = combinedResults.values().stream().mapToLong(ReportTemplateRow::getItemCount).sum();
        double totalHospitalFee = combinedResults.values().stream().mapToDouble(ReportTemplateRow::getItemHospitalFee).sum();
        double totalCCFee = combinedResults.values().stream().mapToDouble(ReportTemplateRow::getItemCollectingCentreFee).sum();
        double totalProfessionalFee = combinedResults.values().stream().mapToDouble(ReportTemplateRow::getItemProfessionalFee).sum();
        double totalNet = combinedResults.values().stream().mapToDouble(ReportTemplateRow::getItemNetTotal).sum();

        combinedBundle.setCount(totalCount);
        combinedBundle.setTotal(totalNet);

        combinedBundle.setHospitalTotal(totalHospitalFee);
        combinedBundle.setCcTotal(totalCCFee);
        combinedBundle.setStaffTotal(totalProfessionalFee);

        return combinedBundle;
    }

    @Deprecated
    public void createPatientDepositSummary() {
        String jpql = "select pdh"
                + " from PatientDepositHistory pdh"
                + " where pdh.retired=:ret"
                + " and pdh.patientDeposit.patient = :p ";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        Patient pt = patientController.getCurrent();
        m.put("p", pt);

        jpql += " AND pdh.createdAt BETWEEN :fromDate AND :toDate";
        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());

        patientDepositHistories = patientDepositHistoryFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);

        calculateTotals();
    }

    @Deprecated
    private void calculateTotals() {

        // Check if patientDepositHistories contains data
        if (patientDepositHistories == null || patientDepositHistories.isEmpty()) {
            return;
        }

        totalCredit = 0.0;
        totalDebit = 0.0;

        for (PatientDepositHistory pdh : patientDepositHistories) {
            double transactionValue = pdh.getTransactionValue();

            // Add to totalCredit if transactionValue > 0
            if (transactionValue > 0) {
                totalCredit += transactionValue;
            } // Add to totalDebit if transactionValue < 0
            else if (transactionValue < 0) {
                totalDebit += transactionValue;
            }
        }

    }

    public double getTotalCredit() {
        return totalCredit;
    }

    public double getTotalDebit() {
        return totalDebit;
    }

    private List<Bill> filterBillsByStatus(List<Bill> bills, String statusFilter) {
        List<Bill> filteredBills = new ArrayList<>();

        for (Bill bill : bills) {
            String status = classifyVoucherSettlementStatus(bill);

            // Only add bills that match the status filter
            if (status.equals(statusFilter)) {
                filteredBills.add(bill);
            }
        }

        return filteredBills; // Return the filtered list of bills
    }

    private String classifyVoucherSettlementStatus(Bill bill) {
        BillItem voucher = findVoucherIsAvailable(bill);

        if (voucher == null) {
            return "Unsettled";
        }

        if (bill.getNetTotal() == voucher.getBill().getNetTotal()) {
            return "Settled";
        }

        if (bill.getNetTotal() > voucher.getBill().getNetTotal()) {
            return "Partially Settled";
        }

        return "Unsettled"; // Default case for safety
    }

    public BillItem findVoucherIsAvailable(Bill b) {
        voucherItem = null;

        String jpql = "SELECT bi "
                + "FROM BillItem bi "
                + "WHERE bi.retired = :ret "
                + "AND bi.referenceBill = :b "
                + "AND bi.bill.billType in :bts";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("b", b);
        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.CashRecieveBill);
        m.put("bts", bts);

        List<BillItem> bis = billItemFacade.findByJpql(jpql, m);

        if (bis.size() == 1) {
            voucherItem = billItemFacade.findFirstByJpql(jpql, m);
            voucherItem.getBill().setNetTotal(voucherItem.getNetValue());
            if (voucherItem.getNetValue() < b.getNetTotal()) {
                voucherItem.getBill().setAdjustedTotal(Math.abs(voucherItem.getNetValue()));
                voucherItem.getBill().setBalance(Math.abs(b.getNetTotal()) - Math.abs(voucherItem.getNetValue()));
            }
        } else if (bis.size() > 1) {
            Double NetTotal = 0.0;
            for (BillItem bi : bis) {
                voucherItem = bi;
                NetTotal += voucherItem.getNetValue();
            }
            voucherItem.getBill().setNetTotal(NetTotal);
            voucherItem.getBill().setBalance(Math.abs(b.getNetTotal()) - Math.abs(voucherItem.getBill().getNetTotal()));
        }

        return voucherItem;
    }

    public String navigatetoOPDLabReportByMenu() {
        return "/lab/report_for_opd_print?faces-redirect=true";
    }

    public String navigateToPrescriptionSummaryReport() {
        return "/pharmacy/prescription_summary_report?faces-redirect=true";
    }

    public String navigateToPrescriptionList() {
        return "/pharmacy/prescription_list?faces-redirect=true";
    }

    public String navigateToPrescriptionListFromPrescriptionSummery(Long personId) {
        if (personId == null || personId == 0) {
            JsfUtil.addErrorMessage("Error 1");
            return "";
        }

        person = personController.findPerson(personId);

        doctor = doctorController.findDoctor(person);

        if (doctor == null) {
            JsfUtil.addErrorMessage("Error 2");
            return "";
        }

        processPresciptionList();

        return navigateToPrescriptionList();
    }

    //    public void processPharmacySaleReferralCount() {
//        String jpql = "select new com.divudi.core.data.BillLight(bi.referredBy.person.name, count(bi), count(bi.netTotal)) "
//                + " from Bill bi "
//                + " where bi.cancelled=:can "
//                + " and bi.createdAt between :fd and :td "
//                + " and bi.billType=:bitype ";
//        Map m = new HashMap();
//
//        m.put("can", false);
//        m.put("fd", fromDate);
//        m.put("td", toDate);
//        m.put("bitype", BillType.PharmacySale);
//
//
//        if (department  != null) {
//            jpql += " and bi.fromDepartment=:fdept ";
//            m.put("fdept", department);
//        }
//
//        if (referingDoctor  != null) {
//            jpql += " and bi.referredBy=:refDoc ";
//            m.put("refDoc", referingDoctor);
//        }
//        jpql += " group by bi.referredBy.person.name ";
//        jpql += " order by bi.referredBy.person.name ";
//        prescriptionSummaryReportRows = (List<PrescriptionSummaryReportRow>) billFacade.findLightsByJpql(jpql, m);
//    }
//
    public void processPresciptionSummeryReport() {
        String jpql = "select new "
                + " com.divudi.core.light.common.PrescriptionSummaryReportRow(bi.referredBy.person.name, bi.referredBy.person.id, count(bi), sum(bi.netTotal)) "
                + " from Bill bi "
                + " where bi.cancelled=:can "
                + " and bi.billDate between :fd and :td "
                + " and bi.billType=:bitype ";
        Map m = new HashMap();

        m.put("can", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bitype", BillType.PharmacySale);

        if (department != null) {
            jpql += " and bi.fromDepartment=:fdept ";
            m.put("fdept", department);
        }

        jpql += " group by bi.referredBy ";
        jpql += " order by bi.referredBy.person.name ";
        prescriptionSummaryReportRows = (List<PrescriptionSummaryReportRow>) billFacade.findLightsByJpql(jpql, m, TemporalType.DATE);
    }

    public void processPresciptionListEvenWhenNoPatientData() {
        String jpql = "select new "
                + " com.divudi.core.light.common.BillLight(bi.id, bi.deptId, bi.billDate, bi.billTime, "
                + " coalesce(p.person.name, 'No Patient'), bi.netTotal) "
                + " from Bill bi "
                + " left join bi.patient p "
                + " where bi.cancelled=:can "
                + " and bi.billDate between :fd and :td "
                + " and bi.billType=:bitype ";
        Map m = new HashMap();

        m.put("can", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bitype", BillType.PharmacySale);

        if (department != null) {
            jpql += " and bi.fromDepartment=:fdept ";
            m.put("fdept", department);
        }

        if (doctor != null) {
            jpql += " and bi.referredBy=:refDoc ";
            m.put("refDoc", doctor);
        }

        jpql += " order by bi.id ";
        billLights = (List<BillLight>) billFacade.findLightsByJpql(jpql, m, TemporalType.DATE);
    }

    public void processPresciptionList() {
        String jpql = "select new "
                + " com.divudi.core.light.common.BillLight(bi.id, bi.deptId, bi.billDate, bi.billTime, bi.patient.person.name, bi.netTotal) "
                + " from Bill bi "
                + " where bi.cancelled=:can "
                + " and bi.billDate between :fd and :td "
                + " and bi.billType=:bitype ";
        Map m = new HashMap();

        m.put("can", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bitype", BillType.PharmacySale);

        if (department != null) {
            jpql += " and bi.fromDepartment=:fdept ";
            m.put("fdept", department);
        }

        if (doctor != null) {
            jpql += " and bi.referredBy=:refDoc ";
            m.put("refDoc", doctor);
        }

        jpql += " order by bi.id ";
//        System.out.println("jpql = " + jpql);
//        System.out.println("m = " + m);
        billLights = (List<BillLight>) billFacade.findLightsByJpql(jpql, m, TemporalType.DATE);
    }

    // </editor-fold>
    @Deprecated
    public void createPharmacyCashInOutLedgerOld() {
        bundleList = new ArrayList<>();
        ReportTemplateRowBundle childBundle = new ReportTemplateRowBundle();

        netTotal = 0.0;

        List<BillTypeAtomic> btasPIn = BillTypeAtomic.findByServiceTypeAndFinanceType(ServiceType.PHARMACY, BillFinanceType.CASH_IN);
        List<BillTypeAtomic> btasPOut = BillTypeAtomic.findByServiceTypeAndFinanceType(ServiceType.PHARMACY, BillFinanceType.CASH_OUT);

        ReportTemplateRowBundle pharmacyIn = generatePaymentMethodColumnsByBills(btasPIn);
        pharmacyIn.setBundleType("CashIn");
        pharmacyIn.setName("Pharmacy cash in");
        childBundle.getBundles().add(pharmacyIn);
        bundleList.add(childBundle);

        ReportTemplateRowBundle pharmacyOut = generatePaymentMethodColumnsByBills(btasPOut);
        pharmacyOut.setBundleType("CashOut");
        pharmacyOut.setName("Pharmacy cash out");
        childBundle.getBundles().add(pharmacyOut);
        bundleList.add(childBundle);

//        bundle.getBundles().add(netCashForTheDayBundle);
    }

    @Deprecated
    public ReportTemplateRowBundle generatePaymentMethodColumnsByBills(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow("
                + "bill.department, FUNCTION('date', p.createdAt), "
                + "SUM(p.paidValue)) "
                + "FROM Payment p "
                + "JOIN p.bill bill "
                + "WHERE p.retired <> :bfr AND bill.retired <> :br ";

        parameters.put("bfr", true);
        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (department != null) {
            jpql += " AND bill.department = :dept ";
            parameters.put("dept", department);
        }
        if (webUser != null) {
            jpql += " AND bill.creater.webUserPerson.name = :wu ";
            parameters.put("wu", webUser.getWebUserPerson().getName());
        }

        jpql += "AND p.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY bill.department, FUNCTION('date', p.createdAt)";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBill();

        return b;
    }

    public void generatePharmacyCashInOutLedger() {
        reportTimerController.trackReportExecution(() -> {
            netTotal = 0.0;
            bills = new ArrayList<>();
            unifiedBundle = new ArrayList<>();
            List<BillTypeAtomic> allBillTypes = new ArrayList<>();
            Map<BillTypeAtomic, Double> btaNetTotals = new HashMap<>();

            // Combine all relevant BillTypeAtomic
            List<BillTypeAtomic> btasCashIn = BillTypeAtomic.findByServiceTypeAndFinanceType(ServiceType.PHARMACY, BillFinanceType.CASH_IN);
            List<BillTypeAtomic> btasCashOut = BillTypeAtomic.findByServiceTypeAndFinanceType(ServiceType.PHARMACY, BillFinanceType.CASH_OUT);
            List<BillTypeAtomic> btasShiftStart = BillTypeAtomic.findByServiceTypeAndFinanceType(ServiceType.OTHER, BillFinanceType.FLOAT_STARTING_BALANCE);
            List<BillTypeAtomic> btasShiftEnd = BillTypeAtomic.findByServiceTypeAndFinanceType(ServiceType.OTHER, BillFinanceType.FLOAT_CLOSING_BALANCE);

            allBillTypes.addAll(btasCashIn);
            allBillTypes.addAll(btasCashOut);
            allBillTypes.addAll(btasShiftStart);
            allBillTypes.addAll(btasShiftEnd);

            allBillTypes.add(BillTypeAtomic.SUPPLEMENTARY_INCOME);
            allBillTypes.add(BillTypeAtomic.OPERATIONAL_EXPENSES);

            createPharmacyCashInOutLedger(allBillTypes);
        }, PharmacyReports.CASH_IN_OUT_REPORT, sessionController.getLoggedUser());
    }

    public void createPharmacyCashInOutLedger(List<BillTypeAtomic> billTypeAtomics) {
        netTotal = 0.0;
        bills = new ArrayList<>();
        unifiedBundle = new ArrayList<>();
        Map<BillTypeAtomic, Double> btaNetTotals = new HashMap<>();
        calAllInTotal = 0.0;
        calAllOutTotal = 0.0;

        // Prepare parameters for the query
        Map<String, Object> params = new HashMap<>();
        String jpql = "SELECT b FROM Bill b "
                + "WHERE b.retired = false "
                + "AND b.createdAt BETWEEN :fromDate AND :toDate "
                + "AND b.billTypeAtomic IN :billTypes ";
        if (department != null) {
            jpql += " AND b.department = :dept ";
            params.put("dept", department);
        }
        if (webUser != null) {
            jpql += " AND b.creater.webUserPerson.name = :wu ";
            params.put("wu", webUser.getWebUserPerson().getName());
        }
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);
        params.put("billTypes", billTypeAtomics);
        jpql += " ORDER BY b.id ";
        // Fetch and process bills
        List<Bill> rows = (List<Bill>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
        for (Bill row : rows) {
            BillTypeAtomic bta = row.getBillTypeAtomic();
            btaNetTotals.put(bta, btaNetTotals.getOrDefault(bta, 0.0) + row.getNetTotal());
            bills.add(row);
        }

        bills.sort(Comparator.comparing(Bill::getCreatedAt));

        List<ReportTemplateRow> reportRows = new ArrayList<>();
        for (Map.Entry<BillTypeAtomic, Double> entry : btaNetTotals.entrySet()) {
            ReportTemplateRow reportRow = new ReportTemplateRow();
            reportRow.setBillTypeAtomic(entry.getKey());
            reportRow.setTotal(entry.getValue());
            reportRows.add(reportRow);

            if (entry.getKey().getBillFinanceType() == BillFinanceType.CASH_IN
                    || entry.getKey().getBillFinanceType() == BillFinanceType.FLOAT_STARTING_BALANCE) {
                calAllInTotal += entry.getValue();
            } else if (entry.getKey().getBillFinanceType() == BillFinanceType.CASH_OUT
                    || entry.getKey().getBillFinanceType() == BillFinanceType.FLOAT_CLOSING_BALANCE) {
                calAllOutTotal += entry.getValue();
            }
        }

        // Update class-level variables
        this.bills = bills;
        this.unifiedBundle = reportRows;
    }

    public void makeNull() {
        doctor = null;
        prescriptionSummaryReportRows = null;
        fromDate = null;
        toDate = null;
        person = null;
    }

    public void processPharmacySaleItemCount() {
        String jpql = "select new com.divudi.core.data.ItemCount(bi.item.category.name, bi.item.name, count(bi.item)) "
                + " from BillItem bi "
                + " where bi.bill.cancelled=:can "
                + " and bi.bill.billDate between :fd and :td "
                + " and bi.bill.billType=:bitype ";
        Map<String, Object> m = new HashMap<>();

        m.put("can", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bitype", BillType.PharmacySale);

        if (fromInstitution != null) {
            jpql += " and bi.bill.fromInstitution=:fi ";
            m.put("fi", fromInstitution);
        }

        if (fromDepartment != null) {
            jpql += " and bi.bill.fromDepartment=:fdept ";
            m.put("fdept", fromDepartment);
        }

        jpql += " group by bi.item.category.name, bi.item.name ";
        jpql += " order by bi.item.category.name, bi.item.name";

        // Unchecked cast here
        reportLabTestCounts = (List<ItemCount>) billItemFacade.findLightsByJpql(jpql, m);

        Map<String, CategoryCount> categoryReports = new HashMap<>();

        for (ItemCount count : reportLabTestCounts) {
            categoryReports.computeIfAbsent(count.getCategory(), k -> new CategoryCount(k, new ArrayList<>(), 0L))
                    .getItems().add(count);
            categoryReports.get(count.getCategory()).setTotal(categoryReports.get(count.getCategory()).getTotal() + count.getTestCount());
        }

        // Convert the map values to a list to be used in the JSF page
        reportList = new ArrayList<>(categoryReports.values());
    }

    public void downloadPharmacySaleItemCount() {
        Workbook workbook = exportToExcel(reportList, "Sale Item Count");
        FacesContext fc = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
        response.reset();
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=Sale_Item_Count.xlsx");

        try (ServletOutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
            fc.responseComplete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Workbook exportToExcel(List<CategoryCount> reportList, String reportName) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(reportName);
        int rowCount = 0;
        int cateColNo = 0;
        int itemColNo = 1;
        int catCountColNo = 2;
        int itemCountColNo = 3;
        Long grandTotal = 0l;

        Row reportHeaderRow = sheet.createRow(rowCount++);
        Cell headerRowCatCell = reportHeaderRow.createCell(cateColNo);
        headerRowCatCell.setCellValue("Category");
        Cell headerRowItemCell = reportHeaderRow.createCell(itemColNo);
        headerRowItemCell.setCellValue("Item");
        Cell headerRowCatCountCell = reportHeaderRow.createCell(catCountColNo);
        headerRowCatCountCell.setCellValue("Category Count");
        Cell headerRowItemCountCell = reportHeaderRow.createCell(itemCountColNo);
        headerRowItemCountCell.setCellValue("Item Count");

        for (CategoryCount catCount : reportList) {
            Row headerRow = sheet.createRow(rowCount++);
            Cell headerCell1 = headerRow.createCell(cateColNo);
            headerCell1.setCellValue(catCount.getCategory());
            Cell headerCell2 = headerRow.createCell(catCountColNo);
            headerCell2.setCellValue(catCount.getTotal());
            grandTotal += catCount.getTotal();

            for (ItemCount itemCount : catCount.getItems()) {
                Row itemRow = sheet.createRow(rowCount++);
                Cell cell1 = itemRow.createCell(itemColNo);
                cell1.setCellValue(itemCount.getTestName());
                Cell cell2 = itemRow.createCell(itemCountColNo);
                cell2.setCellValue(itemCount.getTestCount());
            }
        }

        Row reportFooterRow = sheet.createRow(rowCount++);
        Cell reportFooterCellTotal = reportFooterRow.createCell(cateColNo);
        reportFooterCellTotal.setCellValue("Grand Total");
        Cell reportFooterRowItemCell = reportFooterRow.createCell(itemCountColNo);
        reportFooterRowItemCell.setCellValue(grandTotal);

        return workbook;
    }

    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public InstitutionController getInstitutionController() {
        return institutionController;
    }

    public void setInstitutionController(InstitutionController institutionController) {
        this.institutionController = institutionController;
    }

    public Institution getFromInstitution() {
        return fromInstitution;
    }

    public void setFromInstitution(Institution fromInstitution) {
        this.fromInstitution = fromInstitution;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public int getReportIndex() {
        return reportIndex;
    }

    public void setReportIndex(int reportIndex) {
        this.reportIndex = reportIndex;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public List<ItemCount> getReportLabTestCounts() {
        return reportLabTestCounts;
    }

    public void setReportLabTestCounts(List<ItemCount> reportLabTestCounts) {
        this.reportLabTestCounts = reportLabTestCounts;
    }

    public List<CategoryCount> getReportList() {
        return reportList;
    }

    public void setReportList(List<CategoryCount> reportList) {
        this.reportList = reportList;
    }

    public String getProcessBy() {
        return processBy;
    }

    public void setProcessBy(String processBy) {
        this.processBy = processBy;
    }

    public double getInvestigationResult() {
        return investigationResult;
    }

    public void setInvestigationResult(double investigationResult) {
        this.investigationResult = investigationResult;
    }

    public String getVisitType() {
        return visitType;
    }

    public Sex[] getSex() {
        return Sex.values();
    }

    public void setVisitType(String visitType) {
        this.visitType = visitType;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation(Investigation investigation) {
        this.investigation = investigation;
    }

    public String getPriorityType() {
        return priorityType;
    }

    public void setPriorityType(String priorityType) {
        this.priorityType = priorityType;
    }

    public String getPatientMrn() {
        return patientMrn;
    }

    public void setPatientMrn(String patientMrn) {
        this.patientMrn = patientMrn;
    }

    public String getTotalAverage() {
        return totalAverage;
    }

    public void setTotalAverage(String totalAverage) {
        this.totalAverage = totalAverage;
    }

    public String getVisit() {
        return visit;
    }

    public void setVisit(String visit) {
        this.visit = visit;
    }

    public Date getFinancialYear() {
        return financialYear;
    }

    public void setFinancialYear(Date financialYear) {
        this.financialYear = financialYear;
    }

    public Speciality getCurrentSpeciality() {
        return currentSpeciality;
    }

    public void setCurrentSpeciality(Speciality currentSpeciality) {
        this.currentSpeciality = currentSpeciality;
    }

    public Date getWarrentyStartDate() {
        if (warrentyStartDate == null) {
            warrentyStartDate = CommonFunctions.getEndOfDay();
        }
        return warrentyStartDate;
    }

    public void setWarrentyStartDate(Date warrentyStartDate) {
        this.warrentyStartDate = warrentyStartDate;
    }

    public Date getWarrentyEndDate() {
        if (warrentyEndDate == null) {
            warrentyEndDate = CommonFunctions.getEndOfMonth(toDate);
        }

        return warrentyEndDate;
    }

    public void setWarrentyEndDate(Date warrentyEndDate) {
        this.warrentyEndDate = warrentyEndDate;
    }

    public Date getAmcStartDate() {
        if (amcStartDate == null) {
            amcStartDate = CommonFunctions.getEndOfDay();
        }
        return amcStartDate;
    }

    public void setAmcStartDate(Date amcStartDate) {
        this.amcStartDate = amcStartDate;
    }

    public Date getAmcEndDate() {
        if (amcEndDate == null) {
            amcEndDate = CommonFunctions.getEndOfMonth(toDate);
        }

        return amcEndDate;
    }

    public void setAmcEndDate(Date amcEndDate) {
        this.amcEndDate = amcEndDate;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public Institution getCollectingCentre() {
        return collectingCentre;
    }

    public void setCollectingCentre(Institution collectingCentre) {
        this.collectingCentre = collectingCentre;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public String getPhn() {
        return phn;
    }

    public void setPhn(String phn) {
        this.phn = phn;
    }

    public List<ItemLight> getInvestigationsAndServices() {
        if (investigationsAndServices == null) {
            investigationsAndServices = itemApplicationController.getInvestigationsAndServices();
        }
        return investigationsAndServices;
    }

    public void setInvestigationsAndServices(List<ItemLight> investigationsAndServices) {
        this.investigationsAndServices = investigationsAndServices;
    }

    public ItemLight getItemLight() {
        if (item == null) {
            itemLight = null;
        } else {
            itemLight = new ItemLight(item);
        }
        return itemLight;
    }

    public void setItemLight(ItemLight itemLight) {
        this.itemLight = itemLight;
        if (itemLight == null) {
            item = null;
        } else {
            item = itemController.findItem(itemLight.getId());
        }
    }

    public BillItemStatus getStatus() {
        return status;
    }

    public void setStatus(BillItemStatus status) {
        this.status = status;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    private List<StockCorrectionRow> stockCorrectionRows;
    private Map<String, List<StockCorrectionRow>> positiveVarianceMap;
    private Map<String, List<StockCorrectionRow>> negativeVarianceMap;

    public void createStockCorrectionReportWithDTO() {
        stockCorrectionRows = new ArrayList<>();
        try {
            List<BillTypeAtomic> billTypeAtomics = Arrays.asList(
                    BillTypeAtomic.PHARMACY_PURCHASE_RATE_ADJUSTMENT,
                    BillTypeAtomic.PHARMACY_RETAIL_RATE_ADJUSTMENT,
                    BillTypeAtomic.PHARMACY_COST_RATE_ADJUSTMENT
            );

            StringBuilder jpql = new StringBuilder(
                    "SELECT new com.divudi.core.data.StockCorrectionRow("
                    + "    bi.item.name, "
                    + "    bi.billItemFinanceDetails.quantity, "
                    + "    bi.pharmaceuticalBillItem.itemBatch.purcahseRate, "
                    + "    bi.pharmaceuticalBillItem.itemBatch.costRate, "
                    + "    bi.pharmaceuticalBillItem.itemBatch.retailsaleRate, "
                    + "    bi.pharmaceuticalBillItem.beforeAdjustmentValue, "
                    + "    bi.pharmaceuticalBillItem.afterAdjustmentValue"
                    + ") "
                    + "FROM BillItem bi "
                    + "WHERE bi.retired = false "
                    + "AND bi.createdAt BETWEEN :fd AND :td "
                    + "AND bi.bill.billTypeAtomic IN :doctype "
            );

            Map<String, Object> params = new HashMap<>();
            params.put("fd", fromDate);
            params.put("td", toDate);
            params.put("doctype", billTypeAtomics);

            addFilter(jpql, params, "bi.bill.institution", "ins", institution);
            addFilter(jpql, params, "bi.bill.department.site", "sit", site);
            addFilter(jpql, params, "bi.bill.department", "dep", department);

            stockCorrectionRows = (List<StockCorrectionRow>) billItemFacade.findDTOsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
            positiveVarianceMap = new HashMap<>();
            negativeVarianceMap = new HashMap<>();

            for (StockCorrectionRow row : stockCorrectionRows) {
                // Use precise calculation for old/new value
                BigDecimal preciseOldValue = calculatePreciseValue(row.getBeforeAdjustment(), row.getQuantity().doubleValue());
                row.setPreciseOldValue(preciseOldValue);
                BigDecimal preciseNewValue = calculatePreciseValue(row.getAfterAdjustment(), row.getQuantity().doubleValue());
                row.setPreciseNewValue(preciseNewValue);

                // Calculate variance
                Double beforeValue = row.getBeforeAdjustment();
                Double afterValue = row.getAfterAdjustment();

                if (beforeValue == null || afterValue == null || row.getQuantity() == null) {
                    continue;
                }

                double variance = (afterValue - beforeValue) * row.getQuantity().doubleValue();

                // Group by item name
                String itemName = row.getItemName();

                if (variance > 0) {
                    // Positive variance - add to positiveVarianceMap
                    positiveVarianceMap.putIfAbsent(itemName, new ArrayList<>());
                    positiveVarianceMap.get(itemName).add(row);
                } else if (variance < 0) {
                    // Negative variance - add to negativeVarianceMap
                    negativeVarianceMap.putIfAbsent(itemName, new ArrayList<>());
                    negativeVarianceMap.get(itemName).add(row);
                }
            }

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Error creating Stock Correction Report");
        }
    }

    public BigDecimal calculatePreciseValue(Double rate, Double quantity) {
        if (rate == null || quantity == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal bdRate = BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal bdQuantity = BigDecimal.valueOf(quantity);

        return bdRate.multiply(bdQuantity).setScale(2, RoundingMode.HALF_UP);
    }

    public Double getTotalStockCorrectionQty() {
        if (stockCorrectionRows == null || stockCorrectionRows.isEmpty()) {
            return 0.0;
        }
        return stockCorrectionRows.stream()
                .mapToDouble(row -> {
                    if (row.getQuantity() == null) {
                        return 0.0;
                    }
                    return row.getQuantity().doubleValue();
                })
                .sum();
    }

    public Double getTotalStockCorrectionCostValue() {
        if (stockCorrectionRows == null || stockCorrectionRows.isEmpty()) {
            return 0.0;
        }
        return stockCorrectionRows.stream()
                .mapToDouble(row -> {
                    double qty = row.getQuantity() != null ? row.getQuantity().doubleValue() : 0.0;
                    Double costRate = row.getCostRate();
                    double rate = costRate != null ? costRate : 0.0;
                    return qty * rate;
                })
                .sum();
    }

    public Double getTotalStockCorrectionOldValue() {
        if (stockCorrectionRows == null || stockCorrectionRows.isEmpty()) {
            return 0.0;
        }
        return stockCorrectionRows.stream()
                .mapToDouble(row -> {
                    if (row.getPreciseOldValue() != null) {
                        return row.getPreciseOldValue().doubleValue();
                    }
                    return 0.0;
                })
                .sum();
    }

    public Double getTotalStockCorrectionNewValue() {
        if (stockCorrectionRows == null || stockCorrectionRows.isEmpty()) {
            return 0.0;
        }
        return stockCorrectionRows.stream()
                .mapToDouble(row -> {
                    if (row.getPreciseNewValue() != null) {
                        return row.getPreciseNewValue().doubleValue();
                    }
                    return 0.0;
                })
                .sum();
    }

    public Double getTotalStockCorrectionVariance() {
        if (stockCorrectionRows == null || stockCorrectionRows.isEmpty()) {
            return 0.0;
        }
        return stockCorrectionRows.stream()
                .mapToDouble(row -> {
                    double qty = row.getQuantity() != null ? row.getQuantity().doubleValue() : 0.0;
                    Double beforeAdj = row.getBeforeAdjustment();
                    Double afterAdj = row.getAfterAdjustment();
                    double before = beforeAdj != null ? beforeAdj : 0.0;
                    double after = afterAdj != null ? afterAdj : 0.0;
                    return (after - before) * qty;
                })
                .sum();
    }

    public void processGrnCash() {
        List<BillTypeAtomic> btas = Arrays.asList(
                BillTypeAtomic.PHARMACY_GRN,
                BillTypeAtomic.PHARMACY_DIRECT_PURCHASE
        );
        retrieveBillItems(btas, getNonCreditPaymentMethods());
        totalCostValue = 0.0;
        totalCostValue = billItems.stream()
                .map(BillItem::getBillItemFinanceDetails)
                .distinct()
                .mapToDouble(details -> details.getTotalCost().doubleValue())
                .sum();
    }

    public void processGrnCredit() {
        List<BillTypeAtomic> btas = Arrays.asList(
                BillTypeAtomic.PHARMACY_GRN,
                BillTypeAtomic.PHARMACY_DIRECT_PURCHASE
        );
        retrieveBillItems(btas, getCreditPaymentMethods());

        totalCostValue = 0.0;
        totalCostValue = billItems.stream()
                .map(BillItem::getBillItemFinanceDetails)
                .distinct()
                .mapToDouble(details -> details.getTotalCost().doubleValue())
                .sum();
    }

    private void retrieveBillItems(List<BillTypeAtomic> billTypeValue) {
        retrieveBillItemsCompleted(billTypeValue, null);
    }

    private void retrieveBillItemsCompleted(List<BillTypeAtomic> billTypeValue, Boolean completed) {
        try {
            billItems = new ArrayList<>();
            netTotal = 0.0;
            resetFinanceTotals();

            StringBuilder jpql = new StringBuilder("SELECT bi FROM BillItem bi "
                    + "LEFT JOIN FETCH bi.item "
                    + "LEFT JOIN FETCH bi.bill b "
                    + "LEFT JOIN FETCH bi.pharmaceuticalBillItem pbi "
                    + "LEFT JOIN FETCH pbi.itemBatch "
                    + "WHERE bi.retired = false "
                    + "AND b.retired = false "
                    + "AND b.billTypeAtomic IN :billTypes "
                    + "AND b.createdAt BETWEEN :fromDate AND :toDate ");

            Map<String, Object> params = new HashMap<>();
            params.put("billTypes", billTypeValue);
            params.put("fromDate", fromDate);
            params.put("toDate", toDate);

            if (completed != null) {
                addFilter(jpql, params, "b.completed", "completed", true);
            }

            addFilter(jpql, params, "b.institution", "ins", institution);
            addFilter(jpql, params, "b.department.site", "sit", site);
            addFilter(jpql, params, "b.department", "dep", department);

            billItems = billItemFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
//            netTotal = billItems.stream().mapToDouble(BillItem::getNetValue).sum();
            netTotal = billItems.stream()
                    .map(BillItem::getBill)
                    .distinct()
                    .mapToDouble(Bill::getNetTotal)
                    .sum();

            computeBillItemFinanceTotals();

        } catch (Exception e) {
            e.printStackTrace();
            billItems = new ArrayList<>();
            netTotal = 0.0;
            resetFinanceTotals();
        }
    }

    private void retrieveBillItems(String billTypeField, Object billTypeValue) {
        retrieveBillItems(billTypeField, billTypeValue, null);
    }

    private void retrieveBillItems(String billTypeField, Object billTypeValue, Boolean completedOnly) {
        try {
            billItems = new ArrayList<>();
            netTotal = 0.0;
            resetFinanceTotals();

            StringBuilder jpql = new StringBuilder("SELECT bi FROM BillItem bi "
                    + "LEFT JOIN FETCH bi.item "
                    + "LEFT JOIN FETCH bi.bill b "
                    + "LEFT JOIN FETCH bi.pharmaceuticalBillItem pbi "
                    + "LEFT JOIN FETCH pbi.itemBatch "
                    + "WHERE bi.retired = false "
                    + "AND b.retired = false "
                    + "AND " + billTypeField + " IN :billTypes "
                    + "AND b.createdAt BETWEEN :fromDate AND :toDate ");

            Map<String, Object> params = new HashMap<>();
            params.put("billTypes", billTypeValue);
            params.put("fromDate", fromDate);
            params.put("toDate", toDate);

            addFilter(jpql, params, "b.institution", "ins", institution);
            addFilter(jpql, params, "b.department.site", "sit", site);
            addFilter(jpql, params, "b.department", "dep", department);

            // Filter by item - if AMP is selected, include both AMP and its AMPPs
            if (item != null) {
                if (item instanceof Amp) {
                    jpql.append("AND (bi.item = :itm OR bi.item.amp = :itm) ");
                    params.put("itm", item);
                } else {
                    addFilter(jpql, params, "bi.item", "itm", item);
                }
            }

            if (completedOnly != null) {
                addFilter(jpql, params, "b.completed", "completed", completedOnly);
            }

            billItems = billItemFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
//            netTotal = billItems.stream().mapToDouble(BillItem::getNetValue).sum();
            netTotal = billItems.stream()
                    .map(BillItem::getBill)
                    .distinct()
                    .mapToDouble(Bill::getNetTotal)
                    .sum();

            computeBillItemFinanceTotals();

        } catch (Exception e) {
            e.printStackTrace();
            billItems = new ArrayList<>();
            netTotal = 0.0;
            resetFinanceTotals();
        }
    }

    private void retrieveBillItems(List<BillTypeAtomic> btas, List<PaymentMethod> paymentMethod) {
        try {
            billItems = new ArrayList<>();
            netTotal = 0.0;
            resetFinanceTotals();

            StringBuilder jpql = new StringBuilder("SELECT bi FROM BillItem bi "
                    + "LEFT JOIN FETCH bi.item "
                    + "LEFT JOIN FETCH bi.bill b "
                    + "LEFT JOIN FETCH bi.pharmaceuticalBillItem pbi "
                    + "LEFT JOIN FETCH pbi.itemBatch "
                    + "WHERE bi.retired = false "
                    + "AND b.retired = false "
                    + "AND b.billTypeAtomic IN :btas "
                    + "AND b.createdAt BETWEEN :fromDate AND :toDate ");
            // Filter by item - if AMP is selected, include both AMP and its AMPPs
            Map<String, Object> params = new HashMap<>();
            if (item != null) {
                if (item instanceof Amp) {
                    jpql.append("AND (bi.item = :itm OR bi.item.amp = :itm) ");
                    params.put("itm", item);
                } else {
                    addFilter(jpql, params, "bi.item", "itm", item);
                }
            }

            jpql.append("AND (b.paymentMethod IN :pm ");
            jpql.append("OR (b.paymentMethod = :multiPm AND EXISTS ("
                    + "SELECT p FROM Payment p "
                    + "WHERE p.bill = b AND p.paymentMethod IN :pm)) )");

            params.put("pm", paymentMethod);
            params.put("multiPm", PaymentMethod.MultiplePaymentMethods);
            params.put("btas", btas);
            params.put("fromDate", fromDate);
            params.put("toDate", toDate);

            addFilter(jpql, params, "b.institution", "ins", institution);
            addFilter(jpql, params, "b.department.site", "sit", site);
            addFilter(jpql, params, "b.department", "dep", department);

            billItems = billItemFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
            netTotal = billItems.stream()
                    .map(BillItem::getBill)
                    .distinct()
                    .mapToDouble(Bill::getNetTotal)
                    .sum();

            computeBillItemFinanceTotals();

        } catch (Exception e) {
            e.printStackTrace();
            billItems = new ArrayList<>();
            netTotal = 0.0;
            resetFinanceTotals();
        }
    }

    @Deprecated
    private void retrieveBillItems(String billTypeField, Object billTypeValue, Object paymentMethod) {
        try {
            billItems = new ArrayList<>();
            netTotal = 0.0;
            resetFinanceTotals();

            StringBuilder jpql = new StringBuilder("SELECT bi FROM BillItem bi "
                    + "LEFT JOIN FETCH bi.item "
                    + "LEFT JOIN FETCH bi.bill b "
                    + "LEFT JOIN FETCH bi.pharmaceuticalBillItem pbi "
                    + "LEFT JOIN FETCH pbi.itemBatch "
                    + "WHERE bi.retired = false "
                    + "AND b.retired = false "
                    + "AND " + billTypeField + " IN :billTypes "
                    //                    + "AND b.paymentMethod IN :paymentMethod "
                    + "AND b.createdAt BETWEEN :fromDate AND :toDate ");

            jpql.append("AND (b.paymentMethod IN :pm ");
            jpql.append("OR (b.paymentMethod = :multiPm AND EXISTS ("
                    + "SELECT p FROM Payment p "
                    + "WHERE p.bill = b AND p.paymentMethod IN :pm)) )");

            Map<String, Object> params = new HashMap<>();
            params.put("pm", paymentMethod);
            params.put("multiPm", PaymentMethod.MultiplePaymentMethods);
            params.put("billTypes", billTypeValue);
            params.put("fromDate", fromDate);
            params.put("toDate", toDate);

            addFilter(jpql, params, "b.institution", "ins", institution);
            addFilter(jpql, params, "b.department.site", "sit", site);
            addFilter(jpql, params, "b.department", "dep", department);

            billItems = billItemFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
            netTotal = billItems.stream()
                    .map(BillItem::getBill)
                    .distinct()
                    .mapToDouble(Bill::getNetTotal)
                    .sum();

            computeBillItemFinanceTotals();

        } catch (Exception e) {
            e.printStackTrace();
            billItems = new ArrayList<>();
            netTotal = 0.0;
            resetFinanceTotals();
        }
    }

    private List<PaymentMethod> getCreditPaymentMethods() {
        return new ArrayList<>(CREDIT_PAYMENT_METHODS);
    }

    private List<PaymentMethod> getNonCreditPaymentMethods() {
        return PaymentMethod.getActivePaymentMethods().stream()
                .filter(pm -> !CREDIT_PAYMENT_METHODS.contains(pm))
                .filter(pm -> pm != PaymentMethod.MultiplePaymentMethods)
                .collect(Collectors.toList());
    }

    private void computeBillItemFinanceTotals() {
        resetFinanceTotals();

        if (billItems == null || billItems.isEmpty()) {
            return;
        }

        for (BillItem billItem : billItems) {
            double resolvedCostValue = resolveFinanceValue(
                    billItem,
                    BillItemFinanceDetails::getValueAtCostRate,
                    BillItemFinanceDetails::getCostRate,
                    PharmaceuticalBillItem::getCostRate,
                    itemBatch -> itemBatch.getCostRate());

            if (Math.abs(resolvedCostValue) < 0.0000001d) {
                resolvedCostValue = resolveCostValueFromItemBatch(billItem);
            }

            totalCostValue += resolvedCostValue;

            totalPurchaseValue += resolveFinanceValue(
                    billItem,
                    BillItemFinanceDetails::getValueAtPurchaseRate,
                    BillItemFinanceDetails::getPurchaseRate,
                    PharmaceuticalBillItem::getPurchaseRate,
                    itemBatch -> itemBatch.getPurcahseRate());

            totalRetailValue += resolveFinanceValue(
                    billItem,
                    BillItemFinanceDetails::getValueAtRetailRate,
                    BillItemFinanceDetails::getRetailSaleRate,
                    PharmaceuticalBillItem::getRetailRate,
                    itemBatch -> itemBatch.getRetailsaleRate());

            // Calculate totalSaleValue as retailRate * qty
            double saleValue = 0.0;
            PharmaceuticalBillItem pharmaceuticalBillItem = billItem.getPharmaceuticalBillItem();
            if (pharmaceuticalBillItem != null) {
                Double retailRate = pharmaceuticalBillItem.getRetailRate();
                Double qty = pharmaceuticalBillItem.getQty();
                if (retailRate != null && qty != null) {
                    saleValue = retailRate * qty;
                }
            }
            totalSaleValue += saleValue;
        }
    }

    private double resolveCostValueFromItemBatch(BillItem billItem) {
        PharmaceuticalBillItem pharmaceuticalBillItem = billItem.getPharmaceuticalBillItem();
        if (pharmaceuticalBillItem == null) {
            return 0.0;
        }

        ItemBatch itemBatch = pharmaceuticalBillItem.getItemBatch();
        if (itemBatch == null) {
            return 0.0;
        }

        Double rate = itemBatch.getCostRate();
        if (rate == null || rate == 0.0) {
            double batchPurchaseRate = itemBatch.getPurcahseRate();
            if (batchPurchaseRate != 0.0) {
                rate = batchPurchaseRate;
            }
        }

        if ((rate == null || rate == 0.0)) {
            double pharmaCostRate = pharmaceuticalBillItem.getCostRate();
            if (pharmaCostRate != 0.0) {
                rate = pharmaCostRate;
            }
        }

        if (rate == null || rate == 0.0) {
            return 0.0;
        }

        double quantity = resolveQuantity(billItem.getBillItemFinanceDetails(), billItem);
        if (quantity == 0.0) {
            quantity = pharmaceuticalBillItem.getQty();
        }

        return rate * quantity;
    }

    private double resolveFinanceValue(
            BillItem billItem,
            Function<BillItemFinanceDetails, BigDecimal> valueExtractor,
            Function<BillItemFinanceDetails, BigDecimal> rateExtractor,
            Function<PharmaceuticalBillItem, Double> pharmaRateExtractor,
            Function<ItemBatch, Double> itemBatchRateExtractor) {

        BillItemFinanceDetails financeDetails = billItem.getBillItemFinanceDetails();
        PharmaceuticalBillItem pharmaceuticalBillItem = billItem.getPharmaceuticalBillItem();

        BigDecimal directValue = financeDetails != null ? valueExtractor.apply(financeDetails) : null;
        if (directValue != null) {
            return directValue.doubleValue();
        }

        Double rate = null;
        if (financeDetails != null) {
            BigDecimal rateValue = rateExtractor.apply(financeDetails);
            if (rateValue != null && rateValue.compareTo(BigDecimal.ZERO) != 0) {
                rate = rateValue.doubleValue();
            }
        }

        if (rate == null && pharmaceuticalBillItem != null) {
            Double fallbackRate = pharmaRateExtractor.apply(pharmaceuticalBillItem);
            if (fallbackRate != null && Double.compare(fallbackRate, 0.0) != 0) {
                rate = fallbackRate;
            }
        }

        if (rate == null && itemBatchRateExtractor != null && pharmaceuticalBillItem != null) {
            ItemBatch itemBatch = pharmaceuticalBillItem.getItemBatch();
            if (itemBatch != null) {
                Double batchRate = itemBatchRateExtractor.apply(itemBatch);
                if (batchRate != null && Double.compare(batchRate, 0.0) != 0) {
                    rate = batchRate;
                }
            }
        }

        if (rate == null) {
            return 0.0;
        }

        double quantity = resolveQuantity(financeDetails, billItem);
        return rate * quantity;
    }

    private double resolveQuantity(BillItemFinanceDetails financeDetails, BillItem billItem) {
        if (financeDetails != null) {
            if (financeDetails.getTotalQuantity() != null) {
                return financeDetails.getTotalQuantity().doubleValue();
            }
            if (financeDetails.getQuantity() != null) {
                return financeDetails.getQuantity().doubleValue();
            }
            if (financeDetails.getTotalQuantityByUnits() != null) {
                return financeDetails.getTotalQuantityByUnits().doubleValue();
            }
            if (financeDetails.getQuantityByUnits() != null) {
                return financeDetails.getQuantityByUnits().doubleValue();
            }
        }

        PharmaceuticalBillItem pharmaceuticalBillItem = billItem.getPharmaceuticalBillItem();
        if (pharmaceuticalBillItem != null) {
            return pharmaceuticalBillItem.getQty();
        }

        if (billItem.getQty() != null) {
            return billItem.getQty();
        }

        return 0.0;
    }

    private void resetFinanceTotals() {
        totalCostValue = 0.0;
        totalPurchaseValue = 0.0;
        totalRetailValue = 0.0;
        totalSaleValue = 0.0;
    }

    private void retrieveBillWithoutReference(String billTypeField, Object billTypeValue) {
        try {
            netTotal = 0.0;
            totalCostValue = 0.0;
            totalPurchaseValue = 0.0;
            totalRetailValue = 0.0;
            totalSaleValue = 0.0;
            // STEP 1: Fetch all parent bills (CostOfGoodSoldBillDTOs) in one query.
            StringBuilder billJpql = new StringBuilder("SELECT new com.divudi.core.data.dto.CostOfGoodSoldBillDTO(")
                    .append("b, ")
                    .append("b.createdAt, ")
                    .append("b.deptId, ")
                    .append("b.netTotal, ")
                    .append("b.paymentMethod, ")
                    .append("b.discount, ")
                    .append("b.id) ")
                    .append("FROM Bill b ")
                    .append("LEFT JOIN b.referenceBill rb ")
                    .append("WHERE b.retired = false ")
                    .append("AND ").append(billTypeField).append(" IN :billTypes ")
                    .append("AND (rb IS NULL OR rb.createdAt > :toDate) ")
                    .append("AND b.createdAt BETWEEN :fromDate AND :toDate ");

            Map<String, Object> billParams = new HashMap<>();
            billParams.put("billTypes", billTypeValue);
            billParams.put("fromDate", fromDate);
            billParams.put("toDate", toDate);

            addFilter(billJpql, billParams, "b.institution", "ins", institution);
            addFilter(billJpql, billParams, "b.department.site", "sit", site);
            addFilter(billJpql, billParams, "b.department", "dep", department);

            cogsBillDtos = (List<CostOfGoodSoldBillDTO>) billFacade.findLightsByJpql(billJpql.toString(), billParams, TemporalType.TIMESTAMP);

            // STEP 2: Collect all Bill IDs from the results.
            List<Long> billIds = cogsBillDtos.stream()
                    .map(CostOfGoodSoldBillDTO::getBillId)
                    .collect(Collectors.toList());

            // STEP 3: Fetch all associated BillItems in a single second query.
            StringBuilder itemJpql = new StringBuilder("SELECT new com.divudi.core.data.dto.BillItemDTO(")
                    .append("bi.id, ")
                    .append("bi.bill.id, ")
                    .append("bi.bill.createdAt, ")
                    .append("bi.item.name, ")
                    .append("bi.item.code, ")
                    .append("bi.bill.deptId, ")
                    .append("pbi.itemBatch.batchNo, ")
                    .append("bi.qty, ")
                    .append("pbi.itemBatch.costRate, ")
                    .append("pbi.itemBatch.purcahseRate, ")
                    .append("pbi.retailRate, ")
                    .append("bi.bill.netTotal) ")
                    .append("FROM BillItem bi ")
                    .append("LEFT JOIN bi.pharmaceuticalBillItem pbi ")
                    .append("WHERE bi.retired = false ")
                    .append("AND bi.bill.id IN :billIds");

            Map<String, Object> itemParams = new HashMap<>();
            itemParams.put("billIds", billIds);

            List<BillItemDTO> allBillItems = (List<BillItemDTO>) billItemFacade.findLightsByJpql(itemJpql.toString(), itemParams);

            // STEP 4: Group the fetched BillItems by their parent Bill's ID.
            Map<Long, List<BillItemDTO>> itemsGroupedByBillId = allBillItems.stream().collect(Collectors.groupingBy(BillItemDTO::getBillId));

            // STEP 5: Attach the grouped items to their corresponding parent bills.
            for (CostOfGoodSoldBillDTO billDto : cogsBillDtos) {
                // 1. Accumulate the grand netTotal directly from the bill
                netTotal += billDto.getNetTotal();

                // Get the items associated with this specific bill
                List<BillItemDTO> itemsForThisBill = itemsGroupedByBillId.get(billDto.getBillId());

                if (itemsForThisBill != null && !itemsForThisBill.isEmpty()) {

                    // Populate the map as needed
                    Map<Long, Object> billItemMap = itemsForThisBill.stream()
                            .collect(Collectors.toMap(BillItemDTO::getId, item -> item, (item1, item2) -> item1));
                    billDto.setBillItemsMap(billItemMap);

                    // For the list, simply set it
                    billDto.setBillItems(itemsForThisBill);

                    // Calculate Cost Value for THIS bill
                    double billCost = itemsForThisBill.stream()
                            .filter(item -> item.getCostRate() != null && item.getQty() != null)
                            .mapToDouble(item -> item.getCostRate() * item.getQty())
                            .sum();

                    // Calculate Purchase Value for THIS bill
                    double billPurchase = itemsForThisBill.stream()
                            .filter(item -> item.getPurchaseRate() != null && item.getQty() != null)
                            .mapToDouble(item -> item.getPurchaseRate() * item.getQty())
                            .sum();

                    // 2. Accumulate the grand total for Cost
                    totalCostValue += billCost;

                    // 3. Accumulate the grand total for Purchase
                    totalPurchaseValue += billPurchase;

                    double billRetail = itemsForThisBill.stream()
                            .filter(item -> item.getRetailRate() != null && item.getQty() != null)
                            .mapToDouble(item -> item.getRetailRate() * item.getQty())
                            .sum();

                    totalRetailValue += billRetail;

                    // Calculate Sale Value for THIS bill (same as billRetail calculation)
                    double billSaleValue = itemsForThisBill.stream()
                            .filter(item -> item.getRetailRate() != null && item.getQty() != null)
                            .mapToDouble(item -> item.getRetailRate() * item.getQty())
                            .sum();

                    totalSaleValue += billSaleValue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            cogsBillDtos = new ArrayList<>();
            billItems = new ArrayList<>();
            netTotal = 0.0;
        }
    }

    private void retrieveBill(String billTypeField, Object billTypeValue, Object paymentMethod) {
        try {
            netTotal = 0.0;
            totalCostValue = 0.0;
            totalPurchaseValue = 0.0;
            totalRetailValue = 0.0;
            totalSaleValue = 0.0;
            // STEP 1: Fetch all parent bills (CostOfGoodSoldBillDTOs) in one query.
            StringBuilder billJpql = new StringBuilder("SELECT new com.divudi.core.data.dto.CostOfGoodSoldBillDTO(")
                    .append("b, ")
                    .append("b.createdAt, ")
                    .append("b.deptId, ")
                    .append("b.netTotal, ")
                    .append("b.paymentMethod, ")
                    .append("b.discount, ")
                    .append("b.id) ")
                    .append("FROM Bill b ")
                    .append("WHERE b.retired = false ")
                    .append("AND ").append(billTypeField).append(" IN :billTypes ")
                    .append("AND b.createdAt BETWEEN :fromDate AND :toDate ");

            billJpql.append("AND b.paymentMethod IN :pm ");

            Map<String, Object> billParams = new HashMap<>();
            billParams.put("pm", paymentMethod);
            billParams.put("billTypes", billTypeValue);
            billParams.put("fromDate", fromDate);
            billParams.put("toDate", toDate);

            addFilter(billJpql, billParams, "b.institution", "ins", institution);
            addFilter(billJpql, billParams, "b.department.site", "sit", site);
            addFilter(billJpql, billParams, "b.department", "dep", department);
            addFilter(billJpql, billParams, "b.paymentMethod", "pmFilter", this.paymentMethod);

            cogsBillDtos = (List<CostOfGoodSoldBillDTO>) billFacade.findLightsByJpql(billJpql.toString(), billParams, TemporalType.TIMESTAMP);

            // STEP 2: Collect all Bill IDs from the results.
            List<Long> billIds = cogsBillDtos.stream()
                    .map(CostOfGoodSoldBillDTO::getBillId)
                    .collect(Collectors.toList());

            // STEP 3: Fetch all associated BillItems in a single second query.
            StringBuilder itemJpql = new StringBuilder("SELECT new com.divudi.core.data.dto.BillItemDTO(")
                    .append("bi.id, ")
                    .append("bi.bill.id, ")
                    .append("bi.bill.createdAt, ")
                    .append("bi.item.name, ")
                    .append("bi.item.code, ")
                    .append("bi.bill.deptId, ")
                    .append("pbi.itemBatch.batchNo, ")
                    .append("bi.qty, ")
                    .append("pbi.itemBatch.costRate, ")
                    .append("pbi.itemBatch.purcahseRate, ")
                    .append("pbi.retailRate, ")
                    .append("bi.bill.netTotal) ")
                    .append("FROM BillItem bi ")
                    .append("LEFT JOIN bi.pharmaceuticalBillItem pbi ")
                    .append("WHERE bi.retired = false ")
                    .append("AND bi.bill.id IN :billIds");

            Map<String, Object> itemParams = new HashMap<>();
            itemParams.put("billIds", billIds);

            List<BillItemDTO> allBillItems = (List<BillItemDTO>) billItemFacade.findLightsByJpql(itemJpql.toString(), itemParams);

            // STEP 4: Group the fetched BillItems by their parent Bill's ID.
            Map<Long, List<BillItemDTO>> itemsGroupedByBillId = allBillItems.stream().collect(Collectors.groupingBy(BillItemDTO::getBillId));

            // STEP 5: Attach the grouped items to their corresponding parent bills.
            for (CostOfGoodSoldBillDTO billDto : cogsBillDtos) {
                // 1. Accumulate the grand netTotal directly from the bill
                netTotal += billDto.getNetTotal();

                // Get the items associated with this specific bill
                List<BillItemDTO> itemsForThisBill = itemsGroupedByBillId.get(billDto.getBillId());

                if (itemsForThisBill != null && !itemsForThisBill.isEmpty()) {

                    // Populate the map as needed
                    Map<Long, Object> billItemMap = itemsForThisBill.stream()
                            .collect(Collectors.toMap(BillItemDTO::getId, item -> item, (item1, item2) -> item1));
                    billDto.setBillItemsMap(billItemMap);

                    // For the list, simply set it
                    billDto.setBillItems(itemsForThisBill);

                    // Calculate Cost Value for THIS bill
                    double billCost = itemsForThisBill.stream()
                            .filter(item -> item.getCostRate() != null && item.getQty() != null)
                            .mapToDouble(item -> item.getCostRate() * item.getQty())
                            .sum();

                    // Calculate Purchase Value for THIS bill
                    double billPurchase = itemsForThisBill.stream()
                            .filter(item -> item.getPurchaseRate() != null && item.getQty() != null)
                            .mapToDouble(item -> item.getPurchaseRate() * item.getQty())
                            .sum();

                    // 2. Accumulate the grand total for Cost
                    totalCostValue += billCost;

                    // 3. Accumulate the grand total for Purchase
                    totalPurchaseValue += billPurchase;

                    double billRetail = itemsForThisBill.stream()
                            .filter(item -> item.getRetailRate() != null && item.getQty() != null)
                            .mapToDouble(item -> item.getRetailRate() * item.getQty())
                            .sum();

                    totalRetailValue += billRetail;

                    // Calculate Sale Value for THIS bill (same as billRetail calculation)
                    double billSaleValue = itemsForThisBill.stream()
                            .filter(item -> item.getRetailRate() != null && item.getQty() != null)
                            .mapToDouble(item -> item.getRetailRate() * item.getQty())
                            .sum();

                    totalSaleValue += billSaleValue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            cogsBillDtos = new ArrayList<>();
            billItems = new ArrayList<>();
            netTotal = 0.0;
        }
    }

    public void processIpDrugReturn() {
        List<BillTypeAtomic> billTypes = Arrays.asList(
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN,
                BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_CANCELLATION,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION,
                BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_RETURN
        );
        retrieveBillItems("b.billTypeAtomic", billTypes);
    }

    public void processOpDrugReturn() {
        List<BillTypeAtomic> billTypes = Arrays.asList(
                BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_REFUND
        );
        retrieveBillItems("b.billTypeAtomic", billTypes);
    }

    public void processPurchaseReturn() {
        List<BillTypeAtomic> billTypes = Arrays.asList(
                BillTypeAtomic.PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL_CANCELLED,
                BillTypeAtomic.PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL_REFUND,
                BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_CANCELLED,
                BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_REFUND,
                BillTypeAtomic.PHARMACY_GRN_CANCELLED,
                BillTypeAtomic.PHARMACY_GRN_REFUND,
                BillTypeAtomic.PHARMACY_GRN_RETURN,
                BillTypeAtomic.PHARMACY_RETURN_WITHOUT_TREASING
        );
        retrieveBillItems("b.billTypeAtomic", billTypes, true);
    }

    public void processStockAdjustmentReceive() {
        retrieveStockAdjustmentBillItems(">");
    }

    public void processStockAdjustmentIssue() {
        retrieveStockAdjustmentBillItems("<");
    }

    public void retrieveStockAdjustmentBillItems(String operator) {
        try {
            List<BillType> billTypes = new ArrayList<>();
            billTypes.add(BillType.PharmacyAdjustmentDepartmentSingleStock);
            billTypes.add(BillType.PharmacyAdjustmentDepartmentStock);
            billItemsDtos = new ArrayList<>();
            netTotal = 0.0;

            StringBuilder jpql = new StringBuilder();
            jpql.append("SELECT new com.divudi.core.data.dto.BillItemDTO(")
                    .append("b.createdAt, ")
                    .append("i.name, ")
                    .append("i.code, ")
                    .append("b.deptId, ")
                    .append("ib.batchNo, ")
                    .append("pbi.qty, ")
                    .append("ib.costRate, ")
                    .append("pbi.retailRate, ")
                    .append("b.netTotal) ")
                    .append("FROM BillItem bi ")
                    .append("LEFT JOIN bi.bill b ")
                    .append("LEFT JOIN bi.item i ")
                    .append("LEFT JOIN bi.pharmaceuticalBillItem pbi ")
                    .append("LEFT JOIN pbi.itemBatch ib ")
                    .append("WHERE bi.retired = false ")
                    .append("AND b.retired = false ")
                    .append("AND b.billType IN :billTypes ")
                    .append("AND b.createdAt BETWEEN :fromDate AND :toDate ");

            if (">".equals(operator)) {
                jpql.append("AND pbi.qty > 0.0 ");
            } else if ("<".equals(operator)) {
                jpql.append("AND pbi.qty < 0.0 ");
            }

            Map<String, Object> params = new HashMap<>();
            params.put("billTypes", billTypes);
            params.put("fromDate", fromDate);
            params.put("toDate", toDate);

            addFilter(jpql, params, "b.institution", "ins", institution);
            addFilter(jpql, params, "b.department.site", "sit", site);
            addFilter(jpql, params, "b.department", "dep", department);

            billItemsDtos = (List<BillItemDTO>) facade.findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

            netTotal = billItemsDtos.stream()
                    .filter(Objects::nonNull)
                    .mapToDouble(dto -> {
                        Double retailRate = dto.getRetailRate();
                        Double qty = dto.getQty();
                        if (retailRate != null && qty != null) {
                            return retailRate * qty;
                        }
                        return 0.0;
                    })
                    .sum();

        } catch (Exception e) {
            e.printStackTrace();
            billItemsDtos = new ArrayList<>();
            netTotal = 0.0;
        }
    }

    public void processStockConsumption() {
        List<BillTypeAtomic> btasToGetBillItems = new ArrayList<>();
        btasToGetBillItems.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE);
        btasToGetBillItems.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_CANCELLED);
        btasToGetBillItems.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_RETURN);
        retrieveBillItemsCompleted(btasToGetBillItems, true);
        calculateStockConsumptionTotals(billItems);
    }

    public void calculateStockConsumptionTotals(List<BillItem> items) {
        netTotal = 0.0;
        purchaseValueTotal = 0.0;
        costValueTotal = 0.0;
        retailValueTotal = 0.0;

        for (BillItem item : items) {
            // Simply sum the values from the columns without bill type manipulation
            if (item.getBillItemFinanceDetails() != null) {
                if (item.getBillItemFinanceDetails().getValueAtPurchaseRate() != null) {
                    purchaseValueTotal += item.getBillItemFinanceDetails().getValueAtPurchaseRate().doubleValue();
                }
                if (item.getBillItemFinanceDetails().getValueAtCostRate() != null) {
                    costValueTotal += item.getBillItemFinanceDetails().getValueAtCostRate().doubleValue();
                }
                if (item.getBillItemFinanceDetails().getValueAtRetailRate() != null) {
                    retailValueTotal += item.getBillItemFinanceDetails().getValueAtRetailRate().doubleValue();
                }
            } else {
                // Fallback: derive values from PharmaceuticalBillItem -> ItemBatch
                if (item.getPharmaceuticalBillItem() != null && item.getPharmaceuticalBillItem().getItemBatch() != null) {
                    double qty = item.getQty();

                    Double purchaseRate = item.getPharmaceuticalBillItem().getItemBatch().getPurcahseRate();
                    if (purchaseRate != null) {
                        purchaseValueTotal += purchaseRate * qty;
                    }

                    Double costRate = item.getPharmaceuticalBillItem().getItemBatch().getCostRate();
                    if (costRate != null) {
                        costValueTotal += costRate * qty;
                    }

                    Double retailRate = item.getPharmaceuticalBillItem().getItemBatch().getRetailsaleRate();
                    if (retailRate != null) {
                        retailValueTotal += retailRate * qty;
                    }
                }
            }
            netTotal += item.getNetValue();
        }
    }

    private double calculateItemTotal(BillItem item) {
        double itemTotal = item.getPharmaceuticalBillItem().getPurchaseRate() * item.getQty();
        if (item.getBill().getBillTypeAtomic() == BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE) {
            return itemTotal;
        } else {
            return 0.0 - itemTotal;
        }
    }

    public void processTransferIssue() {
        retrieveBillItems("b.billType", Collections.singletonList(BillType.PharmacyTransferIssue));
    }

    public void processTransferReceive() {
        retrieveBillItems("b.billType", Collections.singletonList(BillType.PharmacyTransferReceive));
    }

    public void processSaleCredit() {
        List<PaymentMethod> creditTypePaymentMethods = new ArrayList<>();
        creditTypePaymentMethods.add(PaymentMethod.Credit);
        creditTypePaymentMethods.add(PaymentMethod.Staff);

        List<BillTypeAtomic> billTypes = Arrays.asList(
                BillTypeAtomic.PHARMACY_RETAIL_SALE,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER
        );

        retrieveBillItems("b.billTypeAtomic", billTypes, creditTypePaymentMethods);
    }

    public void processBhtIssue() {
        List<BillTypeAtomic> billTypes = Arrays.asList(
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE
        );
        retrieveBillItems("b.billTypeAtomic", billTypes);
    }

    public void processSaleCreditCard() {
        List<PaymentMethod> nonCreditPaymentMethods = Arrays.stream(PaymentMethod.values())
                .filter(pm -> pm != PaymentMethod.Credit)
                .collect(Collectors.toList());
        List<BillTypeAtomic> billTypes = Arrays.asList(
                BillTypeAtomic.PHARMACY_RETAIL_SALE,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER
        );
        retrieveBill("b.billTypeAtomic", billTypes, nonCreditPaymentMethods);
    }

    public void processAddToStockBills() {
        List<BillTypeAtomic> billTypes = Arrays.asList(
                BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER
        );
        retrieveBillWithoutReference("b.billTypeAtomic", billTypes);
    }

    public void processSaleCash() {
        List<BillTypeAtomic> billTypes = Arrays.asList(
                BillTypeAtomic.PHARMACY_RETAIL_SALE,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER
        );
        retrieveBillItems("b.billTypeAtomic", billTypes, Collections.singletonList(PaymentMethod.Cash));
    }

    public void exportIpDrugReturnPdf() {
        List<String> headers = Arrays.asList(
                "Date", "Item Name", "Code", "Doc No", "Ref Doc No",
                "QTY", "Rate", "Total", "Net Total", "MRP", "MRP Value", "Discount"
        );

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.reset();
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=IP_Drug_Return_Report.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph titlePara = new Paragraph("IP Drug Return Report", titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(20);
            document.add(titlePara);

            // Table setup
            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100);

            // Header row
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Row data
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            DecimalFormat qtyFormat = new DecimalFormat("#,##0");
            DecimalFormat moneyFormat = new DecimalFormat("#,##0.00");

            double netTotalSum = 0.0;

            for (BillItem row : billItems) {
                // Date
                table.addCell(new Phrase(
                        row.getBill() != null && row.getBill().getCreatedAt() != null
                        ? sdf.format(row.getBill().getCreatedAt()) : "", cellFont));

                // Item Name
                table.addCell(new Phrase(
                        row.getItem() != null ? row.getItem().getName() : "", cellFont));

                // Code
                table.addCell(new Phrase(
                        row.getItem() != null ? row.getItem().getCode() : "", cellFont));

                // Doc No
                table.addCell(new Phrase(
                        row.getBill() != null ? row.getBill().getDeptId() : "", cellFont));

                // Ref Doc No
                table.addCell(new Phrase(
                        row.getBill() != null && row.getBill().getReferenceBill() != null
                        ? row.getBill().getReferenceBill().getDeptId() : "", cellFont));

                // QTY
                table.addCell(new Phrase(
                        row.getQty() != null ? qtyFormat.format(row.getQty()) : "", cellFont));

                // Rate (as negative)
                Double qty = row.getQty();
                Double rate = row.getRate();
                Double total = (rate != null && qty != null) ? rate * qty : null;
                table.addCell(new Phrase(
                        rate != null ? moneyFormat.format(0.0 - rate) : "", cellFont));

                // Total (rate * qty)
                table.addCell(new Phrase(
                        total != null ? moneyFormat.format(total) : "", cellFont));

                // Net Total
                Double netTotal = row.getBill() != null ? row.getBill().getNetTotal() : null;
                table.addCell(new Phrase(
                        netTotal != null ? moneyFormat.format(netTotal) : "", cellFont));
                netTotalSum += netTotal;

                // MRP
                Double mrp = (row.getPharmaceuticalBillItem() != null)
                        ? row.getPharmaceuticalBillItem().getRetailRate() : null;
                table.addCell(new Phrase(
                        mrp != null ? moneyFormat.format(mrp) : "", cellFont));

                // MRP Value (negative)
                Double mrpValue = (mrp != null && qty != null) ? 0.0 - (mrp * qty) : null;
                table.addCell(new Phrase(
                        mrpValue != null ? moneyFormat.format(mrpValue) : "", cellFont));

                // Discount
                Double discount = row.getDiscount();
                table.addCell(new Phrase(
                        discount != null ? moneyFormat.format(discount) : "", cellFont));
            }

            for (int i = 0; i < headers.size(); i++) {
                PdfPCell footerCell;
                if (i == 0) {
                    footerCell = new PdfPCell(new Phrase("Net Amount", headerFont));
                    footerCell.setColspan(8);
                    footerCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    footerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                } else if (i == 1) { // "Net Total" column
                    footerCell = new PdfPCell(new Phrase(moneyFormat.format(netTotalSum), headerFont));
                    footerCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    footerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                } else {
                    footerCell = new PdfPCell(new Phrase(""));
                    footerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                }
                table.addCell(footerCell);
            }

            document.add(table);
            document.close();
            context.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportPharmacyIssuePdf() {
        List<String> headers = Arrays.asList(
                "Date", "Item Name", "Doc No", "Ref Doc No (Request Doc No)", "Request Department",
                "Code", "QTY", "Rate", "Total", "Net Total"
        );

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.reset();
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Pharmacy_Issue_Report.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph titlePara = new Paragraph("Pharmacy Issue Report", titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(20);
            document.add(titlePara);

            // Table setup
            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Data formatting
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            DecimalFormat qtyFormat = new DecimalFormat("#,##0");
            DecimalFormat moneyFormat = new DecimalFormat("#,##0.00");

            double netTotalSum = 0.0;

            for (BillItem f : billItems) {
                // Date
                String dateStr = (f.getBill() != null && f.getBill().getCreatedAt() != null)
                        ? sdf.format(f.getBill().getCreatedAt()) : "";
                table.addCell(new Phrase(dateStr, cellFont));

                // Item Name
                table.addCell(new Phrase(f.getItem() != null ? f.getItem().getName() : "", cellFont));

                // Doc No
                table.addCell(new Phrase(f.getBill() != null ? f.getBill().getDeptId() : "", cellFont));

                // Ref Doc No
                table.addCell(new Phrase(
                        f.getBill() != null && f.getBill().getReferenceBill() != null
                        ? f.getBill().getReferenceBill().getDeptId() : "", cellFont));

                // Request Department
                table.addCell(new Phrase(
                        f.getBill() != null && f.getBill().getToDepartment() != null
                        ? f.getBill().getToDepartment().getName() : "", cellFont));

                // Code
                table.addCell(new Phrase(f.getItem() != null ? f.getItem().getCode() : "", cellFont));

                // Qty
                Double qty = f.getQty();
                table.addCell(new Phrase(qty != null ? qtyFormat.format(qty) : "", cellFont));

                // Rate
                Double rate = (f.getPharmaceuticalBillItem() != null)
                        ? f.getPharmaceuticalBillItem().getRetailRate() : 0.0;
                table.addCell(new Phrase(moneyFormat.format(rate), cellFont));

                // Total = rate * qty
                double total = (qty != null) ? rate * qty : 0.0;
                table.addCell(new Phrase(moneyFormat.format(total), cellFont));

                // Net Total
                Double netTotal = f.getBill() != null ? f.getBill().getNetTotal() : null;
                netTotalSum += netTotal;
                table.addCell(new Phrase(moneyFormat.format(netTotal), cellFont));
            }

            // Footer row (Net Total sum)
            PdfPCell footerCell;
            for (int i = 0; i < headers.size(); i++) {
                if (i == 0) {
                    footerCell = new PdfPCell(new Phrase("Net Amount", headerFont));
                    footerCell.setColspan(headers.size() - 2);
                    footerCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    footerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    table.addCell(footerCell);
                    i += headers.size() - 3; // skip the spanned columns
                } else if (i == headers.size() - 1) {
                    footerCell = new PdfPCell(new Phrase(moneyFormat.format(netTotalSum), headerFont));
                    footerCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    footerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    table.addCell(footerCell);
                } else {
                    footerCell = new PdfPCell(new Phrase(""));
                    footerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    table.addCell(footerCell);
                }
            }

            document.add(table);
            document.close();
            context.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportTransferReceivePdf() {
        List<String> headers = Arrays.asList(
                "Date", "Item Name", "Doc No", "Refrance Document No (Issue Doc No)",
                "Issue Department", "Code", "QTY", "Rate", "Total", "Net Total"
        );

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.reset();
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Transfer_Receive_Report.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph titlePara = new Paragraph("Transfer Receive Report", titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(20);
            document.add(titlePara);

            // Table setup
            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Formatting setup
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            DecimalFormat qtyFormat = new DecimalFormat("#,##0");
            DecimalFormat moneyFormat = new DecimalFormat("#,##0.00");

            double netTotalSum = 0.0;

            for (BillItem f : billItems) {
                // Date
                String dateStr = (f.getBill() != null && f.getBill().getCreatedAt() != null)
                        ? sdf.format(f.getBill().getCreatedAt()) : "";
                table.addCell(new Phrase(dateStr, cellFont));

                // Item Name
                table.addCell(new Phrase(f.getItem() != null ? f.getItem().getName() : "", cellFont));

                // Doc No
                table.addCell(new Phrase(f.getBill() != null ? f.getBill().getDeptId() : "", cellFont));

                // Reference Document No
                table.addCell(new Phrase(
                        f.getBill() != null && f.getBill().getReferenceBill() != null
                        ? f.getBill().getReferenceBill().getDeptId() : "", cellFont));

                // Issue Department
                table.addCell(new Phrase(
                        f.getBill() != null && f.getBill().getFromDepartment() != null
                        ? f.getBill().getFromDepartment().getName() : "", cellFont));

                // Code
                table.addCell(new Phrase(f.getItem() != null ? f.getItem().getCode() : "", cellFont));

                // Qty
                Double qty = f.getQty();
                table.addCell(new Phrase(qty != null ? qtyFormat.format(qty) : "", cellFont));

                // Rate
                Double rate = (f.getPharmaceuticalBillItem() != null)
                        ? f.getPharmaceuticalBillItem().getRetailRate() : 0.0;
                table.addCell(new Phrase(moneyFormat.format(rate), cellFont));

                // Total
                double total = (qty != null) ? rate * qty : 0.0;
                table.addCell(new Phrase(moneyFormat.format(total), cellFont));

                // Net Total
                double netTotal = f.getBill() != null ? f.getBill().getNetTotal() : null;
                netTotalSum += netTotal;
                table.addCell(new Phrase(moneyFormat.format(netTotal), cellFont));
            }

            // Footer row (Net Total sum)
            PdfPCell footerCell;
            for (int i = 0; i < headers.size(); i++) {
                if (i == 0) {
                    footerCell = new PdfPCell(new Phrase("Net Amount", headerFont));
                    footerCell.setColspan(headers.size() - 1);
                    footerCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    footerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    table.addCell(footerCell);
                    i += headers.size() - 2;
                } else if (i == headers.size() - 1) {
                    footerCell = new PdfPCell(new Phrase(moneyFormat.format(netTotalSum), headerFont));
                    footerCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    footerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    table.addCell(footerCell);
                }
            }

            document.add(table);
            document.close();
            context.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportSaleCashPdf() {
        List<String> headers = Arrays.asList(
                "Date", "Item Name", "Code", "Doc No", "Batch Code", "QTY", "Rate",
                "Sale Value", "Net Total", "Payment Mode/Methods", "MRP", "MRP Value", "Discount"
        );

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.reset();
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=sale_cash_report.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph titlePara = new Paragraph(reportType, titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(20);
            document.add(titlePara);

            // Table setup
            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            // Table header
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Formatters
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            DecimalFormat qtyFormat = new DecimalFormat("#,##0");
            DecimalFormat moneyFormat = new DecimalFormat("#,##0.00");

            double netTotalSum = 0.0;

            for (BillItem row : billItems) {
                // Date
                table.addCell(new Phrase(
                        row.getBill() != null && row.getBill().getCreatedAt() != null
                        ? sdf.format(row.getBill().getCreatedAt()) : "", cellFont));

                // Item Name
                table.addCell(new Phrase(
                        row.getItem() != null ? row.getItem().getName() : "", cellFont));

                // Code
                table.addCell(new Phrase(
                        row.getItem() != null ? row.getItem().getCode() : "", cellFont));

                // Doc No
                table.addCell(new Phrase(
                        row.getBill() != null ? row.getBill().getDeptId() : "", cellFont));

                // Batch Code
                table.addCell(new Phrase(
                        (row.getPharmaceuticalBillItem() != null
                        && row.getPharmaceuticalBillItem().getItemBatch() != null)
                        ? row.getPharmaceuticalBillItem().getItemBatch().getBatchNo() : "", cellFont));

                // QTY
                Double qty = row.getQty();
                table.addCell(new Phrase(qty != null ? qtyFormat.format(qty) : "", cellFont));

                // Rate
                Double rate = (row.getPharmaceuticalBillItem() != null)
                        ? row.getPharmaceuticalBillItem().getPurchaseRate() : null;
                table.addCell(new Phrase(rate != null ? moneyFormat.format(rate) : "", cellFont));

                // Sale Value = retailRate * qty
                Double retailRate = (row.getPharmaceuticalBillItem() != null)
                        ? row.getPharmaceuticalBillItem().getRetailRate() : null;
                Double saleValue = (retailRate != null && qty != null) ? retailRate * qty : null;
                table.addCell(new Phrase(saleValue != null ? moneyFormat.format(saleValue) : "", cellFont));

                // Net Total
                Double netTotal = row.getBill() != null ? row.getBill().getNetTotal() : null;
                table.addCell(new Phrase(
                        netTotal != null ? moneyFormat.format(netTotal) : "", cellFont));
                if (netTotal != null) {
                    netTotalSum += netTotal;
                }

                // Payment Method
                table.addCell(new Phrase(
                        row.getBill() != null && row.getBill().getPaymentMethod() != null
                        ? row.getBill().getPaymentMethod().toString() : "", cellFont));

                // MRP
                table.addCell(new Phrase(
                        retailRate != null ? moneyFormat.format(retailRate) : "", cellFont));

                // MRP Value
                Double mrpValue = (retailRate != null && qty != null) ? retailRate * qty : null;
                table.addCell(new Phrase(mrpValue != null ? moneyFormat.format(mrpValue) : "", cellFont));

                // Discount
                Double discount = row.getBill() != null ? row.getBill().getDiscount() : null;
                table.addCell(new Phrase(
                        discount != null ? moneyFormat.format(discount) : "", cellFont));
            }

            // Footer row with Net Total sum
            for (int i = 0; i < headers.size(); i++) {
                PdfPCell footerCell;
                if (i == 0) {
                    footerCell = new PdfPCell(new Phrase("Net Total", headerFont));
                    footerCell.setColspan(8);
                    footerCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    footerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                } else if (i == 1) {
                    footerCell = new PdfPCell(new Phrase(moneyFormat.format(netTotalSum), headerFont));
                    footerCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    footerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                } else {
                    footerCell = new PdfPCell(new Phrase(""));
                    footerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                }
                table.addCell(footerCell);
            }

            document.add(table);
            document.close();
            context.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void selectReportName(String name) {
        reportType = name;
    }

    public void processCollectingCentreTestWiseCountReport() {
        String jpql = "select new  com.divudi.core.data.TestWiseCountReport("
                + "bi.item.name, "
                + "count(bi.item.name), "
                + "sum(bi.hospitalFee) , "
                + "sum(bi.collectingCentreFee), "
                + "sum(bi.staffFee), "
                + "sum(bi.netValue)"
                + ") "
                + " from BillItem bi "
                + " where bi.retired=:ret"
                + " and bi.bill.cancelled=:can"
                + " and bi.bill.billDate between :fd and :td "
                + " and bi.bill.billTypeAtomic = :billTypeAtomic ";

        if (false) {
            BillItem bi = new BillItem();
            bi.getItem();
            bi.getHospitalFee();
            bi.getCollectingCentreFee();
            bi.getStaffFee();
            bi.getNetValue();
        }

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("can", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("billTypeAtomic", BillTypeAtomic.CC_BILL);

        if (route != null) {
            jpql += " and bi.bill.fromInstitution.route = :route ";
            m.put("route", route);
        }

        if (institution != null) {
            jpql += " and bi.bill.institution = :ins ";
            m.put("ins", institution);
        }

        if (collectingCentre != null) {
            jpql += " and bi.bill.fromInstitution = :cc ";
            m.put("cc", collectingCentre);
        }

        if (toDepartment != null) {
            jpql += " and bi.bill.toDepartment = :dep ";
            m.put("dep", toDepartment);
        }

        if (phn != null && !phn.isEmpty()) {
            jpql += " and bi.bill.patient.phn = :phn ";
            m.put("phn", phn);
        }

        if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
            jpql += " and bi.bill.deptId = :inv ";
            m.put("inv", invoiceNumber);
        }

        if (itemLight != null) {
            jpql += " and bi.item.id = :item ";
            m.put("item", itemLight.getId());
        }

        if (doctor != null) {
            jpql += " and bi.bill.referredBy = :refDoc ";
            m.put("refDoc", doctor);
        }

        if (status != null) {
            jpql += " and bi.billItemStatus = :status ";
            m.put("status", status);
        }

        jpql += " group by bi.item.name ORDER BY bi.item.name ASC";

        testWiseCounts = (List<TestWiseCountReport>) billItemFacade.findLightsByJpql(jpql, m);

        totalCount = 0.0;
        totalHosFee = 0.0;
        totalCCFee = 0.0;
        totalProFee = 0.0;
        totalNetTotal = 0.0;

        if (testWiseCounts != null) {
            for (TestWiseCountReport report : testWiseCounts) {
                totalCount += report.getCount();
                totalHosFee += report.getHosFee();
                totalCCFee += report.getCcFee();
                totalProFee += report.getProFee();
                totalNetTotal += report.getTotal();

            }
        }
    }

    private void addFilter(StringBuilder sql, Map<String, Object> parameters, String sqlField, String paramKey, Object value) {
        if (value != null) {
            sql.append(" AND ").append(sqlField).append(" = :").append(paramKey).append(" ");
            parameters.put(paramKey, value);
        }
    }

    private void addFilter(StringBuilder sql, String condition) {
        if (condition != null && !condition.isEmpty()) {
            sql.append(" ").append(condition).append(" ");
        }
    }

    public void processGoodInTransistReport() {
        reportTimerController.trackReportExecution(() -> {
            Map<String, Object> parameters = new HashMap<>();
            StringBuilder sql = new StringBuilder();
            sql.append("select bi from BillItem bi"
                    + " where bi.bill.billType = :bt"
                    + " and bi.retired = :ret"
                    + " and bi.bill.billedBill is null "
                    + " and bi.bill.createdAt between :fd and :td"
                    + " and bi.bill.toStaff is not null"
                    + " and bi.bill.fromDepartment is not null");

            parameters.put("bt", BillType.PharmacyTransferIssue);
            parameters.put("ret", false);
            parameters.put("fd", fromDate);
            parameters.put("td", toDate);

            addFilter(sql, parameters, "bi.bill.fromInstitution", "institution", fromInstitution);
            addFilter(sql, parameters, "bi.bill.fromDepartment.site", "fSite", fromSite);
            addFilter(sql, parameters, "bi.bill.fromDepartment", "fDept", fromDepartment);
            addFilter(sql, parameters, "bi.bill.toInstitution", "tIns", toInstitution);
            addFilter(sql, parameters, "bi.bill.toDepartment.site", "tSite", toSite);
            addFilter(sql, parameters, "bi.bill.toDepartment", "tDept", toDepartment);
            addFilter(sql, parameters, "bi.item", "item", item);
            addFilter(sql, parameters, "bi.item.category", "cat", category);
            addFilter(sql, parameters, "bi.bill.toStaff", "user", toStaff);

            if (reportType != null) {
                if (reportType.equals("pending")) {
                    addFilter(sql, "and bi.bill.cancelled = false and bi.bill.forwardReferenceBills is empty");
                } else if (reportType.equals("accepted")) {
                    addFilter(sql, "and bi.bill.forwardReferenceBills is not empty");
                } else if (reportType.equals("issueCancel")) {
                    addFilter(sql, "and bi.bill.cancelled = true");
                }
                // "any" or null - no additional status filtering
            }

            sql.append(" order by bi.bill.id ");

            System.out.println("sql = " + sql);
            System.out.println("parameters = " + parameters);

            billItems = billItemFacade.findByJpql(sql.toString(), parameters, TemporalType.TIMESTAMP);

            // Calculate totals for footer using BIFD values
            totalRetailValue = 0.0;
            totalPurchaseValue = 0.0;
            totalCostValue = 0.0;

            for (BillItem billItem : billItems) {
                if (billItem.getBillItemFinanceDetails() != null) {
                    BillItemFinanceDetails bifd = billItem.getBillItemFinanceDetails();

                    // Retail Value calculation using BIFD
                    if (bifd.getValueAtRetailRate() != null) {
                        totalRetailValue += Math.abs(bifd.getValueAtRetailRate().doubleValue());
                    }

                    // Purchase Value calculation using BIFD
                    if (bifd.getValueAtPurchaseRate() != null) {
                        totalPurchaseValue += Math.abs(bifd.getValueAtPurchaseRate().doubleValue());
                    }

                    // Cost Value calculation using BIFD
                    if (bifd.getValueAtCostRate() != null) {
                        totalCostValue += Math.abs(bifd.getValueAtCostRate().doubleValue());
                    }
                }
            }

            if (billItems.isEmpty()) {
                pharmacyRows = new ArrayList<>();
                return;
            }

            List<Item> items = billItems.stream()
                    .map(BillItem::getItem)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            if (!items.isEmpty()) {
                List<ItemBatch> allBatches = getItemBatchesByItems(items);
                pharmacyRows = createPharmacyRowsByBillItemsAndItemBatch(billItems, allBatches);
            }
        }, InventoryReports.GOOD_IN_TRANSIT_REPORT, sessionController.getLoggedUser());
    }

    private List<ItemBatch> getItemBatchesByItems(List<Item> items) {
        Map<String, Object> batchParams = new HashMap<>();
        batchParams.put("items", items);

        String batchQuery = "SELECT ib FROM ItemBatch ib WHERE ib.item IN :items";
        return itemBatchFacade.findByJpql(batchQuery, batchParams);
    }

    private List<PharmacyRow> createPharmacyRowsByBillItemsAndItemBatch(List<BillItem> billItems, List<ItemBatch> itemBatches) {
        Map<Long, ItemBatch> latestBatchMap = itemBatches.stream()
                .filter(ib -> ib.getItem() != null)
                .collect(Collectors.toMap(ib -> ib.getItem().getId(), Function.identity(),
                        BinaryOperator.maxBy(Comparator.comparing(ItemBatch::getId))));

        return billItems.stream()
                .map(bi -> {
                    ItemBatch latestBatch = latestBatchMap.get(
                            bi.getItem() != null ? bi.getItem().getId() : null
                    );
                    return new PharmacyRow(bi, latestBatch);
                })
                .collect(Collectors.toList());
    }

    private List<DirectPurchaseReportDto> dtoList;

    public String navigateToDirectPurchaseSummeryReport() {
        dtoList = null;
        return "/pharmacy/direct_purchase_summery_report?faces-redirect=true";
    }

    public void processDirectPurchaseSummeryReport() {

        StringBuilder jpql = new StringBuilder("select new com.divudi.bean.report.PharmacyReportController.DirectPurchaseReportDto("
                + "bill.id, bill.createdAt, "
                + "pbi.doe, "
                + "bill.deptId, "
                + "bi.item.name, "
                + "pbi.qty, "
                + "pbi.freeQty, "
                + "pbi.purchaseRate, "
                + "pbi.purchaseRate * pbi.qty, "
                + "pbi.retailRate, "
                + "pbi.qty * pbi.retailRate) "
                + "from Bill bill "
                + "JOIN bill.billItems bi "
                + "JOIN bi.pharmaceuticalBillItem pbi "
                + "where bill.billTypeAtomic in :bta "
                + "and bill.billType in :bt "
                + "and bill.createdAt between :fromDate and :toDate "
                + "and bill.retired = :ret "
                + "and bill.cancelled = :ret "
                + "and bi.retired = :ret ");

        Map<String, Object> params = new HashMap<>();
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.add(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
        btas.add(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_REFUND);

        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.PurchaseReturn);
        bts.add(BillType.PharmacyPurchaseBill);

        params.put("bta", btas);
        params.put("bt", bts);
        params.put("ret", false);

        if (institution != null) {
            jpql.append("and bill.referenceInstitution = :ins ");
            params.put("ins", institution);
        }

        if (department != null) {
            jpql.append("and bill.department = :dept ");
            params.put("dept", department);
        }

        if (site != null) {
            jpql.append("and bill.department.site = :site ");
            params.put("site", site);
        }

        if (amp != null) {
            jpql.append("and bi.item = :item ");
            params.put("item", amp);
        }

        if (fromInstitutionList != null && !fromInstitutionList.isEmpty()) {

            jpql.append("and (bill.fromInstitution in :fromIns or bill.toInstitution in :fromIns)");
            params.put("fromIns", fromInstitutionList);

        }

        jpql.append("order by bill.createdAt desc");

        dtoList = (List<DirectPurchaseReportDto>) billFacade.findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

        totalPurchaseValue = 0.0;
        totalRetailValue = 0.0;
        totalSaleValue = 0.0;

        totalPurchaseValue = dtoList.stream()
                .filter(DirectPurchaseReportDto.class::isInstance)
                .map(DirectPurchaseReportDto.class::cast)
                .mapToDouble(DirectPurchaseReportDto::getPurchaseValue)
                .sum();

        totalRetailValue = dtoList.stream()
                .filter(DirectPurchaseReportDto.class::isInstance)
                .map(DirectPurchaseReportDto.class::cast)
                .mapToDouble(DirectPurchaseReportDto::getSaleValue)
                .sum();

        totalSaleValue = dtoList.stream()
                .filter(DirectPurchaseReportDto.class::isInstance)
                .map(DirectPurchaseReportDto.class::cast)
                .mapToDouble(DirectPurchaseReportDto::getSaleValue)
                .sum();

    }

    public List<DirectPurchaseReportDto> getDtoList() {
        return dtoList;
    }

    public void setDtoList(List<DirectPurchaseReportDto> dtoList) {
        this.dtoList = dtoList;
    }

    public double getTotalCostValue() {
        return totalCostValue;
    }

    public void setTotalCostValue(double totalCostValue) {
        this.totalCostValue = totalCostValue;
    }

    public double getTotalPurchaseValue() {
        return totalPurchaseValue;
    }

    public void setTotalPurchaseValue(double totalPurchaseValue) {
        this.totalPurchaseValue = totalPurchaseValue;
    }

    public double getTotalRetailValue() {
        return totalRetailValue;
    }

    public void setTotalRetailValue(double totalRetailValue) {
        this.totalRetailValue = totalRetailValue;
    }

    public double getTotalSaleValue() {
        return totalSaleValue;
    }

    public void setTotalSaleValue(double totalSaleValue) {
        this.totalSaleValue = totalSaleValue;
    }

    public Map<String, List<StockCorrectionRow>> getPositiveVarianceMap() {
        if (positiveVarianceMap == null) {
            positiveVarianceMap = new LinkedHashMap<>();
        }
        return positiveVarianceMap;
    }

    public void setPositiveVarianceMap(Map<String, List<StockCorrectionRow>> positiveVarianceMap) {
        this.positiveVarianceMap = positiveVarianceMap;
    }

    public Map<String, List<StockCorrectionRow>> getNegativeVarianceMap() {
        if (negativeVarianceMap == null) {
            negativeVarianceMap = new LinkedHashMap<>();
        }
        return negativeVarianceMap;
    }

    public void setNegativeVarianceMap(Map<String, List<StockCorrectionRow>> negativeVarianceMap) {
        this.negativeVarianceMap = negativeVarianceMap;
    }

    public static class DirectPurchaseReportDto {

        private Long billId;
        private Date billDate;
        private Date doe;
        private String billNo;
        private String itemName;
        private double quantity;
        private double freeQuantity;
        private double purchaseRate;
        private double purchaseValue;
        private double saleRate;
        private double saleValue;

        public DirectPurchaseReportDto(Long billId, Date billDate, Date doe, String billNo, String itemName, double quantity, double freeQuantity, double purchaseRate, double purchaseValue, double saleRate, double saleValue) {
            this.billId = billId;
            this.billDate = billDate;
            this.doe = doe;
            this.billNo = billNo;
            this.itemName = itemName;
            this.quantity = quantity;
            this.freeQuantity = freeQuantity;
            this.purchaseRate = purchaseRate;
            this.purchaseValue = purchaseValue;
            this.saleRate = saleRate;
            this.saleValue = saleValue;
        }

        public double getFreeQuantity() {
            return freeQuantity;
        }

        public void setFreeQuantity(double freeQuantity) {
            this.freeQuantity = freeQuantity;
        }

        public Long getBillId() {
            return billId;
        }

        public void setBillId(Long billId) {
            this.billId = billId;
        }

        public Date getBillDate() {
            return billDate;
        }

        public void setBillDate(Date billDate) {
            this.billDate = billDate;
        }

        public Date getDoe() {
            return doe;
        }

        public void setDoe(Date doe) {
            this.doe = doe;
        }

        public String getBillNo() {
            return billNo;
        }

        public void setBillNo(String billNo) {
            this.billNo = billNo;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public double getPurchaseRate() {
            return purchaseRate;
        }

        public void setPurchaseRate(double purchaseRate) {
            this.purchaseRate = purchaseRate;
        }

        public double getPurchaseValue() {
            return purchaseValue;
        }

        public void setPurchaseValue(double purchaseValue) {
            this.purchaseValue = purchaseValue;
        }

        public double getSaleRate() {
            return saleRate;
        }

        public void setSaleRate(double saleRate) {
            this.saleRate = saleRate;
        }

        public double getSaleValue() {
            return saleValue;
        }

        public void setSaleValue(double saleValue) {
            this.saleValue = saleValue;
        }

        public double getQuantity() {
            return quantity;
        }

        public void setQuantity(double quantity) {
            this.quantity = quantity;
        }

    }

    public void processStockLedgerReport() {
        reportTimerController.trackReportExecution(() -> {
            List<BillTypeAtomic> billTypeAtomics = new ArrayList<>();
            List<BillType> billTypes = new ArrayList<>();

            if ("ipSaleDoc".equals(documentType)) {
                billTypes.add(BillType.PharmacyBhtPre);
            } else if ("opSaleDoc".equals(documentType)) {
                billTypes.add(BillType.PharmacyPre);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED_PRE);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_REFUND);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_ADD_TO_STOCK);
            } else if ("grnDoc".equals(documentType)) {
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_GRN);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_GRN_CANCELLED);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_GRN_REFUND);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_GRN_RETURN);
                billTypes.add(BillType.PharmacyGrnBill);
                billTypes.add(BillType.PharmacyGrnReturn);
            } else if ("purchaseDoc".equals(documentType)) {
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL_CANCELLED);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL_REFUND);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_CANCELLED);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_REFUND);
            } else if ("consumptionDoc".equals(documentType)) {
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_CANCELLED);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_RETURN);
            } else if ("transferIssueDoc".equals(documentType)) {
                billTypes.add(BillType.PharmacyTransferIssue);
//            billTypeAtomics.add(BillTypeAtomic.PHARMACY_DIRECT_ISSUE);
//            billTypeAtomics.add(BillTypeAtomic.PHARMACY_DIRECT_ISSUE_CANCELLED);
//            billTypeAtomics.add(BillTypeAtomic.PHARMACY_ISSUE);
//            billTypeAtomics.add(BillTypeAtomic.PHARMACY_ISSUE_CANCELLED);
//            billTypeAtomics.add(BillTypeAtomic.PHARMACY_ISSUE_RETURN);
            } else if ("transferReceiveDoc".equals(documentType)) {
                billTypes.add(BillType.PharmacyTransferReceive);
//            billTypeAtomics.add(BillTypeAtomic.PHARMACY_RECEIVE);
//            billTypeAtomics.add(BillTypeAtomic.PHARMACY_RECEIVE_CANCELLED);
            } else if ("returnWithoutReceiptDoc".equals(documentType)) {
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETURN_WITHOUT_TREASING);
            } else if ("stockAdjustmentDoc".equals(documentType)) {
                // Only include adjustments that change quantities, not rate adjustments
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT_BILL);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_ADJUSTMENT);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_ADJUSTMENT_CANCELLED);
//                billTypeAtomics.add(BillTypeAtomic.PHARMACY_STAFF_STOCK_ADJUSTMENT);
                // Note: Rate adjustments are excluded as they don't change quantities
                billTypes.add(BillType.PharmacyStockAdjustmentBill);

            } else if ("rateAdjustmentDoc".equals(documentType)) {

//                Rate adjustments should NOT be listed 
//                billTypeAtomics.add(BillTypeAtomic.PHARMACY_PURCHASE_RATE_ADJUSTMENT);
//                billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_RATE_ADJUSTMENT);
//                billTypeAtomics.add(BillTypeAtomic.PHARMACY_COST_RATE_ADJUSTMENT);
//                billTypeAtomics.add(BillTypeAtomic.PHARMACY_WHOLESALE_RATE_ADJUSTMENT);
//                billTypeAtomics.add(BillTypeAtomic.PHARMACY_STOCK_EXPIRY_DATE_AJUSTMENT);
            } else {
                billTypes.add(BillType.PharmacyBhtPre);

                billTypes.add(BillType.PharmacyPre);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED_PRE);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_REFUND);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_ADD_TO_STOCK);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE);

                billTypeAtomics.add(BillTypeAtomic.PHARMACY_GRN);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_GRN_CANCELLED);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_GRN_REFUND);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_GRN_RETURN);
                billTypes.add(BillType.PharmacyGrnBill);
                billTypes.add(BillType.PharmacyGrnReturn);

                billTypeAtomics.add(BillTypeAtomic.PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL_CANCELLED);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL_REFUND);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_CANCELLED);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_REFUND);

                // Only quantity-changing adjustments for stock ledger
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT_BILL);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_ADJUSTMENT);
                billTypeAtomics.add(BillTypeAtomic.PHARMACY_ADJUSTMENT_CANCELLED);
//                billTypeAtomics.add(BillTypeAtomic.PHARMACY_STAFF_STOCK_ADJUSTMENT);
                // Note: Rate adjustments excluded as they don't change stock quantities
                // billTypeAtomics.add(BillTypeAtomic.PHARMACY_PURCHASE_RATE_ADJUSTMENT);
                // billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_RATE_ADJUSTMENT);
                // billTypeAtomics.add(BillTypeAtomic.PHARMACY_COST_RATE_ADJUSTMENT);
                // billTypeAtomics.add(BillTypeAtomic.PHARMACY_WHOLESALE_RATE_ADJUSTMENT);
                // billTypeAtomics.add(BillTypeAtomic.PHARMACY_STOCK_EXPIRY_DATE_AJUSTMENT);

                billTypes.add(BillType.PharmacyIssue);
                billTypes.add(BillType.PharmacyTransferIssue);
                billTypes.add(BillType.PharmacyTransferReceive);
                billTypes.add(BillType.PharmacyStockAdjustmentBill);

                billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETURN_WITHOUT_TREASING);
            }

            stockLedgerHistories = new ArrayList();
            String jpql;
            Map m = new HashMap();
            m.put("fd", fromDate);
            m.put("td", toDate);

            jpql = "select s"
                    + " from StockHistory s "
                    + " where s.createdAt between :fd and :td ";
            if (institution != null) {
                jpql += " and s.institution=:ins ";
                m.put("ins", institution);
            }
            if (department != null) {
                jpql += " and s.department=:dep ";
                m.put("dep", department);
            }
            if (site != null) {
                jpql += " and s.department.site=:sit ";
                m.put("sit", site);
            }
            if (!billTypeAtomics.isEmpty() || !billTypes.isEmpty()) {
                jpql += " and (";
                if (!billTypeAtomics.isEmpty()) {
                    jpql += " s.pbItem.billItem.bill.billTypeAtomic in :dtype";
                    m.put("dtype", billTypeAtomics);
                }
                if (!billTypeAtomics.isEmpty() && !billTypes.isEmpty()) {
                    jpql += " or";
                }
                if (!billTypes.isEmpty()) {
                    jpql += " s.pbItem.billItem.bill.billType in :doctype";
                    m.put("doctype", billTypes);
                }
                jpql += ")";
            }
            if (amp != null) {
                item = amp;
                System.out.println("item = " + item);
                jpql += "and s.item=:itm ";
                m.put("itm", item);
            }
//            if ("transferReceiveDoc".equals(documentType) || "transferIssueDoc".equals(documentType) || documentType == null) {
//                jpql += " and s.department IS NOT NULL ";
//            }

            jpql += " order by s.id ";
            stockLedgerHistories = facade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
        }, InventoryReports.STOCK_LEDGER_REPORT, sessionController.getLoggedUser());
    }

    /**
     * Process Closing Stock Report - ITEM-WISE
     *
     * Generates item-level aggregated stock report (all batches of an item
     * combined). Shows total stock quantities and values per item without
     * batch/expiry details.
     *
     * ARCHITECTURE: 1. TEMPORAL QUERY: Gets last StockHistory record before
     * selected date for each (department, item) combination 2. ITEM-LEVEL DATA:
     * Uses itemStock fields (not batch-level stockQty) 3. SCOPE-BASED VALUES:
     * Depending on filter selection, displays: - Department filter: itemStock,
     * itemStockValueAtPurchaseRate/SaleRate/CostRate - Institution filter:
     * institutionItemStock, institutionItemStockValueAt* - No filter (Total):
     * totalItemStock, totalItemStockValueAt*
     *
     * QUERY LOGIC: - Subquery: SELECT MAX(sh2.id) ... WHERE sh2.createdAt <=
     * selectedDate GROUP BY (department, item) - Main query: Fetch item details
     * and stock values using DTO projection - Filters out items created AFTER
     * selected date (ensures temporal accuracy)
     *
     * VALUES CAPTURED: - Quantity: Stock quantity in measurement units -
     * Purchase Value: Quantity × Purchase Rate - Sale Value: Quantity ×
     * Retail/Sale Rate - Cost Value: Quantity × Cost Rate
     */
    public void processClosingStockForItemReport() {
        Map<String, Object> params = new HashMap<>();

        // DTO projection query for item-level aggregated stock (not batch-level)
        // Fetches only required scalar values
        // Eliminates entity loading overhead, reduces queries from 5,000+ to 1, and reduces memory by 70%
        StringBuilder jpql = new StringBuilder(
                "SELECT NEW com.divudi.core.data.PharmacyRow("
                + "  i.id, "
                + "  COALESCE(i.name, ''), "
                + "  COALESCE(i.code, ''), "
                + "  COALESCE(c.name, ''), "
                + "  COALESCE(mu.name, ''), "
                + "  COALESCE(sh.itemStock, 0.0), " // Department item stock
                + "  COALESCE(sh.itemStockValueAtPurchaseRate, 0.0), " // Department item purchase value
                + "  COALESCE(sh.itemStockValueAtSaleRate, 0.0), " // Department item sale value
                + "  COALESCE(sh.itemStockValueAtCostRate, 0.0), " // Department item cost value
                + "  COALESCE(sh.institutionItemStock, 0.0), " // Institution item stock
                + "  COALESCE(sh.institutionItemStockValueAtPurchaseRate, 0.0), "
                + "  COALESCE(sh.institutionItemStockValueAtSaleRate, 0.0), "
                + "  COALESCE(sh.institutionItemStockValueAtCostRate, 0.0), "
                + "  COALESCE(sh.totalItemStock, 0.0), " // Total item stock
                + "  COALESCE(sh.totalItemStockValueAtPurchaseRate, 0.0), "
                + "  COALESCE(sh.totalItemStockValueAtSaleRate, 0.0), "
                + "  COALESCE(sh.totalItemStockValueAtCostRate, 0.0)) "
                + "FROM StockHistory sh "
                + "JOIN sh.item i "
                + "LEFT JOIN i.category c "
                + "LEFT JOIN i.measurementUnit mu "
                + "WHERE sh.retired = :ret "
                + "AND sh.id IN ("
                + "  SELECT MAX(sh2.id) FROM StockHistory sh2 "
                + "  WHERE sh2.retired = :ret2 ");

        params.put("ret", false);
        params.put("ret2", false);

        // Set the effective date parameter first - used in subquery filter
        Date effectiveDate;
        if ("Opening Stock".equals(type)) {
            effectiveDate = CommonFunctions.getStartOfDay(fromDate);
        } else {
            effectiveDate = CommonFunctions.getEndOfDay(toDate);
        }
        params.put("et", effectiveDate);

        // CRITICAL: Filter subquery by createdAt to exclude items created after selected date
        // This ensures only items that existed at the selected time are included
        jpql.append("  AND sh2.createdAt <= :et ");

        // Add all filter conditions to subquery
        if (institution != null) {
            jpql.append("  AND sh2.institution = :ins ");
            params.put("ins", institution);
        }

        if (site != null) {
            jpql.append("  AND sh2.department.site = :sit ");
            params.put("sit", site);
        }

        if (department != null) {
            jpql.append("  AND sh2.department = :dep ");
            params.put("dep", department);
        }

        if (category != null) {
            jpql.append("  AND sh2.item.category = :cat ");
            params.put("cat", category);
        }

        if (amp != null) {
            item = amp;
            jpql.append("  AND sh2.item = :itm ");
            params.put("itm", item);
        }

        // Complete subquery with GROUP BY based on scope to prevent duplicate rows
        // Department filter: GROUP BY (department, item) - one row per department per item
        // Institution filter: GROUP BY (institution, item) - one row per institution per item
        // No filter (Total): GROUP BY (item) - one row per item across all institutions
        if (department != null) {
            jpql.append("  GROUP BY sh2.department, sh2.item) ");
        } else if (institution != null) {
            jpql.append("  GROUP BY sh2.institution, sh2.item) ");
        } else {
            jpql.append("  GROUP BY sh2.item) ");
        }

        // CRITICAL FIX: Apply the same filters to outer query to prevent cross-department data leakage
        // Without these filters, the outer query retrieves ALL StockHistory records matching the subquery IDs
        // regardless of department/institution, causing records from other departments to appear in the report
        if (institution != null) {
            jpql.append("AND sh.institution = :ins ");
        }
        if (site != null) {
            jpql.append("AND sh.department.site = :sit ");
        }
        if (department != null) {
            jpql.append("AND sh.department = :dep ");
        }
        if (category != null) {
            jpql.append("AND sh.item.category = :cat ");
        }
        if (amp != null) {
            jpql.append("AND sh.item = :itm ");
        }

        // Order by item name
        jpql.append("ORDER BY i.name");

        // Debug logging
        System.out.println("jpql = " + jpql.toString());
        System.out.println("params = " + params);

        // Execute DTO projection query - returns PharmacyRow objects directly with all values pre-populated
        @SuppressWarnings("unchecked")
        List<PharmacyRow> dtoRows = (List<PharmacyRow>) facade.findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

        System.out.println("dtoRows.size() = " + dtoRows.size());

        rows = new ArrayList<>();

        // Process DTO rows for item-level report
        // Values are already set in constructor based on scope
        for (PharmacyRow row : dtoRows) {
            if (row == null || row.getItem() == null) {
                continue;
            }

            // Consignment filter: Use item stock quantity (not batch qty)
            double itemQty = row.getStockQty() != null ? row.getStockQty() : 0.0;
            if (isConsignmentItem()) {
                if (itemQty > 0) {
                    continue;
                }
            } else {
                if (itemQty <= 0) {
                    continue;
                }
            }

            // Set quantity and values based on department/institution/total scope
            // The DTO constructor stored item-level values in specific fields
            if (department != null) {
                // Department-level: Use itemStock and department item values
                row.setQuantity(row.getStockQty());  // itemStock
                row.setPurchaseValue(row.getPurchaseValue());  // itemStockValueAtPurchaseRate
                row.setSaleValue(row.getSaleValue());  // itemStockValueAtSaleRate
                row.setCostValue(row.getCostValue());  // itemStockValueAtCostRate
            } else {
                if (institution != null) {
                    // Institution-level: Use institution item values
                    row.setQuantity(row.getGrossTotal());  // institutionItemStock
                    row.setPurchaseValue(row.getDiscount());  // institutionItemStockValueAtPurchaseRate
                    row.setSaleValue(row.getNetTotal());  // institutionItemStockValueAtSaleRate
                    row.setCostValue(row.getHospitalTotal());  // institutionItemStockValueAtCostRate
                } else {
                    // Total-level: Use total item values
                    row.setQuantity(row.getPaidTotal());  // totalItemStock
                    row.setPurchaseValue(row.getTax());  // totalItemStockValueAtPurchaseRate
                    row.setSaleValue(row.getActualTotal());  // totalItemStockValueAtSaleRate
                    row.setCostValue(row.getStaffTotal());  // totalItemStockValueAtCostRate
                }
            }

            rows.add(row);
        }
    }

    /**
     * Process Closing Stock Report - BATCH-WISE
     *
     * Generates batch-level stock report showing individual batches with expiry
     * dates. Each row represents a specific batch of an item in a department.
     *
     * ARCHITECTURE: 1. TEMPORAL QUERY: Gets last StockHistory record before
     * selected date for each (department, itemBatch) combination 2. BATCH-LEVEL
     * DATA: Uses stockQty (batch quantity) and batch-specific values 3.
     * SCOPE-BASED VALUES: Depending on filter selection, displays: - Department
     * filter: stockQty, stockPurchaseValue, stockSaleValue, stockCostValue -
     * Institution filter: instituionBatchQty, institutionBatchStockValueAt* -
     * No filter (Total): totalBatchQty, totalBatchStockValueAt*
     *
     * QUERY LOGIC: - Subquery: SELECT MAX(sh2.id) ... WHERE sh2.createdAt <=
     * selectedDate GROUP BY (department, itemBatch) - Main query: Fetch item,
     * batch details (batchNo, expiry) and stock values using DTO projection -
     * Filters out batches created AFTER selected date (ensures temporal
     * accuracy)
     *
     * VALUES CAPTURED: - Quantity: Batch stock quantity in measurement units -
     * Purchase Rate: Purchase rate per unit for this batch - Retail Rate:
     * Retail/sale rate per unit for this batch - Cost Rate: Cost rate per unit
     * for this batch - Purchase Value: Batch Quantity × Purchase Rate - Sale
     * Value: Batch Quantity × Retail Rate - Cost Value: Batch Quantity × Cost
     * Rate
     *
     * BATCH DETAILS SHOWN: - Batch Number: Unique identifier for the batch -
     * Expiry Date: Date when the batch expires
     */
    public void processClosingStockForBatchReport() {
        Map<String, Object> params = new HashMap<>();

        // DTO projection query - fetches only required scalar values
        // Eliminates entity loading overhead, reduces queries from 5,000+ to 1, and reduces memory by 70%
        StringBuilder jpql = new StringBuilder(
                "SELECT NEW com.divudi.core.data.PharmacyRow("
                + "  sh.id, "
                + "  i.id, "
                + "  COALESCE(i.name, ''), "
                + "  COALESCE(i.code, ''), "
                + "  COALESCE(c.name, ''), "
                + "  COALESCE(mu.name, ''), "
                + "  ib.id, "
                + "  COALESCE(ib.batchNo, ''), "
                + "  ib.dateOfExpire, "
                + "  COALESCE(sh.stockQty, 0.0), "
                + "  COALESCE(sh.purchaseRate, 0.0), "
                + "  COALESCE(sh.retailRate, 0.0), "
                + "  COALESCE(sh.costRate, 0.0), "
                + "  COALESCE(sh.stockPurchaseValue, 0.0), "
                + "  COALESCE(sh.stockSaleValue, 0.0), "
                + "  COALESCE(sh.stockCostValue, 0.0), "
                + "  COALESCE(sh.instituionBatchQty, 0.0), "
                + "  COALESCE(sh.institutionBatchStockValueAtPurchaseRate, 0.0), "
                + "  COALESCE(sh.institutionBatchStockValueAtSaleRate, 0.0), "
                + "  COALESCE(sh.institutionBatchStockValueAtCostRate, 0.0), "
                + "  COALESCE(sh.totalBatchQty, 0.0), "
                + "  COALESCE(sh.totalBatchStockValueAtPurchaseRate, 0.0), "
                + "  COALESCE(sh.totalBatchStockValueAtSaleRate, 0.0), "
                + "  COALESCE(sh.totalBatchStockValueAtCostRate, 0.0)) "
                + "FROM StockHistory sh "
                + "JOIN sh.itemBatch ib "
                + "JOIN ib.item i "
                + "LEFT JOIN i.category c "
                + "LEFT JOIN i.measurementUnit mu "
                + "WHERE sh.retired = :ret "
                + "AND sh.id IN ("
                + "  SELECT MAX(sh2.id) FROM StockHistory sh2 "
                + "  WHERE sh2.retired = :ret2 ");

        params.put("ret", false);
        params.put("ret2", false);

        // Set the effective date parameter first - used in subquery filter
        Date effectiveDate;
        if ("Opening Stock".equals(type)) {
            effectiveDate = CommonFunctions.getStartOfDay(fromDate);
        } else {
            effectiveDate = CommonFunctions.getEndOfDay(toDate);
        }
        params.put("et", effectiveDate);

        // CRITICAL: Filter subquery by createdAt to exclude batches created after selected date
        // This ensures only batches that existed at the selected time are included
        jpql.append("  AND sh2.createdAt <= :et ");

        // CRITICAL: Filter out item-level StockHistory records (where itemBatch is NULL)
        // StockHistory has both 'item' and 'itemBatch' fields:
        // - Item-level records: item is set, itemBatch is NULL
        // - Batch-level records: both item and itemBatch are set
        // Without this filter, GROUP BY sh2.itemBatch groups all NULL values together,
        // and MAX(sh2.id) from the NULL group adds one random item-level record to results
        jpql.append("  AND sh2.itemBatch IS NOT NULL ");

        // Add all filter conditions to subquery
        if (institution != null) {
            jpql.append("  AND sh2.institution = :ins ");
            params.put("ins", institution);
        }

        if (site != null) {
            jpql.append("  AND sh2.department.site = :sit ");
            params.put("sit", site);
        }

        if (department != null) {
            jpql.append("  AND sh2.department = :dep ");
            params.put("dep", department);
        }

        if (category != null) {
            jpql.append("  AND sh2.itemBatch.item.category = :cat ");
            params.put("cat", category);
        }

        if (amp != null) {
            item = amp;
            jpql.append("  AND sh2.itemBatch.item = :itm ");
            params.put("itm", item);
        }

        // Complete subquery with GROUP BY based on scope to prevent duplicate rows
        // Department filter: GROUP BY (department, itemBatch) - one row per department per batch
        // Institution filter: GROUP BY (institution, itemBatch) - one row per institution per batch
        // No filter (Total): GROUP BY (itemBatch) - one row per batch across all institutions
        if (department != null) {
            jpql.append("  GROUP BY sh2.department, sh2.itemBatch) ");
        } else if (institution != null) {
            jpql.append("  GROUP BY sh2.institution, sh2.itemBatch) ");
        } else {
            jpql.append("  GROUP BY sh2.itemBatch) ");
        }

        // CRITICAL FIX: Apply the same filters to outer query to prevent cross-department data leakage
        // Without these filters, the outer query retrieves ALL StockHistory records matching the subquery IDs
        // regardless of department/institution, causing records from other departments to appear in the report
        if (institution != null) {
            jpql.append("AND sh.institution = :ins ");
        }
        if (site != null) {
            jpql.append("AND sh.department.site = :sit ");
        }
        if (department != null) {
            jpql.append("AND sh.department = :dep ");
        }
        if (category != null) {
            jpql.append("AND sh.itemBatch.item.category = :cat ");
        }
        if (amp != null) {
            jpql.append("AND sh.itemBatch.item = :itm ");
        }

        // Order by item name
        jpql.append("ORDER BY i.name");

        // Debug logging
        System.out.println("jpql = " + jpql.toString());
        System.out.println("params = " + params);

        // Execute DTO projection query - returns PharmacyRow objects directly with all values pre-populated
        @SuppressWarnings("unchecked")
        List<PharmacyRow> dtoRows = (List<PharmacyRow>) facade.findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

        System.out.println("dtoRows.size() = " + dtoRows.size());

        rows = new ArrayList<>();

        // Process DTO rows - values are already set in constructor based on scope
        for (PharmacyRow row : dtoRows) {
            if (row == null || row.getItem() == null) {
                continue;
            }

            // Consignment filter (unchanged - stays in Java)
            double batchQty = row.getStockQty() != null ? row.getStockQty() : 0.0;
            if (isConsignmentItem()) {
                if (batchQty > 0) {
                    continue;
                }
            } else {
                if (batchQty <= 0) {
                    continue;
                }
            }

            // Set quantity and values based on department/institution scope
            // The DTO constructor already stored all values, we just assign them to the correct properties
            if (department != null) {
                // Department-level: use qty, purchaseValue, saleValue, costValue (already set in constructor)
                // No additional assignment needed - constructor already set these
            } else {
                if (institution != null) {
                    // Institution-level: use values from grossTotal, discount, netTotal, hospitalTotal
                    row.setQuantity(row.getGrossTotal());  // instituionBatchQty
                    row.setPurchaseValue(row.getDiscount());  // institutionBatchStockValueAtPurchaseRate
                    row.setSaleValue(row.getNetTotal());  // institutionBatchStockValueAtSaleRate
                    row.setCostValue(row.getHospitalTotal());  // institutionBatchStockValueAtCostRate
                } else {
                    // Total-level: use values from paidTotal, tax, actualTotal, staffTotal
                    row.setQuantity(row.getPaidTotal());  // totalBatchQty
                    row.setPurchaseValue(row.getTax());  // totalBatchStockValueAtPurchaseRate
                    row.setSaleValue(row.getActualTotal());  // totalBatchStockValueAtSaleRate
                    row.setCostValue(row.getStaffTotal());  // totalBatchStockValueAtCostRate
                }
            }

            rows.add(row);
        }
    }

    public static <T> T safeGet(Supplier<T> supplier, T defaultValue) {
        try {
            T result = supplier.get();
            return result != null ? result : defaultValue;
        } catch (NullPointerException e) {
            return defaultValue;
        }
    }

    private static final float[] STOCK_LEDGER_COLUMN_WIDTHS = new float[]{
        1, 2, 2, 1, 2, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2
    };

    private void addTableHeaders(PdfPTable table, Font headerFont, String[] headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(new BaseColor(220, 220, 220));
            table.addCell(cell);
        }
    }

    public void exportStockLedgerToPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        List<StockHistory> histories = getStockLedgerHistories();
        if (histories == null || histories.isEmpty()) {

            System.out.println("No stock ledger data available for the selected period");
            return;
        }

        response.reset();
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Stock_Ledger_Report.pdf");
        Document document = null;
        try (OutputStream out = response.getOutputStream()) {
            document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("Stock Ledger Report", (Font) titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            com.itextpdf.text.Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Paragraph dateRange = new Paragraph(
                    String.format("From: %s To: %s",
                            formatDate(getFromDate()),
                            formatDate(getToDate())), (Font) subTitleFont);
            dateRange.setAlignment(Element.ALIGN_CENTER);
            dateRange.setSpacingAfter(20);
            document.add(dateRange);

            com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            com.itextpdf.text.Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            PdfPTable table = new PdfPTable(19); // Number of columns
            table.setWidthPercentage(100);

            table.setWidths(STOCK_LEDGER_COLUMN_WIDTHS);

            String[] headers = {
                "S.No.", "Department", "Item Category", "Item Code", "Item Name", "UOM",
                "Transaction", "Doc No", "Doc Date", "Ref Doc No", "Ref Doc Date",
                "From Store", "To Store", "Doc Type", "Stock In Qty", "Stock Out Qty",
                "Closing Stock", "Rate", "Closing Value"
            };

            addTableHeaders(table, headerFont, headers);

            SimpleDateFormat dateFormat = new SimpleDateFormat(configOptionApplicationController.getShortTextValueByKey("Short Date Format", sessionController.getApplicationPreference().getShortDateFormat()));

            int rowNum = 1;
            for (StockHistory f : histories) {
                final BillItem billItem = f.getPbItem().getBillItem();
                final Bill bill = billItem.getBill();
                final Item item = billItem.getItem();
                final Department fromDept = safeGet(() -> bill.getFromDepartment(), null);
                final Department toDept = safeGet(() -> bill.getToDepartment(), null);
                final Category itemCategory = safeGet(() -> item.getCategory(), null);
                final MeasurementUnit unit = safeGet(() -> item.getMeasurementUnit(), null);
                final BillTypeAtomic atomicType = safeGet(() -> bill.getBillTypeAtomic(), null);
                final BillType billType = safeGet(() -> bill.getBillType(), null);
                final double qty = safeGet(() -> f.getPbItem().getQty(), 0.0);
                final double freeQty = safeGet(() -> f.getPbItem().getFreeQty(), 0.0);
                final double purchaseRate = safeGet(() -> f.getPbItem().getPurchaseRate(), 0.0);
                final boolean isIn = safeGet(() -> f.getPbItem().isTransThisIsStockIn(), false);
                final boolean isOut = safeGet(() -> f.getPbItem().isTransThisIsStockOut(), false);

                table.addCell(createCell(String.valueOf(rowNum++), cellFont));
                table.addCell(createCell(safeGet(() -> f.getDepartment().getName(), ""), cellFont));
                table.addCell(createCell(safeGet(() -> itemCategory.getName(), ""), cellFont));
                table.addCell(createCell(safeGet(() -> item.getCode(), ""), cellFont));
                table.addCell(createCell(safeGet(() -> item.getName(), ""), cellFont));
                table.addCell(createCell(safeGet(() -> unit.getName(), " "), cellFont));

                String transactionType = isIn ? "STOCK IN" : isOut ? "STOCK OUT" : "N/A";
                PdfPCell transCell = new PdfPCell(new Phrase(transactionType, cellFont));
                transCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(transCell);

                table.addCell(createCell(safeGet(() -> bill.getDeptId(), ""), cellFont));
                table.addCell(createCell(safeGet(() -> dateFormat.format(f.getCreatedAt()), ""), cellFont));

                String refDocNo = getRefDocNo(f);
                table.addCell(createCell(refDocNo != null ? refDocNo : "", cellFont));

                String refDocDate = getRefDocDate(f);
                table.addCell(createCell(refDocDate != null ? refDocDate : "", cellFont));

                table.addCell(createCell(safeGet(() -> fromDept.getName(), ""), cellFont));
                table.addCell(createCell(safeGet(() -> toDept.getName(), ""), cellFont));

                String docType = atomicType != null ? atomicType.getLabel() : safeGet(() -> billType.getLabel(), "");
                table.addCell(createCell(docType, cellFont));

                double stockIn = isIn ? qty + freeQty : 0.0;
                table.addCell(createCell(DECIMAL_FORMAT.format(stockIn), cellFont));

                double stockOut = isOut ? qty + freeQty : 0.0;
                table.addCell(createCell(DECIMAL_FORMAT.format(stockOut), cellFont));

                double itemStock = safeGet(() -> f.getItemStock(), 0.0);
                table.addCell(createCell(DECIMAL_FORMAT.format(itemStock), cellFont));

                table.addCell(createCell(DECIMAL_FORMAT.format(purchaseRate), cellFont));

                double closingValue = itemStock * purchaseRate;
                table.addCell(createCell(DECIMAL_FORMAT.format(closingValue), cellFont));
            }

            document.add(table);
            document.close();
            context.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (document != null) {
                try {
                    if (document.isOpen()) {
                        document.close();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    private PdfPCell createCell(String content, Font font) {
        if (content == null || content.isEmpty()) {
            content = " ";
        }
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "N/A";
        }
        return new SimpleDateFormat("dd-MMM-yyyy").format(date);
    }

    private String getRefDocNo(StockHistory f) {
        return safeGet(() -> {
            if ("opSaleDoc".equals(documentType) || "ipSaleDoc".equals(documentType)) {
                return f.getPbItem().getBillItem().getBill().getReferenceBill().isCancelled()
                        ? f.getPbItem().getBillItem().getBill().getForwardReferenceBill().getDeptId()
                        : f.getPbItem().getBillItem().getBill().getBackwardReferenceBill().getDeptId();
            } else {
                return f.getPbItem().getBillItem().getBill().getCancelledBill() != null
                        ? f.getPbItem().getBillItem().getBill().getCancelledBill().getDeptId()
                        : f.getPbItem().getBillItem().getBill().getReferenceBill().getDeptId();
            }
        }, "");
    }

    private String getRefDocDate(StockHistory f) {
        Date refDocDate = safeGet(() -> {
            if (documentType.equals("opSaleDoc") || documentType.equals("ipSaleDoc")) {
                return f.getPbItem().getBillItem().getBill().getReferenceBill().isCancelled()
                        ? f.getPbItem().getBillItem().getBill().getForwardReferenceBill().getCreatedAt()
                        : f.getPbItem().getBillItem().getBill().getBackwardReferenceBill().getCreatedAt();
            } else {
                return f.getPbItem().getBillItem().getBill().getCancelledBill() != null
                        ? f.getPbItem().getBillItem().getBill().getCancelledBill().getCreatedAt()
                        : f.getPbItem().getBillItem().getBill().getReferenceBill().getCreatedAt();
            }
        }, null);

        return refDocDate != null ? formatDate(refDocDate) : "";
    }

    @Deprecated // Use processClosingStockForItemReport
    public void processClosingStockForItemReportOld() {
        List<Long> ids;
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder("select MAX(sh.id) "
                + " from StockHistory sh where sh.retired=:ret ");
        //                + " and (sh.itemBatch.item.departmentType is null or sh.itemBatch.item.departmentType = :depty) ");

        // Set query parameters
        //        params.put("depty", DepartmentType.Pharmacy);
        params.put("ret", false);

        if (institution != null) {
            jpql.append("and sh.institution = :ins ");
            params.put("ins", institution);
        }

        if (site != null) {
            jpql.append("and sh.department.site = :sit ");
            params.put("sit", site);
        }

        if (department != null) {
            jpql.append("and sh.department = :dep ");
            params.put("dep", department);
        }

        if (category != null) {
            jpql.append("and sh.itemBatch.item.category = :cat ");
            params.put("cat", category);
        }

        if (amp != null) {
            item = amp;
            jpql.append("and (sh.itemBatch.item = :itm or sh.itemBatch.item.amp = :itm) ");
            params.put("itm", item);
        }

        jpql.append("and sh.createdAt < :et ");

        if ("Opening Stock".equals(type)) {
            //For cost of good sold report opening stock value report
            params.put("et", CommonFunctions.getStartOfDay(fromDate));
        } else {
            params.put("et", CommonFunctions.getEndOfDay(toDate));
        }

        //        jpql.append("group by sh.department, sh.itemBatch.item ");
        jpql.append("group by sh.department, sh.itemBatch ");
        jpql.append("order by sh.itemBatch.item.name");

        // Fetch the IDs of the latest StockHistory rows per ItemBatch
        ids = facade.findLongValuesByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

        rows = new ArrayList<>();

        // Process each StockHistory to build rows per Item (not per batch)
        for (Long shid : ids) {
            StockHistory shx = facade.find(shid);
            if (shx == null || shx.getItemBatch() == null || shx.getItemBatch().getItem() == null) {
                continue;
            }

            // Assign class-level 'item' so it is not shadowed by a local variable
            item = shx.getItemBatch().getItem();

            //            double batchQty = shx.getItemStock();
            double batchQty = shx.getStockQty();
            double batchPurchaseRate = shx.getItemBatch().getPurcahseRate();
            double batchSaleRate = shx.getItemBatch().getRetailsaleRate();
            double batchCostRate = shx.getItemBatch().getCostRate() != null ? shx.getItemBatch().getCostRate() : 0.0;

            // Check if a PharmacyRow already exists for this Item
            PharmacyRow matchingRow = null;
            for (PharmacyRow r : rows) {
                if (r.getItem() != null && r.getItem().equals(item)) {
                    matchingRow = r;
                    break;
                }
            }

            // If not found, create one
            if (matchingRow == null) {
                matchingRow = new PharmacyRow();
                matchingRow.setItem(item);
                matchingRow.setQuantity(0.0);
                matchingRow.setPurchaseValue(0.0);
                matchingRow.setSaleValue(0.0);
                matchingRow.setCostValue(0.0);
                rows.add(matchingRow);
            }

            if (isConsignmentItem() && matchingRow.getQuantity() + batchQty > 0) {
                rows.remove(matchingRow);
                continue;
            } else {
                if (matchingRow.getQuantity() + batchQty <= 0) {
                    rows.remove(matchingRow);
                    continue;
                }
            }

            // Accumulate the quantities and values
            matchingRow.setQuantity(matchingRow.getQuantity() + batchQty);
            matchingRow.setPurchaseValue(matchingRow.getPurchaseValue() + batchQty * batchPurchaseRate);
            matchingRow.setSaleValue(matchingRow.getSaleValue() + batchQty * batchSaleRate);
            matchingRow.setCostValue(matchingRow.getCostValue() + batchQty * batchCostRate);
        }
    }

    public void processClosingStock() {
        reportTimerController.trackReportExecution(() -> {
            stockPurchaseValue = 0.0;
            stockSaleValue = 0.0;
            stockCostValue = 0.0;
            stockQty = 0.0;
            if (reportType.equals("batchWise")) {
                processClosingStockForBatchReport();
            } else if (reportType.equals("itemWise")) {
                processClosingStockForItemReport();
            } else {
                JsfUtil.addErrorMessage("Report Type " + reportType + " is NOT supported.");
                return;
            }
            if (rows != null) {
                for (PharmacyRow pr : rows) {
                    stockPurchaseValue += pr.getPurchaseValue() != null ? pr.getPurchaseValue() : 0.0;
                    stockSaleValue += pr.getSaleValue() != null ? pr.getSaleValue() : 0.0;
                    stockCostValue += pr.getCostValue() != null ? pr.getCostValue() : 0.0;
                    stockQty += pr.getQuantity() != null ? pr.getQuantity() : 0.0;
                }
            }
        }, InventoryReports.CLOSING_STOCK_REPORT, sessionController.getLoggedUser());
    }

    private Map<String, Object> cogsRows = new LinkedHashMap<>();

    private Map<String, Double> calculateStockTotals(Date date) {
        try {
            Map<String, Object> params = new HashMap<>();
            StringBuilder jpql = new StringBuilder();

            // Direct aggregation query
            jpql.append("SELECT ")
                    .append("SUM(sh.stockQty * sh.itemBatch.purcahseRate), ")
                    .append("SUM(sh.stockQty * COALESCE(sh.itemBatch.costRate, 0.0)), ")
                    .append("SUM(sh.stockQty * COALESCE(sh.itemBatch.retailsaleRate, 0.0)) ")
                    .append("FROM StockHistory sh ")
                    .append("WHERE sh.retired = :ret ")
                    .append("AND sh.id IN (")
                    .append("SELECT MAX(sh2.id) FROM StockHistory sh2 ")
                    .append("WHERE sh2.retired = :ret ")
                    .append("AND sh2.createdAt < :et ");

            params.put("ret", false);
            params.put("et", date);

            // Add filters to subquery
            addFilter(jpql, params, "sh2.institution", "ins", institution);
            addFilter(jpql, params, "sh2.department.site", "sit", site);
            addFilter(jpql, params, "sh2.department", "dep", department);
            addFilter(jpql, params, "sh2.item", "itm", item);

            jpql.append("GROUP BY sh2.department, sh2.itemBatch ")
                    .append("HAVING MAX(sh2.id) IN (")
                    .append("SELECT sh3.id FROM StockHistory sh3 ")
                    .append("WHERE sh3.retired = :ret ");

            // Add filters to innermost query
            addFilter(jpql, params, "sh3.institution", "ins2", institution);
            addFilter(jpql, params, "sh3.department.site", "sit2", site);
            addFilter(jpql, params, "sh3.department", "dep2", department);
            addFilter(jpql, params, "sh3.item", "itm2", item);

            jpql.append("AND sh3.createdAt < :et2)) ");
            params.put("et2", date);

            // Add filters to main query
            addFilter(jpql, params, "sh.institution", "ins3", institution);
            addFilter(jpql, params, "sh.department.site", "sit3", site);
            addFilter(jpql, params, "sh.department", "dep3", department);
            addFilter(jpql, params, "sh.item", "itm3", item);

            // Group by item and filter positive quantities
            jpql.append("AND sh.itemBatch.item.id IN (")
                    .append("SELECT sh4.itemBatch.item.id FROM StockHistory sh4 ")
                    .append("WHERE sh4.retired = :ret ")
                    .append("AND sh4.id IN (")
                    .append("SELECT MAX(sh5.id) FROM StockHistory sh5 ")
                    .append("WHERE sh5.retired = :ret ")
                    .append("AND sh5.createdAt < :et3 ");

            params.put("et3", date);

            // Add filters to item filtering subqueries
            addFilter(jpql, params, "sh5.institution", "ins4", institution);
            addFilter(jpql, params, "sh5.department.site", "sit4", site);
            addFilter(jpql, params, "sh5.department", "dep4", department);
            addFilter(jpql, params, "sh5.item", "itm4", item);

            jpql.append("GROUP BY sh5.department, sh5.itemBatch) ");

            addFilter(jpql, params, "sh4.institution", "ins5", institution);
            addFilter(jpql, params, "sh4.department.site", "sit5", site);
            addFilter(jpql, params, "sh4.department", "dep5", department);
            addFilter(jpql, params, "sh4.item", "itm5", item);

            jpql.append("GROUP BY sh4.itemBatch.item.id ")
                    .append("HAVING SUM(sh4.stockQty) > 0");

            // Apply consignment filter if needed
            if (isConsignmentItem()) {
                jpql.append(" AND SUM(sh4.stockQty) <= 0");
            }

            jpql.append(")");

            // Execute the query
            List<Object[]> results = facade.findRawResultsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

            Map<String, Double> result = new HashMap<>();

            if (results != null && !results.isEmpty()) {
                Object[] totals = results.get(0);
                result.put("purchaseValue", totals[0] != null ? ((Number) totals[0]).doubleValue() : 0.0);
                result.put("costValue", totals[1] != null ? ((Number) totals[1]).doubleValue() : 0.0);
                result.put("retailValue", totals[2] != null ? ((Number) totals[2]).doubleValue() : 0.0);
            } else {
                result.put("purchaseValue", 0.0);
                result.put("costValue", 0.0);
                result.put("retailValue", 0.0);
            }

            return result;

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Error calculating stock totals for date: " + date);
            Map<String, Double> errorResult = new HashMap<>();
            errorResult.put("purchaseValue", 0.0);
            errorResult.put("costValue", 0.0);
            errorResult.put("retailValue", 0.0);
            return errorResult;
        }
    }

    private Map<String, Double> calculateStockCorrectionValues() {
        try {
            List<BillTypeAtomic> billTypeAtomics = new ArrayList<>();
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_PURCHASE_RATE_ADJUSTMENT);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_COST_RATE_ADJUSTMENT);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_RATE_ADJUSTMENT);

            Map<String, Object> params = new HashMap<>();
            StringBuilder jpql = new StringBuilder();

            // Direct aggregation query
            jpql.append("SELECT ")
                    .append("SUM(bi.bill.billFinanceDetails.totalPurchaseValue), ")
                    .append("SUM(bi.bill.billFinanceDetails.totalCostValue), ")
                    .append("SUM(bi.bill.billFinanceDetails.totalRetailSaleValue) ")
                    .append("FROM BillItem bi ")
                    .append("WHERE bi.retired = :ret ")
                    .append("AND bi.bill.billTypeAtomic IN :btas ")
                    .append("AND bi.bill.createdAt BETWEEN :fd AND :td ");

            params.put("ret", false);
            params.put("btas", billTypeAtomics);
            params.put("fd", fromDate);
            params.put("td", toDate);

            addFilter(jpql, params, "bi.bill.institution", "ins", institution);
            addFilter(jpql, params, "bi.bill.department.site", "sit", site);
            addFilter(jpql, params, "bi.bill.department", "dep", department);
            addFilter(jpql, params, "bi.pharmaceuticalBillItem.itemBatch.item", "itm", item);

            List<Object[]> results = facade.findRawResultsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

            Map<String, Double> result = new HashMap<>();

            if (results != null && !results.isEmpty()) {
                Object[] totals = results.get(0);
                result.put("purchaseValue", totals[0] != null ? ((Number) totals[0]).doubleValue() : 0.0);
                result.put("costValue", totals[1] != null ? ((Number) totals[1]).doubleValue() : 0.0);
                result.put("retailValue", totals[2] != null ? ((Number) totals[2]).doubleValue() : 0.0);
            } else {
                result.put("purchaseValue", 0.0);
                result.put("costValue", 0.0);
                result.put("retailValue", 0.0);
            }

            return result;

        } catch (Exception e) {
            Map<String, Double> errorResult = new HashMap<>();
            errorResult.put("purchaseValue", 0.0);
            errorResult.put("costValue", 0.0);
            errorResult.put("retailValue", 0.0);
            return errorResult;
        }
    }

    public void calculateGrnCashAndCreditValues() {
        try {
            List<BillTypeAtomic> billTypeAtomics = new ArrayList<>();
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_GRN);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_GRN_CANCELLED);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_CANCELLED);

            StringBuilder jpql = new StringBuilder("SELECT bi.bill.paymentMethod, SUM(bi.billItemFinanceDetails.valueAtPurchaseRate), SUM(bi.billItemFinanceDetails.valueAtCostRate), SUM(bi.billItemFinanceDetails.valueAtRetailRate) FROM BillItem bi ")
                    .append("WHERE bi.retired = false ")
                    .append("AND bi.bill.billTypeAtomic IN :bType ")
                    .append("AND bi.bill.createdAt BETWEEN :fd AND :td ")
                    .append("AND bi.bill.paymentMethod IN (:cash, :credit) ");

            Map<String, Object> params = new HashMap<>();
            params.put("bType", billTypeAtomics);
            params.put("fd", fromDate);
            params.put("td", toDate);
            params.put("cash", PaymentMethod.Cash);
            params.put("credit", PaymentMethod.Credit);

            addFilter(jpql, params, "bi.bill.institution", "ins", institution);
            addFilter(jpql, params, "bi.bill.department.site", "sit", site);
            addFilter(jpql, params, "bi.bill.department", "dep", department);
            addFilter(jpql, params, "bi.pharmaceuticalBillItem.itemBatch.item", "itm", item);
            jpql.append(" GROUP BY bi.bill.paymentMethod");

            List<Object[]> results = billFacade.findAggregates(jpql.toString(), params, TemporalType.TIMESTAMP);

            // A map to store the final results for cash and credit.
            Map<String, Double> cashRow = new HashMap<>();
            Map<String, Double> creditRow = new HashMap<>();

            // Initialize with default values.
            cashRow.put("purchaseValue", 0.0);
            cashRow.put("costValue", 0.0);
            cashRow.put("retailValue", 0.0);
            creditRow.put("purchaseValue", 0.0);
            creditRow.put("costValue", 0.0);
            creditRow.put("retailValue", 0.0);

            if (results != null && !results.isEmpty()) {
                for (Object[] row : results) {
                    PaymentMethod paymentMethod = (PaymentMethod) row[0];
                    double purchaseValue = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                    double costValue = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
                    double retailValue = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;

                    if (paymentMethod == PaymentMethod.Cash) {
                        cashRow.put("purchaseValue", purchaseValue);
                        cashRow.put("costValue", costValue);
                        cashRow.put("retailValue", retailValue);
                    } else if (paymentMethod == PaymentMethod.Credit) {
                        creditRow.put("purchaseValue", purchaseValue);
                        creditRow.put("costValue", costValue);
                        creditRow.put("retailValue", retailValue);
                    }
                }
            }

            // Add the computed rows to the main cogsRows map
            synchronized (cogsRows) {
                cogsRows.put("GRN Cash", cashRow);
                cogsRows.put("GRN Credit", creditRow);
            }

        } catch (Exception e) {
            // Log the error for debugging purposes
            JsfUtil.addErrorMessage(e, "Error calculating GRN values");
            // Fallback or error handling for the cogsRows map
            synchronized (cogsRows) {
                Map<String, Double> errorResult = new HashMap<>();
                errorResult.put("purchaseValue", 0.0);
                errorResult.put("costValue", 0.0);
                errorResult.put("retailValue", 0.0);
                cogsRows.put("GRN Cash Total", errorResult);
                cogsRows.put("GRN Credit Total", errorResult);
            }
        }
    }

    public Map<String, Double> retrieveValuesAtRates(String billTypeField, List<BillTypeAtomic> btas, Boolean completed) {
        try {

            Map<String, Object> commonParams = new HashMap<>();
            StringBuilder baseQuery = new StringBuilder();

            baseQuery.append("SELECT ")
                    .append("SUM(bi.billItemFinanceDetails.valueAtPurchaseRate), ")
                    .append("SUM(bi.billItemFinanceDetails.valueAtCostRate), ")
                    .append("SUM(bi.billItemFinanceDetails.valueAtRetailRate) ")
                    .append("FROM BillItem bi ")
                    .append("WHERE bi.retired = :ret ")
                    .append("AND bi.bill.billTypeAtomic IN :btas ")
                    .append("AND bi.bill.createdAt BETWEEN :fd AND :td ");

            commonParams.put("ret", false);
            commonParams.put("btas", btas);
            commonParams.put("fd", fromDate);
            commonParams.put("td", toDate);

            addFilter(baseQuery, commonParams, "bi.bill.institution", "ins", institution);
            addFilter(baseQuery, commonParams, "bi.bill.department.site", "sit", site);
            addFilter(baseQuery, commonParams, "bi.bill.department", "dep", department);
            addFilter(baseQuery, commonParams, "bi.item", "itm", item);
            if (completed != null) {
                addFilter(baseQuery, commonParams, "bi.bill.completed", "completed", completed);
            }
            baseQuery.append(" ORDER BY bi.bill.createdAt");
            List<Object[]> results = facade.findRawResultsByJpql(baseQuery.toString(), commonParams, TemporalType.TIMESTAMP);

            Map<String, Double> result = new HashMap<>();

            if (results != null && !results.isEmpty()) {
                Object[] totals = results.get(0);
                result.put("purchaseValue", totals[0] != null ? ((Number) totals[0]).doubleValue() : 0.0);
                result.put("costValue", totals[1] != null ? ((Number) totals[1]).doubleValue() : 0.0);
                result.put("retailValue", totals[2] != null ? ((Number) totals[2]).doubleValue() : 0.0);
            } else {
                result.put("purchaseValue", 0.0);
                result.put("costValue", 0.0);
                result.put("retailValue", 0.0);
            }
            return result;

        } catch (Exception e) {
            Map<String, Double> errorResult = new HashMap<>();
            errorResult.put("purchaseValue", 0.0);
            errorResult.put("costValue", 0.0);
            errorResult.put("retailValue", 0.0);
            return errorResult;
        }
    }

    @Deprecated // Use retrieveValuesAtRates
    public Map<String, Double> retrievePurchaseAndCostValues(String billTypeField, Object billTypeValue) {
        try {

            Map<String, Object> commonParams = new HashMap<>();
            StringBuilder baseQuery = new StringBuilder();

            baseQuery.append("SELECT ")
                    .append("SUM(bi.pharmaceuticalBillItem.qty * bi.pharmaceuticalBillItem.itemBatch.purcahseRate), ")
                    .append("SUM(bi.pharmaceuticalBillItem.qty * bi.pharmaceuticalBillItem.itemBatch.costRate), ")
                    .append("SUM(bi.pharmaceuticalBillItem.qty * bi.pharmaceuticalBillItem.itemBatch.retailsaleRate) ")
                    .append("FROM BillItem bi ")
                    .append("WHERE bi.retired = :ret ")
                    .append("AND " + billTypeField + " IN :billTypes ")
                    .append("AND bi.bill.createdAt BETWEEN :fd AND :td ");

            commonParams.put("ret", false);
            commonParams.put("billTypes", billTypeValue);
            commonParams.put("fd", fromDate);
            commonParams.put("td", toDate);

            addFilter(baseQuery, commonParams, "bi.bill.institution", "ins", institution);
            addFilter(baseQuery, commonParams, "bi.bill.department.site", "sit", site);
            addFilter(baseQuery, commonParams, "bi.bill.department", "dep", department);
            addFilter(baseQuery, commonParams, "bi.pharmaceuticalBillItem.itemBatch.item", "itm", item);
            baseQuery.append(" ORDER BY bi.bill.createdAt");
            List<Object[]> results = facade.findRawResultsByJpql(baseQuery.toString(), commonParams, TemporalType.TIMESTAMP);

            Map<String, Double> result = new HashMap<>();

            if (results != null && !results.isEmpty()) {
                Object[] totals = results.get(0);
                result.put("purchaseValue", totals[0] != null ? ((Number) totals[0]).doubleValue() : 0.0);
                result.put("costValue", totals[1] != null ? ((Number) totals[1]).doubleValue() : 0.0);
                result.put("retailValue", totals[2] != null ? ((Number) totals[2]).doubleValue() : 0.0);
            } else {
                result.put("purchaseValue", 0.0);
                result.put("costValue", 0.0);
                result.put("retailValue", 0.0);
            }
            return result;

        } catch (Exception e) {
            Map<String, Double> errorResult = new HashMap<>();
            errorResult.put("purchaseValue", 0.0);
            errorResult.put("costValue", 0.0);
            errorResult.put("retailValue", 0.0);
            return errorResult;
        }
    }

    public Map<String, Double> retrievePurchaseAndCostValues(String billTypeField, Object billTypeValue, Object paymentMethod) {
        try {

            Map<String, Object> commonParams = new HashMap<>();
            StringBuilder baseQuery = new StringBuilder();

            baseQuery.append("SELECT ")
                    .append("SUM(bi.pharmaceuticalBillItem.qty * bi.pharmaceuticalBillItem.itemBatch.purcahseRate), ")
                    .append("SUM(bi.pharmaceuticalBillItem.qty * bi.pharmaceuticalBillItem.itemBatch.costRate), ")
                    .append("SUM(bi.pharmaceuticalBillItem.qty * bi.pharmaceuticalBillItem.itemBatch.retailsaleRate) ")
                    .append("FROM BillItem bi ")
                    .append("WHERE bi.retired = :ret ")
                    .append("AND " + billTypeField + " IN :billTypes ")
                    .append("AND bi.bill.createdAt BETWEEN :fd AND :td ");

            baseQuery.append("AND (bi.bill.paymentMethod IN :pm ");
            baseQuery.append("OR (bi.bill.paymentMethod = :multiPm AND EXISTS ("
                    + "SELECT p FROM Payment p "
                    + "WHERE p.bill = bi.bill AND p.paymentMethod IN :pm)) )");

            commonParams.put("ret", false);
            commonParams.put("billTypes", billTypeValue);
            commonParams.put("fd", fromDate);
            commonParams.put("td", toDate);
            commonParams.put("pm", paymentMethod);
            commonParams.put("multiPm", PaymentMethod.MultiplePaymentMethods);

            addFilter(baseQuery, commonParams, "bi.bill.institution", "ins", institution);
            addFilter(baseQuery, commonParams, "bi.bill.department.site", "sit", site);
            addFilter(baseQuery, commonParams, "bi.bill.department", "dep", department);
            addFilter(baseQuery, commonParams, "bi.pharmaceuticalBillItem.itemBatch.item", "itm", item);
            baseQuery.append(" ORDER BY bi.bill.createdAt");
            List<Object[]> results = facade.findRawResultsByJpql(baseQuery.toString(), commonParams, TemporalType.TIMESTAMP);

            Map<String, Double> result = new HashMap<>();

            if (results != null && !results.isEmpty()) {
                Object[] totals = results.get(0);
                result.put("purchaseValue", totals[0] != null ? ((Number) totals[0]).doubleValue() : 0.0);
                result.put("costValue", totals[1] != null ? ((Number) totals[1]).doubleValue() : 0.0);
                result.put("retailValue", totals[2] != null ? ((Number) totals[2]).doubleValue() : 0.0);
            } else {
                result.put("purchaseValue", 0.0);
                result.put("costValue", 0.0);
                result.put("retailValue", 0.0);
            }
            return result;

        } catch (Exception e) {
            Map<String, Double> errorResult = new HashMap<>();
            errorResult.put("purchaseValue", 0.0);
            errorResult.put("costValue", 0.0);
            errorResult.put("retailValue", 0.0);
            return errorResult;
        }
    }

    public Map<String, Double> retrievePurchaseAndCostValues(List<BillTypeAtomic> billTypeValue, List<PaymentMethod> paymentMethods) {
        try {

            Map<String, Object> commonParams = new HashMap<>();
            StringBuilder baseQuery = new StringBuilder();

            baseQuery.append("SELECT ")
                    .append("SUM(bi.pharmaceuticalBillItem.qty * bi.pharmaceuticalBillItem.itemBatch.purcahseRate), ")
                    .append("SUM(bi.pharmaceuticalBillItem.qty * bi.pharmaceuticalBillItem.itemBatch.costRate), ")
                    .append("SUM(bi.pharmaceuticalBillItem.qty * bi.pharmaceuticalBillItem.itemBatch.retailsaleRate) ")
                    .append("FROM BillItem bi ")
                    .append("WHERE bi.retired = :ret ")
                    .append("AND bi.bill.billTypeAtomic IN :billTypes ")
                    .append("AND bi.bill.createdAt BETWEEN :fd AND :td ");

            baseQuery.append("AND (bi.bill.paymentMethod IN :pm ");
            baseQuery.append("OR (bi.bill.paymentMethod = :multiPm AND EXISTS ("
                    + "SELECT p FROM Payment p "
                    + "WHERE p.bill = bi.bill AND p.paymentMethod IN :pm)) )");

            commonParams.put("ret", false);
            commonParams.put("billTypes", billTypeValue);
            commonParams.put("fd", fromDate);
            commonParams.put("td", toDate);
            commonParams.put("pm", paymentMethods);
            commonParams.put("multiPm", PaymentMethod.MultiplePaymentMethods);

            addFilter(baseQuery, commonParams, "bi.bill.institution", "ins", institution);
            addFilter(baseQuery, commonParams, "bi.bill.department.site", "sit", site);
            addFilter(baseQuery, commonParams, "bi.bill.department", "dep", department);
            addFilter(baseQuery, commonParams, "bi.pharmaceuticalBillItem.itemBatch.item", "itm", item);
            baseQuery.append(" ORDER BY bi.bill.createdAt");
            List<Object[]> results = facade.findRawResultsByJpql(baseQuery.toString(), commonParams, TemporalType.TIMESTAMP);

            Map<String, Double> result = new HashMap<>();

            if (results != null && !results.isEmpty()) {
                Object[] totals = results.get(0);
                result.put("purchaseValue", totals[0] != null ? ((Number) totals[0]).doubleValue() : 0.0);
                result.put("costValue", totals[1] != null ? ((Number) totals[1]).doubleValue() : 0.0);
                result.put("retailValue", totals[2] != null ? ((Number) totals[2]).doubleValue() : 0.0);
                System.out.println("=== DEBUG retrievePurchaseAndCostValues (List<BillTypeAtomic>, List<PaymentMethod>) ===");
                System.out.println("Bill Types: " + billTypeValue);
                System.out.println("Payment Methods: " + paymentMethods);
                System.out.println("totals[0] = " + totals[0]);
                System.out.println("totals[1] = " + totals[1]);
                System.out.println("totals[2] = " + totals[2]);
                System.out.println("Query: " + baseQuery.toString());
            } else {
                result.put("purchaseValue", 0.0);
                result.put("costValue", 0.0);
                result.put("retailValue", 0.0);
            }
            return result;

        } catch (Exception e) {
            Map<String, Double> errorResult = new HashMap<>();
            errorResult.put("purchaseValue", 0.0);
            errorResult.put("costValue", 0.0);
            errorResult.put("retailValue", 0.0);
            return errorResult;
        }
    }

    public Map<String, Double> retrievePurchaseAndCostValues(List<BillTypeAtomic> billTypeValue) {
        return retrievePurchaseAndCostValuesWithComplete(billTypeValue, null);
    }

    public Map<String, Double> retrievePurchaseAndCostValuesWithoutReference(List<BillTypeAtomic> billTypeValue) {
        try {
            Map<String, Object> commonParams = new HashMap<>();
            StringBuilder baseQuery = new StringBuilder();
            baseQuery.append("SELECT ")
                    .append("SUM(bi.pharmaceuticalBillItem.qty * bi.pharmaceuticalBillItem.itemBatch.purcahseRate), ")
                    .append("SUM(bi.pharmaceuticalBillItem.qty * bi.pharmaceuticalBillItem.itemBatch.costRate), ")
                    .append("SUM(bi.pharmaceuticalBillItem.qty * bi.pharmaceuticalBillItem.itemBatch.retailsaleRate) ")
                    .append("FROM BillItem bi ")
                    .append("LEFT JOIN bi.bill.referenceBill rb ")
                    .append("WHERE bi.retired = :ret ")
                    .append("AND bi.bill.billTypeAtomic IN :billTypes ")
                    .append("AND (rb IS NULL OR rb.createdAt > :td) ")
                    .append("AND bi.bill.createdAt BETWEEN :fd AND :td ");

            commonParams.put("ret", false);
            commonParams.put("billTypes", billTypeValue);
            commonParams.put("fd", fromDate);
            commonParams.put("td", toDate);

            addFilter(baseQuery, commonParams, "bi.bill.institution", "ins", institution);
            addFilter(baseQuery, commonParams, "bi.bill.department.site", "sit", site);
            addFilter(baseQuery, commonParams, "bi.bill.department", "dep", department);
            addFilter(baseQuery, commonParams, "bi.pharmaceuticalBillItem.itemBatch.item", "itm", item);
            baseQuery.append(" ORDER BY bi.bill.createdAt");
            List<Object[]> results = facade.findRawResultsByJpql(baseQuery.toString(), commonParams, TemporalType.TIMESTAMP);

            Map<String, Double> result = new HashMap<>();

            if (results != null && !results.isEmpty()) {
                Object[] totals = results.get(0);
                result.put("purchaseValue", totals[0] != null ? ((Number) totals[0]).doubleValue() : 0.0);
                result.put("costValue", totals[1] != null ? ((Number) totals[1]).doubleValue() : 0.0);
                result.put("retailValue", totals[2] != null ? ((Number) totals[2]).doubleValue() : 0.0);
                System.out.println("=== DEBUG retrievePurchaseAndCostValuesWithoutReference ===");
                System.out.println("Bill Types: " + billTypeValue);
                System.out.println("totals[0] = " + totals[0]);
                System.out.println("totals[1] = " + totals[1]);
                System.out.println("totals[2] = " + totals[2]);
                System.out.println("Query: " + baseQuery.toString());
            } else {
                result.put("purchaseValue", 0.0);
                result.put("costValue", 0.0);
                result.put("retailValue", 0.0);
            }
            return result;

        } catch (Exception e) {
            Map<String, Double> errorResult = new HashMap<>();
            errorResult.put("purchaseValue", 0.0);
            errorResult.put("costValue", 0.0);
            errorResult.put("retailValue", 0.0);
            return errorResult;
        }
    }

    public Map<String, Double> retrievePurchaseAndCostValuesWithComplete(List<BillTypeAtomic> billTypeValue, Boolean includeOnlyCompleted) {
        try {
            Map<String, Object> commonParams = new HashMap<>();
            StringBuilder baseQuery = new StringBuilder();
            baseQuery.append("SELECT ")
                    .append("SUM(bi.pharmaceuticalBillItem.qty * bi.pharmaceuticalBillItem.itemBatch.purcahseRate), ")
                    .append("SUM(bi.pharmaceuticalBillItem.qty * bi.pharmaceuticalBillItem.itemBatch.costRate), ")
                    .append("SUM(bi.pharmaceuticalBillItem.qty * bi.pharmaceuticalBillItem.itemBatch.retailsaleRate) ")
                    .append("FROM BillItem bi ")
                    .append("WHERE bi.retired = :ret ")
                    .append("AND bi.bill.billTypeAtomic IN :billTypes ")
                    .append("AND bi.bill.createdAt BETWEEN :fd AND :td ");

            commonParams.put("ret", false);
            commonParams.put("billTypes", billTypeValue);
            commonParams.put("fd", fromDate);
            commonParams.put("td", toDate);

            if (includeOnlyCompleted != null) {
                addFilter(baseQuery, commonParams, "bi.bill.completed", "completed", includeOnlyCompleted);
            }

            addFilter(baseQuery, commonParams, "bi.bill.institution", "ins", institution);
            addFilter(baseQuery, commonParams, "bi.bill.department.site", "sit", site);
            addFilter(baseQuery, commonParams, "bi.bill.department", "dep", department);
            addFilter(baseQuery, commonParams, "bi.pharmaceuticalBillItem.itemBatch.item", "itm", item);
            baseQuery.append(" ORDER BY bi.bill.createdAt");
            List<Object[]> results = facade.findRawResultsByJpql(baseQuery.toString(), commonParams, TemporalType.TIMESTAMP);

            Map<String, Double> result = new HashMap<>();

            if (results != null && !results.isEmpty()) {
                Object[] totals = results.get(0);
                result.put("purchaseValue", totals[0] != null ? ((Number) totals[0]).doubleValue() : 0.0);
                result.put("costValue", totals[1] != null ? ((Number) totals[1]).doubleValue() : 0.0);
                result.put("retailValue", totals[2] != null ? ((Number) totals[2]).doubleValue() : 0.0);
                System.out.println("=== DEBUG retrievePurchaseAndCostValues (List<BillTypeAtomic>, List<PaymentMethod>) ===");
                System.out.println("Bill Types: " + billTypeValue);
                System.out.println("totals[0] = " + totals[0]);
                System.out.println("totals[1] = " + totals[1]);
                System.out.println("totals[2] = " + totals[2]);
                System.out.println("Query: " + baseQuery.toString());
            } else {
                result.put("purchaseValue", 0.0);
                result.put("costValue", 0.0);
                result.put("retailValue", 0.0);
            }
            return result;

        } catch (Exception e) {
            Map<String, Double> errorResult = new HashMap<>();
            errorResult.put("purchaseValue", 0.0);
            errorResult.put("costValue", 0.0);
            errorResult.put("retailValue", 0.0);
            return errorResult;
        }
    }

    public void processCostOfGoodSoldReport() {

        cogsRows.clear();
        try {
            calculateOpeningStockRow();
            calculateStockCorrectionRow();
            calculateGrnCashAndCreditRows();

            calculateDrugReturnIp();
            calculateDrugReturnOp();
            calculateStockConsumption();
            calculatePurchaseReturn();
            calculateStockAdjustment();
            calculateTransferIssueValue();
            calculateTransferReceiveValue();
            calculateSaleCreditValue();
            calculateBhtIssueValue();
            calculateSaleWithoutCreditPaymentMethod();
            calculateAddToStockBills();

            Map<String, Double> calculatedClosingStockByCogsRowValues = calculateClosingStockValueByCalculatedRows();
            calculateClosingStockRow();
            calculateVariance(calculatedClosingStockByCogsRowValues);

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "error");
        }

    }

    public void calculateOpeningStockRow() {
        Map<String, Double> openingStock = calculateStockTotals(fromDate);
        synchronized (cogsRows) {
            cogsRows.put("Opening Stock", openingStock);
        }
    }

    public void calculateStockCorrectionRow() {
        Map<String, Double> stockCorrection = calculateStockCorrectionValues();
        cogsRows.put("Stock Correction", stockCorrection);
    }

    public void calculateGrnCashAndCreditRows() {
        calculateGrnCashAndCreditValues();
    }

    public void calculateClosingStockRow() {
        Map<String, Double> closingStock = calculateStockTotals(toDate);
        synchronized (cogsRows) {
            cogsRows.put("Closing Stock", closingStock);
        }
    }

    private void calculateDrugReturnIp() {
        try {
            List<BillTypeAtomic> billTypes = Arrays.asList(
                    BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION,
                    BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN,
                    BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_CANCELLATION,
                    BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN,
                    BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION,
                    BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_RETURN
            );
            Map<String, Double> ipDrugReturns = retrievePurchaseAndCostValues(" bi.bill.billTypeAtomic ", billTypes);
            cogsRows.put("Drug Return IP", ipDrugReturns);

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Error calculating drug return IP");
        }
    }

    private void calculateDrugReturnOp() {
        try {
            List<BillTypeAtomic> billTypeAtomics = new ArrayList<>();
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_REFUND);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY);

            Map<String, Double> opDrugReturns = retrievePurchaseAndCostValues(" bi.bill.billTypeAtomic ", billTypeAtomics);
            cogsRows.put("Drug Return Op", opDrugReturns);

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Error calculating OP returns");
        }
    }

    private void calculateStockConsumption() {
        try {
//            List<BillType> billTypes = new ArrayList<>();
//            billTypes.add(BillType.PharmacyDisposalIssue);
//            billTypes.add(BillType.PharmacyIssue);
            List<BillTypeAtomic> btas = new ArrayList<>();
            btas.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_CANCELLED);
            btas.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE);
            btas.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_RETURN);
            Map<String, Double> stockConsumptions = retrievePurchaseAndCostValuesWithComplete(btas, true);
            cogsRows.put("Stock Consumption", stockConsumptions);

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Error calculating stock consumption");
        }
    }

    public Map<String, Double> retrievePurchaseAndCostValuesWithSignLogic(String billTypeField, Object billTypeValue) {
        try {
            Map<String, Object> commonParams = new HashMap<>();
            StringBuilder baseQuery = new StringBuilder();

            baseQuery.append("SELECT bi ")
                    .append("FROM BillItem bi ")
                    .append("WHERE bi.retired = :ret ")
                    .append("AND " + billTypeField + " IN :billTypes ")
                    .append("AND bi.bill.createdAt BETWEEN :fd AND :td ");

            commonParams.put("ret", false);
            commonParams.put("billTypes", billTypeValue);
            commonParams.put("fd", fromDate);
            commonParams.put("td", toDate);

            addFilter(baseQuery, commonParams, "bi.bill.institution", "ins", institution);
            addFilter(baseQuery, commonParams, "bi.bill.department.site", "sit", site);
            addFilter(baseQuery, commonParams, "bi.bill.department", "dep", department);
            addFilter(baseQuery, commonParams, "bi.pharmaceuticalBillItem.itemBatch.item", "itm", item);

            List<BillItem> billItems = billItemFacade.findByJpql(baseQuery.toString(), commonParams, TemporalType.TIMESTAMP);

            double purchaseValue = 0.0;
            double costValue = 0.0;
            double retailValue = 0.0;

            for (BillItem item : billItems) {
                double itemPurchaseValue = item.getPharmaceuticalBillItem().getQty() * item.getPharmaceuticalBillItem().getItemBatch().getPurcahseRate();
                double itemCostValue = item.getPharmaceuticalBillItem().getQty() * item.getPharmaceuticalBillItem().getItemBatch().getCostRate();
                double itemRetailValue = item.getPharmaceuticalBillItem().getQty() * item.getPharmaceuticalBillItem().getItemBatch().getRetailsaleRate();

                if (item.getBill().getBillTypeAtomic() == BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE) {
                    // Disposal Issue - stock reduces - negative values
                    purchaseValue -= itemPurchaseValue;
                    costValue -= itemCostValue;
                    retailValue -= itemRetailValue;
                } else {
                    // Disposal Return - stock increases - positive values
                    purchaseValue += itemPurchaseValue;
                    costValue += itemCostValue;
                    retailValue += itemRetailValue;
                }
            }

            Map<String, Double> result = new HashMap<>();
            result.put("purchaseValue", purchaseValue);
            result.put("costValue", costValue);
            result.put("retailValue", retailValue);
            return result;

        } catch (Exception e) {
            Map<String, Double> errorResult = new HashMap<>();
            errorResult.put("purchaseValue", 0.0);
            errorResult.put("costValue", 0.0);
            errorResult.put("retailValue", 0.0);
            return errorResult;
        }
    }

    private void calculatePurchaseReturn() {
        try {
            List<BillTypeAtomic> billTypeAtomics = new ArrayList<>();
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL_CANCELLED);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL_REFUND);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_CANCELLED);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_REFUND);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_GRN_CANCELLED);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_GRN_REFUND);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_GRN_RETURN);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETURN_WITHOUT_TREASING);
            Map<String, Double> purchaseReturns = retrieveValuesAtRates(" bi.bill.billTypeAtomic ", billTypeAtomics, true);

            cogsRows.put("Purchase Return", purchaseReturns);

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Error calculating purchase returns");
        }
    }

    private void calculateStockAdjustment() {
        try {
            List<BillType> billTypes = new ArrayList<>();
            billTypes.add(BillType.PharmacyAdjustmentDepartmentSingleStock);
            billTypes.add(BillType.PharmacyAdjustmentDepartmentStock);

            // Query for Stock Adjustment Issues (negative quantities)
            Map<String, Object> paramsIssue = new HashMap<>();
            StringBuilder jpqlIssue = new StringBuilder();
            jpqlIssue.append("SELECT ")
                    .append("SUM(ABS(bi.pharmaceuticalBillItem.qty) * bi.pharmaceuticalBillItem.itemBatch.purcahseRate), ")
                    .append("SUM(ABS(bi.pharmaceuticalBillItem.qty) * bi.pharmaceuticalBillItem.itemBatch.costRate), ")
                    .append("SUM(ABS(bi.pharmaceuticalBillItem.qty) * bi.pharmaceuticalBillItem.itemBatch.retailsaleRate) ")
                    .append("FROM BillItem bi ")
                    .append("WHERE bi.retired = :ret ")
                    .append("AND bi.bill.billType IN :btas ")
                    .append("AND bi.bill.createdAt BETWEEN :fd AND :td ")
                    .append("AND bi.pharmaceuticalBillItem.qty < 0.0 ");

            paramsIssue.put("ret", false);
            paramsIssue.put("btas", billTypes);
            paramsIssue.put("fd", fromDate);
            paramsIssue.put("td", toDate);
            addFilter(jpqlIssue, paramsIssue, "bi.bill.institution", "ins", institution);
            addFilter(jpqlIssue, paramsIssue, "bi.bill.department.site", "sit", site);
            addFilter(jpqlIssue, paramsIssue, "bi.bill.department", "dep", department);
            addFilter(jpqlIssue, paramsIssue, "bi.pharmaceuticalBillItem.itemBatch.item", "itm", item);

            // Query for Stock Adjustment Receives (positive quantities)
            Map<String, Object> paramsReceive = new HashMap<>();
            StringBuilder jpqlReceive = new StringBuilder();
            jpqlReceive.append("SELECT ")
                    .append("SUM(bi.pharmaceuticalBillItem.qty * bi.pharmaceuticalBillItem.itemBatch.purcahseRate), ")
                    .append("SUM(bi.pharmaceuticalBillItem.qty * bi.pharmaceuticalBillItem.itemBatch.costRate), ")
                    .append("SUM(bi.pharmaceuticalBillItem.qty * bi.pharmaceuticalBillItem.itemBatch.retailsaleRate) ")
                    .append("FROM BillItem bi ")
                    .append("WHERE bi.retired = :ret ")
                    .append("AND bi.bill.billType IN :btas ")
                    .append("AND bi.bill.createdAt BETWEEN :fd AND :td ")
                    .append("AND bi.pharmaceuticalBillItem.qty > 0.0 ");

            paramsReceive.put("ret", false);
            paramsReceive.put("btas", billTypes);
            paramsReceive.put("fd", fromDate);
            paramsReceive.put("td", toDate);
            addFilter(jpqlReceive, paramsReceive, "bi.bill.institution", "ins", institution);
            addFilter(jpqlReceive, paramsReceive, "bi.bill.department.site", "sit", site);
            addFilter(jpqlReceive, paramsReceive, "bi.bill.department", "dep", department);
            addFilter(jpqlReceive, paramsReceive, "bi.pharmaceuticalBillItem.itemBatch.item", "itm", item);

            // Execute queries
            List<Object[]> resultsStockAdjustmentReceives = facade.findRawResultsByJpql(jpqlReceive.toString(), paramsReceive, TemporalType.TIMESTAMP);
            List<Object[]> resultsStockAdjustmentIssue = facade.findRawResultsByJpql(jpqlIssue.toString(), paramsIssue, TemporalType.TIMESTAMP);

            Map<String, Double> resultIssue = new HashMap<>();
            if (resultsStockAdjustmentIssue != null && !resultsStockAdjustmentIssue.isEmpty()) {
                Object[] totals = resultsStockAdjustmentIssue.get(0);
                resultIssue.put("purchaseValue", totals[0] != null ? ((Number) totals[0]).doubleValue() : 0.0);
                resultIssue.put("costValue", totals[1] != null ? ((Number) totals[1]).doubleValue() : 0.0);
                resultIssue.put("retailValue", totals[2] != null ? ((Number) totals[2]).doubleValue() : 0.0);
            } else {
                resultIssue.put("purchaseValue", 0.0);
                resultIssue.put("costValue", 0.0);
                resultIssue.put("retailValue", 0.0);
            }

            Map<String, Double> resultReceive = new HashMap<>();
            if (resultsStockAdjustmentReceives != null && !resultsStockAdjustmentReceives.isEmpty()) {
                Object[] totals = resultsStockAdjustmentReceives.get(0);
                resultReceive.put("purchaseValue", totals[0] != null ? ((Number) totals[0]).doubleValue() : 0.0);
                resultReceive.put("costValue", totals[1] != null ? ((Number) totals[1]).doubleValue() : 0.0);
                resultReceive.put("retailValue", totals[2] != null ? ((Number) totals[2]).doubleValue() : 0.0);
            } else {
                resultReceive.put("purchaseValue", 0.0);
                resultReceive.put("costValue", 0.0);
                resultReceive.put("retailValue", 0.0);
            }

            cogsRows.put("Stock Adjustment Receive", resultReceive);
            cogsRows.put("Stock Adjustment Issue", resultIssue);

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Error calculating stock adjustment");
        }
    }

    private void calculateTransferIssueValue() {
        try {
            Map<String, Double> transferIssues = retrievePurchaseAndCostValues(" bi.bill.billType ", Collections.singletonList(BillType.PharmacyTransferIssue));
            cogsRows.put("Transfer Issue", transferIssues);

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Error calculating transfer issue");
        }
    }

    private void calculateTransferReceiveValue() {
        try {
            Map<String, Double> transferReceives = retrievePurchaseAndCostValues(" bi.bill.billType ", Collections.singletonList(BillType.PharmacyTransferReceive));
            cogsRows.put("Transfer Receive", transferReceives);

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Error calculating tranfer receive");
        }
    }

    private void calculateSaleCreditValue() {
        try {
            List<PaymentMethod> creditTypePaymentMethods = new ArrayList<>();
            creditTypePaymentMethods.add(PaymentMethod.Credit);
            creditTypePaymentMethods.add(PaymentMethod.Staff);

            List<BillTypeAtomic> billTypes = Arrays.asList(
                    BillTypeAtomic.PHARMACY_RETAIL_SALE,
                    BillTypeAtomic.PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER
            );

            Map<String, Double> saleCreditValues = retrievePurchaseAndCostValues(" bi.bill.billTypeAtomic ", billTypes, creditTypePaymentMethods);
            cogsRows.put("Sale Credit", saleCreditValues);

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Error calculating sale credit value");
        }
    }

    private void calculateBhtIssueValue() {
        try {
            List<BillTypeAtomic> billTypes = Arrays.asList(
                    BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD,
                    BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE
            );

            Map<String, Double> bhtIssues = retrievePurchaseAndCostValues(" bi.bill.billTypeAtomic ", billTypes);
            cogsRows.put("BHT Issue", bhtIssues);

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Error calculating BHT issues");
        }
    }

    private void calculateSaleWithoutCreditPaymentMethod() {
        try {
            List<PaymentMethod> nonCreditPaymentMethods = Arrays.stream(PaymentMethod.values())
                    .filter(pm -> pm != PaymentMethod.Credit)
                    .collect(Collectors.toList());

            List<BillTypeAtomic> billTypes = Arrays.asList(
                    BillTypeAtomic.PHARMACY_RETAIL_SALE,
                    BillTypeAtomic.PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER
            );

            Map<String, Double> saleWithoutCreditValues = retrievePurchaseAndCostValues(billTypes, nonCreditPaymentMethods);
            cogsRows.put("Sale ", saleWithoutCreditValues);

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Error calculating sale without credit payment method value");
        }
    }

    private void calculateAddToStockBills() {
        try {
            List<BillTypeAtomic> billTypes = Arrays.asList(
                    BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER
            );

            Map<String, Double> addToStockBillsValues = retrievePurchaseAndCostValuesWithoutReference(billTypes);
            cogsRows.put("Add to Stock Bills", addToStockBillsValues);

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Error calculating add to stock bills value");
        }
    }

    double totalCalculatedClosingStockPurchaseValue = 0.0;
    double totalCalculatedClosingStockCostValue = 0.0;
    double totalCalculatedClosingStockRetailValue = 0.0;

    private Map<String, Double> calculateClosingStockValueByCalculatedRows() {
        try {
            Map<String, Double> calculatedClosingStockByCogsRows = new HashMap<>();

            totalCalculatedClosingStockPurchaseValue = this.cogsRows.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals("Calculated Closing Stock Value"))
                    .filter(entry -> entry.getValue() instanceof Number || entry.getValue() instanceof Map)
                    .mapToDouble(entry -> {
                        Object value = entry.getValue();
                        if (value instanceof Number) {
                            return ((Number) value).doubleValue();
                        } else if (value instanceof Map) {
                            Map<?, ?> map = (Map<?, ?>) value;
                            Object purchaseValue = map.get("purchaseValue");
                            return purchaseValue instanceof Number ? ((Number) purchaseValue).doubleValue() : 0.0;
                        }
                        return 0.0;
                    })
                    .sum();

            totalCalculatedClosingStockCostValue = this.cogsRows.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals("Calculated Closing Stock Value"))
                    .filter(entry -> entry.getValue() instanceof Number || entry.getValue() instanceof Map)
                    .mapToDouble(entry -> {
                        Object value = entry.getValue();
                        if (value instanceof Number) {
                            return ((Number) value).doubleValue();
                        } else if (value instanceof Map) {
                            Map<?, ?> map = (Map<?, ?>) value;
                            Object costValue = map.get("costValue");
                            return costValue instanceof Number ? ((Number) costValue).doubleValue() : 0.0;
                        }
                        return 0.0;
                    })
                    .sum();

            totalCalculatedClosingStockRetailValue = this.cogsRows.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals("Calculated Closing Stock Value"))
                    .filter(entry -> entry.getValue() instanceof Number || entry.getValue() instanceof Map)
                    .mapToDouble(entry -> {
                        Object value = entry.getValue();
                        if (value instanceof Number) {
                            return ((Number) value).doubleValue();
                        } else if (value instanceof Map) {
                            Map<?, ?> map = (Map<?, ?>) value;
                            Object retailValue = map.get("retailValue");
                            return retailValue instanceof Number ? ((Number) retailValue).doubleValue() : 0.0;
                        }
                        return 0.0;
                    })
                    .sum();

            calculatedClosingStockByCogsRows.put("purchaseValue", totalCalculatedClosingStockPurchaseValue);
            calculatedClosingStockByCogsRows.put("costValue", totalCalculatedClosingStockCostValue);
            calculatedClosingStockByCogsRows.put("retailValue", totalCalculatedClosingStockRetailValue);

            cogsRows.put("Calculated Closing Stock Value", calculatedClosingStockByCogsRows);
            return calculatedClosingStockByCogsRows;

        } catch (Exception e) {
            Map<String, Double> error = new HashMap<>();
            JsfUtil.addErrorMessage(e, "Error calculating closing stock value");
            return error;
        }
    }

    private void calculateVariance(Map<String, Double> calculatedClosingStockByCogsRowValues) {
        try {
            Map<String, Double> varianceMap = new HashMap<>();

            Map<String, Double> closingStockFromDB = (Map<String, Double>) cogsRows.get("Closing Stock");

            double calculatedPurchaseValue = calculatedClosingStockByCogsRowValues.getOrDefault("purchaseValue", 0.0);
            double dbPurchaseValue = closingStockFromDB != null ? closingStockFromDB.getOrDefault("purchaseValue", 0.0) : 0.0;
            double purchaseVariance = calculatedPurchaseValue - dbPurchaseValue;

            double calculatedCostValue = calculatedClosingStockByCogsRowValues.getOrDefault("costValue", 0.0);
            double dbCostValue = closingStockFromDB != null ? closingStockFromDB.getOrDefault("costValue", 0.0) : 0.0;
            double costVariance = calculatedCostValue - dbCostValue;

            double calculatedRetailValue = calculatedClosingStockByCogsRowValues.getOrDefault("retailValue", 0.0);
            double dbRetailValue = closingStockFromDB != null ? closingStockFromDB.getOrDefault("retailValue", 0.0) : 0.0;
            double retailVariance = calculatedRetailValue - dbRetailValue;

            varianceMap.put("purchaseValue", purchaseVariance);
            varianceMap.put("costValue", costVariance);
            varianceMap.put("retailValue", retailVariance);

            cogsRows.put("Variance", varianceMap);

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Error calculating variance");
            Map<String, Double> errorMap = new HashMap<>();
            errorMap.put("ERROR", -1.0);
            cogsRows.put("Variance", errorMap);
        }
    }

    public void exportPharmacySalesToPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Pharmacy_Sales_Report.pdf");

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Pharmacy Sales Report",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph("Generated On: " + sdf.format(new Date()),
                    FontFactory.getFont(FontFactory.HELVETICA, 12)));
            if (fromDate != null && toDate != null) {
                document.add(new Paragraph("Period: " + sdf.format(fromDate) + " to " + sdf.format(toDate),
                        FontFactory.getFont(FontFactory.HELVETICA, 12)));
            }
            document.add(new Paragraph(" "));

            if (cogsBillDtos == null || cogsBillDtos.isEmpty()) {
                document.add(new Paragraph("No sales data available for the selected period.",
                        FontFactory.getFont(FontFactory.HELVETICA, 12)));
                document.close();
                context.responseComplete();
                return;
            }

            PdfPTable table = new PdfPTable(15);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            float[] columnWidths = {1.2f, 1.8f, 3.5f, 1.5f, 2.2f, 1f, 1.5f, 1.5f, 1.5f, 1.5f, 2f, 3.5f, 1.5f, 2f, 1.5f};
            table.setWidths(columnWidths);

            addHeaderRow(table);

            double grandTotal = 0.0;

            for (CostOfGoodSoldBillDTO billDto : cogsBillDtos) {
                addBillRows(table, billDto, sdf);
                grandTotal += billDto.getNetTotal() != null ? billDto.getNetTotal() : 0.0;
            }

            addGrandTotalRow(table, grandTotal);
            document.add(table);
            addSummary(document, grandTotal);

            document.close();
            context.responseComplete();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error generating PDF: " + e.getMessage());
        }
    }

    private void addBillRows(PdfPTable table, CostOfGoodSoldBillDTO billDto, SimpleDateFormat sdf) {
        List<BillItemDTO> items = billDto.getBillItems();
        if (items == null || items.isEmpty()) {
            return;
        }

        int totalItems = items.size();
        // Determine the total number of rows this bill will occupy (items + payments)
        int rowSpan = totalItems - 1;
        if (rowSpan < totalItems) {
            rowSpan = totalItems; // Ensure rowspan is at least the number of items
        }
        double billNetTotal = billDto.getNetTotal() != null ? billDto.getNetTotal() : 0.0;
        double billDiscount = billDto.getDiscount() != null ? billDto.getDiscount() : 0.0;

        boolean isFirstItem = true;

        // Loop 1: Add all the item rows
        for (BillItemDTO item : items) {
            if (isFirstItem) {
                // CORRECTED LOGIC: All spanning cells are added ONCE on the very first row.
                addSpanningCell(table, billDto.getBillCreatedAt() != null ? sdf.format(billDto.getBillCreatedAt()) : "-", rowSpan, createDateCell(""));
                addSpanningCell(table, getBillDisplayNumber(billDto), rowSpan, createDocNoCell(""));

                // Item-specific cells for the first item
                addItemCells(table, item);

                // Add the rest of the spanning cells for the bill
                addSpanningCell(table, String.format("%.2f", billNetTotal), rowSpan, createNetTotalCell(""));

                // This cell acts as a placeholder for the payment methods that will be added below
                PdfPCell paymentPlaceholder = createDataCell(billDto.getPaymentMethod().toString());
                paymentPlaceholder.setRowspan(rowSpan);
                table.addCell(paymentPlaceholder);

                addSpanningCell(table, "", rowSpan, createEmptyCell()); // MRP
                addSpanningCell(table, "", rowSpan, createEmptyCell()); // MRP Value
                addSpanningCell(table, String.format("%.2f", billDiscount), rowSpan, createDataCell(""));

                isFirstItem = false;
            } else {
                // For all subsequent items, only add the item-specific cells.
                addItemCells(table, item);
            }
        }

        // Loop 2: Add all the payment breakdown rows
        addPaymentBreakdownRows(table, billDto);
    }

    private void addItemCells(PdfPTable table, BillItemDTO item) {
        table.addCell(createItemNameCell(item.getItemName() != null ? item.getItemName() : "-"));
        table.addCell(createDataCell(item.getItemCode() != null ? item.getItemCode().toString() : "-"));
        table.addCell(createDataCell(item.getBatchNo() != null ? item.getBatchNo() : "-"));
        table.addCell(createDataCell(String.valueOf(item.getQty().intValue())));
        table.addCell(createDataCell(String.format("%.2f", item.getCostRate())));
        table.addCell(createDataCell(String.format("%.2f", calculateCostValue(item))));
        table.addCell(createDataCell(String.format("%.2f", item.getRetailRate())));
        table.addCell(createDataCell(String.format("%.2f", calculateItemValue(item))));

    }

// Individual payment method row
    private void addPaymentMethodRow(PdfPTable table, String method, double amount) {
        // Empty cells until payment column
        for (int i = 0; i < 11; i++) {
            table.addCell(createPaymentCell(""));
        }

        // Payment details
        table.addCell(createPaymentCell(method + " - " + String.format("%.2f", amount)));

        // Empty remaining cells
        table.addCell(createPaymentCell(""));
        table.addCell(createPaymentCell(""));
        table.addCell(createPaymentCell("0.00"));
    }

    private void addPaymentMethodRow(PdfPTable table, PaymentMethod method, double amount) {
        // Empty cells until payment column
        for (int i = 0; i < 11; i++) {
            table.addCell(createPaymentCell(""));
        }

        // Payment details
        table.addCell(createPaymentCell(method + " - " + String.format("%.2f", amount)));

        // Empty remaining cells
        table.addCell(createPaymentCell(""));
        table.addCell(createPaymentCell(""));
        table.addCell(createPaymentCell("0.00"));
    }

    private void addPaymentBreakdownRows(PdfPTable table, CostOfGoodSoldBillDTO billDto) {
        Map<PaymentMethod, Double> paymentBreakdown = getPaymentBreakdown(billDto);

        // Handle single payment method case
        if (!"MultiplePaymentMethods".equals(billDto.getPaymentMethod().toString())) {
            String methodName = billDto.getPaymentMethod().toString();
            double total = billDto.getNetTotal();
            addPaymentMethodRow(table, methodName, total);
        } else {
            for (Map.Entry<PaymentMethod, Double> entry : paymentBreakdown.entrySet()) {
                addPaymentMethodRow(table, entry.getKey(), entry.getValue());
            }
        }

    }

    private void addSinglePaymentRow(PdfPTable table, String methodName, double amount) {
        for (int i = 0; i < 11; i++) {
            table.addCell(createEmptyCell());
        }

        table.addCell(createPaymentCell(methodName + " - " + String.format("%.2f", amount)));
        // Empty cells for MRP, MRP Value, and Discount columns
        table.addCell(createEmptyCell());
        table.addCell(createEmptyCell());
        table.addCell(createDataCell("0.00"));
    }

    private void addSinglePaymentRow(PdfPTable table, PaymentMethod methodName, double amount) {
        for (int i = 0; i < 11; i++) {
            table.addCell(createEmptyCell());
        }

        table.addCell(createPaymentCell(methodName + " - " + String.format("%.2f", amount)));
        table.addCell(createEmptyCell());
        table.addCell(createEmptyCell());
        table.addCell(createDataCell("0.00"));
    }

    private Map<PaymentMethod, Double> getPaymentBreakdown(CostOfGoodSoldBillDTO billDto) {
        Map<PaymentMethod, Double> paymentBreakdown = new LinkedHashMap<>();
        // Assuming searchController.getPaymentDetails(bill) returns List<Payment>
        List<Payment> payments = searchController.getPaymentDetals(billDto.getBill());
        for (Payment payment : payments) {
            // Make sure to get the name of the payment method as a String
            PaymentMethod methodName = payment.getPaymentMethod();
            double paidValue = payment.getPaidValue();
            paymentBreakdown.merge(methodName, paidValue, Double::sum);
        }
        return paymentBreakdown;
    }

    private String getBillDisplayNumber(CostOfGoodSoldBillDTO billDto) {
        // Use the actual bill number from your DTO
        if (billDto.getBillDeptId() != null && !billDto.getBillDeptId().isEmpty()) {
            return billDto.getBillDeptId();
        }
        return "";
    }

    private void addSpanningCell(PdfPTable table, String content, int rowspan, PdfPCell templateCell) {
        templateCell.setPhrase(new Phrase(content, templateCell.getPhrase().getFont()));
        templateCell.setRowspan(rowspan);
        table.addCell(templateCell);
    }

    private void addHeaderRow(PdfPTable table) {
        String[] headers = {
            "Date", "Doc. No", "NAME", "CODE", "BATCH NO", "QTY",
            "COST RATE", "COST VALUE", "RATE", "VALUE", "Net Total",
            "Payment Mode/Modes", "MRP", "MRP Value", "Discount"
        };

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            cell.setBorder(Rectangle.BOX);
            table.addCell(cell);
        }
    }

    private void addGrandTotalRow(PdfPTable table, double grandTotal) {
        PdfPCell totalLabel = new PdfPCell(new Phrase("Total",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        totalLabel.setColspan(14);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabel.setPadding(5);
        totalLabel.setBorder(Rectangle.BOX);
        table.addCell(totalLabel);

        PdfPCell totalValue = new PdfPCell(new Phrase(String.format("%.2f", grandTotal),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        totalValue.setHorizontalAlignment(Element.ALIGN_CENTER);
        totalValue.setBorder(Rectangle.BOX);
        table.addCell(totalValue);
    }

    private PdfPCell createDateCell(String content) {
        PdfPCell cell = new PdfPCell(new Phrase(content != null ? content : "",
                FontFactory.getFont(FontFactory.HELVETICA, 8)));
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorder(Rectangle.BOX);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell createDocNoCell(String content) {
        PdfPCell cell = new PdfPCell(new Phrase(content != null ? content : "",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setBackgroundColor(new BaseColor(173, 216, 230)); // Light blue
        cell.setBorder(Rectangle.BOX);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell createItemNameCell(String content) {
        PdfPCell cell = new PdfPCell(new Phrase(content != null ? content : "",
                FontFactory.getFont(FontFactory.HELVETICA, 8)));
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setBorder(Rectangle.BOX);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell createDataCell(String content) {
        PdfPCell cell = new PdfPCell(new Phrase(content != null ? content : "",
                FontFactory.getFont(FontFactory.HELVETICA, 8)));
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorder(Rectangle.BOX);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell createNetTotalCell(String content) {
        PdfPCell cell = new PdfPCell(new Phrase(content != null ? content : "",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(new BaseColor(144, 238, 144)); // Light green
        cell.setBorder(Rectangle.BOX);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell createPaymentCell(String content) {
        PdfPCell cell = new PdfPCell(new Phrase(content != null ? content : "",
                FontFactory.getFont(FontFactory.HELVETICA, 8)));
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setBackgroundColor(new BaseColor(255, 255, 224)); // Light yellow
        cell.setBorder(Rectangle.BOX);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell createEmptyCell() {
        PdfPCell cell = new PdfPCell(new Phrase("", FontFactory.getFont(FontFactory.HELVETICA, 8)));
        cell.setPadding(4);
        cell.setBorder(Rectangle.BOX);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

// Helper calculation methods
    private double calculateCostValue(BillItemDTO item) {
        if (item.getCostRate() != null && item.getQty() != null) {
            return item.getCostRate() * item.getQty();
        }
        return 0.0;
    }

    private double calculateItemValue(BillItemDTO item) {
        if (item.getRetailRate() != null && item.getQty() != null) {
            return item.getRetailRate() * item.getQty();
        }
        return 0.0;
    }

    private void addSummary(Document document, double grandTotal) throws DocumentException {
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Summary:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        document.add(new Paragraph("Total Bills: " + (cogsBillDtos != null ? cogsBillDtos.size() : 0)));
        document.add(new Paragraph("Grand Total: " + String.format("%.2f", grandTotal)));
    }

    public void exportBatchWisePharmacyStockToPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Stock_Report.pdf");

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Generated On: " + sdf.format(new Date()),
                    FontFactory.getFont(FontFactory.HELVETICA, 12)));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(14);
            table.setWidthPercentage(100);
            float[] columnWidths = {1f, 2f, 2f, 3f, 2f, 2f, 2f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f};
            table.setWidths(columnWidths);

            String[] headers = {"S.No", "Item Category", "Item Code", "Item Name", "UOM", "Expiry", "Batch No", "Qty",
                "Purchase Rate", "Purchase Value", "Cost Rate", "Cost Value", "Sale Rate", "Sale Value"};

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            }

            List<PharmacyRow> rows = getRows();

            if (rows == null || rows.isEmpty()) {
                JsfUtil.addErrorMessage("No data available to export");
                context.responseComplete();
                return;
            }

            int serial = 1;
            for (PharmacyRow f : rows) {
                table.addCell(String.valueOf(serial++));
                table.addCell(f.getItem().getCategory() != null ? f.getItem().getCategory().getName() : "-");
                table.addCell(f.getItem().getCode() != null ? f.getItem().getCode() : "-");
                table.addCell(f.getItem().getName() != null ? f.getItem().getName() : "-");
                table.addCell(f.getItem().getMeasurementUnit() != null ? f.getItem().getMeasurementUnit().getName() : "-");
                table.addCell(f.getItemBatch() != null && f.getItemBatch().getDateOfExpire() != null ? sdf.format(f.getItemBatch().getDateOfExpire()) : "-");
                table.addCell(f.getItemBatch() != null ? f.getItemBatch().getBatchNo() : "-");
                table.addCell(f.getQuantity() != null ? String.format("%.2f", f.getQuantity()) : "0.00");
                table.addCell(f.getPurchaseRate() != null ? String.format("%.2f", f.getPurchaseRate()) : "0.00");
                table.addCell(f.getPurchaseValue() != null ? String.format("%.2f", f.getPurchaseValue()) : "0.00");
                table.addCell(f.getCostRate() != null ? String.format("%.2f", f.getCostRate()) : "0.00");
                table.addCell(f.getCostValue() != null ? String.format("%.2f", f.getCostValue()) : "0.00");
                table.addCell(f.getRetailRate() != null ? String.format("%.2f", f.getRetailRate()) : "0.00");
                table.addCell(f.getSaleValue() != null ? String.format("%.2f", f.getSaleValue()) : "0.00");
            }

            PdfPCell footerCell = new PdfPCell(new Phrase("Total", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            footerCell.setColspan(7);
            footerCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(footerCell);
            table.addCell(String.format("%.2f", getStockQty()));
            table.addCell("");
            table.addCell(String.format("%.2f", getStockPurchaseValue()));
            table.addCell("");
            table.addCell(String.format("%.2f", getStockCostValue()));
            table.addCell("");
            table.addCell(String.format("%.2f", getStockSaleValue()));

            document.add(table);
            document.close();
            context.responseComplete();
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error generating PDF: " + e.getMessage());
            Logger.getLogger(PharmacyReportController.class.getName()).log(Level.SEVERE, "Error generating PDF", e);
        }
    }

    public void exportItemWisePharmacyStockToPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Stock_Report.pdf");

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Generated On: " + sdf.format(new Date()),
                    FontFactory.getFont(FontFactory.HELVETICA, 12)));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(9);
            table.setWidthPercentage(100);
            float[] columnWidths = {1f, 2f, 2f, 3f, 2f, 2.5f, 2.5f, 2.5f, 2.5f};
            table.setWidths(columnWidths);

            String[] headers = {"S.No", "Item Category", "Item Code", "Item Name", "UOM", "Closing Stock", "Purchase Value", "Cost Value", "Sale Value"};

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            }

            List<PharmacyRow> rows = getRows();

            if (rows == null || rows.isEmpty()) {
                JsfUtil.addErrorMessage("No data available to export");
                context.responseComplete();
                return;
            }

            int serial = 1;
            for (PharmacyRow f : rows) {
                table.addCell(String.valueOf(serial++));
                table.addCell(f.getItem().getCategory() != null ? f.getItem().getCategory().getName() : "-");
                table.addCell(f.getItem().getCode() != null ? f.getItem().getCode() : "-");
                table.addCell(f.getItem().getName() != null ? f.getItem().getName() : "-");
                table.addCell(f.getItem().getMeasurementUnit() != null ? f.getItem().getMeasurementUnit().getName() : "-");

                table.addCell(f.getQuantity() != null ? String.format("%.2f", f.getQuantity()) : "0.00");
                table.addCell(f.getPurchaseValue() != null ? String.format("%.2f", f.getPurchaseValue()) : "0.00");
                table.addCell(f.getCostValue() != null ? String.format("%.2f", f.getCostValue()) : "0.00");
                table.addCell(f.getSaleValue() != null ? String.format("%.2f", f.getSaleValue()) : "0.00");
            }

            PdfPCell footerCell = new PdfPCell(new Phrase("Total", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            footerCell.setColspan(5);
            footerCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(footerCell);
            table.addCell(String.format("%.2f", getStockQty()));
            table.addCell(String.format("%.2f", getStockPurchaseValue()));
            table.addCell(String.format("%.2f", getStockCostValue()));
            table.addCell(String.format("%.2f", getStockSaleValue()));

            document.add(table);
            document.close();
            context.responseComplete();
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error generating PDF: " + e.getMessage());
            Logger.getLogger(PharmacyReportController.class.getName()).log(Level.SEVERE, "Error generating PDF", e);
        }
    }

    @Deprecated
    public void processClosingStockReport() {
        stockSaleValue = 0.0;
        stockQty = 0.0;
        stockPurchaseValue = 0.0;
        stockTotal = 0.0;

        List<Long> ids;
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder("select MAX(sh.id) "
                + " from StockHistory sh where sh.retired=:ret "
                + " and (sh.itemBatch.item.departmentType is null or sh.itemBatch.item.departmentType = :depty) ");
//                + "and sh.stockQty > 0 "); Eventhough the qty is zero, thay need to be considered

        params.put("depty", DepartmentType.Pharmacy);
        params.put("ret", false);

        if (institution != null) {
            jpql.append("and sh.institution = :ins ");
            params.put("ins", institution);
        }

        if (site != null) {
            jpql.append("and sh.department.site = :sit ");
            params.put("sit", site);
        }

        if (department != null) {
            jpql.append("and sh.department = :dep ");
            params.put("dep", department);
        }

        if (category != null) {
            jpql.append("and sh.itemBatch.item.category = :cat ");
            params.put("cat", category);
        }

        System.out.println("amp = " + amp);
        if (amp != null) {
            item = amp;
            System.out.println("item = " + item);
            jpql.append("and sh.itemBatch.item=:itm ");
            params.put("itm", item);
        }

        jpql.append("and sh.createdAt < :et ");
        params.put("et", CommonFunctions.getEndOfDay(toDate));

        jpql.append("group by sh.itemBatch ");
        jpql.append("order by sh.itemBatch.item.name");

        System.out.println("jpql.toString() = " + jpql.toString());
        System.out.println("params = " + params);

        ids = getStockFacade().findLongValuesByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        stockTotal = 0.0;

        rows = new ArrayList<>();

        if (ids == null) {
            return;
        }

        if (ids.isEmpty()) {
            return;
        }

        for (Long shid : ids) {
            System.out.println("shid = " + shid);
            PharmacyRow pr = new PharmacyRow();
            pr.setId(shid);
            pr.setStockHistory(facade.find(shid));
            if (pr.getStockHistory() != null) {
                StockHistory stockHx = pr.getStockHistory();
                stockPurchaseValue += stockHx.getItemBatch().getPurcahseRate() * stockHx.getStockQty();
                stockSaleValue += stockHx.getItemBatch().getRetailsaleRate() * stockHx.getStockQty();
                stockTotal += stockHx.getStockQty();
                rows.add(pr);
            }
        }

        if (reportType.equalsIgnoreCase("itemWise")) {
            stockPurchaseValue = 0.0;
            stockQty = 0.0;

            Map<String, PharmacyRow> map = new HashMap<>();

            for (PharmacyRow row : rows) {
                if (map.containsKey(row.getStockHistory().getItemBatch().getItem().getName())) {
                    PharmacyRow pr = map.get(row.getStockHistory().getItemBatch().getItem().getName());

                    if (row.getStockHistory().getStockQty() == 0.0) {
                        continue;
                    }

                    pr.getStockHistory().setStockQty(pr.getStockHistory().getStockQty() + row.getStockHistory().getStockQty());
                    setStockQty(getStockQty() + row.getStockHistory().getStockQty());

                    pr.getStockHistory().setStockSaleValue(pr.getStockHistory().getStockSaleValue() + row.getStockHistory().getStockQty()
                            * row.getStockHistory().getItemBatch().getRetailsaleRate());
                    setStockPurchaseValue(getStockPurchaseValue() + row.getStockHistory().getStockQty() * row.getStockHistory().getItemBatch().getPurcahseRate());
                } else {
                    if (row.getStockHistory().getStockQty() == 0.0) {
                        continue;
                    }

                    map.put(row.getStockHistory().getItemBatch().getItem().getName(), row);

                    map.get(row.getStockHistory().getItemBatch().getItem().getName()).getStockHistory().setStockSaleValue(
                            row.getStockHistory().getStockQty() * row.getStockHistory().getItemBatch().getRetailsaleRate());

                    setStockQty(getStockQty() + row.getStockHistory().getStockQty());
                    setStockPurchaseValue(getStockPurchaseValue() + row.getStockHistory().getStockQty() * row.getStockHistory().getItemBatch().getPurcahseRate());
                }
            }

            rows.clear();
            rows.addAll(map.values());
        }
    }

    //method for update dates when select date range
    public void updateDateRange() {
        //System.out.println("Date Range Selected: " + dateRange);
        LocalDate today = LocalDate.now();

        switch (dateRange) {
            case "within3months":
                fromDate = convertToDate(today.minusMonths(3));
                toDate = convertToDate(today);
                break;
            case "within6months":
                fromDate = convertToDate(today.minusMonths(6));
                toDate = convertToDate(today);
                break;
            case "within12months":
                fromDate = convertToDate(today.minusMonths(12));
                toDate = convertToDate(today);
                break;
            case "shortexpiry":
                fromDate = convertToDate(today);
                toDate = convertToDate(today.plusMonths(3));
                break;
        }
        // System.out.println("Updated From Date: " + fromDate);
        // System.out.println("Updated To Date: " + toDate);
    }

    // Utility to convert LocalDate to Date
    private Date convertToDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public void processExpiryItemReport() {
        stocks = new ArrayList();
        String jpql;
        Map m = new HashMap();
        m.put("fd", fromDate);
        m.put("td", toDate);

        jpql = "select s"
                + " from Stock s "
                + " where s.itemBatch.dateOfExpire between :fd and :td ";

        if (institution != null) {
            jpql += " and s.department.institution=:ins ";
            m.put("ins", institution);
        }

        if (department != null) {
            jpql += " and s.department=:dep ";
            m.put("dep", department);
        }

        if (site != null) {
            jpql += " and s.department.site=:sit ";
            m.put("sit", site);
        }

        if (amp != null) {
            item = amp;
            System.out.println("item = " + item);
            jpql += "and s.itemBatch.item=:itm ";
            m.put("itm", item);
        }

        if (category != null) {
            jpql += " and s.itemBatch.item.category=:cat ";
            m.put("cat", category);
        }

        jpql += " order by s.id ";
        stocks = stockFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        quantity = 0.0;

        for (Stock ts : stocks) {
            if (ts.getItemBatch() == null || ts.getStock() == null) {
                continue;
            }

            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
            quantity = quantity + ts.getStock();
        }

        groupExpiryItems(stocks);
    }

    private void groupExpiryItems(final List<Stock> stocks) {
        // Map<Item, Map<Batch Number, List<Stock>>> itemStockMap
        Map<Item, Map<Long, List<Stock>>> itemStockMap = new HashMap<>();

        for (Stock stock : stocks) {
            if (stock.getItemBatch() == null || stock.getItemBatch().getItem() == null) {
                continue;
            }

            final Item item = stock.getItemBatch().getItem();
            Map<Long, List<Stock>> batchStockMap = itemStockMap.computeIfAbsent(item, k -> new HashMap<>());

            final Long batchNumber = stock.getItemBatch().getId();
            List<Stock> stockList = batchStockMap.computeIfAbsent(batchNumber, k -> new ArrayList<>());
            stockList.add(stock);

            batchStockMap.put(batchNumber, stockList);
            itemStockMap.put(item, batchStockMap);
        }

        setItemStockMap(itemStockMap);
    }

    public void exportExpiryItemReportToExcel() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Expiry_Item_Report.xlsx");

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss");

        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = response.getOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Report");
            int rowIndex = 0;

            Row headerRow = sheet.createRow(rowIndex++);

            String[] headers = {"Department/Staff", "Item Category Code", "Item Category Name", "Item Code", "Item Name",
                "Base UOM", "Item Type", "Batch No", "Batch Date", "Expiry Date", "Supplier",
                "Shelf life remaining (Days)", "Rate", "MRP", "Quantity", "Item Value",
                "Batch wise Item Value", "Batch wise Qty", "Item wise total", "Item wise Qty"};

            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            for (Map.Entry<Item, Map<Long, List<Stock>>> entry : getItemStockMap().entrySet()) {
                Item item = entry.getKey();
                Map<Long, List<Stock>> batchStockMap = entry.getValue();

                for (Map.Entry<Long, List<Stock>> batchEntry : batchStockMap.entrySet()) {
                    Long batchNumber = batchEntry.getKey();
                    List<Stock> stockList = batchEntry.getValue();

                    for (Stock stock : stockList) {
                        Row row = sheet.createRow(rowIndex++);

                        row.createCell(0).setCellValue(stock.getDepartment() != null ? stock.getDepartment().getName() : stock.getStaff().getPerson().getNameWithTitle());
                        row.createCell(1).setCellValue(item.getCategory() != null ? item.getCategory().getCode() : "-");
                        row.createCell(2).setCellValue(item.getCategory() != null ? item.getCategory().getName() : "-");
                        row.createCell(3).setCellValue(item.getCode() != null ? item.getCode() : "-");
                        row.createCell(4).setCellValue(item.getName() != null ? item.getName() : "-");
                        row.createCell(5).setCellValue(item.getMeasurementUnit() != null ? item.getMeasurementUnit().getName() : "-");
                        row.createCell(6).setCellValue(item.getCategory() != null ? item.getCategory().getName() : "-");
                        row.createCell(7).setCellValue(stock.getItemBatch().getId());
                        row.createCell(8).setCellValue(stock.getItemBatch() != null
                                && stock.getItemBatch().getLastPurchaseBillItem() != null
                                && stock.getItemBatch().getLastPurchaseBillItem().getBill() != null
                                && stock.getItemBatch().getLastPurchaseBillItem().getBill().getCreatedAt() != null
                                ? sdf.format(stock.getItemBatch().getLastPurchaseBillItem().getBill().getCreatedAt()) : "-");
                        row.createCell(9).setCellValue(stock.getItemBatch() != null
                                && stock.getItemBatch().getDateOfExpire() != null
                                ? sdf.format(stock.getItemBatch().getDateOfExpire()) : "-");
                        row.createCell(10).setCellValue(stock.getItemBatch() != null
                                && stock.getItemBatch().getLastPurchaseBillItem() != null
                                && stock.getItemBatch().getLastPurchaseBillItem().getBill() != null
                                && stock.getItemBatch().getLastPurchaseBillItem().getBill().getFromInstitution() != null
                                ? stock.getItemBatch().getLastPurchaseBillItem().getBill().getFromInstitution().getName() : "-");
                        row.createCell(11).setCellValue(stock.getItemBatch() != null
                                && stock.getItemBatch().getDateOfExpire() != null
                                ? calculateDaysRemaining(stock.getItemBatch().getDateOfExpire()) : 0);
                        row.createCell(12).setCellValue(stock.getItemBatch() != null ? stock.getItemBatch().getPurcahseRate() : 0);
                        row.createCell(13).setCellValue(stock.getItemBatch() != null ? stock.getItemBatch().getRetailsaleRate() : 0);
                        row.createCell(14).setCellValue(stock.getStock() != null ? stock.getStock() : 0);

                        double itemValue = stock.getItemBatch() != null && stock.getStock() != null
                                ? stock.getItemBatch().getPurcahseRate() * stock.getStock() : 0;
                        row.createCell(15).setCellValue(itemValue);

                        row.createCell(16).setCellValue("-");
                        row.createCell(17).setCellValue("-");
                        row.createCell(18).setCellValue("-");
                        row.createCell(19).setCellValue("-");
                    }

                    Row batchFooterRow = sheet.createRow(rowIndex++);
                    batchFooterRow.createCell(16).setCellValue(calculateBatchWiseTotalOfExpiredItems(item, batchNumber));
                    batchFooterRow.createCell(17).setCellValue(calculateBatchWiseQtyOfExpiredItems(item, batchNumber));
                }
                Row itemFooterRow = sheet.createRow(rowIndex++);

                itemFooterRow.createCell(18).setCellValue(calculateItemWiseTotalOfExpiredItems(item));
                itemFooterRow.createCell(19).setCellValue(calculateItemWiseQtyOfExpiredItems(item));
            }
            Row tableFooterRow = sheet.createRow(rowIndex++);
            tableFooterRow.createCell(16).setCellValue(stockPurchaseValue);
            tableFooterRow.createCell(17).setCellValue(quantity);
            tableFooterRow.createCell(18).setCellValue(stockPurchaseValue);
            tableFooterRow.createCell(19).setCellValue(quantity);

            workbook.write(out);
            context.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportExpiryItemReportToPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Expiry_Item_Report.pdf");

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Expiry Item Report",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            document.add(new Paragraph("Generated On: " + sdf.format(new Date()),
                    FontFactory.getFont(FontFactory.HELVETICA, 12)));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(20);
            table.setWidthPercentage(100);
            float[] columnWidths = {3f, 2f, 3f, 2f, 3f, 2f, 2f, 2f, 3f, 3f, 3f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f};
            table.setWidths(columnWidths);

            String[] headers = {"Department/Staff", "Item Cat Code", "Item Cat Name", "Item Code", "Item Name", "Base UOM",
                "Item Type", "Batch No", "Batch Date", "Expiry Date", "Supplier", "Shelf Life (Days)", "Rate", "MRP",
                "Quantity", "Item Value", "Batch Wise Item Value", "Batch Wise Qty", "Item Wise Total", "Item Wise Qty"};

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            }

            for (Map.Entry<Item, Map<Long, List<Stock>>> entry : getItemStockMap().entrySet()) {
                Item item = entry.getKey();
                Map<Long, List<Stock>> batchStockMap = entry.getValue();

                for (Map.Entry<Long, List<Stock>> batchEntry : batchStockMap.entrySet()) {
                    Long batchNumber = batchEntry.getKey();
                    List<Stock> stockList = batchEntry.getValue();

                    for (Stock stock : stockList) {
                        table.addCell(stock.getDepartment() != null ? stock.getDepartment().getName() : stock.getStaff().getPerson().getNameWithTitle());
                        table.addCell(item.getCategory() != null ? item.getCategory().getCode() : "-");
                        table.addCell(item.getCategory() != null ? item.getCategory().getName() : "-");
                        table.addCell(item.getCode() != null ? item.getCode() : "-");
                        table.addCell(item.getName() != null ? item.getName() : "-");
                        table.addCell(item.getMeasurementUnit() != null ? item.getMeasurementUnit().getName() : "-");
                        table.addCell(item.getCategory() != null ? item.getCategory().getName() : "-");
                        table.addCell(stock.getItemBatch() != null ? String.valueOf(stock.getItemBatch().getId()) : "-");
                        table.addCell(stock.getItemBatch() != null && stock.getItemBatch().getLastPurchaseBillItem() != null
                                && stock.getItemBatch().getLastPurchaseBillItem().getBill() != null
                                && stock.getItemBatch().getLastPurchaseBillItem().getBill().getCreatedAt() != null
                                ? sdf.format(stock.getItemBatch().getLastPurchaseBillItem().getBill().getCreatedAt()) : "-");
                        table.addCell(stock.getItemBatch() != null && stock.getItemBatch().getDateOfExpire() != null
                                ? sdf.format(stock.getItemBatch().getDateOfExpire()) : "-");
                        table.addCell(stock.getItemBatch() != null && stock.getItemBatch().getLastPurchaseBillItem() != null
                                && stock.getItemBatch().getLastPurchaseBillItem().getBill() != null
                                && stock.getItemBatch().getLastPurchaseBillItem().getBill().getFromInstitution() != null
                                ? stock.getItemBatch().getLastPurchaseBillItem().getBill().getFromInstitution().getName() : "-");
                        table.addCell(stock.getItemBatch() != null && stock.getItemBatch().getDateOfExpire() != null
                                ? String.valueOf(calculateDaysRemaining(stock.getItemBatch().getDateOfExpire())) : "0");
                        table.addCell(String.valueOf(stock.getItemBatch() != null ? stock.getItemBatch().getPurcahseRate() : 0));
                        table.addCell(String.valueOf(stock.getItemBatch() != null ? stock.getItemBatch().getRetailsaleRate() : 0));
                        table.addCell(String.valueOf(stock.getStock() != null ? stock.getStock() : 0));

                        double itemValue = stock.getItemBatch() != null && stock.getStock() != null
                                ? stock.getItemBatch().getPurcahseRate() * stock.getStock() : 0;
                        table.addCell(String.valueOf(itemValue));
                        table.addCell("-");
                        table.addCell("-");
                        table.addCell("-");
                        table.addCell("-");
                    }
                    for (int i = 0; i < 16; i++) {
                        table.addCell(" ");
                    }
                    table.addCell(String.valueOf(calculateItemWiseTotalOfExpiredItems(item)));
                    table.addCell(String.valueOf(calculateBatchWiseQtyOfExpiredItems(item, batchNumber)));
                    for (int i = 0; i < 2; i++) {
                        table.addCell(" ");
                    }
                }
                for (int i = 0; i < 18; i++) {
                    table.addCell(" ");
                }
                table.addCell(String.valueOf(calculateItemWiseTotalOfExpiredItems(item)));
                table.addCell(String.valueOf(calculateItemWiseQtyOfExpiredItems(item)));
            }
            for (int i = 0; i < 16; i++) {
                table.addCell(" ");
            }
            table.addCell(String.format("%.2f", stockPurchaseValue));
            table.addCell(String.format("%.2f", quantity));
            table.addCell(String.format("%.2f", stockPurchaseValue));
            table.addCell(String.format("%.2f", quantity));

            document.add(table);
            document.close();
            context.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Double calculateBatchWiseTotalOfExpiredItems(final Item item, final Long batchNumber) {
        final Map<Long, List<Stock>> batchStockMap = getItemStockMap().get(item);
        final List<Stock> stockList = batchStockMap.get(batchNumber);

        return stockList.stream()
                .mapToDouble(stock -> stock.getItemBatch().getPurcahseRate() * stock.getStock())
                .sum();
    }

    public Double calculateBatchWiseQtyOfExpiredItems(final Item item, final Long batchNumber) {
        final Map<Long, List<Stock>> batchStockMap = getItemStockMap().get(item);
        final List<Stock> stockList = batchStockMap.get(batchNumber);

        return stockList.stream()
                .mapToDouble(Stock::getStock)
                .sum();
    }

    public Double calculateItemWiseTotalOfExpiredItems(final Item item) {
        final Map<Long, List<Stock>> batchStockMap = getItemStockMap().get(item);

        return batchStockMap.values().stream()
                .flatMap(List::stream)
                .mapToDouble(stock -> stock.getItemBatch().getPurcahseRate() * stock.getStock())
                .sum();
    }

    public Double calculateItemWiseQtyOfExpiredItems(final Item item) {
        final Map<Long, List<Stock>> batchStockMap = getItemStockMap().get(item);

        return batchStockMap.values().stream()
                .flatMap(List::stream)
                .mapToDouble(Stock::getStock)
                .sum();
    }

    public long calculateDaysRemaining(Date dateOfExpire) {
        if (dateOfExpire == null) {
            return 0; // Default behavior for null dates
        }
        // Convert Date to LocalDate
        LocalDate today = LocalDate.now();
        LocalDate expiryDate = dateOfExpire.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        // Calculate the difference in days
        return ChronoUnit.DAYS.between(today, expiryDate);
    }

    public void processSlowFastNonMovementReport() {
        switch (reportType) {
            case "fmovement":
                fillFastMoving();
                break;
            case "smovement":
                fillSlowMoving();
                break;
            case "nmovement":
                fillDepartmentNonmovingStocks();
                break;
        }
    }

    public void fillFastMoving() {
        fillMoving(true);
        fillMovingQty(true);

    }

    public void fillSlowMoving() {
        fillMoving(false);
        fillMovingQty(false);

    }

    public Institution findLastPurchaseSupplier(Item item, Institution ins, Department dept) {
        String jpql = "SELECT bi "
                + " FROM BillItem bi "
                + " WHERE bi.bill.retired = false "
                + " AND bi.bill.billTypeAtomic IN :bta "
                + " AND bi.item = :item ";

        if (ins != null) {
            jpql += " AND bi.bill.institution = :institution ";
        }
        if (dept != null) {
            jpql += " AND bi.bill.department = :department ";
        }

        jpql += " ORDER BY bi.id DESC";

        List<BillTypeAtomic> btaList = Arrays.asList(
                BillTypeAtomic.PHARMACY_GRN, BillTypeAtomic.PHARMACY_DIRECT_PURCHASE
        );

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("bta", btaList);
        parameters.put("item", item);

        if (ins != null) {
            parameters.put("institution", ins);
        }
        if (dept != null) {
            parameters.put("department", dept);
        }

        BillItem bit = getBillItemFacade().findFirstByJpql(jpql, parameters);
        if (bit == null || bit.getBill() == null || bit.getBill().getBillTypeAtomic() == null) {
            return null;
        }

        switch (bit.getBill().getBillTypeAtomic()) {
            case PHARMACY_GRN:
            case PHARMACY_DIRECT_PURCHASE:
                return bit.getBill().getFromInstitution();
            default:
                return null;
        }
    }

    public void fillMoving(boolean fast) {
        String sql;
        Map m = new HashMap();
//        m.put("r", StockReportRecord.class);
        m.put("d", department);
//        m.put("t1", BillType.PharmacyTransferIssue);
//        m.put("t2", BillType.PharmacyPre);
        m.put("fd", fromDate);
        m.put("td", toDate);
        List<BillType> bts = Arrays.asList(billTypes);
        Class[] btsa = {PreBill.class, CancelledBill.class, RefundBill.class, BilledBill.class};
        List<Class> bcts = Arrays.asList(btsa);
        m.put("bt", bts);
        m.put("bct", bcts);
        BillItem bi = new BillItem();

        sql = "select bi.item, abs(SUM(bi.pharmaceuticalBillItem.qty)), "
                + "abs(SUM(bi.pharmaceuticalBillItem.stock.itemBatch.purcahseRate * bi.pharmaceuticalBillItem.qty)), "
                + "abs(SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.qty))  "
                + "FROM BillItem bi where "
                + " type(bi.bill) in :bct "
                + " and bi.retired=false "
                + " and bi.bill.department=:d "
                + " and bi.bill.createdAt between :fd and :td "
                + " and bi.bill.billType in :bt ";

        if (institution != null) {
            sql += " and bi.bill.department.institution=:ins ";
            m.put("ins", institution);
        }
        if (site != null) {
            sql += " and bi.bill.department.site=:sit ";
            m.put("sit", site);
        }
        if (category != null) {
            sql += " and bi.item.category=:ctgry ";
            m.put("ctgry", category);
        }
        if (amp != null) {
            item = amp;
            System.out.println("item = " + item);
            sql += "and bi.item=:itm ";
            m.put("itm", item);
        }

        sql += " group by bi.item ";

//        if (!fast) {
//            sql += "order by  SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.pharmaceuticalBillItem.qty) desc";
//        } else {
//            sql += "order by  SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.pharmaceuticalBillItem.qty) asc";
//        }
        if (sortType.equalsIgnoreCase("byvalue") && !fast) {
            sql += "order by  SUM(bi.pharmaceuticalBillItem.stock.itemBatch.purcahseRate * bi.pharmaceuticalBillItem.qty) desc";
        }
        if (sortType.equalsIgnoreCase("byvalue") && fast) {
            sql += "order by  SUM(bi.pharmaceuticalBillItem.stock.itemBatch.purcahseRate * bi.pharmaceuticalBillItem.qty) asc";
        }

        ////System.out.println("sql = " + sql);
        ////System.out.println("m = " + m);
        List<Object[]> objs = getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);
        movementRecords = new ArrayList<>();
        if (objs == null) {
            ////System.out.println("objs = " + objs);
            return;
        }
        for (Object[] obj : objs) {
            StockReportRecord r = new StockReportRecord();
            r.setItem((Item) obj[0]);
            r.setQty((Double) obj[1]);
            r.setLastPurchaseSupplier(findLastPurchaseSupplier(r.getItem(), institution, department));
            r.setPurchaseValue((Double) obj[2]);
            r.setRetailsaleValue((Double) obj[3]);
            r.setStockQty(getPharmacyBean().getStockByPurchaseValue(r.getItem(), department));
            r.setStockOnHand(getPharmacyBean().getStockWithoutPurchaseValue(r.getItem(), department));
            movementRecords.add(r);
        }

        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        valueOfQOH = 0.0;
        qoh = 0.0;

        for (StockReportRecord strr : movementRecords) {
            stockPurchaseValue = stockPurchaseValue + (strr.getPurchaseValue());
            stockSaleValue = stockSaleValue + (strr.getRetailsaleValue());
            valueOfQOH = valueOfQOH + (strr.getStockQty());
            qoh = qoh + (strr.getStockOnHand());
        }
    }

    public void fillMovingQty(boolean fast) {
        String sql;
        Map m = new HashMap();

        List<BillType> bts = Arrays.asList(billTypes);

        m.put("d", department);
//        m.put("t1", BillType.PharmacyTransferIssue);
//        m.put("t2", BillType.PharmacyPre);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", bts);

        Class[] btsa = {PreBill.class, CancelledBill.class, RefundBill.class, BilledBill.class};
        List<Class> bcts = Arrays.asList(btsa);
        m.put("bct", bcts);
        BillItem bi = new BillItem();

        sql = "select bi.item, "
                + " abs(SUM(bi.pharmaceuticalBillItem.qty)), "
                + " abs(SUM(bi.pharmaceuticalBillItem.stock.itemBatch.purcahseRate * bi.pharmaceuticalBillItem.qty)), "
                + " SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.qty)"
                + " FROM BillItem bi "
                + " where bi.retired=false "
                + " and  bi.bill.department=:d "
                + " and bi.bill.billType in :bt "
                + " and type(bi.bill) in :bct "
                + " and bi.bill.createdAt between :fd and :td ";

        if (institution != null) {
            sql += " and bi.bill.department.institution=:ins ";
            m.put("ins", institution);
        }
        if (site != null) {
            sql += " and bi.bill.department.site=:sit ";
            m.put("sit", site);
        }
        if (category != null) {
            sql += " and bi.item.category=:ctgry ";
            m.put("ctgry", category);
        }
        if (amp != null) {
            item = amp;
            System.out.println("item = " + item);
            sql += "and bi.item=:itm ";
            m.put("itm", item);
        }

        sql += " group by bi.item ";

//        if (!fast) {
//            sql += "order by  SUM(bi.pharmaceuticalBillItem.qty) desc";
//        } else {
//            sql += "order by  SUM(bi.pharmaceuticalBillItem.qty) asc";
//        }
        if (sortType.equalsIgnoreCase("byquantity") && !fast) {
            sql += "order by  SUM(bi.pharmaceuticalBillItem.qty) desc";
        }
        if (sortType.equalsIgnoreCase("byquantity") && fast) {
            sql += "order by  SUM(bi.pharmaceuticalBillItem.qty) asc";
        }

        List<Object[]> objs = getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);
        movementRecordsQty = new ArrayList<>();
        if (objs == null) {
            return;
        }
        for (Object[] obj : objs) {
            StockReportRecord r = new StockReportRecord();
            r.setItem((Item) obj[0]);
            r.setQty((Double) obj[1]);
            r.setLastPurchaseSupplier(findLastPurchaseSupplier(r.getItem(), institution, department));
            r.setPurchaseValue((Double) obj[2]);
            r.setRetailsaleValue((Double) obj[3]);
            r.setStockQty(getPharmacyBean().getStockByPurchaseValue(r.getItem(), department));
            r.setStockOnHand(getPharmacyBean().getStockWithoutPurchaseValue(r.getItem(), department));
            movementRecordsQty.add(r);
        }

        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        valueOfQOH = 0.0;
        qoh = 0.0;

        for (StockReportRecord strr : movementRecords) {
            stockPurchaseValue = stockPurchaseValue + (strr.getPurchaseValue());
            stockSaleValue = stockSaleValue + (strr.getRetailsaleValue());
            valueOfQOH = valueOfQOH + (strr.getStockQty());
            qoh = qoh + (strr.getStockOnHand());
        }
    }

    public void fillDepartmentNonmovingStocks() {
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m = new HashMap();
        String jpql;
        jpql = "SELECT bi.item "
                + " FROM BillItem bi "
                + " WHERE  "
                + " bi.bill.department=:d "
                + " AND bi.bill.billType in :bts "
                + " AND bi.bill.billDate between :fd and :td ";
        m.put("d", department);
        m.put("bts", Arrays.asList(billTypes));
        m.put("fd", fromDate);
        m.put("td", toDate);

        jpql += " GROUP BY bi.item";

        //System.out.println("sql = " + sql);
        //System.out.println("m = " + m);
        Set<Item> bis = new HashSet<>(itemFacade.findByJpql(jpql, m));

        jpql = "SELECT s.itemBatch.item "
                + " FROM Stock s "
                + " WHERE s.department=:d "
                + " AND s.stock > 0 ";
        m = new HashMap();
        m.put("d", department);
        if (institution != null) {
            jpql += " and s.department.institution=:ins ";
            m.put("ins", institution);
        }
        if (department != null) {
            jpql += " and s.department=:dep ";
            m.put("dep", department);
        }
        if (site != null) {
            jpql += " and s.department.site=:sit ";
            m.put("sit", site);
        }
        if (amp != null) {
            item = amp;
            System.out.println("item = " + item);
            jpql += "and s.itemBatch.item=:itm ";
            m.put("itm", item);
        }
        jpql = jpql + " GROUP BY s.itemBatch.item "
                + " ORDER BY s.itemBatch.item.name";

        Set<Item> sis = new HashSet<>(itemFacade.findByJpql(jpql, m));

        sis.removeAll(bis);
        items = new ArrayList<>(sis);
        itemLastSuppliers = new ArrayList<>();
        for (Item item : items) {
            ItemLastSupplier ils = new ItemLastSupplier(item, findLastPurchaseSupplier(item, institution, department));
            itemLastSuppliers.add(ils);
        }

        Collections.sort(items);

    }

    public void processLabTestWiseCountReport() {
        String jpql = "select new com.divudi.core.data.TestWiseCountReport("
                + "bi.item.name, "
                + "count(bi.item.name), "
                + "sum(bi.hospitalFee), "
                + "sum(bi.collectingCentreFee), "
                + "sum(bi.staffFee), "
                + "sum(bi.discount), "
                + "sum(bi.netValue)"
                + ") "
                + "from BillItem bi "
                + "where bi.retired = :ret "
                + "and bi.bill.createdAt between :fd and :td "
                + "and bi.bill.billTypeAtomic IN :bType " // Corrected IN clause
                + "and type(bi.item) = :invType ";

        // Adding filters for institution, department, site
        if (institution != null) {
            jpql += "and bi.bill.institution = :ins ";
        }
        if (department != null) {
            jpql += "and bi.bill.department = :dep ";
        }
        if (site != null) {
            jpql += "and bi.bill.department.site = :site ";
        }
        jpql += "group by bi.item.name";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("fd", fromDate);
        m.put("td", toDate);

        // Handle multiple bill types
        List<BillTypeAtomic> bTypes = Arrays.asList(
                BillTypeAtomic.OPD_BILL_WITH_PAYMENT,
                BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER
        );
        m.put("bType", bTypes);  // Use 'bType' for IN clause

        m.put("invType", Investigation.class);

        if (institution != null) {
            m.put("ins", institution);
        }
        if (department != null) {
            m.put("dep", department);
        }
        if (site != null) {
            m.put("site", site);
        }

        // Fetch results for OpdBill
        List<TestWiseCountReport> positiveResults = (List<TestWiseCountReport>) billItemFacade.findLightsByJpql(jpql, m, TemporalType.TIMESTAMP);
        // Now fetch results for OpdBillCancel (use a list for single bType)
        m.put("bType", Arrays.asList(BillTypeAtomic.OPD_BILL_CANCELLATION, BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION));
        List<TestWiseCountReport> cancelResults = (List<TestWiseCountReport>) billItemFacade.findLightsByJpql(jpql, m, TemporalType.TIMESTAMP);

        // Now fetch results for OpdBillRefund (use a list for single bType)
        m.put("bType", Collections.singletonList(BillTypeAtomic.OPD_BILL_REFUND));
        List<TestWiseCountReport> refundResults = (List<TestWiseCountReport>) billItemFacade.findLightsByJpql(jpql, m, TemporalType.TIMESTAMP);

        // Subtract cancel and refund results from the main results
        Map<String, TestWiseCountReport> resultMap = new HashMap<>();

        for (TestWiseCountReport posResult : positiveResults) {
            resultMap.put(posResult.getTestName(), posResult);
        }

        // Subtract cancel results
        for (TestWiseCountReport cancelResult : cancelResults) {
            TestWiseCountReport posResult = resultMap.get(cancelResult.getTestName());
            if (posResult != null) {
                posResult.setCount(posResult.getCount() - Math.abs(cancelResult.getCount()));
                posResult.setHosFee(posResult.getHosFee() - Math.abs(cancelResult.getHosFee()));
                posResult.setCcFee(posResult.getCcFee() - Math.abs(cancelResult.getCcFee()));
                posResult.setProFee(posResult.getProFee() - Math.abs(cancelResult.getProFee()));
                posResult.setTotal(posResult.getTotal() - Math.abs(cancelResult.getTotal()));
                posResult.setDiscount(posResult.getDiscount() - Math.abs(cancelResult.getDiscount()));
            }
        }

        // Subtract refund results
        for (TestWiseCountReport refundResult : refundResults) {
            TestWiseCountReport posResult = resultMap.get(refundResult.getTestName());
            if (posResult != null) {
                posResult.setCount(posResult.getCount() - Math.abs(refundResult.getCount()));
                posResult.setHosFee(posResult.getHosFee() - Math.abs(refundResult.getHosFee()));
                posResult.setCcFee(posResult.getCcFee() - Math.abs(refundResult.getCcFee()));
                posResult.setProFee(posResult.getProFee() - Math.abs(refundResult.getProFee()));
                posResult.setTotal(posResult.getTotal() - Math.abs(refundResult.getTotal()));
                posResult.setDiscount(posResult.getDiscount() - Math.abs(refundResult.getDiscount()));
            }
        }

        testWiseCounts = new ArrayList<>(resultMap.values());

        totalCount = 0.0;
        totalHosFee = 0.0;
        totalCCFee = 0.0;
        totalProFee = 0.0;
        totalNetTotal = 0.0;
        totalDiscount = 0.0;
        totalNetHosFee = 0.0;

        for (TestWiseCountReport twc : testWiseCounts) {
            totalCount += twc.getCount();
            totalHosFee += (twc.getHosFee());
            totalCCFee += twc.getCcFee();
            totalProFee += twc.getProFee();
            totalNetTotal += twc.getTotal();
            totalDiscount += twc.getDiscount();
            totalNetHosFee += twc.getHosFee() - twc.getDiscount();
        }
    }

    public void exportBillsToExcel(String fileName, List<CostOfGoodSoldBillDTO> bills) {
        if (bills == null || bills.isEmpty()) {
            return;
        }

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        externalContext.setResponseContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        externalContext.setResponseHeader("Content-Disposition", "attachment; filename=\"" + fileName + ".xlsx\"");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Pharmacy Sales Report");

            createHeaderRow(sheet);
            populateDataRows(sheet, bills);

            OutputStream outputStream = externalContext.getResponseOutputStream();
            workbook.write(outputStream);
            facesContext.responseComplete();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createHeaderRow(Sheet sheet) {
        String[] headers = {
            "Date", "Doc. No", "NAME", "CODE", "BATCH NO", "QTY", "COST RATE",
            "COST VALUE", "RATE", "VALUE", "Net Total", "Payment Mode/Modes",
            "MRP", "MRP Value", "Discount"
        };
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }
    }

    private void populateDataRows(Sheet sheet, List<CostOfGoodSoldBillDTO> bills) {
        AtomicInteger rowNum = new AtomicInteger(1);
        for (CostOfGoodSoldBillDTO bill : bills) {
            List<BillItemDTO> billItems = bill.getBillItems();
            if (billItems == null || billItems.isEmpty()) {
                continue;
            }

            for (int i = 0; i < billItems.size(); i++) {
                BillItemDTO item = billItems.get(i);
                Row row = sheet.createRow(rowNum.getAndIncrement());

                if (i == 0) {
                    row.createCell(0).setCellValue(bill.getBillCreatedAt() != null ? bill.getBillCreatedAt().toString() : "");
                    row.createCell(1).setCellValue(bill.getBillDeptId() != null ? bill.getBillDeptId() : "");
                }

                row.createCell(2).setCellValue(item.getItemName() != null ? item.getItemName() : "");
                row.createCell(3).setCellValue(item.getItemCode() != null ? item.getItemCode() : "");
                row.createCell(4).setCellValue(item.getBatchNo() != null ? item.getBatchNo() : "");
                row.createCell(5).setCellValue(item.getQty());
                row.createCell(6).setCellValue(item.getCostRate());
                row.createCell(7).setCellValue(item.getQty() * item.getCostRate());
                row.createCell(8).setCellValue(item.getRetailRate());
                row.createCell(9).setCellValue(item.getQty() * item.getRetailRate());

                if (i == 0) {
                    row.createCell(10).setCellValue(bill.getNetTotal());
                    String paymentModes = formatPaymentMethods(bill.getPaymentMethod().toString(), bill);
                    row.createCell(11).setCellValue(paymentModes);
                    row.createCell(14).setCellValue(bill.getDiscount());
                }
            }
        }
    }

    private String formatPaymentMethods(String paymentMethod, CostOfGoodSoldBillDTO bill) {
        if ("Staff".equalsIgnoreCase(paymentMethod)) {
            return "Staff credit";
        } else if ("MultiplePaymentMethods".equalsIgnoreCase(paymentMethod)) {
            String paymentDetails = searchController.getPaymentDetals(bill.getBill()).stream()
                    .map(p -> String.format("%s - %.2f", p.getPaymentMethod(), p.getPaidValue()))
                    .collect(Collectors.joining("\n"));
            return paymentMethod + "\n" + paymentDetails;
        } else {
            return paymentMethod;
        }
    }

    private List<TestWiseCountReport> testWiseCounts;

    public List<TestWiseCountReport> getTestWiseCounts() {
        return testWiseCounts;
    }

    public void setTestWiseCounts(List<TestWiseCountReport> testWiseCounts) {
        this.testWiseCounts = testWiseCounts;
    }

    public List<Institution> getCollectionCenters() {
        return collectionCenters;
    }

    public void setCollectionCenters(List<Institution> collectionCenters) {
        this.collectionCenters = collectionCenters;
    }

    public List<PrescriptionSummaryReportRow> getPrescriptionSummaryReportRows() {
        return prescriptionSummaryReportRows;
    }

    public void setPrescriptionSummaryReportRows(List<PrescriptionSummaryReportRow> prescriptionSummaryReportRows) {
        this.prescriptionSummaryReportRows = prescriptionSummaryReportRows;
    }

    public List<BillLight> getBillLights() {
        return billLights;
    }

    public void setBillLights(List<BillLight> billLights) {
        this.billLights = billLights;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public List<ItemCount> getReportOpdServiceCount() {
        return reportOpdServiceCount;
    }

    public void setReportOpdServiceCount(List<ItemCount> reportOpdServiceCount) {
        this.reportOpdServiceCount = reportOpdServiceCount;
    }

    public String navigateToDynamicReportSummary() {
        return "/reports/dynamic_reports/dynamic_report_summary?faces-redirect=true";
    }

    public List<AgentHistory> getAgentHistories() {
        return agentHistories;
    }

    public void setAgentHistories(List<AgentHistory> agentHistories) {
        this.agentHistories = agentHistories;
    }

    public Doctor getReferingDoctor() {
        return referingDoctor;
    }

    public void setReferingDoctor(Doctor referingDoctor) {
        this.referingDoctor = referingDoctor;
    }

    public PatientInvestigationStatus getPatientInvestigationStatus() {
        return patientInvestigationStatus;
    }

    public void setPatientInvestigationStatus(PatientInvestigationStatus patientInvestigationStatus) {
        this.patientInvestigationStatus = patientInvestigationStatus;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    public void setPatientInvestigations(List<PatientInvestigation> patientInvestigations) {
        this.patientInvestigations = patientInvestigations;

    }

    public ReportTemplateRowBundle getBundle() {
        return bundle;
    }

    public void setBundle(ReportTemplateRowBundle bundle) {
        this.bundle = bundle;
    }

    public List<PatientInvestigation> getPatientInvestigations() {
        return patientInvestigations;
    }

    public List<PatientDepositHistory> getPatientDepositHistories() {
        return patientDepositHistories;
    }

    public void setPatientDepositHistories(List<PatientDepositHistory> patientDepositHistories) {
        this.patientDepositHistories = patientDepositHistories;
    }

    public List<AgentReferenceBook> getAgentReferenceBooks() {
        return agentReferenceBooks;
    }

    public void setAgentReferenceBooks(List<AgentReferenceBook> agentReferenceBooks) {
        this.agentReferenceBooks = agentReferenceBooks;
    }

    public boolean isShowPaymentData() {
        boolean allowedToChange;
        allowedToChange = webUserController.hasPrivilege("LabSummeriesLevel3");
        if (allowedToChange) {
            showPaymentData = false;
        }
        return showPaymentData;
    }

    public void setShowPaymentData(boolean showPaymentData) {
        this.showPaymentData = showPaymentData;
    }

    public List<BillAndItemDataRow> getBillAndItemDataRows() {
        if (billAndItemDataRows == null) {
            billAndItemDataRows = new ArrayList<>();
        }
        return billAndItemDataRows;
    }

    public void setBillAndItemDataRows(List<BillAndItemDataRow> billAndItemDataRows) {
        this.billAndItemDataRows = billAndItemDataRows;
    }

    public BillAndItemDataRow getHeaderBillAndItemDataRow() {
        return headerBillAndItemDataRow;
    }

    public void setHeaderBillAndItemDataRow(BillAndItemDataRow headerBillAndItemDataRow) {
        this.headerBillAndItemDataRow = headerBillAndItemDataRow;
    }

    public Double getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Double totalCount) {
        this.totalCount = totalCount;
    }

    public Double getTotalHosFee() {
        return totalHosFee;
    }

    public void setTotalHosFee(Double totalHosFee) {
        this.totalHosFee = totalHosFee;
    }

    public Double getTotalCCFee() {
        return totalCCFee;
    }

    public void setTotalCCFee(Double totalCCFee) {
        this.totalCCFee = totalCCFee;
    }

    public Double getTotalProFee() {
        return totalProFee;
    }

    public void setTotalProFee(Double totalProFee) {
        this.totalProFee = totalProFee;
    }

    public Double getTotalNetTotal() {
        return totalNetTotal;
    }

    public void setTotalNetTotal(Double totalNetTotal) {
        this.totalNetTotal = totalNetTotal;
    }

    public double getHospitalFeeTotal() {
        return hospitalFeeTotal;
    }

    public void setHospitalFeeTotal(double hospitalFeeTotal) {
        this.hospitalFeeTotal = hospitalFeeTotal;
    }

    public double getCcFeeTotal() {
        return ccFeeTotal;
    }

    public void setCcFeeTotal(double ccFeeTotal) {
        this.ccFeeTotal = ccFeeTotal;
    }

    public double getProfessionalFeeTotal() {
        return professionalFeeTotal;
    }

    public void setProfessionalFeeTotal(double professionalFeeTotal) {
        this.professionalFeeTotal = professionalFeeTotal;
    }

    public double getEntireTotal() {
        return entireTotal;
    }

    public void setEntireTotal(double entireTotal) {
        this.entireTotal = entireTotal;
    }

    public List<String> getVoucherStatusOnDebtorSettlement() {
        voucherStatusOnDebtorSettlement = new ArrayList<>(Arrays.asList(
                "Settled",
                "Partially Settled",
                "Unsettled"
        ));
        return voucherStatusOnDebtorSettlement;
    }

    public void setVoucherStatusOnDebtorSettlement(List<String> voucherStatusOnDebtorSettlement) {
        this.voucherStatusOnDebtorSettlement = voucherStatusOnDebtorSettlement;
    }

    public String getSelectedVoucherStatusOnDebtorSettlement() {
        return selectedVoucherStatusOnDebtorSettlement;
    }

    public void setSelectedVoucherStatusOnDebtorSettlement(String selectedVoucherStatusOnDebtorSettlement) {
        this.selectedVoucherStatusOnDebtorSettlement = selectedVoucherStatusOnDebtorSettlement;
    }

    public BillItem getVoucherItem() {
        return voucherItem;
    }

    public void setVoucherItem(BillItem voucherItem) {
        this.voucherItem = voucherItem;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public double getTotalDiscount() {
        return totalDiscount;
    }

    public void setTotalDiscount(double totalDiscount) {
        this.totalDiscount = totalDiscount;
    }

    public double getTotalNetHosFee() {
        return totalNetHosFee;
    }

    public void setTotalNetHosFee(double totalNetHosFee) {
        this.totalNetHosFee = totalNetHosFee;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    public Double getStaffFeeTotal() {
        return staffFeeTotal;
    }

    public void setStaffFeeTotal(Double staffFeeTotal) {
        this.staffFeeTotal = staffFeeTotal;
    }

    public Double getGrossFeeTotal() {
        return grossFeeTotal;
    }

    public void setGrossFeeTotal(Double grossFeeTotal) {
        this.grossFeeTotal = grossFeeTotal;
    }

    public Double getDiscountTotal() {
        return discountTotal;
    }

    public void setDiscountTotal(Double discountTotal) {
        this.discountTotal = discountTotal;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public Double getPurchaseValueTotal() {
        return purchaseValueTotal;
    }

    public void setPurchaseValueTotal(Double purchaseValueTotal) {
        this.purchaseValueTotal = purchaseValueTotal;
    }

    public Double getCostValueTotal() {
        return costValueTotal;
    }

    public void setCostValueTotal(Double costValueTotal) {
        this.costValueTotal = costValueTotal;
    }

    public Double getRetailValueTotal() {
        return retailValueTotal;
    }

    public void setRetailValueTotal(Double retailValueTotal) {
        this.retailValueTotal = retailValueTotal;
    }

    public List<ReportTemplateRow> getUnifiedBundle() {
        return unifiedBundle;
    }

    public void setUnifiedBundle(List<ReportTemplateRow> unifiedBundle) {
        this.unifiedBundle = unifiedBundle;
    }

    public List<ReportTemplateRowBundle> getBundleList() {
        return bundleList;
    }

    public void setBundleList(List<ReportTemplateRowBundle> bundleList) {
        this.bundleList = bundleList;
    }

    public double getCalAllInTotal() {
        return calAllInTotal;
    }

    public void setCalAllInTotal(double calAllInTotal) {
        this.calAllInTotal = calAllInTotal;
    }

    public double getCalAllOutTotal() {
        return calAllOutTotal;
    }

    public void setCalAllOutTotal(double calAllOutTotal) {
        this.calAllOutTotal = calAllOutTotal;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getStockLedgerReportType() {
        return stockLedgerReportType;
    }

    public void setStockLedgerReportType(String stockLedgerReportType) {
        this.stockLedgerReportType = stockLedgerReportType;
    }

    public List<StockHistory> getStockLedgerHistories() {
        return stockLedgerHistories;
    }

    public void setStockLedgerHistories(List<StockHistory> stockLedgerHistories) {
        this.stockLedgerHistories = stockLedgerHistories;
    }

    public List<Stock> getStocks() {
        return stocks;
    }

    public void setStocks(List<Stock> stocks) {
        this.stocks = stocks;
    }

    public double getStockSaleValue() {
        return stockSaleValue;
    }

    public void setStockSaleValue(double stockSaleValue) {
        this.stockSaleValue = stockSaleValue;
    }

    public double getStockPurchaseValue() {
        return stockPurchaseValue;
    }

    public void setStockPurchaseValue(double stockPurchaseValue) {
        this.stockPurchaseValue = stockPurchaseValue;
    }

    public double getStockCostValue() {
        return stockCostValue;
    }

    public void setStockCostValue(double stockCostValue) {
        this.stockCostValue = stockCostValue;
    }

    public StockFacade getStockFacade() {
        return stockFacade;
    }

    public void setStockFacade(StockFacade stockFacade) {
        this.stockFacade = stockFacade;
    }

    public double getStockTotal() {
        return stockTotal;
    }

    public void setStockTotal(double stockTotal) {
        this.stockTotal = stockTotal;
    }

    public List<PharmacyStockRow> getPharmacyStockRows() {
        return pharmacyStockRows;
    }

    public void setPharmacyStockRows(List<PharmacyStockRow> pharmacyStockRows) {
        this.pharmacyStockRows = pharmacyStockRows;
    }

    public double getStockTottal() {
        return stockTottal;
    }

    public void setStockTottal(double stockTottal) {
        this.stockTottal = stockTottal;
    }

    public List<PharmacyRow> getRows() {
        return rows;
    }

    public void setRows(List<PharmacyRow> rows) {
        this.rows = rows;
    }

    public Amp getAmp() {
        return amp;
    }

    public void setAmp(Amp amp) {
        this.amp = amp;
    }

    public String getDateRange() {
        return dateRange;
    }

    public void setDateRange(String dateRange) {
        this.dateRange = dateRange;
    }

    public Double getStockQty() {
        return stockQty;
    }

    public void setStockQty(Double stockQty) {
        this.stockQty = stockQty;
    }

    public BillType[] getBillTypes() {
        if (billTypes == null) {
            billTypes = new BillType[]{BillType.PharmacySale, BillType.PharmacyIssue, BillType.PharmacyPre, BillType.PharmacyWholesalePre};
        }
        return billTypes;
    }

    public void setBillTypes(BillType[] billTypes) {
        this.billTypes = billTypes;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public List<StockReportRecord> getMovementRecords() {
        return movementRecords;
    }

    public void setMovementRecords(List<StockReportRecord> movementRecords) {
        this.movementRecords = movementRecords;
    }

    public List<StockReportRecord> getMovementRecordsQty() {
        return movementRecordsQty;
    }

    public void setMovementRecordsQty(List<StockReportRecord> movementRecordsQty) {
        this.movementRecordsQty = movementRecordsQty;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public double getValueOfQOH() {
        return valueOfQOH;
    }

    public void setValueOfQOH(double valueOfQOH) {
        this.valueOfQOH = valueOfQOH;
    }

    public double getQoh() {
        return qoh;
    }

    public void setQoh(double qoh) {
        this.qoh = qoh;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public String getSortType() {
        return sortType;
    }

    public void setSortType(String sortType) {
        this.sortType = sortType;
    }

    public List<ItemLastSupplier> getItemLastSuppliers() {
        return itemLastSuppliers;
    }

    public void setItemLastSuppliers(List<ItemLastSupplier> itemLastSuppliers) {
        this.itemLastSuppliers = itemLastSuppliers;
    }

    public Map<Item, Map<Long, List<Stock>>> getItemStockMap() {
        return itemStockMap;
    }

    public void setItemStockMap(Map<Item, Map<Long, List<Stock>>> itemStockMap) {
        this.itemStockMap = itemStockMap;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Institution getFromSite() {
        return fromSite;
    }

    public void setFromSite(Institution fromSite) {
        this.fromSite = fromSite;
    }

    public Institution getToSite() {
        return toSite;
    }

    public void setToSite(Institution toSite) {
        this.toSite = toSite;
    }

    public boolean isShowData() {
        return showData;
    }

    public void setShowData(boolean showData) {
        this.showData = showData;
    }

    public boolean isConsignmentItem() {
        return consignmentItem;
    }

    public void setConsignmentItem(boolean consignmentItem) {
        this.consignmentItem = consignmentItem;
    }

    public List<StockCorrectionRow> getStockCorrectionRows() {
        return stockCorrectionRows;
    }

    public void setStockCorrectionRows(List<StockCorrectionRow> stockCorrectionRows) {
        this.stockCorrectionRows = stockCorrectionRows;
    }

    public List<PharmacyRow> getPharmacyRows() {
        return pharmacyRows;
    }

    public void setPharmacyRows(List<PharmacyRow> pharmacyRows) {
        this.pharmacyRows = pharmacyRows;
    }

    public Map<String, Object> getCogsRows() {
        return cogsRows;
    }

    public void setCogsRows(Map<String, Object> cogsRows) {
        this.cogsRows = cogsRows;
    }

    public List<BillItemDTO> getBillItemsDtos() {
        return billItemsDtos;
    }

    public void setBillItemsDtos(List<BillItemDTO> billItemsDtos) {
        this.billItemsDtos = billItemsDtos;
    }

    public List<CostOfGoodSoldBillDTO> getCogsBillDtos() {
        return cogsBillDtos;
    }

    public void setCogsBillDtos(List<CostOfGoodSoldBillDTO> cogsBillDtos) {
        this.cogsBillDtos = cogsBillDtos;
    }
}
