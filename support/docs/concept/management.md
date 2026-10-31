# Management-Modul (Geschäftsstelle)

Design-Notizen für das Management-Modul. Eine Organisation mit dem Modul `MANAGEMENT` darf sich in definierte andere Organisationen desselben Mandanten öffnen. Packages: `com.emberone.management`, `com.emberone.admin.management`.

Status: Konfiguration, Aktivierung, Zugriffsauswertung, Frontend-Flow und Aktivierungsprotokoll sind umgesetzt.

## Konfigurationsebenen

| Ebene | Wo | Was | Erzeugt Umfang? |
|-------|----|-----|-----------------|
| Standard | Standard-Admin | Modul `MANAGEMENT` an/aus, `MANAGEMENT_ACCESS` in einer PermissionGroup, Mapping einer Organisationsgruppe auf diese Gruppe | **Nie** |
| Organisation | Admin-Org-Fenster (Tenant-Admin), Abschnitt „Verwaltung“ | Audience-Einträge: Umfang (`ORG`, `ORG_SUBTREE`, `ORG_TYPE`, `TENANT`), erlaubte Berechtigungen (Enum-Typen, standardmäßig alle, abwählbar), Pflicht-Höchstdauer | Ja, nur durch diese Einträge |

Die Fähigkeit („darf verwalten“) ist `ManagementCapabilityService.isCapable`: Modul aktiv für den effektiven Standard **und** eine aktive Organisationsgruppe ist auf eine Berechtigungsgruppe gemappt, die `MANAGEMENT_ACCESS` enthält. Ohne Fähigkeit lässt sich keine Audience speichern.

## Datenmodell

Tabelle `org_management_audiences` (in `V1__Initial_setup.sql`, Greenfield):

| Spalte | Bedeutung |
|--------|-----------|
| `tenant_id`, `managing_organization_id` | Mandant und verwaltende Organisation (M) |
| `kind` | `ORG`, `ORG_SUBTREE`, `ORG_TYPE` (brauchen `ref_id`) oder `TENANT` (ohne `ref_id`) |
| `ref_id` | Organisation oder Organisationsart der Ziel-Seite |
| `permission_types` | Erlaubte `PermissionType`-Namen, kommagetrennt, mindestens einer, nie `MANAGEMENT_ACCESS` |
| `max_duration_minutes` | Obergrenze je Aktivierung (> 0) |

- Eindeutig je (Mandant, M, kind, ref_id); FK auf `organizations` mandantensicher.
- `ENABLE` und `FORCE` RLS mit Policy auf `app.tenant_id`, `trg_action_log`.

Tabelle `org_management_activations` (ebenfalls in `V1__Initial_setup.sql`):

| Spalte | Bedeutung |
|--------|-----------|
| `tenant_id`, `user_id` | Mandant und Person; `user_id` wird bei Löschung der Person auf NULL gesetzt (`ON DELETE SET NULL (user_id)`) |
| `managing_organization_id`, `target_organization_id` | Verwaltende Organisation (M) und Zielorganisation (T) |
| `activated_at`, `valid_until` | Beginn und hartes Ende; `valid_until > activated_at` |
| `ended_at`, `ended_by` | Vorzeitiges Ende; `ended_by` ∈ `USER`, `SYSTEM` (beides gesetzt oder beides NULL) |
| `permission_types` | Umfang als kommagetrennte `PermissionType`-Namen (Teilmenge des Umfangs der passenden Regel) |
| `reason` | Anlass, 1 bis 500 Zeichen, unveränderlich nach dem Anlegen |

- Höchstens eine **offene** Zeile je (Mandant, Person, Zielorganisation): partieller Unique-Index `WHERE ended_at IS NULL`. Eine Neuaktivierung beendet die alte Zeile (`USER`) und legt eine neue an.
- Beendete Zeilen bleiben als Protokoll, bis die Löschklasse `MANAGEMENT_ACCESS` sie entfernt (12 Monate nach `COALESCE(ended_at, valid_until)`).
- `ENABLE` und `FORCE` RLS mit Policy auf `app.tenant_id`, `trg_action_log`.

## Aktivierung (Schreibpfad)

`ManagementActivationService.activate`. Eine Aktivierung wird nur angelegt, wenn alle Regeln gelten:

