/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.todo.type.certification;

import com.emberone.todo.domain.TodoType;
import com.emberone.todo.type.AbstractTodoTypeDefinition;
import org.springframework.stereotype.Component;

/**
 * To-do reminding a user that a required certification is missing, expired, or expiring soon.
 * Manually completable, and notifies the assignee on creation.
 */
@Component
public class CertificationExpiringTodoDefinition extends AbstractTodoTypeDefinition {

    public CertificationExpiringTodoDefinition() {
        super(TodoType.CERTIFICATION_EXPIRING, true, true);
    }
}
