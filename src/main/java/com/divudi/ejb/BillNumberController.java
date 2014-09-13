/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.ejb;

import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.DepartmentType;
import com.divudi.entity.Bill;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.RefundBill;
import com.divudi.facade.BillFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PatientFacade;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Named
@ApplicationScoped
public class BillNumberController {

    @EJB
    private DepartmentFacade depFacade;
    @EJB
    private InstitutionFacade insFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    PatientFacade patientFacade;
    @EJB
    ItemFacade itemFacade;

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public void setPatientFacade(PatientFacade patientFacade) {
        this.patientFacade = patientFacade;
    }

    public String institutionBillNumberGenerator(Institution ins, Bill bill, BillType billType, BillNumberSuffix billNumberSuffix) {

        String sql = "SELECT count(b) FROM Bill b"
                + "  where type(b)=:type "
                + " and b.retired=false"
                + " AND b.institution=:ins "
                + " AND b.billType=:btp "
                + " and b.createdAt is not null";
        String result = "";
        HashMap hm = new HashMap();
        hm.put("ins", ins);
        hm.put("btp", billType);
        hm.put("type", bill.getClass());
        Long i = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

        if (i != null) {
            if (billNumberSuffix != BillNumberSuffix.NONE) {
                result = ins.getInstitutionCode() + billNumberSuffix + "/" + (i + 1);
            } else {
                result = ins.getInstitutionCode() + "/" + (i + 1);
            }

        } else {
            if (billNumberSuffix != BillNumberSuffix.NONE) {
                result = ins.getInstitutionCode() + billNumberSuffix + "/" + 1;
            } else {
                result = ins.getInstitutionCode() + "/" + 1;
            }

        }

        return result;
    }

    public String institutionChannelBillNumberGenerator(Institution ins, Bill bill) {
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};

        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "SELECT count(b) FROM Bill b"
                + "  where type(b)=:type "
                + " and b.retired=false"
                + " AND b.institution=:ins "
                + " and b.billType in :bt "
                + " and b.createdAt is not null";
        String result = "";
        HashMap hm = new HashMap();
        hm.put("ins", ins);
        hm.put("bt", bts);
        hm.put("type", bill.getClass());
        Long i = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

        String suffix = "";

        if (bill instanceof BilledBill) {
            suffix = "CHANN";
        }

        if (bill instanceof CancelledBill) {
            suffix = "CHANNCAN";
        }

        if (bill instanceof RefundBill) {
            suffix = "CHANNREF";
        }

        if (i != null) {
            result = ins.getInstitutionCode() + suffix + "/" + (i + 1);
        } else {
            result = ins.getInstitutionCode() + suffix + "/" + 1;
        }

