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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.immutables.generator.AnnotationMirrors;
import org.immutables.value.Value;
import org.immutables.value.processor.meta.Styles.UsingName.TypeNames;

@Value.Nested
public class Proto {
  private Proto() {}

  @Value.Immutable(intern = true, builder = false)
  public static abstract class MetaAnnotated {
    @Value.Parameter
    public abstract String name();

    @Value.Parameter
    @Value.Auxiliary
    public abstract Element element();

    @Value.Derived
    @Value.Auxiliary
    public Optional<StyleMirror> style() {
      return StyleMirror.find(element());
    }

    public static MetaAnnotated from(AnnotationMirror mirror) {
      TypeElement element = (TypeElement) mirror.getAnnotationType().asElement();
      return ImmutableProto.MetaAnnotated.of(
          element.getQualifiedName().toString(),
          element);
    }
  }

  abstract static class Diagnosable {
    /** Element suitable for reporting as a source of declaration which might causing problems. */
    @Value.Auxiliary
    abstract Element element();

    @Value.Auxiliary
    abstract Environment environment();

    ProcessingEnvironment processing() {
      return environment().processing();
    }

    protected Reporter report() {
      return Reporter.from(processing()).withElement(element());
    }
  }

  @Value.Immutable
  abstract static class Environment {
    private static final String STYLE_ANNOTATION = "org.immutables.value.Value.Style";

    @Value.Parameter
    abstract ProcessingEnvironment processing();

    @Value.Derived
    StyleMirror defaultStyles() {
      return StyleMirror.from(processing().getElementUtils().getTypeElement(STYLE_ANNOTATION));
    }
  }

  /**
   * Introspection supertype for the {@link DeclaringType} and {@link DeclaringPackage}
   */
  public static abstract class AbstractDeclaring extends Diagnosable {
    public abstract String name();

    @Value.Lazy
    public List<TypeElement> includedTypes() {
      Optional<IncludeMirror> includes = IncludeMirror.find(element());

      ImmutableList<TypeMirror> typeMirrors = includes.isPresent()
          ? ImmutableList.copyOf(includes.get().valueMirror())
          : ImmutableList.<TypeMirror>of();

      FluentIterable<TypeElement> typeElements = FluentIterable.from(typeMirrors)
          .filter(DeclaredType.class)
          .transform(DeclatedTypeToElement.FUNCTION);

      ImmutableSet<String> uniqueTypeNames = typeElements
          .filter(IsPublic.PREDICATE)
          .transform(ElementToName.FUNCTION)
          .toSet();

      if (uniqueTypeNames.size() != typeMirrors.size()) {
        report().annotationNamed(IncludeMirror.simpleName())
            .warning("Some types were ignored, non-supported for inclusion: duplicates, non declared reference types, non-public");
      }

      return typeElements.toList();
    }

    @Value.Lazy
    public boolean useImmutableDefaults() {
      Optional<ImmutableMirror> immutables = features();
      if (immutables.isPresent()) {
        return immutables.get().getAnnotationMirror().getElementValues().isEmpty();
      }
      return true;
    }

    public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
      return AnnotationMirrors.isAnnotationPresent(
          element().getAnnotationMirrors(),
          annotationType);
    }

    @Value.Derived
    @Value.Auxiliary
    public boolean hasInclude() {
      return IncludeMirror.isPresent(element());
    }

    @Value.Derived
    @Value.Auxiliary
    public boolean isEnclosing() {
      return NestedMirror.isPresent(element());
    }

    /**
     * TODO Move to {@link DeclaringType}?.
     * @return true, if is top level
     */
    @Value.Derived
    @Value.Auxiliary
    public boolean isTopLevel() {
      return element().getEnclosingElement() == null
          || element().getEnclosingElement().getKind() == ElementKind.PACKAGE;
    }

    @Value.Derived
    @Value.Auxiliary
    public Optional<ImmutableMirror> features() {
      return ImmutableMirror.find(element());
    }

    public boolean isImmutable() {
      return features().isPresent();
    }

    @Value.Derived
    @Value.Auxiliary
    public Optional<StyleMirror> style() {
      Optional<StyleMirror> style = StyleMirror.find(element());

      if (style.isPresent()) {
        return style;
      }

      for (AnnotationMirror mirror : element().getAnnotationMirrors()) {
        MetaAnnotated metaAnnotated = MetaAnnotated.from(mirror);
        Optional<StyleMirror> metaStyle = metaAnnotated.style();
        if (metaStyle.isPresent()) {
          return metaStyle;
        }
      }

      return Optional.absent();
    }

