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

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import org.immutables.generator.Naming;
import org.immutables.generator.Naming.Preference;
import org.immutables.value.Value;
import org.immutables.value.processor.meta.Proto.Protoclass;
import org.immutables.value.processor.meta.Styles.UsingName.TypeNames;
import static com.google.common.base.Verify.*;

@Value.Nested
@Value.Immutable
public abstract class Constitution {
  private static final String NA_ERROR = "!should_not_be_used_in_generated_code!";
  private static final String NEW_KEYWORD = "new";
  private static final String BUILDER_CLASS_NAME = "Builder";
  private static final Joiner JOINER = Joiner.on('.').skipNulls();

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
    String relative = JOINER.join(classSegments);
    boolean relativeAlreadyQualified = false;

    if (!protoclass().packageOf().name().equals(packageOf)) {
      relative = JOINER.join(packageOf, relative);
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

  public StyleMirror style() {
    return protoclass().styles().style();
  }

  /**
   * Package relative path
   * @param topLevel
   * @param nested
   * @return
   */
  private String inPackage(String topLevel, String... nested) {
    return JOINER.join(null, topLevel, (Object[]) nested);
  }

  /**
   * Actual immutable value type generated implementation.
   * @return immutable implementation type name forms
   */
  @Value.Lazy
  public NameForms typeImmutable() {
    boolean nested = protoclass().kind().isNested();
    boolean inside = hasImmutableInBuilder();

    String simple = names().typeImmutable;

    String relative;

    if (nested) {
      String enclosingSimpleName = typeImmutableEnclosingSimpleName();
      simple = names().typeImmutableNested;
      relative = inPackage(enclosingSimpleName, simple);
    } else if (inside) {
      relative = inPackage(typeBuilderSimpleName(), simple);
    } else {
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
    String enclosingSimpleName = protoclass().enclosingOf().get().element().getSimpleName().toString();
    String enclosingRawName = names().rawFromAbstract(enclosingSimpleName);
    return names().namings.typeImmutableEnclosing.apply(enclosingRawName);
  }

  private String typeBuilderSimpleName() {
    boolean isOutside = isImplementationHidden() || isFactory();
    Naming typeBuilderNaming = names().namings.typeBuilder;
    if (isOutside) {
      // For outer builder we can override with constant builder naming, but not the default.
      boolean isPlainDefault = isConstantNamingEquals(typeBuilderNaming, BUILDER_CLASS_NAME);

      if (isPlainDefault) {
        typeBuilderNaming = typeBuilderNaming.requireNonConstant(Preference.SUFFIX);
      }
    }

    return Naming.Usage.CAPITALIZED.apply(typeBuilderNaming.apply(names().raw));
  }

  @Value.Lazy
  public NameForms factoryBuilder() {
    boolean isOutside = isImplementationHidden() || isFactory();
    Naming methodBuilderNaming = isOutside
        ? names().namings.newBuilder()
        : names().namings.builder();

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
  public NameForms factoryOf() {
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
  public NameForms factoryInstance() {
    return applyFactoryNaming(names().namings.instance);
  }

  @Value.Lazy
  public NameForms factoryCopyOf() {
    return applyFactoryNaming(names().namings.copyOf);
  }

  private NameForms applyFactoryNaming(Naming naming) {
    if (isImplementationHidden()) {
      naming = naming.requireNonConstant(Preference.PREFIX);
    }

    NameForms nameForms = isImplementationHidden() && protoclass().kind().isNested()
        ? typeEnclosing()
        : typeImmutable();

    String applyName = Naming.Usage.LOWERIZED.apply(naming.apply(names().raw));

    return nameForms.applied(applyName);
  }

  @Value.Lazy
  public NameForms typeEnclosing() {
    return ImmutableConstitution.NameForms.builder()
        .simple(names().typeImmutableEnclosing)
        .relative(names().typeImmutableEnclosing)
        .packageOf(protoclass().packageOf().name())
        .visibility(protoclass().declaringVisibility())
        .build();
  }

  @Value.Lazy
  public NameForms typeBuilder() {
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

    @Value.Default
    public String applied() {
      return "";
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

    public NameForms applied(String input) {
      return ImmutableConstitution.NameForms.builder()
          .packageOf(packageOf())
          .applied(input)
          .simple(applyTo(simple(), input, false))
          .relative(applyTo(relative(), input, true))
          .visibility(visibility())
          .relativeAlreadyQualified(relativeAlreadyQualified())
          .build();
    }

    private String applyTo(String targetType, String input, boolean relative) {
      return NEW_KEYWORD.equals(input)
          ? (NEW_KEYWORD + ' ' + targetType)
          : (relative ? targetType + '.' + input : input);
    }

    /**
     * Fully qualified type name
     */
    @Override
    public String toString() {
      if (relativeAlreadyQualified()) {
        return relative();
      }
      return qualifyWithPackage(relative());
    }

    private String qualifyWithPackage(String reference) {
      return JOINER.join(Strings.emptyToNull(packageOf()), reference);
    }
  }
}
