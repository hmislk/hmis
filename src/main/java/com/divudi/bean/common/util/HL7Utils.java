package com.divudi.bean.common.util;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.Parser;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        System.out.println("segments = " + segments);
        String messageType = null;
        for (String segment : segments) {
            System.out.println("segment = " + segment);
            if (segment.startsWith("MSH|")) {
                String[] fields = segment.split("\\|");
                messageType = fields[8];
                break;
            }
        }
        return messageType;
    }
    
     public static String sendACK_R22ForoulR22R22(String oulR22R22Message) {
        try {
            Parser parser = hapiContext.getGenericParser();
            Message oulR22 = parser.parse(oulR22R22Message);
            Message ackR22 = oulR22.generateACK();
            return ackR22.toString();
        } catch (HL7Exception | IOException ex) {
            Logger.getLogger(HL7Utils.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
    }
}
