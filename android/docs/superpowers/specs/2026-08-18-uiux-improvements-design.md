# Sehati Fi Al-Beit — UI/UX Consistency & Credibility Improvements

**Date:** 2026-08-18
**Status:** Design approved, pending spec review
**Scope:** Full review action list (P0 + P1 + P2)

## Goal

Bring every screen of the Sehati home-healthcare app onto the existing Serene
Health design system, remove fake/placeholder data that misrepresents a
healthcare service, and eliminate duplicated UI scaffolding. The design tokens
and type scale already exist (`ui/theme/Color.kt`, `Type.kt`, `Shape.kt`,
`Theme.kt`); most screens bypass them with hardcoded hex colors and raw `.sp`
sizes. This work makes the screens consume the system instead.

## Non-goals

- No dark mode. `Theme.kt` is intentionally light-only.
- No new features, screens, or data model changes.
- No refactoring unrelated to UI consistency or the listed fake-data fixes.
- No Firebase/backend changes.

## Approach

Foundation-first, then sweep by journey. Build the reusable primitives once so
screens become consistent by construction, then sweep each journey (auth →
patient → provider → admin) to consume them. This avoids relocating the drift
into ad-hoc per-screen fixes, and journey-grouped phases map onto on-device
verification.

## Verification model

- **By the assistant, every phase:** `assembleDebug` compiles + the 26 unit
  tests pass + self-review of each diff against the theme tokens.
- **By the user:** the on-device visual pass per journey. Unit tests cannot
  catch visual regressions, so device confirmation is the definition of done.
- Not a git repository: the spec/plan are written as files but not committed.

## Foundation — shared primitives (Phase 0)

### 1. Semantic status tokens (`ui/theme/Color.kt`)

Status color pairs are currently copy-pasted as raw hex across MyOrders,
ProvidersList, AdminDashboard, HomeScreen, and OrderStatusTimeline. Replace with
a single source:

- A `StatusStyle(val bg: Color, val fg: Color)` data class.
- A `StatusColors` object exposing: `success` (`#DCFCE7`/`#166534`),
  `warning` (`#FEF3C7`/`#B45309`), `error` (`#FEE2E2`/`#991B1B`),
  `neutral` (slate — `surfaceContainer` / `#475569`).
- A `statusColorsFor(status: OrderStatus): StatusStyle` helper so MyOrders,
  OrderSuccess, and OrderStatusTimeline stop hand-mapping status → colors.

Kept outside the M3 `ColorScheme` (which has no custom slots); a plain object is
the simplest fit for a light-only theme.

### 2. `SehatiScreenAppBar` (`ui/components/SehatiScreenAppBar.kt`)

`SehatiScreenAppBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {})`
with a balanced start/end layout so the title centers without the manual
`Spacer(48.dp)` hack. Replaces the duplicated app bar in ProvidersList,
ProviderProfile, Rating, and the other screens carrying the hack.

### 3. `SehatiPrimaryButton` (`ui/components/`)

Wraps M3 `Button` with the single agreed CTA style: `colorScheme.primary`
container, white content, `shapes.medium` (12dp), 52dp height. Normalizes the
OrderSuccess and PaymentMethod buttons that currently use `primaryContainer`.

Decision: 12dp radius (already dominant, lowest churn). DESIGN.md nominally
specifies 8dp for buttons — noted as the alternative if the user prefers strict
doc adherence.

### 4. `sehatiTextFieldColors()` (theme helper)

Uses surface tokens instead of forced `Color.White` field containers (currently
in UploadReceipt and others) so inputs sit correctly on the alabaster
background.

## P0 — fake-data / credibility fixes (Phase 1)

1. **HomeScreen fake ETA** — the hardcoded `"الوصول المتوقع: ٣٠–٤٥ دقيقة"` on
   every provider card is removed. `ProviderEntity` has no ETA field; the card
   keeps the real `distanceKm` and `isAvailableNow`.
2. **Category prices** — the hardcoded `"من 12,000 ج.س"` on the 2×2 category
   grid is replaced by a "starting from" value derived from the minimum real
   `priceSdg` among providers in that category. If the full provider list is not
   available on Home (only the "available now" subset is), remove the price from
   the category cards instead of showing a fabricated one.
3. **PaymentReview fake step** — the 3-step tracker's `"(جاري الآن...)"` implies
   live processing that does not exist (manual bank-transfer review). Drive the
   tracker from the real `order.status` and reword step 2 to an honest
   "قيد المراجعة" with expectation-setting copy (manual review may take time)
   instead of a fake spinner.
