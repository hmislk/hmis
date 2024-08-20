/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.lab;

import com.divudi.data.InvestigationItemType;
import com.divudi.entity.Patient;
import com.divudi.entity.lab.InvestigationItem;
import com.divudi.entity.lab.PatientSample;
import com.divudi.entity.lab.PatientSampleComponant;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private int fieldCount;
    private Map<Integer, String> requestFields;
    private Map<Integer, String> responseFields;

    private String responseString;

    private Boolean limsHasSamplesToSend;
    private Boolean limsFoundPatientInvestigationToEnterResults;

    private MessageType limsMessageType;
    private MessageSubtype limsMessageSubtype;
    private String limsPatientId;
    private String limsSampleId;
    private String limsSampleType;
    private Priority limsPriority;
    private List<String> limsTests;
    private PatientSample limsPatientSample;
    private List<PatientSampleComponant> limsPatientSampleComponants;

    private Patient patient;

    private Boolean toDeleteSampleRequest;

    private MessageType analyzerMessageType;
    private MessageSubtype analyzerMessageSubtype;
    private String analyzerPatientId;
    private String analyzerSampleId;
    private SampleTypeForDimension analyzerSampleType;
    private DimensionPriority analyzerPriority;
    private List<DimensionTestResult> analyzerTestResults;

    private String instrumentId;
    private Byte firstPollValue;
    private Byte requestValue;
    private String requestAcceptanceStatus;

    public void analyzeReceivedMessage() {
        textToByteArraySeperatedBySpace();
        byteArrayToFields();
        classifyMessage();
        determineValues();
        determineMessageSubtype();

    }

    private void classifyMessage() {
        if (requestFields.size() < 2) {
            analyzerMessageType = MessageType.EmptyMessage;
            return;
        }
        String mt = requestFields.get(0).toUpperCase();
        if (mt == null) {
            analyzerMessageType = MessageType.EmptyMessage;
        } else if (mt.equalsIgnoreCase("P")) {
            analyzerMessageType = MessageType.Poll;
        } else if (mt.equalsIgnoreCase("D")) {
            analyzerMessageType = MessageType.SampleRequest;
        } else if (mt.equalsIgnoreCase("N")) {
            analyzerMessageType = MessageType.NoRequest;
        } else if (mt.equalsIgnoreCase("M")) {
            if (requestFields.size() == 3) {
                analyzerMessageType = MessageType.ResultAcceptance;
            } else if (requestFields.size() == 6) {
                analyzerMessageType = MessageType.RequestAcceptance;
            }
        } else if (mt.equalsIgnoreCase("I")) {
            if (requestFields.size() == 4) {
                analyzerMessageType = MessageType.EnhancedQueryMessage;
            } else if (requestFields.size() == 2) {
                analyzerMessageType = MessageType.QueryMessage;
            }
        } else if (mt.equalsIgnoreCase("R")) {
            analyzerMessageType = MessageType.ResultMessage;
        } else if (mt.equalsIgnoreCase("C")) {
            analyzerMessageType = MessageType.CaliberationResultMessage;
        } else {
            analyzerMessageType = MessageType.EmptyMessage;
        }
    }

    private void determineValues() {
        if (analyzerMessageType == MessageType.Poll) {
            instrumentId = requestFields.get(1);
            firstPollValue = getByte(requestFields.get(2));
            requestValue = getByte(requestFields.get(3));
        } else if (analyzerMessageType == MessageType.ResultAcceptance) {
            requestAcceptanceStatus = requestFields.get(1);
            //TODO: Reason for Rejection, Get Cup Positions
        } else if (analyzerMessageType == MessageType.RequestAcceptance) {
            requestAcceptanceStatus = requestFields.get(1);
            //TODO: Reason for Rejection, Get Cup Positions
        } else if (analyzerMessageType == MessageType.QueryMessage) {
            analyzerSampleId = requestFields.get(1);
        } else if (analyzerMessageType == MessageType.ResultMessage) {
            analyzerPatientId = requestFields.get(2);
            analyzerSampleId = requestFields.get(3);
            int resultsCount = Integer.parseInt(requestFields.get(10));
            analyzerTestResults = new ArrayList<>();
            for (int i = 0; i < resultsCount; i++) {
                int count = 11 + i * 4;
                DimensionTestResult tr = new DimensionTestResult();
                tr.setTestName(requestFields.get(count));
                tr.setTestResult(requestFields.get(count + 1));
                tr.setTestUnit(requestFields.get(count + 2));
                tr.setErrorCode(requestFields.get(count + 3));
                analyzerTestResults.add(tr);
            }
        }
    }

    private void determineMessageSubtype() {
//        //System.out.println("determineMessageSubtype");
//        //System.out.println("analyzerMessageType = " + analyzerMessageType);
//        //System.out.println("requestValue = " + requestValue);

        if (analyzerMessageType == MessageType.Poll) {
            if (firstPollValue == 1) {
                analyzerMessageSubtype = MessageSubtype.FirstPoll;
            } else {
                if (requestValue == 1) {
                    analyzerMessageSubtype = MessageSubtype.ConversationalPollReady;
                } else {
                    analyzerMessageSubtype = MessageSubtype.ConversationalPollBusy;
                }
            }
            return;
        } else if (analyzerMessageType == MessageType.RequestAcceptance) {
            if (requestAcceptanceStatus.equals("A")) {
                analyzerMessageSubtype = MessageSubtype.RequestAcceptanceSuccess;
            } else {
                analyzerMessageSubtype = MessageSubtype.RequestAcceptanceFailed;
            }
        }
    }

    private void createResponseFieldsForPollMessage() {
        responseFields = new HashMap<>();
        if (analyzerMessageSubtype == MessageSubtype.FirstPoll) {
            createNoSampleRequestMessage();
        } else if (analyzerMessageSubtype == MessageSubtype.ConversationalPollBusy) {
            createNoSampleRequestMessage();
            return;
        } else if (analyzerMessageSubtype == MessageSubtype.ConversationalPollReady) {
            if (limsHasSamplesToSend) {
//                toDeleteSampleRequest = false;
                createSampleRequestMessage();
            } else {
                createNoSampleRequestMessage();
            }
            return;
        }
    }

    private void createResponseFieldsForQueryMessage() {
        responseFields = new HashMap<>();
        if (limsHasSamplesToSend) {
            createSampleRequestMessage();
        } else {
            createNoSampleRequestMessage();
        }
        return;
    }

    private void createNoSampleRequestMessage() {
        limsMessageType = MessageType.SampleRequest;
        limsMessageSubtype = MessageSubtype.SampleRequestsNo;
        responseFields.put(0, "N");
    }

    private void createSampleRequestMessage() {
        if (limsTests == null || limsTests.isEmpty()) {
            createNoSampleRequestMessage();
            return;
        }
        limsMessageType = MessageType.SampleRequest;
        limsMessageSubtype = MessageSubtype.SampleRequestYes;
        responseFields.put(0, "D");
        responseFields.put(1, "0");
        responseFields.put(2, "0");
        if (toDeleteSampleRequest) {
            responseFields.put(3, "D");
        } else {
            responseFields.put(3, "A");
        }
        responseFields.put(4, limsPatientId);
        responseFields.put(5, limsSampleId);
        responseFields.put(6, analyzerSampleType.getFiledValue());
        responseFields.put(7, "");
        responseFields.put(8, analyzerPriority.getValue() + "");
        responseFields.put(9, "1");
        responseFields.put(10, "**");
        responseFields.put(11, "1");
        responseFields.put(12, limsTests.size() + "");
        int temTestCount = 13;
        for (String t : limsTests) {
            responseFields.put(temTestCount, t);
            temTestCount++;
        }

    }

    private void convertSampleStringToSampleType() {
        if (limsSampleType == null) {
            analyzerSampleType = SampleTypeForDimension.One;
        } else if (limsSampleType.toLowerCase().contains("blood")) {
            analyzerSampleType = SampleTypeForDimension.W;
        } else if (limsSampleType.toLowerCase().contains("serum")) {
            analyzerSampleType = SampleTypeForDimension.One;
        } else if (limsSampleType.toLowerCase().contains("plasma")) {
            analyzerSampleType = SampleTypeForDimension.Two;
        } else if (limsSampleType.toLowerCase().contains("urine")) {
            analyzerSampleType = SampleTypeForDimension.Three;
        } else if (limsSampleType.toLowerCase().contains("csf")) {
            analyzerSampleType = SampleTypeForDimension.Four;
        }
    }

    public void createResponseString() {
        String temRs = "";
        if (getResponseFields().isEmpty()) {
            responseString = "";
            return;
        }
        for (int i = 0; i < responseFields.size(); i++) {
            temRs += responseFields.get(i) + (char) 28;
        }
        String checkSum = calculateChecksum(temRs);
        temRs = (char) 2 + temRs + checkSum + (char) 3;
        byte[] temRes = temRs.getBytes(StandardCharsets.US_ASCII);
        temRs = "";
        for (Byte b : temRes) {
            temRs += b + "+";
        }
        responseString = temRs;
    }

    public String calculateChecksum(String input, boolean replaceFieldSeperator) {
        String ip = input;
        String fs = (char) 28 + "";
        ip = ip.replaceAll("<FS>", fs);
        return calculateChecksum(ip);
    }

    public String calculateChecksum(String input) {
        byte[] temBytes = stringToByteArray(input);
        return calculateChecksum(temBytes);
    }

    public String calculateChecksum(byte[] bytes) {
        long checksum = 0;
        for (int i = 0; i < bytes.length; i++) {
            checksum += (bytes[i] & 0xffffffffL);
        }
        int integerChecksum = (int) checksum;
        String hexChecksum = Integer.toHexString(integerChecksum).toUpperCase();
        return hexChecksum.substring(Math.max(hexChecksum.length() - 2, 0));
    }

    private byte[] stringToByteArray(String s) {
        return s.getBytes();
    }

    public boolean isCorrectReport() {
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
                bytes.add(null);
            }
        }
    }

    private Byte getByte(String input) {
        try {
            Byte b = Byte.parseByte(input);
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer getInteger(String input) {
        try {
            Integer b = Integer.parseInt(input);
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    private Double getDouble(String input) {
        try {
            Double b = Double.parseDouble(input);
            return b;
        } catch (Exception e) {
            return null;
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

    private void byteArrayToFields() {
        List<Byte> temBytes = new ArrayList<>();
        requestFields = new HashMap<>();
        for (Byte b : bytes) {
            if (b != 2 && b != 3 && b != 5) {
                temBytes.add(b);
//                //System.out.println("b = " + b);
            }
        }
        String temStr = "";
        Integer i = 0;
        for (byte b : temBytes) {
            if (b == 28) {
                requestFields.put(i, temStr);
//                //System.out.println("temStr = " + temStr);
                i++;
                temStr = new String();
            } else {
                char c = (char) b;
                temStr += c;
            }
        }
        fieldCount = i;
//        //System.out.println("fieldCount = " + fieldCount);
//        //System.out.println("requestFields.size() = " + requestFields.size());
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

    public void prepareResponseForResultMessages() {
        responseFields = null;
        if (limsFoundPatientInvestigationToEnterResults = true) {
            createResultAcceptanceMessageFields();
        } else {
            createResultRejectMessageFields();
        }
    }

    public void prepareResponseForCaliberationResultMessages() {
        createResultAcceptanceMessageFields();
    }

    private void createResultAcceptanceMessageFields() {
        responseFields = new HashMap<>();
        getResponseFields().put(0, "M");
        getResponseFields().put(1, "A");
        getResponseFields().put(2, "");
    }

    private void createResultRejectMessageFields() {
        responseFields = new HashMap<>();
        getResponseFields().put(0, "M");
        getResponseFields().put(1, "R");
        getResponseFields().put(2, "1");
    }

    public void prepareResponseForPollMessages() {
        PatientSample temPs = this.limsPatientSample;
        if (temPs == null) {
            setLimsHasSamplesToSend(false);
        } else {
            setLimsHasSamplesToSend(true);
            setLimsSampleId(analyzerSampleId);
            setLimsPatientId(temPs.getPatient().getPhn());
            List<String> temSss = getTestsFromPatientSample();
            this.setLimsTests(temSss);
        }
        createResponseFieldsForPollMessage();
//        createResponseString();
    }

    public void prepareResponseForQueryMessages() {
        PatientSample temPs = this.limsPatientSample;
        if (temPs == null) {
            setLimsHasSamplesToSend(false);
        } else {
            setLimsHasSamplesToSend(true);
            setLimsSampleId(analyzerSampleId);
            String temName = temPs.getPatient().getPerson().getName() + "                              ";
            temName = temName.substring(0, 25);
            setLimsPatientId(temName);
            List<String> temSss = getTestsFromPatientSample();
            this.setLimsTests(temSss);
        }
        createResponseFieldsForQueryMessage();
//        createResponseString();
    }

    public List<String> getTestsFromPatientSample() {
        Set<String> temsss = new HashSet<>();
        List<String> temss = new ArrayList<>();
        if (limsPatientSample == null) {
            return temss;
        }
        for (PatientSampleComponant c : limsPatientSampleComponants) {
            for (InvestigationItem tii : c.getPatientInvestigation().getInvestigation().getReportItems()) {
                if (tii.getIxItemType() == InvestigationItemType.Value) {
                    if (tii.getSample() != null) {
                        this.setLimsSampleType(tii.getSample().getName());
                    }
                    if (tii.getItem().getPriority() != null) {
                        this.setLimsPriority(tii.getItem().getPriority());
                    } else {
                        this.setLimsPriority(Priority.Routeine);
                    }
                    if (tii.getItem().isHasMoreThanOneComponant()) {
                        if (tii.getTest() != null && !tii.getTest().getName().trim().equals("")) {
                            if (tii.getSampleComponent().equals(limsPatientSample.getInvestigationComponant())) {
                                temsss.add(tii.getTest().getCode());
                            }
                        }
                    } else {
                        if (tii.getTest() != null && !tii.getTest().getName().trim().equals("")) {
                            temsss.add(tii.getTest().getCode());
                        }
                    }
                }
            }

        }
        temss = new ArrayList<>(temsss);
        return temss;
    }

    public String getInputStringBytesSpaceSeperated() {
        return inputStringBytesSpaceSeperated;
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

    public MessageType getAnalyzerMessageType() {
        return analyzerMessageType;
    }

    public void setAnalyzerMessageType(MessageType analyzerMessageType) {
        this.analyzerMessageType = analyzerMessageType;
    }

    public MessageSubtype getAnalyzerMessageSubtype() {
        return analyzerMessageSubtype;
    }

    public void setAnalyzerMessageSubtype(MessageSubtype analyzerMessageSubtype) {
        this.analyzerMessageSubtype = analyzerMessageSubtype;
    }

    public int getFieldCount() {
        return fieldCount;
    }

    public void setFieldCount(int fieldCount) {
        this.fieldCount = fieldCount;
    }

    public Map<Integer, String> getRequestFields() {
        return requestFields;
    }

    public void setRequestFields(Map<Integer, String> requestFields) {
        this.requestFields = requestFields;
    }

    public Map<Integer, String> getResponseFields() {
        if (responseFields == null) {
            responseFields = new HashMap<>();
        }
        return responseFields;
    }

    public void setResponseFields(Map<Integer, String> responseFields) {
        this.responseFields = responseFields;
    }

    public Boolean getLimsHasSamplesToSend() {
        return limsHasSamplesToSend;
    }

    public void setLimsHasSamplesToSend(Boolean limsHasSamplesToSend) {
        this.limsHasSamplesToSend = limsHasSamplesToSend;
    }

    public Boolean getToDeleteSampleRequest() {
        return toDeleteSampleRequest;
    }

    public void setToDeleteSampleRequest(Boolean toDeleteSampleRequest) {
        this.toDeleteSampleRequest = toDeleteSampleRequest;
    }

    public String getLimsPatientId() {
        return limsPatientId;
    }

    public void setLimsPatientId(String limsPatientId) {
        this.limsPatientId = limsPatientId;
    }

    public String getAnalyzerPatientId() {
        return analyzerPatientId;
    }

    public void setAnalyzerPatientId(String analyzerPatientId) {
        this.analyzerPatientId = analyzerPatientId;
    }

    public String getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }

    public Byte getFirstPollValue() {
        return firstPollValue;
    }

    public void setFirstPollValue(Byte firstPollValue) {
        this.firstPollValue = firstPollValue;
    }

    public Byte getRequestValue() {
        return requestValue;
    }

    public void setRequestValue(Byte requestValue) {
        this.requestValue = requestValue;
    }

    public String getLimsSampleId() {
        return limsSampleId;
    }

    public void setLimsSampleId(String limsSampleId) {
        this.limsSampleId = limsSampleId;
    }

    public String getAnalyzerSampleId() {
        return analyzerSampleId;
    }

    public void setAnalyzerSampleId(String analyzerSampleId) {
        this.analyzerSampleId = analyzerSampleId;
    }

    public String getLimsSampleType() {
        return limsSampleType;
    }

    public void setLimsSampleType(String limsSampleType) {
        this.limsSampleType = limsSampleType;
        convertSampleStringToSampleType();
    }

    public SampleTypeForDimension getAnalyzerSampleType() {
        return analyzerSampleType;
    }

    public void setAnalyzerSampleType(SampleTypeForDimension analyzerSampleType) {
        this.analyzerSampleType = analyzerSampleType;
    }

    public DimensionPriority getAnalyzerPriority() {
        return analyzerPriority;
    }

    public void setAnalyzerPriority(DimensionPriority analyzerPriority) {
        this.analyzerPriority = analyzerPriority;
    }

    public Priority getLimsPriority() {
        return limsPriority;
    }

    public void setLimsPriority(Priority limsPriority) {
        this.limsPriority = limsPriority;
        if (limsPriority == null) {
            limsPriority = Priority.Routeine;
        }
        switch (limsPriority) {
            case Asap:
                analyzerPriority = DimensionPriority.Two;
                break;
            case Stat:
                analyzerPriority = DimensionPriority.One;
                break;
            case Routeine:
                analyzerPriority = DimensionPriority.Zero;
                break;
            default:
                analyzerPriority = DimensionPriority.Zero;
        }
    }

    public List<String> getLimsTests() {
        return limsTests;
    }

    public void setLimsTests(List<String> limsTests) {
        this.limsTests = limsTests;
    }

    public String getResponseString() {
        return responseString;
    }

    public void setResponseString(String responseString) {
        this.responseString = responseString;
    }

    public String getRequestAcceptanceStatus() {
        return requestAcceptanceStatus;
    }

    public void setRequestAcceptanceStatus(String requestAcceptanceStatus) {
        this.requestAcceptanceStatus = requestAcceptanceStatus;
    }

    public MessageType getLimsMessageType() {
        return limsMessageType;
    }

    public void setLimsMessageType(MessageType limsMessageType) {
        this.limsMessageType = limsMessageType;
    }

    public MessageSubtype getLimsMessageSubtype() {
        return limsMessageSubtype;
    }

    public void setLimsMessageSubtype(MessageSubtype limsMessageSubtype) {
        this.limsMessageSubtype = limsMessageSubtype;
    }

    public PatientSample getLimsPatientSample() {
        return limsPatientSample;
    }

    public void setLimsPatientSample(PatientSample limsPatientSample) {
        this.limsPatientSample = limsPatientSample;
    }

    public List<PatientSampleComponant> getLimsPatientSampleComponants() {
        return limsPatientSampleComponants;
    }

    public void setLimsPatientSampleComponants(List<PatientSampleComponant> limsPatientSampleComponants) {
        this.limsPatientSampleComponants = limsPatientSampleComponants;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Boolean getLimsFoundPatientInvestigationToEnterResults() {
        return limsFoundPatientInvestigationToEnterResults;
    }

    public void setLimsFoundPatientInvestigationToEnterResults(Boolean limsFoundPatientInvestigationToEnterResults) {
        this.limsFoundPatientInvestigationToEnterResults = limsFoundPatientInvestigationToEnterResults;
    }

    public List<DimensionTestResult> getAnalyzerTestResults() {
        return analyzerTestResults;
    }

    public void setAnalyzerTestResults(List<DimensionTestResult> analyzerTestResults) {
        this.analyzerTestResults = analyzerTestResults;
    }

}
