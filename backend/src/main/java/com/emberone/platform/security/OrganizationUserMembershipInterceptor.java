/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import com.emberone.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Structurally binds path {@code userId} to path {@code organizationId} for every {@code
 * /organizations/{organizationId}/users/{userId}/…} route. Controllers under this pattern must not
 * rely solely on {@code @PreAuthorize} for the organization - membership of the target user is
 * enforced here before the handler runs.
 */
@Component
@RequiredArgsConstructor
public class OrganizationUserMembershipInterceptor implements HandlerInterceptor {

    /**
     * Matches both {@code /api/v1/auth/organizations/{org}/users/{user}/…} and {@code
     * /api/v1/auth/users/organizations/{org}/users/{user}/…}.
     */
    static final Pattern ORG_USER_PATH =
            Pattern.compile("^/api/v1/auth/(?:users/)?organizations/(\\d+)/users/(\\d+)(?:/|$)");

    private final UserService userService;

    static OrgUserPathIds resolve(String requestUri) {
        if (requestUri == null) {
            return null;
        }
        Matcher matcher = ORG_USER_PATH.matcher(requestUri);
        if (!matcher.find()) {
            return null;
        }
        return new OrgUserPathIds(
                Long.parseLong(matcher.group(1)), Long.parseLong(matcher.group(2)));
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = AdminPathTenantContextInterceptor.decodedRequestUri(request);
        if (path == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed request path");
        }
        OrgUserPathIds ids = resolve(path);
        if (ids == null) {
            return true;
        }
        userService.assertUserBelongsToOrganization(ids.userId(), ids.organizationId());
        return true;
    }

    static record OrgUserPathIds(Long organizationId, Long userId) {}
}
