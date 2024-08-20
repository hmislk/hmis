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
import java.util.Objects;
import java.util.regex.Pattern;

/**
 *
 * @author Dr. M H B Ariyaratne <buddhika.ari@gmail.com>
 */
public class SysMexOld {

    private String inputStringBytesSpaceSeperated;
    private String inputStringBytesPlusSeperated;
    private String inputStringCharactors;
    private List<Byte> bytes;
    private int shift1 = 0;
    private int shift2 = -1;
    private int lengthOfMessage = 444;
    private int instrumentId1Start = 4;
    private int instrumentId1End = 19;
    private int sampleId1Start = 33;
    private int sampleId1End = 47;
    private int year1Start = 48;
    private int yearEnd = 51;
    private int month1Start = 52;
    private int month1End = 53;
    private int date1Start = 54;
    private int date1End = 55;
    private int hour1Start = 56;
    private int hour1End = 57;
    private int min1Start = 58;
    private int min1End = 59;
    private int sampleId2Start = 222;
    private int sampleId2End = 236;
    private int wbcStart = 237;
    private int wbcEnd = 242;
    private int rbcStart = 243;
    private int rbcEnd = 247;
    private int hgbStart = 248;
    private int hgbEnd = 252;
    private int hctStart = 253;
    private int hctEnd = 257;
    private int mcvStart = 258;
    private int mcvEnd = 262;
    private int mchStart = 263;
    private int mchEnd = 267;
    private int mchcStart = 268;
    private int mchcEnd = 272;
    private int pltStart = 273;
    private int pltEnd = 277;
    private int lymphPercentStart = 278;
    private int lymphPercentEnd = 282;
    private int monoPercentStart = 283;
    private int monoPercentEnd = 287;
    private int neuPercentStart = 288;
    private int neuPercentEnd = 292;
    private int eoPercentStart = 293;
    private int eoPercentEnd = 297;
    private int basoPercentStart = 298;
    private int basoPercentEnd = 302;
    private int instrumentId2Start = 421;
    private int instrumentId2End = 436;

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
        //System.out.println("Checking wether the report is Correct");
        boolean flag = true;
        if (bytes == null || bytes.isEmpty()) {
            return false;
        }
        if (bytes.size() < 300) {
            return false;
        }
        Double id1 = findValue(sampleId1Start, sampleId1End, 0);
        //System.out.println("id1 = " + id1);
        Double id2 = findValue(sampleId2Start, sampleId2End, 0);
        //System.out.println("id2 = " + id2);
        if (!Objects.equals(id1, id2)) {
            return false;
        }
        //System.out.println("ID check OK");
        String insId1 = findStringValue(instrumentId1Start, instrumentId1End);
        insId1 = insId1.replaceAll("\\s", "");
        insId1 = insId1.substring(0, 5);
        //System.out.println("instrument Id1 = " + insId1);
        String insId2 = findStringValue(instrumentId2Start, instrumentId2End);
        insId2 = insId2.replaceAll("\\s", "");
        insId2 = insId2.substring(0, 5);
        //System.out.println("instrument Id2 = " + insId2);

        if (insId1 == null ? insId2 != null : !insId1.equals(insId2)) {
            return false;
        }
        //System.out.println("Instrument ID checks ok");

        Double thb = findValue(hgbStart, hgbEnd, 2);
        //System.out.println("Hb Check = " + thb);
        if (thb < 2 || thb > 20) {
            return false;
        }
        //System.out.println("Hb  checks ok");

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

    public void shiftPositions() {
        instrumentId1Start = 4 + shift1;
        instrumentId1End = 19 + shift1;
        sampleId1Start = 33 + shift1;
        sampleId1End = 47 + shift1;
        year1Start = 48 + shift1;
        yearEnd = 51 + shift1;
        month1Start = 52 + shift1;
        month1End = 53 + shift1;
        date1Start = 54 + shift1;
        date1End = 55 + shift1;
        hour1Start = 56 + shift1;
        hour1End = 57 + shift1;
        min1Start = 58 + shift1;
        min1End = 59 + shift1;
        sampleId2Start = 222 + shift2;
        sampleId2End = 236 + shift2;
        wbcStart = 237 + shift2;
        wbcEnd = 242 + shift2;
        rbcStart = 243 + shift2;
        rbcEnd = 247 + shift2;
        hgbStart = 248 + shift2;
        hgbEnd = 252 + shift2;
        hctStart = 253 + shift2;
        hctEnd = 257 + shift2;
        mcvStart = 258 + shift2;
        mcvEnd = 262 + shift2;
        mchStart = 263 + shift2;
        mchEnd = 267 + shift2;
        mchcStart = 268 + shift2;
        mchcEnd = 272 + shift2;
        pltStart = 273 + shift2;
        pltEnd = 277 + shift2;
        lymphPercentStart = 278 + shift2;
        lymphPercentEnd = 282 + shift2;
        monoPercentStart = 283 + shift2;
        monoPercentEnd = 287 + shift2;
        neuPercentStart = 288 + shift2;
        neuPercentEnd = 292 + shift2;
        eoPercentStart = 293 + shift2;
        eoPercentEnd = 297 + shift2;
        basoPercentStart = 298 + shift2;
        basoPercentEnd = 302 + shift2;
        instrumentId2Start = 421 + shift2;
        instrumentId2End = 436 + shift2;
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

    private String stringToAsciiBytesSpaceSeparated(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            sb.append((int) c).append(" ");
        }
        return sb.toString().trim(); // Remove trailing space
    }

