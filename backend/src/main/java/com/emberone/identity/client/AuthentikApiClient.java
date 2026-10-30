/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.identity.client;

import com.emberone.identity.client.dto.*;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface AuthentikApiClient {

    @RequestLine("GET /api/v3/core/groups/?name={name}")
    AuthentikGroupListResponse listGroupsByExactName(@Param("name") String name);

    @RequestLine("GET /api/v3/core/users/{pk}/")
    AuthentikUserDetailResponse getUser(@Param("pk") String pk);

    @RequestLine("POST /api/v3/core/users/")
    @Headers({"Content-Type: application/json"})
    AuthentikUserResponse createUser(AuthentikUserRequest request);

    @RequestLine("PATCH /api/v3/core/users/{pk}/")
    @Headers({"Content-Type: application/json"})
    void updateUser(@Param("pk") String pk, AuthentikUserRequest request);

    @RequestLine("POST /api/v3/core/users/{pk}/set_password/")
    @Headers({"Content-Type: application/json"})
    void setPassword(@Param("pk") String pk, AuthentikSetPasswordRequest request);

    @RequestLine("DELETE /api/v3/core/users/{pk}/")
    void deleteUser(@Param("pk") String pk);
}
