/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.SessionController;
import com.divudi.data.BillType;
import com.divudi.data.InstitutionType;
import com.divudi.data.dataStructure.DepartmentSale;
import com.divudi.data.dataStructure.DepartmentStock;
import com.divudi.data.dataStructure.InstitutionSale;
import com.divudi.data.dataStructure.InstitutionStock;
import com.divudi.data.dataStructure.ItemQuantityAndValues;
import com.divudi.data.dataStructure.ItemTransactionSummeryRow;
import com.divudi.data.dataStructure.StockAverage;

import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.Ampp;
import com.divudi.entity.pharmacy.Atm;
import com.divudi.entity.pharmacy.MeasurementUnit;
import com.divudi.entity.pharmacy.PharmaceuticalItem;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.entity.pharmacy.Vmp;
import com.divudi.entity.pharmacy.Vmpp;
import com.divudi.entity.pharmacy.Vtm;
import com.divudi.facade.AmpFacade;
import com.divudi.facade.AmppFacade;
import com.divudi.facade.AtmFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.PharmaceuticalItemFacade;
import com.divudi.facade.StockFacade;
import com.divudi.facade.VmpFacade;
import com.divudi.facade.VmppFacade;
import com.divudi.facade.VtmFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.java.CommonFunctions;
import com.divudi.light.pharmacy.PharmaceuticalItemLight;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
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
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class PharmacyController implements Serializable {

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    private SessionController sessionController;
    @Inject
    CommonController commonController;
    @Inject
    private AmpController ampController;
    @Inject
    VtmController vtmController;
    @Inject
    AtmController atmController;
    @Inject
    ManufacturerController manufaturerController;
    @Inject
    ImporterController importerController;
    @Inject
    DiscardCategoryController discardCategoryController;
    @Inject
    VmpController vmpController;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    ItemFacade itemFacade;
    @EJB
    VtmFacade vtmFacade;
    @EJB
    AtmFacade atmFacade;
    @EJB
    VmpFacade vmpFacade;
    @EJB
    AmpFacade ampFacade;
    @EJB
    VmppFacade vmppFacade;
    @EJB
    AmppFacade amppFacade;
    @EJB
    PharmaceuticalItemFacade piFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private StockFacade stockFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;

    private CommonFunctions commonFunctions;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    private static final long serialVersionUID = 1L;
    private int pharmacyAdminIndex;
    private int pharmacySummaryIndex;

    private Item pharmacyItem;
    private Date fromDate;
    private Date toDate;
    Department department;
    boolean hasSale;
    boolean hasWholesale;
    boolean hasInward;
    boolean hasIssue;
    boolean hasTransferOut;
    boolean hasPurchase;
    boolean hasTransferIn;
    ////////
    //List<DepartmentStock> departmentStocks;
    private List<DepartmentSale> departmentSale;
    private List<BillItem> grns;
    private List<BillItem> pos;
    private List<BillItem> directPurchase;
    private List<Bill> bills;
    List<ItemTransactionSummeryRow> itemTransactionSummeryRows;
    private int managePharamcyReportIndex = -1;
    double persentage;
    Category category;

    private PharmaceuticalItemLight selectedLight;
    private List<PharmaceuticalItemLight> selectedLights;
    private List<PharmaceuticalItemLight> allLights;

    private List<Atm> atms;
    private List<Vtm> vtms;
    private List<Vmp> vmps;
    private List<Amp> amps;
    private List<Vmpp> vmpps;
    private List<Ampp> ampps;

    private List<Atm> atmsSelected;
    private List<Vtm> vtmsSelected;
    private List<Vmp> vmpsSelected;
    private List<Amp> ampsSelected;
    private List<Vmpp> vmppsSelected;
    private List<Ampp> amppsSelected;

    private Atm atm;
    private Vtm vtm;
    private Vmp vmp;
    private Amp amp;
    private Vmpp vmpp;
    private Ampp ampp;

    private MeasurementUnit issueUnit;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Methods - Fill Data">
    private void fillVtms() {
        String j = "select i "
                + " from Vtm i "
                + " where i.retired != true "
                + " order by i.name";
        vtms = vtmFacade.findByJpql(j);
    }

    private void fillAtms() {
        String j = "select i "
                + " from Atm i "
                + " where i.retired != true "
                + " order by i.name";
        atms = atmFacade.findByJpql(j);
    }

    private void fillVmps() {
        String j = "select i "
                + " from Vmp i "
                + " where i.retired != true "
                + " order by i.name";
        vmps = vmpFacade.findByJpql(j);
    }

    private void fillAmps() {
        String j = "select i "
                + " from Amp i "
                + " where i.retired != true "
                + " order by i.name";
        amps = ampFacade.findByJpql(j);
    }

    private void fillVmpps() {
        String j = "select i "
                + " from Vmpp i "
                + " where i.retired != true "
                + " order by i.name";
        vmpps = vmppFacade.findByJpql(j);
    }

    private void fillAmpps() {
        String j = "select i "
                + " from Ampp i "
                + " where i.retired != true "
                + " order by i.name";
        ampps = amppFacade.findByJpql(j);
    }

    public void listPharmacyPurchaseBills() {
        try {
            StringBuilder jpql = new StringBuilder("select b from Bill b where b.retired=:ret");
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("ret", false);

            List<BillType> billTypes = new ArrayList<>();
            billTypes.add(BillType.PharmacyGrnReturn);
            billTypes.add(BillType.PharmacyGrnBill);
            billTypes.add(BillType.PharmacyPurchaseBill);
            jpql.append(" and b.billType in :bts");
            parameters.put("bts", billTypes);

            if (fromDate != null && toDate != null) {
                jpql.append(" and b.createdAt between :fd and :td");
                parameters.put("fd", fromDate);
                parameters.put("td", toDate);
            }

            if (institution != null) {
                jpql.append(" and b.fromInstitution=:fi");
                parameters.put("fi", institution);
            }

            bills = billFacade.findByJpql(jpql.toString(), parameters, TemporalType.TIMESTAMP);
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the exception appropriately
        }
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="Methods - Navigation">
    
    
    public String navigateToBinCard() {
        return "/pharmacy/bin_card?faces-redirect=true";
    }
    
    public String navigateToItemsList() {
        return "/pharmacy/list_amps?faces-redirect=true";
    }
    
    public String navigateToMedicineList() {
        return "/pharmacy/list_medicines?faces-redirect=true";
    }
    
    public String navigateToItemsWithoutDistributor() {
        return "/pharmacy/pharmacy_report_list_amps_with_out_distributor?faces-redirect=true";
    }
    
    public String navigateToItemsWithSuppliersAndPrices() {
        return "/pharmacy/item_supplier_prices?faces-redirect=true";
    }
    
    public String navigateToItemsWithDistributor() {
        return "/pharmacy/pharmacy_report_list_distributor_with_distributor_items?faces-redirect=true";
    }
    
    public String navigateToItemsWithMultipleDistributorsItemsOnly() {
        return "/pharmacy/pharmacy_report_list_grater_than_one_distributor?faces-redirect=true";
    }
    
    public String navigateToItemWithMultipleDistributors() {
        return "/pharmacy/pharmacy_report_list_item_with_multiple_dealor?faces-redirect=true";
    }
    
    public String navigateToReorderAnalysis() {
        return "/pharmacy/ordering_data?faces-redirect=true";
    }
    
    
    public String navigateToReorderManagement() {
        return "/pharmacy/reorder_management?faces-redirect=true";
    }
    
    public String navigateToAllItemsTransactionSummary() {
        return "/pharmacy/raport_all_item_transaction_summery?faces-redirect=true";
    }
    
    public String navigateToItemTransactionDetails() {
        return "/pharmacy/pharmacy_item_transactions?faces-redirect=true";
    }
    
    public String navigateToListPharmaceuticals() {
        fillPharmaceuticalLights();
        return "/pharmacy/admin/items?faces-redirect=true";
    }

    public String navigateToPharmacyAnalytics() {
        return "/pharmacy/pharmacy_analytics?faces-redirect=true";
    }

    public String navigateToManagePharmaceuticals() {
        return "/pharmacy/admin/index?faces-redirect=true";
    }

    public String navigateToListAllPharmaceuticalItemsLight() {
        return "/pharmacy/admin/items?faces-redirect=true";
    }

    public String navigateToDosageForms() {
        return "/pharmacy/admin/dosage_forms?faces-redirect=true";
    }

    public String navigateToPharmaceuticalItemCategories() {
        return "/pharmacy/admin/pharmaceutical_item_category?faces-redirect=true";
    }

    public String navigateToAtc() {
        return "/pharmacy/admin/atc?faces-redirect=true";
    }

    public String navigateToAmp() {
        ampController.setItems(null);
        return "/pharmacy/admin/amp?faces-redirect=true";
    }

    public String navigateToAmpp() {
        return "/pharmacy/admin/ampp?faces-redirect=true";
    }

    public String navigateToAtm() {
        atmController.getItems();
        atmController.getCurrent();
        return "/pharmacy/admin/atm?faces-redirect=true";
    }

    public String navigateToManufacturers() {
        manufaturerController.getItems();
        manufaturerController.getCurrent();
        return "/pharmacy/pharmacy_manufacturer?faces-redirect=true";
    }

    public String navigateToDiscardCategory() {
        discardCategoryController.fillDiscardCategories();
        discardCategoryController.getItems();
        discardCategoryController.getCurrent();
        return "/pharmacy/pharmacy_discard_category?faces-redirect=true";
    }

    public String navigateToImporters() {
        importerController.getItems();
        importerController.getCurrent();
        return "/pharmacy/pharmacy_importer?faces-redirect=true";
    }
    
    public String navigateToSuppliers(){
        return "/pharmacy/pharmacy_dealer?faces-redirect=true";
    }
    
    public String navigateToItemSuppliers(){
        return "/pharmacy/pharmacy_items_distributors?faces-redirect=true";
    }

    public String navigateToVmp() {
        return "/pharmacy/admin/vmp?faces-redirect=true";
    }

    public String navigateToVtm() {
        vtmController.fillItems();
        return "/pharmacy/admin/vtm?faces-redirect=true";
    }

    public String navigateToVmpp() {
        return "/pharmacy/admin/vmpp?faces-redirect=true";
    }

    public String navigateToDosageFormsMultiple() {
        return "/pharmacy/admin/dosage_forms_multiple?faces-redirect=true";
    }

    public String navigateToAtcMultiple() {
        return "/pharmacy/admin/atc_multiple?faces-redirect=true";
    }

    public String navigateToAmpMultiple() {
        fillAmps();
        return "/pharmacy/admin/amp_multiple?faces-redirect=true";
    }

    public String navigateToAmppMultiple() {
        fillAmpps();
        return "/pharmacy/admin/ampp_multiple?faces-redirect=true";
    }

    public String navigateToAtmMultiple() {
        fillAtms();
        return "/pharmacy/admin/atm_multiple?faces-redirect=true";
    }

    public String navigateToVmpMultiple() {
        fillVmps();
        return "/pharmacy/admin/vmp_multiple?faces-redirect=true";
    }

    public String navigateToVtmMultiple() {
        fillVtms();
        return "/pharmacy/admin/vtm_multiple?faces-redirect=true";
    }

    public String navigateToVmppMultiple() {
        fillVmpps();
        return "/pharmacy/admin/vmpp_multiple?faces-redirect=true";
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Methods - Data Maniulation">
    public void deleteSingleVtm() {
        if (vtm == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        vtm.setRetired(true);
        vtmFacade.edit(vtm);
        fillVtms();
    }

    public void deleteSingleAtm() {
        if (atm == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        atm.setRetired(true);
        atmFacade.edit(atm);
        fillAtms();
    }

    public void deleteSingleVmp() {
        if (vmp == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        vmp.setRetired(true);
        vmpFacade.edit(vmp);
        fillVmps();
    }

    public void deleteSingleAmp() {
        if (amp == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        amp.setRetired(Boolean.TRUE);
        ampFacade.edit(amp);
        fillAmps();
    }

    public void deleteSingleVmpp() {
        if (vmpp == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        vmpp.setRetired(true);
        vmppFacade.edit(vmpp);
        fillVmpps();
    }

    public void deleteSingleAmpp() {
        if (ampp == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        ampp.setRetired(true);
        amppFacade.edit(ampp);
        fillAmpps();
    }

    public void deleteMultipleVtms() {
        if (vtmsSelected == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        if (vtmsSelected.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        for (Vtm i : vtmsSelected) {
            i.setRetired(true);
            //TODO: Write to AuditEvent
        }
        vtmFacade.batchEdit(vtmsSelected);
        fillVtms();
    }

    public void deleteMultipleVmps() {
        if (vmpsSelected == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        if (vmpsSelected.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        for (Vmp i : vmpsSelected) {
            i.setRetired(true);
            //TODO: Write to AuditEvent
        }
        vmpFacade.batchEdit(vmpsSelected);
        fillVmps();
    }

    public void assignIssueUnitToMultipleVmps() {
        if (issueUnit == null) {
            JsfUtil.addErrorMessage("No Issue Unit Selected");
            return;
        }
        if (vmpsSelected == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        if (vmpsSelected.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        for (Vmp i : vmpsSelected) {
            i.setIssueUnit(issueUnit);
            //TODO: Write to AuditEvent
        }
        vmpFacade.batchEdit(vmpsSelected);
        fillVmps();
    }

    public void deleteMultipleVmpps() {
        if (vmppsSelected == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        if (vmppsSelected.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        for (Vmpp i : vmppsSelected) {
            i.setRetired(true);
            //TODO: Write to AuditEvent
        }
        vmppFacade.batchEdit(vmppsSelected);
        fillVmpps();
    }

    public void deleteMultipleAmps() {
        if (ampsSelected == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        if (ampsSelected.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        for (Amp i : ampsSelected) {
            i.setRetired(true);
            //TODO: Write to AuditEvent
        }
        ampFacade.batchEdit(ampsSelected);
        fillAmps();
    }

    public void deleteMultipleAtms() {
        if (atmsSelected == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        if (atmsSelected.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        for (Atm i : atmsSelected) {
            i.setRetired(true);
            //TODO: Write to AuditEvent
        }
        atmFacade.batchEdit(atmsSelected);
        fillAtms();
    }

    public void deleteMultipleAmpps() {
        if (amppsSelected == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        if (amppsSelected.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        for (Ampp i : amppsSelected) {
            i.setRetired(true);
            //TODO: Write to AuditEvent
        }
        amppFacade.batchEdit(amppsSelected);
        fillAmpps();
    }

    // </editor-fold>
    public void clearItemHistory() {

        grantStock = 0.00;
        grantSaleQty = 0.00;
        grantSaleValue = 0.00;
        grantWholeSaleQty = 0.00;
        grantWholeSaleValue = 0.00;
        grantBhtIssueQty = 0.00;
        grantBhtValue = 0.00;
        grantTransferIssueQty = 0.00;
        grantTransferIssueValue = 0.00;
        grantTransferReceiveQty = 0.00;
        grantTransferReceiveValue = 0.00;
        grantIssueQty = 0.00;
        grantIssueValue = 0.00;

        fromDate = CommonFunctions.getStartOfMonth();
        toDate = CommonFunctions.getEndOfDay(new Date());

        pharmacyItem = null;
        institutionStocks = null;
        institutionSales = null;
        grns = null;
        institutionWholeSales = null;
        institutionBhtIssue = null;
        institutionTransferIssue = null;
        institutionTransferReceive = null;
        institutionIssue = null;
        pos = null;
        directPurchase = null;
        ampps = null;

    }

    public void deleteSelectedPharmaceuticalLight() {
        if (selectedLights == null || selectedLights.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        List<Item> itemsToUpdate = new ArrayList<>();
        for (PharmaceuticalItemLight l : selectedLights) {
            if (l.getId() == null) {
                continue;
            }
            Item i = itemFacade.find(l.getId());
            if (i == null) {
                continue;
            }
            i.setRetired(true);
            i.setRetirer(sessionController.getLoggedUser());
            i.setRetiredAt(new Date());
            itemsToUpdate.add(i);
        }
        itemFacade.batchEdit(itemsToUpdate);
        fillPharmaceuticalLights();
    }

    public void fillPharmaceuticalLights() {
        String jpql = "select new com.divudi.bean.pharmacy.PharmaceuticalItemLight(p.id, p.name, p.clazz) "
                + " from PharmaceuticalItem p "
                + " where p.retired != true "
                + " order by p.name";
        jpql = "select new com.divudi.bean.pharmacy.PharmaceuticalItemLight(p.id, p.name, p.clazz) "
                + " from PharmaceuticalItem p ";
//        Map m = new HashMap();
//        m.put("ret", Boolean.TRUE);
//        allLights = (List<PharmaceuticalItemLight>) itemFacade.findLightsByJpql(jpql, m);
//        Map m = new HashMap();
//        m.put("ret", Boolean.TRUE);
//        allLights = (List<PharmaceuticalItemLight>) itemFacade.findLightsByJpql(jpql, m);
        allLights = (List<PharmaceuticalItemLight>) itemFacade.findLightsByJpql(jpql);

    }

    public void makeNull() {
        departmentSale = null;
//        departmentStocks = null;
        pos = null;
        grns = null;
        institutionSales = null;
        institutionStocks = null;
        institutionTransferIssue = null;
        directPurchase = null;

    }

    public List<Stock> completeAllStocks(String qry) {
        List<Stock> items;
        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        double d = 0.0;
        m.put("n", "%" + qry.toUpperCase() + "%");
        sql = "select i from Stock i where i.department=:d and "
                + " ((i.itemBatch.item.name) like :n  or "
                + " (i.itemBatch.item.code) like :n  or  "
                + " (i.itemBatch.item.barcode) like :n ) "
                + " order by i.stock desc";
        items = getStockFacade().findByJpql(sql, m, 30);

        return items;
    }

    public List<Item> completeAllStockItems(String qry) {
        List<Item> items = null;
        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        double d = 0.0;
        m.put("n", "%" + qry.toUpperCase() + "%");
        sql = "select distinct(i.itemBatch.item) from Stock i where i.department=:d and "
                + " ((i.itemBatch.item.name) like :n  or "
                + " (i.itemBatch.item.code) like :n  or  "
                + " (i.itemBatch.item.barcode) like :n ) "
                + " and i.stock>0 ";
        if (getCategory() != null) {
            sql += " and i.itemBatch.item.category=:cat ";
            m.put("cat", getCategory());
        }
        sql += " order by i.itemBatch.item.name ";
        items = getItemFacade().findByJpql(sql, m);
        return items;
    }

    public List<Stock> completeStaffStocks(String qry) {
        List<Stock> items;
        String sql;
        Map m = new HashMap();
        double d = 0.0;
        m.put("s", d);
        m.put("n", "%" + qry.toUpperCase() + "%");
        sql = "select i from Stock i where i.stock >:s and "
                + "((i.staff.code) like :n or "
                + "(i.staff.person.name) like :n or "
                + "(i.itemBatch.item.name) like :n ) "
                + "order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        items = getStockFacade().findByJpql(sql, m, 20);

        return items;
    }

    public List<Department> getInstitutionDepatrments(Institution ins) {
        List<Department> d;
        HashMap hm = new HashMap();
        String sql = "Select d From Department d where d.retired=false and d.institution=:ins";
        hm.put("ins", ins);
        d = getDepartmentFacade().findByJpql(sql, hm);

        return d;
    }

    public void createAllItemTransactionSummery() {
        Date startTime = new Date();

        hasInward = false;
        hasIssue = false;
        hasPurchase = false;
        hasTransferIn = false;
        hasSale = false;
        hasTransferOut = false;
        hasWholesale = false;
        String s;
        s = "select a "
                + " from BillItem bi join bi.item a "
                + " where type(a)=:t "
                + " and bi.bill.billType in :bts "
                + " and bi.bill.createdAt between :fd and :td "
                + " and bi.bill.department =:dep "
                + " group by a "
                + " order by a.name";
        BillType[] abts = new BillType[]{BillType.PharmacySale, BillType.PharmacyTransferReceive, BillType.PharmacyPurchaseBill, BillType.PharmacyGrnBill, BillType.PharmacyBhtPre, BillType.PharmacyTransferIssue, BillType.PharmacyIssue, BillType.PharmacyWholeSale};

        Map p = new HashMap();
        p.put("t", Amp.class);
        p.put("fd", fromDate);
        p.put("td", toDate);
        p.put("dep", department);
        p.put("bts", Arrays.asList(abts));

        List<Amp> allAmps = ampFacade.findByJpql(s, p);

        Map<Long, ItemTransactionSummeryRow> m = new HashMap();

        for (Amp a : allAmps) {
            ItemTransactionSummeryRow r = new ItemTransactionSummeryRow();
            r.setItem(a);
            m.put(a.getId(), r);
        }

        BillType[] bts = new BillType[]{BillType.PharmacySale};
        BillType[] rbts = new BillType[]{BillType.PharmacyPre};
        List<ItemQuantityAndValues> rs = findPharmacyTrnasactionQuantityAndValues(fromDate,
                toDate, null, department, null, bts, rbts);

        for (ItemQuantityAndValues v : rs) {
            ItemTransactionSummeryRow r = m.get(v.getItem().getId());
            if (r != null) {
                if (Math.abs(v.getQuantity()) > 0.0) {
                    hasSale = true;
                }

                r.setRetailSaleQty(Math.abs(v.getQuantity()));
                r.setRetailSaleVal(Math.abs(v.getValue()));
            }
        }

        bts = new BillType[]{BillType.PharmacyWholeSale};
        rbts = new BillType[]{BillType.PharmacyWholesalePre};
        rs = findPharmacyTrnasactionQuantityAndValues(fromDate,
                toDate, null, department, null, bts, rbts);
        for (ItemQuantityAndValues v : rs) {
            ItemTransactionSummeryRow r = m.get(v.getItem().getId());
            if (r != null) {
                if (Math.abs(v.getQuantity()) > 0.0) {
                    hasWholesale = true;
                }
                r.setWholeSaleQty(Math.abs(v.getQuantity()));
                r.setWholeSaleVal(Math.abs(v.getValue()));
            }
        }

        bts = new BillType[]{BillType.PharmacyIssue};
        rs = findPharmacyTrnasactionQuantityAndValues(fromDate,
                toDate, null, department, null, bts, rbts);
        for (ItemQuantityAndValues v : rs) {
            ItemTransactionSummeryRow r = m.get(v.getItem().getId());
            if (r != null) {
                r.setIssueQty(Math.abs(v.getQuantity()));
                if (Math.abs(v.getQuantity()) > 0.0) {
                    hasIssue = true;
                }
                r.setIssueVal(Math.abs(v.getValue()));
            }
        }

        bts = new BillType[]{BillType.PharmacyTransferIssue};
        rs = findPharmacyTrnasactionQuantityAndValues(fromDate,
                toDate, null, department, null, bts, rbts);
        for (ItemQuantityAndValues v : rs) {
            ItemTransactionSummeryRow r = m.get(v.getItem().getId());
            if (r != null) {
                if (Math.abs(v.getQuantity()) > 0.0) {
                    hasTransferOut = true;
                }
                r.setTransferOutQty(Math.abs(v.getQuantity()));
                r.setTransferOutVal(Math.abs(v.getValue()));
            }
        }

        bts = new BillType[]{BillType.PharmacyBhtPre};
        rs = findPharmacyTrnasactionQuantityAndValues(fromDate,
                toDate, null, department, null, bts, rbts);
        for (ItemQuantityAndValues v : rs) {
            ItemTransactionSummeryRow r = m.get(v.getItem().getId());
            if (r != null) {
                if (Math.abs(v.getQuantity()) > 0.0) {
                    hasInward = true;
                }
                r.setBhtSaleQty(Math.abs(v.getQuantity()));
                r.setBhtSaleVal(Math.abs(v.getValue()));
            }
        }

        bts = new BillType[]{BillType.PharmacyPurchaseBill, BillType.PharmacyGrnBill};
        rs = findPharmacyTrnasactionQuantityAndValues(fromDate,
                toDate, null, department, null, bts, null);
        for (ItemQuantityAndValues v : rs) {
            ItemTransactionSummeryRow r = m.get(v.getItem().getId());
            if (r != null) {
                if (Math.abs(v.getQuantity()) > 0.0) {
                    hasPurchase = true;
                }
                r.setPurchaseQty(Math.abs(v.getQuantity()));
                r.setPurchaseVal(Math.abs(v.getValue()));
            }
        }

        bts = new BillType[]{BillType.PharmacyTransferReceive};
        rs = findPharmacyTrnasactionQuantityAndValues(fromDate,
                toDate, null, department, null, bts, rbts);
        for (ItemQuantityAndValues v : rs) {
            ItemTransactionSummeryRow r = m.get(v.getItem().getId());
            if (r != null) {
                if (Math.abs(v.getQuantity()) > 0.0) {
                    hasTransferIn = true;
                }
                r.setTransferInQty(Math.abs(v.getQuantity()));
                r.setTransferInVal(Math.abs(v.getValue()));
            }
        }

//        //System.out.println("m = " + m);
        itemTransactionSummeryRows = new ArrayList<>(m.values());

        for (ItemTransactionSummeryRow r : itemTransactionSummeryRows) {
            if (r.getBhtSaleQty() == 0.0 && r.getIssueQty() == 0.0 && r.getPurchaseQty() == 0.0 && r.getRetailSaleQty() == 0.0
                    && r.getWholeSaleQty() == 0.0 && r.getTransferOutQty() == 0.0 && r.getTransferInQty() == 0.0) {
                itemTransactionSummeryRows.remove(r);
            }
        }

        Collections.sort(itemTransactionSummeryRows);

        

    }

    public List<ItemQuantityAndValues> findPharmacyTrnasactionQuantityAndValues(Date fromDate,
            Date toDate,
            Institution ins,
            Department department,
            Item item,
            BillType[] billTypes,
            BillType[] referenceBillTypes) {

        if (false) {
            BillItem bi = new BillItem();
            bi.getNetValue();
            bi.getPharmaceuticalBillItem().getQty();
        }

        String sql;
        Map m = new HashMap();
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        sql = "select new com.divudi.data.dataStructure.ItemQuantityAndValues(i.item, "
                + "sum(i.pharmaceuticalBillItem.qty), "
                + "sum(i.netValue)) "
                + " from BillItem i "
                + " where i.bill.createdAt between :frm and :to  ";
        if (department != null) {
            m.put("dep", department);
            sql += " and i.bill.department=:dep";
        }
        if (item != null) {
            m.put("item", department);
            sql += " and i.item=:item ";
        }

        if (billTypes != null) {
            List<BillType> bts = Arrays.asList(billTypes);
            m.put("bts", bts);
            sql += " and i.bill.billType in :bts ";
        }

        if (referenceBillTypes != null) {
            List<BillType> rbts = Arrays.asList(referenceBillTypes);
            m.put("rbts", rbts);
            sql += " and i.bill.referenceBill.billType in :rbts ";
        }

        if (ins != null) {
            m.put("ins", ins);
            sql += " and (i.bill.institution=:ins or i.bill.department.institution=:ins) ";
        }
        sql += " group by i.item";
        sql += " order by i.item.name";
//        //System.out.println("m = " + m);
//        //System.out.println("sql = " + sql);
        List<ItemQuantityAndValues> lst = getBillItemFacade().findItemQuantityAndValuesList(sql, m, TemporalType.DATE);
        return lst;

    }

    public void averageByDatePercentage() {
        Calendar frm = Calendar.getInstance();
        frm.setTime(fromDate);
        Calendar to = Calendar.getInstance();
        to.setTime(toDate);

        long lValue = to.getTimeInMillis() - frm.getTimeInMillis();
        double dayCount = 0;
        if (lValue != 0) {
            dayCount = lValue / (1000 * 60 * 60 * 24);
        }

        //System.err.println("Day Count " + dayCount);
        createStockAverageByPer(dayCount);

    }

    public void createStockAverageByPer(double dayCount) {

        stockAverages = new ArrayList<>();
        List<Item> items = getItemController().getDealorItem();
        List<Institution> insList = getCompany();
        for (Item i : items) {
            double itemStockTotal = 0;
            double itemAverageTotal = 0;
            StockAverage stockAverage = new StockAverage();
            stockAverage.setItem(i);
            stockAverage.setInstitutionStocks(new ArrayList<InstitutionStock>());

            for (Institution ins : insList) {
                double insStockTotal = 0;
                double insAverageTotal = 0;
                double insStock = 0.0;
                InstitutionStock newTable = new InstitutionStock();
                newTable.setInstitution(ins);
                newTable.setDepatmentStocks(new ArrayList<DepartmentStock>());
                List<Object[]> objs = calDepartmentStock(ins, i);
                double calPerStock = 0.0;
                for (Object[] obj : objs) {
//                    //System.err.println("Inside ");
                    DepartmentStock r = new DepartmentStock();
                    r.setDepartment((Department) obj[0]);
                    r.setStock((Double) obj[1]);

                    double qty = calDepartmentSaleQtyByPer(r.getDepartment(), i);
                    qty = 0 - qty;
                    if (qty != 0 && dayCount != 0) {
                        double avg = qty / dayCount;
                        calPerStock = (avg * persentage) / 100;
                        insStock = r.getStock();
                        r.setAverage(avg);
                    }

//                    //////System.out.println("calPerStock = " + calPerStock);
//                    //////System.out.println("insStockTotal = " + insStockTotal);
//                    //////System.out.println("insAverageTotal = " + insAverageTotal);
                    if ((insStock < calPerStock) && r.getStock() != 0) {
                        //////System.out.println("*insStock = " + insStock);
                        //////System.out.println("*calPerStock = " + calPerStock);
                        insStockTotal += r.getStock();
                        insAverageTotal += r.getAverage();
                        newTable.getDepatmentStocks().add(r);
                    }

                }

//                //////System.out.println("calPerStock = " + calPerStock);
//                //////System.out.println("insStockTotal = " + insStockTotal);
//                //////System.out.println("insAverageTotal = " + insAverageTotal);
                newTable.setInstitutionTotal(insStockTotal);
                newTable.setInstitutionAverage(insAverageTotal);

                if ((insStockTotal != 0 || insAverageTotal != 0) && insStock < calPerStock) {
                    stockAverage.getInstitutionStocks().add(newTable);
                    itemStockTotal += insStockTotal;
                    itemAverageTotal += insAverageTotal;
                }
            }

            if (itemAverageTotal != 0 || itemStockTotal != 0) {
                stockAverage.setItemAverageTotal(itemAverageTotal);
                stockAverage.setItemStockTotal(itemStockTotal);
                stockAverages.add(stockAverage);
            }

        }

    }

    public void averageByMonthByPercentage() {
        Calendar frm = Calendar.getInstance();
        frm.setTime(fromDate);
        Calendar to = Calendar.getInstance();
        to.setTime(toDate);

        long lValue = to.getTimeInMillis() - frm.getTimeInMillis();
        double monthCount = 0;
        if (lValue != 0) {
            monthCount = lValue / (1000 * 60 * 60 * 24 * 30);
        }

        //System.err.println("Month Count " + monthCount);
        createStockAverageByPer(Math.abs(monthCount));

    }

    public double calDepartmentSaleQtyByPer(Department department, Item itm) {

        if (itm instanceof Ampp) {
            itm = ((Ampp) pharmacyItem).getAmp();
        }

        String sql;
        Map m = new HashMap();
        m.put("itm", itm);
        m.put("dep", department);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyPre);
        m.put("refType", BillType.PharmacySale);
        sql = "select sum(i.pharmaceuticalBillItem.qty) "
                + " from BillItem i where i.bill.department=:dep"
                + " and i.bill.referenceBill.billType=:refType "
                + " and i.item=:itm and i.bill.billType=:btp and "
                + " i.createdAt between :frm and :to  ";

        return getBillItemFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public void makeAllPharmaceuticalItemsToAllowDiscounts() {
        List<PharmaceuticalItem> pis = piFacade.findAll();
        for (PharmaceuticalItem pi : pis) {
            pi.setDiscountAllowed(true);
            piFacade.edit(pi);
        }
        JsfUtil.addSuccessMessage("All Pharmaceutical Items were made to allow discounts");

    }

    private double grantStock;

    public double getGrantStock() {
        return grantStock;

    }

    public double getTransferIssueValueByInstitution(Institution toIns, Item i) {
        //   List<Stock> items;
        String sql;
        Map m = new HashMap();
        m.put("toIns", toIns);
        m.put("frmIns", getSessionController().getInstitution());
        m.put("i", i);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyTransferIssue);
        //   m.put("refType", BillType.PharmacySale);
        sql = "select sum(i.pharmaceuticalBillItem.stock.itemBatch.purcahseRate*i.pharmaceuticalBillItem.qty) "
                + "from BillItem i where i.bill.toInstitution=:toIns and i.bill.fromInstitution=:frmIns "
                + " and i.item=:i and i.bill.billType=:btp and i.createdAt between :frm and :to ";
        return getBillItemFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public double getTransferIssueValueByDepartmet(Department toDep, Item i) {
        //   List<Stock> items;
        String sql;
        Map m = new HashMap();
        m.put("tDep", toDep);
        m.put("fDep", getSessionController().getDepartment());
        m.put("i", i);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyTransferIssue);
        //   m.put("refType", BillType.PharmacySale);
        sql = "select sum(i.pharmaceuticalBillItem.stock.itemBatch.purcahseRate*i.pharmaceuticalBillItem.qty) "
                + " from BillItem i where i.bill.toDepartment=:tDep and i.bill.fromDepartment=:fDep "
                + " and i.item=:i and i.bill.billType=:btp and i.createdAt between :frm and :to ";
        return getBillItemFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public double getTransferIssueQtyByDepartmet(Department tDep, Item i) {
        //   List<Stock> items;
        String sql;
        Map m = new HashMap();
        m.put("tDep", tDep);
        m.put("fDep", getSessionController().getDepartment());
        m.put("i", i);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyTransferIssue);
        //   m.put("refType", BillType.PharmacySale);
        sql = "select sum(i.pharmaceuticalBillItem.qty) from BillItem i where i.bill.toDepartment=:tDep"
                + " and  i.bill.fromDepartment=:fDep"
                + " and i.item=:i and i.bill.billType=:btp and i.createdAt between :frm and :to ";
        return getBillItemFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public double getTransferIssueQtyByInstitution(Institution toIns, Item i) {
        //   List<Stock> items;
        String sql;
        Map m = new HashMap();
        m.put("tIns", toIns);
        m.put("fIns", getSessionController().getInstitution());
        m.put("i", i);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyTransferIssue);
        //   m.put("refType", BillType.PharmacySale);
        sql = "select sum(i.pharmaceuticalBillItem.qty) from BillItem i where "
                + " i.bill.toInstitution=:tIns and i.bill.fromInstitution=:fIns "
                + " and i.item=:i and i.bill.billType=:btp and i.createdAt between :frm and :to ";
        return getBillItemFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    private double grantSaleQty;
    private double grantBhtIssueQty;
    private double grantSaleValue;
    private double grantBhtValue;

    private double grantWholeSaleQty;
    private double grantWholeSaleValue;

    private double grantTransferIssueQty;
    private double grantIssueQty;
    private double grantTransferIssueValue;
    private double grantIssueValue;

    @EJB
    private InstitutionFacade institutionFacade;

    private List<Institution> getCompany() {
        String sql;
        HashMap hm = new HashMap();
        hm.put("type", InstitutionType.Company);
        sql = "select c from Institution c where c.retired=false and c.institutionType=:type order by c.name";

        return getInstitutionFacade().findByJpql(sql, hm);
    }

    private List<InstitutionStock> institutionStocks;

    public List<Object[]> calDepartmentStock(Institution institution) {
        //   //System.err.println("Cal Department Stock");
        Item item;

        if (pharmacyItem instanceof Ampp) {
            item = ((Ampp) pharmacyItem).getAmp();
        } else {
            item = pharmacyItem;
        }

        String sql;
        Map m = new HashMap();
        m.put("ins", institution);
        m.put("i", item);
        sql = "select i.department,sum(i.stock) from Stock i where "
                + " i.department.institution=:ins and i.itemBatch.item=:i"
                + " group by i.department"
                + " having sum(i.stock) > 0 ";

        return getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

    }

    public List<Object[]> calDepartmentStock(Institution institution, Item itm) {
        //   //System.err.println("Cal Department Stock");

        if (itm instanceof Ampp) {
            itm = ((Ampp) itm).getAmp();
        }

        String sql;
        Map m = new HashMap();
        m.put("ins", institution);
        m.put("i", itm);
        sql = "select i.department,sum(i.stock) from Stock i where "
                + " i.department.institution=:ins and i.itemBatch.item=:i"
                + " group by i.department"
                + " having sum(i.stock) > 0 ";

        return getBillItemFacade().findAggregates(sql, m);

    }

    public List<Object[]> calDepartmentTransferIssue(Institution institution) {
        Item item;

        if (pharmacyItem instanceof Ampp) {
            item = ((Ampp) pharmacyItem).getAmp();
        } else {
            item = pharmacyItem;
        }

        String sql;
        Map m = new HashMap();
        m.put("curr", getSessionController().getDepartment());
        m.put("i", item);
        m.put("ins", institution);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyTransferIssue);
        //   m.put("refType", BillType.PharmacySale);
        sql = "select i.bill.toDepartment,"
                + " sum(i.pharmaceuticalBillItem.stock.itemBatch.purcahseRate*i.pharmaceuticalBillItem.qty),"
                + " sum(i.pharmaceuticalBillItem.qty) "
                + " from BillItem i "
                + " where i.bill.toDepartment.institution=:ins "
                + " and i.bill.department=:curr "
                + " and i.item=:i"
                + " and i.bill.billType=:btp "
                + " and i.createdAt between :frm and :to"
                + " group by i.bill.toDepartment";

        return getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

    }

    public List<Object[]> calDepartmentIssue(Institution institution) {
        Item item;

        if (pharmacyItem instanceof Ampp) {
            item = ((Ampp) pharmacyItem).getAmp();
        } else {
            item = pharmacyItem;
        }

        String sql;
        Map m = new HashMap();
//        m.put("curr", getSessionController().getDepartment());
        m.put("i", item);
        m.put("ins", institution);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyIssue);
        //   m.put("refType", BillType.PharmacySale);
//        sql = "select i.bill.toDepartment,"
//                + " sum(i.pharmaceuticalBillItem.stock.itemBatch.purcahseRate*i.pharmaceuticalBillItem.qty),"
//                + " sum(i.pharmaceuticalBillItem.qty) "
//                + " from BillItem i "
//                + " where i.bill.toDepartment.institution=:ins "
//                + " and i.bill.department=:curr "
//                + " and i.item=:i"
//                + " and i.bill.billType=:btp "
//                + " and i.createdAt between :frm and :to"
//                + " group by i.bill.toDepartment";

        sql = "select i.billItem.bill.toDepartment,"
                + " sum(i.stock.itemBatch.purcahseRate*i.qty),"
                + " sum(i.qty) "
                + " from PharmaceuticalBillItem i "
                + " where i.billItem.bill.toDepartment.institution=:ins "
                //                + " and i.billItem.bill.department=:curr "
                + " and i.billItem.item=:i"
                + " and i.billItem.bill.billType=:btp "
                + " and i.billItem.createdAt between :frm and :to"
                + " group by i.billItem.bill.toDepartment";

        return getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

    }

    public List<Object[]> calDepartmentTransferReceive(Institution institution) {
        Item item;

        if (pharmacyItem instanceof Ampp) {
            item = ((Ampp) pharmacyItem).getAmp();
        } else {
            item = pharmacyItem;
        }

        String sql;
        Map m = new HashMap();
        m.put("dep", getSessionController().getDepartment());
        m.put("i", item);
        m.put("ins", institution);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyTransferReceive);
        //   m.put("refType", BillType.PharmacySale);
        sql = "select i.bill.fromDepartment,sum(i.pharmaceuticalBillItem.stock.itemBatch.purcahseRate*i.pharmaceuticalBillItem.qty),"
                + " sum(i.pharmaceuticalBillItem.qty) "
                + " from BillItem i where i.bill.fromDepartment.institution=:ins and i.bill.department=:dep "
                + " and i.item=:i and i.bill.billType=:btp and i.createdAt between :frm and :to group by i.bill.fromDepartment";

        return getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

    }

    public List<Object[]> calDepartmentBhtIssue(Institution institution, BillType billType) {
        Item item;

        if (pharmacyItem instanceof Ampp) {
            item = ((Ampp) pharmacyItem).getAmp();
        } else {
            item = pharmacyItem;
        }

        String sql;
        Map m = new HashMap();
        m.put("itm", item);
        m.put("ins", institution);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", billType);
        sql = "select i.bill.department,"
                + " sum(i.netValue),"
                + " sum(i.pharmaceuticalBillItem.qty) "
                + " from BillItem i "
                + " where i.bill.department.institution=:ins"
                + " and i.item=:itm "
                + " and i.bill.billType=:btp "
                + " and i.createdAt between :frm and :to  "
                + " group by i.bill.department";

        return getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

    }

    public double findAllOutTransactions(Item item) {
        if (item instanceof Amp) {
            return findAllOutTransactions((Amp) amp);
        } else if (item instanceof Vmp) {
            List<Amp> amps = vmpController.ampsOfVmp((Vmp) item);
            return findAllOutTransactions(amps);
        } else {
            return 0.0;
        }
    }

    public double findAllOutTransactions(Amp item) {
        List<Amp> amps = new ArrayList<>();
        amps.add(item);
        return findAllOutTransactions(amps);
    }

    public double findAllOutTransactions(List<Amp> amps) {
        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.PharmacyBhtPre);
        bts.add(BillType.PharmacyPre);
        return findTransactionStocks(null, null, bts, amps, fromDate, toDate);
    }

    public double findTransactionStocks(Department dep, Institution ins, List<BillType> billTypes, List<Amp> amps, Date fd, Date td) {
        StringBuilder jpqlBuilder = new StringBuilder();
        Map<String, Object> parameters = new HashMap<>();

        jpqlBuilder.append("select sum(abs(i.pharmaceuticalBillItem.qty)) from BillItem i where i.retired = false");

        if (dep != null) {
            jpqlBuilder.append(" and i.bill.department = :dep");
            parameters.put("dep", dep);
        }
        if (ins != null) {
            jpqlBuilder.append(" and i.bill.department.institution = :ins");
            parameters.put("ins", ins);
        }
        if (billTypes != null && !billTypes.isEmpty()) {
            jpqlBuilder.append(" and i.bill.billType in :btp");
            parameters.put("btp", billTypes);
        }
        if (amps != null && !amps.isEmpty()) {
            jpqlBuilder.append(" and i.item in :itm");
            parameters.put("itm", amps);
        }
        if (fd != null && td != null) {
            jpqlBuilder.append(" and i.createdAt between :frm and :to");
            parameters.put("frm", fd);
            parameters.put("to", td);
        }

        jpqlBuilder.append(" group by i.bill.department");

        String jpql = jpqlBuilder.toString();
        double qty = getBillItemFacade().findDoubleByJpql(jpql, parameters, TemporalType.TIMESTAMP);
        return qty;
    }

    public double findTransactionQuentity(Item item) {
        //TO DO Senula
        List<BillType> billTypes = new ArrayList<>();
        billTypes.add(BillType.PharmacySale);
        billTypes.add(BillType.InwardPharmacyRequest);
        return 0.0;
    }

    public double findTransactionQuentity(Institution institution, Amp item, List<BillType> billTypes) {
        //TO DO Senula
        return 0.0;
    }

    public double findTransactionQuentity(Institution institution, Item item, List<BillType> billTypes, Date fromDate, Date toDate) {
        //TO DO Senula
        if (pharmacyItem instanceof Ampp) {
            item = ((Ampp) pharmacyItem).getAmp();
        } else {
            item = pharmacyItem;
        }
        String sql;

//        sql = "select i "
//                + " from BillItem i "
//                + " where i.bill.department.institution=:ins"
//                + " and i.bill.referenceBill.billType=:refType "
//                + " and i.bill.referenceBill.cancelled=false "
//                + " and i.item=:itm "
//                + " and i.bill.billType=:btp "
//                + " and i.createdAt between :frm and :to  "
//                + " order by i.bill.department.name,i.bill.insId ";
        Map m = new HashMap();

        m.put("itm", item);
        m.put("ins", institution);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyPre);
        m.put("refType", BillType.PharmacySale);
//        
//        List<BillItem> billItems=getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
//        if (billItems!=null) {
//            grns.addAll(billItems);
//        }
//        //System.out.println("billItems = " + billItems);
//        //System.out.println("institution.getName() = " + institution.getName());

//        for (BillItem bi : billItems) {
//            //System.out.println("bi.getBill().getDepartment().getName() = " + bi.getBill().getDepartment().getName());
//            //System.out.println("bi.getInsId() = " + bi.getInsId());
//            //System.out.println("bi.getDeptId() = " + bi.getDeptId());
//            //System.out.println("bi.getPharmaceuticalBillItem().getQty() = " + bi.getPharmaceuticalBillItem().getQty());
//        }
        sql = "select i.bill.department,"
                + " sum(i.netValue),"
                + " sum(i.pharmaceuticalBillItem.qty) "
                + " from BillItem i "
                + " where i.bill.department.institution=:ins"
                + " and i.bill.referenceBill.billType=:refType "
                + " and i.bill.referenceBill.cancelled=false "
                + " and i.item=:itm "
                + " and i.bill.billType=:btp "
                + " and i.createdAt between :frm and :to  "
                + " group by i.bill.department";

        return 0.0;

    }

    public List<Object[]> calDepartmentSale(Institution institution) {
        Item item;

        if (pharmacyItem instanceof Ampp) {
            item = ((Ampp) pharmacyItem).getAmp();
        } else {
            item = pharmacyItem;
        }

        String sql;

//        sql = "select i "
//                + " from BillItem i "
//                + " where i.bill.department.institution=:ins"
//                + " and i.bill.referenceBill.billType=:refType "
//                + " and i.bill.referenceBill.cancelled=false "
//                + " and i.item=:itm "
//                + " and i.bill.billType=:btp "
//                + " and i.createdAt between :frm and :to  "
//                + " order by i.bill.department.name,i.bill.insId ";
        Map m = new HashMap();

        m.put("itm", item);
        m.put("ins", institution);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyPre);
        m.put("refType", BillType.PharmacySale);
//        
//        List<BillItem> billItems=getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
//        if (billItems!=null) {
//            grns.addAll(billItems);
//        }
//        //System.out.println("billItems = " + billItems);
//        //System.out.println("institution.getName() = " + institution.getName());

//        for (BillItem bi : billItems) {
//            //System.out.println("bi.getBill().getDepartment().getName() = " + bi.getBill().getDepartment().getName());
//            //System.out.println("bi.getInsId() = " + bi.getInsId());
//            //System.out.println("bi.getDeptId() = " + bi.getDeptId());
//            //System.out.println("bi.getPharmaceuticalBillItem().getQty() = " + bi.getPharmaceuticalBillItem().getQty());
//        }
        sql = "select i.bill.department,"
                + " sum(i.netValue),"
                + " sum(i.pharmaceuticalBillItem.qty) "
                + " from BillItem i "
                + " where i.bill.department.institution=:ins"
                + " and i.bill.referenceBill.billType=:refType "
                + " and i.bill.referenceBill.cancelled=false "
                + " and i.item=:itm "
                + " and i.bill.billType=:btp "
                + " and i.createdAt between :frm and :to  "
                + " group by i.bill.department";

        return getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

    }

    public List<Object[]> calDepartmentWholeSale(Institution institution) {
        Item item;

        if (pharmacyItem instanceof Ampp) {
            item = ((Ampp) pharmacyItem).getAmp();
        } else {
            item = pharmacyItem;
        }

        String sql;
        Map m = new HashMap();
        m.put("itm", item);
        m.put("ins", institution);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyWholesalePre);
        m.put("refType", BillType.PharmacyWholeSale);
        sql = "select i.bill.department,"
                + " sum(i.netValue),"
                + " sum(i.pharmaceuticalBillItem.qty) "
                + " from BillItem i "
                + " where i.bill.department.institution=:ins"
                + " and i.bill.referenceBill.billType=:refType "
                + " and i.item=:itm "
                + " and i.bill.billType=:btp "
                + " and i.createdAt between :frm and :to  "
                + " group by i.bill.department";

        return getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

    }

    public double calDepartmentSaleQty(Department department, Item itm) {

        if (itm instanceof Ampp) {
            itm = ((Ampp) pharmacyItem).getAmp();
        }

        String sql;
        Map m = new HashMap();
        m.put("itm", itm);
        m.put("dep", department);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyPre);
        m.put("refType", BillType.PharmacySale);
        sql = "select sum(i.pharmaceuticalBillItem.qty) "
                + " from BillItem i where i.bill.department=:dep"
                + " and i.bill.referenceBill.billType=:refType "
                + " and i.item=:itm and i.bill.billType=:btp and "
                + " i.createdAt between :frm and :to  ";

        return getBillItemFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    private Institution institution;
    private List<StockAverage> stockAverages;

    @Inject
    private ItemController itemController;

    public void averageByDate() {
        Date startTime = new Date();

        Calendar frm = Calendar.getInstance();
        frm.setTime(fromDate);
        Calendar to = Calendar.getInstance();
        to.setTime(toDate);

        long lValue = to.getTimeInMillis() - frm.getTimeInMillis();
        double dayCount = 0;
        if (lValue != 0) {
            dayCount = lValue / (1000 * 60 * 60 * 24);
        }

        createStockAverage(dayCount);

        

    }

    public double getGrantWholeSaleQty() {
        return grantWholeSaleQty;
    }

    public void setGrantWholeSaleQty(double grantWholeSaleQty) {
        this.grantWholeSaleQty = grantWholeSaleQty;
    }

    public double getGrantWholeSaleValue() {
        return grantWholeSaleValue;
    }

    public void setGrantWholeSaleValue(double grantWholeSaleValue) {
        this.grantWholeSaleValue = grantWholeSaleValue;
    }

    public void averageByMonth() {
        Date startTime = new Date();

        Calendar frm = Calendar.getInstance();
        frm.setTime(fromDate);
        Calendar to = Calendar.getInstance();
        to.setTime(toDate);

        long lValue = to.getTimeInMillis() - frm.getTimeInMillis();
        double monthCount = 0;
        if (lValue != 0) {
            monthCount = lValue / (1000 * 60 * 60 * 24 * 30);
        }

        createStockAverage(Math.abs(monthCount));

        

    }

    public void createStockAverage(double dayCount) {

        stockAverages = new ArrayList<>();
        List<Item> items = getItemController().getDealorItem();
        List<Institution> insList = getCompany();
        for (Item i : items) {
            double itemStockTotal = 0;
            double itemAverageTotal = 0;
            StockAverage stockAverage = new StockAverage();
            stockAverage.setItem(i);
            stockAverage.setInstitutionStocks(new ArrayList<InstitutionStock>());

            for (Institution ins : insList) {
                double insStockTotal = 0;
                double insAverageTotal = 0;
                InstitutionStock newTable = new InstitutionStock();
                newTable.setInstitution(ins);
                newTable.setDepatmentStocks(new ArrayList<DepartmentStock>());
                List<Object[]> objs = calDepartmentStock(ins, i);

                for (Object[] obj : objs) {
//                    System.err.println("Inside ");
                    DepartmentStock r = new DepartmentStock();
                    r.setDepartment((Department) obj[0]);
                    r.setStock((Double) obj[1]);

                    double qty = calDepartmentSaleQty(r.getDepartment(), i);
                    qty = 0 - qty;
                    if (qty != 0 && dayCount != 0) {
                        double avg = qty / dayCount;
                        r.setAverage(avg);
                    }

                    insStockTotal += r.getStock();
                    insAverageTotal += r.getAverage();
                    newTable.getDepatmentStocks().add(r);

                }

                newTable.setInstitutionTotal(insStockTotal);
                newTable.setInstitutionAverage(insAverageTotal);

                if (insStockTotal != 0 || insAverageTotal != 0) {
                    stockAverage.getInstitutionStocks().add(newTable);
                    itemStockTotal += insStockTotal;
                    itemAverageTotal += insAverageTotal;
                }
            }

            stockAverage.setItemAverageTotal(itemAverageTotal);
            stockAverage.setItemStockTotal(itemStockTotal);
            stockAverages.add(stockAverage);
        }

    }

    public void createInstitutionStock() {
        //   //System.err.println("Institution Stock");
        List<Institution> insList = getCompany();

        institutionStocks = new ArrayList<>();
        grantStock = 0;

        for (Institution ins : insList) {
            InstitutionStock newTable = new InstitutionStock();
            List<DepartmentStock> list = new ArrayList<>();
            double totalStock = 0;
            List<Object[]> objs = calDepartmentStock(ins);

            for (Object[] obj : objs) {
                DepartmentStock r = new DepartmentStock();
                r.setDepartment((Department) obj[0]);
                r.setStock((Double) obj[1]);
                list.add(r);

                //Total Institution Stock
                totalStock += r.getStock();
                grantStock += r.getStock();

            }

            if (totalStock != 0) {
                newTable.setDepatmentStocks(list);
                newTable.setInstitution(ins);
                newTable.setInstitutionTotal(totalStock);

                institutionStocks.add(newTable);
            }
        }

    }

    public void createInstitutionSale() {
        List<Institution> insList = getCompany();

        institutionSales = new ArrayList<>();
        grantSaleQty = 0;
        grantSaleValue = 0;

        for (Institution ins : insList) {
            InstitutionSale newTable = new InstitutionSale();
            List<DepartmentSale> list = new ArrayList<>();
            double totalValue = 0;
            double totalQty = 0;
            List<Object[]> objs = calDepartmentSale(ins);

            for (Object[] obj : objs) {
                DepartmentSale r = new DepartmentSale();
                r.setDepartment((Department) obj[0]);
                r.setSaleValue((Double) obj[1]);
                r.setSaleQty((Double) obj[2]);
                list.add(r);
                //Total Institution Stock
                totalValue += r.getSaleValue();
                totalQty += r.getSaleQty();
                grantSaleValue += r.getSaleValue();
                grantSaleQty += r.getSaleQty();

            }

            if (totalQty != 0 || totalValue != 0) {
                newTable.setDepartmentSales(list);
                newTable.setInstitution(ins);
                newTable.setInstitutionQty(totalQty);
                newTable.setInstitutionValue(totalValue);

                institutionSales.add(newTable);

            }
        }

    }

    public void createInstitutionWholeSale() {
        List<Institution> insList = getCompany();

        institutionWholeSales = new ArrayList<>();
        grantWholeSaleQty = 0;
        grantWholeSaleValue = 0;

        for (Institution ins : insList) {
            InstitutionSale newTable = new InstitutionSale();
            List<DepartmentSale> list = new ArrayList<>();
            double totalValue = 0;
            double totalQty = 0;
            List<Object[]> objs = calDepartmentWholeSale(ins);

            for (Object[] obj : objs) {
                DepartmentSale r = new DepartmentSale();
                r.setDepartment((Department) obj[0]);
                r.setSaleValue((Double) obj[1]);
                r.setSaleQty((Double) obj[2]);
                list.add(r);
                //Total Institution Stock
                totalValue += r.getSaleValue();
                totalQty += r.getSaleQty();
                grantWholeSaleValue += r.getSaleValue();
                grantWholeSaleQty += r.getSaleQty();

            }

            if (totalQty != 0 || totalValue != 0) {
                newTable.setDepartmentSales(list);
                newTable.setInstitution(ins);
                newTable.setInstitutionQty(totalQty);
                newTable.setInstitutionValue(totalValue);

                institutionWholeSales.add(newTable);

            }
        }

    }

    public double getGrantBhtIssueQty() {
        return grantBhtIssueQty;
    }

    public void setGrantBhtIssueQty(double grantBhtIssueQty) {
        this.grantBhtIssueQty = grantBhtIssueQty;
    }

    public double getGrantBhtValue() {
        return grantBhtValue;
    }

    public void setGrantBhtValue(double grantBhtValue) {
        this.grantBhtValue = grantBhtValue;
    }

    public List<InstitutionSale> getInstitutionBhtIssue() {
        return institutionBhtIssue;
    }

    public void setInstitutionBhtIssue(List<InstitutionSale> institutionBhtIssue) {
        this.institutionBhtIssue = institutionBhtIssue;
    }

    public void createInstitutionBhtIssue() {
        List<Institution> insList = getCompany();

        institutionBhtIssue = new ArrayList<>();
        grantBhtIssueQty = 0;
        grantBhtValue = 0;

        for (Institution ins : insList) {
            InstitutionSale newTable = new InstitutionSale();
            List<DepartmentSale> list = new ArrayList<>();
            double totalValue = 0;
            double totalQty = 0;
            List<Object[]> objs = calDepartmentBhtIssue(ins, BillType.PharmacyBhtPre);

            for (Object[] obj : objs) {
                DepartmentSale r = new DepartmentSale();
                r.setDepartment((Department) obj[0]);
                r.setSaleValue((Double) obj[1]);
                r.setSaleQty((Double) obj[2]);
                list.add(r);
                //Total Institution Stock
                totalValue += r.getSaleValue();
                totalQty += r.getSaleQty();
                grantBhtValue += r.getSaleValue();
                grantBhtIssueQty += r.getSaleQty();

            }

            if (totalQty != 0 || totalValue != 0) {
                newTable.setDepartmentSales(list);
                newTable.setInstitution(ins);
                newTable.setInstitutionQty(totalQty);
                newTable.setInstitutionValue(totalValue);

                institutionBhtIssue.add(newTable);

            }
        }

    }

    public List<InstitutionStock> getInstitutionStock() {
        return institutionStocks;
    }

    private List<InstitutionSale> institutionSales;
    private List<InstitutionSale> institutionWholeSales;
    private List<InstitutionSale> institutionBhtIssue;

    private List<InstitutionSale> institutionTransferIssue;
    private List<InstitutionSale> institutionIssue;

    public List<InstitutionSale> getInstitutionIssue() {
        return institutionIssue;
    }

    public void setInstitutionIssue(List<InstitutionSale> institutionIssue) {
        this.institutionIssue = institutionIssue;
    }

    public List<InstitutionSale> getInstitutionWholeSales() {
        return institutionWholeSales;
    }

    public void setInstitutionWholeSales(List<InstitutionSale> institutionWholeSales) {
        this.institutionWholeSales = institutionWholeSales;
    }

    private List<InstitutionSale> institutionTransferReceive;
    private double grantTransferReceiveQty = 0;
    private double grantTransferReceiveValue = 0;

    public void createInstitutionTransferIssue() {
        List<Institution> insList = getCompany();

        institutionTransferIssue = new ArrayList<>();
        grantTransferIssueQty = 0;
        grantTransferIssueValue = 0;

        for (Institution ins : insList) {
            InstitutionSale newTable = new InstitutionSale();
            List<DepartmentSale> list = new ArrayList<>();
            double totalValue = 0;
            double totalQty = 0;
            List<Object[]> objs = calDepartmentTransferIssue(ins);

            for (Object[] obj : objs) {
                DepartmentSale r = new DepartmentSale();
                r.setDepartment((Department) obj[0]);
                r.setSaleValue((Double) obj[1]);
                r.setSaleQty((Double) obj[2]);
                list.add(r);
                //Total Institution Stock
                totalValue += r.getSaleValue();
                totalQty += r.getSaleQty();
                grantTransferIssueValue += r.getSaleValue();
                grantTransferIssueQty += r.getSaleQty();

            }

            if (totalQty != 0 || totalValue != 0) {
                newTable.setDepartmentSales(list);
                newTable.setInstitution(ins);
                newTable.setInstitutionQty(totalQty);
                newTable.setInstitutionValue(totalValue);

                institutionTransferIssue.add(newTable);

            }
        }

    }

    public double getGrantIssueQty() {
        return grantIssueQty;
    }

    public void setGrantIssueQty(double grantIssueQty) {
        this.grantIssueQty = grantIssueQty;
    }

    public double getGrantIssueValue() {
        return grantIssueValue;
    }

    public void setGrantIssueValue(double grantIssueValue) {
        this.grantIssueValue = grantIssueValue;
    }

    public void createInstitutionIssue() {
        List<Institution> insList = getCompany();

        institutionIssue = new ArrayList<>();
        grantIssueQty = 0;
        grantIssueValue = 0;

        for (Institution ins : insList) {
            InstitutionSale newTable = new InstitutionSale();
            List<DepartmentSale> list = new ArrayList<>();
            double totalValue = 0;
            double totalQty = 0;
            List<Object[]> objs = calDepartmentIssue(ins);

            for (Object[] obj : objs) {
                DepartmentSale r = new DepartmentSale();
                r.setDepartment((Department) obj[0]);
                r.setSaleValue((Double) obj[1]);
                r.setSaleQty((Double) obj[2]);
                list.add(r);
                //Total Institution Stock
                totalValue += r.getSaleValue();
                totalQty += r.getSaleQty();
                grantIssueValue += r.getSaleValue();
                grantIssueQty += r.getSaleQty();

            }

            if (totalQty != 0 || totalValue != 0) {
                newTable.setDepartmentSales(list);
                newTable.setInstitution(ins);
                newTable.setInstitutionQty(totalQty);
                newTable.setInstitutionValue(totalValue);

                institutionIssue.add(newTable);

            }
        }

    }

    public void createInstitutionTransferReceive() {
        List<Institution> insList = getCompany();

        institutionTransferReceive = new ArrayList<>();
        grantTransferReceiveQty = 0;
        grantTransferReceiveValue = 0;

        for (Institution ins : insList) {
            InstitutionSale newTable = new InstitutionSale();
            List<DepartmentSale> list = new ArrayList<>();
            double totalValue = 0;
            double totalQty = 0;
            List<Object[]> objs = calDepartmentTransferReceive(ins);

            for (Object[] obj : objs) {
                DepartmentSale r = new DepartmentSale();
                r.setDepartment((Department) obj[0]);
                r.setSaleValue((Double) obj[1]);
                r.setSaleQty((Double) obj[2]);
                list.add(r);
                //Total Institution Stock
                totalValue += r.getSaleValue();
                totalQty += r.getSaleQty();
                grantTransferReceiveValue += r.getSaleValue();
                grantTransferReceiveQty += r.getSaleQty();

            }

            if (totalQty != 0 || totalValue != 0) {
                newTable.setDepartmentSales(list);
                newTable.setInstitution(ins);
                newTable.setInstitutionQty(totalQty);
                newTable.setInstitutionValue(totalValue);

                institutionTransferReceive.add(newTable);

            }
        }

    }

    public List<BillItem> getGrns() {
        return grns;
    }

    public void fillDetails() {
        Date startTime = new Date();

        createInstitutionSale();
        createInstitutionBhtIssue();
        createInstitutionStock();
        createInstitutionTransferIssue();
        createInstitutionIssue();
        createInstitutionTransferReceive();
        createGrnTable();
        createPoTable();
        createDirectPurchaseTable();
        createInstitutionIssue();

        
    }

    public void createTable() {
        createInstitutionSale();
        createInstitutionWholeSale();
        createInstitutionBhtIssue();
        createInstitutionStock();
        createInstitutionTransferIssue();
        createInstitutionIssue();
        createInstitutionTransferReceive();
    }

    public void createGrnTable() {

        // //System.err.println("Getting GRNS : ");
        String sql = "Select b From BillItem b where type(b.bill)=:class and b.bill.creater is not null "
                + " and b.bill.cancelled=false and b.retired=false and b.item=:i "
                + " and (b.bill.billType=:btp or b.bill.billType=:btp2) and b.createdAt between :frm and :to order by b.id desc ";
        HashMap hm = new HashMap();
        hm.put("i", pharmacyItem);
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());
        hm.put("class", BilledBill.class);
        hm.put("btp", BillType.PharmacyGrnBill);
        hm.put("btp2", BillType.PharmacyGrnReturn);

        grns = getBillItemFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

//    public void createPhrmacyIssueTable() {
//
//        // //System.err.println("Getting GRNS : ");
//        String sql = "Select b From BillItem b where type(b.bill)=:class and b.bill.creater is not null "
//                + " and b.bill.cancelled=false and b.retired=false and b.item=:i "
//                + " and b.bill.billType=:btp and b.createdAt between :frm and :to order by b.id desc ";
//        HashMap hm = new HashMap();
//        hm.put("i", pharmacyItem);
//        hm.put("frm", getFromDate());
//        hm.put("to", getToDate());
//        hm.put("class", BilledBill.class);
//        hm.put("btp", BillType.PharmacyIssue);
//
//        institutionIssue = getBillItemFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
//
//    }
    public void createDirectPurchaseTable() {

        // //System.err.println("Getting GRNS : ");
        String sql = "Select b From BillItem b where type(b.bill)=:class and b.bill.creater is not null "
                + " and b.bill.cancelled=false and b.retired=false and b.item=:i "
                + " and b.bill.billType=:btp and b.createdAt between :frm and :to order by b.id desc ";
        HashMap hm = new HashMap();
        hm.put("i", pharmacyItem);
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());
        hm.put("btp", BillType.PharmacyPurchaseBill);
        hm.put("class", BilledBill.class);
        directPurchase = getBillItemFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<BillItem> getPos() {
        return pos;
    }

    public void createPoTable() {

        // //System.err.println("Getting POS : ");
        String sql = "Select b From BillItem b where type(b.bill)=:class and b.bill.creater is not null "
                + " and b.bill.cancelled=false and b.retired=false and b.item=:i "
                + " and b.bill.billType=:btp and b.createdAt between :frm and :to order by b.id desc";
        HashMap hm = new HashMap();
        hm.put("i", pharmacyItem);
        hm.put("btp", BillType.PharmacyOrderApprove);
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());
        hm.put("class", BilledBill.class);
        pos = getBillItemFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

        for (BillItem t : pos) {
            //   t.setPharmaceuticalBillItem(getPoQty(t));
            t.setTotalGrnQty(getGrnQty(t));
        }

    }

//    private PharmaceuticalBillItem getPoQty(BillItem b) {
//        String sql = "Select b From PharmaceuticalBillItem b where b.billItem=:bt";
//
//        HashMap hm = new HashMap();
//        hm.put("bt", b);
//
//        return getPharmaceuticalBillItemFacade().findFirstByJpql(sql, hm);
//    }
    private double getGrnQty(BillItem b) {
        String sql = "Select sum(b.pharmaceuticalBillItem.qty) From BillItem b where b.retired=false and b.creater is not null"
                + " and b.bill.cancelled=false and b.bill.billType=:btp and "
                + " b.referanceBillItem=:ref";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", BillType.PharmacyGrnBill);
        double value = getBillFacade().findDoubleByJpql(sql, hm);

//        if (pharmacyItem instanceof Ampp) {
//            value = value / pharmacyItem.getDblValue();
//        }
        return value;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public Item getPharmacyItem() {
        return pharmacyItem;
    }

    public void setPharmacyItem(Item pharmacyItem) {
        makeNull();
        grns = new ArrayList<>();
        this.pharmacyItem = pharmacyItem;
        createInstitutionSale();
        createInstitutionWholeSale();
        createInstitutionBhtIssue();
        createInstitutionStock();
        createInstitutionTransferIssue();
        createInstitutionIssue();
        createInstitutionTransferReceive();
    }

    public double findPharmacyMovement(Department department, Item itm, BillType[] bts, Date fd, Date td) {
        try {
            if (itm instanceof Ampp) {
                itm = ((Ampp) pharmacyItem).getAmp();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String sql;
        Map m = new HashMap();
        m.put("itm", itm);
        m.put("dep", department);
        m.put("frm", fd);
        m.put("to", td);
        List<BillType> bts1 = Arrays.asList(bts);
        m.put("bts", bts1);
        sql = "select sum(i.pharmaceuticalBillItem.qty) "
                + " from BillItem i "
                + " where i.bill.department=:dep"
                + " and i.item=:itm "
                + " and i.bill.billType in :bts "
                + " and i.createdAt between :frm and :to  ";
        return getBillItemFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public Date findFirstPharmacyMovementDate(Department department, Item itm, BillType[] bts, Date fd, Date td) {
        try {
            if (itm instanceof Ampp) {
                itm = ((Ampp) pharmacyItem).getAmp();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String sql;
        Map m = new HashMap();
        m.put("itm", itm);
        m.put("dep", department);
        m.put("frm", fd);
        m.put("to", td);
        List<BillType> bts1 = Arrays.asList(bts);
        m.put("bts", bts1);
        sql = "select i "
                + " from BillItem i "
                + " where i.bill.department=:dep"
                + " and i.item=:itm "
                + " and i.bill.billType in :bts "
                + " and i.bill.createdAt between :frm and :to  "
                + " order by i.id";
        BillItem d = getBillItemFacade().findFirstByJpql(sql, m, TemporalType.TIMESTAMP);
        if (d == null) {
            return fd;
        } else if (d.getBill() != null && d.getBill().getCreatedAt() != null) {
            return d.getBill().getCreatedAt();
        } else if (d.getCreatedAt() != null) {
            return d.getCreatedAt();
        } else {
            return fd;
        }
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public StockFacade getStockFacade() {
        return stockFacade;
    }

    public void setStockFacade(StockFacade stockFacade) {
        this.stockFacade = stockFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfMonth();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        makeNull();
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = commonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        makeNull();
        this.toDate = toDate;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public AmpFacade getAmpFacade() {
        return ampFacade;
    }

    public void setAmpFacade(AmpFacade ampFacade) {
        this.ampFacade = ampFacade;
    }

    public List<DepartmentSale> getDepartmentSale() {
        return departmentSale;
    }

    public void setDepartmentSale(List<DepartmentSale> departmentSale) {
        this.departmentSale = departmentSale;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public List<BillItem> getDirectPurchase() {
        return directPurchase;
    }

    public void setDirectPurchase(List<BillItem> directPurchase) {
        this.directPurchase = directPurchase;
    }

    public List<InstitutionStock> getInstitutionStocks() {
        return institutionStocks;
    }

    public void setInstitutionStocks(List<InstitutionStock> institutionStocks) {
        this.institutionStocks = institutionStocks;
    }

    public List<InstitutionSale> getInstitutionSales() {
        return institutionSales;
    }

    public void setInstitutionSales(List<InstitutionSale> institutionSales) {
        this.institutionSales = institutionSales;
    }

    public List<InstitutionSale> getInstitutionTransferIssue() {
        return institutionTransferIssue;
    }

    public void setInstitutionTransferIssue(List<InstitutionSale> institutionTransferIssue) {
        this.institutionTransferIssue = institutionTransferIssue;
    }

    public void setGrns(List<BillItem> grns) {
        this.grns = grns;
    }

    public void setPos(List<BillItem> pos) {
        this.pos = pos;
    }

    public void setGrantStock(double grantStock) {
        this.grantStock = grantStock;
    }

    public double getGrantSaleQty() {
        return grantSaleQty;
    }

    public void setGrantSaleQty(double grantSaleQty) {
        this.grantSaleQty = grantSaleQty;
    }

    public double getGrantSaleValue() {
        return grantSaleValue;
    }

    public void setGrantSaleValue(double grantSaleValue) {
        this.grantSaleValue = grantSaleValue;
    }

    public double getGrantTransferIssueQty() {
        return grantTransferIssueQty;
    }

    public void setGrantTransferIssueQty(double grantTransferIssueQty) {
        this.grantTransferIssueQty = grantTransferIssueQty;
    }

    public double getGrantTransferIssueValue() {
        return grantTransferIssueValue;
    }

    public void setGrantTransferIssueValue(double grantTransferIssueValue) {
        this.grantTransferIssueValue = grantTransferIssueValue;
    }

    public List<InstitutionSale> getInstitutionTransferReceive() {
        return institutionTransferReceive;
    }

    public void setInstitutionTransferReceive(List<InstitutionSale> institutionTransferReceive) {
        this.institutionTransferReceive = institutionTransferReceive;
    }

    public double getGrantTransferReceiveQty() {
        return grantTransferReceiveQty;
    }

    public void setGrantTransferReceiveQty(double grantTransferReceiveQty) {
        this.grantTransferReceiveQty = grantTransferReceiveQty;
    }

    public double getGrantTransferReceiveValue() {
        return grantTransferReceiveValue;
    }

    public void setGrantTransferReceiveValue(double grantTransferReceiveValue) {
        this.grantTransferReceiveValue = grantTransferReceiveValue;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<StockAverage> getStockAverages() {
        return stockAverages;
    }

    public void setStockAverages(List<StockAverage> stockAverages) {
        this.stockAverages = stockAverages;
    }

    public ItemController getItemController() {
        return itemController;
    }

    public void setItemController(ItemController itemController) {
        this.itemController = itemController;
    }

    public double getPersentage() {
        return persentage;
    }

    public void setPersentage(double persentage) {
        this.persentage = persentage;
    }

    public Department getDepartment() {
        if (department == null) {
            department = getSessionController().getDepartment();
        }
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<ItemTransactionSummeryRow> getItemTransactionSummeryRows() {
        return itemTransactionSummeryRows;
    }

    public void setItemTransactionSummeryRows(List<ItemTransactionSummeryRow> itemTransactionSummeryRows) {
        this.itemTransactionSummeryRows = itemTransactionSummeryRows;
    }

    public boolean isHasSale() {
        return hasSale;
    }

    public void setHasSale(boolean hasSale) {
        this.hasSale = hasSale;
    }

    public boolean isHasWholesale() {
        return hasWholesale;
    }

    public void setHasWholesale(boolean hasWholesale) {
        this.hasWholesale = hasWholesale;
    }

    public boolean isHasInward() {
        return hasInward;
    }

    public void setHasInward(boolean hasInward) {
        this.hasInward = hasInward;
    }

    public boolean isHasIssue() {
        return hasIssue;
    }

    public void setHasIssue(boolean hasIssue) {
        this.hasIssue = hasIssue;
    }

    public boolean isHasTransferOut() {
        return hasTransferOut;
    }

    public void setHasTransferOut(boolean hasTransferOut) {
        this.hasTransferOut = hasTransferOut;
    }

    public boolean isHasPurchase() {
        return hasPurchase;
    }

    public void setHasPurchase(boolean hasPurchase) {
        this.hasPurchase = hasPurchase;
    }

    public boolean isHasTransferIn() {
        return hasTransferIn;
    }

    public void setHasTransferIn(boolean hasTransferIn) {
        this.hasTransferIn = hasTransferIn;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public int getPharmacyAdminIndex() {
        return pharmacyAdminIndex;
    }

    public void setPharmacyAdminIndex(int pharmacyAdminIndex) {
        this.pharmacyAdminIndex = pharmacyAdminIndex;
    }

    public int getPharmacySummaryIndex() {
        return pharmacySummaryIndex;
    }

    public void setPharmacySummaryIndex(int pharmacySummaryIndex) {
        this.pharmacySummaryIndex = pharmacySummaryIndex;
    }

    public int getManagePharamcyReportIndex() {
        return managePharamcyReportIndex;
    }

    public void setManagePharamcyReportIndex(int managePharamcyReportIndex) {
        this.managePharamcyReportIndex = managePharamcyReportIndex;
    }

    public PharmaceuticalItemLight getSelectedLight() {
        return selectedLight;
    }

    public void setSelectedLight(PharmaceuticalItemLight selectedLight) {
        this.selectedLight = selectedLight;
    }

    public List<PharmaceuticalItemLight> getSelectedLights() {
        return selectedLights;
    }

    public void setSelectedLights(List<PharmaceuticalItemLight> selectedLights) {
        this.selectedLights = selectedLights;
    }

    public List<PharmaceuticalItemLight> getAllLights() {
        return allLights;
    }

    public void setAllLights(List<PharmaceuticalItemLight> allLights) {
        this.allLights = allLights;
    }

    public AmpController getAmpController() {
        return ampController;
    }

    public void setAmpController(AmpController ampController) {
        this.ampController = ampController;
    }

    public List<Atm> getAtms() {
        return atms;
    }

    public void setAtms(List<Atm> atms) {
        this.atms = atms;
    }

    public List<Vtm> getVtms() {
        return vtms;
    }

    public void setVtms(List<Vtm> vtms) {
        this.vtms = vtms;
    }

    public List<Vmp> getVmps() {
        return vmps;
    }

    public void setVmps(List<Vmp> vmps) {
        this.vmps = vmps;
    }

    public List<Amp> getAmps() {
        return amps;
    }

    public void setAmps(List<Amp> amps) {
        this.amps = amps;
    }

    public List<Vmpp> getVmpps() {
        return vmpps;
    }

    public void setVmpps(List<Vmpp> vmpps) {
        this.vmpps = vmpps;
    }

    public List<Atm> getAtmsSelected() {
        return atmsSelected;
    }

    public void setAtmsSelected(List<Atm> atmsSelected) {
        this.atmsSelected = atmsSelected;
    }

    public List<Vtm> getVtmsSelected() {
        return vtmsSelected;
    }

    public void setVtmsSelected(List<Vtm> vtmsSelected) {
        this.vtmsSelected = vtmsSelected;
    }

    public List<Vmp> getVmpsSelected() {
        return vmpsSelected;
    }

    public void setVmpsSelected(List<Vmp> vmpsSelected) {
        this.vmpsSelected = vmpsSelected;
    }

    public List<Amp> getAmpsSelected() {
        return ampsSelected;
    }

    public void setAmpsSelected(List<Amp> ampsSelected) {
        this.ampsSelected = ampsSelected;
    }

    public List<Vmpp> getVmppsSelected() {
        return vmppsSelected;
    }

    public void setVmppsSelected(List<Vmpp> vmppsSelected) {
        this.vmppsSelected = vmppsSelected;
    }

    public List<Ampp> getAmppsSelected() {
        return amppsSelected;
    }

    public void setAmppsSelected(List<Ampp> amppsSelected) {
        this.amppsSelected = amppsSelected;
    }

    public Atm getAtm() {
        return atm;
    }

    public void setAtm(Atm atm) {
        this.atm = atm;
    }

    public Vtm getVtm() {
        return vtm;
    }

    public void setVtm(Vtm vtm) {
        this.vtm = vtm;
    }

    public Vmp getVmp() {
        return vmp;
    }

    public void setVmp(Vmp vmp) {
        this.vmp = vmp;
    }

    public Amp getAmp() {
        return amp;
    }

    public void setAmp(Amp amp) {
        this.amp = amp;
    }

    public Vmpp getVmpp() {
        return vmpp;
    }

    public void setVmpp(Vmpp vmpp) {
        this.vmpp = vmpp;
    }

    public Ampp getAmpp() {
        return ampp;
    }

    public void setAmpp(Ampp ampp) {
        this.ampp = ampp;
    }

    public List<Ampp> getAmpps() {
        return ampps;
    }

    public void setAmpps(List<Ampp> ampps) {
        this.ampps = ampps;
    }

    public MeasurementUnit getIssueUnit() {
        return issueUnit;
    }

    public void setIssueUnit(MeasurementUnit issueUnit) {
        this.issueUnit = issueUnit;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

}
