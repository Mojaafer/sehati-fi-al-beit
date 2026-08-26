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
IDENTITY = "https://identitytoolkit.googleapis.com"

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

def main():
    token = access_token()
    url = f"{IDENTITY}/admin/v2/projects/{PROJECT}/config"
    config = _request(url, token, "GET")
    print("Project Config:")
    print(json.dumps(config, indent=2))

if __name__ == "__main__":
    main()
