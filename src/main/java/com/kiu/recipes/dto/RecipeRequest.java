package com.kiu.recipes.dto;

import com.kiu.recipes.entity.Difficulty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Incoming recipe data. A record binds both from a JSON body (@RequestBody)
 * and from multipart form fields (@ModelAttribute).
 */
public record RecipeRequest(
        @NotBlank(message = "title is required")
        @Size(max = 150, message = "title must be at most 150 characters")
        String title,

        @Size(max = 1000, message = "description must be at most 1000 characters")
        String description,

        @NotBlank(message = "instructions are required")
        @Size(max = 4000, message = "instructions must be at most 4000 characters")
        String instructions,

        @NotNull(message = "cookTimeMinutes is required")
        @Min(value = 1, message = "cookTimeMinutes must be at least 1")
        @Max(value = 1440, message = "cookTimeMinutes must be at most 1440")
        Integer cookTimeMinutes,

        @NotNull(message = "servings is required")
        @Min(value = 1, message = "servings must be at least 1")
        @Max(value = 100, message = "servings must be at most 100")
        Integer servings,

        @NotNull(message = "difficulty is required (EASY, MEDIUM or HARD)")
        Difficulty difficulty,

        @NotNull(message = "chefId is required")
        Long chefId,

        @NotNull(message = "categoryId is required")
        Long categoryId,

        /** Tag names; unknown tags are created on the fly. */
        Set<@NotBlank(message = "tag names must not be blank")
            @Size(max = 50, message = "tag names must be at most 50 characters") String> tagNames) {
}
