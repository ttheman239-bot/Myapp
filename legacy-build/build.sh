#!/bin/bash
# Full standalone APK build — no Gradle, no Android Studio, no google()
# Maven. Uses only aapt + dx + apksigner from the Ubuntu apt packages
# (android-sdk + dalvik-exchange + android-sdk-build-tools).
#
# Run from the repo root:
#   bash legacy-build/build.sh
# Output: dist/app-debug.apk
set -euo pipefail
unset JAVA_TOOL_OPTIONS

REPO_DIR=$(cd "$(dirname "$0")/.." && pwd)
SRC_DIR="$REPO_DIR/legacy-build"
WORK_DIR=$(mktemp -d -t legacy-apk-XXXXXX)
trap 'rm -rf "$WORK_DIR"' EXIT

ANDROID_JAR=/usr/lib/android-sdk/platforms/android-23/android.jar
DX=/usr/lib/android-sdk/build-tools/debian/dx

mkdir -p "$WORK_DIR/obj" "$WORK_DIR/bin" "$WORK_DIR/assets"
cp -r "$SRC_DIR/src" "$WORK_DIR/src"
cp "$SRC_DIR/AndroidManifest.xml" "$WORK_DIR/AndroidManifest.xml"
cp "$REPO_DIR/app/src/main/assets/research.json" "$WORK_DIR/assets/research.json"

cd "$WORK_DIR"

echo "=== 1. javac: .java -> .class ==="
find src -name '*.java' > sources.txt
javac -source 1.8 -target 1.8 \
  -bootclasspath "$ANDROID_JAR" \
  -classpath "$ANDROID_JAR" \
  -d obj @sources.txt
echo "OK"

echo "=== 2. dx: .class -> classes.dex ==="
"$DX" --dex --output=bin/classes.dex obj
echo "OK, dex size: $(stat -c %s bin/classes.dex) bytes"

echo "=== 3. aapt: package resources + manifest + assets -> unaligned APK ==="
aapt package -f \
  -M AndroidManifest.xml \
  -A assets \
  -I "$ANDROID_JAR" \
  -F bin/app-unaligned.apk
echo "OK, apk (no dex): $(stat -c %s bin/app-unaligned.apk) bytes"

echo "=== 4. Add classes.dex into APK ==="
( cd bin && aapt add app-unaligned.apk classes.dex > /dev/null )
echo "OK, apk (with dex): $(stat -c %s bin/app-unaligned.apk) bytes"

echo "=== 5. zipalign ==="
zipalign -f 4 bin/app-unaligned.apk bin/app-aligned.apk

echo "=== 6. Generate debug keystore (if missing) ==="
KEYSTORE="$REPO_DIR/legacy-build/.debug.keystore"
if [ ! -f "$KEYSTORE" ]; then
  keytool -genkeypair \
    -keystore "$KEYSTORE" \
    -storepass android -keypass android \
    -alias androiddebugkey \
    -dname "CN=Android Debug, O=Android, C=US" \
    -keyalg RSA -keysize 2048 -validity 10000 > /dev/null 2>&1
  echo "Generated new keystore"
else
  echo "Reusing existing keystore"
fi

echo "=== 7. apksigner: sign APK ==="
apksigner sign \
  --ks "$KEYSTORE" \
  --ks-pass pass:android \
  --key-pass pass:android \
  --ks-key-alias androiddebugkey \
  --out bin/app-debug.apk \
  bin/app-aligned.apk

echo "=== 8. Verify signature ==="
apksigner verify --verbose bin/app-debug.apk | head -10

echo ""
echo "=== 9. Copy to dist/ ==="
mkdir -p "$REPO_DIR/dist"
cp bin/app-debug.apk "$REPO_DIR/dist/app-debug.apk"
echo "Wrote $REPO_DIR/dist/app-debug.apk"
stat -c 'Size: %s bytes' "$REPO_DIR/dist/app-debug.apk"
sha256sum "$REPO_DIR/dist/app-debug.apk"
