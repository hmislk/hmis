package com.divudi.bean.emr;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.primefaces.model.DefaultStreamedContent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import com.divudi.bean.clinical.DiagnosisController;
import com.divudi.bean.clinical.PatientEncounterController;
import com.divudi.bean.common.CategoryController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.DepartmentController;
import com.divudi.bean.common.EnumController;
import com.divudi.bean.common.InstitutionController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.ItemFeeController;
import com.divudi.bean.common.ItemFeeManager;
import com.divudi.bean.common.PatientController;
import com.divudi.bean.common.RouteController;
import com.divudi.bean.common.ServiceController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.lab.CollectingCentreController;
import com.divudi.bean.lab.InvestigationCategoryController;
import com.divudi.bean.lab.InvestigationController;
import com.divudi.bean.lab.InvestigationTubeController;
import com.divudi.bean.lab.MachineController;
import com.divudi.bean.lab.SampleController;
import com.divudi.bean.pharmacy.AmpController;
import com.divudi.bean.pharmacy.AtmController;
import com.divudi.bean.pharmacy.MeasurementUnitController;
import com.divudi.bean.pharmacy.VmpController;
import com.divudi.bean.pharmacy.VtmController;
import com.divudi.data.CollectingCentrePaymentMethod;
import com.divudi.data.EncounterType;
import com.divudi.data.FeeType;
import com.divudi.data.InstitutionType;
import com.divudi.data.Sex;
import com.divudi.data.Title;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.Route;
import com.divudi.entity.Service;
import com.divudi.entity.clinical.ClinicalEntity;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.InvestigationTube;
import com.divudi.entity.lab.Machine;
import com.divudi.entity.lab.Sample;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.Atm;
import com.divudi.entity.pharmacy.MeasurementUnit;
import com.divudi.entity.pharmacy.Vmp;
import com.divudi.entity.pharmacy.Vtm;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.VtmFacade;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
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
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

@Named
@ViewScoped
public class DataUploadBean implements Serializable {

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
    @Inject
    ServiceController serviceController;
    @Inject
    InvestigationController investigationController;
    @Inject
    DepartmentController departmentController;
    @Inject
    EnumController enumController;
    @Inject
    SampleController sampleController;
    @Inject
    InvestigationTubeController investigationTubeController;
    @Inject
    MachineController machineController;
    @Inject
    InvestigationCategoryController investigationCategoryController;
    @Inject
    ItemFeeManager itemFeeManager;
    @Inject
    ItemFeeController itemFeeController;
    @Inject
    CollectingCentreController collectingCentreController;
    @Inject
    RouteController routeController;

    @EJB
    PatientFacade patientFacade;
    @EJB
    PersonFacade personFacade;
    @EJB
    VtmFacade vtmFacade;
    @EJB
    ItemFeeFacade itemFeeFacade;
    @EJB
    ItemFacade itemFacade;

    private UploadedFile file;
    private String outputString;
    private List<Item> items;
    private List<ItemFee> itemFees;
    private List<Institution> collectingCentres;
    private StreamedContent templateForItemWithFeeUpload;
    private StreamedContent templateForCollectingCentreUpload;

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

