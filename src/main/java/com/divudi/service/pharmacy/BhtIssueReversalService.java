package com.divudi.service.pharmacy;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.ejb.PharmacyBean;
import com.divudi.service.BillService;
import java.util.ArrayList;
import java.util.Date;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Data-admin reversal of a pharmacy direct issue to a BHT, in one transaction
 * (#26109). The reversal bill, its bill items and pharmaceutical bill items are
 * saved first, then the stock goes back, then the original is marked cancelled.
 * Any failure throws {@link BhtIssueReversalException} or a runtime error and
 * rolls back everything, so no stock change stays.
 */
@Stateless
public class BhtIssueReversalService {

    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private BillService billService;

    public Bill reverse(Long originalBillId, WebUser user, Department department, String deptId) {
        if (originalBillId == null) {
            throw new BhtIssueReversalException("No Bill Found");
        }
        // Read the original inside this transaction: merging the page's copy
        // could drop its items.
        Bill original = billFacade.findWithoutCache(originalBillId);
        if (original == null) {
            throw new BhtIssueReversalException("No Bill Found");
        }
        if (original.isCancelled() || original.getCancelledBill() != null) {
            throw new BhtIssueReversalException("Bill is already cancelled. Cannot reverse cancelled bills");
        }

        Bill reversalBill = new Bill();
        reversalBill.copy(original);
        reversalBill.setCreatedAt(new Date());
        reversalBill.setCreater(user);
        reversalBill.setDepartment(department);
        reversalBill.setReferenceBill(original);
        reversalBill.setBilledBill(original);
        reversalBill.setForwardReferenceBill(original.getForwardReferenceBill());
        reversalBill.setBillType(original.getBillType());
        reversalBill.setBillTypeAtomic(original.getBillTypeAtomic());
        reversalBill.setDeptId(deptId);
        reversalBill.setBillItems(new ArrayList<>());

        ArrayList<PharmaceuticalBillItem> reversalPbis = new ArrayList<>();
        for (BillItem originalItem : original.getBillItems()) {
            BillItem reversalItem = new BillItem();
            reversalItem.copy(originalItem);
            reversalItem.setBill(reversalBill);
            reversalItem.setCreatedAt(new Date());
            reversalItem.setCreater(user);
            reversalItem.invertValue(originalItem);

            PharmaceuticalBillItem originalPbi = originalItem.getPharmaceuticalBillItem();
            if (originalPbi != null) {
                PharmaceuticalBillItem reversalPbi = new PharmaceuticalBillItem();
                reversalPbi.copy(originalPbi);
                reversalPbi.setBillItem(reversalItem);
                reversalPbi.setCreatedAt(new Date());
                reversalPbi.setCreater(user);
                reversalPbi.invertValue(originalPbi);
                reversalItem.setPharmaceuticalBillItem(reversalPbi);
                reversalPbis.add(reversalPbi);
            }
            reversalBill.getBillItems().add(reversalItem);
        }

        // New entities are saved with create(), in order: bill, items, pharmaceutical items.
        billFacade.create(reversalBill);
        for (BillItem reversalItem : reversalBill.getBillItems()) {
            billItemFacade.create(reversalItem);
        }
        for (PharmaceuticalBillItem reversalPbi : reversalPbis) {
            pharmaceuticalBillItemFacade.create(reversalPbi);
        }

        // Only now the stock goes back, with the saved pharmaceutical bill items.
        for (PharmaceuticalBillItem reversalPbi : reversalPbis) {
            if (reversalPbi.getQty() != 0 && reversalPbi.getStock() != null) {
                if (!pharmacyBean.addToStock(reversalPbi.getStock(), reversalPbi.getQty(), reversalPbi, department)) {
                    throw new BhtIssueReversalException("Stock could not be restored. Nothing was changed.");
                }
            }
        }

        billService.createBillFinancialDetailsForPharmacyBill(reversalBill);

        original.setCancelled(true);
        original.setCancelledBill(reversalBill);
        billFacade.edit(original);
        return reversalBill;
    }
}
