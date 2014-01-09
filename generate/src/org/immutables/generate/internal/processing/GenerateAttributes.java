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

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Booleans;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.lang.model.type.TypeMirror;
import static com.google.common.base.Preconditions.*;

/**
 * Immutable implementation of GenerateAttribute.
 * <p>
 * Use static static factory methods to create instances: {@code of()} or {@code builder()}.
 */
public final class GenerateAttributes {
  private GenerateAttributes() {}

  /**
   * Predicate on instance of GenerateAttribute that evaluates attribute isGenerateFunction.
   * @return predicate on GenerateAttribute evaluates to 'isGenerateFunction' attribute
   */
  public static Predicate<GenerateAttribute> isGenerateFunction() {
    return IsGenerateFunctionPredicate.INSTANCE;
  }

  private enum IsGenerateFunctionPredicate
      implements Predicate<GenerateAttribute> {
    INSTANCE;
    @Override
    public boolean apply(GenerateAttribute input) {
      return input.isGenerateFunction();
    }

    @Override
    public String toString() {
      return "GenerateAttributes.isGenerateFunction()";
    }
  }

  /**
   * Predicate on instance of GenerateAttribute that evaluates attribute isGeneratePredicate.
   * @return predicate on GenerateAttribute evaluates to 'isGeneratePredicate' attribute
   */
  public static Predicate<GenerateAttribute> isGeneratePredicate() {
    return IsGeneratePredicatePredicate.INSTANCE;
  }

  private enum IsGeneratePredicatePredicate
      implements Predicate<GenerateAttribute> {
    INSTANCE;
    @Override
    public boolean apply(GenerateAttribute input) {
      return input.isGeneratePredicate();
    }

    @Override
    public String toString() {
      return "GenerateAttributes.isGeneratePredicate()";
    }
  }

  /**
   * Predicate on instance of GenerateAttribute that evaluates attribute isGenerateDefault.
   * @return predicate on GenerateAttribute evaluates to 'isGenerateDefault' attribute
   */
  public static Predicate<GenerateAttribute> isGenerateDefault() {
    return IsGenerateDefaultPredicate.INSTANCE;
  }

  private enum IsGenerateDefaultPredicate
      implements Predicate<GenerateAttribute> {
    INSTANCE;
    @Override
    public boolean apply(GenerateAttribute input) {
      return input.isGenerateDefault();
    }

    @Override
    public String toString() {
      return "GenerateAttributes.isGenerateDefault()";
    }
  }

  /**
   * Predicate on instance of GenerateAttribute that evaluates attribute isGenerateDerived.
   * @return predicate on GenerateAttribute evaluates to 'isGenerateDerived' attribute
   */
  public static Predicate<GenerateAttribute> isGenerateDerived() {
    return IsGenerateDerivedPredicate.INSTANCE;
  }

  private enum IsGenerateDerivedPredicate
      implements Predicate<GenerateAttribute> {
    INSTANCE;
    @Override
    public boolean apply(GenerateAttribute input) {
      return input.isGenerateDerived();
    }

    @Override
    public String toString() {
      return "GenerateAttributes.isGenerateDerived()";
    }
  }

  /**
   * Predicate on instance of GenerateAttribute that evaluates attribute isGenerateAbstract.
   * @return predicate on GenerateAttribute evaluates to 'isGenerateAbstract' attribute
   */
  public static Predicate<GenerateAttribute> isGenerateAbstract() {
    return IsGenerateAbstractPredicate.INSTANCE;
  }

  private enum IsGenerateAbstractPredicate
      implements Predicate<GenerateAttribute> {
    INSTANCE;
    @Override
    public boolean apply(GenerateAttribute input) {
      return input.isGenerateAbstract();
    }

    @Override
    public String toString() {
      return "GenerateAttributes.isGenerateAbstract()";
    }
  }

