#!/bin/bash
set -e
echo "Uploading mapping file to Crashlytics/Play Console..."
./gradlew app:uploadCrashlyticsMappingFileRelease
