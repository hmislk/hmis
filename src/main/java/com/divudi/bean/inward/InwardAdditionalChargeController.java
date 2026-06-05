/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.inward;
import com.divudi.bean.common.ConfigOptionController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Fee;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.BilledBillFacade;
import com.divudi.core.facade.FeeFacade;
import com.divudi.core.facade.ItemFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class InwardAdditionalChargeController implements Serializable {

    private static final long serialVersionUID = 1L;
    @EJB
    private BillNumberGenerator billNumberBean;
    private InwardChargeType inwardChargeType;
    //////////////
    @EJB
    private FeeFacade feeFacade;
    @EJB
    private BilledBillFacade billedBillFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private ItemFacade itemFacade;
    @Inject
    InwardBeanController inwardBean;
    //////////////
    @Inject
    private SessionController sessionController;
    @Inject
    private AdmissionController admissionController;
    @Inject
    private ConfigOptionController configOptionController;
    //////////////
    private BilledBill current;
    private Institution institution;
    private Item selectedItem;
    private List<BillItem> billItemList;
    private boolean printPreview;
    // Print configuration (paper format) — persisted via department-scoped config options
    private boolean posPaper;
    private boolean fiveFivePaper;
    private boolean a4Paper;

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    public String navigateToAddOutsideChargeFromMenu() {
        makeNull();
        return "/inward/inward_bill_outside_charge?faces-redirect=true";
    }

    public String navigateToAddOutsideChargeFromInpatientProfile() {
        // The caller passes the patient via f:setPropertyActionListener, which
        // fires before this action. Reset all state (including printPreview) but
        // keep the encounter that was just selected.
        PatientEncounter pe = getCurrent().getPatientEncounter();
        makeNull();
        getCurrent().setPatientEncounter(pe);
        institution = sessionController.getInstitution();
        return "/inward/inward_bill_outside_charge?faces-redirect=true";
    }

    /**
     * Finalize the current outside-charge bill and switch the page to the print
     * preview. The charges have already been persisted by {@link #addCharge()};
     * this exposes the accumulated bill items to the print composites and flips
     * the {@code printPreview} flag.
     */
    public void settle() {
        if (getBillItemList().isEmpty()) {
            JsfUtil.addErrorMessage("Add at least one charge before settling");
            return;
        }
        // saveBill() overwrites total/netTotal with the last single entry on every
        // Add, so recompute the settled amount from all accumulated charges before
        // showing the preview (otherwise a multi-charge bill prints only the last value).
        double settledTotal = 0.0;
        for (BillItem bi : getBillItemList()) {
            settledTotal += bi.getNetValue();
        }
        current.setTotal(settledTotal);
        current.setNetTotal(settledTotal);
        getBilledBillFacade().edit(current);
        current.setBillItems(getBillItemList());
        printPreview = true;
        JsfUtil.addSuccessMessage("Bill Settled");
    }

    private boolean errorCheck() {
        if (getCurrent().getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("Select BHT");
            return true;
        }

        if (getCurrent().getFromInstitution() == null) {
            JsfUtil.addErrorMessage("Select Outside Institution");
            return true;
        }

        if (selectedItem == null) {
            JsfUtil.addErrorMessage("Select an Item");
            return true;
        }

        if (getCurrent().getTotal() == null || getCurrent().getTotal() < 1) {
            JsfUtil.addErrorMessage("Enter Added Charge Correctly");
            return true;
        }

        if (getCurrent().getComments() == null || getCurrent().getComments().isEmpty()) {
            JsfUtil.addErrorMessage("Enter Description");
            return true;
        }

        return false;
    }

    public void addCharge() {
        if (errorCheck()) {
            return;
        }
        saveBill();
        BillItem b = saveBillItem();
        billItemList.add(b);
        getCurrent().setSingleBillItem(b);
        getBilledBillFacade().edit(current);

        selectedItem = null;
        inwardChargeType = null;
        getCurrent().setTotal(null);
        getCurrent().setComments(null);

        JsfUtil.addSuccessMessage("Charge Added");
    }

    public void makeNull() {
        current = null;
        billItemList = null;
        inwardChargeType = null;
        selectedItem = null;
        printPreview = false;
        institution = sessionController.getInstitution();
    }

    public List<Admission> completePatientByInstitution(String query) {
        return admissionController.completePatientNotFinalizedByInstitution(query, getInstitution());
    }

    public void onInstitutionChange() {
        current = null;
        billItemList = null;
        inwardChargeType = null;
        selectedItem = null;
        printPreview = false;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public void reset() {
        PatientEncounter p = current.getPatientEncounter();
        current = null;
        billItemList = null;
        printPreview = false;
        getCurrent().setPatientEncounter(p);
        inwardChargeType = null;
        selectedItem = null;
        JsfUtil.addSuccessMessage("Cleared Successfully");
    }

    public void makeChargesNull() {
        inwardChargeType = null;
        selectedItem = null;
        current.setTotal(null);
        current.setComments(null);
    }

    private void saveBill() {
        getCurrent().setBillType(BillType.InwardOutSideBill);
        getCurrent().setBillTypeAtomic(BillTypeAtomic.INWARD_OUTSIDE_CHARGES_BILL);
        getCurrent().setDepartment(getSessionController().getDepartment());
        getCurrent().setInstitution(getSessionController().getInstitution());

        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());
        getCurrent().setNetTotal(getCurrent().getTotal());
        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());

        getCurrent().setDeptId(getBillNumberBean().departmentBillNumberGenerator(getCurrent().getDepartment(), getCurrent().getBillType(), BillClassType.BilledBill, BillNumberSuffix.NONE));
        getCurrent().setInsId(getBillNumberBean().institutionBillNumberGenerator(getCurrent().getInstitution(), getCurrent().getBillType(), BillClassType.BilledBill, BillNumberSuffix.INWSER));

        if (getCurrent().getId() == null) {
            getBilledBillFacade().create(getCurrent());
        } else {
            getBilledBillFacade().edit(getCurrent());
        }
    }

    private BillItem saveBillItem() {
        BillItem temBi = new BillItem();
        temBi.setBill(getCurrent());
        temBi.setItem(selectedItem);
        if (selectedItem != null) {
            temBi.setInwardChargeType(selectedItem.getInwardChargeType());
        }
        temBi.setGrossValue(getCurrent().getTotal());
        temBi.setNetValue(getCurrent().getTotal());
        temBi.setCreatedAt(new Date());
        temBi.setCreater(getSessionController().getLoggedUser());

        if (temBi.getId() == null) {
            getBillItemFacade().create(temBi);
        }
        saveBillFee(temBi);

        return temBi;
    }

    private void saveBillFee(BillItem bt) {
        BillFee bf = new BillFee();
        Fee additional = getInwardBean().createAdditionalFee();

        bf.setPatienEncounter(getCurrent().getPatientEncounter());
        bf.setBill(getCurrent());
        bf.setFee(additional);
        bf.setBillItem(bt);
        bf.setCreatedAt(new Date());
        bf.setCreater(getSessionController().getLoggedUser());
        bf.setFeeGrossValue(getCurrent().getTotal());
        bf.setFeeValue(getCurrent().getTotal());

        if (bf.getId() == null) {
            getBillFeeFacade().create(bf);
        }
    }

    public List<Item> completeItem(String qry) {
        String upper = qry.toUpperCase();
        return itemFacade.findByJpql(
                "select i from Item i where i.retired=false "
                + "and (type(i) = com.divudi.core.entity.inward.InwardService "
                + "  or type(i) = com.divudi.core.entity.Service) "
                + "and upper(i.name) like '%" + upper + "%' "
                + "order by i.name");
    }

    public void onItemSelect() {
        if (selectedItem != null && selectedItem.getTotal() != null && selectedItem.getTotal() > 0) {
            getCurrent().setTotal(selectedItem.getTotal());
        }
        if (selectedItem != null) {
            inwardChargeType = selectedItem.getInwardChargeType();
        }
    }

    public Item getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(Item selectedItem) {
        this.selectedItem = selectedItem;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public BilledBillFacade getBilledBillFacade() {
        return billedBillFacade;
    }

    public void setBilledBillFacade(BilledBillFacade billedBillFacade) {
        this.billedBillFacade = billedBillFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public BilledBill getCurrent() {
        if (current == null) {
            current = new BilledBill();
            current.setBillType(BillType.InwardBill);
        }

        return current;
    }

    public void setCurrent(BilledBill current) {
        this.current = current;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public FeeFacade getFeeFacade() {
        return feeFacade;
    }

    public void setFeeFacade(FeeFacade feeFacade) {
        this.feeFacade = feeFacade;
    }

    public InwardChargeType getInwardChargeType() {
        return inwardChargeType;
    }

    public void setInwardChargeType(InwardChargeType inwardChargeType) {
        this.inwardChargeType = inwardChargeType;
    }

    public List<BillItem> getBillItemList() {
        if (billItemList == null) {
            billItemList = new ArrayList<>();
        }
        return billItemList;
    }

    public void setBillItemList(List<BillItem> billItemList) {
        this.billItemList = billItemList;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    // ------------------------------------------------------------------
    // Print (paper format) configuration — opened from the preview dialog
    // ------------------------------------------------------------------
    public void loadPrintConfig() {
        posPaper = configOptionController.getBooleanValueByKey("Inward Outside Charge Bill is POS Paper", false);
        fiveFivePaper = configOptionController.getBooleanValueByKey("Inward Outside Charge Bill is 5x5 Paper", true);
        a4Paper = configOptionController.getBooleanValueByKey("Inward Outside Charge Bill is A4 Paper", false);
    }

    public void savePrintConfig() {
        configOptionController.setBooleanValueByKey("Inward Outside Charge Bill is POS Paper", posPaper);
        configOptionController.setBooleanValueByKey("Inward Outside Charge Bill is 5x5 Paper", fiveFivePaper);
        configOptionController.setBooleanValueByKey("Inward Outside Charge Bill is A4 Paper", a4Paper);
        JsfUtil.addSuccessMessage("Print configuration saved");
    }

    public boolean isPosPaper() {
        return posPaper;
    }

    public void setPosPaper(boolean posPaper) {
        this.posPaper = posPaper;
    }

    public boolean isFiveFivePaper() {
        return fiveFivePaper;
    }

    public void setFiveFivePaper(boolean fiveFivePaper) {
        this.fiveFivePaper = fiveFivePaper;
    }

    public boolean isA4Paper() {
        return a4Paper;
    }

    public void setA4Paper(boolean a4Paper) {
        this.a4Paper = a4Paper;
    }

    public ConfigOptionController getConfigOptionController() {
        return configOptionController;
    }

    public void setConfigOptionController(ConfigOptionController configOptionController) {
        this.configOptionController = configOptionController;
    }
}
