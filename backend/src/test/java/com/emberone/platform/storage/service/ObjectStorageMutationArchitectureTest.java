/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.storage.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Object-storage writes are not transactional with Postgres. Domain code must route mutations
 * through {@link ObjectStorageTransactionCoordinator} (or run after commit with no further DB
 * work).
 */
class ObjectStorageMutationArchitectureTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java/com/emberone");

    private static final Set<String> ALLOWED_MUTATION_FILES =
            Set.of(
                    "ObjectStorageService.java",
                    "ObjectStorageTransactionCoordinator.java",
                    "ObjectStorageDeletionOutboxService.java");

    private static final String FORBIDDEN_RECEIVER = "objectStorageService.";

    private static final List<String> FORBIDDEN_CALL_SUFFIXES =
            List.of(
                    "uploadFile(",
                    "deleteFile(",
                    "copyFile(",
                    "moveFile(",
                    "uploadProfileImage(",
                    "deleteProfileImage(");

    @Test
    void domainSourcesMustNotCallObjectStorageMutationsDirectly() throws IOException {
        assertThat(SOURCE_ROOT).exists().isDirectory();

        List<String> violations = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(SOURCE_ROOT)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !ALLOWED_MUTATION_FILES.contains(path.getFileName().toString()))
                    .forEach(path -> collectViolations(path, violations));
        }

        assertThat(violations)
                .withFailMessage(
                        """
                    Direct ObjectStorageService mutations bypass transaction coupling. Use \
                    ObjectStorageTransactionCoordinator instead. Violations:%n%s""",
                        String.join(System.lineSeparator(), violations))
                .isEmpty();
    }

    private static void collectViolations(Path file, List<String> violations) {
        try {
            String relative = SOURCE_ROOT.relativize(file).toString();
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.startsWith("//") || line.startsWith("*") || line.startsWith("/*")) {
                    continue;
                }
                if (isMethodDeclaration(line)) {
                    continue;
                }
                for (String suffix : FORBIDDEN_CALL_SUFFIXES) {
                    if (line.contains(FORBIDDEN_RECEIVER + suffix)) {
                        violations.add(relative + ":" + (i + 1) + " -> " + line);
                    }
                }
            }
        } catch (IOException e) {
            violations.add(file + " (unreadable: " + e.getMessage() + ")");
        }
    }

    private static boolean isMethodDeclaration(String line) {
        if (!line.contains("(")) {
            return false;
        }
        return Pattern.compile(
                        "^(public|protected|private|)\\s+.*\\s+(uploadFile|deleteFile|copyFile|moveFile|uploadProfileImage|deleteProfileImage)\\s*\\(")
                .matcher(line)
                .find();
    }
}
