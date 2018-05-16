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

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import org.immutables.generator.ExtensionLoader;

public final class CustomImmutableAnnotations {
  private CustomImmutableAnnotations() {}

  private static final Supplier<ImmutableSet<String>> ANNOTATIONS =
      ExtensionLoader.findExtensions("META-INF/annotations/org.immutables.value.immutable");

  public static ImmutableSet<String> annotations() {
    return ANNOTATIONS.get();
  }
}
