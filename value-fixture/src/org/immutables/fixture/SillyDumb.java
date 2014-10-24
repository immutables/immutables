/*
    Copyright 2013-2014 Immutables.org authors

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

import com.google.common.base.Optional;
import java.util.List;
import org.immutables.value.Json;
import org.immutables.value.Value;

@Value.Immutable
@Json.Marshaled
public abstract class SillyDumb {

  @Json.Named("a")
  @Json.ForceEmpty
  public abstract Optional<Integer> a1();

  @Json.Named("b")
  @Json.ForceEmpty
  public abstract List<String> b2();

  @Json.Named("c")
  public abstract Optional<Integer> c3();

  @Json.Named("d")
  public abstract List<String> d4();
}
