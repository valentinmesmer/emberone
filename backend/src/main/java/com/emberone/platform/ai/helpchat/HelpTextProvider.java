/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai.helpchat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Static German help texts, versioned with the code. Data class {@code A}: no personal data.
 *
 * <ul>
 *   <li>Per workflow type: {@code resources/ai/help/workflows/<WORKFLOW_TYPE>.md}. Only types with
 *       a help text can be suggested by the help chat.
 *   <li>Per {@link HelpTopic}: {@code resources/ai/help/topics/<TOPIC>.md}. Only topics with a help
 *       text are offered.
 * </ul>
 */
@Component
public class HelpTextProvider {

    private static final String TYPE_PATTERN = "classpath*:ai/help/workflows/*.md";
    private static final String TOPIC_PATTERN = "classpath*:ai/help/topics/*.md";

    private final Map<String, String> textsByType;
    private final Map<String, String> textsByTopic;

    public HelpTextProvider() {
        this(load(TYPE_PATTERN), load(TOPIC_PATTERN));
    }

    /** For tests. */
    HelpTextProvider(Map<String, String> textsByType) {
        this(textsByType, Map.of());
    }

    /** For tests. */
    HelpTextProvider(Map<String, String> textsByType, Map<String, String> textsByTopic) {
        this.textsByType = Map.copyOf(textsByType);
        this.textsByTopic = Map.copyOf(textsByTopic);
    }

    public Optional<String> forType(String workflowTypeKey) {
        return Optional.ofNullable(textsByType.get(workflowTypeKey));
    }

    public boolean has(String workflowTypeKey) {
        return textsByType.containsKey(workflowTypeKey);
    }

    public Optional<String> forTopic(String topicKey) {
        return Optional.ofNullable(textsByTopic.get(topicKey));
    }

    public boolean hasTopic(String topicKey) {
        return textsByTopic.containsKey(topicKey);
    }

    private static Map<String, String> load(String pattern) {
        Map<String, String> texts = new TreeMap<>();
        try {
            for (Resource resource :
                    new PathMatchingResourcePatternResolver().getResources(pattern)) {
                String filename = resource.getFilename();
                if (filename == null) {
                    continue;
                }
                String text =
                        new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                                .trim();
                if (StringUtils.hasText(text)) {
                    texts.put(filename.substring(0, filename.length() - ".md".length()), text);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not load AI help texts", ex);
        }
        return texts;
    }
}
