package com.pkcorporate.config;

import com.pkcorporate.entity.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() instanceof String) {
                return Optional.of("system");
            }
            if (auth.getPrincipal() instanceof User user) {
                return Optional.of(user.getEmail());
            }
            return Optional.of("system");
        };
    }
}
