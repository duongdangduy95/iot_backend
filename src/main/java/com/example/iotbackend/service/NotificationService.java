package com.example.iotbackend.service;

import com.example.iotbackend.entity.Notification;
import com.example.iotbackend.entity.User;
import com.example.iotbackend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MqttService mqttService;
     @Async
    public void sendNotification(
            User recipient,
            User actor,
            String title,
            String content
    ) {

        Notification notification =
                Notification.builder()
                        .recipient(recipient)
                        .actor(actor)
                        .title(title)
                        .content(content)
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build();

        notificationRepository.save(notification);

        mqttService.publish(
                "users/" + recipient.getId() + "/notifications",
                """
                {
                    "title":"%s",
                    "content":"%s"
                }
                """.formatted(title, content)
        );
    }
}