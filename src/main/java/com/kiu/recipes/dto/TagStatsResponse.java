package com.kiu.recipes.dto;

import com.kiu.recipes.repository.TagStats;

public record TagStatsResponse(Long tagId, String name, long recipeCount) {

    public static TagStatsResponse from(TagStats s) {
        return new TagStatsResponse(s.getTagId(), s.getName(), s.getRecipeCount());
    }
}
