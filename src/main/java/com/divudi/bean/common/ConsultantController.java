/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.Consultant;
import com.divudi.entity.Doctor;
import com.divudi.entity.Person;
import com.divudi.entity.Speciality;
import com.divudi.entity.Vocabulary;
import com.divudi.facade.ConsultantFacade;
import com.divudi.facade.PersonFacade;
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
public class ConsultantController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private ConsultantFacade ejbFacade;
    @EJB
    private PersonFacade personFacade;
    List<Consultant> selectedItems;

    private Consultant current;
    private List<Consultant> items = null;
    String selectText = "";
    private Speciality speciality;

    public List<Consultant> getSelectedItems() {
        String sql;
        HashMap hm = new HashMap();
        if (selectText.trim().equals("")) {
            sql = "select c from Consultant c "
                    + " where c.retired=false ";

            if (speciality != null) {
                sql += " and c.speciality=:s ";
                hm.put("s", speciality);
            }
            sql += " order by c.codeInterger, c.person.name ";

        } else {
            sql = "select c from Consultant c "
                    + " where c.retired=false"
                    + " and (c.person.name) like :q ";

            sql += " and c.speciality=:s ";
            sql += " order by c.codeInterger , c.person.name ";
            hm.put("s", speciality);

            hm.put("q", "%" + getSelectText().toUpperCase() + "%");
        }
        selectedItems = getFacade().findByJpql(sql, hm);

        return selectedItems;
    }

    public void prepareAdd() {
        current = new Consultant();
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
        //  getItems();
        current = null;
        getCurrent();
    }

    public void downloadAsExcel() {
        getItems();
        try {
            // Create a new Excel workbook
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Consultent Data");

            // Create a header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(1).setCellValue("Name");
            headerRow.createCell(2).setCellValue("Code");
            headerRow.createCell(3).setCellValue("Consultant Serial No");
            headerRow.createCell(4).setCellValue("Phone");
            headerRow.createCell(5).setCellValue("Fax");
            headerRow.createCell(6).setCellValue("Mobile");
            headerRow.createCell(7).setCellValue("Address");
            headerRow.createCell(8).setCellValue("Speciality");
            headerRow.createCell(9).setCellValue("Registration");
            headerRow.createCell(10).setCellValue("Qualification");

            int rowNum = 1;
            for (Consultant consultant : items) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(1).setCellValue(consultant.getName());
                row.createCell(2).setCellValue(consultant.getCode());
                row.createCell(3).setCellValue(consultant.getPerson().getSerealNumber());
                row.createCell(4).setCellValue(consultant.getPerson().getPhone());
                row.createCell(5).setCellValue(consultant.getPerson().getFax());
                row.createCell(6).setCellValue(consultant.getPerson().getMobile());
                row.createCell(7).setCellValue(consultant.getPerson().getAddress());
                row.createCell(8).setCellValue(consultant.getSpeciality().getName());
                row.createCell(9).setCellValue(consultant.getRegistration());
                row.createCell(10).setCellValue(consultant.getQualification());
            }

            // Set the response headers to initiate the download
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"consultant_data.xlsx\"");

            // Write the workbook to the response output stream
            workbook.write(response.getOutputStream());
            workbook.close();
            context.responseComplete();
        } catch (Exception e) {
            // Handle any exceptions
            e.printStackTrace();
        }
    }

    public void createConsultantTable() {
        String sql;
        Map m = new HashMap();

        sql = "select c from Consultant c "
                + " where c.retired=false ";

        if (speciality != null) {
            sql += " and c.speciality=:s ";
            m.put("s", speciality);
        }

        sql += " order by c.codeInterger , c.person.name ";

        items = getFacade().findByJpql(sql, m);

    }

    public void setSelectedItems(List<Consultant> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    public void recreateModel() {
        items = null;
    }

    public void saveSelected() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return;
        }
        if (current.getPerson() == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return;
        }
        if (current.getPerson().getName().trim().equals("")) {
            JsfUtil.addErrorMessage("Please Enter a Name");
            return;
        }
        if (current.getSpeciality() == null) {
            JsfUtil.addErrorMessage("Please Select Speciality.");
            return;
        }
        if (current.getPerson().getId() == null || current.getPerson().getId() == 0) {
            getPersonFacade().create(current.getPerson());
        } else {
            getPersonFacade().edit(current.getPerson());
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
        // getItems();
    }

    public void save(Consultant con) {
        if (con == null) {
            return;
        }
        if (con.getPerson() == null) {
            return;
        }
        if (con.getPerson().getName().trim().equals("")) {
            return;
        }
        if (con.getSpeciality() == null) {
            return;
        }
        if (con.getPerson().getId() == null || con.getPerson().getId() == 0) {
            getPersonFacade().create(con.getPerson());
        } else {
            getPersonFacade().edit(con.getPerson());
        }
        if (con.getId() != null && con.getId() > 0) {
            getFacade().edit(con);
        } else {
            con.setCreatedAt(new Date());
            con.setCreater(getSessionController().getLoggedUser());
            getFacade().create(con);
        }
    }

    
    public List<Doctor> completeConsultant(String query) {
        List<Doctor> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = " select p from Consultant p "
                    + " where p.retired=false "
                    + " and ((p.person.name) like :q or (p.code) like :q) "
                    + " order by p.person.name";
            HashMap hm = new HashMap();
            hm.put("q", "%" + query.toUpperCase() + "%");
            suggestions = getFacade().findByJpql(sql, hm);
        }
        return suggestions;
    }
    
    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public ConsultantFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(ConsultantFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public ConsultantController() {
    }

    public Consultant getConsultantByName(String name) {
        String jpql = "select c "
                + " from Consultant c "
                + " where c.retired=:ret "
                + " and c.person.name=:name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("name", name);
        return getFacade().findFirstByJpql(jpql, m);
    }

    public Consultant getCurrent() {
        if (current == null) {
            Person p = new Person();
            current = new Consultant();
            current.setPerson(p);
        }
        return current;
    }

    public void setCurrent(Consultant current) {
        this.current = current;
    }

    private ConsultantFacade getFacade() {
        return ejbFacade;
    }

    public List<Consultant> getItems() {
        if (items == null) {
            String temSql;
            temSql = "SELECT i FROM Consultant i where i.retired=false ";
            items = getFacade().findByJpql(temSql);
        }
        return items;
    }
    
    @Deprecated
    public List<Consultant> completeConsultants() {
        if (items == null) {
            String temSql;
            temSql = "SELECT i FROM Consultant i where i.retired=false ";
            items = getFacade().findByJpql(temSql);
        }
        return items;
    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    /**
     *
     */
    @FacesConverter(forClass = Consultant.class)
    public static class ConsultantControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ConsultantController controller = (ConsultantController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "consultantController");
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
            if (object instanceof Consultant) {
                Consultant o = (Consultant) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ConsultantController.class.getName());
            }
        }
    }

}
