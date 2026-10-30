/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.spi;

import com.emberone.permission.domain.PermissionType;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowMode;
import com.emberone.workflow.domain.WorkflowOpenScope;
import com.emberone.workflow.domain.WorkflowStatus;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.WorkflowDocumentPromotionDefaults;
import com.emberone.workflow.dto.WorkflowDocumentPromotionMetadata;
import com.emberone.workflow.exception.WorkflowValidationException;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for a single workflow type. One Spring bean per {@link WorkflowType}. The engine
 * has no built-in workflows; each handler declares a stable {@link WorkflowType} whose id matches
 * DB rows and API payloads.
 *
 * @param <P> the typed payload schema for this workflow type.
 */
public interface WorkflowHandler<P> {

    WorkflowType workflowType();

    default long workflowTypeId() {
        return workflowType().getId();
    }

    WorkflowMode mode();

    Class<P> payloadType();

    /**
     * Whether the opener may have more than one workflow of this type in {@link
     * WorkflowStatus#OPEN} at the same time (same tenant). When {@code false}, {@link
     * com.emberone.workflow.service.WorkflowService#open} rejects opening if the user already has
     * an open instance of this workflow type.
     */
    boolean allowsMultipleConcurrentOpen();

    /**
     * Stable keys describing which logical targets this open request occupies within its workflow
     * type. When non-empty, the engine rejects opening or updating to keys that already exist on
     * another active instance for the same user and tenant.
     */
    default List<String> resolveOpenConcurrencyKeys(WorkflowOpenContext<P> ctx, Workflow existing) {
        return List.of();
    }

    /** Error code when {@link #resolveOpenConcurrencyKeys} conflicts with an active instance. */
    default String openConcurrencyConflictCode() {
        return WorkflowValidationException.CODE_OPEN_CONCURRENCY_KEY_ALREADY_OPEN;
    }

    /** Client-facing message for {@link #openConcurrencyConflictCode()}. */
    default String openConcurrencyConflictMessage() {
        return "You already have an open workflow for this target; finish or cancel it before opening another";
    }

    /**
     * Optional cross-type locks: at most one active workflow per tenant may hold a given key (see
     * {@link WorkflowOpenExclusivityKeys}). Used when self-service and org-management types share
     * the same business operation.
     */
    default List<String> resolveOpenExclusivityKeys(WorkflowOpenContext<P> ctx, Workflow existing) {
        return List.of();
    }

    default String openExclusivityConflictCode() {
        return WorkflowValidationException.CODE_OPEN_EXCLUSIVITY_ALREADY_OPEN;
    }

    default String openExclusivityConflictMessage() {
        return "A related workflow is already open for this target; finish or cancel it before opening another";
    }

    /** {@link WorkflowType#openScope()} for catalog and admin UI. */
    default WorkflowOpenScope openScope() {
        return workflowType().openScope();
    }

    /**
     * When {@code true} (the default), the opener must have an active <em>main</em> organization
     * membership because that organization is the context approver slot. Membership in other
     * organizations is not enough. Workflows that can resolve approvers without a current main org
     * (join request via the target org, main-organization change via the new main org) override
     * this to {@code false}.
     */
    default boolean requiresOpenerOrganizationMembership() {
        return true;
    }

    /**
     * When {@code true}, the opener may start this workflow type without any active organization
     * membership (e.g. organization join request, onboarding submission after admission).
     */
    default boolean mayOpenWithoutActiveOrganizationMembership() {
        return false;
    }

    /**
     * When {@code true}, Self Anträge → Hinzufügen lists this type. Default {@code false}: open
     * only from a dedicated entry point. The type still appears in {@code GET
     * …/workflows/beginnable-types} when the user may begin it, so dedicated UIs can gate their
     * actions.
     */
    default boolean listedInBeginnableCatalog() {
        return false;
    }

    /**
     * When {@code true} (the default), claimers may call {@code requestInputChange}. When {@code
     * false}, that API is disabled for this type.
     */
    default boolean allowsInputChangeRequests() {
        return true;
    }

    /**
     * When {@code true}, a reject decision becomes an input-change request (comment required)
     * instead of terminating the workflow. Default {@code false}.
     */
    default boolean mapsRejectToInputChange() {
        return false;
    }

    /**
     * When {@code true} (the default), the opener may freely edit the payload while the workflow is
     * {@link WorkflowStatus#OPEN} and not taken over. When {@code false}, only fulfillment of
     * {@link WorkflowStatus#AWAITING_INPUT_CHANGE} is allowed (e.g. onboarding edits happen in the
     * invitation wizard).
     */
    default boolean allowsOpenerFreeInputEdit() {
        return true;
    }

    /**
     * Validates that the opener may open this workflow and that the payload is valid; throws on
     * violation.
     */
    void validateOpen(WorkflowOpenContext<P> ctx);

    /**
     * The internal base organization that is always an approver slot (chosen per workflow type, not
     * a global default).
     */
    Long resolveContextOrganization(WorkflowOpenContext<P> ctx);

