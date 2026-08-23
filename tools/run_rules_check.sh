#!/usr/bin/env bash
# Starts the Firestore emulator, runs tools/rules_check.py against it, and stops it again.
#
# emulators:exec is what guarantees the teardown: the emulator dies with the script even if
# the check fails or is interrupted, so a failed run never leaves port 8080 held.
#
# --project pins the emulator to the same id firestore.rules is deployed under, because the
# unsigned test tokens rules_check.py mints carry that id as their audience. It stays local:
# the emulator serves 127.0.0.1:8080 and the live sehati-home-care data is never touched.
set -euo pipefail

cd "$(dirname "$0")/.."

# The emulator is a JVM process and this machine has no java on PATH, so point at the same
# unpacked JDK the Gradle build uses; without it the CLI aborts on `Could not spawn java -version`.
export JAVA_HOME="${JAVA_HOME:-$HOME/jdk-17.0.13+11}"
export PATH="$JAVA_HOME/bin:$PATH"

# firebase-tools 14+ refuses to start the emulator on anything below JDK 21, and this machine
# only has the 17 that Gradle needs. The 13.x copy under ~/node_modules runs the same emulator
# on 17, so prefer it and fall back to whatever `firebase` is on PATH.
VENDORED="$HOME/node_modules/firebase-tools/lib/bin/firebase.js"
if [ -f "$VENDORED" ]; then
  FIREBASE=(node "$VENDORED")
else
  FIREBASE=(firebase)
fi

# The firestore emulator jar (~130 MB) is fetched into ~/.cache/firebase on first run.
"${FIREBASE[@]}" emulators:exec \
  --only firestore \
  --project sehati-home-care \
  "python tools/rules_check.py"
