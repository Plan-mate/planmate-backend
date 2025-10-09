package com.planmate.planmate_backend.user;

import com.planmate.planmate_backend.user.dto.DailyLoginResDto;
import com.planmate.planmate_backend.user.dto.FcmTokenDto;
import com.planmate.planmate_backend.user.service.DailyLoginService;
import com.planmate.planmate_backend.user.service.FcmTokenService;
import com.planmate.planmate_backend.user.service.LogoutService;
import com.planmate.planmate_backend.user.service.ProfileService;
import com.planmate.planmate_backend.user.dto.ProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;


@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final ProfileService profileService;
    private final DailyLoginService dailyLoginService;
    private final FcmTokenService fcmTokenService;
    private final LogoutService logoutService;

    @GetMapping("/me")
    public ProfileDto getProfile(@AuthenticationPrincipal Long userId) {
        return profileService.getProfile(userId);
    }

    @GetMapping("/check-daily-login")
    public DailyLoginResDto checkDailyLogin(@AuthenticationPrincipal Long userId) {
        return dailyLoginService.checkDailyLogin(userId);
    }

    @PostMapping("/fcm-token")
    public String updateFcmToken(@AuthenticationPrincipal Long userId, @RequestBody FcmTokenDto dto) {
        return fcmTokenService.updateFcmToken(userId, dto);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Long userId) {
        return logoutService.logout(userId);
    }
}