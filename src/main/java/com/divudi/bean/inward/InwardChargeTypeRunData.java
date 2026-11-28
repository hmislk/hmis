package com.divudi.bean.inward;

import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.PatientItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the discount step of Create Final Bill reads for each charge type,
 * read once per run for all charge types and grouped here in memory (#17562).
 * Before, every read ran once per charge type per admission-type period, even
 * for a charge type with no rows.
 * <p>
 * A charge type is "empty" when none of the rows any discount step reads or
 * clears (service fees, service items, timed items, their patient items)
 * exists for it. The discount step then does nothing and returns 0, so the
 * caller can skip it, price-matrix lookup included.
 */
public class InwardChargeTypeRunData {

    private final Map<InwardChargeType, List<BillFee>> serviceFees = new EnumMap<>(InwardChargeType.class);
    private final Map<InwardChargeType, List<BillItem>> serviceItems = new EnumMap<>(InwardChargeType.class);
    private final Map<InwardChargeType, List<Object[]>> timedItemRows = new EnumMap<>(InwardChargeType.class);
    private final Map<InwardChargeType, List<PatientItem>> timedPatientItems = new EnumMap<>(InwardChargeType.class);
    private final Set<InwardChargeType> withOtherRows = EnumSet.noneOf(InwardChargeType.class);

    /**
     * Groups rows by the charge type in column {@code typeColumn}, keeping the
     * order of the rows. The value kept is column {@code valueColumn}, or the
     * whole row when it is negative. Rows with no charge type are dropped (the
     * old per-type query could never match them).
     */
    @SuppressWarnings("unchecked")
    static <T> Map<InwardChargeType, List<T>> groupByChargeType(List<Object[]> rows, int valueColumn, int typeColumn) {
        Map<InwardChargeType, List<T>> grouped = new EnumMap<>(InwardChargeType.class);
        if (rows == null) {
            return grouped;
        }
        for (Object[] row : rows) {
            Object type = row[typeColumn];
            if (!(type instanceof InwardChargeType)) {
                continue;
            }
            T value = (T) (valueColumn < 0 ? row : row[valueColumn]);
            grouped.computeIfAbsent((InwardChargeType) type, k -> new ArrayList<>()).add(value);
        }
        return grouped;
    }

    /**
     * Like {@link #groupByChargeType} for rows {@code [value, itemType, billItemType]}:
     * a row whose item has no type counts under the bill item's stored type, else
     * {@code defaultType} (issue #26272), the same rule the interim bill uses.
     */
    @SuppressWarnings("unchecked")
    static <T> Map<InwardChargeType, List<T>> groupByResolvedChargeType(List<Object[]> rows, int valueColumn,
            int itemTypeColumn, int billItemTypeColumn, InwardChargeType defaultType) {
        Map<InwardChargeType, List<T>> grouped = new EnumMap<>(InwardChargeType.class);
        if (rows == null) {
            return grouped;
        }
        for (Object[] row : rows) {
            InwardChargeType type = com.divudi.service.inward.InwardServiceItemChargeTypeService.resolve(
                    asType(row[itemTypeColumn]), asType(row[billItemTypeColumn]), defaultType);
            if (type == null) {
                continue;
            }
            grouped.computeIfAbsent(type, k -> new ArrayList<>()).add((T) row[valueColumn]);
        }
        return grouped;
    }

    private static InwardChargeType asType(Object o) {
        return o instanceof InwardChargeType ? (InwardChargeType) o : null;
    }

    /** Rows are {@code [BillFee, itemType, billItemType]}; see {@link #groupByResolvedChargeType}. */
    public void setServiceFees(List<Object[]> rows, InwardChargeType defaultType) {
        serviceFees.clear();
        serviceFees.putAll(InwardChargeTypeRunData.<BillFee>groupByResolvedChargeType(rows, 0, 1, 2, defaultType));
    }

    /** Rows are {@code [BillItem, itemType, billItemType]}; see {@link #groupByResolvedChargeType}. */
    public void setServiceItems(List<Object[]> rows, InwardChargeType defaultType) {
        serviceItems.clear();
        serviceItems.putAll(InwardChargeTypeRunData.<BillItem>groupByResolvedChargeType(rows, 0, 1, 2, defaultType));
    }

    /** Rows are {@code [itemType, billItemType, count]}: charge types the "no matrix" clear step reaches. */
    public void addBillItemRowTypes(List<Object[]> rows, InwardChargeType defaultType) {
        if (rows == null) {
            return;
        }
        for (Object[] row : rows) {
            InwardChargeType type = com.divudi.service.inward.InwardServiceItemChargeTypeService.resolve(
                    asType(row[0]), asType(row[1]), defaultType);
            if (type != null) {
                withOtherRows.add(type);
            }
        }
    }

    /** Rows are {@code [BillFee, chargeType]}. */
    public void setServiceFees(List<Object[]> rows) {
        serviceFees.clear();
        serviceFees.putAll(InwardChargeTypeRunData.<BillFee>groupByChargeType(rows, 0, 1));
    }

    /** Rows are {@code [BillItem, chargeType]}. */
    public void setServiceItems(List<Object[]> rows) {
        serviceItems.clear();
        serviceItems.putAll(InwardChargeTypeRunData.<BillItem>groupByChargeType(rows, 0, 1));
    }

    /** Rows are {@code [id, gross, margin, billTime, chargeType]}; kept whole, in id order. */
    public void setTimedItemRows(List<Object[]> rows) {
        timedItemRows.clear();
        timedItemRows.putAll(InwardChargeTypeRunData.<Object[]>groupByChargeType(rows, -1, 4));
    }

    /** Rows are {@code [PatientItem, chargeType]}: timed items with no BillItem. */
    public void setTimedPatientItems(List<Object[]> rows) {
        timedPatientItems.clear();
        timedPatientItems.putAll(InwardChargeTypeRunData.<PatientItem>groupByChargeType(rows, 0, 1));
    }

    /**
     * Charge types that have BillItems of any kind, or timed patient items
     * behind a BillItem: rows the "no matrix" clear steps would touch even when
     * the lists above are empty. Rows are {@code [chargeType, ...]}.
     */
    public void addOtherRowTypes(List<Object[]> rows) {
        if (rows == null) {
            return;
        }
        for (Object[] row : rows) {
            if (row[0] instanceof InwardChargeType) {
                withOtherRows.add((InwardChargeType) row[0]);
            }
        }
    }

    public List<BillFee> serviceFees(InwardChargeType type) {
        return serviceFees.getOrDefault(type, Collections.<BillFee>emptyList());
    }

    public List<BillItem> serviceItems(InwardChargeType type) {
        return serviceItems.getOrDefault(type, Collections.<BillItem>emptyList());
    }

    public List<Object[]> timedItemRows(InwardChargeType type) {
        return timedItemRows.getOrDefault(type, Collections.<Object[]>emptyList());
    }

    public List<PatientItem> timedPatientItems(InwardChargeType type) {
        return timedPatientItems.getOrDefault(type, Collections.<PatientItem>emptyList());
    }

    /** True when no discount step has anything to read or change for the charge type. */
    public boolean isEmpty(InwardChargeType type) {
        return !serviceFees.containsKey(type)
                && !serviceItems.containsKey(type)
                && !timedItemRows.containsKey(type)
                && !timedPatientItems.containsKey(type)
                && !withOtherRows.contains(type);
    }
}
