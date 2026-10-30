/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.event.OrganizationCreatedEvent;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.cache.CacheNames;
import com.emberone.standard.domain.StandardAssignment;
import com.emberone.standard.event.OrganizationStandardOverrideChangedEvent;
import com.emberone.standard.event.OrganizationTypeStandardAssignedEvent;
import com.emberone.standard.event.StandardEnabledModulesChangedEvent;
import com.emberone.standard.repository.StandardAssignmentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = StandardCacheEvictionListenerTest.TestConfig.class)
class StandardCacheEvictionListenerTest {

    private static final Long TENANT_ID = 10L;
    private static final Long ORG_ID = 5L;
    private static final Long TYPE_ID = 2L;
    private static final Long STANDARD_ID = 99L;

    @Autowired StandardCacheEvictionListener listener;

    @Autowired StandardOrganizationIdCache standardOrganizationIdCache;

    @Autowired OrganizationRepository organizationRepository;

    @Autowired StandardAssignmentRepository assignmentRepository;

    @Autowired CacheManager cacheManager;

    @Test
    void organizationCreatedEvictsOrganizationsForStandardAfterCommit() {
        when(assignmentRepository.findByTenantIdAndOrganizationType_Id(TENANT_ID, TYPE_ID))
                .thenReturn(
                        Optional.of(
                                StandardAssignment.builder()
                                        .tenantId(TENANT_ID)
                                        .standardId(STANDARD_ID)
                                        .build()));
        when(organizationRepository.findById(ORG_ID))
                .thenReturn(
                        Optional.of(Organization.builder().id(ORG_ID).tenantId(TENANT_ID).build()));
        when(assignmentRepository.findAllByTenantIdAndStandardId(TENANT_ID, STANDARD_ID))
                .thenReturn(List.of());
        when(organizationRepository.findByType_IdAndStandardIdIsNull(anyLong()))
                .thenReturn(List.of());
        when(organizationRepository.findByTenantIdAndStandardId(TENANT_ID, STANDARD_ID))
                .thenReturn(List.of());

        standardOrganizationIdCache.loadCached(TENANT_ID, STANDARD_ID);
        verify(assignmentRepository, times(1))
                .findAllByTenantIdAndStandardId(TENANT_ID, STANDARD_ID);

        listener.onOrganizationCreated(new OrganizationCreatedEvent(TENANT_ID, ORG_ID, TYPE_ID));

        standardOrganizationIdCache.loadCached(TENANT_ID, STANDARD_ID);
        verify(assignmentRepository, times(2))
                .findAllByTenantIdAndStandardId(TENANT_ID, STANDARD_ID);
    }

    @Test
    void typeStandardAssignedEvictsTypeAndOrganizationListCaches() {
        when(organizationRepository.findByType_IdAndStandardIdIsNull(TYPE_ID))
                .thenReturn(List.of(Organization.builder().id(ORG_ID).tenantId(TENANT_ID).build()));

        var typeCache = cacheManager.getCache(CacheNames.EFFECTIVE_STANDARD_BY_ORG_TYPE);
        assertThat(typeCache).isNotNull();
        typeCache.put(TENANT_ID + ":" + TYPE_ID, Optional.empty());

        listener.onOrganizationTypeStandardAssigned(
                new OrganizationTypeStandardAssignedEvent(TENANT_ID, TYPE_ID, 1L, STANDARD_ID));

        assertThat(typeCache.get(TENANT_ID + ":" + TYPE_ID)).isNull();
    }

    @Test
    void overrideChangedEvictsEffectiveOrganizationCacheKey() {
        var orgCache = cacheManager.getCache(CacheNames.EFFECTIVE_STANDARD_BY_ORG);
        assertThat(orgCache).isNotNull();
        orgCache.put(TENANT_ID + ":" + ORG_ID, Optional.empty());

        listener.onOrganizationStandardOverrideChanged(
                new OrganizationStandardOverrideChangedEvent(TENANT_ID, ORG_ID, 1L, STANDARD_ID));

        assertThat(orgCache.get(TENANT_ID + ":" + ORG_ID)).isNull();
    }

    @Test
    void enabledModulesChangedEvictsModulesCacheKey() {
        var modulesCache = cacheManager.getCache(CacheNames.ENABLED_MODULES_BY_STANDARD);
        assertThat(modulesCache).isNotNull();
        modulesCache.put(TENANT_ID + ":" + STANDARD_ID, java.util.Set.of());

        listener.onStandardEnabledModulesChanged(
                new StandardEnabledModulesChangedEvent(TENANT_ID, STANDARD_ID));

        assertThat(modulesCache.get(TENANT_ID + ":" + STANDARD_ID)).isNull();
    }

    @Configuration
    @EnableCaching
    @Import({
        StandardCacheEvictionListener.class,
        EffectiveStandardOrganizationCacheEvictor.class,
        EffectiveStandardTypeCache.class,
        StandardOrganizationIdCache.class,
        EnabledModulesCacheEvictor.class
    })
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    CacheNames.EFFECTIVE_STANDARD_BY_ORG_TYPE,
                    CacheNames.EFFECTIVE_STANDARD_BY_ORG,
                    CacheNames.ORGANIZATIONS_FOR_STANDARD,
                    CacheNames.ENABLED_MODULES_BY_STANDARD);
        }

        @Bean
        OrganizationRepository organizationRepository() {
            return mock(OrganizationRepository.class);
        }

        @Bean
        StandardAssignmentRepository assignmentRepository() {
            return mock(StandardAssignmentRepository.class);
        }

        @Bean
        com.emberone.standard.repository.StandardRepository standardRepository() {
            return mock(com.emberone.standard.repository.StandardRepository.class);
        }
    }
}
