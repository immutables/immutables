/*
   Copyright 2016 Immutables Authors and Contributors

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

import java.util.List;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

@Value.Immutable
class SuperInnerBuildeValue {
  static class Builder {

    void hello() {}
  }

  @SuppressWarnings("CheckReturnValue")
  static void use() {
    ImmutableSuperInnerBuildeValue.builder().hello();
  }
}

@Value.Immutable
@Value.Style(
    typeBuilder = "*_Builder",
    visibility = ImplementationVisibility.PRIVATE)
abstract class ExtendingInnerBuilderValue {

  abstract int attribute();

  abstract List<String> list();

  static class Builder extends ExtendingInnerBuilderValue_Builder {
    Builder() {
      attribute(1);
    }
  }
}

@SuppressWarnings("deprecation")
@Value.Immutable
@Value.Style(
    typeInnerBuilder = "Creator",
    typeBuilder = "Creator",
    build = "create",
    visibility = ImplementationVisibility.SAME_NON_RETURNED)
interface ExtendingInnerCreatorValue {

  static class Creator extends ImmutableExtendingInnerCreatorValue.Creator {
    @Override
    public ExtendingInnerCreatorValue create() {
      return super.create();
    }
  }

  @SuppressWarnings("CheckReturnValue")
  default void use() {
    ImmutableExtendingInnerCreatorValue.Creator c = new ImmutableExtendingInnerCreatorValue.Creator();
    c.create();
  }
}
