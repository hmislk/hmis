package com.divudi.bean.opd;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.ItemLight;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.BillFeeBundleEntry;
import com.divudi.core.data.dto.OpdQuotationRowDto;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.OpdQuotation;
import com.divudi.core.entity.OpdQuotationItem;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.facade.OpdQuotationFacade;
import com.divudi.core.facade.OpdQuotationItemFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.OpdQuotationException;
import com.divudi.service.OpdQuotationService;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * OPD quotations (#25885): build and save a numbered quotation of service
 * prices, search and reprint it, and open the normal OPD billing screen with
 * its services filled in. A quotation is not a bill and has no stock or money
 * effect.
 *
 * Prices come from the same lookups the OPD bill uses (fees of the item, the
 * price matrix for payment method and scheme, and the fee rules in
 * {@code BillBeanController}); nothing here has its own price rules. Every
 * action re-checks the privilege on the server.
 */
@Named
@SessionScoped
public class OpdQuotationController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String PRIVILEGE = "OpdQuotationCreate";

    @EJB
    private OpdQuotationService quotationService;
    @EJB
    private OpdQuotationFacade quotationFacade;
    @EJB
    private OpdQuotationItemFacade quotationItemFacade;
    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;
    @Inject
    private BillBeanController billBean;
    @Inject
    private PriceMatrixController priceMatrixController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private ItemController itemController;
    @Inject
    private OpdBillController opdBillController;

    // Create page
    private OpdQuotation draft;
    private List<OpdQuotationItem> lines = new ArrayList<>();
    private ItemLight itemLight;
    private Double quantity;

    // Search page
    private Date fromDate;
    private Date toDate;
    private String quotationNoFilter;
    private String keywordFilter;
    private List<OpdQuotationRowDto> rows = new ArrayList<>();

    // View / print page
    private OpdQuotation quotation;
    /** Id of the quotation the page was rendered for; posted back with every action (the bean is shared by browser tabs). */
    private Long quotationIdOnPage;
    private List<OpdQuotationItem> quotationItems = new ArrayList<>();

    public boolean isAllowed() {
        return webUserController.hasPrivilege(PRIVILEGE);
    }

    private boolean deny(String privilege) {
        if (webUserController.hasPrivilege(privilege)) {
            return false;
        }
        JsfUtil.addErrorMessage("You do not have the privilege for this action.");
        return true;
    }

    // ------------------------------------------------------------------ navigation

    public String navigateToNewQuotation() {
        if (deny(PRIVILEGE)) {
            return null;
        }
        draft = new OpdQuotation();
        draft.setPaymentMethod(PaymentMethod.Cash);
        long days = configOptionApplicationController.getLongValueByKey("OPD Quotation - Validity Days", 30L);
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, (int) days);
        draft.setValidUntil(c.getTime());
        lines = new ArrayList<>();
        itemLight = null;
        quantity = null;
        return "/opd/opd_quotation?faces-redirect=true";
    }

    public String navigateToSearchQuotations() {
        if (deny(PRIVILEGE)) {
            return null;
        }
        fromDate = CommonFunctions.getStartOfMonth();
        toDate = CommonFunctions.getEndOfDay();
        quotationNoFilter = null;
        keywordFilter = null;
        rows = new ArrayList<>();
        return "/opd/opd_quotation_search?faces-redirect=true";
    }

    // ------------------------------------------------------------------ create page

    /** Fills the name and phone from the chosen patient and prices again (the patient may be a foreigner). */
    public void patientSelected() {
        Patient p = draft.getPatient();
        if (p != null && p.getPerson() != null) {
            draft.setCustomerName(p.getPerson().getName());
            String phone = p.getPerson().getMobile();
            if (phone == null || phone.trim().isEmpty()) {
                phone = p.getPerson().getPhone();
            }
            draft.setCustomerPhone(phone);
            draft.setForeigner(p.getPerson().isForeigner());
        }
        reprice();
    }

    public void addItem() {
        if (deny(PRIVILEGE)) {
            return;
        }
        if (itemLight == null || itemLight.getId() == null) {
            JsfUtil.addErrorMessage("Please select a service");
            return;
        }
        Item item = itemController.findItem(itemLight.getId());
        if (item == null) {
            JsfUtil.addErrorMessage("Service not found");
            return;
        }
        if (item.getDepartment() == null) {
            JsfUtil.addErrorMessage("Please set Department to Item");
            return;
        }
        if (item.getCategory() == null) {
            JsfUtil.addErrorMessage("Please set Category to Item");
            return;
        }
        double qty = 1.0;
        if (item.isRequestForQuentity()) {
            if (quantity == null || quantity <= 0) {
                JsfUtil.addErrorMessage("Quantity is missing");
                return;
            }
            if (quantity % 1 != 0) {
                JsfUtil.addErrorMessage("Quantity cannot be a decimal value. Please enter a whole number.");
                return;
            }
            qty = quantity;
        }
        for (OpdQuotationItem l : lines) {
            if (l.getItem() != null && l.getItem().equals(item)) {
                JsfUtil.addErrorMessage("Can't select same item " + item.getName());
                return;
            }
        }
        OpdQuotationItem line = new OpdQuotationItem();
        line.setItem(item);
        line.setItemName(item.getName());
        line.setQuantity(qty);
        if (!priceLine(line)) {
            JsfUtil.addErrorMessage("Item Fees is Missing ..! ");
            return;
        }
        lines.add(line);
        itemLight = null;
        quantity = null;
        totals();
    }

    public void removeLine(OpdQuotationItem line) {
        lines.remove(line);
        totals();
    }

    /** Prices every line again, after the payment method, scheme, credit company or foreigner flag changed. */
    public void reprice() {
        List<OpdQuotationItem> unpriced = new ArrayList<>();
        for (OpdQuotationItem l : lines) {
            if (!priceLine(l)) {
                unpriced.add(l);
            }
        }
        if (!unpriced.isEmpty()) {
            lines.removeAll(unpriced);
            JsfUtil.addErrorMessage("Removed services without fees");
        }
        totals();
    }

    public void paymentMethodChanged() {
        if (draft.getPaymentMethod() != PaymentMethod.Credit) {
            draft.setCreditCompany(null);
        }
        reprice();
    }

    private void totals() {
        double gross = 0;
        double dis = 0;
        double net = 0;
        for (OpdQuotationItem l : lines) {
            gross += l.getGrossValue();
            dis += l.getDiscount();
            net += l.getNetValue();
        }
        draft.setGrossTotal(gross);
        draft.setDiscount(dis);
        draft.setNetTotal(net);
    }

    /**
     * Prices one line the way the OPD bill does when a service is added: the
     * same fee lookup (same config keys), the same price-matrix lookup, the same
     * fee calculation, the same selected fee per fee name and the same VAT and
     * doctor-collected-fee handling. The line amount includes VAT.
     *
     * @return false when the item has no fees
     */
    private boolean priceLine(OpdQuotationItem line) {
        Item item = line.getItem();
        PaymentMethod pm = draft.getPaymentMethod() == null ? PaymentMethod.Cash : draft.getPaymentMethod();
        PaymentScheme scheme = draft.getPaymentScheme();
        boolean foreign = draft.isForeigner();

        BillItem bi = new BillItem();
        bi.setItem(item);
        bi.setQty(line.getQuantity());

        Department dept = sessionController.getDepartment();
        String deptName = dept == null ? "" : dept.getName();
        boolean addAllBillFees = configOptionApplicationController.getBooleanValueByKey("OPD Bill Fees are the same for all departments, institutions and sites for " + deptName, true);
        boolean siteBasedBillFees = configOptionApplicationController.getBooleanValueByKey("OPD Bill Fees are based on the site for " + deptName, false);
        boolean departmentBasedBillFees = configOptionApplicationController.getBooleanValueByKey("OPD Bill Fees are based on the department for " + deptName, false);
        List<BillFee> allBillFees;
        if (addAllBillFees) {
            allBillFees = billBean.billFeefromBillItem(bi);
        } else if (siteBasedBillFees) {
            allBillFees = billBean.forInstitutionBillFeesFromBillItem(bi, dept.getSite());
        } else if (departmentBasedBillFees) {
            allBillFees = billBean.forDepartmentBillFeefromBillItem(bi, dept);
        } else {
            allBillFees = billBean.billFeefromBillItem(bi);
        }
        if (allBillFees == null || allBillFees.isEmpty()) {
            return false;
        }
        List<BillFeeBundleEntry> bundles = billBean.bundleFeesByName(allBillFees);
        if (bundles == null || bundles.isEmpty()) {
            return false;
        }

        double gross = 0;
        double dis = 0;
        double net = 0;
        double vat = 0;
        for (BillFeeBundleEntry bundle : bundles) {
            BillFee bf = bundle.getSelectedBillFee();
            if (bf == null || bf.getFee() == null) {
                continue;
            }
            PriceMatrix matrix;
            Department feeDepartment = item.getDepartment();
            if (pm == PaymentMethod.Credit) {
                matrix = draft.getCreditCompany() == null ? null
                        : priceMatrixController.getPaymentSchemeDiscount(pm, scheme, draft.getCreditCompany(), item);
            } else {
                matrix = priceMatrixController.getPaymentSchemeDiscount(pm, scheme, feeDepartment, item);
            }
            // The OPD bill passes no institution here while pricing, so a quotation does the same.
            billBean.setBillFees(bf, foreign, pm, scheme, (Institution) null, matrix);
            if (item.isVatable()) {
                bf.setFeeVat(roundOff(bf.getFeeValue() * item.getVatPercentage() / 100));
            }
            if (bf.isCollectedByDoctorAtBilling()) {
                continue;
            }
            gross += bf.getFeeGrossValue();
            dis += bf.getFeeDiscount();
            net += bf.getFeeValue();
            vat += bf.getFeeVat();
        }
        line.setGrossValue(gross);
        line.setDiscount(dis);
        line.setNetValue(roundOff(net + vat));
        return true;
    }

    private double roundOff(double d) {
        DecimalFormat newFormat = new DecimalFormat("#.##");
        try {
            return Double.valueOf(newFormat.format(d));
        } catch (Exception e) {
            return 0;
        }
    }

    public String save() {
        if (deny(PRIVILEGE)) {
            return null;
        }
        if (draft == null) {
            JsfUtil.addErrorMessage("Start a new quotation first.");
            return null;
        }
        if (draft.getPaymentMethod() == PaymentMethod.Credit && draft.getCreditCompany() == null) {
            JsfUtil.addErrorMessage("Select the credit company");
            return null;
        }
        if (draft.getPatient() != null && (draft.getCustomerName() == null || draft.getCustomerName().trim().isEmpty())) {
            draft.setCustomerName(draft.getPatient().getPerson().getName());
        }
        reprice();
        if (lines.isEmpty()) {
            JsfUtil.addErrorMessage("Add at least one service.");
            return null;
        }
        try {
            OpdQuotation saved = quotationService.save(draft, new ArrayList<>(lines),
                    sessionController.getDepartment(), sessionController.getLoggedUser());
            JsfUtil.addSuccessMessage("Quotation " + saved.getQuotationNo() + " saved.");
            draft = null;
            lines = new ArrayList<>();
            return open(saved);
        } catch (OpdQuotationException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------------ search page

    public void searchQuotations() {
        if (deny(PRIVILEGE)) {
            rows = new ArrayList<>();
            return;
        }
        StringBuilder jpql = new StringBuilder("select new com.divudi.core.data.dto.OpdQuotationRowDto("
                + "q.id, q.quotationNo, q.customerName, q.customerPhone, q.createdAt, q.validUntil, q.netTotal,"
                + " q.creater.webUserPerson.name) from OpdQuotation q"
                + " where q.retired=false and q.institution=:ins and q.createdAt between :fd and :td");
        Map<String, Object> m = new HashMap<>();
        m.put("ins", sessionController.getInstitution());
        m.put("fd", fromDate);
        m.put("td", toDate);
        if (quotationNoFilter != null && !quotationNoFilter.trim().isEmpty()) {
            jpql.append(" and upper(q.quotationNo) like :no");
            m.put("no", "%" + quotationNoFilter.trim().toUpperCase() + "%");
        }
        if (keywordFilter != null && !keywordFilter.trim().isEmpty()) {
            jpql.append(" and (upper(q.customerName) like :kw or upper(q.customerPhone) like :kw)");
            m.put("kw", "%" + keywordFilter.trim().toUpperCase() + "%");
        }
        jpql.append(" order by q.createdAt desc");
        rows = (List<OpdQuotationRowDto>) quotationFacade.findLightsByJpql(jpql.toString(), m, TemporalType.TIMESTAMP);
    }

    public String viewQuotation(Long id) {
        if (deny(PRIVILEGE)) {
            return null;
        }
        OpdQuotation q = id == null ? null : quotationFacade.find(id);
        if (q == null || q.isRetired()) {
            JsfUtil.addErrorMessage("Quotation not found.");
            return null;
        }
        return open(q);
    }

    private String open(OpdQuotation q) {
        quotation = q;
        quotationIdOnPage = q.getId();
        Map<String, Object> m = new HashMap<>();
        m.put("q", q);
        quotationItems = quotationItemFacade.findByJpql(
                "select i from OpdQuotationItem i where i.retired=false and i.quotation=:q order by i.id", m);
        return "/opd/opd_quotation_view?faces-redirect=true";
    }

    // ------------------------------------------------------------------ convert to OPD bill

    /**
     * Opens the normal OPD billing screen and adds the quoted services to it
     * through the same add-to-bill step the screen uses, so fees, scheme
     * discounts and checks are the OPD bill's own, at today's tariff. Nothing
     * is saved here.
     */
    public String convertToOpdBill() {
        if (deny(PRIVILEGE) || deny("OpdBilling")) {
            return null;
        }
        if (quotation == null || quotation.getId() == null || quotationIdOnPage == null
                || !quotation.getId().equals(quotationIdOnPage)) {
            JsfUtil.addErrorMessage("This page is out of date. Open the quotation again.");
            return null;
        }
        String outcome = opdBillController.navigateToNewOpdBill();
        if (outcome == null || !outcome.startsWith("/opd/opd_bill")) {
            // Shift not started: the billing screen sends the user to the cashier page.
            return outcome;
        }
        if (quotation.getPatient() != null) {
            opdBillController.setPatient(quotation.getPatient());
        } else {
            Patient walkIn = opdBillController.getPatient();
            walkIn.getPerson().setName(quotation.getCustomerName());
            walkIn.getPerson().setMobile(quotation.getCustomerPhone());
            walkIn.getPerson().setForeigner(quotation.isForeigner());
        }
        if (opdBillController.getPaymentScheme() == null && quotation.getPaymentScheme() != null) {
            opdBillController.setPaymentScheme(quotation.getPaymentScheme());
        }
        List<String> notAdded = new ArrayList<>();
        for (OpdQuotationItem qi : quotationItems) {
            int before = opdBillController.getLstBillEntries().size();
            opdBillController.clearBillItemValues();
            opdBillController.getCurrentBillItem().setItem(qi.getItem());
            opdBillController.setCurrentBillItemQty(qi.getQuantity());
            opdBillController.addToBill();
            if (opdBillController.getLstBillEntries().size() == before) {
                notAdded.add(qi.getItemName());
            }
        }
        opdBillController.clearBillItemValues();
        if (!notAdded.isEmpty()) {
            JsfUtil.addErrorMessageForRedirect("Not added to the bill (check item fees): " + String.join(", ", notAdded));
        }
        return outcome;
    }

    // ------------------------------------------------------------------ accessors

    public OpdQuotation getDraft() {
        return draft;
    }

    public List<OpdQuotationItem> getLines() {
        return lines;
    }

    public ItemLight getItemLight() {
        return itemLight;
    }

    public void setItemLight(ItemLight itemLight) {
        this.itemLight = itemLight;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public String getQuotationNoFilter() {
        return quotationNoFilter;
    }

    public void setQuotationNoFilter(String quotationNoFilter) {
        this.quotationNoFilter = quotationNoFilter;
    }

    public String getKeywordFilter() {
        return keywordFilter;
    }

    public void setKeywordFilter(String keywordFilter) {
        this.keywordFilter = keywordFilter;
    }

    public List<OpdQuotationRowDto> getRows() {
        return rows;
    }

    public OpdQuotation getQuotation() {
        return quotation;
    }

    public Long getQuotationIdOnPage() {
        return quotationIdOnPage;
    }

    public void setQuotationIdOnPage(Long quotationIdOnPage) {
        this.quotationIdOnPage = quotationIdOnPage;
    }

    public List<OpdQuotationItem> getQuotationItems() {
        return quotationItems;
    }
}
