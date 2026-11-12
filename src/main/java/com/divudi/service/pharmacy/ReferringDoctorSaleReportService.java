package com.divudi.service.pharmacy;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.dto.ReferringDoctorSaleBillDTO;
import com.divudi.core.data.dto.ReferringDoctorSaleItemDTO;
import com.divudi.core.data.dto.ReferringDoctorSaleSummaryDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Doctor;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.DoctorFacade;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 * Referring doctor wise pharmacy retail sale report.
 *
 * Each retail sale is a PRE/settled pair; only the finance bill of a pair is
 * counted. Referring doctor is stored on the original sale only (sometimes
 * only on the pre bill), so cancellations and returns are traced back to the
 * original sale through explicit LEFT JOINs (an implicit path would be an
 * inner join and silently drop rows). Stored signs are already correct
 * (cancellations and returns are negative), so values are plain sums.
 */
@Stateless
public class ReferringDoctorSaleReportService {

    public static final String ROW_SALE = "Sale";
    public static final String ROW_CANCELLATION = "Cancellation";
    public static final String ROW_RETURN = "Return";
    public static final String ROW_RETURN_CANCELLATION = "Return Cancellation";

    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private DoctorFacade doctorFacade;

    private static final List<BillTypeAtomic> SALE_TYPES = Arrays.asList(
            BillTypeAtomic.PHARMACY_RETAIL_SALE,
            BillTypeAtomic.PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER);

    private static final List<BillTypeAtomic> RETURN_FINANCE_TYPES = Arrays.asList(
            BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS,
            BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS,
            BillTypeAtomic.PHARMACY_RETAIL_SALE_REFUND);

    private static final List<BillTypeAtomic> RETURN_PRE_TYPES = Arrays.asList(
            BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY,
            BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS_PREBILL);

    /**
     * The bill sets, each with the LEFT JOINs that reach the original
     * sale's doctor. Doctor aliases are d1..dN in priority order.
     */
    private enum BillSet {
        SALE(ROW_SALE,
                Arrays.asList(BillTypeAtomic.PHARMACY_RETAIL_SALE,
                        BillTypeAtomic.PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER),
                " left join b.referenceBill r1"
                + " left join b.referredBy d1"
                + " left join r1.referredBy d2",
                2, null),
        CANCELLATION(ROW_CANCELLATION,
                Arrays.asList(BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED),
                " left join b.billedBill s1"
                + " left join s1.referenceBill s2"
                + " left join b.referredBy d1"
                + " left join s1.referredBy d2"
                + " left join s2.referredBy d3",
                3, " and s1.billTypeAtomic in :saleTypes"),
        RETURN(ROW_RETURN,
                Arrays.asList(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS,
                        BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS,
                        BillTypeAtomic.PHARMACY_RETAIL_SALE_REFUND),
                " left join b.referenceBill q1"
                + " left join q1.billedBill q2"
                + " left join q2.referenceBill q3"
                + " left join b.billedBill q4"
                + " left join b.referredBy d1"
                + " left join q1.referredBy d2"
                + " left join q2.referredBy d3"
                + " left join q3.referredBy d4"
                + " left join q4.referredBy d5",
                5, null),
        // A cancelled return: positive values that add the return back. Counted only when the
        // cancelled return's refund is itself counted in the RETURN set, i.e. the cancelled bill
        // is a finance return, or a refund payment bill was made against it.
        RETURN_CANCELLATION(ROW_RETURN_CANCELLATION,
                Arrays.asList(BillTypeAtomic.PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION),
                " left join b.refundedBill c1"
                + " left join b.referenceBill c2"
                + " left join c1.billedBill c3"
                + " left join c3.referenceBill c4"
                + " left join b.referredBy d1"
                + " left join c1.referredBy d2"
                + " left join c2.referredBy d3"
                + " left join c3.referredBy d4"
                + " left join c4.referredBy d5",
                5, " and (c1.billTypeAtomic in :returnFinanceTypes"
                + " or (c1.billTypeAtomic in :returnPreTypes and exists ("
                + "select rp.id from Bill rp where rp.referenceBill = c1"
                + " and rp.billTypeAtomic in :returnFinanceTypes"
                + " and (rp.retired is null or rp.retired = false))))");

        final String rowType;
        final List<BillTypeAtomic> types;
        final String joins;
        final int doctorCount;
        final String extraWhere;

        BillSet(String rowType, List<BillTypeAtomic> types, String joins, int doctorCount, String extraWhere) {
            this.rowType = rowType;
            this.types = types;
            this.joins = joins;
            this.doctorCount = doctorCount;
            this.extraWhere = extraWhere;
        }
    }

