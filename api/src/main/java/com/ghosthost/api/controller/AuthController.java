package com.ghosthost.api.controller;

import com.ghosthost.api.dto.AuthRequest;
import com.ghosthost.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Auth controller — handles user registration and login.
 *
 * ENDPOINTS:
 * POST /auth/register — create a new account, returns JWT
 * POST /auth/login — login with email/password, returns JWT
 *
 * Both endpoints are public (no token required) — see SecurityConfig.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new user.
     *
     * curl -X POST http://localhost:8080/auth/register \
     * -H "Content-Type: application/json" \
     * -d '{"email":"student@test.com","password":"secret123"}'
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request) {
        try {
            String token = authService.register(request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(Map.of(
                            "token", token,
                            "email", request.getEmail()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Login an existing user.
     *
     * curl -X POST http://localhost:8080/auth/login \
     * -H "Content-Type: application/json" \
     * -d '{"email":"student@test.com","password":"secret123"}'
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        try {
            String token = authService.login(request);
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "email", request.getEmail()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
