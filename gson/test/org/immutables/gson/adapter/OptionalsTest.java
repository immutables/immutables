/*
<<<<<<< HEAD
   Copyright 2016 Immutables Authors and Contributors
=======
   Copyright 2018 Immutables Authors and Contributors
>>>>>>> #824 refinements over PR

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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;
<<<<<<< HEAD

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.immutables.check.Checkers.check;
import static org.junit.Assert.assertEquals;
=======
import static org.immutables.check.Checkers.check;
>>>>>>> #824 refinements over PR

public class OptionalsTest {

  final Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(new GsonAdaptersOptionals())
      .create();

  Optionals.Host createEmptyHost() {
    return ImmutableOptionals.Host.builder().build();
  }

  Optionals.Host createHost() {
<<<<<<< HEAD
    List<Optionals.A> optList = Arrays.asList((Optionals.A)ImmutableOptionals.D.of(false));
    Map<String, Optionals.A> optMap = new TreeMap<>();
    optMap.put("value", ImmutableOptionals.D.of(false));
=======
>>>>>>> #824 refinements over PR
    return ImmutableOptionals.Host.builder()
            .optInt(3)
            .optDouble(3.14D)
            .optWrappedFloat(3.14F)
            .optWrappedInteger(3)
            .optWrappedLong(33L)
            .optWrappedDouble(3.14D)
            .aPoly(ImmutableOptionals.B.of(4))
            .aFrom(ImmutableOptionals.D.of(false))
<<<<<<< HEAD
            .mappedOptional(optMap)
            .listOptional(optList)
=======
>>>>>>> #824 refinements over PR
            .build();
  }

  protected Optionals.Host roundtrip(Optionals.Host obj) {
    String json = gson.toJson(obj);
    return gson.fromJson(json, Optionals.Host.class);
  }

  @Test
  public void emptyHost() {
    Optionals.Host h1 = createEmptyHost();
    Optionals.Host h2 = roundtrip(h1);
    check(h2).is(h1);
  }

  @Test
  public void populatedHost() {
    Optionals.Host h1 = createHost();
    Optionals.Host h2 = roundtrip(h1);
    check(h2).is(h1);
  }

  @Test
  public void fromJsonHost() {
<<<<<<< HEAD
    String json = "{" +
            "\"optInt\":3," +
            "\"optDouble\":3.14," +
            "\"optWrappedFloat\":3.14," +
            "\"optWrappedInteger\":3," +
            "\"optWrappedLong\":3," +
            "\"optWrappedDouble\":3.14," +
            "\"mappedOptional\":{}," +
            "\"listOptional\":[]" +
            "}";
=======
    String json = "{"
        + "\"optInt\":3,"
        + "\"optDouble\":3.14,"
        + "\"optWrappedFloat\":3.14,"
        + "\"optWrappedInteger\":3,"
        + "\"optWrappedLong\":3,"
        + "\"optWrappedDouble\":3.14"
        + "}";
>>>>>>> #824 refinements over PR
    Optionals.Host h1 = gson.fromJson(json, Optionals.Host.class);
    Optionals.Host h2 = roundtrip(h1);
    String finalJson = gson.toJson(h2);
    check(h2).is(h1);
<<<<<<< HEAD
    assertEquals(json, finalJson);
=======
    check(json).is(finalJson);
>>>>>>> #824 refinements over PR
  }

}
