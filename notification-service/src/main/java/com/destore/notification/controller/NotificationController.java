package com.destore.notification.controller;

import com.destore.dto.ApiResponse;
import com.destore.notification.entity.Notification;
import com.destore.notification.entity.NotificationStatus;
import com.destore.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @GetMapping("/recipient/{recipient}")
    public ResponseEntity<ApiResponse<List<Notification>>> getByRecipient(@PathVariable String recipient) {
        List<Notification> notifications = notificationService.getNotificationsByRecipient(recipient);
        return ResponseEntity.ok(new ApiResponse<>(true, "Notifications retrieved", notifications));
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<Notification>>> getByStatus(@PathVariable NotificationStatus status) {
        List<Notification> notifications = notificationService.getNotificationsByStatus(status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Notifications retrieved", notifications));
    }
    
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Notification service is running", "OK"));
    }
}
