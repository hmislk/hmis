/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.Barcode39;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.inject.Inject;
import javax.inject.Named;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author Buddhika
 */
@Named
@RequestScoped
public class BarcodeController {

    @Inject
    PatientController patientController;

    /**
     * Creates a new instance of BarcodeController
     */
    public BarcodeController() {
    }

    public PatientController getPatientController() {
        return patientController;
    }

    public StreamedContent getCreateBarcodeCode128() {
        StreamedContent barcode = null;
        String code;
        FacesContext context = FacesContext.getCurrentInstance();
        code = context.getExternalContext().getRequestParameterMap().get("code");

        if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
            return new DefaultStreamedContent();
        }
        if (code == null || code.trim().equals("")) {
            return null;
        }
        File barcodeFile = new File(code);
        try {
            BarcodeImageHandler.saveJPEG(BarcodeFactory.createCode128(code), barcodeFile);
            FileInputStream stream = new FileInputStream(barcodeFile);
            barcode = DefaultStreamedContent.builder()
                    .contentType("image/jpeg")
                    .stream(() -> stream)
                    .build();
        } catch (Exception ex) {
        }

        return barcode;
    }

    public void setPatientController(PatientController patientController) {
        this.patientController = patientController;
    }

    public StreamedContent getCreatePatientBarcode() {
        StreamedContent barcode = null;
        //Barcode  
        //   ////System.out.println("creating pt bar code");

        File barcodeFile = new File(getPatientController().getCurrent().toString());
        //   ////System.out.println("current = " + getPatientController().getCurrent());
        if (getPatientController().getCurrent() != null && getPatientController().getCurrent().getCode() != null && !getPatientController().getCurrent().getCode().trim().equals("")) {
            //   ////System.out.println("getCurrent().getCode() = " + getPatientController().getCurrent().getCode());
            try {
                BarcodeImageHandler.saveJPEG(BarcodeFactory.createCode128C(getPatientController().getCurrent().getCode()), barcodeFile);
                InputStream targetStream = new FileInputStream(barcodeFile);
                StreamedContent str = DefaultStreamedContent.builder().contentType("image/jpeg").name(barcodeFile.getName()).stream(() -> targetStream).build();
                barcode = str;

            } catch (Exception ex) {
                //   ////System.out.println("ex = " + ex.getMessage());
            }
        } else {
            //   ////System.out.println("else = ");
            try {
                Barcode bc = BarcodeFactory.createCode128C("0000");
                bc.setBarHeight(5);
                bc.setBarWidth(3);
                bc.setDrawingText(true);
                BarcodeImageHandler.saveJPEG(bc, barcodeFile);
                InputStream targetStream = new FileInputStream(barcodeFile);
                StreamedContent str = DefaultStreamedContent.builder().contentType("image/jpeg").name(barcodeFile.getName()).stream(() -> targetStream).build();
                barcode = str;
            } catch (Exception ex) {
                //   ////System.out.println("ex = " + ex.getMessage());
            }
        }
        return barcode;
    }

    public StreamedContent getCreateBarcode(String code) {
        StreamedContent barcode = null;
        //   ////System.out.println("code = " + code);
        if (code == null || code.trim().equals("")) {
            return null;
        }
        File barcodeFile = new File(code);
        try {
            BarcodeImageHandler.saveJPEG(BarcodeFactory.createCode128C(code), barcodeFile);
            InputStream targetStream = new FileInputStream(barcodeFile);
            StreamedContent str = DefaultStreamedContent.builder().contentType("image/jpeg").name(barcodeFile.getName()).stream(() -> targetStream).build();
            barcode = str;
        } catch (Exception ex) {
            //   ////System.out.println("ex = " + ex.getMessage());
        }

        return barcode;
    }

    public byte[] getBarcodeBytes(String code) {
        Barcode39 code39 = new Barcode39();
        code39.setCode(code);
        code39.setFont(null);
        code39.setExtended(true);
        Image image = null;
        try {
            image = Image.getInstance(code39.createAwtImage(Color.BLACK, Color.WHITE), null);
        } catch (BadElementException ex) {
            Logger.getLogger(BarcodeController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BarcodeController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return image.getRawData();
    }

}
