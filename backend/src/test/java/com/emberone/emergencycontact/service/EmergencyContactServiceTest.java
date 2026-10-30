/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.emergencycontact.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.emergencycontact.domain.UserEmergencyContact;
import com.emberone.emergencycontact.dto.CreateEmergencyContactRequest;
import com.emberone.emergencycontact.dto.EmergencyContactDto;
import com.emberone.emergencycontact.dto.ReorderEmergencyContactsRequest;
import com.emberone.emergencycontact.exception.EmergencyContactNotFoundException;
import com.emberone.emergencycontact.exception.InvalidEmergencyContactOrderException;
import com.emberone.emergencycontact.mapper.EmergencyContactMapper;
import com.emberone.emergencycontact.repository.UserEmergencyContactRepository;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmergencyContactServiceTest {

    @Mock UserEmergencyContactRepository repository;
    @Mock UserRepository userRepository;
    @Mock EmergencyContactMapper mapper;

    @Test
    void createAssignsNextPriorityTenantAndNormalizesFields() {
        EmergencyContactService service =
                new EmergencyContactService(repository, userRepository, mapper);
        User user = User.builder().id(1L).tenantId(7L).build();
        UserEmergencyContact contact = contact(null, 0);
        contact.setFirstName(" Ada ");
        contact.setLastName(" Lovelace ");
        contact.setStreet(" Main 1 ");
        contact.setCity(" Berlin ");
        contact.setPostalCode(" 10115 ");
        contact.setCountry(" DE ");
        contact.setPhone(" +49123 ");
        contact.setEmail(" ");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.findMaxPriorityByUserId(1L)).thenReturn(2);
        when(mapper.fromCreateRequest(any())).thenReturn(contact);
        when(repository.save(contact)).thenReturn(contact);
        when(mapper.toDto(contact)).thenReturn(EmergencyContactDto.builder().build());

        service.createEmergencyContact(1L, CreateEmergencyContactRequest.builder().build());

        assertThat(contact.getUser()).isSameAs(user);
        assertThat(contact.getTenantId()).isEqualTo(7L);
        assertThat(contact.getPriority()).isEqualTo(3);
        assertThat(contact.getFirstName()).isEqualTo("Ada");
        assertThat(contact.getEmail()).isNull();
    }

    @Test
    void reorderRejectsMissingDuplicateAndForeignIds() {
        EmergencyContactService service =
                new EmergencyContactService(repository, userRepository, mapper);
        when(repository.findByUser_IdOrderByPriorityAscIdAsc(1L))
                .thenReturn(List.of(contact(10L, 1), contact(11L, 2)));

        assertThatThrownBy(
                        () ->
                                service.reorderEmergencyContacts(
                                        1L,
                                        ReorderEmergencyContactsRequest.builder()
                                                .contactIds(List.of(10L))
                                                .build()))
                .isInstanceOf(InvalidEmergencyContactOrderException.class);
        assertThatThrownBy(
                        () ->
                                service.reorderEmergencyContacts(
                                        1L,
                                        ReorderEmergencyContactsRequest.builder()
                                                .contactIds(List.of(10L, 10L))
                                                .build()))
                .isInstanceOf(InvalidEmergencyContactOrderException.class);
        assertThatThrownBy(
                        () ->
                                service.reorderEmergencyContacts(
                                        1L,
                                        ReorderEmergencyContactsRequest.builder()
                                                .contactIds(List.of(10L, 99L))
                                                .build()))
                .isInstanceOf(InvalidEmergencyContactOrderException.class);
    }

    @Test
    void reorderUsesTemporaryPrioritiesBeforeFinalOrder() {
        EmergencyContactService service =
                new EmergencyContactService(repository, userRepository, mapper);
        UserEmergencyContact first = contact(10L, 1);
        UserEmergencyContact second = contact(11L, 2);
        when(repository.findByUser_IdOrderByPriorityAscIdAsc(1L))
                .thenReturn(List.of(first, second));
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDto(any()))
                .thenAnswer(
                        invocation ->
                                EmergencyContactDto.builder()
                                        .id(
                                                ((UserEmergencyContact) invocation.getArgument(0))
                                                        .getId())
                                        .build());

        List<EmergencyContactDto> result =
                service.reorderEmergencyContacts(
                        1L,
                        ReorderEmergencyContactsRequest.builder()
                                .contactIds(List.of(11L, 10L))
                                .build());

        assertThat(second.getPriority()).isEqualTo(1);
        assertThat(first.getPriority()).isEqualTo(2);
        assertThat(result).extracting(EmergencyContactDto::getId).containsExactly(11L, 10L);
        var order = inOrder(repository);
        order.verify(repository).saveAll(any());
        order.verify(repository).flush();
        order.verify(repository).saveAll(any());
    }

    @Test
    void deleteRequiresOwnedContactAndRenumbersRemainingContacts() {
        EmergencyContactService service =
                new EmergencyContactService(repository, userRepository, mapper);
        when(repository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteEmergencyContact(1L, 10L))
                .isInstanceOf(EmergencyContactNotFoundException.class);

        UserEmergencyContact removed = contact(10L, 1);
        UserEmergencyContact remaining = contact(11L, 2);
        when(repository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(removed));
        when(repository.findByUser_IdOrderByPriorityAscIdAsc(1L)).thenReturn(List.of(remaining));
        service.deleteEmergencyContact(1L, 10L);

        verify(repository).delete(removed);
        assertThat(remaining.getPriority()).isEqualTo(1);
        verify(repository).flush();
    }

    @Test
    void getEmergencyContactsForOrganizationDelegatesToRepository() {
        EmergencyContactService service =
                new EmergencyContactService(repository, userRepository, mapper);
        UserEmergencyContact contact = contact(5L, 1);
        when(repository.findByOrganizationId(10L)).thenReturn(List.of(contact));
        when(mapper.toDto(contact)).thenReturn(EmergencyContactDto.builder().id(5L).build());

        List<EmergencyContactDto> result = service.getEmergencyContactsForOrganization(10L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(5L);
    }

    private UserEmergencyContact contact(Long id, int priority) {
        return UserEmergencyContact.builder()
                .id(id)
                .priority(priority)
                .firstName("A")
                .lastName("B")
                .street("S")
                .city("C")
                .postalCode("P")
                .country("DE")
                .phone("+491")
                .build();
    }
}
