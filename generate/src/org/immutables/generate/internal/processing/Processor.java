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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
import org.immutables.annotation.GenerateNested;
import org.immutables.annotation.GeneratePredicate;
import org.immutables.generate.internal.javascript.ClasspathModuleSourceProvider;
import org.immutables.generate.internal.javascript.RhinoInvoker;

/**
 * DISCLAIMER: ALL THIS LOGIC IS A PIECE OF CRAP THAT ACCUMULATED OVER TIME.
 * QUALITY OF RESULTED GENERATED CLASSES ALWAYS WAS HIGHEST PRIORITY
 * BUT MODIFIABILITY SUFFERS, SO NEW VERSION WILL REIMPLEMENT IT FROM SCRATCH.
 */
interface A<T extends Object> {}

class B implements A<Integer> {}

class C<N extends Number> implements A<N> {}

@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class Processor extends AbstractProcessor {

  private static final String GENERATE_MAIN_JS = "org/immutables/generate/template/generate.js";

  private final RhinoInvoker invoker = new RhinoInvoker(new ClasspathModuleSourceProvider(getClass()));

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return ImmutableSet.of(GenerateImmutable.class.getName(), GenerateNested.class.getName());
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotationTypes, RoundEnvironment environment) {
/*    Elements elements = processingEnv.getElementUtils();
    Types types = processingEnv.getTypeUtils();

    TypeMirror tA = elements.getTypeElement(A.class.getCanonicalName()).asType();
    TypeMirror tB = elements.getTypeElement(B.class.getCanonicalName()).asType();
    TypeMirror tC = elements.getTypeElement(C.class.getCanonicalName()).asType();

    List<? extends TypeMirror> directSupertypes = types.directSupertypes(tC);
    DeclaredType tCA = (DeclaredType) directSupertypes.get(1);

    throw new RuntimeException("\n\tTypes: " + tB + " and " + tA
    + "\n\tisSubtype: " + types.isSubtype(tB, tA)
    + "\n\terasureIsSubtype: "
    + types.isSubtype(tB, types.erasure(tA))
    + "; both: "
    + types.isSubtype(types.erasure(tB), types.erasure(tA))
    + "\n\ttypeArguments: "
    // + directSupertypes);
    + ((TypeVariable) tCA.getTypeArguments().get(0)).getUpperBound());
*/
    if (!environment.processingOver()) {
      Set<Element> allElemenents = Sets.newLinkedHashSet();
      for (TypeElement annotationType : annotationTypes) {
        allElemenents.addAll(environment.getElementsAnnotatedWith(annotationType));
      }
      List<GenerateType> generateTypes = Lists.newArrayListWithExpectedSize(allElemenents.size());

      for (Element typeElement : allElemenents) {
        if (typeElement instanceof TypeElement) {
          TypeElement type = (TypeElement) typeElement;

          if (type.getEnclosingElement().getAnnotation(GenerateNested.class) == null) {
            collectGenerateTypeDescriptors(generateTypes, type);
          }
        }
      }
      for (GenerateType generateType : generateTypes) {
        runSourceCodeGeneration(generateType);
      }
    }
    return true;
  }

  private void collectGenerateTypeDescriptors(List<GenerateType> generateTypes, TypeElement type) {
    GenerateImmutable genImmutable = type.getAnnotation(GenerateImmutable.class);
    GenerateNested genNested = type.getAnnotation(GenerateNested.class);
    if (genImmutable != null) {
      GenerateType generateType = inspectGenerateType(type, genImmutable);

      if (genNested != null) {
        generateType.setNestedChildren(extractNestedChildren(type));
      }

      generateTypes.add(generateType);
    } else if (genNested != null) {
      List<GenerateType> nestedChildren = extractNestedChildren(type);
      if (!nestedChildren.isEmpty()) {
        GenerateType emptyNestingType = GenerateTypes.builder()
            .internalTypeElement(type)
            .isUseBuilder(false)
            .isGenerateCompact(false)
            .build();

        emptyNestingType.setEmptyNesting(true);
        emptyNestingType.setSegmentedName(SegmentedName.from(type.getQualifiedName()));
        emptyNestingType.setNestedChildren(nestedChildren);

        generateTypes.add(emptyNestingType);
      }
    }
  }

  private List<GenerateType> extractNestedChildren(TypeElement parent) {
    ImmutableList.Builder<GenerateType> children = ImmutableList.builder();
    for (Element element : parent.getEnclosedElements()) {
      switch (element.getKind()) {
      case INTERFACE:
      case CLASS:
        GenerateImmutable annotation = element.getAnnotation(GenerateImmutable.class);
        if (annotation != null) {
          children.add(inspectGenerateType((TypeElement) element, annotation));
        }
        break;
      default:
      }
    }
    return children.build();
  }

  private boolean isGenerateType(TypeElement type, GenerateImmutable annotation) {
    boolean isStaticOrTopLevel =
        type.getKind() == ElementKind.INTERFACE
            || (type.getKind() == ElementKind.CLASS
            && (type.getEnclosingElement().getKind() == ElementKind.PACKAGE || type.getModifiers()
                .contains(Modifier.STATIC)));

    return annotation != null
        && isStaticOrTopLevel
        && isNonFinal(type);
  }

  private boolean isNonFinal(TypeElement type) {
    return !type.getModifiers().contains(Modifier.FINAL);
  }

  GenerateType inspectGenerateType(TypeElement type, GenerateImmutable annotation) {
    if (!isGenerateType(type, annotation)) {
      error(type,
          "Type '%s' annotated with @%s must be non-final class or interface",
          type.getSimpleName(),
          GenerateImmutable.class.getSimpleName());
    }

    SegmentedName segmentedName = SegmentedName.from(processingEnv, type);

    boolean useBuilder = annotation.builder();

    GenerateTypes.Builder typeBuilder =
        GenerateTypes.builder()
            .internalTypeElement(type)
            .isUseBuilder(useBuilder)
            .isGenerateCompact(hasAnnotation(type, GenerateModifiable.class));

    collectGeneratedCandidateMethods(type, typeBuilder);

    GenerateType generateType = typeBuilder.build();
    generateType.setSegmentedName(segmentedName);
    return generateType;
  }

  private void collectGeneratedCandidateMethods(TypeElement type, GenerateTypes.Builder typeBuilder) {
    // TO BE DONE

    for (Element element : processingEnv.getElementUtils().getAllMembers(type)) {
      if (isElegibleCandidateMethod(element)) {
        processGenerationCandidateMethod(typeBuilder, (ExecutableElement) element);
      }
    }
  }

  private boolean isElegibleCandidateMethod(Element element) {
    if (element.getKind() != ElementKind.METHOD) {
      return false;
    }
    if (element.getModifiers().contains(Modifier.STATIC)) {
      return false;
    }
    String definitionType = element.getEnclosingElement().toString();
    if (definitionType.equals("java.lang.Object")) {
      return false;
    }
    if (definitionType.startsWith("org.immutables.common.collect.OrdinalValue")) {
      return false;
    }
    return true;
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
          && attributeMethodCandidate.getParameters().isEmpty()
          && !attributeMethodCandidate.getModifiers().contains(Modifier.PRIVATE)
          && !attributeMethodCandidate.getModifiers().contains(Modifier.ABSTRACT)
          && !attributeMethodCandidate.getModifiers().contains(Modifier.STATIC)
          && !attributeMethodCandidate.getModifiers().contains(Modifier.NATIVE)) {
        type.validationMethodName(attributeMethodCandidate.getSimpleName().toString());
      } else {
        error(attributeMethodCandidate,
            "Method '%s' annotated with @%s must be non-private parameter-less method and have void return type.",
            attributeMethodCandidate.getSimpleName(),
            GenerateCheck.class.getSimpleName());
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
              "Method '%s' annotated with @%s must be non abstract and non-final",
              attributeMethodCandidate.getSimpleName(),
              GenerateLazy.class.getSimpleName());
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

  private void runSourceCodeGeneration(GenerateType generateType) {
    TypeElement type = generateType.internalTypeElement();
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
