package org.immutables.modeling;

import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateNested;
import org.immutables.modeling.Templates.CharConsumer;

@GenerateNested
public final class Output {

  private static Joiner DOT_JOINER = Joiner.on('.');

  @GenerateImmutable
  public static abstract class JavaFileKey {
    public abstract String packageName();

    public abstract String simpleName();

    @Override
    public String toString() {
      return DOT_JOINER.join(packageName(), simpleName());
    }
  }

  private static class JavaFile implements CharConsumer {
    private final StringBuilder builder = new StringBuilder();
    private final JavaFileKey key;

    public JavaFile(JavaFileKey key) {
      this.key = key;
    }

    @Override
    public void append(CharSequence string) {
      builder.append(string);
    }

    @Override
    public void append(char c) {
      builder.append(c);
    }

    public void complete() {
      try {
        getFiler().createSourceFile(key.toString());
      } catch (FilerException ex) {
        try {
          FileObject resource =
              getFiler().getResource(StandardLocation.SOURCE_PATH, key.packageName(), key.simpleName());
        } catch (IOException ex1) {
          ex.addSuppressed(ex1);
          throw Throwables.propagate(ex1);
        }
      } catch (IOException ex) {
        throw Throwables.propagate(ex);
      }
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

  protected static JavaFile createJavaFile(JavaFileKey key) {
    return null;
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
    public void complete() {}
  }
}
