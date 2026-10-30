# User-Dokumente

Design-Notizen für das persönliche Dokumentenarchiv (S3) mit Tags, Unread und optionalen Deep-Links. Package root: `com.emberone.userdocument`. Verwandt: [Attachment & Dokument-Promotion](attachment-and-document-promotion.md) (Draft→Archiv), [Workflow-Engine](workflow-engine.md), [Aufbewahrung und Löschung](retention-and-erasure.md).

## Mental Model

| Konzept | Rolle |
|---------|-------|
| **UserDocument** | Permanentes Dokument im User-Bucket |
| **EntityAttachment** | Temporärer/entity-gebundener Anhang - **nicht** dieses Modul |
| **UserDocumentLink** | Navigation zu einem behaltenden Datensatz (`AppLinkType` + `linkId`) - keine File-Kopie |
| **Promotion** | Workflow und Onboarding schreiben ins User-Archiv; dieses Modul besitzt das Ziel |

**Document-Link-Typen** (`DocumentLinkDeletionCoverage.DOCUMENT_LINK_TYPES`): `WORKFLOW`, `CERTIFICATION_RECORD`, `TRAINING_COURSE_RECORD`, `DRIVING_LICENSE_RECORD`, `ONBOARDING_INVITATION`. Löschung ist **bedingt** über `UserDocumentService#deleteDocumentsForLinkTargets` (Details: [Aufbewahrung und Löschung](retention-and-erasure.md)):

- **Record-Links** (`TRAINING_COURSE_RECORD`, `DRIVING_LICENSE_RECORD`; `CERTIFICATION_RECORD` siehe unten): Dokumente, die nur noch dieses Ziel beanspruchen; andere Record-Links schützen.
- **`WORKFLOW` / `ONBOARDING_INVITATION`:** Dokumente mit anderem behaltenden Link bleiben; nur der gelöschte Link wird entfernt.
- **Eintrittsdokumente:** Referenzen in `usr_entry_document_fulfillments` (und ggf. `usr_entry_document_fulfillment_events`) schützen das Dokument vor Löschung hier. Der rechtliche Nachweis (Version, Hash, Kanal, Bestätigungstext, Historie) liegt in den Fulfillment-Tabellen, nicht im `UserDocument`-Metadatum - siehe [Onboarding](onboarding.md#mandanten-katalog-eintrittsdokumente).
- **`CERTIFICATION_RECORD`:** nicht über `deleteDocumentsForLinkTargets` (`link_id` = Definitions-ID); Aufrufer nutzen `deleteDocumentsByIds` über die Junction.

`CERTIFICATION_EVENT_GRANT` steht in `NON_DOCUMENT_LINK_TYPES` (App-Link auf Zertifizierungs-Records, nicht auf Dokumenten) und erscheint **nicht** in `doc_document_links`.

```
Upload / Workflow-Promotion / Onboarding-Promote
        │
        ▼
UserDocument (+ Tags, Links, viewedAt, createdAt)
        │
        ▼
ObjectStorageService (user-documents-bucket)
```

## Was es besitzt

- `UserDocument`, `UserDocumentTag`, `UserDocumentLink` (Tabelle `doc_documents`)
- Max 10MB je Dokument
- `UserDocumentLinkResolver` - löst Navigationspfade für Dokument-Links auf (System-Links werden vertrauenswürdig über `UserDocumentSystemWriter.applyTrustedLinks` gesetzt)
- Self-API: List/Download + Unread-Count/mark-viewed - **kein** Upload/Update/Delete (Self-Einreichung nur per Workflow `USER_DOCUMENT_SUBMISSION`, Typ 15)
- Org-API (`DOCUMENT_READ` / `DOCUMENT_WRITE`): Upload/Update/Meta - **kein** Delete (Archiv ist append-only)

| Controller | Prefix |
|------------|--------|
| `UserDocumentSelfController` | `/api/v1/self/documents` (paginiert: `ListQuery` + Filter `tag`, `viewed`, `contentType`; Sortierung per `sort` mit `createdAt` (Hochgeladen, Standard `createdAt,desc`), `title`; `GET …/list-filter-options` für Tag- und Dateityp-Optionen; Legacy `GET …/tags` mit `@BoundedList` für bounded Lookups) |
| `UserDocumentOrganizationController` | `/api/v1/auth/organizations/{organizationId}/users/{userId}/documents` (gleicher Listen-Vertrag) |

Dokumentlisten (`GET` ohne Id): `page`/`size` (max. 200), Antwort `PageDto`.

## Was es nicht besitzt

| Thema | Woanders |
|-------|----------|
| Workflow-Draft-/Entity-Attachments | `attachment` |
| Document-Promotion-Regeln | Workflow / Attachment-Promotion |
| Profilbilder | `profileimage` |

## Invarianten & Fallstricke

1. Storage-Key: `tenants/{tid}/users/{uid}/documents/{uuid}` plus optional Dateiendung (z. B. `.pdf`).
2. Links sind Navigation, keine Duplikate der Quelldatei.
3. User-Dokumente sind **nicht löschbar** über die API (Löschung nur über behaltenden Datensatz / Retention, bedingt wie oben).

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Entity | `userdocument/domain/UserDocument.java` |
| Service | `userdocument/service/UserDocumentService.java` |
| Onboarding-Promote | `userdocument/service/UserDocumentPromotionService.java` |
| Link-Navigation | `userdocument/service/UserDocumentLinkResolver.java` |
| Link-Typen / Lösch-Abdeckung | `userdocument/DocumentLinkDeletionCoverage.java` |
| Self / Org | `userdocument/controller/UserDocumentSelfController.java`, `UserDocumentOrganizationController.java` |
| S3 | `platform/storage/service/ObjectStorageService.java` |
