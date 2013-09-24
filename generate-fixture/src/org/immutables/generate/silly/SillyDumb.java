/*
    Copyright 2013 Immutables.org authors

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
package org.immutables.generate.silly;

import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateMarshaledAs;
import org.immutables.annotation.GenerateMarshaler;
import com.google.common.base.Optional;
import java.util.List;

@GenerateImmutable
@GenerateMarshaler
public abstract class SillyDumb {

  @GenerateMarshaledAs(value = "a", forceEmpty = true)
  public abstract Optional<Integer> a1();

  @GenerateMarshaledAs(value = "b", forceEmpty = true)
  public abstract List<String> b2();

  @GenerateMarshaledAs(value = "c", forceEmpty = false)
  public abstract Optional<Integer> c3();

  @GenerateMarshaledAs(value = "d", forceEmpty = false)
  public abstract List<String> d4();
}
