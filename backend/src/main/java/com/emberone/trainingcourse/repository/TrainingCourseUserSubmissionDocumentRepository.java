/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.trainingcourse.repository;

import com.emberone.trainingcourse.domain.TrainingCourseUserSubmissionDocument;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface TrainingCourseUserSubmissionDocumentRepository
        extends JpaRepository<TrainingCourseUserSubmissionDocument, Long> {

    List<TrainingCourseUserSubmissionDocument> findBySubmission_IdIn(
            Collection<Long> submissionIds);

    List<TrainingCourseUserSubmissionDocument> findBySubmission_Id(Long submissionId);

    boolean existsBySubmission_IdAndDocumentId(Long submissionId, Long documentId);
}
