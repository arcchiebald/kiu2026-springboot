package com.kiu.recipes.repository;

/** Projection for the "top chefs" query. */
public interface ChefStats {

    Long getChefId();

    String getName();

    Long getRecipeCount();
}
