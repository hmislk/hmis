package com.divudi.service;

import com.divudi.core.data.BillType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.PaymentFacade;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 * Splits the value of bills paid by Multiple Payment Methods by the Payment
 * rows of their batch bill (#26211).
 *
 * An OPD bill paid by Multiple Payment Methods has
 * Bill.paymentMethod = MultiplePaymentMethods. The individual bill has no
 * Payment rows. The Payment rows (for example Cash and Card) are on the batch
 * bill. The individual bill points to the batch bill with
 * backwardReferenceBill. An individual cancellation bill points to the
 * cancellation batch bill with forwardReferenceBill.
 *
 * Each Payment row gets a share of the batch: paidValue divided by the sum of
 * the paid values of all its rows. A bill takes its value times that share.
 * All individual bills of one batch use the same shares. The sign of the bill
 * value is kept, so cancellation and refund bills come out negative.
 *
 * Credit is never part of Multiple Payment Methods (policy), so this service
 * does not handle credit.
 */
@Stateless
public class MultiplePaymentSplitService {

    @EJB
    private BillFacade billFacade;
    @EJB
    private PaymentFacade paymentFacade;

    /**
     * One bill to split: its value and the ids of the two possible batch bills.
     */
    public static class BillValue {

        private final Long billId;
        private final double value;
        private final Long backwardBatchId;
        private final Long forwardBatchId;

        public BillValue(Long billId, double value, Long backwardBatchId, Long forwardBatchId) {
            this.billId = billId;
            this.value = value;
            this.backwardBatchId = backwardBatchId;
            this.forwardBatchId = forwardBatchId;
        }

        public Long getBillId() {
            return billId;
        }

        public double getValue() {
            return value;
        }

        public Long getBackwardBatchId() {
            return backwardBatchId;
        }

        public Long getForwardBatchId() {
            return forwardBatchId;
        }
    }

    /**
     * Splits one value by the paid amounts of the payment methods. The parts
     * add up to the value. Returns an empty map when there is nothing paid.
     *
     * Example: value 1000, paid Cash 400 and Card 600 gives Cash 400, Card 600.
     */
    public static Map<PaymentMethod, Double> splitValue(double value, Map<PaymentMethod, Double> paidByMethod) {
        Map<PaymentMethod, Double> result = new EnumMap<>(PaymentMethod.class);
        if (paidByMethod == null || paidByMethod.isEmpty()) {
            return result;
        }
        double paidTotal = 0;
        for (Double d : paidByMethod.values()) {
            if (d != null) {
                paidTotal += d;
            }
        }
        if (paidTotal == 0) {
            return result;
        }
        for (Map.Entry<PaymentMethod, Double> e : paidByMethod.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) {
                continue;
            }
            result.put(e.getKey(), value * (e.getValue() / paidTotal));
        }
        return result;
    }

    /**
     * Splits each bill by the payments of its batch bill. A negative bill
     * (cancellation or refund) looks at its forward batch first, then at its
     * backward batch. Other bills look at the backward batch first. A bill
     * with no payments on either batch is left out of the result.
     *
     * @param bills the bills to split
     * @param paymentsByBatchId paid amounts by payment method, by batch bill id
     * @return for each bill id, the part of its value by payment method
     */
    public static Map<Long, Map<PaymentMethod, Double>> splitBills(Collection<BillValue> bills,
            Map<Long, Map<PaymentMethod, Double>> paymentsByBatchId) {
        Map<Long, Map<PaymentMethod, Double>> result = new HashMap<>();
        if (bills == null || paymentsByBatchId == null) {
            return result;
        }
        for (BillValue b : bills) {
            Long first = b.getValue() < 0 ? b.getForwardBatchId() : b.getBackwardBatchId();
            Long second = b.getValue() < 0 ? b.getBackwardBatchId() : b.getForwardBatchId();
            Map<PaymentMethod, Double> paid = first == null ? null : paymentsByBatchId.get(first);
            if (paid == null || paid.isEmpty()) {
                paid = second == null ? null : paymentsByBatchId.get(second);
            }
            Map<PaymentMethod, Double> parts = splitValue(b.getValue(), paid);
            if (!parts.isEmpty()) {
                result.put(b.getBillId(), parts);
            }
        }
        return result;
    }

    /**
     * Adds the split values of all bills into one total for each payment
     * method.
     */
    public static Map<PaymentMethod, Double> totalByMethod(Map<Long, Map<PaymentMethod, Double>> splitBills) {
        Map<PaymentMethod, Double> totals = new EnumMap<>(PaymentMethod.class);
        if (splitBills == null) {
            return totals;
        }
        for (Map<PaymentMethod, Double> parts : splitBills.values()) {
            for (Map.Entry<PaymentMethod, Double> e : parts.entrySet()) {
                totals.merge(e.getKey(), e.getValue(), Double::sum);
            }
        }
        return totals;
    }

    /**
     * The part of a bill that was paid by the given payment methods, as a
     * share from 0 to 1. Example: Cash 400 and Patient Deposit 600 with
     * included = Cash gives 0.4.
     */
    public static double includedShare(Map<PaymentMethod, Double> splitOfOne, Set<PaymentMethod> included) {
        double share = 0;
        if (splitOfOne == null || included == null) {
            return share;
        }
        for (Map.Entry<PaymentMethod, Double> e : splitOfOne.entrySet()) {
            if (included.contains(e.getKey())) {
                share += e.getValue();
            }
        }
        return share;
    }

    /**
     * Adds up value times share for every bill that has a share.
     */
    public static double scaledSum(Map<Long, Double> valueByBillId, Map<Long, Double> shareByBillId) {
        double total = 0;
        if (valueByBillId == null || shareByBillId == null) {
            return total;
        }
        for (Map.Entry<Long, Double> e : valueByBillId.entrySet()) {
            Double share = shareByBillId.get(e.getKey());
            if (share != null && e.getValue() != null) {
                total += e.getValue() * share;
            }
        }
        return total;
    }

    /**
     * Net total of the non-retired bills of one bill type that were paid by
     * Multiple Payment Methods, split by the payment method of their payment
     * rows. Includes cancellation and refund bills as negative values.
     *
     * @param billType the bill type of the individual bills, for example OpdBill
     * @param fromDate start of the date-time range
     * @param toDate end of the date-time range
     * @param institution the institution of the bills
     * @return net total for each payment method, empty when there are none
     */
    public Map<PaymentMethod, Double> fetchNetTotalByPaymentMethod(BillType billType, Date fromDate, Date toDate, Institution institution) {
        List<Object[]> rows = fetchMultiplePaymentBillRows(billType, fromDate, toDate, institution);
        return totalByMethod(splitBills(toBillValues(rows, false), fetchPaidForBatchesOf(rows)));
    }

    /**
     * Share paid by the included payment methods, for each Multiple Payment
     * Methods bill of one bill type in a date-time range. Bills with no share
     * (nothing paid by an included method, or no payments found) are not in
     * the result.
     *
     * @param billType the bill type of the individual bills, for example OpdBill
     * @param fromDate start of the date-time range
     * @param toDate end of the date-time range
     * @param institution the institution of the bills
     * @param included the payment methods the report counts
     * @return share from 0 to 1 by bill id
     */
    public Map<Long, Double> fetchIncludedShares(BillType billType, Date fromDate, Date toDate, Institution institution, Set<PaymentMethod> included) {
        return sharesOf(fetchMultiplePaymentBillRows(billType, fromDate, toDate, institution), included);
    }

    /**
     * Same as the method above, for bills picked by id.
     */
    public Map<Long, Double> fetchIncludedShares(Collection<Long> billIds, Set<PaymentMethod> included) {
        List<Object[]> rows = new ArrayList<>();
        if (billIds == null || billIds.isEmpty()) {
            return new HashMap<>();
        }
        for (List<Long> chunk : chunks(billIds)) {
            Map<String, Object> m = new HashMap<>();
            String jpql = "select b.id, b.netTotal, bb.id, fb.id "
                    + " from Bill b "
                    + " left join b.backwardReferenceBill bb "
                    + " left join b.forwardReferenceBill fb "
                    + " where b.id in :ids ";
            m.put("ids", chunk);
            rows.addAll(billFacade.findObjectArrayByJpql(jpql, m, TemporalType.TIMESTAMP));
        }
        return sharesOf(rows, included);
    }

    private List<Object[]> fetchMultiplePaymentBillRows(BillType billType, Date fromDate, Date toDate, Institution institution) {
        Map<String, Object> m = new HashMap<>();
        String jpql = "select b.id, b.netTotal, bb.id, fb.id "
                + " from Bill b "
                + " left join b.backwardReferenceBill bb "
                + " left join b.forwardReferenceBill fb "
                + " where b.retired=false "
                + " and b.billType=:bt "
                + " and b.paymentMethod=:pm "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate ";
        m.put("bt", billType);
        m.put("pm", PaymentMethod.MultiplePaymentMethods);
        m.put("ins", institution);
        m.put("fromDate", fromDate);
        m.put("toDate", toDate);
        return billFacade.findObjectArrayByJpql(jpql, m, TemporalType.TIMESTAMP);
    }

    private Map<Long, Double> sharesOf(List<Object[]> rows, Set<PaymentMethod> included) {
        Map<Long, Double> shares = new HashMap<>();
        if (rows.isEmpty()) {
            return shares;
        }
        // Use 1 or -1 as the value, so the result is the share and not a money value.
        Map<Long, Map<PaymentMethod, Double>> split = splitBills(toBillValues(rows, true), fetchPaidForBatchesOf(rows));
        for (Map.Entry<Long, Map<PaymentMethod, Double>> e : split.entrySet()) {
            double share = Math.abs(includedShare(e.getValue(), included));
            if (share > 0) {
                shares.put(e.getKey(), share);
            }
        }
        return shares;
    }

    private static List<BillValue> toBillValues(List<Object[]> rows, boolean signOnly) {
        List<BillValue> bills = new ArrayList<>();
        for (Object[] r : rows) {
            double value = r[1] == null ? 0.0 : ((Number) r[1]).doubleValue();
            if (signOnly) {
                value = value < 0 ? -1.0 : 1.0;
            }
            bills.add(new BillValue((Long) r[0], value, (Long) r[2], (Long) r[3]));
        }
        return bills;
    }

    private Map<Long, Map<PaymentMethod, Double>> fetchPaidForBatchesOf(List<Object[]> rows) {
        Set<Long> batchIds = new HashSet<>();
        for (Object[] r : rows) {
            if (r[2] != null) {
                batchIds.add((Long) r[2]);
            }
            if (r[3] != null) {
                batchIds.add((Long) r[3]);
            }
        }
        return fetchPaidByMethod(batchIds);
    }

    private static final int CHUNK_SIZE = 500;

    private static List<List<Long>> chunks(Collection<Long> ids) {
        List<List<Long>> result = new ArrayList<>();
        List<Long> all = new ArrayList<>(ids);
        for (int i = 0; i < all.size(); i += CHUNK_SIZE) {
            result.add(new ArrayList<>(all.subList(i, Math.min(i + CHUNK_SIZE, all.size()))));
        }
        return result;
    }

    private Map<Long, Map<PaymentMethod, Double>> fetchPaidByMethod(Collection<Long> batchIds) {
        Map<Long, Map<PaymentMethod, Double>> result = new HashMap<>();
        if (batchIds == null || batchIds.isEmpty()) {
            return result;
        }
        for (List<Long> chunk : chunks(batchIds)) {
            Map<String, Object> m = new HashMap<>();
            String jpql = "select p.bill.id, p.paymentMethod, sum(p.paidValue) "
                    + " from Payment p "
                    + " where p.retired=false "
                    + " and p.bill.id in :batchIds "
                    + " group by p.bill.id, p.paymentMethod ";
            m.put("batchIds", chunk);
            List<Object[]> rows = paymentFacade.findObjectArrayByJpql(jpql, m, TemporalType.TIMESTAMP);
            for (Object[] r : rows) {
                Long batchId = (Long) r[0];
                PaymentMethod pm = (PaymentMethod) r[1];
                double paid = r[2] == null ? 0.0 : ((Number) r[2]).doubleValue();
                if (batchId == null || pm == null) {
                    continue;
                }
                result.computeIfAbsent(batchId, k -> new EnumMap<>(PaymentMethod.class)).merge(pm, paid, Double::sum);
            }
        }
        return result;
    }
}
