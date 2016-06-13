/*
   Copyright 2014 Immutables Authors and Contributors

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
package org.immutables.value.processor;

import com.google.common.collect.Multimap;
import org.immutables.generator.AbstractTemplate;
import org.immutables.generator.Generator;
import org.immutables.generator.Templates;
import org.immutables.value.processor.meta.LongBits;
import org.immutables.value.processor.meta.Proto.DeclaringPackage;
import org.immutables.value.processor.meta.UnshadeGuava;
import org.immutables.value.processor.meta.ValueAttribute;
import org.immutables.value.processor.meta.ValueType;

abstract class ValuesTemplate extends AbstractTemplate {
  @Generator.Typedef
  ValueType Type;

  @Generator.Typedef
  ValueAttribute Attribute;

  @Generator.Typedef
  LongBits.LongPositions LongPositions;

  @Generator.Typedef
  LongBits.BitPosition BitPosition;

  @Generator.Typedef
  DeclaringPackage Package;

  public abstract Templates.Invokable generate();

  public final String guava = UnshadeGuava.prefix();
  
  public final LongBits longsFor = new LongBits();

  private Multimap<DeclaringPackage, ValueType> values;

  ValuesTemplate usingValues(Multimap<DeclaringPackage, ValueType> values) {
    this.values = values;
    return this;
  }

  public Multimap<DeclaringPackage, ValueType> values() {
    return values;
  }

  Flag flag = new Flag();

  static class Flag {
    boolean is;

    String set() {
      this.is = true;
      return "";
    }

    String clear() {
      this.is = false;
      return "";
    }
  }
}
