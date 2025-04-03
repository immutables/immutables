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

import com.google.common.base.MoreObjects;
import javax.annotation.Nullable;

public final class UnshadeJackson {
  // not sure if it's needed here, but also hiding default prefix from accidental shading (via .concat)
  // i.e. shading tools can mangle string constants which have fully qualified name prefix
  // of the package which is going to be relocated. We use this for Guava, as we use it
  // internally and shade it. We don't shade Jackson, but if someone builds a fork of Immutables,
  // this might be a possibility.
  private static final String JACKSON_PREFIX =
      System.getProperty("jackson.prefix", "com.fast".concat("erxml.jackson"));

  private static volatile @Nullable String prefixOverride;

  public static void overridePrefix(@Nullable String prefix) {
    prefixOverride = prefix;
  }

  public static String qualify(String partiallyQualifiedType) {
    return prefix() + '.' + partiallyQualifiedType;
  }

  public static String prefix() {
    return MoreObjects.firstNonNull(prefixOverride, JACKSON_PREFIX);
  }
}
