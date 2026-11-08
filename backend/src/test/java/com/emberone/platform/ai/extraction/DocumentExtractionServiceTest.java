/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.platform.ai.AiAvailabilityGuard;
import com.emberone.platform.ai.AiCompletionRequest;
import com.emberone.platform.ai.AiGateway;
import com.emberone.platform.ai.AiModel;
import com.emberone.platform.ai.AiProperties;
import com.emberone.platform.ai.AiUnavailableException;
import com.emberone.platform.ai.DocumentImageNormalizer;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.exception.ValidationException;
import com.emberone.platform.security.TokenService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class DocumentExtractionServiceTest {

    @Mock AiGateway gateway;
    @Mock TokenService tokenService;
    @Mock DocumentExtractionSchema schema;

    AiProperties properties = new AiProperties();
    DocumentExtractionService service;

    @BeforeEach
    void setUp() {
        properties.setEnabled(true);
        service =
                new DocumentExtractionService(
                        List.of(schema),
                        new AiAvailabilityGuard(properties),
                        gateway,
                        new DocumentImageNormalizer(),
                        tokenService);
        lenient().when(schema.type()).thenReturn("CERTIFICATION_RECORD");
        lenient().when(tokenService.requireUserId()).thenReturn(5L);
    }

    private static PreparedExtraction prepared() {
        return new PreparedExtraction(
                "schema",
                "instruction",
                Map.of("type", "object"),
                answer ->
                        new DocumentExtractionResult(
                                Map.of("x", answer.get("x").asInt()), List.of()));
    }

    private static MockMultipartFile jpeg() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "jpg", out);
        return new MockMultipartFile("file", "proof.jpg", "image/jpeg", out.toByteArray());
    }

    @Test
    void disabledIsRejected() throws Exception {
        properties.setEnabled(false);
        when(schema.prepare(5L, null)).thenReturn(prepared());

        assertThatThrownBy(() -> service.extract("CERTIFICATION_RECORD", List.of(jpeg()), null))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void pdfIsRejectedWithoutModelCall() {
        when(schema.prepare(5L, null)).thenReturn(prepared());
        MockMultipartFile pdf =
                new MockMultipartFile("file", "proof.pdf", "application/pdf", new byte[] {1, 2});

        assertThatThrownBy(() -> service.extract("CERTIFICATION_RECORD", List.of(pdf), null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("PDF");

        verify(gateway, never()).complete(any());
    }

    @Test
    void unknownTypeIsNotFound() throws Exception {
        assertThatThrownBy(() -> service.extract("NAME_CHANGE", List.of(jpeg()), null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void validatesModelAnswerAndSendsTheUploadedImageToTheVisionModel() throws Exception {
        when(schema.prepare(5L, null)).thenReturn(prepared());
        when(gateway.complete(any()))
                .thenReturn(JsonMapper.builder().build().readTree("{\"x\":3}"));

        DocumentExtractionResult result =
                service.extract("CERTIFICATION_RECORD", List.of(jpeg()), null);

        assertThat(result.fields()).containsEntry("x", 3);
        ArgumentCaptor<AiCompletionRequest> captor =
                ArgumentCaptor.forClass(AiCompletionRequest.class);
        verify(gateway).complete(captor.capture());
        assertThat(captor.getValue().model()).isEqualTo(AiModel.VISION);
        assertThat(captor.getValue().images()).hasSize(1);
        assertThat(captor.getValue().function()).isEqualTo("extract-certification-record");
    }

    @Test
    void sendsAllDocumentsOfOneRequestToTheModel() throws Exception {
        when(schema.prepare(5L, null)).thenReturn(prepared());
        when(gateway.complete(any()))
                .thenReturn(JsonMapper.builder().build().readTree("{\"x\":3}"));

        service.extract("CERTIFICATION_RECORD", List.of(jpeg(), jpeg()), null);

        ArgumentCaptor<AiCompletionRequest> captor =
                ArgumentCaptor.forClass(AiCompletionRequest.class);
        verify(gateway).complete(captor.capture());
        assertThat(captor.getValue().images()).hasSize(2);
    }

    @Test
    void rejectsMoreThanTheAllowedNumberOfDocuments() throws Exception {
        when(schema.prepare(5L, null)).thenReturn(prepared());
        List<MockMultipartFile> files =
                java.util.Collections.nCopies(DocumentExtractionService.MAX_FILES + 1, jpeg());

        assertThatThrownBy(() -> service.extract("CERTIFICATION_RECORD", List.copyOf(files), null))
                .isInstanceOf(ValidationException.class);
        verify(gateway, never()).complete(any());
    }
}
