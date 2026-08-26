# AGENTS.md

## What this repo is

Monorepo with two projects:

1. **`android/`** — Arabic (RTL) Android app **Sehati Fi Al-Beit**: home-healthcare
   booking for Sudan (nursing, lab-draw, physio, doctor visits at home).
   Kotlin + Jetpack Compose (Material 3). Firebase Auth + FCM for identity/push;
   data plane is dual: Firestore **or** Supabase PostgREST behind
   `assembleDebug -PuseSupabase=true` (`RepositoryFactory` switches repositories).
   Single Gradle module `:app`. Hosted Supabase project `wolngyvenfyuaigjxajs`.
2. **`website/`** — bilingual Next.js site (see `website/README.md`). Node/npm;
   do not mix its tooling with the Android build.

## Android build & test commands

Use the committed Gradle wrapper (Gradle 8.14). Run from the repo root:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\jdk-17.0.13+11"
.\android\gradlew.bat -p android testDebugUnitTest assembleDebug -PuseSupabase=true
```

- JDK 17 required; this machine has no `java` on PATH — set `JAVA_HOME` as above.
- Add `--no-daemon` on this 8 GB laptop if a daemon is already resident.
- Debug APK output: `android\app\build\outputs\apk\debug\app-debug.apk`.
- `-PuseSupabase=true` sets `BuildConfig.USE_SUPABASE=true`; without it the app uses
  Firestore. Always smoke-test the Supabase variant after data-layer changes.
- The build succeeds without `android/app/google-services.json`, but the file IS
  committed (repo must stay private). Sign-in fails at runtime if Firebase console
  config drifts — see README setup.
- Keep `android/gradle.properties` as-is: workers capped at 4 and Kotlin compiler
  forced in-process (avoids "Could not connect to Kotlin compile daemon") on this
  8 GB laptop.

## Tests

- `testDebugUnitTest` runs plain-JVM JUnit only. **Robolectric/Roborazzi are intentionally
  disabled** (commented out in `android/app/build.gradle.kts`): they pull a ~100 MB
  android-all jar onto a nearly full disk over a slow link that corrupted a fetch before.
  Do not re-enable. UI behavior is verified on a device instead.
- Screenshots of manual verification live in `.shots/` and `.verify/`;
  `android/tools/uidump.py` dumps the view hierarchy via adb.
- `unitTests.isReturnDefaultValues = true` exists because mappers log through
  `android.util.Log` under JVM tests — don't remove it.

## Device workflow

A physical TECNO CL7k (Android 14) is the primary test device; `adb` hangs on
`logcat -d` there (use file redirection + timeout, or skip). Emulator scripts:

```powershell
android\run-lite-emulator.ps1     # AVD "SehatiLite": API 30, emulator on D:\AndroidSdkLite,
                                  # AVD data on D:\AndroidAvd, 2 cores / ~2 GB RAM
                                  # NOTE: emulator binary currently NOT installed at that path
android\install-debug-apk.ps1     # waits for boot, installs debug APK, launches MainActivity
```

The Google `android` CLI is installed at `%USERPROFILE%\AppData\AndroidCLI\android.exe`
(not on PATH). `adb` is at `C:\Users\HP\Android\Sdk\platform-tools\adb.exe`.

Keep Android Studio closed while an emulator runs (8 GB laptop).

## Firebase / Supabase tooling

- Firebase project `sehati-home-care` (`android/.firebaserc`). Rules/indexes:
  `firebase deploy --only firestore`.
- Supabase CLI is linked to hosted project `wolngyvenfyuaigjxajs`; migrations live in
  `android/supabase/migrations/` and deploy with `supabase db push` (run inside
  `android/supabase`'s parent — linked config is in `android/supabase/config.toml`).
  **Migrations are immutable once pushed** — always add a new timestamped file.
- `python android/tools/fb_admin.py <subcommand>` admin helper against live Firebase
  using the local CLI login. All OAuth-touching scripts require
  `$env:FIREBASE_CLI_CLIENT_SECRET` (hardcoded secrets were removed).

## Architecture notes

- Namespace is `com.example`, but `applicationId` is `com.aistudio.sehatihomecare.sd`.
  Firebase app registration and adb launch use the applicationId:
  `com.aistudio.sehatihomecare.sd/com.example.MainActivity`.
- Layers under `android/app/src/main/java/com/example/`: `data/model`,
  `data/repository` (Firestore), `data/supabase` (PostgREST mirror),
  `data/auth`, `ui/{screens,viewmodel,navigation,components,theme}`, `service` (FCM).
  Navigation entry: `ui/navigation/SehatiNavGraph.kt`.
- Roles come from `users/{uid}.role` ∈ {PATIENT (default), PROVIDER, ADMIN}; RLS/rules
  block clients changing their own role. A role-switcher header exists only in debug builds.
- Order lifecycle lives in `data/model/OrderStatus.kt`; on the Supabase path every
  status change goes through the server-side `transition_order` RPC (direct order
  UPDATE is blocked by policy). Payment is manual Bankak transfer with admin review.
- All user-facing strings are Arabic (RTL).
- Design system ("Serene Health") tokens are in `ui/theme/` plus shared primitives
  (`SehatiPrimaryButton`, `sehatiTextFieldColors()`, `StatusColors`, `RoleColors`,
  `bodySmallReadable`). New UI must consume these — no hardcoded hex or raw `.sp`.

## Misc

- `android/idea/` is design mockups/assets, **not** IDE configuration (`.idea/` is gitignored).
- Root-level leftovers `assets/`, `public/`, `metadata.json` (now `android/public/`,
  `android/metadata.json`) are AI Studio prototype remnants — safe to delete after review.
- `android/task.md` tracks build/test progress as a checklist — update it when completing work.
