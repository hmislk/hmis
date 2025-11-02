/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 *
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.InstitutionController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.SessionController;

import com.divudi.bean.inward.InwardBeanController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.PharmacyImportCol;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.ejb.PharmacyBean;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Service;
import com.divudi.core.entity.hr.StaffShift;
import com.divudi.core.entity.inward.InwardService;
import com.divudi.core.entity.inward.TimedItem;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.Atm;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.ItemsDistributors;
import com.divudi.core.entity.pharmacy.MeasurementUnit;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.PharmaceuticalItem;
import com.divudi.core.entity.pharmacy.PharmaceuticalItemCategory;
import com.divudi.core.entity.pharmacy.PharmaceuticalItemType;
import com.divudi.core.entity.pharmacy.StockHistory;
import com.divudi.core.entity.pharmacy.StoreItemCategory;
import com.divudi.core.entity.pharmacy.Vmp;
import com.divudi.core.entity.pharmacy.Vmpp;
import com.divudi.core.entity.pharmacy.Vtm;
import com.divudi.core.entity.pharmacy.VirtualProductIngredient;
import com.divudi.core.facade.AmpFacade;
import com.divudi.core.facade.AmppFacade;
import com.divudi.core.facade.AtmFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.ItemsDistributorsFacade;
import com.divudi.core.facade.MeasurementUnitFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.PharmaceuticalItemCategoryFacade;
import com.divudi.core.facade.PharmaceuticalItemFacade;
import com.divudi.core.facade.StaffShiftFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.facade.StockHistoryFacade;
import com.divudi.core.facade.StoreItemCategoryFacade;
import com.divudi.core.facade.VmpFacade;
import com.divudi.core.facade.VmppFacade;
import com.divudi.core.facade.VtmFacade;
import com.divudi.core.facade.VirtualProductIngredientFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.util.CommonFunctions;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Iterator;
import org.primefaces.model.file.UploadedFile;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class PharmacyItemExcelManager implements Serializable {

    /**
     *
     * EJBs
     *
     */
    @EJB
    private AtmFacade atmFacade;
    @EJB
    private VtmFacade vtmFacade;
    @EJB
    private AmpFacade ampFacade;
    @EJB
    private VmpFacade vmpFacade;
    @EJB
    private AmppFacade amppFacade;
    @EJB
    private VmppFacade vmppFacade;
    @EJB
    private VirtualProductIngredientFacade vtmInAmpFacade;
    @EJB
    private MeasurementUnitFacade muFacade;
    @EJB
    private PharmaceuticalItemCategoryFacade pharmaceuticalItemCategoryFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    StoreItemCategoryFacade storeItemCategoryFacade;
    @EJB
    PatientEncounterFacade patientEncounterFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private StockHistoryFacade stockHistoryFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private ItemFacade itemFacade;
    @Inject
    private InstitutionController institutionController;
    /**
     *
     * Controllers
     *
     */
    @Inject
    SessionController sessionController;

    @Inject
    PharmacyPurchaseController pharmacyPurchaseController;
    /**
     *
     * Values of Excel Columns
     *
     */
//        Category      0
//        Item Name     1
//        Code          2
//        Trade Name    3
//        Generic Name  4
//        Generic Product  5
//        Strength      6
//        Strength Unit 7
//        Pack Size     8
//        Issue Unit    9
//        Pack Unit     10
    //    Distributor 11
//        Manufacturer  12
//        Importer      13
//          14. Date of Expiary
//                15. Batch
//                16. Quentity
//                17. Purchase Price
//                18. Sale Price
    /**
     * Values of Excel Columns
     */
    private int number = 0;
    private int catCol = 1;
    private int ampCol = 2;
    private int codeCol = 3;
    private int barcodeCol = 4;
    private int vtmCol = 5;
    private int strengthOfIssueUnitCol = 6;
    private int strengthUnitCol = 7;
    private int issueUnitsPerPackCol = 8;
    private int issueUnitCol = 9;
    private int packUnitCol = 10;
    private int distributorCol = 11;
    private int manufacturerCol = 12;
    private int importerCol = 13;
    private int doeCol = 14;
    private int batchCol = 15;
    private int stockQtyCol = 16;
    private int pruchaseRateCol = 17;
    private int saleRateCol = 18;
    private List<PharmacyImportCol> itemNotPresent;
    private List<String> itemsWithDifferentGenericName;
    private List<String> itemsWithDifferentCode;

    private int startRow = 1;
    /**
     * DataModals
     *
     */
    List<Vtm> vtms;
    List<Amp> amps;
    List<Ampp> ampps;
    /**
     *
     * Uploading File
     *
     */
    private UploadedFile file;

    private StringBuilder message;

    /**
     * Creates a new instance of DemographyExcelManager
     */
    public PharmacyItemExcelManager() {
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getDistributorCol() {
        return distributorCol;
    }

    public void setDistributorCol(int distributorCol) {
        this.distributorCol = distributorCol;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public void removeDuplicateAmpps() {
        List<Ampp> temAmpps = getAmppFacade().findAll(true);
        Set<String> uniqueNames = new HashSet<>();

        for (Ampp ampp : temAmpps) {
            if (!ampp.isRetired()) {
                if (uniqueNames.contains(ampp.getName())) {
                    ampp.setRetired(true);
                    getAmppFacade().edit(ampp);
                } else {
                    uniqueNames.add(ampp.getName());
                }
            }
        }
    }

    @EJB
    private BillFacade billFacade;

//    public void resetGrnValue() {
//        String sql;
//        Map temMap = new HashMap();
//
//        sql = "select b from Bill b where (type(b)=:class) "
//                + " and b.billType = :billType ";
//
//        temMap.put("class", BilledBill.class);
//        temMap.put("billType", BillType.PharmacyGrnBill);
//        //temMap.put("dep", getSessionController().getDepartment());
//        List<Bill> bills = getBillFacade().findByJpql(sql, temMap);
//
//        for (Bill b : bills) {
//            if (b.getNetTotal() > 0) {
//                b.setNetTotal(0 - b.getNetTotal());
//                b.setTotal(0 - b.getTotal());
//                getBillFacade().edit(b);
//            }
//        }
//
//        sql = "select b from Bill b where (type(b)=:class) "
//                + " and b.billType = :billType ";
//
//        temMap.put("class", CancelledBill.class);
//        temMap.put("billType", BillType.PharmacyGrnBill);
//        //temMap.put("dep", getSessionController().getDepartment());
//        bills = getBillFacade().findByJpql(sql, temMap);
//
//        for (Bill b : bills) {
//            if (b.getNetTotal() < 0) {
//                b.setNetTotal(0 - b.getNetTotal());
//                b.setTotal(0 - b.getTotal());
//                getBillFacade().edit(b);
//            }
//        }
//
//    }
//    public void resetBillNo() {
//        String sql;
//        Map temMap = new HashMap();
//
//        sql = "select b from Bill b where b.createdAt between :fd and :td ";
//
//        Calendar cal = Calendar.getInstance();
//        cal.setTime(new Date());
//        cal.add(Calendar.DATE, -40);
//        temMap.put("fd", cal.getTime());
//        temMap.put("td", new Date());
//        //temMap.put("dep", getSessionController().getDepartment());
//        List<Bill> bills = getBillFacade().findByJpql(sql, temMap);
//
//        for (Bill b : bills) {
//            String str = "";
//            //Reset Institution ID
//            if (b.getInsId() != null) {
//                str = b.getInsId().replace('\\', '/');
//                //  System.err.println("Ins No " + b.getInsId() + " : " + str);
//                b.setInsId(str);
//            }
//
//            //Reset Department ID
//            if (b.getDeptId() != null) {
//                str = b.getDeptId().replace('\\', '/');
//                //    System.err.println("Dept No " + b.getDeptId() + " : " + str);
//                b.setDeptId(str);
//            }
//
//            getBillFacade().edit(b);
//        }
//
//    }
    public void resetSessionBillNumberType() {
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Item b where type(b)=:tp ";
        temMap.put("tp", Service.class);
        List<Item> list = getItemFacade().findByJpql(sql, temMap);

        for (Item i : list) {
            i.setSessionNumberType(null);
            getItemFacade().edit(i);
        }
    }

    public void resetInwardChargeType() {
        resetInwardChargeTypeInvesitagation();
        resetInwardChargeTypeService();
        resetInwardChargeTypeServiceDefault();
        resetInwardChargeTypeRoomTypeDefault();
    }

    @Inject
    InwardBeanController inwardBeanController;

    public void resetFinalBillPaidValue() {
        String sql = "Select b From BilledBill b "
                + " where b.retired=false"
                + " and b.billType=:btp"
                + " and b.cancelled=false";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardFinalBill);

        List<Bill> bills = billFacade.findByJpql(sql, hm);

        for (Bill b : bills) {
            inwardBeanController.updateFinalFill(b.getPatientEncounter());
        }

    }

    public void restCreditUsedAmount() {
        String sql = "Select p from PatientEncounter p "
                + " where p.retired=false "
                + " and p.creditUsedAmount=0 "
                + " and p.paymentMethod=:pm ";
        HashMap hm = new HashMap();
        hm.put("pm", PaymentMethod.Credit);
        List<PatientEncounter> list = patientEncounterFacade.findByJpql(sql, hm);

        for (PatientEncounter pe : list) {
            if (pe.getFinalBill() == null) {
                continue;
            }
            double net = pe.getFinalBill().getNetTotal() - pe.getFinalBill().getPaidAmount();
            pe.setCreditUsedAmount(net);

            patientEncounterFacade.edit(pe);

        }
    }

    public void resetInwardChargeTypeInvesitagation() {
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Item b where type(b)=:tp ";
        temMap.put("tp", Investigation.class);
        List<Item> list = getItemFacade().findByJpql(sql, temMap);

        for (Item i : list) {
            i.setInwardChargeType(InwardChargeType.Laboratory);
            getItemFacade().edit(i);
        }
    }

//    @Deprecated
//    public void upadatePaidValueInBillFee() {
//
//        Map m = new HashMap();
//
//        String sql = "select bf,bi"
//                + " from BillItem bi join  bi.paidForBillFee bf "
//                + " where bi.retired=false "
//                + " and bf.retired=false"
//                + " and (bf.paidValue!=bi.netValue "
//                + " or bf.paidValue=0 "
//                + " or bf.staff.speciality is null)"
//                + " and bi.bill.billType=:btp "
//                + " and (bf.bill.billType=:refBtp1"
//                + " or bf.bill.billType=:refBtp2)";
//
//        m.put("btp", BillType.PaymentBill);
//        m.put("refBtp1", BillType.InwardBill);
//        m.put("refBtp2", BillType.InwardProfessional);
//
//        List<Object[]> list2 = billFeeFacade.findObjectsArrayByJpql(sql, m, TemporalType.DATE);
//
//        for (Object[] obj : list2) {
//            BillFee billFee = (BillFee) obj[0];
//            BillItem billItem = (BillItem) obj[1];
//
//            billFee.setPaidValue(billFee.getFeeValue());
//            billFeeFacade.edit(billFee);
//        }
//
//    }
    public void resetMarginAndDiscountAndNetTotal() {
        String sql = "Select b from Bill b "
                + " where b.retired=false "
                + " and b.billType=:btp ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.PharmacyBhtPre);
        List<Bill> list = billFacade.findByJpql(sql, hm);

        for (Bill b : list) {
            sql = "select sum(b.marginValue),"
                    + " sum(b.discount),"
                    + " sum(b.netValue)"
                    + " from BillItem b "
                    + " where b.retired=false"
                    + " and b.bill=:bil";
            hm.clear();
            hm.put("bil", b);

            Object[] obj = billItemFacade.findAggregateModified(sql, hm, TemporalType.DATE);

            if (obj == null) {
                continue;
            }

            Double[] dbl = Arrays.copyOf(obj, obj.length, Double[].class);

            b.setMargin(dbl[0]);
            b.setDiscount(dbl[1]);
            b.setNetTotal(dbl[2]);
            billFacade.edit(b);

        }
    }

    public void resetInwardChargeTypeService() {
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Item b "
                + " where type(b)=:tp "
                + " and b.inwardChargeType=:inw ";
        temMap.put("tp", Service.class);
        temMap.put("inw", InwardChargeType.Investigations);
        List<Item> list = getItemFacade().findByJpql(sql, temMap);

        for (Item i : list) {
            i.setInwardChargeType(InwardChargeType.MedicalServices);
            getItemFacade().edit(i);
        }
    }

    public void resetInwardChargeTypeServiceDefault() {
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Item b "
                + " where (type(b)=:tp1 "
                + " or type(b)=:tp2"
                + " or type(b)=:tp3 )"
                + " and b.inwardChargeType is null ";
        temMap.put("tp1", Service.class);
        temMap.put("tp2", TimedItem.class);
        temMap.put("tp3", InwardService.class);
        List<Item> list = getItemFacade().findByJpql(sql, temMap);

        for (Item i : list) {
            i.setInwardChargeType(InwardChargeType.MedicalServices);
            getItemFacade().edit(i);
        }
    }

    public void resetInwardChargeTypeRoomTypeDefault() {
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Item b "
                + " where b.inwardChargeType=:inw1"
                + " or  b.inwardChargeType=:inw2 "
                + " or  b.inwardChargeType=:inw3 "
                + " or  b.inwardChargeType=:inw4 "
                + " or  b.inwardChargeType=:inw5 "
                + " or  b.inwardChargeType=:inw6 "
                + " or  b.inwardChargeType=:inw7 "
                + " or  b.inwardChargeType=:inw8 "
                + " or  b.inwardChargeType=:inw9 "
                + " or  b.inwardChargeType=:inw10 ";

        temMap.put("inw1", InwardChargeType.AdministrationCharge);
        temMap.put("inw2", InwardChargeType.LinenCharges);
        temMap.put("inw3", InwardChargeType.MedicalCareICU);
        temMap.put("inw4", InwardChargeType.MOCharges);
        temMap.put("inw5", InwardChargeType.MaintainCharges);
        temMap.put("inw6", InwardChargeType.NursingCharges);
        temMap.put("inw7", InwardChargeType.RoomCharges);
        //Others
        temMap.put("inw8", InwardChargeType.Medicine);
        temMap.put("inw9", InwardChargeType.ProfessionalCharge);
        temMap.put("inw10", InwardChargeType.MedicalCare);

        List<Item> list = getItemFacade().findByJpql(sql, temMap);
        for (Item i : list) {
            i.setInwardChargeType(InwardChargeType.MedicalServices);
            getItemFacade().edit(i);
        }
    }

//    public void resetPaymentmethod() {
//        String sql;
//        Map temMap = new HashMap();
//
//        sql = "select b from Bill b where b.paymentMethod is null";
//
//        List<Bill> list = getBillFacade().findByJpql(sql, temMap);
//        System.err.println("Size  " + list.size());
//        //  int ind = 1;
//        for (Bill i : list) {
//            //   System.err.println("index " + ind++);
//            //    System.err.println("Bill  " + i);
//            if (i.getPaymentScheme() != null) {
//                i.setPaymentMethod(i.getPaymentScheme().getPaymentMethod());
//                getBillFacade().edit(i);
//            }
//        }
//    }
//    public void resetPharmacyPurhcaseCancelPayentScheme() {
//        String sql;
//        Map temMap = new HashMap();
//
//        sql = "select b from Bill b where (type(b)=:class) "
//                + " and b.billType = :billType ";
//
//        temMap.put("class", BilledBill.class);
//        temMap.put("billType", BillType.PharmacyPurchaseBill);
//        //temMap.put("dep", getSessionController().getDepartment());
//        List<Bill> bills = getBillFacade().findByJpql(sql, temMap);
//
//        for (Bill b : bills) {
//            System.err.println("Billed "+b.getPaymentScheme());
//            System.err.println("Cancelled "+b.getCancelledBill().getPaymentScheme());
//        }
//
//    }
//    public void resetGrnReference() {
//        String sql;
//        Map temMap = new HashMap();
//
//        sql = "select b from Bill b where (type(b)=:class) "
//                + " and b.billType = :billType ";
//
//        temMap.put("class", BilledBill.class);
//        temMap.put("billType", BillType.PharmacyGrnBill);
//        //temMap.put("dep", getSessionController().getDepartment());
//        List<Bill> bills = getBillFacade().findByJpql(sql, temMap);
//        int index = 1;
//        for (Bill b : bills) {
//            if (b.getReferenceBill().getBillType() == BillType.PharmacyOrder) {
//                Bill refApproved = b.getReferenceBill().getReferenceBill();
//
//                b.setReferenceBill(refApproved);
//                getBillFacade().edit(b);
//
//            }
//
//        }
//
//    }
    @Inject
    ItemController itemController;

    public ItemController getItemController() {
        return itemController;
    }

    public void setItemController(ItemController itemController) {
        this.itemController = itemController;
    }

    private Item fetchPharmacyItem(Item item) {
        Map m = new HashMap();
        m.put("dep", DepartmentType.Store);
        m.put("nm", "%" + item.getName().toUpperCase() + "%");

        String sql = " select c from Amp c "
                + " where c.retired=false "
                + " and (c.departmentType is null"
                + " or c.departmentType!=:dep )"
                + " and (c.name) like :nm ";
        return getItemFacade().findFirstByJpql(sql, m);
    }

    @EJB
    StockFacade stockFacade;

    public StockFacade getStockFacade() {
        return stockFacade;
    }

    public void setStockFacade(StockFacade stockFacade) {
        this.stockFacade = stockFacade;
    }

    public double fetchStockQty(Item item) {

        String sql;
        Map m = new HashMap();
        m.put("i", item);
        sql = "select sum(s.stock) from Stock s where s.itemBatch.item=:i";
        return getStockFacade().findDoubleByJpql(sql, m);
    }

    public void removeStoreItem() {
        for (Item storeItem : getItemController().fetchStoreItem()) {

            Item pharmacyItem = fetchPharmacyItem(storeItem);

            if (pharmacyItem == null) {
                continue;
            }

            double qty = fetchStockQty(pharmacyItem);

            if (qty != 0) {
                JsfUtil.addErrorMessage(pharmacyItem.getName() + " NOT Removed Beacause there is stock");
                continue;
            }

            pharmacyItem.setRetired(true);
            pharmacyItem.setRetireComments("Bulk Remove");
            pharmacyItem.setRetiredAt(new Date());
            pharmacyItem.setRetirer(getSessionController().getLoggedUser());
            getItemFacade().edit(pharmacyItem);
        }
    }

    public void updateCancelGRN() {
//        CancelledBill b=new CancelledBill();
//        b.getNetTotal();
//        b.getGrnNetTotal();
//        b.getBillType();

        List<Bill> bills;

        String sql;
        Map m = new HashMap();

        sql = " select b from CancelledBill b where "
                + " b.billType=:bt ";

        m.put("bt", BillType.StoreGrnBill);

        bills = getBillFacade().findByJpql(sql, m);
        for (Bill b : bills) {
            b.setGrnNetTotal(0 - b.getBilledBill().getGrnNetTotal());
            getBillFacade().edit(b);
        }

    }

    @EJB
    StaffShiftFacade staffShiftFacade;

    public void updateStaffShiftWithRoster() {
        String sql = "select s from StaffShift s"
                + "  where s.retired=false"
                + "  and s.staff is not null "
                + " and s.roster is null";
        List<StaffShift> list = staffShiftFacade.findByJpql(sql);
        if (list == null) {
            return;
        }

        for (StaffShift stf : list) {
            stf.setRoster(stf.getStaff().getRoster());
            staffShiftFacade.edit(stf);
        }

    }

    public void updateBillDeptID() {
        BillType[] btps = {BillType.AgentPaymentReceiveBill, BillType.CashRecieveBill, BillType.GrnPaymentPre, BillType.PettyCash};
        List<BillType> btpList = Arrays.asList(btps);
        String sql = "select s from Bill s"
                + "  where s.retired=false "
                + "  and s.billType in :btp "
                + "  and s.deptId is null ";
        HashMap hm = new HashMap();
        hm.put("btp", btpList);
        List<Bill> list = billFacade.findByJpql(sql, hm);
        if (list == null) {
            return;
        }

        for (Bill stf : list) {
            stf.setDeptId("gen");
            billFacade.edit(stf);
        }

    }

//    public void resetQtyValue() {
//        String sql = "Select p from PharmaceuticalBillItem p where p.billItem.retired=false ";
//
//        List<PharmaceuticalBillItem> lis = getPharmaceuticalBillItemFacade().findByJpql(sql);
//
//        for (PharmaceuticalBillItem ph : lis) {
//            if (ph.getBillItem() == null || ph.getBillItem().getBill() == null
//                    || ph.getBillItem().getBill().getBillType() == null) {
//                continue;
//            }
//            switch (ph.getBillItem().getBill().getBillType()) {
//                case PharmacyGrnBill:
//                    if (ph.getBillItem().getBill() instanceof BilledBill) {
//                        if (ph.getQtyInUnit() < 0) {
//                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
//                        }
//                        if (ph.getFreeQtyInUnit() < 0) {
//                            ph.setFreeQtyInUnit(0 - ph.getFreeQtyInUnit());
//                        }
//                    }
//                    if (ph.getBillItem().getBill() instanceof CancelledBill) {
//                        if (ph.getQtyInUnit() > 0) {
//                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
//                        }
//                        if (ph.getFreeQtyInUnit() > 0) {
//                            ph.setFreeQtyInUnit(0 - ph.getFreeQtyInUnit());
//                        }
//                    }
//                    break;
//                case PharmacyGrnReturn:
//                    if (ph.getBillItem().getBill() instanceof BilledBill) {
//                        if (ph.getQtyInUnit() > 0) {
//                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
//                        }
//                        if (ph.getFreeQtyInUnit() > 0) {
//                            ph.setFreeQtyInUnit(0 - ph.getFreeQtyInUnit());
//                        }
//                    }
//                    if (ph.getBillItem().getBill() instanceof CancelledBill) {
//                        if (ph.getQtyInUnit() < 0) {
//                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
//                        }
//                        if (ph.getFreeQtyInUnit() < 0) {
//                            ph.setFreeQtyInUnit(0 - ph.getFreeQtyInUnit());
//                        }
//                    }
//                    break;
//                case PharmacyTransferIssue:
//                    if (ph.getBillItem().getBill() instanceof BilledBill) {
//                        if (ph.getQtyInUnit() > 0) {
//                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
//                        }
//                    }
//                    if (ph.getBillItem().getBill() instanceof CancelledBill) {
//                        if (ph.getQtyInUnit() < 0) {
//                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
//                        }
//                    }
//                    break;
//                case PharmacyTransferReceive:
//                    if (ph.getBillItem().getBill() instanceof BilledBill) {
//                        if (ph.getQtyInUnit() < 0) {
//                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
//                        }
//                    }
//                    if (ph.getBillItem().getBill() instanceof CancelledBill) {
//                        if (ph.getQtyInUnit() > 0) {
//                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
//                        }
//                    }
//                    break;
//                case PharmacyPurchaseBill:
//                    if (ph.getBillItem().getBill() instanceof BilledBill) {
//                        if (ph.getQtyInUnit() < 0) {
//                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
//                        }
//                        if (ph.getFreeQtyInUnit() < 0) {
//                            ph.setFreeQtyInUnit(0 - ph.getFreeQtyInUnit());
//                        }
//                    }
//                    if (ph.getBillItem().getBill() instanceof CancelledBill) {
//                        if (ph.getQtyInUnit() > 0) {
//                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
//                        }
//                        if (ph.getFreeQtyInUnit() > 0) {
//                            ph.setFreeQtyInUnit(0 - ph.getFreeQtyInUnit());
//                        }
//                    }
//                    break;
//                case PharmacyPre:
//                    if (ph.getBillItem().getBill() instanceof BilledBill) {
//                        if (ph.getQtyInUnit() > 0) {
//                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
//                        }
//                    }
//                    if (ph.getBillItem().getBill() instanceof CancelledBill) {
//                        if (ph.getQtyInUnit() < 0) {
//                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
//                        }
//                    }
//                    break;
//                case PharmacySale:
//                    if (ph.getBillItem().getBill() instanceof BilledBill) {
//                        if (ph.getQtyInUnit() > 0) {
//                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
//                        }
//                    }
//                    if (ph.getBillItem().getBill() instanceof CancelledBill) {
//                        if (ph.getQtyInUnit() < 0) {
//                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
//                        }
//                    }
//                    break;
//
//            }
//
//            getPharmaceuticalBillItemFacade().edit(ph);
//
//        }
//
//    }
//    public void resetTransferIssueValue() {
//        String sql;
//        Map temMap = new HashMap();
//
//        sql = "select b from Bill b where b.billType = :billType or b.billType = :billType2  ";
//
//        temMap.put("billType", BillType.PharmacyTransferIssue);
//        temMap.put("billType2", BillType.PharmacyTransferReceive);
//        //temMap.put("dep", getSessionController().getDepartment());
//        List<Bill> bills = getBillFacade().findByJpql(sql, temMap);
//
//        for (Bill b : bills) {
//            temMap.clear();
//            double totalBySql = 0;
//            sql = "select sum(bi.pharmaceuticalBillItem.itemBatch.purcahseRate * bi.pharmaceuticalBillItem.qty) "
//                    + " from BillItem bi where bi.retired=false and bi.bill=:b ";
//            temMap.put("b", b);
//            totalBySql = getBillItemFacade().findDoubleByJpql(sql, temMap);
////            if (b.getNetTotal() != totalBySql) {
//
//            b.setNetTotal(totalBySql);
//            getBillFacade().edit(b);
////            }
//        }
//
//    }
//
//    public void correctIssueToUnit1() {
//        Map m = new HashMap();
//        String sql;
//        m.put("bt", BillType.PharmacyIssue);
//
//        sql = "select b "
//                + " from BillItem b "
//                + " where b.retired=false "
//                + " and b.bill.billType=:bt ";
//
//        List<BillItem> list = billItemFacade.findByJpql(sql, m);
//        if (list == null) {
//            return;
//        }
//
//        for (BillItem obj : list) {
//            obj.setMarginValue(0 - obj.getDiscount());
//            obj.setDiscount(0);
//            billItemFacade.edit(obj);
//        }
//
//        m = new HashMap();
//        m.put("bt", BillType.PharmacyIssue);
//
//        sql = "select b "
//                + " from Bill b "
//                + " where b.retired=false "
//                + " and b.billType=:bt ";
//
//        List<Bill> listB = billFacade.findByJpql(sql, m);
//        if (listB == null) {
//            return;
//        }
//
//        for (Bill obj : listB) {
//            obj.setMargin(0 - obj.getDiscount());
//            obj.setDiscount(0);
//            billFacade.edit(obj);
//        }
//
//    }
//
//    public void correctIssueToUnit2() {
//        Map m = new HashMap();
//        String sql;
//        m.put("bt", BillType.PharmacyIssue);
//
//        sql = "select b "
//                + " from BillItem b "
//                + " where b.retired=false "
//                + " and b.bill.billType=:bt ";
//
//        List<BillItem> list = billItemFacade.findByJpql(sql, m);
//        if (list == null) {
//            return;
//        }
//
//        for (BillItem obj : list) {
//            IssueRateMargins issueRateMargins = pharmacyBean.fetchIssueRateMargins(obj.getBill().getDepartment(), obj.getBill().getToDepartment());
//            if (issueRateMargins == null) {
//                continue;
//            }
//
//            if (issueRateMargins.isAtPurchaseRate()) {
//                obj.setNetRate(obj.getPharmaceuticalBillItem().getStock().getItemBatch().getPurcahseRate());
//            } else {
//                obj.setNetRate(obj.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate());
//            }
//
//            double value = obj.getNetRate() * obj.getPharmaceuticalBillItem().getQty();
//            //System.out.println("*************************************");
//            //System.out.println("*************************************");
//            //System.out.println("*************************************");
//            //System.out.println("*************************************");
//
//            if (obj.getGrossValue() > 0) {
//                obj.setGrossValue(Math.abs(value));
//            } else {
//                obj.setGrossValue(0 - Math.abs(value));
//            }
//
//            obj.setMarginValue(0);
//            obj.setNetValue(obj.getGrossValue());
//
//            billItemFacade.edit(obj);
//        }
//
//        m = new HashMap();
//        m.put("bt", BillType.PharmacyIssue);
//
//        sql = "select b "
//                + " from Bill b "
//                + " where b.retired=false "
//                + " and b.billType=:bt ";
//
//        List<Bill> listB = billFacade.findByJpql(sql, m);
//        if (listB == null) {
//            return;
//        }
//
//        for (Bill obj : listB) {
//            m = new HashMap();
//            sql = "select sum(b.grossValue)"
//                    + " from BillItem b "
//                    + " where b.retired=false "
//                    + " and b.bill=:bt ";
//            m.put("bt", obj);
//            Double dbl = billFacade.findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
//
//            if (dbl == null) {
//                return;
//            }
//
//            obj.setTotal(dbl);
//            obj.setMargin(0);
//            obj.setNetTotal(dbl);
//
//            billFacade.edit(obj);
//
//        }
//
//    }
//
//    public void correctIssueToUnit3() {
//        Map m = new HashMap();
//        String sql;
//        m.put("bt", BillType.PharmacyIssue);
//
//        sql = "select b "
//                + " from BillItem b "
//                + " where b.retired=false "
//                + " and b.bill.billType=:bt ";
//
//        List<BillItem> list = billItemFacade.findByJpql(sql, m);
//        if (list == null) {
//            return;
//        }
//
//        for (BillItem obj : list) {
//            IssueRateMargins issueRateMargins = pharmacyBean.fetchIssueRateMargins(obj.getBill().getDepartment(), obj.getBill().getToDepartment());
//            if (issueRateMargins == null) {
//                continue;
//            }
//
//            if (issueRateMargins.isAtPurchaseRate()) {
//                if (obj.getPharmaceuticalBillItem().getItemBatch().getPurcahseRate() != obj.getRate()) {
//                    obj.setNetRate(obj.getRate());
//                }
//            }
//
//            double value = obj.getNetRate() * obj.getPharmaceuticalBillItem().getQty();
////            //System.out.println("*************************************");
////            System.err.println("BillClass " + obj.getBill().getClass());
////            System.err.println("QTY " + obj.getPharmaceuticalBillItem().getQty());
////            System.err.println("Net Rate " + obj.getNetRate());
////            System.err.println("Gross " + obj.getGrossValue());
////            System.err.println("Value " + value);
//
//            if (obj.getGrossValue() > 0) {
//                obj.setGrossValue(Math.abs(value));
//            } else {
//                obj.setGrossValue(0 - Math.abs(value));
//            }
//
//            obj.setMarginValue(0);
//            obj.setNetValue(obj.getGrossValue());
//
////            billItemFacade.edit(obj);
//        }
//
////        m = new HashMap();
////        m.put("bt", BillType.PharmacyIssue);
////
////        sql = "select b "
////                + " from Bill b "
////                + " where b.retired=false "
////                + " and b.billType=:bt ";
////
////        List<Bill> listB = billFacade.findByJpql(sql, m);
////        if (listB == null) {
////            return;
////        }
////
////        for (Bill obj : listB) {
////            m = new HashMap();
////            sql = "select sum(b.grossValue)"
////                    + " from BillItem b "
////                    + " where b.retired=false "
////                    + " and b.bill=:bt ";
////            m.put("bt", obj);
////            Double dbl = billFacade.findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
////
////            if (dbl == null) {
////                System.err.println("ERR");
////                return;
////            }
////
////            obj.setTotal(dbl);
////            obj.setMargin(0);
////            obj.setNetTotal(dbl);
////
////            billFacade.edit(obj);
////
////        }
//    }
    private StockHistory getPreviousStockHistoryByBatch(ItemBatch itemBatch, Department department, Date date) {
        String sql = "Select sh from StockHistory sh where sh.retired=false and"
                + " sh.itemBatch=:itmB and sh.department=:dep and sh.pbItem.billItem.createdAt<:dt "
                + " order by sh.pbItem.billItem.createdAt desc";
        HashMap hm = new HashMap();
        hm.put("itmB", itemBatch);
        hm.put("dt", date);
        hm.put("dep", department);
        return getStockHistoryFacade().findFirstByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    private PharmaceuticalBillItem getPreviousPharmacuticalBillByBatch(ItemBatch itemBatch, Department department, Date date) {
        String sql = "Select sh from PharmaceuticalBillItem sh where "
                + " sh.itemBatch=:itmB and sh.billItem.bill.department=:dep "
                + " and (sh.billItem.bill.billType=:btp1 or sh.billItem.bill.billType=:btp2 )"
                + "  and sh.billItem.createdAt<:dt "
                + " order by sh.billItem.createdAt desc";
        HashMap hm = new HashMap();
        hm.put("itmB", itemBatch);
        hm.put("dt", date);
        hm.put("dep", department);
        hm.put("btp1", BillType.PharmacyGrnBill);
        hm.put("btp2", BillType.PharmacyPurchaseBill);
        return getPharmaceuticalBillItemFacade().findFirstByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

//    public void resetTransferHistoryValue() {
//        String sql;
//        Map temMap = new HashMap();
//
//        sql = "select p from PharmaceuticalBillItem p where p.billItem.bill.billType = :billType or "
//                + " p.billItem.bill.billType = :billType2 and p.stockHistory is not null order by p.stockHistory.id ";
//
//        temMap.put("billType", BillType.PharmacyTransferIssue);
//        temMap.put("billType2", BillType.PharmacyTransferReceive);
//        List<PharmaceuticalBillItem> list = getPharmaceuticalBillItemFacade().findByJpql(sql, temMap);
//
//        for (PharmaceuticalBillItem b : list) {
//            StockHistory sh = getPreviousStockHistoryByBatch(b.getItemBatch(), b.getBillItem().getBill().getDepartment(), b.getBillItem().getCreatedAt());
//            PharmaceuticalBillItem phi = getPreviousPharmacuticalBillByBatch(b.getStock().getItemBatch(), b.getBillItem().getBill().getDepartment(), b.getBillItem().getCreatedAt());
//            if (sh != null) {
//                //   //System.out.println("Previuos Stock " + sh.getItemStockQty());
//                //   //System.out.println("Ph Qty " + sh.getPbItem().getQtyInUnit() + sh.getPbItem().getFreeQtyInUnit());
//                //   //System.out.println("Acc Qty " + (sh.getItemStockQty() + sh.getPbItem().getQtyInUnit() + sh.getPbItem().getFreeQtyInUnit()));
//                b.getStockHistory().setStockQty((sh.getItemStockQty() + sh.getPbItem().getQtyInUnit() + sh.getPbItem().getFreeQtyInUnit()));
//            } else if (phi != null) {
//                b.getStockHistory().setStockQty(phi.getQtyInUnit() + phi.getFreeQtyInUnit());
//            } else {
//                b.getStockHistory().setStockQty(0.0);
//            }
//            //   //System.out.println("#########");
//            getStockHistoryFacade().edit(b.getStockHistory());
//
//        }
//
//    }
    @Deprecated // Use the mathod with the same name in data upload controller
    public String importToExcelWithStock() {
        if (file == null || file.getFileName() == null) {
            JsfUtil.addErrorMessage("No File");
            return "";
        }

        String strCat;
        String strAmp;
        String strCode;
        String strBarcode;
        String strGenericName;
        String strStrength;
        String strStrengthUnit;
        String strPackSize;
        String strIssueUnit;
        String strPackUnit;
        String strDistributor;
        String strManufacturer;
        String strImporter;

        PharmaceuticalItemCategory cat;
        PharmaceuticalItemType phType;
        Vtm vtm;
        Vmp vmp;
        Amp amp;
//        Ampp ampp;
//        Vmpp vmpp;
//        VirtualProductIngredient vtmsvmps;
        MeasurementUnit issueUnit;
        MeasurementUnit strengthUnit;
        MeasurementUnit packUnit;
        double strengthUnitsPerIssueUnit;
        double issueUnitsPerPack;
        Institution distributor;
        Institution manufacturer;
        Institution importer;

        double stockQty;
        double pp;
        double sp;
        String batch;
        Date doe;

        File inputWorkbook;
        Workbook w;
        Cell cell;
        InputStream in;
        JsfUtil.addSuccessMessage(file.getFileName());
        try {
            JsfUtil.addSuccessMessage(file.getFileName());
            in = file.getInputStream();
            File f;
            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
            FileOutputStream out = new FileOutputStream(f);
            int read;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            inputWorkbook = new File(f.getAbsolutePath());
            JsfUtil.addSuccessMessage("Excel File Opened");
            w = Workbook.getWorkbook(inputWorkbook);
            Sheet sheet = w.getSheet(0);

            getPharmacyPurchaseController().makeNull();

            for (int i = startRow; i < sheet.getRows(); i++) {
                Map m;

                //Category
                cell = sheet.getCell(catCol, i);
                strCat = cell.getContents();
                if (strCat == null || strCat.trim().isEmpty()) {
                    continue;
                }
                cat = getPharmacyBean().getPharmaceuticalCategoryByName(strCat);
                if (cat == null) {
                    continue;
                }

                phType = getPharmacyBean().getPharmaceuticalItemTypeByName(strCat);

                //Strength Unit
                cell = sheet.getCell(strengthUnitCol, i);
                strStrengthUnit = cell.getContents();
                strengthUnit = getPharmacyBean().getUnitByName(strStrengthUnit);
                if (strengthUnit == null) {
                    continue;
                }
                //System.out.println("strengthUnit = " + strengthUnit.getName());
                //Pack Unit
                cell = sheet.getCell(packUnitCol, i);
                strPackUnit = cell.getContents();
                packUnit = getPharmacyBean().getUnitByName(strPackUnit);
                if (packUnit == null) {
                    continue;
                }
                //Issue Unit
                cell = sheet.getCell(issueUnitCol, i);
                strIssueUnit = cell.getContents();
                issueUnit = getPharmacyBean().getUnitByName(strIssueUnit);
                if (issueUnit == null) {
                    continue;
                }
                //StrengthOfAnMeasurementUnit
                cell = sheet.getCell(strengthOfIssueUnitCol, i);
                strStrength = cell.getContents();
                if (!strStrength.isEmpty()) {
                    try {
                        strengthUnitsPerIssueUnit = Double.parseDouble(strStrength);
                    } catch (NumberFormatException e) {
                        strengthUnitsPerIssueUnit = 0.0;
                    }
                } else {
                    strengthUnitsPerIssueUnit = 0.0;
                }

                //Issue Units Per Pack
                cell = sheet.getCell(issueUnitsPerPackCol, i);
                strPackSize = cell.getContents();
                if (!strPackSize.isEmpty()) {
                    try {
                        issueUnitsPerPack = Double.parseDouble(strPackSize);
                    } catch (NumberFormatException e) {
                        issueUnitsPerPack = 0.0;
                    }
                } else {
                    issueUnitsPerPack = 0.0;
                }

                //Vtm
                cell = sheet.getCell(vtmCol, i);
                strGenericName = cell.getContents();
                if (!strGenericName.isEmpty()) {
                    vtm = getPharmacyBean().getVtmByName(strGenericName);
                } else {
                    vtm = null;
                }

                //Vmp
                vmp = getPharmacyBean().getVmp(vtm, strengthUnitsPerIssueUnit, strengthUnit, cat);
                if (vmp == null) {
                    continue;
                } else {
                    vmp.setCategory(phType);
                    getVmpFacade().edit(vmp);
                }

                //Code
                cell = sheet.getCell(codeCol, i);
                strCode = cell.getContents();

                //Code
                cell = sheet.getCell(barcodeCol, i);
                strBarcode = cell.getContents();

                //Distributor
                cell = sheet.getCell(distributorCol, i);

                //Amp
                cell = sheet.getCell(ampCol, i);
                strAmp = cell.getContents();
                m = new HashMap();
                m.put("v", vmp);
                m.put("n", strAmp.toUpperCase());
                if (!strCat.isEmpty()) {
                    amp = ampFacade.findFirstByJpql("SELECT c FROM Amp c Where c.retired=false and (c.name)=:n "
                            + " AND c.vmp=:v", m);
                    if (amp == null) {
                        amp = new Amp();
                        amp.setName(strAmp);
                        amp.setCode(strCode);
                        amp.setBarcode(strBarcode);
                        amp.setMeasurementUnit(strengthUnit);
                        amp.setDblValue(strengthUnitsPerIssueUnit);
                        amp.setCategory(cat);
                        amp.setVmp(vmp);
                        getAmpFacade().create(amp);
                    } else {
                        amp.setRetired(false);
                        getAmpFacade().edit(amp);
                    }
                } else {
                    amp = null;
                }
                if (amp == null) {
                    continue;
                }
                //Ampp
//                ampp = getPharmacyBean().getAmpp(amp, issueUnitsPerPack, packUnit);

                //Code
                cell = sheet.getCell(codeCol, i);
                strCode = cell.getContents();
                amp.setCode(strCode);
                getAmpFacade().edit(amp);
                //Code
                cell = sheet.getCell(barcodeCol, i);
                strBarcode = cell.getContents();
                amp.setCode(strBarcode);
                getAmpFacade().edit(amp);
                //Distributor
                cell = sheet.getCell(distributorCol, i);
                strDistributor = cell.getContents();
                distributor = getInstitutionController().getInstitutionByName(strDistributor, InstitutionType.Dealer);
                if (distributor != null) {
                    ItemsDistributors id = new ItemsDistributors();
                    id.setInstitution(distributor);
                    id.setItem(amp);
                    id.setOrderNo(0);
                    getItemsDistributorsFacade().create(id);
                }
                //Manufacture

                cell = sheet.getCell(manufacturerCol, i);
                strManufacturer = cell.getContents();
                manufacturer = getInstitutionController().getInstitutionByName(strManufacturer, InstitutionType.Manufacturer);
                amp.setManufacturer(manufacturer);
                //Importer
                cell = sheet.getCell(importerCol, i);
                strImporter = cell.getContents();
                importer = getInstitutionController().getInstitutionByName(strImporter, InstitutionType.Importer);
                amp.setManufacturer(importer);
                //
                String temStr;

                cell = sheet.getCell(stockQtyCol, i);
                temStr = cell.getContents();
                try {
                    stockQty = Double.parseDouble(temStr);
                } catch (NumberFormatException e) {
                    stockQty = 0;
                }

                cell = sheet.getCell(pruchaseRateCol, i);
                temStr = cell.getContents();
                try {
                    pp = Double.parseDouble(temStr);
                } catch (NumberFormatException e) {
                    pp = 0;
                }

                cell = sheet.getCell(saleRateCol, i);
                temStr = cell.getContents();
                try {
                    sp = Double.parseDouble(temStr);
                } catch (Exception e) {
                    sp = 0;
                }

                cell = sheet.getCell(batchCol, i);
                batch = cell.getContents();

                cell = sheet.getCell(doeCol, i);
                temStr = cell.getContents();
                try {
                    doe = new SimpleDateFormat("M/d/yyyy", Locale.ENGLISH).parse(temStr);
                } catch (Exception e) {
                    doe = new Date();
                }

                getPharmacyPurchaseController().getCurrentBillItem().setItem(amp);
                getPharmacyPurchaseController().getCurrentBillItem().setTmpQty(stockQty);
                getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().setPurchaseRate(pp);
                getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().setRetailRate(sp);
                getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().setDoe(doe);
                if (batch == null || batch.trim().isEmpty()) {
                    getPharmacyPurchaseController().setBatch();
                } else {
                    getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().setStringValue(batch);
                }
                getPharmacyPurchaseController().addItem();
            }
            JsfUtil.addSuccessMessage("Succesful. All the data in Excel File Impoted to the database");
            return "/pharmacy/pharmacy_purchase";
        } catch (IOException | BiffException ex) {
            JsfUtil.addErrorMessage(ex.getMessage());
            return "";
        }
    }

    List<String> itemNamesFailedToImport;

    public List<String> getItemNamesFailedToImport() {
        return itemNamesFailedToImport;
    }

    public void setItemNamesFailedToImport(List<String> itemNamesFailedToImport) {
        this.itemNamesFailedToImport = itemNamesFailedToImport;
    }

    public String importFromExcelByName() {
        String strAmp;
        Amp amp;
        itemNamesFailedToImport = new ArrayList<>();
        double stockQty;
        double pp;
        double sp;
        String batch;
        Date doe;
        String temStr;

        File inputWorkbook;
        Workbook w;
        Cell cell;
        InputStream in;
        JsfUtil.addSuccessMessage(file.getFileName());
        try {
            JsfUtil.addSuccessMessage(file.getFileName());
            in = file.getInputStream();
            File f;
            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
            FileOutputStream out = new FileOutputStream(f);
            int read;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            inputWorkbook = new File(f.getAbsolutePath());

            JsfUtil.addSuccessMessage("Excel File Opened");
            w = Workbook.getWorkbook(inputWorkbook);
            Sheet sheet = w.getSheet(0);

            getPharmacyPurchaseController().makeNull();

            int doeCol = 1;
            int batchCol = 2;
            int stockQtyCol = 3;
            int pruchaseRateCol = 4;
            int saleRateCol = 5;

            for (int i = startRow; i < sheet.getRows(); i++) {

                Map m;

                cell = sheet.getCell(0, i);
                strAmp = cell.getContents();
                m = new HashMap();
                m.put("n", strAmp.toUpperCase());
                amp = ampFacade.findFirstByJpql("SELECT c FROM Amp c Where c.retired=false and (c.name)=:n ", m);

                if (amp == null) {
                    itemNamesFailedToImport.add(strAmp);
                    continue;
                }

                cell = sheet.getCell(stockQtyCol, i);
                temStr = cell.getContents();
                try {
                    stockQty = Double.parseDouble(temStr);
                } catch (Exception e) {
                    stockQty = 0;
                }

                cell = sheet.getCell(pruchaseRateCol, i);
                temStr = cell.getContents();
                try {
                    pp = Double.parseDouble(temStr);
                } catch (Exception e) {
                    pp = 0;
                }

                cell = sheet.getCell(saleRateCol, i);
                temStr = cell.getContents();
                try {
                    sp = Double.parseDouble(temStr);
                } catch (Exception e) {
                    sp = 0;
                }

                cell = sheet.getCell(batchCol, i);
                batch = cell.getContents();

                cell = sheet.getCell(doeCol, i);
                temStr = cell.getContents();
                try {
                    doe = CommonFunctions.convertDateToDbType(temStr);
                } catch (Exception e) {
                    doe = new Date();
                }

                getPharmacyPurchaseController().getCurrentBillItem().setItem(amp);
                //System.out.println("getPharmacyPurchaseController().getCurrentBillItem().setItem(amp) = " + getPharmacyPurchaseController().getCurrentBillItem().getItem());
                getPharmacyPurchaseController().getCurrentBillItem().setTmpQty(stockQty);
                //System.out.println("getPharmacyPurchaseController().getCurrentBillItem().setTmpQty(stockQty) = " + getPharmacyPurchaseController().getCurrentBillItem().getTmpQty());
                getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().setPurchaseRate(pp);
                //System.out.println("getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().setPurchaseRate(pp); = " + getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate());
                getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().setRetailRate(sp);
                getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().setDoe(doe);
                if (batch == null || batch.trim().isEmpty()) {
                    getPharmacyPurchaseController().setBatch();
                } else {
                    getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().setStringValue(batch);
                }
                getPharmacyPurchaseController().addItem();
            }
            JsfUtil.addSuccessMessage("Succesful. All the data in Excel File Impoted to the database");
            return "/pharmacy/pharmacy_purchase";
        } catch (IOException | BiffException ex) {
            JsfUtil.addErrorMessage(ex.getMessage());
            return "";
        }
    }

    public String importFromExcelByBarcode() {
        String strAmp;
        Amp amp;

        double stockQty;
        double pp;
        double sp;
        String batch;
        Date doe;
        String temStr;

        File inputWorkbook;
        Workbook w;
        Cell cell;
        InputStream in;
        JsfUtil.addSuccessMessage(file.getFileName());
        try {
            JsfUtil.addSuccessMessage(file.getFileName());
            in = file.getInputStream();
            File f;
            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
            FileOutputStream out = new FileOutputStream(f);
            int read;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            inputWorkbook = new File(f.getAbsolutePath());

            JsfUtil.addSuccessMessage("Excel File Opened");
            w = Workbook.getWorkbook(inputWorkbook);
            Sheet sheet = w.getSheet(0);

            getPharmacyPurchaseController().makeNull();

            int doeCol = 1;
            int batchCol = 2;
            int stockQtyCol = 3;
            int pruchaseRateCol = 4;
            int saleRateCol = 5;

            for (int i = startRow; i < sheet.getRows(); i++) {

                Map m;

                cell = sheet.getCell(0, i);
                strAmp = cell.getContents();
                m = new HashMap();
                m.put("n", strAmp.toUpperCase());
                amp = ampFacade.findFirstByJpql("SELECT c FROM Amp c Where c.retired=false and (c.code)=:n ", m);

                if (amp == null) {
                    continue;
                }

                cell = sheet.getCell(stockQtyCol, i);
                temStr = cell.getContents();
                try {
                    stockQty = Double.valueOf(temStr);
                } catch (Exception e) {
                    stockQty = 0;
                }

                cell = sheet.getCell(pruchaseRateCol, i);
                temStr = cell.getContents();
                try {
                    pp = Double.valueOf(temStr);
                } catch (Exception e) {
                    pp = 0;
                }

                cell = sheet.getCell(saleRateCol, i);
                temStr = cell.getContents();
                try {
                    sp = Double.valueOf(temStr);
                } catch (Exception e) {
                    sp = 0;
                }

                cell = sheet.getCell(batchCol, i);
                batch = cell.getContents();

                cell = sheet.getCell(doeCol, i);
                temStr = cell.getContents();
                try {
                    doe = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).parse(temStr);
                } catch (Exception e) {
                    doe = new Date();
                }

                getPharmacyPurchaseController().getCurrentBillItem().setItem(amp);
                //System.out.println("getPharmacyPurchaseController().getCurrentBillItem().setItem(amp) = " + getPharmacyPurchaseController().getCurrentBillItem().getItem());
                getPharmacyPurchaseController().getCurrentBillItem().setTmpQty(stockQty);
                //System.out.println("getPharmacyPurchaseController().getCurrentBillItem().setTmpQty(stockQty) = " + getPharmacyPurchaseController().getCurrentBillItem().getTmpQty());
                getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().setPurchaseRate(pp);
                //System.out.println("getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().setPurchaseRate(pp); = " + getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate());
                getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().setRetailRate(sp);
                getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().setDoe(doe);
                if (batch == null || batch.trim().isEmpty()) {
                    getPharmacyPurchaseController().setBatch();
                } else {
                    getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().setStringValue(batch);
                }
                getPharmacyPurchaseController().addItem();
            }
            JsfUtil.addSuccessMessage("Succesful. All the data in Excel File Impoted to the database");
            return "/pharmacy/pharmacy_purchase";
        } catch (IOException | BiffException ex) {
            JsfUtil.addErrorMessage(ex.getMessage());
            return "";
        }
    }

    public String importItemDistributors() {
        String strAmp;
        String strDistributor;
        Amp amp;
        Institution distributor;

        File inputWorkbook;
        Workbook w;
        Cell cell;
        InputStream in;
        JsfUtil.addSuccessMessage(file.getFileName());
        try {
            JsfUtil.addSuccessMessage(file.getFileName());
            in = file.getInputStream();
            File f;
            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
            FileOutputStream out = new FileOutputStream(f);
            int read;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            inputWorkbook = new File(f.getAbsolutePath());

            JsfUtil.addSuccessMessage("Excel File Opened");
            w = Workbook.getWorkbook(inputWorkbook);
            Sheet sheet = w.getSheet(0);

            StringBuilder error = new StringBuilder();

            for (int i = startRow; i < sheet.getRows(); i++) {

                Map m;

                //Amp
                cell = sheet.getCell(0, i);
                strAmp = cell.getContents();
                m = new HashMap();
                m.put("n", strAmp.trim().toUpperCase());
                amp = ampFacade.findFirstByJpql("SELECT c FROM Amp c Where (c.name)=:n ", m);

                if (amp == null) {
                    error.append(strAmp).append(" is NOT found.\n");
                    continue;
                }

//Distributor
                cell = sheet.getCell(1, i);
                strDistributor = cell.getContents();
                distributor = getInstitutionController().getInstitutionByName(strDistributor, InstitutionType.Dealer);
                if (distributor != null) {
                    ItemsDistributors id = new ItemsDistributors();
                    id.setInstitution(distributor);
                    id.setItem(amp);
                    id.setOrderNo(0);
                    getItemsDistributorsFacade().create(id);
                }
            }

            JsfUtil.addSuccessMessage("Succesful. All the data in Excel File Impoted to the database");
            return "";
        } catch (IOException | BiffException ex) {
            JsfUtil.addErrorMessage(ex.getMessage());
            return "";
        }
    }

    public void tnametoName() {
        List<Amp> amps = getAmpFacade().findAll();
        for (Amp a : amps) {
            if (a.getSname() != null && !a.getSname().trim().isEmpty()) {
                a.setTname(a.getName());
                a.setName(a.getSname());
                getAmpFacade().edit(a);
            }
        }
    }

    public String importCorrectNameByBarcode() {
        String strBarcode;
        String strName;
        Amp amp;

        File inputWorkbook;
        Workbook w;
        Cell cell;
        InputStream in;
        JsfUtil.addSuccessMessage(file.getFileName());
        try {
            JsfUtil.addSuccessMessage(file.getFileName());
            in = file.getInputStream();
            File f;
            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
            FileOutputStream out = new FileOutputStream(f);
            int read;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            inputWorkbook = new File(f.getAbsolutePath());

            JsfUtil.addSuccessMessage("Excel File Opened");
            w = Workbook.getWorkbook(inputWorkbook);
            Sheet sheet = w.getSheet(0);

            StringBuilder error = new StringBuilder();

            for (int i = startRow; i < sheet.getRows(); i++) {

                Map m;

                //Amp
                cell = sheet.getCell(0, i);
                strBarcode = cell.getContents();
                m = new HashMap();
                m.put("n", strBarcode);
                amp = ampFacade.findFirstByJpql("SELECT c FROM Amp c Where c.barcode=:n", m);
                cell = sheet.getCell(1, i);
                strName = cell.getContents();

                if (amp == null) {
                    error.append(strBarcode).append(" is NOT found.\n");
                } else {
                    amp.setTname(amp.getName());
                    amp.setName(strName);
                    getAmpFacade().edit(amp);

                }
            }

            JsfUtil.addSuccessMessage("Succesful. All the data in Excel File Impoted to the database");
            return "";
        } catch (IOException | BiffException ex) {
            JsfUtil.addErrorMessage(ex.getMessage());
            return "";
        }
    }

    private List<Amp> updatingAmps;
    private List<Amp> allAmps;

//    public String importCorrectNameFromSnapshot() {
//        //System.out.println("importing to excel");
//        String strId;
//        Long id;
//        String name;
//        String code;
//        String barcode;
//        Amp amp;
//        Institution distributor;
//
//        File inputWorkbook;
//        Workbook w;
//        Cell cell;
//        InputStream in;
//        JsfUtil.addSuccessMessage(file.getFileName());
//        try {
//            JsfUtil.addSuccessMessage(file.getFileName());
//            in = file.getInputStream();
//            File f;
//            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
//            FileOutputStream out = new FileOutputStream(f);
//            int read = 0;
//            byte[] bytes = new byte[1024];
//            while ((read = in.read(bytes)) != -1) {
//                out.write(bytes, 0, read);
//            }
//            in.close();
//            out.flush();
//            out.close();
//
//            inputWorkbook = new File(f.getAbsolutePath());
//
//            JsfUtil.addSuccessMessage("Excel File Opened");
//            w = Workbook.getWorkbook(inputWorkbook);
//            Sheet sheet = w.getSheet(0);
//
//            message = "";
//
//            allAmps = new ArrayList<>();
//            updatingAmps = new ArrayList<>();
//
//            for (int i = startRow; i < sheet.getRows(); i++) {
//
//                Map m = new HashMap();
//
//                cell = sheet.getCell(0, i);
//                strId = cell.getContents();
//                try{
//                    id = Long.parseLong(strId);
//                }catch(Exception e){
//                    id = 0l;
//                }
//
//                cell = sheet.getCell(1, i);
//                name = cell.getContents();
//
//                cell = sheet.getCell(2, i);
//                code = cell.getContents();
//
//                cell = sheet.getCell(3, i);
//                barcode = cell.getContents();
//
//                m = new HashMap();
//                m.put("n", id);
//                amp = ampFacade.findFirstByJpql("SELECT c FROM Amp c Where c.id=:n", m);
//
//
//                if (amp == null) {
//                    message += "ID NOT FOUND IN" + id + "\t" +  name + "\t" + code + "\t" + barcode + "\n";
//                    continue;
//                } else if(amp.getName().equals(name)){
//                    message += "ID AND NAME MATCH FOUND IN" + id + "\t" +  name + "\t" + code + "\t" + barcode + "\n";
//                }
//                else {
//                    amp.setSnapShotName(name);
//                    amp.setSnapShotBarcode(barcode);
//                    amp.setSnapShotCode(code);
//                    allAmps.add(amp);
//                }
//
//            }
//
//            JsfUtil.addSuccessMessage("Succesful. All the data in Excel File Impoted to the database");
//            return "";
//        } catch (IOException ex) {
//            JsfUtil.addErrorMessage(ex.getMessage());
//            return "";
//        } catch (BiffException e) {
//            JsfUtil.addErrorMessage(e.getMessage());
//            return "";
//        }
//    }
//
//
//    public void updateNamesOfSelectedAmps(){
//        for(Amp a:updatingAmps){
//            a.setName(a.getSnapShotName());
//            getAmpFacade().edit(a);
//        }
//    }
    public String importFindMissingNames() {
        String name;
        String code;
        String barcode;
        Amp amp;

        File inputWorkbook;
        Workbook w;
        Cell cell;
        InputStream in;
        JsfUtil.addSuccessMessage(file.getFileName());
        try {
            JsfUtil.addSuccessMessage(file.getFileName());
            in = file.getInputStream();
            File f;
            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
            FileOutputStream out = new FileOutputStream(f);
            int read;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            inputWorkbook = new File(f.getAbsolutePath());

            JsfUtil.addSuccessMessage("Excel File Opened");
            w = Workbook.getWorkbook(inputWorkbook);
            Sheet sheet = w.getSheet(0);

            message = new StringBuilder();

            for (int i = startRow; i < sheet.getRows(); i++) {

                Map m;

                cell = sheet.getCell(0, i);
                name = cell.getContents();

                cell = sheet.getCell(1, i);
                code = cell.getContents();

                cell = sheet.getCell(2, i);
                barcode = cell.getContents();

                m = new HashMap();
                m.put("n", name);
                amp = ampFacade.findFirstByJpql("SELECT c FROM Amp c Where c.name=:n", m);
                cell = sheet.getCell(1, i);

                if (amp == null) {
                    message.append(name).append("\t").append(code).append("\t").append(barcode).append("\n");
                } else {
                    if (amp.getBarcode().equals(amp.getCode())) {
                        amp.setTname(amp.getCode());
                        amp.setCode(code);
                        getAmpFacade().edit(amp);
                    }
                }

            }

            JsfUtil.addSuccessMessage("Succesful. All the data in Excel File Impoted to the database");
            return "";
        } catch (IOException | BiffException ex) {
            JsfUtil.addErrorMessage(ex.getMessage());
            return "";
        }
    }

    public String importVtmFromExcel() {
        String strCat;
        String strAmp;
        String strCode;
        String strBarcode;
        String strGenericName;
        String strStrength;
        String strStrengthUnit;
        String strPackSize;
        String strIssueUnit;
        String strPackUnit;
        String strDistributor;
        String strManufacturer;
        String strImporter;

        PharmaceuticalItemCategory cat;
        PharmaceuticalItemType phtype;
        Vtm vtm;
        Atm atm;
        Vmp vmp;
        Amp amp;
        Ampp ampp;
        Vmpp vmpp;
        VirtualProductIngredient vtmsvmps;
        MeasurementUnit issueUnit;
        MeasurementUnit strengthUnit;
        MeasurementUnit packUnit;
        double strengthUnitsPerIssueUnit;
        double issueUnitsPerPack;
        Institution distributor;
        Institution Manufacturer;
        Institution Importer;

        File inputWorkbook;
        Workbook w;
        Cell cell;
        InputStream in;
        JsfUtil.addSuccessMessage(file.getFileName());
        try {
            JsfUtil.addSuccessMessage(file.getFileName());
            in = file.getInputStream();
            File f;
            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
            FileOutputStream out = new FileOutputStream(f);
            int read;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            inputWorkbook = new File(f.getAbsolutePath());

            JsfUtil.addSuccessMessage("Excel File Opened");
            w = Workbook.getWorkbook(inputWorkbook);
            Sheet sheet = w.getSheet(0);

            for (int i = startRow; i < sheet.getRows(); i++) {

                Map m;

                //Category
                cell = sheet.getCell(catCol, i);
                strCat = cell.getContents();
                //System.out.println("strCat is " + strCat);
                cat = getPharmacyBean().getPharmaceuticalCategoryByName(strCat);
                if (cat == null) {
                    continue;
                }
                //System.out.println("cat = " + cat.getName());

                phtype = getPharmacyBean().getPharmaceuticalItemTypeByName(strCat);

                //Strength Unit
                cell = sheet.getCell(strengthUnitCol, i);
                strStrengthUnit = cell.getContents();
                //System.out.println("strStrengthUnit is " + strengthUnitCol);
                strengthUnit = getPharmacyBean().getUnitByName(strStrengthUnit);
                if (strengthUnit == null) {
                    continue;
                }
                //System.out.println("strengthUnit = " + strengthUnit.getName());
                //Pack Unit
                cell = sheet.getCell(packUnitCol, i);
                strPackUnit = cell.getContents();
                //System.out.println("strPackUnit = " + strPackUnit);
                packUnit = getPharmacyBean().getUnitByName(strPackUnit);
                if (packUnit == null) {
                    continue;
                }
                //System.out.println("packUnit = " + packUnit.getName());
                //Issue Unit
                cell = sheet.getCell(issueUnitCol, i);
                strIssueUnit = cell.getContents();
                //System.out.println("strIssueUnit is " + strIssueUnit);
                issueUnit = getPharmacyBean().getUnitByName(strIssueUnit);
                if (issueUnit == null) {
                    continue;
                }
                //StrengthOfAnMeasurementUnit
                cell = sheet.getCell(strengthOfIssueUnitCol, i);
                strStrength = cell.getContents();
                //System.out.println("strStrength = " + strStrength);
                if (!strStrength.isEmpty()) {
                    try {
                        strengthUnitsPerIssueUnit = Double.parseDouble(strStrength);
                    } catch (NumberFormatException e) {
                        strengthUnitsPerIssueUnit = 0.0;
                    }
                } else {
                    strengthUnitsPerIssueUnit = 0.0;
                }

                //Issue Units Per Pack
                cell = sheet.getCell(issueUnitsPerPackCol, i);
                strPackSize = cell.getContents();
                //System.out.println("strPackSize = " + strPackSize);
                if (!strPackSize.isEmpty()) {
                    try {
                        issueUnitsPerPack = Double.parseDouble(strPackSize);
                    } catch (NumberFormatException e) {
                        issueUnitsPerPack = 0.0;
                    }
                } else {
                    issueUnitsPerPack = 0.0;
                }

                //Vtm
                cell = sheet.getCell(vtmCol, i);
                strGenericName = cell.getContents();
                //System.out.println("strGenericName = " + strGenericName);
                if (!strGenericName.isEmpty()) {
                    vtm = getPharmacyBean().getVtmByName(strGenericName);
                } else {
                    vtm = null;
                }

                //Vmp
                vmp = getPharmacyBean().getVmp(vtm, strengthUnitsPerIssueUnit, strengthUnit, cat);
                if (vmp == null) {
                    continue;
                } else {
                    vmp.setCategory(phtype);
                    getVmpFacade().edit(vmp);
                }
                //System.out.println("vmp = " + vmp.getName());
                //Amp
                cell = sheet.getCell(ampCol, i);
                strAmp = cell.getContents();
                //System.out.println("strAmp = " + strAmp);

                cell = sheet.getCell(codeCol, i);
                strCode = cell.getContents();
                //System.out.println("strCode = " + strCode);

                //System.out.println("strAmp = " + strAmp);
                m = new HashMap();
                m.put("v", vmp);
                m.put("n", strAmp.trim().toUpperCase());
                if (!strCat.isEmpty()) {
                    amp = ampFacade.findFirstByJpql("SELECT c FROM Amp c Where (c.name)=:n AND c.vmp=:v", m);
                    if (amp == null) {
                        amp = new Amp();
                        amp.setName(strAmp);
                        amp.setCode(strCode);
                        amp.setDepartmentType(DepartmentType.Pharmacy);
                        amp.setMeasurementUnit(strengthUnit);
                        amp.setDblValue(strengthUnitsPerIssueUnit);
                        amp.setCategory(cat);
                        amp.setVmp(vmp);
                        getAmpFacade().create(amp);
                    } else {
                        amp.setRetired(false);
                        amp.setDepartmentType(DepartmentType.Pharmacy);
                        amp.setCode(strCode);
                        getAmpFacade().edit(amp);
                    }
                } else {
                    amp = null;
                }
                if (amp == null) {
                    continue;
                }
                //System.out.println("amp = " + amp.getName());
                //Ampp
                if (issueUnitsPerPack > 1) {
                    ampp = getPharmacyBean().getAmpp(amp, issueUnitsPerPack, packUnit);
                }
                //Code
                cell = sheet.getCell(codeCol, i);
                strCode = cell.getContents();
                amp.setCode(strCode);
                getAmpFacade().edit(amp);
                //Code
                cell = sheet.getCell(barcodeCol, i);
                strBarcode = cell.getContents();
                amp.setBarcode(strBarcode);
                getAmpFacade().edit(amp);
                //Distributor
                cell = sheet.getCell(distributorCol, i);
                strDistributor = cell.getContents();
                distributor = getInstitutionController().getInstitutionByName(strDistributor, InstitutionType.Dealer);
                if (distributor != null) {
                    ItemsDistributors id = new ItemsDistributors();
                    id.setInstitution(distributor);
                    id.setItem(amp);
                    id.setOrderNo(0);
                    getItemsDistributorsFacade().create(id);
                }
            }

            JsfUtil.addSuccessMessage("Succesful. All the data in Excel File Impoted to the database");
            return "";
        } catch (IOException | BiffException ex) {
            JsfUtil.addErrorMessage(ex.getMessage());
            return "";
        }
    }

    public String importToExcel() {
        if (file == null) {
            JsfUtil.addErrorMessage("No File");
            return "";
        }
        if (file.getFileName() == null) {
            JsfUtil.addErrorMessage("No File");
            return "";
        }

        // Set column mappings to match the new UI layout (A-N columns)
        catCol = 1;                   // B - Category
        ampCol = 2;                   // C - Product
        codeCol = 3;                  // D - Code
        barcodeCol = 4;               // E - Bar Code
        vtmCol = 5;                   // F - Generic Name
        strengthOfIssueUnitCol = 6;   // G - Strength
        strengthUnitCol = 7;          // H - Strength Unit
        issueUnitsPerPackCol = 8;     // I - Pack Size
        issueUnitCol = 9;             // J - Issue Unit
        packUnitCol = 10;             // K - Pack Unit
        distributorCol = 11;          // L - Distributor
        manufacturerCol = 12;         // M - Manufacturer
        importerCol = 13;             // N - Importer
        startRow = 1; // If header is on row 0

        String strCat;
        String strAmp;
        String strCode;
        String strBarcode;
        String strGenericName;
        String strStrength;
        String strStrengthUnit;
        String strPackSize;
        String strIssueUnit;
        String strPackUnit;
        String strDistributor;
        String strManufacturer;
        String strImporter;

        PharmaceuticalItemCategory cat;
        PharmaceuticalItemType phType;
        Vtm vtm;
        Atm atm;
        Vmp vmp;
        Amp amp;
        Ampp ampp;
        Vmpp vmpp;
        VirtualProductIngredient vtmsvmps;
        MeasurementUnit issueUnit;
        MeasurementUnit strengthUnit;
        MeasurementUnit packUnit;
        double strengthUnitsPerIssueUnit;
        double issueUnitsPerPack;
        Institution distributor;
        Institution manufacturer;
        Institution importer;

        StringBuilder warningMessages = new StringBuilder();
        int rowCount = 0;

        try (InputStream in = file.getInputStream(); org.apache.poi.ss.usermodel.Workbook workbook = new XSSFWorkbook(in)) {
            rowCount++;
            System.out.println("rowCount at Start of a row= " + rowCount);
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            JsfUtil.addSuccessMessage(file.getFileName());

            int rowIndex = 0;
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (rowIndex++ < startRow) {
                    continue; // Skip header or initial rows as per the startRow value
                }

                Map<String, Object> m = new HashMap<>();

                // Category
                org.apache.poi.ss.usermodel.Cell catCell = row.getCell(catCol);
                strCat = getStringCellValue(catCell);
                if (strCat == null || strCat.trim().isEmpty()) {
                    continue;
                }
                cat = getPharmacyBean().getPharmaceuticalCategoryByName(strCat);
                if (cat == null) {
                    continue;
                }
                phType = getPharmacyBean().getPharmaceuticalItemTypeByName(strCat);

                // Strength Unit
                org.apache.poi.ss.usermodel.Cell strengthUnitCell = row.getCell(strengthUnitCol);
                strStrengthUnit = getStringCellValue(strengthUnitCell);
                if (strStrengthUnit == null || strStrengthUnit.trim().isEmpty()) {
                    continue;
                }
                strengthUnit = getPharmacyBean().getUnitByName(strStrengthUnit);
                if (strengthUnit == null) {
                    continue;
                }

                // Pack Unit
                org.apache.poi.ss.usermodel.Cell packUnitCell = row.getCell(packUnitCol);
                strPackUnit = getStringCellValue(packUnitCell);
                if (strPackUnit == null || strPackUnit.trim().isEmpty()) {
                    continue;
                }
                packUnit = getPharmacyBean().getUnitByName(strPackUnit);
                if (packUnit == null) {
                    continue;
                }

                // Issue Unit
                org.apache.poi.ss.usermodel.Cell issueUnitCell = row.getCell(issueUnitCol);
                strIssueUnit = getStringCellValue(issueUnitCell);
                if (strIssueUnit == null || strIssueUnit.trim().isEmpty()) {
                    continue;
                }
                issueUnit = getPharmacyBean().getUnitByName(strIssueUnit);
                if (issueUnit == null) {
                    continue;
                }

                // Strength
                org.apache.poi.ss.usermodel.Cell strengthCell = row.getCell(strengthOfIssueUnitCol);
                strStrength = getStringCellValue(strengthCell);
                strengthUnitsPerIssueUnit = parseDouble(strStrength);

                // Issue Units Per Pack
                org.apache.poi.ss.usermodel.Cell packSizeCell = row.getCell(issueUnitsPerPackCol);
                strPackSize = getStringCellValue(packSizeCell);
                issueUnitsPerPack = parseDouble(strPackSize);

                // VTM (Generic Name)
                org.apache.poi.ss.usermodel.Cell vtmCell = row.getCell(vtmCol);
                strGenericName = getStringCellValue(vtmCell);
                if (!strGenericName.isEmpty()) {
                    vtm = getPharmacyBean().getVtmByName(strGenericName);
                } else {
                    vtm = null;
                }

                // VMP
                vmp = getPharmacyBean().getVmp(vtm, strengthUnitsPerIssueUnit, strengthUnit, cat);
                if (vmp == null) {
                    continue;
                } else {
                    vmp.setCategory(phType);
                    getVmpFacade().edit(vmp);
                }

                // AMP
                org.apache.poi.ss.usermodel.Cell ampCell = row.getCell(ampCol);
                strAmp = getStringCellValue(ampCell);

                org.apache.poi.ss.usermodel.Cell codeCell = row.getCell(codeCol);
                strCode = getStringCellValue(codeCell);

                m = new HashMap<>();
                m.put("v", vmp);
                m.put("n", strAmp.trim().toUpperCase());

                if (!strCat.isEmpty()) {
                    amp = ampFacade.findFirstByJpql("SELECT c FROM Amp c Where (c.name)=:n AND c.vmp=:v", m);
                    if (amp == null) {
                        amp = new Amp();
                        amp.setName(strAmp);
                        amp.setCode(strCode);
                        amp.setDepartmentType(DepartmentType.Pharmacy);
                        amp.setMeasurementUnit(strengthUnit);
                        amp.setIssueUnit(issueUnit);
                        amp.setStrengthUnit(strengthUnit);
                        amp.setDblValue(strengthUnitsPerIssueUnit);
                        amp.setCategory(cat);
                        amp.setVmp(vmp);
                        getAmpFacade().create(amp);
                    } else {
                        amp.setRetired(false);
                        amp.setCode(strCode);
                        amp.setDepartmentType(DepartmentType.Pharmacy);
                        amp.setMeasurementUnit(strengthUnit);
                        amp.setIssueUnit(issueUnit);
                        amp.setStrengthUnit(strengthUnit);
                        amp.setDblValue(strengthUnitsPerIssueUnit);
                        amp.setCategory(cat);
                        amp.setVmp(vmp);
                        getAmpFacade().edit(amp);
                    }
                } else {
                    amp = null;
                }

                if (amp == null) {
                    continue;
                }

                // AMPP
                if (issueUnitsPerPack > 1) {
                    ampp = getPharmacyBean().getAmpp(amp, issueUnitsPerPack, packUnit);
                }

                // Barcode
                org.apache.poi.ss.usermodel.Cell barcodeCell = row.getCell(barcodeCol);
                strBarcode = getStringCellValue(barcodeCell);
                amp.setBarcode(strBarcode);

                // Distributor
                org.apache.poi.ss.usermodel.Cell distributorCell = row.getCell(distributorCol);
                strDistributor = getStringCellValue(distributorCell);
                if (strDistributor != null && !strDistributor.trim().isEmpty()) {
                    distributor = getInstitutionController().getInstitutionByName(strDistributor, InstitutionType.Dealer);
                    if (distributor != null) {
                        ItemsDistributors id = new ItemsDistributors();
                        id.setInstitution(distributor);
                        id.setItem(amp);
                        id.setOrderNo(0);
                        getItemsDistributorsFacade().create(id);
                    }
                }

                // Manufacturer - FIX: This was previously incorrectly assigned to importer
                org.apache.poi.ss.usermodel.Cell manufacturerCell = row.getCell(manufacturerCol);
                strManufacturer = getStringCellValue(manufacturerCell);
                if (strManufacturer != null && !strManufacturer.trim().isEmpty()) {
                    manufacturer = getInstitutionController().getInstitutionByName(strManufacturer, InstitutionType.Company);
                    if (manufacturer != null) {
                        amp.setManufacturer(manufacturer);
                    }
                }

                // Importer
                org.apache.poi.ss.usermodel.Cell importerCell = row.getCell(importerCol);
                strImporter = getStringCellValue(importerCell);
                if (strImporter != null && !strImporter.trim().isEmpty()) {
                    importer = getInstitutionController().getInstitutionByName(strImporter, InstitutionType.Company);
                    if (importer != null) {
                        amp.setImporter(importer);
                    }
                }

                // Save the amp with all updates
                getAmpFacade().edit(amp);
            }

            JsfUtil.addSuccessMessage("Successfully imported pharmaceutical items from Excel file to the database");
            return "";
        } catch (IOException ex) {
            JsfUtil.addErrorMessage("Error processing Excel file: " + ex.getMessage());
            return "";
        }
    }

    // Helper method to safely get string value from Excel cell
    private String getStringCellValue(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    // Helper method to parse double values safely
    private double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public String importStoreItemsToExcel() {
        String catName;
        String catCode;
        String itenName;
        String itemCode;

        StoreItemCategory cat;
        Amp amp;

        File inputWorkbook;
        Workbook w;
        Cell cell;
        InputStream in;
        JsfUtil.addSuccessMessage(file.getFileName());
        try {
            JsfUtil.addSuccessMessage(file.getFileName());
            in = file.getInputStream();
            File f;
            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
            FileOutputStream out = new FileOutputStream(f);
            int read;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            inputWorkbook = new File(f.getAbsolutePath());

            JsfUtil.addSuccessMessage("Excel File Opened");
            w = Workbook.getWorkbook(inputWorkbook);
            Sheet sheet = w.getSheet(0);

            for (int i = startRow; i < sheet.getRows(); i++) {

                Map m;

                cell = sheet.getCell(0, i);
                catCode = cell.getContents();

                cell = sheet.getCell(1, i);
                catName = cell.getContents();

                cell = sheet.getCell(2, i);
                itenName = cell.getContents();

                cell = sheet.getCell(3, i);
                itemCode = cell.getContents();

                if (catName == null || catName.trim().isEmpty() || itenName == null || itenName.trim().isEmpty()) {
                    continue;
                }

                cat = getPharmacyBean().getStoreItemCategoryByName(catName);
                if (cat == null) {
                    cat = new StoreItemCategory();
                    cat.setName(catName);
                    cat.setCode(catCode);
                    getStoreItemCategoryFacade().create(cat);
                } else {
                    cat.setName(catName);
                    cat.setCode(catCode);
                    getStoreItemCategoryFacade().edit(cat);
                }

                m = new HashMap();
                m.put("dep", DepartmentType.Store);
                m.put("n", itenName.toUpperCase());

                amp = ampFacade.findFirstByJpql("SELECT c FROM Amp c Where (c.name)=:n AND c.departmentType=:dep ", m);

                if (amp == null) {
                    amp = new Amp();
                    amp.setName(itenName);
                    amp.setCode(itemCode);
                    amp.setCategory(cat);
                    amp.setDepartmentType(DepartmentType.Store);
                    getAmpFacade().create(amp);
                } else {
                    amp.setRetired(false);
                    amp.setName(itenName);
                    amp.setCode(itemCode);
                    amp.setCategory(cat);
                    amp.setDepartmentType(DepartmentType.Store);
                    getAmpFacade().edit(amp);
                }
            }

            JsfUtil.addSuccessMessage("Succesful. All the data in Excel File Impoted to the database");
            return "";
        } catch (IOException | BiffException ex) {
            JsfUtil.addErrorMessage(ex.getMessage());
            return "";
        }
    }

    public String detectMismatch() {
        //   //System.out.println("dictecting mismatch");
        String itemName;
        String itemCode;
        String genericName;

        File inputWorkbook;
        Workbook w;
        Cell cell;
        InputStream in;
        JsfUtil.addSuccessMessage(file.getFileName());
        try {
            JsfUtil.addSuccessMessage(file.getFileName());
            in = file.getInputStream();
            File f;
            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
            FileOutputStream out = new FileOutputStream(f);
            int read;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            inputWorkbook = new File(f.getAbsolutePath());

            JsfUtil.addSuccessMessage("Excel File Opened");
            w = Workbook.getWorkbook(inputWorkbook);
            Sheet sheet = w.getSheet(0);

            itemNotPresent = new ArrayList<>();
            itemsWithDifferentCode = new ArrayList<>();
            itemsWithDifferentGenericName = new ArrayList<>();

            for (int i = startRow; i < sheet.getRows(); i++) {

                Map m = new HashMap();

                cell = sheet.getCell(1, i);
                itemName = cell.getContents();

                cell = sheet.getCell(2, i);
                itemCode = cell.getContents();

                cell = sheet.getCell(4, i);
                genericName = cell.getContents();

                String sql;
                m.put("strAmp", itemName.toUpperCase());
                //   //System.out.println("m = " + m);
                sql = "Select amp from Amp amp where amp.retired=false and (amp.name)=:strAmp";
                //   //System.out.println("sql = " + sql);
                Amp amp = getAmpFacade().findFirstByJpql(sql, m);
                //   //System.out.println("amp = " + amp);
                if (amp != null) {
                    if (amp.getCode() != null) {
                        if (!amp.getCode().equalsIgnoreCase(itemCode)) {
                            itemsWithDifferentCode.add(itemName);
                        }
                    }
                    if (amp.getVmp() != null && amp.getVmp().getName() != null) {
                        if (amp.getVmp().getName().equalsIgnoreCase(genericName)) {
                            itemsWithDifferentGenericName.add(itemName);
                        }
                    }
                } else {
                    //   //System.out.println("added to list");
                    PharmacyImportCol npi = new PharmacyImportCol();
                    long l;
                    double d;

                    npi.setItem1_itemCatName(sheet.getCell(0, i).getContents());
                    npi.setItem2_ampName(sheet.getCell(1, i).getContents());
                    npi.setItem3_code(sheet.getCell(2, i).getContents());
                    npi.setItem4_barcode(sheet.getCell(3, i).getContents());
                    npi.setItem5_genericName(sheet.getCell(4, i).getContents());
                    try {
                        d = Double.parseDouble(sheet.getCell(5, i).getContents());
                    } catch (NumberFormatException e) {
                        d = 0.0;
                        //   //System.out.println("e = " + e);
                    }
                    npi.setItem6_StrengthOfIssueUnit(d);

                    npi.setItem7_StrengthUnit(sheet.getCell(6, i).getContents());

                    try {
                        d = Double.parseDouble(sheet.getCell(7, i).getContents());
                    } catch (NumberFormatException e) {
                        d = 0.0;
                        //   //System.out.println("e = " + e);
                    }
                    npi.setItem8_IssueUnitsPerPack(d);

                    npi.setItem9_IssueUnit(sheet.getCell(8, i).getContents());
                    npi.setItem10_PackUnit(sheet.getCell(9, i).getContents());

                    itemNotPresent.add(npi);

                }

            }
            JsfUtil.addSuccessMessage("Succesful. All Mismatches listed below.");
            return "";
        } catch (IOException | BiffException ex) {
            //   //System.out.println("ex = " + ex);
            JsfUtil.addErrorMessage(ex.getMessage());
            return "";
        }
    }

    public String importToExcelBarcode() {

        String strAmp;
        String strBarcode;

        PharmaceuticalItemCategory cat;

        Amp amp;
        File inputWorkbook;
        Workbook w;
        Cell cell;
        InputStream in;
        JsfUtil.addSuccessMessage(file.getFileName());
        try {
            JsfUtil.addSuccessMessage(file.getFileName());
            in = file.getInputStream();
            File f;
            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
            FileOutputStream out = new FileOutputStream(f);
            int read;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            inputWorkbook = new File(f.getAbsolutePath());

            JsfUtil.addSuccessMessage("Excel File Opened");
            w = Workbook.getWorkbook(inputWorkbook);
            Sheet sheet = w.getSheet(0);

            for (int i = startRow; i < sheet.getRows(); i++) {

                Map m;

                //Amp
                cell = sheet.getCell(ampCol, i);
                strAmp = cell.getContents();
                m = new HashMap();
                m.put("n", strAmp);

                amp = ampFacade.findFirstByJpql("SELECT c FROM Amp c Where (c.name)=:n", m);
                if (amp == null) {

                } else {
                    amp.setRetired(false);
                    getAmpFacade().edit(amp);
                }

                if (amp == null) {
                    continue;
                }

                //Code
                cell = sheet.getCell(codeCol, i);
                strBarcode = cell.getContents();
                amp.setCode(strBarcode);

                //Barcode
                cell = sheet.getCell(barcodeCol, i);
                strBarcode = cell.getContents();
                amp.setBarcode(strBarcode);

                getAmpFacade().edit(amp);

            }

            JsfUtil.addSuccessMessage("Succesful. All the data in Excel File Impoted to the database");
            return "";
        } catch (IOException ex) {
            JsfUtil.addErrorMessage(ex.getMessage());
            return "";
        } catch (BiffException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            return "";
        }
    }

    public String importToExcelCategoriOnly() {
        String strCat;
        String strAmp = null;

        PharmaceuticalItemCategory cat;
        Amp amp;

        File inputWorkbook;
        Workbook w;
        Cell cell;
        InputStream in;
        JsfUtil.addSuccessMessage(file.getFileName());
        try {
            JsfUtil.addSuccessMessage(file.getFileName());
            in = file.getInputStream();
            File f;
            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
            FileOutputStream out = new FileOutputStream(f);
            int read;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            inputWorkbook = new File(f.getAbsolutePath());

            JsfUtil.addSuccessMessage("Excel File Opened");
            w = Workbook.getWorkbook(inputWorkbook);
            Sheet sheet = w.getSheet(0);

            for (int i = startRow; i < sheet.getRows(); i++) {

                Map m = new HashMap();

                //Category
                cell = sheet.getCell(catCol, i);
                strCat = cell.getContents();
                cat = getPharmacyBean().getPharmaceuticalCategoryByName(strCat);
                if (cat == null) {
                    continue;
                }
                if (!strCat.isEmpty()) {
                    amp = ampFacade.findFirstByJpql("SELECT c FROM Amp c Where (c.name)=:n AND c.vmp=:v", m);
                    if (amp == null) {
                        amp = new Amp();
                        amp.setName(strAmp);
                        amp.setCategory(cat);
                        getAmpFacade().create(amp);
                    } else {
                        amp.setRetired(false);
                        amp.setCategory(cat);
                        getAmpFacade().edit(amp);
                    }
                }
            }

            JsfUtil.addSuccessMessage("Succesful. All the data in Excel File Impoted to the database");
            return "";
        } catch (IOException | BiffException ex) {
            JsfUtil.addErrorMessage(ex.getMessage());
            return "";
        }
    }

    @EJB
    ItemsDistributorsFacade itemsDistributorsFacade;

    @EJB
    PharmaceuticalItemFacade pharmaceuticalItemFacade;

    public PharmaceuticalItemFacade getPharmaceuticalItemFacade() {
        return pharmaceuticalItemFacade;
    }

    public void setPharmaceuticalItemFacade(PharmaceuticalItemFacade pharmaceuticalItemFacade) {
        this.pharmaceuticalItemFacade = pharmaceuticalItemFacade;
    }

    public void removeAllPharmaceuticalItems() {
        String sql;
        sql = "select p from PharmaceuticalItem p";
        List<PharmaceuticalItem> pis = getPharmaceuticalItemFacade().findByJpql(sql);
        for (PharmaceuticalItem p : pis) {
            getPharmaceuticalItemFacade().remove(p);
        }
    }

    public ItemsDistributorsFacade getItemsDistributorsFacade() {
        return itemsDistributorsFacade;
    }

    public void setItemsDistributorsFacade(ItemsDistributorsFacade itemsDistributorsFacade) {
        this.itemsDistributorsFacade = itemsDistributorsFacade;
    }

    public int getStartRow() {
        return startRow;
    }

    public void setStartRow(int startRow) {
        this.startRow = startRow;
    }

    public int getAmpCol() {
        return ampCol;
    }

    public void setAmpCol(int ampCol) {
        this.ampCol = ampCol;
    }

    public AmpFacade getAmpFacade() {
        return ampFacade;
    }

    public void setAmpFacade(AmpFacade ampFacade) {
        this.ampFacade = ampFacade;
    }

    public AmppFacade getAmppFacade() {
        return amppFacade;
    }

    public void setAmppFacade(AmppFacade amppFacade) {
        this.amppFacade = amppFacade;
    }

    public AtmFacade getAtmFacade() {
        return atmFacade;
    }

    public void setAtmFacade(AtmFacade atmFacade) {
        this.atmFacade = atmFacade;
    }

    public int getMeasurementUnitCol() {
        return issueUnitCol;
    }

    public void setMeasurementUnitCol(int issueUnitCol) {
        this.issueUnitCol = issueUnitCol;
    }

    public int getMeasurementUnitsPerPackCol() {
        return issueUnitsPerPackCol;
    }

    public void setMeasurementUnitsPerPackCol(int issueUnitsPerPackCol) {
        this.issueUnitsPerPackCol = issueUnitsPerPackCol;
    }

    public int getCatCol() {
        return catCol;
    }

    public void setCatCol(int catCol) {
        this.catCol = catCol;
    }

    public VirtualProductIngredientFacade getVtmInAmpFacade() {
        return vtmInAmpFacade;
    }

    public void setVtmInAmpFacade(VirtualProductIngredientFacade vtmInAmpFacade) {
        this.vtmInAmpFacade = vtmInAmpFacade;
    }

    public MeasurementUnitFacade getMuFacade() {
        return muFacade;
    }

    public void setMuFacade(MeasurementUnitFacade muFacade) {
        this.muFacade = muFacade;
    }

    public int getIssueUnitCol() {
        return issueUnitCol;
    }

    public void setIssueUnitCol(int issueUnitCol) {
        this.issueUnitCol = issueUnitCol;
    }

    public int getStrengthUnitCol() {
        return strengthUnitCol;
    }

    public void setStrengthUnitCol(int strengthUnitCol) {
        this.strengthUnitCol = strengthUnitCol;
    }

    public int getIssueUnitsPerPackCol() {
        return issueUnitsPerPackCol;
    }

    public void setIssueUnitsPerPackCol(int issueUnitsPerPackCol) {
        this.issueUnitsPerPackCol = issueUnitsPerPackCol;
    }

    public int getPackUnitCol() {
        return packUnitCol;
    }

    public void setPackUnitCol(int packUnitCol) {
        this.packUnitCol = packUnitCol;
    }

    public PharmaceuticalItemCategoryFacade getPharmaceuticalItemCategoryFacade() {
        return pharmaceuticalItemCategoryFacade;
    }

    public void setPharmaceuticalItemCategoryFacade(PharmaceuticalItemCategoryFacade pharmaceuticalItemCategoryFacade) {
        this.pharmaceuticalItemCategoryFacade = pharmaceuticalItemCategoryFacade;
    }

    public int getStrengthOfIssueUnitCol() {
        return strengthOfIssueUnitCol;
    }

    public void setStrengthOfIssueUnitCol(int strengthOfIssueUnitCol) {
        this.strengthOfIssueUnitCol = strengthOfIssueUnitCol;
    }

    public int getMeasurmentUnitCol() {
        return strengthUnitCol;
    }

    public void setMeasurmentUnitCol(int strengthUnitCol) {
        this.strengthUnitCol = strengthUnitCol;
    }

    public VmpFacade getVmpFacade() {
        return vmpFacade;
    }

    public void setVmpFacade(VmpFacade vmpFacade) {
        this.vmpFacade = vmpFacade;
    }

    public VmppFacade getVmppFacade() {
        return vmppFacade;
    }

    public void setVmppFacade(VmppFacade vmppFacade) {
        this.vmppFacade = vmppFacade;
    }

    public int getVtmCol() {
        return vtmCol;
    }

    public void setVtmCol(int vtmCol) {
        this.vtmCol = vtmCol;
    }

    public VtmFacade getVtmFacade() {
        return vtmFacade;
    }

    public void setVtmFacade(VtmFacade vtmFacade) {
        this.vtmFacade = vtmFacade;
    }

    public VirtualProductIngredientFacade getVtmsVmpsFacade() {
        return vtmInAmpFacade;
    }

    public void setVtmsVmpsFacade(VirtualProductIngredientFacade vtmInAmpFacade) {
        this.vtmInAmpFacade = vtmInAmpFacade;
    }

    public List<Ampp> getAmpps() {
        if (ampps == null) {
            ampps = getAmppFacade().findAll();
        }
        return ampps;
    }

    public void setAmpps(List<Ampp> ampps) {
        this.ampps = ampps;
    }

    public List<Amp> getAmps() {
        if (amps == null) {
            amps = getAmpFacade().findAll();
        }
        return amps;
    }

    public void setAmps(List<Amp> amps) {
        this.amps = amps;
    }

    public List<Vtm> getVtms() {
        if (vtms == null) {
            vtms = getVtmFacade().findAll();
        }
        return vtms;
    }

    public void setVtms(List<Vtm> vtms) {
        this.vtms = vtms;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public int getCodeCol() {
        return codeCol;
    }

    public void setCodeCol(int codeCol) {
        this.codeCol = codeCol;
    }

    public int getManufacturerCol() {
        return manufacturerCol;
    }

    public void setManufacturerCol(int manufacturerCol) {
        this.manufacturerCol = manufacturerCol;
    }

    public int getImporterCol() {
        return importerCol;
    }

    public void setImporterCol(int importerCol) {
        this.importerCol = importerCol;
    }

    public InstitutionController getInstitutionController() {
        return institutionController;
    }

    public void setInstitutionController(InstitutionController institutionController) {
        this.institutionController = institutionController;
    }

    public int getBarcodeCol() {
        return barcodeCol;
    }

    public void setBarcodeCol(int barcodeCol) {
        this.barcodeCol = barcodeCol;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public StockHistoryFacade getStockHistoryFacade() {
        return stockHistoryFacade;
    }

    public void setStockHistoryFacade(StockHistoryFacade stockHistoryFacade) {
        this.stockHistoryFacade = stockHistoryFacade;
    }

    public List<String> getItemsWithDifferentGenericName() {
        return itemsWithDifferentGenericName;
    }

    public void setItemsWithDifferentGenericName(List<String> itemsWithDifferentGenericName) {
        this.itemsWithDifferentGenericName = itemsWithDifferentGenericName;
    }

    public List<String> getItemsWithDifferentCode() {
        return itemsWithDifferentCode;
    }

    public void setItemsWithDifferentCode(List<String> itemsWithDifferentCode) {
        this.itemsWithDifferentCode = itemsWithDifferentCode;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public List<PharmacyImportCol> getItemNotPresent() {
        return itemNotPresent;
    }

    public void setItemNotPresent(List<PharmacyImportCol> itemNotPresent) {
        this.itemNotPresent = itemNotPresent;
    }

    public StoreItemCategoryFacade getStoreItemCategoryFacade() {
        return storeItemCategoryFacade;
    }

    public void setStoreItemCategoryFacade(StoreItemCategoryFacade storeItemCategoryFacade) {
        this.storeItemCategoryFacade = storeItemCategoryFacade;
    }

    public int getDoeCol() {
        return doeCol;
    }

    public void setDoeCol(int doeCol) {
        this.doeCol = doeCol;
    }

    public int getBatchCol() {
        return batchCol;
    }

    public void setBatchCol(int batchCol) {
        this.batchCol = batchCol;
    }

    public int getStockQtyCol() {
        return stockQtyCol;
    }

    public void setStockQtyCol(int stockQtyCol) {
        this.stockQtyCol = stockQtyCol;
    }

    public int getPruchaseRateCol() {
        return pruchaseRateCol;
    }

    public void setPruchaseRateCol(int pruchaseRateCol) {
        this.pruchaseRateCol = pruchaseRateCol;
    }

    public int getSaleRateCol() {
        return saleRateCol;
    }

    public void setSaleRateCol(int saleRateCol) {
        this.saleRateCol = saleRateCol;
    }

    public PharmacyPurchaseController getPharmacyPurchaseController() {
        return pharmacyPurchaseController;
    }

    public void setPharmacyPurchaseController(PharmacyPurchaseController pharmacyPurchaseController) {
        this.pharmacyPurchaseController = pharmacyPurchaseController;
    }

    public String getMessage() {
        return message.toString();
    }

    public void setMessage(String message) {
        this.message = new StringBuilder(message);
    }

    public List<Amp> getUpdatingAmps() {
        return updatingAmps;
    }

    public void setUpdatingAmps(List<Amp> updatingAmps) {
        this.updatingAmps = updatingAmps;
    }

    public List<Amp> getAllAmps() {
        return allAmps;
    }

    public void setAllAmps(List<Amp> allAmps) {
        this.allAmps = allAmps;
    }

}
