package com.divudi.bean.report;

import com.divudi.bean.common.ControllerWithReportFilters;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.ReportViewType;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Consolidated Income Report (issue #19613).
 *
 * Definitions agreed on the issue:
 * <ul>
 * <li>Billed income by bill date, net of cancellations and refunds: received
 * bills and their cancellation/refund bills are summed with their signs.</li>
 * <li>Services (OPD, Lab, ECG, X-Ray, Dressing, ...): OPD bill items grouped by
 * the department that performs the item.</li>
 * <li>Channelling: doctor fee and hospital fee separately; only the hospital
 * fee counts toward the grand total.</li>
 * <li>Wards: final bills of patients discharged in the period, split into
 * Professional Fee / Medicine &amp; Pharmacy / Other, less the bill discount.</li>
 * <li>Collecting Centres: bill value per centre.</li>
 * </ul>
 */
@Named
@SessionScoped
public class ConsolidatedIncomeReportController implements Serializable, ControllerWithReportFilters {

    private static final long serialVersionUID = 1L;

    private static final Set<InwardChargeType> PROFESSIONAL_CHARGE_TYPES = EnumSet.of(
            InwardChargeType.ProfessionalCharge,
            InwardChargeType.DoctorAndNurses,
            InwardChargeType.TechnicianAndParamedicalCharge);

    private static final Set<InwardChargeType> MEDICINE_CHARGE_TYPES = EnumSet.of(
            InwardChargeType.Medicine,
            InwardChargeType.MedicinesAndSurgicalSupplies,
            InwardChargeType.ExtraMedicine,
            InwardChargeType.TheaterConsumbale,
            InwardChargeType.Etu_Medicine,
            InwardChargeType.Pharmacy_Medicine,
            InwardChargeType.Inward_Medicine,
            InwardChargeType.Theatre_Medicine,
            InwardChargeType.Store_Medicine);

    @EJB
    private BillFacade billFacade;

    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Institution site;
    private Department department;
    private AdmissionType admissionType;
    private PaymentScheme paymentScheme;
    private ReportViewType reportViewType;

    private List<IncomeRow> serviceRows;
    private IncomeRow channelRow;
    private List<IncomeRow> wardRows;
    private List<IncomeRow> collectingCentreRows;

    private double serviceTotal;
    private double wardProfessionalTotal;
    private double wardMedicineTotal;
    private double wardOtherTotal;
    private double wardDiscountTotal;
    private double wardNetTotal;
    private double collectingCentreTotal;
    private double grandTotal;

    public String navigateToConsolidatedIncomeReport() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay();
        }
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        clearResults();
        return "/reports/financialReports/consolidated_income_report?faces-redirect=true";
    }

    public void processReport() {
        clearResults();
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select From and To dates");
            return;
        }
        if (fromDate.after(toDate)) {
            JsfUtil.addErrorMessage("From date must be before To date");
            return;
        }
        fillServiceIncome();
        fillChannelIncome();
        fillWardIncome();
        fillCollectingCentreIncome();
        grandTotal = serviceTotal + channelRow.getHospitalFee() + wardNetTotal + collectingCentreTotal;
    }

    private void clearResults() {
        serviceRows = null;
        channelRow = null;
        wardRows = null;
        collectingCentreRows = null;
        serviceTotal = 0;
        wardProfessionalTotal = 0;
        wardMedicineTotal = 0;
        wardOtherTotal = 0;
        wardDiscountTotal = 0;
        wardNetTotal = 0;
        collectingCentreTotal = 0;
        grandTotal = 0;
    }

    private void fillServiceIncome() {
        Map<String, Object> params = new HashMap<>();
        String jpql = "select d, sum(bi.netValue)"
                + " from BillItem bi join bi.bill b left join bi.item i left join i.department d"
                + " where b.retired = false and bi.retired = false"
                + " and b.billTypeAtomic in :btas"
                + " and b.createdAt between :fd and :td"
                + billLocationConditions(params)
                + " group by d";
        params.put("btas", serviceBillTypes());
        params.put("fd", fromDate);
        params.put("td", toDate);

        serviceRows = new ArrayList<>();
        List<Object[]> results = billFacade.findObjectsArrayByJpql(jpql, params, TemporalType.TIMESTAMP);
        if (results == null) {
            return;
        }
        for (Object[] r : results) {
            Department d = (Department) r[0];
            IncomeRow row = new IncomeRow(d == null ? "(No department)" : d.getName());
            row.setNetTotal(toDouble(r[1]));
            serviceRows.add(row);
            serviceTotal += row.getNetTotal();
        }
        serviceRows.sort(Comparator.comparing(IncomeRow::getName));
    }

    private void fillChannelIncome() {
        Map<String, Object> params = new HashMap<>();
        // A settlement bill (payment for an unpaid booking, or the online
        // settlement of one) is not counted, and neither is its cancellation
        // or refund: cancelling a settled credit booking writes a reversal for
        // the booking AND for its payment bill, with the same bill type.
        String jpql = "select sum(b.staffFee), sum(b.hospitalFee), sum(b.netTotal)"
                + " from Bill b left join b.billedBill bb"
                + " where b.retired = false"
                + " and b.createdAt between :fd and :td"
                + " and ((b.billTypeAtomic in :bookings"
                + "       and (b.billTypeAtomic <> :onlineSettle or b.referenceBill is null))"
                + "   or (b.billTypeAtomic in :reversals"
                + "       and (bb is null or (bb.billTypeAtomic <> :payForBooking"
                + "            and (bb.billTypeAtomic <> :onlineSettle or bb.referenceBill is null)))))"
                + billLocationConditions(params);
        params.put("bookings", Arrays.asList(
                BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT,
                BillTypeAtomic.CHANNEL_BOOKING_WITHOUT_PAYMENT,
                BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT_ONLINE,
                BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_COMPLETED_PAYMENT));
        params.put("reversals", Arrays.asList(
                BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT,
                BillTypeAtomic.CHANNEL_CANCELLATION_WITHOUT_PAYMENT,
                BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT_ONLINE_BOOKING,
                BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT_FOR_CREDIT_SETTLED_BOOKINGS,
                BillTypeAtomic.CHANNEL_REFUND,
                BillTypeAtomic.CHANNEL_REFUND_WITH_PAYMENT,
                BillTypeAtomic.CHANNEL_REFUND_WITH_PAYMENT_FOR_CREDIT_SETTLED_BOOKINGS));
        params.put("onlineSettle", BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT_ONLINE);
        params.put("payForBooking", BillTypeAtomic.CHANNEL_PAYMENT_FOR_BOOKING_BILL);
        params.put("fd", fromDate);
        params.put("td", toDate);

        channelRow = new IncomeRow("Channelling");
        List<Object[]> results = billFacade.findObjectsArrayByJpql(jpql, params, TemporalType.TIMESTAMP);
        if (results != null && !results.isEmpty() && results.get(0) != null) {
            Object[] r = results.get(0);
            channelRow.setDoctorFee(toDouble(r[0]));
            channelRow.setHospitalFee(toDouble(r[1]));
            channelRow.setNetTotal(toDouble(r[2]));
        }
    }

    /**
     * Wards: the saved final bill of each admission discharged in the period
     * whose final bill is completed. Each final bill item carries its inward
     * charge type; a bill-level discount is not spread onto the items, so it
     * is read from the bill. When an admission has more than one active final
     * bill, only the latest is counted.
     */
    private void fillWardIncome() {
        Map<String, Object> params = new HashMap<>();
        String from = " from BillItem bi join bi.bill b join b.patientEncounter pe"
                + " where bi.retired = false";
        String billConditions = " and b.retired = false and b.cancelled = false"
                + " and b.billTypeAtomic = :fb"
                + " and b.id = (select max(b2.id) from Bill b2 where b2.patientEncounter = pe"
                + "     and b2.billTypeAtomic = :fb and b2.retired = false and b2.cancelled = false)"
                + " and pe.retired = false and pe.parentEncounter is null"
                + " and pe.discharged = true and pe.paymentFinalized = true"
                + " and pe.dateOfDischarge between :fd and :td"
                + encounterLocationConditions(params);
        params.put("fb", BillTypeAtomic.INWARD_FINAL_BILL);
        params.put("fd", fromDate);
        params.put("td", toDate);

        Map<Department, IncomeRow> byDepartment = new HashMap<>();
        List<Object[]> charges = billFacade.findObjectsArrayByJpql(
                "select pe.department, bi.inwardChargeType, sum(bi.netValue)" + from + billConditions
                + " group by pe.department, bi.inwardChargeType", params, TemporalType.TIMESTAMP);
        if (charges != null) {
            for (Object[] r : charges) {
                IncomeRow row = wardRow(byDepartment, (Department) r[0]);
                InwardChargeType type = (InwardChargeType) r[1];
                double v = toDouble(r[2]);
                if (PROFESSIONAL_CHARGE_TYPES.contains(type)) {
                    row.setProfessionalFee(row.getProfessionalFee() + v);
                } else if (MEDICINE_CHARGE_TYPES.contains(type)) {
                    row.setMedicine(row.getMedicine() + v);
                } else {
                    row.setOther(row.getOther() + v);
                }
            }
        }

        List<Object[]> bills = billFacade.findObjectsArrayByJpql(
                "select pe.department, count(b), sum(b.discount)"
                + " from Bill b join b.patientEncounter pe where 1 = 1" + billConditions
                + " group by pe.department", params, TemporalType.TIMESTAMP);
        if (bills != null) {
            for (Object[] r : bills) {
                IncomeRow row = wardRow(byDepartment, (Department) r[0]);
                row.setCount(r[1] instanceof Number ? ((Number) r[1]).longValue() : 0);
                row.setDiscount(toDouble(r[2]));
            }
        }

        wardRows = new ArrayList<>(byDepartment.values());
        wardRows.sort(Comparator.comparing(IncomeRow::getName));
        for (IncomeRow row : wardRows) {
            row.setNetTotal(row.getProfessionalFee() + row.getMedicine() + row.getOther() - row.getDiscount());
            wardProfessionalTotal += row.getProfessionalFee();
            wardMedicineTotal += row.getMedicine();
            wardOtherTotal += row.getOther();
            wardDiscountTotal += row.getDiscount();
            wardNetTotal += row.getNetTotal();
        }
    }

    private IncomeRow wardRow(Map<Department, IncomeRow> byDepartment, Department d) {
        return byDepartment.computeIfAbsent(d, k -> new IncomeRow(k == null ? "(No department)" : k.getName()));
    }

    /**
     * Institution / Site / Department filters on the admission. Uses the
     * alias "pe" for the patient encounter.
     */
    private String encounterLocationConditions(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        if (institution != null) {
            sb.append(" and pe.institution = :ins");
            params.put("ins", institution);
        }
        if (site != null) {
            sb.append(" and pe.department.site = :site");
            params.put("site", site);
        }
        if (department != null) {
            sb.append(" and pe.department = :dep");
            params.put("dep", department);
        }
        return sb.toString();
    }

    private void fillCollectingCentreIncome() {
        Map<String, Object> params = new HashMap<>();
        String jpql = "select cc, sum(b.netTotal)"
                + " from Bill b left join b.collectingCentre cc"
                + " where b.retired = false"
                + " and b.billTypeAtomic in :btas"
                + " and b.createdAt between :fd and :td"
                + billLocationConditions(params)
                + " group by cc";
        params.put("btas", Arrays.asList(BillTypeAtomic.CC_BILL, BillTypeAtomic.CC_BILL_CANCELLATION, BillTypeAtomic.CC_BILL_REFUND));
        params.put("fd", fromDate);
        params.put("td", toDate);

        collectingCentreRows = new ArrayList<>();
        List<Object[]> results = billFacade.findObjectsArrayByJpql(jpql, params, TemporalType.TIMESTAMP);
        if (results == null) {
            return;
        }
        for (Object[] r : results) {
            Institution cc = (Institution) r[0];
            IncomeRow row = new IncomeRow(cc == null ? "(No collecting centre)" : cc.getName());
            row.setNetTotal(toDouble(r[1]));
            collectingCentreRows.add(row);
            collectingCentreTotal += row.getNetTotal();
        }
        collectingCentreRows.sort(Comparator.comparing(IncomeRow::getName));
    }

    /**
     * Institution / Site / Department filters on the billing location. Uses
     * the alias "b" for the bill.
     */
    private String billLocationConditions(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        if (institution != null) {
            sb.append(" and b.institution = :ins");
            params.put("ins", institution);
        }
        if (site != null) {
            sb.append(" and b.department.site = :site");
            params.put("site", site);
        }
        if (department != null) {
            sb.append(" and b.department = :dep");
            params.put("dep", department);
        }
        return sb.toString();
    }

    /**
     * Individual OPD bills (each carries its items) with their cancellations
     * and refunds. Batch bills are left out because their items are on these
     * individual bills; bills still waiting for payment at the cashier are not
     * yet billed income.
     */
    private List<BillTypeAtomic> serviceBillTypes() {
        return Arrays.asList(
                BillTypeAtomic.OPD_BILL_WITH_PAYMENT,
                BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER,
                BillTypeAtomic.OPD_BILL_CANCELLATION,
                BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION,
                BillTypeAtomic.OPD_BILL_REFUND,
                BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT,
                BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER,
                BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION,
                BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION,
                BillTypeAtomic.PACKAGE_OPD_BILL_REFUND);
    }

    private static double toDouble(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0.0;
    }

    public double getChannelHospitalFee() {
        return channelRow == null ? 0.0 : channelRow.getHospitalFee();
    }

    public boolean isProcessed() {
        return channelRow != null;
    }

    public static class IncomeRow implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String name;
        private long count;
        private double doctorFee;
        private double hospitalFee;
        private double professionalFee;
        private double medicine;
        private double other;
        private double discount;
        private double netTotal;

        public IncomeRow(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public double getDoctorFee() {
            return doctorFee;
        }

        public void setDoctorFee(double doctorFee) {
            this.doctorFee = doctorFee;
        }

        public double getHospitalFee() {
            return hospitalFee;
        }

        public void setHospitalFee(double hospitalFee) {
            this.hospitalFee = hospitalFee;
        }

        public double getProfessionalFee() {
            return professionalFee;
        }

        public void setProfessionalFee(double professionalFee) {
            this.professionalFee = professionalFee;
        }

        public double getMedicine() {
            return medicine;
        }

        public void setMedicine(double medicine) {
            this.medicine = medicine;
        }

        public double getOther() {
            return other;
        }

        public void setOther(double other) {
            this.other = other;
        }

        public double getDiscount() {
            return discount;
        }

        public void setDiscount(double discount) {
            this.discount = discount;
        }

        public double getNetTotal() {
            return netTotal;
        }

        public void setNetTotal(double netTotal) {
            this.netTotal = netTotal;
        }
    }

    @Override
    public Date getFromDate() {
        return fromDate;
    }

    @Override
    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    @Override
    public Date getToDate() {
        return toDate;
    }

    @Override
    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    @Override
    public Institution getInstitution() {
        return institution;
    }

    @Override
    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    @Override
    public Institution getSite() {
        return site;
    }

    @Override
    public void setSite(Institution site) {
        this.site = site;
    }

    @Override
    public Department getDepartment() {
        return department;
    }

    @Override
    public void setDepartment(Department department) {
        this.department = department;
    }

    @Override
    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    @Override
    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    @Override
    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    @Override
    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    @Override
    public ReportViewType getReportViewType() {
        return reportViewType;
    }

    @Override
    public void setReportViewType(ReportViewType reportViewType) {
        this.reportViewType = reportViewType;
    }

    public List<IncomeRow> getServiceRows() {
        return serviceRows;
    }

    public IncomeRow getChannelRow() {
        return channelRow;
    }

    public List<IncomeRow> getWardRows() {
        return wardRows;
    }

    public List<IncomeRow> getCollectingCentreRows() {
        return collectingCentreRows;
    }

    public double getServiceTotal() {
        return serviceTotal;
    }

    public double getWardProfessionalTotal() {
        return wardProfessionalTotal;
    }

    public double getWardMedicineTotal() {
        return wardMedicineTotal;
    }

    public double getWardOtherTotal() {
        return wardOtherTotal;
    }

    public double getWardDiscountTotal() {
        return wardDiscountTotal;
    }

    public double getWardNetTotal() {
        return wardNetTotal;
    }

    public double getCollectingCentreTotal() {
        return collectingCentreTotal;
    }

    public double getGrandTotal() {
        return grandTotal;
    }
}
