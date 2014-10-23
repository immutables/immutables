package org.immutables.generator;

import com.google.common.base.*;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.CharStreams;
import org.immutables.generator.Templates.Invokable;
import org.immutables.generator.Templates.Invokation;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

@Value.Nested
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

  public final Templates.Invokable java = new Templates.Fragment(3) {
    @Override
    public void run(Invokation invokation) {
      String packageName = invokation.param(0).toString();
      String simpleName = invokation.param(1).toString();
      Invokable body = (Invokable) invokation.param(2);

      ResourceKey key = new ResourceKey(packageName, simpleName);
      SourceFile javaFile = getFiles().sourceFiles.getUnchecked(key);
      body.invoke(new Invokation(javaFile.consumer));
      javaFile.complete();
    }
  };

  public final Templates.Invokable service = new Templates.Fragment(3) {
    private static final String META_INF_SERVICES = "META-INF/services/";

    @Override
    public void run(Invokation invokation) {
      String interfaceName = invokation.param(0).toString();
      Invokable body = (Invokable) invokation.param(1);

      ResourceKey key = new ResourceKey("", META_INF_SERVICES + interfaceName);
      AppendResourceFile servicesFile = getFiles().appendResourceFiles.getUnchecked(key);
      body.invoke(new Invokation(servicesFile.consumer));
    }
  };

  private final static class ResourceKey {
    private static Joiner PACKAGE_RESOURCE_JOINER = Joiner.on('.').skipNulls();

    final String packageName;
    final String relativeName;

    ResourceKey(String packageName, String simpleName) {
      this.packageName = checkNotNull(packageName);
      this.relativeName = checkNotNull(simpleName);
    }

    @Override
    public String toString() {
      return PACKAGE_RESOURCE_JOINER.join(Strings.emptyToNull(packageName), relativeName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(packageName, relativeName);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ResourceKey) {
        ResourceKey other = (ResourceKey) obj;
        return this.packageName.equals(other.packageName)
            || this.relativeName.equals(other.relativeName);
      }
      return false;
    }
  }

  private static class AppendResourceFile {
    private static final Pattern COMMENT_LINE = Pattern.compile("^\\#.*");

    final ResourceKey key;
    final Templates.CharConsumer consumer = new Templates.CharConsumer();

    AppendResourceFile(ResourceKey key) {
      this.key = key;
    }

    void complete() {
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
      LinkedHashSet<String> services = Sets.newLinkedHashSet();
      try {
        FileObject existing = getFiler().getResource(StandardLocation.CLASS_PATH, key.packageName, key.relativeName);
        FluentIterable.from(CharStreams.readLines(existing.openReader(true)))
            .filter(Predicates.not(Predicates.contains(COMMENT_LINE)))
            .copyInto(services);
      } catch (Exception ex) {
        // unable to read existing file
      }

      FluentIterable.from(Splitter.on("\n").split(consumer.asCharSequence()))
          .copyInto(services);

      new CharSink() {
        @Override
        public Writer openStream() throws IOException {
          return getFiler()
              .createResource(StandardLocation.CLASS_OUTPUT, key.packageName, key.relativeName)
              .openWriter();
        }
      }.writeLines(services, "\n");
    }
  }

  private static class SourceFile {
    final ResourceKey key;
    final Templates.CharConsumer consumer = new Templates.CharConsumer();

    SourceFile(ResourceKey key) {
      this.key = key;
    }

    void complete() {
      if (!StaticEnvironment.round().errorRaised()) {
        try {
          writeFile();
        } catch (IOException ex) {
          throw Throwables.propagate(ex);
        }
      }
    }

    private void writeFile() throws IOException {
      CharSequence sourceCode = extractSourceCode();

      try (Writer writer = getFiler().createSourceFile(key.toString()).openWriter()) {
        writer.append(sourceCode);
      } catch (IOException ex) {
        if (!identicalFileIsAlreadyGenerated(sourceCode)) {
          throw ex;
        }
      }
    }

    private boolean identicalFileIsAlreadyGenerated(CharSequence sourceCode) {
      try {
        String existingContent = new CharSource() {
          @Override
          public Reader openStream() throws IOException {
            return getFiler()
                .getResource(StandardLocation.SOURCE_OUTPUT, key.packageName, key.relativeName)
                .openReader(true);
          }
        }.read();

        if (existingContent.contentEquals(sourceCode)) {
          // We are ok, for some reason the same file is already generated,
          // happens in Eclipse for example.
          return true;
        }
      } catch (Exception ignoredAttemptToGetExistingFile) {
        // we have some other problem, not an existing file
      }
      return false;
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
    final LoadingCache<ResourceKey, SourceFile> sourceFiles = CacheBuilder.newBuilder()
        .concurrencyLevel(1)
        .build(new CacheLoader<ResourceKey, SourceFile>() {
          @Override
          public SourceFile load(ResourceKey key) throws Exception {
            return new SourceFile(key);
          }
        });

    final LoadingCache<ResourceKey, AppendResourceFile> appendResourceFiles = CacheBuilder.newBuilder()
        .concurrencyLevel(1)
        .build(new CacheLoader<ResourceKey, AppendResourceFile>() {
          @Override
          public AppendResourceFile load(ResourceKey key) throws Exception {
            return new AppendResourceFile(key);
          }
        });

    @Override
    public void complete() {
      if (!StaticEnvironment.round().errorRaised()) {
        for (AppendResourceFile file : appendResourceFiles.asMap().values()) {
          file.complete();
        }
      }
    }
  }
}
