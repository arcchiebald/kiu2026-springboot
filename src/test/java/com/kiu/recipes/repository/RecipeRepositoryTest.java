package com.kiu.recipes.repository;

import com.kiu.recipes.entity.Category;
import com.kiu.recipes.entity.Chef;
import com.kiu.recipes.entity.Difficulty;
import com.kiu.recipes.entity.Recipe;
import com.kiu.recipes.entity.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RecipeRepositoryTest {

    private final RecipeRepository recipes;
    private final ChefRepository chefs;
    private final CategoryRepository categories;
    private final TagRepository tags;

    RecipeRepositoryTest(RecipeRepository recipes, ChefRepository chefs,
                         CategoryRepository categories, TagRepository tags) {
        this.recipes = recipes;
        this.chefs = chefs;
        this.categories = categories;
        this.tags = tags;
    }

    Chef anna;
    Chef bob;
    Category dinner;
    Tag vegan;

    @BeforeEach
    void setUp() {
        anna = chefs.save(new Chef("Anna", "anna@test.com", null));
        bob = chefs.save(new Chef("Bob", "bob@test.com", null));
        dinner = categories.save(new Category("Dinner"));
        vegan = tags.save(new Tag("vegan"));
        Tag quick = tags.save(new Tag("quick"));

        recipes.save(recipe("Lentil Soup", 30, Difficulty.EASY, anna, Set.of(vegan, quick)));
        recipes.save(recipe("Beef Wellington", 120, Difficulty.HARD, anna, Set.of()));
        recipes.save(recipe("Veggie Stir Fry", 15, Difficulty.EASY, bob, Set.of(vegan)));
    }

    private Recipe recipe(String title, int minutes, Difficulty d, Chef chef, Set<Tag> tagSet) {
        Recipe r = new Recipe();
        r.setTitle(title);
        r.setInstructions("Cook it.");
        r.setCookTimeMinutes(minutes);
        r.setServings(2);
        r.setDifficulty(d);
        r.setChef(chef);
        r.setCategory(dinner);
        r.setTags(new java.util.HashSet<>(tagSet));
        return r;
    }

    @Test
    void methodNameQuery_categoryAndMaxTime() {
        Page<Recipe> page = recipes.findByCategoryIdAndCookTimeMinutesLessThanEqual(
                dinner.getId(), 30, PageRequest.of(0, 10, Sort.by("cookTimeMinutes")));
        assertThat(page.getContent()).extracting(Recipe::getTitle)
                .containsExactly("Veggie Stir Fry", "Lentil Soup");
    }

    @Test
    void methodNameQuery_titleContainingIgnoreCase() {
        Page<Recipe> page = recipes.findByTitleContainingIgnoreCase("SOUP", Pageable.unpaged());
        assertThat(page.getContent()).extracting(Recipe::getTitle).containsExactly("Lentil Soup");
    }

    @Test
    void methodNameQuery_byChef() {
        assertThat(recipes.findByChefIdOrderByCreatedAtDesc(anna.getId())).hasSize(2);
    }

    @Test
    void query_search_byTagIsCaseInsensitiveAndDistinct() {
        Page<Recipe> page = recipes.search(null, "VEGAN", null, null, 0, 1440, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Recipe::getTitle)
                .containsExactlyInAnyOrder("Lentil Soup", "Veggie Stir Fry");
    }

    @Test
    void query_search_combinesFilters() {
        Page<Recipe> easyVegan = recipes.search(null, "vegan", dinner.getId(), Difficulty.EASY, 0, 20, PageRequest.of(0, 10));
        assertThat(easyVegan.getContent()).extracting(Recipe::getTitle).containsExactly("Veggie Stir Fry");
    }

    @Test
    void query_search_nullFiltersMeanAny() {
        Page<Recipe> all = recipes.search(null, null, null, null, 0, 1440, PageRequest.of(0, 2, Sort.by("title")));
        assertThat(all.getTotalElements()).isEqualTo(3);
        assertThat(all.getContent()).hasSize(2);
        assertThat(all.getTotalPages()).isEqualTo(2);
    }

    @Test
    void query_topChefsAndPopularTags() {
        List<ChefStats> top = chefs.findTopChefs(PageRequest.of(0, 5));
        assertThat(top.get(0).getName()).isEqualTo("Anna");
        assertThat(top.get(0).getRecipeCount()).isEqualTo(2);

        List<TagStats> popular = tags.findPopularTags(PageRequest.of(0, 5));
        assertThat(popular.get(0).getName()).isEqualTo("vegan");
        assertThat(popular.get(0).getRecipeCount()).isEqualTo(2);
    }
}