    public void uploadItems() {
        items = new ArrayList<>();
        if (file != null) {
            try ( InputStream inputStream = file.getInputStream()) {
                items = readItemsFromExcel(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private List<Item> readItemsFromExcel(InputStream inputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();

        List<Item> items = new ArrayList<>();
        Item item;

        // Assuming the first row contains headers, skip it
        if (rowIterator.hasNext()) {
            rowIterator.next();
        }

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();

            item = null;
            Category cat = null;
            Institution institution = null;
            Department department = null;
            InwardChargeType iwct = null;

            String name = null;
            String printingName = null;
            String fullName = null;
            String code = null;
            String categoryName = null;
            String institutionName = null;
            String departmentName = null;
            String inwardName = null;

            String itemType = "Service";
            Double hospitalFee = 0.0;
            Double collectingCentreFee = 0.0;

            Cell nameCell = row.getCell(0);
            if (nameCell != null && nameCell.getCellType() == CellType.STRING) {
                name = nameCell.getStringCellValue();
                if (name == null || name.trim().equals("")) {
                    continue;
                }

            }

            Item masterItem = itemController.findMasterItemByName(name);

            Cell printingNameCell = row.getCell(1);
            if (printingNameCell != null && printingNameCell.getCellType() == CellType.STRING) {
                printingName = printingNameCell.getStringCellValue();

            }
            if (printingName == null || printingName.trim().equals("")) {
                printingName = name;
            }

            Cell fullNameCell = row.getCell(2);
            if (fullNameCell != null && fullNameCell.getCellType() == CellType.STRING) {
                fullName = fullNameCell.getStringCellValue();
            }
            if (fullName == null || fullName.trim().equals("")) {
                fullName = name;
            }

            Cell codeCell = row.getCell(3);
            if (codeCell != null && codeCell.getCellType() == CellType.STRING) {
                code = codeCell.getStringCellValue();
            }
            if (code == null || code.trim().equals("")) {
                code = serviceController.generateShortCode(name);
            }

            Cell categoryCell = row.getCell(4);
            System.out.println("categoryCell = " + categoryCell);
            if (categoryCell != null && categoryCell.getCellType() == CellType.STRING) {
                categoryName = categoryCell.getStringCellValue();
                System.out.println("categoryName = " + categoryName);
            }
            System.out.println("categoryName = " + categoryName);
            if (categoryName == null || categoryName.trim().equals("")) {
                continue;
            }
            
            cat = categoryController.findAndCreateCategoryByName(categoryName);
            System.out.println("cat = " + cat);
            
            
            Cell insCell = row.getCell(5);
            if (insCell != null && insCell.getCellType() == CellType.STRING) {
                institutionName = insCell.getStringCellValue();
            }
            if (institutionName != null && !institutionName.trim().equals("")) {
                institution = institutionController.findAndSaveInstitutionByName(institutionName);
            } else {
                institution = institutionController.findAndSaveInstitutionByName("Other");
            }

            Cell deptCell = row.getCell(6);
            if (deptCell != null && deptCell.getCellType() == CellType.STRING) {
                departmentName = deptCell.getStringCellValue();
            }
            if (departmentName != null && !departmentName.trim().equals("")) {
                department = departmentController.findAndSaveDepartmentByName(departmentName);
            }

            if (department == null) {
                department = departmentController.getDefaultDepatrment(institution);
            }

            Cell inwardCcCell = row.getCell(7);
            if (inwardCcCell != null && inwardCcCell.getCellType() == CellType.STRING) {
                inwardName = inwardCcCell.getStringCellValue();
            }
            if (inwardName != null && !inwardName.trim().equals("")) {
                iwct = enumController.getInaChargeType(inwardName);
            }
            if (iwct == null) {
                iwct = InwardChargeType.OtherCharges;
            }

            Cell itemTypeCell = row.getCell(8);
            if (itemTypeCell != null && itemTypeCell.getCellType() == CellType.STRING) {
                itemType = itemTypeCell.getStringCellValue();
            }
            if (itemType == null || itemType.trim().equals("")) {
                itemType = "Investigation";
            }
            if (itemType.equals("Service")) {
                if (masterItem == null) {
                    masterItem = new Service();
                    masterItem.setName(name);
                    masterItem.setPrintName(printingName);
                    masterItem.setFullName(fullName);
                    masterItem.setCode(code);
                    masterItem.setCategory(cat);
                    masterItem.setIsMasterItem(true);
                    masterItem.setInwardChargeType(iwct);
                    masterItem.setCreater(sessionController.getLoggedUser());
                    masterItem.setCreatedAt(new Date());
                    itemController.saveSelected(masterItem);
                }

                Service service = new Service();
                service.setName(name);
                service.setPrintName(printingName);
                service.setFullName(fullName);
                service.setCode(code);
                service.setCategory(cat);
                service.setMasterItemReference(masterItem);
                service.setInstitution(institution);
                service.setDepartment(department);
                service.setInwardChargeType(iwct);
                service.setCreater(sessionController.getLoggedUser());
                service.setCreatedAt(new Date());
                serviceController.save(service);
                items.add(service);
                item = service;
            } else if (itemType.equals("Investigation")) {

                if (masterItem == null) {
                    masterItem = new Investigation();
                    masterItem.setName(name);
                    masterItem.setPrintName(printingName);
                    masterItem.setFullName(fullName);
                    masterItem.setCode(code);
                    masterItem.setIsMasterItem(true);
                    masterItem.setCategory(cat);
                    masterItem.setInwardChargeType(iwct);
                    masterItem.setCreater(sessionController.getLoggedUser());
                    masterItem.setCreatedAt(new Date());
                    itemController.saveSelected(masterItem);
                }
                Investigation ix = new Investigation();
                ix.setName(name);
                ix.setPrintName(printingName);
                ix.setFullName(fullName);
                ix.setCode(code);
                ix.setCategory(cat);
                ix.setInstitution(institution);
                ix.setDepartment(department);
                ix.setInwardChargeType(iwct);
                ix.setMasterItemReference(masterItem);
                ix.setCreater(sessionController.getLoggedUser());
                ix.setCreatedAt(new Date());
                investigationController.save(ix);
                items.add(ix);
                item = ix;
            }

            if (item == null) {
                continue;
            }

            Cell hospitalFeeTypeCell = row.getCell(9);
            if (hospitalFeeTypeCell != null) {
                if (hospitalFeeTypeCell.getCellType() == CellType.NUMERIC) {
                    // If it's a numeric value
                    hospitalFee = hospitalFeeTypeCell.getNumericCellValue();
                } else if (hospitalFeeTypeCell.getCellType() == CellType.FORMULA) {
                    // If it's a formula, evaluate it
                    Workbook wb = hospitalFeeTypeCell.getSheet().getWorkbook();
                    CreationHelper createHelper = wb.getCreationHelper();
                    FormulaEvaluator evaluator = createHelper.createFormulaEvaluator();
                    CellValue cellValue = evaluator.evaluate(hospitalFeeTypeCell);

                    // Check the type of the evaluated value
                    if (cellValue.getCellType() == CellType.NUMERIC) {
                        hospitalFee = cellValue.getNumberValue();
                    } else {
                        // Handle other types if needed
                    }
                }

                // Rest of your code remains the same
                ItemFee itf = new ItemFee();
                itf.setName("Hospital Fee");
                itf.setItem(item);
                itf.setInstitution(sessionController.getInstitution());
                itf.setDepartment(sessionController.getDepartment());
                itf.setFeeType(FeeType.OwnInstitution);
                itf.setFee(hospitalFee);
                itf.setFfee(hospitalFee);
                itf.setCreatedAt(new Date());
                itf.setCreater(sessionController.getLoggedUser());
                itemFeeFacade.create(itf);
            }

            Cell collectingCenterFeeTypeCell = row.getCell(10);
            if (collectingCenterFeeTypeCell != null) {
                if (collectingCenterFeeTypeCell.getCellType() == CellType.NUMERIC) {
                    // If it's a numeric value
                    collectingCentreFee = collectingCenterFeeTypeCell.getNumericCellValue();
                } else if (collectingCenterFeeTypeCell.getCellType() == CellType.FORMULA) {
                    // If it's a formula, evaluate it
                    Workbook wb = collectingCenterFeeTypeCell.getSheet().getWorkbook();
                    CreationHelper createHelper = wb.getCreationHelper();
                    FormulaEvaluator evaluator = createHelper.createFormulaEvaluator();
                    CellValue cellValue = evaluator.evaluate(collectingCenterFeeTypeCell);

                    // Check the type of the evaluated value
                    if (cellValue.getCellType() == CellType.NUMERIC) {
                        collectingCentreFee = cellValue.getNumberValue();
                    } else {
                        // Handle other types if needed
                    }
                }

                // Rest of your code remains the same
                ItemFee itf = new ItemFee();
                itf.setName("Hospital Fee");
                itf.setItem(item);
                itf.setInstitution(institution);
                itf.setDepartment(department);
                itf.setFeeType(FeeType.CollectingCentre);
                itf.setFee(collectingCentreFee);
                itf.setFfee(collectingCentreFee);
                itf.setCreatedAt(new Date());
                itf.setCreater(sessionController.getLoggedUser());
                itemFeeFacade.create(itf);
            }

            item.setTotal(hospitalFee + collectingCentreFee);
            item.setTotalForForeigner((hospitalFee + collectingCentreFee) * 2);
            item.setDblValue(hospitalFee + collectingCentreFee);
            itemFacade.edit(item);

        }

        return items;
    }

    public void uploadCollectingCentres() {
        collectingCentres = new ArrayList<>();
        if (file != null) {
            try ( InputStream inputStream = file.getInputStream()) {
                collectingCentres = readCollectingCentresFromExcel(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private List<Institution> readCollectingCentresFromExcel(InputStream inputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();

        List<Institution> collectingCentresList = new ArrayList<>();
        Institution collectingCentre;

        // Assuming the first row contains headers, skip it
        if (rowIterator.hasNext()) {
            rowIterator.next();
        }

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();

            collectingCentre = null;
            Route route = null;

            String code = null;
            String collectingCentreName = null;
            Boolean active = null;
            Boolean withCommissionStatus = null;
            String routeName = null;
            Double percentage = null;
            String collectingCentrePrintingName = null;
            Double standardCreditLimit = null;
            Double allowedCreditLimit = null;
            Double maxCreditLimit = null;
            String phone = null;
            String email = null;
            String ownerName = null;
            String address = null;

            Cell codeCell = row.getCell(0);
            if (codeCell != null && codeCell.getCellType() == CellType.STRING) {
                code = codeCell.getStringCellValue();
                System.out.println("codeCell = " + codeCell);
            }

            if (code == null || code.trim().equals("")) {
                System.out.println("code = " + code);
                continue;
            }

            //    Item masterItem = itemController.findMasterItemByName(code);
            Cell agentNameCell = row.getCell(1);
           
            if (agentNameCell != null && agentNameCell.getCellType() == CellType.STRING) {
                collectingCentreName = agentNameCell.getStringCellValue();
                System.out.println("collectingCentreName = " + collectingCentreName);
            }
            if (collectingCentreName == null || collectingCentreName.trim().equals("")) {
                continue;
            }

            Cell agentPrintingNameCell = row.getCell(2);
           
            if (agentPrintingNameCell != null && agentPrintingNameCell.getCellType() == CellType.STRING) {
                collectingCentrePrintingName = agentPrintingNameCell.getStringCellValue();
              
            }
            if (collectingCentrePrintingName == null || collectingCentrePrintingName.trim().equals("")) {
                collectingCentrePrintingName=collectingCentreName;
            }

            Cell activeCell = row.getCell(3);
         
            if (activeCell != null && activeCell.getCellType() == CellType.BOOLEAN) {
                active = activeCell.getBooleanCellValue();
               
            }
            if (active == null) {
                active = false;
            }

            Cell withCommissionStatusCell = row.getCell(4);
           
            if (withCommissionStatusCell != null && withCommissionStatusCell.getCellType() == CellType.BOOLEAN) {
                withCommissionStatus = withCommissionStatusCell.getBooleanCellValue();
               
            }
            if (withCommissionStatus == null) {
                withCommissionStatus = false;
            }

            Cell routeNameCell = row.getCell(5);
            System.out.println("routeNameCell = " + routeNameCell);
            if (routeNameCell != null && routeNameCell.getCellType() == CellType.STRING) {
                routeName = routeNameCell.getStringCellValue();
                route = routeController.findAndCreateRouteByName(routeName);
              
            }
            if (routeName == null || routeName.trim().equals("")) {
                route = null;
            }

            Cell percentageCell = row.getCell(6);
            
            if (percentageCell != null) {
                if (percentageCell.getCellType() == CellType.NUMERIC) {
                    percentage = percentageCell.getNumericCellValue();
                    
                }
                
                else if (percentageCell.getCellType() == CellType.STRING) {
                    percentage = Double.parseDouble(percentageCell.getStringCellValue());
                }
            }

            if (percentage == null) {
                percentage = 0.0;
            }

            Cell contactNumberCell = row.getCell(7);
           
            if (contactNumberCell != null) {
                if (contactNumberCell.getCellType() == CellType.NUMERIC) {
                    phone = String.valueOf(contactNumberCell.getNumericCellValue());
                   
                } else if (contactNumberCell.getCellType() == CellType.STRING) {
                    phone = contactNumberCell.getStringCellValue();
                }
            }
            if (phone == null || phone.trim().equals("")) {
                phone = null;
            }

            Cell emailAddressCell = row.getCell(8);
        
            if (emailAddressCell != null && emailAddressCell.getCellType() == CellType.STRING) {
                email = emailAddressCell.getStringCellValue();
               
            }
            if (email == null || email.trim().equals("")) {
                email = null;
            }

            Cell ownerNameCell = row.getCell(9);
        
            if (ownerNameCell != null && ownerNameCell.getCellType() == CellType.STRING) {
                ownerName = ownerNameCell.getStringCellValue();
                System.out.println("ownerName = " + ownerName);
            }
            if (ownerName == null || ownerName.trim().equals("")) {
                ownerName = null;
            }

            Cell addressCell = row.getCell(10);
         
            if (addressCell != null && addressCell.getCellType() == CellType.STRING) {
                address = addressCell.getStringCellValue();
                System.out.println("address = " + address);
            }
            if (address == null || address.trim().equals("")) {
                address = null;
            }

            Cell standardCreditCell = row.getCell(11);
            if (standardCreditCell != null && standardCreditCell.getCellType() == CellType.NUMERIC) {
                standardCreditLimit = standardCreditCell.getNumericCellValue();

            }
            if (standardCreditLimit == null) {
                standardCreditLimit = 0.0;
            }

            Cell allowedCreditCell = row.getCell(12);
            if (allowedCreditCell != null && allowedCreditCell.getCellType() == CellType.NUMERIC) {
                allowedCreditLimit = allowedCreditCell.getNumericCellValue();

            }
            if (allowedCreditLimit == null) {
                allowedCreditLimit = 0.0;
            }

            Cell maxCreditCell = row.getCell(13);
            if (maxCreditCell != null && maxCreditCell.getCellType() == CellType.NUMERIC) {
                maxCreditLimit = maxCreditCell.getNumericCellValue();

            }
            if (maxCreditLimit == null) {
                maxCreditLimit = 0.0;
            }

            if (code.trim().equals("")) {
                continue;
            }

            if (collectingCentreName.trim().equals("")) {
                continue;
            }

            collectingCentre = collectingCentreController.findCollectingCentreByName(collectingCentreName);
//            if (collectingCentre != null) {
//                continue;
//            }
//            collectingCentre = collectingCentreController.findCollectingCentreByCode(code);
//            if (collectingCentre != null) {
//                continue;
//            }
            if (collectingCentre == null) {
                collectingCentre = new Institution();
            }
//            collectingCentre = new Institution();
            collectingCentre.setInstitutionType(InstitutionType.CollectingCentre);
            collectingCentre.setCode(code);
            collectingCentre.setName(collectingCentreName);
            if (withCommissionStatus) {
                collectingCentre.setCollectingCentrePaymentMethod(CollectingCentrePaymentMethod.FULL_PAYMENT_WITH_COMMISSION);
            } else {
                collectingCentre.setCollectingCentrePaymentMethod(CollectingCentrePaymentMethod.PAYMENT_WITHOUT_COMMISSION);
            }

            collectingCentre.setInactive(active);

//            Route r = routeController.findRouteByName(routeName)
//            if(r==null){
//                r = new Route();
//                r.setName(routeName);
//                r.setCreatedAt(new Date());
//                r.setCreater(sessionController.getLoggedUser());
//                routeController.save(r);
//            }
            collectingCentre.setRoute(route);
            collectingCentre.setChequePrintingName(collectingCentrePrintingName);
            collectingCentre.setPercentage(percentage);
            collectingCentre.setPhone(phone);
            collectingCentre.setEmail(email);
            collectingCentre.setOwnerName(ownerName);
            collectingCentre.setAddress(address);
            collectingCentre.setAllowedCredit(standardCreditLimit);
            collectingCentre.setAllowedCreditLimit(allowedCreditLimit);
            collectingCentre.setMaxCreditLimit(maxCreditLimit);

            collectingCentreController.save(collectingCentre);

            collectingCentresList.add(collectingCentre);
        }

        return collectingCentresList;
    }

    public void uploadItemFeesToUpdateFees() {
        itemFees = new ArrayList<>();
        if (file != null) {
            try ( InputStream inputStream = file.getInputStream()) {
                itemFees = replaceItemFeesFromExcel(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void uploadInvestigations() {
        List<Investigation> investigations;
        if (file != null) {
            try ( InputStream inputStream = file.getInputStream()) {
                readInvestigationsFromExcel(inputStream);
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
                Logger.getLogger(DataUploadBean.class
                        .getName()).log(Level.SEVERE, null, ex);
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

    private List<ItemFee> replaceItemFeesFromExcel(InputStream inputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();

        itemFees = new ArrayList<>();
        ItemFee itemFee;

//        // Assuming the first row contains headers, skip it
//        if (rowIterator.hasNext()) {
//            rowIterator.next();
//        }
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();

            itemFee = null;
            Long itemFeeId = null;
            Double feeValue = null;
            Double foreignerFeeValue = null;

            Cell itemFeeIdCell = row.getCell(0);
            if (itemFeeIdCell != null && itemFeeIdCell.getCellType() == CellType.NUMERIC) {
                double numericValue = itemFeeIdCell.getNumericCellValue();
                if (numericValue % 1 == 0) {
                    itemFeeId = (long) numericValue;
                } else {
                    continue;
                }
            } else {
                continue;
            }

            itemFee = itemFeeController.findItemFeeFromItemFeeId(itemFeeId);

            if (itemFee == null) {
                continue;
            }

            Cell feeCell = row.getCell(6);
            if (feeCell != null && feeCell.getCellType() == CellType.NUMERIC) {
                feeValue = feeCell.getNumericCellValue();
            }

            if (feeValue == null) {
                continue;
            }

            Cell ffeeCell = row.getCell(7);
            if (ffeeCell != null && ffeeCell.getCellType() == CellType.NUMERIC) {
                foreignerFeeValue = ffeeCell.getNumericCellValue();
            }

            if (foreignerFeeValue == null) {
                foreignerFeeValue = feeValue;
            }

            itemFee.setFee(feeValue);
            itemFee.setFfee(foreignerFeeValue);

            itemFeeManager.saveItemFee(itemFee);

            itemFees.add(itemFee);
        }

        return itemFees;
    }

    private List<Investigation> readInvestigationsFromExcel(InputStream inputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();

        List<Investigation> investigations = new ArrayList<>();

        // Assuming the first row contains headers, skip it
        if (rowIterator.hasNext()) {
            rowIterator.next();
        }

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();

            Category cat = null;
            Institution institution = null;
            Department department = null;
            InwardChargeType iwct = null;
            Sample sample = null;
            InvestigationTube investigationTube = null;
            Machine analyser = null;

            String name = null;
            String printingName = null;
            String fullName = null;
            String code = null;
            String categoryName = null;
            String institutionName = null;
            String departmentName = null;
            String inwardName = null;
            String sampleName = null;
            String containerName = null;
            String analyserName = null;

            Cell nameCell = row.getCell(0);
            if (nameCell != null && nameCell.getCellType() == CellType.STRING) {
                name = nameCell.getStringCellValue();
                if (name == null || name.trim().equals("")) {
                    continue;
                }

            }

            Cell printingNameCell = row.getCell(1);
            if (printingNameCell != null && printingNameCell.getCellType() == CellType.STRING) {
                printingName = printingNameCell.getStringCellValue();

            }
            if (printingName == null || printingName.trim().equals("")) {
                printingName = name;
            }

            Cell fullNameCell = row.getCell(2);
            if (fullNameCell != null && fullNameCell.getCellType() == CellType.STRING) {
                fullName = fullNameCell.getStringCellValue();
            }
            if (fullName == null || fullName.trim().equals("")) {
                fullName = name;
            }

            Cell codeCell = row.getCell(3);
            if (codeCell != null && codeCell.getCellType() == CellType.STRING) {
                code = codeCell.getStringCellValue();
            }
            if (code == null || code.trim().equals("")) {
                continue;
            }
            Cell categoryCell = row.getCell(4);
            if (categoryCell != null && categoryCell.getCellType() == CellType.STRING) {
                categoryName = categoryCell.getStringCellValue();
                cat = investigationCategoryController.findAndCreateCategoryByName(categoryName);
            }
            if (categoryName == null || categoryName.trim().equals("")) {
                continue;
            }

            Cell insCell = row.getCell(5);
            if (insCell != null && insCell.getCellType() == CellType.STRING) {
                institutionName = insCell.getStringCellValue();
                institution = institutionController.findAndSaveInstitutionByName(institutionName);
            }

            Cell deptCell = row.getCell(6);
            if (deptCell != null && deptCell.getCellType() == CellType.STRING) {
                departmentName = deptCell.getStringCellValue();
            }
            if (departmentName != null && !departmentName.trim().equals("")) {
                department = departmentController.findAndSaveDepartmentByName(departmentName);
            }

            Cell inwardCcCell = row.getCell(7);
            if (inwardCcCell != null && inwardCcCell.getCellType() == CellType.STRING) {
                inwardName = inwardCcCell.getStringCellValue();
            }
            if (inwardName != null && !inwardName.trim().equals("")) {
                iwct = enumController.getInaChargeType(inwardName);
            }
            if (iwct == null) {
                iwct = InwardChargeType.OtherCharges;
            }

            Cell sampleCell = row.getCell(8);
            if (sampleCell != null && sampleCell.getCellType() == CellType.STRING) {
                sampleName = sampleCell.getStringCellValue();
                sample = sampleController.findAndCreateSampleByName(sampleName);
            }

            if (sampleName == null || sampleName.trim().equals("")) {
                continue;
            }

            Cell containerCell = row.getCell(9);
            if (containerCell != null && containerCell.getCellType() == CellType.STRING) {
                containerName = containerCell.getStringCellValue();
                investigationTube = investigationTubeController.findAndCreateInvestigationTubeByName(containerName);
            }

            if (containerName == null || containerName.trim().equals("")) {
                continue;
            }

            Cell analyserCell = row.getCell(10);
            if (analyserCell != null && analyserCell.getCellType() == CellType.STRING) {
                analyserName = analyserCell.getStringCellValue();
                analyser = machineController.findAndCreateAnalyserByName(analyserName);
            }

            if (analyserName == null || analyserName.trim().equals("")) {
                continue;
            }

            Investigation investigation = new Investigation();
            investigation.setName(name);
            investigation.setPrintName(printingName);
            investigation.setFullName(fullName);
            investigation.setCode(code);
            investigation.setCategory(cat);
            investigation.setInstitution(institution);
            investigation.setDepartment(department);
            investigation.setInwardChargeType(iwct);
            investigation.setSample(sample);
            investigation.setInvestigationTube(investigationTube);
            investigation.setMachine(analyser);

            investigation.setCreater(sessionController.getLoggedUser());
            investigation.setCreatedAt(new Date());
            investigationController.save(investigation);
            investigations.add(investigation);

        }

        return investigations;
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

            Long id;
            String ampName;
            String code = null;
            String barcode = null;
            String vmpName;
            String manufacturerName = null;
            String importerName = null;
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
                if (amp == null) {
                    amp = new Amp();
                    amp.setName(ampName);
                    amp.setCreatedAt(new Date());
                    amp.setCreater(sessionController.getLoggedUser());
                }
            } else {
                continue;
            }

            Cell codeCell = row.getCell(2);
            if (codeCell != null && codeCell.getCellType() == CellType.STRING) {
                code = codeCell.getStringCellValue();
            }

            Cell barcodeCell = row.getCell(3);
            if (barcodeCell != null && barcodeCell.getCellType() == CellType.STRING) {
                barcode = barcodeCell.getStringCellValue();
            }

            Cell vmpCell = row.getCell(4);
            if (vmpCell != null && vmpCell.getCellType() == CellType.STRING) {
                vmpName = vmpCell.getStringCellValue();
                vmp = vmpController.findVmpByName(vmpName);
                if (vmp == null) {
                    continue;
                }
            } else {
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

            manufacturer = institutionController.getInstitutionByName(manufacturerName, InstitutionType.Manufacturer);
            importer = institutionController.getInstitutionByName(importerName, InstitutionType.Importer);

//            amp = new Amp();
            amp.setName(ampName);
            amp.setCode("amp_" + CommonController.nameToCode(ampName));

            if (vmp != null) {
                amp.setVmp(vmp);
                amp.setCategory(vmp.getCategory());
            }
            amp.setCode(code);
            amp.setBarcode(barcode);
            if (manufacturer != null) {
                amp.setManufacturer(manufacturer);
            }
            if (importer != null) {
                amp.setImporter(importer);
            }

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

        if (rowIterator.hasNext()) {
            rowIterator.next(); // Skip header row
        }

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Vmp vmp = new Vmp();
            Vtm vtm = null;
            Category dosageForm = null;
            MeasurementUnit strengthUnit = null;
            MeasurementUnit issueUnit = null;
            Long id;
            String vmpName = "";
            String vtmName;
            String dosageFormName;
            String strengthUnitName;
            String issueUnitName;
            Double strengthUnitsPerIssueUnit = null;
            Double minimumIssueQuantity = null;
            Double issueMultipliesQuantity = null;

            // Read ID (Optional)
            Cell idCell = row.getCell(0);
            if (idCell != null && idCell.getCellType() == CellType.NUMERIC) {
                id = (long) idCell.getNumericCellValue();
                vmp.setItemId(id);
            }

            // Read VMP Name
            Cell nameCell = row.getCell(1);
            if (nameCell != null && nameCell.getCellType() == CellType.STRING) {
                vmpName = nameCell.getStringCellValue();
                if (vmpName != null && !vmpName.trim().isEmpty()) {
                    vmp = vmpController.findVmpByName(vmpName);
                    if (vmp != null) {
                        vmps.add(vmp);
                        continue;
                    }
                } else {
                    continue;
                }
            }

            // Read VTM Name
            Cell vtmCell = row.getCell(2);
            if (vtmCell != null && vtmCell.getCellType() == CellType.STRING) {
                vtmName = vtmCell.getStringCellValue();
                vtm = vtmController.findAndSaveVtmByName(vtmName);
            }

            // Read Dosage Form
            Cell dosageFormCell = row.getCell(3);
            if (dosageFormCell != null && dosageFormCell.getCellType() == CellType.STRING) {
                dosageFormName = dosageFormCell.getStringCellValue();
                dosageForm = categoryController.findAndCreateCategoryByName(dosageFormName);
            }

            // Read Strength Unit
            Cell strengthUnitCell = row.getCell(4);
            if (strengthUnitCell != null && strengthUnitCell.getCellType() == CellType.STRING) {
                strengthUnitName = strengthUnitCell.getStringCellValue();
                strengthUnit = measurementUnitController.findAndSaveMeasurementUnitByName(strengthUnitName);
            }

            // Read Issue Unit
            Cell issueUnitCell = row.getCell(5);
            if (issueUnitCell != null && issueUnitCell.getCellType() == CellType.STRING) {
                issueUnitName = issueUnitCell.getStringCellValue();
                issueUnit = measurementUnitController.findAndSaveMeasurementUnitByName(issueUnitName);
            }

            // Read Strength Units per Issue Unit
            Cell strengthUnitsPerIssueUnitCell = row.getCell(6);
            if (strengthUnitsPerIssueUnitCell != null && strengthUnitsPerIssueUnitCell.getCellType() == CellType.NUMERIC) {
                strengthUnitsPerIssueUnit = strengthUnitsPerIssueUnitCell.getNumericCellValue();
            }

            // Read Min Issue Quantity
            Cell minIQtyCell = row.getCell(7);
            if (minIQtyCell != null && minIQtyCell.getCellType() == CellType.NUMERIC) {
                minimumIssueQuantity = minIQtyCell.getNumericCellValue();
            }

            // Read Issue Multiplies Quantity
            Cell issueMultipliesQuentityCell = row.getCell(8);
            if (issueMultipliesQuentityCell != null && issueMultipliesQuentityCell.getCellType() == CellType.NUMERIC) {
                issueMultipliesQuantity = issueMultipliesQuentityCell.getNumericCellValue();
            }

            // Check for null values and create VMP
            if (vtm != null && dosageForm != null && strengthUnitsPerIssueUnit != null && strengthUnit != null
                    && minimumIssueQuantity != null && issueMultipliesQuantity != null) {
                vmp = vmpController.createVmp(vmpName,
                        vtm,
                        dosageForm,
                        strengthUnit,
                        issueUnit,
                        strengthUnitsPerIssueUnit,
                        minimumIssueQuantity, issueMultipliesQuantity);
                vmps.add(vmp);
            }
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

    public StreamedContent getTemplateForItemWithFeeUpload() {
        try {
            createTemplateForItemWithFeeUpload();
        } catch (IOException e) {
            // Handle IOException
        }
        return templateForItemWithFeeUpload;
    }

    public void createTemplateForItemWithFeeUpload() throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();

        // Creating the first sheet for data entry
        XSSFSheet dataSheet = workbook.createSheet("Data Entry");

        // Creating the second sheet for institution list
        XSSFSheet institutionSheet = workbook.createSheet("Institutions");

        // Fetching institutions and adding to the institution sheet
        List<Institution> institutions = institutionController.getItems();
        for (int i = 0; i < institutions.size(); i++) {
            Row row = institutionSheet.createRow(i);
            Cell cell = row.createCell(0);
            cell.setCellValue(institutions.get(i).getName());
        }

        // Hiding the institution sheet
//        workbook.setSheetHidden(workbook.getSheetIndex("Institutions"), true);
        // Create header row in data sheet
        Row headerRow = dataSheet.createRow(0);
        String[] columnHeaders = {"Name", "Printing Name", "Full Name", "Code", "Category", "Institution", "Department", "Inward Charge Type", "Item Type", "Hospital Fee", "Collecting Centre Fee"};
        for (int i = 0; i < columnHeaders.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columnHeaders[i]);
        }

        // Setting data validation for institution column in data sheet
        XSSFDataValidationHelper validationHelper = new XSSFDataValidationHelper(dataSheet);
        DataValidationConstraint constraint = validationHelper.createFormulaListConstraint("Institutions!$A$1:$A$" + institutions.size());

        CellRangeAddressList addressList = new CellRangeAddressList(1, 65535, 5, 5); // Assuming column F is for institutions
        XSSFDataValidation dataValidation = (XSSFDataValidation) validationHelper.createValidation(constraint, addressList);

        dataValidation.setShowErrorBox(true);
        dataSheet.addValidationData(dataValidation);

        // Auto-size columns for aesthetics
        for (int i = 0; i < columnHeaders.length; i++) {
            dataSheet.autoSizeColumn(i);
        }

        // Write the output to a byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        // Set the downloading file
        templateForItemWithFeeUpload = DefaultStreamedContent.builder()
                .name("template_for_item_fee_upload.xlsx")
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .stream(() -> inputStream)
                .build();
    }

    public StreamedContent getTemplateForCollectingCentreUpload() {
        try {
            createTemplateForCollectingCentreUpload();
        } catch (IOException e) {
            // Handle IOException
        }
        return templateForCollectingCentreUpload;
    }

    public void createTemplateForCollectingCentreUpload() throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();

        // Creating the first sheet for data entry
        XSSFSheet dataSheet = workbook.createSheet("Collecting Centres");

        // Create header row in data sheet
        Row headerRow = dataSheet.createRow(0);
        String[] columnHeaders = {"Code", "Agent Name", "Agent Printing Name", "Active", "With Commission Status", "Route Name", "Percentage", "Agent Contact No", "Email Address", "Owner Name", "Agent Address", "Standard Credit Limit", "Allowed Credit Limit", "Max Credit Limit"};
        for (int i = 0; i < columnHeaders.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columnHeaders[i]);
        }

        // Auto-size columns for aesthetics
        for (int i = 0; i < columnHeaders.length; i++) {
            dataSheet.autoSizeColumn(i);
        }

        // Write the output to a byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        // Set the downloading file
        templateForCollectingCentreUpload = DefaultStreamedContent.builder()
                .name("template_for_collecting_centres_upload.xlsx")
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .stream(() -> inputStream)
                .build();
    }

    public String getOutputString() {
        return outputString;
    }

    public void setOutputString(String outputString) {
        this.outputString = outputString;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public List<ItemFee> getItemFees() {
        return itemFees;
    }

    public void setItemFees(List<ItemFee> itemFees) {
        this.itemFees = itemFees;
    }

    public List<Institution> getCollectingCentres() {
        return collectingCentres;
    }

    public void setCollectingCentres(List<Institution> collectingCentres) {
        this.collectingCentres = collectingCentres;
    }

}
