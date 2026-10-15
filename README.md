# Avatar Studio

## Developer Setup

Follow these exact steps to build the release application out of the box:

a. Copy `gradle.properties.example` → `gradle.properties` and fill local values.
b. Generate keystore with `scripts/generate-keystore.sh`.
c. `./gradlew :app:assembleRelease :app:bundleRelease`
d. Verify the APK/AAB is correctly signed using `apksigner verify` or `jarsigner -verify`.


## Service and generation verification

Reuse the user's existing Termux Ollama at `http://127.0.0.1:11434` for text tasks instead of silently installing a duplicate server. Ollama health or a model-list response proves reachability only. A chat test needs non-empty generated text. A 3D job needs a real output asset that passes GLB/GLTF validation and loads in the renderer. If this project only imports or views existing models, keep generation services optional rather than requiring a backend just to open the app.
