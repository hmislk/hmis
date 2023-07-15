package com.divudi.bean.opd;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;

/**
 *
 * @author Dr. Buddhika Ariyaratne
 * 
 */
@Named
@SessionScoped
public class OpdController implements Serializable {


    private int opdSummaryIndex;
    
    public OpdController() {
    }

    public int getOpdSummaryIndex() {
        return opdSummaryIndex;
    }

    public void setOpdSummaryIndex(int opdSummaryIndex) {
        this.opdSummaryIndex = opdSummaryIndex;
    }
    
    
    
}
