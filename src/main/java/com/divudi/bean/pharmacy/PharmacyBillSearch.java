/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SearchController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.bean.inward.InwardBeanController;
import com.divudi.bean.pharmacy.PharmacyReturnwithouttresing;
import com.divudi.bean.pharmacy.PreReturnController;
import com.divudi.bean.pharmacy.SaleReturnController;
import com.divudi.bean.store.StoreBillSearch;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.PaperType;
import com.divudi.core.data.Privileges;
import com.divudi.core.data.PaymentMethod;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;

import com.divudi.ejb.EjbApplication;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.service.StaffService;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillComponent;
import com.divudi.core.entity.BillEntry;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.RoomCategory;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillComponentFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillFeePaymentFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ItemBatchFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.EmailFacade;
import com.divudi.ejb.EmailManagerEjb;
import com.divudi.core.entity.AppEmail;
import com.divudi.core.data.MessageType;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.StockBill;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.data.dto.PharmacySaleSearchDTO;
import com.divudi.core.data.dto.PharmacyTransferIssueSearchDTO;
import com.divudi.service.BillService;
import com.divudi.service.pharmacy.TransferIssueNativeSqlService;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.primefaces.event.RowEditEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class PharmacyBillSearch implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(PharmacyBillSearch.class.getName());

    private static final String DEFAULT_INWARD_PHARMACY_REQUEST_REPRINT_RETURN_PAGE =
            "/ward/ward_pharmacy_bht_issue_request_list_for_issue?faces-redirect=true";

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private StaffService staffBean;
    @EJB
    BillService billService;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private BillItemFacade billItemFacede;
    @EJB
    private BillComponentFacade billCommponentFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;

    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private EjbApplication ejbApplication;
    @EJB
    private BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    private EmailFacade emailFacade;
    @EJB
    private EmailManagerEjb emailManagerEjb;
    @EJB
    private TransferIssueNativeSqlService transferIssueNativeSqlService;
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    SessionController sessionController;
    @Inject
    GrnController grnController;
    @Inject
    private WebUserController webUserController;
    @Inject
    InwardBeanController inwardBean;
    @Inject
    PharmacySaleController pharmacySaleController;
    @Inject
    DrawerController drawerController;
    @Inject
    PharmacyRequestForBhtController pharmacyRequestForBhtController;
    @Inject
    PharmacyCalculation pharmacyCalculation;
    @Inject
    GrnCostingController grnCostingController;
    @Inject
    GrnCostingNativeSqlController grnCostingNativeSqlController;
    @Inject
    SearchController searchController;
    @Inject
    PreReturnController preReturnController;
    @Inject
    SaleReturnController saleReturnController;
    @Inject
    PharmacyReturnwithouttresing pharmacyReturnwithouttresing;
    @Inject
    PharmacyDirectPurchaseController pharmacyDirectPurchaseController;
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    private UploadedFile file;
    private boolean printPreview = false;
    @Deprecated
    private boolean showAllBillFormats = false;
    private double refundAmount;
    private String txtSearch;
    private String comment;
    private String emailRecipient;
    private Bill bill;
    /**
     * The page to navigate to when the Back button on the inward pharmacy
     * request reprint page is pressed. Callers set this via
     * f:setPropertyActionListener before navigating to that page; defaults
     * to the all-requests list if a caller does not set it.
     */
    private String returnPage;
    private PaymentMethod paymentMethod;
    private PaymentScheme paymentScheme;
    private RefundBill billForRefund;
    private Date fromDate;
    private Date toDate;
    private WebUser user;
    private StoreBillSearch storeBillSearch;
    private List<BillItem> refundingItems;
    private List<Bill> bills;
    private List<Bill> filteredBill;
    private List<Bill> selectedBills;
    private List<BillEntry> billEntrys;
    private List<BillItem> billItems;
    private List<BillComponent> billComponents;
    private List<BillFee> billFees;
    private List<BillItem> tempbillItems;
    private List<Bill> searchRetaiBills;
    // Bill id used when navigating directly from DTO tables
    private Long billId;
    private List<PharmacySaleSearchDTO> saleBillDtos;
    private List<com.divudi.core.data.dto.PharmacySaleBillItemSearchDTO> saleBillItemDtos;
    private List<com.divudi.core.data.dto.PharmacyTransferRequestListDTO> transferRequestSearchDtos;
    private List<PharmacyTransferIssueSearchDTO> transferIssueSearchDtos;
    private List<com.divudi.core.data.dto.PharmacyTransferReceivedListDTO> transferReceiveSearchDtos;
    private List<com.divudi.core.data.dto.PharmacyPreBillSearchDTO> preBillSearchDtos;
    private List<com.divudi.core.data.dto.PharmacyWholeSaleSearchDTO> wholeSaleSearchDtos;
    private List<com.divudi.core.data.dto.PharmacyPurchaseOrderDTO> poRequestSearchDtos;
    private List<com.divudi.core.data.dto.PharmacyPurchaseOrderDTO> poApproveSearchDtos;
    private List<com.divudi.core.data.dto.PharmacyGrnSearchDTO> grnSearchDtos;
    private List<com.divudi.core.data.dto.PharmacyDirectPurchaseSearchDTO> purchaseSearchDtos;
    private List<com.divudi.core.data.dto.PharmacyGrnReturnSearchDTO> grnReturnSearchDtos;
    private List<com.divudi.core.data.dto.PharmacyReturnWithoutTraisingSearchDTO> returnWithoutTraisingSearchDtos;
    private List<com.divudi.core.data.dto.PharmacyIssueSearchDTO> issueSearchDtos;
    private List<com.divudi.core.data.dto.PharmacyAdjustmentSearchDTO> adjustmentSearchDtos;
    private List<com.divudi.core.data.dto.PharmacyGrnPaymentSearchDTO> grnPaymentSearchDtos;
    private List<com.divudi.core.data.dto.PharmacyPurchaseReturnSearchDTO> purchaseReturnSearchDtos;

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Constructors">
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Navigation Methods">
    public String navigateToCancelPharmacyDirectIssueToInpatients() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        if (bill.getCheckeAt() != null) {
            JsfUtil.addErrorMessage("This bill is already checked. A checked bill cannot be cancelled.");
            return null;
        }
        return "/inward/pharmacy_cancel_bill_retail_bht?faces-redirect=true";
    }

    public String navigateToReversePharmacyDirectIssueToInpatients() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        return "/dataAdmin/pharmacy_bill_reverse_bht?faces-redirect=true";
    }

    public String navigateToReversePharmacyDirectIssueToInpatientsById(Long billId) {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill ID Provided");
            return null;
        }

        Bill selectedBill = billService.fetchBillById(billId);
        if (selectedBill == null) {
            JsfUtil.addErrorMessage("Bill Not Found");
            return null;
        }

        if (selectedBill.getBillType() != BillType.PharmacyBhtPre) {
            JsfUtil.addErrorMessage("Invalid Bill Type for Reversal");
            return null;
        }

        bill = selectedBill;
        printPreview = false;
        return "/dataAdmin/pharmacy_bill_reverse_bht?faces-redirect=true";
    }

    public String navigateToReprintPharmacyPurchaseOrder() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No purchase order is selected to view");
            return null;
        }
        return "/pharmacy/pharmacy_reprint_po?faces-redirect=true";
    }

    public String navigateToReprintPharmacyPurchaseOrderFromDto() {
        if (billId == null) {
            JsfUtil.addErrorMessage("No purchase order is selected to view");
            return null;
        }
        bill = billService.reloadBill(billId);
        if (bill == null) {
            JsfUtil.addErrorMessage("Purchase order not found");
            return null;
        }
        return "/pharmacy/pharmacy_reprint_po?faces-redirect=true";
    }

    public String navigateToReprintPharmacyOrderRequestById() {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        Bill tb = billBean.fetchBillWithItemsAndFees(billId);
        if (tb == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        bill = tb;
        return "/pharmacy/pharmacy_reprint_order_request?faces-redirect=true";
    }

    public String navigateToImportBillsFromJson() {
        return "/pharmacy/admin/import_bill?faces-redirect=true";
    }

    public String navigateToViewPharmacyDirectIssueForInpatientBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Found");
            return null;
        }
        // Reload the bill with its billItems. When this is reached from the BHT
        // Issue Return page, `bill` is a detached entity whose billItems were
        // never fetched, so the reprint page's Item/QTY table renders empty
        // (issue #22035). Fetching with items populates that table.
        Bill reloaded = billBean.fetchBillWithItemsAndFees(bill.getId());
        if (reloaded != null) {
            bill = reloaded;
        }
        return "/inward/pharmacy_reprint_bill_sale_bht?faces-redirect=true";
    }

    public String navigateToCancelPharmacyRetailSale() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        paymentMethod = bill.getPaymentMethod();
        return "/pharmacy/pharmacy_cancel_bill_retail?faces-redirect=true";
    }

    public String navigatePharmacyReprintPo() {
        return "/pharmacy/pharmacy_reprint_po?faces-redirect=true";
    }

    private boolean isManageCostingEnabled() {
        return configOptionApplicationController.getBooleanValueByKey("Manage Costing", true);
    }

    public String navigateToPharmacyGrnReprint() {
        if (isManageCostingEnabled()) {
            return "/pharmacy/pharmacy_reprint_grn_with_costing?faces-redirect=true";
        }
        return "/pharmacy/pharmacy_reprint_grn?faces-redirect=true";
    }

    public String navigateToPharmacyGrnReprintById() {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        Bill tb = billBean.fetchBillWithItemsAndFees(billId);
        if (tb == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        bill = tb;
        if (isManageCostingEnabled()) {
            return "/pharmacy/pharmacy_reprint_grn_with_costing?faces-redirect=true";
        }
        return "/pharmacy/pharmacy_reprint_grn?faces-redirect=true";
    }

    public String navigateToPharmacyPurchaseReprintById() {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        Bill tb = billBean.fetchBillWithItemsAndFees(billId);
        if (tb == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        bill = tb;
        if (tb.getBillType() == com.divudi.core.data.BillType.PurchaseReturn) {
            return "/pharmacy/pharmacy_reprint_purchase_return?faces-redirect=true";
        }
        return "/pharmacy/pharmacy_reprint_purchase?faces-redirect=true";
    }

    /**
     * Id-based counterpart of the "Return Note No" column link on
     * purchase_return.xhtml (issue #20523). Fetches the PurchaseReturn bill
     * fresh by id and navigates to its reprint page, replacing the old
     * {@code f:setPropertyActionListener value="#{bill}"} entity binding now
     * that the search table is driven by PharmacyPurchaseReturnSearchDTO
     * rows.
     *
     * @param billId id of the PurchaseReturn bill (the return note itself)
     * @return the navigation outcome, or {@code null} if the bill could not be found
     */
    public String navigateToDirectPurchaseReturnById(Long billId) {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        Bill tb = billBean.fetchBillWithItemsAndFees(billId);
        if (tb == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        bill = tb;
        return "/pharmacy/pharmacy_reprint_direct_purchase_return?faces-redirect=true";
    }

    /**
     * Id-based counterpart of the "Purchase No" column link on
     * purchase_return.xhtml (issue #20523). Fetches the original purchase
     * bill fresh by id and navigates to its reprint page, replacing the old
     * {@code f:setPropertyActionListener value="#{bill.referenceBill}"}
     * entity binding now that the search table is driven by
     * PharmacyPurchaseReturnSearchDTO rows (which only carry the
     * referenced bill's id/deptId, not the entity).
     *
     * @param billId id of the original purchase bill (the PurchaseReturn's referenceBill)
     * @return the navigation outcome, or {@code null} if the bill could not be found
     */
    public String navigateToPurchaseById(Long billId) {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        Bill tb = billBean.fetchBillWithItemsAndFees(billId);
        if (tb == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        bill = tb;
        return "/pharmacy/pharmacy_reprint_purchase?faces-redirect=true";
    }

    /**
     * Id-based counterpart of the "Actions" column button on
     * pharmacy_transfer_recieve.xhtml (issue #20523). Fetches the
     * PharmacyTransferReceive bill fresh by id and navigates to its reprint
     * page, replacing the old {@code f:setPropertyActionListener value="#{bill}"}
     * entity binding now that the search table is driven by
     * PharmacyTransferReceivedListDTO rows.
     *
     * @param billId id of the PharmacyTransferReceive bill
     * @return the navigation outcome, or {@code null} if the bill could not be found
     */
    public String navigateToTransferReceiveReprintById(Long billId) {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        Bill tb = billBean.fetchBillWithItemsAndFees(billId);
        if (tb == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        bill = tb;
        return "/pharmacy/pharmacy_reprint_transfer_receive?faces-redirect=true";
    }

    public String navigateToPharmacyGrnReturnReprintById() {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        Bill tb = billBean.fetchBillWithItemsAndFees(billId);
        if (tb == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        bill = tb;
        return "/pharmacy/pharmacy_reprint_grn_return?faces-redirect=true";
    }

    public String navigateToPharmacyReturnWithoutTraisingById() {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        Bill tb = billBean.fetchBillWithItemsAndFees(billId);
        if (tb == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        bill = tb;
        if (pharmacyReturnwithouttresing != null) {
            pharmacyReturnwithouttresing.setPrintBill(tb);
            pharmacyReturnwithouttresing.setBillPreview(true);
        }
        return "/pharmacy/pharmacy_return_withouttresing?faces-redirect=true";
    }

    /**
     * @deprecated Use {@link #navigateToViewPharmacyTransferRequestById()}
     * which works with DTO driven tables by accepting only the bill id.
     */
    @Deprecated
    public String navigateToViewPharmacyTransferReqest() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        return "/pharmacy/pharmacy_reprint_transfer_request?faces-redirect=true";
    }

    /**
     * Navigation helper when only the bill id is available (e.g. from DTO
     * tables).
     */
    public String navigateToViewPharmacyTransferRequestById() {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        return viewRequestByBillId(billId);
    }

    public String viewRequestByBillId(Long billId) {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        bill = transferIssueNativeSqlService.loadRequestBillForView(billId);
        if (bill == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        return "/pharmacy/pharmacy_reprint_transfer_request?faces-redirect=true";
    }

    public String navigatePharmacyReprintRetailBill() {
        if (bill == null && billId != null) {
            bill = billService.reloadBill(billId);
        }
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        bill = billService.reloadBill(bill);
        return "/pharmacy/pharmacy_reprint_bill_sale?faces-redirect=true";
    }
    
    public String navigatePharmacyReturnRetailBill() {
        searchController.createReturnSaleBills();
        
        return "/pharmacy/pharmacy_search_return_bill_pre.xhtml?faces-redirect=true";
    }

    public String navigateToViewPharmacyRetailCancellationBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        if (bill.getBillTypeAtomic() == null) {
            JsfUtil.addErrorMessage("No Bill Type");
            return null;
        }
        if (bill.getBillTypeAtomic() != BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED) {
            JsfUtil.addErrorMessage("Wrong Bill Type");
            return null;
        }

        return "/pharmacy/pharmacy_reprint_retail_cancelltion_bill?faces-redirect=true";
    }

    public String navigateToViewPharmacyRetailCancellationPreBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        if (bill.getBillTypeAtomic() == null) {
            JsfUtil.addErrorMessage("No Bill Type");
            return null;
        }
        if (bill.getBillTypeAtomic() != BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED_PRE) {
            JsfUtil.addErrorMessage("Wrong Bill Type");
            return null;
        }
        return "/pharmacy/pharmacy_reprint_retail_cancelltion_bill?faces-redirect=true";
    }

    public String navigateToReprintPharmacyTransferIssue() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        return "/pharmacy/pharmacy_reprint_transfer_isssue?faces-redirect=true";
    }

    /**
     * Used by DTO reports where only the bill id is available.
     */
    public String navigateToReprintPharmacyTransferIssueById() {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        Bill tb = billBean.fetchBillWithItemsAndFees(billId);
        if (tb == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        bill = tb;
        return "/pharmacy/pharmacy_reprint_transfer_isssue?faces-redirect=true";
    }

    public String navigateToReprintInpatientIssueForRequestsById() {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        Bill tb = billService.reloadBill(billId);
        if (tb == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        bill = tb;
        return "/pharmacy/pharmacy_reprint_transfer_issue_for_inpatient_requests?faces-redirect=true";
    }

    /**
     * Used by DTO reports where only the bill id is available.
     */
    public String navigateToReprintPharmacyTransferReceiveById() {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        Bill tb = billBean.fetchBillWithItemsAndFees(billId);
        if (tb == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        bill = tb;
        return "/pharmacy/pharmacy_reprint_transfer_receive?faces-redirect=true";
    }

    public String navigateToReprintPharmacyWholeSaleById() {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        Bill tb = billBean.fetchBillWithItemsAndFees(billId);
        if (tb == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        bill = tb;
        return "/pharmacy/pharmacy_reprint_bill?faces-redirect=true";
    }

    /**
     * Used by DTO reports where the bill id is passed as a parameter. This
     * avoids potential ViewExpiredException issues with
     * f:setPropertyActionListener.
     */
    public String navigateToReprintPharmacyTransferIssueByIdWithParam(Long paramBillId) {
        if (paramBillId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        Bill tb = billBean.fetchBillWithItemsAndFees(paramBillId);
        if (tb == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        bill = tb;
        return "/pharmacy/pharmacy_reprint_transfer_isssue?faces-redirect=true";
    }

    public String navigateToReprintPharmacyTransferReceive() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        return "/pharmacy/pharmacy_reprint_transfer_receive?faces-redirect=true";
    }

    public String navigateToReprintPharmacyTransferRequest() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        return "/pharmacy/pharmacy_reprint_transfer_request?faces-redirect=true";
    }

    /**
     * Used by DTO-based tables where only bill ID is available
     * Loads bill and navigates to return cash bill reprint page
     */
    public String navigateToBillByIdForReprint(Long paramBillId) {
        if (paramBillId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        Bill tb = billService.reloadBill(paramBillId);
        if (tb == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        bill = tb;
        return "/pharmacy/pharmacy_reprint_bill_return_cash?faces-redirect=true";
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Functions">
    public StreamedContent exportAsJson() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No bill is selected");
            return null;
        }

        String json = billService.convertBillToJson(bill);
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        return DefaultStreamedContent.builder()
                .name("bill_" + bill.getDeptId() + ".json")
                .contentType("application/json")
                .stream(() -> stream)
                .build();
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Inner Classes">
    // </editor-fold>
    //////////////////
    ///////////////////
    /**
     * Navigates back from the inward pharmacy request reprint page to
     * wherever the user came from (all-requests list, search results, or a
     * single patient's request list), rather than always returning to the
     * all-requests list (#22106 follow-up).
     */
    public String navigateBackFromInwardPharmacyRequestReprint() {
        String dest = returnPage != null ? returnPage : DEFAULT_INWARD_PHARMACY_REQUEST_REPRINT_RETURN_PAGE;
        returnPage = null;
        return dest;
    }

    public String editInwardPharmacyRequestBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Not Bill Found !");
            return "";
        }
        pharmacyRequestForBhtController.setBillPreview(false);
        pharmacyRequestForBhtController.setPatientEncounter(bill.getPatientEncounter());
        // `department` on PharmacyRequestForBhtController means the target pharmacy
        // (toDepartment), not the ward (fromDepartment) — see loadDraftForEditing().
        pharmacyRequestForBhtController.setDepartment(bill.getToDepartment());
        pharmacyRequestForBhtController.setPreBill((PreBill) bill);
        billFacade.edit(bill);
        return "/ward/ward_pharmacy_bht_issue_request_edit?faces-redirect=true";
    }

    public String cancelInwardPharmacyRequestBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Not Bill Found !");
            return "";
        }
        if (comment == null) {
            JsfUtil.addErrorMessage("Provide Comment To Cancel !");
            return "";
        }
        if (hasNonCancelledIssuingAgainstRequest(bill)) {
            JsfUtil.addErrorMessage("This request has already been issued from pharmacy and can no longer be cancelled.");
            return "";
        }
        CancelledBill cb = pharmacyCreateCancelBill();
        cb.setBillItems(getBill().getBillItems());
        bill.setCancelled(true);
        bill.setCancelledBill(cb);
        billFacade.edit(bill);
        // Invalidate the cached print DTO so a subsequent "View Request" for this
        // same bill id (this bean is session-scoped) re-reads the now-cancelled
        // status instead of serving the stale pre-cancellation snapshot.
        bhtIssueRequestPrintDtoBillId = null;
        return navigateBackFromInwardPharmacyRequestReprint();
    }

    public String markInwardPharmacyRequestComplete() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Not Bill Found !");
            return "";
        }
        if (!webUserController.hasPrivilege(Privileges.PharmacyBhtRequestForceComplete.toString())) {
            JsfUtil.addErrorMessage("You do not have privilege to force-complete this request.");
            return "";
        }
        if (bill.isCompleted()) {
            JsfUtil.addErrorMessage("This request is already marked as Completed.");
            return "";
        }
        if (bill.isCancelled()) {
            JsfUtil.addErrorMessage("Cannot complete a cancelled request.");
            return "";
        }
        bill.setCompleted(true);
        bill.setCompletedAt(new Date());
        bill.setCompletedBy(sessionController.getLoggedUser());
        billFacade.edit(bill);
        // Invalidate the cached print DTO so a subsequent "View Request" for this
        // same bill id (this bean is session-scoped) re-reads the now-completed
        // status instead of serving the stale pre-completion snapshot.
        bhtIssueRequestPrintDtoBillId = null;
        JsfUtil.addSuccessMessage("Request marked as Completed.");
        return "";
    }

    /**
     * Checks whether any non-cancelled, non-refunded {@code PharmacyBhtPre} bill
     * still references this BHT issue request - i.e. issuing has started and is
     * still in effect (#21516).
     */
    private boolean hasNonCancelledIssuingAgainstRequest(Bill requestBill) {
        String jpql = "SELECT COUNT(b) FROM Bill b WHERE b.retired = false "
                + "AND b.billType = :btp "
                + "AND b.referenceBill = :ref "
                + "AND b.cancelled = false "
                + "AND b.refunded = false";
        Map<String, Object> params = new HashMap<>();
        params.put("btp", BillType.PharmacyBhtPre);
        params.put("ref", requestBill);
        Long count = getBillFacade().findLongByJpql(jpql, params);
        return count != null && count > 0;
    }

    public String navigateToDirectPurchaseBillFromId(Long id) {
        if (id == null) {
            return "";
        }

        Bill bill = billService.reloadBill(id);

        if (bill == null) {
            return "";
        }

        this.bill = bill;

        if (bill.getBillType() == BillType.PurchaseReturn) {
            return "/pharmacy/pharmacy_reprint_purchase_return?faces-redirect=true";
        } else {
            return "/pharmacy/pharmacy_reprint_purchase?faces-redirect=true";
        }

    }

    public String cancelPharmacyTransferRequestBill() {
        if (comment == null || comment.isEmpty()) {
            JsfUtil.addErrorMessage("Please Provide a comment to cancel the bill");
            return "";
        }
        if (bill == null) {
            JsfUtil.addErrorMessage("Not Bill Found !");
            return "";
        }
        CancelledBill cb = pharmacyCreateCancelBill();
        cb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_TRANSFER_REQUEST_CANCELLED);
        cb.setBillItems(getBill().getBillItems());
        bill.setCancelled(true);
        bill.setCancelledBill(cb);
        billFacade.edit(bill);
        return "/pharmacy/pharmacy_transfer_request_list?faces-redirect=true";
    }

    public String navigateToCancelBhtRequest() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }

        return "/inward/bht_bill_cancel?faces-redirect=true";
    }

    public void markAsChecked() {
        if (bill == null) {
            return;
        }

        if (bill.getPatientEncounter() == null) {
            return;
        }

        if (bill.getPatientEncounter().isPaymentFinalized()) {
            return;
        }

        bill.setCheckeAt(new Date());
        bill.setCheckedBy(getSessionController().getLoggedUser());

        getBillFacade().edit(bill);

        JsfUtil.addSuccessMessage("Mark as Checked");

    }

    public void markAsUnChecked() {
        if (bill == null) {
            return;
        }

        if (bill.getPatientEncounter() == null) {
            return;
        }

        if (bill.getPatientEncounter().isPaymentFinalized()) {
            return;
        }

        bill.setCheckeAt(null);
        bill.setCheckedBy(null);

        getBillFacade().edit(bill);

        JsfUtil.addSuccessMessage("Mark As Un Check");

    }

    // <editor-fold defaultstate="collapsed" desc="Reprint BHT Issue - Print Format Settings (Issue #21880)">
    public boolean isReprintBhtIssuePosPaper() {
        return PaperType.PosPaper == getSessionController().getDepartmentPreference().getPharmacyBillPaperType();
    }

    public void setReprintBhtIssuePosPaper(boolean value) {
        if (value) {
            getSessionController().getDepartmentPreference().setPharmacyBillPaperType(PaperType.PosPaper);
        } else if (isReprintBhtIssuePosPaper()) {
            getSessionController().getDepartmentPreference().setPharmacyBillPaperType(null);
        }
    }

    public boolean isReprintBhtIssueFiveFivePaper() {
        return PaperType.FiveFivePaper == getSessionController().getDepartmentPreference().getPharmacyBillPaperType();
    }

    public void setReprintBhtIssueFiveFivePaper(boolean value) {
        if (value) {
            getSessionController().getDepartmentPreference().setPharmacyBillPaperType(PaperType.FiveFivePaper);
        } else if (isReprintBhtIssueFiveFivePaper()) {
            getSessionController().getDepartmentPreference().setPharmacyBillPaperType(null);
        }
    }

    public void saveReprintBhtIssuePaperConfig() {
        JsfUtil.addSuccessMessage("Print format settings saved");
    }
    // </editor-fold>

    public void unitCancell() {

        if (bill == null) {
            JsfUtil.addErrorMessage("Please Select a Bill");
            return;
        }

        if (checkIssueReturn(getBill())) {
            JsfUtil.addErrorMessage("Issue Bill had been Returned You can't cancell bill ");
            return;
        }

        if (getBill().getComments() == null || getBill().getComments().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please Enter Comments ");
            return;
        }

        if (checkDepartment(getBill())) {
            return;
        }

        Bill prebill = getPharmacyBean().reAddToStock(getBill(), getSessionController().getLoggedUser(),
                getSessionController().getDepartment(), BillNumberSuffix.ISSCAN);

        if (prebill != null) {
            getBill().setCancelled(true);
            getBill().setCancelledBill(prebill);
            getBill().setReferenceBill(prebill);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Canceled");
            printPreview = true;
        }
    }

    public void cancelBill() {
        if (getBill() == null) {
            JsfUtil.addErrorMessage("Please Select a Bill");
            return;
        }

        if (checkIssueReturn(getBill())) {
            JsfUtil.addErrorMessage("Issue Bill had been Returned. You can't cancel the bill.");
            return;
        }

        if (getComment() == null ||getComment().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please Enter Comments");
            return;
        }

        if (checkDepartment(getBill())) {
            return;
        }

//        Bill prebill = getPharmacyBean().reAddToStock(getBill(), getSessionController().getLoggedUser(),
//                getSessionController().getDepartment(), BillNumberSuffix.ISSCAN);
        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Canceled");
            return;
        }

        try {
            createCancellationBill();
            JsfUtil.addSuccessMessage("Canceled");
            printPreview = true;
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Failed to cancel the bill: " + e.getMessage());
            // Log the exception
        }
    }

    private void createCancellationBill() {
        CancelledBill c = createCancelledBillInstance(getBill());

        List<BillItem> billItems = savePreBillItems(c, sessionController.getLoggedUser(),
                sessionController.getLoggedUser().getDepartment());
        c.setBillItems(billItems);

        if (c.getStockBill() != null) {
            c.getStockBill().invertStockBillValues(getBill());
        } else {
            // Ensure getBill().getStockBill() is not null
            StockBill stockBill = getBill().getStockBill();
            if (stockBill != null) {
                c.setStockBill(stockBill);
                c.getStockBill().invertStockBillValues(getBill());
            } else {
                // Handle the case where there is no StockBill (either log or throw an exception)
            }
        }

        getBillFacade().edit(c);

        bill.setForwardReferenceBill(c);
        bill.setCancelled(true);
        bill.setCancelledBill(c);
        getBillFacade().edit(bill);
    }

    private CancelledBill createCancelledBillInstance(Bill b) {
        CancelledBill c = new CancelledBill();
        c.copy(b);
        c.copyValue(b);
        c.invertQty();
        c.setBilledBill(getBill());
        // Handle Department ID generation (independent)
        String deptId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Issue Cancelled - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getLoggedUser().getDepartment(), BillTypeAtomic.PHARMACY_ISSUE_CANCELLED);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Issue Cancelled - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                    sessionController.getLoggedUser().getDepartment(), BillTypeAtomic.PHARMACY_ISSUE_CANCELLED);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Issue Cancelled - Prefix + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getLoggedUser().getDepartment(), BillTypeAtomic.PHARMACY_ISSUE_CANCELLED);
        } else {
            // Use existing method for backward compatibility
            deptId = getBillNumberBean().institutionBillNumberGenerator(
                    sessionController.getLoggedUser().getDepartment(), getBill().getBillType(), BillClassType.PreBill, BillNumberSuffix.ISSCAN);
        }

        // Handle Institution ID generation (completely separate)
        String insId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Issue Cancelled - Prefix + Institution Code + Year + Yearly Number", false)) {
            insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getLoggedUser().getDepartment(), BillTypeAtomic.PHARMACY_ISSUE_CANCELLED);
        } else {
            // Smart fallback logic
            if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Issue Cancelled - Prefix + Department Code + Institution Code + Year + Yearly Number", false)
                    || configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Issue Cancelled - Prefix + Institution Code + Year + Yearly Number", false)) {
                insId = deptId; // Use same number as department
            } else {
                // Preserve old behavior: reuse deptId for insId to avoid consuming counter twice
                insId = deptId;
            }
        }

        c.setDeptId(deptId);
        c.setInsId(insId);
        c.setInstitution(sessionController.getLoggedUser().getInstitution());
        c.setDepartment(sessionController.getLoggedUser().getDepartment());
        c.setCreatedAt(new Date());
        c.setCreater(sessionController.getLoggedUser());
        c.setComments(comment);
        c.setBackwardReferenceBill(b);
        c.setReferenceBill(b);
        c.setBillClassType(BillClassType.CancelledBill);
        c.setBillType(BillType.PharmacyIssue);
        c.setBillTypeAtomic(BillTypeAtomic.PHARMACY_ISSUE_CANCELLED);

        if (c.getId() == null) {
            getBillFacade().create(c);
        }
        return c;
    }

    private List<BillItem> savePreBillItems(CancelledBill cancelBill, WebUser user, Department department) {
        List<BillItem> billItems = new ArrayList<>();

        if (getBill() == null) {
            return billItems;
        }

        if (getBill().getBillItems() == null) {
            return billItems;
        }

        for (BillItem bItem : getBill().getBillItems()) {
            try {
                if (bItem == null) {
                    continue;
                }

                BillItem newBillItem = new BillItem();
                newBillItem.copy(bItem);
                newBillItem.invertValue(bItem);
                newBillItem.setBill(cancelBill);
                newBillItem.setReferanceBillItem(bItem);
                newBillItem.setCreatedAt(new Date());
                newBillItem.setCreater(user);

                if (newBillItem.getId() == null) {
                    getBillItemFacade().create(newBillItem);
                }

                PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
                if (bItem.getPharmaceuticalBillItem() != null) {
                    ph.copy(bItem.getPharmaceuticalBillItem());
                    ph.invertValue(bItem.getPharmaceuticalBillItem());
                    ph.setBillItem(newBillItem);

                    if (ph.getId() == null) {
                        getPharmaceuticalBillItemFacade().create(ph);
                    }

                    newBillItem.setPharmaceuticalBillItem(ph);
                    getBillItemFacade().edit(newBillItem);

                    double qty = (bItem.getQty() != null) ? Math.abs(bItem.getQty()) : 0.0;
                    if (ph.getStock() != null) {
                        getPharmacyBean().addToStock(ph.getStock(), qty, ph, department);
                    } else {
                    }
                } else {
                }

                billItems.add(newBillItem);
            } catch (Exception e) {
                e.printStackTrace();
                throw e; // Re-throw to trigger transaction rollback
            }
        }

        return billItems;
    }

    public void unitIssueCancel() {

        if (bill == null) {
            JsfUtil.addErrorMessage("Please Select a Bill");
            return;
        }
        CancelledBill c = new CancelledBill();

        c.setBillDate(new Date());
        c.setBillTime(new Date());

        getBillFacade().create(c);
        c.setBillType(BillType.PharmacyIssue);
        c.setBilledBill(bill);
        c.setReferenceBill(bill);
        c.setCreater(getSessionController().getLoggedUser());
        c.setCreatedAt(new Date());

        c.setTotal(0.0 - bill.getTotal());
        c.setCashPaid(0.0 - bill.getCashPaid());
        c.setClaimableTotal(0.0 - bill.getClaimableTotal());
        c.setDiscount(0.0 - bill.getDiscount());
        c.setGrantTotal(0.0 - bill.getGrantTotal());
        c.setPaidAmount(0.0 - bill.getPaidAmount());
        c.setNetTotal(0.0 - bill.getNetTotal());
        c.setCancelled(true);

        List<BillItem> cancelBillItems = new ArrayList<>();
        List<PharmaceuticalBillItem> cancelPharmaciuticalBillItems = new ArrayList<>();

        for (BillItem bi : bill.getBillItems()) {
            BillItem canbi = new BillItem();

            canbi.setItem(bi.getItem());
            canbi.setItemId(bi.getItemId());

            canbi.setDiscount(0.0 - bi.getDiscount());
            canbi.setDiscountRate(0.0 - bi.getDiscountRate());
            canbi.setGrossValue(0.0 - bi.getGrossValue());
            canbi.setNetRate(0.0 - bi.getNetRate());
            canbi.setNetValue(0.0 - bi.getNetValue());
            canbi.setQty(0.0 - bi.getQty());
            canbi.setRate(0.0 - bi.getRate());
            canbi.setRemainingQty(0.0 - bi.getRemainingQty());
            canbi.setTmpQty(0.0 - bi.getTmpQty());
            canbi.setTotalGrnQty(0.0 - bi.getTotalGrnQty());
            canbi.setReferanceBillItem(bi);
            canbi.setBill(c);

            PharmaceuticalBillItem canphbi = new PharmaceuticalBillItem();
            canphbi.copy(bi.getPharmaceuticalBillItem());
            canphbi.invertValue(bi.getPharmaceuticalBillItem());
            canphbi.setBillItem(bi);
            if (canphbi.getId() == null) {
                getPharmaceuticalBillItemFacade().create(canphbi);
            }

            bi.setPharmaceuticalBillItem(canphbi);

            //System.err.println("QTY " + bi.getQty());
            double qty = 0;
            if (bi.getQty() != null) {
                qty = Math.abs(bi.getQty());
            }
            //Add to Stock
            getPharmacyBean().addToStock(bi.getPharmaceuticalBillItem(), qty, bill.getDepartment());
            getPharmacyBean().addToStock(canphbi, canphbi.getQty(), bill.getDepartment());

            getBillItemFacede().create(canbi);
            getPharmaceuticalBillItemFacade().edit(canphbi);

            cancelBillItems.add(canbi);
            cancelPharmaciuticalBillItems.add(canphbi);

        }

        c.setBillItems(cancelBillItems);
        getBillFacade().edit(bill);

        JsfUtil.addSuccessMessage("Canceled");
        printPreview = true;

    }

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    @EJB
    BillItemFacade billItemFacade;
    @Inject
    PriceMatrixController priceMatrixController;

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public void setPriceMatrixController(PriceMatrixController priceMatrixController) {
        this.priceMatrixController = priceMatrixController;
    }

    /**
     * The room category of the patient's current room, or null when the patient
     * is not in a room (or the room has no facility charge / category). Drives the
     * room-category dimension of the inward pharmacy-margin matrix (issue #21981);
     * null means "wildcard row only", preserving legacy behaviour.
     */
    private RoomCategory resolveCurrentRoomCategory(PatientEncounter encounter) {
        if (encounter == null
                || encounter.getCurrentPatientRoom() == null
                || encounter.getCurrentPatientRoom().getRoomFacilityCharge() == null) {
            return null;
        }
        return encounter.getCurrentPatientRoom().getRoomFacilityCharge().getRoomCategory();
    }

    public void updateMargin(List<BillItem> billItems, Bill bill, Department matrixDepartment, PaymentMethod paymentMethod) {
        double total = 0;
        double netTotal = 0;
        PatientEncounter encounter = bill != null ? bill.getPatientEncounter() : null;
        for (BillItem bi : billItems) {

            double rate = Math.abs(bi.getRate());
            double margin = 0;

            PriceMatrix priceMatrix = getPriceMatrixController().fetchInwardMargin(bi, rate, matrixDepartment, paymentMethod, null,
                    encounter != null ? encounter.getAdmissionType() : null, resolveCurrentRoomCategory(encounter));

            if (priceMatrix != null) {
                margin = ((bi.getGrossValue() * priceMatrix.getMargin()) / 100);
            }

            bi.setMarginValue(margin);

            bi.setNetValue(bi.getGrossValue() + bi.getMarginValue());
            bi.setAdjustedValue(bi.getNetValue());
            getBillItemFacade().edit(bi);

            total += bi.getGrossValue();
            netTotal += bi.getNetValue();
        }

        bill.setTotal(total);
        bill.setNetTotal(netTotal);
        getBillFacade().edit(bill);

    }

    public void updateFeeMargin() {

        updateMargin(getBill().getBillItems(), getBill(), getBill().getFromDepartment(), getBill().getPatientEncounter().getPaymentMethod());

    }

    public void editBill() {
        if (errorCheckForEdit()) {
            return;
        }
        getBillFacade().edit(getBill());
    }

    public void editBill(Bill bill) {

        getBillFacade().edit(bill);
    }

    private boolean errorCheckForEdit() {

        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
            return true;
        }

        if (getBill().getPaidAmount() != 0.0) {
            JsfUtil.addErrorMessage("Already Credit Company Paid For This Bill. Can not cancel.");
            return true;
        }

        return false;
    }

    private boolean errorCheckForEdit(Bill bill) {

        if (bill.isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (bill.isRefunded()) {
            JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
            return true;
        }

        if (bill.getPaidAmount() != 0.0) {
            JsfUtil.addErrorMessage("Already Credit Company Paid For This Bill. Can not cancel.");
            return true;
        }

        return false;
    }

    public void editBillItem(BillItem billItem) {

        if (errorCheckForEdit(billItem.getBill())) {
            return;
        }

        getBillItemFacede().edit(billItem);
        getPharmaceuticalBillItemFacade().edit(billItem.getPharmaceuticalBillItem());

        calTotalAndUpdate(billItem.getBill());
    }

    public void editBillItem2(BillItem billItem) {
        getBillItemFacede().edit(billItem);
        billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - billItem.getQty());
        getPharmaceuticalBillItemFacade().edit(billItem.getPharmaceuticalBillItem());

        calTotalAndUpdate2(billItem.getBill());
    }

    private void calTotalAndUpdate2(Bill bill) {
        double tmp = 0;
        for (BillItem b : bill.getBillItems()) {
            double tmp2 = (b.getPharmaceuticalBillItem().getQtyInUnit() * b.getPharmaceuticalBillItem().getItemBatch().getPurcahseRate());
            tmp += tmp2;
        }

        bill.setTotal(tmp);
        bill.setNetTotal(tmp);
        getBillFacade().edit(bill);
    }

    private void calTotalAndUpdate(Bill bill) {
        double tmp = 0;
        for (BillItem b : bill.getBillItems()) {
            if (b.getPharmaceuticalBillItem() == null) {
                continue;
            }
            double tmp2 = (b.getPharmaceuticalBillItem().getQtyInUnit() * b.getPharmaceuticalBillItem().getPurchaseRateInUnit());
            tmp += tmp2;
        }

        bill.setTotal(0 - tmp);
        bill.setNetTotal(0 - tmp);
        getBillFacade().edit(bill);
    }

    public void calTotalSaleRate(Bill bill) {
        double tmp = 0;
        for (BillItem b : bill.getBillItems()) {
            if (b.getPharmaceuticalBillItem() == null) {
                continue;
            }
            double tmp2 = (b.getPharmaceuticalBillItem().getQty() * b.getPharmaceuticalBillItem().getRetailRate());
            tmp += tmp2;
        }

        bill.setTransTotalSaleValue(tmp);

    }

    private boolean errorsPresentOnPoBillCancellation() {
        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
            return true;
        }

        return false;
    }

    /**
     * Authorization helper shared by the PO / transfer-issue / GRN
     * cancellation actions: checks the privilege and audits denied access.
     *
     * @param action The action being attempted (e.g. CANCEL, PHARMACY_GRN_CANCEL)
     * @param requiredPrivilege The specific privilege required
     * @return true if authorized, false if not
     */
    private boolean isAuthorized(String action, String requiredPrivilege) {
        if (webUserController == null || sessionController == null) {
            LOGGER.log(Level.SEVERE, "Authorization failed - missing controllers: action={0}, userId=null, billId={1}",
                    new Object[]{action, bill != null ? bill.getId() : "null"});
            return false;
        }

        if (!webUserController.hasPrivilege(requiredPrivilege)) {
            Long userId = sessionController.getLoggedUser() != null ? sessionController.getLoggedUser().getId() : null;
            Long billId = bill != null ? bill.getId() : null;

            LOGGER.log(Level.WARNING, "SECURITY: Unauthorized access attempt - action={0}, userId={1}, billId={2}, requiredPrivilege={3}",
                    new Object[]{action, userId, billId, requiredPrivilege});

            JsfUtil.addErrorMessage("You don't have permission to " + action.toLowerCase().replace('_', ' ') + ".");
            return false;
        }

        return true;
    }

    public String cancelPoBill() {
        if (!isAuthorized("CANCEL", "PharmacyOrderCancellation")) {
            return "";
        }
        if (getSelectedBills() == null || getSelectedBills().isEmpty()) {
            JsfUtil.addErrorMessage("Please select Bills");
            return "";
        }
        if (getComment() == null || getComment().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return "";
        }
        for (Bill selected : selectedBills) {
            setBill(selected);
            if (getBill() == null) {
                JsfUtil.addErrorMessage("No bill");
                continue;
            }
            if (getBill().getId() == null) {
                JsfUtil.addErrorMessage("No Saved bill");
                continue;
            }

            if (errorsPresentOnPoBillCancellation()) {
                continue;
            }

            CancelledBill cancellationBill = pharmacyCreateCancelBill();
            cancellationBill.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyPurchaseBill, BillClassType.CancelledBill, BillNumberSuffix.GRNCAN));
            cancellationBill.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyPurchaseBill, BillClassType.CancelledBill, BillNumberSuffix.GRNCAN));
            cancellationBill.setBillTypeAtomic(BillTypeAtomic.MULTIPLE_PHARMACY_ORDER_CANCELLED_BILL);

            if (cancellationBill.getId() == null) {
                getBillFacade().create(cancellationBill);
            }
            getBill().setCancelled(true);
            getBill().setCancelledBill(cancellationBill);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

        }
        selectedBills = null;
        return "";
    }

    public String navigateToViewPharmacyGrn() {
        if (isManageCostingEnabled()) {
            return "/pharmacy/pharmacy_reprint_grn_with_costing?faces-redirect=true";
        }
        return "/pharmacy/pharmacy_reprint_grn?faces-redirect=true";
    }

    public String navigateToEditSavedGrn() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        if (bill.getBillTypeAtomic() != BillTypeAtomic.PHARMACY_GRN_PRE) {
            JsfUtil.addErrorMessage("Selected bill is not a saved GRN (PRE).");
            return null;
        }
        grnController.setCurrentGrnBillPre(bill);
        return grnController.navigateToEditGrn();
    }

    /**
     * Id-based counterpart of navigateToEditSavedGrn(), for pages (e.g. the
     * DTO PO-receive list, issue #22612) that only carry a lightweight GRN
     * summary DTO rather than a preloaded Bill entity.
     */
    public String navigateToEditSavedGrnByBillId(Long billId) {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        bill = billService.reloadBill(billId);
        if (bill == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        return navigateToEditSavedGrn();
    }

    public String navigateToEditSavedDirectPurchase() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        if (bill.getBillTypeAtomic() != BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_PRE) {
            JsfUtil.addErrorMessage("Selected bill is not a saved Direct Purchase (PRE).");
            return null;
        }
        return pharmacyDirectPurchaseController.loadDraftForEditing(bill);
    }

    public String navigateToEditSavedGrnCosting() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        if (bill.getBillTypeAtomic() != BillTypeAtomic.PHARMACY_GRN_PRE) {
            JsfUtil.addErrorMessage("Selected bill is not a saved GRN (PRE).");
            return null;
        }
        bill = billService.reloadBill(bill);
        grnCostingController.setCurrentGrnBillPre(bill);
        return grnCostingController.navigateToEditGrnCosting();
    }

    /**
     * Id-based counterpart of navigateToEditSavedGrnCosting(), for pages
     * (e.g. the DTO PO-receive list, issue #22612) that only carry a
     * lightweight GRN summary DTO rather than a preloaded Bill entity.
     */
    public String navigateToEditSavedGrnCostingByBillId(Long billId) {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        bill = billService.reloadBill(billId);
        if (bill == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        return navigateToEditSavedGrnCosting();
    }

    /**
     * Native-SQL sibling of navigateToEditSavedGrnCosting() -- routes into
     * GrnCostingNativeSqlController/pharmacy_grn_costing_native.xhtml instead
     * of the legacy GrnCostingController/pharmacy_grn_costing_with_save_approve.xhtml.
     * The legacy pair above is left untouched so the "Legacy View" fallback
     * keeps working (issue #22874, mirrors the #22872 lesson on preserving
     * legacy reachability during a native-SQL conversion).
     */
    public String navigateToEditSavedGrnCostingNative() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        if (bill.getBillTypeAtomic() != BillTypeAtomic.PHARMACY_GRN_PRE) {
            JsfUtil.addErrorMessage("Selected bill is not a saved GRN (PRE).");
            return null;
        }
        bill = billService.reloadBill(bill);
        grnCostingNativeSqlController.setCurrentGrnBillPre(bill);
        return grnCostingNativeSqlController.navigateToEditGrnCosting();
    }

    /**
     * Id-based counterpart of navigateToEditSavedGrnCostingNative(), for
     * pages that only carry a lightweight GRN summary DTO rather than a
     * preloaded Bill entity (same rationale as navigateToEditSavedGrnCostingByBillId()).
     */
    public String navigateToEditSavedGrnCostingNativeByBillId(Long billId) {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        bill = billService.reloadBill(billId);
        if (bill == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        return navigateToEditSavedGrnCostingNative();
    }

//    public String navigateToApproveGrn() {
//        if (bill == null) {
//            JsfUtil.addErrorMessage("No Bill Selected");
//            return null;
//        }
//        if (bill.getBillTypeAtomic() != BillTypeAtomic.PHARMACY_GRN_PRE) {
//            JsfUtil.addErrorMessage("Invalid Bill Type");
//            return null;
//        }
//        grnController.setCurrentGrnBillPre(bill);
//        return grnController.navigateToApproveRecieveGrnPreBill();
//    }
//    public String navigateToViewCompletedGrn() {
//        if (bill == null) {
//            JsfUtil.addErrorMessage("No Bill Selected");
//            return null;
//        }
//        return navigateToViewPharmacyGrn();
//    }
    public String navigateToViewPurchaseOrder() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill");
            return "";
        }
        double tmp = 0;
        for (BillItem b : bill.getBillItems()) {
            if (b.getPharmaceuticalBillItem() == null) {
                continue;
            }
            double tmp2 = (b.getPharmaceuticalBillItem().getQty() * b.getPharmaceuticalBillItem().getRetailRate());
            tmp += tmp2;
        }
        bill.setTransTotalSaleValue(tmp);

        if (isManageCostingEnabled()) {
            return "/pharmacy/pharmacy_reprint_grn_with_costing?faces-redirect=true";
        }

        return "/pharmacy/pharmacy_reprint_grn?faces-redirect=true";
    }

    public String navigateToViewPharmacyBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill");
            return "";
        }
        double tmp = 0;
        for (BillItem b : bill.getBillItems()) {
            if (b.getPharmaceuticalBillItem() == null) {
                continue;
            }
            double tmp2 = (b.getPharmaceuticalBillItem().getQty() * b.getPharmaceuticalBillItem().getRetailRate());
            tmp += tmp2;
        }
        bill.setTransTotalSaleValue(tmp);
        return "/pharmacy/pharmacy_reprint_bill?faces-redirect=true";
    }

    public WebUser getUser() {
        return user;
    }

    public void onEdit(RowEditEvent event) {

        BillFee tmp = (BillFee) event.getObject();

        if (tmp.getPaidValue() != 0.0) {
            JsfUtil.addErrorMessage("Already Staff FeePaid");
            return;
        }

        getBillFeeFacade().edit(tmp);

    }

    public void setUser(WebUser user) {
        // recreateModel();
        this.user = user;
        recreateModel();
    }

    private LazyDataModel<Bill> lazyBills;

    public LazyDataModel<Bill> getSearchSaleBills() {
        return lazyBills;
    }

    public EjbApplication getEjbApplication() {
        return ejbApplication;
    }

    public void setEjbApplication(EjbApplication ejbApplication) {
        this.ejbApplication = ejbApplication;
    }

    public boolean calculateRefundTotal() {
        double d = 0.0;
        //billItems=null;
        tempbillItems = null;
        for (BillItem i : getRefundingItems()) {
            if (checkPaidIndividual(i)) {
                JsfUtil.addErrorMessage("Doctor Payment Already Paid So Cant Refund Bill");
                return false;
            }

            if (!i.isRefunded()) {
                d = d + i.getNetValue();
                getTempbillItems().add(i);
            }

        }
        refundAmount = d;
        return true;
    }

    public List<Bill> getUserBillsOwn() {
        List<Bill> userBills;
        if (getUser() == null) {
            userBills = new ArrayList<>();
        } else {
            userBills = getBillBean().billsFromSearchForUser(txtSearch, getFromDate(), getToDate(), getUser(), getSessionController().getInstitution(), BillType.OpdBill);
        }
        if (userBills == null) {
            userBills = new ArrayList<>();
        }
        return userBills;
    }

    public List<Bill> getBillsOwn() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().isEmpty()) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), getSessionController().getInstitution(), BillType.OpdBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), getSessionController().getInstitution(), BillType.OpdBill);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public StoreBillSearch getStoreBillSearch() {
        return storeBillSearch;
    }

    public void setStoreBillSearch(StoreBillSearch storeBillSearch) {
        this.storeBillSearch = storeBillSearch;
    }

    public List<BillItem> getRefundingItems() {
        return refundingItems;
    }

    public void setRefundingItems(List<BillItem> refundingItems) {
        this.refundingItems = refundingItems;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setBillFees(List<BillFee> billFees) {
        this.billFees = billFees;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public PaymentMethod getPaymentMethod() {
        if (paymentMethod == null) {
            paymentMethod = getBill().getPaymentMethod();
        }
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void recreateModel() {
        billForRefund = null;
        refundAmount = 0.0;
        billFees = null;
//        billFees
        lazyBills = null;
        billComponents = null;
        filteredBill = null;
        billItems = null;
        bills = null;
        printPreview = false;
        tempbillItems = null;
        //  comment = null;
    }

    private boolean checkPaid() {
        String sql = "SELECT bf FROM BillFee bf where bf.retired=false and bf.bill.id=" + getBill().getId();
        List<BillFee> tempFe = getBillFeeFacade().findByJpql(sql);

        for (BillFee f : tempFe) {
            if (f.getPaidValue() != 0.0) {
                return true;
            }

        }
        return false;
    }

    private boolean checkPaidIndividual(BillItem bi) {
        String sql = "SELECT bf FROM BillFee bf where bf.retired=false and bf.billItem.id=" + bi.getId();
        List<BillFee> tempFe = getBillFeeFacade().findByJpql(sql);

        for (BillFee f : tempFe) {
            if (f.getPaidValue() != 0.0) {
                return true;
            }

        }
        return false;
    }

    private boolean errorCheck() {
        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
            return true;
        }

        if (getBill().getPaidAmount() != 0.0) {
            JsfUtil.addErrorMessage("Already Credit Company Paid For This Bill. Can not cancel.");
            return true;
        }

        if (checkPaid()) {
            JsfUtil.addErrorMessage("Doctor Payment Already Paid So Cant Cancel Bill");
            return true;
        }

        if (checkCollectedReported()) {
            JsfUtil.addErrorMessage("Sample Already collected can't cancel or report already issued");
            return true;
        }

        if (getBill().getBillType() != BillType.LabBill && getBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a payment scheme.");
            return true;
        }
        if (getBill().getComments() == null || getBill().getComments().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return true;
        }

        return false;
    }

    private boolean pharmacyErrorCheck() {
        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (getBill().getBillType() == BillType.PharmacyOrderApprove) {
            if (checkGrn()) {
                JsfUtil.addErrorMessage("Grn already head been Come u can't bill ");
                return true;
            }
        }

        if (getBill().getBillType() == BillType.PharmacyGrnBill) {
            if (checkGrnReturn()) {
                JsfUtil.addErrorMessage("Grn had been Returned u can't cancell bill ");
                return true;
            }
        }
        if (getBill().getBillType() == BillType.PharmacyPre) {
            if (checkSaleReturn(getBill())) {
                JsfUtil.addErrorMessage("Sale had been Returned u can't cancell bill ");
                return true;
            }
        }

        if (getBill().getBillType() == BillType.PharmacySale) {
            if (checkSaleReturn(getBill())) {
                JsfUtil.addErrorMessage("Sale had been Returned u can't cancell bill ");
                return true;
            }
        }

        if (getBill().getBillType() == BillType.PharmacyTransferIssue) {
            if (getBill().checkActiveForwardReference()) {
                JsfUtil.addErrorMessage("Item for this bill already recieve");
                return true;
            }
            if (!getBill().getDepartment().equals(getSessionController().getLoggedUser().getDepartment())) {
                JsfUtil.addErrorMessage("You Can't Cancel This Transfer Using " + getSessionController().getLoggedUser().getDepartment().getName()
                        + " Department. Please Log " + getBill().getDepartment().getName() + " Deaprtment.");
                return true;
            }
        }

        if (getBill().getComments() == null || getBill().getComments().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return true;
        }
        return false;
    }

    private boolean checkGrn() {
        String sql = "Select b From BilledBill b where b.retired=false and b.creater is not null"
                + " and b.cancelled=false and b.billType=:btp and "
                + " b.referenceBill=:ref and b.referenceBill.cancelled=false ";
        HashMap hm = new HashMap();
        hm.put("ref", getBill());
        hm.put("btp", BillType.PharmacyOrder);
        List<Bill> tmp = getBillFacade().findByJpql(sql, hm);

        if (!tmp.isEmpty()) {
            return false;
        }

        return true;
    }

    private boolean checkGrnReturn() {
        String sql = "Select b From BilledBill b where b.retired=false and b.creater is not null"
                + " and b.cancelled=false and b.billType=:btp and "
                + " b.referenceBill=:ref and b.referenceBill.cancelled=false";
        HashMap hm = new HashMap();
        hm.put("ref", getBill());
        hm.put("btp", BillType.PharmacyGrnReturn);
        List<Bill> tmp = getBillFacade().findByJpql(sql, hm);

        if (!tmp.isEmpty()) {
            return true;
        }

        return false;
    }

    /**
     * Public method to check if the current bill has any GRN returns (completed or pending)
     * Used in XHTML to disable cancel button when return process exists
     * @return true if GRN has any returns or pending return processes
     */
    public boolean hasGrnReturnsOrPendingReturns() {
        if (getBill() == null || getBill().getBillType() != BillType.PharmacyGrnBill) {
            return false;
        }

        // Check for completed GRN returns
        if (checkGrnReturn()) {
            return true;
        }

        // Check for pending/unapproved GRN returns
        String jpql = "SELECT COUNT(b) FROM RefundBill b "
                + "WHERE b.billType = :bt "
                + "AND b.billTypeAtomic = :bta "
                + "AND b.referenceBill = :refBill "
                + "AND b.cancelled = false "
                + "AND b.retired = false "
                + "AND (b.checked IS NULL OR b.checked = false OR b.completed IS NULL OR b.completed = false)";

        Map<String, Object> params = new HashMap<>();
        params.put("bt", BillType.PharmacyGrnReturn);
        params.put("bta", BillTypeAtomic.PHARMACY_GRN_RETURN);
        params.put("refBill", getBill());

        Long count = getBillFacade().findLongByJpql(jpql, params);
        return count != null && count > 0;
    }

    /**
     * Public method to check if the current Sale-for-Cashier bill already has
     * an active (non-cancelled) item return against it.
     * Used in XHTML to disable the Cancel button once a return has been processed.
     * @return true if this Sale bill has been returned
     */
    public boolean hasActiveSaleReturn() {
        if (getBill() == null || getBill().getBillType() != BillType.PharmacySale) {
            return false;
        }
        return checkSaleReturn(getBill());
    }

    /**
     * Public method to check if the current item-return bill already has an
     * active (non-cancelled) refund payment processed against it. Queries
     * fresh instead of navigating Bill.returnCashBills, whose lazy collection
     * can be stale in the shared L2 cache when the refund bill was created in
     * a separate request. Used both to gate the Cancel button in XHTML and to
     * guard the server-side cancel action.
     *
     * Queries the base {@code Bill} type, not {@code RefundBill}: the refund
     * payment bill created by PharmacyRefundForItemReturnsController is
     * persisted as a plain {@code Bill} (DTYPE='Bill'), so a "From RefundBill"
     * query would silently miss it.
     * @return true if a refund payment has been processed for this bill
     */
    public boolean hasActiveReturnCashBill() {
        if (getBill() == null) {
            return false;
        }
        String sql = "Select count(b) From Bill b where b.retired=false "
                + " and b.cancelled=false "
                + " and b.billType=:bt "
                + " and b.referenceBill=:ref "
                + " and b.billedBill is null";
        HashMap hm = new HashMap();
        hm.put("ref", getBill());
        hm.put("bt", BillType.PharmacySale);
        Long count = getBillFacade().findLongByJpql(sql, hm);
        return count != null && count > 0;
    }

    private boolean checkSaleReturn(Bill b) {
        String sql = "Select b From RefundBill b where b.retired=false "
                + " and b.creater is not null"
                + " and b.cancelled=false "
                + " and b.billType=:btp "
                + " and b.billedBill=:ref "
                + " and b.referenceBill.cancelled=false";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", BillType.PharmacyPre);
        List<Bill> tmp = getBillFacade().findByJpql(sql, hm);

        if (!tmp.isEmpty()) {
            return true;
        }

        return false;
    }

    private boolean checkIssueReturn(Bill b) {
        String sql = "Select b From RefundBill b where b.retired=false "
                + " and b.creater is not null"
                + " and b.cancelled=false "
                + " and b.billType=:btp "
                + " and b.billedBill=:ref ";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", BillType.PharmacyIssue);
        List<Bill> tmp = getBillFacade().findByJpql(sql, hm);

        if (!tmp.isEmpty()) {
            return true;
        }

        return false;
    }

    private CancelledBill pharmacyCreateCancelBill() {
        CancelledBill cb = new CancelledBill();

        cb.setBilledBill(getBill());
        cb.copy(getBill());
        cb.setReferenceBill(getBill().getReferenceBill());
        cb.invertAndAssignValuesFromOtherBill(getBill());

        cb.setPaymentScheme(getBill().getPaymentScheme());
        //cb.setPaymentMethod(paymentMethod);
        cb.setBalance(0.0);
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());

        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());

        cb.setComments(getComment());
        cb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED);
        cb.setCompleted(true);

        return cb;
    }

    private CancelledBill createPharmacyRetailSaleCancellationBill(Bill originalBill) {
        CancelledBill cb = new CancelledBill();

        cb.setBilledBill(originalBill);
        cb.copy(originalBill);
        cb.setReferenceBill(originalBill.getReferenceBill());
        cb.invertAndAssignValuesFromOtherBill(originalBill);

        cb.setPaymentScheme(originalBill.getPaymentScheme());
        cb.setBalance(0.0);
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());

        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());

        cb.setComments(getComment());
        cb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED);

        return cb;
    }

    private void calculateCancelledDirectPurchaseFinancials(CancelledBill cancelledBill, Bill originalBill) {
        // Create BillFinanceDetails for cancellation (following DirectPurchaseReturnWorkflowController pattern)
        if (originalBill.getBillFinanceDetails() != null) {
            BillFinanceDetails cancelledBfd = new BillFinanceDetails();
            BillFinanceDetails originalBfd = originalBill.getBillFinanceDetails();

            // Clone and invert financial totals (money flows)
            if (originalBfd.getNetTotal() != null) {
                cancelledBfd.setNetTotal(originalBfd.getNetTotal().negate()); // Money refunded (positive)
            }
            if (originalBfd.getGrossTotal() != null) {
                cancelledBfd.setGrossTotal(originalBfd.getGrossTotal().negate());
            }

            // Invert stock valuations (following DirectPurchaseReturn pattern lines 1021-1023)
            if (originalBfd.getTotalPurchaseValue() != null) {
                cancelledBfd.setTotalPurchaseValue(originalBfd.getTotalPurchaseValue().abs().negate()); // Stock value removed (negative)
            }
            if (originalBfd.getTotalCostValue() != null) {
                cancelledBfd.setTotalCostValue(originalBfd.getTotalCostValue().abs().negate()); // Cost removed (negative)
            }
            if (originalBfd.getTotalRetailSaleValue() != null) {
                cancelledBfd.setTotalRetailSaleValue(originalBfd.getTotalRetailSaleValue().abs().negate()); // Retail value removed (negative)
            }

            // Set relationships
            cancelledBfd.setBill(cancelledBill);
            cancelledBfd.setCreatedAt(new Date());
            cancelledBfd.setCreatedBy(getSessionController().getLoggedUser());

            // Set finance details on bill (will be cascade saved when bill is saved)
            cancelledBill.setBillFinanceDetails(cancelledBfd);
        }
    }

    private void calculateCancelledBillItemFinancials(BillItem cancelledItem, BillItem originalItem) {
        // Create BillItemFinanceDetails for cancellation (following pharmacyCancelReceivedItems pattern)
        if (originalItem.getBillItemFinanceDetails() != null) {
            BillItemFinanceDetails cancelledBifd = new BillItemFinanceDetails();
            cancelledBifd.invertValue(originalItem.getBillItemFinanceDetails()); // Use existing invertValue method

            // Set relationships
            cancelledBifd.setBillItem(cancelledItem);
            cancelledBifd.setCreatedAt(new Date());

            // Set finance details on bill item (will be cascade saved when bill item is saved)
            cancelledItem.setBillItemFinanceDetails(cancelledBifd);
        }
    }

    private void calculateCancelledRetailSaleFinancials(CancelledBill cancelledBill, Bill originalBill) {
        // Use getId() to check for a persisted BillFinanceDetails.
        // Bill.getBillFinanceDetails() lazily creates a new instance, so a null
        // check on the getter would always be true and would mutate the original bill.
        if (originalBill.getBillFinanceDetails() != null && originalBill.getBillFinanceDetails().getId() != null) {
            BillFinanceDetails cancelledBfd = new BillFinanceDetails();
            cancelledBfd.invertValue(originalBill.getBillFinanceDetails());
            cancelledBfd.setBill(cancelledBill);
            cancelledBfd.setCreatedAt(new Date());
            cancelledBfd.setCreatedBy(getSessionController().getLoggedUser());
            cancelledBill.setBillFinanceDetails(cancelledBfd);
        }
    }

    private void calculateCancelledRetailSaleBillItemFinancials(BillItem cancelledItem, BillItem originalItem) {
        // Use getId() to check for a persisted BillItemFinanceDetails.
        // BillItem.getBillItemFinanceDetails() lazily creates a new instance, so a null
        // check on the getter would always be true and would mutate the original item.
        if (originalItem.getBillItemFinanceDetails() != null && originalItem.getBillItemFinanceDetails().getId() != null) {
            BillItemFinanceDetails cancelledBifd = new BillItemFinanceDetails();
            cancelledBifd.invertValue(originalItem.getBillItemFinanceDetails());
            cancelledBifd.setBillItem(cancelledItem);
            cancelledBifd.setCreatedAt(new Date());
            cancelledItem.setBillItemFinanceDetails(cancelledBifd);
        }
    }

    private RefundBill pharmacyCreateRefundCancelBill() {
        RefundBill cb = new RefundBill();
        cb.invertQty();
        cb.copy(getBill());
        cb.invertAndAssignValuesFromOtherBill(getBill());
        cb.setRefundedBill(getBill());
        cb.setReferenceBill(getBill().getReferenceBill());
        cb.setForwardReferenceBill(getBill().getForwardReferenceBill());

        cb.setPaymentMethod(paymentMethod);
        cb.setPaymentScheme(getBill().getPaymentScheme());
        cb.setBalance(0.0);
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());

        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());

        cb.setComments(getBill().getComments());
        cb.setPaymentMethod(getPaymentMethod());
        cb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION);

        return cb;
    }

    // Atomically claims the "cancel this return" slot with a single conditional UPDATE, so two
    // overlapping requests for the same bill (double-click, slow-AJAX retry, duplicate tab) can't
    // both pass an in-memory isCancelled() check and each create their own full stock reversal.
    // The old pattern set bill.cancelled=true only at the END of the method, after the reversal
    // was already created — a real request can take seconds (multiple bill-item + stock writes),
    // long enough for a second click to sail through the same stale check. Found via a July 2026
    // COGS variance investigation: a single return was cancelled 4 times in 15 seconds (bills
    // 5196838/5196837/5196867/5196866, all referencing bill 5196715), driving item batch 4609211
    // to a negative quantity. Returns true only if THIS call is the one that flips the flag.
    private boolean claimReturnCancellationOrReportError() {
        Map<String, Object> params = new HashMap<>();
        params.put("id", getBill().getId());
        int updated = getBillFacade().updateByJpql(
                "UPDATE Bill b SET b.cancelled = true WHERE b.id = :id AND b.cancelled = false",
                params);
        if (updated != 1) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return false;
        }
        getBill().setCancelled(true);
        return true;
    }

