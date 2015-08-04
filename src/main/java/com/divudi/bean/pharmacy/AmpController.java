/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.DepartmentType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Service;
import com.divudi.facade.AmpFacade;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.Ampp;
import com.divudi.entity.pharmacy.Vmp;
import com.divudi.entity.pharmacy.Vtm;
import com.divudi.entity.pharmacy.VtmsVmps;
import com.divudi.facade.StockFacade;
import com.divudi.facade.VmpFacade;
import com.divudi.facade.VtmsVmpsFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
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
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class AmpController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private AmpFacade ejbFacade;
    List<Amp> selectedItems;
    private Amp current;
    private Vtm vtm;
    private List<Amp> items = null;
    String selectText = "";
    private String tabId = "tabVmp";
    private VtmsVmps addingVtmInVmp;
    private Vmp currentVmp;
    @EJB
    private VmpFacade vmpFacade;
    @EJB
    private VtmsVmpsFacade vivFacade;
    List<Amp> itemsByCode = null;
    List<Amp> listToRemove = null;
    Department department;
    List<Amp> itemList;
    

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

    @EJB
    PharmacyBean pharmacyBean;

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    @EJB
    StockFacade stockFacade;

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
                UtilityController.addErrorMessage(s.getName() + " NOT Removed Beacause there is stock");
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

    public void createItemList() {
        Map m = new HashMap();
        m.put("dep", DepartmentType.Store);
        String sql = "select c from Amp c "
                + " where c.retired=false "
                + " and (c.departmentType is null"
                + " or c.departmentType!=:dep )"
                + " order by c.name";

        items = getFacade().findBySQL(sql, m);
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

        items = getFacade().findBySQL(sql, m);
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
        return getFacade().findBySQL(sql, m);
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
        return getFacade().findBySQL(sql, m);
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
        if (selectText.trim().equals("")) {
            selectedItems = getFacade().findBySQL("select c from Amp c where c.retired=false order by c.name");
        } else {
            selectedItems = getFacade().findBySQL("select c from Amp c where c.retired=false and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        }
        return selectedItems;
    }
    
    
    public List<Amp> completeAmp(String qry) {
        List<Amp> a = null;
        Map m = new HashMap();
        m.put("n", "%" + qry + "%");
        m.put("dep", DepartmentType.Store);
        if (qry != null) {
            a = getFacade().findBySQL("select c from Amp c where "
                    + " c.retired=false and (c.departmentType!=:dep or c.departmentType is null) "
                    + " and (upper(c.name) like :n or upper(c.code)  "
                    + "like :n or upper(c.barcode) like :n) order by c.name", m, 30);
        }

        if (a == null) {
            a = new ArrayList<>();
        }
        return a;
    }
    List<Amp> ampList = null;

    public List<Amp> completeAmpByName(String qry) {

        Map m = new HashMap();
        m.put("n", "%" + qry + "%");
        m.put("dep", DepartmentType.Store);
        if (qry != null) {
            ampList = getFacade().findBySQL("select c from Amp c where "
                    + " c.retired=false and"
                    + " (c.departmentType is null"
                    + " or c.departmentType!=:dep )and "
                    + "(upper(c.name) like :n ) order by c.name", m, 30);
            ////System.out.println("a size is " + a.size());
        }
        if (ampList == null) {
            ampList = new ArrayList<>();
        }
        return ampList;
    }
    
    

    public List<Amp> completeAmpByCode(String qry) {

        Map m = new HashMap();
        m.put("n", "%" + qry + "%");
        m.put("dep", DepartmentType.Store);
        if (qry != null) {
            ampList = getFacade().findBySQL("select c from Amp c where "
                    + " c.retired=false and (c.departmentType is null or c.departmentType!=:dep) and "
                    + "(upper(c.code) like :n ) order by c.code", m, 30);
            ////System.out.println("a size is " + a.size());
        }
        if (ampList == null) {
            ampList = new ArrayList<>();
        }
        return ampList;
    }

    public List<Amp> completeAmpByBarCode(String qry) {

        Map m = new HashMap();
        m.put("n", "%" + qry + "%");
        m.put("dep", DepartmentType.Store);
        String sql = "select c from Amp c where "
                + " c.retired=false and c.departmentType!=:dep and "
                + "(upper(c.barcode) like :n ) order by c.barcode";
     //   //System.out.println("sql = " + sql);
        //   //System.out.println("m = " + m);

        if (qry != null) {
            ampList = getFacade().findBySQL(sql, m, 30);
            //   //System.out.println("a = " + a);
            ////System.out.println("a size is " + a.size());
        }
        if (ampList == null) {
            ampList = new ArrayList<>();
        }
        return ampList;
    }
    @EJB
    BillNumberGenerator billNumberBean;

    public void prepareAdd() {
        current = new Amp();
        currentVmp = new Vmp();
        addingVtmInVmp = new VtmsVmps();
        //(dangerous function dont touch)current.setCode(billNumberBean.pharmacyItemNumberGenerator());
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
//            UtilityController.addErrorMessage("Please Select Manufacturer");
//            return true;
//        }
        
        if(current.getCategory() == null){
            UtilityController.addErrorMessage("Please Select Category");
            return true;
        }

        if (getTabId().toString().equals("tabVmp")) {
            if (getCurrent().getVmp() == null) {
                UtilityController.addErrorMessage("Please Select VMP");
                return true;
            }
        }

        return false;
    }

    private boolean errorCheckForGen() {
        if (addingVtmInVmp == null) {
            return true;
        }
        if (addingVtmInVmp.getVtm() == null) {
            UtilityController.addErrorMessage("Select Vtm");
            return true;
        }

        if (currentVmp == null) {
            return true;
        }
        if (addingVtmInVmp.getStrength() == 0.0) {
            UtilityController.addErrorMessage("Type Strength");
            return true;
        }
        if (currentVmp.getCategory() == null) {
            UtilityController.addErrorMessage("Select Category");
            return true;
        }
        if (addingVtmInVmp.getStrengthUnit() == null) {
            UtilityController.addErrorMessage("Select Strenth Unit");
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
        if (getTabId().toString().equals("tabGen")) {
            return getCurrentVmp().getName();
        } else {
            return getCurrent().getVmp().getName();
        }
    }

    private void saveVmp() {
        if (currentVmp.getName() == null || currentVmp.getName().equals("")) {
            currentVmp.setName(createVmpName());
        }

        if (currentVmp.getId() == null || currentVmp.getId() == 0) {
            getVmpFacade().create(currentVmp);
        } else {
            getVmpFacade().edit(currentVmp);
        }

    }

    public void saveSelected() {
        if (errorCheck()) {
            return;
        }

        if (getTabId().toString().equals("tabGen")) {
            if (errorCheckForGen()) {
                return;
            }

            saveVmp();
            getAddingVtmInVmp().setVmp(currentVmp);
            if (getAddingVtmInVmp().getId() == null || getAddingVtmInVmp().getId() == null) {
                getVivFacade().create(getAddingVtmInVmp());
            } else {
                getVivFacade().edit(getAddingVtmInVmp());
            }

            getCurrent().setVmp(currentVmp);
        }

        if (current.getName() == null || current.getName().equals("")) {
            current.setName(createAmpName());
        }

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        // getItems();
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
        addingVtmInVmp = new VtmsVmps();
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

    private AmpFacade getFacade() {
        return ejbFacade;
    }
    private List<Amp> filteredItems;

    public List<Amp> getItems() {
        if (items == null) {
            items = getFacade().findAll("name", true);
        }
        return items;
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

    public VtmsVmps getAddingVtmInVmp() {
        if (addingVtmInVmp == null) {
            addingVtmInVmp = new VtmsVmps();
        }
        return addingVtmInVmp;
    }

    public void setAddingVtmInVmp(VtmsVmps addingVtmInVmp) {
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

    public VtmsVmpsFacade getVivFacade() {
        return vivFacade;
    }

    public void setVivFacade(VtmsVmpsFacade vivFacade) {
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

    
    /**
     *
     */
    @FacesConverter("ampCon")
    public static class AmpControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            AmpController controller = (AmpController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "ampController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key = 0l;
            try {
                key = Long.valueOf(value);
            } catch (Exception e) {
                key = 0l;
            }
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
            if (object instanceof Amp) {
                Amp o = (Amp) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + AmpController.class.getName());
            }
        }
    }

    @FacesConverter(forClass = Amp.class)
    public static class AmpConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            AmpController controller = (AmpController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "ampController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key = 0l;
            try {
                key = Long.valueOf(value);
            } catch (Exception e) {
                key = 0l;
            }
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
