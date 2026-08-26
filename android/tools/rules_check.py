"""
Drives the real firestore.rules against the local emulator over its REST API.

Everything in app/src/test runs against FakeSehatiRepository, which answers every call happily.
That means the whole suite would still pass if Firestore denied every write on the device — the
rules are the one layer unit tests structurally cannot reach. This walks the booking lifecycle
(order -> receipt -> approval -> completion -> rating) as real signed-in users and asserts both
that the legitimate steps are allowed and that the obvious attacks are refused.

Run via tools/run_rules_check.sh, which starts the emulator around it. Talks only to
localhost:8080 — the live sehati-home-care project is never touched.
"""

import base64
import json
import sys
import urllib.error
import urllib.request

PROJECT = "sehati-home-care"
BASE = f"http://127.0.0.1:8080/v1/projects/{PROJECT}/databases/(default)/documents"

failures = []
checks = 0


def token_for(uid, sign_in_provider="anonymous"):
    """The emulator accepts an unsigned JWT, so a test user needs no Auth emulator."""
    def seg(obj):
        raw = json.dumps(obj, separators=(",", ":")).encode()
        return base64.urlsafe_b64encode(raw).rstrip(b"=").decode()

    header = seg({"alg": "none", "typ": "JWT"})
    payload = seg({
        "iss": f"https://securetoken.google.com/{PROJECT}",
        "aud": PROJECT,
        "sub": uid,
        "user_id": uid,
        "auth_time": 1000,
        "iat": 1000,
        "exp": 9999999999,
        "firebase": {"identities": {}, "sign_in_provider": sign_in_provider},
    })
    return f"{header}.{payload}."


def encode(value):
    if isinstance(value, bool):
        return {"booleanValue": value}
    if isinstance(value, int):
        return {"integerValue": str(value)}
    if isinstance(value, float):
        return {"doubleValue": value}
    if isinstance(value, list):
        return {"arrayValue": {"values": [encode(v) for v in value]}}
    if value is None:
        return {"nullValue": None}
    return {"stringValue": str(value)}


def request(method, path, uid=None, fields=None, token=None, mask=None,
            sign_in_provider="anonymous"):
    """Returns (status, body). 200 means the rules allowed it, 403 means they refused."""
    url = f"{BASE}/{path}"
    # A bare PATCH replaces the document, but the app's writes are nearly all partial
    # `update()` calls, where request.resource.data is the merge of the new fields over the
    # stored ones. An updateMask is the only way to reproduce that over REST, and the rules
    # treat the two very differently: a full replace drops the fields it omits, so
    # termsUnchanged() would refuse a write the real client makes happily.
    if mask:
        url = f"{url}?" + "&".join(f"updateMask.fieldPaths={f}" for f in mask)
    data = None
    if fields is not None:
        data = json.dumps({"fields": {k: encode(v) for k, v in fields.items()}}).encode()

    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    elif uid:
        req.add_header("Authorization",
                       f"Bearer {token_for(uid, sign_in_provider)}")
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, json.loads(resp.read() or b"{}")
    except urllib.error.HTTPError as e:
        return e.code, {}
    except urllib.error.URLError as e:
        print(f"cannot reach the emulator: {e}")
        sys.exit(2)


def allowed(label, method, path, uid, fields=None, mask=None, sign_in_provider="anonymous"):
    global checks
    checks += 1
    status, _ = request(method, path, uid, fields, mask=mask,
                        sign_in_provider=sign_in_provider)
    if status != 200:
        failures.append(f"{label}: expected ALLOW, rules returned {status}")
        print(f"  FAIL  {label} (expected allow, got {status})")
    else:
        print(f"  ok    {label}")


def denied(label, method, path, uid, fields=None, mask=None, sign_in_provider="anonymous"):
    global checks
    checks += 1
    status, _ = request(method, path, uid, fields, mask=mask,
                        sign_in_provider=sign_in_provider)
    if status == 200:
        failures.append(f"{label}: expected DENY, rules allowed it")
        print(f"  FAIL  {label} (expected deny, but it was allowed)")
    else:
        print(f"  ok    {label} (denied {status})")


PATIENT = "uid-patient"
OTHER = "uid-other-patient"
PROVIDER_OWNER = "uid-provider-owner"
ADMIN = "uid-admin"