- Die Person ist aktives Mitglied von M und hat `MANAGEMENT_ACCESS` **direkt** in M (Berechtigungskarte, kein Admin-Wildcard, kein Bypass). M ist verwaltungsfähig (`isCapable`). Sonst `AccessDeniedException` (403).
- M ≠ T; beide aktiv und im Mandanten. (Selbst-Ziel ist bewusst ausgeschlossen.)
- Es gibt **einen** Audience-Eintrag von M, der T abdeckt (`ORG`, `ORG_SUBTREE`, `ORG_TYPE` über den gemeinsamen Resolver, oder `TENANT`), und für diesen Eintrag gilt: Dauer ≤ `max_duration_minutes` und die gewählten Berechtigungen ⊆ erlaubte Typen des Eintrags ∩ Module von T ∩ ohne `MANAGEMENT_ACCESS`.
- Die Person wählt die Berechtigungen selbst aus den Rechten der Zielorganisation. Standardmäßig ist nichts gewählt; die Auswahl muss mindestens einen Eintrag haben. Anlass getrimmt 1 bis 500 Zeichen.
- Dauer wird als `valid_until = activated_at + Dauer` festgeschrieben. `activated_at` ist auf die volle Minute abgeschnitten, Dauer nur in ganzen Minuten; Zeitstempel haben keine Sekunden.
- Ablauf-Event: `ManagementActivationExpiryScheduler` (alle 15 s, ShedLock, pro Mandant) sucht offene Aktivierungen mit abgelaufenem `valid_until` und ohne `expiry_notified_at`, beansprucht `expiry_notified_at` atomar (`UPDATE … FOR UPDATE SKIP LOCKED … RETURNING`, instanzsicher auch ohne ShedLock) und sendet einmal `management.activation.expired` an die Person (`/queue/permissions`, Entity-IDs `activationId`, `managingOrganizationId`, `targetOrganizationId`). Das Frontend führt die Person danach zur Verwaltungsliste von M zurück.
- Nach dem Speichern: `permissions.changed` an die Person (`PermissionRealtimeNotifier`, ohne Cache-Eviction).

Die Liste der öffnenbaren Ziele zeigt je (M, T) die **größte** Dauer und die **Vereinigung** der erlaubten Typen über die abdeckenden Einträge. Die Aktivierung verlangt dagegen **einen** Eintrag, der Dauer und gewählte Rechte zugleich erlaubt. Eine Kombination aus zwei Einträgen kann die Liste anzeigen und die Aktivierung dennoch ablehnen.

Beenden (`ManagementActivationService.endOwn`): nur eigene offene Aktivierungen, sonst 404. Die Bereinigung beim Verlust der Fähigkeit beendet offene Aktivierungen mit `SYSTEM` (siehe unten).

## Zugriffsauswertung

`ManagementAccessService` (request-scoped) ist der **einzige** Weg, wie eine Aktivierung Rechte gibt. `OrganizationAccessService.hasOrganizationPermission(T, p)` fragt ihn nach dem TENANT_ADMIN-Bypass und dem direkten Treffer: `rightsIn(T).contains(p)`. Kein weiterer Check, keine SpEL-Methode pro Feature. `isActiveOrganizationMember` und `OrganizationUserMembershipInterceptor` bleiben unverändert: die Person ist in T nie Mitglied, die Gates gelten weiter.

Rechte in T für die Person = (gewählte Rechte ∩ erlaubte Typen der abdeckenden Einträge ∩ Module von T) − `MANAGEMENT_ACCESS`. Ausgewertet wird **live** bei jedem Request, nicht gecacht:

1. Offene Aktivierung der Person für T (`validUntil` in der Zukunft, nicht beendet). Ohne sie keine Rechte. Die Liste der offenen Aktivierungen wird einmal je Request geladen.
2. M (verwaltende Org aus der Aktivierung): gleicher Mandant, aktiv, Person ist aktives Mitglied von M, M hat `MANAGEMENT_ACCESS` **direkt** (Berechtigungskarte, nicht aus Management), und `isCapable(M)` gilt (Modul an, Gruppe gemappt).
3. T: gleicher Mandant, aktiv.
4. Mindestens ein Audience-Eintrag von M deckt T ab (gemeinsamer Resolver, `TENANT` deckt alles ab). Ohne Treffer keine Rechte. Die erlaubten Typen werden über alle abdeckenden Einträge vereinigt, damit ein Eintrag, der weiter gilt, nicht durch einen anderen mitgerissen wird.
5. Schnitt mit den gewählten Rechten der Aktivierung. `MANAGEMENT_ACCESS` ist in keinem Schritt Teil des Ergebnisses.

Die Regeln für Abdeckung, Obergrenze (Module von T) und erlaubte Typen liegen in `ManagementScope` und werden von Aktivierung und Zugriff gemeinsam genutzt. Fail-closed: jede Exception und jede fehlende Vorbedingung ergibt keine Rechte und eine WARN-Zeile ohne Nutzerdaten. Bei jedem Treffer mit Rechten gibt es eine INFO-Zeile je Request und Ziel (`actor`, `managing`, `target`, `activation`, keine Freitexte).

