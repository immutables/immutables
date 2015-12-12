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
package org.immutables.gson.adapter;

import com.google.common.base.Optional;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters(emptyAsNulls = true, fieldNamingStrategy = true)
@Value.Immutable
public interface Simple {
  Optional<String> optionalString();

  @SuppressWarnings("deprecation")
  @Gson.Named("_nullable_")
  @Nullable
  Integer nlb();

  List<Character> characterList();
}
