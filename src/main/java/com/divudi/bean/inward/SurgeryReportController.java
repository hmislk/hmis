package com.divudi.bean.inward;

import com.divudi.core.data.BillType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author pubudupiyankara
 */
@Named
@SessionScoped
public class SurgeryReportController implements Serializable{

    public SurgeryReportController() {
    }
    
    @EJB
    BillFacade billFacade;
    
    private Institution institution;
    private Institution site;
    private Department department;

    private Date fromDate;
    private Date toDate;
    
    private Item procedure;
    private RoomFacilityCharge operationTheatreRoom;
    private List<Bill> billList;

    public void processSurgeryStatusReport() {
        billList = new ArrayList<>();
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select both From and To dates.");
            return;
        }

        StringBuilder jpql = new StringBuilder();
        jpql.append(" select b ")
                .append(" from Bill b ")
                .append(" join b.procedure p ")
                .append(" join p.item i ")
                .append(" left join i.category c ")
                .append(" where b.retired = false ")
                .append(" and b.cancelled = false ")
                .append(" and b.billType = :bt ")
                .append(" and b.createdAt between :fd and :td ");

        Map<String, Object> params = new HashMap<>();
        params.put("bt", BillType.SurgeryBill);
        params.put("fd", com.divudi.core.util.CommonFunctions.getStartOfDay(fromDate));
        params.put("td", com.divudi.core.util.CommonFunctions.getEndOfDay(toDate));

        if (institution != null) {
            jpql.append(" and b.institution = :inst ");
            params.put("inst", institution);
        }
        if (department != null) {
            jpql.append(" and b.department = :dept ");
            params.put("dept", department);
        }
        if (site != null) {
            jpql.append(" and b.department.site = :site ");
            params.put("site", site);
        }
        if (procedure != null) {
            jpql.append(" and i = :proc ");
            params.put("proc", procedure);
        }
        
        jpql.append(" order by i.name ");

        billList = billFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

    }

    public List<Bill> getBillList() {
        return billList;
    }

    public void setBillList(List<Bill> billList) {
        this.billList = billList;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
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

    public com.divudi.core.entity.Item getProcedure() {
        return procedure;
    }

    public void setProcedure(com.divudi.core.entity.Item procedure) {
        this.procedure = procedure;
    }

    public RoomFacilityCharge getOperationTheatreRoom() {
        return operationTheatreRoom;
    }

    public void setOperationTheatreRoom(RoomFacilityCharge operationTheatreRoom) {
        this.operationTheatreRoom = operationTheatreRoom;
    }

}
