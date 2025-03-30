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
package org.immutables.value.processor.meta;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Lists;
import org.immutables.generator.SourceOrdering;
import org.immutables.generator.SourceOrdering.AccessorProvider;
import org.immutables.value.processor.encode.Instantiator;
import org.immutables.value.processor.encode.Instantiator.InstantiationCreator;
import org.immutables.value.processor.meta.Proto.DeclaringType;
import org.immutables.value.processor.meta.Proto.Protoclass;
import org.immutables.value.processor.meta.Reporter.About;
import org.immutables.value.processor.meta.Styles.UsingName.AttributeNames;

import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.List;

final class AccessorAttributesCollector {
  private static final String ORDINAL_ORDINAL_ATTRIBUTE_NAME = "ordinal";
  private static final String ORDINAL_DOMAIN_ATTRIBUTE_NAME = "domain";
  private static final String PARCELABLE_DESCRIBE_CONTENTS_METHOD = "describeContents";
  private static final @Nullable Modifier DEFAULT_MODIFIER;
  static {
    // because we still don't fully go java 8)
    @Nullable Modifier def = null;
    for (Modifier m : Modifier.values()) {
      if (m.name().equals("DEFAULT")) def = m;
    }
    DEFAULT_MODIFIER = def;
  }

  private static final String ORG_ECLIPSE = "org.eclipse";

  static final String EQUALS_METHOD = "equals";
  static final String TO_STRING_METHOD = "toString";
  static final String HASH_CODE_METHOD = "hashCode";

  private final Protoclass protoclass;
  private final ValueType type;
  private final ProcessingEnvironment processing;
  private final List<ValueAttribute> attributes = Lists.newArrayList();
  private final Styles styles;
  private final Reporter reporter;
  private ImmutableListMultimap<String, TypeElement> accessorMapping = ImmutableListMultimap.of();

  private final boolean isEclipseImplementation;
  private boolean hasNonInheritedAttributes;

  AccessorAttributesCollector(Protoclass protoclass, ValueType type) {
    this.protoclass = protoclass;
    this.processing = protoclass.processing();
    this.styles = protoclass.styles();
    this.type = type;
    this.reporter = protoclass.report();
    this.isEclipseImplementation = ProcessingEnvironments.isEclipseImplementation(processing);
  }

  void collect() {
    collectGeneratedCandidateMethods(getTypeElement());

    Instantiator encodingInstantiator = protoclass.encodingInstantiator();
    @Nullable InstantiationCreator instantiationCreator =
        encodingInstantiator.creatorFor((Parameterizable) type.element);

    for (ValueAttribute attribute : attributes) {
      attribute.initAndValidate(instantiationCreator);
    }

    if (instantiationCreator != null) {
      type.additionalImports(instantiationCreator.imports);
    }

    type.attributes.addAll(attributes);
    type.accessorMapping = accessorMapping;
  }

  private TypeElement getTypeElement() {
    return (TypeElement) type.element;
  }

