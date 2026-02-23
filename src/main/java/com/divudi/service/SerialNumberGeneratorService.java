package com.divudi.service;

import com.divudi.core.data.BillType;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.Staff;
import java.util.Date;
import java.util.HashMap;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Stateless
public class SerialNumberGeneratorService {

    public String generateDailyBillNumberForOpd(Department department, Category cat, Staff fromStaff) {
        String sql = "SELECT count(b) FROM Bill b "
                + " where (b.billType=:bTp1 or b.billType=:bTp2) "
                + " and b.billDate=:bd "
                + " and (type(b)=:class1 or type(b)=:class2) ";
        HashMap hm = new HashMap();

        if (department != null) {
            sql += " and b.department=:dep ";
            hm.put("dep", department);
        }

        if (cat != null) {
            sql += " and b.category=:cat ";
            hm.put("cat", cat);
        }

        if (fromStaff != null) {
            sql += " and b.fromStaff=:staff ";
            hm.put("staff", fromStaff);
        }

        hm.put("bTp1", BillType.OpdBill);
        hm.put("bTp2", BillType.OpdPreBill);
        hm.put("bd", new Date());
        hm.put("class1", BilledBill.class);
        hm.put("class2", PreBill.class);

        Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);
        return (dd != null) ? String.valueOf(dd) : "0";
    }
    

}
