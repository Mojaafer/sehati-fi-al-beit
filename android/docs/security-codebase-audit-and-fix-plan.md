# Security and Codebase Audit

Date: 2026-08-25

Scope: read-only review of the Android application, Firebase authentication, Firestore legacy path, Supabase migration, Gradle dependencies, tests, and operational scripts.

## Audit Rules

- No application code was changed during the audit.
- Findings are ordered by practical risk.
- Fixes require approval before implementation.
- Major dependency upgrades are not included in the safe-fix group.
- Existing tests must pass after every fix group.

## Stack

- Kotlin `2.2.10`
- Android SDK 35, target SDK 35, minimum SDK 24
- Jetpack Compose and Material 3
- Gradle Kotlin DSL, Version Catalog, Gradle 8.14
- Firebase Auth, Firestore, Cloud Messaging, Crashlytics, App Check
- Supabase PostgreSQL/PostgREST with Firebase third-party authentication
- Retrofit, OkHttp, Moshi, Coil, Kotlin Coroutines
- JUnit 4 and coroutine/ViewModel unit tests
- Device verification through `adb`

## Test Coverage

Tests exist and are currently runnable:

- JVM unit tests for models, repositories, ViewModels, navigation, auth helpers, and Supabase mappings
- Compose-related test dependencies
- Instrumented test source set with limited coverage
- Device install and launch workflow
- Existing screenshot reference files

Important gaps:

- No meaningful Supabase RLS integration suite
- No authenticated Firebase-to-Supabase end-to-end test
- No complete Supabase-enabled patient/provider/admin device journey test
- No automated vulnerability scanner configured for Gradle dependencies
- UI screenshot/Robolectric tooling is intentionally disabled per project guidance

## Pass 1: Findings

### 1. Security

#### Critical

| Finding | Location | Why it matters | Suggested fix |
|---|---|---|---|
| Firebase OAuth client secret is hardcoded in operational scripts | `tools/fb_admin.py:29`; `tools/check_email_config.py:11`; `tools/delete_all_orders.py:11`; `tools/update_email_template.py:11` | The value is committed source and was exposed during this session. Anyone with repository or chat access may reuse it. | Remove the constant. Reuse Firebase CLI credentials through supported commands or load the value from a local ignored environment variable. Rotate it if it is a real confidential credential. |
| Original Supabase migration contains anonymous RLS bypasses | `supabase/migrations/20260823135131_create_sehati_tables.sql:254-398` | Fresh databases created from this migration are insecure even though the later hardening migration fixes the current hosted database. | Replace or amend the baseline migration so new environments start secure. Keep the deployed hardening migration for existing environments. |
| Authenticated participants can update broad order lifecycle fields | `supabase/migrations/20260823150000_supabase_cutover_security.sql:75-83`; `app/src/main/java/com/example/data/supabase/SupabaseSehatiRepository.kt:138-180` | A client can submit status, refund, cancellation, receipt, and rating flags. The database does not enforce valid lifecycle transitions or actor-specific permissions. | Use narrowly scoped RPCs or column-specific policies that validate current status, actor, and allowed transition. |
| Public `SECURITY DEFINER` functions are not explicitly locked down | `supabase/migrations/20260823135131_create_sehati_tables.sql:36-44`; `189-237` | Functions in the exposed `public` schema may be executable by `PUBLIC`. `submit_provider_rating` does not independently prove that the caller owns the order. | Revoke execute from `PUBLIC`, grant only intended roles, move privileged helpers to a non-exposed schema where possible, and validate `auth.uid()` against the order inside the rating function. |
| Any authenticated user can insert admin notifications | `supabase/migrations/20260823150000_supabase_cutover_security.sql:110-115` | A normal user can spam or forge the admin inbox. | Remove client insert access or require admin/trusted server authorization. |
| Any authenticated user can insert notifications for arbitrary recipients | `supabase/migrations/20260823150000_supabase_cutover_security.sql:100-108` | A user can potentially create notifications targeting another user. | Restrict inserts to the sender/order participants, the recipient itself, or a trusted server-side path. |

#### High

