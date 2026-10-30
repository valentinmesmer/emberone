/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument.service;

import com.emberone.platform.navigation.AppNavigationResolver;
import com.emberone.userdocument.domain.UserDocumentLink;
import com.emberone.userdocument.dto.UserDocumentLinkDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Resolves navigation paths for personal document links. */
@Service
@RequiredArgsConstructor
public class UserDocumentLinkResolver {

    private final AppNavigationResolver appNavigationResolver;

    public UserDocumentLinkDto toDto(UserDocumentLink link) {
        return UserDocumentLinkDto.builder()
                .id(link.getId())
                .linkType(link.getLinkType())
                .linkId(link.getLinkId())
                .navigationPath(
                        appNavigationResolver.resolveNavigationPath(
                                link.getLinkType(), link.getLinkId()))
                .build();
    }
}
