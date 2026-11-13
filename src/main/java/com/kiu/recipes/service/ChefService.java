package com.kiu.recipes.service;

import com.kiu.recipes.dto.ChefRequest;
import com.kiu.recipes.dto.ChefResponse;
import com.kiu.recipes.dto.ChefStatsResponse;
import com.kiu.recipes.dto.PageResponse;
import com.kiu.recipes.entity.Chef;
import com.kiu.recipes.exception.DuplicateResourceException;
import com.kiu.recipes.exception.ResourceInUseException;
import com.kiu.recipes.exception.ResourceNotFoundException;
import com.kiu.recipes.repository.ChefRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChefService {

    private final ChefRepository chefRepository;

    public ChefService(ChefRepository chefRepository) {
        this.chefRepository = chefRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ChefResponse> findAll(Pageable pageable) {
        return PageResponse.from(chefRepository.findAll(pageable), ChefResponse::from);
    }

    @Transactional(readOnly = true)
    public ChefResponse findById(Long id) {
        return ChefResponse.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<ChefStatsResponse> top(int limit) {
        return chefRepository.findTopChefs(PageRequest.of(0, Math.max(1, Math.min(limit, 50))))
                .stream().map(ChefStatsResponse::from).toList();
    }

    @Transactional
    public ChefResponse create(ChefRequest request) {
        String email = request.email().trim();
        if (chefRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("A chef with email '" + email + "' already exists");
        }
        Chef chef = new Chef(request.name().trim(), email, request.bio());
        return ChefResponse.from(chefRepository.save(chef));
    }

    @Transactional
    public ChefResponse update(Long id, ChefRequest request) {
        Chef chef = getEntity(id);
        String email = request.email().trim();
        chefRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("A chef with email '" + email + "' already exists");
            }
        });
        chef.setName(request.name().trim());
        chef.setEmail(email);
        chef.setBio(request.bio());
        return ChefResponse.from(chef);
    }

    @Transactional
    public void delete(Long id) {
        Chef chef = getEntity(id);
        if (!chef.getRecipes().isEmpty()) {
            throw new ResourceInUseException("Chef " + id + " still has " + chef.getRecipes().size() + " recipe(s)");
        }
        chefRepository.delete(chef);
    }

    private Chef getEntity(Long id) {
        return chefRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Chef", id));
    }
}