    private String byteArrayToTextSeperatedBySpace(List<Byte> bytes) {
        StringBuilder sb = new StringBuilder();
        for (Byte b : bytes) {
            if (b != null) {
                sb.append(b.toString()).append(" ");
            }
        }
        return sb.toString().trim(); // Remove trailing space
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
//        //System.out.println("from = " + from);
//        //System.out.println("to = " + to);

        String display = "";
        for (int i = from; i < to + 1; i++) {
//            //System.out.println("i = " + i);
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

    public int getInstrumentId1Start() {
        return instrumentId1Start;
    }

    public void setInstrumentId1Start(int instrumentId1Start) {
        this.instrumentId1Start = instrumentId1Start;
    }

    public int getInstrumentId1End() {
        return instrumentId1End;
    }

    public void setInstrumentId1End(int instrumentId1End) {
        this.instrumentId1End = instrumentId1End;
    }

    public int getSampleId1Start() {
        return sampleId1Start;
    }

    public void setSampleId1Start(int sampleId1Start) {
        this.sampleId1Start = sampleId1Start;
    }

    public int getSampleId1End() {
        return sampleId1End;
    }

    public void setSampleId1End(int sampleId1End) {
        this.sampleId1End = sampleId1End;
    }

    public int getYear1Start() {
        return year1Start;
    }

    public void setYear1Start(int year1Start) {
        this.year1Start = year1Start;
    }

    public int getYearEnd() {
        return yearEnd;
    }

    public void setYearEnd(int yearEnd) {
        this.yearEnd = yearEnd;
    }

    public int getMonth1Start() {
        return month1Start;
    }

    public void setMonth1Start(int month1Start) {
        this.month1Start = month1Start;
    }

    public int getMonth1End() {
        return month1End;
    }

    public void setMonth1End(int month1End) {
        this.month1End = month1End;
    }

    public int getDate1Start() {
        return date1Start;
    }

    public void setDate1Start(int date1Start) {
        this.date1Start = date1Start;
    }

    public int getDate1End() {
        return date1End;
    }

    public void setDate1End(int date1End) {
        this.date1End = date1End;
    }

    public int getHour1Start() {
        return hour1Start;
    }

    public void setHour1Start(int hour1Start) {
        this.hour1Start = hour1Start;
    }

    public int getHour1End() {
        return hour1End;
    }

    public void setHour1End(int hour1End) {
        this.hour1End = hour1End;
    }

    public int getMin1Start() {
        return min1Start;
    }

    public void setMin1Start(int min1Start) {
        this.min1Start = min1Start;
    }

    public int getMin1End() {
        return min1End;
    }

    public void setMin1End(int min1End) {
        this.min1End = min1End;
    }

    public int getSampleId2Start() {
        return sampleId2Start;
    }

    public void setSampleId2Start(int sampleId2Start) {
        this.sampleId2Start = sampleId2Start;
    }

    public int getSampleId2End() {
        return sampleId2End;
    }

    public void setSampleId2End(int sampleId2End) {
        this.sampleId2End = sampleId2End;
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
        String tmpString = stringToAsciiBytesSpaceSeparated(inputStringBytesSpaceSeperated);
        this.inputStringBytesSpaceSeperated = tmpString;
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
            sampleId = (findValue(sampleId1Start, sampleId1End, 0)).longValue();
        } catch (Exception e) {
            sampleId = 0;
        }
        return sampleId;
    }

    public String getSampleIdString() {
        return findStringValue(sampleId1Start, sampleId1End);
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

    public int getShift1() {
        return shift1;
    }

    public void setShift1(int shift1) {
        this.shift1 = shift1;
    }

    public int getShift2() {
        return shift2;
    }

    public void setShift2(int shift2) {
        this.shift2 = shift2;
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
