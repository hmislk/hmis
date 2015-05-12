/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.ejb;

import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.DepartmentType;
import com.divudi.entity.Bill;
import com.divudi.entity.BillNumber;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.PreBill;
import com.divudi.entity.RefundBill;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillNumberFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PatientFacade;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Named
@Singleton
public class BillNumberGenerator {

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

    public String institutionBillNumberGenerator(Institution ins, BillType billType, BillClassType billClassType, BillNumberSuffix billNumberSuffix) {

        BillNumber billNumber = fetchLastBillNumber(ins, billType, billClassType);
        StringBuilder result = new StringBuilder();
        Long b = billNumber.getLastBillNumber();

        result.append(ins.getInstitutionCode());
        result.append(billNumberSuffix.getSuffix());

        result.append("/");
        result.append(++b);

        billNumber.setLastBillNumber(b);
        billNumberFacade.edit(billNumber);

        return result.toString();
    }

    @EJB
    CommonFunctions commonFunctions;

    public String fetchPaymentSchemeCount(PaymentScheme paymentScheme, BillType billType, Institution institution) {
        if (paymentScheme == null) {
            return "";
        }

        String sql = "SELECT count(b) FROM PreBill b "
                + "  where  b.retired=false "
                + " and b.institution=:ins "
                + " and b.billType = :bt "
                + " and b.createdAt between :f and :t";
        HashMap hm = new HashMap();
        hm.put("ins", institution);
        hm.put("bt", billType);
        hm.put("f", commonFunctions.getFirstDayOfYear(new Date()));
        hm.put("t", commonFunctions.getLastDayOfYear(new Date()));
        Long i = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

        return (i + 1) + "";
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

        String sql = "SELECT count(b) "
                + " FROM Bill b"
                + " where type(b)=:type "
                + "and b.retired=false"
                + " AND  b.institution=:ins"
                + " AND b.billType=:btp"
                + " and b.createdAt is not null"
                + " and b.referenceBill is not null";
        StringBuilder result = new StringBuilder();
        HashMap hm = new HashMap();
        hm.put("ins", ins);
        hm.put("btp", billType);
        hm.put("type", bill.getClass());
        Long i = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

        result.append(ins.getInstitutionCode());
        result.append(billNumberSuffix.getSuffix());
        result.append("/");
        if (i != null) {
            result.append(i + 1);
        } else {
            result.append(1);
        }

        return result.toString();
    }

    public String institutionBillNumberGeneratorByPayment(Institution ins, Bill bill, BillType billType, BillNumberSuffix billNumberSuffix) {

        String sql = "SELECT count(b) FROM Bill b "
                + " where type(b)=:type "
                + " and b.retired=false "
                + " AND b.institution=:ins"
                + " AND b.billType=:btp "
                + " and b.createdAt is not null";
//                + " and (b.netTotal >0 or b.total >0)  ";
        StringBuilder result = new StringBuilder();
        HashMap hm = new HashMap();
        hm.put("ins", ins);
        hm.put("btp", billType);
        hm.put("type", bill.getClass());
        Long i = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);

        result.append(ins.getInstitutionCode());
        result.append(billNumberSuffix.getSuffix());
        result.append("/");

