/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.bean.lab.InvestigationController;
import com.divudi.data.BillType;
import com.divudi.data.DepartmentType;
import com.divudi.data.dataStructure.BillListWithTotals;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.ejb.BillEjb;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillNumber;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Service;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Staff;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.lab.PatientReport;
import com.divudi.entity.lab.PatientReportItemValue;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.ItemBatch;
import com.divudi.entity.pharmacy.PharmaceuticalItemCategory;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillEntryFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillNumberFacade;
import com.divudi.facade.CategoryFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.ItemBatchFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.facade.PatientInvestigationItemValueFacade;
import com.divudi.facade.PatientReportFacade;
import com.divudi.facade.PatientReportItemValueFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.PharmaceuticalItemCategoryFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.bean.common.util.JsfUtil;
import java.sql.SQLSyntaxErrorException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Entity;
import javax.persistence.PersistenceException;
import javax.persistence.TemporalType;
import org.reflections.Reflections;

/**
 *
 * @author Administrator
 */
@Named(value = "dataAdministrationController")
@ApplicationScoped
public class DataAdministrationController {

    /**
     * EJBs
     */
    @EJB
    PatientInvestigationItemValueFacade patientInvestigationItemValueFacade;
    @EJB
    PatientReportItemValueFacade patientReportItemValueFacade;
    @EJB
    PatientReportFacade patientReportFacade;
    @EJB
    PatientInvestigationFacade patientInvestigationFacade;
    @EJB
    BillComponentFacade billComponentFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    BillEntryFacade billEntryFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    BillNumberFacade billNumberFacade;
    @EJB
    PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    InstitutionFacade institutionFacade;
    @EJB
    PharmaceuticalItemCategoryFacade pharmaceuticalItemCategoryFacade;

    @Inject
    SessionController sessionController;
    @Inject
    BillSearch billSearch;
    @Inject
    InstitutionController institutionController;
    @Inject
    CommonFunctionsController commonFunctionsController;

    @EJB
    ItemFacade itemFacade;
    @EJB
    CategoryFacade categoryFacade;
    @EJB
    ItemBatchFacade itemBatchFacade;
    @EJB
    StaffFacade staffFacade;
    @EJB
    PersonFacade personFacade;
    @EJB
    ServiceSessionFacade serviceSessionFacade;
    @EJB
    private DepartmentFacade departmentFacade;

    @EJB
    BillEjb billEjb;

    List<Bill> bills;
    List<Bill> selectedBills;
    List<Institution> institutions;
    List<Institution> selectedInstitutions;
    private List<Item> items;
    private List<Item> selectedItems;
    private List<Department> departments;
    private List<PatientInvestigation> patientInvestigations;
    List<PharmaceuticalItemCategory> pharmaceuticalItemCategorys;
    List<PharmaceuticalItemCategory> selectedPharmaceuticalItemCategorys;

    double val1;
    double val2;
    double val3;
    double val4;

    boolean bool1;
    boolean bool2;
    boolean bool3;
    boolean bool4;
    private boolean vatableItem = true;
    private boolean editableVatableItem = true;
    private boolean vatableStatus = false;

    private ReportKeyWord reportKeyWord;
    private String item;
    private Category itemCategory;
    private Double vatPrecentage = 0.0;
    private DepartmentType departmentType;
    private SearchKeyword searchKeyword;
    CommonController commonController;
    private int manageCheckEnteredDataIndex;
    private String errors;
    private String suggestedSql;
    private String executionFeedback;

    Date fromDate;
    Date toDate;

    public void addWholesalePrices() {
        List<ItemBatch> ibs = itemBatchFacade.findAll();
        for (ItemBatch ib : ibs) {
            if (ib.getItem() != null) {
                ////System.out.println("ib.getItem().getName() = " + ib.getItem().getName());
            }
            if (ib.getWholesaleRate() == 0) {
                ////System.out.println("ib.getPurcahseRate = " + ib.getPurcahseRate());
                ////System.out.println("ib.getWholesaleRate() = " + ib.getWholesaleRate());
                ib.setWholesaleRate((ib.getPurcahseRate() / 115) * 108);
                itemBatchFacade.edit(ib);
                ////System.out.println("ib.getWholesaleRate() = " + ib.getWholesaleRate());
            } else {
                ////System.out.println("no change");
            }
        }
    }

    public void removeUnsedPharmaceuticalCategories() {
        Map m = new HashMap();
        String sql;

        sql = "SELECT c FROM PharmaceuticalItemCategory c ";

        Set<Category> allCats = new HashSet<>(categoryFacade.findByJpql(sql, m));

        sql = "SELECT i.category "
                + " FROM Item i "
                + " GROUP BY i.category";

        ////System.out.println("sql = " + sql);
        m = new HashMap();

        Set<Category> usedCats = new HashSet<>(categoryFacade.findByJpql(sql, m));

        ////System.out.println("Used Cats " + usedCats.size());
        ////System.out.println("All Cats after removing " + allCats.size());
        allCats.removeAll(usedCats);
        ////System.out.println("All Cats after removing " + allCats.size());

        for (Category c : allCats) {
            ////System.out.println("c = " + c);
            ////System.out.println("c.getName() = " + c.getName());
            c.setRetired(true);
            c.setRetiredAt(new Date());
            c.setRetireComments("Bulk1");
            categoryFacade.edit(c);
        }
    }

    @Inject
    InvestigationController investigationController;

    public void addInstitutionToInvestigationsWithoutInstitution() {
        List<Investigation> lst = investigationController.getItems();
        for (Investigation ix : lst) {
            if (ix.getInstitution() == null) {
                ix.setInstitution(ix.getDepartment().getInstitution());
                itemFacade.edit(ix);
            }
        }
    }

