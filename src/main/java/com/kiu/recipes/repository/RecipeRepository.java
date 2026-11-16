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
    /**
     * Combined search: every filter is optional (null = ignore); title is a case-insensitive substring match. Joins the many-to-many
     * tags table, so DISTINCT plus an explicit count query keep paging correct.
     */
    @Query(value = """
            select distinct r from Recipe r left join r.tags t
            where (:tag is null or lower(t.name) = lower(:tag))
              and (:title is null or lower(r.title) like lower(concat('%', :title, '%')))
              and (:categoryId is null or r.category.id = :categoryId)
              and (:difficulty is null or r.difficulty = :difficulty)
              and r.cookTimeMinutes between :minTime and :maxTime
            """,
            countQuery = """
                    select count(distinct r) from Recipe r left join r.tags t
                    where (:tag is null or lower(t.name) = lower(:tag))
              and (:title is null or lower(r.title) like lower(concat('%', :title, '%')))
                      and (:categoryId is null or r.category.id = :categoryId)
                      and (:difficulty is null or r.difficulty = :difficulty)
                      and r.cookTimeMinutes between :minTime and :maxTime
                    """)
    Page<Recipe> search(@Param("title") String title,
                        @Param("tag") String tag,
                        @Param("categoryId") Long categoryId,
                        @Param("difficulty") Difficulty difficulty,
                        @Param("minTime") int minTime,
                        @Param("maxTime") int maxTime,
                        Pageable pageable);
}
