package com.divudi.core.data.dto;

import com.divudi.core.entity.Department;
import java.io.Serializable;

public class HospitalCensusSummaryDto implements Serializable {

    private String ward;
    private long totalBeds;
    private long openBeds;
    private long previousDaysTotal;
    private long newAdmissions;
    private long transferIn;
    private long transferOut;
    private long markedForDischarge;
    private long normalDischarges;
    private long lama;
    private long deaths;
    private long others;
    private long totalPresent;
    private double bedOccupancyRate;

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public long getTotalBeds() {
        return totalBeds;
    }

    public void setTotalBeds(long totalBeds) {
        this.totalBeds = totalBeds;
    }

    public long getOpenBeds() {
        return openBeds;
    }

    public void setOpenBeds(long openBeds) {
        this.openBeds = openBeds;
    }

    public long getPreviousDaysTotal() {
        return previousDaysTotal;
    }

    public void setPreviousDaysTotal(long previousDaysTotal) {
        this.previousDaysTotal = previousDaysTotal;
    }

    public long getNewAdmissions() {
        return newAdmissions;
    }

    public void setNewAdmissions(long newAdmissions) {
        this.newAdmissions = newAdmissions;
    }

    public long getTransferIn() {
        return transferIn;
    }

    public void setTransferIn(long transferIn) {
        this.transferIn = transferIn;
    }

    public long getTransferOut() {
        return transferOut;
    }

    public void setTransferOut(long transferOut) {
        this.transferOut = transferOut;
    }

    public long getMarkedForDischarge() {
        return markedForDischarge;
    }

    public void setMarkedForDischarge(long markedForDischarge) {
        this.markedForDischarge = markedForDischarge;
    }

    public long getNormalDischarges() {
        return normalDischarges;
    }

    public void setNormalDischarges(long normalDischarges) {
        this.normalDischarges = normalDischarges;
    }

    public long getLama() {
        return lama;
    }

    public void setLama(long lama) {
        this.lama = lama;
    }

    public long getDeaths() {
        return deaths;
    }

    public void setDeaths(long deaths) {
        this.deaths = deaths;
    }

    public long getOthers() {
        return others;
    }

    public void setOthers(long others) {
        this.others = others;
    }

    public long getTotalPresent() {
        return totalPresent;
    }

    public void setTotalPresent(long totalPresent) {
        this.totalPresent = totalPresent;
    }

    public double getBedOccupancyRate() {
        return bedOccupancyRate;
    }

    public void setBedOccupancyRate(double bedOccupancyRate) {
        this.bedOccupancyRate = bedOccupancyRate;
    }
}
