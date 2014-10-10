package org.immutables.value.processor;

import com.google.auto.service.AutoService;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import org.immutables.generator.AbstractGenerator;
import org.immutables.generator.Generator;
import org.immutables.value.Value;

@AutoService(javax.annotation.processing.Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@Generator.SupportedAnnotations({ Value.Immutable.class, Value.Nested.class })
public final class Processor extends AbstractGenerator {
  @Override
  protected void process() {
    invoke(new Generator_Values().main());
  }
}
