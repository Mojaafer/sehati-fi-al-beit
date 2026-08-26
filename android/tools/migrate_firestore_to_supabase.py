"""Migrate legacy Firestore documents into Supabase tables.

The script is dry-run by default. It reads Firebase CLI credentials from the same
config store used by fb_admin.py and requires SUPABASE_SERVICE_ROLE_KEY only when
--apply is supplied. Never ship that key in the Android app or commit it.

Examples:
    python tools/migrate_firestore_to_supabase.py
    $env:SUPABASE_SERVICE_ROLE_KEY = "..."
    python tools/migrate_firestore_to_supabase.py --apply
"""

import argparse
import datetime
import json
import os
import sys
import time
import urllib.parse
import urllib.request

from fb_admin import FIRESTORE, PROJECT, access_token, doc_path

SUPABASE_URL = "https://wolngyvenfyuaigjxajs.supabase.co"
COLLECTIONS = (
    "users",
    "providers",
    "orders",
    "payouts",
    "ratings",
    "notifications",
    "admin_notifications",
    "images",
)


def request_json(url, token, method="GET", body=None):
    payload = json.dumps(body).encode() if body is not None else None
    request = urllib.request.Request(url, data=payload, method=method)
    request.add_header("Authorization", "Bearer " + token)
    request.add_header("Content-Type", "application/json")
    request.add_header("Accept", "application/json")
    try:
        with urllib.request.urlopen(request) as response:
            raw = response.read().decode()
    except urllib.error.HTTPError as exc:
        raise SystemExit("%s %s\n%s" % (exc.code, exc.reason, exc.read().decode()))
    return json.loads(raw) if raw else {}


def firestore_value(value):
    if "stringValue" in value:
        return value["stringValue"]
    if "integerValue" in value:
        return int(value["integerValue"])
    if "doubleValue" in value:
        return float(value["doubleValue"])
    if "booleanValue" in value:
        return value["booleanValue"]
    if "timestampValue" in value:
        return value["timestampValue"]
    if "nullValue" in value:
        return None
    if "arrayValue" in value:
        return [firestore_value(item) for item in value["arrayValue"].get("values", [])]
    if "mapValue" in value:
        return firestore_fields(value["mapValue"].get("fields", {}))
    return None


def firestore_fields(fields):
    return {key: firestore_value(value) for key, value in fields.items()}


def normalize_timestamps(row):
    for key, value in list(row.items()):
        if key.endswith("_timestamp") and isinstance(value, str):
            try:
                parsed = datetime.datetime.fromisoformat(value.replace("Z", "+00:00"))
                row[key] = int(parsed.timestamp() * 1000)
            except ValueError:
                pass
        elif (key.endswith("_sdg") or key.endswith("_timestamp")) and isinstance(value, float):
            if value.is_integer():
                row[key] = int(value)
    return row


def fetch_collection(token, collection):
    url = "%s/projects/%s/databases/(default)/documents/%s" % (FIRESTORE, PROJECT, collection)
    documents = []
    while url:
        page = request_json(url, token)
        documents.extend(page.get("documents", []))
        url = page.get("nextPageToken")
        if url:
            url = "%s?pageToken=%s" % (
                "%s/projects/%s/databases/(default)/documents/%s" % (FIRESTORE, PROJECT, collection),
                urllib.parse.quote(url),
            )
    return documents


def fetch_subcollection(token, parent_collection, parent_id, subcollection):
    path = "%s/%s/%s" % (parent_collection, parent_id, subcollection)
    url = "%s/projects/%s/databases/(default)/documents/%s" % (FIRESTORE, PROJECT, path)
    page = request_json(url, token)
    return page.get("documents", [])


def document_id(name):
    return name.rsplit("/", 1)[-1]


def rename_fields(source, mapping):
    result = {}
    for old, new in mapping.items():
        if old in source:
            result[new] = source[old]
    return result


