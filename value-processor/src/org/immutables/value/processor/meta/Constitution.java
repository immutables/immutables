/*
   Copyright 2014-2025 Immutables Authors and Contributors

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

import com.google.common.base.CaseFormat;
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
import org.immutables.generator.SourceTypes;
import org.immutables.value.Value;
import org.immutables.value.processor.meta.Proto.DeclaringType;
import org.immutables.value.processor.meta.Proto.Protoclass;
import org.immutables.value.processor.meta.Reporter.About;
import org.immutables.value.processor.meta.Styles.PackageNaming;
import org.immutables.value.processor.meta.Styles.UsingName.TypeNames;
import org.immutables.value.processor.meta.ValueMirrors.Style.BuilderVisibility;
import org.immutables.value.processor.meta.ValueMirrors.Style.ImplementationVisibility;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;

@Value.Enclosing
@Value.Immutable
public abstract class Constitution {
  private static final String NA_ERROR = "!should_not_be_used_in_generated_code!";
  private static final String NEW_KEYWORD = "new";
  private static final Joiner DOT_JOINER = Joiner.on('.').skipNulls();

  public abstract Protoclass protoclass();

  @Value.Lazy
  public Generics generics() {
    return new Generics(protoclass(),
        protoclass().kind().isConstructor()
            ? protoclass().sourceElement().getEnclosingElement()
            : protoclass().sourceElement());
  }

  @Value.Derived
  public Visibility implementationVisibility() {
    ImplementationVisibility visibility;
    if (!style().visibilityString().isEmpty()) {
      visibility = ImplementationVisibility.valueOf(style().visibilityString());
    } else {
      visibility = style().visibility();
    }

    if (visibility == ImplementationVisibility.PRIVATE
        && !protoclass().features().builder()
        && !protoclass().kind().isNested()) {
      protoclass()
          .report()
          .warning(About.INCOMPAT,
              "effective Style.visibility cannot be PRIVATE when builder is disabled and is not nested,"
                  + " automatically switching visibility to PACKAGE because top level implementation class is " +
                  "required");
      return Visibility.PACKAGE;
    }
    return protoclass().visibility().forImplementation(visibility);
  }

  @Value.Derived
  public Visibility builderVisibility() {
    Visibility visibility;
    if (!style().builderVisibilityString().isEmpty()) {
      visibility = protoclass().visibility().forBuilder(BuilderVisibility.valueOf(style().builderVisibilityString()));
    } else {
      visibility = protoclass().visibility().forBuilder(style().builderVisibility());
    }
    if (visibility == Visibility.PUBLIC && protoclass().styles().style().stagedBuilder()
        && isNestedFactoryOrConstructor()) {
      return Visibility.PRIVATE;
    }
    return visibility;
  }

  public boolean isImplementationHidden() {
    return implementationVisibility().isPrivate();
  }

  public boolean returnsAbstractValueType() {
    return isImplementationHidden()
        || style().visibility() == ValueMirrors.Style.ImplementationVisibility.SAME_NON_RETURNED
        || style().overshadowImplementation()
        || (style().implementationNestedInBuilder()
        && implementationVisibility().isMoreRestrictiveThan(builderVisibility()));
  }

  public boolean isImplementationPrimary() {
    return protoclass().visibility().isMoreRestrictiveThan(implementationVisibility())
        && protoclass().kind().isValue();
  }

  @Value.Derived
  public String implementationPackage() {
    PackageNaming naming = protoclass().styles().packageGenerated();
    return naming.apply(definingPackage());
  }

  public String definingPackage() {
    return protoclass().packageOf().name();
  }

  @Value.Derived
  public TypeNames names() {
    return protoclass().createTypeNames();
  }

  @Value.Lazy
  public NameForms typePreferablyAbstract() {
    if (protoclass().kind().isValue()) {
      return isImplementationPrimary()
          ? typeImmutable()
          : typeAbstract();
    }
    return typeValue();
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

  @Value.Lazy
  public NameForms typeModifiable() {
    checkState(protoclass().kind().isModifiable());
    String simple = names().typeModifiable();
    return ImmutableConstitution.NameForms.builder()
        .simple(simple)
        .relativeRaw(inPackage(simple))
        .genericArgs(generics().args())
        .packageOf(implementationPackage())
        .visibility(implementationVisibility())
        .build();
  }

  @Value.Lazy
  public AppliedNameForms factoryCreate() {
    return typeModifiable().applied(names().create());
  }

  private boolean isAbstractPrimary() {
    return returnsAbstractValueType()
        || !isImplementationPrimary();
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
   * For a factory, it is a special surrogate.
   * @return canonical value type name forms
   */
  @Value.Lazy
  public NameForms typeValue() {
    if (protoclass().kind().isValue()) {
      return returnsAbstractValueType()
          ? typeAbstract()
          : typeImmutable();
    }

    if (protoclass().kind().isJavaBean()) {
      return typeAbstract();
    }

    if (isFactory()) {
      if (protoclass().kind().isRecord()) {
        TypeElement enclosingType = (TypeElement) protoclass().sourceElement();

        return ImmutableConstitution.NameForms.builder()
            .simple(enclosingType.getSimpleName().toString())
            .relativeRaw(enclosingType.getQualifiedName().toString())
            .genericArgs(generics().args())
            .relativeAlreadyQualified(true)
            .packageOf(NA_ERROR)
            .visibility(protoclass().visibility())
            .build();
      }

      if (protoclass().kind().isConstructor()) {
        TypeElement enclosingType = (TypeElement) protoclass().sourceElement().getEnclosingElement();

        return ImmutableConstitution.NameForms.builder()
            .simple(enclosingType.getSimpleName().toString())
            .relativeRaw(enclosingType.getQualifiedName().toString())
            .genericArgs(generics().args())
            .relativeAlreadyQualified(true)
            .packageOf(NA_ERROR)
            .visibility(protoclass().visibility())
            .build();
      }

      ExecutableElement method = (ExecutableElement) protoclass().sourceElement();
      String type = method.getReturnType().toString();

      return ImmutableConstitution.NameForms.builder()
          .simple(NA_ERROR)
          .relativeRaw(type)
          .packageOf(NA_ERROR)
          .relativeAlreadyQualified(true)
          .visibility(protoclass().visibility())
          .build();
    }
    return typeEnclosing();
  }

  @Value.Derived
  public boolean hasImmutableInBuilder() {
    return isOutsideBuilder() && isTopLevelValue();
  }

  public boolean hasTopLevelBuilder() {
    return (isFactory() && !isNestedFactoryOrConstructor())
        || (isTopLevelValue() && isOutsideBuilder());
  }

  public boolean isFactory() {
    return protoclass().kind().isFactory();
  }

  public boolean isNestedFactoryOrConstructor() {
    return protoclass().kind().isNestedFactoryOrConstructor();
  }

  public boolean isNested() {
    return protoclass().kind().isNested();
  }

  public boolean hasTopLevelImmutable() {
    return isTopLevelValue()
        && !hasImmutableInBuilder();
  }

  public boolean isTopLevelRecord() {
    return protoclass().kind() == Protoclass.Kind.DEFINED_RECORD;
  }

  public boolean isOutsideBuilder() {
    return isFactory()
        || (protoclass().features().builder()
        && (isImplementationHidden()
        || style().implementationNestedInBuilder()));
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
    if (protoclass().kind().isConstructor() || protoclass().kind().isRecord()) {
      return typeValue();
    }

    List<String> classSegments = Lists.newArrayListWithExpectedSize(2);
    Element e = SourceNames.collectClassSegments(protoclass().sourceElement(), classSegments);
    verify(e instanceof PackageElement);

    String packageOf = ((PackageElement) e).getQualifiedName().toString();
    String relative = DOT_JOINER.join(classSegments);
    boolean relativeAlreadyQualified = false;

    if (!implementationPackage().equals(packageOf)) {
      relative = DOT_JOINER.join(packageOf, relative);
      relativeAlreadyQualified = true;
    }

    return ImmutableConstitution.NameForms.builder()
        .simple(names().typeAbstract)
        .relativeRaw(relative)
        .packageOf(packageOf)
        .genericArgs(generics().args())
        .relativeAlreadyQualified(relativeAlreadyQualified)
        .visibility(protoclass().visibility())
        .build();
  }

  public StyleInfo style() {
    return protoclass().styles().style();
  }

  /**
   * Package relative path
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
    if (protoclass().kind().isConstructor() || protoclass().kind().isRecord()) {
      return typeValue();
    }
    String simple, relative;

    if (protoclass().kind().isNested()) {
      String enclosingSimpleName = typeImmutableEnclosingSimpleName();
      simple = names().typeImmutableNested();
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
        .relativeRaw(relative)
        .genericArgs(generics().args())
        .packageOf(implementationPackage())
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
    // Here we check for having both enclosing and value
    // if we had protoclass it would be kind().isEnclosing() && kind().isValue()
    Naming naming = declaringType.isImmutable()
        ? names().namings.typeImmutable
        : names().namings.typeImmutableEnclosing;

    return naming.apply(enclosingRawName);
  }

  private String typeBuilderSimpleName() {
    boolean isOutside = isOutsideBuilder();
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
    boolean isOutsideNotNested = isOutsideBuilderNotNested();
    Naming methodBuilderNaming = isOutsideNotNested
        ? names().namings.newBuilder
        : names().namings.builder;

    boolean isNewBuilder = isConstantNamingEquals(methodBuilderNaming, NEW_KEYWORD);
    boolean haveConstructorOnBuilder = isOutsideNotNested
        || isNewBuilder;

    NameForms typeNameForms = haveConstructorOnBuilder
        ? typeBuilder()
        : isNestedFactoryOrConstructor()
            && protoclass().enclosingOf().isPresent()
            ? typeEnclosingImplOf() : typeImmutable();

    String applied = methodBuilderNaming.apply(names().raw);
    if (isNestedFactoryOrConstructor() && !isNewBuilder) {
      applied = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, names().raw)
          + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, applied);
    }
    return typeNameForms.applied(applied);
  }

  private NameForms typeEnclosingImplOf() {
    String enclosingSimpleName = typeImmutableEnclosingSimpleName();
    return ImmutableConstitution.NameForms.builder()
        .simple(enclosingSimpleName)
        .relativeRaw(enclosingSimpleName)
        .genericArgs(generics().args())
        .packageOf(implementationPackage())
        .visibility(implementationVisibility())
        .build();
  }

  public boolean isOutsideBuilderNotNested() {
    return isOutsideBuilder() && !isNestedFactoryOrConstructor();
  }

  private boolean isConstantNamingEquals(Naming naming, String name) {
    return naming.isConstant()
        && naming.apply("").equals(name);
  }

  @Value.Lazy
  public AppliedNameForms factoryOf() {
    if (isFactory()) {
      if (protoclass().kind().isRecord()) {
        TypeElement recordType = (TypeElement) protoclass().sourceElement();

        return ImmutableConstitution.NameForms.builder()
            .simple(recordType.getSimpleName().toString())
            .relativeRaw(recordType.getQualifiedName().toString())
            .genericArgs(generics().args())
            .relativeAlreadyQualified(true)
            .packageOf(NA_ERROR)
            .visibility(protoclass().visibility())
            .build()
            .applied("new");
      }

      TypeElement enclosingType = (TypeElement) protoclass().sourceElement().getEnclosingElement();

      String invoke = protoclass().kind().isConstructor()
          ? "new"
          : protoclass().sourceElement().getSimpleName().toString();

      return ImmutableConstitution.NameForms.builder()
          .simple(enclosingType.getSimpleName().toString())
          .relativeRaw(enclosingType.getQualifiedName().toString())
          .genericArgs(generics().args())
          .relativeAlreadyQualified(true)
          .packageOf(NA_ERROR)
          .visibility(protoclass().visibility())
          .build()
          .applied(invoke);
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
        .relativeRaw(enclosingSimpleName)
        .packageOf(implementationPackage())
        .visibility(protoclass().declaringVisibility())
        .build();
  }

  @Value.Lazy
  public NameForms typeEnclosing() {
    String name = protoclass().kind().isDefinedValue()
        ? names().typeImmutable
        : names().typeImmutableEnclosing();

    return ImmutableConstitution.NameForms.builder()
        .simple(name)
        .relativeRaw(name)
        .packageOf(implementationPackage())
        .visibility(implementationEnclosingVisibility())
        .build();
  }

  private Visibility implementationEnclosingVisibility() {
    return implementationVisibility().max(Visibility.PACKAGE);
  }

  @Value.Lazy
  public NameForms typeWith() {
    String simple, relative;

    if (protoclass().kind().isNested()
        || protoclass().kind() == Protoclass.Kind.DEFINED_NESTED_RECORD) {
      String enclosingSimpleName = typeImmutableEnclosingSimpleName();
      simple = names().typeWith();
      relative = inPackage(enclosingSimpleName, simple);
    } else if (hasImmutableInBuilder()) {
      simple = names().typeWith();
      relative = inPackage(typeBuilderSimpleName(), simple);
    } else {
      simple = names().typeWith();
      relative = inPackage(simple);
    }

    return ImmutableConstitution.NameForms.builder()
        .simple(simple)
        .relativeRaw(relative)
        .genericArgs(generics().args())
        .packageOf(implementationPackage())
        .visibility(implementationVisibility())
        .build();
  }

  @Value.Lazy
  public NameForms typeBuilder() {
    InnerBuilderDefinition innerBuilder = innerBuilder();
    if (innerBuilder.isExtending) {
      NameForms typeAbstract = typeAbstract();
      return ImmutableConstitution.NameForms.copyOf(typeAbstract)
          .withRelativeRaw(DOT_JOINER.join(typeAbstract.relativeRaw(), innerBuilder.simpleName))
          .withSimple(innerBuilder.simpleName);
    }
    return typeImplementationBuilder();
  }

  @Value.Lazy
  public NameForms typeImplementationBuilder() {
    TypeNames names = names();

    boolean outside = isOutsideBuilder();
    boolean nested = protoclass().kind().isNested()
        || protoclass().kind() == Protoclass.Kind.DEFINED_NESTED_RECORD;

    String simple = typeBuilderSimpleName();
    String relative;

    if (outside && nested) {
      relative = inPackage(typeImmutableEnclosingSimpleName(), simple);
    } else if (outside) {
      relative = inPackage(simple);
    } else if (nested) {
      relative = inPackage(inPackage(typeImmutableEnclosingSimpleName(), names.typeImmutableNested(), simple));
    } else {
      relative = inPackage(inPackage(names.typeImmutable, simple));
    }

    Visibility visibility = builderVisibility();

    return ImmutableConstitution.NameForms.builder()
        .simple(simple)
        .relativeRaw(relative)
        .genericArgs(generics().args())
        .packageOf(implementationPackage())
        .visibility(visibility)
        .build();
  }

  @Value.Immutable
  public static abstract class AppliedNameForms extends AbstractNameForms {
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
    public String relativeRaw() {
      return isNew()
          ? (NEW_KEYWORD + ' ' + forms().relativeRaw())
          : (forms().relativeRaw() + '.' + applied());
    }

    @Override
    public String relative() {
      return combineApplied(false);
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
      return combineApplied(true);
    }

    @Override
    public String genericArgs() {
      return forms().genericArgs();
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

    private String combineApplied(boolean qualifyWithPackage) {
      String base = forms().relativeRaw();
      if (qualifyWithPackage) {
        base = qualifyWithPackage(base);
      }
      return isNew()
          ? (NEW_KEYWORD + ' ' + base + genericArgs())
          : (base + '.' + genericArgs() + applied());
    }
  }

  public static abstract class AbstractNameForms {
    private static final String PUBLIC_MODIFIER_PREFIX = "public ";
    private static final String PRIVATE_MODIFIER_PREFIX = "private ";

    public abstract String simple();

    public abstract String relativeRaw();

    public abstract String packageOf();

    public abstract Visibility visibility();

    @Value.Default
    public String absolute() {
      return DOT_JOINER.join(packageOf(), relative());
    }

    @Value.Default
    public String absoluteRaw() {
      return DOT_JOINER.join(packageOf(), relativeRaw());
    }

    @Value.Default
    public String genericArgs() {
      return "";
    }

    @Value.Default
    public boolean relativeAlreadyQualified() {
      return false;
    }

    public String relative() {
      return relativeRaw() + genericArgs();
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

    protected String qualifyWithPackage(String reference) {
      return DOT_JOINER.join(Strings.emptyToNull(packageOf()), reference);
    }
  }

  @Value.Immutable
  public static abstract class NameForms extends AbstractNameForms {

    /**
     * Fully qualified type name
     */
    @Override
    public String toString() {
      return relativeAlreadyQualified()
          ? relative()
          : qualifyWithPackage(relative());
    }

    public AppliedNameForms applied(String input) {
      return ImmutableConstitution.AppliedNameForms.builder()
          .forms(this)
          .applied(input)
          .build();
    }
  }

  @Value.Lazy
  public InnerBuilderDefinition innerBuilder() {
    return new InnerBuilderDefinition();
  }

  @Value.Lazy
  public InnerModifiableDefinition innerModifiable() {
    return new InnerModifiableDefinition();
  }

  public final class InnerBuilderDefinition extends InnerBaseClassDefinition {
    public InnerBuilderDefinition() {
      super(names().namings.typeInnerBuilder);
    }

    @Override
    protected boolean isApplicableTo(Protoclass p) {
      return p.kind().isValue() || p.kind().isRecord();
    }

    @Override
    protected boolean isExtending(TypeElement element) {
      if (element.getKind() == ElementKind.CLASS) {
        String superclassString = SourceExtraction.getSuperclassString(element);
        String rawSuperclass = SourceTypes.extract(superclassString).getKey();
        // If we are extending yet to be generated builder, we detect it by having the same name
        // as relative name of builder type
        return rawSuperclass.endsWith(typeImplementationBuilder().relativeRaw());
      }
      return false;
    }

    @Override
    protected void lateValidateExtending(TypeElement t) {
      super.lateValidateExtending(t);

      if (protoclass().styles().style().stagedBuilder()) {
        protoclass()
            .report()
            .withElement(t)
            .warning(About.INCOMPAT,
                "Extending %s shouldn't be used with stagedBuilder style attribute, they are incompartible:"
                    + " Staged builder generate series of staged interfaces, but extending builder actually"
                    + " extends implementation and do not provide type safety for setting first attribute,"
                    + " as well as stagedBuilder forces generated builder interfaces to leak in code using the builder"
                    + " and hence defeating the purpose of using extending builder.",
                t.getSimpleName());
      }
    }
  }

  public final class InnerModifiableDefinition extends InnerBaseClassDefinition {
    public InnerModifiableDefinition() {
      super(names().namings.typeInnerModifiable);
    }

    @Override
    protected boolean isApplicableTo(Protoclass p) {
      return p.kind().isModifiable();
    }

    @Override
    protected boolean isExtending(TypeElement t) {
      return false;
    }

    @Override
    protected void lateValidateSuper(TypeElement t) {
      super.lateValidateSuper(t);

      if (t.getKind() == ElementKind.CLASS) {
        String superclassString = SourceExtraction.getSuperclassString(t);
        String rawSuperclass = SourceTypes.extract(superclassString).getKey();
        // We need to extend the base class
        if (!typeAbstract().toString().endsWith(rawSuperclass)) {
          protoclass()
              .report()
              .withElement(t)
              .error("%s needs to extend the base class",
                  t.getSimpleName());
        }
      }
    }
  }

  public abstract class InnerBaseClassDefinition {
    public final boolean isAccessibleFields;
    public final boolean isPresent;
    public final boolean isExtending;
    public final boolean isSuper;
    public final boolean isInterface;
    public final Visibility visibility;
    public final @Nullable String simpleName;
    public final @Nullable Generics generics;
    public final Naming naming;

    InnerBaseClassDefinition(Naming naming) {
      this.naming = naming;

      @Nullable TypeElement baseElement = findBaseClassElement();
      // The following series of checks designed
      // to not validate inner builder if it's disabled,
      // but at the same time we need such validation
      // if we are using "extending" builder which is still allowed
      // on demand even if builder feature is disabled
      boolean extending = false;

      if (baseElement != null) {
        extending = isExtending(baseElement);
      }

      if (baseElement != null && !protoclass().features().builder() && !extending) {
        baseElement = null;
      }

      if (baseElement != null && !isValidInnerBaseClass(baseElement)) {
        baseElement = null;
      }

      if (baseElement != null) {
        this.isAccessibleFields = AccessibleFieldsMirror.find(baseElement).isPresent();
        this.isPresent = true;
        this.isInterface = baseElement.getKind() == ElementKind.INTERFACE;
        this.isExtending = extending;
        this.isSuper = !extending;
        this.simpleName = baseElement.getSimpleName().toString();
        this.visibility = Visibility.of(baseElement);
        this.generics = new Generics(protoclass(), baseElement);
        if (isExtending) {
          lateValidateExtending(baseElement);
        }
        if (isSuper) {
          lateValidateSuper(baseElement);
        }
      } else {
        this.isAccessibleFields = false;
        this.isPresent = false;
        this.isInterface = false;
        this.isExtending = false;
        this.isSuper = false;
        this.visibility = Visibility.PRIVATE;
        this.simpleName = null;
        this.generics = Generics.empty();
      }
    }

    protected void lateValidateSuper(TypeElement t) {
      List<String> undeclaredParams = Lists.newArrayList();
      for (String v : this.generics.vars()) {
        if (!generics().hasParameter(v)) {
          undeclaredParams.add(v);
        }
      }

      if (!undeclaredParams.isEmpty()) {
        protoclass()
            .report()
            .withElement(t)
            .error("Inner type %s%s uses generic parameter %s which are not present in value's declaration: %s",
                t.getSimpleName(),
                this.generics.args(),
                Joiner.on(", ").join(undeclaredParams),
                generics());
      }
    }

    protected void lateValidateExtending(TypeElement t) {
      if (t.getModifiers().contains(Modifier.ABSTRACT)) {
        protoclass()
            .report()
            .withElement(t)
            .error("Extending %s shouldn't be abstract, it has to be instantiable",
                t.getSimpleName());
      }

      if (!this.generics.def().equals(generics().def())) {
        protoclass()
            .report()
            .withElement(t)
            .error("Inner type %s should have the same type parameters as abstract value type: %s",
                t.getSimpleName(),
                generics().def());
      }
    }

    /**
     * Used to determine if the inner class we're looking for is revelant
     * given the annotations on the prototype class.  For example, there's
     * no point in doing anything with an Modifiable inner class if it's
     * not setup with the Value.Modifiable annotation.
     */
    protected abstract boolean isApplicableTo(Protoclass p);

    protected abstract boolean isExtending(TypeElement t);

    @Nullable
    private TypeElement findBaseClassElement() {
      Protoclass protoclass = protoclass();
      if (!isApplicableTo(protoclass)) {
        return null;
      }
      for (Element t : protoclass.sourceElement().getEnclosedElements()) {
        ElementKind kind = t.getKind();
        if (kind.isClass() || kind.isInterface()) {
          String simpleName = t.getSimpleName().toString();
          Naming typeInnerClassNaming = naming;

          if (!typeInnerClassNaming.detect(simpleName).isEmpty()) {
            return (TypeElement) t;
          }
        }
      }
      return null;
    }

    private boolean isValidInnerBaseClass(Element t) {
      ElementKind kind = t.getKind();
      if (kind != ElementKind.CLASS
          && kind != ElementKind.INTERFACE) {
        protoclass()
            .report()
            .withElement(t)
            .warning(About.INCOMPAT,
                "Inner type %s is %s - not supported as Builder extend/super type",
                t.getSimpleName(),
                kind.name().toLowerCase());

        return false;
      }

      Set<Modifier> modifiers = t.getModifiers();

      if (!modifiers.contains(Modifier.STATIC)
          || modifiers.contains(Modifier.PRIVATE)) {
        protoclass()
            .report()
            .withElement(t)
            .warning(About.INCOMPAT,
                "Inner type %s should be static non-private to be supported as Builder extend/super type",
                t.getSimpleName());

        return false;
      }

      if (kind == ElementKind.CLASS
          && !hasAccessibleConstructor(t)) {
        protoclass()
            .report()
            .withElement(t)
            .warning(About.INCOMPAT,
                "%s should have non-private no-argument constructor to be supported as Builder extend/super type",
                t.getSimpleName());

        return false;
      }

      return true;
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
