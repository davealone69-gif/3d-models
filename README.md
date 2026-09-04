# Avatar Studio

## Developer Setup

Follow these exact steps to build the release application out of the box:

a. Copy `gradle.properties.example` → `gradle.properties` and fill local values.
b. Generate keystore with `scripts/generate-keystore.sh`.
c. `./gradlew :app:assembleRelease :app:bundleRelease`
d. Verify the APK/AAB is correctly signed using `apksigner verify` or `jarsigner -verify`.
