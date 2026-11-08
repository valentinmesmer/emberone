/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai.helpchat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.certification.dto.CertificationDefinitionDto;
import com.emberone.certification.service.CertificationCatalogService;
import com.emberone.drivinglicense.service.DrivingLicenseCatalogService;
import com.emberone.platform.ai.AiAvailabilityGuard;
import com.emberone.platform.ai.AiCompletionRequest;
import com.emberone.platform.ai.AiGateway;
import com.emberone.platform.ai.AiModel;
import com.emberone.platform.ai.AiProperties;
import com.emberone.platform.ai.AiProviderException;
import com.emberone.platform.ai.AiUnavailableException;
import com.emberone.platform.exception.ValidationException;
import com.emberone.platform.i18n.LocalizationService;
import com.emberone.platform.i18n.TenantLocaleService;
import com.emberone.platform.security.TokenService;
import com.emberone.trainingcourse.service.TrainingCourseCatalogService;
import com.emberone.workflow.domain.WorkflowOpenScope;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.WorkflowOpenStateDto;
import com.emberone.workflow.dto.WorkflowTypeCatalogDto;
import com.emberone.workflow.service.BeginnableWorkflowTypeService;
import com.emberone.workflow.service.WorkflowService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class HelpChatServiceTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Mock AiGateway gateway;
    @Mock TokenService tokenService;
    @Mock BeginnableWorkflowTypeService beginnableService;
    @Mock WorkflowService workflowService;
    @Mock CertificationCatalogService certificationCatalogService;
    @Mock TrainingCourseCatalogService trainingCourseCatalogService;
    @Mock DrivingLicenseCatalogService drivingLicenseCatalogService;
    @Mock HelpTopicAvailabilityService topicAvailabilityService;
    @Mock TenantLocaleService tenantLocaleService;

    AiProperties properties = new AiProperties();
    HelpChatService service;

    @BeforeEach
    void setUp() {
        properties.setEnabled(true);
        service =
                new HelpChatService(
                        gateway,
                        new AiAvailabilityGuard(properties),
                        tokenService,
                        beginnableService,
                        workflowService,
                        new HelpTextProvider(
                                Map.of(
                                        "NAME_CHANGE", "Namensänderung Hilfetext",
                                        "ADDRESS_CHANGE", "Adressänderung Hilfetext",
                                        "CERTIFICATION_RECORD", "Nachweis Hilfetext"),
                                Map.of(
                                        "ORG_SERVICE_EVENTS", "Dienstleistung Hilfetext",
                                        "MY_EMERGENCY_CONTACTS", "Notfallkontakt Hilfetext")),
                        new HelpChatActionValidator(),
                        certificationCatalogService,
                        trainingCourseCatalogService,
                        drivingLicenseCatalogService,
                        topicAvailabilityService,
                        tenantLocaleService,
                        new LocalizationService(messages()));
        lenient().when(tokenService.requireUserId()).thenReturn(5L);
        lenient()
                .when(tenantLocaleService.resolveTenantLocale(any()))
                .thenReturn(SupportedLocale.DE);
        lenient()
                .when(workflowService.getOpenStateForCurrentUser(anyLong()))
                .thenReturn(WorkflowOpenStateDto.builder().typeBlocked(false).build());
    }

    private static MessageSource messages() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }

    private static WorkflowTypeCatalogDto type(WorkflowType type, WorkflowOpenScope scope) {
        return WorkflowTypeCatalogDto.builder()
                .workflowTypeId(type.getId())
                .workflowTypeKey(type.name())
                .openScope(scope)
                .build();
    }

    private static HelpChatRequest ask(String question) {
        return new HelpChatRequest(List.of(new HelpChatRequest.Message("user", question)));
    }

    private static JsonNode json(String value) {
        return MAPPER.readTree(value);
    }

    private void beginnable(WorkflowTypeCatalogDto... types) {
        when(beginnableService.listForCurrentUser()).thenReturn(List.of(types));
    }

    @Test
    void helpChatIsAvailableWithExternalEndpointBecauseItIsClassA() {
        beginnable(type(WorkflowType.NAME_CHANGE, WorkflowOpenScope.SELF));
        when(gateway.complete(any())).thenReturn(json("{\"text\":\"Antwort\",\"actions\":[]}"));

        HelpChatResponse response = service.chat(ask("Wie ändere ich meinen Namen?"));

        assertThat(response.text()).isEqualTo("Antwort");
        ArgumentCaptor<AiCompletionRequest> captor =
                ArgumentCaptor.forClass(AiCompletionRequest.class);
        verify(gateway).complete(captor.capture());
        assertThat(captor.getValue().model()).isEqualTo(AiModel.TEXT);
        assertThat(captor.getValue().images()).isEmpty();
    }

    @Test
    void disabledMeansNoModelCall() {
        properties.setEnabled(false);

        assertThatThrownBy(() -> service.chat(ask("hi")))
                .isInstanceOf(AiUnavailableException.class);

        verify(gateway, never()).complete(any());
    }

    @Test
    void inventedTypeAndForeignConfigKeyAreDiscarded() {
        beginnable(
                type(WorkflowType.NAME_CHANGE, WorkflowOpenScope.SELF),
                type(WorkflowType.CERTIFICATION_RECORD, WorkflowOpenScope.SELF));
        when(certificationCatalogService.listAvailableDefinitionsForUser(5L))
                .thenReturn(
                        List.of(
                                CertificationDefinitionDto.builder()
                                        .id(42L)
                                        .configKey("G26.3")
                                        .name("Atemschutz")
                                        .active(true)
                                        .build()));
        when(gateway.complete(any()))
                .thenReturn(
                        json(
                                """
                                {"text":"Antwort","actions":[
                                  {"target":"HACK","configKey":null},
                                  {"target":"CERTIFICATION_RECORD","configKey":"G99"},
                                  {"target":"CERTIFICATION_RECORD","configKey":"G26.3"}]}
                                """));

        HelpChatResponse response = service.chat(ask("Wie reiche ich meine G26.3 ein?"));

        assertThat(response.actions()).hasSize(1);
        assertThat(response.actions().getFirst().catalogId()).isEqualTo(42L);
        assertThat(response.actions().getFirst().workflowTypeKey())
                .isEqualTo("CERTIFICATION_RECORD");
    }

    @Test
    void notStartableTypesAreNotOfferedToTheModel() {
        // ADDRESS_CHANGE has a help text but the user may not begin it (not in beginnable list);
        // RANK_REQUEST is beginnable but has no help text; org-scope types are never offered.
        beginnable(
                type(WorkflowType.NAME_CHANGE, WorkflowOpenScope.SELF),
                type(WorkflowType.RANK_REQUEST, WorkflowOpenScope.SELF),
                type(
                        WorkflowType.ORGANIZATION_MEMBERSHIP_END_REQUEST,
                        WorkflowOpenScope.ORGANIZATION));
        when(gateway.complete(any()))
                .thenReturn(
                        json(
                                """
                                {"text":"Antwort","actions":[
                                  {"target":"ADDRESS_CHANGE","configKey":null},
                                  {"target":"RANK_REQUEST","configKey":null}]}
                                """));

        HelpChatResponse response = service.chat(ask("hi"));

        assertThat(response.actions()).isEmpty();
        ArgumentCaptor<AiCompletionRequest> captor =
                ArgumentCaptor.forClass(AiCompletionRequest.class);
        verify(gateway).complete(captor.capture());
        String schema = captor.getValue().jsonSchema().toString();
        assertThat(schema).contains("NAME_CHANGE").doesNotContain("ADDRESS_CHANGE", "RANK_REQUEST");
        assertThat(captor.getValue().systemPrompt())
                .contains("Namensänderung Hilfetext")
                .doesNotContain("Adressänderung Hilfetext");
    }

    @Test
    void blockedTypeIsNotSuggestedButMentionedToTheModel() {
        beginnable(type(WorkflowType.NAME_CHANGE, WorkflowOpenScope.SELF));
        when(workflowService.getOpenStateForCurrentUser(WorkflowType.NAME_CHANGE.getId()))
                .thenReturn(WorkflowOpenStateDto.builder().typeBlocked(true).build());
        when(gateway.complete(any()))
                .thenReturn(
                        json(
                                "{\"text\":\"Antwort\",\"actions\":[{\"target\":\"NAME_CHANGE\",\"configKey\":null}]}"));

        HelpChatResponse response = service.chat(ask("Namen ändern"));

        assertThat(response.actions()).isEmpty();
        ArgumentCaptor<AiCompletionRequest> captor =
                ArgumentCaptor.forClass(AiCompletionRequest.class);
        verify(gateway).complete(captor.capture());
        assertThat(captor.getValue().systemPrompt())
                .contains("already open")
                .contains("NAME_CHANGE");
    }

    @Test
    void topicsGoToPromptAndSchemaButOrganizationNamesDoNot() {
        beginnable(type(WorkflowType.NAME_CHANGE, WorkflowOpenScope.SELF));
        HelpChatOrganizationDto ffA = new HelpChatOrganizationDto(1L, "FF Ahausen");
        HelpChatOrganizationDto ffB = new HelpChatOrganizationDto(2L, "FF Bdorf");
        when(topicAvailabilityService.availableForCurrentUser())
                .thenReturn(
                        Map.of(
                                HelpTopic.ORG_SERVICE_EVENTS,
                                new AvailableHelpTopic(
                                        HelpTopic.ORG_SERVICE_EVENTS,
                                        List.of(ffA, ffB),
                                        List.of(ffA, ffB),
                                        null)));
        when(gateway.complete(any()))
                .thenReturn(
                        json(
                                "{\"text\":\"Antwort\",\"actions\":[{\"target\":\"ORG_SERVICE_EVENTS\",\"create\":true,\"configKey\":null}]}"));

        HelpChatResponse response = service.chat(ask("Wie lege ich eine neue Dienstleistung an?"));

        assertThat(response.actions()).hasSize(1);
        HelpChatActionDto action = response.actions().getFirst();
        assertThat(action.kind()).isEqualTo(HelpChatActionDto.KIND_TOPIC);
        assertThat(action.create()).isTrue();
        assertThat(action.organizations()).containsExactly(ffA, ffB);
        ArgumentCaptor<AiCompletionRequest> captor =
                ArgumentCaptor.forClass(AiCompletionRequest.class);
        verify(gateway).complete(captor.capture());
        assertThat(captor.getValue().jsonSchema().toString())
                .contains("ORG_SERVICE_EVENTS")
                .contains("NAME_CHANGE")
                .doesNotContain("MY_EMERGENCY_CONTACTS");
        assertThat(captor.getValue().systemPrompt())
                .contains("Answer in German")
                .contains("Dienstleistung Hilfetext")
                .contains("create possible")
                .doesNotContain("FF Ahausen", "FF Bdorf");
    }

    @Test
    void historyMustEndWithUserMessage() {
        HelpChatRequest request =
                new HelpChatRequest(
                        List.of(
                                new HelpChatRequest.Message("user", "a"),
                                new HelpChatRequest.Message("assistant", "b")));

        assertThatThrownBy(() -> service.chat(request)).isInstanceOf(ValidationException.class);

        verify(gateway, never()).complete(any());
    }

    @Test
    void emptyAnswerTextIsProviderError() {
        beginnable(type(WorkflowType.NAME_CHANGE, WorkflowOpenScope.SELF));
        when(gateway.complete(any())).thenReturn(json("{\"text\":\"  \",\"actions\":[]}"));

        assertThatThrownBy(() -> service.chat(ask("hi"))).isInstanceOf(AiProviderException.class);
    }

    @Test
    void noStartableTypesStillAnswersWithoutActionSchema() {
        beginnable();
        when(gateway.complete(any())).thenReturn(json("{\"text\":\"Antwort\"}"));

        HelpChatResponse response = service.chat(ask("hi"));

        assertThat(response.actions()).isEmpty();
        ArgumentCaptor<AiCompletionRequest> captor =
                ArgumentCaptor.forClass(AiCompletionRequest.class);
        verify(gateway).complete(captor.capture());
        assertThat(captor.getValue().jsonSchema().toString()).doesNotContain("actions");
    }

    @Test
    void helpTextsAreLoadedFromClasspathForAllStartableChatTypes() {
        HelpTextProvider provider = new HelpTextProvider();

        for (WorkflowType type :
                Set.of(
                        WorkflowType.NAME_CHANGE,
                        WorkflowType.ADDRESS_CHANGE,
                        WorkflowType.CERTIFICATION_RECORD,
                        WorkflowType.TRAINING_COURSE_RECORD,
                        WorkflowType.DRIVING_LICENSE_RECORD,
                        WorkflowType.RANK_REQUEST)) {
            assertThat(provider.forType(type.name())).isPresent();
        }
        assertThat(provider.has("ONBOARDING_SUBMISSION")).isFalse();
    }

    @Test
    void helpTextPlaceholdersAndAliasesResolveForAllLocales() {
        HelpTextProvider provider = new HelpTextProvider();
        LocalizationService localization = new LocalizationService(messages());
        Pattern placeholder = Pattern.compile("\\{([A-Za-z]+)\\}");

        for (SupportedLocale locale : SupportedLocale.values()) {
            for (HelpTopic topic : HelpTopic.values()) {
                assertThat(provider.hasTopic(topic.name())).as(topic.name()).isTrue();
                assertThat(localization.getMessageOrNull("help.aliases." + topic.name(), locale))
                        .as("aliases " + topic.name())
                        .isNotNull();
            }
            for (WorkflowType type : WorkflowType.values()) {
                provider.forType(type.name())
                        .ifPresent(text -> assertLabels(text, placeholder, localization, locale));
            }
            for (HelpTopic topic : HelpTopic.values()) {
                assertLabels(
                        provider.forTopic(topic.name()).orElseThrow(),
                        placeholder,
                        localization,
                        locale);
            }
        }
    }

    private static void assertLabels(
            String text,
            Pattern placeholder,
            LocalizationService localization,
            SupportedLocale locale) {
        Matcher matcher = placeholder.matcher(text);
        while (matcher.find()) {
            assertThat(localization.getMessageOrNull("help.label." + matcher.group(1), locale))
                    .as("label " + matcher.group(1))
                    .isNotNull();
        }
    }
}
