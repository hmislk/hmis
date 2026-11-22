package com.divudi.core.data.dto;

import com.divudi.service.CreditCompanyClaimReportMath.AgeBucket;
import java.io.Serializable;

/**
 * One credit company on the Claim Ageing report (#25847): outstanding by age band.
 */
public class CreditCompanyClaimAgeingRowDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long companyId;
    private final String companyName;
    private double days0To30;
    private double days31To60;
    private double days61To90;
    private double over90;
    private int claimCount;

    public CreditCompanyClaimAgeingRowDto(Long companyId, String companyName) {
        this.companyId = companyId;
        this.companyName = companyName;
    }

    public void add(AgeBucket bucket, double amount) {
        claimCount++;
        switch (bucket) {
            case DAYS_0_30:
                days0To30 += amount;
                break;
            case DAYS_31_60:
                days31To60 += amount;
                break;
            case DAYS_61_90:
                days61To90 += amount;
                break;
            default:
                over90 += amount;
                break;
        }
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public double getDays0To30() {
        return days0To30;
    }

    public double getDays31To60() {
        return days31To60;
    }

    public double getDays61To90() {
        return days61To90;
    }

    public double getOver90() {
        return over90;
    }

    public int getClaimCount() {
        return claimCount;
    }

    public double getTotal() {
        return days0To30 + days31To60 + days61To90 + over90;
    }
}
