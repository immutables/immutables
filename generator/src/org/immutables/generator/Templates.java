package org.immutables.generator;

import com.google.common.base.CharMatcher;
import javax.annotation.Nullable;
import static com.google.common.base.Preconditions.*;

public final class Templates {
  private Templates() {}

  public interface Binary<L, R, T> {
    T apply(L left, R right);
  }

  public interface Apply<T> {
    T apply(Object... parameters);
  }

  public interface Invokable {
    @Nullable
    Invokable invoke(Invokation invokation, Object... parameters);

    int arity();
  }

  static class CharConsumer {
    private final StringBuilder builder = new StringBuilder();
    /** Index after indentation. */
    private int lineStartIndex;
    public CharSequence indentation = "";
    private boolean delimit;
    private boolean wasNewline = true;

    void append(CharSequence string) {
      if (string.length() == 0) {
        return;
      }
      beforeAppend();
      builder.append(string);
    }

    void append(char c) {
      if (c == '\n') {
        newline();
      } else {
        beforeAppend();
        builder.append(c);
      }
    }

    private void beforeAppend() {
      if (wasNewline) {
        indent();
        wasNewline = false;
      }
    }

    public CharSequence getCurrentIndentation() {
      CharSequence sequence = getCurrentLine();
      return CharMatcher.WHITESPACE.matchesAllOf(sequence)
          ? sequence
          : indentation;
    }

    private CharSequence getCurrentLine() {
      // Optimize subsequence if necessary
      return builder.subSequence(lineStartIndex, builder.length());
    }

    private void indent() {
      if (indentation != null) {
        builder.append(indentation);
      }
    }

    /** makes next newline remove previous whitespace line */
    private void delimit() {
      delimit = true;
    }

    void newline() {
      if (delimit && wasBlankLine()) {
        builder.setLength(lineStartIndex);
        delimit = false;
      } else {
        builder.append('\n');
      }
      lineStartIndex = builder.length();
      wasNewline = true;
    }

    private boolean wasBlankLine() {
      return CharMatcher.WHITESPACE.matchesAllOf(getCurrentLine());
    }

    @Override
    public String toString() {
      return builder.toString();
    }

    public CharSequence asCharSequence() {
      return builder;
    }
  }

  public static class Iteration {
    public int index = 0;
    public boolean first = true;
  }

  public final static class Invokation {
    @Nullable
    final CharConsumer consumer;
    private final Object[] params;

    static Invokation initial() {
      return new Invokation(null, "");
    }

    Invokation(@Nullable CharConsumer consumer, Object... params) {
      this.consumer = consumer;
      this.params = checkNotNull(params);
    }

    public Object param(int ordinal) {
      return params[ordinal];
    }

    public Invokation delimit() {
      if (consumer != null) {
        consumer.delimit();
      }
      return this;
    }

    public Invokation ln() {
      if (consumer != null) {
        consumer.newline();
      }
      return this;
    }

    public Invokation out(Object content) {
      if (content instanceof Invokable) {
        content = ((Invokable) content).invoke(this);
      }
      if (content == null) {
        return this;
      }
      if (consumer != null) {
        consumer.append(content instanceof CharSequence
            ? ((CharSequence) content)
            : content.toString());
      }
      return this;
    }

    public Invokation out(Object... objects) {
      for (Object object : objects) {
        out(object);
      }
      return this;
    }

    public Invokation pos(int pos) {
      return this;
    }
  }

  public static abstract class Fragment implements Invokable {
    private final int arity;
    @Nullable
    private final CharSequence capturedIndentation;

    protected Fragment(int arity, @Nullable Invokation capturedInvokation) {
      assert arity >= 0;
      this.arity = arity;
      this.capturedIndentation =
          capturedInvokation != null
              ? (capturedInvokation.consumer != null
                  ? capturedInvokation.consumer.indentation
                  : "")
              : null;
    }

    protected Fragment(int arity) {
      this(arity, null);
    }

    @Override
    public int arity() {
      return arity;
    }

    public abstract void run(Invokation invokation);

    @Nullable
    @Override
    public Invokable invoke(Invokation invokation, Object... params) {
      CharSequence indentationToResore = "";
      if (invokation.consumer != null) {
        indentationToResore = invokation.consumer.indentation;

        invokation.consumer.indentation = capturedIndentation != null
            ? capturedIndentation
            : invokation.consumer.getCurrentIndentation();
      }

      run(new Invokation(invokation.consumer, params));

      if (invokation.consumer != null) {
        invokation.consumer.indentation = indentationToResore;
      }

      return null;
    }

    private String cachedToString;

    CharSequence toCharSequence() {
      if (capturedIndentation != null && arity == 0) {
        CharConsumer consumer = new CharConsumer();
        invoke(new Invokation(consumer));
        return consumer.asCharSequence();
      }
      return super.toString();
    }

    /**
     * Ability to pass caputured fragment and evaluate it as a string.
     * For non-captured fragments or fragments which expects parameters, plain {@code toString}
     * returned.
     */
    @Override
    public String toString() {
      if (cachedToString == null) {
        cachedToString = toCharSequence().toString();
      }
      return cachedToString;
    }
  }
}
