package com.pkcorporate.security.filter;

import com.pkcorporate.repository.UserRepository;
import com.pkcorporate.security.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Authenticates requests from the JWT bearer token.
 * <p>
 * The token's signature + expiry are validated by JwtService.isTokenValid(). The full
 * User entity is then loaded by id and stored as the SecurityContext principal, so that
 * controller methods using {@code @AuthenticationPrincipal User user} receive the entity.
 * <p>
 * NOTE: a previous "Step 3" optimization set the principal to the bare userId String to
 * skip this DB lookup, but every controller still declares {@code @AuthenticationPrincipal
 * User} — which then resolved to null and caused NPE/500s on authenticated endpoints.
 * The per-request lookup is restored to honor that contract.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String requestUrl = request.getRequestURI();
        final String authHeader = request.getHeader("Authorization");

        log.debug("[JwtAuth] Incoming request: {} {}", request.getMethod(), requestUrl);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("[JwtAuth] No Bearer token on request: {}", requestUrl);
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        log.debug("[JwtAuth] Bearer token found (length={})", jwt.length());

        try {
            if (!jwtService.isTokenValid(jwt)) {
                log.warn("[JwtAuth] Token is INVALID or EXPIRED for request: {}", requestUrl);
                filterChain.doFilter(request, response);
                return;
            }

            final String userId = jwtService.extractUserId(jwt);
            log.debug("[JwtAuth] Token valid. Extracted userId: {}", userId);

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                userRepository.findById(UUID.fromString(userId)).ifPresentOrElse(user -> {

                    // Check both active status AND email verification
                    if (!user.isActive()) {
                        log.warn("[JwtAuth] User {} is DEACTIVATED — skipping authentication", user.getEmail());
                        return;
                    }
                    if (!user.isEnabled()) {
                        log.warn("[JwtAuth] User {} email is NOT VERIFIED — skipping authentication", user.getEmail());
                        return;
                    }

                    var authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    user,  // principal = full User entity (@AuthenticationPrincipal)
                                    null,
                                    List.of(authority)
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("[JwtAuth] Authentication SET — user={}, role=ROLE_{}, endpoint={}",
                            user.getEmail(), user.getRole().name(), requestUrl);

                }, () -> log.warn("[JwtAuth] No user found in DB for userId={} on request: {}", userId, requestUrl));
            }
        } catch (Exception e) {
            log.error("[JwtAuth] Cannot set user authentication for {}: {}", requestUrl, e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
