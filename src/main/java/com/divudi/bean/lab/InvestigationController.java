/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.lab;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.InvestigationItemType;
import com.divudi.data.SymanticType;
import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.ItemFeeManager;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.ItemFee;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.InvestigationCategory;
import com.divudi.entity.lab.ReportItem;
import com.divudi.entity.lab.WorksheetItem;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.InvestigationFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.SpecialityFacade;
import com.divudi.facade.WorksheetItemFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class InvestigationController implements Serializable {

    /**
     * Managed Beans
     */
    @Inject
    SessionController sessionController;
    @Inject
    private BillBeanController billBean;
    @Inject
    InvestigationItemController investigationItemController;
    @Inject
    IxCalController ixCalController;
    @Inject
    ItemFeeManager itemFeeManager;
    /**
     * EJBs
     */
    @EJB
    private InvestigationFacade ejbFacade;
    @EJB
    private SpecialityFacade specialityFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    /**
     * Properties
     */
    List<Investigation> selectedItems;
    private Investigation current;
    private List<Investigation> items = null;
    String selectText = "";
    String bulkText = "";
    boolean billedAs;
    boolean reportedAs;
    boolean listMasterItemsOnly = false;//if boolean is true list only institution null
    InvestigationCategory category;
    List<Investigation> catIxs;
    List<Investigation> allIxs;
    List<Investigation> itemsToRemove;
    Institution institution;
    List<Investigation> deletedIxs;
    List<Investigation> selectedIxs;

    public String toEditReportFormat() {
        if (current == null) {
            JsfUtil.addErrorMessage("Please select investigation");
            return "";
        }
        if (current.getId() == null) {
            JsfUtil.addErrorMessage("Please save investigation first.");
            return "";
        }
        if (current.getReportedAs() == null) {
            current.setReportedAs(current);
        }
        investigationItemController.setCurrentInvestigation((Investigation) current.getReportedAs());

        return "lab_investigation_format";
    }

    public String toEditReportCalculations() {
        if (current == null) {
            JsfUtil.addErrorMessage("Please select investigation");
            return "";
        }
        if (current.getId() == null) {
            JsfUtil.addErrorMessage("Please save investigation first.");
            return "";
        }
        if (current.getReportedAs() == null) {
            current.setReportedAs(current);
        }
        ixCalController.setIx((Investigation) current.getReportedAs()); 
        return "lab_calculation";
    }
    
    public String toEditFees() {
        if (current == null) {
            JsfUtil.addErrorMessage("Please select investigation");
            return "";
        }
        if (current.getId() == null) {
            JsfUtil.addErrorMessage("Please save investigation first.");
            return "";
        }
        itemFeeManager.setItem(current);
        itemFeeManager.fillFees();
        return "/common/manage_item_fees";
    }

    public void listDeletedIxs() {
        String sql = "select c from Investigation c where c.retired=true ";
        deletedIxs = getFacade().findBySQL(sql);
    }

    public void undeleteSelectedIxs() {
        for (Investigation s : selectedIxs) {
            s.setRetired(false);
            s.setRetiredAt(null);
            s.setRetirer(null);
            getFacade().edit(s);
            //System.out.println("undeleted = " + s);
        }
        selectedIxs = null;
        listDeletedIxs();
    }

    public void deleteSelectedInvestigations() {
        for (Investigation s : selectedIxs) {
            s.setRetired(true);
            s.setRetiredAt(new Date());
            s.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(s);
        }
    }

    public List<Investigation> getDeletedIxs() {
        return deletedIxs;
    }

    public void setDeletedIxs(List<Investigation> deletedIxs) {
        this.deletedIxs = deletedIxs;
    }

    public List<Investigation> getSelectedIxs() {
        return selectedIxs;
    }

    public void setSelectedIxs(List<Investigation> selectedIxs) {
        this.selectedIxs = selectedIxs;
    }

    public List<Investigation> getItemsToRemove() {
        return itemsToRemove;
    }

    public void setItemsToRemove(List<Investigation> itemsToRemove) {
        this.itemsToRemove = itemsToRemove;
    }

    public void removeSelectedItems() {
        for (Investigation s : itemsToRemove) {
            s.setRetired(true);
            s.setRetireComments("Bulk Remove");
            s.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(s);
        }
        itemsToRemove = null;
        items = null;
    }

    public String listAllIxs() {
        String sql;
        sql = "Select i from Investigation i where i.retired=false order by i.name";
        allIxs = getFacade().findBySQL(sql);
        return "/lab/lab_investigation_list";
    }

    public List<Department> getInstitutionDepatrments() {
        List<Department> d;
        ////System.out.println("gettin ins dep ");
        if (getCurrent().getInstitution() == null) {
            return new ArrayList<Department>();
        } else {
            String sql = "Select d From Department d where d.retired=false and d.institution.id=" + getCurrent().getInstitution().getId();
            d = getDepartmentFacade().findBySQL(sql);
        }

        return d;
    }

    public InvestigationCategory getCategory() {
        return category;
    }

    public void setCategory(InvestigationCategory category) {
        catIxs = null;
        this.category = category;
    }

    public void catToIxCat() {
        for (Investigation i : getItems()) {
            i.setCategory(i.getInvestigationCategory());
            getFacade().edit(i);
        }
        UtilityController.addSuccessMessage("Saved");
    }

    @EJB
    WorksheetItemFacade worksheetItemFacade;

    public WorksheetItemFacade getWorksheetItemFacade() {
        return worksheetItemFacade;
    }

    public void setWorksheetItemFacade(WorksheetItemFacade worksheetItemFacade) {
        this.worksheetItemFacade = worksheetItemFacade;
    }

    public void reportItemsToWorksheetItems() {
        for (WorksheetItem wi : getWorksheetItemFacade().findAll()) {
            ////System.out.println("item removing is " + wi);
            getWorksheetItemFacade().remove(wi);
        }
        for (Investigation i : getItems()) {
            for (ReportItem ri : i.getReportItems()) {
                if (ri.getIxItemType() == InvestigationItemType.Value && ri.isRetired() == false) {
                    WorksheetItem wi = new WorksheetItem();
                    wi.setItem(i);
                    wi.setName(ri.getName());
                    i.getWorksheetItems().add(wi);
                    ////System.out.println("Worksheet added " + wi);
                }
            }
            getItemFacade().edit(i);
        }
    }

    @EJB
    ItemFacade itemFacade;

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public List<Investigation> getCatIxs() {
        if (catIxs == null) {
            if (category == null) {
                catIxs = getItems();
            } else {
                Map m = new HashMap();
                String sql = "select i from Investigation i where i.retired=false and i.investigationCategory = :cat order by i.department.name, i.name";
                m.put("cat", getCategory());
                catIxs = getFacade().findBySQL(sql, m);
            }
        }
        return catIxs;
    }

    public void setCatIxs(List<Investigation> catIxs) {
        this.catIxs = catIxs;
    }

    public List<Investigation> completeInvest(String query) {
        System.out.println("master" + listMasterItemsOnly);
        System.out.println("master login Lab");
        if (query == null || query.trim().equals("")) {
            return new ArrayList<>();
        }
        List<Investigation> suggestions;
        String sql;
        Map m = new HashMap();

        //m.put(m, m);
        sql = "select c from Investigation c "
                + " where c.retired=false "
                + " and (upper(c.name) like :n or "
                + " upper(c.fullName) like :n or "
                + " upper(c.code) like :n or upper(c.printName) like :n ) ";
        ////System.out.println(sql);

        m.put("n", "%" + query.toUpperCase() + "%");

        if (listMasterItemsOnly == true) {
            System.out.println("inside intitution null only");
            sql += " and c.institution is null ";
        }

        if (sessionController.getInstitutionPreference().isInstitutionSpecificItems()) {
            System.out.println("inside intitution null and logged institution only");
            sql += " and (c.institution is null "
                    + " or c.institution=:ins) ";
            m.put("ins", sessionController.getInstitution());
        }

        sql += " order by c.name";

        suggestions = getFacade().findBySQL(sql, m);

        return suggestions;
    }

    public List<Investigation> completeInvestWithout(String query) {
        List<Investigation> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            // sql = "select c from Investigation c where c.retired=false and upper(c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            sql = "select c from Investigation c where c.retired=false and type(c)!=Packege and upper(c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            ////System.out.println(sql);
            suggestions = getFacade().findBySQL(sql);
        }
        return suggestions;
    }

    public boolean isBilledAs() {
        return billedAs;
    }

    public void setBilledAs(boolean billedAs) {
        this.billedAs = billedAs;
    }

    public boolean isReportedAs() {
        return reportedAs;
    }

    public void setReportedAs(boolean reportedAs) {
        this.reportedAs = reportedAs;
    }

    public boolean isListMasterItemsOnly() {
        return listMasterItemsOnly;
    }

    public void setListMasterItemsOnly(boolean listMasterItemsOnly) {
        this.listMasterItemsOnly = listMasterItemsOnly;
    }

    public void correctIx() {
        List<Investigation> allItems = getEjbFacade().findAll();
        for (Investigation i : allItems) {
            i.setPrintName(i.getName());
            i.setFullName(i.getName());
            i.setShortName(i.getName());
            i.setDiscountAllowed(Boolean.TRUE);
            i.setUserChangable(false);
            i.setTotal(getBillBean().totalFeeforItem(i));
            getEjbFacade().edit(i);
        }

    }

    public void correctIx1() {
        List<Investigation> allItems = getEjbFacade().findAll();
        for (Investigation i : allItems) {
            i.setBilledAs(i);
            i.setReportedAs(i);
            getEjbFacade().edit(i);
        }

    }

    public String getBulkText() {

        return bulkText;
    }

    public void setBulkText(String bulkText) {
        this.bulkText = bulkText;
    }

    public List<Investigation> getInstitutionSelectedItems() {
        Map m = new HashMap();
        String sql;
        sql = "select c "
                + " from Investigation c "
                + " where c.retired=:r ";
        m.put("r", false);
        if (selectText != null && !selectText.trim().equals("")) {
            sql += " and upper(c.name) like :st ";
            m.put("st", "%" + getSelectText().toUpperCase() + "%");
        }
        if (sessionController.getInstitutionPreference().isInstitutionSpecificItems()) {
            sql += " and c.institution=:ins";
            m.put("ins", institution);
        }
        sql += " order by c.name";
        selectedItems = getFacade().findBySQL(sql, m);
        return selectedItems;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<Investigation> getSelectedItems() {
        if (selectText.trim().equals("")) {
            selectedItems = getFacade().findBySQL("select c from Investigation c where c.retired=false order by c.name");
        } else {
            selectedItems = getFacade().findBySQL("select c from Investigation c where c.retired=false and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        }
        return selectedItems;
    }

    public List<Investigation> completeItem(String qry) {
        List<Investigation> completeItems = getFacade().findBySQL("select c from Item c where ( type(c) = Investigation or type(c) = Packege ) and c.retired=false and upper(c.name) like '%" + qry.toUpperCase() + "%' order by c.name");
        return completeItems;
    }

//    public List<Investigation> completeDepartmentItem(String qry) {
//        if (getSessionController().getInstitutionPreference().isInstitutionSpecificItems()) {
//            String sql;
//            Map m = new HashMap();
//            m.put("qry", "'%" + qry.toUpperCase() + "%'");
//            m.put("inv", Investigation.class);
//            m.put("ser", Investigation.class);
//            m.put("pak", Investigation.class);
//            m.put("ins", getSessionController().getInstitution());
//            sql = "select c "
//                    + " from Item c "
//                    + " where (type(c) =:inv or type(c) = :ser or type(c) = :pak) "
//                    + " and c.retired=false "
//                    + " and upper(c.name) like :qry "
//                    + " and c.institution=:ins ";
//            sql += "order by c.name";
//            List<Investigation> completeItems = getFacade().findBySQL(sql, m);
//            return completeItems;
//        } else {
//            return completeItem(qry);
//        }
//    }
    public List<Investigation> completeDepartmentItem(String qry) {
        if (getSessionController().getInstitutionPreference().isInstitutionSpecificItems()) {
            String sql;
            Map m = new HashMap();
//            m.put("qry", "'%" + qry.toUpperCase() + "%'");
//            m.put("inv", Investigation.class);
//            m.put("ser", Investigation.class);
//            m.put("pak", Investigation.class);
            m.put("ins", getSessionController().getInstitution());
            sql = "select c from Item c where ( type(c) = Investigation or type(c) = Packege ) "
                    + "and c.retired=false and c.institution=:ins and upper(c.name) like '%" + qry.toUpperCase() + "%' order by c.name";
            List<Investigation> completeItems = getFacade().findBySQL(sql, m);
            return completeItems;
        } else {
            return completeItem(qry);
        }
    }

    public void prepareAdd() {
        current = new Investigation();
    }

    public void bulkUpload() {
        List<String> lstLines = Arrays.asList(getBulkText().split("\\r?\\n"));
        for (String s : lstLines) {
            List<String> w = Arrays.asList(s.split(","));
            try {
                String code = w.get(0);
                String ix = w.get(1);
                String ic = w.get(2);
                String f = w.get(4);
                ////System.out.println(code + " " + ix + " " + ic + " " + f);

                Investigation tix = new Investigation();
                tix.setCode(code);
                tix.setName(ix);
                tix.setDepartment(null);

            } catch (Exception e) {
            }

        }
    }

    public void setSelectedItems(List<Investigation> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    private boolean errorCheck() {
        if (getCurrent().isUserChangable() && getCurrent().isDiscountAllowed() == true) {
            UtilityController.addErrorMessage("Cant tick both User can Change & Discount Allowed");
            return true;
        }
        return false;
    }

    public void makeSymanticTypeForAllIx() {
        for (Investigation i : getItems()) {
            i.setSymanticType(SymanticType.Laboratory_Procedure);
            getFacade().edit(i);
        }
        UtilityController.addSuccessMessage("Updated");
    }

    public void saveSelected() {

//        if (errorCheck()) {
//            return;
//        }
        getCurrent().setCategory(getCurrent().getInvestigationCategory());
        getCurrent().setSymanticType(SymanticType.Laboratory_Procedure);
//        getCurrent().setInstitution(institution);
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            ////System.out.println("1");
            if (billedAs == false) {
                ////System.out.println("2");
                getCurrent().setBilledAs(getCurrent());

            }
            if (reportedAs == false) {
                ////System.out.println("3");
                getCurrent().setReportedAs(getCurrent());
            }
            getFacade().edit(getCurrent());
            UtilityController.addSuccessMessage("Updated Successfully.");
        } else {
            ////System.out.println("4");
            getCurrent().setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            if (billedAs == false) {
                ////System.out.println("5");
                getCurrent().setBilledAs(getCurrent());
            }
            if (reportedAs == false) {
                ////System.out.println("6");
                getCurrent().setReportedAs(getCurrent());
            }
            getFacade().edit(getCurrent());
            UtilityController.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public InvestigationFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(InvestigationFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public InvestigationController() {
    }

    public Investigation getCurrent() {
        if (current == null) {
            current = new Investigation();
        }
        return current;
    }

    public void setCurrent(Investigation current) {
        this.current = current;
        if (current != null) {
            if (current.getBilledAs() == current) {
                billedAs = false;
            } else {
                billedAs = true;
            }
            if (current.getReportedAs() == current) {
                reportedAs = false;
            } else {
                reportedAs = true;
            }
        }
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private InvestigationFacade getFacade() {
        return ejbFacade;
    }
    @EJB
    private ItemFeeFacade itemFeeFacade;

    public List<ItemFee> getItemFee() {
        List<ItemFee> temp;
        temp = getItemFeeFacade().findBySQL("select c from ItemFee c where c.retired = false and type(c.item) =Investigation order by c.item.name");

        if (temp == null) {
            return new ArrayList<ItemFee>();
        }

        return temp;
    }

    public List<Investigation> getItems() {
        if (items == null) {
            fillItems();
        }
        return items;
    }

    public void fillItems() {
        String sql = "select i from Investigation i where i.retired=false order by i.department.name, i.name";
        items = getFacade().findBySQL(sql);
    }

    public SpecialityFacade getSpecialityFacade() {
        return specialityFacade;
    }

    public void setSpecialityFacade(SpecialityFacade specialityFacade) {
        this.specialityFacade = specialityFacade;
    }

    public ItemFeeFacade getItemFeeFacade() {
        return itemFeeFacade;
    }

    public void setItemFeeFacade(ItemFeeFacade itemFeeFacade) {
        this.itemFeeFacade = itemFeeFacade;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public List<Investigation> getAllIxs() {
        return allIxs;
    }

    public void setAllIxs(List<Investigation> allIxs) {
        this.allIxs = allIxs;
    }

    /**
     *
     */
    @FacesConverter("ixcon")
    public static class InvestigationConverter implements Converter {

        public InvestigationConverter() {
        }

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            InvestigationController controller = (InvestigationController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "investigationController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Investigation) {
                Investigation o = (Investigation) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + InvestigationController.class.getName());
            }
        }
    }
}
