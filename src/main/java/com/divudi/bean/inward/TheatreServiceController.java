/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.inward;
import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.ServiceController;
import com.divudi.bean.common.ServiceSubCategoryController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.FeeType;
import com.divudi.data.SessionNumberType;
import com.divudi.data.dataStructure.ServiceFee;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.entity.Department;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.entity.inward.TheatreService;
import com.divudi.facade.CategoryFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.ServiceCategoryFacade;
import com.divudi.facade.ServiceSubCategoryFacade;
import com.divudi.facade.SpecialityFacade;
import com.divudi.facade.TheatreServiceFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class TheatreServiceController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    private ServiceSubCategoryController serviceSubCategoryController;
    @EJB
    TheatreServiceFacade theatreServiceFacade;
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
//    List<InwardService> selectedItems;
    List<TheatreService> selectedItems;
//    private InwardService current;
    TheatreService current;
//    private List<InwardService> items = null;
    List<TheatreService> items = null;
//    private List<InwardService> filterItem;
    List<TheatreService> filterItem;
    String selectText = "";
    String bulkText = "";
    boolean billedAs;
    boolean reportedAs;
    @EJB
    private DepartmentFacade departmentFacade;
//    List<InwardService> itemsToRemove;
    List<TheatreService> itemsToRemove;

    public void removeSelectedItems() {
        for (TheatreService s : itemsToRemove) {
            s.setRetired(true);
            s.setRetireComments("Bulk Remove");
            s.setRetirer(getSessionController().getLoggedUser());
            getTheatreServiceFacade().edit(s);
        }
    }

    public List<Department> getInstitutionDepatrments() {
        List<Department> d;
        //////// // System.out.println("gettin ins dep ");
        if (getCurrent().getInstitution() == null) {
            return new ArrayList<>();
        } else {
            String sql = "Select d From Department d where d.retired=false and d.institution.id=" + getCurrent().getInstitution().getId();
            d = getDepartmentFacade().findByJpql(sql);
        }

        return d;
    }

//    public List<TheatreService> completeService(String query) {
//        List<TheatreService> suggestions;
//        String sql;
//        if (query == null) {
//            suggestions = new ArrayList<>();
//        } else {
//            sql = "select c from TheatreService c where c.retired=false and (c.name) like '%" + query.toUpperCase() + "%' order by c.name";
//            //////// // System.out.println(sql);
//            suggestions = getTheatreServiceFacade().findByJpql(sql);
//        }
//        return suggestions;
//    }

    public List<TheatreService> getSelectedItems() {
        if (selectText.trim().equals("")) {
            selectedItems = getTheatreServiceFacade().findByJpql("select c from TheatreService c where c.retired=false order by c.name");
        } else {
            selectedItems = getTheatreServiceFacade().findByJpql("select c from TheatreService c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        }
        return selectedItems;
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


    public String getBulkText() {

        return bulkText;
    }

    public void setBulkText(String bulkText) {
        this.bulkText = bulkText;
    }

    public List<TheatreService> completeItem(String qry) {
        List<TheatreService> completeItems = getTheatreServiceFacade().findByJpql("select c from Item c where ( type(c) = TheatreService or type(c) = Packege ) and c.retired=false and (c.name) like '%" + qry.toUpperCase() + "%' order by c.name");
        return completeItems;
    }

    public void prepareAdd() {
        current = new TheatreService();
    
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
                //////// // System.out.println(code + " " + ix + " " + ic + " " + f);

                TheatreService tix = new TheatreService();
                tix.setCode(code);
                tix.setName(ix);
                tix.setDepartment(null);

            } catch (Exception e) {
            }

        }
    }

    public void setSelectedItems(List<TheatreService> selectedItems) {
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
            JsfUtil.addErrorMessage("Cant tick both User can Change & Discount Allowed");
            return true;
        }
        return false;
    }

    public void saveSelected() {

        if (getCurrent().getDepartment() == null) {
            JsfUtil.addErrorMessage("Please Select Department");
            return;
        }
        if (getCurrent().getInwardChargeType() == null) {
            JsfUtil.addErrorMessage("Please Select Inward Charge type");
            return;
        }

//        if (errorCheck()) {
//            return;
//        }
//        if (getServiceSubCategoryController().getParentCategory() != null) {
//            getCurrent().setCategory(getServiceSubCategoryController().getParentCategory());
//        }
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            //////// // System.out.println("1");
            if (billedAs == false) {
                //////// // System.out.println("2");
                getCurrent().setBilledAs(getCurrent());

            }
            if (reportedAs == false) {
                //////// // System.out.println("3");
                getCurrent().setReportedAs(getCurrent());
            }
            getTheatreServiceFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            //////// // System.out.println("4");
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getTheatreServiceFacade().create(getCurrent());
            if (billedAs == false) {
                //////// // System.out.println("5");
                getCurrent().setBilledAs(getCurrent());
            }
            if (reportedAs == false) {
                //////// // System.out.println("6");
                getCurrent().setReportedAs(getCurrent());
            }
            getTheatreServiceFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
        current=null;
        getCurrent();
    }

    public void setSelectText(String selectText) {
        recreateModel();
        this.selectText = selectText;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public TheatreServiceController() {
    }


    public void setCurrent(TheatreService current) {
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
            it.setRetiredAt(new Date());
            it.setRetirer(getSessionController().getLoggedUser());
            getItemFeeFacade().edit(it);
        }

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getTheatreServiceFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current=null;
        getCurrent();


    }

    
    @EJB
    private ItemFeeFacade itemFeeFacade;

    public List<ItemFee> getItemFee() {
        List<ItemFee> temp;
        HashMap hash = new HashMap();
        String sql = "select c from ItemFee c where c.retired = false and type(c.item) = :ser order by c.item.name";
        hash.put("ser", TheatreService.class);
        temp = getItemFeeFacade().findByJpql(sql, hash, TemporalType.TIMESTAMP);

        if (temp == null) {
            return new ArrayList<ItemFee>();
        }

        return temp;
    }

