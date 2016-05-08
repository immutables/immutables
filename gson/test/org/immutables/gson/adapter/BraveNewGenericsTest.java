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
import java.util.Arrays;
import java.util.List;
import org.immutables.gson.adapter.BraveNewGenerics.Top;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class BraveNewGenericsTest {

  final Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(new GsonAdaptersBraveNewGenerics())
      .create();

  @Test
  public void roundtrip() {
    Top t1 = BraveNewGenerics.createTop();
    String json = gson.toJson(t1);
    Top t2 = gson.fromJson(json, Top.class);
    check(t2).is(t1);
  }
}
