"""Admin helper for the live sehati-home-care project.

Reuses the local Firebase CLI login (no service-account key on disk) to call the
Identity Toolkit and Firestore REST APIs. Credentials are never printed.

Usage:
    python tools/fb_admin.py seed-test-roles [providerDocId]
    python tools/fb_admin.py list-users
    python tools/fb_admin.py create-phone-user +249911111111
    python tools/fb_admin.py set-role <uid> PATIENT|PROVIDER|ADMIN
    python tools/fb_admin.py get-user-doc <uid>
    python tools/fb_admin.py set-test-phones +249911111111=111111 ...
    python tools/fb_admin.py sms-regions            # show the current policy
    python tools/fb_admin.py allow-sms-regions SD   # allowlist these regions only
"""

import json
import os
import sys
import time
import urllib.parse
import urllib.request

PROJECT = "sehati-home-care"
CONFIGSTORE = os.path.expanduser("~/.config/configstore/firebase-tools.json")
# Public installed-app client shipped with firebase-tools.
CLIENT_ID = "563584335869-fgrhgmd47bqnekij5i8b5pr03ho849e6.apps.googleusercontent.com"
CLIENT_SECRET = "j9iVZfS8kkCEFUPaAeJV0sAi"

FIRESTORE = "https://firestore.googleapis.com/v1"
IDENTITY = "https://identitytoolkit.googleapis.com"

# Every name in this project is Arabic, and a Windows console defaults to cp1252, which
# cannot encode it — printing a user doc would die on the name instead of showing the role.
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")


def _request(url, token, method="GET", body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Authorization", "Bearer " + token)
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req) as resp:
            raw = resp.read().decode()
    except urllib.error.HTTPError as exc:
        raise SystemExit("%s %s\n%s" % (exc.code, exc.reason, exc.read().decode()))
    return json.loads(raw) if raw else {}


def access_token():
    with open(CONFIGSTORE, encoding="utf-8") as handle:
        tokens = json.load(handle)["tokens"]
    if tokens.get("access_token") and tokens.get("expires_at", 0) > time.time() * 1000 + 60000:
        return tokens["access_token"]
    payload = urllib.parse.urlencode({
        "refresh_token": tokens["refresh_token"],
        "client_id": CLIENT_ID,
        "client_secret": CLIENT_SECRET,
        "grant_type": "refresh_token",
    }).encode()
    req = urllib.request.Request("https://oauth2.googleapis.com/token", data=payload)
    with urllib.request.urlopen(req) as resp:
        return json.load(resp)["access_token"]


def doc_path(collection, doc_id):
    return "%s/projects/%s/databases/(default)/documents/%s/%s" % (
        FIRESTORE, PROJECT, collection, doc_id)


def cmd_list_users(token, _args):
    out = _request("%s/v1/projects/%s/accounts:query" % (IDENTITY, PROJECT), token,
                   "POST", {"returnUserInfo": True})
    for user in out.get("userInfo", []):
        print("%s  phone=%s  anon=%s" % (
            user["localId"], user.get("phoneNumber", "-"),
            not (user.get("phoneNumber") or user.get("email"))))


def cmd_create_phone_user(token, args):
    out = _request("%s/v1/projects/%s/accounts" % (IDENTITY, PROJECT), token,
                   "POST", {"phoneNumber": args[0]})
    print(out["localId"])


def cmd_set_role(token, args):
    uid, role = args[0], args[1].upper()
    if role not in ("PATIENT", "PROVIDER", "ADMIN"):
        raise SystemExit("role must be PATIENT, PROVIDER or ADMIN")
    url = doc_path("users", uid) + "?" + urllib.parse.urlencode(
        [("updateMask.fieldPaths", "role")])
    _request(url, token, "PATCH", {"fields": {"role": {"stringValue": role}}})
    print("users/%s role=%s" % (uid, role))


def cmd_get_user_doc(token, args):
    out = _request(doc_path("users", args[0]), token)
    for key, value in sorted(out.get("fields", {}).items()):
        print("%s = %s" % (key, list(value.values())[0]))


