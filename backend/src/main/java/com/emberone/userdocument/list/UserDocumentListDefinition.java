/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument.list;

import com.emberone.platform.web.list.ListDefinition;
import com.emberone.userdocument.domain.UserDocument;

public final class UserDocumentListDefinition {

    public static final ListDefinition<UserDocument> INSTANCE =
            ListDefinition.<UserDocument>forEntity(UserDocument.class)
                    .searchField("title")
                    .sortKey("createdAt", "createdAt")
                    .textSortKey("title", "title")
                    .defaultSort("createdAt,desc")
                    .build();

    private UserDocumentListDefinition() {}
}
