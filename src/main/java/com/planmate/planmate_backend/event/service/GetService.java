package com.planmate.planmate_backend.event.service;

import com.planmate.planmate_backend.common.exception.BusinessException;
import com.planmate.planmate_backend.event.entity.Category;
import com.planmate.planmate_backend.event.repository.CategoryRepository;
import com.planmate.planmate_backend.event.dto.CategoryDto;
import com.planmate.planmate_backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetService {

    private final CategoryRepository categoryRepository;

    public List<CategoryDto> getCategories() {
        List<Category> categories = categoryRepository.findAll();

        return categories.stream()
                .map(category -> new CategoryDto(
                        category.getId(),
                        category.getName(),
                        category.getColor()
                ))
                .toList();
    }

}
