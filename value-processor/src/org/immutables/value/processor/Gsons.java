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
package org.immutables.value.processor;

import org.immutables.value.processor.meta.Proto.DeclaringType;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.immutables.generator.Generator;
import org.immutables.value.Value;
import org.immutables.value.processor.meta.Proto.AbstractDeclaring;
import org.immutables.value.processor.meta.Proto.Protoclass;
import org.immutables.value.processor.meta.ValueAttribute;
import org.immutables.value.processor.meta.ValueType;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

@Generator.Template
abstract class Gsons extends ValuesTemplate {

  @Value.Immutable
  public interface TypeAdapterTypes {
    AbstractDeclaring definedBy();

    List<ValueType> types();
  }

  public Iterable<TypeAdapterTypes> typeAdapters() {
    Multimap<AbstractDeclaring, ValueType> byDeclaring = HashMultimap.create();
    for (ValueType value : values().values()) {
      Protoclass protoclass = value.constitution.protoclass();
      if (protoclass.kind().isValue()) {
        Optional<AbstractDeclaring> typeAdaptersProvider = protoclass.typeAdaptersProvider();
        if (typeAdaptersProvider.isPresent()) {
          byDeclaring.put(typeAdaptersProvider.get(), value);
        } else if (protoclass.gsonTypeAdapters().isPresent()
            && protoclass.declaringType().isPresent()) {
          DeclaringType topLevel = protoclass.declaringType().get().topLevel();
          byDeclaring.put(topLevel, value);
        }
      }
    }

    ImmutableList.Builder<TypeAdapterTypes> builder = ImmutableList.builder();
    for (Entry<AbstractDeclaring, Collection<ValueType>> entry : byDeclaring.asMap().entrySet()) {
      builder.add(ImmutableTypeAdapterTypes.builder()
          .definedBy(entry.getKey())
          .addAllTypes(entry.getValue())
          .build());
    }

    return builder.build();
  }

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
