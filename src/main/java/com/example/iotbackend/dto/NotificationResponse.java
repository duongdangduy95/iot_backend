package com.example.iotbackend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@Getter
@Setter
public class NotificationResponse {

    private Long id;
    private String title;
    private String content;
    private String actorName;
    private String recipientName;
    private LocalDateTime createdAt;
}
