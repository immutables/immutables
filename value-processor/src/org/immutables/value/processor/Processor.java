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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;
import org.immutables.generator.AbstractGenerator;
import org.immutables.metainf.Metainf;
import org.immutables.value.processor.meta.*;
import org.immutables.value.processor.meta.Proto.DeclaringPackage;

import javax.annotation.processing.SupportedAnnotationTypes;
import java.util.Set;

@Metainf.Service
@SupportedAnnotationTypes({
    FactoryMirror.QUALIFIED_NAME,
    ImmutableMirror.QUALIFIED_NAME,
    EnclosingMirror.QUALIFIED_NAME,
    IncludeMirror.QUALIFIED_NAME,
    ModifiableMirror.QUALIFIED_NAME,
    ValueUmbrellaMirror.QUALIFIED_NAME
})
public final class Processor extends AbstractGenerator {
  @Override
  protected void process() {

    Round round = ImmutableRound.builder()
        .addAllAnnotations(annotations())
        .processing(processing())
        .addAllCustomImmutableAnnotations(CustomImmutableAnnotations.annotations())
        .round(round())
        .build();

    Multimap<DeclaringPackage, ValueType> values = round.collectValues();

    invoke(new Generator_Immutables().usingValues(values).generate());
    invoke(new Generator_Modifiables().usingValues(values).generate());
    invoke(new Generator_Gsons().usingValues(values).generate());
    invoke(new Generator_OkJsons().usingValues(values).generate());
    invoke(new Generator_Repositories().usingValues(values).generate());
    invoke(new Generator_Funcs().usingValues(values).generate());
    invoke(new Generator_Transformers().usingValues(values).generate());
    invoke(new Generator_Asts().usingValues(values).generate());

//  invoke(new Generator_Parboileds().usingValues(values).generate());
//
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return FluentIterable.from(super.getSupportedAnnotationTypes())
        .append(CustomImmutableAnnotations.annotations())
        .toSet();
  }
}
