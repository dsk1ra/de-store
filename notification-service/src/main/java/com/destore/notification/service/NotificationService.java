package com.destore.notification.service;

import com.destore.notification.entity.Notification;
import com.destore.notification.entity.NotificationStatus;
import com.destore.notification.entity.NotificationType;
import com.destore.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    
    @Transactional
    public Notification sendNotification(NotificationType type, String recipient, String subject, String message) {
        Notification notification = Notification.builder()
                .notificationType(type)
                .recipient(recipient)
                .subject(subject)
                .message(message)
                .status(NotificationStatus.PENDING)
                .build();
        
        @SuppressWarnings("null")
        Notification saved = notificationRepository.save(notification);
        
        // Simulate sending notification
        boolean success = sendActualNotification(type, recipient, subject, message);
        
        if (success) {
            saved.setStatus(NotificationStatus.SENT);
            saved.setSentAt(LocalDateTime.now());
        } else {
            saved.setStatus(NotificationStatus.FAILED);
        }
        
        return notificationRepository.save(saved);
    }
    
    private boolean sendActualNotification(NotificationType type, String recipient, String subject, String message) {
        try {
            switch (type) {
                case EMAIL:
                    log.info("=== EMAIL NOTIFICATION ===");
                    log.info("To: {}", recipient);
                    log.info("Subject: {}", subject);
                    log.info("Message: {}", message);
                    log.info("========================");
                    break;
                case SMS:
                    log.info("=== SMS NOTIFICATION ===");
                    log.info("To: {}", recipient);
                    log.info("Message: {}", message);
                    log.info("=======================");
                    break;
                case PUSH_NOTIFICATION:
                    log.info("=== PUSH NOTIFICATION ===");
                    log.info("To: {}", recipient);
                    log.info("Title: {}", subject);
                    log.info("Message: {}", message);
                    log.info("========================");
                    break;
            }
            return true;
        } catch (Exception e) {
            log.error("Error sending notification", e);
            return false;
        }
    }
    
    public List<Notification> getNotificationsByRecipient(String recipient) {
        return notificationRepository.findByRecipient(recipient);
    }
    
    public List<Notification> getNotificationsByStatus(NotificationStatus status) {
        return notificationRepository.findByStatus(status);
    }
}
