/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.attachment.repository;

import com.emberone.attachment.domain.EntityAttachment;
import com.emberone.attachment.domain.EntityAttachmentType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface EntityAttachmentRepository extends JpaRepository<EntityAttachment, Long> {

    List<EntityAttachment> findByEntityTypeAndEntityId(
            EntityAttachmentType entityType, Long entityId);

    List<EntityAttachment> findByEntityTypeAndEntityIdAndOrganizationId(
            EntityAttachmentType entityType, Long entityId, Long organizationId);

    Optional<EntityAttachment> findByIdAndEntityTypeAndEntityIdAndOrganizationId(
            Long id, EntityAttachmentType entityType, Long entityId, Long organizationId);

    List<EntityAttachment> findByEntityTypeInAndUploadedAtBefore(
            Collection<EntityAttachmentType> entityTypes, Instant uploadedBefore);
}