    /**
     * Logic honors {@link ElementType} declared on annotation types to avoid some
     * useless checks. But otherwise it's not exhaustive.
     * TODO Move to {@link DeclaringType}?
     */
    @Value.Check
    protected void validate() {
      if (hasInclude() && !isTopLevel()) {
        report().annotationNamed(IncludeMirror.simpleName())
            .error("@Include could not be used on nested types.");
      }
      if (isEnclosing() && !isTopLevel()) {
        report().annotationNamed(NestedMirror.simpleName())
            .error("@Nested should only be used on a top-level types.");
      }
      if (element().getKind() == ElementKind.ENUM) {
        report().error("@Value.* annotations are not supported on enums");
      }
    }
  }

  @Value.Immutable(intern = true)
  public static abstract class DeclaringPackage extends AbstractDeclaring {

    @Override
    @Value.Auxiliary
    public abstract PackageElement element();

    /**
     * Name is the only equivalence attribute. Basically packages are interned by name.
     * @return package name
     */
    @Override
    @Value.Derived
    public String name() {
      return element().isUnnamed() ? "" : element().getQualifiedName().toString();
    }

    public String asPrefix() {
      return element().isUnnamed() ? "" : (name() + ".");
    }
  }

  @Value.Immutable(intern = true)
  public static abstract class DeclaringType extends AbstractDeclaring {
    @Override
    @Value.Auxiliary
    public abstract TypeElement element();

    @Override
    @Value.Derived
    public String name() {
      return element().getQualifiedName().toString();
    }

    @Override
    @Value.Derived
    @Value.Auxiliary
    public boolean isTopLevel() {
      return element().getEnclosingElement().getKind() == ElementKind.PACKAGE;
    }

    @Value.Derived
    @Value.Auxiliary
    public Optional<DeclaringType> enclosingOf() {
      TypeElement top = element();
      for (Element e = element(); e.getKind() != ElementKind.PACKAGE; e = e.getEnclosingElement()) {
        top = (TypeElement) e;
      }
      if (top != element()) {
        ImmutableProto.DeclaringType enclosingType = ImmutableProto.DeclaringType.builder()
            .environment(environment())
            .element(top)
            .build();

        if (enclosingType.isEnclosing()) {
          return Optional.<DeclaringType>of(enclosingType);
        }
      }

      return Optional.absent();
    }

    @Value.Derived
    @Value.Auxiliary
    public DeclaringPackage packageOf() {
      return ImmutableProto.DeclaringPackage.builder()
          .environment(environment())
          .element(processing().getElementUtils().getPackageOf(element()))
          .build();
    }

    public boolean verifiedFactory(ExecutableElement element) {
      if (!BuilderMirror.isPresent(element)) {
        return false;
      }
      if (!isTopLevel()
          || element.getModifiers().contains(Modifier.PRIVATE)
          || !element.getModifiers().contains(Modifier.STATIC)
          || !element.getThrownTypes().isEmpty()
          || !element.getTypeParameters().isEmpty()) {
        report().withElement(element)
            .annotationNamed(BuilderMirror.simpleName())
            .error("@Value.Builder method '%s' should be static, non-private,"
                + " with no type parameters or throws declaration, and enclosed in top level type",
                element.getSimpleName());
        return false;
      }

      return true;
    }
  }

  /**
   * Prototypical model for generated derived classes. {@code Protoclass} could be used to projects
   * different kind of derived classes.
   */
  @Value.Immutable(intern = true)
  public static abstract class Protoclass extends Diagnosable {

    @Value.Derived
    public String name() {
      return SourceNames.sourceQualifiedNameFor(sourceElement());
    }

    /**
     * Source type elements stores type element which is used as a source of value type model.
     * It is the annotated class for {@code @Value.Immutable} or type referenced in
     * {@code @Value.Immutable.Include}.
     * @return source element
     */
    @Value.Auxiliary
    public abstract Element sourceElement();

    /**
     * Declaring package that defines value type (usually by import).
     * Or the package in which {@link #declaringType()} resides.
     * @return declaring package
     */
    public abstract DeclaringPackage packageOf();

    /**
     * The class, which is annotated to be a {@code @Value.Immutable},
     * {@code @Value.Immutable.Include} or {@code @Value.Nested}.
     * @return declaring type
     */
    public abstract Optional<DeclaringType> declaringType();

    /**
     * Kind of protoclass declaration, it specifies how exactly the protoclass was declared.
     * @return definition kind
     */
    public abstract Kind kind();

    @Value.Derived
    public Visibility visibility() {
      return Visibility.of(sourceElement());
    }

    public Visibility declaringVisibility() {
      if (declaringType().isPresent()) {
        return Visibility.of(declaringType().get().element());
      }
      return Visibility.PUBLIC;
    }

    /**
     * Element used mostly for error reporting,
     * real model provided by {@link #sourceElement()}.
     */
    @Value.Derived
    @Value.Auxiliary
    @Override
    public Element element() {
      if (kind().isFactory()) {
        return sourceElement();
      }
      if (declaringType().isPresent()) {
        return declaringType().get().element();
      }
      return packageOf().element();
    }

    @Value.Lazy
    public ImmutableMirror features() {
      if (declaringType().isPresent()
          && !declaringType().get().useImmutableDefaults()) {
        Optional<ImmutableMirror> features = declaringType().get().features();
        if (features.isPresent()) {
          return features.get();
        }
      }
      return styles().defaults();
    }

    @Value.Lazy
    public Styles styles() {
      return Styles.using(inferStyle().or(environment().defaultStyles()));
    }

    private Optional<StyleMirror> inferStyle() {
      if (declaringType().isPresent()) {
        Optional<DeclaringType> enclosing = enclosingOf();
        if (enclosing.isPresent()) {
          if (enclosing.get() != declaringType().get()) {
            warnIfNestedStylePresent();
          }
          Optional<StyleMirror> enclosingStyle = enclosing.get().style();
          if (enclosingStyle.isPresent()) {
            return enclosingStyle;
          }
        } else {
          Optional<StyleMirror> style = declaringType().get().style();
          if (style.isPresent()) {
            return style;
          }
        }
      }
      return packageOf().style();
    }

    private void warnIfNestedStylePresent() {
      Optional<StyleMirror> style = declaringType().get().style();
      if (style.isPresent()) {
        report().annotationNamed(StyleMirror.simpleName())
            .warning("Use styles only on enclosing types."
                + " All nested styles will inherit it."
                + " Nested immutables cannot deviate in style from enclosing type,"
                + " so generated stucture will be consistent");
      }
    }

    @Value.Derived
    @Value.Auxiliary
    public Optional<DeclaringType> enclosingOf() {
      if (declaringType().isPresent()) {
        if (kind().isFactory()) {
          return declaringType();
        }
        if (kind().isNested()) {
          if (kind().isIncluded()) {
            return declaringType();
          }
          return declaringType().get().enclosingOf();
        }
      }
      return Optional.absent();
    }

    TypeNames createTypeNames() {
      return styles().forType(sourceElement().getSimpleName().toString());
    }

    public enum Kind {
      INCLUDED_IN_PACKAGE,
      INCLUDED_ON_TYPE,
      INCLUDED_IN_TYPE,
      DEFINED_FACTORY,
      DEFINED_TYPE,
      DEFINED_AND_ENCLOSING_TYPE,
      DEFINED_ENCLOSING_TYPE,
      DEFINED_NESTED_TYPE;

      public boolean isNested() {
        switch (this) {
        case INCLUDED_IN_TYPE:
        case DEFINED_NESTED_TYPE:
          return true;
        default:
          return false;
        }
      }

      public boolean isIncluded() {
        switch (this) {
        case INCLUDED_IN_PACKAGE:
        case INCLUDED_IN_TYPE:
        case INCLUDED_ON_TYPE:
          return true;
        default:
          return false;
        }
      }

      public boolean isEnclosing() {
        switch (this) {
        case DEFINED_AND_ENCLOSING_TYPE:
        case DEFINED_ENCLOSING_TYPE:
          return true;
        default:
          return false;
        }
      }

      public boolean isValue() {
        switch (this) {
        case INCLUDED_IN_PACKAGE:
        case INCLUDED_ON_TYPE:
        case INCLUDED_IN_TYPE:
        case DEFINED_TYPE:
        case DEFINED_AND_ENCLOSING_TYPE:
        case DEFINED_NESTED_TYPE:
          return true;
        default:
          return false;
        }
      }

      public boolean isDefinedValue() {
        switch (this) {
        case DEFINED_TYPE:
        case DEFINED_AND_ENCLOSING_TYPE:
        case DEFINED_NESTED_TYPE:
          return true;
        default:
          return false;
        }
      }

      public boolean isFactory() {
        return this == DEFINED_FACTORY;
      }

      public boolean isEnclosingOnly() {
        return this == DEFINED_ENCLOSING_TYPE;
      }
    }

    @Value.Lazy
    public Constitution constitution() {
      return ImmutableConstitution.builder()
          .protoclass(this)
          .build();
    }
  }

  enum ElementToName implements Function<TypeElement, String> {
    FUNCTION;
    @Override
    public String apply(TypeElement input) {
      return input.getQualifiedName().toString();
    }
  }

  enum DeclatedTypeToElement implements Function<DeclaredType, TypeElement> {
    FUNCTION;
    @Override
    public TypeElement apply(DeclaredType input) {
      return (TypeElement) input.asElement();
    }
  }

  enum IsPublic implements Predicate<Element> {
    PREDICATE;
    @Override
    public boolean apply(Element input) {
      return input.getModifiers().contains(Modifier.PUBLIC);
    }
  }
}