    // ------------------------------------------------------------------
    // Shared query builders
    // ------------------------------------------------------------------
    /** Always five doctor id expressions; unused slots repeat the last candidate. */
    private String doctorIdSelect(BillSet set) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            if (i > 1) {
                sb.append(", ");
            }
            sb.append("d").append(Math.min(i, set.doctorCount)).append(".id");
        }
        return sb.toString();
    }

    private String whereClause(BillSet set, Department department, Map<String, Object> params,
            Date from, Date to, boolean forItems) {
        StringBuilder sb = new StringBuilder(" where (b.retired is null or b.retired = false)");
        if (forItems) {
            sb.append(" and (i.retired is null or i.retired = false)");
        }
        sb.append(" and b.billTypeAtomic in :types")
                .append(" and b.createdAt between :fd and :td");
        params.put("types", set.types);
        params.put("fd", from);
        params.put("td", to);
        if (set.extraWhere != null) {
            // CANCELLATION: excludes stray wholesale/transfer rows that reuse the cancelled atomic
            sb.append(set.extraWhere);
            if (set.extraWhere.contains(":saleTypes")) {
                params.put("saleTypes", SALE_TYPES);
            }
            if (set.extraWhere.contains(":returnFinanceTypes")) {
                params.put("returnFinanceTypes", RETURN_FINANCE_TYPES);
                params.put("returnPreTypes", RETURN_PRE_TYPES);
            }
        }
        if (department != null) {
            sb.append(" and dep.id = :deptId");
            params.put("deptId", department.getId());
        }
        return sb.toString();
    }

    private List<ReferringDoctorSaleBillDTO> fetchBillSet(BillSet set, Date from, Date to, Department department) {
        Map<String, Object> params = new HashMap<>();
        String jpql = "select new com.divudi.core.data.dto.ReferringDoctorSaleBillDTO("
                + "b.id, b.deptId, b.createdAt, b.billTypeAtomic, pp.name, b.paymentMethod, dep.name, "
                + "b.total, b.discount, b.netTotal, " + doctorIdSelect(set) + ")"
                + " from Bill b"
                + set.joins
                + " left join b.patient pt left join pt.person pp left join b.department dep"
                + whereClause(set, department, params, from, to, false);
        @SuppressWarnings("unchecked")
        List<ReferringDoctorSaleBillDTO> rows
                = (List<ReferringDoctorSaleBillDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
        if (rows == null) {
            return new ArrayList<>();
        }
        for (ReferringDoctorSaleBillDTO r : rows) {
            r.setRowType(set.rowType);
        }
        return rows;
    }

    private List<ReferringDoctorSaleItemDTO> fetchItemSet(BillSet set, Date from, Date to, Department department) {
        Map<String, Object> params = new HashMap<>();
        String jpql = "select new com.divudi.core.data.dto.ReferringDoctorSaleItemDTO("
                + doctorIdSelect(set) + ", it.id, it.name, p.qty, i.netValue)"
                + " from BillItem i join i.bill b"
                + " left join i.item it left join i.pharmaceuticalBillItem p"
                + set.joins
                + " left join b.department dep"
                + whereClause(set, department, params, from, to, true);
        @SuppressWarnings("unchecked")
        List<ReferringDoctorSaleItemDTO> rows
                = (List<ReferringDoctorSaleItemDTO>) billItemFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
        return rows == null ? new ArrayList<>() : rows;
    }

    private Map<Long, String> loadDoctorNames(Set<Long> ids) {
        Map<Long, String> names = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return names;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("ids", new ArrayList<>(ids));
        String jpql = "select d.id, p.name from Doctor d left join d.person p where d.id in :ids";
        List<Object[]> rows = doctorFacade.findObjectArrayByJpql(jpql, params, TemporalType.TIMESTAMP);
        if (rows != null) {
            for (Object[] r : rows) {
                Long id = (Long) r[0];
                String name = r[1] == null ? null : r[1].toString();
                names.put(id, name);
            }
        }
        return names;
    }

    private String displayName(Long doctorId, Map<Long, String> names) {
        if (doctorId == null) {
            return ReferringDoctorSaleBillDTO.NO_DOCTOR;
        }
        String n = names.get(doctorId);
        return (n == null || n.trim().isEmpty()) ? "Doctor #" + doctorId : n;
    }

    private boolean keepDoctor(Long resolvedId, Doctor doctor, boolean onlyWithoutDoctor) {
        if (doctor != null) {
            return resolvedId != null && resolvedId.equals(doctor.getId());
        }
        if (onlyWithoutDoctor) {
            return resolvedId == null;
        }
        return true;
    }

    private int compareDoctorNames(Long idA, String nameA, Long idB, String nameB) {
        if (idA == null && idB != null) {
            return 1;
        }
        if (idA != null && idB == null) {
            return -1;
        }
        String a = nameA == null ? "" : nameA;
        String b = nameB == null ? "" : nameB;
        return a.compareToIgnoreCase(b);
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------
    public List<ReferringDoctorSaleBillDTO> fetchBills(Date from, Date to, Department department,
            Doctor doctor, boolean onlyWithoutDoctor) {
        List<ReferringDoctorSaleBillDTO> all = new ArrayList<>();
        for (BillSet set : BillSet.values()) {
            all.addAll(fetchBillSet(set, from, to, department));
        }
        Set<Long> ids = new HashSet<>();
        for (ReferringDoctorSaleBillDTO r : all) {
            if (r.getDoctorId() != null) {
                ids.add(r.getDoctorId());
            }
        }
        Map<Long, String> names = loadDoctorNames(ids);
        List<ReferringDoctorSaleBillDTO> result = new ArrayList<>();
        for (ReferringDoctorSaleBillDTO r : all) {
            if (!keepDoctor(r.getDoctorId(), doctor, onlyWithoutDoctor)) {
                continue;
            }
            r.setDoctorName(displayName(r.getDoctorId(), names));
            result.add(r);
        }
        result.sort(Comparator.comparing(ReferringDoctorSaleBillDTO::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    public List<ReferringDoctorSaleSummaryDTO> summarize(List<ReferringDoctorSaleBillDTO> bills) {
        Map<Long, ReferringDoctorSaleSummaryDTO> map = new LinkedHashMap<>();
        if (bills != null) {
            for (ReferringDoctorSaleBillDTO b : bills) {
                ReferringDoctorSaleSummaryDTO s = map.get(b.getDoctorId());
                if (s == null) {
                    s = new ReferringDoctorSaleSummaryDTO(b.getDoctorId(), b.getDoctorName());
                    map.put(b.getDoctorId(), s);
                }
                double gross = b.getGrossTotal() == null ? 0 : b.getGrossTotal();
                double disc = b.getDiscount() == null ? 0 : b.getDiscount();
                double net = b.getNetTotal() == null ? 0 : b.getNetTotal();
                if (ROW_SALE.equals(b.getRowType())) {
                    s.setSaleCount(s.getSaleCount() + 1);
                    s.setSaleGross(s.getSaleGross() + gross);
                    s.setSaleDiscount(s.getSaleDiscount() + disc);
                    s.setSaleNet(s.getSaleNet() + net);
                } else if (ROW_CANCELLATION.equals(b.getRowType())) {
                    s.setCancelCount(s.getCancelCount() + 1);
                    s.setCancelValue(s.getCancelValue() + net);
                } else if (ROW_RETURN.equals(b.getRowType())) {
                    s.setReturnCount(s.getReturnCount() + 1);
                    s.setReturnValue(s.getReturnValue() + net);
                } else if (ROW_RETURN_CANCELLATION.equals(b.getRowType())) {
                    // a cancelled return is added back: one return fewer, value netted off
                    s.setReturnCount(s.getReturnCount() - 1);
                    s.setReturnValue(s.getReturnValue() + net);
                }
            }
        }
        List<ReferringDoctorSaleSummaryDTO> result = new ArrayList<>(map.values());
        for (ReferringDoctorSaleSummaryDTO s : result) {
            s.setNetValue(s.getSaleNet() + s.getCancelValue() + s.getReturnValue());
        }
        result.sort((a, b) -> compareDoctorNames(a.getDoctorId(), a.getDoctorName(), b.getDoctorId(), b.getDoctorName()));
        return result;
    }

    public List<ReferringDoctorSaleItemDTO> fetchItems(Date from, Date to, Department department,
            Doctor doctor, boolean onlyWithoutDoctor) {
        List<ReferringDoctorSaleItemDTO> all = new ArrayList<>();
        for (BillSet set : BillSet.values()) {
            all.addAll(fetchItemSet(set, from, to, department));
        }
        Set<Long> ids = new HashSet<>();
        for (ReferringDoctorSaleItemDTO r : all) {
            if (r.getDoctorId() != null) {
                ids.add(r.getDoctorId());
            }
        }
        Map<Long, String> names = loadDoctorNames(ids);
        Map<String, ReferringDoctorSaleItemDTO> grouped = new LinkedHashMap<>();
        for (ReferringDoctorSaleItemDTO r : all) {
            if (!keepDoctor(r.getDoctorId(), doctor, onlyWithoutDoctor)) {
                continue;
            }
            String key = r.getDoctorId() + "|" + r.getItemId();
            ReferringDoctorSaleItemDTO g = grouped.get(key);
            if (g == null) {
                r.setDoctorName(displayName(r.getDoctorId(), names));
                grouped.put(key, r);
            } else {
                g.setQuantity(g.getQuantity() + r.getQuantity());
                g.setNetValue(g.getNetValue() + r.getNetValue());
            }
        }
        List<ReferringDoctorSaleItemDTO> result = new ArrayList<>(grouped.values());
        result.sort((a, b) -> {
            int c = compareDoctorNames(a.getDoctorId(), a.getDoctorName(), b.getDoctorId(), b.getDoctorName());
            if (c != 0) {
                return c;
            }
            String x = a.getItemName() == null ? "" : a.getItemName();
            String y = b.getItemName() == null ? "" : b.getItemName();
            return x.compareToIgnoreCase(y);
        });
        return result;
    }
}
