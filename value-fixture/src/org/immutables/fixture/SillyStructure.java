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

import com.google.common.base.Optional;
import java.util.List;
import org.immutables.fixture.subpack.SillySubstructure;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Value.Immutable
@Gson.TypeAdapters
public abstract class SillyStructure {

  public abstract String attr1();

  public abstract boolean flag2();

  public abstract Optional<Integer> opt3();

  public abstract long very4();

  public abstract double wet5();

  public abstract List<SillySubstructure> subs6();

  public abstract SillySubstructure nest7();

  public abstract Optional<SillyTuplie> tup3();

  public abstract int int9();
}
