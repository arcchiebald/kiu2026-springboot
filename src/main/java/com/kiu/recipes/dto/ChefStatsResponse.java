package com.kiu.recipes.dto;

import com.kiu.recipes.repository.ChefStats;

public record ChefStatsResponse(Long chefId, String name, long recipeCount) {

    public static ChefStatsResponse from(ChefStats s) {
        return new ChefStatsResponse(s.getChefId(), s.getName(), s.getRecipeCount());
    }
}
