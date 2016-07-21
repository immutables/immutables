package org.immutables.value.processor.encode;

import com.google.common.base.Function;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Code {
  private Code() {}

  private static final CharMatcher DELIMITER = CharMatcher.anyOf("!\"#$%&'()*+,-./:;<=>?@[]^_{|}~");
  private static final CharMatcher WHITESPACE = CharMatcher.WHITESPACE;
  private static final CharMatcher LETTER_OR_DIGIT = CharMatcher.javaLetterOrDigit().or(CharMatcher.anyOf("$_"));
  private static final CharMatcher IDENTIFIER_START = CharMatcher.javaLetter().or(CharMatcher.anyOf("$_"));

  static class Interpolator {
    private final Function<String, String> substitute;

    Interpolator(Function<String, String> substitute) {
      this.substitute = substitute;
    }

    List<Term> bind(List<Term> terms) {
      List<Term> result = new ArrayList<>(terms.size());
      for (Term t : terms) {
        if (t.isBinding()) {
          String identifier = ((Binding) t).identifier();
          String value = substitute.apply(identifier);
          if (value == null) {
            throw new IllegalStateException(
                "Cannot bind '" + identifier + "' subtitution in: " + Code.join(terms));
          }
          result.add(new WordOrNumber(value));
        } else {
          result.add(t);
        }
      }
      return result;
    }
  }

  static class Linker {
    private final Map<String, String> imports;
    private final Set<String> members;

    Linker(Map<String, String> imports, Set<String> members) {
      this.imports = imports;
      this.members = members;
    }

    List<Term> bind(List<Term> terms) {
      List<Term> result = new ArrayList<>();
      resolve(terms.iterator(), result, false);
      return result;
    }

    private void resolve(Iterator<Term> it, List<Term> result, boolean untilGenericsClose) {
      State state = State.NONE;
      while (it.hasNext()) {
        Term t = it.next();
        // On matching close we return to
        if (untilGenericsClose && t.is(">")) {
          result.add(t);
          return;
        }
        // this is to handle generics when invoking methods like
        // ImmutableList.<String>builder()
        // we don't want 'builder' to be considered to identifier
        if ((state == State.DOT || untilGenericsClose) && t.is("<")) {
          result.add(t);
          resolve(it, result, true);
          t = it.next();
        }
        state = state.next(t);
        if (state.isTopIdent()) {
          String iden = t.toString();
          if (members.contains(iden)) {
            result.add(Binding.newImpl(iden));
          } else if (imports.containsKey(iden)) {
            String qualifiedName = imports.get(iden);
            result.addAll(termsFrom(qualifiedName));
          } else {
            result.add(t);
          }
        } else {
          result.add(t);
        }
      }
    }

    enum State {
      NONE,
      DOT,
      THIS,
      THIS_DOT,
      TOP_IDENT,
      THIS_TOP_IDENT;

      boolean isTopIdent() {
        return this == TOP_IDENT
            || this == THIS_TOP_IDENT;
      }

      State next(Term t) {
        boolean isDot = t.isDelimiter()
            && t.is(".");

        boolean isThis = t.isWordOrNumber() && t.is("this");

        boolean isIdent = t.isWordOrNumber()
            && !isThis
            && IDENTIFIER_START.matches(t.toString().charAt(0));

        boolean isNothing = t.isWhitespace() || t.isComment();

        if (isNothing) {
          return this;
        } else if (isDot) {
          switch (this) {
          case THIS:
            return THIS_DOT;
          default:
            return DOT;
          }
        } else if (isThis) {
          return THIS;
        } else if (isIdent) {
          switch (this) {
          case THIS_DOT:
            return THIS_TOP_IDENT;
          case DOT:
            return NONE;
          default:
            return TOP_IDENT;
          }
        } else {
          return NONE;
        }
      }
    }
  }

  static String join(List<Term> terms) {
    return Joiner.on("").join(terms);
  }

  static List<Term> termsFrom(String input) {
    Scanner s = new Scanner(input);
    s.scan();
    return s.terms;
  }

  static class Scanner {
    final List<Term> terms = new ArrayList<>();
    private final String input;

    Scanner(String input) {
      this.input = input;
    }

    void scan() {
      int p = 0;
      char c;
      while ((c = get(p)) != '\0') {
        if (c == '/' && get(p + 1) == '/') {
          int end = slashSlashComment(p + 2);
          terms.add(new Other(get(p, p = end)));
        } else if (c == '/' && get(p + 1) == '*') {
          int end = slashStarComment(p + 2);
          terms.add(new Other(get(p, p = end)));
        } else if (c == '\'' || c == '"') {
          int end = quotedLiteral(p + 1, c);
          terms.add(new Other(get(p, p = end)));
        } else if (c == '@' && get(p + 1) == '@') {
          int end = whileMatches(p + 2, LETTER_OR_DIGIT);
          terms.add(new Binding(get(p, p = end)));
        } else if (DELIMITER.matches(c)) {
          terms.add(new Delimiter(get(p, ++p)));
        } else if (LETTER_OR_DIGIT.matches(c)) {
          int end = whileMatches(p + 1, LETTER_OR_DIGIT);
          terms.add(new WordOrNumber(get(p, p = end)));
        } else if (WHITESPACE.matches(c)) {
          int end = whileMatches(p + 1, WHITESPACE);
          terms.add(new Whitespace(get(p, p = end)));
        } else {
          terms.add(new Other(get(p, ++p)));
        }
      }
    }

    private int whileMatches(int p, CharMatcher matcher) {
      for (;; p++) {
        if (!matcher.matches(get(p))) {
          return p;
        }
      }
    }

    private int quotedLiteral(int p, char quote) {
      for (;;) {
        char c = get(p++);
        if (c == '\\') {
          // skip anything escaped, we are worry only to not meet \' or \",
          // so \u0000 are no concerns to use
          p++;
          continue;
        }
        if (c == quote) {
          return p++;
        }
      }
    }

    private int slashSlashComment(int p) {
      for (;;) {
        char c = get(p++);
        if (c == '\0' || c == '\n') {
          return p;
        }
      }
    }

    private int slashStarComment(int p) {
      for (;;) {
        char c = get(p++);
        if (c == '\0') {
          return p;
        }
        if (c == '*' && get(p) == '/') {
          return p + 1;
        }
      }
    }

    private String get(int from, int to) {
      return input.substring(Math.max(0, from), Math.min(to, input.length()));
    }

    private char get(int i) {
      if (i < 0 || i >= input.length()) {
        return '\0';
      }
      return input.charAt(i);
    }
  }

  // TODO maybe change to CharSequence reference with start/end?
  static abstract class Term extends Eq<Term> {
    private final String string;

    Term(String string) {
      super(string);
      this.string = string;
    }

    char charOf() {
      if (string.length() == 1) {
        return string.charAt(0);
      }
      throw new IllegalStateException("'" + this + "' term is not single character");
    }

    boolean isWordOrNumber() {
      return false;
    }

    boolean isDelimiter() {
      return false;
    }

    boolean isWhitespace() {
      return false;
    }

    boolean isBinding() {
      return false;
    }

    boolean isComment() {
      return false;
    }

    boolean is(String string) {
      return this.string.equals(string);
    }

    @Override
    public String toString() {
      return string;
    }

    @Override
    protected boolean eq(Term other) {
      return string.equals(other.string);
    }
  }

  private static final class WordOrNumber extends Term {
    WordOrNumber(String string) {
      super(string);
    }

    @Override
    boolean isWordOrNumber() {
      return true;
    }
  }

  private static final class Delimiter extends Term {
    Delimiter(String value) {
      super(value);
    }

    @Override
    boolean isDelimiter() {
      return true;
    }
  }

  private static final class Whitespace extends Term {
    Whitespace(String value) {
      super(value);
    }

    @Override
    boolean isWhitespace() {
      return true;
    }
  }

  private static final class Other extends Term {
    Other(String value) {
      super(value);
    }

    @Override
    boolean isComment() {
      return toString().charAt(0) == '/';
    }
  }

  private static final class Binding extends Term {
    Binding(String string) {
      super(string);
    }

    String identifier() {
      return toString().substring(2);
    }

    @Override
    boolean isBinding() {
      return true;
    }

    static Binding newImpl(String member) {
      return new Binding("@@" + member);
    }
  }
}
