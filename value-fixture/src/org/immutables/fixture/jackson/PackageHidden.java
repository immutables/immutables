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

import java.util.Optional;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(
    jdkOnly = true,
    implementationNestedInBuilder = true,
    visibility = Value.Style.ImplementationVisibility.PACKAGE,
    builderVisibility = Value.Style.BuilderVisibility.PUBLIC)
@JsonDeserialize(as = PackageHiddenBuilder.ImmutablePackageHidden.class)
public interface PackageHidden {
  List<String> getStrings();
}

@Value.Immutable(builder = false)
@Value.Style(
    jdkOnly = true,
    overshadowImplementation = true,
    implementationNestedInBuilder = true,
    visibility = Value.Style.ImplementationVisibility.PACKAGE)
@JsonDeserialize(as = ImmutablePackageNoBuilderHidden.class)
interface PackageNoBuilderHidden {
  List<String> getStrings();

  Optional<Integer> getInt();
}
