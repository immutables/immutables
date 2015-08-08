/*
    Copyright 2013-2014 Immutables Authors and Contributors

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
package org.immutables.fixture;

import com.google.common.primitives.UnsignedInteger;
import java.util.List;
import java.util.Map;
import org.immutables.gson.Gson;
import org.immutables.mongo.Mongo;
import org.immutables.value.Value;

@SuppressWarnings("immutables")
@Value.Immutable
@Mongo.Repository
public abstract class SillyEntity {

  @Mongo.Id
  public abstract int id();

  @Gson.Named("v")
  public abstract String val();

  @Gson.Named("p")
  public abstract Map<String, Integer> payload();

  @Gson.Named("i")
  public abstract List<Integer> ints();

  @Value.Derived
  public UnsignedInteger der() {
    return UnsignedInteger.valueOf(1);
  }
}
