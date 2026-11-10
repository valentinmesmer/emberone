/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.standard.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.domain.OrganizationTypeDefinition;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.cache.CacheNames;
import com.emberone.standard.repository.StandardAssignmentRepository;
import com.emberone.standard.repository.StandardRepository;
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

/**
 * Production Redis caches reject {@code null}; the cache manager here does the same, so a missing
 * {@code unless} on a nullable loader fails this test instead of only logging in production.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EffectiveStandardOrganizationCacheTest.TestConfig.class)
class EffectiveStandardOrganizationCacheTest {

    private static final Long TENANT_ID = 10L;
    private static final Long ORG_ID = 5L;
    private static final Long TYPE_ID = 2L;

    @Autowired EffectiveStandardOrganizationCache cache;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired StandardAssignmentRepository assignmentRepository;
    @Autowired CacheManager cacheManager;

    @Test
    void orgWithoutAssignedStandardIsNotCachedAndDoesNotFail() {
        OrganizationTypeDefinition type = OrganizationTypeDefinition.builder().id(TYPE_ID).build();
        Organization org = Organization.builder().id(ORG_ID).tenantId(TENANT_ID).type(type).build();
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
        when(assignmentRepository.findByTenantIdAndOrganizationType_Id(TENANT_ID, TYPE_ID))
                .thenReturn(Optional.empty());

        assertThat(cache.loadCached(TENANT_ID, ORG_ID)).isNull();

        var orgCache = cacheManager.getCache(CacheNames.EFFECTIVE_STANDARD_BY_ORG);
        assertThat(orgCache).isNotNull();
        assertThat(orgCache.get(TENANT_ID + ":" + ORG_ID)).isNull();
    }

    @Configuration
    @EnableCaching
    @Import({EffectiveStandardOrganizationCache.class, EffectiveStandardTypeCache.class})
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            ConcurrentMapCacheManager manager =
                    new ConcurrentMapCacheManager(
                            CacheNames.EFFECTIVE_STANDARD_BY_ORG_TYPE,
                            CacheNames.EFFECTIVE_STANDARD_BY_ORG);
            manager.setAllowNullValues(false);
            return manager;
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
        StandardRepository standardRepository() {
            return mock(StandardRepository.class);
        }
    }
}
