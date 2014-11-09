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

import com.google.auto.service.AutoService;
import org.immutables.generator.AbstractGenerator;
import org.immutables.generator.Generator;
import org.immutables.value.Value;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

@AutoService(javax.annotation.processing.Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@Generator.SupportedAnnotations({Value.Immutable.class, Value.Nested.class, Value.Include.class})
public final class Processor extends AbstractGenerator {
  @Override
  protected void process() {
    invoke(new Generator_Values().main());
  }
}