    public void detectWholeSaleBills() {
        String sql;
        Map m = new HashMap();
        sql = "select b from Bill b where (b.billType=:bt1 or b.billType=:bt2) order by b.id desc";
        m.put("bt1", BillType.PharmacySale);
        m.put("bt2", BillType.PharmacyPre);
        List<Bill> bs = getBillFacade().findByJpql(sql, m, 20);
        for (Bill b : bs) {
            ////System.out.println("b = " + b);
            ////System.out.println("b.getBillType() = " + b.getBillType());
            if (b.getBillItems().get(0).getRate() == b.getBillItems().get(0).getPharmaceuticalBillItem().getStock().getItemBatch().getWholesaleRate()) {
                ////System.out.println("whole sale bill");
                if (b.getBillType() == BillType.PharmacySale) {
                    b.setBillType(BillType.PharmacyWholeSale);
                }
                if (b.getBillType() == BillType.PharmacyPre) {
                    b.setBillType(BillType.PharmacyWholesalePre);
                }
                getBillFacade().edit(b);
            }

        }

    }

    public String navigateToCheckMissingFields() {
        return "/dataAdmin/missing_database_fields";
    }

    public void checkMissingFields1() {
        suggestedSql = "";
        errors = "";
        StringBuilder allErrors = new StringBuilder();

        for (Class<?> entityClass : findEntityClassNames()) {
            String entityName = entityClass.getSimpleName();
            try {
                itemFacade.executeQueryFirstResult(entityClass, "SELECT e FROM " + entityName + " e");
            } catch (PersistenceException pe) {
                Throwable cause = pe.getCause();
                while (cause != null && !(cause instanceof SQLSyntaxErrorException)) {
                    cause = cause.getCause();
                }
                if (cause != null) {
                    Matcher matcher = Pattern.compile("Unknown column '([^']+)' in 'field list'").matcher(cause.getMessage());
                    if (matcher.find()) {
                        String missingColumn = matcher.group(1);
                        errors += String.format("Entity: %s, Missing Column: %s\n", entityName, missingColumn);
                    }
                }
            } catch (Exception e) {
                // Handle other exceptions as needed
            }
        }
    }

    public void checkMissingFields() {
        suggestedSql = "";
        List<EntityFieldError> entityFieldErrors = new ArrayList<>();

        for (Class<?> entityClass : findEntityClassNames()) {
            String entityName = entityClass.getSimpleName();
            EntityFieldError entityFieldError = new EntityFieldError(entityName);
            String jpql = "SELECT e FROM " + entityName + " e";
            try {
                itemFacade.executeQueryFirstResult(entityClass, jpql);
            } catch (Exception e) {
                Throwable cause = e.getCause();
                while (cause != null && !(cause instanceof SQLSyntaxErrorException)) {
                    cause = cause.getCause();
                }
                if (cause != null) {
                    String message = cause.getMessage();
                    Pattern pattern = Pattern.compile("Unknown column '([^']+)' in 'field list'");
                    Matcher matcher = pattern.matcher(message);
                    while (matcher.find()) {
                        String missingColumn = matcher.group(1);
                        entityFieldError.addMissingField(missingColumn);
                    }
                    if (!entityFieldError.missingFields.isEmpty()) {
                        entityFieldErrors.add(entityFieldError);
                    }
                }
            } 
        }

        // Convert the list of EntityFieldError objects to a string
        StringBuilder errorsBuilder = new StringBuilder();
        for (EntityFieldError error : entityFieldErrors) {
            errorsBuilder.append(error.toString()).append("\n");
        }

        errors = errorsBuilder.toString();
    }

    public List<Class<?>> findEntityClassNames() {
        List<Class<?>> lst = new ArrayList<>();
        Reflections reflections = new Reflections("com.divudi.entity");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(Entity.class);
        lst.addAll(annotated);
        return lst;
    }

    public void runSqlToCreateFields() {
        String[] sqlStatements = suggestedSql.split("<br/>");
        StringBuilder executionResults = new StringBuilder();
        for (String sql : sqlStatements) {
            if (sql.trim().isEmpty()) {
                continue; // Skip empty lines
            }
            int result = itemFacade.executeNativeSql(sql);
            if (result >= 0) {
                // Assuming a positive result indicates success. Adjust based on your logic.
                executionResults.append("<br/>Successfully executed: ").append(sql);
            } else {
                // Handle failure case here. Adjust based on your logic.
                executionResults.append("<br/>Failed to execute: ").append(sql);
            }
        }
        executionFeedback = executionResults.toString();
    }

    public void addBillFeesToProfessionalCancelBills() {
        List<Bill> bs;
        String s;
        Map m = new HashMap();
        String newLine = System.getProperty("line.separator");

        s = "select b from Bill b where type(b) =:bct and b.billType=:bt and b.cancelledBill is not null order by b.id desc";

        m.put("bct", BilledBill.class);
        m.put("bt", BillType.PaymentBill);

        bs = billFacade.findByJpql(s, m);
        int i = 1;
        for (Bill b : bs) {
            Bill cb = b.getCancelledBill();
            int n = 0;
            for (BillItem bi : cb.getBillItems()) {
                bi.setPaidForBillFee(b.getBillItems().get(n).getPaidForBillFee());
                n++;
                billItemFacade.edit(bi);
            }
            ////System.out.println(newLine);
            ////System.out.println("Error number " + i + newLine);

            ////System.out.println("Bill Details " + newLine);
            ////System.out.println("\tIns Number = " + b.getInsId() + newLine);
            ////System.out.println("\tDep Number = " + b.getDeptId() + newLine);
            ////System.out.println("\tBill Date = " + b.getCreatedAt() + newLine);
            ////System.out.println("\tValue = " + b.getNetTotal() + newLine);
            ////System.out.println("Cancelled Bill Details " + newLine);
            ////System.out.println("\tIns Number = " + cb.getInsId() + newLine);
            ////System.out.println("\tDep Number = " + cb.getDeptId() + newLine);
            ////System.out.println("\tBill Date = " + cb.getCreatedAt() + newLine);
            ////System.out.println("\tValue = " + cb.getNetTotal() + newLine);
            i++;
        }
    }

