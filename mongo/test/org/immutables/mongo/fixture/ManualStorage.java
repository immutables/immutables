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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import java.util.ServiceLoader;
import org.immutables.mongo.concurrent.FluentFuture;
import org.immutables.mongo.repository.RepositorySetup;
import org.immutables.mongo.types.Binary;

public class ManualStorage {
  private static final Type LIST_ITEMS = new TypeToken<List<Item>>() {}.getType();

  public static void main(String... args) {

    RepositorySetup setup = RepositorySetup.forUri("mongodb://localhost/test");

    ItemRepository items = new ItemRepository(setup);

    items.findAll().deleteAll().getUnchecked();

    ItemRepository.Criteria where = items.criteria();
    FluentFuture<Integer> inserted = items.insert(ImmutableItem.builder()
        .id("1")
        .binary(Binary.create(new byte[] {1, 2, 3}))
        .build());

    inserted.getUnchecked();

    List<Item> all = items.find(where)
        .fetchAll()
        .getUnchecked();

    String json = createGson().toJson(all, LIST_ITEMS);

    System.out.println(json);
  }

  private static Gson createGson() {
    GsonBuilder gsonBuilder = new GsonBuilder();
    for (TypeAdapterFactory factory : ServiceLoader.load(TypeAdapterFactory.class)) {
      gsonBuilder.registerTypeAdapterFactory(factory);
    }
    return gsonBuilder.create();
  }
}
