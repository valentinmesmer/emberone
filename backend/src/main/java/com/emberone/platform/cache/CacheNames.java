/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.cache;

import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CacheNames {

    public static final String USER_PERMISSIONS = "userPermissions";
    public static final String ORG_IN_TENANT = "orgInTenant";
    public static final String EFFECTIVE_STANDARD_BY_ORG_TYPE = "effectiveStandardByOrgType";
    public static final String EFFECTIVE_STANDARD_BY_ORG = "effectiveStandardByOrg";
    public static final String ORGANIZATIONS_FOR_STANDARD = "organizationsForStandard";
    public static final String ENABLED_MODULES_BY_STANDARD = "enabledModulesByStandard";
    public static final String TENANT_LOCALE = "tenantLocale";
    public static final String TENANT_FEDERAL_STATE = "tenantFederalState";
    public static final String ACTIVE_ORG_MEMBERSHIP = "activeOrgMembership";
    public static final String USER_ACTIVE = "userActive";

    /** Every cache name; each one needs a TTL in {@code emberone.cache.ttl}. */
    public static final List<String> ALL =
            List.of(
                    USER_PERMISSIONS,
                    ORG_IN_TENANT,
                    EFFECTIVE_STANDARD_BY_ORG_TYPE,
                    EFFECTIVE_STANDARD_BY_ORG,
                    ORGANIZATIONS_FOR_STANDARD,
                    ENABLED_MODULES_BY_STANDARD,
                    TENANT_LOCALE,
                    TENANT_FEDERAL_STATE,
                    ACTIVE_ORG_MEMBERSHIP,
                    USER_ACTIVE);
}
