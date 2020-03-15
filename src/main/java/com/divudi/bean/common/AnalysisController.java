/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.bean.pharmacy.VmpController;
import com.divudi.data.BillType;
import com.divudi.entity.BillItem;
import com.divudi.entity.Item;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.Atm;
import com.divudi.entity.pharmacy.Vmp;
import com.divudi.entity.pharmacy.Vtm;
import com.divudi.facade.BillItemFacade;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author buddhika.ari@gmail.com
 */
@Named
@SessionScoped
public class AnalysisController implements Serializable {

    /**
     * EJBs
     */
    @EJB
    private BillItemFacade billItemFacade;

    /**
     * Controllers
     */
    @Inject
    private VmpController vmpController;

    /**
     * Class Variables
     */
    private String message;
    private Date fromDate;
    private Date toDate;

    /**
     * Constructors
     */
    public AnalysisController() {
    }

    /**
     * Navigation Methods
     */
    /**
     * Functional Methods
     */
    public void createPharmacyBillItemSale() {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet1 = wb.createSheet("data");

        CreationHelper createHelper = wb.getCreationHelper();
        CellStyle cellStyleDateOnly = wb.createCellStyle();
        cellStyleDateOnly.setDataFormat(
                createHelper.createDataFormat().getFormat("dd/MMMM/yyyy"));
        CellStyle cellStyleTimeOnly = wb.createCellStyle();
        cellStyleTimeOnly.setDataFormat(
                createHelper.createDataFormat().getFormat("hh:mm"));
        CellStyle cellStyleDateTime = wb.createCellStyle();
        cellStyleDateTime.setDataFormat(
                createHelper.createDataFormat().getFormat("dd/MMMM/yyyy hh:mm"));

        String fileName = "Pharmacy Bill Items " + new Date() + ".xlsx";

        message = "Creating Pharmacy Sale<br/>";
        String j;
        Map m;

        j = "select bi from BillItem bi "
                + " where bi.retired=:ret "
                + " and bi.bill.retired=:ret "
                + " and bi.bill.billType = :billType "
                + " and bi.bill.billDate between :fd and :td "
                + " order by bi.item";
        m = new HashMap();
        m.put("ret", false);
        m.put("billType", BillType.PharmacyPre);
        m.put("fd", fromDate);
        m.put("td", toDate);

        List<BillItem> billItems = getBillItemFacade().findBySQL(j, m);

        Row row1 = sheet1.createRow(0);

        Item item = null;
        Vtm vtm = null;
        Amp amp = null;
        Vmp vmp = null;
        String vtms = "";

        for (int cn = 0; cn < 12; cn++) {

            Cell cellHeader = row1.createCell(cn);
            switch (cn) {
                case 0:
                    cellHeader.setCellValue("No.");
                    break;
                case 1:
                    cellHeader.setCellValue("Institution");
                    break;
                case 2:
                    cellHeader.setCellValue("Date");
                    break;
                case 3:
                    cellHeader.setCellValue("Time");
                    break;
                case 4:
                    cellHeader.setCellValue("Date Time");
                    break;
                case 5:
                    cellHeader.setCellValue("Patient ID");
                    break;
                case 6:
                    cellHeader.setCellValue("Patient Gender");
                    break;
                case 7:
                    cellHeader.setCellValue("Patient Age");
                    break;
                case 8:
                    cellHeader.setCellValue("Medicine");
                    break;
                case 9:
                    cellHeader.setCellValue("Product");
                    break;
                case 10:
                    cellHeader.setCellValue("Generic");
                    break;
                case 11:
                    cellHeader.setCellValue("Quantity");
                    break;
            }

        }

        int i = 1;
        int billItemCount = billItems.size();
        for (BillItem bi : billItems) {

            message = "Processing Row " + i + " of " + billItemCount + "rows.<br/>";

            Row row = sheet1.createRow(i);

            for (int cn = 0; cn < 12; cn++) {
                if (bi == null) {
                    continue;
                }
                if (bi.getBill() == null) {
                    continue;
                }

                if (bi.getItem() == null) {
                    continue;
                }

                if (item != null && bi.getItem().equals(item)) {

                } else {

                    item = bi.getItem();
                    if (bi.getItem() instanceof Amp) {
                        amp = (Amp) bi.getItem();
                        vmp = amp.getVmp();
                        if (vmp != null) {
                            vtms = getVmpController().getVivsAsString(vmp);
                        }
                    }

                }

                Cell cell = row.createCell(cn);
                switch (cn) {
                    case 0:
                        cell.setCellValue(i);
                        break;
                    case 1:
                        if (bi.getBill().getInstitution() != null && bi.getBill().getDepartment() != null) {
                            cell.setCellValue(bi.getBill().getInstitution().getId() + " " + bi.getBill().getDepartment().getId());
                        }
                        break;
                    case 2:
                        if (bi.getBill().getBillDate() != null) {
                            cell.setCellValue(bi.getBill().getBillDate());
                            cell.setCellStyle(cellStyleDateOnly);
                        }
                        break;
                    case 3:
                        if (bi.getBill().getBillTime() != null) {
                            cell.setCellValue(bi.getBill().getBillTime());
                            cell.setCellStyle(cellStyleTimeOnly);
                        }
                        break;
                    case 4:
                        if (bi.getBill().getCreatedAt() != null) {
                            cell.setCellValue(bi.getBill().getCreatedAt());
                            cell.setCellStyle(cellStyleDateTime);
                        }
                        break;
                    case 5:
                        if (bi.getBill().getPatient() != null && bi.getBill().getPatient().getPerson() != null) {
                            cell.setCellValue(bi.getBill().getPatient().getId() + "" + bi.getBill().getPatient().getPerson().getId());
                        }
                        break;
                    case 6:
                        if (bi.getBill().getPatient() != null && bi.getBill().getPatient().getPerson().getSex() != null) {
                            cell.setCellValue(bi.getBill().getPatient().getPerson().getSex().toString());
                        }
                        break;
                    case 7:
                        if (bi.getBill().getPatient() != null && bi.getBill().getPatient().getPerson().getDob() != null && bi.getBill().getBillDate() != null) {
                            cell.setCellValue(bi.getBill().getPatient().ageOnBilledDate(bi.getBill().getBillDate()));
                        }
                        break;
                    case 8:
                        if (amp != null) {
                            cell.setCellValue(amp.getName());
                        }
                        break;
                    case 9:
                        if (vmp != null) {
                            cell.setCellValue(vmp.getName());
                        }
                        break;
                    case 10:
                        if (vtms != null) {
                            cell.setCellValue(vtms);
                        }
                        break;
                    case 11:
                        cell.setCellValue(bi.getQty());
                        break;
                }
            }
            i++;
        }

        message = "Writing File";

        try {
            FacesContext fc = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
            response.reset();
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment;filename =" + fileName);
            wb.write(response.getOutputStream());
            fc.responseComplete();

        } catch (FileNotFoundException e) {
            Logger.getLogger(AnalysisController.class.getName()).log(Level.SEVERE, null, e);
            message = "Error in Downloading. " + e.getMessage();
        } catch (IOException e) {
            Logger.getLogger(AnalysisController.class.getName()).log(Level.SEVERE, null, e);
            message = "Error in Downloading. " + e.getMessage();
        }

        message = "Downloading File";

    }

    /**
     * Getters and Setters
     */
    /**
     *
     * @return
     */
    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public VmpController getVmpController() {
        return vmpController;
    }

    public void setVmpController(VmpController vmpController) {
        this.vmpController = vmpController;
    }

}
