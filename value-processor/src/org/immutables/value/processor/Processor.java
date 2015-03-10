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

import org.immutables.value.processor.meta.ValueUmbrellaMirror;
import org.immutables.value.processor.meta.FactoryMirror;
import org.immutables.value.processor.meta.ImmutableMirror;
import org.immutables.value.processor.meta.EnclosingMirror;
import org.immutables.value.processor.meta.IncludeMirror;
import com.google.common.collect.Multimap;
import javax.annotation.processing.SupportedAnnotationTypes;
import org.immutables.generator.AbstractGenerator;
import org.immutables.metainf.Metainf;
import org.immutables.value.processor.meta.ImmutableRound;
import org.immutables.value.processor.meta.Proto.DeclaringPackage;
import org.immutables.value.processor.meta.Round;
import org.immutables.value.processor.meta.ValueType;

@Metainf.Service
@SupportedAnnotationTypes({
    FactoryMirror.QUALIFIED_NAME,
    ImmutableMirror.QUALIFIED_NAME,
    EnclosingMirror.QUALIFIED_NAME,
    IncludeMirror.QUALIFIED_NAME,
    ValueUmbrellaMirror.QUALIFIED_NAME,
})
public final class Processor extends AbstractGenerator {
  @Override
  protected void process() {
    Round round = ImmutableRound.builder()
        .addAllAnnotations(annotations())
        .processing(processing())
        .round(round())
        .build();

    Multimap<DeclaringPackage, ValueType> values = round.collectValues();

    invoke(new Generator_Immutables().usingValues(values).generate());
    invoke(new Generator_Gsons().usingValues(values).generate());
    invoke(new Generator_Repositories().usingValues(values).generate());

//  invoke(new Generator_Modifiables().usingValues(values).generate());
//  invoke(new Generator_Parboileds().usingValues(values).generate());
//  invoke(new Generator_Transformers().usingValues(values).generate());

  }
}
