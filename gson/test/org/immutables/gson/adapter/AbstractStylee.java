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

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multiset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.immutables.gson.Gson;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

/**
 * Compilation test on customized by style value
 */
@Value.Immutable(builder = false)
@Value.Enclosing
@Gson.TypeAdapters
@Value.Style(
    typeAbstract = "I*",
    typeImmutable = "*",
    init = "set*",
    add = "push*",
    addAll = "pushAll*",
    put = "push*",
    putAll = "pushAll*",
    builder = "new",
    build = "create",
    jdkOnly = true,
    visibility = ImplementationVisibility.PUBLIC)
interface IStylee {

  @Value.Parameter
  Set<Stylee.Inr> getSet();

  @Value.Parameter
  Multiset<org.immutables.gson.adapter.Stylee.Nst> getBag();

  @Value.Immutable
  interface IInr {
    String[] getArr();

    List<Integer> getList();

    Map<String, Stylee.Nst> getMap();

    ListMultimap<String, org.immutables.gson.adapter.Stylee.Nst> getListMultimap();
  }

  @Value.Immutable
  interface INst {
    int getValue();

    String getString();
  }
}
