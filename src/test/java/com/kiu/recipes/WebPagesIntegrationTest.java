package com.kiu.recipes;

import com.kiu.recipes.entity.Category;
import com.kiu.recipes.entity.Chef;
import com.kiu.recipes.entity.Recipe;
import com.kiu.recipes.repository.CategoryRepository;
import com.kiu.recipes.repository.ChefRepository;
import com.kiu.recipes.repository.RecipeRepository;
import com.kiu.recipes.repository.TagRepository;
import com.kiu.recipes.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** The server-rendered Thymeleaf pages: they must render, validate forms, and keep the REST API untouched. */
@SpringBootTest
@AutoConfigureMockMvc
class WebPagesIntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4};

    private final MockMvc mvc;
    private final RecipeRepository recipes;
    private final ChefRepository chefs;
    private final CategoryRepository categories;
    private final TagRepository tags;
    private final FileStorageService storage;

    WebPagesIntegrationTest(MockMvc mvc, RecipeRepository recipes, ChefRepository chefs,
                            CategoryRepository categories, TagRepository tags, FileStorageService storage) {
        this.mvc = mvc;
        this.recipes = recipes;
        this.chefs = chefs;
        this.categories = categories;
        this.tags = tags;
        this.storage = storage;
    }

    private long chefId;
    private long categoryId;

    @BeforeEach
    void reset() throws Exception {
        recipes.deleteAll();
        chefs.deleteAll();
        categories.deleteAll();
        tags.deleteAll();
        try (Stream<Path> files = Files.walk(storage.getRoot())) {
            files.filter(Files::isRegularFile).forEach(p -> p.toFile().delete());
        }
        chefId = chefs.save(new Chef("Anna", "anna@test.com", null)).getId();
        categoryId = categories.save(new Category("Dinner")).getId();
    }

    private MockMultipartFile png() {
        return new MockMultipartFile("image", "dish.png", "image/png", PNG);
    }

    /** A valid recipe form submission; pass key/value pairs to override fields (add .file(...) for a photo). */
    private MockMultipartHttpServletRequestBuilder recipeForm(String url, String... overrides) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("title", "Lentil Soup");
        fields.put("instructions", "Simmer 20 min.");
        fields.put("cookTimeMinutes", "30");
        fields.put("servings", "4");
        fields.put("difficulty", "EASY");
        fields.put("chefId", String.valueOf(chefId));
        fields.put("categoryId", String.valueOf(categoryId));
        fields.put("tags", "vegan, quick , vegan");
        for (int i = 0; i < overrides.length; i += 2) {
            fields.put(overrides[i], overrides[i + 1]);
        }
        MockMultipartHttpServletRequestBuilder form = multipart(url);
        fields.forEach(form::param);
        return form;
    }

    // ---------- browse ----------

    @Test
    void homePageRendersRecipeList() throws Exception {
        createRecipeViaForm();

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("recipe/list"))
                .andExpect(content().string(containsString("Lentil Soup")))
                .andExpect(content().string(containsString("/webjars/bootstrap/5.3.3/css/bootstrap.min.css")));
    }

    @Test
    void searchFiltersAndPaginatesByQueryParameters() throws Exception {
        createRecipeViaForm();

        mvc.perform(get("/recipes").param("q", "lentil").param("tag", "VEGAN").param("difficulty", "EASY")
                        .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Lentil Soup")));

        mvc.perform(get("/recipes").param("q", "no-such-dish"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No recipes found")))
                .andExpect(content().string(not(containsString("Lentil Soup"))));

        mvc.perform(get("/recipes").param("maxTime", "10"))
                .andExpect(content().string(containsString("No recipes found")));
    }

    @Test
    void badQueryParametersShowAnErrorPageNotAStackTrace() throws Exception {
        mvc.perform(get("/recipes").param("difficulty", "IMPOSSIBLE"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString("Invalid value for")));

        mvc.perform(get("/recipes").param("sort", "password,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"));
    }

    @Test
    void unknownRecipeShowsHtml404() throws Exception {
        mvc.perform(get("/recipes/999999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString("Recipe with id 999999 not found")));
    }

    // ---------- create / validation ----------

    @Test
    void invalidFormIsRedisplayedWithErrorsAndNothingIsSaved() throws Exception {
        mvc.perform(multipart("/recipes").param("title", "").param("cookTimeMinutes", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("recipe/form"))
                .andExpect(model().attributeHasFieldErrors("recipeForm",
                        "title", "instructions", "cookTimeMinutes", "servings", "chefId", "categoryId"))
                .andExpect(content().string(containsString("Title is required")))
                .andExpect(content().string(containsString("is-invalid")));

        assertThat(recipes.count()).isZero();
    }

    @Test
    void validFormCreatesRecipeWithPhotoAndRedirectsToDetailPage() throws Exception {
        MvcResult result = mvc.perform(recipeForm("/recipes").file(png()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/recipes/*"))
                .andExpect(flash().attributeExists("successMessage"))
                .andReturn();

        Recipe saved = recipes.findAll().get(0);
        assertThat(saved.getImageFileName()).endsWith(".png");
        assertThat(storage.getRoot().resolve(saved.getImageFileName())).exists();

        mvc.perform(get(result.getResponse().getRedirectedUrl()))
                .andExpect(status().isOk())
                .andExpect(view().name("recipe/detail"))
                .andExpect(content().string(containsString("Lentil Soup")))
                .andExpect(content().string(containsString("/uploads/" + saved.getImageFileName())))
                .andExpect(content().string(containsString("#quick")));
    }

    @Test
    void notAnImageIsRejectedOnTheFormWithoutCreatingARecipe() throws Exception {
        MockMultipartFile script = new MockMultipartFile("image", "evil.png", "image/png",
                "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8));

        mvc.perform(recipeForm("/recipes").file(script))
                .andExpect(status().isOk())
                .andExpect(view().name("recipe/form"))
                .andExpect(model().attributeHasFieldErrors("recipeForm", "image"));

        assertThat(recipes.count()).isZero();
    }

    // ---------- edit / delete ----------

    @Test
    void editPrefillsFormAndSavesChanges() throws Exception {
        long id = createRecipeViaForm();

        mvc.perform(get("/recipes/{id}/edit", id))
                .andExpect(status().isOk())
                .andExpect(view().name("recipe/form"))
                .andExpect(content().string(containsString("Lentil Soup")))
                .andExpect(content().string(containsString("quick, vegan")));

        mvc.perform(recipeForm("/recipes/" + id, "title", "Red Lentil Soup", "tags", "vegan"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recipes/" + id));

        assertThat(recipes.findById(id).orElseThrow().getTitle()).isEqualTo("Red Lentil Soup");
    }

    @Test
    void invalidEditKeepsTheOriginalRecipe() throws Exception {
        long id = createRecipeViaForm();

        mvc.perform(recipeForm("/recipes/" + id, "title", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("recipe/form"))
                .andExpect(model().attributeHasFieldErrors("recipeForm", "title"));

        assertThat(recipes.findById(id).orElseThrow().getTitle()).isEqualTo("Lentil Soup");
    }

    @Test
    void deleteRemovesRecipeAndItsPhoto() throws Exception {
        mvc.perform(recipeForm("/recipes").file(png())).andExpect(status().is3xxRedirection());
        Recipe saved = recipes.findAll().get(0);
        Path photo = storage.getRoot().resolve(saved.getImageFileName());
        assertThat(photo).exists();

        mvc.perform(post("/recipes/{id}/delete", saved.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recipes"));

        assertThat(recipes.count()).isZero();
        assertThat(photo).doesNotExist();
    }

    // ---------- chefs & categories ----------

    @Test
    void chefCanBeAddedAndDuplicateEmailIsReportedOnTheForm() throws Exception {
        mvc.perform(post("/chefs").param("name", "Bob").param("email", "bob@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/chefs"));
        assertThat(chefs.existsByEmail("bob@test.com")).isTrue();

        mvc.perform(post("/chefs").param("name", "Bobby").param("email", "bob@test.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("chef/form"))
                .andExpect(model().attributeHasFieldErrors("chefForm", "email"));

        mvc.perform(post("/chefs").param("name", "").param("email", "nope"))
                .andExpect(model().attributeHasFieldErrors("chefForm", "name", "email"));

        mvc.perform(get("/chefs")).andExpect(status().isOk()).andExpect(view().name("chef/list"))
                .andExpect(content().string(containsString("Bob")));
    }

    @Test
    void chefWithRecipesCannotBeDeletedAndUserIsTold() throws Exception {
        createRecipeViaForm();

        mvc.perform(post("/chefs/{id}/delete", chefId))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(chefs.existsById(chefId)).isTrue();

        mvc.perform(get("/chefs/{id}", chefId))
                .andExpect(status().isOk())
                .andExpect(view().name("chef/detail"))
                .andExpect(content().string(containsString("Lentil Soup")));
    }

    @Test
    void categoriesPageListsAddsAndValidates() throws Exception {
        mvc.perform(get("/categories")).andExpect(status().isOk()).andExpect(view().name("category/list"))
                .andExpect(content().string(containsString("Dinner")));

        mvc.perform(post("/categories").param("name", "Dessert"))
                .andExpect(status().is3xxRedirection());
        assertThat(categories.existsByNameIgnoreCase("dessert")).isTrue();

        mvc.perform(post("/categories").param("name", "dessert"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("categoryForm", "name"));

        mvc.perform(post("/categories").param("name", " "))
                .andExpect(model().attributeHasFieldErrors("categoryForm", "name"));
    }

    // ---------- the REST API keeps speaking JSON ----------

    @Test
    void apiErrorsAreStillJsonNotHtml() throws Exception {
        mvc.perform(get("/api/recipes/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/json"));

        mvc.perform(get("/api/recipes/search").param("difficulty", "IMPOSSIBLE"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    // ---------- helpers ----------

    private long createRecipeViaForm() throws Exception {
        mvc.perform(recipeForm("/recipes")).andExpect(status().is3xxRedirection());
        List<Recipe> all = recipes.findAll();
        return all.get(all.size() - 1).getId();
    }
}