//    public List<ServiceFee> getServiceFee() {
//
//        List<ServiceFee> temp = new ArrayList<ServiceFee>();
//
//        for (TheatreService s : getItem()) {
//            ServiceFee si = new ServiceFee();
//            si.setService(s);
//
//            String sql = "select c from ItemFee c where c.retired = false and c.item.id =" + s.getId();
//
//            si.setItemFees(getItemFeeFacade().findByJpql(sql));
//
//            temp.add(si);
//        }
//
//        return temp;
//    }

    public List<TheatreService> getItems() {
        String sql = "select c from TheatreService c where c.retired=false order by c.category.name,c.department.name";
        //////// // System.out.println(sql);
        items = getTheatreServiceFacade().findByJpql(sql);

        for (TheatreService i : items) {

            List<ItemFee> tmp = getFees(i);
            for (ItemFee itf : tmp) {
                i.setItemFee(itf);
            }
        }

        if (items == null) {
            items = new ArrayList<TheatreService>();
        }
        return items;
    }

    public List<TheatreService> getItem() {
        String sql;
        if (selectText.isEmpty()) {
            sql = "select c from TheatreService c where c.retired=false order by c.category.name,c.name";
        } else {
            sql = "select c from TheatreService c where c.retired=false and (c.name) like '%" + selectText.toUpperCase() + "%' order by c.category.name,c.name";
        }
        //////// // System.out.println(sql);
        items = getTheatreServiceFacade().findByJpql(sql);

        if (items == null) {
            items = new ArrayList<TheatreService>();
        }
        return items;
    }

    public List<TheatreService> getServiceDep() {
        if (items == null) {
            String sql;
            sql = "select c from TheatreService c where c.retired=false order by c.category.name,c.name";

            //////// // System.out.println(sql);
            items = getTheatreServiceFacade().findByJpql(sql);

            for (TheatreService i : items) {

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
                items = new ArrayList<TheatreService>();
            }

        }
        return items;
    }

    private List<ItemFee> getFees(Item i) {
        String sql = "Select f From ItemFee f where f.retired=false and f.item.id=" + i.getId();

        return getItemFeeFacade().findByJpql(sql);
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

    public InwardChargeType[] getInwardChargeTypes() {
        return InwardChargeType.values();
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

    public TheatreServiceFacade getTheatreServiceFacade() {
        return theatreServiceFacade;
    }

    public void setTheatreServiceFacade(TheatreServiceFacade theatreServiceFacade) {
        this.theatreServiceFacade = theatreServiceFacade;
    }

    public TheatreService getCurrent() {
        if (current==null) {
            current=new TheatreService();
        }
        return current;
    }

    public void setItems(List<TheatreService> items) {
        this.items = items;
    }

    public List<TheatreService> getFilterItem() {
        return filterItem;
    }

    public void setFilterItem(List<TheatreService> filterItem) {
        this.filterItem = filterItem;
    }

    public List<TheatreService> getItemsToRemove() {
        return itemsToRemove;
    }

    public void setItemsToRemove(List<TheatreService> itemsToRemove) {
        this.itemsToRemove = itemsToRemove;
    }



    /**
     *
     */
    @FacesConverter(forClass = TheatreService.class)
    public static class ServiceControllerConverter implements Converter {

        public ServiceControllerConverter() {
        }

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ServiceController controller = (ServiceController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "theatreServiceController");
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
            if (object instanceof TheatreService) {
                TheatreService o = (TheatreService) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + TheatreServiceController.class.getName());
            }
        }
    }
}
