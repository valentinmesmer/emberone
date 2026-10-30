/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Every table with a person-related {@code user_id} column must be listed in at least one {@link
 * com.emberone.retention.spi.RetentionHandler#tables()}.
 */
class RetentionTableCoverageArchitectureTest {

    private static final Path RETENTION_SOURCES = Path.of("src/main/java/com/emberone/retention");
    private static final Path V1 =
            Path.of("src/main/resources/db/migration/postgresql/V1__Initial_setup.sql");

    private static final Pattern CREATE_TABLE =
            Pattern.compile("^CREATE TABLE (\\w+) \\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern USER_ID_COLUMN =
            Pattern.compile("^\\s+(\\w*user_id\\w*)\\s+BIGINT", Pattern.CASE_INSENSITIVE);
    private static final Pattern HANDLER_TABLE_LITERAL = Pattern.compile("\"([a-z][a-z0-9_]*)\"");

    @Test
    void everyUserIdTableIsOwnedByAHandler() throws IOException {
        Set<String> handlerTables = collectHandlerTableNames();
        Map<String, Set<String>> schemaTables = collectSchemaUserIdTables();

        Set<String> uncovered = new HashSet<>();
        for (String table : schemaTables.keySet()) {
            if (!handlerTables.contains(table)) {
                uncovered.add(table);
            }
        }

        assertThat(uncovered)
                .withFailMessage(
                        """
                    Tables with user_id-like columns missing from RetentionHandler#tables():%n%s%n\
                    Declared handler tables: %s""",
                        String.join(", ", uncovered), handlerTables)
                .isEmpty();
    }

    private static Map<String, Set<String>> collectSchemaUserIdTables() throws IOException {
        Map<String, Set<String>> tables = new LinkedHashMap<>();
        String current = null;
        for (String line : Files.readAllLines(V1)) {
            Matcher create = CREATE_TABLE.matcher(line);
            if (create.find()) {
                current = create.group(1);
                tables.putIfAbsent(current, new HashSet<>());
                continue;
            }
            if (current == null) {
                continue;
            }
            if (line.startsWith(");")) {
                current = null;
                continue;
            }
            Matcher column = USER_ID_COLUMN.matcher(line);
            if (column.find()) {
                String name = column.group(1);
                if (!"idp_user_id".equals(name)) {
                    tables.get(current).add(name);
                }
            }
        }
        tables.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        return tables;
    }

    private static Set<String> collectHandlerTableNames() throws IOException {
        Set<String> tables = new HashSet<>();
        try (Stream<Path> paths = Files.walk(RETENTION_SOURCES)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.getFileName().toString().endsWith("RetentionHandler.java"))
                    .forEach(path -> tables.addAll(extractTableLiterals(path)));
        }
        return tables;
    }

    private static List<String> extractTableLiterals(Path file) {
        try {
            String source = Files.readString(file);
            if (!source.contains("RetentionHandler") && !source.contains("tables")) {
                return List.of();
            }
            return HANDLER_TABLE_LITERAL.matcher(source).results().map(m -> m.group(1)).toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + file, e);
        }
    }
}
