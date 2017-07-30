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

import java.lang.annotation.Annotation;
import org.immutables.mirror.Mirror;

public final class ValueMirrors {
  private ValueMirrors() {}

  @Mirror.Annotation("org.immutables.value.Value")
  public @interface ValueUmbrella {}

  @Mirror.Annotation("org.immutables.value.Value.Immutable")
  public @interface Immutable {

    boolean singleton() default false;

    boolean intern() default false;

    boolean copy() default false;

    boolean prehash() default false;

    boolean builder() default true;
  }

  @Mirror.Annotation("org.immutables.value.Value.Include")
  public @interface Include {
    Class<?>[] value();
  }

  @Mirror.Annotation("org.immutables.value.Value.Enclosing")
  public @interface Enclosing {}

  @Mirror.Annotation("org.immutables.value.Value.Derived")
  public @interface Derived {}

  @Mirror.Annotation("org.immutables.value.Value.Default")
  public @interface Default {}

  @Mirror.Annotation("org.immutables.value.Value.Auxiliary")
  public @interface Auxiliary {}

  @Mirror.Annotation("org.immutables.value.Value.Lazy")
  public @interface Lazy {}

  @Mirror.Annotation("org.immutables.value.Value.Parameter")
  public @interface Parameter {
    int order() default -1;

    boolean value() default true;
  }

  @Mirror.Annotation("org.immutables.value.Value.Check")
  public @interface Check {}

  @Mirror.Annotation("org.immutables.value.Value.NaturalOrder")
  public @interface NaturalOrder {}

  @Mirror.Annotation("org.immutables.value.Value.ReverseOrder")
  public @interface ReverseOrder {}

  @Mirror.Annotation("org.immutables.value.Value.Modifiable")
  public @interface Modifiable {}

  @Mirror.Annotation("org.immutables.value.Value.Style.Depluralize")
  public @interface Depluralize {
    String[] dictionary() default {};
  }

  @Mirror.Annotation("org.immutables.value.Value.Redacted")
  public @interface Redacted {}

  @Mirror.Annotation("org.immutables.value.Value.Style")
  public @interface Style {
    String[] get() default {};

    String init() default "*";

    String with() default "with*";

    String add() default "add*";

    String addAll() default "addAll*";

    String put() default "put*";

    String putAll() default "putAll*";

    String copyOf() default "copyOf";

    String of() default "of";

    String instance() default "of";

    String builder() default "builder";

    String newBuilder() default "new";

    String from() default "from";

    String build() default "build";

    String buildOrThrow() default "";

    String isInitialized() default "isInitialized";

    String isSet() default "*IsSet";

    String set() default "set*";

    String unset() default "unset*";

    String clear() default "clear";

    String create() default "create";

    String toImmutable() default "toImmutable";

    String typeBuilder() default "Builder";

    String typeInnerBuilder() default "Builder";

    String[] typeAbstract() default {};

    String typeImmutable() default "Immutable*";

    String typeImmutableEnclosing() default "Immutable*";

    String typeImmutableNested() default "*";

    String typeModifiable() default "Modifiable*";

    String typeWith() default "With*";

    String packageGenerated() default "*";

    Immutable defaults() default @Immutable;

    boolean strictBuilder() default false;

    ValidationMethod validationMethod() default ValidationMethod.SIMPLE;

    boolean allParameters() default false;

    boolean defaultAsDefault() default false;

    boolean headerComments() default false;

    boolean jdkOnly() default false;

    Class<? extends Annotation>[] passAnnotations() default {};

    Class<? extends Annotation>[] additionalJsonAnnotations() default {};

    ImplementationVisibility visibility() default ImplementationVisibility.SAME;

    boolean optionalAcceptNullable() default false;

    boolean generateSuppressAllWarnings() default true;

    boolean privateNoargConstructor() default false;

    boolean attributelessSingleton() default false;

    boolean unsafeDefaultAndDerived() default false;

    boolean clearBuilder() default false;

    boolean deferCollectionAllocation() default false;

    boolean deepImmutablesDetection() default false;

    boolean overshadowImplementation() default false;

    boolean implementationNestedInBuilder() default false;

    boolean forceJacksonPropertyNames() default true;

    boolean forceJacksonIgnoreFields() default false;

    boolean jacksonIntegration() default true;

    BuilderVisibility builderVisibility() default BuilderVisibility.PUBLIC;

    Class<? extends Exception> throwForInvalidImmutableState() default IllegalStateException.class;

    boolean depluralize() default false;

    String[] depluralizeDictionary() default {};

    Class<?>[] immutableCopyOfRoutines() default {};

    boolean stagedBuilder() default false;

    boolean builtinContainerAttributes() default true;

    boolean beanFriendlyModifiables() default false;

    boolean allMandatoryParameters() default false;

    String redactedMask() default "";

    boolean attributeBuilderDetection() default false;

    String[] attributeBuilder() default {"*Builder", "builder", "new"};

    String getBuilder() default "*Builder";

    String setBuilder() default "*Builder";

    String addBuilder() default "add*Builder";

    String addAllBuilder() default "addAll*Builders";

    String getBuilders() default "*Builders";

    String nullableAnnotation() default "Nullable";

    public enum ImplementationVisibility {
      PUBLIC,
      SAME,
      SAME_NON_RETURNED,
      PACKAGE,
      PRIVATE
    }

    public enum BuilderVisibility {
      PUBLIC,
      SAME,
      PACKAGE
    }

    public enum ValidationMethod {
      NONE,
      SIMPLE,
      VALIDATION_API
    }
  }
}
