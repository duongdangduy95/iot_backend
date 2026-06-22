package com.example.iotbackend.controller;

import com.example.iotbackend.entity.Notification;
import com.example.iotbackend.entity.User;
import com.example.iotbackend.repository.NotificationRepository;
import com.example.iotbackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository repository;
    private final SecurityUtils securityUtils;

    @GetMapping
    public List<Notification> getMyNotifications() {

        User userId = securityUtils.getCurrentUser();
        return repository.findByRecipientIdOrderByCreatedAtDesc(userId.getId());
    }
}