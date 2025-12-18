/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;

import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.FeeType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.SurgeryCountDoctorWiseDTO;
import com.divudi.core.data.hr.ReportKeyWord;
import com.divudi.core.data.inward.InwardChargeType;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Consultant;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.Speciality;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.facade.AdmissionTypeFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.line.LineChartModel;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.axes.cartesian.CartesianScales;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearAxes;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearTicks;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartOptions;
import org.primefaces.model.charts.line.LineChartDataSet;
import org.primefaces.model.charts.line.LineChartOptions;
import org.primefaces.model.charts.optionconfig.legend.Legend;
import org.primefaces.model.charts.optionconfig.title.Title;

/**
 *
 * @author pdhs
 */
@Named
@SessionScoped
public class InwardReportController implements Serializable {

    /**
     * Creates a new instance of InwardReportController
     */
    public InwardReportController() {
    }

    @EJB
    PatientEncounterFacade peFacade;
    @EJB
    AdmissionTypeFacade admissionTypeFacade;
    @EJB
    PatientInvestigationFacade patientInvestigationFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    BillFeeFacade billFeeFacade;

    @Inject
    SessionController sessionController;
    @Inject
    InwardReportControllerBht inwardReportControllerBht;
    @Inject
    BhtSummeryController bhtSummeryController;

    PaymentMethod paymentMethod;
    AdmissionType admissionType;
    Institution institution;
    Date fromDate;
    Date toDate;
    private Date fromYearStartDate;
    private Date toYearEndDate;

    Admission patientEncounter;
    double grossTotals;
    double discounts;
    double netTotals;
    boolean withFooter;
    String invoceNo;
    String vatRegNo;
    Bill bill;
    private String patientCode;

    List<IncomeByCategoryRecord> incomeByCategoryRecords;
    List<IndividualBhtIncomeByCategoryRecord> individualBhtIncomeByCategoryRecord;
    List<AdmissionType> admissionty;
    private List<AdmissionType> admissionTypes;
    List<PatientEncounter> patientEncounters;
    List<BillItem> billItems;

    List<BillItem> billedBill;
    List<BillItem> cancelledBill;
    List<BillItem> refundBill;
    List<PatientInvestigation> patientInvestigations;
    double totalBilledBill;
    double totalCancelledBill;
    double totalRefundBill;

    // for disscharge book
    boolean dischargeDate = true;
    boolean bhtNo = true;
    boolean paymentMethord = true;
    boolean creditCompany = true;
    boolean person = true;
    boolean guardian = true;
    boolean room = true;
    boolean refDoctor = true;
    boolean AddmitDetails = true;
    boolean billedBy = true;
    boolean finalBillTotal = true;
    boolean paidByPatient = true;
    boolean creditPaidAmount = true;
    boolean dueAmount = true;
    boolean calculatedAmount = true;
    boolean differentAmount = true;
    boolean developers = false;
    // for disscharge book
    boolean withoutCancelBHT = true;
    private Speciality currentSpeciality;

    private ReportKeyWord reportKeyWord;

    public List<PatientEncounter> getPatientEncounters() {
        return patientEncounters;
    }

    public void setPatientEncounters(List<PatientEncounter> patientEncounters) {
        this.patientEncounters = patientEncounters;
    }
    double netTotal;
    double netPaid;

    public void fillAdmissionBook() {
        Date startTime = new Date();

        fillAdmissions(null, null);

    }

    public void fillAdmissionBookNew() {
        Date startTime = new Date();
        if (getReportKeyWord().getString().isEmpty() || getReportKeyWord().getString() == null) {
            JsfUtil.addErrorMessage("Select a Selection Methord");
            return;
        }
        if (getReportKeyWord().getString().equals("0")) {
            fillAdmissions(null, null);
        } else if (getReportKeyWord().getString().equals("1")) {
            fillAdmissions(false, false);
        } else if (getReportKeyWord().getString().equals("2")) {
            fillAdmissions(true, false);
        } else if (getReportKeyWord().getString().equals("3")) {
            fillAdmissions(true, true);
        }

    }

    public void fillAdmissionBookOnlyInward() {
        Date startTime = new Date();

        fillAdmissions(false, null);

    }

    public void fillAdmissionBookOnlyDischarged() {
        Date startTime = new Date();
        fillAdmissions(true, null);

    }

    public void fillAdmissionBookOnlyDischargedNotFinalized() {
        Date startTime = new Date();
        fillAdmissions(true, false);

    }

    public void fillAdmissionBookOnlyDischargedFinalized() {
        Date startTime = new Date();
        fillAdmissions(true, true);

    }

    private List<SurgeryCountDoctorWiseDTO> billList;

//    public void processSurgeryCountDoctorWiseReport() {
//
//        billList = new ArrayList<>();
//
//        Map<String, Object> params = new HashMap<>();
//        StringBuilder jpql = new StringBuilder();
//        jpql.append(" Select new com.divudi.core.data.dto.SurgeryCountDoctorWiseDTO(")
//                .append(" b.staff, ")
//                .append(" b.staff.person.name, ")
//                .append(" b.staff.speciality.name, ")
//                .append(" b.createdAt")
//                .append(") ")
//                .append(" from BillFee b ")
//                .append(" Where b.retired = false ")
//                .append(" And b.fee.feeType = :feeType ")
//                .append(" And b.bill.billTypeAtomic = :bta ")
//                .append(" And type(b.staff) = :staffClass ")
//                //                .append(" And (type(b.staff) = :doctorClass OR type(b.staff) = :consultantClass) ")
//                .append(" AND b.createdAt BETWEEN :fromDate AND :toDate ");
//
//        params.put("bta", BillTypeAtomic.INWARD_THEATRE_PROFESSIONAL_FEE_BILL);
//        params.put("fromDate", fromYearStartDate);
//        params.put("toDate", toYearEndDate);
//        params.put("staffClass", Consultant.class);
//        params.put("feeType", FeeType.Staff);  // Ensures we're looking at staff professional fees
//
////        params.put("doctorClass", Doctor.class);
////        params.put("consultantClass", Consultant.class);
//        if (currentSpeciality != null) {
//            jpql.append(" AND b.staff.speciality = :spe ");
//            params.put("spe", currentSpeciality);
//        }
//
//        billList = (List<SurgeryCountDoctorWiseDTO>) billFeeFacade.findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
//
////        double febCount = billList.stream().mapToDouble(SurgeryCountDoctorWiseDTO::getFebruary).count();
//    }
    public void processSurgeryCountDoctorWiseReport() {
        billList = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();

        jpql.append(" Select new com.divudi.core.data.dto.SurgeryCountDoctorWiseDTO(")
                .append(" b.staff, ")
                .append(" b.staff.person.name, ")
                .append(" b.staff.speciality.name, ")
                .append(" b.createdAt")
                .append(") ")
                .append(" from BillFee b ")
                .append(" Where b.retired = false ")
                .append(" And b.bill.billTypeAtomic = :bta ")
                .append(" And (type(b.staff) = :doctorClass OR type(b.staff) = :consultantClass) ")
                .append(" AND b.createdAt BETWEEN :fromDate AND :toDate ");

        params.put("bta", BillTypeAtomic.INWARD_THEATRE_PROFESSIONAL_FEE_BILL);
        params.put("fromDate", fromYearStartDate);
        params.put("toDate", toYearEndDate);
        params.put("doctorClass", Doctor.class);
        params.put("consultantClass", Consultant.class);

        if (currentSpeciality != null) {
            jpql.append(" AND b.staff.speciality = :spe ");
            params.put("spe", currentSpeciality);
        }

        jpql.append(" ORDER BY b.staff.speciality.name, b.staff.person.name ");

        List<SurgeryCountDoctorWiseDTO> rawList = (List<SurgeryCountDoctorWiseDTO>) billFeeFacade.findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

        // Group by specialty and doctor, count surgeries month-wise
        Map<String, Map<Long, SurgeryCountDoctorWiseDTO>> specialtyDoctorMap = new LinkedHashMap<>();
        Calendar cal = Calendar.getInstance();

        for (SurgeryCountDoctorWiseDTO dto : rawList) {
            String speciality = dto.getSpecialityName();
            Long staffId = dto.getStaff().getId();

            // Get or create specialty map
            Map<Long, SurgeryCountDoctorWiseDTO> doctorMap = specialtyDoctorMap.get(speciality);
            if (doctorMap == null) {
                doctorMap = new LinkedHashMap<>();
                specialtyDoctorMap.put(speciality, doctorMap);
            }

            // Get or create doctor aggregation
            SurgeryCountDoctorWiseDTO aggregated = doctorMap.get(staffId);
            if (aggregated == null) {
                aggregated = new SurgeryCountDoctorWiseDTO(
                        dto.getStaff(),
                        dto.getDoctorName(),
                        dto.getSpecialityName(),
                        null
                );
                doctorMap.put(staffId, aggregated);
            }

            // Increment month counter
            cal.setTime(dto.getCreatedAt());
            int month = cal.get(Calendar.MONTH);
            aggregated.addMonthCount(month, 1);
        }

        // Build final list with subtotals and grand total
        billList = new ArrayList<>();
        SurgeryCountDoctorWiseDTO grandTotal = new SurgeryCountDoctorWiseDTO();

        for (Map.Entry<String, Map<Long, SurgeryCountDoctorWiseDTO>> specialtyEntry : specialtyDoctorMap.entrySet()) {
            String speciality = specialtyEntry.getKey();
            Map<Long, SurgeryCountDoctorWiseDTO> doctorMap = specialtyEntry.getValue();

            SurgeryCountDoctorWiseDTO subtotal = new SurgeryCountDoctorWiseDTO(speciality);

            // Add all doctors for this specialty
            for (SurgeryCountDoctorWiseDTO doctor : doctorMap.values()) {
                billList.add(doctor);
                subtotal.addAllCounts(doctor);
                grandTotal.addAllCounts(doctor);
            }

            // Add subtotal row
            billList.add(subtotal);
        }

        // Add grand total row
        billList.add(grandTotal);

        createChartModels();
    }

