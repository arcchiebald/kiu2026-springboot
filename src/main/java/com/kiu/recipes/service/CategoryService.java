package com.kiu.recipes.service;

import com.kiu.recipes.dto.CategoryRequest;
import com.kiu.recipes.dto.CategoryResponse;
import com.kiu.recipes.entity.Category;
import com.kiu.recipes.exception.DuplicateResourceException;
import com.kiu.recipes.exception.ResourceInUseException;
import com.kiu.recipes.exception.ResourceNotFoundException;
import com.kiu.recipes.repository.CategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll(Sort.by("name")).stream().map(CategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return CategoryResponse.from(getEntity(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Category '" + name + "' already exists");
        }
        return CategoryResponse.from(categoryRepository.save(new Category(name)));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getEntity(id);
        String name = request.name().trim();
        categoryRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("Category '" + name + "' already exists");
            }
        });
        category.setName(name);
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = getEntity(id);
        if (!category.getRecipes().isEmpty()) {
            throw new ResourceInUseException(
                    "Category " + id + " still has " + category.getRecipes().size() + " recipe(s)");
        }
        categoryRepository.delete(category);
    }

    private Category getEntity(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }
}
