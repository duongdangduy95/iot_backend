package com.example.iotbackend.service;

import com.example.iotbackend.dto.*;
import com.example.iotbackend.entity.*;
import com.example.iotbackend.repository.*;
import com.example.iotbackend.service.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final UserDeviceTokenRepository userDeviceTokenRepository;

    public void register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUsername(request.getUsername());
        user.setVerified(false);

        userRepository.save(user);

        String otp = String.valueOf((int)(Math.random() * 900000) + 100000);

        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setOtp(otp);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(LocalDateTime.now().plusMinutes(5));

        tokenRepository.save(verificationToken);

        emailService.send(request.getEmail(), "Mã xác thực", "OTP của bạn là: " + otp);
    }

    public String verify(VerifyRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        VerificationToken vt = tokenRepository.findByOtp(request.getOtp())
                .orElseThrow(() -> new RuntimeException("OTP không đúng"));

        if (!vt.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("OTP không thuộc user");
        }

        if (vt.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP hết hạn");
        }

        user.setVerified(true);
        userRepository.save(user);

        return "Xác thực thành công!";
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (!user.getVerified()) {
            throw new RuntimeException("Chưa xác thực email");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Sai mật khẩu");
        }

        // --- XỬ LÝ LƯU HOẶC CẬP NHẬT FCM TOKEN ---
        if (request.getFcmToken() != null && !request.getFcmToken().isBlank()) {
            // Nếu token này đã tồn tại ở thiết bị này trước đó (của chính user này hoặc user khác từng login máy này)
            UserDeviceToken deviceToken = userDeviceTokenRepository
                    .findByFcmToken(request.getFcmToken())
                    .orElse(new UserDeviceToken());

            deviceToken.setUserId(user.getId()); // Gán id user đăng nhập vào
            deviceToken.setFcmToken(request.getFcmToken());

            userDeviceTokenRepository.save(deviceToken);
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token);
    }

    // --- HÀM LOGOUT CHỈ CẦN TRUYỀN LÊN FCM TOKEN ĐỂ XÓA ---
    public void logout(LogoutRequest request) {
        if (request.getFcmToken() != null && !request.getFcmToken().isBlank()) {
            userDeviceTokenRepository.deleteByFcmToken(request.getFcmToken());
        }
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));

        String otp = String.valueOf((int)(Math.random() * 900000) + 100000);

        VerificationToken token = new VerificationToken();
        token.setOtp(otp);
        token.setUser(user);
        token.setType(VerificationToken.TokenType.RESET_PASSWORD);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(5));

        tokenRepository.save(token);

        emailService.send(user.getEmail(), "Reset mật khẩu", "OTP reset mật khẩu: " + otp);
    }

    public String resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        VerificationToken token = tokenRepository
                .findByOtpAndType(request.getOtp(), VerificationToken.TokenType.RESET_PASSWORD)
                .orElseThrow(() -> new RuntimeException("OTP không đúng"));

        if (!token.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("OTP không thuộc user");
        }

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP hết hạn");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        tokenRepository.delete(token);

        return "Đổi mật khẩu thành công!";
    }
}