//    private void updateRemainingQty(PharmaceuticalBillItem nB) {
//        String sql = "Select p from PharmaceuticalBillItem p where p.billItem.id=" + nB.getBillItem().getReferanceBillItem().getId();
//        PharmaceuticalBillItem po = getPharmaceuticalBillItemFacade().findFirstByJpql(sql);
//        po.setRemainingQty(po.getRemainingQty() + nB.getQty());
//
//        //System.err.println("Added Remaini Qty " + nB.getQty());
//        //System.err.println("Final Remaini Qty " + po.getRemainingQty());
//        getPharmaceuticalBillItemFacade().edit(po);
//
//    }
    private void pharmacyCancelBillItemsAddStock(CancelledBill can) {
        for (BillItem nB : getBill().getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB);
            b.invertValue(nB);

            if (can.getBillType() == BillType.PharmacyGrnBill || can.getBillType() == BillType.PharmacyGrnReturn) {
                b.setReferanceBillItem(nB.getReferanceBillItem());
            } else {
                b.setReferanceBillItem(nB);
            }

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB.getPharmaceuticalBillItem());
            ph.invertValue(nB.getPharmaceuticalBillItem());

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }

            b.setPharmaceuticalBillItem(ph);

            if (b.getId() == null) {
                getBillItemFacede().create(b);
            }

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            //    updateRemainingQty(nB);
            //  b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            double qty = ph.getFreeQtyInUnit() + ph.getQtyInUnit();
            //System.err.println("Updating QTY " + qty);
            getPharmacyBean().addToStock(ph.getStock(),
                    Math.abs(qty),
                    ph, getSessionController().getDepartment());

            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

    private void pharmacyCancelBillItemsAddStock(CancelledBill can, Payment p) {
        for (BillItem nB : getBill().getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB);
            b.invertValue(nB);

            if (can.getBillType() == BillType.PharmacyGrnBill || can.getBillType() == BillType.PharmacyGrnReturn) {
                b.setReferanceBillItem(nB.getReferanceBillItem());
            } else {
                b.setReferanceBillItem(nB);
            }

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB.getPharmaceuticalBillItem());
            ph.invertValue(nB.getPharmaceuticalBillItem());

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }

            b.setPharmaceuticalBillItem(ph);

            if (b.getId() == null) {
                getBillItemFacede().create(b);
            }

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            //    updateRemainingQty(nB);
            //  b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            double qty = ph.getFreeQtyInUnit() + ph.getQtyInUnit();
            //System.err.println("Updating QTY " + qty);
            getPharmacyBean().addToStock(ph.getStock(),
                    Math.abs(qty),
                    ph, getSessionController().getDepartment());

            getBillItemFacede().edit(b);
            //get billfees from using cancel billItem
            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + nB.getId();
            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);
            cancelBillFee(can, b, tmp);

            //create BillFeePayments For cancel
            sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + b.getId();
            List<BillFee> tmpC = getBillFeeFacade().findByJpql(sql);
