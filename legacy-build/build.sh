#!/bin/bash
set -e
unset JAVA_TOOL_OPTIONS

cd /tmp/hello-apk
ANDROID_JAR=/usr/lib/android-sdk/platforms/android-23/android.jar
DX=/usr/lib/android-sdk/build-tools/debian/dx

echo "=== 3. aapt: package resources + manifest -> unaligned APK ==="
aapt package -f -M AndroidManifest.xml -I "$ANDROID_JAR" -F bin/app-unaligned.apk
ls -la bin/app-unaligned.apk

echo "=== 4. Add classes.dex into APK ==="
cd bin
aapt add app-unaligned.apk classes.dex
cd ..
ls -la bin/app-unaligned.apk

echo "=== 5. zipalign ==="
zipalign -f 4 bin/app-unaligned.apk bin/app-aligned.apk
ls -la bin/app-aligned.apk

echo "=== 6. Generate debug keystore (if missing) ==="
if [ ! -f bin/debug.keystore ]; then
  keytool -genkeypair -v \
    -keystore bin/debug.keystore \
    -storepass android -keypass android \
    -alias androiddebugkey \
    -dname "CN=Android Debug, O=Android, C=US" \
    -keyalg RSA -keysize 2048 -validity 10000 2>&1 | tail -5
fi

echo "=== 7. apksigner: sign APK ==="
apksigner sign \
  --ks bin/debug.keystore \
  --ks-pass pass:android \
  --key-pass pass:android \
  --ks-key-alias androiddebugkey \
  --out bin/app-debug.apk \
  bin/app-aligned.apk

echo "=== 8. Verify signature ==="
apksigner verify --verbose bin/app-debug.apk

echo ""
echo "=== DONE ==="
ls -la bin/app-debug.apk
sha256sum bin/app-debug.apk
