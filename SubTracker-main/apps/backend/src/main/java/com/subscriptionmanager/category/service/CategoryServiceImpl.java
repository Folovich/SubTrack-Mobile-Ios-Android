package com.subscriptionmanager.category.service;

import com.subscriptionmanager.category.dto.CategoryResponse;
import com.subscriptionmanager.repository.CategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(category -> new CategoryResponse(category.getId(), category.getName()))
                .toList();
    }
}
