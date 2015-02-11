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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.immutables.generator.Generator;
import org.immutables.value.processor.meta.ValueAttribute;

//@Generator.Template
abstract class Marshalers extends ValuesTemplate {
  @Generator.Typedef
  Multimap<Character, ValueAttribute> Mm;
  public final Function<Iterable<ValueAttribute>, Multimap<Character, ValueAttribute>> byFirstCharacter =
      new Function<Iterable<ValueAttribute>, Multimap<Character, ValueAttribute>>() {
        @Override
        public Multimap<Character, ValueAttribute> apply(Iterable<ValueAttribute> attributes) {
          ImmutableMultimap.Builder<Character, ValueAttribute> builder = ImmutableMultimap.builder();

          for (ValueAttribute attribute : attributes) {
            String name = attribute.getMarshaledName();
            char firstChar = name.charAt(0);

            builder.put(firstChar, attribute);
          }

          return builder.build();
        }
      };
}
