package com.divudi.service.pharmacy;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.dto.ItemCurrentStockDto;
import com.divudi.core.data.dto.OrderingRequirementRowDto;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.DosageForm;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.ejb.PharmacyService;
import com.divudi.service.BillService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 * Calculation engine for the Ordering Requirement Report (issue #22466).
 *
 * Answers "what do I need to order, and how much?" for a pharmacy buyer.
 * Deliberately free of any JSF dependency so the calculation - the part most
 * likely to need adjustment once buyers see real output - can be exercised
 * directly.
 *
 * Three aggregate queries, no per-day reconstruction: total consumption per
 * item over the window, current stock per item, and the most recent purchase
 * rate per item. The monthly average is simply consumption divided by the
 * number of days in the window, scaled to a mean month.
 *
 * @author Buddhika
 */
@Stateless
public class PharmacyOrderingRequirementService implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 365.25 / 12 - the mean month length in days. */
    public static final double DAYS_PER_MONTH = 30.4375;

    @EJB
    private StockFacade stockFacade;

    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;

    @EJB
    private PharmacyService pharmacyService;

    @EJB
    private BillService billService;

    /**
     * Builds the report.
     *
     * @param fromDate           start of the consumption window
     * @param toDate             end of the consumption window
     * @param institution        optional scope filter
     * @param site               optional scope filter
     * @param department         optional scope filter
     * @param category           optional item filter
     * @param dosageForm         optional item filter
     * @param departmentTypes    optional item filter
     * @param targetCoverMonths  months of cover to order up to
     * @param urgentThresholdMonths cover below which the decision is Urgent Order
     */
    public List<OrderingRequirementRowDto> generateReport(
            Date fromDate, Date toDate,
            Institution institution, Institution site, Department department,
            Category category, DosageForm dosageForm, List<DepartmentType> departmentTypes,
            double targetCoverMonths, double urgentThresholdMonths) {

        Date windowStart = CommonFunctions.getStartOfDay(fromDate);
        Date windowEnd = CommonFunctions.getEndOfDay(toDate);
        int windowDays = daysBetweenInclusive(windowStart, windowEnd);

        Map<Long, ItemCurrentStockDto> currentStock = fetchCurrentStockByItem(
                institution, site, department, category, dosageForm, departmentTypes);

        Map<Long, ItemCurrentStockDto> consumptionByItem = fetchConsumptionByItem(
                windowStart, windowEnd, institution, site, department,
                category, dosageForm, departmentTypes);

        Map<Long, Double> lastPurchaseRates = fetchLastPurchaseRates(
                windowStart, windowEnd, institution, site, department);

        // An item belongs on the report if it holds stock now OR moved during the
        // window. The second half is what surfaces items sitting at zero today
        // that were previously selling well - precisely the urgent-order cases
        // the report exists to catch.
        Map<Long, String[]> itemLabels = new LinkedHashMap<>();
        for (ItemCurrentStockDto s : currentStock.values()) {
            itemLabels.put(s.getItemId(), new String[]{s.getItemName(), s.getCode()});
        }
        for (ItemCurrentStockDto c : consumptionByItem.values()) {
            if (!itemLabels.containsKey(c.getItemId())) {
                itemLabels.put(c.getItemId(), new String[]{c.getItemName(), c.getCode()});
            }
        }

        List<OrderingRequirementRowDto> rows = new ArrayList<>();
        for (Map.Entry<Long, String[]> entry : itemLabels.entrySet()) {
            Long itemId = entry.getKey();
            OrderingRequirementRowDto row = new OrderingRequirementRowDto(
                    itemId, entry.getValue()[0], entry.getValue()[1]);

            ItemCurrentStockDto stockDto = currentStock.get(itemId);
            double balance = stockDto == null || stockDto.getQty() == null ? 0.0 : stockDto.getQty();

            ItemCurrentStockDto consumptionDto = consumptionByItem.get(itemId);
            // Outward quantities are stored negative and reversals positive, so
            // negating the signed sum yields consumption already net of returns.
            double signedOutward = consumptionDto == null || consumptionDto.getQty() == null
                    ? 0.0 : consumptionDto.getQty();
            // Negating a zero sum yields -0.0, which formats as "-0.00" on the
            // page and in the exports. Flatten it.
            double consumption = signedOutward == 0.0 ? 0.0 : -signedOutward;

            calculateRow(row, balance, consumption, windowDays,
                    lastPurchaseRates.get(itemId), targetCoverMonths, urgentThresholdMonths);
            rows.add(row);
        }

        applyLastSuppliers(rows);

        rows.sort(Comparator.comparing(OrderingRequirementRowDto::getItemName,
                Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return rows;
    }

    /**
     * Fills the Last Supplier column from one batched lookup keyed by item id.
     *
     * Kept out of the main aggregate deliberately. Folding it in would need an
     * extra join and a per-item "most recent purchase" correlation on a query
     * that already groups a large PharmaceuticalBillItem scan; resolving it
     * separately keeps each query simple and avoids a per-item N+1.
     *
     * Reuses BillService.fetchLastSupplierByItemIds(), which the Movement Out
     * with Current Stock report already uses for its own Last Supplier column,
     * so both reports name the same supplier for the same item.
     */
    private void applyLastSuppliers(List<OrderingRequirementRowDto> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        List<Long> itemIds = new ArrayList<>();
        for (OrderingRequirementRowDto row : rows) {
            if (row.getItemId() != null) {
                itemIds.add(row.getItemId());
            }
        }
        if (itemIds.isEmpty()) {
            return;
        }

        Map<Long, String> lastSupplierByItem = billService.fetchLastSupplierByItemIds(itemIds);
        for (OrderingRequirementRowDto row : rows) {
            String supplier = lastSupplierByItem.get(row.getItemId());
            row.setLastSupplier(supplier != null ? supplier : "");
        }
    }

    /**
     * Fills in every calculated figure for one item.
     *
     * Package-private rather than private so the calculation can be exercised
     * directly against the five hand-computed sample rows from the customer's
     * requirement without going through JSF or the database.
     */
    void calculateRow(OrderingRequirementRowDto row,
            double currentBalance, double consumption, int windowDays,
            Double lastPurchaseRate,
            double targetCoverMonths, double urgentThresholdMonths) {

        row.setCurrentBalance(currentBalance);
        row.setWindowDays(windowDays);
        row.setConsumption(consumption);

        double avgMonthly = windowDays > 0 ? consumption / windowDays * DAYS_PER_MONTH : 0.0;
        row.setAvgMonthlyConsumption(avgMonthly);

        if (avgMonthly > 0) {
            row.setStockCover(currentBalance / avgMonthly);
        } else {
            // No consumption in the window - cover is undefined, not infinite.
            row.setStockCover(null);
        }

        double target = avgMonthly * targetCoverMonths;
        row.setTargetStock(target);
        row.setQuantityToOrder(Math.max(0.0, target - currentBalance));

        if (lastPurchaseRate == null) {
            row.setPurchaseRateUnknown(true);
            row.setLastPurchaseRate(0.0);
        } else {
            row.setLastPurchaseRate(lastPurchaseRate);
        }
        row.setEstimatedCost(row.getQuantityToOrder() * row.getLastPurchaseRate());

        row.setDecision(decide(row.getStockCover(), targetCoverMonths, urgentThresholdMonths));
    }

    private String decide(Double cover, double targetCoverMonths, double urgentThresholdMonths) {
        if (cover == null) {
            return OrderingRequirementRowDto.DECISION_NO_ORDER;
        }
        if (cover < urgentThresholdMonths) {
            return OrderingRequirementRowDto.DECISION_URGENT_ORDER;
        }
        if (cover < targetCoverMonths) {
            return OrderingRequirementRowDto.DECISION_ORDER;
        }
        return OrderingRequirementRowDto.DECISION_NO_ORDER;
    }

    /**
     * Current stock per item within scope. Grouped on the item behind the batch:
     * Stock and ItemBatch only ever exist for an Amp, so aggregating here avoids
     * the pack-vs-unit (Ampp vs Amp) split that billItem.item would introduce.
     */
    @SuppressWarnings("unchecked")
    private Map<Long, ItemCurrentStockDto> fetchCurrentStockByItem(
            Institution institution, Institution site, Department department,
            Category category, DosageForm dosageForm, List<DepartmentType> departmentTypes) {

        Map<String, Object> m = new HashMap<>();
        StringBuilder jpql = new StringBuilder(
                "select new com.divudi.core.data.dto.ItemCurrentStockDto("
                + " s.itemBatch.item.id,"
                + " s.itemBatch.item.name,"
                + " s.itemBatch.item.code,"
                + " sum(s.stock))"
                + " from Stock s"
                + " where s.retired = false");

        appendStockScope(jpql, m, institution, site, department);
        appendItemFilters(jpql, m, "s.itemBatch.item", category, dosageForm, departmentTypes);

        jpql.append(" group by s.itemBatch.item.id, s.itemBatch.item.name, s.itemBatch.item.code");

        List<ItemCurrentStockDto> results =
                (List<ItemCurrentStockDto>) stockFacade.findLightsByJpql(jpql.toString(), m);

        return indexByItem(results);
    }

    /**
     * Total signed outward quantity per item over the window.
     *
     * One aggregate over the outward bill types. Reversals (cancellations and
     * returns) carry positive quantities and are included in the same list, so
     * the sum is already net of returns and no second pass is needed.
     */
    @SuppressWarnings("unchecked")
    private Map<Long, ItemCurrentStockDto> fetchConsumptionByItem(
            Date windowStart, Date windowEnd,
            Institution institution, Institution site, Department department,
            Category category, DosageForm dosageForm, List<DepartmentType> departmentTypes) {

        List<BillTypeAtomic> outward = pharmacyService.getOrderingOutwardBillTypes();

        Map<String, Object> m = new HashMap<>();
        StringBuilder jpql = new StringBuilder(
                "select new com.divudi.core.data.dto.ItemCurrentStockDto("
                + " p.itemBatch.item.id,"
                + " p.itemBatch.item.name,"
                + " p.itemBatch.item.code,"
                + " sum(p.qty + p.freeQty))"
                + " from PharmaceuticalBillItem p"
                + " join p.billItem bi"
                + " join bi.bill bill"
                + " where p.retired = false"
                + " and bi.retired = false"
                + " and bill.retired = false"
                + " and bill.createdAt between :fd and :td"
                + " and bill.billTypeAtomic in :outward");

        m.put("fd", windowStart);
        m.put("td", windowEnd);
        m.put("outward", outward);

        appendBillScope(jpql, m, institution, site, department);
        appendItemFilters(jpql, m, "p.itemBatch.item", category, dosageForm, departmentTypes);

        jpql.append(" group by p.itemBatch.item.id, p.itemBatch.item.name, p.itemBatch.item.code");

        List<ItemCurrentStockDto> results = (List<ItemCurrentStockDto>)
                pharmaceuticalBillItemFacade.findLightsByJpql(jpql.toString(), m, TemporalType.TIMESTAMP);

        return indexByItem(results);
    }

    /**
     * Counts in-window movements that touched stock but fall outside every
     * classification list, so the page can warn rather than silently ignoring
     * them.
     *
     * The classification is a whitelist, which is auditable but can silently
     * drop a movement it has never seen - a bill type added later, or the
     * null-atomic rows a data migration can leave behind. Bill types we have
     * deliberately judged not to move stock (financial mirrors and rate
     * adjustments) are subtracted as well, otherwise the count would flag every
     * settled retail sale and never read zero, which would train users to
     * ignore it.
     *
     * Uses findLongByJpql because COUNT returns a Long; findDoubleByJpql would
     * swallow the resulting ClassCastException and report zero every time.
     */
    public long countUnclassifiedMovements(Date fromDate, Date toDate,
            Institution institution, Institution site, Department department) {

        List<BillTypeAtomic> known = new ArrayList<>(pharmacyService.getOrderingStockMovingBillTypes());
        known.addAll(pharmacyService.getOrderingNonStockMovingBillTypes());

        Map<String, Object> m = new HashMap<>();
        StringBuilder jpql = new StringBuilder(
                "select count(p)"
                + " from PharmaceuticalBillItem p"
                + " join p.billItem bi"
                + " join bi.bill bill"
                + " where p.retired = false"
                + " and bi.retired = false"
                + " and bill.retired = false"
                + " and p.stock is not null"
                + " and bill.createdAt between :fd and :td"
                + " and bill.billTypeAtomic not in :known");

        m.put("fd", CommonFunctions.getStartOfDay(fromDate));
        m.put("td", CommonFunctions.getEndOfDay(toDate));
        m.put("known", known);

        appendBillScope(jpql, m, institution, site, department);

        return pharmaceuticalBillItemFacade.findLongByJpql(
                jpql.toString(), m, TemporalType.TIMESTAMP);
    }

    /**
     * Last purchase rate per item, taken from the most recent inbound movement
     * in the window. Items with no inbound bill are absent from the map and get
     * flagged on the row rather than silently costed at zero.
     */
    @SuppressWarnings("unchecked")
    private Map<Long, Double> fetchLastPurchaseRates(Date windowStart, Date windowEnd,
            Institution institution, Institution site, Department department) {

        List<BillTypeAtomic> inbound = pharmacyService.getOrderingInboundBillTypes();

        Map<String, Object> m = new HashMap<>();
        StringBuilder jpql = new StringBuilder(
                "select p.itemBatch.item.id, p.purchaseRate, bill.createdAt"
                + " from PharmaceuticalBillItem p"
                + " join p.billItem bi"
                + " join bi.bill bill"
                + " where p.retired = false"
                + " and bi.retired = false"
                + " and bill.retired = false"
                + " and p.purchaseRate > 0"
                + " and bill.createdAt between :fd and :td"
                + " and bill.billTypeAtomic in :inbound");

        m.put("fd", windowStart);
        m.put("td", windowEnd);
        m.put("inbound", inbound);

        // Scope must be applied before the ORDER BY is appended, and the rate has
        // to come from the same scope as the rest of the report - otherwise
        // Estimated Cost is priced off some other department's purchases.
        appendBillScope(jpql, m, institution, site, department);

        jpql.append(" order by bill.createdAt");

        List<Object[]> rows = (List<Object[]>) pharmaceuticalBillItemFacade.findLightsByJpql(
                jpql.toString(), m, TemporalType.TIMESTAMP);

        // Ordered oldest first, so the last write per item wins - the most
        // recent rate.
        Map<Long, Double> rates = new HashMap<>();
        for (Object[] r : rows) {
            if (r[0] == null || r[1] == null) {
                continue;
            }
            rates.put(((Number) r[0]).longValue(), ((Number) r[1]).doubleValue());
        }
        return rates;
    }

    private Map<Long, ItemCurrentStockDto> indexByItem(List<ItemCurrentStockDto> results) {
        Map<Long, ItemCurrentStockDto> byItem = new LinkedHashMap<>();
        for (ItemCurrentStockDto dto : results) {
            if (dto.getItemId() == null) {
                continue;
            }
            byItem.put(dto.getItemId(), dto);
        }
        return byItem;
    }

    private void appendStockScope(StringBuilder jpql, Map<String, Object> m,
            Institution institution, Institution site, Department department) {
        if (department != null) {
            jpql.append(" and s.department = :dept");
            m.put("dept", department);
        } else if (site != null) {
            jpql.append(" and s.department.site = :site");
            m.put("site", site);
        } else if (institution != null) {
            jpql.append(" and s.department.institution = :ins");
            m.put("ins", institution);
        }
    }

    /**
     * bill.department is always the stock-owning department: it equals
     * fromDepartment on transfer-issue, sale and pre bills, equals toDepartment
     * on transfer-receive, and is set directly on GRN. One filter is therefore
     * correct for every bill type, with no special-casing.
     *
     * Only the narrowest supplied scope is applied - a department already
     * implies its site and institution, so adding all three just makes the
     * query harder for the optimiser.
     */
    private void appendBillScope(StringBuilder jpql, Map<String, Object> m,
            Institution institution, Institution site, Department department) {
        if (department != null) {
            jpql.append(" and bill.department = :dept");
            m.put("dept", department);
        } else if (site != null) {
            jpql.append(" and bill.department.site = :site");
            m.put("site", site);
        } else if (institution != null) {
            jpql.append(" and bill.department.institution = :ins");
            m.put("ins", institution);
        }
    }

    private void appendItemFilters(StringBuilder jpql, Map<String, Object> m, String itemPath,
            Category category, DosageForm dosageForm, List<DepartmentType> departmentTypes) {
        if (category != null) {
            jpql.append(" and ").append(itemPath).append(".category = :cat");
            m.put("cat", category);
        }
        if (dosageForm != null) {
            jpql.append(" and ").append(itemPath).append(".dosageForm = :df");
            m.put("df", dosageForm);
        }
        if (departmentTypes != null && !departmentTypes.isEmpty()) {
            jpql.append(" and ").append(itemPath).append(".departmentType in :depTypes");
            m.put("depTypes", departmentTypes);
        }
    }

    int daysBetweenInclusive(Date from, Date to) {
        if (from == null || to == null) {
            return 0;
        }
        long fromDay = CommonFunctions.getStartOfDay(from).getTime();
        long toDay = CommonFunctions.getStartOfDay(to).getTime();
        if (toDay < fromDay) {
            return 0;
        }
        return (int) Math.round((toDay - fromDay) / (1000.0 * 60 * 60 * 24)) + 1;
    }
}
