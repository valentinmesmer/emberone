/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.cache;

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
}