4. **OrderSuccess status mismatch** — replace string literals
   (`order.status == "ORDER_SENT"`) with `OrderStatus` constants throughout.
   Pure correctness fix; no visible change.

## P1 + P2 — the journey sweep

Each screen: hardcoded hex → tokens / `StatusColors`; raw `.sp` → `Typography`;
adopt the foundation primitives. P2 polish items are folded into the relevant
screen as it is swept.

### Auth (Phase 5)

- **LoginScreen** — `StatusColors.warning` for the spam-warning box; typography
  scale; a one-line note clarifying sign-in is via email/Google (phone SMS is
  blocked for Sudan).

### Patient (Phase 2)

- **HomeScreen** — largest hardcoded-hex offender → tokens; includes P0 ETA and
  category-price fixes; `SehatiPrimaryButton` where applicable.
- **ProvidersListScreen** — `SehatiScreenAppBar`; availability chip via
  `StatusColors`; tokens.
- **ProviderProfileScreen / RatingScreen / NotificationsScreen /
  UserProfileScreen** — already token-clean → `SehatiScreenAppBar` swap and
  minor touch-ups only.
- **Booking** — tokens.
- **PaymentMethodScreen** — rework the single-option radio group into a plain
  confirmed-method card (P2); `SehatiPrimaryButton`; tokens.
- **UploadReceiptScreen** — `sehatiTextFieldColors()`; `StatusColors.warning`
  for the bank-details box; `SehatiPrimaryButton`.
- **PaymentReviewScreen** — real-status tracker (P0 #3); `StatusColors`; tokens.
- **OrderSuccessScreen** — `OrderStatus` constants (P0 #4); primary button.
- **PaymentConfirmedScreen** — `SehatiPrimaryButton` normalize; already clean.
- **MyOrdersScreen** — `statusColorsFor()` replaces the hand-mapped `statusBg` /
  `statusColor` maps; tokens.
- **OrderStatusTimeline** — tokens (already clean) + a distinct cancelled visual
  (currently renders nothing for CANCELLED).

### Provider (Phase 3)

- **ProviderDashboardScreen** — fix the off-brand blue `AvailabilityCard`
  (`#0284C7`/`#334155`) → teal/sage tokens; metrics via `StatusColors`; tokens;
  `SehatiPrimaryButton`.
- **ProviderRegisterScreen / ProviderDocsUploadScreen / ProviderPendingScreen** —
  already clean → minor token touch-ups + `SehatiPrimaryButton`.

### Admin (Phase 4)

- **AdminDashboardScreen** — status chips via `StatusColors`; confirm the reject
  path records a reason so the patient notification is meaningful.

## Phase plan

| Phase | Content | Assistant verify | Device verify (user) |
|-------|---------|------------------|----------------------|
| 0 | Foundation: 4 primitives | compile + tests | none (nothing consumes them yet) |
| 1 | P0 fake-data fixes | compile + tests | spot-check Home, PaymentReview, OrderSuccess |
| 2 | Patient sweep | compile + tests | full patient journey |
| 3 | Provider sweep | compile + tests | provider journey |
| 4 | Admin sweep | compile + tests | admin journey |
| 5 | Auth + final polish | compile + tests | login flow |

The app compiles and the tests pass after every phase.

## Risks

- **Provider/admin device verification (Phases 3–4) depends on a working
  provider/admin login.** +249 phone sign-in is blocked by Google, so this
  relies on the Google/email test accounts having those roles granted
  server-side. If a role login is unreachable, that phase falls back to
  compile + tests + diff self-review, with device verification deferred.
- **Category-price derivation (P0 #2)** depends on the full provider list being
  available on Home. If only the "available now" subset is present, the price is
  removed rather than derived.
- **Light-only theme** — no dark-mode verification required.
- **Not a git repo** — spec and plan are files, not commits.

## Success criteria

- No hardcoded status/brand hex colors remain in swept screens; status colors
  flow from `StatusColors` / `statusColorsFor()`.
- No raw `.sp` font sizes remain in swept screens; text uses `Typography`.
- The `Spacer(48.dp)` app-bar centering hack is gone; screens use
  `SehatiScreenAppBar`.
- Primary CTAs share one color and radius via `SehatiPrimaryButton`.
- No fabricated ETA, category price, or "processing now" state is shown.
- `assembleDebug` compiles and all 26 unit tests pass.
- The user confirms each journey visually on-device.
