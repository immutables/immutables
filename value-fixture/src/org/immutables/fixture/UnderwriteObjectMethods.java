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
package org.immutables.fixture;

import org.immutables.value.Value;

public class UnderwriteObjectMethods {
  @Value.Immutable
  public abstract static class Underwrite {
    public abstract int value();

    @Override
    public int hashCode() {
      return value() + 1;
    }

    @Override
    public boolean equals(Object obj) {
      return "U".equals(obj.toString());
    }

    @Override
    public String toString() {
      return "U";
    }
  }

  public abstract static class AbstractNoUnderwrite {
    public abstract int value();

    @Override
    public int hashCode() {
      return value() + 1;
    }

    @Override
    public boolean equals(Object obj) {
      return "N".equals(obj.toString());
    }

    @Override
    public String toString() {
      return "N";
    }
  }

  @Value.Immutable
  public abstract static class NoUnderwrite extends AbstractNoUnderwrite {
    @Override
    public abstract int value();

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    @Override
    public abstract boolean equals(Object obj);
  }
}
