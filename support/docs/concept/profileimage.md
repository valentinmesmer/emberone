# Profilbild

Design-Notizen für ein Profilbild pro User (PK = `userId`) in S3. Package root: `com.emberone.profileimage`. Verwandt: [User](user.md), Object Storage, [Aufbewahrung und Löschung](retention-and-erasure.md) (`EXIT_IMMEDIATE`).

## Mental Model

| Konzept | Rolle |
|---------|-------|
| **UserProfileImage** | Metadaten + Object-Key (`doc_user_profile_images`) |
| **User** | Besitzer; kein Bild-Feld am User selbst |

```
Upload (JPEG/PNG/GIF/WebP only, no SVG, max 5MB)
        │
        ▼
ProfileImageService ──► ObjectStorage (profile-images-bucket)
        Key: tenants/{tid}/users/{uid}/profile-image/…
```

Schreiben (Upload/Delete) nur über Self; Org-API ist read-only. Bei Austritt löscht `EXIT_IMMEDIATE` (0 Tage) Profilbild + S3-Objekt beim nächsten Retention-Lauf.

## Was es besitzt

- Self: Upload / Download / Delete
- Org: Download (`PROFILE_IMAGE_READ`) - kein Org-Write
- Content-Type-Allowlist wie Attachments (Rasterbilder; kein SVG) plus Delivery-Sanitizing
- Key-Prefix-Hardening gegen Cross-User-Access; Tenant-Mismatch → `SecurityException`

| Controller | Prefix | Gate |
|------------|--------|------|
| `SelfImageController` | `/api/v1/self/profile-image` | Self (Read/Write) |
| `ProfileImageController` | `/api/v1/auth/organizations/{organizationId}/users/{userId}/profile-image` | `PROFILE_IMAGE_READ` (Download only) |

## Was es nicht besitzt

Allgemeine Attachments, User-Dokumente, User-Entity-Felder.

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Entity | `profileimage/domain/UserProfileImage.java` |
| Service | `profileimage/service/ProfileImageService.java` |
| Self / Org | `profileimage/controller/SelfImageController.java`, `ProfileImageController.java` |
