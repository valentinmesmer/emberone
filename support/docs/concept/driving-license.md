# Führerschein (Driving License)

Design-Notizen für Mandanten-Katalog und User-Führerschein-Dokument mit Klassen. Package: `com.emberone.drivinglicense`. Qualifikation kann Führerscheinklassen als dritte Voraussetzungsgruppe nutzen, siehe [Qualifikation](qualification.md). Standard-Filter: [Standard](standard.md).

## Mental Model

```
TenantDrivingLicenseDefinition (config_key, z.B. "CE")
        ↑ classes[]
UserDrivingLicense (ein Dokument pro User)
  ├── licenseNumber, issuingAuthority, issueDate, documentExpirationDate
  └── UserDrivingLicenseClass[] (grantDate / expirationDate, beide nullable)
        │
        └──────────► Qualification (Prerequisites via config_key, ALL|ANY)
```

**Ein Führerschein pro User** (`UNIQUE tenant_id, user_id`). Klassen hängen am Dokument, nicht als separate Nachweise. Kartenerneuerung = Update in place, keine Kartengeschichte.

Wiederkehrende **Führerschein-Kontrollen** laufen über das [Zertifizierungs-Modul](trainingcourse-and-certification.md): eine `TenantCertDefinition` **`FSK`** (Bayern-Baseline), Intervall 365 Tage. Der Workflow `CERTIFICATION_RECORD` bestätigt nur, dass die erfassten Stammdaten noch stimmen, **keine Führerschein-Felder im Payload**.

Qualifikation trennt zwei konfigurierbare Komponenten:

| Komponente | Wo konfiguriert | Wann erfüllt |
|------------|-----------------|--------------|
| **FSK gültig** | `certDefinitions[]` | Zertifizierungsstatus `VALID` |
| **Klasse gültig** | `drivingLicenses[]` | User hat die Klasse **und** Dokument nicht abgelaufen **und** Klasse nicht abgelaufen |

## Was es besitzt

- Mandanten-Katalog Führerscheinklassen (`TenantDrivingLicenseDefinition`)
- User-Führerschein als **Dokument** mit n **Klassen**
- Felder: `issueDate`, `documentExpirationDate`, `licenseNumber`, `issuingAuthority`, `notes`, Dokumente
- Workflow `DRIVING_LICENSE_RECORD` (Self-Service Create/Update)
- Permissions: `DRIVING_LICENSE_READ` (32), `DRIVING_LICENSE_WRITE` (33)

## Was es nicht besitzt

| Thema | Hinweis |
|-------|---------|
| Hartkodiertes Klassen-Enum | Katalog ist mandantenspezifisch, geseedet über Federal-State-Baseline |
| Flächendeckende BayFwG-Verknüpfung Qualifikation↔Klasse | Bayern-Baseline seedet nur Maschinist (`config_key`s `MA` + `FSK` + `C`, Org-Decision; Java: `TC_MA` / `CERT_DEF_FSK` / `DL_C`); weitere Rollen konfiguriert der Admin manuell |
| Eigene „Kontrolle“-Entity | Über Zertifizierung (`config_key` `FSK` in Bayern-Baseline) |
| Org-Materialisierung des Katalogs | Nur Standard-**Selections** zur Read-Time |
| Submission-Modell / Kartengeschichte | Greenfield: kein `UserDrivingLicenseSubmission` |

## Key Flows

1. **Katalog:** Admin pflegt `TenantDrivingLicenseDefinition` → Standard selektiert `config_key`s → Org-Katalog via `DrivingLicenseCatalogService` + `EffectiveStandardResolver`. Hard-Delete publiziert `TenantDrivingLicenseDefinitionDeletedEvent` → Qualification räumt Prerequisite-Links.
2. **Erfassung (Self-Service):** User öffnet Workflow `DRIVING_LICENSE_RECORD` → Genehmigung → Document-Promotion → `UserDrivingLicense` upsert (Create ohne Führerschein, Update sonst) → `DrivingLicenseRecordChangedEvent`
3. **Erfassung (HR):** Org-Controller mit `DRIVING_LICENSE_WRITE` legt/ändert Führerschein manuell an
4. **Erfassung (Onboarding):** Promote → `upsertFromOnboarding` + App-Link `ONBOARDING_INVITATION`
5. **Qualifikation:** `QualificationStatusService` prüft Klassen nur bei gültigem Dokument **und** nicht abgelaufener Klasse (`ALL` / `ANY`)
6. **Klassen entfernen:** Org-Admin entfernt Klasse im Bearbeiten-Dialog → Zeile in `driving_license_classes` wird gelöscht (kein Soft-Delete); Qualifikation berücksichtigt nur noch vorhandene Klassen

