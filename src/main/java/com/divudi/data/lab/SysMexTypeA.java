package com.divudi.data.lab;

public class SysMexTypeA {

    private String inputString;
    private int lengthOfMessage = 298;
    private int sampleIdStart = 121;
    private int sampleIdEnd = 130;
    private int wbcStart = 66;
    private int wbcEnd = 72;
    private int rbcStart = 74;
    private int rbcEnd = 79;
    private int hgbStart = 81;
    private int hgbEnd = 85;
    private int hctStart = 87;
    private int hctEnd = 91;
    private int mcvStart = 93;
    private int mcvEnd = 97;
    private int mchStart = 99;
    private int mchEnd = 103;
    private int mchcStart = 105;
    private int mchcEnd = 109;
    private int pltStart = 125;
    private int pltEnd = 130;
    private int lymphPercentStart = 150;
    private int lymphPercentEnd = 155;
    private int monoPercentStart = 157;
    private int monoPercentEnd = 162;
    private int neutPercentStart = 164;
    private int neutPercentEnd = 169;
    private int eoPercentStart = 171;
    private int eoPercentEnd = 176;
    private int basoPercentStart = 178;
    private int basoPercentEnd = 183;

    private long sampleId;
    private double wbc;
    private double rbc;
    private double hgb;
    private double hct;
    private double mcv;
    private double mch;
    private double mchc;
    private double plt;
    private double lymphPercentage;
    private double monoPercentage;
    private double neutPercentage;
    private double eoPercentage;
    private double basoPercentage;

    public void populateValuesFromInputString() {
        if (inputString.length() == lengthOfMessage) {
            try {
                String sampleIdString = inputString.substring(sampleIdStart, sampleIdEnd).trim();
                sampleId = Long.parseLong(sampleIdString.replaceAll("\\s+", ""));
                wbc = Double.parseDouble(inputString.substring(wbcStart, wbcEnd));
                rbc = Double.parseDouble(inputString.substring(rbcStart, rbcEnd));
                hgb = Double.parseDouble(inputString.substring(hgbStart, hgbEnd));
                hct = Double.parseDouble(inputString.substring(hctStart, hctEnd));
                mcv = Double.parseDouble(inputString.substring(mcvStart, mcvEnd));
                mch = Double.parseDouble(inputString.substring(mchStart, mchEnd));
                mchc = Double.parseDouble(inputString.substring(mchcStart, mchcEnd));
                plt = Double.parseDouble(inputString.substring(pltStart, pltEnd));
                lymphPercentage = Double.parseDouble(inputString.substring(lymphPercentStart, lymphPercentEnd));
                monoPercentage = Double.parseDouble(inputString.substring(monoPercentStart, monoPercentEnd));
                neutPercentage = Double.parseDouble(inputString.substring(neutPercentStart, neutPercentEnd));
                eoPercentage = Double.parseDouble(inputString.substring(eoPercentStart, eoPercentEnd));
                basoPercentage = Double.parseDouble(inputString.substring(basoPercentStart, basoPercentEnd));
            } catch (NumberFormatException e) {
                // Error in parsing double values
                wbc = 0.0;
                rbc = 0.0;
                hgb = 0.0;
                hct = 0.0;
                mcv = 0.0;
                mch = 0.0;
                mchc = 0.0;
                plt = 0.0;
                lymphPercentage = 0.0;
                monoPercentage = 0.0;
                neutPercentage = 0.0;
                eoPercentage = 0.0;
                basoPercentage = 0.0;
            }
        }
    }

    public boolean isCorrectReport() {
        return (wbc != 0.0 && rbc != 0.0 && hgb != 0.0 && hct != 0.0 && mcv != 0.0 && mch != 0.0 && mchc != 0.0 &&
                plt != 0.0 && lymphPercentage != 0.0 && monoPercentage != 0.0 && neutPercentage != 0.0 &&
                eoPercentage != 0.0 && basoPercentage != 0.0);
    }

    public String getInputString() {
        return inputString;
    }

    public void setInputString(String inputString) {
        this.inputString = inputString;
    }

    public int getLengthOfMessage() {
        return lengthOfMessage;
    }

    public void setLengthOfMessage(int lengthOfMessage) {
        this.lengthOfMessage = lengthOfMessage;
    }

    public int getSampleIdStart() {
        return sampleIdStart;
    }

    public void setSampleIdStart(int sampleIdStart) {
        this.sampleIdStart = sampleIdStart;
    }

    public int getSampleIdEnd() {
        return sampleIdEnd;
    }

    public void setSampleIdEnd(int sampleIdEnd) {
        this.sampleIdEnd = sampleIdEnd;
    }

    public int getWbcStart() {
        return wbcStart;
    }

    public void setWbcStart(int wbcStart) {
        this.wbcStart = wbcStart;
    }

    public int getWbcEnd() {
        return wbcEnd;
    }

    public void setWbcEnd(int wbcEnd) {
        this.wbcEnd = wbcEnd;
    }

    public int getRbcStart() {
        return rbcStart;
    }

    public void setRbcStart(int rbcStart) {
        this.rbcStart = rbcStart;
    }

    public int getRbcEnd() {
        return rbcEnd;
    }

    public void setRbcEnd(int rbcEnd) {
        this.rbcEnd = rbcEnd;
    }

    public int getHgbStart() {
        return hgbStart;
    }

    public void setHgbStart(int hgbStart) {
        this.hgbStart = hgbStart;
    }

    public int getHgbEnd() {
        return hgbEnd;
    }

    public void setHgbEnd(int hgbEnd) {
        this.hgbEnd = hgbEnd;
    }

