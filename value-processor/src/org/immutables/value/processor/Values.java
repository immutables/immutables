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

import com.google.common.collect.ImmutableList;
import org.immutables.generator.AbstractTemplate;
import org.immutables.generator.Generator;
import org.immutables.value.processor.meta.ValueType;
import org.immutables.value.processor.meta.Discovery;
import java.util.List;

@Generator.Template
class Values extends AbstractTemplate {

  Immutables immutables = new Generator_Immutables();

  Parboileds parboileds = new Generator_Parboileds();

  Transformers transformers = new Generator_Transformers();

  Marshalers marshalers = new Generator_Marshalers();

  Repositories repositories = new Generator_Repositories();

  private List<ValueType> type;

  public List<ValueType> types() {
    return type != null ? type : (type = new Discovery(
        processing(),
        round(),
        annotations()).discover());
  }

  private List<ValueType> linearizedTypes;

  List<ValueType> linearizedTypes() {
    if (linearizedTypes == null) {
      ImmutableList.Builder<ValueType> builder = ImmutableList.builder();
      for (ValueType type : types()) {
        if (!type.emptyNesting) {
          builder.add(type);
        }
        if (type.isHasNestedChildren()) {
          for (ValueType nestedType : type.getNestedChildren()) {
            builder.add(nestedType);
          }
        }
      }
      linearizedTypes = builder.build();
    }
    return linearizedTypes;
  }

}
