/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.cache;

import com.emberone.platform.event.AfterCommitCallback;
import com.emberone.user.event.UserDeactivatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Drops the cached active flag so a deactivated user is rejected on every instance at once. */
@Service
@RequiredArgsConstructor
public class UserActiveCacheEvictionListener {

    private final UserActiveCacheEvictor userActiveCacheEvictor;

    @AfterCommitCallback
    public void onUserDeactivated(UserDeactivatedEvent event) {
        userActiveCacheEvictor.evict(event.userId());
    }
}
