/*
   Copyright 2015 Immutables Authors and Contributors

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

import com.google.common.collect.ImmutableSet;
import org.immutables.value.Value;
import java.lang.annotation.Annotation;

/**
 * We copy styles to StyleInfo to safely cache styles between rounds etc and prevent any memory
 * leaks by unnecessary retaining compiler internal structures.
 */
@SuppressWarnings("all")
@Value.Immutable(intern = true, builder = false)
public abstract class StyleInfo implements ValueMirrors.Style {

  @Override
  public Class<? extends Annotation> annotationType() {
    return ValueMirrors.Style.class;
  }

  @Override
  @Value.Parameter
  public abstract String[] get();

  @Value.Parameter
  @Override
  public abstract String init();

  @Value.Parameter
  @Override
  public abstract String with();

  @Value.Parameter
  @Override
  public abstract String add();

  @Value.Parameter
  @Override
  public abstract String addAll();

  @Value.Parameter
  @Override
  public abstract String put();

  @Value.Parameter
  @Override
  public abstract String putAll();

  @Value.Parameter
  @Override
  public abstract String copyOf();

  @Value.Parameter
  @Override
  public abstract String of();

  @Value.Parameter
  @Override
  public abstract String instance();

  @Value.Parameter
  @Override
  public abstract String builder();

  @Value.Parameter
  @Override
  public abstract String newBuilder();

  @Value.Parameter
  @Override
  public abstract String from();

  @Value.Parameter
  @Override
  public abstract String build();

  @Value.Parameter
  @Override
  public abstract String buildOrThrow();

  @Value.Parameter
  @Override
  public abstract String isInitialized();

  @Value.Parameter
  @Override
  public abstract String isSet();

  @Value.Parameter
  @Override
  public abstract String set();

  @Value.Parameter
  @Override
  public abstract String unset();

  @Value.Parameter
  @Override
  public abstract String clear();

  @Value.Parameter
  @Override
  public abstract String create();

  @Value.Parameter
  @Override
  public abstract String toImmutable();

  @Value.Parameter
  @Override
  public abstract String typeBuilder();

  @Value.Parameter
  @Override
  public abstract String typeInnerBuilder();

  @Value.Parameter
  @Override
  public abstract String[] typeAbstract();

  @Value.Parameter
  @Override
  public abstract String typeImmutable();

  @Value.Parameter
  @Override
  public abstract String typeImmutableEnclosing();

  @Value.Parameter
  @Override
  public abstract String typeImmutableNested();

  @Value.Parameter
  @Override
  public abstract String typeModifiable();

  @Value.Parameter
  @Override
  public abstract String typeWith();

  @Value.Parameter
  @Override
  public abstract String packageGenerated();

  @Override
  @Value.Parameter
  public abstract ValueImmutableInfo defaults();

  @Value.Parameter
  @Override
  public abstract boolean strictBuilder();

  @Value.Parameter
  @Override
  public abstract ValidationMethod validationMethod();

  @Value.Parameter
  @Override
  public abstract boolean allParameters();

  @Value.Parameter
  @Override
  public abstract boolean defaultAsDefault();

  @Value.Parameter
  @Override
  public abstract boolean headerComments();

  @Value.Parameter
  @Override
  public abstract boolean jdkOnly();

  @Value.Parameter
  public abstract ImmutableSet<String> passAnnotationsNames();

  @Value.Parameter
  public abstract ImmutableSet<String> additionalJsonAnnotationsNames();

  @Value.Parameter
  @Override
  public abstract ImplementationVisibility visibility();

  @Value.Parameter
  @Override
  public abstract boolean optionalAcceptNullable();

  @Value.Parameter
  @Override
  public abstract boolean generateSuppressAllWarnings();

  @Value.Parameter
  @Override
  public abstract boolean privateNoargConstructor();

  @Value.Parameter
  @Override
  public abstract boolean attributelessSingleton();

  @Value.Parameter
  @Override
  public abstract boolean unsafeDefaultAndDerived();

  @Value.Parameter
  @Override
  public abstract boolean clearBuilder();

  @Value.Parameter
  @Override
  public abstract boolean deferCollectionAllocation();

  @Override
  @Value.Parameter
  public abstract boolean deepImmutablesDetection();

  @Value.Parameter
  @Override
  public abstract boolean overshadowImplementation();

  @Value.Parameter
  @Override
  public abstract boolean implementationNestedInBuilder();

  @Value.Parameter
  @Override
  public abstract boolean forceJacksonPropertyNames();

  @Value.Parameter
  @Override
  public abstract boolean forceJacksonIgnoreFields();

  @Value.Parameter
  @Override
  public abstract boolean jacksonIntegration();

  @Value.Parameter
  @Override
  public abstract BuilderVisibility builderVisibility();

  @Override
  public Class<? extends Annotation>[] passAnnotations() {
    throw new UnsupportedOperationException("Use StyleInfo.passAnnotationsNames() instead");
  }

  @Override
  public Class<? extends Annotation>[] additionalJsonAnnotations() {
    throw new UnsupportedOperationException("Use StyleInfo.additionalJsonAnnotationsNames() instead");
  }

  @Override
  public Class<? extends Exception> throwForInvalidImmutableState() {
    throw new UnsupportedOperationException("Use StyleInfo.throwForInvalidImmutableStateName() instead");
  }

  @Override
  public Class<?>[] immutableCopyOfRoutines() {
    throw new UnsupportedOperationException("Use StyleInfo.immutableCopyOfRoutinesNames() instead");
  }

  @Value.Parameter
  public abstract String throwForInvalidImmutableStateName();

  @Value.Parameter
  @Override
  public abstract boolean depluralize();

  @Value.Parameter
  @Override
  public abstract String[] depluralizeDictionary();

  @Value.Parameter
  public abstract ImmutableSet<String> immutableCopyOfRoutinesNames();

  @Value.Parameter
  @Override
  public abstract boolean stagedBuilder();

  @Value.Parameter
  @Override
  public abstract boolean builtinContainerAttributes();

  @Value.Parameter
  @Override
  public abstract boolean beanFriendlyModifiables();

  @Value.Lazy
  public Styles getStyles() {
    return new Styles(this);
  }
}
