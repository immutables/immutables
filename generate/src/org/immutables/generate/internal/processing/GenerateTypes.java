/*
    Copyright 2013 Immutables.org authors

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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Booleans;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.lang.model.element.TypeElement;
import static com.google.common.base.Preconditions.*;

/**
 * Immutable implementation of GenerateType.
 * <p>
 * Use static static factory methods to create instances: {@code of()} or {@code builder()}.
 */
public final class GenerateTypes {
  private GenerateTypes() {
  }

  @Immutable
  private static final class ImmutableGenerateType
      extends GenerateType {
    private final String packageFullyQualifiedName;
    private final String internalName;
    private final TypeElement internalTypeElement;
    private final List<GenerateAttribute> attributes;
    private final boolean isUseBuilder;
    private final boolean isGenerateCompact;
    private final boolean isHashCodeDefined;
    private final boolean isEqualToDefined;
    private final boolean isToStringDefined;

    ImmutableGenerateType(Builder builder) {
      this.packageFullyQualifiedName = checkNotNull(builder.packageFullyQualifiedName);
      this.internalName = checkNotNull(builder.internalName);
      this.internalTypeElement = checkNotNull(builder.internalTypeElement);
      this.attributes = builder.attributesBuilder.build();
      this.isUseBuilder = builder.isUseBuilderIsSet
          ? builder.isUseBuilder
          : super.isUseBuilder();
      this.isGenerateCompact = builder.isGenerateCompactIsSet
          ? builder.isGenerateCompact
          : super.isGenerateModifiable();
      this.isHashCodeDefined = builder.isHashCodeDefinedIsSet
          ? builder.isHashCodeDefined
          : super.isHashCodeDefined();
      this.isEqualToDefined = builder.isEqualToDefinedIsSet
          ? builder.isEqualToDefined
          : super.isEqualToDefined();
      this.isToStringDefined = builder.isToStringDefinedIsSet
          ? builder.isToStringDefined
          : super.isToStringDefined();
    }

    @Override
    public String packageFullyQualifiedName() {
      return packageFullyQualifiedName;
    }

    @Override
    public String internalName() {
      return internalName;
    }

    @Override
    public TypeElement internalTypeElement() {
      return internalTypeElement;
    }

    @Override
    public List<GenerateAttribute> attributes() {
      return attributes;
    }

    @Override
    public boolean isUseBuilder() {
      return isUseBuilder;
    }

    @Override
    public boolean isGenerateModifiable() {
      return isGenerateCompact;
    }

    @Override
    public boolean isHashCodeDefined() {
      return isHashCodeDefined;
    }

    @Override
    public boolean isEqualToDefined() {
      return isEqualToDefined;
    }

    @Override
    public boolean isToStringDefined() {
      return isToStringDefined;
    }

    @Override
    public boolean equals(Object another) {
      return this == another
          || (another instanceof ImmutableGenerateType && equalTo((ImmutableGenerateType) another));
    }

    private boolean equalTo(ImmutableGenerateType another) {
      return true
          && packageFullyQualifiedName.equals(another.packageFullyQualifiedName)
          && internalName.equals(another.internalName)
          && internalTypeElement.equals(another.internalTypeElement)
          && attributes.equals(another.attributes)
          && isUseBuilder == another.isUseBuilder
          && isGenerateCompact == another.isGenerateCompact
          && isHashCodeDefined == another.isHashCodeDefined
          && isEqualToDefined == another.isEqualToDefined
          && isToStringDefined == another.isToStringDefined;
    }

    @Override
    public int hashCode() {
      int h = 31;
      h = h * 17 + packageFullyQualifiedName.hashCode();
      h = h * 17 + internalName.hashCode();
      h = h * 17 + internalTypeElement.hashCode();
      h = h * 17 + attributes.hashCode();
      h = h * 17 + Booleans.hashCode(isUseBuilder);
      h = h * 17 + Booleans.hashCode(isGenerateCompact);
      h = h * 17 + Booleans.hashCode(isHashCodeDefined);
      h = h * 17 + Booleans.hashCode(isEqualToDefined);
      h = h * 17 + Booleans.hashCode(isToStringDefined);
      return h;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper("GenerateType")
          .add("packageFullyQualifiedName", packageFullyQualifiedName)
          .add("internalName", internalName)
          .add("internalTypeElement", internalTypeElement)
          .add("attributes", attributes)
          .add("isUseBuilder", isUseBuilder)
          .add("isGenerateCompact", isGenerateCompact)
          .add("isHashCodeDefined", isHashCodeDefined)
          .add("isEqualToDefined", isEqualToDefined)
          .add("isToStringDefined", isToStringDefined)
          .toString();
    }
  }

