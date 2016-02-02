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
package org.immutables.moshi;

import com.atlassian.fugue.Option;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.SetMultimap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.immutables.value.Value;

@Value.Immutable(builder = false)
@Value.Enclosing
@Json.Adapters
public interface Adapt {

  @Value.Parameter
  @Json.Named("setische")
  Set<Inr> set();

  @Value.Parameter
  Multiset<Nst> bag();

  @Value.Parameter
  Option<String> optionFugue2();

  @Value.Parameter
  io.atlassian.fugue.Option<String> optionFugue3();

  @Value.Immutable
  public interface Inr {
    String[] arr();

    List<Integer> list();

    List<List<Integer>> listList();

    Map<String, Nst> map();

    ListMultimap<String, Nst> listMultimap();

    @Json.Ignore
    SetMultimap<Integer, Nst> setMultimap();
  }

  @Value.Immutable
  public interface Nst {
    @Json.Named("i")
    int value();

    @Json.Named("s")
    String string();
  }
}