//            calculateBillfeePaymentsForCancelRefundBill(tmpC, p);
            //

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

    private void pharmacyCancelBillItemsReduceStock(CancelledBill can) {
        for (BillItem nB : getBill().getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB);
            b.invertValue(nB);

            if (can.getBillType() == BillType.PharmacyGrnBill || can.getBillType() == BillType.PharmacyGrnReturn) {
                b.setReferanceBillItem(nB.getReferanceBillItem());
            } else {
                b.setReferanceBillItem(nB);
            }

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB.getPharmaceuticalBillItem());
            ph.invertValue(nB.getPharmaceuticalBillItem());

            getPharmaceuticalBillItemFacade().create(ph);

            b.setPharmaceuticalBillItem(ph);

            if (b.getId() == null) {
                getBillItemFacede().create(b);
            }

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            //    updateRemainingQty(nB);
            //  b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            double qty = ph.getFreeQtyInUnit() + ph.getQtyInUnit();
            //System.err.println("Updating QTY " + qty);
            boolean returnFlag = getPharmacyBean().deductFromStock(ph.getStock(), Math.abs(qty), ph, getSessionController().getDepartment());

            if (!returnFlag) {
                b.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(b.getPharmaceuticalBillItem());
            }

            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

    private void pharmacyCancelBillItemsReduceStock(CancelledBill cancellationBill, Payment p) {
        for (BillItem originalBillItem : getBill().getBillItems()) {
            BillItem newlyCreatedBillItemForCancelBill = new BillItem();
            newlyCreatedBillItemForCancelBill.setBill(cancellationBill);
            newlyCreatedBillItemForCancelBill.copy(originalBillItem);
            newlyCreatedBillItemForCancelBill.invertValue(originalBillItem);
            calculateCancelledBillItemFinancials(newlyCreatedBillItemForCancelBill, originalBillItem);

            if (cancellationBill.getBillType() == BillType.PharmacyGrnBill || cancellationBill.getBillType() == BillType.PharmacyGrnReturn) {
                newlyCreatedBillItemForCancelBill.setReferanceBillItem(originalBillItem.getReferanceBillItem());
            } else {
                newlyCreatedBillItemForCancelBill.setReferanceBillItem(originalBillItem);
            }

            newlyCreatedBillItemForCancelBill.setCreatedAt(new Date());
            newlyCreatedBillItemForCancelBill.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = newlyCreatedBillItemForCancelBill.getPharmaceuticalBillItem();
            ph.copy(originalBillItem.getPharmaceuticalBillItem());
            ph.invertValue(originalBillItem.getPharmaceuticalBillItem());

            // ROBUSTNESS FIX for Issue #18041: Explicitly calculate stock values as NEGATIVE
            // Following the proven pattern from DirectPurchaseReturnWorkflowController (lines 2410-2412)
            double totalQtyInUnits = Math.abs(ph.getQty() + ph.getFreeQty());
            double purchaseRatePerUnit = ph.getPurchaseRate();
            double costRatePerUnit = ph.getCostRate();
            double retailRatePerUnit = ph.getRetailRate();

            ph.setPurchaseValue(-Math.abs(totalQtyInUnits * purchaseRatePerUnit));
            ph.setCostValue(-Math.abs(totalQtyInUnits * costRatePerUnit));
            ph.setRetailValue(-Math.abs(totalQtyInUnits * retailRatePerUnit));

//            getPharmaceuticalBillItemFacade().create(ph);
            newlyCreatedBillItemForCancelBill.setPharmaceuticalBillItem(ph);

            ph.setBillItem(newlyCreatedBillItemForCancelBill);
//            getPharmaceuticalBillItemFacade().edit(ph);

            if (newlyCreatedBillItemForCancelBill.getId() == null) {
                getBillItemFacade().create(newlyCreatedBillItemForCancelBill);
            } else {
                getBillItemFacade().edit(newlyCreatedBillItemForCancelBill);
            }

            //    updateRemainingQty(nB);
            //  b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            double qty = ph.getFreeQtyInUnit() + ph.getQtyInUnit();
            //System.err.println("Updating QTY " + qty);
            boolean returnFlag = getPharmacyBean().deductFromStock(ph.getStock(), Math.abs(qty), ph, getSessionController().getDepartment());

            if (!returnFlag) {
                newlyCreatedBillItemForCancelBill.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(newlyCreatedBillItemForCancelBill.getPharmaceuticalBillItem());
            }
            //get billfees from using cancel billItem
            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + originalBillItem.getId();
            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);
            cancelBillFee(cancellationBill, newlyCreatedBillItemForCancelBill, tmp);

            //create BillFeePayments For cancel
            sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + newlyCreatedBillItemForCancelBill.getId();
            List<BillFee> tmpC = getBillFeeFacade().findByJpql(sql);
//            calculateBillfeePaymentsForCancelRefundBill(tmpC, p);
            //
            getBillItemFacede().edit(newlyCreatedBillItemForCancelBill);

            cancellationBill.getBillItems().add(newlyCreatedBillItemForCancelBill);
        }

        getBillFacade().edit(cancellationBill);
    }

    private void pharmacyCancelIssuedItems(CancelledBill can) {
        for (BillItem nB : getBill().getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB);
            b.invertValue(nB);

            if (nB.getBillItemFinanceDetails() != null) {
                BillItemFinanceDetails invertedFinanceDetails = new BillItemFinanceDetails();
                invertedFinanceDetails.invertValue(nB.getBillItemFinanceDetails());
                invertedFinanceDetails.setBillItem(b);
                invertedFinanceDetails.setCreatedAt(new Date());
                b.setBillItemFinanceDetails(invertedFinanceDetails);
            }

            b.setReferanceBillItem(nB);

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB.getPharmaceuticalBillItem());
            ph.invertValue(nB.getPharmaceuticalBillItem());

            b.setPharmaceuticalBillItem(ph);

            if (b.getId() == null) {
                getBillItemFacede().create(b);
            } else {
                getBillItemFacede().edit(b);
            }

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            //    updateRemainingQty(nB);
            //  b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            double qty = ph.getFreeQtyInUnit() + ph.getQtyInUnit();
            //System.err.println("Updating QTY " + qty);
            boolean returnFlag = getPharmacyBean().deductFromStockWithoutHistory(ph.getStaffStock(), Math.abs(qty), ph, getSessionController().getDepartment());

            if (returnFlag) {
                getPharmacyBean().addToStock(ph.getStock(), Math.abs(qty), ph, getSessionController().getDepartment());
            } else {
                b.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(b.getPharmaceuticalBillItem());
            }

            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

    private void pharmacyCancelReceivedItems(CancelledBill can) {
        for (BillItem nB : getBill().getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB);
            b.invertValue(nB);

            // Handle BillItemFinanceDetails inversion
            if (nB.getBillItemFinanceDetails() != null) {
                BillItemFinanceDetails invertedFinanceDetails = new BillItemFinanceDetails();
                invertedFinanceDetails.invertValue(nB.getBillItemFinanceDetails());
                invertedFinanceDetails.setBillItem(b);
                invertedFinanceDetails.setCreatedAt(new Date());
                b.setBillItemFinanceDetails(invertedFinanceDetails);
            }

            b.setReferanceBillItem(nB);

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB.getPharmaceuticalBillItem());
            ph.invertValue(nB.getPharmaceuticalBillItem());

            b.setPharmaceuticalBillItem(ph);

            if (b.getId() == null) {
                getBillItemFacede().create(b);
            } else {
                getBillItemFacede().edit(b);
            }

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            //    updateRemainingQty(nB);
            //  b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            double qty = ph.getFreeQtyInUnit() + ph.getQtyInUnit();
            //System.err.println("Updating QTY " + qty);
            getPharmacyBean().addToStockWithoutHistory(ph.getStaffStock(), Math.abs(qty), ph, getSessionController().getDepartment());

            boolean returnFlag = getPharmacyBean().deductFromStock(ph.getStock(), Math.abs(qty), ph, getSessionController().getDepartment());

            if (!returnFlag) {
                b.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(b.getPharmaceuticalBillItem());
            }

            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

    private void pharmacyCancelBillItems(CancelledBill can) {
        for (BillItem nB : getBill().getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB);
            b.invertValue(nB);

            if (can.getBillType() == BillType.PharmacyGrnBill || can.getBillType() == BillType.PharmacyGrnReturn) {
                b.setReferanceBillItem(nB.getReferanceBillItem());
            } else {
                b.setReferanceBillItem(nB);
            }

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB.getPharmaceuticalBillItem());
            ph.invertValue(nB.getPharmaceuticalBillItem());

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }

            b.setPharmaceuticalBillItem(ph);

            if (b.getId() == null) {
                getBillItemFacede().edit(b);
            }

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

    private void pharmacyCancelBillItems(CancelledBill can, Payment p) {
        List<Payment> payments = new ArrayList<>();
        payments.add(p);
        pharmacyCancelBillItems(can, payments);
    }

    private void pharmacyCancelBillItems(CancelledBill newlyCreatedCancellingBill, List<Payment> ps) {
        for (BillItem originalBillItem : getBill().getBillItems()) {
            BillItem newlyCreatedReturningItem = new BillItem();
            newlyCreatedReturningItem.copy(originalBillItem);
            newlyCreatedReturningItem.setBill(newlyCreatedCancellingBill);
            newlyCreatedReturningItem.invertValue(originalBillItem);

            // Invert BillItemFinanceDetails for retail sale cancellation
            calculateCancelledRetailSaleBillItemFinancials(newlyCreatedReturningItem, originalBillItem);

            if (newlyCreatedCancellingBill.getBillType() == BillType.PharmacyGrnBill || newlyCreatedCancellingBill.getBillType() == BillType.PharmacyGrnReturn) {
                newlyCreatedReturningItem.setReferanceBillItem(originalBillItem.getReferanceBillItem());
            } else {
                newlyCreatedReturningItem.setReferanceBillItem(originalBillItem);
            }

            newlyCreatedReturningItem.setCreatedAt(new Date());
            newlyCreatedReturningItem.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem newlyCreatedReturningPharmaceuticalBillItem = new PharmaceuticalBillItem();
            newlyCreatedReturningPharmaceuticalBillItem.copy(originalBillItem.getPharmaceuticalBillItem());
            newlyCreatedReturningPharmaceuticalBillItem.invertValue(originalBillItem.getPharmaceuticalBillItem());

            // No need to create billItem and pharmaceuticalBillItem sepeately as there is a relationship with persistsAll
//            if (newlyCreatedReturningPharmaceuticalBillItem.getId() == null) {
//                getPharmaceuticalBillItemFacade().create(newlyCreatedReturningPharmaceuticalBillItem);
//            }
//
//            newlyCreatedReturningItem.setPharmaceuticalBillItem(newlyCreatedReturningPharmaceuticalBillItem);
//
//            if (newlyCreatedReturningItem.getId() == null) {
//                getBillItemFacede().create(newlyCreatedReturningItem);
//            }
            newlyCreatedReturningItem.setPharmaceuticalBillItem(newlyCreatedReturningPharmaceuticalBillItem);
            newlyCreatedReturningPharmaceuticalBillItem.setBillItem(newlyCreatedReturningItem);

            getBillItemFacede().create(newlyCreatedReturningItem);

            //get billfees from using cancel billItem  >> This feature of BillFee for Bill Items is NOT used in pharmacy related transactions
//            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + originalBillItem.getId();
//            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);
//            cancelBillFee(can, newlyCreatedReturningItem, tmp);
            //create BillFeePayments For cancel >> This feature of BillFee for Bill Items is NOT used in pharmacy related transactions
//            sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + newlyCreatedReturningItem.getId();
//            List<BillFee> tmpC = getBillFeeFacade().findByJpql(sql);
//            calculateBillfeePaymentsForCancelRefundBill(tmpC, p);
            //
            newlyCreatedCancellingBill.getBillItems().add(newlyCreatedReturningItem);

        }

        getBillFacade().edit(newlyCreatedCancellingBill);
    }

    private void cancelBillFee(Bill can, BillItem bt, List<BillFee> tmp) {
        for (BillFee nB : tmp) {
            BillFee bf = new BillFee();
            bf.setFee(nB.getFee());
            bf.setPatienEncounter(nB.getPatienEncounter());
            bf.setPatient(nB.getPatient());
            bf.setDepartment(nB.getDepartment());
            bf.setInstitution(nB.getInstitution());
            bf.setSpeciality(nB.getSpeciality());
            bf.setStaff(nB.getStaff());

            bf.setBill(can);
            bf.setBillItem(bt);
            bf.setFeeValue(0 - nB.getFeeValue());
            bf.setSettleValue(0 - nB.getSettleValue());

            bf.setCreatedAt(new Date());
            bf.setCreater(getSessionController().getLoggedUser());

            getBillFeeFacade().edit(bf);
        }
    }

