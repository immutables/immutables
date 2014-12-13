package org.immutables.value.processor.meta;

import org.immutables.generator.Naming.Usage;
import java.util.Collections;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
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

    String relative = JOINER.join(classSegments);
    String packageOf = ((PackageElement) e).getQualifiedName().toString();

    return ImmutableConstitution.NameForms.builder()
        .simple(allNames().typeAbstract)
        .relative(relative)
        .packageOf(packageOf)
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
    boolean hidden = isImplementationHidden();

    String simple = allNames().typeImmutable;

    String transferredApplyType = NameForms.NOT_TRANSFERRED;
    String relative;

    if (nested) {
      String enclosingSimpleName = typeImmutableEnclosingSimpleName();
      simple = allNames().typeImmutableNested;
      relative = inPackage(enclosingSimpleName, simple);
      if (hidden) {
        transferredApplyType = inPackage(enclosingSimpleName);
      }
    } else if (inside) {
      relative = inPackage(typeBuilderSimpleName(), simple);
    } else {
      relative = inPackage(simple);
    }

    return ImmutableConstitution.NameForms.builder()
        .simple(simple)
        .relative(relative)
        .transferredApply(transferredApplyType)
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
  public String factoryOf() {
    Naming ofNaming = allNames().namings.of;
    if (isImplementationHidden()) {
      ofNaming = ofNaming.requireNonConstant(Preference.PREFIX);
    }
    return Naming.Usage.LOWERIZED.apply(ofNaming.apply(allNames().raw));
  }

  @Value.Lazy
  public String factoryInstance() {
    Naming instanceNaming = allNames().namings.instance;
    if (isImplementationHidden()) {
      instanceNaming = instanceNaming.requireNonConstant(Preference.PREFIX);
    }
    return Naming.Usage.LOWERIZED.apply(instanceNaming.apply(allNames().raw));
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
  public static abstract class NameForms implements Function<String, String> {
    static final String NOT_TRANSFERRED = "";

    public abstract String simple();

    public abstract String relative();

    public abstract String packageOf();

    public abstract TypeVisibility visibility();

    @Value.Default
    public String transferredApply() {
      return NOT_TRANSFERRED;
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

    /**
     * Fully qualified type name
     */
    @Override
    public String toString() {
      return qualifyWithPackage(relative());
    }

    private String qualifyWithPackage(String reference) {
      return JOINER.join(Strings.emptyToNull(packageOf()), reference);
    }

    @Override
    public String apply(String input) {
      String targetType = qualifyWithPackage(
          !transferredApply().isEmpty()
              ? transferredApply()
              : relative());

      return NEW_KEYWORD.equals(input)
          ? (NEW_KEYWORD + ' ' + targetType)
          : (targetType + '.' + input);
    }
  }
}
