/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ejb;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.ItemsDistributors;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.PharmaceuticalItem;
import com.divudi.core.entity.pharmacy.Vmp;
import com.divudi.core.entity.pharmacy.Vmpp;
import com.divudi.core.facade.AmpFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.CategoryFacade;
import com.divudi.core.facade.ItemBatchFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.ItemsDistributorsFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@ApplicationScoped
public class PharmacyCalculation implements Serializable {

    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private AmpFacade ampFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private ItemBatchFacade itemBatchFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private ItemsDistributorsFacade itemsDistributorsFacade;
    @EJB
    private CategoryFacade categoryFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillNumberGenerator billNumberBean;

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    private static final int PRICE_SCALE = 6;

//    public void editBill(Bill bill, Bill ref, SessionController sc) {
//
//
//    }
    public List<Item> getSuggessionOnly(Item item) {
        List<Item> suggessions = new ArrayList<>();

        if (item instanceof Amp) {
            suggessions = findPack((Amp) item);
            suggessions.add(item);
        } else if (item instanceof Ampp) {
            Amp amp = ((Ampp) item).getAmp();
            suggessions = findPack(amp);
            suggessions.add(amp);
        }

        return suggessions;
    }

    public List<Item> getItemsForDealor(Institution i) {
        String jpql;
        HashMap params = new HashMap();
        List<Item> dealerItems;
        params.put("ins", i);
        jpql = "SELECT i.item FROM ItemsDistributors i "
                + " where i.retired=false "
                + " and i.item.retired=false"
                + " and i.institution=:ins "
                + " order by i.item.name ";
        dealerItems = getItemFacade().findByJpql(jpql, params);

        if (dealerItems == null) {
            dealerItems = new ArrayList<>();
        }
        return dealerItems;
    }

    public boolean checkItem(Institution ins, Item i) {
        String sql = "Select i from ItemsDistributors i where i.retired=false and i.institution.id= " + ins.getId() + " and i.item.id=" + i.getId();
        ItemsDistributors tmp = getItemsDistributorsFacade().findFirstByJpql(sql);
        if (tmp != null) {
            return true;
        }

        return false;
    }

    public double calRetailRate(PharmaceuticalBillItem ph) {

        PharmaceuticalItem i = (PharmaceuticalItem) ph.getBillItem().getItem();
        double margin = 0.0;
        String sql;
        Category cat;

        if (i instanceof Amp) {
            sql = "Select p.category from Vmp p where p.retired=false and p.id in (Select a.vmp.id from Amp a where a.retired=false and a.id=" + i.getId() + ")";
        } else if (i instanceof Ampp) {
            sql = "Select p.category from Vmp p where p.retired=false and p.id in (Select a.amp.vmp.id from Ampp a where a.retired=false and a.id=" + i.getId() + ")";

        } else {
            return 0.0f;
        }

        cat = getCategoryFacade().findFirstByJpql(sql);

        if (cat != null) {
            margin = cat.getSaleMargin();
        }

        double retailPrice = ph.getPurchaseRate() + (ph.getPurchaseRate() * (margin / 100));

        return retailPrice;
    }

