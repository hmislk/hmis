package com.divudi.service.inward;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.BilledBillFacade;
import com.divudi.service.PaymentService;
import java.util.Date;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Saves an inward payment as one transaction (#25610): the payment bill, its
 * bill item, the Payment rows and the drawer update are all saved, or none are.
 * Before this, the controller saved the bill first and the Payment afterwards
 * in separate transactions, so a failure left a live payment bill with no
 * Payment and no drawer entry.
 */
@Stateless
public class InwardPaymentService {

    @EJB
    private BilledBillFacade billedBillFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PaymentService paymentService;

    /**
     * @param bill the prepared payment bill (numbers, dates, type already set);
     * must be new (no id yet)
     */
    public Bill savePaymentBillWithPayments(BilledBill bill, PaymentMethodData paymentMethodData,
            Institution institution, Department department, WebUser webUser) {
        if (bill == null) {
            throw new InwardPaymentException("No payment bill to save.");
        }
        if (bill.getId() != null) {
            throw new InwardPaymentException("This payment bill is already saved.");
        }
        // Flush so the bill has its id before the Payment rows point to it.
        billedBillFacade.createAndFlush(bill);

        BillItem item = new BillItem();
        item.setBill(bill);
        item.setGrossValue(bill.getTotal());
        item.setNetValue(bill.getTotal());
        item.setCreatedAt(new Date());
        item.setCreater(webUser);
        billItemFacade.create(item);

        paymentService.createPaymentAndUpdateDrawer(bill, bill.getPaymentMethod(), paymentMethodData,
                institution, department, webUser);
        return bill;
    }
}
