package com.example.demo.Auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Auth.DTOs.AuthRequest;
import com.example.demo.Auth.DTOs.AuthResponse;
import com.example.demo.Auth.DTOs.AuthResult;
import com.example.demo.Auth.DTOs.RefreshTokenRequest;
import com.example.demo.Auth.DTOs.ValidateTokenResponse;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final int secondsExpirationRt;
    private final AuthService authService;

    public AuthController(AuthService authService, @Value("${jwt.expiration-rt}") int expirationRt) {
        this.authService = authService;
        this.secondsExpirationRt = expirationRt / 1000;

    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request,
            HttpServletResponse response) {
        AuthResult registerRes = authService.register(request.email(), request.password(), request.deviceId(),
                request.role());
        Cookie cookie = new Cookie("refresh_token", registerRes.refreshToken());
        cookie.setMaxAge(secondsExpirationRt);
        cookie.setSecure(false);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        AuthResponse authResponse = new AuthResponse(registerRes.accessToken(), registerRes.role(), registerRes.userId());
        response.addCookie(cookie);
        return ResponseEntity.ok().body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResult loginRes = authService.login(request.email(), request.password(), request.deviceId());
        Cookie cookie = new Cookie("refresh_token", loginRes.refreshToken());
        cookie.setMaxAge(secondsExpirationRt);
        cookie.setSecure(false);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        AuthResponse authResp = new AuthResponse(loginRes.accessToken(), loginRes.role(), loginRes.userId());
        response.addCookie(cookie);
        return ResponseEntity.ok().body(authResp);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest refreshTokenRequest,
            @CookieValue(value = "refresh_token", required = true) String token,
            HttpServletResponse response) {
        AuthResult refreshRes = authService.refreshToken(token, refreshTokenRequest.deviceId());
        Cookie cookie = new Cookie("refresh_token", refreshRes.refreshToken());
        cookie.setMaxAge(secondsExpirationRt);
        cookie.setSecure(false);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        AuthResponse authResp = new AuthResponse(refreshRes.accessToken(), refreshRes.role(), refreshRes.userId());
        response.addCookie(cookie);
        return ResponseEntity.ok().body(authResp);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "refresh_token", required = true) String token,
            @RequestBody RefreshTokenRequest refreshTokenRequest,
            HttpServletResponse response) {

        authService.logout(token, refreshTokenRequest.deviceId());

        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");

        response.addCookie(cookie);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/validate")
    public ResponseEntity<ValidateTokenResponse> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(
                    new ValidateTokenResponse(false, "Missing token"));
        }

        String token = authHeader.substring(7);

        boolean valid = authService.validateAccessToken(token);

        if (!valid) {
            return ResponseEntity.ok(
                    new ValidateTokenResponse(false, "Invalid or expired token"));
        }

        return ResponseEntity.ok(new ValidateTokenResponse(true, "Token valid"));
    }

}
