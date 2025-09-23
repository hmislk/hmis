/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ReportTimerController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.reports.PharmacyReports;
import com.divudi.core.data.reports.SummaryReports;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyErrorCheckingEjb;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.pharmacy.StockHistory;
import com.divudi.core.data.dto.PharmacyBinCardDTO;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author ruhunu
 */
@Named
@SessionScoped
public class PharmacyErrorChecking implements Serializable {

    @EJB
    PharmacyErrorCheckingEjb ejb;

    @Inject
    StockHistoryController stockHistoryController;

    @Inject
    ReportTimerController reportTimerController;

    List<BillItem> billItems;
    private List<StockHistory> stockHistories;
    private List<PharmacyBinCardDTO> binCardEntries;
    //private ReportTimerController reportTimerController;
    Date fromDate;
    Date toDate;
    Item item;
    List<Bill> mismatchPreBills;
    Department department;
    double calculatedStock;
    double calculatedSaleValue;
    double calculatedPurchaseValue;
    double currentStock;
    double currentSaleValue;
    double currentPurchaseValue;
    @Named
    @Inject
    private SessionController sessionController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    public void listMismatchPreBills() {
        mismatchPreBills = getEjb().errPreBills(department);
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public void listPharmacyMovement() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        billItems = getEjb().allBillItems(item, department);
        calculateTotals4();

    }

