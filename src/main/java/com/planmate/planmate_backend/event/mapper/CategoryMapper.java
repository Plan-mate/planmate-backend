package com.planmate.planmate_backend.event.mapper;

import com.planmate.planmate_backend.event.dto.CategoryDto;
import com.planmate.planmate_backend.event.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {
    public CategoryDto toDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getColor()
        );
    }
}
