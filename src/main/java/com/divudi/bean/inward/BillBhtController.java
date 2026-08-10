/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.BillController;
import com.divudi.bean.common.BillSearch;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.DepartmentController;
import com.divudi.bean.common.ItemApplicationController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.ItemFeeManager;
import com.divudi.bean.common.ItemMappingController;
import com.divudi.bean.common.PageMetadataRegistry;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.bean.inward.BhtSummeryController;

import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.FeeType;
import com.divudi.core.data.OptionScope;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.admin.ConfigOptionInfo;
import com.divudi.core.data.admin.PageMetadata;
import com.divudi.core.data.admin.PrivilegeInfo;
import com.divudi.core.data.inward.SurgeryBillType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillComponent;
import com.divudi.core.entity.BillEntry;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.Fee;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.ItemFee;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.RoomCategory;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.facade.BillComponentFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.EncounterComponentFacade;
import com.divudi.core.facade.FeeFacade;
import com.divudi.core.facade.ItemFeeFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.facade.PriceMatrixFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.lab.PatientInvestigationController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.ItemLight;
import static com.divudi.core.data.ItemListingStrategy.*;
import com.divudi.core.data.lab.InvestigationTubeSticker;
import com.divudi.core.data.lab.Priority;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.UserPreference;
import com.divudi.ws.lims.Lims;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class BillBhtController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    private WebUserController webUserController;
    @Inject
    BhtSummeryController bhtSummeryController;
    @Inject
    ItemController itemController;
    @Inject
    ItemMappingController itemMappingController;
    @Inject
    ItemApplicationController itemApplicationController;
    @Inject
    PatientInvestigationController patientInvestigationController;
    @Inject
    ItemFeeManager itemFeeManager;
    @Inject
    DepartmentController departmentController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    PageMetadataRegistry pageMetadataRegistry;
    /////////////////
    @EJB
    private ItemFeeFacade itemFeeFacade;
    @EJB
    private PriceMatrixFacade priceAdjustmentFacade;
    @EJB
    private FeeFacade feeFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private BillComponentFacade billComponentFacade;
    @EJB
    private BillFeeFacade billFeeFacade;

    @Inject
    Lims lims;
    @Inject
    InwardBeanController inwardBean;
    @Inject
    private BillBeanController billBean;
    @EJB
    private BillNumberGenerator billNumberBean;
    @Inject
    BillController billController;
    @Inject
    private SurgeryBillController surgeryBillController;
    ///////////////////

    private double total;
    private double discount;
    private double marginTotal;
    private double netTotal;
    private double vat;
    private double vatPlusNetTotal;
    private double cashPaid;
    private double cashBalance;
    private String creditCardRefNo;
    private String chequeRefNo;
    private Institution chequeBank;
    private BillItem currentBillItem;
    private Integer index;
    private PatientEncounter patientEncounter;
    private PaymentScheme paymentScheme;
    private Bill batchBill;
    /////////////////////
    private List<BillComponent> lstBillComponents;
    private List<BillFee> lstBillFees;
    private List<BillItem> lstBillItems;
    private List<BillEntry> lstBillEntries;
    private boolean printPreview;
    private List<Bill> bills;
    private Doctor referredBy;
    Date date;
    private String stickerPrinterString;
    private List<InvestigationTubeSticker> stickers;

    private List<ItemLight> inwardItems;
    private ItemLight itemLight;

    private int entriesIndex;

    private List<ItemLight> departmentInwardItems;
    private Department selectedInwardItemDepartment;
    private List<Department> inwardItemDepartments;
    private List<ItemLight> inwardItem;
    
    private Priority currentBillItemPriority;
    private Double currentBillItemQty;

    @PostConstruct
    public void init() {
        registerPageMetadata();
    }

    /**
     * Register page metadata for the admin configuration interface
     */
    private void registerPageMetadata() {
        if (pageMetadataRegistry == null) {
            return;
        }

        PageMetadata metadata = new PageMetadata();
        metadata.setPagePath("inward/inward_bill_service");
        metadata.setPageName("Inward Add Services");
        metadata.setDescription("Adds services, investigations, and their fees to an inpatient's bill");
        metadata.setControllerClass("BillBhtController");

        metadata.addConfigOption(new ConfigOptionInfo(
                "Inward Bill Fees are based on the site for " + sessionController.getDepartment().getName(),
                "Uses site-specific bill fees (falling back to base fees when none exist for the site) when adding services to the inward bill. Mirrors the equivalent OPD option.",
                "BillBhtController.java: billFeeFromBillItemWithMatrix() method",
                OptionScope.DEPARTMENT
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
                "Use Sample Management for Inward Service and Investigations",
                "Routes the print-preview 'Manage Samples' action to sample management instead of direct label printing",
                "inward_bill_service.xhtml: print-preview action buttons",
                OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
                "Inward Servise Bill size is POS Paper",
                "Uses POS paper format for the inward service bill print preview",
                "inward_bill_service.xhtml: bill print-preview paper format selection",
                OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
                "Inward Servise Bill size is A4 Paper",
                "Uses A4 paper format for the inward service bill print preview",
                "inward_bill_service.xhtml: bill print-preview paper format selection",
                OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
                "Inward Servise Bill size is A4Printed Paper",
                "Uses A4 pre-printed paper format for the inward service bill print preview",
                "inward_bill_service.xhtml: bill print-preview paper format selection",
                OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
                "Inward Servise Bill size is FiveFiveCustom3 Paper",
                "Uses the FiveFiveCustom3 paper format for the inward service bill print preview",
                "inward_bill_service.xhtml: bill print-preview paper format selection",
                OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
                "Inward Servise Bill size is 5x8 inch Paper",
                "Uses the 5x8 inch paper format for the inward service bill print preview",
                "inward_bill_service.xhtml: bill print-preview paper format selection",
                OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
                "Inward Servise Bill size is FiveFivePrinted paper",
                "Uses the FiveFive pre-printed paper format for the inward service bill print preview (this is also the fallback default when the paper type preference is unset)",
                "inward_bill_service.xhtml: bill print-preview paper format selection",
                OptionScope.APPLICATION
        ));

        metadata.addPrivilege(new PrivilegeInfo(
                "NursingWorkBench",
                "Access to the Nursing WorkBench navigation button",
                "inward_bill_service.xhtml: 'Nursing WorkBench' buttons"
        ));

        metadata.addPrivilege(new PrivilegeInfo(
                "InwardSearch",
                "Access to Search Patients, Patient Profile, and Inpatient Dashboard navigation buttons",
                "inward_bill_service.xhtml: patient navigation button group"
        ));

        metadata.addPrivilege(new PrivilegeInfo(
                "ShowServiceCharges",
                "View rate, gross value, discount, service charge, VAT, and net value columns/details for inward services and fees",
                "inward_bill_service.xhtml: Bill Items and Fees tab columns, Bill Details panel"
        ));

        metadata.addPrivilege(new PrivilegeInfo(
                "ShowInwardFee",
                "Allows editing the Total Gross value of an inward fee directly in the Fees tab",
                "inward_bill_service.xhtml: Fees tab, Total Gross input"
        ));

        pageMetadataRegistry.registerPage(metadata);
    }

    public String navigateToAddServiceFromMenu() {
        resetBillData();
        return "/inward/inward_bill_service?faces-redirect=true";
    }

    public String navigateToSurgeryServices(Bill surgeryBill) {
        resetBillData();
        batchBill = surgeryBill;
        patientEncounter = surgeryBill.getPatientEncounter();
        loadExistingSurgeryServiceEntries(surgeryBill);
        return "/theater/inward_bill_surgery_service?faces-redirect=true";
    }

    /**
     * Rebuilds {@link #lstBillEntries} (the "Item Requests" / Bill Items cart
     * shown in inward_bill_surgery_service.xhtml) from the surgery service
     * BillItems already saved to the DB against this surgery bill.
     *
     * Each time the surgery service cart is settled, settleBillSurgery() ->
     * saveBill() creates one or more Service sub-bills whose
     * forwardReferenceBill points back at the surgery bill (see
     * BillBeanController.setSurgeryData). resetBillData() clears
     * lstBillEntries on every navigation into this page, so without this,
     * previously requested/billed items never reappear (issue #20893).
     */
    private void loadExistingSurgeryServiceEntries(Bill surgeryBill) {
        lstBillEntries = new ArrayList<>();
        if (surgeryBill == null) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("surgeryBill", surgeryBill);
        params.put("surgeryBillType", SurgeryBillType.Service);
        String jpql = "select bi from BillItem bi "
                + "where bi.bill.forwardReferenceBill = :surgeryBill "
                + "and bi.bill.surgeryBillType = :surgeryBillType "
                + "and bi.retired = false "
                + "and bi.bill.retired = false "
                + "order by bi.id";
        List<BillItem> existingBillItems = billItemFacade.findByJpql(jpql, params);
        if (existingBillItems == null) {
            return;
        }
        for (BillItem bItem : existingBillItems) {
            BillEntry entry = new BillEntry();
            entry.setBillItem(bItem);
            entry.setLstBillComponents(getBillBean().billComponentsFromBillItem(bItem));
            entry.setLstBillFees(existingBillFeesForBillItem(bItem));
            entry.setLstBillSessions(getBillBean().billSessionsfromBillItem(bItem));
            lstBillEntries.add(entry);
        }
    }

    /**
     * Fetches the BillFees already persisted for a previously-billed BillItem,
     * so re-displaying the item shows the fees actually charged rather than
     * recomputing them against the current price matrix.
     */
    private List<BillFee> existingBillFeesForBillItem(BillItem bItem) {
        Map<String, Object> params = new HashMap<>();
        params.put("billItem", bItem);
        String jpql = "select bf from BillFee bf where bf.billItem = :billItem and bf.retired = false order by bf.id";
        List<BillFee> fees = billFeeFacade.findByJpql(jpql, params);
        return fees != null ? fees : new ArrayList<>();
    }

    public String navigateToPrintLabelsForInvestigations() {
        String json = generateStockerPrinterString();
        stickers = convertJsonToList(json);
        return "/inward/inward_bill_service_investigation_label_print?faces-redirect=true";
    }

    public String navigateToSampleManegmentFromInward() {
        patientInvestigationController.setBills(bills);
        patientInvestigationController.searchBillsWithoutSampleId();
        return "/lab/generate_barcode_p?faces-redirect=true";
    }

    public String navigateToSampleManegmentFromInwardIntrimBill(Bill b) {
        return patientInvestigationController.navigateToSampleManagementFromOPDBatchBillView(b);
    }

    public String navigateToNewBillFromPrintLabelsForInvestigations() {
        resetBillData();
        return "/inward/inward_bill_service?faces-redirect=true";
    }

    public List<InvestigationTubeSticker> convertJsonToList(String json) {
        List<InvestigationTubeSticker> stickers = new ArrayList<>();

        JSONObject jsonObject = new JSONObject(json);
        JSONArray barcodes = jsonObject.getJSONArray("Barcodes");

        for (int i = 0; i < barcodes.length(); i++) {
            JSONObject barcode = barcodes.getJSONObject(i);
            InvestigationTubeSticker sticker = new InvestigationTubeSticker();

            sticker.setInsid(barcode.getString("insid"));
            sticker.setTube(barcode.optString("tube", "")); // Using optString for optional fields
            sticker.setTests(barcode.getString("tests"));
            sticker.setPatientName(barcode.getString("name"));
            sticker.setPatientAge(barcode.getString("age"));
            sticker.setPatientSex(barcode.getString("sex"));
            sticker.setSampleId(barcode.getString("id"));
            sticker.setBillDateString(barcode.getString("billDate"));

            // Add more fields as necessary
            stickers.add(sticker);
        }

        return stickers;
    }

    public String generateStockerPrinterString() {
        //TODO: Prevent Duplicates
        JSONArray combinedBarcodes = new JSONArray();
        if (bills == null) {
            return "";
        }
        String username = sessionController.getUserName();
        String password = sessionController.getPassword();
        int count = 0;
        for (Bill b : bills) {
            String billId = b.getIdStr();
            String result = lims.generateSamplesFromBill(billId, username, password);
            JSONObject resultJson = new JSONObject(result);
            if (resultJson.has("Barcodes")) {
                JSONArray barcodes = resultJson.getJSONArray("Barcodes");
                for (int i = 0; i < barcodes.length(); i++) {
                    combinedBarcodes.put(barcodes.getJSONObject(i));
                }
            }
            count++;
        }
        JSONObject finalJson = new JSONObject();
        finalJson.put("Barcodes", combinedBarcodes);
        return finalJson.toString();
    }

    public void resetBillData() {
        date = null;
        total = 0.0;
        discount = 0.0;
        netTotal = 0.0;
        vat = 0.0;
        vatPlusNetTotal = 0.0;
        cashPaid = 0.0;
        cashBalance = 0.0;
        creditCardRefNo = "";
        chequeRefNo = "";
        chequeBank = null;
        currentBillItem = null;
        index = 0;
        patientEncounter = null;
        paymentScheme = null;
        lstBillComponents = null;
        lstBillFees = null;
        lstBillItems = null;
        lstBillEntries = null;
        printPreview = false;
        batchBill = null;
        bills = null;
        referredBy = null;
        currentBillItemPriority = null;
        currentBillItemQty = null;
        paymentMethod = null;
    }

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    public Date getDate() {
        if (date == null) {
            date = new Date();
        }
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void selectSurgeryBillListener() {
        patientEncounter = getBatchBill().getPatientEncounter();
        loadExistingSurgeryServiceEntries(getBatchBill());
    }

    public String navigateToAddServicesFromAdmissionProfile() {
        BillBhtController date = null;
        total = 0.0;
        discount = 0.0;
        netTotal = 0.0;
        vat = 0.0;
        vatPlusNetTotal = 0.0;
        cashPaid = 0.0;
        cashBalance = 0.0;
        creditCardRefNo = "";
        chequeRefNo = "";
        chequeBank = null;
        currentBillItem = null;
        index = 0;
        paymentScheme = null;
        lstBillComponents = null;
        lstBillFees = null;
        lstBillItems = null;
        lstBillEntries = null;
        printPreview = false;
        batchBill = null;
        bills = null;
        referredBy = null;
        currentBillItemQty = null;
        bhtSummeryController.setInstitution(sessionController.getInstitution());
        return "/inward/inward_bill_service?faces-redirect=true";
    }

    /**
     * Entry point from the Item/Service Request pending queue (issue #21793
     * redesign): seeds this controller's cart with the request's still-unfulfilled
     * lines, exactly as if the user had searched for and added each one manually,
     * then navigates to the same Add Services page. The user can edit/add/remove
     * before clicking the page's own Save button.
     */
    public String navigateToAddServicesFromItemRequest(Bill itemRequest, List<BillItem> remainingLines) {
        navigateToAddServicesFromAdmissionProfile();
        setPatientEncounter(itemRequest.getPatientEncounter());
        // navigateToAddServicesFromAdmissionProfile() resets lstBillEntries to null; normally
        // the next page render (via the getter) lazily re-initializes it before addToBill() is
        // ever invoked. Here we call addToBill() synchronously with no intervening render, so
        // force the lazy-init now to avoid a NullPointerException on the first entry.
        getLstBillEntries();
        for (BillItem requestLine : remainingLines) {
            BillItem seed = new BillItem();
            seed.setItem(requestLine.getItem());
            setCurrentBillItem(seed);
            setCurrentBillItemQty(requestLine.getQty());
            if (requestLine.getItem() != null && requestLine.getItem().getClass() == Investigation.class) {
                seed.setBillTime(new Date());
            }
            int sizeBefore = lstBillEntries.size();
            addToBill();
            if (lstBillEntries.size() > sizeBefore) {
                lstBillEntries.get(lstBillEntries.size() - 1).setSourceRequestBillItem(requestLine);
            }
        }
        return "/inward/inward_bill_service?faces-redirect=true";
    }

    public String navigateToAddServicesFromMenu() {
        BillBhtController date = null;
        patientEncounter = null;
        total = 0.0;
        discount = 0.0;
        netTotal = 0.0;
        vat = 0.0;
        vatPlusNetTotal = 0.0;
        cashPaid = 0.0;
        cashBalance = 0.0;
        creditCardRefNo = "";
        chequeRefNo = "";
        chequeBank = null;
        currentBillItem = null;
        index = 0;
        paymentScheme = null;
        lstBillComponents = null;
        lstBillFees = null;
        lstBillItems = null;
        lstBillEntries = null;
        printPreview = false;
        batchBill = null;
        bills = null;
        referredBy = null;
        marginTotal = 0.0;
        currentBillItemQty = null;
        bhtSummeryController.setInstitution(sessionController.getInstitution());
        return "/inward/inward_bill_service?faces-redirect=true";
    }

    @Inject
    private BillSearch billSearch;

    private void saveBatchBill() {
        Bill tmp = new BilledBill();
        tmp.setCreatedAt(new Date());
        tmp.setCreater(getSessionController().getLoggedUser());
        tmp.setBillTypeAtomic(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
        tmp.setPatient(patientEncounter.getPatient());
        boolean opdBillNumberGenerateStrategySingleNumberForOpdAndInpatientInvestigationsAndServices = configOptionApplicationController.getBooleanValueByKey("OpdBillNumberGenerateStrategy:SingleNumberForOpdAndInpatientInvestigationsAndServices", false);
        String batchBillId = "";
        
        if (opdBillNumberGenerateStrategySingleNumberForOpdAndInpatientInvestigationsAndServices) {
            List<BillTypeAtomic> opdAndInpatientBills = BillTypeAtomic.findOpdAndInpatientServiceAndInvestigationBatchBillTypes();
            batchBillId = billNumberBean.departmentBatchBillNumberGeneratorYearlyForInpatientAndOpdServices(getSessionController().getDepartment(), opdAndInpatientBills);
        }else{
            batchBillId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
        }
        
        tmp.setDeptId(batchBillId);
        tmp.setInsId(batchBillId);

        if (tmp.getId() == null) {
            getBillFacade().create(tmp);
        }

        for (Bill b : getBills()) {
            b.setBackwardReferenceBill(tmp);
            getBillFacade().edit(b);
        }

        for (Bill b : getBills()) {
            tmp.getForwardReferenceBills().add(b);
        }

        getBillFacade().edit(tmp);

    }

    public void cancellAll() {
        for (Bill b : getBills()) {
            getBillSearch().setBill((BilledBill) b);
            getBillSearch().setPaymentMethod(b.getPaymentMethod());
            getBillSearch().setComment("Batch Cancell");
            //////// // System.out.println("ggg : " + getBillSearch().getComment());
            getBillSearch().cancelOpdBill();
        }

    }

    public void putToBills(Department matrixDepartment, PaymentMethod paymentMethod) {

        Set<Department> billDepts = new HashSet<>();
        for (BillEntry e : lstBillEntries) {
            billDepts.add(e.getBillItem().getItem().getDepartment());
        }
        for (Department d : billDepts) {
            BilledBill myBill = new BilledBill();
            saveBill(d, myBill, matrixDepartment);
            List<BillEntry> tmp = new ArrayList<>();
            for (BillEntry e : lstBillEntries) {
                if (e.getBillItem().getItem().getDepartment().equals(d)) {
                    tmp.add(e);
                }
            }
            applyItemRequestReference(myBill, tmp);
            List<BillItem> tmpBis = saveBillItems(myBill, tmp, getSessionController().getLoggedUser(), matrixDepartment, paymentMethod);
            for (int i = 0; i < tmpBis.size(); i++) {
                tmpBis.get(i).setSearialNo(i);
            }
            getBillBean().calculateBillItems(myBill, tmp);
            myBill.setBillItems(tmpBis);
            getBills().add(myBill);
        }

    }

    /**
     * If any of the entries being saved onto this bill originated from an
     * Item/Service Request line (issue #21793 redesign), set the bill's
     * referenceBill so the request stays traceable to the bill it produced.
     * Each such entry's originating request BillItem is threaded onto the
     * real BillItem in {@link #saveBillItems(Bill, BillItem, BillEntry, List, WebUser, Department)}.
     */
    private void applyItemRequestReference(Bill bill, List<BillEntry> entries) {
        for (BillEntry e : entries) {
            if (e.getSourceRequestBillItem() != null && e.getSourceRequestBillItem().getBill() != null) {
                bill.setReferenceBill(e.getSourceRequestBillItem().getBill());
                return;
            }
        }
    }

    public BillItem saveBillItems(Bill bill, BillItem billItem, BillEntry billEntry, List<BillFee> billFees, WebUser wu, Department matrixDepartment) {

        billItem.setCreatedAt(new Date());
        billItem.setCreater(wu);
        billItem.setBill(bill);

        if (billItem.getInwardChargeType() == null && billItem.getItem() != null
                && billItem.getItem().getInwardChargeType() != null) {
            billItem.setInwardChargeType(billItem.getItem().getInwardChargeType());
        }

        if (billEntry != null && billEntry.getSourceRequestBillItem() != null) {
            billItem.setReferanceBillItem(billEntry.getSourceRequestBillItem());
        }

        if (billItem.getId() == null) {
            getBillItemFacade().create(billItem);
        }

        getBillBean().saveBillComponent(billEntry, bill, wu);

        for (BillFee bf : billFees) {
            getInwardBean().saveBillFee(bf, billItem, bill, wu);
            billItem.getBillFees().add(bf);
        }

        getBillBean().updateBillItemByBillFee(billItem);

        return billItem;
    }

    @Inject
    PriceMatrixController priceMatrixController;

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public void setPriceMatrixController(PriceMatrixController priceMatrixController) {
        this.priceMatrixController = priceMatrixController;
    }

    /**
     * The room category of the patient's current room, or null when the patient
     * is not yet in a room (or the room has no facility charge / category). Used
     * as the room-category dimension of the inward service-margin matrix lookup
     * (issue #21977); null means "wildcard row only", preserving legacy behaviour.
     */
    private RoomCategory resolveCurrentRoomCategory(PatientEncounter encounter) {
        if (encounter == null
                || encounter.getCurrentPatientRoom() == null
                || encounter.getCurrentPatientRoom().getRoomFacilityCharge() == null) {
            return null;
        }
        return encounter.getCurrentPatientRoom().getRoomFacilityCharge().getRoomCategory();
    }

    public List<BillItem> saveBillItems(Bill bill, List<BillEntry> billEntries, WebUser webUser, Department matrixDepartment, PaymentMethod paymentMethod) {
        List<BillItem> list = new ArrayList<>();
        for (BillEntry e : billEntries) {
            double staffFee = 0.0;
            double collectingCentreFee = 0.0;
            double hospitalFee = 0.0;
            double reagentFee = 0.0;
            double otherFee = 0.0;
            double marginFee = 0.0;

            BillItem billItem = saveBillItems(bill, e.getBillItem(), e, e.getLstBillFees(), webUser, matrixDepartment);
            billItem.setSearialNo(list.size());

            for (BillFee bf : billItem.getBillFees()) {
                PriceMatrix priceMatrix = getPriceMatrixController().fetchInwardMargin(billItem, bf.getFeeUnitGrossValue() != null ? bf.getFeeUnitGrossValue() : bf.getFeeGrossValue(), matrixDepartment, paymentMethod, null, bill.getPatientEncounter() != null ? bill.getPatientEncounter().getAdmissionType() : null, resolveCurrentRoomCategory(bill.getPatientEncounter()));
                getInwardBean().setBillFeeMargin(bf, bf.getBillItem().getItem(), priceMatrix, bill.getPatientEncounter());
                getBillFeeFacade().edit(bf);

                if (bf.getFee().getFeeType() == FeeType.CollectingCentre) {
                    collectingCentreFee += bf.getFeeValue();
                } else if (bf.getFee().getFeeType() == FeeType.Staff) {
                    staffFee += bf.getFeeValue();
                } else if (bf.getFee().getFeeType() == FeeType.Chemical) {
                    reagentFee += bf.getFeeValue();
                } else if (bf.getFee().getFeeType() == FeeType.Additional) {
                    otherFee += bf.getFeeValue();
                } else {
                    hospitalFee += bf.getFeeValue();
                }

                marginFee += bf.getFeeMargin();
            }

            billItem.setHospitalFee(hospitalFee);
            billItem.setCollectingCentreFee(collectingCentreFee);
            billItem.setReagentFee(reagentFee);
            billItem.setOtherFee(otherFee);
            billItem.setStaffFee(staffFee);
            billItem.setMarginValue(marginFee);

            billItemFacade.editAndCommit(billItem);

            list.add(billItem);

        }

        getBillBean().updateBillByBillFee(bill);

        return list;
    }

    public List<ItemLight> fillInwardItems() {
        UserPreference up = sessionController.getDepartmentPreference();
        switch (up.getInwardItemListingStrategy()) {
            case ALL_ITEMS:
                return itemApplicationController.getInvestigationsAndServices();
            case ITEMS_MAPPED_TO_LOGGED_DEPARTMENT:
                return itemMappingController.fillItemLightByDepartment(sessionController.getDepartment());
            case ITEMS_MAPPED_TO_LOGGED_INSTITUTION:
                return itemMappingController.fillItemLightByInstitution(sessionController.getInstitution());
            case ITEMS_OF_LOGGED_DEPARTMENT:
                return itemController.getDepartmentItems();
            case ITEMS_OF_LOGGED_INSTITUTION:
                return itemController.getInstitutionItems();
            default:
                return itemApplicationController.getInvestigationsAndServices();
        }
    }

    private void settleBill(Department matrixDepartment, PaymentMethod paymentMethod) {
        if (getBillBean().calculateNumberOfBillsPerOrder(getLstBillEntries()) == 1) {
            BilledBill temp = new BilledBill();
            Bill b = saveBill(lstBillEntries.get(0).getBillItem().getItem().getDepartment(), temp, matrixDepartment);
            applyItemRequestReference(b, getLstBillEntries());

            List<BillItem> list = saveBillItems(b, getLstBillEntries(), getSessionController().getLoggedUser(), matrixDepartment, paymentMethod);
            b.setBillItems(list);
            
            Priority highestPriority = Optional
                    .ofNullable(list)
                    .orElse(Collections.emptyList())
                    .stream()
                    .filter(bi -> bi.getPriority() != null)
                    .map(BillItem::getPriority)
                    .max(Comparator.comparingInt(Priority::getLevel))
                    .orElse(Priority.NORMAL);

            b.setPriority(highestPriority);
            
            billFacade.edit(b);
            getBillBean().calculateBillItems(b, getLstBillEntries());
            getBills().add(b);
        } else {
            putToBills(matrixDepartment, paymentMethod);
        }

        printPreview = true;
        saveBatchBill();

        JsfUtil.addSuccessMessage("Bill Saved");

    }

    public void settleBill() {
        bills = null;
        if (errorCheck()) {
            return;
        }
        paymentMethod = null;
        if (getPatientEncounter().getAdmissionType().isRoomChargesAllowed() || getPatientEncounter().getCurrentPatientRoom() != null) {
            settleBill(getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment(), getPatientEncounter().getPaymentMethod());
        } else {
            settleBill(getPatientEncounter().getDepartment(), getPatientEncounter().getPaymentMethod());
        }
    }

    public void settleBillSurgery() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (getBatchBill() == null) {
            return;
        }

        if (batchBill.getBillType() == BillType.SurgeryBill
                && surgeryBillController.isSurgeryLockedForAdditions(batchBill)) {
            JsfUtil.addErrorMessage("This surgery has been validated and is locked. Revert validation to make changes.");
            return;
        }

        if (getBatchBill().getProcedure() == null) {
            return;
        }

        if (getBatchBill().getFromDepartment() == null) {
            return;
        }

        if (getBatchBill().getPatientEncounter().isNursingDischarged()
                && !webUserController.hasPrivilege("InwardAddChargesAfterNursingDischarge")) {
            JsfUtil.addErrorMessage("Cannot add charges: nursing discharge has been confirmed for this patient.");
            return;
        }

        if (getBatchBill().getPatientEncounter().isDischarged()) {
            JsfUtil.addErrorMessage("Sorry Patient is Discharged!!!");
            return;
        }

        settleBill(getBatchBill().getFromDepartment(), getPatientEncounter().getPaymentMethod());

        getBillBean().saveEncounterComponents(getBills(), batchBill, getSessionController().getLoggedUser());
        getBillBean().updateBatchBill(getBatchBill());

        if (batchBill.getBillType() == BillType.SurgeryBill) {
            surgeryBillController.refreshSurgeryServiceDepartmentItems();
        }

    }

    @EJB
    private EncounterComponentFacade encounterComponentFacade;
    PaymentMethod paymentMethod;

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    private Bill saveBill(Department bt, BilledBill temp, Department matrixDepartment) {
        temp.setBillType(BillType.InwardBill);
        temp.setBillTypeAtomic(BillTypeAtomic.INWARD_SERVICE_BILL);
        temp.setIpOpOrCc("IP");
        getBillBean().setSurgeryData(temp, getBatchBill(), SurgeryBillType.Service);

        temp.setDepartment(getSessionController().getLoggedUser().getDepartment());
        temp.setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        temp.setPatient(patientEncounter.getPatient());
        temp.setFromDepartment(matrixDepartment);

        temp.setToDepartment(bt);
        temp.setToInstitution(bt.getInstitution());

        temp.setBillDate(date);
        temp.setBillTime(date);
        temp.setPatientEncounter(patientEncounter);
        temp.setPaymentScheme(getPaymentScheme());
        temp.setPaymentMethod(paymentMethod);
        temp.setReferredBy(referredBy);
        temp.setCreatedAt(new Date());
        temp.setBillDate(new Date());
        temp.setBillTime(new Date());
        temp.setCreater(getSessionController().getLoggedUser());

        boolean inpatientServiceBillNumberGenerateStrategyForFromDepartmentAndToDepartmentCombination
                = configOptionApplicationController.getBooleanValueByKey(
                        "InpatientServiceBillNumberGenerateStrategy:FromDepartmentToDepartmentBillTypes", false);

        boolean inpatientServiceBillNumberGenerateStrategySingleNumberForOpdAndInpatientInvestigationsAndServices
                = configOptionApplicationController.getBooleanValueByKey("OPD Bill Number Generation Strategy - Single Number for OPD and Inpatient Investigations and Services", false);

        boolean inpatientServiceBillNumberGenerateStrategyDefault
                = configOptionApplicationController.getBooleanValueByKey(
                        "InpatientServiceBillNumberGenerateStrategy:Default", false);

        String deptId;
        String insId;

        BillNumberGenerator bnb = getBillNumberBean();

        if (inpatientServiceBillNumberGenerateStrategyForFromDepartmentAndToDepartmentCombination) {
            deptId = bnb.departmentBillNumberGeneratorYearlyByFromDepartmentAndToDepartment(
                    bt, sessionController.getDepartment(), BillTypeAtomic.INWARD_SERVICE_BILL);
            insId = deptId;
        } else if (inpatientServiceBillNumberGenerateStrategySingleNumberForOpdAndInpatientInvestigationsAndServices) {
            List<BillTypeAtomic> opdAndInpatientBills = BillTypeAtomic.findOpdAndInpatientServiceAndInvestigationIndividualBillTypes();
            deptId = bnb.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), opdAndInpatientBills);
            insId = deptId;
        } else if (inpatientServiceBillNumberGenerateStrategyDefault) {
            deptId = bnb.departmentBillNumberGeneratorYearly(bt, BillTypeAtomic.INWARD_SERVICE_BILL);
            insId = deptId;
        } else {
            deptId = bnb.departmentBillNumberGenerator(temp.getDepartment(), temp.getToDepartment(), temp.getBillType(), BillClassType.BilledBill);
            insId = bnb.institutionBillNumberGenerator(temp.getInstitution(), temp.getToDepartment(), temp.getBillType(), BillClassType.BilledBill, BillNumberSuffix.INWSER);
        }

        temp.setDeptId(deptId);
        temp.setInsId(insId);

        if (temp.getId() == null) {
            getFacade().create(temp);
        } else {
            getFacade().edit(temp);
        }

        return temp;

    }

    public void logicalDischage() {
        getPatientEncounter().getCurrentPatientRoom().setDischarged(true);
        getPatientEncounter().getCurrentPatientRoom().setDischargedBy(getSessionController().getLoggedUser());
        JsfUtil.addSuccessMessage("Logically Dischaged Success");
    }

    private boolean errorCheck() {
        if (getLstBillEntries().isEmpty()) {

            JsfUtil.addErrorMessage("No investigations are added to the bill to settle");
            return true;
        }

        if (getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("Please select Bht Number");
            return true;
        }
        
        Patient billPatient = patientFacade.findWithoutCache(patientEncounter.getPatient().getId());
        
        if(billPatient.getPerson().getDob() == null){
            JsfUtil.addErrorMessage("Please add the Patinet DOB");
            return true;
        }
        
        if(billPatient.getPerson().getSex() == null){
            JsfUtil.addErrorMessage("Please add the Patinet Gender");
            return true;
        }

        //Check Staff
        if (checkStaff()) {
            JsfUtil.addErrorMessage("Please select Staff");
            return true;
        }

        if (getPatientEncounter().getAdmissionType().isRoomChargesAllowed() || getPatientEncounter().getCurrentPatientRoom() != null) {
            if (getPatientEncounter().getCurrentPatientRoom() == null) {
                return true;
            }

            if (getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge() == null) {
                return true;
            }
        }

        if (getPatientEncounter().isNursingDischarged()
                && !webUserController.hasPrivilege("InwardAddChargesAfterNursingDischarge")) {
            JsfUtil.addErrorMessage("Cannot add charges: nursing discharge has been confirmed for this patient.");
            return true;
        }

        if (getPatientEncounter().isDischarged()) {
            JsfUtil.addErrorMessage("Sorry Patient is Discharged!!!");
            return true;
        }

        return false;
    }

    public boolean checkStaff() {
        for (BillFee bf : lstBillFees) {
            if (bf.getFee() != null && bf.getFee().getFeeType() != null
                    && bf.getFee().getFeeType() == FeeType.Staff) {
                if (bf.getFeeGrossValue() != 0 && bf.getStaff() == null) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean errorCheckForPatientRoomDepartment() {

        if (getPatientEncounter().getCurrentPatientRoom() == null) {
            JsfUtil.addErrorMessage("Please Set Room or Bed For This Patient");
            return true;
        }

        if (getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge() == null) {
            JsfUtil.addErrorMessage("Please Set Room or Bed For This Patient");
            return true;
        }

        if (getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment() == null) {
            JsfUtil.addErrorMessage("Under administration, add a Department for this Room " + getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getName());
            return true;
        }

        return false;
    }

    private boolean errorCheckForAdding() {
        if (getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("Please Select BHT");
            return true;
        }

        if (getCurrentBillItem() == null) {
            JsfUtil.addErrorMessage("Nothing to add");
            return true;
        }
        if (getCurrentBillItem().getItem() == null) {
            JsfUtil.addErrorMessage("Please select an investigation or Services");
            return true;
        }

        if (getCurrentBillItem().getItem().getDepartment() == null) {
            JsfUtil.addErrorMessage("Please set To Department to This item");
            return true;

        }

        if (!getSessionController().getApplicationPreference().isInwardAddServiceBillTimeCheck()) {
            if (getCurrentBillItem().getItem().getClass() == Investigation.class) {
                if (getCurrentBillItem()
                        .getBillTime() == null) {
                    JsfUtil.addErrorMessage("Please set Time To This Investigation");
                    return true;
                }
//                if (getCurrentBillItem().getDescreption() == null || getCurrentBillItem().getDescreption().equals("")) {
//                    JsfUtil.addErrorMessage("Please set Discription To This Investigation");
//                    return true;
//                }
            }
        } else {
            getCurrentBillItem().setBillTime(new Date());
            getCurrentBillItem().setDescreption("");
        }

        if (getCurrentBillItem().getItem().getCategory() == null) {
            JsfUtil.addErrorMessage("Under administration, add Category For Item : " + getCurrentBillItem().getItem().getName());
            return true;
        }

        return false;
    }

    public void addToBill() {
        if (errorCheckForAdding()) {
            return;
        }

        if (patientEncounter.getAdmissionType().isRoomChargesAllowed() || patientEncounter.getCurrentPatientRoom() != null) {
            if (errorCheckForPatientRoomDepartment()) {
                return;
            }
        }

        for (BillEntry bi : lstBillEntries) {
            if (bi.getBillItem() != null && getCurrentBillItem() != null && getCurrentBillItem().getItem() != null && bi.getBillItem().getItem().equals(getCurrentBillItem().getItem())) {
                JsfUtil.addErrorMessage("Can't select same item " + getCurrentBillItem().getItem());
                return;
            }
        }

        if (getCurrentBillItem().getItem().isRequestForQuentity()) {
            if (currentBillItemQty == null || currentBillItemQty == 0.0) {
                currentBillItemQty = null;
                JsfUtil.addErrorMessage("Quantity is missing.");
                return;
            }
            if (currentBillItemQty < 0) {
                currentBillItemQty = null;
                JsfUtil.addErrorMessage("Quantity cannot be negative.");
                return;
            }
            if (currentBillItemQty % 1 != 0) {
                currentBillItemQty = null;
                JsfUtil.addErrorMessage("Quantity cannot be a decimal. Please enter a whole number.");
                return;
            }
        } else {
            currentBillItemQty = 1.0;
        }

        if (getCurrentBillItem().getItem().isAllowedForBillingPriority()) {
            if (currentBillItemPriority == null) {
                currentBillItemPriority = Priority.NORMAL;
            }
        } else {
            currentBillItemPriority = null;
        }

        BillEntry addingEntry = new BillEntry();
        BillItem bItem = new BillItem();
        bItem.copy(currentBillItem);
        bItem.setQty(currentBillItemQty);
        if (currentBillItemPriority != null) {
            bItem.setPriority(currentBillItemPriority);
        }
        addingEntry.setBillItem(bItem);
        addingEntry.setLstBillComponents(getBillBean().billComponentsFromBillItem(bItem));
        if (patientEncounter.getAdmissionType().isRoomChargesAllowed() || getPatientEncounter().getCurrentPatientRoom() != null) {
            addingEntry.setLstBillFees(billFeeFromBillItemWithMatrix(bItem, getPatientEncounter(), getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment(), getPatientEncounter().getPaymentMethod()));
        } else {
            addingEntry.setLstBillFees(billFeeFromBillItemWithMatrix(bItem, getPatientEncounter(), getPatientEncounter().getDepartment(), getPatientEncounter().getPaymentMethod()));
        }
        addingEntry.setLstBillSessions(getBillBean().billSessionsfromBillItem(bItem));
        bItem.setMarginValue(getBillBean().calBillItemMargin(addingEntry));

        // Block items with no fee configured (neither site nor base fee) before adding
        double feeGrossTotal = 0.0;
        if (addingEntry.getLstBillFees() != null) {
            for (BillFee bf : addingEntry.getLstBillFees()) {
                feeGrossTotal += bf.getFeeGrossValue();
            }
        }
        if (feeGrossTotal == 0.0) {
            JsfUtil.addErrorMessage("This item has no fee configured and cannot be added.");
            return;
        }

        lstBillEntries.add(addingEntry);

        bItem.setRate(getBillBean().billItemRate(addingEntry));

        calTotals();

        clearBillItemValues();
        //JsfUtil.addSuccessMessage("Item Added");
    }

    public List<BillFee> billFeeFromBillItemWithMatrix(BillItem billItem, PatientEncounter patientEncounter, Department matrixDepartment, PaymentMethod paymentMethod) {

        List<BillFee> billFeeList = new ArrayList<>();
        // Falls back to the old application-wide key so any pre-existing configuration
        // (set before this option was made discoverable/per-department) keeps working.
        boolean legacyGlobalSiteBasedBillFees = configOptionApplicationController.getBooleanValueByKey("Inward Bill Fees are based on the site", false);
        boolean siteBasedBillFees = sessionController.getDepartment() != null
                ? configOptionApplicationController.getBooleanValueByKey("Inward Bill Fees are based on the site for " + sessionController.getDepartment().getName(), legacyGlobalSiteBasedBillFees)
                : legacyGlobalSiteBasedBillFees;
        Institution site = sessionController.getDepartment() != null ? sessionController.getDepartment().getSite() : null;
        List<ItemFee> itemFee;

        if (siteBasedBillFees && site != null) {
            itemFee = itemFeeManager.fillFees(billItem.getItem(), site);
            if (itemFee == null || itemFee.isEmpty()) {
                // Fall back to base fees when the item has no site fee for this site
                itemFee = itemFeeManager.fillFees(billItem.getItem());
            }
        } else {
            itemFee = itemFeeManager.fillFees(billItem.getItem());
        }

        for (Fee i : itemFee) {
            BillFee billFee = getBillBean().createBillFee(billItem, i, patientEncounter);

            PriceMatrix priceMatrix = getPriceMatrixController().fetchInwardMargin(billItem, billFee.getFeeGrossValue(), matrixDepartment, paymentMethod, null, patientEncounter != null ? patientEncounter.getAdmissionType() : null, resolveCurrentRoomCategory(patientEncounter));

            getInwardBean().setBillFeeMargin(billFee, billItem.getItem(), priceMatrix, patientEncounter);

            recalculateFeeVat(billFee);

            billFeeList.add(billFee);
        }

        return billFeeList;
    }

    public void addToBillSurgery() {
        if (errorCheckForAdding()) {
            return;
        }

        if (getBatchBill().getFromDepartment() == null) {
            JsfUtil.addErrorMessage("There is no Department to for Matrix please set Department to surgery add again surgery ");
            return;
        }

        for (int i = 0; i < getCurrentBillItem().getQty(); i++) {
            BillEntry addingEntry = new BillEntry();
            BillItem bItem = new BillItem();

            bItem.copy(currentBillItem);
            bItem.setQty(1.0);
            addingEntry.setBillItem(bItem);
            addingEntry.setLstBillComponents(getBillBean().billComponentsFromBillItem(bItem));
            addingEntry.setLstBillFees(billFeeFromBillItemWithMatrix(bItem, getPatientEncounter(), getBatchBill().getFromDepartment(), getPatientEncounter().getPaymentMethod()));
            addingEntry.setLstBillSessions(getBillBean().billSessionsfromBillItem(bItem));
            lstBillEntries.add(addingEntry);

            bItem.setRate(getBillBean().billItemRate(addingEntry));

            calTotals();
            if (bItem.getNetValue() == 0.0) {
                JsfUtil.addErrorMessage("Please enter the rate");
                return;
            }
        }

        clearBillItemValues();
        //JsfUtil.addSuccessMessage("Item Added");
    }

    public void clearBillItemValues() {
        setCurrentBillItem(null);
        setItemLight(null);
        currentBillItemQty = null;
        recreateBillItems();
    }

    private void recreateBillItems() {
        //Only remove Total and BillComponenbts,Fee and Sessions. NOT bill Entries
        lstBillComponents = null;
        lstBillFees = null;
        lstBillItems = null;
        currentBillItemPriority = null;
        //billTotal = 0.0;
    }

    public void calTotals() {
        double tot = 0.0;
        double net = 0.0;
        double margin = 0.0;
        double vatTotal = 0.0;

        for (BillEntry be : getLstBillEntries()) {
            BillItem bi = be.getBillItem();

            bi.setDiscount(0.0);
            bi.setGrossValue(0.0);
            bi.setNetValue(0.0);
            bi.setMarginValue(0.0);
            bi.setVat(0.0);

            for (BillFee bf : be.getLstBillFees()) {
                tot += bf.getFeeGrossValue();
                net += bf.getFeeValue();
                bi.setNetValue(bi.getNetValue() + bf.getFeeValue());
                bi.setGrossValue(bi.getGrossValue() + bf.getFeeGrossValue());
                margin += bf.getFeeMargin();
                bi.setMarginValue(bi.getMarginValue() + bf.getFeeMargin());
                bi.setVat(bi.getVat() + bf.getFeeVat());
            }

            bi.setDiscount(bi.getGrossValue() + bi.getMarginValue() - bi.getNetValue());
            bi.setVatPercentage(bi.getItem() != null && bi.getItem().isVatable() ? bi.getItem().getVatPercentage() : 0.0);
            bi.setVatPlusNetValue(bi.getNetValue() + bi.getVat());
            vatTotal += bi.getVat();
        }

        setTotal(tot);
        setMarginTotal(margin);
        setDiscount(tot + margin - net);
        setNetTotal(net);
        setVat(vatTotal);
        setVatPlusNetTotal(getNetTotal() + getVat());
    }

    public void feeChanged(BillFee bf) {
        if (bf.getFeeGrossValue() == null) {
            return;
        }

        if (errorCheckForPatientRoomDepartment()) {
            return;
        }

        lstBillItems = null;
        getLstBillItems();

        // Recalculate the per-unit gross from the edited total so that setBillFeeMargin
        // uses the current unit rate rather than the stale creation-time value.
        double qty = (bf.getBillItem() != null && bf.getBillItem().getQty() != null && bf.getBillItem().getQty() > 0)
                ? bf.getBillItem().getQty() : 1.0;
        bf.setFeeUnitGrossValue(bf.getFeeGrossValue() / qty);

        PriceMatrix priceMatrix = getPriceMatrixController().fetchInwardMargin(bf.getBillItem(), bf.getFeeUnitGrossValue(), getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment(), getPatientEncounter().getPaymentMethod(), null, getPatientEncounter().getAdmissionType(), resolveCurrentRoomCategory(getPatientEncounter()));

        getInwardBean().updateBillItemMargin(bf, bf.getFeeGrossValue(), getPatientEncounter(), getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment(), priceMatrix);

        recalculateFeeVat(bf);

        calTotals();
    }

    public void feeChangedSurgery(BillFee bf) {
        if (bf.getFeeGrossValue() == null) {
            return;
        }

        if (getBatchBill() == null) {
            return;
        }

        if (getBatchBill().getFromDepartment() == null) {
            return;
        }

        lstBillItems = null;
        getLstBillItems();

        PriceMatrix priceMatrix = getPriceMatrixController().fetchInwardMargin(bf.getBillItem(), bf.getFeeGrossValue(), getBatchBill().getFromDepartment(), getPatientEncounter().getPaymentMethod(), null, getPatientEncounter().getAdmissionType(), resolveCurrentRoomCategory(getPatientEncounter()));

        getInwardBean().updateBillItemMargin(bf, bf.getFeeGrossValue(), getPatientEncounter(), getBatchBill().getFromDepartment(), priceMatrix);

        recalculateFeeVat(bf);

        calTotals();
    }

    private void recalculateFeeVat(BillFee bf) {
        if (bf.getBillItem() != null && bf.getBillItem().getItem() != null
                && bf.getBillItem().getItem().isVatable() && bf.getBillItem().getItem().getVatPercentage() > 0) {
            bf.setFeeVat(roundOff(bf.getFeeValue() * bf.getBillItem().getItem().getVatPercentage() / 100));
        } else {
            bf.setFeeVat(0.0);
        }
        bf.setFeeVatPlusValue(bf.getFeeValue() + bf.getFeeVat());
    }

    public void prepareNewBill() {
        clearBillItemValues();
        resetBillData();
        printPreview = false;

    }

    public void removeBillItem(BillEntry bi) {

        if (bi == null) {
            JsfUtil.addErrorMessage("Error! Please Try Again");
            return;
        }

        if (getEntriesIndex() == -1) {
            JsfUtil.addErrorMessage("Error! Please Try Again");
            return;
        }

        lstBillEntries.remove(entriesIndex);

        lstBillComponents = getBillBean().billComponentsFromBillEntries(lstBillEntries);
        lstBillFees = getBillBean().billFeesFromBillEntries(lstBillEntries);

        JsfUtil.addSuccessMessage("Successfully Removed");
        calTotals();

        setEntriesIndex(-1);

    }

    public void recreateList(BillEntry r) {
        List<BillEntry> temp = new ArrayList<>();
        for (BillEntry b : getLstBillEntries()) {
            if (b.getBillItem().getItem() != r.getBillItem().getItem()) {
                temp.add(b);
                //////// // System.out.println(b.getBillItem().getNetValue());
            }
        }
        lstBillEntries = temp;
        lstBillComponents = getBillBean().billComponentsFromBillEntries(lstBillEntries);
        lstBillFees = getBillBean().billFeesFromBillEntries(lstBillEntries);
    }

    public List<ItemLight> fillInwardItem() {
        UserPreference up = sessionController.getDepartmentPreference();
        List<ItemLight> temItems;
        switch (up.getInwardItemListingStrategy()) {
            case ALL_ITEMS:
                temItems = itemApplicationController.getInvestigationsAndServices();
                break;
            case ITEMS_MAPPED_TO_LOGGED_DEPARTMENT:
                temItems = itemMappingController.fillItemLightByDepartment(sessionController.getDepartment());
                break;
            case ITEMS_MAPPED_TO_LOGGED_INSTITUTION:
                temItems = itemMappingController.fillItemLightByInstitution(sessionController.getInstitution());
                break;
            case ITEMS_OF_LOGGED_DEPARTMENT:
                temItems = itemController.getDepartmentItems();
                break;
            case ITEMS_OF_LOGGED_INSTITUTION:
                temItems = itemController.getInstitutionItems();
                break;
            case SITE_FEE_ITEMS:
                temItems = itemFeeManager.fillItemLightsForSite(sessionController.getDepartment().getSite());
                break;
            default:
                temItems = itemApplicationController.getInvestigationsAndServices();
                break;
        }
        temItems = applyInwardFeeTotals(temItems);
        boolean listItemsByDepartment = configOptionApplicationController.getBooleanValueByKey("List Inward Items by Department", false);
        if (listItemsByDepartment) {
            fillInwardItemDepartments(temItems);
        } else {
            inwardItemDepartments = null;
        }
        if (getSelectedInwardItemDepartment() != null) {
            departmentInwardItems = filterItemLightesByDepartment(temItems, getSelectedInwardItemDepartment());
        }

        return temItems;
    }

    /**
     * Overrides the displayed price of each inward item so the list reflects the
     * fee that will actually be billed. When "Inward Bill Fees are based on the
     * site" is ON and the logged department has a site, the site fee is shown;
     * if the item has no site fee, the base fee is shown (matching the
     * site&rarr;base fallback in {@link #billFeeFromBillItemWithMatrix}). When the
     * config is OFF, the base fee is shown. Items with no fee show 0.00 and are
     * blocked from being added in {@link #addToBill()}.
     */
    private List<ItemLight> applyInwardFeeTotals(List<ItemLight> items) {
        if (items == null || items.isEmpty()) {
            return items;
        }
        boolean siteBasedBillFees = configOptionApplicationController.getBooleanValueByKey("Inward Bill Fees are based on the site", false);
        boolean foreigner = patientEncounter != null && patientEncounter.isForiegner();
        Institution site = sessionController.getDepartment() != null ? sessionController.getDepartment().getSite() : null;

        List<Long> itemIds = new ArrayList<>();
        for (ItemLight il : items) {
            if (il.getId() != null) {
                itemIds.add(il.getId());
            }
        }
        if (itemIds.isEmpty()) {
            return items;
        }

        boolean useSiteFees = siteBasedBillFees && site != null;
        Map<Long, Double> baseTotals = itemFeeManager.fetchInwardFeeTotalsByItemIds(itemIds, null, foreigner);
        Map<Long, Double> siteTotals = useSiteFees
                ? itemFeeManager.fetchInwardFeeTotalsByItemIds(itemIds, site, foreigner)
                : new HashMap<>();

        // Work on copies: the source lists can be application-scoped cached
        // ItemLight instances shared across sessions, so mutating them in place
        // would leak one session's price into another.
        List<ItemLight> result = new ArrayList<>(items.size());
        for (ItemLight src : items) {
            ItemLight il = new ItemLight(src);
            Double value;
            if (useSiteFees) {
                Double s = siteTotals.get(il.getId());
                value = (s != null && s != 0.0) ? s : baseTotals.get(il.getId());
            } else {
                value = baseTotals.get(il.getId());
            }
            il.setTotal(value != null ? value : 0.0);
            result.add(il);
        }
        return result;
    }

    private List<ItemLight> filterItemLightesByDepartment(List<ItemLight> ils, Department dept) {
        boolean listItemsByDepartment = configOptionApplicationController.getBooleanValueByKey("List Inward Items by Department", false);
        if (!listItemsByDepartment || dept == null || dept.getId() == null) {
            return ils;
        }
        List<ItemLight> tils = new ArrayList<>();
        for (ItemLight il : ils) {
            if (il.getDepartmentId() != null && il.getDepartmentId().equals(dept.getId())) {
                tils.add(il);
            }
        }
        return tils;
    }

    public void fillInwardItemDepartments(List<ItemLight> itemLightsToAddDepartments) {
        inwardItemDepartments = new ArrayList<>();
        Set<Long> uniqueDeptIds = new HashSet<>();
        for (ItemLight il : itemLightsToAddDepartments) {
            if (il.getDepartmentId() != null) {
                uniqueDeptIds.add(il.getDepartmentId());
            }
        }
        for (Long deptId : uniqueDeptIds) {
            Department d = departmentController.findDepartment(deptId);
            inwardItemDepartments.add(d);
        }
    }

    public void departmentChanged() {
        if (selectedInwardItemDepartment == null) {
            departmentInwardItems = getInwardItem();
        } else {
            departmentInwardItems = filterItemLightesByDepartment(getInwardItem(), getSelectedInwardItemDepartment());
        }
    }

    public BillFacade getEjbFacade() {
        return billFacade;
    }

    public void setEjbFacade(BillFacade ejbFacade) {
        this.billFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillBhtController() {
    }

    private BillFacade getFacade() {
        return billFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public List<BillComponent> getLstBillComponents() {
        if (lstBillComponents == null) {
            lstBillComponents = getBillBean().billComponentsFromBillEntries(getLstBillEntries());
        }

        return lstBillComponents;
    }

    public void setLstBillComponents(List<BillComponent> lstBillComponents) {
        this.lstBillComponents = lstBillComponents;
    }

    public List<BillFee> getLstBillFees() {
        if (lstBillFees == null) {
            lstBillFees = getBillBean().billFeesFromBillEntries(getLstBillEntries());
        }

        return lstBillFees;
    }

    public void setLstBillFees(List<BillFee> lstBillFees) {
        this.lstBillFees = lstBillFees;
    }

    public List<BillItem> getLstBillItems() {
        if (lstBillItems == null) {
            lstBillItems = new ArrayList<BillItem>();
        }
        return lstBillItems;
    }

    public void setLstBillItems(List<BillItem> lstBillItems) {
        this.lstBillItems = lstBillItems;
    }

    public List<BillEntry> getLstBillEntries() {
        if (lstBillEntries == null) {
            lstBillEntries = new ArrayList<BillEntry>();
        }
        return lstBillEntries;
    }

    public void setLstBillEntries(List<BillEntry> lstBillEntries) {
        this.lstBillEntries = lstBillEntries;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
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

    public double getVat() {
        return vat;
    }

    public void setVat(double vat) {
        this.vat = vat;
    }

    public double getVatPlusNetTotal() {
        return vatPlusNetTotal;
    }

    public void setVatPlusNetTotal(double vatPlusNetTotal) {
        this.vatPlusNetTotal = vatPlusNetTotal;
    }

    private double roundOff(double d) {
        return Math.round(d * 100.0) / 100.0;
    }

    public double getCashPaid() {
        return cashPaid;
    }

    public void setCashPaid(double cashPaid) {
        this.cashPaid = cashPaid;
    }

    public ItemLight getItemLight() {
        if (getCurrentBillItem().getItem() != null) {
            itemLight = new ItemLight(getCurrentBillItem().getItem());
        }
        return itemLight;
    }

    public void setItemLight(ItemLight itemLight) {
        this.itemLight = itemLight;
        if (itemLight != null) {
            getCurrentBillItem().setItem(itemController.findItem(itemLight.getId()));
            if (currentBillItemQty == null) {
                currentBillItemQty = 1.0;
            }
        }
    }

    public double getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(double cashBalance) {
        this.cashBalance = cashBalance;
    }

    public String getCreditCardRefNo() {
        return creditCardRefNo;
    }

    public void setCreditCardRefNo(String creditCardRefNo) {
        this.creditCardRefNo = creditCardRefNo;
    }

    public String getChequeRefNo() {
        return chequeRefNo;
    }

    public void setChequeRefNo(String chequeRefNo) {
        this.chequeRefNo = chequeRefNo;
    }

    public Institution getChequeBank() {
        if (chequeBank == null) {
            chequeBank = new Institution();
        }

        return chequeBank;
    }

    public void setChequeBank(Institution chequeBank) {
        this.chequeBank = chequeBank;
    }

    public BillItem getCurrentBillItem() {
        if (currentBillItem == null) {
            currentBillItem = new BillItem();
            currentBillItem.setQty(1.0);
            currentBillItem.setBillTime(new Date());
        }

        return currentBillItem;
    }

    public void setCurrentBillItem(BillItem currentBillItem) {
        this.currentBillItem = currentBillItem;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;

    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public void setPatientFacade(PatientFacade patientFacade) {
        this.patientFacade = patientFacade;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;

    }

    public BillComponentFacade getBillComponentFacade() {
        return billComponentFacade;
    }

    public void setBillComponentFacade(BillComponentFacade billComponentFacade) {
        this.billComponentFacade = billComponentFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public PatientInvestigationFacade getPatientInvestigationFacade() {
        return patientInvestigationFacade;
    }

    public void setPatientInvestigationFacade(PatientInvestigationFacade patientInvestigationFacade) {
        this.patientInvestigationFacade = patientInvestigationFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;

    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;

    }

    public PriceMatrixFacade getPriceAdjustmentFacade() {
        return priceAdjustmentFacade;
    }

    public void setPriceAdjustmentFacade(PriceMatrixFacade priceAdjustmentFacade) {
        this.priceAdjustmentFacade = priceAdjustmentFacade;
    }

    public FeeFacade getFeeFacade() {
        return feeFacade;
    }

    public void setFeeFacade(FeeFacade feeFacade) {
        this.feeFacade = feeFacade;
    }

    public ItemFeeFacade getItemFeeFacade() {
        return itemFeeFacade;
    }

    public void setItemFeeFacade(ItemFeeFacade itemFeeFacade) {
        this.itemFeeFacade = itemFeeFacade;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public List<Bill> getBills() {
        if (bills == null) {
            bills = new ArrayList<>();
        }
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public BillSearch getBillSearch() {
        return billSearch;
    }

    public void setBillSearch(BillSearch billSearch) {
        this.billSearch = billSearch;
    }

    public Bill getBatchBill() {
        return batchBill;
    }

    public void setBatchBill(Bill batchBill) {
        this.batchBill = batchBill;
    }

    public EncounterComponentFacade getEncounterComponentFacade() {
        return encounterComponentFacade;
    }

    public void setEncounterComponentFacade(EncounterComponentFacade encounterComponentFacade) {
        this.encounterComponentFacade = encounterComponentFacade;
    }

    public Doctor getReferredBy() {
        return referredBy;
    }

    public void setReferredBy(Doctor referredBy) {
        this.referredBy = referredBy;
    }

    public String getStickerPrinterString() {
        return stickerPrinterString;
    }

    public void setStickerPrinterString(String stickerPrinterString) {
        this.stickerPrinterString = stickerPrinterString;
    }

    public List<InvestigationTubeSticker> getStickers() {
        return stickers;
    }

    public void setStickers(List<InvestigationTubeSticker> stickers) {
        this.stickers = stickers;
    }

    public List<ItemLight> getInwardItems() {
        if (inwardItems == null) {
            inwardItems = fillInwardItems();
        }
        return inwardItems;
    }

    public void reloadItemLights() {
        itemApplicationController.reloadItems();
        itemController.reloadItems();
        // Force the inward item list (with its fee-based prices) to rebuild
        inwardItem = null;
        getInwardItem();
    }

    public void setInwardItems(List<ItemLight> inwardItems) {
        this.inwardItems = inwardItems;
    }

    public int getEntriesIndex() {
        return entriesIndex;
    }

    public void setEntriesIndex(int entriesIndex) {
        this.entriesIndex = entriesIndex;
    }

    public List<ItemLight> getInwardItem() {
        if (inwardItem == null) {
            inwardItem = fillInwardItem();
        }
        return inwardItem;
    }

    public void setInwardItem(List<ItemLight> inwardItem) {
        this.inwardItem = inwardItem;
    }

    public List<ItemLight> getDepartmentInwardItems() {
        getInwardItem();
        departmentInwardItems = filterItemLightesByDepartment(getInwardItem(), getSelectedInwardItemDepartment());
        return departmentInwardItems;
    }

    public void setDepartmentInwardItems(List<ItemLight> departmentInwardItems) {
        this.departmentInwardItems = departmentInwardItems;
    }

    public Department getSelectedInwardItemDepartment() {
        if (selectedInwardItemDepartment == null) {
            if (inwardItemDepartments != null && !inwardItemDepartments.isEmpty()) {
                selectedInwardItemDepartment = inwardItemDepartments.get(0);
            }
        }
        return selectedInwardItemDepartment;
    }

    public void setSelectedInwardItemDepartment(Department selectedInwardItemDepartment) {
        this.selectedInwardItemDepartment = selectedInwardItemDepartment;
    }

    public List<Department> getInwardItemDepartments() {
        if (inwardItemDepartments == null) {
            getInwardItem();
        }
        return inwardItemDepartments;
    }

    public void setInwardItemDepartments(List<Department> inwardItemDepartments) {
        this.inwardItemDepartments = inwardItemDepartments;
    }

    public double getMarginTotal() {
        return marginTotal;
    }

    public void setMarginTotal(double marginTotal) {
        this.marginTotal = marginTotal;
    }

    public Priority getCurrentBillItemPriority() {
        if(currentBillItemPriority == null){
            currentBillItemPriority = Priority.NORMAL;
        }
        return currentBillItemPriority;
    }

    public void setCurrentBillItemPriority(Priority currentBillItemPriority) {
        this.currentBillItemPriority = currentBillItemPriority;
    }

    public Double getCurrentBillItemQty() {
        return currentBillItemQty;
    }

    public void setCurrentBillItemQty(Double currentBillItemQty) {
        this.currentBillItemQty = currentBillItemQty;
    }

}
