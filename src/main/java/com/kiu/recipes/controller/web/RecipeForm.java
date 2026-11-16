package com.kiu.recipes.controller.web;

import com.kiu.recipes.dto.RecipeRequest;
import com.kiu.recipes.dto.RecipeResponse;
import com.kiu.recipes.entity.Difficulty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Backing object for the recipe HTML form. It mirrors {@link RecipeRequest} (same constraints) but is a
 * mutable bean, which is what Thymeleaf's {@code th:field} round-tripping needs. Tags are typed as one
 * comma-separated text box, and the optional photo arrives as a multipart file.
 */
public class RecipeForm {

    static final int MAX_TAG_LENGTH = 50;

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    @NotBlank(message = "Instructions are required")
    @Size(max = 4000, message = "Instructions must be at most 4000 characters")
    private String instructions;

    @NotNull(message = "Cooking time is required")
    @Min(value = 1, message = "Cooking time must be at least 1 minute")
    @Max(value = 1440, message = "Cooking time must be at most 1440 minutes")
    private Integer cookTimeMinutes;

    @NotNull(message = "Servings is required")
    @Min(value = 1, message = "Servings must be at least 1")
    @Max(value = 100, message = "Servings must be at most 100")
    private Integer servings;

    @NotNull(message = "Pick a difficulty")
    private Difficulty difficulty = Difficulty.EASY;

    @NotNull(message = "Pick a chef")
    private Long chefId;

    @NotNull(message = "Pick a category")
    private Long categoryId;

    @Size(max = 500, message = "Tags must be at most 500 characters in total")
    private String tags;

    private MultipartFile image;

    public static RecipeForm from(RecipeResponse r) {
        RecipeForm f = new RecipeForm();
        f.title = r.title();
        f.description = r.description();
        f.instructions = r.instructions();
        f.cookTimeMinutes = r.cookTimeMinutes();
        f.servings = r.servings();
        f.difficulty = r.difficulty();
        f.chefId = r.chefId();
        f.categoryId = r.categoryId();
        f.tags = String.join(", ", r.tags());
        return f;
    }

    /** Splits the comma-separated tag box into distinct, trimmed, non-empty names (original order kept). */
    public Set<String> tagNames() {
        if (tags == null || tags.isBlank()) {
            return new LinkedHashSet<>();
        }
        Set<String> names = new LinkedHashSet<>();
        Arrays.stream(tags.split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(names::add);
        return names;
    }

    public boolean hasOverlongTag() {
        return tagNames().stream().anyMatch(t -> t.length() > MAX_TAG_LENGTH);
    }

    public RecipeRequest toRequest() {
        return new RecipeRequest(title, description, instructions, cookTimeMinutes, servings,
                difficulty, chefId, categoryId, tagNames());
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public Integer getCookTimeMinutes() { return cookTimeMinutes; }
    public void setCookTimeMinutes(Integer cookTimeMinutes) { this.cookTimeMinutes = cookTimeMinutes; }
    public Integer getServings() { return servings; }
    public void setServings(Integer servings) { this.servings = servings; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public Long getChefId() { return chefId; }
    public void setChefId(Long chefId) { this.chefId = chefId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public MultipartFile getImage() { return image; }
    public void setImage(MultipartFile image) { this.image = image; }
}
