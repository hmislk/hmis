package com.divudi.bean.emr;

import com.divudi.bean.clinical.DiagnosisController;
import com.divudi.bean.clinical.PatientEncounterController;
import com.divudi.bean.common.CategoryController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.InstitutionController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.PatientController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.pharmacy.AmpController;
import com.divudi.bean.pharmacy.AtmController;
import com.divudi.bean.pharmacy.MeasurementUnitController;
import com.divudi.bean.pharmacy.VmpController;
import com.divudi.bean.pharmacy.VtmController;
import com.divudi.data.EncounterType;
import com.divudi.data.Sex;
import com.divudi.data.Title;
import com.divudi.entity.Category;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.clinical.ClinicalEntity;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.Atm;
import com.divudi.entity.pharmacy.MeasurementUnit;
import com.divudi.entity.pharmacy.Vmp;
import com.divudi.entity.pharmacy.Vtm;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.VtmFacade;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Inject;
import org.primefaces.model.file.UploadedFile;

@Named
@RequestScoped
public class DataUploadBean {

    @Inject
    ItemController itemController;
    @Inject
    SessionController sessionController;
    @Inject
    VtmController vtmController;
    @Inject
    AtmController atmController;
    @Inject
    AmpController ampController;
    @Inject
    CategoryController categoryController;
    @Inject
    MeasurementUnitController measurementUnitController;
    @Inject
    InstitutionController institutionController;
    @Inject
    VmpController vmpController;
    @Inject
    DiagnosisController diagnosisController;
    @Inject
    PatientController patientController;
    @Inject
    PatientEncounterController patientEncounterController;

    @EJB
    PatientFacade patientFacade;
    @EJB
    PersonFacade personFacade;
    @EJB
    VtmFacade vtmFacade;

    private UploadedFile file;
    private String outputString;

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public String navigateToUploadDiagnoses() {
        return "/emr/admin/upload_diagnoses";
    }

    public String navigateToUploadVisits() {
        return "/emr/admin/upload_visits";
    }

    public String toUploadPatients() {
        return "/emr/admin/upload_patients";
    }

    public String toUploadVtms() {
        return "/pharmacy/admin/upload_vtms";
    }

    public String toUploadAtms() {
        return "/pharmacy/admin/upload_atms";
    }

    public String toUploadAmps() {
        return "/pharmacy/admin/upload_amps";
    }
    
    public String toUploadAmpsMin() {
        return "/pharmacy/admin/upload_amps_minimal";
    }

    public String toUploadVmps() {
        return "/pharmacy/admin/upload_vmps";
    }

