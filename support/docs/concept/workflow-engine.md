# Workflow-Engine

Design-Notizen für die Backend-Workflow-Engine - zentrale Domäne mit hoher Kopplung (`Workflow` / `WorkflowService`). Package root: `com.emberone.workflow`.

## Mental Model

Die Engine ist **generisch**. Sie besitzt Instanz-Lebenszyklus, Teilnehmer-Routing, Claims, Konsens, Input-Change-Loops und terminale Übergänge. Sie hat **keine eingebauten Fach-Workflows**.

Jeder konkrete Prozess ist ein `WorkflowHandler`-Spring-Bean mit stabilem `WorkflowType`, typisiertem Payload-Validate, Auflösung der Kontext-Organisation und Domänen-Nebeneffekten bei approve/reject/cancel.

Konfiguration (welche Org-Gruppe entscheidet, ob Typ enabled, auto-approve, self-approve) liegt außerhalb des Handlers in standard-templateten, mandanten-defaulteten, per-Org-Mappings und -Definitionen. Gruppe, auto-approve und self-approve gelten **pro Freigabe-Rolle** (siehe [Freigabe-Rollen](#freigabe-rollen)).

Nebenkanäle - Todos, Notifications, Realtime, Document Promotion - subscriben auf Lifecycle-Events und bleiben aus dem Engine-Kern.

```
Typspezifischer POST ──► WorkflowService.open
                            │
                            ├─ handler.validateOpen / resolveContextOrganization
                            ├─ definitionService.resolveEffectiveDefinition (muss enabled sein)
                            ├─ routingService.resolve + resolverService.resolveParticipants (mappings + Mandanten-Regel + dynamic orgs)
                            ├─ Instanz persistieren + auto-approve decisions
                            ├─ handler.onOpened
                            └─ events → Todo / Notification / Realtime Adapter
```

Die Tenant-Baseline (inkl. neue `wf_definitions`) wird beim Anlegen des Mandanten geseedet und für bestehende Mandanten nach einem Release per `POST /api/v1/auth/admin/tenants/{tenantId}/baseline-sync` nachgezogen (siehe [Standards](standard.md#baseline-sync)); nie im Request-Pfad und nicht in einzelnen Authorization-Checks.

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

**Exclusivity-Locks** (`wf_exclusivity_locks`, `WorkflowOpenExclusivityKeys`): Handler können `resolveOpenExclusivityKeys` liefern. Pro Mandant darf ein Key nur von **einem** aktiven Workflow gehalten werden - **über Workflow-Typen hinweg** (z. B. Typ 8 + 13 teilen `membership-exit:user:{id}:org:{id}`, Typ 7 + 14 `main-organization:user:{id}`). Locks werden beim Open/Update gesetzt und bei terminalen Status freigegeben. Es gibt keinen Backfill: eine neue `resolveOpenExclusivityKeys`-Implementierung sperrt bereits offene Instanzen erst beim nächsten Update. Race beim parallelen Open: DB-Unique `(tenant_id, lock_key)` → `WorkflowValidationException` mit Handler-`openExclusivityConflictCode`.

`listedInBeginnableCatalog()` Default `false` (Start nur über Dedicated-UI). Opt-in mit `true` für Typen ohne eigenen Einstieg. `GET …/workflows/beginnable-types` liefert alle Typen, die der User starten darf (`canBeginSelfServiceWorkflow`), inkl. Flag `listedInBeginnableCatalog` und `typeBlocked` (Singleton-Typ, der User hat bereits eine aktive Instanz; eine Query für alle Typen, kein Open-State-Aufruf pro Typ) (Logik in `BeginnableWorkflowTypeService`, auch vom KI-Hilfe-Chat genutzt, siehe [KI-Assistenz](ai-assistance.md)). Zusätzlich prüft `WorkflowOpenAccess.canBeginSelfServiceWorkflow` typ-spezifische Voraussetzungen (z. B. `RANK_REQUEST` nur ohne offenen `UserRank`).

Die UI zum Antrag-stellen-Picker listet nur Typen mit `listedInBeginnableCatalog=true`, für die kein Singleton-`typeBlocked` (offene Instanz desselben Typs, Feld des Katalogeintrags) gilt.

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

Open/Reconcile materialisiert Org-Group-Slots (`AGR:…`) und zusätzliche Specs (`ADD:…` / explizites `slotKey`). Jeder Org-Slot trägt zusätzlich die Rolle, in der die Org entscheidet (`wf_participants.slot_role`, `WorkflowParticipantDto.slotRole`); der Slot-Key bleibt `AGR:{organizationId}`, eine Org ist also immer genau ein Slot. User-Slots (`ADD:…`) tragen `CONTEXT`.

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

Hat die Rolle eines Org-Slots im Type-Mapping `autoApprove=true`, öffnet der Slot schon `APPROVED` mit Entscheidung `userId == null`. Jede Rolle entscheidet das für sich: ein Antrag kann z. B. in der Rolle `CONTEXT` auf Menschen warten und in `NEW_MAIN_ORG` automatisch freigegeben sein. Diese Entscheidungen überleben Payload-Reset; menschliche nicht.

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
6. Zusätzliche Orgs bestimmen: `WorkflowApproverRoutingService.resolve(typeId, routingSubjectIds)` (Mandanten-Regeln, siehe [Konfigurationsebenen](#konfigurationsebenen)); Teilnehmer auflösen (Kontext ∪ zusätzliche Orgs ∪ dynamic, oder nur die zusätzlichen Orgs wenn die Auflösung „exklusiv“ liefert)
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
| `resolveDynamicOrganizations` | nur `DYNAMIC`-Modus. Liefert `ApproverOrg(organizationId, slotRole)`; die Rolle muss in `approverSlotRoles()` stehen, sonst bricht die Engine mit `WorkflowConfigurationException` ab |
| `approverSlotRoles()` | Default `[CONTEXT]`. Deklariert die Rollen des Typs (siehe [Freigabe-Rollen](#freigabe-rollen)); `ADDITIONAL` nie angeben, das hängt die Engine an. `WorkflowHandlerRegistry` prüft beim Start: `CONTEXT` vorhanden, kein `ADDITIONAL`, keine Doppelten |
| `routingDimension()` / `resolveRoutingSubjectIds(ctx)` | Optional, beide Default leer. Machen den Typ in den Mandanten-Einstellungen nach Untertyp teilbar (siehe [Regeln je Untertyp](#regeln-je-untertyp)). `routingDimension()` nennt den Katalog (`WorkflowRoutingDimension`), `resolveRoutingSubjectIds` liefert dessen Ids für den konkreten Antrag, **serverseitig** aus Payload oder referenzierten Entitäten, nie aus Anzeigefeldern des Clients. Leer = Standard-Regel. |
| `additionalParticipants` | Extra USER/VIEWER/APPROVER Specs |
| `onOpened` / `onInputUpdated` | Docs anhängen, etc. |
| `onInputChangeRequested` / `onInputChangeWithdrawn` | Domänen-Nebeneffekte beim Change-Request (z. B. Onboarding-Einladung wieder `OPEN`) |
| `onApproved` / `onRejected` / `onCancelled` | Domänen-Nebeneffekte |
| `resolveDocumentPromotion*` / `onDocumentsPromoted` | Ausgewählte Medien in Opener-Persönlichen-Ordner kopieren |
| `allowsDocumentPromotion` | `false` sperrt Promotion komplett (leere Preview, keine Übernahme bei Approve); Zertifikats-Workflows mit Nachweis-Scan nutzen Promotion wie andere Zertifikats-Workflows |

`AbstractMembershipContextWorkflowHandler` ist gemeinsame Basis für heutige Handler: Default `STATIC`-Modus, Kontext-Org = aktive Hauptmitgliedschaft des Openers, `requiresOpenerOrganizationMembership() = true`, Draft/Proof-Attachment-Helfer, Default-Promotion-Regeln. Join-/Leave-/Main-Change-Handler setzen `requiresOpenerOrganizationMembership()` auf `false`, weil der Approver aus der Ziel-, Austritts- bzw. neuen Haupt-Org kommt. Join-, Main-Change- und Org-User-Main-Change-Handler sind `DYNAMIC` und liefern die zweite Genehmiger-Org aus dem Payload via `resolveDynamicOrganizations` (mit ihrer Rolle).

`WorkflowMode.DYNAMIC`: Kontext-Org ∪ dynamische Orgs werden additiv als Approver-Slots materialisiert (`WorkflowResolverService`), jede in ihrer Rolle. Produktiv genutzt von `ORGANIZATION_JOIN_REQUEST`, `MAIN_ORGANIZATION_CHANGE` und `ORGANIZATION_USER_MAIN_ORGANIZATION_CHANGE` (Leadership der beteiligten Orgs; ohne aktuelle Haupt-Org nur die Ziel- bzw. neue Haupt-Org).

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
4. Standard-Templates für Type-Mapping + Definition, damit Mandanten/Orgs aktivieren und routen können. Liefert der Handler dynamische Orgs, `approverSlotRoles()` um eine Rolle je Art der dynamischen Org erweitern und die Konstante in `WorkflowApproverSlotRoles` anlegen (Key nie umbenennen: er steht in der DB und in der API); Label und Erklärung unter `workflows.slotRoles.<KEY>` / `workflows.slotRoleHints.<KEY>` in `locales/de.json` ergänzen. Bestehende Standards bekommen die neue Rolle beim Baseline-Sync mit den Werten von `CONTEXT`.
5. Optional: nach Untertyp teilbar machen (`routingDimension()` + `resolveRoutingSubjectIds`), siehe [Regeln je Untertyp](#regeln-je-untertyp).
6. Optional: Document-Promotion-Hooks; Notification/Todo-Verhalten folgt automatisch aus Lifecycle-Events.

## Freigabe-Rollen

Eine **Rolle** sagt, in welcher Eigenschaft eine Org über einen Antrag entscheidet. Gruppe, `autoApprove` und `selfApproveAllowed` werden pro Rolle konfiguriert. Die Keys stehen in `WorkflowApproverSlotRoles`:

| Rolle | Bedeutung | Vorkommen |
|-------|-----------|-----------|
| `CONTEXT` | Kontext-Org des Antrags (`resolveContextOrganization`) | jeder Typ |
| `ADDITIONAL` | Zusätzliche Orgs aus den Mandanteneinstellungen (Standard-Regel und Regeln je Untertyp) | jeder Typ, hängt die Engine an |
| `NEW_MAIN_ORG` | Org, zu der die Hauptmitgliedschaft wechselt | `MAIN_ORGANIZATION_CHANGE`, `ORGANIZATION_USER_MAIN_ORGANIZATION_CHANGE` |
| `TARGET_ORG` | Org, bei der der Beitritt beantragt wird | `ORGANIZATION_JOIN_REQUEST` |
| `HOME_ORG` | Heimat-Org der teilnehmenden Person | `COURSE_OFFERING_REGISTRATION`, `COURSE_REGISTRATION_CANCELLATION` |

Bei den Haupt-Org-Wechseln ist `CONTEXT` die aktuelle Haupt-Org, beim Beitritt die aktuelle Org des Antragstellers (ohne sie die Ziel-Org), bei den Lehrgangs-Workflows der Veranstalter. Alle anderen Handler haben nur `CONTEXT` (+ `ADDITIONAL`).

**Auflösung** (`WorkflowResolverService.resolveApproverOrganizationIds`) liefert eine geordnete Map Org-Id → Rolle: `CONTEXT`, dann die dynamischen Orgs (mit ihrer Rolle), dann die zusätzlichen Orgs als `ADDITIONAL`. **Dieselbe Org in zwei Rollen ist ein Slot, `CONTEXT` gewinnt**, danach die zuerst genannte Rolle (`putIfAbsent`); es gelten die Einstellungen dieser einen Rolle. Das gilt auch bei „Nur diese Orgs entscheiden“: dann entscheiden nur die zusätzlichen Orgs (`ADDITIONAL`), steht die Kontext-Org darunter, ist ihr Slot `CONTEXT`.

**Rolle deklarieren:** Handler überschreibt `approverSlotRoles()` (`[CONTEXT, <Rolle>]`) und gibt in `resolveDynamicOrganizations` `new ApproverOrg(orgId, <Rolle>)` zurück.

**Neue Rolle:** Eine neue Handler-Rolle braucht die Slot-Zeilen auf Templates und Org-Instanzen, über eine Migration oder den Baseline-Sync (`POST …/tenants/{id}/baseline-sync` pro Mandant, siehe [Standards](standard.md#freigabe-rollen-im-standard)). Fehlt die Zeile, scheitert `WorkflowTypeMappingService.requireSlot` mit einer `WorkflowConfigurationException` (fail closed, wie bei einem fehlenden Mapping); es gibt keinen Fallback auf `CONTEXT`.

## Konfigurationsebenen

Drei gestapelte Ebenen. Die zusätzlichen Genehmiger-Orgs kommen **nur** aus Ebene 3; Ebene 1 und 2 tragen keine Kopie davon.

### 1. Per-Org Type Mapping - `WorkflowTypeMapping` (`wf_type_mappings` + `wf_type_mapping_slots`)

Pro Typ und Org eine Zeile (Unique `(tenant, org, workflowTypeId)`), pro Rolle eine Slot-Zeile (`WorkflowTypeMappingSlot`, Unique `(tenant, mapping, slot_role)`) mit **Org-Gruppe**, `autoApprove` und `selfApproveAllowed`. Alle Tabellen FORCE RLS.

- Runtime-Service: `WorkflowTypeMappingService` (`requireMapping` prüft die Existenz, `requireSlot(orgId, typeId, role)` liefert die Einstellungen einer Rolle)
- Standard-Domain: `WorkflowTypeMappingStandardDomain` (`domainKey = "workflow-type-mappings"`)
- Templates (`std_workflow_type_mapping_templates` + `…_template_slots`): `configKey` = Type-ID-String, `enabled` am Template, je Rolle `orgGroupConfigKey` (z. B. leadership), `autoApprove`, `selfApproveAllowed`

### 2. Per-Org Definition - `WorkflowDefinition` (`wf_definitions`)

Nur `enabled` (Org-Instanz, materialisiert aus dem Standard-Template). Es gibt hier **keine** statischen Approver-Orgs, kein `staticApproversExclusive` und kein `selfApproveAllowed` mehr (liegt pro Rolle am Mapping-Slot). Beim Open via `WorkflowDefinitionService.resolveEffectiveDefinition(typeId, contextOrgId)`.

**Self-Approve** wird **pro Freigabe-Slot** und **live** gegen die Einstellung der Rolle des Slots in der Org des Slots geprüft (`WorkflowSelfApproveService.isAllowed(workflow, participant)`), nicht als Open-Time-Snapshot: Bei mehreren Freigabe-Orgs entscheidet jede Org nur über ihren eigenen Slot, die API liefert das Ergebnis als `selfApproveAllowed` je Participant. Vor dem Open (kein Workflow) und für User-Approver nutzt `isAllowedForSlot`/die Pre-Open-Variante die Rolle `CONTEXT`. Für Input-Change-Requests braucht der Opener einen übernommenen Slot, dessen Rolle Self-Approve erlaubt. Ist Self-Approve für einen Slot blockiert, setzt `WorkflowTodoAdapter` den Opener beim Gruppen-Claim-Todo dieses Slots als `TodoExcludedUser` (siehe [Todo](todo.md)).

### 3. Mandantenweite Defaults - `TenantWorkflowDefinition`

Einzige Quelle der zusätzlichen Genehmiger-Orgs (`tenant_workflow_definition_static_orgs`) und des Schalters „Nur diese Orgs entscheiden“ (`staticApproversExclusive`). Das ist die **Standard-Regel** des Typs. Es wird nichts mehr in die Org-Definitionen oder Standard-Templates kopiert; zur Laufzeit liest `WorkflowApproverRoutingService` direkt von hier. Admin-APIs: `AdminWorkflowDefinitionController`, `AdminWorkflowTypeCatalogController` (liefert pro Typ `routingDimension`, `null` wenn nicht teilbar).

#### Regeln je Untertyp

Typen mit `routingDimension()` können zusätzlich Regeln je Untertyp haben (`tenant_workflow_routing_rules`, `…_rule_subjects`, `…_rule_orgs`, alle FORCE RLS). Eine Regel gilt für eine Menge von Untertypen (Ids des Katalogs der Dimension) und trägt nur eigene Orgs. Den Schalter „Nur diese Orgs entscheiden“ gibt es ausschließlich je Workflow (an der Standard-Regel), nicht je Regel. Ein Untertyp kommt pro Typ in höchstens einer Regel vor (Unique `(tenant_id, definition_id, subject_id)` plus Service-Check mit 400). Die Untertyp-Ids werden gegen den Mandantenkatalog der Dimension geprüft; es gibt keinen Fremdschlüssel, weil die Dimension variiert.

| Workflow | Dimension | Untertyp (serverseitig) | Status |
|----------|-----------|-------------------------|--------|
| `CERTIFICATION_RECORD` | `CERTIFICATION_DEFINITION` | `tenantCertDefinitionId` | aktiv |
| `TRAINING_COURSE_RECORD` | `TRAINING_COURSE_TYPE` | `tenantTrainingCourseTypeIds` (mehrere) | aktiv |
| `COURSE_OFFERING_REGISTRATION` | `TRAINING_COURSE_TYPE` | Angebot → Lehrgangstyp (aus der Entität, nicht aus dem Payload) | aktiv |
| `COURSE_REGISTRATION_CANCELLATION` | `TRAINING_COURSE_TYPE` | Anmeldung → Angebot → Lehrgangstyp | aktiv |
| `DRIVING_LICENSE_RECORD` | `DRIVING_LICENSE_DEFINITION` | Klassen im Antrag | nur im Enum vorbereitet, Handler nicht aktiv |
| `RANK_REQUEST` | `RANK` | `tenantRankId` | nur im Enum vorbereitet, Handler nicht aktiv |

Typen ohne Katalog (Name, Adresse, Mitgliedschaften, Gruppen, Onboarding, Dokument) sind nicht teilbar; `POST …/routing-rules` liefert dort 400.

Auflösung `WorkflowApproverRoutingService.resolve(typeId, subjectIds)`:

- Je Untertyp gilt seine Regel, sonst die Standard-Regel. Ohne Untertypen gilt die Standard-Regel.
- Die Orgs sind die Vereinigungsmenge der Standard-Orgs (für Untertypen ohne eigene Regel) und der Orgs jeder getroffenen Regel, ohne Duplikate.
- „Nur diese Orgs entscheiden“ ist der workflowweite Wert `TenantWorkflowDefinition.staticApproversExclusive` und wirkt nur, wenn die Vereinigungsmenge nicht leer ist.

API (alle `@PreAuthorize("@securityService.canAccessTenant(#tenantId)")`, Basis `/api/v1/auth/admin/tenants/{tenantId}/workflow-definitions`; `GET` auf der Basis (`@BoundedList`) liefert alle Tenant-Default-Definitionen in einem Request, alle weiteren Pfade hängen an `/{workflowTypeId}`). Die Regel-Endpunkte liefern einheitlich das `WorkflowRoutingRuleDto` (`id`, `subjectIds`, `staticApproverOrganizationIds`); die Regelliste steht zusätzlich als `routingRules` im `TenantWorkflowDefinitionDto`.

| Methode | Pfad | Antwort | Zweck |
|---------|------|---------|-------|
| `GET` | `/routing-rules/{ruleId}` | `200` `WorkflowRoutingRuleDto`, `404` wenn unbekannt oder zu einem anderen Typ gehörig | Regel lesen |
| `POST` | `/routing-rules` | `201` `WorkflowRoutingRuleDto` (mit neuer `id`) | Regel anlegen |
| `PUT` | `/routing-rules/{ruleId}` | `200` `WorkflowRoutingRuleDto` | Regel ändern |
| `DELETE` | `/routing-rules/{ruleId}` | `204` | Regel löschen |
| `POST` | `/restore` | `200` `TenantWorkflowDefinitionDto` | Standard-Regel auf die Baseline zurücksetzen **und alle Regeln löschen** |

Frontend (Admin, Mandant → Workflows): Die Liste führt nur zur Detailseite (`/admin/tenants/[id]/workflows/[workflowTypeId]`), ohne Zeilen-Aktionen. Dort bearbeitet der Stift die Standard-Regel inkl. Schalter „Nur diese Orgs entscheiden“; „Auf Standard zurücksetzen“ steht unten in der `ActionsSection` (nur bei angepassten Einstellungen). Jede Regel ist eine Zeile zur Regel-Detailseite (`…/rules/[ruleId]`): Stift bearbeitet Untertypen und Orgs, „Regel löschen“ steht unten in der `ActionsSection`; der workflowweite Schalter wird dort nur angezeigt. „Regel hinzufügen“ navigiert nach dem Anlegen zur neuen Regel.

Jede Änderung publiziert `WorkflowConfigurationChangedEvent.tenantWide`.

#### Sync offener Anträge

Die Slots offener Instanzen werden über `WorkflowService.syncApproverSlots` angeglichen (pending Slots ohne Bedarf entfallen mit `APPROVER_SLOT_REMOVED`, fehlende kommen hinzu), und zwar

- beim Reconcile nach Config-Änderung (`reconcileOpenInstancesAfterConfigurationChange`) und
- bei `updatePayload`: ändert sich der Untertyp (z. B. andere Zertifizierung), werden die Slots aus dem frisch gespeicherten Payload neu aufgelöst. Neue Auto-Approve-Slots werden verbucht, ggf. schließt die Instanz ab. Reihenfolge: Locks ersetzen, `onInputUpdated`, Event `INPUT_UPDATED`, erst dann Slot-Abgleich (und ein möglicher Abschluss mit `onApproved` und `APPROVED`), zuletzt `UPDATED`; der Handler sieht also den neuen Input vor einer Freigabe.

Wechselt die Rolle eines noch offenen (`PENDING`) Slots, übernimmt er Gruppe und Auto-Approve der neuen Rolle (dieselbe Logik wie `reconcileApproverParticipant`); entschiedene Slots behalten ihre Entscheidung, nur die Rolle wird nachgezogen.

Ändert sich dabei die Rolle eines bestehenden Slots (z. B. eine Org war `ADDITIONAL` und ist jetzt `CONTEXT`), wird `slot_role` am Slot nachgezogen; der Reconcile wendet danach Gruppe und Auto-/Self-Approve der neuen Rolle an.

**Backfill (temporär, nach Rollout entfernen; ohne bestehende Daten überflüssig):** Das Schema kennt `slot_role` an `wf_participants` und die Slot-Tabellen `std_workflow_type_mapping_template_slots` und `wf_type_mapping_slots` bereits in `V1`; Handler-Rollen (`NEW_MAIN_ORG`, `TARGET_ORG`, `HOME_ORG`) kommen über Baseline und Standard. `WorkflowParticipantSlotRoleBackfillRunner` ist ein reiner Rollout-Runner: Beim Start findet er pro Mandant (`TenantBatchRunner`) per Query nur offene Instanzen mit mehreren Org-Freigabe-Slots und noch ohne Rolle außer `CONTEXT` (nach dem Rollout also keine mehr) und setzt dort die echte Rolle der `AGR:`-Slots (`WorkflowService.backfillApproverSlotRoles(id)`, je Instanz eine eigene Transaktion; nicht auflösbare Instanzen werden geloggt und übersprungen). Ändert keine Entscheidungen oder Claims und löst keine Events aus. Grenze: Eine Instanz mit nur einem Slot, der eine zusätzliche Org (exklusiv) ist, behält `CONTEXT`.

Config-Change → `WorkflowConfigurationChangedEvent` → offene Instanzen reconcilen (pending Slots add/drop, Auto-Approve neu, ggf. complete).

## Controller

| Controller | Pfad | Rolle |
|------------|------|-------|
| Typspezifisch Self (`*WorkflowController`) | `/api/v1/auth/workflows/{kebab-type}` | **Nur** `POST` open (Self-Scope, z. B. `user-document-submission`) |
| Org-Open Typ 13 | `POST …/organizations/{orgId}/users/{userId}/workflows/organization-membership-end` | Org `USER_WRITE`: Austritt eines Mitglieds öffnen |
| Org-Open Typ 14 | `POST …/organizations/{orgId}/users/{userId}/workflows/organization-user-main-organization-change` | Org `USER_WRITE`: Haupt-Org-Wechsel eines Mitglieds öffnen |
| `WorkflowController` | `/api/v1/auth/workflows` | Generische Instanz-Ops (get, decide, claim, cancel, payload, input-change, promotions). **Erstellt nie.** |
| `WorkflowSelfController` | `/api/v1/self/workflows` | Paginierte Listen `GET …/opened`, `GET …/participations` (`ListQuery`, Filter `status` (mehrfach, Werte aus `WorkflowStatus`; Default je Liste im Frontend, ohne Wert keine Einschränkung)), `GET …/pending-count`, beginnbare Typen (`@BoundedList`), `GET …/open-state/{workflowTypeId}` (nur `openConcurrencyKeys` für Begin-/Edit-Dialoge; `typeBlocked` liefert ausschließlich der Katalog) |
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
| Definitionen / Mappings | `workflow/service/WorkflowDefinitionService.java`, `WorkflowTypeMappingService.java`, `workflow/domain/WorkflowTypeMappingSlot.java` |
| Freigabe-Rollen | `workflow/spi/WorkflowApproverSlotRoles.java`, `ApproverOrg.java`, `workflow/service/WorkflowParticipantSlotRoleBackfillRunner.java` |
| Standard-Domains | `workflow/standard/WorkflowTypeMappingStandardDomain.java`, `WorkflowDefinitionStandardDomain.java` |
| Konkrete Handler | `workflow/{namechange,addresschange,certificationrecord,trainingcourse,drivinglicense,organizationjoinrequest,mainorganizationchange,organizationleaverequest,organizationgroupleaverequest,organizationgroupjoinrequest,onboardingsubmission,organizationmembershipend,organizationusermainorganizationchange,userdocumentsubmission}/` (je Typ ein Package mit `*WorkflowHandler`) |
| Events | `workflow/event/WorkflowLifecycleEvent.java`, `WorkflowEventPublisher.java` |
| Todos / Notifications | `todo/adapter/workflow/`, `notification/adapter/workflow/` |
| Realtime | `workflow/realtime/{WorkflowRealtimeAdapter,WorkflowRealtimeNotifier,WorkflowRecipientResolver}.java` |

Siehe auch: [Dienstgrad und Funktion](rank-and-function.md) (`RANK_REQUEST`, Id 16).