PROVIDER_ID = "prov-1"
ORDER_ID = "order-1"

print("\nprofiles and roles")
allowed("patient creates own profile as PATIENT", "PATCH", f"users/{PATIENT}", PATIENT,
        {"phone": "+249912345678", "name": "سارة عثمان", "role": "PATIENT"})
allowed("provider owner creates own profile", "PATCH", f"users/{PROVIDER_OWNER}", PROVIDER_OWNER,
        {"phone": "+249912000000", "name": "محمد عبدالرحمن", "role": "PATIENT"})
allowed("second patient creates own profile", "PATCH", f"users/{OTHER}", OTHER,
        {"phone": "+249912111111", "name": "ضيف", "role": "PATIENT"})
denied("client cannot self-grant ADMIN", "PATCH", f"users/{ADMIN}", ADMIN,
       {"phone": "+249900000000", "name": "مشرف", "role": "ADMIN"})
denied("client cannot self-grant PROVIDER", "PATCH", f"users/{OTHER}-x", f"{OTHER}-x",
       {"phone": "+249912222222", "name": "متسلل", "role": "PROVIDER"})
denied("a patient cannot read another patient's profile", "GET", f"users/{PATIENT}", OTHER)

# The rules just proved a client cannot mint an ADMIN, which is the whole point — so the one
# admin the later checks act as has to be seeded the way a real one is, out of band. The
# emulator treats the literal "owner" bearer token as the project owner and skips rules.
status, _ = request("PATCH", f"users/{ADMIN}", token="owner",
                    fields={"phone": "+249900000000", "name": "مشرف", "role": "ADMIN"})
if status != 200:
    print(f"could not seed the admin profile out of band (got {status})")
    sys.exit(2)

print("\nprovider registration")
# A real provider signs up with email/Google, never a guest session — the rules demand a
# permanent account for exactly this write, so the harness acts as one.
allowed("provider self-registers into PENDING_REVIEW", "PATCH", f"providers/{PROVIDER_ID}",
        PROVIDER_OWNER,
        {"name": "محمد عبدالرحمن", "title": "فني مختبر", "ownerUid": PROVIDER_OWNER,
         "status": "PENDING_REVIEW", "serviceCategory": "LAB_DRAW", "priceSdg": 18000.0,
         "isAvailableNow": True, "isVerified": False, "ratingSum": 0.0, "ratingCount": 0,
         "rating": 0.0, "reviewsCount": 0},
        sign_in_provider="password")
denied("provider cannot self-activate", "PATCH", f"providers/{PROVIDER_ID}", PROVIDER_OWNER,
       {"name": "محمد عبدالرحمن", "ownerUid": PROVIDER_OWNER, "status": "ACTIVE"})
denied("a stranger cannot register a provider they do not own", "PATCH", "providers/prov-hijack",
       OTHER, {"name": "منتحل", "ownerUid": PROVIDER_OWNER, "status": "PENDING_REVIEW"})
allowed("any signed-in user can browse the catalogue", "GET", f"providers/{PROVIDER_ID}", PATIENT)

print("\nbooking")
order = {
    "orderNumber": "HM-4821", "serviceTitle": "سحب عينات منزلية",
    "patientName": "سارة عثمان", "patientPhone": "+249912345678",
    "providerName": "محمد عبدالرحمن", "providerId": PROVIDER_ID,
    "areaLocation": "ود مدني - حي الموردة", "visitDate": "الثلاثاء 11 أغسطس",
    "visitTime": "10:00 صباحاً", "priceSdg": 18000.0, "status": "ORDER_SENT",
    "patientUid": PATIENT, "isRated": False,
}
allowed("patient places an order", "PATCH", f"orders/{ORDER_ID}", PATIENT, order)
denied("nobody can place an order in someone else's name", "PATCH", "orders/order-forged", OTHER,
       dict(order, patientUid=PATIENT))
allowed("patient reads their own order", "GET", f"orders/{ORDER_ID}", PATIENT)
denied("another patient cannot read that order", "GET", f"orders/{ORDER_ID}", OTHER)
allowed("the assigned provider can read the order", "GET", f"orders/{ORDER_ID}", PROVIDER_OWNER)

