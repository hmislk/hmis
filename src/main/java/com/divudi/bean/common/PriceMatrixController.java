/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.data.PaymentMethod;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.entity.BillItem;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.PriceMatrix;
import com.divudi.entity.WebUser;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.entity.inward.InwardPriceAdjustment;
import com.divudi.entity.inward.RoomCategory;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.membership.ChannellingMemberShipDiscount;
import com.divudi.entity.membership.InwardMemberShipDiscount;
import com.divudi.entity.membership.MembershipScheme;
import com.divudi.entity.membership.OpdMemberShipDiscount;
import com.divudi.entity.membership.PaymentSchemeDiscount;
import com.divudi.entity.membership.PharmacyMemberShipDiscount;
import com.divudi.facade.PriceMatrixFacade;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author user
 */
@Named(value = "priceMatrixController")
@SessionScoped
public class PriceMatrixController implements Serializable {

    @EJB
    PriceMatrixFacade priceMatrixFacade;

    public PriceMatrix fetchInwardMargin(BillItem billItem, double serviceValue, Department department, PaymentMethod paymentMethod) {

        PriceMatrix inwardPriceAdjustment;
        Category category;
        if (billItem.getItem() instanceof Investigation) {
            category = ((Investigation) billItem.getItem()).getInvestigationCategory();
        } else {
            category = billItem.getItem().getCategory();
        }

        if (sessionController.getApplicationPreference() != null && sessionController.getApplicationPreference().isPaymentMethodAllowedInInwardMatrix()) {
            inwardPriceAdjustment = getInwardPriceAdjustment(department, serviceValue, category, paymentMethod);
        } else {
            inwardPriceAdjustment = getInwardPriceAdjustment(department, serviceValue, category);
        }

        if (inwardPriceAdjustment == null && category != null) {
            if (sessionController.getApplicationPreference().isPaymentMethodAllowedInInwardMatrix()) {
                inwardPriceAdjustment = getInwardPriceAdjustment(department, serviceValue, category.getParentCategory(), paymentMethod);
            } else {
                inwardPriceAdjustment = getInwardPriceAdjustment(department, serviceValue, category.getParentCategory());
            }
        }

        if (inwardPriceAdjustment == null) {
            return null;
        }

        return inwardPriceAdjustment;

    }

    public PriceMatrix fetchInwardMargin(Item item, double serviceValue, Department department) {

        PriceMatrix inwardPriceAdjustment;
        Category category;
        if (item instanceof Investigation) {
            category = ((Investigation) item).getInvestigationCategory();
        } else {
            category = item.getCategory();
        }

        inwardPriceAdjustment = getInwardPriceAdjustment(department, serviceValue, category);

        if (inwardPriceAdjustment == null && category != null) {
            inwardPriceAdjustment = getInwardPriceAdjustment(department, serviceValue, category.getParentCategory());
        }

        if (inwardPriceAdjustment == null) {
            return null;
        }

        return inwardPriceAdjustment;

    }

    public double getItemWithInwardMargin(Item item) {
        if (item == null) {
            return 0.0;
        }

        PriceMatrix inwardPriceAdjustment;

        Category category;
        if (item instanceof Investigation) {
            category = ((Investigation) item).getInvestigationCategory();
        } else {
            category = item.getCategory();
        }
        if (category == null) {
            return item.getTotal();
        }
        inwardPriceAdjustment = getInwardPriceAdjustment(item.getDepartment(), item.getTotal(), category);
        if (inwardPriceAdjustment == null) {
            inwardPriceAdjustment = getInwardPriceAdjustment(item.getDepartment(), item.getTotal(), category.getParentCategory());
        }
        if (inwardPriceAdjustment == null) {
            return item.getTotal();
        }
        return item.getTotal() * (inwardPriceAdjustment.getMargin() + 100) / 100;
    }

    @Inject
    SessionController sessionController;

