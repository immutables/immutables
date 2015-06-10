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
import java.lang.annotation.Annotation;
import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;
import org.immutables.value.processor.meta.ValueMirrors.Enclosing;

@SuppressWarnings("all")
@Value.Immutable(intern = true, copy = false, builder = false)
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
  public abstract String typeBuilder();

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

  @Override
  @Value.Parameter
  public abstract ValueImmutableInfo defaults();

  @Value.Parameter
  @Override
  public abstract boolean strictBuilder();

  @Value.Parameter
  @Override
  public abstract boolean allParameters();

  @Value.Parameter
  @Override
  public abstract boolean jdkOnly();

  @Value.Parameter
  public abstract ImmutableSet<String> passAnnotationsNames();
  
  @Value.Parameter
  @Override
  public abstract ImplementationVisibility visibility();
  
  @Override
  public Class<? extends Annotation>[] passAnnotations() {
    throw new UnsupportedOperationException("Use StyleInfo.passAnnotationsNames() instead");
  }

  @Value.Lazy
  public Styles getStyles() {
    return new Styles(this);
  }
}
