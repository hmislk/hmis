package com.divudi.bean.report;

import com.divudi.bean.common.DoctorController;
import com.divudi.bean.common.InstitutionController;
import com.divudi.bean.common.ItemApplicationController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.PatientController;
import com.divudi.bean.common.PersonController;
import com.divudi.bean.common.WebUserController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillItemStatus;
import com.divudi.data.BillType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.CategoryCount;
import com.divudi.data.InstitutionType;
import com.divudi.data.ItemCount;
import com.divudi.data.ItemLight;
import com.divudi.data.PaymentMethod;
import com.divudi.data.ReportTemplateRow;
import com.divudi.data.ReportTemplateRowBundle;
import com.divudi.data.Sex;
import com.divudi.data.TestWiseCountReport;
import com.divudi.data.dataStructure.BillAndItemDataRow;
import com.divudi.data.dataStructure.ItemDetailsCell;
import com.divudi.data.lab.PatientInvestigationStatus;
import com.divudi.entity.AgentHistory;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Doctor;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientDepositHistory;
import com.divudi.entity.Person;
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
import com.divudi.facade.AgentHistoryFacade;
import com.divudi.facade.AgentReferenceBookFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.PatientDepositHistoryFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.java.CommonFunctions;
import com.divudi.light.common.BillLight;
import com.divudi.light.common.PrescriptionSummaryReportRow;
import java.io.IOException;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
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

/**
 *
 * @author Senula Nanayakkara
 */
@Named
@SessionScoped
public class ReportController implements Serializable {

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
    private Machine machine;
    private String processBy;
    private Institution collectingCentre;
    private Route route;
    private Date financialYear;
    private String phn;
    private Doctor referingDoctor;
    private Staff toStaff;
    private WebUser webUser;

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

    private List<String> voucherStatusOnDebtorSettlement;
    private String selectedVoucherStatusOnDebtorSettlement;
    private BillItem voucherItem;

    private String type;
    private String reportType;
    private Speciality speciality;

    public void generateItemMovementByBillReport() {
        billAndItemDataRows = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        // Update JPQL to include all bills, regardless of their status
        StringBuilder jpql = new StringBuilder("SELECT bi FROM BillItem bi WHERE bi.retired=:bir AND bi.bill.retired=:br AND bi.bill.createdAt BETWEEN :fd AND :td");
        params.put("bir", false);
        params.put("br", false);
        params.put("fd", fromDate);
        params.put("td", toDate);

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
        
        for(BillAndItemDataRow bir : billAndItemDataRows){
            netTotal = bir.getGrandTotal();
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

        // Set the calculated values to the bundle.
        crBundle.setCount(totalCount);
        crBundle.setHospitalTotal(totalHospitalFee);
        crBundle.setStaffTotal(totalStaffFee);
        crBundle.setCcTotal(totalCcFee);
        crBundle.setTotal(totalNetValue);
        crBundle.setReportTemplateRows(rows);

        bundle = combineBundles(billedBundle, crBundle);
    }

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

    public void createReferringDoctorWiseRevenueReport() {
        switch (reportType) {
            case "Detail":
                createReferringDoctorWiseRevenueDetailedReport();
                break;
            case "Summary":
                createReferringDoctorWiseRevenueSummaryReport();
                break;
            default:
                createReferringDoctorWiseRevenueDetailedReport();
        }
    }

    public void createReferringDoctorWiseRevenueDetailedReport() {
        String jpql = "select bi"
                + " from BillItem bi"
                + " where bi.retired=:ret"
                + " and bi.bill.referredBy IS NOT NULL";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);

        if (institution != null) {
            jpql += " AND bi.bill.institution = :ins";
            m.put("ins", institution);
        }

        if (site != null) {
            jpql += " AND bi.bill.department.site = :site";
            m.put("site", site);
        }

        if (category != null) {
            jpql += " AND bi.item.category = :cat";
            m.put("cat", category);
        }

        if (investigation != null) {
            jpql += " AND bi.patientInvestigation.investigation = :inv";
            m.put("inv", investigation);
        }

        if (type != null) {
            jpql += " AND bi.bill.ipOpOrCc = :type";
            m.put("type", type);
        }

        if (collectingCentre != null) {
            jpql += " AND bi.bill.collectingCentre = :cc";
            m.put("cc", collectingCentre);
        }

        if (doctor != null) {
            jpql += " AND bi.bill.referredBy = :doc";
            m.put("doc", doctor);
        }

        jpql += " AND bi.createdAt BETWEEN :fromDate AND :toDate";
        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());