//    public void calculateBillfeePaymentsForCancelRefundBill(List<BillFee> billFees, Payment p) {
//        for (BillFee bf : billFees) {
//            setBillFeePaymentAndPayment(bf, p);
//        }
//    }
//    public void setBillFeePaymentAndPayment(BillFee bf, Payment p) {
//        BillFeePayment bfp = new BillFeePayment();
//        bfp.setBillFee(bf);
//        bfp.setAmount(bf.getSettleValue());
//        bfp.setInstitution(p.getBill().getFromInstitution());
//        bfp.setDepartment(p.getBill().getFromDepartment());
//        if (bfp.getDepartment() == null) {
//            bfp.setDepartment(p.getDepartment());
//        }
//        bfp.setCreater(getSessionController().getLoggedUser());
//        bfp.setCreatedAt(new Date());
//        bfp.setPayment(p);
//        getBillFeePaymentFacade().create(bfp);
//    }
    private void pharmacyCancelReturnBillItems(Bill can) {
        for (PharmaceuticalBillItem nB : getPharmacyBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB.getBillItem());
            b.invertValue(nB.getBillItem());

            b.setReferanceBillItem(nB.getBillItem().getReferanceBillItem());
            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB);
            ph.invertValue(nB);

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }

            b.setPharmaceuticalBillItem(ph);

            if (b.getId() == null) {
                getBillItemFacede().create(b);
            }

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

//            //System.err.println("Updating QTY " + ph.getQtyInUnit());
//            getPharmacyBean().deductFromStock(ph.getStock(), Math.abs(ph.getQtyInUnit()), ph, getSessionController().getDepartment());
//
//            //    updateRemainingQty(nB);
            // if (b.getReferanceBillItem() != null) {
            //    b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            //  }
            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

    private void pharmacyCancelReturnBillItems(Bill can, Payment p) {
        for (PharmaceuticalBillItem nB : getPharmacyBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB.getBillItem());
            b.invertValue(nB.getBillItem());

            b.setReferanceBillItem(nB.getBillItem().getReferanceBillItem());
            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB);
            ph.invertValue(nB);

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }

            b.setPharmaceuticalBillItem(ph);

            if (b.getId() == null) {
                getBillItemFacede().edit(b);
            }

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            //get billfees from using cancel billItem
            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + nB.getBillItem().getId();
            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);
            cancelBillFee(can, b, tmp);

            //create BillFeePayments For cancel
            sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + b.getId();
            List<BillFee> tmpC = getBillFeeFacade().findByJpql(sql);
//            calculateBillfeePaymentsForCancelRefundBill(tmpC, p);
            //

//            //System.err.println("Updating QTY " + ph.getQtyInUnit());
//            getPharmacyBean().deductFromStock(ph.getStock(), Math.abs(ph.getQtyInUnit()), ph, getSessionController().getDepartment());
//
//            //    updateRemainingQty(nB);
            // if (b.getReferanceBillItem() != null) {
            //    b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            //  }
            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

    private void pharmacyCancelReturnBillItemsWithReducingStock(Bill can) {
        for (PharmaceuticalBillItem nB : getPharmacyBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB.getBillItem());
            b.invertValue(nB.getBillItem());
            b.setTransBillItem(nB.getBillItem());
            b.setReferanceBillItem(nB.getBillItem().getReferanceBillItem());
            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB);
            ph.invertValue(nB);

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }

            b.setPharmaceuticalBillItem(ph);

            if (b.getId() == null) {
                getBillItemFacede().edit(b);
            }

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            //System.err.println("Updating QTY " + ph.getQtyInUnit());
            boolean returnFlag = getPharmacyBean().deductFromStock(ph.getStock(), Math.abs(ph.getQtyInUnit()), ph, getSessionController().getDepartment());

            if (!returnFlag) {
                b.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(b.getPharmaceuticalBillItem());
            }

//
//            //    updateRemainingQty(nB);
            // if (b.getReferanceBillItem() != null) {
            //    b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            //  }
            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

//    private void pharmacyCancelReturnBillItems(Bill can) {
//        for (PharmaceuticalBillItem nB : getPharmacyBillItems()) {
//            BillItem b = new BillItem();
//            b.setBill(can);
//            b.copy(nB.getBillItem());
//            b.invertAndAssignValuesFromOtherBill(nB.getBillItem());
//
//            b.setReferanceBillItem(nB.getBillItem().getReferanceBillItem());
//            b.setCreatedAt(new Date());
//            b.setCreater(getSessionController().getLoggedUser());
//
//            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
//            ph.copy(nB);
//            ph.invertAndAssignValuesFromOtherBill(nB);
//
//            getPharmaceuticalBillItemFacade().create(ph);
//
//            b.setPharmaceuticalBillItem(ph);
//            getBillItemFacede().create(b);
//
//            ph.setBillItem(b);
//            getPharmaceuticalBillItemFacade().edit(ph);
//
//            getBillItemFacede().edit(b);
//
//            can.getBillItems().add(b);
//        }
//
//        getBillFacade().edit(can);
//    }
    @EJB
    private ItemBatchFacade itemBatchFacade;
    @EJB
    CashTransactionBean cashTransactionBean;

    public LazyDataModel<Bill> getLazyBills() {
        return lazyBills;
    }

    public void setLazyBills(LazyDataModel<Bill> lazyBills) {
        this.lazyBills = lazyBills;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public void pharmacyRetailCancelBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();

            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.SALCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.SALCAN));

            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }

            //for Payment,billFee and BillFeepayment
            Payment p = pharmacySaleController.createPayment(cb, paymentMethod);
            drawerController.updateDrawerForOuts(p);
            pharmacyCancelBillItems(cb, p);

            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());

            if (getBill().getReferenceBill() != null) {
                getBill().getReferenceBill().setCancelled(true);
                getBill().getReferenceBill().setReferenceBill(null);
                getBillFacade().edit(getBill().getReferenceBill());
            }

            WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(cb, getSessionController().getLoggedUser());
            getSessionController().setLoggedUser(wb);
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    private boolean checkDepartment(Bill bill) {
        if (bill == null) {
            JsfUtil.addErrorMessage("Bill Null");
            return true;

        }

        if (bill.getDepartment() == null) {
            JsfUtil.addErrorMessage("Bill Department Null");
            return true;
        }

        if (!Objects.equals(bill.getDepartment(), getSessionController().getDepartment())) {
            JsfUtil.addErrorMessage("Billed Department Is Defferent than Logged Department");
            return true;
        }

        return false;
    }

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    private boolean cancellationStarted;

    public void pharmacyRetailCancelBillWithStock() throws ParseException {
        if (cancellationStarted) {
            JsfUtil.addErrorMessage("Cancellation already started");
            return;
        }
        cancellationStarted = true;

        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                cancellationStarted = false;
                return;
            }

            if (getBill().getReferenceBill() == null) {
                cancellationStarted = false;
                return;
            }
            if (!webUserController.hasPrivilege("Admin")) {
                if (configOptionApplicationController.getBooleanValueByKey("Settled pharmacy bills can be cancelled only within settled day.", false)) {
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
                    Date createdDate = formatter.parse(formatter.format(getBill().getCreatedAt()));
                    Date today = formatter.parse(formatter.format(new Date()));
                    if (!createdDate.equals(today)) {
                        JsfUtil.addErrorMessage("Settled bills cancelled can be done only within settled day.");
                        cancellationStarted = false;
                        return;
                    }

                }
            }

            if (getBill().getReferenceBill().getBillType() != BillType.PharmacyPre && getBill().getReferenceBill().getBillType() != BillType.PharmacyWholesalePre) {
                cancellationStarted = false;
                return;
            }

            if (checkDepartment(getBill())) {
                cancellationStarted = false;
                return;
            }

            Bill newlyCreatedRetailSaleCancellationBillPre = getPharmacyBean().createPreBillForRetailSaleCancellation(getBill().getReferenceBill(), getSessionController().getLoggedUser(), getSessionController().getDepartment());

            CancelledBill newlyCreatedRetailSaleCancellationBill = createPharmacyRetailSaleCancellationBill(getBill());

            // Handle Department ID generation
            String deptId;
            if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cancel - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
                deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                        getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED);
            } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cancel - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
                deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                        getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED);
            } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cancel - Prefix + Institution Code + Year + Yearly Number", false)) {
                deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                        getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED);
            } else {
                // Use existing method for backward compatibility
                deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED);
            }

            // Handle Institution ID generation
            String insId;
            if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Cancel - Prefix + Institution Code + Year + Yearly Number", false)) {
                insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                        getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED);
            } else {
                // Use existing method for backward compatibility
                insId = deptId;
            }

            newlyCreatedRetailSaleCancellationBill.setDeptId(deptId);
            newlyCreatedRetailSaleCancellationBill.setInsId(insId);

            if (newlyCreatedRetailSaleCancellationBill.getId() == null) {
                getBillFacade().create(newlyCreatedRetailSaleCancellationBill);
            }

            //for Payment,billFee and BillFeepayment
            List<Payment> newlyCreatedCancellationPayments = pharmacySaleController.createPaymentForRetailSaleCancellation(newlyCreatedRetailSaleCancellationBill, paymentMethod);
            drawerController.updateDrawerForOuts(newlyCreatedCancellationPayments);
            pharmacyCancelBillItems(newlyCreatedRetailSaleCancellationBill, newlyCreatedCancellationPayments);

            // Calculate and set BillFinanceDetails for retail sale cancellation
            calculateCancelledRetailSaleFinancials(newlyCreatedRetailSaleCancellationBill, getBill());
            getBillFacade().edit(newlyCreatedRetailSaleCancellationBill);

            getBill().setCancelled(true);
            getBill().setCancelledBill(newlyCreatedRetailSaleCancellationBill);
            getBillFacade().edit(getBill());

            newlyCreatedRetailSaleCancellationBillPre.setReferenceBill(getBill());
            getBillFacade().edit(newlyCreatedRetailSaleCancellationBillPre);

            JsfUtil.addSuccessMessage("Cancelled");
            if (getBill().getPaymentMethod() == PaymentMethod.Credit) {
                if (getBill().getToStaff() != null) {
                    getStaffBean().updateStaffCredit(getBill().getToStaff(), 0 - getBill().getNetTotal());
                    JsfUtil.addSuccessMessage("Staff Credit Updated");
                    newlyCreatedRetailSaleCancellationBill.setFromStaff(getBill().getToStaff());
                    getBillFacade().edit(newlyCreatedRetailSaleCancellationBill);
                }
            }
            if (getBill().getPaymentMethod() == PaymentMethod.Staff_Welfare) {
                if (getBill().getToStaff() != null) {
                    getStaffBean().updateStaffWelfare(getBill().getToStaff(), 0 - getBill().getNetTotal());
                    JsfUtil.addSuccessMessage("Staff Welfare Updated");
                    newlyCreatedRetailSaleCancellationBill.setFromStaff(getBill().getToStaff());
                    getBillFacade().edit(newlyCreatedRetailSaleCancellationBill);
                }
            }

            printPreview = true;
            cancellationStarted = false;

        } else {
            cancellationStarted = false;
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyRetailCancelBillWithStock(Bill cbill) {
        setBill(cbill);
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (getBill().getBillType() != BillType.PharmacyPre && getBill().getBillType() != BillType.PharmacyWholesalePre) {
                return;
            }

//            if (calculateNumberOfBillsPerOrder(getBill().getReferenceBill())) {
//                return;
//            } before
            if (checkDepartment(getBill())) {
                return;
            }
            getPharmacyBean().reAddToStock(getBill(), getSessionController().getLoggedUser(), getSessionController().getDepartment(), BillNumberSuffix.PRECAN);
            getBill().setCancelled(true);
            getBill().setCancelledBill(null);
            getBillFacade().edit(getBill());

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void savePreComponent(Bill cbill) {
        setBill(cbill);
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (getBill().getBillType() != BillType.PharmacyPre && getBill().getBillType() != BillType.PharmacyWholesalePre) {
                return;
            }

            if (checkDepartment(getBill())) {
                return;
            }
            for (BillItem i : getBill().getBillItems()) {
                i.getPharmaceuticalBillItem().setQty(i.getQty());
                if (i.getPharmaceuticalBillItem().getQty() == 0.0) {
                    continue;
                }

                i.setCreatedAt(Calendar.getInstance().getTime());
                i.setCreater(getSessionController().getLoggedUser());
                //   i.getBillItem().setQty(i.getPharmaceuticalBillItem().getQty());
                double value = i.getNetRate() * i.getQty();
                i.setGrossValue(0 - value);
                i.setNetValue(0 - value);

                PharmaceuticalBillItem tmpPh = i.getPharmaceuticalBillItem();
                i.setPharmaceuticalBillItem(null);
                getBillItemFacade().create(i);

                tmpPh.setBillItem(i);
                getPharmaceuticalBillItemFacade().create(tmpPh);

                i.setPharmaceuticalBillItem(tmpPh);
                getBillItemFacade().edit(i);
                //   getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
                //   getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
                //   getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
                getPharmacyBean().addToStock(tmpPh.getStock(), Math.abs(tmpPh.getQtyInUnit()), tmpPh, getSessionController().getDepartment());

                //   i.getBillItem().getTmpReferenceBillItem().getPharmaceuticalBillItem().setRemainingQty(i.getRemainingQty() - i.getQty());
                //   getPharmaceuticalBillItemFacade().edit(i.getBillItem().getTmpReferenceBillItem().getPharmaceuticalBillItem());
                //      updateRemainingQty(i);
            }

        }

    }

    public void cancelPharmacyDirectIssueToBht() {
        if (getBill().getBillType() != BillType.PharmacyBhtPre) {
            return;
        }
        if (getBill().getPatientEncounter().isPaymentFinalized()) {
            JsfUtil.addErrorMessage("This Bill Already Discharged");
            return;
        }
        if (getBill().getCheckedBy() != null) {
            JsfUtil.addErrorMessage("Checked Bill. Can not cancel");
            return;
        }
        if (getBill().checkActiveReturnBhtIssueBills()) {
            JsfUtil.addErrorMessage("There some return Bill for this please cancel that bills first");
            return;
        }
        if (checkDepartment(getBill())) {
            return;
        }
        // Discharge medicine issues cancel to the discharge cancellation atomic; all other inward direct issues to the regular one.
        BillTypeAtomic cancellationAtomic = getBill().getBillTypeAtomic() == BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE
                ? BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_CANCELLATION
                : BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION;
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), cancellationAtomic);
        // Bills settled through InpatientDirectIssueNativeSqlService write their real netTotal
        // and BillItem rows via native SQL after this session-scoped bean's `bill` reference was
        // already populated from an earlier (now stale) query, so getBill() here can still show
        // netTotal=0 and an empty item list even though the database row is correct. Bypassing the
        // cache guarantees the reversal is built from the real billed amount (#22599).
        Bill billToCancel = getBillFacade().findWithoutCache(getBill().getId());
        Bill originalForwardReferenceBill = billToCancel.getForwardReferenceBill();
        Bill newlyCreatedCancellationBill = getPharmacyBean().reAddToStock(billToCancel, getSessionController().getLoggedUser(), getSessionController().getDepartment(), BillNumberSuffix.PHISSCAN);
        newlyCreatedCancellationBill.setForwardReferenceBill(originalForwardReferenceBill);
        newlyCreatedCancellationBill.setBillTypeAtomic(cancellationAtomic);
        newlyCreatedCancellationBill.setDeptId(deptId);
        newlyCreatedCancellationBill.setReferenceBill(billToCancel);
        getBillFacade().edit(newlyCreatedCancellationBill);
        // REMOVED: Finance details already correctly created by reAddToStock() method
        // Fixes #18144 - This redundant call was overwriting correct positive values with incorrect negative values
        // billService.createBillFinancialDetailsForPharmacyBill(newlyCreatedCancellationBill);

        // Merge through billToCancel (not the stale getBill()): its billItems collection is
        // empty on the stale reference, and Bill.billItems cascades ALL/orphanRemoval, so
        // merging the stale bill here would delete the original BillItem rows the reversal
        // above just linked to via referanceBillItem_ID.
        billToCancel.setCancelled(true);
        billToCancel.setCancelledBill(newlyCreatedCancellationBill);
        getBillFacade().edit(billToCancel);
        setBill(billToCancel);
        JsfUtil.addSuccessMessage("Cancelled");

        printPreview = true;

    }

    private Bill createReversalBillWithCorrectStockAdjustment(Bill originalBill, WebUser user, Department department, BillNumberSuffix billNumberSuffix) {
        // Create a new reversal bill using built-in copy method
        Bill reversalBill = new Bill();
        reversalBill.copy(originalBill);

        // Set reversal-specific properties
        reversalBill.setCreatedAt(new Date());
        reversalBill.setCreater(user);
        reversalBill.setDepartment(department);

        // Set reference relationships
        reversalBill.setReferenceBill(originalBill);
        reversalBill.setBilledBill(originalBill);

        // Initialize bill items collection
        reversalBill.setBillItems(new ArrayList<>());

        // Create reversal bill items using built-in methods
        for (BillItem originalItem : originalBill.getBillItems()) {
            // Create new bill item using copy method
            BillItem reversalItem = new BillItem();
            reversalItem.copy(originalItem);
            reversalItem.setBill(reversalBill);
            reversalItem.setCreatedAt(new Date());
            reversalItem.setCreater(user);

            // Use built-in invert method
            reversalItem.invertValue(originalItem);

            // Handle pharmaceutical bill item if it exists
            if (originalItem.getPharmaceuticalBillItem() != null) {
                PharmaceuticalBillItem originalPbi = originalItem.getPharmaceuticalBillItem();

                // Create new PBI using copy method
                PharmaceuticalBillItem reversalPbi = new PharmaceuticalBillItem();
                reversalPbi.copy(originalPbi);
                reversalPbi.setBillItem(reversalItem);
                reversalPbi.setCreatedAt(new Date());
                reversalPbi.setCreater(user);

                // Use built-in invert method
                reversalPbi.invertValue(originalPbi);

                reversalItem.setPharmaceuticalBillItem(reversalPbi);

                // Apply correct stock adjustment using the reversed quantity
                if (reversalPbi.getQty() != 0 && reversalPbi.getStock() != null) {
                    getPharmacyBean().addToStock(reversalPbi.getStock(), reversalPbi.getQty(), reversalPbi, department);
                }
            }

            // Add the bill item to the reversal bill
            reversalBill.getBillItems().add(reversalItem);
        }

        return reversalBill;
    }

    public void reversePharmacyDirectIssueToBht() {
        if (getBill() == null) {
            JsfUtil.addErrorMessage("No Bill Found");
            return;
        }
        if (getBill().getBillType() != BillType.PharmacyBhtPre) {
            JsfUtil.addErrorMessage("Invalid Bill Type for Reversal");
            return;
        }
        if (getBill().getPatientEncounter().isPaymentFinalized()) {
            JsfUtil.addErrorMessage("This Bill Already Discharged");
            return;
        }
        if (getBill().getCheckedBy() != null) {
            JsfUtil.addErrorMessage("Checked Bill. Can not reverse");
            return;
        }
        if (getBill().checkActiveReturnBhtIssueBills()) {
            JsfUtil.addErrorMessage("There are some return bills for this. Please cancel those bills first");
            return;
        }
        if (checkDepartment(getBill())) {
            return;
        }
        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Bill is already cancelled. Cannot reverse cancelled bills");
            return;
        }

        // Generate new bill number for reversal
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(
                sessionController.getDepartment(),
                getBill().getBillTypeAtomic() // Keep the same bill type atomic
        );

        // Create reversal bill using custom logic to handle proper stock adjustments
        Bill reversalBill = createReversalBillWithCorrectStockAdjustment(
                getBill(),
                getSessionController().getLoggedUser(),
                getSessionController().getDepartment(),
                BillNumberSuffix.PHISSCAN
        );

        // Set reversal bill properties - keep original bill type and bill type atomic
        reversalBill.setForwardReferenceBill(getBill().getForwardReferenceBill());
        reversalBill.setBillType(getBill().getBillType()); // Keep same bill type
        reversalBill.setBillTypeAtomic(getBill().getBillTypeAtomic()); // Keep same bill type atomic
        reversalBill.setDeptId(deptId);
        reversalBill.setReferenceBill(getBill()); // Reference to original bill
        reversalBill.setBilledBill(getBill()); // Reference as billedBill

        // Save the reversal bill
        getBillFacade().edit(reversalBill);
        billService.createBillFinancialDetailsForPharmacyBill(reversalBill);

        // Update original bill to mark as reversed
        getBill().setCancelled(true);
        getBill().setCancelledBill(reversalBill);
        getBillFacade().edit(getBill());

        JsfUtil.addSuccessMessage("Bill Reversed Successfully. Reversal Bill No: " + reversalBill.getDeptId());
        printPreview = true;
    }

    public void cancelPharmacyRequestIssueToBht() {
        if (getBill() == null) {
            JsfUtil.addErrorMessage("No Bill Found");
            return;
        }
        if (getBill().getBillType() != BillType.PharmacyBhtPre) {
            return;
        }
        if (getBill().getPatientEncounter().isPaymentFinalized()) {
            JsfUtil.addErrorMessage("This Bill Already Discharged");
            return;
        }
        if (getBill().getCheckedBy() != null) {
            JsfUtil.addErrorMessage("Checked Bill. Can not cancel");
            return;
        }
        if (getBill().checkActiveReturnBhtIssueBills()) {
            JsfUtil.addErrorMessage("There some return Bill for this please cancel that bills first");
            return;
        }
        if (checkDepartment(getBill())) {
            return;
        }
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION);
        Bill newlyCreatedCancellationBill = getPharmacyBean().reAddToStock(getBill(), getSessionController().getLoggedUser(), getSessionController().getDepartment(), BillNumberSuffix.PHISSCAN);
        newlyCreatedCancellationBill.setForwardReferenceBill(getBill().getForwardReferenceBill());
        newlyCreatedCancellationBill.setBillTypeAtomic(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION);
        newlyCreatedCancellationBill.setDeptId(deptId);
        newlyCreatedCancellationBill.setReferenceBill(getBill());
        getBillFacade().edit(newlyCreatedCancellationBill);
        billService.createBillFinancialDetailsForPharmacyBill(newlyCreatedCancellationBill);

        getBill().setCancelled(true);
        getBill().setCancelledBill(newlyCreatedCancellationBill);
        getBillFacade().edit(getBill());
        JsfUtil.addSuccessMessage("Cancelled");

        printPreview = true;

    }

    public void storeRetailCancelBillWithStockBht() {
        if (getBill().getBillType() != BillType.StoreBhtPre) {
            return;
        }

        cancelDirectIssueToBht(BillNumberSuffix.STTISSUECAN);
    }

    public void cancelPreBillFees(List<BillItem> list) {
        for (BillItem b : list) {
            List<BillFee> bfs = getBillFees(b.getTransBillItem());
            for (BillFee bf : bfs) {
                BillFee nBillFee = new BillFee();
                nBillFee.copy(bf);
                nBillFee.invertValue(bf);
                nBillFee.setBill(b.getBill());
                nBillFee.setBillItem(b);
                nBillFee.setCreatedAt(new Date());
                nBillFee.setCreater(getSessionController().getLoggedUser());

                if (nBillFee.getId() == null) {
                    getBillFeeFacade().create(nBillFee);
                }
            }
        }
    }

    private void cancelDirectIssueToBht(BillNumberSuffix billNumberSuffix) {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (getBill().getPatientEncounter().isPaymentFinalized()) {
                return;
            }

            if (getBill().checkActiveReturnBhtIssueBills()) {
                JsfUtil.addErrorMessage("There some return Bill for this please cancel that bills first");
                return;
            }

            if (checkDepartment(getBill())) {
                return;
            }

            Bill cb = getPharmacyBean().reAddToStock(getBill(), getSessionController().getLoggedUser(), getSessionController().getDepartment(), billNumberSuffix);
            cb.setForwardReferenceBill(getBill().getForwardReferenceBill());
            cb.setBillTypeAtomic(resolveDirectIssueCancellationAtomic(getBill().getBillTypeAtomic()));
            getBillFacade().edit(cb);

            //   cancelPreBillFees(cb.getBillItems());
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());

