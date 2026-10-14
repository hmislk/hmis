package com.divudi.bean.inward;

import com.divudi.core.data.BillType;
import com.divudi.core.data.inward.SurgeryBillType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.entity.inward.PatientRoomTimedItemCharge;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.entity.inward.TheatreRoom;
import com.divudi.core.entity.inward.TimedItemFee;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.PatientRoomFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Per-surgery charge breakdown for the Surgery Workbench
 * (theater/patient_surgery.xhtml), issue #24134.
 *
 * Request scoped on purpose: every value here is derived from data that other
 * beans change (services settled from BillBhtController, professional fees,
 * timed services, pharmacy issues, theatre accept/return in
 * PatientTransferController), and the theatre stay's time-based charge grows
 * with the clock. Recomputing once per request keeps the workbench current
 * without a session-scoped cache that every one of those paths would have to
 * remember to invalidate.
 *
 * Theatre charges are a live estimate using the same calculators the interim
 * bill uses (InwardBeanController.calCount for the room facility charge's
 * block fee, calTotalTimedChargeForItem for items attached to the room). The
 * stored PatientRoom.calculated* columns are not used because they stay 0
 * until the admission's interim/final bill is generated.
 */
@Named
@RequestScoped
public class SurgeryChargeSummaryController implements Serializable {

    private static final List<SurgeryBillType> BILLED_CHILD_TYPES = Arrays.asList(
            SurgeryBillType.Service,
            SurgeryBillType.TimedService,
            SurgeryBillType.ProfessionalFee);

    @Inject
    private SurgeryBillController surgeryBillController;
    @Inject
    private InwardBeanController inwardBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private PatientRoomFacade patientRoomFacade;

    private boolean calculated;
    private double serviceCharges;
    private double timedServiceCharges;
    private double professionalFees;
    private double medicineCharges;
    private double theatreCharges;
    private List<TheatreStayCharge> theatreStays;

