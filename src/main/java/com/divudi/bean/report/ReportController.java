package com.divudi.bean.report;

import com.divudi.bean.common.DoctorController;
import com.divudi.bean.common.InstitutionController;
import com.divudi.bean.common.ItemApplicationController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.PatientController;
import com.divudi.bean.common.PersonController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillItemStatus;
import com.divudi.data.BillType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.CategoryCount;
import com.divudi.data.ItemCount;
import com.divudi.data.ItemLight;
import com.divudi.data.PaymentMethod;
import com.divudi.data.Sex;
import com.divudi.data.TestWiseCountReport;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Doctor;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.Person;
import com.divudi.entity.Route;
import com.divudi.entity.Service;
import com.divudi.entity.Speciality;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.Machine;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.java.CommonFunctions;
import com.divudi.light.common.BillLight;
import com.divudi.light.common.PrescriptionSummaryReportRow;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javax.faces.context.FacesContext;
import javax.persistence.TemporalType;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Senula Nanayakkara
 */
@Named
@SessionScoped
public class ReportController implements Serializable {

    @EJB
    BillItemFacade billItemFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    InstitutionFacade institutionFacade;

    @Inject
    private InstitutionController institutionController;
    @Inject
    DoctorController doctorController;
    @Inject
    PersonController personController;
    @Inject
    ItemApplicationController itemApplicationController;
    @Inject
    ItemController itemController;

    private int reportIndex;
    private Institution institution;
    private Department department;
    private Institution fromInstitution;
    private Institution toInstitution;
    private Department fromDepartment;
    private Department toDepartment;
    private Date fromDate;
    private Date toDate;
    private Category category;
    private Item item;
    private Machine machine;
    private String processBy;
    private Institution collectingCentre;
    private Route route;
    private Date financialYear;
    private String phn;
    private Doctor referingDoctor;

    private double investigationResult;

    private String visitType;
    private Patient patient;
    private String diagnosis;

    private Investigation investigation;
    private Speciality currentSpeciality;
    private Service service;

    private String priorityType;
    private String patientMrn;

    private BillItemStatus status;
    private Doctor doctor;
    private String totalAverage;
    private String visit;

    private List<Bill> bills;
    private List<BillItem> billItems;
    private List<ItemCount> reportLabTestCounts;
    private List<CategoryCount> reportList;
    private List<Institution> collectionCenters;

    private Date warrentyStartDate;
    private Date warrentyEndDate;

    private Date amcStartDate;
    private Date amcEndDate;
    private PaymentMethod paymentMethod;

    private String invoiceNumber;

    private List<ItemLight> investigationsAndServices;
    private ItemLight itemLight;

    private List<PrescriptionSummaryReportRow> prescriptionSummaryReportRows;
    private List<BillLight> billLights;

    private List<ItemCount> reportOpdServiceCount;

    CommonFunctions commonFunctions;

    public void processCollectionCenterBalance() {
        String jpql = "select cc"
                + " from Institution cc"
                + " where cc.retired=:ret"
                + " and cc = :i";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("i", collectingCentre);

        collectionCenters = institutionFacade.findByJpql(jpql, m);
    }

    public String navigateToPrescriptionSummaryReport() {
        return "/pharmacy/prescription_summary_report?faces-redirect=true";
    }

    public String navigateToPrescriptionList() {
        return "/pharmacy/prescription_list?faces-redirect=true";
    }

    private Person person;

    public String navigateToPrescriptionListFromPrescriptionSummery(Long personId) {
        if (personId == null || personId == 0) {
            JsfUtil.addErrorMessage("Error 1");
            return "";
        }

        person = personController.findPerson(personId);

        doctor = doctorController.findDoctor(person);

        if (doctor == null) {
            JsfUtil.addErrorMessage("Error 2");
            return "";
        }

        processPresciptionList();

        return navigateToPrescriptionList();
    }

    public void makeNull() {
        doctor = null;
        prescriptionSummaryReportRows = null;
        fromDate = null;
        toDate = null;
        person = null;
    }

