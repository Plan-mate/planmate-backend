package com.planmate.planmate_backend.common.config;

import com.planmate.planmate_backend.event.entity.Category;
import com.planmate.planmate_backend.event.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CategoryInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() > 0) {
            return;
        }

        List<Category> categories = List.of(
                Category.builder().name("업무").color("#FF6B6B").build(),
                Category.builder().name("회의").color("#4ECDC4").build(),
                Category.builder().name("개인").color("#FFD93D").build(),
                Category.builder().name("운동").color("#1A535C").build(),
                Category.builder().name("공부").color("#FF6B6B").build(),
                Category.builder().name("취미").color("#6A4C93").build(),
                Category.builder().name("쇼핑").color("#F7B32B").build(),
                Category.builder().name("여행").color("#4CAF50").build(),
                Category.builder().name("생일").color("#FF6F91").build(),
                Category.builder().name("기타").color("#9E9E9E").build()
        );

        categoryRepository.saveAll(categories);
        System.out.println("카테고리 초기화 완료!");
    }
}
