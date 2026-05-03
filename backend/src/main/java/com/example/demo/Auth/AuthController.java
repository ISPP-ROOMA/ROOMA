package com.example.demo.Auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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
import com.example.demo.Auth.DTOs.GoogleAuthRequest;
import com.example.demo.Auth.DTOs.LoginRequest;
import com.example.demo.Auth.DTOs.RefreshTokenRequest;
import com.example.demo.Auth.DTOs.ValidateTokenResponse;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final int secondsExpirationRt;
    private final AuthService authService;
    private final boolean refreshCookieSecure;
    private final String refreshCookieSameSite;
    private final String refreshCookieDomain;

    public AuthController(
            AuthService authService,
            @Value("${jwt.expiration-rt}") int expirationRt,
            @Value("${app.cookie.refresh.secure:false}") boolean refreshCookieSecure,
            @Value("${app.cookie.refresh.same-site:Lax}") String refreshCookieSameSite,
            @Value("${app.cookie.refresh.domain:}") String refreshCookieDomain) {
        this.authService = authService;
        this.secondsExpirationRt = expirationRt / 1000;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshCookieSameSite = refreshCookieSameSite;
        this.refreshCookieDomain = refreshCookieDomain;

    }

    private void addRefreshCookie(HttpServletResponse response, String token) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .path("/")
                .maxAge(secondsExpirationRt)
                .sameSite(refreshCookieSameSite);

        if (refreshCookieDomain != null && !refreshCookieDomain.isBlank()) {
            builder.domain(refreshCookieDomain);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(refreshCookieSameSite);

        if (refreshCookieDomain != null && !refreshCookieDomain.isBlank()) {
            builder.domain(refreshCookieDomain);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request,
            HttpServletResponse response) {
        AuthResult registerRes = authService.register(request.email(), request.password(), request.deviceId(),
                request.role());
        addRefreshCookie(response, registerRes.refreshToken());
        AuthResponse authResponse = new AuthResponse(registerRes.accessToken(), registerRes.role(), registerRes.userId());
        return ResponseEntity.ok().body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResult loginRes = authService.login(request.email(), request.password(), request.deviceId());
        addRefreshCookie(response, loginRes.refreshToken());
        AuthResponse authResp = new AuthResponse(loginRes.accessToken(), loginRes.role(), loginRes.userId());
        return ResponseEntity.ok().body(authResp);
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleAuthRequest request,
            HttpServletResponse response) {
        AuthResult googleRes = authService.googleLogin(request.idToken(), request.deviceId(), request.role());
        addRefreshCookie(response, googleRes.refreshToken());
        AuthResponse authResp = new AuthResponse(googleRes.accessToken(), googleRes.role(), googleRes.userId());
        return ResponseEntity.ok().body(authResp);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest refreshTokenRequest,
            @CookieValue(value = "refresh_token", required = true) String token,
            HttpServletResponse response) {
        AuthResult refreshRes = authService.refreshToken(token, refreshTokenRequest.deviceId());
        addRefreshCookie(response, refreshRes.refreshToken());
        AuthResponse authResp = new AuthResponse(refreshRes.accessToken(), refreshRes.role(), refreshRes.userId());
        return ResponseEntity.ok().body(authResp);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "refresh_token", required = true) String token,
            @RequestBody RefreshTokenRequest refreshTokenRequest,
            HttpServletResponse response) {

        authService.logout(token, refreshTokenRequest.deviceId());
        clearRefreshCookie(response);

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