  /**
   * Predicate on instance of GenerateAttribute that evaluates attribute isPrimitive.
   * @return predicate on GenerateAttribute evaluates to 'isPrimitive' attribute
   */
  public static Predicate<GenerateAttribute> isPrimitive() {
    return IsPrimitivePredicate.INSTANCE;
  }

  private enum IsPrimitivePredicate
      implements Predicate<GenerateAttribute> {
    INSTANCE;
    @Override
    public boolean apply(GenerateAttribute input) {
      return input.isPrimitive();
    }

    @Override
    public String toString() {
      return "GenerateAttributes.isPrimitive()";
    }
  }

  @Immutable
  private static final class ImmutableGenerateAttribute
      extends GenerateAttribute {
    private final TypeMirror internalTypeMirror;
    private final boolean isGenerateFunction;
    private final boolean isGeneratePredicate;
    private final boolean isGenerateDefault;
    private final boolean isGenerateLazy;
    private final boolean isGenerateDerived;
    private final boolean isGenerateAbstract;
    private final String internalName;
    private final String internalTypeName;

    ImmutableGenerateAttribute(Builder builder) {
      this.internalTypeMirror = checkNotNull(builder.internalTypeMirror);
      this.internalName = checkNotNull(builder.internalName);
      this.internalTypeName = checkNotNull(builder.internalTypeName);
      this.isGenerateFunction = builder.isGenerateFunctionIsSet
          ? builder.isGenerateFunction
          : super.isGenerateFunction();
      this.isGeneratePredicate = builder.isGeneratePredicateIsSet
          ? builder.isGeneratePredicate
          : super.isGeneratePredicate();
      this.isGenerateDefault = builder.isGenerateDefaultIsSet
          ? builder.isGenerateDefault
          : super.isGenerateDefault();
      this.isGenerateDerived = builder.isGenerateDerivedIsSet
          ? builder.isGenerateDerived
          : super.isGenerateDerived();
      this.isGenerateAbstract = builder.isGenerateAbstractIsSet
          ? builder.isGenerateAbstract
          : super.isGenerateAbstract();
      this.isGenerateLazy = builder.isGenerateLazyIsSet
          ? builder.isGenerateLazy
          : super.isGenerateLazy();
    }

    @Override
    public TypeMirror internalTypeMirror() {
      return internalTypeMirror;
    }

    @Override
    public boolean isGenerateFunction() {
      return isGenerateFunction;
    }

    @Override
    public boolean isGenerateLazy() {
      return isGenerateLazy;
    }

    @Override
    public boolean isGeneratePredicate() {
      return isGeneratePredicate;
    }

    @Override
    public boolean isGenerateDefault() {
      return isGenerateDefault;
    }

    @Override
    public boolean isGenerateDerived() {
      return isGenerateDerived;
    }

    @Override
    public boolean isGenerateAbstract() {
      return isGenerateAbstract;
    }

    @Override
    public String internalName() {
      return internalName;
    }

    @Override
    public String internalTypeName() {
      return internalTypeName;
    }

    @Override
    public boolean equals(Object another) {
      return this == another
          || (another instanceof ImmutableGenerateAttribute && equalTo((ImmutableGenerateAttribute) another));
    }

    private boolean equalTo(ImmutableGenerateAttribute another) {
      return true
          && internalTypeMirror.equals(another.internalTypeMirror)
          && isGenerateFunction == another.isGenerateFunction
          && isGeneratePredicate == another.isGeneratePredicate
          && isGenerateDefault == another.isGenerateDefault
          && isGenerateDerived == another.isGenerateDerived
          && isGenerateAbstract == another.isGenerateAbstract
          && internalName.equals(another.internalName)
          && internalTypeName.equals(another.internalTypeName);
    }

    @Override
    public int hashCode() {
      int h = 31;
      h = h * 17 + internalTypeMirror.hashCode();
      h = h * 17 + Booleans.hashCode(isGenerateFunction);
      h = h * 17 + Booleans.hashCode(isGeneratePredicate);
      h = h * 17 + Booleans.hashCode(isGenerateDefault);
      h = h * 17 + Booleans.hashCode(isGenerateDerived);
      h = h * 17 + Booleans.hashCode(isGenerateAbstract);
      h = h * 17 + internalName.hashCode();
      h = h * 17 + internalTypeName.hashCode();
      return h;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper("GenerateAttribute")
          .add("internalTypeMirror", internalTypeMirror)
          .add("isGenerateFunction", isGenerateFunction)
          .add("isGeneratePredicate", isGeneratePredicate)
          .add("isGenerateDefault", isGenerateDefault)
          .add("isGenerateDerived", isGenerateDerived)
          .add("isGenerateAbstract", isGenerateAbstract)
          .add("internalName", internalName)
          .add("internalTypeName", internalTypeName)
          .toString();
    }
  }

