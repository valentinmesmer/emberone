/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.event.domain.EventType;
import com.emberone.event.domain.ServiceEvent;
import com.emberone.event.dto.CreateServiceEventRequest;
import com.emberone.event.dto.ServiceEventDto;
import com.emberone.event.event.EventDeletingEvent;
import com.emberone.event.event.EventPromotedFromAppointmentEvent;
import com.emberone.event.exception.EventNotFoundException;
import com.emberone.event.exception.EventValidationException;
import com.emberone.event.mapper.EventMapper;
import com.emberone.event.repository.OperationEventRepository;
import com.emberone.event.repository.ServiceEventRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.time.OrganizationWallClockTime;
import com.emberone.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock ServiceEventRepository serviceEventRepository;
    @Mock OperationEventRepository operationEventRepository;
    @Mock EventMapper mapper;
    @Mock com.emberone.event.mapper.EventDtoAssembler eventDtoAssembler;
    @Mock OrganizationRepository organizationRepository;
    @Mock UserRepository userRepository;
    @Mock TokenService tokenService;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks EventService service;

    @BeforeEach
    void setUp() {
        OrganizationWallClockTime.configure(ZoneId.of("Europe/Berlin"));
    }

    private static CreateServiceEventRequest request() {
        CreateServiceEventRequest request = new CreateServiceEventRequest();
        request.setOrganizationId(10L);
        request.setStart(LocalDateTime.of(2026, 3, 1, 10, 0).toInstant(ZoneOffset.UTC));
        request.setEnd(LocalDateTime.of(2026, 3, 1, 12, 0).toInstant(ZoneOffset.UTC));
        request.setTopic("Übung");
        request.setLocation("GH");
        return request;
    }

    @Test
    void createServiceEventValidatesOrganizationId() {
        CreateServiceEventRequest request = request();
        request.setOrganizationId(99L);
        assertThatThrownBy(() -> service.createServiceEvent(1L, 10L, request))
                .isInstanceOf(EventValidationException.class);
    }

    @Test
    void createServiceEventPersistsAndReturnsDto() {
        CreateServiceEventRequest request = request();
        Organization org = Organization.builder().id(10L).tenantId(1L).build();
        when(tokenService.getTenantId()).thenReturn(1L);
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(org));
        when(serviceEventRepository.save(any(ServiceEvent.class)))
                .thenAnswer(
                        inv -> {
                            ServiceEvent e = inv.getArgument(0);
                            e.setId(5L);
                            return e;
                        });
        when(mapper.toDto(any(ServiceEvent.class)))
                .thenReturn(ServiceEventDto.builder().id(5L).build());

        assertThat(service.createServiceEvent(1L, 10L, request).getId()).isEqualTo(5L);
    }

    @Test
    void createServiceEventPublishesPromoteEventWhenSourceAppointmentPresent() {
        CreateServiceEventRequest request = request();
        request.setPromoteFromAppointmentId(42L);
        Organization org = Organization.builder().id(10L).tenantId(1L).build();
        when(tokenService.getTenantId()).thenReturn(1L);
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(org));
        when(serviceEventRepository.save(any(ServiceEvent.class)))
                .thenAnswer(
                        inv -> {
                            ServiceEvent e = inv.getArgument(0);
                            e.setId(5L);
                            return e;
                        });
        when(mapper.toDto(any(ServiceEvent.class)))
                .thenReturn(ServiceEventDto.builder().id(5L).build());

        service.createServiceEvent(1L, 10L, request);

        ArgumentCaptor<EventPromotedFromAppointmentEvent> captor =
                ArgumentCaptor.forClass(EventPromotedFromAppointmentEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue())
                .isEqualTo(
                        new EventPromotedFromAppointmentEvent(
                                10L, 42L, EventType.SERVICE, request.getStart(), request.getEnd()));
    }

    @Test
    void createServiceEventRejectsCrossTenantLeader() {
        CreateServiceEventRequest request = request();
        request.setLeaderId(99L);
        Organization org = Organization.builder().id(10L).tenantId(1L).build();
        when(tokenService.getTenantId()).thenReturn(1L);
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(org));
        when(userRepository.findById(99L))
                .thenReturn(
                        Optional.of(
                                com.emberone.user.domain.User.builder()
                                        .id(99L)
                                        .tenantId(2L)
                                        .build()));

        assertThatThrownBy(() -> service.createServiceEvent(1L, 10L, request))
                .isInstanceOf(EventValidationException.class)
                .hasMessageContaining("Leader does not belong to the tenant");
    }

    @Test
    void getServiceEventReturnsDtoWhenEventBelongsToOrganization() {
        when(tokenService.getTenantId()).thenReturn(1L);
        Organization org = Organization.builder().id(10L).tenantId(1L).build();
        ServiceEvent event = ServiceEvent.builder().id(5L).tenantId(1L).organization(org).build();
        when(serviceEventRepository.findWithLeaderByIdIn(List.of(5L))).thenReturn(List.of(event));
        when(mapper.toDto(event)).thenReturn(ServiceEventDto.builder().id(5L).build());

        assertThat(service.getServiceEvent(10L, 5L).getId()).isEqualTo(5L);
    }

    @Test
    void getServiceEventThrowsWhenEventNotInOrganization() {
        when(tokenService.getTenantId()).thenReturn(1L);
        when(serviceEventRepository.findWithLeaderByIdIn(List.of(5L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.getServiceEvent(10L, 5L))
                .isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void deleteServiceEventPublishesDeletingEventAndRemovesEntity() {
        when(tokenService.getTenantId()).thenReturn(1L);
        ServiceEvent event = ServiceEvent.builder().id(5L).build();
        when(serviceEventRepository.findByIdAndOrganizationId(5L, 10L))
                .thenReturn(Optional.of(event));

        service.deleteServiceEvent(1L, 10L, 5L);

        ArgumentCaptor<EventDeletingEvent> captor =
                ArgumentCaptor.forClass(EventDeletingEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new EventDeletingEvent(1L, EventType.SERVICE, 5L));
        verify(serviceEventRepository).delete(event);
    }

    @Test
    void requireEventStartDateReturnsCalendarDayOfStartInOrganizationZone() {
        ServiceEvent event =
                ServiceEvent.builder()
                        .id(5L)
                        .start(LocalDateTime.of(2026, 8, 12, 22, 0).toInstant(ZoneOffset.UTC))
                        .build();
        when(serviceEventRepository.findByIdAndOrganizationId(5L, 10L))
                .thenReturn(Optional.of(event));

        assertThat(service.requireEventStartDate(EventType.SERVICE, 5L, 10L))
                .isEqualTo(LocalDate.of(2026, 8, 13));
    }
}
