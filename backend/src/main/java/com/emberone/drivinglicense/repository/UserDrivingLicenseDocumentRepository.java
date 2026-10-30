/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.drivinglicense.repository;

import com.emberone.drivinglicense.domain.UserDrivingLicenseDocument;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface UserDrivingLicenseDocumentRepository
        extends JpaRepository<UserDrivingLicenseDocument, Long> {

    List<UserDrivingLicenseDocument> findByLicense_Id(Long licenseId);

    boolean existsByLicense_IdAndDocumentId(Long licenseId, Long documentId);
}