    private LineChartModel lineChartModel;
    private BarChartModel barChartModel;
    private LineChartModel specialtyLineChartModel;
    private BarChartModel specialtyBarChartModel;

    public void createChartModels() {
        createDoctorCharts();
        createSpecialtyCharts();
    }

    private void createDoctorCharts() {
        lineChartModel = new LineChartModel();
        barChartModel = new BarChartModel();

        // Define color palette
        String[] colors = {
            "75, 192, 192", "255, 99, 132", "54, 162, 235", "255, 206, 86",
            "153, 102, 255", "255, 159, 64", "199, 199, 199", "83, 102, 255",
            "255, 99, 255", "99, 255, 132"
        };

        int colorIndex = 0;

        // Create JSON-compatible data structure for Chart.js
        StringBuilder lineDataJson = new StringBuilder("{\"labels\":[");
        lineDataJson.append("\"Jan\",\"Feb\",\"Mar\",\"Apr\",\"May\",\"Jun\",");
        lineDataJson.append("\"Jul\",\"Aug\",\"Sep\",\"Oct\",\"Nov\",\"Dec\"],");
        lineDataJson.append("\"datasets\":[");

        StringBuilder barDataJson = new StringBuilder("{\"labels\":[");
        barDataJson.append("\"Jan\",\"Feb\",\"Mar\",\"Apr\",\"May\",\"Jun\",");
        barDataJson.append("\"Jul\",\"Aug\",\"Sep\",\"Oct\",\"Nov\",\"Dec\"],");
        barDataJson.append("\"datasets\":[");

        boolean firstDataset = true;

        for (SurgeryCountDoctorWiseDTO dto : billList) {
            if (dto.isSubtotal() || dto.isGrandTotal()) {
                continue;
            }

            String color = colors[colorIndex % colors.length];
            colorIndex++;

            if (!firstDataset) {
                lineDataJson.append(",");
                barDataJson.append(",");
            }
            firstDataset = false;

            // Line dataset
            lineDataJson.append("{")
                    .append("\"label\":\"").append(escapeJson(dto.getDoctorName())).append("\",")
                    .append("\"data\":[")
                    .append(dto.getJanuary()).append(",")
                    .append(dto.getFebruary()).append(",")
                    .append(dto.getMarch()).append(",")
                    .append(dto.getApril()).append(",")
                    .append(dto.getMay()).append(",")
                    .append(dto.getJune()).append(",")
                    .append(dto.getJuly()).append(",")
                    .append(dto.getAugust()).append(",")
                    .append(dto.getSeptember()).append(",")
                    .append(dto.getOctober()).append(",")
                    .append(dto.getNovember()).append(",")
                    .append(dto.getDecember())
                    .append("],")
                    .append("\"borderColor\":\"rgb(").append(color).append(")\",")
                    .append("\"fill\":false,")
                    .append("\"tension\":0.4")
                    .append("}");

            // Bar dataset
            barDataJson.append("{")
                    .append("\"label\":\"").append(escapeJson(dto.getDoctorName())).append("\",")
                    .append("\"data\":[")
                    .append(dto.getJanuary()).append(",")
                    .append(dto.getFebruary()).append(",")
                    .append(dto.getMarch()).append(",")
                    .append(dto.getApril()).append(",")
                    .append(dto.getMay()).append(",")
                    .append(dto.getJune()).append(",")
                    .append(dto.getJuly()).append(",")
                    .append(dto.getAugust()).append(",")
                    .append(dto.getSeptember()).append(",")
                    .append(dto.getOctober()).append(",")
                    .append(dto.getNovember()).append(",")
                    .append(dto.getDecember())
                    .append("],")
                    .append("\"backgroundColor\":\"rgba(").append(color).append(",0.7)\",")
                    .append("\"borderColor\":\"rgb(").append(color).append(")\",")
                    .append("\"borderWidth\":1")
                    .append("}");
        }

        lineDataJson.append("]}");
        barDataJson.append("]}");

        // Configure options
        String lineOptions = "{\"plugins\":{\"title\":{\"display\":true,\"text\":\"Doctor Wise Surgery Count - Year " + getSelectedYear() + "\"},\"legend\":{\"display\":true,\"position\":\"right\"}},\"scales\":{\"y\":{\"beginAtZero\":true,\"ticks\":{\"stepSize\":1}}}}";
        String barOptions = "{\"plugins\":{\"title\":{\"display\":true,\"text\":\"Doctor Wise Surgery Count - Year " + getSelectedYear() + "\"},\"legend\":{\"display\":true,\"position\":\"top\"}},\"scales\":{\"y\":{\"beginAtZero\":true,\"ticks\":{\"stepSize\":1}}}}";

        // Set the extender functions to apply data and options
        lineChartModel.setExtender("doctorLineChartExtender");
        barChartModel.setExtender("doctorBarChartExtender");

        // Store JSON in request scope for JavaScript access
        javax.faces.context.FacesContext.getCurrentInstance().getExternalContext()
                .getRequestMap().put("doctorLineData", lineDataJson.toString());
        javax.faces.context.FacesContext.getCurrentInstance().getExternalContext()
                .getRequestMap().put("doctorLineOptions", lineOptions);
        javax.faces.context.FacesContext.getCurrentInstance().getExternalContext()
                .getRequestMap().put("doctorBarData", barDataJson.toString());
        javax.faces.context.FacesContext.getCurrentInstance().getExternalContext()
                .getRequestMap().put("doctorBarOptions", barOptions);
    }

