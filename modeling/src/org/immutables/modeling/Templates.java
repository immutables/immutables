package org.immutables.modeling;

import static com.google.common.base.Preconditions.*;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import com.google.common.base.Strings;
import javax.annotation.Nullable;

public final class Templates {
  private Templates() {}

  public interface Unary<F, T> {}

  public interface Binary<A, B, C> {}

  public interface Applicable<T> {
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
    private int characterSinceNewline = 0;
    private final String whitespace;
    private final Consumer consumer;

    Invokation(Consumer consumer, String whitespace, Object... params) {
      this.consumer = consumer;
      this.params = params;
      this.whitespace = whitespace;
    }

    public Object param(int ordinal) {
      return params[ordinal];
    }

    public Invokation out(Object content) {
      while (content instanceof Invokable) {
        content = ((Invokable) content).invoke(this);
      }
      if (content == null) {
        return this;
      }
      String string = content.toString();
      characterSinceNewline += string.length();
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
      characterSinceNewline = 0;
      consumer.append("\n");
      consumer.append(whitespace);
      return this;
    }

    public Invokation pos(int line, int column) {
      return this;
    }

    public String whitespace() {
      return whitespace.concat(Strings.repeat(" ", characterSinceNewline));
    }

    public Consumer consumer() {
      return consumer;
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

  public static abstract class Template implements Invokable {
    private final int arity;

    protected Template(int arity) {
      checkArgument(arity > 0);
      this.arity = arity;
    }

    public abstract void run(Invokation invokation);

    @Override
    public int arity() {
      return arity;
    }

    @Nullable
    @Override
    public Invokable invoke(Invokation invokation, Object... params) {
      run(new Invokation(
          invokation.consumer(),
          invokation.whitespace(),
          params));
      return null;
    }
  }
}
