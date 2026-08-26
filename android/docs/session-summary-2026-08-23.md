# Session Summary — 2026-08-23

Everything done in this working session, why it was done, and where things now stand.
Written so a new collaborator (or a future session) can pick up without re-reading history.

---

## 1. Repository guidance (`AGENTS.md`)

Created `AGENTS.md` at repo root: exact build commands (no Gradle wrapper — use the unpacked
Gradle 8.14 path), JDK 17 location, Robolectric-is-disabled warning, emulator scripts,
Firebase tooling quirks (vendored firebase-tools 13.x for JDK 17), architecture notes
(`applicationId` vs namespace trap, order lifecycle ownership in `OrderStatus.kt`,
Arabic RTL, design-token rule).

## 2. MVP & business-model review

Assessment delivered (not code): MVP loop is complete and well-scoped for Sudan; the missing
half of the business was **the money plumbing** — no commission, no payout ledger, no refund
process, no financial visibility for admins. That review drove sections 4–5 below.

## 3. Money layer (new)

| Piece | Where |
| --- | --- |
| Fixed 15% platform commission, derived once | `data/model/OrderFees.kt` |
| Unique payable amount per order (+1–99 ج.س) so bank-statement lines self-identify | `OrderFees.payableAmountSdg`, shown on receipt/review screens |
| Money terms frozen at booking | `payableAmountSdg` / `providerPayoutSdg` / `commissionSdg` on `OrderEntity`, guarded by rules |
| Provider earnings ledger, one doc per completed visit | `payouts/{orderId}` — double-payment impossible by id |
| Auto-accrual on visit completion | `FirestorePayoutRepository.accrueForCompletedOrder` (transaction, idempotent) |
| Admin finance screen (collected / commission / owed / paid + mark-paid) | `AdminFinanceScreen` via header button on admin dashboard |
| Provider earnings screen + honest income metric (85% share) | `ProviderEarningsScreen`, provider dashboard |
| Written refund policy | `docs/refund-policy.ar.md` |

## 4. Design system sweep ("Serene Health")

Executed `docs/superpowers/specs/2026-08-18-uiux-improvements-design.md`:

- **New primitives:** `StatusColors` / `statusColorsFor()` (one status palette for all
  journeys), `SehatiScreenAppBar` (killed the `Spacer(48.dp)` hack in 8 screens),
  `SehatiPrimaryButton` (single CTA style), `sehatiTextFieldColors()`.
- **Credibility fixes (P0):** fake ETA removed from Home cards; category prices derived from
  real providers (hidden when none); fake "(جاري الآن…)" processing wording replaced with
  honest manual-review copy; string-literal statuses → `OrderStatus` constants.
- **Sweep:** zero raw `.sp` sizes remain in screens/components; off-brand hexes (blue
  availability card, purple physio icon, copy-pasted status colors) moved onto tokens.

## 5. Hardening round

| Item | Outcome |
| --- | --- |
| Bank account number | Was hardcoded 3× → `BuildConfig.BANK_ACCOUNT_NUMBER/NAME`, overridable `-PsehatiBankAccount=…`. ⚠️ still the placeholder `2401234` — set real value before launch |
| Order numbers | `"HM-random"` collisions → time-seeded `OrderFees.newOrderNumber()` |
| FCM tokens | Verified already fully wired (service + token saved to `users/{uid}`); no change needed |
| Cancelled timeline | Renders neutral cancelled row with reason instead of nothing |
| Firestore rules tests | Harness fixed (permanent-account tokens, lifecycle ordering) + 13 payout checks + 10 refund checks → **85/85 pass** locally against the emulator |
| Crashlytics | Plugin + dependency wired; activates after console setup with real `google-services.json` |
| Unbounded feeds | All order feeds capped at newest 200 docs |
| Refund flow | New `REFUND_REQUESTED` status: patient requests on paid orders → appears in admin queue → admin grants (cancels + notifies both sides) or declines (resumes). Enforced by rules, deployed live |
| Retention metric | Repeat-booking rate computed in `foldAdminFinance`'s sibling `repeatBookingRate()`, shown in finance header once data exists |

**Deliberately deferred:** splitting `SehatiViewModel` (~780 lines) into role-scoped
ViewModels — large refactor deserving its own focused session; base64 images → Cloud Storage
(blocked: needs Blaze billing); phone/SMS auth (blocked by SMS region policy for Sudan).

## 6. Collaboration setup

- Repo created and pushed: **https://github.com/Mojaafer/sehati-fi-al-beit** (**private**).
- Committed on purpose so a collaborator can clone-and-run:
  - `app/google-services.json`
  - `debug.keystore` (root) — same SHA-1 already registered in Firebase, so Google Sign-In
    works untouched on any machine.
- `.gitignore` documents why those files are tracked and warns to purge history before ever
  making the repo public.
- Firebase access granted via Cloud IAM CLI:
  `aelrofai@gmail.com` → `roles/firebase.editor` + `roles/editor`.
- Dev artifacts excluded: APKs, `.shots/`, `.verify/`, logs, `__pycache__`.

## 7. Current state & verification

```
Unit tests      : 153 passing, 0 failures   (testDebugUnitTest)
Build           : assembleDebug green       (app/build/outputs/apk/debug/app-debug.apk)
Rules harness   : 85/85 checks vs local Firestore emulator
Live rules/index: deployed to sehati-home-care (firebase deploy --only firestore)
```

Commands (see `AGENTS.md` for machine specifics):

```powershell
# build + test
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.14-all\c2qonpi39x1mddn7hk5gh9iqj\gradle-8.14\bin\gradle.bat" testDebugUnitTest assembleDebug

# rules checks (local emulator only, never touches live data)
node "$env:USERPROFILE\node_modules\firebase-tools\lib\bin\firebase.js" emulators:exec `
  --only firestore --project sehati-home-care "python tools/rules_check.py"

# device run
.\run-lite-emulator.ps1 ; .\install-debug-apk.ps1
```

## 8. Suggested next steps

1. On-device visual pass of patient → payment → refund → payout journey.
2. Set the real bank account: `-PsehatiBankAccount=<real>` or edit default before launch.
3. Enable Crashlytics in the Firebase console.
4. The deferred `SehatiViewModel` split, when the next feature lands.