  private void collectGeneratedCandidateMethods(TypeElement type) {
    TypeElement originalType = CachingElements.getDelegate(type);

    List<? extends Element> accessorsInSourceOrder;
    if (originalType.getKind() == ElementKind.ANNOTATION_TYPE) {
      accessorsInSourceOrder = SourceOrdering.getEnclosedElements(originalType);
    } else {
      AccessorProvider provider =
          SourceOrdering.getAllAccessorsProvider(processing.getElementUtils(), processing.getTypeUtils(), originalType);
      accessorsInSourceOrder = provider.get();
      accessorMapping = provider.accessorMapping();
    }

    for (ExecutableElement element : ElementFilter.methodsIn(accessorsInSourceOrder)) {
      if (isElegibleAccessorMethod(element)) {
        processGenerationCandidateMethod(element, originalType);
      }
    }

    // We do this afterward, to observe field flag that can
    // inform use during checking for warnings.
    for (Element element : processing.getElementUtils().getAllMembers(originalType)) {
      if (element.getKind() == ElementKind.METHOD) {
        ExecutableElement e = (ExecutableElement) element;
        String simpleName = element.getSimpleName().toString();
        switch (simpleName) {
        case HASH_CODE_METHOD:
        case TO_STRING_METHOD:
        case EQUALS_METHOD:
          processUtilityCandidateMethod(e, originalType);
          break;
        default:
          if (!e.getTypeParameters().isEmpty()) break;
          boolean hasDefaultModifier = element.getModifiers().contains(DEFAULT_MODIFIER);
          boolean hasStaticModifier = element.getModifiers().contains(Modifier.STATIC);
          boolean hasAbstractModifier = element.getModifiers().contains(Modifier.ABSTRACT);
          if (hasStaticModifier || hasDefaultModifier || !hasAbstractModifier) {
            String definedIn = element.getEnclosingElement().toString();
            if (!styles.style().underrideEquals().isEmpty()
                && styles.style().underrideEquals().equals(simpleName)) {
              if (hasStaticModifier) {
                if (e.getParameters().size() == 2) {
                  this.type.underrideEquals = new ValueType.UnderrideMethod(simpleName, true, definedIn);
                  this.type.isEqualToDefined = true;
                }
              } else {
                if (e.getParameters().size() == 1) {
                  this.type.underrideEquals = new ValueType.UnderrideMethod(simpleName, false, definedIn);
                  this.type.isEqualToDefined = true;
                }
              }
            } else if (!styles.style().underrideHashCode().isEmpty()
                && styles.style().underrideHashCode().equals(simpleName)) {
              if (hasStaticModifier) {
                if (e.getParameters().size() == 1) {
                  this.type.underrideHashCode = new ValueType.UnderrideMethod(simpleName, true, definedIn);
                  this.type.isHashCodeDefined = true;
                }
              } else {
                if (e.getParameters().isEmpty()) {
                  this.type.underrideHashCode = new ValueType.UnderrideMethod(simpleName, false, definedIn);
                  this.type.isHashCodeDefined = true;
                }
              }
            } else if (!styles.style().underrideToString().isEmpty()
                && styles.style().underrideToString().equals(simpleName)) {
              if (hasStaticModifier) {
                if (e.getParameters().size() == 1) {
                  this.type.underrideToString = new ValueType.UnderrideMethod(simpleName, true, definedIn);
                  this.type.isToStringDefined = true;
                }
              } else {
                if (e.getParameters().isEmpty()) {
                  this.type.underrideToString = new ValueType.UnderrideMethod(simpleName, false, definedIn);
                  this.type.isToStringDefined = true;
                }
              }
            }
          }
        }
      }
    }
  }

  private boolean isElegibleAccessorMethod(Element element) {
    if (element.getKind() != ElementKind.METHOD) {
      return false;
    }
    if (element.getModifiers().contains(Modifier.STATIC)) {
      return false;
    }
    if (NonAttributeMirror.isPresent(element)) {
      return false;
    }
    String simpleName = element.getSimpleName().toString();
    switch (simpleName) {
    case HASH_CODE_METHOD:
    case TO_STRING_METHOD:
      return false;
    default:
    }
    if (!type.style().toBuilder().isEmpty()
        && !type.style().strictBuilder()
        && simpleName.equals(type.names().toBuilder())
        && element.getModifiers().contains(Modifier.ABSTRACT)) {
      // When we enable toBuilder generation, then matching abstract toBuilder declarations
      // will not be considered accessors. Any other name signature conficts is up to the user to resolve
      // Note: this is not the full check if to builder will be actually generated (isGenerateToBuilder)
      // but it's good enough for this stage when we don't know all attribute set
      return false;
    }
    String definitionType = element.getEnclosingElement().toString();
    return !definitionType.equals(Object.class.getName())
        && !definitionType.equals(Proto.ORDINAL_VALUE_INTERFACE_TYPE)
        && !definitionType.equals(Proto.PARCELABLE_INTERFACE_TYPE);
  }

