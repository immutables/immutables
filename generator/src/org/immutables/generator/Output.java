package org.immutables.generator;

import com.google.common.base.*;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateNested;
import org.immutables.generator.Templates.Invokable;
import org.immutables.generator.Templates.Invokation;

import javax.annotation.Nullable;
import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.Writer;

import static com.google.common.base.Preconditions.checkNotNull;

@GenerateNested
public final class Output {

  public final Templates.Invokable error = new Templates.Invokable() {
    @Override
    @Nullable
    public Invokable invoke(Invokation invokation, Object... parameters) {
      String message = CharMatcher.WHITESPACE.trimFrom(parameters[0].toString());
      StaticEnvironment.processing().getMessager().printMessage(
          Diagnostic.Kind.ERROR, message);
      return null;
    }

    @Override
    public int arity() {
      return 1;
    }
  };

  public final Templates.Invokable trim = new Templates.Fragment(1) {
    @Override
    public void run(Invokation invokation) {
      invokation.out(CharMatcher.WHITESPACE.trimFrom(
          toCharSequence(invokation.param(0).toString())));
    }

    private CharSequence toCharSequence(Object param) {
      checkNotNull(param);
      // Is it worthwhile optimization?
      if (param instanceof Templates.Fragment) {
        return ((Templates.Fragment) param).toCharSequence();
      }
      return param.toString();
    }
  };

  private static Joiner DOT_JOINER = Joiner.on('.').skipNulls();

  public final Templates.Invokable java = new Templates.Fragment(3) {
    @Override
    public void run(Invokation invokation) {
      String packageName = invokation.param(0).toString();
      String simpleName = invokation.param(1).toString();
      Invokable body = (Invokable) invokation.param(2);

      ImmutableOutput.JavaFileKey key = ImmutableOutput.JavaFileKey.builder()
          .packageName(packageName)
          .simpleName(simpleName)
          .build();

      JavaFile javaFile = getFiles().files.getUnchecked(key);
      body.invoke(new Invokation(javaFile.consumer));
      javaFile.complete();
    }
  };

  @GenerateImmutable
  public static abstract class JavaFileKey {
    public abstract String packageName();

    public abstract String simpleName();

    @Override
    public String toString() {
      return DOT_JOINER.join(Strings.emptyToNull(packageName()), simpleName());
    }
  }

  private static class JavaFile {
    final JavaFileKey key;
    final Templates.CharConsumer consumer = new Templates.CharConsumer();

    public JavaFile(JavaFileKey key) {
      this.key = key;
    }

    public void complete() {
      if (!StaticEnvironment.round().errorRaised()) {
        try {
          writeFile();
        } catch (FilerException ex) {
          throw Throwables.propagate(ex);
        } catch (IOException ex) {
          throw Throwables.propagate(ex);
        }
      }
    }

    private void writeFile() throws IOException {
      try (Writer writer = getFiler().createSourceFile(key.toString()).openWriter()) {
        writer.append(extractSourceCode());
      }
    }

    @SuppressWarnings("deprecation")
    private CharSequence extractSourceCode() {
      return LegacyJavaPostprocessing.rewrite(consumer.asCharSequence());
    }
  }

  private static Filer getFiler() {
    return StaticEnvironment.processing().getFiler();
  }

  private static Files getFiles() {
    return StaticEnvironment.getInstance(Files.class, FilesSupplier.INSTANCE);
  }

  private enum FilesSupplier implements Supplier<Files> {
    INSTANCE;
    @Override
    public Files get() {
      return new Files();
    }
  }

  private static class Files implements StaticEnvironment.Completable {
    final LoadingCache<JavaFileKey, JavaFile> files = CacheBuilder.newBuilder()
        .concurrencyLevel(1)
        .build(new CacheLoader<JavaFileKey, JavaFile>() {
          @Override
          public JavaFile load(JavaFileKey key) throws Exception {
            return new JavaFile(key);
          }
        });

    @Override
    public void complete() {
      if (!StaticEnvironment.round().errorRaised()) {
//      for (JavaFile file : files.asMap().values()) {
//        file.complete();
//      }
      }
    }
  }
}
