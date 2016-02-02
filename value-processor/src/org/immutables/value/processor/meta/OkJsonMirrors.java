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
package org.immutables.value.processor.meta;

import org.immutables.mirror.Mirror;

public final class OkJsonMirrors {
  private OkJsonMirrors() {}

  @Mirror.Annotation("org.immutables.moshi.Json.Adapters")
  public @interface OkTypeAdapters {
    boolean emptyAsNulls() default false;
  }

  @Mirror.Annotation("org.immutables.moshi.Json.Named")
  public @interface OkNamed {
    String value();
  }

  @Mirror.Annotation("org.immutables.moshi.Json.Ignore")
  public @interface OkIgnore {}
}
