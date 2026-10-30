/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.service;

import com.emberone.user.domain.UserContactInformation;
import com.emberone.user.repository.UserContactInformationRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserContactInformationService {

    private final UserContactInformationRepository contactInformationRepository;

    /** Private notification email of the user, if one is set. */
    public Optional<String> findPrivateEmail(Long userId) {
        return contactInformationRepository
                .findByUser_Id(userId)
                .map(UserContactInformation::getPrivateEmail)
                .filter(StringUtils::hasText);
    }
}
