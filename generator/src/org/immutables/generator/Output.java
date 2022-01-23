/*
   Copyright 2014-2018 Immutables Authors and Contributors

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

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.immutables.generator.Templates.Invokable;
import org.immutables.generator.Templates.Invokation;
import static com.google.common.base.Preconditions.checkNotNull;

public final class Output {
  public static final String NO_IMPORTS = "//-no-import-rewrite";

  public final Templates.Invokable error = new Templates.Invokable() {
    @Override
    @Nullable
    public Invokable invoke(Invokation invokation, Object... parameters) {
      Messager messager = EnvironmentState.processing().getMessager();
      String message = CharMatcher.whitespace().trimFrom(parameters[parameters.length - 1].toString());
      Element element = (Element) Iterators.find(
          Iterators.forArray(parameters),
          Predicates.instanceOf(Element.class),
          null);
      if (element != null) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
      } else {
        messager.printMessage(Diagnostic.Kind.ERROR, message);
      }
      return null;
    }
  };

  public final Templates.Invokable system = new Templates.Invokable() {
    @Override
    @Nullable
    public Invokable invoke(Invokation invokation, Object... parameters) {
      String message = CharMatcher.whitespace().trimFrom(parameters[0].toString());
      // this is not a debug line, we are printing to system out
      System.out.println(message);
      return null;
    }
  };

  public final Templates.Invokable length = new Templates.Invokable() {
    @Override
    @Nullable
    public Invokable invoke(Invokation invokation, Object... parameters) {
      invokation.out(parameters[0].toString().length());
      return null;
    }
  };

  private static abstract class OutputFilter extends Templates.Fragment {
    public OutputFilter() {
      super(1);
    }

    abstract void apply(Invokation invokation, CharSequence content, @Nullable Templates.Invokable original);

    @Override
    public final void run(Invokation invokation) {
      Object param = invokation.param(0);
      Invokable original = param instanceof Templates.Invokable
          ? (Templates.Invokable) param
          : null;

      apply(invokation, toCharSequence(param), original);
    }

    private CharSequence toCharSequence(Object param) {
      checkNotNull(param);
      // Is it worthwhile optimization?
      if (param instanceof Templates.Fragment) {
        return ((Templates.Fragment) param).toCharSequence();
      }
      return param.toString();
    }
  }

  public final Templates.Invokable trim = new OutputFilter() {
    @Override
    void apply(Invokation invokation, CharSequence content, @Nullable Templates.Invokable original) {
      invokation.out(CharMatcher.whitespace().trimFrom(content));
    }
  };

  public final Templates.Invokable linesShortable = new OutputFilter() {
    private static final int LIMIT = 100;

    @Override
    void apply(Invokation invokation, CharSequence content, @Nullable Templates.Invokable original) {
      String collapsed = CharMatcher.whitespace().trimAndCollapseFrom(content, ' ');
      int estimatedLimitOnThisLine = LIMIT - invokation.consumer.getCurrentIndentation().length();

      if (collapsed.length() < estimatedLimitOnThisLine) {
        invokation.out(collapsed);
      } else {
        if (original != null) {
          original.invoke(invokation);
        } else {
          invokation.out(content);
        }
      }
    }
  };

  public final Templates.Invokable collapsible = new OutputFilter() {
    @Override
    void apply(Invokation invokation, CharSequence content, @Nullable Templates.Invokable original) {
      boolean hasNonWhitespace = !CharMatcher.whitespace().matchesAllOf(content);
      if (hasNonWhitespace) {
        if (original != null) {
          original.invoke(invokation);
        } else {
          invokation.out(content);
        }
      }
    }
  };

  public final Templates.Invokable java = new Templates.Invokable() {
    @Override
    public Invokable invoke(Invokation invokation, Object... parameters) {
      String packageName = parameters[0].toString();
      String simpleName = parameters[1].toString();
      Element originatingElement = (Element) parameters[2];
      Invokable body = (Invokable) parameters[3];

      ResourceKey key = new ResourceKey(packageName, simpleName, Delegated.unwrap(originatingElement));
      SourceFile javaFile = getFiles().sourceFiles.get(key);
      body.invoke(new Invokation(javaFile.consumer));
      return null;
    }
  };

  public final Templates.Invokable service = new Templates.Invokable() {
    private static final String META_INF_SERVICES = "META-INF/services/";

    @Override
    public Invokable invoke(Invokation invokation, Object... parameters) {
      String interfaceName = parameters[0].toString();
      Invokable body = (Invokable) parameters[1];

      ResourceKey key = new ResourceKey("", META_INF_SERVICES + interfaceName);
      AppendServiceFile servicesFile = getServiceFiles().appendResourceFiles.get(key);
      body.invoke(new Invokation(servicesFile.consumer));
      return null;
    }
  };

  private final static class ResourceKey {
    private static final Joiner PACKAGE_RESOURCE_JOINER = Joiner.on('.').skipNulls();

    final String packageName;
    final String relativeName;
    // not included in equals/hashcode
    final @Nullable Element originatingElement;

    ResourceKey(String packageName, String simpleName) {
      this(packageName, simpleName, null);
    }

    ResourceKey(String packageName, String simpleName, @Nullable Element originatingElement) {
      this.packageName = checkNotNull(packageName);
      this.relativeName = checkNotNull(simpleName);
      this.originatingElement = originatingElement;
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
      } catch (Exception ex) {
        EnvironmentState.processing().getMessager().printMessage(
            Diagnostic.Kind.MANDATORY_WARNING,
            "Cannot write service files: " + key + ex.toString());
      }
    }

    private void writeFile() throws IOException {
      LinkedHashSet<String> services = Sets.newLinkedHashSet();
      readExistingEntriesInto(services);
      copyNewMetaservicesInto(services);
      removeBlankLinesIn(services);
      writeLinesFrom(services);
    }

    private void readExistingEntriesInto(Collection<String> services) {
      try {
        FileObject existing = getFiler()
            .getResource(StandardLocation.CLASS_OUTPUT, key.packageName, key.relativeName);
        try (Reader r = existing.openReader(true)) {
          for (String line : CharStreams.readLines(r)) {
            if (!SERVICE_FILE_COMMENT_LINE.matcher(line).find()) {
              services.add(line.trim());
            }
          }
        }
      } catch (Exception ex) {
        // unable to read existing file
      }
    }

    private void writeLinesFrom(Iterable<String> services) throws IOException {
      FileObject resource = getFiler().createResource(StandardLocation.CLASS_OUTPUT, key.packageName, key.relativeName);
      try (Writer w = resource.openWriter()) {
        for (String line : services) {
          w.write(line);
          w.write('\n');
        }
      }
    }

    private void removeBlankLinesIn(Iterable<String> services) {
      Iterables.removeIf(services, Predicates.equalTo(""));
    }

    private void copyNewMetaservicesInto(Collection<String> services) {
      for (String line : Splitter.on("\n").split(consumer.asCharSequence())) {
        services.add(line);
      }
    }
  }

  private static class SourceFile {
    /**
     * Pragma-like comment indicator just at beginning of the source file used to disable import
     * post-processing.
     */
    final ResourceKey key;
    final Templates.CharConsumer consumer = new Templates.CharConsumer();

    SourceFile(ResourceKey key) {
      this.key = key;
    }

    void complete() {
      CharSequence sourceCode = extractSourceCode();
      try {
        JavaFileObject sourceFile = key.originatingElement != null
            ? getFiler().createSourceFile(key.toString(), key.originatingElement)
            : getFiler().createSourceFile(key.toString());

        try (Writer writer = sourceFile.openWriter()) {
          writer.append(sourceCode);
          writer.flush();
        }
      } catch (FilerException ex) {
        if (identicalFileIsAlreadyGenerated(sourceCode)) {
          getMessager().printMessage(Kind.NOTE, "Regenerated file with the same content: " + key);
        } else {
          getMessager().printMessage(Kind.MANDATORY_WARNING,
              String.format(
                  "Generated source file name collision. Attempt to overwrite already generated file: %s, %s."
                      + " If this happens when using @Value.Immutable on same-named nested classes in the same package,"
                      + " use can use @Value.Enclosing annotation to provide some sort of namespacing",
                  key,
                  ex));
        }
      } catch (IOException ex) {
        throw Throwables.propagate(ex);
      }
    }

    private boolean identicalFileIsAlreadyGenerated(CharSequence sourceCode) {
      try {
        String packagePath = !key.packageName.isEmpty() ? (key.packageName.replace('.', '/') + '/') : "";
        String filename = key.relativeName + ".java";
        FileObject resource = getFiler().getResource(StandardLocation.SOURCE_OUTPUT, "", packagePath + filename);

        String existingContent;
        try (Reader r = resource.openReader(true)) {
          existingContent = CharStreams.toString(r);
        }

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

    private CharSequence extractSourceCode() {
      CharSequence charSequence = consumer.asCharSequence();
      if (Strings.commonPrefix(charSequence, NO_IMPORTS).startsWith(NO_IMPORTS)) {
        return charSequence;
      }
      return PostprocessingMachine.rewrite(charSequence);
    }
  }

  private static Filer getFiler() {
    return EnvironmentState.processing().getFiler();
  }

  private static Messager getMessager() {
    return EnvironmentState.processing().getMessager();
  }

  private static Files getFiles() {
    return EnvironmentState.getPerRound(Files.class, FilesSupplier.INSTANCE);
  }

  private enum FilesSupplier implements Supplier<Files> {
    INSTANCE;

    @Override
    public Files get() {
      return new Files();
    }
  }

  private static ServiceFiles getServiceFiles() {
    return EnvironmentState.getPerProcessing(ServiceFiles.class, ServiceFilesSupplier.INSTANCE);
  }

  private enum ServiceFilesSupplier implements Supplier<ServiceFiles> {
    INSTANCE;

    @Override
    public ServiceFiles get() {
      return new ServiceFiles();
    }
  }

  // Do not use guava cache to slim down minimized jar
  @NotThreadSafe
  private static abstract class Cache<K, V> {
    private final Map<K, V> map = new HashMap<>();

    protected abstract V load(K key) throws Exception;

    final V get(K key) {
      @Nullable V value = map.get(key);
      if (value == null) {
        try {
          value = load(key);
        } catch (Exception ex) {
          throw Throwables.propagate(ex);
        }
        map.put(key, value);
      }
      return value;
    }

    public Map<K, V> asMap() {
      return map;
    }
  }

  private static class Files implements Runnable {
    final Cache<ResourceKey, SourceFile> sourceFiles = new Cache<ResourceKey, SourceFile>() {
      @Override
      public SourceFile load(ResourceKey key) throws Exception {
        return new SourceFile(key);
      }
    };

    @Override
    public void run() {
      for (SourceFile file : sourceFiles.asMap().values()) {
        file.complete();
      }
    }
  }

  private static class ServiceFiles implements Runnable {
    final Cache<ResourceKey, AppendServiceFile> appendResourceFiles = new Cache<ResourceKey, AppendServiceFile>() {
      @Override
      public AppendServiceFile load(ResourceKey key) throws Exception {
        return new AppendServiceFile(key);
      }
    };

    @Override
    public void run() {
      for (AppendServiceFile file : appendResourceFiles.asMap().values()) {
        file.complete();
      }
    }
  }
}
