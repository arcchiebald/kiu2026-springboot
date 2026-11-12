package com.kiu.recipes.repository;

import com.kiu.recipes.entity.Difficulty;
import com.kiu.recipes.entity.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    // --- method-name queries ---
    Page<Recipe> findByCategoryIdAndCookTimeMinutesLessThanEqual(Long categoryId, int maxCookTime, Pageable pageable);

    Page<Recipe> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    List<Recipe> findByChefIdOrderByCreatedAtDesc(Long chefId);

    // --- custom @Query ---
    /** Recipes carrying a given tag (many-to-many join). */
    @Query(value = """
            select distinct r from Recipe r join r.tags t
            where lower(t.name) = lower(:tag)
            """,
            countQuery = """
                    select count(distinct r) from Recipe r join r.tags t
                    where lower(t.name) = lower(:tag)
                    """)
    Page<Recipe> findByTagName(@Param("tag") String tag, Pageable pageable);

    /** Optional difficulty plus a cook-time range. A null difficulty means "any". */
    @Query("""
            select r from Recipe r
            where (:difficulty is null or r.difficulty = :difficulty)
              and r.cookTimeMinutes between :minTime and :maxTime
            """)
    Page<Recipe> filterByDifficultyAndTime(@Param("difficulty") Difficulty difficulty,
                                           @Param("minTime") int minTime,
                                           @Param("maxTime") int maxTime,
                                           Pageable pageable);
}
