/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.http;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ContentDispositionHeadersTest {

    @Test
    void attachmentWithNonAsciiFilenameUsesRfc5987Encoding() {
        String header = ContentDispositionHeaders.attachment("fw_führerschein.pdf");

        assertTrue(header.startsWith("attachment;"));
        assertTrue(header.contains("filename*=UTF-8''fw_f%C3%BChrerschein.pdf"));
    }

    @Test
    void attachmentWithAsciiFilenameUsesSimpleFilename() {
        String header = ContentDispositionHeaders.attachment("license.pdf");

        assertTrue(header.contains("filename=\"license.pdf\""));
    }
}
