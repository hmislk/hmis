package com.divudi.data.lab;

@Deprecated
public class SysMexTypeA {

    int startNum = 1;
    private String inputString;
    private int lengthOfMessage = 253;
    private int sampleIdStart = startNum + 30;
    private int sampleIdEnd = sampleIdStart + 15;
    private int wbcStart = sampleIdEnd + 0;
    private int wbcEnd = wbcStart + 6;
    private int rbcStart = wbcEnd + 0;
    private int rbcEnd = rbcStart + 5;
    private int hgbStart = rbcEnd + 0;
    private int hgbEnd = hgbStart + 5;
    private int hctStart = hgbEnd + 0;
    private int hctEnd = hctStart + 5;
    private int mcvStart = hctEnd + 0;
    private int mcvEnd = mcvStart + 5;
    private int mchStart = mcvEnd + 0;
    private int mchEnd = mchStart + 5;
    private int mchcStart = mchEnd + 0;
    private int mchcEnd = mchcStart + 5;
    private int pltStart = mchcEnd + 0;
    private int pltEnd = pltStart + 5;
    private int lymphPercentStart = pltEnd + 0;
    private int lymphPercentEnd = lymphPercentStart + 5;
    private int monoPercentStart = lymphPercentEnd + 0;
    private int monoPercentEnd = monoPercentStart + 5;
    private int neutPercentStart = monoPercentEnd + 0;
    private int neutPercentEnd = neutPercentStart + 5;
    private int eoPercentStart = neutPercentEnd + 0;
    private int eoPercentEnd = eoPercentStart + 5;
    private int basoPercentStart = eoPercentEnd + 0;
    private int basoPercentEnd = basoPercentStart + 5;

    double wbcDiv = 1000;
    double rbcDiv = 1000;
    double hgbDiv = 100;
    double hctDiv = 100;
    double mcvDiv = 100;
    double mchDiv = 100;
    double mchcDiv = 100;
    double pltDiv = 0.01;
    double lymphPercentageDiv = 100;
    double monoPercentageDiv = 100;
    double neutPercentageDiv = 100;
    double eoPercentageDiv = 100;
    double basoPercentageDiv = 100;

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

        try {
            String sampleIdString = inputString.substring(sampleIdStart, sampleIdEnd).trim();
            sampleId = Long.parseLong(sampleIdString.replaceAll("\\s+", ""));
            
            
            String webStr = inputString.substring(wbcStart, wbcEnd);
            wbc = Double.parseDouble(webStr) / wbcDiv;

            String rbcStr = inputString.substring(rbcStart, rbcEnd);
            rbc = Double.parseDouble(rbcStr) / rbcDiv;

            String hgbStr = inputString.substring(hgbStart, hgbEnd);
            hgb = Double.parseDouble(hgbStr) / hgbDiv;

            String hctStr = inputString.substring(hctStart, hctEnd);
            hct = Double.parseDouble(hctStr) / hctDiv;

            String mcvStr = inputString.substring(mcvStart, mcvEnd);
            mcv = Double.parseDouble(mcvStr) / mcvDiv;

            String mchStr = inputString.substring(mchStart, mchEnd);
            mch = Double.parseDouble(mchStr) / mchDiv;

            String mchcStr = inputString.substring(mchcStart, mchcEnd);
            mchc = Double.parseDouble(mchcStr) / mchcDiv;

            String pltStr = inputString.substring(pltStart, pltEnd);
            plt = Double.parseDouble(pltStr) / pltDiv;

            String lymphPercentageStr = inputString.substring(lymphPercentStart, lymphPercentEnd);
            lymphPercentage = Double.parseDouble(lymphPercentageStr) / lymphPercentageDiv;

            String monoPercentageStr = inputString.substring(monoPercentStart, monoPercentEnd);
            monoPercentage = Double.parseDouble(monoPercentageStr) / monoPercentageDiv;

            String neutPercentageStr = inputString.substring(neutPercentStart, neutPercentEnd);
            neutPercentage = Double.parseDouble(neutPercentageStr) / neutPercentageDiv;

            String eoPercentageStr = inputString.substring(eoPercentStart, eoPercentEnd);
            eoPercentage = Double.parseDouble(eoPercentageStr) / eoPercentageDiv;

            String basoPercentageStr = inputString.substring(basoPercentStart, basoPercentEnd);
            basoPercentage = Double.parseDouble(basoPercentageStr) / basoPercentageDiv;

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

    public boolean isCorrectReport() {
        return (wbc != 0.0 && rbc != 0.0 && hgb != 0.0 && hct != 0.0 && mcv != 0.0 && mch != 0.0 && mchc != 0.0
                && plt != 0.0 && lymphPercentage != 0.0 && monoPercentage != 0.0 && neutPercentage != 0.0
                && eoPercentage != 0.0 && basoPercentage != 0.0);
    }

    public String getInputString() {
        return inputString;
    }

    public void setInputString(String inputString) {
        this.inputString = inputString;
        populateValuesFromInputString();
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
