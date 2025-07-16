package com.divudi.service;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.HistoricalRecord;
import com.divudi.core.entity.Upload;
import com.divudi.core.data.dto.ItemMovementSummaryDTO;
import com.divudi.core.facade.HistoricalRecordFacade;
import com.divudi.core.facade.UploadFacade;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
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
            Row header = sheet.createRow(r++);
            header.createCell(0).setCellValue("Bill Type");
            header.createCell(1).setCellValue("Item");
            header.createCell(2).setCellValue("Total Qty");
            header.createCell(3).setCellValue("Net Value");
            for (ItemMovementSummaryDTO d : rows) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(d.getBillTypeAtomic().getLabel());
                row.createCell(1).setCellValue(d.getItemName());
                row.createCell(2).setCellValue(d.getQuantity());
                row.createCell(3).setCellValue(d.getNetValue());
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
