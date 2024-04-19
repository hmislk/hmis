/*
 * Open Hospital Management Information System
 * 
 * Dr M H B Ariyaratne 
 * Acting Consultant (Health Informatics) 
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.entity.DoctorSpeciality;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.facade.SpecialityFacade;
import com.divudi.facade.StaffFacade;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.model.DualListModel;
import com.divudi.bean.common.util.JsfUtil;
/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
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
        List<Speciality> speNoDoc = specialities.getSource();
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
                List<Staff> ss = getStaffFacade().findByJpql(jpql, m);
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
                List<Staff> ss = getStaffFacade().findByJpql(jpql, m);
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
        speNoDoc = getFacade().findByJpql(sql, m);

        sql = "select s from Speciality s where s.retired=false and  type(s) =:sc order by s.name";
        speDoc = getFacade().findByJpql(sql, m);

        specialities = new DualListModel<>(speNoDoc, speDoc);

        return "admin_staff_convert_specialities";
    }

    public List<Speciality> completeSpeciality(String qry) {
        selectedItems = getFacade().findByJpql("select c from Speciality c where c.retired=false and (c.name) like '%" + qry.toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public List<Speciality> completeDoctorSpeciality(String qry) {
        Map m = new HashMap();
        m.put("class", DoctorSpeciality.class);
        selectedItems = getFacade().findByJpql("select c from Speciality c where c.retired=false and type(c)=:class and (c.name) like '%" + qry.toUpperCase() + "%' order by c.name", m);
        return selectedItems;
    }

    public Speciality findSpeciality(String name, boolean createNewIfNotExists) {
        String j;
        j = "select s "
                + " from Speciality s "
                + " where s.retired=:ret "
                + " and s.name=:name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("name", name);
        Speciality ds = getFacade().findFirstByJpql(j, m);
        if (ds == null && createNewIfNotExists) {
            ds = new Speciality();
            ds.setName(name);
            ds.setCreatedAt(new Date());
            ds.setCreater(sessionController.getLoggedUser());
            getFacade().create(ds);
        }
        return ds;
    }

    public List<Speciality> getSelectedItems() {
        if (selectText.trim().equals("")) {
            selectedItems = getFacade().findByJpql("select c from Speciality c where c.retired=false order by c.name");
        } else {
            selectedItems = getFacade().findByJpql("select c from Speciality c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
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

    public void recreateModel() {
        items = null;
    }

    // Method to generate the Excel file and initiate the download
    public void downloadAsExcel() {
        getItems();
        try {
            // Create a new Excel workbook
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("All Speciality Data");

            // Create a header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Speciality Name");
            headerRow.createCell(1).setCellValue("Income Name");
            // Add more columns as needed

            // Populate the data rows
            int rowNum = 1;
            for (Speciality speciality : items) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(speciality.getName());
                row.createCell(1).setCellValue(speciality.getDescription());
            }

            // Set the response headers to initiate the download
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"all_speciality_data.xlsx\"");

            // Write the workbook to the response output stream
            workbook.write(response.getOutputStream());
            workbook.close();
            context.responseComplete();
        } catch (Exception e) {
            // Handle any exceptions
            e.printStackTrace();
        }
    }

    public void saveSelected() {

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
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

    private SpecialityFacade getFacade() {
        return ejbFacade;
    }

    public List<Speciality> getItems() {
        if (items == null) {
            String temSql;
            temSql = "SELECT i FROM Speciality i where i.retired=false order by i.name";
            //////// // System.out.println("Sql for SpacilityController.getItems is " + temSql);
            items = getFacade().findByJpql(temSql);
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
                        + object.getClass().getName() + "; expected type: " + Speciality.class.getName());
            }
        }
    }

}
