package com.divudi.bean.pharmacy;

import com.divudi.entity.Category;
import java.io.Serializable;
import javax.persistence.Entity;

/**
 * Represents the pharmaceutical dosage form of a drug in the NHS UK dm+d (Dictionary of Medicines and Devices).
 * The DosageForm refers to the physical form in which a drug is administered, such as tablets, capsules,
 * liquids, creams, etc. This entity is used to store and manage dosage form information for drugs within
 * the dm+d data model.
 *
 * @author Dr M H B Ariyaratne
 * @version 1.0
 * @since 2023-04-05
 * */
@Entity
public class DosageForm extends Category implements Serializable {

    
    
}
