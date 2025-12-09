package com.divudi.bean.lab;

import javax.inject.Named;
import com.divudi.core.data.InvestigationItemType;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.entity.lab.InvestigationItem;
import com.divudi.core.facade.InvestigationItemFacade;

import java.io.IOException;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.model.file.UploadedFile;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import java.util.Date;
import java.util.Iterator;
import com.divudi.core.data.InvestigationItemValueType;
import com.divudi.core.entity.Item;
import com.divudi.bean.common.EnumController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.emr.DataUploadController;
import com.divudi.core.data.CssFontStyle;
import com.divudi.core.data.CssTextAlign;
import com.divudi.core.data.CssTextDecoration;
import com.divudi.core.data.CssVerticalAlign;
import com.divudi.core.facade.ReportItemFacade;
import com.divudi.core.util.CommonFunctions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Named
@SessionScoped
public class LabReportExportImportController implements Serializable {

// <editor-fold defaultstate="collapsed" desc="Ejbs">
    @EJB
    InvestigationItemFacade investigationItemFacade;
    @EJB
    ReportItemFacade reportItemFacade;
    // </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    InvestigationItemController investigationItemController;
    @Inject
    private DataUploadController dataUploadController;
    @Inject
    SessionController sessionController;
    @Inject
    EnumController enumController;
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Variables">
    private Investigation current;
    private List<InvestigationItem> successItems;
    private List<InvestigationItem> uploadedSuccessItems;
    private List<InvestigationItem> uploadedRejectedItems;
    private UploadedFile file;
    private StreamedContent downloadingExcel;

// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Navigation Method">
// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Methods">
    public LabReportExportImportController() {
    }

    public List<InvestigationItem> fillInvestigationComporentItems(Investigation investigation) {

        List<InvestigationItem> investigationComporent = new ArrayList<>();

        if (investigation == null || investigation.getId() == null) {
            return investigationComporent;
        }

        List<InvestigationItemType> l = new ArrayList<>();
        l.add(InvestigationItemType.Label);
        l.add(InvestigationItemType.Value);
        l.add(InvestigationItemType.Flag);
        l.add(InvestigationItemType.Calculation);
        l.add(InvestigationItemType.Css);
        l.add(InvestigationItemType.Html);
        l.add(InvestigationItemType.DynamicLabel);
        l.add(InvestigationItemType.Investigation);
        l.add(InvestigationItemType.AntibioticList);
        l.add(InvestigationItemType.Template);
        l.add(InvestigationItemType.Barcode);
        l.add(InvestigationItemType.BarcodeVertical);
        l.add(InvestigationItemType.Image);
        l.add(InvestigationItemType.ReportImage);

        investigationComporent = listInvestigationItemsFilteredByItemTypes(investigation, l);
        return investigationComporent;
    }

    public List<InvestigationItem> listInvestigationItemsFilteredByItemTypes(Investigation ix, List<InvestigationItemType> types) {
        List<InvestigationItem> tis = new ArrayList<>();
        if (ix != null) {
            String temSql;
            temSql = "SELECT i FROM InvestigationItem i "
                    + " where i.retired=false "
                    + " and i.item=:item "
                    + " and i.ixItemType in :types "
                    + " order by i.riTop, i.riLeft";
            Map m = new HashMap();
            m.put("item", ix);
            m.put("types", types);
            tis = investigationItemFacade.findByJpql(temSql, m);
        }
        return tis;
    }

    public StreamedContent export() {
        if (getCurrent() == null) {
            JsfUtil.addErrorMessage("Pleace select Investigations ");
            return null;
        }
        List<InvestigationItem> items = investigationItemController.getUserChangableItems();

        if (items == null) {
            JsfUtil.addErrorMessage("This Investigation has no InvestigationItem");
            return null;
        }

        try {
            downloadingExcel = exportInvestigationItemsToExcel(items);
        } catch (IOException e) {
            // Handle IOException
        }
        JsfUtil.addSuccessMessage("Successfuly Download !");
        return downloadingExcel;

    }

