/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.accounts;

import com.divudi.data.AccountRow;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;

/**
 *
 * @author buddhika
 */
@Named(value = "commonAccountsController")
@ViewScoped
public class CommonAccountsController implements Serializable {

    @EJB
    BillFacade billFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    
    AccountRow accountRow;
    List<AccountRow> accountRows;
    
    /**
     * Creates a new instance of CommonAccountsController
     */
    public CommonAccountsController() {
    }
    
    public String viewGrandSummery(){
        
        return "/accounts/grand_summery";
    }
    
    
    
    
    
}
