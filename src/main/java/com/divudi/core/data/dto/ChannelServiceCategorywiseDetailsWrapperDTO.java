package com.divudi.core.data.dto;

import com.divudi.core.data.IncomeBundle;
import java.util.Date;
import java.util.List;

/**
 *
 * @author CHINTHAKA
 */
public class ChannelServiceCategorywiseDetailsWrapperDTO {

    private Long shiftStartBillId;
    private Long shiftEndBillId;
    private String cashierUserName;
    private Date billingStartDate;
    private Date billingEndDate;
    private List<ChannelServiceCategorywiseDetailsDTO> dtoList;
    private IncomeBundle opdBundle;
    private IncomeBundle pharmacyBundle;

    public ChannelServiceCategorywiseDetailsWrapperDTO(Long shiftStartBillId, Long shiftEndBillId, String cashierUserName, Date billingStartDate, Date billingEndDate, List<ChannelServiceCategorywiseDetailsDTO> dtoList) {
        this.shiftStartBillId = shiftStartBillId;
        this.shiftEndBillId = shiftEndBillId;
        this.cashierUserName = cashierUserName;
        this.billingStartDate = billingStartDate;
        this.billingEndDate = billingEndDate;
        this.dtoList = dtoList;
    }

    public ChannelServiceCategorywiseDetailsWrapperDTO() {
    }

    public IncomeBundle getOpdBundle() {
        return opdBundle;
    }

    public void setOpdBundle(IncomeBundle opdBundle) {
        this.opdBundle = opdBundle;
    }

    public IncomeBundle getPharmacyBundle() {
        return pharmacyBundle;
    }

    public void setPharmacyBundle(IncomeBundle pharmacyBundle) {
        this.pharmacyBundle = pharmacyBundle;
    }
    
    

    public Long getShiftStartBillId() {
        return shiftStartBillId;
    }

    public void setShiftStartBillId(Long shiftStartBillId) {
        this.shiftStartBillId = shiftStartBillId;
    }

    public Long getShiftEndBillId() {
        return shiftEndBillId;
    }

    public void setShiftEndBillId(Long shiftEndBillId) {
        this.shiftEndBillId = shiftEndBillId;
    }

    public String getCashierUserName() {
        return cashierUserName;
    }

    public void setCashierUserName(String cashierUserName) {
        this.cashierUserName = cashierUserName;
    }

    public Date getBillingStartDate() {
        return billingStartDate;
    }

    public void setBillingStartDate(Date billingStartDate) {
        this.billingStartDate = billingStartDate;
    }

    public Date getBillingEndDate() {
        return billingEndDate;
    }

    public void setBillingEndDate(Date billingEndDate) {
        this.billingEndDate = billingEndDate;
    }

    public List<ChannelServiceCategorywiseDetailsDTO> getDtoList() {
        return dtoList;
    }

    public void setDtoList(List<ChannelServiceCategorywiseDetailsDTO> dtoList) {
        this.dtoList = dtoList;
    }

}
