/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.module.domain.Module;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.cache.RedisCacheSerializerFactory;
import com.emberone.platform.cache.value.*;
import com.emberone.userpermission.dto.UserPermissionDto;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;

class RedisCacheValueSerializationTest {

    private GenericJacksonJsonRedisSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = RedisCacheSerializerFactory.createValueSerializer();
    }

    @Test
    void userPermissionDto_roundTripsPermissionTypes() {
        UserPermissionDto original =
                UserPermissionDto.builder()
                        .permissions(
                                Map.of(
                                        10L,
                                        List.of(
                                                PermissionType.WORKFLOW_READ,
                                                PermissionType.USER_READ)))
                        .build();

        UserPermissionDto restored = roundTrip(original, UserPermissionDto.class);

        assertThat(restored.getPermissions()).containsKey(10L);
        assertThat(restored.getPermissions().get(10L).get(0)).isInstanceOf(PermissionType.class);
        assertThat(restored.getPermissions().get(10L))
                .containsExactly(PermissionType.WORKFLOW_READ, PermissionType.USER_READ);
    }

    @Test
    void tenantFederalStateCacheValue_roundTrips() {
        TenantFederalStateCacheValue restored =
                roundTrip(
                        new TenantFederalStateCacheValue(
                                com.emberone.federalstate.domain.FederalState.BAYERN),
                        TenantFederalStateCacheValue.class);
        assertThat(restored.federalState())
                .isEqualTo(com.emberone.federalstate.domain.FederalState.BAYERN);
    }

    @Test
    void tenantLocaleCacheValue_roundTrips() {
        TenantLocaleCacheValue restored =
                roundTrip(
                        new TenantLocaleCacheValue(SupportedLocale.DE),
                        TenantLocaleCacheValue.class);
        assertThat(restored.locale()).isEqualTo(SupportedLocale.DE);
    }

    @Test
    void organizationInTenantCacheValue_roundTrips() {
        assertThat(
                        roundTrip(
                                        new OrganizationInTenantCacheValue(true),
                                        OrganizationInTenantCacheValue.class)
                                .exists())
                .isTrue();
    }

    @Test
    void userActiveCacheValue_roundTrips() {
        assertThat(roundTrip(new UserActiveCacheValue(false), UserActiveCacheValue.class).active())
                .isFalse();
    }

    @Test
    void activeOrganizationMembershipCacheValue_roundTrips() {
        assertThat(
                        roundTrip(
                                        new ActiveOrganizationMembershipCacheValue(false),
                                        ActiveOrganizationMembershipCacheValue.class)
                                .active())
                .isFalse();
    }

    @Test
    void enabledModulesCacheValue_roundTrips() {
        Set<Module> modules = EnumSet.of(Module.CORE, Module.SERVICE);
        EnabledModulesCacheValue restored =
                roundTrip(new EnabledModulesCacheValue(modules), EnabledModulesCacheValue.class);
        assertThat(restored.modules()).containsExactlyInAnyOrder(Module.CORE, Module.SERVICE);
    }

    @Test
    void effectiveStandardIdCacheValue_roundTrips() {
        assertThat(
                        roundTrip(
                                        new EffectiveStandardIdCacheValue(42L),
                                        EffectiveStandardIdCacheValue.class)
                                .standardId())
                .isEqualTo(42L);
    }

    @Test
    void organizationIdsForStandardCacheValue_roundTrips() {
        OrganizationIdsForStandardCacheValue restored =
                roundTrip(
                        new OrganizationIdsForStandardCacheValue(List.of(1L, 2L, 10L)),
                        OrganizationIdsForStandardCacheValue.class);
        assertThat(restored.organizationIds()).containsExactly(1L, 2L, 10L);
    }

    private <T> T roundTrip(Object value, Class<T> type) {
        byte[] bytes = serializer.serialize(value);
        Object deserialized = serializer.deserialize(bytes);
        assertThat(deserialized).isInstanceOf(type);
        return type.cast(deserialized);
    }
}
