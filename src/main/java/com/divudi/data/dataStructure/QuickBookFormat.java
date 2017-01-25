/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import java.util.Date;

/**
 *
 * @author archmage
 */
public class QuickBookFormat {
    private String rowType;
    private String trnsType;
    private String date;
    private String accnt;
    private String name;
    private String invItemType;
    private String invItem;
    private double amount;
    private String docNum;
    private String poNum;
    private String qbClass;
    private String memo;
    private String custFld1;
    private String custFld2;
    private String custFld3;
    
    private boolean editAccnt;
    private boolean editQbClass;

    public String getRowType() {
        return rowType;
    }

    public void setRowType(String rowType) {
        this.rowType = rowType;
    }

    public String getTrnsType() {
        return trnsType;
    }

    public void setTrnsType(String trnsType) {
        this.trnsType = trnsType;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getAccnt() {
        return accnt;
    }

    public void setAccnt(String accnt) {
        this.accnt = accnt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInvItemType() {
        return invItemType;
    }

    public void setInvItemType(String invItemType) {
        this.invItemType = invItemType;
    }

    public String getInvItem() {
        return invItem;
    }

    public void setInvItem(String invItem) {
        this.invItem = invItem;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDocNum() {
        return docNum;
    }

    public void setDocNum(String docNum) {
        this.docNum = docNum;
    }

    public String getPoNum() {
        return poNum;
    }

    public void setPoNum(String poNum) {
        this.poNum = poNum;
    }

    public String getQbClass() {
        return qbClass;
    }

    public void setQbClass(String qbClass) {
        this.qbClass = qbClass;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getCustFld1() {
        return custFld1;
    }

    public void setCustFld1(String custFld1) {
        this.custFld1 = custFld1;
    }

    public String getCustFld2() {
        return custFld2;
    }

    public void setCustFld2(String custFld2) {
        this.custFld2 = custFld2;
    }

    public String getCustFld3() {
        return custFld3;
    }

    public void setCustFld3(String custFld3) {
        this.custFld3 = custFld3;
    }

    public boolean isEditAccnt() {
        return editAccnt;
    }

    public void setEditAccnt(boolean editAccnt) {
        this.editAccnt = editAccnt;
    }

    public boolean isEditQbClass() {
        return editQbClass;
    }

    public void setEditQbClass(boolean editQbClass) {
        this.editQbClass = editQbClass;
    }
    
}
