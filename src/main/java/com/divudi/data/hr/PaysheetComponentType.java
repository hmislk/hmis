/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.hr;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author safrin
 */
public enum PaysheetComponentType {

    addition(null),
    BasicSalary(addition),
    FixedAllowance(addition),
    VariableAllowance(addition),
    Bonus(addition),
    OT(addition),
    ExtraDuty(addition),
    PoyaAllowance(addition),
    DayOffAllowance(addition),
    AdjustmentBasicAdd(addition),
    AdjustmentAllowanceAdd(addition),
    //////////////////////
    subtraction(null),
    FixedDeduction(subtraction),
    VariableDeductionToGrossSalary(subtraction),
    VariableDeductionToNetSalary(subtraction),
    VariableDeductionToBasicSalary(subtraction),
    LoanInstallemant(subtraction),
    Institution_Deduction(subtraction),
    No_Pay_Deduction(subtraction),
    AdjustmentBasicSub(subtraction),
    AdjustmentAllowanceSub(subtraction),;

    public String getLabel() {
        switch (this) {
            case BasicSalary:
                return "Basic Salary ";
            case FixedAllowance:
                return "Fixed Allowance ";
            case VariableAllowance:
                return "Variable Allowance ";
            default:
                return this.toString();
        }
    }

    private PaysheetComponentType parent = null;
    private List<PaysheetComponentType> children = new ArrayList<>();

    private PaysheetComponentType(PaysheetComponentType parent) {
        this.parent = parent;
        if (this.parent != null) {
            this.parent.addChild(this);
        }
    }

    public PaysheetComponentType[] children() {
        return children.toArray(new PaysheetComponentType[children.size()]);
    }

    public PaysheetComponentType[] allChildren() {
        List<PaysheetComponentType> list = new ArrayList<>();
        addChildren(this, list);
        return list.toArray(new PaysheetComponentType[list.size()]);
    }

    private static void addChildren(PaysheetComponentType root, List<PaysheetComponentType> list) {
        list.addAll(root.children);
        for (PaysheetComponentType child : root.children) {
            addChildren(child, list);
        }
    }

    private void addChild(PaysheetComponentType child) {
        this.children.add(child);
    }

    public PaysheetComponentType getParent(PaysheetComponentType tmp) {
        return tmp.parent;
    }

    public boolean is(PaysheetComponentType other) {
        if (other == null) {
            return false;
        }

        for (PaysheetComponentType t = this; t != null; t = t.parent) {
            if (other == t) {
                return true;
            }
        }
        return false;
    }
}
