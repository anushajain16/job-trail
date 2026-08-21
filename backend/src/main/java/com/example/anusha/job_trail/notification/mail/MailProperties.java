package com.example.anusha.job_trail.notification.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code app.mail.*} — application-level mail settings that sit
 * alongside, but separate from, Spring Boot's own {@code spring.mail.*}
 * (transport/connection settings autoconfigured into {@code JavaMailSender}).
 *
 * @param from the "From" address every outbound email is sent as.
 */
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(String from) {
}
