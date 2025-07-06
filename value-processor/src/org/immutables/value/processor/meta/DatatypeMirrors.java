/*
   Copyright 2020 Immutables Authors and Contributors

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

public final class DatatypeMirrors {
  private DatatypeMirrors() {}

  @Mirror.Annotation("org.immutables.data.Data")
  public @interface Data {}
  
  @Mirror.Annotation("org.immutables.data.Data.Ignore")
  public @interface DataIgnore {}
  
  @Mirror.Annotation("org.immutables.data.Data.Inline")
  public @interface DataInline {}

  @Mirror.Annotation("org.immutables.datatype.Data")
  public @interface DData {}

  @Mirror.Annotation("org.immutables.datatype.Data.Ignore")
  public @interface DDataIgnore {}

  @Mirror.Annotation("org.immutables.datatype.Data.Inline")
  public @interface DDataInline {}
}
