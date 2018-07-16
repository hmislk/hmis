/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.lab;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 *
 * @author Dr. M H B Ariyaratne <buddhika.ari@gmail.com>
 */
public class Dimension {

    private String inputStringBytesSpaceSeperated;
    private String inputStringBytesPlusSeperated;
    private String inputStringCharactors;
    private List<Byte> bytes;
    MessageType messageType;

    int fieldCount;
    Map<Integer, String> fields;

    private long sampleId;

    public boolean isCorrectReport() {
        System.out.println("Checking wether the report is Correct");
        boolean flag = true;

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
//                System.out.println("e = " + e);
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
//                System.out.println("e = " + e);
                bytes.add(null);
            }
        }
    }

    private void byteArrayToFields() {
        List<Byte> temBytes = new ArrayList<>();
        fields = new HashMap<>();
        for (Byte b : bytes) {
            if (b != 3 && b != 5) {
                temBytes.add(b);
            }
        }
        String temStr = "";
        Integer i = 0;
        for (byte b : temBytes) {
            if (b == 28) {
                fields.put(i, temStr);
                i++;
                temStr = "";
            } else {
                char c = (char) b;
                temStr += c;
            }
        }
        fieldCount = i;
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

    public String getInputStringBytesSpaceSeperated() {
        return inputStringBytesSpaceSeperated;
    }

    public void prepareMessage() {
        textToByteArraySeperatedBySpace();
        byteArrayToFields();
        classifyMessage();
        
    }

    private void classifyMessage() {
        if (fields.size() < 2) {
            messageType = MessageType.EmptyMessage;
            return;
        }
        String mt = fields.get(0).toUpperCase();
        if (mt == null) {
            messageType = MessageType.EmptyMessage;
        } else if (mt.equalsIgnoreCase("P")) {
            messageType = MessageType.Poll;
        } else if (mt.equalsIgnoreCase("D")) {
            messageType = MessageType.SampleRequest;
        } else if (mt.equalsIgnoreCase("N")) {
            messageType = MessageType.NoRequest;
        } else if (mt.equalsIgnoreCase("M")) {
            if (fields.size() == 3) {
                messageType = MessageType.ResultAcceptance;
            } else if (fields.size() == 6) {
                messageType = MessageType.RequestAcceptance;
            }
        } else if (mt.equalsIgnoreCase("I")) {
            if (fields.size() == 4) {
                messageType = MessageType.EnhancedQueryMessage;
            } else if (fields.size() == 2) {
                messageType = MessageType.QueryMessage;
            }
        }else if (mt.equalsIgnoreCase("R")) {
            messageType = MessageType.ResultMessage;
        }else if (mt.equalsIgnoreCase("C")) {
            messageType = MessageType.CaliberationResultMessage;
        }else{
            messageType = MessageType.EmptyMessage;
        }

        
    }

    public void setInputStringBytesSpaceSeperated(String inputStringBytesSpaceSeperated) {
        this.inputStringBytesSpaceSeperated = inputStringBytesSpaceSeperated;
    }

    public List<Byte> getBytes() {
        return bytes;
    }

    public void setBytes(List<Byte> bytes) {
        this.bytes = bytes;
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