print("\nreceipt image")
allowed("patient uploads a receipt", "PATCH", "images/img-1", PATIENT,
        {"ownerUid": PATIENT, "data": "iVBORw0KGgoAAAANSUhEUg=="})
denied("a stranger cannot read that receipt", "GET", "images/img-1", OTHER)
denied("nobody can upload an image owned by someone else", "PATCH", "images/img-forged", OTHER,
       {"ownerUid": PATIENT, "data": "iVBORw0KGgo="})
# The receipt hand-in itself waits until the provider has accepted: the rules (and the real
# journey) never let a patient push an ORDER_SENT booking straight into review.

print("\nprovider works the order")
denied("a patient cannot approve their own payment", "PATCH", f"orders/{ORDER_ID}", PATIENT,
       dict(order, status="PAYMENT_CONFIRMED"))
denied("a patient cannot mark their own visit complete", "PATCH", f"orders/{ORDER_ID}", PATIENT,
       dict(order, status="COMPLETED"))
denied("a provider cannot confirm they were paid", "PATCH", f"orders/{ORDER_ID}", PROVIDER_OWNER,
       dict(order, status="PAYMENT_CONFIRMED"))
denied("a patient cannot discount their own order", "PATCH", f"orders/{ORDER_ID}", PATIENT,
       dict(order, status="PAYMENT_UNDER_REVIEW", priceSdg=1.0))
denied("a provider cannot reassign an order to themselves", "PATCH", f"orders/{ORDER_ID}",
       PROVIDER_OWNER, dict(order, status="ACCEPTED_BY_PROVIDER", providerId="prov-other"))
allowed("provider accepts the order", "PATCH", f"orders/{ORDER_ID}", PROVIDER_OWNER,
        dict(order, status="ACCEPTED_BY_PROVIDER"))
allowed("patient attaches the receipt and moves to review", "PATCH", f"orders/{ORDER_ID}", PATIENT,
        dict(order, status="PAYMENT_UNDER_REVIEW", receiptImageUri="fsimg://img-1",
             transferSenderName="سارة عثمان", transferRefNum="9931204"))
allowed("admin confirms the transfer arrived", "PATCH", f"orders/{ORDER_ID}", ADMIN,
        dict(order, status="PAYMENT_CONFIRMED"))
denied("an unrelated user cannot touch the order", "PATCH", f"orders/{ORDER_ID}", OTHER,
       dict(order, status="CANCELLED"))
allowed("provider flips their own availability", "PATCH", f"providers/{PROVIDER_ID}",
        PROVIDER_OWNER,
        {"name": "محمد عبدالرحمن", "title": "فني مختبر", "ownerUid": PROVIDER_OWNER,
         "status": "PENDING_REVIEW", "serviceCategory": "LAB_DRAW", "priceSdg": 18000.0,
         "isAvailableNow": False, "isVerified": False, "ratingSum": 0.0, "ratingCount": 0,
         "rating": 0.0, "reviewsCount": 0})
# setProviderAvailability sends this one field and nothing else, so the owner branch has to
# hold with status carried over from the stored document rather than restated by the client.
allowed("the availability toggle works as the one-field write the app sends", "PATCH",
        f"providers/{PROVIDER_ID}", PROVIDER_OWNER, {"isAvailableNow": True},
        mask=["isAvailableNow"])
allowed("provider marks the visit complete", "PATCH", f"orders/{ORDER_ID}", PROVIDER_OWNER,
        dict(order, status="COMPLETED"))

print("\ncancellation")
CANCEL_ID = "order-cancel"
allowed("patient books an order they will later drop", "PATCH", f"orders/{CANCEL_ID}", PATIENT,
        dict(order, orderNumber="HM-4822"))
# cancelOrder writes the status alongside who cancelled and why, all in one partial update.
allowed("patient cancels with a reason", "PATCH", f"orders/{CANCEL_ID}", PATIENT,
        {"status": "CANCELLED", "cancelledBy": "BY_PATIENT", "cancelReason": "تغير الموعد"},
        mask=["status", "cancelledBy", "cancelReason"])
