package com.kiu.recipes.dto;

import com.kiu.recipes.entity.Difficulty;
import com.kiu.recipes.entity.Recipe;
import com.kiu.recipes.entity.Tag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Comparator;

public record RecipeResponse(
        Long id,
        String title,
        String description,
        String instructions,
        int cookTimeMinutes,
        int servings,
        Difficulty difficulty,
        String imageUrl,
        LocalDateTime createdAt,
        Long chefId,
        String chefName,
        Long categoryId,
        String categoryName,
        List<String> tags) {

    public static final String UPLOAD_URL_PREFIX = "/uploads/";

    /** Must be called inside a transaction: touches the lazy chef, category and tags. */
    public static RecipeResponse from(Recipe r) {
        String imageUrl = r.getImageFileName() == null ? null : UPLOAD_URL_PREFIX + r.getImageFileName();
        List<String> tagNames = r.getTags().stream()
                .map(Tag::getName)
                .sorted(Comparator.naturalOrder())
                .toList();
        return new RecipeResponse(
                r.getId(), r.getTitle(), r.getDescription(), r.getInstructions(),
                r.getCookTimeMinutes(), r.getServings(), r.getDifficulty(), imageUrl, r.getCreatedAt(),
                r.getChef().getId(), r.getChef().getName(),
                r.getCategory().getId(), r.getCategory().getName(),
                tagNames);
    }
}