    public void processBinCardItems() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        billItems = getEjb().allBillItems(item, department);
        calculateTotals4();

    }

    public void processBinCard() {
        reportTimerController.trackReportExecution(() -> {
            binCardEntries = stockHistoryController.findBinCardDTOs(fromDate, toDate, null, department, item);

            if (configOptionApplicationController.getBooleanValueByKey("Pharmacy Bin Card - Hide Adjustment Bills in Bin Card",true)) {
                List<BillType> bts = new ArrayList<>();
                bts.add(BillType.PharmacyAdjustmentSaleRate);

                Iterator<PharmacyBinCardDTO> iterator = binCardEntries.iterator();
                while (iterator.hasNext()) {
                    PharmacyBinCardDTO dto = iterator.next();
                    if (dto.getBillType() != null && bts.contains(dto.getBillType())) {
                        iterator.remove();
                    }
                }
            }

        }, PharmacyReports.PHARMACY_BIN_CARD, sessionController.getLoggedUser());
    }

    public void listPharmacyMovementByDateRange() {
        billItems = getEjb().allBillItemsByDate(item, department, fromDate, toDate);
    }

    public void listPharmacyMovementByDateRangeOnlyStockChange() {
        Date startTime = new Date();

        billItems = getEjb().allBillItemsByDateOnlyStock(item, department, fromDate, toDate);

    }

    public void listPharmacyMovementNew() {
        billItems = getEjb().allBillItems(item, department);
        calculateTotals4();
    }

    @EJB
    private BillFacade billFacade;
    @EJB
    private PharmacyBean pharmacyBean;

    public double getItemStock() {
        return getPharmacyBean().getStockQty(item, department);
    }

    public void calculateTotals2() {
        calculatedStock = 0.0;
        calculatedSaleValue = 0.0;
        calculatedPurchaseValue = 0.0;
        currentStock = 0.0;
        currentSaleValue = 0.0;
        currentPurchaseValue = 0.0;

        calculatedStock += getEjb().getTotalQty(BillType.PharmacyGrnBill, new BilledBill(), department, item);
        calculatedStock -= getEjb().getTotalQty(BillType.PharmacyGrnBill, new CancelledBill(), department, item);

        calculatedStock -= getEjb().getTotalQty(BillType.PharmacyGrnReturn, new BilledBill(), department, item);
        calculatedStock += getEjb().getTotalQty(BillType.PharmacyGrnReturn, new CancelledBill(), department, item);

        calculatedStock += getEjb().getTotalQty(BillType.PharmacyPurchaseBill, new BilledBill(), department, item);
        calculatedStock -= getEjb().getTotalQty(BillType.PharmacyPurchaseBill, new CancelledBill(), department, item);

        calculatedStock -= getEjb().getTotalQty(BillType.PurchaseReturn, new BilledBill(), department, item);
        calculatedStock += getEjb().getTotalQty(BillType.PurchaseReturn, new CancelledBill(), department, item);

        calculatedStock -= getEjb().getTotalQtyPreDiduction(BillType.PharmacyPre, new PreBill(), department, item);
        //Re Add to Stock of Pre Bill
        //  calculatedStock += getEjb().getTotalQtyPreAdd(BillType.PharmacyPre, new PreBill(), department, item);
        calculatedStock += getEjb().getTotalQty(BillType.PharmacyPre, new RefundBill(), department, item);
        //    calculatedStock -= getEjb().getTotalQtyByBillItem(BillType.PharmacySale, new BilledBill(), department, item);
        //    calculatedStock += getEjb().getTotalQtyByBillItem(BillType.PharmacySale, new CancelledBill(), department, item);
        //    calculatedStock += getEjb().getTotalQtyByBillItem(BillType.PharmacySale, new RefundBill(), department, item);

        calculatedStock -= getEjb().getTotalQty(BillType.PharmacyTransferIssue, new BilledBill(), department, item);
        calculatedStock += getEjb().getTotalQty(BillType.PharmacyTransferIssue, new CancelledBill(), department, item);

        calculatedStock += getEjb().getTotalQty(BillType.PharmacyTransferReceive, new BilledBill(), department, item);
        calculatedStock -= getEjb().getTotalQty(BillType.PharmacyTransferReceive, new CancelledBill(), department, item);

    }

    public void calculateTotals4() {
        calculatedStock = 0.0;
        calculatedSaleValue = 0.0;
        calculatedPurchaseValue = 0.0;
        currentStock = 0.0;
        currentSaleValue = 0.0;
        currentPurchaseValue = 0.0;

        for (BillItem bi : billItems) {

            if (bi.getQty() != bi.getPharmaceuticalBillItem().getQty()) {
                //////// // System.out.println("Error in qty " + bi);
            }

            if (bi.getBill().getCreatedAt() == null) {
                if (bi.isRetired() == true && bi.getBill().isRetired() == true) {

                } else {
                    continue;
                }
            }

            switch (bi.getBill().getBillType()) {
                case PharmacyGrnBill:
                case PharmacyPurchaseBill:
                case PharmacyTransferReceive:
                    if (bi.getBill() instanceof BilledBill) {
                        calculatedStock += Math.abs(bi.getPharmaceuticalBillItem().getQtyInUnit());
                        calculatedStock += Math.abs(bi.getPharmaceuticalBillItem().getFreeQtyInUnit());
                        //System.err.println("1 " + Math.abs(bi.getPharmaceuticalBillItem().getQtyInUnit()));
                        //System.err.println("2 " + Math.abs(bi.getPharmaceuticalBillItem().getFreeQtyInUnit()));
                    } else if (bi.getBill() instanceof CancelledBill || bi.getBill() instanceof RefundBill) {
                        calculatedStock -= Math.abs(bi.getPharmaceuticalBillItem().getQtyInUnit());
                        calculatedStock -= Math.abs(bi.getPharmaceuticalBillItem().getFreeQtyInUnit());
                        //System.err.println("3 " + Math.abs(bi.getPharmaceuticalBillItem().getQtyInUnit()));
                        //System.err.println("4 " + Math.abs(bi.getPharmaceuticalBillItem().getFreeQtyInUnit()));
                    }
                    break;
                case PharmacyGrnReturn:
                case PurchaseReturn:
                case PharmacyTransferIssue:
                    if (bi.getBill() instanceof BilledBill) {
                        calculatedStock -= Math.abs(bi.getPharmaceuticalBillItem().getQtyInUnit());
                        calculatedStock -= Math.abs(bi.getPharmaceuticalBillItem().getFreeQtyInUnit());
                        //System.err.println("5 " + Math.abs(bi.getPharmaceuticalBillItem().getQtyInUnit()));
                        //System.err.println("6 " + Math.abs(bi.getPharmaceuticalBillItem().getFreeQtyInUnit()));
                    } else if (bi.getBill() instanceof CancelledBill || bi.getBill() instanceof RefundBill) {
                        calculatedStock += Math.abs(bi.getPharmaceuticalBillItem().getQtyInUnit());
                        calculatedStock += Math.abs(bi.getPharmaceuticalBillItem().getFreeQtyInUnit());
                        //System.err.println("7 " + Math.abs(bi.getPharmaceuticalBillItem().getQtyInUnit()));
                        //System.err.println("8 " + Math.abs(bi.getPharmaceuticalBillItem().getFreeQtyInUnit()));
                    }
                    break;
                case PharmacySale:
                    if (bi.getBill() instanceof BilledBill) {
                        if (bi.getBill().getReferenceBill() == null) {
                            break;
                        }
                        calculatedStock -= Math.abs(bi.getPharmaceuticalBillItem().getQtyInUnit());
                        //System.err.println("9 " + Math.abs(bi.getPharmaceuticalBillItem().getQtyInUnit()));

                    } else if (bi.getBill() instanceof CancelledBill || bi.getBill() instanceof RefundBill) {
                        calculatedStock += Math.abs(bi.getPharmaceuticalBillItem().getQtyInUnit());

                        //System.err.println("10 " + Math.abs(bi.getPharmaceuticalBillItem().getQtyInUnit()));
                    }
                    break;
                default:
                //System.err.println("Default  " + bi.getBill().getBillType());
                //System.err.println("Default  " + bi.getBill().getClass());
                //System.err.println("Default  " + bi.getQty());

            }

            //////// // System.out.println("calculatedStock = " + calculatedStock + " " + bi.getBill().getBillType() + " " + bi.getBill().getClass() + " " + bi.getId());
//
            //
            //
            //
            //
        }

//        //System.err.println("Befor " + calculatedStock);
//        double saleQty = 0;
//        for (BillItem bi : getEjb().getPreSaleBillItems(BillType.PharmacyPre, new PreBill(), department, item)) {
//
//            if (bi.getQty() != null) {
//                calculatedStock -= Math.abs(bi.getQty());
//                saleQty += Math.abs(bi.getQty());
//            }
//
//        }
//        // calculatedStock -= saleQty;
//        //System.err.println("SaleQty " + saleQty);
//
//        calculatedStock += getEjb().getTotalQty(BillType.PharmacyPre, new RefundBill(), department, item);
    }

    public void calculateTotals3() {
        calculatedStock = 0.0;
        calculatedSaleValue = 0.0;
        calculatedPurchaseValue = 0.0;
        currentStock = 0.0;
        currentSaleValue = 0.0;
        currentPurchaseValue = 0.0;

        for (BillItem bi : getEjb().getTotalBillItems(department, item)) {
            switch (bi.getBill().getBillType()) {
                case PharmacyGrnBill:
                case PharmacyPurchaseBill:
                case PharmacyTransferReceive:
                    if (bi.getBill() instanceof BilledBill) {
                        calculatedStock += Math.abs(bi.getPharmaceuticalBillItem().getQtyInUnit());
                        calculatedStock += Math.abs(bi.getPharmaceuticalBillItem().getFreeQtyInUnit());
                        //System.err.println("1 " + Math.abs(bi.getPharmaceuticalBillItem().getQtyInUnit()));
                        //System.err.println("2 " + Math.abs(bi.getPharmaceuticalBillItem().getFreeQtyInUnit()));
                    } else {
                        calculatedStock -= Math.abs(bi.getPharmaceuticalBillItem().getQtyInUnit());
                        calculatedStock -= Math.abs(bi.getPharmaceuticalBillItem().getFreeQtyInUnit());
                        //System.err.println("3 " + Math.abs(bi.getPharmaceuticalBillItem().getQtyInUnit()));
                        //System.err.println("4 " + Math.abs(bi.getPharmaceuticalBillItem().getFreeQtyInUnit()));
                    }
                    break;
                case PharmacyGrnReturn:
                case PurchaseReturn:
                case PharmacyTransferIssue:
                    if (bi.getBill() instanceof BilledBill) {
                        calculatedStock -= Math.abs(bi.getPharmaceuticalBillItem().getQtyInUnit());
                        calculatedStock -= Math.abs(bi.getPharmaceuticalBillItem().getFreeQtyInUnit());
                        //System.err.println("5 " + Math.abs(bi.getPharmaceuticalBillItem().getQtyInUnit()));
                        //System.err.println("6 " + Math.abs(bi.getPharmaceuticalBillItem().getFreeQtyInUnit()));
                    } else {
                        calculatedStock += Math.abs(bi.getPharmaceuticalBillItem().getQtyInUnit());
                        calculatedStock += Math.abs(bi.getPharmaceuticalBillItem().getFreeQtyInUnit());
                        //System.err.println("7 " + Math.abs(bi.getPharmaceuticalBillItem().getQtyInUnit()));
                        //System.err.println("8 " + Math.abs(bi.getPharmaceuticalBillItem().getFreeQtyInUnit()));
                    }
                    break;
            }
        }

        //System.err.println("Befor " + calculatedStock);
        double saleQty = 0;
        for (BillItem bi : getEjb().getPreSaleBillItems(BillType.PharmacyPre, new PreBill(), department, item)) {

            if (bi.getQty() != null) {
                calculatedStock -= Math.abs(bi.getQty());
                saleQty += Math.abs(bi.getQty());
            }

        }

        // calculatedStock -= saleQty;
        //System.err.println("SaleQty " + saleQty);
        calculatedStock += getEjb().getTotalQty(BillType.PharmacyPre, new RefundBill(), department, item);

    }

    public void calculateTotals(List<BillItem> bis) {
        calculatedStock = 0.0;
        calculatedSaleValue = 0.0;
        calculatedPurchaseValue = 0.0;
        currentStock = 0.0;
        currentSaleValue = 0.0;
        currentPurchaseValue = 0.0;
        for (BillItem bi : bis) {

            if (bi.getBill() instanceof PreBill) {

                switch (bi.getBill().getBillType()) {
                    case PharmacySale:
                    case PharmacyIssue:
                    case PharmacyTransferIssue:
                        calculatedStock = calculatedStock - bi.getQty();
                        break;

                    case PharmacyTransferReceive:
                        calculatedStock = calculatedStock + bi.getQty();
                        break;

                }

            } else if (bi.getBill() instanceof CancelledBill || bi.getBill() instanceof RefundBill) {

                switch (bi.getBill().getBillType()) {
                    case PharmacySale:
                    case PharmacyIssue:
                    case PharmacyGrnReturn:
                    case PharmacyTransferIssue:
                        calculatedStock = calculatedStock + bi.getQty();
                        break;

                    case PharmacyGrnBill:
                    case PharmacyPurchaseBill:
                    case PharmacyTransferReceive:
                        calculatedStock = calculatedStock - bi.getQty();
                        break;

                }

            } else if (bi.getBill() instanceof BilledBill) {

                switch (bi.getBill().getBillType()) {
                    case PharmacyGrnReturn:
                        calculatedStock = calculatedStock - bi.getQty();
                        break;

                    case PharmacyGrnBill:
                    case PharmacyPurchaseBill:
                        calculatedStock = calculatedStock + bi.getQty();
                        break;

                }

            }

        }
    }

    /**
     * Creates a new instance of ItemMovementReportController
     */
    public PharmacyErrorChecking() {
    }

    public PharmacyErrorCheckingEjb getEjb() {
        return ejb;
    }

    public void setEjb(PharmacyErrorCheckingEjb ejb) {
        this.ejb = ejb;
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
            toDate = CommonFunctions.getEndOfDay();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public List<Bill> getMismatchPreBills() {
        return mismatchPreBills;
    }

    public void setMismatchPreBills(List<Bill> mismatchPreBills) {
        this.mismatchPreBills = mismatchPreBills;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public double getCalculatedStock() {
        return calculatedStock;
    }

    public void setCalculatedStock(double calculatedStock) {
        this.calculatedStock = calculatedStock;
    }

    public double getCalculatedSaleValue() {
        return calculatedSaleValue;
    }

    public void setCalculatedSaleValue(double calculatedSaleValue) {
        this.calculatedSaleValue = calculatedSaleValue;
    }

    public double getCalculatedPurchaseValue() {
        return calculatedPurchaseValue;
    }

    public void setCalculatedPurchaseValue(double calculatedPurchaseValue) {
        this.calculatedPurchaseValue = calculatedPurchaseValue;
    }

    public double getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(double currentStock) {
        this.currentStock = currentStock;
    }

    public double getCurrentSaleValue() {
        return currentSaleValue;
    }

    public void setCurrentSaleValue(double currentSaleValue) {
        this.currentSaleValue = currentSaleValue;
    }

    public double getCurrentPurchaseValue() {
        return currentPurchaseValue;
    }

    public void setCurrentPurchaseValue(double currentPurchaseValue) {
        this.currentPurchaseValue = currentPurchaseValue;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public List<StockHistory> getStockHistories() {
        return stockHistories;
    }

    public void setStockHistories(List<StockHistory> stockHistories) {
        this.stockHistories = stockHistories;
    }

    public List<PharmacyBinCardDTO> getBinCardEntries() {
        return binCardEntries;
    }

    public void setBinCardEntries(List<PharmacyBinCardDTO> binCardEntries) {
        this.binCardEntries = binCardEntries;
    }

}