allowed("the assigned provider can also cancel", "PATCH", f"orders/{CANCEL_ID}", PROVIDER_OWNER,
        {"status": "CANCELLED", "cancelledBy": "BY_PROVIDER", "cancelReason": "ظرف طارئ"},
        mask=["status", "cancelledBy", "cancelReason"])
denied("an unrelated user cannot cancel someone else's order", "PATCH", f"orders/{CANCEL_ID}",
       OTHER, {"status": "CANCELLED", "cancelledBy": "BY_PATIENT", "cancelReason": "لا"},
       mask=["status", "cancelledBy", "cancelReason"])

print("\nrefund requests")
# Paid-but-not-yet-visited money needs a way out that is not silent self-cancellation. The
# patient flags it, only an admin resolves it, and no visit proceeds while it is pending.
REFUND_ID = "order-refund"
allowed("patient books an order they will seek a refund on", "PATCH", f"orders/{REFUND_ID}",
        PATIENT, dict(order, orderNumber="HM-4831"))
allowed("provider accepts the refund-candidate", "PATCH", f"orders/{REFUND_ID}", PROVIDER_OWNER,
        dict(order, orderNumber="HM-4831", status="ACCEPTED_BY_PROVIDER"))
allowed("patient pays the refund-candidate", "PATCH", f"orders/{REFUND_ID}", PATIENT,
        dict(order, orderNumber="HM-4831", status="PAYMENT_UNDER_REVIEW",
             receiptImageUri="fsimg://img-1", transferSenderName="سارة عثمان",
             transferRefNum="9931300"))
allowed("admin confirms the refund-candidate's payment", "PATCH", f"orders/{REFUND_ID}", ADMIN,
        dict(order, orderNumber="HM-4831", status="PAYMENT_CONFIRMED"))
denied("a paid patient cannot silently cancel instead of asking", "PATCH", f"orders/{REFUND_ID}",
       PATIENT, {"status": "CANCELLED"}, mask=["status"])
allowed("patient formally asks for their money back", "PATCH", f"orders/{REFUND_ID}", PATIENT,
        {"status": "REFUND_REQUESTED", "cancelReason": "لم أعد بحاجة للخدمة"},
        mask=["status", "cancelReason"])
denied("the visit cannot go ahead while a refund is pending", "PATCH", f"orders/{REFUND_ID}",
       PROVIDER_OWNER, dict(dict(order, orderNumber="HM-4831"), status="COMPLETED"),
       mask=["status"])
denied("the patient cannot resolve their own refund request", "PATCH", f"orders/{REFUND_ID}",
       PATIENT, {"status": "CANCELLED"}, mask=["status"])
allowed("admin declines: the booking resumes as paid", "PATCH", f"orders/{REFUND_ID}", ADMIN,
        {"status": "PAYMENT_CONFIRMED"}, mask=["status"])
allowed("the patient may ask again", "PATCH", f"orders/{REFUND_ID}", PATIENT,
        {"status": "REFUND_REQUESTED"}, mask=["status"])
allowed("admin grants: order cancels with who and why", "PATCH", f"orders/{REFUND_ID}", ADMIN,
        {"status": "CANCELLED", "cancelledBy": "ADMIN",
         "cancelReason": "قبول طلب استرجاع المريض"},
        mask=["status", "cancelledBy", "cancelReason"])

print("\nnotifications")
allowed("provider notifies the patient", "PATCH", f"users/{PATIENT}/notifications/n1",
        PROVIDER_OWNER,
        {"type": "ORDER_ACCEPTED", "title": "تم قبول طلبك", "body": "الزيارة مؤكدة",
         "orderId": ORDER_ID, "read": False})
allowed("patient reads their notifications", "GET", f"users/{PATIENT}/notifications/n1", PATIENT)
denied("a stranger cannot read the patient's notifications", "GET",
       f"users/{PATIENT}/notifications/n1", OTHER)

print("\nrating")
allowed("patient rates the completed visit", "PATCH", f"ratings/{ORDER_ID}", PATIENT,
        {"orderId": ORDER_ID, "providerId": PROVIDER_ID, "patientUid": PATIENT, "stars": 5,
         "chips": ["الالتزام بالموعد", "الاحترافية"], "comment": "خدمة ممتازة"})
