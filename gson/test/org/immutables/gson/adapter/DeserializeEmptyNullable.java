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
package org.immutables.gson.adapter;

import java.util.Set;
import com.google.common.collect.ListMultimap;
import java.util.Map;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

// Deserialization test for when nullable collections
// are empty, null or absent in JSON
@Value.Immutable
@Gson.TypeAdapters
public interface DeserializeEmptyNullable {
  @Nullable
  List<String> tags();

  @Nullable
  Map<String, Integer> mp();

  @Nullable
  ListMultimap<Integer, Integer> mm();

  @Nullable
  Set<Double> st();
}
