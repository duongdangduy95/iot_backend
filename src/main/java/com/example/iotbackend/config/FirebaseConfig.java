package com.example.iotbackend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {

            InputStream serviceAccount;

            // Kiểm tra biến môi trường FIREBASE_CONFIG
            String firebaseConfig = System.getenv("FIREBASE_CONFIG");

            if (firebaseConfig != null && !firebaseConfig.isBlank()) {
                // Render: đọc từ Environment Variable
                serviceAccount = new ByteArrayInputStream(
                        firebaseConfig.getBytes(StandardCharsets.UTF_8)
                );
                System.out.println(">>> Đang sử dụng FIREBASE_CONFIG từ Environment Variable");
            } else {
                // Local: đọc từ file trong resources
                serviceAccount = new ClassPathResource("firebase.json").getInputStream();
                System.out.println(">>> Đang sử dụng firebase.json trong resources");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

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