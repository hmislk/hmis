/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.hr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author safrin
 */
public enum PaysheetComponentType {

    addition(null),
    PerformanceAllowancePercentage(null),
    BasicSalary(addition),
    FixedAllowance(addition),
    VariableAllowance(addition),
    Bonus(addition),
    OT(addition),
    @Deprecated
    ExtraDuty(addition),
    ExtraDutyNormal(addition),
    ExtraDutyMerchantile(addition),
    ExtraDutyPoya(addition),
    ExtraDutyDayOff(addition),
    ExtraDutySleepingDay(addition),
    PerformanceAllowance(addition),
    @Deprecated
    HolidayAllowance(addition),
    MerchantileAllowance(addition),
    PoyaAllowance(addition),
    DayOffAllowance(addition),
    SleepingDayAllowance(addition),
    AdjustmentBasicAdd(addition),
    AdjustmentAllowanceAdd(addition),
    //////////////////////
    subtraction(null),
    FixedDeduction(subtraction),
    VariableDeduction(subtraction),
    @Deprecated
    VariableDeductionToGrossSalary(subtraction),
    @Deprecated
    VariableDeductionToNetSalary(subtraction),
    @Deprecated
    VariableDeductionToBasicSalary(subtraction),
    LoanInstallemant(subtraction),
    LoanNetSalary(subtraction),
    Institution_Deduction(subtraction),
    Advance_Payment_Deduction(subtraction),
    Salary_Advance_Deduction(subtraction),
    @Deprecated
    No_Pay_Deduction(subtraction),
    No_Pay_Deduction_Basic(subtraction),
    No_Pay_Deduction_Allowance(subtraction),
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

    public List<PaysheetComponentType> getSystemDefinedComponents() {

        return Arrays.asList(new PaysheetComponentType[]{PaysheetComponentType.BasicSalary,
            PaysheetComponentType.PerformanceAllowance,
            PaysheetComponentType.DayOffAllowance,
            PaysheetComponentType.SleepingDayAllowance,
            PaysheetComponentType.ExtraDutyNormal,
            PaysheetComponentType.ExtraDutyMerchantile,
            PaysheetComponentType.ExtraDutyPoya,
            PaysheetComponentType.ExtraDutyDayOff,
            PaysheetComponentType.ExtraDutySleepingDay,
            PaysheetComponentType.No_Pay_Deduction_Basic,
            PaysheetComponentType.Salary_Advance_Deduction,
            PaysheetComponentType.No_Pay_Deduction_Allowance,
            PaysheetComponentType.OT,
            PaysheetComponentType.MerchantileAllowance,
            PaysheetComponentType.PoyaAllowance,
            PaysheetComponentType.AdjustmentAllowanceAdd,
            PaysheetComponentType.AdjustmentAllowanceSub,
            PaysheetComponentType.AdjustmentBasicAdd,
            PaysheetComponentType.AdjustmentBasicSub});

    }

    public List<PaysheetComponentType> getUserDefinedComponents() {

        return Arrays.asList(new PaysheetComponentType[]{PaysheetComponentType.Bonus,
            PaysheetComponentType.FixedAllowance,
            PaysheetComponentType.FixedDeduction,
            PaysheetComponentType.Institution_Deduction,
            PaysheetComponentType.Advance_Payment_Deduction,
            PaysheetComponentType.LoanInstallemant,
            PaysheetComponentType.LoanNetSalary,
            PaysheetComponentType.VariableAllowance,
            PaysheetComponentType.VariableDeduction});

    }

    public List<PaysheetComponentType> getUserDefinedComponentsAddidtions() {

        return Arrays.asList(new PaysheetComponentType[]{PaysheetComponentType.Bonus,
            PaysheetComponentType.FixedAllowance,
            PaysheetComponentType.VariableAllowance});

    }

    public List<PaysheetComponentType> getUserDefinedComponentsAddidtionsWithPerformance() {
        return Arrays.asList(new PaysheetComponentType[]{PaysheetComponentType.Bonus,
            PaysheetComponentType.FixedAllowance,
            PaysheetComponentType.VariableAllowance,
            PaysheetComponentType.PerformanceAllowance});

    }

    public List<PaysheetComponentType> getUserDefinedComponentsDeductions() {

        return Arrays.asList(new PaysheetComponentType[]{
            PaysheetComponentType.FixedDeduction,
            PaysheetComponentType.Institution_Deduction,
            PaysheetComponentType.Advance_Payment_Deduction,
            PaysheetComponentType.LoanInstallemant,
            PaysheetComponentType.LoanNetSalary,
            PaysheetComponentType.VariableDeduction});

    }

    public List<PaysheetComponentType> getUserDefinedComponentsDeductionsWithSalaryAdvance() {
        return Arrays.asList(new PaysheetComponentType[]{
            PaysheetComponentType.FixedDeduction,
            PaysheetComponentType.Institution_Deduction,
            PaysheetComponentType.Advance_Payment_Deduction,
            PaysheetComponentType.LoanInstallemant,
            PaysheetComponentType.LoanNetSalary,
            PaysheetComponentType.VariableDeduction,
            PaysheetComponentType.Salary_Advance_Deduction});

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