    public InwardPriceAdjustment getInwardPriceAdjustment(Department department, double dbl, Category category) {
        String sql = "select a from InwardPriceAdjustment a "
                + " where a.retired=false"
                + " and a.category=:cat "
                + " and  a.department=:dep"
                + " and (a.fromPrice< :frPrice and a.toPrice >:tPrice)";
        HashMap hm = new HashMap();
        hm.put("dep", department);
        hm.put("frPrice", dbl);
        hm.put("tPrice", dbl);
        hm.put("cat", category);

        return (InwardPriceAdjustment) getPriceMatrixFacade().findFirstByJpql(sql, hm);
    }

    public InwardPriceAdjustment getInwardPriceAdjustment(Department department, double dbl, Category category, PaymentMethod paymentMethod) {
        String sql = "select a from InwardPriceAdjustment a "
                + " where a.retired=false"
                + " and a.category=:cat "
                + " and  a.department=:dep"
                + " and (a.fromPrice< :frPrice and a.toPrice >:tPrice)"
                + " and a.paymentMethod=:pm";

        HashMap hm = new HashMap();

        hm.put("pm", paymentMethod);
        hm.put("dep", department);
        hm.put("frPrice", dbl);
        hm.put("tPrice", dbl);
        hm.put("cat", category);

        return (InwardPriceAdjustment) getPriceMatrixFacade().findFirstByJpql(sql, hm);
    }

    public InwardMemberShipDiscount getInwardMemberDisCount(PaymentMethod paymentMethod, MembershipScheme membershipScheme, Institution ins, InwardChargeType inwardChargeType, AdmissionType admissionType) {
        String sql;
        HashMap hm = new HashMap();
        hm.put("p", paymentMethod);
        hm.put("admTp", admissionType);
        hm.put("inw", inwardChargeType);
        if (membershipScheme != null) {
            hm.put("m", membershipScheme);
            if (ins != null) {
                sql = "Select i from InwardMemberShipDiscount i"
                        + "  where i.retired=false "
                        + " and i.membershipScheme=:m "
                        + " and i.admissionType=:admTp "
                        + " and i.paymentMethod=:p "
                        + " and i.inwardChargeType=:inw"
                        + " and i.roomCategory is null "
                        + " and i.institution=:ins ";
                hm.put("ins", ins);
            } else {
                sql = "Select i from InwardMemberShipDiscount i "
                        + " where i.retired=false "
                        + " and  i.membershipScheme=:m "
                        + " and i.admissionType=:admTp "
                        + " and i.paymentMethod=:p "
                        + " and i.inwardChargeType=:inw "
                        + " and i.roomCategory is null "
                        + " and i.institution is null ";
            }
        } else {
            if (ins != null) {
                sql = "Select i from InwardMemberShipDiscount i "
                        + " where i.retired=false "
                        + " and i.paymentMethod=:p "
                        + " and i.admissionType=:admTp "
                        + " and i.inwardChargeType=:inw "
                        + " and i.institution=:ins"
                        + " and i.roomCategory is null "
                        + " and i.membershipScheme is null ";
                hm.put("ins", ins);
            } else {
                sql = "Select i from InwardMemberShipDiscount i "
                        + " where i.retired=false "
                        + " and i.paymentMethod=:p "
                        + " and i.admissionType=:admTp "
                        + " and i.inwardChargeType=:inw "
                        + " and i.membershipScheme is null"
                        + " and i.roomCategory is null "
                        + " and i.institution is null ";
            }
        }

        InwardMemberShipDiscount imsd = (InwardMemberShipDiscount) getPriceMatrixFacade().findFirstByJpql(sql, hm);

        if (imsd != null) {
        }

        return imsd;
    }

