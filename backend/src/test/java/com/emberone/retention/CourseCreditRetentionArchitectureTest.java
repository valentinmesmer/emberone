/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emberone.retention.handler.UserMasterRetentionHandler;
import org.junit.jupiter.api.Test;

/**
 * A credited course links its training course record to the course registration that earned it
 * ({@code AppLinkType.COURSE_REGISTRATION}). The link has no foreign key, so the registration must
 * share the lifetime of the record: both expire together with the person.
 */
class CourseCreditRetentionArchitectureTest {

    @Test
    void registrationAndCreditedRecordShareTheUserMasterLifetime() {
        UserMasterRetentionHandler handler = mock(UserMasterRetentionHandler.class);
        org.mockito.Mockito.when(handler.tables()).thenCallRealMethod();

        assertThat(handler.tables())
                .contains("course_registrations", "training_course_user_submissions");
    }
}
