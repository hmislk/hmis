package com.divudi.core.data.dto.user;

import java.util.List;

public class PrivilegeCategoryAssignmentRequestDTO {

    private List<String> categories;

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }
}