    public InwardMemberShipDiscount getInwardMemberDisCount(PaymentMethod paymentMethod, MembershipScheme membershipScheme, Institution ins, InwardChargeType inwardChargeType, AdmissionType admissionType, RoomCategory roomCategory) {
        String sql;
        HashMap hm = new HashMap();
        hm.put("p", paymentMethod);
        hm.put("admTp", admissionType);
        hm.put("inw", inwardChargeType);
        hm.put("rmCat", roomCategory);
        if (membershipScheme != null) {
            hm.put("m", membershipScheme);
            if (ins != null) {
                sql = "Select i from InwardMemberShipDiscount i"
                        + "  where i.retired=false "
                        + " and i.membershipScheme=:m "
                        + " and i.admissionType=:admTp "
                        + " and i.roomCategory=:rmCat "
                        + " and i.paymentMethod=:p "
                        + " and i.inwardChargeType=:inw"
                        + " and i.institution=:ins ";
                hm.put("ins", ins);
            } else {
                sql = "Select i from InwardMemberShipDiscount i "
                        + " where i.retired=false "
                        + " and  i.membershipScheme=:m "
                        + " and i.admissionType=:admTp "
                        + " and i.roomCategory=:rmCat "
                        + " and i.paymentMethod=:p "
                        + " and i.inwardChargeType=:inw "
                        + " and i.institution is null ";
            }
        } else {
            if (ins != null) {
                sql = "Select i from InwardMemberShipDiscount i "
                        + " where i.retired=false "
                        + " and i.paymentMethod=:p "
                        + " and i.admissionType=:admTp "
                        + " and i.roomCategory=:rmCat "
                        + " and i.inwardChargeType=:inw "
                        + " and i.institution=:ins"
                        + " and i.membershipScheme is null ";
                hm.put("ins", ins);
            } else {
                sql = "Select i from InwardMemberShipDiscount i "
                        + " where i.retired=false "
                        + " and i.paymentMethod=:p "
                        + " and i.admissionType=:admTp "
                        + " and i.roomCategory=:rmCat "
                        + " and i.inwardChargeType=:inw "
                        + " and i.membershipScheme is null"
                        + " and i.institution is null ";
            }
        }

        InwardMemberShipDiscount imsd = (InwardMemberShipDiscount) getPriceMatrixFacade().findFirstByJpql(sql, hm);

        if (imsd != null) {
        }

        return imsd;
    }

    public PriceMatrix getChannellingDisCount(PaymentMethod paymentMethod, MembershipScheme membershipScheme, Department department) {
        PriceMatrix channellingPriceMatrix = null;
        if (channellingPriceMatrix == null) {
            channellingPriceMatrix = fetchChannellingMemberShipDiscount(membershipScheme, paymentMethod, department);
        }

        return channellingPriceMatrix;
    }
    
    
    
    public OpdMemberShipDiscount getOpdMemberDisCount(PaymentMethod paymentMethod, MembershipScheme membershipScheme, Department department, Category category) {
        OpdMemberShipDiscount opdMemberShipDiscount = null;

        //Get Discount From Parent Category    
        if (opdMemberShipDiscount == null && category != null && category.getParentCategory() != null) {
            opdMemberShipDiscount = fetchOpdMemberShipDiscount(membershipScheme, paymentMethod, category.getParentCategory());
        }
        //Get Discount From Parent Category
        if (opdMemberShipDiscount == null && category != null) {
            opdMemberShipDiscount = fetchOpdMemberShipDiscount(membershipScheme, paymentMethod, category);
        }

        //Get Discount From Department
        if (opdMemberShipDiscount == null) {
            opdMemberShipDiscount = fetchOpdMemberShipDiscount(membershipScheme, paymentMethod, department);
        }


        return opdMemberShipDiscount;
    }
    
    
    
