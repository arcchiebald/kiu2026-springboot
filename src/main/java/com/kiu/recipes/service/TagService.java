package com.kiu.recipes.service;

import com.kiu.recipes.dto.TagRequest;
import com.kiu.recipes.dto.TagResponse;
import com.kiu.recipes.dto.TagStatsResponse;
import com.kiu.recipes.entity.Tag;
import com.kiu.recipes.exception.DuplicateResourceException;
import com.kiu.recipes.exception.ResourceInUseException;
import com.kiu.recipes.exception.ResourceNotFoundException;
import com.kiu.recipes.repository.TagRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> findAll() {
        return tagRepository.findAll(Sort.by("name")).stream().map(TagResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TagResponse findById(Long id) {
        return TagResponse.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<TagStatsResponse> popular(int limit) {
        return tagRepository.findPopularTags(PageRequest.of(0, Math.max(1, Math.min(limit, 50))))
                .stream().map(TagStatsResponse::from).toList();
    }

    @Transactional
    public TagResponse create(TagRequest request) {
        String name = request.name().trim();
        if (tagRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Tag '" + name + "' already exists");
        }
        return TagResponse.from(tagRepository.save(new Tag(name)));
    }

    @Transactional
    public TagResponse update(Long id, TagRequest request) {
        Tag tag = getEntity(id);
        String name = request.name().trim();
        tagRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("Tag '" + name + "' already exists");
            }
        });
        tag.setName(name);
        return TagResponse.from(tag);
    }

    @Transactional
    public void delete(Long id) {
        Tag tag = getEntity(id);
        if (!tag.getRecipes().isEmpty()) {
            throw new ResourceInUseException("Tag " + id + " is used by " + tag.getRecipes().size() + " recipe(s)");
        }
        tagRepository.delete(tag);
    }

    /** Resolves tag names to entities, creating the ones that do not exist yet. */
    @Transactional
    public Set<Tag> resolveTags(Set<String> names) {
        Set<Tag> result = new HashSet<>();
        if (names == null) {
            return result;
        }
        for (String raw : names) {
            String name = raw.trim();
            Tag tag = tagRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> tagRepository.save(new Tag(name)));
            result.add(tag);
        }
        return result;
    }

    private Tag getEntity(Long id) {
        return tagRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tag", id));
    }
}
