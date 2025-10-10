/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.AuditEventApplicationController;
import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.ReportTimerController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.reports.DisbursementReports;
import com.divudi.core.data.reports.PharmacyReports;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.dto.PharmacyItemPurchaseDTO;
import com.divudi.core.data.dataStructure.StockReportRecord;
import com.divudi.core.data.inward.SurgeryBillType;
import com.divudi.core.data.table.String1Value3;
import com.divudi.core.data.table.String2Value4Transfer;

import com.divudi.ejb.PharmacyBean;
import com.divudi.core.entity.AuditEvent;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.BillService;
import com.divudi.core.data.dto.PharmacyTransferIssueBillDTO;
import com.divudi.core.data.dto.PharmacyTransferReceiveDTO;
import com.divudi.core.data.dto.PharmacyTransferIssueBillItemDTO;
import com.divudi.core.data.dto.PharmacyTransferReceiveBillItemDTO;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.joda.time.Days;
import org.joda.time.LocalDate;

/**
 * @author Buddhika
 */
@Named
@SessionScoped
public class ReportsTransfer implements Serializable {

    @Inject
    private ReportTimerController reportTimerController;

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private StockFacade stockFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFacade BillFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    BillService billService;
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    private BillBeanController billBeanController;
    @Inject
    private PharmacyController pharmacyController;
    @Inject
    private AuditEventApplicationController auditEventApplicationController;
    @Inject
    private SessionController sessionController;
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    private Department fromDepartment;
    private Department toDepartment;
    private Department department;
    private Date fromDate;
    private Date toDate;
    private BillType[] billTypes;

    private Institution institution;
    private List<Stock> stocks;
    private List<ItemCount> itemCounts;
    private List<ItemCountWithOutMargin> itemCountWithOutMargins;
    private double saleValue;
    private double purchaseValue;
    private double totalsValue;
    private double discountsValue;
    private double marginValue;
    private double netTotalValues;
    private double netTotalSaleValues;
    private double netTotalPurchaseValues;
    private double netTotalCostValues;
    private double retailValue;
    private double costValue;
    private double transferValue;
    private Category category;

    private List<BillItem> transferItems;
    private List<Bill> transferBills;
    private List<PharmacyTransferIssueBillDTO> transferIssueDtos; // DTO for efficient display
    private List<PharmacyTransferReceiveDTO> transferReceiveDtos; // DTO for efficient display
    private List<PharmacyTransferIssueBillItemDTO> transferIssueBillItemDtos; // DTO for item level display
    private List<PharmacyTransferReceiveBillItemDTO> transferReceiveBillItemDtos; // DTO for receive item level display
    
    // DTO lists for disposal reports
    private List<PharmacyItemPurchaseDTO> disposalIssueBillDtos;
    private List<PharmacyItemPurchaseDTO> disposalIssueBillItemDtos;
    
    private List<ItemBHTIssueCountTrancerReciveCount> itemBHTIssueCountTrancerReciveCounts;

    // Configuration for DTO vs Entity approach - true by default for better performance
    private boolean useDtoApproach = true;

    /**
     * Determines if DTO version of reports should be available in navigation
     * This method can be used in navigation to conditionally show DTO buttons
     */
    public boolean isTransferIssueDtoEnabled() {
        // For now, return true to enable DTO version by default
        // In future, this could check a configuration option
        return true;
    }

    private List<StockReportRecord> movementRecords;
    private List<StockReportRecord> movementRecordsQty;
    private double stockPurchaseValue;
    private double stockSaleValue;
    private double valueOfQOH;
    private double qoh;
    private double totalIssueQty;
    private double totalBHTIssueQty;
    private double totalIssueValue;
    private double totalBHTIssueValue;
    private int pharmacyDisbursementReportIndex = 9;
    private AdmissionType admissionType;
    private Bill previewBill;

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Constructions">
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Navigation Methods">
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Functions">
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    // </editor-fold>
    public String navigateToTransferIssueByBillItem() {
        transferBills = null;
        pharmacyController.setManagePharamcyReportIndex(pharmacyDisbursementReportIndex);
        return "/pharmacy/reports/disbursement_reports/pharmacy_report_transfer_issue_bill_item?faces-redirect=true";
    }

    public String navigateToTransferReceiveByBillItem() {
        transferBills = null;
        pharmacyController.setManagePharamcyReportIndex(pharmacyDisbursementReportIndex);
        return "/pharmacy/reports/disbursement_reports/pharmacy_report_transfer_receive_bill_item?faces-redirect=true";
    }

    public String navigateToTransferIssueByBill() {
        transferBills = null;
        pharmacyController.setManagePharamcyReportIndex(pharmacyDisbursementReportIndex);
        return "/pharmacy/reports/disbursement_reports/pharmacy_report_transfer_issue_bill?faces-redirect=true";
    }

    public String navigateBackToTransferIssueByBill() {
        return "/pharmacy/reports/disbursement_reports/pharmacy_report_transfer_issue_bill?faces-redirect=true";
    }

    public String navigateToTransferReceiveByBill() {
        transferBills = null;
        pharmacyController.setManagePharamcyReportIndex(pharmacyDisbursementReportIndex);
        return "/pharmacy/reports/disbursement_reports/pharmacy_report_transfer_receive_bill?faces-redirect=true";
    }

    public String navigateToTransferIssueByBillSummary() {
        transferBills = null;
        pharmacyController.setManagePharamcyReportIndex(pharmacyDisbursementReportIndex);
        return "/pharmacy/reports/disbursement_reports/pharmacy_report_transfer_issue_bill_summery?faces-redirect=true";
    }

    public String navigateToPharmacyTransferReports() {
        transferBills = null;
        pharmacyController.setManagePharamcyReportIndex(pharmacyDisbursementReportIndex);
        return pharmacyController.navigateToPharmacyAnalytics();
    }

    public String navigateToBillPreview(Bill b) {
        previewBill = b;
        return "/inward/pharmacy_reprint_bill_sale_bht_bill?faces-redirect=true";
    }

    public String navigateBackFromBillPreview() {
        previewBill = null;
        return "/pharmacy/pharmacy_report_bht_issue_bill?faces-redirect=true";
    }

    /**
     * Methods
     */
    public void fillFastMoving() {
        fillMoving(true);
        fillMovingQty(true);
    }

    public void fillSlowMoving() {
        fillMoving(false);
        fillMovingQty(false);
    }

    public BillBeanController getBillBeanController() {
        return billBeanController;
    }

