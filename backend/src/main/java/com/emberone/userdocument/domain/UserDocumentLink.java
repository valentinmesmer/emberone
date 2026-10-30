/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument.domain;

import com.emberone.platform.navigation.domain.AppLinkType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "doc_document_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserDocumentLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private UserDocument document;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false)
    private AppLinkType linkType;

    @Column(name = "link_id", nullable = false)
    private Long linkId;
}