  private void processUtilityCandidateMethod(ExecutableElement utilityMethodCandidate, TypeElement originalType) {
    Name name = utilityMethodCandidate.getSimpleName();
    List<? extends VariableElement> parameters = utilityMethodCandidate.getParameters();

    TypeElement definingType = (TypeElement) utilityMethodCandidate.getEnclosingElement();
    boolean nonFinal = !utilityMethodCandidate.getModifiers().contains(Modifier.FINAL);
    boolean nonAbstract = !utilityMethodCandidate.getModifiers().contains(Modifier.ABSTRACT);

    if (isJavaLangObjectType(definingType)) {
      // We ignore methods of java.lang.Object
      return;
    }

    if (name.contentEquals(EQUALS_METHOD)
        && parameters.size() == 1
        && isJavaLangObjectType(parameters.get(0).asType())) {

      if (nonAbstract) {
        type.isEqualToDefined = true;
        type.isEqualToFinal = !nonFinal;

        if (!definingType.equals(originalType) && hasNonInheritedAttributes && nonFinal) {
          report(originalType)
              .warning(About.INCOMPAT,
                  "Type inherits overridden 'equals' method but have some non-inherited attributes."
                  + " Please override 'equals' with abstract method to have it generate. Otherwise override"
                  + " with calling super implementation to use custom implementation");
        }
      }
      return;
    }

    if (name.contentEquals(HASH_CODE_METHOD)
        && parameters.isEmpty()) {
      if (nonAbstract) {
        type.isHashCodeDefined = true;
        type.isHashCodeFinal = !nonFinal;

        // inherited non-abstract implementation
        if (!definingType.equals(originalType) && hasNonInheritedAttributes && nonFinal) {
          report(originalType)
              .warning(About.INCOMPAT,
                  "Type inherits non-default 'hashCode' method but have some non-inherited attributes."
                  + " Please override 'hashCode' with abstract method to have it generated. Otherwise override"
                  + " with calling super implementation to use custom implementation");
        }
      }
      return;
    }

    if (name.contentEquals(TO_STRING_METHOD)
        && parameters.isEmpty()) {
      if (nonAbstract) {
        type.isToStringDefined = true;

        // inherited non-abstract implementation
        if (!definingType.equals(originalType) && hasNonInheritedAttributes && nonFinal) {
          report(originalType)
              .warning(About.INCOMPAT,
                  "Type inherits non-default 'toString' method but have some non-inherited attributes."
                  + " Please override 'toString' with abstract method to have generate it. Otherwise override"
                  + " with calling super implementation to use custom implementation");
        }
      }
      return;
    }
  }

  private boolean isJavaLangObjectType(TypeMirror typeMirror) {
    if (typeMirror.getKind() == TypeKind.DECLARED) {
      Element element = ((DeclaredType) typeMirror).asElement();
      if (element.getKind().isClass()) {
        return isJavaLangObjectType(((TypeElement) element));
      }
    }
    return false;
  }

  private boolean isJavaLangObjectType(TypeElement definingType) {
    return definingType.getQualifiedName().contentEquals(Object.class.getName());
  }