        return result;
    }

    public String institutionBillNumberGeneratorWithReference(Institution ins, Bill bill, BillType billType, BillNumberSuffix billNumberSuffix) {

        String sql = "SELECT count(b) FROM Bill b where type(b)=:type and b.retired=false AND "
                + " b.institution=:ins AND b.billType=:btp and b.createdAt is not null and b.referenceBill is not null";
        String result = "";
        HashMap hm = new HashMap();
        hm.put("ins", ins);
        hm.put("btp", billType);
        hm.put("type", bill.getClass());
        Long i = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

        if (i != null) {
            if (billNumberSuffix != BillNumberSuffix.NONE) {
                result = ins.getInstitutionCode() + billNumberSuffix + "/" + (i + 1);
            } else {
                result = ins.getInstitutionCode() + "/" + (i + 1);
            }

        } else {
            if (billNumberSuffix != BillNumberSuffix.NONE) {
                result = ins.getInstitutionCode() + billNumberSuffix + "/" + 1;
            } else {
                result = ins.getInstitutionCode() + "/" + 1;
            }

        }

        return result;
    }

    public String institutionBillNumberGeneratorByPayment(Institution ins, Bill bill, BillType billType, BillNumberSuffix billNumberSuffix) {

        String sql = "SELECT count(b) FROM Bill b "
                + " where type(b)=:type "
                + " and b.retired=false "
                + " AND b.institution=:ins"
                + " AND b.billType=:btp "
                + " and b.createdAt is not null"
                + " and (b.netTotal >0 or b.total >0)  ";
        String result = "";
        HashMap hm = new HashMap();
        hm.put("ins", ins);
        hm.put("btp", billType);
        hm.put("type", bill.getClass());
        Long i = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

        if (i != null) {
            if (billNumberSuffix != BillNumberSuffix.NONE) {
                result = ins.getInstitutionCode() + billNumberSuffix + "/" + (i + 1);
            } else {
                result = ins.getInstitutionCode() + "/" + (i + 1);
            }

        } else {
            if (billNumberSuffix != BillNumberSuffix.NONE) {
                result = ins.getInstitutionCode() + billNumberSuffix + "/" + 1;
            } else {
                result = ins.getInstitutionCode() + "/" + 1;
            }

        }

        return result;
    }

    public String institutionBillNumberGenerator(Department dep, Bill bill, BillType billType, BillNumberSuffix billNumberSuffix) {

        String sql = "SELECT count(b) "
                + " FROM Bill b "
                + " where type(b)=:type "
                + " AND b.retired=false "
                + " AND b.department=:dep "
                + " AND b.createdAt is not null"
                + " AND b.billType=:btp ";
        String result = "";
        HashMap hm = new HashMap();
        hm.put("dep", dep);
        hm.put("btp", billType);
        hm.put("type", bill.getClass());
        Long i = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

        if (i != null) {
            if (billNumberSuffix != BillNumberSuffix.NONE) {
                result = dep.getDepartmentCode() + billNumberSuffix + "/" + (i + 1);
            } else {
                result = dep.getDepartmentCode() + "/" + (i + 1);
            }

        } else {
            if (billNumberSuffix != BillNumberSuffix.NONE) {
                result = dep.getDepartmentCode() + billNumberSuffix + "/" + 1;
            } else {
                result = dep.getDepartmentCode() + "/" + 1;
            }

        }

        return result;
    }

    public String institutionBillNumberGeneratorWithReference(Department dep, Bill bill, BillType billType, BillNumberSuffix billNumberSuffix) {

        String sql = "SELECT count(b) "
                + " FROM Bill b "
                + " where type(b)=:type "
                + " AND b.retired=false "
                + " AND b.department=:dep "
                + " AND b.createdAt is not null "
                + " AND b.billType=:btp "
                + " AND b.referenceBill is not null ";
        String result = "";
        HashMap hm = new HashMap();
        hm.put("dep", dep);
        hm.put("btp", billType);
        hm.put("type", bill.getClass());
        Long i = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

        if (i != null) {
            if (billNumberSuffix != BillNumberSuffix.NONE) {
                result = dep.getDepartmentCode() + billNumberSuffix + "/" + (i + 1);
            } else {
                result = dep.getDepartmentCode() + "/" + (i + 1);
            }

        } else {
            if (billNumberSuffix != BillNumberSuffix.NONE) {
                result = dep.getDepartmentCode() + billNumberSuffix + "/" + 1;
            } else {
                result = dep.getDepartmentCode() + "/" + 1;
            }

        }

        return result;
    }

    public String institutionBillNumberGeneratorByPayment(Department dep, Bill bill, BillType billType, BillNumberSuffix billNumberSuffix) {

        String sql = "SELECT count(b) FROM Bill b where type(b)=:type and b.retired=false AND "
                + " b.department=:dep and b.createdAt is not null AND b.billType=:btp "
                + " and b.billDate is not null and (b.netTotal >0 or b.total >0) ";
        String result = "";
        HashMap hm = new HashMap();
        hm.put("dep", dep);
        hm.put("btp", billType);
        hm.put("type", bill.getClass());
        Long i = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

        if (i != null) {
            if (billNumberSuffix != BillNumberSuffix.NONE) {
                result = dep.getDepartmentCode() + billNumberSuffix + "/" + (i + 1);
            } else {
                result = dep.getDepartmentCode() + "/" + (i + 1);
            }

        } else {
            if (billNumberSuffix != BillNumberSuffix.NONE) {
                result = dep.getDepartmentCode() + billNumberSuffix + "/" + 1;
            } else {
                result = dep.getDepartmentCode() + "/" + 1;
            }

        }

        return result;
    }

    static String intToString(int num, int digits) {
        assert digits > 0 : "Invalid number of digits";
        // create variable length array of zeros
        char[] zeros = new char[digits];
        Arrays.fill(zeros, '0');
        // format number as String
        DecimalFormat df = new DecimalFormat(String.valueOf(zeros));
        return df.format(num);
    }

    public String patientCodeGenerator() {
        long pts = getPatientFacade().count();
        return intToString((int) pts, 6);
    }

    public String gpBookingIdGenerator() {
        String sql = "SELECT count(b) FROM BilledBill b where b.retired=false  AND b.billType= :btp1";
        String result;
        HashMap h = new HashMap();
        h.put("btp1", BillType.gpBooking);
        Long l = getBillFacade().countBySql(sql, h);
        List<Bill> b = getBillFacade().findBySQL(sql, h);
        if (l != null) {
            l = l + 1;
            return "GPV" + l;
        } else {
            return "GPV1";
        }
    }

    public String bookingIdGenerator(Institution institution, Bill bill) {
        BillType[] billTypes = BillType.ChannelCashFlow.allChildren();
        List<BillType> bts = Arrays.asList(billTypes);
        String sql = "SELECT count(b) FROM BillSession b "
                + " where b.bill.retired=false "
                + "  AND type(b.bill)=:class "
                + "  AND b.bill.billType in :bt ";
        String result;
        HashMap h = new HashMap();
        h.put("bt", bts);
        h.put("class", bill.getClass());

        String suff = "";

        if (bill instanceof CancelledBill) {
            suff = "CAN";
        }

        if (bill instanceof RefundBill) {
            suff = "REF";
        }

        Long b = getBillFacade().findAggregateLong(sql, h, TemporalType.DATE);

        if (b != 0) {
            result = institution.getInstitutionCode() + "CHANN" + suff + "/" + (b + 1) + "";
            return result;
        } else {
            result = institution.getInstitutionCode() + "CHANN" + suff + "/" + 1 + "";
            return result;
        }

    }

    public String institutionBillNumberGenerator(Institution ins, Department toDept, Bill bill, BillType billType, BillNumberSuffix billNumberSuffix) {
        if (ins == null) {
            return "";
        }

        String sql = "SELECT count(b.id) FROM BilledBill b where b.retired=false AND "
                + " b.institution=:ins and b.toDepartment=:tDep and b.billType=:btp and type(b)=:class";
        String result;
        HashMap hm = new HashMap();
        hm.put("ins", ins);
        hm.put("tDep", toDept);
        hm.put("btp", billType);
        hm.put("class", bill.getClass());
        Long b = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);
        //System.err.println("fff " + b);

        if (billNumberSuffix == BillNumberSuffix.NONE) {
            if (b != null && b != 0) {
                b = b + 1;
                if (toDept != null) {
                    result = ins.getInstitutionCode() + toDept.getDepartmentCode() + "/" + b;
                } else {
                    result = ins.getInstitutionCode() + "/" + b;
                }
                return result;
            } else {
                if (toDept != null) {
                    result = ins.getInstitutionCode() + toDept.getDepartmentCode() + "/" + 1;
                } else {
                    result = ins.getInstitutionCode() + "/" + 1;
                }
                return result;
            }
        } else {
            if (b != null && b != 0) {
                b = b + 1;
                if (toDept != null) {
                    result = ins.getInstitutionCode() + toDept.getDepartmentCode() + billNumberSuffix + "/" + b;
                } else {
                    result = ins.getInstitutionCode() + billNumberSuffix + "/" + b;
                }
                return result;
            } else {
                if (toDept != null) {
                    result = ins.getInstitutionCode() + toDept.getDepartmentCode() + billNumberSuffix + "/" + 1;
                } else {
                    result = ins.getInstitutionCode() + billNumberSuffix + "/" + 1;
                }
                return result;
            }
        }

    }

    public String departmentBillNumberGenerator(Department dep, BillType billType, BillNumberSuffix billNumberSuffix) {

        if (dep == null || dep.getId() == null) {
            return "";
        }
        String sql = "SELECT count(b) FROM BilledBill b where b.billType= :type"
                + " and b.retired=false AND b.department=:dep and b.createdAt is not null";
        HashMap tmp = new HashMap();
        tmp.put("type", billType);
        tmp.put("dep", dep);
        Long b = getBillFacade().findAggregateLong(sql, tmp, TemporalType.TIMESTAMP);
        String result;
        if (b != 0) {
            result = dep.getDepartmentCode() + billNumberSuffix + (b + 1);
            return result;
        } else {
            result = dep.getDepartmentCode() + billNumberSuffix + 1;
            return result;
        }

    }

    public String departmentBillNumberGenerator(Department dep, Department toDept, BillType billType) {

        String sql = "SELECT count(b) FROM BilledBill b where b.billType=:bTp and"
                + " b.retired=false AND (b.department=:dep "
                + " AND b.toDepartment=:tDep)";
        HashMap hm = new HashMap();
        hm.put("bTp", billType);
        hm.put("dep", dep);
        hm.put("tDep", toDept);
        String result;
        Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

        if (dd != 0) {
            if (toDept != null) {
                result = dep.getDepartmentCode() + toDept.getDepartmentCode() + (dd + 1);
            } else {
                result = dep.getDepartmentCode() + (dd + 1);
            }
            return result;
        } else {
            if (toDept != null) {
                result = dep.getDepartmentCode() + toDept.getDepartmentCode() + 1;
            } else {
                result = dep.getDepartmentCode() + 1;
            }
            return result;
        }

    }

    public String departmentCancelledBill(Department dep, BillType type, BillNumberSuffix billNumberSuffix) {
        if (dep == null || dep.getId() == null) {
            return "";
        }
        String sql = "SELECT count(b) FROM CancelledBill b where "
                + " b.retired=false AND b.department=:dp AND b.billType= :btp";
        //System.out.println("sql");
        String result;
        HashMap h = new HashMap();
        h.put("btp", type);
        h.put("dp", dep);
        Long b = getBillFacade().findAggregateLong(sql, h, TemporalType.TIMESTAMP);

        if (b != 0) {
            result = dep.getDepartmentCode() + billNumberSuffix + (b + 1);
            return result;
        } else {
            result = dep.getDepartmentCode() + billNumberSuffix + 1;
            return result;
        }

    }

    public String departmentCancelledBill(Department dep, Department toDept, BillNumberSuffix billNumberSuffix) {
        if (dep == null || dep.getId() == null) {
            return "";
        }
        String sql = "SELECT count(b) FROM CancelledBill b where b.retired=false "
                + " AND b.department=:dep AND b.toDepartment=:tDep";
        //System.out.println("sql");
        String result;
        HashMap hm = new HashMap();
        hm.put("dep", dep);
        hm.put("tDep", toDept);
        Long b = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

        if (b != 0) {
            result = dep.getDepartmentCode() + toDept.getDepartmentCode() + billNumberSuffix + (b + 1);
            return result;
        } else {
            result = dep.getDepartmentCode() + toDept.getDepartmentCode() + billNumberSuffix + 1;
            return result;
        }

    }

    public String departmentReturnBill(Department dep, BillType billType, BillNumberSuffix billNumberSuffix) {

        String sql = "SELECT count(b) FROM RefundBill b where b.retired=false "
                + " AND b.department=:dep and b.billType = :bTp ";
        String result;
        HashMap hash = new HashMap();
        hash.put("bTp", billType);
        hash.put("dep", dep);
        Long b = getBillFacade().findAggregateLong(sql, hash, TemporalType.TIMESTAMP);

        if (b != 0) {
            result = dep.getDepartmentCode() + billNumberSuffix + (b + 1);
            return result;
        } else {
            result = dep.getDepartmentCode() + billNumberSuffix + 1;
            return result;
        }
    }

    public String departmentRefundBill(Department dep, Department toDept, BillNumberSuffix billNumberSuffix) {

        String sql = "SELECT count(b) FROM RefundBill b where b.retired=false "
                + " AND b.department=:dep AND b.toDepartment=:tDep";
        //System.out.println("sql");
        String result;
        HashMap hm = new HashMap();
        hm.put("dep", dep);
        hm.put("tDep", toDept);
        Long b = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

        if (b != 0) {
            result = dep.getDepartmentCode() + toDept.getDepartmentCode() + billNumberSuffix + (b + 1);
            return result;
        } else {
            result = dep.getDepartmentCode() + toDept.getDepartmentCode() + billNumberSuffix + 1;
            return result;
        }

    }

    public boolean itemCodeExistsInPharmacyOrStoreItems(String code) {
        HashMap hm = new HashMap();
        String sql = "SELECT count(b) FROM Amp b where b.code!=:code ";
        hm.put("code", code);
        long l = getItemFacade().findLongByJpql(sql, hm);
        return l != 0;
    }

    public String pharmacyItemNumberGenerator() {
        for (int i = 1; i < 100000; i++) {
            if (!itemCodeExistsInPharmacyOrStoreItems(i + "")) {
                return i + "";
            }
        }
        return "";
    }

    public String storeItemNumberGenerator() {
        HashMap hm = new HashMap();
        String sql = "SELECT count(b) FROM Amp b where b.retired=false and b.departmentType=:dep ";
        hm.put("dep", DepartmentType.Store);
        String result;
        Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.TIMESTAMP);

        result = dd.toString();

        return result;

    }

    public String storeInventryItemNumberGenerator() {
        HashMap hm = new HashMap();
        String sql = "SELECT count(b) FROM Amp b where b.retired=false and b.departmentType=:dep ";
        hm.put("dep", DepartmentType.Inventry);
        String result;
        Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.TIMESTAMP);

        result = dd.toString();

        return result;

    }

    public String serialNumberGenerater(Institution ins, Department toDept, Item item) {
        if (ins == null) {
            System.out.println("Ins null");
            return "";
        }

        String sql = "SELECT count(b) FROM BillItem b where "
                + " b.bill.institution=:ins "
                + " and b.bill.department=:tDep "
                + " and b.item=:item "
                + " and (b.bill.billType=:btp1 or b.bill.billType=:btp2)";

        HashMap hm = new HashMap();
        hm.put("ins", ins);
        hm.put("tDep", toDept);
        hm.put("item", item);
        hm.put("btp1", BillType.PharmacyPurchaseBill);
        hm.put("btp2", BillType.PharmacyGrnBill);
        Long b = getItemFacade().findAggregateLong(sql, hm, TemporalType.DATE);
        //System.err.println("fff " + b);

//        if (toDept != null) {
//            result = ins.getInstitutionCode() + toDept.getDepartmentCode() + "/" + 1;
//        } else {
//            result = ins.getInstitutionCode() + "/" + 1;
//        }
//        return result;
        System.out.println("In Bill Num Gen");
        String result;
        if (b != null && b != 0) {
            b = b + 1;
            if (toDept != null) {
                result = ins.getInstitutionCode() + toDept.getDepartmentCode() + "/" + b;
                System.out.println("result = " + result);
            } else {
                result = ins.getInstitutionCode() + "/" + b;
                System.out.println("result = " + result);
            }
            return result;
        } else {
            if (toDept != null) {
                result = ins.getInstitutionCode() + toDept.getDepartmentCode() + "/" + 1;
                System.out.println("result = " + result);
            } else {
                result = ins.getInstitutionCode() + "/" + 1;
                System.out.println("result = " + result);
            }
            return result;
        }

    }

    public DepartmentFacade getDepFacade() {
        return depFacade;
    }

    public void setDepFacade(DepartmentFacade depFacade) {
        this.depFacade = depFacade;
    }

    public InstitutionFacade getInsFacade() {
        return insFacade;
    }

    public void setInsFacade(InstitutionFacade insFacade) {
        this.insFacade = insFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

}
