package com.planmate.planmate_backend.event.repository;

import com.planmate.planmate_backend.event.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

}