### Workflow-Payload

`DrivingLicenseRecordWorkflowPayload`:

| Feld | Pflicht | Beschreibung |
|------|---------|--------------|
| `classes[]` | ja | Definition-ID; `grantDate` und `expirationDate` pro Klasse (**nullable**) |
| `issueDate` | ja | Ausstellungsdatum (Feld 4a) |
| `documentExpirationDate` | nein | Ablaufdatum der Karte (Feld 4b) |
| `licenseNumber` | ja | Führerscheinnummer (Feld 5) |
| `issuingAuthority` | ja | Ausstellende Behörde (Feld 4c) |
| `notes` | nein | Freitext |
| `attachmentIds` | ja (Workflow) | Nachweisdokument(e) |

Guard: **ein offener Antrag pro User** (`CODE_TYPE_ALREADY_OPEN`). Ohne bestehenden Führerschein → Create, sonst → Update. Klassen-Sync: `syncActiveClasses` **ersetzt** die Klassenmenge durch das Payload-Set (Klassen außerhalb des Payloads werden entfernt; keine Merge-Keep-Semantik).

## Bayern-Baseline (Platzhalter)

Geseedet über `BayernTenantBaseline` (nicht rechtlich verifiziert, vor Go-Live mit Fachexperte prüfen):

| Inhalt | Werte |
|--------|-------|
| Führerscheinklassen | B, BE, C1, C1E, C, CE |
| Führerschein-Kontrolle | **eine** Definition `config_key` `FSK` (Java `CERT_DEF_FSK`), Intervall 365 Tage, Reminder 30 Tage |
| Workflow | `DRIVING_LICENSE_RECORD` |
| Qualifikation Maschinist | `config_key`s `MA` + `FSK` + `C` (Java `TC_MA` / `CERT_DEF_FSK` / `DL_C`), `requiresOrgDecision=true` |

## Datenbank

Schema in `V1__Initial_setup.sql`:

| Tabelle | Zweck |
|---------|-------|
| `tenant_driving_license_definitions` | Mandanten-Katalog |
| `std_driving_license_selections` | Standard → `definition_config_key` |
| `driving_licenses` | User-Führerschein (1 pro User) |
| `driving_license_classes` | Klassen am Führerschein |
| `driving_license_documents` | M:N Führerschein ↔ Dokument |
| `tenant_qualification_driving_licenses` | Qualifikations-Voraussetzung per Config Key |

`tenant_qualification_definitions.driving_license_requirement_mode` (`ALL` \| `ANY`, Default `ALL`).

## API-Endpunkte

| Bereich | Pfad |
|---------|------|
| Self (lesen) | `GET /api/v1/self/driving-license` |
| Self (Katalog) | `GET /api/v1/self/driving-license/available-definitions` (`@BoundedList`, Membership-Org-Katalog) |
| Org (CRUD) | `GET/POST/PUT/DELETE /api/v1/auth/organizations/{orgId}/users/{userId}/driving-license` |
| Org (pending) | `GET .../driving-license/pending-workflow` |
| Org (Katalog) | `GET .../driving-license/available-definitions` (`@BoundedList`) |
| Workflow (Self) | `POST /api/v1/auth/workflows/driving-license-record` |
| Admin-Katalog | `/api/v1/auth/admin/tenants/{tenantId}/driving-license-definitions` |
| Standard-Selektion | `PUT .../standards/{standardId}/driving-license-selections` |

## Frontend

| Bereich | Ort |
|---------|-----|
| Mein Bereich | `/my-area/driving-licenses` |
| Org User-Management | `UserDrivingLicensesSection` |
| Detail-Ansicht | `DrivingLicenseView`, `DrivingLicensePendingWorkflowCard` |
| Admin-Katalog | `AdminTenantDrivingLicenseManagement` |
| Workflow-Anzeige | `lib/workflows/inputDisplay/plugins/drivingLicenseRecord.tsx` |

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Domain / Service | `drivinglicense/domain/`, `DrivingLicenseRecordService` |
| Workflow-Handler | `workflow/drivinglicense/` |
| Admin-API | `admin/drivinglicense/` |
| Standard-Domain (`@Order(53)`) | `drivinglicense/standard/DrivingLicenseStandardDomain` |
| Qualifikations-Join | `qualification/tenant/TenantQualificationDrivingLicense` |
| Event → Sync | `DrivingLicenseRecordChangedEvent` → `QualificationStatusSyncListener` |
| Katalog-Hard-Delete | `TenantDrivingLicenseDefinitionDeletedEvent` |

Siehe auch: [Federal state](federal-state.md), [Sicherheit](security-and-permissions.md), [Workflow-Engine](workflow-engine.md).
