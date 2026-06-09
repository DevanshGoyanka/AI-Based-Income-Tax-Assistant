package com.itr.interfaces.rest.auth;

import com.itr.dto.AuthRequest;
import com.itr.dto.AuthResponse;
import com.itr.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AuthController — /api/v1/auth/*
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of(
            "accessToken", "",
            "refreshToken", ""
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        return ResponseEntity.ok(Map.of("status", "LOGGED_OUT"));
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<Map<String, Object>> setup2FA() {
        return ResponseEntity.ok(Map.of("secret", "", "qrCodeUrl", ""));
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<Map<String, Object>> verify2FA(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of("verified", true));
    }
}
