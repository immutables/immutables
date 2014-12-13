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
    FiniteStateMachine machine = new FiniteStateMachine();

    for (int i = 0; i < content.length(); i++) {
      char c = content.charAt(i);

      switch (state) {
      case UNDEFINED:
        state = machine.nextChar(c).or(state);
        break;
      case PACKAGE:
        if (c == ' ') {
          packageFrom = i + 1;
        }
        if (c == ';') {
          currentPackage = content.subSequence(packageFrom, i).toString();
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
        state = State.UNDEFINED;
        break;
      }
    }

    parts.set(0, "package " + currentPackage + ";\n");
    parts.set(1, importsBuilder.build(currentPackage));
    return JOINER.join(parts);
  }

  private enum State {
    UNDEFINED,
    PACKAGE,
    IMPORTS,
    CLASS
  }

  private static final class FiniteStateMachine {
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

  private static final class ImportsBuilder {
    private static final String JAVA_LANG = "java.lang";

    private TreeSet<String> imports = Sets.newTreeSet();

    void addImport(String importedPackage) {
      imports.add(importedPackage);
    }

    String build(String currentPackage) {
      imports.remove(JAVA_LANG);
      imports.remove(currentPackage);

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

  public static void main(String[] args) {
    Joiner LINES = Joiner.on('\n');

    CharSequence rewrited = PostprocessingMachine.rewrite(
        LINES.join(
            "// subpackage start;",
            "package start;",
            "import java.util.TreeSet;",
            "import com.google.common.base.Function;",
            "import java.util.List;",
            "import java.util.Map;",
            "class My extends java.util.Set {}"
        ));

    System.out.println(rewrited);
    System.out.println();
  }
}
