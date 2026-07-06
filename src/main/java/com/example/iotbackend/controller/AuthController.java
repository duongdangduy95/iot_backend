package com.example.iotbackend.controller;

import com.example.iotbackend.dto.*;
import com.example.iotbackend.entity.User;
import com.example.iotbackend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return "Đăng ký thành công, vui lòng check email!";
    }

    @PostMapping("/verify")
    public String verify(@RequestBody VerifyRequest request) {
        return authService.verify(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest request) {
        // Chỉ cần truyền request (chứa fcmToken) vào là đủ
        authService.logout(request);
        return ResponseEntity.ok("Logged out successfully, FCM Token removed.");
    }
    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return "Đã gửi OTP về email!";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }
}