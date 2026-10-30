/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument.dto;

import com.emberone.platform.time.OrganizationWallClock;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDocumentDto {
    private Long id;
    private String title;
    private String description;
    private String fileName;
    private String contentType;
    private Long sizeBytes;
    private List<String> tags;
    private List<UserDocumentLinkDto> links;

    @OrganizationWallClock private Instant viewedAt;
}
