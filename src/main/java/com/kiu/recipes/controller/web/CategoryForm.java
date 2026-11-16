package com.kiu.recipes.controller.web;

import com.kiu.recipes.dto.CategoryRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Mutable form bean for creating a category (mirrors {@link CategoryRequest}). */
public class CategoryForm {

    @NotBlank(message = "Name is required")
    @Size(max = 80, message = "Name must be at most 80 characters")
    private String name;

    public CategoryRequest toRequest() {
        return new CategoryRequest(name);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
