/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.lab;

/**
 *
 * @author Dr. M H B Ariyaratne <buddhika.ari@gmail.com>
 */
public class SysMex {

    private String sampleId;
    private String wbc;
    private String rbc;
    private String hgb;
    private String hct;
    private String mcv;
    private String mch;
    private String mchc;
    private String plt;
    private String lymphPercentage;
    private String monoPercentage;
    private String neutPercentage;
    private String eoPercentage;
    private String basoPercentage;
    private Long sampleIdLong;
    // Additional variables
    private String neutAbsolute;
    private String lymphAbsolute;
    private String monoAbsolute;
    private String eoAbsolute;
    private String basoAbsolute;
    private String rdwSd;
    private String rdwCv;
    private String pdw;
    private String mpv;
    private String pLcr;
    private String pct;

    //getters/ setts and constructors here
    public SysMex() {
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getWbc() {
        return wbc;
    }

    public void setWbc(String wbc) {
        this.wbc = wbc;
    }

    public String getRbc() {
        return rbc;
    }

    public void setRbc(String rbc) {
        this.rbc = rbc;
    }

    public String getHgb() {
        return hgb;
    }

    public void setHgb(String hgb) {
        this.hgb = hgb;
    }

    public String getHct() {
        return hct;
    }

    public void setHct(String hct) {
        this.hct = hct;
    }

    public String getMcv() {
        return mcv;
    }

    public void setMcv(String mcv) {
        this.mcv = mcv;
    }

    public String getMch() {
        return mch;
    }

    public void setMch(String mch) {
        this.mch = mch;
    }

    public String getMchc() {
        return mchc;
    }

    public void setMchc(String mchc) {
        this.mchc = mchc;
    }

    public String getPlt() {
        return plt;
    }

    public void setPlt(String plt) {
        this.plt = plt;
    }

    public String getLymphPercentage() {
        return lymphPercentage;
    }

    public void setLymphPercentage(String lymphPercentage) {
        this.lymphPercentage = lymphPercentage;
    }

    public String getMonoPercentage() {
        return monoPercentage;
    }

    public void setMonoPercentage(String monoPercentage) {
        this.monoPercentage = monoPercentage;
    }

    public String getNeutPercentage() {
        return neutPercentage;
    }

    public void setNeutPercentage(String neutPercentage) {
        this.neutPercentage = neutPercentage;
    }

    public String getEoPercentage() {
        return eoPercentage;
    }

    public void setEoPercentage(String eoPercentage) {
        this.eoPercentage = eoPercentage;
    }

    public String getBasoPercentage() {
        return basoPercentage;
    }

    public void setBasoPercentage(String basoPercentage) {
        this.basoPercentage = basoPercentage;
    }

    public Long getSampleIdLong() {
        try {
            sampleIdLong = Long.valueOf(sampleId);
        } catch (Exception e) {
            sampleIdLong = 0l;
        }
        return sampleIdLong;
    }

    public void setSampleIdLong(Long sampleIdLong) {
        this.sampleIdLong = sampleIdLong;
    }

    public String getNeutAbsolute() {
        return neutAbsolute;
    }

    public void setNeutAbsolute(String neutAbsolute) {
        this.neutAbsolute = neutAbsolute;
    }

    public String getLymphAbsolute() {
        return lymphAbsolute;
    }

    public void setLymphAbsolute(String lymphAbsolute) {
        this.lymphAbsolute = lymphAbsolute;
    }

    public String getMonoAbsolute() {
        return monoAbsolute;
    }

    public void setMonoAbsolute(String monoAbsolute) {
        this.monoAbsolute = monoAbsolute;
    }

    public String getEoAbsolute() {
        return eoAbsolute;
    }

    public void setEoAbsolute(String eoAbsolute) {
        this.eoAbsolute = eoAbsolute;
    }

    public String getBasoAbsolute() {
        return basoAbsolute;
    }

    public void setBasoAbsolute(String basoAbsolute) {
        this.basoAbsolute = basoAbsolute;
    }

    public String getRdwSd() {
        return rdwSd;
    }

    public void setRdwSd(String rdwSd) {
        this.rdwSd = rdwSd;
    }

    public String getRdwCv() {
        return rdwCv;
    }

    public void setRdwCv(String rdwCv) {
        this.rdwCv = rdwCv;
    }

    public String getPdw() {
        return pdw;
    }

    public void setPdw(String pdw) {
        this.pdw = pdw;
    }

    public String getMpv() {
        return mpv;
    }

    public void setMpv(String mpv) {
        this.mpv = mpv;
    }

    public String getpLcr() {
        return pLcr;
    }

    public void setpLcr(String pLcr) {
        this.pLcr = pLcr;
    }

    public String getPct() {
        return pct;
    }

    public void setPct(String pct) {
        this.pct = pct;
    }
}
