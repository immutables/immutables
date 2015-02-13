package org.immutables.gson.adapter;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class AdaptReadWriteTest {

  private final Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(new GsonAdaptersAdapt())
      .create();

  private final Adapt adapt =
      ImmutableAdapt.of(
          ImmutableList.of(
              ImmutableAdapt.Inr.builder()
                  .arr("a", "b", "c")
                  .addList(1, 2, 4)
                  .putMap("key",
                      ImmutableAdapt.Nst.builder()
                          .string("a")
                          .value(1)
                          .build())
                  .putMap("other",
                      ImmutableAdapt.Nst.builder()
                          .string("b")
                          .value(2)
                          .build())
                  .putSetMultimap(1,
                      ImmutableAdapt.Nst.builder()
                          .string("u")
                          .value(22)
                          .build())
                  .putSetMultimap(1,
                      ImmutableAdapt.Nst.builder()
                          .string("i")
                          .value(22)
                          .build())
                  .build(),
              ImmutableAdapt.Inr.builder()
                  .arr("x", "y", "z")
                  .addList(5, 6)
                  .putMap("ku",
                      ImmutableAdapt.Nst.builder()
                          .string("x")
                          .value(11)
                          .build())
                  .putMap("la",
                      ImmutableAdapt.Nst.builder()
                          .string("y")
                          .value(21)
                          .build())
                  .putListMultimap("f",
                      ImmutableAdapt.Nst.builder()
                          .string("y")
                          .value(21)
                          .build())
                  .putListMultimap("f",
                      ImmutableAdapt.Nst.builder()
                          .string("y")
                          .value(21)
                          .build())
                  .build()),
          ImmutableList.of(
              ImmutableAdapt.Nst.builder()
                  .string("x")
                  .value(11)
                  .build(),
              ImmutableAdapt.Nst.builder()
                  .string("x")
                  .value(11)
                  .build()));

  @Test
  public void adapt() {
    String json = gson.toJson(adapt);
    Adapt instance = gson.fromJson(json, Adapt.class);
    check(instance).is(adapt);
  }
}
