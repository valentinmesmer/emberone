/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument.repository;

import com.emberone.userdocument.domain.UserDocument;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface UserDocumentRepository
        extends JpaRepository<UserDocument, Long>, JpaSpecificationExecutor<UserDocument> {

    @Query(
            """
        SELECT DISTINCT t.tag FROM UserDocumentTag t
        WHERE t.document.userId = :userId
        """)
    List<String> findDistinctTagsForUser(@Param("userId") Long userId);

    @Query(
            """
        SELECT DISTINCT d.contentType FROM UserDocument d
        WHERE d.userId = :userId
        """)
    List<String> findDistinctContentTypesForUser(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"tags"})
    @Query("SELECT d FROM UserDocument d WHERE d.id IN :ids")
    List<UserDocument> findWithDetailsByIdIn(@Param("ids") Collection<Long> ids);

    Optional<UserDocument> findByIdAndUserId(Long id, Long userId);

    List<UserDocument> findByUserIdOrderByIdDesc(Long userId);

    @Query(
            """
        SELECT d FROM UserDocument d
        WHERE d.userId = :userId
          AND EXISTS (
              SELECT 1 FROM UserDocumentTag t
              WHERE t.document = d AND LOWER(t.tag) = LOWER(:tag)
          )
        ORDER BY d.id DESC
        """)
    List<UserDocument> findByUserIdAndTagOrderByIdDesc(
            @Param("userId") Long userId, @Param("tag") String tag);

    long countByUserIdAndViewedAtIsNull(Long userId);

    @Transactional
    @Modifying
    @Query(
            """
        UPDATE UserDocument d SET d.viewedAt = :viewedAt
        WHERE d.userId = :userId AND d.viewedAt IS NULL
        """)
    int markAllViewedForUser(@Param("userId") Long userId, @Param("viewedAt") Instant viewedAt);

    /**
     * Documents linked to one of the targets that no other link in {@code retainingLinkTypes} still
     * claims. A signed entry document referenced by {@code usr_entry_document_fulfillments} belongs
     * to the personal file and outlives its onboarding invitation.
     */
    String DOCUMENTS_OWNED_BY_LINK_TARGETS =
            """
            FROM doc_documents d
            WHERE EXISTS (
                SELECT 1 FROM doc_document_links l
                WHERE l.tenant_id = d.tenant_id
                  AND l.document_id = d.id
                  AND l.link_type = :linkType
                  AND l.link_id IN (:linkIds)
              )
              AND NOT EXISTS (
                SELECT 1 FROM doc_document_links o
                WHERE o.tenant_id = d.tenant_id
                  AND o.document_id = d.id
                  AND o.link_type IN (:retainingLinkTypes)
                  AND NOT (o.link_type = :linkType AND o.link_id IN (:linkIds))
              )
              AND NOT EXISTS (
                SELECT 1 FROM usr_entry_document_fulfillments f
                WHERE f.tenant_id = d.tenant_id
                  AND f.user_document_id = d.id
              )
            """;

    @Query(value = "SELECT d.storage_key " + DOCUMENTS_OWNED_BY_LINK_TARGETS, nativeQuery = true)
    List<String> findStorageKeysOfDocumentsOwnedByLinkTargets(
            @Param("linkType") String linkType,
            @Param("linkIds") Collection<Long> linkIds,
            @Param("retainingLinkTypes") Collection<String> retainingLinkTypes);

    @Transactional
    @Modifying(flushAutomatically = true)
    @Query(value = "DELETE " + DOCUMENTS_OWNED_BY_LINK_TARGETS, nativeQuery = true)
    int deleteDocumentsOwnedByLinkTargets(
            @Param("linkType") String linkType,
            @Param("linkIds") Collection<Long> linkIds,
            @Param("retainingLinkTypes") Collection<String> retainingLinkTypes);

    @Transactional
    @Modifying(flushAutomatically = true)
    @Query(
            value =
                    """
            DELETE FROM doc_document_links
            WHERE link_type = :linkType
              AND link_id IN (:linkIds)
            """,
            nativeQuery = true)
    int deleteLinksToTargets(
            @Param("linkType") String linkType, @Param("linkIds") Collection<Long> linkIds);

    @Query(
            value = "SELECT storage_key FROM doc_documents WHERE id IN (:documentIds)",
            nativeQuery = true)
    List<String> findStorageKeysByIdIn(@Param("documentIds") Collection<Long> documentIds);

    @Transactional
    @Modifying(flushAutomatically = true)
    @Query(value = "DELETE FROM doc_documents WHERE id IN (:documentIds)", nativeQuery = true)
    int deleteByIdIn(@Param("documentIds") Collection<Long> documentIds);
}
