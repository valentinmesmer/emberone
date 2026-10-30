/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Every domain table with a {@code tenant_id} column must have row level security enabled, forced
 * and a policy on the {@code app.tenant_id} session setting. Guards new tables (for example {@code
 * course_registrations}) against a forgotten policy.
 */
class TenantTableRowLevelSecurityArchitectureTest {

    private static final Path V1 =
            Path.of("src/main/resources/db/migration/postgresql/V1__Initial_setup.sql");

    /** The tenant row counter is only reachable through the tenant service, not per request. */
    private static final Set<String> WITHOUT_RLS = Set.of("tenant_personal_number_sequences");

    private static final Pattern CREATE_TABLE =
            Pattern.compile(
                    "^CREATE TABLE (\\w+) \\((.*?)^\\);", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern TENANT_COLUMN =
            Pattern.compile("^\\s+tenant_id BIGINT", Pattern.MULTILINE);

    @Test
    void everyTenantTableEnablesForcesAndPoliciesRowLevelSecurity() throws IOException {
        String sql = Files.readString(V1);
        List<String> problems = new ArrayList<>();
        Matcher tables = CREATE_TABLE.matcher(sql);
        int checked = 0;
        while (tables.find()) {
            String table = tables.group(1);
            if (!TENANT_COLUMN.matcher(tables.group(2)).find() || WITHOUT_RLS.contains(table)) {
                continue;
            }
            checked++;
            if (!sql.contains("ALTER TABLE " + table + " ENABLE ROW LEVEL SECURITY;")) {
                problems.add(table + ": RLS not enabled");
            }
            if (!sql.contains("ALTER TABLE " + table + " FORCE ROW LEVEL SECURITY;")) {
                problems.add(table + ": RLS not forced");
            }
            if (!Pattern.compile("CREATE POLICY \\w+ ON " + table + "\\s[^;]*app\\.tenant_id[^;]*;")
                    .matcher(sql)
                    .find()) {
                problems.add(table + ": no tenant policy on app.tenant_id");
            }
        }
        assertThat(checked).isGreaterThan(50);
        assertThat(problems).isEmpty();
    }

    @Test
    void globalAdminPoliciesAreSelectOnlyPermissiveAndLimitedToAdminReadTables()
            throws IOException {
        String sql = Files.readString(V1);
        Matcher policies =
                Pattern.compile("CREATE POLICY (\\w+) ON (\\w+)\\s([^;]*);").matcher(sql);
        java.util.Set<String> allowed =
                java.util.Set.of(
                        "idm_users",
                        "users",
                        "organizations",
                        "user_organization_memberships",
                        "org_management_activations");
        java.util.Set<String> found = new java.util.HashSet<>();
        while (policies.find()) {
            if (!policies.group(3).contains("app.global_admin")) {
                continue;
            }
            found.add(policies.group(2));
            assertThat(policies.group(3))
                    .as(policies.group(1))
                    .contains("FOR SELECT")
                    .doesNotContain("RESTRICTIVE")
                    .doesNotContain("WITH CHECK");
        }
        assertThat(found).isEqualTo(allowed);
    }

    @Test
    void courseRegistrationsAreTenantScopedAndOnlyOneActiveRegistrationPerUserAndOffering()
            throws IOException {
        String sql = Files.readString(V1);

        assertThat(sql)
                .contains(
                        "CREATE POLICY course_registrations_tenant_policy ON course_registrations")
                .contains("ALTER TABLE course_registrations FORCE ROW LEVEL SECURITY;")
                .contains("FOREIGN KEY (tenant_id, offering_id)")
                .contains("FOREIGN KEY (tenant_id, user_id)");
        assertThat(sql)
                .containsPattern(
                        "CREATE UNIQUE INDEX uk_course_registrations_active\\s+ON course_registrations"
                                + " \\(tenant_id, offering_id, user_id\\)\\s+WHERE status IN"
                                + " \\('PENDING', 'PARTICIPANT', 'WAITLIST'\\);");
    }
}
