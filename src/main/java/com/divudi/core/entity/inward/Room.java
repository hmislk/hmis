/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity.inward;

import com.divudi.core.data.inward.BedStatus;
import com.divudi.core.entity.Category;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;


/**
 *
 * @author buddhika
 */
@Entity
public class Room extends Category implements Serializable {

    @Enumerated(EnumType.STRING)
    @Column(name = "BED_STATUS")
    private BedStatus bedStatus = BedStatus.Available;

    @Lob
    private String svgChildView;

    public BedStatus getBedStatus() {
        return bedStatus;
    }

    public void setBedStatus(BedStatus bedStatus) {
        this.bedStatus = bedStatus;
    }

    public String getSvgChildView() {
        return svgChildView;
    }

    public void setSvgChildView(String svgChildView) {
        this.svgChildView = svgChildView;
    }
}
