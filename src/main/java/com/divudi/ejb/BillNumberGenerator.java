/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.ejb;

import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.entity.Bill;
import com.divudi.entity.BillNumber;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.PreBill;
import com.divudi.entity.RefundBill;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillNumberFacade;
import java.util.HashMap;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.persistence.TemporalType;

/**
 *
 * @author buddhika
 */
@Singleton
public class BillNumberGenerator {
    
    @EJB
    BillNumberFacade billNumberFacade;
    @EJB
    BillFacade billFacade;

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }
    
    

    public BillNumberFacade getBillNumberFacade() {
        return billNumberFacade;
    }

    public void setBillNumberFacade(BillNumberFacade billNumberFacade) {
        this.billNumberFacade = billNumberFacade;
    }

    public String departmentBillNumberGenerator(Bill bill, Department toDepartment, BillClassType billClassType) {
        BillNumber billNumber = fetchLastBillNumber(bill.getDepartment(), toDepartment, bill.getBillType(), billClassType);
        Long dd = billNumber.getLastBillNumber();
        String result = "";

        result += bill.getDepartment().getDepartmentCode();

        if (toDepartment != null) {
            result += bill.getToDepartment().getDepartmentCode();
        }

        result += "/";

        dd++;

        result += dd;

        billNumber.setLastBillNumber(dd);
        billNumberFacade.edit(billNumber);

        return result;
    }
    
    private BillNumber fetchLastBillNumber(Department department, Department toDepartment, BillType billType, BillClassType billClassType) {
        String sql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.billType=:bTp "
                + " and b.billClassType=:bcl"
                + " and b.department=:dep "
                + " and b.toDepartment=:tDep";
        HashMap hm = new HashMap();
        hm.put("bTp", billType);
        hm.put("bcl", billClassType);
        hm.put("dep", department);
        hm.put("tDep", toDepartment);
        BillNumber billNumber = billNumberFacade.findFirstBySQL(sql, hm);

        if (billNumber == null) {
            billNumber = new BillNumber();
            billNumber.setBillType(billType);
            billNumber.setBillClassType(billClassType);
            billNumber.setDepartment(department);
            billNumber.setToDepartment(toDepartment);

            sql = "SELECT count(b) FROM Bill b "
                    + " where b.billType=:bTp "
                    + " and b.retired=false"
                    + " and b.deptId is not null "
                    + " and type(b)=:class"
                    + " and b.department=:dep "
                    + " and b.toDepartment=:tDep";
            hm = new HashMap();
            hm.put("bTp", billType);
            hm.put("dep", department);
            hm.put("tDep", toDepartment);

            switch (billClassType) {
                case BilledBill:
                    hm.put("class", BilledBill.class);
                    break;
                case CancelledBill:
                    hm.put("class", CancelledBill.class);
                    break;
                case RefundBill:
                    hm.put("class", RefundBill.class);
                    break;
                case PreBill:
                    hm.put("class", PreBill.class);
                    break;
            }

            Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

            if (dd == null) {
                dd = 0l;
            }

            billNumber.setLastBillNumber(dd);

            billNumberFacade.create(billNumber);
        }

        return billNumber;

    }
    
    private BillNumber fetchLastBillNumber(Institution institution, Department toDepartment, BillType billType, BillClassType billClassType) {
        String sql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.billType=:bTp "
                + " and b.billClassType=:bcl"
                + " and b.institution=:ins "
                + " AND b.toDepartment=:tDep";
        HashMap hm = new HashMap();
        hm.put("bTp", billType);
        hm.put("bcl", billClassType);
        hm.put("ins", institution);
        hm.put("tDep", toDepartment);
        BillNumber billNumber = billNumberFacade.findFirstBySQL(sql, hm);

        System.err.println("1 " + billNumber);
        if (billNumber == null) {
            billNumber = new BillNumber();
            billNumber.setBillType(billType);
            billNumber.setBillClassType(billClassType);
            billNumber.setInstitution(institution);
            billNumber.setToDepartment(toDepartment);

            sql = "SELECT count(b) FROM Bill b "
                    + " where b.billType=:bTp "
                    + " and b.retired=false"
                    + " and type(b)=:class"
                    + " and b.institution=:ins "
                    + " and b.toDepartment=:tDep";
            hm = new HashMap();
            hm.put("bTp", billType);
            hm.put("ins", institution);
            hm.put("tDep", toDepartment);

            switch (billClassType) {
                case BilledBill:
                    hm.put("class", BilledBill.class);
                    break;
                case CancelledBill:
                    hm.put("class", CancelledBill.class);
                    break;
                case RefundBill:
                    hm.put("class", RefundBill.class);
                    break;
                case PreBill:
                    hm.put("class", PreBill.class);
                    break;
            }

            Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

            if (dd == null) {
                dd = 0l;
            }

            billNumber.setLastBillNumber(dd);
            System.err.println("2 " + billNumber.getLastBillNumber());

            billNumberFacade.create(billNumber);
        }

        return billNumber;

    }
    
    public String institutionBillNumberGenerator(Bill bill, Department toDepartment, BillClassType billClassType, BillNumberSuffix billNumberSuffix) {
        BillNumber billNumber = fetchLastBillNumber(bill.getInstitution(), toDepartment, bill.getBillType(), billClassType);
        String result = "";
        Long b = billNumber.getLastBillNumber();
        //System.err.println("fff " + b);

        result += bill.getInstitution().getInstitutionCode();
        System.err.println("R1 " + result);
        if (toDepartment != null) {
            result += toDepartment.getDepartmentCode();
            System.err.println("R1 " + result);
        }

        if (BillNumberSuffix.NONE != billNumberSuffix) {
            result += billNumberSuffix;
            System.err.println("R1 " + result);
        }

        result += "/";

        System.err.println("R1 " + result);
        b++;

        result += b;
        System.err.println("R1 " + result);

        System.err.println("3 " + billNumber.getLastBillNumber());
        billNumber.setLastBillNumber(b);
        System.err.println("4 " + billNumber.getLastBillNumber());
        billNumberFacade.edit(billNumber);
        System.err.println("5 " + billNumber.getLastBillNumber());
        System.err.println("Bill Num " + result);
        return result;

    }
    
    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
}
