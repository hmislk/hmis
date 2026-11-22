package com.divudi.core.entity;

import java.io.Serializable;
import javax.persistence.Entity;

/**
 * A patient complaint category (#25886). An admin-maintained list stored in the
 * shared Category table (single-table inheritance), so it needs no table of its own.
 */
@Entity
public class PatientComplaintCategory extends Category implements Serializable {

    private static final long serialVersionUID = 1L;
}