    public void setBillBeanController(BillBeanController billBeanController) {
        this.billBeanController = billBeanController;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void fillMovingWithStock() {
        String sql;
        Map m = new HashMap();
        m.put("i", institution);
        m.put("t1", BillType.PharmacyTransferIssue);
        m.put("t2", BillType.PharmacyPre);
        m.put("fd", fromDate);
        m.put("td", toDate);
        BillItem bi = new BillItem();

        sql = "select bi.item, abs(SUM(bi.pharmaceuticalBillItem.qty)), "
                + "abs(SUM(bi.pharmaceuticalBillItem.stock.itemBatch.purcahseRate * bi.pharmaceuticalBillItem.qty)), "
                + "SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.qty)) "
                + "FROM BillItem bi where bi.retired=false and  bi.bill.department.institution=:i and "
                + "(bi.bill.billType=:t1 or bi.bill.billType=:t2) and "
                + "bi.bill.billDate between :fd and :td group by bi.item "
                + "order by  SUM(bi.pharmaceuticalBillItem.qty) desc";
        List<Object[]> objs = getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);
        movementRecordsQty = new ArrayList<>();
        for (Object[] obj : objs) {
            StockReportRecord r = new StockReportRecord();
            r.setItem((Item) obj[0]);
            r.setQty((Double) obj[1]);
            Days daysBetween = Days.daysBetween(LocalDate.fromDateFields(fromDate), LocalDate.fromDateFields(toDate));
            int ds = daysBetween.getDays();
            r.setPurchaseValue(r.getQty() / ds);
//            r.setRetailsaleValue((Double) obj[2]);
            r.setStockQty(getPharmacyBean().getItemStockQty(r.getItem(), institution));
            movementRecordsQty.add(r);
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
                + " and bi.bill.billType in :bt "
                + " group by bi.item ";

        if (!fast) {
            sql += "order by  SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.pharmaceuticalBillItem.qty) desc";
        } else {
            sql += "order by  SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.pharmaceuticalBillItem.qty) asc";
        }
        List<Object[]> objs = getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);
        movementRecords = new ArrayList<>();
        if (objs == null) {
            return;
        }
        for (Object[] obj : objs) {
            StockReportRecord r = new StockReportRecord();
            r.setItem((Item) obj[0]);
            r.setQty((Double) obj[1]);
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
                + " and bi.bill.createdAt between :fd and :td "
                + " group by bi.item ";

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

    public void fillDepartmentTransfersReceive() {
        Date startTime = new Date();

        transferItems = fetchBillItems(BillType.PharmacyTransferReceive);
        purchaseValue = 0.0;
        saleValue = 0.0;
        for (BillItem ts : transferItems) {
            // Use the actual transfer rate that was stored when the bill was created
            // This respects the configured transfer rate option (purchase/cost/retail)
            if (ts.getBillItemFinanceDetails() != null && ts.getBillItemFinanceDetails().getLineNetTotal() != null) {
                purchaseValue += ts.getBillItemFinanceDetails().getLineNetTotal().doubleValue();
            } else {
                // Fallback to purchase rate if financial details are missing
                purchaseValue += (ts.getPharmaceuticalBillItem().getItemBatch().getPurcahseRate() * ts.getPharmaceuticalBillItem().getQtyInUnit());
            }
            saleValue += (ts.getPharmaceuticalBillItem().getItemBatch().getRetailsaleRate() * ts.getPharmaceuticalBillItem().getQtyInUnit());
        }

    }

    public void fillDepartmentTransfersIssueByBillItem() {
        fillDepartmentTransfersIssueByBillItemDto();
    }

    @Deprecated // Need to remove if QAs are fine with fillDepartmentTransfersIssueByBillItemDto
    private void fillDepartmentTransfersIssueByBillItemEntity() {
        Date startTime = new Date();

        transferItems = fetchBillItems(BillType.PharmacyTransferIssue);
        purchaseValue = 0.0;
        saleValue = 0.0;
        retailValue = 0.0;
        costValue = 0.0;
        transferValue = 0.0;

        for (BillItem ts : transferItems) {
            double qty = ts.getPharmaceuticalBillItem().getQtyInUnit();
            ItemBatch itemBatch = ts.getPharmaceuticalBillItem().getItemBatch();

            // Purchase Value calculation
            if (ts.getBillItemFinanceDetails() != null && ts.getBillItemFinanceDetails().getLineNetTotal() != null) {
                purchaseValue += ts.getBillItemFinanceDetails().getLineNetTotal().doubleValue();
            } else {
                // Fallback to purchase rate if financial details are missing
                purchaseValue += (itemBatch.getPurcahseRate() * qty);
            }

            // Retail Value calculation
            saleValue += (itemBatch.getRetailsaleRate() * qty);
            retailValue += (itemBatch.getRetailsaleRate() * qty);

            // Cost Value calculation
            costValue += (itemBatch.getCostRate() * qty);

            // Transfer Value calculation (from BillItemFinanceDetails)
            if (ts.getBillItemFinanceDetails() != null && ts.getBillItemFinanceDetails().getLineGrossTotal() != null) {
                transferValue += ts.getBillItemFinanceDetails().getLineGrossTotal().doubleValue();
            } else {
                // Fallback to purchase rate if financial details are missing
                transferValue += (itemBatch.getPurcahseRate() * qty);
            }
        }

    }

    public void fillDepartmentAdjustmentByBillItem() {
        Date startTime = new Date();
        transferItems = fetchBillItems(BillType.PharmacyAdjustment);

    }

    public List<BillItem> fetchBillItems(BillType bt) {
        List<BillItem> billItems;

        Map m = new HashMap();
        String sql;
        sql = "select bi from BillItem bi where "
                + " bi.bill.createdAt between :fd and :td "
                + " and bi.bill.billType=:bt ";

        if (bt == BillType.PharmacyTransferIssue) {
            if (fromDepartment != null) {
                sql += " and bi.bill.department=:fdept ";
                m.put("fdept", fromDepartment);
            }

            if (toDepartment != null) {
                sql += " and bi.bill.toDepartment=:tdept ";
                m.put("tdept", toDepartment);
            }
        }

        if (bt == BillType.PharmacyTransferReceive) {
            if (fromDepartment != null) {
                sql += " and bi.bill.fromDepartment=:fdept ";
                m.put("fdept", fromDepartment);
            }

            if (toDepartment != null) {
                sql += " and bi.bill.department=:tdept ";
                m.put("tdept", toDepartment);
            }
        }

        sql += " order by bi.id";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", bt);

        billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        return billItems;
    }

    private void fillDepartmentTransfersIssueByBillItemDto() {

        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();

        jpql.append("select new com.divudi.core.data.dto.PharmacyTransferIssueBillItemDTO(")
                .append("TYPE(b), ")
                .append("b.deptId, b.createdAt, it.name, it.code,")
                .append(" bi.qty, ib.costRate, bfd.valueAtCostRate,")
                .append(" p.retailRate, bfd.valueAtRetailRate,")
                .append(" p.purchaseRate, bfd.valueAtPurchaseRate,")
                .append(" bfd.lineGrossRate, bfd.lineGrossTotal)")
                .append(" from BillItem bi")
                .append(" join bi.bill b")
                .append(" left join bi.pharmaceuticalBillItem p")
                .append(" left join p.itemBatch ib")
                .append(" left join bi.billItemFinanceDetails bfd")
                .append(" left join bi.item it")
                .append(" where b.billType=:bt")
                .append(" and (b.retired=false or b.retired is null)")
                .append(" and (bi.retired=false or bi.retired is null)")
                .append(" and b.createdAt between :fd and :td ");

        params.put("fd", fromDate);
        params.put("td", toDate);
        params.put("bt", BillType.PharmacyTransferIssue);

        if (fromDepartment != null) {
            jpql.append(" and b.department=:fdept");
            params.put("fdept", fromDepartment);
        }

        if (toDepartment != null) {
            jpql.append(" and b.toDepartment=:tdept");
            params.put("tdept", toDepartment);
        }

        jpql.append(" order by bi.id");

        try {
            transferIssueBillItemDtos = (List<PharmacyTransferIssueBillItemDTO>) getBillItemFacade()
                    .findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
        } catch (Exception e) {
            e.printStackTrace();
            transferIssueBillItemDtos = new ArrayList<>();
        }

        purchaseValue = 0.0;
        saleValue = 0.0;
        retailValue = 0.0;
        costValue = 0.0;
        transferValue = 0.0;

        if (transferIssueBillItemDtos != null) {
            for (PharmacyTransferIssueBillItemDTO d : transferIssueBillItemDtos) {
                if (d.getPurchaseValue() != null) {
                    purchaseValue += d.getPurchaseValue();
                }
                if (d.getCostValue() != null) {
                    costValue += d.getCostValue();
                }
                if (d.getRetailValue() != null) {
                    retailValue += d.getRetailValue();
                    saleValue += d.getRetailValue();
                }
                if (d.getTransferValue() != null) {
                    transferValue += d.getTransferValue();
                }
            }
        }

    }

    public void fillDepartmentTransfersReceiveByBillItem() {
        fillDepartmentTransfersReceiveByBillItemDto();
    }

    /**
     * CRITICAL FIX for Issue #15797: Added TYPE(b) to distinguish cancelled receive items
     * for proper item-level reporting.
     */
    private void fillDepartmentTransfersReceiveByBillItemDto() {

        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();

        jpql.append("select new com.divudi.core.data.dto.PharmacyTransferReceiveBillItemDTO(")
                .append("TYPE(b), ")  // ADDED: Bill class discriminator to identify CancelledBill
                .append("b.deptId, b.createdAt, it.name, it.code,")
                .append(" bi.qty, ib.costRate, bfd.valueAtCostRate,")
                .append(" p.retailRate, bfd.valueAtRetailRate,")
                .append(" p.purchaseRate, bfd.valueAtPurchaseRate,")
                .append(" bfd.lineGrossRate, bfd.lineGrossTotal)")
                .append(" from BillItem bi")
                .append(" join bi.bill b")
                .append(" left join bi.pharmaceuticalBillItem p")
                .append(" left join p.itemBatch ib")
                .append(" left join bi.billItemFinanceDetails bfd")
                .append(" left join bi.item it")
                .append(" where b.billType=:bt")
                .append(" and (b.retired=false or b.retired is null)")
                .append(" and (bi.retired=false or bi.retired is null)")
                .append(" and b.createdAt between :fd and :td ");

        params.put("fd", fromDate);
        params.put("td", toDate);
        params.put("bt", BillType.PharmacyTransferReceive);

        if (fromDepartment != null) {
            jpql.append(" and b.fromDepartment=:fdept");
            params.put("fdept", fromDepartment);
        }

        if (toDepartment != null) {
            jpql.append(" and b.department=:tdept");
            params.put("tdept", toDepartment);
        }

        jpql.append(" order by bi.id");

        try {
            transferReceiveBillItemDtos = (List<PharmacyTransferReceiveBillItemDTO>) getBillItemFacade()
                    .findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
        } catch (Exception e) {
            e.printStackTrace();
            transferReceiveBillItemDtos = new ArrayList<>();
        }

        purchaseValue = 0.0;
        saleValue = 0.0;
        retailValue = 0.0;
        costValue = 0.0;
        transferValue = 0.0;

        if (transferReceiveBillItemDtos != null) {
            for (PharmacyTransferReceiveBillItemDTO d : transferReceiveBillItemDtos) {
                if (d.getPurchaseValue() != null) {
                    purchaseValue += d.getPurchaseValue();
                }
                if (d.getCostValue() != null) {
                    costValue += d.getCostValue();
                }
                if (d.getRetailValue() != null) {
                    retailValue += d.getRetailValue();
                    saleValue += d.getRetailValue();
                }
                if (d.getTransferValue() != null) {
                    transferValue += d.getTransferValue();
                }
            }
        }

    }


    /**
     * DTO-based approach for efficient data retrieval - no calculations needed
     * This is the recommended approach for better performance
     */
    public void fillDepartmentTransfersIssueByBillDto() {
        reportTimerController.trackReportExecution(() -> {
            fillTransferIssueBillsDtoDirectly();
        }, DisbursementReports.TRANSFER_ISSUE_BY_BILL, sessionController.getLoggedUser());
    }

    /**
     * Entity-based approach for backward compatibility Uses the traditional
     * method with iterative calculations
     */
    @Deprecated // Use fillDepartmentTransfersIssueByBillDto
    public void fillDepartmentTransfersIssueByBillEntity() {
        reportTimerController.trackReportExecution(() -> {
            fillTransferIssueBillsLegacy();
            calculatePurachaseValuesOfBillItemsInBill(transferBills);
        }, DisbursementReports.TRANSFER_ISSUE_BY_BILL, sessionController.getLoggedUser());
    }

    /**
     * Direct DTO query with aggregated financial data - follows DTO
     * implementation guidelines This is the primary method that should be used
     * for report display
     */
    private void fillTransferIssueBillsDtoDirectly() {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();

        jpql.append("SELECT new com.divudi.core.data.dto.PharmacyTransferIssueBillDTO(")
                .append("TYPE(b), ")
                .append("b.id, ")
                .append("COALESCE(b.deptId, ''), ")
                .append("b.createdAt, ")
                .append("COALESCE(b.department.name, ''), ")
                .append("COALESCE(b.toDepartment.name, ''), ")
                .append("COALESCE(p.name, ''), ")
                .append("COALESCE(b.cancelled, false), ")
                .append("COALESCE(b.refunded, false), ")
                .append("COALESCE(b.comments, ''), ")
                .append("COALESCE(bfd.totalCostValue, 0.0), ")
                .append("COALESCE(bfd.totalPurchaseValue, 0.0), ")
                .append("COALESCE(bfd.lineNetTotal, 0.0), ")
                .append("COALESCE(bfd.totalRetailSaleValue, 0.0), ")
                .append("COALESCE(bb.deptId, ''), ")
                .append("bb.id")
                .append(") ")
                .append("FROM Bill b ")
                .append("LEFT JOIN b.billFinanceDetails bfd ")
                .append("LEFT JOIN b.toStaff ts ")
                .append("LEFT JOIN ts.person p ")
                .append("LEFT JOIN b.billedBill bb ")
                .append("WHERE b.billType = :bt ")
                .append("AND b.retired = false ")
                .append("AND b.createdAt BETWEEN :fd AND :td ");

        params.put("fd", fromDate);
        params.put("td", toDate);
        params.put("bt", BillType.PharmacyTransferIssue);

        if (fromDepartment != null) {
            jpql.append("AND b.department = :fdept ");
            params.put("fdept", fromDepartment);
        }

        if (toDepartment != null) {
            jpql.append("AND b.toDepartment = :tdept ");
            params.put("tdept", toDepartment);
        }

        jpql.append("ORDER BY b.id");

        // Execute the DTO query
        // Execute the DTO query
        try {
            transferIssueDtos = (List<PharmacyTransferIssueBillDTO>) getBillFacade().findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
        } catch (Exception e) {
            // Log the exception for debugging
            // logger.error("Failed to fetch transfer issue DTOs", e);
            transferIssueDtos = new ArrayList<>();
        }
        // Calculate totals from DTOs 
        totalsValue = 0.0;
        netTotalValues = 0.0;
        costValue = 0.0;
        purchaseValue = 0.0;
        transferValue = 0.0;
        if (transferIssueDtos != null) {
            for (PharmacyTransferIssueBillDTO dto : transferIssueDtos) {
                if (dto.getSaleValue() != null) {
                    totalsValue += dto.getSaleValue();
                }
                if (dto.getPurchaseValue() != null) {
                    purchaseValue += dto.getPurchaseValue();
                }
                if (dto.getCostValue() != null) {
                    costValue += dto.getCostValue().doubleValue();
                }
                if (dto.getTransferValueDouble() != null) {
                    transferValue += dto.getTransferValueDouble();
                }
            }
        }
    }

    /**
     * Legacy method for backward compatibility - will be removed in future
     * version
     *
     * @deprecated Use fillTransferIssueBillsDtoDirectly() instead
     */
    @Deprecated
    private void fillTransferIssueBillsLegacy() {

        Map params = new HashMap();
        String jpql;
        params.put("fd", fromDate);
        params.put("td", toDate);
        params.put("bt", BillType.PharmacyTransferIssue);
        if (fromDepartment != null && toDepartment != null) {
            params.put("fdept", fromDepartment);
            params.put("tdept", toDepartment);
            jpql = "select b from Bill b where b.department=:fdept"
                    + " and b.toDepartment=:tdept "
                    + " and b.createdAt between :fd "
                    + " and :td and b.billType=:bt "
                    + " and b.retired=false "
                    + " order by b.id";
        } else if (fromDepartment == null && toDepartment != null) {
            params.put("tdept", toDepartment);
            jpql = "select b from Bill b where "
                    + " b.toDepartment=:tdept and b.createdAt "
                    + " between :fd and :td "
                    + " and b.retired=false "
                    + " and b.billType=:bt order by b.id";
        } else if (fromDepartment != null) {
            params.put("fdept", fromDepartment);
            jpql = "select b from Bill b where "
                    + " b.department=:fdept and b.createdAt "
                    + " between :fd and :td "
                    + " and b.retired=false "
                    + " and b.billType=:bt order by b.id";
        } else {
            jpql = "select b from Bill b where b.createdAt "
                    + " between :fd and :td and b.billType=:bt "
                    + " and b.retired=false "
                    + " order by b.id";
        }

        transferBills = getBillFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    public void calculatePurachaseValuesOfBillItemsInBill(List<Bill> billList) {
        if (billList == null) {
            return;
        }

        // Reset totals before accumulating stored values
        purchaseValue = 0.0;
        retailValue = 0.0;
        costValue = 0.0;

        for (Bill b : billList) {
            retailValue += b.getBillFinanceDetails().getTotalRetailSaleValue().doubleValue();
            purchaseValue += b.getBillFinanceDetails().getTotalPurchaseValue().doubleValue();
            costValue += b.getBillFinanceDetails().getTotalCostValue().doubleValue();
        }

    }

    // TODO: Remove this method if QAs are fine with the DTO approach
    @Deprecated
    public void fillDepartmentBHTIssueByBill() {
        reportTimerController.trackReportExecution(() -> {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
            ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

            String url = request.getRequestURL().toString();

            String ipAddress = request.getRemoteAddr();

            AuditEvent auditEvent = new AuditEvent();
            auditEvent.setEventStatus("Started");
            long duration;
            Date startTime = new Date();
            auditEvent.setEventDataTime(startTime);
            if (sessionController != null && sessionController.getDepartment() != null) {
                auditEvent.setDepartmentId(sessionController.getDepartment().getId());
            }

            if (sessionController != null && sessionController.getInstitution() != null) {
                auditEvent.setInstitutionId(sessionController.getInstitution().getId());
            }
            if (sessionController != null && sessionController.getLoggedUser() != null) {
                auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
            }
            auditEvent.setUrl(url);
            auditEvent.setIpAddress(ipAddress);
            auditEvent.setEventTrigger("fillDepartmentBHTIssueByBill()");
            auditEventApplicationController.logAuditEvent(auditEvent);

            Map m = new HashMap();
            String sql;
            m.put("fd", fromDate);
            m.put("td", toDate);
            m.put("bt", BillType.PharmacyBhtPre);
            m.put("fdept", fromDepartment);
            sql = "select b from Bill b "
                    + " where b.department=:fdept "
                    + " and b.createdAt  between :fd and :td ";

            if (admissionType != null) {
                sql += "  and b.patientEncounter.admissionType=:at ";
                m.put("at", admissionType);
            }

            sql += " and b.billType=:bt order by b.id";

            transferBills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
            totalsValue = 0.0;
            discountsValue = 0.0;
            netTotalValues = 0.0;
            marginValue = 0;
            netTotalPurchaseValues = 0.0;
            for (Bill b : transferBills) {
                if (b.getBillFinanceDetails() == null
                        || b.getBillFinanceDetails().getTotalPurchaseValue() == null
                        || b.getBillFinanceDetails().getTotalPurchaseValue().doubleValue() == 0) {
                    billService.createBillFinancialDetailsForPharmacyBill(b);
                }
                Double pv = (b.getBillFinanceDetails() == null
                        || b.getBillFinanceDetails().getTotalPurchaseValue() == null)
                        ? 0.0
                        : b.getBillFinanceDetails().getTotalPurchaseValue().doubleValue();
                netTotalPurchaseValues += pv;
                totalsValue = totalsValue + (b.getTotal());
                discountsValue = discountsValue + b.getDiscount();
                marginValue += b.getMargin();
                netTotalValues = netTotalValues + b.getNetTotal();
            }

            Date endTime = new Date();
            duration = endTime.getTime() - startTime.getTime();
            auditEvent.setEventDuration(duration);
            auditEvent.setEventStatus("Completed");
            auditEventApplicationController.logAuditEvent(auditEvent);
        }, PharmacyReports.BHT_ISSUE_BY_BILL, sessionController.getLoggedUser());
    }

    public void fillDepartmentBHTIssueByBillByDTOs() {
        reportTimerController.trackReportExecution(() -> {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
            ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

            String url = request.getRequestURL().toString();

            String ipAddress = request.getRemoteAddr();

            AuditEvent auditEvent = new AuditEvent();
            auditEvent.setEventStatus("Started");
            long duration;
            Date startTime = new Date();
            auditEvent.setEventDataTime(startTime);
            if (sessionController != null && sessionController.getDepartment() != null) {
                auditEvent.setDepartmentId(sessionController.getDepartment().getId());
            }

            if (sessionController != null && sessionController.getInstitution() != null) {
                auditEvent.setInstitutionId(sessionController.getInstitution().getId());
            }
            if (sessionController != null && sessionController.getLoggedUser() != null) {
                auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
            }
            auditEvent.setUrl(url);
            auditEvent.setIpAddress(ipAddress);
            auditEvent.setEventTrigger("fillDepartmentBHTIssueByBill()");
            auditEventApplicationController.logAuditEvent(auditEvent);

            Map m = new HashMap();
            StringBuilder sql = new StringBuilder();
            m.put("fd", fromDate);
            m.put("td", toDate);
            m.put("bt", BillType.PharmacyBhtPre);
            m.put("fdept", fromDepartment);

            sql.append("SELECT new com.divudi.core.data.dto.PharmacyTransferIssueBillDTO(")
                    .append("b.id, ")
                    .append("COALESCE(b.deptId, ''), ")
                    .append("b.createdAt, ")
                    .append("CASE WHEN b.patientEncounter IS NOT NULL THEN COALESCE(b.patientEncounter.bhtNo, '') ELSE '' END, ")
                    .append("COALESCE(cb.deptId, ''), ")
                    .append("CASE WHEN b.billFinanceDetails IS NOT NULL THEN COALESCE(b.billFinanceDetails.totalPurchaseValue, 0.0) ELSE 0.0 END, ")
                    .append("COALESCE(b.total, 0.0), ")
                    .append("COALESCE(b.margin, 0.0), ")
                    .append("COALESCE(b.discount, 0.0), ")
                    .append("COALESCE(b.netTotal, 0.0) ")
                    .append(") ")
                    .append("FROM Bill b ")
                    .append("LEFT JOIN b.cancelledBill cb ")
                    .append("WHERE b.createdAt BETWEEN :fd AND :td ")
                    .append("AND b.department=:fdept ");

            if (admissionType != null) {
                sql.append("  and b.patientEncounter.admissionType=:at ") ;
                m.put("at", admissionType);
            }

            sql.append(" and b.billType=:bt order by b.id");

            transferIssueDtos = (List<PharmacyTransferIssueBillDTO>) getBillFacade().findLightsByJpql(sql.toString(), m, TemporalType.TIMESTAMP);
            totalsValue = 0.0;
            discountsValue = 0.0;
            netTotalValues = 0.0;
            marginValue = 0;
            netTotalPurchaseValues = 0.0;
            for (PharmacyTransferIssueBillDTO b : transferIssueDtos) {
                netTotalPurchaseValues += b.getPurchaseValue();
                totalsValue = totalsValue + (b.getTotal());
                discountsValue = discountsValue + b.getDiscount();
                marginValue += b.getMargin();
                netTotalValues = netTotalValues + b.getNetValue();
            }

            Date endTime = new Date();
            duration = endTime.getTime() - startTime.getTime();
            auditEvent.setEventDuration(duration);
            auditEvent.setEventStatus("Completed");
            auditEventApplicationController.logAuditEvent(auditEvent);
        }, PharmacyReports.BHT_ISSUE_BY_BILL, sessionController.getLoggedUser());
    }

    Item item;

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public void fillDepartmentBHTIssueByBillItems() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("fillDepartmentBHTIssueByBillItems()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.PharmacyBhtPre);
        m.put("fdept", fromDepartment);
        sql = "select b from BillItem b "
                + " where b.bill.department=:fdept "
                + " and b.createdAt  between :fd and :td ";

        if (item != null) {
            sql += " and b.item=:itm ";
            m.put("itm", item);
        }

        sql += " and b.bill.billType=:bt "
                + " order by b.item.name";
        transferItems = billItemFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
        totalsValue = 0.0;
        discountsValue = 0.0;
        netTotalValues = 0.0;
        marginValue = 0;
        for (BillItem b : transferItems) {
            totalsValue = totalsValue + (b.getGrossValue());
            discountsValue = discountsValue + b.getDiscount();
            marginValue += b.getMarginValue();
            netTotalValues = netTotalValues + b.getNetValue();
        }

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

    }

    List<String1Value3> listz;
    List<String2Value4Transfer> transferList;

    public List<String1Value3> getListz() {

        return listz;
    }

    public void setListz(List<String1Value3> listz) {
        this.listz = listz;
    }

    public List<String2Value4Transfer> getTransferList() {
        return transferList;
    }

    public void setTransferList(List<String2Value4Transfer> transferList) {
        this.transferList = transferList;
    }

    public void createDepartmentIssue() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("createDepartmentIssue()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        listz = new ArrayList<>();

        // Create custom DTO-based query for disposal bill types
        Map<String, Object> m = new HashMap<>();
        List<BillTypeAtomic> bts = new ArrayList<>();
        bts.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE);
        bts.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_CANCELLED);
        bts.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_RETURN);

                String jpql = "select b.toDepartment, sum(b.netTotal) "
                + " from Bill b "
                + " where b.billTypeAtomic in :bts "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired = false";

        m.put("bts", bts);
        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());

