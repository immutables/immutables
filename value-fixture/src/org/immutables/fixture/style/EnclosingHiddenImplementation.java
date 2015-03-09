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
package org.immutables.fixture.style;

import com.google.common.base.Optional;
import org.immutables.value.Value.Style.ImplementationVisibility;
import org.immutables.value.Value;

@Value.Style(
    typeImmutableEnclosing = "EnclosingFactory",
    instance = "singletonInstance",
    of = "new*",
    newBuilder = "create",
    visibility = ImplementationVisibility.PRIVATE,
    defaults = @Value.Immutable(singleton = true))
@interface Priv {}

/**
 * Feature combination
 * <ul>
 * <li>Nested hidded implementation
 * <li>Defaults mechanism, implementation style override
 * <li>Style meta-annotation usage to create style
 * <li>Forwarding factory methods with names customization and auto-disambiguation
 * </ul>
 */
@Value.Enclosing
@Priv
public abstract class EnclosingHiddenImplementation {
  @Value.Immutable
  public static class HiddenImplementation {}

  @Value.Immutable
  public static abstract class NonexposedImplementation {
    @Value.Parameter
    public abstract Optional<Integer> cons();
  }

  @Value.Immutable(builder = false)
  public static abstract class VisibleImplementation {
    @Value.Parameter
    public abstract Optional<Integer> cons();
  }

  void use() {
    EnclosingFactory.HiddenImplementationBuilder.create().build();

    // Type name suffix was added to avoid name clashing
    EnclosingFactory.singletonInstanceHiddenImplementation();
    EnclosingFactory.singletonInstanceNonexposedImplementation();
    // Strictly follows naming template
    EnclosingFactory.newNonexposedImplementation(Optional.of(11));
    // Implementation is visible
    EnclosingFactory.newVisibleImplementation(Optional.<Integer>absent());
  }
}
