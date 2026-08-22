/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.FeeType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.DepartmentBillItems;
import com.divudi.core.data.inward.InwardChargeType;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Consultant;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Fee;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.PatientItem;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.InwardFee;
import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.entity.inward.Room;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.entity.inward.TimedItem;
import com.divudi.core.entity.inward.TimedItemFee;
import com.divudi.core.entity.inward.AdmissionNumber;
import com.divudi.core.entity.inward.PatientRoomTimedItemCharge;
import com.divudi.core.entity.inward.RoomFacilityTimedItem;
import com.divudi.core.facade.AdmissionFacade;
import com.divudi.core.facade.PatientRoomTimedItemChargeFacade;
import com.divudi.core.facade.RoomFacilityTimedItemFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.FeeFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.ItemFeeFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PatientItemFacade;
import com.divudi.core.facade.PatientRoomFacade;
import com.divudi.core.facade.PriceMatrixFacade;
import com.divudi.core.facade.RoomFacade;
import com.divudi.core.facade.TimedItemFeeFacade;
import com.divudi.core.util.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class InwardBeanController implements Serializable {

    @EJB
    private PatientRoomFacade patientRoomFacade;
    @EJB
    PatientEncounterFacade patientEncounterFacade;
    @EJB
    private RoomFacade roomFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private FeeFacade feeFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    PatientItemFacade patientItemFacade;
    @EJB
    private TimedItemFeeFacade timedItemFeeFacade;
    @EJB
    private RoomFacilityTimedItemFacade roomFacilityTimedItemFacade;
    @EJB
    private PatientRoomTimedItemChargeFacade patientRoomTimedItemChargeFacade;
    @EJB
    private ItemFeeFacade itemFeeFacade;
    @EJB
    private PriceMatrixFacade priceMatrixFacade;
    @EJB
    private AdmissionFacade admissionFacade;
    @EJB
    private PatientEncounterFacade encounterFacade;
    @EJB
    BillNumberGenerator billNumberGenerator;
    @EJB
    private com.divudi.core.facade.EncounterCreditCompanyFacade encounterCreditCompanyFacade;

    @Inject
    BillBeanController billBean;
    @Inject
    InwardReportControllerBht inwardReportControllerBht;
    @Inject
    SessionController sessionController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    com.divudi.bean.common.PriceMatrixController priceMatrixController;

    private Long lastGeneratedBhtLong;

    public Long getLastGeneratedBhtLong() {
        return lastGeneratedBhtLong;
    }

    public String inwardDepositBillText(Bill b) {
        String template = sessionController.getDepartmentPreference().getInwardDepositBillTemplate();
        Map<String, String> replaceables = Bill.toMap(b);
        return CommonFunctions.replaceText(replaceables, template);
    }

    public List<BillItem> createBillItems(Item item, PatientEncounter patientEncounter) {
        String sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.item=:itm"
                + " and b.bill.patientEncounter=:pe ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        hm.put("pe", patientEncounter);
        hm.put("itm", item);
        return getBillItemFacade().findByJpql(sql, hm, TemporalType.TIME);
    }

    /**
     * Inward service BillItems for the "Inward Services" breakdown.
     * <p>
     * TimedItems are excluded because the breakdown screens list them
     * separately from their PatientItems (see
     * {@code resources/inward/breakDown/timedService.xhtml}); leaving them in
     * would show each timed service twice on the same page.
     */
    public List<BillItem> fetchBillItems(PatientEncounter patientEncounter) {
        String sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and type(b.item)!=:cls "
                + " and b.bill.patientEncounter=:pe ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        hm.put("cls", TimedItem.class);
        hm.put("pe", patientEncounter);
        return getBillItemFacade().findByJpql(sql, hm);
    }

    /**
     * Uncached variant of {@link #fetchBillItems(PatientEncounter)}; TimedItems
     * are excluded for the same reason.
     */
    public List<BillItem> fetchEagerBillItems(PatientEncounter patientEncounter) {
        String sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and type(b.item)!=:cls "
                + " and b.bill.patientEncounter=:pe ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        hm.put("cls", TimedItem.class);
        hm.put("pe", patientEncounter);
        return getBillItemFacade().findByJpqlWithoutCache(sql, hm);
    }

    public List<BillItem> fetchBillItems(BillType billType, Bill bill) {
        String sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and type(b.bill)=:class ";
        HashMap hm = new HashMap();
        hm.put("btp", billType);
        hm.put("class", bill.getClass());
        return getBillItemFacade().findByJpql(sql, hm);
    }

    public List<BillItem> fetchBillItems(PatientEncounter patientEncounter, InwardChargeType inwardChargeType) {
        String sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.item.inwardChargeType=:inw"
                + " and b.bill.billType=:btp "
                + " and b.bill.patientEncounter=:pe ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        hm.put("inw", inwardChargeType);
        hm.put("pe", patientEncounter);
        return getBillItemFacade().findByJpql(sql, hm, TemporalType.TIME);
    }

    public List<BillFee> fetchBillFees(PatientEncounter patientEncounter) {
        String sql = "SELECT  b FROM BillFee b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.bill.patientEncounter=:pe ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        hm.put("pe", patientEncounter);
        return billFeeFacade.findByJpql(sql, hm, TemporalType.TIME);
    }

    public List<BillFee> fetchBillFees(PatientEncounter patientEncounter, InwardChargeType inwardChargeType) {
        String sql = "SELECT  b FROM BillFee b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.billItem.item.inwardChargeType=:inw "
                + " and b.bill.patientEncounter=:pe ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        hm.put("inw", inwardChargeType);
        hm.put("pe", patientEncounter);
        return billFeeFacade.findByJpql(sql, hm, TemporalType.TIME);
    }

    public boolean checkRoomDischarge(Date date, PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT pr FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.dischargedAt>:dt "
                + " and pr.patientEncounter=:pe ";
        hm.put("pe", patientEncounter);
        hm.put("dt", date);
        PatientRoom tmp = getPatientRoomFacade().findFirstByJpql(sql, hm, TemporalType.TIMESTAMP);

        if (tmp != null) {
            return true;
        }

        return false;
    }

    public double calTimedPatientItemByInwardChargeType(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {

        HashMap hm = new HashMap();
        String sql = " SELECT sum(i.serviceValue) "
                + " FROM PatientItem i where "
                + " type(i.item)=:cls "
                + " and i.retired=false "
                + " and i.billItem is null "
                + " and i.patientEncounter=:pe "
                + " and i.item.inwardChargeType=:inw ";
        hm.put("pe", patientEncounter);
        hm.put("cls", TimedItem.class);
        hm.put("inw", inwardChargeType);
        return getPatientItemFacade().findDoubleByJpql(sql, hm);

    }

    public List<BillItem> getIssueBillItemByInwardChargeType(PatientEncounter patientEncounter, BillType billType) {
        String sql = "Select s From BillItem s"
                + " where s.retired=false"
                + " and s.bill.billType=:btp"
                + " and s.bill.patientEncounter=:pe ";

        HashMap hm = new HashMap();
        hm.put("btp", billType);
        hm.put("pe", patientEncounter);

        return getBillItemFacade().findByJpql(sql, hm);

    }

    public Double[] fetchDiscountAndNetTotalByBillItem(Bill b) {
        String sql = "Select sum(s.discount),"
                + " sum(s.netValue) "
                + "  From BillItem s"
                + " where s.retired=false"
                + " and s.bill=:bill";

        HashMap hm = new HashMap();
        hm.put("bill", b);

        Object[] obj = getBillItemFacade().findAggregateModified(sql, hm, TemporalType.DATE);

        if (obj == null) {
            Double[] dbl = new Double[2];
            dbl[0] = 0.0;
            dbl[0] = 0.0;

            return dbl;
        }

        return Arrays.copyOf(obj, obj.length, Double[].class);

    }

    public List<Bill> fetchIssueBills(PatientEncounter patientEncounter, BillType billType) {
        String sql = "Select distinct(s.bill)"
                + "  From BillItem s"
                + " where s.retired=false"
                + " and s.bill.billType=:btp"
                + " and s.bill.patientEncounter=:pe ";

        HashMap hm = new HashMap();
        hm.put("btp", billType);
        hm.put("pe", patientEncounter);

        return billFacade.findByJpql(sql, hm);

    }

    public double calIssueBillItemDiscountByInwardChargeType(PatientEncounter patientEncounter, BillType billType) {
        String sql = "Select sum(s.discount) From BillItem s"
                + " where s.retired=false"
                + " and s.bill.billType=:btp"
                + " and s.bill.patientEncounter=:pe ";

        HashMap hm = new HashMap();
        hm.put("btp", billType);
        hm.put("pe", patientEncounter);

        return getBillItemFacade().findDoubleByJpql(sql, hm);

    }

    public double getTimedItemFeeTotalByInwardChargeType(InwardChargeType inwardChargeType, PatientEncounter patientEncounter, List<PatientEncounter> cpts) {

        HashMap hm = new HashMap();
        String sql = " SELECT sum(i.serviceValue) "
                + " FROM PatientItem i where "
                + " type(i.item)=:cls "
                + " and i.retired=false "
                + " and i.billItem is null "
                + " and i.patientEncounter IN :pe "
                + " and i.item.inwardChargeType=:inw ";
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);
        hm.put("cls", TimedItem.class);
        hm.put("inw", inwardChargeType);
        double dbl = getPatientItemFacade().findDoubleByJpql(sql, hm);

        return dbl;
    }

    /**
     * OPTIMIZED: Bulk version - fetches all timed item fee totals in ONE query
     * <p>
     * Only PatientItems with no BillItem are counted here. A timed service that
     * already has a BillItem is charged through
     * {@link #calServiceBillItemsTotalByInwardChargeTypeBulk}, which sums every
     * BillItem on an InwardBill regardless of item type — counting it on both
     * sides would double-charge the patient. The same {@code billItem is null}
     * guard therefore applies to every PatientItem query that feeds a total or
     * a discount.
     */
    public Map<InwardChargeType, Double> getTimedItemFeeTotalByInwardChargeTypeBulk(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        HashMap hm = new HashMap();
        String sql = " SELECT i.item.inwardChargeType, sum(i.serviceValue) "
                + " FROM PatientItem i WHERE "
                + " type(i.item)=:cls "
                + " AND i.retired=false "
                + " AND i.billItem is null "
                + " AND i.patientEncounter IN :pe "
                + " GROUP BY i.item.inwardChargeType";

        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);
        hm.put("cls", TimedItem.class);

        List<Object[]> results = getPatientItemFacade().findObjectsArrayByJpql(sql, hm, TemporalType.TIMESTAMP);

        Map<InwardChargeType, Double> totalsMap = new HashMap<>();
        if (results != null) {
            for (Object[] row : results) {
                InwardChargeType chargeType = (InwardChargeType) row[0];
                Double total = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                totalsMap.put(chargeType, total);
            }
        }

        return totalsMap;
    }

    public List<PatientItem> fetchTimedPatientItemByInwardChargeType(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {

        HashMap hm = new HashMap();
        String sql = " SELECT i "
                + " FROM PatientItem i where "
                + " type(i.item)=:cls "
                + " and i.retired=false "
                + " and i.billItem is null "
                + " and i.patientEncounter=:pe "
                + " and i.item.inwardChargeType=:inw ";
        hm.put("pe", patientEncounter);
        hm.put("cls", TimedItem.class);
        hm.put("inw", inwardChargeType);
        return getPatientItemFacade().findByJpql(sql, hm);

    }

    public List<PatientRoom> fetchPatientRoomAll(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        HashMap hm = new HashMap();
        String sql = "SELECT pr FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter in :pe "
                + " order by pr.createdAt";
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);
        return getPatientRoomFacade().findByJpql(sql, hm);

    }

    public double calCostOfIssue(PatientEncounter patientEncounter, BillType billType, List<PatientEncounter> cpts) {
        String sql;
        HashMap hm;
        sql = "SELECT  sum(b.grossValue+b.marginValue)"
                + " FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and  b.bill.patientEncounter IN :pe";
        hm = new HashMap();
        hm.put("btp", billType);
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);
        return getBillItemFacade().findDoubleByJpql(sql, hm);

    }

    public double calNetCostOfIssue(PatientEncounter patientEncounter, BillType billType, List<PatientEncounter> cpts) {
        String sql = "SELECT sum(b.netTotal)"
                + " FROM Bill b"
                + " WHERE b.retired=false"
                + " and b.billType=:btp"
                + " and b.patientEncounter IN :pe";
        HashMap hm = new HashMap();
        hm.put("btp", billType);
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);
        return getBillFacade().findDoubleByJpql(sql, hm);
    }

    public double calCostOfIssueByBill(PatientEncounter patientEncounter, List<BillTypeAtomic> btas, List<PatientEncounter> cpts) {
        String sql;
        HashMap hm;
        sql = "SELECT  sum(b.netTotal)"
                + " FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billTypeAtomic IN :btp "
                + " and  b.patientEncounter IN :pe";
        hm = new HashMap();
        hm.put("btp", btas);
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);
        return getBillItemFacade().findDoubleByJpql(sql, hm);
    }
    
    /**
     * Sums the value of cancelled/returned issue bills as a positive magnitude, so it can be
     * printed as its own breakup line instead of silently netting out of the parent charge
     * total (issue #22674). {@code cancellationBtas} should be the cancellation-only subset of
     * the {@link BillTypeAtomic} list already used to compute the parent charge type's net total.
     */
    public double calCancelledCostOfIssueByBill(PatientEncounter patientEncounter, List<BillTypeAtomic> cancellationBtas, List<PatientEncounter> cpts) {
        String sql;
        HashMap hm;
        sql = "SELECT  sum(b.netTotal)"
                + " FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billTypeAtomic IN :btp "
                + " and  b.patientEncounter IN :pe";
        hm = new HashMap();
        hm.put("btp", cancellationBtas);
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);
        return -getBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public double calCostOfIssueByBill(PatientEncounter patientEncounter, List<BillTypeAtomic> btas, List<PatientEncounter> cpts, DepartmentType billingDepartmentType) {
        String sql;
        HashMap hm;
        sql = "SELECT  sum(b.netTotal)"
                + " FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billTypeAtomic IN :btp "
                + " and  b.patientEncounter IN :pe"
                + " and  b.department.departmentType = :type";
        hm = new HashMap();
        hm.put("btp", btas);
        hm.put("type", billingDepartmentType);
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);
        return getBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public double calServiceBillItemsTotalByInwardChargeType(InwardChargeType inwardChargeType, PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        String sql = "Select sum(s.grossValue+s.marginValue) "
                + " From BillItem s"
                + " where s.retired=false "
                + " and s.bill.billType=:btp "
                + " and s.bill.patientEncounter IN :pe"
                + " and s.item.inwardChargeType=:inw ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);
        hm.put("inw", inwardChargeType);

        double dbl = getBillFeeFacade().findDoubleByJpql(sql, hm);

        return dbl;

    }

    /**
     * OPTIMIZED: Fetches all inward charge type totals in ONE query instead of N queries
     * Performance: 52 seconds -> <2 seconds
     */
    public Map<InwardChargeType, Double> calServiceBillItemsTotalByInwardChargeTypeBulk(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        String sql = "SELECT s.item.inwardChargeType, sum(s.grossValue+s.marginValue) "
                + " FROM BillItem s"
                + " WHERE s.retired=false "
                + " AND s.bill.billType=:btp "
                + " AND s.bill.patientEncounter IN :pe"
                + " GROUP BY s.item.inwardChargeType";

        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if(cpts != null && !cpts.isEmpty()){
            pts.addAll(cpts);
        }
        hm.put("pe", pts);

        List<Object[]> results = getBillItemFacade().findObjectsArrayByJpql(sql, hm, TemporalType.TIMESTAMP);

        Map<InwardChargeType, Double> totalsMap = new HashMap<>();
        if (results != null) {
            for (Object[] row : results) {
                InwardChargeType chargeType = (InwardChargeType) row[0];
                Double total = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                totalsMap.put(chargeType, total);
            }
        }

        return totalsMap;
    }

    /**
     * Bulk query to get the Gross/Margin/VAT totals per InwardChargeType for
     * services/investigations (BillItem-backed), mirroring
     * {@link #calServiceBillItemsTotalByInwardChargeTypeBulk}. Returns a map of
     * InwardChargeType -> {gross, margin, vat}.
     */
    public Map<InwardChargeType, double[]> calServiceBillItemsGrossMarginVatByInwardChargeTypeBulk(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        String sql = "SELECT s.item.inwardChargeType, sum(s.grossValue), sum(s.marginValue), sum(s.vat) "
                + " FROM BillItem s"
                + " WHERE s.retired=false "
                + " AND s.bill.billType=:btp "
                + " AND s.bill.patientEncounter IN :pe"
                + " GROUP BY s.item.inwardChargeType";

        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);

        List<Object[]> results = getBillItemFacade().findObjectsArrayByJpql(sql, hm, TemporalType.TIMESTAMP);

        Map<InwardChargeType, double[]> totalsMap = new HashMap<>();
        if (results != null) {
            for (Object[] row : results) {
                InwardChargeType chargeType = (InwardChargeType) row[0];
                double gross = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                double margin = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
                double vat = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
                totalsMap.put(chargeType, new double[]{gross, margin, vat});
            }
        }

        return totalsMap;
    }

    public double calculateProfessionalCharges(PatientEncounter patientEncounter, List<PatientEncounter> cpts, boolean isEstimatedBill) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(bt.feeValue)"
                + " FROM BillFee bt"
                + " WHERE bt.retired=false"
                + " and type(bt.staff)=:class "
                + " and bt.fee.feeType=:ftp  "
                + " and bt.bill.patientEncounter IN :pe";
        hm.put("class", Consultant.class);
        hm.put("ftp", FeeType.Staff);
        //  hm.put("btp", BillType.InwardBill);

        if (isEstimatedBill) {
            sql += " and bt.bill.billType in :bt";
            List<BillType> bts = List.of(BillType.InwardProfessional, BillType.InwardProfessionalEstimates);
            hm.put("bt", bts);

        } else if (!isEstimatedBill) {
            sql += " and bt.bill.billType = :bt";
            hm.put("bt", BillType.InwardProfessional);
        }

        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);

        double val = getBillFeeFacade().findDoubleByJpql(sql, hm, TemporalType.TIME);

        return val;
    }

    public double calOutSideBillItemsTotalByInwardChargeType(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {
        String sql = "Select sum(s.feeValue) From BillFee s"
                + " where s.retired=false "
                + " and s.billItem.bill.billType=:btp "
                + " and s.billItem.bill.patientEncounter=:pe"
                + " and s.billItem.inwardChargeType=:inw ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardOutSideBill);
        hm.put("pe", patientEncounter);
        hm.put("inw", inwardChargeType);

        double dbl = getBillFeeFacade().findDoubleByJpql(sql, hm);

        return dbl;

    }

    public List<BillFee> getServiceBillFeesByInwardChargeType(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {
        String sql = "Select s From BillFee s"
                + " where s.retired=false"
                //  + " and s.bill.cancelled=false"
                // + " and type(s.bill)=:billedClass "
                + " and s.billItem.bill.billType=:btp "
                + " and s.billItem.bill.patientEncounter=:pe"
                + " and s.billItem.item.inwardChargeType=:inw "
                + " and s.fee.feeType!=:st ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        hm.put("pe", patientEncounter);
        hm.put("inw", inwardChargeType);
        //  hm.put("billedClass", BilledBill.class);
        hm.put("st", FeeType.Staff);

        return getBillFeeFacade().findByJpql(sql, hm);

    }

    /**
     * Service BillItems whose net value is derived from their BillFees.
     * <p>
     * TimedItem BillItems are excluded on purpose. A timed service is priced by
     * duration and carries no BillFee, so running it through
     * {@code updateBillItemByBillFee} would recompute its net value as the sum
     * of zero fees and silently wipe the charge. Timed services are discounted
     * directly on the BillItem instead — see
     * {@link #fetchTimedServiceBillItemsByInwardChargeType}.
     */
    public List<BillItem> getServiceBillItemByInwardChargeType(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {
        String sql = "Select s From BillItem s"
                + " where s.retired=false "
                + " and s.bill.billType=:btp "
                + " and s.bill.patientEncounter=:pe"
                + " and type(s.item)!=:cls "
                + " and s.item.inwardChargeType=:inw ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        hm.put("pe", patientEncounter);
        hm.put("cls", TimedItem.class);
        hm.put("inw", inwardChargeType);

        return getBillItemFacade().findByJpql(sql, hm);

    }

    /**
     * Timed-service BillItems for one charge type, excluding package-locked
     * ones (their price is fixed by the package and must not be discounted).
     * These are priced from the PatientItem duration, so their discount is
     * applied straight to the BillItem rather than through BillFees.
     */
    public List<BillItem> fetchTimedServiceBillItemsByInwardChargeType(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {
        String sql = "Select s From BillItem s"
                + " where s.retired=false "
                + " and s.bill.billType=:btp "
                + " and s.bill.patientEncounter=:pe"
                + " and type(s.item)=:cls "
                + " and s.fromPackage=false "
                + " and s.item.inwardChargeType=:inw ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        hm.put("pe", patientEncounter);
        hm.put("cls", TimedItem.class);
        hm.put("inw", inwardChargeType);

        return getBillItemFacade().findByJpql(sql, hm);
    }

    /**
     * The PatientItem carrying the timing behind a timed-service BillItem.
     * BillItem has no back-reference, so it is looked up from the owning side.
     */
    public PatientItem fetchPatientItemByBillItem(BillItem billItem) {
        if (billItem == null) {
            return null;
        }
        String sql = "Select i From PatientItem i"
                + " where i.retired=false"
                + " and i.billItem=:bi";
        HashMap hm = new HashMap();
        hm.put("bi", billItem);
        return getPatientItemFacade().findFirstByJpql(sql, hm);
    }

    /**
     * Timed services still running (no stop time) on an admission and on any
     * child encounters attached to it. Used to close them off automatically at
     * discharge.
     * <p>
     * Child encounters are included because a baby's charges are settled on the
     * mother's final bill — the timed-service totals already sum both — so a
     * baby's running service has to be stopped and repriced along with hers.
     */
    public List<PatientItem> fetchRunningTimedPatientItems(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        String sql = "Select i From PatientItem i"
                + " where i.retired=false"
                + " and type(i.item)=:cls"
                + " and i.patientEncounter IN :pe"
                + " and i.toTime is null";
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        HashMap hm = new HashMap();
        hm.put("cls", TimedItem.class);
        hm.put("pe", pts);
        return getPatientItemFacade().findByJpql(sql, hm);
    }

    /**
     * Clears any previously applied discount on timed-service BillItems when no
     * price matrix applies. Mirrors {@link #bulkClearPatientItemsWithOutMatrix}
     * for the BillItem side.
     * <p>
     * The matching PatientItems are cleared too. Their discount is a mirror of
     * the BillItem's, kept for the breakdown screens and for
     * {@code InwardChargeTypeBreakdownController} /
     * {@code InwardChargeTypeDetailController}, which subtract it to show a net
     * figure. {@link #bulkClearPatientItemsWithOutMatrix} cannot reach them —
     * it filters on {@code billItem is null} — so without this they would keep
     * displaying a discount that has just been removed from the bill.
     */
    public void bulkClearTimedServiceBillItemsWithOutMatrix(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {
        String sql = "UPDATE BillItem s SET s.discount = 0.0, s.netValue = s.grossValue + s.marginValue"
                + " WHERE s.retired = false"
                + " AND s.bill.billType = :btp"
                + " AND s.bill.patientEncounter = :pe"
                + " AND type(s.item) = :cls"
                + " AND s.fromPackage = false"
                + " AND s.item.inwardChargeType = :inw";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        hm.put("cls", TimedItem.class);
        hm.put("pe", patientEncounter);
        hm.put("inw", inwardChargeType);
        getBillItemFacade().updateByJpql(sql, hm);

        // Deliberately not filtered on billItem.fromPackage. A package item's
        // price is fixed and never discounted, so its mirrored discount is
        // already zero and clearing it changes nothing — and avoiding that
        // navigation keeps this a plain bulk update over PatientItem's own
        // columns, matching bulkClearPatientItemsWithOutMatrix.
        String piSql = "UPDATE PatientItem s SET s.discount = 0.0"
                + " WHERE s.retired = false"
                + " AND type(s.item) = :cls"
                + " AND s.billItem is not null"
                + " AND s.patientEncounter = :pe"
                + " AND s.item.inwardChargeType = :inw";
        HashMap piHm = new HashMap();
        piHm.put("cls", TimedItem.class);
        piHm.put("pe", patientEncounter);
        piHm.put("inw", inwardChargeType);
        getPatientItemFacade().updateByJpql(piSql, piHm);
    }

    public List<BillFee> createDoctorAndNurseFee(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {

        HashMap hm = new HashMap();
        String sql = "SELECT bt FROM BillFee bt WHERE "
                + " bt.retired=false "
                + " and type(bt.staff)!=:class "
                + " and bt.fee.feeType=:ftp "
                + " and (bt.bill.billType=:btp)"
                + " and bt.bill.patientEncounter IN :pe ";
        hm.put("class", Consultant.class);
        hm.put("ftp", FeeType.Staff);
        hm.put("btp", BillType.InwardProfessional);
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);

        return getBillFeeFacade().findByJpql(sql, hm, TemporalType.TIME);

    }

    public List<BillFee> createProfesionallFee(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        HashMap hm = new HashMap();
        String sql = "SELECT bt FROM BillFee bt WHERE "
                + " bt.retired=false "
                + " and type(bt.staff)=:class "
                + " and bt.fee.feeType=:ftp "
                + " and (bt.bill.billType=:btp)"
                + " and bt.bill.patientEncounter IN :pe "
                + " order by bt.feeAdjusted desc ";
        hm.put("class", Consultant.class);
        hm.put("ftp", FeeType.Staff);
        hm.put("btp", BillType.InwardProfessional);
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);

        return getBillFeeFacade().findByJpql(sql, hm, TemporalType.TIME);
        //////// // System.out.println("Size : " + profesionallFee.size());

    }

    public List<BillFee> createProfesionallFeeEstimated(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT bt FROM BillFee bt WHERE "
                + " bt.retired=false "
                + " and type(bt.staff)=:class "
                + " and bt.fee.feeType=:ftp "
                + " and (bt.bill.billType=:btp)"
                + " and bt.bill.patientEncounter=:pe "
                + " order by bt.feeAdjusted desc ";
        hm.put("class", Consultant.class);
        hm.put("ftp", FeeType.Staff);
        hm.put("btp", BillType.InwardProfessionalEstimates);
        hm.put("pe", patientEncounter);

        return getBillFeeFacade().findByJpql(sql, hm, TemporalType.TIME);
        //////// // System.out.println("Size : " + profesionallFee.size());

    }

    public void setProfesionallFeeAdjusted(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        HashMap hm = new HashMap();
        String sql = "UPDATE BillFee bt SET bt.feeAdjusted = bt.feeValue"
                + " WHERE bt.retired=false"
                + " AND type(bt.staff)=:class"
                + " AND bt.fee.feeType=:ftp"
                + " AND bt.bill.billType=:btp"
                + " AND bt.bill.patientEncounter IN :pe";
        hm.put("class", Consultant.class);
        hm.put("ftp", FeeType.Staff);
        hm.put("btp", BillType.InwardProfessional);
        hm.put("pe", pts);
        getBillFeeFacade().updateByJpql(sql, hm);
    }

    /**
     * Mirror of {@link #setProfesionallFeeAdjusted} for assisting fees
     * (non-Consultant staff). Keeps the adjusted fee equal to the fee value for
     * assistant doctors on every navigation, so the Professional Fee and
     * Adjusted Fee columns always match - exactly as they do for consultants.
     */
    public void setAssistingFeeAdjusted(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        HashMap hm = new HashMap();
        String sql = "UPDATE BillFee bt SET bt.feeAdjusted = bt.feeValue"
                + " WHERE bt.retired=false"
                + " AND type(bt.staff)!=:class"
                + " AND bt.fee.feeType=:ftp"
                + " AND bt.bill.billType=:btp"
                + " AND bt.bill.patientEncounter IN :pe";
        hm.put("class", Consultant.class);
        hm.put("ftp", FeeType.Staff);
        hm.put("btp", BillType.InwardProfessional);
        hm.put("pe", pts);
        getBillFeeFacade().updateByJpql(sql, hm);
    }

    public void bulkClearServiceBillFeesWithOutMatrix(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {
        String sql = "UPDATE BillFee s SET s.feeDiscount = 0.0, s.feeValue = s.feeGrossValue + s.feeMargin"
                + " WHERE s.retired = false"
                + " AND s.billItem.bill.billType = :btp"
                + " AND s.billItem.bill.patientEncounter = :pe"
                + " AND s.billItem.item.inwardChargeType = :inw"
                + " AND s.fee.feeType != :st";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        hm.put("pe", patientEncounter);
        hm.put("inw", inwardChargeType);
        hm.put("st", FeeType.Staff);
        getBillFeeFacade().updateByJpql(sql, hm);

        String biSql = "UPDATE BillItem s SET s.discount = 0.0, s.netValue = s.grossValue + s.marginValue"
                + " WHERE s.retired = false"
                + " AND s.bill.billType = :btp"
                + " AND s.bill.patientEncounter = :pe"
                + " AND s.item.inwardChargeType = :inw";
        HashMap biHm = new HashMap();
        biHm.put("btp", BillType.InwardBill);
        biHm.put("pe", patientEncounter);
        biHm.put("inw", inwardChargeType);
        getBillItemFacade().updateByJpql(biSql, biHm);
    }

    public void bulkClearPatientItemsWithOutMatrix(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {
        String sql = "UPDATE PatientItem s SET s.discount = 0.0"
                + " WHERE s.retired = false"
                + " AND type(s.item) = :cls"
                + " AND s.billItem is null"
                + " AND s.patientEncounter = :pe"
                + " AND s.item.inwardChargeType = :inw";
        HashMap hm = new HashMap();
        hm.put("cls", TimedItem.class);
        hm.put("pe", patientEncounter);
        hm.put("inw", inwardChargeType);
        getPatientItemFacade().updateByJpql(sql, hm);
    }

    public List<Bill> fetchIssueTable(PatientEncounter patientEncounter, BillType billType, List<PatientEncounter> cpts) {
        List<Bill> list = new ArrayList<>();
        String sql;
        HashMap hm;
        sql = "SELECT  b FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billType=:btp "
                + " and (b.billedBill is null )  "
                + " and  b.patientEncounter IN :pe"
                + " and (type(b)=:class) ";
        hm = new HashMap();
        hm.put("btp", billType);
        hm.put("class", PreBill.class);
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);

        List<Bill> bills = getBillFacade().findByJpql(sql, hm);

        hm.clear();
        sql = "SELECT  b FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billType=:btp"
                + " and type(b.billedBill)=:billedClass "
                + " and  b.patientEncounter IN :pe"
                + " and (type(b)=:class) ";
        hm = new HashMap();
        hm.put("btp", billType);
        hm.put("class", RefundBill.class);
        hm.put("billedClass", PreBill.class);
        List<PatientEncounter> pts1 = new ArrayList<>();
        pts1.add(patientEncounter);
        List<PatientEncounter> cpts1 = cpts;
        if (cpts1.size() > 0) {
            for (PatientEncounter pt : cpts1) {
                pts1.add(pt);
            }
        }
        hm.put("pe", pts1);

        List<Bill> bills2 = getBillFacade().findByJpql(sql, hm);

        list.addAll(bills);
        list.addAll(bills2);

        List<Bill> sortedList = list.stream()
                .sorted(Comparator.comparing(Bill::getCreatedAt))
                .collect(Collectors.toList());

        return sortedList;
    }
    
    public List<Bill> fetchIssueTable(PatientEncounter patientEncounter, BillType billType, List<PatientEncounter> cpts, DepartmentType billingDepartmentType) {
        List<Bill> list = new ArrayList<>();
        String sql;
        HashMap hm;
        sql = "SELECT  b FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billType=:btp "
                + " and b.department.departmentType = :type"
                + " and (b.billedBill is null )  "
                + " and  b.patientEncounter IN :pe"
                + " and (type(b)=:class) ";
        hm = new HashMap();
        hm.put("btp", billType);
        hm.put("class", PreBill.class);
        hm.put("type", billingDepartmentType);
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);

        List<Bill> bills = getBillFacade().findByJpql(sql, hm);

        hm.clear();
        sql = "SELECT  b FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billType=:btp"
                + " and  b.department.departmentType = :type"
                + " and type(b.billedBill)=:billedClass "
                + " and  b.patientEncounter IN :pe"
                + " and (type(b)=:class) ";
        hm = new HashMap();
        hm.put("btp", billType);
        hm.put("class", RefundBill.class);
        hm.put("billedClass", PreBill.class);
        hm.put("type", billingDepartmentType);
        List<PatientEncounter> pts1 = new ArrayList<>();
        pts1.add(patientEncounter);
        List<PatientEncounter> cpts1 = cpts;
        if (cpts1.size() > 0) {
            for (PatientEncounter pt : cpts1) {
                pts1.add(pt);
            }
        }
        hm.put("pe", pts1);

        List<Bill> bills2 = getBillFacade().findByJpql(sql, hm);

        list.addAll(bills);
        list.addAll(bills2);

        List<Bill> sortedList = list.stream()
                .sorted(Comparator.comparing(Bill::getCreatedAt))
                .collect(Collectors.toList());

        return sortedList;
    }

    public List<BillItem> fetchPharmacyIssueBillItem(PatientEncounter patientEncounter, BillType billType) {
        List<BillItem> grantList = new ArrayList<>();
        String sql;
        HashMap hm;
        sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and  b.bill.patientEncounter=:pe"
                + " and (type(b.bill)=:class) ";
        hm = new HashMap();
        hm.put("btp", billType);
        hm.put("class", PreBill.class);
        hm.put("pe", patientEncounter);

        List<BillItem> list = getBillItemFacade().findByJpql(sql, hm);

        hm.clear();
        sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp"
                + " and  b.bill.patientEncounter=:pe"
                + " and (type(b.bill)=:class) ";
        hm = new HashMap();
        hm.put("btp", billType);
        hm.put("class", RefundBill.class);
        hm.put("pe", patientEncounter);

        List<BillItem> list2 = getBillItemFacade().findByJpql(sql, hm);

        grantList.addAll(list);
        grantList.addAll(list2);

        return grantList;

    }

    public List<BillItem> fetchBillItem1(BillType billType) {
//        List<BillItem> grantList = new ArrayList<>();
        String sql;
        HashMap hm;
        sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.marginValue=0 "
                + " and b.discount=0 "
                + " and b.grossValue!=b.netValue";
        hm = new HashMap();
        hm.put("btp", billType);

        List<BillItem> list = getBillItemFacade().findByJpql(sql, hm);
        return list;

    }

    public List<BillFee> fetchBillFee1(BillType billType) {
//        List<BillItem> grantList = new ArrayList<>();
        String sql;
        HashMap hm;
        sql = "SELECT  b FROM BillFee b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.feeMargin=0 "
                + " and b.feeDiscount=0 "
                + " and b.feeGrossValue!=b.feeValue";
        hm = new HashMap();
        hm.put("btp", billType);

        List<BillFee> list = billFeeFacade.findByJpql(sql, hm);
        return list;

    }

    public List<BillItem> fetchBillItem2(BillType billType) {
//        List<BillItem> grantList = new ArrayList<>();
        String sql;
        HashMap hm;
        sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.marginValue!=0 "
                + " and b.discount=0 "
                + " and (b.grossValue+b.marginValue)!=b.netValue";
        hm = new HashMap();
        hm.put("btp", billType);

        List<BillItem> list = getBillItemFacade().findByJpql(sql, hm);
        return list;

    }

    public List<BillFee> fetchBillFee2(BillType billType) {
//        List<BillItem> grantList = new ArrayList<>();
        String sql;
        HashMap hm;
        sql = "SELECT  b FROM BillFee b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.feeMargin!=0 "
                + " and b.feeDiscount=0 "
                + " and (b.feeGrossValue+b.feeMargin)!=b.feeValue";
        hm = new HashMap();
        hm.put("btp", billType);

        List<BillFee> list = billFeeFacade.findByJpql(sql, hm);
        return list;

    }

    public List<BillItem> fetchBillItem3(BillType billType) {
//        List<BillItem> grantList = new ArrayList<>();
        String sql;
        HashMap hm;
        sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.marginValue!=0 "
                + " and b.discount!=0 "
                + " and (b.grossValue+b.marginValue-b.discount)!=b.netValue";
        hm = new HashMap();
        hm.put("btp", billType);

        List<BillItem> list = getBillItemFacade().findByJpql(sql, hm);
        return list;

    }

    public List<BillFee> fetchBillFee3(BillType billType) {
//        List<BillItem> grantList = new ArrayList<>();
        String sql;
        HashMap hm;
        sql = "SELECT  b FROM BillFee b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.feeMargin!=0 "
                + " and b.feeDiscount!=0 "
                + " and (b.feeGrossValue+b.feeMargin-b.feeDiscount)!=b.feeValue";
        hm = new HashMap();
        hm.put("btp", billType);

        List<BillFee> list = billFeeFacade.findByJpql(sql, hm);
        return list;

    }

    public List<BillItem> createIssueItemTable(PatientEncounter patientEncounter, BillType billType) {
        List<BillItem> grantList = new ArrayList<>();
        String sql;
        HashMap hm;
        sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp"
                + " and (b.bill.billedBill is null ) "
                + " and  b.bill.patientEncounter=:pe"
                + " and (type(b.bill)=:class) ";
        hm = new HashMap();
        hm.put("btp", billType);
        hm.put("class", PreBill.class);
        hm.put("pe", patientEncounter);

        List<BillItem> list = getBillItemFacade().findByJpql(sql, hm);

        hm.clear();
        sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp"
                + " and type(b.bill.billedBill)=:billedClass "
                + " and  b.bill.patientEncounter=:pe"
                + " and (type(b.bill)=:class) ";
        hm = new HashMap();
        hm.put("btp", billType);
        hm.put("class", RefundBill.class);
        hm.put("billedClass", PreBill.class);
        hm.put("pe", patientEncounter);

        List<BillItem> list2 = getBillItemFacade().findByJpql(sql, hm);

        grantList.addAll(list);
        grantList.addAll(list2);

        return grantList;
    }

    public List<Bill> createStoreTable(PatientEncounter patientEncounter) {
        String sql;
        HashMap hm;
        sql = "SELECT  b FROM Bill b"
                + " WHERE b.retired=false "
                + " and b.billType=:btp  "
                + " and  b.patientEncounter=:pe"
                + " and type(b)=:class ";
        hm = new HashMap();
        hm.put("btp", BillType.StoreBhtPre);
        hm.put("class", PreBill.class);
        hm.put("pe", patientEncounter);
        return getBillFacade().findByJpql(sql, hm);

    }

    public List<BillItem> getService(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {

        String sql = "SELECT  b FROM BillItem b"
                + "  WHERE b.retired=false  "
                + " and b.bill.billType=:btp"
                + " and Type(b.item)!=TimedItem  "
                + " and b.bill.patientEncounter=:pe "
                + " and b.bill.cancelled=false"
                + " and b.item.inwardChargeType=:inw ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        hm.put("pe", patientEncounter);
        hm.put("inw", inwardChargeType);
        return getBillItemFacade().findByJpql(sql, hm, TemporalType.TIME);

    }

    public double calculateDoctorAndNurseCharges(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {

        HashMap hm = new HashMap();
        String sql = "SELECT sum(bt.feeValue)"
                + " FROM BillFee bt"
                + " WHERE bt.retired=false"
                + " and type(bt.staff)!=:class "
                + " and bt.fee.feeType=:ftp  "
                + " and (bt.bill.billType=:btp2) "
                + " and bt.bill.patientEncounter IN :pe";
        hm.put("class", Consultant.class);
        hm.put("ftp", FeeType.Staff);
        //     hm.put("btp", BillType.InwardBill);
        hm.put("btp2", BillType.InwardProfessional);
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);

        double val = getBillFeeFacade().findDoubleByJpql(sql, hm, TemporalType.TIME);

        //   System.err.println("NURSE " + val);
        return val;
    }

    public boolean isRoomFilled(Room room) {
        String sql = "select p from PatientRoom p "
                + " where p.retired=false "
                + " and p.roomFacilityCharge.room=:rm "
                + " and p.discharged=false ";
        HashMap hm = new HashMap();
        hm.put("rm", room);
        PatientRoom patientRoom = getPatientRoomFacade().findFirstByJpql(sql, hm);

        if (patientRoom != null) {
            return true;
        } else {
            return false;
        }
    }

    public double getRoomCharge(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        String sql = "select sum(p.calculatedRoomCharge) "
                + " from PatientRoom p "
                + " where p.retired=false "
                + " and p.patientEncounter IN :pe ";
        HashMap hm = new HashMap();
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);

        return getPatientRoomFacade().findDoubleByJpql(sql, hm);
    }

    public double getAdmissionCharge(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        Double total = 0.0;
        List<PatientEncounter> pts = new ArrayList<>();

        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }

        for (PatientEncounter pt : pts) {
            if (pt.getAdmissionType() != null) {
                total = total + pt.getAdmissionType().getAdmissionFee();
            }
        }
        return total;
    }

    public double getMoCharge(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        String sql = "select sum(p.calculatedMoCharge) "
                + " from PatientRoom p "
                + " where p.retired=false "
                + " and p.patientEncounter IN :pe ";
        HashMap hm = new HashMap();
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);

        return getPatientRoomFacade().findDoubleByJpql(sql, hm);
    }

    public double getProfessionalCharge(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        String sql = "select sum(p.feeAdjusted) "
                + " from BillFee p "
                + " where p.retired=false "
                + " and p.bill.patientEncounter IN :pe"
                + " and p.bill.billType=:bilTp ";
        HashMap hm = new HashMap();
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);
        hm.put("bilTp", BillType.InwardProfessional);
        return getBillFeeFacade().findDoubleByJpql(sql, hm);
    }

    public double getNursingCharge(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        String sql = "select sum(p.calculatedNursingCharge) "
                + " from PatientRoom p "
                + " where p.retired=false "
                + " and p.patientEncounter IN :pe ";
        HashMap hm = new HashMap();
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);

        return getPatientRoomFacade().findDoubleByJpql(sql, hm);
    }

    public double getMaintainCharge(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        String sql = "select sum(p.calculatedMaintainCharge) "
                + " from PatientRoom p "
                + " where p.retired=false "
                + " and p.patientEncounter IN :pe ";
        HashMap hm = new HashMap();
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);

        return getPatientRoomFacade().findDoubleByJpql(sql, hm);
    }

    public double getMedicalCareIcuCharge(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        String sql = "select sum(p.calculatedMedicalCareCharge) "
                + " from PatientRoom p "
                + " where p.retired=false "
                + " and p.patientEncounter IN :pe ";
        HashMap hm = new HashMap();
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);

        return getPatientRoomFacade().findDoubleByJpql(sql, hm);
    }

    public double getAdminCharge(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        String sql = "select sum(p.calculatedAdministrationCharge) "
                + " from PatientRoom p "
                + " where p.retired=false "
                + " and p.patientEncounter IN :pe ";
        HashMap hm = new HashMap();
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);

        return getPatientRoomFacade().findDoubleByJpql(sql, hm);
    }

    public double getLinenCharge(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        String sql = "select sum(p.calculatedLinenCharge) "
                + " from PatientRoom p "
                + " where p.retired=false "
                + " and p.patientEncounter IN :pe ";
        HashMap hm = new HashMap();
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);

        return getPatientRoomFacade().findDoubleByJpql(sql, hm);
    }

    /**
     * Fetches all seven PatientRoom charge sums for the given encounters
     * in a single JPQL query instead of seven separate queries.
     */
    public Map<InwardChargeType, Double> getPatientRoomChargeSumsBulk(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        String sql = "SELECT SUM(p.calculatedRoomCharge - p.discountRoomCharge),"
                + " SUM(p.calculatedMoCharge - p.discountMoCharge),"
                + " SUM(p.calculatedNursingCharge - p.discountNursingCharge),"
                + " SUM(p.calculatedMaintainCharge - p.discountMaintainCharge),"
                + " SUM(p.calculatedMedicalCareCharge - p.discountMedicalCareCharge),"
                + " SUM(p.calculatedAdministrationCharge - p.discountAdministrationCharge),"
                + " SUM(p.calculatedLinenCharge - p.discountLinenCharge)"
                + " FROM PatientRoom p"
                + " WHERE p.retired=false"
                + " AND p.patientEncounter IN :pe";
        HashMap hm = new HashMap();
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);

        Map<InwardChargeType, Double> result = new EnumMap<>(InwardChargeType.class);
        List<Object> rows = getPatientRoomFacade().findObjectByJpql(sql, hm, TemporalType.TIMESTAMP);
        if (rows != null && !rows.isEmpty()) {
            Object row = rows.get(0);
            if (row instanceof Object[]) {
                Object[] arr = (Object[]) row;
                result.put(InwardChargeType.RoomCharges,          toDoubleOrZero(arr[0]));
                result.put(InwardChargeType.MOCharges,            toDoubleOrZero(arr[1]));
                result.put(InwardChargeType.NursingCharges,       toDoubleOrZero(arr[2]));
                result.put(InwardChargeType.MaintainCharges,      toDoubleOrZero(arr[3]));
                result.put(InwardChargeType.MedicalCareICU,       toDoubleOrZero(arr[4]));
                result.put(InwardChargeType.AdministrationCharge, toDoubleOrZero(arr[5]));
                result.put(InwardChargeType.LinenCharges,         toDoubleOrZero(arr[6]));
            }
        }
        Map<InwardChargeType, Double> timedItemSums = getTimedItemChargeSumsBulk(patientEncounter, cpts);
        for (Map.Entry<InwardChargeType, Double> entry : timedItemSums.entrySet()) {
            result.merge(entry.getKey(), entry.getValue(), Double::sum);
        }
        return result;
    }

    private double toDoubleOrZero(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    public PatientItemFacade getPatientItemFacade() {
        return patientItemFacade;
    }

    public void setPatientItemFacade(PatientItemFacade patientItemFacade) {
        this.patientItemFacade = patientItemFacade;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    private List<Department> getToDepartmentList(PatientEncounter patientEncounter, Bill forwardRefBill, List<PatientEncounter> cpts) {
        String sql;
        HashMap hm = new HashMap();

        sql = "SELECT  distinct(b.bill.toDepartment) FROM BillItem b "
                + " WHERE   b.retired=false "
                + " and b.bill.billType=:btp ";

        if (forwardRefBill != null) {
            sql += " and b.bill.forwardReferenceBill=:fB";
            hm.put("fB", forwardRefBill);
        }

        sql += " and Type(b.item)!=TimedItem "
                + " and b.bill.toDepartment is not null "
                + " and b.bill.patientEncounter in :pe ";

        hm.put("btp", BillType.InwardBill);
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);

        return getDepartmentFacade().findByJpql(sql, hm, TemporalType.TIME);
    }

    private List<Item> getToDepartmentItems(PatientEncounter patientEncounter, Department department, Bill forwardBill, List<PatientEncounter> cpts) {
        HashMap hm = new HashMap();
        String sql = "SELECT  distinct(b.item) FROM BillItem b "
                + " WHERE b.retired=false"
                + " and b.bill.billType=:btp";

        if (forwardBill != null) {
            sql += " and b.bill.forwardReferenceBill=:fB";
            hm.put("fB", forwardBill);
        }

        sql += " and Type(b.item)!=TimedItem"
                + "  and b.bill.patientEncounter IN :pe"
                + " and b.bill.toDepartment=:dep "
                + "  order by b.item.name ";

        hm.put("btp", BillType.InwardBill);
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);
        hm.put("dep", department);

        return getItemFacade().findByJpql(sql, hm, TemporalType.TIME);
    }

    public boolean checkByBillItem(PatientEncounter patientEncounter, Bill billClass, BillType billType) {
        String sql = "Select b.bill From BillItem b"
                + " where b.bill.retired=false "
                + " and b.retired=false"
                + " and b.bill.cancelled=false "
                + " and (b.refunded is null "
                + " or b.refunded=false) ";

        if (billClass instanceof PreBill) {
            sql += " and b.bill.billedBill is null ";
        }
        sql += " and b.bill.refundedBill is null "
                + " and b.bill.checkedBy is null"
                + " and b.bill.billType=:bt"
                + " and b.bill.patientEncounter=:pe "
                + " and b.bill.netTotal !=0 "
                + " and type(b.bill)=:class";
        HashMap hm = new HashMap();
        hm.put("bt", billType);
        hm.put("pe", patientEncounter);
        hm.put("class", billClass.getClass());
        Bill bill = getBillFacade().findFirstByJpql(sql, hm);

        if (bill != null) {
            return true;
        }

        return false;

    }

    public boolean checkByBillFee(PatientEncounter patientEncounter, Bill billClass, BillType billType) {
        String sql = "Select b.bill From BillFee b"
                + " where b.bill.retired=false"
                + " and b.retired=false "
                + " and b.bill.cancelled=false "
                + " and (b.billItem.refunded is null "
                + " or b.billItem.refunded=false) "
                + " and b.bill.billedBill is null"
                + " and b.bill.checkedBy is null"
                + " and b.bill.billType=:bt"
                + " and b.bill.patientEncounter=:pe "
                + " and b.bill.netTotal !=0 "
                + " and type(b.bill)=:class";
        HashMap hm = new HashMap();
        hm.put("bt", billType);
        hm.put("pe", patientEncounter);
        hm.put("class", billClass.getClass());
        Bill bill = getBillFacade().findFirstByJpql(sql, hm);

        if (bill != null) {
            return true;
        }

        return false;

    }

