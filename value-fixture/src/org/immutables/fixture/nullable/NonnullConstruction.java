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

import javax.annotation.Nullable;
import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
@JsonDeserialize(as = ImmutableNonnullConstruction.class)
@Value.Style(allParameters = true, defaultAsDefault = true)
public interface NonnullConstruction {
  String[] arr();

  @Nullable
  String[] brr();

  default List<String> ax() {
    return null;// will blowup unless set explicitly to nonnull
  }

  @Value.Derived
  default int a() {
    return 1;
  }
}