# submitRating writes three documents in one transaction, and a single refusal rolls back all
# of them. The rating doc and the provider aggregate are checked here and below; this is the
# third write, flagging the order, which the patient makes on an order already COMPLETED — a
# status they have no route to themselves.
allowed("the rating transaction may flag the order as rated", "PATCH", f"orders/{ORDER_ID}",
        PATIENT, {"isRated": True}, mask=["isRated"])
denied("a partial write cannot sneak the order to confirmed", "PATCH", f"orders/{ORDER_ID}",
       PATIENT, {"status": "PAYMENT_CONFIRMED"}, mask=["status"])
denied("a partial write cannot cut the price", "PATCH", f"orders/{ORDER_ID}", PATIENT,
       {"priceSdg": 1.0}, mask=["priceSdg"])
denied("the same order cannot be rated twice", "PATCH", f"ratings/{ORDER_ID}", PATIENT,
       {"orderId": ORDER_ID, "providerId": PROVIDER_ID, "patientUid": PATIENT, "stars": 1})
denied("a rating cannot be filed in someone else's name", "PATCH", "ratings/order-2", OTHER,
       {"orderId": "order-2", "providerId": PROVIDER_ID, "patientUid": PATIENT, "stars": 5})
denied("stars outside 1-5 are refused", "PATCH", "ratings/order-3", PATIENT,
       {"orderId": "order-3", "providerId": PROVIDER_ID, "patientUid": PATIENT, "stars": 9})

print("\nrating aggregate")
# The rating transaction updates these four fields and touches nothing else, which is exactly
# what the rules key on, so these go through an updateMask too. Writing the whole document
# instead would make the checks depend on every unrelated field still holding the value it had
# earlier in this script, and a stale one would read as a rules failure.
AGG = ["ratingSum", "ratingCount", "rating", "reviewsCount"]
allowed("a patient may add exactly one review to the aggregate", "PATCH",
        f"providers/{PROVIDER_ID}", PATIENT,
        {"ratingSum": 5.0, "ratingCount": 1, "rating": 5.0, "reviewsCount": 1}, mask=AGG)
denied("the aggregate cannot be inflated by more than one review", "PATCH",
       f"providers/{PROVIDER_ID}", PATIENT,
       {"ratingSum": 500.0, "ratingCount": 100, "rating": 5.0, "reviewsCount": 100}, mask=AGG)
denied("a rating write cannot smuggle in a price change", "PATCH", f"providers/{PROVIDER_ID}",
       PATIENT,
       {"priceSdg": 1.0, "ratingSum": 10.0, "ratingCount": 2, "rating": 5.0, "reviewsCount": 2},
       mask=AGG + ["priceSdg"])
denied("a rating write cannot smuggle in verification", "PATCH", f"providers/{PROVIDER_ID}",
       PATIENT,
       {"isVerified": True, "ratingSum": 10.0, "ratingCount": 2, "rating": 5.0,
        "reviewsCount": 2},
       mask=AGG + ["isVerified"])

print("\npayouts ledger")
# The ledger entry is keyed BY the order id and only exists once the visit is COMPLETED.
PAYOUT = {
    "orderId": ORDER_ID, "orderNumber": "HM-4821", "providerId": PROVIDER_ID,
    "providerName": "محمد عبدالرحمن", "patientName": "سارة عثمان",
    "amountSdg": 15300.0, "status": "ACCRUED", "createdAtTimestamp": 1000,
}
allowed("provider accrues earnings for their completed visit", "PATCH",
        f"payouts/{ORDER_ID}", PROVIDER_OWNER, PAYOUT)
allowed("the owning provider can read their payout", "GET", f"payouts/{ORDER_ID}", PROVIDER_OWNER)
denied("another patient cannot read someone's payout", "GET", f"payouts/{ORDER_ID}", OTHER)
denied("a patient cannot accrue earnings on an order served by someone else", "PATCH",
       f"payouts/order-forged-payout", PATIENT, dict(PAYOUT, orderId="order-forged-payout"))
denied("a stranger cannot accrue against somebody else's provider id", "PATCH",
       "payouts/order-stranger-payout", OTHER,
       dict(PAYOUT, orderId="order-stranger-payout"))
denied("a payout may not be keyed apart from its orderId", "PATCH", "payouts/mismatched-id",
       PROVIDER_OWNER, dict(PAYOUT))