    public void makeAllAmpsWithNullDepartmentTypeToPharmacyType() {
        String j = "Select a from Amp a where a.retired=false and a.departmentType is null";
        List<Item> amps = itemFacade.findByJpql(j);
        for (Item a : amps) {
            if (a instanceof Amp) {
                Amp amp = (Amp) a;
                if (amp.getDepartmentType() == null) {
                    amp.setDepartmentType(DepartmentType.Pharmacy);
                    itemFacade.edit(amp);
                }
            }
        }
    }

    public void addOPDBillFeesToProfessionalCancelBills() {
        List<Bill> bills;
        String s;
        Map m = new HashMap();

        s = "select distinct(b.cancelledBill) from BillItem bi join bi.bill b where "
                + " type(b) =:bct "
                + " and b.billType=:bt "
                + " and bi.referenceBill.billType=:rbt "
                + " and b.cancelledBill is not null "
                + " order by b.id ";

        m.put("bct", BilledBill.class);
        m.put("bt", BillType.PaymentBill);
        m.put("rbt", BillType.OpdBill);

//        bills = billFacade.findByJpql(s, m);
        bills = billFacade.findByJpql(s, m, 10);
        for (Bill cb : bills) {
            for (BillItem bi : cb.getBillItems()) {
                //System.out.println("bi = " + bi);
                //System.out.println("bi.getRetiredAt() = " + bi.getRetiredAt());
                //System.out.println("bi.isRetired() = " + bi.isRetired());
                //System.out.println("bi.getBill().getBillType() = " + bi.getBill().getBillType());
                if (bi.getReferanceBillItem() != null) {
                    if (bi.getReferanceBillItem().getBill() != null) {
                    } else {
                    }
                } else {
                }
                if (bi.getReferenceBill() != null) {
                }
                String sql;
                sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + bi.getId();
                List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);
                if (tmp.size() > 0) {
                } else {
                    sql = "Select bi From BillItem bi where bi.retired=false and bi.referanceBillItem.id=" + bi.getReferanceBillItem().getId();
                    BillItem billItem = getBillItemFacade().findFirstByJpql(sql);
                    sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + billItem.getId();
                    tmp = getBillFeeFacade().findByJpql(sql);
                    if (tmp.size() > 0) {
                        billSearch.cancelBillFee(cb, bi, tmp);
                    } else {
                        saveBillFee(billItem);
                        sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + billItem.getId();
                        tmp = getBillFeeFacade().findByJpql(sql);
                        billSearch.cancelBillFee(cb, bi, tmp);
                    }
                }
            }
        }
    }

    public void updateAllServiseAndInvestigationAsVatable() {
        String sql;
        Map m = new HashMap();
        List<Item> items = new ArrayList<>();

        sql = "select i from Item i "
                + " where i.retired=false"
                + " and i.vatable=false "
                + " and type(i) in :tps ";

        m.put("tps", Arrays.asList(new Class[]{Investigation.class, Service.class}));

        items = itemFacade.findByJpql(sql, m);

        int j = 1;
        for (Item i : items) {
            i.setVatable(true);
            i.setVatPercentage(15.0);
            itemFacade.edit(i);
            j++;
        }

    }

    public void saveBillFee(BillItem bi) {
        BillFee bf = new BillFee();
        bf.setCreatedAt(Calendar.getInstance().getTime());
        bf.setCreater(bi.getCreater());
        bf.setBillItem(bi);
        bf.setPatienEncounter(bi.getBill().getPatientEncounter());
        bf.setPatient(bi.getBill().getPatient());
        bf.setFeeValue(0 - bi.getNetValue());
        bf.setFeeGrossValue(0 - bi.getGrossValue());
        bf.setSettleValue(0 - bi.getNetValue());
        bf.setCreatedAt(new Date());
        bf.setBill(bi.getBill());

        if (bf.getId() == null) {
            getBillFeeFacade().create(bf);
        }
    }

    public void restBillNumber() {
        String sql = "Select b from BillNumber b where b.retired=false";
        List<BillNumber> list = billNumberFacade.findByJpql(sql);
        for (BillNumber b : list) {
            b.setRetired(true);
            b.setRetiredAt(new Date());
            b.setRetirer(sessionController.getLoggedUser());
            billNumberFacade.edit(b);
        }
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public void makeRateFromPurchaseRateToSaleInTransferBills() {
        List<Bill> bills;
        String j;
        Map m = new HashMap();

        j = "select b from Bill b where b.billType in :bt";

        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.PharmacyTransferIssue);
        bts.add(BillType.PharmacyTransferReceive);
        bts.add(BillType.PharmacyTransferRequest);
        ////System.out.println("arr" + bts);
        m.put("bt", bts);

//        j="select b from Bill b where (b.billType=: bts and b.billType=: bts2 and b.billType=: bts3)";
//        m.put("bts", BillType.PharmacyTransferIssue);
//        m.put("bts", BillType.PharmacyTransferReceive);
//        m.put("bts", BillType.PharmacyTransferRequest);
//        
        bills = getBillFacade().findByJpql(j, m);

        for (Bill b : bills) {
            ////System.out.println("b = " + b);
            double gt = 0;
            double nt = 0;
            for (BillItem bi : b.getBillItems()) {
                ////System.out.println("billitem" + b.getBillItems());
                //////System.out.println("bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate() = " + bi.getPharmaceuticalBillItem().getStock().getItemBatch());

                ////System.out.println("bi.getRate() = " + bi.getRate());
                ////System.out.println("bi.getNetValue() = " + bi.getNetValue());
                ////System.out.println("bi.getQty() = " + bi.getQty());
                ////System.out.println("bi.getNetValue() = " + bi.getNetValue());
                bi.setRate((double) fetchPharmacyuticalBillitem(bi));
                ////System.out.println("Rate" + fetchPharmacyuticalBillitem(bi));
                ////System.out.println("getRate" + bi.getRate());
                bi.setNetRate((double) fetchPharmacyuticalBillitem(bi));

                //                bi.setRate(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate());
//                bi.setNetRate(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate());
                ////System.out.println("rate" + bi.getNetRate());
                ////System.out.println("Net rate" + bi.getNetValue());
                bi.setNetValue(bi.getNetRate() * bi.getQty());
                bi.setGrossValue(bi.getNetValue());

                billItemFacade.edit(bi);

                gt += bi.getNetValue();
                nt += bi.getGrossValue();

            }
            b.setNetTotal(gt);

            billFacade.edit(b);
        }

    }

    public double fetchPharmacyuticalBillitem(BillItem bi) {
        String sql;
        Map m = new HashMap();

        sql = "Select ph.stock.itemBatch.retailsaleRate from PharmaceuticalBillItem ph where ph.billItem =:bi ";
        m.put("bi", bi);

        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, m);

    }

    public void createInwardServiceBillWithPaymentmethord() {
        bills = new ArrayList<>();
        String sql;
        Map temMap = new HashMap();
        sql = "select b from Bill b where "
                + " b.billType = :billType "
                + " and b.retired=false "
                + " and b.paymentMethod is not null "
                + " order by b.createdAt ";
        temMap.put("billType", BillType.InwardBill);

        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public void updateInwardServiceBillWithPaymentmethord() {
        if (selectedBills.isEmpty() || selectedBills == null) {
            JsfUtil.addErrorMessage("Nothing To Update");
            return;
        }
        for (Bill b : selectedBills) {
            b.setPaymentMethod(null);
            getBillFacade().edit(b);
        }
        createInwardServiceBillWithPaymentmethord();
    }

    public void fillAgencies() {
        institutions = institutionController.getAgencies();
    }

    public void changeCreditLimts() {
        if (selectedInstitutions.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing to Update");
        }
        if (bool1) {
            if (val1 == 0.0) {
                JsfUtil.addErrorMessage("U can't add 0.0 For S.C.L");
            }
        }
        if (bool2) {
            if (val2 == 0.0) {
                JsfUtil.addErrorMessage("U can't add 0.0 For A.C.L");
            }
        }
        if (bool3) {
            if (val3 == 0.0) {
                JsfUtil.addErrorMessage("U can't add 0.0 For M.C.L");
            }
        }
        if (bool4) {
            if (val4 == 0.0) {
                JsfUtil.addErrorMessage("U can't add 0.0 For Balance");
            }
        }
        for (Institution a : selectedInstitutions) {
            if (bool1) {
                if (val1 != 0.0) {
                    a.setStandardCreditLimit(val1);
                }
            }
            if (bool2) {
                if (val2 != 0.0) {
                    a.setAllowedCredit(val2);
                }
            }
            if (bool3) {
                if (val3 != 0.0) {
                    a.setMaxCreditLimit(val3);
                }
            }
            if (bool4) {
                if (val4 != 0.0) {
                    a.setBallance(val4);
                }
            }
            institutionFacade.edit(a);
        }
    }

    /**
     * Creates a new instance of DataAdministrationController
     */
    public DataAdministrationController() {
    }

    public void removeAllBillsAndBillItems() {
        for (PatientReportItemValue v : getPatientReportItemValueFacade().findAll()) {
            getPatientReportItemValueFacade().remove(v);
        }
        JsfUtil.addErrorMessage("Removed all patient report items values");
        for (PatientReport r : getPatientReportFacade().findAll()) {
            getPatientReportFacade().remove(r);
        }
        JsfUtil.addErrorMessage("Removed all patient reports");
        for (BillFee f : getBillFeeFacade().findAll()) {
            getBillFeeFacade().remove(f);
        }
        JsfUtil.addErrorMessage("Removed all bill fees");
    }

    public void updateStaffSelectedIntitutionZoneCode() {
        if (reportKeyWord.getInstitution() != null) {
            JsfUtil.addErrorMessage("Please Select Institution..");
        }
        if (reportKeyWord.getString() != null || reportKeyWord.getString().equals("")) {
            JsfUtil.addErrorMessage("Please Enter Zone ... ");
        }

        String sql;
        Map m = new HashMap();

        List<Staff> staffs = new ArrayList<>();

        sql = " select s from Staff s where s.retired=false "
                + " and s.person.retired =false "
                + " and s.workingDepartment.institution=:ins ";

        m.put("ins", reportKeyWord.getInstitution());

        staffs = getStaffFacade().findByJpql(sql, m);
        for (Staff s : staffs) {
            s.getPerson().setZoneCode(reportKeyWord.getString());
            if (s.getPerson().getNic() != null && !s.getPerson().getNic().equals("")) {
                if (s.getPerson().getNic().length() >= 9) {
                    String s1 = s.getPerson().getNic().substring(0, 9);
                    s.getPerson().setNic(s1 + "V");
                }

            }
            getPersonFacade().edit(s.getPerson());
        }
        JsfUtil.addSuccessMessage("Successfully Updated...");
        createInvestigationsAndServiceTable();

    }

    //get vatable items to data table
    public void createInvestigationsAndServiceTable() {
        String sql;
        Map m = new HashMap();

        sql = "select i from Item i where i.retired=false ";
        if (vatableItem) {
            sql += " and i.vatable=true";
        } else {
            sql += " and i.vatable!=true ";
        }

        if (Integer.parseInt(item) == 1) {
            sql += " and (type(i)=:ins or type(i)=:ser) ";
            m.put("ins", Investigation.class);
            m.put("ser", Service.class);

        }
        if (Integer.parseInt(item) == 2) {
            sql += " and type(i)=:ins ";
            m.put("ins", Investigation.class);
            if (itemCategory != null) {
                sql += " and i.category=:cat ";
                m.put("cat", itemCategory);
            }
        }
        if (Integer.parseInt(item) == 3) {
            sql += " and type(i)=:ser ";
            m.put("ser", Service.class);

            if (itemCategory != null) {
                sql += " and i.category=:cat2 ";
                m.put("cat2", itemCategory);
            }
        }
        sql += " order by i.name ";
        items = itemFacade.findByJpql(sql, m);

    }

    // Edit vat precentage of vatable items
    public void editVatableOfVatableItems() {

        if (errorCheck()) {
            return;
        }
        if (vatableStatus == true) {
            for (Item i : selectedItems) {
                i.setVatPercentage(vatPrecentage);
                i.setVatable(true);
                itemFacade.edit(i);
            }
            JsfUtil.addSuccessMessage("Succesfully Edited VAT For " + selectedItems.size() + " records with vat status ");
        } else {
            for (Item i : selectedItems) {

                i.setVatPercentage(vatPrecentage);
                i.setVatable(false);
                itemFacade.edit(i);

            }
            JsfUtil.addSuccessMessage("Succesfully Edited VAT For " + selectedItems.size() + " records and items as without vat items");
        }
        createInvestigationsAndServiceTable();
    }

    // Edit vat precentage to vatable items
    public void addVatableToVatableItems() {
        if (errorCheck()) {
            return;
        }
        if (vatableStatus == true) {
            for (Item i : selectedItems) {
                i.setVatPercentage(vatPrecentage);
                i.setVatable(true);
                itemFacade.edit(i);
            }
            JsfUtil.addSuccessMessage("Succesfully Added VAT For " + selectedItems.size() + " records with vat status ");
        } else {
            for (Item i : selectedItems) {

                i.setVatPercentage(vatPrecentage);
                i.setVatable(false);
                itemFacade.edit(i);

            }
            JsfUtil.addSuccessMessage("Succesfully Added VAT For " + selectedItems.size() + " records and items as without vat items");
        }
        createInvestigationsAndServiceTable();

    }

    // remove vat precentage from vatable items
    public void removeVatableFromVatableItems() {
        if (selectedItems.isEmpty()) {
            JsfUtil.addErrorMessage("Please Select the item");
            return;
        }
        if (vatableStatus == true) {
            for (Item i : selectedItems) {

                i.setVatPercentage(vatPrecentage);
                i.setVatable(true);
                itemFacade.edit(i);
            }
            JsfUtil.addSuccessMessage("Succesfully Removed VAT For " + selectedItems.size() + " records with vat status ");
        } else {
            for (Item i : selectedItems) {

                i.setVatPercentage(vatPrecentage);
                i.setVatable(false);
                itemFacade.edit(i);

            }
            JsfUtil.addSuccessMessage("Succesfully Removed VAT For " + selectedItems.size() + " records and items as without vat");
        }
    }

    //error check
    public boolean errorCheck() {

        if (selectedItems.isEmpty()) {
            JsfUtil.addErrorMessage("Please Select the item");
            return true;
        }
        ////System.out.println("1.vatPrecentage = " + vatPrecentage);
        if (vatPrecentage == null) {
            // //System.out.println("2.vatPrecentage = " + vatPrecentage);
            JsfUtil.addErrorMessage("Pleace Check your VAT presentage Value");
            return true;

        }
        if (vatPrecentage <= 0.0) {
            ////System.out.println("3.vatPrecentage = " + vatPrecentage);
            JsfUtil.addErrorMessage("Pleace Check your VAT presentage Value");
            return true;
        }
        return false;
    }

    public void createAllSessionAsVatable() {
        for (ServiceSession s : fetchAllSessions()) {
            s.setVatable(true);
            serviceSessionFacade.edit(s);
        }
    }

    public void createAllSessionAsChannel() {
        for (ServiceSession s : fetchAllSessions()) {
            s.setForBillType(BillType.Channel);
            serviceSessionFacade.edit(s);
        }
    }

    public void createAllSessionAsNotVatable() {
        for (ServiceSession s : fetchAllSessions()) {
            s.setVatable(false);
            serviceSessionFacade.edit(s);
        }
    }

    public List<ServiceSession> fetchAllSessions() {
        String sql;
        Map m = new HashMap();
        List<ServiceSession> sessions = new ArrayList<>();
        sql = "Select s From ServiceSession s "
                + " where s.retired=false "
                + " and s.originatingSession is null "
                + " and type(s)=:class ";
        m.put("class", ServiceSession.class);

        sessions = serviceSessionFacade.findByJpql(sql, m);

        sql = "Select s From ServiceSession s where s.retired=false "
                + " and s.originatingSession is not null "
                + " and s.sessionDate >=:cd "
                + " and type(s)=:class ";
        m.put("cd", new Date());

        sessions.addAll(serviceSessionFacade.findByJpql(sql, m, TemporalType.TIMESTAMP));

        return sessions;
    }

    public void fillDepartmentss() {

        String sql;
        Map m = new HashMap();

        sql = "select d from Department d "
                + " where d.retired=false ";

        if (departmentType != null) {
            sql += " and d.departmentType=:depT";
            m.put("depT", departmentType);
        }

        departments = getDepartmentFacade().findByJpql(sql, m);

    }

    public void createAllBillTypesFirstAndLastBill() {
        bills = new ArrayList<>();
        List<Object> objects = fetchAllBilledBillTypes();
        for (Object ob : objects) {
            BillType bt = (BillType) ob;
            Bill b = fetchBill(bt, true);
            if (b != null) {
                bills.add(b);
            }
            b = fetchBill(bt, false);
            if (b != null) {
                bills.add(b);
            }
        }
    }

    public void createDuplicateBillTableByBillType() {
        bills = new ArrayList<>();
        BillListWithTotals totals = billEjb.findBillsAndTotals(fromDate, toDate, new BillType[]{reportKeyWord.getBillType()}, null, null, null, null);
        for (Bill b : totals.getBills()) {
//            System.err.println("Time For In = " + new Date());
            for (Bill bb : totals.getBills()) {
                try {
                    if (b.getInsId().equals(bb.getInsId()) && !b.getId().equals(bb.getId())) {
                        bills.add(b);
                    }
                } catch (Exception e) {
                }
            }
//            System.err.println("Time For Out = " + new Date());
        }

    }

    public void createBillTable() {
        bills = new ArrayList<>();
        BillListWithTotals totals = billEjb.findBillsAndTotals(fromDate, toDate, new BillType[]{reportKeyWord.getBillType()}, null, null, null, null);
        for (Bill b : totals.getBills()) {
            bills.add(b);
        }

    }

    private List<Object> fetchAllBilledBillTypes() {
        List<Object> objects = new ArrayList<>();
        String sql;

        sql = " select distinct(b.billType) from BilledBill b where "
                + " b.retired=false "
                + " and b.billType is not null ";

        objects = getBillFacade().findObjectByJpql(sql);

        return objects;
    }

    private Bill fetchBill(BillType bt, boolean frist) {
        Bill b = new BilledBill();
        String sql;
        Map m = new HashMap();
        sql = " select b from Bill b where "
                + " b.retired=false "
                + " and b.billType=:bt ";
        if (frist) {
            sql += " order by b.createdAt ";
        } else {
            sql += " order by b.createdAt desc";
        }
        m.put("bt", bt);
        b = getBillFacade().findFirstByJpql(sql, m);
        return b;
    }

    public void createCodeSelectedCategory() {
        if (itemCategory == null) {
            JsfUtil.addErrorMessage("Please Select Category");
            return;
        }
        if (itemCategory.getDescription().equals("") || itemCategory.getDescription() == null) {
            JsfUtil.addErrorMessage("Please Check Category Code");
            return;
        }
        Map m = new HashMap();
        String sql = "select c from Amp c "
                + " where c.retired=false"
                + " and c.category=:cat "
                + " and (c.departmentType is null "
                + " or c.departmentType=:dep) "
                + " order by c.name";

        m.put("dep", DepartmentType.Pharmacy);
        m.put("cat", itemCategory);

        items = itemFacade.findByJpql(sql, m);

        int j = 1;

        for (Item i : items) {
            DecimalFormat df = new DecimalFormat("0000");
//            df=new DecimalFormat("####");
//            //System.out.println("df = " + df.format(j));
            i.setCode(itemCategory.getDescription() + df.format(j));
            itemFacade.edit(i);
            j++;
        }

    }

    public void createremoveAllCodes() {

        Map m = new HashMap();
        String sql = "select c from Amp c "
                + " where c.retired=false"
                //                + " and c.category=:cat "
                + " and (c.departmentType is null "
                + " or c.departmentType=:dep) "
                + " order by c.name";

        m.put("dep", DepartmentType.Pharmacy);
//        m.put("cat", itemCategory);

        items = itemFacade.findByJpql(sql, m);

        int j = 1;

        for (Item i : items) {
            i.setCode("");
            itemFacade.edit(i);
        }

    }

    public void createCodeSelectedCategoryStores() {
        if (itemCategory == null) {
            JsfUtil.addErrorMessage("Please Select Category");
            return;
        }
        if (itemCategory.getCode().equals("") || itemCategory.getCode() == null) {
            JsfUtil.addErrorMessage("Please Check Category Code");
            return;
        }
        Map m = new HashMap();
        String sql = "select c from Amp c "
                + " where c.retired=false "
                + " and c.category=:cat "
                + " and c.departmentType=:dep "
                + " order by c.name ";

        m.put("dep", DepartmentType.Store);
        m.put("cat", itemCategory);

        items = itemFacade.findByJpql(sql, m);

        int j = 1;

        for (Item i : items) {
            DecimalFormat df = new DecimalFormat("0000");
//            df=new DecimalFormat("####");
//            //System.out.println("df = " + df.format(j));
            i.setCode(itemCategory.getCode() + df.format(j));
            itemFacade.edit(i);
            j++;
        }

    }

    public void createremoveAllCodesStores() {
        Map m = new HashMap();
        String sql = "select c from Amp c "
                + " where c.retired=false "
                //                + " and c.category=:cat "
                + " and c.departmentType=:dep "
                + " order by c.name ";

        m.put("dep", DepartmentType.Store);
//        m.put("cat", itemCategory);

        items = itemFacade.findByJpql(sql, m);

        for (Item i : items) {
//            //System.out.println("i.getName() = " + i.getName());
//            DecimalFormat df = new DecimalFormat("0000");
//            //System.out.println("df = " + df.format(j));
//            df=new DecimalFormat("####");
//            //System.out.println("df = " + df.format(j));
            i.setCode("");
//            //System.out.println("i.getCode() = " + i.getCode());
            itemFacade.edit(i);
        }

    }

    public void createPatientInvestigationsTable() {
        Map temMap = new HashMap();
//        if (getSearchKeyword().getBillNo() == null && getSearchKeyword().getBillNo().trim().equals("")) {
//            JsfUtil.addErrorMessage("Please Select A bill Number");
//            return ;
//        }

        String sql = "select pi from PatientInvestigation pi join pi.investigation  "
                + " i join pi.billItem.bill b join b.patient.person p where "
                + " b.createdAt between :fromDate and :toDate  ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((i.name) like :itm )";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += " order by pi.id desc  ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        patientInvestigations = getPatientInvestigationFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void deActiveSelectedPatientReport(PatientReport pr) {
        pr.setRetired(true);
        pr.setRetiredAt(new Date());
        pr.setRetireComments("Admin Report Deactivated");
        getPatientReportFacade().edit(pr);
        JsfUtil.addSuccessMessage("Deactivated");
    }

    public void activeSelectedPatientReport(PatientReport pr) {
        pr.setRetired(false);
        pr.setRetiredAt(null);
        pr.setRetireComments("Admin Report Activated");
        getPatientReportFacade().edit(pr);
        JsfUtil.addSuccessMessage("Activated");
    }

    public void itemChangeListener() {
        itemCategory = null;
    }

    public Class[] getItemTypes() {

        Class[] items = {Investigation.class, Service.class};
        return items;

    }

    public void fillPharmacyCategory() {
        if (getReportKeyWord().getString().equals("0")) {
            pharmaceuticalItemCategorys = fetchPharmacyCategories(true);
        } else {
            pharmaceuticalItemCategorys = fetchPharmacyCategories(false);
        }
    }

    public void actveSelectedCategories() {
        if (selectedPharmaceuticalItemCategorys.isEmpty()) {
            JsfUtil.addErrorMessage("Please Select Category");
            return;
        }
        for (PharmaceuticalItemCategory c : selectedPharmaceuticalItemCategorys) {
            c.setRetired(false);
            c.setRetireComments("Category Bulk Activate");
            c.setRetiredAt(new Date());
            c.setRetirer(sessionController.getLoggedUser());
            getPharmaceuticalItemCategoryFacade().edit(c);
        }
        fillPharmacyCategory();
    }

    public void deActveSelectedCategories() {
        if (selectedPharmaceuticalItemCategorys.isEmpty()) {
            JsfUtil.addErrorMessage("Please Select Category");
            return;
        }
        for (PharmaceuticalItemCategory c : selectedPharmaceuticalItemCategorys) {
            c.setRetired(true);
            c.setRetireComments("Category Bulk De-Activate");
            c.setRetiredAt(new Date());
            c.setRetirer(sessionController.getLoggedUser());
            getPharmaceuticalItemCategoryFacade().edit(c);
        }
        fillPharmacyCategory();
    }

    private List<PharmaceuticalItemCategory> fetchPharmacyCategories(boolean active) {
//        items = getFacade().findAll("name", true);
        String sql = " select c from PharmaceuticalItemCategory c where ";
        if (active) {
            sql += " c.retired=false ";
        } else {
            sql += " c.retired!=false ";
        }
        sql += " order by c.description, c.name ";

        return getPharmaceuticalItemCategoryFacade().findByJpql(sql);
    }

//    Getters & Setters
    public PatientReportItemValueFacade getPatientReportItemValueFacade() {
        return patientReportItemValueFacade;
    }

    public void setPatientReportItemValueFacade(PatientReportItemValueFacade patientReportItemValueFacade) {
        this.patientReportItemValueFacade = patientReportItemValueFacade;
    }

    public PatientInvestigationItemValueFacade getPatientInvestigationItemValueFacade() {
        return patientInvestigationItemValueFacade;
    }

    public void setPatientInvestigationItemValueFacade(PatientInvestigationItemValueFacade patientInvestigationItemValueFacade) {
        this.patientInvestigationItemValueFacade = patientInvestigationItemValueFacade;
    }

    public PatientReportFacade getPatientReportFacade() {
        return patientReportFacade;
    }

    public void setPatientReportFacade(PatientReportFacade patientReportFacade) {
        this.patientReportFacade = patientReportFacade;
    }

    public PatientInvestigationFacade getPatientInvestigationFacade() {
        return patientInvestigationFacade;
    }

    public void setPatientInvestigationFacade(PatientInvestigationFacade patientInvestigationFacade) {
        this.patientInvestigationFacade = patientInvestigationFacade;
    }

    public BillComponentFacade getBillComponentFacade() {
        return billComponentFacade;
    }

    public void setBillComponentFacade(BillComponentFacade billComponentFacade) {
        this.billComponentFacade = billComponentFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public BillEntryFacade getBillEntryFacade() {
        return billEntryFacade;
    }

    public void setBillEntryFacade(BillEntryFacade billEntryFacade) {
        this.billEntryFacade = billEntryFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public List<Bill> getSelectedBills() {
        return selectedBills;
    }

    public void setSelectedBills(List<Bill> selectedBills) {
        this.selectedBills = selectedBills;
    }

    public List<Institution> getInstitutions() {
        if (institutions == null) {
            institutions = new ArrayList<>();
        }
        return institutions;
    }

    public void setInstitutions(List<Institution> institutions) {
        this.institutions = institutions;
    }

    public List<Institution> getSelectedInstitutions() {
        if (selectedInstitutions == null) {
            selectedInstitutions = new ArrayList<>();
        }
        return selectedInstitutions;
    }

    public void setSelectedInstitutions(List<Institution> selectedInstitutions) {
        this.selectedInstitutions = selectedInstitutions;
    }

    public double getVal1() {
        return val1;
    }

    public void setVal1(double val1) {
        this.val1 = val1;
    }

    public double getVal2() {
        return val2;
    }

    public void setVal2(double val2) {
        this.val2 = val2;
    }

    public double getVal3() {
        return val3;
    }

    public void setVal3(double val3) {
        this.val3 = val3;
    }

    public double getVal4() {
        return val4;
    }

    public void setVal4(double val4) {
        this.val4 = val4;
    }

    public boolean isBool1() {
        return bool1;
    }

    public void setBool1(boolean bool1) {
        this.bool1 = bool1;
    }

    public boolean isBool2() {
        return bool2;
    }

    public void setBool2(boolean bool2) {
        this.bool2 = bool2;
    }

    public boolean isBool3() {
        return bool3;
    }

    public void setBool3(boolean bool3) {
        this.bool3 = bool3;
    }

    public boolean isBool4() {
        return bool4;
    }

    public void setBool4(boolean bool4) {
        this.bool4 = bool4;
    }

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public Category getItemCategory() {
        return itemCategory;
    }

    public void setItemCategory(Category itemCategory) {
        this.itemCategory = itemCategory;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public boolean isVatableItem() {
        return vatableItem;
    }

    public void setVatableItem(boolean vatableItem) {
        this.vatableItem = vatableItem;
    }

    public List<Item> getSelectedItems() {
        return selectedItems;
    }

    public void setSelectedItems(List<Item> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public boolean isEditableVatableItem() {
        return editableVatableItem;
    }

    public void setEditableVatableItem(boolean editableVatableItem) {
        this.editableVatableItem = editableVatableItem;
    }

    public Double getVatPrecentage() {
        return vatPrecentage;
    }

    public void setVatPrecentage(Double vatPrecentage) {
        this.vatPrecentage = vatPrecentage;
    }

    public boolean isVatableStatus() {
        return vatableStatus;
    }

    public void setVatableStatus(boolean vatableStatus) {
        this.vatableStatus = vatableStatus;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public List<Department> getDepartments() {
        if (departments == null) {
            departments = new ArrayList<>();
        }
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

    public DepartmentType getDepartmentType() {
        return departmentType;
    }

    public void setDepartmentType(DepartmentType departmentType) {
        this.departmentType = departmentType;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = commonFunctionsController.getStartOfMonth(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = commonFunctionsController.getEndOfMonth(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public SearchKeyword getSearchKeyword() {
        if (searchKeyword == null) {
            searchKeyword = new SearchKeyword();
        }
        return searchKeyword;
    }

    public void setSearchKeyword(SearchKeyword searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public List<PatientInvestigation> getPatientInvestigations() {
        return patientInvestigations;
    }

    public void setPatientInvestigations(List<PatientInvestigation> patientInvestigations) {
        this.patientInvestigations = patientInvestigations;
    }

    public PharmaceuticalItemCategoryFacade getPharmaceuticalItemCategoryFacade() {
        return pharmaceuticalItemCategoryFacade;
    }

    public void setPharmaceuticalItemCategoryFacade(PharmaceuticalItemCategoryFacade pharmaceuticalItemCategoryFacade) {
        this.pharmaceuticalItemCategoryFacade = pharmaceuticalItemCategoryFacade;
    }

    public List<PharmaceuticalItemCategory> getPharmaceuticalItemCategorys() {
        if (pharmaceuticalItemCategorys == null) {
            pharmaceuticalItemCategorys = new ArrayList<>();
        }
        return pharmaceuticalItemCategorys;
    }

    public void setPharmaceuticalItemCategorys(List<PharmaceuticalItemCategory> pharmaceuticalItemCategorys) {
        this.pharmaceuticalItemCategorys = pharmaceuticalItemCategorys;
    }

    public List<PharmaceuticalItemCategory> getSelectedPharmaceuticalItemCategorys() {
        if (selectedPharmaceuticalItemCategorys == null) {
            selectedPharmaceuticalItemCategorys = new ArrayList<>();
        }
        return selectedPharmaceuticalItemCategorys;
    }

    public void setSelectedPharmaceuticalItemCategorys(List<PharmaceuticalItemCategory> selectedPharmaceuticalItemCategorys) {
        this.selectedPharmaceuticalItemCategorys = selectedPharmaceuticalItemCategorys;
    }

    public int getManageCheckEnteredDataIndex() {
        return manageCheckEnteredDataIndex;
    }

    public void setManageCheckEnteredDataIndex(int manageCheckEnteredDataIndex) {
        this.manageCheckEnteredDataIndex = manageCheckEnteredDataIndex;
    }

    public String navigateToAdminDataAdministration() {
        return "/dataAdmin/admin_data_administration?faces-redirect=true";
    }

    public String getErrors() {
        return errors;
    }

    public void setErrors(String errors) {
        this.errors = errors;
    }

    public String getSuggestedSql() {
        return suggestedSql;
    }

    public void setSuggestedSql(String suggestedSql) {
        this.suggestedSql = suggestedSql;
    }

    public String getExecutionFeedback() {
        return executionFeedback;
    }

    public void setExecutionFeedback(String executionFeedback) {
        this.executionFeedback = executionFeedback;
    }

    public class EntityFieldError {

        private String entityName;
        private Set<String> missingFields = new HashSet<>();

        public EntityFieldError(String entityName) {
            this.entityName = entityName;
        }

        public void addMissingField(String fieldName) {
            missingFields.add(fieldName);
        }

        @Override
        public String toString() {
            return "Entity: " + entityName + ", Missing Fields: " + String.join(", ", missingFields);
        }

        public String getEntityName() {
            return entityName;
        }

        public void setEntityName(String entityName) {
            this.entityName = entityName;
        }

        public Set<String> getMissingFields() {
            return missingFields;
        }

        public void setMissingFields(Set<String> missingFields) {
            this.missingFields = missingFields;
        }

    }

}
