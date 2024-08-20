/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.hr;

import com.divudi.entity.hr.FingerPrintRecord;
import java.util.Comparator;

/**
 *
 * @author safrin
 */
public class FingerPrintComparator implements Comparator<FingerPrintRecord> {

    @Override
    public int compare(FingerPrintRecord o1, FingerPrintRecord o2) {
        if (o1.getRecordTimeStamp() == null) {
            return 1;
        }
        if (o2.getRecordTimeStamp() == null) {
            return -1;
        }

        return o1.getRecordTimeStamp().compareTo(o2.getRecordTimeStamp());  //To change body of generated methods, choose Tools | Templates.
    }
}