    public PharmacyMemberShipDiscount getPharmacyMemberDisCount(PaymentMethod paymentMethod, MembershipScheme membershipScheme, Department department, Category category) {
        //// // System.out.println("getPharmacyMemberDisCount");
        PharmacyMemberShipDiscount opdMemberShipDiscount = null;

        String jpql;
        HashMap hm = new HashMap();
        hm.put("p", paymentMethod);
        hm.put("m", membershipScheme);
        hm.put("cat", category);
        hm.put("dep", department);
        jpql = "Select i from PharmacyMemberShipDiscount i"
                + "  where i.retired=false "
                + " and i.membershipScheme=:m "
                + " and i.paymentMethod=:p"
                + " and i.category=:cat "
                + " and i.department=:dep ";
        return (PharmacyMemberShipDiscount) getPriceMatrixFacade().findFirstByJpql(jpql, hm);
    }
    
    public OpdMemberShipDiscount getOpdMemberDisCount(PaymentMethod paymentMethod, MembershipScheme membershipScheme, Department department) {
        OpdMemberShipDiscount opdMemberShipDiscount = null;
        //Get Discount From Parent Category
        //Get Discount From Parent Category
        //Get Discount From Parent Category    
        

        //Get Discount From Department
        if (opdMemberShipDiscount == null) {
            opdMemberShipDiscount = fetchOpdMemberShipDiscount(membershipScheme, paymentMethod, department);
        }


        return opdMemberShipDiscount;
    }

    public PaymentSchemeDiscount getPaymentSchemeDiscount(PaymentMethod paymentMethod, PaymentScheme paymentScheme, Department department, Item item) {
        PaymentSchemeDiscount paymentSchemeDiscount = null;
        Category category = null;

        if (item != null) {
            category = item.getCategory();
        }

        System.err.println(paymentMethod);
        System.err.println(paymentScheme);
        System.err.println(department);
        //Get Discount From Item        
        paymentSchemeDiscount = fetchPaymentSchemeDiscount(paymentScheme, paymentMethod, item);

        //Get Discount From Category        
        if (paymentSchemeDiscount == null) {
            paymentSchemeDiscount = fetchPaymentSchemeDiscount(paymentScheme, paymentMethod, category);
        }

        //Get Discount From Parent Category
        if (paymentSchemeDiscount == null && category != null) {
            paymentSchemeDiscount = fetchPaymentSchemeDiscount(paymentScheme, paymentMethod, category.getParentCategory());

        }

        //Get Discount From Department
        if (paymentSchemeDiscount == null) {
            paymentSchemeDiscount = fetchPaymentSchemeDiscount(paymentScheme, paymentMethod, department);
        }

        return paymentSchemeDiscount;
    }

    public PaymentSchemeDiscount getPaymentSchemeDiscount(PaymentMethod paymentMethod, Department department, Item item) {
        PaymentSchemeDiscount paymentSchemeDiscount;
        Category category = null;

        if (item != null) {
            category = item.getCategory();
        }
        // System.err.println(paymentScheme);
        // System.err.println(paymentScheme);


        //Get Discount From Item        
        paymentSchemeDiscount = fetchPaymentSchemeDiscount(paymentMethod, item);

        //Get Discount From Category        
        if (paymentSchemeDiscount == null) {
            paymentSchemeDiscount = fetchPaymentSchemeDiscount(paymentMethod, category);
        }

        //Get Discount From Parent Category
        if (paymentSchemeDiscount == null && category != null) {
            paymentSchemeDiscount = fetchPaymentSchemeDiscount(paymentMethod, category.getParentCategory());

        }

        //Get Discount From Department
        if (paymentSchemeDiscount == null) {
            paymentSchemeDiscount = fetchPaymentSchemeDiscount(paymentMethod, department);
        }

        return paymentSchemeDiscount;
    }

    public OpdMemberShipDiscount fetchOpdMemberShipDiscount(MembershipScheme membershipScheme, PaymentMethod paymentMethod, Category category) {
        String sql;
        HashMap hm = new HashMap();
        hm.put("p", paymentMethod);
        hm.put("m", membershipScheme);
        hm.put("cat", category);
        sql = "Select i from OpdMemberShipDiscount i"
                + "  where i.retired=false "
                + " and i.membershipScheme=:m "
                + " and i.paymentMethod=:p"
                + " and i.category=:cat ";

        return (OpdMemberShipDiscount) getPriceMatrixFacade().findFirstByJpql(sql, hm);

    }

