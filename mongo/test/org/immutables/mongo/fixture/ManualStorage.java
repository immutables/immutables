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

    ItemRepository.Criteria where = items.where();

    FluentFuture<Integer> inserted = items.insert(ImmutableItem.builder()
        .id("1")
        .binary(Binary.create(new byte[] {1, 2, 3}))
        .build());

    inserted.getUnchecked();

    List<Item> all = items.findAll()
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
