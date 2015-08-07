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

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class FieldNameTranslaterTest {
  final Gson gson = new GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
      .create();

  final FieldNamingTranslator translator = new FieldNamingTranslator(gson);

  @Test
  public void applySerializedName() {
    check(translator.translateName(Object.class, Object.class, "toString", "")).is("to-string");
  }

  @Test
  public void overridenSerializedName() {
    check(translator.translateName(Object.class, Object.class, "toString", "TO_STRING")).is("TO_STRING");
  }

  @Test
  public void noConfig() {
    check(new FieldNamingTranslator(new Gson()).translateName(Object.class, Object.class, "toString", ""))
        .is("toString");
  }
}
