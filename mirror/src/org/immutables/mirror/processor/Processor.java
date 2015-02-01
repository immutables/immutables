package org.immutables.mirror.processor;

import org.immutables.generator.AbstractGenerator;
import org.immutables.generator.Generator;
import org.immutables.metainf.Metainf;
import org.immutables.mirror.Mirror;

@Metainf.Service
@Generator.SupportedAnnotations(Mirror.Annotation.class)
public class Processor extends AbstractGenerator {
  @Override
  protected void process() {
    invoke(new Generator_Mirrors().generate());
  }
}
