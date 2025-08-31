package com.divudi.bean.pharmacy;

import com.divudi.core.data.BillType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.ejb.PharmacyBean;
import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Handles GRN return workflow with approval.
 */
@Named
@SessionScoped
public class GrnReturnApprovalController implements Serializable {

    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private PharmacyBean pharmacyBean;

    @Inject
    private SessionController sessionController;

    private Bill grnBill;
    private Bill pendingReturn;

    public void prepareReturnRequest() {
        if (grnBill == null) {
            JsfUtil.addErrorMessage("No GRN selected");
            return;
        }
        pendingReturn = new BilledBill();
        pendingReturn.setBillType(BillType.PharmacyGrnReturn);
        pendingReturn.setReferenceBill(grnBill);
        pendingReturn.setDepartment(sessionController.getDepartment());
        pendingReturn.setInstitution(sessionController.getInstitution());
        pendingReturn.setFromDepartment(grnBill.getDepartment());
        pendingReturn.setToInstitution(grnBill.getFromInstitution());
        pendingReturn.setCreatedAt(new Date());
        pendingReturn.setCreater(sessionController.getLoggedUser());
        pendingReturn.setBillItems(new ArrayList<>());
        pendingReturn.setPaid(false);
        pendingReturn.setBalance(pendingReturn.getNetTotal());
    }

    public String saveReturnRequest() {
        if (pendingReturn == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return null;
        }
        if (pendingReturn.getId() == null) {
            billFacade.create(pendingReturn);
        } else {
            billFacade.edit(pendingReturn);
        }
        for (BillItem bi : pendingReturn.getBillItems()) {
            bi.setBill(pendingReturn);
            if (bi.getPharmaceuticalBillItem() != null) {
                PharmaceuticalBillItem pi = bi.getPharmaceuticalBillItem();
                pi.setBillItem(bi);
            }
            if (bi.getId() == null) {
                billItemFacade.create(bi);
            } else {
                billItemFacade.edit(bi);
            }
        }
        JsfUtil.addSuccessMessage("Return request saved");
        return "/pharmacy/pharmacy_grn_list_for_return?faces-redirect=true";
    }

    public List<Bill> getPendingReturns() {
        String jpql = "select b from Bill b where b.retired=false and b.billType=:bt and b.approveUser is null";
        Map<String, Object> params = new HashMap<>();
        params.put("bt", BillType.PharmacyGrnReturn);
        return billFacade.findByJpql(jpql, params);
    }

    public void approveReturn(Bill b) {
        if (b == null) {
            return;
        }
        b.setApproveUser(sessionController.getLoggedUser());
        b.setApproveAt(Calendar.getInstance().getTime());
        b.setPaid(true);
        b.setPaidAmount(b.getNetTotal());
        b.setBalance(0d);
        for (BillItem bi : b.getBillItems()) {
            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
            if (pbi != null) {
                pharmacyBean.deductFromStock(pbi.getStock(), Math.abs(pbi.getQtyInUnit() + pbi.getFreeQtyInUnit()), pbi, sessionController.getDepartment());
                double qty = pbi.getQty();
                double free = pbi.getFreeQty();
                pbi.setQty(0 - Math.abs(qty));
                pbi.setFreeQty(0 - Math.abs(free));
                pharmaceuticalBillItemFacade.edit(pbi);
            }
            billItemFacade.edit(bi);
        }
        billFacade.edit(b);
        JsfUtil.addSuccessMessage("Return approved");
    }

    public Bill getGrnBill() {
        return grnBill;
    }

    public void setGrnBill(Bill grnBill) {
        this.grnBill = grnBill;
    }

    public Bill getPendingReturn() {
        return pendingReturn;
    }

    public void setPendingReturn(Bill pendingReturn) {
        this.pendingReturn = pendingReturn;
    }
}

