package com.example.anusha.job_trail.auth;

import com.example.anusha.job_trail.auth.dto.AuthResponse;
import com.example.anusha.job_trail.auth.dto.CurrentUserResponse;
import com.example.anusha.job_trail.auth.dto.LoginRequest;
import com.example.anusha.job_trail.auth.dto.OAuthLoginRequest;
import com.example.anusha.job_trail.auth.dto.RefreshRequest;
import com.example.anusha.job_trail.auth.dto.SignupRequest;
import com.example.anusha.job_trail.auth.oauth.AuthProvider;
import com.example.anusha.job_trail.auth.security.AuthenticatedUser;
import com.example.anusha.job_trail.auth.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OAuthAuthService oAuthAuthService;

    public AuthController(AuthService authService, OAuthAuthService oAuthAuthService) {
        this.authService = authService;
        this.oAuthAuthService = oAuthAuthService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // One endpoint for every provider rather than /oauth/google + /oauth/github:
    // the request/response shape is identical, and the provider param is
    // validated against the AuthProvider enum by Spring's converter, so an
    // unsupported value 400s before it ever reaches the service.
    @PostMapping("/oauth/{provider}")
    public AuthResponse oauthLogin(@PathVariable AuthProvider provider, @Valid @RequestBody OAuthLoginRequest request) {
        return oAuthAuthService.authenticate(provider, request.token());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@CurrentUser AuthenticatedUser currentUser) {
        return new CurrentUserResponse(currentUser.id(), currentUser.email());
    }
}