  private void processGenerationCandidateMethod(ExecutableElement attributeMethodCandidate, TypeElement originalType) {
    Name name = attributeMethodCandidate.getSimpleName();

    Reporter reporter = report(attributeMethodCandidate);

    if (CheckMirror.isPresent(attributeMethodCandidate)) {
      if (!attributeMethodCandidate.getParameters().isEmpty()
          || attributeMethodCandidate.getModifiers().contains(Modifier.PRIVATE)
          || attributeMethodCandidate.getModifiers().contains(Modifier.ABSTRACT)
          || attributeMethodCandidate.getModifiers().contains(Modifier.STATIC)
          || attributeMethodCandidate.getModifiers().contains(Modifier.NATIVE)
          || !attributeMethodCandidate.getTypeParameters().isEmpty()) {
        reporter
            .error("Method '%s' annotated with @%s must be non-private parameter-less method",
                name,
                CheckMirror.simpleName());
      } else if (attributeMethodCandidate.getReturnType().getKind() == TypeKind.VOID) {
        type.addNormalizeMethod(name.toString(), false);
      } else if (returnsNormalizedAbstractValueType(attributeMethodCandidate)) {
        type.addNormalizeMethod(name.toString(), true);
      } else {
        reporter
            .error("Method '%s' annotated with @%s must return void or normalized instance of abstract value type",
                name,
                CheckMirror.simpleName());
      }
      return;
    }

    boolean useDefaultAsDefault = type.constitution.style().defaultAsDefault();

    if (isDiscoveredAttribute(attributeMethodCandidate, useDefaultAsDefault)) {
      if (!attributeMethodCandidate.getTypeParameters().isEmpty()) {
        reporter
            .error("Method '%s' cannot have own generic type parameters."
                + " Attribute accessors can only use enclosing type's type variables", name);
        return;
      }

      TypeMirror returnType = resolveReturnType(attributeMethodCandidate);

      ValueAttribute attribute = new ValueAttribute();
      attribute.reporter = this.reporter;
      attribute.returnType = returnType;
      attribute.names = deriveNames(name.toString());
      attribute.element = attributeMethodCandidate;
      attribute.containingType = type;

      boolean isFinal = isFinal(attributeMethodCandidate);
      boolean isAbstract = isAbstract(attributeMethodCandidate);
      boolean defaultAnnotationPresent = DefaultMirror.isPresent(attributeMethodCandidate);
      @Nullable Object constantDefault = DefaultAnnotations.extractConstantDefault(
          reporter, attributeMethodCandidate, returnType);

      boolean derivedAnnotationPresent = DerivedMirror.isPresent(attributeMethodCandidate);

      if (isAbstract) {
        attribute.isGenerateAbstract = true;

        // (constant) default provided by 'default' clause in annotation attribute definition
        if (attributeMethodCandidate.getDefaultValue() != null) {
          attribute.isGenerateDefault = true;
        }

        // Constant default via annotation
        if (constantDefault != null) {
          attribute.isGenerateDefault = true;
          attribute.constantDefault = constantDefault;
        }

        if (defaultAnnotationPresent || derivedAnnotationPresent) {
          if (defaultAnnotationPresent) {
            if (attribute.isGenerateDefault) {
              reporter
                  .annotationNamed(DefaultMirror.simpleName())
                  .warning(About.INCOMPAT,
                      "@Value.Default annotation is superfluous for default annotation attribute");
            } else {
              reporter
                  .annotationNamed(DefaultMirror.simpleName())
                  .error("@Value.Default attribute should have initializer body", name);
            }
          }
          if (derivedAnnotationPresent) {
            if (attribute.isGenerateDefault) {
              reporter
                  .annotationNamed(DerivedMirror.simpleName())
                  .error("@Value.Derived cannot be used with default annotation attribute");
            } else {
              reporter
                  .annotationNamed(DerivedMirror.simpleName())
                  .error("@Value.Derived attribute should have initializer body", name);
            }
          }
        }
      } else if (defaultAnnotationPresent && derivedAnnotationPresent) {
        reporter
            .annotationNamed(DerivedMirror.simpleName())
            .error("Attribute '%s' cannot be both @Value.Default and @Value.Derived", name);
        attribute.isGenerateDefault = true;
      } else if ((defaultAnnotationPresent || derivedAnnotationPresent) && isFinal) {
        reporter
            .error("Annotated attribute '%s' will be overriden and cannot be final", name);
      } else if (defaultAnnotationPresent) {
        attribute.isGenerateDefault = true;

        if (useDefaultAsDefault && attribute.isInterfaceDefaultMethod()) {
          reporter
              .annotationNamed(DefaultMirror.simpleName())
              .warning(About.INCOMPAT,
                  "@Value.Default annotation is superfluous for default annotation attribute"
                      + " when 'defaultAsDefault' style is enabled");
        }
      } else if (derivedAnnotationPresent) {
        attribute.isGenerateDerived = true;
      } else if (useDefaultAsDefault) {
        attribute.isGenerateDefault = attribute.isInterfaceDefaultMethod();
      }

      if (LazyMirror.isPresent(attributeMethodCandidate)) {
        if (isAbstract || isFinal) {
          reporter
              .error("@Value.Lazy attribute '%s' must be non abstract and non-final", name);
        } else if (defaultAnnotationPresent || derivedAnnotationPresent) {
          reporter
              .error("@Value.Lazy attribute '%s' cannot be @Value.Derived or @Value.Default", name);
        } else {
          attribute.isGenerateLazy = true;
          attribute.isGenerateDefault = false;
        }
      }

      attributes.add(attribute);

      // Compute this eagerly here, for no strong reason
      if (attribute.isGenerateDefault) {
        type.defaultAttributesCount++;
      }

      if (attribute.isGenerateDerived) {
        type.derivedAttributesCount++;
      }

      if (attributeMethodCandidate.getEnclosingElement().equals(originalType)) {
        hasNonInheritedAttributes = true;
      }
    }
  }

  private boolean returnsNormalizedAbstractValueType(ExecutableElement validationMethodCandidate) {
    Optional<DeclaringType> declaringType = protoclass.declaringType();
    if (!declaringType.isPresent()) {
      return false;
    }
    TypeStringProvider provider = new TypeStringProvider(
        reporter,
        validationMethodCandidate,
        resolveReturnType(validationMethodCandidate),
        new ImportsTypeStringResolver(declaringType.orNull(), declaringType.orNull()),
        protoclass.constitution().generics().vars(),
        null);
    provider.process();
    String returnTypeName = provider.returnTypeName();
    boolean isCompatibleReturnType =
        protoclass.constitution().typeAbstract().toString().equals(returnTypeName)
            || protoclass.constitution().typeImmutable().toString().equals(returnTypeName);

    if (!isCompatibleReturnType) {
      report(validationMethodCandidate)
          .error("Method '%s' annotated with @%s should have compatible return type to"
              + " be used as normalization method. It should return abstract value type itself"
              + " or immutable generated type (i.e. %s or %s)",
              validationMethodCandidate.getSimpleName(),
              CheckMirror.simpleName(),
              protoclass.constitution().typeAbstract(),
              protoclass.constitution().typeImmutable());
      return false;
    }
    return true;
  }

