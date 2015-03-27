/*
    Copyright 2015 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.generator;

import javax.annotation.Nullable;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

final class PostprocessingMachine {
  private static final Joiner JOINER = Joiner.on("");

  private PostprocessingMachine() {}

  static CharSequence rewrite(CharSequence content) {
    String currentPackage = "";
    ImportsBuilder importsBuilder = new ImportsBuilder();

    State state = State.UNDEFINED;
    int packageFrom = -1;
    int importFrom = -1;
    int nextPartFrom = 0;
    boolean importStarts = false;
    int classNameOccurrencesInImportBlock = 0;
    int classNameFrom = -1;
    int classNameTo = -1;
    FiniteStateMachine machine = new FiniteStateMachine();
    FullyQualifiedNameMachine fullyNameMachine = new FullyQualifiedNameMachine();
    CommentMachine commentMachine = new CommentMachine();
    ClassNameMachine nameMachine = new ClassNameMachine();

    for (int i = 0; i < content.length(); i++) {
      char c = content.charAt(i);

      commentMachine.nextChar(c);
      if (commentMachine.isInComment()) {
        continue;
      }

      switch (state) {
      case UNDEFINED:
        state = machine.nextChar(c).or(state);
        break;
      case PACKAGE:
        if (c == ' ') {
          packageFrom = i + 1;
        }
        if (c == ';') {
          nextPartFrom = i + 2;
          currentPackage = content.subSequence(packageFrom, i).toString();
          importsBuilder.setCurrentPackage(currentPackage);
          state = State.UNDEFINED;
          packageFrom = -1;
        }
        break;
      case IMPORTS:
        nameMachine.nextChar(c, i, true);
        if (!fullyNameMachine.isFinished()) {
          fullyNameMachine.nextChar(c, i);
        }
        if (nameMachine.isFound()) {
          classNameOccurrencesInImportBlock++;
          classNameFrom = nameMachine.classNameFrom;
          classNameTo = nameMachine.classNameTo;
        }
        if (!importStarts && c == ' ') {
          importFrom = i + 1;
          importStarts = true;
        }
        if (c == ';') {
          nextPartFrom = i + 2;
          if (fullyNameMachine.isFinished()) {
            importsBuilder.addOriginalImport(
                content.subSequence(fullyNameMachine.packageTo, fullyNameMachine.importTo).toString(),
                content.subSequence(fullyNameMachine.importFrom, fullyNameMachine.importTo).toString(),
                content.subSequence(importFrom, i).toString());
          } else {
            importsBuilder.addImport(content.subSequence(importFrom, i).toString());
          }
          fullyNameMachine.reset();
          state = State.UNDEFINED;
          importFrom = -1;
          importStarts = false;
          if (classNameOccurrencesInImportBlock == 1) {
            importsBuilder.addToStopList(content.subSequence(classNameFrom, classNameTo).toString());
            nameMachine.reset();
            classNameOccurrencesInImportBlock = 0;
          }
        }
        break;
      case ANNOTATION:
        c = content.charAt(--i); // FIXME? Step back for annotation
        // move to state CLASS
        state = State.CLASS;
        //$FALL-THROUGH$
      case CLASS:
        nameMachine.nextChar(c, i);
        fullyNameMachine.nextChar(c, i);
        if (fullyNameMachine.isFinished()) {
          importsBuilder.addImportCandidate(
              content.subSequence(fullyNameMachine.packageTo, fullyNameMachine.importTo).toString(),
              content.subSequence(fullyNameMachine.importFrom, fullyNameMachine.importTo).toString(),
              fullyNameMachine.importFrom,
              fullyNameMachine.importTo,
              fullyNameMachine.packageTo);
          fullyNameMachine.reset();
        }
        if (fullyNameMachine.state == FullyQualifiedNameState.CLASS) {
          nameMachine.reset();
        }
        if (nameMachine.isFound()) {
          importsBuilder.addException(
              content.subSequence(nameMachine.classNameFrom, nameMachine.classNameTo).toString());
        }
        break;
      }
    }

    importsBuilder.preBuild();

    StringBuilder stringBuilder = new StringBuilder(content.length() << 1);

    for (ImportCandidate importCandidate : importsBuilder.candidates()) {
      if (importCandidate.importTo != -1) {
        importsBuilder.addImport(importCandidate.preparedImport);
      }
      stringBuilder.append(content.subSequence(nextPartFrom, importCandidate.importFrom));
      nextPartFrom = importCandidate.packageTo;
    }

    String imports = importsBuilder.build();

    stringBuilder
        // last part
        .append(content.subSequence(nextPartFrom, content.length()))
        // imports
        .insert(0, imports);

    // package
    if (!currentPackage.isEmpty()) {
      stringBuilder.insert(0, ";\n\n").insert(0, currentPackage).insert(0, "package ");
    }

    return stringBuilder.toString();
  }

  enum State {
    UNDEFINED,
    PACKAGE,
    IMPORTS,
    CLASS,
    ANNOTATION;
    State or(State state) {
      return this == UNDEFINED ? state : this;
    }
  }

  static final class FiniteStateMachine {
    private static final int NO_POSITION = -1;// FIXME? correct name?
    private static final int AT_POSSIBLE_WORD = -1; // FIXME? correct name?
    private static final int UNTRACKED_WORD = -2; // FIXME? correct name?

    private static final char[][] vocabulary = new char[][] {
        "package".toCharArray(),
        "import".toCharArray(),
        "class".toCharArray(),
        "interface".toCharArray(),
        "enum".toCharArray(),
        // "@interface".toCharArray(),
        "@".toCharArray() // counts for annotations before class and annotation type
    };

    private static final State[] finalState = new State[] {
        State.PACKAGE,
        State.IMPORTS,
        State.CLASS,
        State.CLASS,
        State.CLASS,
        // State.CLASS,
        State.ANNOTATION
    };

    int wordIndex;
    int charIndex;

    {
      resetWord();
    }

    State nextChar(char c) {
      State newState = State.UNDEFINED;

      if (wordIndex == UNTRACKED_WORD) {
        if (!isWordChar(c)) {
          resetWord();
        }
      } else if (wordIndex == AT_POSSIBLE_WORD) {
        if (isWordChar(c)) {
          for (int i = 0; i < vocabulary.length; i++) {
            if (c == vocabulary[i][0]) {
              wordIndex = i;
              charIndex = 0;
              break;
            }
          }

          if (wordIndex == AT_POSSIBLE_WORD) {
            wordIndex = UNTRACKED_WORD;
          }
        }
      } else {
        if (vocabulary[wordIndex].length == 1) {
          newState = finalState[wordIndex];
          resetWord();
        } else if (vocabulary[wordIndex][charIndex + 1] == c) {
          charIndex++;
          if (vocabulary[wordIndex].length == charIndex + 1) {
            newState = finalState[wordIndex];
            resetWord();
          }
        } else {
          resetWord();
        }
      }

      return newState;
    }

    private void resetWord() {
      wordIndex = AT_POSSIBLE_WORD;
      charIndex = NO_POSITION;
    }

    private boolean isWordChar(char c) {
      return isAlphabetic(c) || c == '@';
    }
  }

  static final class ImportsBuilder {
    private static final String JAVA_LANG = "java.lang.";

    private final TreeSet<String> imports = Sets.newTreeSet();
    private final HashMap<String, String> originalImports = Maps.newHashMap();
    private Optional<String> currentPackage = Optional.absent();
    private final Multimap<String, ImportCandidate> importCandidates = HashMultimap.create();
    private final HashMap<String, String> nameToQualified = Maps.newHashMap();
    private final HashSet<String> exceptions = Sets.newHashSet();
    private final HashSet<String> stopList = Sets.newHashSet();

    void addImportCandidate(String name, String qualifiedName, int importFrom, int importTo, int packageTo) {
      @Nullable String foundQualified = nameToQualified.get(name);
      if (foundQualified != null && !foundQualified.equals(qualifiedName)) {
        return;
      }

      nameToQualified.put(name, qualifiedName);

      if ((JAVA_LANG + name).equals(qualifiedName)) {
        importCandidates.put(qualifiedName, new ImportCandidate(importFrom, -1, packageTo, qualifiedName, name));
        return;
      }

      if (currentPackage.isPresent() && qualifiedName.equals(currentPackage.get() + '.' + name)) {
        importCandidates.put(qualifiedName, new ImportCandidate(importFrom, -1, packageTo, qualifiedName, name));
        return;
      }

      importCandidates.put(qualifiedName, new ImportCandidate(importFrom, importTo, packageTo, qualifiedName, name));
    }

    void addImport(String importedPackage) {
      imports.add(importedPackage);
    }

    void addOriginalImport(String name, String qualifiedName, String importedPackage) {
      originalImports.put(name, qualifiedName);

      if ((JAVA_LANG + name).equals(qualifiedName)) {
        return;
      }

      if (currentPackage.isPresent() && qualifiedName.equals(currentPackage.get() + '.' + name)) {
        return;
      }

      imports.add(importedPackage);
    }

    void addToStopList(String name) {
      stopList.add(name);
    }

    void addException(String name) {
      if (!stopList.contains(name)) {
        exceptions.add(name);
      }
    }

    void setCurrentPackage(String currentPackage) {
      this.currentPackage = Optional.of(currentPackage);
    }

    void preBuild() {
      for (String exception : exceptions) {
        importCandidates.removeAll(nameToQualified.get(exception));
      }

      for (Map.Entry<String, ImportCandidate> candidateEntry : importCandidates.entries()) {
        ImportCandidate candidate = candidateEntry.getValue();
        String originalFullyName = originalImports.get(candidate.simpleName);
        if (originalFullyName != null && !originalFullyName.equals(candidate.preparedImport)) {
          importCandidates.remove(candidateEntry.getKey(), candidateEntry.getValue());
        }
      }
    }

    String build() {
      return JOINER.join(Iterables.transform(imports, ToImportStatement.FUNCTION));
    }

    List<ImportCandidate> candidates() {
      return Ordering.natural().sortedCopy(importCandidates.values());
    }
  }

  private enum ToImportStatement implements Function<String, String> {
    FUNCTION;

    @Override
    public String apply(String input) {
      return "import " + input + ";\n";
    }
  }

  static final class FullyQualifiedNameMachine {
    FullyQualifiedNameState state = FullyQualifiedNameState.UNDEFINED;
    int importFrom = -1;
    int importTo = -1;
    int packageTo = -1;

    void nextChar(char c, int i) {
      switch (state) {
      case UNDEFINED:
        if (isLowerCaseAlphabetic(c)) {
          state = FullyQualifiedNameState.PACKAGE_PART_CANDIDATE;
          importFrom = i;
        } else if (isAlphabetic(c) || isDigit(c)) {
          state = FullyQualifiedNameState.IDLE;
        }
        break;
      case IDLE:
        if (!isAlphabetic(c) && !isDigit(c) && c != '.') {
          state = FullyQualifiedNameState.UNDEFINED;
        }
        break;
      case PACKAGE_PART_CANDIDATE:
        if (c == '.') {
          state = FullyQualifiedNameState.DOT;
        } else if (!isAlphabetic(c) && !isDigit(c)) {
          reset();
        }
        break;
      case DOT:
        if (isLowerCaseAlphabetic(c)) {
          state = FullyQualifiedNameState.PACKAGE_PART_CANDIDATE;
        } else if (isUpperCaseAlphabetic(c)) {
          state = FullyQualifiedNameState.CLASS;
        } else {
          reset();
        }
        break;
      case CLASS:
        if (packageTo == -1) {
          packageTo = i - 1;
        }
        if (!isAlphabetic(c) && !isDigit(c)) {
          state = FullyQualifiedNameState.FINISH;
          importTo = i;
        }
        break;
      case FINISH:
        reset();
        break;
      }
    }

    boolean isFinished() {
      return FullyQualifiedNameState.FINISH == state;
    }

    void reset() {
      state = FullyQualifiedNameState.UNDEFINED;
      importFrom = -1;
      importTo = -1;
      packageTo = -1;
    }
  }

  enum FullyQualifiedNameState {
    UNDEFINED,
    IDLE,
    PACKAGE_PART_CANDIDATE,
    DOT,
    CLASS,
    FINISH
  }

  static final class CommentMachine {

    CommentState state = CommentState.NOT_IN_COMMENT;

    void nextChar(char c) {
      switch (state) {
      case NOT_IN_COMMENT:
        if (c == '"') {
          state = CommentState.STRING_LITERAL;
        } else if (c == '/') {
          state = CommentState.COMMENT_CANDIDATE;
        }
        break;
      case COMMENT_CANDIDATE:
        if (c == '/') {
          state = CommentState.LINE_COMMENT;
        } else if (c == '*') {
          state = CommentState.BLOCK_COMMENT;
        }
        break;
      case STRING_LITERAL:
        if (c == '"') {
          state = CommentState.NOT_IN_COMMENT;
        }
        break;
      case LINE_COMMENT:
        if (c == '\n') {
          state = CommentState.NOT_IN_COMMENT;
        }
        break;
      case BLOCK_COMMENT:
        if (c == '*') {
          state = CommentState.BLOCK_COMMENT_OUT_CANDIDATE;
        }
        break;
      case BLOCK_COMMENT_OUT_CANDIDATE:
        if (c == '/') {
          state = CommentState.NOT_IN_COMMENT;
        } else if (c != '*') {
          state = CommentState.BLOCK_COMMENT;
        }
        break;
      }
    }

    boolean isInComment() {
      return CommentState.LINE_COMMENT == state
          || CommentState.BLOCK_COMMENT == state
          || CommentState.BLOCK_COMMENT_OUT_CANDIDATE == state
          || CommentState.STRING_LITERAL == state;
    }

  }

  enum CommentState {
    NOT_IN_COMMENT,
    COMMENT_CANDIDATE,
    STRING_LITERAL,
    LINE_COMMENT,
    BLOCK_COMMENT,
    BLOCK_COMMENT_OUT_CANDIDATE
  }

  static final class ClassNameMachine {

    ClassNameState state = ClassNameState.UNDEFINED;
    int classNameFrom = -1;
    int classNameTo = -1;

    boolean isFound() {
      return classNameFrom != -1 && classNameTo != -1;
    }

    void reset() {
      state = ClassNameState.UNDEFINED;
      classNameFrom = -1;
      classNameTo = -1;
    }

    void nextChar(char c, int i) {
      nextChar(c, i, false);
    }

    void nextChar(char c, int i, boolean noIdle) {
      switch (state) {
      case UNDEFINED:
        if (isUpperCaseAlphabetic(c)) {
          state = ClassNameState.CLASS_NAME;
          classNameFrom = i;
          classNameTo = -1;
        } else if (isAlphabetic(c) || isDigit(c)) {
          if (!noIdle) {
            state = ClassNameState.IDLE;
          }
        } else {
          classNameFrom = -1;
          classNameTo = -1;
        }
        break;
      case IDLE:
        if (!isAlphabetic(c) && !isDigit(c) && c != '.') {
          state = ClassNameState.UNDEFINED;
        }
        break;
      case CLASS_NAME:
        if (!isAlphabetic(c) && !isDigit(c) && c != '_') {
          state = ClassNameState.UNDEFINED;
          classNameTo = i;
        }
        break;
      }
    }

  }

  enum ClassNameState {
    UNDEFINED,
    IDLE,
    CLASS_NAME
  }

  private static final class ImportCandidate implements Comparable<ImportCandidate> {
    final int importFrom;
    final int importTo;
    final int packageTo;
    String preparedImport;
    String simpleName;

    private ImportCandidate(int importFrom, int importTo, int packageTo, String preparedImport, String simpleName) {
      this.importFrom = importFrom;
      this.importTo = importTo;
      this.packageTo = packageTo;
      this.preparedImport = preparedImport;
      this.simpleName = simpleName;
    }

    @Override
    public int compareTo(ImportCandidate other) {
      return this.importFrom - other.importFrom;
    }
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private static boolean isAlphabetic(char c) {
    return isLowerCaseAlphabetic(c) || isUpperCaseAlphabetic(c);
  }

  private static boolean isLowerCaseAlphabetic(char c) {
    return c >= 'a' && c <= 'z';
  }

  private static boolean isUpperCaseAlphabetic(char c) {
    return c >= 'A' && c <= 'Z';
  }
}