    public void uploadPatients() {
        List<Patient> patients;

        if (file != null) {
            try ( InputStream inputStream = file.getInputStream()) {
                patients = readPatientDataFromExcel(inputStream);

                for (Patient p : patients) {
                    personFacade.create(p.getPerson());
                    patientFacade.create(p);
                }
                // Persist patients to the database or perform other operations
                // patientService.save(patients);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void uploadVisits() {
        if (file != null) {
            try ( InputStream inputStream = file.getInputStream()) {
                readVisitDataFromExcel(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void uploadVtms() {
        List<Vtm> vtms;
        if (file != null) {
            try ( InputStream inputStream = file.getInputStream()) {
                vtms = readVtmsFromExcel(inputStream);
                for (Vtm v : vtms) {
                    vtmController.findAndSaveVtmByNameAndCode(v);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void uploadAtms() {
        List<Atm> atms;
        if (file != null) {
            try ( InputStream inputStream = file.getInputStream()) {
                atms = readAtmsFromExcel(inputStream);
                for (Atm v : atms) {
                    atmController.findAndSaveAtmByNameAndCode(v, v.getVtm());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void uploadAmps() {
        List<Amp> amps;
        if (file != null) {
            try ( InputStream inputStream = file.getInputStream()) {
                readAmpsFromExcel(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void uploadDiagnoses() {
        if (file != null) {
            try {
                InputStream inputStream = file.getInputStream();
                readDiagnosesFromExcel(inputStream);
            } catch (IOException ex) {
                Logger.getLogger(DataUploadBean.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void uploadVmps() {
        outputString += "uploadVmps\n";
        List<Vmp> vmps;
        if (file != null) {
            try ( InputStream inputStream = file.getInputStream()) {
                vmps = readVmpsFromExcel(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private List<PatientEncounter> readVisitDataFromExcel(InputStream inputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();

        List<PatientEncounter> visits = new ArrayList<>();

        List<String> datePatterns = Arrays.asList("dd MMMM yyyy", "dd/MMM/yyyy hh:mm", "M/d/yy", "MM/dd/yyyy", "d/M/yy", "dd/MM,yyyy", "dd/MM/yyyy", "yyyy-MM-dd");

        Long visitID = 0l;
        Long patientID = 0l;
        Date completedTime = null;
        String Comments = "";
        Double visitWeight = null;
        Long sbp = null;
        Long dbp = null;
        Double bmi = null;
        Long pr = null;

        // Assuming the first row contains headers, skip it
        if (rowIterator.hasNext()) {
            rowIterator.next();
        }

        // Use a DataFormatter to read cell values, as date cells are typically formatted as strings
        DataFormatter dataFormatter = new DataFormatter();

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();

//...
            Cell visitIdCell = row.getCell(0);
            if (visitIdCell != null && visitIdCell.getCellType() == CellType.NUMERIC) {
                visitID = (long) Math.round(visitIdCell.getNumericCellValue());
            }

            Cell patientIdCell = row.getCell(1);
            if (patientIdCell != null && patientIdCell.getCellType() == CellType.NUMERIC) {
                patientID = (long) Math.round(patientIdCell.getNumericCellValue());
            }

            Cell completedTimeCell = row.getCell(2);
            if (completedTimeCell != null) {
                if (completedTimeCell.getCellType() == CellType.STRING) {
                    String dateOfBirthStr = dataFormatter.formatCellValue(completedTimeCell);
                    LocalDate localDateOfBirth = parseDate(dateOfBirthStr, datePatterns);
                    Instant instant = localDateOfBirth.atStartOfDay(ZoneId.systemDefault()).toInstant();
                    completedTime = Date.from(instant);
                } else if (completedTimeCell.getCellType() == CellType.NUMERIC) {
                    if (DateUtil.isCellDateFormatted(completedTimeCell)) {
                        completedTime = completedTimeCell.getDateCellValue();
                    } else {
                    }
                }
            }

            Cell commentsCell = row.getCell(3);
            if (commentsCell != null && commentsCell.getCellType() == CellType.STRING) {
                Comments = commentsCell.getStringCellValue();
            }

            Cell visitWeightCell = row.getCell(4);
            if (visitWeightCell != null && visitWeightCell.getCellType() == CellType.NUMERIC) {
                visitWeight = visitWeightCell.getNumericCellValue();
            }

            Cell sbpCell = row.getCell(5);
            if (sbpCell != null && sbpCell.getCellType() == CellType.NUMERIC) {
                sbp = (long) Math.round(sbpCell.getNumericCellValue());
            }

            Cell dbpCell = row.getCell(6);
            if (dbpCell != null && dbpCell.getCellType() == CellType.NUMERIC) {
                dbp = (long) Math.round(dbpCell.getNumericCellValue());
            }

            Cell bmiCell = row.getCell(7);
            if (bmiCell != null && bmiCell.getCellType() == CellType.NUMERIC) {
                bmi = bmiCell.getNumericCellValue();
            }

            Cell prCell = row.getCell(8);
            if (prCell != null && prCell.getCellType() == CellType.NUMERIC) {
                pr = (long) Math.round(prCell.getNumericCellValue());
            }

            PatientEncounter visit = new PatientEncounter();
            visit.setEncounterType(EncounterType.Opd);
            visit.setEncounterId(visitID);
            Patient patient = patientController.findPatientByPatientId(patientID);
            if (patient == null) {
                continue;
            }
            visit.setPatient(patient);
            visit.setCreatedAt(new Date());
            visit.setEncounterDate(completedTime);
            visit.setEncounterDateTime(completedTime);
            visit.setDepartment(sessionController.getDepartment());
            visit.setInstitution(sessionController.getInstitution());
            visit.setComments(Comments);
            visit.setSbp(sbp);
            visit.setDbp(dbp);
            visit.setBmi(bmi);
            visit.setPr(pr);
            visit.setVisitWeight(visitWeight);
            patientEncounterController.saveEncounter(visit);
            visits.add(visit);
        }
        return visits;
    }

    private List<Patient> readPatientDataFromExcel(InputStream inputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();

        List<Patient> patients = new ArrayList<>();

        List<String> datePatterns = Arrays.asList("dd MMMM yyyy", "M/d/yy", "MM/dd/yyyy", "d/M/yy", "dd/MM,yyyy", "dd/MM/yyyy", "yyyy-MM-dd");

        // Assuming the first row contains headers, skip it
        if (rowIterator.hasNext()) {
            rowIterator.next();
        }

        // Use a DataFormatter to read cell values, as date cells are typically formatted as strings
        DataFormatter dataFormatter = new DataFormatter();

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Patient patient = new Patient();

            patient.setPatientId((long) row.getCell(0).getNumericCellValue());
            patient.getPerson().setName(row.getCell(1).getStringCellValue());
            Cell codeCell = row.getCell(2);
            if (codeCell != null) {
                String code = codeCell.getStringCellValue();
                patient.setCode(code);
            }

            Cell dateOfBirthCell = row.getCell(3);
            if (dateOfBirthCell != null) {
                String dateOfBirthStr = dataFormatter.formatCellValue(dateOfBirthCell);
                LocalDate localDateOfBirth = parseDate(dateOfBirthStr, datePatterns);
                if (localDateOfBirth != null) {
                    Instant instant = localDateOfBirth.atStartOfDay(ZoneId.systemDefault()).toInstant();
                    Date dateOfBirth = Date.from(instant);
                    patient.getPerson().setDob(dateOfBirth);
                }
            }

            Cell addressCell = row.getCell(4);
            if (addressCell != null) {
                patient.getPerson().setAddress(addressCell.getStringCellValue());
            }

            Cell phoneCell = row.getCell(5);
            if (phoneCell != null) {
                patient.getPerson().setPhone(phoneCell.getStringCellValue());
            }

            Cell mobileCell = row.getCell(6);
            if (mobileCell != null) {
                patient.getPerson().setMobile(mobileCell.getStringCellValue());
            }

            Cell emailCell = row.getCell(7);
            if (emailCell != null) {
                patient.getPerson().setEmail(emailCell.getStringCellValue());
            }

            Title title;
            try {
                Cell titleCell = row.getCell(8);
                if (titleCell != null) {
                    title = Title.valueOf(titleCell.getStringCellValue());
                } else {
                    title = Title.Mr;
                }
            } catch (IllegalArgumentException e) {
                title = Title.Mr;
            }
            patient.getPerson().setTitle(title);

            Cell sexCell = row.getCell(9);
            if (sexCell != null) {
                String sex = sexCell.getStringCellValue();
                if (sex.toLowerCase().contains("f")) {
                    patient.getPerson().setSex(Sex.Female);
                } else {
                    patient.getPerson().setSex(Sex.Male);
                }
            }

            Cell civilStatusCell = row.getCell(10);
            if (civilStatusCell != null) {
                String strCivilStatus = civilStatusCell.getStringCellValue();
                Item civilStatus = itemController.findItemByName(strCivilStatus, "civil_statuses");
                patient.getPerson().setCivilStatus(civilStatus);
            }

            Cell raceCell = row.getCell(11);
            if (raceCell != null) {
                String strRace = raceCell.getStringCellValue();
                Item race = itemController.findItemByName(strRace, "races");
                patient.getPerson().setRace(race);
            }

            Cell bloodGroupCell = row.getCell(12);
            if (bloodGroupCell != null) {
                String strBloodGroup = bloodGroupCell.getStringCellValue();
                Item bloodGroup = itemController.findItemByName(strBloodGroup, "blood_groups");
                patient.getPerson().setBloodGroup(bloodGroup);
            }

            Cell commentsCell = row.getCell(13);
            if (commentsCell != null) {
                patient.setComments(commentsCell.getStringCellValue());
            }

            Cell fullNameCell = row.getCell(14);
            if (fullNameCell != null) {
                patient.getPerson().setFullName(fullNameCell.getStringCellValue());
            }

            Cell occupationCell = row.getCell(15);
            if (occupationCell != null) {
                String strOccupation = occupationCell.getStringCellValue();
                Item occupation = itemController.findItemByName(strOccupation, "occupations");
                patient.getPerson().setOccupation(occupation);
            }

            patient.setCreatedAt(new Date());
            patient.setCreater(sessionController.getLoggedUser());
            patient.setCreatedInstitution(sessionController.getInstitution());

            patients.add(patient);

        }

        return patients;
    }

    private List<Vtm> readVtmsFromExcel(InputStream inputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();

        List<Vtm> vtms = new ArrayList<>();

        // Assuming the first row contains headers, skip it
        if (rowIterator.hasNext()) {
            rowIterator.next();
        }

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Vtm vtm = new Vtm();

            Cell idCell = row.getCell(0);
            if (idCell != null) {
                vtm.setItemId((long) idCell.getNumericCellValue());
            }
            Cell nameCell = row.getCell(1);
            if (nameCell != null) {
                vtm.setName(nameCell.getStringCellValue());
                vtm.setCode(CommonController.nameToCode("vtm_" + vtm.getName()));
            }
            vtm.setCreatedAt(new Date());
            vtm.setCreater(sessionController.getLoggedUser());
            vtms.add(vtm);

        }

        return vtms;
    }

    private List<Atm> readAtmsFromExcel(InputStream inputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();
        outputString = "readAtmsFromExcel\n";
        List<Atm> atms = new ArrayList<>();

        // Assuming the first row contains headers, skip it
        if (rowIterator.hasNext()) {
            rowIterator.next();
        }

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Atm atm = new Atm();

            Cell idCell = row.getCell(0);
            if (idCell != null) {
                atm.setItemId((long) idCell.getNumericCellValue());
            }
            Cell nameCell = row.getCell(1);
            if (nameCell != null) {
                atm.setName(nameCell.getStringCellValue());
                atm.setCode(CommonController.nameToCode("atm_" + atm.getName()));
            }
            Cell vtmCell = row.getCell(2);
            if (vtmCell != null) {
                String vtmName = vtmCell.getStringCellValue();
                outputString += vtmName + "\n";
                Vtm vtm = vtmController.findAndSaveVtmByName(vtmName);
                outputString += vtm + "\n";
                atm.setVtm(vtm);
            }
            atm.setCreatedAt(new Date());
            atms.add(atm);
            atmController.saveAtm(atm);
        }

        return atms;
    }

    private List<ClinicalEntity> readDiagnosesFromExcel(InputStream inputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();

        List<ClinicalEntity> dxx = new ArrayList<>();

        // Assuming the first row contains headers, skip it
        if (rowIterator.hasNext()) {
            rowIterator.next();
        }

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            ClinicalEntity dx = new ClinicalEntity();

            /**
             * ItemID tblItem_Item ItemCode tradename_Item generic_Item Category
             * strength unit issue unit pack unit StrengthUnitsPerIssueUnit
             * IssueUnitsPerPack	ROL	ROQ	MinROQ	manu_Institution
             * importer_Institution	supplier_Institution	SalePrice	PurchasePrice
             * LastSalePrice	LastPurchasePrice	MinIQty	MaxIQty	DivQty
             */
            Long id;
            String dxName = null;

            Cell idCell = row.getCell(0);
            if (idCell != null && idCell.getCellType() == CellType.NUMERIC) {
                id = (long) idCell.getNumericCellValue();
                dx.setItemId(id);
            }

            Cell nameCell = row.getCell(1);
            if (nameCell != null && nameCell.getCellType() == CellType.STRING) {
                dxName = nameCell.getStringCellValue();
                if (dxName == null || dxName.trim().equals("")) {
                    continue;
                }
                dx = diagnosisController.findAndSaveDiagnosis(dxName);
                if (dx != null) {
                    continue;
                }
            } else {
                continue;
            }

            dxx.add(dx);
        }

        return dxx;
    }

    private List<Amp> readAmpsFromExcel(InputStream inputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();

        List<Amp> amps = new ArrayList<>();

        // Assuming the first row contains headers, skip it
        if (rowIterator.hasNext()) {
            rowIterator.next();
        }

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Amp amp = new Amp();
            Atm atm = null;
            Vmp vmp = null;

            /**
             * ItemID tblItem_Item ItemCode tradename_Item generic_Item Category
             * strength unit issue unit pack unit StrengthUnitsPerIssueUnit
             * IssueUnitsPerPack	ROL	ROQ	MinROQ	manu_Institution
             * importer_Institution	supplier_Institution	SalePrice	PurchasePrice
             * LastSalePrice	LastPurchasePrice	MinIQty	MaxIQty	DivQty
             */
            Long id;
            String ampName = null;
            String code = null;
            String atmName = null;
            String vmpName = null;
            String manufacturerName = null;
            String importerName = null;
            String supplierName = null;
            Institution supplier;
            Institution importer;
            Institution manufacturer;

            Cell idCell = row.getCell(0);
            if (idCell != null && idCell.getCellType() == CellType.NUMERIC) {
                id = (long) idCell.getNumericCellValue();
                amp.setItemId(id);
            }

            Cell nameCell = row.getCell(1);
            if (nameCell != null && nameCell.getCellType() == CellType.STRING) {
                ampName = nameCell.getStringCellValue();
                if (ampName == null || ampName.trim().equals("")) {
                    continue;
                }
                amp = ampController.findAmpByName(ampName);
                if (amp != null) {
                    continue;
                }
            } else {
                continue;
            }

            Cell codeCell = row.getCell(2);
            if (codeCell != null && codeCell.getCellType() == CellType.STRING) {
                code = codeCell.getStringCellValue();
            }

            Cell atmCell = row.getCell(3);
            if (atmCell != null && atmCell.getCellType() == CellType.STRING) {
                atmName = atmCell.getStringCellValue();
                atm = atmController.findAtmByName(atmName);
            }

            Cell vmpCell = row.getCell(4);
            if (vmpCell != null && vmpCell.getCellType() == CellType.STRING) {
                vmpName = vmpCell.getStringCellValue();
                vmp = vmpController.findVmpByName(vmpName);
            }

            Cell manufacturerCell = row.getCell(5);
            if (manufacturerCell != null && manufacturerCell.getCellType() == CellType.STRING) {
                manufacturerName = manufacturerCell.getStringCellValue();
                manufacturer = institutionController.findAndSaveInstitutionByName(manufacturerName);
            }

            Cell importerCell = row.getCell(6);
            if (importerCell != null && importerCell.getCellType() == CellType.STRING) {
                importerName = importerCell.getStringCellValue();
                importer = institutionController.findAndSaveInstitutionByName(importerName);
            }

            Cell supplierCell = row.getCell(7);
            if (supplierCell != null && supplierCell.getCellType() == CellType.STRING) {
                supplierName = supplierCell.getStringCellValue();
                supplier = institutionController.findAndSaveInstitutionByName(supplierName);
            }

            amp = new Amp();
            amp.setName(ampName);
            amp.setCode("amp_" + CommonController.nameToCode(ampName));
            if (atm != null) {
                amp.setAtm(atm);
            }
            amp.setVmp(vmp);
            amp.setBarcode(code);
            amp.setCreater(sessionController.getLoggedUser());

            ampController.saveAmp(amp);

            amps.add(amp);

        }

        return amps;
    }

    private List<Vmp> readVmpsFromExcel(InputStream inputStream) throws IOException {
        outputString += "readVmpsFromExcel";
        StringBuilder output = new StringBuilder();
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();

        List<Vmp> vmps = new ArrayList<>();

        // Assuming the first row contains headers, skip it
        if (rowIterator.hasNext()) {
            rowIterator.next();
        }

        while (rowIterator.hasNext()) {
            output.append("while").append("\n");
            Row row = rowIterator.next();
            Vmp vmp = new Vmp();
            Vtm vtm = null;
            Category dosageForm = null;
            MeasurementUnit strengthUnit = null;
            MeasurementUnit issueUnit = null;
            MeasurementUnit packUnit = null;
            MeasurementUnit minimumIssueUnit = null;
            MeasurementUnit issueMultipliesUnit = null;
            /**
             * ItemID tblItem_Item ItemCode tradename_Item generic_Item Category
             * strength unit issue unit pack unit StrengthUnitsPerIssueUnit
             * IssueUnitsPerPack	ROL	ROQ	MinROQ	manu_Institution
             * importer_Institution	supplier_Institution	SalePrice	PurchasePrice
             * LastSalePrice	LastPurchasePrice	MinIQty	MaxIQty	DivQty
             */
            Long id;
            String vmpName;
            String vtmName;
            String dosageFormName;
            String strengthUnitName;
            String issueUnitName;
            String packUnitName;
            String minimumIssueUnitName;
            String issueMultipliesUnitName;

            Double strengthUnitsPerIssueUnit = null;
            Double issueUnitsPerPack = null;
            Double minimumIssueQuantity = null;
            Double issueMultipliesQuantity = null;

            Cell idCell = row.getCell(0);
            System.out.println("idCell = " + idCell);
            if (idCell != null && idCell.getCellType() == CellType.NUMERIC) {
                id = (long) idCell.getNumericCellValue();
                vmp.setItemId(id);
            }

            Cell nameCell = row.getCell(1);
            System.out.println("nameCell = " + nameCell);
            if (nameCell != null && nameCell.getCellType() == CellType.STRING) {
                vmpName = nameCell.getStringCellValue();
                output.append("vmpName").append(vmpName).append("/n");
                if (vmpName == null || vmpName.trim().equals("")) {
                    output.append("vmpName is null. exiting from Loop.").append("/n");
                    continue;
                }
                vmp = vmpController.findVmpByName(vmpName);
                output.append("vmp").append(vmp).append("/n");
                if (vmp != null) {
                    vmps.add(vmp);
                    continue;
                }
            } else {
                output.append("exiting from loop").append("/n");
                continue;
            }

            Cell vtmCell = row.getCell(2);
            System.out.println("vtmCell = " + vtmCell);
            if (vtmCell != null && vtmCell.getCellType() == CellType.STRING) {
                vtmName = vtmCell.getStringCellValue();
                vtm = vtmController.findAndSaveVtmByName(vtmName);
            }

            Cell dosageFormCell = row.getCell(3);
            System.out.println("dosageFormCell = " + dosageFormCell);
            if (dosageFormCell != null && dosageFormCell.getCellType() == CellType.STRING) {
                dosageFormName = dosageFormCell.getStringCellValue();
                dosageForm = categoryController.findAndCreateCategoryByName(dosageFormName);
            }

            Cell strengthUnitCell = row.getCell(4);
            System.out.println("strengthUnitCell = " + strengthUnitCell);
            if (strengthUnitCell != null && strengthUnitCell.getCellType() == CellType.STRING) {
                strengthUnitName = strengthUnitCell.getStringCellValue();
                strengthUnit = measurementUnitController.findAndSaveMeasurementUnitByName(strengthUnitName);
                strengthUnit.setStrengthUnit(true);
                measurementUnitController.save(strengthUnit);
            }

            Cell issueUnitCell = row.getCell(5);
            System.out.println("issueUnitCell = " + issueUnitCell);
            if (issueUnitCell != null && issueUnitCell.getCellType() == CellType.STRING) {
                issueUnitName = issueUnitCell.getStringCellValue();
                issueUnit = measurementUnitController.findAndSaveMeasurementUnitByName(issueUnitName);
                issueUnit.setIssueUnit(true);
                measurementUnitController.save(issueUnit);
            }

            Cell strengthUnitsPerIssueUnitCell = row.getCell(6);
            System.out.println("strengthUnitsPerIssueUnitCell = " + strengthUnitsPerIssueUnitCell);
            if (strengthUnitsPerIssueUnitCell != null) {
                // Check the cell type before retrieving the value
                if (strengthUnitsPerIssueUnitCell.getCellType() == CellType.NUMERIC) {
                    strengthUnitsPerIssueUnit = strengthUnitsPerIssueUnitCell.getNumericCellValue();
                } else if (strengthUnitsPerIssueUnitCell.getCellType() == CellType.STRING) {
                    // If the cell contains a string, you can either parse the string as a numeric value or handle the error
                    try {
                        strengthUnitsPerIssueUnit = Double.parseDouble(strengthUnitsPerIssueUnitCell.getStringCellValue());
                    } catch (NumberFormatException e) {
// Handle the error, e.g., log it, throw a custom exception, or set a default value
                                                strengthUnitsPerIssueUnit = 0.0; // Set a default value or any other appropriate value
                    }
                } else {
// Handle other cell types or set a default value
                                        strengthUnitsPerIssueUnit = 0.0; // Set a default value or any other appropriate value
                }
            }

            Cell issueUnitsPerPackCell = row.getCell(7);
            System.out.println("issueUnitsPerPackCell = " + issueUnitsPerPackCell);
            if (issueUnitsPerPackCell != null && issueUnitsPerPackCell.getCellType() == CellType.NUMERIC) {
                issueUnitsPerPack = issueUnitsPerPackCell.getNumericCellValue();
            }

            Cell packUnitCell = row.getCell(8);
            System.out.println("packUnitCell = " + packUnitCell);
            if (packUnitCell != null && packUnitCell.getCellType() == CellType.STRING) {
                packUnitName = packUnitCell.getStringCellValue();
                packUnit = measurementUnitController.findAndSaveMeasurementUnitByName(packUnitName);
                packUnit.setIssueUnit(true);
                measurementUnitController.save(packUnit);
            }

            Cell minIQtyCell = row.getCell(9);
            System.out.println("minIQtyCell = " + minIQtyCell);
            if (minIQtyCell != null && minIQtyCell.getCellType() == CellType.NUMERIC) {
                minimumIssueQuantity = minIQtyCell.getNumericCellValue();
            }

            Cell minimumIssueUnitCell = row.getCell(10);
            if (minimumIssueUnitCell != null && minimumIssueUnitCell.getCellType() == CellType.STRING) {
                minimumIssueUnitName = minimumIssueUnitCell.getStringCellValue();
                minimumIssueUnit = measurementUnitController.findAndSaveMeasurementUnitByName(minimumIssueUnitName);
                minimumIssueUnit.setIssueUnit(true);
                measurementUnitController.save(minimumIssueUnit);
            }

            Cell issueMultipliesQuentityCell = row.getCell(11);
            if (issueMultipliesQuentityCell != null && issueMultipliesQuentityCell.getCellType() == CellType.NUMERIC) {
                issueMultipliesQuantity = issueMultipliesQuentityCell.getNumericCellValue();
            }

            Cell issueMultipliesUnitNameCell = row.getCell(12);
            if (issueMultipliesUnitNameCell != null && issueMultipliesUnitNameCell.getCellType() == CellType.STRING) {
                issueMultipliesUnitName = issueMultipliesUnitNameCell.getStringCellValue();
                issueMultipliesUnit = measurementUnitController.findAndSaveMeasurementUnitByName(issueMultipliesUnitName);
                issueMultipliesUnit.setIssueUnit(true);
                measurementUnitController.save(issueMultipliesUnit);
            }
            output.append("vtm = ").append(vtm).append("\n");
            output.append("dosageForm = ").append(dosageForm).append("\n");
            output.append("strengthUnitsPerIssueUnit = ").append(strengthUnitsPerIssueUnit).append("\n");
            output.append("strengthUnit = ").append(strengthUnit).append("\n");
            output.append("issueUnitsPerPack = ").append(issueUnitsPerPack).append("\n");
            output.append("packUnit = ").append(packUnit).append("\n");
            output.append("minimumIssueQuantity = ").append(minimumIssueQuantity).append("\n");
            output.append("minimumIssueUnit = ").append(minimumIssueUnit).append("\n");
            output.append("issueMultipliesQuantity = ").append(issueMultipliesQuantity).append("\n");
            output.append("issueMultipliesUnit = ").append(issueMultipliesUnit).append("\n");

            if (vtm == null
                    || dosageForm == null
                    || strengthUnitsPerIssueUnit == null
                    || strengthUnit == null
                    || issueUnitsPerPack == null
                    || packUnit == null
                    || minimumIssueQuantity == null
                    || minimumIssueUnit == null
                    || issueMultipliesQuantity == null
                    || issueMultipliesUnit == null) {
                output.append("One or more of the required parameters for createVmp are null.").append("\n");
                continue;
            } else {
                vmp = vmpController.createVmp(vmpName,
                        vtm,
                        dosageForm,
                        strengthUnitsPerIssueUnit,
                        strengthUnit,
                        issueUnitsPerPack,
                        packUnit,
                        minimumIssueQuantity,
                        minimumIssueUnit,
                        issueMultipliesQuantity,
                        issueMultipliesUnit
                );
                output.append("VMP Created.").append("\n");
            }

            vmps.add(vmp);

        }
        outputString = output.toString();

        return vmps;
    }

    private LocalDate parseDate(String dateString, List<String> patterns) {
        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return LocalDate.parse(dateString, formatter);
            } catch (DateTimeParseException e) {
                // Ignore exception and try the next pattern
            }
        }
        throw new IllegalArgumentException("No matching date format found for: " + dateString);
    }

    public String getOutputString() {
        return outputString;
    }

    public void setOutputString(String outputString) {
        this.outputString = outputString;
    }

}
