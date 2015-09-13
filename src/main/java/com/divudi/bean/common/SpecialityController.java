/*
 * MSc(Biomedical Informatics) Project
 * 
 * Development and Implementation of a Web-based Combined Data Repository of 
 Genealogical, Clinical, Laboratory and Genetic Data 
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.common;

import com.divudi.entity.DoctorSpeciality;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.facade.SpecialityFacade;
import com.divudi.facade.StaffFacade;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.DualListModel;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class SpecialityController implements Serializable {

    @Inject
    SessionController sessionController;
    @EJB
    private SpecialityFacade ejbFacade;
    @EJB
    StaffFacade staffFacade;
    List<Speciality> selectedItems;
    private Speciality current;
    private List<Speciality> items = null;
    String selectText = "";

    DualListModel<Speciality> specialities;

    public String convertSpecialities() {
        List<Speciality> speNoDoc = specialities.getSource() ;
        List<Speciality> speDoc = specialities.getTarget();
        Map m;
        String jpql = "";

        for (Speciality s : speDoc) {
            if (!(s instanceof DoctorSpeciality)) {
                DoctorSpeciality ds = new DoctorSpeciality();
                ds.setCode(s.getCode());
                ds.setCreatedAt(new Date());
                ds.setCreater(getSessionController().getLoggedUser());
                ds.setDblValue(s.getDblValue());
                ds.setDescription(s.getDescription());
                ds.setName(s.getName());
                ds.setOrderNo(s.getOrderNo());
                ds.setParentCategory(s.getParentCategory());
                getFacade().create(ds);
                
                s.setRetired(true);
                s.setRetireComments("As a conversion");
                s.setRetiredAt(new Date());
                s.setRetirer(getSessionController().getLoggedUser());
                getFacade().edit(s);
                
                jpql = "select s from Staff s where s.speciality=:sp";
                m = new HashMap();
                m.put("sp", s);
                List<Staff> ss = getStaffFacade().findBySQL(jpql, m);
                for (Staff st : ss) {
                    st.setSpeciality(ds);
                    getStaffFacade().edit(st);
                }
            }
        }

        for (Speciality s : speNoDoc) {
            if ((s instanceof DoctorSpeciality)) {
                Speciality ds = new Speciality();
                ds.setCode(s.getCode());
                ds.setCreatedAt(new Date());
                ds.setCreater(getSessionController().getLoggedUser());
                ds.setDblValue(s.getDblValue());
                ds.setDescription(s.getDescription());
                ds.setName(s.getName());
                ds.setOrderNo(s.getOrderNo());
                ds.setParentCategory(s.getParentCategory());
                getFacade().create(ds);
                
                s.setRetired(true);
                s.setRetireComments("As a conversion");
                s.setRetiredAt(new Date());
                s.setRetirer(getSessionController().getLoggedUser());
                getFacade().edit(s);
                
                jpql = "select s from Staff s where s.speciality=:sp";
                m = new HashMap();
                m.put("sp", s);
                List<Staff> ss = getStaffFacade().findBySQL(jpql, m);
                for (Staff st : ss) {
                    st.setSpeciality(ds);
                    getStaffFacade().edit(st);
                }
            }
        }

        return toConvertSpecialities();
    }

    public String toConvertSpecialities() {
        List<Speciality> speNoDoc;
        List<Speciality> speDoc;
        String sql;
        Map m = new HashMap();
        m.put("sc", DoctorSpeciality.class);
        sql = "select s from Speciality s where s.retired=false and type(s) <>:sc order by s.name";
        speNoDoc = getFacade().findBySQL(sql, m);

        sql = "select s from Speciality s where s.retired=false and  type(s) =:sc order by s.name";
        speDoc = getFacade().findBySQL(sql, m);

        specialities = new DualListModel<>(speNoDoc, speDoc);

        return "admin_staff_convert_specialities";
    }

    public List<Speciality> completeSpeciality(String qry) {
        selectedItems = getFacade().findBySQL("select c from Speciality c where c.retired=false and upper(c.name) like '%" + qry.toUpperCase() + "%' order by c.name");
        return selectedItems;
    }
    
    public List<Speciality> completeDoctorSpeciality(String qry) {
        Map m=new HashMap();
        m.put("class", DoctorSpeciality.class);
        selectedItems = getFacade().findBySQL("select c from Speciality c where c.retired=false and type(c)=:class and upper(c.name) like '%" + qry.toUpperCase() + "%' order by c.name",m);
        return selectedItems;
    }

    public List<Speciality> getSelectedItems() {
        if (selectText.trim().equals("")) {
            selectedItems = getFacade().findBySQL("select c from Speciality c where c.retired=false order by c.name");
        } else {
            selectedItems = getFacade().findBySQL("select c from Speciality c where c.retired=false and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        }

        return selectedItems;
    }

    public void prepareAdd() {
        current = new Speciality();
    }

    public void setSelectedItems(List<Speciality> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {

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
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public SpecialityFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(SpecialityFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public SpecialityController() {
    }

    public Speciality getCurrent() {
        if (current == null) {
            current = new Speciality();
        }
        return current;
    }

    public void setCurrent(Speciality current) {
        this.current = current;
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

    private SpecialityFacade getFacade() {
        return ejbFacade;
    }

    public List<Speciality> getItems() {
        if (items == null) {
            String temSql;
            temSql = "SELECT i FROM Speciality i where i.retired=false order by i.name";
            ////System.out.println("Sql for SpacilityController.getItems is " + temSql);
            items = getFacade().findBySQL(temSql);
        }
        return items;
    }

    public DualListModel<Speciality> getSpecialities() {
        return specialities;
    }

    public void setSpecialities(DualListModel<Speciality> specialities) {
        this.specialities = specialities;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    /**
     *
     */
    @FacesConverter(forClass = Speciality.class)
    public static class SpecialityControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SpecialityController controller = (SpecialityController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "specialityController");
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
            if (object instanceof Speciality) {
                Speciality o = (Speciality) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + SpecialityController.class.getName());
            }
        }
    }

    @FacesConverter("specilityCon")
    public static class SpecialityConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SpecialityController controller = (SpecialityController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "specialityController");
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
            if (object instanceof Speciality) {
                Speciality o = (Speciality) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + SpecialityController.class.getName());
            }
        }
    }

}
