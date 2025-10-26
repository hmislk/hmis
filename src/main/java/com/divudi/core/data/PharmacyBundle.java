package com.divudi.core.data;

import static com.divudi.core.data.BillCategory.BILL;
import static com.divudi.core.data.BillCategory.CANCELLATION;
import static com.divudi.core.data.BillCategory.PAYMENTS;
import static com.divudi.core.data.BillCategory.PREBILL;
import static com.divudi.core.data.BillCategory.REFUND;
import static com.divudi.core.data.PaymentMethod.Agent;
import static com.divudi.core.data.PaymentMethod.Card;
import static com.divudi.core.data.PaymentMethod.Cash;
import static com.divudi.core.data.PaymentMethod.Cheque;
import static com.divudi.core.data.PaymentMethod.Credit;
import static com.divudi.core.data.PaymentMethod.IOU;
import static com.divudi.core.data.PaymentMethod.MultiplePaymentMethods;
import static com.divudi.core.data.PaymentMethod.None;
import static com.divudi.core.data.PaymentMethod.OnCall;
import static com.divudi.core.data.PaymentMethod.OnlineSettlement;
import static com.divudi.core.data.PaymentMethod.PatientDeposit;
import static com.divudi.core.data.PaymentMethod.PatientPoints;
import static com.divudi.core.data.PaymentMethod.Slip;
import static com.divudi.core.data.PaymentMethod.Staff;
import static com.divudi.core.data.PaymentMethod.Staff_Welfare;
import static com.divudi.core.data.PaymentMethod.Voucher;
import static com.divudi.core.data.PaymentMethod.YouOweMe;
import static com.divudi.core.data.PaymentMethod.ewallet;
import com.divudi.core.entity.*;
import com.divudi.core.entity.channel.SessionInstance;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.light.common.BillLight;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import java.util.*;

/**
 * @author buddhika
 */
public class PharmacyBundle implements Serializable {

    private static final long serialVersionUID = 1L;

    // UUID field to uniquely identify each object
    private UUID id;

    //    private SessionController sessionController;
    private List<com.divudi.core.entity.cashTransaction.Denomination> denominations;

    private List<PharmacyBundle> bundles;
    private ReportTemplate reportTemplate;
    private List<PharmacyRow> rows;
    private PharmacyRow summaryRow;
    private Map<String, List<BillItem>> groupedBillItems;
    private Map<Institution, List<Bill>> groupedBillItemsByInstitution;

    private String name;
    private String bundleType;
    private String description;
    private SessionInstance sessionInstance;

    private PaymentMethod paymentMethod;

    private double netTotal;
    private double paidTotal;

    private double saleValue;
    private double purchaseValue;
    private double grossProfitValue;

    private BigDecimal grossSaleRate = BigDecimal.ZERO;
    private BigDecimal discountRate = BigDecimal.ZERO;
    private BigDecimal marginRate = BigDecimal.ZERO;
    private BigDecimal netSaleRate = BigDecimal.ZERO;
    private BigDecimal grossSaleValue = BigDecimal.ZERO;
    private BigDecimal marginValue = BigDecimal.ZERO;
    private BigDecimal discountValue = BigDecimal.ZERO;
    private BigDecimal netSaleValue = BigDecimal.ZERO;

    private double onCallValue;
    private double cashValue;
    private double cardValue;
    private double multiplePaymentMethodsValue;
    private double staffValue;
    private double creditValue;
    private double staffWelfareValue;
    private double voucherValue;
    private double iouValue;
    private double agentValue;
    private double chequeValue;
    private double slipValue;
    private double eWalletValue;
    private double patientDepositValue;
    private double patientPointsValue;
    private double onlineSettlementValue;

    // Booleans to track transactions
    private boolean hasOnCallTransaction;
    private boolean hasCashTransaction;
    private boolean hasCardTransaction;
    private boolean hasMultiplePaymentMethodsTransaction;
    private boolean hasStaffTransaction;
    private boolean hasCreditTransaction;
    private boolean hasStaffWelfareTransaction;
    private boolean hasVoucherTransaction;
    private boolean hasIouTransaction;
    private boolean hasAgentTransaction;
    private boolean hasChequeTransaction;
    private boolean hasSlipTransaction;
    private boolean hasEWalletTransaction;
    private boolean hasPatientDepositTransaction;
    private boolean hasPatientPointsTransaction;
    private boolean hasOnlineSettlementTransaction;

    private WebUser user;
    private WebUser fromUser;
    private WebUser toUser;
    private Date date;
    private Department department;
    private List<Department> departments;
    private Bill startBill;
    private Bill endBill;

    private PaymentHandover paymentHandover;

    private boolean selected;

    private boolean patientDepositsAreConsideredInHandingover = true;

    private Double quantity;
    private Double freeQuantity;
    private Double quantityPlusFreeQuantity;
    private Double quantityValueAtPurchaseRate;
    private Double freeQuantityValueAtPurchaseRate;
    private Double quantityPlusFreeQuantityValueAtPurchaseRate;
    private Double quantityValueAtRetailSaleRate;
    private Double freeQuantityValueAtRetailSaleRate;
    private Double quantityPlusFreeQuantityValueAtRetailSaleRate;

    public PharmacyBundle() {
        this.id = UUID.randomUUID();
        this.rows = new ArrayList<>();
    }

    public void populateSummaryRow() {
        // Initialize all sums to zero
        double sumOfCashValues = 0.0;
        double sumOfCardValues = 0.0;
        double sumOfMultiplePaymentMethodsValues = 0.0;
        double sumOfStaffValues = 0.0;
        double sumOfCreditValues = 0.0;
        double sumOfStaffWelfareValues = 0.0;
        double sumOfVoucherValues = 0.0;
        double sumOfIouValues = 0.0;
        double sumOfAgentValues = 0.0;
        double sumOfChequeValues = 0.0;
        double sumOfSlipValues = 0.0;
        double sumOfEWalletValues = 0.0;
        double sumOfPatientDepositValues = 0.0;
        double sumOfPatientPointsValues = 0.0;
        double sumOfOnlineSettlementValues = 0.0;
        double sumOfNoneValues = 0.0;
        double sumOfOpdCreditValues = 0.0;
        double sumOfInpatientCreditValues = 0.0;

        double sumOfGrossTotal = 0.0;
        double sumOfDiscount = 0.0;
        double sumOfServiceCharge = 0.0;
        double sumOfTax = 0.0;
        double sumOfActualTotal = 0.0;
        double sumOfNetTotal = 0.0;

        BigDecimal sumOfStocksAtCostRate = BigDecimal.ZERO;
        BigDecimal sumOfStocksAtPurchaseRate = BigDecimal.ZERO;
        BigDecimal sumOfStocksAtRetailSaleRate = BigDecimal.ZERO;

        // Aggregate all rows
        for (PharmacyRow r : rows) {
            sumOfCashValues += r.getCashValue();
            sumOfCardValues += r.getCardValue();
            sumOfMultiplePaymentMethodsValues += r.getMultiplePaymentMethodsValue();
            sumOfStaffValues += r.getStaffValue();
            sumOfCreditValues += r.getCreditValue();
            sumOfStaffWelfareValues += r.getStaffWelfareValue();
            sumOfVoucherValues += r.getVoucherValue();
            sumOfIouValues += r.getIouValue();
            sumOfAgentValues += r.getAgentValue();
            sumOfChequeValues += r.getChequeValue();
            sumOfSlipValues += r.getSlipValue();
            sumOfEWalletValues += r.getEwalletValue();
            sumOfPatientDepositValues += r.getPatientDepositValue();
            sumOfPatientPointsValues += r.getPatientPointsValue();
            sumOfOnlineSettlementValues += r.getOnlineSettlementValue();
            sumOfNoneValues += r.getNoneValue();
            sumOfOpdCreditValues += r.getOpdCreditValue();
            sumOfInpatientCreditValues += r.getInpatientCreditValue();

            sumOfGrossTotal += r.getGrossTotal();
            sumOfDiscount += r.getDiscount();
            sumOfServiceCharge += r.getServiceCharge();
            sumOfTax += r.getTax();
            sumOfActualTotal += r.getActualTotal();
            sumOfNetTotal += r.getNetTotal();

            if (r.getValueOfStocksAtCostRate() != null) {
                sumOfStocksAtCostRate = sumOfStocksAtCostRate.add(r.getValueOfStocksAtCostRate());
            }
            if (r.getValueOfStocksAtPurchaseRate() != null) {
                sumOfStocksAtPurchaseRate = sumOfStocksAtPurchaseRate.add(r.getValueOfStocksAtPurchaseRate());
            }
            if (r.getValueOfStocksAtRetailSaleRate() != null) {
                sumOfStocksAtRetailSaleRate = sumOfStocksAtRetailSaleRate.add(r.getValueOfStocksAtRetailSaleRate());
            }
        }

        // Set summary row values
        getSummaryRow().setCashValue(sumOfCashValues);
        getSummaryRow().setCardValue(sumOfCardValues);
        getSummaryRow().setMultiplePaymentMethodsValue(sumOfMultiplePaymentMethodsValues);
        getSummaryRow().setStaffValue(sumOfStaffValues);
        getSummaryRow().setCreditValue(sumOfCreditValues);
        getSummaryRow().setStaffWelfareValue(sumOfStaffWelfareValues);
        getSummaryRow().setVoucherValue(sumOfVoucherValues);
        getSummaryRow().setIouValue(sumOfIouValues);
        getSummaryRow().setAgentValue(sumOfAgentValues);
        getSummaryRow().setChequeValue(sumOfChequeValues);
        getSummaryRow().setSlipValue(sumOfSlipValues);
        getSummaryRow().setEwalletValue(sumOfEWalletValues);
        getSummaryRow().setPatientDepositValue(sumOfPatientDepositValues);
        getSummaryRow().setPatientPointsValue(sumOfPatientPointsValues);
        getSummaryRow().setOnlineSettlementValue(sumOfOnlineSettlementValues);
        getSummaryRow().setNoneValue(sumOfNoneValues);
        getSummaryRow().setOpdCreditValue(sumOfOpdCreditValues);
        getSummaryRow().setInpatientCreditValue(sumOfInpatientCreditValues);

        getSummaryRow().setGrossTotal(sumOfGrossTotal);
        getSummaryRow().setDiscount(sumOfDiscount);
        getSummaryRow().setServiceCharge(sumOfServiceCharge);
        getSummaryRow().setTax(sumOfTax);
        getSummaryRow().setActualTotal(sumOfActualTotal);
        getSummaryRow().setNetTotal(sumOfNetTotal);

        getSummaryRow().setValueOfStocksAtCostRate(sumOfStocksAtCostRate);
        getSummaryRow().setValueOfStocksAtPurchaseRate(sumOfStocksAtPurchaseRate);
        getSummaryRow().setValueOfStocksAtRetailSaleRate(sumOfStocksAtRetailSaleRate);
    }

