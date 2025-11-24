package com.destore.auth.init;

import com.destore.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private final AuthService authService;
    
    @Override
    public void run(String... args) {
        try {
            // Create default users
            authService.createUser("store.manager1", "Password123!", 
                    "manager1@destore.com", "STORE_MANAGER");
            authService.createUser("store.manager2", "Password123!", 
                    "manager2@destore.com", "STORE_MANAGER");
            authService.createUser("admin", "Admin123!", 
                    "admin@destore.com", "ADMINISTRATOR");
            
            log.info("Default users created successfully");
        } catch (Exception e) {
            log.info("Users may already exist: {}", e.getMessage());
        }
    }
}
