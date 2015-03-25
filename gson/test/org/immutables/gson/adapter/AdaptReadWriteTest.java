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
package org.immutables.gson.adapter;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class AdaptReadWriteTest {

  private final Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(new GsonAdaptersAdapt())
      .registerTypeAdapterFactory(new GsonAdaptersIStylee())
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

  @Test
  public void stylee() {
    String json = gson.toJson(adapt);
    Stylee instance = gson.fromJson(json, Stylee.class);
    check(gson.fromJson(gson.toJson(instance), Stylee.class)).is(instance);
  }
}
