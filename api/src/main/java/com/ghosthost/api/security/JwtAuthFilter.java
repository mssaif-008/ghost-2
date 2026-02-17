package com.ghosthost.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT Authentication Filter
 *
 * HOW IT WORKS:
 * 1. Every request passes through this filter
 * 2. We look for "Authorization: Bearer <token>" header
 * 3. If present, we validate the token using JwtUtil
 * 4. If valid, we set the userId in the SecurityContext
 * 5. Controllers can then access the userId via SecurityContextHolder
 *
 * COMMON MISTAKE: Forgetting to call filterChain.doFilter().
 * If you skip it, the request never reaches the controller.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. Extract the Authorization header
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // Remove "Bearer " prefix

            // 2. Validate the token
            Long userId = jwtUtil.validateToken(token);

            if (userId != null) {
                // 3. Set authentication in SecurityContext
                // The "principal" is the userId (as a String)
                // We use an empty authorities list for MVP
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userId,            // principal = userId
                                null,              // credentials (not needed)
                                Collections.emptyList()  // authorities
                        );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // 4. Continue the filter chain (ALWAYS call this!)
        filterChain.doFilter(request, response);
    }
}