    public void importFormat() {
        if (getCurrent() == null) {
            JsfUtil.addErrorMessage("Pleace select Investigations ");
            return;
        }

        if (getFile() == null) {
            JsfUtil.addErrorMessage("Pleace select the File ");
            return;
        }

        System.out.println("File = " + getFile().getFileName());
        System.out.println("Type = " + getFile().getContentType());

        if (file != null) {
            try (InputStream inputStream = file.getInputStream()) {
                readAndUploadReportItemFromExcel(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        JsfUtil.addSuccessMessage("Successfuly Uploaed !");
    }

    public void makeNull() {
        current = null;
        file = null;
        uploadedRejectedItems = null;
        uploadedSuccessItems = null;
        successItems = null;
    }

// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Export">
    private String defaultIfNullOrEmpty(String value, String defaultValue) {
        return (value == null || value.trim().isEmpty()) ? defaultValue : value;
    }

    public StreamedContent exportInvestigationItemsToExcel(List<InvestigationItem> investigationItems) throws IOException { // Define a predefined directory (e.g., user's desktop)
//        String userHome = System.getProperty("user.home");
//        String defaultDirectory = userHome + File.separator + "Desktop"; // Change to any other directory if needed
//        String fullFilePath = defaultDirectory + File.separator + fileName;
//
//        // Ensure the directory exists
//        File file = new File(fullFilePath);
//        file.getParentFile().mkdirs(); // Create parent directories if they don't exist
        //        String userHome = System.getProperty("user.home");
//        String defaultDirectory = userHome + File.separator + "Desktop"; // Change to any other directory if needed
//        String fullFilePath = defaultDirectory + File.separator + fileName;
//
//        // Ensure the directory exists
//        File file = new File(fullFilePath);
//        file.getParentFile().mkdirs(); // Create parent directories if they don't exist

        StreamedContent excelSc;

        // Create a new workbook and a sheet
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("InvestigationItems");

        // Define Header Row
        String[] headers = {
            "Name", "IxItemType", "IxItemValueType", "HtmlText", "RiLeft", "RiTop", "RiWidth", "RiHeight",
            "HtPix", "WtPix", "CssTextAlign", "CssVerticalAlign", "CssFontFamily", "RiFontSize",
            "CssFontStyle", "CssFontWeight", "CssTextDecoration", "CustomCss", "CssBackColor", "CssColor",
            "Automated"
        };

        // Create the header row
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        successItems = new ArrayList();

        // Populate data rows
        int rowNum = 1;
        for (InvestigationItem item : investigationItems) {
            Row row = sheet.createRow(rowNum++);
            System.out.println("Item = " + item.getId() + " - " + item);
            row.createCell(0).setCellValue(defaultIfNullOrEmpty(item.getName(), ""));
            row.createCell(1).setCellValue(defaultIfNullOrEmpty(item.getIxItemType() != null ? item.getIxItemType().toString() : null, ""));
            row.createCell(2).setCellValue(defaultIfNullOrEmpty(item.getIxItemValueType() != null ? item.getIxItemValueType().toString() : null, ""));
            row.createCell(3).setCellValue(defaultIfNullOrEmpty(item.getHtmltext(), ""));
            row.createCell(4).setCellValue(item.getRiLeft());
            row.createCell(5).setCellValue(item.getRiTop());
            row.createCell(6).setCellValue(item.getRiWidth());
            row.createCell(7).setCellValue(item.getRiHeight());
            row.createCell(8).setCellValue(item.getHtPix());
            row.createCell(9).setCellValue(item.getWtPix());
            row.createCell(10).setCellValue(defaultIfNullOrEmpty(item.getCssTextAlign() != null ? item.getCssTextAlign().toString() : null, ""));
            row.createCell(11).setCellValue(defaultIfNullOrEmpty(item.getCssVerticalAlign() != null ? item.getCssVerticalAlign().toString() : null, ""));
            row.createCell(12).setCellValue(defaultIfNullOrEmpty(item.getCssFontFamily(), ""));
            row.createCell(13).setCellValue(item.getRiFontSize());
            row.createCell(14).setCellValue(defaultIfNullOrEmpty(item.getCssFontStyle() != null ? item.getCssFontStyle().toString() : null, ""));
            row.createCell(15).setCellValue(defaultIfNullOrEmpty(item.getCssFontWeight(), ""));
            row.createCell(16).setCellValue(defaultIfNullOrEmpty(item.getCssTextDecoration() != null ? item.getCssTextDecoration().toString() : null, ""));
            row.createCell(17).setCellValue(defaultIfNullOrEmpty(item.getCustomCss(), ""));
            row.createCell(18).setCellValue(defaultIfNullOrEmpty(item.getCssBackColor(), ""));
            row.createCell(19).setCellValue(defaultIfNullOrEmpty(item.getCssColor(), ""));
            row.createCell(20).setCellValue(item.isAutomated());

            successItems.add(item);
        }

        // Resize columns to fit content
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        String fileName = getCurrent().getName() + ".xlsx";
        // Set the downloading file
        excelSc = DefaultStreamedContent.builder()
                .name(fileName)
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .stream(() -> inputStream)
                .build();

        return excelSc;
    }

// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Import">
    private List<Item> readAndUploadReportItemFromExcel(InputStream inputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();

        uploadedRejectedItems = new ArrayList<>();
        uploadedSuccessItems = new ArrayList<>();

        if (rowIterator.hasNext()) {
            rowIterator.next();
        }

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();

            InvestigationItemType investigationItemType = null;
            InvestigationItemValueType investigationItemValueType = null;
            CssVerticalAlign verticalAlign = null;
            CssTextDecoration textDecoration = null;
            CssFontStyle fontStyle = null;
            CssTextAlign textAlign = null;

            String name = null;
            String ixItemType = null;
            String ixItemValueType = null;
            String htmlText = null;

            double riLeft = 0.0;
            double riTop = 0.0;
            double riWidth = 0.0;
            double riHeight = 0.0;
            double htPix = 0.0;
            double wtPix = 0.0;

            String cssTextAlign = null;
            String cssVerticalAlign = null;
            String cssFontFamily = null;

            double riFontSize = 0.0;

            String cssFontStyle = null;
            String cssFontWeight = null;
            String cssTextDecoration = null;
            String customCss = null;
            String cssBackColor = null;
            String cssColor = null;

            // Left Setting
            Cell nameCell = row.getCell(0);
            if (nameCell != null && nameCell.getCellType() == CellType.STRING) {
                name = nameCell.getStringCellValue();
                if (name == null || name.trim().equals("")) {
                    continue;
                }
            }

            // ItemType Setting
            Cell ixItemTypeCell = row.getCell(1);
            if (ixItemTypeCell != null && ixItemTypeCell.getCellType() == CellType.STRING) {
                ixItemType = ixItemTypeCell.getStringCellValue();
            }
            if (ixItemType != null && !ixItemType.trim().equals("")) {
                investigationItemType = enumController.getInvestigationItemType(ixItemType);
            }
            if (ixItemType == null) {
                investigationItemType = InvestigationItemType.Label;
            }

            // ItemValueType Setting
            Cell ixItemValueTypeCell = row.getCell(2);
            if (ixItemValueTypeCell != null && ixItemValueTypeCell.getCellType() == CellType.STRING) {
                ixItemValueType = ixItemValueTypeCell.getStringCellValue();
            }
            if (ixItemValueType != null && !ixItemValueType.trim().equals("")) {
                investigationItemValueType = enumController.getInvestigationItemValueType(ixItemValueType);
            } else if (ixItemValueType == null) {
                investigationItemValueType = InvestigationItemValueType.Varchar;
            }

            // HtmlText Setting
            Cell htmlTextCell = row.getCell(3);
            if (htmlTextCell != null && htmlTextCell.getCellType() == CellType.STRING) {
                htmlText = htmlTextCell.getStringCellValue();
            }

            // Left Setting
            Cell riLeftCell = row.getCell(4);
            if (null != riLeftCell.getCellType()) {
                switch (riLeftCell.getCellType()) {
                    case NUMERIC:
                        riLeft = riLeftCell.getNumericCellValue();
                        break;
                    case FORMULA:
                        // If it's a formula, evaluate it
                        Workbook wb = riLeftCell.getSheet().getWorkbook();
                        CreationHelper createHelper = wb.getCreationHelper();
                        FormulaEvaluator evaluator = createHelper.createFormulaEvaluator();
                        CellValue cellValue = evaluator.evaluate(riLeftCell);
                        // Check the type of the evaluated value
                        if (cellValue.getCellType() == CellType.NUMERIC) {
                            riLeft = cellValue.getNumberValue();
                        } else {
                            // Handle other types if needed
                        }
                        break;
                    case STRING:
                        String strriLeft = riLeftCell.getStringCellValue();
                        riLeft = CommonFunctions.stringToDouble(strriLeft);
                        break;
                    default:
                        break;
                }
            }

            // TOP Setting
            Cell riTopCell = row.getCell(5);
            if (null != riTopCell.getCellType()) {
                switch (riTopCell.getCellType()) {
                    case NUMERIC:
                        riTop = riTopCell.getNumericCellValue();
                        break;
                    case FORMULA:
                        // If it's a formula, evaluate it
                        Workbook wb = riTopCell.getSheet().getWorkbook();
                        CreationHelper createHelper = wb.getCreationHelper();
                        FormulaEvaluator evaluator = createHelper.createFormulaEvaluator();
                        CellValue cellValue = evaluator.evaluate(riTopCell);
                        // Check the type of the evaluated value
                        if (cellValue.getCellType() == CellType.NUMERIC) {
                            riTop = cellValue.getNumberValue();
                        } else {
                            // Handle other types if needed
                        }
                        break;
                    case STRING:
                        String strriTop = riTopCell.getStringCellValue();
                        riTop = CommonFunctions.stringToDouble(strriTop);
                        break;
                    default:
                        break;
                }
            }

            // Width Setting
            Cell riWidthCell = row.getCell(6);
            if (null != riWidthCell.getCellType()) {
                switch (riWidthCell.getCellType()) {
                    case NUMERIC:
                        riWidth = riWidthCell.getNumericCellValue();
                        break;
                    case FORMULA:
                        // If it's a formula, evaluate it
                        Workbook wb = riWidthCell.getSheet().getWorkbook();
                        CreationHelper createHelper = wb.getCreationHelper();
                        FormulaEvaluator evaluator = createHelper.createFormulaEvaluator();
                        CellValue cellValue = evaluator.evaluate(riWidthCell);
                        // Check the type of the evaluated value
                        if (cellValue.getCellType() == CellType.NUMERIC) {
                            riWidth = cellValue.getNumberValue();
                        } else {
                            // Handle other types if needed
                        }
                        break;
                    case STRING:
                        String strWidth = riWidthCell.getStringCellValue();
                        riWidth = CommonFunctions.stringToDouble(strWidth);
                        break;
                    default:
                        break;
                }
            }

            // Height Setting
            Cell heightCell = row.getCell(7);
            if (null != heightCell.getCellType()) {
                switch (heightCell.getCellType()) {
                    case NUMERIC:
                        riHeight = heightCell.getNumericCellValue();
                        break;
                    case FORMULA:
                        // If it's a formula, evaluate it
                        Workbook wb = heightCell.getSheet().getWorkbook();
                        CreationHelper createHelper = wb.getCreationHelper();
                        FormulaEvaluator evaluator = createHelper.createFormulaEvaluator();
                        CellValue cellValue = evaluator.evaluate(heightCell);
                        // Check the type of the evaluated value
                        if (cellValue.getCellType() == CellType.NUMERIC) {
                            riHeight = cellValue.getNumberValue();
                        } else {
                            // Handle other types if needed
                        }
                        break;
                    case STRING:
                        String strHeight = heightCell.getStringCellValue();
                        riHeight = CommonFunctions.stringToDouble(strHeight);
                        break;
                    default:
                        break;
                }
            }

            // HtPix Setting
            Cell htPixCell = row.getCell(8);
            if (null != htPixCell.getCellType()) {
                switch (htPixCell.getCellType()) {
                    case NUMERIC:
                        htPix = htPixCell.getNumericCellValue();
                        break;
                    case FORMULA:
                        // If it's a formula, evaluate it
                        Workbook wb = htPixCell.getSheet().getWorkbook();
                        CreationHelper createHelper = wb.getCreationHelper();
                        FormulaEvaluator evaluator = createHelper.createFormulaEvaluator();
                        CellValue cellValue = evaluator.evaluate(htPixCell);
                        // Check the type of the evaluated value
                        if (cellValue.getCellType() == CellType.NUMERIC) {
                            htPix = cellValue.getNumberValue();
                        } else {
                            // Handle other types if needed
                        }
                        break;
                    case STRING:
                        String strHtPix = htPixCell.getStringCellValue();
                        htPix = CommonFunctions.stringToDouble(strHtPix);
                        break;
                    default:
                        break;
                }
            }

            // WtPix Setting
            Cell wtPixCell = row.getCell(9);
            if (wtPixCell.getCellType() == CellType.NUMERIC) {
                wtPix = wtPixCell.getNumericCellValue();
            } else if (wtPixCell.getCellType() == CellType.FORMULA) {
                // If it's a formula, evaluate it
                Workbook wb = wtPixCell.getSheet().getWorkbook();
                CreationHelper createHelper = wb.getCreationHelper();
                FormulaEvaluator evaluator = createHelper.createFormulaEvaluator();
                CellValue cellValue = evaluator.evaluate(wtPixCell);

                // Check the type of the evaluated value
                if (cellValue.getCellType() == CellType.NUMERIC) {
                    wtPix = cellValue.getNumberValue();
                } else {
                    // Handle other types if needed
                }
            } else if (htPixCell.getCellType() == CellType.STRING) {
                String strHtPix = wtPixCell.getStringCellValue();
                wtPix = CommonFunctions.stringToDouble(strHtPix);
            }

            // CssTextAlign Setting
            Cell cssTextAlignCell = row.getCell(10);
            if (cssTextAlignCell != null && cssTextAlignCell.getCellType() == CellType.STRING) {
                cssTextAlign = cssTextAlignCell.getStringCellValue();
            }
            if (cssTextAlign != null && !cssTextAlign.trim().equals("")) {
                textAlign = enumController.getCssTextAlign(cssTextAlign);
            }
            if (cssTextAlign == null) {
                textAlign = CssTextAlign.Left;
            }

            // CssVerticalAlign Setting
            Cell cssVerticalAlignCell = row.getCell(11);
            if (cssVerticalAlignCell != null && cssVerticalAlignCell.getCellType() == CellType.STRING) {
                cssVerticalAlign = cssVerticalAlignCell.getStringCellValue();
            }
            if (cssVerticalAlign != null && !cssVerticalAlign.trim().equals("")) {
                verticalAlign = enumController.getCssVerticalAlign(cssVerticalAlign);
            }
            if (cssVerticalAlign == null) {
                verticalAlign = CssVerticalAlign.Baseline;
            }

            // CssFontFamily Setting
            Cell cssFontFamilyCell = row.getCell(12);
            if (cssFontFamilyCell != null && cssFontFamilyCell.getCellType() == CellType.STRING) {
                cssFontFamily = cssFontFamilyCell.getStringCellValue();
            }

            // Font Size Setting
            Cell riFontSizeCell = row.getCell(13);
            if (null != riFontSizeCell.getCellType()) {
                switch (riFontSizeCell.getCellType()) {
                    case NUMERIC:
                        riFontSize = riFontSizeCell.getNumericCellValue();
                        break;
                    case FORMULA:
                        // If it's a formula, evaluate it
                        Workbook wb = riFontSizeCell.getSheet().getWorkbook();
                        CreationHelper createHelper = wb.getCreationHelper();
                        FormulaEvaluator evaluator = createHelper.createFormulaEvaluator();
                        CellValue cellValue = evaluator.evaluate(riFontSizeCell);
                        // Check the type of the evaluated value
                        if (cellValue.getCellType() == CellType.NUMERIC) {
                            riFontSize = cellValue.getNumberValue();
                        } else {
                            // Handle other types if needed
                        }
                        break;
                    case STRING:
                        String strRiFontSize = riFontSizeCell.getStringCellValue();
                        riFontSize = CommonFunctions.stringToDouble(strRiFontSize);
                        break;
                    default:
                        break;
                }
            }

            // CssFontStyle Setting
            Cell cssFontStyleCell = row.getCell(14);
            if (cssFontStyleCell != null && cssFontStyleCell.getCellType() == CellType.STRING) {
                cssFontStyle = cssFontStyleCell.getStringCellValue();
            }
            if (cssFontStyle != null && !cssFontStyle.trim().equals("")) {
                fontStyle = enumController.getCssFontStyle(cssFontStyle);
            }
            if (cssFontStyle == null) {
                fontStyle = CssFontStyle.Normal;
            }

            // CssFontWeight Setting
            Cell cssFontWeightCell = row.getCell(15);
            if (cssFontWeightCell != null && cssFontWeightCell.getCellType() == CellType.STRING) {
                cssFontWeight = cssFontWeightCell.getStringCellValue();
            }

            // CssTextDecoration Setting
            Cell cssTextDecorationCell = row.getCell(16);
            if (cssTextDecorationCell != null && cssTextDecorationCell.getCellType() == CellType.STRING) {
                cssTextDecoration = cssTextDecorationCell.getStringCellValue();
            }
            if (cssTextDecoration != null && !cssTextDecoration.trim().equals("")) {
                textDecoration = enumController.getCssTextDecoration(cssTextDecoration);
            }
            if (cssTextDecoration == null) {
                textDecoration = CssTextDecoration.none;
            }

            // CustomCss Setting
            Cell customCssCell = row.getCell(17);
            if (customCssCell != null && customCssCell.getCellType() == CellType.STRING) {
                customCss = customCssCell.getStringCellValue();
            }

            // CssBackColor Setting
            Cell cssBackColorCell = row.getCell(18);
            if (cssBackColorCell != null && cssBackColorCell.getCellType() == CellType.STRING) {
                cssFontFamily = cssBackColorCell.getStringCellValue();
            }

            // CssColor Setting
            Cell cssColorCell = row.getCell(19);
            if (cssColorCell != null && cssColorCell.getCellType() == CellType.STRING) {
                cssColor = cssColorCell.getStringCellValue();
            }

//            System.out.println("-------------------------");
//            System.out.println("row : " + row.getRowNum());
//            System.out.println("name: " + name);
//            System.out.println("ixItemType: " + ixItemType);
//            System.out.println("ixItemValueType: " + ixItemValueType);
//            System.out.println("htmlText: " + htmlText);
//
//            System.out.println("riLeft: " + riLeft);
//            System.out.println("riTop: " + riTop);
//            System.out.println("riWidth: " + riWidth);
//            System.out.println("riHeight: " + riHeight);
//            System.out.println("htPix: " + htPix);
//            System.out.println("wtPix: " + wtPix);
//
//            System.out.println("cssTextAlign: " + cssTextAlign);
//            System.out.println("cssVerticalAlign: " + cssVerticalAlign);
//            System.out.println("cssFontFamily: " + cssFontFamily);
//
//            System.out.println("riFontSize: " + riFontSize);
//
//            System.out.println("cssFontStyle: " + cssFontStyle);
//            System.out.println("cssFontWeight: " + cssFontWeight);
//            System.out.println("cssTextDecoration: " + cssTextDecoration);
//            System.out.println("customCss: " + customCss);
//            System.out.println("cssBackColor: " + cssBackColor);
//            System.out.println("cssColor: " + cssColor);
//
//            System.out.println("***************************");
//            System.out.println("investigationItemType: " + investigationItemType);
//            System.out.println("investigationItemValueType: " + investigationItemValueType);
//            System.out.println("verticalAlign: " + verticalAlign);
//            System.out.println("textDecoration: " + textDecoration);
//            System.out.println("fontStyle: " + fontStyle);
//            System.out.println("textAlign: " + textAlign);
//            System.out.println("***************************");
//
//            System.out.println("-------------------------");
            //Create New InvestigationItem for Current Investigation
            InvestigationItem newItem = new InvestigationItem();
            newItem.setName(name);
            newItem.setItem(getCurrent());
            newItem.setIxItemType(investigationItemType);
            newItem.setIxItemValueType(investigationItemValueType);
            newItem.setHtmltext(htmlText);
            newItem.setRiLeft(riLeft);
            newItem.setRiTop(riTop);
            newItem.setRiWidth(riWidth);
            newItem.setRiHeight(riHeight);
            newItem.setHtPix(htPix);
            newItem.setWtPix(wtPix);
            newItem.setCssTextAlign(textAlign);
            newItem.setCssVerticalAlign(verticalAlign);
            newItem.setCssFontFamily(cssFontFamily);
            newItem.setRiFontSize(riFontSize);
            newItem.setCssFontStyle(fontStyle);
            newItem.setCssFontWeight(cssFontWeight);
            newItem.setCssTextDecoration(textDecoration);
            newItem.setCustomCss(customCss);
            newItem.setCssBackColor(cssBackColor);
            newItem.setCssColor(cssColor);

            newItem.setCreatedAt(new Date());
            newItem.setCreater(sessionController.getLoggedUser());

            reportItemFacade.edit(newItem);
            uploadedSuccessItems.add(newItem);

        }
        return null;
    }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Getter & Setter">
    public Investigation getCurrent() {
        return current;
    }

    public void setCurrent(Investigation current) {
        this.current = current;
    }

    public List<InvestigationItem> getSuccessItems() {
        return successItems;
    }

    public void setSuccessItems(List<InvestigationItem> successItems) {
        this.successItems = successItems;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public List<InvestigationItem> getUploadedSuccessItems() {
        return uploadedSuccessItems;
    }

    public void setUploadedSuccessItems(List<InvestigationItem> uploadedSuccessItems) {
        this.uploadedSuccessItems = uploadedSuccessItems;
    }

    public List<InvestigationItem> getUploadedRejectedItems() {
        return uploadedRejectedItems;
    }

    public void setUploadedRejectedItems(List<InvestigationItem> uploadedRejectedItems) {
        this.uploadedRejectedItems = uploadedRejectedItems;
    }

    public DataUploadController getDataUploadController() {
        return dataUploadController;
    }

    public void setDataUploadController(DataUploadController dataUploadController) {
        this.dataUploadController = dataUploadController;
    }
// </editor-fold>

    public StreamedContent getDownloadingExcel() {
        return downloadingExcel;
    }

    public void setDownloadingExcel(StreamedContent downloadingExcel) {
        this.downloadingExcel = downloadingExcel;
    }

}
