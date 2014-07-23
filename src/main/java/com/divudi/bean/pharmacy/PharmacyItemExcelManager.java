/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.InstitutionController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.bean.inward.InwardBeanController;
import com.divudi.data.BillType;
import com.divudi.data.DepartmentType;
import com.divudi.data.InstitutionType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.PharmacyImportCol;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.ejb.PharmacyBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.Service;
import com.divudi.entity.inward.InwardService;
import com.divudi.entity.inward.TimedItem;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.Ampp;
import com.divudi.entity.pharmacy.Atm;
import com.divudi.entity.pharmacy.ItemBatch;
import com.divudi.entity.pharmacy.ItemsDistributors;
import com.divudi.entity.pharmacy.MeasurementUnit;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.entity.pharmacy.PharmaceuticalItem;
import com.divudi.entity.pharmacy.PharmaceuticalItemCategory;
import com.divudi.entity.pharmacy.StockHistory;
import com.divudi.entity.pharmacy.StoreItemCategory;
import com.divudi.entity.pharmacy.Vmp;
import com.divudi.entity.pharmacy.Vmpp;
import com.divudi.entity.pharmacy.Vtm;
import com.divudi.entity.pharmacy.VtmsVmps;
import com.divudi.facade.AmpFacade;
import com.divudi.facade.AmppFacade;
import com.divudi.facade.AtmFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemsDistributorsFacade;
import com.divudi.facade.MeasurementUnitFacade;
import com.divudi.facade.PatientEncounterFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.PharmaceuticalItemCategoryFacade;
import com.divudi.facade.PharmaceuticalItemFacade;
import com.divudi.facade.StockFacade;
import com.divudi.facade.StockHistoryFacade;
import com.divudi.facade.StoreItemCategoryFacade;
import com.divudi.facade.VmpFacade;
import com.divudi.facade.VmppFacade;
import com.divudi.facade.VtmFacade;
import com.divudi.facade.VtmsVmpsFacade;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author Buddhika
 */
@Named(value = "pharmacyItemExcelManager")
@SessionScoped
public class PharmacyItemExcelManager implements Serializable {

    /**
     *
     * EJBs
     *
     */
    @EJB
    AtmFacade atmFacade;
    @EJB
    VtmFacade vtmFacade;
    @EJB
    AmpFacade ampFacade;
    @EJB
    VmpFacade vmpFacade;
    @EJB
    AmppFacade amppFacade;
    @EJB
    VmppFacade vmppFacade;
    @EJB
    VtmsVmpsFacade vtmInAmpFacade;
    @EJB
    MeasurementUnitFacade muFacade;
    @EJB
    PharmaceuticalItemCategoryFacade pharmaceuticalItemCategoryFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    StoreItemCategoryFacade storeItemCategoryFacade;
    @EJB
    PatientEncounterFacade patientEncounterFacade;

    List<PharmacyImportCol> itemNotPresent;
    List<String> itemsWithDifferentGenericName;
    List<String> itemsWithDifferentCode;

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
    int number = 0;
    int catCol = 1;
    int ampCol = 2;
    int codeCol = 3;
    int barcodeCol = 4;
    int vtmCol = 5;
    int strengthOfIssueUnitCol = 6;
    int strengthUnitCol = 7;
    int issueUnitsPerPackCol = 8;
    int issueUnitCol = 9;
    int packUnitCol = 10;
    int distributorCol = 11;
    int manufacturerCol = 12;
    int importerCol = 13;
    int doeCol = 14;
    int batchCol = 15;
    int stockQtyCol = 16;
    int pruchaseRateCol = 17;
    int saleRateCol = 18;

    int startRow = 1;
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

    @Inject
    private InstitutionController institutionController;

    @Inject
    SessionController sessionController;

    @Inject
    PharmacyPurchaseController pharmacyPurchaseController;

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public void removeDuplicateAmpps() {
        List<Ampp> temAmpps = getAmppFacade().findAll(true);
        for (Ampp ampp : temAmpps) {
            for (Ampp dup : temAmpps) {
                if (ampp.getName().equals(dup.getName())) {
                    if (ampp.isRetired() == false && dup.isRetired() == false) {
                        dup.setRetired(true);
                        getAmppFacade().edit(dup);

                    }
                }
            }
        }
    }

    @EJB
    private BillFacade billFacade;

