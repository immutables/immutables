package org.immutables.generator;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
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
      this.all = ImmutableSet.copyOf(all);
      this.classes = ImmutableMap.copyOf(classes);
    }

    public static Imports of(Set<String> all, Map<String, String> classes) {
      if (all.isEmpty() && classes.isEmpty()) {
        return EMPTY;
      }
      if (!all.containsAll(classes.values())) {
        // This check initially appeared as some imports might be skipped,
        // but all classes imported are tracked, but it should be not a problem
      }
      return new Imports(all, classes);
    }

    public static Imports empty() {
      return EMPTY;
    }

    public boolean isEmpty() {
      return this == EMPTY;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("all", all)
          .add("classes", classes)
          .toString();
    }
  }

  public static Imports readImports(ProcessingEnvironment environment, TypeElement element) {
    try {
      return PostprocessingMachine.collectImports(
          SourceExtraction.extract(environment, element));

    } catch (IOException cannotReadSourceFile) {
      environment.getMessager().printMessage(
          Diagnostic.Kind.MANDATORY_WARNING,
          String.format("Could not read source files to collect imports for %s: %s", element, cannotReadSourceFile));

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

      return resource.getCharContent(true);
    }

    private String toFilename(TypeElement element) {
      return element.getQualifiedName().toString().replace('.', '/') + ".java";
    }
  };

  private static final SourceExtractor EXTRACTOR = createExtractor();

  private static final class JavacSourceExtractor implements SourceExtractor {
    // Triggers loading of class that may be absent in classpath
    static {
      ClassSymbol.class.getCanonicalName();
    }

    @Override
    public CharSequence extract(ProcessingEnvironment environment, TypeElement typeElement) throws IOException {
      if (typeElement instanceof ClassSymbol) {
        JavaFileObject sourceFile = ((ClassSymbol) typeElement).sourcefile;
        return sourceFile.getCharContent(true);
      }
      return DEFAULT_EXTRACTOR.extract(environment, typeElement);
    }
  }

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
    } catch (Throwable eclipseClassesNotAwailable) {
    }
    try {
      return new JavacSourceExtractor();
    } catch (Throwable javacClassesNotAwailable) {
    }
    return DEFAULT_EXTRACTOR;
  }
}
