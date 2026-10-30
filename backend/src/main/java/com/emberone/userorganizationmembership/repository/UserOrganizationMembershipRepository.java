/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.repository;

import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface UserOrganizationMembershipRepository
        extends JpaRepository<UserOrganizationMembership, Long>,
                JpaSpecificationExecutor<UserOrganizationMembership> {

    List<UserOrganizationMembership> findByUser_Id(Long userId);

    List<UserOrganizationMembership> findByUser_IdAndOrganization_Id(
            Long userId, Long organizationId);

    @Query(
            "SELECT m FROM UserOrganizationMembership m WHERE m.user.id = :userId AND m.organization.id = :organizationId "
                    + "AND m.startDate <= :at AND (m.endDate IS NULL OR m.endDate > :at)")
    Optional<UserOrganizationMembership> findActiveByUser_IdAndOrganization_Id(
            @Param("userId") Long userId,
            @Param("organizationId") Long organizationId,
            @Param("at") Instant at);

    @Query(
            "SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM UserOrganizationMembership m "
                    + "WHERE m.user.id = :userId AND m.organization.id = :organizationId "
                    + "AND m.startDate <= :at AND (m.endDate IS NULL OR m.endDate > :at)")
    boolean existsActiveByUser_IdAndOrganization_Id(
            @Param("userId") Long userId,
            @Param("organizationId") Long organizationId,
            @Param("at") Instant at);

    @Query(
            "SELECT m FROM UserOrganizationMembership m WHERE m.organization.id = :organizationId "
                    + "AND m.startDate <= :at AND (m.endDate IS NULL OR m.endDate > :at)")
    List<UserOrganizationMembership> findActiveByOrganization_Id(
            @Param("organizationId") Long organizationId, @Param("at") Instant at);

    @Query("SELECT m FROM UserOrganizationMembership m JOIN FETCH m.user WHERE m.id IN :ids")
    List<UserOrganizationMembership> findWithUserByIdIn(@Param("ids") Collection<Long> ids);

    @Query(
            "SELECT m FROM UserOrganizationMembership m JOIN m.user u WHERE m.organization.id = :organizationId "
                    + "AND m.startDate <= :at AND (m.endDate IS NULL OR m.endDate > :at) "
                    + "ORDER BY u.lastName ASC, u.firstName ASC, m.id ASC")
    Page<UserOrganizationMembership> findActiveByOrganization_IdOrderByUserName(
            @Param("organizationId") Long organizationId,
            @Param("at") Instant at,
            Pageable pageable);

    @Query(
            "SELECT m FROM UserOrganizationMembership m JOIN FETCH m.user JOIN FETCH m.organization "
                    + "WHERE m.organization.id IN :organizationIds "
                    + "AND m.startDate <= :at AND (m.endDate IS NULL OR m.endDate > :at)")
    List<UserOrganizationMembership> findActiveByOrganization_IdIn(
            @Param("organizationIds") Collection<Long> organizationIds, @Param("at") Instant at);

    @Query(
            "SELECT m FROM UserOrganizationMembership m WHERE m.user.id = :userId "
                    + "AND m.startDate <= :at AND (m.endDate IS NULL OR m.endDate > :at)")
    List<UserOrganizationMembership> findActiveByUser_Id(
            @Param("userId") Long userId, @Param("at") Instant at);

    @Query(
            "SELECT m FROM UserOrganizationMembership m JOIN FETCH m.organization "
                    + "WHERE m.user.id = :userId "
                    + "AND m.startDate <= :at AND (m.endDate IS NULL OR m.endDate > :at) "
                    + "ORDER BY m.organization.id ASC, m.id ASC")
    Page<UserOrganizationMembership> findActivePageByUser_Id(
            @Param("userId") Long userId, @Param("at") Instant at, Pageable pageable);

    @Query(
            "SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM UserOrganizationMembership m "
                    + "WHERE m.user.id = :userId "
                    + "AND m.startDate <= :at AND (m.endDate IS NULL OR m.endDate > :at)")
    boolean existsActiveByUser_Id(@Param("userId") Long userId, @Param("at") Instant at);

    Optional<UserOrganizationMembership> findByUser_IdAndMainTrue(Long userId);

    @Query(
            "SELECT m FROM UserOrganizationMembership m WHERE m.user.id = :userId AND m.main = true "
                    + "AND m.startDate <= :at AND (m.endDate IS NULL OR m.endDate > :at)")
    Optional<UserOrganizationMembership> findActiveByUser_IdAndMainTrue(
            @Param("userId") Long userId, @Param("at") Instant at);

    @Query(
            "SELECT m FROM UserOrganizationMembership m WHERE m.user.id = :userId "
                    + "AND m.startDate <= :at AND (m.endDate IS NULL OR m.endDate > :at) ORDER BY m.id ASC")
    Optional<UserOrganizationMembership> findFirstActiveByUser_IdOrderByIdAsc(
            @Param("userId") Long userId, @Param("at") Instant at);

    @Query(
            "SELECT m FROM UserOrganizationMembership m JOIN FETCH m.user JOIN FETCH m.organization "
                    + "WHERE m.endDate <= :at AND m.endProcessedAt IS NULL")
    List<UserOrganizationMembership> findDueEndedUnprocessed(@Param("at") Instant at);

    @Query(
            value =
                    "SELECT u.id FROM UserOrganizationMembership m JOIN m.user u LEFT JOIN u.contactInformation c "
                            + "WHERE m.organization.id = :organizationId "
                            + "AND m.startDate <= :at AND (m.endDate IS NULL OR m.endDate > :at) "
                            + "AND u.deactivatedAt IS NULL "
                            + "AND (:searchPattern IS NULL OR LOWER(u.firstName) LIKE :searchPattern "
                            + "OR LOWER(u.lastName) LIKE :searchPattern OR LOWER(u.username) LIKE :searchPattern "
                            + "OR LOWER(c.privateEmail) LIKE :searchPattern) "
                            + "ORDER BY LOWER(u.lastName) ASC, LOWER(u.firstName) ASC, u.id ASC",
            countQuery =
                    "SELECT COUNT(u.id) FROM UserOrganizationMembership m JOIN m.user u LEFT JOIN u.contactInformation c "
                            + "WHERE m.organization.id = :organizationId "
                            + "AND m.startDate <= :at AND (m.endDate IS NULL OR m.endDate > :at) "
                            + "AND u.deactivatedAt IS NULL "
                            + "AND (:searchPattern IS NULL OR LOWER(u.firstName) LIKE :searchPattern "
                            + "OR LOWER(u.lastName) LIKE :searchPattern OR LOWER(u.username) LIKE :searchPattern "
                            + "OR LOWER(c.privateEmail) LIKE :searchPattern)")
    Page<Long> findActiveUserIdsByOrganizationId(
            @Param("organizationId") Long organizationId,
            @Param("at") Instant at,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);

    @Query(
            "SELECT m FROM UserOrganizationMembership m JOIN FETCH m.organization "
                    + "WHERE m.user.id IN :userIds "
                    + "AND m.startDate <= :at AND (m.endDate IS NULL OR m.endDate > :at)")
    List<UserOrganizationMembership> findActiveByUser_IdIn(
            @Param("userIds") Collection<Long> userIds, @Param("at") Instant at);
}
