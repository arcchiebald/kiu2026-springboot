package com.kiu.recipes.repository;

/** Projection for the "popular tags" query. */
public interface TagStats {

    Long getTagId();

    String getName();

    Long getRecipeCount();
}
