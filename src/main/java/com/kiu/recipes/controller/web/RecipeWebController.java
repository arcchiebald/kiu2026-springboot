package com.kiu.recipes.controller.web;

import com.kiu.recipes.dto.PageResponse;
import com.kiu.recipes.dto.RecipeResponse;
import com.kiu.recipes.entity.Difficulty;
import com.kiu.recipes.exception.DuplicateResourceException;
import com.kiu.recipes.exception.InvalidFileException;
import com.kiu.recipes.exception.InvalidRequestException;
import com.kiu.recipes.exception.ResourceNotFoundException;
import com.kiu.recipes.service.CategoryService;
import com.kiu.recipes.service.ChefService;
import com.kiu.recipes.service.RecipeService;
import com.kiu.recipes.service.TagService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/** Server-rendered pages for browsing, creating, editing and deleting recipes. */
@Controller
public class RecipeWebController {

    private static final int PAGE_SIZE = 9;

    /** Value of the "sort" dropdown -> label. The value is "field,direction". */
    private static final Map<String, String> SORT_OPTIONS = new LinkedHashMap<>();

    static {
        SORT_OPTIONS.put("createdAt,desc", "Newest first");
        SORT_OPTIONS.put("createdAt,asc", "Oldest first");
        SORT_OPTIONS.put("title,asc", "Title A–Z");
        SORT_OPTIONS.put("title,desc", "Title Z–A");
        SORT_OPTIONS.put("cookTimeMinutes,asc", "Quickest first");
        SORT_OPTIONS.put("cookTimeMinutes,desc", "Slowest first");
        SORT_OPTIONS.put("servings,desc", "Most servings");
    }

    private final RecipeService recipeService;
    private final ChefService chefService;
    private final CategoryService categoryService;
    private final TagService tagService;

    public RecipeWebController(RecipeService recipeService, ChefService chefService,
                               CategoryService categoryService, TagService tagService) {
        this.recipeService = recipeService;
        this.chefService = chefService;
        this.categoryService = categoryService;
        this.tagService = tagService;
    }

    // ---------- browse ----------

    @GetMapping({"/", "/recipes"})
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String tag,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) Difficulty difficulty,
                       @RequestParam(required = false) Integer maxTime,
                       @RequestParam(defaultValue = "createdAt,desc") String sort,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        if (!SORT_OPTIONS.containsKey(sort)) {
            throw new InvalidRequestException("Unknown sort order '" + sort + "'");
        }
        String[] sortParts = sort.split(",");
        Sort.Direction direction = Sort.Direction.fromString(sortParts[1]);

        PageResponse<RecipeResponse> result = recipeService.search(
                q, tag, categoryId, difficulty, null, maxTime, sortParts[0], direction, Math.max(page, 0), PAGE_SIZE);

        // Query string that keeps the active filters; the template appends "page=N" for the pager links.
        UriComponentsBuilder qs = UriComponentsBuilder.newInstance();
        addIfPresent(qs, "q", q);
        addIfPresent(qs, "tag", tag);
        addIfPresent(qs, "categoryId", categoryId);
        addIfPresent(qs, "difficulty", difficulty);
        addIfPresent(qs, "maxTime", maxTime);
        qs.queryParam("sort", sort);

        model.addAttribute("recipes", result);
        model.addAttribute("q", q);
        model.addAttribute("tag", tag);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("difficulty", difficulty);
        model.addAttribute("maxTime", maxTime);
        model.addAttribute("sort", sort);
        model.addAttribute("sortOptions", SORT_OPTIONS);
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("popularTags", tagService.popular(12));
        model.addAttribute("filtered", q != null && !q.isBlank() || tag != null && !tag.isBlank()
                || categoryId != null || difficulty != null || maxTime != null);
        model.addAttribute("queryString", qs.build().encode().getQuery());
        return "recipe/list";
    }

    private static void addIfPresent(UriComponentsBuilder qs, String name, Object value) {
        if (value != null && !value.toString().isBlank()) {
            qs.queryParam(name, value.toString().trim());
        }
    }

    @GetMapping("/recipes/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("recipe", recipeService.findById(id));
        return "recipe/detail";
    }

    // ---------- create ----------

    @GetMapping("/recipes/new")
    public String newForm(Model model) {
        model.addAttribute("recipeForm", new RecipeForm());
        addFormLookups(model);
        return "recipe/form";
    }

    @PostMapping("/recipes")
    public String create(@Valid @ModelAttribute("recipeForm") RecipeForm form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        checkTags(form, result);
        if (!result.hasErrors()) {
            try {
                RecipeResponse created = recipeService.createWithImage(form.toRequest(), form.getImage());
                redirect.addFlashAttribute("successMessage", "Recipe \"" + created.title() + "\" was published.");
                return "redirect:/recipes/" + created.id();
            } catch (InvalidFileException e) {
                result.rejectValue("image", "invalid.image", e.getMessage());
            } catch (ResourceNotFoundException | DuplicateResourceException e) {
                result.reject("invalid.reference", e.getMessage());
            }
        }
        addFormLookups(model);
        return "recipe/form";
    }

    // ---------- edit ----------

    @GetMapping("/recipes/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        RecipeResponse recipe = recipeService.findById(id);
        model.addAttribute("recipe", recipe);
        model.addAttribute("recipeForm", RecipeForm.from(recipe));
        addFormLookups(model);
        return "recipe/form";
    }

    @PostMapping("/recipes/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("recipeForm") RecipeForm form,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        RecipeResponse existing = recipeService.findById(id); // 404 page if it vanished
        checkTags(form, result);
        if (!result.hasErrors()) {
            try {
                // Photo first: an invalid file is rejected before anything else is changed.
                if (form.getImage() != null && !form.getImage().isEmpty()) {
                    recipeService.uploadImage(id, form.getImage());
                }
                RecipeResponse updated = recipeService.update(id, form.toRequest());
                redirect.addFlashAttribute("successMessage", "Recipe \"" + updated.title() + "\" was updated.");
                return "redirect:/recipes/" + id;
            } catch (InvalidFileException e) {
                result.rejectValue("image", "invalid.image", e.getMessage());
            } catch (DuplicateResourceException e) {
                result.reject("invalid.reference", e.getMessage());
            } catch (ResourceNotFoundException e) {
                // Chef/category picked in the form no longer exists (the recipe itself was checked above).
                result.reject("invalid.reference", e.getMessage());
            }
        }
        model.addAttribute("recipe", existing);
        addFormLookups(model);
        return "recipe/form";
    }

    // ---------- delete ----------

    @PostMapping("/recipes/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        recipeService.delete(id);
        redirect.addFlashAttribute("successMessage", "Recipe deleted.");
        return "redirect:/recipes";
    }

    @PostMapping("/recipes/{id}/image/delete")
    public String removeImage(@PathVariable Long id, RedirectAttributes redirect) {
        recipeService.removeImage(id);
        redirect.addFlashAttribute("successMessage", "Photo removed.");
        return "redirect:/recipes/" + id + "/edit";
    }

    // ---------- helpers ----------

    private void addFormLookups(Model model) {
        model.addAttribute("chefs", chefService.findAll(PageRequest.of(0, 500, Sort.by("name"))).content());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("difficulties", Difficulty.values());
    }

    private static void checkTags(RecipeForm form, BindingResult result) {
        if (form.hasOverlongTag() && !result.hasFieldErrors("tags")) {
            result.rejectValue("tags", "tags.length",
                    "Each tag must be at most " + RecipeForm.MAX_TAG_LENGTH + " characters");
        }
    }
}
