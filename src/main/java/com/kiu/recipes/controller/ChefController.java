package com.kiu.recipes.controller;

import com.kiu.recipes.dto.ChefRequest;
import com.kiu.recipes.dto.ChefResponse;
import com.kiu.recipes.dto.ChefStatsResponse;
import com.kiu.recipes.dto.PageResponse;
import com.kiu.recipes.dto.RecipeResponse;
import com.kiu.recipes.service.ChefService;
import com.kiu.recipes.service.RecipeService;
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
import java.util.List;

@RestController
@RequestMapping("/api/chefs")
@Tag(name = "Chefs")
public class ChefController {

    private final ChefService chefService;
    private final RecipeService recipeService;

    public ChefController(ChefService chefService, RecipeService recipeService) {
        this.chefService = chefService;
        this.recipeService = recipeService;
    }

    @GetMapping
    public PageResponse<ChefResponse> list(@PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return chefService.findAll(pageable);
    }

    @GetMapping("/top")
    public List<ChefStatsResponse> top(@RequestParam(defaultValue = "5") int limit) {
        return chefService.top(limit);
    }

    @GetMapping("/{id}")
    public ChefResponse get(@PathVariable Long id) {
        return chefService.findById(id);
    }

    @GetMapping("/{id}/recipes")
    public List<RecipeResponse> recipes(@PathVariable Long id) {
        return recipeService.findByChef(id);
    }

    @PostMapping
    public ResponseEntity<ChefResponse> create(@Valid @RequestBody ChefRequest request) {
        ChefResponse created = chefService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ChefResponse update(@PathVariable Long id, @Valid @RequestBody ChefRequest request) {
        return chefService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        chefService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
