/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

/**
 * @author Buddhika
 */
public enum CalculationType {

    Value,
    Constant,
    Addition,
    Substraction,
    Multiplication,
    Devision,
    Power,
    OpeningBracket,
    ClosingBracket,
    AgeInMonths,
    AgeInYears,
    AgeInDays,
    GenderDependentConstant,
    Comma,
    Space;

    public String getLabel() {
        switch (this) {
            case Addition:
                return "Addition";
            case AgeInDays:
                return "Age in days";
            case AgeInMonths:
                return "Age in months";
            case AgeInYears:
                return "Age in Years";
            case ClosingBracket:
                return "Closing Bracket";
            case Constant:
                return "Constant. (Gender non-specific)";
            case GenderDependentConstant:
                return "Gender specific constant";
            case Devision:
                return "Division";
            case Multiplication:
                return "Multiplication";
            case OpeningBracket:
                return "Opening Bracket";
            case Substraction:
                return "Substraction";
            case Power:
                return "Power";
            case Value:
                return "Value";
            case Comma:
                return "Comma";
            case Space:
                return "Sapce";
        }
        return "Other";
    }
}
