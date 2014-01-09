/*
    Copyright 2013-2014 Immutables.org authors

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
package org.immutables.generate.internal.processing;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import org.immutables.annotation.GenerateCheck;
import org.immutables.annotation.GenerateDefault;
import org.immutables.annotation.GenerateDerived;
import org.immutables.annotation.GenerateFunction;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateLazy;
import org.immutables.annotation.GenerateModifiable;
import org.immutables.annotation.GeneratePredicate;
import org.immutables.generate.internal.javascript.ClasspathModuleSourceProvider;
import org.immutables.generate.internal.javascript.RhinoInvoker;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class Processor extends AbstractProcessor {

  private static final String GENERATE_MAIN_JS = "org/immutables/generate/template/generate.js";

  static final String GENERATE_IMMUTABLE_ANNOTATION_TYPE =
      GenerateImmutable.class.getName();

  private final RhinoInvoker invoker = new RhinoInvoker(new ClasspathModuleSourceProvider(getClass()));

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return ImmutableSet.of(GENERATE_IMMUTABLE_ANNOTATION_TYPE);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotatedTypes, RoundEnvironment environment) {
    if (!environment.processingOver()) {
      for (Element type : environment.getElementsAnnotatedWith(GenerateImmutable.class)) {
        if (type instanceof TypeElement) {
          processAnnotations((TypeElement) type);
        }
      }
    }
    return true;
  }

  private void processAnnotations(TypeElement type) {
    if (!isGenerateType(type)) {
      error(type, "Type annotated with %s must be top-level 'abstract class'", GENERATE_IMMUTABLE_ANNOTATION_TYPE);
    }

    runSourceCodeGeneration(inspectGenerateType(type), type);
  }

  private static boolean isGenerateType(TypeElement type) {
    return type.getKind() == ElementKind.CLASS
        && type.getEnclosingElement().getKind() == ElementKind.PACKAGE
        && isAbstract(type)
        && type.getAnnotation(GenerateImmutable.class) != null;
  }

  GenerateType inspectGenerateType(TypeElement type) {
    GenerateImmutable annotation = type.getAnnotation(GenerateImmutable.class);

    String packageFullyQualifiedName = ((PackageElement) type.getEnclosingElement()).getQualifiedName().toString();

    String fullyQualifiedTypeName = type.getSimpleName().toString();

    boolean useBuilder = annotation.builder();

    GenerateTypes.Builder typeBuilder =
        GenerateTypes.builder()
            .packageFullyQualifiedName(packageFullyQualifiedName)
            .internalName(fullyQualifiedTypeName)
            .internalTypeElement(type)
            .isUseBuilder(useBuilder)
            .isGenerateCompact(hasAnnotation(type, GenerateModifiable.class));

    for (Element element : type.getEnclosedElements()) {
      if (element.getKind() == ElementKind.METHOD && !element.getModifiers().contains(Modifier.STATIC)) {
        processGenerationCandidateMethod(typeBuilder, (ExecutableElement) element);
      }
    }

    GenerateType generateType = typeBuilder.build();
    return generateType;
  }

  private void processGenerationCandidateMethod(
      GenerateTypes.Builder type,
      ExecutableElement attributeMethodCandidate) {

    Name name = attributeMethodCandidate.getSimpleName();
    List<? extends VariableElement> parameters = attributeMethodCandidate.getParameters();
    if (name.contentEquals("equals")
        && parameters.size() == 1
        && parameters.get(0).asType().toString().equals(Object.class.getName())
        && !attributeMethodCandidate.getModifiers().contains(Modifier.ABSTRACT)) {
      type.isEqualToDefined(true);
      return;
    }

    if (name.contentEquals("hashCode") && parameters.isEmpty()
        && !attributeMethodCandidate.getModifiers().contains(Modifier.ABSTRACT)) {
      type.isHashCodeDefined(true);
      return;
    }

    if (name.contentEquals("toString") && parameters.isEmpty()
        && !attributeMethodCandidate.getModifiers().contains(Modifier.ABSTRACT)) {
      type.isToStringDefined(true);
      return;
    }

    @Nullable
    GenerateCheck validateAnnotation = attributeMethodCandidate.getAnnotation(GenerateCheck.class);
    if (validateAnnotation != null) {
      if (attributeMethodCandidate.getReturnType().getKind() == TypeKind.VOID
          && attributeMethodCandidate.getModifiers().contains(Modifier.PROTECTED)
          && attributeMethodCandidate.getParameters().isEmpty()
          && !attributeMethodCandidate.getModifiers().contains(Modifier.ABSTRACT)
          && !attributeMethodCandidate.getModifiers().contains(Modifier.STATIC)
          && !attributeMethodCandidate.getModifiers().contains(Modifier.NATIVE)) {
        type.validationMethodName(attributeMethodCandidate.getSimpleName().toString());
      } else {
        error(attributeMethodCandidate,
            "Method annotated with @"
                + GenerateCheck.class.getSimpleName()
                + " must be protected parameter-less method and have void return type.");
      }
    }

    if (isGenerateAttribute(attributeMethodCandidate)) {
      TypeMirror returnType = attributeMethodCandidate.getReturnType();

      GenerateAttributes.Builder attributeBuilder = GenerateAttributes.builder();

      if (isAbstract(attributeMethodCandidate)) {
        attributeBuilder.isGenerateAbstract(true);
      } else if (hasAnnotation(attributeMethodCandidate, GenerateDefault.class)) {
        attributeBuilder.isGenerateDefault(true);
      } else if (hasAnnotation(attributeMethodCandidate, GenerateDerived.class)) {
        attributeBuilder.isGenerateDerived(true);
      }

      if (hasAnnotation(attributeMethodCandidate, GeneratePredicate.class)
          && returnType.getKind() == TypeKind.BOOLEAN) {
        attributeBuilder.isGeneratePredicate(true);
      } else if (hasAnnotation(attributeMethodCandidate, GenerateFunction.class)) {
        attributeBuilder.isGenerateFunction(true);
      }

      if (hasAnnotation(attributeMethodCandidate, GenerateLazy.class)) {
        if (isAbstract(attributeMethodCandidate) || isFinal(attributeMethodCandidate)) {
          error(attributeMethodCandidate,
              "Methon annotated with @"
                  + GenerateLazy.class.getSimpleName()
                  + " must be non abstract and non-final");
        } else {
          attributeBuilder.isGenerateLazy(true);
        }
      }

      attributeBuilder.internalName(name.toString());
      attributeBuilder.internalTypeName(returnType.toString());
      attributeBuilder.internalTypeMirror(returnType);

      GenerateAttribute generateAttribute = attributeBuilder.build();
      generateAttribute.setAttributeElement(attributeMethodCandidate);

      type.addAttributes(generateAttribute);
    }
  }

  private static boolean isAbstract(Element element) {
    return element.getModifiers().contains(Modifier.ABSTRACT);
  }

  private static boolean isFinal(Element element) {
    return element.getModifiers().contains(Modifier.FINAL);
  }

  private static boolean isGenerateAttribute(ExecutableElement attributeMethodCandidate) {
    return attributeMethodCandidate.getParameters().isEmpty()
        && attributeMethodCandidate.getReturnType().getKind() != TypeKind.VOID
        && (isAbstract(attributeMethodCandidate) || hasGenerateAnnotation(attributeMethodCandidate));
  }

  private static boolean hasGenerateAnnotation(ExecutableElement attributeMethodCandidate) {
    return hasAnnotation(attributeMethodCandidate, GenerateDefault.class)
        || hasAnnotation(attributeMethodCandidate, GenerateDerived.class)
        || hasAnnotation(attributeMethodCandidate, GeneratePredicate.class)
        || hasAnnotation(attributeMethodCandidate, GenerateLazy.class)
        || hasAnnotation(attributeMethodCandidate, GenerateFunction.class);
  }

  private static boolean hasAnnotation(Element element, Class<? extends Annotation> annotationType) {
    return element.getAnnotation(annotationType) != null;
  }

  private void error(Element type, String message, Object... parameters) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format(message, parameters), type);
  }

  private void runSourceCodeGeneration(GenerateType generateType, TypeElement type) {
    try {
      invoker.executeModuleMain(
          GENERATE_MAIN_JS,
          generateType,
          new GeneratedJavaSinkFactory(processingEnv, type));
    } catch (Exception ex) {
      error(type,
          "Error generating sources from: %s%n%s%n%s",
          type.getQualifiedName(),
          ex.toString(),
          Throwables.getStackTraceAsString(ex));
    }
  }
}