    private void calculate() {
        if (calculated) {
            return;
        }
        // Only calculate while rendering. The Theatre Charges table is read
        // during decode of every workbench postback too; computing then would
        // run before the button's action (e.g. accept/return theatre) and pin
        // the stale figures for the rest of the request.
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null && context.getCurrentPhaseId() != PhaseId.RENDER_RESPONSE) {
            theatreStays = new ArrayList<>();
            return;
        }
        calculated = true;
        theatreStays = new ArrayList<>();
        Bill surgeryBill = surgeryBillController.getSurgeryBill();
        if (surgeryBill == null || surgeryBill.getId() == null) {
            return;
        }
        calculateChildBillCharges(surgeryBill);
        calculateMedicineCharges(surgeryBill);
        calculateTheatreCharges(surgeryBill);
    }

    private void calculateChildBillCharges(Bill surgeryBill) {
        // Summed from the fees, not Bill.netTotal: removing a professional fee
        // retires its BillFee, and removing a timed service retires its
        // PatientItem (SurgeryBillController.removeTimeService), without either
        // path recalculating the child bill's netTotal.
        String jpql = "SELECT b.surgeryBillType, SUM(bf.feeValue) "
                + " FROM BillFee bf JOIN bf.bill b LEFT JOIN bf.patientItem pi "
                + " WHERE bf.retired = false "
                + " AND b.retired = false "
                + " AND b.cancelled = false "
                + " AND TYPE(b) = :billedClass "
                + " AND b.forwardReferenceBill = :surgeryBill "
                + " AND b.surgeryBillType IN :types "
                + " AND (pi IS NULL OR pi.retired = false) "
                + " GROUP BY b.surgeryBillType";
        Map<String, Object> params = new HashMap<>();
        params.put("billedClass", BilledBill.class);
        params.put("surgeryBill", surgeryBill);
        params.put("types", BILLED_CHILD_TYPES);
        List<Object[]> rows = billFeeFacade.findAggregates(jpql, params);
        if (rows == null) {
            return;
        }
        for (Object[] row : rows) {
            SurgeryBillType type = (SurgeryBillType) row[0];
            double value = row[1] == null ? 0.0 : ((Number) row[1]).doubleValue();
            if (type == SurgeryBillType.Service) {
                serviceCharges += value;
            } else if (type == SurgeryBillType.TimedService) {
                timedServiceCharges += value;
            } else if (type == SurgeryBillType.ProfessionalFee) {
                professionalFees += value;
            }
        }
    }

    private void calculateMedicineCharges(Bill surgeryBill) {
        // Queried fresh rather than from SurgeryBillController's session-cached
        // issue lists, which only refresh via the workbench's own back buttons.
        // Issues are the same PreBills the Pharmacy Issues tab lists
        // (createIssueTable); cancelled ones are left out, and returns
        // (RefundBill) reduce the total whatever sign their netTotal carries.
        List<BillType> issueTypes = Arrays.asList(BillType.PharmacyBhtPre, BillType.StoreBhtPre);
        String issueJpql = "SELECT SUM(b.netTotal) FROM Bill b "
                + " WHERE b.retired = false "
                + " AND b.cancelled = false "
                + " AND b.forwardReferenceBill = :surgeryBill "
                + " AND b.billType IN :types "
                + " AND b.billedBill IS NULL "
                + " AND TYPE(b) = :preClass";
        Map<String, Object> params = new HashMap<>();
        params.put("surgeryBill", surgeryBill);
        params.put("types", issueTypes);
        params.put("preClass", PreBill.class);
        double issued = billFacade.findDoubleByJpql(issueJpql, params);

        String returnJpql = "SELECT b FROM Bill b "
                + " WHERE b.retired = false "
                + " AND b.cancelled = false "
                + " AND b.forwardReferenceBill = :surgeryBill "
                + " AND b.billType IN :types "
                + " AND TYPE(b) = :refundClass "
                + " AND TYPE(b.billedBill) = :preClass";
        Map<String, Object> returnParams = new HashMap<>();
        returnParams.put("surgeryBill", surgeryBill);
        returnParams.put("types", issueTypes);
        returnParams.put("refundClass", RefundBill.class);
        returnParams.put("preClass", PreBill.class);
        double returned = 0.0;
        List<Bill> returns = billFacade.findByJpql(returnJpql, returnParams);
        if (returns != null) {
            for (Bill r : returns) {
                returned += Math.abs(r.getNetTotal());
            }
        }
        medicineCharges = issued - returned;
    }

    private void calculateTheatreCharges(Bill surgeryBill) {
        // PatientTransferController.acceptInTheatre attaches the theatre stay
        // to the surgery's own procedure encounter when a surgery was selected
        // on Send to Theatre.
        if (surgeryBill.getProcedure() == null || surgeryBill.getProcedure().getId() == null) {
            return;
        }
        String jpql = "SELECT p FROM PatientRoom p "
                + " WHERE p.retired = false "
                + " AND TYPE(p) = :theatreClass "
                + " AND p.patientEncounter = :procedure "
                + " ORDER BY p.admittedAt";
        Map<String, Object> params = new HashMap<>();
        params.put("theatreClass", TheatreRoom.class);
        params.put("procedure", surgeryBill.getProcedure());
        List<PatientRoom> rooms = patientRoomFacade.findByJpql(jpql, params);
        if (rooms == null) {
            return;
        }
        Date now = new Date();
        for (PatientRoom room : rooms) {
            if (room.getAdmittedAt() == null) {
                continue;
            }
            TheatreStayCharge stay = createTheatreStayCharge(room, now);
            theatreStays.add(stay);
            theatreCharges += stay.getTotal();
        }
    }

    private TheatreStayCharge createTheatreStayCharge(PatientRoom room, Date now) {
        TheatreStayCharge stay = new TheatreStayCharge();
        Date to = room.getDischargedAt() != null ? room.getDischargedAt() : now;
        RoomFacilityCharge rfc = room.getRoomFacilityCharge();
        TimedItemFee blockFee = rfc == null ? null : rfc.getTimedItemFee();

        stay.setRoomName(rfc == null ? "" : rfc.getName());
        stay.setFrom(room.getAdmittedAt());
        stay.setTo(room.getDischargedAt());
        stay.setBlockUnit(blockFee == null || blockFee.getDurationUnit() == null
                ? "" : blockFee.getDurationUnit().getLabel());

        double blocks = inwardBean.calCount(blockFee, room.getAdmittedAt(), to);
        double roomPart = room.getCurrentRoomCharge() * blocks;
        double perBlock = room.getCurrentMaintananceCharge()
                + room.getCurrentNursingCharge()
                + room.getCurrentAdministrationCharge()
                + room.getCurrentMedicalCareCharge();
        double added = room.getAddedRoomCharge()
                + room.getAddedMaintainCharge()
                + room.getAddedNursingCharge()
                + room.getAddedAdministrationCharge()
                + room.getAddedMedicalCareCharge();
        double discount = room.getDiscountRoomCharge()
                + room.getDiscountMaintainCharge()
                + room.getDiscountNursingCharge()
                + room.getDiscountAdministrationCharge()
                + room.getDiscountMedicalCareCharge();
        stay.setBlocks(blocks);
        stay.setRatePerBlock(perBlock + room.getCurrentRoomCharge());
        stay.setTimeBasedCharge(roomPart + perBlock * blocks + added - discount);

        boolean foreigner = room.getPatientEncounter() != null && room.getPatientEncounter().isForiegner();
        List<PatientRoomTimedItemCharge> itemCharges = inwardBean.fetchTimedItemCharges(room);
        if (itemCharges != null) {
            for (PatientRoomTimedItemCharge tc : itemCharges) {
                if (tc == null || tc.getTimedItem() == null) {
                    continue;
                }
                double value = inwardBean.calTotalTimedChargeForItem(
                        tc.getTimedItem(), room.getAdmittedAt(), to, foreigner) - tc.getDiscountCharge();
                stay.getItems().add(new TheatreItemCharge(tc.getTimedItem().getName(), value));
            }
        }
        return stay;
    }

    public double getServiceCharges() {
        calculate();
        return serviceCharges;
    }

    public double getTimedServiceCharges() {
        calculate();
        return timedServiceCharges;
    }

    public double getProfessionalFees() {
        calculate();
        return professionalFees;
    }

    public double getMedicineCharges() {
        calculate();
        return medicineCharges;
    }

    public double getTheatreCharges() {
        calculate();
        return theatreCharges;
    }

    public double getTotal() {
        calculate();
        return serviceCharges + timedServiceCharges + professionalFees + medicineCharges + theatreCharges;
    }

    public List<TheatreStayCharge> getTheatreStays() {
        calculate();
        return theatreStays;
    }

    public static class TheatreStayCharge implements Serializable {

        private String roomName;
        private Date from;
        private Date to;
        private String blockUnit;
        private double blocks;
        private double ratePerBlock;
        private double timeBasedCharge;
        private final List<TheatreItemCharge> items = new ArrayList<>();

        public double getTotal() {
            double total = timeBasedCharge;
            for (TheatreItemCharge item : items) {
                total += item.getValue();
            }
            return total;
        }

        public boolean isOngoing() {
            return to == null;
        }

        public String getRoomName() {
            return roomName;
        }

        public void setRoomName(String roomName) {
            this.roomName = roomName;
        }

        public Date getFrom() {
            return from;
        }

        public void setFrom(Date from) {
            this.from = from;
        }

        public Date getTo() {
            return to;
        }

        public void setTo(Date to) {
            this.to = to;
        }

        public String getBlockUnit() {
            return blockUnit;
        }

        public void setBlockUnit(String blockUnit) {
            this.blockUnit = blockUnit;
        }

        public double getBlocks() {
            return blocks;
        }

        public void setBlocks(double blocks) {
            this.blocks = blocks;
        }

        public double getRatePerBlock() {
            return ratePerBlock;
        }

        public void setRatePerBlock(double ratePerBlock) {
            this.ratePerBlock = ratePerBlock;
        }

        public double getTimeBasedCharge() {
            return timeBasedCharge;
        }

        public void setTimeBasedCharge(double timeBasedCharge) {
            this.timeBasedCharge = timeBasedCharge;
        }

        public List<TheatreItemCharge> getItems() {
            return items;
        }
    }

    public static class TheatreItemCharge implements Serializable {

        private final String name;
        private final double value;

        public TheatreItemCharge(String name, double value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public double getValue() {
            return value;
        }
    }
}