        billItems = billItemFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);

        // Initialize totals
        hospitalFeeTotal = 0.0;
        ccFeeTotal = 0.0;
        staffFeeTotal = 0.0;
        grossFeeTotal = 0.0;
        discountTotal = 0.0;
        netTotal = 0.0;

        if (billItems != null) {
            for (BillItem billItem : billItems) {
                hospitalFeeTotal += billItem.getHospitalFee();
                ccFeeTotal += billItem.getCollectingCentreFee();
                staffFeeTotal += billItem.getStaffFee();
                grossFeeTotal += billItem.getGrossValue();
                discountTotal += billItem.getDiscount();
                netTotal += billItem.getNetValue();
            }
        }
    }

    public void createReferringDoctorWiseRevenueSummaryReport() {
        String jpql = "select new com.divudi.data.TestWiseCountReport("
                + "bi.bill.referredBy.person, "
                + "count(bi.item.name), "
                + "sum(bi.hospitalFee), "
                + "sum(bi.collectingCentreFee), "
                + "sum(bi.staffFee), "
                + "sum(bi.discount), "
                + "sum(bi.netValue), "
                + "bi.bill.cancelled, "
                + "bi.bill.refunded "
                + ") "
                + "from BillItem bi "
                + "where bi.retired = :ret ";

        Map<String, Object> params = new HashMap<>();

        // Filters based on the provided conditions
        if (institution != null) {
            jpql += " AND bi.bill.institution = :ins";
            params.put("ins", institution);
        }

        if (site != null) {
            jpql += " AND bi.bill.department.site = :site";
            params.put("site", site);
        }

        if (category != null) {
            jpql += " AND bi.item.category = :cat";
            params.put("cat", category);
        }

        if (investigation != null) {
            jpql += " AND bi.patientInvestigation.investigation = :inv";
            params.put("inv", investigation);
        }

        if (type != null) {
            jpql += " AND bi.bill.ipOpOrCc = :type";
            params.put("type", type);
        }

        if (collectingCentre != null) {
            jpql += " AND bi.bill.collectingCentre = :cc";
            params.put("cc", collectingCentre);
        }

        if (doctor != null) {
            jpql += " AND bi.bill.referredBy = :doc";
            params.put("doc", doctor);
        }

        jpql += " AND bi.bill.createdAt between :fd and :td "
                + " AND bi.bill.referredBy IS NOT NULL "
                + " GROUP BY bi.bill.referredBy.person, bi.bill.cancelled, bi.bill.refunded";

        params.put("ret", false);
        params.put("fd", fromDate);
        params.put("td", toDate);

        List<TestWiseCountReport> rawResults = (List<TestWiseCountReport>) billItemFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);

        // Process results to handle cancellations and refunds
        Map<String, TestWiseCountReport> resultMap = new HashMap<>();

        for (TestWiseCountReport result : rawResults) {
            String doctorName = result.getDoctor().getNameWithTitle();
            TestWiseCountReport existingResult = resultMap.get(doctorName);

            if (existingResult == null) {
                existingResult = result;
            } else {
                existingResult.setCount(existingResult.getCount() + result.getCount());
                existingResult.setHosFee(existingResult.getHosFee() + result.getHosFee());
                existingResult.setCcFee(existingResult.getCcFee() + result.getCcFee());
                existingResult.setProFee(existingResult.getProFee() + result.getProFee());
                existingResult.setTotal(existingResult.getTotal() + result.getTotal());
                existingResult.setDiscount(existingResult.getDiscount() + result.getDiscount());
            }

            // Check if the bill was cancelled or refunded and adjust totals accordingly
            if (result.isCancelled() || result.isRefunded()) {
                existingResult.setCount(existingResult.getCount() - Math.abs(result.getCount()));
                existingResult.setHosFee(existingResult.getHosFee() - Math.abs(result.getHosFee()));
                existingResult.setCcFee(existingResult.getCcFee() - Math.abs(result.getCcFee()));
                existingResult.setProFee(existingResult.getProFee() - Math.abs(result.getProFee()));
                existingResult.setTotal(existingResult.getTotal() - Math.abs(result.getTotal()));
                existingResult.setDiscount(existingResult.getDiscount() - Math.abs(result.getDiscount()));
            }

            resultMap.put(doctorName, existingResult);
        }

        // Convert the map to a list for further processing or reporting
        testWiseCounts = new ArrayList<>(resultMap.values());

        // Initialize totals
        totalCount = 0.0;
        totalHosFee = 0.0;
        totalCCFee = 0.0;
        totalProFee = 0.0;
        totalNetTotal = 0.0;
        totalDiscount = 0.0;
        totalNetHosFee = 0.0;

        // Calculate total values
        for (TestWiseCountReport twc : testWiseCounts) {
            totalCount += twc.getCount();
            totalHosFee += (twc.getHosFee() + twc.getDiscount() + twc.getProFee());
            totalCCFee += twc.getCcFee();
            totalProFee += twc.getProFee();
            totalNetTotal += twc.getTotal();
            totalDiscount += twc.getDiscount();
            totalNetHosFee += twc.getHosFee();
        }
    }

    public double getTotalCredit() {
        return totalCredit;
    }

    public double getTotalDebit() {
        return totalDebit;
    }

    public void processCollectionCenterBalance() {
        bundle = new ReportTemplateRowBundle();
        String jpql;
        Map<String, Object> parameters = new HashMap<>();

        // JPQL query to fetch the last unique AgentHistory for each collecting centre (agency)
        jpql = "select new com.divudi.data.ReportTemplateRow(ah) "
                + " from AgentHistory ah "
                + " where ah.retired <> :ret "
                + " and ah.createdAt < :hxDate "
                + " and ah.agency.institutionType=:insType "
                + " and ah.createdAt = (select max(subAh.createdAt) "
                + " from AgentHistory subAh "
                + " where subAh.retired <> :ret "
                + " and subAh.agency = ah.agency "
                + " and subAh.agency.institutionType=:insType  "
                + " and subAh.createdAt < :hxDate)";

        Date nextDayStart = CommonFunctions.getNextDateStart(getFromDate());

        parameters.put("ret", true);
        parameters.put("hxDate", nextDayStart);  // Ensure this is the first millisecond of the next day
        parameters.put("insType", InstitutionType.CollectingCentre);  // Ensure correct type is passed

        if (collectingCentre != null) {
            jpql += " and ah.agency = :cc";
            parameters.put("cc", collectingCentre);
        }

        // Fetch the results and convert to ReportTemplateRow
        List<ReportTemplateRow> results = (List<ReportTemplateRow>) institutionFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        bundle.setReportTemplateRows(results);
    }

    public void processCurrentCollectionCenterBalance() {
        bundle = new ReportTemplateRowBundle();
        String jpql;
        Map<String, Object> parameters = new HashMap<>();

        // JPQL query to fetch the last unique AgentHistory for each collecting centre (agency)
        jpql = "select new com.divudi.data.ReportTemplateRow(cc) "
                + " from Institution cc "
                + " where cc.retired <> :ret "
                + " and cc.institutionType=:insType ";

        parameters.put("ret", true);
        parameters.put("insType", InstitutionType.CollectingCentre);

        if (collectingCentre != null) {
            jpql += " and cc=:cc";
            parameters.put("cc", collectingCentre);
        }

        // Fetch the results and convert to ReportTemplateRow
        List<ReportTemplateRow> results = (List<ReportTemplateRow>) institutionFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        bundle.setReportTemplateRows(results);
    }

    public void processCollectingCentreBook() {
        String sql;
        HashMap m = new HashMap();
        sql = "select a from AgentReferenceBook a "
                + " where a.retired=false "
                + " and a.deactivate=false "
                + " and a.createdAt between :fd and :td "
                + " and a.fullyUtilized=false ";

        if (collectingCentre != null) {
            sql += "and a.institution=:ins order by a.id";
            m.put("ins", collectingCentre);
        }
        m.put("fd", fromDate);
        m.put("td", toDate);
        
        agentReferenceBooks = agentReferenceBookFacade.findByJpql(sql, m,TemporalType.TIMESTAMP);

    }

    public void createDebtorSettlement() {
        String jpql = "SELECT cb "
                + "FROM Bill cb "
                + "WHERE cb.retired = :ret "
                + "AND cb.paymentMethod = :pm "
                + "AND cb.billType in :bts ";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("pm", PaymentMethod.Credit);
        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.OpdBill);
        m.put("bts", bts);

        if (institution != null) {
            jpql += "AND cb.creditCompany = : cc ";
            m.put("cc", institution);
        }

        jpql += "AND cb.createdAt BETWEEN :fromDate AND :toDate";
        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());

        bills = billFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
        if (selectedVoucherStatusOnDebtorSettlement != null) {
            // Filter the bills list based on the statusFilter
            bills = filterBillsByStatus(bills, selectedVoucherStatusOnDebtorSettlement);
        }
        netTotal = 0.0;

        for (Bill b : bills) {
            BillItem voucher = findVoucherIsAvailable(b);  // Call it once per loop
            if (voucher != null) {
                netTotal += voucher.getBill().getNetTotal();
            }
        }
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

        if (bill.getNetTotal() < voucher.getBill().getNetTotal()) {
            return "Partially Settled";
        }

        return "Unsettled"; // Default case for safety
    }

    public BillItem findVoucherIsAvailable(Bill b) {
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

        voucherItem = billItemFacade.findFirstByJpql(jpql, m);
        return voucherItem;
    }

    public void processPettyCashPayment() {
        String jpql = "SELECT pc "
                + "FROM Bill pc "
                + "WHERE pc.retired = :ret "
                + "AND pc.billType = :bt ";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("bt", BillType.PettyCash);

        if (toDepartment != null) {
            jpql += " AND pc.toDepartment=:dpt ";
            m.put("dpt", toDepartment);
        }

        if (toStaff != null) {
            jpql += " AND pc.staff=:st ";
            m.put("st", toStaff);
        }

        if (institution != null) {
            jpql += " AND pc.institution=:ins ";
            m.put("ins", institution);
        }

        if (site != null) {
            jpql += " AND pc.department.site=:site ";
            m.put("site", site);
        }
        
        if (department != null) {
            jpql += " AND pc.department=:dept ";
            m.put("dept", department);
        }

        if (webUser != null) {
            jpql += " AND pc.creater=:wu ";
            m.put("wu", webUser);
        }

        jpql += "AND pc.createdAt BETWEEN :fromDate AND :toDate";
        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());

        bills = billFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);

        netTotal = 0.0;

        for (Bill b : bills) {
            netTotal += b.getTotal();
        }
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

    private Person person;

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

    public void makeNull() {
        doctor = null;
        prescriptionSummaryReportRows = null;
        fromDate = null;
        toDate = null;
        person = null;
    }

    public void processCollectingCentreBillWiseDetailReport() {
        String jpql = "select bill "
                + " from Bill bill "
                + " where bill.retired=:ret"
                + " and bill.billDate between :fd and :td "
                + " and bill.billType = :bType";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bType", BillType.CollectingCentreBill);

        if (route != null) {
            jpql += " and bill.fromInstitution.route = :route ";
            m.put("route", route);
        }

        if (institution != null) {
            jpql += " and bill.institution = :ins ";
            m.put("ins", institution);
        }

        if (collectingCentre != null) {
            jpql += " and bill.fromInstitution = :cc ";
            m.put("cc", collectingCentre);
        }

        if (toDepartment != null) {
            jpql += " and bill.toDepartment = :dep ";
            m.put("dep", toDepartment);
        }

        if (phn != null && !phn.isEmpty()) {
            jpql += " and bill.patient.phn = :phn ";
            m.put("phn", phn);
        }

        if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
            jpql += " and bill.deptId = :inv ";
            m.put("inv", invoiceNumber);
        }

