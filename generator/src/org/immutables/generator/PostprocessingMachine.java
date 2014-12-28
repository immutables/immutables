package org.immutables.generator;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.TreeSet;

final class PostprocessingMachine {
  private static final Joiner JOINER = Joiner.on("");

  private PostprocessingMachine() {
  }

  static CharSequence rewrite(CharSequence content) {
    String currentPackage = "";
    ImportsBuilder importsBuilder = new ImportsBuilder();
    ArrayList<String> parts = Lists.newArrayList();
    // reserve position for package
    parts.add("");
    // reserve position for imports
    parts.add("");

    State state = State.UNDEFINED;
    int packageFrom = -1;
    int importFrom = -1;
    int nextPartFrom = -1;
    FiniteStateMachine machine = new FiniteStateMachine();
    FullyQualifiedNameMachine fullyQualifiedNameMachine = new FullyQualifiedNameMachine();

    for (int i = 0; i < content.length(); i++) {
      char c = content.charAt(i);

      switch (state) {
      case UNDEFINED:
        state = machine.nextChar(c).or(state);
        if (!Character.isAlphabetic(c)) {
          nextPartFrom = i + 1;
        }
        break;
      case PACKAGE:
        if (c == ' ') {
          packageFrom = i + 1;
        }
        if (c == ';') {
          currentPackage = content.subSequence(packageFrom, i).toString();
          importsBuilder.setCurrentPackage(currentPackage);
          state = State.UNDEFINED;
          packageFrom = -1;
        }
        break;
      case IMPORTS:
        if (c == ' ') {
          importFrom = i + 1;
        }
        if (c == ';') {
          importsBuilder.addImport(content.subSequence(importFrom, i).toString());
          state = State.UNDEFINED;
          importFrom = -1;
        }
        break;
      case CLASS:
        fullyQualifiedNameMachine.nextChar(c, i);
        if (fullyQualifiedNameMachine.isFinished()) {
          importsBuilder.addImport(
              content.subSequence(fullyQualifiedNameMachine.importFrom, fullyQualifiedNameMachine.importTo).toString());
          parts.add(content.subSequence(nextPartFrom, fullyQualifiedNameMachine.importFrom).toString());
          nextPartFrom = fullyQualifiedNameMachine.packageTo;
        }
        break;
      }
    }
    // last part
    parts.add(content.subSequence(nextPartFrom, content.length()).toString());

    parts.set(0, "package " + currentPackage + ";\n");
    parts.set(1, importsBuilder.build());
    return JOINER.join(parts);
  }

  enum State {
    UNDEFINED,
    PACKAGE,
    IMPORTS,
    CLASS
  }

  static final class FiniteStateMachine {
    private static final char[][] vocabulary = new char[][] {
        {'p', 'a', 'c', 'k', 'a', 'g', 'e'},
        {'i', 'm', 'p', 'o', 'r', 't'},
        {'c', 'l', 'a', 's', 's'}
    };

    private static final State[] finalState = new State[] {
        State.PACKAGE,
        State.IMPORTS,
        State.CLASS
    };

    int wordIndex = -1;
    int charIndex = -1;

    Optional<State> nextChar(char c) {
      Optional<State> state = Optional.absent();

      if (wordIndex == -2) {

        if (!Character.isAlphabetic(c) && !Character.isDigit(c)) {
          wordIndex = -1;
        }

      } else if (wordIndex == -1) {

        for (int i = 0; i < vocabulary.length; i++) {
          if (c == vocabulary[i][0]) {
            wordIndex = i;
            charIndex = 0;
            break;
          }
        }

        if (wordIndex == -1 && (Character.isAlphabetic(c) || Character.isDigit(c))) {
          wordIndex = -2;
        }

      } else {

        if (vocabulary[wordIndex][charIndex + 1] == c) {
          charIndex++;
          if (vocabulary[wordIndex].length == charIndex + 1) {
            state = Optional.of(finalState[wordIndex]);
            wordIndex = -1;
            charIndex = -1;
          }
        } else {
          wordIndex = -1;
          charIndex = -1;
        }

      }

      return state;
    }
  }

  static final class ImportsBuilder {
    private static final String JAVA_LANG = "java.lang";

    private TreeSet<String> imports = Sets.newTreeSet();
    private Optional<String> currentPackage = Optional.absent();

    void addImport(String importedPackage) {
      String normalized = normalize(importedPackage);

      if (normalized.startsWith(JAVA_LANG)) {
        return;
      }

      if (currentPackage.isPresent() && normalized.startsWith(currentPackage.get())) {
        return;
      }

      imports.add(normalized);
    }

