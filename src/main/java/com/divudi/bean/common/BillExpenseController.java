/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.BillExpense;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author pdhs
 */
@Named(value = "billExpenseController")
@SessionScoped
public class BillExpenseController implements Serializable {

    /**
     * Creates a new instance of BillExpenseController
     */
    public BillExpenseController() {
    }

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private BillExpenseFacade ejbFacade;
    List<BillExpense> selectedItems;
    private BillExpense current;
    private List<BillExpense> items = null;
    private List<BillExpense> filterItem;
    String selectText = "";
    String bulkText = "";
    boolean billedAs;
    boolean reportedAs;
    List<BillExpense> itemsToRemove;

    public List<BillExpense> getItemsToRemove() {
        return itemsToRemove;
    }

//    public List<BillExpense> completeBillExpense(String query) {
//        List<BillExpense> suggestions;
//        String sql;
//        if (query == null) {
//            suggestions = new ArrayList<>();
//        } else {
//            sql = "select c from BillExpense c where c.retired=false and (c.name) like '%" + query.toUpperCase() + "%' order by c.name";
//            //////// // System.out.println(sql);
//            suggestions = getFacade().findByJpql(sql);
//        }
//        return suggestions;
//    }

    public List<BillExpense> getSelectedItems() {
        if (selectText.trim().equals("")) {
            selectedItems = getFacade().findByJpql("select c from BillExpense c where c.retired=false order by c.name");
        } else {
            selectedItems = getFacade().findByJpql("select c from BillExpense c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        }
        return selectedItems;
    }

//    public List<BillExpense> completeItem(String qry) {
//        List<BillExpense> completeItems = getFacade().findByJpql("select c from Item c "
//                + " where ( type(c) = BillExpense or type(c) = Packege ) "
//                + " and c.retired=false and (c.name) like '%" + qry.toUpperCase() + "%'"
//                + "  order by c.name");
//        return completeItems;
//    }

    public void prepareAdd() {
        current = new BillExpense();
    }

    public String getSelectText() {
        return selectText;
    }

    public void recreateModel() {
        items = null;
        filterItem = null;
    }

    public void saveSelected() {
        
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        recreateModel();
        this.selectText = selectText;
    }

    public BillExpenseFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(BillExpenseFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillExpense getCurrent() {
        if (current == null) {
            current = new BillExpense();
        }
        return current;
    }

    public void setCurrent(BillExpense current) {
        this.current = current;
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
        current=null;
        getCurrent();

    }

    private BillExpenseFacade getFacade() {
        return ejbFacade;
    }

    public List<BillExpense> getItems() {
        String sql = "select c from BillExpense c where c.retired=false order by c.category.name,c.department.name";
        items = getFacade().findByJpql(sql);
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public List<BillExpense> getFilterItem() {
        return filterItem;
    }

    public void setFilterItem(List<BillExpense> filterItem) {
        this.filterItem = filterItem;
    }

    /**
     *
     */
    @FacesConverter(forClass = BillExpense.class)
    public static class BillExpenseControllerConverter implements Converter {

        public BillExpenseControllerConverter() {
        }

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            BillExpenseController controller = (BillExpenseController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "billExpenseController");
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
            if (object instanceof BillExpense) {
                BillExpense o = (BillExpense) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + BillExpenseController.class.getName());
            }
        }
    }


}
