/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.testsupport;

import com.emberone.platform.security.EmberOneAppRole;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class SecurityTestFixtures {

    public static final long ORG_ID = 10L;
    public static final long TENANT_ID = 7L;
    public static final long USER_ID = 42L;
    public static final long GROUP_ID = 5L;
    public static final long WORKFLOW_ID = 1L;
    public static final long APPOINTMENT_ID = 5L;

    private SecurityTestFixtures() {}

    public static RequestPostProcessor tenantUserJwt() {
        return JwtTestSupport.jwtForRole(EmberOneAppRole.USER, TENANT_ID, USER_ID);
    }

    public static RequestPostProcessor tenantAdminJwt() {
        return JwtTestSupport.jwtForRole(EmberOneAppRole.TENANT_ADMIN, TENANT_ID, USER_ID);
    }

    public static RequestPostProcessor globalAdminJwt() {
        return JwtTestSupport.jwtForRole(EmberOneAppRole.GLOBAL_ADMIN, null, USER_ID);
    }
}