| Finding | Location | Why it matters | Suggested fix |
|---|---|---|---|
| Firebase ID token is force-refreshed synchronously on every Supabase request | `app/src/main/java/com/example/data/supabase/SupabaseAuthTokenProvider.kt:11-16` | Each request can block up to 10 seconds and causes unnecessary auth/network work. | Cache the current token and refresh only when expired or rejected. Do not force refresh on every request. |
| Supabase polling loops silently swallow errors | `SupabaseSehatiRepository.kt:19-100`; `SupabasePayoutRepository.kt:19-46`; `SupabaseNotificationRepository.kt:19-57`; `SupabaseRatingRepository.kt:18-33` | Users can see stale or empty data without knowing that the request failed. Polling every 4-5 seconds also wastes battery and network. | Use explicit loading/content/empty/error/stale state, retry/backoff, and Supabase Realtime for suitable tables. |
| Supabase write failures are swallowed | `SupabaseUserRepository.kt:55-78`; `SupabaseNotificationRepository.kt:60-110`; `SupabaseSehatiRepository.kt:111-180` | The UI may imply that profile, availability, order, notification, or status actions succeeded when the server rejected them. | Return typed results or throw to ViewModels, then expose a retryable error state. |
| Firebase and Supabase profile writes remain mixed | `app/src/main/java/com/example/data/auth/AuthRepository.kt:234-292` | A successful login can leave inconsistent profile/role data when one backend write fails. | Choose Supabase as the profile source of truth and make synchronization failures observable during the transition. |

#### Medium

| Finding | Location | Why it matters | Suggested fix |
|---|---|---|---|
| PostgREST filter strings interpolate user-provided values | `app/src/main/java/com/example/data/supabase/*.kt` | Firebase UIDs are constrained, but arbitrary IDs/categories can produce malformed filters or unexpected queries. | Validate IDs and whitelist enum-like values before interpolation. |
| Release build uses debug signing | `app/build.gradle.kts:77-84` | Production releases are not using a controlled release identity. | Require a real release keystore and fail release builds when it is unavailable. |
| Supabase anon key is duplicated in Gradle and Kotlin | `app/build.gradle.kts:46-49`; `SupabaseConfig.kt:7` | Publishable keys are acceptable in mobile apps only with strict RLS, but duplication makes rotation and review harder. | Keep only publishable keys in client builds and source them from local build properties or generated config. Never use a secret key in Android. |

### 2. Dependencies

The project uses Gradle, not npm or pip. `gradle dependencies --configuration debugRuntimeClasspath` completed successfully.

No lockfile-based Gradle dependency lock is present. OSV Scanner and OWASP Dependency-Check are not installed, so no vulnerability scan result is claimed.

#### Direct dependency inventory

| Package | Current | Latest observed | Decision |
|---|---:|---:|---|
| Android Gradle Plugin | 8.8.0 | 9.5.0-alpha02 | Do not upgrade to alpha automatically |
| Kotlin | 2.2.10 | 2.4.20-RC | Do not upgrade to RC automatically |
| Compose BOM | 2024.09.00 | 2026.08.00 | Requires coordinated UI/API verification |
| Core KTX | 1.15.0 | 1.19.0 | Candidate for separate patch update |
| Activity Compose | 1.10.1 | 1.13.0 | Candidate for separate update |
| Navigation Compose | 2.8.9 | 2.10.0-rc01 | Do not upgrade to RC automatically |
| Lifecycle | 2.8.7 | 2.12.0-alpha01 | Keep stable version |
| Credentials | 1.5.0 | 1.7.0-alpha03 | Keep stable version until Google Sign-In is retested |
| Google ID | 1.1.1 | 1.2.0 | Candidate for separate update |
| Firebase BOM | 34.15.0 | 34.18.0 | Candidate for patch/minor update |
| Coroutines | 1.10.2 | 1.11.0 | Candidate for separate update |
| Retrofit | 2.12.0 | 3.0.0 | Major upgrade; do not apply automatically |
| OkHttp | 4.10.0 | 5.5.0 | Major upgrade; do not apply automatically |
| Moshi | 1.15.2 | 1.15.2 | Current observed latest |
| Coil | 2.7.0 | 2.7.0 | Current observed latest |
| JUnit 4 | 4.13.2 | 4.13.2 | Current observed latest |
| Logging Interceptor | 4.10.0 | Resolves OkHttp to 4.12.0 | Align direct version in a dependency-only pass |

### 3. Meaningful Duplication

| Finding | Location | Why it matters | Suggested fix |
|---|---|---|---|
| Firebase token/session ownership is split | `AuthRepository.kt`; `SupabaseAuthTokenProvider.kt`; Supabase repositories | Refresh, sign-out, and testing behavior can drift because authentication is resolved in multiple layers. | Create one approved session/token boundary only as part of the auth security fix. |
| Firestore and Supabase implementations duplicate domain operations | `app/src/main/java/com/example/data/repository/*`; `app/src/main/java/com/example/data/supabase/*` | Migration fixes must be applied twice and behavior can diverge. | Keep while migration rollback is needed. Remove Firestore implementation only after cutover approval. |
| Supabase polling loops are repeated | Four Supabase repository files | Retry, delay, stale-state, and cancellation behavior are inconsistent. | Prefer Realtime; otherwise extract a small polling policy after deciding state semantics. |