  private AttributeNames deriveNames(String accessorName) {
    AttributeNames names = styles.forAccessor(accessorName);
    switch (names.raw) {
    case HASH_CODE_METHOD: //$FALL-THROUGH$
    case TO_STRING_METHOD:
      // name could equal reserved method name if template is used
      // like "getToString" accessor -> "toString" attribute
      // then we force literal accessor name as raw name
      return styles.forAccessorWithRaw(accessorName, accessorName);
    case ORDINAL_ORDINAL_ATTRIBUTE_NAME: //$FALL-THROUGH$
    case ORDINAL_DOMAIN_ATTRIBUTE_NAME:
      if (type.isOrdinalValue()) {
        // name could equal reserved method name if template is used
        // like "getOrdinal" accessor -> "ordinal" attribute
        // then we force literal accessor name as raw name.
        // Here we have assumption that actual "ordinal" and "domain" accessors
        // defined in OrdinalValue interface were filtered out beforehand
        return styles.forAccessorWithRaw(accessorName, accessorName);
      }
      break;
    case PARCELABLE_DESCRIBE_CONTENTS_METHOD:
      if (type.isParcelable()) {
        return styles.forAccessorWithRaw(accessorName, accessorName);
      }
      break;
    }
    return names;
  }

  private TypeMirror resolveReturnType(ExecutableElement method) {
    TypeElement typeElement = getTypeElement();
    if (isEclipseImplementation) {
      return method.getReturnType();
    }
    return resolveReturnType(processing, method, typeElement);
  }

  static TypeMirror resolveReturnType(
      ProcessingEnvironment processing,
      ExecutableElement method,
      TypeElement typeElement) {
    method = CachingElements.getDelegate(method);
    TypeMirror returnType = method.getReturnType();

    // We do not support parametrized accessor methods,
    // but we do support inheriting parametrized accessors, which
    // we supposedly parametrized with actual type parameters as
    // our target class could not define formal type parameters also.
    if (returnType.getKind() == TypeKind.TYPEVAR) {
      return asInheritedMemberReturnType(processing, typeElement, method);
    } else if (returnType.getKind() == TypeKind.DECLARED
        || returnType.getKind() == TypeKind.ERROR) {
      if (!((DeclaredType) returnType).getTypeArguments().isEmpty()) {
        return asInheritedMemberReturnType(processing, typeElement, method);
      }
    }
    return returnType;
  }

  static TypeMirror asInheritedMemberReturnType(
      ProcessingEnvironment processing,
      TypeElement typeElement,
      ExecutableElement method) {
    ExecutableType asMethodOfType =
        (ExecutableType) processing.getTypeUtils()
            .asMemberOf((DeclaredType) typeElement.asType(), method);

    return asMethodOfType.getReturnType();
  }

  private static boolean isAbstract(Element element) {
    return element.getModifiers().contains(Modifier.ABSTRACT);
  }

  private static boolean isFinal(Element element) {
    return element.getModifiers().contains(Modifier.FINAL);
  }

  private static boolean isDiscoveredAttribute(ExecutableElement attributeMethodCandidate, boolean isDefaultAsDefault) {
    return attributeMethodCandidate.getParameters().isEmpty()
        && attributeMethodCandidate.getReturnType().getKind() != TypeKind.VOID
        && (isAbstract(attributeMethodCandidate)
            || hasGenerateAnnotation(attributeMethodCandidate)
            || isDefaultAsDefault);
  }

  private static boolean hasGenerateAnnotation(ExecutableElement attributeMethodCandidate) {
    return DefaultMirror.isPresent(attributeMethodCandidate)
        || DerivedMirror.isPresent(attributeMethodCandidate)
        || LazyMirror.isPresent(attributeMethodCandidate);
  }

  private Reporter report(Element type) {
    return Reporter.from(protoclass.processing()).withElement(type);
  }
}
