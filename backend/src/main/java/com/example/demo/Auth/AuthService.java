package com.example.demo.Auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Auth.DTOs.AuthResult;
import com.example.demo.Exceptions.InvalidHashException;
import com.example.demo.Exceptions.InvalidPasswordException;
import com.example.demo.Exceptions.InvalidRefreshTokenException;
import com.example.demo.Exceptions.InvalidRoleException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.Exceptions.UserExistsException;
import com.example.demo.Jwt.JwtService;
import com.example.demo.User.RefreshTokenEntity;
import com.example.demo.User.RefreshTokenService;
import com.example.demo.User.Role;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

@Service
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final GoogleIdTokenVerifier googleVerifier;

    public AuthService(UserService userService, JwtService jwtService, RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder, @Value("${google.client-id}") String googleClientId) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.googleVerifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    @Transactional
    public AuthResult register(String email, String password, String deviceId, Role role) {
        Optional<UserEntity> existingUser = userService.findByEmail(email);

        if (existingUser.isPresent()) {
            throw new UserExistsException("User already exists");
        }

        if (role == Role.ADMIN) {
            throw new InvalidRoleException("Cannot register as admin");
        }

        UserEntity newUser = new UserEntity();
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole(role);
        newUser.setAuthProvider("LOCAL");
        userService.save(newUser);

        String accessToken = jwtService.generateAccessTokenFromEmail(newUser.getEmail());
        String refreshToken = jwtService.generateRefreshTokenFromEmail(newUser.getEmail());

        RefreshTokenEntity newRefreshToken = new RefreshTokenEntity();
        newRefreshToken.setToken(hashToken(refreshToken));
        newRefreshToken.setExpiration(jwtService.getExpirarationFromToken(refreshToken));
        newRefreshToken.setDeviceId(deviceId);
        newRefreshToken.setUser(newUser);

        refreshTokenService.save(newRefreshToken);

        return new AuthResult(accessToken, refreshToken, newUser.getRole().name(), newUser.getId());
    }

    @Transactional
    public AuthResult login(String email, String password, String deviceId) {
        UserEntity user = userService.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getPassword() == null) {
            throw new InvalidPasswordException("This account uses Google Sign-In. Please log in with Google.");
        }

        boolean validPassword = passwordEncoder.matches(password, user.getPassword());

        if (!validPassword) {
            throw new InvalidPasswordException("Invalid password");
        }

        user.setLastConnectionAt(LocalDateTime.now());
        userService.save(user);

        String accessToken = jwtService.generateAccessTokenFromEmail(user.getEmail());
        String refreshToken = jwtService.generateRefreshTokenFromEmail(user.getEmail());

        RefreshTokenEntity rt = refreshTokenService.findByUserAndDeviceId(user, deviceId);

        rt.setToken(hashToken(refreshToken));
        rt.setExpiration(jwtService.getExpirarationFromToken(refreshToken));
        rt.setDeviceId(deviceId);
        rt.setUser(user);

        refreshTokenService.save(rt);

        return new AuthResult(accessToken, refreshToken, user.getRole().name(), user.getId());
    }

    @Transactional
    public AuthResult googleLogin(String idTokenString, String deviceId, Role role) {
        GoogleIdToken idToken;
        try {
            idToken = googleVerifier.verify(idTokenString);
        } catch (Exception e) {
            throw new InvalidPasswordException("Invalid Google token");
        }

        if (idToken == null) {
            throw new InvalidPasswordException("Invalid Google token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String googleId = payload.getSubject();
        String email = payload.getEmail();

        // Try to find existing user by googleId first, then by email
        Optional<UserEntity> existingByGoogleId = userService.findByGoogleId(googleId);
        Optional<UserEntity> existingByEmail = userService.findByEmail(email);

        UserEntity user;

        if (existingByGoogleId.isPresent()) {
            // User already linked with Google
            user = existingByGoogleId.get();
        } else if (existingByEmail.isPresent()) {
            // Existing user (registered via email) — link Google account
            user = existingByEmail.get();
            user.setGoogleId(googleId);
            if (user.getAuthProvider() == null || "LOCAL".equals(user.getAuthProvider())) {
                user.setAuthProvider("LOCAL_GOOGLE");
            }
        } else {
            // New user — register via Google
            if (role == null) {
                throw new InvalidRoleException("Role is required for new Google registrations");
            }
            if (role == Role.ADMIN) {
                throw new InvalidRoleException("Cannot register as admin");
            }

            user = new UserEntity();
            user.setEmail(email);
            user.setGoogleId(googleId);
            user.setAuthProvider("GOOGLE");
            user.setRole(role);

            String name = (String) payload.get("given_name");
            String surname = (String) payload.get("family_name");
            String picture = (String) payload.get("picture");
            if (name != null) user.setName(name);
            if (surname != null) user.setSurname(surname);
            if (picture != null) user.setProfileImageUrl(picture);
        }

        user.setLastConnectionAt(LocalDateTime.now());
        userService.save(user);

        String accessToken = jwtService.generateAccessTokenFromEmail(user.getEmail());
        String refreshToken = jwtService.generateRefreshTokenFromEmail(user.getEmail());

        // Create or update refresh token for this device
        RefreshTokenEntity rt;
        try {
            rt = refreshTokenService.findByUserAndDeviceId(user, deviceId);
        } catch (Exception e) {
            rt = new RefreshTokenEntity();
        }
        rt.setToken(hashToken(refreshToken));
        rt.setExpiration(jwtService.getExpirarationFromToken(refreshToken));
        rt.setDeviceId(deviceId);
        rt.setUser(user);
        refreshTokenService.save(rt);

        return new AuthResult(accessToken, refreshToken, user.getRole().name(), user.getId());
    }

    @Transactional
    public AuthResult refreshToken(String refreshToken, String deviceId) {

        if (!jwtService.verifyToken(refreshToken)) {
            throw new InvalidRefreshTokenException("Invalid Refresh Token");
        }

        String email = jwtService.getEmailFromToken(refreshToken);
        UserEntity user = userService.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setLastConnectionAt(LocalDateTime.now());
        userService.save(user);

        RefreshTokenEntity storedToken = refreshTokenService.findByUserAndDeviceId(user, deviceId);

        String incomingTokenHash = hashToken(refreshToken);
        if (!incomingTokenHash.equals(storedToken.getToken())) {
            throw new InvalidRefreshTokenException("Token does not match records");
        }

        if (storedToken.getExpiration().before(new java.util.Date())) {
            refreshTokenService.delete(storedToken);
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        String newAccessToken = jwtService.generateAccessTokenFromEmail(user.getEmail());
        String newRefreshToken = jwtService.generateRefreshTokenFromEmail(user.getEmail());

        storedToken.setToken(hashToken(newRefreshToken));
        storedToken.setExpiration(jwtService.getExpirarationFromToken(newRefreshToken));

        refreshTokenService.save(storedToken);

        return new AuthResult(newAccessToken, newRefreshToken, user.getRole().name(), user.getId());
    }

    @Transactional
    public void logout(String refreshToken, String deviceId) {

        if (!jwtService.verifyToken(refreshToken)) {
            throw new InvalidRefreshTokenException("Invalid Refresh Token");
        }

        String username = jwtService.getEmailFromToken(refreshToken);

        UserEntity user = userService.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        RefreshTokenEntity storedToken = refreshTokenService.findByUserAndDeviceId(user, deviceId);

        String incomingTokenHash = hashToken(refreshToken);

        if (!incomingTokenHash.equals(storedToken.getToken())) {
            throw new InvalidRefreshTokenException("Token does not match records");
        }

        refreshTokenService.delete(storedToken);
    }

    public boolean validateAccessToken(String token) {
        return jwtService.verifyToken(token);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidHashException("Error hashing token");
        }
    }

}