    void setCurrentPackage(String currentPackage) {
      this.currentPackage = Optional.of(currentPackage);
    }

    private String normalize(String s) {
      return s.replace(" ", "").replace("\n", "").replace("\t", "").replace("\r", "");
    }

    String build() {
      return JOINER.join(Iterables.transform(imports, ToImportStatement.FUNCTION));
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
        if (Character.isLetter(c) && Character.isAlphabetic(c)) {
          state = FullyQualifiedNameState.PACKAGE_PART_CANDIDATE;
          importFrom = i;
        }
        break;
      case PACKAGE_PART_CANDIDATE:
        if (c == '.') {
          state = FullyQualifiedNameState.DOT;
        } else if (isSpaceChar(c)) {
          state = FullyQualifiedNameState.SPACE_BEFORE_DOT;
        } else if (!Character.isAlphabetic(c) && !Character.isDigit(c)) {
          state = FullyQualifiedNameState.UNDEFINED;
        }
        break;
      case SPACE_BEFORE_DOT:
        if (c == '.') {
          state = FullyQualifiedNameState.DOT;
        } else if (Character.isAlphabetic(c)) {
          state = FullyQualifiedNameState.PACKAGE_PART_CANDIDATE;
          importFrom = i;
        } else if (!isSpaceChar(c)) {
          state = FullyQualifiedNameState.UNDEFINED;
        }
        break;
      case DOT:
        if (Character.isAlphabetic(c) && Character.isLowerCase(c)) {
          state = FullyQualifiedNameState.PACKAGE_PART_CANDIDATE;
        } else if (Character.isAlphabetic(c) && Character.isUpperCase(c)) {
          state = FullyQualifiedNameState.CLASS;
        } else if (!isSpaceChar(c)) {
          state = FullyQualifiedNameState.UNDEFINED;
        }
        break;
      case CLASS:
        if (packageTo == -1) {
          packageTo = i - 1;
        }
        if (!Character.isAlphabetic(c) && !Character.isDigit(c)) {
          state = FullyQualifiedNameState.AFTER_CLASS;
        }
        break;
      case AFTER_CLASS:
        if (importTo == -1) {
          importTo = i - 1;
        }
        if (Character.isAlphabetic(c)) {
          state = FullyQualifiedNameState.METHOD_OR_FIELD;
        } else if (c == '(' || c == '<' || c == ')' || c == '>' || c == '{' || c == '}') {
          state = FullyQualifiedNameState.FINISH;
        } else if (!isSpaceChar(c)) {
          state = FullyQualifiedNameState.UNDEFINED;
        }
        break;
      case METHOD_OR_FIELD:
        if (!Character.isAlphabetic(c) && !Character.isDigit(c)) {
          state = FullyQualifiedNameState.FINISH;
        }
        break;
      case FINISH:
        reset();
        break;
      }
    }

    boolean isFinished() {
      return FullyQualifiedNameState.FINISH.equals(state);
    }

    void reset() {
      state = FullyQualifiedNameState.UNDEFINED;
      importFrom = -1;
      importTo = -1;
      packageTo = 1;
    }
  }

  enum FullyQualifiedNameState {
    UNDEFINED,
    PACKAGE_PART_CANDIDATE,
    SPACE_BEFORE_DOT,
    DOT,
    CLASS,
    AFTER_CLASS,
    METHOD_OR_FIELD,
    FINISH
  }

  private static boolean isSpaceChar(char c) {
    return Character.isSpaceChar(c) || c == '\n' || c == '\t' || c == '\r';
  }

  public static void main(String[] args) {
    String s0 = "s java.until.Set {}";
    String s1 = "com.      google.\tcommon.base\n. Function    .  apply();";
    String s2 = "com.google.common.base.Function.class;";
    String s3 = "com.google.common.base.Function();";
    String s4 = "com.google.common.collect.ImmutableList.Builder<>;";
    String s5 = "com.google.common.base.Preconditions.checkState();";

    test(s0);
    test(s1);
    test(s2);
    test(s3);
    test(s4);
    test(s5);
  }

  private static void test(String s) {
    FullyQualifiedNameMachine fullyQualifiedNameMachine = new FullyQualifiedNameMachine();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      fullyQualifiedNameMachine.nextChar(c, i);

      if (FullyQualifiedNameState.FINISH.equals(fullyQualifiedNameMachine.state)) {
        System.out.println(s.substring(fullyQualifiedNameMachine.importFrom, fullyQualifiedNameMachine.importTo));
        System.out.println(s.substring(fullyQualifiedNameMachine.importFrom, fullyQualifiedNameMachine.packageTo));
      }
    }
    System.out.println();
    System.out.println();
  }
}