  /**
   * Creates builder for {@link GenerateAttribute}.
   * @return new GenerateAttribute builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builds instances of {@link GenerateAttribute}.
   * Builder is not thread safe and generally should not be stored in fields and collections,
   * but used immediately to create instances.
   */
  @NotThreadSafe
  public static final class Builder {
    private static final String REQUIRED_ATTRIBUTE =
        "Cannot build GenerateAttribute: required attribute '%s' is not set";

    @Nullable
    private TypeMirror internalTypeMirror;
    private boolean isGenerateFunction;
    private boolean isGenerateFunctionIsSet;
    private boolean isGeneratePredicate;
    private boolean isGeneratePredicateIsSet;
    private boolean isGenerateDefault;
    private boolean isGenerateDefaultIsSet;
    private boolean isGenerateDerived;
    private boolean isGenerateDerivedIsSet;
    private boolean isGenerateAbstract;
    private boolean isGenerateAbstractIsSet;
    private boolean isGenerateLazy;
    private boolean isGenerateLazyIsSet;
    @Nullable
    private String internalName;
    @Nullable
    private String internalTypeName;
    private ImmutableList.Builder<String> typeParametersBuilder =
        ImmutableList.builder();

    private Builder() {}

    /**
     * Fill builder with values from provided {@link GenerateAttribute} instance.
     * @param fromInstance instance to copy values from
     * @return {@code this} builder
     */
    public Builder copy(GenerateAttribute fromInstance) {
      checkNotNull(fromInstance);
      internalTypeMirror(fromInstance.internalTypeMirror());
      isGenerateFunction(fromInstance.isGenerateFunction());
      isGeneratePredicate(fromInstance.isGeneratePredicate());
      isGenerateDefault(fromInstance.isGenerateDefault());
      isGenerateDerived(fromInstance.isGenerateDerived());
      isGenerateAbstract(fromInstance.isGenerateAbstract());
      internalName(fromInstance.internalName());
      internalTypeName(fromInstance.internalTypeName());
      addTypeParameters(fromInstance.typeParameters());
      return this;
    }

    /**
     * Initializes value for {@link GenerateAttribute#internalTypeMirror()}.
     * @param internalTypeMirror
     *          value for internalTypeMirror, not {@code null}
     * @return {@code this} builder
     */
    public Builder internalTypeMirror(TypeMirror internalTypeMirror) {
      this.internalTypeMirror = checkNotNull(internalTypeMirror);
      return this;
    }

    /**
     * Initializes value for {@link GenerateAttribute#isGenerateFunction()}.
     * @param isGenerateFunction
     *          value for isGenerateFunction, not {@code null}
     * @return {@code this} builder
     */
    public Builder isGenerateFunction(boolean isGenerateFunction) {
      this.isGenerateFunction = isGenerateFunction;
      isGenerateFunctionIsSet = true;
      return this;
    }

    /**
     * Initializes value for {@link GenerateAttribute#isGeneratePredicate()}.
     * @param isGeneratePredicate
     *          value for isGeneratePredicate, not {@code null}
     * @return {@code this} builder
     */
    public Builder isGeneratePredicate(boolean isGeneratePredicate) {
      this.isGeneratePredicate = isGeneratePredicate;
      isGeneratePredicateIsSet = true;
      return this;
    }

