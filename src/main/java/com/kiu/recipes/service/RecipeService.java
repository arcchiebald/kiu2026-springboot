package com.kiu.recipes.service;

import com.kiu.recipes.dto.PageResponse;
import com.kiu.recipes.dto.RecipeRequest;
import com.kiu.recipes.dto.RecipeResponse;
import com.kiu.recipes.entity.Category;
import com.kiu.recipes.entity.Chef;
import com.kiu.recipes.entity.Difficulty;
import com.kiu.recipes.entity.Recipe;
import com.kiu.recipes.exception.InvalidRequestException;
import com.kiu.recipes.exception.ResourceNotFoundException;
import com.kiu.recipes.repository.CategoryRepository;
import com.kiu.recipes.repository.ChefRepository;
import com.kiu.recipes.repository.RecipeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class RecipeService {

    /** Fields the search endpoint may sort by (guards against arbitrary property names). */
    static final Set<String> SORTABLE_FIELDS =
            Set.of("title", "cookTimeMinutes", "servings", "difficulty", "createdAt");
    static final int MAX_PAGE_SIZE = 50;
    static final int MAX_COOK_TIME = 1440;

    private final RecipeRepository recipeRepository;
    private final ChefRepository chefRepository;
    private final CategoryRepository categoryRepository;
    private final TagService tagService;

    public RecipeService(RecipeRepository recipeRepository,
                         ChefRepository chefRepository,
                         CategoryRepository categoryRepository,
                         TagService tagService) {
        this.recipeRepository = recipeRepository;
        this.chefRepository = chefRepository;
        this.categoryRepository = categoryRepository;
        this.tagService = tagService;
    }

    // ---------- reads ----------

    /** Plain list; paging and sorting come straight from the request's Pageable (?page&size&sort). */
    @Transactional(readOnly = true)
    public PageResponse<RecipeResponse> findAll(String title, Pageable pageable) {
        Page<Recipe> page = (title == null || title.isBlank())
                ? recipeRepository.findAll(pageable)
                : recipeRepository.findByTitleContainingIgnoreCase(title.trim(), pageable);
        return PageResponse.from(page, RecipeResponse::from);
    }

    @Transactional(readOnly = true)
    public RecipeResponse findById(Long id) {
        return RecipeResponse.from(getEntity(id));
    }

    /** Combined filters + explicit Sort built from sortBy/direction. */
    @Transactional(readOnly = true)
    public PageResponse<RecipeResponse> search(String tag, Long categoryId, Difficulty difficulty,
                                               Integer minTime, Integer maxTime,
                                               String sortBy, Sort.Direction direction,
                                               int page, int size) {
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new InvalidRequestException("sortBy must be one of " + SORTABLE_FIELDS);
        }
        int min = minTime == null ? 0 : minTime;
        int max = maxTime == null ? MAX_COOK_TIME : maxTime;
        if (min > max) {
            throw new InvalidRequestException("minTime must not be greater than maxTime");
        }
        if (page < 0 || size < 1) {
            throw new InvalidRequestException("page must be >= 0 and size must be >= 1");
        }
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by(direction, sortBy));
        String tagFilter = (tag == null || tag.isBlank()) ? null : tag.trim();
        return PageResponse.from(
                recipeRepository.search(tagFilter, categoryId, difficulty, min, max, pageable),
                RecipeResponse::from);
    }

    @Transactional(readOnly = true)
    public List<RecipeResponse> findByChef(Long chefId) {
        if (!chefRepository.existsById(chefId)) {
            throw new ResourceNotFoundException("Chef", chefId);
        }
        return recipeRepository.findByChefIdOrderByCreatedAtDesc(chefId).stream()
                .map(RecipeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<RecipeResponse> findByCategoryUpToTime(Long categoryId, int maxTime, Pageable pageable) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category", categoryId);
        }
        return PageResponse.from(
                recipeRepository.findByCategoryIdAndCookTimeMinutesLessThanEqual(categoryId, maxTime, pageable),
                RecipeResponse::from);
    }

    // ---------- writes ----------

    @Transactional
    public RecipeResponse create(RecipeRequest request) {
        Recipe recipe = new Recipe();
        apply(recipe, request);
        return RecipeResponse.from(recipeRepository.save(recipe));
    }

    @Transactional
    public RecipeResponse update(Long id, RecipeRequest request) {
        Recipe recipe = getEntity(id);
        apply(recipe, request);
        return RecipeResponse.from(recipe);
    }

    @Transactional
    public void delete(Long id) {
        recipeRepository.delete(getEntity(id));
    }

    // ---------- helpers ----------

    private void apply(Recipe recipe, RecipeRequest req) {
        Chef chef = chefRepository.findById(req.chefId())
                .orElseThrow(() -> new ResourceNotFoundException("Chef", req.chefId()));
        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", req.categoryId()));

        recipe.setTitle(req.title().trim());
        recipe.setDescription(req.description());
        recipe.setInstructions(req.instructions());
        recipe.setCookTimeMinutes(req.cookTimeMinutes());
        recipe.setServings(req.servings());
        recipe.setDifficulty(req.difficulty());
        recipe.setChef(chef);
        recipe.setCategory(category);
        recipe.setTags(tagService.resolveTags(req.tagNames()));
    }

    private Recipe getEntity(Long id) {
        return recipeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Recipe", id));
    }
}
