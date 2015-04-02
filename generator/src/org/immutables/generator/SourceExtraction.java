package org.immutables.generator;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.eclipse.jdt.internal.compiler.apt.model.ElementImpl;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;

public final class SourceExtraction {
  private SourceExtraction() {}

  public static final class Imports {
    private static final Imports EMPTY = new Imports(
        ImmutableSet.<String>of(),
        ImmutableMap.<String, String>of());

    public final ImmutableSet<String> all;
    public final ImmutableMap<String, String> classes;

    private Imports(Set<String> all, Map<String, String> classes) {
      Preconditions.checkState(all.size() >= classes.size());
      this.all = ImmutableSet.copyOf(all);
      this.classes = ImmutableMap.copyOf(classes);
    }

    public static Imports of(Set<String> all, Map<String, String> classes) {
      if (all.isEmpty()) {
        return EMPTY;
      }
      return new Imports(all, classes);
    }

    public static Imports empty() {
      return EMPTY;
    }

    public boolean isEmpty() {
      return this == EMPTY;
    }
  }

  public static Imports readImports(ProcessingEnvironment environment, TypeElement element) {
    try {
      Imports imports = PostprocessingMachine.collectImports(SourceExtraction.extract(environment, element));
      return imports;
    } catch (IOException ex) {
      return Imports.empty();
    }
  }

  public static CharSequence extract(ProcessingEnvironment environment, TypeElement element) throws IOException {
    return EXTRACTOR.extract(environment, element);
  }

  interface SourceExtractor {
    CharSequence extract(ProcessingEnvironment environment, TypeElement typeElement) throws IOException;
  }

  private static final SourceExtractor DEFAULT_EXTRACTOR = new SourceExtractor() {
    @Override
    public CharSequence extract(ProcessingEnvironment environment, TypeElement element) throws IOException {
      FileObject resource = environment.getFiler().getResource(
          StandardLocation.SOURCE_PATH, "", toFilename(element));

      try (Reader reader = resource.openReader(true)) {
        return CharStreams.toString(reader);
      }
    }

    private String toFilename(TypeElement element) {
      return element.getQualifiedName().toString().replace('.', '/') + ".java";
    }
  };

  private static final SourceExtractor EXTRACTOR = createExtractor();

  private static final class EclipseSourceExtractor implements SourceExtractor {
    // Triggers loading of class that may be absent in classpath
    static {
      ElementImpl.class.getCanonicalName();
    }

    @Override
    public CharSequence extract(ProcessingEnvironment environment, TypeElement typeElement) throws IOException {
      if (typeElement instanceof ElementImpl) {
        Binding binding = ((ElementImpl) typeElement)._binding;
        if (binding instanceof SourceTypeBinding) {
          CompilationUnitDeclaration unit = ((SourceTypeBinding) binding).scope.referenceCompilationUnit();
          char[] contents = unit.compilationResult.compilationUnit.getContents();
          return CharBuffer.wrap(contents);
        }
      }
      return DEFAULT_EXTRACTOR.extract(environment, typeElement);
    }
  }

  private static SourceExtractor createExtractor() {
    try {
      return new EclipseSourceExtractor();
    } catch (Throwable ex) {
    }
    return DEFAULT_EXTRACTOR;
  }
}
