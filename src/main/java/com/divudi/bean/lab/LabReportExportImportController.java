package com.divudi.bean.lab;

import javax.inject.Named;
import com.divudi.data.InvestigationItemType;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.InvestigationItem;
import com.divudi.facade.InvestigationItemFacade;
import java.io.File;
import java.io.FileOutputStream;
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
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    InvestigationItemController investigationItemController;
// </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Navigation Method">

// </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Variables">
    private Investigation current;
    private List<InvestigationItem> successItems;

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

    public void export() throws IOException {
        System.out.println("current = " + getCurrent().getName());
        List<InvestigationItem> items = investigationItemController.getUserChangableItems();
        System.out.println("Items = " + items.size());
        exportInvestigationItemsToExcel(items);

        System.out.println("Done");

    }

// </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Export">
    private String defaultIfNullOrEmpty(String value, String defaultValue) {
        return (value == null || value.trim().isEmpty()) ? defaultValue : value;
    }

    public void exportInvestigationItemsToExcel(List<InvestigationItem> investigationItems) throws IOException {
        // Define a predefined directory (e.g., user's desktop)
        System.out.println("Investigation Items = " + investigationItems.size());
        String userHome = System.getProperty("user.home");
        String defaultDirectory = userHome + File.separator + "Desktop"; // Change to any other directory if needed
        String fileName = getCurrent().getName().toString()+".xlsx";
        String fullFilePath = defaultDirectory + File.separator + fileName;

        // Ensure the directory exists
        File file = new File(fullFilePath);
        file.getParentFile().mkdirs(); // Create parent directories if they don't exist

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
            System.out.println("success " + item.getName() + "Added");
        }

        // Resize columns to fit content
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write the workbook to the default file path
        System.out.println("Attempting to write to: " + fullFilePath);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            workbook.write(outputStream);
            System.out.println("Excel file created successfully at " + fullFilePath);
        } catch (IOException e) {
            System.out.println("Error writing Excel file: " + e.getMessage());
            e.printStackTrace();
        } finally {
            workbook.close();
        }
    }

// </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Import">
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Getter & Setter">
    public Investigation getCurrent() {
        current = investigationItemController.getCurrentInvestigation();
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

// </editor-fold>
}
