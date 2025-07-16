package com.divudi.service;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.HistoricalRecord;
import com.divudi.core.entity.Upload;
import com.divudi.core.data.dto.ItemMovementSummaryDTO;
import com.divudi.core.facade.HistoricalRecordFacade;
import com.divudi.core.facade.UploadFacade;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Stateless
public class PharmacyAsyncReportService {

    @EJB
    private BillService billService;
    @EJB
    private UploadFacade uploadFacade;
    @EJB
    private HistoricalRecordFacade historicalRecordFacade;
    @Inject
    SessionController sessionController;

    @Asynchronous
    public void generateAllItemMovementReport(HistoricalRecord hr) {
        if (hr == null) {
            return;
        }
        hr.setStartedAt(new Date());
        historicalRecordFacade.edit(hr);
        try {
            List<BillTypeAtomic> types = Arrays.asList(BillTypeAtomic.values());
            List<ItemMovementSummaryDTO> rows = billService.fetchItemMovementSummaryDTOs(
                    hr.getFromDateTime(),
                    hr.getToDateTime(),
                    hr.getInstitution(),
                    hr.getSite(),
                    hr.getDepartment(),
                    types);
            XSSFWorkbook wb = new XSSFWorkbook();
            XSSFSheet sheet = wb.createSheet("Item Movement Summary");
            int r = 0;
            
            //TODO: If institution, Site, Department is not given, it should come as Department: All, Site : All, , Institution: All
            //TODO: Date time should be formatted with - sessionController.getApplicationPreference().getLongDateFormat()
            // TODO: THe rows should have a border
            // TODO: the Bill TYpe Atomic should be in a merged cell above the respective Quantity and Net value columns
            // TODO: For each item: the final Qty and final value should be added as the total columns
            // TODO: as the last column the current stock should be listed . the total stock can be taken from 
            // StockController
            // method depending on selection, public double findInstitutionStock(Institution institution, Item item) {, public double findDepartmentStock(Department department, Item item) { or  public double findSiteStock(Institution site, Item item) {
            // TODO: at the bottom, there should be a row to total all the net values ( quentities are not useful to total as they are from different items)
            
            if (hr.getFromDateTime() != null) {
                Row fr = sheet.createRow(r++);
                fr.createCell(0).setCellValue("From");
                fr.createCell(1).setCellValue(hr.getFromDateTime());
            }
            if (hr.getToDateTime() != null) {
                Row tr = sheet.createRow(r++);
                tr.createCell(0).setCellValue("To");
                tr.createCell(1).setCellValue(hr.getToDateTime());
            }
            if (hr.getInstitution() != null) {
                Row ir = sheet.createRow(r++);
                ir.createCell(0).setCellValue("Institution");
                ir.createCell(1).setCellValue(hr.getInstitution().getName());
            }
            if (hr.getSite() != null) {
                Row sr = sheet.createRow(r++);
                sr.createCell(0).setCellValue("Site");
                sr.createCell(1).setCellValue(hr.getSite().getName());
            }
            if (hr.getDepartment() != null) {
                Row dr = sheet.createRow(r++);
                dr.createCell(0).setCellValue("Department");
                dr.createCell(1).setCellValue(hr.getDepartment().getName());
            }
            r++; // blank line
// ChatGPT contribution: One row per item, with Qty and Net Value as adjacent columns under each Bill Type
            Map<String, Map<BillTypeAtomic, ItemMovementSummaryDTO>> grouped = new TreeMap<>();
            Set<BillTypeAtomic> billTypes = new TreeSet<>(Comparator.comparing(BillTypeAtomic::getLabel));

// Group data
            for (ItemMovementSummaryDTO dto : rows) {
                grouped.computeIfAbsent(dto.getItemName(), k -> new EnumMap<>(BillTypeAtomic.class))
                        .put(dto.getBillTypeAtomic(), dto);
                billTypes.add(dto.getBillTypeAtomic());
            }

// First header row: Bill type names
            Row header1 = sheet.createRow(r++);
            header1.createCell(0).setCellValue("Item");
            int col = 1;
            for (BillTypeAtomic bt : billTypes) {
                header1.createCell(col).setCellValue(bt.getLabel());
                header1.createCell(col + 1).setCellValue(""); // Placeholder for merge visual
                col += 2;
            }

// Second header row: Qty / Net Value
            Row header2 = sheet.createRow(r++);
            header2.createCell(0).setCellValue("");
            col = 1;
            for (int i = 0; i < billTypes.size(); i++) {
                header2.createCell(col++).setCellValue("Total Qty");
                header2.createCell(col++).setCellValue("Net Value");
            }

// Data rows: One row per item
            for (Map.Entry<String, Map<BillTypeAtomic, ItemMovementSummaryDTO>> entry : grouped.entrySet()) {
                Row dataRow = sheet.createRow(r++);
                dataRow.createCell(0).setCellValue(entry.getKey());
                Map<BillTypeAtomic, ItemMovementSummaryDTO> itemMap = entry.getValue();

                col = 1;
                for (BillTypeAtomic bt : billTypes) {
                    ItemMovementSummaryDTO dto = itemMap.get(bt);
                    if (dto != null) {
                        dataRow.createCell(col).setCellValue(dto.getQuantity());
                        dataRow.createCell(col + 1).setCellValue(dto.getNetValue());
                    } else {
                        dataRow.createCell(col).setCellValue("");
                        dataRow.createCell(col + 1).setCellValue("");
                    }
                    col += 2;
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            wb.close();
            Upload u = new Upload();
            u.setBaImage(baos.toByteArray());
            u.setFileName("All_Item_Movement_Summary.xlsx");
            u.setFileType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            u.setCreatedAt(new Date());
            u.setCreater(hr.getCreatedBy());
            u.setInstitution(hr.getInstitution());
            u.setHistoricalRecord(hr);
            uploadFacade.create(u);
            hr.setCompleted(true);
            hr.setCompletedAt(new Date());
        } catch (Exception e) {
            hr.setCompleted(false);
        } finally {
            historicalRecordFacade.edit(hr);
        }
    }
}
