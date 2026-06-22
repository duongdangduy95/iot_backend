package com.example.iotbackend.service;

import com.example.iotbackend.entity.Device;
import com.example.iotbackend.entity.Notification;
import com.example.iotbackend.entity.User;
import com.example.iotbackend.entity.UserDevice;
import com.example.iotbackend.repository.NotificationRepository;
import com.example.iotbackend.repository.UserDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MqttService mqttService;
    private final UserDeviceRepository userDeviceRepository;
     @Async
     public void sendNotification(Device device, User actor, String title, String content) {

         List<User> users = userDeviceRepository
                 .findByDeviceId(device.getId())
                 .stream()
                 .map(UserDevice::getUser)
                 .toList();

         for (User u : users) {

             Notification n = Notification.builder()
                     .device(device)   // 🔥 GẮN DEVICE
                     .actor(actor)
                     .recipient(u)
                     .title(title)
                     .content(content)
                     .createdAt(LocalDateTime.now())
                     .build();

             notificationRepository.save(n);

             mqttService.publish(
                     "users/" + u.getId() + "/notifications",
                     """
                     {
                         "title":"%s",
                         "content":"%s",
                         "deviceName":"%s"
                     }
                     """.formatted(title, content, device.getName())
             );
         }
     }
}