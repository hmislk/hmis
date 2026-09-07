package com.divudi.core.data.dataStructure;

import com.divudi.core.data.Title;
import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 * 
 */
public class InvestigationDetails implements Serializable  {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Date billDate;
    private String billNumber;
    private Title patientTitle;
    private String patientName;
    private Double itemAmount;

    public InvestigationDetails(Long id, String billNumber, Date billDate, String patientName, Double itemAmount) {
        this.id = id;
        this.billNumber = billNumber;
        this.billDate = billDate;
        this.patientName = patientName;
        this.itemAmount = itemAmount;
    }

    public Double getItemAmount() {
        return itemAmount;
    }

    public void setItemAmount(Double itemAmount) {
        this.itemAmount = itemAmount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getBillDate() {
        return billDate;
    }

    public void setBillDate(Date billDate) {
        this.billDate = billDate;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public Title getPatientTitle() {
        return patientTitle;
    }

    public void setPatientTitle(Title patientTitle) {
        this.patientTitle = patientTitle;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }
    
    

}
