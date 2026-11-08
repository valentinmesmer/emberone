/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.rankrequest;

import com.emberone.platform.ai.extraction.DocumentExtractionResult;
import com.emberone.platform.ai.extraction.DocumentExtractionSchema;
import com.emberone.platform.ai.extraction.ExtractionHint;
import com.emberone.platform.ai.extraction.ExtractionJson;
import com.emberone.platform.ai.extraction.PreparedExtraction;
import com.emberone.platform.security.TokenService;
import com.emberone.rank.dto.TenantRankDto;
import com.emberone.rank.service.TenantRankCatalogService;
import com.emberone.workflow.domain.WorkflowType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Extraction schema for {@link WorkflowType#RANK_REQUEST}: the rank (from the tenant catalog)
 * proven by a promotion certificate or similar.
 */
@Component
@RequiredArgsConstructor
public class RankRequestExtractionSchema implements DocumentExtractionSchema {

    private final TenantRankCatalogService rankCatalogService;
    private final TokenService tokenService;

    @Override
    public String type() {
        return WorkflowType.RANK_REQUEST.name();
    }

    @Override
    public PreparedExtraction prepare(long userId, Long selectionId) {
        Map<String, TenantRankDto> byKey = new LinkedHashMap<>();
        for (TenantRankDto rank :
                rankCatalogService.listActiveRanks(tokenService.requireTenantId())) {
            if (rank.getConfigKey() != null) {
                byKey.put(rank.getConfigKey(), rank);
            }
        }
        Map<String, String> keyToName = new LinkedHashMap<>();
        byKey.forEach((key, rank) -> keyToName.put(key, rank.getName()));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put(
                "rank",
                ExtractionJson.nullableEnum(
                        byKey.keySet(),
                        "The rank the document confirms for the person (for example a promotion"
                                + " certificate). Null if no catalog rank is clearly named."));
        return new PreparedExtraction(
                "rank_request_extraction",
                "The document proves a rank of the person. Catalog of ranks:\n"
                        + ExtractionJson.catalogList(keyToName),
                ExtractionJson.object(properties),
                answer -> validate(answer, byKey));
    }

    /** Deterministic plausibility rules; invalid values are dropped, never repaired. */
    static DocumentExtractionResult validate(JsonNode answer, Map<String, TenantRankDto> byKey) {
        Map<String, Object> fields = new LinkedHashMap<>();
        List<ExtractionHint> hints = new ArrayList<>();
        JsonNode node = answer.get("rank");
        if (node != null && node.isString()) {
            TenantRankDto rank = byKey.get(node.asString());
            if (rank != null) {
                fields.put("tenantRankId", rank.getId());
            } else {
                hints.add(new ExtractionHint(ExtractionHint.VALUE_REJECTED, "tenantRankId"));
            }
        }
        if (fields.isEmpty()) {
            hints.add(new ExtractionHint(ExtractionHint.NOTHING_RECOGNIZED, null));
        }
        return new DocumentExtractionResult(fields, hints);
    }
}