    /**
     * Initializes value for {@link GenerateAttribute#isGenerateDefault()}.
     * @param isGenerateDefault
     *          value for isGenerateDefault, not {@code null}
     * @return {@code this} builder
     */
    public Builder isGenerateDefault(boolean isGenerateDefault) {
      this.isGenerateDefault = isGenerateDefault;
      isGenerateDefaultIsSet = true;
      return this;
    }

    /**
     * Initializes value for {@link GenerateAttribute#isGenerateDerived()}.
     * @param isGenerateDerived
     *          value for isGenerateDerived, not {@code null}
     * @return {@code this} builder
     */
    public Builder isGenerateDerived(boolean isGenerateDerived) {
      this.isGenerateDerived = isGenerateDerived;
      isGenerateDerivedIsSet = true;
      return this;
    }

    public Builder isGenerateLazy(boolean isGenerateLazy) {
      this.isGenerateLazy = isGenerateLazy;
      isGenerateLazyIsSet = true;
      return this;
    }

    /**
     * Initializes value for {@link GenerateAttribute#isGenerateAbstract()}.
     * @param isGenerateAbstract
     *          value for isGenerateAbstract, not {@code null}
     * @return {@code this} builder
     */
    public Builder isGenerateAbstract(boolean isGenerateAbstract) {
      this.isGenerateAbstract = isGenerateAbstract;
      isGenerateAbstractIsSet = true;
      return this;
    }

    /**
     * Initializes value for {@link GenerateAttribute#internalName()}.
     * @param internalName
     *          value for internalName, not {@code null}
     * @return {@code this} builder
     */
    public Builder internalName(String internalName) {
      this.internalName = checkNotNull(internalName);
      return this;
    }

    /**
     * Initializes value for {@link GenerateAttribute#internalTypeName()}.
     * @param internalTypeName
     *          value for internalTypeName, not {@code null}
     * @return {@code this} builder
     */
    public Builder internalTypeName(String internalTypeName) {
      this.internalTypeName = checkNotNull(internalTypeName);
      return this;
    }

    /**
     * Adds element to {@link GenerateAttribute#typeParameters() list}.
     * @param typeParametersElement single typeParameters element
     * @return {@code this} builder
     */
    public Builder addTypeParameters(String typeParametersElement) {
      typeParametersBuilder.add(typeParametersElement);
      return this;
    }

    /**
     * Adds elements to {@link GenerateAttribute#typeParameters() list}.
     * @param typeParametersElements
     *          rest typeParameters elements
     * @return {@code this} builder
     */
    public Builder addTypeParameters(String... typeParametersElements) {
      for (String it : typeParametersElements) {
        typeParametersBuilder.add(it);
      }
      return this;
    }

    /**
     * Adds elements to {@link GenerateAttribute#typeParameters()} list}.
     * @param typeParametersElements
     *          iterable typeParameters elements
     * @return {@code this} builder
     */
    public Builder addTypeParameters(
        Iterable<? extends String> typeParametersElements) {
      typeParametersBuilder.addAll(typeParametersElements);
      return this;
    }

    /**
     * Clears elements for {@link GenerateAttribute#typeParameters()} list.
     * @return {@code this} builder
     */
    public Builder clearTypeParameters() {
      typeParametersBuilder = ImmutableList.builder();
      return this;
    }

    /**
     * Builds new {@link GenerateAttribute}.
     * @return immutable instance of GenerateAttribute
     */
    public GenerateAttribute build() {
      checkState(internalTypeMirror != null, REQUIRED_ATTRIBUTE, "internalTypeMirror");
      checkState(internalName != null, REQUIRED_ATTRIBUTE, "internalName");
      checkState(internalTypeName != null, REQUIRED_ATTRIBUTE, "internalTypeName");
      return new ImmutableGenerateAttribute(this);
    }
  }
}
