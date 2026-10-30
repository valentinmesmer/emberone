# Workflow-Engine

Design-Notizen für die Backend-Workflow-Engine - zentrale Domäne mit hoher Kopplung (`Workflow` / `WorkflowService`). Package root: `com.emberone.workflow`.

## Mental Model

Die Engine ist **generisch**. Sie besitzt Instanz-Lebenszyklus, Teilnehmer-Routing, Claims, Konsens, Input-Change-Loops und terminale Übergänge. Sie hat **keine eingebauten Fach-Workflows**.

Jeder konkrete Prozess ist ein `WorkflowHandler`-Spring-Bean mit stabilem `WorkflowType`, typisiertem Payload-Validate, Auflösung der Kontext-Organisation und Domänen-Nebeneffekten bei approve/reject/cancel.

Konfiguration (welche Org-Gruppe entscheidet, ob Typ enabled, auto-approve, self-approve) liegt außerhalb des Handlers in standard-templateten, mandanten-defaulteten, per-Org-Mappings und -Definitionen.

Nebenkanäle - Todos, Notifications, Realtime, Document Promotion - subscriben auf Lifecycle-Events und bleiben aus dem Engine-Kern.

```
Typspezifischer POST ──► WorkflowService.open
                            │
                            ├─ handler.validateOpen / resolveContextOrganization
                            ├─ definitionService.resolveEffectiveDefinition (muss enabled sein)
                            ├─ resolverService.resolveParticipants (mappings + static/dynamic orgs)
                            ├─ Instanz persistieren + auto-approve decisions
                            ├─ handler.onOpened
                            └─ events → Todo / Notification / Realtime Adapter
```

Tenant-Baseline-Kataloge (inkl. neue `wf_definitions` nach Deploy) werden idempotent über `TenantBaselineSyncFilter` vor `/api/v1/self` und `/api/v1/auth` synchronisiert, nicht in einzelnen Authorization-Checks.

## Kernentitäten

### `Workflow` (`wf_instances`)