    public void processCollectingCentreBillWiseDetailReport() {
        String jpql = "select bill "
                + " from Bill bill "
                + " where bill.retired=:ret"
                + " and bill.billDate between :fd and :td "
                + " and bill.billType = :bType";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bType", BillType.CollectingCentreBill);

        if (route != null) {
            jpql += " and bill.fromInstitution.route = :route ";
            m.put("route", route);
        }

        if (institution != null) {
            jpql += " and bill.institution = :ins ";
            m.put("ins", institution);
        }

        if (collectingCentre != null) {
            jpql += " and bill.fromInstitution = :cc ";
            m.put("cc", collectingCentre);
        }

        if (toDepartment != null) {
            jpql += " and bill.toDepartment = :dep ";
            m.put("dep", toDepartment);
        }

        if (phn != null && !phn.isEmpty()) {
            jpql += " and bill.patient.phn = :phn ";
            m.put("phn", phn);
        }

        if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
            jpql += " and bill.deptId = :inv ";
            m.put("inv", invoiceNumber);
        }

//        if (itemLight != null) {
//            jpql += " and bi.item.id = :item ";
//            m.put("item", itemLight.getId());
//        }
        if (doctor != null) {
            jpql += " and bill.referredBy = :refDoc ";
            m.put("refDoc", doctor);
        }

//        if (status != null) {
//            jpql += " and billItemStatus = :status ";
//            m.put("status", status);
//        }
        bills = billFacade.findByJpql(jpql, m);
    }

    public ReportController() {
    }

    // Modified by Dr M H B Ariyaratne with assistance from ChatGPT from OpenAI.
    public void processLabTestCount() {
        String jpql = "select new com.divudi.data.ItemCount(bi.item.category.name, bi.item.name, count(bi.item)) "
                + " from BillItem bi "
                + " where bi.bill.cancelled=:can "
                + " and bi.bill.billDate between :fd and :td "
                + " and TYPE(bi.item) = Investigation ";
        Map<String, Object> m = new HashMap<>();
        m.put("can", false);
        m.put("fd", fromDate);
        m.put("td", toDate);

        if (fromInstitution != null) {
            jpql += " and bi.bill.fromInstitution=:fi ";
            m.put("fi", fromInstitution);
        }

        if (toInstitution != null) {
            jpql += " and bi.bill.toInstitution=:ti ";
            m.put("ti", toInstitution);
        }

        if (fromDepartment != null) {
            jpql += " and bi.bill.fromDepartment=:fdept ";
            m.put("fdept", fromDepartment);
        }

        if (machine != null) {
            jpql += " and bi.item.machine=:machine ";
            m.put("machine", machine);
        }

        jpql += " group by bi.item.category.name, bi.item.name ";
        jpql += " order by bi.item.category.name, bi.item.name";

        // Unchecked cast here
        reportLabTestCounts = (List<ItemCount>) billItemFacade.findLightsByJpql(jpql, m);

        Map<String, CategoryCount> categoryReports = new HashMap<>();

        for (ItemCount count : reportLabTestCounts) {
            categoryReports.computeIfAbsent(count.getCategory(), k -> new CategoryCount(k, new ArrayList<>(), 0L))
                    .getItems().add(count);
            categoryReports.get(count.getCategory()).setTotal(categoryReports.get(count.getCategory()).getTotal() + count.getTestCount());
        }

        // Convert the map values to a list to be used in the JSF page
        reportList = new ArrayList<>(categoryReports.values());
    }
    
    public void filterOpdServiceCountBySelectedService(Long selectedItemId){
        if (selectedItemId != null) {
            item=itemController.findItem(selectedItemId);
            doctor=null;
        }
        processOpdServiceCountDoctorWise();
    }
    
    public void filterOpdServiceCountBySelectedDoctor(Long selectedDoctorId){
        if (selectedDoctorId != null) {
            doctor=doctorController.findDoctor(selectedDoctorId);
            item=null;
        }
        processOpdServiceCountDoctorWise();
    }

    public void processOpdServiceCountDoctorWise() {
        List<BillTypeAtomic> billtypes = new ArrayList<>();
        billtypes.add(BillTypeAtomic.OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER);
        
        String jpql = "select new com.divudi.data.ItemCount(bi.bill.fromStaff.person.name, bi.bill.fromStaff.id, bi.item.name, bi.item.id, count(bi)) "
                + " from BillItem bi "
                + " where bi.bill.cancelled=:can "
                + " and bi.bill.billTypeAtomic IN :bt"
                + " and bi.bill.billDate between :fd and :td ";

        Map<String, Object> m = new HashMap<>();
        m.put("can", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", billtypes);
        if (department != null) {
            jpql += " and bi.bill.department=:fdept ";
            m.put("fdept", department);
        }
        if (doctor != null) {
            jpql += " and bi.bill.fromStaff =:fs";
            m.put("fs", doctor);
        }
        if(item != null){
            jpql += " and bi.item =:it";
            m.put("it", item);
        }
        
        jpql += " group by bi.item, bi.bill.fromStaff ";
        jpql += " order by bi.bill.fromStaff.person.name, bi.item.name";
        reportOpdServiceCount = (List<ItemCount>) billItemFacade.findLightsByJpql(jpql, m);
    }

    
    
    public void processCollectingCentreReportsToPrint() {
        String jpql = "select bi "
                + " from BillItem bi "
                + " where bi.retired=:ret"
                + " and bi.bill.billDate between :fd and :td "
                + " and bi.bill.billType = :bType";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bType", BillType.CollectingCentreBill);

        if (route != null) {
            jpql += " and bi.bill.fromInstitution.route = :route ";
            m.put("route", route);
        }

        if (institution != null) {
            jpql += " and bi.bill.institution = :ins ";
            m.put("ins", institution);
        }

        if (collectingCentre != null) {
            jpql += " and bi.bill.fromInstitution = :cc ";
            m.put("cc", collectingCentre);
        }

        if (toDepartment != null) {
            jpql += " and bi.bill.toDepartment = :dep ";
            m.put("dep", toDepartment);
        }

        if (phn != null && !phn.isEmpty()) {
            jpql += " and bi.bill.patient.phn = :phn ";
            m.put("phn", phn);
        }

        if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
            jpql += " and bi.bill.deptId = :inv ";
            m.put("inv", invoiceNumber);
        }

        if (itemLight != null) {
            jpql += " and bi.item.id = :item ";
            m.put("item", itemLight.getId());
        }

        if (doctor != null) {
            jpql += " and bi.bill.referredBy = :refDoc ";
            m.put("refDoc", doctor);
        }

        if (status != null) {
            jpql += " and bi.billItemStatus = :status ";
            m.put("status", status);
        }

        billItems = billItemFacade.findByJpql(jpql, m);
    }

    
    
    
    public void processCollectingCentreStatementReport() {
        String jpql = "select bi "
                + " from BillItem bi "
                + " where bi.retired=:ret"
                + " and bi.bill.billDate between :fd and :td "
                + " and bi.bill.billType = :bType";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bType", BillType.CollectingCentreBill);

//        if (route != null) {
//            jpql += " and bi.bill.fromInstitution.route = :route ";
//            m.put("route", route);
//        }
        if (institution != null) {
            jpql += " and bi.bill.institution = :ins ";
            m.put("ins", institution);
        }

        if (collectingCentre != null) {
            jpql += " and bi.bill.fromInstitution = :cc ";
            m.put("cc", collectingCentre);
        }

//        if (toDepartment != null) {
//            jpql += " and bi.bill.toDepartment = :dep ";
//            m.put("dep", toDepartment);
//        }
//
//        if (phn != null && !phn.isEmpty()) {
//            jpql += " and bi.bill.patient.phn = :phn ";
//            m.put("phn", phn);
//        }
        if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
            jpql += " and bi.bill.deptId = :inv ";
            m.put("inv", invoiceNumber);
        }