    private void createSpecialtyCharts() {
        specialtyLineChartModel = new LineChartModel();
        specialtyBarChartModel = new BarChartModel();

        // Define color palette for specialties
        String[] colors = {
            "220, 20, 60", "65, 105, 225", "255, 140, 0",
            "34, 139, 34", "138, 43, 226", "255, 215, 0"
        };

        int colorIndex = 0;

        // Create JSON-compatible data structure
        StringBuilder lineDataJson = new StringBuilder("{\"labels\":[");
        lineDataJson.append("\"Jan\",\"Feb\",\"Mar\",\"Apr\",\"May\",\"Jun\",");
        lineDataJson.append("\"Jul\",\"Aug\",\"Sep\",\"Oct\",\"Nov\",\"Dec\"],");
        lineDataJson.append("\"datasets\":[");

        StringBuilder barDataJson = new StringBuilder("{\"labels\":[");
        barDataJson.append("\"Jan\",\"Feb\",\"Mar\",\"Apr\",\"May\",\"Jun\",");
        barDataJson.append("\"Jul\",\"Aug\",\"Sep\",\"Oct\",\"Nov\",\"Dec\"],");
        barDataJson.append("\"datasets\":[");

        boolean firstDataset = true;

        for (SurgeryCountDoctorWiseDTO dto : billList) {
            if (!dto.isSubtotal()) {
                continue;
            }

            String color = colors[colorIndex % colors.length];
            colorIndex++;

            if (!firstDataset) {
                lineDataJson.append(",");
                barDataJson.append(",");
            }
            firstDataset = false;

            // Line dataset
            lineDataJson.append("{")
                    .append("\"label\":\"").append(escapeJson(dto.getSpecialityName())).append("\",")
                    .append("\"data\":[")
                    .append(dto.getJanuary()).append(",")
                    .append(dto.getFebruary()).append(",")
                    .append(dto.getMarch()).append(",")
                    .append(dto.getApril()).append(",")
                    .append(dto.getMay()).append(",")
                    .append(dto.getJune()).append(",")
                    .append(dto.getJuly()).append(",")
                    .append(dto.getAugust()).append(",")
                    .append(dto.getSeptember()).append(",")
                    .append(dto.getOctober()).append(",")
                    .append(dto.getNovember()).append(",")
                    .append(dto.getDecember())
                    .append("],")
                    .append("\"borderColor\":\"rgb(").append(color).append(")\",")
                    .append("\"borderWidth\":3,")
                    .append("\"fill\":false,")
                    .append("\"tension\":0.4")
                    .append("}");

            // Bar dataset
            barDataJson.append("{")
                    .append("\"label\":\"").append(escapeJson(dto.getSpecialityName())).append("\",")
                    .append("\"data\":[")
                    .append(dto.getJanuary()).append(",")
                    .append(dto.getFebruary()).append(",")
                    .append(dto.getMarch()).append(",")
                    .append(dto.getApril()).append(",")
                    .append(dto.getMay()).append(",")
                    .append(dto.getJune()).append(",")
                    .append(dto.getJuly()).append(",")
                    .append(dto.getAugust()).append(",")
                    .append(dto.getSeptember()).append(",")
                    .append(dto.getOctober()).append(",")
                    .append(dto.getNovember()).append(",")
                    .append(dto.getDecember())
                    .append("],")
                    .append("\"backgroundColor\":\"rgba(").append(color).append(",0.7)\",")
                    .append("\"borderColor\":\"rgb(").append(color).append(")\",")
                    .append("\"borderWidth\":2")
                    .append("}");
        }

        lineDataJson.append("]}");
        barDataJson.append("]}");

        // Configure options
        String lineOptions = "{\"plugins\":{\"title\":{\"display\":true,\"text\":\"Specialty Wise Surgery Count - Year " + getSelectedYear() + "\"},\"legend\":{\"display\":true,\"position\":\"right\"}},\"scales\":{\"y\":{\"beginAtZero\":true,\"ticks\":{\"stepSize\":5}}}}";
        String barOptions = "{\"plugins\":{\"title\":{\"display\":true,\"text\":\"Specialty Wise Surgery Count - Year " + getSelectedYear() + "\"},\"legend\":{\"display\":true,\"position\":\"top\"}},\"scales\":{\"y\":{\"beginAtZero\":true,\"ticks\":{\"stepSize\":5}}}}";

        // Set the extender functions
        specialtyLineChartModel.setExtender("specialtyLineChartExtender");
        specialtyBarChartModel.setExtender("specialtyBarChartExtender");

        // Store JSON in request scope
        javax.faces.context.FacesContext.getCurrentInstance().getExternalContext()
                .getRequestMap().put("specialtyLineData", lineDataJson.toString());
        javax.faces.context.FacesContext.getCurrentInstance().getExternalContext()
                .getRequestMap().put("specialtyLineOptions", lineOptions);
        javax.faces.context.FacesContext.getCurrentInstance().getExternalContext()
                .getRequestMap().put("specialtyBarData", barDataJson.toString());
        javax.faces.context.FacesContext.getCurrentInstance().getExternalContext()
                .getRequestMap().put("specialtyBarOptions", barOptions);
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    public int getSelectedYear() {
        Calendar cal = Calendar.getInstance();
        if (fromYearStartDate != null) {
            cal.setTime(fromYearStartDate);
        }
        return cal.get(Calendar.YEAR);
    }

    public void fillAdmissions(Boolean discharged, Boolean finalized) {
        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.dateOfAdmission between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad";
            m.put("ad", admissionType);
        }

        if (withoutCancelBHT) {
            sql += " and b.retired=false ";
        }
        //// // System.out.println("discharged = " + discharged);
        if (discharged != null) {
            if (discharged) {
                sql += " and b.discharged=true ";
            } else {
                sql += " and b.discharged=false ";
            }
        }
        if (finalized != null) {
            if (finalized) {
                sql += " and b.paymentFinalized=true ";
            } else {
                sql += " and b.paymentFinalized=false ";
            }
        }

        sql += " order by b.dateOfAdmission,b.bhtNo ";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
//        calTtoal();
        calTtoal(patientEncounters);
    }

    private void calTtoal() {
        if (patientEncounters == null) {
            return;
        }

        netTotal = 0;
        netPaid = 0;
        for (PatientEncounter p : patientEncounters) {
            bhtSummeryController.setPatientEncounter((Admission) p);
            bhtSummeryController.createTables();
            p.setTransTotal(bhtSummeryController.getGrantTotal());
            p.setTransPaid(bhtSummeryController.getPaid());

            netTotal += p.getTransTotal();
            netPaid += p.getTransPaid();
        }
    }

    private void calTtoal(List<PatientEncounter> patientEncounters) {
        if (patientEncounters == null) {
            return;
        }
        netTotal = 0;
        netPaid = 0;
        for (PatientEncounter p : patientEncounters) {
            if (p.getFinalBill() != null) {
                netTotal += p.getFinalBill().getNetTotal();
                netPaid += p.getPaidByCreditCompany() + p.getFinalBill().getPaidAmount();
            }
        }
    }

    public void fillAdmissionBookOnlyInwardDeleted() {
        Date startTime = new Date();

        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=true "
                + " and b.dateOfAdmission between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad";
            m.put("ad", admissionType);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    double total;
    double paid;
    double creditPaid;
    double creditUsed;
    double calTotal;
    double totalVat;
    double totalVatCalculatedValue;

    public void fillDischargeBook() {
        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                //                + " and b.paymentFinalized=true "
                + " and b.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad ";
            m.put("ad", admissionType);
        }

        if (institution != null) {
            sql += " and b.creditCompany =:ins ";
            m.put("ins", institution);
        }

        if (paymentMethod != null) {
            sql += " and b.paymentMethod =:pm ";
            m.put("pm", paymentMethod);
        }

        if (reportKeyWord.getStaff() != null) {
            sql += " and b.referringDoctor =:refDoc ";
            m.put("refDoc", reportKeyWord.getStaff());
        }

        sql += " order by  b.dateOfDischarge";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        calTotalDischarged();
    }

