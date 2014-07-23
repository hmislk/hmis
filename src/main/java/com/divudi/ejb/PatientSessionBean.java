/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.ejb;

import com.divudi.data.SessionNumberType;
import com.divudi.entity.Item;
import java.util.Date;
import javax.ejb.Stateless;

/**
 *
 * @author Buddhika
 */
@Stateless
public class PatientSessionBean {

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    
    public int sessionNumber(Date sessionDate, Item item){
        if(item.getSessionNumberType()== SessionNumberType.ByCategory ){
            
        }else if(item.getSessionNumberType()== SessionNumberType.BySubCategory){
            
        }else if(item.getSessionNumberType()== SessionNumberType.ByItem){
            
        }else if(item.getSessionNumberType()== SessionNumberType.ByDoctor){
            
        }
        return 1;
    }

}
