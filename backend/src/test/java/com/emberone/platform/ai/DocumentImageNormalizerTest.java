/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emberone.platform.exception.ValidationException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class DocumentImageNormalizerTest {

    private final DocumentImageNormalizer normalizer = new DocumentImageNormalizer();

    private static byte[] encode(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    @Test
    void rendersFirstPageOfPdfAsJpeg() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (org.apache.pdfbox.pdmodel.PDDocument document =
                new org.apache.pdfbox.pdmodel.PDDocument()) {
            document.addPage(
                    new org.apache.pdfbox.pdmodel.PDPage(
                            org.apache.pdfbox.pdmodel.common.PDRectangle.A4));
            document.save(out);
        }
        AiImage image = normalizer.normalize("application/pdf", out.toByteArray());
        assertThat(image.mediaType()).isEqualTo("image/jpeg");
        assertThat(image.data()).isNotEmpty();
    }

    @Test
    void rejectsInvalidPdf() {
        assertThatThrownBy(() -> normalizer.normalize("application/pdf", new byte[] {1}))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("PDF");
    }

    @Test
    void rejectsUnknownTypeAndGarbage() {
        assertThatThrownBy(() -> normalizer.normalize("text/html", new byte[] {1}))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> normalizer.normalize("image/png", new byte[] {1, 2, 3}))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> normalizer.normalize("image/webp", new byte[] {1, 2, 3}))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void jpegWithinLimitsIsPassedThroughUnchanged() throws IOException {
        byte[] jpeg = encode(new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB), "jpg");

        AiImage image = normalizer.normalize("image/jpeg", jpeg);

        assertThat(image.mediaType()).isEqualTo("image/jpeg");
        assertThat(image.data()).isEqualTo(jpeg);
    }

    @Test
    void transparentPngIsFlattenedOntoWhiteJpeg() throws IOException {
        BufferedImage transparent = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        byte[] png = encode(transparent, "png");

        AiImage image = normalizer.normalize("image/png", png);

        assertThat(image.mediaType()).isEqualTo("image/jpeg");
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(image.data()));
        int rgb = decoded.getRGB(10, 10) & 0xFFFFFF;
        assertThat(rgb & 0xFF).isGreaterThan(240);
        assertThat((rgb >> 8) & 0xFF).isGreaterThan(240);
    }

    @Test
    void oversizedImageIsDownscaled() throws IOException {
        byte[] png = encode(new BufferedImage(4000, 1000, BufferedImage.TYPE_INT_RGB), "png");

        AiImage image = normalizer.normalize("image/png", png);

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(image.data()));
        assertThat(Math.max(decoded.getWidth(), decoded.getHeight()))
                .isEqualTo(DocumentImageNormalizer.MAX_SIDE_PIXELS);
    }

    @Test
    void rejectsHeicPhotoEvenIfNamedJpeg() {
        byte[] heic = {
            0, 0, 0, 0x34, 'f', 't', 'y', 'p', 'h', 'e', 'i', 'c', 0, 0, 0, 0, 'm', 'i', 'f', '1'
        };
        assertThatThrownBy(() -> normalizer.normalize("image/jpeg", heic))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("HEIC");
    }

    @Test
    void rendersEveryPdfPageUpToTheLimit() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (org.apache.pdfbox.pdmodel.PDDocument document =
                new org.apache.pdfbox.pdmodel.PDDocument()) {
            for (int i = 0; i < 5; i++) {
                document.addPage(
                        new org.apache.pdfbox.pdmodel.PDPage(
                                org.apache.pdfbox.pdmodel.common.PDRectangle.A4));
            }
            document.save(out);
        }
        assertThat(normalizer.normalizeAll("application/pdf", out.toByteArray()))
                .hasSize(DocumentImageNormalizer.MAX_PDF_PAGES);
    }

    @Test
    void trimsBlankMarginsAroundASmallCardButIgnoresDustSpecks() {
        BufferedImage page = new BufferedImage(1000, 700, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = page.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, 1000, 700);
        g.setColor(java.awt.Color.DARK_GRAY);
        g.fillRect(20, 20, 300, 190);
        g.fillRect(900, 600, 3, 3);
        g.dispose();

        BufferedImage trimmed = DocumentImageNormalizer.trimMargins(page);

        assertThat(trimmed.getWidth()).isLessThan(400);
        assertThat(trimmed.getHeight()).isLessThan(260);
    }

    @Test
    void keepsPagesWhoseContentFillsMostOfThePage() {
        BufferedImage page = new BufferedImage(1000, 700, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = page.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, 1000, 700);
        g.setColor(java.awt.Color.DARK_GRAY);
        g.fillRect(40, 40, 920, 620);
        g.dispose();

        assertThat(DocumentImageNormalizer.trimMargins(page)).isSameAs(page);
    }
}
