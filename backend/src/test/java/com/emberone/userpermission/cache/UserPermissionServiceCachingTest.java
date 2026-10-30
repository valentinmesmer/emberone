/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userpermission.cache;

import static org.mockito.Mockito.*;

import com.emberone.permission.cache.PermissionCacheEvictionListener;
import com.emberone.permission.cache.UserPermissionCacheEvictor;
import com.emberone.permission.event.UserPermissionsChangedEvent;
import com.emberone.permissiongroupmapping.repository.PermissionGroupMappingRepository;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
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
@ContextConfiguration(classes = UserPermissionServiceCachingTest.TestConfig.class)
class UserPermissionServiceCachingTest {

    private static final Long TENANT_ID = 7L;
    private static final Long USER_ID = 42L;

    @Autowired CachedUserPermissionLoader cachedUserPermissionLoader;

    @Autowired UserOrganizationGroupMembershipRepository membershipRepository;

    @Autowired PermissionCacheEvictionListener permissionCacheEvictionListener;

    @Test
    void loadUsesCacheUntilEvicted() {
        when(membershipRepository.findByUserId(USER_ID)).thenReturn(java.util.List.of());

        cachedUserPermissionLoader.load(TENANT_ID, USER_ID);
        cachedUserPermissionLoader.load(TENANT_ID, USER_ID);
        verify(membershipRepository, times(1)).findByUserId(USER_ID);

        permissionCacheEvictionListener.onUserPermissionsChanged(
                new UserPermissionsChangedEvent(TENANT_ID, USER_ID));

        cachedUserPermissionLoader.load(TENANT_ID, USER_ID);
        verify(membershipRepository, times(2)).findByUserId(USER_ID);
    }

    @Configuration
    @EnableCaching
    @Import({
        CachedUserPermissionLoader.class,
        UserPermissionCacheEvictor.class,
        PermissionCacheEvictionListener.class
    })
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("userPermissions");
        }

        @Bean
        UserOrganizationGroupMembershipRepository membershipRepository() {
            return mock(UserOrganizationGroupMembershipRepository.class);
        }

        @Bean
        UserOrganizationMembershipRepository organizationMembershipRepository() {
            return mock(UserOrganizationMembershipRepository.class);
        }

        @Bean
        PermissionGroupMappingRepository mappingRepository() {
            return mock(PermissionGroupMappingRepository.class);
        }
    }
}
