package com.kiu.recipes.config;

import com.kiu.recipes.dto.CategoryRequest;
import com.kiu.recipes.dto.CategoryResponse;
import com.kiu.recipes.dto.ChefRequest;
import com.kiu.recipes.dto.ChefResponse;
import com.kiu.recipes.dto.RecipeRequest;
import com.kiu.recipes.entity.Difficulty;
import com.kiu.recipes.repository.ChefRepository;
import com.kiu.recipes.service.CategoryService;
import com.kiu.recipes.service.ChefService;
import com.kiu.recipes.service.RecipeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/** Inserts demo data on first start. Goes through the services, so it uses the same validation and transactions as the API. */
@Configuration
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true")
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner seedDatabase(ChefRepository chefRepository,
                                   ChefService chefService,
                                   CategoryService categoryService,
                                   RecipeService recipeService) {
        return args -> {
            if (chefRepository.count() > 0) {
                return;
            }
            ChefResponse nino = chefService.create(new ChefRequest("Nino Beridze", "nino@recipes.test", "Home cook from Tbilisi, loves Georgian classics."));
            ChefResponse luka = chefService.create(new ChefRequest("Luka Gelashvili", "luka@recipes.test", "Weeknight dinners in 30 minutes or less."));
            ChefResponse mari = chefService.create(new ChefRequest("Mariam Kapanadze", "mari@recipes.test", "Pastry and baking."));

            CategoryResponse mains = categoryService.create(new CategoryRequest("Main course"));
            CategoryResponse soups = categoryService.create(new CategoryRequest("Soups"));
            CategoryResponse desserts = categoryService.create(new CategoryRequest("Desserts"));
            CategoryResponse salads = categoryService.create(new CategoryRequest("Salads"));

            recipeService.create(recipe("Khachapuri Adjaruli", "Boat-shaped cheese bread with a runny egg.",
                    "Make the dough, shape boats, fill with sulguni and imeruli cheese, bake at 230C, top with egg and butter.",
                    75, 4, Difficulty.MEDIUM, nino.id(), mains.id(), "georgian", "vegetarian", "baking"));
            recipeService.create(recipe("Lobio", "Slow-cooked kidney beans with walnuts and herbs.",
                    "Soak beans overnight, simmer until soft, mash lightly, add fried onion, walnuts, coriander and spices.",
                    90, 4, Difficulty.EASY, nino.id(), mains.id(), "georgian", "vegan"));
            recipeService.create(recipe("Chicken Kharcho", "Beef-and-walnut soup in the Georgian style, here with chicken.",
                    "Simmer chicken, add tkemali, ground walnuts, garlic and khmeli suneli; finish with fresh coriander.",
                    60, 6, Difficulty.MEDIUM, nino.id(), soups.id(), "georgian", "spicy"));
            recipeService.create(recipe("Lentil Soup", "Warming red lentil soup.",
                    "Saute onion and carrot, add lentils and stock, simmer 20 minutes, blend, finish with lemon.",
                    30, 4, Difficulty.EASY, luka.id(), soups.id(), "vegan", "quick", "healthy"));
            recipeService.create(recipe("Garlic Butter Pasta", "Fast pasta for busy evenings.",
                    "Boil pasta, fry garlic in butter, toss together with parsley and parmesan.",
                    20, 2, Difficulty.EASY, luka.id(), mains.id(), "quick", "vegetarian"));
            recipeService.create(recipe("Greek Salad", "Crisp vegetables, feta and olives.",
                    "Chop tomatoes, cucumber, onion and pepper; add olives and feta; dress with olive oil and oregano.",
                    10, 2, Difficulty.EASY, luka.id(), salads.id(), "quick", "vegetarian", "healthy"));
            recipeService.create(recipe("Chocolate Lava Cake", "Molten-centre individual cakes.",
                    "Melt chocolate with butter, whisk in eggs, sugar and flour, bake in ramekins for 12 minutes.",
                    25, 4, Difficulty.HARD, mari.id(), desserts.id(), "baking", "chocolate"));
            recipeService.create(recipe("Vegan Banana Bread", "Moist banana bread without eggs or dairy.",
                    "Mash bananas, mix with oil, sugar, flour and baking soda, bake at 180C for 50 minutes.",
                    65, 8, Difficulty.EASY, mari.id(), desserts.id(), "vegan", "baking"));
            log.info("Seeded demo data: 3 chefs, 4 categories, 8 recipes");
        };
    }

    private RecipeRequest recipe(String title, String description, String instructions, int minutes, int servings,
                                 Difficulty difficulty, Long chefId, Long categoryId, String... tags) {
        return new RecipeRequest(title, description, instructions, minutes, servings, difficulty, chefId, categoryId, Set.of(tags));
    }
}
