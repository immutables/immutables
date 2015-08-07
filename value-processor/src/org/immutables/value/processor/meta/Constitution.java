/*
    Copyright 2014-2015 Immutables Authors and Contributors

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

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import org.immutables.generator.Naming;
import org.immutables.generator.Naming.Preference;
import org.immutables.generator.SourceExtraction;
import org.immutables.value.Value;
import org.immutables.value.processor.meta.Proto.DeclaringType;
import org.immutables.value.processor.meta.Proto.Protoclass;
import org.immutables.value.processor.meta.Styles.UsingName.TypeNames;
import static com.google.common.base.Verify.*;

@Value.Nested
@Value.Immutable
public abstract class Constitution {
  private static final String NA_ERROR = "!should_not_be_used_in_generated_code!";
  private static final String NEW_KEYWORD = "new";
  private static final Joiner DOT_JOINER = Joiner.on('.').skipNulls();

  public abstract Protoclass protoclass();

  @Value.Derived
  public Visibility implementationVisibility() {
    return protoclass().visibility().forImplementation(style().visibility());
  }

  public boolean isImplementationHidden() {
    return implementationVisibility().isPrivate();
  }

  public boolean returnsAbstractValueType() {
    return isImplementationHidden()
        || style().visibility() == ValueMirrors.Style.ImplementationVisibility.SAME_NON_RETURNED;
  }

  @Value.Derived
  public TypeNames names() {
    return protoclass().createTypeNames();
  }

  @Value.Lazy
  public NameForms typeDocument() {
    if (protoclass().kind().isValue()) {
      return isAbstractPrimary()
          ? typeAbstract()
          : typeImmutable();
    }
    return typeValue();
  }

  private boolean isAbstractPrimary() {
    return returnsAbstractValueType()
        || !protoclass().visibility().isMoreRestrictiveThan(implementationVisibility());
  }

  public boolean isSimple() {
    return protoclass().kind().isValue()
        && !protoclass().kind().isNested()
        && implementationVisibility().isPublic()
        && !returnsAbstractValueType();
  }

  /**
   * Value is the canonical outside look of the value type. It should be either
   * {@link #typeAbstract()} or {@link #typeImmutable()}.
   * For factory it is a special surrogate.
   * @return canonical value type name forms
   */
  @Value.Lazy
  public NameForms typeValue() {
    if (protoclass().kind().isValue()) {
      return returnsAbstractValueType()
          ? typeAbstract()
          : typeImmutable();
    }
    if (isFactory()) {
      ExecutableElement method = (ExecutableElement) protoclass().sourceElement();
      String type = method.getReturnType().toString();

      return ImmutableConstitution.NameForms.builder()
          .simple(NA_ERROR)
          .relative(type)
          .packageOf(NA_ERROR)
          .relativeAlreadyQualified(true)
          .visibility(protoclass().visibility())
          .build();
    }
    return typeEnclosing();
  }

  @Value.Derived
  public boolean hasImmutableInBuilder() {
    return implementationVisibility().isPrivate() && isTopLevelValue();
  }

  public boolean hasTopLevelBuilder() {
    return isFactory() || (isTopLevelValue() && isOutsideBuilder());
  }

  private boolean isFactory() {
    return protoclass().kind().isFactory();
  }

  public boolean hasTopLevelImmutable() {
    return isTopLevelValue()
        && !hasImmutableInBuilder();
  }

  public boolean isOutsideBuilder() {
    return protoclass().features().builder()
        && isImplementationHidden();
  }

  private boolean isTopLevelValue() {
    return protoclass().kind().isValue()
        && !protoclass().kind().isNested();
  }

  public boolean hasEnclosingNonvalue() {
    return protoclass().kind().isEnclosing()
        && !protoclass().kind().isValue();
  }

  /**
   * Actual abstract value type that is definitive model for the value type.
   * @return abstract value type name forms
   */
  @Value.Lazy
  public NameForms typeAbstract() {
    List<String> classSegments = Lists.newArrayListWithExpectedSize(2);
    Element e = SourceNames.collectClassSegments(protoclass().sourceElement(), classSegments);
    verify(e instanceof PackageElement);

    String packageOf = ((PackageElement) e).getQualifiedName().toString();
    String relative = DOT_JOINER.join(classSegments);
    boolean relativeAlreadyQualified = false;

    if (!protoclass().packageOf().name().equals(packageOf)) {
      relative = DOT_JOINER.join(packageOf, relative);
      relativeAlreadyQualified = true;
    }

    return ImmutableConstitution.NameForms.builder()
        .simple(names().typeAbstract)
        .relative(relative)
        .packageOf(packageOf)
        .relativeAlreadyQualified(relativeAlreadyQualified)
        .visibility(protoclass().visibility())
        .build();
  }

  public StyleInfo style() {
    return protoclass().styles().style();
  }

  /**
   * Package relative path
   * @param topLevel
   * @param nested
   * @return
   */
  private String inPackage(String topLevel, String... nested) {
    return DOT_JOINER.join(null, topLevel, (Object[]) nested);
  }

  /**
   * Actual immutable value type generated implementation.
   * @return immutable implementation type name forms
   */
  @Value.Lazy
  public NameForms typeImmutable() {
    String simple, relative;

    if (protoclass().kind().isNested()) {
      String enclosingSimpleName = typeImmutableEnclosingSimpleName();
      simple = names().typeImmutableNested;
      relative = inPackage(enclosingSimpleName, simple);
    } else if (hasImmutableInBuilder()) {
      simple = names().typeImmutable;
      relative = inPackage(typeBuilderSimpleName(), simple);
    } else {
      simple = names().typeImmutable;
      relative = inPackage(simple);
    }

    return ImmutableConstitution.NameForms.builder()
        .simple(simple)
        .relative(relative)
        .packageOf(protoclass().packageOf().name())
        .visibility(implementationVisibility())
        .build();
  }

  /**
   * Walks to the enclosing type's simple names and applies naming convention.
   * This shortcut/fix shows deficiency of model (it's probably more complicated than needed).
   * @return enclosing immutable name
   */
  @Value.Lazy
  String typeImmutableEnclosingSimpleName() {
    DeclaringType declaringType = protoclass().enclosingOf().get();
    String enclosingSimpleName = declaringType.element().getSimpleName().toString();
    String enclosingRawName = names().rawFromAbstract(enclosingSimpleName);
    // Here we checking for having both enclosing and value
    // if we had protoclass it would be kind().isEnclosing() && kind().isValue()
    Naming naming = declaringType.isImmutable()
        ? names().namings.typeImmutable
        : names().namings.typeImmutableEnclosing;

    return naming.apply(enclosingRawName);
  }

  private String typeBuilderSimpleName() {
    boolean isOutside = isImplementationHidden() || isFactory();
    Naming typeBuilderNaming = names().namings.typeBuilder;
    if (isOutside) {
      // For outer builder we can override with constant builder naming, but not the default.
      boolean isPlainDefault =
          isConstantNamingEquals(typeBuilderNaming, protoclass().environment().defaultStyles().typeBuilder());

      if (isPlainDefault) {
        typeBuilderNaming = typeBuilderNaming.requireNonConstant(Preference.SUFFIX);
      }
    }

    return Naming.Usage.CAPITALIZED.apply(typeBuilderNaming.apply(names().raw));
  }

  @Value.Lazy
  public AppliedNameForms factoryBuilder() {
    InnerBuilderDefinition innerBuilder = innerBuilder();
    if (innerBuilder.isExtending) {
      return typeBuilder().applied(NEW_KEYWORD);
    }
    return factoryImplementationBuilder();
  }

  private AppliedNameForms factoryImplementationBuilder() {
    boolean isOutside = isImplementationHidden() || isFactory();
    Naming methodBuilderNaming = isOutside
        ? names().namings.newBuilder
        : names().namings.builder;

    boolean haveConstructorOnBuilder = isOutside
        || isConstantNamingEquals(methodBuilderNaming, NEW_KEYWORD);

    NameForms typeNameForms = haveConstructorOnBuilder
        ? typeBuilder()
        : typeImmutable();

    return typeNameForms.applied(methodBuilderNaming.apply(names().raw));
  }

  private boolean isConstantNamingEquals(Naming naming, String name) {
    return naming.isConstant()
        && naming.apply("").equals(name);
  }

  @Value.Lazy
  public AppliedNameForms factoryOf() {
    if (isFactory()) {
      return ImmutableConstitution.NameForms.builder()
          .simple(protoclass().declaringType().get().element().getSimpleName().toString())
          .relative(protoclass().declaringType().get().name())
          .relativeAlreadyQualified(true)
          .packageOf(protoclass().packageOf().name())
          .visibility(protoclass().visibility())
          .build()
          .applied(protoclass().sourceElement().getSimpleName().toString());
    }
    return applyFactoryNaming(names().namings.of);
  }

  @Value.Lazy
  public AppliedNameForms factoryInstance() {
    return applyFactoryNaming(names().namings.instance);
  }

  @Value.Lazy
  public AppliedNameForms factoryCopyOf() {
    return applyFactoryNaming(names().namings.copyOf);
  }

  private AppliedNameForms applyFactoryNaming(Naming naming) {
    String raw = names().raw;

    boolean hasForwardingFactoryMethods = isImplementationHidden()
        && protoclass().kind().isNested();

    NameForms nameForms = hasForwardingFactoryMethods
        ? typeEnclosingFactory()
        : typeImmutable();

    if (hasForwardingFactoryMethods) {
      naming = naming.requireNonConstant(Preference.PREFIX);
    }

    String applyName = Naming.Usage.LOWERIZED.apply(naming.apply(raw));

    return nameForms.applied(applyName);
  }

  @Value.Lazy
  public NameForms typeEnclosingFactory() {
    String enclosingSimpleName = typeImmutableEnclosingSimpleName();
    return ImmutableConstitution.NameForms.builder()
        .simple(enclosingSimpleName)
        .relative(enclosingSimpleName)
        .packageOf(protoclass().packageOf().name())
        .visibility(protoclass().declaringVisibility())
        .build();
  }

  @Value.Lazy
  public NameForms typeEnclosing() {
    String name = protoclass().kind().isDefinedValue()
        ? names().typeImmutable
        : names().typeImmutableEnclosing;

    return ImmutableConstitution.NameForms.builder()
        .simple(name)
        .relative(name)
        .packageOf(protoclass().packageOf().name())
        .visibility(implementationEnclosingVisibility())
        .build();
  }

  private Visibility implementationEnclosingVisibility() {
    return implementationVisibility().max(Visibility.PACKAGE);
  }

  @Value.Lazy
  public NameForms typeBuilder() {
    InnerBuilderDefinition innerBuilder = innerBuilder();
    if (innerBuilder.isExtending) {
      NameForms typeAbstract = typeAbstract();
      return ImmutableConstitution.NameForms.copyOf(typeAbstract)
          .withRelative(DOT_JOINER.join(typeAbstract.relative(), innerBuilder.simpleName))
          .withSimple(innerBuilder.simpleName);
    }
    return typeImplementationBuilder();
  }

  @Value.Lazy
  public NameForms typeImplementationBuilder() {
    TypeNames names = names();

    boolean outside = isOutsideBuilder() || isFactory();
    boolean nested = protoclass().kind().isNested();

    String simple = typeBuilderSimpleName();
    String relative;

    if (outside && nested) {
      relative = inPackage(typeImmutableEnclosingSimpleName(), simple);
    } else if (outside) {
      relative = inPackage(simple);
    } else if (nested) {
      relative = inPackage(inPackage(typeImmutableEnclosingSimpleName(), names.typeImmutableNested, simple));
    } else {
      relative = inPackage(inPackage(names.typeImmutable, simple));
    }

    return ImmutableConstitution.NameForms.builder()
        .simple(simple)
        .relative(relative)
        .packageOf(protoclass().packageOf().name())
        .visibility(protoclass().visibility().max(implementationVisibility()))
        .build();
  }

  @Value.Immutable
  public static abstract class AppliedNameForms extends NameForms {
    public abstract NameForms forms();

    public abstract String applied();

    @Override
    @Value.Derived
    public String simple() {
      return isNew()
          ? (NEW_KEYWORD + ' ' + forms().simple())
          : applied();
    }

    @Override
    public String relative() {
      return isNew()
          ? (NEW_KEYWORD + ' ' + forms().relative())
          : (forms().relative() + '.' + applied());
    }

    @Value.Derived
    public boolean isNew() {
      return NEW_KEYWORD.equals(applied());
    }

    @Override
    public String toString() {
      if (relativeAlreadyQualified()) {
        return relative();
      }
      return isNew()
          ? (NEW_KEYWORD + ' ' + qualifyWithPackage(forms().relative()))
          : qualifyWithPackage(relative());
    }

    @Override
    public String packageOf() {
      return forms().packageOf();
    }

    @Override
    public Visibility visibility() {
      return forms().visibility();
    }

    @Override
    public boolean relativeAlreadyQualified() {
      return forms().relativeAlreadyQualified();
    }
  }

  @Value.Immutable
  public static abstract class NameForms {
    private static final String PUBLIC_MODIFIER_PREFIX = "public ";
    private static final String PRIVATE_MODIFIER_PREFIX = "private ";

    public abstract String simple();

    public abstract String relative();

    public abstract String packageOf();

    public abstract Visibility visibility();

    @Value.Default
    public boolean relativeAlreadyQualified() {
      return false;
    }

    /**
     * Access prefix. Includes trailing space separator if not empty (package private).
     * @return access keyword text
     */
    public String access() {
      switch (visibility()) {
      case PRIVATE:
        return PRIVATE_MODIFIER_PREFIX;
      case PUBLIC:
        return PUBLIC_MODIFIER_PREFIX;
      default:
        return "";
      }
    }

    public AppliedNameForms applied(String input) {
      return ImmutableConstitution.AppliedNameForms.builder()
          .forms(this)
          .applied(input)
          .build();
    }

    /**
     * Fully qualified type name
     */
    @Override
    public String toString() {
      return relativeAlreadyQualified()
          ? relative()
          : qualifyWithPackage(relative());
    }

    protected String qualifyWithPackage(String reference) {
      return DOT_JOINER.join(Strings.emptyToNull(packageOf()), reference);
    }
  }

  @Value.Lazy
  public InnerBuilderDefinition innerBuilder() {
    return new InnerBuilderDefinition();
  }

  public final class InnerBuilderDefinition {
    public final boolean isPresent;
    public final boolean isExtending;
    public final boolean isSuper;
    public final boolean isInterface;
    public final Visibility visibility;
    public final @Nullable String simpleName;

    InnerBuilderDefinition() {
      @Nullable
      TypeElement builderElement = findBuilderElement();
      if (builderElement != null) {
        this.isPresent = true;
        this.isInterface = builderElement.getKind() == ElementKind.INTERFACE;
        this.isExtending = isExtending(builderElement);
        this.isSuper = !isExtending;
        this.simpleName = builderElement.getSimpleName().toString();
        this.visibility = Visibility.of(builderElement);
        if (isExtending) {
          lateValidateExtending(builderElement);
        }
      } else {
        this.isPresent = false;
        this.isInterface = false;
        this.isExtending = false;
        this.isSuper = false;
        this.simpleName = null;
        this.visibility = Visibility.PRIVATE;
      }
    }

    private void lateValidateExtending(TypeElement t) {
      if (t.getModifiers().contains(Modifier.ABSTRACT)) {
        protoclass()
            .report()
            .withElement(t)
            .error("Extending %s shouldn't be abstract, it need to be instantiable",
                t.getSimpleName());
      }
    }

    private boolean isExtending(TypeElement element) {
      if (element.getKind() == ElementKind.CLASS) {
        String superclassString = SourceExtraction.getSuperclassString(element);
        // If we are extending yet to be generated builder, we detect it by having the same name
        // as relative name of builder type
        if (superclassString.endsWith(typeImplementationBuilder().relative())) {

          return true;
        }
      }
      return false;
    }

    @Nullable
    private TypeElement findBuilderElement() {
      Protoclass protoclass = protoclass();
      if (!protoclass.kind().isValue()) {
        return null;
      }
      for (Element t : protoclass.sourceElement().getEnclosedElements()) {
        ElementKind kind = t.getKind();
        if (kind.isClass() || kind.isInterface()) {
          String simpleName = t.getSimpleName().toString();
          Naming typeInnerBuilderNaming = names().namings.typeInnerBuilder;

          if (!typeInnerBuilderNaming.detect(simpleName).isEmpty()) {

            if (kind != ElementKind.CLASS
                && kind != ElementKind.INTERFACE) {
              protoclass
                  .report()
                  .withElement(t)
                  .warning("Inner type %s is %s - not supported as Builder extend/super type",
                      t.getSimpleName(),
                      kind.name().toLowerCase());

              return null;
            }

            Set<Modifier> modifiers = t.getModifiers();

            if (!modifiers.contains(Modifier.STATIC)
                || modifiers.contains(Modifier.PRIVATE)) {
              protoclass
                  .report()
                  .withElement(t)
                  .warning("Inner type %s should be static non-private to be supported as Builder extend/super type",
                      t.getSimpleName());

              return null;
            }

            if (kind == ElementKind.CLASS
                && !hasAccessibleConstructor(t)) {
              protoclass()
                  .report()
                  .withElement(t)
                  .warning("%s should have non-private no-argument constructor to be supported as Builder extend/super type",
                      t.getSimpleName());

              return null;
            }

            return (TypeElement) t;
          }
        }
      }
      return null;
    }

    private boolean hasAccessibleConstructor(Element type) {
      List<ExecutableElement> constructors = ElementFilter.constructorsIn(type.getEnclosedElements());

      if (constructors.isEmpty()) {
        // It is unclear (not checked) if we will have syntethic no-arg constructor
        // included, so we will assume no constructor to equate having a single constructors.
        return true;
      }

      for (ExecutableElement c : constructors) {
        if (c.getParameters().isEmpty()) {
          return !Visibility.of(c).isPrivate();
        }
      }

      return false;
    }
  }
}