    public PaymentSchemeDiscount fetchPaymentSchemeDiscount(PaymentScheme paymentScheme, PaymentMethod paymentMethod, Category category) {
        String sql;
        HashMap hm = new HashMap();
        hm.put("p", paymentMethod);
        hm.put("m", paymentScheme);
        hm.put("cat", category);
        sql = "Select i from PaymentSchemeDiscount i"
                + "  where i.retired=false "
                + " and i.paymentScheme=:m "
                + " and i.paymentMethod=:p"
                + " and i.category=:cat ";

        return (PaymentSchemeDiscount) getPriceMatrixFacade().findFirstByJpql(sql, hm);

    }

    public PaymentSchemeDiscount fetchPaymentSchemeDiscount(PaymentMethod paymentMethod, Category category) {
        String sql;
        HashMap hm = new HashMap();
        hm.put("p", paymentMethod);
        //  hm.put("m", paymentScheme);
        hm.put("cat", category);
        sql = "Select i from PaymentSchemeDiscount i"
                + "  where i.retired=false "
                + " and i.paymentScheme is null "
                + " and i.membershipScheme is null "
                + " and i.paymentMethod=:p"
                + " and i.category=:cat ";

        return (PaymentSchemeDiscount) getPriceMatrixFacade().findFirstByJpql(sql, hm);

    }

    public PaymentSchemeDiscount fetchPaymentSchemeDiscount(PaymentScheme paymentScheme, PaymentMethod paymentMethod, Item item) {
        System.out.println("fetchPaymentSchemeDiscount");
        String jpql;
        HashMap params = new HashMap();
        params.put("p", paymentMethod);
        params.put("m", paymentScheme);
        params.put("i", item);
        jpql = "Select i from PaymentSchemeDiscount i"
                + "  where i.retired=false "
                + " and i.paymentScheme=:m "
                + " and i.paymentMethod=:p"
                + " and i.item=:i ";
        PaymentSchemeDiscount psd=  (PaymentSchemeDiscount) getPriceMatrixFacade().findFirstByJpql(jpql, params);
        return psd;
    }

    public PaymentSchemeDiscount fetchPaymentSchemeDiscount(PaymentMethod paymentMethod, Item item) {
        String sql;
        HashMap hm = new HashMap();
        hm.put("p", paymentMethod);
        // hm.put("m", paymentScheme);
        hm.put("i", item);
        sql = "Select i from PaymentSchemeDiscount i"
                + "  where i.retired=false "
                + " and i.paymentScheme is null "
                + " and i.membershipScheme is null "
                + " and i.paymentMethod=:p"
                + " and i.item=:i ";

        return (PaymentSchemeDiscount) getPriceMatrixFacade().findFirstByJpql(sql, hm);

    }

    public OpdMemberShipDiscount fetchOpdMemberShipDiscount(MembershipScheme membershipScheme, PaymentMethod paymentMethod, Department department) {
        String sql;
        HashMap hm = new HashMap();
        hm.put("p", paymentMethod);
        hm.put("m", membershipScheme);
        hm.put("dep", department);
        sql = "Select i from OpdMemberShipDiscount i"
                + "  where i.retired=false "
                + " and i.membershipScheme=:m "
                + " and i.paymentMethod=:p"
                + " and i.department=:dep ";
        return (OpdMemberShipDiscount) getPriceMatrixFacade().findFirstByJpql(sql, hm);

    }
    
    
    public ChannellingMemberShipDiscount fetchChannellingMemberShipDiscount(MembershipScheme membershipScheme, PaymentMethod paymentMethod, Department department) {
        String sql;
        HashMap hm = new HashMap();
        hm.put("p", paymentMethod);
        hm.put("m", membershipScheme);
        hm.put("dep", department);
        sql = "Select i from ChannellingMemberShipDiscount i"
                + "  where i.retired=false "
                + " and i.membershipScheme=:m "
                + " and i.paymentMethod=:p"
                + " and i.department=:dep ";
        return (ChannellingMemberShipDiscount) getPriceMatrixFacade().findFirstByJpql(sql, hm);
    }