denied("a zero or negative payout is refused", "PATCH", "payouts/zero-accrual",
       PROVIDER_OWNER, dict(PAYOUT, amountSdg=0.0))
denied("re-accruing means overwriting an existing doc, which updates are admin-only", "PATCH",
       f"payouts/{ORDER_ID}", PROVIDER_OWNER, PAYOUT)
denied("a provider can never pay themselves out", "PATCH", f"payouts/{ORDER_ID}",
       PROVIDER_OWNER, {"status": "PAID", "paidAtTimestamp": 2000},
       mask=["status", "paidAtTimestamp"])
denied("an admin marking paid cannot smuggle an amount change along", "PATCH",
       f"payouts/{ORDER_ID}", ADMIN,
       {"status": "PAID", "paidAtTimestamp": 2000, "amountSdg": 999999.0},
       mask=["status", "paidAtTimestamp", "amountSdg"])
allowed("admin records the Bankak transfer as paid", "PATCH", f"payouts/{ORDER_ID}", ADMIN,
        {"status": "PAID", "paidAtTimestamp": 2000}, mask=["status", "paidAtTimestamp"])
denied("a PAID payout is frozen — no unpaying, no re-marking", "PATCH", f"payouts/{ORDER_ID}",
       ADMIN, {"status": "ACCRUED"}, mask=["status"])
denied("payout documents are never deletable", "DELETE", f"payouts/{ORDER_ID}", ADMIN)

print("\nadmin oversight")
allowed("admin reads any order", "GET", f"orders/{ORDER_ID}", ADMIN)
allowed("admin reads a patient's profile", "GET", f"users/{PATIENT}", ADMIN)
allowed("admin activates a pending provider", "PATCH", f"providers/{PROVIDER_ID}", ADMIN,
        {"status": "ACTIVE", "isVerified": True}, mask=["status", "isVerified"])
denied("a patient cannot list every order", "GET", "orders", PATIENT)

print("\nprovider application")
# registerProvider links the application to the applicant by writing this one field on their own
# user doc. The role has to survive untouched or the users rule refuses the write.
allowed("applicant links the application to their own profile", "PATCH",
        f"users/{PROVIDER_OWNER}", PROVIDER_OWNER, {"providerId": PROVIDER_ID},
        mask=["providerId"])
denied("nobody can link an application onto another user's profile", "PATCH",
       f"users/{PROVIDER_OWNER}", OTHER, {"providerId": PROVIDER_ID}, mask=["providerId"])
# attachDocuments writes nested docs.* paths, which affectedKeys() reports as the top-level
# `docs` key — so the owner branch carries it, not the rating allowlist.
allowed("applicant attaches their credentials", "PATCH", f"providers/{PROVIDER_ID}",
        PROVIDER_OWNER, {"docs": {"nationalId": "fsimg://img-1"}}, mask=["docs"])
denied("a stranger cannot attach documents to someone else's application", "PATCH",
       f"providers/{PROVIDER_ID}", OTHER, {"docs": {"nationalId": "fsimg://img-1"}},
       mask=["docs"])
# The other half of approveProvider: without this the provider would go ACTIVE while their
# owner stayed a PATIENT, so they would never reach the provider dashboard.
allowed("admin promotes the approved applicant to PROVIDER", "PATCH",
        f"users/{PROVIDER_OWNER}", ADMIN, {"role": "PROVIDER"}, mask=["role"])
denied("the applicant cannot promote themselves once approved", "PATCH",
       f"users/{PATIENT}", PATIENT, {"role": "PROVIDER"}, mask=["role"])

print("\nunsigned access")
denied("an unauthenticated client cannot read the catalogue", "GET", f"providers/{PROVIDER_ID}",
       None)
denied("an unauthenticated client cannot read orders", "GET", f"orders/{ORDER_ID}", None)
denied("collections outside the schema stay closed", "PATCH", "secrets/s1", PATIENT, {"a": "b"})

print(f"\n{checks - len(failures)}/{checks} rule checks passed")
if failures:
    print("\nfailures:")
    for f in failures:
        print(f"  - {f}")
    sys.exit(1)
print("firestore.rules permits the full booking lifecycle and refuses every probe above")
