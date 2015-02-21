/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.divudi.data.hr;

/**
 *
 * @author User
 */
public enum Month {
    January(0);

    private int monthValue;
    private Month() {
    }
    
    public int getMonthValue(){
        return monthValue;
    }
    
    private Month(int value) {
        this.monthValue=value;
    }
}
