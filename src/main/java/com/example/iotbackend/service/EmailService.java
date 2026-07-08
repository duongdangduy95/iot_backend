package com.example.iotbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    @Async
    public void send(String to, String subject, String content) {
        try {
            // 1. Tạo Header theo định dạng Brevo API yêu cầu
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);
            headers.set("accept", "application/json");

            // 2. Tạo Request Body (Sử dụng Map để đóng gói JSON nhanh, đỡ phải tạo thêm class DTO)
            Map<String, Object> body = Map.of(
                    "sender", Map.of("email", senderEmail),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "htmlContent", content // Sử dụng htmlContent để bạn có thể truyền cả text thuần lẫn giao diện HTML
            );

            // 3. Đóng gói request và gửi đi bằng HTTP POST
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_API_URL, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Gửi mail qua Brevo API thành công!");
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi mail qua API: " + e.getMessage());
        }
    }
}