        if (i != null) {
            result.append(i + 1);
        } else {
            result.append(1);
        }
        return result.toString();
    }

    public String institutionBillNumberGenerator(Department dep, BillType billType, BillClassType billClassType, BillNumberSuffix billNumberSuffix) {

        BillNumber billNumber = fetchLastBillNumber(dep, billType, billClassType);
        StringBuilder result = new StringBuilder();
        Long b = billNumber.getLastBillNumber();

        result.append(dep.getDepartmentCode());

        if (billNumberSuffix != BillNumberSuffix.NONE) {
            result.append(billNumberSuffix);
        }

        result.append("/");
        result.append(++b);

        billNumber.setLastBillNumber(b);
        billNumberFacade.edit(billNumber);

        return result.toString();
    }

    public boolean checkBillNumberDeptId(Department dep, Bill bill, BillType billType, String number) {

        String sql = "SELECT b "
                + " FROM Bill b "
                + " where type(b)=:type "
                + " AND b.retired=false "
                + " AND b.department=:dep"
                + " and upper(b.deptId)=:str "
                + " AND b.billType=:btp ";

        HashMap hm = new HashMap();
        hm.put("dep", dep);
        hm.put("btp", billType);
        hm.put("type", bill.getClass());
        hm.put("str", number.toUpperCase());
        Bill result = getBillFacade().findFirstBySQL(sql, hm, TemporalType.DATE);

        if (result != null) {
            return true;
        } else {
            return false;
        }
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

        result = createNumber(i, billNumberSuffix, dep);

        if (checkBillNumberDeptId(dep, bill, billType, result)) {
            result = createNumber(i + 1, billNumberSuffix, dep);
        }

        return result;
    }

    private String createNumber(Long i, BillNumberSuffix billNumberSuffix, Department dep) {
        String result = "";
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

        String sql = "SELECT count(b) FROM Bill b "
                + " where type(b)=:type"
                + " and b.retired=false "
                + " AND b.department=:dep "
                + " and b.createdAt is not null "
                + " AND b.billType=:btp "
                + " and b.billDate is not null";
//                + " and (b.netTotal >0 or b.total >0) ";
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
        h.put("btp1", BillType.ClinicalOpdBooking);
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

//    public String institutionBillNumberGenerator(Institution ins, Department toDept, Bill bill, BillType billType, BillNumberSuffix billNumberSuffix) {
//        if (ins == null) {
//            return "";
//        }
//
//        String sql = "SELECT count(b.id) "
//                + " FROM Bill b "
//                + " where b.retired=false "
//                + " and b.insId is not null"
//                + " AND  b.institution=:ins "
//                + " and b.toDepartment=:tDep"
//                + " and b.billType=:btp "
//                + " and type(b)=:class";
//        String result;
//        HashMap hm = new HashMap();
//        hm.put("ins", ins);
//        hm.put("tDep", toDept);
//        hm.put("btp", billType);
//        hm.put("class", bill.getClass());
//        Long b = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);
//        //System.err.println("fff " + b);
//
//        if (billNumberSuffix == BillNumberSuffix.NONE) {
//            if (b != null && b != 0) {
//                b = b + 1;
//                if (toDept != null) {
//                    result = ins.getInstitutionCode() + toDept.getDepartmentCode() + "/" + b;
//                } else {
//                    result = ins.getInstitutionCode() + "/" + b;
//                }
//                return result;
//            } else {
//                if (toDept != null) {
//                    result = ins.getInstitutionCode() + toDept.getDepartmentCode() + "/" + 1;
//                } else {
//                    result = ins.getInstitutionCode() + "/" + 1;
//                }
//                return result;
//            }
//        } else {
//            if (b != null && b != 0) {
//                b = b + 1;
//                if (toDept != null) {
//                    result = ins.getInstitutionCode() + toDept.getDepartmentCode() + billNumberSuffix + "/" + b;
//                } else {
//                    result = ins.getInstitutionCode() + billNumberSuffix + "/" + b;
//                }
//                return result;
//            } else {
//                if (toDept != null) {
//                    result = ins.getInstitutionCode() + toDept.getDepartmentCode() + billNumberSuffix + "/" + 1;
//                } else {
//                    result = ins.getInstitutionCode() + billNumberSuffix + "/" + 1;
//                }
//                return result;
//            }
//        }
//
//    }
    public String institutionBillNumberGenerator(Institution institution, Department toDepartment, BillType billType, BillClassType billClassType, BillNumberSuffix billNumberSuffix) {
        BillNumber billNumber = fetchLastBillNumber(institution, toDepartment, billType, billClassType);
        StringBuilder result = new StringBuilder();
        Long b = billNumber.getLastBillNumber();
        //System.err.println("fff " + b);

        result.append(institution.getInstitutionCode());
//        System.err.println("R1 " + result);
        if (toDepartment != null) {
            result.append(toDepartment.getDepartmentCode());
//            System.err.println("R1 " + result);
        }

        if (BillNumberSuffix.NONE != billNumberSuffix) {
            result.append(billNumberSuffix);
//            System.err.println("R1 " + result);
        }

        result.append("/");

        result.append(++b);
//        System.err.println("R1 " + result);

//        System.err.println("3 " + billNumber.getLastBillNumber());
        billNumber.setLastBillNumber(b);
//        System.err.println("4 " + billNumber.getLastBillNumber());
        billNumberFacade.edit(billNumber);
//        System.err.println("5 " + billNumber.getLastBillNumber());
//        System.err.println("Bill Num " + result);
        return result.toString();

    }
    
    public String institutionBillNumberGenerator(Institution institution, List<BillType>billTypes,BillClassType billClassType,String suffix) {
        BillNumber billNumber = fetchLastBillNumber(institution, billTypes, billClassType);
        StringBuilder result = new StringBuilder();
        Long b = billNumber.getLastBillNumber();

        result.append(suffix);

        result.append("/");

        result.append(++b);

        billNumber.setLastBillNumber(b);

        billNumberFacade.edit(billNumber);

        return result.toString();

    }

//    public String institutionBillNumberGenerator(Institution institution,BillType billType, BillClassType billClassType, BillNumberSuffix billNumberSuffix) {
//        BillNumber billNumber = fetchLastBillNumber(bill.getInstitution(), bill.getBillType(), billClassType);
//        String result = "";
//        Long b = billNumber.getLastBillNumber();
//        //System.err.println("fff " + b);
//
//        result += bill.getInstitution().getInstitutionCode();
//
//        if (BillNumberSuffix.NONE != billNumberSuffix) {
//            result += billNumberSuffix;
//        }
//
//        result += "/";
//
//        b++;
//
//        result += b;
//
//        billNumber.setLastBillNumber(b);
//        billNumberFacade.edit(billNumber);
//
//        return result;
//
//    }
//    public String departmentBillNumberGenerator(Department dep, BillType billType, BillNumberSuffix billNumberSuffix) {
//
//        if (dep == null || dep.getId() == null) {
//            return "";
//        }
//        
//        
//        String sql = "SELECT count(b) FROM BilledBill b where b.billType= :type"
//                + " and b.retired=false AND b.department=:dep and b.createdAt is not null";
//        HashMap tmp = new HashMap();
//        tmp.put("type", billType);
//        tmp.put("dep", dep);
//        Long b = getBillFacade().findAggregateLong(sql, tmp, TemporalType.TIMESTAMP);
//        String result;
//        if (b != 0) {
//            result = dep.getDepartmentCode() + billNumberSuffix + (b + 1);
//            return result;
//        } else {
//            result = dep.getDepartmentCode() + billNumberSuffix + 1;
//            return result;
//        }
//
//    }
    @EJB
    BillNumberFacade billNumberFacade;

    private BillNumber fetchLastBillNumber(Department department, Department toDepartment, BillType billType, BillClassType billClassType) {
        String sql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.retired=false "
                + " and b.billType=:bTp "
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

    private BillNumber fetchLastBillNumber(Department department, BillType billType, BillClassType billClassType) {
        String sql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.retired=false "
                + " and  b.billType=:bTp "
                + " and b.billClassType=:bcl "
                + " and b.department=:dep ";
        HashMap hm = new HashMap();
        hm.put("bTp", billType);
        hm.put("bcl", billClassType);
        hm.put("dep", department);
        BillNumber billNumber = billNumberFacade.findFirstBySQL(sql, hm);

        if (billNumber == null) {
            billNumber = new BillNumber();
            billNumber.setBillType(billType);
            billNumber.setBillClassType(billClassType);
            billNumber.setDepartment(department);

            sql = "SELECT count(b) FROM Bill b "
                    + " where b.billType=:bTp "
                    + " and b.retired=false"
                    + " and b.deptId is not null "
                    + " and type(b)=:class"
                    + " and b.department=:dep ";
            hm = new HashMap();
            hm.put("bTp", billType);
            hm.put("dep", department);

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
                + " where b.retired=false "
                + " and b.billType=:bTp "
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

    private BillNumber fetchLastBillNumber(Institution institution, BillType billType, BillClassType billClassType) {
        String sql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.retired=false "
                + " and b.billType=:bTp "
                + " and b.billClassType=:bcl"
                + " and b.institution=:ins "
                + " and b.toDepartment is null ";
        HashMap hm = new HashMap();
        hm.put("bTp", billType);
        hm.put("bcl", billClassType);
        hm.put("ins", institution);
        BillNumber billNumber = billNumberFacade.findFirstBySQL(sql, hm);

        if (billNumber == null) {
            billNumber = new BillNumber();
            billNumber.setBillType(billType);
            billNumber.setBillClassType(billClassType);
            billNumber.setInstitution(institution);

            sql = "SELECT count(b) FROM Bill b "
                    + " where b.billType=:bTp "
                    + " and b.retired=false"
                    + " and b.deptId is not null "
                    + " and type(b)=:class"
                    + " and b.institution=:ins "
                    + " and b.toDepartment is null  ";
            hm = new HashMap();
            hm.put("bTp", billType);
            hm.put("ins", institution);

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

    private BillNumber fetchLastBillNumber(Institution institution, List<BillType> billTypes, BillClassType billClassType) {
        String sql = "SELECT b FROM "
                + " BillNumber b "
                + " where b.retired=false "
                + " and b.billClassType=:class "
                + " and b.institution=:ins "
                + " and b.billType is null ";
        HashMap hm = new HashMap();
        hm.put("class", billClassType);
        hm.put("ins", institution);
        BillNumber billNumber = billNumberFacade.findFirstBySQL(sql, hm);

        System.err.println("1 " + billNumber);
        if (billNumber == null) {
            billNumber = new BillNumber();
            billNumber.setBillClassType(billClassType);
            billNumber.setInstitution(institution);
            HashMap m = new HashMap();

            sql = "SELECT count(b) FROM Bill b"
                    + "  where type(b)=:class "
                    + " and b.retired=false"
                    + " and b.institution=:ins "
                    + " and b.billType in :bt "
                    + " and b.createdAt is not null";
            
            hm.put("ins", institution);
            hm.put("bt", billTypes);

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

    public String departmentBillNumberGenerator(Department dep, Department toDept, BillType billType, BillClassType billClassType) {
        BillNumber billNumber = fetchLastBillNumber(dep, toDept, billType, billClassType);
        Long dd = billNumber.getLastBillNumber();
        StringBuilder result = new StringBuilder();

        result.append(dep.getDepartmentCode());

        if (toDept != null) {
            result.append(toDept.getDepartmentCode());
        }

        result.append("/");
        result.append(++dd);

        billNumber.setLastBillNumber(dd);
        billNumberFacade.edit(billNumber);

        return result.toString();
    }

    public String departmentBillNumberGenerator(Department dep, Department toDept, BillType billType, BillClassType billClassType, BillNumberSuffix billNumberSuffix) {
        BillNumber billNumber = fetchLastBillNumber(dep, toDept, billType, billClassType);
        Long dd = billNumber.getLastBillNumber();
        StringBuilder result = new StringBuilder();

        result.append(dep.getDepartmentCode());

        if (toDept != null) {
            result.append(toDept.getDepartmentCode());
        }

        if (billNumberSuffix != BillNumberSuffix.NONE) {
            result.append(billNumberSuffix);
        }

        result.append("/");
        result.append(++dd);

        billNumber.setLastBillNumber(dd);
        billNumberFacade.edit(billNumber);

        return result.toString();
    }

    public String departmentBillNumberGenerator(Department dep, BillType billType, BillClassType billClassType, BillNumberSuffix billNumberSuffix) {
        BillNumber billNumber = fetchLastBillNumber(dep, billType, billClassType);
        Long dd = billNumber.getLastBillNumber();
        StringBuilder result = new StringBuilder();

        result.append(dep.getDepartmentCode());

        if (billNumberSuffix != BillNumberSuffix.NONE) {
            result.append(billNumberSuffix);
        }

        result.append("/");
        result.append(++dd);

        billNumber.setLastBillNumber(dd);
        billNumberFacade.edit(billNumber);

        return result.toString();
    }

//    public String departmentBillNumberGenerator(Bill bill, Department toDepartment, BillClassType billClassType) {
//        BillNumber billNumber = fetchLastBillNumber(bill.getDepartment(), toDepartment, bill.getBillType(), billClassType);
//        Long dd = billNumber.getLastBillNumber();
//        String result = "";
//
//        result += bill.getDepartment().getDepartmentCode();
//
//        if (toDepartment != null) {
//            result += bill.getToDepartment().getDepartmentCode();
//        }
//
//        result += "/";
//
//        dd++;
//
//        result += dd;
//
//        billNumber.setLastBillNumber(dd);
//        billNumberFacade.edit(billNumber);
//
//        return result;
//    }
//    public String departmentBillNumberGenerator(Department department, BillType billType, BillClassType billClassType, BillNumberSuffix billNumberSuffix) {
//        BillNumber billNumber = fetchLastBillNumber(department, billType, billClassType);
//        Long dd = billNumber.getLastBillNumber();
//        String result = "";
//
//        result += bill.getDepartment().getDepartmentCode();
//
//        if (billNumberSuffix != BillNumberSuffix.NONE) {
//            result += billNumberSuffix;
//        }
//
//        result += "/";
//
//        dd++;
//
//        result += dd;
//
//        billNumber.setLastBillNumber(dd);
//        billNumberFacade.edit(billNumber);
//
//        return result;
//    }
//    public String departmentCancelledBill(Department dep, BillType type, BillNumberSuffix billNumberSuffix) {
//        if (dep == null || dep.getId() == null) {
//            return "";
//        }
//        String sql = "SELECT count(b) FROM CancelledBill b where "
//                + " b.retired=false AND b.department=:dp AND b.billType= :btp";
//        ////System.out.println("sql");
//        String result;
//        HashMap h = new HashMap();
//        h.put("btp", type);
//        h.put("dp", dep);
//        Long b = getBillFacade().findAggregateLong(sql, h, TemporalType.TIMESTAMP);
//
//        if (b != 0) {
//            result = dep.getDepartmentCode() + billNumberSuffix + (b + 1);
//            return result;
//        } else {
//            result = dep.getDepartmentCode() + billNumberSuffix + 1;
//            return result;
//        }
//
//    }
//    public String departmentCancelledBill(Department dep, Department toDept, BillNumberSuffix billNumberSuffix) {
//        if (dep == null || dep.getId() == null) {
//            return "";
//        }
//        String sql = "SELECT count(b) FROM CancelledBill b where b.retired=false "
//                + " AND b.department=:dep AND b.toDepartment=:tDep";
//        ////System.out.println("sql");
//        String result;
//        HashMap hm = new HashMap();
//        hm.put("dep", dep);
//        hm.put("tDep", toDept);
//        Long b = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);
//
//        if (b != 0) {
//            result = dep.getDepartmentCode() + toDept.getDepartmentCode() + billNumberSuffix + (b + 1);
//            return result;
//        } else {
//            result = dep.getDepartmentCode() + toDept.getDepartmentCode() + billNumberSuffix + 1;
//            return result;
//        }
//
//    }
//    public String departmentReturnBill(Department dep, BillType billType, BillNumberSuffix billNumberSuffix) {
//
//        String sql = "SELECT count(b) FROM RefundBill b where b.retired=false "
//                + " AND b.department=:dep and b.billType = :bTp ";
//        String result;
//        HashMap hash = new HashMap();
//        hash.put("bTp", billType);
//        hash.put("dep", dep);
//        Long b = getBillFacade().findAggregateLong(sql, hash, TemporalType.TIMESTAMP);
//
//        if (b != 0) {
//            result = dep.getDepartmentCode() + billNumberSuffix + (b + 1);
//            return result;
//        } else {
//            result = dep.getDepartmentCode() + billNumberSuffix + 1;
//            return result;
//        }
//    }
//    public String departmentRefundBill(Department dep, Department toDept, BillNumberSuffix billNumberSuffix) {
//
//        String sql = "SELECT count(b) FROM RefundBill b where b.retired=false "
//                + " AND b.department=:dep AND b.toDepartment=:tDep";
//        ////System.out.println("sql");
//        String result;
//        HashMap hm = new HashMap();
//        hm.put("dep", dep);
//        hm.put("tDep", toDept);
//        Long b = getBillFacade().findAggregateLong(sql, hm, TemporalType.DATE);
//
//        if (b != 0) {
//            result = dep.getDepartmentCode() + toDept.getDepartmentCode() + billNumberSuffix + (b + 1);
//            return result;
//        } else {
//            result = dep.getDepartmentCode() + toDept.getDepartmentCode() + billNumberSuffix + 1;
//            return result;
//        }
//
//    }
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
        String sql = "SELECT count(b) FROM Amp b where b.retired=false"
                + " and b.departmentType=:dep ";
        hm.put("dep", DepartmentType.Store);
        String result;
        Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.TIMESTAMP);
        dd = dd + 1;
        //System.out.println("dd = " + dd);
        result = "MS" + dd.toString();
        //System.out.println("result = " + result);
        return result;

    }

    public String storeInventryItemNumberGenerator() {
        HashMap hm = new HashMap();
        String sql = "SELECT count(b) FROM Amp b where b.retired=false and b.departmentType=:dep ";
        hm.put("dep", DepartmentType.Inventry);
        String result;
        Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.TIMESTAMP);
        dd = dd + 1;
        //System.out.println("dd = " + dd);
        result = "ASS" + dd.toString();
        //System.out.println("result = " + result);
        return result;

    }

//    public String storeInventryItemNumberGenerator() {
//        HashMap hm = new HashMap();
//        String sql = "SELECT count(b) FROM Amp b where b.retired=false and b.departmentType=:dep ";
//        hm.put("dep", DepartmentType.Inventry);
//        String result;
//        Long dd = getBillFacade().findAggregateLong(sql, hm, TemporalType.TIMESTAMP);
//
//        result = dd.toString();
//
//        return result;
//
//    }
    public String serialNumberGenerater(Institution ins, Department toDept, Item item) {
        if (ins == null) {
            //System.out.println("Ins null");
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
        //System.out.println("In Bill Num Gen");
        String result;
        if (b != null && b != 0) {
            b = b + 1;
            if (toDept != null) {
                result = ins.getInstitutionCode() + toDept.getDepartmentCode() + "/" + b;
                //System.out.println("result = " + result);
            } else {
                result = ins.getInstitutionCode() + "/" + b;
                //System.out.println("result = " + result);
            }
            return result;
        } else {
            if (toDept != null) {
                result = ins.getInstitutionCode() + toDept.getDepartmentCode() + "/" + 1;
                //System.out.println("result = " + result);
            } else {
                result = ins.getInstitutionCode() + "/" + 1;
                //System.out.println("result = " + result);
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

    public Long inventoryItemSerialNumberGenerater(Institution ins, Item item) {
        if (ins == null) {
            //System.out.println("Ins null");
            return 0l;
        }
        String sql = "SELECT count(b) FROM BillItem b where "
                + " b.bill.institution=:ins "
                + " and b.item=:item "
                + " and (b.bill.billType=:btp1 or b.bill.billType=:btp2)";

        HashMap hm = new HashMap();
        hm.put("ins", ins);
        hm.put("item", item);
        hm.put("btp1", BillType.StoreGrnBill);
        hm.put("btp2", BillType.StorePurchase);
        Long b = getItemFacade().findAggregateLong(sql, hm, TemporalType.DATE);
        //System.out.println("In Bill Num Gen" + b);
        return b;
    }

}