//            if (getBill().getReferenceBill() != null) {
//                getBill().getReferenceBill().setReferenceBill(null);
//                getBillFacade().edit(getBill().getReferenceBill());
//            }
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    /**
     * Resolves the cancellation atomic for a direct-issue-to-BHT cancellation
     * based on the source bill's atomic. cancelDirectIssueToBht() is shared by
     * pharmacy direct issues, discharge medicine issues and store direct issues,
     * so each must cancel to its own matching atomic rather than always to the
     * pharmacy medicine cancellation. Unknown sources fall back to the pharmacy
     * medicine cancellation to preserve prior behaviour.
     */
    private BillTypeAtomic resolveDirectIssueCancellationAtomic(BillTypeAtomic sourceAtomic) {
        if (sourceAtomic == null) {
            return BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION;
        }
        switch (sourceAtomic) {
            case DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE:
                return BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_CANCELLATION;
            case DIRECT_ISSUE_STORE_INWARD:
                return BillTypeAtomic.DIRECT_ISSUE_STORE_INWARD_CANCELLATION;
            default:
                return BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION;
        }
    }

    public void pharmacyReturnPreCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (hasActiveReturnCashBill()) {
                JsfUtil.addErrorMessage("Payment for this bill Already Paid");
                return;
            }
            if (!claimReturnCancellationOrReportError()) {
                return;
            }

            RefundBill cb = pharmacyCreateRefundCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.RefundBill, BillNumberSuffix.RETCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.RefundBill, BillNumberSuffix.RETCAN));

            if (cb.getId() == null) {
                getBillFacade().edit(cb);
            }

            pharmacyCancelReturnBillItemsWithReducingStock(cb);

//            List<PharmaceuticalBillItem> tmp = getPharmaceuticalBillItemFacade().findByJpql("Select p from PharmaceuticalBillItem p where p.billItem.bill.id=" + getBill().getId());
//
//            for (PharmaceuticalBillItem ph : tmp) {
//                getPharmacyBean().deductFromStock(ph.getItemBatch(), ph.getQtyInUnit() + ph.getFreeQtyInUnit(), getBill().getDepartment());
//
//                getPharmacyBean().reSetPurchaseRate(ph.getItemBatch(), getBill().getDepartment());
//                getPharmacyBean().reSetRetailRate(ph.getItemBatch(), getSessionController().getDepartment());
//            }
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyReturnBhtCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {

            if (checkDepartment(getBill())) {
                return;
            }

            if (getBill().getCheckedBy() != null) {
                JsfUtil.addErrorMessage("Checked Bill. Can not cancel");
                return;
            }

            if (getBill().getPatientEncounter().isPaymentFinalized()) {
                JsfUtil.addErrorMessage("This BHT Already Discharge..");
                return;
            }
            if (!claimReturnCancellationOrReportError()) {
                return;
            }

            RefundBill cb = pharmacyCreateRefundCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.RefundBill, BillNumberSuffix.RETCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.RefundBill, BillNumberSuffix.RETCAN));

            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }

            pharmacyCancelReturnBillItemsWithReducingStock(cb);

            // cancelPreBillFees(cb.getBillItems());
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyReturnCashCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }
            if (!claimReturnCancellationOrReportError()) {
                return;
            }

            RefundBill cb = pharmacyCreateRefundCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.RefundBill, BillNumberSuffix.RETCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.RefundBill, BillNumberSuffix.RETCAN));

            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }
            //for Payment,billFee and BillFeepayment
            Payment p = pharmacySaleController.createPayment(cb, paymentMethod);
            pharmacyCancelReturnBillItems(cb, p);

            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());

            getCashTransactionBean().saveBillCashInTransaction(cb, getSessionController().getLoggedUser());

            JsfUtil.addSuccessMessage("Cancelled");
            if (getBill().getPaymentMethod() == PaymentMethod.Credit) {
                if (getBill().getToStaff() != null) {
                    getStaffBean().updateStaffCredit(getBill().getToStaff(), 0 - getBill().getNetTotal());
                    JsfUtil.addSuccessMessage("Staff Credit Updated");
                    cb.setFromStaff(getBill().getToStaff());
                    getBillFacade().edit(cb);
                }
            }

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyPoCancel() {
        if (!isAuthorized("CANCEL", "PharmacyOrderCancellation")) {
            return;
        }
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.POCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.POCAN));
            cb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_ORDER_CANCELLED);
            
            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }
            pharmacyCancelBillItems(cb);

            getBill().getReferenceBill().setReferenceBill(null);
            getBillFacade().edit(getBill().getReferenceBill());

            getBill().setReferenceBill(null);

            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            //       //System.err.println("Bill : "+getBill().getBillType());
//            //System.err.println("Reference Bill : "+getBill().getReferenceBill().getBillType());
            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyPoRequestCancel() {
        if (!isAuthorized("CANCEL", "PharmacyOrderCancellation")) {
            return;
        }
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (getBill().getReferenceBill() != null && !getBill().getReferenceBill().isCancelled()) {
                JsfUtil.addErrorMessage("Sorry You cant Cancell Approved Bill");
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();
            cb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_ORDER_CANCELLED);

            // Check if bill number suffix is configured, if not set default "C-POR" for Purchase Order Request Cancellations
            String billSuffix = configOptionApplicationController.getLongTextValueByKey("Bill Number Suffix for " + BillTypeAtomic.PHARMACY_ORDER_CANCELLED, "");
            if (billSuffix == null || billSuffix.trim().isEmpty()) {
                // Set default suffix for Purchase Order Request Cancellations if not configured
                configOptionApplicationController.setLongTextValueByKey("Bill Number Suffix for " + BillTypeAtomic.PHARMACY_ORDER_CANCELLED, "C-POR");
            }

            boolean billNumberGenerationStrategyForDepartmentIdIsPrefixDeptInsYearCount = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Purchase Order Request Cancellations - Prefix + Institution Code + Department Code + Year + Yearly Number and Yearly Number", false);
            boolean billNumberGenerationStrategyForDepartmentIdIsPrefixInsYearCount = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Purchase Order Request Cancellations - Prefix + Institution Code + Year + Yearly Number and Yearly Number", false);
            boolean billNumberGenerationStrategyForInstitutionIdIsPrefixInsYearCount = configOptionApplicationController.getBooleanValueByKey("Institution Number Generation Strategy for Purchase Order Request Cancellations - Prefix + Institution Code + Year + Yearly Number and Yearly Number", false);

            // Handle Department ID generation
            String deptId;
            if (billNumberGenerationStrategyForDepartmentIdIsPrefixDeptInsYearCount) {
                deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_ORDER_CANCELLED);
            } else if (billNumberGenerationStrategyForDepartmentIdIsPrefixInsYearCount) {
                deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                        getSessionController().getDepartment(),
                        BillTypeAtomic.PHARMACY_ORDER_CANCELLED
                );
            } else {
                // Default behavior - use the original method
                deptId = getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PORCAN);
            }

            // Handle Institution ID generation separately
            String insId;
            if (billNumberGenerationStrategyForInstitutionIdIsPrefixInsYearCount) {
                insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                        getSessionController().getDepartment(),
                        BillTypeAtomic.PHARMACY_ORDER_CANCELLED
                );
            } else {
                // Default behavior - use the department ID for institution ID or original method
                if (billNumberGenerationStrategyForDepartmentIdIsPrefixDeptInsYearCount || billNumberGenerationStrategyForDepartmentIdIsPrefixInsYearCount) {
                    insId = deptId;
                } else {
                    // Preserve old behavior: reuse deptId for insId to avoid consuming counter twice
                    insId = deptId;
                }
            }

            cb.setDeptId(deptId);
            cb.setInsId(insId);

            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }
            pharmacyCancelBillItems(cb);

            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            //       //System.err.println("Bill : "+getBill().getBillType());
//            //System.err.println("Reference Bill : "+getBill().getReferenceBill().getBillType());
            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    private boolean checkStock(PharmaceuticalBillItem pharmaceuticalBillItem) {
        //System.err.println("Batch " + pharmaceuticalBillItem.getItemBatch());
        double stockQty = getPharmacyBean().getBatchStockQty(pharmaceuticalBillItem.getItemBatch(), getBill().getDepartment());
        if (Math.abs(pharmaceuticalBillItem.getQtyInUnit() + pharmaceuticalBillItem.getFreeQtyInUnit()) > stockQty) {
            return true;
        } else {
            return false;
        }
    }

    private boolean checkBillItemStock() {
        //System.err.println("Checking Item Stock");
        for (BillItem bi : getBill().getBillItems()) {
            if (checkStock(bi.getPharmaceuticalBillItem())) {
                return true;
            }
        }

        return false;
    }

    public void pharmacyGrnCancel() {
        if (!isAuthorized("PHARMACY_GRN_CANCEL", "PharmacyGrnCancel")) {
            return;
        }
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (checkDepartment(getBill())) {
                return;
            }

            if (checkBillItemStock()) {
                JsfUtil.addErrorMessage("Items for this GRN Already issued so you can't cancel ");
                return;
            }

            if (getBill().getPaidAmount() != 0) {
                JsfUtil.addErrorMessage("Payments for this GRN Already Given ");
                return;
            }

            // Set default suffix for cancelled GRN if not already set
            String billNumberSuffix = configOptionApplicationController.getShortTextValueByKey("Bill Number Suffix for Cancelled GRN", "C-GRN");
            if (billNumberSuffix == null || billNumberSuffix.trim().isEmpty()) {
                billNumberSuffix = "C-GRN";
            }

            CancelledBill cb = pharmacyCreateCancelBill();

            // Get configuration options for bill numbering strategies
            boolean billNumberGenerationStrategyForDepartmentIdIsPrefixDeptInsYearCount = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department ID is Prefix Dept Ins Year Count", false);
            boolean billNumberGenerationStrategyForDepartmentIdIsPrefixInsYearCount = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department ID is Prefix Ins Year Count", false);
            boolean billNumberGenerationStrategyForInstitutionIdIsPrefixInsYearCount = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Institution ID is Prefix Ins Year Count", false);

            String billId;

            // Independent department ID generation
            if (billNumberGenerationStrategyForDepartmentIdIsPrefixDeptInsYearCount) {
                billId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_GRN_CANCELLED);
                cb.setDeptId(billId);
            } else if (billNumberGenerationStrategyForDepartmentIdIsPrefixInsYearCount) {
                billId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_GRN_CANCELLED);
                cb.setDeptId(billId);
            } else {
                cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.GRNCAN));
            }

            // Independent institution ID generation
            if (billNumberGenerationStrategyForInstitutionIdIsPrefixInsYearCount) {
                billId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_GRN_CANCELLED);
                cb.setInsId(billId);
            } else {
                cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.GRNCAN));
            }

            cb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN_CANCELLED);
            cb.setReferenceBill(getBill().getBilledBill());

            if (cb.getId() == null) {
                getBillFacade().create(cb);
            } else {
                getBillFacade().edit(cb);
            }

            //to create payments for cancel bill
            Payment p = pharmacySaleController.createPayment(cb, paymentMethod);

//            pharmacyCancelBillItemsReduceStock(cb); //for create billfees ,billfee payments
            pharmacyCancelBillItemsReduceStock(cb, p);
            calculateCancelledDirectPurchaseFinancials(cb, getBill());
            getBillFacade().edit(cb);
//
//            List<PharmaceuticalBillItem> tmp = getPharmaceuticalBillItemFacade().findByJpql("Select p from PharmaceuticalBillItem p where p.billItem.bill.id=" + getBill().getId());
//
//            for (PharmaceuticalBillItem ph : tmp) {
//                double qty = ph.getQtyInUnit() + ph.getFreeQtyInUnit();
//                getPharmacyBean().deductFromStock(ph.getStock(), qty);
//
//                getPharmacyBean().reSetPurchaseRate(ph.getItemBatch(), getBill().getDepartment());
//                getPharmacyBean().reSetRetailRate(ph.getItemBatch(), getSessionController().getDepartment());
//            }

            getBill().setCancelled(true);
            getBill().setCompleted(true);
            getBill().setCancelledBill(cb);
            pharmacyCalculation.calculateRetailSaleValueAndFreeValueAtPurchaseRate(getBill());
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyTransferIssueCancel() {
        if (!isAuthorized("CANCEL", "PharmacyTransferIssueCancel")) {
            return;
        }
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (checkDepartment(getBill())) {
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PHTICAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PHTICAN));
            cb.setBackwardReferenceBill(getBill().getBackwardReferenceBill());
            cb.setReferenceBill(getBill());
            cb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_ISSUE_CANCELLED);
            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }

            pharmacyCancelIssuedItems(cb);

            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());

//            getBill().getBackwardReferenceBill().setForwardReferenceBill(null);
//            getBillFacade().edit(getBill().getBackwardReferenceBill());
//
//            getBill().setBackwardReferenceBill(null);
//            getBill().setForwardReferenceBill(null);
//            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyTransferIssueInpatientCancel() {
        if (!isAuthorized("CANCEL", "PharmacyTransferIssueCancel")) {
            return;
        }
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (checkDepartment(getBill())) {
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PHTICAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PHTICAN));
            cb.setBackwardReferenceBill(getBill().getBackwardReferenceBill());
            cb.setReferenceBill(getBill());
            cb.setBillTypeAtomic(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION);
            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }

            pharmacyCancelIssuedItems(cb);

            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());

            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyTransferReceiveCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (checkDepartment(getBill())) {
                return;
            }

            if (checkBillItemStock()) {
                JsfUtil.addErrorMessage("Items for this Note Already issued so you can't cancel ");
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PHTRCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PHTRCAN));
            cb.setBackwardReferenceBill(getBill().getBackwardReferenceBill());
            cb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RECEIVE_CANCELLED);
            cb.setPaymentMethod(PaymentMethod.None);
            cb.setReferenceBill(getBill());
            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }

            pharmacyCancelReceivedItems(cb);

            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());

            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyPurchaseCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (checkDepartment(getBill())) {
                return;
            }

            if (checkBillItemStock()) {
                JsfUtil.addErrorMessage("ITems for this GRN Already issued so you can't cancel ");
                return;
            }

            if (getBill().getPaidAmount() != 0) {
                JsfUtil.addErrorMessage("Payments for this GRN Already Given ");
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();
            cb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_CANCELLED);
            
            cb.setInvoiceDate(getBill().getInvoiceDate());
            cb.setInvoiceNumber(getBill().getInvoiceNumber());
            cb.setFromInstitution(getBill().getFromInstitution());
            
            // Handle Department ID generation (independent)
            String deptId;
            if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Dept Ins Year Count", false)) {
                deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                        getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_CANCELLED);
            } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Ins Year Count", false)) {
                deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                        getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_CANCELLED);
            } else {
                deptId = getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PURCAN);
            }

            // Handle Institution ID generation (completely separate)
            String insId;
            if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Institution Id is Prefix Ins Year Count", false)) {
                insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                        getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_CANCELLED);
            } else {
                if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Dept Ins Year Count", false)
                        || configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Ins Year Count", false)) {
                    insId = deptId;
                } else {
                    // Preserve old behavior: reuse deptId for insId to avoid consuming counter twice
                    insId = deptId;
                }
            }

            cb.setDeptId(deptId);
            cb.setInsId(insId);
            cb.setReferenceBill(getBill());

            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }

            Payment p = pharmacySaleController.createPayment(cb, getBill().getPaymentMethod());

            pharmacyCancelBillItemsReduceStock(cb, p);
            calculateCancelledDirectPurchaseFinancials(cb, getBill());
            getBillFacade().edit(cb);  // CRITICAL FIX for Issue #18041: Save CancelledBill to persist BFD with negative stock valuations

//            //   List<PharmaceuticalBillItem> tmp = getPharmaceuticalBillItemFacade().findByJpql("Select p from PharmaceuticalBillItem p where p.billItem.bill.id=" + getBill().getId());
//            for (BillItem bi : getBill().getBillItems()) {
//                double qty = bi.getPharmaceuticalBillItem().getQtyInUnit() + bi.getPharmaceuticalBillItem().getFreeQtyInUnit();
//                getPharmacyBean().deductFromStock(bi.getPharmaceuticalBillItem().getStock(), qty);
//
//                getPharmacyBean().reSetPurchaseRate(bi.getPharmaceuticalBillItem().getItemBatch(), getBill().getDepartment());
//                getPharmacyBean().reSetRetailRate(bi.getPharmaceuticalBillItem().getItemBatch(), getSessionController().getDepartment());
//            }
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);

            pharmacyCalculation.calculateRetailSaleValueAndFreeValueAtPurchaseRate(getBill());
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            WebUser wb = getCashTransactionBean().saveBillCashInTransaction(cb, getSessionController().getLoggedUser());
            getSessionController().setLoggedUser(wb);

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyGrnReturnCancel() {
        if (!isAuthorized("PHARMACY_GRN_RETURN_CANCEL", "PharmacyGrnReturnCancel")) {
            return;
        }
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (checkDepartment(getBill())) {
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.GRNRETCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.GRNRETCAN));

            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }
            //create Billfee Payments
            Payment p = pharmacySaleController.createPayment(cb, getBill().getPaymentMethod());
            pharmacyCancelBillItemsAddStock(cb, p);

            //        List<PharmaceuticalBillItem> tmp = getPharmaceuticalBillItemFacade().findByJpql("Select p from PharmaceuticalBillItem p where p.billItem.bill.id=" + getBill().getId());
