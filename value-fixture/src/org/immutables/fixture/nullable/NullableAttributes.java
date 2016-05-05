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
package org.immutables.fixture.nullable;

import java.util.Collections;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Set;
import org.immutables.gson.Gson;
import java.util.Map;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;


@Gson.TypeAdapters
@Value.Immutable
@JsonDeserialize(as = ImmutableNullableAttributes.class)
@Value.Style(defaultAsDefault = true)
public interface NullableAttributes {
  @Nullable
  Integer integer();

  @Nullable
  List<String> list();

  @Nullable
  default Set<Integer> set() {
    return Collections.emptySet();
  }

  @Nullable
  Integer[] array();

  @Nullable
  default Double[] defArray() {
    return new Double[] {Double.NaN};
  }

  @Nullable
  Map<String, Object> map();
}
