/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.common;

import com.divudi.data.FeeType;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.data.dataStructure.ServiceFee;
import com.divudi.data.SessionNumberType;
import com.divudi.entity.Department;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.entity.Service;
import com.divudi.facade.CategoryFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.ServiceCategoryFacade;
import com.divudi.facade.ServiceFacade;
import com.divudi.facade.ServiceSubCategoryFacade;
import com.divudi.facade.SpecialityFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 Informatics)
 */
@Named
@SessionScoped
public class ServiceController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    private ServiceSubCategoryController serviceSubCategoryController;
    @EJB
    private ServiceFacade ejbFacade;
    @EJB
    private SpecialityFacade specialityFacade;
    @Inject
    private BillBeanController billBean;
    @EJB
    private ServiceCategoryFacade serviceCategoryFacade;
    @EJB
    private ServiceSubCategoryFacade serviceSubCategoryFacade;
    @EJB
    private CategoryFacade categoryFacade;
    List<Service> selectedItems;
    List<Service> selectedRetiredItems;
    private Service current;
    Service currentInactiveService;    
    private List<Service> items = null;
    private List<Service> filterItem;
    String selectText = "";
    String selectRetiredText = "";
    String bulkText = "";
    boolean billedAs;
    boolean reportedAs;
    @EJB
    private DepartmentFacade departmentFacade;
    List<Service> itemsToRemove;

    public List<Service> getItemsToRemove() {
        return itemsToRemove;
    }

    public void setItemsToRemove(List<Service> itemsToRemove) {
        this.itemsToRemove = itemsToRemove;
    }

    public void removeSelectedItems() {
        for (Service s : itemsToRemove) {
            s.setRetired(true);
            s.setRetireComments("Bulk Remove");
            s.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(s);
        }
        itemsToRemove=null;
        items=null;
    }

    public List<Service> getSelectedRetiredItems() {
        return selectedRetiredItems;
    }

    public void setSelectedRetiredItems(List<Service> selectedRetiredItems) {
        this.selectedRetiredItems = selectedRetiredItems;
    }

    public String getSelectRetiredText() {
        return selectRetiredText;
    }

    public void setSelectRetiredText(String selectRetiredText) {
        this.selectRetiredText = selectRetiredText;
    }

    public Service getCurrentInactiveService() {
        return currentInactiveService;
    }

    public void setCurrentInactiveService(Service currentInactiveService) {
        this.currentInactiveService = currentInactiveService;
    }
          

    public List<Department> getInstitutionDepatrments() {
        List<Department> d;
        ////System.out.println("gettin ins dep ");
        if (getCurrent().getInstitution() == null) {
            return new ArrayList<>();
        } else {
            String sql = "Select d From Department d where d.retired=false and d.institution.id=" + getCurrent().getInstitution().getId();
            d = getDepartmentFacade().findBySQL(sql);
        }

        return d;
    }

    public List<Service> completeService(String query) {
        List<Service> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<Service>();
        } else {
            sql = "select c from Service c where c.retired=false and upper(c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            ////System.out.println(sql);
            suggestions = getFacade().findBySQL(sql);
        }
        return suggestions;
    }

    public List<Service> getSelectedItems() {
        if (selectText.trim().equals("")) {
            selectedItems = getFacade().findBySQL("select c from Service c where c.retired=false order by c.name");
        } else {
            selectedItems = getFacade().findBySQL("select c from Service c where c.retired=false and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        }
        return selectedItems;
    }

    public List<Service> getRetiredSelectedItems() {
        if (selectRetiredText.trim().equals("")) {
            selectedRetiredItems = getFacade().findBySQL("select c from Service c where c.retired=true order by c.name");
        } else {
            selectedRetiredItems = getFacade().findBySQL("select c from Service c where c.retired=true and upper(c.name) like '%" + getSelectRetiredText().toUpperCase() + "%' order by c.name");
        }
        return selectedRetiredItems;
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

    public void correctIx() {
        List<Service> allItems = getEjbFacade().findAll();
        for (Service i : allItems) {
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
        List<Service> allItems = getEjbFacade().findAll();
        for (Service i : allItems) {
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

    public List<Service> completeItem(String qry) {
        List<Service> completeItems = getFacade().findBySQL("select c from Item c "
                + " where ( type(c) = Service or type(c) = Packege ) "
                + " and c.retired=false and upper(c.name) like '%" + qry.toUpperCase() + "%'"
                + "  order by c.name");
        return completeItems;
    }

    public void prepareAdd() {
        current = new Service();
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

                Service tix = new Service();
                tix.setCode(code);
                tix.setName(ix);
                tix.setDepartment(null);

            } catch (Exception e) {
            }

        }
    }

    public void setSelectedItems(List<Service> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    public void recreateModel() {
        items = null;
        filterItem = null;
    }

    private boolean errorCheck() {
        if (getCurrent().isUserChangable() && getCurrent().isDiscountAllowed() == true) {
            UtilityController.addErrorMessage("Cant tick both User can Change & Discount Allowed");
            return true;
        }
        return false;
    }

    public void saveSelected() {

        if (getCurrent().getDepartment() == null) {
            UtilityController.addErrorMessage("Please Select Department");
            return;
        }
        if (getCurrent().getInwardChargeType() == null) {
            UtilityController.addErrorMessage("Please Select Inward Charge type");
            return;
        }

//        if (errorCheck()) {
//            return;
//        }
//        if (getServiceSubCategoryController().getParentCategory() != null) {
//            getCurrent().setCategory(getServiceSubCategoryController().getParentCategory());
//        }
        //System.out.println("getCurrent().getId() = " + getCurrent());
        //System.out.println("getCurrent().getId() = " + getCurrent().getId());
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
            UtilityController.addSuccessMessage("Saved Old Successfully");
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
        recreateModel();
        this.selectText = selectText;
    }

    public ServiceFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(ServiceFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public ServiceController() {
    }

    public Service getCurrent() {
        if (current == null) {
            current = new Service();
        }
        return current;
    }

    public void setCurrent(Service current) {
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

        for (ItemFee it : getFees(current)) {
            it.setRetired(true);
            it.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            it.setRetirer(getSessionController().getLoggedUser());
            getItemFeeFacade().edit(it);
        }

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Deleted Successfull");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getSelectedItems();

    }
    
    public void activateService() {

        for (ItemFee it : getFees(currentInactiveService)) {
            it.setRetired(true);
            it.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            it.setRetirer(getSessionController().getLoggedUser());
            getItemFeeFacade().edit(it);
        }

        if (currentInactiveService != null) {
            currentInactiveService.setRetired(false);
            currentInactiveService.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            currentInactiveService.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(currentInactiveService);
            UtilityController.addSuccessMessage("Deleted Successfull");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getSelectedRetiredItems();

    }

    private ServiceFacade getFacade() {
        return ejbFacade;
    }
    @EJB
    private ItemFeeFacade itemFeeFacade;

    public List<ItemFee> getItemFee() {
        List<ItemFee> temp;
        HashMap hash = new HashMap();
        String sql = "select c from ItemFee c where c.retired = false and type(c.item) = :ser order by c.item.name";
        hash.put("ser", Service.class);
        temp = getItemFeeFacade().findBySQL(sql, hash, TemporalType.TIMESTAMP);

        if (temp == null) {
            return new ArrayList<ItemFee>();
        }

        return temp;
    }

    public List<ServiceFee> getServiceFee() {

        List<ServiceFee> temp = new ArrayList<ServiceFee>();

        for (Service s : getItem()) {
            ServiceFee si = new ServiceFee();
            si.setService(s);

            String sql = "select c from ItemFee c where c.retired = false and c.item.id =" + s.getId();

            si.setItemFees(getItemFeeFacade().findBySQL(sql));

            temp.add(si);
        }

        return temp;
    }
    
    List<Service> deletedServices;
    List<Service> deletingServices;

    public List<Service> getDeletedServices() {
        return deletedServices;
    }

    public void setDeletedServices(List<Service> deletedServices) {
        this.deletedServices = deletedServices;
    }

    public List<Service> getDeletingServices() {
        return deletingServices;
    }

    public void setDeletingServices(List<Service> deletingServices) {
        this.deletingServices = deletingServices;
    }

    
    public void listDeletedServices() {
        String sql = "select c from Service c where c.retired=true order by c.category.name,c.department.name";
        deletedServices = getFacade().findBySQL(sql);
        if (deletedServices == null) {
            deletedServices = new ArrayList<>();
        }
    }

    public void undeleteSelectedServices(){
        for(Service s:deletingServices){
            s.setRetired(false);
            s.setRetiredAt(null);
            s.setRetirer(null);
            getFacade().edit(s);
            //System.out.println("undeleted = " + s);
        }
        deletingServices = null;
        listDeletedServices();
    }
    

    public List<Service> getItems() {
        if(items==null){
            fillItems();
        }
        return items;
    }
    
    public void fillItems() {
        String sql = "select c from Service c where c.retired=false order by c.category.name,c.department.name";
        items = getFacade().findBySQL(sql);
        for (Service i : items) {
            List<ItemFee> tmp = getFees(i);
            for (ItemFee itf : tmp) {
                i.setItemFee(itf);
            }
        }
        if (items == null) {
            items = new ArrayList<Service>();
        }
    }

    public List<Service> getItem() {
        String sql;
        if (selectText.isEmpty()) {
            sql = "select c from Service c where c.retired=false order by c.category.name,c.name";
        } else {
            sql = "select c from Service c where c.retired=false and upper(c.name) like '%" + selectText.toUpperCase() + "%' order by c.category.name,c.name";
        }
        ////System.out.println(sql);
        items = getFacade().findBySQL(sql);

        if (items == null) {
            items = new ArrayList<Service>();
        }
        return items;
    }

    public List<Service> getServiceDep() {
        if (items == null) {
            String sql;
            sql = "select c from Service c where c.retired=false order by c.category.name,c.name";

            ////System.out.println(sql);
            items = getFacade().findBySQL(sql);

            for (Service i : items) {

                List<ItemFee> tmp = getFees(i);
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

            if (items == null) {
                items = new ArrayList<Service>();
            }

        }
        return items;
    }

    private List<ItemFee> getFees(Item i) {
        String sql = "Select f From ItemFee f where f.retired=false and f.item.id=" + i.getId();

        return getItemFeeFacade().findBySQL(sql);
    }

    public SpecialityFacade getSpecialityFacade() {
        return specialityFacade;
    }

    public void setSpecialityFacade(SpecialityFacade specialityFacade) {
        this.specialityFacade = specialityFacade;
    }

    public ServiceSubCategoryController getServiceSubCategoryController() {
        return serviceSubCategoryController;
    }

    public void setServiceSubCategoryController(ServiceSubCategoryController serviceSubCategoryController) {
        this.serviceSubCategoryController = serviceSubCategoryController;
    }

    public ServiceCategoryFacade getServiceCategoryFacade() {
        return serviceCategoryFacade;
    }

    public void setServiceCategoryFacade(ServiceCategoryFacade serviceCategoryFacade) {
        this.serviceCategoryFacade = serviceCategoryFacade;
    }

    public ServiceSubCategoryFacade getServiceSubCategoryFacade() {
        return serviceSubCategoryFacade;
    }

    public void setServiceSubCategoryFacade(ServiceSubCategoryFacade serviceSubCategoryFacade) {
        this.serviceSubCategoryFacade = serviceSubCategoryFacade;
    }

    public CategoryFacade getCategoryFacade() {
        return categoryFacade;
    }

    public void setCategoryFacade(CategoryFacade categoryFacade) {
        this.categoryFacade = categoryFacade;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public SessionNumberType[] getSessionNumberType() {
        return SessionNumberType.values();
    }

    public ItemFeeFacade getItemFeeFacade() {
        return itemFeeFacade;
    }

    public void setItemFeeFacade(ItemFeeFacade itemFeeFacade) {
        this.itemFeeFacade = itemFeeFacade;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public List<Service> getFilterItem() {
        return filterItem;
    }

    public void setFilterItem(List<Service> filterItem) {
        this.filterItem = filterItem;
    }

    /**
     *
     */
    @FacesConverter(forClass = Service.class)
    public static class ServiceControllerConverter implements Converter {

        public ServiceControllerConverter() {
        }

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ServiceController controller = (ServiceController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "serviceController");
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
            if (object instanceof Service) {
                Service o = (Service) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ServiceController.class.getName());
            }
        }
    }

    @FacesConverter("serv")
    public static class ServiceConverter implements Converter {

        public ServiceConverter() {
        }

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ServiceController controller = (ServiceController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "serviceController");
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
            if (object instanceof Service) {
                Service o = (Service) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ServiceController.class.getName());
            }
        }
    }
}