        if (getFromDepartment() != null) {
            jpql += " and b.fromDepartment = :fromDept";
            m.put("fromDept", getFromDepartment());
        }

        jpql += " group by b.toDepartment order by b.toDepartment.name";

        List<Object[]> list = getBillFacade().findAggregates(jpql, m, TemporalType.TIMESTAMP);
        if (list == null) {
            return;
        }

        netTotalValues = 0;
        for (Object[] obj : list) {
            Department item = (Department) obj[0];
            Double dbl = (Double) obj[1];

            String1Value3 newD = new String1Value3();
            newD.setString(item.getName());
            newD.setValue1(dbl);
            newD.setSummery(false);
            listz.add(newD);

            netTotalValues += dbl;
        }
        
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

    }

    public void createTransferIssueBillSummery() {
        reportTimerController.trackReportExecution(() -> {
            fetchBillTotalByToDepartment(fromDate, toDate, fromDepartment, BillType.PharmacyTransferIssue);
        }, DisbursementReports.TRANSFER_ISSUE_BY_BILL_SUMMARY, sessionController.getLoggedUser());
    }

    public void createTransferIssueBillSummeryEnhanced() {
        reportTimerController.trackReportExecution(() -> {
            fetchBillTotalByDepartmentsEnhanced(fromDate, toDate, fromDepartment, toDepartment, BillType.PharmacyTransferIssue);
        }, DisbursementReports.TRANSFER_ISSUE_BY_BILL_SUMMARY, sessionController.getLoggedUser());
    }

    public void createTransferReciveBillSummery() {
        reportTimerController.trackReportExecution(() -> {
            fetchBillTotalByFromDepartmentFinance(fromDate, toDate, fromDepartment, BillType.PharmacyTransferReceive);
        }, DisbursementReports.TRANSFER_RECEIVE_BY_BILL_SUMMARY, sessionController.getLoggedUser());
    }

    public void fetchBillTotalByToDepartment(Date fd, Date td, Department dep, BillType bt) {
        listz = new ArrayList<>();
        netTotalValues = 0.0;
        netTotalSaleValues = 0.0;
        netTotalPurchaseValues = 0.0;
        netTotalCostValues = 0.0;

        // Use BillFinanceDetails for consistent calculations (matching DTO approach)
        List<Object[]> objects = getBillBeanController().fetchBilledDepartmentFinanceDetails(fd, td, dep, bt, true);

        for (Object[] ob : objects) {
            Department d = (Department) ob[0];

            // Handle both BigDecimal and Double types from aggregation functions
            double dbl1 = 0.0;
            if (ob[1] instanceof BigDecimal) {
                dbl1 = ((BigDecimal) ob[1]).doubleValue();
            } else if (ob[1] instanceof Double) {
                dbl1 = (Double) ob[1];
            }

            double dbl2 = 0.0;
            if (ob[2] instanceof BigDecimal) {
                dbl2 = ((BigDecimal) ob[2]).doubleValue();
            } else if (ob[2] instanceof Double) {
                dbl2 = (Double) ob[2];
            }

            double dbl3 = 0.0;
            if (ob[3] instanceof BigDecimal) {
                dbl3 = ((BigDecimal) ob[3]).doubleValue();
            } else if (ob[3] instanceof Double) {
                dbl3 = (Double) ob[3];
            }

            double dbl4 = 0.0;
            if (ob[4] instanceof BigDecimal) {
                dbl4 = ((BigDecimal) ob[4]).doubleValue();
            } else if (ob[4] instanceof Double) {
                dbl4 = (Double) ob[4];
            }

            String1Value3 sv = new String1Value3();
            sv.setString(d.getName());
            sv.setValue1(dbl1);                  // Transfer Value (lineNetTotal)
            sv.setValue2(dbl2);                  // Purchase Value (totalCostValue)
            sv.setValue3(dbl3);                  // Sale Value (totalRetailSaleValue)
            sv.setValue4(dbl4);                  // Cost Value (totalCostValue)
            listz.add(sv);

            netTotalValues += dbl1;
            netTotalPurchaseValues += dbl2;
            netTotalSaleValues += dbl3;
            netTotalCostValues += dbl4;

        }

    }

    public void fetchBillTotalByDepartmentsEnhanced(Date fd, Date td, Department fromDep, Department toDep, BillType bt) {
        listz = new ArrayList<>();
        netTotalValues = 0.0;
        netTotalSaleValues = 0.0;
        netTotalPurchaseValues = 0.0;
        netTotalCostValues = 0.0;

        List<Object[]> objects = getBillBeanController().fetchBilledDepartmentFinanceDetailsEnhanced(fd, td, fromDep, toDep, bt);

        for (Object[] ob : objects) {
            Department d = (Department) ob[0];

            double dbl1 = 0.0;
            if (ob[1] instanceof BigDecimal) {
                dbl1 = ((BigDecimal) ob[1]).doubleValue();
            } else if (ob[1] instanceof Double) {
                dbl1 = (Double) ob[1];
            }

            double dbl2 = 0.0;
            if (ob[2] instanceof BigDecimal) {
                dbl2 = ((BigDecimal) ob[2]).doubleValue();
            } else if (ob[2] instanceof Double) {
                dbl2 = (Double) ob[2];
            }

            double dbl3 = 0.0;
            if (ob[3] instanceof BigDecimal) {
                dbl3 = ((BigDecimal) ob[3]).doubleValue();
            } else if (ob[3] instanceof Double) {
                dbl3 = (Double) ob[3];
            }

            double dbl4 = 0.0;
            if (ob[4] instanceof BigDecimal) {
                dbl4 = ((BigDecimal) ob[4]).doubleValue();
            } else if (ob[4] instanceof Double) {
                dbl4 = (Double) ob[4];
            }

            String1Value3 sv = new String1Value3();
            sv.setString(d.getName());
            sv.setValue1(dbl1);                  // Transfer Value (lineNetTotal)
            sv.setValue2(dbl2);                  // Purchase Value (totalPurchaseValue)
            sv.setValue3(dbl3);                  // Sale Value (totalRetailSaleValue)
            sv.setValue4(dbl4);                  // Cost Value (totalCostValue)
            listz.add(sv);

            netTotalValues += dbl1;
            netTotalPurchaseValues += dbl2;
            netTotalSaleValues += dbl3;
            netTotalCostValues += dbl4;
        }
    }

    public void createTransferIssueBillSummeryWithFromAndTo() {
        reportTimerController.trackReportExecution(() -> {
            fetchBillTotalByDepartmentsWithFromAndTo(fromDate, toDate, fromDepartment, toDepartment, BillType.PharmacyTransferIssue);
        }, DisbursementReports.TRANSFER_ISSUE_BY_BILL_SUMMARY, sessionController.getLoggedUser());
    }

    public void createTransferReceiveBillSummeryWithFromAndTo() {
        reportTimerController.trackReportExecution(() -> {
            fetchBillTotalByDepartmentsWithFromAndTo(fromDate, toDate, fromDepartment, toDepartment, BillType.PharmacyTransferReceive);
        }, DisbursementReports.TRANSFER_RECEIVE_BY_BILL_SUMMARY, sessionController.getLoggedUser());
    }

    public void fetchBillTotalByDepartmentsWithFromAndTo(Date fd, Date td, Department fromDep, Department toDep, BillType bt) {
        transferList = new ArrayList<>();
        netTotalValues = 0.0;
        netTotalSaleValues = 0.0;
        netTotalPurchaseValues = 0.0;
        netTotalCostValues = 0.0;

        List<Object[]> objects = getBillBeanController().fetchBilledDepartmentFinanceDetailsWithFromAndTo(fd, td, fromDep, toDep, bt);

        for (Object[] ob : objects) {
            Department fromDepartment = (Department) ob[0];
            Department toDepartment = (Department) ob[1];

            double dbl1 = 0.0;
            if (ob[2] instanceof BigDecimal) {
                dbl1 = ((BigDecimal) ob[2]).doubleValue();
            } else if (ob[2] instanceof Double) {
                dbl1 = (Double) ob[2];
            }

            double dbl2 = 0.0;
            if (ob[3] instanceof BigDecimal) {
                dbl2 = ((BigDecimal) ob[3]).doubleValue();
            } else if (ob[3] instanceof Double) {
                dbl2 = (Double) ob[3];
            }

            double dbl3 = 0.0;
            if (ob[4] instanceof BigDecimal) {
                dbl3 = ((BigDecimal) ob[4]).doubleValue();
            } else if (ob[4] instanceof Double) {
                dbl3 = (Double) ob[4];
            }

            double dbl4 = 0.0;
            if (ob[5] instanceof BigDecimal) {
                dbl4 = ((BigDecimal) ob[5]).doubleValue();
            } else if (ob[5] instanceof Double) {
                dbl4 = (Double) ob[5];
            }

            String2Value4Transfer transferRecord = new String2Value4Transfer();
            transferRecord.setFromDepartment(fromDepartment != null ? fromDepartment.getName() : "Unknown");
            transferRecord.setToDepartment(toDepartment != null ? toDepartment.getName() : "Unknown");
            transferRecord.setTransferValue(dbl1);       // Transfer Value (lineNetTotal)
            transferRecord.setPurchaseValue(dbl2);       // Purchase Value (totalPurchaseValue)
            transferRecord.setRetailValue(dbl3);         // Sale Value (totalRetailSaleValue)
            transferRecord.setCostValue(dbl4);           // Cost Value (totalCostValue)
            transferList.add(transferRecord);

            netTotalValues += dbl1;
            netTotalPurchaseValues += dbl2;
            netTotalSaleValues += dbl3;
            netTotalCostValues += dbl4;
        }
    }

    public void fetchBillTotalByFromDepartment(Date fd, Date td, Department dep, BillType bt) {
        listz = new ArrayList<>();
        netTotalValues = 0.0;

        List<Object[]> objects = getBillBeanController().fetchBilledDepartmentItem(fd, td, dep, bt, false);

        for (Object[] ob : objects) {
            Department d = (Department) ob[0];
            double dbl = (double) ob[1];

            String1Value3 sv = new String1Value3();
            sv.setString(d.getName());
            sv.setValue1(dbl);
            listz.add(sv);

            netTotalValues += dbl;

        }

    }

    public void fetchBillTotalByFromDepartmentFinance(Date fd, Date td, Department dep, BillType bt) {
        listz = new ArrayList<>();
        netTotalValues = 0.0;
        netTotalSaleValues = 0.0;
        netTotalPurchaseValues = 0.0;
        netTotalCostValues = 0.0;

        // Use BillFinanceDetails for consistent calculations (matching DTO approach)
        List<Object[]> objects = getBillBeanController().fetchBilledDepartmentFinanceDetails(fd, td, dep, bt, false);

        for (Object[] ob : objects) {
            Department d = (Department) ob[0];

            // Handle both BigDecimal and Double types from aggregation functions
            double dbl1 = 0.0;
            if (ob[1] instanceof BigDecimal) {
                dbl1 = ((BigDecimal) ob[1]).doubleValue();
            } else if (ob[1] instanceof Double) {
                dbl1 = (Double) ob[1];
            }

            double dbl2 = 0.0;
            if (ob[2] instanceof BigDecimal) {
                dbl2 = ((BigDecimal) ob[2]).doubleValue();
            } else if (ob[2] instanceof Double) {
                dbl2 = (Double) ob[2];
            }

            double dbl3 = 0.0;
            if (ob[3] instanceof BigDecimal) {
                dbl3 = ((BigDecimal) ob[3]).doubleValue();
            } else if (ob[3] instanceof Double) {
                dbl3 = (Double) ob[3];
            }

            double dbl4 = 0.0;
            if (ob[4] instanceof BigDecimal) {
                dbl4 = ((BigDecimal) ob[4]).doubleValue();
            } else if (ob[4] instanceof Double) {
                dbl4 = (Double) ob[4];
            }

            String1Value3 sv = new String1Value3();
            sv.setString(d.getName());
            sv.setValue1(dbl1);                  // Transfer Value (lineNetTotal)
            sv.setValue2(dbl2);                  // Purchase Value (totalCostValue)
            sv.setValue3(dbl3);                  // Sale Value (totalRetailSaleValue)
            sv.setValue4(dbl4);                  // Cost Value (totalCostValue)
            listz.add(sv);

            netTotalValues += dbl1;
            netTotalPurchaseValues += dbl2;
            netTotalSaleValues += dbl3;
            netTotalCostValues += dbl4;
        }
    }

    public void createDepartmentIssueStore() {
        listz = new ArrayList<>();

        List<Object[]> list = getBillBeanController().fetchBilledDepartmentItemStore(getFromDate(), getToDate(), getFromDepartment());
        if (list == null) {
            return;
        }

        for (Object[] obj : list) {
            Department item = (Department) obj[0];
            Double dbl = (Double) obj[1];
            //double count = 0;

            String1Value3 newD = new String1Value3();
            newD.setString(item.getName());
            newD.setValue1(dbl);
            newD.setSummery(false);
            listz.add(newD);

        }

        netTotalValues = getBillBeanController().calNetTotalBilledDepartmentItemStore(fromDate, toDate, department);

    }

    public void fillDepartmentUnitIssueByBill() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("fillDepartmentUnitIssueByBill()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        Map<String, Object> m = new HashMap<>();
        List<BillTypeAtomic> bts = new ArrayList<>();
        bts.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE);
        bts.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_CANCELLED);
        bts.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_RETURN);

        StringBuilder jpql = new StringBuilder();
        jpql.append("select new com.divudi.core.data.dto.PharmacyItemPurchaseDTO(");
        jpql.append("b.id, b.deptId, b.createdAt, ");
        jpql.append("b.institution.name, b.department.name, b.fromDepartment.name, ");
        jpql.append("b.billType, b.total, b.netTotal, b.discount) ");
        jpql.append(" from Bill b");
        jpql.append(" where b.billTypeAtomic in :bts");
        jpql.append(" and b.createdAt between :fd and :td");
        jpql.append(" and b.retired = false");

        m.put("bts", bts);
        m.put("fd", fromDate);
        m.put("td", toDate);

        if (fromDepartment != null) {
            jpql.append(" and b.fromDepartment=:fdept");
            m.put("fdept", fromDepartment);
        }

        if (toDepartment != null) {
            jpql.append(" and b.toDepartment=:tdept");
            m.put("tdept", toDepartment);
        }

        jpql.append(" order by b.id");

        disposalIssueBillDtos = (List<PharmacyItemPurchaseDTO>) getBillFacade().findLightsByJpql(jpql.toString(), m, TemporalType.TIMESTAMP);
        
        // Calculate totals from DTO list
        totalsValue = 0.0;
        discountsValue = 0.0;
        netTotalValues = 0.0;
        if (disposalIssueBillDtos != null) {
            for (PharmacyItemPurchaseDTO dto : disposalIssueBillDtos) {
                totalsValue += dto.getBillTotal() != null ? dto.getBillTotal() : 0.0;
                discountsValue += dto.getBillDiscount() != null ? dto.getBillDiscount() : 0.0;
                netTotalValues += dto.getBillNetTotal() != null ? dto.getBillNetTotal() : 0.0;
            }
        }
        
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

    }

    public void fillDepartmentUnitIssueByBillStore() {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.StoreIssue);
        m.put("fdept", fromDepartment);
        m.put("tdept", toDepartment);
        sql = "select b from Bill b where "
                + " b.fromDepartment=:fdept and "
                + " b.toDepartment=:tdept and "
                + " b.createdAt "
                + " between :fd and :td and "
                + " b.billType=:bt order by b.id";
        transferBills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        totalsValue = 0.0;
        discountsValue = 0.0;
        netTotalValues = 0.0;
        for (Bill b : transferBills) {
            totalsValue = totalsValue + (b.getTotal());
            discountsValue = discountsValue + b.getDiscount();
            netTotalValues = netTotalValues + b.getNetTotal();
        }
    }

    private List<Object[]> fetchBillItem(BillType billType) {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select b.pharmaceuticalBillItem.itemBatch,"
                + " sum(b.grossValue),"
                + " sum(b.marginValue),"
                + " sum(b.discount),"
                + " sum(b.netValue)"
                + " from BillItem b "
                + " where b.bill.department=:fdept ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.bill.createdAt between :fd and :td"
                + " and b.bill.billType=:bt"
                + " group by b.pharmaceuticalBillItem.itemBatch "
                + " order by b.item.name";

        return getBillFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);
    }

    private List<Object[]> fetchBillItemWithOutMargin(BillType billType) {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select DISTINCT(b.pharmaceuticalBillItem.itemBatch.item),"
                + " sum(b.netValue)"
                + " from BillItem b "
                + " where b.bill.department=:fdept ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.bill.createdAt between :fd and :td"
                + " and b.bill.billType=:bt"
                + " group by b.pharmaceuticalBillItem.itemBatch.item "
                + " order by b.item.name";

        return getBillFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);
    }

    private List<Object[]> fetchBillItem(BillType billType, SurgeryBillType surgeryBillType) {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select b.pharmaceuticalBillItem.itemBatch,"
                + " sum(b.grossValue),"
                + " sum(b.marginValue),"
                + " sum(b.discount),"
                + " sum(b.netValue),"
                + " sum(b.pharmaceuticalBillItem.qty)"
                + " from BillItem b "
                + " where b.bill.department=:fdept ";

        if (surgeryBillType != null) {
            sql += " and b.bill.surgeryBillType=:surg";
            m.put("surg", surgeryBillType);
        } else {
//            sql += " and b.bill.surgeryBillType is null ";
        }

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        sql += " and b.bill.createdAt between :fd and :td "
                + " and b.bill.billType=:bt "
                + " group by b.pharmaceuticalBillItem.itemBatch "
                + " order by b.item.name ";

        return getBillFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);
    }

    private Double fetchBillTotal(BillType billType) {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select sum(b.netValue)"
                + " from BillItem b "
                + " where b.bill.department=:fdept ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    private Double fetchBillMargin(BillType billType) {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select sum(b.netValue)"
                + " from BillItem b "
                + " where b.bill.department=:fdept ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    private Double fetchBillDiscount(BillType billType) {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select sum(b.netValue)"
                + " from BillItem b "
                + " where b.bill.department=:fdept ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    private Double fetchBillNetTotal(BillType billType) {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select sum(b.netValue)"
                + " from BillItem b "
                + " where b.bill.department=:fdept ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public void fillItemCounts() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("fillItemCounts()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        // Use DTO-based approach with BillTypeAtomic for disposal issues
        Map<String, Object> m = new HashMap<>();
        List<BillTypeAtomic> bts = new ArrayList<>();
        bts.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE);
        bts.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_CANCELLED);
        bts.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_RETURN);

        StringBuilder jpql = new StringBuilder();
        jpql.append("select new com.divudi.core.data.dto.PharmacyItemPurchaseDTO(");
        jpql.append("bi.bill.id, bi.bill.deptId, bi.bill.createdAt, ");
        jpql.append("bi.item.code, bi.item.name, bi.bill.billType, ");
        jpql.append("bi.bill.fromDepartment.name, bi.netValue, bi.qty, bi.grossValue) ");
        jpql.append(" from BillItem bi");
        jpql.append(" LEFT JOIN bi.pharmaceuticalBillItem pbi");
        jpql.append(" LEFT JOIN pbi.itemBatch ib");
        jpql.append(" where bi.bill.billTypeAtomic in :bts");
        jpql.append(" and bi.bill.createdAt between :fd and :td");
        jpql.append(" and bi.retired = false");
        jpql.append(" and bi.bill.retired = false");
        jpql.append(" and bi.item.retired = false");
        jpql.append(" and (pbi.retired = false OR pbi IS NULL)");
        jpql.append(" and (ib.retired = false OR ib IS NULL)");

        m.put("bts", bts);
        m.put("fd", fromDate);
        m.put("td", toDate);

        if (fromDepartment != null) {
            jpql.append(" and bi.bill.fromDepartment = :fdept");
            m.put("fdept", fromDepartment);
        }

        if (toDepartment != null) {
            jpql.append(" and bi.bill.toDepartment = :tdept");
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            jpql.append(" and bi.item.category = :cat");
            m.put("cat", category);
        }

        jpql.append(" order by bi.item.name");

        disposalIssueBillItemDtos = (List<PharmacyItemPurchaseDTO>) getBillFacade().findLightsByJpql(jpql.toString(), m, TemporalType.TIMESTAMP);

        // Calculate totals from DTO list
        totalsValue = 0;
        marginValue = 0;
        netTotalValues = 0;
        purchaseValue = 0;
        retailValue = 0;
        
        if (disposalIssueBillItemDtos != null) {
            for (PharmacyItemPurchaseDTO dto : disposalIssueBillItemDtos) {
                totalsValue += dto.getBillTotal() != null ? dto.getBillTotal() : 0.0;
                netTotalValues += dto.getBillNetTotal() != null ? dto.getBillNetTotal() : 0.0;
            }
        }

        // Set bill totals (calculated from DTO data)
        billTotal = totalsValue;
        billMargin = marginValue;
        billDiscount = 0.0; // Not available in current DTO
        billNetTotal = netTotalValues;

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

    }

    public void fillDisposalIssueBillItemDtos() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();
        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }
        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("fillDisposalIssueBillItemDtos()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        // Use DTO-based approach with BillTypeAtomic for disposal issues
        Map<String, Object> m = new HashMap<>();
        List<BillTypeAtomic> bts = new ArrayList<>();
        bts.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE);
        bts.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_CANCELLED);
        bts.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_RETURN);

        StringBuilder jpql = new StringBuilder();
        jpql.append("select new com.divudi.core.data.dto.PharmacyItemPurchaseDTO(");
        jpql.append("bi.bill.id, bi.bill.deptId, bi.bill.createdAt, ");
        jpql.append("bi.item.code, bi.item.name, bi.bill.billType, ");
        jpql.append("bi.bill.fromDepartment.name, bi.netValue, bi.qty, bi.grossValue) ");
        jpql.append(" from BillItem bi");
        jpql.append(" LEFT JOIN bi.pharmaceuticalBillItem pbi");
        jpql.append(" LEFT JOIN pbi.itemBatch ib");
        jpql.append(" where bi.bill.billTypeAtomic in :bts");
        jpql.append(" and bi.bill.createdAt between :fd and :td");
        jpql.append(" and bi.retired = false");
        jpql.append(" and bi.bill.retired = false");
        jpql.append(" and bi.item.retired = false");
        jpql.append(" and (pbi.retired = false OR pbi IS NULL)");
        jpql.append(" and (ib.retired = false OR ib IS NULL)");

        m.put("bts", bts);
        m.put("fd", fromDate);
        m.put("td", toDate);

        if (fromDepartment != null) {
            jpql.append(" and bi.bill.fromDepartment = :fdept");
            m.put("fdept", fromDepartment);
        }

        if (toDepartment != null) {
            jpql.append(" and bi.bill.toDepartment = :tdept");
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            jpql.append(" and bi.item.category = :cat");
            m.put("cat", category);
        }

        jpql.append(" order by bi.item.name");

        disposalIssueBillItemDtos = (List<PharmacyItemPurchaseDTO>) getBillFacade().findLightsByJpql(jpql.toString(), m, TemporalType.TIMESTAMP);

        // Calculate totals from DTO list
        totalsValue = 0;
        marginValue = 0;
        netTotalValues = 0;
        purchaseValue = 0;
        retailValue = 0;
        
        if (disposalIssueBillItemDtos != null) {
            for (PharmacyItemPurchaseDTO dto : disposalIssueBillItemDtos) {
                totalsValue += dto.getBillTotal() != null ? dto.getBillTotal() : 0.0;
                netTotalValues += dto.getBillNetTotal() != null ? dto.getBillNetTotal() : 0.0;
                marginValue += dto.getMarginValue() != null ? dto.getMarginValue() : 0.0;
                purchaseValue += dto.getPurchaseRate() != null && dto.getQty() != null ? 
                    dto.getPurchaseRate() * dto.getQty() : 0.0;
                retailValue += dto.getRetailRate() != null && dto.getQty() != null ? 
                    dto.getRetailRate() * dto.getQty() : 0.0;
            }
        }

        // Set bill totals (calculated from DTO data)
        billTotal = totalsValue;
        billMargin = marginValue;
        billDiscount = 0.0; // Not available in current DTO
        billNetTotal = netTotalValues;

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
    }

    public void fillItemCountsWithOutMarginPharmacy() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("fillItemCountsWithOutMarginPharmacy()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        // Use DTO-based approach with BillTypeAtomic for disposal issues (without margin/batch details)
        Map<String, Object> m = new HashMap<>();
        List<BillTypeAtomic> bts = new ArrayList<>();
        bts.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE);
        bts.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_CANCELLED);
        bts.add(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_RETURN);

        StringBuilder jpql = new StringBuilder();
        jpql.append("select new com.divudi.core.data.dto.PharmacyItemPurchaseDTO(");
        jpql.append("bi.bill.id, bi.bill.deptId, bi.bill.createdAt, ");
        jpql.append("bi.item.code, bi.item.name, bi.bill.billType, ");
        jpql.append("bi.bill.fromDepartment.name, bi.netValue, bi.qty, bi.grossValue) ");
        jpql.append(" from BillItem bi");
        jpql.append(" where bi.bill.billTypeAtomic in :bts");
        jpql.append(" and bi.bill.createdAt between :fd and :td");
        jpql.append(" and bi.retired = false");
        jpql.append(" and bi.bill.retired = false");
        jpql.append(" and bi.item.retired = false");

        m.put("bts", bts);
        m.put("fd", fromDate);
        m.put("td", toDate);

        if (fromDepartment != null) {
            jpql.append(" and bi.bill.fromDepartment = :fdept");
            m.put("fdept", fromDepartment);
        }

        if (toDepartment != null) {
            jpql.append(" and bi.bill.toDepartment = :tdept");
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            jpql.append(" and bi.item.category = :cat");
            m.put("cat", category);
        }

        jpql.append(" order by bi.item.name");

        disposalIssueBillItemDtos = (List<PharmacyItemPurchaseDTO>) getBillFacade().findLightsByJpql(jpql.toString(), m, TemporalType.TIMESTAMP);

        // Calculate totals from DTO list
        netTotalValues = 0;
        if (disposalIssueBillItemDtos != null) {
            for (PharmacyItemPurchaseDTO dto : disposalIssueBillItemDtos) {
                netTotalValues += dto.getBillNetTotal() != null ? dto.getBillNetTotal() : 0.0;
            }
        }

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

    }

    public void fillItemCountsWithOutMarginStore() {
        Date startTime = new Date();
        fillItemCountsWithOutMargin(BillType.StoreIssue);

    }

    public void fillItemCountsWithOutMargin(BillType bt) {

        List<Object[]> list = fetchBillItemWithOutMargin(bt);
        if (list == null) {
            return;
        }

        itemCountWithOutMargins = new ArrayList<>();
        netTotalValues = 0;

        for (Object[] obj : list) {
            ItemCountWithOutMargin row = new ItemCountWithOutMargin();
            row.setItem((Item) obj[0]);
            row.setNet((Double) obj[1]);

            Double pre = calCountItem(row.getItem(), bt, new PreBill());
            Double preCancel = calCountCanItem(row.getItem(), bt, new PreBill());
            Double returned = calCountReturnItem(row.getItem(), bt, new RefundBill());

            row.setCount(pre - (preCancel + returned));

            netTotalValues += row.getNet();

            itemCountWithOutMargins.add(row);
        }

        billNetTotal = fetchBillNetTotal(bt);

    }

    public void fillItemCountsStore() {
        Date startTime = new Date();

        List<Object[]> list = fetchBillItem(BillType.StoreIssue);

        if (list == null) {
            return;
        }

        itemCounts = new ArrayList<>();
        totalsValue = 0;
        marginValue = 0;
        netTotalValues = 0;
        purchaseValue = 0;
        retailValue = 0;
        for (Object[] obj : list) {
            ItemCount row = new ItemCount();
            row.setItemBatch((ItemBatch) obj[0]);
            row.setGross((Double) obj[1]);
            row.setMargin((Double) obj[2]);
            row.setNet((Double) obj[4]);

            Double pre = calCount(row.getItemBatch(), BillType.StoreIssue, new PreBill());
            Double preCancel = calCountCan(row.getItemBatch(), BillType.StoreIssue, new PreBill());
            Double returned = calCountReturn(row.getItemBatch(), BillType.StoreIssue, new RefundBill());

            row.setCount(pre - (preCancel + returned));

            totalsValue += row.getGross();
            marginValue += row.getMargin();
            netTotalValues += row.getNet();

            itemCounts.add(row);
        }

        billTotal = fetchBillTotal(BillType.StoreIssue);
        billMargin = fetchBillMargin(BillType.StoreIssue);
        billDiscount = fetchBillDiscount(BillType.StoreIssue);
        billNetTotal = fetchBillNetTotal(BillType.StoreIssue);

    }

    public void fillItemCountsBht() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("fillItemCountsBht()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        List<Object[]> list = fetchBillItem(BillType.PharmacyBhtPre, null);

        if (list == null) {
            return;
        }

        itemCounts = new ArrayList<>();
        totalsValue = 0;
        marginValue = 0;
        discountsValue = 0;
        netTotalValues = 0;
        purchaseValue = 0;
        retailValue = 0;
        for (Object[] obj : list) {
            ItemCount row = new ItemCount();
            row.setItemBatch((ItemBatch) obj[0]);
            row.setGross((Double) obj[1]);
            row.setMargin((Double) obj[2]);
            row.setDiscount((Double) obj[3]);
            row.setNet((Double) obj[4]);
            row.setCount((Double) obj[5]);

            totalsValue += row.getGross();
            marginValue += row.getMargin();
            discountsValue += row.getDiscount();
            netTotalValues += row.getNet();

            itemCounts.add(row);
        }

        billTotal = fetchBillTotal(BillType.PharmacyBhtPre);
        billMargin = fetchBillMargin(BillType.PharmacyBhtPre);
        billDiscount = fetchBillDiscount(BillType.PharmacyBhtPre);
        billNetTotal = fetchBillNetTotal(BillType.PharmacyBhtPre);

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

    }

    public void fillItemCountsBhtSurgery() {
        Date startTime = new Date();

        List<Object[]> list = fetchBillItem(BillType.PharmacyBhtPre, SurgeryBillType.PharmacyItem);

        if (list == null) {
            return;
        }

        itemCounts = new ArrayList<>();
        totalsValue = 0;
        marginValue = 0;
        discountsValue = 0;
        netTotalValues = 0;
        purchaseValue = 0;
        retailValue = 0;
        for (Object[] obj : list) {
            ItemCount row = new ItemCount();
            row.setItemBatch((ItemBatch) obj[0]);
            row.setGross((Double) obj[1]);
            row.setMargin((Double) obj[2]);
            row.setDiscount((Double) obj[3]);
            row.setNet((Double) obj[4]);

            Double pre = calCount(row.getItemBatch(), BillType.PharmacyBhtPre, new PreBill());
            Double preCancel = calCountCan(row.getItemBatch(), BillType.PharmacyBhtPre, new PreBill());
            Double returned = calCountReturn(row.getItemBatch(), BillType.PharmacyBhtPre, new RefundBill());

            row.setCount(pre - (preCancel + returned));

            totalsValue += row.getGross();
            marginValue += row.getMargin();
            discountsValue += row.getDiscount();
            netTotalValues += row.getNet();

            itemCounts.add(row);
        }

        billTotal = fetchBillTotal(BillType.PharmacyBhtPre);
        billMargin = fetchBillMargin(BillType.PharmacyBhtPre);
        billDiscount = fetchBillDiscount(BillType.PharmacyBhtPre);
        billNetTotal = fetchBillNetTotal(BillType.PharmacyBhtPre);

    }

    public void fillItemCountsBhtStore() {

        List<Object[]> list = fetchBillItem(BillType.StoreBhtPre);

        if (list == null) {
            return;
        }

        itemCounts = new ArrayList<>();
        totalsValue = 0;
        marginValue = 0;
        discountsValue = 0;
        netTotalValues = 0;
        purchaseValue = 0;
        retailValue = 0;
        for (Object[] obj : list) {
            ItemCount row = new ItemCount();
            row.setItemBatch((ItemBatch) obj[0]);
            row.setGross((Double) obj[1]);
            row.setMargin((Double) obj[2]);
            row.setDiscount((Double) obj[3]);
            row.setNet((Double) obj[4]);

            Double pre = calCount(row.getItemBatch(), BillType.StoreBhtPre, new PreBill());
            Double preCancel = calCountCan(row.getItemBatch(), BillType.StoreBhtPre, new PreBill());
            Double returned = calCountReturn(row.getItemBatch(), BillType.StoreBhtPre, new RefundBill());

            row.setCount(pre - (preCancel + returned));

            totalsValue += row.getGross();
            marginValue += row.getMargin();
            discountsValue += row.getDiscount();
            netTotalValues += row.getNet();

            itemCounts.add(row);
        }

        billTotal = fetchBillTotal(BillType.StoreBhtPre);
        billMargin = fetchBillMargin(BillType.StoreBhtPre);
        billDiscount = fetchBillDiscount(BillType.StoreBhtPre);
        billNetTotal = fetchBillNetTotal(BillType.StoreBhtPre);

    }

    double billTotal;
    double billMargin;
    double billDiscount;
    double billNetTotal;

    private Double calCount(ItemBatch item, BillType billType, Bill bill) {

        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("class", bill.getClass());
        m.put("itm", item);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select abs(sum(b.pharmaceuticalBillItem.qty))"
                + " from BillItem b "
                + " where b.bill.department=:fdept "
                + " and b.bill.billedBill is null "
                + " and type(b.bill)=:class "
                + " and b.pharmaceuticalBillItem.itemBatch=:itm ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.bill.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    private Double calCountReturn(ItemBatch item, BillType billType, Bill bill) {

        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("class", bill.getClass());
        m.put("itm", item);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select abs(sum(b.pharmaceuticalBillItem.qty))"
                + " from BillItem b "
                + " where b.bill.department=:fdept "
                + " and b.bill.billedBill is not null "
                + " and type(b.bill)=:class "
                + " and b.pharmaceuticalBillItem.itemBatch=:itm ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.bill.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    private Double calCountCan(ItemBatch item, BillType billType, Bill bill) {

        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("class", bill.getClass());
        m.put("itm", item);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select abs(sum(b.pharmaceuticalBillItem.qty))"
                + " from BillItem b "
                + " where b.bill.department=:fdept "
                + " and b.bill.billedBill is not null "
                + " and type(b.bill)=:class "
                + " and b.pharmaceuticalBillItem.itemBatch=:itm ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.bill.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    private Double calCountItem(Item item, BillType billType, Bill bill) {

        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("class", bill.getClass());
        m.put("itm", item);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select abs(sum(b.pharmaceuticalBillItem.qty))"
                + " from BillItem b "
                + " where b.bill.department=:fdept "
                + " and b.bill.billedBill is null "
                + " and type(b.bill)=:class "
                + " and b.pharmaceuticalBillItem.itemBatch.item=:itm ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.bill.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    private Double calCountReturnItem(Item item, BillType billType, Bill bill) {

        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("class", bill.getClass());
        m.put("itm", item);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select abs(sum(b.pharmaceuticalBillItem.qty))"
                + " from BillItem b "
                + " where b.bill.department=:fdept "
                + " and b.bill.billedBill is not null "
                + " and type(b.bill)=:class "
                + " and b.pharmaceuticalBillItem.itemBatch.item=:itm ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.bill.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    private Double calCountCanItem(Item item, BillType billType, Bill bill) {

        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("class", bill.getClass());
        m.put("itm", item);
        m.put("bt", billType);
        m.put("fdept", fromDepartment);

        sql = "select abs(sum(b.pharmaceuticalBillItem.qty))"
                + " from BillItem b "
                + " where b.bill.department=:fdept "
                + " and b.bill.billedBill is not null "
                + " and type(b.bill)=:class "
                + " and b.pharmaceuticalBillItem.itemBatch.item=:itm ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        if (category != null) {
            sql += " and b.item.category=:cat";
            m.put("cat", category);

        }

        sql += " and b.bill.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    /**
     * Main method that delegates to either DTO or Entity approach based on
     * configuration
     */
    public void fillDepartmentTransfersRecieveByBill() {
        if (useDtoApproach) {
            fillDepartmentTransfersReceiveByBillDto();
        } else {
            fillDepartmentTransfersReceiveByBillEntity();
        }
    }

    /**
     * DTO-based approach for efficient data retrieval - no calculations needed
     * This is the recommended approach for better performance
     */
    public void fillDepartmentTransfersReceiveByBillDto() {
        reportTimerController.trackReportExecution(() -> {
            fillTransferReceiveBillsDtoDirectly();
        }, DisbursementReports.TRANSFER_RECEIVE_BY_BILL, sessionController.getLoggedUser());
    }

    /**
     * Entity-based approach for backward compatibility Uses the traditional
     * method with iterative calculations
     */
    public void fillDepartmentTransfersReceiveByBillEntity() {
        reportTimerController.trackReportExecution(() -> {
            fillTransferReceiveBillsLegacy();
            calculatePurachaseValuesOfBillItemsInBill(transferBills);
        }, DisbursementReports.TRANSFER_RECEIVE_BY_BILL, sessionController.getLoggedUser());
    }

    /**
     * Direct DTO query with aggregated financial data - follows DTO
     * implementation guidelines This is the primary method that should be used
     * for report display
     *
     * CRITICAL FIX for Issue #15797: Added TYPE(b) and billedBill join to distinguish
     * cancelled receive bills and link them to original issue bills for proper reporting.
     */
    private void fillTransferReceiveBillsDtoDirectly() {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();

        jpql.append("SELECT new com.divudi.core.data.dto.PharmacyTransferReceiveDTO(")
                .append("TYPE(b), ")  // ADDED: Bill class discriminator to identify CancelledBill
                .append("b.id, ")
                .append("COALESCE(b.deptId, ''), ")
                .append("b.createdAt, ")
                .append("COALESCE(b.department.name, ''), ")
                .append("COALESCE(b.fromDepartment.name, ''), ")
                .append("COALESCE(p.name, ''), ")
                .append("COALESCE(b.cancelled, false), ")
                .append("COALESCE(b.refunded, false), ")
                .append("COALESCE(b.comments, ''), ")
                .append("COALESCE(bfd.totalCostValue, 0.0), ")
                .append("COALESCE(bfd.totalPurchaseValue, 0.0), ")
                .append("COALESCE(bfd.lineNetTotal, 0.0), ")
                .append("COALESCE(bfd.totalRetailSaleValue, 0.0), ")
                .append("COALESCE(bb.deptId, ''), ")  // ADDED: Original bill deptId for cancellations
                .append("bb.id")  // ADDED: Original bill id for cancellations
                .append(") ")
                .append("FROM Bill b ")
                .append("LEFT JOIN b.billFinanceDetails bfd ")
                .append("LEFT JOIN b.fromStaff fs ")
                .append("LEFT JOIN fs.person p ")
                .append("LEFT JOIN b.billedBill bb ")  // ADDED: Join to original issue bill for traceability
                .append("WHERE b.billType = :bt ")
                .append("AND b.retired = false ")
                .append("AND b.createdAt BETWEEN :fd AND :td ");

        params.put("fd", fromDate);
        params.put("td", toDate);
        params.put("bt", BillType.PharmacyTransferReceive);

        if (fromDepartment != null) {
            jpql.append("AND b.fromDepartment = :fdept ");
            params.put("fdept", fromDepartment);
        }

        if (toDepartment != null) {
            jpql.append("AND b.department = :tdept ");
            params.put("tdept", toDepartment);
        }

        jpql.append("ORDER BY b.id");

        // Execute the DTO query
        try {
            transferReceiveDtos = (List<PharmacyTransferReceiveDTO>) getBillFacade().findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
        } catch (Exception e) {
            String msg = "Failed to fetch transfer receive DTOs";
            String context = " From: "
                    + (fromDepartment == null ? "All" : fromDepartment.getName())
                    + " To: "
                    + (toDepartment == null ? "All" : toDepartment.getName())
                    + " Between: " + fromDate + " and " + toDate;
            Logger.getLogger(ReportsTransfer.class.getName()).log(Level.SEVERE, msg + context, e);
            JsfUtil.addErrorMessage(e, msg);
            transferReceiveDtos = new ArrayList<>();
        }
        // Calculate totals from DTOs
        saleValue = 0.0;
        costValue = 0.0;
        purchaseValue = 0.0;
        transferValue = 0.0;
        if (transferReceiveDtos != null) {
            for (PharmacyTransferReceiveDTO dto : transferReceiveDtos) {
                if (dto.getSaleValue() != null) {
                    saleValue += dto.getSaleValue();
                }
                if (dto.getPurchaseValue() != null) {
                    purchaseValue += dto.getPurchaseValue();
                }
                if (dto.getCostValue() != null) {
                    costValue += dto.getCostValue().doubleValue();
                }
                if (dto.getTransferValueDouble() != null) {
                    transferValue += dto.getTransferValueDouble();
                }
            }
        }
    }

    /**
     * Legacy entity-based approach for transfer receive bills
     *
     * @deprecated Use fillTransferReceiveBillsDtoDirectly() instead
     */
    @Deprecated
    private void fillTransferReceiveBillsLegacy() {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder("select b from Bill b where b.createdAt between :fd and :td and b.billType=:bt");
        params.put("fd", fromDate);
        params.put("td", toDate);
        params.put("bt", BillType.PharmacyTransferReceive);

        if (fromDepartment != null) {
            jpql.append(" and b.fromDepartment=:fdept");
            params.put("fdept", fromDepartment);
        }
        if (toDepartment != null) {
            jpql.append(" and b.department=:tdept");
            params.put("tdept", toDepartment);
        }

        jpql.append(" order by b.id");
        transferBills = getBillFacade().findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

        totalsValue = 0.0;
        discountsValue = 0.0;
        netTotalValues = 0.0;
        for (Bill b : transferBills) {
            discountsValue = discountsValue + b.getDiscount();
            netTotalValues = netTotalValues + b.getNetTotal();
        }
    }

    public void fillTheaterTransfersReceiveWithBHTIssue() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("fillTheaterTransfersReceiveWithBHTIssue()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        if (fromDepartment == null || toDepartment == null) {
            JsfUtil.addErrorMessage("Please Check From To Departments");
            return;
        }
        itemBHTIssueCountTrancerReciveCounts = new ArrayList<>();
        totalIssueQty = 0.0;
        totalBHTIssueQty = 0.0;
        totalIssueValue = 0.0;
        totalBHTIssueValue = 0.0;
        for (Item i : fetchStockItems()) {
            ItemBHTIssueCountTrancerReciveCount count = new ItemBHTIssueCountTrancerReciveCount();
            count.setI(i);
            List<Object[]> object = fetchItemDetails(i);
            double qty;
            try {
                qty = (double) object.get(0)[0];
                totalIssueQty += qty;
            } catch (Exception e) {
                qty = 0.0;
            }
            double totalValue;
            try {
                totalValue = (double) object.get(0)[1];
                totalIssueValue += totalValue;
            } catch (Exception e) {
                totalValue = 0.0;
            }
            count.setCountIssue(qty);
            count.setTotalIssue(totalValue);
            List<Object[]> objectBHT = fetchBHTIsssue(BillType.PharmacyBhtPre, i);
            double qtyBHT;
            try {
                qtyBHT = (double) objectBHT.get(0)[0];
                totalBHTIssueQty += qtyBHT;
            } catch (Exception e) {
                qtyBHT = 0.0;
            }
            double totalBHTValue;
            try {
                totalBHTValue = (double) objectBHT.get(0)[1];
                totalBHTIssueValue += totalBHTValue;
            } catch (Exception e) {
                totalBHTValue = 0.0;
            }
            count.setCountBht(qtyBHT);
            count.setTotalBht(totalBHTValue);
            itemBHTIssueCountTrancerReciveCounts.add(count);
        }

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

    }

    public List<Item> fetchStockItems() {

        Map m = new HashMap();
        String sql;
        sql = "select distinct(s.itemBatch.item) from Stock s "
                + " where s.department=:d "
                + " order by s.itemBatch.item.name";
        m.put("d", toDepartment);
        List<Item> items = getItemFacade().findByJpql(sql, m);
        return items;
    }

    public List<Object[]> fetchItemDetails(Item i) {
        Map m = new HashMap();
        String sql;

        sql = "select sum(bi.pharmaceuticalBillItem.qty),sum(bi.pharmaceuticalBillItem.itemBatch.purcahseRate*bi.pharmaceuticalBillItem.qty) "
                + " from BillItem bi "
                + " where bi.bill.fromDepartment=:fdept "
                + " and bi.bill.department=:tdept "
                + " and bi.bill.createdAt between :fd and :td "
                + " and bi.bill.billType=:bt "
                + " and bi.item=:i ";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.PharmacyTransferReceive);
        m.put("fdept", fromDepartment);
        m.put("tdept", toDepartment);
        m.put("i", i);

        return getItemFacade().findObjectsArrayByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public List<Object[]> fetchBHTIsssue(BillType billType, Item i) {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", billType);
        m.put("fdept", toDepartment);
        m.put("i", i);

        sql = "select sum(b.pharmaceuticalBillItem.qty),sum(b.pharmaceuticalBillItem.qty*b.pharmaceuticalBillItem.itemBatch.purcahseRate) "
                + " from BillItem b "
                + " where b.bill.department=:fdept "
                + " and b.bill.createdAt between :fd and :td "
                + " and b.bill.billType=:bt "
                + " and b.item=:i ";

        return getBillFacade().findObjectsArrayByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public List<ItemBHTIssueCountTrancerReciveCount> getItemBHTIssueCountTrancerReciveCounts() {
        return itemBHTIssueCountTrancerReciveCounts;
    }

    public void setItemBHTIssueCountTrancerReciveCounts(List<ItemBHTIssueCountTrancerReciveCount> itemBHTIssueCountTrancerReciveCounts) {
        this.itemBHTIssueCountTrancerReciveCounts = itemBHTIssueCountTrancerReciveCounts;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public Bill getPreviewBill() {
        return previewBill;
    }

    public void setPreviewBill(Bill previewBill) {
        this.previewBill = previewBill;
    }

    public double getNetTotalSaleValues() {
        return netTotalSaleValues;
    }

    public void setNetTotalSaleValues(double netTotalSaleValues) {
        this.netTotalSaleValues = netTotalSaleValues;
    }

    public double getNetTotalPurchaseValues() {
        return netTotalPurchaseValues;
    }

    public void setNetTotalPurchaseValues(double netTotalPurchaseValues) {
        this.netTotalPurchaseValues = netTotalPurchaseValues;
    }

    public double getCostValue() {
        return costValue;
    }

    public void setCostValue(double costValue) {
        this.costValue = costValue;
    }

    public double getTransferValue() {
        return transferValue;
    }

    public void setTransferValue(double transferValue) {
        this.transferValue = transferValue;
    }

    public double getNetTotalCostValues() {
        return netTotalCostValues;
    }

    public void setNetTotalCostValues(double netTotalCostValues) {
        this.netTotalCostValues = netTotalCostValues;
    }

    public class ItemBHTIssueCountTrancerReciveCount {

        private Item i;
        private double countIssue;
        private double countBht;
        private double totalIssue;
        private double totalBht;

        public Item getI() {
            return i;
        }

        public void setI(Item i) {
            this.i = i;
        }

        public double getCountIssue() {
            return countIssue;
        }

        public void setCountIssue(double countIssue) {
            this.countIssue = countIssue;
        }

        public double getCountBht() {
            return countBht;
        }

        public void setCountBht(double countBht) {
            this.countBht = countBht;
        }

        public double getTotalIssue() {
            return totalIssue;
        }

        public void setTotalIssue(double totalIssue) {
            this.totalIssue = totalIssue;
        }

        public double getTotalBht() {
            return totalBht;
        }

        public void setTotalBht(double totalBht) {
            this.totalBht = totalBht;
        }

    }

    /**
     * Getters & Setters
     *
     * @return
     */
    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<Stock> getStocks() {
        return stocks;
    }

    public void setStocks(List<Stock> stocks) {
        this.stocks = stocks;
    }

    public StockFacade getStockFacade() {
        return stockFacade;
    }

    public void setStockFacade(StockFacade stockFacade) {
        this.stockFacade = stockFacade;
    }

    /**
     * Constructor
     */
    public ReportsTransfer() {
    }

    public double getSaleValue() {
        return saleValue;
    }

    public void setSaleValue(double saleValue) {
        this.saleValue = saleValue;
    }

    public double getStockPurchaseValue() {
        return stockPurchaseValue;
    }

    public void setStockPurchaseValue(double stockPurchaseValue) {
        this.stockPurchaseValue = stockPurchaseValue;
    }

    public double getStockSaleValue() {
        return stockSaleValue;
    }

    public void setStockSaleValue(double stockSaleValue) {
        this.stockSaleValue = stockSaleValue;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public List<BillItem> getTransferItems() {
        return transferItems;
    }

    public void setTransferItems(List<BillItem> transferItems) {
        this.transferItems = transferItems;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfMonth();
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

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public double getPurchaseValue() {
        return purchaseValue;
    }

    public void setPurchaseValue(double purchaseValue) {
        this.purchaseValue = purchaseValue;
    }

    public List<StockReportRecord> getMovementRecords() {
        return movementRecords;
    }

    public void setMovementRecords(List<StockReportRecord> movementRecords) {
        this.movementRecords = movementRecords;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public List<StockReportRecord> getMovementRecordsQty() {
        return movementRecordsQty;
    }

    public void setMovementRecordsQty(List<StockReportRecord> movementRecordsQty) {
        this.movementRecordsQty = movementRecordsQty;
    }

    public List<Bill> getTransferBills() {
        return transferBills;
    }

    public void setTransferBills(List<Bill> transferBills) {
        this.transferBills = transferBills;
    }

    public List<PharmacyTransferIssueBillDTO> getTransferIssueDtos() {
        return transferIssueDtos;
    }

    public void setTransferIssueDtos(List<PharmacyTransferIssueBillDTO> transferIssueDtos) {
        this.transferIssueDtos = transferIssueDtos;
    }

    public List<PharmacyTransferReceiveDTO> getTransferReceiveDtos() {
        return transferReceiveDtos;
    }

    public void setTransferReceiveDtos(List<PharmacyTransferReceiveDTO> transferReceiveDtos) {
        this.transferReceiveDtos = transferReceiveDtos;
    }

    public List<PharmacyTransferIssueBillItemDTO> getTransferIssueBillItemDtos() {
        return transferIssueBillItemDtos;
    }

    public void setTransferIssueBillItemDtos(List<PharmacyTransferIssueBillItemDTO> transferIssueBillItemDtos) {
        this.transferIssueBillItemDtos = transferIssueBillItemDtos;
    }

    public List<PharmacyTransferReceiveBillItemDTO> getTransferReceiveBillItemDtos() {
        return transferReceiveBillItemDtos;
    }

    public void setTransferReceiveBillItemDtos(List<PharmacyTransferReceiveBillItemDTO> transferReceiveBillItemDtos) {
        this.transferReceiveBillItemDtos = transferReceiveBillItemDtos;
    }

    public boolean isUseDtoApproach() {
        return useDtoApproach;
    }

    public void setUseDtoApproach(boolean useDtoApproach) {
        this.useDtoApproach = useDtoApproach;
    }

    public BillFacade getBillFacade() {
        return BillFacade;
    }

    public void setBillFacade(BillFacade BillFacade) {
        this.BillFacade = BillFacade;
    }

    public double getTotalsValue() {
        return totalsValue;
    }

    public void setTotalsValue(double totalsValue) {
        this.totalsValue = totalsValue;
    }

    public double getDiscountsValue() {
        return discountsValue;
    }

    public void setDiscountsValue(double discountsValue) {
        this.discountsValue = discountsValue;
    }

    public double getNetTotalValues() {
        return netTotalValues;
    }

    public void setNetTotalValues(double netTotalValues) {
        this.netTotalValues = netTotalValues;
    }

    public double getTotalCostValue() {
        return costValue;
    }

    public double getTotalTransferValue() {
        return transferValue;
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

    public class ItemCount {

        ItemBatch itemBatch;
        double count;
        double gross;
        double margin;
        double discount;
        double net;

        public ItemBatch getItemBatch() {
            return itemBatch;
        }

        public void setItemBatch(ItemBatch itemBatch) {
            this.itemBatch = itemBatch;
        }

        public double getDiscount() {
            return discount;
        }

        public void setDiscount(double discount) {
            this.discount = discount;
        }

        public double getCount() {
            return count;
        }

        public void setCount(double count) {
            this.count = count;
        }

        public double getGross() {
            return gross;
        }

        public void setGross(double gross) {
            this.gross = gross;
        }

        public double getMargin() {
            return margin;
        }

        public void setMargin(double margin) {
            this.margin = margin;
        }

        public double getNet() {
            return net;
        }

        public void setNet(double net) {
            this.net = net;
        }

    }

    public class ItemCountWithOutMargin {

        Item item;
        double count;
        double net;

        public Item getItem() {
            return item;
        }

        public void setItem(Item item) {
            this.item = item;
        }

        public double getCount() {
            return count;
        }

        public void setCount(double count) {
            this.count = count;
        }

        public double getNet() {
            return net;
        }

        public void setNet(double net) {
            this.net = net;
        }

    }

    public List<ItemCountWithOutMargin> getItemCountWithOutMargins() {
        return itemCountWithOutMargins;
    }

    public void setItemCountWithOutMargins(List<ItemCountWithOutMargin> itemCountWithOutMargins) {
        this.itemCountWithOutMargins = itemCountWithOutMargins;
    }

    public List<ItemCount> getItemCounts() {
        return itemCounts;
    }

    public void setItemCounts(List<ItemCount> itemCounts) {
        this.itemCounts = itemCounts;
    }

    public double getMarginValue() {
        return marginValue;
    }

    public void setMarginValue(double marginValue) {
        this.marginValue = marginValue;
    }

    public double getBillTotal() {
        return billTotal;
    }

    public void setBillTotal(double billTotal) {
        this.billTotal = billTotal;
    }

    public double getBillMargin() {
        return billMargin;
    }

    public void setBillMargin(double billMargin) {
        this.billMargin = billMargin;
    }

    public double getBillDiscount() {
        return billDiscount;
    }

    public void setBillDiscount(double billDiscount) {
        this.billDiscount = billDiscount;
    }

    public double getBillNetTotal() {
        return billNetTotal;
    }

    public void setBillNetTotal(double billNetTotal) {
        this.billNetTotal = billNetTotal;
    }

    public double getRetailValue() {
        return retailValue;
    }

    public void setRetailValue(double retailValue) {
        this.retailValue = retailValue;
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

    public double getTotalIssueQty() {
        return totalIssueQty;
    }

    public void setTotalIssueQty(double totalIssueQty) {
        this.totalIssueQty = totalIssueQty;
    }

    public double getTotalBHTIssueQty() {
        return totalBHTIssueQty;
    }

    public void setTotalBHTIssueQty(double totalBHTIssueQty) {
        this.totalBHTIssueQty = totalBHTIssueQty;
    }

    public double getTotalIssueValue() {
        return totalIssueValue;
    }

    public void setTotalIssueValue(double totalIssueValue) {
        this.totalIssueValue = totalIssueValue;
    }

    public double getTotalBHTIssueValue() {
        return totalBHTIssueValue;
    }

    public void setTotalBHTIssueValue(double totalBHTIssueValue) {
        this.totalBHTIssueValue = totalBHTIssueValue;
    }

    // Getter and setter methods for disposal DTO lists
    public List<PharmacyItemPurchaseDTO> getDisposalIssueBillDtos() {
        return disposalIssueBillDtos;
    }

    public void setDisposalIssueBillDtos(List<PharmacyItemPurchaseDTO> disposalIssueBillDtos) {
        this.disposalIssueBillDtos = disposalIssueBillDtos;
    }

    public List<PharmacyItemPurchaseDTO> getDisposalIssueBillItemDtos() {
        return disposalIssueBillItemDtos;
    }

    public void setDisposalIssueBillItemDtos(List<PharmacyItemPurchaseDTO> disposalIssueBillItemDtos) {
        this.disposalIssueBillItemDtos = disposalIssueBillItemDtos;
    }

}
