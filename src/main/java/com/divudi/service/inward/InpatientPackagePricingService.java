package com.divudi.service.inward;

import com.divudi.core.data.inward.InpatientPackageComponentType;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.inward.InpatientPackage;
import com.divudi.core.entity.inward.InpatientPackageItem;
import com.divudi.core.facade.InpatientPackageItemFacade;
import com.divudi.core.util.InpatientPackagePricing;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private InwardProfessionalFeeClassificationService professionalFeeClassificationService;

    /**
     * The package's price broken down per InwardChargeType: its charge-type
     * amounts plus each active component's fixed price, or an empty map when
     * {@code inpatientPackage} is null.
     */
    public Map<InwardChargeType, Double> allocationsFor(InpatientPackage inpatientPackage) {
        if (inpatientPackage == null) {
            return new HashMap<>();
        }
        return InpatientPackagePricing.calculateChargeTypeAllocations(
                inpatientPackage.getChargeTypeAmounts(), componentAllocations(inpatientPackage));
    }

    /**
     * Each active component's fixed price, keyed by the InwardChargeType it
     * resolves to: the component's Item's own inwardChargeType for
     * SERVICE/TIMED_ITEM/OUTSIDE_CHARGE/PHARMACY_ITEM, or the same
     * staff-independent default category a professional fee entry form would
     * preselect (professionalFeeClassificationService.defaultCategoryFor with
     * a null staff) for PROFESSIONAL_FEE_ROLE, whose real BillFee has no
     * category until a specific staff member is later assigned.
     */
    private Map<InwardChargeType, Double> componentAllocations(InpatientPackage inpatientPackage) {
        Map<InwardChargeType, Double> result = new HashMap<>();
        Map<String, Object> params = new HashMap<>();
        params.put("pkg", inpatientPackage);
        List<InpatientPackageItem> components = inpatientPackageItemFacade.findByJpql(
                "SELECT i FROM InpatientPackageItem i WHERE i.retired = false AND i.inpatientPackage = :pkg",
                params);
        if (components == null) {
            return result;
        }
        for (InpatientPackageItem component : components) {
            InwardChargeType type;
            if (component.getComponentType() == InpatientPackageComponentType.PROFESSIONAL_FEE_ROLE) {
                type = professionalFeeClassificationService.defaultCategoryFor(null, component.getSpeciality());
            } else if (component.getItem() != null) {
                type = component.getItem().getInwardChargeType();
            } else {
                continue;
            }
            if (type == null || component.getFixedPrice() == null) {
                continue;
            }
            result.merge(type, component.getFixedPrice(), Double::sum);
        }
        return result;
    }
}
