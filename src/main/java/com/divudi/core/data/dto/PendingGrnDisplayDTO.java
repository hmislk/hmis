package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import java.io.Serializable;
import java.util.Date;

/**
 * DTO for the Pending GRN block and tab of the pharmacy item history. Carries
 * only the scalar values the UI shows, so rendering does not lazy-load the
 * bill, supplier, department, reference bill or pharmaceutical bill item.
 */
public class PendingGrnDisplayDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String billDeptId;
    private Date billCreatedAt;
    private String fromInstitutionName;
    private String departmentName;
    private String referenceBillDeptId;
    private BillTypeAtomic billTypeAtomic;
    private String itemName;
    private Double qty;
    private Double freeQty;

    public PendingGrnDisplayDTO() {
    }

    public PendingGrnDisplayDTO(String billDeptId,
            Date billCreatedAt,
            String fromInstitutionName,
            String departmentName,
            String referenceBillDeptId,
            BillTypeAtomic billTypeAtomic,
            String itemName,
            Double qty,
            Double freeQty) {
        this.billDeptId = billDeptId;
        this.billCreatedAt = billCreatedAt;
        this.fromInstitutionName = fromInstitutionName;
        this.departmentName = departmentName;
        this.referenceBillDeptId = referenceBillDeptId;
        this.billTypeAtomic = billTypeAtomic;
        this.itemName = itemName;
        this.qty = qty;
        this.freeQty = freeQty;
    }

    public String getBillDeptId() {
        return billDeptId;
    }

    public Date getBillCreatedAt() {
        return billCreatedAt;
    }

    public String getFromInstitutionName() {
        return fromInstitutionName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getReferenceBillDeptId() {
        return referenceBillDeptId;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public String getItemName() {
        return itemName;
    }

    public Double getQty() {
        return qty;
    }

    public Double getFreeQty() {
        return freeQty;
    }
}
