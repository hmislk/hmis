package com.divudi.bean.lab;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.lab.BillBarcode;
import com.divudi.core.data.lab.ListingEntity;
import com.divudi.core.data.lab.PatientInvestigationStatus;
import com.divudi.core.data.lab.PatientInvestigationWrapper;
import com.divudi.core.data.lab.PatientSampleWrapper;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Route;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.lab.PatientSample;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Named
@SessionScoped
public class LaboratoryManagementController implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public LaboratoryManagementController() {
        activeIndex = 1;
        listingEntity = ListingEntity.BILLS; // Set default view
    }

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    BillFacade billFacade;
    @EJB
    private PatientInvestigationFacade ejbFacade;
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    SessionController sessionController;
    @Inject
    PatientInvestigationController patientInvestigationController;
    
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Variables">
    private ListingEntity listingEntity;
    
    private int activeIndex = 0;

    private Date fromDate;
    private Date toDate;
    private String patientName;
    private String type;
    private Staff referringDoctor;
    private PatientInvestigationStatus patientInvestigationStatus;
    private Route route;
    private Institution collectionCenter;
    private Institution orderedInstitution;
    private Department orderedDepartment;
    private Institution performingInstitution;
    private Department performingDepartment;
    private String billNo;
    private String bhtNo;
    
    private List<BillBarcode> billBarcodes;
    private List<BillBarcode> selectedBillBarcodes;
    private Bill currentBill;
    
    private boolean printIndividualBarcodes;
            

    private List<Bill> bills = null;

    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Navigation Method">
    public String navigateToLaboratoryManagementDashboard() {
        activeIndex = 1;
        listingEntity = ListingEntity.BILLS;
        if(configOptionApplicationController.getBooleanValueByKey("Only bills that have been sent to the Log department should be displayed.", false)){
            performingInstitution = sessionController.getInstitution();
            performingDepartment = sessionController.getDepartment();
        }
        patientInvestigationStatus = PatientInvestigationStatus.ORDERED;
        return "/lab/laboratory_management_dashboard?faces-redirect=true";
    }
    
    public void navigateToLaboratoryBills() {
        activeIndex = 1;
        listingEntity = ListingEntity.BILLS;
        if(configOptionApplicationController.getBooleanValueByKey("Only bills that have been sent to the Log department should be displayed.", false)){
            performingInstitution = sessionController.getInstitution();
            performingDepartment = sessionController.getDepartment();
        }
        patientInvestigationStatus = PatientInvestigationStatus.ORDERED;
    }

    public void navigateToBarcodes() {
        activeIndex = 2;
        listingEntity = ListingEntity.BILL_BARCODES;
    }

    public void navigateToSamples() {
        activeIndex = 3;
        listingEntity = ListingEntity.PATIENT_SAMPLES;
    }

    public void navigateToReport() {
        activeIndex = 4;
        listingEntity = ListingEntity.PATIENT_INVESTIGATIONS;
    }

    public void navigateToReportPrint() {
        activeIndex = 5;
        listingEntity = ListingEntity.PATIENT_REPORTS;
    }

    public String navigateToLaboratoryAdministration() {
        return "/admin/lims/index?faces-redirect=true";
    }

    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Function">
    public void searchLabBills() {
        listingEntity = ListingEntity.BILLS;
        String jpql;
        bills = new ArrayList();
        Map<String, Object> params = new HashMap<>();
        jpql = "SELECT pi.billItem.bill "
                + " FROM PatientInvestigation pi"
                + " WHERE pi.billItem.bill.retired = :ret"
                + " AND pi.billItem.bill.createdAt BETWEEN :fd AND :td";

        if (billNo != null && !billNo.trim().isEmpty()) {
            jpql += " AND pi.billItem.bill.deptId LIKE :billNo";
            params.put("billNo", "%" + getBillNo().trim() + "%");
        }
        
        if (bhtNo != null && !bhtNo.trim().isEmpty()) {
            jpql += " AND pi.billItem.bill.patientEncounter is not null AND pi.billItem.bill.patientEncounter.bhtNo LIKE :bht";
            params.put("bht", "%" + getBhtNo().trim() + "%");
        }
        
        if (orderedInstitution != null) {
            jpql += " AND pi.billItem.bill.institution = :orderedInstitution";
            params.put("orderedInstitution", getOrderedInstitution());
        }

        if (orderedDepartment != null) {
            jpql += " AND pi.billItem.bill.department = :orderedDepartment";
            params.put("orderedDepartment", getOrderedDepartment());
        }

        if (performingInstitution != null) {
            jpql += " AND pi.billItem.bill.toInstitution = :performingInstitution";
            params.put("performingInstitution", performingInstitution);
        }

        if (performingDepartment != null) {
            jpql += " AND pi.billItem.bill.toDepartment = :performingDepartment";
            params.put("performingDepartment", performingDepartment);
        }

        if (collectionCenter != null) {
            jpql += " AND (pi.billItem.bill.collectingCentre = :collectionCenter OR pi.billItem.bill.fromInstitution = :collectionCenter)";
            params.put("collectionCenter", getCollectionCenter());
        }

        if (route != null) {
            jpql += " AND (pi.billItem.bill.collectingCentre.route = :route OR pi.billItem.bill.fromInstitution.route = :route)";
            params.put("route", getRoute());
        }

        if (patientName != null && !patientName.trim().isEmpty()) {
            jpql += " AND pi.billItem.bill.patient.person.name LIKE :patientName";
            params.put("patientName", "%" + getPatientName().trim() + "%");
        }

        if (type != null && !type.trim().isEmpty()) {
            jpql += " AND pi.billItem.bill.ipOpOrCc = :type";
            params.put("type", getType().trim());
        }

        if (referringDoctor != null) {
            jpql += " AND pi.billItem.bill.referredBy = :referringDoctor";
            params.put("referringDoctor", getReferringDoctor());
        }

        if (patientInvestigationStatus != null) {
            jpql += " AND pi.billItem.bill.status = :status";
            params.put("status", patientInvestigationStatus);
        }

        jpql += " GROUP BY pi.billItem.bill ";

        jpql += " ORDER BY pi.billItem.bill.id DESC";

        params.put("ret", false);
        params.put("fd", getFromDate());
        params.put("td", getToDate());

        bills = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
    }
    
    public void generateBarcodesForSelectedBill(Bill billForBarcode) {
        selectedBillBarcodes = new ArrayList<>();
        billBarcodes = new ArrayList<>();
        setCurrentBill(billForBarcode);
        if (billForBarcode == null) {
            JsfUtil.addErrorMessage("No Bills Seelcted");
            return;
        }

        if (billForBarcode.isCancelled()) {
            JsfUtil.addErrorMessage("This Bill is Already Cancel");
            return;
        }

        BillBarcode bb = new BillBarcode(billForBarcode);
        List<PatientSampleWrapper> psws = new ArrayList<>();
        List<PatientSample> pss = patientInvestigationController.prepareSampleCollectionByBillsForPhlebotomyRoom(billForBarcode, sessionController.getLoggedUser());
        StringBuilder sampleIDs = new StringBuilder();
        if (pss != null) {
            for (PatientSample ps : pss) {
                PatientSampleWrapper ptsw = new PatientSampleWrapper(ps);
                psws.add(ptsw);
                if (!sampleIDs.toString().contains(ps.getIdStr())) {
                    sampleIDs.append(ps.getIdStr()).append(" ");  // Add space for separation
                }
            }
        }

        for (PatientInvestigationWrapper piw : bb.getPatientInvestigationWrappers()) {
            if (billForBarcode.getStatus() == PatientInvestigationStatus.ORDERED) {
                piw.getPatientInvestigation().setBarcodeGenerated(true);
                piw.getPatientInvestigation().setBarcodeGeneratedAt(new Date());

            }

            // Properly add unique sample IDs to PatientInvestigation
            String[] idsToAdd = sampleIDs.toString().trim().split("\\s+");
            String existingSampleIds = piw.getPatientInvestigation().getSampleIds();
            for (String id : idsToAdd) {
                if (!existingSampleIds.contains(id)) {
                    existingSampleIds += " " + id;
                }
            }
            if (billForBarcode.getStatus() == PatientInvestigationStatus.ORDERED) {
                piw.getPatientInvestigation().setBarcodeGeneratedBy(sessionController.getLoggedUser());
                piw.getPatientInvestigation().setStatus(PatientInvestigationStatus.SAMPLE_GENERATED);
            }
            piw.getPatientInvestigation().setSampleIds(existingSampleIds.trim());

            ejbFacade.edit(piw.getPatientInvestigation());
        }
        if (billForBarcode.getStatus() == PatientInvestigationStatus.ORDERED) {
            billForBarcode.setStatus(PatientInvestigationStatus.SAMPLE_GENERATED);
        }

        billFacade.edit(billForBarcode);
        bb.setPatientSampleWrappers(psws);

        billBarcodes.add(bb);
        selectedBillBarcodes = billBarcodes;
        listingEntity = ListingEntity.VIEW_BARCODE;
    }
    

    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Getter & Setter">
    public ListingEntity getListingEntity() {
        return listingEntity;
    }

    public void setListingEntity(ListingEntity listingEntity) {
        this.listingEntity = listingEntity;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Staff getReferringDoctor() {
        return referringDoctor;
    }

    public void setReferringDoctor(Staff referringDoctor) {
        this.referringDoctor = referringDoctor;
    }

    public PatientInvestigationStatus getPatientInvestigationStatus() {
        return patientInvestigationStatus;
    }

    public void setPatientInvestigationStatus(PatientInvestigationStatus patientInvestigationStatus) {
        this.patientInvestigationStatus = patientInvestigationStatus;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public Institution getCollectionCenter() {
        return collectionCenter;
    }

    public void setCollectionCenter(Institution collectionCenter) {
        this.collectionCenter = collectionCenter;
    }

    public Institution getOrderedInstitution() {
        return orderedInstitution;
    }

    public void setOrderedInstitution(Institution orderedInstitution) {
        this.orderedInstitution = orderedInstitution;
    }

    public Department getOrderedDepartment() {
        return orderedDepartment;
    }

    public void setOrderedDepartment(Department orderedDepartment) {
        this.orderedDepartment = orderedDepartment;
    }

    public Institution getPerformingInstitution() {
        return performingInstitution;
    }

    public void setPerformingInstitution(Institution performingInstitution) {
        this.performingInstitution = performingInstitution;
    }

    public Department getPerformingDepartment() {
        return performingDepartment;
    }

    public void setPerformingDepartment(Department performingDepartment) {
        this.performingDepartment = performingDepartment;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }
    
    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setActiveIndex(int activeIndex) {
        this.activeIndex = activeIndex;
    }
    
    public List<BillBarcode> getBillBarcodes() {
        return billBarcodes;
    }

    public void setBillBarcodes(List<BillBarcode> billBarcodes) {
        this.billBarcodes = billBarcodes;
    }

    public List<BillBarcode> getSelectedBillBarcodes() {
        return selectedBillBarcodes;
    }

    public void setSelectedBillBarcodes(List<BillBarcode> selectedBillBarcodes) {
        this.selectedBillBarcodes = selectedBillBarcodes;
    }

    public Bill getCurrentBill() {
        return currentBill;
    }

    public void setCurrentBill(Bill currentBill) {
        this.currentBill = currentBill;
    }

    public boolean isPrintIndividualBarcodes() {
        return printIndividualBarcodes;
    }

    public void setPrintIndividualBarcodes(boolean printIndividualBarcodes) {
        this.printIndividualBarcodes = printIndividualBarcodes;
    }
    // </editor-fold>
}
