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

import javax.lang.model.type.TypeKind;
import org.immutables.generator.SourceExtraction;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.immutables.value.Value;
import org.immutables.value.processor.meta.Styles.UsingName.TypeNames;

@Value.Nested
public class Proto {
  private Proto() {}

  @Value.Immutable(builder = false)
  public static abstract class MetaAnnotated {
    @Value.Parameter
    public abstract String name();

    @Value.Parameter
    @Value.Auxiliary
    public abstract Element element();

    @Value.Derived
    @Value.Auxiliary
    public Optional<StyleInfo> style() {
      return StyleMirror.find(element()).transform(ToStyleInfo.FUNCTION);
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

    @Value.Auxiliary
    @Value.Derived
    public String simpleName() {
      return element().getSimpleName().toString();
    }

    protected Reporter report() {
      return Reporter.from(processing()).withElement(element());
    }
  }

  @Value.Immutable
  abstract static class Environment {
    @Value.Parameter
    abstract ProcessingEnvironment processing();

    @Value.Parameter
    abstract Round round();

    @Value.Derived
    StyleInfo defaultStyles() {
      TypeElement typeElement = processing()
          .getElementUtils()
          .getTypeElement(StyleMirror.qualifiedName());

      return ToStyleInfo.FUNCTION.apply(StyleMirror.from(typeElement));
    }

    /**
     * Default type adapters should only be called if {@code Gson.TypeAdapters} annotation is
     * definitely in classpath. Currenlty, it is called by for mongo repository module,
     * which have {@code gson} module as a transitive dependency.
     * @return default type adapters
     */
    @Value.Lazy
    TypeAdaptersMirror defaultTypeAdapters() {
      TypeElement typeElement = Preconditions.checkNotNull(processing()
          .getElementUtils()
          .getTypeElement(TypeAdaptersMirror.qualifiedName()),
          "Processor internal error, @%s is not know to be on the classpath",
          TypeAdaptersMirror.qualifiedName());

      return TypeAdaptersMirror.from(typeElement);
    }

    ValueType composeValue(Protoclass protoclass) {
      return round().composer().compose(protoclass);
    }

    Optional<Protoclass> definedValueProtoclassFor(TypeElement typeElement) {
      return round().definedValueProtoclassFor(typeElement);
    }
  }

  /**
   * Introspection supertype for the {@link DeclaringType} and {@link DeclaringPackage}
   */
  public static abstract class AbstractDeclaring extends Diagnosable {
    public abstract String name();

    public abstract DeclaringPackage packageOf();

    @Value.Lazy
    protected Optional<IncludeMirror> include() {
      return IncludeMirror.find(element());
    }

    public boolean hasInclude() {
      return include().isPresent();
    }

    /** used to intern packaged created internally */
    @Value.Auxiliary
    abstract Round.Interning interner();

    @Value.Lazy
    public Optional<TypeAdaptersMirror> typeAdapters() {
      return TypeAdaptersMirror.find(element());
    }

    @Value.Lazy
    public List<TypeElement> includedTypes() {
      Optional<IncludeMirror> includes = include();

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
    public Optional<StyleInfo> style() {
      Optional<StyleInfo> style = StyleMirror.find(element()).transform(ToStyleInfo.FUNCTION);

      if (style.isPresent()) {
        return style;
      }

      for (AnnotationMirror mirror : element().getAnnotationMirrors()) {
        MetaAnnotated metaAnnotated = MetaAnnotated.from(mirror);
        Optional<StyleInfo> metaStyle = metaAnnotated.style();
        if (metaStyle.isPresent()) {
          return metaStyle;
        }
      }

      if (name().equals("org.immutables.fixture.style")) {
        TypeElement typeElement = environment().processing()
            .getElementUtils().getTypeElement("org.immutables.fixture.style.OutsideBuildable");
        List<? extends AnnotationMirror> annotationMirrors = null;

        if (typeElement != null) {
          environment().processing()
              .getMessager()
              .printMessage(javax.tools.Diagnostic.Kind.MANDATORY_WARNING,
                  "&&& TYPEEL " + typeElement.getAnnotationMirrors());

          annotationMirrors = typeElement.getEnclosingElement().getAnnotationMirrors();
        }

        environment().processing()
            .getMessager()
            .printMessage(javax.tools.Diagnostic.Kind.MANDATORY_WARNING,
                "! NNNOOISH !!! "
                    + name()
                    + "   ANNMIRRORS   "
                    + element().getAnnotationMirrors()
                    + "  /ALT/"
                    + annotationMirrors);
      }

      return Optional.absent();
    }
  }

