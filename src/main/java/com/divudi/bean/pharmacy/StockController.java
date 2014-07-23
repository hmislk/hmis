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
import com.divudi.data.BillType;
import com.divudi.data.dataStructure.DepartmentSale;
import com.divudi.data.dataStructure.DepartmentStock;
import com.divudi.entity.Department;
import com.divudi.entity.Item;
import com.divudi.facade.StockFacade;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.DepartmentFacade;
import static com.lowagie.text.SpecialSymbol.get;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class StockController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private StockFacade ejbFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    List<Stock> selectedItems;
    private Stock current;
    private List<Stock> items = null;
    String selectText = "";

    public List<Stock> getSelectedItems() {
        selectedItems = getFacade().findBySQL("select c from Stock c where c.retired=false and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

  
 
   
   
    public List<Stock> completeStock(String qry) {
        List<Stock> a = null;
        if (qry != null) {
            a = getFacade().findBySQL("select c from Stock c where c.retired=false and upper(c.name) like '%" + qry.toUpperCase() + "%' order by c.name");
        }
        if (a == null) {
            a = new ArrayList<>();
        }
        return a;
    }

    public void prepareAdd() {
        current = new Stock();
    }

    public void setSelectedItems(List<Stock> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    private boolean errorCheck() {
        if (getCurrent().getDepartment() == null) {
            UtilityController.addErrorMessage("Please Select Manufacturer");
            return true;
        }
        return false;
    }

    public void saveSelected() {
        if (errorCheck()) {
            return;
        }

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            UtilityController.addSuccessMessage("savedOldSuccessfully");
        } else {
            getFacade().create(current);
            UtilityController.addSuccessMessage("savedNewSuccessfully");
        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public StockFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(StockFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public StockController() {
    }

    public Stock getCurrent() {
        if (current == null) {
            current = new Stock();
        }
        return current;
    }

    public void setCurrent(Stock current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            getFacade().remove(current);
            UtilityController.addSuccessMessage("DeleteSuccessfull");
        } else {
            UtilityController.addSuccessMessage("NothingToDelete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private StockFacade getFacade() {
        return ejbFacade;
    }

    public List<Stock> getItems() {
        items = getFacade().findAll("name", true);
        return items;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

   
    /**
     *
     */
    @FacesConverter(forClass = Stock.class)
    public static class StockConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            StockController controller = (StockController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "stockController");
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
            if (object instanceof Stock) {
                Stock o = (Stock) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + StockController.class.getName());
            }
        }
    }

    /**
     *
     */
    @FacesConverter("stockCon")
    public static class StockControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            StockController controller = (StockController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "stockController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            try {
                key = Long.valueOf(value);
            } catch (NumberFormatException e) {
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
            if (object instanceof Stock) {
                Stock o = (Stock) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + StockController.class.getName());
            }
        }
    }
}