  /**
   * Creates builder for {@link GenerateType}.
   * @return new GenerateType builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builds instances of {@link GenerateType}.
   * Builder is not thread safe and generally should not be stored in fields and collections,
   * but used immediately to create instances.
   */
  @NotThreadSafe
  public static final class Builder {
    private static final String REQUIRED_ATTRIBUTE =
        "Cannot build GenerateType: required attribute '%s' is not set";

    @Nullable
    private String validationMethodName;
    @Nullable
    private String packageFullyQualifiedName;
    @Nullable
    private String internalName;
    @Nullable
    private TypeElement internalTypeElement;
    private ImmutableList.Builder<GenerateAttribute> attributesBuilder =
        ImmutableList.builder();
    private boolean isUseBuilder;
    private boolean isUseBuilderIsSet;
    private boolean isGenerateCompact;
    private boolean isGenerateCompactIsSet;
    private boolean isHashCodeDefined;
    private boolean isHashCodeDefinedIsSet;
    private boolean isEqualToDefined;
    private boolean isEqualToDefinedIsSet;
    private boolean isToStringDefined;
    private boolean isToStringDefinedIsSet;

    private Builder() {
    }

    /**
     * Fill builder with values from provided {@link GenerateType} instance.
     * @param fromInstance instance to copy values from
     * @return {@code this} builder
     */
    public Builder copy(GenerateType fromInstance) {
      checkNotNull(fromInstance);
      packageFullyQualifiedName(fromInstance.packageFullyQualifiedName());
      internalName(fromInstance.internalName());
      internalTypeElement(fromInstance.internalTypeElement());
      addAttributes(fromInstance.attributes());
      isUseBuilder(fromInstance.isUseBuilder());
      isGenerateCompact(fromInstance.isGenerateModifiable());
      isHashCodeDefined(fromInstance.isHashCodeDefined());
      isEqualToDefined(fromInstance.isEqualToDefined());
      isToStringDefined(fromInstance.isToStringDefined());
      return this;
    }

    public Builder validationMethodName(String validationMethodName) {
      this.validationMethodName = validationMethodName;
      return this;
    }

    /**
     * Initializes value for {@link GenerateType#packageFullyQualifiedName()}.
     * @param packageFullyQualifiedName
     *          value for packageFullyQualifiedName, not {@code null}
     * @return {@code this} builder
     */
    public Builder packageFullyQualifiedName(String packageFullyQualifiedName) {
      this.packageFullyQualifiedName = checkNotNull(packageFullyQualifiedName);
      return this;
    }

    /**
     * Initializes value for {@link GenerateType#internalName()}.
     * @param internalName
     *          value for internalName, not {@code null}
     * @return {@code this} builder
     */
    public Builder internalName(String internalName) {
      this.internalName = checkNotNull(internalName);
      return this;
    }

    /**
     * Initializes value for {@link GenerateType#internalTypeElement()}.
     * @param internalTypeElement
     *          value for internalTypeElement, not {@code null}
     * @return {@code this} builder
     */
    public Builder internalTypeElement(TypeElement internalTypeElement) {
      this.internalTypeElement = checkNotNull(internalTypeElement);
      return this;
    }

