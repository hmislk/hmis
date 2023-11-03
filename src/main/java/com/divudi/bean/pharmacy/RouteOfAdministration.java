package com.divudi.bean.pharmacy;

import com.divudi.entity.Category;
import java.io.Serializable;
import javax.persistence.Entity;

/**
 * Represents the route of administration for a drug in the NHS UK dm+d (Dictionary of Medicines and Devices).
 * The RouteOfAdministration refers to the path by which a drug is taken into the body, such as oral, intravenous,
 * topical, etc. This entity is used to store and manage route of administration information for drugs within
 * the dm+d data model.
 *
 * @author Dr M H B Ariyaratne
 * @version 1.0
 * @since 2023-04-05
 */
@Entity
public class RouteOfAdministration extends Category implements Serializable {

    
    
}