//        if (itemLight != null) {
//            jpql += " and bi.item.id = :item ";
//            m.put("item", itemLight.getId());
//        }
        if (doctor != null) {
            jpql += " and bill.referredBy = :refDoc ";
            m.put("refDoc", doctor);
        }

//        if (status != null) {
//            jpql += " and billItemStatus = :status ";
//            m.put("status", status);
//        }
        bills = billFacade.findByJpql(jpql, m);
    }

    public ReportController() {
    }

    // Modified by Dr M H B Ariyaratne with assistance from ChatGPT from OpenAI.
    public void processLabTestCount() {
        String jpql = "select new com.divudi.data.ItemCount(bi.item.category.name, bi.item.name, count(bi.item)) "
                + " from BillItem bi "
                + " where bi.bill.cancelled=:can "
                + " and bi.bill.billDate between :fd and :td "
                + " and TYPE(bi.item) = Investigation ";
        Map<String, Object> m = new HashMap<>();
        m.put("can", false);
        m.put("fd", fromDate);
        m.put("td", toDate);

        if (fromInstitution != null) {
            jpql += " and bi.bill.fromInstitution=:fi ";
            m.put("fi", fromInstitution);
        }

        if (toInstitution != null) {
            jpql += " and bi.bill.toInstitution=:ti ";
            m.put("ti", toInstitution);
        }

        if (fromDepartment != null) {
            jpql += " and bi.bill.fromDepartment=:fdept ";
            m.put("fdept", fromDepartment);
        }

        if (machine != null) {
            jpql += " and bi.item.machine=:machine ";
            m.put("machine", machine);
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

    public void filterOpdServiceCountBySelectedService(Long selectedItemId) {
        if (selectedItemId != null) {
            item = itemController.findItem(selectedItemId);
            doctor = null;
        }
        processOpdServiceCountDoctorWise();
    }

    public void filterOpdServiceCountBySelectedDoctor(Long selectedDoctorId) {
        if (selectedDoctorId != null) {
            doctor = doctorController.findDoctor(selectedDoctorId);
            item = null;
        }
        processOpdServiceCountDoctorWise();
    }

    public void processOpdServiceCountDoctorWise() {
        List<BillTypeAtomic> billtypes = new ArrayList<>();
        billtypes.add(BillTypeAtomic.OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER);

        String jpql = "select new com.divudi.data.ItemCount(bi.bill.fromStaff.person.name, bi.bill.fromStaff.id, bi.item.name, bi.item.id, count(bi)) "
                + " from BillItem bi "
                + " where bi.bill.cancelled=:can "
                + " and bi.bill.billTypeAtomic IN :bt"
                + " and bi.bill.billDate between :fd and :td ";

        Map<String, Object> m = new HashMap<>();
        m.put("can", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", billtypes);
        if (department != null) {
            jpql += " and bi.bill.department=:fdept ";
            m.put("fdept", department);
        }
        if (doctor != null) {
            jpql += " and bi.bill.fromStaff =:fs";
            m.put("fs", doctor);
        }
        if (item != null) {
            jpql += " and bi.item =:it";
            m.put("it", item);
        }

        jpql += " group by bi.item, bi.bill.fromStaff ";
        jpql += " order by bi.bill.fromStaff.person.name, bi.item.name";
        reportOpdServiceCount = (List<ItemCount>) billItemFacade.findLightsByJpql(jpql, m);
    }

    public void processCollectingCentreReportsToPrint() {
        String jpql = "select bi "
                + " from BillItem bi "
                + " where bi.retired=:ret"
                + " and bi.bill.billDate between :fd and :td "
                + " and bi.bill.billType = :bType";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bType", BillType.CollectingCentreBill);

        if (route != null) {
            jpql += " and bi.bill.fromInstitution.route = :route ";
            m.put("route", route);
        }

        if (institution != null) {
            jpql += " and bi.bill.institution = :ins ";
            m.put("ins", institution);
        }

        if (collectingCentre != null) {
            jpql += " and bi.bill.collectingCentre = :cc ";
            m.put("cc", collectingCentre);
        } else {
            jpql += " and bi.bill.collectingCentre is not null ";
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

        billItems = billItemFacade.findByJpql(jpql, m);
    }

    public void listCcReportPrint() {
        String jpql;
        Map<String, Object> params = new HashMap<>();

        jpql = "SELECT i "
                + " FROM PatientInvestigation i "
                + " WHERE i.retired = :ret ";

        jpql += " AND i.billItem.bill.createdAt BETWEEN :fd AND :td ";
        params.put("fd", getFromDate());
        params.put("td", getToDate());

        if (institution != null) {
            jpql += " AND i.billItem.bill.creater.institution.name = :orderedInstitution ";
            params.put("orderedInstitution", institution.getName());
        }

        if (department != null) {
            jpql += " AND i.billItem.bill.department = :orderedDepartment ";
            params.put("orderedDepartment", department);
        }

        if (site != null) {
            jpql += " AND i.billItem.bill.department.site=:site";
            params.put("site", site);
        }

        if (toInstitution != null) {
            jpql += " AND i.performInstitution = :peformingInstitution ";
            params.put("peformingInstitution", toInstitution);
        }

        if (toDepartment != null) {
            jpql += " AND i.performDepartment = :peformingDepartment ";
            params.put("peformingDepartment", toDepartment);
        }

        if (collectingCentre != null) {
            jpql += " AND (i.billItem.bill.collectingCentre = :collectionCenter OR i.billItem.bill.fromInstitution = :collectionCenter) ";
            params.put("collectionCenter", collectingCentre);
        } else {
            jpql += " AND (i.billItem.bill.collectingCentre is not null OR i.billItem.bill.fromInstitution.institutionType=:ccType) ";
            params.put("ccType", InstitutionType.CollectingCentre);
        }

        if (route != null) {
            jpql += " AND (i.billItem.bill.collectingCentre.route = :route OR i.billItem.bill.fromInstitution.route = :route) ";
            params.put("route", getRoute());
        }

        if (phn != null && !phn.trim().isEmpty()) {
            jpql += " AND i.billItem.bill.patient.phn=:phn ";
            params.put("phn", phn);
        }

        if (doctor != null) {
            jpql += " AND i.billItem.bill.referredBy.person.name = :referringDoctor ";
            params.put("referringDoctor", doctor.getPerson().getName());
        }

        if (investigation != null) {
            jpql += " AND i.investigation = :investigation ";
            params.put("investigation", getInvestigation());
        }

        if (category != null) {
            jpql += " AND i.investigation.category = :cat ";
            params.put("cat", category);
        }

        if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
            jpql += " AND i.billItem.bill.deptId = :iNo ";
            params.put("iNo", invoiceNumber);
        }

        if (patientInvestigationStatus != null) {
            jpql += " AND i.status = :patientInvestigationStatus ";
            params.put("patientInvestigationStatus", getPatientInvestigationStatus());
        }

        jpql += " ORDER BY i.id DESC";

        params.put("ret", false);

        patientInvestigations = patientInvestigationFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    public void processCollectingCentreStatementReportNew() {

        String jpql = "select ah "
                + " from AgentHistory ah "
                + " where ah.retired=:ret"
                + " and ah.createdAt between :fd and :td ";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("fd", fromDate);
        m.put("td", toDate);

        if (collectingCentre != null) {
            jpql += " and ah.agency = :cc ";
            m.put("cc", collectingCentre);
        }

        if (institution != null) {
            jpql += " and ah.bill.institution = :ins ";
            m.put("ins", institution);
        }

        if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
            jpql += " and (ah.bill.insId = :inv or ah.bill.deptId = :inv) ";
            m.put("inv", invoiceNumber);
        }
        agentHistories = agentHistoryFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
    }

    public void processCollectingCentreStatementReport() {
        String jpql = "select bi "
                + " from BillItem bi "
                + " where bi.retired=:ret"
                + " and bi.bill.billDate between :fd and :td "
                + " and bi.bill.billType = :bType";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bType", BillType.CollectingCentreBill);

//        if (route != null) {
//            jpql += " and bi.bill.fromInstitution.route = :route ";
//            m.put("route", route);
//        }
        if (institution != null) {
            jpql += " and bi.bill.institution = :ins ";
            m.put("ins", institution);
        }

        if (collectingCentre != null) {
            jpql += " and bi.bill.fromInstitution = :cc ";
            m.put("cc", collectingCentre);
        }

//        if (toDepartment != null) {
//            jpql += " and bi.bill.toDepartment = :dep ";
//            m.put("dep", toDepartment);
//        }
//
//        if (phn != null && !phn.isEmpty()) {
//            jpql += " and bi.bill.patient.phn = :phn ";
//            m.put("phn", phn);
//        }
        if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
            jpql += " and bi.bill.deptId = :inv ";
            m.put("inv", invoiceNumber);
        }

//        if (itemLight != null) {
//            jpql += " and bi.item.id = :item ";
//            m.put("item", itemLight.getId());
//        }
//
//        if (doctor != null) {
//            jpql += " and bi.bill.referredBy = :refDoc ";
//            m.put("refDoc", doctor);
//        }
//
//        if (status != null) {
//            jpql += " and bi.billItemStatus = :status ";
//            m.put("status", status);
//        }
        billItems = billItemFacade.findByJpql(jpql, m);
    }

//    
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

    public void processOpdServiceCount() {
        String jpql = "select new com.divudi.data.ItemCount(bi.item.category.name, bi.item.name, count(bi.item)) "
                + " from BillItem bi "
                + " where bi.bill.cancelled=:can "
                + " and bi.bill.billDate between :fd and :td "
                + " and bi.bill.billType=:bitype ";
        Map<String, Object> m = new HashMap<>();

        m.put("can", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bitype", BillType.OpdBill);

        if (fromInstitution != null) {
            jpql += " and bi.bill.fromInstitution=:fi ";
            m.put("fi", fromInstitution);
        }

        if (fromDepartment != null) {
            jpql += " and bi.bill.fromDepartment=:fdept ";
            m.put("fdept", fromDepartment);
        }

        if (toInstitution != null) {
            jpql += " and bi.bill.toInstitution=:ti ";
            m.put("ti", toInstitution);
        }

        if (toDepartment != null) {
            jpql += " and bi.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
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

    public void processCollectingCentreTransactionReport() {
        String jpql = "select bill "
                + " from Bill bill "
                + " where bill.retired=:ret"
                + " and bill.billDate between :fd and :td "
                + " and bill.billType = :bType";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bType", BillType.CollectingCentreBill);

        if (route != null) {
            jpql += " and bill.fromInstitution.route = :route ";
            m.put("route", route);
        }

        if (institution != null) {
            jpql += " and bill.institution = :ins ";
            m.put("ins", institution);
        }

        if (collectingCentre != null) {
            jpql += " and bill.fromInstitution = :cc ";
            m.put("cc", collectingCentre);
        }

        if (toDepartment != null) {
            jpql += " and bill.toDepartment = :dep ";
            m.put("dep", toDepartment);
        }

        if (phn != null && !phn.isEmpty()) {
            jpql += " and bill.patient.phn = :phn ";
            m.put("phn", phn);
        }

        if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
            jpql += " and bill.deptId = :inv ";
            m.put("inv", invoiceNumber);
        }

//        if (itemLight != null) {
//            jpql += " and bi.item.id = :item ";
//            m.put("item", itemLight.getId());
//        }
        if (doctor != null) {
            jpql += " and bill.referredBy = :refDoc ";
            m.put("refDoc", doctor);
        }

//        if (status != null) {
//            jpql += " and billItemStatus = :status ";
//            m.put("status", status);
//        }
        bills = billFacade.findByJpql(jpql, m);
    }

    public void processCollectingCentreAgentHistory() {
        String jpql = "select ah "
                + " from AgentHistory ah "
                + " where ah.retired=:ret"
                + " and ah.createdAt between :fd and :td ";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("fd", fromDate);
        m.put("td", toDate);

        if (collectingCentre != null) {
            jpql += " and ah.agency = :cc ";
            m.put("cc", collectingCentre);
        }

        agentHistories = agentHistoryFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
    }

    public void processCollectingCentreReciptReport() {
        bundle = new ReportTemplateRowBundle();
        List<BillTypeAtomic> billtypes = new ArrayList<>();
        billtypes.add(BillTypeAtomic.CC_PAYMENT_MADE_BILL);
        billtypes.add(BillTypeAtomic.CC_PAYMENT_MADE_CANCELLATION_BILL);
        billtypes.add(BillTypeAtomic.CC_PAYMENT_RECEIVED_BILL);
        billtypes.add(BillTypeAtomic.CC_PAYMENT_CANCELLATION_BILL);

        String jpql = "select new com.divudi.data.ReportTemplateRow(bill) "
                + " from Bill bill "
                + " where bill.retired=:ret"
                + " and bill.billDate between :fd and :td "
                + " and bill.billTypeAtomic in :bTypes";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bTypes", billtypes);

        if (site != null) {
            jpql += " and bill.department.site = :route ";
            m.put("route", site);
        }

        if (route != null) {
            jpql += " and bill.fromInstitution.route = :route ";
            m.put("route", route);
        }

        if (institution != null) {
            jpql += " and bill.institution = :ins ";
            m.put("ins", institution);
        }

        if (collectingCentre != null) {
            jpql += " and bill.fromInstitution = :cc ";
            m.put("cc", collectingCentre);
        }

        if (toDepartment != null) {
            jpql += " and bill.toDepartment = :tdep ";
            m.put("tdep", toDepartment);
        }

        if (department != null) {
            jpql += " and bill.department = :dep ";
            m.put("dep", department);
        }

        if (phn != null && !phn.isEmpty()) {
            jpql += " and bill.patient.phn = :phn ";
            m.put("phn", phn);
        }

        if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
            jpql += " and bill.deptId = :inv ";
            m.put("inv", invoiceNumber);
        }

        if (doctor != null) {
            jpql += " and bill.referredBy = :refDoc ";
            m.put("refDoc", doctor);
        }

        bundle.setReportTemplateRows((List<ReportTemplateRow>) billFacade.findLightsByJpql(jpql, m));
        bundle.calculateTotalByBills();
    }

    public void downloadLabTestCount() {
        Workbook workbook = exportToExcel(reportList, "Test Count");
        FacesContext fc = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
        response.reset();
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=test_counts.xlsx");

        try (ServletOutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
            fc.responseComplete();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public void downloadOpdServiceCount() {
        Workbook workbook = exportToExcel(reportList, "Opd Service Count");
        FacesContext fc = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
        response.reset();
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=service_count.xlsx");

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

    public String navigateToAssetRegister() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }

        return "/reports/assets/asset_register?faces-redirect=true";
    }

    public String navigateToLabReportsTestCount() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/test_count?faces-redirect=true";
    }

    public String navigateToLabPeakHourStatistics() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/peak_hour_statistics?faces-redirect=true";
    }

    public String navigateToLabInvetigationWiseReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/investigation_wise_report?faces-redirect=true";
    }

    public String navigateToExternalLaborataryWorkloadReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/external_laboratary_workload?faces-redirect=true";
    }

    public String navigateToInvestigationMonthEndSummery() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/investigation_month_end_summery?faces-redirect=true";

    }

    public String navigateToInvestigationMonthEndDetails() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/investigation_month_end_detailed?faces-redirect=true";

    }

    public String navigateToLabOrganismAntibioticSensitivityReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/organism_antibiotic_sensitivity?faces-redirect=true";
    }

    public String navigateToLabRegisterReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/lab_register?faces-redirect=true";
    }

    public String navigateToTurnAroundTimeDetails() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/turn_around_time_details?faces-redirect=true";
    }

    public String navigateToTestWiseCountReports() {
        return "/reports/lab/test_wise_count_report?faces-redirect=true";
    }

    public String navigateToLabBillItemList() {
        return "/reports/lab/lab_bill_item_list?faces-redirect=true";
    }

    public String navigateToAnnualTestStatistics() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/annual_test_statistics?faces-redirect=true";
    }

    public String navigateToPoStatusReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }

        return "/reports/assets/po_status_report?faces-redirect=true";
    }

    public String navigateToEmployeeAssetIssue() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/assets/employee_asset_issue?faces-redirect=true";
    }

    public String navigateToFixedAssetIssue() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/assets/fixed_asset_issue?faces-redirect=true";
    }

    public String navigateToAssetWarentyExpireReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/assets/asset_warranty_expire_report?faces-redirect=true";
    }

    public String navigateToAssetGrnReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/assets/asset_grn_report?faces-redirect=true";
    }

    public String navigateToAssetTransferReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/assets/assest_transfer_report?faces-redirect=true";

    }

    public String navigateToAssetAmcExpiryReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/assets/assest_amc_expiry_report?faces-redirect=true";

    }

    public String navigateToAssetAmcReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/assets/amc_report?faces-redirect=true";

    }

    public String navigateToTurnAroundTimeHourly() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/turn_around_time_hourly?faces-redirect=true";

    }

    public String navigateToCollectionCenterStatement() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/collection_center_statement?faces-redirect=true";

    }

    public String navigateToManagementAdmissionCountReport() {

        return "/reports/managementReports/referring_doctor_wise_revenue?faces-redirect=true";
    }

    public String navigateToReferringDoctorWiseRevenue() {

        return "/reports/managementReports/re?faces-redirect=true";
    }

    public String navigateToSurgeryWiseCount() {

        return "/reports/managementReports/surgery_wise_count?faces-redirect=true";
    }

    public String navigateToSurgeryCountDoctorWise() {

        return "/reports/managementReports/surgery_count_doctor_wise?faces-redirect=true";
    }

    public String navigateToLeaveReport() {

        return "/reports/HRReports/leave_report?faces-redirect=true";
    }

    public String navigateToLeaveReportSummery() {

        return "/reports/HRReports/leave_report_summery?faces-redirect=true";
    }

    public String navigateToLateLeaveDetails() {

        return "/reports/HRReports/late_leave_details?faces-redirect=true";
    }

    public String navigateToLeaveSummeryReport() {

        return "/reports/HRReports/leave_summery_report?faces-redirect=true";
    }

    public String navigateToDepartmentReports() {

        return "/reports/HRReports/department_report?faces-redirect=true";
    }

    public String navigateToEmployeeDetails() {

        return "/reports/HRReports/employee_details?faces-redirect=true";
    }

    public String navigateToEmployeeToRetired() {

        return "/reports/HRReports/employee_to_retired?faces-redirect=true";
    }

    public String navigateToStaffShiftReport() {

        return "/reports/HRReports/staff_shift_report?faces-redirect=true";
    }

    public String navigateToRosterTimeAndVerifyTime() {

        return "/reports/HRReports/rosterTabel_verify_time?faces-redirect=true";
    }

    public String navigateToEmployeeEndofProbation() {

        return "/reports/HRReports/employee_end_of_probation?faces-redirect=true";
    }

    public String navigateToAttendanceReport() {

        return "/reports/HRReports/attendance_report?faces-redirect=true";
    }

    public String navigateToLateInAndEarlyOut() {

        return "/reports/HRReports/late_in_and_early_out?faces-redirect=true";
    }

    public String navigateToStaffShiftDetailsByStaff() {

        return "/reports/HRReports/staff_shift_details_by_staff?faces-redirect=true";
    }

    public String navigateToVerifiedReport() {

        return "/reports/HRReports/verified_report?faces-redirect=true";
    }

    public String navigateToHeadCountReport() {

        return "/reports/HRReports/head_count?faces-redirect=true";
    }

    public String navigateToFingerPrintRecordByLogged() {

        return "/reports/HRReports/fingerprint_record_by_logged?faces-redirect=true";
    }

    public String navigateToFingerPrintRecordByVerified() {

        return "/reports/HRReports/fingerprint_record_by_verified?faces-redirect=true";
    }

    public String navigateToFingerPrintRecordNoShiftSettled() {

        return "/reports/HRReports/fingerprint_record_no_shift_settled?faces-redirect=true";
    }

    public String navigateToEmployeeWorkedDayReport() {

        return "/reports/HRReports/employee_worked_day_report?faces-redirect=true";
    }

    public String navigateToEmployeeWorkedDayReportSalaryCycle() {

        return "/reports/HRReports/employee_worked_day_report_salary_cycle?faces-redirect=true";
    }

    public String navigateToMonthendEmployeeWorkingTimeAndOvertime() {

        return "/reports/HRReports/monthend_employee_working_Time_and_overtime?faces-redirect=true";
    }

    public String navigateToMonthEndEmployeeNoPayReportByMinutes() {

        return "/reports/HRReports/month_end_employee_nopay_report_by_minutes?faces-redirect=true";
    }

    public String navigateToMonthEndEmployeeSummery() {

        return "/reports/HRReports/month_end_employee_summery?faces-redirect=true";
    }

    public String navigateToFingerAnalysisReportBySalaryCycle() {

        return "/reports/HRReports/finger_analysis_report_by_salary_cycle?faces-redirect=true";
    }

    public String navigateToFingerPrintApprove() {

        return "/reports/HRReports/fingerprint_approve?faces-redirect=true";
    }

    public String navigateToStaffPayrollAccountant() {
        return "/reports/salary_reports/staff_payroll_accountant?faces-redirect=true?faces-redirect=true";
    }

    public String navigateToNopayandSalaryAllowanceReport() {
        return "/reports/salary_reports/nopay_and_salary_allowance_report?faces-redirect=true";
    }

    public String navigateToStaffSalaryBankWise() {
        return "/reports/salary_reports/staff_salary_bank_wise?faces-redirect=true";
    }

    public String navigateToEPF() {
        return "/reports/salary_reports/EPF?faces-redirect=true";
    }

    public String navigateToETF() {
        return "/reports/salary_reports/ETF?faces-redirect=true";
    }

    public String navigateToLeaveForm() {

        return "/reports/HRReports/leave_form?faces-redirect=true";
    }

    public String navigateToAdditionalFormReportVerification() {

        return "/reports/HRReports/additional_form_report_veification?faces-redirect=true";
    }

    public String navigateToOnlineFormStatus() {

        return "/reports/HRReports/online_form_status?faces-redirect=true";
    }

    public String navigateToAdmissionDischargeReport() {

        return "/reports/inpatientReports/admission_discharge_report?faces-redirect=true";
    }

    public String navigateToGoodInTransit() {

        return "/reports/inventoryReports/good_in_transit?faces-redirect=true";
    }

    public String navigateToGrnReport() {

        return "/reports/inventoryReports/grn_report?faces-redirect=true";
    }

    public String navigateToSlowFastNoneMovement() {

        return "/reports/inventoryReports/slow_fast_none_movement?faces-redirect=true";
    }

    public String navigateToBeforeStockTaking() {

        return "/reports/inventoryReports/before_stock_taking?faces-redirect=true";
    }

    public String navigateToAfterStockTaking() {

        return "/reports/inventoryReports/after_stock_taking?faces-redirect=true";
    }

    public String navigateToStockLedger() {

        return "/reports/inventoryReports/stock_ledger?faces-redirect=true";
    }

    public String navigateToExpiryItem() {

        return "/reports/inventoryReports/expiry_item?faces-redirect=true";
    }

    public String navigateToIpUnsettledInvoices() {

        return "/reports/inpatientReports/ip_unsettled_invoices?faces-redirect=true";
    }

    public String navigateToconsumption() {

        return "/reports/inventoryReports/consumption?faces-redirect=true";
    }

    public String navigateToClosingStockReport() {

        return "/reports/inventoryReports/closing_stock_report?faces-redirect=true";
    }

    public String navigateToAdmissionCategoryWiseAdmission() {

        return "/reports/inpatientReports/admission_category_wise_admission?faces-redirect=true";
    }

    public String navigateToStockTransferReport() {

        return "/reports/inventoryReports/stock_transfer_report?faces-redirect=true";
    }

    public String navigateToCostOfGoodsSold() {

        return "/reports/inventoryReports/cost_of_goods_sold?faces-redirect=true";
    }

    public String navigateToDiscount() {

        return "/reports/financialReports/discount?faces-redirect=true";
    }

    public String navigateToOutsidePayment() {

        return "/reports/financialReports/outside_payment?faces-redirect=true";
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
        m.put("bType", Collections.singletonList(BillTypeAtomic.OPD_BILL_CANCELLATION));
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
            totalHosFee += (twc.getHosFee() + twc.getDiscount());
            totalCCFee += twc.getCcFee();
            totalProFee += twc.getProFee();
            totalNetTotal += twc.getTotal();
            totalDiscount += twc.getDiscount();
            totalNetHosFee += twc.getHosFee();
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

}
