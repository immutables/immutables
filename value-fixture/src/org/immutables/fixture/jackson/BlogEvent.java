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
package org.immutables.fixture.jackson;

import org.immutables.value.Value.Style.ImplementationVisibility;
import org.immutables.value.Value;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

interface Jsonable {}

@JsonSerialize // Jackson automatic integration, why not?
@Value.Style(
    typeAbstract = "Abstract*",
    typeImmutable = "*",
    visibility = ImplementationVisibility.PUBLIC)
@interface MyStyle {}

@MyStyle //<-- Meta annotated with @JsonSerialize and @Value.Style
// and applies to nested immutable objects
interface BlogEvent extends Jsonable {

  @Value.Immutable
  interface AbstractPostAdded extends BlogEvent {
    String getPostId();
    BodyChanged getContent();
  }

  @Value.Immutable
  interface AbstractBodyChanged extends BlogEvent {
    @Value.Parameter
    String getBody();
  }

  @Value.Immutable
  interface AbstractPostPublished extends BlogEvent {
    @Value.Parameter
    String getPostId();
  }
}
