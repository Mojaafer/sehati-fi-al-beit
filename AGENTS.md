# AGENTS.md

## What this is

Arabic (RTL) Android app **Sehati Fi Al-Beit** — home-healthcare booking for Sudan
(nursing, lab-draw, physio, doctor visits at home). Kotlin + Jetpack Compose (Material 3),
Firebase Auth + Firestore + FCM. No own backend: no Cloud Functions/Blaze — Firestore
security rules plus client-side transactions only. Single Gradle module `:app`.

## Build & test commands

There is **no Gradle wrapper in the repo** and `gradle` is not on PATH — README's
`./gradlew ...` will fail. Use the unpacked distribution directly (Gradle 8.14):

```powershell
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.14-all\c2qonpi39x1mddn7hk5gh9iqj\gradle-8.14\bin\gradle.bat" assembleDebug
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.14-all\c2qonpi39x1mddn7hk5gh9iqj\gradle-8.14\bin\gradle.bat" testDebugUnitTest
```

- JDK 17 required; this machine has no `java` on PATH. Scripts set `JAVA_HOME=$HOME\jdk-17.0.13+11`.
- Debug APK output: `app\build\outputs\apk\debug\app-debug.apk`.
- The build succeeds without `app/google-services.json` (gitignored): the google-services
  plugin is set to WARN instead of failing (`googleServices.missing.passthrough=true`).
  Sign-in and all data fail at *runtime* until the real file is added — see README setup.
- Keep `gradle.properties` as-is: workers capped at 4 and Kotlin compiler forced in-process
  (avoids "Could not connect to Kotlin compile daemon") for this 8 GB laptop.

## Tests

- `testDebugUnitTest` runs plain-JVM JUnit only. **Robolectric/Roborazzi are intentionally
  disabled** (commented out in `app/build.gradle.kts`): they pull a ~100 MB android-all jar
  onto a nearly full disk over a slow link that corrupted a fetch before. Do not re-enable.
  UI behavior is verified on the emulator/device instead.
- Screenshots of manual verification live in `.shots/` and `.verify/`;
  `tools/uidump.py` dumps the view hierarchy via adb.
- `unitTests.isReturnDefaultValues = true` exists because the Firestore mapper logs through
  `android.util.Log` under JVM tests — don't remove it.

## Emulator workflow

```powershell
.\run-lite-emulator.ps1     # AVD "SehatiLite": API 30, emulator on D:\AndroidSdkLite,
                            # AVD data on D:\AndroidAvd, 2 cores / ~2 GB RAM
.\install-debug-apk.ps1     # waits for boot, installs debug APK, launches MainActivity
```

Keep Android Studio closed while the emulator runs (8 GB laptop).

## Firebase tooling

- Project `sehati-home-care` (`.firebaserc`). Rules/indexes: `firebase deploy --only firestore`.
- `tools/run_rules_check.sh` runs `tools/rules_check.py` against the local Firestore
  emulator (never touches live data). It pins `--project sehati-home-care` because the test
  tokens carry that audience id. It prefers the **vendored firebase-tools 13.x** under
  `~/node_modules/firebase-tools`: firebase-tools ≥14 refuses to run the emulator on JDK 17
  (needs 21). First run fetches a ~130 MB emulator jar into `~/.cache/firebase`.
- `python tools/fb_admin.py <subcommand>` admin helper against the live project using the
  local Firebase CLI login (no service-account key); subcommands: seed-test-roles,
  list-users, create-phone-user, set-role, get-user-doc, sms-regions, allow-sms-regions…

## Architecture notes

- Namespace is `com.example`, but `applicationId` is `com.aistudio.sehatihomecare.sd`.
  Firebase app registration and adb launch use the applicationId:
  `com.aistudio.sehatihomecare.sd/com.example.MainActivity`.
- Layers under `app/src/main/java/com/example/`: `data/model` (Firestore entities),
  `data/repository`, `data/auth`, `ui/{screens,viewmodel,navigation,components,theme}`,
  `service` (FCM). Navigation entry: `ui/navigation/SehatiNavGraph.kt`.
- Roles come from `users/{uid}.role` ∈ {PATIENT (default), PROVIDER, ADMIN}; rules block
  clients changing their own role. A role-switcher header exists **only in debug builds**.
- Order lifecycle lives in `data/model/OrderStatus.kt` — status transitions and who may
  cancel/pay/complete are derived there, not per-screen. Payment is manual Bankak bank
  transfer with admin receipt review (`PAYMENT_UNDER_REVIEW`), no online gateway.
- All user-facing strings are Arabic (RTL).
- Design system ("Serene Health") tokens are in `ui/theme/` (`Color.kt`, `Type.kt`,
  `Shape.kt`, `Theme.kt`). New UI must consume these instead of hardcoded hex colors /
  raw `.sp` values — see `docs/superpowers/specs/2026-08-18-uiux-improvements-design.md`.

## Misc

- `idea/` at repo root is design mockups/assets, **not** IDE configuration (`.idea/` is gitignored).
- `task.md` tracks build/test progress as a checklist — update it when completing related work.
