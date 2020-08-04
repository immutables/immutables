/*
   Copyright 2017 Immutables Authors and Contributors

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
package org.immutables.mongo.fixture.generic;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.gson.Gson;
import org.immutables.mongo.Mongo;
import org.immutables.value.Value;

import java.util.List;

@Gson.TypeAdapters
@Mongo.Repository("genericHolder")
@Value.Immutable
@JsonSerialize(as = ImmutableGenericHolder.class)
@JsonDeserialize(as = ImmutableGenericHolder.class)
public interface GenericHolder {

  @Mongo.Id
  String id();

  GenericType<?> value();

  List<GenericType<?>> listOfValues();
}
