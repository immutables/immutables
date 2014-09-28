package org.immutables.modeling;

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

  public static class Iteration {
    public int index = 0;
    public boolean first = true;
  }

  public final static class Invokation {
    private final Object[] params;
    private final String indentation;
    private final Consumer consumer;
    private String spacing = "";

    Invokation(Consumer consumer, String indentation, Object... params) {
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
      while (content instanceof Invokable) {
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

    public Consumer consumer() {
      return consumer;
    }

    public String indentation() {
      return indentation;
    }
  }

  interface Consumer {
    void append(CharSequence string);

    void append(char c);
  }

  public static class SysoutConsumer implements Consumer {
    @Override
    public void append(CharSequence string) {
      System.out.append(string);
    }

    @Override
    public void append(char c) {
      System.out.print(c);
    }

    Invokation asInvokation() {
      return new Invokation(this, "");
    }
  }

  public static abstract class Fragment implements Invokable {
    private final int arity;
    @Nullable
    private final Invokation capturedInvokation;

    protected Fragment(int arity, @Nullable Invokation capturedInvokation) {
      checkArgument(arity > 0);
      this.arity = arity;
      this.capturedInvokation = capturedInvokation;
    }

    protected Fragment(int arity) {
      this(arity, null);
    }

    public abstract void run(Invokation invokation);

    @Override
    public int arity() {
      return arity;
    }

    @Nullable
    @Override
    public Invokable invoke(Invokation invokation, Object... params) {
      String indentation = capturedInvokation != null
          ? capturedInvokation.indentation()
          : invokation.indentation().concat(invokation.spacing());

      invokation.spacing("");

      run(new Invokation(
          invokation.consumer(),
          indentation,
          params));

      return null;
    }
  }
}
