package com.hisobnoma.platform.auth.service;

import com.hisobnoma.platform.auth.dto.*;
import com.hisobnoma.platform.auth.entity.PasswordResetToken;
import com.hisobnoma.platform.auth.entity.RefreshToken;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.PasswordResetTokenRepository;
import com.hisobnoma.platform.auth.repository.RefreshTokenRepository;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.JwtTokenProvider;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.UnauthorizedException;
import com.hisobnoma.platform.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextHelper securityContextHelper;

    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.lock-duration-minutes:30}")
    private int lockDurationMinutes;

    @Value("${app.security.password-reset-expiration-minutes:60}")
    private int passwordResetExpirationMinutes;

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String deviceInfo) {
        User user = userRepository.findByUsernameWithRolesAndPermissions(request.getUsername())
                .or(() -> userRepository.findByPhoneWithRolesAndPermissions(request.getUsername()))
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        // Check if account is locked
        if (user.isLocked() && !user.isAccountNonLocked()) {
            throw new LockedException("Account is locked. Try again later.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword())
            );

            // Reset failed attempts on successful login
            user.resetFailedLoginAttempts();
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            String accessToken = tokenProvider.generateAccessToken(userPrincipal);
            String refreshToken = createRefreshToken(user, ipAddress, deviceInfo);

            return buildAuthResponse(accessToken, refreshToken, user, userPrincipal);

        } catch (BadCredentialsException e) {
            handleFailedLogin(user);
            throw new UnauthorizedException("Invalid credentials");
        }
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new UnauthorizedException("Refresh token is expired or revoked");
        }

        User user = refreshToken.getUser();
        user = userRepository.findByIdWithRolesAndPermissions(user.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String newAccessToken = tokenProvider.generateAccessToken(userPrincipal);

        // Rotate refresh token
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);
        String newRefreshToken = createRefreshToken(user, refreshToken.getIpAddress(), refreshToken.getDeviceInfo());

        return buildAuthResponse(newAccessToken, newRefreshToken, user, userPrincipal);
    }

    @Transactional
    public void logout() {
        UserPrincipal currentUser = securityContextHelper.getRequiredCurrentUser();
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        refreshTokenRepository.revokeAllByUser(user);
        log.info("User {} logged out, all refresh tokens revoked", user.getUsername());
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException("Passwords do not match");
        }

        UserPrincipal currentUser = securityContextHelper.getRequiredCurrentUser();
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect", "INVALID_PASSWORD");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all refresh tokens to force re-login
        refreshTokenRepository.revokeAllByUser(user);

        log.info("User {} changed password", user.getUsername());
    }

    public AuthResponse.UserInfo getCurrentUserInfo() {
        UserPrincipal currentUser = securityContextHelper.getRequiredCurrentUser();
        User user = userRepository.findByIdWithRolesAndPermissions(currentUser.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return buildUserInfo(user, currentUser);
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByUsername(request.getIdentifier())
                .or(() -> userRepository.findByPhone(request.getIdentifier()))
                .orElse(null);

        // Always return success to prevent user enumeration
        if (user == null) {
            log.warn("Password reset requested for non-existent user: {}", request.getIdentifier());
            return "If the account exists, a reset link will be sent";
        }

        // Invalidate any existing reset tokens
        passwordResetTokenRepository.invalidateAllByUser(user);

        // Create new reset token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(passwordResetExpirationMinutes * 60L))
                .build();

        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset token generated for user: {}", user.getUsername());

        // In production, send SMS/notification here
        // For now, return the token (only for development)
        return token;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException("Passwords do not match");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findValidToken(request.getToken(), Instant.now())
                .orElseThrow(() -> new BusinessException("Invalid or expired reset token", "INVALID_TOKEN"));

        User user = resetToken.getUser();

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.resetFailedLoginAttempts();
        user.setLocked(false);
        user.setLockedUntil(null);
        userRepository.save(user);

        // Mark token as used
        resetToken.markAsUsed();
        passwordResetTokenRepository.save(resetToken);

        // Revoke all refresh tokens
        refreshTokenRepository.revokeAllByUser(user);

        log.info("Password reset completed for user: {}", user.getUsername());
    }

    private void handleFailedLogin(User user) {
        user.incrementFailedLoginAttempts();

        if (user.getFailedLoginAttempts() >= maxLoginAttempts) {
            user.setLocked(true);
            user.setLockedUntil(Instant.now().plusSeconds(lockDurationMinutes * 60L));
            log.warn("User {} account locked after {} failed attempts",
                    user.getUsername(), maxLoginAttempts);
        }

        userRepository.save(user);
    }

    private String createRefreshToken(User user, String ipAddress, String deviceInfo) {
        String token = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(Instant.now().plusMillis(tokenProvider.getRefreshTokenExpiration()))
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken,
                                           User user, UserPrincipal userPrincipal) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenExpiration() / 1000)
                .user(buildUserInfo(user, userPrincipal))
                .build();
    }

    private AuthResponse.UserInfo buildUserInfo(User user, UserPrincipal userPrincipal) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getCode())
                .collect(Collectors.toSet());

        Set<String> permissions = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> !auth.startsWith("ROLE_"))
                .collect(Collectors.toSet());

        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .tenantId(user.getTenantId())
                .roles(roles)
                .permissions(permissions)
                .build();
    }
}
