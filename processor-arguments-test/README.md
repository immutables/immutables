
# Immutables Annotation Processor Options Test

This directory contains manual tests for Immutables annotation processor options, specifically:

1. `-Aimmutables.annotations.pick` - Controls which annotation packages are used in generated code
2. `-Aimmutables.guava.suppress` - Forces JDK-only collections even when Guava is on classpath

## Directory Structure

```
immutables-annotation-processor-test/
├── lib/                            # Downloaded JAR dependencies
│   ├── value-2.12.0-rc0.jar       # Immutables annotation processor
│   ├── value-annotations-2.12.0-rc0.jar  # Immutables annotations
│   ├── guava-33.0.0-jre.jar       # Google Guava
│   ├── jakarta.annotation-api-3.0.0.jar  # Jakarta annotations
│   └── javax.annotation-api-1.3.2.jar    # Javax annotations
├── src/com/example/               # Sample Java source files
│   ├── Person.java                # Simple immutable for annotations test
│   └── Team.java                  # Immutable with collections for Guava test
├── generated/                     # Generated code output (created when tests run)
├── download-jars.sh               # Script to download all JAR dependencies
├── test-annotations-pick.sh       # Test script for -Aimmutables.annotations.pick
├── test-guava-suppress.sh         # Test script for -Aimmutables.guava.suppress
└── README.md                      # This file
```

## Getting Started

### Download Dependencies

If the `lib/` directory is empty (e.g., after cloning the repository), run the download script to fetch all required JARs from Maven Central:

```bash
chmod +x download-jars.sh
./download-jars.sh
```

This will download:
- Immutables 2.12.0-rc0 (annotation processor and annotations)
- Guava 33.0.0-jre
- Jakarta Annotation API 3.0.0
- Javax Annotation API 1.3.2

The script is idempotent - it will skip files that already exist, so it's safe to run multiple times.

## Dependencies

- **Immutables 2.12.0-rc0**: The version being tested
- **Guava 33.0.0-jre**: Latest stable Guava for testing suppression
- **Jakarta Annotation API 3.0.0**: For testing jakarta annotation generation
- **Javax Annotation API 1.3.2**: For testing javax annotation generation

## Test 1: Annotations Pick (`-Aimmutables.annotations.pick`)

This option controls which annotation packages Immutables uses in generated code.

### Usage

```bash
chmod +x test-annotations-pick.sh
./test-annotations-pick.sh
```

### What it tests

The script compiles `Person.java` with different values for `-Aimmutables.annotations.pick`:

1. **`jakarta`** - Uses `jakarta.annotation.Generated` and `jakarta.annotation.ParametersAreNonnullByDefault`
2. **`javax`** - Uses `javax.annotation.Generated`
3. **`javax+processing`** - Uses `javax.annotation.processing.Generated`
4. **`legacy`** - Uses legacy `javax.annotation.*` packages
5. **`none`** (empty value) - No processor-generated annotations

### Expected output

The script shows:
- Which annotations are imported in the generated `ImmutablePerson.java`
- Which annotations are applied to the class
- Differences between jakarta vs javax vs none

### Generated code locations

After running, inspect the generated code:
```
generated/annotations-pick-jakarta/com/example/ImmutablePerson.java
generated/annotations-pick-javax/com/example/ImmutablePerson.java
generated/annotations-pick-javax-processing/com/example/ImmutablePerson.java
generated/annotations-pick-legacy/com/example/ImmutablePerson.java
generated/annotations-pick-none/com/example/ImmutablePerson.java
```

## Test 2: Guava Suppress (`-Aimmutables.guava.suppress`)

This option forces the processor to ignore Guava even when it's on the classpath.

### Usage

```bash
chmod +x test-guava-suppress.sh
./test-guava-suppress.sh
```

### What it tests

The script runs three compilation scenarios:

1. **Guava on classpath, NOT suppressed** - Should use `com.google.common.collect.ImmutableList/Set/Map`
2. **Guava on classpath, BUT suppressed** - Should use JDK `Collections.unmodifiableList/Set/Map`
3. **Guava NOT on classpath** - Baseline comparison, should match scenario 2

### Expected output

The script shows:
- Whether Guava imports are present in generated code
- What collection types are used in the generated fields
- A diff command to compare the generated files

### Generated code locations

After running, inspect and compare:
```
generated/guava-not-suppressed/com/example/ImmutableTeam.java  # Uses Guava
generated/guava-suppressed/com/example/ImmutableTeam.java      # Uses JDK only
generated/guava-absent/com/example/ImmutableTeam.java          # Uses JDK only
```

Compare them:
```bash
diff generated/guava-not-suppressed/com/example/ImmutableTeam.java \
     generated/guava-suppressed/com/example/ImmutableTeam.java
```

## Manual Testing

You can also run javac manually to test specific scenarios:

### Test annotations.pick manually

```bash
javac -cp lib/value-annotations-2.12.0-rc0.jar:lib/jakarta.annotation-api-3.0.0.jar \
      -processorpath lib/value-2.12.0-rc0.jar \
      -proc:only \
      -s generated/manual-test \
      -Aimmutables.annotations.pick=jakarta \
      src/com/example/Person.java
```

### Test guava.suppress manually

```bash
javac -cp lib/value-annotations-2.12.0-rc0.jar:lib/guava-33.0.0-jre.jar \
      -processorpath lib/value-2.12.0-rc0.jar \
      -proc:only \
      -s generated/manual-test \
      -Aimmutables.guava.suppress \
      src/com/example/Team.java
```

## Requirements

- Java 11 or later (for running javac)
- bash shell
- curl (for downloading dependencies)
- Basic Unix tools (grep, diff)

## Documentation References

These options are documented in the Immutables documentation:

- **Annotation Processing Options**: `/newandnice.mdx` section "Annotation Processing Options"
- **`-Aimmutables.annotations.pick`**: Controls javax vs jakarta vs none for standard annotations
- **`-Aimmutables.guava.suppress`**: Forces JDK-only collections when Guava is present

## Cleaning Up

To clean all generated code:

```bash
rm -rf generated/
```

To start fresh (including downloads):

```bash
rm -rf lib/ generated/
```

Then re-download dependencies by running the test scripts again (or manually with curl).
