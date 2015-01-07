/*
    Copyright 2014 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
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

public final class Output {
  public final Templates.Invokable error = new Templates.Invokable() {
    @Override
    @Nullable
    public Invokable invoke(Invokation invokation, Object... parameters) {
      String message = CharMatcher.WHITESPACE.trimFrom(parameters[0].toString());
      StaticEnvironment.processing().getMessager().printMessage(Diagnostic.Kind.ERROR, message);
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
          toCharSequence(invokation.param(0))));
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

  public final Templates.Invokable java = new Templates.Invokable() {
    @Override
    public Invokable invoke(Invokation invokation, Object... parameters) {
      String packageName = parameters[0].toString();
      String simpleName = parameters[1].toString();
      Invokable body = (Invokable) parameters[2];

      ResourceKey key = new ResourceKey(packageName, simpleName);
      SourceFile javaFile = getFiles().sourceFiles.getUnchecked(key);
      body.invoke(new Invokation(javaFile.consumer));
      javaFile.complete();
      return null;
    }

    @Override
    public int arity() {
      return 3;
    }
  };

  public final Templates.Invokable service = new Templates.Invokable() {
    private static final String META_INF_SERVICES = "META-INF/services/";

    @Override
    public Invokable invoke(Invokation invokation, Object... parameters) {
      String interfaceName = parameters[0].toString();
      Invokable body = (Invokable) parameters[1];

      ResourceKey key = new ResourceKey("", META_INF_SERVICES + interfaceName);
      AppendServiceFile servicesFile = getFiles().appendResourceFiles.getUnchecked(key);
      body.invoke(new Invokation(servicesFile.consumer));
      return null;
    }

    @Override
    public int arity() {
      return 3;
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

  private static class AppendServiceFile {
    private static final Pattern SERVICE_FILE_COMMENT_LINE = Pattern.compile("^\\#.*");

    final ResourceKey key;
    final Templates.CharConsumer consumer = new Templates.CharConsumer();

    AppendServiceFile(ResourceKey key) {
      this.key = key;
    }

    void complete() {
      try {
        writeFile();
      } catch (FilerException ex) {
        throw Throwables.propagate(ex);
      } catch (IOException ex) {
        throw Throwables.propagate(ex);
      }
    }

    private void writeFile() throws IOException {
      LinkedHashSet<String> services = Sets.newLinkedHashSet();
      try {
        FileObject existing = getFiler().getResource(StandardLocation.CLASS_OUTPUT, key.packageName, key.relativeName);
        FluentIterable.from(CharStreams.readLines(existing.openReader(true)))
            .filter(Predicates.not(Predicates.contains(SERVICE_FILE_COMMENT_LINE)))
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
      CharSequence sourceCode = extractSourceCode();
      try (Writer writer = getFiler().createSourceFile(key.toString()).openWriter()) {
        writer.append(sourceCode);
      } catch (IOException ex) {
        if (!identicalFileIsAlreadyGenerated(sourceCode)) {
          throw Throwables.propagate(ex);
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

    final LoadingCache<ResourceKey, AppendServiceFile> appendResourceFiles = CacheBuilder.newBuilder()
        .concurrencyLevel(1)
        .build(new CacheLoader<ResourceKey, AppendServiceFile>() {
          @Override
          public AppendServiceFile load(ResourceKey key) throws Exception {
            return new AppendServiceFile(key);
          }
        });

    @Override
    public void complete() {
      for (AppendServiceFile file : appendResourceFiles.asMap().values()) {
        file.complete();
      }
    }
  }
}