  @Value.Immutable
  public static abstract class DeclaringPackage extends AbstractDeclaring {

    @Override
    @Value.Auxiliary
    public abstract PackageElement element();

    @Override
    public DeclaringPackage packageOf() {
      return this;
    }

    @Override
    @Value.Auxiliary
    @Value.Derived
    public String simpleName() {
      return element().isUnnamed() ? "" : element().getSimpleName().toString();
    }

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

    @Value.Lazy
    Optional<DeclaringPackage> namedParentPackage() {
      String parentPackageName = SourceNames.parentPackageName(element());
      if (!parentPackageName.isEmpty()) {
        @Nullable PackageElement parentPackage =
            environment().processing()
                .getElementUtils()
                .getPackageElement(parentPackageName);

        if (parentPackage != null) {
          return Optional.of(interner().forPackage(
              ImmutableProto.DeclaringPackage.builder()
                  .environment(environment())
                  .interner(interner())
                  .element(parentPackage)
                  .build()));
        }
      }
      return Optional.absent();
    }

    @Override
    @Value.Lazy
    public Optional<StyleInfo> style() {
      Optional<StyleInfo> style = super.style();
      if (style.isPresent()) {
        return style;
      }
      Optional<DeclaringPackage> parent = namedParentPackage();
      if (parent.isPresent()) {
        return parent.get().style();
      }
      return Optional.absent();
    }
  }

  @Value.Immutable
  public static abstract class DeclaringType extends AbstractDeclaring {
    @Override
    @Value.Auxiliary
    public abstract TypeElement element();

    @Override
    @Value.Derived
    public String name() {
      return element().getQualifiedName().toString();
    }

    /**
     * returns this class if it's top level or enclosing top level type.
     * @return accossiated top level type.
     */
    public DeclaringType associatedTopLevel() {
      return enclosingTopLevel().or(this);
    }

    @Value.Lazy
    public Optional<DeclaringType> enclosingTopLevel() {
      TypeElement top = element();
      for (Element e = top; e.getKind() != ElementKind.PACKAGE; e = e.getEnclosingElement()) {
        top = (TypeElement) e;
      }
      if (top == element()) {
        return Optional.absent();
      }
      return Optional.of(interner().forType(
          ImmutableProto.DeclaringType.builder()
              .environment(environment())
              .interner(interner())
              .element(top)
              .build()));
    }

    @Value.Lazy
    public Optional<RepositoryMirror> repository() {
      return RepositoryMirror.find(element());
    }

    @Value.Lazy
    public Optional<DeclaringType> enclosingOf() {
      Optional<DeclaringType> topLevel = enclosingTopLevel();
      if (topLevel.isPresent() && topLevel.get().isEnclosing()) {
        return topLevel;
      }
      return Optional.absent();
    }

    @Override
    @Value.Derived
    @Value.Auxiliary
    public DeclaringPackage packageOf() {
      Element e = element();
      for (; e.getKind() != ElementKind.PACKAGE; e = e.getEnclosingElement()) {
      }
      return interner().forPackage(
          ImmutableProto.DeclaringPackage.builder()
              .environment(environment())
              .interner(interner())
              .element((PackageElement) e)
              .build());
    }

    @Value.Lazy
    public Optional<ValueImmutableInfo> features() {
      return ImmutableMirror.find(element()).transform(ToImmutableInfo.FUNCTION);
    }

    @Value.Lazy
    public boolean useImmutableDefaults() {
      Optional<ValueImmutableInfo> immutables = features();
      if (immutables.isPresent()) {
        return immutables.get().isDefault();
      }
      return true;
    }

    @Value.Lazy
    public boolean isEnclosing() {
      return EnclosingMirror.isPresent(element());
    }

