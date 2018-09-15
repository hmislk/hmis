/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.lab;

/**
 *
 * @author buddhika_ari
 */
public enum SampleRequestType {
    A,
    D;
    
    public String getLabel(){
        if(this==A){
            return "Accept";
        }else{
            return "Delete";
        }
    }
}
