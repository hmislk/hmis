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
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private static final Logger LOGGER = Logger.getLogger(PaymentSchemeDuplicationService.class.getName());

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
            PriceMatrix npm = newMatrixOfSameType(pm);
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

    /**
     * Creates an empty price matrix of the same concrete type as the source.
     *
     * <p>
     * {@link PriceMatrix} is a single-table hierarchy and the discount lookups
     * query the subtypes directly - {@code PaymentSchemeDiscount},
     * {@code OpdMemberShipDiscount} and so on. Copying every row into a plain
     * {@code PriceMatrix} would write the base discriminator, and the duplicated
     * scheme's discounts would then be invisible to every one of those queries.
     * </p>
     *
     * <p>
     * Reflection is safe here because none of the subtypes declares a field of its
     * own; they exist purely to carry the discriminator, so copying the fields
     * declared on {@code PriceMatrix} copies the whole row. If a subtype ever gains
     * its own state, this method is where that has to be handled.
     * </p>
     *
     * @param source the row being duplicated
     * @return a new, empty instance of the same concrete type, or a plain
     *         {@code PriceMatrix} if that type cannot be instantiated
     */
    private PriceMatrix newMatrixOfSameType(PriceMatrix source) {
        Class<? extends PriceMatrix> type = source.getClass();
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not instantiate " + type.getName()
                    + " while duplicating a price matrix; falling back to PriceMatrix.", e);
            return new PriceMatrix();
        }
    }
}
