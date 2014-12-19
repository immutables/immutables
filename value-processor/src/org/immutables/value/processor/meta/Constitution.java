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
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
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
  private static final Joiner JOINER = Joiner.on('.').skipNulls();
  private static final String NEW_KEYWORD = "new";

  public abstract Protoclass protoclass();

  @Value.Derived
  public TypeVisibility implementationVisibility() {
    return protoclass().visibility().forImplementation(
        protoclass().features().visibility());
  }

  public boolean isImplementationHidden() {
    return implementationVisibility().isMoreRestrictiveThan(protoclass().visibility());
  }

  @Value.Derived
  public TypeNames allNames() {
    return protoclass().styles().forType(
        protoclass().sourceElement().getSimpleName().toString());
  }

  /**
   * Value is the canonical outside look of the value type. It should be either
   * {@link #typeAbstract()} or {@link #typeImmutable()}.
   * @return canonical value type name forms
   */
  @Value.Lazy
  public NameForms typeValue() {
    if (protoclass().kind().isValue()) {
      return isImplementationHidden()
          ? typeAbstract()
          : typeImmutable();
    }
    return typeEnclosing();
  }

  @Value.Derived
  public boolean hasImmutableInBuilder() {
    return implementationVisibility().isPrivate() && isTopLevelValue();
  }

  public boolean hasTopLevelBuilder() {
    return isTopLevelValue() && isOutsideBuilder();
  }

  public boolean hasTopLevelImmutable() {
    return isTopLevelValue()
        && !hasImmutableInBuilder();
  }

  public boolean isOutsideBuilder() {
    return protoclass().features().builder()
        && implementationVisibility().isPrivate();
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
    Element e = collectClassSegments(classSegments);
    verify(e instanceof PackageElement);

    String packageOf = ((PackageElement) e).getQualifiedName().toString();
    String relative = JOINER.join(classSegments);
    boolean relativeAlreadyQualified = false;

    if (!protoclass().packageOf().name().equals(packageOf)) {
      relative = JOINER.join(packageOf, relative);
      relativeAlreadyQualified = true;
    }

    return ImmutableConstitution.NameForms.builder()
        .simple(allNames().typeAbstract)
        .relative(relative)
        .packageOf(packageOf)
        .relativeAlreadyQualified(relativeAlreadyQualified)
        .visibility(protoclass().visibility())
        .build();
  }

  private Element collectClassSegments(List<String> classSegments) {
    Element e = protoclass().sourceElement();
    for (; e.getKind() != ElementKind.PACKAGE; e = e.getEnclosingElement()) {
      classSegments.add(e.getSimpleName().toString());
    }
    Collections.reverse(classSegments);
    return e;
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

    String simple = allNames().typeImmutable;

    String relative;

    if (nested) {
      String enclosingSimpleName = typeImmutableEnclosingSimpleName();
      simple = allNames().typeImmutableNested;
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
    String enclosingRawName = allNames().rawFromAbstract(enclosingSimpleName);
    return allNames().namings.typeImmutableEnclosing.apply(enclosingRawName);
  }

  private String typeBuilderSimpleName() {
    Naming builderNaming = allNames().namings.typeBuilder;
    if (isImplementationHidden()) {
      builderNaming = builderNaming.requireNonConstant(Preference.SUFFIX);
    }
    return Naming.Usage.CAPITALIZED.apply(builderNaming.apply(allNames().raw));
  }

  @Value.Lazy
  public NameForms factoryBuilder() {
    NameForms nameForms = isImplementationHidden()
        ? typeBuilder()
        : typeImmutable();

    return nameForms.applied(allNames().builder);
  }

  @Value.Lazy
  public NameForms factoryOf() {
    return applyFactoryNaming(allNames().namings.of);
  }

  @Value.Lazy
  public NameForms factoryInstance() {
    return applyFactoryNaming(allNames().namings.instance);
  }

  @Value.Lazy
  public NameForms factoryCopyOf() {
    return applyFactoryNaming(allNames().namings.copyOf);
  }

  private NameForms applyFactoryNaming(Naming naming) {
    if (isImplementationHidden()) {
      naming = naming.requireNonConstant(Preference.PREFIX);
    }

    NameForms nameForms = isImplementationHidden() && protoclass().kind().isNested()
        ? typeEnclosing()
        : typeImmutable();

    String applyName = Naming.Usage.LOWERIZED.apply(naming.apply(allNames().raw));

    return nameForms.applied(applyName);
  }

  @Value.Lazy
  public NameForms typeEnclosing() {
    return ImmutableConstitution.NameForms.builder()
        .simple(allNames().typeImmutableEnclosing)
        .relative(allNames().typeImmutableEnclosing)
        .packageOf(protoclass().packageOf().name())
        .visibility(protoclass().declaringVisibility())
        .build();
  }

  @Value.Lazy
  public NameForms typeBuilder() {
    TypeNames names = allNames();

    boolean outside = isOutsideBuilder();
    boolean nested = protoclass().kind().isNested();

    String simple = typeBuilderSimpleName();
    String relative;

    if (outside && nested) {
      relative = inPackage(inPackage(typeImmutableEnclosingSimpleName(), simple));
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
    public abstract String simple();

    public abstract String relative();

    public abstract String packageOf();

    public abstract TypeVisibility visibility();

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
        return "private ";
      case PUBLIC:
        return "public ";
      default:
        return "";
      }
    }

    public NameForms applied(String input) {
      return ImmutableConstitution.NameForms.builder()
          .packageOf(packageOf())
          .simple(applyTo(simple(), input, false))
          .relative(applyTo(relative(), input, true))
          .visibility(TypeVisibility.PUBLIC)
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
