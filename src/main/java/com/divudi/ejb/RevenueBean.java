package com.divudi.ejb;

import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author Buddhika
 */
@Stateless
public class RevenueBean {

    /**
     * EJBs
     */
    @EJB
    BillFacade billFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    BillFeeFacade billFeeFacade;

    /**
     * Bill Facade
     *
     * @return
     */
    public BillFacade getBillFacade() {
        return billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

}