    public double getTotalQty(BillItem b, BillType billType, Bill bill) {
        String sql = "Select sum(p.pharmaceuticalBillItem.qty) from BillItem p where"
                + "  type(p.bill)=:class and p.creater is not null and"
                + " p.referanceBillItem=:bt and p.bill.billType=:btp";

        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("btp", billType);
        hm.put("class", bill.getClass());

        //System.err.println("GETTING TOTAL QTY " + value);
        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public double getQtyPlusFreeQtyInUnits(BillItem b, BillType billType, Bill bill) {
        String sql = "Select sum(p.pharmaceuticalBillItem.qty+p.pharmaceuticalBillItem.freeQty) from BillItem p where"
                + "  type(p.bill)=:class and p.creater is not null and"
                + " p.referanceBillItem=:bt and p.bill.billType=:btp";
        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("btp", billType);
        hm.put("class", bill.getClass());
        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public double getQtyPlusFreeQtyInUnits(BillItem b, BillTypeAtomic billTypeAtomic) {
        String sql = "Select sum(p.pharmaceuticalBillItem.qty+p.pharmaceuticalBillItem.freeQty) "
                + " from BillItem p "
                + " where p.retired=false "
                + " and p.referanceBillItem=:bt "
                + " and p.bill.billTypeAtomic=:btp";
        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("btp", billTypeAtomic);
        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }
    
    public double getFreeQtyInUnits(BillItem b, BillTypeAtomic billTypeAtomic) {
        String sql = "Select sum(p.pharmaceuticalBillItem.freeQty) "
                + " from BillItem p "
                + " where p.retired=false "
                + " and p.referanceBillItem=:bt "
                + " and p.bill.billTypeAtomic=:btp";
        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("btp", billTypeAtomic);
        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }
    
    public double getQtyInUnits(BillItem b, BillTypeAtomic billTypeAtomic) {
        String sql = "Select sum(p.pharmaceuticalBillItem.qty) "
                + " from BillItem p "
                + " where p.retired=false "
                + " and p.referanceBillItem=:bt "
                + " and p.bill.billTypeAtomic=:btp";
        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("btp", billTypeAtomic);
        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public double getTotalFreeQty(BillItem b, BillType billType, Bill bill) {
        String sql = "Select sum(p.pharmaceuticalBillItem.freeQty) from BillItem p where"
                + "  type(p.bill)=:class and p.creater is not null and"
                + " p.referanceBillItem=:bt and p.bill.billType=:btp";

        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("btp", billType);
        hm.put("class", bill.getClass());

        //System.err.println("GETTING TOTAL QTY " + value);
        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public double getTotalQty(BillItem b, BillType billType) {
        String sql = "Select sum(p.pharmaceuticalBillItem.qty) from BillItem p where"
                + "  p.creater is not null and"
                + " p.referanceBillItem=:bt and p.bill.billType=:btp";

        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("btp", billType);

        //System.err.println("GETTING TOTAL QTY " + value);
        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }
    
    public double getTotalQty(BillItem b, List<BillTypeAtomic> btas) {
        if (btas == null || btas.isEmpty()) {
            return 0.0;
        }
        String sql = "Select sum(p.pharmaceuticalBillItem.qty) "
                + " from BillItem p "
                + " where (p.bill.retired is null or p.bill.retired=false)"
                + " and (p.retired is null or p.retired=false) "
                + " and p.referanceBillItem=:bt "
                + " and p.bill.billTypeAtomic in :btas";

        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("btas", btas);

        //System.err.println("GETTING TOTAL QTY " + value);
        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public double getTotalFreeQty(BillItem b, BillType billType) {
        String sql = "Select sum(p.pharmaceuticalBillItem.freeQty) from BillItem p where"
                + "  p.creater is not null and"
                + " p.referanceBillItem=:bt and p.bill.billType=:btp";

        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("btp", billType);

        //System.err.println("GETTING TOTAL QTY " + value);
        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public double getBilledIssuedByRequestedItem(BillItem b, BillType billType) {
        String sql = "Select sum(p.qty) from BillItem p where"
                + "  p.creater is not null and type(p.bill)=:class and "
                + " p.referanceBillItem=:bt and p.bill.billType=:btp";

        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("class", BilledBill.class);
        hm.put("btp", billType);

        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public double getBilledIssuedByRequestedItemBatch(BillItem b, BillType billType, ItemBatch batch) {
        String sql = "Select sum(p.pharmaceuticalBillItem.qty) from BillItem p where"
                + "  p.creater is not null and type(p.bill)=:class and "
                + " p.referanceBillItem=:bt and p.bill.billType=:btp "
                + " and p.pharmaceuticalBillItem.itemBatch = :batch";

        BillItem bb = new BillItem();
        bb.getPharmaceuticalBillItem().getItemBatch();

        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("batch", batch);
        hm.put("class", BilledBill.class);
        hm.put("btp", billType);

        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public double getBilledInwardPharmacyRequest(BillItem b, BillType billType) {
        String sql = "Select sum(p.pharmaceuticalBillItem.qty) from BillItem p where"
                + "  p.creater is not null and type(p.bill)=:class and "
                + " p.referanceBillItem=:bt and p.bill.billType=:btp";

        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("class", PreBill.class);
        hm.put("btp", billType);

        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public double getCancelledIssuedByRequestedItem(BillItem b, BillType billType) {
        String sql = "Select sum(p.qty) from BillItem p where"
                + "  p.creater is not null and type(p.bill)=:class and "
                + " p.referanceBillItem.referanceBillItem=:bt and p.bill.billType=:btp";

        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("class", CancelledBill.class);
        hm.put("btp", billType);

        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public double getCancelledIssuedByRequestedItemBatch(BillItem b, BillType billType, ItemBatch batch) {
        String sql = "Select sum(p.pharmaceuticalBillItem.qty) from BillItem p where"
                + "  p.creater is not null and type(p.bill)=:class and "
                + " p.referanceBillItem.referanceBillItem=:bt and p.bill.billType=:btp "
                + " and p.pharmaceuticalBillItem.itemBatch = :batch ";

        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("batch", batch);
        hm.put("class", CancelledBill.class);
        hm.put("btp", billType);

        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public double getCancelledInwardPharmacyRequest(BillItem b, BillType billType) {
        String sql = "Select sum(p.pharmaceuticalBillItem.qty) "
                + " from BillItem p where p.creater is not null "
                + " and type(p.bill)=:class "
                + " and p.referanceBillItem=:bt "
                + " and p.bill.billType=:btp "
                + " and p.bill.cancelled=true ";

        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("class", PreBill.class);
        hm.put("btp", billType);

        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public double getRefundedInwardPharmacyRequest(BillItem b, BillType billType) {
        String sql = "Select sum(p.pharmaceuticalBillItem.qty) from BillItem p where"
                + "  p.creater is not null and type(p.bill)=:class and "
                + " p.referanceBillItem.referanceBillItem=:bt and p.bill.billType=:btp";

        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("class", RefundBill.class);
        hm.put("btp", billType);

        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public double getTotalQty(BillItem b, BillType billType, Bill refund, Bill cancel) {
        String sql = "Select sum(p.pharmaceuticalBillItem.qty) from BillItem p where"
                + "  (type(p.bill)=:class or type(p.bill)=:class2)and p.creater is not null and"
                + " p.referanceBillItem=:bt and p.bill.billType=:btp";

        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("btp", billType);
        hm.put("class", refund.getClass());
        hm.put("class2", cancel.getClass());
        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);

    }

//    public double getTotalQtyInSingleSql(BillItem b, BillType billType) {
//        String sql = "Select sum(p.pharmaceuticalBillItem.qty) from BillItem p where"
//                + "   p.creater is not null and"
//                + " p.referanceBillItem=:bt and p.bill.billType=:btp";
//
//        HashMap hm = new HashMap();
//        hm.put("bt", b);
//        hm.put("btp", billType);
////        hm.put("class", refund.getClass());
////        hm.put("class2", cancel.getClass());
//        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);
//
//    }
    public double getReturnedTotalQty(BillItem b, BillType billType, Bill bill) {
        String sql = "Select sum(p.pharmaceuticalBillItem.qty) from BillItem p where"
                + "  type(p.bill)=:class and p.bill.creater is not null and"
                + " p.referanceBillItem.referanceBillItem=:bt and p.bill.billType=:btp";

        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("btp", billType);
        hm.put("class", bill.getClass());

        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);

    }

    public double getReturnedTotalQtyWithFreeQty(BillItem b, BillType billType, Bill bill) {
        String sql = "Select sum(p.pharmaceuticalBillItem.qty+p.pharmaceuticalBillItem.freeQty) from BillItem p where"
                + "  type(p.bill)=:class and p.bill.creater is not null and"
                + " p.referanceBillItem.referanceBillItem=:bt and p.bill.billType=:btp";

        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("btp", billType);
        hm.put("class", bill.getClass());

        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);

    }

    public double getReturnedTotalFreeQty(BillItem b, BillType billType, Bill bill) {
        String sql = "Select sum(p.pharmaceuticalBillItem.freeQty) from BillItem p where"
                + "  type(p.bill)=:class and p.bill.creater is not null and"
                + " p.referanceBillItem.referanceBillItem=:bt and p.bill.billType=:btp";

        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("btp", billType);
        hm.put("class", bill.getClass());

        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);

    }

    public double getReturnedTotalQty(BillItem b, BillType billType) {
        String sql = "Select sum(p.pharmaceuticalBillItem.qty) from BillItem p where"
                + "  p.bill.creater is not null and"
                + " p.referanceBillItem.referanceBillItem=:bt and p.bill.billType=:btp";

        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("btp", billType);

        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);

    }

    public double getReturnedTotalFreeQty(BillItem b, BillType billType) {
        String sql = "Select sum(p.pharmaceuticalBillItem.freeQty) from BillItem p where"
                + "  p.bill.creater is not null and"
                + " p.referanceBillItem.referanceBillItem=:bt and p.bill.billType=:btp";

        HashMap hm = new HashMap();
        hm.put("bt", b);
        hm.put("btp", billType);

        return getPharmaceuticalBillItemFacade().findDoubleByJpql(sql, hm);

    }

    /**
     * Calculates the remaining quantity of items from a purchase order. Initial
     * quantity is equal to the ordered quantity. GRNs (Goods Received Notes)
     * reduce the remaining quantity. Cancelled GRNs increase the remaining
     * quantity. GRN Returns and GRN Return Cancellations are not considered.
     *
     * @param po The pharmaceutical bill item linked to the purchase order.
     * @return Remaining quantity from the original order.
     */
    public double calculateRemainigQtyFromOrder(PharmaceuticalBillItem po) {
        double billed = getTotalQty(po.getBillItem(), BillType.PharmacyGrnBill, new BilledBill());
        double cancelled = getTotalQty(po.getBillItem(), BillType.PharmacyGrnBill, new CancelledBill());;
//      double returnedB = getReturnedTotalQty(po.getBillItem(), BillType.PharmacyGrnReturn, new BilledBill());
//      double returnedC = getReturnedTotalQty(po.getBillItem(), BillType.PharmacyGrnReturn, new CancelledBill());
        double recieveNet = Math.abs(billed) - Math.abs(cancelled);
//      double retuernedNet = Math.abs(returnedB) - Math.abs(returnedC);
//      return (Math.abs(recieveNet) - Math.abs(retuernedNet));
        return Math.abs(recieveNet);
    }

    /**
     * Calculates the remaining free quantity of items from a purchase order.
     * Initial quantity is equal to the free quantity in the order. GRNs reduce
     * the remaining free quantity. Cancelled GRNs increase the remaining free
     * quantity. GRN Returns and GRN Return Cancellations are not considered.
     *
     * @param po The pharmaceutical bill item linked to the purchase order.
     * @return Remaining free quantity from the original order.
     */
    public double calculateRemainingFreeQtyFromOrder(PharmaceuticalBillItem po) {
        double billed = getTotalFreeQty(po.getBillItem(), BillType.PharmacyGrnBill, new BilledBill());
        double cancelled = getTotalFreeQty(po.getBillItem(), BillType.PharmacyGrnBill, new CancelledBill());
//      double returnedB = getReturnedTotalFreeQty(po.getBillItem(), BillType.PharmacyGrnReturn, new BilledBill());
//      double returnedC = getReturnedTotalFreeQty(po.getBillItem(), BillType.PharmacyGrnReturn, new CancelledBill());

        double recieveNet = Math.abs(billed) - Math.abs(cancelled);
//      double retuernedNet = Math.abs(returnedB) - Math.abs(returnedC);
//      returned values are not considered
        return (Math.abs(recieveNet));
    }

    @Deprecated // Use calculateRemainigQtyFromOrder
    public double calQtyInTwoSql(PharmaceuticalBillItem po) {
        double grns = getTotalQty(po.getBillItem(), BillType.PharmacyGrnBill);
        double grnReturn = getReturnedTotalQty(po.getBillItem(), BillType.PharmacyGrnReturn);
        return Math.abs(grns) - Math.abs(grnReturn);
    }

    @Deprecated // use calculateRemainingFreeQtyFromOrder
    public double calFreeQtyInTwoSql(PharmaceuticalBillItem po) {

        double grnsFree = getTotalFreeQty(po.getBillItem(), BillType.PharmacyGrnBill);
        double grnReturnFree = getReturnedTotalFreeQty(po.getBillItem(), BillType.PharmacyGrnReturn);

        return grnsFree - grnReturnFree;
    }

    public double calQty2(BillItem bil) {

        double returnBill = getTotalQty(bil, BillType.PharmacySale, new RefundBill());

        //System.err.println("RETURN " + returnBill);
        return bil.getQty() - returnBill;
    }

    public double calQty3(BillItem bil) {

        double returnBill = getTotalQty(bil, BillType.PharmacyPre, new RefundBill());

        return bil.getQty() - returnBill;
    }

    public double calQty4(BillItem bil) {

        double returnBill = getTotalQty(bil, BillType.PharmacyBhtPre, new RefundBill());

        return bil.getQty() - returnBill;
    }

//     public double calculateRemainigQtyFromOrder(PharmaceuticalBillItem po) {
//
//        double billed =getTotalQty(po.getBillItem(),BillType.PharmacyGrnBill,new BilledBill());
//        double cancelled = getTotalQty(po.getBillItem(),BillType.PharmacyGrnBill,new CancelledBill());;
//        double returned = getTotalQty(po.getBillItem(),BillType.PharmacyGrnReturn,new BilledBill());
//
//        //System.err.println("BILLED " + billed);
//        //System.err.println("Cancelled " + cancelled);
//        //System.err.println("Refunded " + returned);
//        //System.err.println("Cal Qty " + (billed - (cancelled + returned)));
//
//        return billed - (cancelled + returned);
//    }
//    public double checkQty(PharmaceuticalBillItem ph) {
//        if (ph.getQty() == 0.0) {
//            return 0.0;
//        }
//
//        double adjustedGrnQty;
//        String sql = "Select p from PharmaceuticalBillItem p where p.billItem.id=" + ph.getBillItem().getReferanceBillItem().getId();
//        PharmaceuticalBillItem po = getPharmaceuticalBillItemFacade().findFirstByJpql(sql);
//
//        Item poItem = po.getBillItem().getItem();
//        Item grnItem = ph.getBillItem().getItem();
//
//        if ((poItem instanceof Amp || poItem instanceof Vmp) && grnItem instanceof Amp) {
//            if (po.getQty() < ph.getQty()) {
//                return po.getQty();
//            } else {
//                return ph.getQty();
//            }
//        } else if ((poItem instanceof Ampp || poItem instanceof Vmpp) && grnItem instanceof Ampp) {
//            adjustedGrnQty = ph.getQty() * grnItem.getDblValue();
//            double adjustedPoQty = po.getQty() * poItem.getDblValue();
//            if (adjustedPoQty < adjustedGrnQty) {
//                return adjustedPoQty / grnItem.getDblValue();
//            } else {
//                return ph.getQty();
//            }
//
//        } else {
//            if ((poItem instanceof Amp || poItem instanceof Vmp) && grnItem instanceof Ampp) {
//                adjustedGrnQty = ph.getQty() * grnItem.getDblValue();
//                if (po.getQty() < adjustedGrnQty) {
//                    return po.getQty() / grnItem.getDblValue();
//                } else {
//                    return ph.getQty();
//                }
//
//            } else {
//                adjustedGrnQty = ph.getQty() / poItem.getDblValue();
//                if (po.getQty() < adjustedGrnQty) {
//                    return po.getQty() * poItem.getDblValue();
//                } else {
//                    return ph.getQty();
//                }
//            }
//        }
//    }
    public double getRemainingQty(PharmaceuticalBillItem ph) {

        String sql = "Select p from PharmaceuticalBillItem p where p.billItem.id = " + ph.getBillItem().getReferanceBillItem().getId();
        PharmaceuticalBillItem po = getPharmaceuticalBillItemFacade().findFirstByJpql(sql);

        //    Item poItem = po.getBillItem().getItem();
        //    Item grnItem = ph.getBillItem().getItem();
        double poQty, grnQty, remainsFree;
        poQty = po.getQtyInUnit();
        remainsFree = poQty - calculateRemainingFreeQtyFromOrder(po);

        return remainsFree;

    }

    public double getRemainingFreeQty(PharmaceuticalBillItem ph) {

        String sql = "Select p from PharmaceuticalBillItem p where p.billItem.id = " + ph.getBillItem().getReferanceBillItem().getId();
        PharmaceuticalBillItem po = getPharmaceuticalBillItemFacade().findFirstByJpql(sql);

        //    Item poItem = po.getBillItem().getItem();
        //    Item grnItem = ph.getBillItem().getItem();
        double poQty, grnQty, remains;
        poQty = po.getFreeQtyInUnit();
        remains = poQty - calculateRemainingFreeQtyFromOrder(po);

        return remains;

    }

    public boolean checkQty(PharmaceuticalBillItem ph) {

        String sql = "Select p from PharmaceuticalBillItem p where p.billItem.id=" + ph.getBillItem().getReferanceBillItem().getId();
        PharmaceuticalBillItem po = getPharmaceuticalBillItemFacade().findFirstByJpql(sql);

        //    Item poItem = po.getBillItem().getItem();
        //    Item grnItem = ph.getBillItem().getItem();
        double poQty, grnQty, remains;
        poQty = Math.abs(po.getQtyInUnit());
        remains = Math.abs(poQty) - calculateRemainigQtyFromOrder(po);
        grnQty = Math.abs(ph.getQtyInUnit());

        //System.err.println("poQty : " + poQty);
        //System.err.println("grnQty : " + grnQty);
        //System.err.println("remain : " + remains);
        return remains < grnQty;

    }

    public boolean checkPurchasePrice(PharmaceuticalBillItem i) {
        double oldPrice, newPrice = 0.0;

        String sql = "Select p from PharmaceuticalBillItem p where p.billItem.id=" + i.getBillItem().getReferanceBillItem().getId();
        PharmaceuticalBillItem tmp = getPharmaceuticalBillItemFacade().findFirstByJpql(sql);

        oldPrice = tmp.getPurchaseRate();
        newPrice = i.getPurchaseRate();

        double max = oldPrice + (oldPrice * (getPharmacyBean().getMaximumPurchasePriceChange() / 100.0));
        //System.err.println("Old Pur Price : " + oldPrice);
        //System.err.println("New Pur Price : " + newPrice);
        //System.err.println("MAX Price : " + max);

        return max < newPrice;
    }

//    public boolean checkRetailPrice(PharmaceuticalBillItem i) {
//        double oldPrice, newPrice = 0.0;
//
//        String sql = "Select p from PharmaceuticalBillItem p where p.billItem.id=" + i.getBillItem().getReferanceBillItem().getId();
//        PharmaceuticalBillItem tmp = getPharmaceuticalBillItemFacade().findFirstByJpql(sql);
//
//        oldPrice = tmp.getRetailRate();
//        newPrice = i.getRetailRate();
//
////        //System.err.println("Old Ret Price : " + oldPrice);
////        //System.err.println("New Ret Price : " + newPrice);
//        double max = oldPrice + (oldPrice * (getPharmacyBean().getMaximumRetailPriceChange() / 100));
//
//        //System.err.println("Old Ret Price : " + oldPrice);
//        //System.err.println("New Ret Price : " + newPrice);
//        //System.err.println("MAX Price : " + max);
//
//        if (max < newPrice) {
//            return true;
//        } else {
//            return false;
//        }
//    }
    public boolean checkRetailPrice(PharmaceuticalBillItem i) {

        double max = i.getPurchaseRate() + (i.getPurchaseRate() * (getPharmacyBean().getMaximumRetailPriceChange() / 100));

        //System.err.println("Purchase Price : " + i.getPurchaseRate());
        //System.err.println("Retail Price : " + i.getRetailRate());
        //System.err.println("MAX Price : " + max);
        return max < i.getRetailRate();
    }

    public ItemBatch saveItemBatch(BillItem tmp) {
        //System.err.println("Save Item Batch");
        ItemBatch itemBatch = new ItemBatch();
        Item itm = tmp.getItem();

        if (itm instanceof Ampp) {
            itm = ((Ampp) itm).getAmp();
        }

        double purchase = tmp.getPharmaceuticalBillItem().getPurchaseRateInUnit();
        double retail = tmp.getPharmaceuticalBillItem().getRetailRateInUnit();
        double wholesale = tmp.getPharmaceuticalBillItem().getWholesaleRate();
        ////// // System.out.println("wholesale = " + wholesale);
        itemBatch.setDateOfExpire(tmp.getPharmaceuticalBillItem().getDoe());
        itemBatch.setBatchNo(tmp.getPharmaceuticalBillItem().getStringValue());
        itemBatch.setPurcahseRate(purchase);
        itemBatch.setRetailsaleRate(retail);
        itemBatch.setWholesaleRate(wholesale);
        itemBatch.setLastPurchaseBillItem(tmp);
        HashMap hash = new HashMap();
        String sql;

        itemBatch.setItem(itm);
        sql = "Select p from ItemBatch p where  p.item=:itm "
                + " and p.dateOfExpire= :doe and p.retailsaleRate=:ret "
                + " and p.purcahseRate=:pur";

        hash.put("doe", itemBatch.getDateOfExpire());
        hash.put("itm", itemBatch.getItem());
        hash.put("ret", itemBatch.getRetailsaleRate());
        hash.put("pur", itemBatch.getPurcahseRate());
        List<ItemBatch> i = getItemBatchFacade().findByJpql(sql, hash, TemporalType.TIMESTAMP);
        //System.err.println("Size " + i.size());
        if (!i.isEmpty()) {
//            //System.err.println("Edit");
//            i.get(0).setBatchNo(i.get(0).getBatchNo());
//            i.get(0).setDateOfExpire(i.get(0).getDateOfExpire());
            itemBatch.setMake(tmp.getPharmaceuticalBillItem().getMake());
            itemBatch.setModal(tmp.getPharmaceuticalBillItem().getModel());
            ItemBatch ib = i.get(0);
            ib.setWholesaleRate(wholesale);
            getItemBatchFacade().edit(ib);
            return ib;
        } else {
            //System.err.println("Create");
            itemBatch.setMake(tmp.getPharmaceuticalBillItem().getMake());
            itemBatch.setModal(tmp.getPharmaceuticalBillItem().getModel());
            getItemBatchFacade().create(itemBatch);
        }

        //System.err.println("ItemBatc Id " + itemBatch.getId());
        return itemBatch;
    }

    private ItemBatch fetchItemBatchWithCosting(Item item, double purchaseRate, double retailRate, double costRate, Date dateOfExpiry) {
        String jpql = "SELECT p FROM ItemBatch p "
                + "WHERE p.retired = false "
                + "AND p.item = :itm "
                + "AND p.dateOfExpire = :doe "
                + "AND p.retailsaleRate = :ret "
                + "AND p.costRate = :cr "
                + "AND p.purcahseRate = :pur";

        Map<String, Object> params = new HashMap<>();
        params.put("itm", item);
        params.put("doe", dateOfExpiry);
        params.put("ret", retailRate);
        params.put("cr", costRate);
        params.put("pur", purchaseRate);

        return getItemBatchFacade().findFirstByJpql(jpql, params, TemporalType.DATE);
    }

    private ItemBatch fetchItemBatchWithoutCosting(Item item, double purchaseRate, double retailRate, Date dateOfExpiry) {
        String jpql = "SELECT p FROM ItemBatch p "
                + "WHERE p.retired = false "
                + "AND p.item = :itm "
                + "AND p.dateOfExpire = :doe "
                + "AND p.retailsaleRate = :ret "
                + "AND p.purcahseRate = :pur";

        Map<String, Object> params = new HashMap<>();
        params.put("itm", item);
        params.put("doe", dateOfExpiry);
        params.put("ret", retailRate);
        params.put("pur", purchaseRate);

        return getItemBatchFacade().findFirstByJpql(jpql, params, TemporalType.DATE);
    }

    /**
     * Creates or fetches an existing ItemBatch based on costing and expiry
     * logic.Ensures uniqueness based on AMP, purchaseRate, retailRate,
     * costRate, and expiry.Additional fields like wholesaleRate, make, etc.,
     * are set but not used for uniqueness.
     *
     * @param inputBillItem
     * @return
     */
    public ItemBatch saveItemBatchWithCosting(BillItem inputBillItem) {
        if (inputBillItem == null || inputBillItem.getItem() == null || inputBillItem.getPharmaceuticalBillItem() == null) {
            return null;
        }

        // Extract AMP (Actual Medicinal Product) even if input is AMPP (Pack)
        Item originalItem = inputBillItem.getItem();
        Item amp = originalItem;
        if (amp instanceof Ampp) {
            amp = ((Ampp) amp).getAmp();
        } else {
        }

        Date expiryDate = inputBillItem.getPharmaceuticalBillItem().getDoe();
        if (expiryDate == null || amp == null) {
            return null;
        }

        ItemBatch itemBatch = null;

        double purchaseRatePerUnit;
        double retailRatePerUnit;
        double wholesaleRate = 0.0;
        double costRatePerUnit = 0.0;

        boolean manageCosting = configOptionApplicationController.getBooleanValueByKey("Manage Costing", true);

        if (manageCosting) {
            // Use finance details when costing is enabled
            if (inputBillItem.getBillItemFinanceDetails() == null) {
                return null;
            }

            BigDecimal prGiven = inputBillItem.getBillItemFinanceDetails().getLineNetRate();

            BigDecimal unitsPerPack = inputBillItem.getBillItemFinanceDetails().getUnitsPerPack();
            if (unitsPerPack.compareTo(BigDecimal.ZERO) <= 0) {
                unitsPerPack = BigDecimal.ONE;
            }

            BigDecimal prPerUnit = prGiven.divide(
                    unitsPerPack,
                    PRICE_SCALE,
                    RoundingMode.HALF_EVEN
            );

            purchaseRatePerUnit = prPerUnit.doubleValue();
            retailRatePerUnit = inputBillItem.getBillItemFinanceDetails().getRetailSaleRatePerUnit().doubleValue();
            costRatePerUnit = inputBillItem.getBillItemFinanceDetails().getTotalCostRate().doubleValue();

            System.out.println("@@@ PharmacyCalculation.saveItemBatchWithCosting @@@");
            System.out.println("Item: " + (inputBillItem.getItem() != null ? inputBillItem.getItem().getName() : "null"));
            System.out.println("costRatePerUnit FROM BillItemFinanceDetails.getTotalCostRate(): " + costRatePerUnit);
            System.out.println("purchaseRatePerUnit: " + purchaseRatePerUnit);
            System.out.println("retailRatePerUnit: " + retailRatePerUnit);

            itemBatch = fetchItemBatchWithCosting(amp, purchaseRatePerUnit, retailRatePerUnit, costRatePerUnit, expiryDate);

            System.out.println("ItemBatch fetched/created with costRate: " + (itemBatch != null ? itemBatch.getCostRate() : "null"));
        } else {
            // Use values from PharmaceuticalBillItem when costing is not enabled
            purchaseRatePerUnit = inputBillItem.getPharmaceuticalBillItem().getPurchaseRate();
            retailRatePerUnit = inputBillItem.getPharmaceuticalBillItem().getRetailRateInUnit();
            wholesaleRate = inputBillItem.getPharmaceuticalBillItem().getWholesaleRate();

            itemBatch = fetchItemBatchWithoutCosting(amp, purchaseRatePerUnit, retailRatePerUnit, expiryDate);
        }

        // If no matching batch found, create a new one
        if (itemBatch == null) {
            itemBatch = new ItemBatch();
            itemBatch.setItem(amp);
            itemBatch.setDateOfExpire(expiryDate);
            itemBatch.setBatchNo(inputBillItem.getPharmaceuticalBillItem().getStringValue());
            itemBatch.setPurcahseRate(purchaseRatePerUnit);
            itemBatch.setRetailsaleRate(retailRatePerUnit);
            itemBatch.setWholesaleRate(wholesaleRate);
            itemBatch.setCostRate(costRatePerUnit);
            itemBatch.setLastPurchaseBillItem(inputBillItem);
            itemBatch.setMake(inputBillItem.getPharmaceuticalBillItem().getMake());
            itemBatch.setModal(inputBillItem.getPharmaceuticalBillItem().getModel());

            getItemBatchFacade().create(itemBatch);
        } else {
        }

        return itemBatch;
    }

    public List<Item> findItem(Amp tmp, List<Item> items) {

        String sql;

        sql = "SELECT i from Amp i where i.retired=false and "
                + "i.vmp.id in (select vv.vmp.id from VirtualProductIngredient vv where vv.vmp.id=" + tmp.getVmp().getId() + ")";
        items = getItemFacade().findByJpql(sql);

        sql = "SELECT i from Ampp i where i.retired=false and "
                + "i.amp.vmp.id in (select vv.vmp.id from VirtualProductIngredient vv where vv.vmp.id=" + tmp.getVmp().getId() + ")";
        List<Item> amppList = getItemFacade().findByJpql(sql);
        items.addAll(amppList);

        return items;
    }

    public List<Item> findPack(Amp amp) {

        String sql;
        HashMap hm = new HashMap();
        sql = "SELECT i from Ampp i where i.retired=false and "
                + " i.amp=:am";

        hm.put("am", amp);

        return getItemFacade().findByJpql(sql, hm);

    }

    public List<Item> findItem(Ampp tmp, List<Item> items) {

        String sql;
        sql = "SELECT i from Amp i where i.retired=false and "
                + "i.vmp.id in (select vv.vmp.id from VirtualProductIngredient vv where vv.vmp.id=" + tmp.getAmp().getVmp().getId() + ")";
        items = getItemFacade().findByJpql(sql);

        sql = "SELECT i from Ampp i where i.retired=false and "
                + "i.amp.vmp.id in (select vv.vmp.id from VirtualProductIngredient vv where vv.vmp.id=" + tmp.getAmp().getVmp().getId() + ")";
        List<Item> amppList = getItemFacade().findByJpql(sql);
        items.addAll(amppList);
        return items;
    }

    public List<Item> findItem(Vmp tmp, List<Item> items) {

        String sql;
        sql = "SELECT i from Amp i where i.retired=false and "
                + "i.vmp.id in (select vv.vmp.id from VirtualProductIngredient vv where vv.vmp.id=" + tmp.getId() + ")";
        items = getItemFacade().findByJpql(sql);

        sql = "SELECT i from Ampp i where i.retired=false and "
                + "i.amp.vmp.id in (select vv.vmp.id from VirtualProductIngredient vv where vv.vmp.id=" + tmp.getId() + ")";
        List<Item> amppList = getItemFacade().findByJpql(sql);
        items.addAll(amppList);
        return items;
    }

    public List<Item> findItem(Vmpp tmp, List<Item> items) {

        String sql;
        sql = "SELECT i from Amp i where i.retired=false and "
                + "i.vmp.id in (select vv.vmp.id from VirtualProductIngredient vv where vv.vmp.id=" + tmp.getVmp().getId() + ")";
        items = getItemFacade().findByJpql(sql);

        sql = "SELECT i from Ampp i where i.retired=false and "
                + "i.amp.vmp.id in (select vv.vmp.id from VirtualProductIngredient vv where vv.vmp.id=" + tmp.getVmp().getId() + ")";
        List<Item> amppList = getItemFacade().findByJpql(sql);
        items.addAll(amppList);
        return items;
    }

    public void editBillItem(PharmaceuticalBillItem i, WebUser w) {

        i.getBillItem().setNetValue(0 - (i.getQty() * i.getPurchaseRate()));

        i.getBillItem().setCreatedAt(Calendar.getInstance().getTime());
        i.getBillItem().setCreater(w);
        i.getBillItem().setPharmaceuticalBillItem(i);

        getBillItemFacade().edit(i.getBillItem());
//
//        double consumed = calculateRemainigQtyFromOrder(i.getBillItem().getReferanceBillItem().getPharmaceuticalBillItem());
//        i.getBillItem().setRemainingQty(i.getBillItem().getReferanceBillItem().getPharmaceuticalBillItem().getQty() - consumed);
//        getBillItemFacade().edit(i.getBillItem());
    }

//    public BillItem saveBillItem(Item i, Bill b, SessionController s) {
//        BillItem tmp = new BillItem();
//        tmp.setBill(b);
//        tmp.setItem(i);
//        //  tmp.setQty(getPharmacyBean().getOrderingQty(i, s.getDepartment()));
//        // tmp.setRate(getPharmacyBean().getPurchaseRate(i, s.getDepartment()));
//        // tmp.setNetValue(tmp.getQty() * tmp.getRate());
//
//        getBillItemFacade().create(tmp);
//
//        return tmp;
//    }
    public String errorCheck(Bill b, List<BillItem> billItems) {
        String msg = "";

        if (b.getInvoiceNumber() == null || b.getInvoiceNumber().trim().isEmpty()) {
            msg = "Please Fill invoice number";
        }

        if (b.getPaymentMethod() != null && b.getPaymentMethod() == PaymentMethod.Cheque) {
            if (b.getBank() == null || b.getBank().getId() == null || b.getChequeRefNo() == null) {
                msg = "Please select Cheque Number and Bank";
            }
        }

        if (b.getPaymentMethod() != null && b.getPaymentMethod() == PaymentMethod.Slip) {
            if (b.getBank() == null || b.getBank().getId() == null || b.getComments() == null) {
                msg = "Please Fill Memo and Bank";
            }
        }

        if (billItems.isEmpty()) {
            msg = "There is no Item to receive";
        }

        if (checkItemBatch(billItems)) {
            msg = "Please Fill Batch deatail and Sale Price to All Item";
        }

        if (b.getReferenceInstitution() == null) {
            msg = "Please Fill Reference Institution";
        }

        return msg;
    }

    public void calculateRetailSaleValueAndFreeValueAtPurchaseRate(Bill b) {
        double sale = 0.0;
        double free = 0.0;

        for (BillItem i : b.getBillItems()) {
            PharmaceuticalBillItem ph = i.getPharmaceuticalBillItem();
            if (ph == null) {
                continue;
            }
            sale += (ph.getQty() + ph.getFreeQty()) * ph.getRetailRate();
            free += ph.getFreeQty() * ph.getPurchaseRate();
        }
        if (b.getBillType() == BillType.PharmacyGrnReturn || b.getBillType() == BillType.PurchaseReturn || b.getClass().equals(CancelledBill.class) || b.getClass().equals(RefundBill.class)) {
            b.setSaleValue(0.0 - Math.abs(sale));
            b.setFreeValue(0.0 - Math.abs(free));
        } else {
            b.setSaleValue(Math.abs(sale));
            b.setFreeValue(Math.abs(free));
        }
    }

    public boolean checkItemBatch(List<BillItem> list) {

        for (BillItem i : list) {
            PharmaceuticalBillItem ph = i.getPharmaceuticalBillItem();
            if (ph == null) {
                continue;
            }
            if (ph.getQty() != 0.0) {
                if (ph.getDoe() == null || ph.getStringValue().trim().isEmpty()) {
                    return true;
                }
                if (ph.getPurchaseRate() > ph.getRetailRate()) {
                    return true;
                }

            }

        }
        return false;
    }

//    public void preCalForAddToStock(PharmacyItemData ph, ItemBatch itb, Department d) {
//
//        Item item = ph.getPharmaceuticalBillItem().getBillItem().getItem();
//        Double qty = 0.0;
//
//        if (item instanceof Amp) {
//            qty = ph.getPharmaceuticalBillItem().getQty() + ph.getPharmaceuticalBillItem().getFreeQty();
//
//        } else {
//            qty = (ph.getPharmaceuticalBillItem().getQty() + ph.getPharmaceuticalBillItem().getFreeQty()) * item.getDblValue();
//            //      //////// // System.out.println("sssssss " + qty);
//        }
//
//        if (itb.getId() != null) {
//
//        }
//    }
    public void updatePharmacyPurchaseGrnCancelRefundBilledBills() {
        BillType[] bts = {BillType.PharmacyGrnReturn, BillType.PharmacyGrnBill, BillType.PharmacyPurchaseBill, BillType.PurchaseReturn};
        List<BillType> billTypes = Arrays.asList(bts);

        String sql;
        Map m = new HashMap();

        sql = "select b from Bill b "
                + " where b.billType in :bts "
                + " and b.saleValue=:sv ";

        m.put("bts", billTypes);
        m.put("sv", 0.0);
        List<Bill> bills = getBillFacade().findByJpql(sql, m, 100);
        for (Bill b : bills) {
            calculateRetailSaleValueAndFreeValueAtPurchaseRate(b);
            getBillFacade().edit(b);
        }
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public AmpFacade getAmpFacade() {
        return ampFacade;
    }

    public void setAmpFacade(AmpFacade ampFacade) {
        this.ampFacade = ampFacade;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public ItemBatchFacade getItemBatchFacade() {
        return itemBatchFacade;
    }

    public void setItemBatchFacade(ItemBatchFacade itemBatchFacade) {
        this.itemBatchFacade = itemBatchFacade;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public ItemsDistributorsFacade getItemsDistributorsFacade() {
        return itemsDistributorsFacade;
    }

    public void setItemsDistributorsFacade(ItemsDistributorsFacade itemsDistributorsFacade) {
        this.itemsDistributorsFacade = itemsDistributorsFacade;
    }

    public CategoryFacade getCategoryFacade() {
        return categoryFacade;
    }

    public void setCategoryFacade(CategoryFacade categoryFacade) {
        this.categoryFacade = categoryFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    /**
     * Bulk calculation method to replace individual N+1 queries for bill item calculations.
     * Retrieves issued and cancelled quantities for all bill items in a single query.
     * 
     * @param billItems List of bill items to calculate for
     * @param billTypeAtomic The atomic bill type to filter by (e.g., PHARMACY_ISSUE)
     * @return Map with bill item ID as key and calculation DTO as value
     */
    public java.util.Map<Long, com.divudi.core.data.dto.BillItemCalculationDTO> getBulkCalculationsForBillItems(
            java.util.List<com.divudi.core.entity.BillItem> billItems, com.divudi.core.data.BillTypeAtomic billTypeAtomic) {
        
        if (billItems == null || billItems.isEmpty()) {
            return new java.util.HashMap<>();
        }
        
        java.util.List<Long> billItemIds = billItems.stream()
            .map(com.divudi.core.entity.BillItem::getId)
            .filter(id -> id != null)
            .collect(java.util.stream.Collectors.toList());
        
        if (billItemIds.isEmpty()) {
            return new java.util.HashMap<>();
        }
        
        try {
            
            // Step 1: Get issued quantities (BilledBill)
            String issuedSql = "SELECT bi.referanceBillItem.id, SUM(ABS(bi.qty)) " +
                             "FROM BillItem bi " +
                             "WHERE bi.referanceBillItem.id IN :billItemIds " +
                             "  AND bi.bill.billTypeAtomic = :billTypeAtomic " +
                             "  AND bi.retired = false " +
                             "  AND bi.creater IS NOT NULL " +
                             "GROUP BY bi.referanceBillItem.id";
            
            java.util.Map<String, Object> issuedParams = new java.util.HashMap<>();
            issuedParams.put("billItemIds", billItemIds);
            issuedParams.put("billTypeAtomic", billTypeAtomic);
            
            java.util.List<Object[]> issuedResults = getPharmaceuticalBillItemFacade().findObjectArrayByJpql(issuedSql, issuedParams, javax.persistence.TemporalType.TIMESTAMP);
            java.util.Map<Long, Double> issuedMap = new java.util.HashMap<>();
            
            for (Object[] row : issuedResults) {
                Long billItemId = ((Number) row[0]).longValue();
                Double issuedQty = ((Number) row[1]).doubleValue();
                issuedMap.put(billItemId, issuedQty);
            }
            
            // Step 2: Get cancelled quantities (CancelledBill) - cancellations of the issued bills
            String cancelledSql = "SELECT bi.referanceBillItem.referanceBillItem.id, SUM(ABS(bi.qty)) " +
                                "FROM BillItem bi " +
                                "WHERE bi.referanceBillItem.referanceBillItem.id IN :billItemIds " +
                                "  AND bi.bill.billTypeAtomic = :cancelledBillTypeAtomic " +
                                "  AND bi.retired = false " +
                                "  AND bi.creater IS NOT NULL " +
                                "GROUP BY bi.referanceBillItem.referanceBillItem.id";
            
            java.util.Map<String, Object> cancelledParams = new java.util.HashMap<>();
            cancelledParams.put("billItemIds", billItemIds);
            cancelledParams.put("cancelledBillTypeAtomic", com.divudi.core.data.BillTypeAtomic.PHARMACY_ISSUE_CANCELLED);
            
            java.util.List<Object[]> cancelledResults = getPharmaceuticalBillItemFacade().findObjectArrayByJpql(cancelledSql, cancelledParams, javax.persistence.TemporalType.TIMESTAMP);
            java.util.Map<Long, Double> cancelledMap = new java.util.HashMap<>();
            
            for (Object[] row : cancelledResults) {
                Long billItemId = ((Number) row[0]).longValue();
                Double cancelledQty = ((Number) row[1]).doubleValue();
                cancelledMap.put(billItemId, cancelledQty);
            }
            
            // Step 3: Get original quantities and combine results
            String originalSql = "SELECT bi.id, bi.qty " +
                               "FROM BillItem bi " +
                               "WHERE bi.id IN :billItemIds " +
                               "  AND bi.retired = false";
            
            java.util.Map<String, Object> originalParams = new java.util.HashMap<>();
            originalParams.put("billItemIds", billItemIds);
            
            java.util.List<Object[]> originalResults = getPharmaceuticalBillItemFacade().findObjectArrayByJpql(originalSql, originalParams, javax.persistence.TemporalType.TIMESTAMP);
            
            
            java.util.Map<Long, com.divudi.core.data.dto.BillItemCalculationDTO> resultMap = new java.util.HashMap<>();
            
            for (Object[] row : originalResults) {
                Long billItemId = ((Number) row[0]).longValue();
                Double originalQty = ((Number) row[1]).doubleValue();
                Double issuedQty = issuedMap.getOrDefault(billItemId, 0.0);
                Double cancelledQty = cancelledMap.getOrDefault(billItemId, 0.0);
                
                com.divudi.core.data.dto.BillItemCalculationDTO dto = new com.divudi.core.data.dto.BillItemCalculationDTO(
                    billItemId, originalQty, issuedQty, cancelledQty);
                    
                resultMap.put(billItemId, dto);
                
            }
            
            return resultMap;
            
        } catch (Exception e) {
// Log error and return empty map as fallback
                        e.printStackTrace();
            return new java.util.HashMap<>();
        }
    }
}
