/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.membership;

import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.PaymentSchemeFacade;
import com.divudi.core.facade.PriceMatrixFacade;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Duplicates a {@link PaymentScheme} together with its price matrices.
 *
 * <p>
 * The work lives here, in a {@code @Stateless} EJB, rather than in the JSF
 * controller. Holding a JTA transaction across a session-scoped controller action
 * is what let a rolled-back transaction stay bound to an HTTP worker thread during
 * the 2026-09-01 Ruhunu outage; container-managed transactions on a stateless bean
 * begin and end inside a single call, so nothing can outlive the invocation.
 * </p>
 */
@Stateless
public class PaymentSchemeDuplicationService {

    @EJB
    private PaymentSchemeFacade paymentSchemeFacade;

    @EJB
    private PriceMatrixFacade priceMatrixFacade;

    /**
     * Creates a copy of the given payment scheme along with a copy of every one of
     * its non-retired price matrices.
     *
     * <p>
     * The whole copy is one unit of work: if any price matrix fails to duplicate,
     * the exception propagates and the container rolls back the new scheme too,
     * so a half-copied scheme can never be left behind.
     * </p>
     *
     * @param sourceSchemeId id of the scheme to copy
     * @param creator        user recorded as the creator of the copies
     * @return the newly created scheme
     * @throws IllegalArgumentException if the source scheme no longer exists
     */
    public PaymentScheme duplicate(Long sourceSchemeId, WebUser creator) {
        PaymentScheme source = paymentSchemeFacade.find(sourceSchemeId);
        if (source == null) {
            throw new IllegalArgumentException("Payment scheme " + sourceSchemeId + " no longer exists");
        }

        PaymentScheme dup = new PaymentScheme();
        dup.setName("a copy of " + source.getName());
        dup.setPrintingName(source.getPrintingName());
        dup.setOrderNo(source.getOrderNo());
        dup.setValidForPharmacy(source.isValidForPharmacy());
        dup.setValidForBilledBills(source.isValidForBilledBills());
        dup.setValidForInpatientBills(source.isValidForInpatientBills());
        dup.setValidForChanneling(source.isValidForChanneling());
        dup.setStaffMemberRequired(source.isStaffMemberRequired());
        dup.setMembershipRequired(source.isMembershipRequired());
        dup.setStaffRequired(source.isStaffRequired());
        dup.setStaffOrFamilyRequired(source.isStaffOrFamilyRequired());
        dup.setMemberRequired(source.isMemberRequired());
        dup.setMemberOrFamilyRequired(source.isMemberOrFamilyRequired());
        dup.setSeniorCitizenRequired(source.isSeniorCitizenRequired());
        dup.setPregnantMotherRequired(source.isPregnantMotherRequired());
        dup.setExpiryDate(source.getExpiryDate());
        dup.setCliantType(source.getCliantType());
        dup.setInstitution(source.getInstitution());
        dup.setPerson(source.getPerson());
        dup.setDepartment(source.getDepartment());
        dup.setCreatedAt(new Date());
        dup.setCreater(creator);

        paymentSchemeFacade.create(dup);

        Map<String, Object> params = new HashMap<>();
        params.put("ps", source);
        String jpql = "select pm from PriceMatrix pm where pm.retired=false and pm.paymentScheme=:ps";
        List<PriceMatrix> matrices = priceMatrixFacade.findByJpql(jpql, params);

        for (PriceMatrix pm : matrices) {
            PriceMatrix npm = new PriceMatrix();
            npm.setBillType(pm.getBillType());
            npm.setCategory(pm.getCategory());
            npm.setInstitution(pm.getInstitution());
            npm.setDepartment(pm.getDepartment());
            npm.setItem(pm.getItem());
            npm.setFromPrice(pm.getFromPrice());
            npm.setToPrice(pm.getToPrice());
            npm.setMargin(pm.getMargin());
            npm.setRoomLocation(pm.getRoomLocation());
            npm.setMembershipScheme(pm.getMembershipScheme());
            npm.setPaymentScheme(dup);
            npm.setPaymentMethod(pm.getPaymentMethod());
            npm.setInwardChargeType(pm.getInwardChargeType());
            npm.setDiscountPercent(pm.getDiscountPercent());
            npm.setAdmissionType(pm.getAdmissionType());
            npm.setRoomCategory(pm.getRoomCategory());
            npm.setToInstitution(pm.getToInstitution());
            npm.setCreatedAt(new Date());
            npm.setCreater(creator);
            priceMatrixFacade.create(npm);
        }

        return dup;
    }
}
