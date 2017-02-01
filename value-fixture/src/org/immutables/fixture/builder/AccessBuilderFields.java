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
package org.immutables.fixture.builder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.immutables.builder.Builder.AccessibleFields;
import org.immutables.value.Value.Immutable;

@Immutable
abstract class AccessBuilderFields {
  abstract int a();
  abstract String b();
  abstract EnumSet<T> c();
  abstract EnumMap<T, String> d();
  abstract List<String> e();
  abstract Map<String, String> f();
  abstract Optional<String> g();
  @AccessibleFields
  static class Builder extends ImmutableAccessBuilderFields.Builder {
    public final Builder accessFields() {
      int a1 = a;
      String b1 = b;
      EnumSet<T> c1 = c;
      EnumMap<T, String> d1 = d;
      ImmutableList.Builder<String> e1 = e;
      ImmutableMap.Builder<String, String> f1 = f;
      String g1 = g;
      return this;
    }
  }
  enum T { U,V }
}
