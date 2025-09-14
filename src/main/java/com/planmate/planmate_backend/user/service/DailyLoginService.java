package com.planmate.planmate_backend.user.service;

import com.planmate.planmate_backend.user.dto.DailyLoginResDto;
import com.planmate.planmate_backend.user.entity.User;
import com.planmate.planmate_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DailyLoginService {

    private final ProfileService profileService;
    private final UserRepository userRepository;

    @Transactional
    public DailyLoginResDto checkDailyLogin(Long userId) {
        User user = profileService.getUser(userId);

        LocalDateTime lastLoginAt = user.getLastLoginAt();
        LocalDate today = LocalDate.now();

        if (lastLoginAt == null || !lastLoginAt.toLocalDate().equals(today)) {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            return DailyLoginResDto.builder()
                    .isFirstLoginToday(true)
                    .build();
        }

        return DailyLoginResDto.builder().isFirstLoginToday(false).build();
    }
}
