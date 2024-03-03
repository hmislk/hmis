/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Logins;
import com.divudi.entity.clinical.ClinicalEntity;
import com.divudi.facade.LoginsFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author Buddhika
 */
@Named(value = "loginController")
@SessionScoped
public class LoginController implements Serializable {

    Department department;
    Institution institution;
    Date fromDate;
    Date toDate;
    Logins longin;
    List<Logins> logins;
    @EJB
    LoginsFacade facade;
    @Inject
    SessionController sessionController;

    /**
     * Creates a new instance of LoginController
     */
    public LoginController() {
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay();
        }
        return fromDate;
    }

    private LoginsFacade getFacade() {
        return facade;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Logins getLongin() {
        return longin;
    }

    public void setLongin(Logins longin) {
        this.longin = longin;
    }

    public String navigateToViewLogins() {
        fillLogins();
        return "/admin/users/user_logins";
    }

    public void fillLogins() {
        String sql;
        Map m = new HashMap();
        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());
        sql = "select l from Logins l where l.logedAt between :fromDate and :toDate or l.logoutAt between :fromDate and :toDate  order by l.logedAt, l.logoutAt";
        logins = getFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public void downloadAsExcel() {
        getLogins();
        SimpleDateFormat dateFormat = new SimpleDateFormat(sessionController.getApplicationPreference().getLongDateTimeFormat());
        try {
            // Create a new Excel workbook
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Login");

            // Create a header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("No");
            headerRow.createCell(1).setCellValue("Institution");
            headerRow.createCell(2).setCellValue("Department");
            headerRow.createCell(3).setCellValue("Code");
            headerRow.createCell(4).setCellValue("User Name");
            headerRow.createCell(5).setCellValue("Login At");
            headerRow.createCell(6).setCellValue("Logout At");
            headerRow.createCell(7).setCellValue("IP Address");
            // Add more columns as needed

            // Populate the data rows
            int rowNum = 1;
            for (Logins login : logins) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rowNum);
                row.createCell(1).setCellValue(login.getInstitution().getName());
                row.createCell(2).setCellValue(login.getDepartment().getName());

                if (login.getWebUser().getCode() != null) {
                    row.createCell(3).setCellValue(login.getWebUser().getCode());
                }

                if (login.getWebUser().getWebUserPerson().getName() != null) {
                    row.createCell(3).setCellValue(login.getWebUser().getCode());
                }

                row.createCell(4).setCellValue(login.getWebUser().getWebUserPerson().getName());
                String formattedLogedAt = (login.getLogedAt() != null) ? dateFormat.format(login.getLogedAt()) : "";
                String formattedLogoutAt = (login.getLogoutAt() != null) ? dateFormat.format(login.getLogoutAt()) : "";

                row.createCell(5).setCellValue(formattedLogedAt);
                row.createCell(6).setCellValue(formattedLogoutAt);
                row.createCell(7).setCellValue(login.getIpaddress());
            }

            // Set the response headers to initiate the download
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"login_report.xlsx\"");

            // Write the workbook to the response output stream
            workbook.write(response.getOutputStream());
            workbook.close();
            context.responseComplete();
        } catch (Exception e) {
            // Handle any exceptions
            e.printStackTrace();
        }
    }

    public List<Logins> getLogins() {
        return logins;
    }

    public void setLogins(List<Logins> logins) {
        this.logins = logins;
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
}
