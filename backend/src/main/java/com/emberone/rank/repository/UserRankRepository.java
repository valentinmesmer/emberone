/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.rank.repository;

import com.emberone.rank.domain.UserRank;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface UserRankRepository extends JpaRepository<UserRank, Long> {

    Optional<UserRank> findByUserIdAndEndDateIsNull(Long userId);

    List<UserRank> findByUserIdOrderByStartDateDescIdDesc(Long userId);

    List<UserRank> findByUserIdInAndEndDateIsNull(List<Long> userIds);
}
