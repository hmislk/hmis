package com.divudi.core.data;

import com.divudi.core.entity.Institution;
import java.util.List;

/**
 *
 * @author Dr M H B Ariyaratne
 *
 */
public class SitesGroupedIntoInstitutions {
    private Institution institution;
    private List<Institution> sites;

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<Institution> getSites() {
        return sites;
    }

    public void setSites(List<Institution> sites) {
        this.sites = sites;
    }


}
