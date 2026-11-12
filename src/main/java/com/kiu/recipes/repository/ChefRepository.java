package com.kiu.recipes.repository;

import com.kiu.recipes.entity.Chef;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChefRepository extends JpaRepository<Chef, Long> {

    // --- method-name queries ---
    Optional<Chef> findByEmail(String email);

    boolean existsByEmail(String email);

    // --- custom @Query ---
    /** Chefs ranked by number of recipes (chefs with no recipes included, count 0). */
    @Query("""
            select c.id as chefId, c.name as name, count(r) as recipeCount
            from Chef c left join c.recipes r
            group by c.id, c.name
            order by count(r) desc, c.name asc
            """)
    List<ChefStats> findTopChefs(Pageable pageable);
}
