# App Testing & Build Progress (`task.md`)

## Tasks

### Unit & UI Tests
- [x] Configure JDK 17 & Android SDK environment <!-- id: 0 -->
- [x] Fix Gradle / AGP / AndroidX / Kotlin dependencies <!-- id: 1 -->
- [x] Create unit test suite (`SehatiUiStateTest`, `SehatiViewModelTest`, `OrderEntityTest`, `SehatiDaoTest`) <!-- id: 2 -->
- [x] Create UI test suite (`WelcomeRoleScreenTest`, `HomeScreenTest`, `BookingAndPaymentFlowTest`, `ProviderDashboardTest`, `AdminDashboardTest`) <!-- id: 3 -->
- [x] Execute unit and UI test suite via `gradle testDebugUnitTest` (26 tests, 100% pass) <!-- id: 4 -->

### APK Compilation
- [x] Build Debug APK (`assembleDebug`) $\rightarrow$ [`app-debug.apk`](file:///c:/Users/HP/Desktop/health/app/build/outputs/apk/debug/app-debug.apk) (22.8 MB) <!-- id: 5 -->
- [x] Build Release APK (`assembleRelease`) $\rightarrow$ [`app-release.apk`](file:///c:/Users/HP/Desktop/health/app/build/outputs/apk/release/app-release.apk) (15.7 MB) <!-- id: 6 -->
- [x] Update `walkthrough.md` with complete APK build details <!-- id: 7 -->

### Money Layer (commission + payouts)
- [x] Fixed 15% commission derived in `OrderFees` (payout = price − commission, no drift) <!-- id: 8 -->
- [x] Unique payable amount per order (+1–99 ج.س suffix) for bank-statement matching <!-- id: 9 -->
- [x] `payableAmountSdg` / `providerPayoutSdg` / `commissionSdg` on `OrderEntity`, frozen by rules <!-- id: 10 -->
- [x] `payouts/{orderId}` accrual ledger, auto-created when a provider completes a visit <!-- id: 11 -->
- [x] Firestore rules + index for payouts (provider reads own, admin flips ACCRUED→PAID only) <!-- id: 12 -->
- [x] Admin finance screen: collected / commission / owed / paid + one-tap "تم التحويل" <!-- id: 13 -->
- [x] Provider earnings screen + payout-based "دخل مكتمل" on the dashboard <!-- id: 14 -->
- [x] Unit tests for fees, folds and defaults (149 passing); refund policy in `docs/refund-policy.ar.md` <!-- id: 15 -->

### Hardening Round
- [x] Bank account/name → BuildConfig (`-PsehatiBankAccount=…`), no more triple-pasted "2401234" <!-- id: 16 -->
- [x] Collision-proof order numbers (`OrderFees.newOrderNumber`, time-seeded + random tail) <!-- id: 17 -->
- [x] FCM token wiring verified end-to-end (service registered + token saved to users/{uid}) <!-- id: 18 -->
- [x] OrderStatusTimeline renders CANCELLED with reason instead of nothing <!-- id: 19 -->
- [x] Rules harness fixed (permanent-account provider tokens, receipt-after-accept order) + 13 payout checks → **85/85** <!-- id: 20 -->
- [x] Crashlytics plugin + dependency wired (no-op until console setup) <!-- id: 21 -->
- [x] All order feeds capped at newest 200 docs <!-- id: 22 -->
- [x] Refund flow: REFUND_REQUESTED status, patient request UI, admin grant/decline, rules + 10 harness checks, deployed live <!-- id: 23 -->
- [x] Repeat-booking rate shown in admin finance (hidden until first completed visit) <!-- id: 24 -->
