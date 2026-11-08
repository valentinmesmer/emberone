/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai;

import com.emberone.attachment.AllowedAttachmentMediaTypes;
import com.emberone.platform.exception.ValidationException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

/**
 * Turns an uploaded proof document into a model input ({@code ImageIO}, PDFBox for PDFs).
 *
 * <ul>
 *   <li>Accepts JPEG, PNG, GIF and WEBP ({@link AllowedAttachmentMediaTypes#isAllowedImage}).
 *   <li>PDFs: up to {@link #MAX_PDF_PAGES} pages are rendered with PDFBox to JPEGs, blank margins
 *       cropped (encrypted or empty PDFs are rejected).
 *   <li>HEIC/HEIF is rejected with a clear message (no decoder in the JDK).
 *   <li>PNG/GIF are re-encoded as JPEG on a white background (a transparent background would reach
 *       the model as black); oversized images are downscaled. A JPEG within limits is passed
 *       through untouched so EXIF orientation stays intact. WEBP is passed through (no JDK
 *       decoder).
 * </ul>
 */
@Component
public class DocumentImageNormalizer {

    static final int MAX_SIDE_PIXELS = 2048;
    static final long MAX_PIXELS = 80_000_000L;
    static final int MAX_PASS_THROUGH_BYTES = 4 * 1024 * 1024;
    static final float PDF_MAX_DPI = 200f;
    static final int MAX_PDF_PAGES = 3;
    private static final int BLANK_LUMINANCE = 235;
    private static final int MIN_DENSE_PIXELS = 12;
    private static final double MAX_TRIM_KEEP_RATIO = 0.7;

    /** First image of the document (single photo, or first PDF page). */
    public AiImage normalize(String contentType, byte[] data) {
        return normalizeAll(contentType, data).getFirst();
    }

    /** All images of the document: one per photo, up to {@link #MAX_PDF_PAGES} for a PDF. */
    public List<AiImage> normalizeAll(String contentType, byte[] data) {
        String type = contentType == null ? "" : contentType.toLowerCase();
        if ("application/pdf".equals(type)) {
            return renderPages(data);
        }
        return List.of(normalizeImage(type, data));
    }

