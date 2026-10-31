# User-Modul

Design-Notizen für Kern-User und Kontaktinformationen. Package root: `com.emberone.user`. Verwandt: [Mitgliedschaften](user-memberships.md), [Identity Provider](identity-provider-authentik.md), [Admin](admin.md), [Profilbild](profileimage.md).

## Mental Model

| Konzept | Rolle |
|---------|-------|
| **User** | Persistierte Person im Mandanten (`users`) |
| **UserContactInformation** | Adresse/Kontakt am User |
| **Identity** | Authentik-Konto; Provisioning bei Create/Update |
| **Platform-Admin** | JWT `userId == 0` - kein `users`-Row, kein Self-Service |

```
CreateUser (Initial-Member) ──► User + Contact + Username/PersonalNumber
                                    + main Org-Membership
                                    + Authentik Provisioning
CreateUser (Org-Invite)     ──► User + Contact + Username/PersonalNumber
                                    + Authentik Provisioning
                                    + recordInvitationForAdmission:
                                        OPEN (Steps offen)
                                        | APPROVED + Membership (live erfüllt / keine Steps)
```

Produkt-API für User-Hard-Delete gibt es nicht. Hard-Delete von User-Zeilen erfolgt nur über Retention (`USER_MASTER` löscht deaktivierte User nach Frist) bzw. Mandanten-Purge; Organisation hat keinen Hard-Delete-Pfad. **Org-/Konto-Austritt** nur per Workflow, **sofort nach Freigabe** (kein Wunschtermin): Self `ORGANIZATION_LEAVE_REQUEST` (Typ 8, Mein Bereich); Org-Verwaltung für ein anderes Mitglied `ORGANIZATION_MEMBERSHIP_END_REQUEST` (Typ 13). Verlässt der User die **Hauptorg**, endet das Konto vollständig (`deactivateUser`); bei Nebenorg nur diese Mitgliedschaft. Im Admin-Bereich weiterhin direkt `POST /api/v1/auth/admin/tenants/{tenantId}/users/{userId}/deactivate` (ebenfalls sofort). Effekt bei Deaktivierung: alle Org- und Gruppen-Mitgliedschaften enden, `users.deactivated_at`, Identity via `scheduleDeprovision` (Account disable + `deprovisionPendingAt`; asynchroner Job löscht das IdP-Konto), pending Onboarding-Einladungen inkl. Draft-Uploads werden gelöscht. `UserDeactivatedEvent`: aktive Workflows mit dem User als **Opener und als Subject** werden abgebrochen; offene Approver-Claims werden freigegeben (After-Commit). **Reaktivierung ist nicht implementiert** (kein API/Service). Details: [Mitgliedschaften](user-memberships.md), [Workflow-Engine](workflow-engine.md), [Identity Provider](identity-provider-authentik.md), [Aufbewahrung und Löschung](retention-and-erasure.md).

## Was es besitzt

- `User`, `UserContactInformation`, `Salutation`
- Create/Update inkl. Username- und PersonalNumber-Vergabe
- Self-API: Profil, Org-Tree, Attendance-Stats-Hooks, Notification-E-Mail-Verifikation, eigene Onboarding-Einladungen
- Org-API: User unter Organisationen (List, Create, Update Personal-Data); Org-Mitgliedschaft beenden nur per Workflow Typ 13 (kein direktes Deactivate unter `/auth/organizations/...`) - Konto-Deaktivierung nur Admin `POST …/admin/tenants/{tenantId}/users/{userId}/deactivate`
- Public: Notification-E-Mail-Verify-Token

| Controller | Prefix |
|------------|--------|
| `UserSelfController` | `/api/v1/self` |
| `UserController` | `/api/v1/auth/users` → Orgs |
| `UserMembershipOverviewController` | `/api/v1/auth/organizations/{organizationId}/users/{userId}/membership-overview` und `…/main-change-targets` |
| `PublicNotificationEmailController` | `/api/v1/public` |

Onboarding-Einladungen (Self/Org): siehe [Onboarding](onboarding.md) (`OnboardingInvitationSelfController`, `OnboardingInvitationOrganizationController`).

## Was es nicht besitzt

| Thema | Woanders |
|-------|----------|
| Org-/Gruppen-Mitgliedschaften | `userorganizationmembership`, `userorganizationgroupmembership` |
| App-Rollen-Zuweisung | `admin.user` + Authentik |
| Permissions / Profilbild / Dokumente | `userpermission`, `profileimage`, `userdocument` |

## Invarianten & Fallstricke

1. `createInitialTenantMember` (Admin `POST …/leader-user`) legt sofort eine main Org-Membership an. Danach kann Admin optional Onboarding für diesen Member erfassen (`recordCompletedOnboarding`, siehe [Onboarding](onboarding.md)). Membership bleibt, kein Workflow. Org-Invite (`createInvitedUser`) nutzt `recordInvitationForAdmission`: Steps offen → `OPEN`-Einladung ohne Membership; alle Steps live erfüllt (oder keine konfiguriert) → leere `APPROVED`-Einladung + `ensureOrganizationMembershipAndAllMembersGroup` (wie Join-Direktaufnahme). Admin legt keine weiteren Mitglieder an.
2. Kein Produkt-API-Hard-Delete für User/Organisation/Onboarding-Einladung. Ausnahmen außerhalb der Produkt-API: `USER_MASTER`-Retention (löscht deaktivierte User), Tenant-Purge, IdP-Delete nach Deprovision. Mitgliedschaften werden nur beendet, außer bei **User-Deaktivierung** (alle Memberships enden automatisch).
3. Notification-E-Mail-Verify: Token gehasht, 24h TTL.
4. `dateOfBirth` (Create/Update/Setup): wenn gesetzt, muss es ein vollständiges Kalenderdatum mit plausibler vierstelliger Jahreszahl sein - nicht in der Zukunft und nicht älter als 150 Jahre (`@DateOfBirth` / Frontend `isValidDateOfBirth`). Kurzjahre wie `4` (oft als `0004-…`) werden abgelehnt.

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Entity | `user/domain/User.java`, `UserContactInformation.java` |
| Service | `user/service/UserService.java` |
| Self / Org / Public | `user/controller/UserSelfController.java`, `UserController.java`, `UserMembershipOverviewController.java`, `PublicNotificationEmailController.java` |
| Events | `user/event/UserCreatedEvent.java`, `UserDeactivatedEvent.java` |
| Geburtsdatum-Validierung | `@DateOfBirth` / `DateOfBirthRules` (`platform.validation`); Frontend `lib/validation/dateOfBirth.ts` |
