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

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.immutables.extgenerator.GeneratedImportsModifier;
import org.immutables.generator.SourceExtraction.Imports;

public final class PostprocessingMachine {
  private static final char ASCII_MAX = '\u007f';

  private static final Joiner JOINER = Joiner.on("");

  private static final ImmutableList<GeneratedImportsModifier> importsModifiers =
      ImmutableList.copyOf(ServiceLoader.load(GeneratedImportsModifier.class,
          PostprocessingMachine.class.getClassLoader()));

  private PostprocessingMachine() {}

  public static CharSequence rewrite(CharSequence content) {
    return rewrite(content, true);
  }

  public static CharSequence rewrite(CharSequence content, boolean useSemicolon) {
    try {
      return rewrite(content, new ImportsBuilder(useSemicolon), ScanAtMost.ALL);
    } catch (UnsupportedEncodingException ex) {
      return content;
    }
  }

  public static CharSequence collectHeader(CharSequence content) {
    try {
      return rewrite(content, new ImportsBuilder(true), ScanAtMost.HEADER);
    } catch (UnsupportedEncodingException ex) {
      return "";
    }
  }

  public static Imports collectImports(CharSequence content) {
    try {
      ImportsBuilder importsBuilder = new ImportsBuilder(true);
      rewrite(content, importsBuilder, ScanAtMost.IMPORTS);
      return Imports.of(importsBuilder.imports, importsBuilder.originalImports);
    } catch (UnsupportedEncodingException ex) {
      return Imports.empty();
    }
  }

  private enum ScanAtMost {
    HEADER,
    IMPORTS,
    ALL
  }

