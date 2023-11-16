/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.lab;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * @author Dr. M H B Ariyaratne <buddhika.ari@gmail.com>
 */
public class SysMexAdf2 {

    private String inputStringBytesSpaceSeperated;
    private String inputStringBytesPlusSeperated;
    private String inputStringCharactors;
    private List<Byte> bytes;
   
    private int lengthOfMessage = 255;
    private int instrumentIdStart = 4;
    private int instrumentIdEnd = 19;
    private int sampleIdStart = 33;
    private int sampleIdEnd = 47;
    private int wbcStart = 48;
    private int wbcEnd = 53;
    private int rbcStart = 54;
    private int rbcEnd = 58;
    private int hgbStart = 59;
    private int hgbEnd = 63;
    private int hctStart = 64;
    private int hctEnd = 68;
    private int mcvStart = 69;
    private int mcvEnd = 73;
    private int mchStart = 74;
    private int mchEnd = 78;
    private int mchcStart = 79;
    private int mchcEnd = 83;
    private int pltStart = 84;
    private int pltEnd = 88;
    private int lymphPercentStart = 89;
    private int lymphPercentEnd = 93;
    private int monoPercentStart = 94;
    private int monoPercentEnd = 98;
    private int neuPercentStart = 99;
    private int neuPercentEnd = 103;
    private int eoPercentStart = 104;
    private int eoPercentEnd = 108;
    private int basoPercentStart = 109;
    private int basoPercentEnd = 113;
    private int instrumentId2Start = 114;
    private int instrumentId2End = 119;

    private long sampleId;
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

    public boolean isCorrectReport() {
        boolean flag = true;
        if (bytes == null || bytes.isEmpty()) {
            return false;
        }
        if (bytes.size() < 300) {
            return false;
        }
        Double id1 = findValue(sampleIdStart, sampleIdEnd, 0);

        Double thb = findValue(hgbStart, hgbEnd, 2);
        if (thb < 2 || thb > 20) {
            return false;
        }

        Double tpcv = findValue(hctStart, hctEnd, 2);
        if (tpcv < 5 || tpcv > 60) {
            return false;
        }

        Double twbc = findValue(wbcStart, wbcEnd, 0);
        if (twbc < 1000 || twbc > 50000) {
            return false;
        }
        return true;
    }

    
    private void textToByteArraySeperatedByPlus() {
        bytes = new ArrayList<>();
        String strInput = inputStringBytesPlusSeperated;
        String[] strByte = strInput.split(Pattern.quote("+"));
        for (String s : strByte) {
            try {
                Byte b = Byte.parseByte(s);
                bytes.add(b);
            } catch (Exception e) {
//                //System.out.println("e = " + e);
                bytes.add(null);
            }
        }
    }

    private void textToByteArraySeperatedBySpace() {
        bytes = new ArrayList<>();
        String strInput = inputStringBytesSpaceSeperated;
        String[] strByte = strInput.split("\\s+");
        for (String s : strByte) {
            try {
                Byte b = Byte.parseByte(s);
                bytes.add(b);
            } catch (Exception e) {
//                //System.out.println("e = " + e);
                bytes.add(null);
            }
        }
    }

    private void textToByteArrayByCharactors() {
        bytes = new ArrayList<>();
        String strInput = inputStringCharactors;
        char[] strByte = strInput.toCharArray();
        for (char s : strByte) {
            try {
                Byte b = (byte) s;
                bytes.add(b);
            } catch (Exception e) {
//                //System.out.println("e = " + e);
                bytes.add(null);
            }
        }
    }

    public String addDecimalSeperator(String val) {
        String formatString = "#,###";
        Double dbl = Double.parseDouble(val);
        DecimalFormat formatter = new DecimalFormat(formatString);
        return formatter.format(dbl);
    }

