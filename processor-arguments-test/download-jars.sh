#!/bin/bash

# Download all required JAR dependencies for Immutables annotation processor testing
# This script downloads JARs from Maven Central to the lib/ directory

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="$SCRIPT_DIR/lib"

# Maven Central base URL
MAVEN_CENTRAL="https://repo1.maven.org/maven2"

# JAR definitions: groupId:artifactId:version:filename
JARS=(
    "org.immutables:value:2.12.0-rc0:value-2.12.0-rc0.jar"
    "org.immutables:value-annotations:2.12.0-rc0:value-annotations-2.12.0-rc0.jar"
    "com.google.guava:guava:33.0.0-jre:guava-33.0.0-jre.jar"
    "jakarta.annotation:jakarta.annotation-api:3.0.0:jakarta.annotation-api-3.0.0.jar"
    "javax.annotation:javax.annotation-api:1.3.2:javax.annotation-api-1.3.2.jar"
)

echo "=========================================="
echo "Downloading JAR Dependencies"
echo "=========================================="
echo ""

# Create lib directory if it doesn't exist
if [ ! -d "$LIB_DIR" ]; then
    echo "Creating lib directory: $LIB_DIR"
    mkdir -p "$LIB_DIR"
    echo ""
fi

# Function to convert Maven coordinates to URL path
# e.g., org.immutables:value:2.12.0-rc0 -> org/immutables/value/2.12.0-rc0
maven_path() {
    local GROUP_ID=$1
    local ARTIFACT_ID=$2
    local VERSION=$3
    local FILENAME=$4

    # Replace dots with slashes in groupId
    local GROUP_PATH="${GROUP_ID//.//}"

    echo "${MAVEN_CENTRAL}/${GROUP_PATH}/${ARTIFACT_ID}/${VERSION}/${FILENAME}"
}

# Download each JAR
DOWNLOADED=0
SKIPPED=0
FAILED=0

for jar_def in "${JARS[@]}"; do
    # Parse the JAR definition
    IFS=':' read -r GROUP_ID ARTIFACT_ID VERSION FILENAME <<< "$jar_def"

    TARGET_FILE="$LIB_DIR/$FILENAME"

    # Check if already downloaded
    if [ -f "$TARGET_FILE" ]; then
        echo "✓ Already exists: $FILENAME"
        ((SKIPPED++))
        continue
    fi

    # Build Maven Central URL
    URL=$(maven_path "$GROUP_ID" "$ARTIFACT_ID" "$VERSION" "$FILENAME")

    echo "Downloading: $FILENAME"
    echo "  From: $URL"

    # Download with curl
    if curl -f -L -o "$TARGET_FILE" "$URL" 2>/dev/null; then
        FILE_SIZE=$(ls -lh "$TARGET_FILE" | awk '{print $5}')
        echo "  ✓ Downloaded successfully ($FILE_SIZE)"
        ((DOWNLOADED++))
    else
        echo "  ✗ Failed to download!"
        ((FAILED++))
        # Remove partial download if it exists
        rm -f "$TARGET_FILE"
    fi

    echo ""
done

echo "=========================================="
echo "Download Summary"
echo "=========================================="
echo ""
echo "Downloaded: $DOWNLOADED"
echo "Skipped (already exists): $SKIPPED"
echo "Failed: $FAILED"
echo ""

if [ $FAILED -gt 0 ]; then
    echo "⚠️  Some downloads failed. Please check your internet connection"
    echo "   and try again."
    exit 1
fi

echo "All dependencies are ready in: $LIB_DIR"
echo ""
echo "JAR files:"
ls -lh "$LIB_DIR" | tail -n +2

echo ""
echo "You can now run the test scripts:"
echo "  ./test-annotations-pick.sh"
echo "  ./test-guava-suppress.sh"
