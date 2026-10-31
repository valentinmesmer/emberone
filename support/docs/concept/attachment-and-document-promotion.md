# Attachment & Dokument-Promotion

Design-Notizen für temporäre Entity-Anhänge vs. permanentes User-Dokumentenarchiv. Packages: `com.emberone.attachment`, `userdocument`, Orchestrierung in `workflow.service`, Storage in `platform.storage`. Verwandt: [User-Dokumente](userdocument.md), [Workflow-Engine](workflow-engine.md), [Aufbewahrung und Löschung](retention-and-erasure.md).

## Mental Model

```
Draft-Upload (Workflows-Bucket)
        │ relink
        ▼
Workflow-/Comment-Attachment
        │ promote (bei Approval, Auswahl)
        ▼
UserDocument (User-Documents-Bucket)
```

Nach Promotion bleibt das Workflow-Attachment in der Regel als Original; Ausnahme Zertifikatsnachweis (`CERTIFICATION_RECORD`): `onDocumentsPromoted` löscht die Prozess-Kopien im Workflows-Bucket (`deleteCertificationProofAttachments`).

| Konzept | Owns | Does not own |
|---------|------|--------------|
| **EntityAttachment** | Metadaten + `storageKey` an Entities | User-Archiv, Promotion-Auswahl |
| **UserDocument** | Permanente persönliche Dokumente | Workflow-Status |
| **WorkflowDocumentService** | Draft→Workflow-/Comment-Relink | Copy nach User-Docs |
| **WorkflowDocumentPromotionService** | Preview, Pending-Selections, Bucket-Copy + `UserDocument`-Anlage | Attachment-Upload selbst |

## Attachment-Typen

`EntityAttachmentType`: `APPOINTMENT`, `WORKFLOW_DRAFT`, `WORKFLOW`, `WORKFLOW_COMMENT_DRAFT`, `WORKFLOW_COMMENT`.

## Lifecycle (Workflow)

1. **Draft-Upload** - Type `WORKFLOW_DRAFT`, Key `tenants/{t}/workflows/drafts/users/{u}/…`
2. **An Workflow binden** - bei Open: `relink` → `WORKFLOW`, Key `tenants/{t}/workflows/{id}/…`
3. **Comment-Attachments** - `WORKFLOW_COMMENT_DRAFT` → nach Create `WORKFLOW_COMMENT`
4. **Promotion-Auswahl** - Preview + `PUT …/document-promotions` → `Workflow.pendingDocumentPromotions`
5. **Bei Approval** - `promoteToUserDocuments`: Copy Workflows-Bucket → User-Documents-Bucket → neues `UserDocument` (Owner = `openedByUserId`) → `handler.onDocumentsPromoted`. Generisch bleibt das Workflow-Attachment; bei Zertifikatsnachweisen verknüpft der Handler den Record und löscht die Prozess-Kopien im Workflows-Bucket. Löschfrist für Record und Scan: [Aufbewahrung und Löschung](retention-and-erasure.md) (`EVIDENCE`).

**Appointment:** eigener Pfad (`attachments-bucket`, Type `APPOINTMENT`) - **ohne** Promotion zu UserDocuments. Bei Termin-Delete: sync `AppointmentDeletingEvent` → Attachment- + Object-Storage-Cleanup (polymorph, kein FK).

**Onboarding:** Drafts im `attachments-bucket` unter `…/onboarding-invitations/{id}/…` (kein `EntityAttachment`). Entfernen aus dem Step-Payload löscht die S3-Datei (`deleteStoredAttachment`); beim Speichern werden verwaiste Record-/Führerschein-Anhänge bereinigt. Bei Promote: `OnboardingAttachmentPromotionService` → `UserDocumentPromotionService` → User-Documents-Bucket inkl. Zertifikatsnachweise; danach `OnboardingPromotionService` löscht die Draft-Dateien im `attachments`-Bucket (Step-Payload bleibt). App-Link `ONBOARDING_INVITATION` bleibt neben Record-Links; `ONBOARDING`-Retention entfernt nur Einladungs-Links, solange andere behaltende Links existieren; signierte Eintrittsdokumente (referenziert von `usr_entry_document_fulfillments`) bleiben ebenfalls in der persönlichen Akte. Details: [Onboarding](onboarding.md).

**Verwaiste Workflow-Drafts:** `WORKFLOW_DRAFT` / `WORKFLOW_COMMENT_DRAFT`, die nie an einen Workflow/Comment gebunden wurden, werden per Scheduler (`EntityAttachmentDraftCleanupScheduler`, Default 12 Stunden) aus DB + Workflows-Bucket entfernt.

## Buckets

| Config | Nutzung |
|--------|---------|
| `object-storage.workflows-bucket` | Workflow-/Draft-/Comment-Attachments |
| `object-storage.user-documents-bucket` | Permanente UserDocuments |
| `object-storage.attachments-bucket` | Appointment-Attachments; Onboarding-Drafts und Eintrittsdokument-Vorlagen |

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Attachment-Entity | `attachment/domain/EntityAttachment.java` |
| Generische Ops | `attachment/service/EntityAttachmentService.java` |
| Workflow-Docs | `workflow/service/WorkflowDocumentService.java` |
| Promotion | `workflow/service/WorkflowDocumentPromotionService.java` |
| User-Archiv | `userdocument/` → [userdocument.md](userdocument.md) |
| S3 | `platform/storage/service/ObjectStorageService.java` |
| Storage ↔ DB-Transaktion | `platform/storage/service/ObjectStorageTransactionCoordinator.java` (Upload/Copy mit Rollback-Cleanup; Delete/Move nach Commit). **Regel:** Schreibzugriffe auf S3 nur hier oder in `@AfterCommitEventListener` ohne weitere DB-Arbeit; Downloads direkt über `ObjectStorageService`. |
| Appointment-Docs | `appointment/…/AppointmentDocumentService` |
| Appointment-Delete-Cleanup | `appointment/…/AppointmentDeletionAttachmentCleanupListener` |
| Onboarding-Promote | `onboarding/…/OnboardingAttachmentPromotionService`, `userdocument/…/UserDocumentPromotionService` |
| Draft-Cleanup | `attachment/…/EntityAttachmentDraftCleanupService`, `EntityAttachmentDraftCleanupScheduler` |