    public void resetGrnValue() {
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where (type(b)=:class) "
                + " and b.billType = :billType ";

        temMap.put("class", BilledBill.class);
        temMap.put("billType", BillType.PharmacyGrnBill);
        //temMap.put("dep", getSessionController().getDepartment());
        List<Bill> bills = getBillFacade().findBySQL(sql, temMap);

        for (Bill b : bills) {
            if (b.getNetTotal() > 0) {
                b.setNetTotal(0 - b.getNetTotal());
                b.setTotal(0 - b.getTotal());
                getBillFacade().edit(b);
            }
        }

        sql = "select b from Bill b where (type(b)=:class) "
                + " and b.billType = :billType ";

        temMap.put("class", CancelledBill.class);
        temMap.put("billType", BillType.PharmacyGrnBill);
        //temMap.put("dep", getSessionController().getDepartment());
        bills = getBillFacade().findBySQL(sql, temMap);

        for (Bill b : bills) {
            if (b.getNetTotal() < 0) {
                b.setNetTotal(0 - b.getNetTotal());
                b.setTotal(0 - b.getTotal());
                getBillFacade().edit(b);
            }
        }

    }

    public void resetBillNo() {
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where b.createdAt between :fd and :td ";

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, -40);
        temMap.put("fd", cal.getTime());
        temMap.put("td", new Date());
        //temMap.put("dep", getSessionController().getDepartment());
        List<Bill> bills = getBillFacade().findBySQL(sql, temMap);