  private static CharSequence rewrite(CharSequence content, ImportsBuilder importsBuilder, ScanAtMost scanAtMost)
      throws UnsupportedEncodingException {
    String currentPackage = "";

    State state = State.UNDEFINED;
    int packageFrom = -1;
    int importFrom = -1;
    int nextPartFrom = 0;
    boolean importStarts = false;
    int classNameOccurrencesInImportBlock = 0;
    int classNameFrom = -1;
    int classNameTo = -1;
    FiniteStateMachine machine = new FiniteStateMachine();
    QualifiedNameMachine qualifiedNameMachine = new QualifiedNameMachine();
    QualifiedNameMachine importsQualifiedNameMachine = new QualifiedNameMachine().allowNestedTypes();
    CommentMachine commentMachine = new CommentMachine();
    ClassNameMachine nameMachine = new ClassNameMachine();
    @Nullable CharSequence header = null;

    for (int i = 0; i < content.length(); i++) {
      char c = content.charAt(i);
      if (c == '\r') {
        continue;
      }

      commentMachine.nextChar(c);

      if (commentMachine.isInComment()) {
        continue;
      }

      // non-ascii characters outside of comments or string literals
      // will cancel source-rewriting or imports parsing.
      if (c > ASCII_MAX) {
        throw new UnsupportedEncodingException();
      }

      if (header == null && state.pastHeader()) {
        int lastLineIndex = i;
        // find last line break index before package declaration
        while (--lastLineIndex > 0) {
          if (content.charAt(lastLineIndex) == '\n') {
            break;
          }
        }

        // so we will cut all from file start
        // up to the package (or first package annotation) declaration
        // not including newline character
        header = content.subSequence(0, Math.max(0, lastLineIndex));

        if (scanAtMost == ScanAtMost.HEADER) {
          // Short circuit everything when only collecting header
          return header;
        }
      }

      if (scanAtMost == ScanAtMost.IMPORTS && state.atClassDefinition()) {
        // Short circuit everything when only collecting imports
        return "";
      }

      switch (state) {
      case UNDEFINED:
        state = machine.nextChar(c).or(state);
        break;
      case PACKAGE:
        if (c == ' ') {
          packageFrom = i + 1;
        }
        if (c == ';' || c == '\n') {
          nextPartFrom = i + 2;
          currentPackage = content.subSequence(packageFrom, i).toString();
          importsBuilder.setCurrentPackage(currentPackage);
          state = State.UNDEFINED;
          packageFrom = -1;
        }
        break;
      case IMPORTS:
        nameMachine.nextChar(c, i, true);
        if (!importsQualifiedNameMachine.isFinished()) {
          importsQualifiedNameMachine.nextChar(c, i);
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
        if (c == ';' || c == '\n') {
          nextPartFrom = i + 2;
          if (importsQualifiedNameMachine.isFinished()) {
            String simpleName = content.subSequence(
                importsQualifiedNameMachine.packageTo,
                importsQualifiedNameMachine.importTo).toString();

            String qualifiedName =
                content.subSequence(
                    importsQualifiedNameMachine.importFrom,
                    importsQualifiedNameMachine.importTo).toString();

            String packageFromImport =
                content.subSequence(importFrom, i).toString();

            importsBuilder.addOriginalImport(
                simpleName,
                qualifiedName,
                packageFromImport);
          } else {
            importsBuilder.addImport(content.subSequence(importFrom, i).toString());
          }
          importsQualifiedNameMachine.reset();
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
        // Step back for annotation
        c = content.charAt(--i);
        // move to state CLASS
        state = State.CLASS;
        //$FALL-THROUGH$
      case CLASS:
        nameMachine.nextChar(c, i);
        qualifiedNameMachine.nextChar(c, i);
        if (qualifiedNameMachine.isFinished()) {
          importsBuilder.addImportCandidate(
              content.subSequence(qualifiedNameMachine.packageTo, qualifiedNameMachine.importTo).toString(),
              content.subSequence(qualifiedNameMachine.importFrom, qualifiedNameMachine.importTo).toString(),
              qualifiedNameMachine.importFrom,
              qualifiedNameMachine.importTo,
              qualifiedNameMachine.packageTo);
          qualifiedNameMachine.reset();
        }
        if (qualifiedNameMachine.state == FullyQualifiedNameState.CLASS) {
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

    List<ImportCandidate> candidates = importsBuilder.candidates();
    List<CharSequence> contentParts = Lists.newArrayListWithExpectedSize(candidates.size());

    for (ImportCandidate importCandidate : candidates) {
      if (importCandidate.importTo != -1) {
        importsBuilder.addImport(importCandidate.preparedImport);
      }
      contentParts.add(content.subSequence(nextPartFrom, importCandidate.importFrom));
      nextPartFrom = importCandidate.packageTo;
    }

    String imports = importsBuilder.build();

    // last part
    contentParts.add(content.subSequence(nextPartFrom, content.length()));

    List<CharSequence> headerParts = Lists.newArrayListWithExpectedSize(8);

    // header
    if (header != null) {
      headerParts.add(header);
      // compensate line break that we cut off
      // from header
      if (header.length() > 0) {
        headerParts.add("\n");
      }
    }

    // package
    if (!currentPackage.isEmpty()) {
      headerParts.add("package ");
      headerParts.add(currentPackage);
      if (importsBuilder.useSemicolon) {
        headerParts.add(";");
      }
      headerParts.add("\n\n");
    }

    // imports
    headerParts.add(imports);

    return JOINER.join(Iterables.concat(headerParts, contentParts));
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

    public boolean atClassDefinition() {
      return this == ANNOTATION
          || this == CLASS;
    }

    public boolean pastHeader() {
      return this != UNDEFINED;
    }
  }

  static final class FiniteStateMachine {
    private static final int NO_POSITION = -1;
    private static final int AT_POSSIBLE_WORD = -1;
    private static final int UNTRACKED_WORD = -2;

    private static final char[][] vocabulary = new char[][] {
        "package".toCharArray(),
        "import".toCharArray(),
        "class".toCharArray(),
        "interface".toCharArray(),
        "enum".toCharArray(),
        "@".toCharArray() // counts for annotations before class and annotation type
    };

    private static final State[] finalState = new State[] {
        State.PACKAGE,
        State.IMPORTS,
        State.CLASS,
        State.CLASS,
        State.CLASS,
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
        if (wordIndex == 1 && charIndex == 0 && c == 'n') {
          // very dirty hack to parse 'interface' instead of 'import'
          wordIndex = 3;
        }
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
    private static final int NO_IMPORT = -1;

    private static final String JAVA_LANG = "java.lang.";

    private final TreeSet<String> imports = Sets.newTreeSet();
    private final HashMap<String, String> originalImports = Maps.newHashMap();
    private final Multimap<String, ImportCandidate> importCandidates = HashMultimap.create();
    private final HashMap<String, String> nameToQualified = Maps.newHashMap();
    private final HashSet<String> exceptions = Sets.newHashSet();
    private final HashSet<String> stopList = Sets.newHashSet();

    private String currentPackagePrefix = "";

    private String currentPackage;

    final boolean useSemicolon;

    ImportsBuilder(boolean useSemicolon) {
      this.useSemicolon = useSemicolon;
    }

    void addImportCandidate(String name, String qualifiedName, int importFrom, int importTo, int packageTo) {
      @Nullable String foundQualified = nameToQualified.get(name);
      if (foundQualified != null && !foundQualified.equals(qualifiedName)) {
        return;
      }

      nameToQualified.put(name, qualifiedName);

      if (omittedImport(name, qualifiedName)) {
        importTo = NO_IMPORT;
      }

      importCandidates.put(qualifiedName, new ImportCandidate(importFrom, importTo, packageTo, qualifiedName, name));
    }

    void addImport(String importedPackage) {
      imports.add(importedPackage);
    }

    void addOriginalImport(String name, String qualifiedName, String importedPackage) {
      originalImports.put(name, qualifiedName);
      if (!omittedImport(name, qualifiedName)) {
        imports.add(importedPackage);
      }
    }

    private boolean omittedImport(String name, String qualifiedName) {
      if ((JAVA_LANG + name).equals(qualifiedName)) {
        return true;
      }
      if (qualifiedName.equals(currentPackagePrefix.concat(name))) {
        return true;
      }
      return false;
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
      this.currentPackage = currentPackage;
      this.currentPackagePrefix = currentPackage.isEmpty() ? "" : (currentPackage + '.');
    }

    void preBuild() {
      for (String exception : exceptions) {
        @Nullable String qualifiedName = nameToQualified.get(exception);
        if (qualifiedName != null) {
          String asInCurrentPackage = currentPackagePrefix.concat(exception);
          if (!qualifiedName.equals(asInCurrentPackage)) {
            importCandidates.removeAll(qualifiedName);
          }
        }
      }

      for (Iterator<Entry<String, ImportCandidate>> it = importCandidates.entries().iterator(); it.hasNext();) {
				Map.Entry<String, ImportCandidate> candidateEntry = it.next();
				ImportCandidate candidate = candidateEntry.getValue();
        String originalFullyName = originalImports.get(candidate.simpleName);
        if (originalFullyName != null && !originalFullyName.equals(candidate.preparedImport)) {
        	it.remove();
        }
			}
    }

    String build() {
      invokeImportModifiers();
      List<String> generatedImports = new ArrayList<>();
      for (String i : imports) {
        generatedImports.add("import ");
        generatedImports.add(i);
        if (useSemicolon) {
          generatedImports.add(";");
        }
        generatedImports.add("\n");
      }
      return JOINER.join(generatedImports);
    }

    private void invokeImportModifiers() {
      for (GeneratedImportsModifier modifier : importsModifiers) {
        modifier.modify(currentPackage, imports);
      }
    }

    List<ImportCandidate> candidates() {
      return Ordering.natural().sortedCopy(importCandidates.values());
    }
  }

  static final class QualifiedNameMachine {
    FullyQualifiedNameState state = FullyQualifiedNameState.UNDEFINED;
    int importFrom = -1;
    int importTo = -1;
    int packageTo = -1;
    boolean allowNestedTypes;

    void nextChar(char c, int i) {
      switch (state) {
      case UNDEFINED:
        if (isLowerCaseAlphabetic(c) || isUnderscore(c)) {
          state = FullyQualifiedNameState.PACKAGE_PART_CANDIDATE;
          importFrom = i;
        } else if (isAlphabetic(c) || isDigit(c) || isUnderscore(c)) {
          state = FullyQualifiedNameState.IDLE;
        }
        break;
      case IDLE:
        if (!isAlphabetic(c) && !isDigit(c) && !isUnderscore(c) && c != '.') {
          state = FullyQualifiedNameState.UNDEFINED;
        }
        break;
      case PACKAGE_PART_CANDIDATE:
        if (c == '.') {
          state = FullyQualifiedNameState.DOT;
        } else if (!isAlphabetic(c) && !isDigit(c) && !isUnderscore(c)) {
          reset();
        }
        break;
      case DOT:
        if (isLowerCaseAlphabetic(c) || isUnderscore(c)) {
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
        if (c == '.' & allowNestedTypes) {
          state = FullyQualifiedNameState.DOT;
          packageTo = -1;
        } else if (!isAlphabetic(c) && !isDigit(c) && !isUnderscore(c)) {
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

    QualifiedNameMachine allowNestedTypes() {
      allowNestedTypes = true;
      return this;
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
      default:
        break;
      }
    }

    boolean isInComment() {
      switch (state) {
      case LINE_COMMENT:
      case BLOCK_COMMENT:
      case BLOCK_COMMENT_OUT_CANDIDATE:
      case STRING_LITERAL:
        return true;
      default:
        return false;
      }
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

  private static boolean isUnderscore(char c) {
    return c == '_';
  }
}
