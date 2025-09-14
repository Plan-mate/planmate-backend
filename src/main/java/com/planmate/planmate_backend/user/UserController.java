package com.planmate.planmate_backend.user;

import com.planmate.planmate_backend.user.dto.DailyLoginResDto;
import com.planmate.planmate_backend.user.service.DailyLoginService;
import com.planmate.planmate_backend.user.service.ProfileService;
import com.planmate.planmate_backend.user.dto.ProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;


@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final ProfileService profileService;
    private final DailyLoginService dailyLoginService;

    @GetMapping("/me")
    public ProfileDto getProfile(@AuthenticationPrincipal Long userId) {
        return profileService.getProfile(userId);
    }

    @GetMapping("/check-daily-login")
    public DailyLoginResDto checkDailyLogin(@AuthenticationPrincipal Long userId) {
        return dailyLoginService.checkDailyLogin(userId);
    }
}