    public int getHctStart() {
        return hctStart;
    }

    public void setHctStart(int hctStart) {
        this.hctStart = hctStart;
    }

    public int getHctEnd() {
        return hctEnd;
    }

    public void setHctEnd(int hctEnd) {
        this.hctEnd = hctEnd;
    }

    public int getMcvStart() {
        return mcvStart;
    }

    public void setMcvStart(int mcvStart) {
        this.mcvStart = mcvStart;
    }

    public int getMcvEnd() {
        return mcvEnd;
    }

    public void setMcvEnd(int mcvEnd) {
        this.mcvEnd = mcvEnd;
    }

    public int getMchStart() {
        return mchStart;
    }

    public void setMchStart(int mchStart) {
        this.mchStart = mchStart;
    }

    public int getMchEnd() {
        return mchEnd;
    }

    public void setMchEnd(int mchEnd) {
        this.mchEnd = mchEnd;
    }

    public int getMchcStart() {
        return mchcStart;
    }

    public void setMchcStart(int mchcStart) {
        this.mchcStart = mchcStart;
    }

    public int getMchcEnd() {
        return mchcEnd;
    }

    public void setMchcEnd(int mchcEnd) {
        this.mchcEnd = mchcEnd;
    }

    public int getPltStart() {
        return pltStart;
    }

    public void setPltStart(int pltStart) {
        this.pltStart = pltStart;
    }

    public int getPltEnd() {
        return pltEnd;
    }

    public void setPltEnd(int pltEnd) {
        this.pltEnd = pltEnd;
    }

    public int getLymphPercentStart() {
        return lymphPercentStart;
    }

    public void setLymphPercentStart(int lymphPercentStart) {
        this.lymphPercentStart = lymphPercentStart;
    }

    public int getLymphPercentEnd() {
        return lymphPercentEnd;
    }

    public void setLymphPercentEnd(int lymphPercentEnd) {
        this.lymphPercentEnd = lymphPercentEnd;
    }

    public int getMonoPercentStart() {
        return monoPercentStart;
    }

    public void setMonoPercentStart(int monoPercentStart) {
        this.monoPercentStart = monoPercentStart;
    }

    public int getMonoPercentEnd() {
        return monoPercentEnd;
    }

    public void setMonoPercentEnd(int monoPercentEnd) {
        this.monoPercentEnd = monoPercentEnd;
    }

    public int getNeutPercentStart() {
        return neutPercentStart;
    }

    public void setNeutPercentStart(int neutPercentStart) {
        this.neutPercentStart = neutPercentStart;
    }

    public int getNeutPercentEnd() {
        return neutPercentEnd;
    }

    public void setNeutPercentEnd(int neutPercentEnd) {
        this.neutPercentEnd = neutPercentEnd;
    }

    public int getEoPercentStart() {
        return eoPercentStart;
    }

    public void setEoPercentStart(int eoPercentStart) {
        this.eoPercentStart = eoPercentStart;
    }

    public int getEoPercentEnd() {
        return eoPercentEnd;
    }

    public void setEoPercentEnd(int eoPercentEnd) {
        this.eoPercentEnd = eoPercentEnd;
    }

    public int getBasoPercentStart() {
        return basoPercentStart;
    }

    public void setBasoPercentStart(int basoPercentStart) {
        this.basoPercentStart = basoPercentStart;
    }

    public int getBasoPercentEnd() {
        return basoPercentEnd;
    }

    public void setBasoPercentEnd(int basoPercentEnd) {
        this.basoPercentEnd = basoPercentEnd;
    }

    public long getSampleId() {
        return sampleId;
    }

    public void setSampleId(long sampleId) {
        this.sampleId = sampleId;
    }

    public double getWbc() {
        return wbc;
    }

    public void setWbc(double wbc) {
        this.wbc = wbc;
    }

    public double getRbc() {
        return rbc;
    }

    public void setRbc(double rbc) {
        this.rbc = rbc;
    }

    public double getHgb() {
        return hgb;
    }

    public void setHgb(double hgb) {
        this.hgb = hgb;
    }

    public double getHct() {
        return hct;
    }

    public void setHct(double hct) {
        this.hct = hct;
    }

    public double getMcv() {
        return mcv;
    }

    public void setMcv(double mcv) {
        this.mcv = mcv;
    }

    public double getMch() {
        return mch;
    }

    public void setMch(double mch) {
        this.mch = mch;
    }

    public double getMchc() {
        return mchc;
    }

    public void setMchc(double mchc) {
        this.mchc = mchc;
    }

    public double getPlt() {
        return plt;
    }

    public void setPlt(double plt) {
        this.plt = plt;
    }

    public double getLymphPercentage() {
        return lymphPercentage;
    }

    public void setLymphPercentage(double lymphPercentage) {
        this.lymphPercentage = lymphPercentage;
    }

    public double getMonoPercentage() {
        return monoPercentage;
    }

    public void setMonoPercentage(double monoPercentage) {
        this.monoPercentage = monoPercentage;
    }

    public double getNeutPercentage() {
        return neutPercentage;
    }

    public void setNeutPercentage(double neutPercentage) {
        this.neutPercentage = neutPercentage;
    }

    public double getEoPercentage() {
        return eoPercentage;
    }

    public void setEoPercentage(double eoPercentage) {
        this.eoPercentage = eoPercentage;
    }

    public double getBasoPercentage() {
        return basoPercentage;
    }

    public void setBasoPercentage(double basoPercentage) {
        this.basoPercentage = basoPercentage;
    }

    
}
