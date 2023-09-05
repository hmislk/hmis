package com.divudi.bean.common.util;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.Parser;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.datatype.CX;
import ca.uhn.hl7v2.model.v25.datatype.XPN;
import ca.uhn.hl7v2.model.v25.group.OUL_R22_ORDER;
import ca.uhn.hl7v2.model.v25.group.OUL_R22_RESULT;
import ca.uhn.hl7v2.model.v25.group.OUL_R22_SPECIMEN;
import ca.uhn.hl7v2.model.v25.message.OUL_R22;
import ca.uhn.hl7v2.model.v25.segment.*;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.parser.PipeParser;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author buddh
 */
public class HL7Utils {

    private static final HapiContext hapiContext = new DefaultHapiContext();

    public static String sendACK_R22ForoulR22(String oulR22Message) {
        try {
            Parser parser = hapiContext.getGenericParser();
            Message oulR22 = parser.parse(oulR22Message);
            Message ackR22 = oulR22.generateACK();
            return ackR22.toString();
        } catch (HL7Exception | IOException ex) {
            Logger.getLogger(HL7Utils.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
    }

    public static String findMessageType(String hl7Message) {
        String[] segments = hl7Message.split("\r");
        String messageType = null;
        for (String segment : segments) {
            if (segment.startsWith("MSH|")) {
                String[] fields = segment.split("\\|");
                messageType = fields[8];
                break;
            }
        }
        if(messageType==null){
            messageType = "ASTM";
        }
        return messageType;
    }

//    public static String sendACK_R22ForoulR22R22(String oulR22R22Message) {
//        try {
//            Parser parser = hapiContext.getGenericParser();
//            Message oulR22 = parser.parse(oulR22R22Message);
//            Message ackR22 = oulR22.generateACK();
//            return ackR22.toString();
//        } catch (HL7Exception | IOException ex) {
//            Logger.getLogger(HL7Utils.class.getName()).log(Level.SEVERE, null, ex);
//            return "";
//        }
//    }

//    public void extractOULR22MessageData(String hl7Message) {
//        Parser parser = new PipeParser();
//        try {
//            Message message = parser.parse(hl7Message);
//            if (message instanceof OUL_R22) {
//                OUL_R22 oulR22Message = (OUL_R22) message;
//                OUL_R22_ORDER oulR22Order = (OUL_R22_ORDER) message;
//                
//                // Extract Sample Details
//                List<OUL_R22_SPECIMEN> specimens = oulR22Message.getSPECIMENAll();
//                for (OUL_R22_SPECIMEN sp : specimens) {
//                    SPM spm = sp.getSPM();
//                    String specimenId = spm.getSpecimenID().toString();
//                }
//                
//                List<OUL_R22_RESULT> rs=  oulR22Order.getRESULTAll();
//                
//                for(OUL_R22_RESULT r:rs){
//                    TCD tcd = r.getTCD();
//                    
//                }
//                
//                
//                
//
//                // Extract Patient Details
//                PID pid = oulR22Message.getPATIENT().getPID();
//                CX patientId = pid.getPatientID();
//                XPN patientName = pid.getPatientName(0);
//
//                List<String> orderDetailsList = new ArrayList<>();
//                List<String> resultDetailsList = new ArrayList<>();
//
//                // Extract Order and Result Details
//                
//
//                // You can now use extracted data (e.g., specimenId, patientId, patientName, orderDetailsList, resultDetailsList) as needed
//            } else {
//                throw new HL7Exception("Unsupported message type");
//            }
//        } catch (HL7Exception e) {
//        }
//    }
}