### 4. Obvious Refactors

| Finding | Location | Why it matters | Suggested fix |
|---|---|---|---|
| Login composables are compressed into long one-line declarations | `app/src/main/java/com/example/ui/screens/LoginScreen.kt:206-243` | Layout and accessibility regressions are difficult to review and maintain. | Reformat into normal blocks only. No behavior change. |
| Logging is not centralized | `SehatiViewModel.kt:263,458,557`; `ProviderRegistrationViewModel.kt:116` | Error details and identifiers may be logged inconsistently. | Sanitize identifiers and centralize only if approved as a security cleanup. |
| Firestore fallback remains embedded in AuthRepository | `AuthRepository.kt:234-292` | The migration boundary is unclear and makes it hard to prove which backend owns profile data. | Remove only after explicit Supabase cutover approval. |

### 5. Reusable Pieces

| Finding | Location | Why it matters | Suggested fix |
|---|---|---|---|
| Shared feedback primitives are inconsistently used | `FeedbackComponents.kt`; screen-specific loading/error blocks | Screens present loading/error/empty states with different hierarchy and recovery actions. | Standardize network-feed screens on shared components. |
| Shared primary CTA is not used everywhere | `SehatiPrimaryButton.kt`; raw `Button` usage throughout screens | Button height, loading state, colors, and disabled behavior drift. | Migrate booking, payment, receipt, provider registration, and admin confirmation actions first. |

### 6. Quick Health Checks

| Finding | Location | Why it matters | Suggested fix |
|---|---|---|---|
| Supabase admin/participant feeds request up to 200 rows every 4 seconds | `SupabaseSehatiRepository.kt:36-84` | Poor scalability and battery/network usage. | Realtime or bounded pagination with backoff. |
| Auth token failure falls back to anon key | `SupabaseClient.kt:20-29` | Hides authentication failures and makes user-facing errors confusing. | Fail authenticated requests explicitly when token acquisition fails. Use anon only for truly public endpoints. |
| `markAllRead()` filters notifications by `id=eq.<uid>` | `SupabaseNotificationRepository.kt:102-107` | The operation likely updates zero rows because it should filter `recipient_uid`. | Change filter to `recipient_uid` and add a focused test. |
| Admin notification polling starts for any signed-in user | `SupabaseNotificationRepository.kt:40-57` | Non-admin users generate rejected requests and unnecessary polling. | Start only for known admin roles. |
| Broad exception swallowing is common in Supabase repositories | `app/src/main/java/com/example/data/supabase/*.kt` | Failures become stale/empty UI without recovery. | Use typed results and explicit error states. |
| No automated Gradle vulnerability scanner | Root build configuration | Vulnerabilities can remain unnoticed. | Add OSV Scanner or OWASP Dependency-Check in a separate approved change. |

## Pass 2: Fix Plan

Implementation is approval-gated. Each group stops for build/test verification before the next group.

### Group 1: Security fixes

#### 1.1 Remove committed Firebase client secrets

Files:

- `tools/fb_admin.py`
- `tools/check_email_config.py`
- `tools/delete_all_orders.py`
- `tools/update_email_template.py`

Plan:

1. Replace hardcoded client secret reads with a local environment variable or supported Firebase CLI credential flow.
2. Add the variable to `.env.example` only as a name, never a value.
3. Confirm all scripts fail with a clear message when credentials are missing.
4. Recommend rotation of the exposed credential after deployment.

Behavior change: admin scripts will no longer run with the committed fallback secret.

Verification:

- Python syntax checks
- Run each script help path without credentials
- Confirm no secret-like literal remains in tracked files

#### 1.2 Harden Supabase RPC and policies

Plan:

1. Lock down `SECURITY DEFINER` execution privileges.
2. Add caller ownership checks to `submit_provider_rating`.
3. Restrict admin notification insertion to admins or trusted server paths.
4. Restrict notification insertion to valid sender/order/recipient relationships.
5. Replace broad order update permissions with validated transitions.
6. Use `TO authenticated`/`TO service_role` policy targeting where appropriate instead of relying on `auth.role()` predicates.
7. Add a new migration rather than editing already-applied history.

