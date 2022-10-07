/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.channel;

/**
 *
 * @author Lahiru Madushanka
 */
public enum ReferenceBookEnum {

    ChannelBook,
    LabBook,;

    public String getLabel() {
        switch (this) {
            case ChannelBook:
                return "Agent Book";
            case LabBook:
                return "Lab Book";

        }
        return "Other";
    }

}
