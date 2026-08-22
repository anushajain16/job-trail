package com.example.anusha.job_trail.auth.config;

import com.example.anusha.job_trail.auth.oauth.AuthProvider;
import com.example.anusha.job_trail.auth.security.CurrentUserArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.core.convert.converter.Converter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Locale;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    public WebMvcConfig(CurrentUserArgumentResolver currentUserArgumentResolver) {
        this.currentUserArgumentResolver = currentUserArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }

    // Spring's default enum conversion is Enum.valueOf, which is
    // case-sensitive — without this, POST /api/auth/oauth/google (the
    // natural lowercase URL) would 400 while only /api/auth/oauth/GOOGLE
    // worked. Case-insensitive matching here is what actually makes the
    // path param ergonomic.
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, AuthProvider>() {
            @Override
            public AuthProvider convert(String source) {
                return AuthProvider.valueOf(source.toUpperCase(Locale.ROOT));
            }
        });
    }
}
