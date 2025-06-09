package com.divudi.core.entity;

import java.util.Date;

/**
 *
 * @author Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 * +94 71 58 123 99
 *
 */
public interface RetirableEntity {
    void setRetired(boolean retired);
    boolean isRetired();

    void setRetiredAt(Date retiredAt);
    Date getRetiredAt();

    void setRetirer(WebUser retirer);
    WebUser getRetirer();

    void setRetireComments(String comments);
    String getRetireComments();
}
