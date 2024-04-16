package com.divudi.bean.common;

import com.divudi.data.DepartmentItemCount;
import com.divudi.data.DepartmentType;
import com.divudi.data.FeeType;
import com.divudi.data.InstitutionItemCount;
import com.divudi.data.ItemLight;
import com.divudi.data.ItemType;
import com.divudi.data.dataStructure.ItemFeeRow;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.entity.BillExpense;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.entity.Packege;
import com.divudi.entity.Service;
import com.divudi.entity.ServiceCategory;
import com.divudi.entity.ServiceSubCategory;
import com.divudi.entity.inward.InwardService;
import com.divudi.entity.inward.TheatreService;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.ItemForItem;
import com.divudi.entity.lab.Machine;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.Ampp;
import com.divudi.entity.pharmacy.PharmaceuticalItem;
import com.divudi.entity.pharmacy.Vmp;
import com.divudi.entity.pharmacy.Vmpp;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.bean.common.util.JsfUtil;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.apache.commons.beanutils.BeanUtils;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class ItemController implements Serializable {

    /**
     * EJBs
     */
    private static final long serialVersionUID = 1L;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private ItemFeeFacade itemFeeFacade;
    /**
     * Managed Beans
     */
    @Inject
    SessionController sessionController;
    @Inject
    ItemMappingController itemMappingController;
    @Inject
    ItemFeeManager itemFeeManager;
    @Inject
    DepartmentController departmentController;
    @Inject
    InstitutionController institutionController;
    @Inject
    ItemForItemController itemForItemController;
    @Inject
    ItemFeeController itemFeeController;
    @Inject
    ServiceController serviceController;
    @Inject
    ItemApplicationController itemApplicationController;

    /**
     * Properties
     */
    private Item current;
    private Item sampleComponent;
    private List<Item> items = null;
    private List<Item> investigationsAndServices = null;
    private List<Item> itemlist;
    List<ItemLight> allItems;
    private List<ItemLight> filteredItems;
    private ItemLight selectedItemLight;
    private List<ItemLight> departmentItems;
    private List<ItemLight> institutionItems;
    private List<ItemLight> ccDeptItems;
    private List<ItemLight> ccInstitutionItems;
    List<ItemFee> allItemFees;
    List<Item> selectedList;
    List<ItemFee> selectedItemFeeList;
    private Institution institution;
    private Department department;
    private Institution filterInstitution;
    private Department filterDepartment;
    FeeType feeType;
    List<Department> departments;
    private Machine machine;
    private List<Item> machineTests;
    private List<Item> investigationSampleComponents;
    private List<ItemFee> ItemFeesList;
    private List<ItemFeeRow> itemFeeRows;
    private List<DepartmentItemCount> departmentItemCounts;
    private DepartmentItemCount departmentItemCount;
    private List<InstitutionItemCount> institutionItemCounts;
    private InstitutionItemCount institutionItemCount;

    boolean masterItem;

    ReportKeyWord reportKeyWord;

    public void processDepartmentItemCount() {
        // Query for count of items without a department
        String jpqlWithoutDept = "select count(i) "
                + "from Item i "
                + "where i.retired=:ret "
                + "and i.department is null "
                + "and (TYPE(i)=:ix or TYPE(i)=:sv)";

        // Query for items with departments
        String jpqlWithDept = "select new com.divudi.data.DepartmentItemCount("
                + "i.department.id, i.department.name, count(i)) "
                + "from Item i "
                + "where i.retired=:ret "
                + "and i.department is not null "
                + "and (TYPE(i)=:ix or TYPE(i)=:sv) "
                + "group by i.department.id, i.department.name "
                + "order by i.department.name";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("ix", Investigation.class);
        m.put("sv", Service.class);

        // Get count of items without a department
        Long countWithoutDepartment = itemFacade.countByJpql(jpqlWithoutDept, m);
        DepartmentItemCount icWithoutDept = new DepartmentItemCount(-1L, "No Department", countWithoutDepartment);

        // Get list of items with a department
        List<DepartmentItemCount> withDeptList = (List<DepartmentItemCount>) itemFacade.findLightsByJpql(jpqlWithDept, m);

        // Create final list and add count for items without a department first
        departmentItemCounts = new ArrayList<>();
        departmentItemCounts.add(icWithoutDept);
        departmentItemCounts.addAll(withDeptList);
    }

    public void processInstitutionItemCount() {
        // Query for count of items without an institution
        String jpqlWithoutIns = "select count(i) "
                + "from Item i "
                + "where i.retired=:ret "
                + "and i.institution is null "
                + "and (TYPE(i)=:ix or TYPE(i)=:sv)";

        // Query for items with institutions
        String jpqlWithIns = "select new com.divudi.data.InstitutionItemCount("
                + "i.institution.id, i.institution.name, i.institution.code, count(i)) "
                + "from Item i "
                + "where i.retired=:ret "
                + "and i.institution is not null "
                + "and (TYPE(i)=:ix or TYPE(i)=:sv) "
                + "group by i.institution.id, i.institution.name, i.institution.code "
                + "order by i.institution.name";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("ix", Investigation.class);
        m.put("sv", Service.class);

        // Get count of items without an institution
        Long countWithoutInstitution = itemFacade.countByJpql(jpqlWithoutIns, m);
        InstitutionItemCount icWithout = new InstitutionItemCount(-1L, "No Institution", "No Code", countWithoutInstitution);

        // Get list of items with an institution
        List<InstitutionItemCount> withInsList = (List<InstitutionItemCount>) itemFacade.findLightsByJpql(jpqlWithIns, m);

        // Create final list and add count for items without an institution first
        institutionItemCounts = new ArrayList<>();
        institutionItemCounts.add(icWithout);
        institutionItemCounts.addAll(withInsList);
    }

    public List<ItemFee> fetchItemFeeList() {
        List<ItemFee> itemFees = new ArrayList<>();
        String sql;
        sql = "select c from ItemFee c "
                + " where c.retired=false order by c.name ";
        ItemFeesList = getItemFeeFacade().findByJpql(sql);
        return ItemFeesList;
    }

    private List<ItemFee> fetchItemFeesForItem(Item item) {
        String sql = "select c from ItemFee c where c.item.id = :itemId and c.retired=false";
        Map<String, Object> params = new HashMap<>();
        params.put("itemId", item.getId());
        return getItemFeeFacade().findByJpql(sql, params);
    }

    public void fillFilteredItems() {
        filteredItems = fillItems();
    }

    public String fillDepartmentItems() {
        if (filterDepartment == null) {
            JsfUtil.addErrorMessage("Select dept");
            return null;
        }
        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.data.ItemLight("
                + "i.id, "
                + "i.name, "
                + "i.code, "
                + "i.total) "
                + "FROM Item i "
                + "WHERE i.retired = :ret "
                + "AND (TYPE(i)=:ixc OR TYPE(i)=:svc) "
                + "AND i.department = :dep "
                + "ORDER BY i.name";

        parameters.put("ret", false);
        parameters.put("ixc", Investigation.class);
        parameters.put("svc", Service.class);
        parameters.put("dep", filterDepartment);

        filteredItems = (List<ItemLight>) itemFacade.findLightsByJpql(jpql, parameters);
        return "/admin/items/list?faces-redirect=true;";
    }

    public String fillInstitutionItems() {
        if (filterInstitution == null) {
            JsfUtil.addErrorMessage("Select institution");
            return null;
        }

        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.data.ItemLight("
                + "i.id, "
                + "i.name, "
                + "i.code, "
                + "i.total) "
                + "FROM Item i "
                + "WHERE i.retired = :ret "
                + "AND (TYPE(i)=:ixc OR TYPE(i)=:svc) "
                + "AND i.institution = :ins "
                + "ORDER BY i.name";

        parameters.put("ret", false);
        parameters.put("ixc", Investigation.class);
        parameters.put("svc", Service.class);
        parameters.put("ins", filterInstitution);

        filteredItems = (List<ItemLight>) itemFacade.findLightsByJpql(jpql, parameters);
        return "/admin/items/list?faces-redirect=true;";
    }

    public String fillItemsWithoutDepartment() {
        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.data.ItemLight("
                + "i.id, "
                + "i.name, "
                + "i.code, "
                + "i.total) "
                + "FROM Item i "
                + "WHERE i.retired = :ret "
                + "AND i.department IS NULL "
                + "AND (TYPE(i)=:ixc OR TYPE(i)=:svc) "
                + "ORDER BY i.name";

        parameters.put("ret", false);
        parameters.put("ixc", Investigation.class);
        parameters.put("svc", Service.class);
        filteredItems = (List<ItemLight>) itemFacade.findLightsByJpql(jpql, parameters);
        return "/admin/items/list?faces-redirect=true;";
    }

    public String fillItemsWithoutInstitution() {
        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.data.ItemLight("
                + "i.id, "
                + "i.name, "
                + "i.code, "
                + "i.total) "
                + "FROM Item i "
                + "WHERE i.retired = :ret "
                + "AND i.institution IS NULL "
                + "AND (TYPE(i)=:ixc OR TYPE(i)=:svc) "
                + "ORDER BY i.name";

        parameters.put("ret", false);
        parameters.put("ixc", Investigation.class);
        parameters.put("svc", Service.class);

        filteredItems = (List<ItemLight>) itemFacade.findLightsByJpql(jpql, parameters);
        return "/admin/items/list?faces-redirect=true;";
    }

    public String navigateToListItemsOfSelectedDepartment() {
        if (departmentItemCount == null) {
            JsfUtil.addErrorMessage("Select dept");
            return "";
        }
        filterInstitution = null;
        if (departmentItemCount.getDepartmentId() == null) {
            filterDepartment = null;
            return fillItemsWithoutDepartment();
        } else {
            filterDepartment = departmentController.findDepartment(departmentItemCount.getDepartmentId());
            return fillDepartmentItems();
        }
    }

    public String navigateToListItemsOfSelectedInstitution() {
        if (institutionItemCount == null) {
            JsfUtil.addErrorMessage("Select dept");
            return "";
        }
        filterDepartment = null;
        if (institutionItemCount.getInstitutionId() == null || institutionItemCount.getInstitutionId() < 1l) {
            filterInstitution = null;
            return fillItemsWithoutInstitution();
        } else {
            filterInstitution = institutionController.findInstitution(institutionItemCount.getInstitutionId());
            return fillInstitutionItems();
        }
    }

    public List<ItemLight> fillItems() {
        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.data.ItemLight("
                + "i.id, i.orderNo, i.isMasterItem, i.hasReportFormat, "
                + "c.name, c.id, ins.name, ins.id, "
                + "d.name, d.id, s.name, s.id, "
                + "p.name, stf.id, i.name, i.code, i.barcode, "
                + "i.printName, i.shortName, i.fullName, i.total) "
                + "FROM Item i "
                + "LEFT JOIN i.category c "
                + "LEFT JOIN i.institution ins "
                + "LEFT JOIN i.department d "
                + "LEFT JOIN i.speciality s "
                + "LEFT JOIN i.staff stf "
                + "LEFT JOIN stf.person p "
                + "WHERE i.retired = :ret "
                + "AND (TYPE(i)=:ixc OR TYPE(i)=:svc) ";

        if (filterInstitution != null) {
            jpql += "AND i.institution=:ins ";
            parameters.put("ins", filterInstitution);
        } else {
            jpql += "AND i.institution IS NULL ";
        }

        if (filterDepartment != null) {
            jpql += "AND i.department=:dep ";
            parameters.put("dep", filterDepartment);
        } else {
            jpql += "AND i.department IS NULL ";
        }

        parameters.put("ret", false);
        parameters.put("ixc", Investigation.class);
        parameters.put("svc", Service.class);

        jpql += "ORDER BY i.name";

        List<ItemLight> lst = (List<ItemLight>) itemFacade.findLightsByJpql(jpql, parameters);
        return lst;
    }

    public String navigateToListAllItems() {
        allItems = null;
        return "/item/reports/item_list";
    }

    public String navigateToListFilteredItems() {
        filteredItems = null;
        return "/admin/items/list";
    }

    public String navigateToListAllItemsForAdmin() {
        allItems = null;
        return "/admin/items/list";
    }

    public void fillInvestigations() {
        fillItemsByType(Investigation.class);
    }

    public void fillServices() {
        fillItemsByType(Service.class);
    }

    public void fillMedicines() {
        fillItemsByType(PharmaceuticalItem.class);
    }

    public void fillItemsByType(Class it) {
        String jpql = "select i "
                + " from Item i"
                + " where type(i)=:scs "
                + " order by i.name";
        Map m = new HashMap();
        m.put("scs", it);
        allItems = getFacade().findByJpql(jpql, m);
    }

    public String toManageItemdIndex() {
        return "/admin/items/index";
    }

    public String toListInvestigations() {
        fillInvestigations();
        return "/admin/investigations";
    }

    public String toAddNewInvestigation() {
        current = new Investigation();
        return "/admin/investigation";
    }

    public String toEditInvestigation() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        return "/admin/institution";
    }

    public String deleteInvestigation() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        current.setRetired(true);
        getFacade().edit(current);
        return toListInvestigations();
    }

    public String saveSelectedInvestigation() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        if (current.getId() == null) {
            getFacade().create(current);
        } else {
            getFacade().edit(current);
        }
        return toListInvestigations();
    }

    public Item findItemByCode(String code) {
        String jpql;
        Map m = new HashMap();
        jpql = "select i "
                + " from Item i "
                + " where i.retired=:ret "
                + " and i.code=:code";
        m.put("ret", false);
        m.put("code", code);
        Item item = getFacade().findFirstByJpql(jpql, m);
        if (item == null) {
            jpql = "select i "
                    + " from Item i "
                    + " where i.code=:code";
            m = new HashMap();
            m.put("code", code);
            item = getFacade().findFirstByJpql(jpql, m);
            if (item != null) {
                item.setRetired(false);
                getFacade().edit(item);
            } else {
                item = new Item();
                item.setName(code);
                item.setCode(code);
                getFacade().create(item);
            }
        }
        return item;
    }

    public Item findItem(Long id) {
        return getFacade().find(id);
    }

    public Item findItemByCode(String code, String parentCode) {
        Item parentItem = findItemByCode(parentCode);
        String jpql;
        Map m = new HashMap();
        jpql = "select i "
                + " from Item i "
                + " where i.retired=:ret "
                + " and i.parentItem=:pi "
                + " and i.code=:code";
        m.put("ret", false);
        m.put("code", code);
        m.put("pi", parentItem);
        Item item = getFacade().findFirstByJpql(jpql, m);
        if (item == null) {
            jpql = "select i "
                    + " from Item i "
                    + " and i.parentItem=:pi "
                    + " where i.code=:code";
            m = new HashMap();
            m.put("code", code);
            m.put("pi", parentItem);
            item = getFacade().findFirstByJpql(jpql, m);
            if (item != null) {
                item.setRetired(false);
                getFacade().edit(item);
            } else {
                item = new Item();
                item.setName(code);
                item.setCode(code);
                item.setParentItem(parentItem);
                getFacade().create(item);
            }
        }
        return item;
    }

    public Item findItemByName(String name, String parentCode) {
        Item parentItem = findItemByCode(parentCode);
        String jpql;
        Map m = new HashMap();
        jpql = "select i "
                + " from Item i "
                + " where i.retired=:ret "
                + " and i.parentItem=:pi "
                + " and i.name=:name";
        m.put("ret", false);
        m.put("name", name);
        m.put("pi", parentItem);
        Item item = getFacade().findFirstByJpql(jpql, m);
        if (item == null) {
            jpql = "select i "
                    + " from Item i "
                    + " where i.parentItem=:pi "
                    + " and i.name=:name";
            m = new HashMap();
            m.put("name", name);
            m.put("pi", parentItem);
            item = getFacade().findFirstByJpql(jpql, m);
            if (item != null) {
                item.setRetired(false);
                getFacade().edit(item);
            } else {
                item = new Item();
                item.setName(name);
                item.setCode(CommonController.nameToCode(name));
                item.setParentItem(parentItem);
                getFacade().create(item);
            }
        }
        return item;
    }

    public Item findItemByName(String name, Department dept) {
        try {
            String jpql;
            Map m = new HashMap();
            jpql = "select i "
                    + " from Item i "
                    + " where i.retired=:ret "
                    + " and i.department=:dept "
                    + " and i.name=:name";
            m.put("ret", false);
            m.put("name", name);
            m.put("dept", dept);
            Item item = getFacade().findFirstByJpql(jpql, m);
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    public Item findItemByName(String name, String code, Department dept) {
        try {
            String jpql;
            Map m = new HashMap();
            jpql = "select i "
                    + " from Item i "
                    + " where i.retired=:ret "
                    + " and i.department=:dept "
                    + " and i.code=:code "
                    + " and i.name=:name ";
            m.put("ret", false);
            m.put("name", name);
            m.put("code", code);
            m.put("dept", dept);
            Item item = getFacade().findFirstByJpql(jpql, m);
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    public Item findMasterItemByName(String name) {
        try {
            String jpql;
            Map m = new HashMap();
            jpql = "select i "
                    + " from Item i "
                    + " where i.retired=:ret "
                    + " and i.isMasterItem=:mi "
                    + " and i.name=:name";
            m.put("ret", false);
            m.put("name", name);
            m.put("mi", true);
            return getFacade().findFirstByJpql(jpql, m);
        } catch (Exception e) {
            return null;
        }
    }

    public void fillInvestigationSampleComponents() {
        if (current == null) {
            JsfUtil.addErrorMessage("Select an investigation");
            return;
        }
        if (current instanceof Investigation) {
            investigationSampleComponents = findInvestigationSampleComponents((Investigation) current);
            if (investigationSampleComponents != null && investigationSampleComponents.size() > 1) {
                current.setHasMoreThanOneComponant(true);
                getFacade().edit(current);
            }
        } else {
            investigationSampleComponents = null;
        }
    }

    public List<Item> getInvestigationSampleComponents(Item ix) {
        if (ix == null) {
            JsfUtil.addErrorMessage("Select an investigation");
            return null;
        }
        if (ix instanceof Investigation) {
            return findInvestigationSampleComponents((Investigation) ix);
        }
        return null;
    }

    public Item getFirstInvestigationSampleComponents(Item ix) {
        if (ix == null) {
            JsfUtil.addErrorMessage("Select an investigation");
            return null;
        }
        if (ix instanceof Investigation) {
            List<Item> is = findInvestigationSampleComponents((Investigation) ix);
            if (is != null && !is.isEmpty()) {
                return is.get(0);
            } else {
                Item sc = new Item();
                sc.setParentItem(ix);
                sc.setItemType(ItemType.SampleComponent);
                sc.setCreatedAt(new Date());
                sc.setCreater(sessionController.getLoggedUser());
                sc.setName(ix.getName());
                getFacade().create(sc);
                return sc;
            }
        }
        return null;
    }

    public List<Item> findInvestigationSampleComponents(Investigation ix) {
        if (ix == null) {
            JsfUtil.addErrorMessage("Select an investigation");
            return null;
        }
        String j = "select i from Item i where i.itemType=:t and i.parentItem=:m and i.retired=:r order by i.name";
        Map m = new HashMap();
        m.put("t", ItemType.SampleComponent);
        m.put("r", false);
        m.put("m", ix);
        return getFacade().findByJpql(j, m);
    }

    public void removeSampleComponent() {
        if (sampleComponent == null) {
            JsfUtil.addErrorMessage("Select one to delete");
            return;
        }
        sampleComponent.setRetired(true);
        sampleComponent.setRetirer(sessionController.getLoggedUser());
        sampleComponent.setRetiredAt(new Date());
        getFacade().edit(sampleComponent);
        fillInvestigationSampleComponents();
        JsfUtil.addSuccessMessage("Removed");
    }

    public void toCreateSampleComponent() {
        if (current == null) {
            JsfUtil.addErrorMessage("Select an investigation");
            return;
        }
        sampleComponent = new Item();
        sampleComponent.setParentItem(current);
        sampleComponent.setItemType(ItemType.SampleComponent);
        sampleComponent.setCreatedAt(new Date());
        sampleComponent.setCreater(sessionController.getLoggedUser());
    }

    public void createOrUpdateSampleComponent() {
        if (sampleComponent == null) {
            JsfUtil.addErrorMessage("Select one to delete");
            return;
        }
        sampleComponent.setParentItem(current);
        sampleComponent.setItemType(ItemType.SampleComponent);

        if (sampleComponent.getId() == null) {
            getFacade().create(sampleComponent);
            JsfUtil.addSuccessMessage("Added");
        } else {
            getFacade().edit(sampleComponent);
            JsfUtil.addSuccessMessage("Updated");
        }
        fillInvestigationSampleComponents();
    }

    public void addSampleComponentsForAllInvestigationsWithoutSampleComponents() {
        String j = "select ix from Investigation ix ";
        List<Item> ixs = getFacade().findByJpql(j);
        for (Item ix : ixs) {
            if (ix instanceof Investigation) {
                Investigation tix = (Investigation) ix;
                List<Item> scs = findInvestigationSampleComponents(tix);
                if (scs == null || scs.isEmpty()) {
                    sampleComponent = new Item();
                    sampleComponent.setName(tix.getName());
                    sampleComponent.setParentItem(tix);
                    sampleComponent.setItemType(ItemType.SampleComponent);
                    sampleComponent.setCreatedAt(new Date());
                    sampleComponent.setCreater(sessionController.getLoggedUser());
                    getFacade().create(sampleComponent);
                } else {
                    if (scs.size() > 1) {
                        tix.setHasMoreThanOneComponant(true);
                        getFacade().edit(tix);
                    } else {
                        tix.setHasMoreThanOneComponant(false);
                        getFacade().edit(tix);
                    }
                }
            }
        }
        JsfUtil.addSuccessMessage("Added");
    }

    public void fillMachineTests() {
        if (machine == null) {
            JsfUtil.addErrorMessage("Select a machine");
            return;
        }
        String j = "select i from Item i where i.itemType=:t and i.machine=:m and i.retired=:r order by i.code";
        Map m = new HashMap();
        m.put("t", ItemType.AnalyzerTest);
        m.put("m", machine);
        m.put("r", false);
        machineTests = getFacade().findByJpql(j, m);
    }

    public List<Item> completeMachineTests(String qry) {
        List<Item> ts;
        String j = "select i from Item i where i.itemType=:t and ((i.name) like :m or (i.name) like :m ) and i.retired=:r order by i.code";
        Map m = new HashMap();
        m.put("t", ItemType.AnalyzerTest);
        m.put("m", "%" + qry.toLowerCase() + "%");
        m.put("r", false);
        ts = getFacade().findByJpql(j, m);
        return ts;
    }

    public void removeTest() {
        if (current == null) {
            JsfUtil.addErrorMessage("Select one to delete");
            return;
        }
        current.setRetired(true);
        current.setRetirer(sessionController.getLoggedUser());
        current.setRetiredAt(new Date());
        getFacade().edit(current);
        fillMachineTests();
        JsfUtil.addSuccessMessage("Removed");
    }

    public void toCreateNewTest() {
        if (machine == null) {
            JsfUtil.addErrorMessage("Select a machine");
            return;
        }
        current = new Item();
        current.setItemType(ItemType.AnalyzerTest);
        current.setCreatedAt(new Date());
        current.setCreater(sessionController.getLoggedUser());
    }

    public void createOrUpdateTest() {
        if (current == null) {
            JsfUtil.addErrorMessage("Select one to delete");
            return;
        }
        current.setMachine(machine);
//        current.setInstitution(machine.getInstitution());
        current.setItemType(ItemType.AnalyzerTest);

        if (current.getId() == null) {
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Added");
        } else {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated");
        }
        fillMachineTests();
    }

    public void refreshInvestigationsAndServices() {
        investigationsAndServices = null;
        getInvestigationsAndServices();
        for (Item i : getInvestigationsAndServices()) {
            i.getItemFeesAuto();
        }
    }

    public void createItemFessForItemsWithoutFee() {
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing is selected");
            return;
        }
        for (Item i : selectedList) {
            if (i.getItemFeesAuto() == null || i.getItemFeesAuto().isEmpty()) {
                ItemFee itf = new ItemFee();
                itf.setName("Fee");
                itf.setItem(i);
                itf.setInstitution(i.getInstitution());
                itf.setDepartment(i.getDepartment());
                itf.setFeeType(FeeType.OwnInstitution);
                itf.setFee(0.0);
                itf.setFfee(0.0);
                itemFeeManager.setItemFee(itf);
                itemFeeManager.setItem(i);
                itemFeeManager.addNewFee();
            }
        }
    }

    public void updateSelectedItemFees() {
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing is selected");
            return;
        }
        for (Item i : selectedList) {
            if (!(i.getItemFeesAuto() == null) && !(i.getItemFeesAuto().isEmpty())) {
                double t = 0.0;
                double tf = 0.0;
                for (ItemFee itf : i.getItemFeesAuto()) {
                    getItemFeeFacade().edit(itf);
                    t += itf.getFee();
                    tf += itf.getFfee();
                }
                i.setTotal(t);
                i.setTotalForForeigner(tf);
                getFacade().edit(i);
            }
        }
        investigationsAndServices = null;
        getInvestigationsAndServices();
    }

    public void updateSelectedFeesForDiscountAllow() {
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing is selected");
            return;
        }
        for (Item i : selectedList) {
            if (!(i.getItemFeesAuto() == null) && !(i.getItemFeesAuto().isEmpty())) {
                for (ItemFee itf : i.getItemFeesAuto()) {
                    if (itf.getFeeType() == FeeType.OwnInstitution) {
                        itf.setDiscountAllowed(true);
                    } else {
                        itf.setDiscountAllowed(false);
                    }
                    getItemFeeFacade().edit(itf);
                }
                getFacade().edit(i);
            }
        }
        investigationsAndServices = null;
        getInvestigationsAndServices();
    }

    public List<Department> getDepartments() {
        departments = departmentController.getInstitutionDepatrments(institution);
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

    public void createNewItemsFromMasterItems() {
        ////// // System.out.println("createNewItemsFromMasterItems");
        if (institution == null) {
            JsfUtil.addErrorMessage("Select institution");
            return;
        }
        if (department == null) {
            JsfUtil.addErrorMessage("Select department");
            return;
        }
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Select Items");
            return;
        }
        for (Item i : selectedList) {
            ////// // System.out.println("i.getName() = " + i.getName());
            Item ni = null;
            if (i instanceof Investigation) {
                try {
                    ni = new Investigation();
                    BeanUtils.copyProperties(ni, i);

                } catch (IllegalAccessException | InvocationTargetException ex) {
                    Logger.getLogger(ItemController.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (i instanceof Service) {
                try {
                    ni = new Service();
                    BeanUtils.copyProperties(ni, i);
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    Logger.getLogger(ItemController.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                continue;
            }
            if (ni == null) {
                continue;
            }
            ni.setId(null);
            ni.setInstitution(institution);
            ni.setDepartment(department);
            ni.setItemFee(null);
            getFacade().create(ni);
            i.setItemFees(itemFeeManager.fillFees(i));
            ////// // System.out.println("ni = " + ni);
            ////// // System.out.println("i.getItemFees() = " + i.getItemFees());
            ////// // System.out.println("ni.getItemFees() = " + ni.getItemFees());

            for (ItemFee f : i.getItemFees()) {
                ItemFee nf = new ItemFee();
                ////// // System.out.println("f = " + f);
                try {
                    BeanUtils.copyProperties(nf, f);
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    Logger.getLogger(ItemController.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (nf.getInstitution() != null) {
                    nf.setInstitution(institution);
                }
                if (nf.getDepartment() != null) {
                    nf.setDepartment(department);
                }
                nf.setId(null);
                nf.setItem(ni);
                ni.getItemFees().add(nf);
                getItemFeeFacade().create(nf);
                ////// // System.out.println("nf = " + nf);
            }
            getFacade().edit(ni);
            List<Item> ifis = itemForItemController.getItemsForParentItem(i);
            if (ifis != null) {
                for (Item ifi : ifis) {
                    ItemForItem ifin = new ItemForItem();
                    ifin.setParentItem(ni);
                    ifin.setChildItem(ifi);
                    ifin.setCreatedAt(new Date());
                    ifin.setCreater(getSessionController().getLoggedUser());
                }
            }
            ////// // System.out.println("ni.getItemFees() = " + ni.getItemFees());
        }
    }

    public void updateItemsFromMasterItems() {
        if (institution == null) {
            JsfUtil.addErrorMessage("Select institution");
            return;
        }
        if (department == null) {
            JsfUtil.addErrorMessage("Select department");
            return;
        }
        if (selectedList == null || selectedList.isEmpty()) {
            JsfUtil.addErrorMessage("Select Items");
            return;
        }

        for (Item i : selectedList) {
            if (i.getDepartment() != null) {
                i.setDepartment(department);
            }

            if (i.getInstitution() != null) {
                i.setInstitution(institution);
            }
            getFacade().edit(i);
        }

        selectedList = null;

    }

    public void updateItemsAndFees() {
        if (institution == null) {
            JsfUtil.addErrorMessage("Select institution");
            return;
        }
        if (department == null) {
            JsfUtil.addErrorMessage("Select department");
            return;
        }
        if (selectedItemFeeList == null || selectedItemFeeList.isEmpty()) {
            JsfUtil.addErrorMessage("Select Items");
            return;
        }

        for (ItemFee fee : selectedItemFeeList) {
            if (fee.getDepartment() != null) {
                fee.setDepartment(department);
            }

            if (fee.getInstitution() != null) {
                fee.setInstitution(institution);
            }
            getItemFeeFacade().edit(fee);
        }

        selectedItemFeeList = null;
    }

    public List<Item> completeDealorItem(String query) {
        List<Item> suggestions;
        String sql;
        HashMap hm = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c.item from ItemsDistributors c"
                    + " where c.retired=false "
                    + " and c.item.retired=false "
                    + " and c.institution=:ins and ((c.item.name) like :q or "
                    + " (c.item.barcode) like :q or (c.item.code) like :q )order by c.item.name";
            hm.put("ins", getInstitution());
            hm.put("q", "%" + query + "%");
            //////// // System.out.println(sql);
            suggestions = getFacade().findByJpql(sql, hm, 20);
        }
        return suggestions;

    }

    public List<Item> getDealorItem() {
        List<Item> suggestions;
        String sql;
        HashMap hm = new HashMap();

        sql = "select c.item from ItemsDistributors c where c.retired=false "
                + " and c.institution=:ins "
                + " order by c.item.name";
        hm.put("ins", getInstitution());

        //////// // System.out.println(sql);
        suggestions = getFacade().findByJpql(sql, hm);

        return suggestions;

    }

    public List<Item> completeItem(String query, Class[] itemClasses, DepartmentType[] departmentTypes, int count) {
        String sql;
        List<Item> lst;
        HashMap tmpMap = new HashMap();
        if (query == null) {
            lst = new ArrayList<>();
        } else {
            sql = "select c "
                    + " from Item c "
                    + " where c.retired=false ";

            if (departmentTypes != null) {
                sql += " and c.departmentType in :deps ";
                tmpMap.put("deps", Arrays.asList(departmentTypes));
            }

            if (itemClasses != null) {
                sql += " and type(c) in :types ";
                tmpMap.put("types", Arrays.asList(itemClasses));
            }

            sql += " and ((c.name) like :q or (c.code) like :q or (c.barcode) like :q  ) ";
            tmpMap.put("q", "%" + query.toUpperCase() + "%");

            sql += " order by c.name";

            if (count != 0) {
                lst = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP, count);
            } else {
                lst = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP);
            }
        }
        return lst;
    }

    public List<Item> completeMasterItems(String query) {
        String jpql;
        List<Item> lst;
        if (query == null) {
            lst = new ArrayList<>();
        } else {
            HashMap tmpMap = new HashMap();
            jpql = "select damith "
                    + " from Item damith "
                    + " where damith.retired=:ret ";
            jpql += " and (damith.name like :q or damith.code like :q or damith.barcode like :q ) ";
            jpql += " and damith.isMasterItem=:mi ";
            tmpMap.put("q", "%" + query + "%");
            tmpMap.put("mi", true);
            tmpMap.put("ret", false);
            jpql += " order by damith.name";

            lst = getFacade().findByJpql(jpql, tmpMap);
        }

        return lst;
    }

    public List<Item> completeItem(String query) {
        return completeItem(query, null, null, 20);
//        List<Item> suggestions;
//        String sql;
//        HashMap hm = new HashMap();
//        if (query == null) {
//            suggestions = new ArrayList<>();
//        } else {
//            sql = "select c from Item c "
//                    + " where c.retired=false"
//                    + "  and ((c.name) like :q"
//                    + "  or (c.barcode) like :q"
//                    + "  or (c.code) like :q )"
//                    + " order by c.name";
//            hm.put("q", "%" + query.toUpperCase() + "%");
////////// // System.out.println(sql);
//            suggestions = getFacade().findByJpql(sql, hm, 20);
//        }
//        return suggestions;
//
    }

    List<Item> itemList;

    List<Item> suggestions;

    public List<Item> completeMedicine(String query) {
        DepartmentType[] dts = new DepartmentType[]{DepartmentType.Pharmacy, null};
        Class[] classes = new Class[]{Vmp.class, Amp.class, Vmp.class, Amp.class, Vmpp.class, Ampp.class};
        return completeItem(query, classes, dts, 0);
    }

    public List<Item> completeItem(String query, Class[] itemClasses, DepartmentType[] departmentTypes) {
        return completeItem(query, itemClasses, departmentTypes, 0);
    }

    public List<Item> completeAmpItem(String query) {
//        DepartmentType[] dts = new DepartmentType[]{DepartmentType.Pharmacy, null};
//        Class[] classes = new Class[]{Amp.class};
//        return completeItem(query, classes, dts, 30);
//        
        String sql;
        HashMap tmpMap = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {

            sql = "select c from Item c where c.retired=false "
                    + " and (type(c)= :amp) and "
                    + " ( c.departmentType is null or c.departmentType!=:dep ) "
                    + " and ((c.name) like :str or (c.code) like :str or"
                    + " (c.barcode) like :str ) order by c.name";
            //////// // System.out.println(sql);
            tmpMap.put("dep", DepartmentType.Store);
            tmpMap.put("amp", Amp.class);
            tmpMap.put("str", "%" + query.toUpperCase() + "%");
            suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP, 30);
        }
        return suggestions;

    }

    public List<Item> completeAmps(String query) {
        String sql;
        HashMap tmpMap = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c "
                    + " from Item c "
                    + " where c.retired=false "
                    + " and (type(c)= :amp) "
                    + " and "
                    + " (c.departmentType is null or c.departmentType!=:dep ) "
                    + " and "
                    + " ("
                    + " (c.name) like :str "
                    + " or "
                    + " (c.code) like :str "
                    + " or "
                    + " (c.barcode) like :str ) "
                    + "order by c.name";
            tmpMap.put("dep", DepartmentType.Pharmacy);
            tmpMap.put("amp", Amp.class);
            tmpMap.put("str", "%" + query.toUpperCase() + "%");
            suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP, 30);
        }
        return suggestions;

    }

    public List<Item> completeAmpItemAll(String query) {
        String sql;
        HashMap tmpMap = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {

            sql = "select c from Item c where "
                    + " c.retired=:ret "
                    + " and (type(c)= :amp) "
                    + " and "
                    + " ( c.departmentType is null or c.departmentType!=:dep ) "
                    + " and "
                    + " ((c.name) like :str or (c.code) like :str or (c.barcode) like :str ) "
                    + " order by c.name";
            tmpMap.put("dep", DepartmentType.Store);
            tmpMap.put("amp", Amp.class);
            tmpMap.put("ret", false);
            tmpMap.put("str", "%" + query.toUpperCase() + "%");
            suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP, 30);
        }
        return suggestions;

    }

    public List<Item> completeStoreItem(String query) {
        DepartmentType[] dts = new DepartmentType[]{DepartmentType.Store, DepartmentType.Inventry};
        Class[] classes = new Class[]{Amp.class};
        return completeItem(query, classes, dts, 0);
//        String sql;
//        HashMap tmpMap = new HashMap();
//        if (query == null) {
//            suggestions = new ArrayList<>();
//        } else {
//
//            sql = "select c from Item c "
//                    + "where c.retired=false and "
//                    + "(type(c)= :amp) "
//                    + "and (c.departmentType=:dep or c.departmentType=:inven )"
//                    + "and ((c.name) like :str or "
//                    + "(c.code) like :str or "
//                    + "(c.barcode) like :str) "
//                    + "order by c.name";
//            //////// // System.out.println(sql);
//            tmpMap.put("amp", Amp.class);
//            tmpMap.put("dep", DepartmentType.Store);
//            tmpMap.put("inven", DepartmentType.Inventry);
//            tmpMap.put("str", "%" + query.toUpperCase() + "%");
//            suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP, 30);
//        }
//        return suggestions;
//
    }

    public List<Item> completeStoreInventryItem(String query) {
        DepartmentType[] dts = new DepartmentType[]{DepartmentType.Inventry};
        Class[] classes = new Class[]{Amp.class};
        return completeItem(query, classes, dts, 0);
//        String sql;
//        HashMap tmpMap = new HashMap();
//        if (query == null) {
//            suggestions = new ArrayList<>();
//        } else {
//
//            sql = "select c from Item c "
//                    + "where c.retired=false and "
//                    + "(type(c)= :amp) "
//                    + "and c.departmentType=:dep "
//                    + "and ((c.name) like :str or "
//                    + "(c.code) like :str or "
//                    + "(c.barcode) like :str) "
//                    + "order by c.name";
//            //////// // System.out.println(sql);
//            tmpMap.put("amp", Amp.class);
//            tmpMap.put("dep", DepartmentType.Inventry);
//            tmpMap.put("str", "%" + query.toUpperCase() + "%");
//            suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP, 30);
//        }
//        return suggestions;

    }

    public List<Item> completeStoreItemOnly(String query) {
        DepartmentType[] dts = new DepartmentType[]{DepartmentType.Store};
        Class[] classes = new Class[]{Amp.class};
        return completeItem(query, classes, dts, 0);
//        String sql;
//        HashMap tmpMap = new HashMap();
//        if (query == null) {
//            suggestions = new ArrayList<>();
//        } else {
//
//            sql = "select c from Item c "
//                    + "where c.retired=false and "
//                    + "(type(c)= :amp) "
//                    + "and c.departmentType=:dep "
//                    + "and ((c.name) like :str or "
//                    + "(c.code) like :str or "
//                    + "(c.barcode) like :str) "
//                    + "order by c.name";
//            //////// // System.out.println(sql);
//            tmpMap.put("amp", Amp.class);
//            tmpMap.put("dep", DepartmentType.Store);
//            tmpMap.put("str", "%" + query.toUpperCase() + "%");
//            suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP, 30);
//        }
//        return suggestions;
//
    }

    public List<Item> completeExpenseItem(String query) {
        Class[] classes = new Class[]{BillExpense.class};
        return completeItem(query, classes, null, 0);
//        String sql;
//        HashMap tmpMap = new HashMap();
//        if (query == null) {
//            suggestions = new ArrayList<>();
//        } else {
//            sql = "select c from Item c "
//                    + "where c.retired=false and "
//                    + "(type(c)= :amp) "
//                    + "and ((c.name) like :str or "
//                    + "(c.code) like :str or "
//                    + "(c.barcode) like :str) "
//                    + "order by c.name";
//            //////// // System.out.println(sql);
//            tmpMap.put("amp", BillExpense.class);
//            tmpMap.put("str", "%" + query.toUpperCase() + "%");
//            suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP, 30);
//        }
//        return suggestions;
    }

    public List<Item> fetchStoreItem() {
        List<Item> suggestions;
        String sql;
        HashMap tmpMap = new HashMap();

        sql = "select c from Item c"
                + "  where c.retired=false and "
                + " (type(c)= :amp) "
                + " and c.departmentType=:dep "
                + " order by c.name";
        //////// // System.out.println(sql);
        tmpMap.put("amp", Amp.class);
        tmpMap.put("dep", DepartmentType.Store);

        suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP);

        return suggestions;

    }

    public List<Item> completeAmpAndAmppItem(String query) {
        List<Item> suggestions;
        String sql;
        HashMap tmpMap = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            if (query.length() > 4) {
                sql = "select c from Item c where c.retired=false and (type(c)= :amp or type(c)=:ampp ) and ((c.name) like '%" + query.toUpperCase() + "%' or (c.code) like '%" + query.toUpperCase() + "%' or (c.barcode) like '%" + query.toUpperCase() + "%') order by c.name";
            } else {
                sql = "select c from Item c where c.retired=false and (type(c)= :amp or type(c)=:ampp ) and ((c.name) like '%" + query.toUpperCase() + "%' or (c.code) like '%" + query.toUpperCase() + "%') order by c.name";
            }

//////// // System.out.println(sql);
            tmpMap.put("amp", Amp.class);
            tmpMap.put("ampp", Ampp.class);
            suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP, 30);
        }
        return suggestions;

    }

    public List<Item> completeAmpAndVmpItem(String query) {
        List<Item> suggestions;
        String sql;
        HashMap tmpMap = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            if (query.length() > 4) {
                sql = "select c from Item c where c.retired=false and (type(c)= :amp or type(c)=:vmp ) and ((c.name) like '%" + query.toUpperCase() + "%' or (c.code) like '%" + query.toUpperCase() + "%' or (c.barcode) like '%" + query.toUpperCase() + "%') order by c.name";
            } else {
                sql = "select c from Item c where c.retired=false and (type(c)= :amp or type(c)=:vmp ) and ((c.name) like '%" + query.toUpperCase() + "%' or (c.code) like '%" + query.toUpperCase() + "%') order by c.name";
            }

//////// // System.out.println(sql);
            tmpMap.put("amp", Amp.class);
            tmpMap.put("vmp", Vmp.class);
            suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP, 30);
        }
        return suggestions;

    }

    public List<Item> completePackage(String query) {
        List<Item> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Item c where c.retired=false"
                    + " and (c.inactive=false or c.inactive is null) "
                    + "and type(c)=Packege "
                    + "and (c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            //////// // System.out.println(sql);
            suggestions = getFacade().findByJpql(sql);
        }
        return suggestions;

    }

    public List<Item> completeService(String query) {
        List<Item> suggestions;
        String sql;
        HashMap hm = new HashMap();

        sql = "select c from Item c where c.retired=false and type(c)=:cls"
                + " and (c.name) like :q order by c.name";

        hm.put("cls", Service.class);
        hm.put("q", "%" + query.toUpperCase() + "%");
        suggestions = getFacade().findByJpql(sql, hm, 20);

        return suggestions;

    }

    public List<Item> completeInvestigation(String query) {
        List<Item> suggestions;
        String sql;
        HashMap hm = new HashMap();

        sql = "select c from Item c where c.retired=false and type(c)=:cls"
                + " and (c.name) like :q order by c.name";

        hm.put("cls", Investigation.class);
        hm.put("q", "%" + query.toUpperCase() + "%");
        suggestions = getFacade().findByJpql(sql, hm, 20);

        return suggestions;

    }

    public List<Item> completeLoggedInstitutionInvestigation(String query) {
        List<Item> lst;
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Item c "
                + " where c.retired=false "
                + " and type(c)=:cls "
                + " and (c.name) like :q "
                + " and c.institution=:ins "
                + " order by c.name";
        hm.put("cls", Investigation.class);
        hm.put("q", "%" + query.toUpperCase() + "%");
        hm.put("ins", sessionController.getLoggedUser().getInstitution());
        lst = getFacade().findByJpql(sql, hm, 20);
        return lst;
    }

    public List<Item> completeServiceWithoutProfessional(String query) {
        List<Item> lst;
        String sql;
        HashMap hm = new HashMap();

        sql = "select c from Item c where c.retired=false and type(c)=:cls"
                + " and (c.name) like :q "
                + " c.id in (Select f.item.id From Itemfee f where f.retired=false "
                + " and f.feeType!=:ftp ) order by c.name ";

        hm.put("ftp", FeeType.Staff);
        hm.put("cls", Service.class);
        hm.put("q", "%" + query.toUpperCase() + "%");
        lst = getFacade().findByJpql(sql, hm, 20);
        return lst;
    }

    public List<Item> completeMedicalPackage(String query) {
        List<Item> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Item c where c.retired=false "
                    + " and (c.inactive=false or c.inactive is null) "
                    + "and type(c)=MedicalPackage "
                    + "and (c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            //////// // System.out.println(sql);
            suggestions = getFacade().findByJpql(sql);
        }
        return suggestions;

    }

    public List<Item> completeInwardItems(String query) {
        List<Item> suggestions;
        HashMap m = new HashMap();
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Item c "
                    + " where c.retired=false "
                    + " and type(c)!=:pac "
                    + " and (type(c)=:ser "
                    + " or type(c)=:inv"
                    + " or type(c)=:ward "
                    + " or type(c)=:the)  "
                    + " and (c.name) like :q"
                    + " order by c.name";
            m.put("pac", Packege.class);
            m.put("ser", Service.class);
            m.put("inv", Investigation.class);
            m.put("ward", InwardService.class);
            m.put("the", TheatreService.class);
            m.put("q", "%" + query.toUpperCase() + "%");
            //    //////// // System.out.println(sql);
            suggestions = getFacade().findByJpql(sql, m, 20);
        }
        return suggestions;
    }

    public List<Item> completeTheatreItems(String query) {
        List<Item> suggestions;
        HashMap m = new HashMap();
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Item c "
                    + " where c.retired=false "
                    + " and type(c)=:the "
                    //                    + " and type(c)!=:pac "
                    //                    + " and (type(c)=:ser "
                    //                    + " or type(c)=:inv "
                    //                    + " or type(c)=:the)  "
                    + " and (c.name) like :q"
                    + " order by c.name";
//            m.put("pac", Packege.class);
//            m.put("ser", Service.class);
//            m.put("inv", Investigation.class);
            m.put("the", TheatreService.class);
            m.put("q", "%" + query.toUpperCase() + "%");
            //    //////// // System.out.println(sql);
            suggestions = getFacade().findByJpql(sql, m, 20);
        }
        return suggestions;
    }

    Category category;

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    private List<Item> fetchInwardItems(String query, Department department) {
        HashMap m = new HashMap();
        String sql;
        sql = "select c from Item c "
                + " where c.retired=false "
                + " and type(c)!=:pac "
                + " and (type(c)=:ser "
                + " or type(c)=:inward "
                + " or type(c)=:inv) "
                + " and (c.inactive=false or c.inactive is null) "
                + " and (c.name) like :q";
        if (department != null) {
            sql += " and c.department=:dep ";
            m.put("dep", getReportKeyWord().getDepartment());
        }
        sql += " order by c.name";
        m.put("pac", Packege.class);
        m.put("ser", Service.class);
        m.put("inward", InwardService.class);
        m.put("inv", Investigation.class);
        m.put("q", "%" + query.toUpperCase() + "%");

        return getFacade().findByJpql(sql, m, 20);

    }

    private List<Item> fetchInwardItems(String query, Category cat, Department department) {
        HashMap m = new HashMap();
        String sql;
        sql = "select c from Item c "
                + " where c.retired=false "
                + " and c.category=:ct"
                + " and type(c)!=:pac "
                + " and (type(c)=:ser "
                + " or type(c)=:inward "
                + " or type(c)=:inv) "
                + " and (c.inactive=false or c.inactive is null) "
                + " and (c.name) like :q";
        if (department != null) {
            sql += " and c.department=:dep ";
            m.put("dep", getReportKeyWord().getDepartment());
        }
        sql += " order by c.name";
        m.put("ct", cat);
        m.put("pac", Packege.class);
        m.put("ser", Service.class);
        m.put("inv", Investigation.class);
        m.put("inward", InwardService.class);
        m.put("q", "%" + query.toUpperCase() + "%");

        return getFacade().findByJpql(sql, m, 20);

    }

    @Inject
    ServiceSubCategoryController serviceSubCategoryController;

    public ServiceSubCategoryController getServiceSubCategoryController() {
        return serviceSubCategoryController;
    }

    public void setServiceSubCategoryController(ServiceSubCategoryController serviceSubCategoryController) {
        this.serviceSubCategoryController = serviceSubCategoryController;
    }

    public List<Item> completeInwardItemsCategory(String query) {
        List<Item> suggestions = new ArrayList<>();

        if (category == null) {
            suggestions = fetchInwardItems(query, null);
        } else if (category instanceof ServiceCategory) {
            suggestions = fetchInwardItems(query, category, null);
            getServiceSubCategoryController().setParentCategory(category);
            for (ServiceSubCategory ssc : getServiceSubCategoryController().getItems()) {
                suggestions.addAll(fetchInwardItems(query, ssc, null));
            }
        } else {
            suggestions = fetchInwardItems(query, category, null);
        }

        return suggestions;
    }

    public List<Item> completeInwardItemsCategoryNew(String query) {
        List<Item> suggestions = new ArrayList<>();

        if (category == null) {
            suggestions = fetchInwardItems(query, getReportKeyWord().getDepartment());
        } else if (category instanceof ServiceCategory) {
            suggestions = fetchInwardItems(query, category, getReportKeyWord().getDepartment());
            getServiceSubCategoryController().setParentCategory(category);
            for (ServiceSubCategory ssc : getServiceSubCategoryController().getItems()) {
                suggestions.addAll(fetchInwardItems(query, ssc, getReportKeyWord().getDepartment()));
            }
        } else {
            suggestions = fetchInwardItems(query, category, getReportKeyWord().getDepartment());
        }

        return suggestions;
    }

    public List<Item> completeOpdItemsByNamesAndCode(String query) {
        if (sessionController.getLoggedPreference().isInstitutionRestrictedBilling()) {
            return completeOpdItemsByNamesAndCodeInstitutionSpecificOrNotSpecific(query, true);
        } else {
            return completeOpdItemsByNamesAndCodeInstitutionSpecificOrNotSpecific(query, false);
        }
    }

    public void makeItemsAsActiveOrInactiveByRetiredStatus() {
        String j = "select i from Item i";
        List<Item> tis = getFacade().findByJpql(j);
        for (Item i : tis) {
            if (i.isRetired()) {
                i.setInactive(true);
            } else {
                i.setInactive(false);
            }
            getFacade().edit(i);
        }
    }

    public void toggleItemIctiveInactiveState() {
        String j = "select i from Item i";
        List<Item> tis = getFacade().findByJpql(j);
        for (Item i : tis) {
            if (i.isInactive()) {
                i.setInactive(false);
            } else {
                i.setInactive(true);
            }
            getFacade().edit(i);
        }
    }

    public List<Item> completeOpdItemsByNamesAndCodeInstitutionSpecificOrNotSpecific(String query, boolean spcific) {
        if (query == null || query.trim().equals("")) {
            return new ArrayList<>();
        }
        List<Item> mySuggestions;
        HashMap m = new HashMap();
        String sql;

        sql = "select c from Item c "
                + " where c.retired<>true "
                + " and (c.inactive<>true) "
                + " and type(c)!=:pac "
                + " and type(c)!=:inw "
                + " and (type(c)=:ser "
                + " or type(c)=:inv)  "
                + " and ((c.name) like :q or (c.fullName) like :q or "
                + " (c.code) like :q or (c.printName) like :q ) ";
        if (spcific) {
            sql += " and c.institution=:ins";
            m.put("ins", getSessionController().getInstitution());
        }
        if (getReportKeyWord().getDepartment() != null) {
            sql += " and c.department=:dep";
            m.put("dep", getReportKeyWord().getDepartment());
        }
        sql += " order by c.name";
        m.put("pac", Packege.class);
        m.put("inw", InwardService.class);
        m.put("ser", Service.class);
        m.put("inv", Investigation.class);
        m.put("q", "%" + query.toUpperCase() + "%");

//        //// // System.out.println(sql);
//        //// // System.out.println("m = " + m);
        mySuggestions = getFacade().findByJpql(sql, m, 20);
//        //// // System.out.println("mySuggestions = " + mySuggestions);
        return mySuggestions;
    }

    public List<Item> completeItemsByDepartment(String query, Department department) {
        List<Item> suggestions;
        HashMap<String, Object> parameters = new HashMap<>();
        String jpql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            jpql = "SELECT c FROM Item c "
                    + "WHERE c.retired = false "
                    + "AND (type(c)=:ser OR type(c)=:inv) "
                    + "AND (c.name LIKE :qry OR c.fullName LIKE :qry OR c.code LIKE :qry) "
                    + "AND c.department=:department "
                    + "ORDER BY c.name";
            parameters.put("ser", Service.class);
            parameters.put("inv", Investigation.class);
            parameters.put("q", "%" + query.toLowerCase() + "%");
            parameters.put("department", department);
            suggestions = getFacade().findByJpql(jpql, parameters, 20);
        }
        return suggestions;
    }

    public List<Item> completeItemsByDepartment(String query, Institution institution) {
        List<Item> suggestions;
        HashMap<String, Object> parameters = new HashMap<>();
        String jpql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            jpql = "SELECT c FROM Item c "
                    + "WHERE c.retired = false "
                    + "AND (type(c)=:ser OR type(c)=:inv) "
                    + "AND (c.name LIKE :qry OR c.fullName LIKE :qry OR c.code LIKE :qry) "
                    + "AND c.department.institution=:ins "
                    + "ORDER BY c.name";
            parameters.put("ser", Service.class);
            parameters.put("inv", Investigation.class);
            parameters.put("q", "%" + query.toLowerCase() + "%");
            parameters.put("ins", institution);
            suggestions = getFacade().findByJpql(jpql, parameters, 20);
        }
        return suggestions;
    }

    public List<Item> completeServicesPlusInvestigationsAll(String query) {
        List<Item> mySuggestions;
        HashMap m = new HashMap();
        String sql;
        if (query == null) {
            mySuggestions = new ArrayList<>();
        } else {
            sql = "select c from Item c "
                    + " where c.retired=false "
                    + " and (type(c)=:ser or type(c)=:inv)  "
                    + " and (c.name) like :q"
                    + " order by c.name";
            m.put("ser", Service.class);
            m.put("inv", Investigation.class);
            m.put("q", "%" + query.toUpperCase() + "%");
            //    //////// // System.out.println(sql);
            mySuggestions = getFacade().findByJpql(sql, m, 20);
        }
        return mySuggestions;
    }

    public List<Item> completeItemsByInstitution(String query, Institution institution) {
        List<Item> suggestions;
        HashMap<String, Object> parameters = new HashMap<>();
        String jpql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            jpql = "SELECT c FROM Item c "
                    + "WHERE c.retired = false "
                    + "AND (type(c)=:ser OR type(c)=:inv) "
                    + "AND (LOWER(c.name) LIKE :q) "
                    + "AND c.institution = :institution "
                    + "ORDER BY c.name";
            parameters.put("ser", Service.class);
            parameters.put("inv", Investigation.class);
            parameters.put("q", "%" + query.toLowerCase() + "%");
            parameters.put("institution", institution);
            suggestions = getFacade().findByJpql(jpql, parameters, 20);
        }
        return suggestions;
    }

    @Deprecated
    public List<Item> completeOpdItemsForItemListringStrategyLoggedInstitution(String query) {
        List<Item> mySuggestions;
        HashMap m = new HashMap();
        String sql;
        if (query == null) {
            mySuggestions = new ArrayList<>();
        } else {
            sql = "select c "
                    + " from Item c "
                    + " where c.retired=false "
                    + " and type(c)!=:pac "
                    + " and type(c)!=:inw "
                    + " and (type(c)=:ser "
                    + " or type(c)=:inv)  "
                    + " and (c.name) like :q "
                    + " and c.institution=:ins "
                    + " order by c.name";
            m.put("pac", Packege.class);
            m.put("inw", InwardService.class);
            m.put("ser", Service.class);
            m.put("inv", Investigation.class);
            m.put("ins", sessionController.getInstitution());

            m.put("q", "%" + query.toUpperCase() + "%");
            //    //////// // System.out.println(sql);
            mySuggestions = getFacade().findByJpql(sql, m, 20);
        }
        return mySuggestions;
    }

    public List<Item> completeItemWithoutPackOwn(String query) {
        List<Item> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<Item>();
        } else {
            sql = "select c from Item c where c.institution.id = " + getSessionController().getInstitution().getId() + " and c.retired=false and type(c)!=Packege and type(c)!=TimedItem and (c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            //////// // System.out.println(sql);
            suggestions = getFacade().findByJpql(sql);
        }
        return suggestions;
    }

    public void makeSelectedAsMasterItems() {
        for (Item i : selectedList) {
            ////// // System.out.println("i = " + i.getInstitution());
            if (i.getInstitution() != null) {
                ////// // System.out.println("i = " + i.getInstitution().getName());
                i.setInstitution(null);
                getFacade().edit(i);
            }
        }
    }

    public List<Item> fetchOPDItemList(boolean ins) {
        List<Item> items = new ArrayList<>();
        HashMap m = new HashMap();
        String sql;

        sql = "select c from Item c "
                + " where c.retired=false "
                + " and type(c)!=:pac "
                + " and type(c)!=:inw "
                + " and (type(c)=:ser "
                + " or type(c)=:inv)  ";

        if (ins) {
            sql += " and c.institution is null ";
        }

        sql += " order by c.name";

        m.put("pac", Packege.class);
        m.put("inw", InwardService.class);
        m.put("ser", Service.class);
        m.put("inv", Investigation.class);
        ////// // System.out.println(sql);
        items = getFacade().findByJpql(sql, m);
        return items;
    }

    public List<ItemFee> fetchOPDItemFeeList(boolean ins, FeeType ftype) {
        List<ItemFee> itemFees = new ArrayList<>();
        HashMap m = new HashMap();
        String sql;

        sql = "select c from ItemFee c "
                + " where c.retired=false "
                + " and type(c.item)!=:pac "
                + " and type(c.item)!=:inw "
                + " and (type(c.item)=:ser "
                + " or type(c.item)=:inv)  ";

        if (ftype != null) {
            sql += " and c.feeType=:fee ";
            m.put("fee", ftype);
        }

        if (ins) {
            sql += " and c.institution is null ";
        }

        sql += " order by c.name";

        m.put("pac", Packege.class);
        m.put("inw", InwardService.class);
        m.put("ser", Service.class);
        m.put("inv", Investigation.class);
        ////// // System.out.println(sql);
        itemFees = getItemFeeFacade().findByJpql(sql, m);
        return itemFees;
    }

    public void createAllItemsFeeList() {
        allItemFees = new ArrayList<>();
        allItemFees = fetchOPDItemFeeList(false, feeType);
    }

    public void updateSelectedOPDItemList() {

    }

    public void createOpdSeviceInvestgationList() {
        itemlist = getItems();
        for (Item i : itemlist) {
            List<ItemFee> tmp = serviceController.getFees(i);
            for (ItemFee itf : tmp) {
                i.setItemFee(itf);
                if (itf.getFeeType() == FeeType.OwnInstitution) {
                    i.setHospitalFee(i.getHospitalFee() + itf.getFee());
                    i.setHospitalFfee(i.getHospitalFfee() + itf.getFfee());
                } else if (itf.getFeeType() == FeeType.Staff) {
                    i.setProfessionalFee(i.getProfessionalFee() + itf.getFee());
                    i.setProfessionalFfee(i.getProfessionalFfee() + itf.getFfee());
                }
            }
        }
    }

    public void createInwardList() {
        itemlist = getInwardItems();
        for (Item i : itemlist) {
            List<ItemFee> tmp = serviceController.getFees(i);
            for (ItemFee itf : tmp) {
                i.setItemFee(itf);
                if (itf.getFeeType() == FeeType.OwnInstitution) {
                    i.setHospitalFee(i.getHospitalFee() + itf.getFee());
                    i.setHospitalFfee(i.getHospitalFfee() + itf.getFfee());
                } else if (itf.getFeeType() == FeeType.Staff) {
                    i.setProfessionalFee(i.getProfessionalFee() + itf.getFee());
                    i.setProfessionalFfee(i.getProfessionalFfee() + itf.getFfee());
                }
            }
        }
    }

    /**
     *
     */
    public ItemController() {
    }

    /**
     * Prepare to add new Collecting Centre
     *
     * @return
     */
    public void prepareAdd() {
        current = new Item();
    }

    public void prepareAddingInvestigation() {
        current = new Investigation();
    }

    public void prepareAddingService() {
        current = new Service();
    }

    /**
     *
     * @return
     */
    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    /**
     *
     * @param itemFacade
     */
    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    /**
     *
     * @return
     */
    public SessionController getSessionController() {
        return sessionController;
    }

    /**
     *
     * @param sessionController
     */
    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    /**
     * Return the current item
     *
     * @return
     */
    public Item getCurrent() {
        if (current == null) {
            current = new Item();
        }
        return current;
    }

    /**
     * Set the current item
     *
     * @param current
     */
    public void setCurrent(Item current) {
        this.current = current;
    }

    private ItemFacade getFacade() {
        return itemFacade;
    }

    /**
     *
     * @return
     */
    public List<Item> getItems() {
        if (items == null) {
            fillItemsWithInvestigationsAndServices();
        }
        return items;
    }

    public void fillItemsWithInvestigationsAndServices() {
        String temSql;
        HashMap h = new HashMap();
        temSql = "SELECT i FROM Item i where (type(i)=:t1 or type(i)=:t2 ) and i.retired=false order by i.department.name";
        h.put("t1", Investigation.class);
        h.put("t2", Service.class);
        items = getFacade().findByJpql(temSql, h, TemporalType.TIME);
    }

    public List<Item> getInwardItems() {
        String temSql;
        HashMap h = new HashMap();
        temSql = "SELECT i FROM Item i where type(i)=:t1 and i.retired=false order by i.department.name";
        h.put("t1", InwardService.class);
        items = getFacade().findByJpql(temSql, h, TemporalType.TIME);
        return items;
    }

    public List<Item> getItems(Category category) {
        String temSql;
        HashMap h = new HashMap();
        temSql = "SELECT i FROM Item i where i.category=:cat and i.retired=false order by i.name";
        h.put("cat", category);
        return getFacade().findByJpql(temSql, h);
    }

    /**
     *
     * Set all Items to null
     *
     */
    private void recreateModel() {
        items = null;
    }

    /**
     *
     */
    public void saveSelected() {
        saveSelected(getCurrent());
        JsfUtil.addSuccessMessage("Saved");
        recreateModel();
        allItems = null;
        getAllItems();
        getItems();
        current = null;
        getCurrent();
    }

    public void saveSelected(Item item) {
        if (item.getId() != null && item.getId() > 0) {
            getFacade().edit(item);
        } else {
            item.setCreatedAt(new Date());
            item.setCreater(getSessionController().getLoggedUser());
            getFacade().create(item);
        }
    }

    /**
     *
     * Delete the current Item
     *
     */
    public void delete() {

        if (getCurrent() != null) {
            getCurrent().setRetired(true);
            getCurrent().setRetiredAt(new Date());
            getCurrent().setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        allItems = null;
        getAllItems();
        getItems();
        current = null;
        getCurrent();
    }

    public Institution getInstitution() {
        if (institution == null) {
            institution = getSessionController().getInstitution();
        }
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public FeeType getFeeType() {
        return feeType;
    }

    public void setFeeType(FeeType feeType) {
        this.feeType = feeType;
    }

    public List<Item> getSelectedList() {
        return selectedList;
    }

    public void setSelectedList(List<Item> selectedList) {
        this.selectedList = selectedList;
    }

    public List<ItemLight> getAllItems() {
        if (allItems == null) {
            allItems = itemApplicationController.getItems();
        }
        return allItems;
    }

    public void setAllItems(List<ItemLight> allItems) {
        this.allItems = allItems;
    }

    public List<ItemFee> getAllItemFees() {
        return allItemFees;
    }

    public void setAllItemFees(List<ItemFee> allItemFees) {
        this.allItemFees = allItemFees;
    }

    public List<ItemFee> getSelectedItemFeeList() {
        return selectedItemFeeList;
    }

    public void setSelectedItemFeeList(List<ItemFee> selectedItemFeeList) {
        this.selectedItemFeeList = selectedItemFeeList;
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

    public List<Item> getItemList() {
        return itemList;
    }

    public void setItemList(List<Item> itemList) {
        this.itemList = itemList;
    }

    public List<Item> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<Item> suggestions) {
        this.suggestions = suggestions;
    }

    public ItemFeeFacade getItemFeeFacade() {
        return itemFeeFacade;
    }

    public void setItemFeeFacade(ItemFeeFacade itemFeeFacade) {
        this.itemFeeFacade = itemFeeFacade;
    }

    public ItemFeeManager getItemFeeManager() {
        return itemFeeManager;
    }

    public void setItemFeeManager(ItemFeeManager itemFeeManager) {
        this.itemFeeManager = itemFeeManager;
    }

    public List<Item> getItemlist() {
        return itemlist;
    }

    public void setItemlist(List<Item> itemlist) {
        this.itemlist = itemlist;
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

    public List<Item> getInvestigationsAndServices() {
        if (investigationsAndServices == null) {
            String temSql;
            HashMap h = new HashMap();
            temSql = "SELECT i FROM Item i where (type(i)=:t1 or type(i)=:t2 ) and i.retired=false order by i.department.name";
            h.put("t1", Investigation.class);
            h.put("t2", Service.class);
            investigationsAndServices = getFacade().findByJpql(temSql, h, TemporalType.TIME);
        }
        return investigationsAndServices;
    }

    public void setInvestigationsAndServices(List<Item> investigationsAndServices) {
        this.investigationsAndServices = investigationsAndServices;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public List<Item> getMachineTests() {
        return machineTests;
    }

    public void setMachineTests(List<Item> machineTests) {
        this.machineTests = machineTests;
    }

    public List<Item> getInvestigationSampleComponents() {
        return investigationSampleComponents;
    }

    public void setInvestigationSampleComponents(List<Item> investigationSampleComponents) {
        this.investigationSampleComponents = investigationSampleComponents;
    }

    public Item getSampleComponent() {
        return sampleComponent;
    }

    public void setSampleComponent(Item sampleComponent) {
        this.sampleComponent = sampleComponent;
    }

    public boolean isMasterItem() {
        return masterItem;
    }

    public void setMasterItem(boolean masterItem) {
        this.masterItem = masterItem;
    }

    public List<ItemFee> getItemFeesList() {
        return ItemFeesList;
    }

    public void setItemFeesList(List<ItemFee> ItemFeesList) {
        this.ItemFeesList = ItemFeesList;
    }

    public List<ItemLight> fillItemsByDepartment(Department dept) {
        List<ItemLight> deptItems = new ArrayList<>();
        for (ItemLight i : itemApplicationController.getItems()) {
            if (i.getDepartmentId() != null && i.getDepartmentId().equals(dept.getId())) {
                deptItems.add(i);
            }
        }
        return deptItems;
    }

    public List<ItemLight> fillItemsByInstitution(Institution institution) {
        List<ItemLight> insItems = new ArrayList<>();
        if (institution == null) {
            return insItems;
        }
        if (institution.getId() == null) {
            return insItems;
        }
        for (ItemLight i : itemApplicationController.getItems()) {
            if (i.getInstitutionId() == null) {
                continue;
            }
            if (Objects.equals(i.getInstitutionId(), institution.getId())) {
                insItems.add(i);
            }
        }

        return insItems;
    }

    public List<ItemLight> getDepartmentItems() {
        if (departmentItems == null) {
            departmentItems = fillItemsByDepartment(getSessionController().getDepartment());
        }
        return departmentItems;
    }

    public List<ItemLight> getInstitutionItems() {
        if (institutionItems == null) {
            institutionItems = fillItemsByInstitution(getSessionController().getInstitution());
        }
        return institutionItems;
    }

    public List<ItemLight> getCcDeptItems() {
        return ccDeptItems;
    }

    public void setCcDeptItems(List<ItemLight> ccDeptItems) {
        this.ccDeptItems = ccDeptItems;
    }

    public List<ItemLight> getCcInstitutionItems() {
        return ccInstitutionItems;
    }

    public void setCcInstitutionItems(List<ItemLight> ccInstitutionItems) {
        this.ccInstitutionItems = ccInstitutionItems;
    }

    public List<ItemFeeRow> getItemFeeRows() {
        return itemFeeRows;
    }

    public void setItemFeeRows(List<ItemFeeRow> itemFeeRows) {
        this.itemFeeRows = itemFeeRows;
    }

    public ItemLight findItemLightById(Long id) {
        System.out.println("findItemLightById");
        System.out.println("id = " + id);
        Optional<ItemLight> itemLightOptional = findItemLightByIdStreaming(id);
        ItemLight il = itemLightOptional.orElse(null);
        System.out.println("il = " + il);
        return il;
    }

    public Optional<ItemLight> findItemLightByIdStreaming(Long id) {
        if (id == null) {
            return Optional.empty(); // Clearly indicate absence of value
        }
        return itemApplicationController.getItems().stream()
                .filter(itemLight -> id.equals(itemLight.getId()))
                .findFirst(); // Returns an Optional describing the first matching element, or an empty Optional if no match is found
    }

    public ItemLight getSelectedItemLight() {
        if (getCurrent() == null) {
            selectedItemLight = null;
        } else {
            selectedItemLight = new ItemLight(getCurrent());
        }
        return selectedItemLight;
    }

    public void setSelectedItemLight(ItemLight selectedItemLight) {
        this.selectedItemLight = selectedItemLight;
        if (selectedItemLight == null) {
            setCurrent(null);
        } else {
            setCurrent(findItem(selectedItemLight.getId()));
        }
    }

    public List<DepartmentItemCount> getDepartmentItemCounts() {
        return departmentItemCounts;
    }

    public void setDepartmentItemCounts(List<DepartmentItemCount> departmentItemCounts) {
        this.departmentItemCounts = departmentItemCounts;
    }

    public DepartmentItemCount getDepartmentItemCount() {
        return departmentItemCount;
    }

    public void setDepartmentItemCount(DepartmentItemCount departmentItemCount) {
        this.departmentItemCount = departmentItemCount;
    }

    public List<InstitutionItemCount> getInstitutionItemCounts() {
        return institutionItemCounts;
    }

    public void setInstitutionItemCounts(List<InstitutionItemCount> institutionItemCounts) {
        this.institutionItemCounts = institutionItemCounts;
    }

    public InstitutionItemCount getInstitutionItemCount() {
        return institutionItemCount;
    }

    public void setInstitutionItemCount(InstitutionItemCount institutionItemCount) {
        this.institutionItemCount = institutionItemCount;
    }

    public List<ItemLight> getFilteredItems() {
        return filteredItems;
    }

    public void setFilteredItems(List<ItemLight> filteredItems) {
        this.filteredItems = filteredItems;
    }

    public Institution getFilterInstitution() {
        return filterInstitution;
    }

    public void setFilterInstitution(Institution filterInstitution) {
        this.filterInstitution = filterInstitution;
    }

    public Department getFilterDepartment() {
        return filterDepartment;
    }

    public void setFilterDepartment(Department filterDepartment) {
        this.filterDepartment = filterDepartment;
    }

    @FacesConverter("itemLightConverter")
    public static class ItemLightConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext context, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            try {
                Long id = Long.valueOf(value);
                ItemController controller = (ItemController) context.getApplication().getELResolver()
                        .getValue(context.getELContext(), null, "itemController");
                ItemLight il = controller.findItemLightById(id);
                return il;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public String getAsString(FacesContext context, UIComponent component, Object value) {
            if (value instanceof ItemLight) {
                return ((ItemLight) value).getId().toString(); // Assuming getId() returns the ID
            }
            return null; // Or handle the error condition
        }
    }

    @FacesConverter(forClass = Item.class)
    public static class ItemControllerConverter implements Converter {

        /**
         *
         * @param facesContext
         * @param component
         * @param value
         * @return
         */
        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ItemController controller = (ItemController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "itemController");
            return controller.getItemFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key = 0l;
            try {
                key = Long.valueOf(value);
            } catch (Exception e) {

            }

            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        /**
         *
         * @param facesContext
         * @param component
         * @param object
         * @return
         */
        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Item) {
                Item o = (Item) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ItemController.class.getName());
            }
        }
    }

    /**
     *
     */
}
