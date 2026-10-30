/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.list;

import com.emberone.certification.service.CertificationStatusService;
import com.emberone.platform.web.list.options.ListFilterOptionsDto;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Filter options for {@code GET …/certifications/status-overview}. */
@Component
@RequiredArgsConstructor
public class CertificationOrganizationListFilterOptions {

    private final CertificationStatusService certificationStatusService;

    public ListFilterOptionsDto forOrganization(long organizationId) {
        return new ListFilterOptionsDto(
                Map.of(
                        "type",
                        certificationStatusService.listCertDefinitionFilterOptionsForOrganization(
                                organizationId)));
    }
}
