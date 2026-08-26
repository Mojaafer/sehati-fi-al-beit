import json
import os
import sys
import time
import urllib.parse
import urllib.request

PROJECT = "sehati-home-care"
CONFIGSTORE = os.path.expanduser("~/.config/configstore/firebase-tools.json")
CLIENT_ID = "563584335869-fgrhgmd47bqnekij5i8b5pr03ho849e6.apps.googleusercontent.com"
CLIENT_SECRET = os.environ.get("FIREBASE_CLI_CLIENT_SECRET", "")
FIRESTORE = "https://firestore.googleapis.com/v1"

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

def access_token():
    if not CLIENT_SECRET:
        raise SystemExit("FIREBASE_CLI_CLIENT_SECRET is required")
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
        body = json.loads(resp.read().decode())
    tokens["access_token"] = body["access_token"]
    tokens["expires_at"] = int(time.time() * 1000) + body.get("expires_in", 3600) * 1000
    with open(CONFIGSTORE, "w", encoding="utf-8") as handle:
        json.dump({"tokens": tokens}, handle)
    return body["access_token"]

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

def delete_all_orders():
    token = access_token()
    url = f"{FIRESTORE}/projects/{PROJECT}/databases/(default)/documents/orders?pageSize=300"
    data = _request(url, token, "GET")
    documents = data.get("documents", [])
    
    if not documents:
        print("No orders found in database. The collection is already empty.")
        return

    print(f"Found {len(documents)} orders in Firestore. Deleting...")
    deleted_count = 0
    for doc in documents:
        doc_name = doc["name"] # projects/sehati-home-care/databases/(default)/documents/orders/<id>
        delete_url = f"{FIRESTORE}/{doc_name}"
        _request(delete_url, token, "DELETE")
        order_id = doc_name.split("/")[-1]
        print(f"Deleted order: {order_id}")
        deleted_count += 1
        
    print(f"\nSuccessfully deleted all {deleted_count} customer orders from database.")

if __name__ == "__main__":
    delete_all_orders()