    /**
     * If empty, this is a <em>self-service</em> workflow: any caller with a linked user id and
     * tenant on the self API may open it (and use type-specific prep endpoints such as proof
     * upload), subject to {@link #validateOpen}.
     *
     * <p>If present, the opener must hold that permission in the organization returned by {@link
     * #resolvePermissionCheckOrganization(WorkflowOpenContext, Long)} (same rules as in {@code
     * WorkflowService} when opening). Global admin skips the permission check when opening.
     */
    default Optional<PermissionType> requiredPermissionToOpen() {
        return Optional.empty();
    }

    /**
     * Context organization for access checks <em>before</em> an open payload exists (e.g. upload
     * proof). Uses the same rules as opening with a {@code null} payload where applicable.
     *
     * <p>Default delegates to {@link #resolveContextOrganization(WorkflowOpenContext)} with a
     * {@code null} payload. Override if the context organization cannot be resolved without a
     * concrete payload.
     */
    default Long resolveSecurityContextOrganization(Long openedByUserId, Long tenantId) {
        WorkflowOpenContext<P> ctx = new WorkflowOpenContext<>(openedByUserId, tenantId, null);
        return resolveContextOrganization(ctx);
    }

    /**
     * Organization context for an existing workflow instance (e.g. org-scoped read access).
     * Defaults to {@link #resolveSecurityContextOrganization(Long, Long)}; override when the
     * context organization is only known from a persisted payload.
     */
    default Long resolveInstanceOrganization(Workflow workflow) {
        return resolveSecurityContextOrganization(
                workflow.getOpenedByUserId(), workflow.getTenantId());
    }

    /**
     * Organization used to evaluate {@link #requiredPermissionToOpen()} for the current user via
     * {@link
     * com.emberone.platform.security.OrganizationAccessService#hasOrganizationPermission(Long,
     * PermissionType)}. Defaults to the resolved context organization.
     *
     * @param contextOrganizationId result of {@link
     *     #resolveContextOrganization(WorkflowOpenContext)}
     */
    default Long resolvePermissionCheckOrganization(
            WorkflowOpenContext<P> ctx, Long contextOrganizationId) {
        return contextOrganizationId;
    }

    /**
     * Additional approver orgs computed at runtime; only relevant for {@link WorkflowMode#DYNAMIC}.
     */
    default List<ApproverOrg> resolveDynamicOrganizations(WorkflowOpenContext<P> ctx) {
        return List.of();
    }

    /** Additional USER approvers or viewers (person/group) contributed by the handler. */
    default List<ParticipantSpec> additionalParticipants(WorkflowOpenContext<P> ctx) {
        return List.of();
    }

    /**
     * Called after the workflow instance is persisted (id assigned). Use for linking supporting
     * documents to the instance (e.g. move proof from draft storage to {@code
     * tenants/{tenantId}/workflows/{workflowId}/…}).
     */
    default void onOpened(Workflow workflow) {}

    /**
     * Validates that the opener may update the payload of an existing workflow (free edit or
     * change-request fulfillment). Default reuses {@link #validateOpen}.
     */
    default void validateUpdate(WorkflowOpenContext<P> ctx, Workflow existing) {
        validateOpen(ctx);
    }

    /**
     * Called after the workflow payload was updated and the instance was persisted. Use for
     * re-linking supporting documents similar to {@link #onOpened}.
     */
    default void onInputUpdated(Workflow workflow) {}

    /**
     * Called after an approver requested an input change and the instance was persisted as {@link
     * WorkflowStatus#AWAITING_INPUT_CHANGE}. Use for domain side effects such as reopening an
     * onboarding invitation.
     */
    default void onInputChangeRequested(Workflow workflow) {}

    /**
     * Called after the requester withdrew an open input-change request and the instance was
     * persisted back to {@link WorkflowStatus#OPEN}.
     */
    default void onInputChangeWithdrawn(Workflow workflow) {}

    /**
     * Whether workflow media may be copied into the opener's personal document folder at all. When
     * {@code false}, the promotion preview is empty and approver selections are rejected/ignored.
     */
    default boolean allowsDocumentPromotion(Workflow workflow) {
        return true;
    }

    /**
     * Defaults for copying workflow media into the opener's personal document folder on approval.
     * Most workflows include workflow attachments and exclude comment attachments.
     */
    default WorkflowDocumentPromotionDefaults resolveDocumentPromotionDefaults(Workflow workflow) {
        return WorkflowDocumentPromotionDefaults.builder()
                .includeWorkflowMedia(true)
                .includeCommentMedia(false)
                .build();
    }

    /**
     * Default title, description, and tags when workflow media is copied into the opener's document
     * folder. Override when metadata depends on workflow payload (e.g. certification definition
     * name).
     */
    default Optional<WorkflowDocumentPromotionMetadata> resolveDocumentPromotionMetadata(
            Workflow workflow) {
        return Optional.empty();
    }

    default void onApproved(Workflow workflow) {}

    /**
     * Called after workflow media has been promoted into the opener's personal document folder.
     * {@code documentIds} lists the created {@code UserDocument} ids (empty when nothing was
     * promoted).
     */
    default void onDocumentsPromoted(Workflow workflow, List<Long> documentIds) {}

    default void onRejected(Workflow workflow) {}

    default void onCancelled(Workflow workflow) {}
}
