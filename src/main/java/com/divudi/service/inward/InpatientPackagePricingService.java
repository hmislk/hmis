package com.divudi.service.inward;

import com.divudi.core.data.inward.InpatientPackageComponentType;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.InpatientPackage;
import com.divudi.core.entity.inward.InpatientPackageItem;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.InpatientPackageItemFacade;
import com.divudi.core.util.InpatientPackagePricing;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Which charge types an inpatient package covers, and for how much. Shared by
 * the interim bill (BhtSummeryController) and the inward charge aggregation
 * service behind the Invoice Journal and debtor reports (issue #24744), so
 * both apply the same package price.
 */
@Stateless
public class InpatientPackagePricingService {

    @EJB
    private InpatientPackageItemFacade inpatientPackageItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private InwardProfessionalFeeClassificationService professionalFeeClassificationService;

    /**
     * The package's price broken down per InwardChargeType: its charge-type
     * amounts plus each active component's fixed price, or an empty map when
     * {@code inpatientPackage} is null. Professional fee roles use the default
     * category, as no staff is known.
     */
    public Map<InwardChargeType, Double> allocationsFor(InpatientPackage inpatientPackage) {
        return allocationsFor(inpatientPackage, null);
    }

    /**
     * As {@link #allocationsFor(InpatientPackage)}, but a professional fee
     * role covers the row of the staff actually assigned to that admission's
     * package fee (see {@link #componentAllocations}).
     *
     * @param admission the package admission; may be null
     */
    public Map<InwardChargeType, Double> allocationsFor(InpatientPackage inpatientPackage, PatientEncounter admission) {
        if (inpatientPackage == null) {
            return new HashMap<>();
        }
        return InpatientPackagePricing.calculateChargeTypeAllocations(
                inpatientPackage.getChargeTypeAmounts(), componentAllocations(inpatientPackage, admission));
    }

    /**
     * Each active component's fixed price, keyed by the InwardChargeType it
     * resolves to: the component's Item's own inwardChargeType for
     * SERVICE/TIMED_ITEM/OUTSIDE_CHARGE/PHARMACY_ITEM. For PROFESSIONAL_FEE_ROLE
     * it is the row the admission's own package fee (BillFee with that
     * sourcePackageItem) is billed under; with no such fee yet, the default
     * category with no staff (the speciality's configured default, else
     * Professional Fee).
     */
    private Map<InwardChargeType, Double> componentAllocations(InpatientPackage inpatientPackage, PatientEncounter admission) {
        Map<String, Object> params = new HashMap<>();
        params.put("pkg", inpatientPackage);
        List<InpatientPackageItem> components = inpatientPackageItemFacade.findByJpql(
                "SELECT i FROM InpatientPackageItem i WHERE i.retired = false AND i.inpatientPackage = :pkg",
                params);
        if (components == null) {
            return new HashMap<>();
        }
        return allocate(components, packageFeesOf(admission, components));
    }

    /** The admission's live package professional fees for these components. */
    private List<BillFee> packageFeesOf(PatientEncounter admission, List<InpatientPackageItem> components) {
        if (admission == null || admission.getId() == null) {
            return new ArrayList<>();
        }
        List<Long> componentIds = new ArrayList<>();
        for (InpatientPackageItem c : components) {
            if (c.getComponentType() == InpatientPackageComponentType.PROFESSIONAL_FEE_ROLE && c.getId() != null) {
                componentIds.add(c.getId());
            }
        }
        if (componentIds.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("eid", admission.getId());
        params.put("ids", componentIds);
        List<BillFee> fees = billFeeFacade.findByJpql(
                "SELECT f FROM BillFee f WHERE f.retired = false AND f.bill.cancelled = false"
                + " AND f.patienEncounter.id = :eid AND f.sourcePackageItem.id IN :ids",
                params);
        return fees != null ? fees : new ArrayList<>();
    }

    /**
     * Pure allocation step. A professional fee role with package fees on the
     * admission is split per fee: each fee counts its fee value under the row
     * it is billed under (its stored category, Professional Fee when none,
     * folded by rowTypeFor), assigned or not, so fees saved before the default
     * changed still match their bill row. A role with no fee yet counts its
     * fixed price under the default category.
     */
    Map<InwardChargeType, Double> allocate(List<InpatientPackageItem> components, List<BillFee> fees) {
        Map<InwardChargeType, Double> result = new HashMap<>();
        for (InpatientPackageItem component : components) {
            if (component.getComponentType() == InpatientPackageComponentType.PROFESSIONAL_FEE_ROLE) {
                InwardChargeType unassigned = professionalFeeClassificationService.rowTypeFor(
                        professionalFeeClassificationService.defaultCategoryFor(null, component.getSpeciality()));
                boolean hasFee = false;
                for (BillFee fee : fees) {
                    InpatientPackageItem source = fee.getSourcePackageItem();
                    if (source == null
                            || (source != component && (component.getId() == null
                            || !Objects.equals(source.getId(), component.getId())))) {
                        continue;
                    }
                    hasFee = true;
                    InwardChargeType type = professionalFeeClassificationService
                            .rowTypeFor(professionalFeeClassificationService.chargeTypeOf(fee));
                    result.merge(type, fee.getFeeValue(), Double::sum);
                }
                if (!hasFee && component.getFixedPrice() != null) {
                    result.merge(unassigned, component.getFixedPrice(), Double::sum);
                }
                continue;
            }
            InwardChargeType type = component.getItem() != null ? component.getItem().getInwardChargeType() : null;
            if (type == null || component.getFixedPrice() == null) {
                continue;
            }
            result.merge(type, component.getFixedPrice(), Double::sum);
        }
        return result;
    }

    void setProfessionalFeeClassificationService(InwardProfessionalFeeClassificationService service) {
        this.professionalFeeClassificationService = service;
    }
}
