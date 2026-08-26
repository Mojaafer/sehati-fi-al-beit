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

HTML_EMAIL_TEMPLATE = """<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f0fdfa; margin: 0; padding: 20px; direction: rtl; text-align: right; }
    .container { max-width: 540px; margin: 0 auto; background: #ffffff; border-radius: 18px; border: 1px solid #ccfbf1; padding: 32px 24px; box-shadow: 0 4px 12px rgba(15, 118, 110, 0.08); }
    .header { text-align: center; border-bottom: 2px solid #f0fdfa; padding-bottom: 20px; margin-bottom: 24px; }
    .logo-badge { font-size: 38px; display: inline-block; background: #ccfbf1; border-radius: 14px; padding: 10px 14px; margin-bottom: 12px; }
    h1 { color: #0f766e; font-size: 22px; margin: 0 0 6px 0; font-weight: 800; }
    p.subtitle { color: #64748b; font-size: 13px; margin: 0; }
    .content { font-size: 15px; color: #1e293b; line-height: 1.8; margin-bottom: 28px; }
    .btn-container { text-align: center; margin: 28px 0; }
    .btn { display: inline-block; background-color: #0f766e; color: #ffffff !important; text-decoration: none; padding: 14px 32px; border-radius: 12px; font-weight: bold; font-size: 16px; box-shadow: 0 4px 6px -1px rgba(15, 118, 110, 0.2); }
    .fallback { background: #f8fafc; border-radius: 10px; padding: 12px; font-size: 12px; color: #64748b; word-break: break-all; margin-top: 20px; }
    .footer { text-align: center; font-size: 11px; color: #94a3b8; margin-top: 28px; border-top: 1px solid #f1f5f9; padding-top: 16px; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <div class="logo-badge">🏥</div>
      <h1>صحتي في البيت</h1>
      <p class="subtitle">رعاية صحية منزلية موثوقة في ود مدني</p>
    </div>
    <div class="content">
      <p>مرحباً بك،</p>
      <p>تلقينا طلباً لتسجيل الدخول إلى حسابك في تطبيق <strong>صحتي في البيت</strong>. اضغط على الزر أدناه لإكمال الدخول بأمان وبدون كلمة مرور:</p>
      <div class="btn-container">
        <a href="%LINK%" class="btn">تسجيل الدخول إلى التطبيق 📲</a>
      </div>
      <p style="font-size: 13px; color: #64748b;">إذا لم تطلب هذا الرابط، يمكنك تجاهل هذه الرسالة بأمان.</p>
      <div class="fallback">
        <p style="margin:0 0 6px 0;">إذا لم يعمل الزر أعلاه، انسخ الرابط التالي وافتحه في هاتفك:</p>
        <a href="%LINK%" style="color: #0f766e;">%LINK%</a>
      </div>
    </div>
    <div class="footer">
      &copy; 2026 صحتي في البيت • ود مدني، ولاية الجزيرة<br>
      هذه رسالة آلية لتأكيد تسجيل الدخول.
    </div>
  </div>
</body>
</html>"""

def main():
    token = access_token()
    url = f"{IDENTITY}/admin/v2/projects/{PROJECT}/config?updateMask=notification.sendEmail.verifyEmailTemplate,notification.sendEmail.resetPasswordTemplate,notification.defaultLocale"
    
    body = {
        "notification": {
            "defaultLocale": "ar",
            "sendEmail": {
                "verifyEmailTemplate": {
                    "senderLocalPart": "auth",
                    "subject": "رابط تسجيل الدخول إلى صحتي في البيت 🏥",
                    "body": HTML_EMAIL_TEMPLATE,
                    "bodyFormat": "HTML",
                    "replyTo": "support@sehati-home-care.firebaseapp.com"
                },
                "resetPasswordTemplate": {
                    "senderLocalPart": "auth",
                    "subject": "إعادة تعيين كلمة المرور - صحتي في البيت 🏥",
                    "body": HTML_EMAIL_TEMPLATE,
                    "bodyFormat": "HTML",
                    "replyTo": "support@sehati-home-care.firebaseapp.com"
                }
            }
        }
    }
    
    res = _request(url, token, "PATCH", body)
    print("Email template updated successfully in Firebase Identity Toolkit!")
    print(json.dumps(res.get("notification", {}), indent=2))

if __name__ == "__main__":
    main()
