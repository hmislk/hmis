package com.divudi.data;

import java.util.Date;
import java.util.List;

/**
 *
 * @author Dr M H B Ariyaratne
 * 
 */
public class DateAndListOfSitesGroupedIntoInstitutions {
    private Date date;
    private List<SitesGroupedIntoInstitutions> listOfSitesGroupedIntoInstitutionses;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public List<SitesGroupedIntoInstitutions> getListOfSitesGroupedIntoInstitutionses() {
        return listOfSitesGroupedIntoInstitutionses;
    }

    public void setListOfSitesGroupedIntoInstitutionses(List<SitesGroupedIntoInstitutions> listOfSitesGroupedIntoInstitutionses) {
        this.listOfSitesGroupedIntoInstitutionses = listOfSitesGroupedIntoInstitutionses;
    }
    
    
}
