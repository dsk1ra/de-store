package com.destore.gateway.config;

import com.destore.gateway.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GatewayConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service - No JWT required for login
                .route("auth-service", r -> r
                        .path("/auth/**")
                        .uri("http://auth-service:8081"))
                
                // Pricing Service - JWT required
                .route("pricing-service", r -> r
                        .path("/api/pricing/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("http://pricing-service:8082"))
                
                // Inventory Service - JWT required
                .route("inventory-service", r -> r
                        .path("/api/inventory/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("http://inventory-service:8083"))
                
                // Finance Service - JWT required
                .route("finance-service", r -> r
                        .path("/api/finance/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("http://finance-service:8084"))
                
                // Notification Service - JWT required
                .route("notification-service", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("http://notification-service:8085"))
                
                // Enabling Simulator - No JWT required (external system)
                .route("enabling-simulator", r -> r
                        .path("/api/enabling/**")
                        .uri("http://enabling-simulator:9000"))
                
                .build();
    }
}
