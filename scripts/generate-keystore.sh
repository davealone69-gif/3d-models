#!/bin/bash
set -e
echo "Generating release keystore..."
keytool -genkey -v -keystore release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000 -storepass password -keypass password -dname "CN=App,O=App,c=US"
echo "Keystore generated: release.keystore"
echo ""
echo "Add the following to your gradle.properties or environment variables:"
echo "KEYSTORE_PATH=\$(pwd)/release.keystore"
echo "KEYSTORE_PASSWORD=password"
echo "KEY_ALIAS=release"
echo "KEY_PASSWORD=password"
