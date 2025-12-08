#!/bin/bash

# Test script for -Aimmutables.guava.suppress option
# This tests whether Guava collections or JDK collections are used in generated code

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="$SCRIPT_DIR/lib"
SRC_DIR="$SCRIPT_DIR/src"

# Build classpath WITH Guava
CP_WITH_GUAVA="$LIB_DIR/value-annotations-2.12.0-rc0.jar"
CP_WITH_GUAVA="$CP_WITH_GUAVA:$LIB_DIR/guava-33.0.0-jre.jar"
CP_WITH_GUAVA="$CP_WITH_GUAVA:$LIB_DIR/jakarta.annotation-api-3.0.0.jar"

# Build classpath WITHOUT Guava (for comparison)
CP_WITHOUT_GUAVA="$LIB_DIR/value-annotations-2.12.0-rc0.jar"
CP_WITHOUT_GUAVA="$CP_WITHOUT_GUAVA:$LIB_DIR/jakarta.annotation-api-3.0.0.jar"

# Annotation processor path
PROCESSOR_PATH="$LIB_DIR/value-2.12.0-rc0.jar"

echo "=========================================="
echo "Testing -Aimmutables.guava.suppress"
echo "=========================================="
echo ""

# Test 1: Guava on classpath, NOT suppressed (default behavior)
echo ">>> Test 1: Guava on classpath, NOT suppressed (default)"
echo "    Expected: Should use com.google.common.collect.Immutable* collections"
OUT_DIR_1="$SCRIPT_DIR/generated/guava-not-suppressed"
rm -rf "$OUT_DIR_1"
mkdir -p "$OUT_DIR_1"

javac -cp "$CP_WITH_GUAVA" \
      -processorpath "$PROCESSOR_PATH" \
      -proc:only \
      -s "$OUT_DIR_1" \
      "$SRC_DIR/com/example/Team.java"

GENERATED_FILE_1="$OUT_DIR_1/com/example/ImmutableTeam.java"
if [ -f "$GENERATED_FILE_1" ]; then
    echo "    ✓ Generated: ImmutableTeam.java"
    echo "    Checking for Guava imports:"
    grep -n "import com.google.common.collect" "$GENERATED_FILE_1" || echo "    (no Guava imports found - UNEXPECTED!)"

    echo ""
    echo "    Collection types used in fields:"
    grep -n "private final.*List\|private final.*Set\|private final.*Map" "$GENERATED_FILE_1" | head -5
else
    echo "    ✗ ERROR: ImmutableTeam.java not generated!"
fi
echo ""

# Test 2: Guava on classpath, BUT suppressed
echo ">>> Test 2: Guava on classpath, BUT suppressed via -Aimmutables.guava.suppress"
echo "    Expected: Should use JDK java.util.* collections (no Guava)"
OUT_DIR_2="$SCRIPT_DIR/generated/guava-suppressed"
rm -rf "$OUT_DIR_2"
mkdir -p "$OUT_DIR_2"

javac -cp "$CP_WITH_GUAVA" \
      -processorpath "$PROCESSOR_PATH" \
      -proc:only \
      -s "$OUT_DIR_2" \
      -Aimmutables.guava.suppress \
      "$SRC_DIR/com/example/Team.java"

GENERATED_FILE_2="$OUT_DIR_2/com/example/ImmutableTeam.java"
if [ -f "$GENERATED_FILE_2" ]; then
    echo "    ✓ Generated: ImmutableTeam.java"
    echo "    Checking for Guava imports:"
    grep -n "import com.google.common.collect" "$GENERATED_FILE_2" && echo "    ✗ UNEXPECTED: Found Guava imports!" || echo "    ✓ No Guava imports (as expected)"

    echo ""
    echo "    Collection types used in fields:"
    grep -n "private final.*List\|private final.*Set\|private final.*Map" "$GENERATED_FILE_2" | head -5
else
    echo "    ✗ ERROR: ImmutableTeam.java not generated!"
fi
echo ""

# Test 3: Guava NOT on classpath (for comparison)
echo ">>> Test 3: Guava NOT on classpath (baseline comparison)"
echo "    Expected: Should use JDK java.util.* collections (no choice)"
OUT_DIR_3="$SCRIPT_DIR/generated/guava-absent"
rm -rf "$OUT_DIR_3"
mkdir -p "$OUT_DIR_3"

javac -cp "$CP_WITHOUT_GUAVA" \
      -processorpath "$PROCESSOR_PATH" \
      -proc:only \
      -s "$OUT_DIR_3" \
      "$SRC_DIR/com/example/Team.java"

GENERATED_FILE_3="$OUT_DIR_3/com/example/ImmutableTeam.java"
if [ -f "$GENERATED_FILE_3" ]; then
    echo "    ✓ Generated: ImmutableTeam.java"
    echo "    Checking for Guava imports:"
    grep -n "import com.google.common.collect" "$GENERATED_FILE_3" && echo "    ✗ UNEXPECTED: Found Guava imports!" || echo "    ✓ No Guava imports (as expected)"

    echo ""
    echo "    Collection types used in fields:"
    grep -n "private final.*List\|private final.*Set\|private final.*Map" "$GENERATED_FILE_3" | head -5
else
    echo "    ✗ ERROR: ImmutableTeam.java not generated!"
fi
echo ""

echo "=========================================="
echo "Summary"
echo "=========================================="
echo ""
echo "Generated code locations:"
echo "  Test 1 (Guava NOT suppressed): $OUT_DIR_1"
echo "  Test 2 (Guava suppressed):     $OUT_DIR_2"
echo "  Test 3 (Guava absent):         $OUT_DIR_3"
echo ""
echo "Key difference to look for:"
echo "  - Test 1 should import and use: com.google.common.collect.ImmutableList"
echo "  - Test 2 should use JDK collections: java.util.Collections.unmodifiableList"
echo "  - Test 3 should match Test 2 (JDK collections)"
echo ""
echo "Compare the files to see the difference:"
echo "  diff $OUT_DIR_1/com/example/ImmutableTeam.java \\"
echo "       $OUT_DIR_2/com/example/ImmutableTeam.java"
