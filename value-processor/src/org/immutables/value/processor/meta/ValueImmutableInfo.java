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

import java.lang.annotation.Annotation;
import org.immutables.value.Value;

@SuppressWarnings("all")
@Value.Style(copyOf = "theCopyOf", of = "theOf", builder = "theBuilder")
@Value.Immutable(intern = true)
public abstract class ValueImmutableInfo implements ValueMirrors.Immutable {
  @Override
  public Class<? extends Annotation> annotationType() {
    return ValueMirrors.Immutable.class;
  }

  @Value.Default
  public boolean isDefault() {
    return false;
  }

  @Value.Parameter
  @Override
  public abstract boolean builder();

  @Value.Parameter
  @Override
  public abstract boolean copy();

  @Value.Parameter
  @Override
  public abstract boolean intern();

  @Value.Parameter
  @Override
  public abstract boolean prehash();

  @Value.Parameter
  @Override
  public abstract boolean singleton();

  static ImmutableValueImmutableInfo infoFrom(ImmutableMirror input) {
    return ImmutableValueImmutableInfo.theOf(
        input.builder(),
        input.copy(),
        input.intern(),
        input.prehash(),
        input.singleton())
        .withIsDefault(input.getAnnotationMirror().getElementValues().isEmpty());
  }
}
