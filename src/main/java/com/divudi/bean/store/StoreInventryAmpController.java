package com.divudi.bean.store;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.DepartmentType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.facade.AmpFacade;
import java.io.Serializable;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 Informatics)
 */
@Named
@SessionScoped
public class StoreInventryAmpController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private AmpFacade ejbFacade;
    private Amp current;
    private List<Amp> items = null;
    List<Amp> itemsByCode = null;

    public List<Amp> getItemsByCode() {
        if (itemsByCode == null) {
            itemsByCode = getFacade().findByJpql("select a from Amp a where a.retired=false order by a.code");
        }
        return itemsByCode;
    }

    public void setItemsByCode(List<Amp> itemsByCode) {
        this.itemsByCode = itemsByCode;
    }

    @EJB
    BillNumberGenerator billNumberBean;

    public void prepareAdd() {
        current = null;
    }

    private void recreateModel() {
        items = null;
        current = null;
    }

    public void saveSelected() {

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        // getItems();
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

    public StoreInventryAmpController() {
    }

    public Amp getCurrent() {
        if (current == null) {
            current = new Amp();
            current.setDepartmentType(DepartmentType.Inventry);
            current.setCode(billNumberBean.storeInventryItemNumberGenerator());
        }
        return current;
    }

    public void setCurrent(Amp current) {
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
        current = null;
        getCurrent();
    }

    private AmpFacade getFacade() {
        return ejbFacade;
    }
    private List<Amp> filteredItems;

    public List<Amp> getItems() {
        if (items == null) {
            String j;
            j="select a "
                    + " from Amp a "
                    + " where a.retired=false "
                    + " order by a.name";
            items = getFacade().findByJpql(j);
        }
        return items;
    }

    public List<Amp> getFilteredItems() {
        return filteredItems;
    }

    public void setFilteredItems(List<Amp> filteredItems) {
        this.filteredItems = filteredItems;
    }

    /**
     *
     */
    @FacesConverter("stoAmpCon")
    public static class AmpControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            StoreInventryAmpController controller = (StoreInventryAmpController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "storeAmpController");
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
            if (object instanceof Amp) {
                Amp o = (Amp) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + StoreInventryAmpController.class.getName());
            }
        }
    }
}
