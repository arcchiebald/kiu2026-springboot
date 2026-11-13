package com.kiu.recipes.controller;

import com.kiu.recipes.dto.PageResponse;
import com.kiu.recipes.dto.RecipeRequest;
import com.kiu.recipes.dto.RecipeResponse;
import com.kiu.recipes.entity.Difficulty;
import com.kiu.recipes.exception.InvalidRequestException;
import com.kiu.recipes.service.RecipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/recipes")
@Tag(name = "Recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @Operation(summary = "List recipes (paged + sorted)",
            description = "Standard Spring Data paging: ?page=0&size=5&sort=cookTimeMinutes,asc. Optional ?title= filter.")
    @GetMapping
    public PageResponse<RecipeResponse> list(
            @RequestParam(required = false) String title,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return recipeService.findAll(title, pageable);
    }

    @Operation(summary = "Search recipes with combined optional filters",
            description = "Filters: tag, categoryId, difficulty, minTime, maxTime. "
                    + "Sorting: sortBy (title, cookTimeMinutes, servings, difficulty, createdAt) + direction (asc/desc).")
    @GetMapping("/search")
    public PageResponse<RecipeResponse> search(
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) Integer minTime,
            @RequestParam(required = false) Integer maxTime,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Sort.Direction dir = Sort.Direction.fromOptionalString(direction)
                .orElseThrow(() -> new InvalidRequestException("direction must be 'asc' or 'desc'"));
        return recipeService.search(tag, categoryId, difficulty, minTime, maxTime, sortBy, dir, page, size);
    }

    @GetMapping("/{id}")
    public RecipeResponse get(@PathVariable Long id) {
        return recipeService.findById(id);
    }

    @Operation(summary = "Create a recipe (JSON)")
    @PostMapping
    public ResponseEntity<RecipeResponse> create(@Valid @RequestBody RecipeRequest request) {
        RecipeResponse created = recipeService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public RecipeResponse update(@PathVariable Long id, @Valid @RequestBody RecipeRequest request) {
        return recipeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        recipeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
