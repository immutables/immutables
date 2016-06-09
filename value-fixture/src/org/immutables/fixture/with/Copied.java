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
package org.immutables.fixture.with;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

@Value.Immutable
@Value.Style(visibility = ImplementationVisibility.PACKAGE)
public abstract class Copied implements WithCopied {

  public abstract int attr();

  public abstract @Nullable Void voids();

  public abstract String[] arr();

  public abstract Map<String, Integer> map();

  public abstract List<Boolean> list();

  public static class Builder extends ImmutableCopied.Builder {}

  static void use() {
    new Copied.Builder()
        .attr(1)
        .build()
        .withList(true, false);
  }
}
