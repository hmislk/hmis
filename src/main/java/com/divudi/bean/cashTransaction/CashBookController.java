/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.cashTransaction;

import com.divudi.bean.common.SessionController;
import com.divudi.entity.cashTransaction.CashBook;
import com.divudi.facade.CashBookFacade;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Lawan Chaamindu
 */
@Named
@SessionScoped
public class CashBookController implements Serializable {

    @EJB
    private CashBookFacade CashbookFacade;
    @EJB
    private CashBook cashBook;
    
    @Inject
    private SessionController sessionController;

    
    public CashBookController() {
    }

    public CashBookFacade getCashbookFacade() {
        return CashbookFacade;
    }

    public void setCashbookFacade(CashBookFacade CashbookFacade) {
        this.CashbookFacade = CashbookFacade;
    }

    public CashBook getCashBook() {
        return cashBook;
    }

    public void setCashBook(CashBook cashBook) {
        this.cashBook = cashBook;
    }

}
