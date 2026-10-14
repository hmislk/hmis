package com.divudi.service.inward;

import com.divudi.core.entity.Item;
import com.divudi.core.entity.Service;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.InwardService;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.facade.FeeFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.service.AuditService;
import com.divudi.service.service.ServiceApiService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * One-off data tools used when setting up inpatient discounts (issue #24038).
 * Both tools have a count (dry run) and an apply step, and are shared by the
 * Data Administration page and the REST API.
 *
 * <ul>
 * <li>Copy the deprecated {@code Investigation.investigationCategory} into
 * {@code Item.category} where the latter is blank.</li>
 * <li>Turn on "Discount Allowed" for items and their fees. Inpatient service
 * discounts from the Inward Discount Matrix apply only when both the item and
 * the fee allow discounts.</li>
 * </ul>
 */
@Stateless
public class DiscountSetupService {

    /**
     * Item kinds the discount-allowed tool can target. Each maps to an exact
     * entity type (TYPE() match), so SERVICE does not also pick up the
     * InwardService subclass. The type names are the ones accepted by
     * {@link ServiceApiService}'s bulk flag methods, which do the updates.
     */
    public enum ItemScope {
        INVESTIGATION(Investigation.class, "Investigation"),
        SERVICE(Service.class, "Service"),
        INWARD_SERVICE(InwardService.class, "InwardService");

        private final Class<? extends Item> type;
        private final String itemTypeName;

        ItemScope(Class<? extends Item> type, String itemTypeName) {
            this.type = type;
            this.itemTypeName = itemTypeName;
        }

        public Class<? extends Item> getType() {
            return type;
        }

        public String getItemTypeName() {
            return itemTypeName;
        }
    }

    @EJB
    private ItemFacade itemFacade;
    @EJB
    private FeeFacade feeFacade;
    @EJB
    private AuditService auditService;
    @EJB
    private ServiceApiService serviceApiService;

    // ---------------------------------------------------------------------
    // Legacy investigation category
    // ---------------------------------------------------------------------
    private static final String LEGACY_CATEGORY_WHERE
            = " where i.retired = false and i.category is null and i.investigationCategory is not null";

    public long countInvestigationsUsingLegacyCategory() {
        return itemFacade.findLongByJpql("select count(i) from Investigation i" + LEGACY_CATEGORY_WHERE,
                new HashMap<>());
    }

    /**
     * Copies {@code investigationCategory} into {@code category} for active
     * investigations whose category is blank.
     *
     * @return number of investigations updated
     */
    public int copyLegacyInvestigationCategories(WebUser user) {
        long before = countInvestigationsUsingLegacyCategory();
        int updated = itemFacade.updateByJpql(
                "update Investigation i set i.category = i.investigationCategory" + LEGACY_CATEGORY_WHERE,
                new HashMap<>());
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("investigationsFound", before);
        after.put("investigationsUpdated", updated);
        auditService.logAudit(null, after, user, "Investigation", "Copy Legacy Investigation Category");
        return updated;
    }

    // ---------------------------------------------------------------------
    // Discount allowed flags
    // ---------------------------------------------------------------------
    /**
     * Counts active items of the given kinds, and their active item fees, that
     * do not allow discounts. Read only.
     *
     * @return map with itemsNotAllowed and feesNotAllowed
     */
    public Map<String, Long> countDiscountNotAllowed(List<ItemScope> scopes) {
        Map<String, Object> p = typeParams(scopes);
        long items = itemFacade.findLongByJpql("select count(i) from Item i where i.retired = false"
                + " and type(i) in :types and (i.discountAllowed is null or i.discountAllowed = false)", p);
        long fees = feeFacade.findLongByJpql("select count(f) from ItemFee f where f.retired = false"
                + " and f.discountAllowed = false and type(f.item) in :types", p);
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("itemsNotAllowed", items);
        m.put("feesNotAllowed", fees);
        return m;
    }

    /**
     * Turns on "Discount Allowed" for active items of the given kinds and all
     * their active item fees, using the same bulk methods as
     * {@code POST /api/services/items/bulk-discount-allowed} and
     * {@code POST /api/services/fees/bulk-margin} (both audited there).
     *
     * @return map with itemsUpdated and feesUpdated (rows set, including
     * any that already allowed discounts)
     */
    public Map<String, Integer> allowDiscounts(List<ItemScope> scopes, WebUser user) throws Exception {
        typeParams(scopes);
        int items = 0;
        int fees = 0;
        for (ItemScope s : scopes) {
            Map<String, Object> ri = serviceApiService.bulkUpdateItemDiscountAllowed(null, s.getItemTypeName(), true, user);
            Map<String, Object> rf = serviceApiService.bulkUpdateMargin(null, s.getItemTypeName(), null, null, true, user);
            items += countOf(ri);
            fees += countOf(rf);
        }
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("itemsUpdated", items);
        m.put("feesUpdated", fees);
        return m;
    }

    private int countOf(Map<String, Object> r) {
        Object o = r == null ? null : r.get("count");
        return o instanceof Number ? ((Number) o).intValue() : 0;
    }

    private Map<String, Object> typeParams(List<ItemScope> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalArgumentException("At least one scope is required");
        }
        List<Class<? extends Item>> types = new ArrayList<>();
        for (ItemScope s : scopes) {
            types.add(s.getType());
        }
        Map<String, Object> p = new HashMap<>();
        p.put("types", types);
        return p;
    }
}