    /**
     * Adds element to {@link GenerateType#attributes() list}.
     * @param attributesElement single attributes element
     * @return {@code this} builder
     */
    public Builder addAttributes(GenerateAttribute attributesElement) {
      attributesBuilder.add(attributesElement);
      return this;
    }

    /**
     * Adds elements to {@link GenerateType#attributes() list}.
     * @param attributesElements
     *          rest attributes elements
     * @return {@code this} builder
     */
    public Builder addAttributes(GenerateAttribute... attributesElements) {
      for (GenerateAttribute it : attributesElements) {
        attributesBuilder.add(it);
      }
      return this;
    }

    /**
     * Adds elements to {@link GenerateType#attributes()} list}.
     * @param attributesElements
     *          iterable attributes elements
     * @return {@code this} builder
     */
    public Builder addAttributes(
        Iterable<? extends GenerateAttribute> attributesElements) {
      attributesBuilder.addAll(attributesElements);
      return this;
    }

    /**
     * Clears elements for {@link GenerateType#attributes()} list.
     * @return {@code this} builder
     */
    public Builder clearAttributes() {
      attributesBuilder = ImmutableList.builder();
      return this;
    }

    /**
     * Initializes value for {@link GenerateType#isUseBuilder()}.
     * @param isUseBuilder
     *          value for isUseBuilder, not {@code null}
     * @return {@code this} builder
     */
    public Builder isUseBuilder(boolean isUseBuilder) {
      this.isUseBuilder = isUseBuilder;
      isUseBuilderIsSet = true;
      return this;
    }

    /**
     * Initializes value for {@link GenerateType#isGenerateModifiable()}.
     * @param isGenerateCompact
     *          value for isGenerateCompact, not {@code null}
     * @return {@code this} builder
     */
    public Builder isGenerateCompact(boolean isGenerateCompact) {
      this.isGenerateCompact = isGenerateCompact;
      isGenerateCompactIsSet = true;
      return this;
    }

    /**
     * Initializes value for {@link GenerateType#isHashCodeDefined()}.
     * @param isHashCodeDefined
     *          value for isHashCodeDefined, not {@code null}
     * @return {@code this} builder
     */
    public Builder isHashCodeDefined(boolean isHashCodeDefined) {
      this.isHashCodeDefined = isHashCodeDefined;
      isHashCodeDefinedIsSet = true;
      return this;
    }

    /**
     * Initializes value for {@link GenerateType#isEqualToDefined()}.
     * @param isEqualToDefined
     *          value for isEqualToDefined, not {@code null}
     * @return {@code this} builder
     */
    public Builder isEqualToDefined(boolean isEqualToDefined) {
      this.isEqualToDefined = isEqualToDefined;
      isEqualToDefinedIsSet = true;
      return this;
    }

    /**
     * Initializes value for {@link GenerateType#isToStringDefined()}.
     * @param isToStringDefined
     *          value for isToStringDefined, not {@code null}
     * @return {@code this} builder
     */
    public Builder isToStringDefined(boolean isToStringDefined) {
      this.isToStringDefined = isToStringDefined;
      isToStringDefinedIsSet = true;
      return this;
    }

    /**
     * Builds new {@link GenerateType}.
     * @return immutable instance of GenerateType
     */
    public GenerateType build() {
      checkState(packageFullyQualifiedName != null, REQUIRED_ATTRIBUTE, "packageFullyQualifiedName");
      checkState(internalName != null, REQUIRED_ATTRIBUTE, "internalName");
      checkState(internalTypeElement != null, REQUIRED_ATTRIBUTE, "internalTypeElement");

      ImmutableGenerateType type = new ImmutableGenerateType(this);
      type.setValidationMethodName(validationMethodName);
      return type;
    }
  }
}
