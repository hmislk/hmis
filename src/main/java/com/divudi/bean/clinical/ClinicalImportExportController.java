/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.clinical;

import com.divudi.bean.common.SessionController;
import com.divudi.ejb.CommonFunctions;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.file.UploadedFile;
//import org.primefaces.model.file.UploadedFile;

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