//        if (itemLight != null) {
//            jpql += " and bi.item.id = :item ";
//            m.put("item", itemLight.getId());
//        }
//
//        if (doctor != null) {
//            jpql += " and bi.bill.referredBy = :refDoc ";
//            m.put("refDoc", doctor);
//        }
//
//        if (status != null) {
//            jpql += " and bi.billItemStatus = :status ";
//            m.put("status", status);
//        }
        billItems = billItemFacade.findByJpql(jpql, m);
    }

//    
//    public void processPharmacySaleReferralCount() {
//        String jpql = "select new com.divudi.data.BillLight(bi.referredBy.person.name, count(bi), count(bi.netTotal)) "
//                + " from Bill bi "
//                + " where bi.cancelled=:can "
//                + " and bi.createdAt between :fd and :td "
//                + " and bi.billType=:bitype ";
//        Map m = new HashMap();
//
//        m.put("can", false);
//        m.put("fd", fromDate);
//        m.put("td", toDate);
//        m.put("bitype", BillType.PharmacySale);
//
//
//        if (department  != null) {
//            jpql += " and bi.fromDepartment=:fdept ";
//            m.put("fdept", department);
//        }
//        
//        if (referingDoctor  != null) {
//            jpql += " and bi.referredBy=:refDoc ";
//            m.put("refDoc", referingDoctor);
//        }
//        jpql += " group by bi.referredBy.person.name ";
//        jpql += " order by bi.referredBy.person.name ";
//        prescriptionSummaryReportRows = (List<PrescriptionSummaryReportRow>) billFacade.findLightsByJpql(jpql, m);
//    }
//    
    public void processPresciptionSummeryReport() {
        String jpql = "select new "
                + " com.divudi.light.common.PrescriptionSummaryReportRow(bi.referredBy.person.name, bi.referredBy.person.id, count(bi), sum(bi.netTotal)) "
                + " from Bill bi "
                + " where bi.cancelled=:can "
                + " and bi.billDate between :fd and :td "
                + " and bi.billType=:bitype ";
        Map m = new HashMap();

        m.put("can", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bitype", BillType.PharmacySale);

        if (department != null) {
            jpql += " and bi.fromDepartment=:fdept ";
            m.put("fdept", department);
        }

        jpql += " group by bi.referredBy ";
        jpql += " order by bi.referredBy.person.name ";
        prescriptionSummaryReportRows = (List<PrescriptionSummaryReportRow>) billFacade.findLightsByJpql(jpql, m, TemporalType.DATE);
    }

    public void processPresciptionListEvenWhenNoPatientData() {
        String jpql = "select new "
                + " com.divudi.light.common.BillLight(bi.id, bi.deptId, bi.billDate, bi.billTime, "
                + " coalesce(p.person.name, 'No Patient'), bi.netTotal) "
                + " from Bill bi "
                + " left join bi.patient p "
                + " where bi.cancelled=:can "
                + " and bi.billDate between :fd and :td "
                + " and bi.billType=:bitype ";
        Map m = new HashMap();

        m.put("can", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bitype", BillType.PharmacySale);

        if (department != null) {
            jpql += " and bi.fromDepartment=:fdept ";
            m.put("fdept", department);
        }

        if (doctor != null) {
            jpql += " and bi.referredBy=:refDoc ";
            m.put("refDoc", doctor);
        }

        jpql += " order by bi.id ";
        billLights = (List<BillLight>) billFacade.findLightsByJpql(jpql, m, TemporalType.DATE);
    }

    public void processPresciptionList() {
        String jpql = "select new "
                + " com.divudi.light.common.BillLight(bi.id, bi.deptId, bi.billDate, bi.billTime, bi.patient.person.name, bi.netTotal) "
                + " from Bill bi "
                + " where bi.cancelled=:can "
                + " and bi.billDate between :fd and :td "
                + " and bi.billType=:bitype ";
        Map m = new HashMap();

        m.put("can", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bitype", BillType.PharmacySale);

        if (department != null) {
            jpql += " and bi.fromDepartment=:fdept ";
            m.put("fdept", department);
        }

        if (doctor != null) {
            jpql += " and bi.referredBy=:refDoc ";
            m.put("refDoc", doctor);
        }

        jpql += " order by bi.id ";
//        System.out.println("jpql = " + jpql);
//        System.out.println("m = " + m);
        billLights = (List<BillLight>) billFacade.findLightsByJpql(jpql, m, TemporalType.DATE);
    }

    public void processPharmacySaleItemCount() {
        String jpql = "select new com.divudi.data.ItemCount(bi.item.category.name, bi.item.name, count(bi.item)) "
                + " from BillItem bi "
                + " where bi.bill.cancelled=:can "
                + " and bi.bill.billDate between :fd and :td "
                + " and bi.bill.billType=:bitype ";
        Map<String, Object> m = new HashMap<>();

        m.put("can", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bitype", BillType.PharmacySale);

        if (fromInstitution != null) {
            jpql += " and bi.bill.fromInstitution=:fi ";
            m.put("fi", fromInstitution);
        }

        if (fromDepartment != null) {
            jpql += " and bi.bill.fromDepartment=:fdept ";
            m.put("fdept", fromDepartment);
        }

        jpql += " group by bi.item.category.name, bi.item.name ";
        jpql += " order by bi.item.category.name, bi.item.name";

        // Unchecked cast here
        reportLabTestCounts = (List<ItemCount>) billItemFacade.findLightsByJpql(jpql, m);

        Map<String, CategoryCount> categoryReports = new HashMap<>();

        for (ItemCount count : reportLabTestCounts) {
            categoryReports.computeIfAbsent(count.getCategory(), k -> new CategoryCount(k, new ArrayList<>(), 0L))
                    .getItems().add(count);
            categoryReports.get(count.getCategory()).setTotal(categoryReports.get(count.getCategory()).getTotal() + count.getTestCount());
        }

        // Convert the map values to a list to be used in the JSF page
        reportList = new ArrayList<>(categoryReports.values());
    }

    public void processOpdServiceCount() {
        String jpql = "select new com.divudi.data.ItemCount(bi.item.category.name, bi.item.name, count(bi.item)) "
                + " from BillItem bi "
                + " where bi.bill.cancelled=:can "
                + " and bi.bill.billDate between :fd and :td "
                + " and bi.bill.billType=:bitype ";
        Map<String, Object> m = new HashMap<>();

        m.put("can", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bitype", BillType.OpdBill);

        if (fromInstitution != null) {
            jpql += " and bi.bill.fromInstitution=:fi ";
            m.put("fi", fromInstitution);
        }

        if (fromDepartment != null) {
            jpql += " and bi.bill.fromDepartment=:fdept ";
            m.put("fdept", fromDepartment);
        }

        if (toInstitution != null) {
            jpql += " and bi.bill.toInstitution=:ti ";
            m.put("ti", toInstitution);
        }

        if (toDepartment != null) {
            jpql += " and bi.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        jpql += " group by bi.item.category.name, bi.item.name ";
        jpql += " order by bi.item.category.name, bi.item.name";

        // Unchecked cast here
        reportLabTestCounts = (List<ItemCount>) billItemFacade.findLightsByJpql(jpql, m);

        Map<String, CategoryCount> categoryReports = new HashMap<>();

        for (ItemCount count : reportLabTestCounts) {
            categoryReports.computeIfAbsent(count.getCategory(), k -> new CategoryCount(k, new ArrayList<>(), 0L))
                    .getItems().add(count);
            categoryReports.get(count.getCategory()).setTotal(categoryReports.get(count.getCategory()).getTotal() + count.getTestCount());
        }

        // Convert the map values to a list to be used in the JSF page
        reportList = new ArrayList<>(categoryReports.values());
    }

    public void processCollectingCentreTransactionReport() {
        String jpql = "select bill "
                + " from Bill bill "
                + " where bill.retired=:ret"
                + " and bill.billDate between :fd and :td "
                + " and bill.billType = :bType";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bType", BillType.CollectingCentreBill);

        if (route != null) {
            jpql += " and bill.fromInstitution.route = :route ";
            m.put("route", route);
        }

        if (institution != null) {
            jpql += " and bill.institution = :ins ";
            m.put("ins", institution);
        }

        if (collectingCentre != null) {
            jpql += " and bill.fromInstitution = :cc ";
            m.put("cc", collectingCentre);
        }

        if (toDepartment != null) {
            jpql += " and bill.toDepartment = :dep ";
            m.put("dep", toDepartment);
        }

        if (phn != null && !phn.isEmpty()) {
            jpql += " and bill.patient.phn = :phn ";
            m.put("phn", phn);
        }

        if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
            jpql += " and bill.deptId = :inv ";
            m.put("inv", invoiceNumber);
        }

//        if (itemLight != null) {
//            jpql += " and bi.item.id = :item ";
//            m.put("item", itemLight.getId());
//        }
        if (doctor != null) {
            jpql += " and bill.referredBy = :refDoc ";
            m.put("refDoc", doctor);
        }

//        if (status != null) {
//            jpql += " and billItemStatus = :status ";
//            m.put("status", status);
//        }
        bills = billFacade.findByJpql(jpql, m);
    }

    public void processCollectingCentreReciptReport() {
        String jpql = "select bill "
                + " from Bill bill "
                + " where bill.retired=:ret"
                + " and bill.billDate between :fd and :td "
                + " and bill.billType = :bType";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bType", BillType.CollectingCentreBill);

        if (route != null) {
            jpql += " and bill.fromInstitution.route = :route ";
            m.put("route", route);
        }

        if (institution != null) {
            jpql += " and bill.institution = :ins ";
            m.put("ins", institution);
        }

        if (collectingCentre != null) {
            jpql += " and bill.fromInstitution = :cc ";
            m.put("cc", collectingCentre);
        }

        if (toDepartment != null) {
            jpql += " and bill.toDepartment = :dep ";
            m.put("dep", toDepartment);
        }

        if (phn != null && !phn.isEmpty()) {
            jpql += " and bill.patient.phn = :phn ";
            m.put("phn", phn);
        }

        if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
            jpql += " and bill.deptId = :inv ";
            m.put("inv", invoiceNumber);
        }

//        if (itemLight != null) {
//            jpql += " and bi.item.id = :item ";
//            m.put("item", itemLight.getId());
//        }
        if (doctor != null) {
            jpql += " and bill.referredBy = :refDoc ";
            m.put("refDoc", doctor);
        }

//        if (status != null) {
//            jpql += " and billItemStatus = :status ";
//            m.put("status", status);
//        }
        bills = billFacade.findByJpql(jpql, m);
    }

    public void downloadLabTestCount() {
        Workbook workbook = exportToExcel(reportList, "Test Count");
        FacesContext fc = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
        response.reset();
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=test_counts.xlsx");

        try ( ServletOutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
            fc.responseComplete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void downloadPharmacySaleItemCount() {
        Workbook workbook = exportToExcel(reportList, "Sale Item Count");
        FacesContext fc = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
        response.reset();
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=Sale_Item_Count.xlsx");

        try ( ServletOutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
            fc.responseComplete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void downloadOpdServiceCount() {
        Workbook workbook = exportToExcel(reportList, "Opd Service Count");
        FacesContext fc = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
        response.reset();
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=service_count.xlsx");

        try ( ServletOutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
            fc.responseComplete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Workbook exportToExcel(List<CategoryCount> reportList, String reportName) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(reportName);
        int rowCount = 0;
        int cateColNo = 0;
        int itemColNo = 1;
        int catCountColNo = 2;
        int itemCountColNo = 3;
        Long grandTotal = 0l;

        Row reportHeaderRow = sheet.createRow(rowCount++);
        Cell headerRowCatCell = reportHeaderRow.createCell(cateColNo);
        headerRowCatCell.setCellValue("Category");
        Cell headerRowItemCell = reportHeaderRow.createCell(itemColNo);
        headerRowItemCell.setCellValue("Item");
        Cell headerRowCatCountCell = reportHeaderRow.createCell(catCountColNo);
        headerRowCatCountCell.setCellValue("Category Count");
        Cell headerRowItemCountCell = reportHeaderRow.createCell(itemCountColNo);
        headerRowItemCountCell.setCellValue("Item Count");

        for (CategoryCount catCount : reportList) {
            Row headerRow = sheet.createRow(rowCount++);
            Cell headerCell1 = headerRow.createCell(cateColNo);
            headerCell1.setCellValue(catCount.getCategory());
            Cell headerCell2 = headerRow.createCell(catCountColNo);
            headerCell2.setCellValue(catCount.getTotal());
            grandTotal += catCount.getTotal();

            for (ItemCount itemCount : catCount.getItems()) {
                Row itemRow = sheet.createRow(rowCount++);
                Cell cell1 = itemRow.createCell(itemColNo);
                cell1.setCellValue(itemCount.getTestName());
                Cell cell2 = itemRow.createCell(itemCountColNo);
                cell2.setCellValue(itemCount.getTestCount());
            }
        }

        Row reportFooterRow = sheet.createRow(rowCount++);
        Cell reportFooterCellTotal = reportFooterRow.createCell(cateColNo);
        reportFooterCellTotal.setCellValue("Grand Total");
        Cell reportFooterRowItemCell = reportFooterRow.createCell(itemCountColNo);
        reportFooterRowItemCell.setCellValue(grandTotal);

        return workbook;
    }

    public String navigateToAssetRegister() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }

        return "/reports/assets/asset_register";
    }

    public String navigateToLabReportsTestCount() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/test_count";
    }

    public String navigateToLabPeakHourStatistics() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/peak_hour_statistics";
    }

    public String navigateToLabInvetigationWiseReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/investigation_wise_report";
    }

    public String navigateToExternalLaborataryWorkloadReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/external_laboratary_workload";
    }

    public String navigateToLabOrganismAntibioticSensitivityReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/organism_antibiotic_sensitivity";
    }

    public String navigateToLabRegisterReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/lab_register";
    }

    public String navigateToTurnAroundTimeDetails() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/turn_around_time_details";
    }

    public String navigateToAnnualTestStatistics() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/annual_test_statistics";
    }

    public String navigateToPoStatusReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }

        return "/reports/assets/po_status_report";
    }

    public String navigateToEmployeeAssetIssue() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/assets/employee_asset_issue";
    }

    public String navigateToFixedAssetIssue() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/assets/fixed_asset_issue";
    }

    public String navigateToAssetWarentyExpireReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/assets/asset_warranty_expire_report";
    }

    public String navigateToAssetGrnReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/assets/asset_grn_report";
    }

    public String navigateToAssetTransferReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/assets/assest_transfer_report";

    }

    public String navigateToAssetAmcExpiryReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/assets/assest_amc_expiry_report";

    }

    public String navigateToAssetAmcReport() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/assets/amc_report";

    }

    public String navigateToTurnAroundTimeHourly() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/turn_around_time_hourly";

    }

    public String navigateToCollectionCenterStatement() {
        if (institutionController.getItems() == null) {
            institutionController.fillItems();
        }
        return "/reports/lab/collection_center_statement";

    }

    public String navigateToManagementAdmissionCountReport() {

        return "/reports/managementReports/admission_count(consultant_wise)";
    }

    public String navigateToSurgeryWiseCount() {

        return "/reports/managementReports/surgery_wise_count";
    }

    public String navigateToSurgeryCountDoctorWise() {

        return "/reports/managementReports/surgery_count_doctor_wise";
    }

    public String navigateToLeaveReport() {

        return "/reports/HRReports/leave_report";
    }

    public String navigateToLeaveReportSummery() {

        return "/reports/HRReports/leave_report_summery";
    }

    public String navigateToLateLeaveDetails() {

        return "/reports/HRReports/late_leave_details";
    }

    public String navigateToLeaveSummeryReport() {

        return "/reports/HRReports/leave_summery_report";
    }

    public String navigateToDepartmentReports() {

        return "/reports/HRReports/department_report";
    }

    public String navigateToEmployeeDetails() {

        return "/reports/HRReports/employee_details";
    }

    public String navigateToEmployeeToRetired() {

        return "/reports/HRReports/employee_to_retired";
    }

    public String navigateToStaffShiftReport() {

        return "/reports/HRReports/staff_shift_report";
    }

    public String navigateToRosterTimeAndVerifyTime() {

        return "/reports/HRReports/rosterTabel_verify_time";
    }

    public String navigateToEmployeeEndofProbation() {

        return "/reports/HRReports/employee_end_of_probation";
    }

    public String navigateToAttendanceReport() {

        return "/reports/HRReports/attendance_report";
    }

    public String navigateToLateInAndEarlyOut() {

        return "/reports/HRReports/late_in_and_early_out";
    }

    public String navigateToStaffShiftDetailsByStaff() {

        return "/reports/HRReports/staff_shift_details_by_staff";
    }

    public String navigateToVerifiedReport() {

        return "/reports/HRReports/verified_report";
    }

    public String navigateToHeadCountReport() {

        return "/reports/HRReports/head_count";
    }

    public String navigateToFingerPrintRecordByLogged() {

        return "/reports/HRReports/fingerprint_record_by_logged";
    }

    public String navigateToFingerPrintRecordByVerified() {

        return "/reports/HRReports/fingerprint_record_by_verified";
    }

    public String navigateToFingerPrintRecordNoShiftSettled() {

        return "/reports/HRReports/fingerprint_record_no_shift_settled";
    }

    public String navigateToEmployeeWorkedDayReport() {

        return "/reports/HRReports/employee_worked_day_report";
    }

    public String navigateToEmployeeWorkedDayReportSalaryCycle() {

        return "/reports/HRReports/employee_worked_day_report_salary_cycle";
    }

    public String navigateToMonthendEmployeeWorkingTimeAndOvertime() {

        return "/reports/HRReports/monthend_employee_working_Time_and_overtime";
    }

    public String navigateToMonthEndEmployeeNoPayReportByMinutes() {

        return "/reports/HRReports/month_end_employee_nopay_report_by_minutes";
    }

    public String navigateToMonthEndEmployeeSummery() {

        return "/reports/HRReports/month_end_employee_summery";
    }

    public String navigateToFingerAnalysisReportBySalaryCycle() {

        return "/reports/HRReports/finger_analysis_report_by_salary_cycle";
    }

    public String navigateToFingerPrintApprove() {

        return "/reports/HRReports/fingerprint_approve";
    }

    public String navigateToLeaveForm() {

        return "/reports/HRReports/leave_form";
    }

    public String navigateToAdditionalFormReportVerification() {

        return "/reports/HRReports/additional_form_report_veification";
    }

    public String navigateToOnlineFormStatus() {

        return "/reports/HRReports/online_form_status";
    }

    public String navigateToAdmissionDischargeReport() {

        return "/reports/inpatientReports/admission_discharge_report";
    }

    public String navigateToGoodInTransit() {

        return "/reports/inventoryReports/good_in_transit";
    }

    public String navigateToGrnReport() {

        return "/reports/inventoryReports/grn_report";
    }

    public String navigateToSlowFastNoneMovement() {

        return "/reports/inventoryReports/slow_fast_none_movement";
    }

    public String navigateToBeforeStockTaking() {

        return "/reports/inventoryReports/before_stock_taking";
    }

    public String navigateToAfterStockTaking() {

        return "/reports/inventoryReports/after_stock_taking";
    }

    public String navigateToStockLedger() {

        return "/reports/inventoryReports/stock_ledger";
    }

    public String navigateToExpiryItem() {

        return "/reports/inventoryReports/expiry_item";
    }

    public String navigateToIpUnsettledInvoices() {

        return "/reports/inpatientReports/ip_unsettled_invoices";
    }

    public String navigateToconsumption() {

        return "/reports/inventoryReports/consumption";
    }

    public String navigateToClosingStockReport() {

        return "/reports/inventoryReports/closing_stock_report";
    }

    public String navigateToAdmissionCategoryWiseAdmission() {

        return "/reports/inpatientReports/admission_category_wise_admission";
    }

    public String navigateToStockTransferReport() {

        return "/reports/inventoryReports/stock_transfer_report";
    }

    public String navigateToCostOfGoodsSold() {

        return "/reports/inventoryReports/cost_of_goods_sold";
    }

    public String navigateToDiscount() {

        return "/reports/financialReports/discount";
    }

    public String navigateToOutsidePayment() {

        return "/reports/financialReports/outside_payment";
    }

    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public InstitutionController getInstitutionController() {
        return institutionController;
    }

    public void setInstitutionController(InstitutionController institutionController) {
        this.institutionController = institutionController;
    }

    public Institution getFromInstitution() {
        return fromInstitution;
    }

    public void setFromInstitution(Institution fromInstitution) {
        this.fromInstitution = fromInstitution;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public int getReportIndex() {
        return reportIndex;
    }

    public void setReportIndex(int reportIndex) {
        this.reportIndex = reportIndex;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = commonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = commonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public List<ItemCount> getReportLabTestCounts() {
        return reportLabTestCounts;
    }

    public void setReportLabTestCounts(List<ItemCount> reportLabTestCounts) {
        this.reportLabTestCounts = reportLabTestCounts;
    }

    public List<CategoryCount> getReportList() {
        return reportList;
    }

    public void setReportList(List<CategoryCount> reportList) {
        this.reportList = reportList;
    }

    public String getProcessBy() {
        return processBy;
    }

    public void setProcessBy(String processBy) {
        this.processBy = processBy;
    }

    public double getInvestigationResult() {
        return investigationResult;
    }

    public void setInvestigationResult(double investigationResult) {
        this.investigationResult = investigationResult;
    }

    public String getVisitType() {
        return visitType;
    }

    public Sex[] getSex() {
        return Sex.values();
    }

    public void setVisitType(String visitType) {
        this.visitType = visitType;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation(Investigation investigation) {
        this.investigation = investigation;
    }

    public String getPriorityType() {
        return priorityType;
    }

    public void setPriorityType(String priorityType) {
        this.priorityType = priorityType;
    }

    public String getPatientMrn() {
        return patientMrn;
    }

    public void setPatientMrn(String patientMrn) {
        this.patientMrn = patientMrn;
    }

    public String getTotalAverage() {
        return totalAverage;
    }

    public void setTotalAverage(String totalAverage) {
        this.totalAverage = totalAverage;
    }

    public String getVisit() {
        return visit;
    }

    public void setVisit(String visit) {
        this.visit = visit;
    }

    public Date getFinancialYear() {
        return financialYear;
    }

    public void setFinancialYear(Date financialYear) {
        this.financialYear = financialYear;
    }

    public Speciality getCurrentSpeciality() {
        return currentSpeciality;
    }

    public void setCurrentSpeciality(Speciality currentSpeciality) {
        this.currentSpeciality = currentSpeciality;
    }

    public Date getWarrentyStartDate() {
        if (warrentyStartDate == null) {
            warrentyStartDate = CommonFunctions.getEndOfDay();
        }
        return warrentyStartDate;
    }

    public void setWarrentyStartDate(Date warrentyStartDate) {
        this.warrentyStartDate = warrentyStartDate;
    }

    public Date getWarrentyEndDate() {
        if (warrentyEndDate == null) {
            warrentyEndDate = CommonFunctions.getEndOfMonth(toDate);
        }

        return warrentyEndDate;
    }

    public void setWarrentyEndDate(Date warrentyEndDate) {
        this.warrentyEndDate = warrentyEndDate;
    }

    public Date getAmcStartDate() {
        if (amcStartDate == null) {
            amcStartDate = CommonFunctions.getEndOfDay();
        }
        return amcStartDate;
    }

    public void setAmcStartDate(Date amcStartDate) {
        this.amcStartDate = amcStartDate;
    }

    public Date getAmcEndDate() {
        if (amcEndDate == null) {
            amcEndDate = CommonFunctions.getEndOfMonth(toDate);
        }

        return amcEndDate;
    }

    public void setAmcEndDate(Date amcEndDate) {
        this.amcEndDate = amcEndDate;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public Institution getCollectingCentre() {
        return collectingCentre;
    }

    public void setCollectingCentre(Institution collectingCentre) {
        this.collectingCentre = collectingCentre;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public String getPhn() {
        return phn;
    }

    public void setPhn(String phn) {
        this.phn = phn;
    }

    public List<ItemLight> getInvestigationsAndServices() {
        if (investigationsAndServices == null) {
            itemApplicationController.getInvestigationsAndServices();
        }
        return investigationsAndServices;
    }

    public void setInvestigationsAndServices(List<ItemLight> investigationsAndServices) {
        this.investigationsAndServices = investigationsAndServices;
    }

    public ItemLight getItemLight() {
        if (item == null) {
            itemLight = null;
        } else {
            itemLight = new ItemLight(item);
        }
        return itemLight;
    }

    public void setItemLight(ItemLight itemLight) {
        this.itemLight = itemLight;
        if (itemLight == null) {
            item = null;
        } else {
            item = itemController.findItem(itemLight.getId());
        }
    }

    public BillItemStatus getStatus() {
        return status;
    }

    public void setStatus(BillItemStatus status) {
        this.status = status;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public void processCollectingCentreTestWiseCountReport() {
        String jpql = "select new  com.divudi.data.TestWiseCountReport("
                + "bi.item.name, "
                + "count(bi.item.name), "
                + "sum(bi.hospitalFee) , "
                + "sum(bi.collectingCentreFee), "
                + "sum(bi.staffFee), "
                + "sum(bi.netValue)"
                + ") "
                + " from BillItem bi "
                + " where bi.retired=:ret"
                + " and bi.bill.billDate between :fd and :td "
                + " and bi.bill.billType = :bType ";

        if (false) {
            BillItem bi = new BillItem();
            bi.getItem();
            bi.getHospitalFee();
            bi.getCollectingCentreFee();
            bi.getStaffFee();
            bi.getNetValue();
        }

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bType", BillType.CollectingCentreBill);

        if (route != null) {
            jpql += " and bi.bill.fromInstitution.route = :route ";
            m.put("route", route);
        }

        if (institution != null) {
            jpql += " and bi.bill.institution = :ins ";
            m.put("ins", institution);
        }

        if (collectingCentre != null) {
            jpql += " and bi.bill.fromInstitution = :cc ";
            m.put("cc", collectingCentre);
        }

        if (toDepartment != null) {
            jpql += " and bi.bill.toDepartment = :dep ";
            m.put("dep", toDepartment);
        }

        if (phn != null && !phn.isEmpty()) {
            jpql += " and bi.bill.patient.phn = :phn ";
            m.put("phn", phn);
        }

        if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
            jpql += " and bi.bill.deptId = :inv ";
            m.put("inv", invoiceNumber);
        }

        if (itemLight != null) {
            jpql += " and bi.item.id = :item ";
            m.put("item", itemLight.getId());
        }

        if (doctor != null) {
            jpql += " and bi.bill.referredBy = :refDoc ";
            m.put("refDoc", doctor);
        }

        if (status != null) {
            jpql += " and bi.billItemStatus = :status ";
            m.put("status", status);
        }

        jpql += " group by bi.item.name";

        testWiseCounts = (List<TestWiseCountReport>) billItemFacade.findLightsByJpql(jpql, m);

    }

    private List<TestWiseCountReport> testWiseCounts;

    public List<TestWiseCountReport> getTestWiseCounts() {
        return testWiseCounts;
    }

    public void setTestWiseCounts(List<TestWiseCountReport> testWiseCounts) {
        this.testWiseCounts = testWiseCounts;
    }

    public List<Institution> getCollectionCenters() {
        return collectionCenters;
    }

    public void setCollectionCenters(List<Institution> collectionCenters) {
        this.collectionCenters = collectionCenters;
    }

    public List<PrescriptionSummaryReportRow> getPrescriptionSummaryReportRows() {
        return prescriptionSummaryReportRows;
    }

    public void setPrescriptionSummaryReportRows(List<PrescriptionSummaryReportRow> prescriptionSummaryReportRows) {
        this.prescriptionSummaryReportRows = prescriptionSummaryReportRows;
    }

    public List<BillLight> getBillLights() {
        return billLights;
    }

    public void setBillLights(List<BillLight> billLights) {
        this.billLights = billLights;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public List<ItemCount> getReportOpdServiceCount() {
        return reportOpdServiceCount;
    }

    public void setReportOpdServiceCount(List<ItemCount> reportOpdServiceCount) {
        this.reportOpdServiceCount = reportOpdServiceCount;
    }

}
