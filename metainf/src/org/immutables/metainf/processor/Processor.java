package org.immutables.metainf.processor;

import com.google.auto.service.AutoService;
import org.immutables.generator.AbstractGenerator;
import org.immutables.generator.Generator;
import org.immutables.metainf.Metainf;

@Generator.SupportedAnnotations(Metainf.Service.class)
@AutoService(javax.annotation.processing.Processor.class)
public class Processor extends AbstractGenerator {
  @Override
  protected void process() {
    invoke(new Generator_Metaservices().generate());
  }
}
