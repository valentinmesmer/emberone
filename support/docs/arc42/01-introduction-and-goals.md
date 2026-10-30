# 1. Einführung und Ziele

## 1.1 Anforderungsüberblick

**EmberOne** ist eine mandantenfähige Feuerwehr-Verwaltungsplattform für Organisation, Personal, Qualifikation, Termine und Freigabeprozesse.

Sie bündelt:

| Bereich | Was EmberOne leistet |
|---------|----------------------|
| **Organisation** | Organisationsbaum, Org-Gruppen, Mitgliedschaften, Rollen-/Berechtigungskaskade |
| **Standards** | Blueprints für Setup (Gruppen, Permissions, Workflow-Definitionen, Module) inkl. Tenant-/Org-Overrides |
| **Workflows** | Generische Genehmigungs-Engine (Adressänderung, Namensänderung, Zertifizierungs-/Lehrgangsnachweise, …) |
| **Qualifikation & Zertifizierung** | Kataloge, Übungsstatus, Org-Entscheidungen, Zertifikats-/Lehrgangsnachweise |
| **Termine & Events** | Termine (inkl. Serien), Einsätze/Events mit Anwesenheit |
| **Zeiterfassung & Abwesenheit** | Freie Zeiteinträge (kein Stempeluhr-Modell); Abwesenheiten als direktes CRUD (kein Workflow) |
| **Dokumente & Profil** | User-Dokumente, Profilbilder, Notfallkontakte |
| **Kommunikation** | In-App-Benachrichtigungen, E-Mail, Realtime (WebSocket/STOMP), Todos |

Zielgruppe sind Organisationen, die Personal, Nachweise und Freigabeprozesse **mandantenisoliert** und **berechtigungsgesteuert** digitalisieren wollen - nicht eine generische CRM-/ERP-Suite.

Fachliche Vertiefung: [`support/docs/concept/`](../concept/00-index.md).

## 1.2 Qualitätsziele

| Priorität | Qualitätsziel | Motivation |
|-----------|---------------|------------|
| 1 | **Mandantenisolation & Sicherheit** | Falsche Tenant-/Permission-Grenzen sind Data-Leak-Klasse; FORCE RLS + Authz sind Kernrisiko |
| 2 | **Korrektheit der Domänenprozesse** | Workflow-, Qualifikations- und Zertifizierungszustände müssen nachvollziehbar und konsistent sein |
| 3 | **Wartbarkeit bei Domänenwachstum** | Modularer Monolith mit klaren Packages; zentrale Facades (Workflow, Security, Standard) dokumentieren statt „implizit wissen“ |
| 4 | **Bedienbarkeit & Realtime-Feedback** | Org-Alltag (Inbox, Benachrichtigungen, Termine) braucht schnelle UI und Live-Updates |
| 5 | **Betreibbarkeit** | Docker-basierte Dev-/Deploy-Pfade, reproduzierbare CI, externe IdP-/Storage-Abhängigkeiten klar trennbar |

## 1.3 Stakeholder

| Rolle | Erwartungen |
|-------|-------------|
| **Endnutzer (Mitglieder)** | Self-Service: Profil, Abwesenheit, Zeiterfassung, Workflows, Dokumente, Benachrichtigungen |
| **Org-Verantwortliche / Gruppenleiter** | Freigaben, Mitgliederverwaltung, Qualifikations-/Zertifizierungsentscheidungen, Termine |
| **Tenant-Admins** | Mandanten-Setup (Standards, Kataloge, Workflow-Definitionen, User-Provisioning) |
| **Global Admins (Plattform)** | Tenant-Lifecycle, plattformweite Konfiguration, Support über Mandantengrenzen hinweg |
| **Entwicklungsteam** | Klare Baustein-/Package-Grenzen, testbare Security, nachvollziehbare Cross-Cutting-Konzepte |
| **Betrieb / Deploy** | Container-Images, Secrets, Abhängigkeiten (Postgres, Redis, Authentik, S3, Mail) |