    public PharmacyBundle(List<?> entries) {
        this(); // Initialize id and rows list
        if (entries != null && !entries.isEmpty()) {
            Object firstElement = entries.get(0);
            if (firstElement instanceof Bill) {
                // Process list as Bills
                for (Object obj : entries) {
                    if (obj instanceof Bill) {
                        Bill bill = (Bill) obj;
                        PharmacyRow ir = new PharmacyRow(bill);

                        rows.add(ir);
                    }
                }
            } else if (firstElement instanceof BillLight) {
                // Process list as BillLights
                for (Object obj : entries) {
                    if (obj instanceof BillLight) {
                        BillLight billLight = (BillLight) obj;
                        PharmacyRow ir = new PharmacyRow(billLight);
                        rows.add(ir);
                    }
                }
            } else if (firstElement instanceof BillItem) {
                // Process list as BillItems
                for (Object obj : entries) {
                    if (obj instanceof BillItem) {
                        BillItem billItem = (BillItem) obj;
                        PharmacyRow ir = new PharmacyRow(billItem, true);
                        rows.add(ir);
                    }
                }
            } else if (firstElement instanceof PharmaceuticalBillItem) {
                // Process list as PharmaceuticalBillItems
                for (Object obj : entries) {
                    if (obj instanceof PharmaceuticalBillItem) {
                        PharmaceuticalBillItem pbi = (PharmaceuticalBillItem) obj;
                        PharmacyRow ir = new PharmacyRow(pbi);
                        rows.add(ir);
                    }
                }
            } else if (firstElement instanceof PharmacyRow) {
                // Process list as PharmacyRows
                for (Object obj : entries) {
                    if (obj instanceof PharmacyRow) {
                        PharmacyRow incomeRow = (PharmacyRow) obj;
                        rows.add(incomeRow);
                    }
                }
            }
        }
    }

    public void generateProcurementForBills() {
        for (PharmacyRow r : getRows()) {
            Bill b = r.getBill();
            populateRowFromBill(r, b);
        }
        populateSummaryRow();
    }

    /**
     * Generates procurement details for BillLight objects.
     * Processes each row by populating it from the BillLight data.
     */
    public void generateProcurementForBillLights() {
        for (PharmacyRow r : getRows()) {
            BillLight b = r.getBillLight();
            populateRowFromBill(r, b);
        }
        populateSummaryRow();
    }

    public void generateRetailAndCostDetailsForPharmaceuticalBillItems() {
        saleValue = 0;
        purchaseValue = 0;
        grossProfitValue = 0;

        for (PharmacyRow r : getRows()) {
            PharmaceuticalBillItem b = r.getPharmaceuticalBillItem();
            if (b == null || b.getBillItem() == null || b.getBillItem().getBill() == null) {
                continue;
            }

            BillTypeAtomic bta = Optional
                    .ofNullable(b.getBillItem())
                    .map(BillItem::getBill)
                    .map(Bill::getBillTypeAtomic)
                    .orElse(null);
            if (bta == null || bta.getBillCategory() == null) {
                continue; // unable to categorise safely
            }
            BillCategory bc = bta.getBillCategory();

            Double q = b.getQty();
            Double rRate = b.getRetailRate();
            if (bta == BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS) {
                rRate = b.getBillItem().getNetRate();
            }

            Double pRate = b.getPurchaseRate();

            if (q == null || rRate == null || pRate == null) {
                continue;
            }

            double qty = Math.abs(q);
            double retail = Math.abs(rRate);
            double purchase = Math.abs(pRate);

            double retailTotal = 0;
            double purchaseTotal = 0;
            double grossProfit = 0;

            switch (bc) {
                case BILL:
                case PAYMENTS:
                case PREBILL:
                    retailTotal = retail * qty;
                    purchaseTotal = purchase * qty;
                    grossProfit = (retail - purchase) * qty;
                    break;

                case CANCELLATION:
                case REFUND:
                    retailTotal = -retail * qty;
                    purchaseTotal = -purchase * qty;
                    grossProfit = -(retail - purchase) * qty;
                    break;

                default:
                    break;
            }

            saleValue += retailTotal;
            purchaseValue += purchaseTotal;
            grossProfitValue += grossProfit;
        }
    }

    public void generateRetailAndCostDetailsForPharmaceuticalBillType() {
        saleValue = 0;
        purchaseValue = 0;
        grossProfitValue = 0;

        Map<BillTypeAtomic, PharmacyRow> grouped = new LinkedHashMap<>();

        for (PharmacyRow r : getRows()) {
            PharmaceuticalBillItem b = r.getPharmaceuticalBillItem();
            if (b == null || b.getBillItem() == null || b.getBillItem().getBill() == null) {
                continue;
            }

            BillTypeAtomic bta = Optional.ofNullable(b.getBillItem())
                    .map(BillItem::getBill)
                    .map(Bill::getBillTypeAtomic)
                    .orElse(null);

            if (bta == null || bta.getBillCategory() == null) {
                continue;
            }

            BillCategory bc = bta.getBillCategory();
            Double q = b.getQty();
//            Double rRate = bta == BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS
//                    ? b.getBillItem().getNetRate()
//                    : b.getRetailRate();
            Double rRate = b.getRetailRate();
            Double pRate = b.getPurchaseRate();

            if (q == null || rRate == null || pRate == null) {
                continue;
            }

            double qty = Math.abs(q);
            double retail = Math.abs(rRate);
            double purchase = Math.abs(pRate);

            double retailTotal = 0;
            double purchaseTotal = 0;
            double grossProfit = 0;

            switch (bc) {
                case BILL:
                case PAYMENTS:
                case PREBILL:
                    retailTotal = retail * qty;
                    purchaseTotal = purchase * qty;
                    grossProfit = (retail - purchase) * qty;
                    break;
                case CANCELLATION:
                case REFUND:
                    retailTotal = -retail * qty;
                    purchaseTotal = -purchase * qty;
                    grossProfit = -(retail - purchase) * qty;
                    break;
                default:
                    break;
            }

            PharmacyRow groupRow = grouped.computeIfAbsent(bta, k -> {
                PharmacyRow ir = new PharmacyRow();
                ir.setBillTypeAtomic(k);
                return ir;
            });

            groupRow.setRetailValue(groupRow.getRetailValue() + retailTotal);
            groupRow.setPurchaseValue(groupRow.getPurchaseValue() + purchaseTotal);
            groupRow.setGrossProfit(groupRow.getGrossProfit() + grossProfit);

            saleValue += retailTotal;
            purchaseValue += purchaseTotal;
            grossProfitValue += grossProfit;
        }

        // Replace old rows with grouped values
        getRows().clear();
        getRows().addAll(grouped.values());

    }

    public void generateProcurementDetailsForBillItems() {
        // Reset all values before starting calculations
        quantity = 0.0;
        freeQuantity = 0.0;
        quantityPlusFreeQuantity = 0.0;
        quantityValueAtPurchaseRate = 0.0;
        freeQuantityValueAtPurchaseRate = 0.0;
        quantityPlusFreeQuantityValueAtPurchaseRate = 0.0;
        quantityValueAtRetailSaleRate = 0.0;
        freeQuantityValueAtRetailSaleRate = 0.0;
        quantityPlusFreeQuantityValueAtRetailSaleRate = 0.0;

        for (PharmacyRow r : getRows()) {
            BillItem bi = r.getBillItem();
            if (bi == null || bi.getPharmaceuticalBillItem() == null) {
                continue;
            }

            double qty = bi.getPharmaceuticalBillItem().getQty();
            double freeQty = bi.getPharmaceuticalBillItem().getFreeQty();
            double purchaseRate = bi.getPharmaceuticalBillItem().getPurchaseRate();
            double retailRate = bi.getPharmaceuticalBillItem().getRetailRate();

            // Add current row's quantities to totals
            quantity += qty;
            freeQuantity += freeQty;
            quantityPlusFreeQuantity += qty + freeQty;

            // Calculate and add values at purchase rate
            quantityValueAtPurchaseRate += qty * purchaseRate;
            freeQuantityValueAtPurchaseRate += freeQty * purchaseRate;
            quantityPlusFreeQuantityValueAtPurchaseRate += (qty + freeQty) * purchaseRate;

            // Calculate and add values at retail rate
            quantityValueAtRetailSaleRate += qty * retailRate;
            freeQuantityValueAtRetailSaleRate += freeQty * retailRate;
            quantityPlusFreeQuantityValueAtRetailSaleRate += (qty + freeQty) * retailRate;
        }
    }

    // ChatGPT contributed - Groups PharmacyRow list by Item ID to avoid detached entity issues
    public void groupSaleDetailsByItems() {
        grossSaleValue = BigDecimal.ZERO;
        marginValue = BigDecimal.ZERO;
        discountValue = BigDecimal.ZERO;
        netSaleValue = BigDecimal.ZERO;
        quantity = 0.0;
        Map<Long, PharmacyRow> itemRowMap = new HashMap<>();
        for (PharmacyRow r : getRows()) {
            PharmaceuticalBillItem pbi = r.getPharmaceuticalBillItem();
            if (pbi == null) {
                continue;
            }
            BillItem bi = pbi.getBillItem();
            if (bi == null) {
                continue;
            }
            if (bi.getItem() == null) {
                continue;
            }
            Item item = bi.getItem();
            Long itemId = item.getId();
            if (itemId == null) {
                continue;
            }
            double qty = bi.getQty();
            BigDecimal gross = BigDecimal.valueOf(bi.getGrossValue());
            BigDecimal margin = BigDecimal.valueOf(bi.getMarginValue());
            BigDecimal discount = BigDecimal.valueOf(bi.getDiscount());
            BigDecimal net = BigDecimal.valueOf(bi.getNetValue());
            PharmacyRow row = itemRowMap.get(itemId);
            if (row == null) {
                row = new PharmacyRow();
                row.setItem(item);
                row.setQuantity(0.0);
                row.setGrossSaleValue(BigDecimal.ZERO);
                row.setMarginValue(BigDecimal.ZERO);
                row.setDiscountValue(BigDecimal.ZERO);
                row.setNetSaleValue(BigDecimal.ZERO);
                itemRowMap.put(itemId, row);
            }
            row.setQuantity(row.getQuantity() + qty);
            row.setGrossSaleValue(row.getGrossSaleValue().add(gross));
            row.setMarginValue(row.getMarginValue().add(margin));
            row.setDiscountValue(row.getDiscountValue().add(discount));
            row.setNetSaleValue(row.getNetSaleValue().add(net));
            quantity += qty;
            grossSaleValue = grossSaleValue.add(gross);
            marginValue = marginValue.add(margin);
            discountValue = discountValue.add(discount);
            netSaleValue = netSaleValue.add(net);
        }
        List<PharmacyRow> grouped = new ArrayList<>(itemRowMap.values());
        grouped.sort(Comparator.comparing(r -> r.getItem().getName(), Comparator.nullsLast(String::compareToIgnoreCase)));
        setRows(grouped);
        summaryRow = new PharmacyRow();
        summaryRow.setGrossSaleValue(grossSaleValue);
        summaryRow.setMarginValue(marginValue);
        summaryRow.setDiscountValue(discountValue);
        summaryRow.setNetSaleValue(netSaleValue);
        summaryRow.setQuantity(quantity);
    }