    private String round(double value, int places) {
        String returnVal = "";
        if (places == 0) {
            returnVal = ((long) value) + "";
        } else if (places < 0) {
            long tn = (long) value;
            long pow = (long) Math.pow(10, Math.abs(places));
            tn = (tn / pow) * pow;
            returnVal = tn + "";
        } else {
            BigDecimal bd = new BigDecimal(value);
            bd = bd.setScale(places, RoundingMode.HALF_UP);
            returnVal = bd.doubleValue() + "";
        }
        return returnVal;
    }

    private Double findValue(int from, int to, int decimals) {
        Double val = null;

        String display = "";
        for (int i = from; i < to + 1; i++) {
            //System.out.println("i = " + i);
            int temN;
            try {
                temN = bytes.get(i);
            } catch (Exception e) {
                temN = 0;
            }

            display += (char) temN + "";
        }

        if (decimals
                > 0) {
            String wn = display.substring(0, display.length() - decimals);
            String fn = display.substring(display.length() - decimals, display.length());
            display = wn + "." + fn;
            try {
                val = Double.parseDouble(display);
            } catch (Exception e) {
                val = null;
            }
        } else if (decimals
                > 0) {
            try {
                val = Double.parseDouble(display);
            } catch (Exception e) {
                val = null;
            }
        } else {
            try {
                val = Double.parseDouble(display);
                val = val * Math.pow(10, Math.abs(decimals));
            } catch (Exception e) {
                val = null;
            }
        }
        return val;
    }

    private String findStringValue(int from, int to) {
        String display = "";
        for (int i = from; i < to + 1; i++) {
//            //System.out.println("i = " + i);
            int temN = bytes.get(i);
            display += (char) temN + "";
        }
        return display;
    }

    public int getLengthOfMessage() {
        return lengthOfMessage;
    }

    public void setLengthOfMessage(int lengthOfMessage) {
        this.lengthOfMessage = lengthOfMessage;
    }

    public int getInstrumentIdStart() {
        return instrumentIdStart;
    }

    public void setInstrumentIdStart(int instrumentIdStart) {
        this.instrumentIdStart = instrumentIdStart;
    }

    public int getInstrumentIdEnd() {
        return instrumentIdEnd;
    }

