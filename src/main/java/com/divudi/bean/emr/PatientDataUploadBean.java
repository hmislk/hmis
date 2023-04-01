package com.divudi.bean.emr;

import com.divudi.bean.common.ItemController;
import com.divudi.data.Sex;
import com.divudi.data.Title;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
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
public class PatientDataUploadBean {

    @Inject
    ItemController itemController;
    private List<Patient> patients;
    @EJB
    PatientFacade patientFacade;
    @EJB
    PersonFacade personFacade;

    private UploadedFile file;

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public String toUploadPatients() {
        return "/emr/upload_patients";
    }

    public void upload() {
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

    private List<Patient> readPatientDataFromExcel(InputStream inputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();

        List<Patient> patients = new ArrayList<>();

        List<String> datePatterns = Arrays.asList("M/d/yy", "MM/dd/yyyy", "d/M/yy", "dd/MM,yyyy", "dd/MM/yyyy", "yyyy-MM-dd");

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
            patient.setCode(row.getCell(2).getStringCellValue());

            String dateOfBirthStr = dataFormatter.formatCellValue(row.getCell(3));
            LocalDate localDateOfBirth = parseDate(dateOfBirthStr, datePatterns);
            Instant instant = localDateOfBirth.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Date dateOfBirth = Date.from(instant);
            patient.getPerson().setDob(dateOfBirth);

            patient.getPerson().setAddress(row.getCell(4).getStringCellValue());
            patient.getPerson().setPhone(row.getCell(5).getStringCellValue());
            patient.getPerson().setMobile(row.getCell(6).getStringCellValue());
            patient.getPerson().setEmail(row.getCell(7).getStringCellValue());

            Title title;
            try {
                title = Title.valueOf(row.getCell(8).getStringCellValue());
            } catch (IllegalArgumentException e) {
                title = Title.Mr;
            }
            patient.getPerson().setTitle(title);

            String sex = row.getCell(9).getStringCellValue();
            if (sex.toLowerCase().contains("f")) {
                patient.getPerson().setSex(Sex.Female);
            } else {
                patient.getPerson().setSex(Sex.Male);
            }

            String strCivilStatus = row.getCell(10).getStringCellValue();
            Item civilStatus = itemController.findItemByName(strCivilStatus, "civil_statuses");
            patient.getPerson().setCivilStatus(civilStatus);

            // Code for Race
            String strRace = row.getCell(11).getStringCellValue();
            Item race = itemController.findItemByName(strRace, "races");
            patient.getPerson().setRace(race);

// Code for BloodGroup
            String strBloodGroup = row.getCell(12).getStringCellValue();
            Item bloodGroup = itemController.findItemByName(strBloodGroup, "blood_groups");
            patient.getPerson().setBloodGroup(bloodGroup);

            patient.setComments(row.getCell(13).getStringCellValue());
            patient.getPerson().setFullName(row.getCell(14).getStringCellValue());

// Code for Occupation
            String strOccupation = row.getCell(15).getStringCellValue();
            Item occupation = itemController.findItemByName(strOccupation, "occupations");
            patient.getPerson().setOccupation(occupation);

            patients.add(patient);
        }

        return patients;
    }

    public List<Patient> getPatients() {
        return patients;
    }

    public void setPatients(List<Patient> patients) {
        this.patients = patients;
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
