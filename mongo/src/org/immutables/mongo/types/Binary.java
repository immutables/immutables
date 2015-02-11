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
package org.immutables.mongo.types;

import java.util.Arrays;
import javax.annotation.concurrent.Immutable;

/**
 * Holds byte array and have specific type for binding.
 */
@Immutable
public final class Binary {
  private final byte[] data;

  private Binary(byte[] data) {
    this.data = data;
  }

  public static Binary create(byte[] data) {
    return new Binary(data.clone());
  }

  public byte[] value() {
    return data.clone();
  }

  public int length() {
    return data.length;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(data);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof Id) {
      Binary binary = (Binary) other;
      return Arrays.equals(data, binary.data);
    }
    return false;
  }

  @Override
  public String toString() {
    return "Binary(" + data.length + " bytes)";
  }
}
