/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.platform.navigation.domain.AppLinkType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Every document {@code link_type} has a code path that deletes linked user documents. */
class DocumentLinkDeletionArchitectureTest {

    private static final Path MAIN_SOURCES = Path.of("src/main/java/com/emberone");

    @Test
    void everyAppLinkTypeIsClassified() {
        Set<AppLinkType> classified = EnumSet.noneOf(AppLinkType.class);
        classified.addAll(DocumentLinkDeletionCoverage.DOCUMENT_LINK_TYPES);
        classified.addAll(DocumentLinkDeletionCoverage.NON_DOCUMENT_LINK_TYPES);

        assertThat(classified).containsExactlyInAnyOrder(AppLinkType.values());
        assertThat(DocumentLinkDeletionCoverage.DOCUMENT_LINK_TYPES)
                .doesNotContainAnyElementsOf(DocumentLinkDeletionCoverage.NON_DOCUMENT_LINK_TYPES);
    }

    @Test
    void everyComponentDeletingADocumentLinkTargetDeletesItsDocuments() throws IOException {
        List<String> missing = new ArrayList<>();
        for (AppLinkType linkType : DocumentLinkDeletionCoverage.DOCUMENT_LINK_TYPES) {
            Set<String> components = DocumentLinkDeletionCoverage.DELETING_COMPONENTS.get(linkType);
            assertThat(components)
                    .withFailMessage("Missing deleting components for link type %s", linkType)
                    .isNotNull()
                    .isNotEmpty();
            for (String component : components) {
                if (!Files.readString(findSourceFile(component))
                        .contains(DocumentLinkDeletionCoverage.DELETE_DOCUMENTS_CALL)) {
                    missing.add(linkType + " -> " + component);
                }
            }
        }
        assertThat(missing)
                .withFailMessage(
                        "Component(s) delete a document link target without %s:%n%s",
                        DocumentLinkDeletionCoverage.DELETE_DOCUMENTS_CALL,
                        String.join(System.lineSeparator(), missing))
                .isEmpty();
    }

    private static Path findSourceFile(String simpleClassName) throws IOException {
        try (Stream<Path> paths = Files.walk(MAIN_SOURCES)) {
            return paths.filter(
                            path -> path.getFileName().toString().equals(simpleClassName + ".java"))
                    .findFirst()
                    .orElseThrow(
                            () ->
                                    new IllegalStateException(
                                            "Source not found for " + simpleClassName));
        }
    }
}
