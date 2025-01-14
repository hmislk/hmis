package com.divudi.bean.report;

import com.divudi.bean.common.DoctorController;
import com.divudi.bean.common.InstitutionController;
import com.divudi.bean.common.ItemApplicationController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.PatientController;
import com.divudi.bean.common.PersonController;
import com.divudi.bean.common.WebUserController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.pharmacy.StockHistoryController;
import com.divudi.data.BillFinanceType;
import com.divudi.data.BillItemStatus;
import com.divudi.data.BillType;
import static com.divudi.data.BillType.PharmacyBhtPre;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.CategoryCount;
import com.divudi.data.DepartmentType;
import com.divudi.data.InstitutionType;
import com.divudi.data.ItemCount;
import com.divudi.data.ItemLight;
import com.divudi.data.PaymentMethod;
import com.divudi.data.PharmacyRow;
import com.divudi.data.ReportTemplateRow;
import com.divudi.data.ReportTemplateRowBundle;
import com.divudi.data.ServiceType;
import com.divudi.data.Sex;
import com.divudi.data.TestWiseCountReport;
import com.divudi.data.dataStructure.BillAndItemDataRow;
import com.divudi.data.dataStructure.ItemDetailsCell;
import com.divudi.data.dataStructure.ItemLastSupplier;
import com.divudi.data.dataStructure.PharmacyStockRow;
import com.divudi.data.dataStructure.StockReportRecord;
import com.divudi.data.lab.PatientInvestigationStatus;
import com.divudi.ejb.PharmacyBean;
import com.divudi.entity.AgentHistory;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Doctor;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientDepositHistory;
import com.divudi.entity.Person;
import com.divudi.entity.PreBill;
import com.divudi.entity.RefundBill;
import com.divudi.entity.Route;
import com.divudi.entity.Service;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.entity.channel.AgentReferenceBook;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.Machine;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.entity.pharmacy.StockHistory;
import com.divudi.facade.AgentHistoryFacade;
import com.divudi.facade.AgentReferenceBookFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.PatientDepositHistoryFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.StockFacade;
import com.divudi.facade.StockHistoryFacade;
import com.divudi.java.CommonFunctions;
import com.divudi.light.common.BillLight;
import com.divudi.light.common.PrescriptionSummaryReportRow;
import java.io.IOException;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.inject.Inject;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javax.faces.context.FacesContext;
import javax.persistence.TemporalType;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.hl7.fhir.r5.model.Bundle;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import com.divudi.facade.ItemFacade;

/**
 *
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
    PatientInvestigationFacade patientInvestigationFacade;
    @Inject
    StockHistoryController stockHistoryController;

    private int reportIndex;
    private Institution institution;
    private Institution site;
    private Department department;
    private Institution fromInstitution;
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

    private CommonFunctions commonFunctions;
    private List<PatientInvestigation> patientInvestigations;
    private PatientInvestigationStatus patientInvestigationStatus;

    private List<AgentReferenceBook> agentReferenceBooks;

    private boolean showPaymentData;

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

    //Constructor
    public PharmacyReportController() {
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
        String jpql = "select new com.divudi.data.ReportTemplateRow("
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
        jpql = "select new com.divudi.data.ReportTemplateRow("
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
        String jpql = "select new com.divudi.data.ReportTemplateRow("
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
        jpql = "select new com.divudi.data.ReportTemplateRow("
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
//        String jpql = "select new com.divudi.data.BillLight(bi.referredBy.person.name, count(bi), count(bi.netTotal)) "
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
                + " com.divudi.light.common.PrescriptionSummaryReportRow(bi.referredBy.person.name, bi.referredBy.person.id, count(bi), sum(bi.netTotal)) "
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
                + " com.divudi.light.common.BillLight(bi.id, bi.deptId, bi.billDate, bi.billTime, "
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
                + " com.divudi.light.common.BillLight(bi.id, bi.deptId, bi.billDate, bi.billTime, bi.patient.person.name, bi.netTotal) "
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

        String jpql = "SELECT new com.divudi.data.ReportTemplateRow("
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
        String jpql = "select new com.divudi.data.ItemCount(bi.item.category.name, bi.item.name, count(bi.item)) "
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
            toDate = commonFunctions.getEndOfDay(new Date());
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

    public void processCollectingCentreTestWiseCountReport() {
        String jpql = "select new  com.divudi.data.TestWiseCountReport("
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

    public void processStockLedgerReport() {

        List<BillTypeAtomic> billTypeAtomics = new ArrayList<>();
        List<BillType> billTypes = new ArrayList<>();

        if ("ipSaleDoc".equals(documentType)) {
            billTypes.add(BillType.PharmacyBhtPre);
        } else if ("opSaleDoc".equals(documentType)) {
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_REFUND);
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
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_ISSUE);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_ISSUE_CANCELLED);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_ISSUE_RETURN);
        } else if ("transferIssueDoc".equals(documentType)) {
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_DIRECT_ISSUE);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_DIRECT_ISSUE_CANCELLED);
        } else if ("transferReceiveDoc".equals(documentType)) {
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_RECEIVE);
            billTypeAtomics.add(BillTypeAtomic.PHARMACY_RECEIVE_CANCELLED);
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

        jpql += " order by s.createdAt ";
        stockLedgerHistories = facade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
    }

    public void processClosingStockReport() {
        switch (reportType) {
            case "itemWise":
                processClosingStockItemWiseReport();
                break;
            case "batchWise":
                processClosingStockBatchWiseReport();
                break;
        }
    }

    public void processClosingStockBatchWiseReport() {
        List<Long> ids;
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder("select MAX(sh.id) "
                + " from StockHistory sh where sh.retired=:ret "
                + "and (sh.itemBatch.item.departmentType is null or sh.itemBatch.item.departmentType = :depty) ");

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

        // Calculate purchase and sale values
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

    }

    public void processClosingStockItemWiseReport() {
        List<Long> ids;
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder("select MAX(sh.id) "
                + " from StockHistory sh where sh.retired=:ret "
                + "and (sh.itemBatch.item.departmentType is null or sh.itemBatch.item.departmentType = :depty) ");

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
            jpql.append("and sh.item.category = :cat ");
            params.put("cat", category);
        }

        System.out.println("amp = " + amp);
        if (amp != null) {
            item = amp;
            System.out.println("item = " + item);
            jpql.append("and sh.item=:itm ");
            params.put("itm", item);
        }

        jpql.append("and sh.createdAt < :et ");
        params.put("et", CommonFunctions.getEndOfDay(toDate));

        jpql.append("group by sh.item ");
        jpql.append("order by sh.item.name");

        System.out.println("jpql.toString() = " + jpql.toString());
        System.out.println("params = " + params);

        ids = getStockFacade().findLongValuesByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

        // Calculate purchase and sale values
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

        jpql += " order by s.id ";
        stocks = stockFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        for (Stock ts : stocks) {
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
        }
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

        if (!fast) {
            sql += "order by  SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.pharmaceuticalBillItem.qty) desc";
        } else {
            sql += "order by  SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.pharmaceuticalBillItem.qty) asc";
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

        if (!fast) {
            sql += "order by  SUM(bi.pharmaceuticalBillItem.qty) desc";
        } else {
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
        String jpql = "select new com.divudi.data.TestWiseCountReport("
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

    
    
}
