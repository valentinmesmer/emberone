/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list.options;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ListFilterOptionsServiceTest {

    @Mock ListFilterOptionsRegistry registry;

    @InjectMocks ListFilterOptionsService service;

    @Test
    void invalidIncludeReturnsBadRequest() {
        when(registry.find(ListFilterOptionsScope.ADMIN, "nope"))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.resolve(ListFilterOptionsContext.admin(), List.of("nope")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        ex ->
                                assertThat(((ResponseStatusException) ex).getStatusCode().value())
                                        .isEqualTo(400));
    }

    @Test
    void resolvesRegisteredProvider() {
        ListFilterOptionsProvider provider =
                new ListFilterOptionsProvider() {
                    @Override
                    public String key() {
                        return "appRole";
                    }

                    @Override
                    public Set<ListFilterOptionsScope> scopes() {
                        return Set.of(ListFilterOptionsScope.ADMIN);
                    }

                    @Override
                    public List<ListFilterOptionDto> resolve(ListFilterOptionsContext context) {
                        return List.of(new ListFilterOptionDto("USER", "Benutzer"));
                    }
                };
        when(registry.find(ListFilterOptionsScope.ADMIN, "appRole"))
                .thenReturn(java.util.Optional.of(provider));

        var result = service.resolve(ListFilterOptionsContext.admin(), List.of("appRole"));

        assertThat(result.options().get("appRole"))
                .containsExactly(new ListFilterOptionDto("USER", "Benutzer"));
    }
}
