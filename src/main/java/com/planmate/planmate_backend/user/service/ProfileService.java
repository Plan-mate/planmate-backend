package com.planmate.planmate_backend.user.service;

import com.planmate.planmate_backend.common.exception.BusinessException;
import com.planmate.planmate_backend.user.repository.UserRepository;
import com.planmate.planmate_backend.user.dto.ProfileDto;
import com.planmate.planmate_backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;

    public ProfileDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "계정을 찾을 수 없습니다."));

        return new ProfileDto(user.getId(), user.getNickname(), user.getProfileImage());
    }

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "계정을 찾을 수 없습니다."));
    }
}
