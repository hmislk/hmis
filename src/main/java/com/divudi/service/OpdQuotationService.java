package com.divudi.service;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.OpdQuotation;
import com.divudi.core.entity.OpdQuotationItem;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.OpdQuotationFacade;
import com.divudi.core.facade.OpdQuotationItemFacade;
import com.divudi.ejb.BillNumberGenerator;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Saves an OPD quotation (#25885). A quotation is not a bill: this writes only
 * the quotation and its lines, never a bill, stock or payment.
 */
@Stateless
public class OpdQuotationService {

    @EJB
    private OpdQuotationFacade quotationFacade;
    @EJB
    private OpdQuotationItemFacade quotationItemFacade;
    @EJB
    private BillNumberGenerator billNumberGenerator;

    /**
     * Numbers (own counter), then saves the quotation and its lines with create().
     *
     * @throws OpdQuotationException when the input is not enough to quote
     */
    public OpdQuotation save(OpdQuotation quotation, List<OpdQuotationItem> items, Department department, WebUser user) {
        if (quotation == null) {
            throw new OpdQuotationException("Nothing to save.");
        }
        if (items == null || items.isEmpty()) {
            throw new OpdQuotationException("Add at least one service.");
        }
        if (department == null || department.getInstitution() == null) {
            throw new OpdQuotationException("Your department is not set.");
        }
        if (user == null) {
            throw new OpdQuotationException("Please log in again.");
        }
        if (quotation.getPatient() == null
                && (quotation.getCustomerName() == null || quotation.getCustomerName().trim().isEmpty())) {
            throw new OpdQuotationException("Enter the patient or the name of the enquirer.");
        }
        if (quotation.getValidUntil() == null) {
            throw new OpdQuotationException("Enter the date until which the quotation is valid.");
        }
        quotation.setDepartment(department);
        quotation.setInstitution(department.getInstitution());
        quotation.setCreater(user);
        quotation.setCreatedAt(new Date());
        quotation.setQuotationNo(billNumberGenerator.departmentClaimNumberGeneratorYearlyInNewTransaction(
                department, BillTypeAtomic.OPD_QUOTATION, "QTN"));
        quotationFacade.create(quotation);
        for (OpdQuotationItem item : items) {
            item.setQuotation(quotation);
            quotationItemFacade.create(item);
        }
        return quotation;
    }
}
