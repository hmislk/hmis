/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.WebUser;
import java.util.List;

/**
 *
 * @author safrin
 */
public class WebUserBillsTotal {

    private WebUser webUser;
    private List<BillsTotals> billsTotals;

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    public List<BillsTotals> getBillsTotals() {
        return billsTotals;
    }

    public void setBillsTotals(List<BillsTotals> billsTotals) {
        this.billsTotals = billsTotals;
    }
}
