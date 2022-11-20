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
public class SysMexAdf1 {

    private String inputStringBytesSpaceSeperated;
    private String inputStringBytesPlusSeperated;
    private String inputStringCharactors;
    private List<Byte> bytes;

    private int lengthOfMessage = 191;
    private int instrumentIdStart = 4;
    private int instrumentIdEnd = 19;
    private int sampleIdStart = 33;
    private int sampleIdEnd = 47;
    private int yearStart = 48;
    private int yearEnd = 51;
    private int month1Start = 52;
    private int month1End = 53;
    private int date1Start = 54;
    private int date1End = 55;
    private int hour1Start = 56;
    private int hour1End = 57;
    private int min1Start = 58;
    private int min1End = 59;

    private long sampleId;

    public boolean isCorrectReport() {
        boolean flag = true;
        if (bytes == null || bytes.isEmpty()) {
            return false;
        }
        if (bytes.size() != 191) {
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

    public int getSampleId1Start() {
        return sampleIdStart;
    }

    public void setSampleId1Start(int sampleId1Start) {
        this.sampleIdStart = sampleId1Start;
    }

    public int getSampleIdEnd() {
        return sampleIdEnd;
    }

    public void setSampleIdEnd(int sampleIdEnd) {
        this.sampleIdEnd = sampleIdEnd;
    }

    public int getYearStart() {
        return yearStart;
    }

    public void setYearStart(int yearStart) {
        this.yearStart = yearStart;
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
   
}
