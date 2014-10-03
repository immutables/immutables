package org.immutables.modeling;

import java.io.PrintStream;
import javax.annotation.Nullable;
import static com.google.common.base.Preconditions.*;

public final class Templates {
  private Templates() {}

  public interface Unary<F, T> {
    T apply(F from);
  }

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

  interface CharConsumer {
    void append(CharSequence string);

    void append(char c);
  }

  public static class Iteration {
    public int index = 0;
    public boolean first = true;
  }

  public final static class Invokation {
    private final Object[] params;
    private final String indentation;
    private final CharConsumer consumer;
    private String spacing = "";

    static Invokation initial() {
      return new Invokation(new PrintStreamConsumer(System.out), "");
    }

    Invokation(CharConsumer consumer, String indentation, Object... params) {
      this.consumer = checkNotNull(consumer);
      this.params = checkNotNull(params);
      this.indentation = checkNotNull(indentation);
    }

    public Object param(int ordinal) {
      return params[ordinal];
    }

    public Invokation spacing(String spacing) {
      this.spacing = checkNotNull(spacing);
      return this;
    }

    public String spacing() {
      return spacing;
    }

    public Invokation out(Object content) {
      if (content instanceof Invokable) {
        content = ((Invokable) content).invoke(this);
      }
      if (content == null) {
        return this;
      }
      String string = content.toString();
      consumer.append(string);
      return this;
    }

    public Invokation out(Object... objects) {
      for (Object object : objects) {
        out(object);
      }
      return this;
    }

    public Invokation ln() {
      consumer.append('\n');
      consumer.append(indentation);
      return this;
    }

    public Invokation pos(int line, int column) {
      return this;
    }

    public CharConsumer consumer() {
      return consumer;
    }

    public String indentation() {
      return indentation;
    }
  }

  static class PrintStreamConsumer implements CharConsumer {
    private final PrintStream stream;

    private PrintStreamConsumer(PrintStream stream) {
      this.stream = stream;
    }

    @Override
    public void append(CharSequence string) {
      stream.append(string);
    }

    @Override
    public void append(char c) {
      stream.print(c);
    }
  }

  static class StringBuilderConsumer implements CharConsumer {
    private final StringBuilder builder = new StringBuilder();

    @Override
    public void append(CharSequence string) {
      builder.append(string);
    }

    @Override
    public void append(char c) {
      builder.append(c);
    }

    @Override
    public String toString() {
      return builder.toString();
    }

    public CharSequence asCharSequence() {
      return builder;
    }
  }

  public static abstract class Fragment implements Invokable {
    private final int arity;
    @Nullable
    private final Invokation capturedInvokation;

    protected Fragment(int arity, @Nullable Invokation capturedInvokation) {
      assert arity >= 0;
      this.arity = arity;
      this.capturedInvokation = capturedInvokation;
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
      String indentation = capturedInvokation != null
          ? capturedInvokation.indentation()
          : invokation.indentation().concat(consumedSpacing(invokation));

      run(new Invokation(
          invokation.consumer(),
          indentation,
          params));

      return null;
    }

    /**
     * Read and reset spacing, as it is consumed by current invokation of this fragment.
     * Compilation/runtime sequence might be revised to get rid of this side effect
     * @return spacing for this invokation.
     */
    private String consumedSpacing(Invokation invokation) {
      String spacing = invokation.spacing();
      invokation.spacing("");
      return spacing;
    }

    private String cachedToString;

    @Override
    public String toString() {
      // Ability to pass caputured fragment and evaluate it as a string
      if (capturedInvokation != null && arity == 0) {
        if (cachedToString == null) {
          StringBuilderConsumer consumer = new StringBuilderConsumer();
          invoke(new Invokation(consumer, capturedInvokation.indentation()));
          cachedToString = consumer.toString();
        }
        return cachedToString;
      }
      return super.toString();
    }
  }
}