        for (Bill b : bills) {
            String str = "";
            //Reset Institution ID
            if (b.getInsId() != null) {
                str = b.getInsId().replace('\\', '/');
                //  System.err.println("Ins No " + b.getInsId() + " : " + str);
                b.setInsId(str);
            }

            //Reset Department ID
            if (b.getDeptId() != null) {
                str = b.getDeptId().replace('\\', '/');
                //    System.err.println("Dept No " + b.getDeptId() + " : " + str);
                b.setDeptId(str);
            }

            getBillFacade().edit(b);
        }

    }

    @EJB
    private ItemFacade itemFacade;

    public void resetSessionBillNumberType() {
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Item b where type(b)=:tp ";
        temMap.put("tp", Service.class);
        List<Item> list = getItemFacade().findBySQL(sql, temMap);

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

        List<Bill> bills = billFacade.findBySQL(sql, hm);

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
        List<PatientEncounter> list = patientEncounterFacade.findBySQL(sql, hm);

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
        List<Item> list = getItemFacade().findBySQL(sql, temMap);

        for (Item i : list) {
            i.setInwardChargeType(InwardChargeType.Laboratory);
            getItemFacade().edit(i);
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
        List<Item> list = getItemFacade().findBySQL(sql, temMap);

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
        List<Item> list = getItemFacade().findBySQL(sql, temMap);

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

        List<Item> list = getItemFacade().findBySQL(sql, temMap);
        System.err.println("SIZE " + list.size());
        for (Item i : list) {
            System.err.println("Item Name " + i.getName());
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
//        List<Bill> list = getBillFacade().findBySQL(sql, temMap);
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
//        List<Bill> bills = getBillFacade().findBySQL(sql, temMap);
//
//        for (Bill b : bills) {
//            System.err.println("Billed "+b.getPaymentScheme());
//            System.err.println("Cancelled "+b.getCancelledBill().getPaymentScheme());
//        }
//
//    }
    public void resetGrnReference() {
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where (type(b)=:class) "
                + " and b.billType = :billType ";

        temMap.put("class", BilledBill.class);
        temMap.put("billType", BillType.PharmacyGrnBill);
        //temMap.put("dep", getSessionController().getDepartment());
        List<Bill> bills = getBillFacade().findBySQL(sql, temMap);
        int index = 1;
        for (Bill b : bills) {
            if (b.getReferenceBill().getBillType() == BillType.PharmacyOrder) {
                System.err.println("No " + index++);
                Bill refApproved = b.getReferenceBill().getReferenceBill();
                System.err.println("Grn No" + b.getDeptId());
                System.err.println("Po No " + b.getReferenceBill().getDeptId());
                System.err.println("1 " + b.getBillType());
                System.err.println("2 " + b.getReferenceBill().getBillType());
                System.err.println("3" + refApproved.getBillType());
                System.err.println("%%%%%%%%%%%%%%%%%%%%");

                b.setReferenceBill(refApproved);
                getBillFacade().edit(b);

            }

        }

    }

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
                + " and upper(c.name) like :nm ";
        return getItemFacade().findFirstBySQL(sql, m);
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
            System.err.println("Removing Store Item " + storeItem.getName());

            Item pharmacyItem = fetchPharmacyItem(storeItem);

            if (pharmacyItem == null) {
                continue;
            }

            double qty = fetchStockQty(pharmacyItem);

            if (qty != 0) {
                UtilityController.addErrorMessage(pharmacyItem.getName() + " NOT Removed Beacause there is stock");
                continue;
            }

            System.err.println("Removing Pharmacy Item " + pharmacyItem.getName());

            pharmacyItem.setRetired(true);
            pharmacyItem.setRetireComments("Bulk Remove");
            pharmacyItem.setRetirer(getSessionController().getLoggedUser());
            getItemFacade().edit(pharmacyItem);
        }
    }

    @EJB
    private BillItemFacade billItemFacade;

    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;

    public void resetQtyValue() {
        String sql = "Select p from PharmaceuticalBillItem p where p.billItem.retired=false ";

        List<PharmaceuticalBillItem> lis = getPharmaceuticalBillItemFacade().findBySQL(sql);

        for (PharmaceuticalBillItem ph : lis) {
            if (ph.getBillItem() == null || ph.getBillItem().getBill() == null
                    || ph.getBillItem().getBill().getBillType() == null) {
                continue;
            }
            switch (ph.getBillItem().getBill().getBillType()) {
                case PharmacyGrnBill:
                    if (ph.getBillItem().getBill() instanceof BilledBill) {
                        if (ph.getQtyInUnit() < 0) {
                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
                        }
                        if (ph.getFreeQtyInUnit() < 0) {
                            ph.setFreeQtyInUnit(0 - ph.getFreeQtyInUnit());
                        }
                    }
                    if (ph.getBillItem().getBill() instanceof CancelledBill) {
                        if (ph.getQtyInUnit() > 0) {
                            System.err.println("Error 2 " + ph.getQtyInUnit());
                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
                        }
                        if (ph.getFreeQtyInUnit() > 0) {
                            ph.setFreeQtyInUnit(0 - ph.getFreeQtyInUnit());
                        }
                    }
                    break;
                case PharmacyGrnReturn:
                    if (ph.getBillItem().getBill() instanceof BilledBill) {
                        if (ph.getQtyInUnit() > 0) {
                            System.err.println("Error 3 " + ph.getQtyInUnit());
                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
                        }
                        if (ph.getFreeQtyInUnit() > 0) {
                            ph.setFreeQtyInUnit(0 - ph.getFreeQtyInUnit());
                        }
                    }
                    if (ph.getBillItem().getBill() instanceof CancelledBill) {
                        if (ph.getQtyInUnit() < 0) {
                            System.err.println("Error 4 " + ph.getQtyInUnit());
                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
                        }
                        if (ph.getFreeQtyInUnit() < 0) {
                            ph.setFreeQtyInUnit(0 - ph.getFreeQtyInUnit());
                        }
                    }
                    break;
                case PharmacyTransferIssue:
                    if (ph.getBillItem().getBill() instanceof BilledBill) {
                        if (ph.getQtyInUnit() > 0) {
                            System.err.println("Error 5 " + ph.getQtyInUnit());
                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
                        }
                    }
                    if (ph.getBillItem().getBill() instanceof CancelledBill) {
                        if (ph.getQtyInUnit() < 0) {
                            System.err.println("Error 6 " + ph.getQtyInUnit());
                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
                        }
                    }
                    break;
                case PharmacyTransferReceive:
                    if (ph.getBillItem().getBill() instanceof BilledBill) {
                        if (ph.getQtyInUnit() < 0) {
                            System.err.println("Error 7 " + ph.getQtyInUnit());
                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
                        }
                    }
                    if (ph.getBillItem().getBill() instanceof CancelledBill) {
                        if (ph.getQtyInUnit() > 0) {
                            System.err.println("Error 8 " + ph.getQtyInUnit());
                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
                        }
                    }
                    break;
                case PharmacyPurchaseBill:
                    if (ph.getBillItem().getBill() instanceof BilledBill) {
                        if (ph.getQtyInUnit() < 0) {
                            System.err.println("Error 9 " + ph.getQtyInUnit());
                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
                        }
                        if (ph.getFreeQtyInUnit() < 0) {
                            ph.setFreeQtyInUnit(0 - ph.getFreeQtyInUnit());
                        }
                    }
                    if (ph.getBillItem().getBill() instanceof CancelledBill) {
                        if (ph.getQtyInUnit() > 0) {
                            System.err.println("Error 10 " + ph.getQtyInUnit());
                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
                        }
                        if (ph.getFreeQtyInUnit() > 0) {
                            ph.setFreeQtyInUnit(0 - ph.getFreeQtyInUnit());
                        }
                    }
                    break;
                case PharmacyPre:
                    if (ph.getBillItem().getBill() instanceof BilledBill) {
                        if (ph.getQtyInUnit() > 0) {
                            System.err.println("Error 11 " + ph.getQtyInUnit());
                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
                        }
                    }
                    if (ph.getBillItem().getBill() instanceof CancelledBill) {
                        if (ph.getQtyInUnit() < 0) {
                            System.err.println("Error 12 " + ph.getQtyInUnit());
                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
                        }
                    }
                    break;
                case PharmacySale:
                    if (ph.getBillItem().getBill() instanceof BilledBill) {
                        if (ph.getQtyInUnit() > 0) {
                            System.err.println("Error 13 " + ph.getQtyInUnit());
                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
                        }
                    }
                    if (ph.getBillItem().getBill() instanceof CancelledBill) {
                        if (ph.getQtyInUnit() < 0) {
                            System.err.println("Error 14 " + ph.getQtyInUnit());
                            ph.setQtyInUnit(0 - ph.getQtyInUnit());
                        }
                    }
                    break;

            }

            getPharmaceuticalBillItemFacade().edit(ph);

        }

    }

    public void resetTransferIssueValue() {
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where b.billType = :billType or b.billType = :billType2  ";

        temMap.put("billType", BillType.PharmacyTransferIssue);
        temMap.put("billType2", BillType.PharmacyTransferReceive);
        //temMap.put("dep", getSessionController().getDepartment());
        List<Bill> bills = getBillFacade().findBySQL(sql, temMap);

        for (Bill b : bills) {
            temMap.clear();
            double totalBySql = 0;
            sql = "select sum(bi.pharmaceuticalBillItem.itemBatch.purcahseRate * bi.pharmaceuticalBillItem.qty) "
                    + " from BillItem bi where bi.retired=false and bi.bill=:b ";
            temMap.put("b", b);
            totalBySql = getBillItemFacade().findDoubleByJpql(sql, temMap);

//            if (b.getNetTotal() != totalBySql) {
            System.err.println("Net Total " + b.getNetTotal());
            System.err.println("Sql Total " + totalBySql);
            b.setNetTotal(totalBySql);
            getBillFacade().edit(b);
//            }
        }

    }

    @EJB
    private StockHistoryFacade stockHistoryFacade;

    private StockHistory getPreviousStockHistoryByBatch(ItemBatch itemBatch, Department department, Date date) {
        String sql = "Select sh from StockHistory sh where sh.retired=false and"
                + " sh.itemBatch=:itmB and sh.department=:dep and sh.pbItem.billItem.createdAt<:dt "
                + " order by sh.pbItem.billItem.createdAt desc";
        HashMap hm = new HashMap();
        hm.put("itmB", itemBatch);
        hm.put("dt", date);
        hm.put("dep", department);
        return getStockHistoryFacade().findFirstBySQL(sql, hm, TemporalType.TIMESTAMP);
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
        return getPharmaceuticalBillItemFacade().findFirstBySQL(sql, hm, TemporalType.TIMESTAMP);
    }

    public void resetTransferHistoryValue() {
        String sql;
        Map temMap = new HashMap();

        sql = "select p from PharmaceuticalBillItem p where p.billItem.bill.billType = :billType or "
                + " p.billItem.bill.billType = :billType2 and p.stockHistory is not null order by p.stockHistory.id ";

        temMap.put("billType", BillType.PharmacyTransferIssue);
        temMap.put("billType2", BillType.PharmacyTransferReceive);
        List<PharmaceuticalBillItem> list = getPharmaceuticalBillItemFacade().findBySQL(sql, temMap);

        for (PharmaceuticalBillItem b : list) {
            System.err.println("Item Name " + b.getBillItem().getItem().getName());
            System.err.println("History Id " + b.getStockHistory().getId());
            System.err.println("Stock History " + b.getStockHistory().getStockQty());
            System.err.println("Department " + b.getBillItem().getBill().getDepartment().getName());
            StockHistory sh = getPreviousStockHistoryByBatch(b.getItemBatch(), b.getBillItem().getBill().getDepartment(), b.getBillItem().getCreatedAt());
            PharmaceuticalBillItem phi = getPreviousPharmacuticalBillByBatch(b.getStock().getItemBatch(), b.getBillItem().getBill().getDepartment(), b.getBillItem().getCreatedAt());
            if (sh != null) {
                System.err.println("Prev History Id " + sh.getId());
                //   System.out.println("Previuos Stock " + sh.getStockQty());
                //   System.out.println("Ph Qty " + sh.getPbItem().getQtyInUnit() + sh.getPbItem().getFreeQtyInUnit());
                //   System.out.println("Acc Qty " + (sh.getStockQty() + sh.getPbItem().getQtyInUnit() + sh.getPbItem().getFreeQtyInUnit()));
                b.getStockHistory().setStockQty((sh.getStockQty() + sh.getPbItem().getQtyInUnit() + sh.getPbItem().getFreeQtyInUnit()));
            } else if (phi != null) {
                b.getStockHistory().setStockQty(phi.getQtyInUnit() + phi.getFreeQtyInUnit());
            } else {
                b.getStockHistory().setStockQty(0.0);
            }
            //   System.out.println("#########");
            getStockHistoryFacade().edit(b.getStockHistory());

        }

    }

    public String importToExcelWithStock() {
        //System.out.println("importing to excel");
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
        Vtm vtm;
        Atm atm;
        Vmp vmp;
        Amp amp;
        Ampp ampp;
        Vmpp vmpp;
        VtmsVmps vtmsvmps;
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
        UtilityController.addSuccessMessage(file.getFileName());
        try {
            UtilityController.addSuccessMessage(file.getFileName());
            in = file.getInputstream();
            File f;
            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
            FileOutputStream out = new FileOutputStream(f);
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            inputWorkbook = new File(f.getAbsolutePath());

            UtilityController.addSuccessMessage("Excel File Opened");
            w = Workbook.getWorkbook(inputWorkbook);
            Sheet sheet = w.getSheet(0);

            getPharmacyPurchaseController().makeNull();

            for (int i = startRow; i < sheet.getRows(); i++) {

                Map m = new HashMap();

                //Category
                cell = sheet.getCell(catCol, i);
                strCat = cell.getContents();
                //System.out.println("strCat is " + strCat);
                cat = getPharmacyBean().getPharmaceuticalCategoryByName(strCat);
                if (cat == null) {
                    continue;
                }
                //System.out.println("cat = " + cat.getName());

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
                if (!strStrength.equals("")) {
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
                if (!strPackSize.equals("")) {
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
                if (!strGenericName.equals("")) {
                    vtm = getPharmacyBean().getVtmByName(strGenericName);
                } else {
                    //System.out.println("vtm is null");
                    vtm = null;
                }

                //Vmp
                vmp = getPharmacyBean().getVmp(vtm, strengthUnitsPerIssueUnit, strengthUnit, cat);
                if (vmp == null) {
                    //System.out.println("vmp is null");
                    continue;
                }
                //System.out.println("vmp = " + vmp.getName());
                //Amp
                cell = sheet.getCell(ampCol, i);
                strAmp = cell.getContents();
                //System.out.println("strAmp = " + strAmp);
                m = new HashMap();
                m.put("v", vmp);
                m.put("n", strAmp);
                if (!strCat.equals("")) {
                    amp = ampFacade.findFirstBySQL("SELECT c FROM Amp c Where upper(c.name)=:n AND c.vmp=:v", m);
                    if (amp == null) {
                        amp = new Amp();
                        amp.setName(strAmp);
                        amp.setMeasurementUnit(strengthUnit);
                        amp.setDblValue((double) strengthUnitsPerIssueUnit);
                        amp.setCategory(cat);
                        amp.setVmp(vmp);
                        getAmpFacade().create(amp);
                    } else {
                        amp.setRetired(false);
                        getAmpFacade().edit(amp);
                    }
                } else {
                    amp = null;
                    //System.out.println("amp is null");
                }
                if (amp == null) {
                    continue;
                }
                //System.out.println("amp = " + amp.getName());
                //Ampp
                ampp = getPharmacyBean().getAmpp(amp, issueUnitsPerPack, packUnit);

                //Code
                cell = sheet.getCell(codeCol, i);
                strCode = cell.getContents();
                //System.out.println("strCode = " + strCode);
                amp.setCode(strCode);
                getAmpFacade().edit(amp);
                //Code
                cell = sheet.getCell(barcodeCol, i);
                strBarcode = cell.getContents();
                //System.out.println("strBarCode = " + strBarcode);
                amp.setCode(strBarcode);
                getAmpFacade().edit(amp);
                //Distributor
                cell = sheet.getCell(distributorCol, i);
                strDistributor = cell.getContents();
                distributor = getInstitutionController().getInstitutionByName(strDistributor, InstitutionType.Dealer);
                if (distributor != null) {
                    //System.out.println("distributor = " + distributor.getName());
                    ItemsDistributors id = new ItemsDistributors();
                    id.setInstitution(distributor);
                    id.setItem(amp);
                    id.setOrderNo(0);
                    getItemsDistributorsFacade().create(id);
                } else {
                    //System.out.println("distributor is null");
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
                    doe = new SimpleDateFormat("M/d/yyyy", Locale.ENGLISH).parse(temStr);
                } catch (Exception e) {
                    doe = new Date();
                }

                getPharmacyPurchaseController().getCurrentBillItem().setItem(amp);
                System.out.println("getPharmacyPurchaseController().getCurrentBillItem().setItem(amp) = " + getPharmacyPurchaseController().getCurrentBillItem().getItem());
                getPharmacyPurchaseController().getCurrentBillItem().setTmpQty(stockQty);
                System.out.println("getPharmacyPurchaseController().getCurrentBillItem().setTmpQty(stockQty) = " + getPharmacyPurchaseController().getCurrentBillItem().getTmpQty());
                getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().setPurchaseRate(pp);
                System.out.println("getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().setPurchaseRate(pp); = " + getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().getPurchaseRate());
                getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().setRetailRate(sp);
                System.out.println("getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().setRetailRate(sp); = " + getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().getRetailRate());
                getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().setDoe(doe);
                System.out.println("getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().setDoe(doe) = " + getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().getDoe());
                if (batch == null || batch.trim().equals("")) {
                    getPharmacyPurchaseController().setBatch();
                } else {
                    getPharmacyPurchaseController().getCurrentBillItem().getPharmaceuticalBillItem().setStringValue(batch);
                }
                getPharmacyPurchaseController().addItem();
            }
            UtilityController.addSuccessMessage("Succesful. All the data in Excel File Impoted to the database");
            return "/pharmacy/pharmacy_purchase";
        } catch (IOException | BiffException ex) {
            UtilityController.addErrorMessage(ex.getMessage());
            return "";
        }
    }

    public String importToExcel() {
        //System.out.println("importing to excel");
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
        Vtm vtm;
        Atm atm;
        Vmp vmp;
        Amp amp;
        Ampp ampp;
        Vmpp vmpp;
        VtmsVmps vtmsvmps;
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
        UtilityController.addSuccessMessage(file.getFileName());
        try {
            UtilityController.addSuccessMessage(file.getFileName());
            in = file.getInputstream();
            File f;
            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
            FileOutputStream out = new FileOutputStream(f);
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            inputWorkbook = new File(f.getAbsolutePath());

            UtilityController.addSuccessMessage("Excel File Opened");
            w = Workbook.getWorkbook(inputWorkbook);
            Sheet sheet = w.getSheet(0);

            for (int i = startRow; i < sheet.getRows(); i++) {

                Map m = new HashMap();

                //Category
                cell = sheet.getCell(catCol, i);
                strCat = cell.getContents();
                //System.out.println("strCat is " + strCat);
                cat = getPharmacyBean().getPharmaceuticalCategoryByName(strCat);
                if (cat == null) {
                    continue;
                }
                //System.out.println("cat = " + cat.getName());

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
                if (!strStrength.equals("")) {
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
                if (!strPackSize.equals("")) {
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
                if (!strGenericName.equals("")) {
                    vtm = getPharmacyBean().getVtmByName(strGenericName);
                } else {
                    //System.out.println("vtm is null");
                    vtm = null;
                }

                //Vmp
                vmp = getPharmacyBean().getVmp(vtm, strengthUnitsPerIssueUnit, strengthUnit, cat);
                if (vmp == null) {
                    //System.out.println("vmp is null");
                    continue;
                }
                //System.out.println("vmp = " + vmp.getName());
                //Amp
                cell = sheet.getCell(ampCol, i);
                strAmp = cell.getContents();
                //System.out.println("strAmp = " + strAmp);
                m = new HashMap();
                m.put("v", vmp);
                m.put("n", strAmp);
                if (!strCat.equals("")) {
                    amp = ampFacade.findFirstBySQL("SELECT c FROM Amp c Where upper(c.name)=:n AND c.vmp=:v", m);
                    if (amp == null) {
                        amp = new Amp();
                        amp.setName(strAmp);
                        amp.setMeasurementUnit(strengthUnit);
                        amp.setDblValue((double) strengthUnitsPerIssueUnit);
                        amp.setCategory(cat);
                        amp.setVmp(vmp);
                        getAmpFacade().create(amp);
                    } else {
                        amp.setRetired(false);
                        getAmpFacade().edit(amp);
                    }
                } else {
                    amp = null;
                    //System.out.println("amp is null");
                }
                if (amp == null) {
                    continue;
                }
                //System.out.println("amp = " + amp.getName());
                //Ampp
                ampp = getPharmacyBean().getAmpp(amp, issueUnitsPerPack, packUnit);

                //Code
                cell = sheet.getCell(codeCol, i);
                strCode = cell.getContents();
                //System.out.println("strCode = " + strCode);
                amp.setCode(strCode);
                getAmpFacade().edit(amp);
                //Code
                cell = sheet.getCell(barcodeCol, i);
                strBarcode = cell.getContents();
                //System.out.println("strBarCode = " + strBarcode);
                amp.setCode(strBarcode);
                getAmpFacade().edit(amp);
                //Distributor
                cell = sheet.getCell(distributorCol, i);
                strDistributor = cell.getContents();
                distributor = getInstitutionController().getInstitutionByName(strDistributor, InstitutionType.Dealer);
                if (distributor != null) {
                    //System.out.println("distributor = " + distributor.getName());
                    ItemsDistributors id = new ItemsDistributors();
                    id.setInstitution(distributor);
                    id.setItem(amp);
                    id.setOrderNo(0);
                    getItemsDistributorsFacade().create(id);
                } else {
                    //System.out.println("distributor is null");
                }
            }

            UtilityController.addSuccessMessage("Succesful. All the data in Excel File Impoted to the database");
            return "";
        } catch (IOException ex) {
            UtilityController.addErrorMessage(ex.getMessage());
            return "";
        } catch (BiffException e) {
            UtilityController.addErrorMessage(e.getMessage());
            return "";
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
        UtilityController.addSuccessMessage(file.getFileName());
        try {
            UtilityController.addSuccessMessage(file.getFileName());
            in = file.getInputstream();
            File f;
            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
            FileOutputStream out = new FileOutputStream(f);
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            inputWorkbook = new File(f.getAbsolutePath());

            UtilityController.addSuccessMessage("Excel File Opened");
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

                if (catName == null || catName.trim().equals("") || itenName == null || itenName.trim().equals("")) {
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

                amp = ampFacade.findFirstBySQL("SELECT c FROM Amp c Where upper(c.name)=:n AND c.departmentType=:dep ", m);

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

            UtilityController.addSuccessMessage("Succesful. All the data in Excel File Impoted to the database");
            return "";
        } catch (IOException ex) {
            UtilityController.addErrorMessage(ex.getMessage());
            return "";
        } catch (BiffException e) {
            UtilityController.addErrorMessage(e.getMessage());
            return "";
        }
    }

    public String detectMismatch() {
        //   System.out.println("dictecting mismatch");
        String itemName;
        String itemCode;
        String genericName;

        File inputWorkbook;
        Workbook w;
        Cell cell;
        InputStream in;
        UtilityController.addSuccessMessage(file.getFileName());
        try {
            UtilityController.addSuccessMessage(file.getFileName());
            in = file.getInputstream();
            File f;
            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
            FileOutputStream out = new FileOutputStream(f);
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            inputWorkbook = new File(f.getAbsolutePath());

            UtilityController.addSuccessMessage("Excel File Opened");
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
                //   System.out.println("m = " + m);
                sql = "Select amp from Amp amp where amp.retired=false and upper(amp.name)=:strAmp";
                //   System.out.println("sql = " + sql);
                Amp amp = getAmpFacade().findFirstBySQL(sql, m);
                //   System.out.println("amp = " + amp);
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
                    //   System.out.println("added to list");
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
                        //   System.out.println("e = " + e);
                    }
                    npi.setItem6_StrengthOfIssueUnit(d);

                    npi.setItem7_StrengthUnit(sheet.getCell(6, i).getContents());

                    try {
                        d = Double.parseDouble(sheet.getCell(7, i).getContents());
                    } catch (NumberFormatException e) {
                        d = 0.0;
                        //   System.out.println("e = " + e);
                    }
                    npi.setItem8_IssueUnitsPerPack(d);

                    npi.setItem9_IssueUnit(sheet.getCell(8, i).getContents());
                    npi.setItem10_PackUnit(sheet.getCell(9, i).getContents());

                    itemNotPresent.add(npi);

                }

            }
            UtilityController.addSuccessMessage("Succesful. All Mismatches listed below.");
            return "";
        } catch (IOException | BiffException ex) {
            //   System.out.println("ex = " + ex);
            UtilityController.addErrorMessage(ex.getMessage());
            return "";
        }
    }

    public String importToExcelBarcode() {
        //System.out.println("importing to excel");

        String strAmp;
        String strBarcode;

        PharmaceuticalItemCategory cat;

        Amp amp;
        File inputWorkbook;
        Workbook w;
        Cell cell;
        InputStream in;
        UtilityController.addSuccessMessage(file.getFileName());
        try {
            UtilityController.addSuccessMessage(file.getFileName());
            in = file.getInputstream();
            File f;
            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
            FileOutputStream out = new FileOutputStream(f);
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            inputWorkbook = new File(f.getAbsolutePath());

            UtilityController.addSuccessMessage("Excel File Opened");
            w = Workbook.getWorkbook(inputWorkbook);
            Sheet sheet = w.getSheet(0);

            for (int i = startRow; i < sheet.getRows(); i++) {

                Map m = new HashMap();

                //Amp
                cell = sheet.getCell(ampCol, i);
                strAmp = cell.getContents();
                //System.out.println("strAmp = " + strAmp);
                m = new HashMap();
                m.put("n", strAmp);

                amp = ampFacade.findFirstBySQL("SELECT c FROM Amp c Where upper(c.name)=:n", m);
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

            UtilityController.addSuccessMessage("Succesful. All the data in Excel File Impoted to the database");
            return "";
        } catch (IOException ex) {
            UtilityController.addErrorMessage(ex.getMessage());
            return "";
        } catch (BiffException e) {
            UtilityController.addErrorMessage(e.getMessage());
            return "";
        }
    }

    public String importToExcelCategoriOnly() {
        //System.out.println("importing to excel");
        String strCat;
        String strAmp = null;

        PharmaceuticalItemCategory cat;
        Amp amp;

        File inputWorkbook;
        Workbook w;
        Cell cell;
        InputStream in;
        UtilityController.addSuccessMessage(file.getFileName());
        try {
            UtilityController.addSuccessMessage(file.getFileName());
            in = file.getInputstream();
            File f;
            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
            FileOutputStream out = new FileOutputStream(f);
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            inputWorkbook = new File(f.getAbsolutePath());

            UtilityController.addSuccessMessage("Excel File Opened");
            w = Workbook.getWorkbook(inputWorkbook);
            Sheet sheet = w.getSheet(0);

            for (int i = startRow; i < sheet.getRows(); i++) {

                Map m = new HashMap();

                //Category
                cell = sheet.getCell(catCol, i);
                strCat = cell.getContents();
                //System.out.println("strCat is " + strCat);
                cat = getPharmacyBean().getPharmaceuticalCategoryByName(strCat);
                if (cat == null) {
                    continue;
                }
                //System.out.println("cat = " + cat.getName());
                if (!strCat.equals("")) {
                    amp = ampFacade.findFirstBySQL("SELECT c FROM Amp c Where upper(c.name)=:n AND c.vmp=:v", m);
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
                } else {
                    amp = null;
                    //System.out.println("amp is null");
                }
                if (amp == null) {
                    continue;
                }
            }

            UtilityController.addSuccessMessage("Succesful. All the data in Excel File Impoted to the database");
            return "";
        } catch (IOException ex) {
            UtilityController.addErrorMessage(ex.getMessage());
            return "";
        } catch (BiffException e) {
            UtilityController.addErrorMessage(e.getMessage());
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
        List<PharmaceuticalItem> pis = getPharmaceuticalItemFacade().findBySQL(sql);
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

    public VtmsVmpsFacade getVtmInAmpFacade() {
        return vtmInAmpFacade;
    }

    public void setVtmInAmpFacade(VtmsVmpsFacade vtmInAmpFacade) {
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

    public VtmsVmpsFacade getVtmsVmpsFacade() {
        return vtmInAmpFacade;
    }

    public void setVtmsVmpsFacade(VtmsVmpsFacade vtmInAmpFacade) {
        this.vtmInAmpFacade = vtmInAmpFacade;
    }

    public List<Ampp> getAmpps() {
        return getAmppFacade().findAll();
    }

    public void setAmpps(List<Ampp> ampps) {
        this.ampps = ampps;
    }

    public List<Amp> getAmps() {
        return getAmpFacade().findAll();
    }

    public void setAmps(List<Amp> amps) {
        this.amps = amps;
    }

    public List<Vtm> getVtms() {
        return getVtmFacade().findAll();
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

}