//    public boolean checkRefundedBill(PatientEncounter patientEncounter, BillType billType) {
//        String sql = "Select b From RefundBill b"
//                + " where b.retired=false "
//                + " and b.cancelled=false "
//                + " and b.billedBill is null "
//                + " and b.checkedBy is null "
//                + " and b.netTotal!=0"
//                + " and b.billType=:bt "
//                + " and b.patientEncounter=:pe ";
//        HashMap hm = new HashMap();
//        hm.put("bt", billType);
//        hm.put("pe", patientEncounter);
//        Bill bill = getBillFacade().findFirstByJpql(sql, hm);
//
//        if (bill != null) {
//            return true;
//        }
//
//        return false;
//
//    }
    private double calBillItemCount(Bill bill, Item item, PatientEncounter patientEncounter, Bill forwardBill, List<PatientEncounter> cpts) {
        HashMap hm = new HashMap();
        String sql = "SELECT  count(b) FROM BillItem b "
                + " WHERE b.retired=false "
                + "  and b.bill.billType=:btp ";

        if (forwardBill != null) {
            sql += " and b.bill.forwardReferenceBill=:fB";
            hm.put("fB", forwardBill);
        }

        sql += " and b.bill.patientEncounter IN :pe "
                + " and b.item=:itm "
                + " and type(b.bill)=:cls";

        hm.put("btp", BillType.InwardBill);
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);
        hm.put("itm", item);
        hm.put("cls", bill.getClass());
        double dbl = getBillItemFacade().countByJpql(sql, hm, TemporalType.TIME);

        return dbl;
    }

    private double calCheckedBillItemCount(Item item, PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT  count(b) FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.bill.patientEncounter=:pe "
                + " and b.item=:itm "
                + " and type(b.bill)=:cls"
                + " and b.bill.checkedBy is not null "
                + " and b.bill.cancelled=false ";

        hm.put("btp", BillType.InwardBill);
        hm.put("pe", patientEncounter);
        hm.put("itm", item);
        hm.put("cls", BilledBill.class);

        double dbl = getBillItemFacade().countByJpql(sql, hm);

        return dbl;
    }

    public List<Bill> fetchOutSideBill(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {

        String sql = "Select i From BilledBill i "
                + " where i.retired=false"
                + "  and i.billType=:btp "
                + " and i.patientEncounter in :pe ";

        HashMap m = new HashMap();
        m.put("btp", BillType.InwardOutSideBill);
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        m.put("pe", pts);
        return getBillFacade().findByJpql(sql, m, TemporalType.DATE);

        //return additionalChargeBill;
    }

    public List<Bill> fetchOutSideBill2(PatientEncounter patientEncounter) {

        String sql = "Select i From Bill i "
                + " where i.retired=false"
                + "  and i.billType=:btp "
                + " and i.patientEncounter=:pe ";

        HashMap m = new HashMap();
        m.put("btp", BillType.InwardOutSideBill);
        m.put("pe", patientEncounter);
        return getBillFacade().findByJpql(sql, m, TemporalType.DATE);

        //return additionalChargeBill;
    }

    public double caltValueFromAdditionalCharge(InwardChargeType inwardChargeType, PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        //   additionalChargeBill = new ArrayList<>();
        String sql = "Select sum(i.netValue)"
                + " From BillItem i "
                + " where i.retired=false "
                + " and i.bill.billType=:btp "
                + "and i.bill.patientEncounter IN :pe "
                + " and i.inwardChargeType=:inwCh ";
        HashMap m = new HashMap();
        m.put("btp", BillType.InwardOutSideBill);
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        m.put("pe", pts);
        m.put("inwCh", inwardChargeType);
        double val = getBillFacade().findDoubleByJpql(sql, m, TemporalType.DATE);

        return val;
    }

    /**
     * OPTIMIZED: Bulk version - fetches all additional charge totals in ONE query
     */
    public Map<InwardChargeType, Double> caltValueFromAdditionalChargeBulk(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        String sql = "SELECT i.inwardChargeType, sum(i.netValue)"
                + " FROM BillItem i "
                + " WHERE i.retired=false "
                + " AND i.bill.billType=:btp "
                + " AND i.bill.patientEncounter IN :pe "
                + " GROUP BY i.inwardChargeType";

        HashMap m = new HashMap();
        m.put("btp", BillType.InwardOutSideBill);
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        m.put("pe", pts);

        List<Object[]> results = getBillItemFacade().findObjectsArrayByJpql(sql, m, TemporalType.DATE);

        Map<InwardChargeType, Double> totalsMap = new HashMap<>();
        if (results != null) {
            for (Object[] row : results) {
                InwardChargeType chargeType = (InwardChargeType) row[0];
                Double total = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                totalsMap.put(chargeType, total);
            }
        }

        return totalsMap;
    }

    public List<PatientEncounter> fetchChildPatientEncounter(PatientEncounter patientEncounter) {
        List<PatientEncounter> cpt = new ArrayList<>();

        HashMap hm = new HashMap();
        String sql = "SELECT pe FROM PatientEncounter pe "
                + " where pe.parentEncounter = :pe "
                + " and pe.retired=false ";
        hm.put("pe", patientEncounter);

        cpt = encounterFacade.findByJpql(sql, hm);

        return cpt;
    }

    public List<PatientItem> fetchPatientItem(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        HashMap hm = new HashMap();
        String sql = "SELECT i FROM PatientItem i "
                + " where Type(i.item)=TimedItem "
                + " and i.retired=false "
                + " and i.patientEncounter in :pe";
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);
        return getPatientItemFacade().findByJpql(sql, hm);
    }

    public List<Bill> fetchPaymentBill(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {

        HashMap hm = new HashMap();
        String sql = "SELECT  b FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billType=:btp "
                + " and b.patientEncounter IN :pe ";
        hm.put("btp", BillType.InwardPaymentBill);
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);
        return getBillFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<Bill> fetchPostFinalPaymentBill(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {

        HashMap hm = new HashMap();
        String sql = "SELECT  b FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billType=:btp "
                + " and b.patientEncounter IN :pe ";
        hm.put("btp", BillType.PostFinalBillInwardPayment);
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        hm.put("pe", pts);
        return getBillFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double calPatientRoomChargeDiscount(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.discountRoomCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientRoomAdminChargeDiscount(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.discountAdministrationCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientRoomMadicalCareChargeDiscount(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.discountMedicalCareCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientRoomLinenChargeDiscount(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.discountLinenCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientMoChargeDiscount(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.discountMoCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientMaintananceChargeDiscount(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.discountMaintainCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientNursingChargeDiscount(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.discountNursingCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientRoomChargeAdjusted(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.adjustedRoomCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientRoomLinenChargeAdjusted(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.ajdustedLinenCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientRoomAdminAdjusted(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.ajdustedAdministrationCharge) "
                + " FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientRoomMadicalCareAdjusted(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.ajdustedMedicalCareCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientMoChargeAdjusted(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.adjustedMoCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientMaintananceChargeAdjusted(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.adjustedMaintainCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientNursingChargeAdjusted(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.ajdustedNursingCharge) "
                + " FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public List<DepartmentBillItems> createDepartmentBillItems(PatientEncounter patientEncounter, Bill forwardRefBill, List<PatientEncounter> cpts) {
        long startTime = System.currentTimeMillis();
        System.out.println("=== createDepartmentBillItems START ===");

        List<DepartmentBillItems> list = new ArrayList<>();

        List<Department> deptList = getToDepartmentList(patientEncounter, forwardRefBill, cpts);
        System.out.println("Found " + deptList.size() + " departments");

        for (Department dep : deptList) {
            // A bill item with a null toDepartment can make the DISTINCT query above
            // return a null element; skip it rather than NPE on dep.getName().
            if (dep == null) {
                continue;
            }
            long deptStartTime = System.currentTimeMillis();
            DepartmentBillItems table = new DepartmentBillItems();

            List<Item> items = getToDepartmentItems(patientEncounter, dep, forwardRefBill, cpts);
            System.out.println("Department: " + dep.getName() + " has " + items.size() + " items");

            for (Item itm : items) {
                long itemStartTime = System.currentTimeMillis();
                double billed = calBillItemCount(new BilledBill(), itm, patientEncounter, forwardRefBill, cpts);
                double cancelld = calBillItemCount(new CancelledBill(), itm, patientEncounter, forwardRefBill, cpts);
                double refund = calBillItemCount(new RefundBill(), itm, patientEncounter, forwardRefBill, cpts);
//                System.err.println("Billed " + billed);
//                System.err.println("Cancelled " + cancelld);
//                System.err.println("Refun " + refund);

                itm.setTransCheckedCount(calCheckedBillItemCount(itm, patientEncounter));
                itm.setTransBillItemCount(billed - (cancelld + refund));

                long itemTime = System.currentTimeMillis() - itemStartTime;
                if (itemTime > 100) {
                    System.out.println("  SLOW Item: " + itm.getName() + " took " + itemTime + "ms");
                }
            }

            table.setDepartment(dep);
            table.setItems(items);

            list.add(table);

            long deptTime = System.currentTimeMillis() - deptStartTime;
            System.out.println("Department " + dep.getName() + " completed in " + deptTime + "ms");
        }

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("=== createDepartmentBillItems END: Total time = " + totalTime + "ms ===");
//        calServiceTot(departmentBillItems);
        return list;

    }

    /**
     * OPTIMIZED VERSION: Fetches all bill item counts in bulk queries instead of N+1 queries
     * This reduces 88 database round-trips to just 4 round-trips
     * Performance: 30 seconds -> <2 seconds on high-latency connections
     */
    public List<DepartmentBillItems> createDepartmentBillItemsOptimized(PatientEncounter patientEncounter, Bill forwardRefBill, List<PatientEncounter> cpts) {
        long startTime = System.currentTimeMillis();
        System.out.println("=== createDepartmentBillItemsOptimized START ===");

        List<DepartmentBillItems> list = new ArrayList<>();
        List<Department> deptList = getToDepartmentList(patientEncounter, forwardRefBill, cpts);
        System.out.println("Found " + deptList.size() + " departments");

        // Build list of patient encounters
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }

        for (Department dep : deptList) {
            // A bill item with a null toDepartment can make the DISTINCT query above
            // return a null element; skip it rather than NPE on dep.getName().
            if (dep == null) {
                continue;
            }
            long deptStartTime = System.currentTimeMillis();
            DepartmentBillItems table = new DepartmentBillItems();

            List<Item> items = getToDepartmentItems(patientEncounter, dep, forwardRefBill, cpts);
            System.out.println("Department: " + dep.getName() + " has " + items.size() + " items");

            if (!items.isEmpty()) {
                // BULK QUERY 1: Get all billed counts for all items in one query
                Map<Long, Long> billedCounts = getBulkBillItemCounts(items, pts, forwardRefBill, BilledBill.class);

                // BULK QUERY 2: Get all cancelled counts
                Map<Long, Long> cancelledCounts = getBulkBillItemCounts(items, pts, forwardRefBill, CancelledBill.class);

                // BULK QUERY 3: Get all refund counts
                Map<Long, Long> refundCounts = getBulkBillItemCounts(items, pts, forwardRefBill, RefundBill.class);

                // BULK QUERY 4: Get all checked counts
                Map<Long, Long> checkedCounts = getBulkCheckedBillItemCounts(items, patientEncounter);

                // BULK QUERY 5: Get Gross/Discount/Margin/Net/VAT value breakdown
                Map<Long, double[]> valueBreakdown = getBulkBillItemValueBreakdown(items, pts);

                // Apply the counts to items (no more database queries!)
                for (Item itm : items) {
                    long billed = billedCounts.getOrDefault(itm.getId(), 0L);
                    long cancelled = cancelledCounts.getOrDefault(itm.getId(), 0L);
                    long refund = refundCounts.getOrDefault(itm.getId(), 0L);
                    long checked = checkedCounts.getOrDefault(itm.getId(), 0L);
                    double[] values = valueBreakdown.getOrDefault(itm.getId(), new double[5]);

                    itm.setTransCheckedCount(checked);
                    itm.setTransBillItemCount(billed - (cancelled + refund));
                    itm.setTransGrossValue(values[0]);
                    itm.setTransDiscount(values[1]);
                    itm.setTransMarginValue(values[2]);
                    itm.setTransNetValue(values[3]);
                    itm.setTransVat(values[4]);
                }
            }

            table.setDepartment(dep);
            table.setItems(items);
            list.add(table);

            long deptTime = System.currentTimeMillis() - deptStartTime;
            System.out.println("Department " + dep.getName() + " completed in " + deptTime + "ms");
        }

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("=== createDepartmentBillItemsOptimized END: Total time = " + totalTime + "ms ===");
        return list;
    }

    /**
     * Bulk query to get bill item counts for multiple items at once
     * Returns a map of itemId -> count
     */
    private Map<Long, Long> getBulkBillItemCounts(List<Item> items, List<PatientEncounter> pts, Bill forwardBill, Class billClass) {
        if (items == null || items.isEmpty()) {
            return new HashMap<>();
        }

        HashMap hm = new HashMap();
        String sql = "SELECT b.item.id, count(b) FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.bill.patientEncounter IN :pe "
                + " and b.item IN :items "
                + " and type(b.bill)=:cls";

        if (forwardBill != null) {
            sql += " and b.bill.forwardReferenceBill=:fB";
            hm.put("fB", forwardBill);
        }

        sql += " GROUP BY b.item.id";

        hm.put("btp", BillType.InwardBill);
        hm.put("pe", pts);
        hm.put("items", items);
        hm.put("cls", billClass);

        List<Object[]> results = getBillItemFacade().findObjectsArrayByJpql(sql, hm, TemporalType.TIME);

        Map<Long, Long> countMap = new HashMap<>();
        if (results != null) {
            for (Object[] row : results) {
                Long itemId = (Long) row[0];
                Long count = (Long) row[1];
                countMap.put(itemId, count);
            }
        }

        return countMap;
    }

    /**
     * Bulk query to get the Gross/Discount/Margin/Net/VAT value breakdown for
     * multiple items at once. Returns a map of itemId -> {gross, discount, margin, net, vat}.
     */
    private Map<Long, double[]> getBulkBillItemValueBreakdown(List<Item> items, List<PatientEncounter> pts) {
        if (items == null || items.isEmpty()) {
            return new HashMap<>();
        }

        HashMap hm = new HashMap();
        String sql = "SELECT b.item.id, sum(b.grossValue), sum(b.discount), sum(b.marginValue), sum(b.netValue), sum(b.vat) "
                + " FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.bill.patientEncounter IN :pe "
                + " and b.item IN :items "
                + " GROUP BY b.item.id";

        hm.put("btp", BillType.InwardBill);
        hm.put("pe", pts);
        hm.put("items", items);

        List<Object[]> results = getBillItemFacade().findObjectsArrayByJpql(sql, hm, TemporalType.TIME);

        Map<Long, double[]> valueMap = new HashMap<>();
        if (results != null) {
            for (Object[] row : results) {
                Long itemId = (Long) row[0];
                double gross = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                double discount = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
                double margin = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
                double net = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
                double vat = row[5] != null ? ((Number) row[5]).doubleValue() : 0.0;
                valueMap.put(itemId, new double[]{gross, discount, margin, net, vat});
            }
        }

        return valueMap;
    }

    /**
     * Bulk query to get checked bill item counts for multiple items at once
     */
    private Map<Long, Long> getBulkCheckedBillItemCounts(List<Item> items, PatientEncounter patientEncounter) {
        if (items == null || items.isEmpty()) {
            return new HashMap<>();
        }

        HashMap hm = new HashMap();
        String sql = "SELECT b.item.id, count(b) FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.bill.patientEncounter=:pe "
                + " and b.item IN :items "
                + " and type(b.bill)=:cls "
                + " and b.bill.checkedBy is not null "
                + " and b.bill.cancelled=false "
                + " GROUP BY b.item.id";

        hm.put("btp", BillType.InwardBill);
        hm.put("pe", patientEncounter);
        hm.put("items", items);
        hm.put("cls", BilledBill.class);

        List<Object[]> results = getBillItemFacade().findObjectsArrayByJpql(sql, hm, TemporalType.TIMESTAMP);

        Map<Long, Long> countMap = new HashMap<>();
        if (results != null) {
            for (Object[] row : results) {
                Long itemId = (Long) row[0];
                Long count = (Long) row[1];
                countMap.put(itemId, count);
            }
        }

        return countMap;
    }

    /**
     * Bulk query returning the most recently checked inward BillItem for each
     * item of the given patient encounter. Used by the Service Details tab to
     * display "Checked By" / "Checked At" per aggregated item row without adding
     * transient fields to the shared Item entity.
     *
     * @return map of itemId -> latest checked BillItem
     */
    public Map<Long, BillItem> getLatestCheckedBillItemsByItem(PatientEncounter patientEncounter) {
        Map<Long, BillItem> map = new HashMap<>();
        if (patientEncounter == null) {
            return map;
        }

        HashMap hm = new HashMap();
        String sql = "SELECT b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.bill.patientEncounter=:pe "
                + " and type(b.bill)=:cls "
                + " and b.bill.checkedBy is not null "
                + " and b.bill.checkeAt is not null "
                + " and b.bill.cancelled=false "
                + " order by b.bill.checkeAt desc ";

        hm.put("btp", BillType.InwardBill);
        hm.put("pe", patientEncounter);
        hm.put("cls", BilledBill.class);

        List<BillItem> results = getBillItemFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
        if (results != null) {
            // Ordered by checkeAt desc, so the first row seen per item is the latest.
            for (BillItem bi : results) {
                if (bi.getItem() != null && bi.getItem().getId() != null
                        && !map.containsKey(bi.getItem().getId())) {
                    map.put(bi.getItem().getId(), bi);
                }
            }
        }
        return map;
    }

    public Fee getStaffFeeForInward(WebUser webUser) {
        String sql = "Select f From InwardFee f "
                + " where f.retired=false "
                + " and f.feeType=:st ";

        HashMap hm = new HashMap();
        hm.put("st", FeeType.Staff);

        Fee fee = getFeeFacade().findFirstByJpql(sql, hm);
        if (fee == null) {
            fee = new InwardFee();
            fee.setCreatedAt(new Date());
            fee.setCreater(webUser);
            fee.setFeeType(FeeType.Staff);

            if (fee.getId() == null) {
                getFeeFacade().create(fee);
            }
        }

        return fee;

    }

    public Bill fetchFinalBill(PatientEncounter patientEncounter) {
        String sql = "Select b From BilledBill b "
                + " where b.retired=false "
                + " and b.cancelled=false "
                + " and b.billType=:btp "
                + " and b.confirmedFinalBill=true "
                + " and b.patientEncounter=:pe"
                + " order by b.id desc";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardFinalBill);
        hm.put("pe", patientEncounter);

        return getBillFacade().findFirstByJpql(sql, hm);
    }

    public List<Bill> fetchFinalBills() {
        String sql = "Select b From BilledBill b "
                + " where b.retired=false "
                + " and b.cancelled=false "
                + " and b.billType=:btp "
                + " and b.confirmedFinalBill=true "
                + " and b.patientEncounter.paymentFinalized=true";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardFinalBill);

        return getBillFacade().findByJpql(sql, hm);
    }

    public void updateFinalFill(PatientEncounter patientEncounter) {
        Bill b = fetchFinalBill(patientEncounter);
        if (b == null) {
            return;
        }

        double paid = getPaidValue(patientEncounter);
//        System.err.println("NET " + b.getNetTotal());
//        System.err.println("PAID " + paid);

        b.setPaidAmount(paid);
        getBillFacade().edit(b);

    }

    public double getPaidValue(PatientEncounter patientEncounter) {

        HashMap hm = new HashMap();
        String sql = "SELECT  sum(b.netTotal) FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billType=:btp "
                + " and b.patientEncounter=:pe ";
        hm.put("btp", BillType.InwardPaymentBill);
        hm.put("pe", patientEncounter);
       
        double dbl = getBillFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

        return dbl;

    }

    public double getPaidByPatientValue(PatientEncounter patientEncounter) {

        HashMap hm = new HashMap();
        String sql = "SELECT  sum(b.netTotal) FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billType=:btp "
                + " and b.paymentMethod !=:pm "
                + " and b.patientEncounter=:pe ";
        hm.put("btp", BillType.InwardPaymentBill);
        hm.put("pm", PaymentMethod.Credit);
        hm.put("pe", patientEncounter);

        double dbl = getBillFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

        return dbl;

    }

    public double getPaidByCompanyValue(PatientEncounter patientEncounter) {

        HashMap hm = new HashMap();
        String sql = "SELECT  sum(b.netTotal) FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billTypeAtomic in :bts "
                + " and b.patientEncounter=:pe ";
        List<BillTypeAtomic> bts = new ArrayList<>();
        bts.add(BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED);
        hm.put("bts", bts);
        hm.put("pe", patientEncounter);

        double dbl = getBillFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

        return dbl;

    }

    public void updateCreditDetail(PatientEncounter patientEncounter, double netTotal) {
        if (patientEncounter == null) {
            return;
        }

        if (patientEncounter.getCreditLimit() == 0) {
            patientEncounter.setCreditUsedAmount(netTotal);
            patientEncounterFacade.edit(patientEncounter);
            return;
        }

        if (patientEncounter.getCreditLimit() <= netTotal) {
            patientEncounter.setCreditUsedAmount(patientEncounter.getCreditLimit());
        } else {
            patientEncounter.setCreditUsedAmount(netTotal);
        }

        patientEncounterFacade.edit(patientEncounter);
    }

    public PatientRoom savePatientRoom(PatientRoom patientRoom, PatientRoom previousRoom, RoomFacilityCharge newRoomFacilityCharge, PatientEncounter patientEncounter, Date admittedAt, WebUser webUser) {
//     patientRoom.setCurrentLinenCharge(patientRoom.getRoomFacilityCharge().getLinenCharge());
        if (patientRoom == null) {
            return null;
        }

        if (sessionController.getApplicationPreference().isInwardMoChargeCalculateInitialTime()) {
            patientRoom.setCurrentMoChargeForAfterDuration(newRoomFacilityCharge.getMoChargeForAfterDuration());
        }

        if (newRoomFacilityCharge.getMaintananceCharge() != null) {
            patientRoom.setCurrentMaintananceCharge(newRoomFacilityCharge.getMaintananceCharge());
        }
        if (newRoomFacilityCharge.getMoCharge() != null) {
            patientRoom.setCurrentMoCharge(newRoomFacilityCharge.getMoCharge());
        }
        if (newRoomFacilityCharge.getNursingCharge() != null) {
            patientRoom.setCurrentNursingCharge(newRoomFacilityCharge.getNursingCharge());
        }
        if (newRoomFacilityCharge.getRoomCharge() != null) {
            patientRoom.setCurrentRoomCharge(newRoomFacilityCharge.getRoomCharge());
        }
        if (newRoomFacilityCharge.getLinenCharge() != null) {
            patientRoom.setCurrentLinenCharge(newRoomFacilityCharge.getLinenCharge());
        }
        patientRoom.setCurrentMedicalCareCharge(newRoomFacilityCharge.getMedicalCareCharge());
        patientRoom.setCurrentAdministrationCharge(newRoomFacilityCharge.getAdminstrationCharge());

        patientRoom.setPreviousRoom(previousRoom);
        patientRoom.setCreatedAt(Calendar.getInstance().getTime());
        patientRoom.setCreater(webUser);
        patientRoom.setAdmittedAt(admittedAt);
        patientRoom.setAddmittedBy(webUser);
        patientRoom.setPatientEncounter(patientEncounter);
        patientRoom.setRoomFacilityCharge(newRoomFacilityCharge);

//        if (patientEncounter.getAdmissionType().isRoomChargesAllowed() == false) {
//            patientRoom.setDischarged(true);
//        }
        if (patientRoom.getId() == null || patientRoom.getId() == 0) {
            getPatientRoomFacade().create(patientRoom);
        } else {
            getPatientRoomFacade().edit(patientRoom);
        }
        snapshotTimedItems(patientRoom, newRoomFacilityCharge);

        return patientRoom;
    }

    /**
     * Same as the 6-arg {@link #savePatientRoom(PatientRoom, PatientRoom, RoomFacilityCharge, PatientEncounter, Date, WebUser)}
     * but additionally sets {@code PatientRoom.admitted}, for callers (e.g. the "simultaneous"
     * admit-and-room-assign flow) that must mark the room as occupied immediately rather than
     * leaving it pending a separate handover/accept step.
     */
    public PatientRoom savePatientRoom(PatientRoom patientRoom, PatientRoom previousRoom, RoomFacilityCharge newRoomFacilityCharge, PatientEncounter patientEncounter, Date admittedAt, WebUser webUser, boolean admitted) {
        if (patientRoom != null) {
            patientRoom.setAdmitted(admitted);
        }
        return savePatientRoom(patientRoom, previousRoom, newRoomFacilityCharge, patientEncounter, admittedAt, webUser);
    }

    public PatientRoom admitPatientRoom(PatientRoom patientRoom, RoomFacilityCharge newRoomFacilityCharge, Date admittedAt, WebUser webUser) {
//     patientRoom.setCurrentLinenCharge(patientRoom.getRoomFacilityCharge().getLinenCharge());
        if (patientRoom == null) {
            return null;
        }

        if (newRoomFacilityCharge == null) {
            return null;
        }

        if (sessionController.getApplicationPreference().isInwardMoChargeCalculateInitialTime()) {
            patientRoom.setCurrentMoChargeForAfterDuration(newRoomFacilityCharge.getMoChargeForAfterDuration());
        }

        if (newRoomFacilityCharge.getMaintananceCharge() != null) {
            patientRoom.setCurrentMaintananceCharge(newRoomFacilityCharge.getMaintananceCharge());
        }
        if (newRoomFacilityCharge.getMoCharge() != null) {
            patientRoom.setCurrentMoCharge(newRoomFacilityCharge.getMoCharge());
        }
        if (newRoomFacilityCharge.getNursingCharge() != null) {
            patientRoom.setCurrentNursingCharge(newRoomFacilityCharge.getNursingCharge());
        }
        if (newRoomFacilityCharge.getRoomCharge() != null) {
            patientRoom.setCurrentRoomCharge(newRoomFacilityCharge.getRoomCharge());
        }
        if (newRoomFacilityCharge.getLinenCharge() != null) {
            patientRoom.setCurrentLinenCharge(newRoomFacilityCharge.getLinenCharge());
        }
        patientRoom.setCurrentMedicalCareCharge(newRoomFacilityCharge.getMedicalCareCharge());
        patientRoom.setCurrentAdministrationCharge(newRoomFacilityCharge.getAdminstrationCharge());

        patientRoom.setAdmitted(true);
        patientRoom.setAdmittedAt(admittedAt);
        patientRoom.setAddmittedBy(webUser);
        patientRoom.setRoomFacilityCharge(newRoomFacilityCharge);

        if (patientRoom.getId() == null || patientRoom.getId() == 0) {
            getPatientRoomFacade().create(patientRoom);
        } else {
            getPatientRoomFacade().edit(patientRoom);
        }
        snapshotTimedItems(patientRoom, newRoomFacilityCharge);

        return patientRoom;
    }

    public PatientRoom savePatientRoom(PatientRoom patientRoom, PatientEncounter patientEncounter, WebUser webUser) {
        if (patientRoom == null) {
            return null;
        }

        if (patientEncounter == null) {
            return null;
        }

        patientRoom.setCreatedAt(new Date());
        patientRoom.setCreater(webUser);
        patientRoom.setAdmitted(false);
        patientRoom.setPatientEncounter(patientEncounter);

        if (patientRoom.getId() == null || patientRoom.getId() == 0) {
            getPatientRoomFacade().create(patientRoom);
        } else {
            getPatientRoomFacade().edit(patientRoom);
        }
        return patientRoom;
    }

    public PatientRoom savePatientRoom(PatientRoom patientRoom, RoomFacilityCharge newRoomFacilityCharge, PatientEncounter patientEncounter, Date admittedAt, WebUser webUser) {

//     patientRoom.setCurrentLinenCharge(patientRoom.getRoomFacilityCharge().getLinenCharge());
        if (patientRoom == null) {
            return null;
        }
        if (newRoomFacilityCharge == null) {
            return null;
        }

        patientRoom.setCurrentMaintananceCharge(newRoomFacilityCharge.getMaintananceCharge());
        patientRoom.setCurrentMoCharge(newRoomFacilityCharge.getMoCharge());

        if (sessionController.getApplicationPreference().isInwardMoChargeCalculateInitialTime()) {
            patientRoom.setCurrentMoChargeForAfterDuration(newRoomFacilityCharge.getMoChargeForAfterDuration());
        }

        patientRoom.setCurrentNursingCharge(newRoomFacilityCharge.getNursingCharge());
        patientRoom.setCurrentRoomCharge(newRoomFacilityCharge.getRoomCharge());
        patientRoom.setCurrentLinenCharge(newRoomFacilityCharge.getLinenCharge());
        patientRoom.setCurrentMedicalCareCharge(newRoomFacilityCharge.getMedicalCareCharge());
        patientRoom.setCurrentAdministrationCharge(newRoomFacilityCharge.getAdminstrationCharge());

        patientRoom.setCreatedAt(Calendar.getInstance().getTime());
        patientRoom.setCreater(webUser);
        patientRoom.setAdmittedAt(admittedAt);
        patientRoom.setAddmittedBy(webUser);
        patientRoom.setPatientEncounter(patientEncounter);
        patientRoom.setRoomFacilityCharge(newRoomFacilityCharge);

//        if (patientEncounter.getAdmissionType().isRoomChargesAllowed() == false) {
//            patientRoom.setDischarged(true);
//        }
        if (patientRoom.getId() == null || patientRoom.getId() == 0) {
            getPatientRoomFacade().create(patientRoom);
        } else {
            getPatientRoomFacade().edit(patientRoom);
        }
        snapshotTimedItems(patientRoom, newRoomFacilityCharge);

        return patientRoom;
    }

    public void snapshotTimedItems(PatientRoom patientRoom, RoomFacilityCharge rfc) {
        if (patientRoom == null || rfc == null) {
            return;
        }
        String jpql = "SELECT r FROM RoomFacilityTimedItem r WHERE r.roomFacilityCharge = :rfc AND r.retired = false";
        HashMap<String, Object> params = new HashMap<>();
        params.put("rfc", rfc);
        List<RoomFacilityTimedItem> links = roomFacilityTimedItemFacade.findByJpql(jpql, params);
        if (links == null || links.isEmpty()) {
            return;
        }
        for (RoomFacilityTimedItem link : links) {
            if (link.getTimedItem() == null) {
                continue;
            }
            HashMap<String, Object> existsParams = new HashMap<>();
            existsParams.put("pr", patientRoom);
            existsParams.put("ti", link.getTimedItem());
            PatientRoomTimedItemCharge existing = patientRoomTimedItemChargeFacade.findFirstByJpql(
                    "SELECT t FROM PatientRoomTimedItemCharge t WHERE t.patientRoom = :pr AND t.timedItem = :ti",
                    existsParams);
            if (existing != null) {
                continue;
            }
            PatientRoomTimedItemCharge snapshot = new PatientRoomTimedItemCharge();
            snapshot.setPatientRoom(patientRoom);
            snapshot.setTimedItem(link.getTimedItem());
            patientRoomTimedItemChargeFacade.create(snapshot);
        }
    }

    public List<PatientRoomTimedItemCharge> fetchTimedItemCharges(PatientRoom patientRoom) {
        if (patientRoom == null || patientRoom.getId() == null) {
            return new ArrayList<>();
        }
        String jpql = "SELECT t FROM PatientRoomTimedItemCharge t WHERE t.patientRoom = :pr";
        HashMap<String, Object> params = new HashMap<>();
        params.put("pr", patientRoom);
        List result = patientRoomTimedItemChargeFacade.findByJpql(jpql, params);
        return result != null ? result : new ArrayList<>();
    }

    public Map<InwardChargeType, Double> getTimedItemChargeSumsBulk(PatientEncounter patientEncounter, List<PatientEncounter> cpts) {
        Map<InwardChargeType, Double> result = new EnumMap<>(InwardChargeType.class);
        List<PatientEncounter> pts = new ArrayList<>();
        pts.add(patientEncounter);
        if (cpts != null && !cpts.isEmpty()) {
            pts.addAll(cpts);
        }
        String jpql = "SELECT t.timedItem.inwardChargeType, SUM(t.calculatedCharge - t.discountCharge)"
                + " FROM PatientRoomTimedItemCharge t"
                + " WHERE t.patientRoom.retired = false"
                + " AND t.patientRoom.patientEncounter IN :pe"
                + " GROUP BY t.timedItem.inwardChargeType";
        HashMap<String, Object> params = new HashMap<>();
        params.put("pe", pts);
        List<Object> rows = patientRoomTimedItemChargeFacade.findObjectByJpql(jpql, params, TemporalType.TIMESTAMP);
        if (rows != null) {
            for (Object row : rows) {
                if (row instanceof Object[]) {
                    Object[] arr = (Object[]) row;
                    if (arr[0] instanceof InwardChargeType && arr[1] instanceof Number) {
                        InwardChargeType ict = (InwardChargeType) arr[0];
                        double val = ((Number) arr[1]).doubleValue();
                        result.merge(ict, val, Double::sum);
                    }
                }
            }
        }
        return result;
    }

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    public PatientRoomFacade getPatientRoomFacade() {
        return patientRoomFacade;
    }

    public void setPatientRoomFacade(PatientRoomFacade patientRoomFacade) {
        this.patientRoomFacade = patientRoomFacade;
    }

    public RoomFacade getRoomFacade() {
        return roomFacade;
    }

    public void setRoomFacade(RoomFacade roomFacade) {
        this.roomFacade = roomFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public FeeFacade getFeeFacade() {
        return feeFacade;
    }

    public void setFeeFacade(FeeFacade feeFacade) {
        this.feeFacade = feeFacade;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public InwardReportControllerBht getInwardReportControllerBht() {
        return inwardReportControllerBht;
    }

    public void setInwardReportControllerBht(InwardReportControllerBht inwardReportControllerBht) {
        this.inwardReportControllerBht = inwardReportControllerBht;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public List<PatientRoom> getPatientRooms(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT pr FROM PatientRoom pr where pr.retired=false"
                + " and pr.patientEncounter=:pe order by pr.createdAt";
        hm.put("pe", patientEncounter);
        List<PatientRoom> tmp = getPatientRoomFacade().findByJpql(sql, hm);

        if (tmp == null) {
            tmp = new ArrayList<>();
        }

        return tmp;
    }

    public String getBhtText(AdmissionType admissionType) {
        boolean institutionBasedBht = configOptionApplicationController
                .getBooleanValueByKey("Generate Separate BHT Number Series for Each Institution");
        Institution currentInstitution = getSessionController().getInstitution();

        AdmissionNumber an = billNumberGenerator.fetchNextAdmissionNumber(
                admissionType, currentInstitution, institutionBasedBht);

        Long temp = an.getLastAdmissionNumber();
        lastGeneratedBhtLong = temp;

        return formatBhtText(admissionType, temp, institutionBasedBht, currentInstitution);
    }

    public String getBhtTextPreview(AdmissionType admissionType) {
        boolean institutionBasedBht = configOptionApplicationController
                .getBooleanValueByKey("Generate Separate BHT Number Series for Each Institution");
        Institution currentInstitution = getSessionController().getInstitution();

        Long temp = billNumberGenerator.peekNextAdmissionNumber(
                admissionType, currentInstitution, institutionBasedBht);

        return formatBhtText(admissionType, temp, institutionBasedBht, currentInstitution);
    }

    private String formatBhtText(AdmissionType admissionType, Long temp, boolean institutionBasedBht, Institution currentInstitution) {
        String bhtText;

        if (getSessionController().getApplicationPreference().isBhtNumberWithOutAdmissionType()) {
            bhtText = "BHT";
        } else {
            bhtText = admissionType.getCode().trim();
        }

        if (getSessionController().getApplicationPreference().isBhtNumberWithYear()) {
            Calendar c = Calendar.getInstance();
            bhtText = bhtText + "/" + c.get(Calendar.YEAR);
        }

        bhtText += "/" + Long.toString(temp);

        if (institutionBasedBht && currentInstitution != null
                && currentInstitution.getInstitutionCode() != null
                && !currentInstitution.getInstitutionCode().trim().isEmpty()) {
            bhtText = currentInstitution.getInstitutionCode().trim() + "/" + bhtText;
        }

        return bhtText;
    }

    public Fee createAdditionalFee() {
        String sql = "Select f from Fee f where f.retired=false and f.feeType=:nm";
        HashMap hm = new HashMap();
        hm.put("nm", FeeType.Additional);
        List<Fee> fee = getFeeFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
        Fee additional;

        if (fee.isEmpty()) {
            additional = new Fee();
            additional.setName("Additional");
            additional.setFeeType(FeeType.Additional);
            additional.setCreatedAt(new Date());
            if (additional.getId() == null) {
                getFeeFacade().create(additional);
            }
            return additional;
        } else {
            return fee.get(0);
        }
    }

    public void setBillFeeMargin(BillFee billFee, Item item, PriceMatrix priceMatrix) {
        double margin = 0;

        if (billFee == null || item.isMarginNotAllowed()
                || billFee.getFee() == null
                || Boolean.FALSE.equals(billFee.getFee().getMarginAllowed())) {
            // Fix C: clear stale discount and recompute net so calTotals() sees correct feeValue
            if (billFee != null) {
                billFee.setFeeDiscount(0.0);
                billFee.setFeeUnitDiscount(0.0);
                double gross = billFee.getFeeGrossValue() != null ? billFee.getFeeGrossValue() : 0.0;
                billFee.setFeeValue(gross);
            }
            return;
        }

        if (billFee.getFee().getFeeType() != FeeType.Staff && priceMatrix != null) {
            margin = (billFee.getFeeGrossValue() * priceMatrix.getMargin()) / 100;
            billFee.setFeeMargin(margin);
            billFeeFacade.edit(billFee);
        }

        // Fix A: reset discount before recalculating so stale values don't survive
        billFee.setFeeDiscount(0.0);
        billFee.setFeeUnitDiscount(0.0);

        double net = (billFee.getFeeGrossValue() + margin) - billFee.getFeeDiscount();

        billFee.setFeeValue(net);
    }

    public void setBillFeeMargin(BillFee billFee, Item item, PriceMatrix priceMatrix, PatientEncounter patientEncounter) {
        if (billFee == null || item == null || billFee.getFee() == null) {
            return;
        }

        double qty = (billFee.getBillItem() != null && billFee.getBillItem().getQty() != null && billFee.getBillItem().getQty() > 0)
                ? billFee.getBillItem().getQty() : 1.0;
        double unitGross = (billFee.getFeeUnitGrossValue() != null)
                ? billFee.getFeeUnitGrossValue()
                : (billFee.getFeeGrossValue() != null ? billFee.getFeeGrossValue() / qty : 0.0);

        // Margin and discount are independent — a fee that disallows margin can still receive a discount.
        boolean marginEligible = !item.isMarginNotAllowed()
                && !Boolean.FALSE.equals(billFee.getFee().getMarginAllowed())
                && billFee.getFee().getFeeType() != FeeType.Staff
                && patientEncounter != null
                && patientEncounter.getAdmissionType() != null
                && patientEncounter.getAdmissionType().isAllowToCalculateMargin();

        double unitMargin = 0;
        if (marginEligible) {
            // Try CC-specific priceMatrix first; fall back to the caller-provided one.
            PriceMatrix effectivePriceMatrix = priceMatrix;
            com.divudi.core.entity.Institution cc = resolveSingleCreditCompany(patientEncounter);
            if (cc != null && billFee.getBillItem() != null) {
                BillItem bi = billFee.getBillItem();
                double svcValue = bi.getRate() != 0.0 ? Math.abs(bi.getRate()) : unitGross;
                Department dept = item.getDepartment() != null ? item.getDepartment()
                        : (bi.getBill() != null ? bi.getBill().getDepartment() : null);
                PriceMatrix ccMatrix = priceMatrixController.fetchInwardMargin(bi, svcValue, dept,
                        patientEncounter.getPaymentMethod(), cc, patientEncounter.getAdmissionType(), resolveCurrentRoomCategory(patientEncounter));
                if (ccMatrix != null) {
                    effectivePriceMatrix = ccMatrix;
                }
            }
            if (effectivePriceMatrix != null) {
                unitMargin = (unitGross * effectivePriceMatrix.getMargin()) / 100;
                billFee.setFeeUnitMargin(unitMargin);
                billFee.setFeeMargin(unitMargin * qty);
                billFeeFacade.edit(billFee);
            }
        }

        // Reset discount fields before applying so stale values don't survive.
        billFee.setFeeUnitDiscount(0.0);
        billFee.setFeeDiscount(0.0);

        // Apply discount (has its own eligibility guards inside).
        if (patientEncounter != null && patientEncounter.getAdmissionType() != null) {
            applyInwardDiscountToBillFee(billFee, item, patientEncounter);
        } else {
            billFee.setFeeUnitValue(unitGross);
            billFee.setFeeValue(unitGross * qty);
            return;
        }

        double unitDiscount = (billFee.getFeeUnitDiscount() != null) ? billFee.getFeeUnitDiscount() : 0.0;
        double unitNet = (unitGross + unitMargin) - unitDiscount;
        billFee.setFeeUnitValue(unitNet);
        billFee.setFeeValue(unitNet * qty);
    }

    private com.divudi.core.entity.Institution resolveSingleCreditCompany(PatientEncounter encounter) {
        if (encounter == null) {
            return null;
        }
        String jpql = "select e from EncounterCreditCompany e where e.retired = false and e.patientEncounter = :enc";
        java.util.HashMap<String, Object> hm = new java.util.HashMap<>();
        hm.put("enc", encounter);
        List<com.divudi.core.entity.EncounterCreditCompany> list = encounterCreditCompanyFacade.findByJpql(jpql, hm, 2);
        if (list != null && list.size() == 1) {
            return list.get(0).getInstitution();
        }
        return null;
    }

    /**
     * Room category of the encounter's current room, or null when the patient is
     * not in a room (or the room has no facility charge / category). Drives the
     * room-category dimension of the inward margin matrix (issue #21977); null
     * means "wildcard row only", preserving legacy behaviour.
     */
    private com.divudi.core.entity.inward.RoomCategory resolveCurrentRoomCategory(PatientEncounter encounter) {
        if (encounter == null
                || encounter.getCurrentPatientRoom() == null
                || encounter.getCurrentPatientRoom().getRoomFacilityCharge() == null) {
            return null;
        }
        return encounter.getCurrentPatientRoom().getRoomFacilityCharge().getRoomCategory();
    }

    /**
     * Apply the Inward Discount Matrix discount to a BillFee.
     *
     * Runs on hospital-portion fees only (skips Staff fees). Skipped when the
     * item does not allow discount or the fee does not allow discount.
     * The discount scheme is taken from the admission/encounter itself
     * (set at admission time). BHT type is the encounter's paymentMethod.
     * When exactly one credit company is on the encounter, a credit-company-
     * specific matrix row is tried first before falling back to generic rows.
     * When no matrix row matches the discount is 0, so existing behaviour is
     * preserved for sites that have not configured the matrix.
     */
    public void applyInwardDiscountToBillFee(BillFee billFee, Item item, PatientEncounter patientEncounter) {
        if (billFee == null || item == null || patientEncounter == null) {
            return;
        }
        if (billFee.getFee() == null
                || billFee.getFee().getFeeType() == FeeType.Staff
                || !Boolean.TRUE.equals(item.isDiscountAllowed())
                || !billFee.getFee().isDiscountAllowed()) {
            billFee.setFeeUnitDiscount(0.0);
            billFee.setFeeDiscount(0.0);
            return;
        }
        Department matrixDept = item.getDepartment();
        if (matrixDept == null && billFee.getBillItem() != null && billFee.getBillItem().getBill() != null) {
            matrixDept = billFee.getBillItem().getBill().getDepartment();
        }
        com.divudi.core.entity.Institution creditCompany = resolveSingleCreditCompany(patientEncounter);
        double pct = priceMatrixController.getInwardDiscountPercent(
                patientEncounter.getPaymentMethod(),
                patientEncounter.getPaymentScheme(),
                patientEncounter.getAdmissionType(),
                matrixDept,
                item,
                creditCompany);
        double qty = (billFee.getBillItem() != null && billFee.getBillItem().getQty() != null && billFee.getBillItem().getQty() > 0)
                ? billFee.getBillItem().getQty() : 1.0;
        double unitGross = (billFee.getFeeUnitGrossValue() != null)
                ? billFee.getFeeUnitGrossValue()
                : (billFee.getFeeGrossValue() != null ? billFee.getFeeGrossValue() / qty : 0.0);
        double unitDiscount = pct > 0.0 ? (unitGross * pct) / 100.0 : 0.0;
        billFee.setFeeUnitDiscount(unitDiscount);
        billFee.setFeeDiscount(unitDiscount * qty);
    }

    public void updateBillItemMargin(BillItem billItem, double serviceValue, PatientEncounter patientEncounter, Department matrixDepartment, PriceMatrix priceMatrix) {
        PriceMatrix effectivePriceMatrix = priceMatrix;
        if (patientEncounter != null) {
            com.divudi.core.entity.Institution creditCompany = resolveSingleCreditCompany(patientEncounter);
            if (creditCompany != null) {
                PriceMatrix ccMatrix = priceMatrixController.fetchInwardMargin(billItem, serviceValue, matrixDepartment,
                        patientEncounter.getPaymentMethod(), creditCompany, patientEncounter.getAdmissionType(), resolveCurrentRoomCategory(patientEncounter));
                if (ccMatrix != null) {
                    effectivePriceMatrix = ccMatrix;
                }
            }
        }

        List<BillFee> billFees = getBillBean().getBillFee(billItem);

        for (BillFee billFee : billFees) {
            if (patientEncounter != null && patientEncounter.getAdmissionType() != null) {
                setBillFeeMargin(billFee, billItem.getItem(), effectivePriceMatrix, patientEncounter);
            } else {
                setBillFeeMargin(billFee, billItem.getItem(), effectivePriceMatrix);
            }

            if (billFee.getId() != null) {
                getBillFeeFacade().edit(billFee);
            }
        }

    }

    public void updateBillItemMargin(BillFee billFee, double serviceValue, PatientEncounter patientEncounter, Department matrixDepartment, PriceMatrix priceMatrix) {
        PriceMatrix effectivePriceMatrix = priceMatrix;
        if (patientEncounter != null && billFee.getBillItem() != null) {
            com.divudi.core.entity.Institution creditCompany = resolveSingleCreditCompany(patientEncounter);
            if (creditCompany != null) {
                PriceMatrix ccMatrix = priceMatrixController.fetchInwardMargin(billFee.getBillItem(), serviceValue,
                        matrixDepartment, patientEncounter.getPaymentMethod(), creditCompany, patientEncounter.getAdmissionType(), resolveCurrentRoomCategory(patientEncounter));
                if (ccMatrix != null) {
                    effectivePriceMatrix = ccMatrix;
                }
            }
        }

        if (patientEncounter != null && patientEncounter.getAdmissionType() != null) {
            setBillFeeMargin(billFee, billFee.getBillItem().getItem(), effectivePriceMatrix, patientEncounter);
        } else {
            setBillFeeMargin(billFee, billFee.getBillItem().getItem(), effectivePriceMatrix);
        }

        if (billFee.getId() != null) {
            getBillFeeFacade().edit(billFee);
        }

    }

    public void saveBillFee(BillFee bf, BillItem billItem, Bill b, WebUser wu) {

        bf.setCreatedAt(Calendar.getInstance().getTime());
        bf.setCreater(wu);
        bf.setBillItem(billItem);
        bf.setPatienEncounter(b.getPatientEncounter());
        bf.setPatient(b.getPatient());

        bf.setBill(b);
        if (bf.getId() == null) {
            getBillFeeFacade().create(bf);
        }

    }

//    public void updateBillItemMargin(BillItem billItem, List<BillFee> billFees, PriceMatrix priceMatrix, PatientEncounter patientEncounter) {
//        System.err.println("///////////////////////");
//        System.err.println("Margin " + priceMatrix.getMargin());
//
//        for (BillFee billFee : billFees) {
//            updateBillFeeMargin(billFee, priceMatrix);
//            getBillFeeFacade().edit(billFee);
//        }
//
//    }
    @EJB
    private BillFeeFacade billFeeFacade;

    public BillFee getIssueBillFee(BillItem billItem, Institution institution) {
        String sql = "Select bf from BillFee bf where bf.retired=false and "
                + " bf.billItem=:bItem and bf.fee.feeType=:ftp ";
        HashMap hm = new HashMap();
        hm.put("bItem", billItem);
        hm.put("ftp", FeeType.Issue);

        BillFee billtItemFee = getBillFeeFacade().findFirstByJpql(sql, hm);

        if (billtItemFee == null) {
            billtItemFee = new BillFee();

            Fee issueFee = getIssueFee();

            billtItemFee.setBillItem(billItem);
            billtItemFee.setFee(issueFee);
            billtItemFee.setCreatedAt(new Date());
            billtItemFee.setInstitution(institution);
//            getBillFeeFacade().create(billFee);
        }

        return billtItemFee;
    }

    private Fee getIssueFee() {
        String sql = "Select f from Fee f where f.retired=false and f.feeType=:nm";
        HashMap hm = new HashMap();
        hm.put("nm", FeeType.Issue);
        Fee issue = getFeeFacade().findFirstByJpql(sql, hm);

        if (issue == null) {
            issue = new Fee();
            issue.setName("Issue");
            issue.setFeeType(FeeType.Issue);
            issue.setCreatedAt(new Date());

            if (issue.getId() == null) {
                getFeeFacade().create(issue);
            }

        }

        return issue;
    }

    public TimedItemFee getTimedItemFee(TimedItem ti) {
        TimedItemFee tmp = new TimedItemFee();
        if (ti.getId() != null) {
            String sql = "SELECT tif FROM TimedItemFee tif where tif.retired=false AND tif.item.id=" + ti.getId();
            tmp = getTimedItemFeeFacade().findFirstByJpql(sql);
        }

        if (tmp == null) {
            tmp = new TimedItemFee();
            tmp.setDurationHours(0);
            tmp.setOverShootHours(0);
        }
        return tmp;
    }

    public List<TimedItemFee> getAllTimedItemFees(TimedItem ti) {
        if (ti == null || ti.getId() == null) {
            return new ArrayList<>();
        }
        HashMap hm = new HashMap();
        hm.put("id", ti.getId());
        String sql = "SELECT tif FROM TimedItemFee tif WHERE tif.retired=false AND tif.item.id=:id ORDER BY tif.sortOrder ASC";
        List<TimedItemFee> fees = getTimedItemFeeFacade().findByJpql(sql, hm);
        return fees != null ? fees : new ArrayList<>();
    }

    public TimedItemFee getFeeForBlock(List<TimedItemFee> fees, int blockNumber) {
        if (fees == null || fees.isEmpty()) {
            return null;
        }
        if (blockNumber <= fees.size()) {
            return fees.get(blockNumber - 1);
        }
        TimedItemFee lastFee = fees.get(fees.size() - 1);
        if (lastFee.isRepeating() || fees.size() == 1) {
            return lastFee;
        }
        return null;
    }

    public double calTotalTimedChargeForItem(TimedItem ti, Date fromTime, Date toTime, boolean foreigner) {
        List<TimedItemFee> fees = getAllTimedItemFees(ti);
        if (fees.isEmpty()) {
            return 0.0;
        }
        TimedItemFee firstFee = fees.get(0);
        double count = calCount(firstFee, fromTime, toTime);
        int wholeBlocks = (int) count;
        double total = 0.0;
        for (int b = 1; b <= wholeBlocks; b++) {
            TimedItemFee fee = getFeeForBlock(fees, b);
            if (fee != null) {
                total += foreigner ? fee.getFfee() : fee.getFee();
            }
        }
        double remainder = count - wholeBlocks;
        if (remainder > 0) {
            TimedItemFee fee = getFeeForBlock(fees, wholeBlocks + 1);
            if (fee != null) {
                total += (foreigner ? fee.getFfee() : fee.getFee()) * remainder;
            }
        }
        return total;
    }

    public TimedItemFeeFacade getTimedItemFeeFacade() {
        return timedItemFeeFacade;
    }

    public void setTimedItemFeeFacade(TimedItemFeeFacade timedItemFeeFacade) {
        this.timedItemFeeFacade = timedItemFeeFacade;
    }

    public double calTotalLinen(PatientEncounter patientEncounter) {

        if (patientEncounter == null || patientEncounter.getAdmissionType() == null) {
            return 0;
        }

        double linen = 0.0;

        Long dayCount = CommonFunctions.getDayCount(patientEncounter.getDateOfAdmission(), patientEncounter.getDateOfDischarge());

        for (PatientRoom pr : getPatientRooms(patientEncounter)) {
            linen += pr.getAddedLinenCharge();
        }

        if (patientEncounter.getAdmissionType().getDblValue() != null) {
            if (dayCount != 0) {
                linen += (patientEncounter.getAdmissionType().getDblValue() * dayCount);
            } else {
                linen += (patientEncounter.getAdmissionType().getDblValue() * 1);
            }
        }

        return linen;
    }

    public double calCountWithoutOverShoot(TimedItemFee tif, Date admittedAt, Date dischargedAt) {

        // No fee configured at all counts the same as a fee with no duration set:
        // nothing to bill. RoomFacilityCharge.timedItemFee is a nullable mapping,
        // so this is reachable from the room paths, and a missing configuration
        // should not break the page that is rendering the bill.
        if (tif == null) {
            return 0;
        }

        // A one-time fee is charged once for the whole service, however long it ran.
        if (tif.isOneTime()) {
            return 1;
        }

        double duration = tif.getDurationMinutes();

        // Same guard calCount already applies. Persisted data can still carry a
        // time-based fee with no duration set, and dividing by it below yields
        // Infinity — which casts to a huge block count and overcharges the bill.
        if (duration <= 0) {
            return 0;
        }

        double consumeTimeM = 0L;

        if (admittedAt == null) {
            admittedAt = new Date();
        }

        if (dischargedAt == null) {
            dischargedAt = new Date();
        }

        consumeTimeM = CommonFunctions.calculateDurationMin(admittedAt, dischargedAt);

        double count = 0;

        if (tif.isBooleanValue()) {
            //For Minute Calculation
            count = (consumeTimeM / duration);
        } else {
            //For Hour Calculation
            count = (long) (consumeTimeM / duration);
        }

        //  System.err.println("Min " + duration);
        //     System.err.println("Consume " + consumeTimeM);
        //   System.err.println("Count " + count);
        if (0 != (consumeTimeM % duration)) {
            count++;
        }

        return count;
    }
    
    private static final List<BillTypeAtomic> INWARD_MEDICINE_BILL_TYPES = Arrays.asList(
        BillTypeAtomic.PHARMACY_DIRECT_ISSUE,
        BillTypeAtomic.PHARMACY_DIRECT_ISSUE_CANCELLED,
        BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE,
        BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN,
        BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION,
        BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE,
        BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_RETURN,
        BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_CANCELLATION,
        BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD,
        BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN,
        BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION
    );
    public double calculateInwardTotal(PatientEncounter patientEncounter) {
        if (patientEncounter == null) {
            return 0.0;
        }
        return calculateInwardTotal(patientEncounter, fetchChildPatientEncounter(patientEncounter));
    }

    public double calculateInwardTotal(PatientEncounter patientEncounter, List<PatientEncounter> childPatientEncounters) {
        if (patientEncounter == null) {
            return 0.0;
        }

        double total = 0.0;

        total += getAdmissionCharge(patientEncounter, childPatientEncounters);
        total += getRoomCharge(patientEncounter, childPatientEncounters);
        total += getMoCharge(patientEncounter, childPatientEncounters);
        total += getNursingCharge(patientEncounter, childPatientEncounters);
        total += getMaintainCharge(patientEncounter, childPatientEncounters);
        total += getMedicalCareIcuCharge(patientEncounter, childPatientEncounters);
        total += getAdminCharge(patientEncounter, childPatientEncounters);
        total += getLinenCharge(patientEncounter, childPatientEncounters);

        total += calCostOfIssueByBill(patientEncounter, INWARD_MEDICINE_BILL_TYPES, childPatientEncounters);
        total += calCostOfIssue(patientEncounter, BillType.StoreBhtPre, childPatientEncounters);
        total += calculateProfessionalCharges(patientEncounter, childPatientEncounters, false);
        total += calculateDoctorAndNurseCharges(patientEncounter, childPatientEncounters);

        total += sumTotals(calServiceBillItemsTotalByInwardChargeTypeBulk(patientEncounter, childPatientEncounters));
        total += sumTotals(getTimedItemFeeTotalByInwardChargeTypeBulk(patientEncounter, childPatientEncounters));
        total += sumTotals(caltValueFromAdditionalChargeBulk(patientEncounter, childPatientEncounters));

        return Math.max(0.0, total);
    }

    private double sumTotals(Map<InwardChargeType, Double> totals) {
        double total = 0.0;
        if (totals != null && !totals.isEmpty()) {
            for (Double value : totals.values()) {
                if (value != null) {
                    total += value;
                }
            }
        }
        return total;
    }
    

    public double calCount(TimedItemFee tif, Date admittedDate, Date dischargedDate) {

        // No fee configured at all counts the same as a fee with no duration set:
        // nothing to bill. RoomFacilityCharge.timedItemFee is a nullable mapping,
        // so this is reachable from the room paths, and a missing configuration
        // should not break the page that is rendering the bill.
        if (tif == null) {
            return 0;
        }

        // A one-time fee is charged once for the whole service, however long it
        // ran — no block counting, and no dependency on elapsed time at all.
        if (tif.isOneTime()) {
            return 1;
        }

        double duration = tif.getDurationMinutes();
        double overShoot = tif.getOverShootMinutes();
        //  double tempFee = tif.getFee();
        double consumeTime = 0;

        if (dischargedDate == null) {
            dischargedDate = new Date();
        }

        consumeTime = CommonFunctions.calculateDurationMin(admittedDate, dischargedDate);
        if (consumeTime == 0) {
            return 0;
        }
        double count = 0;
        double calculation = 0;

        if (consumeTime != 0 && duration != 0) {
            if (tif.isBooleanValue()) {
                //For Minut Calculation (Theatre Charges)
                count = (consumeTime / duration);
            } else {
                //For Room Calculation Hour(For Room Charges)
                count = (long) (consumeTime / duration);
            }

            calculation = (consumeTime - (count * duration));
            if ((overShoot != 0 && overShoot <= calculation) || count == 0) {
                count++;
            }
        }

        return count;
    }

    public ItemFeeFacade getItemFeeFacade() {
        return itemFeeFacade;
    }

    public void setItemFeeFacade(ItemFeeFacade itemFeeFacade) {
        this.itemFeeFacade = itemFeeFacade;
    }

    public AdmissionFacade getAdmissionFacade() {
        return admissionFacade;
    }

    public void setAdmissionFacade(AdmissionFacade admissionFacade) {
        this.admissionFacade = admissionFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public PriceMatrixFacade getPriceMatrixFacade() {
        return priceMatrixFacade;
    }

    public void setPriceMatrixFacade(PriceMatrixFacade priceMatrixFacade) {
        this.priceMatrixFacade = priceMatrixFacade;
    }

}
