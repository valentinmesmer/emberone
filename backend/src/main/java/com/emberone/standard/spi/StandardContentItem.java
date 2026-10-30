/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.spi;

import java.util.List;
import java.util.Map;

/**
 * One human-readable item of a standard's content, used by the admin "view / edit standard" UI to
 * show what a standard bundles (organization groups, permission groups, group mappings, workflow
 * approvals).
 *
 * @param domainKey the UI section this item belongs to (matches {@link StandardDomain#domainKey()}
 *     or a synthetic section key such as {@code "group-mappings"}).
 * @param configKey stable key of the template item within its section (used for targeted edits).
 * @param title display title (e.g. group name or workflow type label).
 * @param subtitle optional secondary line (e.g. raw group type, approver key), may be {@code null}.
 * @param permissionTypes for permission groups: granted {@code PermissionType} names; empty
 *     otherwise.
 * @param attributes raw editable values for prefilling edit forms (e.g. {@code description}, {@code
 *     autoApprove}, {@code enabled}); never {@code null}.
 * @param editable whether this item can be edited via a domain edit endpoint (false for
 *     system-derived).
 */
public record StandardContentItem(
        String domainKey,
        String configKey,
        String title,
        String subtitle,
        List<String> permissionTypes,
        Map<String, String> attributes,
        boolean editable) {

    public StandardContentItem {
        permissionTypes = permissionTypes == null ? List.of() : List.copyOf(permissionTypes);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
