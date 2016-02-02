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
package org.immutables.gson.packg;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class PackgTest {
  final GsonAdaptersPackg adapterFactory = new GsonAdaptersPackg();
  final Gson gson = new Gson();

  @Test
  public void adaptersProvided() {
    check(adapterFactory.create(gson, TypeToken.get(A.class))).notNull();
    check(adapterFactory.create(gson, TypeToken.get(B.class))).notNull();
    check(adapterFactory.create(gson, TypeToken.get(B.C.class))).notNull();
    check(adapterFactory.create(gson, TypeToken.get(D.E.class))).notNull();
    check(adapterFactory.create(gson, TypeToken.get(D.F.class))).notNull();
    check(adapterFactory.create(gson, TypeToken.get(G.H.class))).notNull();
  }
}
