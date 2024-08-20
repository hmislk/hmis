/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.dataStructure;

/**
 *
 * @author Dushan
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
    private String custFld4;
    private String custFld5;
    private String custFld6;

    private boolean editRowType = false;
    private boolean editTrnsType = false;
    private boolean editDate = false;
    private boolean editAccnt = false;
    private boolean editName = false;
    private boolean editInvItemType = false;
    private boolean editInvItem = false;
    private boolean editAmount = false;
    private boolean editDocNum = false;
    private boolean editPoNum = false;
    private boolean editQbClass = false;
    private boolean editMemo = false;
    private boolean editCustFld1 = false;
    private boolean editCustFld2 = false;
    private boolean editCustFld3 = false;
    private boolean editCustFld4 = false;
    private boolean editCustFld5 = false;
    private boolean editCustFld6 = false;

    public QuickBookFormat() {
    }

    public QuickBookFormat(String rowType, String trnsType, String date, String accnt, String name, String invItemType, String invItem, double amount, String docNum, String poNum, String qbClass, String memo, String custFld1, String custFld2, String custFld3, String custFld4, String custFld5) {
        this.rowType = rowType;
        this.trnsType = trnsType;
        this.date = date;
        this.accnt = accnt;
        this.name = name;
        this.invItemType = invItemType;
        this.invItem = invItem;
        this.amount = amount;
        this.docNum = docNum;
        this.poNum = poNum;
        this.qbClass = qbClass;
        this.memo = memo;
        this.custFld1 = custFld1;
        this.custFld2 = custFld2;
        this.custFld3 = custFld3;
        this.custFld4 = custFld4;
        this.custFld5 = custFld5;
    }

    public QuickBookFormat(String accnt, String name, String invItem, double amount, String qbClass) {
        this.rowType = "SPL";
        this.trnsType = "INVOICE";
        this.accnt = accnt;
        this.name = name;
        this.invItemType = "SERV";
        if (invItem.length() > 30) {
            this.invItem = invItem.substring(0, 30);
        } else {
            this.invItem = invItem;
        }
        this.amount = amount;
        this.qbClass = qbClass;
        this.memo = invItem;// invItem name limit for 30 characters
    }

    public QuickBookFormat(String accnt, String name, String invItem, double amount, String qbClass, String aa) {
        this.rowType = "SPL";
        this.trnsType = "INVOICE";
        this.accnt = accnt;
        this.name = name;
        this.invItemType = "SERV";
        if (invItem.length() > 30) {
            this.invItem = accnt + ":" + invItem.substring(0, 30);
        } else {
            this.invItem = accnt + ":" + invItem;
        }
        this.amount = amount;
        this.qbClass = qbClass;
        this.memo = invItem;// invItem name limit for 30 characters
    }

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

    public boolean isEditRowType() {
        return editRowType;
    }

    public void setEditRowType(boolean editRowType) {
        this.editRowType = editRowType;
    }

    public boolean isEditTrnsType() {
        return editTrnsType;
    }

    public void setEditTrnsType(boolean editTrnsType) {
        this.editTrnsType = editTrnsType;
    }

    public boolean isEditDate() {
        return editDate;
    }

    public void setEditDate(boolean editDate) {
        this.editDate = editDate;
    }

    public boolean isEditName() {
        return editName;
    }

    public void setEditName(boolean editName) {
        this.editName = editName;
    }

    public boolean isEditInvItemType() {
        return editInvItemType;
    }

    public void setEditInvItemType(boolean editInvItemType) {
        this.editInvItemType = editInvItemType;
    }

    public boolean isEditInvItem() {
        return editInvItem;
    }

    public void setEditInvItem(boolean editInvItem) {
        this.editInvItem = editInvItem;
    }

    public boolean isEditAmount() {
        return editAmount;
    }

    public void setEditAmount(boolean editAmount) {
        this.editAmount = editAmount;
    }

    public boolean isEditDocNum() {
        return editDocNum;
    }

    public void setEditDocNum(boolean editDocNum) {
        this.editDocNum = editDocNum;
    }

    public boolean isEditPoNum() {
        return editPoNum;
    }

    public void setEditPoNum(boolean editPoNum) {
        this.editPoNum = editPoNum;
    }

    public boolean isEditMemo() {
        return editMemo;
    }

    public void setEditMemo(boolean editMemo) {
        this.editMemo = editMemo;
    }

    public boolean isEditCustFld1() {
        return editCustFld1;
    }

    public void setEditCustFld1(boolean editCustFld1) {
        this.editCustFld1 = editCustFld1;
    }

    public boolean isEditCustFld2() {
        return editCustFld2;
    }

    public void setEditCustFld2(boolean editCustFld2) {
        this.editCustFld2 = editCustFld2;
    }

    public boolean isEditCustFld3() {
        return editCustFld3;
    }

    public void setEditCustFld3(boolean editCustFld3) {
        this.editCustFld3 = editCustFld3;
    }

    public boolean isEditCustFld4() {
        return editCustFld4;
    }

    public void setEditCustFld4(boolean editCustFld4) {
        this.editCustFld4 = editCustFld4;
    }

    public boolean isEditCustFld5() {
        return editCustFld5;
    }

    public void setEditCustFld5(boolean editCustFld5) {
        this.editCustFld5 = editCustFld5;
    }

    public String getCustFld4() {
        return custFld4;
    }

    public void setCustFld4(String custFld4) {
        this.custFld4 = custFld4;
    }

    public String getCustFld5() {
        return custFld5;
    }

    public void setCustFld5(String custFld5) {
        this.custFld5 = custFld5;
    }

    public String getCustFld6() {
        return custFld6;
    }

    public void setCustFld6(String custFld6) {
        this.custFld6 = custFld6;
    }

    public boolean isEditCustFld6() {
        return editCustFld6;
    }

    public void setEditCustFld6(boolean editCustFld6) {
        this.editCustFld6 = editCustFld6;
    }

}
