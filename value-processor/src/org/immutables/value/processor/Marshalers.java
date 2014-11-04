/*
    Copyright 2014 Ievgen Lukash

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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimaps;
import org.immutables.generator.Generator;
import org.immutables.value.processor.meta.ValueType;
import java.util.Collection;
import java.util.Map;

@Generator.Template
abstract class Marshalers extends ValuesTemplate {

  int roundCode() {
    return System.identityHashCode(round());
  }

  final Function<Iterable<ValueType>, Iterable<ValueType>> onlyMarshaled =
      new Function<Iterable<ValueType>, Iterable<ValueType>>() {
        @Override
        public Iterable<ValueType> apply(Iterable<ValueType> input) {
          ImmutableList.Builder<ValueType> builder = ImmutableList.builder();
          for (ValueType value : input) {
            if (value.isGenerateMarshaled()) {
              builder.add(value);
            }
          }
          return builder.build();
        }
      };

  final ByPackageGrouper byPackage = new ByPackageGrouper();

  class ByPackageGrouper
      implements Function<Iterable<ValueType>, Iterable<Map.Entry<String, Collection<ValueType>>>> {

    @Override
    public Iterable<Map.Entry<String, Collection<ValueType>>> apply(
        Iterable<ValueType> discoveredValue) {
      return Multimaps.index(discoveredValue, new Function<ValueType, String>() {
        @Override
        public String apply(ValueType input) {
          return input.getPackageName();
        }
      }).asMap().entrySet();
    }
  }

  @Generator.Typedef
  Map.Entry<String, Collection<ValueType>> ByPackage;
}
