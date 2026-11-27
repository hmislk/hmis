package com.divudi.core.entity.inward;

import com.divudi.core.entity.Category;
import java.io.Serializable;
import javax.persistence.Entity;

/**
 * A reason for cancelling an admission (#25745). An admin-maintained list stored
 * in the shared Category table (single-table inheritance), so it needs no table
 * of its own.
 */
@Entity
public class AdmissionCancellationReason extends Category implements Serializable {

    private static final long serialVersionUID = 1L;
}
