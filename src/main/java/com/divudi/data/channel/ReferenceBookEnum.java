/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
