/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.CategoryController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.SessionController;

import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.ItemSupplierPrices;
import com.divudi.core.data.ItemType;
import com.divudi.core.data.SymanticType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Vmp;
import com.divudi.core.entity.pharmacy.Vtm;
import com.divudi.core.entity.pharmacy.VirtualProductIngredient;
import com.divudi.core.facade.AmpFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.facade.VmpFacade;
import com.divudi.core.facade.VirtualProductIngredientFacade;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.model.file.UploadedFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.util.Iterator;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class AmpController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private AmpFacade ejbFacade;
    @EJB
    BillNumberGenerator billNumberBean;
    @EJB
    private VmpFacade vmpFacade;
    @EJB
    private VirtualProductIngredientFacade vivFacade;
    @EJB
    PharmacyBean pharmacyBean;
    @EJB
    StockFacade stockFacade;

    List<Amp> selectedItems;
    private Amp current;
    private Vtm vtm;
    private List<Amp> items = null;
    String selectText = "";
    private String tabId = "tabVmp";
    private VirtualProductIngredient addingVtmInVmp;
    private Vmp currentVmp;

    List<Amp> itemsByCode = null;
    List<Amp> listToRemove = null;
    Department department;
    List<Amp> itemList;
    List<ItemSupplierPrices> itemSupplierPrices;

    @Inject
    SessionController sessionController;
    @Inject
    ItemsDistributorsController itemDistributorsController;
    @Inject
    CategoryController categoryController;
    @Inject
    VmpController vmpController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private ItemController itemController;

    private boolean duplicateCode;
    private boolean editable;

    private UploadedFile file;

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public String navigateToCreateItemList() {
        return "/pharmacy/list_amps?faces-redirect=true"; // Then navigate
    }

    public String navigateToCreateMedicineList() {
        return "/pharmacy/list_medicines?faces-redirect=true"; // Then navigate
    }

    public void uploadAmps() {
        try {
            Workbook workbook = new XSSFWorkbook(file.getInputStream());
            Sheet datatypeSheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = datatypeSheet.iterator();

            while (iterator.hasNext()) {
                Row currentRow = iterator.next();
                Cell categoryNameCell = currentRow.getCell(0); // adjust the index as per your Excel file structure
                Cell vmpNameCell = currentRow.getCell(1); // adjust the index as per your Excel file structure
                Cell ampNameCell = currentRow.getCell(2); // adjust the index as per your Excel file structure
                Cell ampCodeCell = currentRow.getCell(3);
                Cell ampBarCodeCell = currentRow.getCell(4);

                String categoryName = categoryNameCell.getStringCellValue();
                String vmpName = vmpNameCell.getStringCellValue();
                String ampName = ampNameCell.getStringCellValue();
                String ampCode = null;
                if (ampCodeCell.getCellType() == CellType.STRING) {
                    ampCode = ampCodeCell.getStringCellValue();
                } else if (ampCodeCell.getCellType() == CellType.NUMERIC) {
                    double numericCellValue = ampCodeCell.getNumericCellValue();
                    ampCode = BigDecimal.valueOf(numericCellValue).toPlainString();
                }
                String ampBarcode = "";
                if (ampBarCodeCell.getCellType() == CellType.STRING) {
                    ampBarcode = ampBarCodeCell.getStringCellValue();
                } else if (ampBarCodeCell.getCellType() == CellType.NUMERIC) {
                    double numericCellValue = ampBarCodeCell.getNumericCellValue();
                    ampBarcode = BigDecimal.valueOf(numericCellValue).toPlainString();
                }

                Category cat = categoryController.findAndCreateCategoryByName(categoryName);
                Vmp vmp = vmpController.findOrCreateVmpByName(vmpName);

                Amp amp;

                amp = findAmpByName(ampName);
                if (amp == null) {
                    amp = new Amp();
                }
                amp.setCategory(cat);
                amp.setVmp(vmp);
                amp.setName(ampName);
                amp.setCode(ampCode);
                amp.setBarcode(ampBarcode);
                amp.setSymanticType(SymanticType.Pharmacologic_Substance);
                if (amp.getId() == null) {
                    getFacade().create(amp);
                } else {
                    getFacade().edit(amp);
                }
            }

            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String navigateToListAllAmps() {
        String jpql = "Select amp "
                + " from Amp amp "
                + " where amp.retired=:ret "
                + " order by amp.name";
        Map m = new HashMap();
        m.put("ret", false);
        items = getFacade().findByJpql(jpql, m);
        return "/emr/reports/amps?faces-redirect=true";
    }

    public void fillItemsForItemSupplierPrices() {
        List<Amp> amps = getLongCodeItems();
        itemSupplierPrices = new ArrayList<>();
        for (Amp a : amps) {
            ItemSupplierPrices p = new ItemSupplierPrices();
            p.setItem(a);
            p.setAmp(a);
            p.setVmp(a.getVmp());
            itemSupplierPrices.add(p);
        }
//        for (ItemSupplierPrices p : itemSupplierPrices) {
//            p.setPp(getPharmacyBean().getLastPurchaseRate(p.getAmp()));
//        }
//        for (ItemSupplierPrices p : itemSupplierPrices) {
//            p.setSp(getPharmacyBean().getLastRetailRate(p.getAmp()));
//        }
//        for (ItemSupplierPrices p : itemSupplierPrices) {
//            p.setSupplier(itemDistributorsController.getDistributor(p.getAmp()));
//        }
    }

    public void fillPricesForItemSupplierPrices() {
//        List<Amp> amps = getLongCodeItems();
//        itemSupplierPrices = new ArrayList<>();
//        for (Amp a : amps) {
//            ItemSupplierPrices p = new ItemSupplierPrices();
//            p.setItem(a);
//            p.setAmp(a);
//            p.setVmp(a.getVmp());
//            itemSupplierPrices.add(p);
//        }
        for (ItemSupplierPrices p : itemSupplierPrices) {
            p.setPp(getPharmacyBean().getLastPurchaseRate(p.getAmp()));
            p.setSp(getPharmacyBean().getLastRetailRate(p.getAmp()));
        }
//        for (ItemSupplierPrices p : itemSupplierPrices) {
//            p.setSupplier(itemDistributorsController.getDistributor(p.getAmp()));
//        }
    }

    public void fillSuppliersForItemSupplierPrices() {
        for (ItemSupplierPrices p : itemSupplierPrices) {
            p.setSupplier(itemDistributorsController.getDistributor(p.getAmp()));
        }
    }

    public List<Amp> getListToRemove() {
        if (listToRemove == null) {
            listToRemove = new ArrayList<>();
        }
        return listToRemove;
    }

    public void setListToRemove(List<Amp> listToRemove) {
        this.listToRemove = listToRemove;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public StockFacade getStockFacade() {
        return stockFacade;
    }

    public void setStockFacade(StockFacade stockFacade) {
        this.stockFacade = stockFacade;
    }

    public List<Amp> getItemList() {
        return itemList;
    }

    public void setItemList(List<Amp> itemList) {
        this.itemList = itemList;
    }

    public double fetchStockQty(Item item) {

        String sql;
        Map m = new HashMap();
        m.put("i", item);
        sql = "select sum(s.stock) from Stock s where s.itemBatch.item=:i";
        return getStockFacade().findDoubleByJpql(sql, m);
    }

    public void removeSelectedItems() {
        for (Amp s : getListToRemove()) {
            double qty = fetchStockQty(s);

            if (qty != 0) {
                JsfUtil.addErrorMessage(s.getName() + " NOT Removed Beacause there is stock");
                continue;
            }

            s.setRetired(true);
            s.setRetireComments("Bulk Remove");
            s.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(s);
        }

        listToRemove = null;
        createItemList();
    }

    public void createMedicineList() {
        Map m = new HashMap();
        m.put("dep", DepartmentType.Pharmacy);
        String sql = "select c from PharmaceuticalItem c "
                + " where c.retired=false "
                + " and (c.departmentType is null "
                + " or c.departmentType=:dep) "
                + " order by c.name";

        items = getFacade().findByJpql(sql, m);
    }

    public void createItemList() {
        Map m = new HashMap();
        m.put("dep", DepartmentType.Pharmacy);
        String sql = "select c from Amp c "
                + " where c.retired=false "
                + " and (c.departmentType is null "
                + " or c.departmentType=:dep) "
                + " order by c.name";

        items = getFacade().findByJpql(sql, m);
    }

    public void createItemListPharmacy() {
        Map m = new HashMap();
        m.put("dep", DepartmentType.Store);
        m.put("dep2", DepartmentType.Inventry);
        String sql = "select c from Amp c "
                + " where c.retired=false "
                + " and (c.departmentType is null "
                + " or c.departmentType!=:dep "
                + " or c.departmentType!=:dep2 )"
                + " order by c.name ";

        items = getFacade().findByJpql(sql, m);
    }

    public List<Amp> deleteOrNotItem(boolean b, DepartmentType dt) {
        Map m = new HashMap();
        String sql = " select c from Amp c where "
                + " (c.departmentType is null"
                + " or c.departmentType!=:dt )";
        if (b) {
            sql += " and c.retired=false ";
        } else {
            sql += " and c.retired=true ";
        }
        m.put("dt", dt);
        return getFacade().findByJpql(sql, m);
    }

    public List<Amp> deleteOrNotStoreItem(boolean b, DepartmentType dt) {
        Map m = new HashMap();
        String sql = " select c from Amp c where "
                + " c.departmentType=:dt ";
        if (b) {
            sql += " and c.retired=false ";
        } else {
            sql += " and c.retired=true ";
        }
        m.put("dt", dt);
        return getFacade().findByJpql(sql, m);
    }

    public void pharmacyDeleteItem() {
        itemList = deleteOrNotItem(false, DepartmentType.Store);
    }

    public void pharmacyNoDeleteItem() {
        itemList = deleteOrNotItem(true, DepartmentType.Store);
    }

    public void storeDeleteItem() {
        itemList = deleteOrNotStoreItem(false, DepartmentType.Store);
    }

    public void storeNoDeleteItem() {
        itemList = deleteOrNotStoreItem(true, DepartmentType.Store);
    }

    public void onTabChange(TabChangeEvent event) {
        setTabId(event.getTab().getId());
    }

    public List<Amp> getSelectedItems() {
        if (selectText.trim().isEmpty()) {
            selectedItems = getFacade().findByJpql("select c from Amp c where c.retired=false order by c.name");
        } else {
            selectedItems = getFacade().findByJpql("select c from Amp c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        }
        return selectedItems;
    }

    public List<Amp> completeAmp(String qry) {
        List<Amp> a = null;
        Map m = new HashMap();
        m.put("n", "%" + qry + "%");
        if (qry != null) {
            a = getFacade().findByJpql("select c from Amp c where "
                    + " c.retired=false "
                    + " and ((c.name) like :n or (c.code)  "
                    + "like :n or (c.barcode) like :n) order by c.name", m, 30);
        }

        if (a == null) {
            a = new ArrayList<>();
        }
        return a;
    }

    public List<Amp> completeAmpWithRetired(String qry) {
        List<Amp> a = null;
        Map m = new HashMap();
        m.put("n", "%" + qry + "%");
        m.put("dep", DepartmentType.Store);
        if (qry != null) {
            a = getFacade().findByJpql("select c from Amp c where "
                    + " (c.departmentType!=:dep or c.departmentType is null) "
                    + " and ((c.name) like :n or (c.code)  "
                    + "like :n or (c.barcode) like :n) order by c.name", m, 30);
        }

        if (a == null) {
            a = new ArrayList<>();
        }
        return a;
    }

    List<Amp> ampList = null;

    public List<Item> getPharmaceuticalAndStoreItemAmp(String qry) {
        List<Item> a = new ArrayList<>();
        a.addAll(completeAmpWithRetired(qry));
        a.addAll(itemController.completeStoreItemOnlyWithRetired(qry));

        return a;
    }

    public List<Amp> completeAmpByName(String qry) {
        Map<String, Object> m = new HashMap<>();
        m.put("q", "%" + qry + "%");
        m.put("dep", DepartmentType.Store);

        if (qry != null) {
            ampList = getFacade().findByJpql("select c from Amp c where "
                    + " c.retired = false and "
                    + " (c.departmentType is null or c.departmentType != :dep) and "
                    + " (lower(c.name) like lower(:q) or lower(c.code) like lower(:q)) "
                    + " order by c.name", m, 15);
        }

        if (ampList == null) {
            ampList = new ArrayList<>();
        }

        return ampList;
    }

    public Amp findAmpByName(String name) {
        Map m = new HashMap();
        m.put("n", name);
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String jpql = "select c "
                + " from Amp c "
                + " where "
                + " c.retired=:ret"
                + " and c.name=:n";
        m.put("ret", false);
        m.put("n", name);
        Amp amp = getFacade().findFirstByJpql(jpql, m);
        return amp;
    }

    public List<Vmp> completeVmpByName(String qry) {

        List<Vmp> vmps = new ArrayList<>();
        Map m = new HashMap();
        m.put("n", "%" + qry + "%");
        m.put("dep", DepartmentType.Store);
        if (qry != null) {
            vmps = getVmpFacade().findByJpql("select c from Vmp c where "
                    + " c.retired=false and"
                    + " (c.departmentType is null"
                    + " or c.departmentType!=:dep )and "
                    + "((c.name) like :n ) order by c.name", m, 30);
        }
        return vmps;
    }

    public void prepareAddNewVmp() {
        addingVtmInVmp = new VirtualProductIngredient();
    }

//    public List<Amp> completeAmpByCode(String qry) {
//
//        Map m = new HashMap();
//        m.put("n", "%" + qry + "%");
//        m.put("dep", DepartmentType.Store);
//        if (qry != null) {
//            ampList = getFacade().findByJpql("select c from Amp c where "
//                    + " c.retired=false and (c.departmentType is null or c.departmentType!=:dep) and "
//                    + "((c.code) like :n ) order by c.code", m, 30);
//        }
//        if (ampList == null) {
//            ampList = new ArrayList<>();
//        }
//        return ampList;
//    }
//    public List<Amp> completeAmpByBarCode(String qry) {
//
//        Map m = new HashMap();
//        m.put("n", "%" + qry + "%");
//        m.put("dep", DepartmentType.Store);
//        String sql = "select c from Amp c where "
//                + " c.retired=false and c.departmentType!=:dep and "
//                + "((c.barcode) like :n ) order by c.barcode";
//
//        if (qry != null) {
//            ampList = getFacade().findByJpql(sql, m, 30);
//        }
//        if (ampList == null) {
//            ampList = new ArrayList<>();
//        }
//        return ampList;
//    }
    public void prepareAdd() {
        current = new Amp();
        current.setItemType(ItemType.Amp);
        current.setDepartmentType(DepartmentType.Pharmacy);
        editable = true;
    }

    public void edit() {
        if (current == null) {
            JsfUtil.addErrorMessage("Select one to edit");
            return;
        }
        editable = true;
    }

    public void cancel() {
        current = null;
        editable = false;
    }

    public void listnerCategorySelect() {
        if (getCurrent().getCategory() == null) {
            JsfUtil.addErrorMessage("Please Select Category");
            getCurrent().setCode("");
            return;
        }
        if (getCurrent().getCategory().getDescription() == null || getCurrent().getCategory().getDescription().isEmpty()) {
            getCurrent().getCategory().setDescription(getCurrent().getName());
        }

        Map m = new HashMap();
        String sql = "select c from Amp c "
                + " where c.retired=false"
                + " and c.category=:cat "
                + " and c.code is not null "
                + " and (c.departmentType is null "
                + " or c.departmentType=:dep) "
                + " order by c.code desc";

        m.put("dep", DepartmentType.Pharmacy);
        m.put("cat", getCurrent().getCategory());

        Amp amp = getFacade().findFirstByJpql(sql, m);

        DecimalFormat df = new DecimalFormat("0000");
        if (amp != null && !amp.getCode().isEmpty()) {

            String s = amp.getCode().substring(2);

            int i = Integer.parseInt(s);
            i++;
            if (getCurrent().getId() != null) {
                Amp selectedAmp = getFacade().find(getCurrent().getId());
                if (!getCurrent().getCategory().equals(selectedAmp.getCategory())) {
                    getCurrent().setCode(getCurrent().getCategory().getDescription() + df.format(i));
                } else {
                    getCurrent().setCode(selectedAmp.getCode());
                }
            } else {
                getCurrent().setCode(getCurrent().getCategory().getDescription() + df.format(i));
            }
        } else {
            getCurrent().setCode(getCurrent().getCategory().getDescription() + df.format(1));
        }

    }

    public void setSelectedItems(List<Amp> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {

        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    private boolean errorCheck() {
//        if (getCurrent().getInstitution() == null) {
//            JsfUtil.addErrorMessage("Please Select Manufacturer");
//            return true;
//        }

//        listnerCategorySelect();
        if (current.getCategory() == null) {
//            listnerCategorySelect();
            JsfUtil.addErrorMessage("Please Select Category");
            return true;
        }

        if (getTabId().equals("tabVmp")) {
            if (getCurrent().getVmp() == null) {
                JsfUtil.addErrorMessage("Please Select VMP");
                return true;
            }
        }
        if (getCurrent().getCode() == null || getCurrent().getCode().isEmpty()) {
            JsfUtil.addErrorMessage("Code Empty.You Can't Save Item without Code.");
            return true;
        }

        return false;
    }

    private boolean errorCheckForGen() {
        if (addingVtmInVmp == null) {
            return true;
        }
        if (addingVtmInVmp.getVtm() == null) {
            JsfUtil.addErrorMessage("Select Vtm");
            return true;
        }

        if (currentVmp == null) {
            return true;
        }
        if (addingVtmInVmp.getStrength() == 0.0) {
            JsfUtil.addErrorMessage("Type Strength");
            return true;
        }
        if (currentVmp.getCategory() == null) {
            JsfUtil.addErrorMessage("Select Category");
            return true;
        }
        if (addingVtmInVmp.getStrengthUnit() == null) {
            JsfUtil.addErrorMessage("Select Strenth Unit");
            return true;
        }

        return false;
    }

    public String createVmpName() {
        return addingVtmInVmp.getVtm().getName()
                + " " + addingVtmInVmp.getStrength()
                + " " + addingVtmInVmp.getStrengthUnit().getName()
                + " " + currentVmp.getCategory().getName();
    }

    public String createAmpName() {
        if (getTabId().equals("tabGen")) {
            return getCurrentVmp().getName();
        } else {
            return getCurrent().getVmp().getName();
        }
    }

    private void saveVmp() {
        if (currentVmp.getName() == null || currentVmp.getName().isEmpty()) {
            currentVmp.setName(createVmpName());
        }

        if (currentVmp.getId() == null || currentVmp.getId() == 0) {
            getVmpFacade().create(currentVmp);
        } else {
            getVmpFacade().edit(currentVmp);
        }

    }

    public void save() {
        if (current == null) {
            JsfUtil.addErrorMessage("No AMP is selected");
            return;
        }

        if (current.getId() != null) {
            if (!configOptionApplicationController.getBooleanValueByKey("Enable edit and delete AMP from Pharmacy Administration.", false)) {
                JsfUtil.addErrorMessage("Deleting and Editing is disabled by Configuration Options.");
                return;
            }
        }

        if (current.getName() == null || current.getName().isEmpty()) {
            JsfUtil.addErrorMessage("Please add a name to AMP");
            return;
        }

        int maxCodeLeanth = Integer.parseInt(configOptionApplicationController.getShortTextValueByKey("Minimum Number of Characters to Search for Item", "4"));

        if (current.getCode().trim().length() < maxCodeLeanth) {
            JsfUtil.addErrorMessage("Minimum " + maxCodeLeanth + " characters are Required for Item Code");
            return;
        }

        if (checkItemCode(current.getCode(), current)) {
            JsfUtil.addErrorMessage("This Code has Already been Used.");
            return;
        }
        if (current.getDepartmentType() == null) {
            current.setDepartmentType(DepartmentType.Pharmacy);
        }
        if (current.getVmp() == null) {
            JsfUtil.addErrorMessage("No VMP selected");
            return;
        }

        if (current.getCategory() == null) {
            if (current.getVmp().getCategory() != null) {
                current.setCategory(current.getVmp().getCategory());
                JsfUtil.addSuccessMessage("Taken the category from VMP");
            } else {
                JsfUtil.addErrorMessage("No category");
                return;
            }
        }

        if (current.getItemType() == null) {
            current.setItemType(ItemType.Amp);
        }

        if (getCurrent().getId() != null) {
            getCurrent().setEditedAt(new Date());
            getCurrent().setEditer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
            recreateModel();
            getItems();
        }
    }

    public boolean checkItemCode(String code, Amp savingAmp) {
        if (savingAmp == null) {
            return false;
        }
        Map m = new HashMap();
        String jpql = "select c from Amp c "
                + " where c.retired=false"
                + " and (c.code is not null and c.code=:icode)";
        if (savingAmp.getId() != null) {
            jpql += " and c.id <> :id ";
            m.put("id", savingAmp.getId());
        }
        m.put("icode", code);
        Amp amp = getFacade().findFirstByJpql(jpql, m);
        return amp != null;
    }

    public void checkCodeDuplicate() {
        duplicateCode = checkItemCode(current.getCode(), current);
        if (duplicateCode) {
            JsfUtil.addErrorMessage("This Code has Already been Used.");
        }
    }

    public void generateCode() {
        int length = configOptionApplicationController.getIntegerValueByKey("AMP_CODE_LENGTH", 4);
        String code = "";

        if (configOptionApplicationController.getBooleanValueByKey("AMP_CODE_NUMERIC_ONLY")) {
            code = generateNumericCode(length);
        } else if (configOptionApplicationController.getBooleanValueByKey("AMP_CODE_CHARACTERS_ONLY")) {
            code = generateCharacterCode(length);
        } else if (configOptionApplicationController.getBooleanValueByKey("AMP_CODE_ALPHANUMERIC")) {
            code = generateAlphaNumericCode(length);
        } else {
            // Default fallback if no generation mode is configured - use numeric
            code = generateNumericCode(length);
            JsfUtil.addSuccessMessage("Generated numeric code (default mode). Configure AMP_CODE_* options for other formats.");
        }

        if (code != null && !code.trim().isEmpty()) {
            current.setCode(code);
            checkCodeDuplicate();
            if (!duplicateCode) {
                JsfUtil.addSuccessMessage("Unique code generated successfully: " + code);
            }
        } else {
            JsfUtil.addErrorMessage("Failed to generate code. Please check configuration.");
        }
    }

    private String generateNumericCode(int length) {
        long max = 0;
        List<Amp> all = findItems();
        for (Amp a : all) {
            try {
                long val = Long.parseLong(a.getCode());
                if (val > max) {
                    max = val;
                }
            } catch (Exception e) {
            }
        }
        long next = max + 1;
        String format = "%0" + length + "d";
        String code = String.format(format, next);
        while (checkItemCode(code, current)) {
            next++;
            code = String.format(format, next);
        }
        return code;
    }

    private String generateCharacterCode(int length) {
        String base = generateShortCode(current.getName());
        if (base.isEmpty()) {
            base = "AMP"; // Default fallback
        }
        if (base.length() > length) {
            base = base.substring(0, length);
        }
        String code = base.toUpperCase();
        int index = 1;
        while (checkItemCode(code, current)) {
            String suffix = String.valueOf(index);
            int cut = Math.max(0, length - suffix.length());
            String prefix = base.length() > cut ? base.substring(0, cut) : base;
            code = (prefix + suffix).toUpperCase();
            index++;
        }
        if (code.length() > length) {
            code = code.substring(0, length);
        }
        return code;
    }

    private String generateAlphaNumericCode(int length) {
        String base = generateShortCode(current.getName()).toUpperCase();
        if (base.length() >= length) {
            base = base.substring(0, length - 1);
        }
        int digits = Math.max(1, length - base.length());
        long index = 1;
        String code;
        String format = "%0" + digits + "d";
        code = base + String.format(format, index);
        while (checkItemCode(code, current)) {
            index++;
            code = base + String.format(format, index);
        }
        return code;
    }

    private String generateShortCode(String name) {
        StringBuilder sc = new StringBuilder();
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        String[] words = name.split(" ");
        if (words.length == 1 && words[0].length() >= 3) {
            sc = new StringBuilder(words[0].substring(0, 3).toLowerCase());
        } else {
            for (String w : words) {
                if (!w.isEmpty()) {
                    sc.append(w.charAt(0));
                }
            }
            sc = new StringBuilder(sc.toString().toLowerCase());
        }
        return sc.toString();
    }

    public void saveSelected() {
        if (errorCheck()) {
            return;
        }
//
//        if (getTabId().toString().equals("tabGen")) {
//            if (errorCheckForGen()) {
//                return;
//            }
//
//            saveVmp();
//            getAddingVtmInVmp().setVmp(currentVmp);
//            if (getAddingVtmInVmp().getId() == null || getAddingVtmInVmp().getId() == null) {
//                getVivFacade().create(getAddingVtmInVmp());
//            } else {
//                getVivFacade().edit(getAddingVtmInVmp());
//            }
//
//            getCurrent().setVmp(currentVmp);
//        }

        if (current.getName() == null || current.getName().isEmpty()) {
            current.setName(createAmpName());
        }

        current.setDepartmentType(DepartmentType.Pharmacy);

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        // getItems();
        editable = false;
    }

    public void saveAmp(Amp amp) {
        if (amp == null) {
            return;
        }
        if (amp.getId() == null) {
            getFacade().create(amp);
        } else {
            getFacade().edit(amp);
        }
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public AmpFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(AmpFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public AmpController() {
    }

    public Amp getCurrent() {
        if (current == null) {
            current = new Amp();
        }
        return current;
    }

    public void setCurrent(Amp current) {
        this.current = current;
        currentVmp = new Vmp();
        addingVtmInVmp = new VirtualProductIngredient();
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
        editable = false;
    }

    private AmpFacade getFacade() {
        return ejbFacade;
    }
    private List<Amp> filteredItems;

    public List<Amp> getItems() {
        if (items == null) {
            items = findItems();
        }
        return items;
    }

    public List<Amp> findItems() {
        String jpql = "select i "
                + " from Amp i "
                + " where i.retired=:ret"
                + " order by i.name";
        Map m = new HashMap();
        m.put("ret", false);
        return getFacade().findByJpql(jpql, m);
    }

    public List<Amp> getLongCodeItems() {
        List<Amp> lst;
        String sql;
        sql = "select a from Amp a where a.retired=false and length(a.code) > 5";
        lst = getFacade().findByJpql(sql);
        return lst;
    }

    public Vtm getVtm() {
        return vtm;
    }

    public void setVtm(Vtm vtm) {
        this.vtm = vtm;
    }

    public String getTabId() {
        return tabId;
    }

    public void setTabId(String tabId) {
        this.tabId = tabId;
    }

    public VirtualProductIngredient getAddingVtmInVmp() {
        if (addingVtmInVmp == null) {
            addingVtmInVmp = new VirtualProductIngredient();
        }
        return addingVtmInVmp;
    }

    public void setAddingVtmInVmp(VirtualProductIngredient addingVtmInVmp) {
        this.addingVtmInVmp = addingVtmInVmp;
    }

    public Vmp getCurrentVmp() {
        if (currentVmp == null) {
            currentVmp = new Vmp();
        }
        return currentVmp;
    }

    public void setCurrentVmp(Vmp currentVmp) {
        this.currentVmp = currentVmp;
        getCurrent().setVmp(currentVmp);
    }

    public VmpFacade getVmpFacade() {
        return vmpFacade;
    }

    public void setVmpFacade(VmpFacade vmpFacade) {
        this.vmpFacade = vmpFacade;
    }

    public VirtualProductIngredientFacade getVivFacade() {
        return vivFacade;
    }

    public void setVivFacade(VirtualProductIngredientFacade vivFacade) {
        this.vivFacade = vivFacade;
    }

    public List<Amp> getFilteredItems() {
        return filteredItems;
    }

    public void setFilteredItems(List<Amp> filteredItems) {
        this.filteredItems = filteredItems;

    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<ItemSupplierPrices> getItemSupplierPrices() {
        return itemSupplierPrices;
    }

    public void setItemSupplierPrices(List<ItemSupplierPrices> itemSupplierPrices) {
        this.itemSupplierPrices = itemSupplierPrices;
    }

    public void setItems(List<Amp> items) {
        this.items = items;
    }

    public ConfigOptionApplicationController getConfigOptionApplicationController() {
        return configOptionApplicationController;
    }

    public void setConfigOptionApplicationController(ConfigOptionApplicationController configOptionApplicationController) {
        this.configOptionApplicationController = configOptionApplicationController;
    }

    public boolean isDuplicateCode() {
        return duplicateCode;
    }

    public void setDuplicateCode(boolean duplicateCode) {
        this.duplicateCode = duplicateCode;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    // ========== BULK CODE GENERATION METHODS ==========

    /**
     * Finds all AMPs that need code generation or code improvement.
     * This includes AMPs with:
     * 1. Missing codes (null or empty)
     * 2. Codes shorter than the minimum length (default: 4 characters)
     */
    public List<Amp> findAmpsNeedingCodeGeneration() {
        int minCodeLength = configOptionApplicationController.getIntegerValueByKey("AMP_CODE_LENGTH", 4);

        String jpql = "select a from Amp a "
                + " where a.retired = false "
                + " and (a.code is null or a.code = '' or length(a.code) < :minLength)"
                + " order by a.name";

        Map<String, Object> params = new HashMap<>();
        params.put("minLength", minCodeLength);

        return getFacade().findByJpql(jpql, params);
    }

    /**
     * Gets count of AMPs that need code generation
     */
    public int getAmpsNeedingCodeGenerationCount() {
        int minCodeLength = configOptionApplicationController.getIntegerValueByKey("AMP_CODE_LENGTH", 4);

        String jpql = "select count(a) from Amp a "
                + " where a.retired = false "
                + " and (a.code is null or a.code = '' or length(a.code) < :minLength)";

        Map<String, Object> params = new HashMap<>();
        params.put("minLength", minCodeLength);

        Long count = getFacade().findLongByJpql(jpql, params);
        return count != null ? count.intValue() : 0;
    }

    /**
     * Prepares the bulk code generation by showing preview
     */
    public void prepareBulkCodeGeneration() {
        List<Amp> ampsToUpdate = findAmpsNeedingCodeGeneration();
        if (ampsToUpdate.isEmpty()) {
            JsfUtil.addSuccessMessage("All AMPs already have proper codes. No action needed.");
            return;
        }

        String message = String.format("Found %d AMPs that need code generation/improvement:%n%n", ampsToUpdate.size());
        StringBuilder details = new StringBuilder();
        int displayLimit = 10; // Show first 10 items

        for (int i = 0; i < Math.min(ampsToUpdate.size(), displayLimit); i++) {
            Amp amp = ampsToUpdate.get(i);
            String currentCode = amp.getCode();
            details.append(String.format("- %s (Current code: %s)%n",
                amp.getName(),
                (currentCode == null || currentCode.trim().isEmpty()) ? "MISSING" : "'" + currentCode + "'"));
        }

        if (ampsToUpdate.size() > displayLimit) {
            details.append(String.format("... and %d more items%n", ampsToUpdate.size() - displayLimit));
        }

        JsfUtil.addInfoMessage(message + details.toString());
    }

    /**
     * Performs bulk code generation for all AMPs that need it
     */
    public void performBulkCodeGeneration() {
        List<Amp> ampsToUpdate = findAmpsNeedingCodeGeneration();
        if (ampsToUpdate.isEmpty()) {
            JsfUtil.addSuccessMessage("All AMPs already have proper codes. No action needed.");
            return;
        }

        int successCount = 0;
        int errorCount = 0;
        int minCodeLength = configOptionApplicationController.getIntegerValueByKey("AMP_CODE_LENGTH", 4);

        StringBuilder errors = new StringBuilder();

        for (Amp amp : ampsToUpdate) {
            try {
                String existingCode = amp.getCode();
                String newCode = null;

                if (existingCode == null || existingCode.trim().isEmpty()) {
                    // Generate new code
                    newCode = generateCodeForAmp(amp);
                } else if (existingCode.length() < minCodeLength) {
                    // Pad existing short code with leading zeros
                    newCode = padCodeWithZeros(existingCode, minCodeLength);
                }

                if (newCode != null && !newCode.trim().isEmpty()) {
                    // Verify uniqueness
                    if (!checkItemCode(newCode, amp)) {
                        amp.setCode(newCode);
                        amp.setEditedAt(new Date());
                        amp.setEditer(getSessionController().getLoggedUser());
                        getFacade().edit(amp);
                        successCount++;
                    } else {
                        errorCount++;
                        errors.append(String.format("Failed to generate unique code for %s%n", amp.getName()));
                    }
                } else {
                    errorCount++;
                    errors.append(String.format("Failed to generate code for %s%n", amp.getName()));
                }

            } catch (Exception e) {
                errorCount++;
                errors.append(String.format("Error processing %s: %s%n", amp.getName(), e.getMessage()));
            }
        }

        // Clear cache and refresh
        recreateModel();

        // Report results
        if (successCount > 0) {
            JsfUtil.addSuccessMessage(String.format("Successfully generated codes for %d AMPs.", successCount));
        }

        if (errorCount > 0) {
            JsfUtil.addErrorMessage(String.format("%d AMPs had errors:%n%s", errorCount, errors.toString()));
        }
    }

    /**
     * Generates a code for a specific AMP using current configuration
     */
    private String generateCodeForAmp(Amp amp) {
        int length = configOptionApplicationController.getIntegerValueByKey("AMP_CODE_LENGTH", 4);

        // Temporarily set the amp as current to use existing generation methods
        Amp originalCurrent = this.current;
        this.current = amp;

        String code = "";
        try {
            if (configOptionApplicationController.getBooleanValueByKey("AMP_CODE_NUMERIC_ONLY")) {
                code = generateNumericCode(length);
            } else if (configOptionApplicationController.getBooleanValueByKey("AMP_CODE_CHARACTERS_ONLY")) {
                code = generateCharacterCode(length);
            } else if (configOptionApplicationController.getBooleanValueByKey("AMP_CODE_ALPHANUMERIC")) {
                code = generateAlphaNumericCode(length);
            } else {
                // Default to numeric
                code = generateNumericCode(length);
            }
        } finally {
            // Restore original current
            this.current = originalCurrent;
        }

        return code;
    }

    /**
     * Pads a short code with leading zeros to reach minimum length
     */
    private String padCodeWithZeros(String existingCode, int minLength) {
        if (existingCode == null) {
            return null;
        }

        String trimmedCode = existingCode.trim();
        if (trimmedCode.length() >= minLength) {
            return trimmedCode;
        }

        // Check if the code is numeric - if so, pad with zeros
        try {
            Long.parseLong(trimmedCode);
            // It's numeric, pad with leading zeros
            String format = "%0" + minLength + "d";
            return String.format(format, Long.parseLong(trimmedCode));
        } catch (NumberFormatException e) {
            // It's not numeric, pad with zeros at the beginning
            StringBuilder padded = new StringBuilder();
            int zerosNeeded = minLength - trimmedCode.length();
            for (int i = 0; i < zerosNeeded; i++) {
                padded.append("0");
            }
            padded.append(trimmedCode);
            return padded.toString();
        }
    }

    /**
     *
     */
    @FacesConverter(forClass = Amp.class)
    public static class AmpConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            AmpController controller = (AmpController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "ampController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            long key;
            try {
                key = Long.parseLong(value);
            } catch (Exception e) {
                key = 0L;
            }
            return key;
        }

        String getStringKey(java.lang.Long value) {
            return String.valueOf(value);
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Amp) {
                Amp o = (Amp) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + AmpController.class.getName());
            }
        }
    }
}
