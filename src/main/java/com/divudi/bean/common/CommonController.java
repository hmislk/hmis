/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.divudi.bean.common;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author buddhika
 */
@Named(value = "commonController")
@SessionScoped
public class CommonController implements Serializable {

    /**
     * Creates a new instance of CommonController
     */
    public CommonController() {
    }
    
    public Date getCurrentDateTime(){
        return new Date();
    }
    
}
