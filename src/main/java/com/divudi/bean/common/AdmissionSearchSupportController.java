/*
 * Author : Dr. M H B Ariyaratne
 *
 * Consultant (Health Informatics)
 * (94) 71 5812399
 * Email : buddhika.ari@gmail.com
 *
 */
package com.divudi.bean.common;

import java.io.Serializable;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

/**
 * Backing support for the shared {@code ezcomp/inpatient/admission_search.xhtml}
 * composite, used across Inward/Pharmacy/Theater/Store admission-search pages
 * (issue #23165).
 *
 * The composite's optional {@code itemSelectListener} attribute needs a
 * default method to bind to when a calling page doesn't provide its own
 * "on select" business logic - {@link #noop()} is that default. Stateless
 * and side-effect free by design, so application scope is safe.
 */
@Named
@ApplicationScoped
public class AdmissionSearchSupportController implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Default {@code itemSelectListener} for {@code admcc:admission_search}.
     * Intentionally does nothing - pages that need real logic to run when an
     * admission is selected pass their own listener instead.
     */
    public void noop() {
        // Intentionally empty.
    }

}
