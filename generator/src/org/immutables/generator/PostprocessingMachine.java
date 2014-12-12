package org.immutables.generator;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.util.ArrayList;

final class PostprocessingMachine {
  private static final Joiner JOINER = Joiner.on("");

  private PostprocessingMachine() {
  }

  static CharSequence rewrite(CharSequence content) {
    String packageStatement = "";
    String importsBlock = "";
    ArrayList<String> parts = Lists.newArrayList();
    // reserve position for package
    parts.add("");
    // reserve position for imports
    parts.add("");

    State state = State.UNDEFINED;
    int packageFrom = 0;
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
          packageStatement = content.subSequence(packageFrom, i).toString();
          state = State.UNDEFINED;
        }
        break;
      case IMPORTS:
        state = State.UNDEFINED;
        break;
      case CLASS:
        state = State.UNDEFINED;
        break;
      }
    }

    parts.set(0, "package " + packageStatement + ";\n");
    parts.set(1, importsBlock + "\n");
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

      if (wordIndex == -1) {
        for (int i = 0; i < vocabulary.length; i++) {
          if (c == vocabulary[i][0]) {
            wordIndex = i;
            charIndex = 0;
            break;
          }
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
}
