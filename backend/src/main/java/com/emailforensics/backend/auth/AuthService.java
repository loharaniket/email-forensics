package com.emailforensics.backend.auth;

import com.emailforensics.backend.dto.auth.AnalystProfileDto;
import com.emailforensics.backend.dto.auth.LoginRequest;
import com.emailforensics.backend.dto.auth.LoginResponse;
import com.emailforensics.backend.security.JwtService;
import com.emailforensics.backend.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", principal.getId().toString());
        claims.put("email", principal.getEmail());
        claims.put("role", principal.getRole());

        String token = jwtService.generateToken(principal.getUsername(), claims);
        Instant expiresAt = Instant.now().plusMillis(jwtService.getExpirationMs());

        AnalystProfileDto profile = getProfile(principal);

        return new LoginResponse(token, "Bearer", expiresAt, profile);
    }

    public AnalystProfileDto getProfile(UserPrincipal principal) {
        List<String> permissions = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return new AnalystProfileDto(
                principal.getId(),
                principal.getUsername(),
                principal.getEmail(),
                principal.getRole(),
                permissions
        );
    }
}
