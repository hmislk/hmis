package com.divudi.bean.pharmacy;

// <editor-fold defaultstate="collapsed" desc="Import Statements">
import com.divudi.bean.common.*;
import com.divudi.bean.cashTransaction.CashBookEntryController;
import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.cashTransaction.DrawerEntryController;
import com.divudi.bean.channel.ChannelSearchController;
import com.divudi.bean.channel.analytics.ReportTemplateController;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.data.hr.ReportKeyWord;

import com.divudi.ejb.PharmacyBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.Staff;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.StockFacade;
import com.divudi.bean.opd.OpdBillController;
import com.divudi.data.BillClassType;

import com.divudi.data.BillTypeAtomic;
import com.divudi.data.ReportTemplateRowBundle;
import com.divudi.entity.Category;
import com.divudi.entity.Payment;
import com.divudi.entity.WebUser;
import com.divudi.facade.DrawerFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.java.CommonFunctions;
import com.divudi.light.common.BillLight;
import com.divudi.light.common.BillSummaryRow;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.file.UploadedFile;

import org.primefaces.model.StreamedContent;
// </editor-fold>  

/**
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class PharmacySummaryReportController implements Serializable {

    private static final long serialVersionUID = 1L;

// <editor-fold defaultstate="collapsed" desc="EJBs">
    private CommonFunctions commonFunctions;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private StockFacade stockFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private DrawerFacade drawerFacade;

// </editor-fold>  
// <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    private BillBeanController billBean;
    @Inject
    private SessionController sessionController;
    @Inject
    TransferController transferController;
    @Inject
    private CommonController commonController;
    @Inject
    private PharmacySaleBhtController pharmacySaleBhtController;
    @Inject
    private WebUserController webUserController;
    @Inject
    private OpdPreSettleController opdPreSettleController;
    @Inject
    private PharmacyPreSettleController pharmacyPreSettleController;
    @Inject
    private TokenController tokenController;
    @Inject
    private DepartmentController departmentController;
    @Inject
    private BillSearch billSearch;
    @Inject
    private PharmacyBillSearch pharmacyBillSearch;
    @Inject
    private OpdBillController opdBillController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private ChannelSearchController channelSearchController;
    @Inject
    private ReportTemplateController reportTemplateController;
    @Inject
    private CashBookEntryController cashBookEntryController;
    @Inject
    private ExcelController excelController;
    @Inject
    private PdfController pdfController;
    @Inject
    private DrawerEntryController drawerEntryController;
    @Inject
    private DrawerController drawerController;
    @Inject
    private EnumController enumController;
// </editor-fold>  
// <editor-fold defaultstate="collapsed" desc="Class Variables">
    // Basic types
    private String visitType;
    private String methodType;
    private String searchType;
    private String reportType;

    // Date range
    private Date fromDate;
    private Date toDate;

    // Enum and category types
    private Category category;
    private BillType billType;
    private BillTypeAtomic billTypeAtomic;
    private BillClassType billClassType;
    private PaymentMethod paymentMethod;

    // Collections
    private List<PaymentMethod> paymentMethods;

    // User-related
    private WebUser webUser;
    private Staff staff;
    private Staff currentStaff;

    // Institutional and departmental entities
    private Institution institution;
    private Institution creditCompany;
    private Institution dealer;
    private Institution site;
    private Institution toSite;
    private Institution fromInstitution;
    private Institution toInstitution;

    private Department department;
    private Department fromDepartment;
    private Department toDepartment;

    // Healthcare-specific entities
    private Patient patient;
    private PatientEncounter patientEncounter;
    private Item item;

    // Reporting and files
    private SearchKeyword searchKeyword;
    private ReportKeyWord reportKeyWord;
    private ReportTemplateRowBundle bundle;

    private StreamedContent downloadingExcel;
    private UploadedFile file;

    // Numeric variables
    private int maxResult = 50;

// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Navigators">
    public String navigateToPharmacyIncomeReport() {
        return "/pharmacy/reports/pharmacy_income_report?faces-redirect=true";
    }

    public String navigateToDailyStockBalanceReport() {
        return "/pharmacy/reports/daily_stock_balance_report?faces-redirect=true";
    }
// </editor-fold>  
// <editor-fold defaultstate="collapsed" desc="Functions">

    public void resetAllFiltersExceptDateRange() {
        institution = null;
        department = null;
        webUser = null;
        site = null;
        paymentMethod = null;
        searchKeyword = null;
        category = null;
        item = null;
        staff = null;
        currentStaff = null;
        creditCompany = null;
        dealer = null;
        toSite = null;
        fromInstitution = null;
        toInstitution = null;
        fromDepartment = null;
        toDepartment = null;
        patient = null;
        patientEncounter = null;
        billType = null;
        billTypeAtomic = null;
        billClassType = null;
        paymentMethods = null;
        reportKeyWord = null;
        bundle = new ReportTemplateRowBundle();
        downloadingExcel = null;
        file = null;
        searchKeyword = new SearchKeyword();
    }

    public void resetAllFilters() {
        resetAllFiltersExceptDateRange();
        fromDate = null;
        toDate = null;
    }

// </editor-fold>  
// <editor-fold defaultstate="collapsed" desc="Constructors">
    public PharmacySummaryReportController() {
    }
// </editor-fold>  
// <editor-fold defaultstate="collapsed" desc="Getters and Setters">

// </editor-fold>  
}