def transform(collection, document):
    values = firestore_fields(document.get("fields", {}))
    identifier = document_id(document["name"])

    maps = {
        "users": {
            "phoneNumber": "phone_number",
            "displayName": "display_name",
            "fcmToken": "fcm_token",
            "providerId": "provider_id",
            "createdAt": "created_at_timestamp",
            "updatedAt": "updated_at_timestamp",
        },
        "providers": {
            "ownerUid": "owner_uid",
            "fullName": "full_name",
            "phoneNumber": "phone",
            "serviceCategory": "category",
            "title": "specialization",
            "area": "city",
            "about": "bio",
            "experienceYears": "years_of_experience",
            "priceSdg": "price",
            "isAvailableNow": "is_available",
            "reviewNote": "rejection_reason",
            "ratingSum": "rating_sum",
            "ratingCount": "rating_count",
            "reviewsCount": "reviews_count",
            "docs": "document_images",
            "createdAt": "created_at_timestamp",
        },
        "orders": {
            "orderNumber": "order_number",
            "patientUid": "patient_uid",
            "patientName": "patient_name",
            "patientPhone": "patient_phone",
            "providerId": "provider_id",
            "providerName": "provider_name",
            "providerPhone": "provider_phone",
            "serviceTitle": "category",
            "areaLocation": "location",
            "serviceDetails": "notes",
            "priceSdg": "price_sdg",
            "payableAmountSdg": "payable_amount_sdg",
            "providerPayoutSdg": "provider_payout_sdg",
            "commissionSdg": "commission_sdg",
            "visitTime": "scheduled_time",
            "receiptImageUri": "receipt_image_uri",
            "cancelledBy": "cancelled_by",
            "cancelReason": "cancellation_reason",
            "isRated": "is_rated",
            "createdAt": "created_at_timestamp",
            "updatedAt": "updated_at_timestamp",
        },
        "payouts": {
            "orderId": "order_id",
            "providerId": "provider_id",
            "orderNumber": "order_number",
            "amountSdg": "amount_sdg",
            "status": "status",
            "paidAtTimestamp": "paid_at_timestamp",
            "createdAtTimestamp": "created_at_timestamp",
        },
        "ratings": {
            "orderId": "order_id",
            "providerId": "provider_id",
            "patientUid": "patient_uid",
            "patientName": "patient_name",
            "stars": "stars",
            "comment": "comment",
            "createdAt": "created_at_timestamp",
        },
        "images": {
            "ownerUid": "owner_uid",
            "data": "data",
            "createdAt": "created_at_timestamp",
        },
        "notifications": {
            "recipientUid": "recipient_uid",
            "title": "title",
            "body": "message",
            "message": "message",
            "read": "read",
            "orderId": "order_id",
            "type": "type",
            "createdAt": "created_at_timestamp",
        },
        "admin_notifications": {
            "title": "title",
            "body": "message",
            "message": "message",
            "read": "read",
            "orderId": "order_id",
            "type": "type",
            "createdAt": "created_at_timestamp",
        },
    }
    row = rename_fields(values, maps[collection])
    row["id" if collection != "users" else "uid"] = identifier
    if collection == "users":
        row.setdefault("address", values.get("address", ""))
    if collection == "providers":
        row.setdefault("full_name", values.get("name", ""))
        row.setdefault("city", values.get("area", ""))
    if collection == "orders" and row.get("receipt_image_uri") is None:
        row["receipt_image_uri"] = ""
    if collection == "ratings":
        row.setdefault("id", identifier)
    return normalize_timestamps(row)


def upsert_rows(rows, service_key):
    for collection, values in rows:
        url = "%s/rest/v1/%s" % (SUPABASE_URL, collection)
        request = urllib.request.Request(url, data=json.dumps(values).encode(), method="POST")
        request.add_header("apikey", service_key)
        request.add_header("Authorization", "Bearer " + service_key)
        request.add_header("Content-Type", "application/json")
        request.add_header("Prefer", "resolution=merge-duplicates,return=minimal")
        try:
            with urllib.request.urlopen(request):
                pass
        except urllib.error.HTTPError as exc:
            raise SystemExit("Supabase %s failed: %s\n%s" % (collection, exc.code, exc.read().decode()))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true", help="write rows; default is dry-run")
    args = parser.parse_args()
    token = access_token()
    rows = []
    counts = {}
    for collection in COLLECTIONS:
        documents = fetch_collection(token, collection)
        counts[collection] = len(documents)
        rows.extend((collection, transform(collection, document)) for document in documents)

    nested_notifications = []
    for user_document in fetch_collection(token, "users"):
        user_id = document_id(user_document["name"])
        for document in fetch_subcollection(token, "users", user_id, "notifications"):
            fields = document.setdefault("fields", {})
            fields["recipientUid"] = {"stringValue": user_id}
            nested_notifications.append(document)
    counts["users/{uid}/notifications"] = len(nested_notifications)
    rows.extend(("notifications", transform("notifications", document)) for document in nested_notifications)

    print(json.dumps({"mode": "apply" if args.apply else "dry-run", "counts": counts}, indent=2))
    if not args.apply:
        print("No data was written. Re-run with --apply and SUPABASE_SERVICE_ROLE_KEY set.")
        return
    service_key = os.environ.get("SUPABASE_SERVICE_ROLE_KEY", "")
    if not service_key:
        raise SystemExit("SUPABASE_SERVICE_ROLE_KEY is required with --apply")
    upsert_rows(rows, service_key)
    print("Migrated %d rows." % len(rows))


if __name__ == "__main__":
    main()
