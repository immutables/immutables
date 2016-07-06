/*
   Copyright 2016 Immutables Authors and Contributors

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
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.eclipse.jdt.internal.compiler.apt.model.ElementImpl;
import org.eclipse.jdt.internal.compiler.apt.model.ExecutableElementImpl;
import org.eclipse.jdt.internal.compiler.apt.model.TypeElementImpl;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

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

  public static CharSequence headerFrom(CharSequence source) {
    if (source.length() != 0) {
      return PostprocessingMachine.collectHeader(source);
    }
    return SourceExtractor.UNABLE_TO_EXTRACT;
  }

  public static Imports importsFrom(CharSequence source) {
    if (source.length() != 0) {
      return PostprocessingMachine.collectImports(source);
    }
    return Imports.empty();
  }

  public static CharSequence extract(ProcessingEnvironment processing, TypeElement element) {
    try {
      return EXTRACTOR.extract(processing, element);
    } catch (UnsupportedOperationException | IllegalArgumentException cannotReadSourceFile) {
      return SourceExtractor.UNABLE_TO_EXTRACT;
    } catch (IOException cannotReadSourceFile) {
      processing.getMessager().printMessage(
          Diagnostic.Kind.MANDATORY_WARNING,
          String.format("Was unable to read source file for %s[%s.class]: %s",
              element,
              element.getClass().getName(),
              cannotReadSourceFile));
    }
    return SourceExtractor.UNABLE_TO_EXTRACT;
  }

  interface SourceExtractor {
    CharSequence UNABLE_TO_EXTRACT = "";

    boolean claim(Element element);

    CharSequence extract(ProcessingEnvironment environment, TypeElement typeElement) throws IOException;

    CharSequence extractReturnType(ExecutableElement executableElement);
  }

  private static final class DefaultExtractor implements SourceExtractor {
    static final DefaultExtractor INSTANCE = new DefaultExtractor();

    @Override
    public CharSequence extract(ProcessingEnvironment environment, TypeElement element) throws IOException {
      try {
        FileObject resource = environment.getFiler().getResource(
            StandardLocation.SOURCE_PATH, "", toFilename(element));

        return resource.getCharContent(true);
      } catch (UnsupportedOperationException | IllegalArgumentException ex) {
        return UNABLE_TO_EXTRACT;
      }
    }

    private String toFilename(TypeElement element) {
      return element.getQualifiedName().toString().replace('.', '/') + ".java";
    }

    @Override
    public CharSequence extractReturnType(ExecutableElement executableElement) {
      return UNABLE_TO_EXTRACT;
    }

    @Override
    public boolean claim(Element element) {
      return false;
    }
  }

  private static final class JavacSourceExtractor implements SourceExtractor {

    @Override
    public boolean claim(Element element) {
      return element instanceof ClassSymbol;
    }

    @Override
    public CharSequence extract(ProcessingEnvironment environment, TypeElement typeElement) throws IOException {
      if (typeElement instanceof ClassSymbol) {
        ClassSymbol classSymbol = (ClassSymbol) typeElement;
        if (classSymbol.sourcefile != null) {
          return classSymbol.sourcefile.getCharContent(true);
        }
      }
      return UNABLE_TO_EXTRACT;
    }

    @Override
    public CharSequence extractReturnType(ExecutableElement executableElement) {
      return UNABLE_TO_EXTRACT;
    }
  }

  private static final class EclipseSourceExtractor implements SourceExtractor {
    private static final Splitter DECLARATION_SPLITTER =
        Splitter.on(CharMatcher.WHITESPACE.or(CharMatcher.is(',')))
            .omitEmptyStrings()
            .trimResults();

    @Override
    public boolean claim(Element element) {
      return element instanceof ElementImpl;
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
      return UNABLE_TO_EXTRACT;
    }

    @Override
    public CharSequence extractReturnType(ExecutableElement executableElement) {
      if (executableElement instanceof ExecutableElementImpl) {
        Binding binding = ((ExecutableElementImpl) executableElement)._binding;
        if (binding instanceof MethodBinding) {
          MethodBinding methodBinding = (MethodBinding) binding;

          @Nullable AbstractMethodDeclaration sourceMethod = methodBinding.sourceMethod();
          if (sourceMethod != null) {
            CharSequence rawType = getRawType(methodBinding);
            char[] content = sourceMethod.compilationResult.compilationUnit.getContents();

            int sourceEnd = methodBinding.sourceStart();// intentionaly
            int sourceStart = scanForTheSourceStart(content, sourceEnd);

            char[] methodTest = Arrays.copyOfRange(content, sourceStart, sourceEnd);

            Entry<String, List<String>> extracted =
                SourceTypes.extract(String.valueOf(methodTest));

            return SourceTypes.stringify(
                Maps.immutableEntry(rawType.toString(), extracted.getValue()));
          }
        }
      }
      return UNABLE_TO_EXTRACT;
    }

    private int scanForTheSourceStart(char[] content, int sourceEnd) {
      int i = sourceEnd;
      for (; i >= 0; i--) {
        char c = content[i];
        // FIXME how else I can scan?
        if (c == '\n') {
          return i;
        }
      }
      return i;
    }

    private static CharSequence extractSuperclass(SourceTypeBinding binding) {
      CharSequence declaration = readSourceDeclaration(binding);
      Iterator<String> iterator = DECLARATION_SPLITTER.split(declaration).iterator();
      while (iterator.hasNext()) {
        String token = iterator.next();
        if (token.equals("extends")) {
          return readSourceSuperclass(iterator);
        }
      }
      return UNABLE_TO_EXTRACT;
    }

    private static CharSequence readSourceSuperclass(Iterator<String> declarationParts) {
      StringBuilder superclass = new StringBuilder();
      while (declarationParts.hasNext()) {
        String part = declarationParts.next();
        if (superclass.length() == 0
            || part.charAt(0) == '.'
            || superclass.charAt(superclass.length() - 1) == '.') {
          superclass.append(part);
        } else {
          break;
        }
      }
      return superclass;
    }

    private static CharSequence readSourceDeclaration(SourceTypeBinding binding) {
      TypeDeclaration referenceContext = binding.scope.referenceContext;
      char[] content = referenceContext.compilationResult.compilationUnit.getContents();
      int start = referenceContext.declarationSourceStart;
      int end = referenceContext.declarationSourceEnd;

      StringBuilder declaration = new StringBuilder();
      for (int p = start; p <= end; p++) {
        char c = content[p];
        if (c == '{') {
          break;
        }
        declaration.append(c);
      }
      return declaration;
    }

    private static CharSequence getRawType(MethodBinding methodBinding) {
      TypeBinding returnType = methodBinding.returnType;
      char[] sourceName = returnType.sourceName();
      if (sourceName == null) {
        sourceName = new char[] {};
      }
      return CharBuffer.wrap(sourceName);
    }
  }

  private static final class CompositeExtractor implements SourceExtractor {
    private final SourceExtractor[] extractors;

    CompositeExtractor(List<SourceExtractor> extractors) {
      this.extractors = extractors.toArray(new SourceExtractor[extractors.size()]);
    }

    @Override
    public boolean claim(Element element) {
      return false;
    }

    @Override
    public CharSequence extract(ProcessingEnvironment environment, TypeElement typeElement) throws IOException {
      for (SourceExtractor extractor : extractors) {
        CharSequence source = extractor.extract(environment, typeElement);
        if (!source.equals(UNABLE_TO_EXTRACT)) {
          return source;
        }
        if (extractor.claim(typeElement)) {
          // cannot extract, but an extractor knows for sure
          // that it was his case which it may claim, then no
          // need to consult other extractors, which actually might
          // cause additional trouble.
          return UNABLE_TO_EXTRACT;
        }
      }
      return DefaultExtractor.INSTANCE.extract(environment, typeElement);
    }

    @Override
    public CharSequence extractReturnType(ExecutableElement executableElement) {
      for (SourceExtractor extractor : extractors) {
        CharSequence source = extractor.extractReturnType(executableElement);
        if (!source.equals(UNABLE_TO_EXTRACT)) {
          return source;
        }
        if (extractor.claim(executableElement)) {
          // cannot extract, but an extractor knows for sure
          // that it was his case which it may claim, then no
          // need to consult other extractors, which actually might
          // cause additional trouble.
          return UNABLE_TO_EXTRACT;
        }
      }
      return DefaultExtractor.INSTANCE.extractReturnType(executableElement);
    }
  }

  private static SourceExtractor createExtractor() {
    if (Compiler.ECJ.isPresent() || Compiler.JAVAC.isPresent()) {
      List<SourceExtractor> extractors = Lists.newArrayListWithCapacity(2);
      if (Compiler.ECJ.isPresent()) {
        extractors.add(new EclipseSourceExtractor());
      }
      if (Compiler.JAVAC.isPresent()) {
        extractors.add(new JavacSourceExtractor());
      }
      if (extractors.size() == 1) {
        return extractors.get(0);
      }
      return new CompositeExtractor(extractors);
    }
    return DefaultExtractor.INSTANCE;
  }

  private static final SourceExtractor EXTRACTOR = createExtractor();

  public static CharSequence getReturnTypeString(ExecutableElement method) {
    return EXTRACTOR.extractReturnType(method);
  }

  public static String getSuperclassString(TypeElement element) {
    if (Compiler.ECJ.isPresent()) {
      if (element instanceof TypeElementImpl) {
        TypeElementImpl elementImpl = ((TypeElementImpl) element);
        if (elementImpl._binding instanceof SourceTypeBinding) {
          return EclipseSourceExtractor.extractSuperclass((SourceTypeBinding) elementImpl._binding).toString();
        }
      }
    }
    return element.getSuperclass().toString();
  }
}
