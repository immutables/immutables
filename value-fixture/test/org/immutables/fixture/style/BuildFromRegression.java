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
package org.immutables.fixture.style;

import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(visibility = Value.Style.ImplementationVisibility.PUBLIC)
abstract class BuildFromRegression {
  public abstract List<String> values();

  public ImmutableBuildFromRegression replace(ImmutableBuildFromRegression that) {
    return ImmutableBuildFromRegression.copyOf(this)
        .withValues(that.values());
  }

  public ImmutableBuildFromRegression append(ImmutableBuildFromRegression that) {
    return ImmutableBuildFromRegression.builder()
        .from(this)
        .addAllValues(that.values())
        .build();
  }
}

interface Supertype2 {
  int a();
}

@Value.Immutable
@Value.Style(visibility = Value.Style.ImplementationVisibility.PUBLIC)
abstract class BuildFromRegression2 implements Supertype2 {
  public abstract List<String> values();

  public ImmutableBuildFromRegression2 append(ImmutableBuildFromRegression2 that) {
    return ImmutableBuildFromRegression2.builder()
        .from(this)
        .addAllValues(that.values())
        .build();
  }

  public ImmutableBuildFromRegression2 append(Supertype2 that) {
    return ImmutableBuildFromRegression2.builder()
        .from(this)
        .from(that)
        .build();
  }
}
