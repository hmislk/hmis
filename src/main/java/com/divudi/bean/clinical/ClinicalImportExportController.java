/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.clinical;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.dataStructure.YearMonthDay;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Patient;
import com.divudi.entity.Person;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class ClinicalImportExportController implements Serializable {

    /**
     *
     * EJBs
     *
     */
    @EJB
    PersonFacade personFacade;
    @EJB
    PatientFacade patientFacade;
    @Inject
    SessionController sessionController;
    @EJB
    CommonFunctions commonFunctions;
    /**
     *
     * Values of Excel Columns
     *
     */
            //        Name              0
    //        Age & Address     1
    //        Comments 1        2
    //        Comments 2        3
    //        Comments 3        4
    //        Comments 4        5
    //        Comments 5        6
    //        Comments 6        7
    //        Comments 7        8
    /**
     * Values of Excel Columns
     */
    int nameCol = 0;
    int addresCol = 1;
    int ampCol = 2;
    int codeCol = 3;
    int startRow = 1;
    /**
     * DataModals
     *
     */

    /**
     *
     * Uploading File
     *
     */
    private UploadedFile file;

    /**
     * Creates a new instance of DemographyExcelManager
     */
    public ClinicalImportExportController() {
    }

    public int getNameCol() {
        return nameCol;
    }

    public void setNameCol(int nameCol) {
        this.nameCol = nameCol;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public String importToExcel() {
        ////System.out.println("importing to excel");
        String strName;
        String strAgeAndAddress;
        String strAge;
        String strAddress;
        String strComments;
        int age;
        Date dob = new Date();

        File inputWorkbook;
        Workbook w;
        Cell cell;
        InputStream in;
        UtilityController.addSuccessMessage(file.getFileName());
        try {
            UtilityController.addSuccessMessage(file.getFileName());
            in = file.getInputstream();
            File f;
            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
            FileOutputStream out = new FileOutputStream(f);
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            inputWorkbook = new File(f.getAbsolutePath());

            UtilityController.addSuccessMessage("Excel File Opened");
            w = Workbook.getWorkbook(inputWorkbook);
            Sheet sheet = w.getSheet(0);

            for (int i = startRow; i < sheet.getRows(); i++) {

                Map m = new HashMap();

                //Name
                cell = sheet.getCell(nameCol, i);
                strName = cell.getContents();
                if (strName == null || strName.trim().equals("")) {
                    continue;
                }
                //Age & Address
                cell = sheet.getCell(addresCol, i);
                strAgeAndAddress = cell.getContents();
                //Age & Address
                cell = sheet.getCell(2, i);
                strComments = cell.getContents();

                cell = sheet.getCell(3, i);
                if (cell != null && cell.getContents() != null && !cell.getContents().trim().equals("")) {
                    strComments = strComments + '\n' + cell.getContents();
                }

                if (cell != null && cell.getContents() != null && !cell.getContents().trim().equals("")) {
                    cell = sheet.getCell(4, i);
                    strComments = strComments + '\n' + cell.getContents();

                }

                if (cell != null && cell.getContents() != null && !cell.getContents().trim().equals("")) {
                    cell = sheet.getCell(5, i);
                    strComments = strComments + '\n' + cell.getContents();

                }

                if (cell != null && cell.getContents() != null && !cell.getContents().trim().equals("")) {
                    cell = sheet.getCell(6, i);
                    strComments = strComments + '\n' + cell.getContents();

                }

                if (cell != null && cell.getContents() != null && !cell.getContents().trim().equals("")) {
                    cell = sheet.getCell(7, i);
                    strComments = strComments + '\n' + cell.getContents();

                }

                if (cell != null && cell.getContents() != null && !cell.getContents().trim().equals("")) {
                    cell = sheet.getCell(8, i);
                    strComments = strComments + '\n' + cell.getContents();
                }

                if (cell != null && cell.getContents() != null && !cell.getContents().trim().equals("")) {
                    cell = sheet.getCell(9, i);
                    strComments = strComments + '\n' + cell.getContents();
                }

                String[] addLines = strAgeAndAddress.split(",");

                if (addLines.length > 0) {
                    strAge = addLines[0];
                } else {
                    continue;
                }
                strAddress = "";
                for (int n = 1; n < addLines.length; n++) {
                    strAddress = strAddress + addLines[n] + "/n";
                }

                Pattern pat = Pattern.compile("-?\\d+");
                Matcher mat = pat.matcher("There are more than -2 and less than 12 numbers here");
                boolean ageDone = false;
                while (mat.find()) {
                    if (ageDone == false) {
                        strAge = mat.group();
                        age = Integer.valueOf(strAge);
                        if (age > 0 && age < 120) {
                            YearMonthDay ymd = new YearMonthDay();
                            ymd.setYear(strAge);
                            dob = getCommonFunctions().guessDob(ymd);
                            ageDone = true;
                        }
                    }
                }
                Person p = new Person();
                p.setName(strName);
                p.setAddress(strAddress);
                p.setDob(dob);
                getPersonFacade().create(p);

                Patient pt = new Patient();
                pt.setPerson(p);
                pt.setComments(strComments);
                getPatientFacade().create(pt);

            }

            UtilityController.addSuccessMessage("Succesful. All the data in Excel File Impoted to the database");
            return "";
        } catch (IOException ex) {
            UtilityController.addErrorMessage(ex.getMessage());
            return "";
        } catch (BiffException e) {
            UtilityController.addErrorMessage(e.getMessage());
            return "";
        }
    }

    public int getStartRow() {
        return startRow;
    }

    public void setStartRow(int startRow) {
        this.startRow = startRow;
    }

    public int getCodeCol() {
        return codeCol;
    }

    public void setCodeCol(int codeCol) {
        this.codeCol = codeCol;
    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public void setPatientFacade(PatientFacade patientFacade) {
        this.patientFacade = patientFacade;
    }

    public int getAddresCol() {
        return addresCol;
    }

    public void setAddresCol(int addresCol) {
        this.addresCol = addresCol;
    }

    public int getAmpCol() {
        return ampCol;
    }

    public void setAmpCol(int ampCol) {
        this.ampCol = ampCol;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

}
