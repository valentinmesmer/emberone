/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.emergencycontact.service;

import com.emberone.emergencycontact.domain.UserEmergencyContact;
import com.emberone.emergencycontact.dto.CreateEmergencyContactRequest;
import com.emberone.emergencycontact.dto.EmergencyContactDto;
import com.emberone.emergencycontact.dto.ReorderEmergencyContactsRequest;
import com.emberone.emergencycontact.dto.UpdateEmergencyContactRequest;
import com.emberone.emergencycontact.exception.EmergencyContactNotFoundException;
import com.emberone.emergencycontact.exception.InvalidEmergencyContactOrderException;
import com.emberone.emergencycontact.mapper.EmergencyContactMapper;
import com.emberone.emergencycontact.repository.UserEmergencyContactRepository;
import com.emberone.user.domain.User;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmergencyContactService {

    private final UserEmergencyContactRepository emergencyContactRepository;
    private final UserRepository userRepository;
    private final EmergencyContactMapper mapper;

    @Transactional(readOnly = true)
    public List<EmergencyContactDto> getEmergencyContactsForUser(Long userId) {
        ensureUserExists(userId);
        return emergencyContactRepository.findByUser_IdOrderByPriorityAscIdAsc(userId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmergencyContactDto> getEmergencyContactsForOrganization(Long organizationId) {
        return emergencyContactRepository.findByOrganizationId(organizationId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public EmergencyContactDto createEmergencyContact(
            Long userId, CreateEmergencyContactRequest request) {
        return createEmergencyContactInternal(userId, request);
    }

    @Transactional
    public void createFromOnboarding(
            Long tenantId, Long userId, CreateEmergencyContactRequest request) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));
        if (!user.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("User does not belong to tenant");
        }
        createEmergencyContactInternal(userId, request);
    }

    private EmergencyContactDto createEmergencyContactInternal(
            Long userId, CreateEmergencyContactRequest request) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));

        int nextPriority = emergencyContactRepository.findMaxPriorityByUserId(userId) + 1;

        UserEmergencyContact contact = mapper.fromCreateRequest(request);
        contact.setUser(user);
        contact.setTenantId(user.getTenantId());
        contact.setPriority(nextPriority);
        normalizeContactFields(contact);

        UserEmergencyContact saved = emergencyContactRepository.save(contact);
        log.info("Created emergency contact {} for user {}", saved.getId(), userId);
        return mapper.toDto(saved);
    }

    @Transactional
    public EmergencyContactDto updateEmergencyContact(
            Long userId, Long contactId, UpdateEmergencyContactRequest request) {
        UserEmergencyContact contact = getContactForUser(userId, contactId);
        mapper.updateFromRequest(request, contact);
        normalizeContactFields(contact);

        UserEmergencyContact saved = emergencyContactRepository.save(contact);
        log.info("Updated emergency contact {} for user {}", contactId, userId);
        return mapper.toDto(saved);
    }

    @Transactional
    public void deleteEmergencyContact(Long userId, Long contactId) {
        UserEmergencyContact contact = getContactForUser(userId, contactId);
        emergencyContactRepository.delete(contact);
        renumberPriorities(userId);
        log.info("Deleted emergency contact {} for user {}", contactId, userId);
    }

    @Transactional
    public List<EmergencyContactDto> reorderEmergencyContacts(
            Long userId, ReorderEmergencyContactsRequest request) {
        List<UserEmergencyContact> contacts =
                emergencyContactRepository.findByUser_IdOrderByPriorityAscIdAsc(userId);
        validateReorderRequest(contacts, request.getContactIds());

        Map<Long, UserEmergencyContact> contactById =
                contacts.stream()
                        .collect(
                                Collectors.toMap(UserEmergencyContact::getId, Function.identity()));

        int temporaryPriorityOffset = contacts.size() + 1000;
        for (int i = 0; i < request.getContactIds().size(); i++) {
            UserEmergencyContact contact = contactById.get(request.getContactIds().get(i));
            contact.setPriority(temporaryPriorityOffset + i);
        }
        emergencyContactRepository.saveAll(contacts);
        emergencyContactRepository.flush();

        for (int i = 0; i < request.getContactIds().size(); i++) {
            UserEmergencyContact contact = contactById.get(request.getContactIds().get(i));
            contact.setPriority(i + 1);
        }
        List<UserEmergencyContact> saved = emergencyContactRepository.saveAll(contacts);
        log.info("Reordered {} emergency contacts for user {}", saved.size(), userId);

        return saved.stream()
                .sorted((a, b) -> Integer.compare(a.getPriority(), b.getPriority()))
                .map(mapper::toDto)
                .toList();
    }

    private void validateReorderRequest(
            List<UserEmergencyContact> contacts, List<Long> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new InvalidEmergencyContactOrderException("Contact order must not be empty");
        }

        Set<Long> existingIds =
                contacts.stream().map(UserEmergencyContact::getId).collect(Collectors.toSet());

        if (orderedIds.size() != existingIds.size()) {
            throw new InvalidEmergencyContactOrderException(
                    "Contact order must include all emergency contacts exactly once");
        }

        Set<Long> providedIds = new HashSet<>(orderedIds);
        if (providedIds.size() != orderedIds.size() || !providedIds.equals(existingIds)) {
            throw new InvalidEmergencyContactOrderException(
                    "Contact order must include all emergency contacts exactly once");
        }
    }

    private void renumberPriorities(Long userId) {
        List<UserEmergencyContact> contacts =
                emergencyContactRepository.findByUser_IdOrderByPriorityAscIdAsc(userId);
        int temporaryPriorityOffset = contacts.size() + 1000;

        for (int i = 0; i < contacts.size(); i++) {
            contacts.get(i).setPriority(temporaryPriorityOffset + i);
        }
        emergencyContactRepository.saveAll(contacts);
        emergencyContactRepository.flush();

        for (int i = 0; i < contacts.size(); i++) {
            contacts.get(i).setPriority(i + 1);
        }
        emergencyContactRepository.saveAll(contacts);
    }

    private UserEmergencyContact getContactForUser(Long userId, Long contactId) {
        return emergencyContactRepository
                .findByIdAndUser_Id(contactId, userId)
                .orElseThrow(() -> new EmergencyContactNotFoundException(contactId));
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
    }

    private void normalizeContactFields(UserEmergencyContact contact) {
        contact.setFirstName(contact.getFirstName().trim());
        contact.setLastName(contact.getLastName().trim());
        contact.setStreet(contact.getStreet().trim());
        contact.setCity(contact.getCity().trim());
        contact.setPostalCode(contact.getPostalCode().trim());
        contact.setCountry(contact.getCountry().trim());
        contact.setPhone(contact.getPhone().trim());
        if (contact.getEmail() != null) {
            String email = contact.getEmail().trim();
            contact.setEmail(email.isEmpty() ? null : email);
        }
    }
}