Folgen der Regeln:

- Ablauf und Beenden wirken für die Rechte ohne Job, weil `validUntil` und `ended_at` bei jedem Request gelesen werden. Der Ablauf-Job sendet nur das Event.
- Modul-Entzug bei T, Entfernen eines Audience-Eintrags, Verlust der Fähigkeit oder der Mitgliedschaft in M entziehen die Rechte beim nächsten Request.
- Org-Baum: Move oder Typwechsel von T wirkt beim nächsten Request, weil die Abdeckung gegen die aktuellen aktiven Organisationen gerechnet wird.
- Keine Ketten: Rechte, die über Management erworben wurden, zählen nicht als `MANAGEMENT_ACCESS` in M. Die Quelle der verwaltenden Organisationen ist die direkte Berechtigungskarte.
- Die Wirkung ist nur in T (`organizationId == T`), nie in anderen Orgs.
- Persönlicher Zustand gehört nur zu Mitgliedschaften: Lesestand, Filter „gelesen/ungelesen“, eigene Rückmeldung zu Terminen und deren Anzeige. Die Rückmeldung liegt unter `POST /api/v1/self/organizations/{id}/appointments/{appointmentId}/responses` mit `isActiveOrganizationMember`. Die Terminliste in T liefert nur die Einträge; `viewed`- und `response`-Filter sind dort mit 400 abgelehnt.

Die Matrix ohne Datenbank steht in `ManagementSecurityTest` (`management/service`). Mandantentrennung auf Tabellenebene prüft `TenantTableRowLevelSecurityArchitectureTest`.

## Validierung (Schreibpfad)

`ManagementAudienceService.replace` (Replace-Semantik; behaltene Einträge behalten ihre ID):

- M ist verwaltungsfähig (`isCapable`), sonst Ablehnung.
- Referenz passt zur Art (`TENANT` ohne Referenz, die anderen mit), keine Duplikate.
- Ziel ist aktiv im Mandanten (Organisation bzw. Organisationsart). Inaktive Ziele werden abgelehnt, damit Auswahl und Speicherung übereinstimmen.
- Ein direkter `ORG`-Eintrag auf die verwaltende Organisation selbst wird abgelehnt (kein Selbst-Ziel). Unterbäume und Arten, die M mit abdecken, sind erlaubt; die Aktivierung schließt M zur Laufzeit ohnehin aus.
- Dauer ≥ 1 Minute.
- Erlaubte Typen: mindestens einer, kein `MANAGEMENT_ACCESS`.

## Bereinigung

`ManagementAudienceCleanupListener` (`@AfterCommitEventListener`) auf Modul-, Standard-, Override- und Berechtigungsgruppen-Änderungen: Einträge von Organisationen, die die Fähigkeit verloren haben, werden gelöscht. Deren offene Aktivierungen werden mit `ended_by = SYSTEM` beendet, und die betroffenen Personen erhalten `permissions.changed`. Das ist Hygiene, damit alte Ziele nicht wieder auftauchen. Der Zugriffsschutz kommt aus der Live-Auswertung, nicht aus dieser Bereinigung.

## API

| Methode | Pfad | Berechtigung |
|---------|------|--------------|
| GET | `/api/v1/auth/admin/tenants/{tenantId}/organizations/{organizationId}/management-audiences` | `canAccessTenant` |
| PUT | dieselbe Pfad | `canAccessTenant` |

Self-API (`isSelfServiceUser()`; Anzeige- und Schreibpfad, die Zugriffswirkung kommt aus der Zugriffsauswertung):

| Methode | Pfad | Hinweis |
|---------|------|---------|
| GET | `/api/v1/self/management/organizations` | `@BoundedList`: Ziele je (M, T) mit größter Dauer und Vereinigung der erlaubten Typen. Keine Paginierung, Suche und Filter (Abweichung von der Listen-Regel, siehe Offen) |
| GET | `/api/v1/self/management/activations` | `@BoundedList`: offene Aktivierungen der Person, aufsteigend nach `validUntil` |
| POST | `/api/v1/self/management/activations` | 201; Body: `managingOrganizationId`, `targetOrganizationId`, `durationMinutes`, `permissionTypes`, `reason` |
| DELETE | `/api/v1/self/management/activations/{id}` | 204; nur eigene offene Aktivierung |

Antwort: Einträge plus Auswahllisten (Organisationen, Organisationsarten). Rechte wählt die aktivierende Person selbst, standardmäßig keine. Das Admin-Org-Detail trägt zusätzlich `managementCapable`; der Abschnitt im Admin-Org-Fenster erscheint nur dann.

## Protokoll der Aktivierungen

