/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.spi;

import com.emberone.standard.domain.Standard;
import java.util.List;

/**
 * Typed SPI implemented by every domain that participates in the Standards module (organization
 * groups, permission groups, workflow type mappings, workflow definitions).
 *
 * <p>The Standards module orchestrates standard lifecycle (clone / assign / materialize) and
 * delegates the domain-specific work to the registered {@code StandardDomain} beans. New areas dock
 * onto the same SPI without structural changes.
 *
 * <p>Templates live in the standard library ({@code std_*} tables, keyed by {@code standard_id} +
 * {@code config_key}); instances are the existing per-org domain tables, each carrying a {@code
 * config_key} and {@code standard_origin_id}.
 *
 * <p>Propagation of template <em>edits</em> onto instance rows is handled inside each domain's own
 * template-editing service; this SPI covers the lifecycle steps the core module drives.
 */
public interface StandardDomain {

    /** Stable identifier of this domain (e.g. {@code "organization-groups"}). Used for logging. */
    String domainKey();

    /**
     * Copy all template items of {@code source} into {@code target} (clone "Klonen &amp;
     * anpassen").
     */
    void copyTemplates(Standard source, Standard target);

    /**
     * Materialize this standard's template items into fresh per-org instance rows for a newly
     * created org.
     */
    void materializeForOrg(Long organizationId, Standard standard);

    /**
     * Re-materialize an existing org after its effective standard changed: add new items, rebase
     * same-key instances, and remove items no longer present in the standard.
     */
    void reassignForOrg(Long organizationId, Standard standard);

    /**
     * Describe this domain's template items of {@code standard} for the admin "view / edit
     * standard" UI. Read-only; returns an empty list by default for domains that do not surface
     * content yet.
     */
    default List<StandardContentItem> describeContent(Standard standard) {
        return List.of();
    }
}
