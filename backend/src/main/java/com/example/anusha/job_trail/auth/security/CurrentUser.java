package com.example.anusha.job_trail.auth.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method parameter of type {@link AuthenticatedUser} to
 * be resolved from the current request's security context, e.g.
 * {@code me(@CurrentUser AuthenticatedUser user)}. Only valid on protected
 * routes — {@link CurrentUserArgumentResolver} throws if there's no
 * authenticated principal, which can't happen on a route Spring Security
 * already required authentication for.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