Behavior changes:

- Clients will no longer be able to forge admin notifications.
- Clients will no longer be able to write notifications to arbitrary users.
- Invalid order transitions will be rejected by the database.
- Ratings will require proof that the caller owns the order.

Verification:

- Local/remote migration review
- Supabase RLS tests for patient, provider, admin, and unauthenticated requests
- Positive and negative transition tests

#### 1.3 Fix authenticated request fallback

Plan:

1. Stop falling back to the anon key when a signed-in Firebase user exists but token refresh fails.
2. Expose a typed authentication/network failure to the ViewModel.
3. Show the existing Arabic retry guidance.

Behavior change: failed authenticated requests will show an error instead of silently making an anonymous request.

Verification:

- Auth token provider unit tests
- Offline/device retry test
- Existing JVM suite and Supabase build

#### 1.4 Require production release signing

Plan:

1. Remove debug signing from the release build type.
2. Require `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` for release.
3. Keep debug signing only for debug builds.

Behavior change: release builds fail unless a real release keystore is configured.

Verification:

- Debug build remains successful
- Release build succeeds only with explicit test keystore variables
- `signingReport` confirms distinct release/debug certificates

### Group 2: Reliability and user-facing state fixes

Plan:

1. Correct `markAllRead()` recipient filtering.
2. Add explicit error/stale state to Supabase polling flows.
3. Add retry/backoff and stop polling when the screen/session is not active.
4. Prevent admin notification polling for non-admin users.
5. Replace silent write failures with typed results and retryable UI events.
6. Standardize network feed screens on `SkeletonCard`, `EmptyState`, and a shared error/retry component.

Behavior changes:

- Users see when content is stale or unavailable.
- Failed actions show recovery options instead of appearing successful.
- Notification read-all works correctly.

Verification:

- ViewModel tests for loading/content/empty/error/stale states
- Offline device test
- Existing unit test suite

### Group 3: Dependency scan and safe updates

Plan:

1. Add a Gradle-compatible vulnerability scan using OSV Scanner or OWASP Dependency-Check.
2. Generate a report for debug and release runtime dependencies.
3. Update only non-major candidates after reviewing scan output:
   - Firebase BOM `34.15.0 → 34.18.0`
   - Google ID `1.1.1 → 1.2.0`
   - Core KTX `1.15.0 → 1.19.0`
   - Activity Compose `1.10.1 → 1.13.0`
   - Coroutines `1.10.2 → 1.11.0`
4. Align direct OkHttp/logging-interceptor versions if the scan supports it.
5. Keep major upgrades separate:
   - Retrofit 2 to 3
   - OkHttp 4 to 5
   - Compose BOM 2024 to 2026
   - AGP 8 to 9
   - Kotlin 2.2 to 2.4

Verification:

- Dependency scan report
- `testDebugUnitTest`
- `assembleDebug -PuseSupabase=true`
- Google Sign-In device test

### Group 4: Safe cleanup and reuse

Plan:

1. Reformat compressed login composables.
2. Migrate booking/payment/receipt/provider registration/admin CTAs to `SehatiPrimaryButton`.
3. Migrate repeated network-feed error/loading blocks to shared feedback components.
4. Keep Firestore/Supabase duplication until the final cutover decision.

Behavior requirement: these changes must not alter business logic or authorization.

Verification:

- Before/after UI screenshots on the phone
- Existing unit tests
- `git diff` review limited to approved files

### Group 5: Motion, illustrations, and status screens

Plan:

1. Use Canvas/Compose graphics only for lightweight, brand-relevant illustrations.
2. Keep continuous animation limited to welcome/hero moments and pause or reduce it when system animation scale is zero.
3. Standardize status screens:
   - Loading: skeleton or progress with explanatory copy
   - Empty: reason plus next action
   - Error: human-readable cause plus retry
   - Offline/stale: explicit offline status and last-known content
   - Success: confirmation, next step, and route-safe back action
4. Use route transitions around 300-500ms; reserve longer choreography for welcome/onboarding only.
5. Avoid animating layout dimensions every frame; use opacity/translation/draw transforms.

Verification:

- Reduced-motion setting
- Font scale 1.5
- Narrow phone width
- RTL long strings
- Device startup and key journey transitions

## Approval Request

Before Pass 2, approve one of these scopes:

1. Security only
2. Security plus reliability/state fixes
3. All groups, including dependency scan and safe cleanups

Recommended: start with **Security plus reliability/state fixes**, then handle dependencies and UI cleanup in separate verified groups.
