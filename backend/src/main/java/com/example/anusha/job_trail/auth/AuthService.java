package com.example.anusha.job_trail.auth;

import com.example.anusha.job_trail.auth.dto.AuthResponse;
import com.example.anusha.job_trail.auth.dto.LoginRequest;
import com.example.anusha.job_trail.auth.dto.RefreshRequest;
import com.example.anusha.job_trail.auth.dto.SignupRequest;
import com.example.anusha.job_trail.auth.exception.EmailAlreadyInUseException;
import com.example.anusha.job_trail.auth.security.JwtService;
import com.example.anusha.job_trail.user.User;
import com.example.anusha.job_trail.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyInUseException("An account with this email already exists");
        }
        User user = new User(request.email(), passwordEncoder.encode(request.password()));
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Closes the race between the existsByEmail check above and this
            // insert: two concurrent signups for the same address both pass
            // the check, and the DB's unique constraint is what actually
            // decides — the loser gets the same 409 the check would've given.
            throw new EmailAlreadyInUseException("An account with this email already exists");
        }
        return issueTokens(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        User user = refreshTokenService.verifyAndRevoke(request.refreshToken());
        return issueTokens(user);
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenService.verifyAndRevoke(request.refreshToken());
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.issueAccessToken(user.getId(), user.getEmail()); //jwt token
        String refreshToken = refreshTokenService.issue(user); //token saved for few days 
        return new AuthResponse(accessToken, refreshToken);
    }
}
