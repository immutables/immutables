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
package org.immutables.mongo.fixture;

import com.google.common.base.Optional;
import java.util.List;
import java.util.Set;
import org.immutables.gson.Gson;
import org.immutables.mongo.Mongo;
import org.immutables.mongo.types.Binary;
import org.immutables.mongo.types.Id;
import org.immutables.value.Value;

@Mongo.Repository
@Value.Immutable
@Gson.TypeAdapters
public interface Item {

  @Mongo.Id
  String id();

  List<String> list();

  List<Tag> tags();

  Set<Id> ids();

  Optional<Binary> binary();

  @Value.Immutable
  @Gson.TypeAdapters
  interface Tag {
    @Value.Parameter
    String name();
  }
}
