package com.kiu.recipes.repository;

import com.kiu.recipes.entity.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    // --- method-name queries ---
    Optional<Tag> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    // --- custom @Query ---
    /** Tags ranked by how many recipes use them. */
    @Query("""
            select t.id as tagId, t.name as name, count(r) as recipeCount
            from Tag t left join t.recipes r
            group by t.id, t.name
            order by count(r) desc, t.name asc
            """)
    List<TagStats> findPopularTags(Pageable pageable);
}