    public void setInstrumentIdEnd(int instrumentIdEnd) {
        this.instrumentIdEnd = instrumentIdEnd;
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

    public int getNeuPercentStart() {
        return neuPercentStart;
    }

    public void setNeuPercentStart(int neuPercentStart) {
        this.neuPercentStart = neuPercentStart;
    }

    public int getNeuPercentEnd() {
        return neuPercentEnd;
    }

    public void setNeuPercentEnd(int neuPercentEnd) {
        this.neuPercentEnd = neuPercentEnd;
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

    public String getInputStringBytesSpaceSeperated() {
        return inputStringBytesSpaceSeperated;
    }

    public void setInputStringBytesSpaceSeperated(String inputStringBytesSpaceSeperated) {
        this.inputStringBytesSpaceSeperated = inputStringBytesSpaceSeperated;
        textToByteArraySeperatedBySpace();
    }

    public List<Byte> getBytes() {
        return bytes;
    }

    public void setBytes(List<Byte> bytes) {
        this.bytes = bytes;
    }

    public long getSampleId() {
        try {
            sampleId = (findValue(sampleIdStart, sampleIdEnd, 0)).longValue();
        } catch (Exception e) {
            sampleId = 0;
        }
        return sampleId;
    }

    public String getSampleIdString() {
        return findStringValue(sampleIdStart, sampleIdEnd);
    }

    public void setSampleId(long sampleId) {
        this.sampleId = sampleId;
    }

    public String getWbc() {
        double w = findValue(wbcStart, wbcEnd, 0);
        String ws = findStringValue(wbcStart, wbcEnd);
        wbc = round(w, -2);
        wbc = addDecimalSeperator(wbc);
        return wbc;
    }

    public void setWbc(String wbc) {
        this.wbc = wbc;
    }

    public String getRbc() {
        double r = findValue(rbcStart, rbcEnd, 3);
        rbc = round(r, 2);
        return rbc;
    }

    public void setRbc(String rbc) {
        this.rbc = rbc;
    }

    public String getHgb() {
        double h = findValue(hgbStart, hgbEnd, 2);
        hgb = round(h, 1);
        return hgb;
    }

    public void setHgb(String hgb) {
        this.hgb = hgb;
    }

    public String getHct() {
        double h = findValue(hctStart, hctEnd, 2);
        hct = round(h, 1);
        return hct;
    }

    public void setHct(String hct) {
        this.hct = hct;
    }

    public String getMcv() {
        double m = findValue(mcvStart, mcvEnd, 2);
        mcv = round(m, 1);
        return mcv;
    }

    public void setMcv(String mcv) {
        this.mcv = mcv;
    }

    public String getMch() {
        double m = findValue(mchStart, mchEnd, 2);
        mch = round(m, 1);
        return mch;
    }

    public void setMch(String mch) {
        this.mch = mch;
    }

    public String getMchc() {
        double m = findValue(mchcStart, mchcEnd, 2);
        mchc = round(m, 1);
        return mchc;
    }

    public void setMchc(String mchc) {
        this.mchc = mchc;
    }

    public String getPlt() {
        double p = findValue(pltStart, pltEnd, -2);
        plt = round(p, -3);
        plt = addDecimalSeperator(plt);
        return plt;
    }

    public void setPlt(String plt) {
        this.plt = plt;
    }

    public String getLymphPercentage() {
        double l = findValue(lymphPercentStart, lymphPercentEnd, 2);
        lymphPercentage = round(l, 0);
        return lymphPercentage;
    }

    public void setLymphPercentage(String lymphPercentage) {
        this.lymphPercentage = lymphPercentage;
    }

    public String getMonoPercentage() {
        double m = findValue(monoPercentStart, monoPercentEnd, 2);
        monoPercentage = round(m, 0);
        return monoPercentage;
    }

    public void setMonoPercentage(String monoPercentage) {
        this.monoPercentage = monoPercentage;
    }

    public String getNeutPercentage() {
        double n = findValue(neuPercentStart, neuPercentEnd, 2);
        neutPercentage = round(n, 0);
        return neutPercentage;
    }

    public void setNeutPercentage(String neutPercentage) {
        this.neutPercentage = neutPercentage;
    }

    public String getEoPercentage() {
        double e = findValue(eoPercentStart, eoPercentEnd, 2);
        eoPercentage = round(e, 0);
        return eoPercentage;
    }

    public void setEoPercentage(String eoPercentage) {
        this.eoPercentage = eoPercentage;
    }

    public String getBasoPercentage() {
        double b = findValue(basoPercentStart, basoPercentEnd, 2);
        basoPercentage = round(b, 0);
        return basoPercentage;
    }

    public void setBasoPercentage(String basoPercentage) {
        this.basoPercentage = basoPercentage;
    }

    public String getInputStringBytesPlusSeperated() {
        return inputStringBytesPlusSeperated;
    }

    public void setInputStringBytesPlusSeperated(String inputStringBytesPlusSeperated) {
        this.inputStringBytesPlusSeperated = inputStringBytesPlusSeperated;
        textToByteArraySeperatedByPlus();
    }

    public String getInputStringCharactors() {
        return inputStringCharactors;
    }

    public void setInputStringCharactors(String inputStringCharactors) {
        this.inputStringCharactors = inputStringCharactors;
        textToByteArrayByCharactors();
    }

   
    public int getInstrumentId2Start() {
        return instrumentId2Start;
    }

    public void setInstrumentId2Start(int instrumentId2Start) {
        this.instrumentId2Start = instrumentId2Start;
    }

    public int getInstrumentId2End() {
        return instrumentId2End;
    }

    public void setInstrumentId2End(int instrumentId2End) {
        this.instrumentId2End = instrumentId2End;
    }

}
