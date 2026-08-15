package com.example.anusha.job_trail.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Interim security config: no auth endpoints exist yet, so nothing is
 * protected. Its only real job today is keeping Swagger and the actuator
 * health check reachable now that spring-boot-starter-security is on the
 * classpath (which locks everything down by default). Real JWT-based auth
 * replaces the permit-all rule when the {@code auth} package grows endpoints.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/health"
                        ).permitAll()
                        // TODO(auth): tighten to authenticated() once login/JWT lands.
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
