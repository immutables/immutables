/*
   Copyright 2013-2014 Immutables Authors and Contributors

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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Predicate;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Only functions left in this previously generated file (long time ago)
 */
final class ValueAttributeFunctions {
  private ValueAttributeFunctions() {}

  /**
   * Predicate on instance of DiscoveredAttribute that evaluates attribute isGenerateDefault.
   * @return predicate on DiscoveredAttribute evaluates to 'isGenerateDefault' attribute
   */
  public static Predicate<ValueAttribute> isGenerateDefault() {
    return IsGenerateDefaultPredicate.INSTANCE;
  }

  private enum IsGenerateDefaultPredicate
      implements Predicate<ValueAttribute> {
    INSTANCE;
    @Override
    public boolean apply(ValueAttribute input) {
      return input.isGenerateDefault;
    }

    @Override
    public String toString() {
      return ValueAttributeFunctions.class.getSimpleName() + ".isGenerateDefault()";
    }
  }

  /**
   * Predicate on instance of DiscoveredAttribute that evaluates attribute isGenerateDerived.
   * @return predicate on DiscoveredAttribute evaluates to 'isGenerateDerived' attribute
   */
  public static Predicate<ValueAttribute> isGenerateDerived() {
    return IsGenerateDerivedPredicate.INSTANCE;
  }

  private enum IsGenerateDerivedPredicate
      implements Predicate<ValueAttribute> {
    INSTANCE;
    @Override
    public boolean apply(ValueAttribute input) {
      return input.isGenerateDerived;
    }

    @Override
    public String toString() {
      return ValueAttributeFunctions.class.getSimpleName() + ".isGenerateDerived()";
    }
  }

  /**
   * Predicate on instance of DiscoveredAttribute that evaluates attribute isGenerateAbstract.
   * @return predicate on DiscoveredAttribute evaluates to 'isGenerateAbstract' attribute
   */
  public static Predicate<ValueAttribute> isGenerateAbstract() {
    return IsGenerateAbstractPredicate.INSTANCE;
  }

  private enum IsGenerateAbstractPredicate
      implements Predicate<ValueAttribute> {
    INSTANCE;
    @Override
    public boolean apply(ValueAttribute input) {
      return input.isGenerateAbstract;
    }

    @Override
    public String toString() {
      return ValueAttributeFunctions.class.getSimpleName() + ".isGenerateAbstract()";
    }
  }

  /**
   * Predicate on instance of DiscoveredAttribute that evaluates attribute isPrimitive.
   * @return predicate on DiscoveredAttribute evaluates to 'isPrimitive' attribute
   */
  public static Predicate<ValueAttribute> isPrimitive() {
    return IsPrimitivePredicate.INSTANCE;
  }

  private enum IsPrimitivePredicate
      implements Predicate<ValueAttribute> {
    INSTANCE;
    @Override
    public boolean apply(ValueAttribute input) {
      return input.isPrimitive();
    }

    @Override
    public String toString() {
      return ValueAttributeFunctions.class.getSimpleName() + ".isPrimitive()";
    }
  }

  public static Predicate<ValueAttribute> isNestedImmutableWithBuilder() {
    return NestedBuilderPredicate.INSTANCE;
  }

  private enum NestedBuilderPredicate
      implements Predicate<ValueAttribute> {
    INSTANCE;

    @Override
    public boolean apply(ValueAttribute input) {
      return ImmutableNestedBuilderReflection.of(input).isNestedBuilder();
    }

    @Override
    public String toString() {
      return ValueAttributeFunctions.class.getSimpleName() + ".isNestedImmutableWithBuilder()";
    }
  }

  public static Predicate<ValueAttribute> isListType() {
    return IsListType.INSTANCE;
  }

  private enum IsListType
      implements Predicate<ValueAttribute> {
    INSTANCE;

    @Override
    public boolean apply(ValueAttribute input) {
      return input.isListType();
    }

    @Override
    public String toString() {
      return ValueAttributeFunctions.class.getSimpleName() + ".isListKind()";
    }
  }

  public static Predicate<ValueAttribute> uniqueOnNestedBuilder() {
    return new UniqueOnNestedBuilder();
  }

  private static class UniqueOnNestedBuilder implements Predicate<ValueAttribute> {
    Set<NestedBuilderDescriptor> uniqueSet;

    public UniqueOnNestedBuilder() {
      uniqueSet = new HashSet<>();
    }

    @Nullable
    @Override
    public boolean apply(ValueAttribute valueAttribute) {

      if (uniqueSet.contains(checkNotNull(valueAttribute.getNestedBuilder()))) {
        return false;
      }

      uniqueSet.add(valueAttribute.getNestedBuilder());

      return true;
    }

    @Override
    public String toString() {
      return ValueAttributeFunctions.class.getSimpleName() + ".uniqueOnNestedBuilder()";
    }
  }
}
