/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
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
