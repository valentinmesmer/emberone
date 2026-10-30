/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument.service;

import com.emberone.userdocument.domain.UserDocument;
import com.emberone.userdocument.domain.UserDocumentLink;
import com.emberone.userdocument.domain.UserDocumentTag;
import com.emberone.userdocument.dto.UserDocumentLinkInput;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Applies tags and links during trusted system promotions (onboarding, workflows) without pulling
 * in user-facing authorization dependencies.
 */
@Component
public class UserDocumentSystemWriter {

    public void applyTags(UserDocument document, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            if (!StringUtils.hasText(tag)) {
                continue;
            }
            String trimmed = tag.trim();
            if (trimmed.length() > 128) {
                throw new IllegalArgumentException("Tag must be at most 128 characters");
            }
            normalized.add(trimmed);
        }
        for (String tag : normalized) {
            UserDocumentTag entity =
                    UserDocumentTag.builder()
                            .tenantId(document.getTenantId())
                            .document(document)
                            .tag(tag)
                            .build();
            document.getTags().add(entity);
        }
    }

    public void applyTagIfAbsent(UserDocument document, String tag) {
        if (!StringUtils.hasText(tag)) {
            return;
        }
        String normalizedTag = tag.trim();
        if (normalizedTag.length() > 128) {
            throw new IllegalArgumentException("Tag must be at most 128 characters");
        }
        boolean hasTag =
                document.getTags().stream()
                        .anyMatch(existing -> normalizedTag.equals(existing.getTag()));
        if (!hasTag) {
            document.getTags()
                    .add(
                            UserDocumentTag.builder()
                                    .tenantId(document.getTenantId())
                                    .document(document)
                                    .tag(normalizedTag)
                                    .build());
        }
    }

    public void applyTrustedLinks(UserDocument document, List<UserDocumentLinkInput> links) {
        if (links == null || links.isEmpty()) {
            return;
        }
        Set<String> seen = new LinkedHashSet<>();
        for (UserDocumentLinkInput input : links) {
            if (input == null || input.getLinkType() == null || input.getLinkId() == null) {
                throw new IllegalArgumentException("Each link requires linkType and linkId");
            }
            String key = input.getLinkType().name() + ":" + input.getLinkId();
            if (!seen.add(key)) {
                continue;
            }
            UserDocumentLink entity =
                    UserDocumentLink.builder()
                            .tenantId(document.getTenantId())
                            .document(document)
                            .linkType(input.getLinkType())
                            .linkId(input.getLinkId())
                            .build();
            document.getLinks().add(entity);
        }
    }
}