    private AiImage normalizeImage(String type, byte[] data) {
        if (!AllowedAttachmentMediaTypes.isAllowedImage(type)) {
            throw new ValidationException("Unsupported document type for AI extraction");
        }
        if (data == null || data.length == 0) {
            throw new ValidationException("The document is empty");
        }
        if (looksLikeHeif(data)) {
            throw new ValidationException(
                    "The document is a HEIC/HEIF photo, which cannot be read. Please use JPEG or PNG");
        }
        if ("image/webp".equals(type)) {
            if (!looksLikeWebp(data)) {
                throw new ValidationException("The document is not a valid image");
            }
            return new AiImage(type, data);
        }
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new ValidationException("The document is not a valid image");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
                    throw new ValidationException("The image dimensions are not supported");
                }
                boolean oversized =
                        Math.max(width, height) > MAX_SIDE_PIXELS
                                || data.length > MAX_PASS_THROUGH_BYTES;
                if ("image/jpeg".equals(type) && !oversized) {
                    return new AiImage(type, data);
                }
                BufferedImage image = reader.read(0);
                return new AiImage("image/jpeg", encodeJpeg(flattenAndScale(image)));
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException ex) {
            if (ex instanceof ValidationException validation) {
                throw validation;
            }
            throw new ValidationException("The document could not be read as an image");
        }
    }

    private static List<AiImage> renderPages(byte[] data) {
        if (data == null || data.length == 0) {
            throw new ValidationException("The document is empty");
        }
        try (PDDocument document = Loader.loadPDF(data)) {
            if (document.isEncrypted() || document.getNumberOfPages() == 0) {
                throw new ValidationException("The PDF could not be read");
            }
            PDFRenderer renderer = new PDFRenderer(document);
            List<AiImage> images = new ArrayList<>();
            int pages = Math.min(document.getNumberOfPages(), MAX_PDF_PAGES);
            for (int index = 0; index < pages; index++) {
                PDRectangle box = document.getPage(index).getCropBox();
                float longestSidePoints = Math.max(box.getWidth(), box.getHeight());
                if (longestSidePoints <= 0) {
                    throw new ValidationException("The PDF could not be read");
                }
                // Render at a DPI that yields at most MAX_SIDE_PIXELS on the longest side.
                float dpi = Math.min(PDF_MAX_DPI, MAX_SIDE_PIXELS * 72f / longestSidePoints);
                BufferedImage page = renderer.renderImageWithDPI(index, dpi, ImageType.RGB);
                images.add(
                        new AiImage("image/jpeg", encodeJpeg(flattenAndScale(trimMargins(page)))));
            }
            return images;
        } catch (IOException | RuntimeException ex) {
            if (ex instanceof ValidationException validation) {
                throw validation;
            }
            throw new ValidationException("The PDF could not be read");
        }
    }

    /**
     * Crops blank margins, e.g. a card scanned in the corner of an A4 page, so the content gets the
     * full model resolution. Rows and columns count only with enough non-white pixels, so dust
     * specks of a scan do not widen the crop. Pages whose content fills most of the page stay
     * untouched.
     */
    static BufferedImage trimMargins(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int[] rows = new int[height];
        int[] columns = new int[width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);
                int luminance =
                        (((rgb >> 16) & 0xFF) * 3 + ((rgb >> 8) & 0xFF) * 6 + (rgb & 0xFF)) / 10;
                if (luminance < BLANK_LUMINANCE) {
                    rows[y]++;
                    columns[x]++;
                }
            }
        }
        int top = firstDense(rows, false);
        int bottom = firstDense(rows, true);
        int left = firstDense(columns, false);
        int right = firstDense(columns, true);
        if (top < 0 || left < 0 || bottom < top || right < left) {
            return source;
        }
        int marginX = Math.max(1, (right - left + 1) / 50);
        int marginY = Math.max(1, (bottom - top + 1) / 50);
        int x0 = Math.max(0, left - marginX);
        int y0 = Math.max(0, top - marginY);
        int x1 = Math.min(width - 1, right + marginX);
        int y1 = Math.min(height - 1, bottom + marginY);
        long kept = (long) (x1 - x0 + 1) * (y1 - y0 + 1);
        if (kept > (long) width * height * MAX_TRIM_KEEP_RATIO) {
            return source;
        }
        return source.getSubimage(x0, y0, x1 - x0 + 1, y1 - y0 + 1);
    }

    private static int firstDense(int[] counts, boolean fromEnd) {
        for (int i = 0; i < counts.length; i++) {
            int index = fromEnd ? counts.length - 1 - i : i;
            if (counts[index] >= MIN_DENSE_PIXELS) {
                return index;
            }
        }
        return -1;
    }

    private static BufferedImage flattenAndScale(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        double scale = Math.min(1.0, (double) MAX_SIDE_PIXELS / Math.max(width, height));
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));
        BufferedImage target =
                new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, targetWidth, targetHeight);
            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private static byte[] encodeJpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "jpg", out)) {
            throw new IOException("No JPEG writer available");
        }
        return out.toByteArray();
    }

    /** HEIC/HEIF (iPhone default format), often renamed to .jpg: ISO base media {@code ftyp}. */
    private static boolean looksLikeHeif(byte[] data) {
        if (data.length < 12
                || data[4] != 'f'
                || data[5] != 't'
                || data[6] != 'y'
                || data[7] != 'p') {
            return false;
        }
        String brand = new String(data, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
        return switch (brand) {
            case "heic", "heix", "heim", "heis", "hevc", "hevx", "hevm", "hevs", "mif1", "msf1" ->
                    true;
            default -> false;
        };
    }

    private static boolean looksLikeWebp(byte[] data) {
        return data.length > 12
                && data[0] == 'R'
                && data[1] == 'I'
                && data[2] == 'F'
                && data[3] == 'F'
                && data[8] == 'W'
                && data[9] == 'E'
                && data[10] == 'B'
                && data[11] == 'P';
    }
}
