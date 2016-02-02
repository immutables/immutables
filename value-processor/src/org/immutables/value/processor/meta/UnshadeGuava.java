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

public final class UnshadeGuava {
  private static final String GUAVA_PREFIX =
      System.getProperty("guava.prefix", "com.go".concat("ogle.common"));

  public static String typeString(String partiallyQualifiedType) {
    return GUAVA_PREFIX + '.' + partiallyQualifiedType;
  }

  public static String prefix() {
    return GUAVA_PREFIX;
  }
}