    public void generatePaymentDetailsForBills() {
        for (PharmacyRow r : getRows()) {
            Bill b = r.getBill();
            populateRowFromBill(r, b);
        }
        populateSummaryRow();
    }

    public void generatePaymentDetailsGroupedByBillType() {
        Map<BillTypeAtomic, PharmacyRow> grouped = new LinkedHashMap<>();

        for (PharmacyRow r : getRows()) {
            Bill b = r.getBill();
            if (b == null || b.getBillTypeAtomic() == null) {
                continue;
            }

            // Standard processing from generatePaymentDetailsForBills()
            r.setGrossTotal(b.getTotal());
            r.setNetTotal(b.getNetTotal());
            r.setDiscount(b.getDiscount());
            r.setServiceCharge(b.getMargin());
            r.setActualTotal(b.getTotal() - b.getServiceCharge());

            if (b.getPaymentMethod() == null) {
                r.setCreditValue(b.getNetTotal());
                if (b.getPatientEncounter() != null) {
                    r.setOpdCreditValue(0);
                    r.setInpatientCreditValue(b.getNetTotal());
                } else {
                    r.setOpdCreditValue(0);
                    r.setInpatientCreditValue(0);
                    r.setNoneValue(b.getNetTotal());
                }
            } else {
                switch (b.getPaymentMethod()) {
                    case Agent:
                        r.setAgentValue(b.getNetTotal());
                        break;
                    case Card:
                        r.setCardValue(b.getNetTotal());
                        break;
                    case Cash:
                        r.setCashValue(b.getNetTotal());
                        break;
                    case Cheque:
                        r.setChequeValue(b.getNetTotal());
                        break;
                    case IOU:
                        r.setIouValue(b.getNetTotal());
                        break;
                    case None:
                        break;
                    case OnCall:
                        r.setOnCallValue(b.getNetTotal());
                        break;
                    case Credit:
                        r.setCreditValue(b.getNetTotal());
                        if (b.getPatientEncounter() != null) {
                            r.setOpdCreditValue(0);
                            r.setInpatientCreditValue(b.getNetTotal());
                        } else {
                            r.setOpdCreditValue(b.getNetTotal());
                            r.setInpatientCreditValue(0);
                        }
                        break;
                    case MultiplePaymentMethods:
                        calculateBillPaymentValuesFromPayments(r);
                        break;
                    case OnlineSettlement:
                        r.setOnlineSettlementValue(b.getNetTotal());
                        break;
                    case PatientDeposit:
                        r.setPatientDepositValue(b.getNetTotal());
                        break;
                    case PatientPoints:
                        r.setPatientPointsValue(b.getNetTotal());
                        break;
                    case Slip:
                        r.setSlipValue(b.getNetTotal());
                        break;
                    case Staff:
                        r.setStaffValue(b.getNetTotal());
                        break;
                    case Staff_Welfare:
                        r.setStaffWelfareValue(b.getNetTotal());
                        break;
                    case Voucher:
                        r.setVoucherValue(b.getNetTotal());
                        break;
                    case ewallet:
                        r.setEwalletValue(b.getNetTotal());
                        break;
                    case YouOweMe:
                        break;
                }
            }

            // Now group by BillTypeAtomic
            BillTypeAtomic bta = b.getBillTypeAtomic();
            PharmacyRow groupRow = grouped.computeIfAbsent(bta, k -> {
                PharmacyRow ir = new PharmacyRow();
                ir.setBillTypeAtomic(k);
                return ir;
            });

            groupRow.setNetTotal(groupRow.getNetTotal() + r.getNetTotal());
            groupRow.setGrossTotal(groupRow.getGrossTotal() + r.getGrossTotal());
            groupRow.setDiscount(groupRow.getDiscount() + r.getDiscount());
            groupRow.setServiceCharge(groupRow.getServiceCharge() + r.getServiceCharge());
            groupRow.setActualTotal(groupRow.getActualTotal() + r.getActualTotal());

            groupRow.setCashValue(groupRow.getCashValue() + r.getCashValue());
            groupRow.setCardValue(groupRow.getCardValue() + r.getCardValue());
            groupRow.setChequeValue(groupRow.getChequeValue() + r.getChequeValue());
            groupRow.setCreditValue(groupRow.getCreditValue() + r.getCreditValue());
            groupRow.setOpdCreditValue(groupRow.getOpdCreditValue() + r.getOpdCreditValue());
            groupRow.setInpatientCreditValue(groupRow.getInpatientCreditValue() + r.getInpatientCreditValue());
            groupRow.setNoneValue(groupRow.getNoneValue() + r.getNoneValue());

            groupRow.setAgentValue(groupRow.getAgentValue() + r.getAgentValue());
            groupRow.setIouValue(groupRow.getIouValue() + r.getIouValue());
            groupRow.setOnlineSettlementValue(groupRow.getOnlineSettlementValue() + r.getOnlineSettlementValue());
            groupRow.setPatientDepositValue(groupRow.getPatientDepositValue() + r.getPatientDepositValue());
            groupRow.setPatientPointsValue(groupRow.getPatientPointsValue() + r.getPatientPointsValue());
            groupRow.setSlipValue(groupRow.getSlipValue() + r.getSlipValue());
            groupRow.setStaffValue(groupRow.getStaffValue() + r.getStaffValue());
            groupRow.setStaffWelfareValue(groupRow.getStaffWelfareValue() + r.getStaffWelfareValue());
            groupRow.setVoucherValue(groupRow.getVoucherValue() + r.getVoucherValue());
            groupRow.setEwalletValue(groupRow.getEwalletValue() + r.getEwalletValue());
            groupRow.setOnCallValue(groupRow.getOnCallValue() + r.getOnCallValue());
        }

        // Replace with grouped rows
        getRows().clear();
        grouped.values().stream()
                .sorted(Comparator.comparing(PharmacyRow::getBillTypeAtomic, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEachOrdered(getRows()::add);
        populateSummaryRow();
    }

// Contribution by ChatGPT - adapted based on provided instructions
    public void generatePaymentDetailsGroupedDiscountSchemeAndAdmissionType() {
        Map<String, PharmacyRow> grouped = new LinkedHashMap<>();

        for (PharmacyRow r : getRows()) {
            Bill b = r.getBill();
            if (b == null) {
                continue;
            }

            populateRowFromBill(r, b);

            String groupKey;
            if (b.getPatientEncounter() != null) {
                // Inpatient Sale
                r.setAdmissionType(b.getPatientEncounter().getAdmissionType());
                if (b.getPatientEncounter().getAdmissionType() == null) {
                    r.setRowType("Inpatient Sale - No Admission Type");
                    groupKey = "Inpatient Sale - No Admission Type";
                } else {
                    r.setRowType("Inpatient Sale - " + b.getPatientEncounter().getAdmissionType().getName());
                    groupKey = "Inpatient Sale - " + b.getPatientEncounter().getAdmissionType().getName();
                }
            } else {
                // Outpatient Sale
                r.setPaymentScheme(b.getPaymentScheme());
                if (b.getPaymentScheme() == null) {
                    r.setRowType("Outpatient Sale - No Discount Scheme");
                    groupKey = "Outpatient Sale - No Discount Scheme";
                } else {
                    r.setRowType("Outpatient Sale - " + b.getPaymentScheme().getName());
                    groupKey = "Outpatient Sale - " + b.getPaymentScheme().getName();
                }
            }

            PharmacyRow groupRow = grouped.computeIfAbsent(groupKey, k -> {
                PharmacyRow ir = new PharmacyRow();
                ir.setRowType(k);
                return ir;
            });

            groupRow.setNetTotal(groupRow.getNetTotal() + r.getNetTotal());
            groupRow.setGrossTotal(groupRow.getGrossTotal() + r.getGrossTotal());
            groupRow.setDiscount(groupRow.getDiscount() + r.getDiscount());
            groupRow.setServiceCharge(groupRow.getServiceCharge() + r.getServiceCharge());
            groupRow.setActualTotal(groupRow.getActualTotal() + r.getActualTotal());

            groupRow.setCashValue(groupRow.getCashValue() + r.getCashValue());
            groupRow.setCardValue(groupRow.getCardValue() + r.getCardValue());
            groupRow.setChequeValue(groupRow.getChequeValue() + r.getChequeValue());
            groupRow.setCreditValue(groupRow.getCreditValue() + r.getCreditValue());
            groupRow.setOpdCreditValue(groupRow.getOpdCreditValue() + r.getOpdCreditValue());
            groupRow.setInpatientCreditValue(groupRow.getInpatientCreditValue() + r.getInpatientCreditValue());
            groupRow.setNoneValue(groupRow.getNoneValue() + r.getNoneValue());

            groupRow.setAgentValue(groupRow.getAgentValue() + r.getAgentValue());
            groupRow.setIouValue(groupRow.getIouValue() + r.getIouValue());
            groupRow.setOnlineSettlementValue(groupRow.getOnlineSettlementValue() + r.getOnlineSettlementValue());
            groupRow.setPatientDepositValue(groupRow.getPatientDepositValue() + r.getPatientDepositValue());
            groupRow.setPatientPointsValue(groupRow.getPatientPointsValue() + r.getPatientPointsValue());
            groupRow.setSlipValue(groupRow.getSlipValue() + r.getSlipValue());
            groupRow.setStaffValue(groupRow.getStaffValue() + r.getStaffValue());
            groupRow.setStaffWelfareValue(groupRow.getStaffWelfareValue() + r.getStaffWelfareValue());
            groupRow.setVoucherValue(groupRow.getVoucherValue() + r.getVoucherValue());
            groupRow.setEwalletValue(groupRow.getEwalletValue() + r.getEwalletValue());
            groupRow.setOnCallValue(groupRow.getOnCallValue() + r.getOnCallValue());
        }

        // Replace with grouped rows
        getRows().clear();
        grouped.values().stream()
                .sorted(Comparator.comparing(PharmacyRow::getRowType, Comparator.nullsLast(String::compareToIgnoreCase)))
                .forEachOrdered(getRows()::add);
        populateSummaryRow();
    }

    // Contribution by ChatGPT - combines grouping by BillTypeAtomic + (Admission Type / Discount Scheme)
    public void generatePaymentDetailsGroupedByBillTypeAndDiscountSchemeAndAdmissionType() {
        Map<String, PharmacyRow> grouped = new LinkedHashMap<>();

        for (PharmacyRow r : getRows()) {
            Bill b = r.getBill();
            if (b == null || b.getBillTypeAtomic() == null) {
                continue;
            }

            populateRowFromBill(r, b);

            BillTypeAtomic bta = b.getBillTypeAtomic();
            String detail;
            if (b.getPatientEncounter() != null) {
                r.setAdmissionType(b.getPatientEncounter().getAdmissionType());
                if (b.getPatientEncounter().getAdmissionType() == null) {
                    detail = "No Admission Type";
                } else {
                    detail = b.getPatientEncounter().getAdmissionType().getName();
                }
            } else {
                r.setPaymentScheme(b.getPaymentScheme());
                if (b.getPaymentScheme() == null) {
                    detail = "No Discount Scheme";
                } else {
                    detail = b.getPaymentScheme().getName();
                }
            }

            String groupKey = bta.name() + " - " + detail;
            r.setRowType(groupKey);  // Optional: if needed in JSF display

            PharmacyRow groupRow = grouped.computeIfAbsent(groupKey, k -> {
                PharmacyRow ir = new PharmacyRow();
                ir.setBillTypeAtomic(bta);
                ir.setRowType(k);
                return ir;
            });

            groupRow.setNetTotal(groupRow.getNetTotal() + r.getNetTotal());
            groupRow.setGrossTotal(groupRow.getGrossTotal() + r.getGrossTotal());
            groupRow.setDiscount(groupRow.getDiscount() + r.getDiscount());
            groupRow.setServiceCharge(groupRow.getServiceCharge() + r.getServiceCharge());
            groupRow.setActualTotal(groupRow.getActualTotal() + r.getActualTotal());

            groupRow.setCashValue(groupRow.getCashValue() + r.getCashValue());
            groupRow.setCardValue(groupRow.getCardValue() + r.getCardValue());
            groupRow.setChequeValue(groupRow.getChequeValue() + r.getChequeValue());
            groupRow.setCreditValue(groupRow.getCreditValue() + r.getCreditValue());
            groupRow.setOpdCreditValue(groupRow.getOpdCreditValue() + r.getOpdCreditValue());
            groupRow.setInpatientCreditValue(groupRow.getInpatientCreditValue() + r.getInpatientCreditValue());
            groupRow.setNoneValue(groupRow.getNoneValue() + r.getNoneValue());

            groupRow.setAgentValue(groupRow.getAgentValue() + r.getAgentValue());
            groupRow.setIouValue(groupRow.getIouValue() + r.getIouValue());
            groupRow.setOnlineSettlementValue(groupRow.getOnlineSettlementValue() + r.getOnlineSettlementValue());
            groupRow.setPatientDepositValue(groupRow.getPatientDepositValue() + r.getPatientDepositValue());
            groupRow.setPatientPointsValue(groupRow.getPatientPointsValue() + r.getPatientPointsValue());
            groupRow.setSlipValue(groupRow.getSlipValue() + r.getSlipValue());
            groupRow.setStaffValue(groupRow.getStaffValue() + r.getStaffValue());
            groupRow.setStaffWelfareValue(groupRow.getStaffWelfareValue() + r.getStaffWelfareValue());
            groupRow.setVoucherValue(groupRow.getVoucherValue() + r.getVoucherValue());
            groupRow.setEwalletValue(groupRow.getEwalletValue() + r.getEwalletValue());
            groupRow.setOnCallValue(groupRow.getOnCallValue() + r.getOnCallValue());
        }

        // Replace with grouped rows, sorted by combined key
        getRows().clear();
        grouped.values().stream()
                .sorted(Comparator.comparing(PharmacyRow::getRowType, Comparator.nullsLast(String::compareToIgnoreCase)))
                .forEachOrdered(getRows()::add);
        populateSummaryRow();
    }

    // Contribution by ChatGPT - combines grouping by BillTypeAtomic
    public void generatePharmacyPurchaseGroupedByBillType() {
        Map<String, PharmacyRow> grouped = new LinkedHashMap<>();

        for (PharmacyRow r : getRows()) {
            Bill b = r.getBill();
            if (b == null || b.getBillTypeAtomic() == null) {
                continue;
            }

            populateRowFromBill(r, b);

            BillTypeAtomic bta = b.getBillTypeAtomic();

            String groupKey = bta.name();
            r.setRowType(groupKey);  // Optional: if needed in JSF display

            PharmacyRow groupRow = grouped.computeIfAbsent(groupKey, k -> {
                PharmacyRow ir = new PharmacyRow();
                ir.setBillTypeAtomic(bta);
                ir.setRowType(k);
                return ir;
            });

            groupRow.setNetTotal(groupRow.getNetTotal() + r.getNetTotal());
            groupRow.setGrossTotal(groupRow.getGrossTotal() + r.getGrossTotal());
            groupRow.setDiscount(groupRow.getDiscount() + r.getDiscount());
            groupRow.setServiceCharge(groupRow.getServiceCharge() + r.getServiceCharge());
            groupRow.setActualTotal(groupRow.getActualTotal() + r.getActualTotal());

            // Aggregate stock values
            if (r.getValueOfStocksAtCostRate() != null) {
                groupRow.setValueOfStocksAtCostRate(
                    groupRow.getValueOfStocksAtCostRate().add(r.getValueOfStocksAtCostRate())
                );
            }
            if (r.getValueOfStocksAtPurchaseRate() != null) {
                groupRow.setValueOfStocksAtPurchaseRate(
                    groupRow.getValueOfStocksAtPurchaseRate().add(r.getValueOfStocksAtPurchaseRate())
                );
            }
            if (r.getValueOfStocksAtRetailSaleRate() != null) {
                groupRow.setValueOfStocksAtRetailSaleRate(
                    groupRow.getValueOfStocksAtRetailSaleRate().add(r.getValueOfStocksAtRetailSaleRate())
                );
            }

        }

        // Replace with grouped rows, sorted by combined key
        getRows().clear();
        grouped.values().stream()
                .sorted(Comparator.comparing(PharmacyRow::getRowType, Comparator.nullsLast(String::compareToIgnoreCase)))
                .forEachOrdered(getRows()::add);
        populateSummaryRow();
    }

    public void generatePharmacyPurchaseGroupedByBillTypeDtos() {
        Map<String, PharmacyRow> grouped = new LinkedHashMap<>();

        for (PharmacyRow r : getRows()) {
            BillLight b = r.getBillLight();
            if (b == null || b.getBillTypeAtomic() == null) {
                continue;
            }

            populateRowFromBill(r, b);

            BillTypeAtomic bta = b.getBillTypeAtomic();

            String groupKey = bta.name();
            r.setRowType(groupKey);  // Optional: if needed in JSF display

            PharmacyRow groupRow = grouped.computeIfAbsent(groupKey, k -> {
                PharmacyRow ir = new PharmacyRow();
                ir.setBillTypeAtomic(bta);
                ir.setRowType(k);
                return ir;
            });

            groupRow.setNetTotal(groupRow.getNetTotal() + r.getNetTotal());
            groupRow.setGrossTotal(groupRow.getGrossTotal() + r.getGrossTotal());
            groupRow.setDiscount(groupRow.getDiscount() + r.getDiscount());
            groupRow.setServiceCharge(groupRow.getServiceCharge() + r.getServiceCharge());
            groupRow.setActualTotal(groupRow.getActualTotal() + r.getActualTotal());

            // Aggregate stock values
            if (r.getValueOfStocksAtCostRate() != null) {
                groupRow.setValueOfStocksAtCostRate(
                    groupRow.getValueOfStocksAtCostRate().add(r.getValueOfStocksAtCostRate())
                );
            }
            if (r.getValueOfStocksAtPurchaseRate() != null) {
                groupRow.setValueOfStocksAtPurchaseRate(
                    groupRow.getValueOfStocksAtPurchaseRate().add(r.getValueOfStocksAtPurchaseRate())
                );
            }
            if (r.getValueOfStocksAtRetailSaleRate() != null) {
                groupRow.setValueOfStocksAtRetailSaleRate(
                    groupRow.getValueOfStocksAtRetailSaleRate().add(r.getValueOfStocksAtRetailSaleRate())
                );
            }

        }

        // Replace with grouped rows, sorted by combined key
        getRows().clear();
        grouped.values().stream()
                .sorted(Comparator.comparing(PharmacyRow::getRowType, Comparator.nullsLast(String::compareToIgnoreCase)))
                .forEachOrdered(getRows()::add);
        populateSummaryRow();
    }

    public void generatePaymentDetailsForBillsAndBatchBills() {
        for (PharmacyRow r : getRows()) {
            Bill b = r.getBill();
            Bill bb = r.getBatchBill();
            Bill rb = r.getReferanceBill();

            if (b == null) {
                continue;
            }

            r.setGrossTotal(b.getTotal());
            r.setNetTotal(b.getNetTotal());
            r.setDiscount(b.getDiscount());
            r.setServiceCharge(b.getMargin());
            r.setActualTotal(b.getTotal() - b.getServiceCharge());

            PaymentMethod pm = null;
            if (bb != null) {
                pm = bb.getPaymentMethod();
            } else {
                pm = b.getPaymentMethod();
            }

            if (pm == null) {
                r.setCreditValue(b.getNetTotal());
                if (r.getBill().getPatientEncounter() != null) {
                    r.setOpdCreditValue(0);
                    r.setInpatientCreditValue(b.getNetTotal());
                } else {
                    r.setOpdCreditValue(0);
                    r.setInpatientCreditValue(0);
                    r.setNoneValue(b.getNetTotal());
                }

            } else {
                switch (pm) {
                    case Agent:
                        r.setAgentValue(b.getNetTotal());
                        break;
                    case Card:
                        r.setCardValue(b.getNetTotal());
                        break;
                    case Cash:
                        r.setCashValue(b.getNetTotal());
                        break;
                    case Cheque:
                        r.setChequeValue(b.getNetTotal());
                        break;
                    case IOU:
                        r.setIouValue(b.getNetTotal());
                        break;
                    case None:
                        break;
                    case OnCall:
                        r.setOnCallValue(b.getNetTotal());
                        break;
                    case Credit:
                        r.setCreditValue(b.getNetTotal());
                        if (r.getBill().getPatientEncounter() != null) {
                            r.setOpdCreditValue(0);
                            r.setInpatientCreditValue(b.getNetTotal());
                        } else {
                            r.setOpdCreditValue(b.getNetTotal());
                            r.setInpatientCreditValue(0);
                        }
                        break;
                    case MultiplePaymentMethods:
                        calculateBillAndBatchBillPaymentValuesFromPayments(r);
                        break;
                    case OnlineSettlement:
                        r.setOnlineSettlementValue(b.getNetTotal());
                        break;
                    case PatientDeposit:
                        r.setPatientDepositValue(b.getNetTotal());
                        break;
                    case PatientPoints:
                        r.setPatientPointsValue(b.getNetTotal());
                        break;
                    case Slip:
                        r.setSlipValue(b.getNetTotal());
                        break;
                    case Staff:
                        r.setStaffValue(b.getNetTotal());
                        break;
                    case Staff_Welfare:
                        r.setStaffWelfareValue(b.getNetTotal());
                        break;
                    case Voucher:
                        r.setVoucherValue(b.getNetTotal());
                        break;
                    case ewallet:
                        r.setEwalletValue(b.getNetTotal());
                        break;
                    case YouOweMe:
                        break;
                }

            }

        }
        populateSummaryRow();
    }

    public void generatePaymentDetailsForBillsAndBatchBillsByDate() {
        if (getRows() == null || getRows().isEmpty()) {
            return;
        }

        // This map will accumulate totals keyed by the date portion of bill.getCreatedAt().
        Map<Date, PharmacyRow> dailySummaries = new HashMap<>();

        for (PharmacyRow originalRow : getRows()) {
            Bill bill = originalRow.getBill();
            if (bill == null) {
                continue;
            }

            // Extract only the date portion from bill.getCreatedAt().
            // Adjust as needed if you use LocalDate or a different date-handling approach.
            Date billDate = truncateToDateOnly(bill.getCreatedAt());

            // Fetch or create the summary row for this date.
            PharmacyRow dailyRow = dailySummaries.get(billDate);
            if (dailyRow == null) {
                dailyRow = new PharmacyRow();
                dailyRow.setDate(billDate);
                // If your PharmacyRow has a date property, set it here.
                // dailyRow.setSomeDateField(billDate);
                dailySummaries.put(billDate, dailyRow);
            }

            // Calculate main totals (gross, net, discount, etc.).
            dailyRow.setGrossTotal(dailyRow.getGrossTotal() + bill.getTotal());
            dailyRow.setNetTotal(dailyRow.getNetTotal() + bill.getNetTotal());
            dailyRow.setDiscount(dailyRow.getDiscount() + bill.getDiscount());
            dailyRow.setServiceCharge(dailyRow.getServiceCharge() + bill.getMargin());
            dailyRow.setActualTotal(dailyRow.getActualTotal() + (bill.getTotal() - bill.getServiceCharge()));

            // Determine the payment method to guide how values are added.
            PaymentMethod pm = originalRow.getBatchBill() != null
                    ? originalRow.getBatchBill().getPaymentMethod()
                    : bill.getPaymentMethod();

            if (pm == null) {
                // Credit-like scenario with no explicit payment method.
                dailyRow.setCreditValue(dailyRow.getCreditValue() + bill.getNetTotal());
                if (bill.getPatientEncounter() != null) {
                    dailyRow.setInpatientCreditValue(dailyRow.getInpatientCreditValue() + bill.getNetTotal());
                } else {
                    // Depending on your domain, decide how to handle OPD vs. None here.
                    dailyRow.setNoneValue(dailyRow.getNoneValue() + bill.getNetTotal());
                }
            } else {
                switch (pm) {
                    case Agent:
                        dailyRow.setAgentValue(dailyRow.getAgentValue() + bill.getNetTotal());
                        break;
                    case Card:
                        dailyRow.setCardValue(dailyRow.getCardValue() + bill.getNetTotal());
                        break;
                    case Cash:
                        dailyRow.setCashValue(dailyRow.getCashValue() + bill.getNetTotal());
                        break;
                    case Cheque:
                        dailyRow.setChequeValue(dailyRow.getChequeValue() + bill.getNetTotal());
                        break;
                    case IOU:
                        dailyRow.setIouValue(dailyRow.getIouValue() + bill.getNetTotal());
                        break;
                    case OnCall:
                        dailyRow.setOnCallValue(dailyRow.getOnCallValue() + bill.getNetTotal());
                        break;
                    case Credit:
                        dailyRow.setCreditValue(dailyRow.getCreditValue() + bill.getNetTotal());
                        if (bill.getPatientEncounter() != null) {
                            dailyRow.setInpatientCreditValue(dailyRow.getInpatientCreditValue() + bill.getNetTotal());
                        } else {
                            dailyRow.setOpdCreditValue(dailyRow.getOpdCreditValue() + bill.getNetTotal());
                        }
                        break;
                    case MultiplePaymentMethods:
                        // For multiple payment methods, handle ratio allocation:
                        // Summation by date happens inside the helper method.
                        calculateBillAndBatchBillPaymentValuesFromPaymentsByDate(originalRow, dailyRow);
                        break;
                    case OnlineSettlement:
                        dailyRow.setOnlineSettlementValue(dailyRow.getOnlineSettlementValue() + bill.getNetTotal());
                        break;
                    case PatientDeposit:
                        dailyRow.setPatientDepositValue(dailyRow.getPatientDepositValue() + bill.getNetTotal());
                        break;
                    case PatientPoints:
                        dailyRow.setPatientPointsValue(dailyRow.getPatientPointsValue() + bill.getNetTotal());
                        break;
                    case Slip:
                        dailyRow.setSlipValue(dailyRow.getSlipValue() + bill.getNetTotal());
                        break;
                    case Staff:
                        dailyRow.setStaffValue(dailyRow.getStaffValue() + bill.getNetTotal());
                        break;
                    case Staff_Welfare:
                        dailyRow.setStaffWelfareValue(dailyRow.getStaffWelfareValue() + bill.getNetTotal());
                        break;
                    case Voucher:
                        dailyRow.setVoucherValue(dailyRow.getVoucherValue() + bill.getNetTotal());
                        break;
                    case ewallet:
                        dailyRow.setEwalletValue(dailyRow.getEwalletValue() + bill.getNetTotal());
                        break;
                    default:
                        // YouOweMe, None, etc. fall back to None usage.
                        dailyRow.setNoneValue(dailyRow.getNoneValue() + bill.getNetTotal());
                }
            }
        }

        // Replace the original rows with our date-based summaries.
        List<PharmacyRow> summarizedRows = new ArrayList<>(dailySummaries.values());
        summarizedRows.sort(Comparator.comparing(PharmacyRow::getDate));

        setRows(summarizedRows);
        // If you need additional overall calculations across all dates:
        populateSummaryRow();
    }

    private void populateRowFromBill(PharmacyRow r, Bill b) {
        if (b == null) {
            return;
        }

        r.setGrossTotal(b.getTotal());
        r.setNetTotal(b.getNetTotal());
        r.setDiscount(b.getDiscount());
        r.setServiceCharge(b.getMargin());
        r.setActualTotal(b.getTotal() - b.getServiceCharge());

        PaymentMethod pm = b.getPaymentMethod();

        if (pm == null) {
            r.setCreditValue(b.getNetTotal());
            if (b.getPatientEncounter() != null) {
                r.setOpdCreditValue(0);
                r.setInpatientCreditValue(b.getNetTotal());
            } else {
                r.setOpdCreditValue(0);
                r.setInpatientCreditValue(0);
                r.setNoneValue(b.getNetTotal());
            }
            return;
        }

        switch (pm) {
            case Agent:
                r.setAgentValue(b.getNetTotal());
                break;
            case Card:
                r.setCardValue(b.getNetTotal());
                break;
            case Cash:
                r.setCashValue(b.getNetTotal());
                break;
            case Cheque:
                r.setChequeValue(b.getNetTotal());
                break;
            case IOU:
                r.setIouValue(b.getNetTotal());
                break;
            case OnCall:
                r.setOnCallValue(b.getNetTotal());
                break;
            case Credit:
                r.setCreditValue(b.getNetTotal());
                if (b.getPatientEncounter() != null) {
                    r.setOpdCreditValue(0);
                    r.setInpatientCreditValue(b.getNetTotal());
                } else {
                    r.setOpdCreditValue(b.getNetTotal());
                    r.setInpatientCreditValue(0);
                }
                break;
            case MultiplePaymentMethods:
                calculateBillPaymentValuesFromPayments(r);
                break;
            case OnlineSettlement:
                r.setOnlineSettlementValue(b.getNetTotal());
                break;
            case PatientDeposit:
                r.setPatientDepositValue(b.getNetTotal());
                break;
            case PatientPoints:
                r.setPatientPointsValue(b.getNetTotal());
                break;
            case Slip:
                r.setSlipValue(b.getNetTotal());
                break;
            case Staff:
                r.setStaffValue(b.getNetTotal());
                break;
            case Staff_Welfare:
                r.setStaffWelfareValue(b.getNetTotal());
                break;
            case Voucher:
                r.setVoucherValue(b.getNetTotal());
                break;
            case ewallet:
                r.setEwalletValue(b.getNetTotal());
                break;
            case YouOweMe:
                break;
            case None:
                break;
        }
    }

    private void populateRowFromBill(PharmacyRow r, BillLight b) {
        if (b == null) {
            return;
        }

        r.setGrossTotal(nullSafeDouble(b.getTotal()));
        r.setNetTotal(nullSafeDouble(b.getNetTotal()));
        r.setDiscount(nullSafeDouble(b.getDiscount()));
        r.setServiceCharge(nullSafeDouble(b.getServiceCharge()));
        r.setActualTotal(nullSafeDouble(b.getTotal()) - nullSafeDouble(b.getServiceCharge()));

        r.setValueOfStocksAtCostRate(b.getTotalCostValue() == null ? BigDecimal.ZERO : b.getTotalCostValue());
        r.setValueOfStocksAtPurchaseRate(b.getTotalPurchaseValue() == null ? BigDecimal.ZERO : b.getTotalPurchaseValue());
        r.setValueOfStocksAtRetailSaleRate(b.getTotalRetailSaleValue() == null ? BigDecimal.ZERO : b.getTotalRetailSaleValue());

        PaymentMethod pm = b.getPaymentMethod();

        if (pm == null) {
            r.setCreditValue(nullSafeDouble(b.getNetTotal()));
            if (b.getPatientEncounter() != null) {
                r.setOpdCreditValue(0);
                r.setInpatientCreditValue(nullSafeDouble(b.getNetTotal()));
            } else {
                r.setOpdCreditValue(0);
                r.setInpatientCreditValue(0);
                r.setNoneValue(nullSafeDouble(b.getNetTotal()));
            }
            return;
        }

        switch (pm) {
            case Agent:
                r.setAgentValue(nullSafeDouble(b.getNetTotal()));
                break;
            case Card:
                r.setCardValue(nullSafeDouble(b.getNetTotal()));
                break;
            case Cash:
                r.setCashValue(nullSafeDouble(b.getNetTotal()));
                break;
            case Cheque:
                r.setChequeValue(nullSafeDouble(b.getNetTotal()));
                break;
            case IOU:
                r.setIouValue(nullSafeDouble(b.getNetTotal()));
                break;
            case OnCall:
                r.setOnCallValue(nullSafeDouble(b.getNetTotal()));
                break;
            case Credit:
                r.setCreditValue(nullSafeDouble(b.getNetTotal()));
                if (b.getPatientEncounter() != null) {
                    r.setOpdCreditValue(0);
                    r.setInpatientCreditValue(nullSafeDouble(b.getNetTotal()));
                } else {
                    r.setOpdCreditValue(nullSafeDouble(b.getNetTotal()));
                    r.setInpatientCreditValue(0);
                }
                break;
            case MultiplePaymentMethods:
                // Note: BillLight does not have payment details, so this will be skipped
                // The calculateBillPaymentValuesFromPayments method checks for r.getBill() != null
                calculateBillPaymentValuesFromPayments(r);
                break;
            case OnlineSettlement:
                r.setOnlineSettlementValue(nullSafeDouble(b.getNetTotal()));
                break;
            case PatientDeposit:
                r.setPatientDepositValue(nullSafeDouble(b.getNetTotal()));
                break;
            case PatientPoints:
                r.setPatientPointsValue(nullSafeDouble(b.getNetTotal()));
                break;
            case Slip:
                r.setSlipValue(nullSafeDouble(b.getNetTotal()));
                break;
            case Staff:
                r.setStaffValue(nullSafeDouble(b.getNetTotal()));
                break;
            case Staff_Welfare:
                r.setStaffWelfareValue(nullSafeDouble(b.getNetTotal()));
                break;
            case Voucher:
                r.setVoucherValue(nullSafeDouble(b.getNetTotal()));
                break;
            case ewallet:
                r.setEwalletValue(nullSafeDouble(b.getNetTotal()));
                break;
            case YouOweMe:
                break;
            case None:
                break;
        }
    }

    /**
     * Helper method to allocate multiple-payment totals into the daily summary
     * row.
     */
    private void calculateBillAndBatchBillPaymentValuesFromPaymentsByDate(PharmacyRow originalRow, PharmacyRow dailyRow) {
        Bill batchBill = originalRow.getBatchBill();
        Bill individualBill = originalRow.getBill();
        if (individualBill == null
                || individualBill.getPaymentMethod() == null
                || individualBill.getPaymentMethod() != PaymentMethod.MultiplePaymentMethods) {
            return;
        }

        // Ratio for distributing from the batch bill to the individual bill.
        double netTotalOfBatchBill = batchBill != null ? batchBill.getNetTotal() : 0.0;
        double netTotalOfIndividualBill = individualBill.getNetTotal();
        double ratio = (batchBill != null && netTotalOfBatchBill != 0.0)
                ? (netTotalOfIndividualBill / netTotalOfBatchBill)
                : 1.0;

        List<Payment> payments = originalRow.getPayments();
        if (payments == null || payments.isEmpty()) {
            return;
        }

        for (Payment p : payments) {
            double allocatedValue = p.getPaidValue() * ratio;
            PaymentMethod pm = p.getPaymentMethod();

            if (pm == null) {
                dailyRow.setNoneValue(dailyRow.getNoneValue() + allocatedValue);
            } else {
                switch (pm) {
                    case Agent:
                        dailyRow.setAgentValue(dailyRow.getAgentValue() + allocatedValue);
                        break;
                    case Card:
                        dailyRow.setCardValue(dailyRow.getCardValue() + allocatedValue);
                        break;
                    case Cash:
                        dailyRow.setCashValue(dailyRow.getCashValue() + allocatedValue);
                        break;
                    case Cheque:
                        dailyRow.setChequeValue(dailyRow.getChequeValue() + allocatedValue);
                        break;
                    case Credit:
                        dailyRow.setCreditValue(dailyRow.getCreditValue() + allocatedValue);
                        if (individualBill.getPatientEncounter() != null) {
                            dailyRow.setInpatientCreditValue(dailyRow.getInpatientCreditValue() + allocatedValue);
                        } else {
                            dailyRow.setOpdCreditValue(dailyRow.getOpdCreditValue() + allocatedValue);
                        }
                        break;
                    case IOU:
                        dailyRow.setIouValue(dailyRow.getIouValue() + allocatedValue);
                        break;
                    case OnCall:
                        dailyRow.setOnCallValue(dailyRow.getOnCallValue() + allocatedValue);
                        break;
                    case OnlineSettlement:
                        dailyRow.setOnlineSettlementValue(dailyRow.getOnlineSettlementValue() + allocatedValue);
                        break;
                    case PatientDeposit:
                        dailyRow.setPatientDepositValue(dailyRow.getPatientDepositValue() + allocatedValue);
                        break;
                    case PatientPoints:
                        dailyRow.setPatientPointsValue(dailyRow.getPatientPointsValue() + allocatedValue);
                        break;
                    case Slip:
                        dailyRow.setSlipValue(dailyRow.getSlipValue() + allocatedValue);
                        break;
                    case Staff:
                        dailyRow.setStaffValue(dailyRow.getStaffValue() + allocatedValue);
                        break;
                    case Staff_Welfare:
                        dailyRow.setStaffWelfareValue(dailyRow.getStaffWelfareValue() + allocatedValue);
                        break;
                    case Voucher:
                        dailyRow.setVoucherValue(dailyRow.getVoucherValue() + allocatedValue);
                        break;
                    case ewallet:
                        dailyRow.setEwalletValue(dailyRow.getEwalletValue() + allocatedValue);
                        break;
                    default:
                        // YouOweMe, None, MultiplePaymentMethods (again), etc.
                        dailyRow.setNoneValue(dailyRow.getNoneValue() + allocatedValue);
                }
            }
        }
    }

    /**
     * Example utility to truncate a Date to just its date part. Adjust the
     * implementation if you use LocalDate or other date classes.
     */
    private Date truncateToDateOnly(Date date) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private void calculateBillPaymentValuesFromPayments(PharmacyRow r) {
        if (r == null || r.getBill() == null || r.getBill().getPaymentMethod() == null
                || r.getBill().getPaymentMethod() != PaymentMethod.MultiplePaymentMethods) {
            return;
        }

        List<Payment> payments = r.getPayments();
        if (payments == null || payments.isEmpty()) {
            return;
        }

        for (Payment p : payments) {
            if (p.getPaymentMethod() == null) {
                r.setNoneValue(r.getNoneValue() + p.getPaidValue());
            } else {
                switch (p.getPaymentMethod()) {
                    case Agent:
                        r.setAgentValue(r.getAgentValue() + p.getPaidValue());
                        break;
                    case Card:
                        r.setCardValue(r.getCardValue() + p.getPaidValue());
                        break;
                    case Cash:
                        r.setCashValue(r.getCashValue() + p.getPaidValue());
                        break;
                    case Cheque:
                        r.setChequeValue(r.getChequeValue() + p.getPaidValue());
                        break;
                    case Credit:
                        r.setCreditValue(r.getCreditValue() + p.getPaidValue());
                        if (r.getBill().getPatientEncounter() != null) {
                            r.setInpatientCreditValue(r.getInpatientCreditValue() + p.getPaidValue());
                        } else {
                            r.setOpdCreditValue(r.getOpdCreditValue() + p.getPaidValue());
                        }
                        break;
                    case IOU:
                        r.setIouValue(r.getIouValue() + p.getPaidValue());
                        break;
                    case OnCall:
                        r.setOnCallValue(r.getOnCallValue() + p.getPaidValue());
                        break;
                    case OnlineSettlement:
                        r.setOnlineSettlementValue(r.getOnlineSettlementValue() + p.getPaidValue());
                        break;
                    case PatientDeposit:
                        r.setPatientDepositValue(r.getPatientDepositValue() + p.getPaidValue());
                        break;
                    case PatientPoints:
                        r.setPatientPointsValue(r.getPatientPointsValue() + p.getPaidValue());
                        break;
                    case Slip:
                        r.setSlipValue(r.getSlipValue() + p.getPaidValue());
                        break;
                    case Staff:
                        r.setStaffValue(r.getStaffValue() + p.getPaidValue());
                        break;
                    case Staff_Welfare:
                        r.setStaffWelfareValue(r.getStaffWelfareValue() + p.getPaidValue());
                        break;
                    case Voucher:
                        r.setVoucherValue(r.getVoucherValue() + p.getPaidValue());
                        break;
                    case ewallet:
                        r.setEwalletValue(r.getEwalletValue() + p.getPaidValue());
                        break;
                    case YouOweMe:
                        break;
                    case None:
                    case MultiplePaymentMethods:
                        break;
                    default:
                        r.setNoneValue(r.getNoneValue() + p.getPaidValue());
                }
            }
        }
    }

    private void calculateBillAndBatchBillPaymentValuesFromPayments(PharmacyRow r) {
        // Validate the row and Bill.
        if (r == null || r.getBill() == null || r.getBill().getPaymentMethod() == null
                || r.getBill().getPaymentMethod() != PaymentMethod.MultiplePaymentMethods) {
            return;
        }

        // Identify the batch bill and the individual bill.
        Bill batchBill = r.getBatchBill();
        Bill individualBill = r.getBill();

        // Net totals for ratio calculation (if needed).
        double netTotalOfBatchBill = batchBill != null ? batchBill.getNetTotal() : 0.0;
        double netTotalOfIndividualBill = individualBill.getNetTotal();

        // Determine the ratio for allocating batch-bill payments to the individual bill.
        // If there's no batch bill or the batch bill total is zero, we use a ratio of 1.0 (i.e., full amount).
        double ratio = 1.0;
        if (batchBill != null && netTotalOfBatchBill != 0.0) {
            ratio = netTotalOfIndividualBill / netTotalOfBatchBill;
        }

        // Retrieve the associated payments.
        List<Payment> payments = r.getPayments();
        if (payments == null || payments.isEmpty()) {
            return;
        }

        // Process each payment, allocating its value according to the ratio if a batch bill is present.
        for (Payment p : payments) {
            double allocatedValue = p.getPaidValue() * ratio;

            if (p.getPaymentMethod() == null) {
                r.setNoneValue(r.getNoneValue() + allocatedValue);
            } else {
                switch (p.getPaymentMethod()) {
                    case Agent:
                        r.setAgentValue(r.getAgentValue() + allocatedValue);
                        break;
                    case Card:
                        r.setCardValue(r.getCardValue() + allocatedValue);
                        break;
                    case Cash:
                        r.setCashValue(r.getCashValue() + allocatedValue);
                        break;
                    case Cheque:
                        r.setChequeValue(r.getChequeValue() + allocatedValue);
                        break;
                    case Credit:
                        r.setCreditValue(r.getCreditValue() + allocatedValue);
                        if (individualBill.getPatientEncounter() != null) {
                            r.setInpatientCreditValue(r.getInpatientCreditValue() + allocatedValue);
                        } else {
                            r.setOpdCreditValue(r.getOpdCreditValue() + allocatedValue);
                        }
                        break;
                    case IOU:
                        r.setIouValue(r.getIouValue() + allocatedValue);
                        break;
                    case OnCall:
                        r.setOnCallValue(r.getOnCallValue() + allocatedValue);
                        break;
                    case OnlineSettlement:
                        r.setOnlineSettlementValue(r.getOnlineSettlementValue() + allocatedValue);
                        break;
                    case PatientDeposit:
                        r.setPatientDepositValue(r.getPatientDepositValue() + allocatedValue);
                        break;
                    case PatientPoints:
                        r.setPatientPointsValue(r.getPatientPointsValue() + allocatedValue);
                        break;
                    case Slip:
                        r.setSlipValue(r.getSlipValue() + allocatedValue);
                        break;
                    case Staff:
                        r.setStaffValue(r.getStaffValue() + allocatedValue);
                        break;
                    case Staff_Welfare:
                        r.setStaffWelfareValue(r.getStaffWelfareValue() + allocatedValue);
                        break;
                    case Voucher:
                        r.setVoucherValue(r.getVoucherValue() + allocatedValue);
                        break;
                    case ewallet:
                        r.setEwalletValue(r.getEwalletValue() + allocatedValue);
                        break;
                    case YouOweMe:
                    case None:
                    case MultiplePaymentMethods:
                        // No special action needed here.
                        break;
                    default:
                        r.setNoneValue(r.getNoneValue() + allocatedValue);
                }
            }
        }
    }

    private double nullSafeDouble(Double value) {
        return value != null ? value : 0.0;
    }

    private void resetTotals() {
        onCallValue = 0.0;
        cashValue = 0.0;
        cardValue = 0.0;
        multiplePaymentMethodsValue = 0.0;
        staffValue = 0.0;
        creditValue = 0.0;
        staffWelfareValue = 0.0;
        voucherValue = 0.0;
        iouValue = 0.0;
        agentValue = 0.0;
        chequeValue = 0.0;
        slipValue = 0.0;
        eWalletValue = 0.0;
        patientDepositValue = 0.0;
        patientPointsValue = 0.0;
        onlineSettlementValue = 0.0;

        hasOnCallTransaction = false;
        hasCashTransaction = false;
        hasCardTransaction = false;
        hasMultiplePaymentMethodsTransaction = false;
        hasStaffTransaction = false;
        hasCreditTransaction = false;
        hasStaffWelfareTransaction = false;
        hasVoucherTransaction = false;
        hasIouTransaction = false;
        hasAgentTransaction = false;
        hasChequeTransaction = false;
        hasSlipTransaction = false;
        hasEWalletTransaction = false;
        hasPatientDepositTransaction = false;
        hasPatientPointsTransaction = false;
        hasOnlineSettlementTransaction = false;

        saleValue = 0.0;
        purchaseValue = 0.0;
        grossProfitValue = 0.0;
    }

    public void collectDepartments() {
        Set<Department> uniqueDepartments = new HashSet<>();
        if (bundles != null) {
            for (PharmacyBundle b : bundles) {
                if (b.getDepartment() != null) {
                    uniqueDepartments.add(b.getDepartment());
                }
            }
        }
        departments = new ArrayList<>(uniqueDepartments);
    }

    public void aggregateTotalsFromAllChildBundles() {
        resetTotals(); // Resets all totals before computation

        if (bundles != null) {
            for (PharmacyBundle childBundle : bundles) {

                // Payment values
                onCallValue += childBundle.onCallValue;
                cashValue += childBundle.cashValue;
                cardValue += childBundle.cardValue;
                multiplePaymentMethodsValue += childBundle.multiplePaymentMethodsValue;
                staffValue += childBundle.staffValue;
                creditValue += childBundle.creditValue;
                staffWelfareValue += childBundle.staffWelfareValue;
                voucherValue += childBundle.voucherValue;
                iouValue += childBundle.iouValue;
                agentValue += childBundle.agentValue;
                chequeValue += childBundle.chequeValue;
                slipValue += childBundle.slipValue;
                eWalletValue += childBundle.eWalletValue;
                patientDepositValue += childBundle.patientDepositValue;
                patientPointsValue += childBundle.patientPointsValue;
                onlineSettlementValue += childBundle.onlineSettlementValue;

                // Aggregate flags
                hasOnCallTransaction |= childBundle.hasOnCallTransaction;
                hasCashTransaction |= childBundle.hasCashTransaction;
                hasCardTransaction |= childBundle.hasCardTransaction;
                hasMultiplePaymentMethodsTransaction |= childBundle.hasMultiplePaymentMethodsTransaction;
                hasStaffTransaction |= childBundle.hasStaffTransaction;
                hasCreditTransaction |= childBundle.hasCreditTransaction;
                hasStaffWelfareTransaction |= childBundle.hasStaffWelfareTransaction;
                hasVoucherTransaction |= childBundle.hasVoucherTransaction;
                hasIouTransaction |= childBundle.hasIouTransaction;
                hasAgentTransaction |= childBundle.hasAgentTransaction;
                hasChequeTransaction |= childBundle.hasChequeTransaction;
                hasSlipTransaction |= childBundle.hasSlipTransaction;
                hasEWalletTransaction |= childBundle.hasEWalletTransaction;
                hasPatientDepositTransaction |= childBundle.hasPatientDepositTransaction;
                hasPatientPointsTransaction |= childBundle.hasPatientPointsTransaction;
                hasOnlineSettlementTransaction |= childBundle.hasOnlineSettlementTransaction;

                saleValue += childBundle.saleValue;
                purchaseValue += childBundle.purchaseValue;
                grossProfitValue += childBundle.grossProfitValue;
            }
        }
    }

    public UUID getId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PharmacyBundle that = (PharmacyBundle) o;
        return Objects.equals(getId(), that.id);
    }

    // Override hashCode() using UUID field
    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    // Override toString() for better readability (optional)
    @Override
    public String toString() {
        return "ReportTemplateRowBundle{id=" + getId() + '}';
    }

    public List<com.divudi.core.entity.cashTransaction.Denomination> getDenominations() {
        return denominations;
    }

    public void setDenominations(List<com.divudi.core.entity.cashTransaction.Denomination> denominations) {
        this.denominations = denominations;
    }

    public List<PharmacyBundle> getBundles() {
        return bundles;
    }

    public void setBundles(List<PharmacyBundle> bundles) {
        this.bundles = bundles;
    }

    public ReportTemplate getReportTemplate() {
        return reportTemplate;
    }

    public void setReportTemplate(ReportTemplate reportTemplate) {
        this.reportTemplate = reportTemplate;
    }

    public List<PharmacyRow> getRows() {
        if (rows == null) {
            rows = new ArrayList<>();
        }
        return rows;
    }

    public void setRows(List<PharmacyRow> rows) {
        this.rows = rows;
    }

    public Map<String, List<BillItem>> getGroupedBillItems() {
        return groupedBillItems;
    }

    public void setGroupedBillItems(Map<String, List<BillItem>> groupedBillItems) {
        this.groupedBillItems = groupedBillItems;
    }

    public Map<Institution, List<Bill>> getGroupedBillItemsByInstitution() {
        return groupedBillItemsByInstitution;
    }

    public void setGroupedBillItemsByInstitution(Map<Institution, List<Bill>> groupedBillItemsByInstitution) {
        this.groupedBillItemsByInstitution = groupedBillItemsByInstitution;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBundleType() {
        return bundleType;
    }

    public void setBundleType(String bundleType) {
        this.bundleType = bundleType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SessionInstance getSessionInstance() {
        return sessionInstance;
    }

    public void setSessionInstance(SessionInstance sessionInstance) {
        this.sessionInstance = sessionInstance;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getOnCallValue() {
        return onCallValue;
    }

    public void setOnCallValue(double onCallValue) {
        this.onCallValue = onCallValue;
    }

    public double getCashValue() {
        return cashValue;
    }

    public void setCashValue(double cashValue) {
        this.cashValue = cashValue;
    }

    public double getCardValue() {
        return cardValue;
    }

    public void setCardValue(double cardValue) {
        this.cardValue = cardValue;
    }

    public double getMultiplePaymentMethodsValue() {
        return multiplePaymentMethodsValue;
    }

    public void setMultiplePaymentMethodsValue(double multiplePaymentMethodsValue) {
        this.multiplePaymentMethodsValue = multiplePaymentMethodsValue;
    }

    public double getStaffValue() {
        return staffValue;
    }

    public void setStaffValue(double staffValue) {
        this.staffValue = staffValue;
    }

    public double getCreditValue() {
        return creditValue;
    }

    public void setCreditValue(double creditValue) {
        this.creditValue = creditValue;
    }

    public double getStaffWelfareValue() {
        return staffWelfareValue;
    }

    public void setStaffWelfareValue(double staffWelfareValue) {
        this.staffWelfareValue = staffWelfareValue;
    }

    public double getVoucherValue() {
        return voucherValue;
    }

    public void setVoucherValue(double voucherValue) {
        this.voucherValue = voucherValue;
    }

    public double getIouValue() {
        return iouValue;
    }

    public void setIouValue(double iouValue) {
        this.iouValue = iouValue;
    }

    public double getAgentValue() {
        return agentValue;
    }

    public void setAgentValue(double agentValue) {
        this.agentValue = agentValue;
    }

    public double getChequeValue() {
        return chequeValue;
    }

    public void setChequeValue(double chequeValue) {
        this.chequeValue = chequeValue;
    }

    public double getSlipValue() {
        return slipValue;
    }

    public void setSlipValue(double slipValue) {
        this.slipValue = slipValue;
    }

    public double geteWalletValue() {
        return eWalletValue;
    }

    public void seteWalletValue(double eWalletValue) {
        this.eWalletValue = eWalletValue;
    }

    public double getPatientDepositValue() {
        return patientDepositValue;
    }

    public void setPatientDepositValue(double patientDepositValue) {
        this.patientDepositValue = patientDepositValue;
    }

    public double getPatientPointsValue() {
        return patientPointsValue;
    }

    public void setPatientPointsValue(double patientPointsValue) {
        this.patientPointsValue = patientPointsValue;
    }

    public double getOnlineSettlementValue() {
        return onlineSettlementValue;
    }

    public void setOnlineSettlementValue(double onlineSettlementValue) {
        this.onlineSettlementValue = onlineSettlementValue;
    }

    public boolean isHasOnCallTransaction() {
        return hasOnCallTransaction;
    }

    public void setHasOnCallTransaction(boolean hasOnCallTransaction) {
        this.hasOnCallTransaction = hasOnCallTransaction;
    }

    public boolean isHasCashTransaction() {
        return hasCashTransaction;
    }

    public void setHasCashTransaction(boolean hasCashTransaction) {
        this.hasCashTransaction = hasCashTransaction;
    }

    public boolean isHasCardTransaction() {
        return hasCardTransaction;
    }

    public void setHasCardTransaction(boolean hasCardTransaction) {
        this.hasCardTransaction = hasCardTransaction;
    }

    public boolean isHasMultiplePaymentMethodsTransaction() {
        return hasMultiplePaymentMethodsTransaction;
    }

    public void setHasMultiplePaymentMethodsTransaction(boolean hasMultiplePaymentMethodsTransaction) {
        this.hasMultiplePaymentMethodsTransaction = hasMultiplePaymentMethodsTransaction;
    }

    public boolean isHasStaffTransaction() {
        return hasStaffTransaction;
    }

    public void setHasStaffTransaction(boolean hasStaffTransaction) {
        this.hasStaffTransaction = hasStaffTransaction;
    }

    public boolean isHasCreditTransaction() {
        return hasCreditTransaction;
    }

    public void setHasCreditTransaction(boolean hasCreditTransaction) {
        this.hasCreditTransaction = hasCreditTransaction;
    }

    public boolean isHasStaffWelfareTransaction() {
        return hasStaffWelfareTransaction;
    }

    public void setHasStaffWelfareTransaction(boolean hasStaffWelfareTransaction) {
        this.hasStaffWelfareTransaction = hasStaffWelfareTransaction;
    }

    public boolean isHasVoucherTransaction() {
        return hasVoucherTransaction;
    }

    public void setHasVoucherTransaction(boolean hasVoucherTransaction) {
        this.hasVoucherTransaction = hasVoucherTransaction;
    }

    public boolean isHasIouTransaction() {
        return hasIouTransaction;
    }

    public void setHasIouTransaction(boolean hasIouTransaction) {
        this.hasIouTransaction = hasIouTransaction;
    }

    public boolean isHasAgentTransaction() {
        return hasAgentTransaction;
    }

    public void setHasAgentTransaction(boolean hasAgentTransaction) {
        this.hasAgentTransaction = hasAgentTransaction;
    }

    public boolean isHasChequeTransaction() {
        return hasChequeTransaction;
    }

    public void setHasChequeTransaction(boolean hasChequeTransaction) {
        this.hasChequeTransaction = hasChequeTransaction;
    }

    public boolean isHasSlipTransaction() {
        return hasSlipTransaction;
    }

    public void setHasSlipTransaction(boolean hasSlipTransaction) {
        this.hasSlipTransaction = hasSlipTransaction;
    }

    public boolean isHasEWalletTransaction() {
        return hasEWalletTransaction;
    }

    public void setHasEWalletTransaction(boolean hasEWalletTransaction) {
        this.hasEWalletTransaction = hasEWalletTransaction;
    }

    public boolean isHasPatientDepositTransaction() {
        return hasPatientDepositTransaction;
    }

    public void setHasPatientDepositTransaction(boolean hasPatientDepositTransaction) {
        this.hasPatientDepositTransaction = hasPatientDepositTransaction;
    }

    public boolean isHasPatientPointsTransaction() {
        return hasPatientPointsTransaction;
    }

    public void setHasPatientPointsTransaction(boolean hasPatientPointsTransaction) {
        this.hasPatientPointsTransaction = hasPatientPointsTransaction;
    }

    public boolean isHasOnlineSettlementTransaction() {
        return hasOnlineSettlementTransaction;
    }

    public void setHasOnlineSettlementTransaction(boolean hasOnlineSettlementTransaction) {
        this.hasOnlineSettlementTransaction = hasOnlineSettlementTransaction;
    }

    public WebUser getUser() {
        return user;
    }

    public void setUser(WebUser user) {
        this.user = user;
    }

    public WebUser getFromUser() {
        return fromUser;
    }

    public void setFromUser(WebUser fromUser) {
        this.fromUser = fromUser;
    }

    public WebUser getToUser() {
        return toUser;
    }

    public void setToUser(WebUser toUser) {
        this.toUser = toUser;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

    public Bill getStartBill() {
        return startBill;
    }

    public void setStartBill(Bill startBill) {
        this.startBill = startBill;
    }

    public Bill getEndBill() {
        return endBill;
    }

    public void setEndBill(Bill endBill) {
        this.endBill = endBill;
    }

    public PaymentHandover getPaymentHandover() {
        return paymentHandover;
    }

    public void setPaymentHandover(PaymentHandover paymentHandover) {
        this.paymentHandover = paymentHandover;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isPatientDepositsAreConsideredInHandingover() {
        return patientDepositsAreConsideredInHandingover;
    }

    public void setPatientDepositsAreConsideredInHandingover(boolean patientDepositsAreConsideredInHandingover) {
        this.patientDepositsAreConsideredInHandingover = patientDepositsAreConsideredInHandingover;
    }

    public PharmacyRow getSummaryRow() {
        if (summaryRow == null) {
            summaryRow = new PharmacyRow();
        }
        return summaryRow;
    }

    public void setSummaryRow(PharmacyRow summaryRow) {
        this.summaryRow = summaryRow;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getFreeQuantity() {
        return freeQuantity;
    }

    public void setFreeQuantity(Double freeQuantity) {
        this.freeQuantity = freeQuantity;
    }

    public Double getQuantityPlusFreeQuantity() {
        return quantityPlusFreeQuantity;
    }

    public void setQuantityPlusFreeQuantity(Double quantityPlusFreeQuantity) {
        this.quantityPlusFreeQuantity = quantityPlusFreeQuantity;
    }

    public Double getQuantityValueAtPurchaseRate() {
        return quantityValueAtPurchaseRate;
    }

    public void setQuantityValueAtPurchaseRate(Double quantityValueAtPurchaseRate) {
        this.quantityValueAtPurchaseRate = quantityValueAtPurchaseRate;
    }

    public Double getFreeQuantityValueAtPurchaseRate() {
        return freeQuantityValueAtPurchaseRate;
    }

    public void setFreeQuantityValueAtPurchaseRate(Double freeQuantityValueAtPurchaseRate) {
        this.freeQuantityValueAtPurchaseRate = freeQuantityValueAtPurchaseRate;
    }

    public Double getQuantityPlusFreeQuantityValueAtPurchaseRate() {
        return quantityPlusFreeQuantityValueAtPurchaseRate;
    }

    public void setQuantityPlusFreeQuantityValueAtPurchaseRate(Double quantityPlusFreeQuantityValueAtPurchaseRate) {
        this.quantityPlusFreeQuantityValueAtPurchaseRate = quantityPlusFreeQuantityValueAtPurchaseRate;
    }

    public Double getQuantityValueAtRetailSaleRate() {
        return quantityValueAtRetailSaleRate;
    }

    public void setQuantityValueAtRetailSaleRate(Double quantityValueAtRetailSaleRate) {
        this.quantityValueAtRetailSaleRate = quantityValueAtRetailSaleRate;
    }

    public Double getFreeQuantityValueAtRetailSaleRate() {
        return freeQuantityValueAtRetailSaleRate;
    }

    public void setFreeQuantityValueAtRetailSaleRate(Double freeQuantityValueAtRetailSaleRate) {
        this.freeQuantityValueAtRetailSaleRate = freeQuantityValueAtRetailSaleRate;
    }

    public Double getQuantityPlusFreeQuantityValueAtRetailSaleRate() {
        return quantityPlusFreeQuantityValueAtRetailSaleRate;
    }

    public void setQuantityPlusFreeQuantityValueAtRetailSaleRate(Double quantityPlusFreeQuantityValueAtRetailSaleRate) {
        this.quantityPlusFreeQuantityValueAtRetailSaleRate = quantityPlusFreeQuantityValueAtRetailSaleRate;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public double getPaidTotal() {
        return paidTotal;
    }

    public void setPaidTotal(double paidTotal) {
        this.paidTotal = paidTotal;
    }

    public double getSaleValue() {
        return saleValue;
    }

    public void setSaleValue(double saleValue) {
        this.saleValue = saleValue;
    }

    public double getPurchaseValue() {
        return purchaseValue;
    }

    public void setPurchaseValue(double purchaseValue) {
        this.purchaseValue = purchaseValue;
    }

    public double getGrossProfitValue() {
        return grossProfitValue;
    }

    public void setGrossProfitValue(double grossProfitValue) {
        this.grossProfitValue = grossProfitValue;
    }

    public BigDecimal getGrossSaleRate() {
        return grossSaleRate;
    }

    public void setGrossSaleRate(BigDecimal grossSaleRate) {
        this.grossSaleRate = grossSaleRate;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    public BigDecimal getMarginRate() {
        return marginRate;
    }

    public void setMarginRate(BigDecimal marginRate) {
        this.marginRate = marginRate;
    }

    public BigDecimal getNetSaleRate() {
        return netSaleRate;
    }

    public void setNetSaleRate(BigDecimal netSaleRate) {
        this.netSaleRate = netSaleRate;
    }

    public BigDecimal getGrossSaleValue() {
        return grossSaleValue;
    }

    public void setGrossSaleValue(BigDecimal grossSaleValue) {
        this.grossSaleValue = grossSaleValue;
    }

    public BigDecimal getMarginValue() {
        return marginValue;
    }

    public void setMarginValue(BigDecimal marginValue) {
        this.marginValue = marginValue;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getNetSaleValue() {
        return netSaleValue;
    }

    public void setNetSaleValue(BigDecimal netSaleValue) {
        this.netSaleValue = netSaleValue;
    }

}
