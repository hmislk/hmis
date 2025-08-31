package com.divudi.service.pharmacy;

import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.WebUserFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import java.util.Date;
import java.util.List;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class StockTakeApprovalService {

    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private StockFacade stockFacade;
    @EJB
    private WebUserFacade webUserFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private ApprovalProgressTracker progressTracker;

    @Asynchronous
    public void approvePhysicalCountAsync(Long physicalCountBillId, Long approverUserId, String jobId) {
        Bill physicalCountBill = billFacade.find(physicalCountBillId);
        if (physicalCountBill == null) {
            return;
        }
        Department dept = physicalCountBill.getDepartment();
        WebUser approver = approverUserId != null ? webUserFacade.find(approverUserId) : null;

        List<BillItem> items = physicalCountBill.getBillItems();
        int total = items == null ? 0 : items.size();
        progressTracker.start(jobId, total, "Preparing adjustment bill");

        Bill adjustmentBill = new Bill();
        adjustmentBill.setBillType(BillType.PharmacyStockAdjustmentBill);
        adjustmentBill.setBillClassType(BillClassType.BilledBill);
        adjustmentBill.setDepartment(dept);
        adjustmentBill.setInstitution(dept != null ? dept.getInstitution() : null);
        Date now = new Date();
        adjustmentBill.setBillDate(now);
        adjustmentBill.setBillTime(now);
        adjustmentBill.setCreatedAt(now);
        adjustmentBill.setCreater(approver);
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(dept, BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT_BILL);
        adjustmentBill.setDeptId(deptId);
        adjustmentBill.setInsId(deptId);
        adjustmentBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT_BILL);
        adjustmentBill.setFromDepartment(dept);
        adjustmentBill.setFromInstitution(dept != null ? dept.getInstitution() : null);
        adjustmentBill.setBackwardReferenceBill(physicalCountBill);
        physicalCountBill.setForwardReferenceBill(adjustmentBill);
        billFacade.create(adjustmentBill);

        int processed = 0;
        if (items != null) {
            for (BillItem bi : items) {
                processed++;
                progressTracker.step(jobId, processed, "Posting adjustment " + processed + "/" + total);
                if (bi == null) continue;
                double variance = bi.getAdjustedValue();
                if (variance == 0) {
                    continue;
                }
                BillItem abi = new BillItem();
                abi.setBill(adjustmentBill);
                abi.setItem(bi.getItem());
                abi.setQty(variance);
                abi.setCreatedAt(now);
                abi.setCreater(approver);
                abi.setInwardChargeType(InwardChargeType.Medicine);

                PharmaceuticalBillItem apbi = new PharmaceuticalBillItem();
                apbi.setBillItem(abi);
                apbi.setItemBatch(bi.getPharmaceuticalBillItem() != null ? bi.getPharmaceuticalBillItem().getItemBatch() : null);
                Stock stock = null;
                if (bi.getReferanceBillItem() != null && bi.getReferanceBillItem().getPharmaceuticalBillItem() != null) {
                    stock = bi.getReferanceBillItem().getPharmaceuticalBillItem().getStock();
                }
                apbi.setStock(stock);
                apbi.setQty(variance);
                if (stock != null) {
                    double before = stock.getStock();
                    double after = before + variance;
                    apbi.setBeforeAdjustmentValue(before);
                    apbi.setAfterAdjustmentValue(after);
                }
                abi.setPharmaceuticalBillItem(apbi);
                billItemFacade.create(abi);
                adjustmentBill.getBillItems().add(abi);

                if (stock != null && dept != null) {
                    double targetQty = apbi.getAfterAdjustmentValue();
                    pharmacyBean.resetStock(apbi, stock, targetQty, dept);
                }
            }
        }

        physicalCountBill.setApproveUser(approver);
        physicalCountBill.setApproveAt(new Date());
        billFacade.edit(physicalCountBill);
        billFacade.edit(adjustmentBill);
        progressTracker.complete(jobId);
    }
}