    public PaymentSchemeDiscount fetchPaymentSchemeDiscount(PaymentScheme paymentScheme, PaymentMethod paymentMethod, Department department) {
        String sql;
        HashMap hm = new HashMap();
        hm.put("p", paymentMethod);
        hm.put("m", paymentScheme);
        hm.put("dep", department);
        sql = "Select i from PaymentSchemeDiscount i"
                + "  where i.retired=false "
                + " and i.paymentScheme=:m "
                + " and i.paymentMethod=:p"
                + " and i.department=:dep ";

        return (PaymentSchemeDiscount) getPriceMatrixFacade().findFirstByJpql(sql, hm);

    }

    public PaymentSchemeDiscount fetchPaymentSchemeDiscount(PaymentScheme paymentScheme, PaymentMethod paymentMethod) {
        String sql;
        HashMap hm = new HashMap();
        hm.put("p", paymentMethod);
        hm.put("m", paymentScheme);
        sql = "Select i from PaymentSchemeDiscount i"
                + "  where i.retired=false "
                + " and i.paymentScheme=:m "
                + " and i.paymentMethod=:p"
                + " and i.department is null ";

        return (PaymentSchemeDiscount) getPriceMatrixFacade().findFirstByJpql(sql, hm);

    }

    public PaymentSchemeDiscount fetchPaymentSchemeDiscount(PaymentMethod paymentMethod, Department department) {
        String sql;
        HashMap hm = new HashMap();
        hm.put("p", paymentMethod);
        //    hm.put("m", paymentScheme);
        hm.put("dep", department);
        sql = "Select i from PaymentSchemeDiscount i"
                + "  where i.retired=false "
                + " and i.paymentScheme is null "
                + " and i.membershipScheme is null "
                + " and i.paymentMethod=:p"
                + " and i.department=:dep ";

        return (PaymentSchemeDiscount) getPriceMatrixFacade().findFirstByJpql(sql, hm);

    }

    public List<PriceMatrix> getInwardMemberShipDiscounts(PaymentMethod paymentMethod) {
        String sql = "select ipa from InwardMemberShipDiscount ipa "
                + " where ipa.retired=false"
                + " and ipa.paymentMethod=:pm"
                + " and ipa.membershipScheme is null"
                + " and ipa.institution is null"
                + " order by ipa.paymentMethod ";

        HashMap hm = new HashMap();
        hm.put("pm", paymentMethod);

        return getPriceMatrixFacade().findByJpql(sql, hm);
    }

    public List<PriceMatrix> getOpdMemberShipDiscountsDepartment(MembershipScheme membershipScheme) {
        String sql = "select ipa from OpdMemberShipDiscount ipa "
                + " where ipa.retired=false"
                + " and ipa.membershipScheme=:pm"
                + " and ipa.category is null"
                + " order by ipa.department.name ";

        HashMap hm = new HashMap();
        hm.put("pm", membershipScheme);

        return getPriceMatrixFacade().findByJpql(sql, hm);
    }

    public List<PriceMatrix> getOpdMemberShipDiscountsCategory(MembershipScheme membershipScheme) {
        String sql = "select ipa from OpdMemberShipDiscount ipa "
                + " where ipa.retired=false"
                + " and ipa.membershipScheme=:pm"
                + " and ipa.department is null"
                + " order by ipa.category.name ";

        HashMap hm = new HashMap();
        hm.put("pm", membershipScheme);

        return getPriceMatrixFacade().findByJpql(sql, hm);
    }

