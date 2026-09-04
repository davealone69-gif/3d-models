# Avatar Studio (Android Rewrite)

This is an Android rewrite of the Grok Girls Studio application, migrated from a web-based React/Capacitor setup to a fully native Android application using Kotlin and Jetpack Compose.

## Features Preserved
- **Native 3D Avatar Rendering:** Integrates the original high-performance Kotlin/GLES3 native 3D renderer (`HdAvatarRenderer` / `GltfAvatarView`) into the Jetpack Compose app.
- **Appearance & Persona Preview:** A dedicated Appearance screen for viewing the 3D model.
- **Chat Interface:** Basic UI foundation for character interaction.
- **Gallery Interface:** A dedicated gallery tab.
- **Modern UI:** Redesigned cleanly with Material Design 3 and a cohesive dark/cyberpunk aesthetic matching the original feel.

## Architecture
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **3D Graphics:** Native Android OpenGL ES 3.0 via `GLSurfaceView`
- **Build System:** Gradle (Kotlin DSL)

## Notes
- Original React specific files, Capacitor implementations, and WebGL bindings have been removed and replaced by the single-source native Android implementation.
