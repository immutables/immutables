/*
    Copyright 2013-2014 Immutables Authors and Contributors

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
import java.util.Set;
import org.immutables.value.ext.Json;
import org.immutables.value.Value;

@Value.Immutable(builder = false)
@Json.Marshaled
public abstract class SillyTuplie {

  @Value.Parameter(order = 0)
  public abstract float float1();

  @Value.Parameter(order = 1)
  public abstract Optional<Byte> opt2();

  @Value.Parameter(order = 3)
  public abstract Set<Boolean> set3();

}
