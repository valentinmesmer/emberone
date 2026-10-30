/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.spi;

import com.emberone.standard.domain.Standard;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Holds the registered {@link StandardDomain} beans and fans lifecycle calls out to all of them.
 * Spring injects every {@code StandardDomain} on the classpath, so new domains register
 * automatically.
 */
@Slf4j
@Component
public class StandardDomainRegistry {

    private final List<StandardDomain> domains;

    public StandardDomainRegistry(List<StandardDomain> domains) {
        this.domains = List.copyOf(domains);
        log.info(
                "Registered {} standard domain(s): {}",
                domains.size(),
                domains.stream().map(StandardDomain::domainKey).toList());
    }

    public List<StandardDomain> domains() {
        return domains;
    }

    public void copyTemplates(Standard source, Standard target) {
        domains.forEach(domain -> domain.copyTemplates(source, target));
    }

    public void materializeForOrg(Long organizationId, Standard standard) {
        domains.forEach(domain -> domain.materializeForOrg(organizationId, standard));
    }

    public void reassignForOrg(Long organizationId, Standard standard) {
        domains.forEach(domain -> domain.reassignForOrg(organizationId, standard));
    }

    public List<StandardContentItem> describeContent(Standard standard) {
        List<StandardContentItem> all = new ArrayList<>();
        domains.forEach(domain -> all.addAll(domain.describeContent(standard)));
        return all;
    }
}