    /**
     * @return true, if is top level
     */
    @Value.Derived
    @Value.Auxiliary
    public boolean isTopLevel() {
      return element().getNestingKind() == NestingKind.TOP_LEVEL;
    }

    public boolean isImmutable() {
      return features().isPresent();
    }

    public boolean verifiedFactory(ExecutableElement element) {
      if (!FactoryMirror.isPresent(element)) {
        return false;
      }
      if (!isTopLevel()
          || element.getReturnType().getKind() == TypeKind.VOID
          || element.getModifiers().contains(Modifier.PRIVATE)
          || !element.getModifiers().contains(Modifier.STATIC)
          || !element.getThrownTypes().isEmpty()
          || !element.getTypeParameters().isEmpty()) {
        report().withElement(element)
            .annotationNamed(FactoryMirror.simpleName())
            .error("@%s method '%s' should be static, non-private, non-void"
                + " with no type parameters or throws declaration, and enclosed in top level type",
                FactoryMirror.simpleName(),
                element.getSimpleName());
        return false;
      }

      return true;
    }

    /**
     * Some validations, not exhaustive.
     */
    @Value.Check
    protected void validate() {
      if (hasInclude() && !isTopLevel()) {
        report()
            .annotationNamed(IncludeMirror.simpleName())
            .error("@%s could not be used on nested types.", IncludeMirror.simpleName());
      }
      if (isEnclosing() && !isTopLevel()) {
        report()
            .annotationNamed(EnclosingMirror.simpleName())
            .error("@%s should only be used on a top-level types.", EnclosingMirror.simpleName());
      }
      if (isImmutable() && element().getKind() == ElementKind.ENUM) {
        report()
            .annotationNamed(ImmutableMirror.simpleName())
            .error("@%s is not supported on enums", ImmutableMirror.simpleName());
      }
    }

    @Value.Lazy
    public SourceExtraction.Imports sourceImports() {
      return SourceExtraction.readImports(processing(), CachingElements.getDelegate(element()));
    }
  }

  /**
   * Prototypical model for generated derived classes. {@code Protoclass} could be used to projects
   * different kind of derived classes.
   */
  @Value.Immutable
  public static abstract class Protoclass extends Diagnosable {

    @Value.Derived
    public String name() {
      return SourceNames.sourceQualifiedNameFor(sourceElement());
    }

    /**
     * Source type elements stores type element which is used as a source of value type model.
     * It is the annotated class for {@code @Value.Immutable} or type referenced in
     * {@code @Value.Include}.
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
     * The class, which is annotated to be a {@code @Value.Immutable}, {@code @Value.Include} or
     * {@code @Value.Enclosing}.
     * @return declaring type
     */
    public abstract Optional<DeclaringType> declaringType();

    @Value.Lazy
    public Optional<RepositoryMirror> repository() {
      Optional<RepositoryMirror> repositoryMirror =
          kind().isDefinedValue() && declaringType().isPresent()
              ? declaringType().get().repository()
              : Optional.<RepositoryMirror>absent();

      if (repositoryMirror.isPresent()
          && !typeAdaptersProvider().isPresent()
          && kind().isNested()) {
        report().annotationNamed(RepositoryMirror.simpleName())
            .error("@Mongo.%s should also have associated @Gson.%s on a top level type."
                + " For top level immutable adapters would be auto-added",
                RepositoryMirror.simpleName(),
                TypeAdaptersMirror.simpleName());
      }

      return repositoryMirror;
    }

    @Value.Lazy
    public Optional<TypeAdaptersMirror> gsonTypeAdapters() {
      Optional<AbstractDeclaring> typeAdaptersProvider = typeAdaptersProvider();
      if (typeAdaptersProvider.isPresent()) {
        return typeAdaptersProvider.get().typeAdapters();
      }
      if (kind().isDefinedValue()
          && !kind().isNested()
          && repository().isPresent()) {
        return Optional.of(environment().defaultTypeAdapters());
      }
      return Optional.absent();
    }

