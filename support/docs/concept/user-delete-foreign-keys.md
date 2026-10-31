# User-Löschung: Fremdschlüssel

Design-Notizen zu allen User-FKs in `V1__Initial_setup.sql` - CREATE-Zeit und der Block nachgezogener User-FKs. Postgres 18: `ON DELETE SET NULL (spalte)` bei zusammengesetzten User-FKs, damit `tenant_id` erhalten bleibt.

## Mental Model

```
DELETE users
        │
        ├── CASCADE  → Personendaten der Person verschwinden
        └── SET NULL → Akteur-Spalten werden NULL („Ehemaliges Mitglied“)
```

| Regel | Bedeutung |
|-------|-----------|
| **CASCADE** | Daten der Person (Stammdaten, Mitgliedschaften, Dokumente, Nachweise, Benachrichtigungen, …) |
| **SET NULL** | Akteur-Referenz (`leader_id`, `uploaded_by`, `opened_by_user_id`, …); UI: „Ehemaliges Mitglied“ |
| **Tenant CASCADE** | Alle `REFERENCES tenants(id) ON DELETE CASCADE` (Grundlage Tenant-Purge) |

Kein FK: `action_log.user_id` (nur Index) - Audit-Zeilen überleben den User-Delete und laufen mit `AUDIT` ab.

## Scope

Zwei Stellen in `V1`:

1. **CREATE-Zeit** - bei Tabellenanlage, z. B. Kontakt, Notfallkontakte, Abwesenheiten, Zeiteinträge, Termin-Responses/-Reads, Onboarding-Einladung (`user_id`), Eintrittsdokument-Fulfillments (`user_id`).
2. **Nachgezogene User-FKs** (Kommentar vor `action_log`) - nachgezogene User-FKs für Mitgliedschaften, Events, Dokumente, Workflows, Notifications, Todos, Zertifikate, Lehrgänge, Führerscheine, Qualifikation, Onboarding-Akteure.

Vollständige Wahrheit: die Constraint-Definitionen in `V1`, nicht nur der Block-Kommentar.

## CASCADE (Auswahl)

**CREATE-Zeit (u. a.):** `user_contact_information`, `user_emergency_contacts`, `user_absences`, `user_time_entries`, `appointment_responses`, `appointment_reads`, `onb_invitations` (Person), `usr_entry_document_fulfillments` (Person).

**Nachgezogen (u. a.):** `user_organization_*_memberships`, `event_attendances`, `doc_documents`, `doc_user_profile_images`, `idm_users`, `ntf_notifications`, `ntf_user_channel_overrides`, `cert_user_*`, `training_course_user_submissions`, `course_registrations` (Person; `final_decided_by_user_id` SET NULL), `driving_licenses`, `qual_user_org_decisions` (Person), `todo_excluded_users`, `cert_event_grant_recipients`.

## SET NULL (Akteur)

`org_management_activations.user_id` (Protokoll bleibt bis `MANAGEMENT_ACCESS` ablaufen lässt), `service_events.leader_id`, `operation_events.leader_id`, `doc_entity_attachments.uploaded_by`, `wf_instances.opened_by_user_id`, `wf_participants.claimed_by_user_id`, `wf_decisions.user_id`, `wf_comments.user_id`, `wf_input_changes.requested_by_user_id`, `todo_items.assignee_user_id`, `todo_items.completed_by_user_id`, `cert_event_grants.created_by_user_id`, `qual_user_org_decisions.decided_by_user_id`, `course_registrations.ended_by_user_id`, `course_registrations.workflow_id` (Workflow-Retention), `onb_invitations.invited_by_user_id`, `onb_document_definitions.template_uploaded_by_user_id`, `onb_org_document_definitions.template_uploaded_by_user_id`.

Plattform-Admin (`userId` `0`) hat keine `users`-Zeile. Tabellen ohne User-FK (z. B. `tenants`) speichern den Akteur nur im `action_log` (`app.user_id` darf `0` sein). Nullable Akteur-FKs schreiben `TokenService.getReferencedUserId()` (`null` statt `0`): Einladung `invited_by`, Qualifikationsentscheidung `decided_by`, Nachweis-Grant `created_by`, Eintrittsdokument-Vorlage `template_uploaded_by`, Anhang `uploaded_by`.

## Weitere FKs

- `usr_entry_document_fulfillments.user_document_id` → `doc_documents`: `ON DELETE SET NULL (user_document_id)`
- `user_time_entries`: `(tenant_id, user_id)` → `users`, `(tenant_id, organization_id)` → `organizations`
- `wf_instances.opened_at`: Startzeitpunkt für `WORKFLOW_STALE`

Siehe auch: [Aufbewahrung und Löschung](retention-and-erasure.md) (`USER_MASTER` und Cascade-Verhalten), [Dienstgrad und Funktion](rank-and-function.md) (`user_ranks` CASCADE).
