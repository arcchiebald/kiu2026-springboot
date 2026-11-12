package com.kiu.recipes.dto;

import com.kiu.recipes.entity.Chef;

public record ChefResponse(Long id, String name, String email, String bio) {

    public static ChefResponse from(Chef chef) {
        return new ChefResponse(chef.getId(), chef.getName(), chef.getEmail(), chef.getBio());
    }
}
