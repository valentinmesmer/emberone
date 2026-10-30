# Todo-Modul

Design-Notizen für die Aufgaben-Inbox - kein eigener Business-Prozess. Package root: `com.emberone.todo`. Verwandt: [Workflow-Engine](workflow-engine.md), Zertifizierungs-Reminder in [Training & Zertifizierung](trainingcourse-and-certification.md), [Geplante Jobs](scheduled-jobs.md), Listen-Vertrag [Paginierung von Listen](pagination.md).

## Mental Model

| Konzept | Rolle |
|---------|-------|
| **Todo** | Persistierte Inbox-Zeile: Wer, Link zur Quelle (kein Due-/Remind-Feld) |
| **TodoExcludedUser** | Sichtbarkeits-Ausnahme trotz Assignee-Gruppe (Inbox, Detail, Realtime/Notify) |
| **Workflow** | Eigentümer des Genehmigungsprozesses; Todos spiegeln Slot-/Input-Zustand |
| **Event** (`com.emberone.event`) | Dienst/Einsatz - **orthogonal**, erzeugt keine Todos → [event.md](event.md) |
| **TodoCreatedEvent** | After-Commit-Signal (Realtime/Notifications), kein Kalender-Event |
| **TodoVisibilityGainedEvent** | Nutzer erhält neu Sicht auf offenes Gruppen-Todo (Membership-Join / Assignee-Gruppen-Remap) |

```
WorkflowLifecycle / CertificationReminder / Group-Membership
        │
        ▼
TodoService.open / finalize / reopen  ·  TodoAssigneeVisibilityListener
        │
        ▼
todo_items (+ todo_excluded_users)  ──► Self-API, Realtime, Notifications
```

## Was es besitzt

- Status: `OPEN` / `COMPLETED` / `CANCELLED`
- Assignees: User und/oder Org-Gruppen (`TodoAssigneeGroup`)
- Exclusions: `TodoExcludedUser` (`todo_excluded_users`) - gesetzt über `TodoOpenRequest.excludedUserIds`
- Typ (`TodoType`), Params (JSON), `AppLink`, Source-Referenz (`WORKFLOW` | `CERTIFICATION_RECORD`)
- Inbox-Archiv (`archived_at` ohne `archived`-Boolean - siehe [Lifecycle & Statusfelder](lifecycle-and-status-fields.md)); **kein** Due-/Remind-Feld am Todo
- Self-API unter `/api/v1/self/todos`: `GET /` paginiert (`ListQuery` + `status=OPEN|DONE`, Default nur Offen; Sortierung je Status per Sort-Override wenn kein Client-`sort`: ein Status → `OPEN` `createdAt` absteigend, `DONE` `archivedAt` absteigend; kein Client-`sort` in der UI) - siehe [Paginierung von Listen](pagination.md); `GET /{id}`, `GET /open-count`, `POST /{id}/complete` - Sichtbarkeit via `TodoListSpecs` (Assignee/Gruppe, Exclusions `NOT EXISTS` auf offenen Listen)
- Visibility-Sync: Membership-Aktivierung und Assignee-Gruppenwechsel → `TodoVisibilityGainedEvent` (Notify/Realtime nur an neu berechtigte Nutzer; Exclusions greifen auch im Recipient-Resolver)

**TodoTypes:** `WORKFLOW_START_REQUESTED` (Gruppen-Claim), `WORKFLOW_APPROVAL_REQUIRED`, `WORKFLOW_INPUT_CHANGE_REQUIRED`, `CERTIFICATION_EXPIRING`.

## Was es nicht besitzt

| Thema | Woanders |
|-------|----------|
| Workflow-Lifecycle, Entscheidungen, Slots | Workflow-Engine |
| Certification-Records / Status | Certification-Domain |
| Zertifizierungs-Erinnerungen (Wann öffnen) | `CertificationReminderService` / `CertificationReminderScheduler` |
| Kalender-Termine / Attendance | `appointment` / `event` |
| Dedup-Regeln der Quelle | Adapter (`WorkflowTodoAdapter`) bzw. `CertificationReminderService` |
| Gruppen-Membership anlegen/ändern | [user-memberships.md](user-memberships.md) |

## Lifecycle

1. **Open** - `TodoService.open`: Dedup über Source + Type + `participantId` in Params (fehlende Keys ⇒ kein Treffer, neues Todo möglich); bereits offenes Todo wird unverändert zurückgegeben; Status `OPEN`; Exclusions via `replaceExcludedUsers`
2. **Finalize** - öffentliche Finalize-Methoden setzen `COMPLETED` / `CANCELLED` und nutzen intern `archive()` (private)
3. **Reopen** - Workflow-Adapter nutzt typspezifische Reopen-Methoden (`reopenGroupClaimTodo`, `reopenApprovalTodo`, `reopenInputChangeTodo`); Exclusions werden bei Reopen erneut gesetzt
4. **Manuell complete** - Self-API, wenn Type `allowsManualComplete()` (aktuell alle Types)
5. **Visibility gained** - Nutzer tritt Approver-Gruppe bei oder Assignee-Gruppe am offenen Todo wechselt → gezieltes Notify/Realtime (kein neues Todo); ausgeschlossene User bleiben außen vor

**Exclusion-Fall:** Gruppen-Claim (`WORKFLOW_START_REQUESTED`) bei blockiertem Self-Approve → Opener (`openedByUserId`) landet in `excludedUserIds`, damit er das Gruppen-Todo nicht sieht/bekommt (siehe [Workflow-Engine](workflow-engine.md) / `selfApproveAllowed`). Die meisten systemgetriebenen Todos haben keine Exclusions.

Treiber: `WorkflowTodoAdapter` (Lifecycle-Events), `CertificationReminderService` / Scheduler, `TodoAssigneeVisibilityListener` (Membership / Gruppen-Remap).

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Entity | `todo/domain/Todo.java`, `todo/domain/TodoExcludedUser.java` |
| Schreib-API | `todo/service/TodoService.java` (`TodoOpenRequest`) |
| Self-API | `todo/controller/TodoSelfController.java` |
| Listen-Query | `todo/list/TodoListDefinition.java`, `TodoListFilter.java`, `TodoListSpecs.java` |
| Frontend-Liste | `frontend/lib/list/todoList.ts`, `components/todo/TodoListView.tsx` |
| Visibility-Queries | `todo/repository/TodoRepository.java` |
| Recipient / Realtime | `todo/realtime/TodoRecipientResolver.java` |
| Workflow-Adapter | `todo/adapter/workflow/WorkflowTodoAdapter.java` |
| Visibility-Sync | `todo/assignee/TodoAssigneeVisibilityListener.java` |
| Typ-Registry | `todo/type/TodoTypeDefinitionRegistry.java` |
