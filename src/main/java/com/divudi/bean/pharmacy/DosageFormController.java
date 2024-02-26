/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;

import com.divudi.entity.clinical.ClinicalEntity;

import com.divudi.facade.DosageFormFacade;
import com.divudi.bean.common.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
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

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class DosageFormController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private DosageFormFacade ejbFacade;
    private DosageForm current;
    private List<DosageForm> items = null;
    List<DosageForm> dosageFormList = null;

    public void downloadAsExcel() {
        getItems();
        try {
            // Create a new Excel workbook
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Dosage Forms");

            // Create a header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("No");
            headerRow.createCell(1).setCellValue("Name");
            headerRow.createCell(1).setCellValue("Description");
            // Add more columns as needed

            // Populate the data rows
            int rowNum = 1;
            for (DosageForm diag : items) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rowNum);
                row.createCell(1).setCellValue(diag.getName());
                 row.createCell(2).setCellValue(diag.getDescription());
            }

            // Set the response headers to initiate the download
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"dosage_forms.xlsx\"");

            // Write the workbook to the response output stream
            workbook.write(response.getOutputStream());
            workbook.close();
            context.responseComplete();
        } catch (Exception e) {
            // Handle any exceptions
            e.printStackTrace();
        }
    }

    public List<DosageForm> completeCategory(String qry) {

        Map m = new HashMap();
        m.put("n", "%" + qry + "%");
        String sql = "select c from DosageForm c where "
                + " c.retired=false and (((c.name) like :n) or ((c.description) like :n)) order by c.name";

        dosageFormList = getFacade().findByJpql(sql, m, 20);
        //////// // System.out.println("a size is " + a.size());

        if (dosageFormList == null) {
            dosageFormList = new ArrayList<>();
        }
        return dosageFormList;
    }

    public void prepareAdd() {
        current = new DosageForm();
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {
        if (errorCheck()) {
            return;
        }

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

    private boolean errorCheck() {
        if (getCurrent() != null) {
            if (getCurrent().getDescription() == null || getCurrent().getDescription().isEmpty()) {
                return false;
            } else {
                String sql;
                Map m = new HashMap();

                sql = " select c from DosageForm c where "
                        + " c.retired=false "
                        + " and c.description=:dis ";

                m.put("dis", getCurrent().getDescription());
                List<DosageForm> list = getFacade().findByJpql(sql, m);
                if (list.size() > 0) {
                    JsfUtil.addErrorMessage("Category Code " + getCurrent().getDescription() + " is alredy exsist.");
                    return true;
                } else {
                    return false;
                }
            }
        }

        return false;
    }

    public DosageFormFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(DosageFormFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public DosageFormController() {
    }

    public DosageForm getCurrent() {
        if (current == null) {
            current = new DosageForm();
        }
        return current;
    }

    public void setCurrent(DosageForm current) {
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

    private DosageFormFacade getFacade() {
        return ejbFacade;
    }

    public List<DosageForm> getItems() {
//        items = getFacade().findAll("name", true);
        String sql = " select c from DosageForm c where "
                + " c.retired=false "
                + " order by c.name ";

        items = getFacade().findByJpql(sql);
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = DosageForm.class)
    public static class DosageFormControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            DosageFormController controller = (DosageFormController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "dosageFormController");
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
            if (object instanceof DosageForm) {
                DosageForm o = (DosageForm) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + DosageFormController.class.getName());
            }
        }
    }
}
