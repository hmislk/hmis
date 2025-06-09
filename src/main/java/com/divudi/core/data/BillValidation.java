package com.divudi.core.data;

import com.divudi.core.entity.Institution;

/**
 *
 * @author Dr M H Buddhika Ariyaratne
 *
 */
public class BillValidation {

    private boolean errorPresent;
    private String errorMessage;
    private Institution company;

    public boolean isErrorPresent() {
        return errorPresent;
    }

    public void setErrorPresent(boolean errorPresent) {
        this.errorPresent = errorPresent;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Institution getCompany() {
        return company;
    }

    public void setCompany(Institution company) {
        this.company = company;
    }


}
