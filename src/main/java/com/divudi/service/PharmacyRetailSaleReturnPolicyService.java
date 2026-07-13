package com.divudi.service;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.RequestType;
import com.divudi.core.entity.Bill;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * Enforces the configurable day-limit policy for pharmacy retail sale
 * returns: allowed without approval up to N days after the original sale
 * bill's creation, allowed with an approved Request between day N+1 and M,
 * and hard-blocked beyond M days with no override.
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Stateless
public class PharmacyRetailSaleReturnPolicyService {

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    @Inject
    RequestService requestService;

    public Long getNoApprovalDayLimit() {
        return configOptionApplicationController.getLongValueByKey("Pharmacy Retail Sale Return - Days Allowed Without Approval", 3L);
    }

    public Long getWithApprovalDayLimit() {
        return configOptionApplicationController.getLongValueByKey("Pharmacy Retail Sale Return - Days Allowed With Approval", 7L);
    }

    public long daysSinceBillCreation(Bill saleBill) {
        if (saleBill == null || saleBill.getCreatedAt() == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(saleBill.getCreatedAt().toInstant(), Instant.now());
    }

    public boolean hasApprovedReturnRequest(Bill saleBill) {
        return requestService.findApprovedRequestByBillAndType(saleBill, RequestType.PHARMACY_RETAIL_SALE_RETURN_APPROVAL) != null;
    }

    public String checkReturnAllowed(Bill saleBill) {
        long days = daysSinceBillCreation(saleBill);
        long withApprovalLimit = getWithApprovalDayLimit();
        long noApprovalLimit = getNoApprovalDayLimit();
        if (days > withApprovalLimit) {
            return "This bill is " + days + " days old. Returns are not allowed after " + withApprovalLimit + " days.";
        }
        if (days > noApprovalLimit && !hasApprovedReturnRequest(saleBill)) {
            return "This bill is " + days + " days old. Returns after " + noApprovalLimit + " days require an approved request. Please request approval.";
        }
        return null;
    }

}
