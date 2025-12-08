#!/bin/bash

# Test script for -Aimmutables.annotations.pick option
# This tests which annotation packages are used in generated code

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="$SCRIPT_DIR/lib"
SRC_DIR="$SCRIPT_DIR/src"

# Build classpath
CP="$LIB_DIR/value-annotations-2.12.0-rc0.jar"
CP="$CP:$LIB_DIR/guava-33.0.0-jre.jar"
CP="$CP:$LIB_DIR/jakarta.annotation-api-3.0.0.jar"
CP="$CP:$LIB_DIR/javax.annotation-api-1.3.2.jar"

# Annotation processor path
PROCESSOR_PATH="$LIB_DIR/value-2.12.0-rc0.jar"

echo "=========================================="
echo "Testing -Aimmutables.annotations.pick"
echo "=========================================="
echo ""

# Helper function to compile and check generated annotations
test_annotations_pick() {
    local OPTION_VALUE=$1
    local OPTION_NAME=$2
    local OUT_DIR="$SCRIPT_DIR/generated/annotations-pick-$OPTION_NAME"

    echo ">>> Testing: -Aimmutables.annotations.pick=$OPTION_VALUE"
    echo "    Output dir: $OUT_DIR"

    # Clean and create output directory
    rm -rf "$OUT_DIR"
    mkdir -p "$OUT_DIR"

    # Compile with annotation processor
    if [ -z "$OPTION_VALUE" ]; then
        # Empty value - test with empty string
        javac -cp "$CP" \
              -processorpath "$PROCESSOR_PATH" \
              -proc:only \
              -s "$OUT_DIR" \
              -Aimmutables.annotations.pick= \
              "$SRC_DIR/com/example/Person.java"
    else
        javac -cp "$CP" \
              -processorpath "$PROCESSOR_PATH" \
              -proc:only \
              -s "$OUT_DIR" \
              -Aimmutables.annotations.pick=$OPTION_VALUE \
              "$SRC_DIR/com/example/Person.java"
    fi

    # Check generated file
    GENERATED_FILE="$OUT_DIR/com/example/ImmutablePerson.java"
    if [ -f "$GENERATED_FILE" ]; then
        echo "    ✓ Generated: ImmutablePerson.java"
        echo "    Checking imports and annotations:"

        # Show Generated annotation imports and usage
        echo "    --- @Generated annotation:"
        grep -n "import.*Generated;" "$GENERATED_FILE" || echo "    (no Generated import found)"
        grep -n "^@Generated" "$GENERATED_FILE" || echo "    (no @Generated annotation found)"

        # Show ParametersAreNonnullByDefault imports
        echo "    --- @ParametersAreNonnullByDefault annotation:"
        grep -n "import.*ParametersAreNonnullByDefault;" "$GENERATED_FILE" || echo "    (no ParametersAreNonnullByDefault import found)"
        grep -n "^@ParametersAreNonnullByDefault" "$GENERATED_FILE" || echo "    (no @ParametersAreNonnullByDefault annotation found)"

    else
        echo "    ✗ ERROR: ImmutablePerson.java not generated!"
    fi

    echo ""
}

# Test different values
test_annotations_pick "jakarta" "jakarta"
test_annotations_pick "javax" "javax"
test_annotations_pick "javax+processing" "javax-processing"
test_annotations_pick "legacy" "legacy"
test_annotations_pick "" "none"

echo "=========================================="
echo "Summary"
echo "=========================================="
echo ""
echo "Generated code locations:"
for dir in "$SCRIPT_DIR/generated"/annotations-pick-*; do
    if [ -d "$dir" ]; then
        echo "  $(basename "$dir"): $dir"
    fi
done
echo ""
echo "You can inspect the generated ImmutablePerson.java files to see"
echo "the differences in @Generated and other annotations."
