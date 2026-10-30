/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.repository;

import com.emberone.certification.domain.UserCertificationRecordDocument;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface UserCertificationRecordDocumentRepository
        extends JpaRepository<UserCertificationRecordDocument, Long> {

    List<UserCertificationRecordDocument> findByRecord_IdIn(Collection<Long> recordIds);

    List<UserCertificationRecordDocument> findByRecord_Id(Long recordId);

    boolean existsByRecord_IdAndDocumentId(Long recordId, Long documentId);

    boolean existsByDocumentIdAndRecord_IdNot(Long documentId, Long recordId);

    boolean existsByDocumentIdAndRecord_IdNotIn(Long documentId, Collection<Long> recordIds);
}