//            for (PharmaceuticalBillItem ph : tmp) {
//                getPharmacyBean().addToStock(ph.getStock(), ph.getQtyInUnit());
//
//                getPharmacyBean().reSetPurchaseRate(ph.getItemBatch(), getBill().getDepartment());
//                getPharmacyBean().reSetRetailRate(ph.getItemBatch(), getSessionController().getDepartment());
//            }
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    private void returnBillFee(Bill b, BillItem bt, List<BillFee> tmp) {
        for (BillFee nB : tmp) {
            BillFee bf = new BillFee();
            bf.setFee(nB.getFee());
            bf.setPatienEncounter(nB.getPatienEncounter());
            bf.setPatient(nB.getPatient());
            bf.setDepartment(nB.getDepartment());
            bf.setInstitution(nB.getInstitution());
            bf.setSpeciality(nB.getSpeciality());
            bf.setStaff(nB.getStaff());

            bf.setBill(b);
            bf.setBillItem(bt);
            bf.setFeeValue(0 - nB.getFeeValue());
            bf.setFeeGrossValue(0 - nB.getFeeGrossValue());
            bf.setCreatedAt(new Date());
            bf.setCreater(getSessionController().getLoggedUser());

            if (bf.getId() == null) {
                getBillFeeFacade().create(bf);
            }
        }
    }

    @Inject
    private BillBeanController billBean;

    boolean showAllBills;

    public boolean isShowAllBills() {
        return showAllBills;
    }

    public void setShowAllBills(boolean showAllBills) {
        this.showAllBills = showAllBills;
    }

    public void allBills() {
        showAllBills = true;
    }

    public List<Bill> getBills() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().isEmpty()) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.OpdBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.OpdBill);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        showAllBills = false;
        return bills;
    }

    public List<Bill> getColBills() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().isEmpty()) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.LabBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.LabBill);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        showAllBills = false;
        return bills;
    }

    public List<Bill> getPos() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().isEmpty()) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.PharmacyOrder);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.PharmacyOrder);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getSales() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().isEmpty()) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.PharmacySale);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.PharmacySale);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getPreBills() {
        if (bills == null) {
//            if (txtSearch == null || txtSearch.trim().equals("")) {
            bills = getBillBean().billsForTheDay2(getFromDate(), getToDate(), BillType.PharmacyPre);
//            } else {
//                bills = getBillBean().billsFromSearch2(txtSearch, getFromDate(), getToDate(), BillType.PharmacySale);
//            }
//            if (bills == null) {
//                bills = new ArrayList<>();
//            }
        }
        return bills;
    }

    public void makeNull() {
        refundAmount = 0;
        txtSearch = null;
        bill = null;
        paymentMethod = null;
        paymentScheme = null;
        billForRefund = null;
        fromDate = null;
        toDate = null;
        user = null;
        ////////////////
        refundingItems = null;
        bills = null;
        filteredBill = null;
        selectedBills = null;
        billEntrys = null;
        billItems = null;
        billComponents = null;
        billFees = null;
        tempbillItems = null;
        searchRetaiBills = null;
        lazyBills = null;

    }

    public List<Bill> getPreRefundBills() {
        if (bills == null) {
            //   List<Bill> lstBills;
            String sql;
            Map temMap = new HashMap();
            sql = "select b from RefundBill b where b.billType = :billType"
                    + " and b.createdAt between :fromDate and :toDate and b.retired=false order by b.id desc ";

            temMap.put("billType", BillType.PharmacyPre);
            // temMap.put("refBillType", BillType.PharmacySale);
            temMap.put("toDate", toDate);
            temMap.put("fromDate", fromDate);

            bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
            //   bills = getBillBean().billsRefundForTheDay(getFromDate(), getToDate(), BillType.PharmacyPre);

        }
        return bills;
    }

    public List<Bill> getRequests() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().isEmpty()) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.PharmacyTransferRequest);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.PharmacyTransferRequest);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getGrns() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().isEmpty()) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.PharmacyGrnBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.PharmacyGrnBill);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getInstitutionPaymentBills() {
        if (bills == null) {
            String sql;
            Map temMap = new HashMap();
            if (txtSearch == null || txtSearch.trim().isEmpty()) {
                sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.id in(Select bt.bill.id From BillItem bt Where bt.referenceBill.billType!=:btp and bt.referenceBill.billType!=:btp2) and b.billType=:type and b.createdAt between :fromDate and :toDate order by b.id";

            } else {
                sql = "select b from BilledBill b where b.retired=false and"
                        + " b.id in(Select bt.bill.id From BillItem bt Where bt.referenceBill.billType!=:btp and bt.referenceBill.billType!=:btp2) "
                        + "and b.billType=:type and b.createdAt between :fromDate and :toDate and ((b.staff.person.name) like '%" + txtSearch.toUpperCase() + "%'  or (b.staff.person.phone) like '%" + txtSearch.toUpperCase() + "%'  or (b.insId) like '%" + txtSearch.toUpperCase() + "%') order by b.id desc  ";
            }

            temMap.put("toDate", getToDate());
            temMap.put("fromDate", getFromDate());
            temMap.put("type", BillType.PaymentBill);
            temMap.put("btp", BillType.ChannelPaid);
            temMap.put("btp2", BillType.ChannelCredit);
            bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 100);

            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;

    }

    public List<Bill> getChannelPaymentBills() {
        if (bills == null) {
            String sql;
            Map temMap = new HashMap();
            sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.id in"
                    + "(Select bt.bill.id From BillItem bt Where bt.referenceBill.billType=:btp"
                    + " or bt.referenceBill.billType=:btp2) and b.billType=:type and b.createdAt "
                    + "between :fromDate and :toDate order by b.id";

            temMap.put("toDate", getToDate());
            temMap.put("fromDate", getFromDate());
            temMap.put("type", BillType.PaymentBill);
            temMap.put("btp", BillType.ChannelPaid);
            temMap.put("btp2", BillType.ChannelCredit);
            bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 100);

            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;

    }

    public List<Bill> getUserBills() {
        List<Bill> userBills;
        if (getUser() == null) {
            userBills = new ArrayList<>();
        } else {
            userBills = getBillBean().billsFromSearchForUser(txtSearch, getFromDate(), getToDate(), getUser(), BillType.OpdBill);
        }
        if (userBills == null) {
            userBills = new ArrayList<>();
        }
        return userBills;
    }

    public List<Bill> getOpdBills() {
        if (txtSearch == null || txtSearch.trim().isEmpty()) {
            bills = getBillBean().billsForTheDay(fromDate, toDate, BillType.OpdBill);
        } else {
            bills = getBillBean().billsFromSearch(txtSearch, fromDate, toDate, BillType.OpdBill);
        }

        if (bills == null) {
            bills = new ArrayList<>();
        }
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public String getTxtSearch() {
        return txtSearch;
    }

    public void setTxtSearch(String txtSearch) {
        this.txtSearch = txtSearch;
        recreateModel();
    }

    public Bill getBill() {
        //recreateModel();
        return bill;
    }

    public void setBill(Bill bb) {
        recreateModel();
        if (bb == null) {
            bb = this.bill;
        }
        this.bill = bb;
//        if (bb.getPaymentMethod() != null) {
//            paymentMethod = bb.getPaymentMethod();
//        }

    }

    @EJB
    private com.divudi.service.pharmacy.BhtIssueRequestNativeSqlService bhtIssueRequestNativeSqlService;

    private com.divudi.core.data.dto.pharmacy.BhtIssueRequestPrintDto bhtIssueRequestPrintDto;
    private Long bhtIssueRequestPrintDtoBillId;

    public com.divudi.core.data.dto.pharmacy.BhtIssueRequestPrintDto getBhtIssueRequestPrintDto() {
        if (bill == null || bill.getId() == null) {
            return null;
        }
        if (!bill.getId().equals(bhtIssueRequestPrintDtoBillId)) {
            bhtIssueRequestPrintDto = bhtIssueRequestNativeSqlService.loadPrintDtoByBillId(bill.getId());
            bhtIssueRequestPrintDtoBillId = bill.getId();
            if (bhtIssueRequestPrintDto == null) {
                JsfUtil.addErrorMessage("BHT Issue Request not found");
            }
        }
        return bhtIssueRequestPrintDto;
    }

    // Per-print-job "With Rate" / "Without Rate" choice for the BHT Issue Bill
    // reprint (ward_pharmacy_reprint_bht_issue_bill_reprint.xhtml). Gated by
    // the IPRequestViewRates privilege in the XHTML; defaults to false (no
    // rate) so users never see rates unless they explicitly opt in.
    private boolean printBhtIssueBillWithRate;

    public boolean isPrintBhtIssueBillWithRate() {
        return printBhtIssueBillWithRate;
    }

    public void setPrintBhtIssueBillWithRate(boolean printBhtIssueBillWithRate) {
        this.printBhtIssueBillWithRate = printBhtIssueBillWithRate;
    }

    public String getReturnPage() {
        return returnPage;
    }

    public void setReturnPage(String returnPage) {
        this.returnPage = returnPage;
    }

    public List<BillEntry> getBillEntrys() {
        return billEntrys;
    }

    public void setBillEntrys(List<BillEntry> billEntrys) {
        this.billEntrys = billEntrys;
    }

    public List<BillItem> getBillItems() {
        String sql;
        if (getBill() != null) {
            if (getBill().getRefundedBill() == null) {
                sql = "SELECT b FROM BillItem b WHERE b.retired=false and b.bill.id=" + getBill().getId();
            } else {
                sql = "SELECT b FROM BillItem b WHERE b.retired=false and b.bill.id=" + getBill().getRefundedBill().getId();
            }
            billItems = getBillItemFacede().findByJpql(sql);
            if (billItems == null) {
                billItems = new ArrayList<>();
            }
        }

        return billItems;
    }

    public List<PharmaceuticalBillItem> getPharmacyBillItems() {
        List<PharmaceuticalBillItem> tmp = new ArrayList<>();
        if (getBill() != null) {
            String sql = "SELECT b FROM PharmaceuticalBillItem b WHERE b.billItem.retired=false and b.billItem.bill.id=" + getBill().getId();
            tmp = getPharmaceuticalBillItemFacade().findByJpql(sql);
        }

        return tmp;
    }

    public List<BillComponent> getBillComponents() {
        if (getBill() != null) {
            String sql = "SELECT b FROM BillComponent b WHERE b.retired=false and b.bill.id=" + getBill().getId();
            billComponents = getBillCommponentFacade().findByJpql(sql);
            if (billComponents == null) {
                billComponents = new ArrayList<>();
            }
        }
        return billComponents;
    }

    public List<BillFee> getBillFees() {
        if (getBill() != null) {
            if (billFees == null || billForRefund == null) {
                String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getBill().getId();
                billFees = getBillFeeFacade().findByJpql(sql);
            }
        }

        if (billFees == null) {
            billFees = new ArrayList<>();
        }

        return billFees;
    }

    public List<BillFee> getBillFees(BillItem bi) {

        String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.billItem=:b ";
        HashMap hm = new HashMap();
        hm.put("b", bi);
        List<BillFee> ls = getBillFeeFacade().findByJpql(sql, hm);

        if (ls == null) {
            ls = new ArrayList<>();
        }

        return ls;
    }

    public List<BillFee> getBillFees2() {
        if (getBill() != null) {
            if (billFees == null) {
                String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getBill().getId();
                billFees = getBillFeeFacade().findByJpql(sql);
            }
        }

        if (billFees == null) {
            billFees = new ArrayList<>();
        }

        return billFees;
    }

    public List<BillFee> getPayingBillFees() {
        if (getBill() != null) {
            String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getBill().getId();
            billFees = getBillFeeFacade().findByJpql(sql);
            if (billFees == null) {
                billFees = new ArrayList<>();
            }

        }

        return billFees;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public void setBillComponents(List<BillComponent> billComponents) {
        this.billComponents = billComponents;
    }

    /**
     * Creates a new instance of BillSearch
     */
    public PharmacyBillSearch() {
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public BillItemFacade getBillItemFacede() {
        return billItemFacede;
    }

    public void setBillItemFacede(BillItemFacade billItemFacede) {
        this.billItemFacede = billItemFacede;
    }

    public BillComponentFacade getBillCommponentFacade() {
        return billCommponentFacade;
    }

    public void setBillCommponentFacade(BillComponentFacade billCommponentFacade) {
        this.billCommponentFacade = billCommponentFacade;
    }

    private void setRefundAttribute() {
        billForRefund.setBalance(getBill().getBalance());

        billForRefund.setBillDate(Calendar.getInstance().getTime());
        billForRefund.setBillTime(Calendar.getInstance().getTime());
        billForRefund.setCreater(getSessionController().getLoggedUser());
        billForRefund.setCreatedAt(Calendar.getInstance().getTime());

        billForRefund.setBillType(getBill().getBillType());
        billForRefund.setBilledBill(getBill());

        billForRefund.setCatId(getBill().getCatId());
        billForRefund.setCollectingCentre(getBill().getCollectingCentre());
        billForRefund.setCreditCardRefNo(getBill().getCreditCardRefNo());
        billForRefund.setCreditCompany(getBill().getCreditCompany());

        billForRefund.setDepartment(getBill().getDepartment());
        billForRefund.setDeptId(getBill().getDeptId());
        billForRefund.setDiscount(getBill().getDiscount());

        billForRefund.setDiscountPercent(getBill().getDiscountPercent());
        billForRefund.setFromDepartment(getBill().getFromDepartment());
        billForRefund.setFromInstitution(getBill().getFromInstitution());
        billForRefund.setFromStaff(getBill().getFromStaff());

        billForRefund.setInsId(getBill().getInsId());
        billForRefund.setInstitution(getBill().getInstitution());

        billForRefund.setPatient(getBill().getPatient());
        billForRefund.setPatientEncounter(getBill().getPatientEncounter());
        billForRefund.setPaymentMethod(paymentMethod);
        billForRefund.setPaymentScheme(getBill().getPaymentScheme());
        billForRefund.setPaymentSchemeInstitution(getBill().getPaymentSchemeInstitution());

        billForRefund.setReferredBy(getBill().getReferredBy());
        billForRefund.setReferringDepartment(getBill().getReferringDepartment());

        billForRefund.setStaff(getBill().getStaff());

        billForRefund.setToDepartment(getBill().getToDepartment());
        billForRefund.setToInstitution(getBill().getToInstitution());
        billForRefund.setToStaff(getBill().getToStaff());
        billForRefund.setTotal(calTot());
        //Need To Add Net Total Logic
        billForRefund.setNetTotal(billForRefund.getTotal());
    }

    public double calTot() {
        if (getBillFees() == null) {
            return 0.0f;
        }
        double tot = 0.0f;
        for (BillFee f : getBillFees()) {
            tot += f.getFeeValue();
        }
        getBillForRefund().setTotal(tot);
        return tot;
    }

    public RefundBill getBillForRefund() {

        if (billForRefund == null) {
            billForRefund = new RefundBill();
            setRefundAttribute();
        }

        return billForRefund;
    }

    public String viewBill() {
        boolean manageCosting = isManageCostingEnabled();

        if (bill != null) {
            switch (bill.getBillType()) {
                case PharmacyPre:
                case PharmacyBhtPre:
                case PharmacyWholesalePre:
                    return "pharmacy_reprint_bill_sale?faces-redirect=true";
                case PharmacyIssue:
                    return "pharmacy_reprint_bill_unit_issue?faces-redirect=true";
                case PharmacyTransferIssue:
                    return "/pharmacy/pharmacy_reprint_transfer_isssue?faces-redirect=true";
                case PharmacyTransferReceive:
                    return "pharmacy_reprint_transfer_receive?faces-redirect=true";
                case PharmacyPurchaseBill:
                    return "pharmacy_reprint_purchase?faces-redirect=true";
                case PharmacyGrnBill:
                    if (manageCosting) {
                        return "/pharmacy/pharmacy_reprint_grn_with_costing?faces-redirect=true";
                    }
                    return "pharmacy_reprint_grn?faces-redirect=true";
                case PharmacyGrnReturn:
                    return "pharmacy_reprint_grn_return?faces-redirect=true";
                case PurchaseReturn:
                    return "pharmacy_reprint_purchase_return?faces-redirect=true";
                case PharmacyAdjustment:
                    return "pharmacy_reprint_adjustment?faces-redirect=true";
                case PharmacyDonationBill:
                    return "pharmacy_reprint_donation?faces-redirect=true";
                default:
                    return "pharmacy_reprint_bill_sale?faces-redirect=true";
            }
        } else {

            return "";
        }

    }

    public void setBillForRefund(RefundBill billForRefund) {
        this.billForRefund = billForRefund;
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(double refundAmount) {
        this.refundAmount = refundAmount;
    }

    public WebUserController getWebUserController() {
        return webUserController;
    }

    public void setWebUserController(WebUserController webUserController) {
        this.webUserController = webUserController;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    @Deprecated
    public boolean isShowAllBillFormats() {
        return showAllBillFormats;
    }

    @Deprecated
    public void setShowAllBillFormats(boolean showAllBillFormats) {
        this.showAllBillFormats = showAllBillFormats;
    }

    @Deprecated
    public String toggleShowAllBillFormats() {
        this.showAllBillFormats = !this.showAllBillFormats;
        return "";
    }

    public List<BillItem> getTempbillItems() {
        if (tempbillItems == null) {
            tempbillItems = new ArrayList<>();
        }
        return tempbillItems;
    }

    public void setTempbillItems(List<BillItem> tempbillItems) {
        this.tempbillItems = tempbillItems;
    }

    public void resetLists() {
        recreateModel();
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        resetLists();
        this.toDate = toDate;

    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        resetLists();
        this.fromDate = fromDate;

    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public ItemBatchFacade getItemBatchFacade() {
        return itemBatchFacade;
    }

    public void setItemBatchFacade(ItemBatchFacade itemBatchFacade) {
        this.itemBatchFacade = itemBatchFacade;
    }

    public List<Bill> getFilteredBill() {
        return filteredBill;
    }

    public void setFilteredBill(List<Bill> filteredBill) {
        this.filteredBill = filteredBill;
    }

    private boolean checkCollectedReported() {
        return false;
    }

    public List<Bill> getSearchRetaiBills() {
        return searchRetaiBills;
    }

    public String importBill() {
        if (file == null || file.getSize() == 0) {
            JsfUtil.addErrorMessage("No JSON file selected.");
            return null;
        }

        String jsonString;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            jsonString = reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error reading file: " + e.getMessage());
            return null;
        }

        bill = billService.convertJsonToBill(jsonString);
        if (bill == null || bill.getBillTypeAtomic() == null) {
            JsfUtil.addErrorMessage("Invalid JSON structure.");
            return null;
        }

        if (Objects.requireNonNull(bill.getBillTypeAtomic()) == BillTypeAtomic.PHARMACY_GRN) {
            return importGrnBill(bill);
        }
        JsfUtil.addErrorMessage("Bill Type does NOT support import.");
        return null;
    }

    public String importGrnBill(Bill importGrnBill) {
        return grnController.navigateToResiveFromImportGrn(importGrnBill);
    }

    public void setSearchRetaiBills(List<Bill> searchRetaiBills) {
        this.searchRetaiBills = searchRetaiBills;
    }

    public List<Bill> getSelectedBills() {
        if (selectedBills == null) {
            selectedBills = new ArrayList<>();
        }
        return selectedBills;
    }

    public void setSelectedBills(List<Bill> selectedBills) {
        this.selectedBills = selectedBills;
    }

    public StaffService getStaffBean() {
        return staffBean;
    }

    public void setStaffBean(StaffService staffBean) {
        this.staffBean = staffBean;
    }

    public BillFeePaymentFacade getBillFeePaymentFacade() {
        return billFeePaymentFacade;
    }

    public void setBillFeePaymentFacade(BillFeePaymentFacade billFeePaymentFacade) {
        this.billFeePaymentFacade = billFeePaymentFacade;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public void prepareEmailDialog() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill");
            return;
        }

        // Set default email if available
        if (bill.getToInstitution() != null && bill.getToInstitution().getEmail() != null) {
            emailRecipient = bill.getToInstitution().getEmail();
        } else {
            emailRecipient = "";
        }
    }

    public void sendPurchaseOrderEmail() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill");
            return;
        }

        if (emailRecipient == null || emailRecipient.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter recipient email");
            return;
        }

        String recipient = emailRecipient.trim();
        if (!CommonFunctions.isValidEmail(recipient)) {
            JsfUtil.addErrorMessage("Please enter a valid email address");
            return;
        }

        String body = generatePurchaseOrderHtml();
        if (body == null) {
            JsfUtil.addErrorMessage("Could not generate email body");
            return;
        }

        AppEmail email = new AppEmail();
        email.setCreatedAt(new Date());
        email.setCreater(sessionController.getLoggedUser());
        email.setReceipientEmail(recipient);
        email.setMessageSubject("Purchase Order");
        email.setMessageBody(body);
        email.setDepartment(sessionController.getLoggedUser().getDepartment());
        email.setInstitution(sessionController.getLoggedUser().getInstitution());
        email.setBill(bill);
        email.setMessageType(MessageType.Marketing);
        email.setSentSuccessfully(false);
        email.setPending(true);
        emailFacade.create(email);

        try {
            boolean success = emailManagerEjb.sendEmail(
                    java.util.Collections.singletonList(recipient),
                    body,
                    "Purchase Order",
                    true
            );
            email.setSentSuccessfully(success);
            email.setPending(!success);
            if (success) {
                email.setSentAt(new Date());
                JsfUtil.addSuccessMessage("Email Sent Successfully");
            } else {
                JsfUtil.addErrorMessage("Sending Email Failed");
            }
            emailFacade.edit(email);
        } catch (Exception ex) {
            JsfUtil.addErrorMessage("Sending Email Failed");
        }
    }

    private String generatePurchaseOrderHtml() {
        try {
            javax.faces.context.FacesContext fc = javax.faces.context.FacesContext.getCurrentInstance();
            javax.faces.component.UIComponent comp = fc.getViewRoot().findComponent("gpBillPreview");
            if (comp == null) {
                return null;
            }
            java.io.StringWriter sw = new java.io.StringWriter();
            javax.faces.context.ResponseWriter original = fc.getResponseWriter();
            javax.faces.context.ResponseWriter rw = fc.getRenderKit().createResponseWriter(sw, null, "UTF-8");
            fc.setResponseWriter(rw);
            comp.encodeAll(fc);
            fc.setResponseWriter(original);
            return sw.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public EmailFacade getEmailFacade() {
        return emailFacade;
    }

    public void setEmailFacade(EmailFacade emailFacade) {
        this.emailFacade = emailFacade;
    }

    public EmailManagerEjb getEmailManagerEjb() {
        return emailManagerEjb;
    }

    public void setEmailManagerEjb(EmailManagerEjb emailManagerEjb) {
        this.emailManagerEjb = emailManagerEjb;
    }

    public String getEmailRecipient() {
        return emailRecipient;
    }

    public void setEmailRecipient(String emailRecipient) {
        this.emailRecipient = emailRecipient;
    }

    /**
     * Navigate to Return Items Only page (Payment will be released at the
     * cashier)
     */
    public String navigateToReturnItemsOnly() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        if (bill.isCancelled()) {
            JsfUtil.addErrorMessage("Cancelled bills cannot be returned");
            return null;
        }
        // Check if credit has been partially or fully settled
        if (bill.getPaymentMethod() == PaymentMethod.Credit){
            if (bill.getPaidAmount() > 0) {
                JsfUtil.addErrorMessage("Cannot return items for bills with partially or fully settled credit. Please contact the administrator.");
                return null;
            }
        }
        
        // Set the bill in PreReturnController and navigate directly to return process
        preReturnController.setBill(bill);
        return "/pharmacy/pharmacy_bill_return_pre?faces-redirect=true";
    }

    /**
     * Navigate to Return Goods and Payment page (Will take goods and return
     * payments)
     */
    public String navigateToReturnGoodsAndPayment() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        if (bill.isCancelled()) {
            JsfUtil.addErrorMessage("Cancelled bills cannot be returned");
            return null;
        }
        // Check if credit has been partially or fully settled
        if (bill.getPaymentMethod() == PaymentMethod.Credit){
            if (bill.getPaidAmount() > 0) {
                JsfUtil.addErrorMessage("Cannot return items for bills with partially or fully settled credit. Please contact the administrator.");
                return null;
            }
        }     
        // Set the bill in SaleReturnController and navigate directly to return process
        saleReturnController.setBill(bill);
        return saleReturnController.navigateToReturnItemsAndPaymentsForPharmacyRetailSale();
    }

    /**
     * Navigate back to the reprint bill page from return pages
     */
    public String navigateBackToReprintBill() {
        return "/pharmacy/pharmacy_reprint_bill_sale_cashier?faces-redirect=true";
    }

    /**
     * Navigate to search sale pre bill page
     */
    public String navigateToSearchSalePreBill() {
        return "/pharmacy/pharmacy_search_sale_pre_bill?faces-redirect=true";
    }

    public String navigateToViewPharmacyDisposalIssueBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected.");
            return null;
        }
        // Disposal issues share the generic unit-issue reprint view.
        return "/pharmacy/pharmacy_reprint_bill_unit_issue?faces-redirect=true";
    }

    public String navigateToViewPharmacyDisposalReturnBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected.");
            return null;
        }
        // Navigate to disposal return reprint page
        return "/pharmacy/pharmacy_disposal_return_reprint?faces-redirect=true";
    }

    public String navigateToViewPharmacyIssueBill() {
        System.out.println("navigateToViewPharmacyIssueBill");
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill Selected.");
            return null;
        }
        // Navigate to pharmacy issue view page with transfer issue print formats
        return "/pharmacy/pharmacy_issue_view?faces-redirect=true";
    }

    /**
     * Navigation helper for DTO-based transfer issue tables; loads the full
     * Bill from the id set via {@link #setBillId(Long)}.
     */
    public String navigateToViewPharmacyTransferIssueById() {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        Bill selectedBill = billService.reloadBill(billId);
        if (selectedBill == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        if (selectedBill.getBillType() != BillType.PharmacyTransferIssue) {
            JsfUtil.addErrorMessage("Invalid Bill Type");
            return null;
        }
        bill = selectedBill;
        return "/pharmacy/pharmacy_reprint_transfer_isssue?faces-redirect=true";
    }

    /**
     * Bill id used by DTO report tables to fetch a bill directly.
     */
    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
        if (billId != null) {
            this.bill = billService.reloadBill(billId);
        } else {
            this.bill = null;
        }
    }

    public List<PharmacySaleSearchDTO> getSaleBillDtos() {
        return saleBillDtos;
    }

    public void setSaleBillDtos(List<PharmacySaleSearchDTO> saleBillDtos) {
        this.saleBillDtos = saleBillDtos;
    }

    /**
     * Fetches PHARMACY_RETAIL_SALE and PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER bills as DTOs.
     * <p>
     * Uses COALESCE on every nullable LEFT JOIN string field so that bills whose
     * creator, patient, referredBy or paymentScheme is null are still included in
     * the result set instead of being silently dropped by JPQL.
     *
     * @param maxResult the maximum number of rows to return; 0 or negative means unlimited
     */
    @SuppressWarnings("unchecked")
    public void fetchSaleSearchDtosFromNativeBills(int maxResult) {
        List<com.divudi.core.data.BillTypeAtomic> btas = new ArrayList<>();
        btas.add(com.divudi.core.data.BillTypeAtomic.PHARMACY_RETAIL_SALE);
        btas.add(com.divudi.core.data.BillTypeAtomic.PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER);
        Map<String, Object> m = new HashMap<>();
        m.put("btas", btas);
        m.put("fd", searchController.getFromDate());
        m.put("td", searchController.getToDate());
        m.put("ins", sessionController.getInstitution());
        m.put("ldep", sessionController.getLoggedUser().getDepartment());

        // COALESCE on every nullable LEFT JOIN string prevents rows from being
        // silently dropped when any joined relationship is null (JPQL behaviour).
        String sql = "SELECT new com.divudi.core.data.dto.PharmacySaleSearchDTO("
                + "b.id, b.deptId, b.department.name, b.createdAt, "
                + "COALESCE(creatorPerson.name, ''), "
                + "COALESCE(patientPerson.name, ''), "
                + "COALESCE(refDoctorPerson.name, ''), "
                + "b.paymentMethod, "
                + "COALESCE(ps.name, ''), "
                + "b.total, b.discount, b.netTotal, "
                + "b.refunded, b.cancelled, "
                + "COALESCE(b.comments, '')) "
                + "FROM Bill b "
                + "LEFT JOIN b.creater creater "
                + "LEFT JOIN creater.webUserPerson creatorPerson "
                + "LEFT JOIN b.patient patient "
                + "LEFT JOIN patient.person patientPerson "
                + "LEFT JOIN b.referredBy referredBy "
                + "LEFT JOIN referredBy.person refDoctorPerson "
                + "LEFT JOIN b.paymentScheme ps "
                + "WHERE b.billTypeAtomic IN :btas "
                + "AND b.createdAt BETWEEN :fd AND :td "
                + "AND b.institution = :ins "
                + "AND b.department = :ldep";

        if (searchController.getSearchKeyword().getPatientName() != null && !searchController.getSearchKeyword().getPatientName().trim().isEmpty()) {
            sql += " AND patientPerson.name LIKE :patientName";
            m.put("patientName", "%" + searchController.getSearchKeyword().getPatientName().trim() + "%");
        }
        if (searchController.getSearchKeyword().getBillNo() != null && !searchController.getSearchKeyword().getBillNo().trim().isEmpty()) {
            sql += " AND b.deptId LIKE :billNo";
            m.put("billNo", "%" + searchController.getSearchKeyword().getBillNo().trim() + "%");
        }
        if (searchController.getSearchKeyword().getDepartment() != null && !searchController.getSearchKeyword().getDepartment().trim().isEmpty()) {
            sql += " AND b.department.name LIKE :dep";
            m.put("dep", "%" + searchController.getSearchKeyword().getDepartment().trim() + "%");
        }
        if (searchController.getSearchKeyword().getPatientPhone() != null && !searchController.getSearchKeyword().getPatientPhone().trim().isEmpty()) {
            sql += " AND patientPerson.phone LIKE :phone";
            m.put("phone", "%" + searchController.getSearchKeyword().getPatientPhone().trim() + "%");
        }
        if (searchController.getPaymentMethod() != null) {
            sql += " AND b.paymentMethod = :pay";
            m.put("pay", searchController.getPaymentMethod());
        }
        if (searchController.getSearchKeyword().getNetTotal() != null && !searchController.getSearchKeyword().getNetTotal().trim().isEmpty()) {
            try {
                sql += " AND b.netTotal = :netTotal";
                m.put("netTotal", Double.parseDouble(searchController.getSearchKeyword().getNetTotal().trim()));
            } catch (NumberFormatException e) {
                // skip invalid input
            }
        }
        if (searchController.getSearchKeyword().getTotal() != null && !searchController.getSearchKeyword().getTotal().trim().isEmpty()) {
            try {
                sql += " AND b.total = :total";
                m.put("total", Double.parseDouble(searchController.getSearchKeyword().getTotal().trim()));
            } catch (NumberFormatException e) {
                // skip invalid input
            }
        }

        sql += " ORDER BY b.createdAt DESC";

        if (maxResult > 0) {
            saleBillDtos = (List<PharmacySaleSearchDTO>) billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP, maxResult);
        } else {
            saleBillDtos = (List<PharmacySaleSearchDTO>) billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP);
        }
        if (saleBillDtos == null) {
            saleBillDtos = new ArrayList<>();
        }
    }

    /**
     * @deprecated Use {@link #fetchSaleSearchDtosFromNativeBills(int)} which
     * accepts an explicit row limit from {@code searchController.getMaxResult()}.
     * Kept for backward compatibility with existing callers
     * ({@code SearchController.createPharmacyRetailBills()} and
     * {@code createPharmacyRetailAllBills()}).
     */
    @Deprecated
    public void fetchSaleSearchDtosFromNativeBills(boolean maxNum) {
        fetchSaleSearchDtosFromNativeBills(maxNum ? 25 : 0);
    }

    public List<com.divudi.core.data.dto.PharmacySaleBillItemSearchDTO> getSaleBillItemDtos() {
        return saleBillItemDtos;
    }

    public void setSaleBillItemDtos(List<com.divudi.core.data.dto.PharmacySaleBillItemSearchDTO> saleBillItemDtos) {
        this.saleBillItemDtos = saleBillItemDtos;
    }

    /**
     * Fetches pharmacy retail sale bill items (via PreBill -> BilledBill) as DTOs.
     * Used by pharmacy_search_sale_bill_item.xhtml (issue #21792).
     *
     * @param maxResult the maximum number of rows to return; 0 or negative means unlimited
     */
    @SuppressWarnings("unchecked")
    public void fetchSaleBillItemSearchDtos(int maxResult) {
        Map<String, Object> m = new HashMap<>();
        m.put("class", com.divudi.core.entity.PreBill.class);
        m.put("rClass", com.divudi.core.entity.BilledBill.class);
        m.put("bType", com.divudi.core.data.BillType.PharmacyPre);
        m.put("rBType", com.divudi.core.data.BillType.PharmacySale);
        m.put("fromDate", searchController.getFromDate());
        m.put("toDate", searchController.getToDate());
        m.put("ins", sessionController.getInstitution());
        m.put("ldep", sessionController.getLoggedUser().getDepartment());

        String sql = "SELECT new com.divudi.core.data.dto.PharmacySaleBillItemSearchDTO("
                + "rb.id, rb.deptId, rb.createdAt, "
                + "it.id, COALESCE(it.name, ''), COALESCE(it.code, ''), "
                + "bi.qty, bi.netValue, "
                + "COALESCE(pp.name, ''), "
                + "rb.cancelled, rb.refunded) "
                + "FROM BillItem bi "
                + "JOIN bi.bill b "
                + "JOIN b.referenceBill rb "
                + "LEFT JOIN bi.item it "
                + "LEFT JOIN b.patient p "
                + "LEFT JOIN p.person pp "
                + "WHERE type(b) = :class "
                + "AND type(rb) = :rClass "
                + "AND b.institution = :ins "
                + "AND b.department = :ldep "
                + "AND b.billType = :bType "
                + "AND rb.billType = :rBType "
                + "AND rb.createdAt BETWEEN :fromDate AND :toDate";

        if (searchController.getSearchKeyword().getPatientName() != null && !searchController.getSearchKeyword().getPatientName().trim().isEmpty()) {
            sql += " AND pp.name LIKE :patientName";
            m.put("patientName", "%" + searchController.getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }
        if (searchController.getSearchKeyword().getBillNo() != null && !searchController.getSearchKeyword().getBillNo().trim().isEmpty()) {
            sql += " AND rb.deptId LIKE :billNo";
            m.put("billNo", "%" + searchController.getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }
        if (searchController.getSearchKeyword().getTotal() != null && !searchController.getSearchKeyword().getTotal().trim().isEmpty()) {
            try {
                double total = Double.parseDouble(searchController.getSearchKeyword().getTotal().trim());
                sql += " AND bi.netValue = :total";
                m.put("total", total);
            } catch (NumberFormatException e) {
                // ignore invalid numeric input, skip filter
            }
        }
        if (searchController.getSearchKeyword().getItemName() != null && !searchController.getSearchKeyword().getItemName().trim().isEmpty()) {
            sql += " AND it.name LIKE :itm";
            m.put("itm", "%" + searchController.getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }
        if (searchController.getSearchKeyword().getCode() != null && !searchController.getSearchKeyword().getCode().trim().isEmpty()) {
            sql += " AND it.code LIKE :cde";
            m.put("cde", "%" + searchController.getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " ORDER BY bi.id DESC";

        if (maxResult > 0) {
            saleBillItemDtos = (List<com.divudi.core.data.dto.PharmacySaleBillItemSearchDTO>) getBillItemFacade()
                    .findLightsByJpql(sql, m, TemporalType.TIMESTAMP, maxResult);
        } else {
            saleBillItemDtos = (List<com.divudi.core.data.dto.PharmacySaleBillItemSearchDTO>) getBillItemFacade()
                    .findLightsByJpql(sql, m, TemporalType.TIMESTAMP);
        }
        if (saleBillItemDtos == null) {
            saleBillItemDtos = new ArrayList<>();
        }
    }

    // -----------------------------------------------------------------------
    // Transfer Request search DTO (issue #21006 / #20299)
    // -----------------------------------------------------------------------

    public List<com.divudi.core.data.dto.PharmacyTransferRequestListDTO> getTransferRequestSearchDtos() {
        return transferRequestSearchDtos;
    }

    public void setTransferRequestSearchDtos(List<com.divudi.core.data.dto.PharmacyTransferRequestListDTO> transferRequestSearchDtos) {
        this.transferRequestSearchDtos = transferRequestSearchDtos;
    }

    /**
     * Fetches PharmacyTransferRequest bills as lightweight DTOs.
     * <p>
     * Uses COALESCE on the creator name (nullable LEFT JOIN) so that bills
     * whose creator has no linked Person record are still included.
     * Cancellation date and canceller details are intentionally omitted from
     * the list view to avoid multi-hop nullable LEFT JOINs; users can open the
     * bill via the View action to see full cancellation information.
     *
     * @param maxResult maximum rows to return; 0 or negative means unlimited
     */
    @SuppressWarnings("unchecked")
    public void fetchTransferRequestSearchDtos(int maxResult) {
        Map<String, Object> m = new HashMap<>();
        m.put("bt", com.divudi.core.data.BillType.PharmacyTransferRequest);
        m.put("fd", searchController.getFromDate());
        m.put("td", searchController.getToDate());
        m.put("dep", sessionController.getLoggedUser().getDepartment());

        // 8-param constructor: (billId, deptId, createdAt, fromDepartmentName,
        // creatorName, cancelled, cancelledAt, cancellerName).
        // NULL literal for cancelledAt; '' for cancellerName — list view shows
        // only the cancelled badge; full details available via View action.
        String sql = "SELECT new com.divudi.core.data.dto.PharmacyTransferRequestListDTO("
                + "b.id, b.deptId, b.createdAt, "
                + "COALESCE(b.toDepartment.name, ''), "
                + "COALESCE(creatorPerson.name, ''), "
                + "b.cancelled, NULL, '') "
                + "FROM Bill b "
                + "LEFT JOIN b.creater creater "
                + "LEFT JOIN creater.webUserPerson creatorPerson "
                + "WHERE b.billType = :bt "
                + "AND b.createdAt BETWEEN :fd AND :td "
                + "AND b.department = :dep "
                + "AND b.retired = false";

        if (searchController.getSearchKeyword().getBillNo() != null
                && !searchController.getSearchKeyword().getBillNo().trim().isEmpty()) {
            sql += " AND b.deptId LIKE :billNo";
            m.put("billNo", "%" + searchController.getSearchKeyword().getBillNo().trim() + "%");
        }
        if (searchController.getSearchKeyword().getDepartment() != null
                && !searchController.getSearchKeyword().getDepartment().trim().isEmpty()) {
            sql += " AND b.toDepartment.name LIKE :toDep";
            m.put("toDep", "%" + searchController.getSearchKeyword().getDepartment().trim() + "%");
        }
        if (searchController.getSearchKeyword().getNetTotal() != null
                && !searchController.getSearchKeyword().getNetTotal().trim().isEmpty()) {
            try {
                sql += " AND b.netTotal = :netTotal";
                m.put("netTotal", Double.parseDouble(searchController.getSearchKeyword().getNetTotal().trim()));
            } catch (NumberFormatException e) {
                // skip invalid input
            }
        }

        sql += " ORDER BY b.createdAt DESC";

        if (maxResult > 0) {
            transferRequestSearchDtos = (List<com.divudi.core.data.dto.PharmacyTransferRequestListDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP, maxResult);
        } else {
            transferRequestSearchDtos = (List<com.divudi.core.data.dto.PharmacyTransferRequestListDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP);
        }
        if (transferRequestSearchDtos == null) {
            transferRequestSearchDtos = new ArrayList<>();
        }
    }

    // -----------------------------------------------------------------------
    // Transfer Issue search DTO (issue #21007 / #20299)
    // -----------------------------------------------------------------------

    public List<PharmacyTransferIssueSearchDTO> getTransferIssueSearchDtos() {
        return transferIssueSearchDtos;
    }

    public void setTransferIssueSearchDtos(List<PharmacyTransferIssueSearchDTO> transferIssueSearchDtos) {
        this.transferIssueSearchDtos = transferIssueSearchDtos;
    }

    /**
     * Fetches PharmacyTransferIssue bills as lightweight DTOs.
     * <p>
     * COALESCE is applied to every nullable LEFT JOIN string field so that
     * bills whose creator or staff record is null are still included rather
     * than being silently dropped by JPQL.
     *
     * @param maxResult maximum rows to return; 0 or negative means unlimited
     */
    @SuppressWarnings("unchecked")
    public void fetchTransferIssueSearchDtos(int maxResult) {
        Map<String, Object> m = new HashMap<>();
        m.put("bt", com.divudi.core.data.BillType.PharmacyTransferIssue);
        m.put("fd", searchController.getFromDate());
        m.put("td", searchController.getToDate());
        m.put("dep", sessionController.getLoggedUser().getDepartment());

        // Use BilledBill (not Bill) to exclude CancelledBill subtype rows that
        // share the same BillType. The original fetchPharmacyBillsNew also
        // queried FROM BilledBill and filtered b.cancelled=false.
        String sql = "SELECT new com.divudi.core.data.dto.PharmacyTransferIssueSearchDTO("
                + "b.id, b.deptId, b.createdAt, "
                + "COALESCE(b.toDepartment.name, ''), "
                + "COALESCE(creatorPerson.name, ''), "
                + "COALESCE(staffPerson.name, ''), "
                + "b.cancelled, b.netTotal, COALESCE(b.comments, '')) "
                + "FROM BilledBill b "
                + "LEFT JOIN b.creater creater "
                + "LEFT JOIN creater.webUserPerson creatorPerson "
                + "LEFT JOIN b.toStaff toStaff "
                + "LEFT JOIN toStaff.person staffPerson "
                + "WHERE b.billType = :bt "
                + "AND b.createdAt BETWEEN :fd AND :td "
                + "AND b.department = :dep "
                + "AND b.retired = false";

        if (searchController.getSearchKeyword().getBillNo() != null
                && !searchController.getSearchKeyword().getBillNo().trim().isEmpty()) {
            sql += " AND b.deptId LIKE :billNo";
            m.put("billNo", "%" + searchController.getSearchKeyword().getBillNo().trim() + "%");
        }
        if (searchController.getSearchKeyword().getDepartment() != null
                && !searchController.getSearchKeyword().getDepartment().trim().isEmpty()) {
            sql += " AND b.toDepartment.name LIKE :toDep";
            m.put("toDep", "%" + searchController.getSearchKeyword().getDepartment().trim() + "%");
        }
        if (searchController.getSearchKeyword().getNetTotal() != null
                && !searchController.getSearchKeyword().getNetTotal().trim().isEmpty()) {
            try {
                sql += " AND b.netTotal = :netTotal";
                m.put("netTotal", Double.parseDouble(searchController.getSearchKeyword().getNetTotal().trim()));
            } catch (NumberFormatException e) {
                // skip invalid input
            }
        }

        sql += " ORDER BY b.createdAt DESC";

        if (maxResult > 0) {
            transferIssueSearchDtos = (List<PharmacyTransferIssueSearchDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP, maxResult);
        } else {
            transferIssueSearchDtos = (List<PharmacyTransferIssueSearchDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP);
        }
        if (transferIssueSearchDtos == null) {
            transferIssueSearchDtos = new ArrayList<>();
        }
    }

    public List<com.divudi.core.data.dto.PharmacyTransferReceivedListDTO> getTransferReceiveSearchDtos() {
        return transferReceiveSearchDtos;
    }

    public void setTransferReceiveSearchDtos(List<com.divudi.core.data.dto.PharmacyTransferReceivedListDTO> transferReceiveSearchDtos) {
        this.transferReceiveSearchDtos = transferReceiveSearchDtos;
    }

    @SuppressWarnings("unchecked")
    public void fetchTransferReceiveSearchDtos(int maxResult) {
        Map<String, Object> m = new HashMap<>();
        m.put("bt", com.divudi.core.data.BillType.PharmacyTransferReceive);
        m.put("fd", searchController.getFromDate());
        m.put("td", searchController.getToDate());
        m.put("dep", sessionController.getLoggedUser().getDepartment());

        // Note: b.insId (institution-wide receive no.) and b.deptId (department-local
        // receive no.) genuinely differ for most PharmacyTransferReceive bills - the
        // pharmacy_transfer_recieve.xhtml "Receive No" column has always read insId, so
        // both are carried here (issue #20523) rather than assuming deptId is equivalent.
        // cancelledBill join + refundedBill join restore the "Cancelled By"/"Cancelled at"
        // and comments detail the composite renders, which the previous 9-arg-constructor
        // call here never populated.
        String sql = "SELECT new com.divudi.core.data.dto.PharmacyTransferReceivedListDTO("
                + "b.id, b.deptId, b.insId, b.createdAt, "
                + "b.cancelled, "
                + "COALESCE(creatorPerson.name, ''), "
                + "b.netTotal, "
                + "COALESCE(cancellerPerson.name, ''), cb.createdAt, "
                + "COALESCE(b.fromDepartment.name, ''), "
                + "COALESCE(staffPerson.name, ''), "
                + "COALESCE(rb.comments, cb.comments, '')) "
                + "FROM BilledBill b "
                + "LEFT JOIN b.creater creater "
                + "LEFT JOIN creater.webUserPerson creatorPerson "
                + "LEFT JOIN b.cancelledBill cb "
                + "LEFT JOIN cb.creater canceller "
                + "LEFT JOIN canceller.webUserPerson cancellerPerson "
                + "LEFT JOIN b.refundedBill rb "
                + "LEFT JOIN b.toStaff toStaff "
                + "LEFT JOIN toStaff.person staffPerson "
                + "WHERE b.billType = :bt "
                + "AND b.createdAt BETWEEN :fd AND :td "
                + "AND b.department = :dep "
                + "AND b.retired = false";

        if (searchController.getSearchKeyword().getBillNo() != null
                && !searchController.getSearchKeyword().getBillNo().trim().isEmpty()) {
            sql += " AND b.deptId LIKE :billNo";
            m.put("billNo", "%" + searchController.getSearchKeyword().getBillNo().trim() + "%");
        }
        if (searchController.getSearchKeyword().getDepartment() != null
                && !searchController.getSearchKeyword().getDepartment().trim().isEmpty()) {
            sql += " AND b.fromDepartment.name LIKE :fromDep";
            m.put("fromDep", "%" + searchController.getSearchKeyword().getDepartment().trim() + "%");
        }
        if (searchController.getSearchKeyword().getNetTotal() != null
                && !searchController.getSearchKeyword().getNetTotal().trim().isEmpty()) {
            try {
                sql += " AND b.netTotal = :netTotal";
                m.put("netTotal", Double.parseDouble(searchController.getSearchKeyword().getNetTotal().trim()));
            } catch (NumberFormatException e) {
                // skip invalid input
            }
        }

        sql += " ORDER BY b.createdAt DESC";

        if (maxResult > 0) {
            transferReceiveSearchDtos = (List<com.divudi.core.data.dto.PharmacyTransferReceivedListDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP, maxResult);
        } else {
            transferReceiveSearchDtos = (List<com.divudi.core.data.dto.PharmacyTransferReceivedListDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP);
        }
        if (transferReceiveSearchDtos == null) {
            transferReceiveSearchDtos = new ArrayList<>();
        }
    }

    public List<com.divudi.core.data.dto.PharmacyPreBillSearchDTO> getPreBillSearchDtos() {
        return preBillSearchDtos;
    }

    public void setPreBillSearchDtos(List<com.divudi.core.data.dto.PharmacyPreBillSearchDTO> preBillSearchDtos) {
        this.preBillSearchDtos = preBillSearchDtos;
    }

    @SuppressWarnings("unchecked")
    public void fetchPreBillSearchDtos(int maxResult) {
        Map<String, Object> m = new HashMap<>();
        m.put("fd", searchController.getFromDate());
        m.put("td", searchController.getToDate());
        m.put("dep", sessionController.getLoggedUser().getDepartment());

        // Use PreBill (the actual JPA subtype for pharmacy pre-bills) rather than
        // BilledBill; BilledBill has a different discriminator and returns zero rows
        // for pre-bills. LEFT JOIN referenceBill to avoid an implicit INNER JOIN that
        // would drop unsettled pre-bills (referenceBill is nullable).
        String sql = "SELECT new com.divudi.core.data.dto.PharmacyPreBillSearchDTO("
                + "b.id, refBill.id, b.deptId, b.createdAt, "
                + "b.cancelled, cb.createdAt, "
                + "COALESCE(creatorPerson.name, ''), COALESCE(cancellerPerson.name, ''), '', "
                + "b.billTypeAtomic, b.paymentMethod, b.netTotal) "
                + "FROM PreBill b "
                + "LEFT JOIN b.referenceBill refBill "
                + "LEFT JOIN b.creater creater "
                + "LEFT JOIN creater.webUserPerson creatorPerson "
                + "LEFT JOIN b.cancelledBill cb "
                + "LEFT JOIN cb.creater canceller "
                + "LEFT JOIN canceller.webUserPerson cancellerPerson "
                + "WHERE b.createdAt BETWEEN :fd AND :td "
                + "AND b.department = :dep "
                + "AND b.retired = false";

        if (searchController.getSearchKeyword().getBillNo() != null
                && !searchController.getSearchKeyword().getBillNo().trim().isEmpty()) {
            sql += " AND b.deptId LIKE :billNo";
            m.put("billNo", "%" + searchController.getSearchKeyword().getBillNo().trim() + "%");
        }
        if (searchController.getSearchKeyword().getNetTotal() != null
                && !searchController.getSearchKeyword().getNetTotal().trim().isEmpty()) {
            try {
                sql += " AND b.netTotal = :netTotal";
                m.put("netTotal", Double.parseDouble(searchController.getSearchKeyword().getNetTotal().trim()));
            } catch (NumberFormatException e) {
                // skip invalid input
            }
        }

        sql += " ORDER BY b.createdAt DESC";

        if (maxResult > 0) {
            preBillSearchDtos = (List<com.divudi.core.data.dto.PharmacyPreBillSearchDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP, maxResult);
        } else {
            preBillSearchDtos = (List<com.divudi.core.data.dto.PharmacyPreBillSearchDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP);
        }
        if (preBillSearchDtos == null) {
            preBillSearchDtos = new ArrayList<>();
        }
    }

    public List<com.divudi.core.data.dto.PharmacyWholeSaleSearchDTO> getWholeSaleSearchDtos() {
        return wholeSaleSearchDtos;
    }

    public void setWholeSaleSearchDtos(List<com.divudi.core.data.dto.PharmacyWholeSaleSearchDTO> wholeSaleSearchDtos) {
        this.wholeSaleSearchDtos = wholeSaleSearchDtos;
    }

    public void fetchWholeSaleSearchDtos(int maxResult) {
        Map<String, Object> m = new HashMap<>();
        m.put("bt", com.divudi.core.data.BillType.PharmacyWholeSale);
        m.put("dep", sessionController.getDepartment());
        m.put("fd", searchController.getFromDate());
        m.put("td", searchController.getToDate());
        String sql = "SELECT new com.divudi.core.data.dto.PharmacyWholeSaleSearchDTO("
                + "b.id, b.deptId, b.createdAt, "
                + "COALESCE(creatorPerson.name, ''), "
                + "COALESCE(b.comments, ''), "
                + "b.cancelled) "
                + "FROM BilledBill b "
                + "LEFT JOIN b.creater creater "
                + "LEFT JOIN creater.webUserPerson creatorPerson "
                + "WHERE b.billType = :bt "
                + "AND b.createdAt BETWEEN :fd AND :td "
                + "AND b.department = :dep "
                + "AND b.retired = false";
        if (maxResult > 0) {
            wholeSaleSearchDtos =
                    (List<com.divudi.core.data.dto.PharmacyWholeSaleSearchDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP, maxResult);
        } else {
            wholeSaleSearchDtos =
                    (List<com.divudi.core.data.dto.PharmacyWholeSaleSearchDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP);
        }
        if (wholeSaleSearchDtos == null) {
            wholeSaleSearchDtos = new ArrayList<>();
        }
    }

    public List<com.divudi.core.data.dto.PharmacyPurchaseOrderDTO> getPoRequestSearchDtos() {
        return poRequestSearchDtos;
    }

    public void setPoRequestSearchDtos(List<com.divudi.core.data.dto.PharmacyPurchaseOrderDTO> poRequestSearchDtos) {
        this.poRequestSearchDtos = poRequestSearchDtos;
    }

    public void fetchPoRequestSearchDtos(int maxResult) {
        Map<String, Object> m = new HashMap<>();
        m.put("bt", com.divudi.core.data.BillType.PharmacyOrder);
        m.put("dep", sessionController.getDepartment());
        m.put("fd", searchController.getFromDate());
        m.put("td", searchController.getToDate());
        String sql = "SELECT new com.divudi.core.data.dto.PharmacyPurchaseOrderDTO("
                + "b.id, "
                + "COALESCE(b.insId, ''), "
                + "COALESCE(b.deptId, ''), "
                + "b.createdAt, "
                + "COALESCE(b.department.name, ''), "
                + "COALESCE(b.toInstitution.name, ''), "
                + "b.paymentMethod, "
                + "b.cancelled, "
                + "COALESCE(creatorPerson.name, ''), "
                + "b.netTotal) "
                + "FROM BilledBill b "
                + "LEFT JOIN b.creater creater "
                + "LEFT JOIN creater.webUserPerson creatorPerson "
                + "WHERE b.billType = :bt "
                + "AND b.createdAt BETWEEN :fd AND :td "
                + "AND b.department = :dep "
                + "AND b.retired = false "
                + "ORDER BY b.createdAt DESC";
        if (maxResult > 0) {
            poRequestSearchDtos =
                    (List<com.divudi.core.data.dto.PharmacyPurchaseOrderDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP, maxResult);
        } else {
            poRequestSearchDtos =
                    (List<com.divudi.core.data.dto.PharmacyPurchaseOrderDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP);
        }
        if (poRequestSearchDtos == null) {
            poRequestSearchDtos = new ArrayList<>();
        }
    }

    public List<com.divudi.core.data.dto.PharmacyPurchaseOrderDTO> getPoApproveSearchDtos() {
        return poApproveSearchDtos;
    }

    public void setPoApproveSearchDtos(List<com.divudi.core.data.dto.PharmacyPurchaseOrderDTO> poApproveSearchDtos) {
        this.poApproveSearchDtos = poApproveSearchDtos;
    }

    public void fetchPoApproveSearchDtos(int maxResult) {
        Map<String, Object> m = new HashMap<>();
        m.put("bt", com.divudi.core.data.BillType.PharmacyOrderApprove);
        m.put("dep", sessionController.getDepartment());
        m.put("fd", searchController.getFromDate());
        m.put("td", searchController.getToDate());
        String sql = "SELECT new com.divudi.core.data.dto.PharmacyPurchaseOrderDTO("
                + "b.id, "
                + "COALESCE(b.deptId, ''), "
                + "refBill.id, "
                + "COALESCE(refBill.insId, ''), "
                + "b.createdAt, "
                + "COALESCE(b.department.name, ''), "
                + "COALESCE(creatorPerson.name, ''), "
                + "COALESCE(b.toInstitution.name, ''), "
                + "b.paymentMethod, "
                + "b.cancelled, "
                + "b.netTotal) "
                + "FROM BilledBill b "
                + "LEFT JOIN b.referenceBill refBill "
                + "LEFT JOIN b.creater creater "
                + "LEFT JOIN creater.webUserPerson creatorPerson "
                + "WHERE b.billType = :bt "
                + "AND b.createdAt BETWEEN :fd AND :td "
                + "AND b.department = :dep "
                + "AND b.retired = false "
                + "ORDER BY b.createdAt DESC";
        if (maxResult > 0) {
            poApproveSearchDtos =
                    (List<com.divudi.core.data.dto.PharmacyPurchaseOrderDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP, maxResult);
        } else {
            poApproveSearchDtos =
                    (List<com.divudi.core.data.dto.PharmacyPurchaseOrderDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP);
        }
        if (poApproveSearchDtos == null) {
            poApproveSearchDtos = new ArrayList<>();
        }
    }

    public List<com.divudi.core.data.dto.PharmacyGrnSearchDTO> getGrnSearchDtos() {
        return grnSearchDtos;
    }

    public void setGrnSearchDtos(List<com.divudi.core.data.dto.PharmacyGrnSearchDTO> grnSearchDtos) {
        this.grnSearchDtos = grnSearchDtos;
    }

    public void fetchGrnSearchDtos(int maxResult) {
        Map<String, Object> m = new HashMap<>();
        m.put("bt", com.divudi.core.data.BillType.PharmacyGrnBill);
        m.put("dep", sessionController.getDepartment());
        m.put("fd", searchController.getFromDate());
        m.put("td", searchController.getToDate());
        String sql = "SELECT new com.divudi.core.data.dto.PharmacyGrnSearchDTO("
                + "b.id, "
                + "COALESCE(b.deptId, ''), "
                + "COALESCE(refBill.deptId, ''), "
                + "COALESCE(b.invoiceNumber, ''), "
                + "b.invoiceDate, "
                + "COALESCE(b.fromInstitution.name, ''), "
                + "b.createdAt, "
                + "COALESCE(creatorPerson.name, ''), "
                + "b.paymentMethod, "
                + "b.netTotal, "
                + "b.saleValue, "
                + "b.cancelled) "
                + "FROM BilledBill b "
                + "LEFT JOIN b.referenceBill refBill "
                + "LEFT JOIN b.creater creater "
                + "LEFT JOIN creater.webUserPerson creatorPerson "
                + "WHERE b.billType = :bt "
                + "AND b.createdAt BETWEEN :fd AND :td "
                + "AND b.department = :dep "
                + "AND b.retired = false "
                + "ORDER BY b.createdAt DESC";
        if (maxResult > 0) {
            grnSearchDtos =
                    (List<com.divudi.core.data.dto.PharmacyGrnSearchDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP, maxResult);
        } else {
            grnSearchDtos =
                    (List<com.divudi.core.data.dto.PharmacyGrnSearchDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP);
        }
        if (grnSearchDtos == null) {
            grnSearchDtos = new ArrayList<>();
        }
    }

    public List<com.divudi.core.data.dto.PharmacyDirectPurchaseSearchDTO> getPurchaseSearchDtos() {
        return purchaseSearchDtos;
    }

    public void setPurchaseSearchDtos(List<com.divudi.core.data.dto.PharmacyDirectPurchaseSearchDTO> purchaseSearchDtos) {
        this.purchaseSearchDtos = purchaseSearchDtos;
    }

    /**
     * Fetches PharmacyPurchaseBill bills as lightweight DTOs (issue #21013 / #20299).
     * LEFT JOINs on cancelledBill and refundedBill capture cancel/refund metadata
     * without triggering N+1 lazy loads.
     *
     * @param maxResult maximum rows to return; 0 or negative means unlimited
     */
    @SuppressWarnings("unchecked")
    public void fetchPurchaseSearchDtos(int maxResult) {
        Map<String, Object> m = new HashMap<>();
        m.put("bt", com.divudi.core.data.BillType.PharmacyPurchaseBill);
        m.put("fd", searchController.getFromDate());
        m.put("td", searchController.getToDate());
        m.put("dep", sessionController.getLoggedUser().getDepartment());

        String sql = "SELECT new com.divudi.core.data.dto.PharmacyDirectPurchaseSearchDTO("
                + "b.id, "
                + "COALESCE(b.deptId, ''), "
                + "COALESCE(b.invoiceNumber, ''), "
                + "b.invoiceDate, "
                + "b.createdAt, "
                + "COALESCE(creatorPerson.name, ''), "
                + "COALESCE(b.fromInstitution.name, ''), "
                + "b.paymentMethod, "
                + "b.netTotal, "
                + "b.saleValue, "
                + "b.cancelled, "
                + "cb.createdAt, "
                + "COALESCE(cancellerPerson.name, ''), "
                + "COALESCE(cb.comments, ''), "
                + "b.refunded, "
                + "rb.createdAt, "
                + "COALESCE(refunderPerson.name, ''), "
                + "COALESCE(rb.comments, '')) "
                + "FROM BilledBill b "
                + "LEFT JOIN b.creater creater "
                + "LEFT JOIN creater.webUserPerson creatorPerson "
                + "LEFT JOIN b.cancelledBill cb "
                + "LEFT JOIN cb.creater canceller "
                + "LEFT JOIN canceller.webUserPerson cancellerPerson "
                + "LEFT JOIN b.refundedBill rb "
                + "LEFT JOIN rb.creater refunder "
                + "LEFT JOIN refunder.webUserPerson refunderPerson "
                + "WHERE b.billType = :bt "
                + "AND b.createdAt BETWEEN :fd AND :td "
                + "AND b.department = :dep "
                + "AND b.retired = false "
                + "ORDER BY b.createdAt DESC";

        if (maxResult > 0) {
            purchaseSearchDtos =
                    (List<com.divudi.core.data.dto.PharmacyDirectPurchaseSearchDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP, maxResult);
        } else {
            purchaseSearchDtos =
                    (List<com.divudi.core.data.dto.PharmacyDirectPurchaseSearchDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP);
        }
        if (purchaseSearchDtos == null) {
            purchaseSearchDtos = new ArrayList<>();
        }
    }

    public List<com.divudi.core.data.dto.PharmacyGrnReturnSearchDTO> getGrnReturnSearchDtos() {
        return grnReturnSearchDtos;
    }

    public void setGrnReturnSearchDtos(List<com.divudi.core.data.dto.PharmacyGrnReturnSearchDTO> grnReturnSearchDtos) {
        this.grnReturnSearchDtos = grnReturnSearchDtos;
    }

    @SuppressWarnings("unchecked")
    public void fetchGrnReturnSearchDtos(int maxResult) {
        Map<String, Object> m = new HashMap<>();
        m.put("bt", com.divudi.core.data.BillType.PharmacyGrnReturn);
        m.put("fd", searchController.getFromDate());
        m.put("td", searchController.getToDate());
        m.put("dep", sessionController.getDepartment());
        String sql = "SELECT new com.divudi.core.data.dto.PharmacyGrnReturnSearchDTO("
                + "b.id, COALESCE(b.deptId, ''), COALESCE(b.referenceBill.deptId, ''), "
                + "COALESCE(b.toInstitution.name, ''), b.createdAt, COALESCE(creatorPerson.name, ''), "
                + "b.cancelled, cb.createdAt, COALESCE(cancellerPerson.name, ''), "
                + "b.refunded, rb.createdAt, COALESCE(refunderPerson.name, ''), "
                + "COALESCE(cb.comments, rb.comments, ''), b.paymentMethod, "
                + "b.netTotal, b.saleValue) "
                + "FROM BilledBill b "
                + "LEFT JOIN b.creater creater LEFT JOIN creater.webUserPerson creatorPerson "
                + "LEFT JOIN b.cancelledBill cb LEFT JOIN cb.creater canceller "
                + "LEFT JOIN canceller.webUserPerson cancellerPerson "
                + "LEFT JOIN b.refundedBill rb LEFT JOIN rb.creater refunder "
                + "LEFT JOIN refunder.webUserPerson refunderPerson "
                + "WHERE b.billType = :bt AND b.createdAt BETWEEN :fd AND :td "
                + "AND b.department = :dep AND b.retired = false ORDER BY b.createdAt DESC";
        if (maxResult > 0) {
            grnReturnSearchDtos = (List<com.divudi.core.data.dto.PharmacyGrnReturnSearchDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP, maxResult);
        } else {
            grnReturnSearchDtos = (List<com.divudi.core.data.dto.PharmacyGrnReturnSearchDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP);
        }
        if (grnReturnSearchDtos == null) {
            grnReturnSearchDtos = new ArrayList<>();
        }
    }

    public List<com.divudi.core.data.dto.PharmacyReturnWithoutTraisingSearchDTO> getReturnWithoutTraisingSearchDtos() {
        return returnWithoutTraisingSearchDtos;
    }

    public void setReturnWithoutTraisingSearchDtos(List<com.divudi.core.data.dto.PharmacyReturnWithoutTraisingSearchDTO> returnWithoutTraisingSearchDtos) {
        this.returnWithoutTraisingSearchDtos = returnWithoutTraisingSearchDtos;
    }

    @SuppressWarnings("unchecked")
    public void fetchReturnWithoutTraisingSearchDtos(int maxResult) {
        Map<String, Object> m = new HashMap<>();
        m.put("bt", com.divudi.core.data.BillType.PharmacyReturnWithoutTraising);
        m.put("fd", searchController.getFromDate());
        m.put("td", searchController.getToDate());
        m.put("dep", sessionController.getDepartment());
        String sql = "SELECT new com.divudi.core.data.dto.PharmacyReturnWithoutTraisingSearchDTO("
                + "b.id, COALESCE(b.deptId, ''), COALESCE(b.toInstitution.name, ''), "
                + "b.createdAt, COALESCE(creatorPerson.name, ''), "
                + "b.cancelled, cb.createdAt, COALESCE(cancellerPerson.name, ''), "
                + "b.refunded, rb.createdAt, COALESCE(refunderPerson.name, ''), "
                + "COALESCE(cb.comments, rb.comments, ''), "
                + "b.netTotal, b.pharmacyBill.saleValue, COALESCE(b.comments, '')) "
                + "FROM PreBill b "
                + "LEFT JOIN b.creater creater LEFT JOIN creater.webUserPerson creatorPerson "
                + "LEFT JOIN b.cancelledBill cb LEFT JOIN cb.creater canceller "
                + "LEFT JOIN canceller.webUserPerson cancellerPerson "
                + "LEFT JOIN b.refundedBill rb LEFT JOIN rb.creater refunder "
                + "LEFT JOIN refunder.webUserPerson refunderPerson "
                + "WHERE b.billType = :bt AND b.createdAt BETWEEN :fd AND :td "
                + "AND b.department = :dep AND b.retired = false ORDER BY b.createdAt DESC";
        if (maxResult > 0) {
            returnWithoutTraisingSearchDtos = (List<com.divudi.core.data.dto.PharmacyReturnWithoutTraisingSearchDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP, maxResult);
        } else {
            returnWithoutTraisingSearchDtos = (List<com.divudi.core.data.dto.PharmacyReturnWithoutTraisingSearchDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP);
        }
        if (returnWithoutTraisingSearchDtos == null) {
            returnWithoutTraisingSearchDtos = new ArrayList<>();
        }
    }

    public List<com.divudi.core.data.dto.PharmacyIssueSearchDTO> getIssueSearchDtos() {
        return issueSearchDtos;
    }

    public void setIssueSearchDtos(List<com.divudi.core.data.dto.PharmacyIssueSearchDTO> issueSearchDtos) {
        this.issueSearchDtos = issueSearchDtos;
    }

    public List<com.divudi.core.data.dto.PharmacyAdjustmentSearchDTO> getAdjustmentSearchDtos() {
        return adjustmentSearchDtos;
    }

    public void setAdjustmentSearchDtos(List<com.divudi.core.data.dto.PharmacyAdjustmentSearchDTO> adjustmentSearchDtos) {
        this.adjustmentSearchDtos = adjustmentSearchDtos;
    }

    @SuppressWarnings("unchecked")
    public void fetchAdjustmentSearchDtos(int maxResult) {
        Map<String, Object> m = new HashMap<>();
        m.put("billTypeAtomics", java.util.Arrays.asList(
                com.divudi.core.data.BillTypeAtomic.PHARMACY_ADJUSTMENT,
                com.divudi.core.data.BillTypeAtomic.PHARMACY_ADJUSTMENT_CANCELLED,
                com.divudi.core.data.BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT,
                com.divudi.core.data.BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT_BILL,
                com.divudi.core.data.BillTypeAtomic.PHARMACY_STAFF_STOCK_ADJUSTMENT,
                com.divudi.core.data.BillTypeAtomic.PHARMACY_PURCHASE_RATE_ADJUSTMENT,
                com.divudi.core.data.BillTypeAtomic.PHARMACY_RETAIL_RATE_ADJUSTMENT,
                com.divudi.core.data.BillTypeAtomic.PHARMACY_WHOLESALE_RATE_ADJUSTMENT,
                com.divudi.core.data.BillTypeAtomic.PHARMACY_COST_RATE_ADJUSTMENT,
                com.divudi.core.data.BillTypeAtomic.PHARMACY_STOCK_EXPIRY_DATE_AJUSTMENT
        ));
        m.put("fd", searchController.getFromDate());
        m.put("td", searchController.getToDate());
        m.put("dep", sessionController.getDepartment());
        String sql = "SELECT new com.divudi.core.data.dto.PharmacyAdjustmentSearchDTO("
                + "b.id, COALESCE(b.deptId, ''), b.billTypeAtomic, "
                + "COALESCE(b.department.name, ''), b.createdAt, COALESCE(creatorPerson.name, ''), "
                + "COALESCE(b.comments, ''), b.netTotal, "
                + "b.cancelled, cb.createdAt, COALESCE(cancellerPerson.name, ''), "
                + "COALESCE(cb.comments, '')) "
                + "FROM BilledBill b "
                + "LEFT JOIN b.creater creater LEFT JOIN creater.webUserPerson creatorPerson "
                + "LEFT JOIN b.cancelledBill cb LEFT JOIN cb.creater canceller "
                + "LEFT JOIN canceller.webUserPerson cancellerPerson "
                + "WHERE b.billTypeAtomic IN :billTypeAtomics AND b.createdAt BETWEEN :fd AND :td "
                + "AND b.department = :dep AND b.retired = false ORDER BY b.createdAt DESC";
        if (maxResult > 0) {
            adjustmentSearchDtos = (List<com.divudi.core.data.dto.PharmacyAdjustmentSearchDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP, maxResult);
        } else {
            adjustmentSearchDtos = (List<com.divudi.core.data.dto.PharmacyAdjustmentSearchDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP);
        }
        if (adjustmentSearchDtos == null) {
            adjustmentSearchDtos = new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    public void fetchIssueSearchDtos(int maxResult) {
        Map<String, Object> m = new HashMap<>();
        m.put("bt", com.divudi.core.data.BillType.PharmacyIssue);
        m.put("fd", searchController.getFromDate());
        m.put("td", searchController.getToDate());
        m.put("dep", sessionController.getDepartment());
        String sql = "SELECT new com.divudi.core.data.dto.PharmacyIssueSearchDTO("
                + "b.id, COALESCE(b.deptId, ''), COALESCE(b.insId, ''), "
                + "COALESCE(b.toDepartment.name, ''), b.createdAt, COALESCE(creatorPerson.name, ''), "
                + "b.cancelled, cb.createdAt, COALESCE(cancellerPerson.name, ''), "
                + "COALESCE(cb.comments, ''), b.netTotal) "
                + "FROM BilledBill b "
                + "LEFT JOIN b.creater creater LEFT JOIN creater.webUserPerson creatorPerson "
                + "LEFT JOIN b.cancelledBill cb LEFT JOIN cb.creater canceller "
                + "LEFT JOIN canceller.webUserPerson cancellerPerson "
                + "WHERE b.billType = :bt AND b.createdAt BETWEEN :fd AND :td "
                + "AND b.department = :dep AND b.retired = false ORDER BY b.createdAt DESC";
        if (maxResult > 0) {
            issueSearchDtos = (List<com.divudi.core.data.dto.PharmacyIssueSearchDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP, maxResult);
        } else {
            issueSearchDtos = (List<com.divudi.core.data.dto.PharmacyIssueSearchDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP);
        }
        if (issueSearchDtos == null) {
            issueSearchDtos = new ArrayList<>();
        }
    }

    public List<com.divudi.core.data.dto.PharmacyGrnPaymentSearchDTO> getGrnPaymentSearchDtos() {
        return grnPaymentSearchDtos;
    }

    public void setGrnPaymentSearchDtos(List<com.divudi.core.data.dto.PharmacyGrnPaymentSearchDTO> grnPaymentSearchDtos) {
        this.grnPaymentSearchDtos = grnPaymentSearchDtos;
    }

    /**
     * Fetches GrnPaymentPre bills as lightweight DTOs (issue #20523).
     * LEFT JOINs on referenceBill (GRN), cancelledBill and refundedBill capture
     * GRN/cancel/refund metadata without triggering N+1 lazy loads.
     *
     * @param maxResult maximum rows to return; 0 or negative means unlimited
     */
    @SuppressWarnings("unchecked")
    public void fetchGrnPaymentSearchDtos(int maxResult) {
        Map<String, Object> m = new HashMap<>();
        m.put("bt", com.divudi.core.data.BillType.GrnPaymentPre);
        m.put("fd", searchController.getFromDate());
        m.put("td", searchController.getToDate());
        m.put("dep", sessionController.getDepartment());
        String sql = "SELECT new com.divudi.core.data.dto.PharmacyGrnPaymentSearchDTO("
                + "b.id, COALESCE(b.deptId, ''), COALESCE(refBill.deptId, ''), refBill.createdAt, "
                + "b.billTime, COALESCE(b.toInstitution.name, ''), b.createdAt, COALESCE(creatorPerson.name, ''), "
                + "b.cancelled, cb.createdAt, COALESCE(cancellerPerson.name, ''), "
                + "b.refunded, rb.createdAt, COALESCE(refunderPerson.name, ''), "
                + "COALESCE(cb.comments, rb.comments, ''), b.paymentMethod, "
                + "b.netTotal, b.saleValue) "
                + "FROM BilledBill b "
                + "LEFT JOIN b.referenceBill refBill "
                + "LEFT JOIN b.creater creater LEFT JOIN creater.webUserPerson creatorPerson "
                + "LEFT JOIN b.cancelledBill cb LEFT JOIN cb.creater canceller "
                + "LEFT JOIN canceller.webUserPerson cancellerPerson "
                + "LEFT JOIN b.refundedBill rb LEFT JOIN rb.creater refunder "
                + "LEFT JOIN refunder.webUserPerson refunderPerson "
                + "WHERE b.billType = :bt AND b.createdAt BETWEEN :fd AND :td "
                + "AND b.department = :dep AND b.retired = false";

        // billNo filter: this page's "GRN No" input is bound to searchKeyword.billNo
        // but historically filters the bill's own deptId (matches the generic
        // fallback JPQL semantics for BillType.GrnPaymentPre).
        if (searchController.getSearchKeyword().getBillNo() != null
                && !searchController.getSearchKeyword().getBillNo().trim().isEmpty()) {
            sql += " AND (b.deptId) LIKE :billNo";
            m.put("billNo", "%" + searchController.getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }
        // refBillNo filter ("PO No" input) filters the referenced GRN bill's deptId.
        if (searchController.getSearchKeyword().getRefBillNo() != null
                && !searchController.getSearchKeyword().getRefBillNo().trim().isEmpty()) {
            sql += " AND (refBill.deptId) LIKE :refBillNo";
            m.put("refBillNo", "%" + searchController.getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
        }
        // number filter ("Invoice No" input) filters the bill's own invoice number.
        if (searchController.getSearchKeyword().getNumber() != null
                && !searchController.getSearchKeyword().getNumber().trim().isEmpty()) {
            sql += " AND (b.invoiceNumber) LIKE :invNo";
            m.put("invNo", "%" + searchController.getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }
        // fromInstitution filter ("Supplier Name" input) filters b.toInstitution.name
        // (the dealer being paid), matching this composite's existing field binding.
        if (searchController.getSearchKeyword().getFromInstitution() != null
                && !searchController.getSearchKeyword().getFromInstitution().trim().isEmpty()) {
            sql += " AND UPPER(b.toInstitution.name) LIKE :toIns";
            m.put("toIns", "%" + searchController.getSearchKeyword().getFromInstitution().trim().toUpperCase() + "%");
        }
        if (searchController.getSearchKeyword().getNetTotal() != null
                && !searchController.getSearchKeyword().getNetTotal().trim().isEmpty()) {
            try {
                double netTotal = Double.parseDouble(searchController.getSearchKeyword().getNetTotal().trim());
                sql += " AND b.netTotal = :netTotal";
                m.put("netTotal", netTotal);
            } catch (NumberFormatException e) {
                // ignore invalid numeric input, skip filter
            }
        }
        if (searchController.getSearchKeyword().getItemName() != null
                && !searchController.getSearchKeyword().getItemName().trim().isEmpty()) {
            sql += " AND b.id IN (SELECT bItem.bill.id FROM BillItem bItem "
                    + "WHERE bItem.retired = false AND bItem.item.name LIKE :itm)";
            m.put("itm", "%" + searchController.getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }
        if (searchController.getSearchKeyword().getCode() != null
                && !searchController.getSearchKeyword().getCode().trim().isEmpty()) {
            sql += " AND b.id IN (SELECT bItem.bill.id FROM BillItem bItem "
                    + "WHERE bItem.retired = false AND bItem.item.code LIKE :cde)";
            m.put("cde", "%" + searchController.getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " ORDER BY b.createdAt DESC";
        if (maxResult > 0) {
            grnPaymentSearchDtos = (List<com.divudi.core.data.dto.PharmacyGrnPaymentSearchDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP, maxResult);
        } else {
            grnPaymentSearchDtos = (List<com.divudi.core.data.dto.PharmacyGrnPaymentSearchDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP);
        }
        if (grnPaymentSearchDtos == null) {
            grnPaymentSearchDtos = new ArrayList<>();
        }
    }

    public List<com.divudi.core.data.dto.PharmacyPurchaseReturnSearchDTO> getPurchaseReturnSearchDtos() {
        return purchaseReturnSearchDtos;
    }

    public void setPurchaseReturnSearchDtos(List<com.divudi.core.data.dto.PharmacyPurchaseReturnSearchDTO> purchaseReturnSearchDtos) {
        this.purchaseReturnSearchDtos = purchaseReturnSearchDtos;
    }

    /**
     * Fetches PurchaseReturn bills as lightweight DTOs (issue #20523).
     * LEFT JOINs on referenceBill (original purchase), cancelledBill and
     * refundedBill capture purchase/cancel/refund metadata without triggering
     * N+1 lazy loads.
     *
     * @param maxResult maximum rows to return; 0 or negative means unlimited
     */
    @SuppressWarnings("unchecked")
    public void fetchPurchaseReturnSearchDtos(int maxResult) {
        Map<String, Object> m = new HashMap<>();
        m.put("bt", com.divudi.core.data.BillType.PurchaseReturn);
        m.put("fd", searchController.getFromDate());
        m.put("td", searchController.getToDate());
        m.put("dep", sessionController.getDepartment());
        String sql = "SELECT new com.divudi.core.data.dto.PharmacyPurchaseReturnSearchDTO("
                + "b.id, refBill.id, COALESCE(b.deptId, ''), COALESCE(refBill.deptId, ''), "
                + "COALESCE(b.toInstitution.name, ''), b.createdAt, COALESCE(creatorPerson.name, ''), "
                + "b.cancelled, cb.createdAt, COALESCE(cancellerPerson.name, ''), "
                + "b.refunded, rb.createdAt, COALESCE(refunderPerson.name, ''), "
                + "COALESCE(cb.comments, rb.comments, ''), b.paymentMethod, "
                + "b.netTotal, b.saleValue) "
                + "FROM BilledBill b "
                + "LEFT JOIN b.referenceBill refBill "
                + "LEFT JOIN b.creater creater LEFT JOIN creater.webUserPerson creatorPerson "
                + "LEFT JOIN b.cancelledBill cb LEFT JOIN cb.creater canceller "
                + "LEFT JOIN canceller.webUserPerson cancellerPerson "
                + "LEFT JOIN b.refundedBill rb LEFT JOIN rb.creater refunder "
                + "LEFT JOIN refunder.webUserPerson refunderPerson "
                + "WHERE b.billType = :bt AND b.createdAt BETWEEN :fd AND :td "
                + "AND b.department = :dep AND b.retired = false";

        // billNo filter ("Return Note No" input) filters the bill's own deptId.
        if (searchController.getSearchKeyword().getBillNo() != null
                && !searchController.getSearchKeyword().getBillNo().trim().isEmpty()) {
            sql += " AND (b.deptId) LIKE :billNo";
            m.put("billNo", "%" + searchController.getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }
        // refBillNo filter ("Purchase No" input) filters the referenced purchase bill's deptId.
        if (searchController.getSearchKeyword().getRefBillNo() != null
                && !searchController.getSearchKeyword().getRefBillNo().trim().isEmpty()) {
            sql += " AND (refBill.deptId) LIKE :refBillNo";
            m.put("refBillNo", "%" + searchController.getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
        }
        // toInstitution filter ("Supplier Name" input) filters b.toInstitution.name.
        if (searchController.getSearchKeyword().getToInstitution() != null
                && !searchController.getSearchKeyword().getToInstitution().trim().isEmpty()) {
            sql += " AND UPPER(b.toInstitution.name) LIKE :toIns";
            m.put("toIns", "%" + searchController.getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }
        if (searchController.getSearchKeyword().getNetTotal() != null
                && !searchController.getSearchKeyword().getNetTotal().trim().isEmpty()) {
            try {
                double netTotal = Double.parseDouble(searchController.getSearchKeyword().getNetTotal().trim());
                sql += " AND b.netTotal = :netTotal";
                m.put("netTotal", netTotal);
            } catch (NumberFormatException e) {
                // ignore invalid numeric input, skip filter
            }
        }
        if (searchController.getSearchKeyword().getItemName() != null
                && !searchController.getSearchKeyword().getItemName().trim().isEmpty()) {
            sql += " AND b.id IN (SELECT bItem.bill.id FROM BillItem bItem "
                    + "WHERE bItem.retired = false AND bItem.item.name LIKE :itm)";
            m.put("itm", "%" + searchController.getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }
        if (searchController.getSearchKeyword().getCode() != null
                && !searchController.getSearchKeyword().getCode().trim().isEmpty()) {
            sql += " AND b.id IN (SELECT bItem.bill.id FROM BillItem bItem "
                    + "WHERE bItem.retired = false AND bItem.item.code LIKE :cde)";
            m.put("cde", "%" + searchController.getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " ORDER BY b.createdAt DESC";
        if (maxResult > 0) {
            purchaseReturnSearchDtos = (List<com.divudi.core.data.dto.PharmacyPurchaseReturnSearchDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP, maxResult);
        } else {
            purchaseReturnSearchDtos = (List<com.divudi.core.data.dto.PharmacyPurchaseReturnSearchDTO>)
                    billFacade.findLightsByJpql(sql, m, TemporalType.TIMESTAMP);
        }
        if (purchaseReturnSearchDtos == null) {
            purchaseReturnSearchDtos = new ArrayList<>();
        }
    }

}
