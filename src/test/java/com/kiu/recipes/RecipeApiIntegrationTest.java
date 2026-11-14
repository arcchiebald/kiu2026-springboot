package com.kiu.recipes;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecipeApiIntegrationTest {

    /** Smallest thing the upload validator accepts: the 8-byte PNG signature plus some payload. */
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4};

    private final MockMvc mvc;
    private final ObjectMapper json;
    private final RecipeRepository recipes;
    private final ChefRepository chefs;
    private final CategoryRepository categories;
    private final TagRepository tags;
    private final FileStorageService storage;

    RecipeApiIntegrationTest(MockMvc mvc, ObjectMapper json, RecipeRepository recipes, ChefRepository chefs,
                             CategoryRepository categories, TagRepository tags, FileStorageService storage) {
        this.mvc = mvc;
        this.json = json;
        this.recipes = recipes;
        this.chefs = chefs;
        this.categories = categories;
        this.tags = tags;
        this.storage = storage;
    }

    private long chefId;
    private long categoryId;

    @BeforeEach
    void resetDatabaseAndUploads() throws Exception {
        recipes.deleteAll();
        chefs.deleteAll();
        categories.deleteAll();
        tags.deleteAll();
        try (Stream<Path> files = Files.walk(storage.getRoot())) {
            files.filter(Files::isRegularFile).forEach(p -> p.toFile().delete());
        }
        chefId = createAndGetId("/api/chefs", Map.of("name", "Anna", "email", "anna@test.com"));
        categoryId = createAndGetId("/api/categories", Map.of("name", "Dinner"));
    }

    // ---------- validation ----------

    @Test
    void invalidRecipeBodyReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"cookTimeMinutes\":0,\"servings\":-1,\"chefId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.title").value("title is required"))
                .andExpect(jsonPath("$.fieldErrors.cookTimeMinutes").exists())
                .andExpect(jsonPath("$.fieldErrors.servings").exists())
                .andExpect(jsonPath("$.fieldErrors.difficulty").exists())
                .andExpect(jsonPath("$.fieldErrors.categoryId").exists());
    }

    @Test
    void invalidEmailIsRejected() throws Exception {
        mvc.perform(post("/api/chefs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bob\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").value("email must be a valid address"));
    }

    @Test
    void invalidMultipartFormIsRejected() throws Exception {
        mvc.perform(multipart("/api/recipes/with-image").param("title", "").param("cookTimeMinutes", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.cookTimeMinutes").value("has an invalid value"));
    }

    // ---------- CRUD, relationships, errors ----------

    @Test
    void createRecipeWithTagsThenReadUpdateDelete() throws Exception {
        long id = createRecipe("Lentil Soup", 30, "EASY", "vegan", "Quick");

        mvc.perform(get("/api/recipes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Lentil Soup"))
                .andExpect(jsonPath("$.chefName").value("Anna"))
                .andExpect(jsonPath("$.categoryName").value("Dinner"))
                .andExpect(jsonPath("$.tags", contains("Quick", "vegan")));

        mvc.perform(put("/api/recipes/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(recipeBody("Red Lentil Soup", 25, "MEDIUM", "vegan"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Red Lentil Soup"))
                .andExpect(jsonPath("$.tags", contains("vegan")));

        mvc.perform(delete("/api/recipes/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/recipes/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void unknownReferencesAndDuplicatesAreReportedClearly() throws Exception {
        Map<String, Object> body = recipeBody("Ghost", 10, "EASY");
        body.put("chefId", 99999);
        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Chef with id 99999 not found"));

        mvc.perform(post("/api/chefs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Other Anna\",\"email\":\"anna@test.com\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void chefWithRecipesCannotBeDeleted() throws Exception {
        createRecipe("Soup", 30, "EASY");
        mvc.perform(delete("/api/chefs/{id}", chefId)).andExpect(status().isConflict());
    }

    // ---------- pagination, sorting, custom queries ----------

    @Test
    void listIsPagedAndSortedByRequestedField() throws Exception {
        createRecipe("Pasta", 45, "MEDIUM");
        createRecipe("Salad", 10, "EASY");
        createRecipe("Soup", 30, "EASY");

        mvc.perform(get("/api/recipes").param("page", "0").param("size", "2").param("sort", "cookTimeMinutes,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", contains("Salad", "Soup")))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        mvc.perform(get("/api/recipes").param("page", "1").param("size", "2").param("sort", "cookTimeMinutes,asc"))
                .andExpect(jsonPath("$.content[*].title", contains("Pasta")))
                .andExpect(jsonPath("$.last").value(true));

        mvc.perform(get("/api/recipes").param("sort", "cookTimeMinutes,desc"))
                .andExpect(jsonPath("$.content[0].title").value("Pasta"));
    }

    @Test
    void unknownSortPropertyIs400NotAServerError() throws Exception {
        mvc.perform(get("/api/recipes").param("sort", "doesNotExist,asc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/recipes/search").param("sortBy", "password")).andExpect(status().isBadRequest());
    }

    @Test
    void searchCombinesTagDifficultyAndTimeFilters() throws Exception {
        createRecipe("Lentil Soup", 30, "EASY", "vegan");
        createRecipe("Vegan Stew", 90, "EASY", "vegan");
        createRecipe("Beef Wellington", 120, "HARD");

        mvc.perform(get("/api/recipes/search").param("tag", "VEGAN"))
                .andExpect(jsonPath("$.totalElements").value(2));

        mvc.perform(get("/api/recipes/search").param("tag", "vegan").param("maxTime", "60")
                        .param("sortBy", "title").param("direction", "desc"))
                .andExpect(jsonPath("$.content[*].title", contains("Lentil Soup")));

        mvc.perform(get("/api/recipes/search").param("difficulty", "EASY").param("sortBy", "title").param("direction", "asc"))
                .andExpect(jsonPath("$.content[*].title", contains("Lentil Soup", "Vegan Stew")))
                .andExpect(jsonPath("$.sort").value("title: ASC"));
    }

    @Test
    void topChefsAndPopularTagsEndpoints() throws Exception {
        createRecipe("A", 10, "EASY", "vegan");
        createRecipe("B", 20, "EASY", "vegan", "quick");

        mvc.perform(get("/api/chefs/top"))
                .andExpect(jsonPath("$[0].name").value("Anna"))
                .andExpect(jsonPath("$[0].recipeCount").value(2));
        mvc.perform(get("/api/tags/popular"))
                .andExpect(jsonPath("$[0].name").value("vegan"))
                .andExpect(jsonPath("$[0].recipeCount").value(2));
    }

    @Test
    void categoryRecipesUsesMethodNameQueryWithMaxTime() throws Exception {
        createRecipe("Fast", 10, "EASY");
        createRecipe("Slow", 100, "EASY");
        mvc.perform(get("/api/categories/{id}/recipes", categoryId).param("maxTime", "30"))
                .andExpect(jsonPath("$.content[*].title", contains("Fast")));
    }

    // ---------- file upload ----------

    @Test
    void uploadStoresFileOnDiskAndSavesNameInDatabase() throws Exception {
        long id = createRecipe("Soup", 30, "EASY");

        MvcResult result = mvc.perform(multipart("/api/recipes/{id}/image", id)
                        .file(new MockMultipartFile("file", "my dish.png", "image/png", PNG)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl", startsWith("/uploads/")))
                .andExpect(jsonPath("$.imageUrl", endsWith(".png")))
                .andReturn();

        String url = json.readTree(result.getResponse().getContentAsString()).get("imageUrl").asText();
        String storedName = url.substring("/uploads/".length());

        // reflected in the database ...
        Recipe saved = recipes.findById(id).orElseThrow();
        assertThat(saved.getImageFileName()).isEqualTo(storedName);
        // ... physically stored, under a server-generated name (the client's "my dish.png" is not used)
        assertThat(storedName).doesNotContain("dish");
        assertThat(Files.readAllBytes(storage.getRoot().resolve(storedName))).isEqualTo(PNG);

        mvc.perform(get(url)).andExpect(status().isOk());
    }

    @Test
    void replacingAnImageRemovesTheOldFile() throws Exception {
        long id = createRecipe("Soup", 30, "EASY");
        String first = uploadImage(id);
        String second = uploadImage(id);

        assertThat(second).isNotEqualTo(first);
        assertThat(storage.getRoot().resolve(first)).doesNotExist();
        assertThat(storage.getRoot().resolve(second)).exists();
    }

    @Test
    void deletingARecipeRemovesItsImageFile() throws Exception {
        long id = createRecipe("Soup", 30, "EASY");
        String stored = uploadImage(id);

        mvc.perform(delete("/api/recipes/{id}", id)).andExpect(status().isNoContent());
        assertThat(storage.getRoot().resolve(stored)).doesNotExist();
    }

    @Test
    void nonImageAndEmptyUploadsAreRejected() throws Exception {
        long id = createRecipe("Soup", 30, "EASY");
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes(StandardCharsets.UTF_8);

        // content type and extension claim "png", but the bytes are not
        mvc.perform(multipart("/api/recipes/{id}/image", id).file(new MockMultipartFile("file", "x.png", "image/png", svg)))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/recipes/{id}/image", id).file(new MockMultipartFile("file", "x.png", "image/png", new byte[0])))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/recipes/{id}/image", id)).andExpect(status().isBadRequest());

        assertThat(recipes.findById(id).orElseThrow().getImageFileName()).isNull();
        assertThat(listUploadedFiles()).isEmpty();
    }

    @Test
    void multipartFormCreatesRecipeWithImageUsingModelAttribute() throws Exception {
        mvc.perform(multipart("/api/recipes/with-image")
                        .file(new MockMultipartFile("file", "p.png", "image/png", PNG))
                        .param("title", "Stir Fry").param("instructions", "Fry.")
                        .param("cookTimeMinutes", "15").param("servings", "2").param("difficulty", "EASY")
                        .param("chefId", String.valueOf(chefId)).param("categoryId", String.valueOf(categoryId))
                        .param("tagNames", "vegan", "wok"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Stir Fry"))
                .andExpect(jsonPath("$.imageUrl", startsWith("/uploads/")))
                .andExpect(jsonPath("$.tags", containsInAnyOrder("vegan", "wok")));
        assertThat(listUploadedFiles()).hasSize(1);
    }

    // ---------- helpers ----------

    private long createAndGetId(String url, Map<String, Object> body) throws Exception {
        MvcResult r = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createRecipe(String title, int minutes, String difficulty, String... tagNames) throws Exception {
        return createAndGetId("/api/recipes", recipeBody(title, minutes, difficulty, tagNames));
    }

    private Map<String, Object> recipeBody(String title, int minutes, String difficulty, String... tagNames) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("title", title);
        body.put("instructions", "Cook it.");
        body.put("cookTimeMinutes", minutes);
        body.put("servings", 2);
        body.put("difficulty", difficulty);
        body.put("chefId", chefId);
        body.put("categoryId", categoryId);
        body.put("tagNames", List.of(tagNames));
        return body;
    }

    private String uploadImage(long recipeId) throws Exception {
        MvcResult r = mvc.perform(multipart("/api/recipes/{id}/image", recipeId)
                        .file(new MockMultipartFile("file", "a.png", "image/png", PNG)))
                .andExpect(status().isOk()).andReturn();
        String url = json.readTree(r.getResponse().getContentAsString()).get("imageUrl").asText();
        return url.substring("/uploads/".length());
    }

    private List<Path> listUploadedFiles() throws IOException {
        try (Stream<Path> files = Files.walk(storage.getRoot())) {
            return files.filter(Files::isRegularFile).sorted(Comparator.naturalOrder()).toList();
        }
    }
}
