package com.divudi.bean.emr;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.pharmacy.AmpController;
import com.divudi.bean.pharmacy.AtmController;
import com.divudi.bean.pharmacy.VtmController;
import com.divudi.data.Sex;
import com.divudi.data.Title;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.Atm;
import com.divudi.entity.pharmacy.Vtm;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.VtmFacade;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import javax.servlet.http.Part;
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

    @EJB
    PatientFacade patientFacade;
    @EJB
    PersonFacade personFacade;
    @EJB
    VtmFacade vtmFacade;

    private UploadedFile file;

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public String toUploadPatients() {
        return "/emr/setup/upload_patients";
    }

    public String toUploadVtms() {
        return "/emr/setup/upload_vtms";
    }

    public String toUploadAtms() {
        return "/emr/setup/upload_atms";
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
                amps = readAmpsFromExcel(inputStream);
                for (Amp v : amps) {
                    ampController.findAndSaveAtmByNameAndCode(v, v.getVtm());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
                Vtm vtm = vtmController.findAndSaveVtmByName(vtmName);
                atm.setVtm(vtm);
            }
            atm.setCreatedAt(new Date());
            atm.setCreater(sessionController.getLoggedUser());
            atms.add(atm);

        }

        return atms;
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

            /**
             * ItemID tblItem_Item ItemCode tradename_Item generic_Item Category
             * strength unit issue unit pack unit StrengthUnitsPerIssueUnit
             * IssueUnitsPerPack	ROL	ROQ	MinROQ	manu_Institution
             * importer_Institution	supplier_Institution	SalePrice	PurchasePrice
             * LastSalePrice	LastPurchasePrice	MinIQty	MaxIQty	DivQty
             */
            Long id;
            String ampName;
            String code;
            String atm;
            String vtm;
            String category;
            String strengthUnit;
            String issueUnit;
            String packUnit;

            Double strengthUnitsPerIssueUnit;
            Double issueUnitsPerPack;
            Double ROL;
            Double ROQ;
            Double minROQ;

            String manufacturer;
            String importer;
            String supplier;

            Double salePrice;
            Double purchasePrice;
            Double lastSalePrice;
            Double lastPurchasePrice;
            Double minIQty;
            Double maxIQty;
            Double divQty;

            Cell idCell = row.getCell(0);
            if (idCell != null) {
                id = (long) idCell.getNumericCellValue();
                amp.setItemId(id);
            }

            Cell nameCell = row.getCell(1);
            if (nameCell != null) {
                ampName = nameCell.getStringCellValue();
                amp = ampController.findAmpByName(ampName);
                if (amp != null) {
                    continue;
                }
            }

            Cell codeCell = row.getCell(2);
            if (codeCell != null) {
                code = codeCell.getStringCellValue();
            }


            Cell atmCell = row.getCell(3);
            if (atmCell != null) {
                atm = atmCell.getStringCellValue();
            }

            Cell vtmCell = row.getCell(4);
            if (vtmCell != null) {
                vtm = vtmCell.getStringCellValue();
            }

            Cell categoryCell = row.getCell(5);
            if (categoryCell != null) {
                category = categoryCell.getStringCellValue();
            }

            Cell strengthUnitCell = row.getCell(6);
            if (strengthUnitCell != null) {
                strengthUnit = strengthUnitCell.getStringCellValue();
            }

            Cell issueUnitCell = row.getCell(7);
            if (issueUnitCell != null) {
                issueUnit = issueUnitCell.getStringCellValue();
            }

            Cell packUnitCell = row.getCell(8);
            if (packUnitCell != null) {
                packUnit = packUnitCell.getStringCellValue();
            }

            Cell strengthUnitsPerIssueUnitCell = row.getCell(9);
            if (strengthUnitsPerIssueUnitCell != null) {
                strengthUnitsPerIssueUnit = strengthUnitsPerIssueUnitCell.getNumericCellValue();
            }

            Cell issueUnitsPerPackCell = row.getCell(10);
            if (issueUnitsPerPackCell != null) {
                issueUnitsPerPack = issueUnitsPerPackCell.getNumericCellValue();
            }

            Cell ROLCell = row.getCell(11);
            if (ROLCell != null) {
                ROL = ROLCell.getNumericCellValue();
            }

            Cell ROQCell = row.getCell(12);
            if (ROQCell != null) {
                ROQ = ROQCell.getNumericCellValue();
            }

            Cell minROQCell = row.getCell(13);
            if (minROQCell != null) {
                minROQ = minROQCell.getNumericCellValue();
            }

            Cell manufacturerCell = row.getCell(14);
            if (manufacturerCell != null) {
                manufacturer = manufacturerCell.getStringCellValue();
            }

            Cell importerCell = row.getCell(15);
            if (importerCell != null) {
                importer = importerCell.getStringCellValue();
            }

            Cell supplierCell = row.getCell(16);
            if (supplierCell != null) {
                supplier = supplierCell.getStringCellValue();
            }

            Cell salePriceCell = row.getCell(17);
            if (salePriceCell != null) {
                salePrice = salePriceCell.getNumericCellValue();
            }

            Cell purchasePriceCell = row.getCell(18);
            if (purchasePriceCell != null) {
                purchasePrice = purchasePriceCell.getNumericCellValue();
            }

            Cell lastSalePriceCell = row.getCell(19);
            if (lastSalePriceCell != null) {
                lastSalePrice = lastSalePriceCell.getNumericCellValue();
            }

            Cell lastPurchasePriceCell = row.getCell(20);
            if (lastPurchasePriceCell != null) {
                lastPurchasePrice = lastPurchasePriceCell.getNumericCellValue();
            }

            Cell minIQtyCell = row.getCell(21);
            if (minIQtyCell != null) {
                minIQty = minIQtyCell.getNumericCellValue();
            }

            Cell maxIQtyCell = row.getCell(22);
            if (maxIQtyCell != null) {
                maxIQty = maxIQtyCell.getNumericCellValue();
            }

            Cell divQtyCell = row.getCell(23);
            if (divQtyCell != null) {
                divQty = divQtyCell.getNumericCellValue();
            }

            amp.setCreatedAt(new Date());
            amp.setCreater(sessionController.getLoggedUser());
            amps.add(amp);

        }

        return amps;
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

}