def cmd_set_test_phones(token, args):
    numbers = dict(pair.split("=", 1) for pair in args)
    _request(_config_url("signIn.phoneNumber.testPhoneNumbers"), token, "PATCH",
             {"signIn": {"phoneNumber": {"testPhoneNumbers": numbers}}})
    for number in numbers:
        print("test number registered: %s" % number)


def _config_url(mask):
    return "%s/admin/v2/projects/%s/config?%s" % (
        IDENTITY, PROJECT, urllib.parse.urlencode({"updateMask": mask}))


def cmd_sms_regions(token, _args):
    out = _request("%s/admin/v2/projects/%s/config" % (IDENTITY, PROJECT), token)
    print(json.dumps(out.get("smsRegionConfig", {}), ensure_ascii=False))


def cmd_allow_sms_regions(token, args):
    """An empty allowlist blocks every region, test numbers included (error 17006)."""
    regions = [region.upper() for region in args] or ["SD"]
    _request(_config_url("smsRegionConfig"), token, "PATCH",
             {"smsRegionConfig": {"allowlistOnly": {"allowedRegions": regions}}})
    print("SMS allowed in: %s" % ", ".join(regions))


"""Phone number -> (fixed OTP, role, display name). Sudanese subscriber numbers only:
SudanPhoneNumber.normalize accepts +249 followed by a 9-digit number starting 1 or 9."""

TEST_ROLE_ACCOUNTS = [
    ("+249911000001", "110001", "PATIENT", "مريض تجريبي"),
    ("+249911000002", "110002", "PROVIDER", "مقدم خدمة تجريبي"),
    ("+249911000003", "110003", "ADMIN", "مدير تجريبي"),
]


def _uid_for_phone(token, phone):
    out = _request("%s/v1/projects/%s/accounts:lookup" % (IDENTITY, PROJECT), token,
                   "POST", {"phoneNumber": [phone]})
    users = out.get("users", [])
    if users:
        return users[0]["localId"]
    created = _request("%s/v1/projects/%s/accounts" % (IDENTITY, PROJECT), token,
                       "POST", {"phoneNumber": phone})
    return created["localId"]


def _write_user_doc(token, uid, fields):
    mask = [("updateMask.fieldPaths", key) for key in fields]
    url = doc_path("users", uid) + "?" + urllib.parse.urlencode(mask)
    body = {"fields": {k: {"stringValue": v} for k, v in fields.items()}}
    _request(url, token, "PATCH", body)


def cmd_seed_test_roles(token, args):
    """Creates one signed-in-able account per role. Safe to re-run."""
    provider_doc = args[0] if args else None
    cmd_set_test_phones(token, ["%s=%s" % (p, c) for p, c, _, _ in TEST_ROLE_ACCOUNTS])
    for phone, code, role, name in TEST_ROLE_ACCOUNTS:
        uid = _uid_for_phone(token, phone)
        fields = {"phone": phone, "name": name, "role": role}
        if role == "PROVIDER" and provider_doc:
            fields["providerId"] = provider_doc
            purl = doc_path("providers", provider_doc) + "?" + urllib.parse.urlencode(
                [("updateMask.fieldPaths", "ownerUid"), ("updateMask.fieldPaths", "status")])
            _request(purl, token, "PATCH", {"fields": {
                "ownerUid": {"stringValue": uid},
                "status": {"stringValue": "ACTIVE"}}})
        _write_user_doc(token, uid, fields)
        local = "0" + phone.removeprefix("+249")
        print("%-8s login %s  otp %s  uid %s" % (role, local, code, uid))


COMMANDS = {
    "seed-test-roles": cmd_seed_test_roles,
    "list-users": cmd_list_users,
    "create-phone-user": cmd_create_phone_user,
    "set-role": cmd_set_role,
    "get-user-doc": cmd_get_user_doc,
    "set-test-phones": cmd_set_test_phones,
    "sms-regions": cmd_sms_regions,
    "allow-sms-regions": cmd_allow_sms_regions,
}

if __name__ == "__main__":
    if len(sys.argv) < 2 or sys.argv[1] not in COMMANDS:
        raise SystemExit(__doc__)
    COMMANDS[sys.argv[1]](access_token(), sys.argv[2:])
