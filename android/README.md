# صحتي في البيت — Sehati Fi Al-Beit

Arabic (RTL) home-healthcare booking app for Sudan. Patients book nursing, lab-draw,
physiotherapy and doctor visits at home; providers accept and complete jobs; an admin
verifies bank-transfer receipts and approves provider applications.

Android · Kotlin · Jetpack Compose (Material 3) · Firebase Auth + Firestore.

## Setup

**Prerequisites:** Android Studio, JDK 17.

The app will build without Firebase configured, but sign-in and all data will fail at
runtime until the steps below are done.

### 1. Firebase console

1. Create a Firebase project, then add an **Android** app with package name
   **`com.aistudio.sehatihomecare.sd`** (this is the `applicationId` — *not* the
   `com.example` namespace).
2. Download `google-services.json` and place it in `app/`.
   It is committed so collaborators can build immediately — this repo must stay private.
3. Register your signing certificates for Google sign-in:
   ```bash
   ./gradlew signingReport
   ```
   Copy the debug **SHA-1** and **SHA-256** into the Firebase Android app settings.
4. **Authentication → Sign-in method:** enable **Email/Password**, turn on
   **Email link (passwordless sign-in)**, and enable **Google** and **Anonymous**.
5. In **Authentication → Settings → Authorized domains**, ensure
   `sehati-home-care.firebaseapp.com` is present. Email sign-in links return to
   `https://sehati-home-care.firebaseapp.com/__/auth/links` and are opened by the app.
6. Create a **Firestore** database. Pick the region closest to your users
   (`europe-west1` is the nearest option to Sudan) — it can never be changed afterwards:
   ```bash
   firebase firestore:databases:create "(default)" --location europe-west1
   ```
   Create it *before* your first deploy, or the CLI will silently create it in `nam5`.
7. Publish the security rules and indexes from this repo:
   ```bash
   firebase deploy --only firestore
   ```
   Or paste `firestore.rules` into the console by hand.

### 2. Run

Run from this `android/` directory using the committed Gradle wrapper (Gradle 8.14,
JDK 17):

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\jdk-17.0.13+11"
.\gradlew.bat testDebugUnitTest assembleDebug -PuseSupabase=true
```

`-PuseSupabase=true` routes the data layer through Supabase PostgREST (server-side
`transition_order` RPC + RLS). Omit it to build against Firestore instead.

Then run the app from Android Studio. Passwordless email links should be opened on
the same device; if opened elsewhere, the app asks for the same email again.

### Lightweight local emulator

This workspace includes a small API 30 Google APIs emulator configured for low-resource laptops.
Its SDK image and AVD data live on `D:` to avoid consuming the limited space on `C:`:

```powershell
.\run-lite-emulator.ps1
.\install-debug-apk.ps1
```

The emulator uses Windows Hypervisor Platform, 2 CPU cores, and approximately 2 GB RAM.
Keep Android Studio closed while it is running on an 8 GB laptop.

## Roles

Role comes from `users/{uid}.role` and is one of `PATIENT` (default), `PROVIDER`, `ADMIN`.
Clients cannot change their own role — the rules block it. To get an admin, edit the
role field directly in the Firestore console.

In debug builds a role-switcher header is shown so all three graphs can be exercised on
one device; it is compiled out of release builds.

Provider sign-up writes a `PENDING_REVIEW` provider document and stores `providerId` on
the user doc. An admin approving the application flips the provider to `ACTIVE` and sets
that user's role to `PROVIDER`.

## Firestore data

| Path | Purpose |
| --- | --- |
| `users/{uid}` | email, name, address, role, providerId |
| `users/{uid}/notifications/{id}` | in-app inbox, written by whoever changes an order |
| `providers/{id}` | profile, price, status, `ratingSum`/`ratingCount`, document URLs |
| `orders/{id}` | booking, status, payment details, `receiptUrl`, `isRated` |
| `ratings/{orderId}` | one rating per order — the document id enforces it |
| `images/{id}` | base64 receipt and provider-document images |

Ratings are aggregated onto the provider in a client-side transaction, so no Cloud
Functions (and therefore no Blaze plan) are needed. Offline support comes from
Firestore's built-in persistence.

## Images

Cloud Storage needs the Blaze plan, so images live in Firestore instead: each one is
downscaled and JPEG-compressed on device, then written to `images/{id}` as a base64
string. Order and provider documents store only the image id, which keeps list queries
small. `FirestoreImageRepository.MAX_BYTES` guards the 1 MiB per-document limit.

Images are chosen with `PickVisualMedia`, which needs no storage permission.