    public List<PriceMatrix> getInwardMemberShipDiscounts(Institution ins, PaymentMethod pay) {
        String sql = "select ipa from InwardMemberShipDiscount ipa "
                + " where ipa.retired=false"
                + " and ipa.paymentMethod=:pm"
                + " and ipa.institution=:ins"
                + " and ipa.membershipScheme is null";

        HashMap hm = new HashMap();
        hm.put("pm", pay);
        hm.put("ins", ins);

        return getPriceMatrixFacade().findByJpql(sql, hm);
    }

    public List<PriceMatrix> getInwardMemberShipDiscounts(MembershipScheme mem, PaymentMethod pay) {
        String sql = "select ipa from InwardMemberShipDiscount ipa "
                + " where ipa.retired=false"
                + " and ipa.paymentMethod=:pm"
                + " and ipa.membershipScheme=:mem"
                + " and ipa.institution is null";

        HashMap hm = new HashMap();
        hm.put("pm", pay);
        hm.put("mem", mem);

        return getPriceMatrixFacade().findByJpql(sql, hm);
    }

    public List<PriceMatrix> getInwardMemberShipDiscounts(Institution ins, MembershipScheme mem, PaymentMethod pay) {
        String sql = "select ipa from InwardMemberShipDiscount ipa "
                + " where ipa.retired=false"
                + " and ipa.paymentMethod=:pm"
                + " and ipa.membershipScheme=:mem"
                + " and ipa.institution=:ins";

        HashMap hm = new HashMap();
        hm.put("pm", pay);
        hm.put("mem", mem);
        hm.put("ins", ins);

        return getPriceMatrixFacade().findByJpql(sql, hm);
    }

    public InwardMemberShipDiscount getInwardMemberShipDiscount(MembershipScheme membershipScheme, Institution institution, PaymentMethod paymentMethod, InwardChargeType inwardChargeType, AdmissionType admissionType, WebUser webUser) {
        PriceMatrix object = getInwardMemberDisCount(paymentMethod, membershipScheme, institution, inwardChargeType, admissionType);

        if (object == null) {
            object = new InwardMemberShipDiscount();
            object.setCreatedAt(new Date());
            object.setCreater(webUser);
            object.setAdmissionType(admissionType);
            object.setInwardChargeType(inwardChargeType);
            object.setMembershipScheme(membershipScheme);
            object.setPaymentMethod(paymentMethod);
            object.setInstitution(institution);

            if (object.getId() == null) {
                getPriceMatrixFacade().create(object);
            }
        }

        return (InwardMemberShipDiscount) object;

    }

    public InwardMemberShipDiscount getInwardMemberShipDiscount(MembershipScheme membershipScheme, Institution institution, PaymentMethod paymentMethod, InwardChargeType inwardChargeType, AdmissionType admissionType, RoomCategory roomCategory, WebUser webUser) {
        PriceMatrix object = getInwardMemberDisCount(paymentMethod, membershipScheme, institution, inwardChargeType, admissionType, roomCategory);

        if (object == null) {
            object = new InwardMemberShipDiscount();
            object.setCreatedAt(new Date());
            object.setCreater(webUser);
            object.setRoomCategory(roomCategory);
            object.setAdmissionType(admissionType);
            object.setInwardChargeType(inwardChargeType);
            object.setMembershipScheme(membershipScheme);
            object.setPaymentMethod(paymentMethod);
            object.setInstitution(institution);

            if (object.getId() == null) {
                getPriceMatrixFacade().create(object);
            }
        }

        return (InwardMemberShipDiscount) object;

    }

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    public PriceMatrixFacade getPriceMatrixFacade() {
        return priceMatrixFacade;
    }

    public void setPriceMatrixFacade(PriceMatrixFacade priceMatrixFacade) {
        this.priceMatrixFacade = priceMatrixFacade;
    }

    /**
     * Creates a new instance of PriceMatrixController
     */
    public PriceMatrixController() {
    }

}