    @Value.Lazy
    public Optional<AbstractDeclaring> typeAdaptersProvider() {
      Optional<DeclaringType> typeDefining =
          declaringType().isPresent()
              ? Optional.of(declaringType().get().associatedTopLevel())
              : Optional.<DeclaringType>absent();

      Optional<TypeAdaptersMirror> typeDefined =
          typeDefining.isPresent()
              ? typeDefining.get().typeAdapters()
              : Optional.<TypeAdaptersMirror>absent();

      Optional<TypeAdaptersMirror> packageDefined = packageOf().typeAdapters();

      if (packageDefined.isPresent()) {
        if (typeDefined.isPresent()) {
          report()
              .withElement(typeDefining.get().element())
              .annotationNamed(TypeAdaptersMirror.simpleName())
              .warning("@%s is also used on the package, this type level annotation is ignored",
                  TypeAdaptersMirror.simpleName());
        }
        return Optional.<AbstractDeclaring>of(packageOf());
      }

      return typeDefined.isPresent()
          ? Optional.<AbstractDeclaring>of(typeDefining.get())
          : Optional.<AbstractDeclaring>absent();
    }

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
    public ValueImmutableInfo features() {
      if (declaringType().isPresent()
          && !declaringType().get().useImmutableDefaults()) {
        Optional<ValueImmutableInfo> features = declaringType().get().features();
        if (features.isPresent()) {
          return features.get();
        }
      }
      return styles().defaults();
    }

    @Value.Lazy
    public Styles styles() {
      return determineStyle().or(environment().defaultStyles()).getStyles();
    }

    private Optional<StyleInfo> determineStyle() {
      if (declaringType().isPresent()) {
        DeclaringType type = declaringType().get();

        Optional<DeclaringType> enclosing = type.enclosingOf();
        if (enclosing.isPresent()) {
          if (enclosing.get() != type) {
            Optional<StyleInfo> style = type.style();
            if (style.isPresent()) {
              warnAboutIncompatibleStyles();
            }
          }
          Optional<StyleInfo> enclosingStyle = enclosing.get().style();
          if (enclosingStyle.isPresent()) {
            return enclosingStyle;
          }
        } else {
          Optional<StyleInfo> style = type.style();
          if (style.isPresent()) {
            return style;
          }
          Optional<DeclaringType> topLevel = type.enclosingTopLevel();
          if (topLevel.isPresent() && topLevel.get().style().isPresent()) {
            return topLevel.get().style();
          }
        }
      }
      return packageOf().style();
    }

    private void warnAboutIncompatibleStyles() {
      report().annotationNamed(StyleMirror.simpleName())
          .warning("Use styles only on enclosing types."
              + " All nested styles will inherit it."
              + " Nested immutables cannot deviate in style from enclosing type,"
              + " so generated stucture will be consistent");
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

    SourceExtraction.Imports sourceImports() {
      return declaringType().isPresent()
          ? declaringType().get().associatedTopLevel().sourceImports()
          : SourceExtraction.Imports.empty();
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

  enum ToImmutableInfo implements Function<ImmutableMirror, ValueImmutableInfo> {
    FUNCTION;
    @Override
    public ValueImmutableInfo apply(ImmutableMirror input) {
      return ImmutableValueImmutableInfo.theOf(
          input.builder(),
          input.copy(),
          input.intern(),
          input.prehash(),
          input.singleton())
          .withIsDefault(input.getAnnotationMirror().getElementValues().isEmpty());
    }
  }

  enum ToStyleInfo implements Function<StyleMirror, StyleInfo> {
    FUNCTION;
    @Override
    public StyleInfo apply(StyleMirror input) {
      return ImmutableStyleInfo.of(input.get(),
          input.init(),
          input.with(),
          input.add(),
          input.addAll(),
          input.put(),
          input.putAll(),
          input.copyOf(),
          input.of(),
          input.instance(),
          input.builder(),
          input.newBuilder(),
          input.from(),
          input.build(),
          input.typeBuilder(),
          input.typeAbstract(),
          input.typeImmutable(),
          input.typeImmutableEnclosing(),
          input.typeImmutableNested(),
          ToImmutableInfo.FUNCTION.apply(input.defaults()),
          input.strictBuilder(),
          input.visibility(),
          input.jdkOnly());
    }
  }
}
