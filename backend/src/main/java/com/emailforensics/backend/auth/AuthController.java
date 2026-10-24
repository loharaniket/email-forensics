package com.emailforensics.backend.auth;

import com.emailforensics.backend.dto.auth.AnalystProfileDto;
import com.emailforensics.backend.dto.auth.LoginRequest;
import com.emailforensics.backend.dto.auth.LoginResponse;
import com.emailforensics.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<AnalystProfileDto> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        AnalystProfileDto profile = authService.getProfile(principal);
        return ResponseEntity.ok(profile);
    }
}