| Feld | Rolle |
|------|-------|
| `workflowTypeId` | Numerischer FK zu `WorkflowType.id` (keine Enum-Spalte) |
| `status` | Siehe [Zustände](#zustände-und-übergänge) |
| `openedByUserId` | Ersteller |
| `subjectEntityType` / `subjectEntityId` | Optionales Subjekt (oft `"USER"` + Opener) |
| `payload` | JSON-Text; Schema gehört dem Handler |
| `openConcurrencyKeys` | JSON-Array stabiler Keys (`org:`, `group:`, `cert:`, `training:`, `user:`); blockiert parallele Offene Anträge desselben Users zum selben Target |
| `decidedAt` | Bei approve / reject / cancel gesetzt |
| `pendingDocumentPromotions` | Promotion-Auswahl des letzten Genehmigers (JSON) |
| collections | `participants`, `decisions`, `inputChanges` (cascade + orphanRemoval) |

### `WorkflowType`

Stabiler Diskriminator als DB-ID, API-Wert und Mapping-Key:

| Enum | id | Zweck |
|------|----|-------|
| `NAME_CHANGE` | 1 | Vor-/Nachname ändern (Nachweis Pflicht) |
| `ADDRESS_CHANGE` | 2 | Privatadresse ändern (Nachweis optional) |
| `CERTIFICATION_RECORD` | 3 | Zertifizierungsnachweis einreichen |
| `TRAINING_COURSE_RECORD` | 4 | Schulungsnachweis einreichen |
| `DRIVING_LICENSE_RECORD` | 5 | Führerschein erfassen oder aktualisieren |
| `ORGANIZATION_JOIN_REQUEST` | 6 | Onboarding für eine Org anfragen (Dual-Approval: Haupt-Org + Ziel-Org; ohne aktuelle Haupt-Org nur die Ziel-Org) |
| `MAIN_ORGANIZATION_CHANGE` | 7 | Haupt-Org wechseln (Dual-Approval: aktuelle + neue Haupt-Org; ohne aktuelle Haupt-Org nur die neue) |
| `ORGANIZATION_LEAVE_REQUEST` | 8 | Austritt aus einer Org (Self); Approve → `OrganizationMembershipExitService.applyApprovedExit` (Hauptorg: `deactivateUser`; Nebenorg: `endMembership(now)`) |
| `ORGANIZATION_GROUP_LEAVE_REQUEST` | 9 | Austritt aus einer Custom-Org-Gruppe |
| `ORGANIZATION_GROUP_JOIN_REQUEST` | 10 | Beitritt zu einer Custom-Org-Gruppe (`MEMBER`; nur mit aktiver Org-Membership) |
| `ONBOARDING_SUBMISSION` | 11 | Onboarding-Einreichung: Wizard-Abschluss in Ziel-Org; Approve → Membership + Step-`promote` (nicht im beginnbaren Katalog; kein `requestInputChange`; Reject → Input-Change) |
| `ORGANIZATION_MEMBERSHIP_END_REQUEST` | 13 | Org `USER_WRITE`: Austritt eines Mitglieds aus der Org; Approve → wie Typ 8 (`OrganizationMembershipExitService`) |
| `ORGANIZATION_USER_MAIN_ORGANIZATION_CHANGE` | 14 | Org `USER_WRITE`: Hauptorganisation eines anderen Mitglieds wechseln (dynamisch: neue Haupt-Org); Approve → `changeMainOrganization` |
| `USER_DOCUMENT_SUBMISSION` | 15 | Persönliches Dokument einreichen (Nachweis Pflicht); Approve → Promotion ins User-Dokumentenarchiv |
| `RANK_REQUEST` | 16 | Dienstgrad beantragen (Self); Open nur ohne offenen `UserRank` und mit aktiver Hauptorg; Approve → `assignFirstRank`; `POST /api/v1/auth/workflows/rank-request` |
| `COURSE_OFFERING_REGISTRATION` | 17 | Lehrgangsanmeldung (Self); Kontext-Org = Anbieter-Org, optional zusätzlich die Hauptorg des Users (`DYNAMIC`); Approve → Teilnehmer oder Warteliste; `POST /api/v1/auth/workflows/course-offering-registration`, siehe [Lehrgangsangebot](courseoffering.md#anmeldung) |
| `COURSE_REGISTRATION_CANCELLATION` | 18 | Lehrgangsabsage eines Teilnehmers (Self); Kontext-Org = Anbieter-Org, die Hauptorg des Users entscheidet immer mit (`DYNAMIC`, bei gleicher Org eine Entscheidung); Approve → Anmeldung `CANCELLED`, Platz frei; `POST /api/v1/auth/workflows/course-registration-cancellation`, siehe [Lehrgangsangebot](courseoffering.md#absage) |

JSON serialisiert als numerische ID. (Frühere Typ-ID 12 - Deaktivierungs-Workflow - ist entfernt; Typ 15 ist heute `USER_DOCUMENT_SUBMISSION`. `V1__Initial_setup.sql` ist leeres Schema-Setup ohne Alt-Daten-Bereinigung.) Neue Typen brauchen neuen Enum-Wert **und** Handler-Bean; Registry scheitert beim Startup bei doppelter Type-ID. Unbekannte Typ-IDs in der API → 404 (`WorkflowType.isKnownId`).

Jeder Typ hat einen **Open-Scope** (`WorkflowOpenScope`): `SELF` (Mein Bereich) oder `ORGANIZATION` (Org-Benutzerverwaltung, `USER_WRITE`). Der Katalog (`WorkflowTypeCatalogDto.openScope`) und Standard-Inhalte (`workflow-type-mappings`, Attribut `openScope`) liefern den Scope für Gruppierung in der UI; die Anzeigenamen sind pro Typ einheitlich (z. B. „Organisation verlassen“ für Selbst- und Verwaltungs-Austritt).

**Exclusivity-Locks** (`wf_exclusivity_locks`, `WorkflowOpenExclusivityKeys`): Handler können `resolveOpenExclusivityKeys` liefern. Pro Mandant darf ein Key nur von **einem** aktiven Workflow gehalten werden - **über Workflow-Typen hinweg** (z. B. Typ 8 + 13 teilen `membership-exit:user:{id}:org:{id}`, Typ 7 + 14 `main-organization:user:{id}`). Locks werden beim Open/Update gesetzt und bei terminalen Status freigegeben. `WorkflowExclusivityLockBackfillRunner` materialisiert Locks für bereits offene Instanzen nach Deploy. Race beim parallelen Open: DB-Unique `(tenant_id, lock_key)` → `WorkflowValidationException` mit Handler-`openExclusivityConflictCode`.

`listedInBeginnableCatalog()` Default `false` (Start nur über Dedicated-UI). Opt-in mit `true` für Typen ohne eigenen Einstieg. `GET …/workflows/beginnable-types` liefert alle Typen, die der User starten darf (`canBeginSelfServiceWorkflow`), inkl. Flag `listedInBeginnableCatalog`. Zusätzlich prüft `WorkflowOpenAccess.canBeginSelfServiceWorkflow` typ-spezifische Voraussetzungen (z. B. `RANK_REQUEST` nur ohne offenen `UserRank`).

Die UI zum Antrag-stellen-Picker listet nur Typen mit `listedInBeginnableCatalog=true`, für die kein Singleton-`typeBlocked` (offene Instanz desselben Typs) gilt.

### Teilnehmer, Slots, Entscheidungen

- **`WorkflowParticipant`** - eine Zeile pro Slot. Unique `(tenant, workflow, slotKey)`. Rolle `APPROVER` oder `VIEWER`. Principal `USER` oder `ORGANIZATION_GROUP`. Approver-Slots tragen `PENDING` / `APPROVED` / `REJECTED` und optional Claim (`claimedByUserId`).
- **`WorkflowDecision`** - eine Entscheidung pro entschiedenem Slot. `userId == null` bedeutet System-Auto-Approve.
- **`WorkflowInputChange`** - Opener muss Payload nach Approver-Anfrage aktualisieren. Status: `OPEN` / `FULFILLED` / `CANCELLED`. Höchstens eine offene Anfrage gleichzeitig.

### Slot Keys (`SlotKeys`)

`SlotKeys` ist **package-private** (`com.emberone.workflow.service`) - Hilfsklasse für Slot-Key-Patterns, keine öffentliche API.

| Art | Pattern |
|-----|---------|
| Org-Group-Approver | `AGR:{organizationId}` |
| Zusätzlich (Handler) | explizit `spec.slotKey` oder `ADD:{role}:{principalType}:{principalId}[:orgId]` |

Open/Reconcile materialisiert Org-Group-Slots (`AGR:…`) und zusätzliche Specs (`ADD:…` / explizites `slotKey`).

## Zustände und Übergänge

```
                    open
                     │
                     ▼
                   OPEN ◄──────────────────────────────┐
                  / |  \                                │
     claim/decide │  │ requestInputChange               │ withdrawInputChange
                  │  ▼                                  │
                  │ AWAITING_INPUT_CHANGE ──updatePayload─┘
                  │         (kann DECISIONS_RESET emittieren)
                  │
     ┌────────────┼────────────┐
     ▼            ▼            ▼
 APPROVED     REJECTED     CANCELLED
 (alle Slots  (ein Reject   (nur Opener,
  approved)   beendet WF)   aktive Status)
```

| Status | Bedeutung |
|--------|-----------|
| `OPEN` | Aktiv; claim / decide / input-change erlaubt |
| `AWAITING_INPUT_CHANGE` | Noch aktiv; Opener muss Payload updaten (oder Approver zieht Anfrage zurück) |
| `APPROVED` | Terminal - jeder Approver-Slot approved |
| `REJECTED` | Terminal - ein Reject beendet den gesamten Workflow |
| `CANCELLED` | Terminal - Opener bricht bei aktivem Status ab |

`WorkflowStatus.isActive()` = `OPEN` \| `AWAITING_INPUT_CHANGE`.

### Konsens-Regeln

1. **Ein Reject beendet den Workflow** - Slot → `REJECTED`, Workflow → `REJECTED`.
2. **All-Approver-Konsens für Approve** - Workflow → `APPROVED` nur wenn jeder Approver-Slot `APPROVED` (`completeIfAllApproved`).
3. **Decide braucht Claim** - entscheidender User muss Claimer sein; Slot muss `PENDING`.
4. **Gruppen-Slots manuell claimen.** Auto-Claim gilt nur für zusätzliche `PrincipalType.USER`-Approver-Specs (`mayAutoClaimUserApprover`) - heute liefert kein Handler `additionalParticipants()` (Default `List.of()`), Org-Group-Slots werden nie auto-geclaimed.
5. **Snapshot beim Open** - Teilnehmermenge einmal materialisiert. Spätere Admin-Config-Änderungen reconcilen offene Instanzen via `reconcileOpenInstancesAfterConfigurationChange`.

### Input-Change / „übernommen“

Sobald ein Gruppen-Slot geclaimt **oder** eine menschliche Entscheidung existiert (`userId != null`), ist der Workflow „übernommen“:

- Opener kann Payload nicht mehr frei editieren.
- Claimer darf `requestInputChange` **wenn** der Handler `allowsInputChangeRequests()` (Default `true`) erlaubt; Opener erfüllt via `updatePayload` (Workflow zurück zu `OPEN`; menschliche Entscheidungen werden bei Bedarf gelöscht, System-Auto-Approves und Claims bleiben). `DECISIONS_RESET` wird nur emittiert, wenn tatsächlich menschliche Entscheidungen oder nicht-pending Approver-Slots zurückgesetzt wurden.
- Handler mit `mapsRejectToInputChange() = true` (z. B. `ONBOARDING_SUBMISSION`) haben kein separates `requestInputChange`: ein Reject mit Pflicht-Kommentar wird zur Input-Change-Anfrage (`AWAITING_INPUT_CHANGE`), ohne terminales `REJECTED`.
- Handler mit `allowsOpenerFreeInputEdit() = false` blockieren freies Payload-Edit im Status `OPEN` (nur Change-Request-Erfüllung).
- `PUT /workflows/{id}/payload` validiert das in den Handler-Typ konvertierte Payload per Bean Validation (`@Size`, `@NotNull` etc. wie `@Valid` beim Öffnen), bevor `validateUpdate` läuft; Verstöße → `WorkflowValidationException` (400).
- Approver-Slots werden nur beim Öffnen aufgelöst. Daher ändern `ORGANIZATION_JOIN_REQUEST` (Ziel-Org) und `ORGANIZATION_GROUP_JOIN_REQUEST` (Gruppe) ihr Ziel beim Payload-Update nicht: abweichende Ziele werden abgelehnt, der Opener muss den Antrag abbrechen und neu öffnen.

### System-Auto-Approve

Bei Type-Mapping mit `autoApprove=true` öffnet Org-Slot schon `APPROVED` mit Entscheidung `userId == null`. Diese überleben Payload-Reset; menschliche nicht.

## `WorkflowService`-API

Datei: `service/WorkflowService.java`.

| Methode | Rolle |
|---------|-------|
| `open` / `openForUser` | Instanz anlegen (nur aus **typspezifischen** Controllern) |
| `getWorkflow` / Inbox / Listing-Helfer | Read Models |
| `claim` / `unclaim` | Gruppen-Approver-Slots |
| `decide` | Einen Slot approven oder Workflow rejecten |
| `withdrawApproval` | Eigene Genehmigung zurücknehmen solange noch Claimer |
| `cancel` | Opener; aktiv → `CANCELLED` |
| `requestInputChange` / `withdrawInputChange` | Approver ↔ Opener-Loop |
| `updatePayload` | Frei editieren oder Change Request erfüllen |
| `getDocumentPromotionPreview` / `updateDocumentPromotionSelections` | Promotion-UI vor Genehmigung |
| `reconcileOpenInstancesAfterConfigurationChange` | Slots nach Admin-Mapping/-Definition syncen |

Open-Sequenz (`WorkflowService.openForUser`):

1. Mitgliedschafts-Check (`requiresOpenerOrganizationMembership`)
2. Type-Concurrency (`allowsMultipleConcurrentOpen` - höchstens eine offene Instanz dieses Typs pro Opener)
3. Handler `validateOpen`, Kontext-Org, optional Open-Permission, dynamic orgs, additional participants
4. Key-Concurrency (`resolveOpenConcurrencyKeys` gegen aktive Instanzen desselben Openers/Typs) und Exclusivity-Konfliktprüfung (`resolveOpenExclusivityKeys` / `assertNoConflict`)
5. Effektive Definition muss `enabled` sein
6. Teilnehmer auflösen (Kontext ∪ static ∪ dynamic, oder nur static wenn `staticApproversExclusive`)
7. Persist → Exclusivity-Locks setzen (`replaceLocks`) → Auto-Approve-Entscheidungen → `handler.onOpened`
8. `OPENED` publizieren wenn pending Slots bleiben; immer `completeIfAllApproved` versuchen (All-Auto-Approve-Short-Circuit)

## Handler SPI

### `WorkflowHandler<P>`

Ein Bean pro Typ. Indexiert von `WorkflowHandlerRegistry` bei `@PostConstruct` (doppelte Type-ID → Startup-Fehler).

Wichtige Hooks:

| Hook | Zweck |
|------|-------|
| `workflowType()` / `mode()` / `payloadType()` | Identität |
| `allowsMultipleConcurrentOpen()` | Type-weite Concurrency: `false` = höchstens eine offene Instanz dieses Typs pro User |
| `resolveOpenConcurrencyKeys` | Target-Keys innerhalb des Typs (`WorkflowOpenConcurrencyKeys`: `org:{id}`, `group:{id}`, `cert:{id}`, `training:{id}`, `user:{id}`). Nicht leer → Open und Payload-Update scheitern, wenn ein Key schon auf einer anderen aktiven Instanz liegt. Persistiert auf `wf_instances.open_concurrency_keys`. |
| `openConcurrencyConflictCode()` | Code/Message bei Key-Kollision; Default `OPEN_CONCURRENCY_KEY_ALREADY_OPEN`, Handler überschreiben mit typspezifischen `*_ALREADY_OPEN`-Codes |
| `requiresOpenerOrganizationMembership()` | Default `true`: Opener braucht eine aktive **Haupt**-Org-Mitgliedschaft, weil die Kontext-/Approver-Org daraus kommt. Mitgliedschaft in anderen Orgs reicht nicht. `false` für Workflows, die Approver ohne aktuelle Haupt-Org auflösen können (Beitrittsanfrage über die Ziel-Org; Haupt-Org-Wechsel über die neue Haupt-Org; Leave-/Group-Join/-Leave über die Ziel-Org bzw. -Gruppe). Die Engine prüft das beim Open und in `WorkflowOpenAccess.canBeginSelfServiceWorkflow`. |
| `mayOpenWithoutActiveOrganizationMembership()` | Default `false`. Nur Handler mit `true` (Organisationsbeitritt per Workflow, Onboarding-Einreichung) dürfen ohne aktive Org-Mitgliedschaft geöffnet werden; `WorkflowOpenAccess` und `WorkflowService.open` prüfen das vor den übrigen Gates. |
| `listedInBeginnableCatalog()` | Default `false` → kein Eintrag unter Self **Anträge → Hinzufügen**, Typ bleibt in `beginnable-types` für Dedicated-UIs; Opt-in `true` nur für Typen ohne eigenen Einstieg |
| `allowsInputChangeRequests()` | Default `true`; `false` → `requestInputChange` API/Capability disabled |
| `mapsRejectToInputChange()` | Default `false`; `true` → Reject mit Kommentar wird Input-Change statt terminalem `REJECTED` |
| `allowsOpenerFreeInputEdit()` | Default `true`; `false` → kein freies Payload-Edit im Status `OPEN` |
| `validateOpen` / `validateUpdate` | Payload-Regeln |
| `resolveContextOrganization` | Basis-Genehmiger-Org |
| `requiredPermissionToOpen()` | Leer = Self-Service |
| `resolveDynamicOrganizations` | nur `DYNAMIC`-Modus |
| `additionalParticipants` | Extra USER/VIEWER/APPROVER Specs |
| `onOpened` / `onInputUpdated` | Docs anhängen, etc. |
| `onInputChangeRequested` / `onInputChangeWithdrawn` | Domänen-Nebeneffekte beim Change-Request (z. B. Onboarding-Einladung wieder `OPEN`) |
| `onApproved` / `onRejected` / `onCancelled` | Domänen-Nebeneffekte |
| `resolveDocumentPromotion*` / `onDocumentsPromoted` | Ausgewählte Medien in Opener-Persönlichen-Ordner kopieren |
| `allowsDocumentPromotion` | `false` sperrt Promotion komplett (leere Preview, keine Übernahme bei Approve); Zertifikats-Workflows mit Nachweis-Scan nutzen Promotion wie andere Zertifikats-Workflows |

`AbstractMembershipContextWorkflowHandler` ist gemeinsame Basis für heutige Handler: Default `STATIC`-Modus, Kontext-Org = aktive Hauptmitgliedschaft des Openers, `requiresOpenerOrganizationMembership() = true`, Draft/Proof-Attachment-Helfer, Default-Promotion-Regeln. Join-/Leave-/Main-Change-Handler setzen `requiresOpenerOrganizationMembership()` auf `false`, weil der Approver aus der Ziel-, Austritts- bzw. neuen Haupt-Org kommt. Join-, Main-Change- und Org-User-Main-Change-Handler sind `DYNAMIC` und liefern die zweite Genehmiger-Org aus dem Payload via `resolveDynamicOrganizations`.

`WorkflowMode.DYNAMIC`: Kontext-Org ∪ dynamische Orgs werden additiv als Approver-Slots materialisiert (`WorkflowResolverService`). Produktiv genutzt von `ORGANIZATION_JOIN_REQUEST`, `MAIN_ORGANIZATION_CHANGE` und `ORGANIZATION_USER_MAIN_ORGANIZATION_CHANGE` (Leadership der beteiligten Orgs; ohne aktuelle Haupt-Org nur die Ziel- bzw. neue Haupt-Org).

### Konkrete Handler

| Handler | Typ | Concurrent Opens | Bei Approve |
|---------|-----|------------------|-------------|
| `NameChangeWorkflowHandler` | `NAME_CHANGE` | nein | `UserService.applyApprovedNameChange` |
| `AddressChangeWorkflowHandler` | `ADDRESS_CHANGE` | nein | `applyApprovedAddressChange` |
| `CertificationRecordWorkflowHandler` | `CERTIFICATION_RECORD` | ja (keine überlappenden Definition-IDs) | `CertificationAssignmentService.createAssignmentsFromWorkflow` |
| `TrainingCourseRecordWorkflowHandler` | `TRAINING_COURSE_RECORD` | ja (keine überlappenden Type-IDs) | `TrainingCourseRecordService.createSubmissionFromWorkflow` |
| `DrivingLicenseRecordWorkflowHandler` | `DRIVING_LICENSE_RECORD` | nein (ein Antrag pro User) | `DrivingLicenseRecordService.upsertFromWorkflow` |
| `OrganizationJoinRequestWorkflowHandler` | `ORGANIZATION_JOIN_REQUEST` | ja (kein zweiter offener Antrag zur selben Ziel-Org) | `OnboardingInvitationService.recordInvitationForAdmission`: bei offenen Steps `OPEN` ohne Membership; bei live erfüllten/keinen Steps Direktaufnahme (`APPROVED` + `ensureOrganizationMembershipAndAllMembersGroup`) |
| `MainOrganizationChangeWorkflowHandler` | `MAIN_ORGANIZATION_CHANGE` | ja (kein zweiter offener Antrag zur selben neuen Haupt-Org) | `UserOrganizationMembershipService.changeMainOrganization` |
| `OrganizationLeaveRequestWorkflowHandler` | `ORGANIZATION_LEAVE_REQUEST` | ja (kein zweiter offener Antrag zur selben Org) | `OrganizationMembershipExitService.applyApprovedExit` (Nebenorg: `endMembership` mit `now`; Hauptorg: `deactivateUser`); Leadership-Guard |
| `OrganizationGroupLeaveRequestWorkflowHandler` | `ORGANIZATION_GROUP_LEAVE_REQUEST` | ja (kein zweiter offener Antrag zur selben Gruppe) | `endActiveMembership`; Leitungs- und All-Members-Gruppen sind **keine** Leave-Targets (`LEADERSHIP_GROUP_LEAVE_NOT_ALLOWED`) |
| `OrganizationGroupJoinRequestWorkflowHandler` | `ORGANIZATION_GROUP_JOIN_REQUEST` | ja (kein zweiter offener Antrag zur selben Gruppe) | `createMembership` als `MEMBER`; Leitungs- und All-Members-Gruppen sind **keine** Join-Targets (`LEADERSHIP_GROUP_JOIN_NOT_ALLOWED`) |
| `OnboardingSubmissionWorkflowHandler` | `ONBOARDING_SUBMISSION` | ja (kein zweiter offener Antrag zur selben Ziel-Org) | Membership + `promote`; `allowsInputChangeRequests=false`, `mapsRejectToInputChange=true`, `allowsOpenerFreeInputEdit=false` |
| `OrganizationMembershipEndRequestWorkflowHandler` | `ORGANIZATION_MEMBERSHIP_END_REQUEST` | ja (`org:` + `user:`; `org:` blockiert denselben Opener+Org bei anderem Ziel-User; `user:` blockiert denselben Ziel-User bei anderer Org - beide Keys) | wie `ORGANIZATION_LEAVE_REQUEST` für Ziel-User (`applyApprovedExit`) |
| `OrganizationUserMainOrganizationChangeWorkflowHandler` | `ORGANIZATION_USER_MAIN_ORGANIZATION_CHANGE` | ja (`user:`; kein zweiter offener Antrag für denselben Ziel-User) | `changeMainOrganization` für Ziel-User; `DYNAMIC` (neue Haupt-Org) |
| `UserDocumentSubmissionWorkflowHandler` | `USER_DOCUMENT_SUBMISSION` | ja (ohne Target-Keys) | Promotion ins User-Dokumentenarchiv (Nachweis Pflicht) |
| `CourseOfferingRegistrationWorkflowHandler` | `COURSE_OFFERING_REGISTRATION` | ja (`course-offering:{id}`: kein zweiter offener Antrag desselben Users für dasselbe Angebot) | `CourseRegistrationService.approve` vergibt unter Zeilen-Lock auf dem Angebot Platz oder Wartelistenplatz; `DYNAMIC`, `Subject = COURSE_OFFERING`, kein Input-Change, kein Payload-Edit; bei Absage des Angebots schließt `CourseOfferingCancellationService` offene Anträge über `WorkflowService.cancelActiveWorkflows` (Status `CANCELLED`, Locks frei, Event `CANCELLED`, **kein** `onCancelled`-Callback, weil die Anmeldung dort mit `endedBy = ORGANIZATION` endet), siehe [Absage des Angebots](courseoffering.md#absage-des-angebots) |
| `CourseRegistrationCancellationWorkflowHandler` | `COURSE_REGISTRATION_CANCELLATION` | ja (`course-registration:{id}`: keine zweite offene Absage derselben Anmeldung) | `CourseRegistrationService.approveCancellation` beendet unter Zeilen-Lock auf dem Angebot die Anmeldung (`endedBy = USER`); Ablehnung und Abbruch geben nur die Absage frei; `DYNAMIC`, `Subject = COURSE_REGISTRATION`, kein Input-Change, kein Payload-Edit |

### Neuen Workflow-Typ hinzufügen

1. `WorkflowType`-Enum-Wert mit neuer stabiler numerischer ID.
2. `WorkflowHandler<P>` implementieren (meist `AbstractMembershipContextWorkflowHandler` erweitern). `requiresOpenerOrganizationMembership()` nur auf `false` setzen, wenn Approver ohne aktive Haupt-Org-Mitgliedschaft auflösbar sind. Bei mehreren parallelen Anträgen desselben Typs `allowsMultipleConcurrentOpen() = true` und `resolveOpenConcurrencyKeys` mit Prefix aus `WorkflowOpenConcurrencyKeys` liefern.
3. Typspezifischen Controller, der nur `POST` open mit typisiertem Payload macht.
4. Standard-Templates für Type-Mapping + Definition, damit Mandanten/Orgs aktivieren und routen können.
5. Optional: Document-Promotion-Hooks; Notification/Todo-Verhalten folgt automatisch aus Lifecycle-Events.

## Konfigurationsebenen

Drei gestapelte Ebenen (äußere überschreibt / materialisiert aus inneren Templates):

### 1. Per-Org Type Mapping - `WorkflowTypeMapping` (`wf_type_mappings`)

Welche **Organisationsgruppe** für einen Typ in einer Org entscheidet, plus `autoApprove`. Unique `(tenant, org, workflowTypeId)`.

- Runtime-Service: `WorkflowTypeMappingService`
- Standard-Domain: `WorkflowTypeMappingStandardDomain` (`domainKey = "workflow-type-mappings"`)
- Templates tragen `orgGroupConfigKey` (z. B. leadership), `configKey` = Type-ID-String, `enabled`, `selfApproveAllowed`

### 2. Per-Org Definition - `WorkflowDefinition` (`wf_definitions`)

`enabled`, `staticApproversExclusive`, `selfApproveAllowed`, statische Approver-Orgs. Beim Open via `WorkflowDefinitionService.resolveEffectiveDefinition(typeId, contextOrgId)`. Self-Approve wird **live** gegen aktuelle Definition geprüft (`WorkflowSelfApproveService`), nicht Open-Time-Snapshot. Ist Self-Approve blockiert, setzt `WorkflowTodoAdapter` den Opener bei Gruppen-Claim-Todos als `TodoExcludedUser` (siehe [Todo](todo.md)).

### 3. Mandantenweite Defaults - `TenantWorkflowDefinition`

Mandanten-Einstellungen für static exclusivity + static Org-Liste; propagiert in per-Org-Definitionen. Admin-APIs: `AdminWorkflowDefinitionController`, `AdminWorkflowTypeCatalogController`.

Config-Change → `WorkflowConfigurationChangedEvent` → offene Instanzen reconcilen (pending Slots add/drop, Auto-Approve neu, ggf. complete).

## Controller

| Controller | Pfad | Rolle |
|------------|------|-------|
| Typspezifisch Self (`*WorkflowController`) | `/api/v1/auth/workflows/{kebab-type}` | **Nur** `POST` open (Self-Scope, z. B. `user-document-submission`) |
| Org-Open Typ 13 | `POST …/organizations/{orgId}/users/{userId}/workflows/organization-membership-end` | Org `USER_WRITE`: Austritt eines Mitglieds öffnen |
| Org-Open Typ 14 | `POST …/organizations/{orgId}/users/{userId}/workflows/organization-user-main-organization-change` | Org `USER_WRITE`: Haupt-Org-Wechsel eines Mitglieds öffnen |
| `WorkflowController` | `/api/v1/auth/workflows` | Generische Instanz-Ops (get, decide, claim, cancel, payload, input-change, promotions). **Erstellt nie.** |
| `WorkflowSelfController` | `/api/v1/self/workflows` | Paginierte Listen `GET …/opened`, `GET …/participations` (`ListQuery`, Filter `status` (mehrfach, Werte aus `WorkflowStatus`; Default je Liste im Frontend, ohne Wert keine Einschränkung)), `GET …/pending-count`, beginnbare Typen (`@BoundedList`), `GET …/open-concurrency-keys` (`@BoundedList`), `GET …/open-state/{workflowTypeId}` |
| `WorkflowOrganizationController` | `/api/v1/auth/organizations/{orgId}/workflows` | Paginierte Liste `GET …/open` (`ListQuery`, Filter `status` (mehrfach, Werte aus `WorkflowStatus`; Default je Liste im Frontend, ohne Wert keine Einschränkung); `WORKFLOW_READ`) |
| `WorkflowUserOrganizationController` | `…/users/{userId}/workflows` | Paginierte Liste der vom User eröffneten Workflows (`ListQuery`, Filter `status` (mehrfach, Werte aus `WorkflowStatus`; Default je Liste im Frontend, ohne Wert keine Einschränkung); `WORKFLOW_READ`) |

Workflow-Listen unterstützen Client-`sort` (je `asc`/`desc`): `openedAt` (Standard `openedAt,desc`); ohne `sort` gilt der Standard.

Org-Open-Pfade (Typen 13/14) liegen unter `/api/v1/auth/organizations/{orgId}/users/{userId}/workflows/…`, nicht unter `/workflows/{kebab-type}`. Listing derselben Prefixe: `WorkflowUserOrganizationController`.

Design-Regel (aus `WorkflowService.open` Javadoc): konkreter Business-Endpoint öffnet; generischer Controller erstellt nie.

Picker-Hilfen für Org-Workflows (nicht im Workflow-Package): `GET /api/v1/self/organizations/joinable` (ohne Membership und ohne pending Onboarding), `…/main-change-targets`, `…/leave-targets` (ohne Orgs, in denen der User in der Leitungsgruppe ist), `…/group-leave-targets` (ohne Leadership/ALL_MEMBERS) und `…/group-join-targets` (ohne Leadership/ALL_MEMBERS und ohne bestehende Mitgliedschaft) in `OrganizationSelfController`. Leadership-Austritt ist serverseitig unmöglich; siehe [Org- und Gruppen-Mitgliedschaften](user-memberships.md#leadership-blockiert-org-ende-und-austritt).

Außerdem: `WorkflowCommentController`, `WorkflowDocumentController`, Org/User-Listing-Controller, Admin Definition/Catalog.

## Lifecycle-Events & Adapter

`WorkflowLifecycleEvent`-Arten: `OPENED`, `APPROVED`, `REJECTED`, `CANCELLED`, `CLAIMED`, `UNCLAIMED`, `DECISION_RECORDED`, `APPROVAL_WITHDRAWN`, `APPROVER_SLOT_REMOVED`, `INPUT_CHANGE_REQUESTED`, `INPUT_CHANGE_WITHDRAWN`, `INPUT_UPDATED`, `DECISIONS_RESET`, `UPDATED`, `COMMENT_CREATED`.

`WorkflowEventPublisher` fächert an alle `WorkflowEventListener`s aus (Listener-Fehler werden geloggt, nicht rethrown):

| Adapter | Effekt |
|---------|--------|
| `WorkflowTodoAdapter` | Todos öffnen/archivieren für Claim, Approval, Input-Change; Restore nach `DECISIONS_RESET` |
| `WorkflowNotificationAdapter` | Ersteller-Notifications approved/rejected/input-change |
| `WorkflowRealtimeAdapter` | UI-Push: Entity-Topic `workflow.{id}` + User-Queue `/user/queue/workflows` an Empfänger via `WorkflowRecipientResolver` (Opener, Participants inkl. aktiver Gruppenmitglieder, Decisions, Claimed-By) |

Document Promotion (`WorkflowDocumentPromotionService`) bei voller Genehmigung: ausgewählte Workflow-/Comment-Medien in Opener-`UserDocument`-Ordner, dann `handler.onDocumentsPromoted`.

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Instanz + Lifecycle | `workflow/service/WorkflowService.java` |
| Domänenmodell | `workflow/domain/*` |
| Handler SPI | `workflow/spi/WorkflowHandler.java`, `WorkflowHandlerRegistry.java` |
| Open-Concurrency | `workflow/service/WorkflowOpenConcurrencyService.java`, `workflow/spi/WorkflowOpenConcurrencyKeys.java`; Frontend `lib/workflows/openConcurrency.ts` |
| Teilnehmer-Auflösung | `workflow/service/WorkflowResolverService.java`, package-private `SlotKeys.java` |
| Definitionen / Mappings | `workflow/service/WorkflowDefinitionService.java`, `WorkflowTypeMappingService.java` |
| Standard-Domains | `workflow/standard/WorkflowTypeMappingStandardDomain.java`, `WorkflowDefinitionStandardDomain.java` |
| Konkrete Handler | `workflow/{namechange,addresschange,certificationrecord,trainingcourse,drivinglicense,organizationjoinrequest,mainorganizationchange,organizationleaverequest,organizationgroupleaverequest,organizationgroupjoinrequest,onboardingsubmission,organizationmembershipend,organizationusermainorganizationchange,userdocumentsubmission}/` (je Typ ein Package mit `*WorkflowHandler`) |
| Events | `workflow/event/WorkflowLifecycleEvent.java`, `WorkflowEventPublisher.java` |
| Todos / Notifications | `todo/adapter/workflow/`, `notification/adapter/workflow/` |
| Realtime | `workflow/realtime/{WorkflowRealtimeAdapter,WorkflowRealtimeNotifier,WorkflowRecipientResolver}.java` |

Siehe auch: [Dienstgrad und Funktion](rank-and-function.md) (`RANK_REQUEST`, Id 16).
