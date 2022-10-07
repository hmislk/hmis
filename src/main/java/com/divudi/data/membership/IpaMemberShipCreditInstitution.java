/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.membership;

import com.divudi.entity.Institution;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author safrin
 */
public class IpaMemberShipCreditInstitution {

    Institution institution;
    List<IpaMemberShip> ipaMemberShips;

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<IpaMemberShip> getIpaMemberShips() {
        if (ipaMemberShips == null) {
            ipaMemberShips = new ArrayList<>();
        }
        return ipaMemberShips;
    }

    public void setIpaMemberShips(List<IpaMemberShip> ipaMemberShips) {
        this.ipaMemberShips = ipaMemberShips;
    }

}
