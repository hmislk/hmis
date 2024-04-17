/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.entity.BillItem;
import com.divudi.entity.WebUser;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.entity.pharmacy.UserStock;
import com.divudi.entity.pharmacy.UserStockContainer;
import com.divudi.facade.StockFacade;
import com.divudi.facade.UserStockContainerFacade;
import com.divudi.facade.UserStockFacade;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class UserStockController implements Serializable {

    @EJB
    UserStockFacade userStockFacade;
    @EJB
    UserStockContainerFacade userStockContainerFacade;
    @EJB
    StockFacade stockFacade;
    
    
     //check Is there any other user added same stock & exceedintg qty than need for current user
    //ONLY CHECK Within 30 min transaction
    //Checked
    public boolean isStockAvailable(Stock stock, double qty, WebUser webUser) {
        Stock fetchedStock = getStockFacade().find(stock.getId());

        if (qty > fetchedStock.getStock()) {
            return false;
        }

        String sql = "Select sum(us.updationQty) "
                + " from UserStock us "
                + " where us.retired=false "
                + " and us.userStockContainer.retired=false "
                + " and us.stock=:stk "
//                + " and us.creater!=:wb "
                + " and us.createdAt between :frm and :to ";

        Calendar cal = Calendar.getInstance();
        Date toTime = cal.getTime();
        cal.add(Calendar.MINUTE, -30);
        Date fromTime = cal.getTime();

        HashMap hm = new HashMap();
        hm.put("stk", stock);
//        hm.put("wb", webUser);
        hm.put("to", toTime);
        hm.put("frm", fromTime);

        double updatableQty1 = getUserStockFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

        double netUpdate = updatableQty1 + qty;

//        System.err.println("2 Qty " + qty);
//        System.err.println("3 Stock " + fetchedStock.getStock());
//        System.err.println("4 Net Update Qty " + netUpdate);
        if (netUpdate > fetchedStock.getStock()) {
            return false;
        } else {
            return true;
        }
    }
    
       public void retireUserStock(UserStockContainer userStockContainer, WebUser webUser) {

        if (userStockContainer.getUserStocks().size() == 0) {
            return;
        }

//        if (bill.getDepartment().getId() != department.getId()) {
//            return "Sorry You cant add Another Department Stock";
//        }
        userStockContainer.setRetiredAt(new Date());
        userStockContainer.setRetirer(webUser);
        userStockContainer.setRetireComments("New Bill Cliked");
        userStockContainer.setRetired(true);
        getUserStockContainerFacade().edit(userStockContainer);

        for (UserStock bItem : userStockContainer.getUserStocks()) {

            bItem.setRetired(true);
            bItem.setRetiredAt(new Date());
            bItem.setRetirer(webUser);
            bItem.setRetireComments("New Bill Cliked");

            getUserStockFacade().edit(bItem);

        }

    }


    public UserStockContainer saveUserStockContainer(UserStockContainer userStockContainer, WebUser webUser) {
        if (userStockContainer.getId() == null) {
            retiredAllUserStockContainer(webUser);
            userStockContainer.setCreater(webUser);
            userStockContainer.setCreatedAt(new Date());

            getUserStockContainerFacade().create(userStockContainer);
        }

        return userStockContainer;

    }

    public UserStock saveUserStock(BillItem tbi, WebUser webUser, UserStockContainer userStockContainer) {
        UserStock us = new UserStock();
        us.setStock(tbi.getPharmaceuticalBillItem().getStock());
        us.setUpdationQty(tbi.getQty());
        us.setCreater(webUser);
        us.setCreatedAt(new Date());
        us.setUserStockContainer(userStockContainer);
        //   ////// // System.out.println("2");
        if (us.getId() == null) {
            getUserStockFacade().create(us);
        } else {
            getUserStockFacade().edit(us);
        }
        userStockContainer.getUserStocks().add(us);

        return us;
    }

    public void removeUserStock(UserStock userStock, WebUser webUser) {
        if (userStock.isRetired()) {
            return;
        }

        userStock.setRetired(true);
        userStock.setRetiredAt(new Date());
        userStock.setRetireComments("Remove From Bill ");
        userStock.setRetirer(webUser);
        getUserStockFacade().edit(userStock);

    }

   
    
    public void updateUserStock(UserStock userStock, double qty) {
        if (userStock == null) {
            return;
        }
        userStock.setUpdationQty(qty);
        getUserStockFacade().edit(userStock);
    }

    public void retiredAllUserStockContainer(WebUser webUser) {
        String sql = "Select us from UserStockContainer us "
                + " where us.retired=false"
                + " and us.creater=:wb ";

        HashMap hm = new HashMap();
        hm.put("wb", webUser);

        List<UserStockContainer> usList = getUserStockContainerFacade().findByJpql(sql, hm);

        for (UserStockContainer usc : usList) {
            usc.setRetiredAt(new Date());
            usc.setRetirer(webUser);
            usc.setRetireComments("Menu Access");
            usc.setRetired(true);
            getUserStockContainerFacade().edit(usc);

            for (UserStock bItem : usc.getUserStocks()) {

                bItem.setRetired(true);
                bItem.setRetiredAt(new Date());
                bItem.setRetirer(webUser);
                bItem.setRetireComments("Menu Access");

                getUserStockFacade().edit(bItem);

            }

        }

    }

    public UserStockFacade getUserStockFacade() {
        return userStockFacade;
    }

    public void setUserStockFacade(UserStockFacade userStockFacade) {
        this.userStockFacade = userStockFacade;
    }

    public UserStockContainerFacade getUserStockContainerFacade() {
        return userStockContainerFacade;
    }

    public void setUserStockContainerFacade(UserStockContainerFacade userStockContainerFacade) {
        this.userStockContainerFacade = userStockContainerFacade;
    }

    public StockFacade getStockFacade() {
        return stockFacade;
    }

    public void setStockFacade(StockFacade stockFacade) {
        this.stockFacade = stockFacade;
    }

    
    
    /**
     * Creates a new instance of PharmacySessionScoped
     */
    public UserStockController() {
    }

}
