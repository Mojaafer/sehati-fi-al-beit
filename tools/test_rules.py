"""Evaluate firestore.rules against the exact accesses the app performs.

Security rules gate every step of the booking journey, and a rule that is merely
plausible will reject a real client at runtime. Google's Rules test API evaluates the
ruleset server-side, so this checks the real file without an emulator or a device.

Usage: python tools/test_rules.py
"""
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

PROJECT = "sehati-home-care"
RULES = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "firestore.rules")
DOCS = "/databases/(default)/documents"

PATIENT = "patient-uid"
OTHER = "other-uid"
ADMIN = "admin-uid"
OWNER = "provider-owner-uid"
PROVIDER_ID = "provider-1"
ORDER_ID = "order-1"


def token():
    cfg = json.load(open(os.path.expanduser("~/.config/configstore/firebase-tools.json")))
    body = urllib.parse.urlencode({
        "client_id": "563584335869-fgrhgmd47bqnekij5i8b5pr03ho849e6.apps.googleusercontent.com",
        "client_secret": "j9iVZfS8kkCEFUPaAeJV0sAi",
        "refresh_token": cfg["tokens"]["refresh_token"],
        "grant_type": "refresh_token",
    }).encode()
    req = urllib.request.Request("https://oauth2.googleapis.com/token", data=body)
    return json.load(urllib.request.urlopen(req))["access_token"]


def user_doc(uid, role):
    """isAdmin() reads the caller's user doc, so every case must mock it."""
    return [
        {"function": "exists",
         "args": [{"exactValue": f"{DOCS}/users/{uid}"}],
         "result": {"value": True}},
        {"function": "get",
         "args": [{"exactValue": f"{DOCS}/users/{uid}"}],
         "result": {"value": {"data": {"role": role}}}},
    ]


def provider_doc(owner_uid):
    """ownsProvider() reads the provider doc named by the order."""
    return [
        {"function": "exists",
         "args": [{"exactValue": f"{DOCS}/providers/{PROVIDER_ID}"}],
         "result": {"value": True}},
        {"function": "get",
         "args": [{"exactValue": f"{DOCS}/providers/{PROVIDER_ID}"}],
         "result": {"value": {"data": {"ownerUid": owner_uid}}}},
    ]


def case(name, expect, path, method, uid=None, role="PATIENT",
         data=None, resource=None, mocks=None, provider="phone"):
    request = {"path": DOCS + path, "method": method}
    if uid:
        # Real Firebase tokens always carry firebase.sign_in_provider; hasPhoneAccount()
        # reads it to tell a guest session from an account with a number behind it.
        request["auth"] = {
            "uid": uid,
            "token": {"sub": uid, "firebase": {"sign_in_provider": provider}},
        }
    if data is not None:
        request["resource"] = {"data": data}
    test = {"expectation": expect, "request": request}
    if resource is not None:
        test["resource"] = {"data": resource}
    fn = list(mocks or [])
    if uid:
        fn += user_doc(uid, role)
    if fn:
        test["functionMocks"] = fn
    return name, test


ORDER = {
    "orderNumber": "HM-1042", "patientUid": PATIENT, "providerId": PROVIDER_ID,
    "status": "ORDER_SENT", "priceSdg": 15000.0,
}
PROVIDER = {
    "name": "n", "ownerUid": OWNER, "status": "ACTIVE",
    "ratingSum": 10.0, "ratingCount": 2, "rating": 5.0, "reviewsCount": 2,
}

