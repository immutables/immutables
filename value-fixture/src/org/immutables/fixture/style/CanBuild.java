/*
   Copyright 2018 Immutables Authors and Contributors

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
package org.immutables.fixture.style;

import java.util.Optional;
import org.immutables.value.Value;

// checks generation and functionality of canBuild method on builder
@Value.Immutable
@Value.Style(canBuild = "isBuilder*")
public interface CanBuild {
  int a();

  Optional<String> b();

  class Builder extends ImmutableCanBuild.Builder {}
}
