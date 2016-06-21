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
package org.immutables.gson.adapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import org.immutables.gson.adapter.BraveNewGenerics.Top;
import org.immutables.gson.adapter.CustomGenerics.Botts;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class BraveNewGenericsTest {

  final Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(new GsonAdaptersBraveNewGenerics())
      .registerTypeAdapterFactory(new GsonAdaptersSchool())
      .registerTypeAdapterFactory(new GsonAdaptersCustomGenerics())
      .create();

  @Test
  public void roundtrip() {
    Top t1 = BraveNewGenerics.createTop();
    String json = gson.toJson(t1);
    Top t2 = gson.fromJson(json, Top.class);
    check(t2).is(t1);
  }

  @Test
  public void schoolRoundtrip() {
    School school = School.create();
    String json = gson.toJson(school);
    School school2 = gson.fromJson(json, School.class);
    check(school).is(school2);
  }

  @Test
  public void customGenerics() {
    Type t = new TypeToken<Botts<Integer>>() {}.getType();
    Botts<Integer> t1 = CustomGenerics.createBotts();
    String json = gson.toJson(t1, t);
    Botts<Integer> t2 = gson.fromJson(json, t);
    check(t2).is(t1);
  }
}
