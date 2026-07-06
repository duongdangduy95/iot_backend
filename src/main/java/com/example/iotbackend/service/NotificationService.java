package com.example.iotbackend.service;

import com.example.iotbackend.entity.Device;
import com.example.iotbackend.entity.Notification;
import com.example.iotbackend.entity.User;
import com.example.iotbackend.entity.UserDevice;
import com.example.iotbackend.entity.UserDeviceToken;
import com.example.iotbackend.repository.NotificationRepository;
import com.example.iotbackend.repository.UserDeviceRepository;
import com.example.iotbackend.repository.UserDeviceTokenRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final UserDeviceTokenRepository userDeviceTokenRepository;

    public void sendNotification(Device device, User actor, String title, String content) {
        List<User> users = userDeviceRepository.findByDeviceId(device.getId())
                .stream()
                .map(UserDevice::getUser)
                .distinct()
                .toList();

        for (User u : users) {
            sendAsync(device, actor, u, title, content);
        }
    }

    @Async
    public void sendAsync(Device device, User actor, User recipient, String title, String content) {
        // 1. Lưu thông báo lịch sử vào DB như cũ
        Notification n = Notification.builder()
                .device(device)
                .actor(actor)
                .recipient(recipient)
                .title(title)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(n);

        // 2. Lấy danh sách Token ứng với ID của người nhận thông qua Java code
        List<UserDeviceToken> tokens = userDeviceTokenRepository.findByUserId(recipient.getId());

        // 3. Gửi thông báo realtime bằng Firebase thay thế MQTT cũ
        for (UserDeviceToken tokenEntity : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(tokenEntity.getFcmToken())
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle(title)
                                .setBody(content)
                                .build())
                        .putData("deviceName", device.getName())
                        .putData("deviceId", device.getId().toString())
                        .build();

                FirebaseMessaging.getInstance().send(message);
            } catch (Exception e) {
                System.err.println("Gửi FCM lỗi cho token " + tokenEntity.getFcmToken() + ": " + e.getMessage());
            }
        }
    }
}