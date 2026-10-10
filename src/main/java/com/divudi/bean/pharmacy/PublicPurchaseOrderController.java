/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SecurityController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.dto.pharmacy.PurchaseOrderPrintDto;
import com.divudi.core.entity.Bill;
import com.divudi.core.facade.BillFacade;
import com.divudi.service.pharmacy.PurchaseOrderNativeSqlService;
import java.io.Serializable;
import java.util.Date;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Backing bean for the public (no-login) Purchase Order view page,
 * {@code requests/po.xhtml}. Opened from an SMS link sent to a supplier, so
 * it must never require an authenticated session and must never reveal why a
 * token was rejected — every failure collapses to a single generic invalid
 * state. Related issue: #23811.
 */
@Named
@RequestScoped
public class PublicPurchaseOrderController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionController sessionController;
    @Inject
    private SecurityController securityController;

    @EJB
    private BillFacade billFacade;
    @EJB
    private PurchaseOrderNativeSqlService purchaseOrderNativeSqlService;

    private String token;
    private boolean valid;
    private PurchaseOrderPrintDto printDto;

    public void loadFromToken() {
        valid = false;
        printDto = null;

        if (token == null || token.trim().isEmpty()) {
            return;
        }

        String hmacKey;
        try {
            hmacKey = sessionController.getApplicationPreference().getEncrptionKey();
        } catch (Exception e) {
            return;
        }
        if (hmacKey == null || hmacKey.trim().isEmpty()) {
            return;
        }

        long[] decoded;
        try {
            decoded = securityController.decodeBillToken(token, hmacKey);
        } catch (Exception e) {
            return;
        }
        if (decoded == null) {
            return;
        }
        if (new Date().getTime() > decoded[1]) {
            return; // link expired
        }

        Bill bill;
        try {
            bill = billFacade.find(decoded[0]);
        } catch (Exception e) {
            return;
        }
        if (bill == null || bill.isRetired() || bill.getBillTypeAtomic() != BillTypeAtomic.PHARMACY_ORDER_APPROVAL) {
            return;
        }

        PurchaseOrderPrintDto dto;
        try {
            dto = purchaseOrderNativeSqlService.loadPrintDtoByBillId(bill.getId());
        } catch (Exception e) {
            return;
        }
        if (dto == null) {
            return;
        }

        printDto = dto;
        valid = true;
    }

    public boolean isValid() {
        return valid;
    }

    public PurchaseOrderPrintDto getPrintDto() {
        return printDto;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
