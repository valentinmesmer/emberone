# 10. Qualitätsanforderungen

## 10.1 Qualitätsbaum

Ableitung aus [1.2 Qualitätsziele](01-introduction-and-goals.md):

```
EmberOne Qualität
├── Sicherheit
│   ├── Mandantenisolation (RLS / Tenant-Kontext)
│   ├── Autorisierung (App-Rolle + Org-Permissions)
│   └── Token-/Session-Integrität (JWT / OIDC)
├── Funktionale Korrektheit
│   ├── Workflow-Lebenszyklus
│   ├── Qualifikations-/Zertifizierungszustände
│   └── Standard-Materialisierung / effektive Config
├── Wartbarkeit
│   ├── Package-/Modulgrenzen
│   ├── Dokumentierte zentrale Facades
│   └── Testbarkeit (Security-WebMvc, Domänentests)
├── Bedienbarkeit / UX
│   ├── Nachvollziehbare Freigabe-Inbox
│   └── Realtime-Feedback (Notifications, Listen)
└── Betreibbarkeit
    ├── Reproduzierbare Dev-Umgebung
    └── CI-Images (+ betreiberspezifisches Deploy)
```

## 10.2 Qualitätsszenarien

| ID | Szenario | Qualitätsmerkmal | Priorität |
|----|----------|------------------|-----------|
| QS-1 | User A aus Mandant 1 darf keine Domänendaten von Mandant 2 lesen/schreiben - auch nicht über erratene IDs | Sicherheit / Isolation | hoch |
| QS-2 | `TENANT_ADMIN` darf Admin-APIs nur im eigenen Mandanten; `GLOBAL_ADMIN` gezielt mit Tenant-Override | Sicherheit / AuthZ | hoch |
| QS-3 | Org-Permission fehlt → Organization-API antwortet 403; Self-API bleibt für eigenen User nutzbar | Sicherheit / AuthZ | hoch |
| QS-4 | Workflow open → participants/definition resolved → approve löst Handler-Nebeneffekte und Side-Channels aus | Korrektheit | hoch |
| QS-5 | Permission-Änderung → Cache-Eviction after commit → nächster Request sieht neue Rechte | Korrektheit / Cache | mittel |
| QS-6 | Subscribe auf fremdes Org-Topic wird vom Realtime-Interceptor abgelehnt | Sicherheit / Realtime | hoch |
| QS-7 | Neuer Entwickler findet für Workflow/Security/Tenant innerhalb der Concept-/arc42-Docs ein Mental Model | Wartbarkeit | mittel |
| QS-8 | Push auf `development`/`main` mit erfolgreichem Semantic Release veröffentlicht Images nach GHCR (nur wenn ein Release publiziert wurde); Deploy ist betreiberspezifisch verdrahtet | Betreibbarkeit | mittel |
| QS-9 | Lokal: `setup.sh` + Compose → Backend/Frontend gegen Authentik/Postgres/Redis/S3 lauffähig | Betreibbarkeit | mittel |
| QS-10 | UI spiegelt grob Backend-Permissions (`usePermissions`); Abweichung dokumentiert/vermeidbar | UX / Sicherheit | mittel |

Performance-, Skalierbarkeits- und Verfügbarkeitsszenarien (Lasttests, Multi-Instanz, p95-Ziele) sind im Qualitätsbaum noch nicht ausgearbeitet. Design: [Skalierbarkeit](../concept/scalability.md).