    public void calTotalDischarged() {
        if (patientEncounters == null) {
            return;
        }
        setTotal(0);
        paid = 0;
        calTotal = 0;
        creditPaid = 0;
        creditUsed = 0;
        for (PatientEncounter p : patientEncounters) {
            inwardReportControllerBht.setPatientEncounter(p);
            inwardReportControllerBht.process();
            p.setTransTotal(inwardReportControllerBht.getNetTotal());

            if (p.getFinalBill() != null) {
                setTotal(getTotal() + p.getFinalBill().getNetTotal());
                paid += p.getFinalBill().getPaidAmount();
            }

            creditUsed += p.getCreditUsedAmount();
            creditPaid += p.getPaidByCreditCompany();
            calTotal += p.getTransTotal();
        }
    }

    public void calTotalDischargedNoChanges() {
        if (patientEncounters == null) {
            return;
        }

        total = 0.0;
        totalVat = 0.0;
        totalVatCalculatedValue = 0.0;
        paid = 0;
        calTotal = 0;
        creditPaid = 0;
        creditUsed = 0;
        for (PatientEncounter p : patientEncounters) {
            p.setTransPaidByPatient(calPaidByPatient(p));
            p.setTransPaidByCompany(calPaidByCompany(p));
            if (p.getFinalBill() == null) {
                continue;
            }
            for (BillItem bi : p.getFinalBill().getBillItems()) {
                if (bi.getInwardChargeType() == InwardChargeType.VAT) {
                    p.getFinalBill().setVat(bi.getNetValue() + p.getFinalBill().getVat());
                }
                if (bi.getInwardChargeType() != InwardChargeType.VAT && bi.getInwardChargeType() != InwardChargeType.Medicine) {
                    p.getFinalBill().setVatCalulatedAmount(bi.getNetValue() + p.getFinalBill().getVatCalulatedAmount());
                }
            }

            total += p.getFinalBill().getNetTotal();
            totalVat += p.getFinalBill().getVat();
            totalVatCalculatedValue += p.getFinalBill().getVatCalulatedAmount();
            paid += p.getTransPaidByPatient();
            creditPaid += p.getTransPaidByCompany();
        }
    }

    private double calPaidByPatient(PatientEncounter patientEncounter) {
        Map m = new HashMap();
        String sql = "select sum(b.netTotal) from Bill b "
                + " where b.patientEncounter=:pe"
                + " and b.billType=:btp "
                + " and b.createdAt <= :td ";

        m.put("btp", BillType.InwardPaymentBill);
        m.put("td", toDate);
        m.put("pe", patientEncounter);
        return getPeFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    private double calPaidByCompany(PatientEncounter patientEncounter) {
        Map m = new HashMap();
        String sql = "select sum(b.netValue) "
                + "  from BillItem b "
                + " where b.patientEncounter=:pe"
                + " and b.bill.billType=:btp "
                + " and b.bill.createdAt <= :td ";

        m.put("btp", BillType.CashRecieveBill);
        m.put("td", toDate);
        m.put("pe", patientEncounter);
        return getPeFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public void fillDischargeBookPaymentNotFinalized() {
        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                + " and b.paymentFinalized=false "
                + " and b.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad ";
            m.put("ad", admissionType);
        }

        if (institution != null) {
            sql += " and b.creditCompany =:ins ";
            m.put("ins", institution);
        }

        if (paymentMethod != null) {
            sql += " and b.paymentMethod =:pm ";
            m.put("pm", paymentMethod);
        }

        if (reportKeyWord.getStaff() != null) {
            sql += " and b.referringDoctor =:refDoc ";
            m.put("refDoc", reportKeyWord.getStaff());
        }

        sql += " order by  b.dateOfDischarge";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        calTotalDischarged();
    }

    public void fillDischargeBookPaymentFinalized() {
        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                + " and b.paymentFinalized=true "
                + " and b.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad ";
            m.put("ad", admissionType);
        }

        if (institution != null) {
            sql += " and b.creditCompany =:ins ";
            m.put("ins", institution);
        }

        if (paymentMethod != null) {
            sql += " and b.paymentMethod =:pm ";
            m.put("pm", paymentMethod);
        }

        if (reportKeyWord.getStaff() != null) {
            sql += " and b.referringDoctor =:refDoc ";
            m.put("refDoc", reportKeyWord.getStaff());
        }

        sql += " order by  b.dateOfDischarge";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        calTotalDischarged();
    }

    public void fillDischargeBookPaymentFinalizedNoChanges() {
        Date startTime = new Date();

        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                //                + " and b.paymentFinalized=true "
                + " and b.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad ";
            m.put("ad", admissionType);
        }

        if (institution != null) {
            sql += " and b.creditCompany =:ins ";
            m.put("ins", institution);
        }

        if (paymentMethod != null) {
            sql += " and b.paymentMethod =:pm ";
            m.put("pm", paymentMethod);
        }

        sql += " order by  b.dateOfDischarge";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        calTotalDischargedNoChanges();

    }

    public void fillDischargeBookPaymentFinalizedNoChangesOnlyDue() {
        Date startTime = new Date();

        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                + " and b.paymentFinalized=true "
                + " and b.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad ";
            m.put("ad", admissionType);
        }

        if (institution != null) {
            sql += " and b.creditCompany =:ins ";
            m.put("ins", institution);
        }

        if (paymentMethod != null) {
            sql += " and b.paymentMethod =:pm ";
            m.put("pm", paymentMethod);
        }

        sql += " order by  b.dateOfDischarge";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        calTotalDischargedNoChanges();

        List<PatientEncounter> list = patientEncounters;
        patientEncounters = null;
        patientEncounters = new ArrayList<>();
        setTotal(0);
        paid = 0;
        calTotal = 0;
        creditPaid = 0;
        creditUsed = 0;
        for (PatientEncounter p : list) {
            if (p.getFinalBill() == null) {
                continue;
            }
            p.setTransPaidByPatient(calPaidByPatient(p));
            p.setTransPaidByCompany(calPaidByCompany(p));

            double paidValue = p.getTransPaidByPatient() + p.getTransPaidByCompany();
            double dueValue = p.getFinalBill().getNetTotal() - paidValue;

            if (Math.round(dueValue) != 0) {
                setTotal(getTotal() + p.getFinalBill().getNetTotal());
                paid += p.getTransPaidByPatient();
                creditPaid += p.getTransPaidByCompany();

                patientEncounters.add(p);
            }

        }

    }

    public void makeListNull() {
        billItems = null;
    }

    public void updateOutSideBill(BillItem bi) {
        if (bi.getBill().isPaid()) {
            if (bi.getDescreption() == null || bi.getDescreption().equals("")) {
                JsfUtil.addErrorMessage("Please Enter Memo");
                return;
            }
            if (bi.getBill().getEditedAt() == null && bi.getBill().getEditor() == null) {
                bi.getBill().setEditor(getSessionController().getLoggedUser());
                bi.getBill().setEditedAt(new Date());
                getBillFacade().edit(bi.getBill());
                getBillItemFacade().edit(bi);
                JsfUtil.addSuccessMessage("This Bill Mark as Paid");
            } else {
                JsfUtil.addErrorMessage("Alreddy Mark as Paid");
            }
        } else {
            bi.getBill().setEditor(null);
            bi.getBill().setEditedAt(null);
            getBillFacade().edit(bi.getBill());
            bi.setDescreption("");
            getBillItemFacade().edit(bi);
            JsfUtil.addSuccessMessage("This Bill Mark as Un Paid");
        }
    }

    public void createOutSideBills() {
        Date startTime = new Date();

        makeListNull();
        String sql;
        Map temMap = new HashMap();
        sql = "select b from BillItem b"
                + " where b.bill.billType = :billType "
                + " and b.retired=false "
                + " and b.bill.retired=false ";

        if (reportKeyWord.getString().equals("0")) {
            sql += " and b.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
            temMap.put("toDate", toDate);
            temMap.put("fromDate", fromDate);
        }

        if (reportKeyWord.getString().equals("1")) {
            sql += " and b.bill.createdAt between :fromDate and :toDate ";
            temMap.put("toDate", toDate);
            temMap.put("fromDate", fromDate);
        }

        if (reportKeyWord.getString1().equals("0")) {
            sql += " and b.bill.paid!=true ";
        }

        if (reportKeyWord.getString1().equals("1")) {
            sql += " and b.bill.paid=true ";
        }

        if (institution != null) {
            sql += " and b.bill.fromInstitution=:ins ";
            temMap.put("ins", institution);
        }

        temMap.put("billType", BillType.InwardOutSideBill);

        billItems = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        if (billItems == null) {
            billItems = new ArrayList<>();
        }

        total = 0.0;
        for (BillItem b : billItems) {
            total += b.getBill().getNetTotal();
        }

    }

