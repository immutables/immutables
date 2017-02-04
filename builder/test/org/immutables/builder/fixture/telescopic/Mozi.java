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
package org.immutables.builder.fixture.telescopic;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Style(stagedBuilder = true)
@Value.Immutable
public interface Mozi<K, V> {
  K a();

  V b();

  List<Long> c();

  Optional<String> d();

  Multimap<K, V> e();

  @Nullable
  String f();
}
