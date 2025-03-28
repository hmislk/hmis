package com.divudi.core.data.dataStructure;

import com.divudi.core.entity.Item;
import java.util.Objects;
import java.util.UUID;

/**
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
public class ItemDetailsCell {

    private Item item;
    private Double quentity;
    private UUID id;

    public ItemDetailsCell() {
        id = UUID.randomUUID();
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Double getQuentity() {
        return quentity;
    }

    public void setQuentity(Double quentity) {
        this.quentity = quentity;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ItemDetailsCell other = (ItemDetailsCell) obj;
        return Objects.equals(this.id, other.id);
    }

}
