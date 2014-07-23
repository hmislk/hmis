package com.divudi.bean.pharmacy;

import com.divudi.bean.common.UtilityController;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Person;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.entity.pharmacy.PharmaceuticalItem;
import com.divudi.entity.pharmacy.Reorder;
import com.divudi.facade.ReorderFacade;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import org.primefaces.event.RowEditEvent;

@Named
@SessionScoped
public class ReorderController implements Serializable {

    private Reorder current;
    private List<Reorder> items = null;
    @EJB
    ReorderFacade ejbFacade;

    Department department;
    Institution institution;
    Person person;

    @Inject
    AmpController ampController;

    List<Reorder> departmentReorders;

    public void onEdit(RowEditEvent event) {

        Reorder tmp = (Reorder) event.getObject();
        getEjbFacade().edit(tmp);
        UtilityController.addSuccessMessage("Reorder Level Updted");
    }

    public void fillDepartmentReorders() {
        if (department == null) {
            return;
        }
        items = new ArrayList<>();
        List<Amp> amps = getAmpController().getItems();

        for (Amp a : amps) {
            Reorder r;
            Map m = new HashMap();
            m.put("d", department);
            m.put("i", a);
            String sql = "Select r from Reorder r where r.item=:i and r.department=:d";
            r = getEjbFacade().findFirstBySQL(sql, m);
            if (r == null) {
                r = new Reorder();
                r.setDepartment(department);
                r.setItem(a);
                getEjbFacade().create(r);

            }
            items.add(r);
        }
    }

    public AmpController getAmpController() {
        return ampController;
    }

    public void setAmpController(AmpController ampController) {
        this.ampController = ampController;
    }

    public List<Reorder> getDepartmentReorders() {
        return departmentReorders;
    }

    public void setDepartmentReorders(List<Reorder> departmentReorders) {
        this.departmentReorders = departmentReorders;
    }

    public Reorder getCurrent() {
        return current;
    }

    public void setCurrent(Reorder current) {
        this.current = current;
    }

    public List<Reorder> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public void setItems(List<Reorder> items) {
        this.items = items;
    }

    public ReorderFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(ReorderFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public ReorderController() {
    }

    public Reorder getReorder(java.lang.Long id) {
        return ejbFacade.find(id);
    }

    @FacesConverter(forClass = Reorder.class)
    public static class ReorderControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ReorderController controller = (ReorderController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "reorderController");
            return controller.getReorder(getKey(value));
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
            if (object instanceof Reorder) {
                Reorder o = (Reorder) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + Reorder.class.getName());
            }
        }

    }

}