CASES = [
    # --- users -------------------------------------------------------------
    case("patient creates own profile as PATIENT", "ALLOW",
         f"/users/{PATIENT}", "create", PATIENT, data={"role": "PATIENT"}),
    case("patient cannot self-promote to ADMIN", "DENY",
         f"/users/{PATIENT}", "create", PATIENT, data={"role": "ADMIN"}),
    case("patient reads own profile", "ALLOW", f"/users/{PATIENT}", "get", PATIENT),
    case("patient cannot read another profile", "DENY", f"/users/{OTHER}", "get", PATIENT),
    case("patient cannot escalate own role by update", "DENY",
         f"/users/{PATIENT}", "update", PATIENT,
         data={"role": "ADMIN"}, resource={"role": "PATIENT"}),
    case("patient may edit own profile keeping role", "ALLOW",
         f"/users/{PATIENT}", "update", PATIENT,
         data={"role": "PATIENT", "name": "new"}, resource={"role": "PATIENT"}),

    # --- providers ---------------------------------------------------------
    case("signed-in user reads the catalogue", "ALLOW",
         f"/providers/{PROVIDER_ID}", "get", PATIENT),
    case("anonymous user cannot read the catalogue", "DENY",
         f"/providers/{PROVIDER_ID}", "get"),
    case("provider self-registers as PENDING_REVIEW", "ALLOW",
         "/providers/new", "create", OWNER,
         data={"ownerUid": OWNER, "status": "PENDING_REVIEW"}),
    case("provider cannot self-activate", "DENY",
         "/providers/new", "create", OWNER,
         data={"ownerUid": OWNER, "status": "ACTIVE"}),
    # An admin reviews an application and returns a decision to the applicant, so it has to
    # belong to an account that can be signed back into. A guest session cannot be.
    case("guest cannot file a provider application", "DENY",
         "/providers/new", "create", OWNER, provider="anonymous",
         data={"ownerUid": OWNER, "status": "PENDING_REVIEW"}),
    # Sudanese numbers cannot receive SMS, so Google and email are the routes that actually work
    # here. The gate is "not a guest", not "has a phone number" — these prove it.
    case("google account files a provider application", "ALLOW",
         "/providers/new", "create", OWNER, provider="google.com",
         data={"ownerUid": OWNER, "status": "PENDING_REVIEW"}),
    case("email account files a provider application", "ALLOW",
         "/providers/new", "create", OWNER, provider="password",
         data={"ownerUid": OWNER, "status": "PENDING_REVIEW"}),
    case("guest still browses the catalogue", "ALLOW",
         f"/providers/{PROVIDER_ID}", "get", PATIENT, provider="anonymous"),
    case("admin activates a pending provider", "ALLOW",
         f"/providers/{PROVIDER_ID}", "update", ADMIN, role="ADMIN",
         data=dict(PROVIDER, status="ACTIVE"),
         resource=dict(PROVIDER, status="PENDING_REVIEW")),
    # The rating transaction is a patient writing to a doc they do not own.
    case("patient folds a 5-star rating into the aggregate", "ALLOW",
         f"/providers/{PROVIDER_ID}", "update", PATIENT,
         data=dict(PROVIDER, ratingSum=15.0, ratingCount=3, rating=5.0, reviewsCount=3),
         resource=PROVIDER),
    case("patient folds a 1-star rating into the aggregate", "ALLOW",
         f"/providers/{PROVIDER_ID}", "update", PATIENT,
         data=dict(PROVIDER, ratingSum=11.0, ratingCount=3, rating=3.67, reviewsCount=3),
         resource=PROVIDER),
    case("patient cannot inflate the aggregate beyond 5 stars", "DENY",
         f"/providers/{PROVIDER_ID}", "update", PATIENT,
         data=dict(PROVIDER, ratingSum=99.0, ratingCount=3, rating=5.0, reviewsCount=3),
         resource=PROVIDER),
    case("patient cannot rename a provider while rating it", "DENY",
         f"/providers/{PROVIDER_ID}", "update", PATIENT,
         data=dict(PROVIDER, name="hacked", ratingSum=15.0, ratingCount=3),
         resource=PROVIDER),
    case("patient cannot edit a provider outright", "DENY",
         f"/providers/{PROVIDER_ID}", "update", PATIENT,
         data=dict(PROVIDER, name="hacked"), resource=PROVIDER),

    # --- orders ------------------------------------------------------------
    case("patient books in their own name", "ALLOW",
         f"/orders/{ORDER_ID}", "create", PATIENT, data=ORDER),
    case("patient cannot book in someone else's name", "DENY",
         f"/orders/{ORDER_ID}", "create", PATIENT, data=dict(ORDER, patientUid=OTHER)),
    case("patient reads their own order", "ALLOW",
         f"/orders/{ORDER_ID}", "get", PATIENT, resource=ORDER,
         mocks=provider_doc(OWNER)),
    case("stranger cannot read someone else's order", "DENY",
         f"/orders/{ORDER_ID}", "get", OTHER, resource=ORDER,
         mocks=provider_doc(OWNER)),
    case("provider owner reads an order for their listing", "ALLOW",
         f"/orders/{ORDER_ID}", "get", OWNER, resource=ORDER,
         mocks=provider_doc(OWNER)),
    case("admin reads any order", "ALLOW",
         f"/orders/{ORDER_ID}", "get", ADMIN, role="ADMIN", resource=ORDER,
         mocks=provider_doc(OWNER)),
    case("provider owner accepts the order", "ALLOW",
         f"/orders/{ORDER_ID}", "update", OWNER,
         data=dict(ORDER, status="ACCEPTED_BY_PROVIDER"), resource=ORDER,
         mocks=provider_doc(OWNER)),
    case("admin confirms the payment", "ALLOW",
         f"/orders/{ORDER_ID}", "update", ADMIN, role="ADMIN",
         data=dict(ORDER, status="PAYMENT_CONFIRMED"), resource=ORDER,
         mocks=provider_doc(OWNER)),
    case("patient flags their own order as rated", "ALLOW",
         f"/orders/{ORDER_ID}", "update", PATIENT,
         data=dict(ORDER, isRated=True), resource=ORDER,
         mocks=provider_doc(OWNER)),
    case("stranger cannot change an order's status", "DENY",
         f"/orders/{ORDER_ID}", "update", OTHER,
         data=dict(ORDER, status="PAYMENT_CONFIRMED"), resource=ORDER,
         mocks=provider_doc(OWNER)),

    # --- ratings -----------------------------------------------------------
    case("patient submits a rating", "ALLOW",
         f"/ratings/{ORDER_ID}", "create", PATIENT,
         data={"patientUid": PATIENT, "providerId": PROVIDER_ID, "stars": 5}),
    case("rating stars above 5 are rejected", "DENY",
         f"/ratings/{ORDER_ID}", "create", PATIENT,
         data={"patientUid": PATIENT, "providerId": PROVIDER_ID, "stars": 6}),
    case("rating stars below 1 are rejected", "DENY",
         f"/ratings/{ORDER_ID}", "create", PATIENT,
         data={"patientUid": PATIENT, "providerId": PROVIDER_ID, "stars": 0}),
    case("a rating cannot be submitted for another patient", "DENY",
         f"/ratings/{ORDER_ID}", "create", PATIENT,
         data={"patientUid": OTHER, "providerId": PROVIDER_ID, "stars": 5}),
    case("an order can only be rated once", "DENY",
         f"/ratings/{ORDER_ID}", "update", PATIENT,
         data={"patientUid": PATIENT, "stars": 5},
         resource={"patientUid": PATIENT, "stars": 4}),
    case("anyone signed in reads reviews", "ALLOW",
         f"/ratings/{ORDER_ID}", "get", PATIENT),

    # --- images (receipts and provider documents) ---------------------------
    case("patient uploads their own receipt", "ALLOW",
         "/images/img1", "create", PATIENT,
         data={"ownerUid": PATIENT, "data": "x" * 100}),
    case("patient cannot upload under another owner", "DENY",
         "/images/img1", "create", PATIENT,
         data={"ownerUid": OTHER, "data": "x" * 100}),
    case("an oversized image is rejected", "DENY",
         "/images/img1", "create", PATIENT,
         data={"ownerUid": PATIENT, "data": "x" * 900001}),
    case("patient reads their own receipt", "ALLOW",
         "/images/img1", "get", PATIENT, resource={"ownerUid": PATIENT}),
    case("admin reviews any receipt", "ALLOW",
         "/images/img1", "get", ADMIN, role="ADMIN", resource={"ownerUid": PATIENT}),
    case("stranger cannot read a receipt", "DENY",
         "/images/img1", "get", OTHER, resource={"ownerUid": PATIENT}),
    case("an uploaded image cannot be rewritten", "DENY",
         "/images/img1", "update", PATIENT,
         data={"ownerUid": PATIENT, "data": "y"}, resource={"ownerUid": PATIENT}),

    # --- notifications ------------------------------------------------------
    case("an actor pushes a notification to the patient's inbox", "ALLOW",
         f"/users/{PATIENT}/notifications/n1", "create", OWNER,
         data={"title": "t", "body": "b"}),
    case("patient reads their own inbox", "ALLOW",
         f"/users/{PATIENT}/notifications/n1", "get", PATIENT),
    case("stranger cannot read another inbox", "DENY",
         f"/users/{PATIENT}/notifications/n1", "get", OTHER),

    # --- shared admin inbox --------------------------------------------------
    # A provider filing an application cannot read users/{uid} to find an admin's uid, so it
    # cannot address a per-user notification to one. It drops the alert here instead.
    case("an applicant alerts the admins without knowing who they are", "ALLOW",
         "/adminNotifications/n1", "create", OWNER,
         data={"title": "t", "body": "b"}),
    case("admin reads the shared inbox", "ALLOW",
         "/adminNotifications/n1", "get", ADMIN, role="ADMIN"),
    case("admin marks a shared alert as read", "ALLOW",
         "/adminNotifications/n1", "update", ADMIN, role="ADMIN",
         data={"title": "t", "read": True}, resource={"title": "t", "read": False}),
    # The inbox names who applied and what they are waiting on, so it must not leak.
    case("patient cannot read the admin inbox", "DENY",
         "/adminNotifications/n1", "get", PATIENT),
    case("provider owner cannot read the admin inbox", "DENY",
         "/adminNotifications/n1", "get", OWNER),
    case("guest cannot read the admin inbox", "DENY",
         "/adminNotifications/n1", "get", PATIENT, provider="anonymous"),
    case("a signed-out client cannot post to the admin inbox", "DENY",
         "/adminNotifications/n1", "create", data={"title": "t"}),

    # --- catch-all ----------------------------------------------------------
    case("undeclared collections stay locked", "DENY", "/secrets/s1", "get", PATIENT),
]


def main():
    names = [n for n, _ in CASES]
    body = json.dumps({
        "source": {"files": [{"name": "firestore.rules", "content": open(RULES, encoding="utf-8").read()}]},
        "testSuite": {"testCases": [t for _, t in CASES]},
    }).encode()

    req = urllib.request.Request(
        f"https://firebaserules.googleapis.com/v1/projects/{PROJECT}:test",
        data=body, headers={"Authorization": "Bearer " + token(), "Content-Type": "application/json"})
    try:
        result = json.load(urllib.request.urlopen(req))
    except urllib.error.HTTPError as e:
        print("HTTP", e.code, e.read().decode()[:1500])
        return 1

    if result.get("issues"):
        for issue in result["issues"]:
            print("RULES ISSUE:", issue)
        return 1

    failed = 0
    for name, res in zip(names, result.get("testResults", [])):
        state = res.get("state")
        if state == "SUCCESS":
            print(f"  ok   {name}")
        else:
            failed += 1
            errs = "; ".join(str(e) for e in res.get("errorPosition", []) or []) or state
            print(f"  FAIL {name}  [{errs}]")

    print(f"\n{len(names) - failed}/{len(names)} rules cases passed")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
