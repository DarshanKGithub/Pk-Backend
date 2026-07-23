package com.pkcorporate.config;

import com.pkcorporate.security.filter.JwtAuthenticationFilter;
import com.pkcorporate.security.filter.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.pkcorporate.repository.UserRepository;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final RateLimitingFilter rateLimitingFilter;
    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String[] allowedOrigins;

    @Value("${app.csp.connect-src:http://localhost:9090 http://localhost:3000 http://localhost:5173}")
    private String cspConnectSrc;

    // Truly public endpoints — no token required
    private static final String[] PUBLIC_URLS = {
            "/auth/login",
            "/auth/refresh-token",
            "/auth/forgot-password",
            "/auth/reset-password",
            "/v1/orders/track/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health",
            "/ping"
    };

    // Authenticated-only auth endpoints (need a valid token)
    private static final String[] AUTH_REQUIRED_URLS = {
            "/auth/logout",
            "/auth/change-password",
            "/auth/me"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public — no token required
                .requestMatchers(PUBLIC_URLS).permitAll()
                // Auth endpoints that need a valid token
                .requestMatchers(AUTH_REQUIRED_URLS).authenticated()
                // Admin-only paths
                .requestMatchers("/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/v1/users/**").hasRole("ADMIN")
                .requestMatchers("/v1/analytics/**").hasRole("ADMIN")
                // Admin + Accountant
                .requestMatchers("/v1/invoices/**").hasAnyRole("ADMIN", "ACCOUNTANT")
                .requestMatchers("/v1/payments/**").hasAnyRole("ADMIN", "ACCOUNTANT")
                .requestMatchers("/v1/dispatch/**").hasAnyRole("ADMIN", "ACCOUNTANT")
                // Admin + Agent
                .requestMatchers("/v1/customers/**").hasAnyRole("ADMIN", "AGENT")
                .requestMatchers("/v1/orders/**").hasAnyRole("ADMIN", "AGENT", "DESIGNER", "ACCOUNTANT")
                // Designer
                .requestMatchers("/v1/design/**").hasAnyRole("ADMIN", "DESIGNER")
                // Products (public catalog read, admin write handled by @PreAuthorize on controller)
                .requestMatchers("/v1/products/**").authenticated()
                // All other authenticated
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"message\": \"Unauthorized: Please log in.\"}");
                })
            );
        
        // Security headers configuration
        http.headers(headers -> {
                // Content Security Policy
                headers.contentSecurityPolicy(csp -> csp
                        .policyDirectives(
                            "default-src 'self'; " +
                            "script-src 'self'; " +
                            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                            "img-src 'self' data: https://res.cloudinary.com; " +
                            "font-src 'self' https://fonts.gstatic.com; " +
                            "frame-ancestors 'none'; " +
                            "connect-src 'self' " + cspConnectSrc
                        )
                );
                // X-Frame-Options DENY
                headers.frameOptions(frame -> frame.deny());
                // X-Content-Type-Options nosniff (enabled by default, but explicit)
                headers.contentTypeOptions(cto -> {});
                // HTTP Strict Transport Security
                headers.httpStrictTransportSecurity(hsts -> hsts
                        .maxAgeInSeconds(31536000)
                        .includeSubDomains(true)
                        .preload(true)
                );
                // Referrer Policy
                headers.referrerPolicy(ref -> ref.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                // Permissions Policy
                headers.permissionsPolicy(p -> p.policy("camera=(), microphone=(), geolocation=(), payment=()"));
                // Remove Server header / technology disclosure
                headers.addHeaderWriter(new StaticHeadersWriter("Server", ""));
        });

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> patterns = new java.util.ArrayList<>();
        for (String origin : allowedOrigins) {
            String trimmed = origin.trim();
            String noSlash = trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
            if (!noSlash.isEmpty()) {
                patterns.add(noSlash);
                patterns.add(noSlash + "/");
            }
        }
        patterns.add("https://*.vercel.app");
        patterns.add("https://*.vercel.app/");
        // Capacitor Android WebView origins
        patterns.add("https://localhost");
        patterns.add("https://localhost/");
        patterns.add("capacitor://localhost");
        patterns.add("capacitor://localhost/");
        patterns.add("http://localhost");
        patterns.add("http://localhost/");
        config.setAllowedOriginPatterns(patterns);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Strength 10 is the NIST-recommended default and still strongly secure.
        // Strength 12 (the previous setting) is ~4x more CPU-intensive — on a
        // 0.1 CPU Render free tier that made logins take several seconds.
        return new BCryptPasswordEncoder(10);
    }
}