Der Admin (Global-Admin: alle Mandanten, Tenant-Admin: der eigene) sieht, **wer wann in welcher Org mit welcher Begründung** aktiv war. Nur lesen: kein Beenden, kein Export, keine Detailseite.

| Methode | Pfad | Berechtigung |
|---------|------|--------------|
| GET | `/api/v1/auth/admin/management-activations` | `isAdminUser`; Global-Admin alle Mandanten (Cross-Tenant-Lesen), Tenant-Admin der eigene |
| GET | `/api/v1/auth/admin/management-activations/{id}` | `isAdminUser`; Detail eines Eintrags, Mandanten-Grenze über RLS; Detailseite `/admin/management/[id]` |
| GET | `/api/v1/auth/admin/management-activations/list-filter-options` | `isAdminUser`; `tenantId` nur für Global-Admin, Organisationen als `targetOrganizationId`/`managingOrganizationId` |
| GET | `/api/v1/auth/organizations/{organizationId}/management-activations` | `ORGANIZATION_READ` in der Ziel-Org; fest auf T gefiltert |

Liste: `ListQuery` + `ManagementActivationListDefinition` + Specification (`ManagementActivationLogService`, kein eigener Zugriffspfad). Zeile nur Person (`userId`/Namen `null` = „Ehemaliges Mitglied“), M, T und `status`; Zeile verlinkt auf die Detailseite. Detail: Mandant, M, T, `status`, `activatedAt`, `validUntil`, `endedAt`/`endedBy`, `permissionTypes`, `reason`.

- **Status** wird abgeleitet, nie gespeichert: `ENDED` (`endedAt` gesetzt), `EXPIRED` (offen, `validUntil` vorbei), `ACTIVE` (offen, `validUntil` in der Zukunft).
- **Sortierung:** `activatedAt` (Standard `desc`), `validUntil`, `person` (Nachname, Vorname), `targetOrganization`. Die Namen kommen über schreibgeschützte Assoziationen an `ManagementActivation` (`user`, `managingOrganization`, `targetOrganization`, nur Lesen, Schreibpfad bleibt über die Ids).
- **Suche:** Wörter müssen je in Person, Namen von M/T oder Anlass vorkommen (Left Joins, damit Zeilen gelöschter Personen auffindbar bleiben).
- **Filter:** `tenantId` (Tenant-Admin: nur der eigene Mandant, sonst 403), `status`, `targetOrganizationId`, `managingOrganizationId` (wiederholbar, OR je Dimension). Ungültige Werte ergeben 400.
- **Beenden:** nur laufende Aktivierungen (offen und `validUntil` in der Zukunft), sonst 404. Die Person erhält `permissions.changed`.

**Lesen global, Schreiben pro Mandant:** Die Liste liest mandantenübergreifend über die Global-Admin-Lese-Policy auf `org_management_activations` (und die gejointen `users`/`organizations`), siehe `multi-tenancy.md`. Das Beenden ist ein Schreibzugriff und läuft deshalb über den Mandantenpfad (`canAccessTenant`, Pfad-Interceptor setzt den Ziel-Mandanten), die Zeile liefert dafür `tenantId`.

Frontend: `/admin/management` (`AdminManagementActivationList`: `defineList`, `useUrlSyncedListState`, `ListControls`, `ListRow`, Aktion „Beenden“ mit Bestätigung nur bei `ACTIVE`), im Admin-Menü als „Verwaltungsprotokoll“; Mandantenfilter nur für Global-Admins. Tests: `AdminManagementActivationControllerSecurityTest`, `AdminTenantManagementActivationControllerSecurityTest`, `ManagementActivationLogServiceTest`, `OrganizationManagementActivationControllerSecurityTest`, `ManagementActivationListTest`, `ManagementActivationServiceTest` (Beenden durch Admin).

## Offen

- Das Protokoll hat keinen DB-Integrationstest (Sortierung über Assoziationen und Left-Join-Suche sind nur beim Start gegen die Entities validiert).
- Abweichung von der Listen-Regel: `GET /self/management/organizations` ist `@BoundedList` statt paginiert mit `ListQuery`. Begründung: die Ziele werden aus Audience-Regeln berechnet, nicht aus einer Tabelle, und sind damit klein. Suche und Paginierung erst nachziehen, wenn die Zielmengen wachsen.
- `ref_id` ohne FK (hängende Einträge werden in der Live-Prüfung ignoriert), Performance des Org-Baum-Ladens bei großen Mandanten.
- Die Fähigkeits-Query ist ein JPQL-Statement ohne DB-Integrationstest. Sie ist beim Start der Anwendung gegen die Entities validiert, aber nicht per Test abgedeckt.
