// Tạo file mới: config/FirebaseConfig.java
package com.example.iotbackend.config;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            // Đọc trực tiếp file JSON từ thư mục resources
            InputStream serviceAccount = new ClassPathResource("firebase.json").getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // Kiểm tra tránh khởi tạo trùng lặp ứng dụng Firebase
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println(">>> Firebase Admin SDK đã được khởi tạo thành công!");
            }
        } catch (IOException e) {
            System.err.println(">>> Lỗi khởi tạo Firebase Admin SDK: " + e.getMessage());
            e.printStackTrace();
        }
    }
}