package com.example.anusha.job_trail.auth;

import com.example.anusha.job_trail.auth.dto.AuthResponse;
import com.example.anusha.job_trail.auth.exception.OAuthVerificationException;
import com.example.anusha.job_trail.auth.oauth.AuthProvider;
import com.example.anusha.job_trail.auth.oauth.OAuthProviderClient;
import com.example.anusha.job_trail.auth.oauth.OAuthUserInfo;
import com.example.anusha.job_trail.auth.oauth.UserIdentity;
import com.example.anusha.job_trail.auth.oauth.UserIdentityRepository;
import com.example.anusha.job_trail.auth.security.JwtService;
import com.example.anusha.job_trail.user.User;
import com.example.anusha.job_trail.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The OAuth counterpart to {@link AuthService#login} / {@link AuthService#signup}:
 * verifies a provider-issued credential instead of a local password, then
 * issues the same access/refresh token pair either way — from the rest of
 * the app's point of view, a user is a user regardless of how they signed in.
 */
@Service
public class OAuthAuthService {

    private final Map<AuthProvider, OAuthProviderClient> providerClients;
    private final UserIdentityRepository userIdentityRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public OAuthAuthService(List<OAuthProviderClient> providerClients, UserIdentityRepository userIdentityRepository,
                             UserRepository userRepository, JwtService jwtService,
                             RefreshTokenService refreshTokenService) {
        this.providerClients = providerClients.stream()
                .collect(Collectors.toUnmodifiableMap(OAuthProviderClient::provider, Function.identity()));
        this.userIdentityRepository = userIdentityRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse authenticate(AuthProvider provider, String token) {
        OAuthProviderClient client = providerClients.get(provider);
        if (client == null) {
            throw new OAuthVerificationException(provider.name().toLowerCase());
        }
        OAuthUserInfo info = client.resolve(token);
        User user = findOrCreateUser(provider, info);

        String accessToken = jwtService.issueAccessToken(user.getId(), user.getEmail());
        String refreshToken = refreshTokenService.issue(user);
        return new AuthResponse(accessToken, refreshToken);
    }

    private User findOrCreateUser(AuthProvider provider, OAuthUserInfo info) {
        return userIdentityRepository.findByProviderAndProviderSubject(provider, info.subject())
                .map(UserIdentity::getUser)
                .orElseGet(() -> linkOrCreate(provider, info));
    }

    private User linkOrCreate(AuthProvider provider, OAuthUserInfo info) {
        // An account with this email may already exist (password signup, or
        // a different provider linked earlier) — link the new identity to
        // it rather than creating a duplicate user.
        User user = userRepository.findByEmail(info.email())
                .orElseGet(() -> userRepository.save(new User(info.email(), null)));
        try {
            userIdentityRepository.save(new UserIdentity(user, provider, info.subject()));
        } catch (DataIntegrityViolationException e) {
            // Same race as AuthService.signup's duplicate-email guard: two
            // concurrent OAuth logins for the same provider identity both
            // pass the lookup above, and the unique constraint on
            // (provider, provider_subject) is what actually decides. Whoever
            // loses just re-reads what the winner inserted.
            return userIdentityRepository.findByProviderAndProviderSubject(provider, info.subject())
                    .map(UserIdentity::getUser)
                    .orElseThrow(() -> e);
        }
        return user;
    }
}
