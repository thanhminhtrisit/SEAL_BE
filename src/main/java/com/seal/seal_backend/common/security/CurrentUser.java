package com.seal.seal_backend.common.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.lang.annotation.*;

/**
 * Inject the authenticated principal into a controller method param:
 *   public ApiResponse<?> me(@CurrentUser UserPrincipal user) {...}
 * Resolves once the Auth module (M1) installs the JWT filter + UserPrincipal.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {}