//    public void createOutSideBillsByAddedDate() {
//        Date startTime = new Date();
//
//        makeListNull();
//        String sql;
//        Map temMap = new HashMap();
//        sql = "select b from BillItem b"
//                + " where b.bill.billType = :billType "
//                + " and b.bill.createdAt between :fromDate and :toDate "
//                + " and b.retired=false "
//                + " and b.bill.retired=false ";
//
//        if (institution != null) {
//            sql += " and b.bill.fromInstitution=:ins ";
//            temMap.put("ins", institution);
//        }
//
//        temMap.put("billType", BillType.InwardOutSideBill);
//        temMap.put("toDate", toDate);
//        temMap.put("fromDate", fromDate);
//
//        billItems = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
//
//        if (billItems == null) {
//            billItems = new ArrayList<>();
//
//        }
//
//        setTotal(0.0);
//        for (BillItem b : billItems) {
//            setTotal(getTotal() + b.getBill().getNetTotal());
//        }
//
//
//
//    }
//
//    public void createOutSideBillsByDischargeDate() {
//
//        Date startTime = new Date();
//
//        makeListNull();
//        String sql;
//        Map temMap = new HashMap();
//        sql = "select b from BillItem b"
//                + " where b.bill.billType = :billType "
//                + " and b.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate "
//                + " and b.retired=false "
//                + " and b.bill.retired=false ";
//
//        if (institution != null) {
//            sql += " and b.bill.fromInstitution=:ins ";
//            temMap.put("ins", institution);
//        }
//
//        temMap.put("billType", BillType.InwardOutSideBill);
//        temMap.put("toDate", toDate);
//        temMap.put("fromDate", fromDate);
//
//        billItems = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
//
//        if (billItems == null) {
//            billItems = new ArrayList<>();
//
//        }
//
//        setTotal(0.0);
//        for (BillItem b : billItems) {
//            setTotal(getTotal() + b.getBill().getNetTotal());
//        }
//
//
//
//    }
    public void createPatientInvestigationsTableAll() {
        Date startTime = new Date();

        String sql = "select pi from PatientInvestigation pi join pi.investigation  "
                + " i join pi.billItem.bill b join b.patient.person p where "
                + " b.createdAt between :fromDate and :toDate  "
                + "and pi.encounter is not null ";

        Map temMap = new HashMap();

        if (patientEncounter != null) {
            sql += "and pi.encounter=:en";
            temMap.put("en", patientEncounter);
        }

        if (getPatientCode() != null && !getPatientCode().trim().equals("")) {
            sql += " and  (((pi.billItem.bill.patientEncounter.patient.code) =:number ) or ((pi.billItem.bill..patientEncounter.patient.phn) =:number )) ";
            temMap.put("number", getPatientCode().trim().toUpperCase());
        }
//
        sql += " order by pi.id desc  ";
//

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        //System.err.println("Sql " + sql);
        patientInvestigations = getPatientInvestigationFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public Admission getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(Admission patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public PatientInvestigationFacade getPatientInvestigationFacade() {
        return patientInvestigationFacade;
    }

    public void setPatientInvestigationFacade(PatientInvestigationFacade patientInvestigationFacade) {
        this.patientInvestigationFacade = patientInvestigationFacade;
    }

    public List<PatientInvestigation> getPatientInvestigations() {
        return patientInvestigations;
    }

    public void setPatientInvestigations(List<PatientInvestigation> patientInvestigations) {
        this.patientInvestigations = patientInvestigations;
    }

    public BhtSummeryController getBhtSummeryController() {
        return bhtSummeryController;
    }

    public void setBhtSummeryController(BhtSummeryController bhtSummeryController) {
        this.bhtSummeryController = bhtSummeryController;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getPaid() {
        return paid;
    }

    public void setPaid(double paid) {
        this.paid = paid;
    }

    public List<IncomeByCategoryRecord> getIncomeByCategoryRecords() {
        return incomeByCategoryRecords;
    }

    public void setIncomeByCategoryRecords(List<IncomeByCategoryRecord> incomeByCategoryRecords) {
        this.incomeByCategoryRecords = incomeByCategoryRecords;
    }

    public void listBhtViceIncome() {
        Date startTime = new Date();

        String sql;
        individualBhtIncomeByCategoryRecord = new ArrayList<>();
        grossTotals = 0.0;
        netTotals = 0.0;
        discounts = 0.0;
        Map m = new HashMap();
        sql = "select pe, category,"
                + " bf.billItem.item.inwardChargeType, "
                + " sum(bf.feeGrossValue), sum(bf.feeDiscount),"
                + " sum(bf.feeValue) "
                + "from BillFee bf "
                + "join bf.billItem.item.category as category "
                + "join bf.bill.patientEncounter as pe "
                + "where "
                + "pe is not null and "
                + "bf.bill.billType=:billType and "
                + "pe.dateOfDischarge between :fd and :td ";
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("billType", BillType.InwardBill);

        sql = sql + " group by pe.id, category.name, bf.billItem.item.inwardChargeType ";
        sql = sql + " order by pe.id, bf.billItem.item.inwardChargeType, category.name";

//        Item item;
//        item.getInwardChargeType()
        List<Object[]> results = getPeFacade().findAggregates(sql, m, TemporalType.DATE);

//        PatientEncounter pe = new PatientEncounter();
//        pe.getAdmissionType();
        if (results == null) {
            return;
        }

        for (Object[] objs : results) {
            IndividualBhtIncomeByCategoryRecord ibr = new IndividualBhtIncomeByCategoryRecord();
            PatientEncounter pe = (PatientEncounter) objs[0];
            Category cat = (Category) objs[1];
            InwardChargeType ict = (InwardChargeType) objs[2];
            ibr.setBht(pe);
            ibr.setFinalBill(pe.getFinalBill());
            ibr.setCategory(cat);
            ibr.setInwardChargeType(ict);
            ibr.setGrossValue((Double) objs[3]);
            ibr.setDiscount((Double) objs[4]);
            ibr.setNetValue((Double) objs[5]);

            grossTotals = grossTotals + ibr.getGrossValue();
            discounts = discounts + ibr.getDiscount();
            netTotals = netTotals + ibr.getNetValue();

            individualBhtIncomeByCategoryRecord.add(ibr);
        }

    }

    public void listDischargedBhtIncomeByCategories() {
        String sql;
        incomeByCategoryRecords = new ArrayList<>();
        grossTotals = 0.0;
        netTotals = 0.0;
        discounts = 0.0;
        Map m = new HashMap();
        sql = "select bf.billItem.item.category, "
                + " sum(bf.feeDiscount),"
                + " sum(bf.feeMargin),"
                + " sum(bf.feeGrossValue),"
                + " sum(bf.feeValue)"
                + " from BillFee bf where"
                + " bf.bill.patientEncounter is not null"
                + " and bf.bill.patientEncounter.discharged=true ";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("billType", BillType.InwardBill);
        sql = sql + " and bf.bill.billType=:billType and"
                + " bf.bill.patientEncounter.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql = sql + " and bf.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and bf.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and bf.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", institution);
        }

        sql = sql + " group by bf.billItem.item.category order by bf.billItem.item.category.name";
        List<Object[]> results = getPeFacade().findAggregates(sql, m, TemporalType.DATE);

//        PatientEncounter pe = new PatientEncounter();
//        pe.getAdmissionType();
        if (results == null) {
            return;
        }

        for (Object[] objs : results) {

            IncomeByCategoryRecord ibr = new IncomeByCategoryRecord();
            ibr.setCategory((Category) objs[0]);
            ibr.setDiscount((double) objs[1]);
            ibr.setMatrix((double) objs[2]);
            ibr.setGrossAmount((double) objs[3]);
            ibr.setNetAmount((double) objs[4]);

            grossTotals = grossTotals + ibr.getGrossAmount();
            discounts = discounts + ibr.getDiscount();
            netTotals = netTotals + ibr.getNetAmount();

            incomeByCategoryRecords.add(ibr);

        }

    }

    public void fillProfessionalPaymentDone() {
        Date startTime = new Date();

        billedBill = createBilledBillProfessionalPaymentTableInwardAll(new BilledBill());
        cancelledBill = createCancelBillRefundBillProfessionalPaymentTableInwardAll(new CancelledBill());
        refundBill = createCancelBillRefundBillProfessionalPaymentTableInwardAll(new RefundBill());

        totalBilledBill = calTotalCreateBilledBillProfessionalPaymentTableInwardAll(new BilledBill());
        totalCancelledBill = calTotalCreateCancelBillRefundBillProfessionalPaymentTableInwardAll(new CancelledBill());
        totalRefundBill = calTotalCreateCancelBillRefundBillProfessionalPaymentTableInwardAll(new RefundBill());

    }

    public void fillProfessionalPaymentDoneOPD() {
        Date startTime = new Date();

        BillType[] bts = {BillType.OpdBill};
        List<BillType> billTypes = Arrays.asList(bts);
        billedBill = createProfessionalPaymentTable(new BilledBill(), BillType.PaymentBill, billTypes);
        cancelledBill = createProfessionalPaymentTable(new CancelledBill(), BillType.PaymentBill, null);
        refundBill = createProfessionalPaymentTable(new RefundBill(), BillType.PaymentBill, null);

        totalBilledBill = createProfessionalPaymentTableTotals(new BilledBill(), BillType.PaymentBill, billTypes);
        totalCancelledBill = createProfessionalPaymentTableTotals(new CancelledBill(), BillType.PaymentBill, null);
        totalRefundBill = createProfessionalPaymentTableTotals(new RefundBill(), BillType.PaymentBill, null);

    }

    List<BillItem> createBilledBillProfessionalPaymentTableInwardAll(Bill bill) {
        billItems = null;
        HashMap temMap = new HashMap();
        temMap.put("bclass", bill.getClass());
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
        temMap.put("refType", BillType.InwardBill);
        temMap.put("refType2", BillType.InwardProfessional);
        String sql = "Select b FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                //                + " and b.bill.cancelled=false "
                + " and type(b.bill)=:bclass"
                + " and (b.referenceBill.billType=:refType "
                + " or b.referenceBill.billType=:refType2) "
                + " and b.createdAt between :fromDate and :toDate ";

        if (admissionType != null) {
            sql = sql + " and (b.referenceBill.billType=:refType "
                    + " or b.referenceBill.billType=:refType2) ";
            temMap.put("at", admissionType);
        }

        if (admissionType != null) {
            sql = sql + " and b.bill.patientEncounter.admissionType=:at ";
            temMap.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql = sql + " and b.bill.patientEncounter.paymentMethod=:bt ";
            temMap.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.bill.patientEncounter.creditCompany=:cc ";
            temMap.put("cc", institution);
        }

        return getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    List<BillItem> createCancelBillRefundBillProfessionalPaymentTableInwardAll(Bill bill) {
        billItems = null;
        HashMap temMap = new HashMap();
        temMap.put("bclass", bill.getClass());
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
//        temMap.put("refType", BillType.InwardBill);
//        temMap.put("refType2", BillType.InwardProfessional);
        String sql = "Select b FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.paidForBillFee.bill.patientEncounter is not null"
                + " and type(b.bill)=:bclass"
                //                + " and (b.bill.billedBill.referenceBill.billType=:refType "
                //                + " or b.bill.billedBill.referenceBill.billType=:refType2) "
                + " and b.createdAt between :fromDate and :toDate ";

        if (admissionType != null) {
            sql = sql + " and b.bill.patientEncounter.admissionType=:at ";
            temMap.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql = sql + " and b.bill.patientEncounter.paymentMethod=:bt ";
            temMap.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.bill.patientEncounter.creditCompany=:cc ";
            temMap.put("cc", institution);
        }

        return getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    List<BillItem> createProfessionalPaymentTable(Bill bill, BillType bt, List<BillType> billTypes) {
        billItems = null;
        HashMap temMap = new HashMap();
        temMap.put("bclass", bill.getClass());
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bt", bt);
        String sql = " Select b FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bt "
                + " and type(b.bill)=:bclass "
                + " and b.paidForBillFee.bill.patientEncounter is null "
                + " and b.createdAt between :fromDate and :toDate ";

        if (paymentMethod != null) {
            sql = sql + " and b.paidForBillFee.bill.paymentMethod=:pm ";
            temMap.put("pm", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.paidForBillFee.bill.creditCompany=:cc ";
            temMap.put("cc", institution);
        }
        if (billTypes != null) {
            sql += " and b.referenceBill.billType in :bts ";
            temMap.put("bts", billTypes);
        }

        return getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public double createProfessionalPaymentTableTotals(Bill bill, BillType bt, List<BillType> billTypes) {
        billItems = null;
        HashMap temMap = new HashMap();
        temMap.put("bclass", bill.getClass());
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bt", bt);
        String sql = " Select sum(b.netValue) FROM BillItem b  "
                + " where b.retired=false "
                + " and b.bill.billType=:bt "
                + " and type(b.bill)=:bclass"
                + " and b.paidForBillFee.bill.patientEncounter is null "
                + " and b.createdAt between :fromDate and :toDate ";

        if (paymentMethod != null) {
            sql = sql + " and b.paidForBillFee.bill.paymentMethod=:pm ";
            temMap.put("pm", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.paidForBillFee.bill.creditCompany=:cc ";
            temMap.put("cc", institution);
        }

        if (billTypes != null) {
            sql += " and b.referenceBill.billType in :bts ";
            temMap.put("bts", billTypes);
        }

        return getBillItemFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public double calTotalCreateBilledBillProfessionalPaymentTableInwardAll(Bill bill) {
        billItems = null;
        HashMap temMap = new HashMap();
        temMap.put("bclass", bill.getClass());
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
        temMap.put("refType", BillType.InwardBill);
        temMap.put("refType2", BillType.InwardProfessional);
        String sql = "Select sum(b.netValue) FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                //                + " and b.bill.cancelled=false "
                + " and type(b.bill)=:bclass"
                + " and (b.referenceBill.billType=:refType "
                + " or b.referenceBill.billType=:refType2) "
                + " and b.createdAt between :fromDate and :toDate ";

        if (admissionType != null) {
            sql = sql + " and b.bill.patientEncounter.admissionType=:at ";
            temMap.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql = sql + " and b.bill.patientEncounter.paymentMethod=:bt ";
            temMap.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.bill.patientEncounter.creditCompany=:cc ";
            temMap.put("cc", institution);
        }

        return getBillItemFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public double calTotalCreateCancelBillRefundBillProfessionalPaymentTableInwardAll(Bill bill) {
        billItems = null;
        HashMap temMap = new HashMap();
        temMap.put("bclass", bill.getClass());
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
//        temMap.put("refType", BillType.InwardBill);
//        temMap.put("refType2", BillType.InwardProfessional);
        String sql = "Select sum(b.netValue) FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.paidForBillFee.bill.patientEncounter is not null"
                + " and type(b.bill)=:bclass"
                //                + " and (b.bill.billedBill.referenceBill.billType=:refType "
                //                + " or b.bill.billedBill.referenceBill.billType=:refType2) "
                + " and b.createdAt between :fromDate and :toDate ";

        if (admissionType != null) {
            sql = sql + " and b.bill.patientEncounter.admissionType=:at ";
            temMap.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql = sql + " and b.bill.patientEncounter.paymentMethod=:bt ";
            temMap.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.bill.patientEncounter.creditCompany=:cc ";
            temMap.put("cc", institution);
        }

        return getBillItemFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public void listnerDeveloperCheckBox() {
        if (developers) {
            dischargeDate = false;
            bhtNo = false;
            paymentMethord = false;
            creditCompany = false;
            person = false;
            guardian = false;
            room = false;
            refDoctor = false;
            AddmitDetails = false;
            billedBy = false;
            finalBillTotal = false;
            paidByPatient = false;
            creditPaidAmount = false;
            dueAmount = false;
            calculatedAmount = false;
            differentAmount = false;
        } else {
            dischargeDate = true;
            bhtNo = true;
            paymentMethord = true;
            creditCompany = true;
            person = true;
            guardian = true;
            room = true;
            refDoctor = true;
            AddmitDetails = true;
            billedBy = true;
            finalBillTotal = true;
            paidByPatient = true;
            creditPaidAmount = true;
            dueAmount = true;
            calculatedAmount = true;
            differentAmount = true;
        }
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    @Deprecated
    public List<AdmissionType> getAdmissionty() {
        admissionty = getAdmissionTypeFacade().findAll("name", true);
        return admissionty;
    }

    public void setAdmissionty(List<AdmissionType> admissionty) {
        this.admissionty = admissionty;
    }

    public AdmissionTypeFacade getAdmissionTypeFacade() {
        return admissionTypeFacade;
    }

    public void setAdmissionTypeFacade(AdmissionTypeFacade admissionTypeFacade) {
        this.admissionTypeFacade = admissionTypeFacade;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = com.divudi.core.util.CommonFunctions.getStartOfMonth(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public List<BillItem> getBilledBill() {
        return billedBill;
    }

    public void setBilledBill(List<BillItem> billedBill) {
        this.billedBill = billedBill;
    }

    public List<BillItem> getCancelledBill() {
        return cancelledBill;
    }

    public void setCancelledBill(List<BillItem> cancelledBill) {
        this.cancelledBill = cancelledBill;
    }

    public List<BillItem> getRefundBill() {
        return refundBill;
    }

    public void setRefundBill(List<BillItem> refundBill) {
        this.refundBill = refundBill;
    }

    public double getTotalBilledBill() {
        return totalBilledBill;
    }

    public void setTotalBilledBill(double totalBilledBill) {
        this.totalBilledBill = totalBilledBill;
    }

    public double getTotalCancelledBill() {
        return totalCancelledBill;
    }

    public void setTotalCancelledBill(double totalCancelledBill) {
        this.totalCancelledBill = totalCancelledBill;
    }

    public double getTotalRefundBill() {
        return totalRefundBill;
    }

    public void setTotalRefundBill(double totalRefundBill) {
        this.totalRefundBill = totalRefundBill;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = com.divudi.core.util.CommonFunctions.getEndOfMonth(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PatientEncounterFacade getPeFacade() {
        return peFacade;
    }

    public void setPeFacade(PatientEncounterFacade peFacade) {
        this.peFacade = peFacade;
    }

    public double getGrossTotals() {
        return grossTotals;
    }

    public void setGrossTotals(double grossTotals) {
        this.grossTotals = grossTotals;
    }

    public double getDiscounts() {
        return discounts;
    }

    public void setDiscounts(double discounts) {
        this.discounts = discounts;
    }

    public double getNetTotals() {
        return netTotals;
    }

    public void setNetTotals(double netTotals) {
        this.netTotals = netTotals;
    }

    public List<IndividualBhtIncomeByCategoryRecord> getIndividualBhtIncomeByCategoryRecord() {
        return individualBhtIncomeByCategoryRecord;
    }

    public void setIndividualBhtIncomeByCategoryRecord(List<IndividualBhtIncomeByCategoryRecord> individualBhtIncomeByCategoryRecord) {
        this.individualBhtIncomeByCategoryRecord = individualBhtIncomeByCategoryRecord;
    }

    public boolean isWithFooter() {
        return withFooter;
    }

    public void setWithFooter(boolean withFooter) {
        this.withFooter = withFooter;
    }

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public List<AdmissionType> getAdmissionTypes() {
        if (admissionTypes == null) {
            fillAdmissionTypes();
        }
        return admissionTypes;
    }

    public void setAdmissionTypes(List<AdmissionType> admissionTypes) {
        this.admissionTypes = admissionTypes;
    }

    private void fillAdmissionTypes() {
        String jpql = "select ad from AdmissionType ad "
                + "where ad.retired=false "
                + "order by ad.name";
        admissionTypes = admissionTypeFacade.findByJpql(jpql);
    }

    public String getPatientCode() {
        return patientCode;
    }

    public void setPatientCode(String patientCode) {
        this.patientCode = patientCode;
    }

    public Date getFromYearStartDate() {
        if (fromYearStartDate == null) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MONTH, Calendar.JANUARY);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            fromYearStartDate = cal.getTime();
        }
        return fromYearStartDate;
    }

    public void setFromYearStartDate(Date fromYearStartDate) {
        this.fromYearStartDate = fromYearStartDate;
    }

    public Date getToYearEndDate() {
        if (toYearEndDate == null) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MONTH, Calendar.DECEMBER);
            cal.set(Calendar.DAY_OF_MONTH, 31);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);

            toYearEndDate = cal.getTime();
        }
        return toYearEndDate;
    }

    public void setToYearEndDate(Date toYearEndDate) {
        this.toYearEndDate = toYearEndDate;
    }

    public List<SurgeryCountDoctorWiseDTO> getBillList() {
        return billList;
    }

    public void setBillList(List<SurgeryCountDoctorWiseDTO> billList) {
        this.billList = billList;
    }

    public Speciality getCurrentSpeciality() {
        return currentSpeciality;
    }

    public void setCurrentSpeciality(Speciality currentSpeciality) {
        this.currentSpeciality = currentSpeciality;
    }

    public LineChartModel getLineChartModel() {
        return lineChartModel;
    }

    public void setLineChartModel(LineChartModel lineChartModel) {
        this.lineChartModel = lineChartModel;
    }

    public BarChartModel getBarChartModel() {
        return barChartModel;
    }

    public void setBarChartModel(BarChartModel barChartModel) {
        this.barChartModel = barChartModel;
    }

    public LineChartModel getSpecialtyLineChartModel() {
        return specialtyLineChartModel;
    }

    public void setSpecialtyLineChartModel(LineChartModel specialtyLineChartModel) {
        this.specialtyLineChartModel = specialtyLineChartModel;
    }

    public BarChartModel getSpecialtyBarChartModel() {
        return specialtyBarChartModel;
    }

    public void setSpecialtyBarChartModel(BarChartModel specialtyBarChartModel) {
        this.specialtyBarChartModel = specialtyBarChartModel;
    }

    public class IncomeByCategoryRecord {

        Category category;
        Category subCategory;
        double grossAmount;
        double discount;
        double matrix;
        double netAmount;

        public Category getCategory() {
            return category;
        }

        public void setCategory(Category category) {
            this.category = category;
        }

        public Category getSubCategory() {
            return subCategory;
        }

        public void setSubCategory(Category subCategory) {
            this.subCategory = subCategory;
        }

        public double getGrossAmount() {
            return grossAmount;
        }

        public void setGrossAmount(double grossAmount) {
            this.grossAmount = grossAmount;
        }

        public double getMatrix() {
            return matrix;
        }

        public void setMatrix(double matrix) {
            this.matrix = matrix;
        }

        public double getDiscount() {
            return discount;
        }

        public void setDiscount(double discount) {
            this.discount = discount;
        }

        public double getNetAmount() {
            return netAmount;
        }

        public void setNetAmount(double netAmount) {
            this.netAmount = netAmount;
        }

    }

    public class IndividualBhtIncomeByCategoryRecord {

        PatientEncounter bht;
        Bill finalBill;
        Category category;
        Category subCategory;
        InwardChargeType inwardChargeType;
        double grossValue;
        double discount;
        double inwardAddition;
        double netValue;

        public PatientEncounter getBht() {
            return bht;
        }

        public void setBht(PatientEncounter bht) {
            this.bht = bht;
        }

        public Bill getFinalBill() {
            return finalBill;
        }

        public void setFinalBill(Bill finalBill) {
            this.finalBill = finalBill;
        }

        public Category getCategory() {
            return category;
        }

        public void setCategory(Category category) {
            this.category = category;
        }

        public Category getSubCategory() {
            return subCategory;
        }

        public void setSubCategory(Category subCategory) {
            this.subCategory = subCategory;
        }

        public InwardChargeType getInwardChargeType() {
            return inwardChargeType;
        }

        public void setInwardChargeType(InwardChargeType inwardChargeType) {
            this.inwardChargeType = inwardChargeType;
        }

        public double getGrossValue() {
            return grossValue;
        }

        public void setGrossValue(double grossValue) {
            this.grossValue = grossValue;
        }

        public double getDiscount() {
            return discount;
        }

        public void setDiscount(double discount) {
            this.discount = discount;
        }

        public double getInwardAddition() {
            return inwardAddition;
        }

        public void setInwardAddition(double inwardAddition) {
            this.inwardAddition = inwardAddition;
        }

        public double getNetValue() {
            return netValue;
        }

        public void setNetValue(double netValue) {
            this.netValue = netValue;
        }

    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public double getNetPaid() {
        return netPaid;
    }

    public void setNetPaid(double netPaid) {
        this.netPaid = netPaid;
    }

    public double getCalTotal() {
        return calTotal;
    }

    public void setCalTotal(double calTotal) {
        this.calTotal = calTotal;
    }

    public double getCreditPaid() {
        return creditPaid;
    }

    public void setCreditPaid(double creditPaid) {
        this.creditPaid = creditPaid;
    }

    public double getCreditUsed() {
        return creditUsed;
    }

    public void setCreditUsed(double creditUsed) {
        this.creditUsed = creditUsed;
    }

    public InwardReportControllerBht getInwardReportControllerBht() {
        return inwardReportControllerBht;
    }

    public void setInwardReportControllerBht(InwardReportControllerBht inwardReportControllerBht) {
        this.inwardReportControllerBht = inwardReportControllerBht;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public boolean isDischargeDate() {
        return dischargeDate;
    }

    public void setDischargeDate(boolean dischargeDate) {
        this.dischargeDate = dischargeDate;
    }

    public boolean isBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(boolean bhtNo) {
        this.bhtNo = bhtNo;
    }

    public boolean isPaymentMethord() {
        return paymentMethord;
    }

    public void setPaymentMethord(boolean paymentMethord) {
        this.paymentMethord = paymentMethord;
    }

    public boolean isCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(boolean creditCompany) {
        this.creditCompany = creditCompany;
    }

    public boolean isPerson() {
        return person;
    }

    public void setPerson(boolean person) {
        this.person = person;
    }

    public boolean isRoom() {
        return room;
    }

    public void setRoom(boolean room) {
        this.room = room;
    }

    public boolean isRefDoctor() {
        return refDoctor;
    }

    public void setRefDoctor(boolean refDoctor) {
        this.refDoctor = refDoctor;
    }

    public boolean isAddmitDetails() {
        return AddmitDetails;
    }

    public void setAddmitDetails(boolean AddmitDetails) {
        this.AddmitDetails = AddmitDetails;
    }

    public boolean isBilledBy() {
        return billedBy;
    }

    public void setBilledBy(boolean billedBy) {
        this.billedBy = billedBy;
    }

    public boolean isFinalBillTotal() {
        return finalBillTotal;
    }

    public void setFinalBillTotal(boolean finalBillTotal) {
        this.finalBillTotal = finalBillTotal;
    }

    public boolean isPaidByPatient() {
        return paidByPatient;
    }

    public void setPaidByPatient(boolean paidByPatient) {
        this.paidByPatient = paidByPatient;
    }

    public boolean isCreditPaidAmount() {
        return creditPaidAmount;
    }

    public void setCreditPaidAmount(boolean creditPaidAmount) {
        this.creditPaidAmount = creditPaidAmount;
    }

    public boolean isDueAmount() {
        return dueAmount;
    }

    public void setDueAmount(boolean dueAmount) {
        this.dueAmount = dueAmount;
    }

    public boolean isCalculatedAmount() {
        return calculatedAmount;
    }

    public void setCalculatedAmount(boolean calculatedAmount) {
        this.calculatedAmount = calculatedAmount;
    }

    public boolean isDifferentAmount() {
        return differentAmount;
    }

    public void setDifferentAmount(boolean differentAmount) {
        this.differentAmount = differentAmount;
    }

    public boolean isGuardian() {
        return guardian;
    }

    public void setGuardian(boolean guardian) {
        this.guardian = guardian;
    }

    public boolean isDevelopers() {
        return developers;
    }

    public void setDevelopers(boolean developers) {
        this.developers = developers;
    }

    public boolean isWithoutCancelBHT() {
        return withoutCancelBHT;
    }

    public void setWithoutCancelBHT(boolean withoutCancelBHT) {
        this.withoutCancelBHT = withoutCancelBHT;
    }

    public String getInvoceNo() {
        return invoceNo;
    }

    public void setInvoceNo(String invoceNo) {
        this.invoceNo = invoceNo;
    }

    public double getTotalVat() {
        return totalVat;
    }

    public void setTotalVat(double totalVat) {
        this.totalVat = totalVat;
    }

    public double getTotalVatCalculatedValue() {
        return totalVatCalculatedValue;
    }

    public void setTotalVatCalculatedValue(double totalVatCalculatedValue) {
        this.totalVatCalculatedValue = totalVatCalculatedValue;
    }

    public String getVatRegNo() {
        return vatRegNo;
    }

    public void setVatRegNo(String vatRegNo) {
        this.vatRegNo = vatRegNo;
    }

}
